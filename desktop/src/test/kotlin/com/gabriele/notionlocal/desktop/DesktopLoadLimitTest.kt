package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.DEFAULT_LOAD_LIMIT
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.loadLimit
import com.gabriele.notionlocal.data.entity.nameColumnHidden
import com.gabriele.notionlocal.data.entity.withLoadLimit
import com.gabriele.notionlocal.data.entity.withNameHidden
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **"Load limit", database per database** (chiesto dall'utente il 23/11/2026: "Volevo deciderlo
 * database per database, dipendentemente dalle righe che mi servono"): un segno nella vista, accanto
 * a quello di Name nascosta, e un database dentro una pagina che mostra le prime righe e "Load more".
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopLoadLimitTest {
    private val dir: File = Files.createTempDirectory("pageflow-loadlimit").toFile()
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
    fun theMarkLivesBesideTheOthers() {
        val hidden = withNameHidden(null, true)
        val limited = PageEntity(title = "Vista", viewHiddenColumnIds = withLoadLimit(hidden, 25))
        assertEquals(25, limited.loadLimit())
        assertTrue(limited.nameColumnHidden())
        // Di nuovo 50, quello di sempre: il segno se ne va.
        val back = withLoadLimit(limited.viewHiddenColumnIds, DEFAULT_LOAD_LIMIT)
        assertEquals(hidden, back)
        assertEquals(DEFAULT_LOAD_LIMIT, PageEntity(title = "Vista").loadLimit())
    }

    @Test
    fun aDatabaseInAPageShowsItsFirstRows() {
        AppSettings.init(context)
        val db = AppDatabase.getInstance(context)
        runBlocking {
            val repo = PageRepository(db)
            repo.createPage(PageEntity(id = "casa", title = "Casa"))
            repo.createPage(PageEntity(id = "elenco", title = "Elenco", isDatabase = true, parentId = "casa", viewHiddenColumnIds = withLoadLimit(null, 10)))
            repo.saveBlock(BlockEntity(id = "db", pageId = "casa", type = BlockType.DATABASE_LINK, linkedPageId = "elenco", orderIndex = 0))
            for (i in 1..30) db.databaseDao().insertRow(DatabaseRowEntity(id = "r$i", pageId = "elenco", title = "Riga $i", orderIndex = i))
        }
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 1000, height = 1400, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            PageEditorScreen(
                                pageId = "casa", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
                                onNavigateToPage = {}, onNavigateToDatabase = {}, viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
        try {
            repeat(40) { i -> onWindowThread { scene.render(i * 100_000_000L) }; Thread.sleep(30) }
            val texts = onWindowThread {
                scene.semanticsOwners.flatMap { it.getAllSemanticsNodes(mergingEnabled = false) }
                    .mapNotNull { node -> node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } }
            }
            assertTrue("Riga 10 c'è: ${texts.filter { it.startsWith("Riga") }}", "Riga 10" in texts)
            assertFalse("Riga 11 no", "Riga 11" in texts)
            assertTrue("Load more", texts.any { it == "Load more" })
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
