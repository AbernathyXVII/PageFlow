package com.gabriele.notionlocal.ui

import java.time.Duration
import org.robolectric.Shadows.shadowOf
import com.gabriele.notionlocal.data.LinkTitles
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsActions
import android.os.Looper
import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
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
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.PROPERTY_MENU_TAG
import com.gabriele.notionlocal.ui.screen.PROPERTY_NAME_FIELD_TAG
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
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
 * **Il menu di una proprietà** (27/10/2026, chiesto dall'utente con uno
 * screenshot di Notion): si apre toccando l'intestazione, e tenerla premuta
 * non fa più niente. Dentro le voci di prima nello stesso ordine, con
 * "Change type", "Filter" e "Sort" ("Filter" è la stessa finestra dell'icona
 * in cima; "Sort" dal 02/11/2026 ordina solo per quella proprietà, vedi
 * `PropertySortTest`). "Edit property" non c'è
 * più (l'utente: "togli Edit property proprio"): il nome si scrive nella
 * riga in cima, e "Delete property" sta in fondo, con la domanda.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-mdpi")
class PropertyMenuTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        AppDatabase.resetForTests()
        context.getDatabasePath("notion_local.db").delete()
        val db = AppDatabase.getInstance(context)
        db.withTransaction {
            db.pageDao().insert(PageEntity(id = "pagina", title = "Pagina di prova"))
            db.pageDao().insert(PageEntity(id = "giochi", parentId = "pagina", title = "Giochi", isDatabase = true))
            db.databaseDao().insertColumn(
                DatabaseColumnEntity(
                    id = "console", pageId = "giochi", name = "Console", type = ColumnType.SELECT, orderIndex = 0,
                    optionsJson = """[{"label":"Xbox"},{"label":"Wii"}]"""
                )
            )
            db.databaseDao().insertColumn(DatabaseColumnEntity(id = "voto", pageId = "giochi", name = "Voto", type = ColumnType.NUMBER, orderIndex = 1))
            db.databaseDao().insertRow(DatabaseRowEntity(id = "r1", pageId = "giochi", title = "Zelda", orderIndex = 0))
            db.databaseDao().insertCell(DatabaseCellEntity(rowId = "r1", columnId = "console", value = "Wii"))
            db.blockDao().insert(BlockEntity(pageId = "pagina", type = BlockType.DATABASE_LINK, linkedPageId = "giochi", orderIndex = 0))
        }
    }

    @After
    fun tearDown() = AppDatabase.resetForTests()

    private fun open() {
        val factory = ViewModelFactory(context)
        compose.setContent {
            NotionLocalTheme {
                PageEditorScreen(pageId = "pagina", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {}, onNavigateToPage = {}, onNavigateToDatabase = {})
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Console").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun menuShown() = compose.onAllNodesWithTag(PROPERTY_MENU_TAG).fetchSemanticsNodes().isNotEmpty()

    private fun column(id: String) = runBlocking {
        AppDatabase.getInstance(context).databaseDao().getColumnsForPageOnce("giochi").single { it.id == id }
    }

    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(5_000) {
            Thread.sleep(20)
            condition()
        }
        compose.waitForIdle()
    }

    /** La cima di una voce del menu, per controllarne l'ordine (anche quelle più giù, da scorrere). */
    private fun topOf(label: String): Float =
        compose.onAllNodesWithText(label).fetchSemanticsNodes().last().positionInRoot.y

    @Test
    fun aTapOpensTheMenuWithTheSameItemsInOrderAndALongPressDoesNothing() {
        open()
        compose.onNodeWithText("Console").performTouchInput { longClick() }
        compose.waitForIdle()
        assertTrue("tenendo premuto non si apre niente", !menuShown())

        compose.onNodeWithText("Console").performClick()
        compose.waitForIdle()
        assertTrue(menuShown())
        val order = listOf(
            "Move to left", "Move to right", "Change type", "Filter", "Sort",
            "Align left", "Center", "Align right", "Resize column", "Fit to content", "Hide", "Hide icon", "Calculate",
            "Delete property"
        )
        val tops = order.map(::topOf)
        assertEquals("le voci nell'ordine: $order", tops.sorted(), tops)
        // "Edit property" non c'è più; le caselle nemmeno: questa non è una colonna di caselle.
        assertTrue(compose.onAllNodesWithText("Edit property").fetchSemanticsNodes().isEmpty())
        assertTrue(compose.onAllNodesWithText("Activate all").fetchSemanticsNodes().isEmpty())
    }

    /**
     * **"Show as" di una proprietà Link** (13/11/2026, chiesto dall'utente):
     * subito sotto il nome, prima di tutto il resto, e solo per lei; "Page
     * title" lo salva per la proprietà e le celle mostrano i titoli.
     */
    @Test
    fun aLinkPropertyHasShowAsUnderItsName() {
        val original = LinkTitles.source
        LinkTitles.source = { url -> if (url == "https://example.com/zelda") "Zelda - La wiki" else null }
        try {
            runBlocking {
                val db = AppDatabase.getInstance(context)
                db.databaseDao().insertColumn(DatabaseColumnEntity(id = "sito", pageId = "giochi", name = "Sito", type = ColumnType.URL, orderIndex = -1))
                db.databaseDao().insertCell(DatabaseCellEntity(rowId = "r1", columnId = "sito", value = "https://example.com/zelda"))
            }
            open()
            compose.onNodeWithText("Sito").performClick()
            compose.waitForIdle()
            assertTrue("\"Show as\" sotto il nome, prima di \"Move to left\"", topOf("Show as") < topOf("Move to left"))
            compose.onNodeWithText("Page title").performClick()
            waitFor { column("sito").numberDisplay == "TITLE" }
            compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)).performSemanticsAction(SemanticsActions.Dismiss)
            compose.waitUntil(5_000) {
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
                compose.onAllNodesWithText("Zelda - La wiki").fetchSemanticsNodes().isNotEmpty()
            }
            // Le altre proprietà non ce l'hanno.
            compose.onNodeWithText("Voto").performClick()
            compose.waitForIdle()
            assertTrue(compose.onAllNodesWithText("Page title").fetchSemanticsNodes().isEmpty())
        } finally {
            LinkTitles.source = original
        }
    }

    @Test
    fun changeTypeHasTheTypesAndChangesIt() {
        open()
        compose.onNodeWithText("Voto").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Change type").performClick()
        compose.waitForIdle()
        assertTrue(!menuShown())
        // Solo i tipi, col titolo "Change type": il nome non si scrive qui.
        compose.onAllNodesWithText("Change type").onLast().assertExists()
        assertTrue(compose.onAllNodesWithText("Property name").fetchSemanticsNodes().isEmpty())
        compose.onNodeWithText("Checkbox").performScrollTo().performClick()
        compose.onNodeWithText("Save").performClick()
        waitFor { column("voto").type == ColumnType.CHECKBOX }
        assertEquals("Voto", column("voto").name)
    }

    @Test
    fun choosingATypeWithoutSettingsMovesNothing() {
        // 27/10/2026, l'utente: scegliendo un tipo "è come se si ingrandisse e
        // sposta di poco [...] quello sotto". Le righe dei tipi restano alla
        // stessa distanza, qualunque sia quello scelto (Number, che ha le sue
        // impostazioni, sta in cima: è quello di adesso).
        open()
        compose.onNodeWithText("Voto").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Change type").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Date").performScrollTo().performClick()
        compose.waitForIdle()
        val date = topOf("Date")
        val duration = topOf("Duration")
        val checkbox = topOf("Checkbox")
        assertEquals("fra Date (scelto) e Duration come fra Duration e Checkbox", checkbox - duration, duration - date, 0.5f)
    }

    @Test
    fun theNameIsWrittenInTheTopLine() {
        open()
        compose.onNodeWithText("Voto").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(PROPERTY_NAME_FIELD_TAG).performTextReplacement("Punteggio")
        // Chiudendo il menu il nome nuovo vale: qui con una voce che non cambia
        // niente ("Filter" apre la sua finestra), sul telefono anche col tasto indietro.
        compose.onNodeWithText("Filter").performClick()
        waitFor { column("voto").name == "Punteggio" }
        assertEquals(ColumnType.NUMBER, column("voto").type)
    }

    @Test
    fun deletePropertyAsksFirst() {
        open()
        compose.onNodeWithText("Voto").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Delete property").performScrollTo().performClick()
        compose.waitForIdle()
        // Prima la domanda: "Cancel" lascia tutto com'è.
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()
        assertEquals(2, runBlocking { AppDatabase.getInstance(context).databaseDao().getColumnsForPageOnce("giochi").size })
        compose.onNodeWithText("Voto").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Delete property").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Delete").performClick()
        waitFor { runBlocking { AppDatabase.getInstance(context).databaseDao().getColumnsForPageOnce("giochi").none { it.id == "voto" } } }
    }

    @Test
    fun filterStartsFromThePropertyAndIsTheDatabaseFilter() {
        open()
        // "Filter" parte dai valori di questa proprietà: "Xbox" c'è subito,
        // senza scegliere prima la proprietà. Scelto, è il filtro del database,
        // quello dell'icona in cima.
        compose.onNodeWithText("Console").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Filter").performClick()
        compose.waitForIdle()
        assertTrue(!menuShown())
        compose.onAllNodesWithText("Xbox").onLast().performClick()
        waitFor {
            runBlocking { AppDatabase.getInstance(context).pageDao().getById("giochi")!!.filterColumnId == "console" }
        }
    }

    // "Sort" (dal 02/11/2026) ordina solo per la proprietà: vedi `PropertySortTest`.
}
