package com.gabriele.notionlocal.data.notify

import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.settings.openSettingsStore
import com.gabriele.notionlocal.platform.PlatformContext
import com.gabriele.notionlocal.ui.i18n.NotifyStrings
import com.gabriele.notionlocal.ui.i18n.ReminderTexts
import com.gabriele.notionlocal.ui.i18n.Strings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * **I promemoria delle pagine sul PC** (dal 09/11/2026): "/notification" in
 * una pagina e le date dei database con "Include notification", come sul
 * telefono (`NoteAlarms`), con lo stesso calcolo (`loadReminders`) e lo stesso
 * "fin dove si è già mandato" (`deliveredUpTo`, nelle impostazioni
 * "notifications"): una notifica non arriva due volte, e una persa a PC spento
 * arriva alla riapertura, se non è passata da più di 12 ore.
 *
 * Al posto della sveglia di Android c'è un'attesa dentro l'app fino al
 * prossimo promemoria: arriva finché PageFlow è aperta (vedi `DesktopNotifications`).
 */
object DesktopReminders {
    private const val PREFS = "notifications"
    private const val KEY_DELIVERED_UP_TO = "deliveredUpTo"
    private const val MISSED_WINDOW_MS = 12 * 60 * 60 * 1000L
    private const val HORIZON_MS = 3L * 366 * 24 * 60 * 60 * 1000L
    private const val DEBOUNCE_MS = 400L
    private const val MAX_WAIT_MS = 60_000L

    private lateinit var context: PlatformContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var waiting: Job? = null

    /** All'avvio: le repository chiedono di qui di rimettere l'attesa, e si guarda subito. */
    fun init(context: PlatformContext) {
        this.context = context
        ReminderAlarms.register(::reschedule)
        reschedule()
    }

    /** Fra un attimo (chi chiama ha appena scritto nel database): manda quello che è arrivato e aspetta il prossimo. */
    @Synchronized
    fun reschedule() {
        if (!::context.isInitialized) return
        waiting?.cancel()
        waiting = scope.launch {
            delay(DEBOUNCE_MS)
            val now = System.currentTimeMillis()
            val due = runCatching { takeDue(context, now) }.getOrDefault(emptyList())
            if (!DesktopNotifications.muted) {
                due.forEach { DesktopNotifications.show(it.title, it.body, OpenRequest(it.pageId, it.rowId, it.databaseId)) }
            }
            val next = runCatching {
                loadReminders(AppDatabase.getInstance(context), AppSettings.zoneId, now, now + HORIZON_MS, words()).minOfOrNull { it.at }
            }.getOrNull() ?: return@launch
            // Al massimo un minuto: dopo la sospensione del PC o un cambio d'orologio l'attesa sarebbe sbagliata.
            delay((next - System.currentTimeMillis()).coerceIn(0, MAX_WAIT_MS))
            reschedule()
        }
    }

    /** I promemoria arrivati dall'ultima volta a [now], in ordine; da qui in poi sono dati per mandati. */
    suspend fun takeDue(context: PlatformContext, now: Long): List<ReminderFire> {
        val prefs = openSettingsStore(context, PREFS)
        // La prima volta è adesso: le date passate prima delle notifiche non arrivano tutte insieme.
        val from = prefs.getLong(KEY_DELIVERED_UP_TO, 0L).takeIf { it > 0L } ?: now
        val due = loadReminders(AppDatabase.getInstance(context), AppSettings.zoneId, from, now, words())
            .filter { now - it.at <= MISSED_WINDOW_MS }
            .sortedBy { it.at }
        prefs.edit().putLong(KEY_DELIVERED_UP_TO, now).apply()
        return due
    }

    private fun words() = ReminderWords(
        untitled = Strings.untitled,
        starts = NotifyStrings.starts,
        ends = NotifyStrings.ends,
        startsIn = { ReminderTexts.startsIn(it) }
    )
}
