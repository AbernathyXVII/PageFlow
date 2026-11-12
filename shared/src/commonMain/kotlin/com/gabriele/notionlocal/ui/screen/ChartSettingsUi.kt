package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlignHorizontalLeft
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.StackedBarChart
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.ChartAggregation
import com.gabriele.notionlocal.data.entity.ChartHeight
import com.gabriele.notionlocal.data.entity.ChartReferenceLine
import com.gabriele.notionlocal.data.entity.ChartSettings
import com.gabriele.notionlocal.data.entity.ChartSort
import com.gabriele.notionlocal.data.entity.ChartType
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.ui.i18n.ChartStrings
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import java.math.BigDecimal

/** Le schermate dentro la finestra delle impostazioni, per le voci del grafico. */
enum class ChartSettingsPage { GROUP, VALUE, SORT, DECIMALS, REFERENCE, COLOR, STYLE }

fun chartPageTitle(page: ChartSettingsPage, settings: ChartSettings): String = when (page) {
    ChartSettingsPage.GROUP -> ChartStrings.whatToShow
    ChartSettingsPage.VALUE -> if (settings.type == ChartType.DONUT) ChartStrings.sliceBy else ChartStrings.whatToShow
    ChartSettingsPage.SORT -> ChartStrings.sortBy
    ChartSettingsPage.DECIMALS -> ChartStrings.decimalPlaces
    ChartSettingsPage.REFERENCE -> ChartStrings.referenceLine
    ChartSettingsPage.COLOR -> ChartStrings.color
    ChartSettingsPage.STYLE -> ChartStrings.moreStyleOptions
}

fun ChartType.icon(): ImageVector = when (this) {
    ChartType.VERTICAL -> Icons.Filled.BarChart
    ChartType.HORIZONTAL -> Icons.Filled.AlignHorizontalLeft
    ChartType.LINE -> Icons.Filled.ShowChart
    ChartType.DONUT -> Icons.Filled.DonutLarge
}

/** "Name", o il nome della proprietà per cui si raggruppa. */
private fun groupName(settings: ChartSettings, columns: List<DatabaseColumnEntity>, nameLabel: String): String =
    settings.groupColumnId?.let { id -> columns.find { it.id == id } }?.let { DbStrings.columnName(it.name) } ?: nameLabel

private fun valueColumn(settings: ChartSettings, columns: List<DatabaseColumnEntity>): DatabaseColumnEntity? =
    settings.valueColumnId?.let { id -> columns.find { it.id == id && it.type == ColumnType.NUMBER } }

/** "Count", o "Price (Values)". */
private fun valueName(settings: ChartSettings, columns: List<DatabaseColumnEntity>): String =
    valueColumn(settings, columns)?.let { ChartStrings.valueName(DbStrings.columnName(it.name), settings.aggregation) } ?: ChartStrings.count

private fun sortName(sort: ChartSort, settings: ChartSettings, columns: List<DatabaseColumnEntity>, nameLabel: String): String {
    val group = groupName(settings, columns, nameLabel)
    val value = valueColumn(settings, columns)?.let { DbStrings.columnName(it.name) } ?: ChartStrings.count
    return when (sort) {
        ChartSort.LABEL_ASC -> ChartStrings.sortLabelAsc(group)
        ChartSort.LABEL_DESC -> ChartStrings.sortLabelDesc(group)
        ChartSort.VALUE_ASC -> ChartStrings.sortValueAsc(value)
        ChartSort.VALUE_DESC -> ChartStrings.sortValueDesc(value)
    }
}

/** Le cifre decimali come le mostra Notion: 1, 1.0, 1.00… */
private fun decimalsSample(n: Int): String = if (n == 0) "1" else "1." + "0".repeat(n)

private fun plain(value: Double): String = BigDecimal(value).stripTrailingZeros().toPlainString()

private fun colorName(settings: ChartSettings): String =
    settings.color?.let { hex -> CHART_PALETTE.find { chartHex(it.second) == hex }?.first?.let(DbStrings::tagColorName) } ?: ChartStrings.auto

/**
 * **Le voci del grafico** nella finestra delle impostazioni, sotto
 * "Layout", come le mette Notion: il tipo, poi i due assi (o "Data" per la
 * ciambella) e lo stile. Toccata, una voce apre la sua schermata
 * (`onOpenPage`) o accende e spegne.
 */
