package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.CopiedLine
import com.gabriele.notionlocal.data.entity.LinkRange
import com.gabriele.notionlocal.data.entity.PasteFormat
import com.gabriele.notionlocal.data.entity.PastedLine
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.StyleRange
import androidx.compose.ui.platform.Clipboard

// ---------------------------------------------------------------------------
// **Incollare coi link** (dal 17/10/2026, chiesto dall'utente): il testo
// copiato da un'altra app — OneNote, un browser — arriva coi suoi
// collegamenti, sulle stesse parole.
//
// Le altre app mettono negli appunti il testo semplice e, accanto, lo stesso
// testo in HTML (o un testo coi link dentro). L'incolla di Compose prende
// solo il testo semplice: `LinkClipboard` gli dà lo stesso testo con i link
// segnati sopra (`PASTED_LINK_TAG`), che arrivano nel valore del campo
// (`pastedFormat`) e diventano link veri nel testo ricco (`applyPaste`).
//
// **I tre modi di incollare** (dal 18/10/2026, chiesti dall'utente, come in
// Word), dal pulsante "incolla" della barra sopra la tastiera:
//  - Keep Source Formatting: il testo incollato perde la formattazione di
//    dove arriva e prende quella di chi l'ha copiato — grassetto, corsivo,
//    sottolineato, barrato, link;
//  - Match Destination Formatting: testo semplice, che prende la
//    formattazione del punto in cui arriva;
//  - Keep Text Only: testo e basta, senza nessuna formattazione.
// Il "Paste" del menu che si apre tenendo premuto resta come prima: i link
// sì, il resto prende la formattazione del punto in cui arriva.
//
// **Copiando** da notE (sia con la barra sia col menu) negli appunti va anche
// l'HTML del testo con la sua formattazione, così si incolla altrove — o qui,
// con "Keep Source Formatting" — com'era.
//
// **E il tipo delle righe** (dal 19/10/2026, chiesto dall'utente): copiate
// intere, le caselle da spuntare (spuntate o no), gli elenchi puntati e
// numerati e il rientro vanno negli appunti in un commento dell'HTML
// (`<!--notE-lines:…-->`), che le altre app non vedono. Incollando in notE
// tornano sulle righe (`PastedLine`), tranne con "Keep Text Only".
// ---------------------------------------------------------------------------

internal const val PASTED_LINK_TAG = "pasted-link"
internal const val PASTED_STYLE_TAG = "pasted-style"
internal const val PASTED_PLAIN_TAG = "pasted-plain"
internal const val PASTED_LINES_TAG = "pasted-lines"
internal const val PASTED_TABLE_TAG = "pasted-table"

/** Fra le righe e fra le celle di una tabella incollata, nell'etichetta: caratteri che nel testo non ci sono. */
internal const val TABLE_ROW_SEPARATOR = '\u001E'
internal const val TABLE_CELL_SEPARATOR = '\u001F'

/** Il commento con il tipo delle righe copiate, nell'HTML degli appunti. */
internal const val LINES_MARKER = "PageFlow-lines:"

/** Il segno di quando l'app si chiamava notE (fino al 03/11/2026): una copia di allora si incolla uguale. */
internal const val OLD_LINES_MARKER = "notE-lines:"

/** Come si incolla (vedi sopra). */
enum class PasteMode { LINKS_ONLY, KEEP_SOURCE, MATCH_DESTINATION, TEXT_ONLY }

/**
 * Il commento col tipo delle righe: `TIPO,spuntata,rientro` per riga, `x` per
 * una riga presa in parte (non `-`: accanto alla chiusura `-->` del commento
 * la chiuderebbe un carattere prima).
 */
fun linesMarker(lines: List<CopiedLine?>): String =
    "<!--" + LINES_MARKER + lines.joinToString(";") { line ->
        if (line == null) "x" else "${line.type.name},${if (line.checked) 1 else 0},${line.indent}"
    } + "-->"

/** Le voci del commento col tipo delle righe, se l'HTML ce l'ha. */
internal fun linesMarkerOf(html: String?): String? {
    if (html == null) return null
    val marker = listOf(LINES_MARKER, OLD_LINES_MARKER).firstOrNull { html.contains("<!--$it") } ?: return null
    val start = html.indexOf("<!--$marker")
    val from = start + 4 + marker.length
    val end = html.indexOf("-->", from)
    return if (end < 0) null else html.substring(from, end)
}

