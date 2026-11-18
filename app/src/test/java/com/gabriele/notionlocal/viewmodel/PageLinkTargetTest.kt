package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.data.entity.blockLink
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
 * **L'indice navigabile** (20/10/2026): il pin dal menu dei sei puntini fa di
 * una riga un punto di arrivo, e il collegamento a lei si salva sul testo come
 * quello a un sito.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PageLinkTargetTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(title = "Libro")
    private val index = BlockEntity(pageId = page.id, type = BlockType.PARAGRAPH, textJson = """[{"text":"Capitolo 1"}]""", orderIndex = 0)
    private val chapter = BlockEntity(pageId = page.id, type = BlockType.PARAGRAPH, textJson = """[{"text":"Capitolo 1 - L'inizio"}]""", orderIndex = 1)
    private val large = BlockEntity(pageId = page.id, type = BlockType.PARAGRAPH, textJson = """[{"text":"Capitolo 2","size":24}]""", orderIndex = 2)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(page)
        db.blockDao().insertAll(listOf(index, chapter, large))
        viewModel = PageEditorViewModel(pages)
        viewModel.load(page.id)
        waitFor("le righe") { viewModel.blocks.value.size == 3 }
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

    private fun saved(id: String) = runBlocking { db.blockDao().getBlocksForPageOnce(page.id) }.single { it.id == id }

    @Test
    fun theLargeLineIsATargetFromTheStartAndThePinAddsAnother() {
        // Scritta in grande, la riga è già un punto di arrivo.
        assertEquals(listOf(large.id), viewModel.linkTargets().map { it.blockId })

        viewModel.setLinkTarget(viewModel.blocks.value.single { it.id == chapter.id }, true)
        waitFor("il pin salvato") { saved(chapter.id).attrs.linkTarget }
        waitFor("il pin nell'elenco") { viewModel.linkTargets().map { it.blockId } == listOf(chapter.id, large.id) }
        assertEquals("Capitolo 1 - L'inizio", viewModel.linkTargets().first().label)

        // Tolto il pin, la riga non è più nell'elenco.
        viewModel.setLinkTarget(viewModel.blocks.value.single { it.id == chapter.id }, false)
        waitFor("il pin tolto") { viewModel.linkTargets().map { it.blockId } == listOf(large.id) }
    }

    /** **"Flag (for Table of contents)"** (18/11/2026): il blocco entra nell'indice a destra, e se ne toglie. */
    @Test
    fun theFlagPutsTheLineInTheTableOfContents() {
        assertTrue("di suo nessuna voce, nemmeno il titolo grande", viewModel.tocTargets().isEmpty())
        viewModel.setTocFlag(chapter, true)
        waitFor("segnata") { saved(chapter.id).attrs.tocFlag }
        waitFor("nell'indice") { viewModel.tocTargets().map { it.blockId } == listOf(chapter.id) }
        viewModel.setTocFlag(viewModel.blocks.value.single { it.id == chapter.id }, false)
        waitFor("tolta") { !saved(chapter.id).attrs.tocFlag && viewModel.tocTargets().isEmpty() }
    }

    @Test
    fun theIndexEntryLinksToTheChapter() {
        // "Capitolo 1" selezionato nella prima riga, collegato alla terza.
        viewModel.reportSelection(listOf(index.id, chapter.id), 0, 10)
        viewModel.captureSelection()
        assertTrue(viewModel.applyLinkToCapturedSelection(blockLink(large.id)))
        waitFor("il collegamento salvato") {
            viewModel.spansOf(saved(index.id)) == listOf(RichTextSpan(text = "Capitolo 1", link = blockLink(large.id)))
        }
        // Le altre righe non cambiano.
        assertEquals(listOf(RichTextSpan(text = "Capitolo 1 - L'inizio")), viewModel.spansOf(saved(chapter.id)))
    }
}
