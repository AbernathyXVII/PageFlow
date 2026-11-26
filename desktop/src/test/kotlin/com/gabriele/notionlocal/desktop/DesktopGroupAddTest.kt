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
import com.gabriele.notionlocal.data.entity.GroupRuleSpec
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.encodeGroupRules
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **I gruppi di un database** (26/11/2026, chiesto dall'utente come su Notion): il "+" accanto al nome di
 * un gruppo crea una pagina già dentro di lui; in fondo c'è "New group" e non un altro "New page", e il
 * gruppo nuovo è un'etichetta nuova della proprietà dei gruppi.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopGroupAddTest {
    private val dir: File = Files.createTempDirectory("pageflow-groupadd").toFile()
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
        db.pageDao().insert(
            PageEntity(
                id = "data", title = "Data", isDatabase = true, databaseLayout = DatabaseLayout.TABLE,
                tableGroupRulesJson = encodeGroupRules(listOf(GroupRuleSpec("number")))
            )
        )
        db.databaseDao().insertColumn(DatabaseColumnEntity(id = "number", pageId = "data", name = "number", type = ColumnType.SELECT, orderIndex = 0, optionsJson = """[{"label":"1"},{"label":"2"}]"""))
        listOf("b" to "1", "d" to "1", "c" to "2", "e" to "2").forEachIndexed { i, (name, value) ->
            db.databaseDao().insertRow(DatabaseRowEntity(id = name, pageId = "data", title = name, orderIndex = i))
            db.databaseDao().insertCell(DatabaseCellEntity(rowId = name, columnId = "number", value = value))
        }
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }
    private var time = 0L

    private fun ImageComposeScene.frames(count: Int) = repeat(count) {
        onWindowThread { render(time) }
        time += 100_000_000L
        Thread.sleep(30)
    }

    private fun ImageComposeScene.shot(name: String) {
        val bitmap = onWindowThread { Bitmap.makeFromImage(render(time)) }
        File("build/desktop-screenshots").mkdirs()
        File("build/desktop-screenshots", "$name.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.PNG)!!.bytes)
    }

    private fun ImageComposeScene.nodes(): List<Pair<String, Rect>> = onWindowThread {
        semanticsOwners.flatMap { it.getAllSemanticsNodes(mergingEnabled = false) }.mapNotNull { node ->
            (node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }
                ?: node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(""))?.let { it to node.boundsInRoot }
        }
    }

    private fun ImageComposeScene.mouse(type: PointerEventType, at: Offset, pressed: Boolean = false) = onWindowThread {
        sendPointerEvent(
            type, at, type = PointerType.Mouse,
            buttons = if (pressed) PointerButtons(isPrimaryPressed = true) else PointerButtons(),
            button = if (type == PointerEventType.Move) null else PointerButton.Primary
        )
    }

    private fun ImageComposeScene.click(at: Offset) {
        mouse(PointerEventType.Move, at)
        mouse(PointerEventType.Press, at, pressed = true)
        mouse(PointerEventType.Release, at)
        frames(5)
    }

    private fun values(): Map<String, String> = runBlocking {
        val dao = AppDatabase.getInstance(context).databaseDao()
        dao.getRowsForPageOnce("data").associate { it.id to (dao.getCell(it.id, "number")?.value ?: "") }
    }

    @Test
    fun thePlusOfAGroupAndNewGroup() {
        val factory = ViewModelFactory(context)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 700, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            DatabaseViewScreen(
                                pageId = "data", factory = factory, onBack = {}, onOpenRowPage = {}, onOpenSearch = {},
                                onOpenUpdates = {}, onOpenPage = {}, onOpenDatabase = {}
                            )
                        }
                    }
                }
            }
        }
        try {
            scene.frames(40)
            val labels = scene.nodes().map { it.first }
            // Un "New page" per gruppo, e in fondo "New group" al posto del terzo.
            assertEquals("i New page: $labels", 2, labels.count { it == "New page" })
            assertTrue("New group in fondo: $labels", "New group" in labels)
            // Senza il mouse sopra, nessun "+" sui gruppi.
            assertTrue(labels.none { it == "New page in this group" })

            // Col mouse sull'intestazione del gruppo "2", il "+" accanto al nome; cliccato, una pagina col valore 2.
            // L'intestazione del gruppo "2" è la riga con un "2" più vicina sopra la riga "c" (anche il gruppo "1" ha "2" pagine).
            val c = scene.nodes().last { it.first == "c" }.second
            val header = scene.nodes().filter { it.first == "2" && it.second.bottom < c.top }.maxBy { it.second.top }.second
            scene.mouse(PointerEventType.Move, Offset(header.right + 120f, header.center.y))
            scene.frames(3)
            val plus = scene.nodes().singleOrNull { it.first == "New page in this group" }?.second
                ?: throw AssertionError("manca il + del gruppo: ${scene.nodes().map { it.first }}")
            assertTrue("accanto al nome, sulla stessa riga: $plus, $header", plus.left > header.right && kotlin.math.abs(plus.center.y - header.center.y) < 6f)
            scene.shot("group_plus")
            scene.click(plus.center)
            val end = System.currentTimeMillis() + 5_000
            while (values().size < 5 && System.currentTimeMillis() < end) Thread.sleep(30)
            assertEquals("una pagina nuova nel gruppo 2: ${values()}", 3, values().values.count { it == "2" })

            // "New group": il nome, Create; il gruppo nuovo si vede con la sua prima pagina.
            scene.frames(10)
            scene.click(scene.nodes().single { it.first == "New group" }.second.center)
            scene.frames(5)
            scene.click(scene.nodes().single { it.first == "A new value of number" }.second.center)
            "Tre".forEach { char ->
                onWindowThread {
                    val awt = java.awt.event.KeyEvent(java.awt.Canvas(), java.awt.event.KeyEvent.KEY_TYPED, 0L, 0, java.awt.event.KeyEvent.VK_UNDEFINED, char)
                    scene.sendKeyEvent(KeyEvent(Key.Unknown, KeyEventType.Unknown, codePoint = char.code, nativeEvent = awt))
                }
                scene.frames(1)
            }
            scene.frames(3)
            scene.shot("group_dialog")
            scene.click(scene.nodes().single { it.first == "Create" }.second.center)
            val end2 = System.currentTimeMillis() + 5_000
            while (values().values.none { it == "Tre" } && System.currentTimeMillis() < end2) Thread.sleep(30)
            assertTrue("una pagina nel gruppo nuovo: ${values()}", values().values.any { it == "Tre" })
            val options = runBlocking { AppDatabase.getInstance(context).databaseDao().getColumnsForPageOnce("data").single().optionsJson }
            assertTrue("l'etichetta nuova: $options", "\"Tre\"" in options)
            scene.frames(10)
            scene.shot("group_new")
            assertTrue("il gruppo si vede: ${scene.nodes().map { it.first }}", scene.nodes().any { it.first == "Tre" })
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
