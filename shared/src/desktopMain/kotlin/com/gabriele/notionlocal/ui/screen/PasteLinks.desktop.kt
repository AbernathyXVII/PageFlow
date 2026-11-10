package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.CopiedLine
import com.gabriele.notionlocal.data.entity.LinkRange
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.StyleRange
import com.gabriele.notionlocal.data.entity.plainText
import com.gabriele.notionlocal.platform.HtmlSelection
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable

/**
 * **Gli appunti coi link sul PC** (dal 10/11/2026), come sul telefono
 * (`PasteLinks.android.kt`): incollando, il testo copiato da un browser, da
 * Office o da PageFlow arriva coi suoi link (e, con "Keep Source Formatting",
 * stili, elenchi e tabelle), letti dall'HTML degli appunti di Windows con le
 * stesse funzioni del telefono (`parseClipHtml`, `htmlStyleRuns`,
 * `matchAnchors`). Copiando da PageFlow, negli appunti va anche l'HTML col
 * suo stile e il tipo delle righe. [system] sono gli appunti di Windows (nei
 * test, degli appunti finti).
 */
@OptIn(ExperimentalComposeUiApi::class)
actual class LinkClipboard(
    private val base: ClipboardManager,
    private val clipboard: Clipboard,
    private val richSelection: () -> List<RichTextSpan>? = { null },
    private val selectedLines: () -> List<CopiedLine?>? = { null },
    private val system: java.awt.datatransfer.Clipboard = Toolkit.getDefaultToolkit().systemClipboard
) : ClipboardManager by base, Clipboard by clipboard {
    actual var nextPasteMode: PasteMode = PasteMode.LINKS_ONLY

    override val nativeClipboard: NativeClipboard get() = clipboard.nativeClipboard

    private fun read(flavor: DataFlavor): String? = runCatching {
        system.getContents(null)?.takeIf { it.isDataFlavorSupported(flavor) }?.getTransferData(flavor) as? String
    }.getOrNull()

    private fun clipHtml(): String? = read(DataFlavor.allHtmlFlavor) ?: read(HTML_FLAVOR)

    /** Il testo che il campo incolla: quello degli appunti o, se c'è solo l'HTML, il suo testo. */
    private fun clipText(): String? = read(DataFlavor.stringFlavor) ?: clipHtml()?.let { html -> parseClipHtml(html).joinToString("\n") { it.text }.trimEnd() }

    override suspend fun getClipEntry(): ClipEntry? {
        val text = clipText() ?: return null
        return ClipEntry(StringSelection(text))
    }

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        val text = runCatching { (clipEntry?.nativeClipEntry as? Transferable)?.getTransferData(DataFlavor.stringFlavor) as? String }.getOrNull()
        if (text != null) {
            setText(AnnotatedString(text))
            return
        }
        clipboard.setClipEntry(clipEntry)
    }

    actual fun formatPaste(before: TextFieldValue, after: TextFieldValue): TextFieldValue {
        val clip = clipText()?.takeIf { it.isNotEmpty() } ?: return after
        val start = before.selection.min.coerceIn(0, before.text.length)
        val end = before.selection.max.coerceIn(start, before.text.length)
        if (after.text != before.text.substring(0, start) + clip + before.text.substring(end)) return after
        val pasted = getText() ?: return after
        val text = AnnotatedString.Builder().apply {
            append(before.annotatedString.subSequence(0, start))
            append(pasted)
            append(before.annotatedString.subSequence(end, before.text.length))
        }.toAnnotatedString()
        return TextFieldValue(text, TextRange(start + pasted.length))
    }

    override fun getText(): AnnotatedString? {
        val mode = nextPasteMode
        nextPasteMode = PasteMode.LINKS_ONLY
        val html = clipHtml()
        // Com'era fatto nella fonte: da un'altra app con l'HTML (da PageFlow il tipo delle righe c'è già, nel commento).
        if (mode == PasteMode.KEEP_SOURCE && html != null && linesMarkerOf(html) == null) {
            val lines = runCatching { parseClipHtml(html) }.getOrNull()
            if (!lines.isNullOrEmpty()) return sourceFormatted(lines)
        }
        val plain = clipText() ?: return base.getText()
        val builder = AnnotatedString.Builder(plain)
        if (mode == PasteMode.KEEP_SOURCE || mode == PasteMode.TEXT_ONLY) {
            builder.addStringAnnotation(PASTED_PLAIN_TAG, "", 0, plain.length)
        }
        if (html != null && (mode == PasteMode.KEEP_SOURCE || mode == PasteMode.LINKS_ONLY)) {
            runCatching { linksOf(html, plain) }.getOrDefault(emptyList())
                .forEach { builder.addStringAnnotation(PASTED_LINK_TAG, it.url, it.start, it.end) }
        }
        if (html != null && mode == PasteMode.KEEP_SOURCE) {
            runCatching { matchStyles(htmlStyleRuns(html), plain) }.getOrDefault(emptyList()).forEach {
                builder.addStringAnnotation(PASTED_STYLE_TAG, styleCode(it), it.start, it.end)
            }
        }
        if (mode != PasteMode.TEXT_ONLY) {
            linesMarkerOf(html)?.takeIf { it.split(';').size == plain.split('\n').size }?.let {
                builder.addStringAnnotation(PASTED_LINES_TAG, it, 0, plain.length)
            }
        }
        return builder.toAnnotatedString()
    }

    /** Copiando: col testo semplice anche l'HTML con la formattazione e il tipo delle righe, se ce n'è. */
    override fun setText(annotatedString: AnnotatedString) {
        val text = annotatedString.text
        val selection = runCatching { richSelection() }.getOrNull()?.takeIf { it.plainText() == text }
        val rich = selection?.takeIf { spans -> spans.any { it.hasStyle() } }
        val lines = selection?.let { runCatching { selectedLines() }.getOrNull() }
            ?.takeIf { lines -> lines.size == text.split('\n').size && lines.any { it != null && (it.type != BlockType.PARAGRAPH || it.indent > 0) } }
        if (rich == null && lines == null) {
            system.setContents(StringSelection(text), null)
            return
        }
        val html = spansToHtml(rich ?: listOf(RichTextSpan(text = text))) + (lines?.let { linesMarker(it) } ?: "")
        system.setContents(HtmlSelection(text, html), null)
    }

    /** Il testo com'era nella fonte (come sul telefono, `sourceFormatted`). */
    private fun sourceFormatted(lines: List<ClipLine>): AnnotatedString {
        val sizeOf = noteSizes(lines)
        val plain = lines.joinToString("\n") { it.text }
        val builder = AnnotatedString.Builder(plain)
        if (plain.isNotEmpty()) builder.addStringAnnotation(PASTED_PLAIN_TAG, "", 0, plain.length)
        var lineStart = 0
        for (line in lines) {
            var at = lineStart
            for (run in line.runs) {
                val end = at + run.text.length
                if (end > at && line.table == null) {
                    val style = StyleRange(at, end, run.bold, run.italic, run.underline, run.strikethrough, sizeOf(run.sizePt))
                    if (style.bold || style.italic || style.underline || style.strikethrough || style.size != null) {
                        builder.addStringAnnotation(PASTED_STYLE_TAG, styleCode(style), at, end)
                    }
                    run.link?.let { builder.addStringAnnotation(PASTED_LINK_TAG, it.trim(), at, end) }
                }
                at = end
            }
            val length = line.text.length
            if (line.table != null && length > 0) {
                builder.addStringAnnotation(PASTED_TABLE_TAG, encodeTable(line.table), lineStart, lineStart + length)
            }
            lineStart += length + 1
        }
        if (plain.isNotEmpty() && lines.any { it.type != BlockType.PARAGRAPH }) {
            builder.addStringAnnotation(PASTED_LINES_TAG, lines.joinToString(";") { "${it.type.name},0,${it.indent}" }, 0, plain.length)
        }
        return builder.toAnnotatedString()
    }

    /** I link dell'HTML, ritrovati in ordine nelle parole di [plain]. */
    private fun linksOf(html: String, plain: String): List<LinkRange> {
        val anchors = parseClipHtml(html).flatMap { line -> line.runs.mapNotNull { run -> run.link?.let { run.text to it } } }
            .filter { usableLink(it.second) }
        return matchAnchors(anchors, plain)
    }

    private companion object {
        val HTML_FLAVOR = DataFlavor("text/html;class=java.lang.String")
    }
}

@Composable
actual fun rememberLinkClipboard(
    richSelection: () -> List<RichTextSpan>?,
    selectedLines: () -> List<CopiedLine?>?
): LinkClipboard {
    val base = LocalClipboardManager.current
    val clipboard = LocalClipboard.current
    return remember(base, clipboard) { if (base is LinkClipboard) base else LinkClipboard(base, clipboard, richSelection, selectedLines) }
}

// Sul PC la barra sopra la tastiera non c'è: Ctrl+C, Ctrl+V e Ctrl+X li fa la tastiera vera.
@Composable
actual fun rememberEditShortcuts(): (EditShortcut) -> Unit = remember { { } }
