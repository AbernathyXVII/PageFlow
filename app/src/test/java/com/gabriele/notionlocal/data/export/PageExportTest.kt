package com.gabriele.notionlocal.data.export

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.TableCellEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.zip.ZipFile

/**
 * **"Export"** (02/11/2026, chiesto dall'utente: "Export in PDF o Markdown.
 * Serve per mandare una pagina a qualcuno, stamparla, o non restare chiuso
 * dentro l'app"): la pagina in Markdown, in PDF e nello .zip con i file e le
 * sottopagine.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
// Il PDF lo disegna la grafica vera: con quella finta di Robolectric il documento non si apre.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PageExportTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private var order = 0

    private fun block(
        type: BlockType,
        vararg spans: RichTextSpan,
        pageId: String = "viaggio",
        indent: Int = 0,
        checked: Boolean = false,
        linked: String? = null,
        attrs: BlockAttrs? = null,
        id: String = "b${order}"
    ) = BlockEntity(
        id = id,
        pageId = pageId,
        type = type,
        textJson = Json.encodeToString(spans.toList()),
        indentLevel = indent,
        isChecked = checked,
        linkedPageId = linked,
        orderIndex = order++,
        attrsJson = attrs?.let { Json.encodeToString(it) } ?: "{}"
    )

    private fun text(text: String) = RichTextSpan(text)

    @Test
    fun aPageBecomesMarkdown() {
        val blocks = listOf(
            block(BlockType.PARAGRAPH, RichTextSpan("Il piano", bold = true, size = 24)),
            // Un rientro senza un elenco sopra in Markdown sarebbe codice: resta testo.
            block(BlockType.PARAGRAPH, text("Rientrata senza elenco"), indent = 2),
            block(
                BlockType.PARAGRAPH,
                text("Prendere "), RichTextSpan("il treno", bold = true), text(" alle "),
                RichTextSpan("8:00", bold = true, italic = true), text(" *presto*")
            ),
            block(BlockType.BULLET_LIST_ITEM, text("Biglietti")),
            block(BlockType.BULLET_LIST_ITEM, text("Andata"), indent = 1),
            block(BlockType.NUMBERED_LIST_ITEM, text("Uno")),
            block(BlockType.NUMBERED_LIST_ITEM, text("Due")),
            block(BlockType.CHECKBOX, text("Valigia"), checked = true),
            block(BlockType.CHECKBOX, text("Ombrello")),
            block(BlockType.PARAGRAPH, text("Nota rientrata"), indent = 2),
            block(BlockType.QUOTE, text("Chi va piano")),
            block(BlockType.CALLOUT, text("Attenzione"), attrs = BlockAttrs(icon = "⚠️")),
            block(BlockType.CODE, text("val x = 1"), attrs = BlockAttrs(language = "kotlin")),
            block(BlockType.DIVIDER),
            block(BlockType.TABLE, id = "tabella").copy(tableRows = 2, tableCols = 2),
            block(BlockType.PAGE_LINK, linked = "hotel"),
            block(BlockType.IMAGE, attrs = BlockAttrs(file = "abc.jpg", name = "foto mare.jpg")),
            block(BlockType.BOOKMARK, attrs = BlockAttrs(url = "https://example.com/a b", name = "Sito")),
            block(BlockType.PARAGRAPH, text("Vedi "), RichTextSpan("qui", link = "https://x.it")),
            block(BlockType.PARAGRAPH, text("# non un titolo")),
            block(BlockType.PARAGRAPH, text("1. non un elenco")),
            block(BlockType.BUTTON, text("Fai qualcosa"))
        )
        val cells = listOf(
            TableCellEntity(blockId = "tabella", rowIndex = 0, colIndex = 0, text = "Città"),
            TableCellEntity(blockId = "tabella", rowIndex = 0, colIndex = 1, text = "Notti"),
            TableCellEntity(blockId = "tabella", rowIndex = 1, colIndex = 0, text = "Roma | centro"),
            TableCellEntity(blockId = "tabella", rowIndex = 1, colIndex = 1, text = "2")
        )
        val page = ExportPage(PageEntity(id = "viaggio", title = "Viaggio: Roma/Napoli"), blocks, cells, mapOf("hotel" to "Hotel"))
        val markdown = markdownOf(
            page,
            "Untitled",
            fileLink = { if (it == "abc.jpg") "Viaggio/foto mare.jpg" else null },
            pageLink = { if (it == "hotel") "Viaggio/Hotel.md" else null }
        )
        val expected = listOf(
            "# Viaggio: Roma/Napoli",
            "## Il piano",
            "Rientrata senza elenco",
            """Prendere **il treno** alle ***8:00*** \*presto\*""",
            "- Biglietti\n    - Andata\n1. Uno\n2. Due\n- [x] Valigia\n- [ ] Ombrello",
            "    Nota rientrata",
            "> Chi va piano",
            "> ⚠️ Attenzione",
            "```kotlin\nval x = 1\n```",
            "---",
            "| Città | Notti |\n| --- | --- |\n| Roma \\| centro | 2 |",
            "📄 [Hotel](Viaggio/Hotel.md)",
            "![foto mare.jpg](Viaggio/foto%20mare.jpg)",
            "🔖 [Sito](https://example.com/a%20b)",
            "Vedi [qui](https://x.it)",
            """\# non un titolo""",
            """1\. non un elenco"""
        ).joinToString("\n\n") + "\n"
        assertEquals(expected, markdown)
    }

    @Test
    fun theMarksCloseInsideOut() {
        assertEquals("**a*b***", inlineMarkdown(listOf(RichTextSpan("a", bold = true), RichTextSpan("b", bold = true, italic = true))))
        // Gli spazi sempre fuori dai segni: "** y **" non sarebbe grassetto.
        assertEquals("x **y** z", inlineMarkdown(listOf(text("x"), RichTextSpan(" y ", bold = true), text("z"))))
        assertEquals("[~~qui~~](https://x.it)", inlineMarkdown(listOf(RichTextSpan("qui", strikethrough = true, link = "https://x.it"))))
        assertEquals("""snake\_case e 2\*3""", inlineMarkdown(listOf(text("snake_case e 2*3"))))
        assertEquals("H<sub>2</sub>O", inlineMarkdown(listOf(text("H"), RichTextSpan("2", subscript = true), text("O"))))
    }

    @Test
    fun fileNamesAreSafe() {
        assertEquals("Viaggio Roma Napoli", exportFileName("Viaggio: Roma/Napoli", "Untitled"))
        assertEquals("Untitled", exportFileName("  ", "Untitled"))
        assertEquals("Fine", exportFileName("Fine...", "Untitled"))
        val used = HashSet<String>()
        assertEquals("foto.jpg", PageExporter.unique("foto.jpg", used))
        assertEquals("foto 2.jpg", PageExporter.unique("foto.jpg", used))
        assertEquals("Foto 3.JPG", PageExporter.unique("Foto.JPG", used))
    }

    @Test
    fun aLongPageMakesAPdfOfManySheets() {
        val blocks = (1..120).map { block(BlockType.PARAGRAPH, text("Riga numero $it, abbastanza lunga da occupare un po' di spazio sul foglio.")) } +
            block(BlockType.CHECKBOX, text("Fatto"), checked = true) +
            block(BlockType.CODE, text("println(1)"))
        val page = ExportPage(PageEntity(id = "viaggio", title = "Lunga"), blocks, emptyList())
        // Il file PDF lo scrive Android (`PdfDocument`), che Robolectric non ha:
        // qui i fogli si disegnano su un'immagine.
        val export = PdfExport(fileOf = { null }, untitled = "Untitled")
        val sheets = export.layOut(listOf(page, page.copy(blocks = blocks.take(3))))
        // Più fogli per la pagina lunga, e la sottopagina su un foglio suo.
        assertTrue("fogli: ${sheets.list.size}", sheets.list.size >= 3)
        val bitmap = Bitmap.createBitmap(PdfExport.PAGE_WIDTH, PdfExport.PAGE_HEIGHT, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        export.draw(sheets, 0, AndroidPdfCanvas(Canvas(bitmap)))
        // Il titolo in alto a sinistra: scritto scuro sul bianco.
        val dark = (60 until 280).sumOf { x -> (56 until 84).count { y -> Color.red(bitmap.getPixel(x, y)) < 128 } }
        assertTrue("punti scuri nel titolo: $dark", dark > 100)
        // E niente oltre il margine di destra.
        assertTrue((PdfExport.PAGE_WIDTH - 40 until PdfExport.PAGE_WIDTH).all { x -> (0 until PdfExport.PAGE_HEIGHT step 7).all { y -> bitmap.getPixel(x, y) == Color.WHITE } })
    }

    @Test
    fun theZipHasTheFilesAndOnlyTheRealSubpages() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val repo = PageRepository(db)
            repo.createPage(PageEntity(id = "viaggio", title = "Viaggio"))
            repo.createPage(PageEntity(id = "hotel", parentId = "viaggio", title = "Hotel"))
            repo.createPage(PageEntity(id = "altrove", title = "Altrove"))
            PageImageStore(context).fileFor("abc.jpg").apply { parentFile?.mkdirs() }.writeBytes(byteArrayOf(1, 2, 3))
            db.blockDao().insertAll(
                listOf(
                    block(BlockType.PAGE_LINK, linked = "hotel"),
                    block(BlockType.PAGE_LINK, linked = "altrove"),
                    block(BlockType.IMAGE, attrs = BlockAttrs(file = "abc.jpg", name = "foto mare")),
                    block(BlockType.PARAGRAPH, text("Camera 12"), pageId = "hotel"),
                    // Un giro: l'hotel che riporta al viaggio non lo esporta due volte.
                    block(BlockType.PAGE_LINK, pageId = "hotel", linked = "viaggio")
                )
            )
            val exporter = PageExporter(context, repo)
            val zip = exporter.export("viaggio", ExportOptions(ExportFormat.MARKDOWN, subpages = true), untitled = "Untitled")!!
            assertEquals("Viaggio.zip", zip.name)
            assertEquals(PageExporter.ZIP_MIME, zip.mime)
            ZipFile(zip.file).use { file ->
                assertEquals(
                    listOf("Viaggio.md", "Viaggio/foto mare.jpg", "Viaggio/Hotel.md"),
                    file.entries().toList().map { it.name }
                )
                val main = file.getInputStream(file.getEntry("Viaggio.md")).readBytes().toString(Charsets.UTF_8)
                assertTrue(main, main.contains("📄 [Hotel](Viaggio/Hotel.md)"))
                assertTrue(main, main.contains("📄 Altrove\n"))
                assertTrue(main, main.contains("![foto mare](Viaggio/foto%20mare.jpg)"))
                val hotel = file.getInputStream(file.getEntry("Viaggio/Hotel.md")).readBytes().toString(Charsets.UTF_8)
                assertTrue(hotel, hotel.startsWith("# Hotel\n\nCamera 12"))
            }

            // Senza file né sottopagine, un .md da solo.
            val plain = exporter.export("hotel", ExportOptions(ExportFormat.MARKDOWN), untitled = "Untitled")!!
            assertEquals("Hotel.md", plain.name)
            assertTrue(plain.file.readText().startsWith("# Hotel\n\nCamera 12"))

        } finally {
            db.close()
        }
    }
}
