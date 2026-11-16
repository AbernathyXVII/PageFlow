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

/**
 * **Cambiata la grandezza di un titolo, le righe sopra e sotto restano
 * fitte** (16/11/2026, trovato dall'utente sul PC: "quando su PC cambio il
 * font size di un titolo, mi rifà il bug dello spacing tra i blocchi sopra e
 * sotto"). Un campo di testo ha un'altezza di riga sola, quella del testo più
 * grande: il titolo deve uscire dal campo delle righe normali.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopTitleSizeSpacingTest {
    private val dir: File = Files.createTempDirectory("pageflow-titlesize").toFile()
    private val context = DesktopContext(dir)
    private lateinit var db: AppDatabase

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    private val lines = listOf("Uno", "Due", "Titolo", "Tre", "Quattro")

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        db = AppDatabase.getInstance(context)
        val repo = PageRepository(db)
        repo.createPage(PageEntity(id = "titolo", title = "Pagina"))
        lines.forEachIndexed { i, text ->
            repo.saveBlock(BlockEntity(id = "r$i", pageId = "titolo", type = BlockType.PARAGRAPH, textJson = """[{"text":"$text"}]""", orderIndex = i))
        }
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    @Test
    fun aBiggerTitleLeavesTheLinesAroundAsTheyWere() {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 900, density = Density(1f)) {
                CompositionLocalProvider(
                    LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner
                ) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            PageEditorScreen(
                                pageId = "titolo", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
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
            frames(4)
        }
        fun click(at: Offset) {
            onWindowThread {
                scene.sendPointerEvent(PointerEventType.Move, at, type = PointerType.Mouse)
                scene.sendPointerEvent(PointerEventType.Press, at, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
                scene.sendPointerEvent(PointerEventType.Release, at, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
            }
            frames(5)
        }
        fun bright(c: Int) = ((c shr 16) and 0xFF) > 140 && ((c shr 8) and 0xFF) > 140 && (c and 0xFF) > 140
        fun bands(bmp: Bitmap): List<IntRange> {
            val rows = (170 until 800).filter { y -> (60 until 500).any { x -> bright(bmp.getColor(x, y)) } }
            val out = ArrayList<IntRange>()
            for (y in rows) {
                val last = out.lastOrNull()
                if (last != null && y <= last.last + 1) out[out.lastIndex] = last.first..y else out += y..y
            }
            out.removeAll { it.last - it.first < 4 }
            return out
        }
        // Le distanze fra le righe, dalla cima di una alla cima della seguente.
        fun pitches(b: List<IntRange>) = b.zipWithNext { a, c -> c.first - a.first }
        try {
            frames(40)
            val before = bands(shot("titlesize_before"))
            println("TITLESIZE prima $before ${pitches(before)}")
            assertEquals("righe: $before", 5, before.size)
            val pitch = before[1].first - before[0].first
            // Il titolo: clic, Home, Maiusc+Fine (selezionato), poi la grandezza come la finestrella.
            click(Offset(400f, (before[2].first + before[2].last) / 2f))
            key(Key.MoveHome)
            key(Key.MoveEnd, shift = true)
            onWindowThread {
                viewModel.captureSelection()
                assertTrue("titolo selezionato", viewModel.applySizeToCapturedSelection(32))
            }
            // Fotogramma per fotogramma, col cursore ancora nel titolo: le righe sopra e sotto
            // devono restare alla loro distanza (quella di prima, coppia per coppia) da subito.
            val above = before[1].first - before[0].first
            val below = before[4].first - before[3].first
            for (step in 1..30) {
                frames(3)
                val now = bands(shot("titlesize_$step"))
                println("TITLESIZE passo $step (${step * 3 * 30} ms) ${pitches(now)}")
                if (now.size != 5) continue
                assertEquals("passo $step, sopra: ${pitches(now)}", above, now[1].first - now[0].first)
                assertEquals("passo $step, sotto: ${pitches(now)}", below, now[4].first - now[3].first)
            }
            val after = bands(shot("titlesize_after"))
            assertEquals("righe: $after", 5, after.size)
            assertTrue("il titolo è più grande: $after", after[2].last - after[2].first > before[2].last - before[2].first + 5)
            println("TITLESIZE pitch iniziale $pitch")
            // Il cursore è rimasto nel titolo: Fine e una lettera finiscono in fondo a lui.
            key(Key.MoveEnd)
            onWindowThread {
                val awt = java.awt.event.KeyEvent(java.awt.Canvas(), java.awt.event.KeyEvent.KEY_TYPED, 0L, 0, java.awt.event.KeyEvent.VK_UNDEFINED, 'Z')
                scene.sendKeyEvent(KeyEvent(Key.Unknown, KeyEventType.Unknown, codePoint = 'Z'.code, nativeEvent = awt))
            }
            frames(10)
            val texts = runBlocking {
                db.blockDao().getBlocksForPageOnce("titolo").sortedBy { it.orderIndex }.map { b ->
                    Regex("\"text\":\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(b.textJson).joinToString("") { it.groupValues[1] }
                }
            }
            assertEquals("la lettera va in fondo al titolo: $texts", listOf("Uno", "Due", "TitoloZ", "Tre", "Quattro"), texts)
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
