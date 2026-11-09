package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.ui.format.Formats

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.DateRange
import com.gabriele.notionlocal.data.entity.DayBlock
import com.gabriele.notionlocal.data.entity.EVENT_COLORS
import com.gabriele.notionlocal.data.entity.EVENT_MIN_VISIBLE_MINUTES
import com.gabriele.notionlocal.data.entity.EventDraft
import com.gabriele.notionlocal.data.entity.allDayOn
import com.gabriele.notionlocal.data.entity.darkTextOn
import com.gabriele.notionlocal.data.entity.dayBlocks
import com.gabriele.notionlocal.data.entity.slotStartMinute
import com.gabriele.notionlocal.data.notify.NotificationPermission
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.DarkSurfaceVariant
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import com.gabriele.notionlocal.platform.screenHeightDp
import com.gabriele.notionlocal.platform.rememberRootHeight

/**
 * Quanto è alta un'ora nella griglia del giorno: mezz'ora è un quadratino
 * da 30dp, che col dito si prende senza sbagliare.
 */
private val DAY_HOUR_HEIGHT = 60.dp
private val DAY_LABEL_WIDTH = 52.dp

/**
 * Il margine a destra dei blocchi, come su Google Calendar: lì c'è sempre
 * un pezzo di griglia da toccare, anche in un'ora già occupata.
 */
private val DAY_BLOCK_GAP_END = 12.dp

/**
 * Quanto spazio sopra la griglia prendono la barra, il nome della vista e
 * le frecce, finché la griglia non sa dove sta (vedi `CalendarDayView`).
 */
private val DAY_CHROME_HEIGHT = 330.dp
private val DAY_MIN_VIEWPORT = 320.dp

/** Lo stacco fra il fondo della griglia e quello dello schermo. */
private val DAY_BOTTOM_MARGIN = 8.dp
private val DAY_ALL_DAY_HEIGHT = 24.dp

/**
 * Quanti giorni si possono sfogliare col dito dal giorno di partenza del
 * pager (`dayPagerOrigin`): circa ventisette anni per parte.
 *
 * **Non di più**: il pager tiene la sua posizione in pixel con i numeri
 * con la virgola, che oltre i sedici milioni non sanno più contare di uno
 * in uno. Con duecentomila pagine (la prima versione) si arrivava a
 * centoventi milioni di pixel, i conti andavano a passi di otto, e
 * scorrendo col dito il giorno si fermava a una ventina di pixel dal
 * bordo, con un filo del giorno accanto. Così sono dodici milioni e mezzo,
 * come la settimana (`WEEK_PAGER_PAGES`).
 */
private const val DAY_PAGER_PAGES = 20_001
private const val DAY_PAGER_CENTER = DAY_PAGER_PAGES / 2

/**
 * Il giorno da cui il pager conta le pagine: cambia una volta ogni
 * vent'anni, quindi il giorno mostrato è sempre a meno di dieci anni da
 * lui, ben dentro le pagine che ci sono. Passando a un altro ventennio il
 * pager rinasce sullo stesso giorno, e non si vede.
 */
private fun dayPagerOrigin(day: LocalDate): LocalDate =
    LocalDate.of(day.year - Math.floorMod(day.year, 20) + 10, 1, 1)

/** La linea di "adesso", rossa come su Google Calendar. */
private val NOW_LINE_COLOR = Color(0xFFEA4335)

private val EVENT_TIME_FORMAT: DateTimeFormatter get() = Formats.timeFormatter()
private val EVENT_DAY_FORMAT: DateTimeFormatter
    get() = DateTimeFormatter.ofPattern("EEEE, d MMMM", AppSettings.language.locale)
private val EVENT_DATE_FORMAT: DateTimeFormatter
    get() = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", AppSettings.language.locale)

/** Il colore di un evento, o quello di sempre se non ne ha uno. */
fun eventBackground(hex: String?): Color {
    if (hex == null) return DarkSurfaceVariant
    return try {
        Color(parseColorString(hex))
    } catch (e: IllegalArgumentException) {
        DarkSurfaceVariant
    }
}

/** Le scritte sopra un evento: chiare sui colori scuri, scure su quelli chiari (Banana). */
@Composable
fun eventContentColor(hex: String?): Color = when {
    hex == null -> MaterialTheme.colorScheme.onBackground
    darkTextOn(hex) -> Color.Black
    else -> Color.White
}