/** Una voce del commento: la riga col suo tipo, o null. */
internal fun copiedLineOf(entry: String): CopiedLine? {
    val parts = entry.split(',')
    if (parts.size != 3) return null
    val type = BlockType.entries.find { it.name == parts[0] } ?: return null
    return CopiedLine(type, parts[1] == "1", parts[2].toIntOrNull()?.coerceIn(0, 10) ?: 0)
}

internal fun RichTextSpan.hasStyle(): Boolean = bold || italic || underline || strikethrough || link != null

internal fun styleCode(style: StyleRange): String = buildString {
    if (style.bold) append('b')
    if (style.italic) append('i')
    if (style.underline) append('u')
    if (style.strikethrough) append('s')
    style.size?.let { append('z').append(it) }
}

/** La grandezza nel codice di uno stile ("bz24"), o null. */
internal fun sizeOfCode(code: String): Int? = Regex("z(\\d+)").find(code)?.groupValues?.get(1)?.toIntOrNull()

/** Le celle di una tabella nell'etichetta dell'incolla, e ritorno. */
internal fun encodeTable(cells: List<List<String>>): String =
    cells.joinToString(TABLE_ROW_SEPARATOR.toString()) { row ->
        row.joinToString(TABLE_CELL_SEPARATOR.toString()) { it.filterNot { c -> c == TABLE_ROW_SEPARATOR || c == TABLE_CELL_SEPARATOR } }
    }

internal fun decodeTable(code: String): List<List<String>> =
    code.split(TABLE_ROW_SEPARATOR).map { it.split(TABLE_CELL_SEPARATOR) }

/**
 * **Le parole dei link ritrovate nel testo semplice**, una dopo l'altra:
 * ognuna si cerca da dove finisce la precedente, così due link con la stessa
 * parola vanno ognuno al suo posto. Gli spazi speciali dell'HTML (quello che
 * non va a capo) valgono come spazi normali.
 */
fun matchAnchors(anchors: List<Pair<String, String>>, plain: String): List<LinkRange> {
    val haystack = plain.replace('\u00A0', ' ')
    var from = 0
    return anchors.mapNotNull { (anchor, url) ->
        val needle = anchor.replace('\u00A0', ' ').trim()
        if (needle.isEmpty()) return@mapNotNull null
        val at = haystack.indexOf(needle, from)
        if (at < 0) return@mapNotNull null
        from = at + needle.length
        LinkRange(at, at + needle.length, url.trim())
    }
}

/** Un pezzo di testo dell'HTML con uno stile. */
data class StyledRun(val text: String, val bold: Boolean, val italic: Boolean, val underline: Boolean, val strikethrough: Boolean)

/** Gli stessi pezzi ritrovati nel testo semplice, in ordine (come `matchAnchors`). */
fun matchStyles(runs: List<StyledRun>, plain: String): List<StyleRange> {
    val haystack = plain.replace('\u00A0', ' ')
    var from = 0
    return runs.mapNotNull { run ->
        val needle = run.text.replace('\u00A0', ' ')
        if (needle.isBlank()) return@mapNotNull null
        val at = haystack.indexOf(needle, from)
        if (at < 0) return@mapNotNull null
        from = at + needle.length
        StyleRange(at, at + needle.length, run.bold, run.italic, run.underline, run.strikethrough)
    }
}

/**
 * **Gli stili dell'HTML degli appunti**, pezzo per pezzo: `<b>`, `<strong>`,
 * `<i>`, `<em>`, `<u>`, `<s>`, `<strike>`, `<del>`, e gli stessi scritti come
 * stile di uno `<span>` — è così che copiano OneNote, Word e Google Docs
 * ("font-weight:bold", "font-style:italic", "text-decoration:underline").
 * Il resto dell'HTML conta solo per il testo.
 */
