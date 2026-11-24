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
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.screen.CELL_FILL_HANDLE_TAG
import com.gabriele.notionlocal.ui.screen.DatabaseViewScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
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
 * **Il pallino per riempire, come in Excel** (24/11/2026, chiesto dall'utente con le foto di Notion): scrivendo in
 * una cella, Esc la lascia scelta col bordo blu e un pallino in basso a destra; trascinandolo in giù le celle sotto
 * della stessa colonna prendono il suo valore.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopCellFillTest {
    private val dir: File = Files.createTempDirectory("pageflow-fill").toFile()
    private val context = DesktopContext(dir)

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        val db = AppDatabase.getInstance(context)
        db.pageDao().insert(PageEntity(id = "spesa", title = "Spesa", isDatabase = true, databaseLayout = DatabaseLayout.TABLE))
        db.databaseDao().insertColumn(DatabaseColumnEntity(id = "num", pageId = "spesa", name = "Numero", type = ColumnType.NUMBER, orderIndex = 0))
        listOf("a", "b", "c", "d").forEachIndexed { i, id ->
            db.databaseDao().insertRow(DatabaseRowEntity(id = id, pageId = "spesa", title = "Riga ${id.uppercase()}", orderIndex = i))
        }
        db.databaseDao().insertCell(DatabaseCellEntity(rowId = "a", columnId = "num", value = "7"))
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    @Test
    fun theHandleFillsTheCellsBelow() {
        val factory = ViewModelFactory(context)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 1000, height = 600, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            DatabaseViewScreen(
                                pageId = "spesa", factory = factory, onBack = {}, onOpenRowPage = {}, onOpenSearch = {},
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
        fun nodes(): List<Triple<String?, String?, Rect>> = onWindowThread {
            scene.semanticsOwners.flatMap { it.getAllSemanticsNodes(mergingEnabled = false) }.map { node ->
                Triple(
                    node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }
                        ?: node.config.getOrNull(SemanticsProperties.EditableText)?.text,
                    node.config.getOrNull(SemanticsProperties.TestTag),
                    node.boundsInRoot
                )
            }
        }
        fun at(text: String) = nodes().firstOrNull { it.first == text }?.third ?: error("manca \"$text\" fra ${nodes().mapNotNull { it.first }}")
        fun handle() = nodes().firstOrNull { it.second == CELL_FILL_HANDLE_TAG }?.third
        fun mouse(type: PointerEventType, point: Offset, pressed: Boolean) = onWindowThread {
            scene.sendPointerEvent(
                type, point, type = PointerType.Mouse,
                buttons = if (pressed) PointerButtons(isPrimaryPressed = true) else PointerButtons(),
                button = if (type == PointerEventType.Move) null else PointerButton.Primary
            )
        }
        fun key(k: Key) {
            onWindowThread {
                scene.sendKeyEvent(KeyEvent(k, KeyEventType.KeyDown))
                scene.sendKeyEvent(KeyEvent(k, KeyEventType.KeyUp))
            }
            frames(5)
        }
        fun shot(name: String) = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }.also {
            File("build/desktop-screenshots").mkdirs()
            File("build/desktop-screenshots", "$name.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(it).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        fun value(row: String) = runBlocking { AppDatabase.getInstance(context).databaseDao().getCell(row, "num")?.value }
        try {
            frames(40)
            // Nella cella del 7: si scrive, poi Esc la lascia scelta, col pallino.
            val seven = at("7")
            mouse(PointerEventType.Move, seven.center, false)
            mouse(PointerEventType.Press, seven.center, true)
            mouse(PointerEventType.Release, seven.center, false)
            frames(10)
            assertEquals("niente pallino mentre si scrive", null, handle())
            key(Key.Escape)
            shot("fill_chosen")
            val grip = handle() ?: error("dopo Esc il pallino non c'è")

            // Il pallino in giù fino all'ultima riga: le tre celle sotto prendono il 7.
            val bottom = at("Riga D").center.y
            mouse(PointerEventType.Move, grip.center, false)
            mouse(PointerEventType.Press, grip.center, true)
            frames(2)
            for (step in 1..10) {
                mouse(PointerEventType.Move, Offset(grip.center.x, grip.center.y + (bottom - grip.center.y) * step / 10f), true)
                frames(1)
            }
            shot("fill_dragging")
            mouse(PointerEventType.Release, Offset(grip.center.x, bottom), false)
            frames(10)
            shot("fill_done")
            val end = System.currentTimeMillis() + 5_000
            while (System.currentTimeMillis() < end && value("d") != "7") Thread.sleep(50)
            assertEquals("le tre righe sotto", listOf("7", "7", "7"), listOf(value("b"), value("c"), value("d")))
            assertEquals("la prima resta", "7", value("a"))

            // Esc la lascia.
            key(Key.Escape)
            assertEquals("Esc toglie la scelta", null, handle())
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
