package com.gabriele.notionlocal.desktop

import androidx.compose.ui.geometry.Offset
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.ui.screen.BlockBox
import com.gabriele.notionlocal.ui.screen.BlockDrop
import com.gabriele.notionlocal.ui.screen.DropPlace
import com.gabriele.notionlocal.ui.screen.dropFor
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Un blocco trascinato dentro un toggle** (25/11/2026, chiesto dall'utente: "consentimi di spostare
 * blocchi direttamente dentro di essi"): sulla metà bassa del titolo del toggle, col mouse sul testo,
 * il blocco va per primo dentro di lui, e il toggle chiuso si apre.
 */
class DesktopToggleDropTest {
    private val dir: File = Files.createTempDirectory("pageflow-toggledrop").toFile()
    private val context = DesktopContext(dir)

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    @Test
    fun theTitleOfAToggleTakesTheBlockInside() {
        val blocks = listOf(
            BlockEntity(id = "a", pageId = "p", orderIndex = 0),
            BlockEntity(id = "t", pageId = "p", type = BlockType.TOGGLE, orderIndex = 1),
            BlockEntity(id = "in", pageId = "p", parentBlockId = "t", orderIndex = 0),
            BlockEntity(id = "b", pageId = "p", orderIndex = 2)
        )
        val box = BlockBox(100f, 120f, 48f, 480f)
        fun drop(dragged: String, x: Float, y: Float) = dropFor(dragged, "t", Offset(x, y), box, 60f, blocks, nested = 28f)
        // Metà bassa, sul testo: dentro, con la linea dove cominciano le sue righe (28 più a destra).
        assertEquals(BlockDrop("t", DropPlace.INSIDE, 76f, 120f, 404f, vertical = false), drop("b", 200f, 115f))
        // Metà alta: sopra, con la linea dal bordo della riga.
        assertEquals(BlockDrop("t", DropPlace.BEFORE, 48f, 100f, 432f, vertical = false), drop("b", 200f, 105f))
        // Dal margine dei puntini: sotto il toggle, non dentro.
        assertEquals(DropPlace.AFTER, drop("a", 10f, 115f)?.place)
        // Già il primo dentro: resta dov'è. Il toggle dentro se stesso no.
        assertEquals(null, drop("in", 200f, 115f))
        assertEquals(null, drop("t", 200f, 115f))
        // Sul bordo del testo restano le colonne.
        assertEquals(DropPlace.RIGHT, drop("b", 470f, 115f)?.place)
    }

    @Test
    fun theBlockGoesFirstInsideAndTheToggleOpens() = runBlocking {
        AppSettings.init(context)
        val db = AppDatabase.getInstance(context)
        val repo = PageRepository(db)
        repo.createPage(PageEntity(id = "p", title = "P"))
        listOf(
            BlockEntity(id = "a", pageId = "p", orderIndex = 0),
            BlockEntity(id = "t", pageId = "p", type = BlockType.TOGGLE, isExpanded = false, orderIndex = 1),
            BlockEntity(id = "in", pageId = "p", parentBlockId = "t", orderIndex = 0),
            BlockEntity(id = "b", pageId = "p", orderIndex = 2)
        ).forEach { repo.saveBlock(it) }

        assertTrue(repo.moveBlockInto("b", "t"))
        val after = db.blockDao().getBlocksForPageOnce("p").associateBy { it.id }
        assertEquals("t", after["b"]!!.parentBlockId)
        assertEquals(0, after["b"]!!.orderIndex)
        assertEquals(1, after["in"]!!.orderIndex)
        assertTrue("il toggle si apre", after["t"]!!.isExpanded)
        // Il toggle non va dentro un blocco che ha dentro, né dentro se stesso.
        assertEquals(false, repo.moveBlockInto("t", "t"))
        repo.saveBlock(BlockEntity(id = "t2", pageId = "p", type = BlockType.TOGGLE, parentBlockId = "t", orderIndex = 2))
        assertEquals(false, repo.moveBlockInto("t", "t2"))
        // "a" dentro: le righe della pagina si richiudono senza buchi.
        assertTrue(repo.moveBlockInto("a", "t"))
        val top = db.blockDao().getBlocksForPageOnce("p").filter { it.parentBlockId == null }.sortedBy { it.orderIndex }
        assertEquals(listOf("t" to 0), top.map { it.id to it.orderIndex })
    }
}
