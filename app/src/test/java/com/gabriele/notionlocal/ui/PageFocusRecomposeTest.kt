package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.InternalComposeTracingApi
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.i18n.EditorStrings
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
 * **La tastiera non fa ridisegnare i database della pagina** (27/10/2026,
 * l'utente: "nella home continua a laggare la tastiera quando la apro e
 * quando la chiudo"). Aprendo e chiudendo la tastiera la pagina si rifà un
 * paio di volte — il cursore che entra e che esce, la tastiera che c'è o non
 * c'è — e i database dentro si rifacevano con lei: nella home sono dieci,
 * tutti tenuti pronti anche fuori dallo schermo, e sul telefono costava fino
 * a 69 ms per fotogramma su 8.
 *
 * Si conta, coi segni che Compose lascia per i profilatori
 * (`Composer.setTracer`), quante volte riparte il resto: quando il cursore
 * entra nel titolo (la pagina si rifà, come quando la tastiera compare) e
 * mentre la tastiera sale e scende, con le sue altezze mandate una dopo
 * l'altra come fa il telefono a ogni fotogramma (dal 27/10/2026, chiesto
 * dall'utente: «Hai fatto sì che non riaccada più in futuro?»).
 */
@OptIn(InternalComposeTracingApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-mdpi")
class PageFocusRecomposeTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Quante volte è ripartita ogni funzione (per nome, come nella traccia) da quando si conta. */
    private val started = mutableListOf<String>()
    private var counting = false

    private fun count(name: String) = started.count { name in it }

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        // Un test di prima può aver lasciato aperto il suo database, che qui
        // non vale più: nella suite intera, senza questo, la prova falliva
        // (o si fermava) già scrivendo la pagina.
        AppDatabase.resetForTests()
        context.getDatabasePath("notion_local.db").delete()
        val db = AppDatabase.getInstance(context)
        db.withTransaction {
            db.pageDao().insert(PageEntity(id = "pagina", title = "Pagina di prova"))
            db.pageDao().insert(PageEntity(id = "spesa", parentId = "pagina", title = "Spesa", isDatabase = true, isSimpleDatabase = true))
            repeat(5) { i ->
                db.databaseDao().insertRow(DatabaseRowEntity(id = "r$i", pageId = "spesa", title = "Cosa $i", orderIndex = i))
            }
            db.blockDao().insert(BlockEntity(pageId = "pagina", type = BlockType.DATABASE_LINK, linkedPageId = "spesa", orderIndex = 0))
            db.blockDao().insert(BlockEntity(pageId = "pagina", textJson = """[{"text":"Riga di prova"}]""", orderIndex = 1))
        }
        Composer.setTracer(object : CompositionTracer {
            override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
                if (counting) started += info
            }
            override fun traceEventEnd() = Unit
            override fun isTraceInProgress() = true
        })
    }

    @After
    fun tearDown() {
        counting = false
        Composer.setTracer(object : CompositionTracer {
            override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) = Unit
            override fun traceEventEnd() = Unit
            override fun isTraceInProgress() = false
        })
        AppDatabase.resetForTests()
    }

    /**
     * La tastiera alta [heightPx], come la manda il telefono a ogni
     * fotogramma della sua corsa: un'altezza nuova dopo l'altra.
     */
    private fun keyboard(heightPx: Int, visible: Boolean = heightPx > 0) {
        compose.runOnIdle {
            // La vista di Compose vera, quella che ascolta la tastiera, dentro il `ComposeView`.
            val composeView = (compose.activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as ViewGroup).getChildAt(0)
            val insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, heightPx))
                .setVisible(WindowInsetsCompat.Type.ime(), visible)
                .build()
            ViewCompat.dispatchApplyWindowInsets(composeView, insets)
        }
        compose.waitForIdle()
    }

    private fun showPage() {
        val factory = ViewModelFactory(context)
        compose.setContent {
            NotionLocalTheme {
                PageEditorScreen(
                    pageId = "pagina",
                    factory = factory,
                    onBack = {},
                    onOpenSearch = {},
                    onOpenUpdates = {},
                    onNavigateToPage = {},
                    onNavigateToDatabase = {}
                )
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Cosa 0").fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }

    /** Le funzioni dell'app ripartite da quando si conta, coi soli nomi. */
    private fun ours() = started.filter { it.startsWith("com.gabriele") }.map { it.substringBefore(" (") }

    @Test
    fun theKeyboardMovingLeavesThePageAlone() {
        showPage()
        // Finché nessuno gliela manda, per Compose la tastiera è "visibile":
        // prima la si chiude, come sul telefono all'apertura della pagina.
        keyboard(0)
        compose.onNode(hasSetTextAction() and hasText("Riga di prova", substring = true))
            .performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()

        // La tastiera comincia a salire: compare la barra, e la pagina si
        // rifà per lei — lo si controlla, se no le altezze qui sotto non
        // sarebbero arrivate a nessuno.
        counting = true
        keyboard(40)
        counting = false
        assertTrue("la pagina non ha visto la tastiera", count("PageEditorContent") > 0)
        // "Block options" c'è anche sul database (i sei puntini, in alto): la
        // barra è quello più in basso.
        fun barTop() = compose.onAllNodesWithContentDescription(EditorStrings.blockOptions)
            .fetchSemanticsNodes().maxOf { it.boundsInRoot.top }
        val barLow = barTop()

        // La corsa, su e giù: niente si rifà, si sposta e basta.
        started.clear()
        counting = true
        for (h in 80..400 step 40) keyboard(h)
        val barHigh = barTop()
        for (h in 360 downTo 40 step 40) keyboard(h)
        counting = false
        assertTrue("la barra non è salita con la tastiera", barHigh < barLow)
        // Dell'app si rifanno solo due cose piccole, e nient'altro (né la
        // pagina, né i database, né cose nuove che un giorno leggessero la
        // tastiera mentre si costruisce lo schermo): il cursore che si spegne
        // a tastiera chiusa, che guarda dove la tastiera sta andando — qui
        // cambia a ogni passo perché le altezze arrivano senza la sua
        // animazione, sul telefono una volta sola — e la barretta di
        // scorrimento, perché la pagina scorre per tenere la riga sopra la
        // tastiera.
        val allowed = setOf(
            "com.gabriele.notionlocal.ui.screen.ClearFocusWhenKeyboardCloses",
            "com.gabriele.notionlocal.ui.screen.PageScrollIndicator"
        )
        // Anche le parti della barretta (dal 24/11/2026 sul telefono ha la presa per il dito, in una funzione sua).
        assertEquals(emptyMap<String, Int>(), ours().filter { name -> allowed.none { name == it || name.startsWith("$it.") } }.groupingBy { it }.eachCount())
        // E la lista non riparte con tutte le sue voci.
        assertEquals(0, count("LazyLayoutItemContentFactory"))
        keyboard(0)
    }

    @Test
    fun theCursorInAndOutOfTheTitleLeavesTheDatabaseAlone() {
        val factory = ViewModelFactory(context)
        lateinit var focusManager: FocusManager
        compose.setContent {
            focusManager = LocalFocusManager.current
            NotionLocalTheme {
                PageEditorScreen(
                    pageId = "pagina",
                    factory = factory,
                    onBack = {},
                    onOpenSearch = {},
                    onOpenUpdates = {},
                    onNavigateToPage = {},
                    onNavigateToDatabase = {}
                )
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Cosa 0").fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()

        counting = true
        compose.onNode(hasSetTextAction() and hasText("Pagina di prova")).performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()
        compose.runOnIdle { focusManager.clearFocus() }
        compose.waitForIdle()
        counting = false

        // La pagina si è rifatta davvero, se no la prova non direbbe niente...
        assertTrue("la pagina non si è rifatta", count("PageEditorContent") > 0)
        // ...ma il database dentro no, né la sua voce della lista.
        assertEquals(0, count("DatabaseViewBody"))
        assertEquals(0, count("IslandItem"))
        // E la lista non è ripartita con tutte le sue voci.
        assertEquals(0, count("LazyLayoutItemContentFactory"))
    }
}
