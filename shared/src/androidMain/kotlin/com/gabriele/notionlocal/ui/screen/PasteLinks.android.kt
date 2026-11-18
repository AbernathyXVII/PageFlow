package com.gabriele.notionlocal.ui.screen

import android.content.ClipData
import android.content.Context
import android.graphics.Typeface
import android.text.Html
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.CopiedLine
import com.gabriele.notionlocal.data.entity.LinkRange
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.StyleRange
import com.gabriele.notionlocal.data.entity.plainText
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.TextRange
import com.gabriele.notionlocal.platform.LocalPlatformContext
import androidx.compose.ui.platform.LocalView

/** Gli appunti dell'editor: come quelli di sempre, ma il testo incollato porta i suoi link e, a richiesta, il suo stile. */
actual class LinkClipboard(
    private val context: Context,
    private val base: ClipboardManager,
    /** La selezione di adesso col suo stile, per copiare con la formattazione; null se non c'è. */
    private val richSelection: () -> List<RichTextSpan>? = { null },
    /** Le righe della selezione di adesso col loro tipo, una per riga (vedi [CopiedLine]). */
    private val selectedLines: () -> List<CopiedLine?>? = { null }
) : ClipboardManager by base, Clipboard {

    /** Il modo del prossimo incolla; poi si torna a quello di sempre. */
    actual var nextPasteMode: PasteMode = PasteMode.LINKS_ONLY

    override val nativeClipboard: NativeClipboard get() = base.nativeClipboard

    /** Il primo elemento degli appunti di Android, se c'è. */
    private fun clipItem(): ClipData.Item? = runCatching {
        context.getSystemService(android.content.ClipboardManager::class.java)?.primaryClip
            ?.takeIf { it.itemCount > 0 }?.getItemAt(0)
    }.getOrNull()

    /** Il testo che il campo incolla: quello degli appunti o, se c'è solo l'HTML, il suo testo. */
    private fun clipText(item: ClipData.Item): String? =
        (item.text?.toString() ?: item.htmlText?.let { htmlToSpanned(it).toString().trimEnd() })?.withoutCarriageReturns()

    // **Gli appunti dei campi di Compose 1.8 e dopo** (dal 03/11/2026): i
    // campi copiano e incollano da qui, non più da [getText] e [setText].
    // Copiando si passa da [setText], che ci mette l'HTML; incollando il
    // campo riceve il testo e basta, e [formatPaste] lo cambia con quello di
    // [getText], coi link, gli stili e il tipo delle righe.

    override suspend fun getClipEntry(): ClipEntry? {
        val text = clipItem()?.let(::clipText) ?: return null
        return ClipData.newPlainText("PageFlow", text).toClipEntry()
    }

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        val text = clipEntry?.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
        if (text != null) {
            setText(AnnotatedString(text))
            return
        }
        val system = context.getSystemService(android.content.ClipboardManager::class.java) ?: return
        if (clipEntry == null) system.clearPrimaryClip() else system.setPrimaryClip(clipEntry.clipData)
    }

    /**
     * **L'incolla dei campi di Compose 1.8 e dopo** (dal 03/11/2026): se
     * [after] è [before] col testo degli appunti al posto della selezione,
     * quel testo diventa quello di [getText] — coi link, gli stili e il tipo
     * delle righe, nelle annotazioni che legge [pastedFormat] — e il cursore
     * va dopo di lui. Altrimenti [after] resta com'è.
     */
    actual fun formatPaste(before: TextFieldValue, after: TextFieldValue): TextFieldValue {
        val clip = clipItem()?.let(::clipText)?.takeIf { it.isNotEmpty() } ?: return after
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
        val item = runCatching {
            context.getSystemService(android.content.ClipboardManager::class.java)?.primaryClip
                ?.takeIf { it.itemCount > 0 }?.getItemAt(0)
        }.getOrNull() ?: return base.getText()
        // **Com'era fatto nella fonte** (dal 20/10/2026): da un'altra app con
        // l'HTML, righe, elenchi, tabelle e grandezze (vedi `ClipHtml.kt`).
        // Da notE stesso il tipo delle righe c'è già, nel commento.
        val html = item.htmlText
        if (mode == PasteMode.KEEP_SOURCE && html != null && linesMarkerOf(html) == null) {
            val lines = runCatching { parseClipHtml(html) }.getOrNull()
            if (!lines.isNullOrEmpty()) return sourceFormatted(lines)
        }
        val raw: CharSequence = item.text ?: item.htmlText?.let { htmlToSpanned(it).toString().trimEnd() } ?: return base.getText()
        val plain = raw.toString()
        val builder = AnnotatedString.Builder(plain)
        if (mode == PasteMode.KEEP_SOURCE || mode == PasteMode.TEXT_ONLY) {
            builder.addStringAnnotation(PASTED_PLAIN_TAG, "", 0, plain.length)
        }
        if (mode == PasteMode.KEEP_SOURCE || mode == PasteMode.LINKS_ONLY) {
            // Un indirizzo copiato da solo, senza link negli appunti: un link (vedi `bareWebLink`).
            runCatching { clipLinks(item, plain) }.getOrDefault(emptyList())
                .ifEmpty { listOfNotNull(bareWebLink(plain)) }
                .forEach { builder.addStringAnnotation(PASTED_LINK_TAG, it.url, it.start, it.end) }
        }
        if (mode == PasteMode.KEEP_SOURCE) {
            runCatching { clipStyles(item, plain) }.getOrDefault(emptyList()).forEach {
                builder.addStringAnnotation(PASTED_STYLE_TAG, styleCode(it), it.start, it.end)
            }
        }
        // Il tipo delle righe copiate da notE: su tutto il testo, una voce per riga.
        if (mode != PasteMode.TEXT_ONLY) {
            linesMarkerOf(item.htmlText)?.takeIf { it.split(';').size == plain.split('\n').size }?.let {
                builder.addStringAnnotation(PASTED_LINES_TAG, it, 0, plain.length)
            }
        }
        return builder.toAnnotatedString()
    }

    /**
     * Copiando: col testo semplice anche l'HTML con la formattazione, se la
     * selezione ne ha, e il tipo delle righe, se ce n'è una che non è testo
     * normale (vedi [CopiedLine]).
     */
    override fun setText(annotatedString: AnnotatedString) {
        val text = annotatedString.text
        val selection = runCatching { richSelection() }.getOrNull()?.takeIf { it.plainText() == text }
        val rich = selection?.takeIf { spans -> spans.any { it.hasStyle() } }
        val lines = selection?.let { runCatching { selectedLines() }.getOrNull() }
            ?.takeIf { lines -> lines.size == text.split('\n').size && lines.any { it != null && (it.type != BlockType.PARAGRAPH || it.indent > 0) } }
        if (rich == null && lines == null) {
            base.setText(annotatedString)
            return
        }
        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
        if (clipboard == null) {
            base.setText(annotatedString)
            return
        }
        val html = spansToHtml(rich ?: listOf(RichTextSpan(text = text))) + (lines?.let { linesMarker(it) } ?: "")
        clipboard.setPrimaryClip(ClipData.newHtmlText("PageFlow", text, html))
    }

    /** I link di un elemento degli appunti, nelle posizioni di [plain]. */
    private fun clipLinks(item: ClipData.Item, plain: String): List<LinkRange> {
        // Un testo coi link dentro: le posizioni sono già quelle giuste.
        (item.text as? Spanned)?.let { spanned ->
            val found = spanned.getSpans(0, spanned.length, URLSpan::class.java)
                .sortedBy { spanned.getSpanStart(it) }
                .map { LinkRange(spanned.getSpanStart(it), spanned.getSpanEnd(it), it.url.orEmpty()) }
                .filter { it.end <= plain.length && usableLink(it.url) }
            if (found.isNotEmpty()) return found
        }
        // L'HTML accanto al testo semplice: le parole dei link si ritrovano
        // nel testo, in ordine.
        val html = item.htmlText ?: return emptyList()
        val spanned = htmlToSpanned(html)
        val anchors = spanned.getSpans(0, spanned.length, URLSpan::class.java)
            .sortedBy { spanned.getSpanStart(it) }
            .map { spanned.subSequence(spanned.getSpanStart(it), spanned.getSpanEnd(it)).toString() to it.url.orEmpty() }
            .filter { usableLink(it.second) }
        return matchAnchors(anchors, plain)
    }

    /** Gli stili di un elemento degli appunti, nelle posizioni di [plain]. */
    private fun clipStyles(item: ClipData.Item, plain: String): List<StyleRange> {
        (item.text as? Spanned)?.let { spanned ->
            val found = stylesOfSpanned(spanned).filter { it.end <= plain.length }
            if (found.isNotEmpty()) return found
        }
        val html = item.htmlText ?: return emptyList()
        return matchStyles(htmlStyleRuns(html), plain)
    }
}

