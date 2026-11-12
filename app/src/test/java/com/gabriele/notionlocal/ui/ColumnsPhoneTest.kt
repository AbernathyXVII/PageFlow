package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.screen.SlashAction
import com.gabriele.notionlocal.ui.screen.SlashEntry
import com.gabriele.notionlocal.ui.screen.slashEntries
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import kotlin.math.abs

/**
 * **Le colonne sul telefono** (dal 12/11/2026, chiesto dall'utente: "sul
 * telefono le colonne non esistono, come sulla versione mobile di Notion"):
 * il contenuto si legge dall'alto al basso, prima tutta la prima colonna,
 * poi la seconda, e dopo il resto della pagina. E i menu non le creano.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-mdpi")
class ColumnsPhoneTest {

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
            db.pageDao().insert(PageEntity(id = "p", title = "Prova"))
            db.blockDao().insert(BlockEntity(id = "prima", pageId = "p", textJson = """[{"text":"PRIMA"}]""", orderIndex = 0))
            db.blockDao().insert(BlockEntity(id = "gruppo", pageId = "p", type = BlockType.COLUMN_LIST, orderIndex = 1))
            db.blockDao().insert(BlockEntity(id = "c1", pageId = "p", parentBlockId = "gruppo", type = BlockType.COLUMN, orderIndex = 0))
            db.blockDao().insert(BlockEntity(id = "c2", pageId = "p", parentBlockId = "gruppo", type = BlockType.COLUMN, orderIndex = 1))
            db.blockDao().insert(BlockEntity(pageId = "p", parentBlockId = "c1", textJson = """[{"text":"SINISTRA"}]""", orderIndex = 0))
            db.blockDao().insert(BlockEntity(pageId = "p", parentBlockId = "c1", textJson = """[{"text":"ANCORA"}]""", orderIndex = 1))
            db.blockDao().insert(BlockEntity(pageId = "p", parentBlockId = "c2", textJson = """[{"text":"DESTRA"}]""", orderIndex = 0))
            db.blockDao().insert(BlockEntity(id = "dopo", pageId = "p", textJson = """[{"text":"DOPO"}]""", orderIndex = 2))
        }
    }

    @After
    fun tearDown() = AppDatabase.resetForTests()

    @Test
    fun onThePhoneTheColumnsAreOneBelowTheOther() {
        val factory = ViewModelFactory(context)
        compose.setContent {
            NotionLocalTheme {
                PageEditorScreen(pageId = "p", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {}, onNavigateToPage = {}, onNavigateToDatabase = {})
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("DESTRA", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
        fun box(text: String) = compose.onNode(hasSetTextAction() and hasText(text, substring = true)).getBoundsInRoot()
        val order = listOf("PRIMA", "SINISTRA", "ANCORA", "DESTRA", "DOPO").map { box(it) }
        order.zipWithNext().forEach { (above, below) -> assertTrue("$above sopra $below", above.top < below.top) }
        // Tutte allo stesso margine, come le altre righe della pagina.
        val sinistra = order[1].left
        val destra = order[3].left
        assertTrue("SINISTRA a $sinistra, DESTRA a $destra", abs((sinistra - destra).value) < 1f)
    }

    /** Le righe della colonna [columnId] nel database, dall'alto. */
    private fun linesOf(columnId: String) = runBlocking {
        AppDatabase.getInstance(context).blockDao().getBlocksForPageOnce("p")
            .filter { it.parentBlockId == columnId }.sortedBy { it.orderIndex }.map { it.textJson }
    }

    private fun settle() {
        repeat(10) {
            compose.mainClock.advanceTimeBy(200)
            Thread.sleep(50)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
            compose.waitForIdle()
        }
    }

    /**
     * **Scrivere e andare a capo dentro una colonna** (trovato dall'utente il
     * 12/11/2026: "se vado a capo premendo Invio mi duplica quello che ho
     * scritto rimanendo nello stesso blocco"): due Invio in fondo a
     * "SINISTRA" fanno due righe vuote sotto di lei, e nessun doppione.
     */
    @Test
    fun twoEntersInAColumnMakeTwoEmptyLines() {
        val factory = ViewModelFactory(context)
        compose.setContent {
            NotionLocalTheme {
                PageEditorScreen(pageId = "p", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {}, onNavigateToPage = {}, onNavigateToDatabase = {})
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("SINISTRA", substring = true).fetchSemanticsNodes().isNotEmpty() }
        val field = compose.onNode(hasSetTextAction() and hasText("SINISTRA", substring = true))
        field.performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()
        field.performTextInputSelection(TextRange(1 + "SINISTRA".length))
        field.performTextInput("\n")
        settle()
        compose.onNode(isFocused()).performTextInput("\n")
        settle()
        assertEquals(
            listOf("""[{"text":"SINISTRA"}]""", """[{"text":""}]""", """[{"text":""}]""", """[{"text":"ANCORA"}]"""),
            linesOf("c1")
        )
    }

    @Test
    fun onThePhoneTheMenusDontCreateColumns() {
        var entries: List<SlashEntry> = emptyList()
        compose.setContent { entries = slashEntries() }
        compose.waitForIdle()
        assertTrue(entries.isNotEmpty())
        assertTrue(entries.none { it.action is SlashAction.Columns })
    }
}
