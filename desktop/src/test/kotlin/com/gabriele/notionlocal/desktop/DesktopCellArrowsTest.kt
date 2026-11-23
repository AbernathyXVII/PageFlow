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
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.screen.DatabaseViewScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
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
 * **Le frecce da una cella all'altra mentre si scrive** (chiesto dall'utente il 23/11/2026):
 * scritto in "Note" della prima riga, giù porta alla stessa colonna della seconda; sinistra,
 * dall'inizio del testo, porta al nome; destra, dalla fine, torna in "Note".
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopCellArrowsTest {
    private val dir: File = Files.createTempDirectory("pageflow-cellarrows").toFile()
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
        PageRepository(db).createPage(PageEntity(id = "giochi", title = "Giochi", isDatabase = true))
        db.databaseDao().insertColumn(DatabaseColumnEntity(id = "note", pageId = "giochi", name = "Note", type = ColumnType.TEXT, orderIndex = 0))
        listOf("Uno", "Due").forEachIndexed { i, title ->
            db.databaseDao().insertRow(DatabaseRowEntity(id = "r$i", pageId = "giochi", title = title, orderIndex = i))
        }
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    @Test
    fun theArrowsMoveFromCellToCell() {
        val factory = ViewModelFactory(context)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 1000, height = 500, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            DatabaseViewScreen(
                                pageId = "giochi", factory = factory, onBack = {}, onOpenRowPage = {}, onOpenSearch = {},
                                onOpenUpdates = {}, onOpenPage = {}, onOpenDatabase = {}
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
        fun center(text: String): Offset = onWindowThread {
            scene.semanticsOwners.flatMap { it.getAllSemanticsNodes(mergingEnabled = true) }
                .first { node -> node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == text } == true }
                .boundsInRoot.center
        }
        fun click(at: Offset) = onWindowThread {
            scene.sendPointerEvent(PointerEventType.Move, at, type = PointerType.Mouse)
            scene.sendPointerEvent(PointerEventType.Press, at, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
            scene.sendPointerEvent(PointerEventType.Release, at, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
        }
        fun type(char: Char) {
            onWindowThread {
                val awt = java.awt.event.KeyEvent(java.awt.Canvas(), java.awt.event.KeyEvent.KEY_TYPED, 0L, 0, java.awt.event.KeyEvent.VK_UNDEFINED, char)
                scene.sendKeyEvent(KeyEvent(Key.Unknown, KeyEventType.Unknown, codePoint = char.code, nativeEvent = awt))
            }
            frames(2)
        }
        fun key(key: Key) {
            onWindowThread {
                scene.sendKeyEvent(KeyEvent(key, KeyEventType.KeyDown))
                scene.sendKeyEvent(KeyEvent(key, KeyEventType.KeyUp))
            }
            frames(4)
        }
        fun cell(rowId: String) = runBlocking { db.databaseDao().getCell(rowId, "note")?.value.orEmpty() }
        fun title(rowId: String) = runBlocking { db.databaseDao().getRowById(rowId)!!.title }
        try {
            frames(40)
            // La cella "Note" della prima riga: sotto l'intestazione, all'altezza di "Uno".
            click(Offset(center("Note").x, center("Uno").y))
            frames(5)
            type('a')
            key(Key.DirectionDown)
            type('b')
            // Sinistra: prima fra le lettere (dopo la "b" all'inizio), poi dall'inizio al nome.
            key(Key.DirectionLeft)
            key(Key.DirectionLeft)
            type('x')
            // Il cursore entra nel nome in fondo: "Duex". Destra, dalla fine, torna in "Note".
            key(Key.DirectionRight)
            type('c')
            frames(10)
            assertEquals("a", cell("r0"))
            assertEquals("bc", cell("r1"))
            assertEquals("Duex", title("r1"))
            assertEquals("Uno", title("r0"))
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
