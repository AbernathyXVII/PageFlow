package com.gabriele.notionlocal.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.GroupLevel
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.BoardLayout
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.DatabaseTableState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **La bacheca coi sottogruppi** (14/10/2026, chiesto dall'utente): fasce
 * orizzontali, una per valore della seconda proprietà, ognuna con tutte le
 * colonne; e le colonne fatte anche da una casella, non solo da un tag.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w1200dp-h2000dp-mdpi")
class BoardLayoutTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val shop = DatabaseColumnEntity(
        id = "shop", pageId = "food", name = "Supermercato", type = ColumnType.SELECT, orderIndex = 0,
        optionsJson = """[{"label":"IPERPOLI","color":"#E03E3E"},{"label":"Lidl","color":"#3A6590"}]"""
    )
    private val kind = DatabaseColumnEntity(
        id = "kind", pageId = "food", name = "Tipologia", type = ColumnType.SELECT, orderIndex = 1,
        optionsJson = """[{"label":"Cibo","color":"#3F6F55"},{"label":"Casa","color":"#9065B0"},{"label":"Altro","color":"#787774"}]"""
    )
    private val bought = DatabaseColumnEntity(id = "bought", pageId = "food", name = "Preso", type = ColumnType.CHECKBOX, orderIndex = 2)

    private val pasta = DatabaseRowEntity(id = "pasta", pageId = "food", title = "Pasta", orderIndex = 0)
    private val shampoo = DatabaseRowEntity(id = "shampoo", pageId = "food", title = "Shampoo", orderIndex = 1)
    private val salt = DatabaseRowEntity(id = "salt", pageId = "food", title = "Sale", orderIndex = 2)

    private val state = DatabaseTableState(
        columns = listOf(shop, kind, bought),
        rows = listOf(pasta, shampoo, salt),
        cellValues = mapOf(
            ("pasta" to "shop") to "IPERPOLI", ("pasta" to "kind") to "Cibo",
            ("shampoo" to "shop") to "Lidl", ("shampoo" to "kind") to "Casa",
            ("salt" to "shop") to "IPERPOLI", ("salt" to "kind") to "Cibo", ("salt" to "bought") to "true"
        )
    )

    private var addedInGroup: String? = null
    private var addedInSubGroup: Pair<String, String>? = null
    private var addedIn: List<String>? = null

    @Before
    fun setUp() {
        AppSettings.init(ApplicationProvider.getApplicationContext())
    }

    private fun show(
        group: DatabaseColumnEntity,
        sub: DatabaseColumnEntity? = null,
        hideEmptySubGroups: Boolean = true,
        hideEmptyGroups: Boolean = false,
        hideNew: Boolean = false,
        colorCards: Boolean = false,
        /** Le fasce dentro le fasce (dal 28/10/2026). */
        more: List<GroupLevel> = emptyList()
    ) {
        // Dal 28/10/2026 la bacheca riceve i livelli: le colonne, poi le fasce.
        val levels = listOf(GroupLevel(group, hideEmptyGroups)) +
            listOfNotNull(sub?.let { GroupLevel(it, hideEmptySubGroups) }) + more
        compose.setContent {
            // Le fasce chiuse, come le tiene la vista dal 11/11/2026 (`GroupRuleSpec.collapsed`).
            var shut by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyMap<String, Set<String>>()) }
            NotionLocalTheme {
                BoardLayout(
                    state = state,
                    levels = levels.map { it.copy(collapsed = shut[it.column.id].orEmpty()) },
                    onCollapseGroup = { column, key, closed ->
                        val keys = shut[column].orEmpty()
                        shut = shut + (column to if (closed) keys + key else keys - key)
                    },
                    colorCards = colorCards,
                    hideNew = hideNew,
                    newRowTick = 0,
                    onOpenRow = {},
                    onRowLongPress = {},
                    onAddRowInGroups = { keys ->
                        addedIn = keys
                        if (keys.size == 1) addedInGroup = keys[0]
                        if (keys.size == 2) addedInSubGroup = keys[0] to keys[1]
                    },
                    onAddGroupProperty = {}
                )
            }
        }
        compose.waitForIdle()
    }

    private fun bounds(text: String) = compose.onNodeWithText(text).getBoundsInRoot()

    /** La scheda `card` sta nella colonna che ha in cima `header` (le colonne sono larghe 260dp). */
    private fun assertInColumn(card: String, header: String) {
        val left = bounds(header).left
        val cardLeft = bounds(card).left
        assertTrue("$card non è sotto $header", cardLeft >= left && cardLeft < left + 240.dp)
    }

    @Test
    fun eachCardSitsInItsColumnInsideItsSubGroup() {
        show(shop, kind)
        // Le fasce coi loro nomi, in ordine; "Altro" è vuota e si nasconde.
        assertTrue(bounds("Cibo").top < bounds("Casa").top)
        assertEquals(0, compose.onAllNodesWithText("Altro").fetchSemanticsNodes().size)
        // Pasta sotto IPERPOLI nella fascia "Cibo", Shampoo sotto Lidl in "Casa".
        assertInColumn("Pasta", "IPERPOLI")
        assertInColumn("Shampoo", "Lidl")
        assertTrue(bounds("Cibo").top < bounds("Pasta").top && bounds("Pasta").top < bounds("Casa").top)
        assertTrue(bounds("Casa").top < bounds("Shampoo").top)
        // Il valore delle colonne e quello delle fasce non si ripetono sulle schede.
        assertEquals(1, compose.onAllNodesWithText("IPERPOLI").fetchSemanticsNodes().size)
        assertEquals(1, compose.onAllNodesWithText("Cibo").fetchSemanticsNodes().size)
    }

    @Test
    fun emptySubGroupsCanBeShown() {
        show(shop, kind, hideEmptySubGroups = false)
        compose.onNodeWithText("Altro").assertIsDisplayed()
    }

    @Test
    fun tappingASubGroupClosesIt() {
        show(shop, kind)
        compose.onNodeWithText("Cibo").performClick()
        compose.waitForIdle()
        assertEquals(0, compose.onAllNodesWithText("Pasta").fetchSemanticsNodes().size)
        compose.onNodeWithText("Shampoo").assertIsDisplayed()
        compose.onNodeWithText("Cibo").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Pasta").assertIsDisplayed()
    }

    @Test
    fun newInsideASubGroupGetsBothValues() {
        show(shop, kind)
        // In ogni fascia un "New" per colonna: IPERPOLI, Lidl e quella senza
        // valore. Il quarto è il primo della fascia "Casa".
        compose.onAllNodesWithText("New")[3].performClick()
        compose.waitForIdle()
        assertEquals("IPERPOLI" to "Casa", addedInSubGroup)
    }

    @Test
    fun bandsInsideBands() {
        // Dal 28/10/2026 le fasce sono quante se ne vogliono: dentro "Cibo" e
        // "Casa", una fascia per "Preso". In "Casa" niente è preso: quella vuota
        // si nasconde.
        show(shop, kind, more = listOf(GroupLevel(bought)))
        assertEquals(1, compose.onAllNodesWithText("Checked").fetchSemanticsNodes().size)
        assertEquals(2, compose.onAllNodesWithText("Unchecked").fetchSemanticsNodes().size)
        assertTrue(bounds("Cibo").top < bounds("Sale").top)
        assertTrue(bounds("Sale").top < bounds("Pasta").top)
        assertTrue(bounds("Pasta").top < bounds("Casa").top)
        assertTrue(bounds("Casa").top < bounds("Shampoo").top)
        assertInColumn("Sale", "IPERPOLI")
        assertInColumn("Shampoo", "Lidl")
        // Le fasce del livello dopo stanno un po' più a destra: si misura la
        // scritta, non l'intestazione intera (larga quanto la colonna).
        fun textLeft(text: String) = compose.onAllNodesWithText(text, useUnmergedTree = true).onFirst().getBoundsInRoot().left
        assertTrue(textLeft("Checked") > textLeft("Cibo"))
        // "New" nella prima fascia della prima: la colonna e le due fasce.
        compose.onAllNodesWithText("New")[0].performClick()
        compose.waitForIdle()
        assertEquals(listOf("IPERPOLI", "Cibo", "true"), addedIn)
        // Chiudendo "Cibo" se ne vanno anche le sue fasce.
        compose.onNodeWithText("Cibo").performClick()
        compose.waitForIdle()
        assertEquals(0, compose.onAllNodesWithText("Checked").fetchSemanticsNodes().size)
        assertEquals(1, compose.onAllNodesWithText("Unchecked").fetchSemanticsNodes().size)
    }

    @Test
    fun theBoardCanBeGroupedByACheckbox() {
        show(bought)
        assertInColumn("Sale", "Checked")
        assertInColumn("Pasta", "Unchecked")
        assertInColumn("Shampoo", "Unchecked")
        // Una casella non ha un "senza valore": le colonne sono due, e due i "New".
        assertEquals(2, compose.onAllNodesWithText("New").fetchSemanticsNodes().size)
        compose.onAllNodesWithText("New")[1].performClick()
        compose.waitForIdle()
        assertEquals("false", addedInGroup)
    }

    @Test
    fun withoutSubGroupsTheColumnWithoutAValueIsStillThere() {
        show(shop)
        compose.onNodeWithText("No Supermercato").assertIsDisplayed()
        assertEquals(3, compose.onAllNodesWithText("New").fetchSemanticsNodes().size)
        compose.onAllNodesWithText("New")[2].performClick()
        compose.waitForIdle()
        assertEquals("", addedInGroup)
    }

    @Test
    fun ofItsOwnTheBoardShowsTheEmptyColumns() {
        // "Altro" è vuota, e c'è la colonna senza valore.
        show(kind)
        assertEquals(1, compose.onAllNodesWithText("Altro").fetchSemanticsNodes().size)
        assertEquals(1, compose.onAllNodesWithText("No Tipologia", substring = true, ignoreCase = true).fetchSemanticsNodes().size)
    }

    @Test
    fun hideEmptyGroupsTakesAwayTheColumnsWithoutCards() {
        // Con "Hide empty groups" (21/10/2026) restano solo quelle con schede.
        show(kind, hideEmptyGroups = true)
        assertEquals(0, compose.onAllNodesWithText("Altro").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("No Tipologia", substring = true, ignoreCase = true).fetchSemanticsNodes().size)
        assertInColumn("Pasta", "Cibo")
        assertInColumn("Shampoo", "Casa")
    }

    @Test
    fun hideNewTakesAwayEveryNewInTheSubGroups() {
        // "Hide New" (26/10/2026, chiesto dall'utente): nelle fasce, come in
        // "KPOP Music Shows", e con le colonne colorate; le schede restano.
        // Spento, i "New" ci sono come prima (vedi gli altri test).
        show(shop, kind, hideNew = true, colorCards = true)
        assertEquals(0, compose.onAllNodesWithText("New").fetchSemanticsNodes().size)
        assertInColumn("Pasta", "IPERPOLI")
        assertInColumn("Shampoo", "Lidl")
    }

    @Test
    fun hideNewTakesAwayEveryNewWithoutSubGroups() {
        // Senza fasce: niente "New", e la colonna senza valore resta.
        show(shop, hideNew = true)
        assertEquals(0, compose.onAllNodesWithText("New").fetchSemanticsNodes().size)
        compose.onNodeWithText("No Supermercato").assertIsDisplayed()
        assertInColumn("Pasta", "IPERPOLI")
    }

    @Test
    fun aBoardWithThousandsOfColumnsShowsTwelveAndLoadMore() {
        // Raggruppata per una data giorno per giorno (26/10/2026): l'app si
        // chiudeva per la memoria, con 4.000 colonne disegnate insieme.
        val day = DatabaseColumnEntity(id = "day", pageId = "food", name = "Giorno", type = ColumnType.DATE, orderIndex = 0)
        val rows = (0 until 30).map { DatabaseRowEntity(id = "r$it", pageId = "food", title = "Riga $it", orderIndex = it) } +
            DatabaseRowEntity(id = "senza", pageId = "food", title = "Senza data", orderIndex = 30)
        val cells = (0 until 30).associate { ("r$it" to "day") to java.time.LocalDate.of(2026, 1, 1).plusDays(it.toLong())
            .atStartOfDay(AppSettings.zoneId).toInstant().toEpochMilli().toString() }
        compose.setContent {
            NotionLocalTheme {
                androidx.compose.runtime.CompositionLocalProvider(com.gabriele.notionlocal.ui.screen.LocalRowLimits provides com.gabriele.notionlocal.ui.screen.RowLimits()) {
                    BoardLayout(
                        state = DatabaseTableState(columns = listOf(day), rows = rows, cellValues = cells),
                        levels = listOf(GroupLevel(day, hideEmpty = false)),
                        onCollapseGroup = { _, _, _ -> },
                        newRowTick = 0,
                        onOpenRow = {},
                        onRowLongPress = {},
                        onAddRowInGroups = {},
                        onAddGroupProperty = {}
                    )
                }
            }
        }
        compose.waitForIdle()
        assertEquals(12, (0 until 30).count { compose.onAllNodesWithText("Riga $it").fetchSemanticsNodes().isNotEmpty() })
        // La colonna senza data resta, per le schede nuove; le altre 18 dietro "Load more".
        assertEquals(1, compose.onAllNodesWithText("Senza data").fetchSemanticsNodes().size)
        assertEquals(1, compose.onAllNodesWithText("18").fetchSemanticsNodes().size)
    }
}