package com.gabriele.notionlocal.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.GroupLevel
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.ViewArrangement
import com.gabriele.notionlocal.ui.screen.displayedRowIds
import com.gabriele.notionlocal.viewmodel.DatabaseTableState
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **Le frecce "pagina prima" e "pagina dopo"** (16/10/2026) seguono l'ordine
 * in cui si vedono le righe: qui quello calcolato per ogni vista.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RowOrderTest {

    private val shop = DatabaseColumnEntity(
        id = "shop", pageId = "food", name = "Supermercato", type = ColumnType.SELECT, orderIndex = 0,
        optionsJson = """[{"label":"ALDI"},{"label":"IPERPOLI"}]"""
    )
    private val kind = DatabaseColumnEntity(
        id = "kind", pageId = "food", name = "Tipo", type = ColumnType.SELECT, orderIndex = 1,
        optionsJson = """[{"label":"Cibo"},{"label":"Casa"}]"""
    )
    private val day = DatabaseColumnEntity(id = "day", pageId = "food", name = "Giorno", type = ColumnType.DATE, orderIndex = 2)

    private fun row(id: String, order: Int) = DatabaseRowEntity(id = id, pageId = "food", title = id, orderIndex = order)

    // Nell'ordine della vista (già filtrata e ordinata): pasta, sapone, pane, sale.
    private val pasta = row("pasta", 0)
    private val sapone = row("sapone", 1)
    private val pane = row("pane", 2)
    private val sale = row("sale", 3)

    private val state = DatabaseTableState(
        columns = listOf(shop, kind, day),
        rows = listOf(pasta, sapone, pane, sale),
        cellValues = mapOf(
            ("pasta" to "shop") to "IPERPOLI", ("pasta" to "kind") to "Cibo", ("pasta" to "day") to "2000",
            ("sapone" to "shop") to "ALDI", ("sapone" to "kind") to "Casa", ("sapone" to "day") to "1000",
            ("pane" to "shop") to "ALDI", ("pane" to "kind") to "Cibo",
            ("sale" to "shop") to "IPERPOLI", ("sale" to "kind") to "Cibo", ("sale" to "day") to "500"
        )
    )

    @Before
    fun setUp() {
        AppSettings.init(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun aPlainTableKeepsTheViewOrder() {
        assertEquals(listOf("pasta", "sapone", "pane", "sale"), displayedRowIds(state, ViewArrangement(DatabaseLayout.TABLE)))
    }

    @Test
    fun aGroupedTableGoesGroupByGroup() {
        // ALDI (sapone, pane), poi IPERPOLI (pasta, sale).
        assertEquals(
            listOf("sapone", "pane", "pasta", "sale"),
            displayedRowIds(state, ViewArrangement(DatabaseLayout.TABLE, tableLevels = listOf(GroupLevel(shop))))
        )
        // E dentro ogni gruppo i sottogruppi: ALDI → Cibo (pane), Casa (sapone).
        assertEquals(
            listOf("pane", "sapone", "pasta", "sale"),
            displayedRowIds(state, ViewArrangement(DatabaseLayout.TABLE, tableLevels = listOf(GroupLevel(shop), GroupLevel(kind))))
        )
    }

    @Test
    fun aGroupedListGoesGroupByGroupToo() {
        // Dal 18/11/2026 l'elenco ha i gruppi della tabella: le frecce delle pagine li seguono.
        assertEquals(listOf("pasta", "sapone", "pane", "sale"), displayedRowIds(state, ViewArrangement(DatabaseLayout.LIST)))
        assertEquals(
            listOf("pane", "sapone", "pasta", "sale"),
            displayedRowIds(state, ViewArrangement(DatabaseLayout.LIST, tableLevels = listOf(GroupLevel(shop), GroupLevel(kind))))
        )
    }

    @Test
    fun aTableWithThreeLevelsGoesDeepFirst() {
        // Dal 28/10/2026 i livelli sono quanti se ne vogliono: per tipo, poi
        // supermercato, poi giorno. Cibo → ALDI (pane, senza giorno), IPERPOLI (pasta
        // e sale, lo stesso giorno); Casa → ALDI (sapone).
        assertEquals(
            listOf("pane", "pasta", "sale", "sapone"),
            displayedRowIds(state, ViewArrangement(DatabaseLayout.TABLE, tableLevels = listOf(GroupLevel(kind), GroupLevel(shop), GroupLevel(day))))
        )
    }

    @Test
    fun aBoardGoesColumnByColumnAndLaneByLane() {
        assertEquals(
            listOf("sapone", "pane", "pasta", "sale"),
            displayedRowIds(state, ViewArrangement(DatabaseLayout.BOARD, boardLevels = listOf(GroupLevel(shop))))
        )
        // Fascia Cibo (ALDI: pane; IPERPOLI: pasta, sale), poi fascia Casa (sapone).
        assertEquals(
            listOf("pane", "pasta", "sale", "sapone"),
            displayedRowIds(state, ViewArrangement(DatabaseLayout.BOARD, boardLevels = listOf(GroupLevel(shop), GroupLevel(kind))))
        )
    }

    @Test
    fun aCalendarGoesByDateThenTheUndated() {
        assertEquals(
            listOf("sale", "sapone", "pasta", "pane"),
            displayedRowIds(state, ViewArrangement(DatabaseLayout.CALENDAR, dateColumn = day))
        )
    }
}
