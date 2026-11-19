package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.formula.FormulaConfig
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.FORMULA_CELL_TAG
import com.gabriele.notionlocal.ui.screen.FORMULA_CHIP_TAG
import com.gabriele.notionlocal.ui.screen.FORMULA_FIELD_TAG
import com.gabriele.notionlocal.ui.screen.FORMULA_PREVIEW_TAG
import com.gabriele.notionlocal.ui.screen.PROPERTY_MENU_TAG
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.screen.inserting
import com.gabriele.notionlocal.ui.screen.operatorSnippet
import com.gabriele.notionlocal.ui.screen.propReference
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **Le formule sullo schermo** (28/10/2026), nella pagina vera con la
 * tabella vera: la cella col risultato, la casella di un sì/no, l'errore,
 * la finestra che si apre toccando la cella (con l'anteprima su quella
 * riga), il menu dell'intestazione e una proprietà Formula nuova.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h900dp-mdpi")
class FormulaScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db get() = AppDatabase.getInstance(context)

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        AppDatabase.resetForTests()
        context.getDatabasePath("notion_local.db").delete()
        db.withTransaction {
            db.pageDao().insert(PageEntity(id = "pagina", title = "Pagina di prova"))
            db.blockDao().insert(BlockEntity(pageId = "pagina", textJson = """[{"text":"Sopra"}]""", orderIndex = 0))
            db.pageDao().insert(PageEntity(id = "spesa", parentId = "pagina", title = "Spesa", isDatabase = true))
            db.databaseDao().insertColumn(DatabaseColumnEntity(id = "prezzo", pageId = "spesa", name = "Prezzo", type = ColumnType.NUMBER, orderIndex = 0))
            db.databaseDao().insertColumn(DatabaseColumnEntity(id = "quanti", pageId = "spesa", name = "Quanti", type = ColumnType.NUMBER, orderIndex = 1))
            db.databaseDao().insertColumn(formula("totale", "Totale", "prop(\"Prezzo\") * prop(\"Quanti\")", 2))
            db.databaseDao().insertColumn(formula("caro", "Caro", "prop(\"Prezzo\") > 2", 3))
            db.databaseDao().insertRow(DatabaseRowEntity(id = "latte", pageId = "spesa", title = "Latte", orderIndex = 0))
            db.databaseDao().insertRow(DatabaseRowEntity(id = "uova", pageId = "spesa", title = "Uova", orderIndex = 1))
            db.databaseDao().insertCell(DatabaseCellEntity(rowId = "latte", columnId = "prezzo", value = "1.5"))
            db.databaseDao().insertCell(DatabaseCellEntity(rowId = "latte", columnId = "quanti", value = "4"))
            db.databaseDao().insertCell(DatabaseCellEntity(rowId = "uova", columnId = "prezzo", value = "3.25"))
            db.databaseDao().insertCell(DatabaseCellEntity(rowId = "uova", columnId = "quanti", value = "10"))
            db.blockDao().insert(BlockEntity(pageId = "pagina", type = BlockType.DATABASE_LINK, linkedPageId = "spesa", orderIndex = 1))
        }
    }

    @After
    fun tearDown() {
        compose.waitForIdle()
        Thread.sleep(300)
        compose.waitForIdle()
        AppDatabase.resetForTests()
    }

    private fun formula(id: String, name: String, expression: String, order: Int) = DatabaseColumnEntity(
        id = id, pageId = "spesa", name = name, type = ColumnType.FORMULA, orderIndex = order,
        optionsJson = FormulaConfig.encode(FormulaConfig(expression))
    )

    private fun open() {
        val factory = ViewModelFactory(context)
        compose.setContent {
            NotionLocalTheme {
                PageEditorScreen(pageId = "pagina", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {}, onNavigateToPage = {}, onNavigateToDatabase = {})
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Sopra").fetchSemanticsNodes().isNotEmpty() }
    }

    /** Aspetta [condition] lasciando andare ogni volta il thread principale, dove partono i salvataggi. */
    private fun waitFor(what: String, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < end) {
            compose.waitForIdle()
            if (condition()) return
            Thread.sleep(20)
        }
        throw AssertionError("Non è successo: $what")
    }

    private fun shown(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    private fun chip(label: String) = compose.onNode(hasTestTag(FORMULA_CHIP_TAG) and hasText(label))
    private fun expression(columnId: String) =
        runBlocking { FormulaConfig.parse(db.databaseDao().getColumnById(columnId)!!.optionsJson).expression }

    @Test
    fun theCellsShowTheResults() {
        open()
        waitFor("i totali") { shown("6") && shown("32.5") }
        // Il sì/no è una casella: spuntata per le uova (3,25 > 2), vuota per il latte.
        waitFor("le caselle") {
            compose.onAllNodes(hasContentDescription("Checked")).fetchSemanticsNodes().size == 1 &&
                compose.onAllNodes(hasContentDescription("Unchecked")).fetchSemanticsNodes().size == 1
        }
    }

    @Test
    fun tappingACellOpensTheFormulaWithThePreviewOnThatRow() {
        open()
        waitFor("i totali") { shown("32.5") }
        // La terza cella di formula, dall'alto e da sinistra: il Totale delle uova.
        compose.onAllNodes(hasTestTag(FORMULA_CELL_TAG) and hasText("32.5")).onFirst().performScrollTo().performClick()
        waitFor("la finestra") { compose.onAllNodesWithTag(FORMULA_FIELD_TAG).fetchSemanticsNodes().isNotEmpty() }
        assertTrue(shown("prop(\"Prezzo\") * prop(\"Quanti\")"))
        assertTrue(shown("Result for “Uova”"))

        // Si riscrive: l'anteprima cambia mentre si scrive.
        compose.onNodeWithTag(FORMULA_FIELD_TAG).performTextClearance()
        compose.onNodeWithTag(FORMULA_FIELD_TAG).performTextInput("round(prop(\"Prezzo\") * 1.1, 2)")
        waitFor("l'anteprima") { compose.onAllNodes(hasText("3.58")).fetchSemanticsNodes().isNotEmpty() }

        // Scritta male, dice che cosa non va.
        compose.onNodeWithTag(FORMULA_FIELD_TAG).performTextClearance()
        compose.onNodeWithTag(FORMULA_FIELD_TAG).performTextInput("prop(\"Boh\") + ")
        waitFor("l'errore") { shown("The formula stops halfway") }
        compose.onNodeWithTag(FORMULA_FIELD_TAG).performTextInput("1")
        waitFor("l'altro errore") { shown("There's no property “Boh”") }

        // Le proprietà da toccare: prop("…") dove sta il cursore.
        compose.onNodeWithTag(FORMULA_FIELD_TAG).performTextClearance()
        chip("Quanti").performScrollTo().performClick()
        chip("+").performScrollTo().performClick()
        chip("Prezzo").performScrollTo().performClick()
        compose.waitForIdle()
        assertTrue(shown("prop(\"Quanti\") + prop(\"Prezzo\")"))
        // Solo l'anteprima dice 13,25: nella tabella le uova fanno ancora 32,5.
        waitFor("l'anteprima della somma") {
            compose.onAllNodes(hasAnyAncestor(hasTestTag(FORMULA_PREVIEW_TAG)) and hasText("13.25")).fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithText("Done").performClick()
        waitFor("salvata") { expression("totale") == "prop(\"Quanti\") + prop(\"Prezzo\")" }
        waitFor("la cella nuova") { shown("13.25") && shown("5.5") }
    }

    @Test
    fun aBrokenFormulaShowsErrorInTheCell() {
        runBlocking { db.databaseDao().updateColumn(formula("totale", "Totale", "prop(\"Prezzo\") * ", 2)) }
        open()
        waitFor("gli errori") { compose.onAllNodes(hasTestTag(FORMULA_CELL_TAG) and hasText("Error", substring = true)).fetchSemanticsNodes().size == 2 }
    }

    @Test
    fun theHeaderMenuOfAFormulaHasEditFormulaAndSortButNoFilter() {
        open()
        waitFor("l'intestazione") { shown("Totale") }
        compose.onAllNodes(hasText("Totale") and !hasTestTag(FORMULA_CELL_TAG)).onFirst().performScrollTo().performClick()
        waitFor("il menu") { compose.onAllNodesWithTag(PROPERTY_MENU_TAG).fetchSemanticsNodes().isNotEmpty() }
        assertTrue(shown("Edit formula"))
        assertTrue(shown("Sort"))
        assertTrue(!shown("Filter"))
        compose.onNodeWithText("Edit formula").performClick()
        waitFor("la finestra") { compose.onAllNodesWithTag(FORMULA_FIELD_TAG).fetchSemanticsNodes().isNotEmpty() }
        // Dall'intestazione, l'anteprima è sulla prima riga.
        assertTrue(shown("Result for “Latte”"))
    }

    @Test
    fun changeTypeToFormulaWritesTheFormulaUnderIt() {
        open()
        waitFor("l'intestazione") { shown("Quanti") }
        compose.onAllNodes(hasText("Quanti")).onFirst().performScrollTo().performClick()
        waitFor("il menu") { compose.onAllNodesWithTag(PROPERTY_MENU_TAG).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Change type").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Formula").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Edit formula · No formula yet").performScrollTo().performClick()
        waitFor("la finestra") { compose.onAllNodesWithTag(FORMULA_FIELD_TAG).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(FORMULA_FIELD_TAG).performTextInput("prop(\"Prezzo\") * 2")
        compose.onNodeWithText("Done").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Edit formula · prop(\"Prezzo\") * 2").performScrollTo().assertExists()
        compose.onNodeWithText("Save").performClick()
        // Prima di cambiare tipo si chiede (dal 19/11/2026).
        compose.onNode(hasText("Change type") and hasClickAction()).performClick()
        waitFor("la proprietà è una formula") {
            runBlocking { db.databaseDao().getColumnById("quanti")!!.type } == ColumnType.FORMULA
        }
        assertEquals("prop(\"Prezzo\") * 2", expression("quanti"))
        // Totale leggeva Quanti, che adesso è una formula: si calcola lo stesso (1,5 × 3 = 4,5).
        waitFor("i valori nuovi") { shown("3") && shown("6.5") && shown("4.5") }
    }

    // --- Inserire dalla finestra ---

    @Test
    fun insertingGoesWhereTheCursorIs() {
        val start = TextFieldValue("1 + ", TextRange(4))
        assertEquals(TextFieldValue("1 + prop(\"A\")", TextRange(13)), start.inserting(propReference("A")))
        // Una funzione: il cursore fra le parentesi.
        assertEquals(TextFieldValue("round()", TextRange(6)), TextFieldValue("").inserting("round()", 1))
        // Dopo una parola, uno spazio prima della prossima.
        assertEquals("prop(\"A\") and ", TextFieldValue("prop(\"A\")", TextRange(9)).inserting(operatorSnippet("and").first).text)
        assertEquals("prop(\"A\") + ", TextFieldValue("prop(\"A\")", TextRange(9)).inserting(operatorSnippet("+").first).text)
        // Le virgolette: il cursore dentro.
        val quotes = operatorSnippet("\"\"")
        assertEquals(TextFieldValue("if(\"\")", TextRange(4)), TextFieldValue("if()", TextRange(3)).inserting(quotes.first, quotes.second))
        // Al posto della parte scelta.
        assertEquals("1 * 2", TextFieldValue("1 + 2", TextRange(2, 3)).inserting("*").text)
        // Un nome con le virgolette si protegge.
        assertEquals("prop(\"Il \\\"costo\\\"\")", propReference("Il \"costo\""))
    }
}
