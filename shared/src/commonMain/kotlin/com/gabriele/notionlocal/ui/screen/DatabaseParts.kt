package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.platform.isPc
import com.gabriele.notionlocal.ui.theme.NoRowLinesOnPc
import com.gabriele.notionlocal.ui.theme.pcOr
import com.gabriele.notionlocal.data.entity.DateUnit
import com.gabriele.notionlocal.data.entity.endOf
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ArrowDropDownCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewComfy
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.NaturalOrder
import com.gabriele.notionlocal.data.entity.CalendarMode
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.DateGroupBy
import com.gabriele.notionlocal.data.entity.DateRange
import com.gabriele.notionlocal.data.entity.GalleryCardPreview
import com.gabriele.notionlocal.data.entity.GalleryCardSize
import com.gabriele.notionlocal.data.entity.GroupLevel
import com.gabriele.notionlocal.data.entity.GroupOrder
import com.gabriele.notionlocal.data.entity.MULTI_VALUE_SEPARATOR
import com.gabriele.notionlocal.data.entity.SelectOption
import com.gabriele.notionlocal.data.entity.TimelineZoom
import com.gabriele.notionlocal.data.entity.darkTextOn
import com.gabriele.notionlocal.data.entity.parseDateRange
import com.gabriele.notionlocal.data.entity.startOf
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.theme.DarkSurface
import com.gabriele.notionlocal.ui.theme.DarkSurfaceVariant
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.viewmodel.DatabaseTableState
import java.time.DayOfWeek
import java.time.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Un gruppo di righe della tabella, o una colonna (o una fascia) della
 * bacheca: il valore che lo definisce, come si legge, il colore se è un
 * tag, e le righe che ci stanno dentro.
 */
data class RowGroup(
    val key: String,
    val label: String,
    val color: String?,
    val rows: List<DatabaseRowEntity>,
    /** Il secondo colore del tag e la linea in mezzo: vedi `SelectOption.secondColor`. */
    val secondColor: String? = null,
    val colorLine: Boolean = false
)

/**
 * Divide le righe in gruppi secondo la proprietà scelta.
 *
 * L'ordine dei gruppi non è casuale: per i tag è quello in cui sono
 * stati creati, lo stesso che si vede modificando la proprietà, così
 * spostandosi fra tabella e bacheca non cambia niente. Per tutto il
 * resto è l'ordine naturale del valore. **Il gruppo "senza valore" sta
 * sempre in fondo**: davanti ruberebbe il primo sguardo alle righe già
 * smistate, che sono il contenuto vero.
 *
 * Con la selezione multipla una riga compare in **tutti** i gruppi dei
 * suoi tag: è quello che ci si aspetta da un raggruppamento. Dal 14/10/2026
 * anche le colonne della bacheca vengono da qui, e una scheda sta nella
 * colonna di ognuno dei suoi tag.
 */
