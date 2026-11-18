package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.PAGE_CONTENTS_TAG
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **L'indice a destra anche sul telefono** (18/11/2026, chiesto dall'utente: "mettila sia su PC che
 * su telefono"), **coi soli blocchi segnati** con "Flag (for Table of contents)": un tocco sulle
 * lineette apre l'elenco dal basso, un tocco su un nome porta lì e lo chiude. Il titolo grande non
 * segnato non c'è.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h900dp-mdpi")
class PhoneContentsTest {

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
    fun theFlaggedLinesAreInTheContentsOnThePhone() {
        runBlocking {
            db.withTransaction {
                db.pageDao().insert(PageEntity(id = "tedesco", title = "Tedesco"))
                fun line(id: String, order: Int, text: String, attrs: String = "{}", size: Int? = null) = BlockEntity(
                    id = id, pageId = "tedesco", type = BlockType.PARAGRAPH, orderIndex = order, attrsJson = attrs,
                    textJson = if (size == null) """[{"text":"$text"}]""" else """[{"text":"$text","size":$size}]"""
                )
                db.blockDao().insertAll(
                    listOf(
                        line("titolo", 0, "Kapitel eins", size = 24),
                        line("a", 1, "Grammatik", """{"tocFlag":true}""", size = 24),
                        line("b", 2, "Testo normale"),
                        line("c", 3, "Wortschatz", """{"tocFlag":true}""")
                    )
                )
            }
        }
        val factory = ViewModelFactory(context)
        compose.setContent {
            NotionLocalTheme {
                PageEditorScreen(pageId = "tedesco", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {}, onNavigateToPage = {}, onNavigateToDatabase = {})
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag(PAGE_CONTENTS_TAG)).fetchSemanticsNodes().isNotEmpty() }
        fun count(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().size
        val before = listOf("Grammatik", "Wortschatz", "Kapitel eins").associateWith { count(it) }
        compose.onAllNodes(hasTestTag(PAGE_CONTENTS_TAG)).onLast().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Table of contents").fetchSemanticsNodes().isNotEmpty() }
        // Nell'elenco le due righe segnate; il titolo grande non segnato no.
        assertEquals(before.getValue("Grammatik") + 1, count("Grammatik"))
        assertEquals(before.getValue("Wortschatz") + 1, count("Wortschatz"))
        assertEquals(before.getValue("Kapitel eins"), count("Kapitel eins"))
        compose.onAllNodesWithText("Wortschatz").onLast().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Table of contents").fetchSemanticsNodes().isEmpty() }
        assertFalse(compose.onAllNodesWithText("Table of contents").fetchSemanticsNodes().isNotEmpty())
    }
}
