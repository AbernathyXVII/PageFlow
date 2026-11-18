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
 * **L'indice porta a una Quote dentro le colonne e illumina lei sola** (18/11/2026, trovato
 * dall'utente: "quando clicco su una voce del table of content e seleziono una Quote che ho scritto,
 * mi fa l'highlight blu in tutta la pagina"): si illuminava il blocco di colonne intero, che occupa
 * la pagina, e la pagina scendeva solo fin dove comincia. Adesso la Quote arriva in vista e la
 * colonna accanto resta com'è.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopTocColumnsTest {
    private val dir: File = Files.createTempDirectory("pageflow-toccolumns").toFile()
    private val context = DesktopContext(dir)

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        val repo = PageRepository(AppDatabase.getInstance(context))
        repo.createPage(PageEntity(id = "casa", title = "Menu"))
        fun line(id: String, parent: String?, order: Int, text: String, type: BlockType = BlockType.PARAGRAPH, attrs: String = "{}") =
            BlockEntity(id = id, pageId = "casa", parentBlockId = parent, type = type, textJson = """[{"text":"$text"}]""", attrsJson = attrs, orderIndex = order)
        repo.saveBlock(line("sopra", null, 0, "In cima"))
        repo.saveBlock(BlockEntity(id = "cols", pageId = "casa", type = BlockType.COLUMN_LIST, orderIndex = 1))
        repo.saveBlock(BlockEntity(id = "c1", pageId = "casa", parentBlockId = "cols", type = BlockType.COLUMN, orderIndex = 0))
        repo.saveBlock(BlockEntity(id = "c2", pageId = "casa", parentBlockId = "cols", type = BlockType.COLUMN, orderIndex = 1))
        repeat(40) { i ->
            if (i == 30) {
                repo.saveBlock(line("quote", "c1", i, "Una citazione importante", BlockType.QUOTE, """{"linkTarget":true}"""))
            } else {
                repo.saveBlock(line("s$i", "c1", i, "Sinistra $i"))
            }
            repo.saveBlock(line("d$i", "c2", i, "Destra $i"))
        }
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    @Test
    fun theDashesOpenTheListAndAClickGoesThere() {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }
        val scene = onWindowThread {
            ImageComposeScene(width = 1000, height = 700, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
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
        fun shot(name: String): Bitmap = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }.also {
            File("build/desktop-screenshots").mkdirs()
            File("build/desktop-screenshots", "$name.png").writeBytes(Image.makeFromBitmap(it).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        fun lit(c: Int) = ((c shr 16) and 0xFF) > 60
        /** Le lineette nel margine a destra: per ognuna l'altezza e se è chiara (il punto dove si è). */
        fun dashes(bitmap: Bitmap): List<Pair<Int, Boolean>> {
            val rows = (100 until 400).filter { y -> (950 until 990).count { x -> lit(bitmap.getColor(x, y)) } >= 8 }
            return rows.filterIndexed { i, y -> i == 0 || rows[i - 1] < y - 1 }.map { y ->
                y to ((950 until 990).maxOf { x -> (bitmap.getColor(x, y) shr 16) and 0xFF } > 180)
            }
        }
        fun mouse(type: PointerEventType, at: Offset, pressed: Boolean = false, button: PointerButton? = null) = onWindowThread {
            scene.sendPointerEvent(type, at, type = PointerType.Mouse, buttons = if (pressed) PointerButtons(isPrimaryPressed = true) else PointerButtons(), button = button)
        }
        try {
            frames(50)
            val clean = shot("toccolumns_start")
            val railY = dashes(clean).single().first.toFloat()
            // Col mouse sopra la lineetta, l'elenco; un clic sul nome della Quote.
            mouse(PointerEventType.Move, Offset(975f, railY))
            frames(5)
            val open = shot("toccolumns_open")
            val names = (100 until 450).filter { y -> (760 until 940).any { x -> open.getColor(x, y).let { c -> ((c shr 16) and 0xFF) > 120 } } }
            val at = Offset(850f, names.first() + 5f)
            mouse(PointerEventType.Move, at)
            mouse(PointerEventType.Press, at, pressed = true, button = PointerButton.Primary)
            mouse(PointerEventType.Release, at, button = PointerButton.Primary)
            mouse(PointerEventType.Move, Offset(500f, 690f))
            frames(4)
            val jumped = shot("toccolumns_jumped")
            // Dov'è illuminato: il velo azzurro (il testo bianco no), nello spazio vuoto delle due colonne.
            fun tinted(c: Int) = (c and 0xFF) - ((c shr 16) and 0xFF) > 25
            fun litRows(xs: IntRange) = (60 until 690).filter { y -> xs.count { x -> tinted(jumped.getColor(x, y)) } > xs.count() / 2 }
            val left = litRows(380 until 470)
            val right = litRows(700 until 800)
            assertTrue("la Quote illuminata, in vista: $left", left.size in 10..120)
            assertTrue("la colonna accanto non si illumina: ${right.size} righe", right.isEmpty())
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
