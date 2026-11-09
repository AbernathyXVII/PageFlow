package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.Composable
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.patchnotes.PatchNotesAsset
import com.gabriele.notionlocal.platform.PlatformContext
import kotlin.system.exitProcess

// shortcut: sul PC le impostazioni delle notifiche non ci sono, finché non ci sono le notifiche di Windows.
@Composable
actual fun NotificationSettingsSection() = Unit

actual fun restartAppAfterReset(context: PlatformContext) {
    AppDatabase.closeInstance()
    exitProcess(0)
}

actual fun appVersionName(context: PlatformContext): String? =
    runCatching { PatchNotesAsset.load(context).versions.firstOrNull()?.version }.getOrNull()

actual val hasNotificationSettings: Boolean = false
