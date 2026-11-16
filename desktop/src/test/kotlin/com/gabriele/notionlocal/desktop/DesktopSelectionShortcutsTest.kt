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
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
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
import com.gabriele.notionlocal.ui.navigation.LocalShortcuts
import com.gabriele.notionlocal.ui.navigation.Shortcuts
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.ui.theme.ctrlWheelZoom
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Le scorciatoie del testo selezionato sul PC** (16/11/2026, chiesto
 * dall'utente): Ctrl+B, Ctrl+I, Ctrl+U, Ctrl+S, Ctrl+Maiusc+S, e il corpo con
 * Ctrl + e Ctrl − o con la rotella e Ctrl, solo con del testo selezionato;
 * senza, Ctrl + resta lo zoom. La finestra è fatta come quella vera:
 * `Shortcuts.windowKey` prima di tutti, e `ctrlWheelZoom` intorno a tutto.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopSelectionShortcutsTest {
    private val dir: File = Files.createTempDirectory("pageflow-shortcuts").toFile()
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
        AppSettings.zoom = 100
        db = AppDatabase.getInstance(context)
        val repo = PageRepository(db)
        repo.createPage(PageEntity(id = "stili", title = "Stili"))
        listOf("Prima riga", "Parole da cambiare", "Ultima riga").forEachIndexed { i, text ->
            repo.saveBlock(BlockEntity(id = "r$i", pageId = "stili", type = BlockType.PARAGRAPH, textJson = """[{"text":"$text"}]""", orderIndex = i))
        }
    }

    @After
    fun tearDown() {
        AppSettings.zoom = 100
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun json(id: String): String = runBlocking { db.blockDao().getById(id)!!.textJson }

    @Test
    fun stylesAndSizeOnTheSelectedText() {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        val shortcuts = Shortcuts()
        fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 700, density = Density(1f)) {
                CompositionLocalProvider(
                    LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner,
                    LocalShortcuts provides shortcuts
                ) {
                    NotionLocalTheme {
                        Box(
                            Modifier.fillMaxSize()
                                .onPreviewKeyEvent { shortcuts.windowKey(it) }
                                .ctrlWheelZoom { bigger -> shortcuts.resizeSelection(bigger) }
                        ) {
                            PageEditorScreen(
                                pageId = "stili", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
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
        fun wheel(at: Offset, up: Boolean) {
            onWindowThread {
                scene.sendPointerEvent(
                    PointerEventType.Scroll, at, scrollDelta = Offset(0f, if (up) -1f else 1f), type = PointerType.Mouse,
                    keyboardModifiers = PointerKeyboardModifiers(isCtrlPressed = true)
                )
            }
            frames(6)
        }
        fun bright(c: Int) = ((c shr 16) and 0xFF) > 140 && ((c shr 8) and 0xFF) > 140 && (c and 0xFF) > 140
        try {
            frames(40)
            val clean = shot("shortcuts_page")
            val rows = (170 until 600).filter { y -> (60 until 400).any { x -> bright(clean.getColor(x, y)) } }
            val bands = ArrayList<IntRange>()
            for (y in rows) {
                val last = bands.lastOrNull()
                if (last != null && y <= last.last + 1) bands[bands.lastIndex] = last.first..y else bands += y..y
            }
            bands.removeAll { it.last - it.first < 4 }
            assertEquals("righe: $bands", 3, bands.size)
            val middle = Offset(400f, (bands[1].first + bands[1].last) / 2f)

            // Senza testo selezionato: Ctrl+B non fa niente, Ctrl + è lo zoom.
            click(middle)
            key(Key.MoveEnd)
            key(Key.B, ctrl = true)
            assertFalse("senza selezione niente grassetto: ${json("r1")}", "\"bold\":true" in json("r1"))
            key(Key.Equals, ctrl = true)
            assertEquals("senza selezione Ctrl + è lo zoom", 110, AppSettings.zoom)
            key(Key.Minus, ctrl = true)
            assertEquals(100, AppSettings.zoom)

            // La riga selezionata: Home, Maiusc+Fine.
            key(Key.MoveHome)
            key(Key.MoveEnd, shift = true)
            key(Key.B, ctrl = true)
            key(Key.I, ctrl = true)
            key(Key.U, ctrl = true)
            key(Key.S, ctrl = true)
            key(Key.S, ctrl = true, shift = true)
            frames(6)
            val styled = json("r1")
            println("SHORTCUTS stili $styled")
            for (style in listOf("bold", "italic", "underline", "strikethrough", "spoiler")) {
                assertTrue("$style: $styled", "\"$style\":true" in styled)
            }
            // Il corpo: Ctrl + due volte (16 → 18 → 20), Ctrl − una (18); lo zoom non cambia.
            key(Key.Equals, ctrl = true)
            frames(6)
            key(Key.Equals, ctrl = true)
            frames(6)
            assertTrue("Ctrl + due volte: ${json("r1")}", "\"size\":20" in json("r1"))
            key(Key.Minus, ctrl = true)
            frames(6)
            assertTrue("Ctrl −: ${json("r1")}", "\"size\":18" in json("r1"))
            assertEquals("con la selezione lo zoom resta", 100, AppSettings.zoom)
            // La rotella con Ctrl: in su ingrandisce (18 → 20).
            wheel(middle, up = true)
            frames(6)
            println("SHORTCUTS rotella ${json("r1")}")
            assertTrue("rotella in su: ${json("r1")}", "\"size\":20" in json("r1"))
            assertEquals("con la selezione lo zoom resta anche con la rotella", 100, AppSettings.zoom)
            // Le altre righe non sono state toccate.
            assertEquals("""[{"text":"Prima riga"}]""", json("r0"))
            assertEquals("""[{"text":"Ultima riga"}]""", json("r2"))
            shot("shortcuts_end")
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
