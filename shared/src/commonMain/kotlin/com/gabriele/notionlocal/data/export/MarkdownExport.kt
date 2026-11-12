package com.gabriele.notionlocal.data.export

import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.data.entity.isInternalLink
import com.gabriele.notionlocal.ui.i18n.ButtonStrings
import java.text.DateFormat
import java.util.Date

// ---------------------------------------------------------------------------
// **La pagina in Markdown** (dal 02/11/2026): testo semplice con i segni (`**`
// per il grassetto, `- ` per gli elenchi, `- [ ]` per le caselle…) che leggono
// Obsidian, GitHub e le altre app di note. È il formato per "non restare
// chiuso dentro l'app": il testo resta tuo anche senza notE.
// ---------------------------------------------------------------------------

/**
 * Il Markdown di [page], col titolo in cima.
 *
 * [fileLink] dà, per il file di un blocco (`BlockAttrs.file`), dove sta
 * accanto al Markdown (nel .zip); null se non c'è, e allora resta il nome.
 * [pageLink] lo stesso per le sottopagine esportate insieme.
 */
fun markdownOf(
    page: ExportPage,
    untitled: String,
    fileLink: (String) -> String? = { null },
    pageLink: (String) -> String? = { null },
    /** Per id del blocco di un database: dove stanno il suo CSV e le pagine delle righe. */
    databaseLinks: (String) -> MarkdownDatabaseLinks? = { null },
    dateText: (Long) -> String = ::defaultDateText
): String {
    val out = StringBuilder()
    val title = page.page.title.ifBlank { untitled }
    out.append("# ").append(escapeMarkdown(title))
    // Il rientro dell'ultimo elemento d'elenco scritto, -1 fuori dagli elenchi:
    // in Markdown un rientro senza un elenco sopra diventa un blocco di codice,
    // quindi una riga non rientra mai più di uno oltre l'elenco che la contiene.
    var openList = -1
    var previousWasList = false
    for (line in exportLines(page)) {
        val type = line.block.type
        val isList = type in LIST_TYPES
        val depth = if (openList < 0) 0 else minOf(line.depth, openList + 1)
        val chunk = blockMarkdown(line, depth, page, untitled, fileLink, pageLink, databaseLinks, dateText)
        if (chunk == null) {
            // Una riga vuota fra due elementi li separa anche qui.
            previousWasList = false
            continue
        }
        out.append(if (isList && previousWasList) "\n" else "\n\n")
        out.append(if (isList) chunk else indentLines(chunk, "    ".repeat(depth)))
        openList = when {
            isList -> depth
            depth == 0 -> -1
            else -> depth - 1
        }
        previousWasList = isList
    }
    return out.append('\n').toString()
}

private val LIST_TYPES = setOf(BlockType.BULLET_LIST_ITEM, BlockType.NUMBERED_LIST_ITEM, BlockType.CHECKBOX, BlockType.TOGGLE)

/** Dove stanno, accanto al Markdown, il CSV di un database e le pagine delle sue righe (null dove una riga non ne ha). */
data class MarkdownDatabaseLinks(val csv: String?, val rows: List<String?>)