@Composable
fun ChartSettingsSection(
    settings: ChartSettings,
    columns: List<DatabaseColumnEntity>,
    enabled: Boolean,
    onChange: (ChartSettings) -> Unit,
    onOpenPage: (ChartSettingsPage) -> Unit,
    onPickType: () -> Unit,
    onEditRange: () -> Unit
) {
    SheetGroup {
        SettingsRow(
            icon = settings.type.icon(),
            label = ChartStrings.chartType,
            value = ChartStrings.typeName(settings.type),
            enabled = enabled,
            onClick = onPickType
        )
    }

    @Composable
    fun groupRows() {
        SettingsRow(Icons.Filled.Category, ChartStrings.whatToShow, groupName(settings, columns, nameColumnLabel()), { onOpenPage(ChartSettingsPage.GROUP) }, enabled = enabled)
        HorizontalDivider()
        SettingsRow(Icons.Filled.SwapVert, ChartStrings.sortBy, sortName(settings.sort, settings, columns, nameColumnLabel()), { onOpenPage(ChartSettingsPage.SORT) }, enabled = enabled)
        HorizontalDivider()
        SettingsToggleRow(Icons.Filled.VisibilityOff, ChartStrings.omitZeroValues, settings.omitZero) { if (enabled) onChange(settings.copy(omitZero = it)) }
    }

    @Composable
    fun valueRows() {
        SettingsRow(Icons.Filled.Functions, ChartStrings.whatToShow, valueName(settings, columns), { onOpenPage(ChartSettingsPage.VALUE) }, enabled = enabled)
        HorizontalDivider()
        SettingsRow(
            Icons.Filled.Tag, ChartStrings.decimalPlaces,
            settings.decimals?.let(::decimalsSample) ?: ChartStrings.default,
            { onOpenPage(ChartSettingsPage.DECIMALS) }, enabled = enabled
        )
        HorizontalDivider()
        SettingsToggleRow(Icons.Filled.StackedBarChart, ChartStrings.cumulative, settings.cumulative) { if (enabled) onChange(settings.copy(cumulative = it)) }
        HorizontalDivider()
        SettingsRow(
            Icons.Filled.Height, ChartStrings.range,
            if (settings.rangeMin == null && settings.rangeMax == null) {
                ChartStrings.auto
            } else {
                "${settings.rangeMin?.let(::plain) ?: ChartStrings.auto} – ${settings.rangeMax?.let(::plain) ?: ChartStrings.auto}"
            },
            onEditRange, enabled = enabled
        )
        HorizontalDivider()
        SettingsRow(
            Icons.Filled.HorizontalRule, ChartStrings.referenceLine,
            ChartStrings.linesCount(settings.referenceLines.size),
            { onOpenPage(ChartSettingsPage.REFERENCE) }, enabled = enabled
        )
    }

    when (settings.type) {
        ChartType.VERTICAL, ChartType.LINE -> {
            ChartSectionTitle(ChartStrings.xAxis)
            SheetGroup { groupRows() }
            ChartSectionTitle(ChartStrings.yAxis)
            SheetGroup { valueRows() }
        }
        // Sdraiate le barre, gli assi si scambiano: i numeri in basso.
        ChartType.HORIZONTAL -> {
            ChartSectionTitle(ChartStrings.xAxis)
            SheetGroup { valueRows() }
            ChartSectionTitle(ChartStrings.yAxis)
            SheetGroup { groupRows() }
        }
        ChartType.DONUT -> {
            ChartSectionTitle(ChartStrings.data)
            SheetGroup {
                SettingsRow(Icons.Filled.Category, ChartStrings.whatToShow, groupName(settings, columns, nameColumnLabel()), { onOpenPage(ChartSettingsPage.GROUP) }, enabled = enabled)
                HorizontalDivider()
                SettingsRow(Icons.Filled.PieChart, ChartStrings.sliceBy, valueName(settings, columns), { onOpenPage(ChartSettingsPage.VALUE) }, enabled = enabled)
                HorizontalDivider()
                SettingsRow(Icons.Filled.SwapVert, ChartStrings.sortBy, sortName(settings.sort, settings, columns, nameColumnLabel()), { onOpenPage(ChartSettingsPage.SORT) }, enabled = enabled)
            }
        }
    }
    ChartSectionTitle(ChartStrings.style)
    SheetGroup {
        SettingsRow(Icons.Filled.Palette, ChartStrings.color, colorName(settings), { onOpenPage(ChartSettingsPage.COLOR) }, enabled = enabled)
        HorizontalDivider()
        SettingsRow(Icons.Filled.Brush, ChartStrings.moreStyleOptions, null, { onOpenPage(ChartSettingsPage.STYLE) }, enabled = enabled)
    }
    Spacer(modifier = Modifier.size(12.dp))
}

@Composable
private fun ChartSectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 6.dp)
    )
}

/** Una voce da scegliere, col segno di spunta su quella attuale. */
@Composable
private fun ChoiceRow(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    SettingsRow(icon = icon, label = label, value = if (selected) "✓" else null, onClick = onClick)
}