internal fun htmlToSpanned(html: String): Spanned = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)

/** Gli stili di un testo con gli stili dentro (un'app che copia un testo formattato). */
internal fun stylesOfSpanned(spanned: Spanned): List<StyleRange> {
    val result = mutableListOf<StyleRange>()
    spanned.getSpans(0, spanned.length, Any::class.java).forEach { span ->
        val start = spanned.getSpanStart(span)
        val end = spanned.getSpanEnd(span)
        if (start >= end) return@forEach
        when (span) {
            is StyleSpan -> result += StyleRange(
                start, end,
                bold = span.style == Typeface.BOLD || span.style == Typeface.BOLD_ITALIC,
                italic = span.style == Typeface.ITALIC || span.style == Typeface.BOLD_ITALIC
            )
            is UnderlineSpan -> result += StyleRange(start, end, underline = true)
            is StrikethroughSpan -> result += StyleRange(start, end, strikethrough = true)
            // I colori (dal 17/11/2026), con le stesse regole dell'HTML (vedi `cssColor`).
            is ForegroundColorSpan -> result += StyleRange(start, end, color = cssColor(argbCss(span.foregroundColor))?.ifEmpty { null })
            is BackgroundColorSpan -> result += StyleRange(start, end, background = cssColor(argbCss(span.backgroundColor))?.ifEmpty { null })
        }
    }
    return result.filter { it.isStyled }
}

