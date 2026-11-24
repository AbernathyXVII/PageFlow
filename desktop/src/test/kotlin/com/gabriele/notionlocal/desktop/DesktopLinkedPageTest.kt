package com.gabriele.notionlocal.desktop

import androidx.lifecycle.viewmodel.CreationExtras
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **"Delete linked page" e "Lock with password" dal menu del blocco di un database** (24/11/2026, chiesti
 * dall'utente nel collaudo): via solo il collegamento, l'originale resta; una vista collegata va nel
 * cestino con lui. Il blocco con la password si mette e si toglie anche da lì.
 */
class DesktopLinkedPageTest {
    private val dir: File = Files.createTempDirectory("pageflow-linked").toFile()
    private val context = DesktopContext(dir)

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    @Test
    fun deleteLinkedPageKeepsTheOriginal() {
        AppSettings.init(context)
        val db = AppDatabase.getInstance(context)
        val repo = PageRepository(db)
        val toOriginal = BlockEntity(id = "b-x", pageId = "casa", type = BlockType.PAGE_LINK, linkedPageId = "x", orderIndex = 0)
        val toView = BlockEntity(id = "b-v", pageId = "altrove", type = BlockType.DATABASE_LINK, linkedPageId = "v", orderIndex = 0)
        runBlocking {
            repo.createPage(PageEntity(id = "casa", title = "Casa"))
            repo.createPage(PageEntity(id = "altrove", title = "Altrove"))
            repo.createPage(PageEntity(id = "x", title = "Serie A", isDatabase = true, parentId = "casa"))
            repo.createPage(PageEntity(id = "v", title = "Serie A", isDatabase = true, sourceDatabaseId = "x", parentId = "altrove"))
            repo.saveBlock(toOriginal)
            repo.saveBlock(toView)
        }
        val factory = ViewModelFactory(context)
        val viewModel = onWindowThread { factory.create(PageEditorViewModel::class, CreationExtras.Empty) }
        fun waitFor(what: String, check: () -> Boolean) {
            val end = System.currentTimeMillis() + 5_000
            while (!check() && System.currentTimeMillis() < end) Thread.sleep(20)
            assertTrue(what, check())
        }
        fun page(id: String) = runBlocking { repo.getPage(id) }
        fun block(id: String) = runBlocking { db.blockDao().getById(id) }

        // Il collegamento al database vero: via il blocco, il database resta vivo.
        onWindowThread { viewModel.deleteLinkedPage(toOriginal, page("x")!!) }
        waitFor("via il collegamento") { block("b-x") == null }
        assertNull("il database non va nel cestino", page("x")!!.trashedAt)

        // Una vista collegata: via il blocco e la vista nel cestino, l'originale intatto.
        onWindowThread { viewModel.deleteLinkedPage(toView, page("v")!!) }
        waitFor("via la vista") { block("b-v") == null && page("v")!!.trashedAt != null }
        assertNull(page("x")!!.trashedAt)

        // "Lock with password" dal menu del blocco: si mette e si toglie.
        onWindowThread { viewModel.setPasswordLockedFor("x", true) }
        waitFor("chiuso con la password") { page("x")!!.isProtected }
        onWindowThread { viewModel.setPasswordLockedFor("x", false) }
        waitFor("riaperto") { !page("x")!!.isProtected }
        assertNotNull(page("x"))
        assertEquals(null, runBlocking { repo.lockingPageOf("x") })
    }
}
