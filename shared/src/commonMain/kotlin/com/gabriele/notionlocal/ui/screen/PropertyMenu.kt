package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.data.entity.LinkColumnShow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.SortKind
import com.gabriele.notionlocal.data.entity.SortRuleSpec
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkSheet

// ---------------------------------------------------------------------------
// **Il menu di una proprietà** (dal 27/10/2026, chiesto dall'utente con uno
// screenshot di Notion su telefono): si apre **toccando** l'intestazione di
// una colonna — tenerla premuta non fa più niente — ed è una finestra che
// sale dal basso, a gruppi, come quella di Notion. Dentro le voci di prima,
// nello stesso ordine, con in più "Change type" (i tipi, che prima stavano in
// "Edit property"), "Filter" e "Sort". "Filter" apre la stessa finestra
// dell'icona in cima al database; "Sort" dal 02/11/2026 ordina solo per
// questa proprietà (vedi `propertySortRows`). "Edit property" non c'è più
// ("togli Edit property proprio, tanto non serve a nulla"): il nome si scrive
// nella riga in cima, come su Notion, le impostazioni del tipo stanno in
// "Change type" sotto il tipo di adesso, e "Delete property" in fondo.
// ---------------------------------------------------------------------------

const val PROPERTY_MENU_TAG = "propertyMenu"
const val PROPERTY_NAME_FIELD_TAG = "propertyNameField"

/**
 * Le voci del menu che portano fuori dalla tabella: il tipo, il filtro e
 * l'ordinamento. Passano da qui, come `LocalPropertyIcons`, perché le
 * intestazioni stanno in fondo a catene lunghe. [filter], [sortBy] e
 * [removeSort] sono null con "Lock view", come le icone in cima.
 */
@Stable
class PropertyMenuActions(
    /** Il nome nuovo, scritto nella riga in cima al menu; null col database bloccato. */
    val rename: ((DatabaseColumnEntity, String) -> Unit)?,
    /** "Delete property", dopo la domanda; null col database bloccato. */
    val delete: ((DatabaseColumnEntity) -> Unit)?,
    val changeType: (DatabaseColumnEntity) -> Unit,
    /** Il filtro, già sulla proprietà data (null = la proprietà principale, che non si filtra). */
    val filter: ((String) -> Unit)?,
    /** "Sort" di una proprietà (`SORT_BY_NAME` per il nome): la ordina per prima, nel verso dato. */
    val sortBy: ((String, Boolean) -> Unit)?,
    /** Toglie la regola di ordinamento su quella proprietà. */
    val removeSort: ((String) -> Unit)?,
    /** Le regole di ordinamento di adesso, nell'ordine in cui contano. */
    val sortRules: () -> List<SortRuleSpec>,
    /** Che valori ha una proprietà: per le scritte delle due scelte di "Sort". */
    val sortKind: (String) -> SortKind,
    /** "Edit actions" di una proprietà Button (dal 27/10/2026); null col database bloccato. */
    val editButton: ((DatabaseColumnEntity) -> Unit)? = null,
    /** "Show as" di una proprietà Link (dal 13/11/2026, vedi `LinkColumnShow`); null col database bloccato. */
    val setLinkShow: ((DatabaseColumnEntity, LinkColumnShow) -> Unit)? = null
)

val LocalPropertyMenuActions = compositionLocalOf<PropertyMenuActions?> { null }

/**
 * La finestra del menu: in cima la proprietà, con la sua icona e il nome, e
 * sotto i gruppi di voci di [content]. Con [onRename] il nome **si scrive lì**,
 * come su Notion: chiudendo il menu, se è cambiato, la proprietà lo prende.
 * Senza, toccare la riga porta a [onHeader].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyMenuSheet(
    name: String,
    icon: @Composable () -> Unit,
    onHeader: (() -> Unit)?,
    onDismiss: () -> Unit,
    onRename: ((String) -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var draft by remember(name) { mutableStateOf(name) }
    val rename by rememberUpdatedState(onRename)
    // Il menu si chiude in tanti modi (una voce, il velo, il tasto indietro):
    // il nome scritto vale in tutti.
    DisposableEffect(Unit) {
        onDispose {
            val clean = draft.replace("\n", " ").trim()
            if (clean.isNotEmpty() && clean != name) rename?.invoke(clean)
        }
    }
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(PROPERTY_MENU_TAG)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .let { if (onHeader != null) it.clickable(onClick = onHeader) else it }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon()
                Spacer(modifier = Modifier.size(12.dp))
                if (onRename != null) {
                    AutoCaps {
                        BasicTextField(
                            value = draft,
                            onValueChange = { draft = it.replace("\n", " ") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth().testTag(PROPERTY_NAME_FIELD_TAG)
                        )
                    }
                } else {
                    Text(
                        name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            content()
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}

/** Un gruppo di voci, staccato da quello sopra; fra una voce e l'altra una riga sottile. */
@Composable
fun PropertyMenuGroup(rows: List<@Composable () -> Unit>) {
    if (rows.isEmpty()) return
    Spacer(modifier = Modifier.size(12.dp))
    OptionsGroup {
        rows.forEachIndexed { i, row ->
            if (i > 0) HorizontalDivider()
            row()
        }
    }
}

