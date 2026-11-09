package com.gabriele.notionlocal.desktop

import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.export.ExportFormat
import com.gabriele.notionlocal.data.export.ExportOptions
import com.gabriele.notionlocal.data.export.defaultPdfBackend
import com.gabriele.notionlocal.data.export.exportToFile
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

/**
 * **Il PDF sul PC** (dal 09/11/2026): una pagina con un po' di tutto diventa
 * un PDF col testo vero (lo si ritrova leggendolo), su più fogli se è lunga;
 * la foto del primo foglio resta in `build/desktop-screenshots/pdf_page1.png`.
 * Serve Windows (i suoi caratteri): altrove il test non parte.
 */
class DesktopPdfTest {
    private val dir = Files.createTempDirectory("pageflow-pdf").toFile()
    private val context = DesktopContext(dir)
    private var order = 0

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun block(type: BlockType, vararg spans: RichTextSpan, checked: Boolean = false, attrs: BlockAttrs? = null) = BlockEntity(
        pageId = "prova",
        type = type,
        textJson = Json.encodeToString<List<RichTextSpan>>(spans.toList()),
        isChecked = checked,
        orderIndex = order++,
        attrsJson = attrs?.let { Json.encodeToString(it) } ?: "{}"
    )

    @Test
    fun aPageBecomesAPdfWithRealText() = runBlocking<Unit> {
        assumeTrue("servono i caratteri di Windows", defaultPdfBackend() != null)
        AppSettings.init(context)
        val factory = ViewModelFactory(context)
        val repo = factory.pageRepository
        repo.createPage(PageEntity(id = "prova", title = "Prova del PDF", icon = "🧪"))
        val photo = PageImageStore(context).fileFor("foto.png").apply { parentFile?.mkdirs() }
        ImageIO.write(BufferedImage(240, 120, BufferedImage.TYPE_INT_RGB).apply {
            createGraphics().apply { color = Color(0x2F6FDE); fillRect(0, 0, 240, 120); color = Color.WHITE; fillOval(80, 20, 80, 80); dispose() }
        }, "png", photo)
        val blocks = listOf(
            block(
                BlockType.PARAGRAPH,
                RichTextSpan("Testo normale, "),
                RichTextSpan("grassetto", bold = true),
                RichTextSpan(", "),
                RichTextSpan("corsivo", italic = true),
                RichTextSpan(", "),
                RichTextSpan("rosso", color = "#E03E3E"),
                RichTextSpan(", "),
                RichTextSpan("evidenziato", background = "#FBE3A0"),
                RichTextSpan(", "),
                RichTextSpan("sottolineato", underline = true),
                RichTextSpan(", "),
                RichTextSpan("barrato", strikethrough = true),
                RichTextSpan(" e x"),
                RichTextSpan("2", superscript = true)
            ),
            block(BlockType.BULLET_LIST_ITEM, RichTextSpan("Un pallino")),
            block(BlockType.NUMBERED_LIST_ITEM, RichTextSpan("Un numero")),
            block(BlockType.CHECKBOX, RichTextSpan("Una cosa fatta"), checked = true),
            block(BlockType.CHECKBOX, RichTextSpan("Una cosa da fare")),
            block(BlockType.QUOTE, RichTextSpan("Una citazione")),
            block(BlockType.CALLOUT, RichTextSpan("Un riquadro")),
            block(BlockType.CODE, RichTextSpan("val pdf = \"sul PC\"")),
            block(BlockType.DIVIDER),
            block(BlockType.TOGGLE, RichTextSpan("Un toggle")),
            block(BlockType.PARAGRAPH, RichTextSpan("Emoji 😀 ☁️ e coreano 안녕하세요")),
            block(BlockType.IMAGE, attrs = BlockAttrs(file = "foto.png", name = "foto")),
            block(BlockType.PARAGRAPH, RichTextSpan("Una riga lunga che deve andare a capo da sola. ".repeat(6)))
        ) + (1..40).map { block(BlockType.PARAGRAPH, RichTextSpan("Riga $it")) }
        blocks.forEach { repo.saveBlock(it) }

        val pdf = exportToFile(context, factory, "prova", ExportOptions(ExportFormat.PDF), "Untitled")!!
        assertEquals("Prova del PDF.pdf", pdf.name)
        Loader.loadPDF(pdf.file).use { document ->
            assertTrue("più fogli: ${document.numberOfPages}", document.numberOfPages >= 2)
            val text = PDFTextStripper().getText(document)
            for (expected in listOf("Prova del PDF", "grassetto", "evidenziato", "Un pallino", "Una cosa da fare", "Una citazione", "val pdf", "안녕하세요", "Riga 40", "1 / ")) {
                assertTrue("manca \"$expected\" in:\n$text", text.contains(expected))
            }
            val image = PDFRenderer(document).renderImageWithDPI(0, 110f)
            File("build/desktop-screenshots/pdf_page1.png").apply { parentFile.mkdirs() }.let { ImageIO.write(image, "png", it) }
        }
    }
}