fun buildRowGroups(
    state: DatabaseTableState,
    column: DatabaseColumnEntity,
    hideEmpty: Boolean,
    /** Le righe da dividere: tutte, o quelle di un gruppo per farne i sottogruppi. */
    rows: List<DatabaseRowEntity> = state.rows,
    /**
     * Tiene il gruppo "senza valore" anche vuoto: nella bacheca è la colonna
     * dove nasce una pagina creata dal pulsante in fondo.
     */
    keepEmptyNone: Boolean = false,
    /** Per una data: gruppi per giorno, settimana, mese o anno ("Date by", dal 21/10/2026). */
    dateBy: DateGroupBy = DateGroupBy(),
    /** "Sort groups" (dal 28/10/2026): a mano ([manual], le chiavi), crescente o decrescente. */
    order: GroupOrder = GroupOrder.MANUAL,
    manual: List<String> = emptyList()
): List<RowGroup> {
    fun valueOf(row: DatabaseRowEntity) = state.cellValues[row.id to column.id].orEmpty()

    val options = parseSelectOptionsFull(column.optionsJson)
    val groups = mutableListOf<RowGroup>()
    val emptyLabel = DbStrings.noValue(DbStrings.columnName(column.name))

    when (column.type) {
        ColumnType.SELECT, ColumnType.MULTI_SELECT -> {
            val multi = column.type == ColumnType.MULTI_SELECT
            fun tagsOf(row: DatabaseRowEntity): List<String> = if (multi) {
                valueOf(row).split(MULTI_VALUE_SEPARATOR).filter { it.isNotEmpty() }
            } else {
                listOfNotNull(valueOf(row).takeIf { it.isNotEmpty() })
            }

            val declared = options.map { it.label }
            // Un valore rimasto nelle celle dopo che l'opzione è stata
            // cancellata non deve far sparire le sue righe: gli si dà
            // comunque un gruppo, in fondo a quelli dichiarati.
            val orphans = rows.flatMap { tagsOf(it) }.distinct().filter { it !in declared }
            (declared + orphans).forEach { label ->
                val option = options.find { it.label == label }
                groups += RowGroup(
                    key = label,
                    label = label,
                    color = option?.color,
                    rows = rows.filter { label in tagsOf(it) },
                    secondColor = option?.secondColor,
                    colorLine = option?.colorLine == true
                )
            }
            groups += RowGroup("", emptyLabel, null, rows.filter { tagsOf(it).isEmpty() })
        }

        ColumnType.CHECKBOX -> {
            // Niente gruppo "senza valore": una casella o è spuntata o
            // non lo è, e "vuota" vuol dire già "non spuntata".
            groups += RowGroup(
                "true",
                DbStrings.checked,
                null,
                rows.filter { valueOf(it) == "true" }
            )
            groups += RowGroup(
                "false",
                DbStrings.unchecked,
                null,
                rows.filter { valueOf(it) != "true" }
            )
        }

        ColumnType.DATE -> {
            // Un gruppo per giorno: raggruppare per istante esatto
            // darebbe un gruppo per riga. O, con "Date by", per settimana,
            // mese o anno: la chiave è il primo giorno del gruppo, che è
            // anche la data di una riga nata lì dentro.
            val unit = dateBy.unit
            // La settimana va dal lunedì alla domenica, come su Soridata e
            // nelle classifiche K-pop (dal 21/10/2026): col telefono in
            // inglese americano cominciava di domenica, e l'Inkigayo della
            // domenica finiva nella settimana dopo.
            val byStart = rows.groupBy { row ->
                parseDateRange(valueOf(row))?.start?.toLocalDate()?.let { unit.startOf(it, DayOfWeek.MONDAY) }
            }
            val starts = byStart.keys.filterNotNull().sorted()
            (if (dateBy.newestFirst) starts.reversed() else starts).forEach { start ->
                groups += RowGroup(
                    key = start.toEpochDay().toString(),
                    label = dateGroupLabel(unit, start),
                    color = null,
                    rows = byStart[start].orEmpty()
                )
            }
            groups += RowGroup("", emptyLabel, null, byStart[null].orEmpty())
        }

        else -> {
            val byValue = rows.groupBy { valueOf(it) }
            val comparator = if (column.type == ColumnType.NUMBER) {
                compareBy<String> { it.toDoubleOrNull() ?: Double.MAX_VALUE }
            } else {
                // I gruppi come le righe: "2" prima di "10" (vedi `NaturalOrder`).
                NaturalOrder
            }
            byValue.keys.filter { it.isNotEmpty() }.sortedWith(comparator).forEach { value ->
                groups += RowGroup(value, value, null, byValue[value].orEmpty())
            }
            groups += RowGroup("", emptyLabel, null, byValue[""].orEmpty())
        }
    }

    // Le date hanno il loro ordine ("Newest first" in "Date by").
    val ordered = if (column.type == ColumnType.DATE) groups else orderGroups(groups, order, manual, numeric = column.type == ColumnType.NUMBER)
    // Un gruppo "senza valore" vuoto non si mostra: non è una scelta
    // dell'utente, è semplicemente che non c'è niente da dire. Tranne la
    // colonna della bacheca, che è il posto delle pagine nuove.
    return ordered.filter {
        it.rows.isNotEmpty() || (!hideEmpty && it.key.isNotEmpty()) || (keepEmptyNone && it.key.isEmpty())
    }
}

