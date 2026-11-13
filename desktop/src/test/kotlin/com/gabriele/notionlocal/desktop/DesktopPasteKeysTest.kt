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
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.text.AnnotatedString
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
import com.gabriele.notionlocal.ui.screen.LinkClipboard
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Frecce e Backspace dopo un incolla sul PC** (13/11/2026, trovato
 * dall'utente: "ho copiato del testo da una parte della pagina ad un'altra e
 * non posso né andare sopra o sotto, né a destra o sinistra, non posso
 * neanche usare backspace per sistemare la lista numerata"). Degli appunti
 * finti: quelli di Windows restano come sono.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopPasteKeysTest {
    private val dir: File = Files.createTempDirectory("pageflow-pastekeys").toFile()
    private val context = DesktopContext(dir)
    private lateinit var db: AppDatabase
    private val system = java.awt.datatransfer.Clipboard("prova")

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    private val lines = listOf(
        Triple("p1", BlockType.PARAGRAPH, "Prima riga"),
        Triple("p2", BlockType.PARAGRAPH, "Seconda riga"),
        Triple("n1", BlockType.NUMBERED_LIST_ITEM, "Uno"),
        Triple("n2", BlockType.NUMBERED_LIST_ITEM, "Due"),
        Triple("n3", BlockType.NUMBERED_LIST_ITEM, "Tre"),
        Triple("p3", BlockType.PARAGRAPH, "Ultima riga")
    )

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        db = AppDatabase.getInstance(context)
        val repo = PageRepository(db)
        repo.createPage(PageEntity(id = "incolla", title = "Incolla"))
        lines.forEachIndexed { i, (id, type, text) ->
            repo.saveBlock(BlockEntity(id = id, pageId = "incolla", type = type, textJson = """[{"text":"$text"}]""", orderIndex = i))
        }
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun page(): List<Pair<BlockType, String>> = runBlocking {
        db.blockDao().getBlocksForPageOnce("incolla").sortedBy { it.orderIndex }.map { block ->
            block.type to Regex("\"text\":\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(block.textJson).joinToString("") { it.groupValues[1] }
        }
    }

    private fun typed(scene: ImageComposeScene, char: Char) {
        val awt = java.awt.event.KeyEvent(java.awt.Canvas(), java.awt.event.KeyEvent.KEY_TYPED, 0L, 0, java.awt.event.KeyEvent.VK_UNDEFINED, char)
        scene.sendKeyEvent(KeyEvent(Key.Unknown, KeyEventType.Unknown, codePoint = char.code, nativeEvent = awt))
    }

    @Test
    fun afterAPasteTheKeysStillWork() {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        val base = object : ClipboardManager {
            override fun getText(): AnnotatedString? = null
            override fun setText(annotatedString: AnnotatedString) = Unit
        }
        val compose = object : Clipboard {
            override val nativeClipboard: NativeClipboard get() = system
            override suspend fun getClipEntry(): ClipEntry? = null
            override suspend fun setClipEntry(clipEntry: ClipEntry?) = Unit
        }
        val clipboard = LinkClipboard(base, compose, system = system)
        fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 900, density = Density(1f)) {
                CompositionLocalProvider(
                    LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner,
                    LocalClipboardManager provides clipboard, LocalClipboard provides clipboard
                ) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            PageEditorScreen(
                                pageId = "incolla", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
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
        fun key(k: Key, ctrl: Boolean = false, shift: Boolean = false) {
            onWindowThread {
                scene.sendKeyEvent(KeyEvent(k, KeyEventType.KeyDown, isCtrlPressed = ctrl, isShiftPressed = shift))
                scene.sendKeyEvent(KeyEvent(k, KeyEventType.KeyUp, isCtrlPressed = ctrl, isShiftPressed = shift))
            }
            frames(4)
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
            val clean = shot("pastekeys_page")
            fun bright(c: Int) = ((c shr 16) and 0xFF) > 140 && ((c shr 8) and 0xFF) > 140 && (c and 0xFF) > 140
            val rows = (170 until 700).filter { y -> (60 until 400).any { x -> bright(clean.getColor(x, y)) } }
            val bands = ArrayList<IntRange>()
            for (y in rows) {
                val last = bands.lastOrNull()
                if (last != null && y <= last.last + 1) bands[bands.lastIndex] = last.first..y else bands += y..y
            }
            bands.removeAll { it.last - it.first < 5 }
            fun center(i: Int) = (bands[i].first + bands[i].last) / 2f
            fun blockWith(letter: Char) = page().indexOfFirst { letter in it.second }
            // Senza incollare: in fondo a "Seconda riga" (lunga), giù: nella riga sotto,
            // "1. Uno" (più corta, coi numeri davanti), non in quella dopo.
            click(Offset(400f, center(1)))
            key(Key.MoveEnd)
            key(Key.DirectionDown)
            onWindowThread { typed(scene, 'Q') }
            frames(6)
            assertEquals("giù da \"Seconda riga\": ${page()}", 2, blockWith('Q'))
            assertEquals("Uno" + "Q", page()[2].second)
            // Le due prime righe: dall'inizio della prima, Maiusc+giù e Maiusc+Fine, Ctrl+C.
            click(Offset(54f, center(0)))
            key(Key.MoveHome)
            key(Key.DirectionDown, shift = true)
            key(Key.MoveEnd, shift = true)
            key(Key.C, ctrl = true)
            // In fondo a "Tre", a capo (un "4." nuovo) e Ctrl+V.
            click(Offset(400f, center(4)))
            key(Key.MoveEnd)
            key(Key.Enter)
            frames(6)
            key(Key.V, ctrl = true)
            frames(10)
            shot("pastekeys_pasted")
            assertEquals(
                listOf("Prima riga", "Seconda riga", "UnoQ", "Due", "Tre", "Prima riga", "Seconda riga", "Ultima riga"),
                page().map { it.second }
            )
            // Su: nella voce sopra, "4. Prima riga" (prima finiva in "Ultima riga").
            key(Key.DirectionUp)
            onWindowThread { typed(scene, 'Z') }
            frames(6)
            assertEquals("su dopo l'incolla: ${page()}", 5, blockWith('Z'))
            assertEquals("Prima rigaZ", page()[5].second)
            // Giù: di nuovo in "5. Seconda riga".
            key(Key.DirectionDown)
            onWindowThread { typed(scene, 'W') }
            frames(6)
            assertEquals("giù dopo l'incolla: ${page()}", 6, blockWith('W'))
            // Destra e sinistra: una lettera dove si è.
            key(Key.MoveEnd)
            key(Key.DirectionLeft)
            key(Key.DirectionLeft)
            onWindowThread { typed(scene, 'K') }
            frames(6)
            assertTrue("sinistra: ${page()[6]}", page()[6].second.endsWith("K" + page()[6].second.takeLast(2)))
            // A inizio della voce, Backspace: via il numero, il testo resta.
            key(Key.MoveHome)
            key(Key.Backspace)
            frames(6)
            println("PASTEKEYS after home backspace ${page()}")
            assertEquals("Backspace a inizio voce: ${page()}", BlockType.PARAGRAPH, page()[6].first)
            shot("pastekeys_after")
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
