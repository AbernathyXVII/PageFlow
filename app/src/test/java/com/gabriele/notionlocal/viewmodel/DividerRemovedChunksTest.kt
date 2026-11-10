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
 * **Tolto il divisore sotto un titolo grande, le righe sotto restano alte
 * come le altre** (dal 10/11/2026, trovato dall'utente sul PC: cancellato un
 * divider, tutte le righe prendevano lo spazio del titolo). Le righe prima e
 * dopo un divisore sono due gruppi; tolto lui diventano uno, e il primo pezzo
 * del gruppo sotto non aveva un confine suo: finiva nel campo del titolo, con
 * la sua altezza di riga, finché si scriveva (i pezzi non si ridividono).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DividerRemovedChunksTest {
    private lateinit var db: AppDatabase
    private lateinit var pages: PageRepository
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(title = "Una pagina presa dal Web Clipper")
    private val title = BlockEntity(pageId = page.id, textJson = """[{"text":"With that out of the way,","bold":true,"size":20}]""", orderIndex = 0)
    private val divider = BlockEntity(pageId = page.id, type = BlockType.DIVIDER, orderIndex = 1)
    private val lines = (2..5).map { i -> BlockEntity(pageId = page.id, textJson = """[{"text":"riga $i"}]""", orderIndex = i) }

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pages = PageRepository(db)
        pages.createPage(page)
        db.blockDao().insertAll(listOf(title, divider) + lines)
        viewModel = PageEditorViewModel(pages)
        viewModel.load(page.id)
        waitFor("le righe") { viewModel.blocks.value.size == 6 }
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

    private fun runs(): List<List<String>> {
        val top = viewModel.blocks.value.filter { it.parentBlockId == null }
        return groupIntoRenderItems(top, viewModel.chunkStartsOf(top)).filterIsInstance<RenderItem.Run>().map { run -> run.blocks.map { it.id } }
    }

    @Test
    fun theLinesBelowARemovedDividerDoNotJoinTheBigTitle() = runBlocking<Unit> {
        // Si sta scrivendo (il cursore in una riga): i pezzi non si ridividono.
        viewModel.setEditing(true)
        pages.deleteBlock(divider)
        waitFor("il divisore tolto") { viewModel.blocks.value.none { it.id == divider.id } }
        assertEquals(listOf(listOf(title.id), lines.map { it.id }), runs())
    }
}
