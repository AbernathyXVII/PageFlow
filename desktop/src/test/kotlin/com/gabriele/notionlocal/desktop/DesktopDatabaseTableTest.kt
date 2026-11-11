package com.gabriele.notionlocal.desktop

import androidx.compose.ui.InternalComposeUiApi
import com.gabriele.notionlocal.ui.screen.notePresses
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
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
 * **La tabella di un database sul PC** (dal 11/11/2026, chiesto dall'utente,
 * come su Notion): in una finestra larga le linee fra le righe arrivano fino
 * in fondo, non si fermano dopo l'ultima colonna; accanto al "+" delle colonne
 * ci sono i tre puntini, che aprono "Property visibility" in un riquadro
 * accanto al clic, non più nel pannello che sale dal basso (`PcPanel.desktop.kt`).
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopDatabaseTableTest {
    private val dir: File = Files.createTempDirectory("pageflow-table").toFile()
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
        PageRepository(db).createPage(PageEntity(id = "giochi", title = "Giochi", isDatabase = true))
        db.databaseDao().insertColumn(DatabaseColumnEntity(id = "voto", pageId = "giochi", name = "Voto", type = ColumnType.NUMBER, orderIndex = 0))
        db.databaseDao().insertRow(DatabaseRowEntity(id = "r1", pageId = "giochi", title = "Persona 5", orderIndex = 0))
        db.databaseDao().insertRow(DatabaseRowEntity(id = "r2", pageId = "giochi", title = "Zelda", orderIndex = 1))
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    @Test
    fun theRowLinesReachTheEndAndTheDotsOpenPropertyVisibility() {
        val factory = ViewModelFactory(context)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 1200, height = 600, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize().notePresses()) {
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
        fun picture(): Bitmap = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }
        fun save(bitmap: Bitmap, name: String) =
            File("build/desktop-screenshots", name).apply { parentFile.mkdirs() }.writeBytes(Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        try {
            frames(40)
            val table = picture()
            save(table, "database_table_wide.png")
            // Una linea orizzontale vicino al bordo destro: una riga di pixel diversa dallo sfondo
            // per tutta la fascia fra x 1000 e 1150, dove non c'è nessuna colonna.
            val background = table.getColor(1100, table.height - 5)
            val lines = (60 until table.height - 20).filter { y -> (1000..1150).all { x -> table.getColor(x, y) != background } }
            assertTrue("le linee delle righe arrivano fino al bordo destro", lines.size >= 3)
            // I tre puntini: subito a destra del "+", nell'intestazione (sopra la prima linea).
            val before = picture()
            val dots = Offset(418f, lines.first() - 22f)
            click(scene, dots)
            frames(15)
            val after = picture()
            save(after, "database_property_visibility.png")
            // Il riquadro parte dal clic e va a destra e in basso; dove stava il pannello
            // dal basso (a sinistra del clic, in fondo alla finestra) non c'è niente.
            fun changedAt(x: Int, y: Int) = after.getColor(x, y) != before.getColor(x, y)
            assertTrue("il riquadro si apre sotto il clic", changedAt(dots.x.toInt() + 150, dots.y.toInt() + 60))
            assertTrue("niente pannello dal basso", !changedAt(300, 560) && !changedAt(dots.x.toInt() - 60, 560))
            // Esc lo chiude tutto, come su Notion.
            onWindowThread {
                scene.sendKeyEvent(KeyEvent(Key.Escape, KeyEventType.KeyDown))
                scene.sendKeyEvent(KeyEvent(Key.Escape, KeyEventType.KeyUp))
            }
            frames(10)
            assertEquals("Esc chiude il riquadro", before.getColor(dots.x.toInt() + 150, dots.y.toInt() + 60), picture().getColor(dots.x.toInt() + 150, dots.y.toInt() + 60))
        } finally {
            onWindowThread { scene.close() }
        }
    }

    private fun click(scene: ImageComposeScene, at: Offset) = onWindowThread {
        scene.sendPointerEvent(PointerEventType.Move, at, type = PointerType.Mouse)
        scene.sendPointerEvent(PointerEventType.Press, at, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Release, at, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
    }
}