/**
 * **Le schermate delle voci del grafico.** Scelta una voce la schermata
 * resta aperta, come le altre delle impostazioni: il segno di spunta si
 * sposta, e il grafico dietro cambia subito.
 */
@Composable
fun ChartSettingsDetail(
    page: ChartSettingsPage,
    settings: ChartSettings,
    columns: List<DatabaseColumnEntity>,
    onChange: (ChartSettings) -> Unit
) {
    var addingLine by remember { mutableStateOf(false) }
    when (page) {
        ChartSettingsPage.GROUP -> SheetGroup {
            ChoiceRow(Icons.Filled.TextFields, nameColumnLabel(), settings.groupColumnId == null) {
                onChange(settings.copy(groupColumnId = null))
            }
            // "Page link" no: le barre si chiamerebbero con l'id della pagina.
            // "Relation" sì (dal 21/10/2026): una barra per riga collegata, col suo nome.
            // Un bottone non ha un valore (dal 27/10/2026). Una formula sì (dal
            // 28/10/2026): una barra per ogni risultato, come un testo.
            columns.filter {
                it.type != ColumnType.PAGE_LINK && it.type != ColumnType.IMAGE && it.type != ColumnType.DURATION && it.type != ColumnType.BUTTON
            }.forEach { column ->
                HorizontalDivider()
                ChoiceRow(column.type.icon(), DbStrings.columnName(column.name), settings.groupColumnId == column.id) {
                    onChange(settings.copy(groupColumnId = column.id))
                }
            }
        }

        ChartSettingsPage.VALUE -> {
            val numbers = columns.filter { it.type == ColumnType.NUMBER }
            SheetGroup {
                ChoiceRow(Icons.Filled.Summarize, ChartStrings.count, settings.valueColumnId == null) {
                    onChange(settings.copy(valueColumnId = null))
                }
                numbers.forEach { column ->
                    HorizontalDivider()
                    ChoiceRow(column.type.icon(), DbStrings.columnName(column.name), settings.valueColumnId == column.id) {
                        onChange(settings.copy(valueColumnId = column.id))
                    }
                }
            }
            // Scelta una proprietà Number, come se ne fa un numero solo per gruppo.
            if (valueColumn(settings, columns) != null) {
                ChartSectionTitle(ChartStrings.calculate)
                SheetGroup {
                    ChartAggregation.entries.forEachIndexed { index, aggregation ->
                        if (index > 0) HorizontalDivider()
                        ChoiceRow(Icons.Filled.Functions, ChartStrings.aggregationName(aggregation), settings.aggregation == aggregation) {
                            onChange(settings.copy(aggregation = aggregation))
                        }
                    }
                }
            }
        }

        ChartSettingsPage.SORT -> SheetGroup {
            ChartSort.entries.forEachIndexed { index, sort ->
                if (index > 0) HorizontalDivider()
                ChoiceRow(Icons.Filled.SwapVert, sortName(sort, settings, columns, nameColumnLabel()), settings.sort == sort) {
                    onChange(settings.copy(sort = sort))
                }
            }
        }

        ChartSettingsPage.DECIMALS -> SheetGroup {
            ChoiceRow(Icons.Filled.Tag, ChartStrings.default, settings.decimals == null) { onChange(settings.copy(decimals = null)) }
            (0..4).forEach { n ->
                HorizontalDivider()
                ChoiceRow(Icons.Filled.Tag, decimalsSample(n), settings.decimals == n) { onChange(settings.copy(decimals = n)) }
            }
        }

        ChartSettingsPage.REFERENCE -> {
            SheetGroup {
                settings.referenceLines.forEachIndexed { index, line ->
                    if (index > 0) HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.HorizontalRule, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.size(14.dp))
                        Text(
                            if (line.label.isBlank()) plain(line.value) else "${line.label} · ${plain(line.value)}",
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onChange(settings.copy(referenceLines = settings.referenceLines - line)) }) {
                            Icon(Icons.Filled.Delete, contentDescription = Strings.delete, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (settings.referenceLines.isNotEmpty()) HorizontalDivider()
                SettingsRow(Icons.Filled.Add, ChartStrings.addReferenceLine, null, { addingLine = true })
            }
        }

        ChartSettingsPage.COLOR -> SheetGroup {
            ChoiceRow(Icons.Filled.Palette, ChartStrings.auto, settings.color == null) { onChange(settings.copy(color = null)) }
            CHART_PALETTE.forEach { (name, argb) ->
                HorizontalDivider()
                val hex = chartHex(argb)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChange(settings.copy(color = hex)) }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(androidx.compose.ui.graphics.Color(argb), CircleShape)
                    )
                    Spacer(modifier = Modifier.size(14.dp))
                    Text(DbStrings.tagColorName(name), modifier = Modifier.weight(1f))
                    if (settings.color == hex) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        ChartSettingsPage.STYLE -> {
            ChartSectionTitle(ChartStrings.height)
            SheetGroup {
                ChartHeight.entries.forEachIndexed { index, height ->
                    if (index > 0) HorizontalDivider()
                    ChoiceRow(Icons.Filled.Height, ChartStrings.heightName(height), settings.height == height) {
                        onChange(settings.copy(height = height))
                    }
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
            SheetGroup {
                if (settings.type != ChartType.DONUT) {
                    SettingsToggleRow(Icons.Filled.GridOn, ChartStrings.gridLines, settings.gridLines) { onChange(settings.copy(gridLines = it)) }
                    HorizontalDivider()
                }
                SettingsToggleRow(Icons.Filled.Label, ChartStrings.dataLabels, settings.dataLabels) { onChange(settings.copy(dataLabels = it)) }
                if (settings.type == ChartType.LINE) {
                    HorizontalDivider()
                    SettingsToggleRow(Icons.Filled.Timeline, ChartStrings.smoothLine, settings.smoothLine) { onChange(settings.copy(smoothLine = it)) }
                }
                if (settings.type == ChartType.LINE || settings.type == ChartType.DONUT) {
                    HorizontalDivider()
                    SettingsToggleRow(Icons.Filled.FormatListBulleted, ChartStrings.showLegend, settings.showLegend) { onChange(settings.copy(showLegend = it)) }
                }
                if (settings.type == ChartType.DONUT) {
                    HorizontalDivider()
                    SettingsToggleRow(Icons.Filled.DonutLarge, ChartStrings.showTotal, settings.showTotal) { onChange(settings.copy(showTotal = it)) }
                }
            }
        }
    }

    if (addingLine) {
        ReferenceLineDialog(
            onDismiss = { addingLine = false },
            onConfirm = { line ->
                addingLine = false
                onChange(settings.copy(referenceLines = settings.referenceLines + line))
            }
        )
    }
}

/**
 * **La finestrella dei quattro grafici**: si apre scegliendo "Chart" come
 * vista, e da "Chart type" nelle impostazioni. Toccato un tipo, si chiude.
 */
@Composable
fun ChartTypeDialog(current: ChartType, onPick: (ChartType) -> Unit, onDismiss: () -> Unit) {
    FitAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(ChartStrings.chartType) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChartType.entries.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { type ->
                            val accent = if (type == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, accent, RoundedCornerShape(8.dp))
                                    .clickable { onPick(type) }
                                    .padding(vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(type.icon(), contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(ChartStrings.typeName(type), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}

/** "Range": il minimo e il massimo dell'asse dei numeri; un campo vuoto lo sceglie il grafico. */
@Composable
fun ChartRangeDialog(settings: ChartSettings, onConfirm: (Double?, Double?) -> Unit, onDismiss: () -> Unit) {
    var min by remember { mutableStateOf(settings.rangeMin?.let(::plain).orEmpty()) }
    var max by remember { mutableStateOf(settings.rangeMax?.let(::plain).orEmpty()) }
    val minValue = min.replace(',', '.').toDoubleOrNull()
    val maxValue = max.replace(',', '.').toDoubleOrNull()
    val valid = (min.isBlank() || minValue != null) && (max.isBlank() || maxValue != null) &&
        (minValue == null || maxValue == null || minValue < maxValue)
    FitAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(ChartStrings.range) },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInput(min, ChartStrings.min, Modifier.weight(1f)) { min = it }
                    NumberInput(max, ChartStrings.max, Modifier.weight(1f)) { max = it }
                }
                Spacer(modifier = Modifier.size(8.dp))
                Text(ChartStrings.rangeHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onConfirm(minValue, maxValue) }) { Text(Strings.done) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}

@Composable
private fun ReferenceLineDialog(onConfirm: (ChartReferenceLine) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    val number = value.replace(',', '.').toDoubleOrNull()
    FitAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(ChartStrings.addReferenceLine) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberInput(value, ChartStrings.value, Modifier.fillMaxWidth()) { value = it }
                AutoCaps {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it.replace("\n", "") },
                        singleLine = true,
                        label = { Text(ChartStrings.labelOptional) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = number != null, onClick = { onConfirm(ChartReferenceLine(number!!, label.trim())) }) { Text(Strings.done) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}

@Composable
private fun NumberInput(value: String, label: String, modifier: Modifier, onChange: (String) -> Unit) {
    AutoCaps {
        OutlinedTextField(
            value = value,
            onValueChange = { typed -> onChange(typed.filter { it.isDigit() || it == '.' || it == ',' || it == '-' }) },
            singleLine = true,
            label = { Text(label) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = modifier
        )
    }
}
