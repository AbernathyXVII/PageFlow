package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.platform.isPc
import com.gabriele.notionlocal.ui.theme.pcOr
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.NotebookColors
import com.gabriele.notionlocal.data.entity.NotebookEntity
import com.gabriele.notionlocal.data.entity.NotebookSectionEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.security.AppLock
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.ui.i18n.NotebookStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.DarkSurfaceVariant
import com.gabriele.notionlocal.ui.theme.NotebookBand
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.viewmodel.NotebookSectionViewModel
import com.gabriele.notionlocal.viewmodel.NotebooksViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.platform.rememberImagePicker

// ---------------------------------------------------------------------------
// **I quaderni** (dal 26/09/2026), come quelli di OneNote: quaderni, gruppi
// di sezioni che si aprono e chiudono come i toggle, sezioni colorate e
// dentro le sezioni le pagine. Vivono nella barra laterale, sotto la voce
// "Notebooks", e sono **un mondo a parte** rispetto al menu principale.
// ---------------------------------------------------------------------------

/** Il quaderno accanto al nome: l'immagine scelta dall'utente, o quello disegnato nel suo colore. */
@Composable
fun NotebookIcon(notebook: NotebookEntity, store: PageImageStore, size: Dp = NOTEBOOK_ICON) {
    val image = notebook.iconImage
    if (image != null) {
        PageImage(
            fileName = image,
            store = store,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(4.dp))
        )
    } else {
        DrawnNotebook(Color(notebook.color), Modifier.size(size))
    }
}

/**
 * **Il quaderno disegnato**, come l'icona di OneNote: la copertina nel
 * colore scelto, il dorso più scuro, l'etichetta chiara e gli anelli sul
 * bordo destro. Disegnato e non un'immagine, così prende qualunque colore.
 */
@Composable
fun DrawnNotebook(color: Color, modifier: Modifier = Modifier) {
    val dark = color.scaled(0.72f)
    Canvas(modifier) {
        val u = size.width / 24f
        drawRoundRect(color, Offset(3.5f * u, 2f * u), Size(15.5f * u, 20f * u), CornerRadius(2.4f * u))
        drawRect(dark, Offset(6f * u, 2f * u), Size(1.6f * u, 20f * u))
        drawRoundRect(Color.White.copy(alpha = 0.9f), Offset(9.4f * u, 5.4f * u), Size(6.6f * u, 3.2f * u), CornerRadius(0.8f * u))
        for (y in listOf(5.5f, 10.9f, 16.3f)) {
            drawRoundRect(dark, Offset(18.2f * u, y * u), Size(2.8f * u, 2.2f * u), CornerRadius(1.1f * u))
        }
    }
}

private fun Color.scaled(factor: Float) = Color(red * factor, green * factor, blue * factor, alpha)

/** La linguetta colorata di una sezione, come in OneNote. */
@Composable
fun SectionTab(color: Int, modifier: Modifier = Modifier, height: Dp = 18.dp) {
    Box(
        modifier = modifier
            .size(width = 6.dp, height = height)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(color))
    )
}

// --- La barra laterale ------------------------------------------------------

/** Una riga dell'albero delle sezioni di un quaderno, con quanto è rientrata. */
private class SectionRowItem(val section: NotebookSectionEntity, val depth: Int, val isEmptyGroup: Boolean)

/** Le sezioni e i gruppi di un quaderno messi in fila, dentro i gruppi aperti. */
private fun flattenSections(sections: List<NotebookSectionEntity>): List<SectionRowItem> {
    val byParent = sections.groupBy { it.parentId }
    val rows = mutableListOf<SectionRowItem>()
    fun visit(parentId: String?, depth: Int, seen: Set<String>) {
        byParent[parentId].orEmpty().sortedBy { it.orderIndex }.forEach { section ->
            if (section.id in seen) return@forEach
            val children = byParent[section.id].orEmpty()
            rows += SectionRowItem(section, depth, section.isGroup && section.isExpanded && children.isEmpty())
            if (section.isGroup && section.isExpanded) visit(section.id, depth + 1, seen + section.id)
        }
    }
    visit(null, 1, emptySet())
    return rows
}

/**
 * **La voce "Notebooks" della barra laterale**: la fascia col titolo — la
 * freccetta la apre e la chiude, il + crea un quaderno — e sotto i quaderni,
 * come nella colonna di sinistra di OneNote. Toccando un quaderno o un
 * gruppo si apre e si chiude; toccando una sezione si va alle sue pagine.
 * **Tenendo premuto** si apre il menu della voce (rinomina, colore, sezioni
 * nuove, password, su e giù, elimina).
 */
