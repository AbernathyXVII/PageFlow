package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.export.ExportPage
import com.gabriele.notionlocal.data.export.exportLines
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.screen.SLASH_ENTRIES
import com.gabriele.notionlocal.ui.screen.SlashAction
import com.gabriele.notionlocal.ui.screen.notePresses
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.math.abs

/**
 * **Le colonne affiancate** (dal 12/11/2026, chieste dall'utente, come su
 * Notion): come nascono dai menu, come si vedono sul PC (una accanto
 * all'altra), e che export e copia non perdono niente di quello che hanno
 * dentro.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopColumnsTest {
    private val dir: File = Files.createTempDirectory("pageflow-columns").toFile()
    private val context = DesktopContext(dir)
    private lateinit var db: AppDatabase
    private lateinit var repo: PageRepository

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        db = AppDatabase.getInstance(context)
        repo = PageRepository(db)
        repo.createPage(PageEntity(id = "colonne", title = "Colonne"))
        repo.saveBlock(BlockEntity(id = "prima", pageId = "colonne", textJson = """[{"text":"PRIMA"}]""", orderIndex = 0))
        repo.saveBlock(BlockEntity(id = "vuota", pageId = "colonne", textJson = """[{"text":""}]""", orderIndex = 1))
        repo.saveBlock(BlockEntity(id = "dopo", pageId = "colonne", textJson = """[{"text":"DOPO"}]""", orderIndex = 2))
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun blocks() = runBlocking { db.blockDao().getBlocksForPageOnce("colonne") }
    private fun childrenOf(id: String?) = blocks().filter { it.parentBlockId == id }.sortedBy { it.orderIndex }

    /** Scrive [text] nella riga della colonna [index] del gruppo [listId]. */
    private fun write(listId: String, index: Int, text: String) = runBlocking {
        val line = childrenOf(childrenOf(listId)[index].id).single()
        repo.saveBlock(line.copy(textJson = """[{"text":"$text"}]"""))
    }

    /** Una riga vuota diventa lei le colonne, ognuna con una riga vuota; il resto della pagina non si muove. */
    @Test
    fun anEmptyLineBecomesTheColumns() = runBlocking {
        val first = repo.makeColumns("vuota", 3)
        assertEquals(listOf("prima", "vuota", "dopo"), childrenOf(null).map { it.id })
        assertEquals(BlockType.COLUMN_LIST, childrenOf(null)[1].type)
        val columns = childrenOf("vuota")
        assertEquals(List(3) { BlockType.COLUMN }, columns.map { it.type })
        columns.forEach { column -> assertEquals(1, childrenOf(column.id).size) }
        assertEquals(childrenOf(columns.first().id).single().id, first)
    }

    /** Da una riga con del testo le colonne vanno subito sotto, e la riga resta com'è. */
    @Test
    fun aLineWithTextKeepsItAndTheColumnsGoBelow() = runBlocking {
        repo.makeColumns("prima", 2)
        val top = childrenOf(null)
        assertEquals(4, top.size)
        assertEquals(listOf("prima", null, "vuota", "dopo"), top.map { it.id.takeUnless { _ -> it.type == BlockType.COLUMN_LIST } })
        assertEquals(BlockType.PARAGRAPH, top[0].type)
        assertEquals(2, childrenOf(top[1].id).size)
        assertEquals(top.map { it.orderIndex }.distinct().size, 4)
    }

    /** Export e copia: le righe delle colonne ci sono tutte, una colonna dopo l'altra, al loro posto. */
    @Test
    fun exportAndDuplicateKeepWhatIsInside() = runBlocking {
        repo.makeColumns("vuota", 2)
        write("vuota", 0, "SINISTRA")
        write("vuota", 1, "DESTRA")
        val page = db.pageDao().getById("colonne")!!
        val lines = exportLines(ExportPage(page, blocks(), emptyList())).map { line -> line.spans.joinToString("") { it.text } }
        assertEquals(listOf("PRIMA", "SINISTRA", "DESTRA", "DOPO"), lines)
        repo.duplicateBlock("vuota") { it }
        val all = blocks()
        assertEquals(2, all.count { it.type == BlockType.COLUMN_LIST })
        assertEquals(4, all.count { it.type == BlockType.COLUMN })
        assertEquals(2, all.count { it.textJson.contains("SINISTRA") })
    }

    /**
     * **Copiare (o tagliare) le colonne scelte col mouse** porta con sé le loro
     * righe, una colonna dopo l'altra e senza rientro: col taglio non si perde niente.
     */
    @Test
    fun copyingTheColumnsTakesTheirLines() {
        runBlocking {
            repo.makeColumns("vuota", 2)
            write("vuota", 0, "SINISTRA")
            write("vuota", 1, "DESTRA")
        }
        val viewModel = PageEditorViewModel(repo)
        viewModel.load("colonne")
        val end = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < end && viewModel.blocks.value.none { it.type == BlockType.COLUMN_LIST }) Thread.sleep(20)
        val lines = runBlocking { viewModel.pageCopyLines(setOf("vuota")) }
        assertEquals(listOf("SINISTRA", "DESTRA"), lines.map { line -> line.spans.joinToString("") { it.text } })
        assertEquals(listOf(0, 0), lines.map { it.kind?.indent })
        viewModel.viewModelScope.cancel()
    }

    /** La voce dei menu (come la fa la pagina): le colonne nascono, e il cursore va nella prima. */
    @Test
    fun theMenuEntryMakesTheColumnsAndPutsTheCursorInTheFirst() {
        val viewModel = PageEditorViewModel(repo)
        viewModel.load("colonne")
        fun waitFor(condition: () -> Boolean) {
            val end = System.currentTimeMillis() + 10_000
            while (System.currentTimeMillis() < end && !condition()) Thread.sleep(20)
            assertTrue(condition())
        }
        waitFor { viewModel.blocks.value.size == 3 }
        viewModel.makeColumns(viewModel.blocks.value.single { it.id == "vuota" }, 4)
        waitFor { viewModel.blocks.value.count { it.type == BlockType.COLUMN } == 4 }
        val first = childrenOf(childrenOf("vuota").first().id).single().id
        waitFor { viewModel.focusRequestBlockId.value == first }
        viewModel.viewModelScope.cancel()
    }

    /** La riga della colonna [index] del gruppo [listId] (la prima, se ce n'è più d'una). */
    private fun lineOf(listId: String, index: Int) = childrenOf(childrenOf(listId)[index].id).first()

    /** Gli ordini dei fratelli sono 0, 1, 2… senza buchi né doppioni, a ogni livello. */
    private fun assertTidy() {
        blocks().groupBy { it.parentBlockId }.forEach { (parent, list) ->
            assertEquals("ordine sotto $parent", list.indices.toList(), list.map { it.orderIndex }.sorted())
        }
    }

    /**
     * **Un blocco portato sul bordo destro di un altro fa le colonne** (chiesto
     * dall'utente il 12/11/2026, come su Notion): "DOPO" accanto a "PRIMA", e
     * il gruppo prende il posto di "PRIMA".
     */
    @Test
    fun besideALineMakesTwoColumns() = runBlocking {
        assertTrue(repo.moveBlockBeside("dopo", "prima", right = true))
        val top = childrenOf(null)
        assertEquals(listOf(BlockType.COLUMN_LIST, BlockType.PARAGRAPH), top.map { it.type })
        assertEquals("vuota", top[1].id)
        val columns = childrenOf(top[0].id)
        assertEquals(listOf("prima", "dopo"), columns.map { childrenOf(it.id).single().id })
        assertTidy()
        // A sinistra: lui prima.
        assertTrue(repo.moveBlockBeside("vuota", "prima", right = false))
        assertEquals(listOf("vuota", "prima", "dopo"), childrenOf(top[0].id).map { childrenOf(it.id).single().id })
        assertTidy()
    }

    /** Accanto a una riga dentro una colonna: una colonna in più, proprio lì. */
    @Test
    fun besideALineInAColumnAddsAColumn() = runBlocking {
        repo.makeColumns("vuota", 2)
        write("vuota", 0, "SINISTRA")
        write("vuota", 1, "DESTRA")
        assertTrue(repo.moveBlockBeside("prima", lineOf("vuota", 0).id, right = true))
        assertEquals(listOf("vuota", "dopo"), childrenOf(null).map { it.id })
        val texts = childrenOf("vuota").map { column -> childrenOf(column.id).single().id }
        assertEquals(3, texts.size)
        assertEquals("prima", texts[1])
        assertTidy()
    }

    /** Accanto al gruppo intero: una colonna in più in cima o in fondo. */
    @Test
    fun besideTheGroupAddsAColumnAtItsEdge() = runBlocking {
        repo.makeColumns("vuota", 2)
        assertTrue(repo.moveBlockBeside("prima", "vuota", right = false))
        assertEquals("prima", childrenOf(childrenOf("vuota").first().id).single().id)
        assertTrue(repo.moveBlockBeside("dopo", "vuota", right = true))
        assertEquals("dopo", childrenOf(childrenOf("vuota").last().id).single().id)
        assertEquals(4, childrenOf("vuota").size)
        assertEquals(listOf("vuota"), childrenOf(null).map { it.id })
        assertTidy()
    }

    /** Dentro una colonna, sopra la sua prima riga. */
    @Test
    fun intoAColumnAboveItsFirstLine() = runBlocking {
        repo.makeColumns("vuota", 2)
        write("vuota", 0, "SINISTRA")
        val sinistra = lineOf("vuota", 0).id
        assertTrue(repo.moveBlockNextTo("prima", sinistra, after = false))
        assertEquals(listOf("prima", sinistra), childrenOf(childrenOf("vuota")[0].id).map { it.id })
        assertEquals(listOf("vuota", "dopo"), childrenOf(null).map { it.id })
        assertTidy()
    }

    /**
     * **L'ultima riga che esce da una colonna**: la colonna se ne va, e il
     * gruppo rimasto con una colonna sola torna righe normali, al suo posto.
     */
    @Test
    fun theLastLineOutRemovesTheColumnAndUngroups() = runBlocking {
        repo.makeColumns("vuota", 2)
        write("vuota", 0, "SINISTRA")
        write("vuota", 1, "DESTRA")
        val sinistra = lineOf("vuota", 0).id
        val destra = lineOf("vuota", 1).id
        assertTrue(repo.moveBlockNextTo(destra, "dopo", after = true))
        assertEquals(listOf("prima", sinistra, "dopo", destra), childrenOf(null).map { it.id })
        assertTrue(blocks().none { it.type == BlockType.COLUMN || it.type == BlockType.COLUMN_LIST })
        assertTidy()
    }

    /**
     * **Al massimo quattro colonne** (chiesto dall'utente il 12/11/2026: "ho
     * già creato nove colonne solo spostando i blocchi"): accanto a un gruppo
     * di quattro non nasce la quinta; con tre sì. Spostare la riga sola di una
     * colonna accanto a un'altra dello stesso gruppo resta permesso: la sua
     * colonna se ne va, e le colonne restano quattro.
     */
    @Test
    fun noMoreThanFourColumns() = runBlocking {
        repo.makeColumns("vuota", 4)
        (0 until 4).forEach { write("vuota", it, "C$it") }
        assertEquals(false, repo.moveBlockBeside("prima", lineOf("vuota", 0).id, right = true))
        assertEquals(false, repo.moveBlockBeside("dopo", "vuota", right = true))
        assertEquals(listOf("prima", "vuota", "dopo"), childrenOf(null).map { it.id })
        assertEquals(4, childrenOf("vuota").size)
        val first = lineOf("vuota", 0).id
        assertTrue(repo.moveBlockBeside(first, lineOf("vuota", 2).id, right = true))
        assertEquals(4, childrenOf("vuota").size)
        assertEquals(first, childrenOf(childrenOf("vuota")[2].id).single().id)
        assertTidy()
        // Con tre colonne la quarta nasce.
        assertTrue(repo.moveBlockNextTo(lineOf("vuota", 3).id, "dopo", after = true))
        assertEquals(3, childrenOf("vuota").size)
        assertTrue(repo.moveBlockBeside("prima", lineOf("vuota", 0).id, right = false))
        assertEquals(4, childrenOf("vuota").size)
        assertTidy()
    }

    /** Niente colonne dentro le colonne, e niente blocco dentro se stesso. */
    @Test
    fun columnsDontGoIntoColumns() = runBlocking {
        repo.makeColumns("vuota", 2)
        val line = lineOf("vuota", 0).id
        assertEquals(false, repo.moveBlockNextTo("vuota", line, after = true))
        assertEquals(false, repo.moveBlockBeside("vuota", "prima", right = true))
        assertEquals(false, repo.moveBlockBeside("prima", "prima", right = true))
        assertEquals(listOf("prima", "vuota", "dopo"), childrenOf(null).map { it.id })
        assertTidy()
    }

    /** "Create two/three/four columns" ci sono, nei menu "/" e "+" del PC. */
    @Test
    fun theMenusHaveTheThreeEntries() {
        assertEquals(listOf(2, 3, 4), SLASH_ENTRIES.mapNotNull { (it.action as? SlashAction.Columns)?.count })
        assertEquals(
            listOf("Create two columns", "Create three columns", "Create four columns"),
            SLASH_ENTRIES.filter { it.action is SlashAction.Columns }.map { it.label }
        )
    }

    /**
     * **Sul PC una accanto all'altra**: la pagina disegnata, con "SINISTRA"
     * nella prima colonna e "DESTRA" nella seconda, le ha sulla stessa riga,
     * una nella metà sinistra e una in quella destra; "PRIMA" sopra e "DOPO"
     * sotto.
     */
    @Test
    fun onThePcTheColumnsSitSideBySide() {
        runBlocking {
            repo.makeColumns("vuota", 2)
            write("vuota", 0, "SINISTRA")
            write("vuota", 1, "DESTRA")
        }
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 600, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize().notePresses()) {
                            PageEditorScreen(
                                pageId = "colonne", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
                                onNavigateToPage = {}, onNavigateToDatabase = {}, viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
        try {
            var time = 0L
            repeat(40) {
                onWindowThread { scene.render(time) }
                time += 100_000_000L
                Thread.sleep(30)
            }
            val shot = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }
            File("build/desktop-screenshots").mkdirs()
            File("build/desktop-screenshots", "columns_pc.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(shot).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            fun bright(x: Int, y: Int) = shot.getColor(x, y).let { c -> ((c shr 16) and 0xFF) > 140 && ((c shr 8) and 0xFF) > 140 && (c and 0xFF) > 140 }
            val mid = shot.width / 2
            // Sotto la barra in alto: le altezze con del testo nella metà sinistra e in quella destra.
            val left = (60 until shot.height).filter { y -> (80 until mid - 20).any { x -> bright(x, y) } }.toSet()
            val right = (60 until shot.height).filter { y -> (mid + 20 until shot.width - 20).any { x -> bright(x, y) } }
            assertTrue("niente testo nella metà destra", right.isNotEmpty())
            assertTrue("le due colonne non sono sulla stessa riga", right.all { it in left })
            // "PRIMA" sopra le colonne e "DOPO" sotto: righe solo a sinistra, prima e dopo.
            assertTrue("niente sopra le colonne", left.any { it < right.min() - 4 })
            assertTrue("niente sotto le colonne", left.any { it > right.max() + 4 })

            // **I sei puntini a metà delle lettere della prima riga** (trovato
            // dall'utente il 12/11/2026: "i sei puntini diventano storti"),
            // col mouse sulla riga di destra come su quella di sinistra.
            val letters = right.min()..right.max()
            for (x in listOf(200f, mid + 60f)) {
                onWindowThread {
                    scene.sendPointerEvent(
                        androidx.compose.ui.input.pointer.PointerEventType.Move,
                        androidx.compose.ui.geometry.Offset(x, (letters.first + letters.last) / 2f),
                        type = androidx.compose.ui.input.pointer.PointerType.Mouse
                    )
                }
                repeat(5) {
                    onWindowThread { scene.render(time) }
                    time += 100_000_000L
                    Thread.sleep(30)
                }
                val hovered = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }
                File("build/desktop-screenshots", "columns_dots.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(hovered).encodeToData(EncodedImageFormat.PNG)!!.bytes)
                // I puntini della riga sotto il mouse, subito prima del suo testo (dal 12/11/2026 anche
                // nella colonna di destra: prima c'erano solo quelli del gruppo, nel margine).
                val textStart = (if (x < mid) 0 until mid else mid until shot.width).first { cx -> (letters.first..letters.last).any { cy -> bright(cx, cy) } }
                val dots = (0 until hovered.height).filter { y -> (textStart - 28 until textStart - 3).any { dx -> hovered.getColor(dx, y) != shot.getColor(dx, y) } }
                val center = (letters.first + letters.last) / 2f
                assertTrue("puntini $dots, lettere $letters (mouse a x $x)", dots.isNotEmpty() && abs(dots.average() - center) <= 1.5)
            }
        } finally {
            onWindowThread { scene.close() }
        }
    }

    /** Un tasto battuto, come lo manda la finestra di Windows (un carattere, non un comando). */
    private fun typed(scene: ImageComposeScene, char: Char) {
        val awt = java.awt.event.KeyEvent(java.awt.Canvas(), java.awt.event.KeyEvent.KEY_TYPED, 0L, 0, java.awt.event.KeyEvent.VK_UNDEFINED, char)
        scene.sendKeyEvent(androidx.compose.ui.input.key.KeyEvent(androidx.compose.ui.input.key.Key.Unknown, androidx.compose.ui.input.key.KeyEventType.Unknown, codePoint = char.code, nativeEvent = awt))
    }

    /**
     * **Tutto il giro col mouse e la tastiera** (l'utente il 12/11/2026: "le
     * due colonne non si creano"): clic sulla riga vuota, "/two", clic sulla
     * voce; poi si scrive nella prima colonna, Invio, Invio.
     */
    @Test
    fun fromTheSlashMenuToTwoEntersInAColumn() = slashRound("click")

    /**
     * **Invio sceglie la voce del menu "/"** (l'utente il 12/11/2026, dopo la
     * v0.153.69.1: "continua a non creare le colonne"): come su Notion,
     * "/two" e Invio fanno le colonne, invece di andare a capo nel testo.
     */
    @Test
    fun enterChoosesTheSlashEntry() = slashRound("enter")

    /** Le frecce cambiano la voce evidenziata del menu "/": "/", giù, giù e Invio fa un elenco numerato. */
    @Test
    fun theArrowsMoveThroughTheSlashMenu() = slashRound("arrows")

    /** Il nome scritto per intero, con gli spazi: il menu resta aperto e Invio fa le colonne. */
    @Test
    fun theWholeNameWithSpacesAndEnterMakesTheColumns() = slashRound("spaces")

    /** Una barra in mezzo a una parola ("e/o") non apre il menu: Invio va solo a capo. */
    @Test
    fun aSlashInsideAWordIsJustText() = slashRound("inword")

    private fun slashRound(mode: String) {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 600, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize().notePresses()) {
                            PageEditorScreen(
                                pageId = "colonne", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
                                onNavigateToPage = {}, onNavigateToDatabase = {}, viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
        var time = 0L
        fun frames(count: Int) = repeat(count) {
            onWindowThread { scene.render(time) }
            time += 100_000_000L
            Thread.sleep(30)
        }
        fun shot(name: String): Bitmap = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }.also {
            File("build/desktop-screenshots").mkdirs()
            File("build/desktop-screenshots", "$name.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(it).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        fun click(x: Float, y: Float) = onWindowThread {
            val at = androidx.compose.ui.geometry.Offset(x, y)
            val mouse = androidx.compose.ui.input.pointer.PointerType.Mouse
            scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Move, at, type = mouse)
            scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Press, at, type = mouse,
                buttons = androidx.compose.ui.input.pointer.PointerButtons(isPrimaryPressed = true), button = androidx.compose.ui.input.pointer.PointerButton.Primary)
            scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Release, at, type = mouse,
                buttons = androidx.compose.ui.input.pointer.PointerButtons(), button = androidx.compose.ui.input.pointer.PointerButton.Primary)
        }
        fun type(text: String) {
            text.forEach { c -> onWindowThread { typed(scene, c) }; frames(1) }
        }
        fun key(key: androidx.compose.ui.input.key.Key) {
            onWindowThread {
                scene.sendKeyEvent(androidx.compose.ui.input.key.KeyEvent(key, androidx.compose.ui.input.key.KeyEventType.KeyDown))
                scene.sendKeyEvent(androidx.compose.ui.input.key.KeyEvent(key, androidx.compose.ui.input.key.KeyEventType.KeyUp))
            }
            frames(3)
        }
        try {
            frames(40)
            // La riga vuota sta fra "PRIMA" (y ~190) e "DOPO" (y ~236).
            click(100f, 213f)
            frames(5)
            if (mode == "arrows") {
                type("/")
                key(androidx.compose.ui.input.key.Key.DirectionDown)
                key(androidx.compose.ui.input.key.Key.DirectionDown)
                key(androidx.compose.ui.input.key.Key.Enter)
                frames(10)
                assertEquals(BlockType.NUMBERED_LIST_ITEM, childrenOf(null).single { it.id == "vuota" }.type)
                assertEquals(listOf("prima", "vuota", "dopo"), childrenOf(null).map { it.id })
                return
            }
            if (mode == "inword") {
                type("e/o")
                key(androidx.compose.ui.input.key.Key.Enter)
                frames(10)
                val top = childrenOf(null)
                assertTrue(top.none { it.type == BlockType.COLUMN_LIST })
                assertEquals(4, top.size)
                assertEquals("""[{"text":"e/o"}]""", top.single { it.id == "vuota" }.textJson)
                return
            }
            type(if (mode == "spaces") "/create two columns" else "/two")
            frames(5)
            // Nel menu c'è solo "Create two columns": la sua riga sta sotto la pagina, a sinistra.
            val menu = shot("columns_slash")
            fun bright(x: Int, y: Int) = menu.getColor(x, y).let { c -> ((c shr 16) and 0xFF) > 140 && ((c shr 8) and 0xFF) > 140 && (c and 0xFF) > 140 }
            val entry = (245 until 400).first { y -> (130 until 300).any { x -> bright(x, y) } }
            if (mode == "enter" || mode == "spaces") {
                onWindowThread {
                    scene.sendKeyEvent(androidx.compose.ui.input.key.KeyEvent(androidx.compose.ui.input.key.Key.Enter, androidx.compose.ui.input.key.KeyEventType.KeyDown))
                    scene.sendKeyEvent(androidx.compose.ui.input.key.KeyEvent(androidx.compose.ui.input.key.Key.Enter, androidx.compose.ui.input.key.KeyEventType.KeyUp))
                }
            } else {
                click(200f, entry + 6f)
            }
            frames(20)
            shot("columns_made")
            val group = childrenOf(null).single { it.type == BlockType.COLUMN_LIST }
            val columns = childrenOf(group.id)
            assertEquals(2, columns.size)
            assertEquals(listOf("prima", group.id, "dopo"), childrenOf(null).map { it.id })
            // Si scrive nella prima colonna, poi Invio due volte.
            type("abc")
            frames(5)
            fun enter() {
                onWindowThread {
                    scene.sendKeyEvent(androidx.compose.ui.input.key.KeyEvent(androidx.compose.ui.input.key.Key.Enter, androidx.compose.ui.input.key.KeyEventType.KeyDown))
                    scene.sendKeyEvent(androidx.compose.ui.input.key.KeyEvent(androidx.compose.ui.input.key.Key.Enter, androidx.compose.ui.input.key.KeyEventType.KeyUp))
                }
                frames(10)
            }
            enter()
            enter()
            type("d")
            frames(10)
            shot("columns_enters")
            val texts = childrenOf(columns[0].id).map { it.textJson }
            assertEquals(listOf("""[{"text":"abc"}]""", """[{"text":""}]""", """[{"text":"d"}]"""), texts)
            assertEquals(1, childrenOf(columns[1].id).size)
            // E sullo schermo: "PRIMA" una volta sopra, "DOPO" una volta sotto, le righe della
            // colonna fra loro (prima si vedevano anche le righe di prima, "DOPO" due volte).
            val screen = shot("columns_enters")
            fun brightAt(x: Int, y: Int) = screen.getColor(x, y).let { c -> ((c shr 16) and 0xFF) > 140 && ((c shr 8) and 0xFF) > 140 && (c and 0xFF) > 140 }
            val rows = (170 until screen.height).filter { y -> (86 until 300).any { x -> brightAt(x, y) } }
            val bands = ArrayList<IntRange>()
            for (y in rows) {
                val last = bands.lastOrNull()
                if (last != null && y <= last.last + 1) bands[bands.lastIndex] = last.first..y else bands += y..y
            }
            assertEquals("righe di testo sullo schermo: $bands", 4, bands.size)
            // **Un clic nella seconda colonna**, all'altezza della prima riga, e si scrive lì.
            val abc = bands[1]
            click(screen.width / 2f + 100f, (abc.first + abc.last) / 2f)
            frames(10)
            type("xyz")
            frames(10)
            shot("columns_second")
            assertEquals(listOf("""[{"text":"xyz"}]"""), childrenOf(columns[1].id).map { it.textJson })
        } finally {
            onWindowThread { scene.close() }
        }
    }

    /** La pagina "colonne" disegnata sul PC, larga [width]: la sua foto. */
    private fun render(width: Int = 900): Bitmap {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }
        val scene = onWindowThread {
            ImageComposeScene(width = width, height = 500, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        PageEditorScreen(
                            pageId = "colonne", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
                            onNavigateToPage = {}, onNavigateToDatabase = {}, viewModel = viewModel
                        )
                    }
                }
            }
        }
        try {
            var time = 0L
            repeat(40) {
                onWindowThread { scene.render(time) }
                time += 100_000_000L
                Thread.sleep(30)
            }
            return onWindowThread { Bitmap.makeFromImage(scene.render(time)) }
        } finally {
            onWindowThread { scene.close() }
        }
    }

    /**
     * **"Create three columns" fa tre blocchi che si vedono** (chiesto
     * dall'utente il 12/11/2026, come su Notion: "un blocco a sinistra, uno al
     * centro e uno a destra"): tre riquadri chiari sulla stessa riga, e
     * scritto qualcosa in una colonna il suo riquadro se ne va.
     */
    @Test
    fun threeNewColumnsAreThreeVisibleBlocks() {
        runBlocking { repo.makeColumns("vuota", 3) }
        val shot = render(1200)
        File("build/desktop-screenshots").mkdirs()
        File("build/desktop-screenshots", "columns_three.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(shot).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        val background = shot.getColor(shot.width - 5, shot.height - 5)
        // La riga a metà dei riquadri: dove, fra "PRIMA" e "DOPO", il fondo cambia di più.
        val rows = (150 until 300).filter { y -> (0 until shot.width).count { x -> shot.getColor(x, y) != background } > shot.width / 2 }
        assertTrue("nessuna riga coi riquadri", rows.isNotEmpty())
        val y = rows[rows.size / 2]
        val runs = ArrayList<IntRange>()
        for (x in 0 until shot.width) {
            if (shot.getColor(x, y) == background) continue
            val last = runs.lastOrNull()
            if (last != null && x <= last.last + 1) runs[runs.lastIndex] = last.first..x else runs += x..x
        }
        val boxes = runs.filter { it.last - it.first > 100 }
        assertEquals("riquadri a y $y: $runs", 3, boxes.size)
        val centers = boxes.map { (it.first + it.last) / 2 }
        assertTrue("uno a sinistra, uno al centro, uno a destra: $boxes", centers[0] < shot.width / 3 && centers[1] in shot.width / 3..shot.width * 2 / 3 && centers[2] > shot.width * 2 / 3)

        write("vuota", 1, "CENTRO")
        val after = render(1200)
        val stillBoxes = ArrayList<IntRange>()
        for (x in 0 until after.width) {
            if (after.getColor(x, y) == background) continue
            val last = stillBoxes.lastOrNull()
            if (last != null && x <= last.last + 1) stillBoxes[stillBoxes.lastIndex] = last.first..x else stillBoxes += x..x
        }
        assertEquals("con la colonna di mezzo scritta restano due riquadri: $stillBoxes", 2, stillBoxes.count { it.last - it.first > 100 })
    }

    /** La pagina "colonne" disegnata e da usare col mouse: [act] riceve chi muove il mouse e chi fa le foto. */
    private fun withMouse(act: (mouse: (String, Float, Float) -> Unit, shot: (String) -> Bitmap) -> Unit) {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 600, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize().notePresses()) {
                            PageEditorScreen(
                                pageId = "colonne", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
                                onNavigateToPage = {}, onNavigateToDatabase = {}, viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
        var time = 0L
        fun frames(count: Int) = repeat(count) {
            onWindowThread { scene.render(time) }
            time += 100_000_000L
            Thread.sleep(30)
        }
        val mouse: (String, Float, Float) -> Unit = { what, x, y ->
            val at = androidx.compose.ui.geometry.Offset(x, y)
            val type = androidx.compose.ui.input.pointer.PointerType.Mouse
            onWindowThread {
                when (what) {
                    "move" -> scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Move, at, type = type)
                    "drag" -> scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Move, at, type = type,
                        buttons = androidx.compose.ui.input.pointer.PointerButtons(isPrimaryPressed = true))
                    "press" -> scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Press, at, type = type,
                        buttons = androidx.compose.ui.input.pointer.PointerButtons(isPrimaryPressed = true), button = androidx.compose.ui.input.pointer.PointerButton.Primary)
                    else -> scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Release, at, type = type,
                        buttons = androidx.compose.ui.input.pointer.PointerButtons(), button = androidx.compose.ui.input.pointer.PointerButton.Primary)
                }
            }
            frames(1)
        }
        val shot: (String) -> Bitmap = { name ->
            onWindowThread { Bitmap.makeFromImage(scene.render(time)) }.also {
                File("build/desktop-screenshots").mkdirs()
                File("build/desktop-screenshots", "$name.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(it).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
        }
        try {
            frames(40)
            act(mouse, shot)
            frames(10)
        } finally {
            onWindowThread { scene.close() }
        }
    }

    /** Trascina coi puntini (che stanno a [dotsX]) la riga all'altezza [fromY] fino a ([toX], [toY]), a piccoli passi; la foto prima di lasciare. */
    private fun dragLine(mouse: (String, Float, Float) -> Unit, shot: (String) -> Bitmap, textX: Float, dotsX: Float, fromY: Float, toX: Float, toY: Float, name: String): Bitmap {
        mouse("move", textX, fromY)
        mouse("move", dotsX, fromY)
        mouse("press", dotsX, fromY)
        val steps = 30
        for (i in 1..steps) mouse("drag", dotsX + (toX - dotsX) * i / steps, fromY + (toY - fromY) * i / steps)
        val before = shot(name)
        mouse("release", toX, toY)
        return before
    }

    /** I punti blu della linea di dove cadrà il blocco: quanti sono, e da sinistra a destra dove. */
    private fun blueOf(bitmap: Bitmap): List<Int> = (0 until bitmap.height).flatMap { y ->
        (0 until bitmap.width).filter { x -> bitmap.getColor(x, y).let { c -> ((c shr 16) and 0xFF) < 80 && (c and 0xFF) > 180 && ((c shr 8) and 0xFF) in 100..160 } }
    }

    /**
     * **Trascinando "DOPO" sul bordo destro di "PRIMA" nascono le colonne**
     * (chiesto dall'utente il 12/11/2026, come nelle foto di Notion): mentre
     * si trascina c'è una linea blu verticale a destra, e lasciando "PRIMA" e
     * "DOPO" stanno una accanto all'altra.
     */
    @Test
    fun draggingOntoTheRightEdgeMakesColumns() {
        runBlocking { repo.saveBlock(childrenOf(null).single { it.id == "vuota" }.copy(textJson = """[{"text":"MEZZO"}]""")) }
        withMouse { mouse, shot ->
            // Le righe: PRIMA a y ~190, MEZZO ~213, DOPO ~236 (il testo comincia a x 86).
            val during = dragLine(mouse, shot, textX = 120f, dotsX = 74f, fromY = 236f, toX = 800f, toY = 190f, name = "columns_drag_right")
            val blue = blueOf(during)
            assertTrue("la linea blu verticale a destra: ${blue.size} punti", blue.size > 20 && blue.average() > 700)
        }
        val top = childrenOf(null)
        assertEquals(listOf(BlockType.COLUMN_LIST, BlockType.PARAGRAPH), top.map { it.type })
        assertEquals(listOf("prima", "dopo"), childrenOf(top[0].id).map { childrenOf(it.id).single().id })
        assertTidy()
    }

    /**
     * **La riga di una colonna trascinata fuori, sotto "DOPO"**: coi suoi
     * puntini, accanto al suo testo; la colonna rimasta vuota se ne va e il
     * gruppo, con una colonna sola, torna righe normali.
     */
    @Test
    fun draggingTheLastLineOfAColumnOutUngroups() {
        runBlocking {
            repo.makeColumns("vuota", 2)
            write("vuota", 0, "SINISTRA")
            write("vuota", 1, "DESTRA")
        }
        val sinistra = lineOf("vuota", 0).id
        val destra = lineOf("vuota", 1).id
        withMouse { mouse, shot ->
            // "DESTRA" comincia a x 470 (metà della pagina più il margine): i suoi puntini poco prima.
            dragLine(mouse, shot, textX = 500f, dotsX = 456f, fromY = 213f, toX = 200f, toY = 242f, name = "columns_drag_out")
        }
        val top = childrenOf(null)
        assertEquals(listOf("prima", sinistra, "dopo", destra), top.map { it.id })
        assertTrue(blocks().none { it.type == BlockType.COLUMN || it.type == BlockType.COLUMN_LIST })
        assertTidy()
    }
}
