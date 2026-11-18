package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.buttons.ButtonConfig
import com.gabriele.notionlocal.data.entity.AutomationConfig
import com.gabriele.notionlocal.data.entity.AutomationEntity
import com.gabriele.notionlocal.data.entity.AutomationTrigger
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.Repeat
import com.gabriele.notionlocal.data.entity.SelectOption
import com.gabriele.notionlocal.data.notify.ReminderAlarms
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.AutomationStrings
import com.gabriele.notionlocal.ui.i18n.ButtonStrings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalTime

// ---------------------------------------------------------------------------
// **"Automations"** nei tre puntini di un database (dal 03/11/2026, chiesto
// dall'utente: "ogni lunedì crea la pagina della settimana", "quando spunto
// Fatto, metti la data di oggi"). L'elenco delle automazioni del database,
// ognuna da accendere e spegnere, e la finestra per farne una: il **quando**
// in cima, sotto le **azioni**, le stesse dei bottoni (vedi `ButtonEditor.kt`).
// Come partono: `data/automations/Automations.kt`.
// ---------------------------------------------------------------------------

const val AUTOMATIONS_TAG = "automations"

/** Le azioni che un'automazione può fare: senza finestre, niente conferme né pagine da aprire. */
private val AUTOMATION_KINDS = listOf(StepKind.EDIT_PAGES, StepKind.ADD_PAGE, StepKind.INSERT_BLOCKS, StepKind.NOTIFY)

/** Le proprietà che, cambiando, possono far partire un'automazione: quelle che si scrivono. */
private fun DatabaseColumnEntity.canTrigger(): Boolean = when (type) {
    ColumnType.FORMULA, ColumnType.ROLLUP, ColumnType.CREATED_TIME, ColumnType.LAST_EDITED_TIME,
    ColumnType.BUTTON, ColumnType.RELATION -> false
    else -> true
}

private val selectJson = Json { ignoreUnknownKeys = true }

private fun DatabaseColumnEntity.optionLabels(): List<String> =
    runCatching { selectJson.decodeFromString<List<SelectOption>>(optionsJson) }.getOrDefault(emptyList()).map { it.label }

private fun timeText(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)

/** **Il quando a parole**, per l'elenco: "Quando Fatto diventa Spuntata", "Ogni lunedì alle 09:00". */
fun automationSummary(trigger: AutomationTrigger, columns: List<DatabaseColumnEntity>): String = when (trigger) {
    is AutomationTrigger.PageAdded -> AutomationStrings.pageAdded
    is AutomationTrigger.PropertyEdited -> {
        val column = columns.find { it.id == trigger.columnId }
        val name = column?.name ?: "?"
        if (trigger.values.isEmpty()) {
            "${AutomationStrings.propertyEdited}: $name"
        } else {
            "$name ${AutomationStrings.becomes} " + trigger.values.joinToString(", ") { valueName(column, it) }
        }
    }
    is AutomationTrigger.Schedule -> when (trigger.repeat) {
        Repeat.DAILY -> "${AutomationStrings.everyDay} ${AutomationStrings.at} ${timeText(trigger.minute)}"
        Repeat.WEEKLY -> "${AutomationStrings.everyWeek} (${trigger.days.sorted().joinToString(", ") { AutomationStrings.dayName(it) }}) " +
            "${AutomationStrings.at} ${timeText(trigger.minute)}"
        Repeat.MONTHLY -> "${AutomationStrings.everyMonth}, ${AutomationStrings.day(trigger.dayOfMonth).lowercase()} " +
            "${AutomationStrings.at} ${timeText(trigger.minute)}"
    }
}

private fun valueName(column: DatabaseColumnEntity?, value: String): String =
    if (column?.type == ColumnType.CHECKBOX) (if (value == "true") ButtonStrings.checked else ButtonStrings.unchecked) else value

