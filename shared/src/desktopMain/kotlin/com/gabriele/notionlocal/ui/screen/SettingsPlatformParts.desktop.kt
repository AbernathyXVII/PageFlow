package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.platform.showMessage
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.NotifyStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.notify.DesktopNotifications
import com.gabriele.notionlocal.data.patchnotes.PatchNotesAsset
import com.gabriele.notionlocal.platform.PlatformContext
import java.io.File
import kotlin.system.exitProcess

/**
 * **Le notifiche nelle Impostazioni del PC** (dal 10/11/2026): la prova,
 * spegnerle e spegnerle per un po', come sul telefono. Suono, vibrazione,
 * priorità e pallino sono di Android: sul PC le notifiche sono quelle di
 * Windows, col suo suono (vedi `DesktopNotifications`).
 */
@Composable
actual fun NotificationSettingsSection() {
    val context = LocalPlatformContext.current
    var muteFor by remember { mutableStateOf(false) }
    SettingsItem(
        NotifyStrings.sendTest,
        icon = Icons.Filled.NotificationsActive,
        onClick = {
            if (!DesktopNotifications.show(NotifyStrings.testTitle, NotifyStrings.testText)) showMessage(context, NotifyStrings.unavailableOnPc)
        }
    )
    SwitchItem(Strings.turnOffNotifications, checked = AppSettings.notificationsMuted) { AppSettings.notificationsMuted = it }
    val mutedUntil = AppSettings.mutedUntil
    SettingsItem(
        Strings.turnOffForAWhile,
        icon = Icons.Filled.Timer,
        value = mutedUntil?.let { Strings.offUntil(Formats.dateTime(it)) },
        enabled = !AppSettings.notificationsMuted,
        onClick = { muteFor = true },
        trailing = if (mutedUntil != null) {
            { TextButton(onClick = { AppSettings.mutedUntil = null }) { Text(Strings.turnBackOn) } }
        } else {
            null
        }
    )
    Text(
        NotifyStrings.onPcNote,
        color = NotionGray400,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
    )
    if (muteFor) MuteForDialog(onDismiss = { muteFor = false })
}

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
