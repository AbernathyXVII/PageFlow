package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.DatabaseSnapshot
import com.gabriele.notionlocal.data.entity.MULTI_VALUE_SEPARATOR
import com.gabriele.notionlocal.data.entity.RollupFormatter
import com.gabriele.notionlocal.data.entity.encodeRelationIds
import com.gabriele.notionlocal.data.entity.parseColorCell
import com.gabriele.notionlocal.data.entity.parseDateRange
import com.gabriele.notionlocal.data.entity.relationConfig
import com.gabriele.notionlocal.data.entity.relationIds
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.i18n.RelationStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.DarkSurface
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.viewmodel.DatabaseViewModel

/**
 * **"Relation" e "Rollup" nelle celle** (dal 21/10/2026, chiesti dall'utente,
 * come su Notion). Vedi `Relations.kt` per come sono fatti i valori.
 *
 * Nella cella di una Relation si vedono i nomi delle righe collegate, con
 * la freccia come i collegamenti a pagina; toccandone uno si apre la sua
 * pagina, toccando il resto si sceglie quali righe collegare, da un elenco
 * con la ricerca (le righe possono essere migliaia). La cella di un Rollup
 * mostra il calcolo e non si scrive.
 */

/** I database collegati dalle Relation, per id: le loro righe e il loro nome. Li dà `RelationHost`. */
val LocalRelated = compositionLocalOf<Map<String, DatabaseSnapshot>> { emptyMap() }

/** Aprire la pagina di una riga collegata; `onMissing` se non ne ha una (un database semplice). */
@Stable
class RelationActions(val openRow: (rowId: String, onMissing: () -> Unit) -> Unit)

val LocalRelationActions = staticCompositionLocalOf<RelationActions?> { null }

/**
 * Chi sa i database collegati e apre le loro pagine: la schermata del
 * database e le proprietà in cima alla pagina di una riga lo mettono
 * intorno alle loro celle, come `PageLinkHost`.
 */
@Composable
fun RelationHost(
    viewModel: DatabaseViewModel,
    onOpenPage: (String) -> Unit,
    content: @Composable () -> Unit
) {
    val related by viewModel.related.collectAsStateWithLifecycle()
    val open by rememberUpdatedState(onOpenPage)
    val actions = remember(viewModel) {
        RelationActions { rowId, onMissing ->
            viewModel.openRelatedRow(rowId) { pageId -> if (pageId != null) open(pageId) else onMissing() }
        }
    }
    CompositionLocalProvider(LocalRelated provides related, LocalRelationActions provides actions, content = content)
}

/**
 * Come si scrivono numeri e date nei Rollup: come nel resto dell'app, con le
 * scelte di Settings. Lo mette `MainActivity` all'avvio (`RollupFormatting`).
 */
object AppRollupFormatter : RollupFormatter {
    override fun number(value: Double): String {
        val plain = if (value == Math.floor(value) && !value.isInfinite()) value.toLong().toString()
        else java.math.BigDecimal(value).setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        return Formats.number(plain)
    }

    override fun date(millis: Long): String = Formats.date(millis)

    override fun dateTime(millis: Long): String = Formats.dateTime(millis)

    override fun cell(column: DatabaseColumnEntity, raw: String): String = when (column.type) {
        ColumnType.CHECKBOX -> if (raw == "true") "✓" else ""
        ColumnType.COLOR -> parseColorCell(raw).text
        ColumnType.NUMBER -> formatNumberValue(raw, column)
        ColumnType.DATE -> parseDateRange(raw)?.let { Formats.date(it.start) } ?: raw
        ColumnType.MULTI_SELECT, ColumnType.PEOPLE -> raw.split(MULTI_VALUE_SEPARATOR).filter { it.isNotBlank() }.joinToString(", ")
        else -> raw
    }
}

