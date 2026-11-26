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
import androidx.lifecycle.viewmodel.CreationExtras
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.GroupRuleSpec
import com.gabriele.notionlocal.data.entity.encodeGroupRules
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.screen.DatabaseViewScreen
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
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
import kotlin.math.abs

/**
 * **Le pagine di un database scelte con la casella, sul PC** (25/11/2026, chiesto dall'utente con le
 * foto di Notion): la casella compare col mouse sopra la riga; quella dei titoli le sceglie tutte; con
 * delle righe scelte c'è la barra "N selected" al posto del nome; trascinando una riga scelta vengono
 * con lei tutte le altre, una dopo l'altra.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopRowSelectionTest {
    private val dir: File = Files.createTempDirectory("pageflow-rowselect").toFile()
    private val context = DesktopContext(dir)
    private val names = listOf("a", "b", "c", "d", "e")

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        val db = AppDatabase.getInstance(context)
        db.pageDao().insert(PageEntity(id = "esempi", title = "Esempi", isDatabase = true, databaseLayout = DatabaseLayout.TABLE))
        names.forEachIndexed { i, name -> db.databaseDao().insertRow(DatabaseRowEntity(id = name, pageId = "esempi", title = name, orderIndex = i)) }
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

    /** I testi e le descrizioni sullo schermo, con dove stanno. */
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
        frames(4)
    }

    private fun order(): List<String> = runBlocking { AppDatabase.getInstance(context).databaseDao().getRowsForPageOnce("esempi").map { it.title } }

    @Test
    fun checkboxesBarAndDraggingTheChosenRows() {
        val factory = ViewModelFactory(context)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 600, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            DatabaseViewScreen(
                                pageId = "esempi", factory = factory, onBack = {}, onOpenRowPage = {}, onOpenSearch = {},
                                onOpenUpdates = {}, onOpenPage = {}, onOpenDatabase = {}
                            )
                        }
                    }
                }
            }
        }
        try {
            scene.frames(40)
            fun row(name: String) = scene.nodes().last { it.first == name }.second
            fun checkboxes() = scene.nodes().filter { it.first == "Select" }.map { it.second }
            // Senza il mouse sopra, nessuna casella.
            assertEquals("nessuna casella senza il mouse", 0, checkboxes().size)

            // Col mouse sopra "b" la sua casella, a sinistra della riga; cliccata, "1 selected" al posto del nome.
            fun choose(name: String) {
                val r = row(name)
                scene.mouse(PointerEventType.Move, Offset(r.center.x + 100f, r.center.y))
                scene.frames(3)
                val box = checkboxes().minByOrNull { abs(it.center.y - r.center.y) } ?: throw AssertionError("manca la casella di $name")
                assertTrue("la casella sta a sinistra del nome: $box, $r", box.right < r.left)
                scene.click(box.center)
            }
            choose("b")
            scene.mouse(PointerEventType.Move, Offset(800f, 580f))
            scene.frames(5)
            scene.shot("rowselect_one")
            assertTrue("la barra: ${scene.nodes().map { it.first }}", scene.nodes().any { it.first == "1 selected" })
            assertEquals("la casella spuntata resta senza il mouse", 1, checkboxes().size)

            // La casella dei titoli le sceglie tutte, e poi le toglie.
            scene.click(scene.nodes().single { it.first == "Select all" }.second.center)
            scene.frames(4)
            assertTrue("tutte: ${scene.nodes().map { it.first }}", scene.nodes().any { it.first == "5 selected" })
            scene.click(scene.nodes().single { it.first == "Select all" }.second.center)
            scene.frames(4)
            assertTrue("nessuna", scene.nodes().none { it.first.endsWith(" selected") })

            // a e c scelte, poi a presa per la maniglia e portata sotto e: arrivano tutte e due, in ordine.
            choose("a")
            choose("c")
            scene.frames(4)
            scene.shot("rowselect_two")
            val a = row("a")
            scene.mouse(PointerEventType.Move, Offset(a.center.x + 100f, a.center.y))
            scene.frames(3)
            val handle = scene.nodes().first { it.first == "Drag to move" }.second
            scene.mouse(PointerEventType.Move, handle.center)
            scene.mouse(PointerEventType.Press, handle.center, pressed = true)
            scene.frames(2)
            val to = row("e").bottom - 3f
            val steps = ((to - handle.center.y) / 4f).toInt()
            for (i in 1..steps) {
                scene.mouse(PointerEventType.Move, Offset(handle.center.x, handle.center.y + (to - handle.center.y) * i / steps), pressed = true)
                scene.frames(1)
            }
            scene.shot("rowselect_dragging")
            scene.mouse(PointerEventType.Release, Offset(handle.center.x, to))
            scene.frames(10)
            val end = System.currentTimeMillis() + 5_000
            while (order() != listOf("b", "d", "e", "a", "c") && System.currentTimeMillis() < end) Thread.sleep(30)
            assertEquals(listOf("b", "d", "e", "a", "c"), order())
            scene.frames(5)
            scene.shot("rowselect_moved")

            // I tre puntini della barra: le voci di Notion che l'app ha.
            scene.click(scene.nodes().single { it.first == "More options" }.second.center)
            scene.frames(5)
            scene.shot("rowselect_menu")
            val menu = scene.nodes().map { it.first }
            assertTrue("il menu: $menu", listOf("Add to favorites", "Edit icon", "Duplicate", "Move to", "Delete").all { it in menu })
            // Nel menu "Delete" è la voce più in basso (il cestino della barra sta più in alto).
            scene.click(scene.nodes().filter { it.first == "Delete" }.maxBy { it.second.top }.second.center)
            scene.frames(5)
            scene.shot("rowselect_delete")
            // La domanda prima di eliminare: "Delete" in fondo alla finestra.
            scene.click(scene.nodes().filter { it.first == "Delete" }.maxBy { it.second.top }.second.center)
            val gone = System.currentTimeMillis() + 5_000
            while (order() != listOf("b", "d", "e") && System.currentTimeMillis() < gone) Thread.sleep(30)
            assertEquals(listOf("b", "d", "e"), order())
        } finally {
            onWindowThread { scene.close() }
        }
    }

    /** Lo stesso database dentro una pagina: le caselle nel margine della pagina si vedono e si cliccano. */
    @Test
    fun insideAPageTooTheCheckboxesWork() {
        runBlocking {
            val repo = PageRepository(AppDatabase.getInstance(context))
            repo.createPage(PageEntity(id = "casa", title = "Casa"))
            repo.saveBlock(BlockEntity(id = "link", pageId = "casa", type = BlockType.DATABASE_LINK, linkedPageId = "esempi", orderIndex = 0))
            repo.saveBlock(BlockEntity(id = "sotto", pageId = "casa", type = BlockType.PARAGRAPH, textJson = """[{"text":"Sotto"}]""", orderIndex = 1))
        }
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 600, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        PageEditorScreen(
                            pageId = "casa", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
                            onNavigateToPage = {}, onNavigateToDatabase = {}, viewModel = viewModel
                        )
                    }
                }
            }
        }
        try {
            scene.frames(40)
            // Il testo sotto il database non si sposta quando la barra prende il posto del nome (26/11/2026).
            fun below() = onWindowThread {
                scene.semanticsOwners.flatMap { it.getAllSemanticsNodes(mergingEnabled = false) }
                    .first { it.config.getOrNull(SemanticsProperties.EditableText)?.text?.contains("Sotto") == true }.boundsInRoot.top
            }
            val belowBefore = below()
            val r = scene.nodes().last { it.first == "c" }.second
            scene.mouse(PointerEventType.Move, Offset(r.center.x + 100f, r.center.y))
            scene.frames(3)
            val box = scene.nodes().filter { it.first == "Select" }.map { it.second }.minByOrNull { abs(it.center.y - r.center.y) }
                ?: throw AssertionError("manca la casella: ${scene.nodes().map { it.first }}")
            scene.click(box.center)
            scene.frames(5)
            scene.shot("rowselect_inline")
            assertTrue("la barra dentro la pagina: ${scene.nodes().map { it.first }}", scene.nodes().any { it.first == "1 selected" })
            assertEquals("il testo sotto resta dov'era", belowBefore, below(), 0.5f)
        } finally {
            onWindowThread { scene.close() }
        }
    }

    /** La riga [name] col mouse sopra: la sua casella c'è, a sinistra, e cliccata fa "1 selected". */
    private fun ImageComposeScene.chooseAndCheck(name: String, shotName: String) {
        frames(40)
        val r = nodes().last { it.first == name }.second
        mouse(PointerEventType.Move, Offset(r.center.x + 100f, r.center.y))
        frames(3)
        val box = nodes().filter { it.first == "Select" }.map { it.second }.minByOrNull { abs(it.center.y - r.center.y) }
            ?: throw AssertionError("manca la casella di $name: ${nodes().map { it.first }}")
        assertTrue("la casella a sinistra di $name: $box, $r", box.right < r.left)
        click(box.center)
        frames(5)
        shot(shotName)
        assertTrue("la barra: ${nodes().map { it.first }}", nodes().any { it.first == "1 selected" })
    }

    /**
     * Nell'elenco la casella "sceglile tutte" sta una riga sopra la prima (25/11/2026): cliccata le sceglie tutte;
     * poi Esc toglie la selezione.
     */
    private fun ImageComposeScene.selectAllAboveAndEsc(shotName: String) {
        val first = nodes().last { it.first == "a" }.second
        val second = nodes().last { it.first == "b" }.second
        val pitch = second.center.y - first.center.y
        val all = nodes().single { it.first == "Select all" }.second
        assertTrue("sopra la prima riga, una riga più su: $all, prima $first, passo $pitch", abs(all.center.y - (first.center.y - pitch)) <= 3f && all.right < first.left)
        click(all.center)
        frames(5)
        shot(shotName)
        assertTrue("tutte: ${nodes().map { it.first }}", nodes().any { it.first == "5 selected" })
        onWindowThread {
            sendKeyEvent(androidx.compose.ui.input.key.KeyEvent(androidx.compose.ui.input.key.Key.Escape, androidx.compose.ui.input.key.KeyEventType.KeyDown))
            sendKeyEvent(androidx.compose.ui.input.key.KeyEvent(androidx.compose.ui.input.key.Key.Escape, androidx.compose.ui.input.key.KeyEventType.KeyUp))
        }
        frames(5)
        assertTrue("Esc toglie la selezione: ${nodes().map { it.first }}", nodes().none { it.first.endsWith(" selected") })
    }

    private fun databaseScene(): ImageComposeScene {
        val factory = ViewModelFactory(context)
        val owner = ScreenOwner()
        return onWindowThread {
            ImageComposeScene(width = 900, height = 600, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            DatabaseViewScreen(
                                pageId = "esempi", factory = factory, onBack = {}, onOpenRowPage = {}, onOpenSearch = {},
                                onOpenUpdates = {}, onOpenPage = {}, onOpenDatabase = {}
                            )
                        }
                    }
                }
            }
        }
    }

    /** **Anche nell'elenco** (25/11/2026, trovato dall'utente: in Notebook, un elenco, le caselle non c'erano), a tutta pagina. */
    @Test
    fun aListHasTheCheckboxesToo() {
        runBlocking {
            val dao = AppDatabase.getInstance(context).pageDao()
            dao.update(dao.getById("esempi")!!.copy(databaseLayout = DatabaseLayout.LIST))
        }
        val scene = databaseScene()
        try {
            scene.chooseAndCheck("c", "rowselect_list")
            scene.selectAllAboveAndEsc("rowselect_list_all")
        } finally {
            onWindowThread { scene.close() }
        }
    }

    /** Una tabella coi gruppi e un ordinamento, come "Main calendar" dell'utente: le caselle ci sono lo stesso. */
    @Test
    fun aGroupedSortedTableHasTheCheckboxes() {
        runBlocking {
            val db = AppDatabase.getInstance(context)
            db.databaseDao().insertColumn(DatabaseColumnEntity(id = "kind", pageId = "esempi", name = "Tipo", type = ColumnType.SELECT, orderIndex = 0, optionsJson = """[{"label":"Uno"},{"label":"Due"}]"""))
            names.forEachIndexed { i, name -> db.databaseDao().insertCell(DatabaseCellEntity(rowId = name, columnId = "kind", value = if (i % 2 == 0) "Uno" else "Due")) }
            val page = db.pageDao().getById("esempi")!!
            db.pageDao().update(page.copy(tableGroupRulesJson = encodeGroupRules(listOf(GroupRuleSpec("kind"))), sortColumnId = "kind"))
        }
        val scene = databaseScene()
        try {
            scene.chooseAndCheck("d", "rowselect_grouped")
        } finally {
            onWindowThread { scene.close() }
        }
    }

    /** **Un database semplice** (25/11/2026, "Main calendar" dell'utente): le caselle ci sono, e il menu ha solo "Delete". */
    @Test
    fun aSimpleDatabaseHasTheCheckboxesToo() {
        runBlocking {
            val dao = AppDatabase.getInstance(context).pageDao()
            dao.update(dao.getById("esempi")!!.copy(isSimpleDatabase = true))
        }
        val scene = databaseScene()
        try {
            scene.chooseAndCheck("b", "rowselect_simple")
            scene.click(scene.nodes().single { it.first == "More options" }.second.center)
            scene.frames(5)
            val menu = scene.nodes().map { it.first }
            assertTrue("il menu: $menu", "Delete" in menu && "Move to" !in menu && "Add to favorites" !in menu)
        } finally {
            onWindowThread { scene.close() }
        }
    }

    /** **Un elenco dentro una pagina** (25/11/2026, "Notebook" dell'utente). */
    @Test
    fun aListInsideAPageHasTheCheckboxes() {
        runBlocking {
            val db = AppDatabase.getInstance(context)
            db.pageDao().update(db.pageDao().getById("esempi")!!.copy(databaseLayout = DatabaseLayout.LIST))
            val repo = PageRepository(db)
            repo.createPage(PageEntity(id = "casa", title = "Casa"))
            repo.saveBlock(BlockEntity(id = "link", pageId = "casa", type = BlockType.DATABASE_LINK, linkedPageId = "esempi", orderIndex = 0))
        }
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 600, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        PageEditorScreen(
                            pageId = "casa", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
                            onNavigateToPage = {}, onNavigateToDatabase = {}, viewModel = viewModel
                        )
                    }
                }
            }
        }
        try {
            scene.chooseAndCheck("d", "rowselect_list_inline")
            scene.selectAllAboveAndEsc("rowselect_list_inline_all")
        } finally {
            onWindowThread { scene.close() }
        }
    }

    /**
     * **"Sceglile tutte" sparisce col mouse fuori dall'elenco** (26/11/2026, trovato dall'utente in Notebook e
     * Foreign languages: passato il mouse su un punto dell'elenco, la casella restava accesa).
     */
    @Test
    fun theSelectAllOfAListGoesAwayWithTheMouse() {
        runBlocking {
            val dao = AppDatabase.getInstance(context).pageDao()
            dao.update(dao.getById("esempi")!!.copy(databaseLayout = DatabaseLayout.LIST))
        }
        val scene = databaseScene()
        try {
            scene.frames(40)
            fun selectAllShown() = scene.nodes().any { it.first == "Select all" }
            // Su e giù per l'elenco, poi via, come col mouse vero.
            val rows = names.map { n -> scene.nodes().last { it.first == n }.second }
            for (r in rows + rows.reversed()) {
                scene.mouse(PointerEventType.Move, Offset(r.center.x + 150f, r.center.y))
                scene.frames(1)
            }
            // Come la casella dei titoli nella tabella (26/11/2026): col mouse sulle righe non c'è.
            assertTrue("col mouse sulle righe non si vede", !selectAllShown())
            // Col mouse sul suo posto, una riga sopra la prima, nel margine, sì.
            val pitch = rows[1].center.y - rows[0].center.y
            scene.mouse(PointerEventType.Move, Offset(rows[0].center.x + 150f, rows[0].center.y))
            scene.frames(3)
            val firstBox = scene.nodes().filter { it.first == "Select" }.minBy { abs(it.second.center.y - rows[0].center.y) }.second
            scene.mouse(PointerEventType.Move, Offset(firstBox.center.x, firstBox.center.y - pitch))
            scene.frames(4)
            assertTrue("col mouse sopra di lei si vede", selectAllShown())
            scene.mouse(PointerEventType.Move, Offset(600f, 560f))
            scene.frames(4)
            scene.shot("rowselect_list_left")
            assertTrue("col mouse fuori non si vede: ${scene.nodes().map { it.first }}", !selectAllShown())
        } finally {
            onWindowThread { scene.close() }
        }
    }

    /** Lo stesso con l'elenco dentro una pagina, in una colonna, come "Notebook" dell'utente. */
    @Test
    fun theSelectAllOfAListInAColumnGoesAwayWithTheMouse() {
        runBlocking {
            val db = AppDatabase.getInstance(context)
            db.pageDao().update(db.pageDao().getById("esempi")!!.copy(databaseLayout = DatabaseLayout.LIST))
            val repo = PageRepository(db)
            repo.createPage(PageEntity(id = "casa", title = "Casa"))
            repo.saveBlock(BlockEntity(id = "link", pageId = "casa", type = BlockType.DATABASE_LINK, linkedPageId = "esempi", orderIndex = 0))
            repo.saveBlock(BlockEntity(id = "accanto", pageId = "casa", type = BlockType.PARAGRAPH, textJson = """[{"text":"Accanto"}]""", orderIndex = 1))
            assertTrue(repo.moveBlockBeside("accanto", "link", right = true))
        }
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 600, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        PageEditorScreen(
                            pageId = "casa", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
                            onNavigateToPage = {}, onNavigateToDatabase = {}, viewModel = viewModel
                        )
                    }
                }
            }
        }
        try {
            scene.frames(40)
            fun selectAllShown() = scene.nodes().any { it.first == "Select all" }
            val rows = names.map { n -> scene.nodes().last { it.first == n }.second }
            for (r in rows + rows.reversed()) {
                scene.mouse(PointerEventType.Move, Offset(r.center.x + 60f, r.center.y))
                scene.frames(1)
            }
            assertTrue("col mouse sulle righe non si vede", !selectAllShown())
            scene.shot("rowselect_column_on")
            // Via verso il titolo della pagina, poi nell'altra colonna, poi in fondo.
            for (at in listOf(Offset(200f, 140f), Offset(700f, rows[2].center.y), Offset(450f, 580f))) {
                scene.mouse(PointerEventType.Move, at)
                scene.frames(4)
                assertTrue("col mouse in $at non si vede: ${scene.nodes().map { it.first }}", !selectAllShown())
            }
        } finally {
            onWindowThread { scene.close() }
        }
    }

    /** Con le icone sulle righe, come "Notebook" e "Foreign languages", e uscendo da sinistra sopra le caselle. */
    @Test
    fun theSelectAllOfAListWithIconsGoesAwayWithTheMouse() {
        runBlocking {
            val db = AppDatabase.getInstance(context)
            db.pageDao().update(db.pageDao().getById("esempi")!!.copy(databaseLayout = DatabaseLayout.LIST))
            names.forEach { n ->
                db.pageDao().insert(PageEntity(id = "p-$n", title = n, isRowPage = true, icon = "📘"))
                val row = db.databaseDao().getRowById(n)!!
                db.databaseDao().updateRow(row.copy(linkedPageId = "p-$n"))
            }
            val repo = PageRepository(db)
            repo.createPage(PageEntity(id = "casa", title = "Casa"))
            repo.saveBlock(BlockEntity(id = "link", pageId = "casa", type = BlockType.DATABASE_LINK, linkedPageId = "esempi", orderIndex = 0))
        }
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 600, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        PageEditorScreen(
                            pageId = "casa", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
                            onNavigateToPage = {}, onNavigateToDatabase = {}, viewModel = viewModel
                        )
                    }
                }
            }
        }
        try {
            scene.frames(40)
            fun selectAllShown() = scene.nodes().any { it.first == "Select all" }
            val rows = names.map { n -> scene.nodes().last { it.first == n }.second }
            // Sulle righe, sull'icona, sulla casella, poi a sinistra fuori e in alto.
            for (r in rows) {
                for (x in listOf(r.center.x + 200f, r.left + 2f, r.left - 20f, r.left - 35f)) {
                    scene.mouse(PointerEventType.Move, Offset(x, r.center.y))
                    scene.frames(1)
                }
            }
            scene.mouse(PointerEventType.Move, Offset(rows[0].left - 20f, rows[0].center.y - (rows[1].center.y - rows[0].center.y)))
            scene.frames(2)
            for (at in listOf(Offset(3f, 300f), Offset(450f, 90f), Offset(450f, 580f))) {
                scene.mouse(PointerEventType.Move, at)
                scene.frames(4)
                scene.shot("rowselect_icons_left")
                assertTrue("col mouse in $at non si vede: ${scene.nodes().map { it.first }}", !selectAllShown())
            }
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
