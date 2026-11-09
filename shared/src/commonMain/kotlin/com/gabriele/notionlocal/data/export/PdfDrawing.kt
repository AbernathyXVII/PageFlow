package com.gabriele.notionlocal.data.export

import java.io.File
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// ---------------------------------------------------------------------------
// **Gli attrezzi del PDF** (dal 09/11/2026, quando il PDF è passato nel codice
// comune): l'impaginazione (`PdfExport`) è la stessa su telefono e PC, e chiede
// a [PdfBackend] di misurare il testo e di disegnare su un foglio
// ([PdfCanvas]). Sul telefono lo fanno gli strumenti di Android, come prima;
// sul PC PDFBox.
// ---------------------------------------------------------------------------

enum class PdfCap { BUTT, ROUND, SQUARE }

enum class PdfAlign { LEFT, CENTER, RIGHT }

/**
 * **Come si disegna**: il colore (ARGB) e, per le forme, pieno o solo il bordo
 * ([stroke] è lo spessore); per il testo la misura, il grassetto, il
 * carattere a larghezza fissa e l'allineamento.
 */
data class PdfPaint(
    val color: Int = PdfColors.TEXT,
    val stroke: Float? = null,
    val cap: PdfCap = PdfCap.BUTT,
    val roundJoin: Boolean = false,
    val textSize: Float = 0f,
    val bold: Boolean = false,
    val mono: Boolean = false,
    val align: PdfAlign = PdfAlign.LEFT
)

/** Un pezzo di testo con la sua formattazione. [sizeFactor] è la misura rispetto al testo (16 = 1). */
data class PdfSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strike: Boolean = false,
    val sizeFactor: Float? = null,
    /** -1 pedice, 1 apice, 0 normale (allora il testo è anche più piccolo, 0.75). */
    val script: Int = 0,
    val color: Int? = null,
    val background: Int? = null
)

/** Un testo semplice, tutto uguale. */
fun plainText(text: String): List<PdfSpan> = listOf(PdfSpan(text))

/** Un percorso fatto di segmenti e curve, in punti. */
class PdfPath {
    sealed class Op {
        data class Move(val x: Float, val y: Float) : Op()
        data class Line(val x: Float, val y: Float) : Op()
        data class Quad(val x1: Float, val y1: Float, val x2: Float, val y2: Float) : Op()
        data object Close : Op()
    }

    val ops = ArrayList<Op>()

    fun moveTo(x: Float, y: Float) { ops += Op.Move(x, y) }
    fun lineTo(x: Float, y: Float) { ops += Op.Line(x, y) }
    fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float) { ops += Op.Quad(x1, y1, x2, y2) }
    fun close() { ops += Op.Close }

    /** Dove arriva il percorso, curve comprese (come `computeBounds(exact = true)` di Android): [sinistra, alto, destra, basso]. */
    fun bounds(): FloatArray {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        fun add(x: Float, y: Float) {
            minX = min(minX, x); maxX = max(maxX, x)
            minY = min(minY, y); maxY = max(maxY, y)
        }
        // Il punto più in là di una curva su un asse, se cade dentro la curva.
        fun extreme(a: Float, b: Float, c: Float): Float? {
            val d = a - 2 * b + c
            if (d == 0f) return null
            val t = (a - b) / d
            return if (t > 0f && t < 1f) (1 - t) * (1 - t) * a + 2 * (1 - t) * t * b + t * t * c else null
        }
        var lastX = 0f
        var lastY = 0f
        for (op in ops) {
            when (op) {
                is Op.Move -> { add(op.x, op.y); lastX = op.x; lastY = op.y }
                is Op.Line -> { add(op.x, op.y); lastX = op.x; lastY = op.y }
                is Op.Quad -> {
                    add(op.x2, op.y2)
                    // Ogni asse per sé: basta che il riquadro contenga il punto più in là della curva.
                    extreme(lastX, op.x1, op.x2)?.let { minX = min(minX, it); maxX = max(maxX, it) }
                    extreme(lastY, op.y1, op.y2)?.let { minY = min(minY, it); maxY = max(maxY, it) }
                    lastX = op.x2; lastY = op.y2
                }
                Op.Close -> Unit
            }
        }
        return if (minX > maxX) floatArrayOf(0f, 0f, 0f, 0f) else floatArrayOf(minX, minY, maxX, maxY)
    }
}

