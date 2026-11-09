package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import java.util.Locale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.widgets.CityResult
import com.gabriele.notionlocal.data.widgets.MAX_WEATHER_CITIES
import com.gabriele.notionlocal.data.widgets.WeatherApi
import com.gabriele.notionlocal.data.widgets.WeatherCity
import com.gabriele.notionlocal.data.widgets.WeatherDay
import com.gabriele.notionlocal.data.widgets.WeatherHour
import com.gabriele.notionlocal.data.widgets.WeatherReport
import com.gabriele.notionlocal.data.widgets.WeatherSky
import com.gabriele.notionlocal.data.widgets.WeatherUpdates
import com.gabriele.notionlocal.data.widgets.WeatherWidget
import com.gabriele.notionlocal.data.widgets.WidgetStore
import com.gabriele.notionlocal.data.widgets.daysFrom
import com.gabriele.notionlocal.data.widgets.hoursFrom
import com.gabriele.notionlocal.data.widgets.temperatureText
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.i18n.WidgetStrings
import com.gabriele.notionlocal.ui.theme.AccentBlue
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle

// ---------------------------------------------------------------------------
// **Il widget del meteo** (dal 17/10/2026, chiesto dall'utente): una pagina
// per città, che si sfogliano. Un tocco apre il tempo ora per ora; tenendo
// premuto si aggiungono, cambiano e tolgono le città (fino a dieci, la prima
// è la principale). Vedi `WeatherWidget`.
// ---------------------------------------------------------------------------