fun htmlStyleRuns(html: String): List<StyledRun> {
    // Il grassetto lo decide il più interno che ne dice qualcosa (null: non ne parla).
    data class Open(val tag: String, val bold: Boolean?, val italic: Boolean, val underline: Boolean, val strike: Boolean)
    val stack = mutableListOf<Open>()
    val runs = mutableListOf<StyledRun>()
    // Il frammento copiato, se c'è: il resto (la testa del documento) non è testo.
    val body = Regex("<!--StartFragment-->(.*?)<!--EndFragment-->", RegexOption.DOT_MATCHES_ALL).find(html)?.groupValues?.get(1)
        ?: Regex("<body[^>]*>(.*)</body>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)).find(html)?.groupValues?.get(1)
        ?: html
    val cleaned = body
        .replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("<(style|script)[^>]*>.*?</\\1>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
    val token = Regex("<(/?)([a-zA-Z0-9]+)([^>]*)>|([^<]+)")
    for (match in token.findAll(cleaned)) {
        val text = match.groupValues[4]
        if (text.isNotEmpty()) {
            val decoded = decodeEntities(text).replace(Regex("\\s+"), " ")
            val bold = stack.lastOrNull { it.bold != null }?.bold == true
            val italic = stack.any { it.italic }
            val underline = stack.any { it.underline }
            val strike = stack.any { it.strike }
            if ((bold || italic || underline || strike) && decoded.isNotBlank()) {
                runs += StyledRun(decoded.trim(), bold, italic, underline, strike)
            }
            continue
        }
        val closing = match.groupValues[1] == "/"
        val tag = match.groupValues[2].lowercase()
        val attrs = match.groupValues[3]
        if (tag in setOf("br", "img", "hr", "meta", "link") || attrs.trimEnd().endsWith("/")) continue
        if (closing) {
            val at = stack.indexOfLast { it.tag == tag }
            if (at >= 0) while (stack.size > at) stack.removeAt(stack.lastIndex)
            continue
        }
        val style = Regex("style\\s*=\\s*(\"[^\"]*\"|'[^']*')", RegexOption.IGNORE_CASE).find(attrs)?.groupValues?.get(1)?.lowercase().orEmpty()
        val weight = Regex("font-weight\\s*:\\s*([a-z0-9]+)").find(style)?.groupValues?.get(1)
        val boldStyle = weight == "bold" || weight == "bolder" || (weight?.toIntOrNull() ?: 0) >= 600
        val italicStyle = Regex("font-style\\s*:\\s*italic").containsMatchIn(style)
        val decoration = Regex("text-decoration(-line)?\\s*:\\s*([^;\"']*)").find(style)?.groupValues?.get(2).orEmpty()
        // "font-weight:normal" spegne il grassetto di chi lo contiene: è il
        // caso di Google Docs, che avvolge tutto in un <b> "normale".
        val normal = weight == "normal" || weight == "400" || weight == "lighter"
        stack += Open(
            tag = tag,
            bold = when {
                normal -> false
                tag == "b" || tag == "strong" || boldStyle -> true
                else -> null
            },
            italic = tag == "i" || tag == "em" || italicStyle,
            underline = tag == "u" || "underline" in decoration,
            strike = tag == "s" || tag == "strike" || tag == "del" || "line-through" in decoration
        )
    }
    return runs
}

/** Il testo con la sua formattazione in HTML, per gli appunti: grassetto, corsivo, sottolineato, barrato, link. */
fun spansToHtml(spans: List<RichTextSpan>): String = buildString {
    spans.forEach { span ->
        var piece = span.text
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
            .replace("\n", "<br>")
        if (span.bold) piece = "<b>$piece</b>"
        if (span.italic) piece = "<i>$piece</i>"
        if (span.underline) piece = "<u>$piece</u>"
        if (span.strikethrough) piece = "<s>$piece</s>"
        span.link?.let { piece = "<a href=\"${it.replace("\"", "&quot;")}\">$piece</a>" }
        append(piece)
    }
}

/**
 * Quello che un incolla ha portato nel valore del campo — link, stili, pezzi
 * da ripulire — nelle posizioni del testo vero: senza i primi [lead]
 * caratteri (l'a-capo nascosto in testa).
 */
