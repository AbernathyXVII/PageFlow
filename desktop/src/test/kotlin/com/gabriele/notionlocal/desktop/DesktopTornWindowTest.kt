package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.navigation.AppNavHost
import com.gabriele.notionlocal.ui.navigation.PcTab
import com.gabriele.notionlocal.ui.navigation.PcTabs
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Una scheda staccata in una finestra sua** (24/11/2026, trovato dall'utente): la finestra nuova si
 * chiudeva con "Cannot navigate to page/…: Navigation graph has not been set". La scheda chiedeva la sua
 * pagina appena nata, prima che la finestra fosse disegnata la prima volta.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopTornWindowTest {
    private val dir: File = Files.createTempDirectory("pageflow-torn").toFile()
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
    fun aTornTabOpensItsPageInTheNewWindow() {
        AppSettings.init(context)
        AppSettings.lastVisitedRoute = null
        runBlocking {
            val repo = PageRepository(AppDatabase.getInstance(context))
            repo.createPage(PageEntity(id = PageEntity.ROOT_PAGE_ID, title = "My Space"))
            repo.createPage(PageEntity(id = "staccata", title = "Pagina staccata"))
        }
        val failures = mutableListOf<Throwable>()
        val factory = ViewModelFactory(context)
        val owner = ScreenOwner()
        val window = PcTabs.detached(PcTab(id = "t", route = "page/staccata"))
        // Come la finestra nuova: il contenuto nasce subito, il primo disegno arriva dopo.
        val scene = onWindowThread {
            ImageComposeScene(
                width = 900, height = 400, density = Density(1f),
                coroutineContext = Dispatchers.Unconfined + CoroutineExceptionHandler { _, e -> failures += e }
            ) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme { Surface(modifier = Modifier.fillMaxSize()) { AppNavHost(factory = factory, windowTabs = window) } }
                }
            }
        }
        try {
            Thread.sleep(1_000)
            var time = 0L
            repeat(30) {
                onWindowThread { scene.render(time) }
                time += 100_000_000L
                Thread.sleep(30)
            }
            assertEquals("nessun errore: $failures", emptyList<Throwable>(), failures)
            val texts = onWindowThread {
                scene.semanticsOwners.flatMap { it.getAllSemanticsNodes(mergingEnabled = false) }
                    .mapNotNull { node -> node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } }
            }
            assertTrue("la finestra apre la sua pagina: ${texts.take(30)}", texts.any { "Pagina staccata" in it })
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
