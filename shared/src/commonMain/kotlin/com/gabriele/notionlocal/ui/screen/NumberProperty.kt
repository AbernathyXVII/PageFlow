package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.entity.TimeFormatKind
import com.gabriele.notionlocal.data.entity.CellAlignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriele.notionlocal.data.entity.Calculation
import com.gabriele.notionlocal.data.entity.CalculationResult
import com.gabriele.notionlocal.data.entity.DurationUnit
import com.gabriele.notionlocal.data.entity.durationBetween
import com.gabriele.notionlocal.data.entity.durationUnits
import java.time.LocalDateTime
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DEFAULT_NUMBER_COLOR
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.NumberDisplay
import com.gabriele.notionlocal.data.entity.NumberFormatKind
import com.gabriele.notionlocal.data.entity.NumberSettings
import com.gabriele.notionlocal.data.entity.calculationsFor
import com.gabriele.notionlocal.data.entity.decorate
import com.gabriele.notionlocal.data.entity.numberFill
import com.gabriele.notionlocal.data.entity.numberSettings
import com.gabriele.notionlocal.data.entity.numberWithDecimals
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.DarkSurface
import com.gabriele.notionlocal.ui.theme.NotionGray400
import java.math.BigDecimal

// ---------------------------------------------------------------------------
// **Le colonne Number** (dal 26/09/2026): formato (numero, percentuale,
// valute), cifre decimali, e il numero mostrato come barra o anello. E i
// **calcoli** sotto la tabella ("Calculate"), per qualunque colonna.
// ---------------------------------------------------------------------------

/** Il numero di una cella come si legge: cifre scelte, separatori delle impostazioni, valuta o %. */
fun formatNumberValue(raw: String, settings: NumberSettings): String {
    val plain = numberWithDecimals(raw, settings.decimals) ?: return raw
    return settings.format.decorate(Formats.number(plain))
}

fun formatNumberValue(raw: String, column: DatabaseColumnEntity): String = formatNumberValue(raw, column.numberSettings())

// --- A sinistra, al centro, a destra (dal 26/09/2026) ---

/** Dove sta il contenuto della cella che si sta disegnando: lo dà la tabella, altrove è a sinistra. */
val LocalCellAlignment = staticCompositionLocalOf { CellAlignment.LEFT }

val CellAlignment.box: Alignment
    get() = when (this) {
        CellAlignment.LEFT -> Alignment.CenterStart
        CellAlignment.CENTER -> Alignment.Center
        CellAlignment.RIGHT -> Alignment.CenterEnd
    }

val CellAlignment.arrangement: Arrangement.Horizontal
    get() = when (this) {
        CellAlignment.LEFT -> Arrangement.Start
        CellAlignment.CENTER -> Arrangement.Center
        CellAlignment.RIGHT -> Arrangement.End
    }

val CellAlignment.textAlign: TextAlign
    get() = when (this) {
        CellAlignment.LEFT -> TextAlign.Start
        CellAlignment.CENTER -> TextAlign.Center
        CellAlignment.RIGHT -> TextAlign.End
    }

private fun hexColor(hex: String?): Color =
    runCatching { Color(parseColorString(hex ?: DEFAULT_NUMBER_COLOR)) }
        .getOrDefault(Color(parseColorString(DEFAULT_NUMBER_COLOR)))

