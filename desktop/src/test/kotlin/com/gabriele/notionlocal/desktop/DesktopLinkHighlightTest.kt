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
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **La riga arrivata con un collegamento resta illuminata un secondo in più** (18/11/2026, chiesto
 * dall'utente: "vorrei che il focus rimanesse un secondo più a lungo, cosicché io sia sicuro di dove
 * si trova il titolo linkato"): dopo il tempo in cui prima la luce era già sparita, c'è ancora.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopLinkHighlightTest {
    private val dir: File = Files.createTempDirectory("pageflow-linklight").toFile()
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
        repo.createPage(PageEntity(id = "luce", title = "Luce"))
        repeat(6) { i -> repo.saveBlock(BlockEntity(id = "r$i", pageId = "luce", type = BlockType.PARAGRAPH, textJson = """[{"text":"Riga $i"}]""", orderIndex = i)) }
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    @Test
    fun theLinkedLineStaysLitASecondMore() {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }
        val scene = onWindowThread {
            ImageComposeScene(width = 800, height = 500, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            PageEditorScreen(
                                pageId = "luce", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
                                onNavigateToPage = {}, onNavigateToDatabase = {}, viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
        var time = 0L
        fun frames(count: Int, sleep: Long = 20) = repeat(count) {
            onWindowThread { scene.render(time) }
            time += 100_000_000L
            Thread.sleep(sleep)
        }
        fun picture(): Bitmap = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }
        /** Quanti punti a destra del testo, fra le righe, non sono del colore di una pagina senza luce. */
        fun lit(now: Bitmap, clean: Bitmap) = (150 until 450 step 3).sumOf { y -> (300 until 700 step 5).count { x -> now.getColor(x, y) != clean.getColor(x, y) } }
        try {
            frames(40)
            val clean = picture()
            viewModel.highlightBlock("r3")
            // Un secondo nei fotogrammi: prima la luce (0,7 s) era già finita; ora resta piena ancora un secondo.
            frames(10)
            val held = lit(picture(), clean)
            assertTrue("dopo un secondo la riga è ancora illuminata: $held", held > 20)
            // Passato il secondo e la dissolvenza, se ne va.
            Thread.sleep(1_200)
            frames(15)
            val gone = lit(picture(), clean)
            assertTrue("poi la luce se ne va: $gone", gone < 5)
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
