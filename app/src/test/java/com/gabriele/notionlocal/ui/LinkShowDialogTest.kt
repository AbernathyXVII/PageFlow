package com.gabriele.notionlocal.ui

import androidx.compose.ui.test.onNodeWithContentDescription
import android.app.Application
import android.content.Intent
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.LinkTitles
import com.gabriele.notionlocal.data.entity.LinkLabel
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.LinkDialog
import com.gabriele.notionlocal.ui.screen.TextLikeCell
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * **Quello che si vede di un link** (27/10/2026, chiesto dall'utente: "il
 * link per intero", "il titolo della pagina web" o "una scritta che io
 * scriverò manualmente"): le tre scelte sotto l'indirizzo nella finestrella
 * del link, e (dal 13/11/2026) la matita di una cella URL. Il titolo viene da
 * `LinkTitles`, qui senza rete.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-xhdpi")
class LinkShowDialogTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val natures = "https://pokemondb.net/mechanics/natures"
    private val bulbapedia = "https://bulbapedia.bulbagarden.net/wiki/Bulbasaur_(Pok%C3%A9mon)"
    private val original = LinkTitles.source
    private var saved: Pair<String, String?>? = null

    @Before
    fun setUp() {
        AppSettings.init(ApplicationProvider.getApplicationContext())
        LinkTitles.source = { url ->
            when (url) {
                natures -> "Natures | Pokémon Database"
                bulbapedia -> "Bulbasaur (Pokémon) - Bulbapedia"
                else -> null
            }
        }
    }

    @After
    fun tearDown() {
        LinkTitles.source = original
    }

    private fun dialog(current: String?, shown: String?) {
        compose.setContent {
            NotionLocalTheme {
                LinkDialog(current = current, shown = shown, targets = emptyList(), onDismiss = {}, onSave = { url, text -> saved = url to text }, onRemove = {})
            }
        }
        compose.waitForIdle()
    }

    /** Il tempo passa: la pausa prima di leggere il titolo. */
    private fun waitForText(text: String) {
        compose.waitUntil(5_000) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun thePageTitleTakesThePlaceOfTheLink() {
        dialog(current = natures, shown = natures)
        // Il link si vede per intero: "Full link" lo mostra com'è.
        compose.onNodeWithText("Full link").assertExists()
        compose.onNodeWithText("Page title").performClick()
        waitForText("Natures | Pokémon Database")
        compose.onNodeWithText("Done").performClick()
        compose.waitForIdle()
        assertEquals(natures to "Natures | Pokémon Database", saved)
    }

    @Test
    fun yourTextTakesThePlaceOfTheLink() {
        dialog(current = natures, shown = natures)
        compose.onNodeWithText("Your text").performClick()
        compose.waitForIdle()
        // Il suo campo parte vuoto: il link per intero non è una scritta.
        compose.onAllNodes(hasSetTextAction())[1].performTextInput("Le nature")
        compose.onNodeWithText("Done").performClick()
        compose.waitForIdle()
        assertEquals(natures to "Le nature", saved)
    }

    @Test
    fun withoutATitleNothingIsSaved() {
        dialog(current = "https://example.com/offline", shown = "https://example.com/offline")
        compose.onNodeWithText("Page title").performClick()
        waitForText("No title found")
        compose.onNodeWithText("Done").assertIsNotEnabled()
        // Il link per intero si può ancora scegliere.
        compose.onNodeWithText("Full link").performClick()
        compose.onNodeWithText("Done").performClick()
        compose.waitForIdle()
        assertEquals("https://example.com/offline" to null, saved)
    }

    @Test
    fun aNewLinkOnAWordKeepsTheWord() {
        dialog(current = null, shown = "Espeon")
        compose.onNode(hasSetTextAction() and hasText("Espeon")).assertExists()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("pokemondb.net/pokedex/espeon")
        compose.onNodeWithText("Done").performClick()
        compose.waitForIdle()
        assertEquals("https://pokemondb.net/pokedex/espeon" to null, saved)
    }

    @Test
    fun aSiteWrittenAsTextIsAlreadyTheAddress() {
        dialog(current = null, shown = "www.google.com")
        compose.onNodeWithText("Done").performClick()
        compose.waitForIdle()
        assertEquals("https://www.google.com" to null, saved)
    }

    @Test
    fun aChangedAddressGetsItsOwnTitle() {
        dialog(current = natures, shown = natures)
        compose.onNodeWithText("Page title").performClick()
        waitForText("Natures | Pokémon Database")
        compose.onNode(hasSetTextAction() and hasText(natures)).performTextReplacement(bulbapedia)
        waitForText("Bulbasaur (Pokémon) - Bulbapedia")
        compose.onNodeWithText("Done").performClick()
        compose.waitForIdle()
        assertEquals(bulbapedia to "Bulbasaur (Pokémon) - Bulbapedia", saved)
    }

    // --- Una cella URL ---

    private var label: LinkLabel? = null

    private fun cell(current: LinkLabel?) {
        label = current
        compose.setContent {
            NotionLocalTheme {
                Box(modifier = Modifier.width(380.dp)) {
                    TextLikeCell(
                        value = bulbapedia,
                        onValueChange = {},
                        keyboardOptions = KeyboardOptions.Default,
                        isLink = true,
                        linkLabel = current,
                        onLinkLabel = { label = it },
                        cellKey = "r:c"
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun aCellShowsItsTextAndStillOpensTheLink() {
        cell(LinkLabel("Bulbapedia", isTitle = false))
        compose.onNodeWithText(bulbapedia).assertDoesNotExist()
        compose.onNodeWithText("Bulbapedia").performClick()
        compose.waitForIdle()
        val intent = shadowOf(compose.activity).nextStartedActivity
            ?: shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent?.action)
        assertEquals(bulbapedia, intent?.dataString)
    }

    /** Dal 13/11/2026 "Show as…" non sta più nel menu della cella: è nel menu della proprietà. */
    @Test
    fun theCellMenuHasNoShowAs() {
        cell(null)
        compose.onNodeWithText(bulbapedia).performTouchInput { longClick() }
        compose.waitForIdle()
        compose.onNodeWithText("Copy link").assertExists()
        compose.onNodeWithText("Show as…").assertDoesNotExist()
        // Senza titolo, niente matita.
        compose.onNodeWithContentDescription("Edit title").assertDoesNotExist()
    }

    /** **La matita** (13/11/2026, chiesto dall'utente): il titolo letto si corregge. */
    @Test
    fun thePencilEditsTheTitle() {
        cell(LinkLabel("Bulbasaur (Pokémon) - Bulbapedia", isTitle = true))
        compose.onNodeWithContentDescription("Edit title").performClick()
        compose.waitForIdle()
        compose.onNode(hasSetTextAction()).performTextReplacement("Bulbasaur")
        compose.onNodeWithText("Save").performClick()
        compose.waitForIdle()
        assertEquals(LinkLabel("Bulbasaur", isTitle = false), label)
    }

    /** Svuotata: di nuovo il titolo della pagina (la scritta se ne va, il database lo rilegge). */
    @Test
    fun theEmptiedPencilGivesThePageTitleBack() {
        cell(LinkLabel("Bulbasaur", isTitle = false))
        compose.onNodeWithContentDescription("Edit title").performClick()
        compose.waitForIdle()
        compose.onNode(hasSetTextAction()).performTextReplacement("")
        compose.onNodeWithText("Save").performClick()
        compose.waitForIdle()
        assertEquals(null, label)
    }
}
