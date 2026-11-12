package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.platform.isPc
import com.gabriele.notionlocal.data.settings.OpenMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Spellcheck
import com.gabriele.notionlocal.ui.i18n.ProofingStrings
import com.gabriele.notionlocal.ui.i18n.ParagraphStrings
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.security.AppLock
import com.gabriele.notionlocal.ui.i18n.LockStrings
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.gabriele.notionlocal.data.AppCleanup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.settings.AnimationLevel
import com.gabriele.notionlocal.data.settings.AppLanguage
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.settings.DateStyle
import com.gabriele.notionlocal.data.settings.NumberStyle
import com.gabriele.notionlocal.data.settings.ThemeMode
import com.gabriele.notionlocal.data.settings.TitlePosition
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionGray900
import com.gabriele.notionlocal.ui.theme.NotionWhite
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.filled.Schedule
import com.gabriele.notionlocal.data.entity.TimeFormatKind
import com.gabriele.notionlocal.ui.i18n.DbStrings
import java.time.ZoneId
import java.time.format.TextStyle
import com.gabriele.notionlocal.ui.i18n.ShareStrings
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material.icons.filled.RocketLaunch
import com.gabriele.notionlocal.data.settings.StartupMode
import com.gabriele.notionlocal.platform.showMessage
import com.gabriele.notionlocal.platform.LocalPlatformContext

/** Quale finestra di scelta è aperta. */
private enum class SettingsDialog { THEME, TITLE_POSITION, ANIMATIONS, NUMBER, DATE, TIME, LANGUAGE, PROOFING, PARAGRAPH, OPEN_PAGES, TIME_ZONE, LOCK_PASSWORD, CLEAR_CACHE, DELETE_ALL }

