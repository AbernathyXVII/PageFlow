package com.gabriele.notionlocal.data.export

import org.apache.fontbox.ttf.TTFParser
import org.apache.fontbox.ttf.TrueTypeCollection
import org.apache.fontbox.ttf.TrueTypeFont
import org.apache.pdfbox.io.RandomAccessReadBuffer
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import org.apache.pdfbox.util.Matrix
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FilterOutputStream
import java.io.OutputStream
import javax.imageio.ImageIO
import kotlin.math.max

/** Il pennello del PC, se ci sono i caratteri di Windows. */
actual fun defaultPdfBackend(): PdfBackend? = DesktopPdfBackend.fontsDir()?.let { DesktopPdfBackend(it) }

/**
 * **Il PDF del PC** (dal 09/11/2026), con PDFBox: il testo è testo vero (si
 * seleziona e si cerca), coi caratteri di Windows dentro il file — Segoe UI,
 * Consolas per il codice e, per quello che Segoe UI non ha, Segoe UI Symbol,
 * Segoe UI Emoji (in bianco e nero), Malgun Gothic (coreano) e Microsoft
 * YaHei (cinese e giapponese).
 *
 * Il testo va a capo fra le parole come sul telefono, ma lo misura PDFBox:
 * le righe vengono un po' diverse da quelle del telefono (Segoe UI non è
 * Roboto), l'impaginazione è la stessa (`PdfExport`).
 *
 * shortcut: solo i caratteri di Windows; sul Mac servono altri file (vedi [fontsDir]).
 */
class DesktopPdfBackend(private val dir: File) : PdfBackend {

    /** Un carattere: il file (e quale dentro una raccolta .ttc), letto una volta per esportazione. */
    class Face(private val file: File) {
        private val bytes by lazy { file.readBytes() }

        /** Per misurare: resta aperto per tutta l'esportazione. */
        val font: TrueTypeFont by lazy { parse() }
        private val cmap by lazy { font.getUnicodeCmapLookup(false) }
        private val unitsPerEm by lazy { font.unitsPerEm.toFloat() }
        private val widths = HashMap<Int, Float>()

        /** Un TrueTypeFont nuovo, per un documento (PDFBox lo chiude col documento). */
        fun parse(): TrueTypeFont {
            if (!file.name.endsWith(".ttc", ignoreCase = true)) return TTFParser().parse(RandomAccessReadBuffer(bytes))
            var first: TrueTypeFont? = null
            TrueTypeCollection(ByteArrayInputStream(bytes)).processAllFonts { font -> if (first == null) first = font }
            return first ?: error("Nessun carattere in $file")
        }

        fun has(codePoint: Int): Boolean = cmap.getGlyphId(codePoint) != 0

        /** La larghezza di [codePoint] alto [size] punti. */
        fun width(codePoint: Int, size: Float): Float =
            widths.getOrPut(codePoint) { font.getAdvanceWidth(cmap.getGlyphId(codePoint)) / unitsPerEm } * size

        val ascent: Float by lazy { font.horizontalHeader.ascender / unitsPerEm }
        val descent: Float by lazy { -font.horizontalHeader.descender / unitsPerEm }
        val lineGap: Float by lazy { font.horizontalHeader.lineGap / unitsPerEm }
    }

    private val faces = HashMap<String, Face?>()

    private fun face(name: String): Face? = faces.getOrPut(name) { File(dir, name).takeIf { it.isFile }?.let(::Face) }

    private fun main(bold: Boolean, italic: Boolean, mono: Boolean): Face {
        val name = when {
            mono -> if (bold) "consolab.ttf" else "consola.ttf"
            bold && italic -> "segoeuiz.ttf"
            bold -> "segoeuib.ttf"
            italic -> "segoeuii.ttf"
            else -> REGULAR
        }
        return face(name) ?: face(REGULAR)!!
    }

    /** Il primo carattere che ha [codePoint]: quello del testo, poi i ripieghi; null se nessuno. */
    private fun faceFor(codePoint: Int, main: Face): Face? =
        if (main.has(codePoint)) main else FALLBACKS.asSequence().mapNotNull { face(it) }.firstOrNull { it.has(codePoint) }

    // --- Il testo in pezzi ----------------------------------------------------

    /** Un pezzo di riga: lo stesso carattere, la stessa misura, la stessa formattazione. */
    class Run(val text: String, val face: Face, val size: Float, val span: PdfSpan, val width: Float, val shift: Float) {
        val isSpace get() = text.isBlank()
    }

