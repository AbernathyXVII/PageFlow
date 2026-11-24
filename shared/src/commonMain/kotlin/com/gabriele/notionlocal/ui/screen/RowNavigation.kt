package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.GroupLevel
import com.gabriele.notionlocal.data.entity.parseDateRange
import com.gabriele.notionlocal.data.entity.rowTree
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.viewmodel.DatabaseTableState
import com.gabriele.notionlocal.viewmodel.DatabaseViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory

/**
 * **Le pagine di un database una dopo l'altra** (dal 16/10/2026, chiesto
 * dall'utente): nella pagina di una riga, due frecce per andare alla pagina
 * prima o dopo, nell'ordine in cui si vedevano nella vista da cui la si è
 * aperta — filtro, ordinamento, ricerca, gruppi e sottogruppi, colonne e
 * fasce della bacheca, date del calendario.
 *
 * L'ordine si fotografa **quando si apre la riga** (`remember`) e resta
 * quello mentre si passa da una pagina all'altra: cambiando il valore su cui
 * si ordina, la pagina non salta da un'altra parte sotto le frecce. Si
 * ricorda uno per database, finché l'app resta aperta. Aprendo la pagina da
 * altrove (la ricerca, un collegamento, dopo aver chiuso l'app) vale l'ordine
 * del database stesso: il suo filtro e il suo ordinamento.
 */
object RowNavigation {
    private val orders = HashMap<String, List<String>>()

    /** L'ordine delle righe di `databaseId` come si vedono adesso. */
    fun remember(databaseId: String, rowIds: List<String>) {
        orders[databaseId] = rowIds
    }

    fun order(databaseId: String): List<String>? = orders[databaseId]
}

/** Come si vede una vista: quello che serve a `displayedRowIds` per mettere le righe in fila. */
class ViewArrangement(
    val layout: DatabaseLayout,
    /** I livelli dei gruppi della tabella (dal 28/10/2026 quanti se ne vogliono). */
    val tableLevels: List<GroupLevel> = emptyList(),
    /** Quelli della bacheca: il primo fa le colonne, gli altri le fasce. */
    val boardLevels: List<GroupLevel> = emptyList(),
    val dateColumn: DatabaseColumnEntity? = null
)

/**
 * **Le righe nell'ordine in cui si leggono nella vista**, dall'alto in basso
 * e da sinistra a destra. Una riga che compare due volte (con la selezione
 * multipla sta in più gruppi) conta la prima.
 *
 * - Tabella ed elenco: gruppo dopo gruppo (e sottogruppo dopo sottogruppo);
 *   nella tabella, dentro ognuno, con le sotto-righe aperte sotto la loro riga.
 * - Bacheca: colonna dopo colonna; con le fasce, fascia dopo fascia (a ogni livello).
 * - Calendario e timeline: per data, come le barre della timeline, e in fondo
 *   le righe senza data.
 * - Galleria, grafico: l'ordine della vista.
 */
