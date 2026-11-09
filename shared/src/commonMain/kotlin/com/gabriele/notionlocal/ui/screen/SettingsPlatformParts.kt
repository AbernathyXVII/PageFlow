package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.Composable
import com.gabriele.notionlocal.platform.PlatformContext

/**
 * **La sezione "Notifications" delle impostazioni**: sul telefono quella di
 * sempre (`NotificationSettings.kt`); sul PC, finché le notifiche di Windows
 * non ci sono, non c'è.
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

/** Se le impostazioni hanno la sezione "Notifications" (sul PC, per ora, no: niente titolo vuoto). */
expect val hasNotificationSettings: Boolean
