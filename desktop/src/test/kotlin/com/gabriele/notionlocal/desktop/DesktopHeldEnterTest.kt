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
import com.gabriele.notionlocal.data.entity.heldListType
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
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Invio e Backspace in fondo a un elenco** (17/11/2026, trovati dall'utente): Invio su una riga
 * vuota che tiene il posto di un elenco la fa tornare voce, senza lasciarla invisibile sotto il
 * sottoelenco; e il Backspace sull'ultima di tante voci vuote non fa sparire per un fotogramma il
 * numero di quella sopra ("la lista numerata mi scatta tutta").
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopHeldEnterTest {
    private val dir: File = Files.createTempDirectory("pageflow-heldenter").toFile()
    private val context = DesktopContext(dir)
    private lateinit var db: AppDatabase

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

    private fun describe(): String = runBlocking {
        db.blockDao().getBlocksForPageOnce("prova").sortedBy { it.orderIndex }.joinToString(" | ") { b ->
            val text = Regex("\"text\":\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(b.textJson).joinToString("") { it.groupValues[1] }
            "${b.type.name.take(6)} i${b.indentLevel} held=${b.heldListType?.name?.take(6)} '$text'"
        }
    }

    private inner class Harness(val scene: ImageComposeScene, val name: String) {
        var time = 0L
        fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }
        fun frames(count: Int, sleep: Long = 30) = repeat(count) {
            onWindowThread { scene.render(time) }
            time += 16_000_000L
            Thread.sleep(sleep)
        }
        fun key(k: Key, shift: Boolean = false) = onWindowThread {
            scene.sendKeyEvent(KeyEvent(k, KeyEventType.KeyDown, isShiftPressed = shift))
            scene.sendKeyEvent(KeyEvent(k, KeyEventType.KeyUp, isShiftPressed = shift))
        }
        fun clickBelow() = onWindowThread {
            val at = Offset(300f, 460f)
            scene.sendPointerEvent(PointerEventType.Move, at, type = PointerType.Mouse)
            scene.sendPointerEvent(PointerEventType.Press, at, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
            scene.sendPointerEvent(PointerEventType.Release, at, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
        }
        fun shot(n: String) = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }.also {
            File("build/desktop-screenshots", "heldenter_${name}_$n.png").apply { parentFile.mkdirs() }.writeBytes(org.jetbrains.skia.Image.makeFromBitmap(it).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
    }

    private fun page(name: String, blocks: List<BlockEntity>, body: Harness.() -> Unit) {
        AppSettings.init(context)
        db = AppDatabase.getInstance(context)
        runBlocking {
            val repo = PageRepository(db)
            repo.createPage(PageEntity(id = "prova", title = "P"))
            blocks.forEachIndexed { i, block -> repo.saveBlock(block.copy(pageId = "prova", orderIndex = i)) }
        }
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        val scene = runBlocking(Dispatchers.Main) {
            ImageComposeScene(width = 900, height = 500, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            PageEditorScreen(
                                pageId = "prova", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
                                onNavigateToPage = {}, onNavigateToDatabase = {}, viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
        val h = Harness(scene, name)
        try {
            h.frames(40)
            h.body()
        } finally {
            h.onWindowThread { scene.close() }
            AppDatabase.closeInstance()
        }
    }

    private fun item(id: String, type: BlockType, text: String, indent: Int = 0, attrs: String = "{}") =
        BlockEntity(id = id, pageId = "prova", type = type, indentLevel = indent, textJson = """[{"text":"$text"}]""", attrsJson = attrs)

    private val ab = listOf(item("a", BlockType.BULLET_LIST_ITEM, "A"), item("b", BlockType.BULLET_LIST_ITEM, "B", 1))

    private fun blocks(): List<Triple<BlockType, Int, BlockType?>> = runBlocking {
        db.blockDao().getBlocksForPageOnce("prova").sortedBy { it.orderIndex }.map { Triple(it.type, it.indentLevel, it.heldListType) }
    }

    /** Come l'utente: da B Invio, Backspace (la riga tiene il posto di B), Invio. */
    @Test
    fun enterOnAnEmptyHeldLineMakesItTheItem() = page("flow", ab) {
        clickBelow(); frames(10)
        key(Key.Enter); frames(10)
        key(Key.Backspace); frames(10)
        assertEquals(describe(), Triple(BlockType.PARAGRAPH, 1, BlockType.BULLET_LIST_ITEM), blocks()[2])
        key(Key.Enter); frames(10)
        assertEquals(
            "la riga torna un pallino sotto B, nessuna riga invisibile: ${describe()}",
            listOf(Triple(BlockType.BULLET_LIST_ITEM, 0, null), Triple(BlockType.BULLET_LIST_ITEM, 1, null), Triple(BlockType.BULLET_LIST_ITEM, 1, null)),
            blocks()
        )
        // E si scrive lì, nella voce.
        onWindowThread {
            val awt = java.awt.event.KeyEvent(java.awt.Canvas(), java.awt.event.KeyEvent.KEY_TYPED, 0L, 0, java.awt.event.KeyEvent.VK_UNDEFINED, 'c')
            scene.sendKeyEvent(KeyEvent(Key.Unknown, KeyEventType.Unknown, codePoint = 'c'.code, nativeEvent = awt))
        }
        frames(10)
        assertEquals("BULLET i1 held=null 'c'", describe().split(" | ")[2])
    }

    /** La riga vuota al posto di A: torna un pallino pieno. Una riga con del testo continua l'elenco sotto, come prima. */
    @Test
    fun atTheLevelOfAItBecomesAnItemOfA() = page("heldA", ab + item("c", BlockType.PARAGRAPH, "", 0, """{"listIndent":"BULLET_LIST_ITEM"}""")) {
        clickBelow(); frames(10)
        key(Key.Enter); frames(10)
        assertEquals(describe(), listOf(BlockType.BULLET_LIST_ITEM to 0, BlockType.BULLET_LIST_ITEM to 1, BlockType.BULLET_LIST_ITEM to 0), blocks().map { it.first to it.second })
    }

    /** I numeri come lettere, ogni fotogramma che cambia: stampato e fotografato. */
    private class Frames(val h: Harness, val name: String) {
        val codes = mutableMapOf<String, Char>()
        var last = ""
        var frame = 0
        val seen = mutableListOf<String>()
        fun look() {
            val bitmap = h.onWindowThread { Bitmap.makeFromImage(h.scene.render(h.time)) }
            fun bright(c: Int) = ((c shr 16) and 0xFF) > 120 && ((c shr 8) and 0xFF) > 120 && (c and 0xFF) > 120
            val rows = (170 until 480).map { y -> (40 until 66).map { x -> if (bright(bitmap.getColor(x, y))) '#' else '.' }.joinToString("") }
            val out = StringBuilder()
            var start = -1
            val band = StringBuilder()
            rows.forEachIndexed { i, r ->
                if ('#' in r) { if (start < 0) start = i; band.append(r) }
                else if (start >= 0) {
                    out.append(codes.getOrPut(band.toString()) { 'A' + codes.size }).append(start + 170).append(' ')
                    start = -1; band.clear()
                }
            }
            val now = out.toString()
            if (now != last) {
                println("HELDENTER $name f$frame: $now")
                seen += now
                File("build/desktop-screenshots", "heldenter_${name}_f$frame.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
            last = now
            frame++
            h.time += 16_000_000L
        }
    }

    /** Backspace tenuto premuto sull'ultima di tante voci vuote: ogni fotogramma, i numeri come lettere. */
    @Test
    fun numberedHold() = page("numhold", listOf(item("n0", BlockType.NUMBERED_LIST_ITEM, "Uno")) +
        (1..8).map { item("e$it", BlockType.NUMBERED_LIST_ITEM, "") }
    ) {
        clickBelow(); frames(10)
        val f = Frames(this, "numhold")
        repeat(5) { f.look() }
        repeat(14) {
            key(Key.Backspace)
            repeat(2) { f.look(); Thread.sleep(15) }
        }
        repeat(60) { f.look(); Thread.sleep(10) }
        // I numeri mostrati (le lettere della prima foto) col Backspace possono solo calare.
        val numbers = f.seen.map { frame -> frame.count { it in 'A'..'I' } }
        assertEquals("i numeri ricompaiono: ${f.seen}", numbers.sortedDescending(), numbers)
        assertEquals(4, blocks().count { it.first == BlockType.NUMBERED_LIST_ITEM })
    }

    /** Invio premuti in fretta dopo "1. Uno": ogni fotogramma. */
    @Test
    fun numberedEnterFast() = page("numenter", listOf(item("n0", BlockType.NUMBERED_LIST_ITEM, "Uno"))) {
        clickBelow(); frames(10)
        val f = Frames(this, "numenter")
        repeat(3) { f.look() }
        repeat(8) {
            key(Key.Enter)
            repeat(2) { f.look(); Thread.sleep(15) }
        }
        repeat(60) { f.look(); Thread.sleep(10) }
        // Con Invio possono solo crescere, uno alla volta.
        assertEquals("i numeri scattano: ${f.seen}", (1..9).toList(), f.seen.map { frame -> frame.count { it.isLetter() } })
    }
}