private fun eventColorName(hex: String?): String =
    EVENT_COLORS.find { it.hex == hex }?.name ?: DbStrings.defaultColor

private fun capitalized(text: String): String =
    text.replaceFirstChar { it.titlecase(AppSettings.language.locale) }

/**
 * **La modalità Day del calendario** (dal 30/09/2026, il time-blocking come
 * Google Calendar): le ventiquattro ore una sotto l'altra, divise in
 * quadratini da mezz'ora, e ogni pagina con un'ora come un blocco alto
 * quanto dura, del suo colore. In cima la striscia delle pagine senza ora
 * (o lunghe un giorno o più).
 *
 * - Toccando un quadratino vuoto si crea un evento lì (`onSlotTap`, che
 *   apre la finestra rapida); null col database bloccato.
 * - Toccando un blocco si apre la sua pagina, tenendolo premuto le azioni
 *   della riga (con "Edit event"), come nelle altre viste.
 * - Col dito a destra e a sinistra si cambia giorno; su e giù si scorrono
 *   le ore, dentro un riquadro alto quanto lo schermo lascia. Le ore a
 *   sinistra restano ferme mentre i giorni scorrono.
 * - Oggi ha la linea rossa di adesso, e aprendo si parte da un'ora fa.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarDayView(
    day: LocalDate,
    dated: List<Pair<DatabaseRowEntity, DateRange>>,
    /** L'evento nuovo che si sta scrivendo: si vede già nel suo quadratino, come su Google Calendar. */
    draft: EventDraft?,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit,
    onSlotTap: ((LocalDateTime) -> Unit)?,
    onDayChange: (LocalDate) -> Unit
) {
    val zone = AppSettings.zoneId
    // "Adesso", rinfrescato allo scoccare di ogni minuto.
    val now by produceState(LocalDateTime.now(zone), zone) {
        while (true) {
            delay(60_000L - System.currentTimeMillis() % 60_000L)
            value = LocalDateTime.now(zone)
        }
    }

    val allDay = remember(day, dated) { allDayOn(day, dated, zone) }
    if (allDay.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = DAY_LABEL_WIDTH, end = DAY_BLOCK_GAP_END, top = 4.dp, bottom = 4.dp)
        ) {
            allDay.forEach { (row, _) ->
                AllDayItem(row = row, onOpen = { onOpenRow(row) }, onLongPress = { onRowLongPress(row) })
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
        HorizontalDivider()
    }

    val density = LocalDensity.current
    val hourPx = with(density) { DAY_HOUR_HEIGHT.toPx() }
    // Si apre un'ora prima di adesso: quello che interessa è quasi sempre
    // quello che viene, e un po' di quello appena passato.
    val scroll = rememberScrollState(((now.hour - 1).coerceAtLeast(0) * hourPx).toInt())
    // **La griglia finisce dove finisce lo schermo**: alla prima
    // impaginazione si guarda dove comincia e la si fa arrivare in fondo.
    // Con un'altezza fissa usciva di sotto, e le ore in basso si vedevano
    // solo spostando la pagina intera — che col dito sulla griglia non si
    // sposta, perché il dito scorre le ore. Si misura una volta sola: poi
    // la pagina si può scorrere senza che la griglia cambi altezza.
    // Più in basso di così (la vista in fondo a una pagina lunga) resta
    // alta almeno `DAY_MIN_VIEWPORT`.
    val screenHeight = screenHeightDp()
    var fitted by remember { mutableStateOf<Dp?>(null) }
    val viewport = fitted ?: (screenHeight - DAY_CHROME_HEIGHT).coerceIn(DAY_MIN_VIEWPORT, DAY_HOUR_HEIGHT * 24)
    val rootHeight = rememberRootHeight()
    val bottomInset = WindowInsets.navigationBars.getBottom(density)

    val origin = dayPagerOrigin(day)
    fun pageOf(date: LocalDate) = DAY_PAGER_CENTER + ChronoUnit.DAYS.between(origin, date).toInt()
    fun dayOf(page: Int) = origin.plusDays((page - DAY_PAGER_CENTER).toLong())
    // Il pager rinasce quando cambia il giorno da cui conta (vedi `dayPagerOrigin`).
    key(origin) {
        val pagerState = rememberPagerState(initialPage = pageOf(day), pageCount = { DAY_PAGER_PAGES })

        // Come la settimana (`CalendarWeekSwiper`): il giorno cambia a
        // scorrimento finito, e se cambia da fuori (frecce, Today) il pager ci
        // salta senza animazione.
        LaunchedEffect(pagerState.settledPage) {
            val settled = dayOf(pagerState.settledPage)
            if (settled != day) onDayChange(settled)
        }
        LaunchedEffect(day) {
            val target = pageOf(day)
            if (pagerState.currentPage != target && !pagerState.isScrollInProgress) {
                pagerState.scrollToPage(target)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(viewport)
                .onGloballyPositioned { coordinates ->
                    if (fitted == null) {
                        val top = coordinates.positionInWindow().y
                        val free = with(density) { (rootHeight() - bottomInset - top).toDp() } - DAY_BOTTOM_MARGIN
                        fitted = free.coerceIn(DAY_MIN_VIEWPORT, DAY_HOUR_HEIGHT * 24)
                    }
                }
        ) {
            Row(modifier = Modifier.fillMaxSize().verticalScroll(scroll)) {
                HourLabels(modifier = Modifier.width(DAY_LABEL_WIDTH).height(DAY_HOUR_HEIGHT * 24))
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).height(DAY_HOUR_HEIGHT * 24),
                    verticalAlignment = Alignment.Top,
                    beyondViewportPageCount = 1
                ) { page ->
                    val shown = dayOf(page)
                    val blocks = remember(shown, dated) { dayBlocks(shown, dated, zone) }
                    DayColumn(
                        day = shown,
                        blocks = blocks,
                        nowMinute = if (now.toLocalDate() == shown) now.hour * 60 + now.minute else null,
                        draft = draft?.takeIf { it.rowId == null && !it.allDay && it.start.toLocalDate() == shown },
                        onOpenRow = onOpenRow,
                        onRowLongPress = onRowLongPress,
                        onSlotTap = onSlotTap
                    )
                }
            }
        }
    }
}