/**
 * Le impostazioni. Ogni scelta si salva appena la si fa e vale subito: il
 * tema cambia sotto le dita, la lingua ridisegna le voci, le date si
 * riscrivono nel formato nuovo ovunque compaiano.
 *
 * Carattere e dimensione del carattere non ci sono più (tolti il
 * 25/09/2026, su richiesta): si scelgono sul testo selezionato, nella
 * barra Aa della pagina.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPatchNotes: () -> Unit = {},
    factory: ViewModelFactory? = null,
    /** "Startup window", che dal 03/11/2026 si apre da qui (prima dalla barra laterale). */
    onOpenStartup: () -> Unit = {}
) {
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    // "Shared items and quick notes": la pagina in cui finiscono (dal 02/11/2026).
    var pickingSharePage by remember { mutableStateOf(false) }
    var sharePageTitle by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(AppSettings.sharePageId) {
        val chosen = AppSettings.sharePageId
        // Lo stato dello schermo dal suo thread, il database su IO (vedi `ShareSheet`).
        withContext(Dispatchers.Main.immediate) {
            sharePageTitle = withContext(Dispatchers.IO) {
                chosen?.let { id -> factory?.pageRepository?.getPage(id) }
                    ?.takeIf { it.trashedAt == null && !it.isDatabase }
                    ?.title?.ifBlank { Strings.untitled }
            }
        }
    }
    val context = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    // Quanto occupa la cache, contato fuori dal disegno: sono file su disco.
    var cacheBytes by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) { cacheBytes = withContext(Dispatchers.IO) { AppCleanup.cacheBytes(context) } }

    Scaffold(
        topBar = { WorkspaceTopBar(title = Strings.settings, onBack = onBack) },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            SettingsItem(
                Strings.theme,
                icon = Icons.Filled.DarkMode,
                value = themeLabel(AppSettings.themeMode),
                onClick = { dialog = SettingsDialog.THEME }
            )

            // **Le notifiche** (dal 05/10/2026 tutte qui, chiesto dall'utente):
            // suono (anche le suonerie), vibrazione, priorità, pallino
            // sull'icona, la notifica di prova e le notifiche spente. Prima
            // questa sezione era "App sounds", con solo suono e notifiche
            // spente, e il resto stava nella pagina "Notifications" della
            // barra laterale. Vedi `NotificationSettingsSection`.
            SectionHeader(Strings.notifications)
            NotificationSettingsSection()
            HorizontalDivider(color = NotionGray900, modifier = Modifier.padding(vertical = 12.dp))

            // **"Startup window"** (dal 03/11/2026 qui, chiesto dall'utente:
            // "spostala dalla tendina a sinistra in Settings sopra Title
            // position"): da dove parte l'app, nella sua schermata.
            SettingsItem(
                Strings.startupWindow,
                icon = Icons.Filled.RocketLaunch,
                value = when (AppSettings.startupMode) {
                    StartupMode.LAST_VISITED -> Strings.lastVisitedPage
                    StartupMode.HOME -> Strings.mainMenu
                    StartupMode.SPECIFIC_PAGE -> Strings.specificPage
                },
                onClick = onOpenStartup
            )
            // **"Title position"** (dal 05/10/2026, chiesto dall'utente: "in
            // Settings sopra Animations", per tutte le pagine insieme).
            SettingsItem(
                Strings.titlePosition,
                icon = Icons.Filled.Title,
                value = titlePositionLabel(AppSettings.titlePosition),
                onClick = { dialog = SettingsDialog.TITLE_POSITION }
            )
            // **"Full line view"** (dal 19/10/2026, chiesto dall'utente), sotto
            // la posizione del titolo: le righe lunghe non vanno a capo e si
            // scorrono di lato. Acceso di partenza.
            SettingsItem(
                Strings.fullLineView,
                icon = Icons.Filled.SettingsEthernet,
                value = Strings.fullLineViewNote,
                onClick = { AppSettings.fullLineView = !AppSettings.fullLineView },
                trailing = {
                    Switch(checked = AppSettings.fullLineView, onCheckedChange = { AppSettings.fullLineView = it })
                }
            )
            // **"Paragraph"** (dal 21/10/2026, chiesto dall'utente, come lo
            // spacing del paragrafo di Word): lo spazio fra i blocchi e quello
            // dopo numeri, pallini e caselle, per tutte le pagine. Ogni pagina
            // può dire altro dai suoi tre puntini, e ogni gruppo di righe dal
            // pulsante ¶ della barra sopra la tastiera.
            SettingsItem(
                ParagraphStrings.paragraph,
                icon = Icons.Filled.FormatLineSpacing,
                value = paragraphSettingsSummary(),
                onClick = { dialog = SettingsDialog.PARAGRAPH }
            )
            // **"Open pages"** (dal 12/11/2026, solo sul PC, chiesto dall'utente
            // come in Notion): come si apre una pagina toccata in un collegamento
            // o in un database. Di partenza Center peek. Vedi `PageOpener`.
            if (isPc) {
                SettingsItem(
                    Strings.openPages,
                    icon = openModeIcon(AppSettings.openPagesAs),
                    value = Strings.openModeName(AppSettings.openPagesAs),
                    onClick = { dialog = SettingsDialog.OPEN_PAGES }
                )
            }
            SettingsItem(
                Strings.animations,
                icon = Icons.Filled.Animation,
                value = animationLabel(AppSettings.animations),
                onClick = { dialog = SettingsDialog.ANIMATIONS }
            )
            SettingsItem(
                Strings.numberFormat,
                icon = Icons.Filled.Numbers,
                value = Formats.numberSample(AppSettings.numberStyle),
                onClick = { dialog = SettingsDialog.NUMBER }
            )
            // **"Date format" è tornato qui** (dal 16/10/2026, chiesto
            // dall'utente: "L'opzione deve essere globale, non scelta
            // singolarmente"). Dal 26/09 si sceglieva per ogni colonna Date;
            // ora uno solo vale ovunque: celle e calcoli dei database, grafici,
            // cronologia, preferiti, cestino, quaderni.
            SettingsItem(
                Strings.dateFormat,
                icon = Icons.Filled.CalendarMonth,
                value = "${Formats.dateStyleName(AppSettings.dateStyle)} — ${Formats.sample(AppSettings.dateStyle, today())}",
                onClick = { dialog = SettingsDialog.DATE }
            )
            // **"Time format"** subito sotto (dal 16/10/2026, chiesto
            // dall'utente): anche lui uno solo per tutta l'app, non più per
            // ogni colonna Date.
            SettingsItem(
                DbStrings.timeFormat,
                icon = Icons.Filled.Schedule,
                value = DbStrings.timeFormatName(AppSettings.timeFormat),
                onClick = { dialog = SettingsDialog.TIME }
            )
            SettingsItem(
                Strings.language,
                icon = Icons.Filled.Language,
                value = languageName(AppSettings.language),
                onClick = { dialog = SettingsDialog.LANGUAGE }
            )
            // **Il correttore** (dal 08/10/2026), sotto la lingua dell'app:
            // acceso o spento in tutte le pagine, la sua lingua, le parole
            // aggiunte. Ogni pagina può dire altro dai suoi tre puntini.
            SettingsItem(
                ProofingStrings.proofing,
                icon = Icons.Filled.Spellcheck,
                value = if (AppSettings.proofingEnabled) {
                    "${ProofingStrings.on} · ${AppSettings.proofingLanguage.label}"
                } else {
                    ProofingStrings.off
                },
                onClick = { dialog = SettingsDialog.PROOFING }
            )
            SettingsItem(
                Strings.timeZone,
                icon = Icons.Filled.Public,
                value = zoneLabel(AppSettings.timeZoneId),
                onClick = { dialog = SettingsDialog.TIME_ZONE }
            )

            // La password delle pagine bloccate (dal 26/09/2026): cambiarla,
            // toglierla, e se si apre anche con l'impronta.
            SettingsItem(
                LockStrings.setPasswordTitle,
                icon = Icons.Filled.Fingerprint,
                value = if (AppLock.hasPassword) LockStrings.passwordSet else LockStrings.passwordNotSet,
                onClick = { dialog = SettingsDialog.LOCK_PASSWORD }
            )

            // **Dove finisce quello che si condivide con notE e le note al
            // volo** (dal 02/11/2026, chiesto dall'utente): una pagina scelta,
            // o Inbox, che nasce in fondo al menu principale la prima volta.
            if (factory != null) {
                SettingsItem(
                    ShareStrings.settingsTitle,
                    icon = Icons.Filled.MoveToInbox,
                    value = sharePageTitle ?: ShareStrings.inboxNote,
                    onClick = { pickingSharePage = true }
                )
            }

            // **Sotto il fuso orario, come ha chiesto l'utente** (26/09/2026):
            // svuotare la cache e cancellare tutto il contenuto. Separate
            // dal resto da una riga: non sono scelte, sono azioni.
            HorizontalDivider(color = NotionGray900, modifier = Modifier.padding(vertical = 12.dp))
            SettingsItem(
                Strings.clearCache,
                icon = Icons.Filled.CleaningServices,
                value = formatFileSize(cacheBytes),
                onClick = { dialog = SettingsDialog.CLEAR_CACHE }
            )
            SettingsItem(
                Strings.deleteAllContent,
                icon = Icons.Filled.DeleteForever,
                value = Strings.deleteAllContentNote,
                onClick = { dialog = SettingsDialog.DELETE_ALL }
            )

            // **"Patch Notes", in fondo** (dal 30/09/2026, chiesto
            // dall'utente): le versioni dell'app come sono scritte nella
            // Cronologia del README. Sotto, la versione di adesso.
            HorizontalDivider(color = NotionGray900, modifier = Modifier.padding(vertical = 12.dp))
            val appVersion = remember(context) { appVersionName(context) }
            SettingsItem(
                Strings.patchNotes,
                icon = Icons.Filled.NewReleases,
                value = appVersion?.let { "v$it" },
                onClick = onOpenPatchNotes
            )
        }
    }

    if (pickingSharePage && factory != null) {
        DestinationPickerSheet(
            factory = factory,
            title = ShareStrings.settingsTitle,
            hint = ShareStrings.pickerHint,
            viewModelKey = "share-settings",
            onClear = if (AppSettings.sharePageId != null) {
                {
                    pickingSharePage = false
                    AppSettings.sharePageId = null
                }
            } else {
                null
            },
            clearLabel = ShareStrings.inboxNote,
            clearIcon = Icons.Filled.MoveToInbox,
            clearIsDestructive = false,
            onDismiss = { pickingSharePage = false },
            onPick = { node ->
                pickingSharePage = false
                scope.launch(Dispatchers.Main.immediate) {
                    val id = node.pageId ?: withContext(Dispatchers.IO) { node.rowId?.let { factory.pageRepository.ensureRowPage(it) } } ?: return@launch
                    AppSettings.sharePageId = id
                }
            }
        )
    }
    when (dialog) {
        SettingsDialog.THEME -> ChoiceDialog(
            title = Strings.theme,
            options = ThemeMode.entries.map { it to themeLabel(it) },
            selected = AppSettings.themeMode,
            onSelect = { AppSettings.themeMode = it },
            onDismiss = { dialog = null }
        )
        SettingsDialog.TITLE_POSITION -> ChoiceDialog(
            title = Strings.titlePosition,
            subtitle = Strings.titlePositionNote,
            options = TitlePosition.entries.map { it to titlePositionLabel(it) },
            selected = AppSettings.titlePosition,
            onSelect = { AppSettings.titlePosition = it },
            onDismiss = { dialog = null }
        )
        SettingsDialog.OPEN_PAGES -> ChoiceDialog(
            title = Strings.openPages,
            options = OpenMode.entries.map { it to Strings.openModeName(it) },
            selected = AppSettings.openPagesAs,
            onSelect = { AppSettings.openPagesAs = it },
            onDismiss = { dialog = null }
        )
        SettingsDialog.ANIMATIONS -> ChoiceDialog(
            title = Strings.animations,
            options = AnimationLevel.entries.map { it to animationLabel(it) },
            selected = AppSettings.animations,
            onSelect = { AppSettings.animations = it },
            onDismiss = { dialog = null }
        )
        SettingsDialog.NUMBER -> ChoiceDialog(
            title = Strings.numberFormat,
            subtitle = Strings.numberFormatNote,
            // Nell'ordine dell'utente: spazio, niente, punto, punto in alto;
            // in fondo quello all'inglese, con la virgola.
            options = NumberStyle.entries.map { it to Formats.numberSample(it) },
            details = NumberStyle.entries.associateWith { Formats.numberDecimalSample(it) },
            selected = AppSettings.numberStyle,
            onSelect = { AppSettings.numberStyle = it },
            onDismiss = { dialog = null }
        )
        SettingsDialog.DATE -> ChoiceDialog(
            title = Strings.dateFormat,
            options = DateStyle.entries.map { it to Formats.dateStyleName(it) },
            details = DateStyle.entries.associateWith { Formats.sample(it, today()) },
            selected = AppSettings.dateStyle,
            onSelect = { AppSettings.dateStyle = it },
            onDismiss = { dialog = null }
        )
        SettingsDialog.TIME -> ChoiceDialog(
            title = DbStrings.timeFormat,
            // Nell'ordine di Notion: Hidden, 12 hour, 24 hour.
            options = listOf(TimeFormatKind.HIDDEN, TimeFormatKind.H12, TimeFormatKind.H24).map { it to DbStrings.timeFormatName(it) },
            details = mapOf(
                TimeFormatKind.HIDDEN to DbStrings.timeHiddenNote,
                TimeFormatKind.H12 to DateTimeFormatter.ofPattern("h:mm a", AppSettings.language.locale).format(LocalTime.now(AppSettings.zoneId)),
                TimeFormatKind.H24 to DateTimeFormatter.ofPattern("HH:mm").format(LocalTime.now(AppSettings.zoneId))
            ),
            selected = AppSettings.timeFormat,
            onSelect = { AppSettings.timeFormat = it },
            onDismiss = { dialog = null }
        )
        SettingsDialog.LANGUAGE -> ChoiceDialog(
            title = Strings.language,
            subtitle = Strings.languageNote,
            options = AppLanguage.entries.map { it to languageName(it) },
            selected = AppSettings.language,
            onSelect = { AppSettings.language = it },
            onDismiss = { dialog = null }
        )
        SettingsDialog.PROOFING -> ProofingSettingsDialog(onDismiss = { dialog = null })
        SettingsDialog.PARAGRAPH -> ParagraphSettingsDialog(onDismiss = { dialog = null })
        SettingsDialog.TIME_ZONE -> TimeZoneDialog(onDismiss = { dialog = null })
        SettingsDialog.LOCK_PASSWORD -> LockPasswordSettings(onDismiss = { dialog = null })
        SettingsDialog.CLEAR_CACHE -> FitAlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(Strings.clearCacheTitle) },
            text = { Text(Strings.clearCacheText) },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    scope.launch {
                        AppCleanup.clearCache(context)
                        cacheBytes = AppCleanup.cacheBytes(context)
                        showMessage(context, Strings.cacheCleared)
                    }
                }) { Text(Strings.clear) }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text(Strings.cancel) } }
        )
        SettingsDialog.DELETE_ALL -> DeleteAllDialog(
            onDismiss = { dialog = null },
            onConfirm = {
                scope.launch {
                    AppCleanup.deleteAllContent(context)
                    restartAppAfterReset(context)
                }
            }
        )
        null -> Unit
    }
}