    /** [text] in pezzi di una lettera (o di uno spazio), col carattere che la sa scrivere. */
    private fun runs(text: List<PdfSpan>, paint: PdfPaint): List<Run> {
        val result = ArrayList<Run>()
        for (span in text) {
            val size = paint.textSize * (span.sizeFactor ?: 1f) * (if (span.script != 0) 0.75f else 1f)
            val main = main(paint.bold || span.bold, span.italic, paint.mono)
            // Apice e pedice: mezza altezza delle lettere in su o in giù, come in Android.
            val shift = if (span.script == 0) 0f else -span.script * main.ascent * size / 2f
            var i = 0
            while (i < span.text.length) {
                val codePoint = span.text.codePointAt(i)
                val chars = String(Character.toChars(codePoint))
                i += chars.length
                // Le lettere che non si vedono (le varianti degli emoji, gli "a capo" larghi zero) e quelle che nessun carattere ha.
                if (codePoint == 0xFE0F || codePoint == 0xFE0E || codePoint == 0x200D || codePoint == 0x200B) continue
                val face = faceFor(codePoint, main) ?: continue
                result += Run(chars, face, size, span, face.width(codePoint, size), shift)
            }
        }
        return result
    }

    /** Una riga: i suoi pezzi e le sue misure, in punti dalla cima del testo. */
    private class Line(val runs: List<Run>, val top: Float, val ascent: Float, val descent: Float, val bottom: Float)

    override fun fontSpacing(paint: PdfPaint): Float {
        val face = main(paint.bold, false, paint.mono)
        return (face.ascent + face.descent + face.lineGap) * paint.textSize
    }

    override fun layout(text: List<PdfSpan>, paint: PdfPaint, width: Int, spacing: Float): PdfTextLayout {
        val base = main(paint.bold, false, paint.mono)
        val lines = ArrayList<Line>()
        var y = 0f
        fun addLine(runs: List<Run>) {
            val ascent = max(runs.maxOfOrNull { it.face.ascent * it.size - it.shift } ?: 0f, if (runs.isEmpty()) base.ascent * paint.textSize else 0f)
            val descent = max(runs.maxOfOrNull { it.face.descent * it.size + it.shift } ?: 0f, if (runs.isEmpty()) base.descent * paint.textSize else 0f)
            val height = (ascent + descent) * spacing
            lines += Line(runs, y, ascent, descent, y + height)
            y += height
        }
        // Gli "a capo" scritti fanno righe a sé; il resto va a capo fra le parole.
        for (paragraph in splitLines(text)) {
            val all = runs(paragraph, paint)
            if (all.isEmpty()) {
                addLine(emptyList())
                continue
            }
            var start = 0
            while (start < all.size) {
                var end = start
                var used = 0f
                var lastBreak = -1
                while (end < all.size) {
                    val run = all[end]
                    // Gli spazi in fondo alla riga non la allungano.
                    if (!run.isSpace && used + run.width > width && end > start) break
                    used += run.width
                    end++
                    if (run.isSpace) lastBreak = end
                }
                if (end < all.size && lastBreak > start) end = lastBreak
                addLine(all.subList(start, end))
                start = end
            }
        }
        val height = y
        return object : PdfTextLayout {
            override val lineCount get() = lines.size
            override val height get() = height
            override fun lineTop(line: Int) = lines[line].top
            override fun lineBottom(line: Int) = lines[line].bottom
            override fun lineBaseline(line: Int) = lines[line].top + lines[line].ascent
            override fun lineAscent(line: Int) = -lines[line].ascent
            override fun lineDescent(line: Int) = lines[line].descent
            override fun drawLine(canvas: PdfCanvas, line: Int) {
                (canvas as PdfBoxCanvas).drawRuns(lines[line].runs, 0f, lineBaseline(line), paint.color)
            }
            override fun draw(canvas: PdfCanvas) {
                for (line in lines.indices) drawLine(canvas, line)
            }
        }
    }

    /** Il testo diviso agli "a capo". */
    private fun splitLines(text: List<PdfSpan>): List<List<PdfSpan>> {
        val result = ArrayList<List<PdfSpan>>()
        var current = ArrayList<PdfSpan>()
        for (span in text) {
            val parts = span.text.split('\n')
            parts.forEachIndexed { index, part ->
                if (index > 0) {
                    result += current
                    current = ArrayList()
                }
                if (part.isNotEmpty()) current += span.copy(text = part)
            }
        }
        result += current
        return result
    }

    override fun imageSize(file: File): Pair<Int, Int>? = runCatching {
        ImageIO.createImageInputStream(file).use { input ->
            val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull() ?: return null
            try {
                reader.input = input
                reader.getWidth(0) to reader.getHeight(0)
            } finally {
                reader.dispose()
            }
        }
    }.getOrNull()

