package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.screen.LibraryScreen
import com.gabriele.notionlocal.ui.screen.RecentsSidebarSection
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **"Recents" e la Library sul PC** (dal 13/11/2026, chiesti dall'utente come
 * in Notion): a sinistra la sezione della barra laterale, a destra la Library
 * larga, a tabella. La foto serve a guardarla; la prova conta le righe.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopLibraryTest {
    private val dir: File = Files.createTempDirectory("pageflow-library").toFile()
    private val context = DesktopContext(dir)
    private lateinit var repo: PageRepository

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        repo = PageRepository(AppDatabase.getInstance(context))
        repo.createPage(PageEntity(id = "biblio-casa", title = "Casa", icon = "🏠"))
        listOf("Ricette", "Viaggi", "Libri letti", "Film").forEachIndexed { i, title ->
            repo.createPage(PageEntity(id = "biblio-$i", title = title))
            repo.saveBlock(BlockEntity(pageId = "biblio-casa", type = BlockType.PAGE_LINK, linkedPageId = "biblio-$i", orderIndex = i))
            AppSettings.recordVisit("biblio-$i", at = System.currentTimeMillis() - (4 - i) * 3_600_000L)
        }
        AppSettings.recordVisit("biblio-casa")
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    @Test
    fun theSidebarSectionAndTheWideLibrary() {
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 1300, height = 600, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Row(Modifier.fillMaxSize().background(DarkBackground)) {
                            Column(Modifier.width(240.dp).fillMaxHeight()) {
                                RecentsSidebarSection(observePages = repo::observePages, currentPageId = "biblio-3", onOpen = {}, onViewAll = {})
                            }
                            LibraryScreen(repository = repo, onBack = {}, onOpenPage = {})
                        }
                    }
                }
            }
        }
        try {
            var time = 0L
            repeat(40) {
                onWindowThread { scene.render(time) }
                time += 100_000_000L
                Thread.sleep(30)
            }
            val shot = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }
            File("build/desktop-screenshots").mkdirs()
            File("build/desktop-screenshots", "library.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(shot).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            fun bright(c: Int) = ((c shr 16) and 0xFF) > 140 && ((c shr 8) and 0xFF) > 140 && (c and 0xFF) > 140
            fun bands(fromX: Int, toX: Int): Int {
                val rows = (0 until shot.height).filter { y -> (fromX until toX).any { x -> bright(shot.getColor(x, y)) } }
                return rows.zipWithNext().count { (a, b) -> b - a > 1 } + if (rows.isEmpty()) 0 else 1
            }
            // Nella barra: le cinque pagine e "View all".
            assertTrue("righe nella barra: ${bands(30, 230)}", bands(30, 230) >= 6)
            // Nella Library: la barra in alto, le cinque righe.
            assertTrue("righe nella Library: ${bands(300, 700)}", bands(300, 700) >= 6)
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
