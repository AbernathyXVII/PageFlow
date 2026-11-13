package com.gabriele.notionlocal.viewmodel

import com.gabriele.notionlocal.ui.screen.LINK_PENCIL_SPACE
import android.app.Application
import android.os.Looper
import androidx.compose.material3.Typography
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.repository.PageRepository.DuplicateTarget
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.screen.cellFitWidth
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * **"Page link"** (29/09/2026): una proprietà che collega la riga a
 * un'altra pagina del Workspace — anche la pagina di un'altra riga dello
 * stesso database. Si salva l'id; si mostra il titolo di adesso.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PageLinkTest {

    private lateinit var db: AppDatabase
    private lateinit var pages: PageRepository
    private lateinit var viewModel: DatabaseViewModel

    private val home = PageEntity(title = "Casa")
    private val recipe = PageEntity(title = "Ricetta della crostata")
    private val notes = PageEntity(title = "Appunti")
    private val pantry = PageEntity(title = "Dispensa", isDatabase = true)
    private val link = DatabaseColumnEntity(pageId = pantry.id, name = "Vedi", type = ColumnType.PAGE_LINK)
    private val jam = DatabaseRowEntity(pageId = pantry.id, title = "Marmellata", orderIndex = 0)
    private val flour = DatabaseRowEntity(pageId = pantry.id, title = "Farina", orderIndex = 1)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pages = PageRepository(db)
        listOf(home, recipe, notes, pantry).forEach { pages.createPage(it) }
        db.blockDao().insertAll(
            listOf(BlockEntity(pageId = home.id, type = BlockType.DATABASE_LINK, linkedPageId = pantry.id, orderIndex = 0))
        )
        db.databaseDao().insertColumn(link)
        db.databaseDao().insertRow(jam)
        db.databaseDao().insertRow(flour)
        viewModel = DatabaseViewModel(DatabaseRepository(db), pages)
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

    @Test
    fun theCellShowsTheLinkedPageAsItIsNow() = runBlocking<Unit> {
        viewModel.load(pantry.id)
        waitFor("le righe") { viewModel.tableState.value.rows.size == 2 }
        viewModel.setCellValue(jam, link, recipe.id)
        waitFor("la pagina collegata") { viewModel.linkedPages.value[recipe.id]?.title == "Ricetta della crostata" }
        // Rinominata altrove, qui si vede subito.
        db.pageDao().setTitle(recipe.id, "Crostata", System.currentTimeMillis())
        waitFor("il titolo nuovo") { viewModel.linkedPages.value[recipe.id]?.title == "Crostata" }
    }

    @Test
    fun itCanLinkTheRowPageOfAnotherRowInTheSameDatabase() = runBlocking<Unit> {
        // Una riga mai aperta: la sua pagina nasce quando la si sceglie.
        val flourPage = pages.ensureRowPage(flour.id)!!
        db.databaseDao().insertCell(DatabaseCellEntity(rowId = jam.id, columnId = link.id, value = flourPage))
        viewModel.load(pantry.id)
        waitFor("la pagina della riga") { viewModel.linkedPages.value[flourPage]?.title == "Farina" }
    }

    @Test
    fun sortingByAPageLinkFollowsTheTitles() = runBlocking<Unit> {
        db.databaseDao().insertCell(DatabaseCellEntity(rowId = jam.id, columnId = link.id, value = recipe.id))
        db.databaseDao().insertCell(DatabaseCellEntity(rowId = flour.id, columnId = link.id, value = notes.id))
        viewModel.load(pantry.id)
        waitFor("le pagine") { viewModel.linkedPages.value.size == 2 }
        viewModel.setSort(link.id, descending = false)
        // "Appunti" prima di "Ricetta della crostata": prima Farina, poi Marmellata.
        waitFor("per titolo") { viewModel.tableState.value.rows.map { it.title } == listOf("Farina", "Marmellata") }
    }

    @Test
    fun aCopiedDatabaseLinksTheCopiesOfItsOwnRowPages() = runBlocking<Unit> {
        val flourPage = pages.ensureRowPage(flour.id)!!
        db.databaseDao().insertCell(DatabaseCellEntity(rowId = jam.id, columnId = link.id, value = flourPage))
        db.databaseDao().insertCell(DatabaseCellEntity(rowId = flour.id, columnId = link.id, value = recipe.id))
        val copyId = pages.duplicatePage(pantry.id, DuplicateTarget.NextToOriginal, "copy") { it }!!
        val copyColumn = db.databaseDao().getColumnsForPageOnce(copyId).single()
        val copyRows = db.databaseDao().getRowsForPageOnce(copyId)
        val cells = db.databaseDao().getAllCellsForPageOnce(copyId).filter { it.columnId == copyColumn.id }
        val copiedFlour = copyRows.single { it.title == "Farina" }
        val jamLink = cells.single { it.rowId == copyRows.single { r -> r.title == "Marmellata" }.id }.value
        // La marmellata copiata punta alla farina copiata; la ricetta, fuori, resta quella.
        assertEquals(copiedFlour.linkedPageId, jamLink)
        assertEquals(recipe.id, cells.single { it.rowId == copiedFlour.id }.value)
    }

    @Test
    fun fitToContentMeasuresTheTitle() {
        val measure: (String, androidx.compose.ui.text.TextStyle) -> androidx.compose.ui.unit.Dp = { text, _ -> (text.length * 7).dp }
        val titles = mapOf(recipe.id to "Crostata")
        // Icona 16, 6 punti, "Crostata" (8 lettere).
        assertEquals((16 + 6 + 56).dp, cellFitWidth(link, jam, recipe.id, Typography(), measure) { titles[it] })
        assertEquals((DbStrings.pageNotFound.length * 7).dp, cellFitWidth(link, jam, "sparita", Typography(), measure) { titles[it] })
        assertEquals(0.dp, cellFitWidth(link, jam, "", Typography(), measure) { titles[it] })
    }

    @Test
    fun fitToContentMeasuresWhatAUrlCellShows() {
        // 27/10/2026, nel Main calendar dell'utente: un link di Teams lungo
        // 70 lettere, mostrato come "Meeting Teams". La colonna si misura su
        // quello che si vede, non sul link intero.
        val measure: (String, androidx.compose.ui.text.TextStyle) -> androidx.compose.ui.unit.Dp = { text, _ -> (text.length * 7).dp }
        val url = DatabaseColumnEntity(pageId = "db", name = "URL", type = ColumnType.URL, orderIndex = 0)
        val teams = "https://teams.microsoft.com/l/meetup-join/19%3ameeting_abcdefghijklmno"
        // Dal 13/11/2026 accanto alla scritta c'è la matita per correggerla: anche lei.
        assertEquals(("Meeting Teams".length * 7 + 2).dp + LINK_PENCIL_SPACE, cellFitWidth(url, jam, teams, Typography(), measure, linkLabel = "Meeting Teams"))
        // Senza scritta, il link intero come prima; una cella vuota non ha niente da misurare.
        assertEquals((teams.length * 7 + 2).dp, cellFitWidth(url, jam, teams, Typography(), measure))
        assertEquals(2.dp, cellFitWidth(url, jam, "", Typography(), measure, linkLabel = "Meeting Teams"))
    }
}
