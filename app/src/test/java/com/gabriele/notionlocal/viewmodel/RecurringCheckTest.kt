package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.Recurrence
import com.gabriele.notionlocal.data.entity.RecurrenceFreq
import com.gabriele.notionlocal.data.entity.encodeDateRange
import com.gabriele.notionlocal.data.entity.parseDateRange
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
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
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * **Spuntare un evento che si ripete lo porta alla volta dopo** (dal
 * 13/11/2026, chiesto dall'utente con "Elena's payment"): in una tabella
 * raggruppata per la sua casella, l'evento mensile spuntato resta fra le non
 * spuntate e la sua data passa al mese dopo; uno senza ripetizione si spunta
 * come sempre. Vedi `DatabaseViewModel.completeRecurring`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RecurringCheckTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: DatabaseViewModel
    private val agenda = PageEntity(title = "Agenda", isDatabase = true, databaseLayout = DatabaseLayout.TABLE)
    private val done = DatabaseColumnEntity(pageId = agenda.id, name = "Fatto", type = ColumnType.CHECKBOX, orderIndex = 0)
    private val date = DatabaseColumnEntity(pageId = agenda.id, name = "Data", type = ColumnType.DATE, orderIndex = 1)
    private lateinit var elena: DatabaseRowEntity
    private lateinit var spesa: DatabaseRowEntity
    private val first = LocalDate.of(2026, 12, 1)

    private fun millis(day: LocalDate) = day.atStartOfDay(AppSettings.zoneId).toInstant().toEpochMilli()

    @Before
    fun setUp() = runBlocking<Unit> {
        AppSettings.init(ApplicationProvider.getApplicationContext())
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(agenda)
        db.databaseDao().insertColumn(done)
        db.databaseDao().insertColumn(date)
        elena = DatabaseRowEntity(pageId = agenda.id, title = "Elena's payment", orderIndex = 0)
        spesa = DatabaseRowEntity(pageId = agenda.id, title = "Spesa", orderIndex = 1)
        db.databaseDao().insertRow(elena)
        db.databaseDao().insertRow(spesa)
        db.databaseDao().insertCell(DatabaseCellEntity(rowId = elena.id, columnId = date.id, value = encodeDateRange(millis(first), recurrence = Recurrence(RecurrenceFreq.MONTHLY))))
        db.databaseDao().insertCell(DatabaseCellEntity(rowId = spesa.id, columnId = date.id, value = encodeDateRange(millis(first))))
        viewModel = DatabaseViewModel(DatabaseRepository(db), pages)
        viewModel.load(agenda.id)
        waitFor("il database") { viewModel.page.value != null && viewModel.tableState.value.rows.size == 2 }
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

    private fun cell(row: DatabaseRowEntity, column: DatabaseColumnEntity) = viewModel.tableState.value.cellValues[row.id to column.id]
    private fun dayOf(row: DatabaseRowEntity) = parseDateRange(cell(row, date).orEmpty())?.let { Instant.ofEpochMilli(it.start).atZone(AppSettings.zoneId).toLocalDate() }

    @Test
    fun aRecurringEventTickedGoesToTheNextMonthAndStaysUnchecked() {
        viewModel.addGroupRule(board = false, columnId = done.id)
        waitFor("il gruppo per casella") { viewModel.tableGroupLevels().any { it.column.id == done.id } }

        viewModel.setCellValue(elena, done, "true")
        // Un attimo piena (l'animazione), mai scritta come spuntata.
        assertTrue("piena un attimo", "${elena.id}:${done.id}" in viewModel.checkFlash.value)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
        waitFor("il mese dopo") { dayOf(elena) == first.plusMonths(1) }
        waitFor("di nuovo vuota") { viewModel.checkFlash.value.isEmpty() }
        assertTrue("mai spuntata: ${cell(elena, done)}", cell(elena, done) != "true")

        // Una data che non si ripete: la spunta è quella di sempre, e la data resta.
        viewModel.setCellValue(spesa, done, "true")
        waitFor("Spesa spuntata") { cell(spesa, done) == "true" }
        assertEquals(first, dayOf(spesa))
    }

    @Test
    fun withoutTheGroupOnTheCheckboxItIsANormalTick() {
        viewModel.setCellValue(elena, done, "true")
        waitFor("spuntata") { cell(elena, done) == "true" }
        assertEquals(first, dayOf(elena))
    }
}
