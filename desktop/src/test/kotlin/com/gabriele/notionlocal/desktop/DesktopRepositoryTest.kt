package com.gabriele.notionlocal.desktop

import androidx.room.execSQL
import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.repository.PageRepository.DuplicateTarget
import com.gabriele.notionlocal.platform.DesktopContext
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Le repository sul PC** (dal 04/11/2026): le stesse del telefono, sul
 * database del PC (SQLite dentro l'app, in una cartella temporanea). Qui le
 * transazioni passano da `useWriterConnection` invece che dal
 * `withTransaction` di Android: duplicare, Annulla e Ripristina, buttare e
 * ripristinare devono fare le stesse cose che fanno sul telefono
 * (`PageRepositoryTest`, nel modulo `app`).
 *
 * La pagina di prova: il menu principale con "Parent", e in "Parent" due
 * righe di testo con in mezzo il collegamento a "Child".
 */
class DesktopRepositoryTest {
    private val dir: File = Files.createTempDirectory("pageflow-repo").toFile()
    private lateinit var db: AppDatabase
    private lateinit var repo: PageRepository

    private val root = PageEntity(id = PageEntity.ROOT_PAGE_ID, title = "Root")
    private val parent = PageEntity(title = "Parent")
    private val child = PageEntity(title = "Child")
    private lateinit var childLink: BlockEntity

    @Before
    fun setUp() = runBlocking {
        db = AppDatabase.getInstance(DesktopContext(dir))
        repo = PageRepository(db)
        listOf(root, parent, child).forEach { repo.createPage(it) }
        repo.saveBlock(BlockEntity(pageId = root.id, type = BlockType.PAGE_LINK, linkedPageId = parent.id, orderIndex = 0))
        repo.saveBlock(BlockEntity(pageId = parent.id, textJson = """[{"text":"before"}]""", orderIndex = 0))
        childLink = BlockEntity(pageId = parent.id, type = BlockType.PAGE_LINK, linkedPageId = child.id, orderIndex = 1)
        repo.saveBlock(childLink)
        repo.saveBlock(BlockEntity(pageId = parent.id, textJson = """[{"text":"after"}]""", orderIndex = 2))
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private suspend fun blocksOf(pageId: String) = db.blockDao().getBlocksForPageOnce(pageId).sortedBy { it.orderIndex }

    /**
     * **Un tipo di blocco che questa versione non conosce** (dal 12/11/2026,
     * prima delle colonne): arrivato da una versione più nuova, si legge come
     * una riga di testo invece di far chiudere l'app, e nel database resta
     * com'era.
     */
    @Test
    fun anUnknownBlockTypeReadsAsText() = runBlocking {
        val before = blocksOf(parent.id).first()
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction { execSQL("UPDATE blocks SET type = 'FROM_A_NEWER_VERSION' WHERE id = '${before.id}'") }
        }
        val read = blocksOf(parent.id)
        assertEquals(3, read.size)
        assertEquals(BlockType.PARAGRAPH, read.first().type)
        assertEquals("""[{"text":"before"}]""", read.first().textJson)
        val raw = db.useReaderConnection { connection ->
            connection.usePrepared("SELECT type FROM blocks WHERE id = '${before.id}'") { it.step(); it.getText(0) }
        }
        assertEquals("FROM_A_NEWER_VERSION", raw)
    }

    @Test
    fun duplicateUndoAndRedoWorkOnThePc() = runBlocking {
        val copyId = repo.duplicatePage(child.id, DuplicateTarget.NextToOriginal, "copy", besideLinkBlockId = childLink.id) { it }!!
        assertEquals(copyId, blocksOf(parent.id)[2].linkedPageId)
        assertTrue(db.pageDao().getById(copyId)!!.title.startsWith("Child"))

        val undone = repo.undoPageChange(repo.describeCopy(copyId))!!
        assertNotNull(db.pageDao().getById(copyId)!!.trashedAt)
        assertTrue(db.blockDao().getBlocksLinkingTo(copyId).isEmpty())

        repo.redoPageChange(undone)
        assertNull(db.pageDao().getById(copyId)!!.trashedAt)
        assertEquals(copyId, blocksOf(parent.id)[2].linkedPageId)
        assertEquals(child.id, blocksOf(parent.id)[1].linkedPageId)
    }

    @Test
    fun aPageGoesToTheTrashAndComesBack() = runBlocking {
        assertNotNull(repo.moveToTrash(child.id))
        assertNotNull(db.pageDao().getById(child.id)!!.trashedAt)
        assertTrue(db.blockDao().getBlocksLinkingTo(child.id).isEmpty())

        repo.restoreFromTrash(child.id)
        assertNull(db.pageDao().getById(child.id)!!.trashedAt)
        // Torna in fondo al menu principale, come sul telefono.
        assertEquals(child.id, blocksOf(root.id).last().linkedPageId)
    }

    @Test
    fun theSearchFindsTheTextOfAPage() = runBlocking {
        val hits = repo.search("before", inContent = true, inTrash = false)
        assertEquals(listOf(parent.id), hits.mapNotNull { it.pageId }.distinct())
    }
}
