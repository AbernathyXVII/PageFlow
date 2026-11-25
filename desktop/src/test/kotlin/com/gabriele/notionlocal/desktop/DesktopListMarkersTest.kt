package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
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
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.listMarker
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Gli elenchi** (25/11/2026, trovati e chiesti dall'utente): Invio all'inizio di una riga sotto un
 * "6." non fa un "7." sopra di lei; "7." scritto davanti a del testo fa la voce e il testo resta; il segno
 * scelto col tasto destro vale per tutto l'elenco a quel rientro, anche dentro un toggle.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopListMarkersTest {
    private val dir: File = Files.createTempDirectory("pageflow-markers").toFile()
    private val context = DesktopContext(dir)
    private lateinit var db: AppDatabase

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        db = AppDatabase.getInstance(context)
        val repo = PageRepository(db)
        repo.createPage(PageEntity(id = "m", title = "M"))
        listOf(
            BlockEntity(id = "n1", pageId = "m", type = BlockType.NUMBERED_LIST_ITEM, textJson = """[{"text":"Uno"}]"""),
            BlockEntity(id = "n2", pageId = "m", type = BlockType.NUMBERED_LIST_ITEM, textJson = """[{"text":"Due"}]"""),
            BlockEntity(id = "p", pageId = "m", type = BlockType.PARAGRAPH, textJson = """[{"text":"Testo"}]"""),
            BlockEntity(id = "t", pageId = "m", type = BlockType.TOGGLE, textJson = """[{"text":"Toggle"}]""", isExpanded = true)
        ).forEachIndexed { i, block -> repo.saveBlock(block.copy(orderIndex = i)) }
        listOf(
            BlockEntity(id = "b1", pageId = "m", type = BlockType.BULLET_LIST_ITEM, textJson = """[{"text":"Primo"}]"""),
            BlockEntity(id = "b2", pageId = "m", type = BlockType.BULLET_LIST_ITEM, textJson = """[{"text":"Secondo"}]""")
        ).forEachIndexed { i, block -> repo.saveBlock(block.copy(parentBlockId = "t", orderIndex = i)) }
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun blocks(): List<BlockEntity> = runBlocking { db.blockDao().getBlocksForPageOnce("m") }
    private fun top(): List<BlockEntity> = blocks().filter { it.parentBlockId == null }.sortedBy { it.orderIndex }
    private fun text(block: BlockEntity): String = Regex("\"text\":\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(block.textJson).joinToString("") { it.groupValues[1] }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    private class Page(val scene: ImageComposeScene, val viewModel: PageEditorViewModel)

    private fun page(): Page {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 700, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            PageEditorScreen(
                                pageId = "m", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
                                onNavigateToPage = {}, onNavigateToDatabase = {}, viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
        return Page(scene, viewModel)
    }

    private var time = 0L
    private fun frames(scene: ImageComposeScene, count: Int) = repeat(count) {
        onWindowThread { scene.render(time) }
        time += 100_000_000L
        Thread.sleep(30)
    }

    private fun key(scene: ImageComposeScene, k: Key) {
        onWindowThread {
            scene.sendKeyEvent(KeyEvent(k, KeyEventType.KeyDown))
            scene.sendKeyEvent(KeyEvent(k, KeyEventType.KeyUp))
        }
        frames(scene, 6)
    }

    private fun type(scene: ImageComposeScene, char: Char) {
        onWindowThread {
            val awt = java.awt.event.KeyEvent(java.awt.Canvas(), java.awt.event.KeyEvent.KEY_TYPED, 0L, 0, java.awt.event.KeyEvent.VK_UNDEFINED, char)
            scene.sendKeyEvent(KeyEvent(Key.Unknown, KeyEventType.Unknown, codePoint = char.code, nativeEvent = awt))
        }
        frames(scene, 6)
    }

    private fun click(scene: ImageComposeScene, at: Offset) {
        onWindowThread {
            scene.sendPointerEvent(PointerEventType.Move, at, type = PointerType.Mouse)
            scene.sendPointerEvent(PointerEventType.Press, at, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
            scene.sendPointerEvent(PointerEventType.Release, at, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
        }
        frames(scene, 5)
    }

    /** Le fasce di righe con del testo chiaro, dall'alto. */
    private fun bands(scene: ImageComposeScene, name: String): List<IntRange> {
        val shot = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }
        File("build/desktop-screenshots").mkdirs()
        File("build/desktop-screenshots", "$name.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(shot).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        fun bright(c: Int) = ((c shr 16) and 0xFF) > 150 && ((c shr 8) and 0xFF) > 150 && (c and 0xFF) > 150
        val rows = (60 until shot.height).filter { y -> (40 until 800).any { x -> bright(shot.getColor(x, y)) } }
        val result = ArrayList<IntRange>()
        for (y in rows) {
            val last = result.lastOrNull()
            if (last != null && y <= last.last + 3) result[result.lastIndex] = last.first..y else result += y..y
        }
        return result
    }

    private fun waitFor(what: String, check: () -> Boolean) {
        val end = System.currentTimeMillis() + 5_000
        while (!check() && System.currentTimeMillis() < end) Thread.sleep(30)
        assertTrue(what, check())
    }

    @Test
    fun enterAtTheStartOfALineUnderAListMakesAPlainLine() {
        val page = page()
        try {
            frames(page.scene, 40)
            val rows = bands(page.scene, "markers_start")
            // L'icona, il titolo, 1. Uno, 2. Due, Testo, Toggle, Primo, Secondo.
            assertEquals("righe: $rows", 8, rows.size)
            click(page.scene, Offset(400f, (rows[4].first + rows[4].last) / 2f))
            key(page.scene, Key.MoveHome)
            key(page.scene, Key.Enter)
            frames(page.scene, 10)
            waitFor("una riga in più: ${top().map { it.type to text(it) }}") { top().size == 5 }
            assertEquals(
                listOf(BlockType.NUMBERED_LIST_ITEM to "Uno", BlockType.NUMBERED_LIST_ITEM to "Due", BlockType.PARAGRAPH to "", BlockType.PARAGRAPH to "Testo"),
                top().take(4).map { it.type to text(it) }
            )
        } finally {
            onWindowThread { page.scene.close() }
        }
    }

    @Test
    fun aNumberAndADotBeforeTextMakeTheItem() {
        val page = page()
        try {
            frames(page.scene, 40)
            val rows = bands(page.scene, "markers_shortcut")
            click(page.scene, Offset(400f, (rows[4].first + rows[4].last) / 2f))
            key(page.scene, Key.MoveHome)
            type(page.scene, '7')
            type(page.scene, '.')
            frames(page.scene, 10)
            waitFor("la riga è una voce: ${top().map { it.type to text(it) }}") { top()[2].type == BlockType.NUMBERED_LIST_ITEM }
            assertEquals("Testo", text(top()[2]))
            assertEquals(7, top()[2].numberStartsAt)
        } finally {
            onWindowThread { page.scene.close() }
        }
    }

    @Test
    fun theChosenSymbolGoesToTheWholeList() {
        val page = page()
        try {
            frames(page.scene, 40)
            onWindowThread { page.viewModel.setListMarker("n1", 2) }
            onWindowThread { page.viewModel.setListMarker("b2", 3) }
            waitFor("il segno scelto su tutto l'elenco") {
                val byId = blocks().associateBy { it.id }
                byId["n1"]?.listMarker == 2 && byId["n2"]?.listMarker == 2 && byId["b1"]?.listMarker == 3 && byId["b2"]?.listMarker == 3
            }
            assertEquals("il paragrafo resta com'è", null, blocks().single { it.id == "p" }.listMarker)
            frames(page.scene, 10)
            bands(page.scene, "markers_chosen")
            // Dentro il toggle i pallini sono testi a sé: si leggono.
            val shown = onWindowThread {
                page.scene.semanticsOwners.flatMap { it.getAllSemanticsNodes(mergingEnabled = false) }
                    .mapNotNull { node -> node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } }
            }
            assertEquals("i quadratini vuoti nel toggle: $shown", 2, shown.count { it == "▫" })
            // Invio in fondo a "Due": la voce nuova ha lo stesso segno.
            val rows = bands(page.scene, "markers_chosen_rows")
            click(page.scene, Offset(400f, (rows[3].first + rows[3].last) / 2f))
            key(page.scene, Key.MoveEnd)
            key(page.scene, Key.Enter)
            frames(page.scene, 10)
            waitFor("la voce nuova: ${top().map { it.type to it.listMarker }}") { top().size == 5 && top()[2].type == BlockType.NUMBERED_LIST_ITEM }
            assertEquals(2, top()[2].listMarker)

            // Il tasto destro sul numero della prima voce apre il menu coi segni.
            val marker = Offset(58f, (rows[2].first + rows[2].last) / 2f)
            onWindowThread {
                page.scene.sendPointerEvent(PointerEventType.Move, marker, type = PointerType.Mouse)
                page.scene.sendPointerEvent(PointerEventType.Press, marker, type = PointerType.Mouse, buttons = PointerButtons(isSecondaryPressed = true), button = PointerButton.Secondary)
                page.scene.sendPointerEvent(PointerEventType.Release, marker, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Secondary)
            }
            frames(page.scene, 15)
            bands(page.scene, "markers_menu")
            val menu = onWindowThread {
                page.scene.semanticsOwners.flatMap { it.getAllSemanticsNodes(mergingEnabled = false) }
                    .mapNotNull { node -> node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } }
            }
            assertTrue("il menu coi segni: $menu", "Automatic" in menu && "i)" in menu && "First." in menu)
        } finally {
            onWindowThread { page.scene.close() }
        }
    }
}
