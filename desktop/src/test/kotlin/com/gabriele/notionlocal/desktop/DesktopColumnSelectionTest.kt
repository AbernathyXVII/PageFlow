package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
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
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.screen.notePresses
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Le righe dentro le colonne si scelgono col mouse** (trovato dall'utente il 23/11/2026: nei testi
 * delle canzoni in due colonne "non posso selezionare i vari blocchi del testo ... per colorarli, e
 * sembra che anche CTRL + Z funzioni male"; e nei team dei Pokémon in tre colonne). Due colonne:
 * "Uno", "Due", "Tre" a sinistra e "Quattro" a destra.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopColumnSelectionTest {
    private val dir: File = Files.createTempDirectory("pageflow-colsel").toFile()
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
        repo.createPage(PageEntity(id = "testo", title = "Testo"))
        repo.saveBlock(BlockEntity(id = "sopra", pageId = "testo", textJson = """[{"text":"SOPRA"}]""", orderIndex = 0))
        repo.saveBlock(BlockEntity(id = "gruppo", pageId = "testo", type = BlockType.COLUMN_LIST, textJson = "[]", orderIndex = 1))
        repo.saveBlock(BlockEntity(id = "sinistra", pageId = "testo", type = BlockType.COLUMN, textJson = "[]", parentBlockId = "gruppo", orderIndex = 0))
        repo.saveBlock(BlockEntity(id = "destra", pageId = "testo", type = BlockType.COLUMN, textJson = "[]", parentBlockId = "gruppo", orderIndex = 1))
        listOf("Uno", "Due", "Tre").forEachIndexed { i, text ->
            repo.saveBlock(BlockEntity(id = text.lowercase(), pageId = "testo", textJson = """[{"text":"$text"}]""", parentBlockId = "sinistra", orderIndex = i))
        }
        repo.saveBlock(BlockEntity(id = "quattro", pageId = "testo", textJson = """[{"text":"Quattro"}]""", parentBlockId = "destra", orderIndex = 0))
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    private fun children(parent: String) = runBlocking { db.blockDao().getBlocksForPageOnce("testo") }.filter { it.parentBlockId == parent }.sortedBy { it.orderIndex }

    private class Page(
        val viewModel: PageEditorViewModel,
        val frames: (Int) -> Unit,
        val picture: () -> Bitmap,
        val line: (String) -> Rect,
        val mouse: (PointerEventType, Offset, Boolean, PointerKeyboardModifiers?) -> Unit,
        val key: (Key, Boolean) -> Unit
    )

    private fun withPage(act: Page.() -> Unit) {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 600, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize().notePresses()) {
                            PageEditorScreen(
                                pageId = "testo", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
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
        val picture: () -> Bitmap = { onWindowThread { Bitmap.makeFromImage(scene.render(time)) } }
        val line: (String) -> Rect = { text ->
            onWindowThread {
                scene.semanticsOwners.flatMap { it.getAllSemanticsNodes(mergingEnabled = false) }
                    .first { node -> node.config.getOrNull(SemanticsProperties.EditableText)?.text?.trim() == text }
                    .boundsInRoot
            }
        }
        val mouse: (PointerEventType, Offset, Boolean, PointerKeyboardModifiers?) -> Unit = { type, at, pressed, modifiers ->
            onWindowThread {
                scene.sendPointerEvent(
                    type, at, type = PointerType.Mouse,
                    buttons = if (pressed) PointerButtons(isPrimaryPressed = true) else PointerButtons(),
                    keyboardModifiers = modifiers,
                    button = if (type == PointerEventType.Move) null else PointerButton.Primary
                )
            }
        }
        val key: (Key, Boolean) -> Unit = { k, ctrl ->
            onWindowThread {
                scene.sendKeyEvent(KeyEvent(k, KeyEventType.KeyDown, isCtrlPressed = ctrl))
                scene.sendKeyEvent(KeyEvent(k, KeyEventType.KeyUp, isCtrlPressed = ctrl))
            }
            frames(5)
        }
        try {
            frames(40)
            Page(viewModel, frames, picture, line, mouse, key).act()
        } finally {
            onWindowThread { scene.close() }
        }
    }

    private fun Page.drag(from: Offset, to: Offset) {
        mouse(PointerEventType.Move, from, false, null)
        mouse(PointerEventType.Press, from, true, null)
        frames(2)
        for (step in 1..8) {
            mouse(PointerEventType.Move, Offset(from.x + (to.x - from.x) * step / 8f, from.y + (to.y - from.y) * step / 8f), true, null)
            frames(1)
        }
        mouse(PointerEventType.Release, to, false, null)
        frames(5)
    }

    private fun Page.ctrlClick(at: Offset) {
        val ctrl = PointerKeyboardModifiers(isCtrlPressed = true)
        mouse(PointerEventType.Move, at, false, ctrl)
        mouse(PointerEventType.Press, at, true, ctrl)
        mouse(PointerEventType.Release, at, false, ctrl)
        frames(4)
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < end && !condition()) Thread.sleep(50)
        assertTrue(what, condition())
    }

    /** Trascinando da "Uno" a "Due" si scelgono quelle due righe, non il gruppo: Canc toglie solo loro. */
    @Test
    fun draggingFromALineToAnotherChoosesTheLines() = withPage {
        val uno = line("Uno")
        val due = line("Due")
        drag(Offset(uno.left + 10f, uno.center.y), Offset(due.left + 10f, due.center.y))
        File("build/desktop-screenshots", "column_lines_chosen.png").apply { parentFile.mkdirs() }
            .writeBytes(Image.makeFromBitmap(picture()).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        key(Key.Delete, false)
        waitFor("restano Tre e Quattro") { children("sinistra").map { it.id } == listOf("tre") }
        assertEquals(listOf("quattro"), children("destra").map { it.id })
    }

    /** Con Ctrl, "Uno" e "Tre" (non attaccate) e "Quattro" nell'altra colonna; Canc lascia "Due". */
    @Test
    fun ctrlClickChoosesLinesInColumns() = withPage {
        ctrlClick(line("Uno").center)
        ctrlClick(line("Tre").center)
        ctrlClick(line("Quattro").center)
        key(Key.Delete, false)
        // L'ultima riga a destra se n'è andata con la sua colonna: il gruppo torna righe normali.
        waitFor("resta Due") { runBlocking { db.blockDao().getBlocksForPageOnce("testo") }.none { it.id in setOf("uno", "tre", "quattro") } }
        assertTrue(runBlocking { db.blockDao().getBlocksForPageOnce("testo") }.any { it.id == "due" })
    }

    /**
     * Il colore a due righe scelte in una colonna, e Ctrl+Z lo toglie: nel database e sullo schermo,
     * dove "Uno" torna bianca.
     */
    @Test
    fun colorAndUndoInAColumn() = withPage {
        val uno = line("Uno")
        val due = line("Due")
        drag(Offset(uno.left + 10f, uno.center.y), Offset(due.left + 10f, due.center.y))
        viewModel.colorBlocks(setOf("uno", "due"), background = false, hex = "#E03E3E")
        waitFor("rosse") { children("sinistra").filter { it.id != "tre" }.all { "#E03E3E" in it.textJson } }
        frames(10)
        fun redIn(bitmap: Bitmap, box: Rect) = (box.top.toInt() until box.bottom.toInt()).sumOf { y ->
            (box.left.toInt() until box.right.toInt()).count { x -> bitmap.getColor(x, y).let { c -> ((c shr 16) and 0xFF) > 170 && ((c shr 8) and 0xFF) < 110 } }
        }
        assertTrue("Uno rossa sullo schermo", redIn(picture(), line("Uno")) > 10)
        key(Key.Z, true)
        frames(10)
        waitFor("di nuovo senza colore") { children("sinistra").none { "#E03E3E" in it.textJson } }
        frames(10)
        val after = picture()
        File("build/desktop-screenshots", "column_lines_undo.png").writeBytes(Image.makeFromBitmap(after).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        assertEquals("Uno bianca sullo schermo", 0, redIn(after, line("Uno")))
    }

    /** Col cursore dentro "Uno": il colore della riga (Color del clic destro), poi Ctrl+Z la rimette bianca. */
    @Test
    fun undoWithTheCursorInALineOfAColumn() = withPage {
        val uno = line("Uno")
        mouse(PointerEventType.Move, uno.center, false, null)
        mouse(PointerEventType.Press, uno.center, true, null)
        mouse(PointerEventType.Release, uno.center, false, null)
        frames(5)
        viewModel.colorWholeBlock("uno", background = false, hex = "#E03E3E")
        waitFor("rossa") { "#E03E3E" in children("sinistra").first().textJson }
        frames(10)
        fun redIn(bitmap: Bitmap, box: Rect) = (box.top.toInt() until box.bottom.toInt()).sumOf { y ->
            (box.left.toInt() until box.right.toInt()).count { x -> bitmap.getColor(x, y).let { c -> ((c shr 16) and 0xFF) > 170 && ((c shr 8) and 0xFF) < 110 } }
        }
        assertTrue("Uno rossa sullo schermo", redIn(picture(), line("Uno")) > 10)
        key(Key.Z, true)
        frames(10)
        waitFor("di nuovo senza colore") { "#E03E3E" !in children("sinistra").first().textJson }
        frames(10)
        assertEquals("Uno bianca sullo schermo", 0, redIn(picture(), line("Uno")))
        assertEquals("il testo è lo stesso", """[{"text":"Uno"}]""", children("sinistra").first().textJson)
    }
}
