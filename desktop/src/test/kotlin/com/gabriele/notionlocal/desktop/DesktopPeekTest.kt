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
import androidx.compose.ui.input.pointer.PointerType
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
import com.gabriele.notionlocal.ui.navigation.AppShortcuts
import com.gabriele.notionlocal.ui.navigation.LocalPcWindow
import com.gabriele.notionlocal.ui.navigation.PcTabs
import com.gabriele.notionlocal.ui.navigation.PcWindowActions
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
            assertTrue("la pagina nel peek, al centro", textIn(center, 200..1000, 90..300) > 200)
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
            assertTrue("la pagina nel peek, a destra", textIn(side, 640..1150, 60..260) > 200)
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

    /** Ctrl+T apre una scheda nuova sul menu principale, Ctrl+W chiude quella aperta (12/11/2026). */
    @Test
    fun ctrlTAndCtrlWOpenAndCloseTabs() {
        AppSettings.init(context)
        AppSettings.lastVisitedRoute = null
        AppSettings.pcTabs = null
        runBlocking {
            PageRepository(AppDatabase.getInstance(context)).createPage(PageEntity(id = PageEntity.ROOT_PAGE_ID, title = "My Space"))
        }
        val factory = ViewModelFactory(context)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 300, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme { Surface(modifier = Modifier.fillMaxSize()) { AppNavHost(factory = factory) } }
                }
            }
        }
        var time = 0L
        fun bright(c: Int) = ((c shr 16) and 0xFF) > 140 && ((c shr 8) and 0xFF) > 140 && (c and 0xFF) > 140
        /** Quanti punti di scritta dove sta la seconda scheda (da 200 a 380): con una sola c'è solo il +. */
        fun tabText(name: String): Int {
            var picture: Bitmap? = null
            repeat(15) {
                picture = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }
                time += 100_000_000L
                Thread.sleep(30)
            }
            File("build/desktop-screenshots", "$name.png").writeBytes(Image.makeFromBitmap(picture!!).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            return (6..28).sumOf { y -> (230 until 380).count { x -> bright(picture!!.getColor(x, y)) } }
        }
        try {
            val one = tabText("tabs_one")
            assertEquals("con una scheda sola la barra non c'è", 0, one)
            onWindowThread { AppShortcuts.newTab!!.invoke() }
            val two = tabText("tabs_ctrl_t")
            assertTrue("Ctrl+T: una scheda in più ($one → $two)", two > one + 60)
            onWindowThread { AppShortcuts.closeTab!!.invoke() }
            val back = tabText("tabs_ctrl_w")
            assertTrue("Ctrl+W: di nuovo una ($two → $back)", back < two - 60)
        } finally {
            onWindowThread { scene.close() }
            AppSettings.pcTabs = null
        }
    }

    /** Le regole dello spostamento: le fissate fra le fissate, le altre fra le altre; una finestra staccata non si ricorda. */
    @Test
    fun movingAndDetachingTabsFollowThePins() {
        AppSettings.init(context)
        AppSettings.pcTabs = """{"tabs":[{"id":"p1","route":"page/x","pinned":true},{"id":"p2","route":"page/y","pinned":true},{"id":"a","route":"home"},{"id":"b","route":"page/z"}],"activeId":"a"}"""
        val tabs = PcTabs.load()
        fun order() = tabs.tabs.map { it.id }
        assertTrue("una fissata dopo le altre no", !tabs.move("p2", 2))
        assertTrue("un'altra fra le fissate no", !tabs.move("a", 1))
        assertTrue(tabs.move("p2", 0))
        assertTrue(tabs.move("b", 2))
        assertEquals(listOf("p2", "p1", "b", "a"), order())
        assertEquals(null, tabs.detach("p1"))
        val torn = tabs.detach("b")
        assertEquals("b", torn?.id)
        assertEquals(listOf("p2", "p1", "a"), order())
        // La finestra nuova ha la sua scheda, ma non tocca quelle salvate.
        val saved = AppSettings.pcTabs
        val window = PcTabs.detached(torn!!)
        window.open(Routes.HOME)
        assertEquals(saved, AppSettings.pcTabs)
        assertEquals(null, window.launchId)
        // Diventata lei la finestra che si ricorda, sì.
        window.persist = true
        assertTrue(AppSettings.pcTabs!!.contains("\"b\""))
        AppSettings.pcTabs = null
    }

    /**
     * **Le schede si trascinano** (13/11/2026, chiesto dall'utente): col mouse
     * una scheda passa dopo quella accanto; una fissata, unica fra le fissate,
     * resta dov'è e non si stacca; una scheda portata giù, fuori dalla barra, va
     * in una finestra nuova e sparisce da questa.
     */
    @Test
    fun aTabIsDraggedAmongTheOthersAndOutIntoANewWindow() {
        AppSettings.init(context)
        AppSettings.lastVisitedRoute = null
        runBlocking {
            val repo = PageRepository(AppDatabase.getInstance(context))
            repo.createPage(PageEntity(id = PageEntity.ROOT_PAGE_ID, title = "My Space"))
            repo.createPage(PageEntity(id = "target", title = "Target"))
            repo.createPage(PageEntity(id = "other", title = "Other"))
        }
        AppSettings.pcTabs = """{"tabs":[{"id":"p","route":"page/target","pinned":true},{"id":"a","route":"home"},{"id":"b","route":"page/other"}],"activeId":"a"}"""
        val opened = mutableListOf<Pair<String, Offset>>()
        val factory = ViewModelFactory(context)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 300, density = Density(1f)) {
                CompositionLocalProvider(
                    LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner,
                    LocalPcWindow provides PcWindowActions { tab, at -> opened += tab.id to at }
                ) {
                    NotionLocalTheme { Surface(modifier = Modifier.fillMaxSize()) { AppNavHost(factory = factory) } }
                }
            }
        }
        var time = 0L
        fun frames(count: Int) = repeat(count) {
            onWindowThread { scene.render(time) }
            time += 100_000_000L
            Thread.sleep(30)
        }
        fun order() = Regex(""""id":"(\w+)"""").findAll(AppSettings.pcTabs.orEmpty()).map { it.groupValues[1] }.toList()
        fun drag(from: Offset, by: Offset) {
            fun send(type: PointerEventType, at: Offset, pressed: Boolean) = onWindowThread {
                scene.sendPointerEvent(
                    type, at, type = PointerType.Mouse,
                    buttons = if (pressed) PointerButtons(isPrimaryPressed = true) else PointerButtons(),
                    button = if (type == PointerEventType.Move) null else PointerButton.Primary
                )
            }
            send(PointerEventType.Move, from, false)
            send(PointerEventType.Press, from, true)
            for (step in 1..20) {
                send(PointerEventType.Move, from + by * (step / 20f), true)
                frames(1)
            }
            send(PointerEventType.Release, from + by, false)
            frames(5)
        }
        try {
            frames(20)
            File("build/desktop-screenshots", "tabs_drag_before.png").writeBytes(Image.makeFromBitmap(onWindowThread { Bitmap.makeFromImage(scene.render(time)) }).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            // La fissata (6..38) portata a destra: è l'unica fissata, resta prima.
            drag(Offset(22f, 17f), Offset(300f, 0f))
            assertEquals(listOf("p", "a", "b"), order())
            // "My Space" (da 42) portata oltre "Other": passa dopo.
            drag(Offset(70f, 17f), Offset(260f, 0f))
            assertEquals(listOf("p", "b", "a"), order())
            File("build/desktop-screenshots", "tabs_drag_moved.png").writeBytes(Image.makeFromBitmap(onWindowThread { Bitmap.makeFromImage(scene.render(time)) }).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            // La fissata portata giù non si stacca.
            drag(Offset(22f, 17f), Offset(40f, 150f))
            assertEquals(listOf("p", "b", "a"), order())
            assertTrue(opened.isEmpty())
            // "Other", ora seconda (da 42), portata giù: una finestra nuova con lei, dove il mouse l'ha lasciata.
            drag(Offset(70f, 17f), Offset(60f, 150f))
            assertEquals(listOf("b"), opened.map { it.first })
            assertTrue("il mouse a ${opened.single().second}", (opened.single().second - Offset(130f, 167f)).getDistance() < 3f)
            assertEquals(listOf("p", "a"), order())
        } finally {
            onWindowThread { scene.close() }
            AppSettings.pcTabs = null
        }
    }

    private companion object {
        /** Il collegamento "Target" (con una scheda sola la barra delle schede non c'è). */
        const val LINK_Y = 189f
    }
}
