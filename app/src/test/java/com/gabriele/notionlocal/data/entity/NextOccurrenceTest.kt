package com.gabriele.notionlocal.data.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * **La volta dopo di una data che si ripete** (dal 13/11/2026, chiesto
 * dall'utente): spuntando "Elena's payment", che si ripete ogni mese, la data
 * passa al mese dopo. Vedi `nextOccurrenceValue`.
 */
class NextOccurrenceTest {
    private val zone = ZoneId.of("Europe/Rome")

    private fun millis(day: LocalDate, time: LocalTime = LocalTime.MIDNIGHT) = day.atTime(time).atZone(zone).toInstant().toEpochMilli()
    private fun dayOf(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    /** Il valore dopo [times] spunte, letto. */
    private fun after(value: String, times: Int): DateRange? {
        var current: String? = value
        repeat(times) { current = current?.let { nextOccurrenceValue(it, zone) } }
        return current?.let { parseDateRange(it) }
    }

    @Test
    fun everyMonthGoesToTheNextMonth() {
        val value = encodeDateRange(millis(LocalDate.of(2026, 11, 1)), recurrence = Recurrence(RecurrenceFreq.MONTHLY))
        val next = after(value, 1)!!
        assertEquals(LocalDate.of(2026, 12, 1), dayOf(next.start))
        // La regola resta: si ripete ancora, e la volta dopo è gennaio.
        assertEquals(RecurrenceFreq.MONTHLY, next.recurrence?.freq)
        assertEquals(LocalDate.of(2027, 1, 1), dayOf(after(value, 2)!!.start))
    }

    @Test
    fun theThirtyFirstComesBackAfterAShortMonth() {
        val value = encodeDateRange(millis(LocalDate.of(2027, 1, 31)), recurrence = Recurrence(RecurrenceFreq.MONTHLY))
        assertEquals(LocalDate.of(2027, 2, 28), dayOf(after(value, 1)!!.start))
        assertEquals(LocalDate.of(2027, 3, 31), dayOf(after(value, 2)!!.start))
    }

    @Test
    fun theTimeAndTheLengthStay() {
        val start = millis(LocalDate.of(2026, 11, 13), LocalTime.of(18, 30))
        val end = millis(LocalDate.of(2026, 11, 14), LocalTime.of(9, 0))
        val value = encodeDateRange(start, end, hasTime = true, recurrence = Recurrence(RecurrenceFreq.WEEKLY))
        val next = after(value, 1)!!
        assertEquals(millis(LocalDate.of(2026, 11, 20), LocalTime.of(18, 30)), next.start)
        assertEquals(millis(LocalDate.of(2026, 11, 21), LocalTime.of(9, 0)), next.end)
        // Il giorno della settimana, prima preso dall'inizio, ora è nella regola.
        assertEquals(setOf(DayOfWeek.FRIDAY), next.recurrence?.weekdays)
    }

    @Test
    fun theThirdMondayStaysTheThirdMonday() {
        // Lunedì 16 novembre 2026 è il terzo del mese.
        val value = encodeDateRange(millis(LocalDate.of(2026, 11, 16)), recurrence = Recurrence(RecurrenceFreq.MONTHLY, monthlyMode = MonthlyMode.NTH_WEEKDAY))
        assertEquals(LocalDate.of(2026, 12, 21), dayOf(after(value, 1)!!.start))
        assertEquals(LocalDate.of(2027, 1, 18), dayOf(after(value, 2)!!.start))
    }

    @Test
    fun aRuleWithACountEnds() {
        val value = encodeDateRange(millis(LocalDate.of(2026, 11, 1)), recurrence = Recurrence(RecurrenceFreq.MONTHLY, end = RecurrenceEnd.AfterCount(3)))
        assertEquals(RecurrenceEnd.AfterCount(2), after(value, 1)!!.recurrence?.end)
        assertEquals(LocalDate.of(2027, 1, 1), dayOf(after(value, 2)!!.start))
        // La terza era l'ultima: dopo non c'è niente.
        assertNull(after(value, 3))
    }

    @Test
    fun aRuleThatEndedOrNoRuleHasNoNext() {
        val ended = encodeDateRange(
            millis(LocalDate.of(2026, 11, 1)),
            recurrence = Recurrence(RecurrenceFreq.MONTHLY, end = RecurrenceEnd.OnDate(LocalDate.of(2026, 11, 20).toEpochDay()))
        )
        assertNull(nextOccurrenceValue(ended, zone))
        assertNull(nextOccurrenceValue(encodeDateRange(millis(LocalDate.of(2026, 11, 1))), zone))
    }
}