/** Le righe collegate da una cella, nell'ordine scelto: quelle che non ci sono più si saltano. */
private fun linkedRows(value: String, target: DatabaseSnapshot?): List<DatabaseRowEntity> =
    target?.let { snapshot -> relationIds(value).mapNotNull { snapshot.rowsById[it] } }.orEmpty()

/** La cella di una Relation: i nomi delle righe collegate; toccandola si scelgono. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RelationCell(
    column: DatabaseColumnEntity,
    value: String,
    onValueChange: (String) -> Unit,
    contentPadding: Dp,
    alignment: Alignment = Alignment.CenterStart
) {
    val config = column.relationConfig()
    val target = config?.let { LocalRelated.current[it.database] }
    val locked = LocalDatabaseLocked.current
    val actions = LocalRelationActions.current
    var picking by remember { mutableStateOf(false) }
    val rows = linkedRows(value, target)
    // Senza una pagina da aprire (un database semplice), il tocco sceglie le righe.
    fun openOrPick(rowId: String) {
        val open = actions?.openRow
        if (open != null) open(rowId) { picking = !locked } else picking = !locked
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clickable(enabled = !locked && target != null) { picking = true }
            .padding(horizontal = contentPadding)
            .cellWrapPadding(),
        contentAlignment = alignment
    ) {
        when {
            config != null && target != null && target.page == null && value.isNotEmpty() -> Text(
                RelationStrings.databaseMissing,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = cellMaxLines,
                overflow = TextOverflow.Ellipsis
            )
            LocalWrapCells.current -> FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                rows.forEach { row -> RelatedRowLink(row, onOpen = { openOrPick(row.id) }) }
            }
            else -> RelatedRowsLine(rows, onOpen = { openOrPick(it.id) })
        }
    }
    if (picking && target != null) {
        RelationPickerSheet(
            title = DbStrings.columnName(column.name),
            target = target,
            selected = relationIds(value),
            onChange = { onValueChange(encodeRelationIds(it)) },
            onDismiss = { picking = false }
        )
    }
}

/**
 * Le righe collegate in fila, su una riga sola: quelle che ci stanno intere,
 * poi "+N" per le altre (dal 21/10/2026). Prima l'ultima restava tagliata a
 * metà, una freccia e due puntini. Se non ci sta nemmeno la prima, la prima
 * si accorcia coi tre puntini.
 */
@Composable
private fun RelatedRowsLine(rows: List<DatabaseRowEntity>, onOpen: (DatabaseRowEntity) -> Unit) {
    // Si misurano i nomi col misuratore del testo, senza disegnarli: una riga
    // composta e non mostrata la leggerebbe lo stesso TalkBack.
    val measurer = rememberTextMeasurer()
    val nameStyle = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.Underline)
    val moreStyle = MaterialTheme.typography.bodySmall
    val density = LocalDensity.current
    BoxWithConstraints {
        val max = constraints.maxWidth
        val gap = with(density) { 8.dp.roundToPx() }
        val arrow = with(density) { (14.dp + 3.dp).roundToPx() }
        val widths = remember(rows, nameStyle, density) {
            rows.map { arrow + measurer.measure(it.title.ifBlank { Strings.untitled }, nameStyle, maxLines = 1).size.width }
        }
        val total = widths.sum() + gap * (rows.size - 1).coerceAtLeast(0)
        var count = rows.size
        var firstRoom: Int? = null
        if (total > max && rows.isNotEmpty()) {
            // Il posto per "+N" col numero più lungo possibile.
            val widest = measurer.measure("+${rows.size}", moreStyle, maxLines = 1).size.width
            var used = 0
            count = 0
            while (count < rows.size) {
                val next = used + (if (count > 0) gap else 0) + widths[count]
                if (next + gap + widest > max) break
                used = next
                count++
            }
            if (count == 0) {
                // Nemmeno la prima: accorciata, col posto per "+N" se ce ne sono altre.
                firstRoom = (max - (if (rows.size > 1) gap + widest else 0)).coerceAtLeast(0)
                count = 1
            }
        }
        val hidden = rows.size - count
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            rows.take(count).forEachIndexed { i, row ->
                val room = firstRoom.takeIf { i == 0 }
                RelatedRowLink(
                    row,
                    onOpen = { onOpen(row) },
                    modifier = if (room != null) Modifier.widthIn(max = with(density) { room.toDp() }) else Modifier
                )
            }
            if (hidden > 0) MoreRows(hidden)
        }
    }
}
/** "+N": quante righe collegate non ci stanno nella cella. */
@Composable
private fun MoreRows(count: Int) {
    Text(
        "+$count",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1
    )
}

