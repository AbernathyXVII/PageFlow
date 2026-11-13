package com.gabriele.notionlocal.data.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * **"Date by"** (dal 21/10/2026, per il database delle vittorie K-pop
 * dell'utente, come le settimane di soridata.com): raggruppando per una
 * data, i gruppi possono essere giorni, settimane, mesi o anni, come su
 * Notion, e andare dal più vecchio o dal più recente.
 *
 * Ogni raggruppamento di una vista ha il suo: i gruppi e i sottogruppi
 * della tabella, le colonne e le fasce della bacheca. Stanno tutti in
 * `PageEntity.dateGroupingJson`.
 */

/** Quanto è largo un gruppo di date. */
enum class DateUnit { DAY, WEEK, MONTH, YEAR }

/** Come si raggruppa per una data: per quanto, e da quale parte si comincia. */
@Serializable
data class DateGroupBy(
    val unit: DateUnit = DateUnit.DAY,
    /** I gruppi dal più recente al più vecchio. Di suo dal più vecchio, com'era. */
    val newestFirst: Boolean = false
)

/** Quale raggruppamento della vista. */
enum class GroupSlot { TABLE, TABLE_SUB, BOARD, BOARD_SUB }

@Serializable
data class DateGroupings(
    val table: DateGroupBy = DateGroupBy(),
    val tableSub: DateGroupBy = DateGroupBy(),
    val board: DateGroupBy = DateGroupBy(),
    val boardSub: DateGroupBy = DateGroupBy()
) {
    operator fun get(slot: GroupSlot): DateGroupBy = when (slot) {
        GroupSlot.TABLE -> table
        GroupSlot.TABLE_SUB -> tableSub
        GroupSlot.BOARD -> board
        GroupSlot.BOARD_SUB -> boardSub
    }

    fun with(slot: GroupSlot, by: DateGroupBy): DateGroupings = when (slot) {
        GroupSlot.TABLE -> copy(table = by)
        GroupSlot.TABLE_SUB -> copy(tableSub = by)
        GroupSlot.BOARD -> copy(board = by)
        GroupSlot.BOARD_SUB -> copy(boardSub = by)
    }
}

private val groupingJson = Json { ignoreUnknownKeys = true; encodeDefaults = false }

fun parseDateGroupings(json: String?): DateGroupings =
    json?.let { runCatching { groupingJson.decodeFromString<DateGroupings>(it) }.getOrNull() } ?: DateGroupings()

/** Null quando è tutto com'è di suo: per giorno, dal più vecchio. */
fun encodeDateGroupings(groupings: DateGroupings): String? =
    if (groupings == DateGroupings()) null else groupingJson.encodeToString(groupings)

/**
 * **Le settimane cominciano di lunedì**, in tutti i calendari (dal 13/11/2026,
 * chiesto dall'utente): prima il primo giorno seguiva la lingua del sistema, e
 * con Windows o il telefono in inglese era la domenica.
 */
val FIRST_DAY_OF_WEEK: DayOfWeek = DayOfWeek.MONDAY

/** Il primo giorno del gruppo che contiene [day]: lui stesso, l'inizio della settimana, del mese o dell'anno. */
fun DateUnit.startOf(day: LocalDate, firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY): LocalDate = when (this) {
    DateUnit.DAY -> day
    DateUnit.WEEK -> day.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    DateUnit.MONTH -> day.withDayOfMonth(1)
    DateUnit.YEAR -> day.withDayOfYear(1)
}

/** L'ultimo giorno del gruppo che comincia con [start]. */
fun DateUnit.endOf(start: LocalDate): LocalDate = when (this) {
    DateUnit.DAY -> start
    DateUnit.WEEK -> start.plusDays(6)
    DateUnit.MONTH -> start.plusMonths(1).minusDays(1)
    DateUnit.YEAR -> start.plusYears(1).minusDays(1)
}
