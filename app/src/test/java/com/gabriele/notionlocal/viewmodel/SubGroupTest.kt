package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DateGroupBy
import com.gabriele.notionlocal.data.entity.DateUnit
import com.gabriele.notionlocal.data.entity.GroupOrder
import com.gabriele.notionlocal.data.entity.GroupRuleSpec
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.tableGroupRules
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.repository.PageRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * **I gruppi dentro i gruppi** della tabella: prima un sottogruppo solo
 * (29/09/2026), dal 28/10/2026 quante regole si vogliono, come i Sort
 * (chiesto dall'utente: "la stessa cosa con il Group").
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SubGroupTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: DatabaseViewModel

    private val shopping = PageEntity(title = "Spesa", isDatabase = true)
    private val shop = DatabaseColumnEntity(pageId = shopping.id, name = "Supermercato", type = ColumnType.SELECT, orderIndex = 0)
    private val kind = DatabaseColumnEntity(pageId = shopping.id, name = "Tipo", type = ColumnType.SELECT, orderIndex = 1)
    private val bought = DatabaseColumnEntity(pageId = shopping.id, name = "Preso", type = ColumnType.CHECKBOX, orderIndex = 2)
    private val day = DatabaseColumnEntity(pageId = shopping.id, name = "Giorno", type = ColumnType.DATE, orderIndex = 3)
    private val photo = DatabaseColumnEntity(pageId = shopping.id, name = "Foto", type = ColumnType.IMAGE, orderIndex = 4)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(shopping)
        listOf(shop, kind, bought, day, photo).forEach { db.databaseDao().insertColumn(it) }
        viewModel = DatabaseViewModel(DatabaseRepository(db), pages)
        viewModel.load(shopping.id)
        waitFor("le colonne") { viewModel.tableState.value.columns.size == 5 && viewModel.page.value != null }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < end) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10)
        }
        fail("Non è successo: $what")
    }

    private fun saved() = runBlocking { db.pageDao().getById(shopping.id)!! }
    private fun levels() = viewModel.tableGroupLevels().map { it.column.id }

    @Test
    fun asManyGroupsAsWantedInTheOrderTheyWereAdded() {
        assertEquals(emptyList<String>(), levels())
        viewModel.addGroupRule(board = false, shop.id)
        viewModel.addGroupRule(board = false, kind.id)
        viewModel.addGroupRule(board = false, bought.id)
        viewModel.addGroupRule(board = false, day.id)
        assertEquals(listOf(shop.id, kind.id, bought.id, day.id), levels())
        // La stessa proprietà due volte non fa un livello in più.
        viewModel.addGroupRule(board = false, kind.id)
        assertEquals(4, levels().size)
        waitFor("salvati") { saved().tableGroupRules().map { it.column } == listOf(shop.id, kind.id, bought.id, day.id) }
    }

    @Test
    fun aRuleMovesUpOrDownAndIsRemoved() {
        listOf(shop, kind, bought).forEach { viewModel.addGroupRule(board = false, it.id) }
        viewModel.moveGroupRule(board = false, bought.id, -1)
        assertEquals(listOf(shop.id, bought.id, kind.id), levels())
        // Già in cima non sale più.
        viewModel.moveGroupRule(board = false, shop.id, -1)
        assertEquals(listOf(shop.id, bought.id, kind.id), levels())
        viewModel.removeGroupRule(board = false, shop.id)
        assertEquals(listOf(bought.id, kind.id), levels())
        viewModel.removeAllGroupRules(board = false)
        assertEquals(emptyList<String>(), levels())
        waitFor("tolti anche nel database") { saved().tableGroupRulesJson == null }
    }

    @Test
    fun choosingAPropertyOfAnotherRuleSwapsThem() {
        listOf(shop, kind, bought).forEach { viewModel.addGroupRule(board = false, it.id) }
        viewModel.setGroupRuleColumn(board = false, shop.id, bought.id)
        assertEquals(listOf(bought.id, kind.id, shop.id), levels())
        viewModel.setGroupRuleColumn(board = false, kind.id, day.id)
        assertEquals(listOf(bought.id, day.id, shop.id), levels())
    }

    @Test
    fun everyRuleHasItsOwnHideEmptyAndDateBy() {
        listOf(shop, kind, day).forEach { viewModel.addGroupRule(board = false, it.id) }
        viewModel.setGroupRuleHideEmpty(board = false, kind.id, false)
        viewModel.setGroupRuleDateBy(board = false, day.id, DateGroupBy(DateUnit.MONTH, newestFirst = true))
        waitFor("salvato") {
            saved().tableGroupRules() == listOf(
                GroupRuleSpec(shop.id),
                GroupRuleSpec(kind.id, hideEmpty = false),
                GroupRuleSpec(day.id, dateBy = DateGroupBy(DateUnit.MONTH, newestFirst = true))
            )
        }
        // La pagina del ViewModel arriva anche lei dal database: si aspetta l'ultima, con
        // tutte e due le scelte (fra le due scritture c'è un istante con la prima sola).
        waitFor("i livelli") {
            val levels = viewModel.tableGroupLevels()
            levels.map { it.hideEmpty } == listOf(true, false, true) && levels[2].dateBy.unit == DateUnit.MONTH
        }
        assertEquals(DateUnit.MONTH, viewModel.tableGroupLevels()[2].dateBy.unit)
    }

    @Test
    fun aRuleOnADeletedOrUngroupablePropertyIsSkipped() = runBlocking<Unit> {
        listOf(shop, kind, bought).forEach { viewModel.addGroupRule(board = false, it.id) }
        // Un'immagine non si raggruppa: anche scritta a mano nella regola, non conta.
        db.pageDao().update(saved().copy(tableGroupRulesJson = """[{"column":"${shop.id}"},{"column":"${photo.id}"},{"column":"${kind.id}"}]"""))
        waitFor("la regola dell'immagine saltata") { levels() == listOf(shop.id, kind.id) }
        viewModel.deleteColumn(shop)
        waitFor("quella dopo sale") { levels() == listOf(kind.id) }
    }

    @Test
    fun aRowAddedInAGroupGetsTheValueOfEveryLevel() {
        listOf(shop, kind, bought).forEach { viewModel.addGroupRule(board = false, it.id) }
        viewModel.addRowInGroups(listOf(shop.id to "IPERPOLI", kind.id to "Cibo", bought.id to "true"))
        waitFor("la riga nuova coi tre valori") {
            val state = viewModel.tableState.value
            val row = state.rows.singleOrNull() ?: return@waitFor false
            state.cellValues[row.id to shop.id] == "IPERPOLI" &&
                state.cellValues[row.id to kind.id] == "Cibo" &&
                state.cellValues[row.id to bought.id] == "true"
        }
        // Il gruppo "senza valore" lascia la cella vuota.
        viewModel.addRowInGroups(listOf(shop.id to "", kind.id to "Casa"))
        waitFor("la seconda riga") {
            val state = viewModel.tableState.value
            val row = state.rows.find { state.cellValues[it.id to kind.id] == "Casa" } ?: return@waitFor false
            state.cellValues[row.id to shop.id].isNullOrEmpty()
        }
    }

    @Test
    fun theTableAndTheBoardHaveTheirOwnRules() {
        viewModel.addGroupRule(board = false, kind.id)
        viewModel.setBoardGroupColumn(shop.id)
        viewModel.addGroupRule(board = true, bought.id)
        assertEquals(listOf(kind.id), levels())
        assertEquals(listOf(shop.id, bought.id), viewModel.boardGroupLevels().map { it.column.id })
        waitFor("salvati") { saved().tableGroupRulesJson != null && saved().boardGroupRulesJson != null }
        assertTrue(saved().tableGroupRules().none { it.column == shop.id })
    }

    @Test
    fun theOrderOfTheGroupsIsSavedAndStartsOverWithAnotherProperty() {
        listOf(shop, bought).forEach { viewModel.addGroupRule(board = false, it.id) }
        // "Sort groups" (28/10/2026): Unchecked prima di Checked, a mano.
        viewModel.setGroupRuleManual(board = false, bought.id, listOf("false", "true"))
        viewModel.setGroupRuleOrder(board = false, shop.id, GroupOrder.DESCENDING)
        waitFor("salvato") {
            saved().tableGroupRules() == listOf(
                GroupRuleSpec(shop.id, order = GroupOrder.DESCENDING),
                GroupRuleSpec(bought.id, manual = listOf("false", "true"))
            )
        }
        waitFor("nei livelli") { viewModel.tableGroupLevels().map { it.order } == listOf(GroupOrder.DESCENDING, GroupOrder.MANUAL) }
        assertEquals(listOf("false", "true"), viewModel.tableGroupLevels()[1].manual)
        // Un'altra proprietà ha altri gruppi: l'ordine riparte da capo.
        viewModel.setGroupRuleColumn(board = false, bought.id, kind.id)
        waitFor("da capo") { saved().tableGroupRules().last() == GroupRuleSpec(kind.id) }
    }
}