@Composable
fun NotebooksSidebarSection(
    viewModel: NotebooksViewModel,
    store: PageImageStore,
    currentSectionId: String?,
    onOpenSection: (String) -> Unit,
    /** Il quaderno aperto adesso a schermo intero, se ce n'è uno: si evidenzia. */
    currentNotebookId: String? = null,
    /** Toccando il nome di un quaderno: la sua pagina, con tutte le sezioni. */
    onOpenNotebook: (String) -> Unit = {}
) {
    val notebooks by viewModel.notebooks.collectAsStateWithLifecycle()
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val menus = remember { NotebookMenus() }
    val collapsed = AppSettings.notebooksCollapsed
    val currentSectionNotebookId = sections.firstOrNull { it.id == currentSectionId }?.notebookId

    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(NotebookBand)
            .clickable { AppSettings.notebooksCollapsed = !collapsed }
            .padding(start = 10.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (collapsed) Icons.Filled.KeyboardArrowRight else Icons.Filled.KeyboardArrowDown,
            contentDescription = if (collapsed) Strings.expand else Strings.collapse,
            tint = NotionGray400,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.Filled.MenuBook, contentDescription = null, tint = NotionWhite, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            NotebookStrings.notebooks,
            color = NotionWhite,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        // Sul PC piccolo come i "+" della barra di Notion (vedi `pcOr`).
        IconButton(onClick = { menus.dialog = NotebookDialog.NewNotebook }, modifier = if (isPc) Modifier.size(28.dp) else Modifier) {
            Icon(Icons.Filled.Add, contentDescription = NotebookStrings.newNotebook, tint = NotionWhite, modifier = if (isPc) Modifier.size(18.dp) else Modifier)
        }
    }

    if (!collapsed) {
        Spacer(modifier = Modifier.height(4.dp))
        if (notebooks.isEmpty()) {
            AddRow(NotebookStrings.newNotebook, depth = 0) { menus.dialog = NotebookDialog.NewNotebook }
        }
        notebooks.forEach { notebook ->
            NotebookRow(
                notebook = notebook,
                store = store,
                // Chiuso, il quaderno dice lui dove si è: la sezione aperta non si vede.
                isCurrent = notebook.id == currentNotebookId ||
                    (notebook.id == currentSectionNotebookId && !notebook.isExpanded),
                // **Il nome apre la pagina del quaderno, la freccetta apre e
                // chiude l'albero qui** (dal 26/09/2026, chiesto dall'utente).
                onClick = { onOpenNotebook(notebook.id) },
                onToggle = { viewModel.toggleNotebook(notebook) },
                onLongPress = { menus.sheet = NotebookMenuTarget.OfNotebook(notebook) }
            )
            if (notebook.isExpanded) {
                flattenSections(sections.filter { it.notebookId == notebook.id }).forEach { row ->
                    val section = row.section
                    if (section.isGroup) {
                        GroupRow(
                            section = section,
                            depth = row.depth,
                            onClick = { viewModel.toggleGroup(section) },
                            onLongPress = { menus.sheet = NotebookMenuTarget.OfSection(section) }
                        )
                        if (row.isEmptyGroup) {
                            Text(
                                NotebookStrings.emptyGroup,
                                color = NotionGray400,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(
                                    start = rowStart(row.depth + 1) + MARKER_WIDTH + MARKER_GAP,
                                    top = 2.dp,
                                    bottom = 6.dp
                                )
                            )
                        }
                    } else {
                        SectionRow(
                            section = section,
                            depth = row.depth,
                            isCurrent = section.id == currentSectionId,
                            onClick = { onOpenSection(section.id) },
                            onLongPress = { menus.sheet = NotebookMenuTarget.OfSection(section) }
                        )
                    }
                }
                AddRow(NotebookStrings.newSection, depth = 1) {
                    menus.dialog = NotebookDialog.NewSection(notebook.id, parentId = null, isGroup = false)
                }
            }
        }
    }

    NotebookMenuHost(
        menus = menus,
        viewModel = viewModel,
        store = store,
        sections = sections,
        onOpenSection = onOpenSection
    )
}

private val NOTEBOOK_ICON = 24.dp
private val ROW_START = 20.dp
private val ROW_INDENT = 22.dp
// Larga abbastanza da toccarla col dito: nella riga di un quaderno è un
// pulsante a sé (apre e chiude), accanto al nome che apre la pagina.
private val MARKER_WIDTH = 28.dp
private val MARKER_GAP = 6.dp

private fun rowStart(depth: Int): Dp = ROW_START + ROW_INDENT * depth

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TreeLine(
    depth: Int,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    marker: @Composable () -> Unit,
    /** La freccetta si tocca da sola: apre e chiude, mentre il resto della riga fa `onClick`. */
    onMarkerClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isCurrent) DarkSurfaceVariant else Color.Transparent)
            .onRightClick(onLongPress)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(start = rowStart(depth) - 8.dp, end = 8.dp)
            .height(pcOr(28.dp, 40.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(MARKER_WIDTH)
                .fillMaxHeight()
                .then(
                    if (onMarkerClick != null) {
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onMarkerClick)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) { marker() }
        Spacer(modifier = Modifier.width(MARKER_GAP))
        content()
    }
}

