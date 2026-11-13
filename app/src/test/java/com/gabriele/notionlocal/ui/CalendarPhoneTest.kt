package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
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
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

/**
 * **Il calendario del mese sul telefono** (dal 13/11/2026, trovato
 * dall'utente: "su telefono adesso il calendario è come era prima su PC, con
 * troppo spazio inutile"): le settimane compatte anche qui, cinque eventi al
 * giorno e "Show more"/"Show less".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-mdpi")
class CalendarPhoneTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        AppDatabase.resetForTests()
        context.getDatabasePath("notion_local.db").delete()
        val db = AppDatabase.getInstance(context)
        val today = LocalDate.now()
        fun millis(hour: Int) = today.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        db.withTransaction {
            db.pageDao().insert(PageEntity(id = "agenda", title = "Agenda"))
            db.pageDao().insert(PageEntity(id = "eventi", title = "EVENTI", isDatabase = true, parentId = "agenda", databaseLayout = DatabaseLayout.CALENDAR))
            db.databaseDao().insertColumn(DatabaseColumnEntity(id = "quando", pageId = "eventi", name = "Date", type = ColumnType.DATE, orderIndex = 0))
            for (i in 1..8) {
                db.databaseDao().insertRow(DatabaseRowEntity(id = "e$i", pageId = "eventi", title = "Evento $i", orderIndex = i))
                db.databaseDao().insertCell(DatabaseCellEntity(rowId = "e$i", columnId = "quando", value = millis(8 + i).toString()))
            }
            db.blockDao().insert(BlockEntity(id = "cal", pageId = "agenda", type = BlockType.DATABASE_LINK, linkedPageId = "eventi", orderIndex = 0))
        }
    }

    @After
    fun tearDown() = AppDatabase.resetForTests()

    private fun events() = compose.onAllNodesWithText("Evento", substring = true).fetchSemanticsNodes().size

    @Test
    fun fiveEventsADayThenShowMore() {
        val factory = ViewModelFactory(context)
        compose.setContent {
            NotionLocalTheme {
                PageEditorScreen(pageId = "agenda", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {}, onNavigateToPage = {}, onNavigateToDatabase = {})
            }
        }
        compose.waitUntil(10_000) { events() > 0 }
        compose.waitForIdle()
        assertEquals(5, events())
        compose.onNodeWithText("Show more").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(8, events())
        compose.onNodeWithText("Show less").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(5, events())
    }
}
