package com.gabriele.notionlocal.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
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
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Un toggle chiuso resta chiuso** (chiesto dall'utente l'11/11/2026): chiuso
 * col clic sulla freccia, uscendo dalla pagina e riaprendola è ancora chiuso.
 */
@OptIn(ExperimentalComposeUiApi::class)
class DesktopToggleMemoryTest {
    private val dir: File = Files.createTempDirectory("pageflow-toggle").toFile()
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

    private fun bright(c: Int) = ((c shr 16) and 0xFF) > 140 && ((c shr 8) and 0xFF) > 140 && (c and 0xFF) > 140

    /** Le fasce di righe con lettere chiare sotto la barra in alto. */
    private fun bands(page: Bitmap): List<IntRange> {
        val rows = (60 until page.height).filter { y -> (47 until page.width - 20).any { x -> bright(page.getColor(x, y)) } }
        val bands = ArrayList<IntRange>()
        for (y in rows) {
            val last = bands.lastOrNull()
            if (last != null && y <= last.last + 1) bands[bands.lastIndex] = last.first..y else bands += y..y
        }
        return bands
    }

    private fun expandedInDatabase() = runBlocking { AppDatabase.getInstance(context).blockDao().getById("toggle")?.isExpanded }

    @Test
    fun aClosedToggleStaysClosedAfterLeavingThePage() {
        AppSettings.init(context)
        runBlocking {
            val repo = PageRepository(AppDatabase.getInstance(context))
            repo.createPage(PageEntity(id = "casa", title = "Casa"))
            repo.saveBlock(BlockEntity(id = "toggle", pageId = "casa", type = BlockType.TOGGLE, textJson = """[{"text":"Titolo"}]""", orderIndex = 0))
            repo.saveBlock(BlockEntity(id = "dentro", pageId = "casa", parentBlockId = "toggle", textJson = """[{"text":"Dentro"}]""", orderIndex = 0))
            repo.saveBlock(BlockEntity(id = "sotto", pageId = "casa", textJson = """[{"text":"Sotto"}]""", orderIndex = 1))
        }

        /** Apre la pagina, fa [act] e la chiude come uscendo (il ViewModel se ne va). */
        fun openPage(name: String, act: (ImageComposeScene, Bitmap, () -> Bitmap) -> Unit = { _, _, _ -> }): Bitmap {
            val factory = ViewModelFactory(context)
            val owner = ScreenOwner()
            val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
            val scene = onWindowThread {
                ImageComposeScene(width = 600, height = 400, density = Density(1f)) {
                    CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                        NotionLocalTheme {
                            PageEditorScreen(
                                pageId = "casa", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
                                onNavigateToPage = {}, onNavigateToDatabase = {}, viewModel = viewModel
                            )
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
            fun picture(): Bitmap = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }
            try {
                frames(40)
                act(scene, picture()) { frames(15); picture() }
                frames(10)
                return picture().also {
                    File("build/desktop-screenshots", name).writeBytes(Image.makeFromBitmap(it).encodeToData(EncodedImageFormat.PNG)!!.bytes)
                }
            } finally {
                onWindowThread { scene.close() }
                owner.viewModelStore.clear()
                Thread.sleep(500)
            }
        }

        var openBands = 0
        val closed = openPage("toggle_closed.png") { scene, page, after ->
            val lines = bands(page)
            openBands = lines.size
            // Le ultime tre righe: "Titolo" col triangolino, "Dentro", "Sotto" (sopra c'è il titolo della pagina).
            val toggleLine = lines[lines.size - 3]
            val y = (toggleLine.first + toggleLine.last) / 2
            val arrowLeft = (47 until 300).first { x -> (toggleLine.first..toggleLine.last).any { bright(page.getColor(x, it)) } }
            val at = Offset(arrowLeft + 4f, y.toFloat())
            onWindowThread {
                scene.sendPointerEvent(PointerEventType.Move, at, type = PointerType.Mouse)
                scene.sendPointerEvent(PointerEventType.Press, at, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
                scene.sendPointerEvent(PointerEventType.Release, at, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
            }
            val shut = after()
            assertEquals("chiuso, \"Dentro\" non si vede più: ${bands(shut)}", openBands - 1, bands(shut).size)
        }
        assertEquals("chiuso nel database", false, expandedInDatabase())
        assertEquals(openBands - 1, bands(closed).size)

        // Riaperta la pagina: il toggle è ancora chiuso.
        val again = openPage("toggle_reopened.png")
        assertEquals("ancora chiuso nel database", false, expandedInDatabase())
        assertTrue("riaperta la pagina il toggle è ancora chiuso: ${bands(again)}", bands(again).size == openBands - 1)
    }
}
