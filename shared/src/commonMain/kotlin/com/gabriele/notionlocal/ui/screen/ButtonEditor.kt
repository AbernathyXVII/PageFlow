package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.buttons.BUTTON_BLOCK_TYPES
import com.gabriele.notionlocal.data.buttons.ButtonBlock
import com.gabriele.notionlocal.data.buttons.ButtonConfig
import com.gabriele.notionlocal.data.buttons.ButtonFilter
import com.gabriele.notionlocal.data.buttons.ButtonStep
import com.gabriele.notionlocal.data.buttons.EditOp
import com.gabriele.notionlocal.data.buttons.InsertPlace
import com.gabriele.notionlocal.data.buttons.NAME_PROPERTY_ID
import com.gabriele.notionlocal.data.buttons.PropertyEdit
import com.gabriele.notionlocal.data.buttons.buttonEditable
import com.gabriele.notionlocal.data.buttons.editOps
import com.gabriele.notionlocal.data.buttons.needsValue
import com.gabriele.notionlocal.data.buttons.selectOptions
import com.gabriele.notionlocal.data.buttons.splitTags
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.isMultiTag
import com.gabriele.notionlocal.data.entity.isTag
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.MULTI_VALUE_SEPARATOR
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.notify.NotificationPermission
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.ui.i18n.ButtonStrings
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import com.gabriele.notionlocal.platform.showMessage
import com.gabriele.notionlocal.platform.LocalPlatformContext

// ---------------------------------------------------------------------------
// **La finestra delle azioni di un bottone** (dal 27/10/2026, chiesto
// dall'utente: "quando attivo un bottone per la prima volta, mi chiederà che
// cosa voglio fare [...] se tocco quel bottone allora accade questo, questo e
// quest'altro"). In cima nome e icona, sotto le azioni in fila, ognuna nel suo
// riquadro, e "New action" per aggiungerne. Come su Notion non si salva
// un'azione a metà: "Done" dice cosa manca. Chiudendola in un altro modo si
// salva se è completa, se no resta com'era.
// ---------------------------------------------------------------------------

const val BUTTON_EDITOR_TAG = "buttonEditor"
const val BUTTON_NAME_FIELD_TAG = "buttonNameField"

/**
 * Quello che la finestra delle azioni legge da fuori: i database e le loro
 * proprietà, le pagine da scegliere, le immagini delle icone. Lo danno le
 * schermate della pagina e del database.
 */
@Stable
class ButtonEditorEnv(
    val pages: PageRepository,
    /** Per scegliere la pagina di "Open page": null dove non si può. */
    val factory: ViewModelFactory?,
    val imageStore: PageImageStore
)

val LocalButtonEditorEnv = compositionLocalOf<ButtonEditorEnv?> { null }

/** Il bottone che si sta cambiando nella finestra, finché non lo si salva. */
@Stable
class ButtonEditorState(initial: ButtonConfig) {
    var draft by mutableStateOf(initial)
}

/** Se tutte le azioni di [config] sono complete: solo allora si salva. */
fun isComplete(config: ButtonConfig, thisPageDatabaseId: String?): Boolean =
    config.steps.all { stepProblem(it, thisPageDatabaseId) == null }

/**
 * La finestra. [inDatabase]: il bottone è una proprietà, e "Insert blocks"
 * va solo in cima o in fondo alla pagina della riga. [thisPageDatabaseId]: il
 * database di "This page", o null se qui "This page" non c'è. [linked]: si sta
 * cambiando il bottone vero da una sua copia collegata.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButtonEditorSheet(
    initial: ButtonConfig,
    inDatabase: Boolean,
    thisPageDatabaseId: String?,
    onDismiss: () -> Unit,
    onSave: (ButtonConfig) -> Unit,
    linked: Boolean = false
) {
    val state = remember { ButtonEditorState(initial) }
    NoteBottomSheet(
        onDismissRequest = {
            if (isComplete(state.draft, thisPageDatabaseId) && state.draft != initial) onSave(state.draft)
            onDismiss()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        ButtonEditorContent(
            state = state,
            inDatabase = inDatabase,
            thisPageDatabaseId = thisPageDatabaseId,
            linked = linked,
            onCancel = onDismiss,
            onDone = {
                if (state.draft != initial) onSave(state.draft)
                onDismiss()
            },
            modifier = Modifier.fillMaxHeight(0.92f).navigationBarsPadding().imePadding()
        )
    }
}

/**
 * Il contenuto della finestra: in cima "Cancel" e "Done" ([onDone] solo con
 * le azioni complete, se no dice cosa manca), poi nome, icona e azioni.
 */
