package com.gabriele.notionlocal.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.widgets.CountdownEntry
import com.gabriele.notionlocal.data.widgets.CountdownWidget
import com.gabriele.notionlocal.data.widgets.WidgetStore
import com.gabriele.notionlocal.data.widgets.countdownParts
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.i18n.WidgetStrings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

// ---------------------------------------------------------------------------
// **Il widget Countdown** (dal 27/10/2026, chiesto dall'utente con una foto
// del countdown di Indify su Notion): il titolo, e sotto sei foglietti chiari
// coi numeri — mesi, settimane, giorni, ore, minuti, secondi — e il loro nome
// in azzurro. Quando un numero cambia il suo foglietto si sfoglia, come quelli
// di un calendario da tavolo. I dati stanno in `Countdown.kt`.
// ---------------------------------------------------------------------------

const val COUNTDOWN_TAG = "countdown"

/** Il foglietto e il numero, come nella foto: chiaro, col numero scuro. */
private val TileColor = Color(0xFFECECEC)
private val TileShadeColor = Color(0xFFDADADA)
private val TileTextColor = Color(0xFF151515)
private val TileSplitColor = Color(0x33000000)
private val CountdownLabelColor = Color(0xFF8CB9E8)

/** Quanto dura lo sfogliarsi di un foglietto. */
private const val FLIP_MS = 520

private val TILE_HEIGHT = 36.dp
private val TILE_GAP = 6.dp

/**
 * I countdown del widget, uno sotto l'altro. Tenendone premuto uno si cambia
 * (o si toglie); [adding] apre la finestra per uno nuovo, dai tre puntini.
 */
@Composable
fun CountdownView(widget: CountdownWidget, now: Long, adding: Boolean, onAddingChange: (Boolean) -> Unit) {
    var editing by remember(widget.id) { mutableStateOf<CountdownEntry?>(null) }
    val nowLocal = Instant.ofEpochMilli(now).atZone(AppSettings.zoneId).toLocalDateTime().truncatedTo(ChronoUnit.SECONDS)
    if (widget.countdowns.isEmpty()) {
        AddRow(label = WidgetStrings.addCountdown, onClick = { onAddingChange(true) })
    }
    widget.countdowns.forEachIndexed { index, entry ->
        if (index > 0) Spacer(modifier = Modifier.height(14.dp))
        CountdownBlock(entry, nowLocal, onEdit = { editing = entry })
    }

    editing?.let { entry ->
        CountdownEditorDialog(
            initial = entry,
            onDismiss = { editing = null },
            onSave = {
                editing = null
                WidgetStore.updateCountdown(widget.id, it)
            },
            onDelete = {
                editing = null
                WidgetStore.removeCountdown(widget.id, entry.id)
            }
        )
    }
    if (adding) {
        CountdownEditorDialog(
            initial = null,
            onDismiss = { onAddingChange(false) },
            onSave = {
                onAddingChange(false)
                WidgetStore.addCountdown(widget.id, it)
            },
            onDelete = null
        )
    }
}

