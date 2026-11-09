package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.gabriele.notionlocal.data.backup.DesktopBackups
import com.gabriele.notionlocal.data.entity.RollupFormatting
import com.gabriele.notionlocal.data.ink.InkPrefs
import com.gabriele.notionlocal.data.security.AppLock
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.widgets.WidgetStore
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.navigation.AppNavHost
import com.gabriele.notionlocal.ui.screen.AppRollupFormatter
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import org.jetbrains.skia.Image

/**
 * **PageFlow sul PC** (dal 04/11/2026): la stessa app del telefono, con lo
 * stesso codice (`shared`). Dal 09/11/2026 la finestra apre la home, la barra
 * laterale e le pagine (`AppNavHost`), come il telefono; quello che è solo
 * del telefono (notifiche, backup, impostazioni…) sul PC per ora manca.
 */
fun main() {
    val context = DesktopContext()
    // Un backup del telefono scelto in "Backup": al suo posto prima che si legga qualcosa.
    DesktopBackups.applyPendingRestore(context)
    // Come `MainActivity` sul telefono, per quello che c'è anche sul PC.
    AppSettings.init(context)
    WidgetStore.init(context)
    InkPrefs.init(context)
    AppLock.init(context)
    RollupFormatting.formatter = AppRollupFormatter
    val factory = ViewModelFactory(context)
    val icon = PlatformIcon.load()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "PageFlow",
            icon = icon,
            state = rememberWindowState(width = 520.dp, height = 900.dp)
        ) {
            CompositionLocalProvider(LocalPlatformContext provides context) {
                NotionLocalTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavHost(factory = factory)
                    }
                }
            }
        }
    }
}

/** L'icona di PageFlow, la stessa del telefono (cartella `branding`). */
private object PlatformIcon {
    fun load(): BitmapPainter? {
        val bytes = javaClass.getResourceAsStream("/pageflow-icon-512.png")?.use { it.readBytes() } ?: return null
        return BitmapPainter(Image.makeFromEncoded(bytes).toComposeImageBitmap())
    }
}
