package com.gabriele.notionlocal.ui.screen

import androidx.compose.ui.unit.Dp
import com.gabriele.notionlocal.ui.theme.pcOr
import com.gabriele.notionlocal.platform.isPc
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import com.gabriele.notionlocal.data.entity.Recurrence
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkSurfaceVariant
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import com.gabriele.notionlocal.platform.PlaceDialogAtBottom

/*
 * I selettori di data e ora "come quelli di Samsung" (dal 25/09/2026),
 * chiesti dall'utente con tre foto dei Promemoria del S25 Ultra:
 *
 *  - la data: il **mese** a griglia, con le frecce ai lati e lo
 *    scorrimento orizzontale per cambiarlo; toccando "Settembre 2026 ▾" si
 *    passa alle **rotelle** di giorno, mese e anno, e ritoccandolo si torna
 *    al mese;
 *  - l'ora: le **rotelle** di ore e minuti, un minuto alla volta (nella foto
 *    andavano di cinque in cinque: l'utente li vuole tutti).
 *
 * In fondo Cancel | Fatto, come nelle foto. Le rotelle sono le stesse del
 * timer della barra laterale (`WheelPicker`), in bianco invece che pesca.
 */

/** L'arancio di Samsung: il giorno scelto qui, Start e Resume nel cronometro e nel timer. */
val SamsungOrange = Color(0xFFE39A62)
private val SundayRed = Color(0xFFE5646E)

val WHEEL_ITEM_HEIGHT = 38.dp

/** Quante volte si ripete una rotella che gira senza fine (dopo 59 c'è 00). */
private const val WHEEL_REPEATS = 1_000

/**
 * Una rotella da far scorrere in verticale: tre valori visibili, quello in
 * mezzo è quello scelto, e ci si ferma sempre su un valore intero.
 *
 * Con `wraps` gira senza fine (sopra 00 c'è l'ultimo); senza — gli anni —
 * si ferma ai due capi. Il valore si comunica **quando la rotella si
 * ferma**, non a ogni numero che passa; e se cambia da fuori (un altro
 * mese, un timer riportato alle rotelle) la rotella ci va da sola. Se
 * cambia `range` (i giorni di un mese diverso) la rotella rinasce.
 *
 * `onLiveValue`, se c'è, sa invece **il numero in mezzo a ogni momento**,
 * anche mentre la rotella gira o si sta assestando: vedi `SamsungTimeDialog`.
 */
