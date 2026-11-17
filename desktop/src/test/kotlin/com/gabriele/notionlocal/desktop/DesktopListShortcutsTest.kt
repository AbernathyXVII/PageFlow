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
import com.gabriele.notionlocal.ui.screen.runPrefixes
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Elenchi: i numeri fermi e "- "** (17/11/2026, chiesto dall'utente: "se
 * premo Invio ripetutamente e molto velocemente, i numeri prima mi scattano",
 * e "se premo - e uno spazio, fai sì che inizi una bulleted list").
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopListShortcutsTest {
    private val dir: File = Files.createTempDirectory("pageflow-listshortcuts").toFile()
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
        repo.createPage(PageEntity(id = "elenchi", title = "E"))
        repo.saveBlock(BlockEntity(id = "riga", pageId = "elenchi", type = BlockType.PARAGRAPH, textJson = """[{"text":""}]""", orderIndex = 0))
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    /**
     * Tre Invio di fila dopo "1. Uno", e il database ancora indietro di uno: le
     * righe vuote nuove sono tutte uguali e il blocco salvato finiva abbinato
     * all'ultima. I numeri sono comunque 1, 2, 3 (prima "1. 2. 2.", poi
     * "1. 2. 3.": lo scatto).
     */
    @Test
    fun theNumbersStayStillWhileTheDatabaseIsBehind() {
        val viewModel = ViewModelFactory(context).create(PageEditorViewModel::class, CreationExtras.Empty)
        val uno = BlockEntity(id = "n1", pageId = "elenchi", type = BlockType.NUMBERED_LIST_ITEM, textJson = """[{"text":"Uno"}]""", orderIndex = 0)
        val nuova = BlockEntity(id = "n2", pageId = "elenchi", type = BlockType.NUMBERED_LIST_ITEM, textJson = """[{"text":""}]""", orderIndex = 1)
        val markers = runPrefixes(listOf("Uno", "", ""), listOf(uno, nuova), mapOf("n1" to 1, "n2" to 2), viewModel).map { it.marker.trim() }
        assertEquals(listOf("1.", "2.", "3."), markers)
        // E un blocco che il conto dei numeri non conosce ancora non diventa "1.".
        val unknown = runPrefixes(listOf("Uno", ""), listOf(uno, nuova), mapOf("n1" to 1), viewModel).map { it.marker.trim() }
        assertEquals(listOf("1.", "2."), unknown)
        // Un numero di partenza scelto a mano ("5.") vale sempre.
        val five = nuova.copy(numberStartsAt = 5)
        val restarted = runPrefixes(listOf("Uno", ""), listOf(uno, five), mapOf("n1" to 1, "n2" to 5), viewModel).map { it.marker.trim() }
        assertEquals(listOf("1.", "5."), restarted)
    }

    /** "- " scritto da solo in una riga: la riga diventa un elenco puntato e il "- " sparisce. */
    @Test
    fun dashAndSpaceStartABulletedList() {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 600, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
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
        fun type(char: Char) {
            onWindowThread {
                val awt = java.awt.event.KeyEvent(java.awt.Canvas(), java.awt.event.KeyEvent.KEY_TYPED, 0L, 0, java.awt.event.KeyEvent.VK_UNDEFINED, char)
                scene.sendKeyEvent(KeyEvent(Key.Unknown, KeyEventType.Unknown, codePoint = char.code, nativeEvent = awt))
            }
            frames(8)
        }
        fun line(): Pair<BlockType, String> = runBlocking {
            val block = db.blockDao().getBlocksForPageOnce("elenchi").single()
            block.type to Regex("\"text\":\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(block.textJson).joinToString("") { it.groupValues[1] }
        }
        try {
            frames(40)
            // La riga vuota sotto il titolo della pagina: un clic nel vuoto sotto porta lì.
            onWindowThread {
                val at = Offset(300f, 400f)
                scene.sendPointerEvent(PointerEventType.Move, at, type = PointerType.Mouse)
                scene.sendPointerEvent(PointerEventType.Press, at, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
                scene.sendPointerEvent(PointerEventType.Release, at, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
            }
            frames(15)
            type('-')
            type(' ')
            frames(10)
            assertEquals("\"- \": un elenco puntato", BlockType.BULLET_LIST_ITEM to "", line())
            type('x')
            frames(10)
            assertEquals(BlockType.BULLET_LIST_ITEM to "x", line())
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
