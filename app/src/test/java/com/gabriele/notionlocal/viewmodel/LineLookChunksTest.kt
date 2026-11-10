package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.ui.screen.RenderItem
import com.gabriele.notionlocal.ui.screen.groupIntoRenderItems
import com.gabriele.notionlocal.ui.screen.lineScaleFor
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * **Lo spazio fra le righe non cambia da solo** (27/10/2026, trovato
 * dall'utente in "Fast notes": «Da Anki field codes in giù lo spacing è
 * diverso rispetto alle prime righe... scorrendo ancora più giù lo spacing
 * tra le righe cambia da solo»).
 *
 * Un campo di testo ha un'altezza di riga sola, presa dal corpo più grande
 * che contiene e dalle caselle da spuntare: un titolo col corpo a 20
 * allungava tutte le righe del suo pezzo da 50, e i pezzi sullo schermo che
 * si univano e si ridividevano portavano le righe dentro e fuori da quel
 * campo. Ora una riga che appare diversa da quella sopra comincia un campo,
 * e due campi che appaiono diversi non si uniscono.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LineLookChunksTest {

    private lateinit var db: AppDatabase
    private lateinit var pages: PageRepository
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(title = "Fast notes di prova")

    /** Un titolo a corpo 20, poi righe normali con tre caselle in mezzo (60, 61, 62). */
    private val lines = listOf(
        BlockEntity(pageId = page.id, textJson = """[{"text":"Anki field codes","bold":true,"size":20}]""", orderIndex = 0)
    ) + (1 until 120).map { i ->
        BlockEntity(
            pageId = page.id,
            type = if (i in 60..62) BlockType.CHECKBOX else BlockType.PARAGRAPH,
            textJson = """[{"text":"riga $i"}]""",
            orderIndex = i
        )
    }

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pages = PageRepository(db)
        pages.createPage(page)
        db.blockDao().insertAll(lines)
        viewModel = PageEditorViewModel(pages)
        viewModel.load(page.id)
        waitFor("le righe") { viewModel.blocks.value.size == lines.size }
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

    private fun top() = viewModel.blocks.value.filter { it.parentBlockId == null }

    /** I campi di testo come li disegna la pagina. */
    private fun runs() = groupIntoRenderItems(top(), viewModel.chunkStartsOf(top())).filterIsInstance<RenderItem.Run>()

    /** Come la schermata calcola l'altezza di riga di un campo: corpo più grande e caselle. */
    private fun lookOfField(run: RenderItem.Run) =
        lineScaleFor(run.blocks.flatMap { viewModel.spansOf(it) }) to run.blocks.any { it.type == BlockType.CHECKBOX }

    /** L'altezza di riga che ha adesso la riga [index]: quella del campo in cui sta. */
    private fun lookOfLine(index: Int) = lookOfField(runs().single { run -> run.blocks.any { it.id == lines[index].id } })

    private fun assertEveryFieldIsOfOneLook() {
        for (run in runs()) {
            val looks = run.blocks.map { lineScaleFor(viewModel.spansOf(it)) to (it.type == BlockType.CHECKBOX) }.toSet()
            assertEquals("un campo con righe diverse: ${run.blocks.map { viewModel.plainTextOf(it) }}", 1, looks.size)
        }
    }

    @Test
    fun theBigTitleAndTheCheckboxesHaveTheirOwnFields() {
        assertEveryFieldIsOfOneLook()
        assertEquals(listOf(lines[0].id), runs().first().blocks.map { it.id })
        assertEquals(listOf(lines[60].id, lines[61].id, lines[62].id), runs().single { lines[60].id in it.blocks.map { b -> b.id } }.blocks.map { it.id })
        // Le righe normali sotto il titolo hanno l'altezza di quelle normali.
        assertEquals(1f to false, lookOfLine(1))
        assertEquals(1.25f to false, lookOfLine(0))
    }

    @Test
    fun scrollingNeverChangesTheSpacingOfALine() {
        val normal = listOf(1, 30, 50, 51, 59, 63, 100, 119)
        val before = normal.map { lookOfLine(it) }
        // La pagina che scorre: le righe sullo schermo, una finestra dopo l'altra.
        for (from in 0 until 100 step 7) {
            viewModel.keepVisibleTogether(lines.subList(from, from + 20).map { it.id }, focusedId = null)
            assertEveryFieldIsOfOneLook()
            assertEquals("con le righe da $from sullo schermo", before, normal.map { lookOfLine(it) })
        }
    }

    @Test
    fun aPieceStartsLikeTheOneAboveSoTheTextDoesNotMove() = runBlocking<Unit> {
        // Un elenco puntato lungo con una riga normale ogni tre, che comincia
        // con una normale: Compose mette il testo di un campo guardando la sua
        // prima riga, e un pallino (scritto più grande del testo) lo sposta.
        val list = PageEntity(title = "Elenco")
        val lines = (0 until 150).map { i ->
            val bullet = i % 3 != 0
            BlockEntity(
                pageId = list.id,
                type = if (bullet) BlockType.BULLET_LIST_ITEM else BlockType.PARAGRAPH,
                indentLevel = if (bullet) 1 else 0,
                textJson = """[{"text":"voce $i"}]""",
                orderIndex = i
            )
        }
        pages.createPage(list)
        db.blockDao().insertAll(lines)
        val vm = PageEditorViewModel(pages)
        vm.load(list.id)
        waitFor("l'elenco") { vm.blocks.value.size == lines.size }
        fun top() = vm.blocks.value.filter { it.parentBlockId == null }
        fun firstTypes() = groupIntoRenderItems(top(), vm.chunkStartsOf(top())).filterIsInstance<RenderItem.Run>()
            .map { it.blocks.first().type }

        assertTrue("una pagina così lunga va in più pezzi", firstTypes().size > 1)
        assertEquals(setOf(BlockType.PARAGRAPH), firstTypes().toSet())
        // E scorrendo nessun pezzo comincia col pallino.
        for (from in 0 until 130 step 9) {
            vm.keepVisibleTogether(lines.subList(from, from + 20).map { it.id }, focusedId = null)
            assertEquals("con le righe da $from sullo schermo", setOf(BlockType.PARAGRAPH), firstTypes().toSet())
        }
    }

    @Test
    fun theFieldWithTheCursorLetsTheBigTitleGoAndKeepsItsKey() = runBlocking<Unit> {
        // Una pagina col solo titolo a 50; sotto si scrivono due righe, e mentre
        // si scrive i campi non si ridividono: finiscono nel campo del titolo.
        val short = PageEntity(title = "Corta")
        val title = BlockEntity(pageId = short.id, textJson = """[{"text":"Titolo","size":50}]""", orderIndex = 0)
        pages.createPage(short)
        db.blockDao().insertAll(listOf(title))
        val vm = PageEditorViewModel(pages)
        vm.load(short.id)
        waitFor("il titolo") { vm.blocks.value.size == 1 }
        fun top() = vm.blocks.value.filter { it.parentBlockId == null }
        fun runs() = groupIntoRenderItems(top(), vm.chunkStartsOf(top())).filterIsInstance<RenderItem.Run>()
        fun idOf(text: String) = top().single { vm.plainTextOf(it) == text }.id
        vm.setEditing(true)
        vm.updateRun(listOf(title.id), listOf("Titolo", "uno", "due"), listOf("Titolo"), caretLine = 1)
        waitFor("le righe scritte") { vm.blocks.value.size == 3 }
        val before = runs().single().key

        // Mentre si scrive non si tocca niente; dopo la pausa il titolo esce.
        assertTrue(vm.keepVisibleTogether(top().map { it.id }, focusedId = idOf("uno")))
        assertEquals(1, runs().size)
        Thread.sleep(PageEditorViewModel.TYPING_QUIET_MS + 100)
        vm.keepVisibleTogether(top().map { it.id }, focusedId = idOf("uno"))
        val after = runs()
        assertEquals(listOf(listOf("Titolo"), listOf("uno", "due")), after.map { run -> run.blocks.map { vm.plainTextOf(it) } })
        // Il campo col cursore è lo stesso di prima: stessa chiave.
        assertEquals(before, after.single { run -> run.blocks.any { it.id == idOf("uno") } }.key)
        assertEquals(after.size, after.map { it.key }.toSet().size)

        // Smesso di scrivere, i pezzi si rifanno: il titolo riprende la chiave
        // del gruppo, e le chiavi restano tutte diverse.
        vm.setEditing(false)
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(3))
        assertEquals(before, runs().first().key)
        assertEquals(runs().size, runs().map { it.key }.toSet().size)
        assertEquals(listOf(listOf("Titolo"), listOf("uno", "due")), runs().map { run -> run.blocks.map { vm.plainTextOf(it) } })
    }

    /**
     * **Selezionando la fine del titolo grande le righe sotto non si allargano**
     * (dal 10/11/2026, trovato dall'utente sul PC: tutto il testo sotto il
     * titolo di una pagina presa dal Web Clipper prendeva lo spazio del titolo).
     * La selezione che arriva in fondo a un campo si porta dietro quello dopo
     * solo se le sue righe appaiono uguali.
     */
    @Test
    fun selectingTheEndOfTheBigTitleDoesNotStretchTheLinesBelow() {
        assertFalse(viewModel.joinChunkAfter(lines[0].id))
        assertEquals(listOf(lines[0].id), runs().first().blocks.map { it.id })
        assertEquals(1f to false, lookOfLine(1))
        assertEveryFieldIsOfOneLook()
    }

    @Test
    fun linesThatLookAlikeStillJoinOnTheScreen() {
        // Le righe 45-55 sullo schermo: il confine dei 50 fra loro se ne va...
        viewModel.keepVisibleTogether(lines.subList(45, 56).map { it.id }, focusedId = null)
        val field = runs().single { run -> run.blocks.any { it.id == lines[50].id } }
        assertTrue(field.blocks.any { it.id == lines[51].id })
        // ...ma il titolo e le caselle no, anche se si vedono.
        viewModel.keepVisibleTogether(lines.subList(0, 3).map { it.id } + lines.subList(58, 65).map { it.id }, focusedId = null)
        assertEquals(listOf(lines[0].id), runs().first().blocks.map { it.id })
        assertEveryFieldIsOfOneLook()
    }
}