    override fun writeDocument(width: Int, height: Int, pageCount: Int, out: OutputStream, drawPage: (Int, PdfCanvas) -> Unit) {
        PDDocument().use { document ->
            val fonts = HashMap<Face, PDType0Font>()
            val fontOf = { face: Face -> fonts.getOrPut(face) { PDType0Font.load(document, face.parse(), true) } }
            repeat(pageCount) { index ->
                val page = PDPage(PDRectangle(width.toFloat(), height.toFloat()))
                document.addPage(page)
                PDPageContentStream(document, page).use { stream ->
                    // L'origine in alto a sinistra, come sul telefono.
                    stream.transform(Matrix(1f, 0f, 0f, -1f, 0f, height.toFloat()))
                    drawPage(index, PdfBoxCanvas(this, document, stream, fontOf))
                }
            }
            // PDFBox chiude il flusso a fine salvataggio: quello dello .zip deve restare aperto.
            document.save(object : FilterOutputStream(out) {
                override fun write(b: ByteArray, off: Int, len: Int) = out.write(b, off, len)
                override fun close() = out.flush()
            })
        }
    }

    internal fun measure(text: String, paint: PdfPaint): List<Run> = runs(plainText(text), paint)

    companion object {
        private const val REGULAR = "segoeui.ttf"
        private val FALLBACKS = listOf("seguisym.ttf", "seguiemj.ttf", "malgun.ttf", "msyh.ttc", "simsun.ttc")

        /** La cartella dei caratteri di Windows, se c'è Segoe UI. */
        fun fontsDir(): File? {
            val windows = System.getenv("WINDIR") ?: "C:\\Windows"
            return File(windows, "Fonts").takeIf { File(it, REGULAR).isFile }
        }
    }
}

