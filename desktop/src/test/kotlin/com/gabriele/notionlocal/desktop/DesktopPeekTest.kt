package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
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
import com.gabriele.notionlocal.data.settings.OpenMode
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.navigation.AppNavHost
import com.gabriele.notionlocal.ui.navigation.PcTabs
import com.gabriele.notionlocal.ui.navigation.Routes
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Sul PC le pagine in Center peek e in Side peek** (12/11/2026, chiesto
 * dall'utente come in Notion): un clic sul collegamento a una pagina la apre
 * nel modo di Settings.
 */
class DesktopPeekTest {
    private val dir: File = Files.createTempDirectory("pageflow-peek").toFile()
    private val context = DesktopContext(dir)

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    @After
    fun tearDown() {
        AppSettings.openPagesAs = OpenMode.CENTER_PEEK
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    @Test
    fun aLinkOpensInThePeek() {
        AppSettings.init(context)
        AppSettings.sidebarPinned = false
        AppSettings.lastVisitedRoute = null
        runBlocking {
            val repo = PageRepository(AppDatabase.getInstance(context))
            repo.createPage(PageEntity(id = PageEntity.ROOT_PAGE_ID, title = "My Space"))
            repo.createPage(PageEntity(id = "target", title = "Target"))
            repo.saveBlock(BlockEntity(pageId = "target", textJson = """[{"text":"Peeked text"}]""", orderIndex = 0))
            repo.saveBlock(BlockEntity(id = "link", pageId = PageEntity.ROOT_PAGE_ID, type = BlockType.PAGE_LINK, linkedPageId = "target", orderIndex = 0))
        }
        val factory = ViewModelFactory(context)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 1200, height = 700, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme { Surface(modifier = Modifier.fillMaxSize()) { AppNavHost(factory = factory) } }
                }
            }
        }
        var time = 0L
        fun shot(name: String): Bitmap {
            var picture: Bitmap? = null
            repeat(25) {
                picture = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }
                time += 100_000_000L
                Thread.sleep(30)
            }
            File("build/desktop-screenshots", "$name.png").apply { parentFile.mkdirs() }
                .writeBytes(Image.makeFromBitmap(picture!!).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            return picture!!
        }
        fun click(x: Float, y: Float) = onWindowThread {
            val at = Offset(x, y)
            scene.sendPointerEvent(PointerEventType.Move, at)
            scene.sendPointerEvent(PointerEventType.Press, at, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
            scene.sendPointerEvent(PointerEventType.Release, at, buttons = PointerButtons(), button = PointerButton.Primary)
        }
        try {
            val home = shot("peek_home")
            // Center peek (quello di Settings di partenza): la finestra al centro, il resto scurito.
            click(128f, LINK_Y)
            val center = shot("peek_center")
            fun bright(c: Int) = ((c shr 16) and 0xFF) > 140 && ((c shr 8) and 0xFF) > 140 && (c and 0xFF) > 140
            fun textIn(picture: Bitmap, xs: IntRange, ys: IntRange) = ys.sumOf { y -> xs.count { x -> bright(picture.getColor(x, y)) } }
            assertNotEquals("in Center peek il resto è scurito", home.getColor(1190, 690), center.getColor(1190, 690))
            assertTrue("la pagina nel peek, al centro", textIn(center, 200..1000, 130..280) > 200)
            // Un clic fuori la chiude.
            click(60f, 400f)
            val closed = shot("peek_closed")
            assertEquals("chiuso, niente più velo", home.getColor(1190, 690), closed.getColor(1190, 690))
            // Side peek: a destra, la pagina a sinistra com'era.
            AppSettings.openPagesAs = OpenMode.SIDE_PEEK
            click(128f, LINK_Y)
            val side = shot("peek_side")
            assertEquals("in Side peek la pagina a sinistra resta com'è", home.getColor(300, 600), side.getColor(300, 600))
            assertTrue("il bordo a metà", (590..612).any { x -> side.getColor(x, 600) != home.getColor(x, 600) })
            assertTrue("la pagina nel peek, a destra", textIn(side, 640..1150, 90..250) > 200)
            // New tab: una scheda nuova con la pagina; la prima resta sul menu principale.
            click(60f, 400f)
            AppSettings.openPagesAs = OpenMode.NEW_TAB
            shot("peek_before_tab")
            click(128f, LINK_Y)
            val tabbed = shot("tab_new")
            assertTrue("la pagina nella scheda nuova", textIn(tabbed, 80..600, 90..250) > 200)
        } finally {
            onWindowThread { scene.close() }
        }
    }

    /** Le schede: aprirle, fissarle (vanno a sinistra), chiuderle, e ritrovarle alla volta dopo. */
    @Test
    fun theTabsKeepTheirOrderAndComeBack() {
        AppSettings.init(context)
        AppSettings.pcTabs = null
        val tabs = PcTabs.load()
        assertEquals(1, tabs.tabs.size)
        tabs.open(Routes.pageEditor("a"))
        tabs.open(Routes.pageEditor("b"))
        assertEquals(listOf(Routes.HOME, "page/a", "page/b"), tabs.tabs.map { it.route })
        assertEquals("page/b", tabs.active.route)
        // Fissata, "b" va a sinistra, prima delle altre.
        tabs.setPinned(tabs.active.id, true)
        assertEquals(listOf("page/b", Routes.HOME, "page/a"), tabs.tabs.map { it.route })
        // Chiusa la scheda attiva, si passa a quella accanto.
        val a = tabs.tabs.first { it.route == "page/a" }.id
        tabs.select(a)
        tabs.close(a)
        assertEquals(Routes.HOME, tabs.active.route)
        // Riaperta l'app: le stesse schede, la stessa attiva.
        val again = PcTabs.load()
        assertEquals(listOf("page/b" to true, Routes.HOME to false), again.tabs.map { it.route to it.pinned })
        assertEquals(Routes.HOME, again.active.route)
        AppSettings.pcTabs = null
    }

    /** Una scheda fissata è solo l'icona della pagina: il nome della scheda dopo comincia subito. */
    @Test
    fun aPinnedTabIsJustTheIcon() {
        AppSettings.init(context)
        AppSettings.lastVisitedRoute = null
        runBlocking {
            val repo = PageRepository(AppDatabase.getInstance(context))
            repo.createPage(PageEntity(id = PageEntity.ROOT_PAGE_ID, title = "My Space"))
            repo.createPage(PageEntity(id = "target", title = "Target"))
        }
        AppSettings.pcTabs = """{"tabs":[{"id":"a","route":"page/target","pinned":true},{"id":"b","route":"home"}],"activeId":"b"}"""
        val factory = ViewModelFactory(context)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 300, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme { Surface(modifier = Modifier.fillMaxSize()) { AppNavHost(factory = factory) } }
                }
            }
        }
        try {
            var picture: Bitmap? = null
            var time = 0L
            repeat(20) {
                picture = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }
                time += 100_000_000L
                Thread.sleep(30)
            }
            File("build/desktop-screenshots", "tab_pinned.png").writeBytes(Image.makeFromBitmap(picture!!).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            fun bright(c: Int) = ((c shr 16) and 0xFF) > 140 && ((c shr 8) and 0xFF) > 140 && (c and 0xFF) > 140
            // Il nome "My Space" della seconda scheda comincia prima di 80: la prima è larga quanto l'icona.
            val firstText = (40 until 300).first { x -> (8..26).any { y -> bright(picture!!.getColor(x, y)) } }
            assertTrue("il nome della seconda scheda comincia a $firstText", firstText < 80)
        } finally {
            onWindowThread { scene.close() }
            AppSettings.pcTabs = null
        }
    }

    private companion object {
        /** Il collegamento "Target", sotto la barra delle schede. */
        const val LINK_Y = 223f
    }
}
