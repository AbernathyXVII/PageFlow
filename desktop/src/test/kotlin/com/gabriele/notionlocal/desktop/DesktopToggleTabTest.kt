package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
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
 * **Tab e Maiusc+Tab dentro un toggle** (25/11/2026, chiesto dall'utente: "tab dentro i toggle non
 * funziona, né per le indentazioni né per gli spazi predefiniti"): come nelle righe della pagina
 * (`DesktopTabKeyTest`), il Tab è largo fino al prossimo punto di tabulazione e una voce di elenco,
 * col cursore all'inizio, si sposta a destra e torna indietro.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopToggleTabTest {
    private val dir: File = Files.createTempDirectory("pageflow-toggletab").toFile()
    private val context = DesktopContext(dir)
    private lateinit var db: AppDatabase

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        db = AppDatabase.getInstance(context)
        val repo = PageRepository(db)
        repo.createPage(PageEntity(id = "tab", title = "T"))
        repo.saveBlock(BlockEntity(id = "t", pageId = "tab", type = BlockType.TOGGLE, textJson = """[{"text":"Toggle"}]""", isExpanded = true, orderIndex = 0))
        val children = listOf(
            BlockEntity(id = "p1", pageId = "tab", type = BlockType.PARAGRAPH, textJson = """[{"text":"A"}]"""),
            BlockEntity(id = "p2", pageId = "tab", type = BlockType.PARAGRAPH, textJson = """[{"text":"AB"}]"""),
            BlockEntity(id = "b1", pageId = "tab", type = BlockType.BULLET_LIST_ITEM, textJson = """[{"text":"Primo"}]"""),
            BlockEntity(id = "b2", pageId = "tab", type = BlockType.BULLET_LIST_ITEM, textJson = """[{"text":"Secondo"}]""")
        )
        children.forEachIndexed { i, block -> repo.saveBlock(block.copy(parentBlockId = "t", orderIndex = i)) }
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun block(id: String): BlockEntity = runBlocking { db.blockDao().getBlocksForPageOnce("tab").single { it.id == id } }

    private fun text(id: String): String =
        Regex("\"text\":\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(block(id).textJson).joinToString("") { it.groupValues[1] }.replace("\\t", "\t")

    private fun typed(scene: ImageComposeScene, char: Char) {
        val awt = java.awt.event.KeyEvent(java.awt.Canvas(), java.awt.event.KeyEvent.KEY_TYPED, 0L, 0, java.awt.event.KeyEvent.VK_UNDEFINED, char)
        scene.sendKeyEvent(KeyEvent(Key.Unknown, KeyEventType.Unknown, codePoint = char.code, nativeEvent = awt))
    }

    @Test
    fun tabInsideAToggle() {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 700, density = Density(1f)) {
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
        fun frames(count: Int) = repeat(count) {
            onWindowThread { scene.render(time) }
            time += 100_000_000L
            Thread.sleep(30)
        }
        fun shot(name: String): Bitmap = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }.also {
            File("build/desktop-screenshots").mkdirs()
            File("build/desktop-screenshots", "$name.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(it).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        fun key(k: Key, shift: Boolean = false) {
            onWindowThread {
                scene.sendKeyEvent(KeyEvent(k, KeyEventType.KeyDown, isShiftPressed = shift))
                scene.sendKeyEvent(KeyEvent(k, KeyEventType.KeyUp, isShiftPressed = shift))
            }
            frames(6)
        }
        fun type(c: Char) {
            onWindowThread { typed(scene, c) }
            frames(6)
        }
        fun click(at: Offset) {
            onWindowThread {
                scene.sendPointerEvent(PointerEventType.Move, at, type = PointerType.Mouse)
                scene.sendPointerEvent(PointerEventType.Press, at, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
                scene.sendPointerEvent(PointerEventType.Release, at, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
            }
            frames(5)
        }
        fun bright(c: Int) = ((c shr 16) and 0xFF) > 150 && ((c shr 8) and 0xFF) > 150 && (c and 0xFF) > 150
        fun bands(bitmap: Bitmap): List<IntRange> {
            val rows = (60 until bitmap.height).filter { y -> (40 until 800).any { x -> bright(bitmap.getColor(x, y)) } }
            val result = ArrayList<IntRange>()
            for (y in rows) {
                val last = result.lastOrNull()
                if (last != null && y <= last.last + 3) result[result.lastIndex] = last.first..y else result += y..y
            }
            return result
        }
        fun firstBright(bitmap: Bitmap, band: IntRange) = (40 until 800).first { x -> band.any { y -> bright(bitmap.getColor(x, y)) } }
        try {
            frames(40)
            val start = bands(shot("toggle_tab_start"))
            // L'icona della pagina, il titolo, "Toggle", "A", "AB", "Primo", "Secondo".
            assertEquals("righe: $start", 7, start.size)
            fun center(band: IntRange) = (band.first + band.last) / 2f

            click(Offset(400f, center(start[3])))
            key(Key.MoveEnd)
            key(Key.Tab)
            type('X')
            click(Offset(400f, center(start[4])))
            key(Key.MoveEnd)
            key(Key.Tab)
            type('Y')
            frames(10)
            assertEquals("A\tX", text("p1"))
            assertEquals("AB\tY", text("p2"))
            val tabbed = shot("toggle_tab_aligned")
            val lines = bands(tabbed)
            fun afterGap(band: IntRange): Pair<Int, Int> {
                val columns = (40 until 800).filter { x -> band.any { y -> bright(tabbed.getColor(x, y)) } }
                var best = 0 to 0
                for (k in 1 until columns.size) if (columns[k] - columns[k - 1] > best.second) best = columns[k] to columns[k] - columns[k - 1]
                return best
            }
            val (xAt, xGap) = afterGap(lines[3])
            val (yAt, yGap) = afterGap(lines[4])
            assertTrue("X dopo un Tab largo ($xGap) e Y allineata ($xAt, $yAt)", xGap > 20 && yGap > 10 && abs(xAt - yAt) <= 2)

            // Maiusc+Tab col cursore subito dopo il Tab: lo toglie.
            key(Key.DirectionLeft)
            key(Key.Tab, shift = true)
            frames(10)
            assertEquals("ABY", text("p2"))

            // All'inizio di "Secondo": Tab sposta la voce a destra, anche a vedersi; Maiusc+Tab la riporta.
            val before = firstBright(tabbed, lines[6])
            click(Offset(400f, center(lines[6])))
            key(Key.MoveHome)
            key(Key.Tab)
            frames(10)
            assertEquals("voce spostata a destra", 1, block("b2").indentLevel)
            assertEquals("Secondo", text("b2"))
            val moved = shot("toggle_tab_indented")
            val after = firstBright(moved, bands(moved)[6])
            // Quattro spazi, come nelle righe della pagina (`INDENT_SPACES`).
            assertTrue("la voce rientra: da $before a $after", after - before > 8)
            key(Key.Tab, shift = true)
            frames(10)
            assertEquals("voce riportata a sinistra", 0, block("b2").indentLevel)

            // **Un blocco trascinato dentro il toggle** (stesso giorno, chiesto dall'utente): "ABY" preso per i
            // sei puntini e portato sulla metà bassa del titolo "Toggle", col mouse sul testo, va per primo dentro.
            runBlocking { db.blockDao().update(block("t").copy(isExpanded = false)) }
            runBlocking { PageRepository(db).moveBlockNextTo("p2", "t", after = true) }
            frames(20)
            val rows = bands(shot("toggle_drop_start"))
            // L'icona della pagina, il titolo, "Toggle" (chiuso) e "ABY" sotto.
            assertEquals("righe: $rows", 4, rows.size)
            val toggleRow = rows[2]
            val moving = rows[3]
            fun mouse(type: PointerEventType, at: Offset, pressed: Boolean) = scene.sendPointerEvent(
                type, at, type = PointerType.Mouse,
                buttons = if (pressed) PointerButtons(isPrimaryPressed = true) else PointerButtons(),
                button = if (type == PointerEventType.Move) null else PointerButton.Primary
            )
            onWindowThread { mouse(PointerEventType.Move, Offset(200f, center(moving)), pressed = false) }
            frames(3)
            onWindowThread {
                mouse(PointerEventType.Move, Offset(41f, center(moving)), pressed = false)
                mouse(PointerEventType.Press, Offset(41f, center(moving)), pressed = true)
            }
            frames(2)
            val to = toggleRow.last - 1f
            val steps = ((center(moving) - to) / 4f).toInt().coerceAtLeast(1)
            for (i in 1..steps) {
                onWindowThread { mouse(PointerEventType.Move, Offset(200f, center(moving) + (to - center(moving)) * i / steps), pressed = true) }
                frames(1)
            }
            shot("toggle_drop_line")
            onWindowThread { mouse(PointerEventType.Release, Offset(200f, to), pressed = false) }
            frames(15)
            assertEquals("dentro il toggle", "t", block("p2").parentBlockId)
            assertEquals("per primo", 0, block("p2").orderIndex)
            assertTrue("il toggle si è aperto", block("t").isExpanded)
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
