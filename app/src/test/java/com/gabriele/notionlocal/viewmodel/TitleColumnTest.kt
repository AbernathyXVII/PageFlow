package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.NAME_COLUMN_KEY
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.SelectOption
import com.gabriele.notionlocal.data.entity.nameColumnHidden
import com.gabriele.notionlocal.data.entity.tableKeys
import com.gabriele.notionlocal.data.entity.titleColumnId
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * **La proprietà principale come le altre** (dal 13/11/2026, chiesto
 * dall'utente): "Change type" su Name la fa diventare una colonna vera, al suo
 * posto e coi nomi dentro; Name si nasconde, e le righe si chiamano come il
 * valore della colonna. Vedi `TitleColumn.kt`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TitleColumnTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: DatabaseViewModel
    private lateinit var pages: PageRepository
    private val habits = PageEntity(title = "Habit tracker", isDatabase = true)
    private val work = DatabaseColumnEntity(pageId = habits.id, name = "Work", type = ColumnType.CHECKBOX, orderIndex = 0)
    private lateinit var rows: List<DatabaseRowEntity>

    @Before
    fun setUp() = runBlocking<Unit> {
        AppSettings.init(ApplicationProvider.getApplicationContext())
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pages = PageRepository(db)
        pages.createPage(habits)
        pages.setNameColumnTitle(habits.id, "Day")
        db.databaseDao().insertColumn(work)
        rows = listOf("1", "2", "3").mapIndexed { i, day ->
            DatabaseRowEntity(pageId = habits.id, title = day, orderIndex = i).also { db.databaseDao().insertRow(it) }
        }
        viewModel = DatabaseViewModel(DatabaseRepository(db), pages)
        viewModel.load(habits.id)
        waitFor("il database") { viewModel.page.value != null && viewModel.tableState.value.rows.size == 3 }
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

    private fun page() = runBlocking { pages.getPage(habits.id)!! }
    private fun columns() = runBlocking { db.databaseDao().getColumnsForPageOnce(habits.id) }
    private fun title(i: Int) = runBlocking { db.databaseDao().getRowById(rows[i].id)!!.title }

    @Test
    fun nameBecomesANumberColumnAndTheRowsFollowIt() {
        viewModel.convertNameColumn("Day", ColumnType.NUMBER, "[]", null)
        waitFor("la colonna Day, e Name nascosta") { page().titleColumnId() != null && page().nameColumnHidden() }
        val day = columns().single { it.name == "Day" }
        assertEquals(ColumnType.NUMBER, day.type)
        assertEquals(day.id, page().titleColumnId())
        assertTrue("Name nascosta", page().nameColumnHidden())
        // Al posto di Name: prima di Work, e Name subito dopo.
        assertEquals(listOf(day.id, NAME_COLUMN_KEY, work.id), tableKeys(columns().sortedBy { it.orderIndex }, page().nameColumnOrder))
        waitFor("i valori copiati") { viewModel.tableState.value.cellValues[rows[0].id to day.id] == "1" }
        assertEquals("3", viewModel.tableState.value.cellValues[rows[2].id to day.id])

        // Cambiato il valore, la riga si chiama come lui.
        viewModel.setCellValue(rows[1], day, "12")
        waitFor("il nome dalla colonna") { title(1) == "12" }
        // Il nome non si scrive più da Name.
        viewModel.updateRowTitle(rows[1], "Altro")
        Thread.sleep(200)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals("12", title(1))

        // Eliminata la colonna, i nomi restano e Name torna a vedersi.
        viewModel.deleteColumn(day)
        waitFor("Name di nuovo") { page().titleColumnId() == null && !page().nameColumnHidden() }
        assertEquals("12", title(1))
    }

    @Test
    fun aSelectColumnHasTheNamesAsTags() {
        viewModel.convertNameColumn("Day", ColumnType.SELECT, "[]", null)
        waitFor("la colonna Day") { page().titleColumnId() != null }
        val day = columns().single { it.name == "Day" }
        val labels = Json.decodeFromString<List<SelectOption>>(day.optionsJson).map { it.label }
        assertEquals(listOf("1", "2", "3"), labels)
    }

    @Test
    fun nameHidesAndComesBack() {
        viewModel.setNameHidden(true)
        waitFor("nascosta") { page().nameColumnHidden() }
        assertNull(page().titleColumnId())
        viewModel.setNameHidden(false)
        waitFor("di nuovo") { !page().nameColumnHidden() }
        assertFalse(page().nameColumnHidden())
    }
}