private fun today(): LocalDate = LocalDate.now(AppSettings.zoneId)

/**
 * **"Elimina tutto il contenuto"**: una volta fatto non si torna indietro,
 * quindi il pulsante rosso si accende solo dopo aver **scritto la parola**
 * ("ELIMINA" in italiano). Un tocco sbagliato non deve bastare.
 */
@Composable
private fun DeleteAllDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var typed by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    val word = Strings.deleteAllWord
    val armed = typed.trim().equals(word, ignoreCase = true) && !working
    FitAlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        icon = { Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(Strings.deleteAllTitle) },
        text = {
            Column {
                Text(Strings.deleteAllText)
                Spacer(modifier = Modifier.height(16.dp))
                Text(Strings.typeToConfirm(word), color = NotionGray400, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                AutoCaps {
                    OutlinedTextField(
                        value = typed,
                        onValueChange = { typed = it },
                        singleLine = true,
                        enabled = !working,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    working = true
                    onConfirm()
                },
                enabled = armed
            ) {
                Text(
                    Strings.deleteEverything,
                    color = if (armed) MaterialTheme.colorScheme.error else NotionGray400
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !working) { Text(Strings.cancel) } }
    )
}

/**
 * **La password delle pagine bloccate**, dalle impostazioni. Senza password
 * si sceglie; con la password: l'impronta sì o no, cambiarla (prima si
 * conferma che si è noi) e toglierla — solo quando nessuna pagina è più
 * bloccata, altrimenti quelle resterebbero chiuse per sempre.
 */
@Composable
private fun LockPasswordSettings(onDismiss: () -> Unit) {
    val context = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    // 0 il menu, 1 conferma per cambiarla, 2 password nuova, 3 conferma per toglierla
    var step by remember { mutableStateOf(if (AppLock.hasPassword) 0 else 2) }
    when (step) {
        0 -> FitAlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Filled.Fingerprint, contentDescription = null) },
            title = { Text(LockStrings.setPasswordTitle) },
            text = {
                Column {
                    if (Biometrics.available(context)) {
                        var fingerprint by remember { mutableStateOf(AppLock.biometricEnabled) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(LockStrings.alsoFingerprint, modifier = Modifier.weight(1f))
                            Switch(checked = fingerprint, onCheckedChange = {
                                fingerprint = it
                                AppLock.biometricEnabled = it
                            })
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    TextButton(onClick = { step = 1 }) { Text(LockStrings.changePassword) }
                    TextButton(onClick = {
                        scope.launch {
                            val locked = withContext(Dispatchers.IO) {
                                // Anche le sezioni dei quaderni: senza password resterebbero chiuse.
                                AppDatabase.getInstance(context).let {
                                    it.pageDao().countProtected() + it.notebookDao().countProtectedSections()
                                }
                            }
                            if (locked > 0) {
                                showMessage(context, LockStrings.removePasswordBlocked, long = true)
                            } else {
                                step = 3
                            }
                        }
                    }) { Text(LockStrings.removePassword, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text(Strings.done) } }
        )
        1 -> UnlockPrompt(onUnlocked = { step = 2 }, onDismiss = onDismiss)
        2 -> SetPasswordDialog(
            onSaved = {
                showMessage(context, LockStrings.passwordChanged)
                onDismiss()
            },
            onDismiss = onDismiss
        )
        3 -> UnlockPrompt(
            onUnlocked = {
                AppLock.removePassword()
                showMessage(context, LockStrings.passwordRemoved)
                onDismiss()
            },
            onDismiss = onDismiss
        )
    }
}

private fun themeLabel(mode: ThemeMode) = when (mode) {
    ThemeMode.DARK -> Strings.themeDark
    ThemeMode.LIGHT -> Strings.themeLight
    ThemeMode.SYSTEM -> Strings.themeSystem
}

private fun titlePositionLabel(position: TitlePosition) = when (position) {
    TitlePosition.BELOW_ICON -> Strings.titleBelowIcon
    TitlePosition.BESIDE_ICON -> Strings.titleBesideIcon
    TitlePosition.HIDDEN -> Strings.titleHidden
}

private fun animationLabel(level: AnimationLevel) = when (level) {
    AnimationLevel.FULL -> Strings.animationsOn
    AnimationLevel.REDUCED -> Strings.animationsReduced
    AnimationLevel.OFF -> Strings.animationsOff
}

/**
 * Il nome di ogni lingua **nella sua lingua**: chi ha messo per sbaglio il
 * coreano deve poter ritrovare "Italiano" senza saper leggere il coreano.
 */
private fun languageName(language: AppLanguage) = when (language) {
    AppLanguage.ENGLISH -> "English"
    AppLanguage.ITALIAN -> "Italiano"
    AppLanguage.GERMAN -> "Deutsch"
    AppLanguage.FRENCH -> "Français"
    AppLanguage.SPANISH -> "Español"
    AppLanguage.CHINESE -> "中文"
    AppLanguage.KOREAN -> "한국어"
    AppLanguage.JAPANESE -> "日本語"
}

/** "Berlin (UTC+02:00)": la città, e di quanto è avanti adesso — ora legale compresa. */
private fun zoneLabel(id: String): String {
    val zone = runCatching { ZoneId.of(id) }.getOrNull() ?: return id
    val offset = zone.rules.getOffset(Instant.now())
    val city = id.substringAfterLast('/').replace('_', ' ')
    val region = id.substringBefore('/', missingDelimiterValue = "")
    val name = if (region.isNotEmpty()) "$city · $region" else zone.getDisplayName(TextStyle.FULL, AppSettings.language.locale)
    return "$name (UTC${if (offset.totalSeconds == 0) "" else offset.id})"
}

/** Una scelta fra poche voci: tocchi, si salva, si chiude. */
@Composable
fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    details: Map<T, String> = emptyMap()
) {
    FitAlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSheet,
        title = { Text(title, color = NotionWhite) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (subtitle != null) {
                    Text(
                        subtitle,
                        color = NotionGray400,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                options.forEach { (value, label) ->
                    RadioItem(
                        title = label,
                        detail = details[value],
                        selected = value == selected,
                        onClick = {
                            onSelect(value)
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}

@Composable
fun DialogAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = NotionWhite,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    )
}

/** Notifiche spente per un po': un'ora, otto ore, un giorno, una settimana. */
@Composable
fun MuteForDialog(onDismiss: () -> Unit) {
    val hour = 60L * 60 * 1000
    val options = listOf(
        Strings.forOneHour to hour,
        Strings.forEightHours to 8 * hour,
        Strings.forOneDay to 24 * hour,
        Strings.forOneWeek to 7 * 24 * hour
    )
    FitAlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSheet,
        title = { Text(Strings.turnOffForAWhile, color = NotionWhite) },
        text = {
            Column {
                options.forEach { (label, duration) ->
                    DialogAction(label) {
                        AppSettings.mutedUntil = System.currentTimeMillis() + duration
                        onDismiss()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}

/**
 * Il fuso orario, cercandolo per città o regione. Si elencano solo i fusi
 * "Regione/Città" (Europe/Berlin), non le sigle come CET: le sigle non
 * seguono l'ora legale allo stesso modo, e la città è quello che uno cerca.
 */
@Composable
private fun TimeZoneDialog(onDismiss: () -> Unit) {
    val all = remember {
        ZoneId.getAvailableZoneIds()
            .filter { it.contains('/') && !it.startsWith("Etc/") && !it.startsWith("SystemV/") }
            .sorted()
    }
    var filter by remember { mutableStateOf("") }
    val shown = remember(filter) {
        val needle = filter.trim().replace(' ', '_')
        if (needle.isEmpty()) all else all.filter { it.contains(needle, ignoreCase = true) }
    }
    FitAlertDialog(
        fitContent = false,
        onDismissRequest = onDismiss,
        containerColor = DarkSheet,
        title = { Text(Strings.timeZone, color = NotionWhite) },
        text = {
            Column {
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    if (filter.isEmpty()) {
                        Text(Strings.searchTimeZones, color = NotionGray400, style = MaterialTheme.typography.bodyLarge)
                    }
                    AutoCaps {
                        BasicTextField(
                            value = filter,
                            onValueChange = { filter = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = NotionWhite),
                            cursorBrush = SolidColor(NotionWhite),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                HorizontalDivider(color = NotionGray900)
                LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                    items(shown, key = { it }) { id ->
                        RadioItem(
                            title = zoneLabel(id),
                            selected = id == AppSettings.timeZoneId,
                            onClick = {
                                AppSettings.timeZoneId = id
                                onDismiss()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}
