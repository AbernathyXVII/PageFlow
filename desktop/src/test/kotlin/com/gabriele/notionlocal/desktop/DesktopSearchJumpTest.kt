package com.gabriele.notionlocal.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.getAllSemanticsNodes
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
import com.gabriele.notionlocal.ui.screen.SearchJump
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Dalla ricerca al punto trovato** (chiesto dall'utente il 19/11/2026: "la pagina deve
 * scorrere fino al punto in cui quella voce che ho cercato si trova"): il risultato sa in
 * quale blocco sta il testo, e la pagina aperta da lì ci scorre e lo illumina.
 */
@OptIn(ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)
class DesktopSearchJumpTest {
    private val dir: File = Files.createTempDirectory("pageflow-searchjump").toFile()
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

    /** Una pagina di 80 righe; alla 70 una casella da spuntare (un campo a sé) con "zebra", alla 75 una riga "giraffa". */
    private fun longPage(repo: PageRepository) = runBlocking {
        repo.createPage(PageEntity(id = "lunga", title = "Lunga"))
        repeat(80) { i ->
            val (id, type, text) = when (i) {
                70 -> Triple("zebra", BlockType.CHECKBOX, "la zebra trovata")
                75 -> Triple("giraffa", BlockType.PARAGRAPH, "la giraffa trovata")
                else -> Triple("riga$i", BlockType.PARAGRAPH, "riga numero $i")
            }
            repo.saveBlock(BlockEntity(id = id, pageId = "lunga", type = type, textJson = """[{"text":"$text"}]""", orderIndex = i))
        }
    }

    @Test
    fun theHitKnowsItsBlock() {
        AppSettings.init(context)
        val repo = PageRepository(AppDatabase.getInstance(context))
        longPage(repo)
        val hits = runBlocking { repo.search("giraffa", inContent = true, inTrash = false) }
        assertEquals("giraffa", hits.single().blockId)
        // Trovata solo nel titolo: la pagina si apre in cima.
        assertNull(runBlocking { repo.search("Lunga", inContent = true, inTrash = false) }.single().blockId)
    }

    @Test
    fun theOpenedPageScrollsToTheCheckbox() = jumpTo("zebra") { scene ->
        // La casella è un campo a sé: il suo testo è sullo schermo, sotto la barra in alto.
        val top = onWindowThread {
            scene.semanticsOwners.flatMap { it.getAllSemanticsNodes(mergingEnabled = false) }
                .firstOrNull { node -> node.config.getOrNull(SemanticsProperties.EditableText)?.text?.contains("zebra") == true }
                ?.boundsInRoot?.top
        }
        assertTrue("la zebra sullo schermo: $top", top != null && top in 40f..560f)
    }

    @Test
    fun theOpenedPageScrollsToALineOfText() = jumpTo("giraffa") { scene ->
        val shot = onWindowThread { org.jetbrains.skia.Bitmap.makeFromImage(scene.render(10_000_000_000L)) }
        File("build/desktop-screenshots").mkdirs()
        File("build/desktop-screenshots", "search_jump_line.png").writeBytes(
            org.jetbrains.skia.Image.makeFromBitmap(shot).encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)!!.bytes
        )
    }

    private fun jumpTo(target: String, check: (ImageComposeScene) -> Unit) {
        AppSettings.init(context)
        val repo = PageRepository(AppDatabase.getInstance(context))
        longPage(repo)
        SearchJump.request("lunga", target)
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 600, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        PageEditorScreen(
                            pageId = "lunga", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
                            onNavigateToPage = {}, onNavigateToDatabase = {}, viewModel = viewModel
                        )
                    }
                }
            }
        }
        try {
            var time = 0L
            val end = System.currentTimeMillis() + 8_000
            while (System.currentTimeMillis() < end && viewModel.movedBlock.value?.blockId != target) {
                onWindowThread { scene.render(time) }
                time += 50_000_000L
                Thread.sleep(30)
            }
            // La riga trovata si illumina (la pagina ci è già scesa).
            assertEquals(target, viewModel.movedBlock.value?.blockId)
            repeat(5) {
                onWindowThread { scene.render(time) }
                time += 50_000_000L
            }
            check(scene)
            // Una volta sola: la richiesta è stata presa.
            assertNull(SearchJump.take("lunga"))
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