/** Una riga collegata: la freccia e il nome sottolineato, come un collegamento a pagina. */
@Composable
private fun RelatedRowLink(row: DatabaseRowEntity, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clickable(onClick = onOpen),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.NorthEast,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.size(3.dp))
        Text(
            row.title.ifBlank { Strings.untitled },
            style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.Underline),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Il testo di una cella Relation su una scheda (bacheca, galleria, elenco): i nomi, con la freccia. */
fun relationCardText(value: String, target: DatabaseSnapshot?): String =
    linkedRows(value, target).joinToString("  ") { "↗ " + it.title.ifBlank { Strings.untitled } }

/**
 * **La scelta delle righe da collegare**: in cima quelle già collegate,
 * sotto tutte le altre del database, con la ricerca. Un tocco collega o
 * scollega, e la cella cambia subito.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelationPickerSheet(
    title: String,
    target: DatabaseSnapshot,
    selected: List<String>,
    onChange: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf(selected) }
    fun toggle(id: String) {
        chosen = if (id in chosen) chosen - id else chosen + id
        onChange(chosen)
    }
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                text = RelationStrings.rowsOf(target.page?.title?.ifBlank { Strings.untitled } ?: RelationStrings.databaseMissing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
            )
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.size(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(Strings.search, color = NotionGray400, style = MaterialTheme.typography.bodyLarge)
                    }
                    AutoCaps {
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it.replace("\n", "") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = NotionWhite),
                            cursorBrush = SolidColor(NotionWhite),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.size(8.dp))
            val wanted = query.trim()
            fun matches(row: DatabaseRowEntity) = wanted.isEmpty() || row.title.contains(wanted, ignoreCase = true)
            // Le collegate nell'ordine in cui sono state scelte, le altre in quello del database.
            val linked = chosen.mapNotNull { target.rowsById[it] }.filter(::matches)
            val others = target.rows.filter { it.id !in chosen && matches(it) }
            LazyColumn(modifier = Modifier.weight(1f)) {
                if (linked.isNotEmpty()) {
                    item(key = "linked-title") { PickerSectionTitle(RelationStrings.linked) }
                    items(linked, key = { "linked:" + it.id }) { row -> PickerRow(row, checked = true) { toggle(row.id) } }
                    item(key = "linked-divider") { HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp)) }
                }
                if (linked.isEmpty() && others.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            if (target.rows.isEmpty()) RelationStrings.noRows else Strings.noResults,
                            color = NotionGray400,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                    }
                }
                items(others, key = { it.id }) { row -> PickerRow(row, checked = false) { toggle(row.id) } }
            }
        }
    }
}

@Composable
private fun PickerSectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = NotionGray400,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun PickerRow(row: DatabaseRowEntity, checked: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            row.title.ifBlank { Strings.untitled },
            color = NotionWhite,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (checked) {
            Icon(Icons.Filled.Check, contentDescription = DbStrings.selected, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** La cella di un Rollup: il calcolo, da leggere. */
@Composable
fun RollupCell(value: String, contentPadding: Dp, alignment: Alignment = Alignment.CenterStart) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = contentPadding)
            .cellWrapPadding(),
        contentAlignment = alignment
    ) {
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = cellMaxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}