fun displayedRowIds(state: DatabaseTableState, view: ViewArrangement): List<String> {
    val out = LinkedHashSet<String>()
    when (view.layout) {
        DatabaseLayout.TABLE, DatabaseLayout.LIST -> {
            // Gruppo dopo gruppo, a ogni livello, e all'ultimo le righe (l'elenco coi gruppi della tabella dal 18/11/2026).
            fun walk(depth: Int, rows: List<DatabaseRowEntity>) {
                val level = view.tableLevels.getOrNull(depth)
                if (level == null) {
                    if (view.layout == DatabaseLayout.TABLE) rowTree(rows).forEach { out += it.row.id } else rows.forEach { out += it.id }
                    return
                }
                for (g in buildLevelGroups(state, level, rows)) walk(depth + 1, g.rows)
            }
            walk(0, state.rows)
        }
        DatabaseLayout.BOARD -> {
            val first = view.boardLevels.firstOrNull()
            if (first == null) {
                state.rows.forEach { out += it.id }
            } else {
                val columns = buildLevelGroups(state, first, hideEmpty = false, keepEmptyNone = true)
                // Fascia dopo fascia, a ogni livello; in ognuna, colonna dopo colonna.
                fun walk(depth: Int, rows: List<DatabaseRowEntity>) {
                    val level = view.boardLevels.getOrNull(depth)
                    if (level == null) {
                        val inBand = rows.mapTo(HashSet()) { it.id }
                        columns.forEach { c -> c.rows.forEach { if (it.id in inBand) out += it.id } }
                        return
                    }
                    for (band in buildLevelGroups(state, level, rows)) walk(depth + 1, band.rows)
                }
                walk(1, state.rows)
            }
        }
        DatabaseLayout.CALENDAR, DatabaseLayout.TIMELINE -> {
            val date = view.dateColumn
            if (date == null) {
                state.rows.forEach { out += it.id }
            } else {
                val value = { id: String -> state.cellValues[id to date.id].orEmpty() }
                state.rows
                    .mapNotNull { row -> parseDateRange(value(row.id))?.let { row to it } }
                    .sortedWith(
                        compareBy<Pair<com.gabriele.notionlocal.data.entity.DatabaseRowEntity, com.gabriele.notionlocal.data.entity.DateRange>> { it.second.start }
                            .thenBy { it.second.end ?: it.second.start }
                            .thenBy { it.first.title }
                            .thenBy { it.first.id }
                    )
                    .forEach { out += it.first.id }
                state.rows.filter { value(it.id).isBlank() }.forEach { out += it.id }
            }
        }
        DatabaseLayout.GALLERY, DatabaseLayout.CHART -> state.rows.forEach { out += it.id }
    }
    return out.toList()
}

/** La riga prima e quella dopo, lasciate da `RowNavigationButtons` per le frecce della tastiera (vedi `RowArrows`). */
class SiblingRows {
    var previous: String? = null
    var next: String? = null
}

/**
 * Le due frecce nella pagina di una riga: la pagina prima e quella dopo
 * (vedi `RowNavigation`). Nella pagina di una riga sola, o se la riga non è
 * fra quelle che la vista mostra, non ci sono; in cima e in fondo la freccia
 * che non porta da nessuna parte è spenta. [siblings] riceve le due righe.
 */
@Composable
fun RowNavigationButtons(
    pageId: String,
    factory: ViewModelFactory,
    buttonSize: Dp,
    iconSize: Dp,
    background: Color,
    tint: Color,
    siblings: SiblingRows? = null,
    onOpenRow: (rowId: String) -> Unit
) {
    val row by remember(pageId) { factory.pageRepository.observeRowOfPage(pageId) }
        .collectAsStateWithLifecycle(initialValue = null)
    val current = row
    val databaseId = current?.pageId
    // Lo stesso ViewModel "leggero" delle proprietà in cima alla pagina: le
    // righe col filtro e l'ordinamento del database, se non c'è un ordine
    // fotografato da una vista.
    val viewModel: DatabaseViewModel = viewModel(factory = factory, key = "row-properties-${databaseId.orEmpty()}")
    LaunchedEffect(databaseId) { if (databaseId != null) viewModel.load(databaseId, light = true) }
    val state by viewModel.tableState.collectAsStateWithLifecycle()
    val order = databaseId?.let { RowNavigation.order(it) } ?: state.rows.map { it.id }
    val index = current?.let { order.indexOf(it.id) } ?: -1
    val shown = index >= 0 && order.size >= 2
    val previous = if (shown) order.getOrNull(index - 1) else null
    val next = if (shown) order.getOrNull(index + 1) else null
    SideEffect {
        siblings?.previous = previous
        siblings?.next = next
    }
    if (!shown) return

    @Composable
    fun Arrow(target: String?, up: Boolean) {
        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(CircleShape)
                .background(background)
                .then(if (target != null) Modifier.clickable { onOpenRow(target) } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (up) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (up) DbStrings.previous else DbStrings.next,
                tint = if (target != null) tint else tint.copy(alpha = 0.3f),
                modifier = Modifier.size(iconSize)
            )
        }
    }
    Arrow(previous, up = true)
    Arrow(next, up = false)
}