/** Le ore a sinistra della griglia, all'altezza della loro linea. */
@Composable
private fun HourLabels(modifier: Modifier) {
    Box(modifier = modifier) {
        for (hour in 1..23) {
            Text(
                text = LocalTime.of(hour, 0).format(Formats.hourLabelFormatter()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = DAY_HOUR_HEIGHT * hour - 8.dp)
                    .padding(end = 8.dp)
            )
        }
    }
}

/**
 * Un giorno della griglia: le linee delle ore e delle mezz'ore, i
 * quadratini da toccare sotto, i blocchi sopra.
 *
 * I quadratini stanno in uno strato **sotto** i blocchi, come i giorni
 * della settimana sotto le barre (`CalendarWeekRow`): un tocco su un
 * blocco lo prende il blocco, uno sul vuoto la griglia.
 */
@Composable
private fun DayColumn(
    day: LocalDate,
    blocks: List<DayBlock>,
    nowMinute: Int?,
    draft: EventDraft?,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit,
    onSlotTap: ((LocalDateTime) -> Unit)?
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val hourPx = with(LocalDensity.current) { DAY_HOUR_HEIGHT.toPx() }
    val currentSlotTap by rememberUpdatedState(onSlotTap)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(DAY_HOUR_HEIGHT * 24)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(lineColor, Offset(0f, 0f), Offset(0f, size.height), stroke)
                for (hour in 0..24) {
                    val y = hour * hourPx
                    drawLine(lineColor, Offset(0f, y), Offset(size.width, y), stroke)
                    if (hour < 24) {
                        val half = y + hourPx / 2
                        drawLine(lineColor.copy(alpha = 0.35f), Offset(0f, half), Offset(size.width, half), stroke)
                    }
                }
            }
    ) {
        if (onSlotTap != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(day) {
                        detectTapGestures(onTap = { offset ->
                            val minute = slotStartMinute(offset.y / hourPx * 60f)
                            currentSlotTap?.invoke(day.atStartOfDay().plusMinutes(minute.toLong()))
                        })
                    }
            )
        }

        val usable = maxWidth - DAY_BLOCK_GAP_END
        blocks.forEach { block ->
            val width = usable / block.columns
            val minutes = (block.endMinute - block.startMinute).coerceAtLeast(EVENT_MIN_VISIBLE_MINUTES)
            EventBlock(
                block = block,
                onOpen = { onOpenRow(block.row) },
                onLongPress = { onRowLongPress(block.row) },
                modifier = Modifier
                    .offset(x = width * block.column, y = DAY_HOUR_HEIGHT * (block.startMinute / 60f))
                    .width(width)
                    .height(DAY_HOUR_HEIGHT * (minutes / 60f))
                    .padding(start = 1.dp, end = 1.dp, bottom = 1.dp)
            )
        }

        if (draft != null) {
            val startMinute = draft.start.hour * 60 + draft.start.minute
            val minutes = ChronoUnit.MINUTES.between(draft.start, minOf(draft.end, draft.start.toLocalDate().plusDays(1).atStartOfDay()))
                .toInt().coerceAtLeast(EVENT_MIN_VISIBLE_MINUTES)
            DraftBlock(
                draft = draft,
                modifier = Modifier
                    .offset(y = DAY_HOUR_HEIGHT * (startMinute / 60f))
                    .width(usable)
                    .height(DAY_HOUR_HEIGHT * (minutes / 60f))
                    .padding(start = 1.dp, end = 1.dp, bottom = 1.dp)
            )
        }

        if (nowMinute != null) {
            val y = DAY_HOUR_HEIGHT * (nowMinute / 60f)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        val at = y.toPx()
                        drawLine(NOW_LINE_COLOR, Offset(0f, at), Offset(size.width, at), 2.dp.toPx())
                        drawCircle(NOW_LINE_COLOR, 5.dp.toPx(), Offset(5.dp.toPx(), at))
                    }
            )
        }
    }
}

