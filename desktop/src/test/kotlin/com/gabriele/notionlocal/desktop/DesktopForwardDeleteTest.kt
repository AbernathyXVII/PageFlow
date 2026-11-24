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
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Canc in fondo a una riga** (24/11/2026, trovato dall'utente sul PC): sulla riga vuota sotto una
 * citazione Canc non faceva niente; deve portare su quello che c'è sotto.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopForwardDeleteTest {
    private val dir: File = Files.createTempDirectory("pageflow-fwddelete").toFile()
    private val context = DesktopContext(dir)
    private lateinit var db: AppDatabase

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun texts(): List<String> = runBlocking {
        db.blockDao().getBlocksForPageOnce("prova").sortedBy { it.orderIndex }.map { b ->
            b.type.name.take(5) + ":" + Regex("\"text\":\"([^\"]*)\"").findAll(b.textJson).joinToString("") { it.groupValues[1] }
        }
    }

    private fun item(id: String, type: BlockType, text: String, size: Int? = null) =
        BlockEntity(id = id, pageId = "prova", type = type, textJson = if (size == null) """[{"text":"$text"}]""" else """[{"text":"$text","size":$size}]""")

    private fun run(name: String, blocks: List<BlockEntity>, body: (scene: ImageComposeScene, frames: (Int) -> Unit, shot: (String) -> Bitmap) -> Unit) {
        AppSettings.init(context)
        db = AppDatabase.getInstance(context)
        runBlocking {
            val repo = PageRepository(db)
            repo.createPage(PageEntity(id = "prova", title = "P"))
            blocks.forEachIndexed { i, block -> repo.saveBlock(block.copy(pageId = "prova", orderIndex = i)) }
        }
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        val scene = runBlocking(Dispatchers.Main) {
            ImageComposeScene(width = 900, height = 600, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            PageEditorScreen(
                                pageId = "prova", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
                                onNavigateToPage = {}, onNavigateToDatabase = {}, viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
        var time = 0L
        fun frames(count: Int) = repeat(count) {
            runBlocking(Dispatchers.Main) { scene.render(time) }
            time += 16_000_000L
            Thread.sleep(30)
        }
        fun shot(n: String) = runBlocking(Dispatchers.Main) { Bitmap.makeFromImage(scene.render(time)) }.also {
            File("build/desktop-screenshots", "fwddelete_${name}_$n.png").apply { parentFile.mkdirs() }
                .writeBytes(org.jetbrains.skia.Image.makeFromBitmap(it).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        try {
            frames(40)
            body(scene, ::frames, ::shot)
        } finally {
            runBlocking(Dispatchers.Main) { scene.close() }
            AppDatabase.closeInstance()
        }
    }

    /** Le fasce di testo chiaro sotto [fromY], dall'alto: dove stanno le righe. */
    private fun bands(bitmap: Bitmap, fromY: Int): List<IntRange> {
        val rows = (fromY until bitmap.height).filter { y -> (45 until 700).any { x -> bitmap.getColor(x, y).let { c -> ((c shr 16) and 0xFF) > 150 && ((c shr 8) and 0xFF) > 150 && (c and 0xFF) > 150 } } }
        val out = ArrayList<IntRange>()
        for (y in rows) {
            val last = out.lastOrNull()
            if (last != null && y <= last.last + 2) out[out.lastIndex] = last.first..y else out += y..y
        }
        return out
    }

    private fun click(scene: ImageComposeScene, at: Offset) = runBlocking(Dispatchers.Main) {
        scene.sendPointerEvent(PointerEventType.Move, at, type = PointerType.Mouse)
        scene.sendPointerEvent(PointerEventType.Press, at, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Release, at, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
    }

    private fun key(scene: ImageComposeScene, k: Key) = runBlocking(Dispatchers.Main) {
        scene.sendKeyEvent(KeyEvent(k, KeyEventType.KeyDown))
        scene.sendKeyEvent(KeyEvent(k, KeyEventType.KeyUp))
    }

    /**
     * Come nella foto dell'utente: citazione scritta più grande, la riga vuota sotto anche lei più grande
     * (quindi in un campo suo, vedi `PageEditorViewModel.lineLookOf`), righe "a". Canc sulla riga vuota la toglie.
     */
    @Test
    fun deleteOnTheEmptyLineUnderAQuote() = run(
        "quote",
        listOf(item("q", BlockType.QUOTE, "When it is dark enough", 20), item("e", BlockType.PARAGRAPH, "", 20)) +
            (1..4).map { item("a$it", BlockType.PARAGRAPH, "a") }
    ) { scene, frames, shot ->
        val before = shot("before")
        val lines = bands(before, 170)
        println("FWDDELETE bands ${lines}")
        // La citazione, poi le "a": la riga vuota sta fra la prima e la seconda fascia.
        val emptyY = (lines[0].last + lines[1].first) / 2f
        click(scene, Offset(400f, emptyY)); frames(10)
        key(scene, Key.Delete); frames(20)
        shot("after")
        assertEquals("la riga vuota se ne va", listOf("QUOTE:When it is dark enough", "PARAG:a", "PARAG:a", "PARAG:a", "PARAG:a"), texts())
    }

    /** In fondo a una riga col testo, sopra una riga scritta più grande (un altro campo), Canc la attacca qui. */
    @Test
    fun deleteAtTheEndOfALinePullsTheNextOneUp() = run(
        "join",
        listOf(item("p", BlockType.PARAGRAPH, "uno"), item("g", BlockType.PARAGRAPH, "due", 20), item("z", BlockType.PARAGRAPH, "tre"))
    ) { scene, frames, shot ->
        val lines = bands(shot("before"), 170)
        println("FWDDELETE join bands ${lines}")
        click(scene, Offset(700f, (lines[0].first + lines[0].last) / 2f)); frames(10)
        key(scene, Key.Delete); frames(20)
        shot("after")
        assertEquals("\"due\" sale in fondo a \"uno\"", listOf("PARAG:unodue", "PARAG:tre"), texts())
    }

    private fun typed(scene: ImageComposeScene, char: Char) = runBlocking(Dispatchers.Main) {
        val awt = java.awt.event.KeyEvent(java.awt.Canvas(), java.awt.event.KeyEvent.KEY_TYPED, 0L, 0, java.awt.event.KeyEvent.VK_UNDEFINED, char)
        scene.sendKeyEvent(KeyEvent(Key.Unknown, KeyEventType.Unknown, codePoint = char.code, nativeEvent = awt))
    }

    /**
     * **Canc sulla riga vuota fra una citazione e un database** (24/11/2026, dal collaudo dell'utente, nel suo
     * menu principale): sotto non c'è una riga dove scrivere, ma la riga vuota se ne va lo stesso e il cursore
     * torna in fondo alla citazione, come col Backspace. Qui al posto del database un divisore.
     */
    @Test
    fun deleteOnTheEmptyLineAboveADatabase() = run(
        "abovedb",
        listOf(item("q", BlockType.QUOTE, "Citazione"), item("e", BlockType.PARAGRAPH, ""), BlockEntity(id = "d", pageId = "prova", type = BlockType.DIVIDER))
    ) { scene, frames, shot ->
        val lines = bands(shot("before"), 170)
        click(scene, Offset(700f, (lines[0].first + lines[0].last) / 2f)); frames(10)
        key(scene, Key.DirectionDown); frames(10)
        key(scene, Key.Delete); frames(20)
        typed(scene, 'x'); frames(15)
        shot("after")
        assertEquals("via la riga vuota, e la x in fondo alla citazione", listOf("QUOTE:Citazionex", "DIVID:"), texts())
    }

    /** **Canc sull'ultima riga, vuota** (24/11/2026, dal collaudo): se ne va e il cursore torna in fondo a quella sopra. */
    @Test
    fun deleteOnTheLastEmptyLine() = run(
        "last",
        listOf(item("p", BlockType.PARAGRAPH, "uno"), item("e", BlockType.PARAGRAPH, "", 20))
    ) { scene, frames, shot ->
        val lines = bands(shot("before"), 170)
        click(scene, Offset(700f, (lines[0].first + lines[0].last) / 2f)); frames(10)
        key(scene, Key.DirectionDown); frames(10)
        key(scene, Key.Delete); frames(20)
        typed(scene, 'x'); frames(15)
        shot("after")
        assertEquals("via l'ultima riga, e la x in fondo a \"uno\"", listOf("PARAG:unox"), texts())
    }
}
