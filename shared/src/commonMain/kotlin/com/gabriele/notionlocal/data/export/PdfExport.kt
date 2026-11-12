package com.gabriele.notionlocal.data.export

import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.InkStrokeEntity
import com.gabriele.notionlocal.data.entity.InkTool
import com.gabriele.notionlocal.data.entity.ParagraphLook
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.data.entity.paragraph
import com.gabriele.notionlocal.data.ink.anchorIds
import com.gabriele.notionlocal.data.ink.decodeInkPoints
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.i18n.ButtonStrings
import com.gabriele.notionlocal.viewmodel.pressureFactor
import java.io.File
import java.io.OutputStream
import java.text.DateFormat
import java.util.Date
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// ---------------------------------------------------------------------------
// **La pagina in PDF** (dal 02/11/2026): fogli col testo vero (si seleziona e
// si cerca), le immagini, le tabelle e il numero di pagina in fondo. È il
// formato per mandare una pagina a qualcuno e per stamparla.
//
// Si impagina in due passate: prima si decide cosa va su ogni foglio (così si
// sa quanti sono, per "2 / 5"), poi si disegna. Le immagini si leggono dal
// file solo al momento di disegnarle, una alla volta.
//
// **Dal 03/11/2026** ci sono anche i database (la tabella della loro vista, o
// una scheda per riga se le colonne sono tante), i bottoni con le loro azioni
// e, a richiesta, i disegni a mano: allora il PDF ha le proporzioni del
// telefono (`PdfLayout.phone`), perché i tratti sono salvati in dp rispetto a
// quello su cui sono fatti, e cadano al loro posto.
//
// **Dal 09/11/2026** è nel codice comune: l'impaginazione è la stessa su
// telefono e PC, il testo lo misurano e i fogli li disegnano gli attrezzi di
// ogni sistema (`PdfBackend`: Android sul telefono, PDFBox sul PC).
// ---------------------------------------------------------------------------

/** Com'è venuto il PDF: quanti fogli. */
data class PdfResult(val pages: Int)

/**
 * **Le misure del PDF**, in punti. [DOCUMENT] è un documento A4 col testo a
 * 11; [phone] rifà la pagina come sul telefono, su fogli A5: il testo e le
 * righe alti come nell'editor, in proporzione alla larghezza dello schermo.
 */
data class PdfLayout(
    val pageWidth: Int,
    val pageHeight: Int,
    val margin: Float,
    val marginTop: Float,
    val marginBottom: Float,
    /** Dove comincia il titolo sul primo foglio di ogni pagina. */
    val titleTop: Float,
    val bodySize: Float,
    val titleSize: Float,
    /** Punti per dp: quanto è grande qui un dp del telefono. Null = un documento, senza disegni. */
    val dpScale: Float?
) {
    companion object {
        val DOCUMENT = PdfLayout(
            pageWidth = 595, pageHeight = 842,
            margin = 56f, marginTop = 56f, marginBottom = 56f, titleTop = 56f,
            bodySize = 11f, titleSize = 22f, dpScale = null
        )

        /** Come sul telefono il cui schermo è largo [screenWidthDp]: tutto lo schermo è la larghezza del foglio. */
        fun phone(screenWidthDp: Float): PdfLayout {
            val s = A5_WIDTH / screenWidthDp
            return PdfLayout(
                pageWidth = A5_WIDTH.toInt(), pageHeight = A5_HEIGHT,
                margin = SIDE_PADDING_DP * s, marginTop = 24f * s, marginBottom = 28f * s,
                titleTop = HEADER_TITLE_TOP_DP * s,
                bodySize = 16f * s, titleSize = 30f * s, dpScale = s
            )
        }

        private const val A5_WIDTH = 420f
        private const val A5_HEIGHT = 595

        /** I margini di lato dell'editor (`PAGE_SIDE_PADDING`). */
        private const val SIDE_PADDING_DP = 20f

        /** Dove comincia il titolo sotto la cima della pagina, nell'editor (sotto i pulsanti sospesi). */
        private const val HEADER_TITLE_TOP_DP = 96f
    }
}

