package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.gabriele.notionlocal.data.entity.LinkTarget
import com.gabriele.notionlocal.data.entity.blockLink
import com.gabriele.notionlocal.data.entity.blockLinkTarget
import com.gabriele.notionlocal.ui.theme.NotionGray400
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.BlockFiles
import com.gabriele.notionlocal.data.LinkTitles
import com.gabriele.notionlocal.data.entity.LinkLabel
import com.gabriele.notionlocal.data.entity.bareLinkAt
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.ui.i18n.BlockStrings
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.ui.theme.TextLinkColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import com.gabriele.notionlocal.data.entity.dateLinkTarget
import com.gabriele.notionlocal.data.entity.isInternalLink
import com.gabriele.notionlocal.data.entity.pageLinkTarget
import com.gabriele.notionlocal.platform.showMessage
import com.gabriele.notionlocal.platform.LocalPlatformContext

// ---------------------------------------------------------------------------
// **I collegamenti sul testo** (dal 17/10/2026, chiesto dall'utente): si
// seleziona del testo, si tocca l'icona del collegamento nella barra sopra
// la tastiera e si scrive l'indirizzo. Il testo diventa blu e sottolineato,
// e toccandolo si apre il link. L'indirizzo sta nel pezzo di testo
// (`RichTextSpan.link`).
// ---------------------------------------------------------------------------

/** Il colore del testo con un collegamento; va messo **prima** del colore scelto col pennello, che vince. */
fun linkStyleOf(span: RichTextSpan): SpanStyle? =
    if (span.link != null) SpanStyle(color = TextLinkColor) else null

/** Segna l'indirizzo sul testo mostrato da [start] a [end], per `linkTaps`. */
fun AnnotatedString.Builder.markLink(span: RichTextSpan, start: Int, end: Int) {
    val link = span.link ?: return
    if (start < end) addStringAnnotation(LINK_TAG, link, start, end)
}

/**
 * L'indirizzo del testo sotto il dito, o null. Conta solo la lettera
 * davvero toccata: un tocco nello spazio vuoto dopo la fine della riga
 * non apre il link che la chiude.
 */
fun linkAt(layout: TextLayoutResult, position: Offset): String? {
    val text = layout.layoutInput.text
    // Si guardano le lettere dei link, una per una: sono poche, e il
    // riquadro di ogni lettera dice proprio se il dito ci è caduto sopra.
    for (mark in text.getStringAnnotations(LINK_TAG, 0, text.length)) {
        for (index in mark.start until mark.end.coerceAtMost(text.length)) {
            if (layout.getBoundingBox(index).contains(position)) return mark.item
        }
    }
    return null
}

/**
 * **Un tocco breve su un testo con un collegamento lo apre**, e il campo
 * sotto non lo riceve: niente cursore né tastiera, come un link in una
 * pagina web. Tenendo premuto, invece, il dito arriva al campo come sempre
 * e seleziona il testo (per cambiare o togliere il link).
 */
fun Modifier.linkTaps(
    layout: () -> TextLayoutResult?,
    onLink: (String) -> Unit
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (linkAt(layout() ?: return@awaitEachGesture, down.position) == null) return@awaitEachGesture
        val up = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            waitForUpOrCancellation(PointerEventPass.Initial)
        } ?: return@awaitEachGesture
        if ((up.position - down.position).getDistance() > viewConfiguration.touchSlop) return@awaitEachGesture
        val link = linkAt(layout() ?: return@awaitEachGesture, up.position) ?: return@awaitEachGesture
        // Preso qui, il tocco non arriva al campo: niente cursore.
        up.consume()
        onLink(link)
    }
}

/**
 * Apre un collegamento; se non si può, lo dice. Le pagine web si aprono nel
 * browser predefinito: vedi `BlockFiles.openUrl`. Un collegamento a una riga
 * della pagina (dal 20/10/2026) porta lì: vedi `LocalBlockLinkJump`. Una
 * menzione "@" (dal 03/11/2026) apre la sua pagina, o fa cambiare la sua
 * data: vedi `Mentions.kt`.
 */
