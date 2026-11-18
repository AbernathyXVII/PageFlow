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
 * **Incollato da OneNote con titoli grandi e un'immagine, le righe nascono fitte** (18/11/2026,
 * trovato dall'utente: "all'inizio lo spacing è enorme, poi seleziono del testo qualsiasi con il
 * mouse e ritorna ad essere normale"). Con un'immagine nell'incolla il campo rilegge le righe da
 * fuori (`externalChangeTick`) e i titoli restavano nel campo dei paragrafi, che prendevano tutti la
 * loro altezza di riga: adesso escono comunque (vedi `splitMixedLooks`). Le righe lunghe vanno a
 * capo, come sul PC dell'utente ("Full line view" spento).
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopPasteHeadingsTest {
    private val dir: File = Files.createTempDirectory("pageflow-pasteheadings").toFile()
    private val context = DesktopContext(dir)
    private val system = java.awt.datatransfer.Clipboard("prova")
    private lateinit var db: AppDatabase

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    // Come lo copia OneNote (la pagina dell'utente del 18/11/2026): due titoli grandi in cima, poi
    // titoli medi e paragrafi a 11 punti, con grassetti, corsivi e un link.
    private val p = "style='margin:0in;font-family:Calibri;font-size:11.0pt'"
    private fun heading(pt: String, text: String) = "<p style='margin:0in;font-family:Calibri;font-size:$pt'><span style='font-weight:bold'>$text</span></p>"
    private fun trait(name: String, intro: String, left: String, right: String, mostly: String, body: String) =
        heading("15.0pt", name) +
            "<p $p>$intro</p>" +
            "<p $p><span style='font-weight:bold'>$left</span> 67% _ 33% <span style='font-weight:bold'>$right</span></p>" +
            "<p $p><span style='font-weight:bold;font-style:italic'>$mostly</span></p>" +
            "<p $p>$body <a href=\"https://example.com/read\">Read more</a></p>"
    private val html = "<html><body lang=it style='font-family:Calibri;font-size:11.0pt'><!--StartFragment-->" +
        heading("20.0pt", "My M.B.T.I. → ESTJ-A") +
        heading("20.0pt", "Your Personality") +
        trait("Mind", "This trait determines how we interact with our environment.", "EXTRAVERTED", "INTROVERTED", "You're mostly Extraverted.", "Extraverted individuals prefer group activities and get energized by social interaction.") +
        trait("Energy", "This trait shows where we direct our mental energy.", "INTUITIVE", "OBSERVANT", "You're mostly Observant.", "Observant individuals are highly practical, pragmatic and down-to-earth.") +
        trait("Nature", "This trait determines how we make decisions and cope with emotions.", "THINKING", "FEELING", "You're mostly Thinking.", "Thinking individuals focus on objectivity and rationality.") +
        trait("Tactics", "This trait reflects our approach to work, planning and decision-making.", "JUDGING", "PROSPECTING", "You're mostly Judging.", "Judging individuals are decisive, thorough and highly organized.") +
        trait("Identity", "This trait underpins all others, showing how confident we are in our abilities and decisions.", "ASSERTIVE", "TURBULENT", "You're mostly Assertive.", "Assertive individuals are self-assured, even-tempered and resistant to stress.") +
        heading("20.0pt", "Introduction") + "<p><img src=\"$IMAGE\"></p>" + heading("16.0pt", "WHO IS AN EXECUTIVE (ESTJ)?") +
        LONG.joinToString("") { "<p $p>$it</p>" } +
        "<!--EndFragment--></body></html>"
    private val plain = listOf(
        "My M.B.T.I. → ESTJ-A", "Your Personality",
        "Mind", "This trait determines how we interact with our environment.", "EXTRAVERTED 67% _ 33% INTROVERTED", "You're mostly Extraverted.",
        "Extraverted individuals prefer group activities and get energized by social interaction. Read more",
        "Energy", "This trait shows where we direct our mental energy.", "INTUITIVE 67% _ 33% OBSERVANT", "You're mostly Observant.",
        "Observant individuals are highly practical, pragmatic and down-to-earth. Read more",
        "Nature", "This trait determines how we make decisions and cope with emotions.", "THINKING 67% _ 33% FEELING", "You're mostly Thinking.",
        "Thinking individuals focus on objectivity and rationality. Read more",
        "Tactics", "This trait reflects our approach to work, planning and decision-making.", "JUDGING 67% _ 33% PROSPECTING", "You're mostly Judging.",
        "Judging individuals are decisive, thorough and highly organized. Read more",
        "Identity", "This trait underpins all others, showing how confident we are in our abilities and decisions.", "ASSERTIVE 67% _ 33% TURBULENT", "You're mostly Assertive.",
        "Assertive individuals are self-assured, even-tempered and resistant to stress. Read more",
        "Introduction", "WHO IS AN EXECUTIVE (ESTJ)?"
    ).plus(LONG).joinToString("\n")

    private companion object {
        // Un quadratino verde in PNG, scritto dentro l'HTML.
        val IMAGE = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(
            java.io.ByteArrayOutputStream().also { out ->
                val image = java.awt.image.BufferedImage(60, 30, java.awt.image.BufferedImage.TYPE_INT_RGB)
                for (x in 0 until 60) for (y in 0 until 30) image.setRGB(x, y, 0x33AA55)
                javax.imageio.ImageIO.write(image, "png", out)
            }.toByteArray()
        )
        // Il resto della pagina: abbastanza testo da passare i 3.500 caratteri di un pezzo (`CHUNK_MAX_CHARS`).
        val LONG = List(10) { i ->
            "Paragraph ${i + 1}: Executives are representatives of tradition and order, utilizing their understanding of what is right, wrong and socially acceptable to bring families and communities together. " +
                "Embracing the values of honesty, dedication and dignity, people with this personality type are valued for their clear advice and guidance."
        }
    }

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        // Come sul PC dell'utente: le righe lunghe vanno a capo.
        AppSettings.fullLineView = false
        db = AppDatabase.getInstance(context)
        val repo = PageRepository(db)
        repo.createPage(PageEntity(id = "mbti", title = "MBTI"))
        repo.saveBlock(BlockEntity(id = "riga", pageId = "mbti", type = BlockType.PARAGRAPH, textJson = """[{"text":""}]""", orderIndex = 0))
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
                                pageId = "mbti", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
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
        /** I centri delle righe con del testo sotto il titolo della pagina: solo l'inizio delle righe, dove non passa il cursore che lampeggia. */
        fun lineCenters(bitmap: Bitmap): List<Float> {
            val rows = (170 until bitmap.height).filter { y -> (40 until 75).any { x -> bright(bitmap.getColor(x, y)) } }
            val bands = ArrayList<IntRange>()
            for (y in rows) {
                val last = bands.lastOrNull()
                if (last != null && y <= last.last + 3) bands[bands.lastIndex] = last.first..y else bands += y..y
            }
            return bands.filter { it.last - it.first >= 4 }.map { (it.first + it.last) / 2f }
        }
        try {
            frames(40)
            val empty = shot("pasteheadings_empty")
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
            val soon = lineCenters(shot("pasteheadings_soon"))
            frames(120)
            val later = lineCenters(shot("pasteheadings_later"))
            fun steps(centers: List<Float>) = centers.zipWithNext { a, b -> b - a }
            val texts = runBlocking {
                db.blockDao().getBlocksForPageOnce("mbti").sortedBy { it.orderIndex }.filter { it.type != BlockType.IMAGE }.map { block ->
                    Regex("\"text\":\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(block.textJson).joinToString("") { it.groupValues[1] }
                }
            }
            org.junit.Assert.assertEquals(plain.split("\n"), texts)
            // Le righe normali di "Mind": il passo di una riga (21 punti), non quello dei titoli (44).
            val small = steps(later).filter { it < 60f }
            assertTrue("righe subito: $soon, dopo: $later", later.size >= 10)
            assertTrue("il passo fra le righe normali dopo l'incolla: ${steps(later)}", small.count { it < 30f } >= 4)
            assertTrue("e anche subito: ${steps(soon)}", steps(soon).count { it < 30f } >= 4)
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
