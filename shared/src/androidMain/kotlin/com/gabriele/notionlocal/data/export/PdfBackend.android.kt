package com.gabriele.notionlocal.data.export

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.UnderlineSpan
import java.io.File
import java.io.OutputStream
import kotlin.math.min

actual fun defaultPdfBackend(): PdfBackend? = AndroidPdfBackend

/** Come sul telefono di adesso: la larghezza del suo schermo (vedi `PdfLayout.phone`). */
fun PdfLayout.Companion.phone(context: Context): PdfLayout {
    val metrics = context.resources.displayMetrics
    return phone(min(metrics.widthPixels, metrics.heightPixels) / metrics.density)
}

/**
 * **Il PDF del telefono** con gli strumenti di Android, come dal 02/11/2026:
 * `StaticLayout` per il testo a capo, `PdfDocument` per il file.
 */
object AndroidPdfBackend : PdfBackend {
    private val paints = HashMap<PdfPaint, Paint>()

    /** Il `Paint` di Android di [paint]: con una misura, per il testo; senza, per le forme. */
    @Synchronized
    fun paintOf(paint: PdfPaint): Paint = paints.getOrPut(paint) {
        if (paint.textSize > 0f) {
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = paint.textSize
                color = paint.color
                typeface = when {
                    paint.mono -> Typeface.MONOSPACE
                    paint.bold -> Typeface.DEFAULT_BOLD
                    else -> Typeface.DEFAULT
                }
                textAlign = when (paint.align) {
                    PdfAlign.LEFT -> Paint.Align.LEFT
                    PdfAlign.CENTER -> Paint.Align.CENTER
                    PdfAlign.RIGHT -> Paint.Align.RIGHT
                }
            }
        } else {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = if (paint.stroke == null) Paint.Style.FILL else Paint.Style.STROKE
                paint.stroke?.let { strokeWidth = it }
                strokeCap = when (paint.cap) {
                    PdfCap.BUTT -> Paint.Cap.BUTT
                    PdfCap.ROUND -> Paint.Cap.ROUND
                    PdfCap.SQUARE -> Paint.Cap.SQUARE
                }
                if (paint.roundJoin) strokeJoin = Paint.Join.ROUND
                color = paint.color
            }
        }
    }

    private fun textPaintOf(paint: PdfPaint) = paintOf(paint) as TextPaint

    override fun fontSpacing(paint: PdfPaint): Float = textPaintOf(paint).fontSpacing

    override fun layout(text: List<PdfSpan>, paint: PdfPaint, width: Int, spacing: Float): PdfTextLayout {
        val chars = spannable(text)
        val layout = StaticLayout.Builder.obtain(chars, 0, chars.length, textPaintOf(paint), width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .setLineSpacing(0f, spacing)
            .build()
        return object : PdfTextLayout {
            override val lineCount get() = layout.lineCount
            override val height get() = layout.height.toFloat()
            override fun lineTop(line: Int) = layout.getLineTop(line).toFloat()
            override fun lineBottom(line: Int) = layout.getLineBottom(line).toFloat()
            override fun lineBaseline(line: Int) = layout.getLineBaseline(line).toFloat()
            override fun lineAscent(line: Int) = layout.getLineAscent(line).toFloat()
            override fun lineDescent(line: Int) = layout.getLineDescent(line).toFloat()

            // Tutto il testo: chi chiama ha ritagliato il foglio sulla riga.
            override fun drawLine(canvas: PdfCanvas, line: Int) = draw(canvas)
            override fun draw(canvas: PdfCanvas) = layout.draw((canvas as AndroidPdfCanvas).canvas)
        }
    }

    override fun imageSize(file: File): Pair<Int, Int>? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        return if (bounds.outWidth <= 0 || bounds.outHeight <= 0) null else bounds.outWidth to bounds.outHeight
    }

    override fun writeDocument(width: Int, height: Int, pageCount: Int, out: OutputStream, drawPage: (Int, PdfCanvas) -> Unit) {
        val document = PdfDocument()
        try {
            for (index in 0 until pageCount) {
                val sheet = document.startPage(PdfDocument.PageInfo.Builder(width, height, index + 1).create())
                drawPage(index, AndroidPdfCanvas(sheet.canvas))
                document.finishPage(sheet)
            }
            document.writeTo(out)
        } finally {
            document.close()
        }
    }

    /** Il testo con la formattazione come lo vuole Android (gli stessi pezzi di prima del 09/11/2026). */
    private fun spannable(spans: List<PdfSpan>): CharSequence {
        val result = SpannableStringBuilder()
        for (span in spans) {
            val start = result.length
            result.append(span.text)
            val end = result.length
            if (start == end) continue
            fun set(what: Any) = result.setSpan(what, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            when {
                span.bold && span.italic -> set(StyleSpan(Typeface.BOLD_ITALIC))
                span.bold -> set(StyleSpan(Typeface.BOLD))
                span.italic -> set(StyleSpan(Typeface.ITALIC))
            }
            if (span.underline) set(UnderlineSpan())
            if (span.strike) set(StrikethroughSpan())
            span.sizeFactor?.let { set(RelativeSizeSpan(it)) }
            if (span.script != 0) {
                set(if (span.script < 0) SubscriptSpan() else SuperscriptSpan())
                set(RelativeSizeSpan(0.75f))
            }
            span.color?.let { set(ForegroundColorSpan(it)) }
            span.background?.let { set(BackgroundColorSpan(it)) }
        }
        return result
    }
}

/** Un foglio del telefono: il `Canvas` di Android (di `PdfDocument`, o di una foto nei test). */
class AndroidPdfCanvas(val canvas: Canvas) : PdfCanvas {
    private fun p(paint: PdfPaint) = AndroidPdfBackend.paintOf(paint)

    override fun drawText(text: String, x: Float, y: Float, paint: PdfPaint) = canvas.drawText(text, x, y, p(paint))
    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: PdfPaint) = canvas.drawRect(left, top, right, bottom, p(paint))
    override fun drawRoundRect(left: Float, top: Float, right: Float, bottom: Float, radius: Float, paint: PdfPaint) =
        canvas.drawRoundRect(RectF(left, top, right, bottom), radius, radius, p(paint))
    override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, paint: PdfPaint) = canvas.drawLine(x1, y1, x2, y2, p(paint))
    override fun drawCircle(cx: Float, cy: Float, radius: Float, paint: PdfPaint) = canvas.drawCircle(cx, cy, radius, p(paint))

    override fun drawPath(path: PdfPath, paint: PdfPaint) {
        val android = Path()
        for (op in path.ops) {
            when (op) {
                is PdfPath.Op.Move -> android.moveTo(op.x, op.y)
                is PdfPath.Op.Line -> android.lineTo(op.x, op.y)
                is PdfPath.Op.Quad -> android.quadTo(op.x1, op.y1, op.x2, op.y2)
                PdfPath.Op.Close -> android.close()
            }
        }
        canvas.drawPath(android, p(paint))
    }

    override fun drawImage(file: File, left: Float, top: Float, right: Float, bottom: Float, pixels: Int) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= pixels) sample *= 2
        val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return
        canvas.drawBitmap(bitmap, null, RectF(left, top, right, bottom), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        bitmap.recycle()
    }

    override fun save() {
        canvas.save()
    }

    override fun restore() = canvas.restore()

    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float) {
        canvas.clipRect(left, top, right, bottom)
    }

    override fun translate(dx: Float, dy: Float) = canvas.translate(dx, dy)
}