/** "09:00 – 10:00", le ore vere dell'evento (non quelle tagliate sul giorno); senza fine, solo l'inizio. */
private fun eventTimeText(range: DateRange): String {
    val zone = AppSettings.zoneId
    val start = Instant.ofEpochMilli(range.start).atZone(zone).toLocalTime().format(EVENT_TIME_FORMAT)
    val end = range.end?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalTime().format(EVENT_TIME_FORMAT) }
    return if (end == null) start else "$start – $end"
}

/**
 * Un evento nella griglia. Alto abbastanza, il nome sopra e le ore sotto;
 * basso, tutto su una riga, come fa Google Calendar con gli eventi corti.
 */
@Composable
private fun EventBlock(
    block: DayBlock,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier
) {
    val shape = RoundedCornerShape(4.dp)
    val content = eventContentColor(block.row.eventColor)
    val time = eventTimeText(block.range)
    Box(
        modifier = modifier
            .clip(shape)
            .background(eventBackground(block.row.eventColor))
            .foundInside(block.row.id, shape)
            .onRightClick { onLongPress() }
            .pointerInput(block.row.id) {
                detectTapGestures(onTap = { onOpen() }, onLongPress = { onLongPress() })
            }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        if (block.endMinute - block.startMinute >= 45) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RowIconBadge(block.row.id, 12.dp)
                    Text(
                        text = highlightedTitle(block.row.title),
                        style = MaterialTheme.typography.labelMedium,
                        color = content,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall,
                    color = content.copy(alpha = 0.85f),
                    maxLines = 1
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RowIconBadge(block.row.id, 12.dp)
                Text(
                    text = highlightedTitle(block.row.title),
                    style = MaterialTheme.typography.labelSmall,
                    color = content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = ", $time",
                    style = MaterialTheme.typography.labelSmall,
                    color = content.copy(alpha = 0.85f),
                    maxLines = 1
                )
            }
        }
    }
}

/** L'evento non ancora salvato, nel quadratino toccato: "(No title)" finché non gli si dà un nome. */
@Composable
private fun DraftBlock(draft: EventDraft, modifier: Modifier) {
    val shape = RoundedCornerShape(4.dp)
    val content = eventContentColor(draft.color)
    Column(
        modifier = modifier
            .shadow(4.dp, shape)
            .clip(shape)
            .background(eventBackground(draft.color))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = draft.title.ifBlank { DbStrings.noTitle },
            style = MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${draft.start.format(EVENT_TIME_FORMAT)} – ${draft.end.format(EVENT_TIME_FORMAT)}",
            style = MaterialTheme.typography.labelSmall,
            color = content.copy(alpha = 0.85f),
            maxLines = 1
        )
    }
}

