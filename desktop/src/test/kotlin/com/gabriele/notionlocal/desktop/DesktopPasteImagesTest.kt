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
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockAttrs
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.Base64
import javax.imageio.ImageIO

/**
 * **Incollando, anche le immagini** (18/11/2026, chiesto dall'utente: "Se copio qualcosa (per esempio
 * da OneNote) che abbia anche un'immagine in mezzo al contenuto che copio, fai sì che incolli
 * sull'app anche le immagini"): fra le righe incollate, ogni immagine diventa un blocco immagine al
 * suo posto, con la sua copia nell'app. Come copia OneNote: l'immagine in un file accanto
 * (`clip_image….png`, dentro `<![if !vml]>`), o scritta nell'HTML (`data:`).
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopPasteImagesTest {
    private val dir: File = Files.createTempDirectory("pageflow-pasteimages").toFile()
    private val context = DesktopContext(dir)
    private val system = java.awt.datatransfer.Clipboard("prova")
    private lateinit var db: AppDatabase

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    private fun png(width: Int, height: Int, rgb: Int): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until width) for (y in 0 until height) image.setRGB(x, y, rgb)
        return ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
    }

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        db = AppDatabase.getInstance(context)
        val repo = PageRepository(db)
        repo.createPage(PageEntity(id = "appunti", title = "Appunti"))
        repo.saveBlock(BlockEntity(id = "riga", pageId = "appunti", type = BlockType.PARAGRAPH, textJson = """[{"text":""}]""", orderIndex = 0))
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    @Test
    fun theImagesArrivePastedAmongTheLines() {
        // Il file che Office scrive accanto all'HTML, nella cartella temporanea.
        val clip = File(dir, "msohtmlclip1/01/clip_image002.png").apply { parentFile.mkdirs(); writeBytes(png(120, 60, 0xCC3333)) }
        val inline = "data:image/png;base64," + Base64.getEncoder().encodeToString(png(80, 40, 0x3366CC))
        val p = "style='margin:0in;font-family:Calibri;font-size:11.0pt'"
        val html = "<html><body lang=it style='font-family:Calibri;font-size:11.0pt'><!--StartFragment-->" +
            "<p $p>Prima</p>" +
            "<p $p><!--[if gte vml 1]><v:shape style='width:90pt;height:45pt'><v:imagedata src=\"file:///C:/x/clip_image001.png\" o:title=\"\"/></v:shape><![endif]-->" +
            "<![if !vml]><img width=120 height=60 src=\"file:///${clip.absolutePath.replace('\\', '/')}\" v:shapes=\"Immagine_x0020_1\"><![endif]></p>" +
            "<p $p>In mezzo</p>" +
            "<p $p><img src=\"$inline\" alt=\"Uno schema\"></p>" +
            "<p $p>Dopo</p>" +
            "<!--EndFragment--></body></html>"
        val plain = "Prima\nIn mezzo\nDopo"

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
                                pageId = "appunti", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
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
        fun bright(c: Int) = ((c shr 16) and 0xFF) > 140 && ((c shr 8) and 0xFF) > 140 && (c and 0xFF) > 140
        try {
            frames(40)
            val empty = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }
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
            frames(40)
            val shot = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }
            File("build/desktop-screenshots").mkdirs()
            File("build/desktop-screenshots", "paste_images.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(shot).encodeToData(EncodedImageFormat.PNG)!!.bytes)

            val blocks = runBlocking { db.blockDao().getBlocksForPageOnce("appunti").sortedBy { it.orderIndex } }
            fun textOf(block: BlockEntity) = Regex("\"text\":\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(block.textJson).joinToString("") { it.groupValues[1] }
            assertEquals(
                "le righe e le immagini al loro posto",
                listOf("PARAGRAPH:Prima", "IMAGE:", "PARAGRAPH:In mezzo", "IMAGE:", "PARAGRAPH:Dopo"),
                blocks.map { "${it.type}:${textOf(it)}" }
            )
            // Ogni immagine ha la sua copia nell'app, ed è quella giusta (le misure).
            val store = PageImageStore(context)
            val files = blocks.filter { it.type == BlockType.IMAGE }.map { BlockAttrs.parse(it.attrsJson).file }
            assertEquals(listOf(120 to 60, 80 to 40), files.map { store.imageSize(it!!) })
            // Nessuna copia in più nella cartella.
            assertEquals(2, store.fileFor("x").parentFile.listFiles()!!.size)
            // Sulla pagina si vedono: il rosso della prima e il blu della seconda.
            fun has(rgb: Int) = (0 until shot.width step 3).any { x -> (0 until shot.height step 3).any { y -> shot.getColor(x, y) and 0xFFFFFF == rgb } }
            assertTrue("le immagini sulla pagina", has(0xCC3333) && has(0x3366CC))
            // E solo le tre righe di testo: il campo non tiene più i segnaposto "Image" dell'incolla.
            val lines = (titleBottom + 10 until shot.height).filter { y -> (40 until 120).any { x -> bright(shot.getColor(x, y)) } }
                .fold(mutableListOf<IntRange>()) { acc, y -> if (acc.isNotEmpty() && acc.last().last >= y - 3) acc[acc.lastIndex] = acc.last().first..y else acc += y..y; acc }
            assertEquals("le righe di testo: $lines", 3, lines.size)
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
