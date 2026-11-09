package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.gabriele.notionlocal.data.backup.Backups
import com.gabriele.notionlocal.data.entity.RollupFormatting
import com.gabriele.notionlocal.data.ink.InkPrefs
import com.gabriele.notionlocal.data.notify.DesktopNotifications
import com.gabriele.notionlocal.data.notify.DesktopReminders
import com.gabriele.notionlocal.data.security.AppLock
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.widgets.WidgetStore
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.navigation.AppNavHost
import com.gabriele.notionlocal.ui.navigation.AppShortcuts
import com.gabriele.notionlocal.ui.screen.AppRollupFormatter
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import org.jetbrains.skia.Image

/**
 * **PageFlow sul PC** (dal 04/11/2026): la stessa app del telefono, con lo
 * stesso codice (`shared`). Dal 09/11/2026 la finestra apre la home, la barra
 * laterale e le pagine (`AppNavHost`), come il telefono; quello che è solo
 * del telefono (i suoni…) sul PC per ora manca.
 */
fun main() {
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
    RollupFormatting.formatter = AppRollupFormatter
    val factory = ViewModelFactory(context)
    val icon = PlatformIcon.load()
    application {
        val windowState = rememberWindowState(width = 520.dp, height = 900.dp)
        Window(
            // Senza togliere l'icona dall'area di notifica il programma resterebbe acceso.
            onCloseRequest = {
                DesktopNotifications.close()
                exitApplication()
            },
            title = "PageFlow",
            icon = icon,
            state = windowState,
            // Le scorciatoie da tastiera (vedi `AppShortcuts`): la finestra le riceve anche senza un cursore.
            onPreviewKeyEvent = { event ->
                if (event.type == KeyEventType.KeyDown && event.isCtrlPressed && event.key == Key.F) {
                    AppShortcuts.openSearch?.invoke()
                    true
                } else {
                    false
                }
            }
        ) {
            // Cliccando una notifica la finestra torna davanti, anche se era ridotta a icona.
            LaunchedEffect(Unit) {
                DesktopNotifications.bringToFront = {
                    windowState.isMinimized = false
                    window.toFront()
                }
            }
            CompositionLocalProvider(LocalPlatformContext provides context) {
                NotionLocalTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        // A finestra larga l'app sta al centro, larga come una pagina e non come lo schermo.
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            Box(modifier = Modifier.widthIn(max = PAGE_MAX_WIDTH).fillMaxWidth().fillMaxHeight()) {
                                AppNavHost(factory = factory)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Quanto può essere larga l'app nella finestra: oltre, ai lati resta lo sfondo. */
private val PAGE_MAX_WIDTH = 900.dp

/** L'icona di PageFlow, la stessa del telefono (cartella `branding`). */
private object PlatformIcon {
    fun load(): BitmapPainter? {
        val bytes = javaClass.getResourceAsStream("/pageflow-icon-512.png")?.use { it.readBytes() } ?: return null
        return BitmapPainter(Image.makeFromEncoded(bytes).toComposeImageBitmap())
    }
}
