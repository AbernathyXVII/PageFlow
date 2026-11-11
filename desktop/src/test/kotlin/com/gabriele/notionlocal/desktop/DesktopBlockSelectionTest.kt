package com.gabriele.notionlocal.desktop

import com.gabriele.notionlocal.data.PageImageStore
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
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
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
        val first = Offset(60f, rows[3].center())
        val second = Offset(60f, rows[5].center())
        onWindowThread {
            mouse(scene, PointerEventType.Move, first, pressed = false)
            mouse(scene, PointerEventType.Press, first, pressed = true)
        }
        frames(3)
        for (step in 1..6) {
            onWindowThread { mouse(scene, PointerEventType.Move, Offset(60f, first.y + (second.y - first.y) * step / 6f), pressed = true) }
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
        val divider = Offset(60f, (rows[5].last + rows[6].first) / 2f)
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
        val divider = Offset(60f, (rows[5].last + rows[6].first) / 2f)
        onWindowThread {
            mouse(scene, PointerEventType.Move, divider, pressed = false)
            mouse(scene, PointerEventType.Press, divider, pressed = true)
            mouse(scene, PointerEventType.Release, divider, pressed = false)
        }
        frames(5)
        onWindowThread { key(scene, Key.Backspace) }
        frames(5)
        waitForBlocks(listOf("uno", "titolo", "due", "tre"))
        val line = Offset(60f, rows[3].center())
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
        val line = Offset(60f, rows[3].center())
        val empty = Offset(450f, rows[6].center())
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

    /** Le altezze dove il margine a sinistra del testo (x 24–48) è diverso da [clean]: dove sono i sei puntini. */
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
        assertTrue("il testo comincia a $firstInk", firstInk >= 50)
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
        val dots = Offset(38f, onDivider.average().toFloat())
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
            assertTrue("il testo comincia a $firstInk", firstInk >= 50)
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
            // Subito a sinistra del titolo, senza toccarlo.
            val dotsRight = DOTS_X.last { x -> (title.first..title.last).any { y -> shown.getColor(x, y) != clean.getColor(x, y) } }
            val titleLeft = (0 until 300).first { x -> (title.first..title.last).any { y -> bright(clean.getColor(x, y)) } }
            assertTrue("puntini fino a $dotsRight, titolo da $titleLeft", dotsRight < titleLeft - 2)
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
            val at = if (acrossBlocks) Offset(80f, before[7].center()) else Offset(90f, before[5].center())
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
            val edge = Offset(60f, 690f)
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
            dragCells(scene, frames, Offset(60f, rows[0].center()), Offset(290f, rows[0].center()))
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
            dragCells(scene, frames, Offset(60f, rows[0].center()), Offset(290f, rows[1].center()))
            onWindowThread { key(scene, Key.Delete) }
            frames(5)
            val end = System.currentTimeMillis() + 5_000
            fun tables() = runBlocking { AppDatabase.getInstance(context).blockDao().getBlocksForPageOnce("tabella2").filter { it.type == BlockType.TABLE } }
            while (System.currentTimeMillis() < end && tables().isNotEmpty()) Thread.sleep(50)
            assertEquals(emptyList<BlockEntity>(), tables())
        }
    }

    private fun bright(c: Int) = ((c shr 16) and 0xFF) > 140 && ((c shr 8) and 0xFF) > 140 && (c and 0xFF) > 140

    /** Le fasce di righe dello schermo con del testo chiaro, fra x 50 (dopo i puntini) e 300: dove sono le righe della pagina. */
    private fun textRows(bitmap: Bitmap): List<IntRange> {
        val rows = (0 until bitmap.height).filter { y -> (50 until 300).any { x -> bright(bitmap.getColor(x, y)) } }
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

    /** Dove stanno i sei puntini: fra il bordo della pagina spostata e il testo (x 50). */
    private val DOTS_X = 24 until 49

    /** Sopra quest'altezza c'è la barra coi pulsanti indietro e menu. */
    private val TOP_BAR = 60

    private fun key(scene: ImageComposeScene, key: Key) {
        scene.sendKeyEvent(KeyEvent(key, KeyEventType.KeyDown))
        scene.sendKeyEvent(KeyEvent(key, KeyEventType.KeyUp))
    }
}