@Composable
fun WheelPicker(
    range: Int,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onLiveValue: ((Int) -> Unit)? = null,
    label: (Int) -> String = { "%02d".format(it) },
    selectedColor: Color = NotionWhite,
    otherColor: Color = NotionGray400.copy(alpha = 0.45f),
    selectedSize: TextUnit = 28.sp,
    otherSize: TextUnit = 22.sp,
    wraps: Boolean = true
) {
    key(range, wraps) {
        WheelPickerContent(range, value, onValueChange, modifier, label, selectedColor, otherColor, selectedSize, otherSize, wraps, onLiveValue)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelPickerContent(
    range: Int,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier,
    label: (Int) -> String,
    selectedColor: Color,
    otherColor: Color,
    selectedSize: TextUnit,
    otherSize: TextUnit,
    wraps: Boolean,
    onLiveValue: ((Int) -> Unit)?
) {
    // Senza fine: tante copie, e si parte da quella in mezzo. Con i capi:
    // un posto vuoto sopra il primo e sotto l'ultimo, così anche loro
    // possono stare al centro.
    val total = if (wraps) range * WHEEL_REPEATS else range + 2
    val base = if (wraps) (WHEEL_REPEATS / 2) * range else 1
    fun valueAt(index: Int): Int? = if (wraps) index % range else (index - 1).takeIf { it in 0 until range }

    val safeValue = value.coerceIn(0, range - 1)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = base + safeValue - 1)
    val itemPx = with(LocalDensity.current) { WHEEL_ITEM_HEIGHT.toPx() }
    // L'elemento al centro dei tre visibili.
    val centerIndex by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex + 1 + if (listState.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val picked = valueAt(centerIndex)?.coerceIn(0, range - 1) ?: return@LaunchedEffect
            if (picked != safeValue) onValueChange(picked)
        }
    }
    if (onLiveValue != null) {
        val live by rememberUpdatedState(onLiveValue)
        LaunchedEffect(Unit) {
            snapshotFlow { valueAt(centerIndex) }.collect { shown ->
                if (shown != null) live(shown.coerceIn(0, range - 1))
            }
        }
    }
    LaunchedEffect(safeValue) {
        if (!listState.isScrollInProgress && valueAt(centerIndex) != safeValue) {
            val start = if (wraps) centerIndex - centerIndex % range else 1
            listState.scrollToItem(start + safeValue - 1)
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(listState),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.height(WHEEL_ITEM_HEIGHT * 3)
    ) {
        items(total) { index ->
            val selected = index == centerIndex
            Box(modifier = Modifier.height(WHEEL_ITEM_HEIGHT), contentAlignment = Alignment.Center) {
                val shown = valueAt(index)
                if (shown != null) {
                    Text(
                        text = label(shown),
                        color = if (selected) selectedColor else otherColor,
                        fontSize = if (selected) selectedSize else otherSize,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// --- Il mese ---

private const val MIN_YEAR = 1900
private const val MAX_YEAR = 2100
private const val MONTH_COUNT = (MAX_YEAR - MIN_YEAR + 1) * 12
private val DAY_CELL_HEIGHT = 44.dp

private fun pageOf(month: YearMonth): Int =
    ((month.year.coerceIn(MIN_YEAR, MAX_YEAR) - MIN_YEAR) * 12 + month.monthValue - 1)

private fun monthAt(page: Int): YearMonth = YearMonth.of(MIN_YEAR + page / 12, page % 12 + 1)

private val pickerLocale: Locale get() = AppSettings.language.locale

/** "Settembre 2026": il mese per esteso con la maiuscola, e l'anno. */
private fun monthTitle(month: YearMonth): String =
    month.month.getDisplayName(TextStyle.FULL_STANDALONE, pickerLocale)
        .replaceFirstChar { it.titlecase(pickerLocale) } + " " + month.year

/** Il giorno da cui comincia la settimana dove si è: domenica negli Stati Uniti, lunedì in Italia. */
private val firstDayOfWeek: DayOfWeek get() = WeekFields.of(Locale.getDefault()).firstDayOfWeek

/**
 * Il calendario di un mese, come quello di Samsung.
 *
 * In alto il mese con le frecce per cambiarlo; il mese si cambia anche
 * **trascinando in orizzontale**. Toccando il nome del mese si passa alle
 * rotelle (giorno, mese, anno — nell'ordine in cui si scrive la data dove si
 * è), e ritoccandolo si torna al mese, su quello della data scelta.
 *
 * Chi lo usa decide cosa fa un tocco su un giorno (`onPick`): una data sola,
 * o l'inizio e la fine di un periodo. Le rotelle scelgono sempre la data di
 * partenza (`onWheelPick`). Fra `start` ed `end`, se ci sono tutte e due, i
 * giorni in mezzo hanno una fascia arancio chiaro.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SamsungCalendar(
    start: LocalDate?,
    end: LocalDate?,
    onPick: (LocalDate) -> Unit,
    onWheelPick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * **La ripetizione** (dal 26/09/2026): i giorni in cui capita hanno il
     * pallino arancione del giorno scelto, da `start` in avanti. Il giorno
     * da cui parte, se non è uno di quelli, ha solo il contorno.
     */
    repeat: Recurrence? = null
) {
    val today = remember { LocalDate.now() }
    val pagerState = rememberPagerState(initialPage = pageOf(YearMonth.from(start ?: today))) { MONTH_COUNT }
    val scope = rememberCoroutineScope()
    var wheels by remember { mutableStateOf(false) }
    val shownMonth = monthAt(pagerState.currentPage)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                if (!wheels) {
                    Icon(
                        Icons.Filled.KeyboardArrowLeft,
                        contentDescription = DbStrings.previousMonth,
                        tint = NotionWhite,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable {
                                scope.launch { pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0)) }
                            }
                    )
                }
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        if (wheels) {
                            // Tornando al mese, si va su quello della data scelta.
                            val target = start?.let { YearMonth.from(it) } ?: shownMonth
                            scope.launch { pagerState.scrollToPage(pageOf(target)) }
                        }
                        wheels = !wheels
                    }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val titleMonth = if (wheels) YearMonth.from(start ?: shownMonth.atDay(1)) else shownMonth
                Text(monthTitle(titleMonth), color = NotionWhite, fontSize = 19.sp)
                Icon(
                    if (wheels) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = NotionWhite
                )
            }
            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                if (!wheels) {
                    Icon(
                        Icons.Filled.KeyboardArrowRight,
                        contentDescription = DbStrings.nextMonth,
                        tint = NotionWhite,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable {
                                scope.launch { pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(MONTH_COUNT - 1)) }
                            }
                    )
                }
            }
            // **"Today"** (dal 30/09/2026, chiesto dall'utente): riporta il
            // calendario al mese di oggi, anche dalle rotelle. Arancione come
            // il numero di oggi nel mese. Non sceglie il giorno: lo si tocca,
            // come gli altri, così lo si può usare anche per arrivare a domani.
            Text(
                text = DbStrings.today,
                color = SamsungOrange,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        val page = pageOf(YearMonth.from(today))
                        if (wheels) {
                            wheels = false
                            scope.launch { pagerState.scrollToPage(page) }
                        } else {
                            scope.launch { pagerState.animateScrollToPage(page) }
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            )
        }

        if (wheels) {
            DateWheels(
                date = start ?: shownMonth.atDay(1),
                onChange = onWheelPick,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            WeekdayHeader()
            HorizontalPager(state = pagerState, beyondViewportPageCount = 1) { page ->
                MonthGrid(
                    month = monthAt(page),
                    today = today,
                    start = start,
                    end = end,
                    onPick = onPick,
                    repeat = repeat
                )
            }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        (0 until 7).forEach { offset ->
            val day = firstDayOfWeek.plus(offset.toLong())
            Text(
                text = day.getDisplayName(TextStyle.SHORT, pickerLocale).uppercase(pickerLocale).trimEnd('.'),
                color = if (day == DayOfWeek.SUNDAY) SundayRed else NotionWhite.copy(alpha = 0.85f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp)
            )
        }
    }
}

/**
 * Sei settimane, dalla prima che tocca il mese. **I giorni degli altri mesi
 * non ci sono** (dal 28/09/2026, chiesto dall'utente): prima si vedevano in
 * grigio in cima e in fondo, e con una ripetizione anche i loro pallini, e
 * confondevano. Le loro caselle restano vuote, così la griglia ha sempre la
 * stessa altezza passando da un mese all'altro.
 */
@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    start: LocalDate?,
    end: LocalDate?,
    onPick: (LocalDate) -> Unit,
    repeat: Recurrence?
) {
    val first = month.atDay(1)
    val shift = (first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val gridStart = first.minusDays(shift.toLong())
    // I giorni della ripetizione nelle sei settimane mostrate: si calcolano
    // solo per queste, come fa il calendario del database.
    val repeatDays = remember(month, repeat, start) {
        val from = start ?: today
        repeat?.occurrences(from, gridStart, gridStart.plusDays(41))?.toSet().orEmpty()
    }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        (0 until 6).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                (0 until 7).forEach { weekday ->
                    val date = gridStart.plusDays((week * 7 + weekday).toLong())
                    if (date.month != month.month) {
                        Spacer(modifier = Modifier.weight(1f).height(DAY_CELL_HEIGHT))
                        return@forEach
                    }
                    DayCell(
                        date = date,
                        today = date == today,
                        start = start,
                        end = end,
                        onPick = onPick,
                        modifier = Modifier.weight(1f),
                        repeating = repeat != null,
                        repeatDay = date in repeatDays
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    today: Boolean,
    start: LocalDate?,
    end: LocalDate?,
    onPick: (LocalDate) -> Unit,
    modifier: Modifier,
    repeating: Boolean = false,
    repeatDay: Boolean = false
) {
    // Con una ripetizione il pallino va sui giorni in cui capita; quello da
    // cui parte, se non capita lì, ha solo il contorno arancione.
    val selected = if (repeating) repeatDay || date == end else date == start || date == end
    val startRing = repeating && date == start && !repeatDay
    val isRange = start != null && end != null && end > start
    val band = SamsungOrange.copy(alpha = 0.28f)
    Box(modifier = modifier.height(DAY_CELL_HEIGHT), contentAlignment = Alignment.Center) {
        // La fascia del periodo: piena fra i due capi, a metà sui capi.
        if (isRange && start != null && end != null && date >= start && date <= end) {
            Row(modifier = Modifier.fillMaxWidth().height(36.dp)) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(if (date == start) Color.Transparent else band))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(if (date == end) Color.Transparent else band))
            }
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (selected) SamsungOrange else Color.Transparent)
                .then(if (startRing) Modifier.border(2.dp, SamsungOrange, CircleShape) else Modifier)
                .clickable { onPick(date) },
            contentAlignment = Alignment.Center
        ) {
            val base = when {
                selected -> Color.White
                date.dayOfWeek == DayOfWeek.SUNDAY -> SundayRed
                today -> SamsungOrange
                else -> NotionWhite
            }
            Text(
                text = date.dayOfMonth.toString(),
                color = base,
                fontSize = 17.sp,
                fontWeight = if (today || selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * Le rotelle della data: il mese in lettere ("SET"), il giorno, l'anno —
 * nell'ordine in cui si scrive la data dove si è. Il giorno si adatta al
 * mese: passando dal 31 marzo a febbraio si finisce al 28 (o 29).
 */
@Composable
private fun DateWheels(date: LocalDate, onChange: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val length = YearMonth.from(date).lengthOfMonth()
    fun emit(year: Int, month: Int, day: Int) {
        val safeDay = day.coerceAtMost(YearMonth.of(year, month).lengthOfMonth())
        onChange(LocalDate.of(year, month, safeDay))
    }
    val monthWheel: @Composable (Modifier) -> Unit = { m ->
        WheelPicker(
            range = 12,
            value = date.monthValue - 1,
            onValueChange = { emit(date.year, it + 1, date.dayOfMonth) },
            label = { java.time.Month.of(it + 1).getDisplayName(TextStyle.SHORT, pickerLocale).uppercase(pickerLocale).trimEnd('.') },
            modifier = m
        )
    }
    val dayWheel: @Composable (Modifier) -> Unit = { m ->
        WheelPicker(
            range = length,
            value = date.dayOfMonth - 1,
            onValueChange = { emit(date.year, date.monthValue, it + 1) },
            label = { (it + 1).toString() },
            modifier = m
        )
    }
    val yearWheel: @Composable (Modifier) -> Unit = { m ->
        WheelPicker(
            range = MAX_YEAR - MIN_YEAR + 1,
            value = date.year - MIN_YEAR,
            onValueChange = { emit(it + MIN_YEAR, date.monthValue, date.dayOfMonth) },
            label = { (it + MIN_YEAR).toString() },
            wraps = false,
            modifier = m
        )
    }
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        val order = if (dayComesFirst()) listOf(dayWheel, monthWheel, yearWheel) else listOf(monthWheel, dayWheel, yearWheel)
        order.forEach { wheel -> wheel(Modifier.weight(1f)) }
    }
}

/** Se dove si è la data si scrive col giorno prima del mese (Italia: 25/9) o dopo (Stati Uniti: 9/25). */
private fun dayComesFirst(): Boolean {
    val pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
        FormatStyle.SHORT, null, IsoChronology.INSTANCE, pickerLocale
    )
    return pattern.indexOf('d').let { d -> d >= 0 && d < pattern.indexOf('M').let { if (it < 0) Int.MAX_VALUE else it } }
}

// --- Le finestre ---

/**
 * La finestra in basso come quelle di Samsung: una scheda arrotondata col
 * titolo in alto a sinistra e **Cancel | Fatto** in fondo, divisi da una
 * lineetta. Toccando fuori si chiude senza scegliere.
 */
@Composable
private fun SamsungPickerDialog(
    title: String,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
    /**
     * Sul PC la scheda è larga così, al centro della finestra (dal
     * 12/11/2026, chiesto dall'utente: "più piccola e contenuta"): larga
     * quanto la finestra, com'è sul telefono, era una striscia enorme.
     */
    pcWidth: Dp,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // **La finestra sta in fondo e non è più alta della scheda.** Con un
        // riquadro a tutto schermo dentro la finestra, e la scheda in fondo
        // a quello, Cancel | Fatto finivano sotto il bordo dello schermo:
        // la finestra non sa dov'è la barra di navigazione (visto sul
        // telefono). Così a metterla in fondo è Android, e il tocco fuori la
        // chiude da sé.
        PlaceDialogAtBottom()
        Box(modifier = if (isPc) Modifier.width(pcWidth) else Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 16.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(pcOr(10.dp, 28.dp)))
                    .background(DarkSurfaceVariant)
                    .padding(top = pcOr(16.dp, 24.dp), bottom = pcOr(4.dp, 8.dp))
            ) {
                Text(
                    text = title,
                    color = NotionWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = pcOr(20.dp, 24.dp))
                )
                Spacer(modifier = Modifier.height(pcOr(8.dp, 16.dp)))
                content()
                Spacer(modifier = Modifier.height(pcOr(4.dp, 8.dp)))
                Row(modifier = Modifier.fillMaxWidth().height(pcOr(40.dp, 52.dp)), verticalAlignment = Alignment.CenterVertically) {
                    DialogButton(Strings.cancel, onDismiss, Modifier.weight(1f))
                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(NotionGray400.copy(alpha = 0.5f)))
                    DialogButton(Strings.done, onDone, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DialogButton(label: String, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = NotionWhite, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** "Set date": il mese (o le rotelle) per scegliere un giorno solo. */
@Composable
fun SamsungDateDialog(
    initial: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var picked by remember { mutableStateOf(initial) }
    SamsungPickerDialog(
        title = DbStrings.setDate,
        pcWidth = 340.dp,
        onDismiss = onDismiss,
        onDone = {
            onPick(picked)
            onDismiss()
        }
    ) {
        SamsungCalendar(
            start = picked,
            end = null,
            onPick = { picked = it },
            onWheelPick = { picked = it }
        )
    }
}

/**
 * "Set time": ore e minuti a rotelle, un minuto alla volta, sulle
 * ventiquattro ore.
 *
 * **"Done" prende i numeri che si vedono in mezzo** (dal 16/10/2026), anche
 * se la rotella si sta ancora fermando. Prima valeva il numero di quando la
 * rotella si era fermata del tutto: con un colpo veloce e "Done" subito
 * dopo restava quello di prima — "scelgo l'ora precisa ma rimane sempre
 * alle 9:00", e poi "non mi fa mettere le 15:01" (l'utente).
 */
@Composable
fun SamsungTimeDialog(
    initial: LocalTime,
    onPick: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    var hour by remember { mutableStateOf(initial.hour) }
    var minute by remember { mutableStateOf(initial.minute) }
    SamsungPickerDialog(
        title = DbStrings.setTime,
        pcWidth = 260.dp,
        onDismiss = onDismiss,
        onDone = {
            onPick(LocalTime.of(hour, minute))
            onDismiss()
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = pcOr(24.dp, 48.dp), vertical = pcOr(8.dp, 16.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelPicker(range = 24, value = hour, onValueChange = { hour = it }, onLiveValue = { hour = it }, modifier = Modifier.weight(1f), selectedSize = 32.sp, otherSize = 26.sp)
            Text(":", color = NotionWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            WheelPicker(range = 60, value = minute, onValueChange = { minute = it }, onLiveValue = { minute = it }, modifier = Modifier.weight(1f), selectedSize = 32.sp, otherSize = 26.sp)
        }
    }
}
