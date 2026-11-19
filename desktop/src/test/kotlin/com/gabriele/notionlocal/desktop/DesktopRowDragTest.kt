package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.semantics.getOrNull
import androidx.lifecycle.viewmodel.CreationExtras
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.screen.notePresses
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
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
import com.gabriele.notionlocal.data.entity.SORT_BY_NAME
import com.gabriele.notionlocal.data.entity.SortRuleSpec
import com.gabriele.notionlocal.data.entity.withSortRules
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Le righe di un database si spostano trascinandole col mouse** (17/11/2026,
 * chiesto dall'utente: "quando in un database non c'è un ordine selezionato con
 * Sort, permettimi di spostare le pagine semplicemente draggandole con il
 * mouse"). Col mouse sopra una riga la maniglia compare nel margine a sinistra;
 * trascinata sotto la terza, la prima va dopo di lei. Con un ordinamento la
 * maniglia non c'è.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopRowDragTest {
    private val dir: File = Files.createTempDirectory("pageflow-rowdrag").toFile()
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
        db.databaseDao().insertColumn(DatabaseColumnEntity(id = "voto", pageId = "giochi", name = "Voto", type = ColumnType.NUMBER, orderIndex = 0))
        listOf("Uno", "Due", "Tre", "Quattro").forEachIndexed { i, title ->
            db.databaseDao().insertRow(DatabaseRowEntity(id = "r$i", pageId = "giochi", title = title, orderIndex = i))
        }
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    private fun order(): List<String> = runBlocking { db.databaseDao().getRowsForPageOnce("giochi").sortedBy { it.orderIndex }.map { it.title } }

    @Test
    fun aRowIsDraggedBelowAnother() {
        val factory = ViewModelFactory(context)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 1200, height = 600, density = Density(1f)) {
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
        fun picture(name: String): Bitmap = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }.also {
            File("build/desktop-screenshots", name).apply { parentFile.mkdirs() }.writeBytes(Image.makeFromBitmap(it).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        fun mouse(type: PointerEventType, at: Offset, pressed: Boolean) = onWindowThread {
            scene.sendPointerEvent(
                type, at, type = PointerType.Mouse,
                buttons = if (pressed) PointerButtons(isPrimaryPressed = true) else PointerButtons(),
                button = if (type == PointerEventType.Press || type == PointerEventType.Release) PointerButton.Primary else null
            )
        }
        try {
            frames(40)
            val table = picture("rowdrag_table.png")
            // Le linee fra le righe (come in DesktopDatabaseTableTest): righe da 33.
            val background = table.getColor(1100, table.height - 5)
            val lines = (60 until table.height - 20).filter { y -> (1000..1100).all { x -> table.getColor(x, y) != background } }
            assertTrue("linee: $lines", lines.size >= 5)
            fun rowCenter(i: Int) = (lines[i] + lines[i + 1]) / 2f
            // La maniglia: nel margine della prima cella (x da 66 a 78), solo col mouse sopra la riga.
            fun handleShown(bitmap: Bitmap, y: Float) = (66 until 78).count { x -> bitmap.getColor(x, y.toInt()) != background } > 1
            assertFalse("senza mouse niente maniglia", handleShown(table, rowCenter(0)))
            // Col mouse sulla riga la maniglia c'è ma non si vede; col mouse sopra di lei sì (dal 17/11/2026).
            mouse(PointerEventType.Move, Offset(200f, rowCenter(0)), pressed = false)
            frames(4)
            assertFalse("col mouse sulla riga la maniglia non si vede", handleShown(picture("rowdrag_hover.png"), rowCenter(0)))
            val handle = Offset(72f, rowCenter(0))
            mouse(PointerEventType.Move, handle, pressed = false)
            frames(4)
            assertTrue("col mouse sopra la maniglia si vede", handleShown(picture("rowdrag_handle.png"), rowCenter(0)))
            // La maniglia di "Uno", tirata giù fin sotto la metà di "Tre": va dopo "Tre".
            mouse(PointerEventType.Press, handle, pressed = true)
            frames(2)
            var y = handle.y
            val goal = rowCenter(2) + 8f
            while (y < goal) {
                y = minOf(goal, y + 10f)
                mouse(PointerEventType.Move, Offset(72f, y), pressed = true)
                frames(1)
            }
            picture("rowdrag_dragging.png")
            mouse(PointerEventType.Release, Offset(72f, y), pressed = false)
            frames(10)
            assertEquals("Uno dopo Tre", listOf("Due", "Tre", "Uno", "Quattro"), order())
            picture("rowdrag_after.png")
            // Con un ordinamento (per nome) le righe stanno dove dice lui: niente maniglia.
            runBlocking {
                val page = db.pageDao().getById("giochi")!!
                db.pageDao().update(page.withSortRules(listOf(SortRuleSpec(SORT_BY_NAME, descending = false))))
            }
            frames(20)
            mouse(PointerEventType.Move, Offset(300f, rowCenter(1)), pressed = false)
            frames(2)
            mouse(PointerEventType.Move, Offset(200f, rowCenter(0)), pressed = false)
            frames(2)
            mouse(PointerEventType.Move, Offset(72f, rowCenter(0)), pressed = false)
            frames(4)
            assertFalse("con un ordinamento niente maniglia", handleShown(picture("rowdrag_sorted.png"), rowCenter(0)))
        } finally {
            onWindowThread { scene.close() }
        }
    }

    /**
     * **Lo stesso in un database dentro una pagina** (l'utente il 19/11/2026: "quando in un
     * database su PC sposto una pagina, mi appare quel famoso rettangolino della selezione"):
     * la maniglia sposta la riga, e il rettangolo azzurro della pagina non parte.
     */
    @Test
    fun aRowOfADatabaseInAPageIsDragged() {
        runBlocking {
            val repo = PageRepository(db)
            repo.createPage(PageEntity(id = "casa", title = "Casa"))
            repo.saveBlock(BlockEntity(id = "sopra", pageId = "casa", textJson = """[{"text":"Sopra"}]""", orderIndex = 0))
            repo.saveBlock(BlockEntity(id = "db", pageId = "casa", type = BlockType.DATABASE_LINK, linkedPageId = "giochi", orderIndex = 1))
        }
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 1200, height = 700, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize().notePresses()) {
                            PageEditorScreen(
                                pageId = "casa", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
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
        fun mouse(type: PointerEventType, at: Offset, pressed: Boolean) = onWindowThread {
            scene.sendPointerEvent(
                type, at, type = PointerType.Mouse,
                buttons = if (pressed) PointerButtons(isPrimaryPressed = true) else PointerButtons(),
                button = if (type == PointerEventType.Press || type == PointerEventType.Release) PointerButton.Primary else null
            )
        }
        fun nodes(what: (androidx.compose.ui.semantics.SemanticsConfiguration) -> Boolean) = onWindowThread {
            scene.semanticsOwners.flatMap { it.getAllSemanticsNodes(mergingEnabled = true) }.filter { what(it.config) }.map { it.boundsInRoot }
        }
        fun textAt(text: String) = nodes { c -> c.getOrNull(SemanticsProperties.Text)?.any { it.text == text } == true }.first()
        try {
            frames(40)
            val uno = textAt("Uno")
            val tre = textAt("Tre")
            mouse(PointerEventType.Move, uno.center, pressed = false)
            frames(4)
            val handle = nodes { c -> c.getOrNull(SemanticsProperties.ContentDescription)?.contains("Drag to move") == true }.single().center
            picture("rowdrag_page_handle.png", scene, time)
            mouse(PointerEventType.Move, handle, pressed = false)
            frames(2)
            mouse(PointerEventType.Press, handle, pressed = true)
            frames(2)
            var y = handle.y
            val goal = tre.center.y + 8f
            while (y < goal) {
                y = minOf(goal, y + 10f)
                mouse(PointerEventType.Move, Offset(handle.x, y), pressed = true)
                frames(1)
            }
            picture("rowdrag_page_dragging.png", scene, time)
            mouse(PointerEventType.Release, Offset(handle.x, y), pressed = false)
            frames(10)
            assertEquals("Uno dopo Tre", listOf("Due", "Tre", "Uno", "Quattro"), order())
        } finally {
            onWindowThread { scene.close() }
        }
    }

    private fun picture(name: String, scene: ImageComposeScene, time: Long) {
        val bitmap = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }
        File("build/desktop-screenshots", name).apply { parentFile.mkdirs() }.writeBytes(Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.PNG)!!.bytes)
    }
}