class PdfExport(
    /** Il file di un blocco (`BlockAttrs.file`), se c'è. */
    private val fileOf: (String) -> File?,
    private val untitled: String,
    private val layout: PdfLayout = PdfLayout.DOCUMENT,
    private val dateText: (Long) -> String = { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it)) },
    /** Chi misura e disegna (vedi `PdfBackend`). */
    private val backend: PdfBackend = requireNotNull(defaultPdfBackend()) { "Qui il PDF non si può fare" }
) {
    /** Scrive [pages] (la pagina, poi le sottopagine, ognuna su un foglio nuovo) in [out]. */
    fun write(pages: List<ExportPage>, out: OutputStream): PdfResult {
        val sheets = layOut(pages)
        backend.writeDocument(layout.pageWidth, layout.pageHeight, sheets.list.size, out) { index, canvas -> draw(sheets, index, canvas) }
        return PdfResult(sheets.list.size)
    }

    /** Cosa va su ogni foglio. */
    fun layOut(pages: List<ExportPage>): Sheets {
        val sheets = Sheets()
        pages.forEachIndexed { index, page ->
            if (index > 0) sheets.newSheet()
            layOutPage(page, sheets)
        }
        return sheets
    }

    /** Il foglio [index] su [canvas], col numero in fondo se i fogli sono più di uno. */
    fun draw(sheets: Sheets, index: Int, canvas: PdfCanvas) {
        for (op in sheets.list[index]) op(canvas)
        val total = sheets.list.size
        if (total > 1) {
            canvas.drawText("${index + 1} / $total", layout.pageWidth / 2f, layout.pageHeight - layout.marginBottom / 2f, footerPaint)
        }
    }

    // --- Le misure ----------------------------------------------------------

    /** Tutto in proporzione al testo: le misure qui sotto sono per il testo a 11. */
    private val k = layout.bodySize / 11f
    private val left0 = layout.margin
    private val right = layout.pageWidth - layout.margin
    private val bottom = layout.pageHeight - layout.marginBottom
    private val contentWidth = right - left0
    private val contentHeight = bottom - layout.marginTop
    private val indent = 18f * k
    private val markerWidth = 18f * k
    private val quotePadding = 12f * k
    private val boxPadding = 8f * k
    private val cellPadding = 5f * k
    private val dividerHeight = 14f * k
    private val blockGap = if (layout.dpScale == null) 3f else 0f
    private val titleGap = 12f * k

    private val bodyPaint = textPaint(layout.bodySize)
    private val boldPaint = textPaint(layout.bodySize, bold = true)
    private val titlePaint = textPaint(layout.titleSize, bold = true)
    private val mutedPaint = textPaint(layout.bodySize).copy(color = MUTED_COLOR)
    private val smallPaint = textPaint(layout.bodySize * 0.9f)
    private val smallMutedPaint = textPaint(layout.bodySize * 0.9f).copy(color = MUTED_COLOR)
    private val monoPaint = textPaint(9.5f * k, mono = true)
    private val cellPaint = textPaint(10f * k)
    private val headerCellPaint = textPaint(10f * k, bold = true)
    private val numberPaint = textPaint(layout.bodySize).copy(align = PdfAlign.RIGHT)
    private val footerPaint = textPaint(8f * k).copy(color = MUTED_COLOR, align = PdfAlign.CENTER)
    private val rulePaint = PdfPaint(color = 0xFFC8C8C8.toInt(), stroke = 0.6f * k)
    private val boxPaint = PdfPaint(color = MUTED_COLOR, stroke = 0.9f * k)
    private val tickPaint = PdfPaint(color = PdfColors.WHITE, stroke = 1.4f * k, cap = PdfCap.ROUND, roundJoin = true)

    /** L'altezza fissa di una riga di testo come nell'editor, in punti; null per il documento (quella del carattere). */
    private fun fixedLine(spans: List<RichTextSpan>, checkbox: Boolean = false, spacing: Int = 0): Float? {
        val s = layout.dpScale ?: return null
        val largest = spans.maxOfOrNull { it.size ?: 16 } ?: 16
        val scale = (largest / 16f).coerceAtLeast(1f)
        return ((if (checkbox) CHECKBOX_LINE_DP else RUN_LINE_DP) * scale + spacing) * s
    }

    // --- I fogli ------------------------------------------------------------

    /** Un punto di un foglio: quale foglio, e quanto in giù. */
    data class Spot(val sheet: Int, val y: Float)

    /** I fogli, ognuno con le cose da disegnarci; [y] è dove si scrive adesso. */
    inner class Sheets {
        val list = ArrayList<MutableList<(PdfCanvas) -> Unit>>().apply { add(ArrayList()) }
        var y = layout.titleTop

        fun newSheet() {
            list += ArrayList<(PdfCanvas) -> Unit>()
            y = layout.marginTop
        }

        /** Un foglio nuovo se [height] non ci sta più (e su questo c'è già qualcosa). */
        fun ensure(height: Float) {
            if (y + height > bottom && y > layout.marginTop) newSheet()
        }

        fun draw(op: (PdfCanvas) -> Unit) {
            list.last() += op
        }

        fun drawOn(sheet: Int, op: (PdfCanvas) -> Unit) {
            while (list.size <= sheet) list += ArrayList<(PdfCanvas) -> Unit>()
            list[sheet] += op
        }

        val here: Spot get() = Spot(list.lastIndex, y)
    }

    // --- La pagina ----------------------------------------------------------

    private fun layOutPage(page: ExportPage, sheets: Sheets) {
        val entity = page.page
        // La pagina comincia in cima a un foglio: lì sta la testata, a cui si
        // attaccano i disegni fatti sul titolo.
        val headerTop = Spot(sheets.list.lastIndex, 0f)
        sheets.y = layout.titleTop
        // Un database senza un'icona scelta non ha l'emoji di riserva del foglietto (come nell'app).
        val icon = entity.icon.takeIf {
            entity.iconImage == null && entity.iconSymbol == null && it.isNotBlank() && !(entity.isDatabase && it == DEFAULT_DATABASE_FALLBACK_EMOJI)
        }
        val title = (if (icon != null) "$icon  " else "") + entity.title.ifBlank { untitled }
        paragraph(sheets, title, titlePaint, left0, contentWidth)
        sheets.y += titleGap

        // Le voci della pagina come nell'editor (vedi `groupIntoRenderItems`):
        // i gruppi di righe di testo e le isole. I disegni si attaccano alla
        // cima di una voce.
        val items = itemsOf(page)
        val look = ParagraphLook(entity.paragraph, AppSettings.blockSpacing)
        val runLine = HashMap<String, Float?>()
        for (item in items.runs) {
            val spans = item.flatMap { spansOf(it) }
            val line = fixedLine(spans, checkbox = item.any { it.type == BlockType.CHECKBOX }, spacing = look.spacingOf(item.first()))
            item.forEach { runLine[it.id] = line }
        }
        val itemTops = HashMap<String, Spot>()
        for (line in exportLines(page)) {
            val block = line.block
            if (items.itemOf[block.id] == block.id) itemTops[block.id] = sheets.here
            block(sheets, line, page, runLine[block.id])
            sheets.y += blockGap
        }
        if (page.strokes.isNotEmpty()) drawStrokes(sheets, page.strokes, headerTop, items.itemOf, itemTops)
    }

    /** Le voci della pagina: per ogni blocco in cima, la prima riga della sua voce; e i gruppi di righe. */
    private class Items(val itemOf: Map<String, String>, val runs: List<List<BlockEntity>>)

    private fun itemsOf(page: ExportPage): Items {
        val itemOf = HashMap<String, String>()
        val runs = ArrayList<List<BlockEntity>>()
        var run = ArrayList<BlockEntity>()
        var chars = 0
        fun close() {
            if (run.isNotEmpty()) runs += run
            run = ArrayList()
            chars = 0
        }
        for (block in page.blocks.filter { it.parentBlockId == null }.sortedBy { it.orderIndex }) {
            if (block.type in FLOWING_TYPES) {
                val length = spansOf(block).sumOf { it.text.length }
                // Come i pezzi dell'editor: al massimo 50 righe o 3500 lettere.
                if (run.size >= CHUNK_MAX_LINES || (run.isNotEmpty() && chars + length > CHUNK_MAX_CHARS)) close()
                run += block
                chars += length
                itemOf[block.id] = run.first().id
            } else {
                close()
                itemOf[block.id] = block.id
            }
        }
        close()
        return Items(itemOf, runs)
    }

    private fun block(sheets: Sheets, line: ExportLine, page: ExportPage, runLine: Float?) {
        val block = line.block
        val left = left0 + line.depth * indent
        val width = right - left
        val fixed = runLine ?: fixedLine(line.spans)
        fun titleOf(id: String?): String = id?.let { page.linkedTitles[it] }?.ifBlank { untitled } ?: line.text.ifBlank { untitled }
        when (block.type) {
            BlockType.PARAGRAPH -> {
                if (line.text.isBlank()) {
                    sheets.ensure(fixed ?: backend.fontSpacing(bodyPaint))
                    sheets.y += fixed ?: backend.fontSpacing(bodyPaint)
                } else {
                    paragraph(sheets, styled(line.spans), bodyPaint, left, width, fixed = fixed)
                }
            }
            BlockType.BULLET_LIST_ITEM -> paragraph(sheets, styled(line.spans), bodyPaint, left + markerWidth, width - markerWidth, fixed = fixed) { canvas, baseline ->
                canvas.drawCircle(left + markerWidth / 2 - 2f * k, baseline - bodyPaint.textSize * 0.33f, 2.2f * k, fill(TEXT_COLOR))
            }
            BlockType.NUMBERED_LIST_ITEM -> paragraph(sheets, styled(line.spans), bodyPaint, left + markerWidth, width - markerWidth, fixed = fixed) { canvas, baseline ->
                canvas.drawText("${line.number}.", left + markerWidth - 5f * k, baseline, numberPaint)
            }
            // Spuntata, barrata e più chiara, come nell'editor.
            BlockType.CHECKBOX -> paragraph(
                sheets,
                if (block.isChecked) styled(line.spans.map { it.copy(strikethrough = true) }).withColor(MUTED_COLOR) else styled(line.spans),
                bodyPaint,
                left + markerWidth,
                width - markerWidth,
                fixed = fixed
            ) { canvas, baseline ->
                checkbox(canvas, left + 1f, baseline, block.isChecked)
            }
            BlockType.TOGGLE -> paragraph(sheets, styled(line.spans), bodyPaint, left + markerWidth, width - markerWidth, fixed = fixed) { canvas, baseline ->
                val size = bodyPaint.textSize * 0.42f
                val x = left + 3f * k
                val y = baseline - bodyPaint.textSize * 0.33f
                canvas.drawPath(PdfPath().apply {
                    moveTo(x, y - size)
                    lineTo(x + size * 1.3f, y)
                    lineTo(x, y + size)
                    close()
                }, fill(TEXT_COLOR))
            }
            BlockType.QUOTE -> paragraph(sheets, styled(line.spans), bodyPaint, left + quotePadding, width - quotePadding, fixed = fixed, background = { canvas, top, bottom ->
                canvas.drawRect(left, top, left + 2.5f * k, bottom, fill(0xFF9B9B9B.toInt()))
            })
            BlockType.CALLOUT -> boxed(sheets, CALLOUT_COLOR, left) {
                val icon = block.attrs.icon ?: "💡"
                paragraph(sheets, styled(line.spans), bodyPaint, left + boxPadding + markerWidth, width - boxPadding * 2 - markerWidth, fixed = fixed, background = { canvas, top, bottom ->
                    canvas.drawRect(left, top, right, bottom, fill(CALLOUT_COLOR))
                }) { canvas, baseline ->
                    canvas.drawText(icon, left + boxPadding, baseline, bodyPaint)
                }
            }
            BlockType.CODE -> boxed(sheets, CODE_COLOR, left) {
                paragraph(sheets, line.text, monoPaint, left + boxPadding, width - boxPadding * 2, background = { canvas, top, bottom ->
                    canvas.drawRect(left, top, right, bottom, fill(CODE_COLOR))
                })
            }
            BlockType.NOTIFICATION -> {
                val text = buildList {
                    add(PdfSpan("🔔 "))
                    addAll(styled(line.spans))
                    block.notifyAt?.let { add(PdfSpan((if (line.text.isBlank()) "" else " · ") + dateText(it))) }
                }
                paragraph(sheets, text, bodyPaint, left, width, fixed = fixed)
            }
            BlockType.BUTTON -> page.buttons[block.id]?.let { button(sheets, it, left, width) }
            BlockType.SYNCED, BlockType.COLUMN_LIST, BlockType.COLUMN -> Unit
            BlockType.DIVIDER -> {
                sheets.ensure(dividerHeight)
                val y = sheets.y + dividerHeight / 2
                sheets.draw { it.drawLine(left, y, right, y, rulePaint) }
                sheets.y += dividerHeight
            }
            BlockType.PAGE_LINK, BlockType.PAGE_MENTION -> {
                val id = if (block.type == BlockType.PAGE_MENTION) block.attrs.pageId ?: block.linkedPageId else block.linkedPageId
                paragraph(sheets, underlined("📄 ", titleOf(id)), bodyPaint, left, width, fixed = fixed)
            }
            BlockType.DATABASE_LINK -> {
                val database = page.databases[block.id]
                if (database == null) {
                    paragraph(sheets, "▦ " + titleOf(block.linkedPageId), mutedPaint, left, width)
                } else {
                    database(sheets, database, left, width, withTitle = block.linkedPageId != page.page.id)
                }
            }
            BlockType.IMAGE -> {
                val file = block.attrs.file?.let(fileOf)
                if (file == null || !image(sheets, file, left, width, block.attrs.width)) {
                    paragraph(sheets, "🖼 " + (block.attrs.name ?: block.attrs.url ?: ""), mutedPaint, left, width)
                }
            }
            BlockType.VIDEO, BlockType.AUDIO, BlockType.FILE, BlockType.BOOKMARK -> {
                val attrs = block.attrs
                val icon = when (block.type) {
                    BlockType.VIDEO -> "🎬 "
                    BlockType.AUDIO -> "🔊 "
                    BlockType.FILE -> "📎 "
                    else -> "🔖 "
                }
                val name = (attrs.name ?: line.text.ifBlank { null } ?: attrs.url ?: "").trim()
                val text = buildList {
                    add(PdfSpan(icon + name))
                    attrs.url?.takeIf { it != name }?.let { url ->
                        add(PdfSpan("\n"))
                        add(PdfSpan(url, color = LINK_COLOR, sizeFactor = 0.85f))
                    }
                }
                paragraph(sheets, text, bodyPaint, left, width)
            }
            BlockType.TABLE -> {
                val byCell = line.cells.associateBy { it.rowIndex to it.colIndex }
                val cols = block.tableCols.coerceAtLeast(1)
                val rows = (0 until block.tableRows.coerceAtLeast(1)).map { row ->
                    (0 until cols).map { col -> byCell[row to col]?.text.orEmpty() }
                }
                grid(sheets, rows, header = false, left = left, width = width)
            }
        }
    }

    /**
     * Un riquadro colorato (callout, codice, bottone): un po' di colore
     * sopra, le righe col loro colore dietro, un po' sotto. Un riquadro che
     * va a capo pagina continua sul foglio dopo.
     */
    private fun boxed(sheets: Sheets, color: Int, left: Float, content: () -> Unit) {
        sheets.ensure(boxPadding * 2 + backend.fontSpacing(bodyPaint))
        val top = sheets.y
        sheets.draw { it.drawRect(left, top, right, top + boxPadding, fill(color)) }
        sheets.y += boxPadding
        content()
        val end = sheets.y
        sheets.draw { it.drawRect(left, end, right, end + boxPadding, fill(color)) }
        sheets.y += boxPadding
    }

    /** **Un bottone**: il suo nome e "Quando lo tocchi:", poi le azioni, in un riquadro. */
    private fun button(sheets: Sheets, button: ExportButton, left: Float, width: Float) {
        val inner = left + boxPadding
        val innerWidth = width - boxPadding * 2
        val background: (PdfCanvas, Float, Float) -> Unit = { canvas, top, end -> canvas.drawRect(left, top, right, end, fill(BUTTON_COLOR)) }
        boxed(sheets, BUTTON_COLOR, left) {
            val head = listOf(PdfSpan("🔘 "), PdfSpan(button.name, bold = true), PdfSpan(" — ${ButtonStrings.whenTapped}:"))
            paragraph(sheets, head, bodyPaint, inner, innerWidth, background = background)
            for (step in button.steps) paragraph(sheets, step, smallPaint, inner + indent / 2, innerWidth - indent / 2, background = background)
        }
    }

    /**
     * **Un database**: il suo nome, poi la tabella della vista; con più di
     * [MAX_TABLE_COLUMNS] colonne non ci starebbe, e ogni riga diventa una
     * scheda col nome e sotto le proprietà. Una riga la cui pagina ha un PDF
     * suo ha il 📄 davanti al nome.
     */
    private fun database(sheets: Sheets, database: ExportDatabase, left: Float, width: Float, withTitle: Boolean) {
        if (withTitle) {
            paragraph(sheets, "▦ " + database.title.ifBlank { untitled }, boldPaint, left, width)
            sheets.y += 4f * k
        }
        fun name(row: ExportDatabaseRow) = (if (row.page != null) "📄 " else "") + row.title.ifBlank { untitled }
        if (database.headers.size <= MAX_TABLE_COLUMNS) {
            val rows = database.rows.map { row -> row.cells.mapIndexed { i, value -> if (i == database.nameIndex) name(row) else value } }
            grid(sheets, listOf(database.headers) + rows, header = true, left = left, width = width)
        } else {
            for (row in database.rows) {
                sheets.ensure(backend.fontSpacing(boldPaint) * 2)
                paragraph(sheets, name(row), boldPaint, left, width)
                row.cells.forEachIndexed { i, value ->
                    if (i == database.nameIndex || value.isBlank()) return@forEachIndexed
                    val text = listOf(PdfSpan(database.headers[i] + ": ", color = MUTED_COLOR), PdfSpan(value))
                    paragraph(sheets, text, smallPaint, left + indent / 2, width - indent / 2)
                }
                sheets.y += 3f * k
                val y = sheets.y
                sheets.draw { it.drawLine(left, y, right, y, rulePaint) }
                sheets.y += 5f * k
            }
        }
        if (database.rows.isEmpty() && database.headers.size > MAX_TABLE_COLUMNS) {
            paragraph(sheets, "—", smallMutedPaint, left, width)
        }
    }

    /**
     * **Una griglia di celle**: il testo va a capo nella cella, le righe
     * passano al foglio dopo intere. Le colonne sono larghe secondo quanto c'è
     * scritto, ma ognuna almeno un po'. Con [header] la prima riga è in
     * grassetto su grigio.
     */
    private fun grid(sheets: Sheets, rows: List<List<String>>, header: Boolean, left: Float, width: Float) {
        val cols = rows.maxOfOrNull { it.size }?.coerceAtLeast(1) ?: return
        val weights = (0 until cols).map { col ->
            val longest = rows.maxOfOrNull { row -> row.getOrNull(col)?.lines()?.maxOfOrNull { it.length } ?: 0 } ?: 0
            longest.coerceIn(4, 28).toFloat()
        }
        val total = weights.sum()
        val widths = weights.map { width * it / total }
        rows.forEachIndexed { index, row ->
            val bold = header && index == 0
            val layouts = (0 until cols).map { col ->
                val text = row.getOrNull(col).orEmpty()
                backend.layout(plainText(text), if (bold) headerCellPaint else cellPaint, max(1, (widths[col] - cellPadding * 2).toInt()))
            }
            val height = min(contentHeight, (layouts.maxOf { it.height } + cellPadding * 2).coerceAtLeast(backend.fontSpacing(cellPaint) + cellPadding * 2))
            sheets.ensure(height)
            val top = sheets.y
            sheets.draw { canvas ->
                var x = left
                layouts.forEachIndexed { col, cell ->
                    val w = widths[col]
                    if (bold) canvas.drawRect(x, top, x + w, top + height, fill(HEADER_CELL_COLOR))
                    canvas.drawRect(x, top, x + w, top + height, rulePaint)
                    canvas.save()
                    canvas.clipRect(x, top, x + w, top + height)
                    canvas.translate(x + cellPadding, top + cellPadding)
                    cell.draw(canvas)
                    canvas.restore()
                    x += w
                }
            }
            sheets.y += height
        }
    }

    /**
     * Un testo che va a capo da solo nella larghezza data, una riga alla
     * volta: una riga che non ci sta più nel foglio va sul prossimo.
     * [background] si disegna dietro ogni riga, [marker] accanto alla prima
     * (il pallino, il numero, la casella), sulla sua linea di base. Con
     * [fixed] ogni riga è alta così e il testo sta nel mezzo, come
     * nell'editor (per i disegni).
     */
    private fun paragraph(
        sheets: Sheets,
        text: String,
        paint: PdfPaint,
        left: Float,
        width: Float,
        fixed: Float? = null,
        background: ((PdfCanvas, Float, Float) -> Unit)? = null,
        marker: ((PdfCanvas, Float) -> Unit)? = null
    ) = paragraph(sheets, plainText(text), paint, left, width, fixed, background, marker)

    private fun paragraph(
        sheets: Sheets,
        text: List<PdfSpan>,
        paint: PdfPaint,
        left: Float,
        width: Float,
        fixed: Float? = null,
        background: ((PdfCanvas, Float, Float) -> Unit)? = null,
        marker: ((PdfCanvas, Float) -> Unit)? = null
    ) {
        val layout = backend.layout(text, paint, max(1, width.toInt()), if (fixed == null) LINE_SPACING else 1f)
        for (i in 0 until layout.lineCount) {
            val lineTop = layout.lineTop(i)
            val natural = layout.lineBottom(i) - lineTop
            val height = fixed ?: natural
            sheets.ensure(height)
            val y = sheets.y
            // Nel mezzo della riga alta [fixed]; di suo, dov'è.
            val shift = if (fixed == null) 0f else {
                val ascent = layout.lineAscent(i)
                val descent = layout.lineDescent(i)
                (fixed - (descent - ascent)) / 2f - ascent - (layout.lineBaseline(i) - lineTop)
            }
            val baseline = y + shift + layout.lineBaseline(i) - lineTop
            val first = i == 0
            sheets.draw { canvas ->
                background?.invoke(canvas, y, y + height)
                canvas.save()
                canvas.clipRect(left - 1f, y, left + width + 1f, y + height)
                canvas.translate(left, y + shift - lineTop)
                layout.drawLine(canvas, i)
                canvas.restore()
                if (first) marker?.invoke(canvas, baseline)
            }
            sheets.y += height
        }
    }

    private fun checkbox(canvas: PdfCanvas, left: Float, baseline: Float, checked: Boolean) {
        val size = bodyPaint.textSize * 0.82f
        val top = baseline - size * 0.92f
        if (checked) {
            canvas.drawRoundRect(left, top, left + size, top + size, 2f * k, fill(CHECKED_COLOR))
            canvas.drawPath(PdfPath().apply {
                moveTo(left + size * 0.22f, top + size * 0.52f)
                lineTo(left + size * 0.42f, top + size * 0.72f)
                lineTo(left + size * 0.78f, top + size * 0.30f)
            }, tickPaint)
        } else {
            canvas.drawRoundRect(left, top, left + size, top + size, 2f * k, boxPaint)
        }
    }

    /** L'immagine larga quanto nella pagina (`BlockAttrs.width`), mai più del foglio; false se il file non si legge. */
    private fun image(sheets: Sheets, file: File, left: Float, width: Float, fraction: Float?): Boolean {
        val (outWidth, outHeight) = backend.imageSize(file) ?: return false
        if (outWidth <= 0 || outHeight <= 0) return false
        var w = width * (fraction?.takeIf { it > 0f }?.coerceAtMost(1f) ?: 1f)
        // Un'immagine piccola (un'icona) non si ingrandisce oltre la sua misura.
        w = min(w, outWidth * 0.75f * k)
        var h = w * outHeight / outWidth
        if (h > contentHeight) {
            h = contentHeight
            w = h * outWidth / outHeight
        }
        sheets.ensure(h)
        val top = sheets.y
        val pixels = (w * IMAGE_PIXELS_PER_POINT).toInt()
        sheets.draw { canvas -> canvas.drawImage(file, left, top, left + w, top + h, pixels) }
        sheets.y += h
        return true
    }

    // --- I disegni a mano ---------------------------------------------------

    /**
     * **I tratti** sopra la pagina, come nell'editor: ognuno attaccato alla
     * cima di quello su cui è stato fatto (la testata, un gruppo di righe,
     * un'isola), in dp moltiplicati per [PdfLayout.dpScale]. Un tratto che
     * scende oltre il foglio continua in cima al foglio dopo. Prima gli
     * evidenziatori, che stanno sotto gli altri.
     */
    private fun drawStrokes(
        sheets: Sheets,
        strokes: List<InkStrokeEntity>,
        headerTop: Spot,
        itemOf: Map<String, String>,
        itemTops: Map<String, Spot>
    ) {
        val s = layout.dpScale ?: return
        for (stroke in strokes.sortedBy { it.tool != InkTool.HIGHLIGHTER }) {
            val origin = if (stroke.anchor == InkStrokeEntity.HEADER_ANCHOR) headerTop else {
                anchorIds(stroke.anchor).firstNotNullOfOrNull { id -> itemOf[id]?.let { itemTops[it] } }
            } ?: continue
            val points = decodeInkPoints(stroke.points)
            if (points.isEmpty()) continue
            val path = inkPath(stroke.tool, stroke.width, points, s)
            val boundsBottom = path.bounds()[3]
            val paint = inkPaint(stroke, s)
            val reach = stroke.width * s
            var sheet = origin.sheet
            var dy = origin.y
            var clipTop = 0f
            while (true) {
                val shift = dy
                val top = clipTop
                sheets.drawOn(sheet) { canvas ->
                    canvas.save()
                    canvas.clipRect(0f, top, layout.pageWidth.toFloat(), bottom)
                    canvas.translate(0f, shift)
                    canvas.drawPath(path, paint)
                    canvas.restore()
                }
                if (dy + boundsBottom + reach <= bottom) break
                dy -= contentHeight
                sheet++
                clipTop = layout.marginTop
            }
        }
    }

    /** Il percorso di un tratto in punti, come `buildInkPath` dell'editor: penna e matita piene e larghe quanto la pressione. */
    private fun inkPath(tool: InkTool, width: Float, points: List<com.gabriele.notionlocal.data.ink.InkPoint>, s: Float): PdfPath {
        val path = PdfPath()
        fun x(i: Int) = points[i].x * s
        fun y(i: Int) = points[i].y * s
        when (tool) {
            InkTool.PEN, InkTool.PENCIL -> {
                val base = width * s / 2f
                val radii = points.map { base * pressureFactor(it.p) }
                for (i in points.indices) addCircle(path, x(i), y(i), radii[i])
                for (i in 1 until points.size) addSegment(path, x(i - 1), y(i - 1), radii[i - 1], x(i), y(i), radii[i])
            }
            InkTool.MARKER, InkTool.HIGHLIGHTER -> {
                path.moveTo(x(0), y(0))
                if (points.size == 1) {
                    path.lineTo(x(0) + 0.01f, y(0))
                } else {
                    for (i in 1 until points.size) path.quadTo(x(i - 1), y(i - 1), (x(i - 1) + x(i)) / 2f, (y(i - 1) + y(i)) / 2f)
                    path.lineTo(x(points.lastIndex), y(points.lastIndex))
                }
            }
        }
        return path
    }

    private fun addCircle(path: PdfPath, cx: Float, cy: Float, r: Float) {
        val sides = (r * 2.4f).toInt().coerceIn(8, 28)
        for (step in 0 until sides) {
            val angle = 2.0 * Math.PI * step / sides
            val px = cx + r * cos(angle).toFloat()
            val py = cy + r * sin(angle).toFloat()
            if (step == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
    }

    private fun addSegment(path: PdfPath, ax: Float, ay: Float, ar: Float, bx: Float, by: Float, br: Float) {
        val length = hypot(bx - ax, by - ay)
        if (length < 0.01f) return
        val nx = -(by - ay) / length
        val ny = (bx - ax) / length
        val xs = floatArrayOf(ax + nx * ar, bx + nx * br, bx - nx * br, ax - nx * ar)
        val ys = floatArrayOf(ay + ny * ar, by + ny * br, by - ny * br, ay - ny * ar)
        var area = 0f
        for (i in 0 until 4) {
            val j = (i + 1) % 4
            area += xs[i] * ys[j] - xs[j] * ys[i]
        }
        val order = if (area >= 0) intArrayOf(0, 1, 2, 3) else intArrayOf(3, 2, 1, 0)
        path.moveTo(xs[order[0]], ys[order[0]])
        for (n in 1 until 4) path.lineTo(xs[order[n]], ys[order[n]])
        path.close()
    }

    /** Il colore del tratto sulla carta: "il colore del testo" è il nero, e un colore troppo chiaro si scurisce. */
    private fun inkPaint(stroke: InkStrokeEntity, s: Float): PdfPaint {
        val chosen = if (stroke.color == InkStrokeEntity.AUTO_COLOR) TEXT_COLOR else stroke.color.toInt()
        val color = if (stroke.tool == InkTool.HIGHLIGHTER) chosen else PdfColors.forPaperText(chosen)
        return when (stroke.tool) {
            InkTool.PEN -> PdfPaint(color = color)
            InkTool.PENCIL -> PdfPaint(color = PdfColors.withAlpha(color, (PdfColors.alpha(color) * 0.8f).toInt()))
            InkTool.MARKER -> PdfPaint(color = color, stroke = stroke.width * s, cap = PdfCap.ROUND, roundJoin = true)
            InkTool.HIGHLIGHTER -> PdfPaint(
                color = PdfColors.withAlpha(color, (255 * 0.38f).toInt()),
                stroke = stroke.width * s,
                cap = PdfCap.SQUARE,
                roundJoin = true
            )
        }
    }

    private fun underlined(prefix: String, text: String): List<PdfSpan> = listOf(PdfSpan(prefix), PdfSpan(text, underline = true))

    companion object {
        /** La larghezza del foglio di un documento: per i test. */
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842

        const val LINE_SPACING = 1.15f

        /** L'emoji che una pagina ha prima che se ne scelga una (`PageRepository.DEFAULT_PAGE_EMOJI`). */
        const val DEFAULT_DATABASE_FALLBACK_EMOJI = "📄"

        /** Le righe dell'editor (`RUN_LINE_HEIGHT`, `CHECKBOX_LINE_HEIGHT`), in dp. */
        const val RUN_LINE_DP = 24f
        const val CHECKBOX_LINE_DP = 27f

        /** I pezzi dei gruppi di righe dell'editor (`PageEditorViewModel.CHUNK_MAX_LINES`, `CHUNK_MAX_CHARS`). */
        const val CHUNK_MAX_LINES = 50
        const val CHUNK_MAX_CHARS = 3_500

        /** Oltre queste colonne un database diventa una scheda per riga. */
        const val MAX_TABLE_COLUMNS = 5

        /** Le immagini a circa 150 punti per pollice: nitide stampate, senza far pesare il file. */
        const val IMAGE_PIXELS_PER_POINT = 2f

        const val TEXT_COLOR = 0xFF1F1F1F.toInt()
        const val MUTED_COLOR = 0xFF6B6B6B.toInt()
        const val LINK_COLOR = 0xFF2563C9.toInt()
        const val CALLOUT_COLOR = 0xFFF1F1EF.toInt()
        const val CODE_COLOR = 0xFFF5F5F4.toInt()
        const val BUTTON_COLOR = 0xFFEEF3FB.toInt()
        const val HEADER_CELL_COLOR = 0xFFF0F0EE.toInt()
        const val CHECKED_COLOR = 0xFF2F6FDE.toInt()

        val FLOWING_TYPES = setOf(BlockType.PARAGRAPH, BlockType.BULLET_LIST_ITEM, BlockType.NUMBERED_LIST_ITEM, BlockType.CHECKBOX)

        fun textPaint(size: Float, bold: Boolean = false, mono: Boolean = false) =
            PdfPaint(color = TEXT_COLOR, textSize = size, bold = bold, mono = mono)

        fun fill(color: Int) = PdfPaint(color = color)
    }
}

/**
 * **Il testo con la formattazione**, per il foglio: grassetto, corsivo,
 * sottolineato, barrato, pedice e apice, la misura e i colori. I colori
 * pensati per il tema scuro dell'app (un testo giallo chiaro, uno sfondo
 * rosso scuro) sul bianco della carta non si leggerebbero: il testo troppo
 * chiaro si scurisce, lo sfondo troppo scuro si schiarisce.
 */
fun styled(spans: List<RichTextSpan>): List<PdfSpan> = spans.filter { it.text.isNotEmpty() }.map { span ->
    PdfSpan(
        text = span.text,
        bold = span.bold,
        italic = span.italic,
        underline = span.underline || span.link != null,
        strike = span.strikethrough,
        sizeFactor = span.size?.let { it / 16f },
        script = when {
            span.subscript -> -1
            span.superscript -> 1
            else -> 0
        },
        color = span.color?.let(PdfColors::parse)?.let(PdfColors::forPaperText) ?: if (span.link != null) PdfExport.LINK_COLOR else null,
        background = span.background?.let(PdfColors::parse)?.let(PdfColors::forPaperBackground)
    )
}

/** Tutto il testo di [color], tranne i pezzi che hanno un colore loro. */
private fun List<PdfSpan>.withColor(color: Int): List<PdfSpan> = map { it.copy(color = it.color ?: color) }
