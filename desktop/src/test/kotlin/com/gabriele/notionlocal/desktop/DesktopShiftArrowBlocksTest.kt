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
 * **Maiusc+frecce e trascinamento come in Notion** (17/11/2026, trovato
 * dall'utente: "non posso andare giù o su perché il titolone bugga tutto", poi
 * scelto "come Notion"): il testo si seleziona dentro la sua riga, poi si
 * passa ai blocchi interi, anche fra due righe dello stesso campo. Da "Uno"
 * Maiusc+giù due volte: "Uno" e "Due". Da "Quattro", sotto il titolo,
 * Maiusc+su due volte: "Tre" e "Quattro"; ancora su anche il titolo, giù
 * uno in meno, su di nuovo. Col mouse, da "Uno" a "Due": i due blocchi.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopShiftArrowBlocksTest {
    private val dir: File = Files.createTempDirectory("pageflow-shiftblocks").toFile()
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
        repo.createPage(PageEntity(id = "maiusc", title = "M"))
        val blocks = listOf(
            BlockEntity(id = "uno", pageId = "maiusc", type = BlockType.BULLET_LIST_ITEM, textJson = """[{"text":"Uno"}]"""),
            BlockEntity(id = "due", pageId = "maiusc", type = BlockType.BULLET_LIST_ITEM, textJson = """[{"text":"Due"}]"""),
            BlockEntity(id = "titolo", pageId = "maiusc", type = BlockType.PARAGRAPH, textJson = """[{"text":"Generali","bold":true,"size":29}]"""),
            BlockEntity(id = "tre", pageId = "maiusc", type = BlockType.BULLET_LIST_ITEM, textJson = """[{"text":"Tre"}]"""),
            BlockEntity(id = "quattro", pageId = "maiusc", type = BlockType.BULLET_LIST_ITEM, textJson = """[{"text":"Quattro"}]"""),
            BlockEntity(id = "cinque", pageId = "maiusc", type = BlockType.PARAGRAPH, textJson = """[{"text":"Cinque"}]""")
        )
        blocks.forEachIndexed { i, block -> repo.saveBlock(block.copy(orderIndex = i)) }
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun ids(): List<String> = runBlocking { db.blockDao().getBlocksForPageOnce("maiusc").sortedBy { it.orderIndex }.map { it.id } }

    @Test
    fun shiftArrowsPastABigTitleChooseWholeBlocks() {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 700, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            PageEditorScreen(
                                pageId = "maiusc", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
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
            frames(6)
        }
        fun bright(c: Int) = ((c shr 16) and 0xFF) > 150 && ((c shr 8) and 0xFF) > 150 && (c and 0xFF) > 150
        fun bands(bitmap: Bitmap): List<IntRange> {
            val rows = (60 until bitmap.height).filter { y -> (40 until 800).any { x -> bright(bitmap.getColor(x, y)) } }
            val result = ArrayList<IntRange>()
            for (y in rows) {
                val last = result.lastOrNull()
                if (last != null && y <= last.last + 3) result[result.lastIndex] = last.first..y else result += y..y
            }
            return result
        }
        fun center(band: IntRange) = (band.first + band.last) / 2f
        fun click(y: Float) {
            onWindowThread {
                val at = Offset(400f, y)
                scene.sendPointerEvent(PointerEventType.Move, at, type = PointerType.Mouse)
                scene.sendPointerEvent(PointerEventType.Press, at, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
                scene.sendPointerEvent(PointerEventType.Release, at, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
            }
            frames(6)
        }
        try {
            frames(40)
            // Le ultime sei: "Uno", "Due", il titolo, "Tre", "Quattro", "Cinque".
            val uno = bands(shot("shiftblocks_start")).takeLast(6).first()
            // Da "Uno", nello stesso campo di "Due": prima la fine della riga, poi i blocchi.
            click(center(uno))
            key(Key.MoveHome)
            key(Key.DirectionDown, shift = true)
            key(Key.DirectionDown, shift = true)
            shot("shiftblocks_same_field")
            key(Key.Delete)
            frames(10)
            assertEquals("tolti \"Uno\" e \"Due\"", listOf("titolo", "tre", "quattro", "cinque"), ids())

            // Da "Quattro", sotto il titolo: in cima alla riga, poi "Tre" e "Quattro", poi anche il titolo;
            // giù uno in meno, su di nuovo il titolo.
            val (_, _, quattro, _) = bands(shot("shiftblocks_after_first")).takeLast(4)
            click(center(quattro))
            key(Key.MoveEnd)
            key(Key.DirectionUp, shift = true)
            key(Key.DirectionUp, shift = true)
            key(Key.DirectionUp, shift = true)
            key(Key.DirectionDown, shift = true)
            key(Key.DirectionUp, shift = true)
            shot("shiftblocks_chosen")
            key(Key.Escape)
            frames(6)
            assertEquals("Esc non toglie niente", listOf("titolo", "tre", "quattro", "cinque"), ids())

            // **Le frecce da sole tolgono la scelta** (24/11/2026, dal collaudo): scelti "Tre" e "Quattro", giù
            // rimette il cursore in fondo a "Quattro", e quello che si scrive va lì.
            val (_, _, quattro2, _) = bands(shot("shiftblocks_before_plain")).takeLast(4)
            click(center(quattro2))
            key(Key.MoveEnd)
            key(Key.DirectionUp, shift = true)
            key(Key.DirectionUp, shift = true)
            key(Key.DirectionDown)
            frames(6)
            onWindowThread {
                val awt = java.awt.event.KeyEvent(java.awt.Canvas(), java.awt.event.KeyEvent.KEY_TYPED, 0L, 0, java.awt.event.KeyEvent.VK_UNDEFINED, 'x')
                scene.sendKeyEvent(KeyEvent(Key.Unknown, KeyEventType.Unknown, codePoint = 'x'.code, nativeEvent = awt))
            }
            frames(15)
            shot("shiftblocks_plain_arrow")
            assertEquals("i blocchi ci sono tutti", listOf("titolo", "tre", "quattro", "cinque"), ids())
            assertEquals("la x in fondo a Quattro", true, runBlocking { db.blockDao().getById("quattro") }!!.textJson.contains("Quattrox"))

            // Col mouse, trascinando dal testo di "Tre" a quello di "Quattro" (lo stesso campo): i due blocchi.
            val (_, tre, quattroAgain, _) = bands(shot("shiftblocks_before_mouse")).takeLast(4)
            val from = Offset(80f, center(tre))
            val to = Offset(80f, center(quattroAgain))
            onWindowThread {
                scene.sendPointerEvent(PointerEventType.Move, from, type = PointerType.Mouse)
                scene.sendPointerEvent(PointerEventType.Press, from, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
            }
            frames(3)
            for (step in 1..6) {
                onWindowThread {
                    scene.sendPointerEvent(
                        PointerEventType.Move, Offset(80f, from.y + (to.y - from.y) * step / 6f), type = PointerType.Mouse,
                        buttons = PointerButtons(isPrimaryPressed = true)
                    )
                }
                frames(1)
            }
            onWindowThread { scene.sendPointerEvent(PointerEventType.Release, to, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary) }
            frames(6)
            shot("shiftblocks_mouse")
            key(Key.Delete)
            frames(10)
            assertEquals("tolti \"Tre\" e \"Quattro\"", listOf("titolo", "cinque"), ids())
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
