package com.gabriele.notionlocal.data.notify

import com.gabriele.notionlocal.platform.PlatformContext

/** "Send notification" di un pulsante: come sul telefono arriva anche a notifiche messe a tacere, l'ha chiesta un clic. */
actual fun postButtonNotification(context: PlatformContext, title: String, text: String): Boolean =
    DesktopNotifications.show(title, text)
