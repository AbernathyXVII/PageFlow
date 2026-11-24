package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.semantics.getOrNull
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
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Le tabelle semplici** (24/11/2026, chiesto dall'utente): le celle di una riga sono alte uguali anche andando a
 * capo in una; Esc lascia la cella scelta e le frecce si muovono fra le celle come in Excel, Invio torna a
 * scriverci; sul PC il bordo fra due colonne si trascina col mouse.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopSimpleTableTest {
    private val dir: File = Files.createTempDirectory("pageflow-table").toFile()
    private val context = DesktopContext(dir)

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    private class Page(val scene: ImageComposeScene, val frames: (Int) -> Unit, val cells: () -> List<Pair<String, Rect>>)

    /** Una pagina con una tabella [rows]×[cols] coi testi [texts] (riga per riga), aperta sul PC. */
    private fun table(rows: Int, cols: Int, texts: List<String>, body: Page.() -> Unit) {
        AppSettings.init(context)
        val db = AppDatabase.getInstance(context)
        runBlocking {
            val repo = PageRepository(db)
            repo.createPage(PageEntity(id = "tab", title = "T"))
            repo.saveBlock(BlockEntity(id = "t", pageId = "tab", type = BlockType.TABLE, tableRows = rows, tableCols = cols, orderIndex = 0))
            texts.forEachIndexed { i, text -> repo.setTableCellText("t", i / cols, i % cols, text) }
        }
        val factory = ViewModelFactory(context)
        val viewModel = onWindowThread { factory.create(PageEditorViewModel::class, CreationExtras.Empty) }
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 600, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            PageEditorScreen(
                                pageId = "tab", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
                                onNavigateToPage = {}, onNavigateToDatabase = {}, viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
        var time = 0L
        val frames: (Int) -> Unit = { count ->
            repeat(count) {
                onWindowThread { scene.render(time) }
                time += 50_000_000L
                Thread.sleep(20)
            }
        }
        // Le celle, in ordine: i campi di testo sotto il titolo della pagina.
        val cells = {
            onWindowThread {
                scene.semanticsOwners.flatMap { it.getAllSemanticsNodes(mergingEnabled = false) }
                    .mapNotNull { node -> node.config.getOrNull(SemanticsProperties.EditableText)?.text?.let { it to node.boundsInRoot } }
                    .filter { (_, r) -> r.top > 150f && r.height < 200f }
                    .sortedWith(compareBy({ it.second.top.toInt() / 10 }, { it.second.left }))
            }
        }
        try {
            frames(40)
            Page(scene, frames, cells).body()
        } finally {
            onWindowThread { scene.close() }
        }
    }

    private fun Page.click(at: Offset) = runBlocking(Dispatchers.Main) {
        scene.sendPointerEvent(PointerEventType.Move, at, type = PointerType.Mouse)
        scene.sendPointerEvent(PointerEventType.Press, at, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Release, at, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
    }

    private fun Page.key(k: Key) {
        runBlocking(Dispatchers.Main) {
            scene.sendKeyEvent(KeyEvent(k, KeyEventType.KeyDown))
            scene.sendKeyEvent(KeyEvent(k, KeyEventType.KeyUp))
        }
        frames(4)
    }

    private fun Page.shot(name: String) = runBlocking(Dispatchers.Main) { Bitmap.makeFromImage(scene.render(0)) }.also {
        File("build/desktop-screenshots").mkdirs()
        File("build/desktop-screenshots", "table_$name.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(it).encodeToData(EncodedImageFormat.PNG)!!.bytes)
    }

    private fun stored(): Map<Pair<Int, Int>, String> = runBlocking {
        PageRepository(AppDatabase.getInstance(context)).getTableCells("t").first().associate { (it.rowIndex to it.colIndex) to it.text }
    }

    @Test
    fun theCellsOfARowAreAsTallAsTheTallest() = table(1, 3, listOf("uno", "due", "a\nb\nc")) {
        shot("rows")
        val heights = cells().map { it.second.height }
        assertEquals("tre celle: ${cells().map { it.first }}", 3, heights.size)
        assertTrue("alte uguali: $heights", heights.max() - heights.min() < 1f)
        assertTrue("alte quanto tre righe: $heights", heights.first() > cells().first().second.width / 20f)
    }

    @Test
    fun escapeThenTheArrowsMoveLikeExcel() = table(2, 2, listOf("A1", "B1", "A2", "B2")) {
        click(cells()[0].second.center)
        frames(6)
        key(Key.Escape)
        key(Key.DirectionRight)
        key(Key.DirectionDown)
        shot("excel_picked")
        key(Key.Enter)
        runBlocking(Dispatchers.Main) {
            val awt = java.awt.event.KeyEvent(java.awt.Canvas(), java.awt.event.KeyEvent.KEY_TYPED, 0L, 0, java.awt.event.KeyEvent.VK_UNDEFINED, 'x')
            scene.sendKeyEvent(KeyEvent(Key.Unknown, KeyEventType.Unknown, codePoint = 'x'.code, nativeEvent = awt))
        }
        frames(10)
        shot("excel_typed")
        val end = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < end && stored()[1 to 1]?.contains('x') != true) Thread.sleep(50)
        val now = stored()
        assertTrue("la x nella cella in basso a destra: $now", now[1 to 1] == "xB2" || now[1 to 1] == "B2x")
        assertEquals("le altre come prima", listOf("A1", "B1", "A2"), listOf(now[0 to 0], now[0 to 1], now[1 to 0]))
    }

    /** Col mouse: dal bordo destro della colonna [column] (della prima riga), di [dx] punti. */
    private fun Page.dragEdge(column: Int, dx: Float) {
        val box = cells()[column].second
        val from = Offset(box.right + 6f, box.center.y)
        val to = Offset(from.x + dx, from.y)
        runBlocking(Dispatchers.Main) {
            scene.sendPointerEvent(PointerEventType.Move, from, type = PointerType.Mouse)
            scene.sendPointerEvent(PointerEventType.Press, from, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
        }
        frames(2)
        for (step in 1..10) {
            runBlocking(Dispatchers.Main) {
                scene.sendPointerEvent(PointerEventType.Move, Offset(from.x + dx * step / 10f, from.y), type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true))
            }
            frames(1)
        }
        runBlocking(Dispatchers.Main) {
            scene.sendPointerEvent(PointerEventType.Release, to, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
        }
        frames(10)
    }

    private fun savedWidths(): String {
        val end = System.currentTimeMillis() + 5_000
        fun attrs() = runBlocking { AppDatabase.getInstance(context).blockDao().getById("t") }!!.attrsJson
        while (System.currentTimeMillis() < end && "tableColWidthsDp" !in attrs()) Thread.sleep(50)
        return attrs()
    }

    /** Il bordo destro della prima colonna: si allarga solo lei, la seconda resta com'era (la tabella si allunga). */
    @Test
    fun theEdgeOfAColumnIsDraggedWithTheMouse() = table(1, 2, listOf("A1", "B1")) {
        val (first, second) = cells().map { it.second }
        dragEdge(0, 150f)
        shot("edge")
        val firstAfter = cells()[0].second
        assertTrue("la prima colonna si allarga: ${first.width} → ${firstAfter.width}", firstAfter.width > first.width + 100f)
        // La seconda ora esce in parte dalla pagina (la tabella scorre di lato): si guarda quanto è larga davvero.
        val saved = Regex("\"tableColWidthsDp\":\\[([^\\]]*)\\]").find(savedWidths())?.groupValues?.get(1)
            ?.split(",")?.map { it.trim().toFloat() } ?: error("larghezze non salvate: ${savedWidths()}")
        // Le larghezze salvate sono delle celle intere: il campo di testo ha 8 punti di margine per lato.
        assertEquals("la prima salvata più larga di 150", first.width + 16f + 150f, saved[0], 5f)
        assertEquals("la seconda resta larga com'era", second.width + 16f, saved[1], 2f)
        assertEquals("nessuna cella scelta trascinando il bordo", listOf("A1", "B1"), cells().map { it.first })
    }

    /**
     * **Anche l'ultima colonna si stringe** (24/11/2026, chiesto dall'utente: "Perché non posso rimpicciolire anche
     * l'ultima colonna? Vorrei che la tabella sia larga fin quando desidero io, anche se del contenuto va a capo").
     */
    @Test
    fun theLastColumnShrinksToo() = table(1, 2, listOf("A1", "una frase lunga che va a capo")) {
        val (first, second) = cells().map { it.second }
        dragEdge(1, -250f)
        shot("last_edge")
        val (firstAfter, secondAfter) = cells().map { it.second }
        assertTrue("l'ultima colonna si stringe: ${second.width} → ${secondAfter.width}", secondAfter.width < second.width - 200f)
        assertEquals("la prima resta com'era", first.width, firstAfter.width, 2f)
        assertTrue("il testo va a capo: ${second.height} → ${secondAfter.height}", secondAfter.height > second.height + 10f)
        assertTrue("le larghezze restano nel blocco: ${savedWidths()}", "tableColWidthsDp" in savedWidths())
    }
}
