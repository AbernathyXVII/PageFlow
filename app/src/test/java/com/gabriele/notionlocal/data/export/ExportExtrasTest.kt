package com.gabriele.notionlocal.data.export

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.buttons.ButtonBlock
import com.gabriele.notionlocal.data.buttons.ButtonConfig
import com.gabriele.notionlocal.data.buttons.ButtonFilter
import com.gabriele.notionlocal.data.buttons.ButtonStep
import com.gabriele.notionlocal.data.buttons.EditOp
import com.gabriele.notionlocal.data.buttons.InsertPlace
import com.gabriele.notionlocal.data.buttons.PropertyEdit
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.DatabaseSnapshot
import com.gabriele.notionlocal.data.entity.InkStrokeEntity
import com.gabriele.notionlocal.data.entity.InkTool
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.RelationConfig
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.encodeRelationConfig
import com.gabriele.notionlocal.data.ink.InkPoint
import com.gabriele.notionlocal.data.ink.encodeInkPoints
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.viewmodel.DatabaseTableState
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.zip.ZipFile

/**
 * **L'export completo** (03/11/2026, chiesto dall'utente: i disegni a
 * scelta, i bottoni con le loro istruzioni, i database "come lo fa Notion"
 * con le pagine delle righe che hanno qualcosa).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExportExtrasTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        AppSettings.init(context)
    }

    private fun text(blockId: String, pageId: String, text: String, order: Int) =
        BlockEntity(id = blockId, pageId = pageId, textJson = Json.encodeToString(listOf(RichTextSpan(text))), orderIndex = order)

    private val tasks = ExportDatabase(
        title = "Compiti",
        headers = listOf("Name", "Stato", "Fatto"),
        rows = listOf(
            ExportDatabaseRow("Spesa", listOf("Spesa", "Da fare", "✓"), page = ExportPage(PageEntity(id = "spesa", title = "Spesa"), emptyList(), emptyList())),
            ExportDatabaseRow("Bollette | luce", listOf("Bollette | luce", "Fatto", ""))
        )
    )

    @Test
    fun aDatabaseAndAButtonInMarkdown() {
        val page = ExportPage(
            page = PageEntity(id = "p", title = "Casa"),
            blocks = listOf(
                BlockEntity(id = "db", pageId = "p", type = BlockType.DATABASE_LINK, linkedPageId = "tasks", orderIndex = 0),
                BlockEntity(id = "btn", pageId = "p", type = BlockType.BUTTON, orderIndex = 1)
            ),
            cells = emptyList(),
            databases = mapOf("db" to tasks),
            buttons = mapOf("btn" to ExportButton("🌙 Fine giornata", listOf("1. Show confirmation: «Chiudo?»", "2. Send notification: «Fatto»")))
        )
        val markdown = markdownOf(
            page,
            "Untitled",
            databaseLinks = { MarkdownDatabaseLinks(csv = "Casa/Compiti.csv", rows = listOf("Casa/Compiti/Spesa.md", null)) }
        )
        assertEquals(
            listOf(
                "# Casa",
                "### Compiti",
                "| Name | Stato | Fatto |\n| --- | --- | --- |\n| [Spesa](Casa/Compiti/Spesa.md) | Da fare | ✓ |\n| Bollette \\| luce | Fatto |  |",
                "[CSV](Casa/Compiti.csv)",
                "> 🔘 **Fine giornata** — When tapped:\n> 1. Show confirmation: «Chiudo?»\n> 2. Send notification: «Fatto»"
                    .replace("**Fine giornata**", "**🌙 Fine giornata**")
            ).joinToString("\n\n") + "\n",
            markdown
        )
    }

    @Test
    fun theCsvIsLikeNotions() {
        val csv = csvOf(tasks.copy(csvHeaders = listOf("Name", "Stato", "Fatto", "Note"), rows = tasks.rows.map { it.copy(csvCells = it.cells + "a, \"b\"") }))
        assertEquals(
            "﻿Name,Stato,Fatto,Note\r\nSpesa,Da fare,✓,\"a, \"\"b\"\"\"\r\nBollette | luce,Fatto,,\"a, \"\"b\"\"\"\r\n",
            csv
        )
    }

    @Test
    fun aButtonSaysWhatItDoes() {
        val config = ButtonConfig(
            name = "Fine giornata",
            icon = "🌙",
            steps = listOf(
                ButtonStep.Confirm(message = "Chiudo la giornata?"),
                ButtonStep.InsertBlocks(blocks = listOf(ButtonBlock(type = "CHECKBOX", text = "Bere acqua"), ButtonBlock(type = "DIVIDER")), place = InsertPlace.BELOW_BUTTON),
                ButtonStep.AddPage(databaseId = "tasks", title = "Diario", edits = listOf(PropertyEdit(columnId = "status", op = EditOp.ADD, value = "Casa|Urgente")), openAfter = true),
                ButtonStep.EditPages(databaseId = "tasks", filter = ButtonFilter("done", listOf("false")), edits = listOf(PropertyEdit(columnId = "count", op = EditOp.ADD, value = "1"))),
                ButtonStep.Open(url = "https://notion.so"),
                ButtonStep.Notify(title = "Fatto", text = "Buonanotte")
            )
        )
        val columns = listOf(
            DatabaseColumnEntity(id = "status", pageId = "tasks", name = "Status", type = ColumnType.MULTI_SELECT),
            DatabaseColumnEntity(id = "done", pageId = "tasks", name = "Done", type = ColumnType.CHECKBOX),
            DatabaseColumnEntity(id = "count", pageId = "tasks", name = "Count", type = ColumnType.NUMBER)
        )
        assertEquals(Pair(setOf("tasks"), setOf("tasks")), buttonReferences(config))
        val button = exportButton(config, ButtonNames(mapOf("tasks" to "Tasks"), mapOf("tasks" to columns)), "Untitled")
        assertEquals("🌙 Fine giornata", button.name)
        assertEquals(
            listOf(
                "1. Show confirmation: «Chiudo la giornata?»",
                "2. Insert blocks (below button): To-do list «Bere acqua», Divider",
                "3. Add page to «Tasks»: «Diario» — Status: Add «Casa, Urgente» — open it afterwards",
                "4. Edit pages in «Tasks» — only pages where Done is «Unchecked»: Count: Increase by «1»",
                "5. Open page or URL: https://notion.so",
                "6. Send notification: «Fatto» — Buonanotte"
            ),
            button.steps
        )
    }

    @Test
    fun theDatabaseIsWhatItsViewShows() {
        val host = PageEntity(id = "tasks", title = "Tasks", isDatabase = true, nameColumnOrder = 1)
        val columns = listOf(
            DatabaseColumnEntity(id = "done", pageId = "tasks", name = "Done", type = ColumnType.CHECKBOX, orderIndex = 0),
            DatabaseColumnEntity(id = "tags", pageId = "tasks", name = "Tags", type = ColumnType.MULTI_SELECT, orderIndex = 1, hidden = true),
            DatabaseColumnEntity(
                id = "who", pageId = "tasks", name = "Who", type = ColumnType.RELATION, orderIndex = 2,
                optionsJson = encodeRelationConfig(RelationConfig(database = "people"))
            )
        )
        val rows = listOf(
            DatabaseRowEntity(id = "r1", pageId = "tasks", title = "Spesa", linkedPageId = "spesa"),
            DatabaseRowEntity(id = "r2", pageId = "tasks", title = "Bollette")
        )
        val state = DatabaseTableState(
            columns = columns,
            rows = rows,
            cellValues = mapOf(("r1" to "done") to "true", ("r1" to "tags") to "Casa|Urgente", ("r2" to "who") to "p1")
        )
        val people = DatabaseSnapshot(
            columns = emptyList(),
            rows = listOf(DatabaseRowEntity(id = "p1", pageId = "people", title = "Marco")),
            cells = emptyMap()
        )
        val read = databaseRead(host, host, host, state, mapOf("people" to people), emptyMap())
        // Name dopo la prima colonna (`nameColumnOrder` 1); Tags è nascosta.
        assertEquals(listOf("Done", "Name", "Who"), read.database.headers)
        assertEquals(1, read.database.nameIndex)
        assertEquals(listOf(listOf("✓", "Spesa", ""), listOf("", "Bollette", "Marco")), read.database.rows.map { it.cells })
        // Nel CSV anche la colonna nascosta.
        assertEquals(listOf("Done", "Name", "Tags", "Who"), read.database.csvHeaders)
        assertEquals(listOf("✓", "Spesa", "Casa, Urgente", ""), read.database.rows[0].csvCells)
        assertEquals(listOf("spesa", null), read.rowPageIds)
    }

    @Test
    fun drawingsGoOnThePdfWhereTheyWereMade() {
        val red = 0xFFE03E3E
        val page = ExportPage(
            page = PageEntity(id = "p", title = "Schizzi"),
            blocks = listOf(text("a", "p", "Prima riga", 0), text("b", "p", "Seconda riga", 1)),
            cells = emptyList(),
            strokes = listOf(
                InkStrokeEntity(
                    pageId = "p", anchor = "a,b", tool = InkTool.MARKER, color = red, width = 6f,
                    points = encodeInkPoints(listOf(InkPoint(40f, 12f), InkPoint(300f, 12f)))
                )
            )
        )
        val layout = PdfLayout.phone(411f)
        val export = PdfExport(fileOf = { null }, untitled = "Untitled", layout = layout)
        val sheets = export.layOut(listOf(page))
        val bitmap = Bitmap.createBitmap(layout.pageWidth, layout.pageHeight, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        export.draw(sheets, 0, AndroidPdfCanvas(Canvas(bitmap)))
        // Il tratto rosso c'è, largo quanto sul telefono (da 40 a 300 dp), sotto il titolo.
        val s = layout.dpScale!!
        val redRows = (0 until layout.pageHeight).filter { y ->
            val px = bitmap.getPixel((170 * s).toInt(), y)
            Color.red(px) > 180 && Color.green(px) < 120
        }
        assertTrue("righe rosse: $redRows", redRows.isNotEmpty())
        assertTrue(redRows.first() > layout.titleTop)
        val row = redRows[redRows.size / 2]
        assertTrue(Color.red(bitmap.getPixel((45 * s).toInt(), row)) > 180)
        assertEquals(Color.WHITE, bitmap.getPixel((310 * s).toInt(), row))
    }

    @Test
    fun aDatabaseWithManyColumnsBecomesCards() {
        val many = ExportDatabase(
            title = "Larga",
            headers = listOf("Name", "A", "B", "C", "D", "E", "F"),
            rows = listOf(ExportDatabaseRow("Riga", listOf("Riga", "1", "2", "3", "4", "5", "6")))
        )
        val page = ExportPage(
            page = PageEntity(id = "p", title = "Pagina"),
            blocks = listOf(BlockEntity(id = "db", pageId = "p", type = BlockType.DATABASE_LINK, linkedPageId = "x", orderIndex = 0)),
            cells = emptyList(),
            databases = mapOf("db" to many)
        )
        val export = PdfExport(fileOf = { null }, untitled = "Untitled")
        val sheets = export.layOut(listOf(page))
        val bitmap = Bitmap.createBitmap(PdfExport.PAGE_WIDTH, PdfExport.PAGE_HEIGHT, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        export.draw(sheets, 0, AndroidPdfCanvas(Canvas(bitmap)))
        // Niente griglia grigia larga tutta la pagina: una scheda.
        assertEquals(1, sheets.list.size)
    }

    @Test
    fun theZipHasTheCsvAndThePagesOfTheRowsWithSomethingWritten() = runBlocking<Unit> {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val repo = PageRepository(db)
            repo.createPage(PageEntity(id = "casa", title = "Casa"))
            repo.createPage(PageEntity(id = "tasks", title = "Compiti", isDatabase = true))
            repo.createPage(PageEntity(id = "spesa", title = "Spesa", isRowPage = true))
            repo.createPage(PageEntity(id = "vuota", title = "Vuota", isRowPage = true))
            db.blockDao().insertAll(
                listOf(
                    BlockEntity(id = "db", pageId = "casa", type = BlockType.DATABASE_LINK, linkedPageId = "tasks", orderIndex = 0),
                    text("latte", "spesa", "Latte e pane", 0),
                    // Una riga vuota non è "qualcosa di scritto".
                    text("niente", "vuota", "", 0)
                )
            )
            val source = object : DatabaseSource {
                override suspend fun read(databaseId: String) = DatabaseRead(
                    ExportDatabase(
                        title = "Compiti",
                        headers = listOf("Name", "Stato"),
                        rows = listOf(ExportDatabaseRow("Spesa", listOf("Spesa", "Da fare")), ExportDatabaseRow("Vuota", listOf("Vuota", "")))
                    ),
                    listOf("spesa", "vuota")
                )
            }
            val zip = PageExporter(context, repo, source).export("casa", ExportOptions(ExportFormat.MARKDOWN), "Untitled")!!
            assertEquals("Casa.zip", zip.name)
            ZipFile(zip.file).use { file ->
                assertEquals(listOf("Casa.md", "Casa/Compiti.csv", "Casa/Compiti/Spesa.md"), file.entries().toList().map { it.name })
                val main = file.getInputStream(file.getEntry("Casa.md")).readBytes().toString(Charsets.UTF_8)
                assertTrue(main, main.contains("| [Spesa](Casa/Compiti/Spesa.md) | Da fare |"))
                assertTrue(main, main.contains("| Vuota |  |"))
                assertTrue(main, main.contains("[CSV](Casa/Compiti.csv)"))
                val row = file.getInputStream(file.getEntry("Casa/Compiti/Spesa.md")).readBytes().toString(Charsets.UTF_8)
                assertEquals("# Spesa\n\nLatte e pane\n", row)
            }
        } finally {
            db.close()
        }
    }
}
