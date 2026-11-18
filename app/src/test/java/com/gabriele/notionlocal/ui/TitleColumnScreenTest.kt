package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.rightClick
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.withTitleColumn
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **La colonna che dà il nome alle righe è la principale** (18/11/2026, chiesto dall'utente:
 * "cambiando il tipo della prima colonna, non ne creasse un'altra di backup, e che quella diventasse
 * e rimanesse a tutti gli effetti la colonna principale"): in un database semplice, dopo "Change
 * type" su Name, la pressione lunga e il clic destro sulle sue celle aprono la scheda della riga
 * (senza il campo del nome, che non scriverebbe niente), e Name non c'è più da nessuna parte: né
 * nella tabella (anche in una vista che non l'ha nascosta), né in "Property visibility", né in "Sort".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h900dp-mdpi")
class TitleColumnScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db get() = AppDatabase.getInstance(context)

    @Before
    fun setUp() {
        AppSettings.init(context)
        AppDatabase.resetForTests()
        context.getDatabasePath("notion_local.db").delete()
        runBlocking {
            db.withTransaction {
                db.pageDao().insert(PageEntity(id = "pagina", title = "Pagina di prova"))
                db.blockDao().insert(BlockEntity(pageId = "pagina", textJson = """[{"text":"Sopra"}]""", orderIndex = 0))
                // La colonna dei nomi, ma Name non segnata come nascosta: come in un'altra vista dello stesso database.
                db.pageDao().insert(
                    PageEntity(
                        id = "spesa", parentId = "pagina", title = "Spesa", isDatabase = true, isSimpleDatabase = true,
                        databaseLayout = DatabaseLayout.TABLE, viewHiddenColumnIds = withTitleColumn(null, "qty")
                    )
                )
                db.databaseDao().insertColumn(DatabaseColumnEntity(id = "qty", pageId = "spesa", name = "Quantità", type = ColumnType.NUMBER, orderIndex = 0))
                db.databaseDao().insertColumn(DatabaseColumnEntity(id = "note", pageId = "spesa", name = "Note", type = ColumnType.TEXT, orderIndex = 1))
                listOf("a" to "12", "b" to "34").forEachIndexed { i, (id, value) ->
                    db.databaseDao().insertRow(DatabaseRowEntity(id = id, pageId = "spesa", title = value, orderIndex = i))
                    db.databaseDao().insertCell(DatabaseCellEntity(rowId = id, columnId = "qty", value = value))
                }
                db.blockDao().insert(BlockEntity(pageId = "pagina", type = BlockType.DATABASE_LINK, linkedPageId = "spesa", orderIndex = 1))
            }
        }
        val factory = ViewModelFactory(context)
        compose.setContent {
            NotionLocalTheme {
                PageEditorScreen(pageId = "pagina", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {}, onNavigateToPage = {}, onNavigateToDatabase = {})
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Quantità").fetchSemanticsNodes().isNotEmpty() }
    }

    @After
    fun tearDown() {
        compose.waitForIdle()
        Thread.sleep(300)
        compose.waitForIdle()
        AppDatabase.resetForTests()
    }

    private fun shown(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun aLongPressOnTheTitleColumnOpensTheRow() {
        assertFalse("niente Name nella tabella", shown("Name"))
        compose.onAllNodesWithText("12").onFirst().performTouchInput { longClick() }
        compose.waitUntil(5_000) { shown("Row") }
        // Sotto, le proprietà: la colonna dei nomi c'è, il campo del nome in cima no (12 solo nella tabella e nella proprietà).
        assertEquals(2, compose.onAllNodesWithText("12").fetchSemanticsNodes().size)
    }

    @Test
    fun aRightClickOnTheTitleColumnOpensTheRow() {
        compose.onAllNodesWithText("34").onFirst().performMouseInput { rightClick() }
        compose.waitUntil(5_000) { shown("Row") }
    }

    @Test
    fun nameIsNotAmongThePropertiesNorTheSorts() {
        compose.onAllNodesWithContentDescription("Database settings").onFirst().performClick()
        compose.waitForIdle()
        compose.onAllNodesWithText("Property visibility").onLast().performScrollTo().performClick()
        compose.waitForIdle()
        assertTrue(shown("Note"))
        assertEquals("Quantità: l'intestazione e la voce", 2, compose.onAllNodesWithText("Quantità").fetchSemanticsNodes().size)
        assertFalse("Name non si rimostra", shown("Name"))
    }

    @Test
    fun nameIsNotAmongTheSorts() {
        compose.onAllNodesWithContentDescription("Sort").onFirst().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Note").fetchSemanticsNodes().size >= 2 }
        assertFalse("Name non c'è fra le scelte", shown("Name"))
    }
}
