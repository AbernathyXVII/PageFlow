package com.gabriele.notionlocal.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.PageParagraph
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.screen.pageInsetFor
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
 * **Sul PC "Full width" o al centro** (12/11/2026, chiesto dall'utente come
 * in Notion): tolto "Full width" dai tre puntini, il testo sta in una colonna
 * larga 637 al centro dello spazio della pagina; rimesso, torna a cominciare
 * a 86 come prima.
 */
class DesktopPageWidthTest {
    private val dir: File = Files.createTempDirectory("pageflow-width").toFile()
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

    @Test
    fun theMarginForEachWidth() {
        assertEquals(66.dp, pageInsetFor(true, 1400.dp))
        // (1400 - 637 - 2 × 20) / 2
        assertEquals(361.5.dp, pageInsetFor(false, 1400.dp))
        // Una finestra stretta: i margini di sempre.
        assertEquals(66.dp, pageInsetFor(false, 700.dp))
    }

    @Test
    fun aCenteredPageAndBackToFullWidth() {
        AppSettings.init(context)
        runBlocking {
            val repo = PageRepository(AppDatabase.getInstance(context))
            repo.createPage(PageEntity(id = "p", title = "Centered page", paragraphJson = PageParagraph.encode(PageParagraph(fullWidth = false))))
            repo.saveBlock(BlockEntity(id = "b", pageId = "p", textJson = """[{"text":"Hello world"}]""", orderIndex = 0))
        }
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 1400, height = 500, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        PageEditorScreen(
                            pageId = "p", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
                            onNavigateToPage = {}, onNavigateToDatabase = {}, viewModel = viewModel
                        )
                    }
                }
            }
        }
        fun bright(c: Int) = ((c shr 16) and 0xFF) > 140 && ((c shr 8) and 0xFF) > 140 && (c and 0xFF) > 140
        /** Il primo punto bianco da sinistra, sotto la barra in alto: dove comincia il testo. */
        fun textStart(name: String): Int {
            var time = 0L
            repeat(30) {
                onWindowThread { scene.render(time) }
                time += 100_000_000L
                Thread.sleep(30)
            }
            val page = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }
            File("build/desktop-screenshots", "$name.png").apply { parentFile.mkdirs() }
                .writeBytes(Image.makeFromBitmap(page).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            return (0 until page.width).first { x -> (60 until page.height).any { y -> bright(page.getColor(x, y)) } }
        }
        try {
            val centered = textStart("page_centered")
            // 361,5 di margine e 20 dentro la riga: il testo a 381.
            assertTrue("al centro comincia a $centered", centered in 378..392)
            onWindowThread { viewModel.setPageParagraph(PageParagraph()) }
            val full = textStart("page_full_width")
            assertTrue("full width comincia a $full", full in 84..92)
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