@Composable
private fun NotebookRow(
    notebook: NotebookEntity,
    store: PageImageStore,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onLongPress: () -> Unit
) {
    TreeLine(
        depth = 0,
        isCurrent = isCurrent,
        onClick = onClick,
        onLongPress = onLongPress,
        onMarkerClick = onToggle,
        marker = {
            Icon(
                if (notebook.isExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                contentDescription = if (notebook.isExpanded) Strings.collapse else Strings.expand,
                tint = NotionGray400,
                modifier = Modifier.size(20.dp)
            )
        }
    ) {
        NotebookIcon(notebook, store)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            notebook.title.ifBlank { Strings.untitled },
            color = NotionWhite,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GroupRow(section: NotebookSectionEntity, depth: Int, onClick: () -> Unit, onLongPress: () -> Unit) {
    TreeLine(
        depth = depth,
        isCurrent = false,
        onClick = onClick,
        onLongPress = onLongPress,
        marker = {
            Icon(
                if (section.isExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                contentDescription = if (section.isExpanded) Strings.collapse else Strings.expand,
                tint = NotionGray400,
                modifier = Modifier.size(20.dp)
            )
        }
    ) {
        Text(
            section.title.ifBlank { Strings.untitled },
            color = NotionWhite,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionRow(
    section: NotebookSectionEntity,
    depth: Int,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    TreeLine(
        depth = depth,
        isCurrent = isCurrent,
        onClick = onClick,
        onLongPress = onLongPress,
        marker = { SectionTab(section.color) }
    ) {
        Text(
            section.title.ifBlank { Strings.untitled },
            color = NotionWhite,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (section.isProtected) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.Filled.Lock, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(14.dp))
        }
    }
}

/** "+ New section" in fondo a un quaderno aperto, come in OneNote. */
@Composable
private fun AddRow(label: String, depth: Int, onClick: () -> Unit) {
    TreeLine(
        depth = depth,
        isCurrent = false,
        onClick = onClick,
        onLongPress = onClick,
        marker = { Icon(Icons.Filled.Add, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(18.dp)) }
    ) {
        Text(label, color = NotionGray400, style = MaterialTheme.typography.bodyMedium)
    }
}

// --- I menu: tenendo premuto, e le finestre che aprono ------------------------

sealed class NotebookMenuTarget {
    data class OfNotebook(val notebook: NotebookEntity) : NotebookMenuTarget()
    data class OfSection(val section: NotebookSectionEntity) : NotebookMenuTarget()
}

sealed class NotebookDialog {
    object NewNotebook : NotebookDialog()
    data class NewSection(val notebookId: String, val parentId: String?, val isGroup: Boolean) : NotebookDialog()
    data class RenameNotebook(val notebook: NotebookEntity) : NotebookDialog()
    data class RenameSection(val section: NotebookSectionEntity) : NotebookDialog()
    data class NotebookColor(val notebook: NotebookEntity) : NotebookDialog()
    data class SectionColor(val section: NotebookSectionEntity) : NotebookDialog()
    data class NotebookIconChoice(val notebook: NotebookEntity) : NotebookDialog()
    data class DeleteNotebook(val notebook: NotebookEntity, val pages: Int) : NotebookDialog()
    data class DeleteSection(val section: NotebookSectionEntity, val pages: Int) : NotebookDialog()
    data class Protect(val section: NotebookSectionEntity, val wanted: Boolean) : NotebookDialog()
    /** Prima impronta o password, poi `then`: per eliminare quello che è protetto. */
    data class ConfirmIdentity(val then: NotebookDialog) : NotebookDialog()
}

/**
 * **Eliminare una sezione protetta chiede prima impronta o password** (anche
 * dentro un gruppo o un quaderno): chi non la può aprire non la deve
 * nemmeno poter cancellare. Se è già aperta, no.
 */
private fun needsIdentity(sections: List<NotebookSectionEntity>, rootIds: Set<String>, notebookId: String?): Boolean {
    val inside = mutableSetOf<String>().apply { addAll(rootIds) }
    if (notebookId != null) inside += sections.filter { it.notebookId == notebookId }.map { it.id }
    var grew = true
    while (grew) grew = inside.addAll(sections.filter { it.parentId in inside }.map { it.id })
    return sections.any { it.id in inside && it.isProtected && !AppLock.isUnlocked(it.id) }
}

/** Quale menu e quale finestra dei quaderni sono aperti: uno per chi li mostra. */
@Stable
class NotebookMenus {
    var sheet by mutableStateOf<NotebookMenuTarget?>(null)
    var dialog by mutableStateOf<NotebookDialog?>(null)
}

/**
 * Il menu di un quaderno o di una sezione, e le finestre che ne escono. Lo
 * usano la barra laterale e la schermata di una sezione (i tre puntini).
 * `onSectionDeleted`: la sezione aperta se n'è andata, e chi la mostrava
 * deve tornare indietro.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookMenuHost(
    menus: NotebookMenus,
    viewModel: NotebooksViewModel,
    store: PageImageStore,
    sections: List<NotebookSectionEntity>,
    onOpenSection: (String) -> Unit,
    onSectionDeleted: (String) -> Unit = {},
    onNotebookDeleted: (String) -> Unit = {}
) {
    menus.sheet?.let { target ->
        NoteBottomSheet(
            onDismissRequest = { menus.sheet = null },
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
                when (target) {
                    is NotebookMenuTarget.OfNotebook -> NotebookMenu(target.notebook, store, menus, viewModel)
                    is NotebookMenuTarget.OfSection -> SectionMenu(target.section, menus, viewModel)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    fun nextName(notebookId: String, isGroup: Boolean): String {
        val count = sections.count { it.notebookId == notebookId && it.isGroup == isGroup } + 1
        return if (isGroup) NotebookStrings.defaultGroupName(count) else NotebookStrings.defaultSectionName(count)
    }

    when (val dialog = menus.dialog) {
        null -> Unit
        NotebookDialog.NewNotebook -> {
            var color by remember { mutableStateOf(NotebookColors.DEFAULT_NOTEBOOK) }
            NameDialog(
                title = NotebookStrings.newNotebook,
                initial = NotebookStrings.defaultNotebookName,
                confirmLabel = NotebookStrings.create,
                extra = {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(NotebookStrings.color, color = NotionGray400, style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    ColorSwatches(selected = color, onPick = { color = it })
                },
                onConfirm = { name ->
                    viewModel.createNotebook(name, color, NotebookStrings.defaultSectionName(1))
                    menus.dialog = null
                },
                onDismiss = { menus.dialog = null }
            )
        }
        is NotebookDialog.NewSection -> NameDialog(
            title = if (dialog.isGroup) NotebookStrings.newSectionGroup else NotebookStrings.newSection,
            initial = nextName(dialog.notebookId, dialog.isGroup),
            confirmLabel = NotebookStrings.create,
            onConfirm = { name ->
                viewModel.createSection(dialog.notebookId, dialog.parentId, name, dialog.isGroup) { created ->
                    // Una sezione nuova si apre subito, come in OneNote: è lì che si vuole scrivere.
                    if (!created.isGroup) onOpenSection(created.id)
                }
                menus.dialog = null
            },
            onDismiss = { menus.dialog = null }
        )
        is NotebookDialog.RenameNotebook -> NameDialog(
            title = NotebookStrings.rename,
            initial = dialog.notebook.title,
            confirmLabel = Strings.done,
            onConfirm = { name ->
                viewModel.renameNotebook(dialog.notebook.id, name)
                menus.dialog = null
            },
            onDismiss = { menus.dialog = null }
        )
        is NotebookDialog.RenameSection -> NameDialog(
            title = NotebookStrings.rename,
            initial = dialog.section.title,
            confirmLabel = Strings.done,
            onConfirm = { name ->
                viewModel.renameSection(dialog.section.id, name)
                menus.dialog = null
            },
            onDismiss = { menus.dialog = null }
        )
        is NotebookDialog.NotebookColor -> ColorDialog(
            title = NotebookStrings.notebookColor,
            selected = dialog.notebook.color,
            onPick = {
                viewModel.setNotebookColor(dialog.notebook.id, it)
                menus.dialog = null
            },
            onDismiss = { menus.dialog = null }
        )
        is NotebookDialog.SectionColor -> ColorDialog(
            title = NotebookStrings.sectionColor,
            selected = dialog.section.color,
            onPick = {
                viewModel.setSectionColor(dialog.section.id, it)
                menus.dialog = null
            },
            onDismiss = { menus.dialog = null }
        )
        is NotebookDialog.NotebookIconChoice -> NotebookIconSheet(
            notebook = dialog.notebook,
            store = store,
            onPicked = { viewModel.setNotebookIcon(dialog.notebook.id, it) },
            onDismiss = { menus.dialog = null }
        )
        is NotebookDialog.DeleteNotebook -> DeleteForeverDialog(
            title = NotebookStrings.deleteNotebookTitle,
            text = NotebookStrings.deleteContentsText(dialog.pages),
            onConfirm = {
                viewModel.deleteNotebook(dialog.notebook.id)
                sections.filter { it.notebookId == dialog.notebook.id }.forEach { onSectionDeleted(it.id) }
                onNotebookDeleted(dialog.notebook.id)
                menus.dialog = null
            },
            onDismiss = { menus.dialog = null }
        )
        is NotebookDialog.DeleteSection -> DeleteForeverDialog(
            title = if (dialog.section.isGroup) NotebookStrings.deleteGroupTitle else NotebookStrings.deleteSectionTitle,
            text = NotebookStrings.deleteContentsText(dialog.pages),
            onConfirm = {
                viewModel.deleteSection(dialog.section.id)
                // Anche le sezioni dentro un gruppo: se ne va tutto il ramo.
                val gone = mutableSetOf(dialog.section.id)
                var grew = true
                while (grew) {
                    val more = sections.filter { it.parentId in gone && it.id !in gone }.map { it.id }
                    grew = gone.addAll(more)
                }
                gone.forEach(onSectionDeleted)
                menus.dialog = null
            },
            onDismiss = { menus.dialog = null }
        )
        is NotebookDialog.Protect -> PasswordLockFlow(
            wanted = dialog.wanted,
            onFinished = { menus.dialog = null },
            apply = { viewModel.setSectionProtected(dialog.section.id, it) }
        )
        is NotebookDialog.ConfirmIdentity -> UnlockPrompt(
            onUnlocked = { menus.dialog = dialog.then },
            onDismiss = { menus.dialog = null }
        )
    }
}

@Composable
private fun MenuTitle(content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
        content()
    }
}

@Composable
private fun NotebookMenu(notebook: NotebookEntity, store: PageImageStore, menus: NotebookMenus, viewModel: NotebooksViewModel) {
    fun open(dialog: NotebookDialog) {
        menus.sheet = null
        menus.dialog = dialog
    }
    MenuTitle {
        NotebookIcon(notebook, store)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            notebook.title.ifBlank { Strings.untitled },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
    OptionsGroup {
        OptionRow(Icons.Filled.DriveFileRenameOutline, NotebookStrings.rename, onClick = { open(NotebookDialog.RenameNotebook(notebook)) })
        HorizontalDivider()
        OptionRow(
            Icons.Filled.Palette,
            NotebookStrings.notebookColor,
            onClick = { open(NotebookDialog.NotebookColor(notebook)) },
            trailing = { ColorDot(notebook.color) }
        )
        HorizontalDivider()
        OptionRow(Icons.Filled.Image, NotebookStrings.notebookIcon, onClick = { open(NotebookDialog.NotebookIconChoice(notebook)) })
    }
    Spacer(modifier = Modifier.height(12.dp))
    OptionsGroup {
        OptionRow(Icons.Filled.Add, NotebookStrings.newSection, onClick = {
            open(NotebookDialog.NewSection(notebook.id, parentId = null, isGroup = false))
        })
        HorizontalDivider()
        OptionRow(Icons.Filled.CreateNewFolder, NotebookStrings.newSectionGroup, onClick = {
            open(NotebookDialog.NewSection(notebook.id, parentId = null, isGroup = true))
        })
    }
    Spacer(modifier = Modifier.height(12.dp))
    OptionsGroup {
        OptionRow(Icons.Filled.ArrowUpward, NotebookStrings.moveUp, onClick = { viewModel.moveNotebook(notebook.id, up = true) })
        HorizontalDivider()
        OptionRow(Icons.Filled.ArrowDownward, NotebookStrings.moveDown, onClick = { viewModel.moveNotebook(notebook.id, up = false) })
    }
    Spacer(modifier = Modifier.height(12.dp))
    OptionsGroup {
        OptionRow(
            Icons.Filled.Delete,
            Strings.delete,
            tint = MaterialTheme.colorScheme.error,
            onClick = {
                menus.sheet = null
                viewModel.countPages(notebook.id, null) { pages ->
                    val delete = NotebookDialog.DeleteNotebook(notebook, pages)
                    menus.dialog = if (needsIdentity(viewModel.sections.value, emptySet(), notebook.id)) {
                        NotebookDialog.ConfirmIdentity(delete)
                    } else {
                        delete
                    }
                }
            }
        )
    }
}

@Composable
private fun SectionMenu(section: NotebookSectionEntity, menus: NotebookMenus, viewModel: NotebooksViewModel) {
    fun open(dialog: NotebookDialog) {
        menus.sheet = null
        menus.dialog = dialog
    }
    // Da una sezione, le sezioni nuove le nascono accanto (nel suo stesso
    // gruppo); da un gruppo, dentro il gruppo. Come in OneNote.
    val newParent = if (section.isGroup) section.id else section.parentId
    MenuTitle {
        if (!section.isGroup) {
            SectionTab(section.color, height = 22.dp)
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            section.title.ifBlank { Strings.untitled },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
    OptionsGroup {
        OptionRow(Icons.Filled.DriveFileRenameOutline, NotebookStrings.rename, onClick = { open(NotebookDialog.RenameSection(section)) })
        if (!section.isGroup) {
            HorizontalDivider()
            OptionRow(
                Icons.Filled.Palette,
                NotebookStrings.sectionColor,
                onClick = { open(NotebookDialog.SectionColor(section)) },
                trailing = { ColorDot(section.color) }
            )
            HorizontalDivider()
            OptionRow(
                Icons.Filled.Fingerprint,
                NotebookStrings.passwordProtectSection,
                onClick = { open(NotebookDialog.Protect(section, !section.isProtected)) },
                trailing = {
                    Switch(
                        checked = section.isProtected,
                        onCheckedChange = { open(NotebookDialog.Protect(section, !section.isProtected)) }
                    )
                }
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    OptionsGroup {
        OptionRow(Icons.Filled.Add, NotebookStrings.newSection, onClick = {
            open(NotebookDialog.NewSection(section.notebookId, newParent, isGroup = false))
        })
        HorizontalDivider()
        OptionRow(Icons.Filled.CreateNewFolder, NotebookStrings.newSectionGroup, onClick = {
            open(NotebookDialog.NewSection(section.notebookId, newParent, isGroup = true))
        })
    }
    Spacer(modifier = Modifier.height(12.dp))
    OptionsGroup {
        OptionRow(Icons.Filled.ArrowUpward, NotebookStrings.moveUp, onClick = { viewModel.moveSection(section.id, up = true) })
        HorizontalDivider()
        OptionRow(Icons.Filled.ArrowDownward, NotebookStrings.moveDown, onClick = { viewModel.moveSection(section.id, up = false) })
    }
    Spacer(modifier = Modifier.height(12.dp))
    OptionsGroup {
        OptionRow(
            Icons.Filled.Delete,
            Strings.delete,
            tint = MaterialTheme.colorScheme.error,
            onClick = {
                menus.sheet = null
                viewModel.countPages(null, section.id) { pages ->
                    val delete = NotebookDialog.DeleteSection(section, pages)
                    menus.dialog = if (needsIdentity(viewModel.sections.value, setOf(section.id), null)) {
                        NotebookDialog.ConfirmIdentity(delete)
                    } else {
                        delete
                    }
                }
            }
        )
    }
}

@Composable
private fun ColorDot(color: Int) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(Color(color))
    )
}

/** I colori di OneNote in due file da sei; quello scelto ha la spunta. */
@Composable
private fun ColorSwatches(selected: Int, onPick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NotebookColors.palette.chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                            .then(
                                if (color == selected) Modifier.border(2.dp, NotionWhite, CircleShape) else Modifier
                            )
                            .clickable { onPick(color) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (color == selected) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorDialog(title: String, selected: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSheet,
        title = { Text(title, color = NotionWhite) },
        text = { ColorSwatches(selected = selected, onPick = onPick) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}

/**
 * La finestra per un nome: già scritto e tutto selezionato, così si
 * sovrascrive subito o si tiene con un tocco. `extra` sta sotto il campo.
 */
@Composable
private fun NameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    extra: @Composable () -> Unit = {}
) {
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val canConfirm = value.text.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSheet,
        title = { Text(title, color = NotionWhite) },
        text = {
            Column {
                AutoCaps {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it.copy(text = it.text.replace("\n", "")) },
                        label = { Text(NotebookStrings.name) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { if (canConfirm) onConfirm(value.text) }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus)
                    )
                }
                extra()
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.text) }, enabled = canConfirm) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}

/**
 * L'icona di un quaderno: **un'immagine a scelta** dalla galleria (per
 * esempio un'icona scaricata da Flaticon), o il quaderno disegnato.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotebookIconSheet(
    notebook: NotebookEntity,
    store: PageImageStore,
    onPicked: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val pickImage = rememberImagePicker(
        store = store,
        onStart = {
            busy = true
            error = null
        }
    ) { result ->
        busy = false
        result.fold(
            onSuccess = {
                onPicked(it)
                onDismiss()
            },
            onFailure = { error = EditorStrings.imageReadFailed }
        )
    }
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            MenuTitle {
                NotebookIcon(notebook, store, size = 32.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(NotebookStrings.notebookIcon, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            OptionsGroup {
                OptionRow(
                    Icons.Filled.Image,
                    EditorStrings.upload,
                    enabled = !busy,
                    onClick = { pickImage() }
                )
                if (notebook.iconImage != null) {
                    HorizontalDivider()
                    OptionRow(
                        Icons.Filled.MenuBook,
                        NotebookStrings.useDrawnNotebook,
                        enabled = !busy,
                        onClick = {
                            onPicked(null)
                            onDismiss()
                        }
                    )
                }
            }
            error?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// --- Una sezione aperta: le sue pagine ---------------------------------------

/**
 * **Una sezione aperta**: in cima dove sta (quaderno e gruppi), il suo nome
 * con la linguetta, "Add page" e le pagine, nell'ordine — la colonna delle
 * pagine di OneNote, a schermo intero perché su un telefono tre colonne
 * non ci stanno. Toccando una pagina la si apre; tenendola premuta la si
 * sposta su o giù, o la si elimina. I tre puntini aprono il menu della
 * sezione, lo stesso della barra laterale.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotebookSectionScreen(
    sectionId: String,
    factory: ViewModelFactory,
    onBack: () -> Unit,
    onOpenSidebar: () -> Unit,
    onOpenPage: (String) -> Unit,
    onOpenSection: (String) -> Unit,
    viewModel: NotebookSectionViewModel = viewModel(factory = factory),
    menuViewModel: NotebooksViewModel = viewModel(factory = factory)
) {
    LaunchedEffect(sectionId) { viewModel.load(sectionId) }
    val section by viewModel.section.collectAsStateWithLifecycle()
    val pages by viewModel.pages.collectAsStateWithLifecycle()
    val path by viewModel.path.collectAsStateWithLifecycle()
    val sections by menuViewModel.sections.collectAsStateWithLifecycle()
    val menus = remember { NotebookMenus() }
    var pageMenuFor by remember { mutableStateOf<PageEntity?>(null) }
    var confirmDeletePage by remember { mutableStateOf<PageEntity?>(null) }
    val context = LocalPlatformContext.current
    val store = remember(context) { PageImageStore(context) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground),
                navigationIcon = {
                    Row {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = Strings.back)
                        }
                        if (!LocalSidebarPinned.current) {
                            IconButton(onClick = onOpenSidebar) {
                                Icon(Icons.Filled.Menu, contentDescription = Strings.openSidebar)
                            }
                        }
                    }
                },
                actions = {
                    section?.let { current ->
                        IconButton(onClick = { menus.sheet = NotebookMenuTarget.OfSection(current) }) {
                            Icon(Icons.Filled.MoreHoriz, contentDescription = Strings.pageOptions)
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 32.dp)
        ) {
            item(key = "header") {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    if (path.isNotEmpty()) {
                        Text(
                            path.joinToString("  ›  "),
                            color = NotionGray400,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        section?.let { SectionTab(it.color, height = 28.dp) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            section?.title?.ifBlank { Strings.untitled }.orEmpty(),
                            color = NotionWhite,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    // "Add page", come il pulsante in cima alla colonna delle pagine di OneNote.
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, NotionGray400.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .clickable { viewModel.addPage(onOpenPage) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.EditNote, contentDescription = null, tint = NotionWhite, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(NotebookStrings.addPage, color = NotionWhite, style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            if (pages.isEmpty()) {
                item(key = "empty") {
                    Text(
                        NotebookStrings.noPages,
                        color = NotionGray400,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }
            items(pages, key = { it.id }) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .onRightClick({ pageMenuFor = page })
                        .combinedClickable(
                            onClick = { onOpenPage(page.id) },
                            onLongClick = { pageMenuFor = page }
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            page.title.ifBlank { NotebookStrings.untitledPage },
                            color = if (page.title.isBlank()) NotionGray400 else NotionWhite,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (page.isProtected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(14.dp))
                        }
                    }
                    Text(
                        Formats.date(page.createdAt),
                        color = NotionGray400,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    pageMenuFor?.let { page ->
        NoteBottomSheet(
            onDismissRequest = { pageMenuFor = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = DarkSheet
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
            ) {
                MenuTitle {
                    Text(
                        page.title.ifBlank { NotebookStrings.untitledPage },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                OptionsGroup {
                    OptionRow(Icons.Filled.ArrowUpward, NotebookStrings.moveUp, onClick = { viewModel.movePage(page.id, up = true) })
                    HorizontalDivider()
                    OptionRow(Icons.Filled.ArrowDownward, NotebookStrings.moveDown, onClick = { viewModel.movePage(page.id, up = false) })
                }
                Spacer(modifier = Modifier.height(12.dp))
                OptionsGroup {
                    OptionRow(
                        Icons.Filled.Delete,
                        NotebookStrings.deletePage,
                        tint = MaterialTheme.colorScheme.error,
                        onClick = {
                            pageMenuFor = null
                            confirmDeletePage = page
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    confirmDeletePage?.let { page ->
        DeleteForeverDialog(
            title = NotebookStrings.deletePageTitle,
            text = NotebookStrings.deletePageText,
            onConfirm = {
                viewModel.deletePage(page.id)
                confirmDeletePage = null
            },
            onDismiss = { confirmDeletePage = null }
        )
    }

    NotebookMenuHost(
        menus = menus,
        viewModel = menuViewModel,
        store = store,
        sections = sections,
        onOpenSection = onOpenSection,
        onSectionDeleted = { if (it == sectionId) onBack() }
    )
}

/**
 * **Sotto il titolo di una pagina di un quaderno**, come in OneNote: una
 * riga sottile, poi il giorno e l'ora in cui la pagina è nata.
 */
@Composable
fun NotebookPageStamp(createdAt: Long) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        HorizontalDivider(color = NotionGray400.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(6.dp))
        Row {
            Text(
                Formats.weekdayAndDate(createdAt),
                color = NotionGray400,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.width(24.dp))
            Text(Formats.time(createdAt), color = NotionGray400, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// --- Un quaderno: tutte le sue sezioni ----------------------------------------

/**
 * **La pagina di un quaderno** (dal 26/09/2026, chiesta dall'utente):
 * toccandone il nome nella barra laterale si arriva qui, con **tutte** le
 * sezioni e i gruppi in una volta sola, rientrati come nell'albero. I
 * gruppi si chiudono con la freccetta (solo qui: la barra resta com'è),
 * una sezione si apre toccandola, e tenendo premuto c'è il suo menu. In
 * fondo, sezione o gruppo nuovi.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotebookScreen(
    notebookId: String,
    factory: ViewModelFactory,
    onBack: () -> Unit,
    onOpenSidebar: () -> Unit,
    onOpenSection: (String) -> Unit,
    viewModel: NotebooksViewModel = viewModel(factory = factory)
) {
    val notebooks by viewModel.notebooks.collectAsStateWithLifecycle()
    val allSections by viewModel.sections.collectAsStateWithLifecycle()
    val pageCounts by viewModel.pageCounts.collectAsStateWithLifecycle()
    val notebook = notebooks.firstOrNull { it.id == notebookId }
    val sections = allSections.filter { it.notebookId == notebookId }
    val menus = remember { NotebookMenus() }
    // I gruppi chiusi qui: all'inizio aperti tutti, è la lista intera. Si
    // ricordano anche tornando indietro da una sezione.
    var collapsed by rememberSaveable(stateSaver = CollapsedSaver) { mutableStateOf(emptySet<String>()) }
    val context = LocalPlatformContext.current
    val store = remember(context) { PageImageStore(context) }

    val rows = remember(sections, collapsed) {
        flattenSections(sections.map { if (it.isGroup) it.copy(isExpanded = it.id !in collapsed) else it })
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground),
                navigationIcon = {
                    Row {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = Strings.back)
                        }
                        if (!LocalSidebarPinned.current) {
                            IconButton(onClick = onOpenSidebar) {
                                Icon(Icons.Filled.Menu, contentDescription = Strings.openSidebar)
                            }
                        }
                    }
                },
                actions = {
                    notebook?.let { current ->
                        IconButton(onClick = { menus.sheet = NotebookMenuTarget.OfNotebook(current) }) {
                            Icon(Icons.Filled.MoreHoriz, contentDescription = Strings.pageOptions)
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 32.dp)
        ) {
            item(key = "header") {
                Row(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    notebook?.let { NotebookIcon(it, store, size = 40.dp) }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        notebook?.title?.ifBlank { Strings.untitled }.orEmpty(),
                        color = NotionWhite,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (sections.isEmpty()) {
                item(key = "empty") {
                    Text(
                        NotebookStrings.noSections,
                        color = NotionGray400,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }
            items(rows, key = { it.section.id }) { row ->
                val section = row.section
                // Qui l'albero parte dal margine: il quaderno è il titolo in cima.
                val depth = row.depth - 1
                if (section.isGroup) {
                    TreeLine(
                        depth = depth,
                        isCurrent = false,
                        onClick = { collapsed = if (section.id in collapsed) collapsed - section.id else collapsed + section.id },
                        onLongPress = { menus.sheet = NotebookMenuTarget.OfSection(section) },
                        marker = {
                            Icon(
                                if (section.isExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                                contentDescription = if (section.isExpanded) Strings.collapse else Strings.expand,
                                tint = NotionGray400,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    ) {
                        Text(
                            section.title.ifBlank { Strings.untitled },
                            color = NotionWhite,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (row.isEmptyGroup) {
                        Text(
                            NotebookStrings.emptyGroup,
                            color = NotionGray400,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(
                                start = rowStart(depth + 1) + MARKER_WIDTH + MARKER_GAP,
                                top = 2.dp,
                                bottom = 6.dp
                            )
                        )
                    }
                } else {
                    TreeLine(
                        depth = depth,
                        isCurrent = false,
                        onClick = { onOpenSection(section.id) },
                        onLongPress = { menus.sheet = NotebookMenuTarget.OfSection(section) },
                        marker = { SectionTab(section.color, height = 20.dp) }
                    ) {
                        // Nome e lucchetto prendono tutto lo spazio: il numero
                        // delle pagine resta in fila sul bordo destro, a
                        // qualunque rientro.
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                section.title.ifBlank { Strings.untitled },
                                color = NotionWhite,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (section.isProtected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            NotebookStrings.pageCount(pageCounts[section.id] ?: 0),
                            color = NotionGray400,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            item(key = "add") {
                Spacer(modifier = Modifier.height(8.dp))
                AddRow(NotebookStrings.newSection, depth = 0) {
                    menus.dialog = NotebookDialog.NewSection(notebookId, parentId = null, isGroup = false)
                }
                AddRow(NotebookStrings.newSectionGroup, depth = 0) {
                    menus.dialog = NotebookDialog.NewSection(notebookId, parentId = null, isGroup = true)
                }
            }
        }
    }

    NotebookMenuHost(
        menus = menus,
        viewModel = viewModel,
        store = store,
        sections = allSections,
        onOpenSection = onOpenSection,
        onNotebookDeleted = { if (it == notebookId) onBack() }
    )
}

/** I gruppi chiusi nella pagina di un quaderno, salvati come elenco. */
private val CollapsedSaver = listSaver<Set<String>, String>(save = { it.toList() }, restore = { it.toSet() })