/** I gruppi di un livello, col suo "Hide empty groups", "Date by" e "Sort groups". */
fun buildLevelGroups(
    state: DatabaseTableState,
    level: GroupLevel,
    rows: List<DatabaseRowEntity> = state.rows,
    hideEmpty: Boolean = level.hideEmpty,
    keepEmptyNone: Boolean = false
): List<RowGroup> = buildRowGroups(state, level.column, hideEmpty, rows, keepEmptyNone, level.dateBy, level.order, level.manual)

internal fun CalendarMode.displayName(): String = DbStrings.calendarModeName(this)

/**
 * "12 Jan 2026", "12 Jan 2026, 09:30" oppure con la freccia e la fine: la
 * data e l'ora come dicono "Date format" e "Time format" in Settings (l'ora a
 * 12, a 24 ore o nascosta).
 */
internal fun DateRange.label(): String {
    val format = if (hasTime) Formats.databaseDateTimeFormatter() else Formats.dateFormatter()
    val startText = Instant.ofEpochMilli(start)
        .atZone(AppSettings.zoneId).format(format)
    val endText = end?.let {
        Instant.ofEpochMilli(it).atZone(AppSettings.zoneId).format(format)
    }
    return if (endText == null) startText else "$startText → $endText"
}

/** Una riga delle impostazioni che accende e spegne invece di aprire qualcosa. */
@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    /** Spento con "Lock view", per le impostazioni della vista. */
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.size(14.dp))
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = if (enabled) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

/**
 * La pastiglia colorata di un tag. La scritta è bianca, o nera su un colore
 * chiaro (dal 05/10/2026: dallo spettro si sceglie anche il giallo acceso,
 * su cui il bianco non si legge), con la regola delle celle colorate.
 */
@Composable
fun TagChip(label: String, option: SelectOption?) =
    TagChip(label = label, color = option?.color, secondColor = option?.secondColor, colorLine = option?.colorLine == true)

/**
 * Con **due colori** (dal 20/10/2026): `color` nella metà di sinistra,
 * `secondColor` in quella di destra, attaccati o con una linea in mezzo del
 * colore della pagina, che sembra un taglio fra le due metà.
 */
