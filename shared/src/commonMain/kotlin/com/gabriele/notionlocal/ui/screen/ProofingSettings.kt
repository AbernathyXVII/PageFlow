package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.PROOFING_DEFAULT
import com.gabriele.notionlocal.data.entity.PROOFING_OFF
import com.gabriele.notionlocal.data.entity.PROOFING_ON
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.ignoredWords
import com.gabriele.notionlocal.data.proofing.ProofingLanguage
import com.gabriele.notionlocal.data.proofing.SpellChecker
import com.gabriele.notionlocal.data.proofing.SpellDictionaries
import com.gabriele.notionlocal.data.proofing.SpellDictionary
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.i18n.ProofingStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.AccentBlue
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import kotlinx.coroutines.launch
import com.gabriele.notionlocal.platform.LocalPlatformContext

/**
 * Le scelte del correttore (dal 08/10/2026): in Settings per tutte le
 * pagine, nei tre puntini per una pagina sola. Vedi `ProofingUi.kt` per
 * quello che si vede nel testo.
 */

/** Se il correttore è acceso in [page], e in che lingua: la scelta della pagina, o quella di Settings. */
fun proofingOf(page: PageEntity): Pair<Boolean, ProofingLanguage> {
    val enabled = when (page.proofingMode) {
        PROOFING_ON -> true
        PROOFING_OFF -> false
        else -> AppSettings.proofingEnabled
    }
    val language = ProofingLanguage.fromCode(page.proofingLanguage)?.takeIf { it.available } ?: AppSettings.proofingLanguage
    return enabled to language
}

/**
 * Il correttore della pagina aperta, o null se lì è spento. Il
 * dizionario si legge la prima volta che serve: fino ad allora null, e
 * le parole si segnano appena è pronto.
 */
@Composable
fun rememberPageProofing(page: PageEntity?, viewModel: PageEditorViewModel): PageProofing? {
    val context = LocalPlatformContext.current
    val (enabled, language) = if (page == null) false to AppSettings.proofingLanguage else proofingOf(page)
    var dictionary by remember { mutableStateOf<SpellDictionary?>(null) }
    LaunchedEffect(enabled, language) {
        dictionary = if (enabled) SpellDictionaries.get(context, language) else null
    }
    val personal by viewModel.personalWords.collectAsStateWithLifecycle(initialValue = emptyList())
    val ignored = page?.ignoredWords.orEmpty()
    val d = dictionary?.takeIf { enabled }
    val checker = remember(d, personal, ignored) { d?.let { SpellChecker(it, personal.toSet(), ignored) } }
    return remember(checker) {
        checker?.let { PageProofing(it, addToDictionary = viewModel::addToDictionary, ignore = viewModel::ignoreWord) }
    }
}

/** Cosa dice la voce "Proofing" nei tre puntini: "Default (On)", "Off", "On · Deutsch"... */
fun proofingSummary(page: PageEntity): String {
    val (enabled, language) = proofingOf(page)
    val state = if (enabled) ProofingStrings.on else ProofingStrings.off
    val mode = if (page.proofingMode == PROOFING_DEFAULT) ProofingStrings.default(state) else state
    return if (enabled) "$mode · ${language.label}" else mode
}

