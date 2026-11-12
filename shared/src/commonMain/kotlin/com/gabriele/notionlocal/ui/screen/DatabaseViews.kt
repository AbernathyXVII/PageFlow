package com.gabriele.notionlocal.ui.screen

import androidx.compose.material.icons.filled.Tune
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkSheet

/**
 * **Le viste di un database** (dal 29/09/2026, come su Notion). Una vista è
 * un modo di guardare lo stesso database: layout, filtro, ordinamento,
 * gruppi, colonne nascoste e grafico suoi. Ogni database ha la sua di
 * partenza (la pagina stessa) e quelle aggiunte col "+" accanto al nome della
 * vista (vedi `PageEntity.viewHostId`).
 */
class DatabaseView(
    val id: String,
    val name: String,
    val layout: DatabaseLayout,
    /**
     * Se è una vista in più. Quella di partenza è il database stesso: dal
     * 08/10/2026 si toglie anche lei, prendendo le impostazioni della vista
     * dopo (vedi `PageRepository.deleteStartingView`).
     */
    val removable: Boolean,
    /** Se il nome è suo: senza, è quello del layout e lo segue. */
    val named: Boolean,
    /**
     * Se si può togliere (dal 21/10/2026): quella di partenza solo se ne resta
     * un'altra con le stesse righe, che prende il suo posto.
     */
    val deletable: Boolean = true
)

/** Il nome di una vista: il suo, o quello del suo layout. */
fun PageEntity.viewDisplayName(): String =
    viewName ?: (databaseLayout ?: DatabaseLayout.TABLE).displayName()

/** La vista di partenza (il database) e poi le altre, nell'ordine in cui sono nate. */
fun databaseViews(host: PageEntity, extra: List<PageEntity>): List<DatabaseView> {
    val own = host.sourceDatabaseId ?: host.id
    return listOf(host.toView(removable = false, deletable = extra.any { it.sourceDatabaseId == own })) +
        extra.map { it.toView(removable = true) }
}

private fun PageEntity.toView(removable: Boolean, deletable: Boolean = true) =
    DatabaseView(id, viewDisplayName(), databaseLayout ?: DatabaseLayout.TABLE, removable, named = viewName != null, deletable = deletable)

