package com.gabriele.notionlocal.data.entity

import java.time.temporal.ChronoUnit
import java.time.ZoneId
import java.time.LocalDate
import java.time.Instant
/**
 * Il valore di una cella data: un inizio, una fine facoltativa, se
 * l'ora conta e la regola di ripetizione.
 *
 * `hasTime` sta scritto nel valore e non si indovina dai millisecondi.
 * Indovinarlo vorrebbe dire dire "se non cade a mezzanotte allora c'è
 * un'ora", e una pagina messa davvero a mezzanotte perderebbe la sua
 * ora — o peggio, si vedrebbe comparire "00:00" dal nulla su tutte le
 * date senza ora.
 *
 * Sta qui e non nella schermata perché il formato non serve più solo a
 * disegnare: anche i filtri devono leggere una data per sapere se una
 * riga passa, e due copie della stessa lettura si sarebbero separate
 * al primo campo aggiunto.
 */
data class DateRange(
    val start: Long,
    val end: Long?,
    val hasTime: Boolean = false,
    /** La regola di ripetizione, se questa data si ripete. */
    val recurrence: Recurrence? = null,
    /**
     * **"Include notification"** (dal 25/09/2026): all'inizio (e alla
     * fine, se c'è) arriva una notifica col nome della pagina. Senza ora,
     * alle 9:00. Vedi `data/notify/Reminders.kt`.
     */
    val notify: Boolean = false,
    /**
     * **Le notifiche, come su Google Calendar** (dal 26/10/2026, chiesto
     * dall'utente: "dieci ore prima, tre giorni prima, due settimane
     * prima"): quanti minuti prima dell'inizio suona ciascuna. 0 è quella
     * di sempre, all'inizio (e alla fine, se c'è). Conta solo con [notify].
     */
    val reminders: List<Int> = listOf(0)
)

/** Il terzo campo del valore, quando l'ora conta. */
const val DATE_TIME_FLAG = "t"

/** L'ultimo campo del valore, quando la data manda una notifica. */
const val DATE_NOTIFY_FLAG = "n"

/** Le notifiche come si scrivono nel valore: minuti prima dell'inizio, senza doppioni, la più vicina prima. */
fun normalizeReminders(reminders: List<Int>): List<Int> =
    reminders.filter { it in 0..REMINDER_MAX_MINUTES }.distinct().sorted()

/**
 * Legge il valore di una cella data.
 *
 * I campi sono nati uno alla volta e mancano nei valori scritti prima:
 * ognuno ha quindi un significato di riserva che coincide con com'era
 * il mondo quando quel campo non esisteva.
 */
fun parseDateRange(value: String): DateRange? {
    if (value.isBlank()) return null
    val parts = value.split(MULTI_VALUE_SEPARATOR)
    val start = parts.getOrNull(0)?.toLongOrNull() ?: return null
    val end = parts.getOrNull(1)?.toLongOrNull()
    // Il segno della notifica è sempre l'ultimo campo: il quinto, o il sesto
    // se prima ci sono le notifiche in anticipo ("0,600,4320").
    val notify = parts.size >= 5 && parts.last() == DATE_NOTIFY_FLAG
    val reminders = parts.takeIf { notify && it.size >= 6 }
        ?.get(4)?.split(",")?.mapNotNull { it.toIntOrNull() }
        ?.let { normalizeReminders(it) }
        ?.takeIf { it.isNotEmpty() }
        ?: listOf(0)
    return DateRange(
        start = start,
        // Una fine che precede l'inizio non vuol dire niente: la si
        // ignora invece di disegnare un intervallo al contrario.
        end = end?.takeIf { it >= start },
        hasTime = parts.getOrNull(2) == DATE_TIME_FLAG,
        recurrence = parts.getOrNull(3)?.let { RecurrenceCodec.decode(it) },
        notify = notify,
        reminders = reminders
    )
}

