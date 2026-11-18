package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import com.gabriele.notionlocal.ui.screen.autoscrollSpeed
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Il tasto centrale del mouse, come nei browser** (18/11/2026, chiesto dall'utente: "fa apparire
 * l'icona che mi permette di spostarmi nella pagina tanto veloce quanto abbasso o alzo la puntina").
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopAutoscrollTest {
    private val dir: File = Files.createTempDirectory("pageflow-autoscroll").toFile()
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
        repo.createPage(PageEntity(id = "lunga", title = "Lunga"))
        // Righe di altezze diverse, così ogni posizione della pagina si riconosce dal disegno.
        repeat(120) { i -> repo.saveBlock(BlockEntity(id = "r$i", pageId = "lunga", type = BlockType.PARAGRAPH, textJson = """[{"text":"Riga ${"x".repeat(1 + i % 7)} $i"}]""", orderIndex = i)) }
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    @Test
    fun theSpeedGrowsWithTheDistance() {
        assertEquals(0f, autoscrollSpeed(10f))
        assertEquals(0f, autoscrollSpeed(-12f))
        assertTrue(autoscrollSpeed(50f) > 0f && autoscrollSpeed(-50f) < 0f)
        assertTrue("più lontano, più svelto", autoscrollSpeed(200f) > 2 * autoscrollSpeed(100f))
        assertEquals(8000f, autoscrollSpeed(5000f))
    }

    @Test
    fun theMiddleButtonScrollsThePage() {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 600, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            PageEditorScreen(
                                pageId = "lunga", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
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
            time += 16_000_000L
            Thread.sleep(20)
        }
        fun shot(name: String): Bitmap = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }.also {
            File("build/desktop-screenshots", "$name.png").apply { parentFile.mkdirs() }.writeBytes(Image.makeFromBitmap(it).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        fun mouse(type: PointerEventType, at: Offset, buttons: PointerButtons = PointerButtons(), button: PointerButton? = null) = onWindowThread {
            scene.sendPointerEvent(type, at, type = PointerType.Mouse, buttons = buttons, button = button)
        }
        val middle = PointerButtons(isTertiaryPressed = true)
        /** Le righe di testo disegnate nella colonna del testo: cambiano quando la pagina scorre. */
        fun rows(bitmap: Bitmap): List<Int> =
            (60 until bitmap.height).filter { y -> (80 until 300).any { x -> bitmap.getColor(x, y).let { c -> ((c shr 16) and 0xFF) > 150 } } }
        fun bright(bitmap: Bitmap, x: Int, y: Int) = bitmap.getColor(x, y).let { c -> ((c shr 16) and 0xFF) > 220 && ((c shr 8) and 0xFF) > 220 && (c and 0xFF) > 220 }
        try {
            frames(40)
            val start = shot("autoscroll_start")
            val at = Offset(600f, 300f)
            // Clic centrale (premuto e lasciato): l'icona chiara nel punto del clic, la pagina ferma.
            mouse(PointerEventType.Move, at)
            mouse(PointerEventType.Press, at, middle, PointerButton.Tertiary)
            mouse(PointerEventType.Release, at, PointerButtons(), PointerButton.Tertiary)
            frames(5)
            val icon = shot("autoscroll_icon")
            assertTrue("l'icona", bright(icon, 600 - 10, 300) && bright(icon, 600 + 10, 300))
            assertEquals("ferma vicino all'icona", rows(start), rows(icon))
            // Il mouse 150 punti sotto: la pagina scende, e sempre di più.
            mouse(PointerEventType.Move, at + Offset(0f, 150f))
            frames(10)
            val down = shot("autoscroll_down")
            assertTrue("la pagina è scesa", rows(down) != rows(start))
            // Un clic sinistro, sul testo, la ferma: due foto di fila uguali, e nessun cursore che lampeggia lì.
            val onText = Offset(70f, 300f)
            mouse(PointerEventType.Move, onText)
            mouse(PointerEventType.Press, onText, PointerButtons(isPrimaryPressed = true), PointerButton.Primary)
            mouse(PointerEventType.Release, onText, PointerButtons(), PointerButton.Primary)
            frames(5)
            val stopped = shot("autoscroll_stopped")
            frames(40)
            val still = shot("autoscroll_still")
            assertEquals("ferma dopo il clic", rows(stopped), rows(still))
            val blinking = (40 until 330).flatMap { x -> (200 until 400).map { y -> x to y } }.count { (x, y) -> stopped.getColor(x, y) != still.getColor(x, y) }
            assertEquals("il clic che ferma non mette il cursore nel testo", 0, blinking)
            assertTrue("l'icona sparita", !bright(stopped, 590, 300) || !bright(stopped, 610, 300))
            // Tenuto premuto e mosso in su: sale; lasciato il tasto, si ferma.
            mouse(PointerEventType.Move, at)
            mouse(PointerEventType.Press, at, middle, PointerButton.Tertiary)
            mouse(PointerEventType.Move, at + Offset(0f, -120f), middle)
            frames(10)
            val up = shot("autoscroll_up")
            assertTrue("la pagina è salita", rows(up) != rows(stopped))
            mouse(PointerEventType.Release, at + Offset(0f, -120f), PointerButtons(), PointerButton.Tertiary)
            frames(5)
            val released = shot("autoscroll_released")
            frames(10)
            assertEquals("ferma dopo aver lasciato il tasto", rows(released), rows(shot("autoscroll_released_still")))
            /** Clic centrale, il mouse 150 sotto: la pagina scende. */
            fun startDown() {
                mouse(PointerEventType.Move, at)
                mouse(PointerEventType.Press, at, middle, PointerButton.Tertiary)
                mouse(PointerEventType.Release, at, PointerButtons(), PointerButton.Tertiary)
                mouse(PointerEventType.Move, at + Offset(0f, 150f))
                frames(5)
            }
            fun stopsBy(what: String) {
                frames(3)
                val first = shot("autoscroll_by_$what")
                frames(10)
                assertEquals("ferma con $what", rows(first), rows(shot("autoscroll_by_${what}_still")))
            }
            // La rotella.
            startDown()
            onWindowThread { scene.sendPointerEvent(PointerEventType.Scroll, at + Offset(0f, 150f), scrollDelta = Offset(0f, 1f), type = PointerType.Mouse) }
            stopsBy("rotella")
            // Esc, scrivendo in una riga.
            mouse(PointerEventType.Move, onText)
            mouse(PointerEventType.Press, onText, PointerButtons(isPrimaryPressed = true), PointerButton.Primary)
            mouse(PointerEventType.Release, onText, PointerButtons(), PointerButton.Primary)
            frames(5)
            startDown()
            onWindowThread {
                scene.sendKeyEvent(androidx.compose.ui.input.key.KeyEvent(androidx.compose.ui.input.key.Key.Escape, androidx.compose.ui.input.key.KeyEventType.KeyDown))
                scene.sendKeyEvent(androidx.compose.ui.input.key.KeyEvent(androidx.compose.ui.input.key.Key.Escape, androidx.compose.ui.input.key.KeyEventType.KeyUp))
            }
            stopsBy("esc")
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
