package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.AccentBlue
import com.gabriele.notionlocal.ui.theme.NotionGray400

// ---------------------------------------------------------------------------
// **Le pagine di un database scelte con la casella, come su Notion** (dal
// 25/11/2026, chiesto dall'utente con le foto di Notion, solo sul PC, nella
// tabella): passando col mouse su una riga, nel margine a sinistra della
// tabella compare la casella; spuntata resta, e la riga si fa azzurra. In alto
// a sinistra dei titoli delle colonne la casella che le sceglie tutte (col
// trattino quando ne è scelta solo qualcuna). Con almeno una scelta, al posto
// del nome del database c'è la barra "N selected" col cestino e i tre puntini
// (vedi `DatabaseViewScreen`); trascinando una riga scelta vengono con lei
// tutte le altre (vedi `RowDrag`).
// ---------------------------------------------------------------------------

/** Le righe scelte della tabella; [all] sono quelle che la vista mostra, per "sceglile tutte". */
class RowSelection {
    var selected by mutableStateOf<Set<String>>(emptySet())
    var all by mutableStateOf<List<String>>(emptyList())

    fun toggle(rowId: String) {
        selected = if (rowId in selected) selected - rowId else selected + rowId
    }

    /** La casella dei titoli, come in Esplora file: le sceglie tutte, e se lo erano già le toglie. */
    fun toggleAll() {
        selected = if (all.isNotEmpty() && all.all { it in selected }) emptySet() else all.toSet()
    }
}

/** Le righe scelte di questa tabella; null dove non si scelgono (il telefono, le altre viste). */
val LocalRowSelection = staticCompositionLocalOf<RowSelection?> { null }

/** Quanto sono lunghe le linee della tabella: una riga scelta è azzurra fin lì, come la sua linea. */
val LocalTableLineWidth = staticCompositionLocalOf { 0.dp }

/** Lo spazio della casella, nel margine a sinistra della tabella. */
val ROW_SELECT_GUTTER = 26.dp

/**
 * Allarga di [width] verso sinistra chi lo porta, senza spostare il resto: misura largo [width]
 * di più e si mette [width] più a sinistra. Chi sta dentro comincia col margine vuoto, dove va la
 * casella; tocchi compresi, perché il margine fa parte di lui.
 */
fun Modifier.bleedStart(width: Dp): Modifier = layout { measurable, constraints ->
    val extra = width.roundToPx()
    val widened = if (constraints.hasBoundedWidth) {
        constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra)
    } else {
        constraints
    }
    val placeable = measurable.measure(widened)
    layout((placeable.width - extra).coerceAtLeast(0), placeable.height) { placeable.place(-extra, 0) }
}

/** La casella di Notion: vuota col bordo, azzurra con la spunta, o col trattino ([partial]). */
@Composable
fun SelectionCheckbox(checked: Boolean, onClick: () -> Unit, description: String, modifier: Modifier = Modifier, partial: Boolean = false) {
    val filled = checked || partial
    Box(
        modifier = modifier
            .size(16.dp)
            .clip(RoundedCornerShape(3.dp))
            .then(if (filled) Modifier.background(AccentBlue) else Modifier.border(1.5.dp, NotionGray400, RoundedCornerShape(3.dp)))
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        if (filled) {
            Icon(if (partial) Icons.Filled.Remove else Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
        }
    }
}

/** L'azzurro sopra una riga scelta. */
val SelectedRowTint = AccentBlue.copy(alpha = 0.14f)

/**
 * Davanti ai titoli delle colonne la casella che sceglie tutte le righe della vista ([RowSelection.all]):
 * col mouse sopra i titoli, o con qualcuna già scelta; col trattino se ne sono scelte solo alcune.
 */
@Composable
fun SelectAllGutter(content: @Composable () -> Unit) {
    val selection = LocalRowSelection.current
    if (selection == null) {
        content()
        return
    }
    var hovered by remember { mutableStateOf(false) }
    Box(
        Modifier
            .bleedStart(ROW_SELECT_GUTTER)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        when (awaitPointerEvent().type) {
                            PointerEventType.Enter -> hovered = true
                            PointerEventType.Exit -> hovered = false
                        }
                    }
                }
            }
    ) {
        Box(Modifier.padding(start = ROW_SELECT_GUTTER)) { content() }
        val chosen = selection.selected
        if (hovered || chosen.isNotEmpty()) {
            val all = selection.all.isNotEmpty() && selection.all.all { it in chosen }
            SelectionCheckbox(
                checked = all,
                partial = !all && chosen.isNotEmpty(),
                onClick = selection::toggleAll,
                description = DbStrings.selectAllRows,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 5.dp)
            )
        }
    }
}

/**
 * **La barra delle righe scelte**, al posto del nome del database: "N selected", il cestino
 * ([onDelete]) e i tre puntini col menu ([entries]), come su Notion.
 */
@Composable
fun RowSelectionBar(count: Int, onDelete: () -> Unit, entries: List<PcMenuEntry>, modifier: Modifier = Modifier) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .padding(start = 16.dp, top = 6.dp, bottom = 6.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, NotionGray400.copy(alpha = 0.35f), RoundedCornerShape(6.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            DbStrings.selectedCount(count),
            style = MaterialTheme.typography.labelLarge,
            color = AccentBlue,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        VerticalDivider(color = NotionGray400.copy(alpha = 0.35f))
        Box(Modifier.size(30.dp).clickable(onClick = onDelete), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Delete, contentDescription = Strings.delete, tint = NotionGray400, modifier = Modifier.size(18.dp))
        }
        VerticalDivider(color = NotionGray400.copy(alpha = 0.35f))
        Box {
            Box(Modifier.size(30.dp).clickable { menuOpen = true }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.MoreHoriz, contentDescription = DbStrings.moreOptions, tint = NotionGray400, modifier = Modifier.size(18.dp))
            }
            if (menuOpen) PcContextMenu(entries, onDismiss = { menuOpen = false })
        }
    }
}