private fun blockMarkdown(
    line: ExportLine,
    depth: Int,
    page: ExportPage,
    untitled: String,
    fileLink: (String) -> String?,
    pageLink: (String) -> String?,
    databaseLinks: (String) -> MarkdownDatabaseLinks?,
    dateText: (Long) -> String
): String? {
    val block = line.block
    val titles = page.linkedTitles
    val indent = "    ".repeat(depth)
    fun listItem(marker: String): String =
        indent + marker + hardBreaks(inlineMarkdown(line.spans), indent + " ".repeat(marker.length))
    fun titleOf(id: String?): String = id?.let { titles[it] }?.ifBlank { untitled } ?: line.text.ifBlank { untitled }
    return when (block.type) {
        BlockType.PARAGRAPH -> {
            if (line.text.isBlank()) return null
            val level = headingLevel(line.spans)
            if (level > 0) {
                "#".repeat(level) + " " + inlineMarkdown(line.spans.map { it.copy(bold = false) }).replace("\n", " ")
            } else {
                hardBreaks(escapeLineStart(inlineMarkdown(line.spans)), "")
            }
        }
        BlockType.BULLET_LIST_ITEM, BlockType.TOGGLE -> listItem("- ")
        BlockType.NUMBERED_LIST_ITEM -> listItem("${line.number}. ")
        BlockType.CHECKBOX -> listItem(if (block.isChecked) "- [x] " else "- [ ] ")
        BlockType.QUOTE -> quoted(inlineMarkdown(line.spans))
        BlockType.CALLOUT -> quoted((block.attrs.icon ?: "💡") + " " + inlineMarkdown(line.spans))
        BlockType.CODE -> {
            val code = line.text
            val longest = Regex("`+").findAll(code).maxOfOrNull { it.value.length } ?: 0
            val fence = "`".repeat(maxOf(3, longest + 1))
            fence + block.attrs.language.orEmpty() + "\n" + code + "\n" + fence
        }
        BlockType.NOTIFICATION -> {
            val text = inlineMarkdown(line.spans)
            val at = block.notifyAt?.let(dateText)
            "🔔 " + listOfNotNull(text.ifBlank { null }, at).joinToString(" · ")
        }
        BlockType.BUTTON -> page.buttons[block.id]?.let(::buttonMarkdown)
        // Cornici: le loro righe si esportano al loro posto (vedi `exportLines`).
        BlockType.SYNCED, BlockType.COLUMN_LIST, BlockType.COLUMN -> null
        BlockType.DIVIDER -> "---"
        BlockType.PAGE_LINK, BlockType.PAGE_MENTION -> {
            val id = if (block.type == BlockType.PAGE_MENTION) block.attrs.pageId ?: block.linkedPageId else block.linkedPageId
            val name = escapeMarkdown(titleOf(id))
            val target = id?.let(pageLink)
            "📄 " + if (target != null) "[$name](${linkTarget(target)})" else name
        }
        BlockType.DATABASE_LINK -> page.databases[block.id]
            ?.let { databaseMarkdown(it, databaseLinks(block.id), withTitle = block.linkedPageId != page.page.id) }
            ?: ("▦ " + escapeMarkdown(titleOf(block.linkedPageId)))
        BlockType.IMAGE, BlockType.VIDEO, BlockType.AUDIO, BlockType.FILE, BlockType.BOOKMARK -> {
            val attrs = block.attrs
            val name = (attrs.name ?: line.text.ifBlank { null } ?: attrs.url ?: attrs.file ?: "").trim()
            val target = attrs.file?.let(fileLink) ?: attrs.url
            val icon = when (block.type) {
                BlockType.IMAGE -> "🖼"
                BlockType.VIDEO -> "🎬"
                BlockType.AUDIO -> "🔊"
                BlockType.FILE -> "📎"
                else -> "🔖"
            }
            when {
                target == null -> "$icon ${escapeMarkdown(name)}"
                block.type == BlockType.IMAGE -> "![${escapeMarkdown(name)}](${linkTarget(target)})"
                else -> "$icon [${escapeMarkdown(name)}](${linkTarget(target)})" +
                    (attrs.description?.takeIf { block.type == BlockType.BOOKMARK && it.isNotBlank() }?.let { "  \n" + escapeMarkdown(it.trim()) } ?: "")
            }
        }
        BlockType.TABLE -> {
            val byCell = line.cells.associateBy { it.rowIndex to it.colIndex }
            val cols = block.tableCols.coerceAtLeast(1)
            val rows = (0 until block.tableRows.coerceAtLeast(1)).map { row ->
                (0 until cols).map { col -> tableCell(byCell[row to col]?.text.orEmpty()) }
            }
            // In Markdown la prima riga di una tabella è sempre l'intestazione.
            val lines = ArrayList<String>()
            rows.forEachIndexed { index, cells ->
                lines += "| " + cells.joinToString(" | ") + " |"
                if (index == 0) lines += "|" + " --- |".repeat(cols)
            }
            lines.joinToString("\n")
        }
    }
}

/**
 * **Un database** come una tabella di Markdown, col suo nome sopra (non per un
 * database esportato da solo, che il nome l'ha già in cima) e il link al CSV.
 * Il nome di una riga che ha una pagina porta alla sua pagina.
 */
private fun databaseMarkdown(database: ExportDatabase, links: MarkdownDatabaseLinks?, withTitle: Boolean): String {
    val parts = ArrayList<String>()
    if (withTitle) parts += "### " + escapeMarkdown(database.title.ifBlank { "▦" })
    if (database.headers.isNotEmpty()) {
        val lines = ArrayList<String>()
        lines += "| " + database.headers.joinToString(" | ") { tableCell(it) } + " |"
        lines += "|" + " --- |".repeat(database.headers.size)
        database.rows.forEachIndexed { index, row ->
            val target = links?.rows?.getOrNull(index)
            val cells = row.cells.mapIndexed { column, value ->
                val text = tableCell(value)
                if (target != null && column == database.nameIndex) "[$text](${linkTarget(target)})" else text
            }
            lines += "| " + cells.joinToString(" | ") + " |"
        }
        parts += lines.joinToString("\n")
    }
    links?.csv?.let { parts += "[CSV](${linkTarget(it)})" }
    return parts.joinToString("\n\n")
}