fun encodeDateRange(
    start: Long,
    end: Long? = null,
    hasTime: Boolean = false,
    recurrence: Recurrence? = null,
    notify: Boolean = false,
    reminders: List<Int> = listOf(0)
): String {
    val times = normalizeReminders(reminders)
    // Nessuna notifica nell'elenco: è come averla spenta.
    val notifies = notify && times.isNotEmpty()
    if (!hasTime && end == null && recurrence == null && !notifies) return start.toString()
    val endPart = end?.toString().orEmpty()
    val timePart = if (hasTime) DATE_TIME_FLAG else ""
    val base = "$start$MULTI_VALUE_SEPARATOR$endPart"
    val rule = recurrence?.let { RecurrenceCodec.encode(it) }.orEmpty()
    val head = "$base$MULTI_VALUE_SEPARATOR$timePart$MULTI_VALUE_SEPARATOR$rule"
    return when {
        // Il segno della notifica sta in fondo, dopo la ripetizione (anche
        // vuota): i valori scritti prima, che non l'hanno, restano validi.
        // Le notifiche in anticipo stanno fra le due, così la "n" resta
        // l'ultima e la sveglia (`getNotifyingDateCells`) la trova come prima;
        // con la sola notifica all'inizio il valore è quello di sempre.
        notifies && times != listOf(0) ->
            "$head$MULTI_VALUE_SEPARATOR${times.joinToString(",")}$MULTI_VALUE_SEPARATOR$DATE_NOTIFY_FLAG"
        notifies -> "$head$MULTI_VALUE_SEPARATOR$DATE_NOTIFY_FLAG"
        recurrence != null -> "$base$MULTI_VALUE_SEPARATOR$timePart$MULTI_VALUE_SEPARATOR$rule"
        hasTime -> "$base$MULTI_VALUE_SEPARATOR$timePart"
        else -> base
    }
}

/**
 * **La volta dopo di una data che si ripete** (dal 13/11/2026, vedi
 * `DatabaseViewModel.completeRecurring`): il valore della cella con l'inizio
 * spostato alla ripetizione successiva, alla stessa ora, con la stessa durata
 * e la stessa regola. I giorni che la regola prendeva dall'inizio (il giorno
 * del mese, quello della settimana, "il terzo lunedì") ci vengono scritti
 * dentro, così le volte dopo restano quelle di prima: il 31 gennaio, passato
 * al 28 febbraio, torna al 31 marzo. Una regola che finisce dopo un numero di
 * volte ne ha una in meno. Null se la data non si ripete o se le ripetizioni
 * sono finite.
 */
fun nextOccurrenceValue(value: String, zone: ZoneId): String? {
    val range = parseDateRange(value) ?: return null
    val rule = range.recurrence ?: return null
    val startAt = Instant.ofEpochMilli(range.start).atZone(zone)
    val first = startAt.toLocalDate()
    val next = rule.occurrences(first, first.plusDays(1), first.plusYears(rule.interval.toLong() + 1)).firstOrNull() ?: return null
    val pinned = when (rule.freq) {
        RecurrenceFreq.WEEKLY -> if (rule.weekdays.isEmpty()) rule.copy(weekdays = setOf(first.dayOfWeek)) else rule
        RecurrenceFreq.MONTHLY -> when (rule.monthlyMode) {
            MonthlyMode.DAY_OF_MONTH -> rule.copy(monthDay = rule.monthDay ?: first.dayOfMonth)
            MonthlyMode.NTH_WEEKDAY -> rule.copy(
                nthWeek = rule.nthWeek
                    ?: if (first.plusWeeks(1).month != first.month) Recurrence.LAST_WEEK else (first.dayOfMonth - 1) / 7 + 1,
                nthWeekday = rule.nthWeekday ?: first.dayOfWeek
            )
        }
        else -> rule
    }
    val left = when (val end = rule.end) {
        is RecurrenceEnd.AfterCount -> RecurrenceEnd.AfterCount(end.count - 1)
        else -> end
    }
    fun at(day: LocalDate, time: java.time.LocalTime): Long = day.atTime(time).atZone(zone).toInstant().toEpochMilli()
    val newEnd = range.end?.let { end ->
        val endAt = Instant.ofEpochMilli(end).atZone(zone)
        at(next.plusDays(ChronoUnit.DAYS.between(first, endAt.toLocalDate())), endAt.toLocalTime())
    }
    return encodeDateRange(
        start = at(next, startAt.toLocalTime()),
        end = newEnd,
        hasTime = range.hasTime,
        recurrence = pinned.copy(end = left),
        notify = range.notify,
        reminders = range.reminders
    )
}