/** Una pagina della striscia in cima al giorno: senza ora, o lunga un giorno o più. */
@Composable
private fun AllDayItem(row: DatabaseRowEntity, onOpen: () -> Unit, onLongPress: () -> Unit) {
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DAY_ALL_DAY_HEIGHT)
            .clip(shape)
            .background(eventBackground(row.eventColor))
            .foundInside(row.id, shape)
            .onRightClick { onLongPress() }
            .pointerInput(row.id) {
                detectTapGestures(onTap = { onOpen() }, onLongPress = { onLongPress() })
            }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowIconBadge(row.id, 12.dp)
        Text(
            text = highlightedTitle(row.title),
            style = MaterialTheme.typography.labelMedium,
            color = eventContentColor(row.eventColor),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Quale finestrella è aperta sopra quella dell'evento. */
private enum class EventPick { START_DATE, START_TIME, END_DATE, END_TIME, REPEAT, CUSTOM_REPEAT }

/**
 * **La finestra rapida di un evento** (la seconda foto di Google Calendar
 * mandata dall'utente): si apre toccando un quadratino vuoto del giorno.
 * Il nome da scrivere (la tastiera si apre da sola), quando — giorno, ora
 * di inizio e di fine, ognuno si tocca per cambiarlo —, se si ripete, il
 * colore, e sotto "More options" e "Save".
 */
@Composable
fun EventQuickDialog(
    draft: EventDraft,
    onChange: (EventDraft) -> Unit,
    onSave: () -> Unit,
    onMoreOptions: () -> Unit,
    onDismiss: () -> Unit
) {
    var pick by remember { mutableStateOf<EventPick?>(null) }
    var colorsOpen by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        ProvidePopupTextToolbar {
            Surface(shape = RoundedCornerShape(20.dp), color = DarkSheet) {
                Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = EditorStrings.close)
                        }
                    }
                    EventTitleField(
                        value = draft.title,
                        onValueChange = { onChange(draft.copy(title = it)) },
                        autoFocus = true,
                        textStyle = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    EventIconRow(icon = { Icon(Icons.Filled.Schedule, contentDescription = null) }) {
                        Column {
                            PickText(capitalized(draft.start.format(EVENT_DAY_FORMAT))) { pick = EventPick.START_DATE }
                            if (draft.allDay) {
                                Text(
                                    text = DbStrings.allDay,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PickText(draft.start.format(EVENT_TIME_FORMAT)) { pick = EventPick.START_TIME }
                                    Text(" – ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    PickText(draft.end.format(EVENT_TIME_FORMAT)) { pick = EventPick.END_TIME }
                                    // Un evento che finisce un altro giorno lo dice.
                                    if (draft.end.toLocalDate() != draft.start.toLocalDate()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        PickText(capitalized(draft.end.format(EVENT_DATE_FORMAT))) { pick = EventPick.END_DATE }
                                    }
                                }
                            }
                            PickText(
                                text = recurrenceSummary(draft.recurrence, draft.start.toLocalDate()),
                                muted = draft.recurrence == null
                            ) { pick = EventPick.REPEAT }
                        }
                    }

                    EventIconRow(icon = { Icon(Icons.Filled.Palette, contentDescription = null) }) {
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { colorsOpen = true }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ColorDot(draft.color, size = 16)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = eventColorName(draft.color),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            DropdownMenu(expanded = colorsOpen, onDismissRequest = { colorsOpen = false }) {
                                EventColorPalette(
                                    selected = draft.color,
                                    onPick = {
                                        onChange(draft.copy(color = it))
                                        colorsOpen = false
                                    },
                                    modifier = Modifier.width(236.dp).padding(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onMoreOptions) { Text(DbStrings.moreOptions) }
                        Spacer(modifier = Modifier.weight(1f))
                        Button(onClick = onSave, modifier = Modifier.padding(end = 8.dp)) { Text(EditorStrings.save) }
                    }
                }
            }
        }
    }
    EventPickers(pick = pick, draft = draft, onChange = onChange, onPick = { pick = it })
}

