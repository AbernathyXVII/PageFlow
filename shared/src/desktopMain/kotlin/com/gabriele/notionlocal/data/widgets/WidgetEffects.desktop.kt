package com.gabriele.notionlocal.data.widgets

import com.gabriele.notionlocal.data.notify.DesktopNotifications
import com.gabriele.notionlocal.data.notify.NotificationTarget
import com.gabriele.notionlocal.platform.PlatformContext
import com.gabriele.notionlocal.ui.i18n.NotifyStrings
import com.gabriele.notionlocal.ui.i18n.WidgetStrings

// shortcut: sul PC niente notifica fissa del timer che corre (sul telefono c'è); il tempo si vede nella barra laterale.
internal actual fun syncTimerNotifications(context: PlatformContext, before: WidgetsState, after: WidgetsState) = Unit

internal actual fun restoreTimerNotifications(context: PlatformContext, state: WidgetsState) = Unit

/**
 * Timer finito, pomodoro che cambia fase: le stesse notifiche del telefono
 * (`Notifier.postWidgetEvents`) e, una volta per tutte, la sveglia
 * (`DesktopSounds.alarm`: il suono scelto per il pomodoro o quello di PageFlow).
 */
internal actual fun postWidgetEvents(context: PlatformContext, events: List<WidgetEvent>, pomodoroSound: String?) {
    if (events.isEmpty() || DesktopNotifications.muted) return
    DesktopSounds.alarm(pomodoroSound)
    events.forEach { event ->
        val (title, body) = when (event) {
            is WidgetEvent.TimerDone -> NotifyStrings.timeIsUp to NotifyStrings.timerOf(durationLabel(event.durationMs))
            is WidgetEvent.PomodoroPhaseEnded -> {
                val session = event.sessionName.ifBlank { WidgetStrings.session }
                when (event.nowIn) {
                    PomodoroPhase.BREAK -> session to NotifyStrings.breakTime(event.nextMinutes)
                    PomodoroPhase.SESSION -> NotifyStrings.breakOver to NotifyStrings.backTo(session, event.nextMinutes)
                }
            }
        }
        DesktopNotifications.show(title, body) { NotificationTarget.openWidgets.value = true }
    }
}

// Sul PC l'attesa del prossimo timer è già dentro `WidgetStore`: niente sveglia da rimettere.
internal actual fun rescheduleWidgetAlarm() = Unit
