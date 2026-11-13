package com.gabriele.notionlocal.desktop

import com.gabriele.notionlocal.ui.navigation.Shortcuts
import com.gabriele.notionlocal.ui.navigation.PcWindowActions
import com.gabriele.notionlocal.ui.navigation.PcTabs
import com.gabriele.notionlocal.ui.navigation.LocalShortcuts
import com.gabriele.notionlocal.ui.navigation.LocalPcWindow
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import com.gabriele.notionlocal.ui.theme.zoomKeyOf
import com.gabriele.notionlocal.ui.theme.ctrlWheelZoom
import com.gabriele.notionlocal.ui.theme.PcZoom
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.gabriele.notionlocal.data.backup.Backups
import com.gabriele.notionlocal.data.entity.RollupFormatting
import com.gabriele.notionlocal.data.ink.InkPrefs
import com.gabriele.notionlocal.data.notify.DesktopNotifications
import com.gabriele.notionlocal.data.notify.DesktopReminders
import com.gabriele.notionlocal.data.ocr.DesktopImageTexts
import com.gabriele.notionlocal.data.record.AudioRecorder
import com.gabriele.notionlocal.data.security.AppLock
import com.gabriele.notionlocal.data.sync.SyncPrompts
import com.gabriele.notionlocal.data.sync.SyncServer
import com.gabriele.notionlocal.data.sync.syncSide
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.widgets.WidgetStore
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.platform.PcEmoji
import com.gabriele.notionlocal.ui.navigation.AppNavHost
import com.gabriele.notionlocal.ui.navigation.AppShortcuts
import com.gabriele.notionlocal.ui.screen.AppRollupFormatter
import com.gabriele.notionlocal.ui.screen.Biometrics
import com.gabriele.notionlocal.ui.screen.notePresses
import com.gabriele.notionlocal.ui.i18n.WifiSyncStrings
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.skia.Image
import java.awt.EventQueue

/**
 * **PageFlow sul PC** (dal 04/11/2026): la stessa app del telefono, con lo
 * stesso codice (`shared`). Dal 09/11/2026 la finestra apre la home, la barra
 * laterale e le pagine (`AppNavHost`), come il telefono; quello che è solo
 * del telefono (i suoni…) sul PC per ora manca.
 */
