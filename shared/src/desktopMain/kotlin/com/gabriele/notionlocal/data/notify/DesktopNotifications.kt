package com.gabriele.notionlocal.data.notify

import com.gabriele.notionlocal.data.settings.AppSettings
import java.awt.SystemTray
import java.awt.TrayIcon
import javax.imageio.ImageIO

/**
 * **Le notifiche sul PC** (dal 09/11/2026): l'icona di PageFlow nell'area di
 * notifica di Windows (accanto all'orologio), che mostra i messaggi come
 * notifiche del sistema. Cliccandone una si apre quello che riguarda — la
 * pagina del promemoria, i widget — come toccando la notifica sul telefono
 * (`NotificationTarget`), e la finestra torna davanti.
 *
 * shortcut: le notifiche arrivano solo con PageFlow aperta (anche ridotta a
 * icona); per averle a finestra chiusa servirebbe restare nell'area di notifica.
 */
object DesktopNotifications {
    private var tray: TrayIcon? = null

    /** Cosa fare cliccando l'ultima notifica. */
    private var lastClick: () -> Unit = {}

    /** La finestra davanti (la mette `Main.kt`). */
    var bringToFront: () -> Unit = {}

    /** Le notifiche messe a tacere dalle impostazioni, come sul telefono (`Notifier.muted`). */
    val muted: Boolean get() = AppSettings.notificationsMuted || AppSettings.mutedUntil != null

    /** Mostra una notifica; cliccandola [onClick] (aprire la pagina…). Falso se il sistema non ha l'area di notifica. */
    @Synchronized
    fun show(title: String, text: String, onClick: () -> Unit = {}): Boolean = runCatching {
        val icon = tray ?: createTray() ?: return false
        lastClick = onClick
        icon.displayMessage(title, text, TrayIcon.MessageType.NONE)
        true
    }.getOrDefault(false)

    private fun createTray(): TrayIcon? {
        if (!SystemTray.isSupported()) return null
        val image = DesktopNotifications::class.java.getResourceAsStream("/pageflow-icon-192.png")?.use { ImageIO.read(it) } ?: return null
        return TrayIcon(image, "PageFlow").apply {
            isImageAutoSize = true
            // Il clic sulla notifica (o sull'icona): quello che riguarda, e la finestra davanti.
            addActionListener {
                lastClick()
                // Una volta sola: un altro clic sull'icona porta solo la finestra davanti.
                lastClick = {}
                bringToFront()
            }
            SystemTray.getSystemTray().add(this)
            tray = this
        }
    }

    /** Toglie l'icona dall'area di notifica: senza, chiusa la finestra il programma resterebbe acceso. */
    @Synchronized
    fun close() {
        tray?.let { runCatching { SystemTray.getSystemTray().remove(it) } }
        tray = null
    }
}