/** Una voce che apre un'altra finestra: la freccetta a destra, come su Notion. */
@Composable
fun PropertyMenuChevronRow(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    OptionRow(
        icon = icon,
        label = label,
        enabled = enabled,
        onClick = onClick,
        trailing = {
            Icon(
                Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

/**
 * **"Sort" di una proprietà** (dal 02/11/2026, chiesto dall'utente:
 * "toccando su Sort mi ordini solo la proprietà che ho selezionato poco
 * prima"). Toccandolo si aprono qui sotto i due versi, con le parole dei
 * suoi valori ([sortChoices]); scegliendone uno la tabella si ordina per
 * questa proprietà e il menu si chiude con [onDone]. "Remove sort" c'è se la
 * proprietà sta già in una regola. Tutte le regole insieme restano nella
 * finestra dell'icona in cima.
 */
@Composable
fun propertySortRows(columnId: String, actions: PropertyMenuActions, onDone: () -> Unit): List<@Composable () -> Unit> {
    var open by remember(columnId) { mutableStateOf(false) }
    val sortBy = actions.sortBy
    val header: @Composable () -> Unit = {
        OptionRow(
            icon = Icons.Filled.Sort,
            label = DbStrings.sort,
            enabled = sortBy != null,
            onClick = { open = !open },
            trailing = {
                Icon(
                    if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }
    if (!open || sortBy == null) return listOf(header)
    val rules = actions.sortRules()
    // La spunta solo se la tabella è ordinata prima di tutto per questa.
    val current = rules.firstOrNull()?.takeIf { it.column == columnId }
    val choices = sortChoices(actions.sortKind(columnId)).map { (descending, label) ->
        val row: @Composable () -> Unit = {
            OptionRow(
                icon = if (descending) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                label = label,
                onClick = {
                    onDone()
                    sortBy(columnId, descending)
                },
                trailing = if (current?.descending == descending) {
                    { Icon(Icons.Filled.Check, contentDescription = DbStrings.selected, tint = MaterialTheme.colorScheme.primary) }
                } else {
                    null
                }
            )
        }
        row
    }
    val remove = actions.removeSort?.takeIf { rules.any { it.column == columnId } }?.let { removeSort ->
        val row: @Composable () -> Unit = {
            OptionRow(
                icon = Icons.Filled.Close,
                label = DbStrings.removeSort,
                onClick = {
                    onDone()
                    removeSort(columnId)
                }
            )
        }
        row
    }
    return listOf(header) + choices + listOfNotNull(remove)
}

/**
 * Le due scelte di "Sort", (decrescente, scritta), prima quella che si
 * sceglie di più: per le parole A → Z, per i numeri dal più grande (come
 * chiesto dall'utente), per le date le più recenti, per le caselle le
 * spuntate. Una formula o un Rollup che danno di tutto: crescente e
 * decrescente, come nella finestra dell'icona.
 */
fun sortChoices(kind: SortKind): List<Pair<Boolean, String>> = when (kind) {
    SortKind.TEXT -> listOf(false to DbStrings.sortAToZ, true to DbStrings.sortZToA)
    SortKind.NUMBER -> listOf(true to DbStrings.largestFirst, false to DbStrings.smallestFirst)
    SortKind.DATE -> listOf(true to Strings.newestFirst, false to Strings.oldestFirst)
    SortKind.CHECKBOX -> listOf(true to DbStrings.checkedFirst, false to DbStrings.uncheckedFirst)
}
