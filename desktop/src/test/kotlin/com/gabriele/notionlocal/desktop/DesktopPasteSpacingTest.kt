package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.text.AnnotatedString
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
import com.gabriele.notionlocal.platform.HtmlSelection
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.screen.LinkClipboard
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Le righe incollate da OneNote nascono fitte** (17/11/2026, trovato
 * dall'utente: incollato un elenco con un titolo in mezzo, le righe erano
 * distanziate e si sistemavano da sole dopo un paio di secondi, quelle sopra
 * solo selezionando tutto): subito dopo Ctrl+V il passo fra le voci è quello
 * di sempre, e non cambia dopo.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopPasteSpacingTest {
    private val dir: File = Files.createTempDirectory("pageflow-pastespacing").toFile()
    private val context = DesktopContext(dir)
    private val system = java.awt.datatransfer.Clipboard("prova")
    private lateinit var db: AppDatabase

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    // Come lo copia OneNote: il carattere sul body, gli elenchi uno dentro l'altro, un titolo grande in mezzo.
    private val li = "style='margin-top:0;margin-bottom:0;vertical-align:middle'"
    private val span = "style='font-family:Calibri;font-size:11.0pt'"
    private val bold = "style='font-weight:bold;font-family:Calibri;font-size:11.0pt'"
    private val html = "<html><body lang=it style='font-family:Calibri;font-size:11.0pt'><!--StartFragment-->" +
        "<ul type=disc style='direction:ltr;margin-top:0in;margin-bottom:0in'>" +
        "<li $li><span $span>Formato data</span></li><li $li><span $span>Lingua</span></li><li $li><span $span>Fuso orario</span></li></ul>" +
        "<p style='margin:0in;font-family:Calibri;font-size:20.0pt'><span style='font-weight:bold'>Generali</span></p>" +
        "<ul type=disc style='direction:ltr;margin-top:0in;margin-bottom:0in'>" +
        "<li $li><span $bold>Avvio dell'app</span></li>" +
        "<ul type=circle style='direction:ltr;margin-top:0in;margin-bottom:0in'>" +
        "<li $li><span $span>Apri ultima pagina visitata</span></li><li $li><span $span>Apri home/dashboard</span></li>" +
        "<li $li><span $span>Apri una pagina specifica</span></li></ul>" +
        "<li $li><span $bold>Pagina iniziale predefinita</span></li><li $li><span $bold>Conferma prima di eliminare</span></li>" +
        "<li $li><span $bold>Cestino</span></li></ul>" +
        "<!--EndFragment--></body></html>"
    private val plain = "Formato data\nLingua\nFuso orario\nGenerali\nAvvio dell'app\nApri ultima pagina visitata\nApri home/dashboard\n" +
        "Apri una pagina specifica\nPagina iniziale predefinita\nConferma prima di eliminare\nCestino"

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        db = AppDatabase.getInstance(context)
        val repo = PageRepository(db)
        repo.createPage(PageEntity(id = "impostazioni", title = "Impostazioni"))
        repo.saveBlock(BlockEntity(id = "riga", pageId = "impostazioni", type = BlockType.PARAGRAPH, textJson = """[{"text":""}]""", orderIndex = 0))
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    @Test
    fun thePastedLinesAreCloseAtOnce() {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        val base = object : ClipboardManager {
            override fun getText(): AnnotatedString? = null
            override fun setText(annotatedString: AnnotatedString) = Unit
        }
        val compose = object : Clipboard {
            override val nativeClipboard: NativeClipboard get() = system
            override suspend fun getClipEntry(): ClipEntry? = null
            override suspend fun setClipEntry(clipEntry: ClipEntry?) = Unit
        }
        val clipboard = LinkClipboard(base, compose, system = system)
        fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 900, density = Density(1f)) {
                CompositionLocalProvider(
                    LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner,
                    LocalClipboardManager provides clipboard, LocalClipboard provides clipboard
                ) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            PageEditorScreen(
                                pageId = "impostazioni", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
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
            File("build/desktop-screenshots", "$name.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(it).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        fun bright(c: Int) = ((c shr 16) and 0xFF) > 140 && ((c shr 8) and 0xFF) > 140 && (c and 0xFF) > 140
        /** I centri delle righe con del testo sotto il titolo della pagina. */
        fun lineCenters(bitmap: Bitmap): List<Float> {
            val rows = (170 until bitmap.height).filter { y -> (40 until 600).any { x -> bright(bitmap.getColor(x, y)) } }
            val bands = ArrayList<IntRange>()
            for (y in rows) {
                val last = bands.lastOrNull()
                if (last != null && y <= last.last + 3) bands[bands.lastIndex] = last.first..y else bands += y..y
            }
            return bands.filter { it.last - it.first >= 4 }.map { (it.first + it.last) / 2f }
        }
        try {
            frames(40)
            val empty = shot("pastespacing_empty")
            // La riga vuota: sotto il titolo della pagina, dove si clicca per scrivere.
            val titleBottom = (100 until 400).last { y -> (40 until 600).any { x -> bright(empty.getColor(x, y)) } }
            onWindowThread {
                val at = Offset(300f, titleBottom + 40f)
                scene.sendPointerEvent(PointerEventType.Move, at, type = PointerType.Mouse)
                scene.sendPointerEvent(PointerEventType.Press, at, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
                scene.sendPointerEvent(PointerEventType.Release, at, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
            }
            frames(6)
            system.setContents(HtmlSelection(plain, html), null)
            onWindowThread {
                scene.sendKeyEvent(KeyEvent(Key.V, KeyEventType.KeyDown, isCtrlPressed = true))
                scene.sendKeyEvent(KeyEvent(Key.V, KeyEventType.KeyUp, isCtrlPressed = true))
            }
            frames(4)
            val soon = lineCenters(shot("pastespacing_soon"))
            frames(120)
            val later = lineCenters(shot("pastespacing_later"))
            // Le tre voci in cima ("Formato data", "Lingua", "Fuso orario") e le ultime tre, dopo il titolo.
            fun steps(centers: List<Float>) = centers.zipWithNext { a, b -> b - a }
            // Nessuna riga doppia: nel database le undici righe incollate, una volta sola.
            val texts = runBlocking {
                db.blockDao().getBlocksForPageOnce("impostazioni").sortedBy { it.orderIndex }.map { block ->
                    Regex("\"text\":\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(block.textJson).joinToString("") { it.groupValues[1] }
                }
            }
            org.junit.Assert.assertEquals(plain.split("\n"), texts)
            // (Due righe vicine con le gambe delle lettere possono fare una fascia sola: si confrontano le fasce.)
            assertTrue("righe subito: $soon, dopo: $later", soon.size >= 8 && later.size == soon.size)
            val stepsSoon = steps(soon)
            val stepsLater = steps(later)
            val usual = stepsLater.first()
            // Fra "Formato data" e "Lingua" il passo di una riga, non quello del titolo (44 prima).
            assertTrue("passo fra le prime voci: $usual", usual < 30f)
            assertTrue(
                "il passo fra le voci subito dopo l'incolla ($stepsSoon) è quello di dopo ($stepsLater)",
                stepsSoon.zip(stepsLater).all { (a, b) -> kotlin.math.abs(a - b) <= 2f } && stepsSoon.take(2).all { it < 30f }
            )
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