/**
 * Le pagine delle città. [managing] apre la finestra delle città: la tiene
 * la cornice del widget, che ha la stessa voce nei suoi tre puntini.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeatherView(
    widget: WeatherWidget,
    active: Boolean,
    now: Long,
    managing: Boolean,
    onManagingChange: (Boolean) -> Unit
) {
    var hourlyFor by remember(widget.id) { mutableStateOf<String?>(null) }
    var adding by remember(widget.id) { mutableStateOf(false) }

    // Si riscarica solo con la barra aperta, e solo quello che ha più di
    // mezz'ora: ogni minuto si guarda se è ora (vedi `WeatherUpdates`).
    val latest by rememberUpdatedState(widget)
    LaunchedEffect(active, widget.cities.map { it.id }) {
        while (active) {
            WeatherUpdates.refresh(latest)
            delay(60_000)
        }
    }

    if (widget.cities.isEmpty()) {
        AddRow(label = WidgetStrings.addCity, onClick = { adding = true })
    } else {
        val pagerState = rememberPagerState(pageCount = { widget.cities.size })
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            HorizontalPager(
                state = pagerState,
                key = { page -> widget.cities.getOrNull(page)?.id ?: page },
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val city = widget.cities.getOrNull(page) ?: return@HorizontalPager
                CityWeatherPage(
                    city = city,
                    report = widget.reports[city.id],
                    now = now,
                    failed = city.id in WeatherUpdates.failed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .onRightClick({ onManagingChange(true) })
                        .combinedClickable(
                            onClick = { hourlyFor = city.id },
                            onLongClick = { onManagingChange(true) }
                        )
                )
            }
            if (widget.cities.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(widget.cities.size) { i ->
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (i == pagerState.currentPage) NotionWhite else NotionGray400.copy(alpha = 0.4f))
                        )
                    }
                }
            }
        }
    }

    hourlyFor?.let { cityId ->
        val city = widget.cities.find { it.id == cityId }
        if (city == null) {
            hourlyFor = null
        } else {
            HourlySheet(city = city, report = widget.reports[city.id], now = now, onDismiss = { hourlyFor = null })
        }
    }
    if (managing) {
        CitiesSheet(widget = widget, startWithSearch = false, onDismiss = { onManagingChange(false) })
    }
    if (adding) {
        CitiesSheet(widget = widget, startWithSearch = true, onDismiss = { adding = false })
    }
}

/** Una città: com'è adesso, la massima e la minima di oggi, i prossimi giorni. */
@Composable
private fun CityWeatherPage(city: WeatherCity, report: WeatherReport?, now: Long, failed: Boolean, modifier: Modifier) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Text(
            text = city.name,
            color = NotionWhite,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (city.region.isNotEmpty()) {
            Text(city.region, color = NotionGray400, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (report == null) {
            Text(
                text = if (failed) WidgetStrings.noConnection else WidgetStrings.loading,
                color = NotionGray400,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            return@Column
        }
        val sky = WeatherSky.of(report.now.code)
        val days = report.daysFrom(now)
        val today = days.firstOrNull()
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            Text(sky.icon(report.now.isDay), fontSize = 34.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(temperatureText(report.now.temperature), color = NotionWhite, fontSize = 34.sp, fontWeight = FontWeight.Light)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(WidgetStrings.skyName(sky), color = NotionWhite, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                if (today != null) {
                    Text(
                        WidgetStrings.maxMin(temperatureText(today.max), temperatureText(today.min)),
                        color = NotionGray400,
                        fontSize = 12.sp
                    )
                }
            }
        }
        // I prossimi giorni, oggi compreso: cinque colonne.
        if (days.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                days.take(5).forEachIndexed { i, day -> DayColumn(day, first = i == 0, modifier = Modifier.weight(1f)) }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = WidgetStrings.updatedAt(clockOf(report.fetchedAt)) + if (failed) " · " + WidgetStrings.noConnection else "",
            color = NotionGray400,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun DayColumn(day: WeatherDay, first: Boolean, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (first) WidgetStrings.today else weekdayOf(day.date), color = NotionGray400, fontSize = 11.sp, maxLines = 1)
        Text(WeatherSky.of(day.code).icon(isDay = true), fontSize = 18.sp)
        Text(temperatureText(day.max), color = NotionWhite, fontSize = 12.sp)
        Text(temperatureText(day.min), color = NotionGray400, fontSize = 12.sp)
    }
}

/** "sab", "Sat": il giorno della settimana, corto, nella lingua dell'app. */
private fun weekdayOf(date: String): String =
    runCatching { LocalDate.parse(date).dayOfWeek.getDisplayName(TextStyle.SHORT, AppSettings.language.locale) }.getOrDefault("")

/** L'ora del telefono a cui è arrivato il meteo, nel formato dell'ora di Settings. */
private fun clockOf(millis: Long): String =
    Formats.timeFormatter().format(Instant.ofEpochMilli(millis).atZone(AppSettings.zoneId))

// --- Ora per ora ---

/**
 * **Il tempo ora per ora** di una città: adesso (percepita, umidità, vento),
 * poi le prossime ore, con l'ora della città e la probabilità di pioggia,
 * divise per giorno; in fondo i prossimi giorni.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HourlySheet(city: WeatherCity, report: WeatherReport?, now: Long, onDismiss: () -> Unit) {
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            item {
                Text(city.name, color = NotionWhite, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (city.region.isNotEmpty()) Text(city.region, color = NotionGray400, style = MaterialTheme.typography.bodySmall)
            }
            if (report == null) {
                item {
                    Text(WidgetStrings.noConnection, color = NotionGray400, modifier = Modifier.padding(vertical = 24.dp))
                }
                return@LazyColumn
            }
            item {
                val sky = WeatherSky.of(report.now.code)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                    Text(sky.icon(report.now.isDay), fontSize = 44.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(temperatureText(report.now.temperature), color = NotionWhite, fontSize = 44.sp, fontWeight = FontWeight.Light)
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(WidgetStrings.skyName(sky), color = NotionWhite, style = MaterialTheme.typography.bodyLarge)
                }
                val details = listOfNotNull(
                    report.now.feelsLike?.let { WidgetStrings.feelsLike(temperatureText(it)) },
                    report.now.humidity?.let { WidgetStrings.humidity(it) },
                    report.now.wind?.let { WidgetStrings.wind(Math.round(it).toInt()) }
                )
                if (details.isNotEmpty()) {
                    Text(details.joinToString(" · "), color = NotionGray400, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(WidgetStrings.hourByHour, color = NotionGray400, style = MaterialTheme.typography.labelLarge)
            }
            val hours = report.hoursFrom(now)
            var lastDate: String? = null
            hours.forEach { hour ->
                val date = hour.time.substringBefore('T')
                if (date != lastDate) {
                    val label = dayHeading(date, report, now)
                    item(key = "day-$date") {
                        Text(
                            label,
                            color = NotionWhite,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    lastDate = date
                }
                item(key = hour.time) { HourRow(hour) }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(report.daysFrom(now), key = { "next-${it.date}" }) { day -> DayRow(day, report, now) }
            item {
                Text(
                    WidgetStrings.updatedAt(clockOf(report.fetchedAt)) + " · " + WidgetStrings.weatherSource,
                    color = NotionGray400,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
                )
            }
        }
    }
}

/** "Oggi", "Domani", poi "sabato 18": il giorno della città. */
private fun dayHeading(date: String, report: WeatherReport, now: Long): String {
    val day = runCatching { LocalDate.parse(date) }.getOrNull() ?: return date
    val today = report.daysFrom(now).firstOrNull()?.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    return when (day) {
        today -> WidgetStrings.today
        today?.plusDays(1) -> WidgetStrings.tomorrow
        else -> day.dayOfWeek.getDisplayName(TextStyle.FULL, AppSettings.language.locale)
            .replaceFirstChar { it.titlecase(AppSettings.language.locale) } + " ${day.dayOfMonth}"
    }
}

@Composable
private fun HourRow(hour: WeatherHour) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            runCatching { Formats.timeFormatter().format(LocalDateTime.parse(hour.time)) }.getOrDefault(hour.time),
            color = NotionWhite,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(72.dp)
        )
        Text(WeatherSky.of(hour.code).icon(hour.isDay), fontSize = 20.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            hour.rainChance?.takeIf { it > 0 }?.let { "$it%" }.orEmpty(),
            color = AccentBlue,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(temperatureText(hour.temperature), color = NotionWhite, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun DayRow(day: WeatherDay, report: WeatherReport, now: Long) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(dayHeading(day.date, report, now), color = NotionWhite, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(WeatherSky.of(day.code).icon(isDay = true), fontSize = 20.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            day.rainChance?.takeIf { it > 0 }?.let { "$it%" }.orEmpty(),
            color = AccentBlue,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(40.dp)
        )
        Text(temperatureText(day.max), color = NotionWhite, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(40.dp))
        Text(temperatureText(day.min), color = NotionGray400, style = MaterialTheme.typography.bodyMedium)
    }
}

// --- Le città ---

/** Cosa si sta facendo nella finestra delle città. */
private sealed interface CityTask {
    data object List : CityTask
    data object Add : CityTask
    data class Change(val cityId: String) : CityTask
}

/**
 * **Le città del widget**: aggiungerne una (in cima, sempre a portata), e per
 * ognuna — dal menu ⋮ — cambiarla, renderla principale, spostarla o
 * toglierla. [startWithSearch]: si apre già sulla ricerca, per la prima città.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CitiesSheet(widget: WeatherWidget, startWithSearch: Boolean, onDismiss: () -> Unit) {
    var task by remember { mutableStateOf<CityTask>(if (startWithSearch) CityTask.Add else CityTask.List) }
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp)
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            when (val current = task) {
                CityTask.List -> CityList(widget, onAdd = { task = CityTask.Add }, onChange = { task = CityTask.Change(it) })
                CityTask.Add, is CityTask.Change -> CitySearch(
                    title = if (current is CityTask.Change) WidgetStrings.changeCity else WidgetStrings.addCity,
                    onBack = if (startWithSearch && widget.cities.isEmpty()) null else ({ task = CityTask.List }),
                    onPick = { result ->
                        if (current is CityTask.Change) {
                            WidgetStore.replaceWeatherCity(widget.id, current.cityId, result.toCity())
                        } else {
                            WidgetStore.addWeatherCity(widget.id, result.toCity())
                        }
                        if (startWithSearch) onDismiss() else task = CityTask.List
                    }
                )
            }
        }
    }
}

@Composable
private fun CityList(widget: WeatherWidget, onAdd: () -> Unit, onChange: (String) -> Unit) {
    Text(WidgetStrings.weatherCities, color = NotionWhite, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text(WidgetStrings.weatherCitiesNote, color = NotionGray400, style = MaterialTheme.typography.bodySmall)
    Spacer(modifier = Modifier.height(8.dp))
    if (widget.cities.size < MAX_WEATHER_CITIES) {
        AddRow(label = WidgetStrings.addCity, onClick = onAdd)
    }
    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
        items(widget.cities, key = { it.id }) { city ->
            val index = widget.cities.indexOf(city)
            CityRow(
                city = city,
                index = index,
                count = widget.cities.size,
                onChange = { onChange(city.id) },
                onMove = { delta -> WidgetStore.moveWeatherCity(widget.id, city.id, delta) },
                onRemove = { WidgetStore.removeWeatherCity(widget.id, city.id) }
            )
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun CityRow(city: WeatherCity, index: Int, count: Int, onChange: () -> Unit, onMove: (Int) -> Unit, onRemove: () -> Unit) {
    var menu by remember(city.id) { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${index + 1}", color = NotionGray400, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(city.name, color = NotionWhite, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val under = listOfNotNull(WidgetStrings.mainCity.takeIf { index == 0 }, city.region.takeIf { it.isNotEmpty() })
            if (under.isNotEmpty()) {
                Text(under.joinToString(" · "), color = NotionGray400, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = WidgetStrings.widgetOptions, tint = NotionGray400)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(WidgetStrings.changeCity) }, onClick = { menu = false; onChange() })
                if (index > 0) {
                    DropdownMenuItem(text = { Text(WidgetStrings.makeMainCity) }, onClick = { menu = false; onMove(-index) })
                }
                DropdownMenuItem(text = { Text(WidgetStrings.moveUp) }, enabled = index > 0, onClick = { menu = false; onMove(-1) })
                DropdownMenuItem(text = { Text(WidgetStrings.moveDown) }, enabled = index < count - 1, onClick = { menu = false; onMove(1) })
                DropdownMenuItem(
                    text = { Text(WidgetStrings.removeCity, color = MaterialTheme.colorScheme.error) },
                    onClick = { menu = false; onRemove() }
                )
            }
        }
    }
}

/**
 * La ricerca di una città per nome: si scrive, e dopo un attimo arrivano le
 * città che si chiamano così, con regione e paese per distinguerle.
 */
@Composable
private fun CitySearch(title: String, onBack: (() -> Unit)?, onPick: (CityResult) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<CityResult>?>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    // La lingua dell'app e quella del telefono (deciso dall'utente il
    // 17/10/2026): con tutti e due in inglese si cerca "Milan"; con uno dei
    // due in italiano "Milano" trova Milano, Lombardia.
    val searchLanguages = remember { listOf(AppSettings.language.locale.language, Locale.getDefault().language).distinct() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(query) {
        if (query.trim().length < 2) {
            results = emptyList()
            searching = false
            return@LaunchedEffect
        }
        searching = true
        delay(350)
        results = WeatherApi.searchCities(query, searchLanguages)
        searching = false
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = Strings.back, tint = NotionWhite) }
        }
        Text(title, color = NotionWhite, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    Spacer(modifier = Modifier.height(8.dp))
    AutoCaps {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text(WidgetStrings.searchWeatherCity) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.fillMaxWidth().focusRequester(focus)
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    val found = results
    when {
        // Senza niente da mostrare intanto: la risposta può metterci qualche secondo.
        searching && found.isNullOrEmpty() -> Text(WidgetStrings.loading, color = NotionGray400, modifier = Modifier.padding(8.dp))
        found == null -> Text(WidgetStrings.noConnection, color = NotionGray400, modifier = Modifier.padding(8.dp))
        found.isEmpty() && query.trim().length >= 2 && !searching ->
            Text(WidgetStrings.noCityFound, color = NotionGray400, modifier = Modifier.padding(8.dp))
        else -> LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
            items(found) { result ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onPick(result) }
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Text(result.name, color = NotionWhite, style = MaterialTheme.typography.bodyLarge)
                    if (result.region.isNotEmpty()) {
                        Text(result.region, color = NotionGray400, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}