@Composable
fun ButtonEditorContent(
    state: ButtonEditorState,
    inDatabase: Boolean,
    thisPageDatabaseId: String?,
    linked: Boolean,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * **Per le automazioni** (dal 03/11/2026, vedi `AutomationsUi.kt`): il
     * titolo, quello che sta sopra le azioni al posto di nome e icona del
     * bottone ([header], e [headerComplete] se è a posto per salvare), la
     * scritta sopra le azioni, quali azioni si possono scegliere, il database
     * in cui "Add page to" aggiunge di suo e se c'è "Open it afterwards".
     */
    title: String = ButtonStrings.editButton,
    header: (@Composable () -> Unit)? = null,
    headerComplete: Boolean = true,
    actionsLabel: String = ButtonStrings.whenTapped,
    kinds: List<StepKind> = StepKind.entries,
    newPageDatabaseId: String? = null,
    canOpenAfter: Boolean = true
) {
    val env = LocalButtonEditorEnv.current
    val context = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    var draft by state::draft
    var showErrors by remember { mutableStateOf(false) }
    var pickingIcon by remember { mutableStateOf(false) }
    var databaseFor by remember { mutableStateOf<String?>(null) }
    var pageFor by remember { mutableStateOf<String?>(null) }
    var databases by remember { mutableStateOf<List<PageRepository.LinkableDatabase>?>(null) }

    fun change(step: ButtonStep) {
        draft = draft.copy(steps = draft.steps.map { if (it.id == step.id) step else it })
    }
    val problems = draft.steps.associate { it.id to stepProblem(it, thisPageDatabaseId) }
    val complete = problems.values.all { it == null } && headerComplete

    Column(modifier = modifier.fillMaxWidth().testTag(BUTTON_EDITOR_TAG)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NotionWhite,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onCancel) { Text(Strings.cancel) }
            TextButton(
                onClick = {
                    if (complete) {
                        onDone()
                    } else {
                        showErrors = true
                        showMessage(context, ButtonStrings.fixActions)
                    }
                }
            ) { Text(Strings.done) }
        }
        HorizontalDivider()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.size(12.dp))
            if (linked) {
                Text(ButtonStrings.linkedNote, style = MaterialTheme.typography.bodySmall, color = NotionGray400)
                Spacer(modifier = Modifier.size(10.dp))
            }
            // Nome e icona, come la riga in cima del menu di una proprietà.
            if (header != null) header() else Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSheet)
                        .clickable { pickingIcon = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (draft.hasIcon) {
                        PageIconView(emoji = draft.icon ?: "", iconImage = draft.iconImage, isDatabase = false, size = 22.dp, iconSymbol = draft.iconSymbol)
                    } else {
                        Icon(Icons.Filled.SentimentSatisfied, contentDescription = ButtonStrings.icon, tint = NotionGray400, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.size(12.dp))
                PlainField(
                    value = draft.name,
                    onValueChange = { draft = draft.copy(name = it.replace("\n", " ")) },
                    hint = if (inDatabase) ButtonStrings.buttonLabel else ButtonStrings.buttonName,
                    modifier = Modifier.weight(1f).testTag(BUTTON_NAME_FIELD_TAG)
                )
            }

            Spacer(modifier = Modifier.size(20.dp))
            Text(actionsLabel, style = MaterialTheme.typography.labelMedium, color = NotionGray400)
            Spacer(modifier = Modifier.size(6.dp))
            if (draft.steps.isEmpty()) {
                Text(ButtonStrings.noActionsYet, style = MaterialTheme.typography.bodyMedium, color = NotionGray400)
                Spacer(modifier = Modifier.size(8.dp))
            }
            draft.steps.forEachIndexed { index, step ->
                key(step.id) {
                    StepCard(
                        step = step,
                        problem = problems[step.id]?.takeIf { showErrors },
                        canMoveUp = index > 0,
                        canMoveDown = index < draft.steps.lastIndex,
                        inDatabase = inDatabase,
                        thisPageDatabaseId = thisPageDatabaseId,
                        onChange = ::change,
                        onMove = { up ->
                            val list = draft.steps.toMutableList()
                            val to = if (up) index - 1 else index + 1
                            list.add(to, list.removeAt(index))
                            draft = draft.copy(steps = list)
                        },
                        onRemove = { draft = draft.copy(steps = draft.steps.filterNot { it.id == step.id }) },
                        onPickDatabase = { databaseFor = step.id },
                        onPickPage = { pageFor = step.id },
                        canOpenAfter = canOpenAfter
                    )
                }
            }
            NewActionButton(
                kinds = kinds,
                onPick = { kind ->
                    if (kind == StepKind.NOTIFY) NotificationPermission.request()
                    val step = kind.create(inDatabase).let { if (it is ButtonStep.AddPage && newPageDatabaseId != null) it.copy(databaseId = newPageDatabaseId) else it }
                    draft = draft.copy(steps = draft.steps + step)
                }
            )
            Spacer(modifier = Modifier.size(32.dp))
        }
    }

    if (pickingIcon && env != null) {
        IconPickerSheet(
            store = env.imageStore,
            hasIcon = draft.hasIcon,
            onPick = { choice ->
                draft = draft.withIcon(choice)
                // L'immagine di prima resta: la può usare ancora l'Annulla, o una copia.
                null
            },
            onDismiss = { pickingIcon = false }
        )
    }

    databaseFor?.let { stepId ->
        LaunchedEffect(Unit) { if (databases == null && env != null) databases = env.pages.linkableDatabases() }
        LinkedSourcePickerSheet(
            databases = databases,
            title = ButtonStrings.chooseDatabase,
            hint = null,
            onDismiss = { databaseFor = null },
            onPick = { page ->
                databaseFor = null
                // Cambiando database, le proprietà scelte erano di quello di prima.
                draft.steps.find { it.id == stepId }?.let { step ->
                    when (step) {
                        is ButtonStep.AddPage -> if (step.databaseId != page.id) change(step.copy(databaseId = page.id, edits = emptyList()))
                        is ButtonStep.EditPages -> if (step.databaseId != page.id) change(step.copy(databaseId = page.id, filter = null, edits = emptyList()))
                        else -> Unit
                    }
                }
            }
        )
    }

    pageFor?.let { stepId ->
        val factory = env?.factory
        if (factory != null) {
            DestinationPickerSheet(
                factory = factory,
                title = ButtonStrings.choosePage,
                hint = "",
                viewModelKey = "button-open-page",
                onDismiss = { pageFor = null },
                onPick = { node ->
                    pageFor = null
                    scope.launch {
                        // Una riga mai aperta si sceglie lo stesso: la sua pagina nasce adesso.
                        val pageId = node.pageId ?: node.rowId?.let { env.pages.ensureRowPage(it) } ?: return@launch
                        (draft.steps.find { it.id == stepId } as? ButtonStep.Open)?.let { change(it.copy(pageId = pageId, url = "")) }
                    }
                }
            )
        }
    }
}