/**
 * **"More options"** (la terza foto di Google Calendar): l'evento per
 * intero. Il nome; "All day"; inizio e fine, giorno e ora; la
 * ripetizione con le scelte di Google Calendar (le stesse della proprietà
 * Date: "Daily", "Weekly on…", "Monthly on…", "Annually on…", "Every
 * weekday", "Custom…"); la notifica; il colore.
 *
 * Si apre anche da "Edit event", tenendo premuta una pagina del calendario.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorSheet(
    draft: EventDraft,
    onChange: (EventDraft) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var pick by remember { mutableStateOf<EventPick?>(null) }
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        // Come nella finestra della data: i pulsanti in cima, sennò sotto
        // a tutto finirebbero fuori schermo.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = EditorStrings.close)
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = onSave, modifier = Modifier.padding(end = 8.dp)) { Text(EditorStrings.save) }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Box(modifier = Modifier.padding(start = 40.dp, end = 16.dp)) {
                EventTitleField(
                    value = draft.title,
                    onValueChange = { onChange(draft.copy(title = it)) },
                    autoFocus = false,
                    textStyle = MaterialTheme.typography.headlineSmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()

            EditorLine(icon = { Icon(Icons.Filled.Schedule, contentDescription = null) }, onClick = { onChange(draft.withAllDay(!draft.allDay)) }) {
                Text(
                    text = DbStrings.allDay,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = draft.allDay, onCheckedChange = { onChange(draft.withAllDay(it)) })
            }
            DateTimeLine(
                date = draft.start.toLocalDate(),
                time = draft.start.toLocalTime().takeUnless { draft.allDay },
                onDate = { pick = EventPick.START_DATE },
                onTime = { pick = EventPick.START_TIME }
            )
            DateTimeLine(
                date = draft.end.toLocalDate(),
                time = draft.end.toLocalTime().takeUnless { draft.allDay },
                onDate = { pick = EventPick.END_DATE },
                onTime = { pick = EventPick.END_TIME }
            )

            HorizontalDivider()
            EditorLine(icon = { Icon(Icons.Filled.Repeat, contentDescription = null) }, onClick = { pick = EventPick.REPEAT }) {
                Text(
                    text = recurrenceSummary(draft.recurrence, draft.start.toLocalDate()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()
            val toggleNotify = { on: Boolean ->
                onChange(draft.copy(notify = on, reminders = draft.reminders.ifEmpty { listOf(0) }))
                if (on) NotificationPermission.request()
            }
            EditorLine(icon = { Icon(Icons.Filled.Notifications, contentDescription = null) }, onClick = { toggleNotify(!draft.notify) }) {
                Text(
                    text = DbStrings.includeNotification,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = draft.notify, onCheckedChange = { toggleNotify(it) })
            }
            // Più notifiche, come su Google Calendar (dal 26/10/2026): vedi `ReminderList`.
            if (draft.notify) {
                ReminderList(
                    reminders = draft.reminders,
                    atStartLabel = DbStrings.notificationWhen(hasTime = !draft.allDay, hasEnd = true),
                    onChange = { changed ->
                        onChange(
                            if (changed.isEmpty()) draft.copy(notify = false, reminders = listOf(0))
                            else draft.copy(reminders = changed)
                        )
                    },
                    indent = 56.dp
                )
            }

            HorizontalDivider()
            EditorLine(icon = { Icon(Icons.Filled.Palette, contentDescription = null) }, onClick = null) {
                Text(
                    text = DbStrings.color,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = eventColorName(draft.color),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            EventColorPalette(
                selected = draft.color,
                onPick = { onChange(draft.copy(color = it)) },
                modifier = Modifier.fillMaxWidth().padding(start = 56.dp, end = 16.dp, bottom = 8.dp)
            )
        }
    }
    EventPickers(pick = pick, draft = draft, onChange = onChange, onPick = { pick = it })
}

/** Il nome dell'evento, sottolineato come su Google Calendar; l'Invio chiude la tastiera. */
@Composable
private fun EventTitleField(
    value: String,
    onValueChange: (String) -> Unit,
    autoFocus: Boolean,
    textStyle: TextStyle
) {
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    AutoCaps {
        TextField(
            value = value,
            onValueChange = { onValueChange(it.replace("\n", "")) },
            placeholder = { Text(DbStrings.addTitle, style = textStyle) },
            textStyle = textStyle,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth().focusRequester(focus)
        )
    }
    if (autoFocus) {
        LaunchedEffect(Unit) { focus.requestFocus() }
    }
}