fun main() {
    // Le emoji di Google al posto di quelle di Windows, prima di qualunque testo (vedi `PcEmoji`).
    PcEmoji.install()
    val context = DesktopContext()
    // Un backup del telefono scelto in "Backup": al suo posto prima che si legga qualcosa.
    Backups.applyPendingRestore(context)
    // Come `MainActivity` sul telefono, per quello che c'è anche sul PC.
    AppSettings.init(context)
    WidgetStore.init(context)
    InkPrefs.init(context)
    AppLock.init(context)
    // I promemoria delle pagine ("/notification", le date con la notifica), come `NoteAlarms` sul telefono.
    DesktopReminders.init(context)
    // I backup: quello di ogni giorno e il promemoria per caricarne uno (vedi `Backups`).
    Backups.init(context)
    Backups.startDailyTasks()
    // Il testo delle foto aggiunte sul PC, per la ricerca (vedi `DesktopImageTexts`).
    DesktopImageTexts.init(context)
    // Se Windows Hello è pronto, per le pagine bloccate (vedi `Biometrics`).
    Biometrics.init(context)
    // La sincronizzazione Wi‑Fi: se il PC aspetta il telefono, comincia subito (vedi `SyncServer`).
    val sync = syncSide(context)
    SyncServer.onSynced = { result ->
        DesktopNotifications.show(
            WifiSyncStrings.syncedWith(result.otherName),
            WifiSyncStrings.done(result.otherName, result.sent, result.received, result.trashedHere, result.conflicts, result.filesSent + result.filesReceived)
        )
    }
    // Un telefono nuovo chiede il codice: la finestra torna davanti, con la domanda.
    SyncPrompts.onRequest = { EventQueue.invokeLater { DesktopNotifications.bringToFront() } }
    if (sync.store.load().acceptPhones) SyncServer.start(sync)
    RollupFormatting.formatter = AppRollupFormatter
    val factory = ViewModelFactory(context)
    val icon = PlatformIcon.load()
    val windowMemory = WindowMemory(context)
    application {
        val windowState = remember { windowMemory.initialState() }
        // Mentre la si sposta o la si allarga, la finestra si ricorda com'è (poco dopo l'ultimo movimento).
        LaunchedEffect(windowState) {
            snapshotFlow { Triple(windowState.placement, windowState.position, windowState.size) }.collectLatest {
                delay(WINDOW_SAVE_DELAY_MS)
                windowMemory.save(windowState)
            }
        }
        // **Più finestre** (dal 13/11/2026, chiesto dall'utente): una scheda trascinata fuori
        // dalla barra si apre in una finestra sua (vedi `PcTabBar`). La prima è quella di sempre.
        val windows = remember { mutableStateListOf(PcWindow(0, PcTabs.load(), windowState)) }
        var nextWindowId by remember { mutableIntStateOf(1) }
        fun close(closing: PcWindow) {
            if (closing.state === windowState) windowMemory.save(windowState)
            windows.remove(closing)
            if (windows.isEmpty()) {
                // Senza togliere l'icona dall'area di notifica il programma resterebbe acceso.
                AudioRecorder.stopBeforeExit(context)
                SyncServer.stop()
                DesktopNotifications.close()
                exitApplication()
            } else if (closing.tabs.persist) {
                // Le schede da ritrovare alla volta dopo diventano quelle di una finestra che resta.
                windows.first().tabs.persist = true
            }
        }
        windows.forEach { pcWindow -> key(pcWindow.id) {
        Window(
            onCloseRequest = { close(pcWindow) },
            title = "PageFlow",
            icon = icon,
            state = pcWindow.state,
            // Le scorciatoie da tastiera (vedi `AppShortcuts`): la finestra le riceve anche senza un cursore.
            onPreviewKeyEvent = { event ->
                // Lo zoom come nei browser: Ctrl + e Ctrl − (vedi `PcZoom`).
                zoomKeyOf(event)?.let { bigger ->
                    PcZoom.step(bigger)
                    return@Window true
                }
                val shortcuts = pcWindow.shortcuts
                val shortcut = when {
                    event.type != KeyEventType.KeyDown || !event.isCtrlPressed -> null
                    event.key == Key.F -> shortcuts.openSearch
                    event.key == Key.N -> shortcuts.newPage
                    // Le schede, come nei browser (vedi `PcTabs`).
                    event.key == Key.T -> shortcuts.newTab
                    event.key == Key.W -> shortcuts.closeTab
                    else -> null
                }
                shortcut?.invoke()
                shortcut != null
            }
        ) {
            // Cliccando una notifica la finestra torna davanti, anche se era ridotta a icona.
            if (pcWindow.state === windowState) {
                LaunchedEffect(Unit) {
                    Biometrics.windowHandle = window.windowHandle
                    DesktopNotifications.bringToFront = {
                        windowState.isMinimized = false
                        window.toFront()
                    }
                }
            }
            // La barra in alto scura come l'app, col vetro di Windows 11 (vedi `WindowFrame`).
            val dark = AppSettings.isDark
            LaunchedEffect(dark) { WindowFrame.style(window.windowHandle, dark) }
            PcEmoji.Load()
            // Una scheda staccata: una finestra nuova, grande come questa, col mouse sulla sua scheda.
            val tearOff = remember(window) {
                PcWindowActions { tab, pointer ->
                    val scale = window.graphicsConfiguration.defaultTransform.scaleX.toFloat()
                    val origin = window.contentPane.locationOnScreen
                    val position = WindowPosition(
                        (origin.x + pointer.x / scale - TORN_TAB_GRAB_X).dp,
                        (origin.y + pointer.y / scale - TORN_TAB_GRAB_Y).dp
                    )
                    windows.add(PcWindow(nextWindowId++, PcTabs.detached(tab), WindowState(position = position, size = pcWindow.state.size)))
                }
            }
            CompositionLocalProvider(
                LocalPlatformContext provides context,
                LocalShortcuts provides pcWindow.shortcuts,
                LocalPcWindow provides tearOff
            ) {
                NotionLocalTheme {
                    // Tutta la finestra, come Notion (chiesto dall'utente il 09/11/2026): la barra
                    // laterale attaccata al bordo sinistro, la pagina larga quanto lo spazio che resta.
                    // I clic segnati: le finestrelle si aprono lì accanto (vedi `PcPanel.desktop.kt`).
                    Surface(modifier = Modifier.fillMaxSize().notePresses().ctrlWheelZoom()) {
                        AppNavHost(factory = factory, windowTabs = pcWindow.tabs)
                    }
                }
            }
        }
        } }
    }
}

/** Una finestra di PageFlow: le sue schede, dove sta e le sue scorciatoie. */
private class PcWindow(val id: Int, val tabs: PcTabs, val state: WindowState) {
    val shortcuts = Shortcuts()
}

/** Dove sta il mouse nella finestra nuova di una scheda staccata: sopra la sua scheda, sotto la barra di Windows. */
private const val TORN_TAB_GRAB_X = 80f
private const val TORN_TAB_GRAB_Y = 16f

private const val WINDOW_SAVE_DELAY_MS = 500L

/** L'icona di PageFlow, la stessa del telefono (cartella `branding`). */
private object PlatformIcon {
    fun load(): BitmapPainter? {
        val bytes = javaClass.getResourceAsStream("/pageflow-icon-512.png")?.use { it.readBytes() } ?: return null
        return BitmapPainter(Image.makeFromEncoded(bytes).toComposeImageBitmap())
    }
}
