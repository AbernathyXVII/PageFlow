package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.Composable
import com.gabriele.notionlocal.platform.PlatformContext

/**
 * **La sezione "Notifications" delle impostazioni**: sul telefono quella di
 * sempre (`NotificationSettings.kt`); sul PC, dal 10/11/2026, la prova e
 * spegnerle (`SettingsPlatformParts.desktop.kt`).
 */
@Composable
expect fun NotificationSettingsSection()

/**
 * Fa ripartire l'app da capo dopo "Delete all content": le schermate aperte
 * hanno ancora in mano il contenuto di prima (un Annulla rimetterebbe i
 * blocchi cancellati). Sul telefono riapre l'attività; sul PC chiude
 * PageFlow, che si riapre su una My Space vuota.
 */
expect fun restartAppAfterReset(context: PlatformContext)

/** La versione dell'app ("0.153.17"): sul telefono quella dell'APK, sul PC quella in cima alle Patch Notes. */
expect fun appVersionName(context: PlatformContext): String?