@Composable
fun rememberLinkOpener(): (String) -> Unit {
    val context = LocalPlatformContext.current
    val jump = LocalBlockLinkJump.current
    val openPage = LocalPageMentionOpen.current
    val tapDate = LocalDateMentionTap.current
    return remember(context, jump, openPage, tapDate) {
        { url ->
            val target = blockLinkTarget(url)
            val page = pageLinkTarget(url)
            if (target != null && jump != null) {
                jump(target)
            } else if (page != null && openPage != null) {
                openPage(page)
            } else if (dateLinkTarget(url) != null && tapDate != null) {
                tapDate(url)
            } else if (isInternalLink(url)) {
                // Una menzione dove non si apre niente (fuori dall'editor): resta testo.
            } else if (!BlockFiles.openUrl(context, url)) {
                showMessage(context, EditorStrings.cantOpenLink)
            }
        }
    }
}

private val EMAIL = Regex("""^[^@\s/:]+@[^@\s/]+\.[^@\s/]+$""")

/**
 * L'indirizzo scritto dall'utente, pronto da aprire: "google.com" diventa
 * "https://google.com", un indirizzo email diventa "mailto:…"; `mailto:` e
 * `tel:` scritti per intero restano come sono. Null se non sembra un link.
 */
fun textLinkTarget(input: String): String? {
    val text = input.trim()
    if (text.isEmpty() || text.any { it.isWhitespace() }) return null
    val lower = text.lowercase()
    if (lower.startsWith("mailto:") || lower.startsWith("tel:")) {
        return text.takeIf { it.substringAfter(':').isNotEmpty() }
    }
    if (EMAIL.matches(text)) return "mailto:$text"
    return normalizedLink(text)
}

/**
 * La finestrella del collegamento: l'indirizzo da scrivere o incollare, e
 * "Remove link" se il testo ne ha già uno. [current] è quello che c'è già.
 *
 * **"In this page"** (dal 20/10/2026): la seconda scheda elenca le righe della
 * pagina a cui ci si può collegare ([targets], vedi `linkTargetsOf`); un
 * tocco su una di loro fa il collegamento.
 *
 * **"Show"** (dal 27/10/2026): sotto l'indirizzo, cosa si vede nel testo —
 * il link per intero, il titolo della pagina web o una scritta propria (vedi
 * `LinkShowChoices`). [shown] è il testo di adesso; null quando la selezione
 * prende più righe, e lì si cambia solo l'indirizzo. [onSave] riceve
 * l'indirizzo e il testo nuovo, o null se il testo resta com'è. Un sito
 * scritto come testo, senza link, fa già da indirizzo.
 */