/** La barra: il fondo grigio e, sopra, la parte piena del colore scelto. */
@Composable
private fun NumberBar(fill: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(NotionGray400.copy(alpha = 0.3f))
    ) {
        if (fill > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fill)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

/** L'anello: il cerchio grigio e, sopra, l'arco pieno dall'alto in senso orario. */
@Composable
private fun NumberRing(fill: Float, color: Color, size: Dp = 20.dp) {
    val track = NotionGray400.copy(alpha = 0.3f)
    Canvas(modifier = Modifier.size(size)) {
        val stroke = size.toPx() * 0.16f
        val inset = stroke / 2
        val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
        drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
        if (fill > 0f) {
            drawArc(color, -90f, 360f * fill, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    }
}

/**
 * **Il numero nella cella come barra o anello** (dal 26/09/2026): pieno per
 * il numero diviso 100, col numero accanto se "Show number" è acceso.
 * L'anello sta a sinistra o in mezzo alla cella, come si è scelto. Una
 * cella vuota resta vuota.
 */
@Composable
fun NumberVisual(
    column: DatabaseColumnEntity,
    raw: String,
    modifier: Modifier = Modifier,
    /** Dove sta nella cella: la barra la riempie comunque; l'anello a destra o al centro se la colonna lo chiede. */
    alignment: CellAlignment = LocalCellAlignment.current
) {
    if (raw.isBlank()) return
    val settings = column.numberSettings()
    val color = hexColor(settings.color)
    val fill = numberFill(raw)
    val text = formatNumberValue(raw, settings)
    when (settings.display) {
        NumberDisplay.NUMBER -> Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            textAlign = alignment.textAlign,
            modifier = modifier.fillMaxWidth()
        )
        NumberDisplay.BAR -> Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (settings.showNumber) {
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            NumberBar(fill, color, Modifier.weight(1f))
        }
        NumberDisplay.RING -> Row(
            modifier = modifier.fillMaxWidth(),
            // A sinistra comanda "Position" dell'anello; al centro o a destra,
            // l'allineamento della colonna.
            horizontalArrangement = when (alignment) {
                CellAlignment.LEFT -> if (settings.ringCentered) Arrangement.Center else Arrangement.Start
                else -> alignment.arrangement
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (settings.showNumber) {
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            NumberRing(fill, color)
        }
    }
}

// --- "Edit property" di una colonna Number ---

/**
 * La parte di "Edit property" propria dei numeri, come su Notion: Number
 * format, Decimal places, poi sotto "Show as" i tre riquadri Number, Bar e
 * Ring; con Bar o Ring il colore e "Show number", con Ring anche dove sta.
 */
@Composable
fun NumberSettingsSection(settings: NumberSettings, onChange: (NumberSettings) -> Unit) {
    var pickingFormat by remember { mutableStateOf(false) }
    var pickingDecimals by remember { mutableStateOf(false) }
    var pickingColor by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsLine(DbStrings.numberFormat, DbStrings.numberFormatName(settings.format)) { pickingFormat = true }
        SettingsLine(
            DbStrings.decimalPlaces,
            settings.decimals?.toString() ?: DbStrings.defaultDecimals
        ) { pickingDecimals = true }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))
        Text(DbStrings.showAs, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            // I tre riquadri come su Notion: "42", una barra piena a metà, un
            // anello pieno al 40%.
            ShowAsTile(
                DbStrings.displayNumber,
                settings.display == NumberDisplay.NUMBER,
                Modifier.weight(1f),
                onClick = { onChange(settings.copy(display = NumberDisplay.NUMBER)) }
            ) {
                Text("42", fontSize = 20.sp, color = tileColor(settings.display == NumberDisplay.NUMBER))
            }
            ShowAsTile(
                DbStrings.displayBar,
                settings.display == NumberDisplay.BAR,
                Modifier.weight(1f),
                onClick = { onChange(settings.copy(display = NumberDisplay.BAR)) }
            ) {
                NumberBar(0.5f, tileColor(settings.display == NumberDisplay.BAR), Modifier.width(52.dp))
            }
            ShowAsTile(
                DbStrings.displayRing,
                settings.display == NumberDisplay.RING,
                Modifier.weight(1f),
                onClick = { onChange(settings.copy(display = NumberDisplay.RING)) }
            ) {
                NumberRing(0.4f, tileColor(settings.display == NumberDisplay.RING), size = 22.dp)
            }
        }

        if (settings.display != NumberDisplay.NUMBER) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { pickingColor = true }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(DbStrings.color, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(hexColor(settings.color))
                    )
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChange(settings.copy(showNumber = !settings.showNumber)) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(DbStrings.showNumber, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = settings.showNumber, onCheckedChange = { onChange(settings.copy(showNumber = it)) })
                }
                if (settings.display == NumberDisplay.RING) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(DbStrings.position, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        SegmentChoice(DbStrings.positionLeft, !settings.ringCentered) {
                            onChange(settings.copy(ringCentered = false))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        SegmentChoice(DbStrings.positionCenter, settings.ringCentered) {
                            onChange(settings.copy(ringCentered = true))
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(DbStrings.appliesToAllViews, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (pickingFormat) {
        NumberFormatSheet(
            current = settings.format,
            onPick = {
                onChange(settings.copy(format = it))
                pickingFormat = false
            },
            onDismiss = { pickingFormat = false }
        )
    }
    if (pickingDecimals) {
        DecimalPlacesDialog(
            current = settings.decimals,
            onPick = {
                onChange(settings.copy(decimals = it))
                pickingDecimals = false
            },
            onDismiss = { pickingDecimals = false }
        )
    }
    if (pickingColor) {
        ColorPickerSheet(
            initialHex = settings.color ?: DEFAULT_NUMBER_COLOR,
            background = false,
            onDismiss = { pickingColor = false },
            onSwitchTarget = {},
            onPick = { hex ->
                pickingColor = false
                onChange(settings.copy(color = hex))
            },
            showTargetSwitch = false,
            previewLabel = DbStrings.color
        )
    }
}

// Il segno del riquadro scelto è il blu di Notion; gli altri restano grigi.
@Composable
private fun tileColor(selected: Boolean): Color = if (selected) MaterialTheme.colorScheme.primary else NotionGray400

@Composable
private fun ShowAsTile(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    preview: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.primary else NotionGray400.copy(alpha = 0.4f),
                RoundedCornerShape(10.dp)
            )
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.height(28.dp), contentAlignment = Alignment.Center) { preview() }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsLine(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SegmentChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .border(1.dp, NotionGray400.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

/**
 * **"Number format"**: Number e Percent, una riga, poi le valute col loro
 * segno, nell'ordine chiesto dall'utente. In cima il campo per cercarne una.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NumberFormatSheet(current: NumberFormatKind, onPick: (NumberFormatKind) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
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
            Text(DbStrings.numberFormat, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(DbStrings.filterFormats, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    AutoCaps {
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val shown = NumberFormatKind.entries.filter {
                    DbStrings.numberFormatName(it).contains(query.trim(), ignoreCase = true) || it.name.contains(query.trim(), ignoreCase = true)
                }
                shown.forEachIndexed { index, kind ->
                    // La riga fra Percent e la prima valuta.
                    if (kind.isCurrency && (index == 0 || !shown[index - 1].isCurrency) && index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(kind) }
                            .padding(vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            when (kind) {
                                NumberFormatKind.NUMBER -> "#"
                                NumberFormatKind.PERCENT -> "%"
                                else -> kind.symbol.trim()
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.width(44.dp)
                        )
                        Text(DbStrings.numberFormatName(kind), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        if (kind == current) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** **"Decimal places"**: Default (come si è scritto), poi da 0 a 5. */
@Composable
private fun DecimalPlacesDialog(current: Int?, onPick: (Int?) -> Unit, onDismiss: () -> Unit) {
    FitAlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSheet,
        title = { Text(DbStrings.decimalPlaces) },
        text = {
            Column {
                (listOf<Int?>(null) + (0..5)).forEach { places ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(places) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(places?.toString() ?: DbStrings.defaultDecimals, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        if (places == current) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}

// --- "Edit property" di una colonna Duration (dal 29/09/2026) ---

/** L'esempio sotto le scelte, quello dato dall'utente: dal 12/02/1996 00:00 al 29/09/2026 21:22. */
private val DURATION_SAMPLE = durationBetween(LocalDateTime.of(1996, 2, 12, 0, 0), LocalDateTime.of(2026, 9, 29, 21, 22))!!

/**
 * **"Show as" di una colonna Duration**: fino a quale unità si scrive la
 * durata, dai soli anni ad anni, mesi, giorni, ore e minuti; il resto si
 * lascia fuori, senza decimali. Sotto ogni scelta, come si legge l'esempio.
 */
@Composable
fun DurationSettingsSection(format: DurationUnit, onChange: (DurationUnit) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(DbStrings.showAs, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        DurationUnit.entries.forEach { unit ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChange(unit) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(DbStrings.durationFormatName(unit), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        DbStrings.durationText(durationUnits(DURATION_SAMPLE, unit)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (unit == format) {
                    Icon(Icons.Filled.Check, contentDescription = DbStrings.selected, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(DbStrings.appliesToAllViews, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Una scelta da un elenco, con la spunta su quella attuale e, se c'è, un esempio sotto il nome. */
@Composable
private fun <T> ChoiceListDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
    details: Map<T, String> = emptyMap()
) {
    FitAlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSheet,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { (option, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(option) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                            details[option]?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (option == selected) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}

// --- "Calculate" ---

/**
 * **"Calculate"** di una colonna: None, e i gruppi Count, Percent, e per le
 * date Date, per i numeri More options. Un gruppo si apre toccandolo; il
 * calcolo scelto ha la spunta ed è aperto il suo gruppo. `type` null = la
 * colonna del nome.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculateSheet(
    title: String,
    type: ColumnType?,
    current: Calculation?,
    onPick: (Calculation?) -> Unit,
    onDismiss: () -> Unit,
    /**
     * **"Include sub rows"** (dal 29/09/2026): se il calcolo conta anche le
     * sotto-righe. Di suo spento: contano solo le righe principali.
     */
    includeSubRows: Boolean = false,
    onIncludeSubRows: ((Boolean) -> Unit)? = null
) {
    val groups = calculationsFor(type)
    var open by remember { mutableStateOf(current?.let { c -> groups.entries.firstOrNull { c in it.value }?.key }) }
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
            Text(DbStrings.calculate, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            OptionsGroup {
                CalcLine(DbStrings.none, checked = current == null, indent = false) { onPick(null) }
                groups.forEach { (group, calcs) ->
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { open = if (open == group) null else group }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(DbStrings.calcGroupName(group), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Icon(
                            if (open == group) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (open == group) {
                        calcs.forEach { calc ->
                            CalcLine(DbStrings.calcName(calc), checked = calc == current, indent = true) { onPick(calc) }
                        }
                    }
                }
            }
            if (onIncludeSubRows != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OptionsGroup {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onIncludeSubRows(!includeSubRows) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(DbStrings.includeSubRows, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Switch(checked = includeSubRows, onCheckedChange = { onIncludeSubRows(it) })
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CalcLine(label: String, checked: Boolean, indent: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = if (indent) 32.dp else 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (indent) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (checked) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

/**
 * Il risultato di un calcolo scritto per il fondo della colonna: i numeri
 * della colonna nel suo formato, le percentuali con al più una cifra, le
 * date nel formato delle impostazioni.
 */
fun calculationText(result: CalculationResult, column: DatabaseColumnEntity?): String = when (result) {
    is CalculationResult.Count -> Formats.number(result.n.toString())
    is CalculationResult.Percent -> {
        val rounded = BigDecimal(result.value).setScale(1, java.math.RoundingMode.HALF_UP).stripTrailingZeros()
        Formats.number(rounded.toPlainString()) + "%"
    }
    is CalculationResult.Number -> {
        val raw = result.value.stripTrailingZeros().toPlainString()
        if (column != null) formatNumberValue(raw, column) else Formats.number(raw)
    }
    // Le date più vecchia e più recente, nel formato di Settings.
    is CalculationResult.Date -> Formats.date(result.epochMillis)
    is CalculationResult.Days -> DbStrings.daysCount(result.days)
    CalculationResult.Nothing -> "—"
}
