package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
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
import com.gabriele.notionlocal.data.entity.PageParagraph
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.i18n.DbStrings
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
 * **"Cell spoiler" sul telefono** (13/11/2026, chiesto dall'utente): acceso
 * da "Database settings", toccando una cella col dito la si copre, e non ci si
 * scrive; toccandola di nuovo si scopre. Qui in un database dentro una pagina.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-mdpi")
class CellSpoilerTest {

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
            db.pageDao().insert(PageEntity(id = "parole", parentId = "pagina", title = "Parole", isDatabase = true))
            db.databaseDao().insertColumn(DatabaseColumnEntity(id = "senso", pageId = "parole", name = "Meaning", type = ColumnType.TEXT, orderIndex = 0))
            db.databaseDao().insertRow(DatabaseRowEntity(id = "r1", pageId = "parole", title = "Ringo", orderIndex = 0))
            db.databaseDao().insertCell(DatabaseCellEntity(rowId = "r1", columnId = "senso", value = "Mela"))
            db.blockDao().insert(BlockEntity(pageId = "pagina", type = BlockType.DATABASE_LINK, linkedPageId = "parole", orderIndex = 0))
        }
    }

    @After
    fun tearDown() = AppDatabase.resetForTests()

    private fun stored() = runBlocking { PageParagraph.parse(AppDatabase.getInstance(context).pageDao().getById("parole")!!.paragraphJson) }

    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(5_000) {
            Thread.sleep(20)
            condition()
        }
        compose.waitForIdle()
    }

    @Test
    fun aTapCoversTheCellAndAnotherUncoversIt() {
        val factory = ViewModelFactory(context)
        compose.setContent {
            NotionLocalTheme {
                PageEditorScreen(pageId = "pagina", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {}, onNavigateToPage = {}, onNavigateToDatabase = {})
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Mela").fetchSemanticsNodes().isNotEmpty() }

        // Acceso da "Database settings".
        compose.onNodeWithContentDescription(DbStrings.databaseSettings).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(DbStrings.cellSpoiler).performScrollTo().performClick()
        waitFor { stored().cellSpoiler == true }
        // Chiusa la finestra, come trascinandola giù.
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)).performSemanticsAction(SemanticsActions.Dismiss)
        compose.waitUntil(5_000) { compose.onAllNodesWithText(DbStrings.cellSpoiler).fetchSemanticsNodes().isEmpty() }

        // Toccata, la cella si copre e non si apre per scriverci.
        compose.onNodeWithText("Mela").performClick()
        waitFor { stored().coveredCells == listOf("r1/senso") }
        assertTrue("nessun campo col cursore", compose.onAllNodes(isFocused() and hasSetTextAction()).fetchSemanticsNodes().isEmpty())
        assertEquals("Mela", runBlocking { AppDatabase.getInstance(context).databaseDao().getCellsForRowOnce("r1").single().value })

        // Toccata di nuovo, si scopre.
        compose.onNodeWithText("Mela").performClick()
        waitFor { stored().coveredCells == null }
    }
}
