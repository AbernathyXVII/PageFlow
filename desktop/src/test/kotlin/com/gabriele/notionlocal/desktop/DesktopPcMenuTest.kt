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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Il menu del tasto destro sul PC** (dal 13/11/2026, chiesto dall'utente):
 * cliccando col tasto destro su una riga si apre, in colonna, quello che sul
 * telefono sta nella barra sopra la tastiera. Il clic destro mette prima il
 * cursore dove si clicca (fuori dal testo selezionato), come in Windows.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopPcMenuTest {
    private val dir: File = Files.createTempDirectory("pageflow-pcmenu").toFile()
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
        repo.createPage(PageEntity(id = "menu", title = "Menu"))
        listOf("prima" to "PRIMA RIGA", "seconda" to "SECONDA RIGA", "terza" to "TERZA RIGA").forEachIndexed { i, (id, text) ->
            repo.saveBlock(BlockEntity(id = id, pageId = "menu", textJson = """[{"text":"$text"}]""", orderIndex = i))
        }
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun lines() = runBlocking { db.blockDao().getBlocksForPageOnce("menu") }.filter { it.parentBlockId == null }.sortedBy { it.orderIndex }

    private class Page(
        val frames: (Int) -> Unit,
        val shot: (String) -> Bitmap,
        val mouse: (PointerEventType, Offset, PointerButton?, PointerButtons) -> Unit,
        val key: (Key, Boolean) -> Unit
    ) {
        fun move(x: Float, y: Float) {
            mouse(PointerEventType.Move, Offset(x, y), null, PointerButtons())
            frames(1)
        }

        fun click(x: Float, y: Float, right: Boolean = false) {
            move(x, y)
            val button = if (right) PointerButton.Secondary else PointerButton.Primary
            mouse(PointerEventType.Press, Offset(x, y), button, if (right) PointerButtons(isSecondaryPressed = true) else PointerButtons(isPrimaryPressed = true))
            frames(1)
            mouse(PointerEventType.Release, Offset(x, y), button, PointerButtons())
            frames(5)
        }
    }

    private fun withPage(act: Page.() -> Unit) {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 1100, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            PageEditorScreen(
                                pageId = "menu", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
                                onNavigateToPage = {}, onNavigateToDatabase = {}, viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
        var time = 0L
        val frames: (Int) -> Unit = { count ->
            repeat(count) {
                onWindowThread { scene.render(time) }
                time += 100_000_000L
                Thread.sleep(30)
            }
        }
        val shot: (String) -> Bitmap = { name ->
            onWindowThread { Bitmap.makeFromImage(scene.render(time)) }.also {
                File("build/desktop-screenshots").mkdirs()
                File("build/desktop-screenshots", "$name.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(it).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
        }
        val mouse: (PointerEventType, Offset, PointerButton?, PointerButtons) -> Unit = { type, at, button, buttons ->
            onWindowThread { scene.sendPointerEvent(type, at, type = PointerType.Mouse, buttons = buttons, button = button) }
        }
        val key: (Key, Boolean) -> Unit = { k, shift ->
            onWindowThread {
                scene.sendKeyEvent(KeyEvent(k, KeyEventType.KeyDown, isShiftPressed = shift))
                scene.sendKeyEvent(KeyEvent(k, KeyEventType.KeyUp, isShiftPressed = shift))
            }
            frames(3)
        }
        try {
            frames(40)
            Page(frames, shot, mouse, key).act()
        } finally {
            onWindowThread { scene.close() }
        }
    }

    /** Le altezze delle righe di testo della pagina, sotto il titolo: dove cliccare. */
    private fun textLines(bitmap: Bitmap): List<Int> {
        val rows = (150 until 400).filter { y -> (60 until 400).any { x -> bitmap.getColor(x, y).let { c -> ((c shr 16) and 0xFF) > 150 && ((c shr 8) and 0xFF) > 150 && (c and 0xFF) > 150 } } }
        val bands = ArrayList<IntRange>()
        for (y in rows) {
            val last = bands.lastOrNull()
            if (last != null && y <= last.last + 1) bands[bands.lastIndex] = last.first..y else bands += y..y
        }
        return bands.map { (it.first + it.last) / 2 }
    }

    /** Dove comincia la prima voce del menu aperto col clic in ([x], [y]): sotto il bordo e il margine di sopra. */
    private fun firstRow(bitmap: Bitmap, x: Int, y: Int): Int {
        val inside = (y - 4 until y + 60).firstOrNull { bitmap.getColor(x + 80, it) == MENU_BACKGROUND }
            ?: error("il menu non si è aperto sotto il clic in ($x, $y)")
        return inside + 7
    }

    /**
     * **Il clic destro su una riga senza cursore, e "Move up"**: il cursore va
     * sulla riga cliccata e la voce agisce su quella. Prima, sul PC, il clic
     * destro apriva il menu di Compose (Cut, Copy, Paste) e basta.
     */
    @Test
    fun rightClickOnALineAndMoveItUp() = withPage {
        val y = textLines(shot("pcmenu_page"))
        assertTrue("tre righe e il titolo: $y", y.size >= 3)
        val terza = y.last()
        click(110f, terza.toFloat(), right = true)
        frames(5)
        val menu = shot("pcmenu_open")
        val top = firstRow(menu, 110, terza)
        // Block options, una riga, quattro voci (copia…), una riga, Outdent, Indent, Move up.
        val moveUp = top + 30 + 9 + 4 * 30 + 9 + 2 * 30 + 15
        click(190f, moveUp.toFloat())
        frames(10)
        assertEquals(listOf("prima", "terza", "seconda"), lines().map { it.id })
    }

    /**
     * **Testo selezionato, clic destro dentro, Aa e Bold**: la selezione resta
     * (il clic dentro non sposta il cursore) e diventa grassetto. Aa si apre
     * di lato passandoci sopra, come i sottomenu di Windows.
     */
    @Test
    fun boldFromTheTextFormattingSubmenu() = withPage {
        val y = textLines(shot("pcmenu_page2"))
        val seconda = y[y.size - 2]
        click(110f, seconda.toFloat())
        key(Key.MoveHome, false)
        key(Key.MoveEnd, true)
        click(110f, seconda.toFloat(), right = true)
        frames(5)
        val menu = shot("pcmenu_open2")
        val top = firstRow(menu, 110, seconda)
        val aaTop = top + 11 * 30 + 3 * 9
        move(190f, aaTop + 15f)
        frames(5)
        val open = shot("pcmenu_aa")
        // Il bordo destro del menu, alla riga sotto Aa: lì comincia il sottomenu.
        val probe = aaTop + 45
        val right = (200 until open.width).first { open.getColor(it, probe) == MENU_BORDER }
        // Font, Font size, una riga, Bold.
        val bold = aaTop + 60 + 9 + 15
        move(right + 60f, aaTop + 15f)
        move(right + 60f, bold.toFloat())
        shot("pcmenu_bold")
        click(right + 60f, bold.toFloat())
        frames(10)
        val text = lines().single { it.id == "seconda" }.textJson
        assertTrue("SECONDA RIGA in grassetto: $text", text.contains("bold", ignoreCase = true))
    }

    private companion object {
        val MENU_BACKGROUND = 0xFF2C2C2C.toInt()
        val MENU_BORDER = 0xFF454545.toInt()
    }
}
