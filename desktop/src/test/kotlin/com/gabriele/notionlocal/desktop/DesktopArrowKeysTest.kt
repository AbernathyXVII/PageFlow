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
 * **Le frecce su e giù sul PC** (dal 13/11/2026, trovato dall'utente: "certe
 * volte mi salta di due blocchi, certe volte non si muove proprio"): ogni
 * freccia in giù porta il cursore alla riga sotto, qualunque blocco sia, e
 * ogni freccia in su a quella sopra.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopArrowKeysTest {
    private val dir: File = Files.createTempDirectory("pageflow-arrows").toFile()
    private val context = DesktopContext(dir)
    private lateinit var db: AppDatabase

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    /** Le righe della pagina, nell'ordine: id, tipo, testo. */
    private val lines = listOf(
        Triple("p1", BlockType.PARAGRAPH, "Prima riga"),
        Triple("p2", BlockType.PARAGRAPH, "Seconda riga"),
        Triple("b1", BlockType.BULLET_LIST_ITEM, "Punto uno"),
        Triple("c1", BlockType.CHECKBOX, "Casella uno"),
        Triple("q1", BlockType.QUOTE, "Citazione qui"),
        Triple("p3", BlockType.PARAGRAPH, "Terza riga"),
        Triple("t1", BlockType.TOGGLE, "Interruttore"),
        Triple("p4", BlockType.PARAGRAPH, "Quarta riga"),
        Triple("p5", BlockType.PARAGRAPH, "Quinta riga")
    )

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        db = AppDatabase.getInstance(context)
        val repo = PageRepository(db)
        repo.createPage(PageEntity(id = "frecce", title = "Frecce"))
        lines.forEachIndexed { i, (id, type, text) ->
            repo.saveBlock(BlockEntity(id = id, pageId = "frecce", type = type, textJson = """[{"text":"$text"}]""", orderIndex = i))
        }
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun textOf(id: String) = runBlocking { db.blockDao().getById(id)!!.textJson }

    /** Un tasto battuto, come lo manda la finestra di Windows. */
    private fun typed(scene: ImageComposeScene, char: Char) {
        val awt = java.awt.event.KeyEvent(java.awt.Canvas(), java.awt.event.KeyEvent.KEY_TYPED, 0L, 0, java.awt.event.KeyEvent.VK_UNDEFINED, char)
        scene.sendKeyEvent(KeyEvent(Key.Unknown, KeyEventType.Unknown, codePoint = char.code, nativeEvent = awt))
    }

    @Test
    fun everyArrowDownGoesToTheNextLine() {
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
                                pageId = "frecce", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
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
        fun key(k: Key) {
            onWindowThread {
                scene.sendKeyEvent(KeyEvent(k, KeyEventType.KeyDown))
                scene.sendKeyEvent(KeyEvent(k, KeyEventType.KeyUp))
            }
            frames(4)
        }
        try {
            frames(40)
            val page = shot("arrows_page")
            fun bright(c: Int) = ((c shr 16) and 0xFF) > 140 && ((c shr 8) and 0xFF) > 140 && (c and 0xFF) > 140
            val rows = (170 until 700).filter { y -> (60 until 400).any { x -> bright(page.getColor(x, y)) } }
            val bands = ArrayList<IntRange>()
            for (y in rows) {
                val last = bands.lastOrNull()
                if (last != null && y <= last.last + 1) bands[bands.lastIndex] = last.first..y else bands += y..y
            }
            // Un clic sulla prima riga, dopo "Pri".
            val first = bands.first()
            val at = Offset(80f, (first.first + first.last) / 2f)
            onWindowThread {
                scene.sendPointerEvent(PointerEventType.Move, at, type = PointerType.Mouse)
                scene.sendPointerEvent(PointerEventType.Press, at, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
                scene.sendPointerEvent(PointerEventType.Release, at, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
            }
            frames(5)
            // Giù, una cifra; giù, la cifra dopo… Ogni cifra deve finire nella riga dopo.
            val landed = ArrayList<String>()
            for (step in 1 until lines.size) {
                key(Key.DirectionDown)
                onWindowThread { typed(scene, ('0' + step)) }
                frames(6)
                val where = lines.firstOrNull { (id, _, text) -> textOf(id).contains(('0' + step).toString()) }?.first ?: "nessuna"
                landed += where
            }
            shot("arrows_after")
            assertEquals("dove è finita ogni cifra: $landed", lines.drop(1).map { it.first }, landed)
            // E in su, dall'ultima alla prima: ogni lettera nella riga sopra.
            val upward = ArrayList<String>()
            for (step in 1 until lines.size) {
                key(Key.DirectionUp)
                // Lettere che nei testi della pagina non ci sono.
                val letter = "XYZWKJVB"[step - 1]
                onWindowThread { typed(scene, letter) }
                frames(6)
                upward += lines.firstOrNull { (id, _, _) -> textOf(id).contains(letter.toString()) }?.first ?: "nessuna"
            }
            shot("arrows_up")
            assertEquals("dove è finita ogni lettera: $upward", lines.dropLast(1).map { it.first }.reversed(), upward)
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