/** Un colore di Android come "rgba(…)" dei CSS, per [cssColor]. */
private fun argbCss(argb: Int): String = "rgba(${argb shr 16 and 0xFF}, ${argb shr 8 and 0xFF}, ${argb and 0xFF}, ${(argb ushr 24) / 255f})"

/**
 * **Ctrl + un tasto**, mandato al campo che ha il fuoco come da una tastiera
 * vera: C copia, V incolla, X taglia, A seleziona tutto. È così che i
 * pulsanti della barra sopra la tastiera fanno il loro lavoro.
 */
fun View.sendCtrlShortcut(keyCode: Int) {
    val now = SystemClock.uptimeMillis()
    dispatchKeyEvent(ShortcutKey(now, KeyEvent.ACTION_DOWN, keyCode))
    dispatchKeyEvent(ShortcutKey(now, KeyEvent.ACTION_UP, keyCode))
}

/**
 * Ctrl + un tasto **senza un carattere**: il campo di testo prende un tasto
 * col suo carattere per una lettera da scrivere (Ctrl+A scriveva "a" in certi
 * telefoni e nei test), uno senza per una scorciatoia.
 */
internal class ShortcutKey(time: Long, action: Int, code: Int) :
    KeyEvent(time, time, action, code, 0, META_CTRL_ON or META_CTRL_LEFT_ON) {
    override fun getUnicodeChar(): Int = 0
    override fun getUnicodeChar(metaState: Int): Int = 0
}

/**
 * Gli appunti coi link per i campi dell'editor; [richSelection] dà lo stile
 * della selezione e [selectedLines] il tipo delle sue righe, per copiare.
 */
@Composable
actual fun rememberLinkClipboard(
    richSelection: () -> List<RichTextSpan>?,
    selectedLines: () -> List<CopiedLine?>?
): LinkClipboard {
    val context = LocalPlatformContext.current
    val base = LocalClipboardManager.current
    return remember(context, base) { if (base is LinkClipboard) base else LinkClipboard(context, base, richSelection, selectedLines) }
}

@Composable
actual fun rememberEditShortcuts(): (EditShortcut) -> Unit {
    val view = LocalView.current
    return remember(view) {
        { shortcut ->
            view.sendCtrlShortcut(
                when (shortcut) {
                    EditShortcut.COPY -> KeyEvent.KEYCODE_C
                    EditShortcut.PASTE -> KeyEvent.KEYCODE_V
                    EditShortcut.CUT -> KeyEvent.KEYCODE_X
                }
            )
        }
    }
}
