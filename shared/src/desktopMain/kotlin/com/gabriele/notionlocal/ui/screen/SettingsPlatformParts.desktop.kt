package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.Composable
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.notify.DesktopNotifications
import com.gabriele.notionlocal.data.patchnotes.PatchNotesAsset
import com.gabriele.notionlocal.platform.PlatformContext
import java.io.File
import kotlin.system.exitProcess

// shortcut: sul PC la sezione delle notifiche non c'è (suono, priorità, spegnerle per un po'); arrivano sempre.
@Composable
actual fun NotificationSettingsSection() = Unit

actual fun restartAppAfterReset(context: PlatformContext) = relaunchApp()

/**
 * **Chiude PageFlow e la riapre**, se è quella installata: la riapertura fa il
 * lavoro rimasto (un ripristino da mettere al suo posto, un database vuoto).
 * Aperta da Gradle (le prove) si chiude e basta.
 */
internal fun relaunchApp() {
    AppDatabase.closeInstance()
    DesktopNotifications.close()
    ProcessHandle.current().info().command().orElse(null)
        ?.takeIf { File(it).name.equals("PageFlow.exe", ignoreCase = true) }
        ?.let { runCatching { ProcessBuilder(it).start() } }
    exitProcess(0)
}

actual fun appVersionName(context: PlatformContext): String? =
    runCatching { PatchNotesAsset.load(context).versions.firstOrNull()?.version }.getOrNull()

actual val hasNotificationSettings: Boolean = false
