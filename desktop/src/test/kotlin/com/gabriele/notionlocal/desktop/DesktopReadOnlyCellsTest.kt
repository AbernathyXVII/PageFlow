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
 * **"Read only" blocca tutte le colonne** (24/11/2026, trovato dall'utente nel collaudo: "Vorrei che lockasse
 * completamente tutto, anche la selezione dei tag, il modificare delle date"): col database bloccato un tocco su
 * un tag, una casella o una data non apre niente e non cambia niente.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopReadOnlyCellsTest {
    private val dir: File = Files.createTempDirectory("pageflow-readonly").toFile()
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
        db.pageDao().insert(PageEntity(id = "chiuso", title = "Chiuso", isDatabase = true, databaseLayout = DatabaseLayout.TABLE, isLocked = true))
        db.databaseDao().insertColumn(DatabaseColumnEntity(id = "stato", pageId = "chiuso", name = "Stato", type = ColumnType.SELECT, orderIndex = 0, optionsJson = """[{"label":"Uno"},{"label":"Due"}]"""))
        db.databaseDao().insertColumn(DatabaseColumnEntity(id = "fatto", pageId = "chiuso", name = "Fatto", type = ColumnType.CHECKBOX, orderIndex = 1))
        db.databaseDao().insertColumn(DatabaseColumnEntity(id = "quando", pageId = "chiuso", name = "Quando", type = ColumnType.DATE, orderIndex = 2))
        db.databaseDao().insertRow(DatabaseRowEntity(id = "r", pageId = "chiuso", title = "Riga", orderIndex = 0))
        db.databaseDao().insertCell(DatabaseCellEntity(rowId = "r", columnId = "stato", value = "Uno"))
        db.databaseDao().insertCell(DatabaseCellEntity(rowId = "r", columnId = "fatto", value = "false"))
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    @Test
    fun aLockedDatabaseChangesNoCell() {
        val factory = ViewModelFactory(context)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 1000, height = 600, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            DatabaseViewScreen(
                                pageId = "chiuso", factory = factory, onBack = {}, onOpenRowPage = {}, onOpenSearch = {},
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
        fun texts(): List<Pair<String, Rect>> = onWindowThread {
            scene.semanticsOwners.flatMap { it.getAllSemanticsNodes(mergingEnabled = false) }.mapNotNull { node ->
                (node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }
                    ?: node.config.getOrNull(SemanticsProperties.EditableText)?.text)?.let { it to node.boundsInRoot }
            }
        }
        fun at(text: String) = texts().firstOrNull { it.first == text }?.second ?: error("manca \"$text\" fra ${texts().map { it.first }}")
        fun click(x: Float, y: Float) = onWindowThread {
            val point = Offset(x, y)
            scene.sendPointerEvent(PointerEventType.Move, point, type = PointerType.Mouse)
            scene.sendPointerEvent(PointerEventType.Press, point, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
            scene.sendPointerEvent(PointerEventType.Release, point, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
        }
        fun shot(name: String) = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }.also {
            File("build/desktop-screenshots").mkdirs()
            File("build/desktop-screenshots", "$name.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(it).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        fun value(column: String) = runBlocking { AppDatabase.getInstance(context).databaseDao().getCell("r", column)?.value }
        try {
            frames(40)
            shot("readonly_before")
            val shown = texts().map { it.first }.toSet()
            val rowY = at("Uno").center.y
            // Sulla pastiglia del tag, sulla casella (a sinistra del nome della colonna, sotto la sua icona), sul trattino della data.
            val targets = mapOf("Stato" to at("Uno").center.x, "Fatto" to at("Fatto").left - 14f, "Quando" to at("Quando").center.x)
            for ((column, x) in targets) {
                click(x, rowY)
                frames(12)
                shot("readonly_$column")
                assertEquals("toccando $column non si apre niente", shown, texts().map { it.first }.toSet())
            }
            assertEquals("il tag resta", "Uno", value("stato"))
            assertEquals("la casella resta vuota", "false", value("fatto"))
            assertEquals("niente data", null, value("quando"))
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