/** Cosa manca a un'azione per poterla salvare, o null se è completa. */
fun stepProblem(step: ButtonStep, thisPageDatabaseId: String?): String? = when (step) {
    is ButtonStep.InsertBlocks -> if (step.blocks.isEmpty()) ButtonStrings.missingBlocks else null
    is ButtonStep.AddPage -> when {
        step.databaseId == null -> ButtonStrings.missingDatabase
        step.edits.any { !it.isComplete() } -> ButtonStrings.missingProperty
        else -> null
    }
    is ButtonStep.EditPages -> when {
        step.databaseId == null && thisPageDatabaseId == null -> ButtonStrings.missingDatabase
        step.edits.isEmpty() -> ButtonStrings.missingEdits
        step.edits.any { !it.isComplete() } -> ButtonStrings.missingProperty
        step.filter?.values?.none { it.isNotBlank() } == true -> ButtonStrings.missingFilter
        else -> null
    }
    is ButtonStep.Open -> if (step.pageId == null && step.url.isBlank()) ButtonStrings.missingPageOrLink else null
    is ButtonStep.Confirm, is ButtonStep.Notify -> null
}

private fun PropertyEdit.isComplete(): Boolean = !op.needsValue() || value.isNotBlank()

/** I tipi di azione del menu "New action", nell'ordine di Notion. */
enum class StepKind(val icon: ImageVector) {
    INSERT_BLOCKS(Icons.Filled.AddBox),
    ADD_PAGE(Icons.Filled.NoteAdd),
    EDIT_PAGES(Icons.Filled.Edit),
    CONFIRM(Icons.Filled.HelpOutline),
    OPEN(Icons.Filled.OpenInNew),
    NOTIFY(Icons.Filled.Notifications);

    val label: String
        get() = when (this) {
            INSERT_BLOCKS -> ButtonStrings.insertBlocks
            ADD_PAGE -> ButtonStrings.addPageTo
            EDIT_PAGES -> ButtonStrings.editPagesIn
            CONFIRM -> ButtonStrings.showConfirmation
            OPEN -> ButtonStrings.openPageOrUrl
            NOTIFY -> ButtonStrings.sendNotification
        }