@Composable
fun TagChip(label: String, color: String?, secondColor: String? = null, colorLine: Boolean = false) {
    val shape = RoundedCornerShape(4.dp)
    val dark = when {
        color == null -> false
        secondColor != null -> darkTextOn(color, secondColor)
        else -> darkTextOn(color)
    }
    val left = tagColor(color)
    val right = secondColor?.let { tagColor(it) }
    val lineColor = MaterialTheme.colorScheme.background
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = if (dark) Color.Black else NotionWhite,
        maxLines = 1,
        modifier = Modifier
            .then(
                if (right == null) {
                    Modifier.background(left, shape)
                } else {
                    Modifier
                        .clip(shape)
                        .drawBehind {
                            val half = size.width / 2
                            drawRect(left, size = Size(half, size.height))
                            drawRect(right, topLeft = Offset(half, 0f), size = Size(size.width - half, size.height))
                            if (colorLine) {
                                val width = 2.dp.toPx()
                                drawRect(lineColor, topLeft = Offset(half - width / 2, 0f), size = Size(width, size.height))
                            }
                        }
                }
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

/**
 * Se il database aperto è bloccato nel contenuto (la voce "Lock page"):
 * righe e celle si leggono ma non si scrivono.
 *
 * Passa da qui e non da un parametro perché i campi di scrittura di una
 * tabella stanno in fondo a una catena lunga di composable — riga,
 * cella, editor — e infilare lo stesso `Boolean` in tutte le firme è un
 * modo sicuro di dimenticarselo in una.
 */
val LocalDatabaseLocked = staticCompositionLocalOf { false }

/** Un riquadro del selettore di visualizzazione, come su Notion. */
@Composable
fun LayoutChoice(
    layout: DatabaseLayout,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    Column(
        modifier = modifier
            .padding(4.dp)
            .border(1.dp, accent, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            layout.icon(),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = accent
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            layout.displayName(),
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Il riquadro arrotondato che raggruppa più voci dentro una finestra,
 * come su Notion: le voci affini stanno insieme su uno sfondo un po'
 * più chiaro di quello della finestra, separate da linee sottili.
 */
@Composable
fun SheetGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Sul PC niente riquadro grigio, come i menu di Notion.
            .then(if (isPc) Modifier else Modifier.background(DarkSurface, RoundedCornerShape(10.dp)))
            .padding(horizontal = pcOr(6.dp, 14.dp))
    ) {
        NoRowLinesOnPc(content)
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    label: String,
    value: String?,
    onClick: () -> Unit,
    /** Rosso, per le voci che tolgono qualcosa e non si possono annullare. */
    danger: Boolean = false,
    /**
     * Spenta: si vede ma non risponde. La usa "Lock view" per le voci
     * che cambiano l'impaginazione — restano in elenco, così si capisce
     * che ci sono e che è la chiave a tenerle ferme.
     */
    enabled: Boolean = true
) {
    val tint = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        danger -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (enabled) it.clickable { onClick() } else it }
            .padding(vertical = pcOr(5.dp, 14.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(pcOr(18.dp, 20.dp)),
            tint = tint
        )
        Spacer(modifier = Modifier.size(pcOr(10.dp, 14.dp)))
        Text(
            text = label,
            color = when {
                !enabled -> tint
                danger -> MaterialTheme.colorScheme.error
                else -> Color.Unspecified
            },
            modifier = Modifier.weight(1f)
        )
        if (!value.isNullOrEmpty()) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.size(4.dp))
        }
        Icon(
            Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = tint
        )
    }
}

@Composable
fun SheetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leading: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            leading,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.size(12.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AutoCaps {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

fun DatabaseLayout.displayName(): String = DbStrings.layoutName(this)

fun DatabaseLayout.icon(): ImageVector = when (this) {
    DatabaseLayout.TABLE -> Icons.Filled.TableChart
    DatabaseLayout.BOARD -> Icons.Filled.ViewColumn
    DatabaseLayout.CALENDAR -> Icons.Filled.CalendarToday
    DatabaseLayout.TIMELINE -> Icons.Filled.ViewTimeline
    DatabaseLayout.LIST -> Icons.Filled.FormatListBulleted
    // La stessa icona della voce "Gallery view" nel menu "/".
    DatabaseLayout.GALLERY -> Icons.Filled.GridView
    DatabaseLayout.CHART -> Icons.Filled.PieChart
}

internal fun GalleryCardPreview.icon(): ImageVector = when (this) {
    GalleryCardPreview.NONE -> Icons.Filled.Block
    GalleryCardPreview.PAGE_COVER -> Icons.Filled.Image
    GalleryCardPreview.PAGE_CONTENT -> Icons.Filled.Notes
}

internal fun GalleryCardSize.icon(): ImageVector = when (this) {
    GalleryCardSize.SMALL -> Icons.Filled.ViewComfy
    GalleryCardSize.MEDIUM -> Icons.Filled.GridView
    GalleryCardSize.LARGE -> Icons.Filled.ViewAgenda
}

/** Il nome del tipo nella lingua dell'app. Per il nome che si salva vedi `DbStrings.englishTypeName`. */
internal fun ColumnType.displayName(): String = DbStrings.typeName(this)

fun ColumnType.icon(): ImageVector = when (this) {
    ColumnType.TEXT -> Icons.Filled.Notes
    ColumnType.NUMBER -> Icons.Filled.Tag
    ColumnType.SELECT -> Icons.Filled.ArrowDropDownCircle
    ColumnType.MULTI_SELECT -> Icons.Filled.Checklist
    ColumnType.DATE -> Icons.Filled.CalendarToday
    ColumnType.DURATION -> Icons.Filled.Timelapse
    ColumnType.CHECKBOX -> Icons.Filled.CheckBox
    ColumnType.COLOR -> Icons.Filled.FormatColorFill
    ColumnType.URL -> Icons.Filled.Link
    ColumnType.EMAIL -> Icons.Filled.AlternateEmail
    ColumnType.PHONE -> Icons.Filled.Call
    ColumnType.PAGE_LINK -> Icons.Filled.NorthEast
    // Le icone di Notion: le due frecce che si incrociano, e la lente sul foglio.
    ColumnType.RELATION -> Icons.Filled.CallMade
    ColumnType.ROLLUP -> Icons.Filled.Search
    ColumnType.IMAGE -> Icons.Filled.Image
    ColumnType.CREATED_TIME -> Icons.Filled.Schedule
    ColumnType.LAST_EDITED_TIME -> Icons.Filled.Update
    // La mano che tocca, come l'icona del bottone di Notion.
    ColumnType.BUTTON -> Icons.Filled.TouchApp
    // La sigma, come l'icona delle formule di Notion.
    ColumnType.FORMULA -> Icons.Filled.Functions
}

/** Le opzioni di una colonna a selezione, lette dal JSON salvato. */
internal fun parseSelectOptionsFull(optionsJson: String): List<SelectOption> {
    return try {
        Json.decodeFromString<List<SelectOption>>(optionsJson)
    } catch (e: Exception) {
        emptyList()
    }
}

/** Il colore di un tag; se manca o è scritto male, il grigio di base. */
internal fun tagColor(hex: String?): Color {
    if (hex == null) return DarkSurfaceVariant
    return try {
        Color(parseColorString(hex))
    } catch (e: IllegalArgumentException) {
        DarkSurfaceVariant
    }
}

internal fun TimelineZoom.displayName(): String = DbStrings.zoomName(this)

internal val DATE_FORMAT: DateTimeFormatter get() = Formats.dateFormatter()

internal val MONTH_FORMAT: DateTimeFormatter
    get() = DateTimeFormatter.ofPattern("MMMM yyyy", AppSettings.language.locale)

/** La lingua in cui scrivere nomi di giorni e mesi: quella dell'app, non quella del telefono. */
internal val displayLocale: Locale get() = AppSettings.language.locale

/**
 * **I gruppi nell'ordine scelto** ("Sort groups", dal 28/10/2026). A mano:
 * quelli in [manual] nel suo ordine, poi gli altri (i nuovi) nell'ordine di
 * sempre; crescente e decrescente per nome, i numeri come numeri. Il gruppo
 * senza valore resta in fondo.
 */
fun orderGroups(groups: List<RowGroup>, order: GroupOrder, manual: List<String>, numeric: Boolean = false): List<RowGroup> {
    val (none, valued) = groups.partition { it.key.isEmpty() }
    val byName: Comparator<RowGroup> = if (numeric) {
        compareBy { it.key.toDoubleOrNull() ?: Double.MAX_VALUE }
    } else {
        Comparator { a, b -> NaturalOrder.compare(a.label, b.label) }
    }
    val sorted = when (order) {
        GroupOrder.MANUAL -> if (manual.isEmpty()) {
            valued
        } else {
            val place = manual.withIndex().associate { it.value to it.index }
            // `sortedBy` non rimescola i pari: i gruppi nuovi restano nell'ordine di sempre.
            valued.sortedBy { place[it.key] ?: Int.MAX_VALUE }
        }
        GroupOrder.ASCENDING -> valued.sortedWith(byName)
        GroupOrder.DESCENDING -> valued.sortedWith(byName.reversed())
    }
    return sorted + none
}

/**
 * Il nome di un gruppo di date: il giorno; per una settimana il primo e
 * l'ultimo giorno ("13/10/2026 – 19/10/2026"); il mese col suo nome; l'anno.
 */
fun dateGroupLabel(unit: DateUnit, start: LocalDate): String = when (unit) {
    DateUnit.DAY -> start.format(DATE_FORMAT)
    DateUnit.WEEK -> start.format(DATE_FORMAT) + " – " + unit.endOf(start).format(DATE_FORMAT)
    DateUnit.MONTH -> start.format(MONTH_FORMAT).replaceFirstChar { it.titlecase(displayLocale) }
    DateUnit.YEAR -> start.year.toString()
}

internal fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(AppSettings.zoneId).toLocalDate()

internal fun Long.toLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(AppSettings.zoneId).toLocalDateTime()
