package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.BACK_TO_TOP_TAG
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **"Back" quando si è scesi molto** (27/10/2026, chiesto dall'utente): in
 * una pagina lunga, scendendo col dito compare in basso "Back" con la
 * freccia in su; salendo a mano se ne va; toccandolo si torna in cima.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-mdpi")
class BackToTopTest {

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
            db.pageDao().insert(PageEntity(id = "p", title = "Pagina lunga"))
            repeat(150) { i ->
                db.blockDao().insert(BlockEntity(pageId = "p", textJson = """[{"text":"Riga $i"}]""", orderIndex = i))
            }
        }
    }

    @After
    fun tearDown() = AppDatabase.resetForTests()

    private fun backShown() = compose.onAllNodesWithTag(BACK_TO_TOP_TAG).fetchSemanticsNodes().isNotEmpty()

    private fun drag(up: Boolean) {
        compose.onRoot().performTouchInput { if (up) swipeUp() else swipeDown() }
        compose.waitForIdle()
    }

    private fun openPage() {
        val factory = ViewModelFactory(context)
        compose.setContent {
            NotionLocalTheme {
                PageEditorScreen(pageId = "p", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {}, onNavigateToPage = {}, onNavigateToDatabase = {})
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Riga 0", substring = true).fetchSemanticsNodes().isNotEmpty() }
    }

    /** Un saltino in giù, lento (senza slancio): dice solo il verso. */
    private fun nudgeDown() {
        compose.onRoot().performTouchInput { swipe(center, center.copy(y = center.y - 120f), durationMillis = 400) }
        compose.waitForIdle()
    }

    /**
     * **Solo oltre i due terzi, e via dopo due secondi fermi** (13/11/2026,
     * chiesto dall'utente).
     */
    /** Trascina di [dy] px e si ferma prima di alzare il dito: la pagina si sposta di tanto, senza slancio. */
    private fun dragWithoutFling(dy: Float) {
        compose.onRoot().performTouchInput {
            down(Offset(centerX, if (dy < 0) bottom - 40f else top + 40f))
            repeat(20) { moveBy(Offset(0f, dy / 20), delayMillis = 16) }
            advanceEventTime(300)
            up()
        }
        compose.waitForIdle()
    }

    /** Dove sta la pagina, come la dice la lista: (valore, massimo); uguali in fondo. */
    private fun position(list: SemanticsNodeInteraction): Pair<Float, Float> =
        list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].let { it.value() to it.maxValue() }

    @Test
    fun onlyPastTwoThirdsAndGoneAfterTwoSeconds() = runBlocking {
        // 600 righe: a meno di due schermate giù si è ben lontani dai due terzi.
        val db = AppDatabase.getInstance(context)
        db.withTransaction {
            for (i in 150 until 600) db.blockDao().insert(BlockEntity(pageId = "p", textJson = """[{"text":"Riga $i"}]""", orderIndex = i))
        }
        openPage()
        val id = compose.onNode(hasScrollToIndexAction() and hasAnyDescendant(hasText("Riga 0", substring = true))).fetchSemanticsNode().id
        val list = compose.onNode(SemanticsMatcher("la pagina") { it.id == id })

        // Quasi due schermate giù (più di una e mezza), ma sotto i due terzi: scendendo non c'è.
        repeat(2) { dragWithoutFling(-700f) }
        compose.mainClock.advanceTimeBy(500)
        assert(!backShown()) { "prima dei due terzi non deve vedersi" }

        // In fondo sì.
        for (i in 0 until 60) {
            val (value, max) = position(list)
            if (value >= max) break
            drag(up = true)
        }
        compose.waitUntil(5_000) { backShown() }

        // Fermi per due secondi, se ne va; riscendendo torna.
        compose.mainClock.advanceTimeBy(2_500)
        compose.waitForIdle()
        assert(!backShown()) { "dopo due secondi fermi deve sparire" }
        dragWithoutFling(200f)
        nudgeDown()
        compose.waitUntil(5_000) { backShown() }
    }

    @Test
    fun backAppearsGoingDownAndNotGoingUp() {
        openPage()
        // In cima non c'è.
        assert(!backShown())

        // Scendendo per qualche schermata compare.
        repeat(4) { drag(up = true) }
        compose.waitUntil(5_000) { backShown() }

        // Salendo a mano se ne va, anche se si è ancora lontani dalla cima.
        compose.onRoot().performTouchInput {
            swipe(center, center.copy(y = center.y + 150f), durationMillis = 300)
        }
        compose.waitUntil(5_000) { !backShown() }

        // Riscendendo torna; toccandolo si va in cima e se ne va.
        drag(up = true)
        compose.waitUntil(5_000) { backShown() }
        compose.onNodeWithTag(BACK_TO_TOP_TAG).performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Riga 0", substring = true).fetchSemanticsNodes().isNotEmpty() && !backShown()
        }
    }
}
