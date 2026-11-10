package com.gabriele.notionlocal.desktop

import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.buildAppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.VersionReason
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.sync.SyncConfirm
import com.gabriele.notionlocal.data.sync.SyncDatabase
import com.gabriele.notionlocal.data.sync.SyncDiscovery
import com.gabriele.notionlocal.data.sync.SyncPrompts
import com.gabriele.notionlocal.data.sync.SyncServer
import com.gabriele.notionlocal.data.sync.SyncResult
import com.gabriele.notionlocal.data.sync.SyncSession
import com.gabriele.notionlocal.data.sync.SyncSide
import com.gabriele.notionlocal.data.sync.SyncStore
import com.gabriele.notionlocal.platform.DesktopContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.ServerSocket
import java.nio.file.Files

/**
 * **La sincronizzazione Wi‑Fi** (dal 10/11/2026) fra due PageFlow finti sullo
 * stesso PC, ognuno col suo database e la sua cartella, attraverso una vera
 * connessione (127.0.0.1): l'abbinamento col codice, le pagine e i file
 * nuovi dalle due parti, le modifiche, un conflitto (vince l'ultima, l'altra
 * resta nella cronologia) e una pagina cancellata (dall'altra parte va nel
 * cestino). Alla fine, una sincronizzazione senza novità non manda niente.
 */
class DesktopSyncTest {
    private class Device(name: String) {
        val dir: File = Files.createTempDirectory("pageflow-sync-$name").toFile()
        val db: AppDatabase = buildAppDatabase(DesktopContext(dir))
        val repo = PageRepository(db)
        val files = File(dir, "page-images").apply { mkdirs() }
        val side = SyncSide(db, files, SyncStore(File(dir, "sync.json")), name)
        val codes = ArrayList<String>()
        val confirm = SyncConfirm { _, code -> codes += code; true }

        suspend fun page(id: String, vararg lines: String) {
            repo.createPage(PageEntity(id = id, title = id))
            lines.forEachIndexed { i, text -> repo.saveBlock(BlockEntity(id = "$id-$i", pageId = id, textJson = """[{"text":"$text"}]""", orderIndex = i)) }
        }

        suspend fun texts(pageId: String): List<String> =
            db.blockDao().getBlocksForPageOnce(pageId).sortedBy { it.orderIndex }.map { it.textJson.substringAfter("\"text\":\"").substringBefore("\"") }

        suspend fun setText(pageId: String, index: Int, text: String) {
            val block = db.blockDao().getBlocksForPageOnce(pageId).single { it.orderIndex == index }
            repo.saveBlock(block.copy(textJson = """[{"text":"$text"}]"""))
        }
    }

    private val phone = Device("telefono")
    private val pc = Device("pc")

    @After
    fun tearDown() {
        phone.db.close()
        pc.db.close()
    }

    private fun sync(): Pair<SyncResult, SyncResult?> = runBlocking {
        ServerSocket(0).use { server ->
            val answered = async(Dispatchers.IO) { SyncSession.serve(pc.side, server.accept(), pc.confirm) }
            val result = SyncSession.runClient(phone.side, "127.0.0.1", server.localPort, phone.confirm)
            result to answered.await()
        }
    }

    /**
     * **Il telefono trova il PC da solo**: il PC che aspetta (`SyncServer`)
     * risponde alla domanda mandata a tutta la rete, e la sincronizzazione
     * parte all'indirizzo e alla porta della risposta. Il codice sul PC lo
     * conferma chi guarda `SyncPrompts`, come la finestra di PageFlow.
     */
    @Test
    fun thePhoneFindsThePcOnTheNetwork() = runBlocking<Unit> {
        pc.page("lavoro", "riunione")
        assertTrue(SyncServer.start(pc.side))
        val answering = launch(Dispatchers.IO) {
            SyncPrompts.current.collect { request -> request?.answer?.complete(true) }
        }
        try {
            val found = SyncDiscovery.search(waitMs = 1_500).single { it.name == "pc" }
            val result = SyncSession.runClient(phone.side, found.host, found.port, phone.confirm)
            assertEquals(1, result.received)
            assertEquals(listOf("riunione"), phone.texts("lavoro"))
            assertEquals("telefono", pc.side.store.load().peers.single().name)
        } finally {
            answering.cancel()
            SyncServer.stop()
        }
    }

