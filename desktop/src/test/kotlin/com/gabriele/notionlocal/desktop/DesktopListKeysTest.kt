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
import com.gabriele.notionlocal.data.entity.heldListType
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
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Destra, sinistra e Invio intorno agli elenchi, sul PC** (16/11/2026,
 * trovato dall'utente): "sopra un divider premo destra (per andare sotto) e
 * non funziona", lo stesso in fondo a un titolo scritto più grande di 16, una
 * voce numerata sotto quel titolo col cursore prima del numero, e Invio da una
 * riga che tiene il posto dell'elenco che non continuava la lista ("Se voglio
 * rimanere sempre nello stesso blocco ma con una riga diversa, userei sempre
 * SHIFT + Invio").
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopListKeysTest {
    private val dir: File = Files.createTempDirectory("pageflow-listkeys").toFile()
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
        repo.createPage(PageEntity(id = "elenchi", title = "Elenchi"))
        val blocks = listOf(
            BlockEntity(id = "p1", pageId = "elenchi", type = BlockType.PARAGRAPH, textJson = """[{"text":"Sopra divisore"}]"""),
            BlockEntity(id = "d1", pageId = "elenchi", type = BlockType.DIVIDER, textJson = "[]"),
            BlockEntity(id = "p2", pageId = "elenchi", type = BlockType.PARAGRAPH, textJson = """[{"text":"Sotto divisore"}]"""),
            // Più grande di 16: sta in un campo suo.
            BlockEntity(id = "t1", pageId = "elenchi", type = BlockType.PARAGRAPH, textJson = """[{"text":"Titolo grande","size":24}]"""),
            BlockEntity(id = "n1", pageId = "elenchi", type = BlockType.NUMBERED_LIST_ITEM, textJson = """[{"text":"Uno"}]"""),
            // La riga che tiene il posto dell'elenco (quella che lascia il Backspace).
            BlockEntity(
                id = "h1", pageId = "elenchi", type = BlockType.PARAGRAPH, textJson = """[{"text":"In colonna"}]""",
                attrsJson = """{"listIndent":"NUMBERED_LIST_ITEM"}"""
            ),
            BlockEntity(id = "n2", pageId = "elenchi", type = BlockType.NUMBERED_LIST_ITEM, textJson = """[{"text":"Due"}]"""),
            BlockEntity(id = "p3", pageId = "elenchi", type = BlockType.PARAGRAPH, textJson = """[{"text":"Fine"}]""")
        )
        blocks.forEachIndexed { i, block -> repo.saveBlock(block.copy(orderIndex = i)) }
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    /** Tipo, testo e (per un paragrafo) l'elenco di cui tiene il posto, riga per riga. */
    private fun page(): List<Triple<BlockType, String, BlockType?>> = runBlocking {
        db.blockDao().getBlocksForPageOnce("elenchi").sortedBy { it.orderIndex }.map { block ->
            Triple(
                block.type,
                Regex("\"text\":\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(block.textJson).joinToString("") { it.groupValues[1] },
                block.heldListType
            )
        }
    }

    private fun texts() = page().map { it.second }

    private fun typed(scene: ImageComposeScene, char: Char) {
        val awt = java.awt.event.KeyEvent(java.awt.Canvas(), java.awt.event.KeyEvent.KEY_TYPED, 0L, 0, java.awt.event.KeyEvent.VK_UNDEFINED, char)
        scene.sendKeyEvent(KeyEvent(Key.Unknown, KeyEventType.Unknown, codePoint = char.code, nativeEvent = awt))
    }

    @Test
    fun keysAroundDividersTitlesAndLists() {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 900, density = Density(1f)) {
                CompositionLocalProvider(
                    LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner
                ) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            PageEditorScreen(
                                pageId = "elenchi", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
                                onNavigateToPage = {}, onNavigateToDatabase = {}, viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
        var time = 0L
        fun frames(count: Int) = repeat(count) {
            onWindowThread { scene.render(time) }
            time += 100_000_000L
            Thread.sleep(30)
        }
        fun shot(name: String): Bitmap = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }.also {
            File("build/desktop-screenshots").mkdirs()
            File("build/desktop-screenshots", "$name.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(it).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        fun key(k: Key, shift: Boolean = false) {
            onWindowThread {
                scene.sendKeyEvent(KeyEvent(k, KeyEventType.KeyDown, isShiftPressed = shift))
                scene.sendKeyEvent(KeyEvent(k, KeyEventType.KeyUp, isShiftPressed = shift))
            }
            frames(4)
        }
        fun type(c: Char) {
            onWindowThread { typed(scene, c) }
            frames(6)
        }
        fun click(at: Offset) {
            onWindowThread {
                scene.sendPointerEvent(PointerEventType.Move, at, type = PointerType.Mouse)
                scene.sendPointerEvent(PointerEventType.Press, at, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
                scene.sendPointerEvent(PointerEventType.Release, at, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
            }
            frames(5)
        }
        try {
            frames(40)
            val clean = shot("listkeys_page")
            fun bright(c: Int) = ((c shr 16) and 0xFF) > 140 && ((c shr 8) and 0xFF) > 140 && (c and 0xFF) > 140
            val rows = (170 until 760).filter { y -> (60 until 400).any { x -> bright(clean.getColor(x, y)) } }
            val bands = ArrayList<IntRange>()
            for (y in rows) {
                val last = bands.lastOrNull()
                if (last != null && y <= last.last + 1) bands[bands.lastIndex] = last.first..y else bands += y..y
            }
            bands.removeAll { it.last - it.first < 5 }
            println("LISTKEYS bande $bands")
            // Le righe scritte: Sopra divisore, Sotto divisore, Titolo grande, 1. Uno, In colonna, 2. Due, Fine.
            assertEquals("righe sullo schermo: $bands", 7, bands.size)
            fun center(i: Int) = (bands[i].first + bands[i].last) / 2f
            fun at(i: Int, home: Boolean) {
                click(Offset(600f, center(i)))
                key(if (home) Key.MoveHome else Key.MoveEnd)
            }

            // 1. Sopra il divisore, in fondo: destra va sotto il divisore.
            at(0, home = false); key(Key.DirectionRight); type('a')
            assertEquals("destra sopra il divisore: ${texts()}", "aSotto divisore", texts()[2])
            // 2. Sotto il divisore, all'inizio: sinistra va sopra.
            at(1, home = true); key(Key.DirectionLeft); type('b')
            assertEquals("sinistra sotto il divisore: ${texts()}", "Sopra divisoreb", texts()[0])
            // 3. In fondo al titolo grande: destra va nella voce sotto.
            at(2, home = false); key(Key.DirectionRight); type('c')
            assertEquals("destra in fondo al titolo: ${texts()}", "cUno", texts()[4])
            // 4. All'inizio della voce sotto il titolo: sinistra sale (e non resta prima del numero).
            at(3, home = true); key(Key.DirectionLeft); type('d')
            assertEquals("sinistra sotto il titolo: ${texts()}", "Titolo granded", texts()[3])
            // Due volte a sinistra dall'inizio della voce: una sale, la seconda va avanti nel titolo.
            at(3, home = true); key(Key.DirectionLeft); key(Key.DirectionLeft); type('e')
            assertEquals("due volte sinistra: ${texts()}", "Titolo grandeed", texts()[3])
            // 5. In fondo alla riga che tiene il posto dell'elenco, Invio: una voce numerata nuova.
            at(4, home = false); key(Key.Enter); type('f')
            frames(6)
            val afterEnter = page()
            assertEquals("Invio dalla riga in colonna: $afterEnter", Triple(BlockType.NUMBERED_LIST_ITEM, "f", null), afterEnter[6])
            assertEquals("la riga in colonna resta: $afterEnter", Triple(BlockType.PARAGRAPH, "In colonna", BlockType.NUMBERED_LIST_ITEM), afterEnter[5])
            // 6. In fondo a "1. cUno", Maiusc+Invio: una riga che tiene il posto dell'elenco, senza numero.
            at(3, home = false); key(Key.Enter, shift = true); type('g')
            frames(6)
            val afterShift = page()
            assertEquals("Maiusc+Invio: $afterShift", Triple(BlockType.PARAGRAPH, "g", BlockType.NUMBERED_LIST_ITEM), afterShift[5])
            assertEquals("la voce resta: $afterShift", Triple(BlockType.NUMBERED_LIST_ITEM, "cUno", null), afterShift[4])
            // 7. Su un paragrafo qualunque Maiusc+Invio è un Invio.
            at(0, home = false); key(Key.Enter, shift = true); type('h')
            frames(6)
            assertEquals("Maiusc+Invio su un paragrafo: ${page()}", Triple(BlockType.PARAGRAPH, "h", null), page()[1])
            shot("listkeys_end")
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