/** **Un foglio** su cui disegnare, in punti, con l'origine in alto a sinistra. */
interface PdfCanvas {
    fun drawText(text: String, x: Float, y: Float, paint: PdfPaint)
    fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: PdfPaint)
    fun drawRoundRect(left: Float, top: Float, right: Float, bottom: Float, radius: Float, paint: PdfPaint)
    fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, paint: PdfPaint)
    fun drawCircle(cx: Float, cy: Float, radius: Float, paint: PdfPaint)
    fun drawPath(path: PdfPath, paint: PdfPaint)

    /** L'immagine di [file] nel riquadro, letta larga al più [pixels] punti immagine. */
    fun drawImage(file: File, left: Float, top: Float, right: Float, bottom: Float, pixels: Int)
    fun save()
    fun restore()
    fun clipRect(left: Float, top: Float, right: Float, bottom: Float)
    fun translate(dx: Float, dy: Float)
}

/** **Un testo che va a capo** in una larghezza: le sue righe, in punti dalla cima del testo. */
interface PdfTextLayout {
    val lineCount: Int
    val height: Float
    fun lineTop(line: Int): Float
    fun lineBottom(line: Int): Float
    fun lineBaseline(line: Int): Float

    /** Sopra la linea di base: negativo, come in Android. */
    fun lineAscent(line: Int): Float
    fun lineDescent(line: Int): Float

    /** Disegna la riga [line] dove sta nel testo (chi chiama ha già spostato e ritagliato il foglio). */
    fun drawLine(canvas: PdfCanvas, line: Int)

    /** Disegna tutte le righe (una cella di una tabella). */
    fun draw(canvas: PdfCanvas)
}

/** **Il pennello di un sistema**: misurare il testo, impaginarlo, leggere le immagini, scrivere il file. */
interface PdfBackend {
    /** L'altezza consigliata di una riga del carattere di [paint]. */
    fun fontSpacing(paint: PdfPaint): Float

    /** [text] a capo in [width] punti, con le righe alte [spacing] volte il carattere. */
    fun layout(text: List<PdfSpan>, paint: PdfPaint, width: Int, spacing: Float = 1f): PdfTextLayout

    /** Larghezza e altezza dell'immagine, o null se non si legge. */
    fun imageSize(file: File): Pair<Int, Int>?

    /** Il file PDF di [pageCount] fogli larghi [width] e alti [height] punti, disegnati da [drawPage]. */
    fun writeDocument(width: Int, height: Int, pageCount: Int, out: OutputStream, drawPage: (Int, PdfCanvas) -> Unit)
}

/** Il pennello di questo sistema; null se qui il PDF non si può fare (sul PC, senza i caratteri). */
expect fun defaultPdfBackend(): PdfBackend?

/** I colori della carta e i conti sui colori (gli stessi di `ColorUtils` di Android). */
object PdfColors {
    const val TEXT = 0xFF1F1F1F.toInt()
    const val MUTED = 0xFF6B6B6B.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()
    const val BLACK = 0xFF000000.toInt()

    fun alpha(color: Int) = (color ushr 24) and 0xFF
    fun withAlpha(color: Int, alpha: Int) = (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

    /** "#RRGGBB" o "#AARRGGBB"; null se non è un colore. */
    fun parse(hex: String): Int? {
        val digits = hex.removePrefix("#")
        val value = digits.toLongOrNull(16) ?: return null
        return when (digits.length) {
            6 -> (0xFF000000L or value).toInt()
            8 -> value.toInt()
            else -> null
        }
    }

    fun luminance(color: Int): Double {
        fun channel(c: Int): Double {
            val v = c / 255.0
            return if (v < 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel((color shr 16) and 0xFF) + 0.7152 * channel((color shr 8) and 0xFF) + 0.0722 * channel(color and 0xFF)
    }

    fun blend(from: Int, to: Int, ratio: Float): Int {
        val inverse = 1 - ratio
        fun mix(shift: Int) = (((from shr shift) and 0xFF) * inverse + ((to shr shift) and 0xFF) * ratio).toInt()
        return (mix(24) shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }

    /** Un testo troppo chiaro per la carta bianca si scurisce. */
    fun forPaperText(color: Int): Int = if (luminance(color) > 0.5) blend(color, BLACK, 0.45f) else color

    /** Uno sfondo troppo scuro per la carta bianca si schiarisce. */
    fun forPaperBackground(color: Int): Int = if (luminance(color) < 0.4) blend(color, WHITE, 0.7f) else color
}
