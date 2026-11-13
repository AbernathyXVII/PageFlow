package com.gabriele.notionlocal.desktop

import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.paragraph
import kotlin.math.abs
import com.gabriele.notionlocal.ui.screen.notePresses
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
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
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.screen.BlockDrop
import com.gabriele.notionlocal.ui.screen.BlockBox
import com.gabriele.notionlocal.ui.screen.DropPlace
import com.gabriele.notionlocal.ui.screen.dropFor
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Più blocchi col mouse sul PC** (dal 10/11/2026, chiesto dall'utente):
 * una pagina vera disegnata, con righe, un titolo più grande e un divisore.
 * Trascinando dalla prima riga a una sotto il titolo si scelgono i blocchi, e
 * Canc li elimina; un clic sul divisore e Backspace eliminano lui. Le righe
 * si trovano guardando dove la foto della pagina ha del testo.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopBlockSelectionTest {
    private val dir: File = Files.createTempDirectory("pageflow-blocks").toFile()
    private val context = DesktopContext(dir)
    private val ids = listOf("uno", "titolo", "due", "divisore", "tre")

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        val repo = PageRepository(AppDatabase.getInstance(context))
        if (AppDatabase.getInstance(context).pageDao().getById("blocchi") == null) repo.createPage(PageEntity(id = "blocchi", title = "Blocchi"))
        repo.saveBlock(BlockEntity(id = "uno", pageId = "blocchi", textJson = """[{"text":"Prima riga"}]""", orderIndex = 0))
        repo.saveBlock(BlockEntity(id = "titolo", pageId = "blocchi", textJson = """[{"text":"Un titolo grande","bold":true,"size":24}]""", orderIndex = 1))
        repo.saveBlock(BlockEntity(id = "due", pageId = "blocchi", textJson = """[{"text":"Seconda riga"}]""", orderIndex = 2))
        repo.saveBlock(BlockEntity(id = "divisore", pageId = "blocchi", type = BlockType.DIVIDER, orderIndex = 3))
        repo.saveBlock(BlockEntity(id = "tre", pageId = "blocchi", textJson = """[{"text":"Terza riga"}]""", orderIndex = 4))
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    private fun remainingIn(pageId: String): List<String> = runBlocking {
        AppDatabase.getInstance(context).blockDao().getBlocksForPageOnce(pageId).map { it.id }
    }

    private fun remaining(): List<String> = runBlocking {
        AppDatabase.getInstance(context).blockDao().getBlocksForPageOnce("blocchi").sortedBy { it.orderIndex }.map { it.id }
    }

    /** La pagina disegnata, con quello che serve per muovercisi: [act] riceve la scena e i suoi giri. */
    private fun withPage(pageId: String = "blocchi", act: (scene: ImageComposeScene, frames: (Int) -> Unit, picture: () -> Bitmap) -> Unit) {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 520, height = 700, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        // I clic segnati come nella finestra vera: i riquadri si aprono lì accanto.
                        Box(Modifier.fillMaxSize().notePresses()) {
                            PageEditorScreen(
                                pageId = pageId, factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
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
        try {
            frames(40)
            act(scene, frames) { onWindowThread { Bitmap.makeFromImage(scene.render(time)) } }
        } finally {
            onWindowThread { scene.close() }
        }
    }

    private fun waitForBlocks(expected: List<String>) {
        val end = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < end && remaining() != expected) Thread.sleep(50)
        assertEquals(expected, remaining())
    }

    /** Dalla prima riga, oltre il titolo grande, fino alla seconda: tre blocchi scelti, e Canc li elimina. */
    @Test
    fun draggingAcrossBlocksSelectsThemAndDeleteRemovesThem() = withPage { scene, frames, picture ->
        val rows = textRows(picture())
        assertEquals("righe trovate: $rows", 7, rows.size)
        val first = Offset(27f, rows[3].center())
        val second = Offset(27f, rows[5].center())
        onWindowThread {
            mouse(scene, PointerEventType.Move, first, pressed = false)
            mouse(scene, PointerEventType.Press, first, pressed = true)
        }
        frames(3)
        for (step in 1..6) {
            onWindowThread { mouse(scene, PointerEventType.Move, Offset(27f, first.y + (second.y - first.y) * step / 6f), pressed = true) }
            frames(1)
        }
        onWindowThread { mouse(scene, PointerEventType.Release, second, pressed = false) }
        frames(5)
        File("build/desktop-screenshots", "blocks_selected.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(picture()).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        onWindowThread { key(scene, Key.Delete) }
        frames(5)
        waitForBlocks(listOf("divisore", "tre"))
    }

    /** Un clic sul divisore lo sceglie, e Backspace lo elimina; le righe restano. */
    @Test
    fun clickingADividerAndBackspaceRemovesIt() = withPage { scene, frames, picture ->
        val rows = textRows(picture())
        val divider = Offset(27f, (rows[5].last + rows[6].first) / 2f)
        onWindowThread {
            mouse(scene, PointerEventType.Move, divider, pressed = false)
            mouse(scene, PointerEventType.Press, divider, pressed = true)
            mouse(scene, PointerEventType.Release, divider, pressed = false)
        }
        frames(5)
        onWindowThread { key(scene, Key.Backspace) }
        frames(5)
        waitForBlocks(listOf("uno", "titolo", "due", "tre"))
    }

    /**
     * **Ctrl+Z dentro una riga è l'Annulla della pagina** (trovato dall'utente
     * l'11/11/2026): tolto il divisore, col cursore in una riga Ctrl+Z lo
     * rimette (l'annulla del solo campo di testo non lo sapeva fare), e Ctrl+Y
     * lo toglie di nuovo.
     */
    @Test
    fun ctrlZInALineUndoesThePageAndCtrlYRedoes() = withPage { scene, frames, picture ->
        val rows = textRows(picture())
        val divider = Offset(27f, (rows[5].last + rows[6].first) / 2f)
        onWindowThread {
            mouse(scene, PointerEventType.Move, divider, pressed = false)
            mouse(scene, PointerEventType.Press, divider, pressed = true)
            mouse(scene, PointerEventType.Release, divider, pressed = false)
        }
        frames(5)
        onWindowThread { key(scene, Key.Backspace) }
        frames(5)
        waitForBlocks(listOf("uno", "titolo", "due", "tre"))
        val line = Offset(27f, rows[3].center())
        onWindowThread {
            mouse(scene, PointerEventType.Move, line, pressed = false)
            mouse(scene, PointerEventType.Press, line, pressed = true)
            mouse(scene, PointerEventType.Release, line, pressed = false)
        }
        frames(5)
        fun ctrl(key: Key, shift: Boolean = false) = onWindowThread {
            scene.sendKeyEvent(KeyEvent(key, KeyEventType.KeyDown, isCtrlPressed = true, isShiftPressed = shift))
            scene.sendKeyEvent(KeyEvent(key, KeyEventType.KeyUp, isCtrlPressed = true, isShiftPressed = shift))
        }
        ctrl(Key.Z)
        frames(5)
        waitForBlocks(ids)
        ctrl(Key.Y)
        frames(5)
        waitForBlocks(listOf("uno", "titolo", "due", "tre"))
        ctrl(Key.Z, shift = false)
        frames(5)
        waitForBlocks(ids)
    }

    /** Col cursore in una riga, Ctrl+A vela tutta la pagina (non solo il testo della riga), ed Esc la lascia. */
    @Test
    fun ctrlASelectsTheWholePage() = withPage { scene, frames, picture ->
        val rows = textRows(picture())
        val line = Offset(100f, rows[3].center())
        val empty = Offset(400f, rows[6].center())
        val before = picture().getColor(empty.x.toInt(), empty.y.toInt())
        onWindowThread {
            mouse(scene, PointerEventType.Move, line, pressed = false)
            mouse(scene, PointerEventType.Press, line, pressed = true)
            mouse(scene, PointerEventType.Release, line, pressed = false)
        }
        frames(5)
        onWindowThread {
            scene.sendKeyEvent(KeyEvent(Key.A, KeyEventType.KeyDown, isCtrlPressed = true))
            scene.sendKeyEvent(KeyEvent(Key.A, KeyEventType.KeyUp, isCtrlPressed = true))
        }
        frames(5)
        assertNotEquals("Ctrl+A vela anche le righe dove non c'era il cursore", before, picture().getColor(empty.x.toInt(), empty.y.toInt()))
        onWindowThread { key(scene, Key.Escape) }
        frames(5)
        assertEquals(before, picture().getColor(empty.x.toInt(), empty.y.toInt()))
        assertEquals(ids, remaining())
    }

    private fun IntRange.center(): Float = (first + last) / 2f

    /** Le altezze dove il margine a sinistra del testo (x 60–84) è diverso da [clean]: dove sono i sei puntini. */
    private fun dotsRows(bitmap: Bitmap, clean: Bitmap): List<Int> =
        (0 until bitmap.height).filter { y -> (DOTS_X).any { x -> bitmap.getColor(x, y) != clean.getColor(x, y) } }

    /**
     * **I sei puntini accanto al blocco sotto il mouse** (chiesto dall'utente
     * l'11/11/2026, come su Notion): compaiono nel margine accanto alla riga
     * sotto il mouse, passano al divisore, restano andandoci sopra, e il clic
     * apre il menu del divisore in un riquadro lì accanto: "Delete" lo toglie.
     */
    @Test
    fun theSixDotsOpenTheMenuOfTheBlockUnderTheMouse() = withPage { scene, frames, picture ->
        val clean = picture()
        val rows = textRows(clean)
        // Sul PC la pagina sta più a destra (chiesto dall'utente): a sinistra del testo c'è il posto per i puntini.
        val firstInk = (0 until 200).first { x -> bright(clean.getColor(x, rows[5].center().toInt())) }
        assertTrue("il testo comincia a $firstInk", firstInk >= 53)
        onWindowThread { mouse(scene, PointerEventType.Move, Offset(200f, rows[5].center()), pressed = false) }
        frames(5)
        val onLine = dotsRows(picture(), clean)
        assertTrue("puntini accanto a \"Seconda riga\": $onLine, riga ${rows[5]}", onLine.isNotEmpty() && abs(onLine.average() - rows[5].center()) < 6)
        val dividerY = (rows[5].last + rows[6].first) / 2f
        onWindowThread { mouse(scene, PointerEventType.Move, Offset(200f, dividerY), pressed = false) }
        frames(5)
        val onDivider = dotsRows(picture(), clean)
        assertTrue("puntini accanto al divisore: $onDivider", onDivider.isNotEmpty() && onDivider.average() > rows[5].last && onDivider.average() < rows[6].first)
        // Andandoci sopra restano.
        val dots = Offset(41f, onDivider.average().toFloat())
        onWindowThread { mouse(scene, PointerEventType.Move, dots, pressed = false) }
        frames(5)
        assertTrue("i puntini restano col mouse sopra", dotsRows(picture(), clean).isNotEmpty())
        onWindowThread {
            mouse(scene, PointerEventType.Press, dots, pressed = true)
            mouse(scene, PointerEventType.Release, dots, pressed = false)
        }
        frames(15)
        val menu = picture()
        File("build/desktop-screenshots", "block_menu.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(menu).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        // "Delete" è scritto col colore degli errori, rossastro: lì si clicca.
        val red = (0 until menu.height).flatMap { y ->
            (0 until menu.width).filter { x ->
                val c = menu.getColor(x, y)
                val r = (c shr 16) and 0xFF
                r > 180 && r - ((c shr 8) and 0xFF) > 40 && r - (c and 0xFF) > 40
            }.map { x -> Offset(x.toFloat(), y.toFloat()) }
        }
        assertTrue("nel menu c'è \"Delete\"", red.size > 20)
        assertTrue("il menu si apre accanto ai puntini", red.minOf { it.x } < 300 && red.minOf { it.y } > dots.y)
        val delete = Offset(red.map { it.x }.average().toFloat(), red.map { it.y }.average().toFloat())
        onWindowThread {
            mouse(scene, PointerEventType.Move, delete, pressed = false)
            mouse(scene, PointerEventType.Press, delete, pressed = true)
            mouse(scene, PointerEventType.Release, delete, pressed = false)
        }
        frames(5)
        waitForBlocks(listOf("uno", "titolo", "due", "tre"))
    }

    /**
     * Dove cade un blocco trascinato: sopra o sotto quello sotto il mouse, e
     * niente se resterebbe dov'è; col mouse sopra l'inizio o la fine del testo,
     * accanto (le colonne), ma non dal margine dei puntini né per un gruppo di colonne.
     */
    @Test
    fun aDraggedBlockLandsAboveOrBelowOrBesideTheOneUnderTheMouse() {
        val blocks = listOf("a", "b", "c", "d").mapIndexed { i, id -> BlockEntity(id = id, pageId = "p", orderIndex = i) } +
            BlockEntity(id = "g", pageId = "p", type = BlockType.COLUMN_LIST, orderIndex = 4) +
            BlockEntity(id = "k", pageId = "p", parentBlockId = "g", type = BlockType.COLUMN, orderIndex = 0) +
            BlockEntity(id = "x", pageId = "p", parentBlockId = "k", orderIndex = 0)
        val box = BlockBox(100f, 120f, 20f, 480f)
        fun drop(dragged: String, target: String, x: Float, y: Float) = dropFor(dragged, target, Offset(x, y), box, 60f, blocks)
        assertEquals(BlockDrop("d", DropPlace.AFTER, 20f, 120f, 460f, vertical = false), drop("b", "d", 200f, 115f))
        assertEquals(BlockDrop("d", DropPlace.BEFORE, 20f, 100f, 460f, vertical = false), drop("b", "d", 200f, 105f))
        assertEquals(DropPlace.BEFORE, drop("c", "a", 200f, 105f)?.place)
        // Su se stesso, o subito sopra o sotto: resta dov'è.
        assertEquals(null, drop("b", "b", 200f, 115f))
        assertEquals(null, drop("b", "a", 200f, 115f))
        assertEquals(null, drop("b", "c", 200f, 105f))
        // Sopra l'inizio o la fine del testo: accanto, con la linea verticale.
        assertEquals(BlockDrop("d", DropPlace.LEFT, 14f, 100f, 20f, vertical = true), drop("b", "d", 30f, 110f))
        assertEquals(BlockDrop("d", DropPlace.RIGHT, 486f, 100f, 20f, vertical = true), drop("b", "d", 470f, 110f))
        // Dal margine dei puntini (prima del testo) resta sopra o sotto.
        assertEquals(DropPlace.BEFORE, drop("b", "d", 10f, 105f)?.place)
        // Un gruppo di colonne non va accanto, né dentro se stesso.
        assertEquals(DropPlace.BEFORE, drop("g", "a", 30f, 105f)?.place)
        assertEquals(null, drop("g", "x", 200f, 105f))
        // Accanto a un gruppo che ha già quattro colonne la linea non è verticale.
        val full = blocks + (1..3).flatMap { i ->
            listOf(
                BlockEntity(id = "k$i", pageId = "p", parentBlockId = "g", type = BlockType.COLUMN, orderIndex = i),
                BlockEntity(id = "x$i", pageId = "p", parentBlockId = "k$i", orderIndex = 0)
            )
        }
        assertEquals(DropPlace.BEFORE, dropFor("a", "x", Offset(30f, 105f), box, 60f, full)?.place)
        assertEquals(DropPlace.LEFT, dropFor("a", "x", Offset(30f, 105f), box, 60f, blocks)?.place)
    }

    /** Tiene premuti i puntini ad [from] (col mouse prima sulla riga, perché compaiano) e trascina fino a [to], a piccoli passi. */
    private fun dragDots(scene: ImageComposeScene, frames: (Int) -> Unit, picture: () -> Bitmap, lineY: Float, to: Float): Int {
        onWindowThread { mouse(scene, PointerEventType.Move, Offset(200f, lineY), pressed = false) }
        frames(3)
        val dots = Offset(41f, lineY)
        onWindowThread {
            mouse(scene, PointerEventType.Move, dots, pressed = false)
            mouse(scene, PointerEventType.Press, dots, pressed = true)
        }
        frames(2)
        val steps = (abs(to - lineY) / 4f).toInt().coerceAtLeast(1)
        for (i in 1..steps) {
            onWindowThread { mouse(scene, PointerEventType.Move, Offset(200f, lineY + (to - lineY) * i / steps), pressed = true) }
            frames(1)
        }
        // La linea blu di dove cadrà, prima di lasciare.
        val shot = picture()
        File("build/desktop-screenshots", "block_drag.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(shot).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        val blue = (0 until shot.height).sumOf { y ->
            (53 until 400).count { x -> shot.getColor(x, y).let { c -> ((c shr 16) and 0xFF) < 80 && (c and 0xFF) > 180 && ((c shr 8) and 0xFF) in 100..160 } }
        }
        onWindowThread { mouse(scene, PointerEventType.Release, Offset(200f, to), pressed = false) }
        frames(5)
        return blue
    }

    /**
     * **Trascinando i sei puntini il blocco si sposta** (chiesto dall'utente
     * il 12/11/2026, come su Notion): la prima riga, presa per i puntini e
     * portata sotto l'ultima, va in fondo; una linea blu mostra dove.
     */
    @Test
    fun draggingTheSixDotsMovesALineToTheBottom() = withPage { scene, frames, picture ->
        val (prima, _, _, terza) = textRows(picture()).takeLast(4)
        val blue = dragDots(scene, frames, picture, prima.center(), terza.last + 3f)
        assertTrue("la linea blu mentre si trascina: $blue punti", blue > 100)
        waitForBlocks(listOf("titolo", "due", "divisore", "tre", "uno"))
    }

    /** Il divisore, preso per i puntini e portato sopra la prima riga, va in cima. */
    @Test
    fun draggingTheSixDotsMovesTheDividerToTheTop() = withPage { scene, frames, picture ->
        val (prima, _, seconda, terza) = textRows(picture()).takeLast(4)
        dragDots(scene, frames, picture, (seconda.last + terza.first) / 2f, prima.first - 1f)
        waitForBlocks(listOf("divisore", "uno", "titolo", "due", "tre"))
    }

    /**
     * **La rotella mentre si trascina fa scorrere la pagina** (trovato
     * dall'utente il 12/11/2026: "non posso scrollare con il mouse"): la
     * prima riga, presa per i puntini, scende con la rotella e va a finire
     * fra righe che prima non si vedevano.
     */
    @Test
    fun theWheelScrollsWhileDraggingABlock() {
        runBlocking {
            val repo = PageRepository(AppDatabase.getInstance(context))
            repo.createPage(PageEntity(id = "rotella", title = "Rotella"))
            for (i in 0 until 60) repo.saveBlock(BlockEntity(id = "rotella-$i", pageId = "rotella", textJson = """[{"text":"Riga numero $i"}]""", orderIndex = i))
        }
        withPage("rotella") { scene, frames, picture ->
            val rows = textRows(picture())
            val shown = rows.size
            val lineY = rows[3].center()
            onWindowThread { mouse(scene, PointerEventType.Move, Offset(200f, lineY), pressed = false) }
            frames(3)
            val dots = Offset(41f, lineY)
            val at = Offset(200f, lineY + 40f)
            onWindowThread {
                mouse(scene, PointerEventType.Move, dots, pressed = false)
                mouse(scene, PointerEventType.Press, dots, pressed = true)
            }
            frames(2)
            for (step in 1..10) {
                onWindowThread { mouse(scene, PointerEventType.Move, Offset(dots.x + (at.x - dots.x) * step / 10f, dots.y + (at.y - dots.y) * step / 10f), pressed = true) }
                frames(1)
            }
            onWindowThread {
                scene.sendPointerEvent(
                    PointerEventType.Scroll, at, scrollDelta = Offset(0f, 30f), type = PointerType.Mouse,
                    buttons = PointerButtons(isPrimaryPressed = true)
                )
            }
            frames(10)
            onWindowThread { mouse(scene, PointerEventType.Move, Offset(at.x, at.y + 2f), pressed = true) }
            frames(2)
            onWindowThread { mouse(scene, PointerEventType.Release, Offset(at.x, at.y + 2f), pressed = false) }
            frames(5)
            val end = System.currentTimeMillis() + 5_000
            fun place() = runBlocking {
                AppDatabase.getInstance(context).blockDao().getBlocksForPageOnce("rotella").sortedBy { it.orderIndex }.indexOfFirst { it.id == "rotella-0" }
            }
            while (System.currentTimeMillis() < end && place() < shown) Thread.sleep(50)
            assertTrue("la prima riga è finita al posto ${place()}, sullo schermo ce n'erano $shown", place() >= shown)
        }
    }

    /**
     * **Un database si sposta tenendo i puntini del suo titolo** (chiesto
     * dall'utente il 12/11/2026): uno vero, con una proprietà e delle righe,
     * portato sopra la prima riga va in cima, e poi sotto l'ultima in fondo.
     */
    @Test
    fun draggingTheSixDotsOfADatabaseMovesIt() {
        runBlocking {
            val db = AppDatabase.getInstance(context)
            val repo = PageRepository(db)
            repo.createPage(PageEntity(id = "spostadb", title = "Sposta"))
            repo.createPage(PageEntity(id = "elenco", title = "TITOLO", isDatabase = true, parentId = "spostadb"))
            db.databaseDao().insertColumn(DatabaseColumnEntity(id = "voto", pageId = "elenco", name = "Voto", type = ColumnType.NUMBER, orderIndex = 0))
            db.databaseDao().insertRow(DatabaseRowEntity(id = "r1", pageId = "elenco", title = "Persona 5", orderIndex = 0))
            db.databaseDao().insertRow(DatabaseRowEntity(id = "r2", pageId = "elenco", title = "Zelda", orderIndex = 1))
            repo.saveBlock(BlockEntity(id = "sopra", pageId = "spostadb", textJson = """[{"text":"SOPRA"}]""", orderIndex = 0))
            repo.saveBlock(BlockEntity(id = "tabella", pageId = "spostadb", type = BlockType.DATABASE_LINK, linkedPageId = "elenco", orderIndex = 1))
            repo.saveBlock(BlockEntity(id = "sotto", pageId = "spostadb", textJson = """[{"text":"SOTTO"}]""", orderIndex = 2))
        }
        fun order() = runBlocking { AppDatabase.getInstance(context).blockDao().getBlocksForPageOnce("spostadb").sortedBy { it.orderIndex }.map { it.id } }
        fun waitFor(expected: List<String>) {
            val end = System.currentTimeMillis() + 5_000
            while (System.currentTimeMillis() < end && order() != expected) Thread.sleep(50)
            assertEquals(expected, order())
        }
        withPage("spostadb") { scene, frames, picture ->
            frames(20)
            val clean = picture()
            val rows = textRows(clean)
            val title = rows[4]
            val blue = dragDots(scene, frames, picture, title.center(), rows[3].first - 1f)
            assertTrue("la linea blu mentre si trascina: $blue punti", blue > 100)
            waitFor(listOf("tabella", "sopra", "sotto"))
            frames(10)
            val now = textRows(picture())
            dragDots(scene, frames, picture, now[3].center(), now.last().last + 3f)
            waitFor(listOf("sopra", "sotto", "tabella"))
        }
    }

    /**
     * **Ogni blocco si elimina dai suoi sei puntini** (chiesto dall'utente il
     * 12/11/2026: le righe di testo non avevano "Delete"): "Seconda riga",
     * dal suo menu, se ne va.
     */
    @Test
    fun aTextLineIsDeletedFromItsMenu() = withPage { scene, frames, picture ->
        val clean = picture()
        val rows = textRows(clean)
        onWindowThread { mouse(scene, PointerEventType.Move, Offset(200f, rows[5].center()), pressed = false) }
        frames(5)
        val dots = Offset(41f, dotsRows(picture(), clean).average().toFloat())
        onWindowThread {
            mouse(scene, PointerEventType.Move, dots, pressed = false)
            mouse(scene, PointerEventType.Press, dots, pressed = true)
            mouse(scene, PointerEventType.Release, dots, pressed = false)
        }
        frames(15)
        val menu = picture()
        File("build/desktop-screenshots", "text_line_menu.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(menu).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        val red = (0 until menu.height).flatMap { y ->
            (0 until menu.width).filter { x ->
                val c = menu.getColor(x, y)
                val r = (c shr 16) and 0xFF
                r > 180 && r - ((c shr 8) and 0xFF) > 40 && r - (c and 0xFF) > 40
            }.map { x -> Offset(x.toFloat(), y.toFloat()) }
        }
        assertTrue("nel menu della riga c'è \"Delete\"", red.size > 20)
        val delete = Offset(red.map { it.x }.average().toFloat(), red.map { it.y }.average().toFloat())
        onWindowThread {
            mouse(scene, PointerEventType.Move, delete, pressed = false)
            mouse(scene, PointerEventType.Press, delete, pressed = true)
            mouse(scene, PointerEventType.Release, delete, pressed = false)
        }
        frames(5)
        waitForBlocks(listOf("uno", "titolo", "divisore", "tre"))
    }

    /**
     * **Sul PC la pagina sta più a destra, ma la copertina no** (chiesto
     * dall'utente l'11/11/2026): il testo comincia dopo il margine dei
     * puntini, la copertina va ancora da un bordo all'altro.
     */
    @Test
    fun theCoverStillReachesBothEdges() {
        runBlocking {
            val bitmap = Bitmap().apply { allocN32Pixels(400, 200) }
            org.jetbrains.skia.Canvas(bitmap).clear(0xFFCC2233.toInt())
            PageImageStore(context).fileFor("copertina.png")
                .writeBytes(org.jetbrains.skia.Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            val repo = PageRepository(AppDatabase.getInstance(context))
            repo.createPage(PageEntity(id = "copertina", title = "Copertina", coverImage = "copertina.png"))
            repo.saveBlock(BlockEntity(id = "riga", pageId = "copertina", textJson = """[{"text":"Sotto la copertina"}]""", orderIndex = 0))
        }
        withPage("copertina") { _, frames, picture ->
            frames(20)
            val page = picture()
            fun red(x: Int, y: Int) = page.getColor(x, y).let { c -> ((c shr 16) and 0xFF) > 150 && ((c shr 8) and 0xFF) < 90 }
            assertTrue("la copertina comincia al bordo sinistro", red(2, 100))
            assertTrue("la copertina arriva al bordo destro", red(page.width - 3, 100))
            val line = textRows(page).last()
            val firstInk = (0 until 300).first { x -> bright(page.getColor(x, line.center().toInt())) }
            assertTrue("il testo comincia a $firstInk", firstInk >= 53)
        }
    }

    /**
     * **La copertina si allunga trascinandone il bordo di sotto** (13/11/2026,
     * chiesto dall'utente, solo sul PC): 100 punti più giù la striscia è più
     * alta di 100, l'altezza si salva fra quelle del PC e l'inquadratura del
     * telefono non cambia.
     */
    @Test
    fun theCoverGrowsByDraggingItsBottomEdge() {
        runBlocking {
            // Metà rossa e metà blu: allungando la striscia se ne vede di più.
            val bitmap = Bitmap().apply { allocN32Pixels(400, 400) }
            val canvas = org.jetbrains.skia.Canvas(bitmap)
            canvas.clear(0xFFCC2233.toInt())
            canvas.drawRect(org.jetbrains.skia.Rect.makeXYWH(0f, 200f, 400f, 200f), org.jetbrains.skia.Paint().apply { color = 0xFF2233CC.toInt() })
            PageImageStore(context).fileFor("alta.png")
                .writeBytes(org.jetbrains.skia.Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            val repo = PageRepository(AppDatabase.getInstance(context))
            repo.createPage(PageEntity(id = "alta", title = "Alta", coverImage = "alta.png", coverScale = 1.5f, coverOffsetY = 0.1f))
            repo.saveBlock(BlockEntity(id = "sotto", pageId = "alta", textJson = """[{"text":"Sotto la copertina"}]""", orderIndex = 0))
        }
        fun stored() = runBlocking { AppDatabase.getInstance(context).pageDao().getById("alta")!! }
        withPage("alta") { scene, frames, picture ->
            frames(20)
            fun coverRows(page: Bitmap) = (0 until page.height).filter { y ->
                page.getColor(10, y).let { c -> ((c shr 16) and 0xFF) > 150 || (c and 0xFF) > 150 } && ((page.getColor(10, y) shr 8) and 0xFF) < 90
            }
            val before = coverRows(picture())
            assertTrue("una copertina alta 160: ${before.size}", before.size in 155..165)
            val edge = Offset(260f, before.last() - 3f)
            onWindowThread {
                mouse(scene, PointerEventType.Move, edge, pressed = false)
                mouse(scene, PointerEventType.Press, edge, pressed = true)
            }
            for (step in 1..20) {
                onWindowThread { mouse(scene, PointerEventType.Move, edge + Offset(0f, step * 5f), pressed = true) }
                frames(1)
            }
            onWindowThread { mouse(scene, PointerEventType.Release, edge + Offset(0f, 100f), pressed = false) }
            frames(10)
            val after = picture()
            File("build/desktop-screenshots", "cover_taller.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(after).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            val grown = coverRows(after)
            assertTrue("alta ${grown.size} invece di ${before.size}", grown.size - before.size in 95..105)
            val page = stored()
            val height = page.paragraph.pcCoverHeight
            assertTrue("salvata $height", height != null && abs(height - 260f) < 3f)
            // Il telefono resta com'era.
            assertEquals(1.5f, page.coverScale)
            assertEquals(0.1f, page.coverOffsetY)

            // "Reposition" sul PC: la rotella ingrandisce, e "Save" lo scrive solo per il PC.
            val clean = picture()
            val middle = Offset(260f, grown.first() + 60f)
            onWindowThread {
                mouse(scene, PointerEventType.Move, middle, pressed = false)
                mouse(scene, PointerEventType.Press, middle, pressed = true)
                mouse(scene, PointerEventType.Release, middle, pressed = false)
            }
            frames(10)
            val panel = picture()
            File("build/desktop-screenshots", "cover_panel.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(panel).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            fun white(c: Int) = ((c shr 16) and 0xFF) > 200 && ((c shr 8) and 0xFF) > 200 && (c and 0xFF) > 200
            // Le righe di scritta bianca del riquadro: "Cover", "Upload", "Reposition".
            val textRows = (0 until panel.height).filter { y -> (0 until panel.width).any { x -> white(panel.getColor(x, y)) && panel.getColor(x, y) != clean.getColor(x, y) } }
            val groups = textRows.fold(mutableListOf<MutableList<Int>>()) { acc, y -> if (acc.isNotEmpty() && y - acc.last().last() <= 2) acc.last() += y else acc += mutableListOf(y); acc }
            assertTrue("righe del riquadro: ${groups.map { it.first()..it.last() }}", groups.size >= 3)
            val repositionY = groups[2].let { (it.first() + it.last()) / 2 }
            val repositionX = (0 until panel.width).first { x -> white(panel.getColor(x, repositionY)) && panel.getColor(x, repositionY) != clean.getColor(x, repositionY) }
            onWindowThread {
                val at = Offset(repositionX + 10f, repositionY.toFloat())
                mouse(scene, PointerEventType.Move, at, pressed = false)
                mouse(scene, PointerEventType.Press, at, pressed = true)
                mouse(scene, PointerEventType.Release, at, pressed = false)
            }
            frames(10)
            repeat(3) {
                onWindowThread {
                    scene.sendPointerEvent(PointerEventType.Move, middle, type = PointerType.Mouse)
                    scene.sendPointerEvent(PointerEventType.Scroll, middle, scrollDelta = Offset(0f, -1f), type = PointerType.Mouse)
                }
                frames(2)
            }
            val zoomed = picture()
            File("build/desktop-screenshots", "cover_reposition_zoomed.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(zoomed).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            // "Save": l'ultimo pulsante in basso a destra della copertina.
            val save = Offset(zoomed.width - 40f, grown.last() - 28f)
            onWindowThread {
                mouse(scene, PointerEventType.Move, save, pressed = false)
                mouse(scene, PointerEventType.Press, save, pressed = true)
                mouse(scene, PointerEventType.Release, save, pressed = false)
            }
            frames(10)
            val saved = stored()
            val scale = saved.paragraph.pcCoverScale
            assertTrue("zoom del PC $scale", scale != null && abs(scale - 1.5f * 1.1f * 1.1f * 1.1f) < 0.02f)
            assertEquals("lo zoom del telefono resta", 1.5f, saved.coverScale)
        }
    }

    /**
     * **I sei puntini a metà delle lettere** (trovato dall'utente l'11/11/2026:
     * "l'icona dei sei puntini è storta", e sul database stava sopra il
     * titolo): su una riga in maiuscolo stanno a metà delle lettere; su un
     * database dentro la pagina a metà del titolo, subito a sinistra.
     */
    @Test
    fun theSixDotsSitOnTheLettersAndOnTheDatabaseTitle() {
        runBlocking {
            val db = AppDatabase.getInstance(context)
            val repo = PageRepository(db)
            repo.createPage(PageEntity(id = "allineata", title = "Allineata"))
            repo.createPage(PageEntity(id = "registro", title = "TITOLO", isDatabase = true, parentId = "allineata"))
            repo.saveBlock(BlockEntity(id = "maiuscole", pageId = "allineata", textJson = """[{"text":"CIAO"}]""", orderIndex = 0))
            repo.saveBlock(BlockEntity(id = "tabella", pageId = "allineata", type = BlockType.DATABASE_LINK, linkedPageId = "registro", orderIndex = 1))
        }
        withPage("allineata") { scene, frames, picture ->
            frames(20)
            val clean = picture()
            val rows = textRows(clean)
            File("build/desktop-screenshots", "dots_aligned_clean.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(clean).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            val ciao = rows[3]
            val title = rows[4]
            fun dotsAt(y: Float): List<Int> {
                onWindowThread { mouse(scene, PointerEventType.Move, Offset(200f, y), pressed = false) }
                frames(5)
                return dotsRows(picture(), clean)
            }
            val onLetters = dotsAt(ciao.center())
            assertTrue("puntini $onLetters, lettere $ciao", onLetters.isNotEmpty() && abs(onLetters.average() - ciao.center()) <= 1.5)
            val onTitle = dotsAt(title.center())
            val shown = picture()
            File("build/desktop-screenshots", "dots_database_title.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(shown).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            assertTrue("puntini $onTitle, titolo $title", onTitle.isNotEmpty() && abs(onTitle.average() - title.center()) <= 1.5)
            // Subito a sinistra del titolo, senza toccarlo: dal 11/11/2026 un poco più staccati
            // (chiesto dall'utente), 11 fra l'ultimo puntino e il titolo invece di 8.
            val dotsRight = DOTS_X.last { x -> (title.first..title.last).any { y -> shown.getColor(x, y) != clean.getColor(x, y) } }
            val titleLeft = (0 until 300).first { x -> (title.first..title.last).any { y -> bright(clean.getColor(x, y)) } }
            assertTrue("puntini fino a $dotsRight, titolo da $titleLeft", dotsRight < titleLeft - 9)
        }
    }

    /**
     * **I puntini seguono il database che si sposta sotto il mouse fermo**
     * (trovato dall'utente il 12/11/2026: "un'icona dei sei puntini è sbucata
     * dal nulla a sinistra del primo database", a metà del suo elenco). Col
     * mouse a sinistra di una riga dell'elenco i puntini stanno sul titolo del
     * database; col mouse sopra di loro, cinque righe nuove sopra il database
     * lo spostano in giù, e i puntini vanno con lui invece di restare lì.
     */
    @Test
    fun theDotsOfAListDatabaseFollowItWhenItMoves() {
        runBlocking {
            val db = AppDatabase.getInstance(context)
            val repo = PageRepository(db)
            repo.createPage(PageEntity(id = "conelenco", title = "Con elenco"))
            repo.createPage(PageEntity(id = "quaderno", title = "QUADERNO", isDatabase = true, parentId = "conelenco", databaseLayout = DatabaseLayout.LIST))
            for (i in 0 until 8) db.databaseDao().insertRow(DatabaseRowEntity(id = "riga$i", pageId = "quaderno", title = "Voce numero $i", orderIndex = i))
            repo.saveBlock(BlockEntity(id = "elenco", pageId = "conelenco", type = BlockType.DATABASE_LINK, linkedPageId = "quaderno", orderIndex = 0))
        }
        withPage("conelenco") { scene, frames, picture ->
            frames(20)
            val clean = picture()
            val rows = textRows(clean)
            val middle = rows[rows.size - 4]
            onWindowThread { mouse(scene, PointerEventType.Move, Offset(200f, middle.center()), pressed = false) }
            frames(5)
            val fromRow = dotsRows(picture(), clean)
            // A metà di una riga vera, sopra l'elenco (il titolo del database), non accanto alla riga del mouse.
            val title = rows.firstOrNull { abs(fromRow.average() - it.center()) <= 2 }
            assertTrue("puntini $fromRow, righe $rows (mouse sulla riga $middle)", fromRow.isNotEmpty() && title != null && title.last < middle.first - 100)
            // Il mouse va sopra i puntini e resta lì.
            onWindowThread { mouse(scene, PointerEventType.Move, Offset(41f, fromRow.average().toFloat()), pressed = false) }
            frames(5)
            runBlocking {
                val repo = PageRepository(AppDatabase.getInstance(context))
                AppDatabase.getInstance(context).blockDao().shiftOrderIndexes("conelenco", null, 0, 5)
                for (i in 0 until 5) repo.saveBlock(BlockEntity(id = "nuova$i", pageId = "conelenco", textJson = """[{"text":"Riga nuova $i"}]""", orderIndex = i))
            }
            frames(20)
            val moved = picture()
            File("build/desktop-screenshots", "list_database_moved.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(moved).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            val after = textRows(moved)
            // I puntini: nel margine prima del testo (x 58–77, prima del titolo del database che comincia a 82).
            val background = moved.getColor(5, moved.height - 5)
            val dots = (TOP_BAR until moved.height).filter { y -> (25 until 45).any { x -> moved.getColor(x, y) != background } }
            val near = after.any { band -> abs(dots.average() - band.center()) <= 3 }
            assertTrue("i puntini $dots non stanno a metà di una riga (righe $after)", dots.isEmpty() || near)
        }
    }

    /**
     * **Tanti database di fila, come nella pagina dell'utente** (12/11/2026:
     * "c'è ancora", i sei puntini a metà dell'elenco del primo database): il
     * primo in vista elenco con tredici righe, poi tre tabelle. Col mouse sulle
     * righe del primo, anche dopo essere scesi e risaliti con la rotella, i
     * puntini stanno sul suo titolo.
     */
    @Test
    fun withManyDatabasesTheDotsStayOnTheFirstTitle() {
        runBlocking {
            val db = AppDatabase.getInstance(context)
            val repo = PageRepository(db)
            repo.createPage(PageEntity(id = "molti", title = "Molti"))
            for (d in 0 until 4) {
                repo.createPage(PageEntity(id = "base$d", title = "BASE $d", isDatabase = true, parentId = "molti", databaseLayout = if (d == 0) DatabaseLayout.LIST else null))
                for (i in 0 until (if (d == 0) 13 else 3)) db.databaseDao().insertRow(DatabaseRowEntity(id = "b$d-r$i", pageId = "base$d", title = "Riga $d.$i", orderIndex = i))
                repo.saveBlock(BlockEntity(id = "tab$d", pageId = "molti", type = BlockType.DATABASE_LINK, linkedPageId = "base$d", orderIndex = d))
            }
            // Sotto i database, un gruppo di colonne con qualche riga (come nella pagina dell'utente dopo le prove).
            repo.saveBlock(BlockEntity(id = "gruppo", pageId = "molti", type = BlockType.COLUMN_LIST, orderIndex = 4))
            for (k in 0 until 3) {
                repo.saveBlock(BlockEntity(id = "col$k", pageId = "molti", parentBlockId = "gruppo", type = BlockType.COLUMN, orderIndex = k))
                for (i in 0 until 6) repo.saveBlock(BlockEntity(id = "col$k-$i", pageId = "molti", parentBlockId = "col$k", textJson = """[{"text":"Colonna $k riga $i"}]""", orderIndex = i))
            }
        }
        withPage("molti") { scene, frames, picture ->
            frames(20)
            val clean = picture()
            File("build/desktop-screenshots", "many_databases.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(clean).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            val rows = textRows(clean)
            val pageTitle = rows.filter { it.first > TOP_BAR }.maxBy { it.last - it.first }
            fun hover(y: Float): List<Int> {
                onWindowThread { mouse(scene, PointerEventType.Move, Offset(200f, y), pressed = false) }
                frames(5)
                return dotsRows(picture(), clean)
            }
            val firstTitle = rows.first { it.first > pageTitle.last + 40 }
            val checks = rows.filter { it.first > firstTitle.last + 40 && it.last < 650 }.map { band ->
                val dots = hover(band.center())
                band to dots
            }
            val wrong = checks.filter { (_, dots) -> dots.isNotEmpty() && abs(dots.average() - firstTitle.center()) > 3 }
            File("build/desktop-screenshots", "many_databases_hover.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(picture()).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            assertTrue("titolo $firstTitle; puntini fuori posto col mouse su: ${wrong.map { (b, d) -> "$b -> ${d.firstOrNull()}..${d.lastOrNull()}" }}", wrong.isEmpty())
            // Giù e su con la rotella, poi di nuovo sulle righe del primo database.
            repeat(12) {
                onWindowThread { scene.sendPointerEvent(PointerEventType.Scroll, Offset(200f, 400f), scrollDelta = Offset(0f, 3f), type = PointerType.Mouse) }
                frames(3)
            }
            File("build/desktop-screenshots", "many_databases_down.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(picture()).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            for (y in listOf(300f, 350f, 400f, 450f)) {
                onWindowThread { mouse(scene, PointerEventType.Move, Offset(200f, y), pressed = false) }
                frames(2)
            }
            repeat(40) {
                onWindowThread { scene.sendPointerEvent(PointerEventType.Scroll, Offset(200f, 400f), scrollDelta = Offset(0f, -3f), type = PointerType.Mouse) }
                frames(3)
            }
            frames(10)
            val back = picture()
            File("build/desktop-screenshots", "many_databases_back.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(back).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            val again = checks.map { (band, _) -> band to hover(band.center()) }
            val wrongAgain = again.filter { (_, dots) -> dots.isNotEmpty() && abs(dots.average() - firstTitle.center()) > 3 }
            assertTrue("dopo la rotella, puntini fuori posto col mouse su: ${wrongAgain.map { (b, d) -> "$b -> ${d.firstOrNull()}..${d.lastOrNull()}" }}", wrongAgain.isEmpty())
        }
    }

    /**
     * **La barretta di scorrimento si trascina col mouse** (chiesto
     * dall'utente il 13/11/2026): col mouse sul bordo destro compare; presa e
     * portata in giù, la pagina la segue; un clic in fondo alla corsia porta la
     * pagina in fondo.
     */
    @Test
    fun theScrollBarCanBeDragged() {
        runBlocking {
            val repo = PageRepository(AppDatabase.getInstance(context))
            repo.createPage(PageEntity(id = "barra", title = "Barra"))
            for (i in 0 until 80) repo.saveBlock(BlockEntity(id = "barra-$i", pageId = "barra", textJson = """[{"text":"Riga numero $i"}]""", orderIndex = i))
        }
        withPage("barra") { scene, frames, picture ->
            frames(20)
            val before = picture()
            val background = before.getColor(5, before.height - 5)
            /** Dove sta la barretta: le altezze in cui il bordo destro non è del colore del fondo. */
            fun thumb(bitmap: Bitmap): List<Int> = (0 until bitmap.height).filter { y -> (510 until 519).any { x -> bitmap.getColor(x, y) != background } }
            assertTrue("prima di avvicinarsi la barretta non si vede", thumb(before).isEmpty())
            onWindowThread { mouse(scene, PointerEventType.Move, Offset(514f, 120f), pressed = false) }
            frames(5)
            val shown = picture()
            val bar = thumb(shown)
            assertTrue("col mouse sul bordo la barretta compare", bar.isNotEmpty())
            val start = bar.average().toFloat()
            onWindowThread {
                mouse(scene, PointerEventType.Move, Offset(514f, start), pressed = false)
                mouse(scene, PointerEventType.Press, Offset(514f, start), pressed = true)
            }
            frames(2)
            for (step in 1..10) {
                onWindowThread { mouse(scene, PointerEventType.Move, Offset(514f, start + 20f * step), pressed = true) }
                frames(1)
            }
            onWindowThread { mouse(scene, PointerEventType.Release, Offset(514f, start + 200f), pressed = false) }
            frames(5)
            val dragged = picture()
            File("build/desktop-screenshots", "scrollbar_dragged.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(dragged).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            val moved = thumb(dragged)
            assertTrue("la barretta è scesa: da $start a ${moved.average()}", moved.isNotEmpty() && moved.average() > start + 120)
            assertNotEquals("la pagina si è mossa", textRows(before).take(6), textRows(dragged).take(6))
            // Un clic in fondo alla corsia: la pagina va in fondo, e la barretta con lei.
            onWindowThread {
                mouse(scene, PointerEventType.Move, Offset(514f, before.height - 12f), pressed = false)
                mouse(scene, PointerEventType.Press, Offset(514f, before.height - 12f), pressed = true)
                mouse(scene, PointerEventType.Release, Offset(514f, before.height - 12f), pressed = false)
            }
            frames(10)
            val bottom = thumb(picture())
            assertTrue("dopo il clic in fondo la barretta è in fondo: ${bottom.lastOrNull()}", bottom.isNotEmpty() && bottom.last() > before.height - 30)
        }
    }

    /** Scorrendo con la rotella, col mouse fermo, i puntini passano alla riga che ora c'è sotto. */
    @Test
    fun theSixDotsFollowTheWheel() {
        runBlocking {
            val repo = PageRepository(AppDatabase.getInstance(context))
            if (AppDatabase.getInstance(context).pageDao().getById("alterna") == null) repo.createPage(PageEntity(id = "alterna", title = "Alterna"))
            for (i in 0 until 60) {
                // Righe alte e basse alternate: dopo lo scorrimento i puntini fermi non starebbero su una riga.
                val size = if (i % 3 == 0) ""","size":30""" else ""
                repo.saveBlock(BlockEntity(id = "alterna-$i", pageId = "alterna", textJson = """[{"text":"Riga numero $i"$size}]""", orderIndex = i))
            }
        }
        withPage("alterna") { scene, frames, picture ->
            val clean = picture()
            val rows = textRows(clean)
            val at = Offset(200f, rows[6].center())
            onWindowThread { mouse(scene, PointerEventType.Move, at, pressed = false) }
            frames(5)
            val before = dotsRows(picture(), clean).average()
            onWindowThread { scene.sendPointerEvent(PointerEventType.Scroll, at, scrollDelta = Offset(0f, 1f), type = PointerType.Mouse) }
            frames(10)
            val scrolled = picture()
            val after = textRows(scrolled)
            assertNotEquals("la pagina non si è mossa", rows.take(8), after.take(8))
            val under = after.first { at.y.toInt() in (it.first - 8)..(it.last + 8) }
            // I puntini sono grigi su fondo scuro: i punti del margine più chiari del fondo.
            val dots = (TOP_BAR until scrolled.height).filter { y -> DOTS_X.any { x -> ((scrolled.getColor(x, y) shr 16) and 0xFF) > 70 } }
            assertTrue("puntini $dots (prima a $before) accanto alla riga sotto il mouse $under", dots.isNotEmpty() && abs(dots.average() - under.center()) < 6)
        }
    }

    /**
     * **La rotella fa scorrere anche mentre si seleziona** (trovato dall'utente
     * il 10/11/2026): col tasto del mouse premuto su una riga, la rotella
     * prima non muoveva la pagina.
     */
    @Test
    fun theWheelScrollsWhileSelecting() {
        runBlocking {
            val repo = PageRepository(AppDatabase.getInstance(context))
            if (AppDatabase.getInstance(context).pageDao().getById("lunga") == null) repo.createPage(PageEntity(id = "lunga", title = "Lunga"))
            for (i in 0 until 60) repo.saveBlock(BlockEntity(id = "lunga-$i", pageId = "lunga", textJson = """[{"text":"Riga numero $i"}]""", orderIndex = i))
        }
        for (acrossBlocks in listOf(false, true)) withPage("lunga") { scene, frames, picture ->
            val before = textRows(picture())
            val from = Offset(30f, before[5].center())
            // Prima si seleziona del testo trascinando nella riga (o, la seconda volta, fino a quella sotto).
            val at = if (acrossBlocks) Offset(47f, before[7].center()) else Offset(57f, before[5].center())
            onWindowThread {
                mouse(scene, PointerEventType.Move, from, pressed = false)
                mouse(scene, PointerEventType.Press, from, pressed = true)
            }
            frames(3)
            for (step in 1..4) {
                onWindowThread { mouse(scene, PointerEventType.Move, Offset(from.x + (at.x - from.x) * step / 4f, from.y + (at.y - from.y) * step / 4f), pressed = true) }
                frames(1)
            }
            onWindowThread {
                scene.sendPointerEvent(
                    PointerEventType.Scroll, at, scrollDelta = Offset(0f, 5f), type = PointerType.Mouse,
                    buttons = PointerButtons(isPrimaryPressed = true)
                )
            }
            frames(5)
            val after = textRows(picture())
            onWindowThread { mouse(scene, PointerEventType.Release, at, pressed = false) }
            frames(3)
            assertNotEquals("la pagina non si è mossa (fra più blocchi: $acrossBlocks)", before.take(8), after.take(8))
        }
    }

    /**
     * **Trascinando la selezione fino al bordo in basso la pagina scorre da
     * sola** (chiesto dall'utente il 10/11/2026), e si passa ai blocchi interi.
     */
    @Test
    fun draggingToTheBottomEdgeScrollsThePage() {
        runBlocking {
            val repo = PageRepository(AppDatabase.getInstance(context))
            if (AppDatabase.getInstance(context).pageDao().getById("lunga") == null) repo.createPage(PageEntity(id = "lunga", title = "Lunga"))
            for (i in 0 until 60) repo.saveBlock(BlockEntity(id = "lunga-$i", pageId = "lunga", textJson = """[{"text":"Riga numero $i"}]""", orderIndex = i))
        }
        withPage("lunga") { scene, frames, picture ->
            val before = textRows(picture())
            val from = Offset(30f, before[5].center())
            val edge = Offset(27f, 690f)
            onWindowThread {
                mouse(scene, PointerEventType.Move, from, pressed = false)
                mouse(scene, PointerEventType.Press, from, pressed = true)
            }
            frames(3)
            for (step in 1..6) {
                onWindowThread { mouse(scene, PointerEventType.Move, Offset(from.x + (edge.x - from.x) * step / 6f, from.y + (edge.y - from.y) * step / 6f), pressed = true) }
                frames(1)
            }
            // Fermi sul bordo: la pagina scorre da sola.
            frames(20)
            val after = textRows(picture())
            onWindowThread { mouse(scene, PointerEventType.Release, edge, pressed = false) }
            frames(3)
            File("build/desktop-screenshots", "edge_scroll.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(picture()).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            assertNotEquals("la pagina non è scesa", before.take(6), after.take(6))
            // La selezione ha seguito la pagina: Canc toglie le righe dalla prima a quella sotto il bordo.
            onWindowThread { key(scene, Key.Delete) }
            frames(5)
            val end = System.currentTimeMillis() + 5_000
            while (System.currentTimeMillis() < end && remainingIn("lunga").size == 60) Thread.sleep(50)
            assertTrue("rimaste ${remainingIn("lunga").size}", remainingIn("lunga").size < 45)
        }
    }

    /** Una pagina con una tabella 2×2: A1 B1 / A2 B2. */
    private fun tablePage(pageId: String, tableId: String) = runBlocking {
        val repo = PageRepository(AppDatabase.getInstance(context))
        if (AppDatabase.getInstance(context).pageDao().getById(pageId) == null) repo.createPage(PageEntity(id = pageId, title = "Tabella"))
        repo.saveBlock(BlockEntity(id = tableId, pageId = pageId, type = BlockType.TABLE, tableRows = 2, tableCols = 2, orderIndex = 0))
        for ((r, row) in listOf(listOf("A1", "B1"), listOf("A2", "B2")).withIndex()) {
            for ((c, text) in row.withIndex()) repo.setTableCellText(tableId, r, c, text)
        }
    }

    private fun cells(tableId: String): Map<Pair<Int, Int>, String> = runBlocking {
        PageRepository(AppDatabase.getInstance(context)).getTableCells(tableId).first().associate { (it.rowIndex to it.colIndex) to it.text }
    }

    /** Trascinando il mouse da una cella a un'altra, dalla prima all'ultima. */
    private fun dragCells(scene: ImageComposeScene, frames: (Int) -> Unit, from: Offset, to: Offset) {
        onWindowThread {
            mouse(scene, PointerEventType.Move, from, pressed = false)
            mouse(scene, PointerEventType.Press, from, pressed = true)
        }
        frames(3)
        for (step in 1..6) {
            onWindowThread { mouse(scene, PointerEventType.Move, Offset(from.x + (to.x - from.x) * step / 6f, from.y + (to.y - from.y) * step / 6f), pressed = true) }
            frames(1)
        }
        onWindowThread { mouse(scene, PointerEventType.Release, to, pressed = false) }
        frames(5)
    }

    /** Due celle della prima riga scelte col mouse: Canc le svuota, il resto della tabella resta. */
    @Test
    fun pickingSomeCellsAndDeleteEmptiesThem() {
        tablePage("tabella1", "tab1")
        withPage("tabella1") { scene, frames, picture ->
            val rows = textRows(picture()).takeLast(2)
            dragCells(scene, frames, Offset(96f, rows[0].center()), Offset(326f, rows[0].center()))
            File("build/desktop-screenshots", "table_cells_picked.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(picture()).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            onWindowThread { key(scene, Key.Delete) }
            frames(5)
            val end = System.currentTimeMillis() + 5_000
            while (System.currentTimeMillis() < end && cells("tab1")[0 to 0] != "") Thread.sleep(50)
            assertEquals(mapOf((0 to 0) to "", (0 to 1) to "", (1 to 0) to "A2", (1 to 1) to "B2"), cells("tab1"))
        }
    }

    /** Tutte le celle scelte: Canc toglie la tabella intera. */
    @Test
    fun pickingAllTheCellsAndDeleteRemovesTheTable() {
        tablePage("tabella2", "tab2")
        withPage("tabella2") { scene, frames, picture ->
            val rows = textRows(picture()).takeLast(2)
            dragCells(scene, frames, Offset(96f, rows[0].center()), Offset(326f, rows[1].center()))
            onWindowThread { key(scene, Key.Delete) }
            frames(5)
            val end = System.currentTimeMillis() + 5_000
            fun tables() = runBlocking { AppDatabase.getInstance(context).blockDao().getBlocksForPageOnce("tabella2").filter { it.type == BlockType.TABLE } }
            while (System.currentTimeMillis() < end && tables().isNotEmpty()) Thread.sleep(50)
            assertEquals(emptyList<BlockEntity>(), tables())
        }
    }

    private fun bright(c: Int) = ((c shr 16) and 0xFF) > 140 && ((c shr 8) and 0xFF) > 140 && (c and 0xFF) > 140

    /** Le fasce di righe dello schermo con del testo chiaro, fra x 53 (dopo i puntini) e 300: dove sono le righe della pagina. */
    private fun textRows(bitmap: Bitmap): List<IntRange> {
        val rows = (0 until bitmap.height).filter { y -> (53 until 300).any { x -> bright(bitmap.getColor(x, y)) } }
        val bands = ArrayList<IntRange>()
        for (y in rows) {
            val last = bands.lastOrNull()
            // La barra in alto (indietro, menu) conta come una riga sola.
            if (last != null && (y <= last.last + 1 || y < TOP_BAR)) bands[bands.lastIndex] = last.first..y else bands += y..y
        }
        return bands
    }

    private fun mouse(scene: ImageComposeScene, type: PointerEventType, at: Offset, pressed: Boolean) =
        scene.sendPointerEvent(
            type, at,
            type = PointerType.Mouse,
            buttons = if (pressed) PointerButtons(isPrimaryPressed = true) else PointerButtons(),
            button = if (type == PointerEventType.Move) null else PointerButton.Primary
        )

    /** Dove stanno i sei puntini: nel margine, subito prima del testo (x 53: dal 13/11/2026 il margine a sinistra è 33). */
    private val DOTS_X = 27 until 52

    /** Sopra quest'altezza c'è la barra in alto, coi pulsanti e il percorso della pagina. */
    private val TOP_BAR = 60

    private fun key(scene: ImageComposeScene, key: Key) {
        scene.sendKeyEvent(KeyEvent(key, KeyEventType.KeyDown))
        scene.sendKeyEvent(KeyEvent(key, KeyEventType.KeyUp))
    }
}
