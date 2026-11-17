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
 * **Le righe vuote in fondo alla pagina si raggiungono** (17/11/2026, trovato
 * dall'utente: "lì non posso usare la freccetta destra per andare giù", "non
 * mi permette proprio di selezionare l'ultimo blocco scrivibile", e Canc le
 * toglieva senza che si potessero scegliere): sotto due righe di testo quattro
 * righe vuote; con la freccia destra il cursore scende di riga in riga, e un
 * clic sull'ultima resta lì.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopTrailingBlankTest {
    private val dir: File = Files.createTempDirectory("pageflow-blank").toFile()
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
        repo.createPage(PageEntity(id = "vuote", title = "V"))
        val texts = listOf("rrrrrrrrrr", "ssssssssssssssssss", "", "", "", "")
        texts.forEachIndexed { i, text ->
            repo.saveBlock(BlockEntity(id = "l$i", pageId = "vuote", type = BlockType.PARAGRAPH, textJson = """[{"text":"$text"}]""", orderIndex = i))
        }
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun texts(): List<String> = runBlocking {
        db.blockDao().getBlocksForPageOnce("vuote").sortedBy { it.orderIndex }.map { block ->
            Regex("\"text\":\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(block.textJson).joinToString("") { it.groupValues[1] }
        }
    }

    private fun typed(scene: ImageComposeScene, char: Char) {
        val awt = java.awt.event.KeyEvent(java.awt.Canvas(), java.awt.event.KeyEvent.KEY_TYPED, 0L, 0, java.awt.event.KeyEvent.VK_UNDEFINED, char)
        scene.sendKeyEvent(KeyEvent(Key.Unknown, KeyEventType.Unknown, codePoint = char.code, nativeEvent = awt))
    }

    @Test
    fun theEmptyLinesAtTheBottomAreReachable() {
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
                                pageId = "vuote", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
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
            frames(6)
        }
        fun type(c: Char) {
            onWindowThread { typed(scene, c) }
            frames(10)
        }
        fun click(at: Offset) {
            onWindowThread {
                scene.sendPointerEvent(PointerEventType.Move, at, type = PointerType.Mouse)
                scene.sendPointerEvent(PointerEventType.Press, at, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
                scene.sendPointerEvent(PointerEventType.Release, at, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
            }
            frames(6)
        }
        fun bright(c: Int) = ((c shr 16) and 0xFF) > 150 && ((c shr 8) and 0xFF) > 150 && (c and 0xFF) > 150
        try {
            frames(40)
            val start = shot("blank_start")
            // Le due righe di testo (le più basse con dell'inchiostro): da loro il passo fra una riga e l'altra.
            val rows = (60 until start.height).filter { y -> (40 until 800).any { x -> bright(start.getColor(x, y)) } }
            val bands = ArrayList<IntRange>()
            for (y in rows) {
                val last = bands.lastOrNull()
                if (last != null && y <= last.last + 3) bands[bands.lastIndex] = last.first..y else bands += y..y
            }
            val (r, s) = bands.takeLast(2)
            fun center(band: IntRange) = (band.first + band.last) / 2f
            val pitch = center(s) - center(r)

            // In fondo alle "s", due volte destra: il cursore scende fino alla seconda riga vuota.
            click(Offset(600f, center(s)))
            key(Key.MoveEnd)
            key(Key.DirectionRight)
            key(Key.DirectionRight)
            type('Q')
            assertEquals("la Q nella seconda riga vuota: ${texts()}", listOf("rrrrrrrrrr", "ssssssssssssssssss", "", "Q", "", ""), texts())

            // Un clic sull'ultima riga vuota: il cursore resta lì.
            click(Offset(300f, center(s) + pitch * 4))
            type('Z')
            assertEquals("la Z nell'ultima riga: ${texts()}", listOf("rrrrrrrrrr", "ssssssssssssssssss", "", "Q", "", "Z"), texts())
            shot("blank_after")
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