/**
 * **Le automazioni di un database**: l'elenco, o la finestra di quella che
 * si sta facendo. [databaseId] è il database vero (non una sua vista).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationsSheet(
    databaseId: String,
    databaseTitle: String,
    factory: ViewModelFactory,
    onDismiss: () -> Unit
) {
    val db = factory.pageRepository.database
    val scope = rememberCoroutineScope()
    val automations by remember(databaseId) { db.automationDao().observeForDatabase(databaseId) }.collectAsState(initial = emptyList())
    val columns by remember(databaseId) { db.databaseDao().getColumnsForPage(databaseId) }.collectAsState(initial = emptyList())
    var editing by remember { mutableStateOf<AutomationEntity?>(null) }
    val env = remember(factory) { ButtonEditorEnv(factory.pageRepository, factory, factory.imageStore) }

    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        CompositionLocalProvider(LocalButtonEditorEnv provides env) {
            val current = editing
            if (current == null) {
                AutomationList(
                    title = databaseTitle,
                    automations = automations,
                    columns = columns,
                    onOpen = { editing = it },
                    onNew = { editing = AutomationEntity(databaseId = databaseId, orderIndex = automations.size, createdAt = System.currentTimeMillis()) },
                    onToggle = { automation, on ->
                        scope.launch(Dispatchers.IO) {
                            db.automationDao().setEnabled(automation.id, on)
                            ReminderAlarms.reschedule()
                        }
                    }
                )
            } else {
                AutomationEditor(
                    automation = current,
                    isNew = automations.none { it.id == current.id },
                    databaseId = databaseId,
                    columns = columns,
                    onCancel = { editing = null },
                    onSave = { saved ->
                        editing = null
                        scope.launch(Dispatchers.IO) {
                            db.automationDao().upsert(saved)
                            ReminderAlarms.reschedule()
                        }
                    },
                    onDelete = {
                        editing = null
                        scope.launch(Dispatchers.IO) {
                            db.automationDao().delete(current)
                            ReminderAlarms.reschedule()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AutomationList(
    title: String,
    automations: List<AutomationEntity>,
    columns: List<DatabaseColumnEntity>,
    onOpen: (AutomationEntity) -> Unit,
    onNew: () -> Unit,
    onToggle: (AutomationEntity, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AUTOMATIONS_TAG)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Text(AutomationStrings.automations, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(title, style = MaterialTheme.typography.bodySmall, color = NotionGray400, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.size(12.dp))
        if (automations.isEmpty()) {
            Text(AutomationStrings.noAutomations, style = MaterialTheme.typography.bodyMedium, color = NotionGray400)
            Spacer(modifier = Modifier.size(8.dp))
        }
        automations.forEach { automation ->
            val config = automation.config
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpen(automation) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Bolt, contentDescription = null, tint = if (automation.enabled) NotionWhite else NotionGray400, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        automation.name.ifBlank { automationSummary(config.trigger, columns) },
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        automationSummary(config.trigger, columns) + " · " + ButtonStrings.actionsCount(config.actions.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = NotionGray400,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    automation.lastRunAt?.let {
                        Text(AutomationStrings.lastRun(Formats.dateTime(it)), style = MaterialTheme.typography.bodySmall, color = NotionGray400)
                    }
                }
                Switch(checked = automation.enabled, onCheckedChange = { onToggle(automation, it) })
            }
        }
        Row(
            modifier = Modifier
                .padding(top = 6.dp, bottom = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onNew)
                .padding(horizontal = 6.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text(AutomationStrings.newAutomation, color = NotionGray400, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** **La finestra di un'automazione**: il nome e il quando in cima, le azioni sotto (quelle dei bottoni). */