/** Un countdown: il titolo e i sei foglietti. Tenendolo premuto si cambia. */
@Composable
private fun CountdownBlock(entry: CountdownEntry, now: LocalDateTime, onEdit: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val target = entry.targetTime ?: now
    val parts = countdownParts(now, target)
    val units = listOf(
        parts.months to WidgetStrings.countdownMonths,
        parts.weeks to WidgetStrings.countdownWeeks,
        parts.days to WidgetStrings.countdownDays,
        parts.hours to WidgetStrings.countdownHours,
        parts.minutes to WidgetStrings.countdownMinutes,
        parts.seconds to WidgetStrings.countdownSeconds
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(COUNTDOWN_TAG)
            .pointerInput(entry.id) {
                detectTapGestures(onLongPress = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEdit()
                })
            }
            .padding(top = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (entry.name.isNotBlank()) {
            Text(
                entry.name,
                color = NotionWhite,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val tileWidth = (maxWidth - TILE_GAP * (units.size - 1)) / units.size
            val labelSize = fittingLabelSize(units.map { it.second }, tileWidth + TILE_GAP)
            Row(horizontalArrangement = Arrangement.spacedBy(TILE_GAP)) {
                units.forEach { (value, label) ->
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        FlipTile(
                            text = value.toString().padStart(2, '0'),
                            modifier = Modifier.fillMaxWidth().height(TILE_HEIGHT)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            label,
                            color = CountdownLabelColor,
                            fontSize = labelSize,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Il corpo dei nomi sotto i foglietti: 10 punti, o meno finché il più lungo
 * ("Settimane", "Secondes") non sta nel posto del suo foglietto.
 */
@Composable
private fun fittingLabelSize(labels: List<String>, room: Dp): TextUnit {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val roomPx = with(density) { room.toPx() }
    return remember(labels, roomPx) {
        var size = 10f
        while (size > 6f && labels.any { measurer.measure(it, TextStyle(fontSize = size.sp)).size.width > roomPx }) size -= 0.5f
        size.sp
    }
}

/**
 * **Un foglietto che si sfoglia.** Ogni foglietto è fatto di due metà;
 * quando il numero cambia, la metà di sopra col numero vecchio si piega in
 * avanti sulla sua cerniera (la riga in mezzo) e, passata la metà corsa, la
 * metà di sotto col numero nuovo scende e si appoggia: come i fogli di un
 * calendario da tavolo. Sotto il foglio che cade si vede già, in alto, il
 * numero nuovo, e in basso resta il vecchio finché il foglio non lo copre.
 */
@Composable
fun FlipTile(text: String, modifier: Modifier = Modifier) {
    var current by remember { mutableStateOf(text) }
    var previous by remember { mutableStateOf(text) }
    val flip = remember { Animatable(1f) }
    LaunchedEffect(text) {
        if (text != current) {
            previous = current
            current = text
            flip.snapTo(0f)
            flip.animateTo(1f, tween(FLIP_MS, easing = FastOutSlowInEasing))
        }
    }
    val p = flip.value
    val density = LocalDensity.current.density
    BoxWithConstraints(modifier = modifier) {
        val half = maxHeight / 2
        val digit = with(LocalDensity.current) { (maxHeight * 0.58f).toSp() }
        Column {
            TileHalf(current, top = true, half, digit)
            TileHalf(if (p < 0.5f) previous else current, top = false, half, digit)
        }
        if (p < 1f) {
            if (p < 0.5f) {
                // La metà di sopra, col numero vecchio, si piega in avanti.
                TileHalf(
                    previous, top = true, half, digit,
                    modifier = Modifier.graphicsLayer {
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        rotationX = -180f * p
                        cameraDistance = 12f * density
                    }
                )
            } else {
                // E dall'altra parte scende la metà di sotto del numero nuovo.
                TileHalf(
                    current, top = false, half, digit,
                    modifier = Modifier
                        .offset(y = half)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.5f, 0f)
                            rotationX = 180f * (1f - p)
                            cameraDistance = 12f * density
                        }
                )
            }
        }
        // La cerniera.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = half - 0.5.dp)
                .height(1.dp)
                .background(TileSplitColor)
        )
    }
}

/** Metà foglietto: si vede la metà di sopra (o di sotto) del numero. */
@Composable
private fun TileHalf(text: String, top: Boolean, half: Dp, digit: TextUnit, modifier: Modifier = Modifier) {
    val corner = 5.dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(half)
            .clip(
                if (top) {
                    RoundedCornerShape(topStart = corner, topEnd = corner)
                } else {
                    RoundedCornerShape(bottomStart = corner, bottomEnd = corner)
                }
            )
            // La metà di sotto appena più scura, come la carta in ombra.
            .background(if (top) TileColor else TileShadeColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(align = if (top) Alignment.Top else Alignment.Bottom, unbounded = true)
                .height(half * 2),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = TileTextColor, fontSize = digit, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

/**
 * La finestra di un countdown: il nome, e il giorno e l'ora d'arrivo —
 * anno, mese, giorno, ore, minuti e secondi, da scrivere — anche nel
 * passato. [initial] null: uno nuovo, che parte da domani a mezzanotte.
 */
@Composable
fun CountdownEditorDialog(
    initial: CountdownEntry?,
    onDismiss: () -> Unit,
    onSave: (CountdownEntry) -> Unit,
    onDelete: (() -> Unit)?
) {
    val start = initial?.targetTime ?: LocalDate.now(AppSettings.zoneId).plusDays(1).atStartOfDay()
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var year by remember { mutableStateOf(start.year.toString()) }
    var month by remember { mutableStateOf(start.monthValue.toString()) }
    var day by remember { mutableStateOf(start.dayOfMonth.toString()) }
    var hour by remember { mutableStateOf(start.hour.toString()) }
    var minute by remember { mutableStateOf(start.minute.toString()) }
    var second by remember { mutableStateOf(start.second.toString()) }
    val target = runCatching {
        LocalDateTime.of(year.toInt().also { require(it in 1..9999) }, month.toInt(), day.toInt(), hour.toInt(), minute.toInt(), second.toInt())
    }.getOrNull()

    FitAlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSheet,
        title = { Text(if (initial == null) WidgetStrings.addCountdown else WidgetStrings.editCountdown, color = NotionWhite) },
        text = {
            Column {
                AutoCaps {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.replace("\n", " ") },
                        singleLine = true,
                        label = { Text(WidgetStrings.name) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(WidgetStrings.fieldYear, year, 4, Modifier.weight(1.4f)) { year = it }
                    NumberField(WidgetStrings.fieldMonth, month, 2, Modifier.weight(1f)) { month = it }
                    NumberField(WidgetStrings.fieldDay, day, 2, Modifier.weight(1f)) { day = it }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(WidgetStrings.fieldHour, hour, 2, Modifier.weight(1f)) { hour = it }
                    NumberField(WidgetStrings.fieldMinute, minute, 2, Modifier.weight(1f)) { minute = it }
                    NumberField(WidgetStrings.fieldSecond, second, 2, Modifier.weight(1f)) { second = it }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (target == null) WidgetStrings.noSuchDate else WidgetStrings.countdownPastNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (target == null) MaterialTheme.colorScheme.error else NotionGray400
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = target != null,
                onClick = {
                    val chosen = target ?: return@TextButton
                    val entry = initial ?: CountdownEntry(target = chosen.toString())
                    onSave(entry.copy(name = name.trim(), target = chosen.toString()))
                }
            ) { Text(Strings.done) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text(Strings.delete, color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text(Strings.cancel) }
            }
        }
    )
}

/** Un numero da scrivere: solo cifre, al massimo [digits]. */
@Composable
private fun NumberField(label: String, value: String, digits: Int, modifier: Modifier, onChange: (String) -> Unit) {
    AutoCaps {
        OutlinedTextField(
            value = value,
            onValueChange = { typed -> onChange(typed.filter { it.isDigit() }.take(digits)) },
            singleLine = true,
            label = { Text(label, maxLines = 1, softWrap = false) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = modifier
        )
    }
}
