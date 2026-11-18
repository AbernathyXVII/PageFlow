package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockTypeConverter
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.SelectOption
import com.gabriele.notionlocal.data.export.exportCellText
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.screen.TagCell
import com.gabriele.notionlocal.ui.screen.buildRowGroups
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.DatabaseTableState
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **La proprietà People** (18/11/2026, chiesta dall'utente: "tramite un sistema simile al Select o
 * Multi Select… senza collegamenti vari tra account"): un Multi-select di persone, col suo nome e
 * la sua icona; una riga con due persone sta nel gruppo di tutte e due.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h900dp-mdpi")
class PeopleColumnTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val people = DatabaseColumnEntity(
        id = "who", pageId = "db", name = "Chi", type = ColumnType.PEOPLE,
        optionsJson = Json.encodeToString(listOf(SelectOption("Marco", "#3A6590"), SelectOption("Giulia", "#7B4A9E")))
    )

    @Test
    fun peopleComesRightAfterMultiSelectWithItsName() {
        val types = ColumnType.entries
        assertEquals(types.indexOf(ColumnType.MULTI_SELECT) + 1, types.indexOf(ColumnType.PEOPLE))
        assertEquals("People", DbStrings.englishTypeName(ColumnType.PEOPLE))
        // Una proprietà di un tipo che questa versione non conosce si legge come testo.
        val converter = BlockTypeConverter()
        assertEquals(ColumnType.PEOPLE, converter.columnTypeOf(converter.columnTypeName(ColumnType.PEOPLE)))
        assertEquals(ColumnType.TEXT, converter.columnTypeOf("SOMETHING_NEWER"))
    }

    /** Nel database vero: People si scrive e si rilegge; un tipo di una versione più nuova si legge come testo. */
    @Test
    fun theDatabaseKeepsPeopleAndReadsAnUnknownTypeAsText() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AppSettings.init(context)
        AppDatabase.resetForTests()
        context.getDatabasePath("notion_local.db").delete()
        try {
            val db = AppDatabase.getInstance(context)
            db.pageDao().insert(PageEntity(id = "db", title = "Spesa", isDatabase = true))
            db.databaseDao().insertColumn(people)
            assertEquals(ColumnType.PEOPLE, db.databaseDao().getColumnsForPageOnce("db").single().type)
            db.openHelper.writableDatabase.execSQL("UPDATE database_columns SET type = 'SOMETHING_NEWER' WHERE id = 'who'")
            assertEquals(ColumnType.TEXT, db.databaseDao().getColumnsForPageOnce("db").single().type)
        } finally {
            AppDatabase.resetForTests()
        }
    }

    @Test
    fun aRowWithTwoPeopleIsInBothGroupsAndExportsBoth() {
        AppSettings.init(ApplicationProvider.getApplicationContext())
        val rows = listOf(
            DatabaseRowEntity(id = "a", pageId = "db", title = "Spesa"),
            DatabaseRowEntity(id = "b", pageId = "db", title = "Bollette")
        )
        val state = DatabaseTableState(columns = listOf(people), rows = rows, cellValues = mapOf(("a" to "who") to "Marco|Giulia", ("b" to "who") to "Giulia"))
        val groups = buildRowGroups(state, people, hideEmpty = true)
        assertEquals(listOf("Marco" to listOf("a"), "Giulia" to listOf("a", "b")), groups.map { g -> g.key to g.rows.map { it.id } })
        assertEquals("Marco, Giulia", exportCellText(people, rows[0], "Marco|Giulia", emptyMap(), emptyMap(), null))
    }

    @Test
    fun theCellChoosesMorePeople() {
        AppSettings.init(ApplicationProvider.getApplicationContext())
        val values = mutableListOf<String>()
        compose.setContent {
            NotionLocalTheme {
                TagCell(
                    column = people,
                    value = "Marco",
                    allowMultiple = true,
                    onValueChange = { values += it },
                    onAddOption = {},
                    onRenameOption = { _, _, _ -> },
                    onSetOptionColor = { _, _, _ -> },
                    onDeleteOption = { _, _ -> },
                    onSetOptionSecondColor = { _, _, _, _ -> },
                    onMoveOptions = { _, _ -> }
                )
            }
        }
        compose.onAllNodesWithText("Marco").onFirst().performClick()
        compose.waitForIdle()
        // La finestra dice "People", non "Multi-select".
        assertTrue(compose.onAllNodesWithText("People").fetchSemanticsNodes().isNotEmpty())
        compose.onAllNodesWithText("Giulia").onLast().performClick()
        compose.waitForIdle()
        assertEquals("Marco|Giulia", values.last())
    }
}