fun TextFieldValue.pastedFormat(lead: Int): PasteFormat {
    val all = annotatedString
    val links = all.getStringAnnotations(PASTED_LINK_TAG, 0, text.length)
        .map { LinkRange(it.start - lead, it.end - lead, it.item) }
        .filter { it.start >= 0 && it.end > it.start }
    val styles = all.getStringAnnotations(PASTED_STYLE_TAG, 0, text.length)
        .map { StyleRange(it.start - lead, it.end - lead, 'b' in it.item, 'i' in it.item, 'u' in it.item, 's' in it.item, sizeOfCode(it.item)) }
        .filter { it.start >= 0 && it.end > it.start }
    // Le celle delle tabelle incollate, per dove comincia la loro riga.
    val tables = all.getStringAnnotations(PASTED_TABLE_TAG, 0, text.length).associate { (it.start - lead) to decodeTable(it.item) }
    val plain = all.getStringAnnotations(PASTED_PLAIN_TAG, 0, text.length)
        .map { (it.start - lead) until (it.end - lead) }
        .filter { it.first >= 0 && !it.isEmpty() }
    // Le righe col loro tipo: si ritrovano dividendo il testo incollato agli a-capo.
    val lines = all.getStringAnnotations(PASTED_LINES_TAG, 0, text.length).flatMap { mark ->
        val entries = mark.item.split(';')
        val pieces = text.substring(mark.start, mark.end).split('\n')
        if (entries.size != pieces.size) return@flatMap emptyList()
        var at = mark.start - lead
        pieces.indices.mapNotNull { i ->
            val start = at
            at += pieces[i].length + 1
            copiedLineOf(entries[i])?.takeIf { start >= 0 }?.let {
                PastedLine(start, start + pieces[i].length, it.type, it.checked, it.indent, tables[start])
            }
        }
    }.filter { it.type != BlockType.TABLE || it.table != null }
    return PasteFormat(links, styles, plain, lines)
}

/** Solo i link incollati (vedi `pastedFormat`). */
fun TextFieldValue.pastedLinks(lead: Int): List<LinkRange> = pastedFormat(lead).links

/**
 * Lo stesso valore senza i segni dell'incolla: rimasti nel campo,
 * tornerebbero con ogni incolla successivo. Senza segni resta com'è (per
 * non disturbare la tastiera mentre si scrive).
 */
fun TextFieldValue.withoutPastedLinks(): TextFieldValue {
    val tags = setOf(PASTED_LINK_TAG, PASTED_STYLE_TAG, PASTED_PLAIN_TAG, PASTED_LINES_TAG, PASTED_TABLE_TAG)
    val marked = annotatedString.getStringAnnotations(0, text.length).any { it.tag in tags }
    return if (marked) copy(annotatedString = AnnotatedString(text)) else this
}

/**
 * **Gli appunti dell'editor** (vedi sopra): come quelli di sempre, ma il testo
 * incollato porta i suoi link e, a richiesta, il suo stile. Sul telefono
 * leggono gli appunti di Android, col loro HTML; sul PC, dal 10/11/2026,
 * quelli di Windows, col loro HTML (`PasteLinks.desktop.kt`).
 */
expect class LinkClipboard : ClipboardManager, Clipboard {
    /** Il modo del prossimo incolla; poi si torna a quello di sempre. */
    var nextPasteMode: PasteMode

    /**
     * **L'incolla dei campi di Compose 1.8 e dopo**: se [after] è [before] col
     * testo degli appunti al posto della selezione, quel testo arriva coi
     * suoi link, stili e tipo delle righe. Altrimenti [after] resta com'è.
     */
    fun formatPaste(before: TextFieldValue, after: TextFieldValue): TextFieldValue
}

/**
 * Gli appunti coi link per i campi dell'editor; [richSelection] dà lo stile
 * della selezione e [selectedLines] il tipo delle sue righe, per copiare.
 */
@Composable
expect fun rememberLinkClipboard(
    richSelection: () -> List<RichTextSpan>? = { null },
    selectedLines: () -> List<CopiedLine?>? = { null }
): LinkClipboard

/** Le scorciatoie dei pulsanti della barra sopra la tastiera: Ctrl+C, Ctrl+V, Ctrl+X. */
enum class EditShortcut { COPY, PASTE, CUT }

/**
 * **Ctrl + un tasto**, mandato al campo che ha il fuoco come da una tastiera
 * vera (sul telefono `sendCtrlShortcut`); sul PC la barra sopra la tastiera
 * non c'è, e per ora non fa niente.
 */
@Composable
expect fun rememberEditShortcuts(): (EditShortcut) -> Unit
