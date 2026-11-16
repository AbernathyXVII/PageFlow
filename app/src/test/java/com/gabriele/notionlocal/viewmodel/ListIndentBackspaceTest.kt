package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.heldListType
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.ui.screen.bulletMarkerFor
import com.gabriele.notionlocal.ui.screen.numberedMarkerFor
import com.gabriele.notionlocal.ui.screen.runPrefixes
import kotlinx.coroutines.runBlocking
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
 * **Il backspace a inizio di un elemento di elenco, come su Word**
 * (18/10/2026, chiesto dall'utente con tre foto): il primo toglie il numero
 * o il pallino e il testo resta dov'era, allineato agli altri; il secondo lo
 * porta a sinistra. La numerazione dopo continua (5, 6…).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ListIndentBackspaceTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(title = "Elenchi")

    private fun item(
        text: String,
        at: Int,
        type: BlockType = BlockType.NUMBERED_LIST_ITEM,
        indent: Int = 0,
        parent: String? = null
    ) = BlockEntity(
        pageId = page.id,
        parentBlockId = parent,
        type = type,
        indentLevel = indent,
        textJson = """[{"text":"$text"}]""",
        orderIndex = at
    )

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(page)
        viewModel = PageEditorViewModel(pages)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun open(blocks: List<BlockEntity>) {
        runBlocking { db.blockDao().insertAll(blocks) }
        viewModel.load(page.id)
        waitFor("le righe") { viewModel.blocks.value.size == blocks.size }
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

    private fun shown(id: String) = viewModel.blocks.value.single { it.id == id }

    private fun run(ids: List<String>) = ids.map { shown(it) }

    /** I prefissi come li disegna il testo unito. */
    private fun prefixes(ids: List<String>) =
        run(ids).let { blocks -> runPrefixes(blocks.map { viewModel.plainTextOf(it) }, blocks, viewModel.numberedOrdinals(viewModel.blocks.value), viewModel) }

    @Test
    fun likeWordTheFirstBackspaceKeepsThePlaceTheSecondGoesLeft() {
        // Come nelle foto: 1..6, il cursore a inizio del quinto.
        val items = (0 until 6).map { item("A", it) }
        val ids = items.map { it.id }
        open(items)

        // Il primo: il numero se ne va, il testo resta al suo posto.
        assertEquals(BackspaceOutcome.STEP_UNDONE, viewModel.backspaceAtLineStartDetailed(ids, 4))
        waitFor("senza numero") { shown(ids[4]).heldListType == BlockType.NUMBERED_LIST_ITEM }
        assertEquals(BlockType.PARAGRAPH, shown(ids[4]).type)
        assertEquals("A", viewModel.plainTextOf(shown(ids[4])))
        val held = prefixes(ids)
        // Il numero c'è ma non si vede, ed è largo come quello sopra: il testo è allineato.
        assertTrue(held[4].hidden)
        assertEquals(numberedMarkerFor(0, 5) + "  ", held[4].marker)
        assertEquals(held[3].text.length, held[4].text.length)
        // La numerazione continua: quello dopo è il 5.
        assertEquals(5, viewModel.numberedListOrdinal(viewModel.blocks.value, shown(ids[5])))

        // Il secondo: il testo va a sinistra, senza spazio davanti.
        assertEquals(BackspaceOutcome.STEP_UNDONE, viewModel.backspaceAtLineStartDetailed(ids, 4))
        waitFor("a sinistra") { shown(ids[4]).heldListType == null }
        assertEquals(BlockType.PARAGRAPH, shown(ids[4]).type)
        val left = prefixes(ids)
        assertFalse(left[4].hidden)
        assertEquals("", left[4].text)
        assertEquals(5, viewModel.numberedListOrdinal(viewModel.blocks.value, shown(ids[5])))

        // Il terzo: non c'è più niente da togliere, il campo unisce la riga a quella sopra.
        assertEquals(BackspaceOutcome.NOTHING, viewModel.backspaceAtLineStartDetailed(ids, 4))
    }

    @Test
    fun bulletsKeepThePlaceOfTheBullet() {
        val items = (0 until 3).map { item("Punto", it, type = BlockType.BULLET_LIST_ITEM) }
        val ids = items.map { it.id }
        open(items)
        viewModel.backspaceAtLineStartDetailed(ids, 1)
        waitFor("senza pallino") { shown(ids[1]).heldListType == BlockType.BULLET_LIST_ITEM }
        val held = prefixes(ids)
        assertTrue(held[1].hidden)
        assertEquals(bulletMarkerFor(0) + "  ", held[1].marker)
        assertEquals(held[0].text, held[1].text)
    }

    @Test
    fun enterFromItContinuesTheList() {
        val items = (0 until 3).map { item("A", it) }
        val ids = items.map { it.id }
        open(items)
        viewModel.backspaceAtLineStartDetailed(ids, 1)
        waitFor("senza numero") { shown(ids[1]).heldListType != null }
        // Invio in fondo alla riga senza numero (dal 16/11/2026, chiesto dall'utente):
        // la nuova è una voce dell'elenco, col numero dopo.
        viewModel.updateRun(ids, listOf("A", "A", "B", "A"), listOf("A", "A", "A"), caretLine = 2)
        waitFor("la riga nuova") { viewModel.blocks.value.size == 4 }
        val added = viewModel.blocks.value.single { it.id !in ids }
        assertEquals(BlockType.NUMBERED_LIST_ITEM, added.type)
        assertNull(added.heldListType)
        assertEquals(2, viewModel.numberedListOrdinal(viewModel.blocks.value, added))
        assertEquals(3, viewModel.numberedListOrdinal(viewModel.blocks.value, shown(ids[2])))
        // Prima di arrivare al database la riga nuova si disegna già col suo numero.
        val drawn = runPrefixes(listOf("A", "A", "B", "A"), viewModel.blocks.value.filter { it.id in ids }.sortedBy { it.orderIndex }, emptyMap(), viewModel)
        assertEquals(numberedMarkerFor(0, 2) + "  ", drawn[2].marker)
    }

    @Test
    fun shiftEnterMakesALineThatKeepsThePlace() {
        val items = (0 until 2).map { item("A", it) }
        val ids = items.map { it.id }
        open(items)
        // Maiusc+Invio in fondo alla prima voce: la riga nuova non ha numero ma resta allineata.
        viewModel.updateRun(ids, listOf("A", "B", "A"), listOf("A", "A"), caretLine = 1, holdNewLine = true)
        waitFor("la riga nuova") { viewModel.blocks.value.size == 3 }
        val added = viewModel.blocks.value.single { it.id !in ids }
        assertEquals(BlockType.PARAGRAPH, added.type)
        assertEquals(BlockType.NUMBERED_LIST_ITEM, added.heldListType)
        assertEquals(2, viewModel.numberedListOrdinal(viewModel.blocks.value, shown(ids[1])))
    }

    @Test
    fun becomingSomethingElseForgetsThePlace() {
        val items = (0 until 2).map { item("A", it) }
        val ids = items.map { it.id }
        open(items)
        viewModel.backspaceAtLineStartDetailed(ids, 1)
        waitFor("senza numero") { shown(ids[1]).heldListType != null }
        // Diventata casella e poi di nuovo testo, non torna rientrata.
        viewModel.updateBlockType(shown(ids[1]), BlockType.CHECKBOX)
        waitFor("casella") { shown(ids[1]).type == BlockType.CHECKBOX }
        viewModel.updateBlockType(shown(ids[1]), BlockType.PARAGRAPH)
        waitFor("testo") { shown(ids[1]).type == BlockType.PARAGRAPH }
        assertNull(shown(ids[1]).heldListType)
        assertFalse(prefixes(ids)[1].hidden)
    }

    @Test
    fun anIndentedItemThenGoesBackOneLevel() {
        val items = listOf(item("Uno", 0), item("Sotto", 1, indent = 1))
        val ids = items.map { it.id }
        open(items)
        viewModel.backspaceAtLineStartDetailed(ids, 1)
        waitFor("senza numero") { shown(ids[1]).heldListType != null }
        viewModel.backspaceAtLineStartDetailed(ids, 1)
        waitFor("a sinistra del suo rientro") { shown(ids[1]).heldListType == null }
        assertEquals(1, shown(ids[1]).indentLevel)
        // Poi, come sempre, un rientro alla volta.
        viewModel.backspaceAtLineStartDetailed(ids, 1)
        waitFor("un rientro in meno") { shown(ids[1]).indentLevel == 0 }
    }

    @Test
    fun insideAToggleTheSameTwoSteps() {
        val toggle = BlockEntity(pageId = page.id, type = BlockType.TOGGLE, textJson = """[{"text":"Titolo"}]""", orderIndex = 0)
        val child = item("Dentro", 0, parent = toggle.id)
        open(listOf(toggle, child))
        // La riga resta la stessa casella di scrittura: il fuoco non si sposta.
        assertFalse(viewModel.backspaceAtIslandStart(shown(child.id)))
        waitFor("senza numero") { shown(child.id).heldListType == BlockType.NUMBERED_LIST_ITEM }
        assertFalse(viewModel.backspaceAtIslandStart(shown(child.id)))
        waitFor("a sinistra") { shown(child.id).heldListType == null }
        assertEquals(BlockType.PARAGRAPH, shown(child.id).type)
        assertEquals("Dentro", viewModel.plainTextOf(shown(child.id)))
    }

    @Test
    fun aCheckboxStillJustBecomesText() {
        val items = listOf(item("Fare", 0, type = BlockType.CHECKBOX), item("Altro", 1, type = BlockType.CHECKBOX))
        val ids = items.map { it.id }
        open(items)
        viewModel.backspaceAtLineStartDetailed(ids, 1)
        waitFor("testo") { shown(ids[1]).type == BlockType.PARAGRAPH }
        assertNull(shown(ids[1]).heldListType)
    }
}
