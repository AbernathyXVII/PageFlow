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
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
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
            // per tutta la fascia fra x 1000 e 1100, dove non c'è nessuna colonna (dal 11/11/2026
            // il database a tutta pagina ha i margini di Notion: 66 per lato, la tabella finisce a 1134).
            val background = table.getColor(1100, table.height - 5)
            val lines = (60 until table.height - 20).filter { y -> (1000..1100).all { x -> table.getColor(x, y) != background } }
            assertTrue("a destra il margine di 66 è vuoto", lines.all { y -> table.getColor(1160, y) == background })
            assertTrue("le linee delle righe arrivano fino al bordo destro", lines.size >= 3)
            // Sul PC le righe sono alte come in Notion (fase 2, 11/11/2026): 32 più la linea.
            assertTrue("righe a ${lines.zipWithNext { a, b -> b - a }} l'una dall'altra", lines.zipWithNext { a, b -> b - a }.all { it == 33 })
            // Sul PC OPEN si vede solo col mouse sopra la cella del nome (fase 4, 11/11/2026).
            val firstRow = (lines[0] + lines[1]) / 2
            fun openShown(bitmap: Bitmap) = (206 until 262).count { x -> bitmap.getColor(x, firstRow) != background } > 4
            assertTrue("senza mouse niente OPEN", !openShown(table))
            onWindowThread { scene.sendPointerEvent(PointerEventType.Move, Offset(126f, firstRow.toFloat()), type = PointerType.Mouse) }
            frames(5)
            assertTrue("col mouse sopra il nome c'è OPEN", openShown(picture()))
            // I tre puntini: subito a destra del "+", nell'intestazione (sopra la prima linea).
            val before = picture()
            val dots = Offset(484f, lines.first() - 22f)
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

    /**
     * **Un tag nuovo in una cella select vuota** (bug trovato dall'utente
     * l'11/11/2026 nel suo Habit tracker): la parte cliccabile era alta quanto
     * i tag, quindi da vuota era alta zero e il clic non apriva niente. Il clic
     * sulla cella vuota deve aprire la scelta dei tag.
     */
    @Test
    fun anEmptySelectCellOpensTheTagPicker() {
        runBlocking {
            AppDatabase.getInstance(context).databaseDao().insertColumn(
                DatabaseColumnEntity(
                    id = "giorno", pageId = "giochi", name = "Day", type = ColumnType.SELECT, orderIndex = -1,
                    optionsJson = """[{"label":"Monday"},{"label":"Tuesday"}]"""
                )
            )
        }
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
        try {
            frames(40)
            val before = picture()
            val background = before.getColor(1100, before.height - 5)
            val lines = (60 until before.height - 20).filter { y -> (1000..1100).all { x -> before.getColor(x, y) != background } }
            // "Day" è la prima colonna dopo il nome (da 266): il clic a metà della prima riga, vuota.
            val cell = Offset(330f, (lines[0] + lines[1]) / 2f)
            click(scene, cell)
            frames(15)
            val after = picture()
            File("build/desktop-screenshots", "empty_select_picker.png").writeBytes(Image.makeFromBitmap(after).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            val changed = (0 until 200 step 4).count { dy -> after.getColor(cell.x.toInt() + 100, cell.y.toInt() + 20 + dy) != before.getColor(cell.x.toInt() + 100, cell.y.toInt() + 20 + dy) }
            assertTrue("il clic sulla cella vuota apre la scelta dei tag ($changed punti cambiati)", changed > 10)
        } finally {
            onWindowThread { scene.close() }
        }
    }

    /**
     * **Un gruppo chiuso resta chiuso** (chiesto dall'utente l'11/11/2026): la
     * tabella raggruppata per "Day", chiuso il primo gruppo con un clic sulla
     * sua intestazione, uscendo e riaprendo il database è ancora chiuso.
     */
    @Test
    fun aClosedGroupStaysClosedAfterLeaving() {
        runBlocking {
            val db = AppDatabase.getInstance(context)
            db.databaseDao().insertColumn(
                DatabaseColumnEntity(
                    id = "giorno", pageId = "giochi", name = "Day", type = ColumnType.SELECT, orderIndex = 1,
                    optionsJson = """[{"label":"Monday"},{"label":"Tuesday"}]"""
                )
            )
            db.databaseDao().insertCell(DatabaseCellEntity(rowId = "r1", columnId = "giorno", value = "Monday"))
            db.databaseDao().insertCell(DatabaseCellEntity(rowId = "r2", columnId = "giorno", value = "Tuesday"))
            val repo = PageRepository(db)
            repo.updatePage(db.pageDao().getById("giochi")!!.copy(tableGroupRulesJson = """[{"column":"giorno"}]"""))
        }

        fun bright(c: Int) = ((c shr 16) and 0xFF) > 140 && ((c shr 8) and 0xFF) > 140 && (c and 0xFF) > 140
        /** Le fasce con lettere chiare nella colonna del nome, sotto la barra delle viste. */
        fun bands(page: Bitmap): List<IntRange> {
            val rows = (120 until page.height).filter { y -> (70 until 250).any { x -> bright(page.getColor(x, y)) } }
            val bands = ArrayList<IntRange>()
            for (y in rows) {
                val last = bands.lastOrNull()
                if (last != null && y <= last.last + 1) bands[bands.lastIndex] = last.first..y else bands += y..y
            }
            return bands
        }

        /** Apre il database, fa [act] e lo chiude come uscendo (il ViewModel se ne va). */
        fun openDatabase(name: String, act: (ImageComposeScene, Bitmap, () -> Bitmap) -> Unit = { _, _, _ -> }): Bitmap {
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
            try {
                frames(40)
                act(scene, picture()) { frames(15); picture() }
                frames(10)
                return picture().also {
                    File("build/desktop-screenshots", name).writeBytes(Image.makeFromBitmap(it).encodeToData(EncodedImageFormat.PNG)!!.bytes)
                }
            } finally {
                onWindowThread { scene.close() }
                owner.viewModelStore.clear()
                Thread.sleep(500)
            }
        }

        var openBands = emptyList<IntRange>()
        val closed = openDatabase("group_closed.png") { scene, page, after ->
            openBands = bands(page)
            // La prima fascia è l'intestazione del gruppo "Monday": un clic la chiude.
            click(scene, Offset(80f, (openBands[0].first + openBands[0].last) / 2f))
            val shut = bands(after())
            assertTrue("chiuso il gruppo, meno righe: $openBands -> $shut", shut.size < openBands.size)
        }
        val saved = runBlocking { AppDatabase.getInstance(context).pageDao().getById("giochi")?.tableGroupRulesJson }
        assertTrue("il gruppo chiuso è salvato nella vista: $saved", saved?.contains("\"collapsed\":[\"Monday\"]") == true)
        val again = openDatabase("group_reopened.png")
        assertEquals("riaperto il database il gruppo è ancora chiuso", bands(closed).size, bands(again).size)
        assertTrue("e ha meno righe di quando era aperto", bands(again).size < openBands.size)
    }

    private fun click(scene: ImageComposeScene, at: Offset) = onWindowThread {
        scene.sendPointerEvent(PointerEventType.Move, at, type = PointerType.Mouse)
        scene.sendPointerEvent(PointerEventType.Press, at, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Release, at, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
    }
}