@Composable
fun LinkDialog(
    current: String?,
    shown: String? = null,
    targets: List<LinkTarget>,
    onDismiss: () -> Unit,
    onSave: (url: String, shown: String?) -> Unit,
    onRemove: () -> Unit
) {
    val currentTarget = blockLinkTarget(current)
    var inPage by remember { mutableStateOf(currentTarget != null) }
    val start = when {
        currentTarget != null -> ""
        current != null -> current
        shown != null && bareLinkAt(shown, 0, 0) == shown.indices -> shown
        else -> ""
    }
    var field by remember { mutableStateOf(TextFieldValue(start, TextRange(0, start.length))) }
    var wrong by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val showState = remember {
        // Un testo che è già il link per intero non fa da scritta: il campo di "Your text" parte vuoto.
        val whole = textLinkTarget(start)?.let { linkShownText(it) }
        if (shown != null && shown == whole) LinkShowState(LinkShow.LINK, "") else LinkShowState(LinkShow.TEXT, shown.orEmpty())
    }
    val url = textLinkTarget(field.text)
    // Il testo da dare al link; null finché non è pronto (il titolo da leggere).
    val newText = if (shown == null || url == null) null else showState.shownFor(url)
    val ready = shown == null || url == null || newText != null
    val submit = {
        if (url == null) wrong = true else if (ready) onSave(url, newText?.takeIf { it != shown })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSheet,
        title = { Text(EditorStrings.link, color = NotionWhite) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                TabRow(selectedTabIndex = if (inPage) 1 else 0, containerColor = DarkSheet) {
                    Tab(selected = !inPage, onClick = { inPage = false }, text = { Text(EditorStrings.linkWeb) }, unselectedContentColor = NotionGray400)
                    Tab(selected = inPage, onClick = { inPage = true }, text = { Text(EditorStrings.linkInThisPage) }, unselectedContentColor = NotionGray400)
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (inPage) {
                    LinkTargetList(targets, currentTarget) { onSave(blockLink(it), null) }
                } else {
                // Un campo per indirizzi: `AutoCaps` lo lascia com'è, niente maiuscola.
                AutoCaps {
                    OutlinedTextField(
                        value = field,
                        onValueChange = {
                            field = it
                            wrong = false
                        },
                        singleLine = true,
                        isError = wrong,
                        placeholder = { Text(EditorStrings.linkHint) },
                        keyboardOptions = KeyboardOptions(
                            autoCorrect = false,
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus)
                    )
                }
                // Qui, accanto al campo: chiesto prima che esista, il fuoco non ha dove andare.
                LaunchedEffect(Unit) { focus.requestFocus() }
                if (wrong) {
                    Text(
                        BlockStrings.notALink,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (shown != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinkShowChoices(url = url, whole = url?.let { linkShownText(it) }, state = showState)
                }
                }
            }
        },
        confirmButton = {
            // Nella pagina basta toccare la riga.
            if (!inPage) TextButton(enabled = field.text.isNotBlank() && ready, onClick = submit) { Text(Strings.done) }
        },
        dismissButton = {
            Row {
                if (current != null) {
                    TextButton(onClick = onRemove) {
                        Text(EditorStrings.removeLink, color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text(Strings.cancel) }
            }
        }
    )
}

// ---------------------------------------------------------------------------
// **Quello che si vede di un link** (dal 27/10/2026, chiesto dall'utente:
// "Concedimi di modificare quello che si vede quando c'è un sito scritto in
// un campo testo normale o in un database, con tre opzioni: 1) il link per
// intero; 2) il titolo della pagina web; 3) una scritta che io scriverò
// manualmente"). Nel testo cambia il testo del link (`LinkDialog`); in una
// cella URL la scritta mostrata al posto dell'indirizzo (`LinkShowDialog`,
// `DatabaseCellEntity.linkLabel`).
// ---------------------------------------------------------------------------

/**
 * Le righe a cui collegarsi, come le voci di un indice: rientrate come nella
 * pagina, col segnalibro quelle col pin e la "T" quelle scritte in grande.
 * Quella a cui il testo porta già è colorata.
 */
@Composable
private fun LinkTargetList(targets: List<LinkTarget>, current: String?, onPick: (String) -> Unit) {
    if (targets.isEmpty()) {
        Text(EditorStrings.noLinkTargets, style = MaterialTheme.typography.bodyMedium, color = NotionGray400)
        return
    }
    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp).verticalScroll(rememberScrollState())) {
        targets.forEach { target ->
            val chosen = target.blockId == current
            val tint = if (chosen) MaterialTheme.colorScheme.primary else NotionGray400
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onPick(target.blockId) }
                    .padding(start = (4 + 14 * target.level.coerceAtMost(4)).dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (target.pinned) Icons.Filled.Bookmark else Icons.Filled.Title,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = tint
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    target.label.ifBlank { EditorStrings.untitledLine },
                    color = if (chosen) MaterialTheme.colorScheme.primary else NotionWhite,
                    fontWeight = if (target.pinned) null else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