/** Un foglio del PDF del PC, coi comandi di PDFBox. */
class PdfBoxCanvas(
    private val backend: DesktopPdfBackend,
    private val document: PDDocument,
    private val stream: PDPageContentStream,
    private val fontOf: (DesktopPdfBackend.Face) -> PDType0Font
) : PdfCanvas {

    private fun rgb(color: Int) = floatArrayOf(((color shr 16) and 0xFF) / 255f, ((color shr 8) and 0xFF) / 255f, (color and 0xFF) / 255f)

    /** Disegna con il colore (e la trasparenza) di [paint]: pieno o solo il bordo. */
    private fun paint(paint: PdfPaint, shape: () -> Unit) {
        val alpha = PdfColors.alpha(paint.color)
        stream.saveGraphicsState()
        if (alpha < 255) {
            stream.setGraphicsStateParameters(PDExtendedGraphicsState().apply {
                nonStrokingAlphaConstant = alpha / 255f
                strokingAlphaConstant = alpha / 255f
            })
        }
        val (r, g, b) = rgb(paint.color).toList()
        val stroke = paint.stroke
        if (stroke == null) {
            stream.setNonStrokingColor(r, g, b)
        } else {
            stream.setStrokingColor(r, g, b)
            stream.setLineWidth(stroke)
            stream.setLineCapStyle(paint.cap.ordinal)
            if (paint.roundJoin) stream.setLineJoinStyle(1)
        }
        shape()
        if (stroke == null) stream.fill() else stream.stroke()
        stream.restoreGraphicsState()
    }

    /** Dei pezzi di testo su una linea di base, da [x]; [color] per quelli senza un colore loro. */
    internal fun drawRuns(runs: List<DesktopPdfBackend.Run>, x: Float, baseline: Float, color: Int) {
        // Prima gli sfondi, poi il testo, poi le righe sopra e sotto.
        var left = x
        for (run in runs) {
            run.span.background?.let { background ->
                val top = baseline + run.shift - run.face.ascent * run.size
                val bottom = baseline + run.shift + run.face.descent * run.size
                paint(PdfPaint(color = background)) { stream.addRect(left, top, run.width, bottom - top) }
            }
            left += run.width
        }
        left = x
        var i = 0
        while (i < runs.size) {
            // I pezzi uguali di fila in un comando solo.
            val first = runs[i]
            var j = i + 1
            while (j < runs.size && runs[j].face === first.face && runs[j].size == first.size && runs[j].span == first.span) j++
            val group = runs.subList(i, j)
            val text = group.joinToString("") { it.text }
            val width = group.sumOf { it.width.toDouble() }.toFloat()
            val textColor = first.span.color ?: color
            val (r, g, b) = rgb(textColor).toList()
            stream.beginText()
            stream.setFont(fontOf(first.face), first.size)
            stream.setNonStrokingColor(r, g, b)
            // Il foglio è capovolto (l'origine in alto): le lettere si raddrizzano.
            stream.setTextMatrix(Matrix(1f, 0f, 0f, -1f, left, baseline + first.shift))
            stream.showText(text)
            stream.endText()
            val thickness = max(0.5f, first.size / 18f)
            if (first.span.underline) {
                val y = baseline + first.shift + first.size * 0.12f
                paint(PdfPaint(color = textColor)) { stream.addRect(left, y, width, thickness) }
            }
            if (first.span.strike) {
                val y = baseline + first.shift - first.size * 0.28f
                paint(PdfPaint(color = textColor)) { stream.addRect(left, y, width, thickness) }
            }
            left += width
            i = j
        }
    }

    override fun drawText(text: String, x: Float, y: Float, paint: PdfPaint) {
        val runs = backend.measure(text, paint)
        val width = runs.sumOf { it.width.toDouble() }.toFloat()
        val left = when (paint.align) {
            PdfAlign.LEFT -> x
            PdfAlign.CENTER -> x - width / 2f
            PdfAlign.RIGHT -> x - width
        }
        drawRuns(runs, left, y, paint.color)
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: PdfPaint) =
        paint(paint) { stream.addRect(left, top, right - left, bottom - top) }

    override fun drawRoundRect(left: Float, top: Float, right: Float, bottom: Float, radius: Float, paint: PdfPaint) = paint(paint) {
        val r = minOf(radius, (right - left) / 2f, (bottom - top) / 2f)
        val c = r * (1f - KAPPA)
        stream.moveTo(left + r, top)
        stream.lineTo(right - r, top)
        stream.curveTo(right - c, top, right, top + c, right, top + r)
        stream.lineTo(right, bottom - r)
        stream.curveTo(right, bottom - c, right - c, bottom, right - r, bottom)
        stream.lineTo(left + r, bottom)
        stream.curveTo(left + c, bottom, left, bottom - c, left, bottom - r)
        stream.lineTo(left, top + r)
        stream.curveTo(left, top + c, left + c, top, left + r, top)
        stream.closePath()
    }

    override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, paint: PdfPaint) = paint(paint) {
        stream.moveTo(x1, y1)
        stream.lineTo(x2, y2)
    }

    override fun drawCircle(cx: Float, cy: Float, radius: Float, paint: PdfPaint) = paint(paint) {
        val k = radius * KAPPA
        stream.moveTo(cx + radius, cy)
        stream.curveTo(cx + radius, cy + k, cx + k, cy + radius, cx, cy + radius)
        stream.curveTo(cx - k, cy + radius, cx - radius, cy + k, cx - radius, cy)
        stream.curveTo(cx - radius, cy - k, cx - k, cy - radius, cx, cy - radius)
        stream.curveTo(cx + k, cy - radius, cx + radius, cy - k, cx + radius, cy)
        stream.closePath()
    }

    override fun drawPath(path: PdfPath, paint: PdfPaint) = paint(paint) {
        var x = 0f
        var y = 0f
        for (op in path.ops) {
            when (op) {
                is PdfPath.Op.Move -> { stream.moveTo(op.x, op.y); x = op.x; y = op.y }
                is PdfPath.Op.Line -> { stream.lineTo(op.x, op.y); x = op.x; y = op.y }
                // Una curva di secondo grado è una di terzo coi punti di controllo a due terzi.
                is PdfPath.Op.Quad -> {
                    stream.curveTo(
                        x + 2f / 3f * (op.x1 - x), y + 2f / 3f * (op.y1 - y),
                        op.x2 + 2f / 3f * (op.x1 - op.x2), op.y2 + 2f / 3f * (op.y1 - op.y2),
                        op.x2, op.y2
                    )
                    x = op.x2; y = op.y2
                }
                PdfPath.Op.Close -> stream.closePath()
            }
        }
    }

    override fun drawImage(file: File, left: Float, top: Float, right: Float, bottom: Float, pixels: Int) {
        val read = runCatching { ImageIO.read(file) }.getOrNull() ?: return
        // Mai più grande di quanto serve: le immagini a circa 150 punti per pollice.
        val image = if (read.width <= pixels * 2) read else {
            val h = max(1, read.height * pixels / read.width)
            val type = if (read.colorModel.hasAlpha()) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
            BufferedImage(pixels, h, type).also { scaled ->
                val g = scaled.createGraphics()
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                g.drawImage(read, 0, 0, pixels, h, null)
                g.dispose()
            }
        }
        val pdImage = if (image.colorModel.hasAlpha()) LosslessFactory.createFromImage(document, image) else JPEGFactory.createFromImage(document, image, 0.85f)
        // L'immagine diritta sul foglio capovolto.
        stream.drawImage(pdImage, Matrix(right - left, 0f, 0f, -(bottom - top), left, bottom))
    }

    override fun save() = stream.saveGraphicsState()
    override fun restore() = stream.restoreGraphicsState()

    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float) {
        stream.addRect(left, top, right - left, bottom - top)
        stream.clip()
    }

    override fun translate(dx: Float, dy: Float) = stream.transform(Matrix.getTranslateInstance(dx, dy))

    private companion object {
        /** Quanto si allontanano i punti di controllo per un quarto di cerchio. */
        const val KAPPA = 0.5522848f
    }
}