/** **Un bottone** con le sue azioni, in una citazione: "🔘 **Nome** — Quando lo tocchi:" e l'elenco numerato. */
private fun buttonMarkdown(button: ExportButton): String =
    (listOf("🔘 **${escapeMarkdown(button.name)}** — ${ButtonStrings.whenTapped}:") + button.steps.map { escapeMarkdown(it) })
        .joinToString("\n") { "> $it" }

/**
 * **Il testo con la formattazione**, in segni Markdown. I segni si aprono e
 * si chiudono come scatole una dentro l'altra (il link fuori, poi grassetto,
 * corsivo, barrato…): così "**grassetto *e corsivo***" resta leggibile da
 * tutte le app. Gli spazi stanno sempre fuori dai segni, perché "** a**" in
 * Markdown non è grassetto.
 */
fun inlineMarkdown(spans: List<RichTextSpan>): String {
    val out = StringBuilder()
    val open = ArrayList<Mark>()
    var pendingSpace = ""
    for (span in spans) {
        val text = span.text
        if (text.isEmpty()) continue
        val start = text.indexOfFirst { !it.isWhitespace() }
        if (start < 0) {
            pendingSpace += text
            continue
        }
        val end = text.indexOfLast { !it.isWhitespace() } + 1
        val wanted = marksOf(span)
        var keep = 0
        while (keep < open.size && keep < wanted.size && open[keep] == wanted[keep]) keep++
        for (i in open.lastIndex downTo keep) out.append(open[i].close)
        while (open.size > keep) open.removeAt(open.lastIndex)
        out.append(pendingSpace).append(text, 0, start)
        for (i in keep until wanted.size) {
            out.append(wanted[i].open)
            open += wanted[i]
        }
        out.append(escapeMarkdown(text.substring(start, end)))
        pendingSpace = text.substring(end)
    }
    for (i in open.lastIndex downTo 0) out.append(open[i].close)
    return out.append(pendingSpace).toString().trimEnd()
}

private data class Mark(val open: String, val close: String)

private fun marksOf(span: RichTextSpan): List<Mark> = buildList {
    // I collegamenti dentro l'app (menzioni, righe della pagina) fuori non portano da nessuna parte: resta il testo.
    span.link?.takeIf { !isInternalLink(it) }?.let { add(Mark("[", "](${linkTarget(it)})")) }
    if (span.bold) add(Mark("**", "**"))
    if (span.italic) add(Mark("*", "*"))
    if (span.strikethrough) add(Mark("~~", "~~"))
    if (span.underline) add(Mark("<u>", "</u>"))
    if (span.subscript) add(Mark("<sub>", "</sub>"))
    if (span.superscript) add(Mark("<sup>", "</sup>"))
}

private val MARKDOWN_SPECIAL = Regex("""([\\`*_\[\]<~])""")

/** Il testo con una barra davanti ai caratteri che in Markdown sarebbero segni. */
fun escapeMarkdown(text: String): String = text.replace(MARKDOWN_SPECIAL, """\\$1""")

private val LINE_START_MARK = Regex("""^([#>+=-]|\d+(?=[.)](\s|$)))""")

/** Una riga che comincia come un titolo, una citazione o un elenco ("1. ") resta testo. */
private fun escapeLineStart(text: String): String =
    LINE_START_MARK.find(text)?.let { match ->
        if (match.value[0].isDigit()) {
            text.substring(0, match.range.last + 1) + "\\" + text.substring(match.range.last + 1)
        } else {
            "\\" + text
        }
    } ?: text

/** Un a-capo dentro una riga resta un a-capo (due spazi in fondo), e le righe sotto stanno al loro posto. */
private fun hardBreaks(text: String, continuation: String): String =
    text.split('\n').joinToString("  \n$continuation")

private fun quoted(text: String): String = text.split('\n').joinToString("  \n") { "> $it" }

private fun indentLines(text: String, indent: String): String =
    if (indent.isEmpty()) text else text.split('\n').joinToString("\n") { if (it.isEmpty()) it else indent + it }

/** Un titolo della pagina (le righe scritte grandi, come i titoli presi dal web): 24 punti o più "##", 20 o più "###". */
private fun headingLevel(spans: List<RichTextSpan>): Int {
    val written = spans.filter { it.text.isNotBlank() }
    if (written.isEmpty()) return 0
    val smallest = written.minOf { it.size ?: 16 }
    return when {
        smallest >= 24 -> 2
        smallest >= 20 -> 3
        else -> 0
    }
}

private fun tableCell(text: String): String =
    escapeMarkdown(text.trim()).replace("|", "\\|").replace("\n", "<br>")

/** Un indirizzo o un percorso fra parentesi: gli spazi e le parentesi si scrivono in codice. */
private fun linkTarget(target: String): String =
    (if (target.contains("://")) target else target.replace("%", "%25"))
        .replace(" ", "%20").replace("(", "%28").replace(")", "%29")

private fun defaultDateText(at: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(at))
