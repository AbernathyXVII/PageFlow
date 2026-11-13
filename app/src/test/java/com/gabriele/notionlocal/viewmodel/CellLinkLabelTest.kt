package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.LinkTitles
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.LinkColumnShow
import com.gabriele.notionlocal.data.entity.LinkLabel
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.repository.PageRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * **La scritta delle celle URL** (27/10/2026, "Show as…"): si salva con la
 * cella e la vista la ha per riga e colonna; il link resta il valore. Se il
 * link cambia, il titolo della pagina si rilegge per quello nuovo, la
 * scritta propria resta; svuotata la cella, non c'è più niente.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CellLinkLabelTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: DatabaseViewModel
    private val pokedex = PageEntity(title = "Pokédex", isDatabase = true)
    private val wiki = DatabaseColumnEntity(pageId = pokedex.id, name = "Bulbapedia", type = ColumnType.URL, orderIndex = 0)
    private val row = DatabaseRowEntity(pageId = pokedex.id, title = "Bulbasaur", orderIndex = 0)
    private val bulbasaur = "https://bulbapedia.bulbagarden.net/wiki/Bulbasaur"
    private val ivysaur = "bulbapedia.bulbagarden.net/wiki/Ivysaur"
    private val titles = mapOf(
        bulbasaur to "Bulbasaur (Pokémon)",
        "https://$ivysaur" to "Ivysaur  (Pokémon)\n"
    )
    private val original = LinkTitles.source
    private val key get() = row.id to wiki.id

    @Before
    fun setUp() = runBlocking<Unit> {
        LinkTitles.source = { url -> titles[url] }
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(pokedex)
        db.databaseDao().insertColumn(wiki)
        db.databaseDao().insertRow(row)
        db.databaseDao().insertCell(DatabaseCellEntity(rowId = row.id, columnId = wiki.id, value = bulbasaur))
        viewModel = DatabaseViewModel(DatabaseRepository(db), pages)
        viewModel.load(pokedex.id)
        waitFor("il database") { viewModel.tableState.value.cellValues[key] == bulbasaur }
    }

    @After
    fun tearDown() {
        LinkTitles.source = original
        db.close()
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < end) {
            // Il tempo passa anche per la pausa prima di rileggere il titolo.
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
            if (condition()) return
            Thread.sleep(10)
        }
        fail("Non è successo: $what — ${viewModel.tableState.value.linkLabels}")
    }

    private fun stored() = runBlocking { db.databaseDao().getCell(row.id, wiki.id)!! }

    @Test
    fun theTitleIsSavedAndFollowsTheLink() {
        assertNull(viewModel.tableState.value.linkLabels[key])
        viewModel.setCellLinkLabel(row, wiki, LinkLabel("Bulbasaur (Pokémon)", isTitle = true))
        waitFor("la scritta nella vista") { viewModel.tableState.value.linkLabels[key] == LinkLabel("Bulbasaur (Pokémon)", true) }
        assertEquals(bulbasaur, stored().value)

        // Un link nuovo, scritto senza "https://": il suo titolo, su una riga.
        viewModel.setCellValue(row, wiki, ivysaur)
        waitFor("il titolo nuovo") { viewModel.tableState.value.linkLabels[key] == LinkLabel("Ivysaur (Pokémon)", true) }
        assertEquals(ivysaur, stored().value)

        // Un link di cui il titolo non arriva: si torna al link.
        viewModel.setCellValue(row, wiki, "https://example.com/offline")
        waitFor("il link per intero") { viewModel.tableState.value.linkLabels[key] == null }
        assertNull(stored().linkLabel)
    }

    @Test
    fun yourTextStaysWhenTheLinkChanges() {
        viewModel.setCellLinkLabel(row, wiki, LinkLabel("Bulbapedia", isTitle = false))
        waitFor("la scritta") { viewModel.tableState.value.linkLabels[key]?.text == "Bulbapedia" }
        viewModel.setCellValue(row, wiki, ivysaur)
        waitFor("il link nuovo") { viewModel.tableState.value.cellValues[key] == ivysaur }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))
        assertEquals(LinkLabel("Bulbapedia", false), viewModel.tableState.value.linkLabels[key])
    }

    @Test
    fun anEmptiedCellHasNoText() {
        viewModel.setCellLinkLabel(row, wiki, LinkLabel("Bulbapedia", isTitle = false))
        waitFor("la scritta") { viewModel.tableState.value.linkLabels[key] != null }
        viewModel.setCellValue(row, wiki, "")
        waitFor("niente scritta") { viewModel.tableState.value.linkLabels[key] == null }
        assertNull(stored().linkLabel)
        // E senza link una scritta non si mette.
        viewModel.setCellLinkLabel(row, wiki, LinkLabel("Bulbapedia", isTitle = false))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
        assertNull(stored().linkLabel)
    }

    @Test
    fun theSearchFindsWhatTheCellShows() {
        viewModel.setCellLinkLabel(row, wiki, LinkLabel("Erba e veleno", isTitle = false))
        waitFor("la scritta") { viewModel.tableState.value.linkLabels[key] != null }
        viewModel.setSearchQuery("veleno")
        waitFor("la riga trovata") { viewModel.tableState.value.search?.foundInside == setOf(row.id) }
        assertEquals(listOf(row.id), viewModel.tableState.value.rows.map { it.id })
    }

    @Test
    fun theFullLinkAgain() {
        viewModel.setCellLinkLabel(row, wiki, LinkLabel("Bulbapedia", isTitle = false))
        waitFor("la scritta") { viewModel.tableState.value.linkLabels[key] != null }
        viewModel.setCellLinkLabel(row, wiki, null)
        waitFor("il link per intero") { viewModel.tableState.value.linkLabels[key] == null }
        assertEquals(bulbasaur, stored().value)
    }

    /**
     * **"Show as" della proprietà** (13/11/2026, chiesto dall'utente): con
     * "Page title" le celle col link e senza scritta lo leggono dal sito; con
     * "Full link" la scritta non si vede ma resta, e torna rimettendo il titolo.
     */
    @Test
    fun aPropertyShowingTitlesReadsThem() {
        val column = runBlocking { db.databaseDao().getColumnsForPageOnce(pokedex.id).single() }
        viewModel.setLinkColumnShow(column, LinkColumnShow.TITLE)
        waitFor("il titolo letto") { viewModel.tableState.value.linkLabels[key] == LinkLabel("Bulbasaur (Pokémon)", true) }
        assertEquals("TITLE", runBlocking { db.databaseDao().getColumnsForPageOnce(pokedex.id).single().numberDisplay })

        viewModel.setLinkColumnShow(column, LinkColumnShow.LINK)
        waitFor("il link per intero") { viewModel.tableState.value.linkLabels[key] == null }
        assertEquals(LinkLabel("Bulbasaur (Pokémon)", true), stored().label)

        viewModel.setLinkColumnShow(column, LinkColumnShow.TITLE)
        waitFor("di nuovo il titolo") { viewModel.tableState.value.linkLabels[key] != null }

        // Corretto con la matita e poi svuotato: il titolo si rilegge.
        viewModel.setCellLinkLabel(row, wiki, LinkLabel("Il primo", isTitle = false))
        waitFor("la scritta") { viewModel.tableState.value.linkLabels[key]?.text == "Il primo" }
        viewModel.setCellLinkLabel(row, wiki, null)
        waitFor("il titolo riletto") { viewModel.tableState.value.linkLabels[key] == LinkLabel("Bulbasaur (Pokémon)", true) }
    }
}
