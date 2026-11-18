package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **In fondo alla pagina lo spazio di tre righe vuote** (18/11/2026, chiesto dall'utente: "Mi
 * aumenti lo spazio di uno, cosicché lo spazio vuoto totale sia di tre?"): scesi fino in fondo,
 * fra l'ultima riga e il bordo c'è posto per tre righe.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopBottomSpaceTest {
    private val dir: File = Files.createTempDirectory("pageflow-bottomspace").toFile()
    private val context = DesktopContext(dir)

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        val repo = PageRepository(AppDatabase.getInstance(context))
        repo.createPage(PageEntity(id = "lunga", title = "Lunga"))
        repeat(60) { i -> repo.saveBlock(BlockEntity(id = "r$i", pageId = "lunga", type = BlockType.PARAGRAPH, textJson = """[{"text":"Riga $i"}]""", orderIndex = i)) }
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    @Test
    fun atTheEndThereIsRoomForThreeLines() {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }
        val scene = onWindowThread {
            ImageComposeScene(width = 800, height = 600, density = Density(1f)) {
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
            time += 100_000_000L
            Thread.sleep(20)
        }
        try {
            frames(40)
            // Giù fino in fondo con la rotella.
            repeat(40) {
                onWindowThread { scene.sendPointerEvent(PointerEventType.Scroll, Offset(400f, 300f), scrollDelta = Offset(0f, 10f), type = PointerType.Mouse) }
                frames(1)
            }
            frames(20)
            val shot = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }
            File("build/desktop-screenshots").mkdirs()
            File("build/desktop-screenshots", "bottom_space.png").writeBytes(Image.makeFromBitmap(shot).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            fun bright(c: Int) = ((c shr 16) and 0xFF) > 140 && ((c shr 8) and 0xFF) > 140 && (c and 0xFF) > 140
            val textRows = (0 until 600).filter { y -> (40 until 300).any { x -> bright(shot.getColor(x, y)) } }
            val gap = 600 - 1 - textRows.max()
            // Le righe sono alte circa 19 punti: tre righe vuote e un po' del margine dell'ultima.
            assertTrue("in fondo lo spazio di tre righe: $gap punti", gap >= 72)
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
