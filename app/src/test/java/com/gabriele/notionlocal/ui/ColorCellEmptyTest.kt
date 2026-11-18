package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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

/**
 * **Una cella "Color cell" vuota resta vuota** (18/11/2026, chiesto dall'utente: "di default appare
 * una lineetta in tutte le celle. Toglimela"): niente trattino; il testo scritto sopra il colore c'è.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h900dp-mdpi")
class ColorCellEmptyTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db get() = AppDatabase.getInstance(context)

    @Before
    fun setUp() {
        AppSettings.init(context)
        AppDatabase.resetForTests()
        context.getDatabasePath("notion_local.db").delete()
    }

    @After
    fun tearDown() {
        compose.waitForIdle()
        Thread.sleep(300)
        compose.waitForIdle()
        AppDatabase.resetForTests()
    }

    @Test
    fun anEmptyColorCellHasNoDash() {
        runBlocking {
            db.withTransaction {
                db.pageDao().insert(PageEntity(id = "pagina", title = "Pagina di prova"))
                db.pageDao().insert(PageEntity(id = "umore", parentId = "pagina", title = "Umore", isDatabase = true, databaseLayout = DatabaseLayout.TABLE))
                db.databaseDao().insertColumn(DatabaseColumnEntity(id = "colore", pageId = "umore", name = "Colore", type = ColumnType.COLOR, orderIndex = 0))
                listOf("lun" to "Lunedì", "mar" to "Martedì", "mer" to "Mercoledì").forEachIndexed { i, (id, title) ->
                    db.databaseDao().insertRow(DatabaseRowEntity(id = id, pageId = "umore", title = title, orderIndex = i))
                }
                // Solo martedì ha il suo colore, con una parola sopra.
                db.databaseDao().insertCell(DatabaseCellEntity(rowId = "mar", columnId = "colore", value = "#2E7D32|Bene"))
                db.blockDao().insert(BlockEntity(pageId = "pagina", type = BlockType.DATABASE_LINK, linkedPageId = "umore", orderIndex = 0))
            }
        }
        val factory = ViewModelFactory(context)
        compose.setContent {
            NotionLocalTheme {
                PageEditorScreen(pageId = "pagina", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {}, onNavigateToPage = {}, onNavigateToDatabase = {})
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Mercoledì").fetchSemanticsNodes().isNotEmpty() }
        assertEquals("nessun trattino nelle celle vuote", 0, compose.onAllNodesWithText("—").fetchSemanticsNodes().size)
        assertEquals(1, compose.onAllNodesWithText("Bene").fetchSemanticsNodes().size)
    }
}