/**
 * **Toccando il nome della vista**: l'elenco delle viste, per passare da una
 * all'altra. Il layout della vista di adesso si cambia dalle impostazioni
 * ("Layout"). Dai tre puntini di una vista: **"Edit view"** (dal 08/10/2026,
 * la si mette davanti e si aprono le sue impostazioni), "Rename" e "Delete
 * view" — anche per quella di partenza, purché ne resti un'altra. In fondo
 * se ne aggiunge una. Con la vista bloccata ("Lock view") si guarda soltanto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewsSheet(
    views: List<DatabaseView>,
    activeId: String,
    canEdit: Boolean,
    onPick: (String) -> Unit,
    onEdit: (DatabaseView) -> Unit,
    onRename: (DatabaseView) -> Unit,
    onDelete: (DatabaseView) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            Text(
                DbStrings.views,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            SheetGroup {
                views.forEachIndexed { index, view ->
                    if (index > 0) HorizontalDivider()
                    ViewRow(
                        view = view,
                        active = view.id == activeId,
                        canEdit = canEdit,
                        // L'ultima vista che resta non si toglie.
                        canDelete = views.size > 1 && view.deletable,
                        onPick = { onPick(view.id) },
                        onEdit = { onEdit(view) },
                        onRename = { onRename(view) },
                        onDelete = { onDelete(view) }
                    )
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
            SheetGroup {
                SettingsRow(
                    icon = Icons.Filled.Add,
                    label = DbStrings.addView,
                    value = null,
                    enabled = canEdit,
                    onClick = onAdd
                )
            }
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ViewRow(
    view: DatabaseView,
    active: Boolean,
    canEdit: Boolean,
    canDelete: Boolean,
    onPick: () -> Unit,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            view.layout.icon(),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.size(14.dp))
        Text(
            view.name,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (active) {
            Icon(
                Icons.Filled.Check,
                contentDescription = DbStrings.selected,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }, enabled = canEdit) {
                Icon(
                    Icons.Filled.MoreHoriz,
                    contentDescription = DbStrings.viewOptions,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (canEdit) 1f else 0.35f)
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(DbStrings.editView) },
                    leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text(EditorStrings.rename) },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onRename()
                    }
                )
                if (canDelete) {
                    DropdownMenuItem(
                        text = { Text(DbStrings.deleteView, color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

/**
 * **"+" accanto al nome della vista: una vista nuova**, come la finestra
 * "New view" di Notion — il nome e il layout; "Done" la crea e la mette
 * davanti. Le altre impostazioni partono da zero e si cambiano dopo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewViewSheet(
    /** Il database delle righe di questa vista di partenza: la sorgente di suo. */
    ownSource: String,
    /** I database fra cui scegliere la sorgente (vedi DatabaseViewModel.loadDatabases). */
    loadDatabases: ((List<PageRepository.LinkableDatabase>) -> Unit) -> Unit,
    /** Nome, layout e, per le righe di un altro database, il suo id. */
    onCreate: (String, DatabaseLayout, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var layout by remember { mutableStateOf(DatabaseLayout.TABLE) }
    // **"Data source"** (dal 21/10/2026, come su Notion): di suo le righe di
    // questo database; toccando la voce, l'elenco degli altri.
    var source by remember { mutableStateOf(ownSource) }
    var choosingSource by remember { mutableStateOf(false) }
    var databases by remember { mutableStateOf<List<PageRepository.LinkableDatabase>?>(null) }
    LaunchedEffect(Unit) { loadDatabases { databases = it } }
    val sourceName = databases?.find { it.page.id == source }?.page?.title?.ifBlank { null } ?: Strings.untitled
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    DbStrings.newView,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss) { Text(Strings.cancel) }
                TextButton(onClick = { onCreate(name, layout, source.takeIf { it != ownSource }) }) { Text(Strings.done) }
            }
            Spacer(modifier = Modifier.size(8.dp))
            AutoCaps {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.replace("\n", "") },
                    singleLine = true,
                    placeholder = { Text(DbStrings.viewName) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            SheetGroup {
                SettingsRow(
                    icon = Icons.Filled.TableChart,
                    label = DbStrings.dataSource,
                    value = if (databases == null) null else sourceName,
                    onClick = { choosingSource = !choosingSource }
                )
                if (choosingSource) {
                    databases?.forEach { item ->
                        HorizontalDivider()
                        DatabaseChoiceRow(
                            title = item.page.title.ifBlank { Strings.untitled },
                            place = item.place,
                            selected = item.page.id == source,
                            onClick = {
                                source = item.page.id
                                choosingSource = false
                            }
                        )
                    }
                }
            }
            Text(
                DbStrings.dataSourceHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 12.dp)
            )
            // Gli stessi riquadri di "Layout" nelle impostazioni.
            DatabaseLayout.entries.chunked(3).forEach { rowLayouts ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowLayouts.forEach { candidate ->
                        LayoutChoice(
                            layout = candidate,
                            isSelected = candidate == layout,
                            onClick = { layout = candidate },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - rowLayouts.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

/**
 * Il nuovo nome di una vista; vuoto, torna quello del suo layout. Lasciato
 * com'era su una vista senza nome suo, resta senza: il nome continua a
 * seguire il layout.
 */
@Composable
fun RenameViewDialog(
    current: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(TextFieldValue(current, TextRange(0, current.length))) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val confirm = { onConfirm(value.text) }
    FitAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(EditorStrings.rename) },
        text = {
            AutoCaps {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.copy(text = it.text.replace("\n", "")) },
                    singleLine = true,
                    placeholder = { Text(DbStrings.viewName) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { confirm() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
        },
        confirmButton = { TextButton(onClick = confirm) { Text(Strings.done) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}

/** Prima di togliere una vista: le sue impostazioni non tornano. */
@Composable
fun DeleteViewDialog(
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    FitAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${DbStrings.deleteView} “$name”") },
        text = { Text(DbStrings.deleteViewMessage) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(Strings.delete, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}