@Composable
private fun AutomationEditor(
    automation: AutomationEntity,
    isNew: Boolean,
    databaseId: String,
    columns: List<DatabaseColumnEntity>,
    onCancel: () -> Unit,
    onSave: (AutomationEntity) -> Unit,
    onDelete: () -> Unit
) {
    val config = remember(automation.id) { automation.config }
    val state = remember(automation.id) { ButtonEditorState(ButtonConfig(name = automation.name, steps = config.actions)) }
    var trigger by remember(automation.id) { mutableStateOf(config.trigger) }
    val triggerComplete = when (val current = trigger) {
        is AutomationTrigger.PropertyEdited -> current.columnId != null && columns.any { it.id == current.columnId }
        is AutomationTrigger.Schedule -> current.repeat != Repeat.WEEKLY || current.days.isNotEmpty()
        is AutomationTrigger.PageAdded -> true
    }
    ButtonEditorContent(
        state = state,
        inDatabase = true,
        thisPageDatabaseId = databaseId,
        linked = false,
        onCancel = onCancel,
        onDone = {
            onSave(
                automation.copy(
                    name = state.draft.name.trim(),
                    configJson = AutomationConfig.encode(AutomationConfig(trigger, state.draft.steps))
                )
            )
        },
        modifier = Modifier.fillMaxHeight(0.92f).navigationBarsPadding().imePadding(),
        title = if (isNew) AutomationStrings.newAutomation else AutomationStrings.editAutomation,
        header = {
            AutomationHeader(
                name = state.draft.name,
                onName = { state.draft = state.draft.copy(name = it) },
                trigger = trigger,
                onTrigger = { trigger = it },
                columns = columns,
                problem = when (val current = trigger) {
                    is AutomationTrigger.PropertyEdited -> AutomationStrings.choosePropertyFirst.takeIf { !triggerComplete }
                    is AutomationTrigger.Schedule -> AutomationStrings.chooseDays.takeIf { !triggerComplete && current.repeat == Repeat.WEEKLY }
                    else -> null
                },
                onDelete = if (isNew) null else onDelete
            )
        },
        headerComplete = triggerComplete,
        actionsLabel = AutomationStrings.thenLabel,
        kinds = AUTOMATION_KINDS,
        newPageDatabaseId = databaseId,
        canOpenAfter = false
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AutomationHeader(
    name: String,
    onName: (String) -> Unit,
    trigger: AutomationTrigger,
    onTrigger: (AutomationTrigger) -> Unit,
    columns: List<DatabaseColumnEntity>,
    problem: String?,
    onDelete: (() -> Unit)?
) {
    var pickingTime by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        PlainField(
            value = name,
            onValueChange = { onName(it.replace("\n", " ")) },
            hint = AutomationStrings.nameHint,
            boxed = true
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(AutomationStrings.whenLabel, style = MaterialTheme.typography.labelMedium, color = NotionGray400)
        val schedule = trigger as? AutomationTrigger.Schedule
        MenuChip(
            text = when (trigger) {
                is AutomationTrigger.PageAdded -> AutomationStrings.pageAdded
                is AutomationTrigger.PropertyEdited -> AutomationStrings.propertyEdited
                is AutomationTrigger.Schedule -> when (trigger.repeat) {
                    Repeat.DAILY -> AutomationStrings.everyDay
                    Repeat.WEEKLY -> AutomationStrings.everyWeek
                    Repeat.MONTHLY -> AutomationStrings.everyMonth
                }
            },
            items = listOf(
                AutomationStrings.propertyEdited to { if (trigger !is AutomationTrigger.PropertyEdited) onTrigger(AutomationTrigger.PropertyEdited()) },
                AutomationStrings.pageAdded to { onTrigger(AutomationTrigger.PageAdded()) },
                AutomationStrings.everyDay to { onTrigger((schedule ?: AutomationTrigger.Schedule()).copy(repeat = Repeat.DAILY)) },
                AutomationStrings.everyWeek to { onTrigger((schedule ?: AutomationTrigger.Schedule()).copy(repeat = Repeat.WEEKLY)) },
                AutomationStrings.everyMonth to { onTrigger((schedule ?: AutomationTrigger.Schedule()).copy(repeat = Repeat.MONTHLY)) }
            )
        )
        when (trigger) {
            is AutomationTrigger.PropertyEdited -> {
                val usable = columns.filter { it.canTrigger() }
                val column = usable.find { it.id == trigger.columnId }
                MenuChip(
                    text = column?.name ?: ButtonStrings.chooseProperty,
                    items = usable.map { c -> c.name to { onTrigger(AutomationTrigger.PropertyEdited(c.id)) } }
                )
                if (column != null) {
                    when (column.type) {
                        ColumnType.CHECKBOX -> MenuChip(
                            text = when (trigger.values.firstOrNull()) {
                                "true" -> "${AutomationStrings.becomes} ${ButtonStrings.checked}"
                                "false" -> "${AutomationStrings.becomes} ${ButtonStrings.unchecked}"
                                else -> AutomationStrings.anyChange
                            },
                            items = listOf(
                                "${AutomationStrings.becomes} ${ButtonStrings.checked}" to { onTrigger(trigger.copy(values = listOf("true"))) },
                                "${AutomationStrings.becomes} ${ButtonStrings.unchecked}" to { onTrigger(trigger.copy(values = listOf("false"))) },
                                AutomationStrings.anyChange to { onTrigger(trigger.copy(values = emptyList())) }
                            )
                        )
                        ColumnType.SELECT, ColumnType.MULTI_SELECT -> {
                            Text(
                                if (trigger.values.isEmpty()) AutomationStrings.anyChange else AutomationStrings.becomes,
                                style = MaterialTheme.typography.bodySmall,
                                color = NotionGray400,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                column.optionLabels().forEach { label ->
                                    val on = label in trigger.values
                                    ToggleChip(label, on) {
                                        onTrigger(trigger.copy(values = if (on) trigger.values - label else trigger.values + label))
                                    }
                                }
                            }
                        }
                        else -> Text(AutomationStrings.anyChange, style = MaterialTheme.typography.bodySmall, color = NotionGray400, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
            is AutomationTrigger.Schedule -> {
                when (trigger.repeat) {
                    Repeat.WEEKLY -> FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..7).forEach { day ->
                            val on = day in trigger.days
                            ToggleChip(AutomationStrings.dayName(day, short = true), on) {
                                onTrigger(trigger.copy(days = if (on) trigger.days - day else (trigger.days + day).sorted()))
                            }
                        }
                    }
                    Repeat.MONTHLY -> MenuChip(
                        text = AutomationStrings.day(trigger.dayOfMonth),
                        items = (1..31).map { n -> AutomationStrings.day(n) to { onTrigger(trigger.copy(dayOfMonth = n)) } }
                    )
                    Repeat.DAILY -> Unit
                }
                Chip(text = "${AutomationStrings.at} ${timeText(trigger.minute)}", onClick = { pickingTime = true })
                if (pickingTime) {
                    SamsungTimeDialog(
                        initial = LocalTime.of(trigger.minute / 60, trigger.minute % 60),
                        onPick = { time -> onTrigger(trigger.copy(minute = time.hour * 60 + time.minute)) },
                        onDismiss = { pickingTime = false }
                    )
                }
            }
            is AutomationTrigger.PageAdded -> Unit
        }
        if (problem != null) {
            Text(problem, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp))
        }
        if (onDelete != null) {
            TextButton(onClick = onDelete, modifier = Modifier.padding(top = 6.dp)) {
                Text(AutomationStrings.deleteAutomation, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ToggleChip(text: String, on: Boolean, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (on) MaterialTheme.colorScheme.onPrimary else NotionWhite,
        modifier = Modifier
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}
