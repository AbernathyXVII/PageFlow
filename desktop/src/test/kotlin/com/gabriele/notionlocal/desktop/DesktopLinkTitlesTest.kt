package com.gabriele.notionlocal.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
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
import com.gabriele.notionlocal.data.LinkTitles
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **"Show as: Page title" sul PC** (l'utente il 14/11/2026, nel collaudo: "Su PC i
 * titoli non arrivano proprio"): un database dentro una pagina, la sua proprietà
 * Link coi titoli, e il titolo della pagina del link arriva nella cella.
 */
@OptIn(ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)
class DesktopLinkTitlesTest {
    private val dir: File = Files.createTempDirectory("pageflow-titles").toFile()
    private val context = DesktopContext(dir)
    private val original = LinkTitles.source

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    @After
    fun tearDown() {
        LinkTitles.source = original
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    @Test
    fun theTitleArrivesInTheCell() {
        LinkTitles.source = { url -> if (url == "https://esempio.it/a") "Titolo A" else null }
        AppSettings.init(context)
        val db = AppDatabase.getInstance(context)
        val column = DatabaseColumnEntity(pageId = "siti", name = "Link", type = ColumnType.URL, orderIndex = 0, numberDisplay = "TITLE")
        val row = DatabaseRowEntity(pageId = "siti", title = "Uno", orderIndex = 0)
        runBlocking {
            val repo = PageRepository(db)
            repo.createPage(PageEntity(id = "spazio", title = "Spazio"))
            repo.createPage(PageEntity(id = "siti", title = "Siti", isDatabase = true, parentId = "spazio"))
            repo.saveBlock(BlockEntity(id = "db", pageId = "spazio", type = BlockType.DATABASE_LINK, linkedPageId = "siti", orderIndex = 0))
            db.databaseDao().insertColumn(column)
            db.databaseDao().insertRow(row)
            db.databaseDao().insertCell(DatabaseCellEntity(rowId = row.id, columnId = column.id, value = "https://esempio.it/a"))
        }
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 700, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        PageEditorScreen(
                            pageId = "spazio", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
                            onNavigateToPage = {}, onNavigateToDatabase = {}, viewModel = viewModel
                        )
                    }
                }
            }
        }
        try {
            var time = 0L
            val end = System.currentTimeMillis() + 8_000
            fun label() = runBlocking { db.databaseDao().getCell(row.id, column.id)?.linkLabel }
            while (System.currentTimeMillis() < end && label() == null) {
                onWindowThread { scene.render(time) }
                time += 100_000_000L
                Thread.sleep(50)
            }
            assertEquals("Titolo A", label())
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