/**
 * "Proofing" dai tre puntini: acceso, spento o come Settings; la lingua
 * di questa pagina; le parole ignorate qui, che si possono rimettere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageProofingSheet(
    page: PageEntity,
    onSetProofing: (mode: Int, language: String?) -> Unit,
    onStopIgnoring: (String) -> Unit,
    onDismiss: () -> Unit
) {
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            Text(ProofingStrings.proofing, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                ProofingStrings.pageNote,
                color = NotionGray400,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            val globalState = if (AppSettings.proofingEnabled) ProofingStrings.on else ProofingStrings.off
            OptionsGroup {
                RadioItem(
                    title = ProofingStrings.default(globalState),
                    selected = page.proofingMode == PROOFING_DEFAULT,
                    onClick = { onSetProofing(PROOFING_DEFAULT, page.proofingLanguage) }
                )
                RadioItem(
                    title = ProofingStrings.on,
                    selected = page.proofingMode == PROOFING_ON,
                    onClick = { onSetProofing(PROOFING_ON, page.proofingLanguage) }
                )
                RadioItem(
                    title = ProofingStrings.off,
                    selected = page.proofingMode == PROOFING_OFF,
                    onClick = { onSetProofing(PROOFING_OFF, page.proofingLanguage) }
                )
            }

            Text(
                ProofingStrings.language,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            OptionsGroup {
                val chosen = ProofingLanguage.fromCode(page.proofingLanguage)?.takeIf { it.available }
                RadioItem(
                    title = ProofingStrings.default(AppSettings.proofingLanguage.label),
                    selected = chosen == null,
                    onClick = { onSetProofing(page.proofingMode, null) }
                )
                LanguageChoices(selected = chosen) { onSetProofing(page.proofingMode, it.code) }
            }

            val ignored = page.ignoredWords.sorted()
            if (ignored.isNotEmpty()) {
                Text(
                    ProofingStrings.ignoredWords,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    ProofingStrings.ignoredWordsNote,
                    color = NotionGray400,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )
                OptionsGroup {
                    ignored.forEachIndexed { i, word ->
                        if (i > 0) HorizontalDivider()
                        WordRow(word, ProofingStrings.stopIgnoring) { onStopIgnoring(word) }
                    }
                }
            }
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}

/** Le lingue, una per riga: cinese, coreano e giapponese grigie, "Not available yet". */
@Composable
private fun LanguageChoices(selected: ProofingLanguage?, onSelect: (ProofingLanguage) -> Unit) {
    ProofingLanguage.entries.forEach { language ->
        if (language.available) {
            RadioItem(title = language.label, selected = language == selected, onClick = { onSelect(language) })
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Con un `onClick`, anche se spento, il pallino tiene lo
                // spazio per il dito che hanno gli altri: senza, la riga
                // partiva più a sinistra.
                RadioButton(
                    selected = false,
                    onClick = {},
                    enabled = false,
                    colors = RadioButtonDefaults.colors(disabledUnselectedColor = NotionGray400.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                    Text(language.label, color = NotionGray400.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyLarge)
                    Text(ProofingStrings.notAvailable, color = NotionGray400.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** Una parola con un'azione a destra: "Remove" nel dizionario, "Check again" fra le ignorate. */
@Composable
private fun WordRow(word: String, action: String, start: Dp = 16.dp, onAction: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = start, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(word, color = NotionWhite, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = onAction) { Text(action, color = AccentBlue) }
    }
}

/**
 * Settings → "Proofing": acceso o spento per tutte le pagine, la lingua,
 * e "My dictionary", le parole aggiunte, che da qui si possono togliere.
 */
@Composable
fun ProofingSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    val dao = remember(context) { AppDatabase.getInstance(context).personalWordDao() }
    val words by remember(dao) { dao.observeAll() }.collectAsState(initial = emptyList())
    var dictionaryOpen by remember { mutableStateOf(false) }
    FitAlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSheet,
        title = { Text(ProofingStrings.proofing, color = NotionWhite) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Lo stesso interruttore delle altre impostazioni: quelli di
                // serie, spenti, su questo fondo quasi non si vedevano.
                SwitchItem(
                    ProofingStrings.checkSpelling,
                    checked = AppSettings.proofingEnabled
                ) { AppSettings.proofingEnabled = it }
                // Sotto e intera: come riga della voce sarebbe tagliata a due righe.
                Text(
                    ProofingStrings.checkSpellingNote,
                    color = NotionGray400,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
                )
                // Tutto allineato al testo delle voci (`SettingsItem`, 20dp dal bordo).
                Text(
                    ProofingStrings.language,
                    color = NotionWhite,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 20.dp)
                )
                LanguageChoices(selected = AppSettings.proofingLanguage) { AppSettings.proofingLanguage = it }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsItem(
                    ProofingStrings.myDictionary,
                    value = ProofingStrings.wordCount(words.size),
                    onClick = { dictionaryOpen = !dictionaryOpen }
                )
                if (dictionaryOpen) {
                    Text(
                        ProofingStrings.myDictionaryNote,
                        color = NotionGray400,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
                    )
                    if (words.isEmpty()) {
                        Text(
                            ProofingStrings.myDictionaryEmpty,
                            color = NotionGray400,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 20.dp)
                        )
                    }
                    words.forEach { entry ->
                        WordRow(entry.word, ProofingStrings.removeWord, start = 20.dp) {
                            scope.launch { dao.delete(entry.word) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(Strings.done) } }
    )
}