    /**
     * **Una pagina nuova nel menu principale da tutte e due le parti**: il
     * menu è in conflitto e ne vince uno, ma la pagina dell'altro non resta
     * senza posto; col secondo giro (come fa il telefono dopo un conflitto)
     * tutti e due i menu le hanno tutte e due.
     */
    @Test
    fun newPagesOnBothSidesKeepTheirPlace() = runBlocking<Unit> {
        for (device in listOf(phone, pc)) device.repo.createPage(PageEntity(id = PageEntity.ROOT_PAGE_ID, title = "My Space", updatedAt = 1))
        sync()
        phone.page("idea", "una cosa")
        phone.repo.saveBlock(BlockEntity(pageId = PageEntity.ROOT_PAGE_ID, type = BlockType.PAGE_LINK, linkedPageId = "idea", orderIndex = 0))
        pc.page("lista", "un'altra")
        pc.repo.saveBlock(BlockEntity(pageId = PageEntity.ROOT_PAGE_ID, type = BlockType.PAGE_LINK, linkedPageId = "lista", orderIndex = 0))
        pc.db.pageDao().update(pc.db.pageDao().getById(PageEntity.ROOT_PAGE_ID)!!.copy(updatedAt = System.currentTimeMillis() + 60_000))
        val (round, _) = sync()
        assertEquals(1, round.conflicts)
        sync()
        for (device in listOf(phone, pc)) {
            val links = device.db.blockDao().getBlocksForPageOnce(PageEntity.ROOT_PAGE_ID).mapNotNull { it.linkedPageId }
            assertEquals(setOf("idea", "lista"), links.toSet())
            assertEquals("un collegamento solo per pagina", 2, links.size)
        }
    }

    @Test
    fun phoneAndPcEndUpWithTheSamePages() = runBlocking<Unit> {
        phone.page("spesa", "latte", "pane")
        File(phone.files, "foto.img").writeBytes(ByteArray(200_000) { (it % 251).toByte() })
        pc.page("lavoro", "riunione")
        File(pc.files, "nota.m4a").writeBytes(byteArrayOf(1, 2, 3))

        // La prima volta: l'abbinamento, e lo stesso codice sui due schermi.
        val (first, served) = sync()
        assertEquals(1, phone.codes.size)
        assertEquals(phone.codes, pc.codes)
        assertEquals("pc", first.otherName)
        assertNotNull(served)
        assertEquals(listOf("latte", "pane"), pc.texts("spesa"))
        assertEquals(listOf("riunione"), phone.texts("lavoro"))
        assertTrue(File(pc.files, "foto.img").readBytes().contentEquals(File(phone.files, "foto.img").readBytes()))
        assertEquals(3, File(phone.files, "nota.m4a").length())
        assertEquals(SyncDatabase.manifest(phone.db).mapValues { it.value.hash }, SyncDatabase.manifest(pc.db).mapValues { it.value.hash })

        // Una modifica per parte: ognuna arriva dall'altra.
        phone.setText("spesa", 1, "pane integrale")
        pc.setText("lavoro", 0, "riunione alle 10")
        val (second, _) = sync()
        assertEquals(1, phone.codes.size)
        assertEquals(1, second.sent)
        assertEquals(1, second.received)
        assertEquals(listOf("latte", "pane integrale"), pc.texts("spesa"))
        assertEquals(listOf("riunione alle 10"), phone.texts("lavoro"))

        // Tutte e due cambiano la stessa riga: vince l'ultima, e la prima resta nella cronologia.
        phone.setText("spesa", 0, "latte di soia")
        phone.db.pageDao().update(phone.db.pageDao().getById("spesa")!!.copy(updatedAt = 1_000))
        pc.setText("spesa", 0, "latte intero")
        pc.db.pageDao().update(pc.db.pageDao().getById("spesa")!!.copy(updatedAt = System.currentTimeMillis() + 60_000))
        val (third, _) = sync()
        assertEquals(1, third.conflicts)
        assertEquals(listOf("latte intero", "pane integrale"), phone.texts("spesa"))
        assertEquals(listOf("latte intero", "pane integrale"), pc.texts("spesa"))
        val history = phone.db.pageVersionDao().observeForPage("spesa").first()
        assertTrue("la versione del telefono resta nella cronologia", history.any { it.reason == VersionReason.BEFORE_SYNC })

        // Cancellata per sempre sul telefono: sul PC va nel cestino.
        phone.repo.moveToTrash("lavoro")
        phone.repo.deletePermanently("lavoro")
        sync()
        assertNull(phone.db.pageDao().getById("lavoro"))
        assertNotNull(pc.db.pageDao().getById("lavoro")?.trashedAt)

        // Niente di nuovo: niente da mandare, e il cestino del PC non torna sul telefono.
        val (quiet, _) = sync()
        assertEquals(0, quiet.sent)
        assertEquals(0, quiet.received)
        assertNull(phone.db.pageDao().getById("lavoro"))
    }
}
