package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import com.gabriele.notionlocal.data.entity.GroupOrder
import com.gabriele.notionlocal.data.entity.GroupRuleSpec
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.boardGroupRules
import com.gabriele.notionlocal.data.entity.encodeGroupRules
import com.gabriele.notionlocal.data.entity.tableGroupRules
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.GROUP_RULE_ROW_TAG
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
 * **"Group" con le regole, quante se ne vogliono** (28/10/2026, chiesto
 * dall'utente: "la stessa cosa con il Group"): dentro "Group" l'elenco dei
 * raggruppamenti e "Add a group", e "Sub groups" non c'è più; nella tabella i
 * gruppi dentro i gruppi, a ogni livello; nella bacheca le colonne e le fasce.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h900dp-mdpi")
class GroupRulesScreenTest {

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

    private fun withDatabase(layout: DatabaseLayout = DatabaseLayout.TABLE, table: List<GroupRuleSpec> = emptyList(), board: List<GroupRuleSpec> = emptyList()) =
        runBlocking {
            db.withTransaction {
                db.pageDao().insert(PageEntity(id = "pagina", title = "Pagina di prova"))
                db.blockDao().insert(BlockEntity(pageId = "pagina", textJson = """[{"text":"Sopra"}]""", orderIndex = 0))
                db.pageDao().insert(
                    PageEntity(
                        id = "spesa", parentId = "pagina", title = "Spesa", isDatabase = true, databaseLayout = layout,
                        tableGroupRulesJson = encodeGroupRules(table), boardGroupRulesJson = encodeGroupRules(board)
                    )
                )
                db.databaseDao().insertColumn(
                    DatabaseColumnEntity(
                        id = "shop", pageId = "spesa", name = "Supermercato", type = ColumnType.SELECT, orderIndex = 0,
                        optionsJson = """[{"label":"IPERPOLI"},{"label":"Lidl"}]"""
                    )
                )
                db.databaseDao().insertColumn(
                    DatabaseColumnEntity(
                        id = "kind", pageId = "spesa", name = "Tipo", type = ColumnType.SELECT, orderIndex = 1,
                        optionsJson = """[{"label":"Cibo"},{"label":"Casa"}]"""
                    )
                )
                db.databaseDao().insertColumn(DatabaseColumnEntity(id = "bought", pageId = "spesa", name = "Preso", type = ColumnType.CHECKBOX, orderIndex = 2))
                listOf("pasta" to "Pasta", "sale" to "Sale", "shampoo" to "Shampoo").forEachIndexed { i, (id, title) ->
                    db.databaseDao().insertRow(DatabaseRowEntity(id = id, pageId = "spesa", title = title, orderIndex = i))
                }
                mapOf(
                    ("pasta" to "shop") to "IPERPOLI", ("pasta" to "kind") to "Cibo",
                    ("sale" to "shop") to "IPERPOLI", ("sale" to "kind") to "Cibo", ("sale" to "bought") to "true",
                    ("shampoo" to "shop") to "Lidl", ("shampoo" to "kind") to "Casa"
                ).forEach { (key, value) -> db.databaseDao().insertCell(DatabaseCellEntity(rowId = key.first, columnId = key.second, value = value)) }
                db.blockDao().insert(BlockEntity(pageId = "pagina", type = BlockType.DATABASE_LINK, linkedPageId = "spesa", orderIndex = 1))
            }
        }

    private fun open() {
        val factory = ViewModelFactory(context)
        compose.setContent {
            NotionLocalTheme {
                PageEditorScreen(pageId = "pagina", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {}, onNavigateToPage = {}, onNavigateToDatabase = {})
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Pasta").fetchSemanticsNodes().isNotEmpty() }
    }

    /** Aspetta [condition] lasciando andare ogni volta il thread principale, dove partono i salvataggi. */
    private fun waitFor(what: String, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < end) {
            compose.waitForIdle()
            if (condition()) return
            Thread.sleep(20)
        }
        throw AssertionError("Non è successo: $what")
    }

    private fun shown(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    private fun count(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().size
    private fun saved() = runBlocking { db.pageDao().getById("spesa")!! }

    /** Tocca l'ultimo nodo con questo testo: quello della finestra, sopra la tabella. */
    private fun tapInSheet(text: String) {
        compose.onAllNodesWithText(text).onLast().performScrollTo().performClick()
        compose.waitForIdle()
    }

    private fun openSettings() {
        compose.onAllNodesWithContentDescription("Database settings").onFirst().performClick()
        compose.waitForIdle()
    }

    /** I nomi delle regole nell'elenco, dall'alto. */
    private fun ruleRows() = compose.onAllNodes(hasTestTag(GROUP_RULE_ROW_TAG)).fetchSemanticsNodes().map { node ->
        node.config.toString()
    }

    @Test
    fun groupHasTheRulesAndAddAGroupAndNoSubGroups() {
        withDatabase()
        open()
        openSettings()
        assertTrue(shown("Group"))
        assertFalse(shown("Sub groups"))
        assertFalse(shown("Hide empty sub groups"))
        tapInSheet("Group")
        assertTrue(shown("Add a group"))
        assertTrue(shown("No groups yet"))

        // Tre livelli, uno dopo l'altro.
        listOf("Supermercato", "Tipo", "Preso").forEach { name ->
            tapInSheet("Add a group")
            tapInSheet(name)
        }
        waitFor("le tre regole") { saved().tableGroupRules().map { it.column } == listOf("shop", "kind", "bought") }
        val rows = ruleRows()
        assertEquals(3, rows.size)
        assertTrue(rows[0].contains("Supermercato") && rows[0].contains("1"))
        assertTrue(rows[2].contains("Preso") && rows[2].contains("3"))
        // Tutte le proprietà che fanno gruppi sono già usate: "Add a group" non ha più niente da offrire.
        assertTrue(shown("Every property that can make groups is already in a group."))
        assertFalse(shown("Sub groups"))
    }

    @Test
    fun theTableShowsGroupsInsideGroupsAtEveryLevel() {
        withDatabase(table = listOf(GroupRuleSpec("shop"), GroupRuleSpec("kind"), GroupRuleSpec("bought")))
        open()
        // IPERPOLI › Cibo › Checked (Sale) e Unchecked (Pasta); Lidl › Casa › Unchecked (Shampoo).
        waitFor("i gruppi") { shown("IPERPOLI") && shown("Lidl") }
        // Le intestazioni del terzo livello: nelle celle la casella è un disegno, non una scritta.
        assertEquals(1, count("Checked"))
        assertEquals(2, count("Unchecked"))
        // Nel gruppo "Lidl › Casa" niente è preso: il suo "Checked" vuoto non c'è.
        assertTrue(shown("Shampoo"))
    }

    /**
     * **I gruppi anche nell'elenco** (18/11/2026, chiesto dall'utente: "Implementa il grouping anche
     * nelle viste List dei database"): gli stessi di "Group" della tabella, che c'è anche qui; un
     * tocco sull'intestazione chiude il gruppo.
     */
    @Test
    fun theListShowsTheGroupsToo() {
        withDatabase(layout = DatabaseLayout.LIST, table = listOf(GroupRuleSpec("shop"), GroupRuleSpec("kind")))
        open()
        // Nelle righe i primi due valori (IPERPOLI e Cibo per Pasta e Sale); in più le intestazioni.
        waitFor("i gruppi") { count("IPERPOLI") == 3 && count("Lidl") == 2 }
        assertEquals("Cibo: le due righe e il sottogruppo", 3, count("Cibo"))
        assertEquals("un \"New page\" per gruppo, e quello in fondo", 3, count("New page"))
        // Chiuso IPERPOLI, le sue righe non si vedono più; quella di Lidl sì.
        compose.onAllNodesWithText("IPERPOLI").onFirst().performClick()
        waitFor("chiuso") { !shown("Pasta") && !shown("Sale") }
        assertTrue(shown("Shampoo"))
        assertEquals(listOf("IPERPOLI"), saved().tableGroupRules().first().collapsed)
        openSettings()
        assertTrue(shown("Group"))
    }

    @Test
    fun theRulePageChangesHideEmptyThePropertyAndRemovesIt() {
        withDatabase(table = listOf(GroupRuleSpec("shop"), GroupRuleSpec("kind")))
        open()
        openSettings()
        tapInSheet("Group")
        tapInSheet("Tipo")
        // La pagina della regola: le proprietà, "Hide empty groups", "Remove group".
        assertTrue(shown("Remove group"))
        tapInSheet("Hide empty groups")
        waitFor("salvato") { saved().tableGroupRules().find { it.column == "kind" }?.hideEmpty == false }
        // Scegliendo la proprietà dell'altra regola, le due si scambiano.
        tapInSheet("Supermercato")
        waitFor("scambiate") { saved().tableGroupRules().map { it.column } == listOf("kind", "shop") }
        tapInSheet("Remove group")
        waitFor("tolta") { saved().tableGroupRules().map { it.column } == listOf("kind") }
        // Si torna all'elenco.
        assertTrue(shown("Add a group"))
    }

    @Test
    fun theBoardHasColumnsThenBands() {
        withDatabase(layout = DatabaseLayout.BOARD, board = listOf(GroupRuleSpec("shop", hideEmpty = false)))
        open()
        openSettings()
        assertTrue(shown("Group by"))
        assertFalse(shown("Sub groups"))
        tapInSheet("Group by")
        assertTrue(shown("Columns"))
        tapInSheet("Add a group")
        tapInSheet("Tipo")
        waitFor("la fascia") { saved().boardGroupRules().map { it.column } == listOf("shop", "kind") }
        assertTrue(shown("Bands"))
        // Le fasce di suo nascondono quelle vuote; le colonne no.
        assertEquals(listOf(false, true), saved().boardGroupRules().map { it.hideEmpty })
    }

    @Test
    fun sortGroupsPutsUncheckedFirst() {
        // Come "Done" nel Main calendar dell'utente: Checked di suo sopra, Unchecked sotto.
        withDatabase(table = listOf(GroupRuleSpec("bought")))
        open()
        waitFor("i gruppi") { shown("Checked") && shown("Unchecked") }
        fun top(text: String) = compose.onAllNodesWithText(text).onFirst().fetchSemanticsNode().boundsInRoot.top
        assertTrue(top("Checked") < top("Unchecked"))
        openSettings()
        tapInSheet("Group")
        tapInSheet("Preso")
        // La pagina della regola: "Sort groups", a mano coi gruppi da trascinare.
        assertTrue(shown("Sort groups"))
        assertTrue(shown("Manual"))
        assertEquals(2, compose.onAllNodesWithContentDescription("Drag to reorder").fetchSemanticsNodes().size)
        tapInSheet("Descending")
        waitFor("salvato") { saved().tableGroupRules().single().order == GroupOrder.DESCENDING }
        // Per nome al contrario non si trascina: le maniglie se ne vanno.
        assertEquals(0, compose.onAllNodesWithContentDescription("Drag to reorder").fetchSemanticsNodes().size)
        // Dietro la finestra la tabella è già cambiata.
        waitFor("Unchecked sopra") { top("Unchecked") < top("Checked") }
    }

    @Test
    fun sortGroupsIsAlsoInTheGroupPage() {
        // Dal 03/11/2026: "Sort groups" si vede già nella pagina "Group", senza
        // dover toccare il nome della proprietà.
        withDatabase(table = listOf(GroupRuleSpec("bought")))
        open()
        waitFor("i gruppi") { shown("Checked") && shown("Unchecked") }
        openSettings()
        tapInSheet("Group")
        assertTrue(shown("Sort groups"))
        assertTrue(shown("Manual"))
        assertEquals(2, compose.onAllNodesWithContentDescription("Drag to reorder").fetchSemanticsNodes().size)
        tapInSheet("Descending")
        waitFor("salvato") { saved().tableGroupRules().single().order == GroupOrder.DESCENDING }
    }

    @Test
    fun withTwoRulesEachSortSaysItsProperty() {
        withDatabase(table = listOf(GroupRuleSpec("kind"), GroupRuleSpec("bought")))
        open()
        openSettings()
        tapInSheet("Group")
        assertTrue(shown("Sort groups · Tipo"))
        assertTrue(shown("Sort groups · Preso"))
        assertFalse(shown("Sort groups"))
    }
}
