package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.buttons.ButtonBlock
import com.gabriele.notionlocal.data.buttons.ButtonConfig
import com.gabriele.notionlocal.data.buttons.ButtonStep
import com.gabriele.notionlocal.data.buttons.EditOp
import com.gabriele.notionlocal.data.buttons.PropertyEdit
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.BUTTON_EDITOR_TAG
import com.gabriele.notionlocal.ui.screen.BUTTON_PILL_TAG
import com.gabriele.notionlocal.ui.screen.PROPERTY_MENU_TAG
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
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
 * **I bottoni sullo schermo** (27/10/2026), nella pagina vera con l'editor
 * vero: il bottone toccato fa le sue azioni, la conferma ferma o lascia
 * andare, un bottone vuoto chiede cosa fare, "Edit button" dal menu del
 * blocco, la copia collegata, e la proprietà Button di un database che
 * cambia la sua riga.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h900dp-mdpi")
class ButtonScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db get() = AppDatabase.getInstance(context)

    private val insertWithConfirm = ButtonConfig(
        name = "Aggiungi nota",
        steps = listOf(
            ButtonStep.Confirm(message = "Aggiungo la nota?"),
            ButtonStep.InsertBlocks(blocks = listOf(ButtonBlock(type = "CHECKBOX", text = "Bere acqua")))
        )
    )

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        AppDatabase.resetForTests()
        context.getDatabasePath("notion_local.db").delete()
        db.withTransaction {
            db.pageDao().insert(PageEntity(id = "pagina", title = "Pagina di prova"))
            db.pageDao().insert(PageEntity(id = "altra", title = "Altra pagina"))
            db.blockDao().insert(BlockEntity(pageId = "pagina", textJson = """[{"text":"Sopra"}]""", orderIndex = 0))
            db.blockDao().insert(
                BlockEntity(
                    id = "bottone", pageId = "pagina", type = BlockType.BUTTON, orderIndex = 1,
                    attrsJson = BlockAttrs.encode(BlockAttrs(button = insertWithConfirm))
                )
            )
            db.blockDao().insert(BlockEntity(pageId = "pagina", textJson = """[{"text":"Sotto"}]""", orderIndex = 2))
        }
    }

    @After
    fun tearDown() {
        // Le letture ancora in viaggio finiscono prima che il database si chiuda:
        // chiuso sotto di loro, l'errore uscirebbe nel test dopo.
        compose.waitForIdle()
        Thread.sleep(300)
        compose.waitForIdle()
        AppDatabase.resetForTests()
    }

    private fun open(pageId: String = "pagina", waitText: String = "Sopra") {
        val factory = ViewModelFactory(context)
        compose.setContent {
            NotionLocalTheme {
                PageEditorScreen(pageId = pageId, factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {}, onNavigateToPage = {}, onNavigateToDatabase = {})
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText(waitText).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        try {
            compose.waitUntil(5_000) {
                Thread.sleep(20)
                condition()
            }
        } catch (e: Throwable) {
            throw AssertionError("Non è successo: $what", e)
        }
        compose.waitForIdle()
    }

    private fun shown(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    private fun blocks(pageId: String = "pagina") = runBlocking {
        db.blockDao().getBlocksForPageOnce(pageId).filter { it.parentBlockId == null }.sortedBy { it.orderIndex }
    }

    private fun textOf(block: BlockEntity) =
        Json { ignoreUnknownKeys = true }.decodeFromString<List<RichTextSpan>>(block.textJson).joinToString("") { it.text }

    /** I campi di testo della finestra delle azioni, non quelli della pagina sotto. */
    private fun editorFields() = compose.onAllNodes(hasSetTextAction() and hasAnyAncestor(hasTestTag(BUTTON_EDITOR_TAG)))

    @Test
    fun theButtonAsksAndContinueInsertsTheBlockBelowIt() {
        open()
        compose.onNodeWithText("Aggiungi nota").performClick()
        waitFor("la domanda") { shown("Aggiungo la nota?") }
        compose.onNodeWithText("Continue").performClick()
        waitFor("il blocco nuovo") { blocks().size == 4 }
        assertEquals(listOf(BlockType.PARAGRAPH, BlockType.BUTTON, BlockType.CHECKBOX, BlockType.PARAGRAPH), blocks().map { it.type })
        assertEquals("Bere acqua", textOf(blocks()[2]))
        // La casella ha davanti l'a-capo nascosto del suo campo (`RUN_LEAD`).
        waitFor("il blocco nuovo sullo schermo") { compose.onAllNodesWithText("Bere acqua", substring = true).fetchSemanticsNodes().isNotEmpty() }
        assertTrue(!shown("Aggiungo la nota?"))
    }

    @Test
    fun cancelInTheQuestionInsertsNothing() {
        open()
        compose.onNodeWithText("Aggiungi nota").performClick()
        waitFor("la domanda") { shown("Aggiungo la nota?") }
        compose.onNodeWithText("Cancel").performClick()
        waitFor("la domanda chiusa") { !shown("Aggiungo la nota?") }
        Thread.sleep(200)
        compose.waitForIdle()
        assertEquals(3, blocks().size)
    }

    @Test
    fun anEmptyButtonAsksWhatToDoAndThenDoesIt() = runBlocking<Unit> {
        db.blockDao().setAttrs("bottone", BlockAttrs.encode(BlockAttrs(button = ButtonConfig())))
        open()
        compose.onNodeWithText("New button").performClick()
        waitFor("la finestra delle azioni") { compose.onAllNodesWithTag(BUTTON_EDITOR_TAG).fetchSemanticsNodes().isNotEmpty() }
        editorFields()[0].performTextInput("Diario")
        compose.onNodeWithText("New action").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Insert blocks").performClick()
        compose.waitForIdle()
        // Il campo del blocco è il secondo, dopo il nome.
        editorFields()[1].performTextInput("Oggi")
        compose.onNodeWithText("Done").performClick()
        waitFor("le azioni salvate") { runBlocking { db.blockDao().getById("bottone")!!.attrs.button?.isSet == true } }
        val saved = runBlocking { db.blockDao().getById("bottone")!!.attrs.button!! }
        assertEquals("Diario", saved.name)
        assertEquals(listOf(ButtonBlock(id = (saved.steps.single() as ButtonStep.InsertBlocks).blocks.single().id, text = "Oggi")), (saved.steps.single() as ButtonStep.InsertBlocks).blocks)
        waitFor("la finestra chiusa") { compose.onAllNodesWithTag(BUTTON_EDITOR_TAG).fetchSemanticsNodes().isEmpty() }
        // Ora il tocco fa quello che si è scelto.
        compose.onNodeWithText("Diario").performClick()
        waitFor("il blocco") { blocks().size == 4 }
        assertEquals("Oggi", textOf(blocks()[2]))
    }

    @Test
    fun anIncompleteActionIsNotSavedAndSaysWhatIsMissing() = runBlocking<Unit> {
        db.blockDao().setAttrs("bottone", BlockAttrs.encode(BlockAttrs(button = ButtonConfig())))
        open()
        compose.onNodeWithText("New button").performClick()
        waitFor("la finestra delle azioni") { compose.onAllNodesWithTag(BUTTON_EDITOR_TAG).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("New action").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Add page to").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Done").performClick()
        waitFor("cosa manca") { shown("Choose the database") }
        // Resta aperta, e il bottone resta vuoto.
        assertTrue(compose.onAllNodesWithTag(BUTTON_EDITOR_TAG).fetchSemanticsNodes().isNotEmpty())
        assertEquals(ButtonConfig(), db.blockDao().getById("bottone")!!.attrs.button)
    }

    @Test
    fun editButtonFromTheBlockMenuOpensTheActions() {
        open()
        compose.onNodeWithText("Aggiungi nota").performTouchInput { longClick() }
        waitFor("il menu del blocco") { shown("Edit button") }
        assertTrue(shown("Link in another page"))
        compose.onNodeWithText("Edit button").performClick()
        waitFor("la finestra delle azioni") { compose.onAllNodesWithTag(BUTTON_EDITOR_TAG).fetchSemanticsNodes().isNotEmpty() }
        // Le azioni di adesso, in ordine.
        assertTrue(shown("Show confirmation"))
        assertTrue(shown("Insert blocks"))
        assertTrue(shown("Below button"))
    }

    @Test
    fun aLinkedCopyShowsTheRealButtonAndInsertsNextToItself() = runBlocking<Unit> {
        db.blockDao().insert(BlockEntity(pageId = "altra", textJson = """[{"text":"Qui la copia"}]""", orderIndex = 0))
        db.blockDao().insert(
            BlockEntity(id = "copia", pageId = "altra", type = BlockType.BUTTON, orderIndex = 1, attrsJson = BlockAttrs.encode(BlockAttrs(buttonLink = "bottone")))
        )
        open("altra", "Qui la copia")
        waitFor("il nome del bottone vero") { shown("Aggiungi nota") }
        compose.onNodeWithText("Aggiungi nota").performClick()
        waitFor("la domanda") { shown("Aggiungo la nota?") }
        compose.onNodeWithText("Continue").performClick()
        waitFor("il blocco sotto la copia") { blocks("altra").size == 3 }
        assertEquals(BlockType.CHECKBOX, blocks("altra")[2].type)
        // La pagina del bottone vero non cambia.
        assertEquals(3, blocks().size)
    }

    @Test
    fun aCopyOfADeletedButtonSaysSo() = runBlocking<Unit> {
        db.blockDao().insert(BlockEntity(pageId = "altra", textJson = """[{"text":"Qui la copia"}]""", orderIndex = 0))
        db.blockDao().insert(
            BlockEntity(id = "copia", pageId = "altra", type = BlockType.BUTTON, orderIndex = 1, attrsJson = BlockAttrs.encode(BlockAttrs(buttonLink = "sparito")))
        )
        open("altra", "Qui la copia")
        waitFor("il bottone cancellato") { shown("Deleted button") }
        compose.onNodeWithText("Deleted button").performClick()
        compose.waitForIdle()
        assertEquals(2, blocks("altra").size)
    }

    // --- La proprietà Button ---

    private fun withDatabase(config: ButtonConfig) = runBlocking {
        db.withTransaction {
            db.pageDao().insert(PageEntity(id = "spesa", parentId = "pagina", title = "Spesa", isDatabase = true))
            db.databaseDao().insertColumn(DatabaseColumnEntity(id = "preso", pageId = "spesa", name = "Preso", type = ColumnType.CHECKBOX, orderIndex = 0))
            db.databaseDao().insertColumn(DatabaseColumnEntity(id = "quanti", pageId = "spesa", name = "Quanti", type = ColumnType.NUMBER, orderIndex = 1))
            db.databaseDao().insertColumn(
                DatabaseColumnEntity(id = "azione", pageId = "spesa", name = "Azione", type = ColumnType.BUTTON, orderIndex = 2, optionsJson = ButtonConfig.encode(config))
            )
            db.databaseDao().insertRow(DatabaseRowEntity(id = "pane", pageId = "spesa", title = "Pane", orderIndex = 0))
            db.databaseDao().insertRow(DatabaseRowEntity(id = "latte", pageId = "spesa", title = "Latte", orderIndex = 1))
            db.databaseDao().insertCell(DatabaseCellEntity(rowId = "latte", columnId = "quanti", value = "2"))
            db.blockDao().insert(BlockEntity(pageId = "pagina", type = BlockType.DATABASE_LINK, linkedPageId = "spesa", orderIndex = 3))
        }
    }

    /** I bottoni delle righe con questa scritta, dall'alto. */
    private fun pills(label: String) = compose.onAllNodes(hasText(label) and hasTestTag(BUTTON_PILL_TAG))

    /** L'intestazione della colonna, che si chiama come la scritta dei suoi bottoni. */
    private fun header(label: String) = compose.onAllNodes(hasText(label) and !hasTestTag(BUTTON_PILL_TAG))

    private fun cell(rowId: String, columnId: String) = runBlocking { db.databaseDao().getCell(rowId, columnId)?.value.orEmpty() }

    @Test
    fun theButtonOfARowChangesThatRow() {
        withDatabase(
            ButtonConfig(
                name = "Preso",
                steps = listOf(
                    ButtonStep.EditPages(
                        edits = listOf(
                            PropertyEdit(columnId = "preso", op = EditOp.TOGGLE),
                            PropertyEdit(columnId = "quanti", op = EditOp.ADD, value = "1")
                        )
                    )
                )
            )
        )
        open()
        waitFor("i bottoni delle righe") { pills("Preso").fetchSemanticsNodes().size == 2 }
        // Il secondo è quello di Latte.
        pills("Preso")[1].performScrollTo().performClick()
        waitFor("Latte cambiata") { cell("latte", "quanti") == "3" }
        assertEquals("true", cell("latte", "preso"))
        // Pane non si tocca.
        assertEquals("", cell("pane", "preso"))
        assertEquals("", cell("pane", "quanti"))
    }

    @Test
    fun anEmptyButtonPropertyAsksWhatToDo() {
        withDatabase(ButtonConfig())
        open()
        // Senza una scritta, il bottone si chiama come la proprietà.
        waitFor("i bottoni delle righe") { pills("Azione").fetchSemanticsNodes().size == 2 }
        pills("Azione").onLast().performScrollTo().performClick()
        waitFor("la finestra delle azioni") { compose.onAllNodesWithTag(BUTTON_EDITOR_TAG).fetchSemanticsNodes().isNotEmpty() }
        // In una riga "Insert blocks" va solo in cima o in fondo alla sua pagina, e "This page" c'è.
        compose.onNodeWithText("New action").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Edit pages in").performClick()
        compose.waitForIdle()
        assertTrue(shown("This page"))
    }

    @Test
    fun changeTypeToButtonHasEditActionsUnderIt() {
        withDatabase(ButtonConfig(name = "Fatto"))
        open()
        waitFor("l'intestazione") { header("Quanti").fetchSemanticsNodes().isNotEmpty() }
        header("Quanti").onFirst().performScrollTo().performClick()
        waitFor("il menu") { compose.onAllNodesWithTag(PROPERTY_MENU_TAG).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Change type").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Button").performScrollTo().performClick()
        compose.waitForIdle()
        // Sotto il tipo scelto, le sue azioni: ancora nessuna.
        compose.onNodeWithText("Edit actions · No actions").performScrollTo().assertExists()
        compose.onNodeWithText("Save").performClick()
        // Prima di cambiare tipo si chiede (dal 19/11/2026).
        compose.onNode(hasText("Change type") and hasClickAction()).performClick()
        waitFor("la proprietà è un bottone") {
            runBlocking { db.databaseDao().getColumnById("quanti")!!.type } == ColumnType.BUTTON
        }
        assertEquals(ButtonConfig(), ButtonConfig.parse(runBlocking { db.databaseDao().getColumnById("quanti")!!.optionsJson }))
    }

    @Test
    fun theSlashMenuHasButtonAndLinkToButton() {
        val labels = com.gabriele.notionlocal.ui.screen.SLASH_ENTRIES.map { it.label }
        assertTrue("Button" in labels)
        assertTrue("Link to button" in labels)
        // Subito dopo "Link to page", fra i blocchi di base.
        assertEquals(labels.indexOf("Link to page") + 1, labels.indexOf("Button"))
    }

    @Test
    fun theHeaderMenuOfAButtonHasEditActionsAndNoFilterSortOrCalculate() {
        withDatabase(ButtonConfig(name = "Fatto"))
        open()
        waitFor("l'intestazione") { header("Azione").fetchSemanticsNodes().isNotEmpty() }
        header("Azione").onFirst().performScrollTo().performClick()
        waitFor("il menu") { compose.onAllNodesWithTag(PROPERTY_MENU_TAG).fetchSemanticsNodes().isNotEmpty() }
        assertTrue(shown("Edit actions"))
        assertTrue(!shown("Filter"))
        assertTrue(!shown("Sort"))
        assertTrue(!shown("Calculate"))
        compose.onNodeWithText("Edit actions").performClick()
        waitFor("la finestra delle azioni") { compose.onAllNodesWithTag(BUTTON_EDITOR_TAG).fetchSemanticsNodes().isNotEmpty() }
        assertTrue(compose.onAllNodes(hasText("Fatto")).fetchSemanticsNodes().isNotEmpty())
    }
}