/** Una riga della finestra rapida: l'icona a sinistra, quello che dice a destra. */
@Composable
private fun EventIconRow(icon: @Composable () -> Unit, content: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Box(modifier = Modifier.width(48.dp).padding(top = 4.dp), contentAlignment = Alignment.TopCenter) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                content = icon
            )
        }
        content()
    }
}

/** Una scritta da toccare per cambiarla: il giorno, un'ora, la ripetizione. */
@Composable
private fun PickText(text: String, muted: Boolean = false, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 5.dp)
    )
}

/** Una riga di "More options", con l'icona a sinistra come su Google Calendar. */
@Composable
private fun EditorLine(
    icon: @Composable () -> Unit,
    onClick: (() -> Unit)?,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(40.dp)) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                content = icon
            )
        }
        content()
    }
}

/** Il giorno a sinistra e l'ora a destra (senza, con "All day"): si toccano per cambiarli. */
@Composable
private fun DateTimeLine(date: LocalDate, time: LocalTime?, onDate: () -> Unit, onTime: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 52.dp, end = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = capitalized(date.format(EVENT_DATE_FORMAT)),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .clickable { onDate() }
                .padding(horizontal = 4.dp, vertical = 10.dp)
        )
        if (time != null) {
            Text(
                text = time.format(EVENT_TIME_FORMAT),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onTime() }
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            )
        }
    }
}

/** Il pallino del colore; quello di sempre ha il bordino, sennò sullo sfondo non si vedrebbe. */
@Composable
private fun ColorDot(hex: String?, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(eventBackground(hex))
            .then(
                if (hex == null) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape) else Modifier
            )
    )
}

/** I colori di Google Calendar, e per primo quello di sempre; quello scelto ha la spunta. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventColorPalette(selected: String?, onPick: (String?) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        (listOf<String?>(null) + EVENT_COLORS.map { it.hex }).forEach { hex ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(eventBackground(hex))
                    .then(
                        if (hex == null) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape) else Modifier
                    )
                    .clickable { onPick(hex) },
                contentAlignment = Alignment.Center
            ) {
                if (hex == selected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = eventColorName(hex),
                        tint = eventContentColor(hex),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Le finestrelle sopra quella dell'evento: il giorno e l'ora (quelle di
 * Samsung, le stesse della proprietà Date) e la ripetizione.
 */
@Composable
private fun EventPickers(
    pick: EventPick?,
    draft: EventDraft,
    onChange: (EventDraft) -> Unit,
    onPick: (EventPick?) -> Unit
) {
    val close = { onPick(null) }
    when (pick) {
        EventPick.START_DATE -> SamsungDateDialog(
            initial = draft.start.toLocalDate(),
            onPick = { day -> onChange(draft.withStart(day.atTime(draft.start.toLocalTime()))) },
            onDismiss = close
        )
        EventPick.START_TIME -> SamsungTimeDialog(
            initial = draft.start.toLocalTime(),
            onPick = { time -> onChange(draft.withStart(draft.start.toLocalDate().atTime(time))) },
            onDismiss = close
        )
        EventPick.END_DATE -> SamsungDateDialog(
            initial = draft.end.toLocalDate(),
            onPick = { day -> onChange(draft.withEnd(day.atTime(draft.end.toLocalTime()))) },
            onDismiss = close
        )
        EventPick.END_TIME -> SamsungTimeDialog(
            initial = draft.end.toLocalTime(),
            onPick = { time -> onChange(draft.withEnd(draft.end.toLocalDate().atTime(time))) },
            onDismiss = close
        )
        EventPick.REPEAT -> RepeatPresetDialog(
            start = draft.start.toLocalDate(),
            current = draft.recurrence,
            onDismiss = close,
            onPick = { rule ->
                onChange(draft.copy(recurrence = rule))
                close()
            },
            onCustom = { onPick(EventPick.CUSTOM_REPEAT) }
        )
        EventPick.CUSTOM_REPEAT -> CustomRecurrenceDialog(
            start = draft.start.toLocalDate(),
            initial = draft.recurrence,
            onDismiss = close,
            onDone = { rule ->
                onChange(draft.copy(recurrence = rule))
                close()
            }
        )
        null -> Unit
    }
}
