package com.gabriele.notionlocal.ui.screen

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.LinkTitles
import com.gabriele.notionlocal.data.entity.LinkLabel
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import kotlinx.coroutines.delay

/** L'etichetta con cui il testo mostrato porta l'indirizzo, per sapere cosa si è toccato. */
const val LINK_TAG = "text-link"

enum class LinkShow { LINK, TITLE, TEXT }

/** Il link per intero come si legge nel testo: un'email o un numero senza "mailto:" e "tel:" davanti. */
fun linkShownText(url: String): String {
    val lower = url.lowercase()
    return if (lower.startsWith("mailto:") || lower.startsWith("tel:")) url.substringAfter(':') else url
}

/** Solo una pagina web ha un titolo da leggere. */
fun hasWebTitle(url: String): Boolean =
    url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)

/** Quanto si aspetta prima di leggere il titolo: l'indirizzo si sta forse ancora scrivendo. */
internal const val TITLE_READ_PAUSE_MS = 400L

/** La scelta fatta nelle finestrelle, col titolo letto ([title]: l'indirizzo e il suo titolo). */
@Stable
class LinkShowState(show: LinkShow, text: String, title: Pair<String, String>? = null) {
    var show by mutableStateOf(show)
    /** La scritta dell'utente. */
    var text by mutableStateOf(text)
    var title by mutableStateOf(title)
    var reading by mutableStateOf(false)
    /** L'indirizzo di cui il titolo non si è potuto leggere. */
    var missing by mutableStateOf<String?>(null)
    /** Scelta adesso "Your text": il suo campo prende il fuoco. */
    var focusText by mutableStateOf(false)

    /** Il testo da mostrare per [url]; null se non è pronto (il titolo da leggere, la scritta vuota). */
    fun shownFor(url: String): String? = when (show) {
        LinkShow.LINK -> linkShownText(url)
        LinkShow.TITLE -> title?.takeIf { it.first == url }?.second
        LinkShow.TEXT -> text.trim().takeIf { it.isNotEmpty() }
    }
}

/**
 * Le tre scelte: "Full link" ([whole], com'è scritto), "Page title" (letto
 * dal sito quando lo si sceglie, e di nuovo se [url] cambia: vedi
 * `LinkTitles`) e "Your text", col suo campo. Senza rete il titolo non
 * arriva, e lo si dice.
 */
@Composable
fun LinkShowChoices(url: String?, whole: String?, state: LinkShowState) {
    LaunchedEffect(state.show, url) {
        if (state.show != LinkShow.TITLE || url == null || !hasWebTitle(url) || state.title?.first == url) return@LaunchedEffect
        state.reading = true
        state.missing = null
        try {
            delay(TITLE_READ_PAUSE_MS)
            val found = LinkTitles.read(url)
            if (found != null) state.title = url to found else state.missing = url
        } finally {
            state.reading = false
        }
    }
    val titleDetail = when {
        url == null -> null
        !hasWebTitle(url) -> EditorStrings.linkNoTitle
        state.title?.first == url -> state.title?.second
        state.show != LinkShow.TITLE -> null
        state.missing == url -> EditorStrings.linkNoTitle
        else -> EditorStrings.linkReadingTitle
    }
    val textFocus = remember { FocusRequester() }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(EditorStrings.linkShow, style = MaterialTheme.typography.labelLarge, color = NotionGray400)
        LinkShowChoice(state, LinkShow.LINK, EditorStrings.linkShowLink, whole)
        LinkShowChoice(state, LinkShow.TITLE, EditorStrings.linkShowTitle, titleDetail, enabled = url == null || hasWebTitle(url))
        LinkShowChoice(state, LinkShow.TEXT, EditorStrings.linkShowText, null)
        if (state.show == LinkShow.TEXT) {
            AutoCaps {
                OutlinedTextField(
                    value = state.text,
                    onValueChange = { state.text = it.replace('\n', ' ') },
                    singleLine = true,
                    placeholder = { Text(EditorStrings.linkTextHint) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp)
                        .focusRequester(textFocus)
                )
            }
            LaunchedEffect(state.focusText) {
                if (state.focusText) {
                    textFocus.requestFocus()
                    state.focusText = false
                }
            }
        }
    }
}

@Composable
internal fun LinkShowChoice(state: LinkShowState, choice: LinkShow, label: String, detail: String?, enabled: Boolean = true) {
    val pick = {
        state.show = choice
        if (choice == LinkShow.TEXT) state.focusText = true
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = enabled, onClick = pick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = state.show == choice, onClick = pick, enabled = enabled)
        Column(modifier = Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(label, color = if (enabled) NotionWhite else NotionGray400)
            if (detail != null) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = NotionGray400,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * **La matita di una cella URL** (dal 13/11/2026, chiesto dall'utente): il
 * titolo letto dal sito (o la scritta di prima) si corregge qui, in un
 * pannello come gli altri del database (sul PC accanto al clic). [onSave]
 * riceve il testo scritto; vuoto vuol dire di nuovo il titolo della pagina.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkTitleEditSheet(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(current) }
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    DbStrings.editLinkTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss) { Text(Strings.cancel) }
                TextButton(onClick = { onSave(text) }) { Text(EditorStrings.save) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            SheetTextField(
                value = text,
                onValueChange = { text = it.replace('\n', ' ') },
                placeholder = EditorStrings.linkTextHint,
                leading = Icons.Filled.Edit
            )
            Text(
                DbStrings.linkTitleEmptyHint,
                style = MaterialTheme.typography.bodySmall,
                color = NotionGray400,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
        }
    }
}
