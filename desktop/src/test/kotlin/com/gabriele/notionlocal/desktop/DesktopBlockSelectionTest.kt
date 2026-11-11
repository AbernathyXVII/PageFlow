package com.gabriele.notionlocal.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Più blocchi col mouse sul PC** (dal 10/11/2026, chiesto dall'utente):
 * una pagina vera disegnata, con righe, un titolo più grande e un divisore.
 * Trascinando dalla prima riga a una sotto il titolo si scelgono i blocchi, e
 * Canc li elimina; un clic sul divisore e Backspace eliminano lui. Le righe
 * si trovano guardando dove la foto della pagina ha del testo.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopBlockSelectionTest {
    private val dir: File = Files.createTempDirectory("pageflow-blocks").toFile()
    private val context = DesktopContext(dir)
    private val ids = listOf("uno", "titolo", "due", "divisore", "tre")

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        val repo = PageRepository(AppDatabase.getInstance(context))
        if (AppDatabase.getInstance(context).pageDao().getById("blocchi") == null) repo.createPage(PageEntity(id = "blocchi", title = "Blocchi"))
        repo.saveBlock(BlockEntity(id = "uno", pageId = "blocchi", textJson = """[{"text":"Prima riga"}]""", orderIndex = 0))
        repo.saveBlock(BlockEntity(id = "titolo", pageId = "blocchi", textJson = """[{"text":"Un titolo grande","bold":true,"size":24}]""", orderIndex = 1))
        repo.saveBlock(BlockEntity(id = "due", pageId = "blocchi", textJson = """[{"text":"Seconda riga"}]""", orderIndex = 2))
        repo.saveBlock(BlockEntity(id = "divisore", pageId = "blocchi", type = BlockType.DIVIDER, orderIndex = 3))
        repo.saveBlock(BlockEntity(id = "tre", pageId = "blocchi", textJson = """[{"text":"Terza riga"}]""", orderIndex = 4))
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    private fun remainingIn(pageId: String): List<String> = runBlocking {
        AppDatabase.getInstance(context).blockDao().getBlocksForPageOnce(pageId).map { it.id }
    }

    private fun remaining(): List<String> = runBlocking {
        AppDatabase.getInstance(context).blockDao().getBlocksForPageOnce("blocchi").sortedBy { it.orderIndex }.map { it.id }
    }

    /** La pagina disegnata, con quello che serve per muovercisi: [act] riceve la scena e i suoi giri. */
    private fun withPage(pageId: String = "blocchi", act: (scene: ImageComposeScene, frames: (Int) -> Unit, picture: () -> Bitmap) -> Unit) {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 520, height = 700, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        PageEditorScreen(
                            pageId = pageId, factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
                            onNavigateToPage = {}, onNavigateToDatabase = {}, viewModel = viewModel
                        )
                    }
                }
            }
        }
        var time = 0L
        val frames: (Int) -> Unit = { count ->
            repeat(count) {
                onWindowThread { scene.render(time) }
                time += 100_000_000L
                Thread.sleep(30)
            }
        }
        try {
            frames(40)
            act(scene, frames) { onWindowThread { Bitmap.makeFromImage(scene.render(time)) } }
        } finally {
            onWindowThread { scene.close() }
        }
    }

    private fun waitForBlocks(expected: List<String>) {
        val end = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < end && remaining() != expected) Thread.sleep(50)
        assertEquals(expected, remaining())
    }

    /** Dalla prima riga, oltre il titolo grande, fino alla seconda: tre blocchi scelti, e Canc li elimina. */
    @Test
    fun draggingAcrossBlocksSelectsThemAndDeleteRemovesThem() = withPage { scene, frames, picture ->
        val rows = textRows(picture())
        assertEquals("righe trovate: $rows", 7, rows.size)
        val first = Offset(60f, rows[3].center())
        val second = Offset(60f, rows[5].center())
        onWindowThread {
            mouse(scene, PointerEventType.Move, first, pressed = false)
            mouse(scene, PointerEventType.Press, first, pressed = true)
        }
        frames(3)
        for (step in 1..6) {
            onWindowThread { mouse(scene, PointerEventType.Move, Offset(60f, first.y + (second.y - first.y) * step / 6f), pressed = true) }
            frames(1)
        }
        onWindowThread { mouse(scene, PointerEventType.Release, second, pressed = false) }
        frames(5)
        File("build/desktop-screenshots", "blocks_selected.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(picture()).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        onWindowThread { key(scene, Key.Delete) }
        frames(5)
        waitForBlocks(listOf("divisore", "tre"))
    }

    /** Un clic sul divisore lo sceglie, e Backspace lo elimina; le righe restano. */
    @Test
    fun clickingADividerAndBackspaceRemovesIt() = withPage { scene, frames, picture ->
        val rows = textRows(picture())
        val divider = Offset(60f, (rows[5].last + rows[6].first) / 2f)
        onWindowThread {
            mouse(scene, PointerEventType.Move, divider, pressed = false)
            mouse(scene, PointerEventType.Press, divider, pressed = true)
            mouse(scene, PointerEventType.Release, divider, pressed = false)
        }
        frames(5)
        onWindowThread { key(scene, Key.Backspace) }
        frames(5)
        waitForBlocks(listOf("uno", "titolo", "due", "tre"))
    }

    /**
     * **Ctrl+Z dentro una riga è l'Annulla della pagina** (trovato dall'utente
     * l'11/11/2026): tolto il divisore, col cursore in una riga Ctrl+Z lo
     * rimette (l'annulla del solo campo di testo non lo sapeva fare), e Ctrl+Y
     * lo toglie di nuovo.
     */
    @Test
    fun ctrlZInALineUndoesThePageAndCtrlYRedoes() = withPage { scene, frames, picture ->
        val rows = textRows(picture())
        val divider = Offset(60f, (rows[5].last + rows[6].first) / 2f)
        onWindowThread {
            mouse(scene, PointerEventType.Move, divider, pressed = false)
            mouse(scene, PointerEventType.Press, divider, pressed = true)
            mouse(scene, PointerEventType.Release, divider, pressed = false)
        }
        frames(5)
        onWindowThread { key(scene, Key.Backspace) }
        frames(5)
        waitForBlocks(listOf("uno", "titolo", "due", "tre"))
        val line = Offset(60f, rows[3].center())
        onWindowThread {
            mouse(scene, PointerEventType.Move, line, pressed = false)
            mouse(scene, PointerEventType.Press, line, pressed = true)
            mouse(scene, PointerEventType.Release, line, pressed = false)
        }
        frames(5)
        fun ctrl(key: Key, shift: Boolean = false) = onWindowThread {
            scene.sendKeyEvent(KeyEvent(key, KeyEventType.KeyDown, isCtrlPressed = true, isShiftPressed = shift))
            scene.sendKeyEvent(KeyEvent(key, KeyEventType.KeyUp, isCtrlPressed = true, isShiftPressed = shift))
        }
        ctrl(Key.Z)
        frames(5)
        waitForBlocks(ids)
        ctrl(Key.Y)
        frames(5)
        waitForBlocks(listOf("uno", "titolo", "due", "tre"))
        ctrl(Key.Z, shift = false)
        frames(5)
        waitForBlocks(ids)
    }

    /** Col cursore in una riga, Ctrl+A vela tutta la pagina (non solo il testo della riga), ed Esc la lascia. */
    @Test
    fun ctrlASelectsTheWholePage() = withPage { scene, frames, picture ->
        val rows = textRows(picture())
        val line = Offset(60f, rows[3].center())
        val empty = Offset(450f, rows[6].center())
        val before = picture().getColor(empty.x.toInt(), empty.y.toInt())
        onWindowThread {
            mouse(scene, PointerEventType.Move, line, pressed = false)
            mouse(scene, PointerEventType.Press, line, pressed = true)
            mouse(scene, PointerEventType.Release, line, pressed = false)
        }
        frames(5)
        onWindowThread {
            scene.sendKeyEvent(KeyEvent(Key.A, KeyEventType.KeyDown, isCtrlPressed = true))
            scene.sendKeyEvent(KeyEvent(Key.A, KeyEventType.KeyUp, isCtrlPressed = true))
        }
        frames(5)
        assertNotEquals("Ctrl+A vela anche le righe dove non c'era il cursore", before, picture().getColor(empty.x.toInt(), empty.y.toInt()))
        onWindowThread { key(scene, Key.Escape) }
        frames(5)
        assertEquals(before, picture().getColor(empty.x.toInt(), empty.y.toInt()))
        assertEquals(ids, remaining())
    }

    private fun IntRange.center(): Float = (first + last) / 2f

    /**
     * **La rotella fa scorrere anche mentre si seleziona** (trovato dall'utente
     * il 10/11/2026): col tasto del mouse premuto su una riga, la rotella
     * prima non muoveva la pagina.
     */
    @Test
    fun theWheelScrollsWhileSelecting() {
        runBlocking {
            val repo = PageRepository(AppDatabase.getInstance(context))
            if (AppDatabase.getInstance(context).pageDao().getById("lunga") == null) repo.createPage(PageEntity(id = "lunga", title = "Lunga"))
            for (i in 0 until 60) repo.saveBlock(BlockEntity(id = "lunga-$i", pageId = "lunga", textJson = """[{"text":"Riga numero $i"}]""", orderIndex = i))
        }
        for (acrossBlocks in listOf(false, true)) withPage("lunga") { scene, frames, picture ->
            val before = textRows(picture())
            val from = Offset(30f, before[5].center())
            // Prima si seleziona del testo trascinando nella riga (o, la seconda volta, fino a quella sotto).
            val at = if (acrossBlocks) Offset(80f, before[7].center()) else Offset(90f, before[5].center())
            onWindowThread {
                mouse(scene, PointerEventType.Move, from, pressed = false)
                mouse(scene, PointerEventType.Press, from, pressed = true)
            }
            frames(3)
            for (step in 1..4) {
                onWindowThread { mouse(scene, PointerEventType.Move, Offset(from.x + (at.x - from.x) * step / 4f, from.y + (at.y - from.y) * step / 4f), pressed = true) }
                frames(1)
            }
            onWindowThread {
                scene.sendPointerEvent(
                    PointerEventType.Scroll, at, scrollDelta = Offset(0f, 5f), type = PointerType.Mouse,
                    buttons = PointerButtons(isPrimaryPressed = true)
                )
            }
            frames(5)
            val after = textRows(picture())
            onWindowThread { mouse(scene, PointerEventType.Release, at, pressed = false) }
            frames(3)
            assertNotEquals("la pagina non si è mossa (fra più blocchi: $acrossBlocks)", before.take(8), after.take(8))
        }
    }

    /**
     * **Trascinando la selezione fino al bordo in basso la pagina scorre da
     * sola** (chiesto dall'utente il 10/11/2026), e si passa ai blocchi interi.
     */
    @Test
    fun draggingToTheBottomEdgeScrollsThePage() {
        runBlocking {
            val repo = PageRepository(AppDatabase.getInstance(context))
            if (AppDatabase.getInstance(context).pageDao().getById("lunga") == null) repo.createPage(PageEntity(id = "lunga", title = "Lunga"))
            for (i in 0 until 60) repo.saveBlock(BlockEntity(id = "lunga-$i", pageId = "lunga", textJson = """[{"text":"Riga numero $i"}]""", orderIndex = i))
        }
        withPage("lunga") { scene, frames, picture ->
            val before = textRows(picture())
            val from = Offset(30f, before[5].center())
            val edge = Offset(60f, 690f)
            onWindowThread {
                mouse(scene, PointerEventType.Move, from, pressed = false)
                mouse(scene, PointerEventType.Press, from, pressed = true)
            }
            frames(3)
            for (step in 1..6) {
                onWindowThread { mouse(scene, PointerEventType.Move, Offset(from.x + (edge.x - from.x) * step / 6f, from.y + (edge.y - from.y) * step / 6f), pressed = true) }
                frames(1)
            }
            // Fermi sul bordo: la pagina scorre da sola.
            frames(20)
            val after = textRows(picture())
            onWindowThread { mouse(scene, PointerEventType.Release, edge, pressed = false) }
            frames(3)
            File("build/desktop-screenshots", "edge_scroll.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(picture()).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            assertNotEquals("la pagina non è scesa", before.take(6), after.take(6))
            // La selezione ha seguito la pagina: Canc toglie le righe dalla prima a quella sotto il bordo.
            onWindowThread { key(scene, Key.Delete) }
            frames(5)
            val end = System.currentTimeMillis() + 5_000
            while (System.currentTimeMillis() < end && remainingIn("lunga").size == 60) Thread.sleep(50)
            assertTrue("rimaste ${remainingIn("lunga").size}", remainingIn("lunga").size < 45)
        }
    }

    /** Una pagina con una tabella 2×2: A1 B1 / A2 B2. */
    private fun tablePage(pageId: String, tableId: String) = runBlocking {
        val repo = PageRepository(AppDatabase.getInstance(context))
        if (AppDatabase.getInstance(context).pageDao().getById(pageId) == null) repo.createPage(PageEntity(id = pageId, title = "Tabella"))
        repo.saveBlock(BlockEntity(id = tableId, pageId = pageId, type = BlockType.TABLE, tableRows = 2, tableCols = 2, orderIndex = 0))
        for ((r, row) in listOf(listOf("A1", "B1"), listOf("A2", "B2")).withIndex()) {
            for ((c, text) in row.withIndex()) repo.setTableCellText(tableId, r, c, text)
        }
    }

    private fun cells(tableId: String): Map<Pair<Int, Int>, String> = runBlocking {
        PageRepository(AppDatabase.getInstance(context)).getTableCells(tableId).first().associate { (it.rowIndex to it.colIndex) to it.text }
    }

    /** Trascinando il mouse da una cella a un'altra, dalla prima all'ultima. */
    private fun dragCells(scene: ImageComposeScene, frames: (Int) -> Unit, from: Offset, to: Offset) {
        onWindowThread {
            mouse(scene, PointerEventType.Move, from, pressed = false)
            mouse(scene, PointerEventType.Press, from, pressed = true)
        }
        frames(3)
        for (step in 1..6) {
            onWindowThread { mouse(scene, PointerEventType.Move, Offset(from.x + (to.x - from.x) * step / 6f, from.y + (to.y - from.y) * step / 6f), pressed = true) }
            frames(1)
        }
        onWindowThread { mouse(scene, PointerEventType.Release, to, pressed = false) }
        frames(5)
    }

    /** Due celle della prima riga scelte col mouse: Canc le svuota, il resto della tabella resta. */
    @Test
    fun pickingSomeCellsAndDeleteEmptiesThem() {
        tablePage("tabella1", "tab1")
        withPage("tabella1") { scene, frames, picture ->
            val rows = textRows(picture()).takeLast(2)
            dragCells(scene, frames, Offset(60f, rows[0].center()), Offset(290f, rows[0].center()))
            File("build/desktop-screenshots", "table_cells_picked.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(picture()).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            onWindowThread { key(scene, Key.Delete) }
            frames(5)
            val end = System.currentTimeMillis() + 5_000
            while (System.currentTimeMillis() < end && cells("tab1")[0 to 0] != "") Thread.sleep(50)
            assertEquals(mapOf((0 to 0) to "", (0 to 1) to "", (1 to 0) to "A2", (1 to 1) to "B2"), cells("tab1"))
        }
    }

    /** Tutte le celle scelte: Canc toglie la tabella intera. */
    @Test
    fun pickingAllTheCellsAndDeleteRemovesTheTable() {
        tablePage("tabella2", "tab2")
        withPage("tabella2") { scene, frames, picture ->
            val rows = textRows(picture()).takeLast(2)
            dragCells(scene, frames, Offset(60f, rows[0].center()), Offset(290f, rows[1].center()))
            onWindowThread { key(scene, Key.Delete) }
            frames(5)
            val end = System.currentTimeMillis() + 5_000
            fun tables() = runBlocking { AppDatabase.getInstance(context).blockDao().getBlocksForPageOnce("tabella2").filter { it.type == BlockType.TABLE } }
            while (System.currentTimeMillis() < end && tables().isNotEmpty()) Thread.sleep(50)
            assertEquals(emptyList<BlockEntity>(), tables())
        }
    }

    /** Le fasce di righe dello schermo con del testo chiaro, fra x 20 e 300: dove sono le righe della pagina. */
    private fun textRows(bitmap: Bitmap): List<IntRange> {
        val rows = (0 until bitmap.height).filter { y ->
            (20 until 300).any { x ->
                val c = bitmap.getColor(x, y)
                ((c shr 16) and 0xFF) > 140 && ((c shr 8) and 0xFF) > 140 && (c and 0xFF) > 140
            }
        }
        val bands = ArrayList<IntRange>()
        for (y in rows) {
            val last = bands.lastOrNull()
            if (last != null && y <= last.last + 1) bands[bands.lastIndex] = last.first..y else bands += y..y
        }
        return bands
    }

    private fun mouse(scene: ImageComposeScene, type: PointerEventType, at: Offset, pressed: Boolean) =
        scene.sendPointerEvent(
            type, at,
            type = PointerType.Mouse,
            buttons = if (pressed) PointerButtons(isPrimaryPressed = true) else PointerButtons(),
            button = if (type == PointerEventType.Move) null else PointerButton.Primary
        )

    private fun key(scene: ImageComposeScene, key: Key) {
        scene.sendKeyEvent(KeyEvent(key, KeyEventType.KeyDown))
        scene.sendKeyEvent(KeyEvent(key, KeyEventType.KeyUp))
    }
}
