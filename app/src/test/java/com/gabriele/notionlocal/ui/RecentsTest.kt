package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.LibraryScreen
import com.gabriele.notionlocal.ui.screen.RecentsSidebarSection
import com.gabriele.notionlocal.ui.screen.timeAgo
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
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
 * **"Recents" e la Library** (dal 13/11/2026, chiesti dall'utente come in
 * Notion): le pagine aperte compaiono nella barra laterale, la più recente
 * prima, al massimo dieci e non quelle nel cestino; "View all" apre la
 * Library, con le schede Recents e Favorites.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h900dp-mdpi")
class RecentsTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var repo: PageRepository

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        AppDatabase.resetForTests()
        context.getDatabasePath("notion_local.db").delete()
        repo = PageRepository(AppDatabase.getInstance(context))
        repo.createPage(PageEntity(id = "contenitore", title = "Contenitore"))
        // Dodici pagine dentro "Contenitore", aperte in fila: la dodicesima è la più recente.
        for (i in 1..12) {
            repo.createPage(PageEntity(id = "p$i", title = "Pagina numero $i"))
            repo.saveBlock(BlockEntity(pageId = "contenitore", type = BlockType.PAGE_LINK, linkedPageId = "p$i", orderIndex = i))
            AppSettings.recordVisit("p$i", at = 1_000L * i)
        }
        // Riaperta: torna in cima, una volta sola.
        AppSettings.recordVisit("p3", at = 50_000L)
        repo.moveToTrash("p11")
        repo.setFavorite("p1", true)
    }

    @After
    fun tearDown() = AppDatabase.resetForTests()

    private fun shown(n: Int) = compose.onAllNodesWithText("Pagina numero $n").fetchSemanticsNodes().isNotEmpty()

    @Test
    fun theLastTenOpenedInTheSidebar() {
        var viewAll = 0
        compose.setContent {
            NotionLocalTheme {
                Column {
                    RecentsSidebarSection(observePages = repo::observePages, currentPageId = null, onOpen = {}, onViewAll = { viewAll++ })
                }
            }
        }
        compose.waitUntil(10_000) { shown(3) }
        // p3 (riaperta), p12, p10…p4: dieci, senza p11 (nel cestino); p1 e p2 non ci stanno più.
        val expected = listOf(3, 12, 10, 9, 8, 7, 6, 5, 4, 2)
        expected.forEach { assertTrue("Pagina numero $it", shown(it)) }
        assertTrue("p11 è nel cestino", !shown(11))
        assertTrue("solo dieci: p1 no", !shown(1))
        val tops = expected.map { compose.onNodeWithText("Pagina numero $it").fetchSemanticsNode().boundsInRoot.top }
        assertEquals("dalla più recente", tops.sorted(), tops)
        compose.onNodeWithText("View all").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(1, viewAll)
    }

    @Test
    fun theLibraryHasRecentsAndFavorites() {
        val opened = ArrayList<String>()
        compose.setContent {
            NotionLocalTheme {
                LibraryScreen(repository = repo, onBack = {}, onOpenPage = { opened += it.id })
            }
        }
        compose.waitUntil(10_000) { shown(3) }
        compose.onNodeWithText("Recents").assertIsDisplayed()
        // Dove sta la pagina: dentro "Contenitore".
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Contenitore", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Pagina numero 12").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(listOf("p12"), opened)
        compose.onNodeWithText("Favorites").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitUntil(10_000) { !shown(12) }
        assertTrue("la preferita", shown(1))
    }

    @Test
    fun howLongAgo() {
        val now = 1_000_000_000_000L
        assertEquals("Just now", timeAgo(now - 20_000, now))
        assertEquals("5m ago", timeAgo(now - 5 * 60_000, now))
        assertEquals("6h ago", timeAgo(now - 6 * 3_600_000L, now))
        assertEquals("1d ago", timeAgo(now - 30 * 3_600_000L, now))
        assertEquals("2w ago", timeAgo(now - 15 * 86_400_000L, now))
    }
}