    fun create(inDatabase: Boolean): ButtonStep = when (this) {
        // In una riga il bottone non sta nella pagina: i blocchi vanno in fondo a lei.
        INSERT_BLOCKS -> ButtonStep.InsertBlocks(place = if (inDatabase) InsertPlace.BOTTOM_OF_PAGE else InsertPlace.BELOW_BUTTON)
        ADD_PAGE -> ButtonStep.AddPage()
        EDIT_PAGES -> ButtonStep.EditPages()
        CONFIRM -> ButtonStep.Confirm()
        OPEN -> ButtonStep.Open()
        NOTIFY -> ButtonStep.Notify()
    }

    companion object {
        fun of(step: ButtonStep): StepKind = when (step) {
            is ButtonStep.InsertBlocks -> INSERT_BLOCKS
            is ButtonStep.AddPage -> ADD_PAGE
            is ButtonStep.EditPages -> EDIT_PAGES
            is ButtonStep.Confirm -> CONFIRM
            is ButtonStep.Open -> OPEN
            is ButtonStep.Notify -> NOTIFY
        }
    }
}

@Composable
private fun NewActionButton(kinds: List<StepKind> = StepKind.entries, onPick: (StepKind) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { open = true }
                .padding(horizontal = 6.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text(ButtonStrings.newAction, color = NotionGray400, style = MaterialTheme.typography.bodyLarge)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            kinds.forEach { kind ->
                DropdownMenuItem(
                    text = { Text(kind.label) },
                    leadingIcon = { Icon(kind.icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    onClick = {
                        open = false
                        onPick(kind)
                    }
                )
            }
        }
    }
}

/** Un'azione nel suo riquadro: il nome in cima col menu "…", sotto i suoi campi, in fondo cosa manca. */
@Composable
private fun StepCard(
    step: ButtonStep,
    problem: String?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    inDatabase: Boolean,
    thisPageDatabaseId: String?,
    onChange: (ButtonStep) -> Unit,
    onMove: (Boolean) -> Unit,
    onRemove: () -> Unit,
    onPickDatabase: () -> Unit,
    onPickPage: () -> Unit,
    canOpenAfter: Boolean = true
) {
    val kind = StepKind.of(step)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(kind.icon, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text(kind.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = NotionWhite, modifier = Modifier.weight(1f))
            var menu by remember { mutableStateOf(false) }
            Box {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { menu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.MoreHoriz, contentDescription = ButtonStrings.options, tint = NotionGray400)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(ButtonStrings.moveUp) }, enabled = canMoveUp, onClick = { menu = false; onMove(true) })
                    DropdownMenuItem(text = { Text(ButtonStrings.moveDown) }, enabled = canMoveDown, onClick = { menu = false; onMove(false) })
                    DropdownMenuItem(
                        text = { Text(ButtonStrings.remove, color = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; onRemove() }
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(end = 8.dp)) {
            when (step) {
                is ButtonStep.InsertBlocks -> InsertBlocksFields(step, inDatabase, onChange)
                is ButtonStep.AddPage -> AddPageFields(step, onChange, onPickDatabase, canOpenAfter)
                is ButtonStep.EditPages -> EditPagesFields(step, thisPageDatabaseId, onChange, onPickDatabase)
                is ButtonStep.Confirm -> PlainField(
                    value = step.message,
                    onValueChange = { onChange(step.copy(message = it)) },
                    hint = ButtonStrings.areYouSure,
                    boxed = true,
                    singleLine = false
                )
                is ButtonStep.Open -> OpenFields(step, onChange, onPickPage)
                is ButtonStep.Notify -> {
                    PlainField(step.title, { onChange(step.copy(title = it.replace("\n", " "))) }, ButtonStrings.titleHint, boxed = true)
                    Spacer(modifier = Modifier.size(8.dp))
                    PlainField(step.text, { onChange(step.copy(text = it)) }, ButtonStrings.text, boxed = true, singleLine = false)
                }
            }
            if (problem != null) {
                Spacer(modifier = Modifier.size(8.dp))
                Text(problem, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// --- Le azioni, una per una ---

@Composable
private fun InsertBlocksFields(step: ButtonStep.InsertBlocks, inDatabase: Boolean, onChange: (ButtonStep) -> Unit) {
    val places = if (inDatabase) listOf(InsertPlace.TOP_OF_PAGE, InsertPlace.BOTTOM_OF_PAGE) else InsertPlace.entries
    MenuChip(
        text = ButtonStrings.placeName(step.place),
        items = places.map { place -> ButtonStrings.placeName(place) to { onChange(step.copy(place = place)) } }
    )
    step.blocks.forEach { block ->
        key(block.id) {
            Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                MenuChip(
                    text = blockTypeName(block.type),
                    items = BUTTON_BLOCK_TYPES.map { type ->
                        blockTypeName(type) to { onChange(step.copy(blocks = step.blocks.map { if (it.id == block.id) it.copy(type = type) else it })) }
                    }
                )
                Spacer(modifier = Modifier.size(8.dp))
                if (block.type == "DIVIDER") {
                    Text("———", color = NotionGray400, modifier = Modifier.weight(1f))
                } else {
                    PlainField(
                        value = block.text,
                        onValueChange = { text ->
                            onChange(step.copy(blocks = step.blocks.map { if (it.id == block.id) it.copy(text = text.replace("\n", " ")) else it }))
                        },
                        hint = ButtonStrings.blockTextHint,
                        boxed = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                RemoveIcon { onChange(step.copy(blocks = step.blocks.filterNot { it.id == block.id })) }
            }
        }
    }
    AddActionRow(ButtonStrings.addBlock) {
        // Il blocco nuovo è dello stesso tipo dell'ultimo: tre caselle si scrivono di fila.
        val type = step.blocks.lastOrNull()?.type?.takeIf { it != "DIVIDER" } ?: "PARAGRAPH"
        onChange(step.copy(blocks = step.blocks + ButtonBlock(type = type)))
    }
}

/** Il nome di un tipo di blocco nella lingua dell'app, come nei menu "/" e "+". */
fun blockTypeName(type: String): String = EditorStrings.blockType(
    when (type) {
        "BULLET_LIST_ITEM" -> "Bulleted list"
        "NUMBERED_LIST_ITEM" -> "Numbered list"
        "CHECKBOX" -> "To-do list"
        "TOGGLE" -> "Toggle list"
        "QUOTE" -> "Quote"
        "CALLOUT" -> "Callout"
        "DIVIDER" -> "Divider"
        else -> "Text"
    }
)

/** Il database di un'azione, letto: la sua pagina (null = non c'è più) e le proprietà. */
private class ChosenDatabase(val page: PageEntity?, val columns: List<DatabaseColumnEntity>)

@Composable
private fun rememberChosenDatabase(databaseId: String?): ChosenDatabase? {
    val env = LocalButtonEditorEnv.current
    val loaded by produceState<ChosenDatabase?>(null, databaseId, env) {
        value = if (databaseId == null || env == null) null else ChosenDatabase(env.pages.realDatabase(databaseId), env.pages.databaseColumns(databaseId))
    }
    return loaded
}

@Composable
private fun DatabaseChip(chosen: ChosenDatabase?, databaseId: String?, onClick: () -> Unit) {
    val text = when {
        databaseId == null -> ButtonStrings.chooseDatabase
        chosen == null -> ""
        chosen.page == null -> ButtonStrings.problem(com.gabriele.notionlocal.data.buttons.ButtonProblem.NO_DATABASE)
        else -> chosen.page.title.ifBlank { Strings.untitled }
    }
    Chip(text = text, leading = chosen?.page?.let { page -> { PageIconView(emoji = page.icon, iconImage = page.iconImage, isDatabase = true, size = 16.dp, iconSymbol = page.iconSymbol) } }, onClick = onClick)
}

@Composable
private fun AddPageFields(step: ButtonStep.AddPage, onChange: (ButtonStep) -> Unit, onPickDatabase: () -> Unit, canOpenAfter: Boolean = true) {
    val chosen = rememberChosenDatabase(step.databaseId)
    DatabaseChip(chosen, step.databaseId, onPickDatabase)
    if (step.databaseId == null) return
    Spacer(modifier = Modifier.size(8.dp))
    PlainField(step.title, { onChange(step.copy(title = it.replace("\n", " "))) }, ButtonStrings.newPageName, boxed = true)
    // {date} e {week} nel nome (dal 03/11/2026): "Settimana {week}" diventa "Settimana 45".
    Text(ButtonStrings.titlePlaceholders, style = MaterialTheme.typography.bodySmall, color = NotionGray400, modifier = Modifier.padding(top = 4.dp))
    // **Il template della pagina nuova** (dal 03/11/2026).
    val env = LocalButtonEditorEnv.current
    val templates by remember(env) { env?.pages?.observeTemplates() ?: kotlinx.coroutines.flow.flowOf(emptyList()) }
        .collectAsState(initial = emptyList())
    val template = templates.find { it.id == step.templateId }
    Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(ButtonStrings.template, style = MaterialTheme.typography.bodyMedium, color = NotionWhite, modifier = Modifier.padding(end = 8.dp))
        MenuChip(
            text = template?.title?.ifBlank { Strings.untitled } ?: ButtonStrings.noTemplate,
            items = listOf(ButtonStrings.noTemplate to { onChange(step.copy(templateId = null)) }) +
                templates.map { page -> page.title.ifBlank { Strings.untitled } to { onChange(step.copy(templateId = page.id)) } }
        )
    }
    PropertyEdits(
        edits = step.edits,
        columns = chosen?.columns,
        allowName = false,
        onChange = { onChange(step.copy(edits = it)) }
    )
    if (canOpenAfter) Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(ButtonStrings.openAfterwards, style = MaterialTheme.typography.bodyMedium, color = NotionWhite, modifier = Modifier.weight(1f))
        Switch(checked = step.openAfter, onCheckedChange = { onChange(step.copy(openAfter = it)) })
    }
}

@Composable
private fun EditPagesFields(
    step: ButtonStep.EditPages,
    thisPageDatabaseId: String?,
    onChange: (ButtonStep) -> Unit,
    onPickDatabase: () -> Unit
) {
    val chosen = rememberChosenDatabase(step.databaseId ?: thisPageDatabaseId)
    if (step.databaseId == null && thisPageDatabaseId != null) {
        MenuChip(
            text = ButtonStrings.thisPage,
            items = listOf(ButtonStrings.thisPage to {}, ButtonStrings.chooseDatabase to onPickDatabase)
        )
        Text(ButtonStrings.thisPageNote, style = MaterialTheme.typography.bodySmall, color = NotionGray400, modifier = Modifier.padding(top = 4.dp))
    } else if (thisPageDatabaseId != null) {
        // Si torna a "This page" dallo stesso menu.
        MenuChip(
            text = chosen?.page?.title?.ifBlank { Strings.untitled } ?: ButtonStrings.chooseDatabase,
            items = listOf(
                ButtonStrings.thisPage to { onChange(step.copy(databaseId = null, filter = null, edits = emptyList())) },
                ButtonStrings.chooseDatabase to onPickDatabase
            )
        )
    } else {
        DatabaseChip(chosen, step.databaseId, onPickDatabase)
    }
    val columns = chosen?.columns
    if (step.databaseId != null) {
        Spacer(modifier = Modifier.size(8.dp))
        FilterFields(step, columns, onChange)
    }
    if (step.databaseId != null || thisPageDatabaseId != null) {
        PropertyEdits(
            edits = step.edits,
            columns = columns,
            allowName = true,
            onChange = { onChange(step.copy(edits = it)) }
        )
    }
}

/** Le proprietà su cui si può filtrare: quelle che si possono anche cambiare, tranne le date. */
private fun DatabaseColumnEntity.filterable(): Boolean = type.buttonEditable && type != ColumnType.DATE

@Composable
private fun FilterFields(step: ButtonStep.EditPages, columns: List<DatabaseColumnEntity>?, onChange: (ButtonStep) -> Unit) {
    val filter = step.filter
    val candidates = columns.orEmpty().filter { it.filterable() }
    MenuChip(
        text = if (filter == null) ButtonStrings.allPages else ButtonStrings.onlyPagesWhere,
        items = listOf(
            ButtonStrings.allPages to { onChange(step.copy(filter = null)) },
            ButtonStrings.onlyPagesWhere to {
                if (filter == null) onChange(step.copy(filter = ButtonFilter(columnId = candidates.firstOrNull()?.id ?: NAME_PROPERTY_ID)))
            }
        )
    )
    if (filter == null) return
    val column = columns?.find { it.id == filter.columnId }
    Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        MenuChip(
            text = if (filter.columnId == NAME_PROPERTY_ID) ButtonStrings.name else column?.let { DbStrings.columnName(it.name) } ?: ButtonStrings.chooseProperty,
            items = listOf(ButtonStrings.name to { onChange(step.copy(filter = ButtonFilter(NAME_PROPERTY_ID))) }) +
                candidates.map { c -> DbStrings.columnName(c.name) to { onChange(step.copy(filter = ButtonFilter(c.id))) } }
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(ButtonStrings.isWord, color = NotionGray400)
    }
    Spacer(modifier = Modifier.size(8.dp))
    ValueInput(
        column = column,
        isName = filter.columnId == NAME_PROPERTY_ID,
        multiple = column?.type?.isTag == true,
        value = filter.values.joinToString(MULTI_VALUE_SEPARATOR),
        onValue = { value -> onChange(step.copy(filter = filter.copy(values = if (value.isEmpty()) emptyList() else value.split(MULTI_VALUE_SEPARATOR)))) },
        checkboxChoice = true
    )
}

/** I cambi di proprietà di un'azione, e "Edit a property" per aggiungerne. */
@Composable
private fun PropertyEdits(
    edits: List<PropertyEdit>,
    columns: List<DatabaseColumnEntity>?,
    allowName: Boolean,
    onChange: (List<PropertyEdit>) -> Unit
) {
    edits.forEach { edit ->
        key(edit.id) {
            PropertyEditRow(
                edit = edit,
                column = columns?.find { it.id == edit.columnId },
                loaded = columns != null,
                onChange = { changed -> onChange(edits.map { if (it.id == edit.id) changed else it }) },
                onRemove = { onChange(edits.filterNot { it.id == edit.id }) }
            )
        }
    }
    val editable = columns.orEmpty().filter { it.type.buttonEditable }
    var open by remember { mutableStateOf(false) }
    Box {
        AddActionRow(ButtonStrings.setProperty) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (allowName) {
                DropdownMenuItem(text = { Text(ButtonStrings.name) }, onClick = {
                    open = false
                    onChange(edits + PropertyEdit(columnId = NAME_PROPERTY_ID, op = EditOp.SET))
                })
            }
            editable.forEach { column ->
                DropdownMenuItem(
                    text = { Text(DbStrings.columnName(column.name)) },
                    leadingIcon = { Icon(column.type.icon(), contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        open = false
                        onChange(edits + PropertyEdit(columnId = column.id, op = column.type.editOps().first()))
                    }
                )
            }
        }
    }
}

@Composable
private fun PropertyEditRow(
    edit: PropertyEdit,
    column: DatabaseColumnEntity?,
    loaded: Boolean,
    onChange: (PropertyEdit) -> Unit,
    onRemove: () -> Unit
) {
    val isName = edit.columnId == NAME_PROPERTY_ID
    val ops = when {
        isName -> listOf(EditOp.SET, EditOp.CLEAR)
        column != null -> column.type.editOps()
        else -> emptyList()
    }
    fun opLabel(op: EditOp) = if (column?.type == ColumnType.NUMBER) ButtonStrings.numberOpName(op) else ButtonStrings.opName(op)
    Column(modifier = Modifier.padding(top = 10.dp)) {
        // La riga intera, con la X in fondo a destra (sul telefono stava a metà).
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (column != null) {
                Icon(column.type.icon(), contentDescription = null, tint = NotionGray400, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(6.dp))
            }
            Text(
                text = when {
                    isName -> ButtonStrings.name
                    column != null -> DbStrings.columnName(column.name)
                    loaded -> ButtonStrings.problem(com.gabriele.notionlocal.data.buttons.ButtonProblem.NO_PROPERTY)
                    else -> ""
                },
                color = if (!isName && column == null && loaded) MaterialTheme.colorScheme.error else NotionWhite,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            if (ops.isNotEmpty()) {
                MenuChip(
                    text = opLabel(edit.op),
                    items = ops.map { op -> opLabel(op) to { onChange(edit.copy(op = op, value = if (op.needsValue()) edit.value else "")) } }
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            RemoveIcon(onRemove)
        }
        if (edit.op.needsValue() && (isName || column != null)) {
            Spacer(modifier = Modifier.size(6.dp))
            ValueInput(
                column = column,
                isName = isName,
                multiple = column?.type?.isMultiTag == true,
                value = edit.value,
                onValue = { onChange(edit.copy(value = it)) },
                checkboxChoice = false
            )
        }
    }
}

/**
 * Il valore da scrivere o da cercare: i tag da toccare per Select e
 * Multi-select ([multiple]: se ne possono scegliere più d'uno), "Checked" e
 * "Unchecked" per la casella di un filtro, se no un campo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ValueInput(
    column: DatabaseColumnEntity?,
    isName: Boolean,
    multiple: Boolean,
    value: String,
    onValue: (String) -> Unit,
    checkboxChoice: Boolean
) {
    val type = if (isName) ColumnType.TEXT else column?.type
    when {
        type?.isTag == true -> {
            val chosen = splitTags(value)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                column!!.selectOptions().forEach { option ->
                    val selected = option.label in chosen
                    TagChoice(label = option.label, color = option.color, selected = selected) {
                        val next = when {
                            !multiple -> if (selected) emptyList() else listOf(option.label)
                            selected -> chosen - option.label
                            else -> chosen + option.label
                        }
                        onValue(next.joinToString(MULTI_VALUE_SEPARATOR))
                    }
                }
            }
        }
        type == ColumnType.CHECKBOX && checkboxChoice -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TagChoice(ButtonStrings.checked, null, value == "true") { onValue("true") }
            TagChoice(ButtonStrings.unchecked, null, value == "false") { onValue("false") }
        }
        else -> PlainField(
            value = value,
            onValueChange = { onValue(it.replace("\n", " ")) },
            hint = ButtonStrings.filterValueHint,
            boxed = true,
            keyboardType = when (type) {
                ColumnType.NUMBER -> KeyboardType.Decimal
                ColumnType.URL -> KeyboardType.Uri
                ColumnType.EMAIL -> KeyboardType.Email
                ColumnType.PHONE -> KeyboardType.Phone
                else -> KeyboardType.Text
            }
        )
    }
}

@Composable
private fun OpenFields(step: ButtonStep.Open, onChange: (ButtonStep) -> Unit, onPickPage: () -> Unit) {
    val env = LocalButtonEditorEnv.current
    // Si sceglie fra pagina e link; il link resta finché non si sceglie una pagina.
    var linkMode by remember { mutableStateOf(step.pageId == null && step.url.isNotBlank()) }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TagChoice(ButtonStrings.page, null, !linkMode) { linkMode = false }
        TagChoice(ButtonStrings.link, null, linkMode) {
            linkMode = true
            if (step.pageId != null) onChange(step.copy(pageId = null))
        }
    }
    Spacer(modifier = Modifier.size(8.dp))
    if (linkMode) {
        PlainField(step.url, { onChange(step.copy(url = it.trim(), pageId = null)) }, "https://…", boxed = true, keyboardType = KeyboardType.Uri)
    } else {
        val page by produceState<PageEntity?>(null, step.pageId, env) {
            value = step.pageId?.let { env?.pages?.getPage(it) }
        }
        Chip(
            text = page?.title?.ifBlank { Strings.untitled } ?: ButtonStrings.choosePage,
            leading = page?.let { p -> { PageIconView(emoji = p.icon, iconImage = p.iconImage, isDatabase = p.isDatabase, size = 16.dp, iconSymbol = p.iconSymbol) } },
            onClick = onPickPage
        )
    }
}

// --- I pezzi della finestra ---

/** Un campo di testo senza bordi, col suggerimento grigio; [boxed]: in un riquadro più scuro. */
@Composable
fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    boxed: Boolean = false,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Box(
        modifier = modifier
            .let { if (boxed) it.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(DarkSheet).padding(horizontal = 10.dp, vertical = 9.dp) else it }
    ) {
        if (value.isEmpty()) {
            Text(hint, color = NotionGray400, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        AutoCaps {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = NotionWhite),
                cursorBrush = SolidColor(NotionWhite),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Una pastiglia che si tocca, con la freccetta: apre un menu o una scelta. */
@Composable
fun Chip(text: String, leading: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSheet)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(modifier = Modifier.size(6.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = NotionWhite, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(20.dp))
    }
}

/** Una pastiglia col suo menu: ogni voce è il testo e cosa fa. */
@Composable
fun MenuChip(text: String, items: List<Pair<String, () -> Unit>>) {
    var open by remember { mutableStateOf(false) }
    Box {
        Chip(text = text, onClick = { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            items.forEach { (label, action) ->
                DropdownMenuItem(text = { Text(label) }, onClick = {
                    open = false
                    action()
                })
            }
        }
    }
}

/** Un tag (o una scelta) da toccare: acceso, col bordo chiaro. */
@Composable
private fun TagChoice(label: String, color: String?, selected: Boolean, onClick: () -> Unit) {
    val background = color?.let { runCatching { androidx.compose.ui.graphics.Color(parseColorString(it)) }.getOrNull() } ?: DarkSheet
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) background else background.copy(alpha = 0.35f))
            .let { if (selected) it.background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f)) else it }
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = if (selected) "✓ $label" else label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) NotionWhite else NotionGray400
        )
    }
}

@Composable
private fun AddActionRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.size(6.dp))
        Text(label, color = NotionGray400, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RemoveIcon(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Close, contentDescription = ButtonStrings.remove, tint = NotionGray400, modifier = Modifier.size(18.dp))
    }
}
