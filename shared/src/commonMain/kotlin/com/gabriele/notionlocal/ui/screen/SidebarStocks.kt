package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.widgets.MAX_STOCKS
import com.gabriele.notionlocal.data.widgets.StockItem
import com.gabriele.notionlocal.data.widgets.StockQuote
import com.gabriele.notionlocal.data.widgets.StockResult
import com.gabriele.notionlocal.data.widgets.StocksApi
import com.gabriele.notionlocal.data.widgets.StocksUpdates
import com.gabriele.notionlocal.data.widgets.StocksWidget
import com.gabriele.notionlocal.data.widgets.WidgetStore
import com.gabriele.notionlocal.data.widgets.displaySymbol
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.i18n.WidgetStrings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

// ---------------------------------------------------------------------------
// **Il widget della borsa** (dal 17/10/2026, chiesto dall'utente con la foto
// di una lista): sigla e nome, variazione del giorno e prezzo, nell'ordine
// scelto. Un tocco apre i dettagli del titolo; tenendo premuto si aggiungono,
// si spostano e si tolgono i titoli. Vedi `StocksWidget`.
// ---------------------------------------------------------------------------

/** Verde e rosso delle variazioni, come nella foto dell'utente. */
private val UpGreen = Color(0xFF3FB950)
private val DownRed = Color(0xFFE5484D)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StocksView(
    widget: StocksWidget,
    active: Boolean,
    managing: Boolean,
    onManagingChange: (Boolean) -> Unit
) {
    var detailFor by remember(widget.id) { mutableStateOf<String?>(null) }
    var adding by remember(widget.id) { mutableStateOf(false) }

    // Con la barra aperta, ogni minuto si guarda se sono passati i cinque
    // minuti dall'ultimo aggiornamento.
    val latest by rememberUpdatedState(widget)
    LaunchedEffect(active, widget.items.map { it.symbol }) {
        while (active) {
            StocksUpdates.refresh(latest)
            delay(60_000)
        }
    }

    if (widget.items.isEmpty()) {
        AddRow(label = WidgetStrings.addStock, onClick = { adding = true })
    } else {
        val failed = widget.id in StocksUpdates.failed
        widget.items.forEach { item ->
            StockRow(
                item = item,
                quote = widget.quotes[item.symbol],
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .onRightClick({ onManagingChange(true) })
                    .combinedClickable(
                        onClick = { detailFor = item.symbol },
                        onLongClick = { onManagingChange(true) }
                    )
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        val updated = widget.fetchedAt?.let { WidgetStrings.updatedAt(clockOfStocks(it)) }
        val line = listOfNotNull(updated, WidgetStrings.noConnection.takeIf { failed }).joinToString(" · ")
        if (line.isNotEmpty()) Text(line, color = NotionGray400, fontSize = 10.sp)
    }

    detailFor?.let { symbol ->
        val item = widget.items.find { it.symbol == symbol }
        if (item == null) detailFor = null else StockSheet(item, widget.quotes[symbol], onDismiss = { detailFor = null })
    }
    if (managing) StockListSheet(widget, startWithSearch = false, onDismiss = { onManagingChange(false) })
    if (adding) StockListSheet(widget, startWithSearch = true, onDismiss = { adding = false })
}

/** Una riga come nella foto: sigla e variazione sopra, nome e prezzo sotto. */
@Composable
private fun StockRow(item: StockItem, quote: StockQuote?, modifier: Modifier) {
    Column(modifier = modifier.padding(vertical = 5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                displaySymbol(item.symbol),
                color = NotionWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            if (quote != null) {
                Text(changeText(quote.changePercent), color = changeColor(quote.changePercent), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.name, color = NotionGray400, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            Text(quote?.let { priceText(it.price, it.decimals) } ?: "—", color = NotionWhite, fontSize = 12.sp)
        }
    }
}

/** "+0,94%", "-0,29%": col separatore dei decimali di Settings. */
private fun changeText(percent: Double): String {
    val rounded = BigDecimal.valueOf(percent).setScale(2, RoundingMode.HALF_UP)
    val sign = if (rounded.signum() > 0) "+" else ""
    return sign + Formats.number(rounded.toPlainString()) + "%"
}

private fun changeColor(percent: Double): Color = when {
    BigDecimal.valueOf(percent).setScale(2, RoundingMode.HALF_UP).signum() > 0 -> UpGreen
    BigDecimal.valueOf(percent).setScale(2, RoundingMode.HALF_UP).signum() < 0 -> DownRed
    else -> NotionGray400
}

/** Il prezzo con le sue cifre decimali, e i separatori di Settings ("49859,75"). */
private fun priceText(price: Double, decimals: Int): String =
    Formats.number(BigDecimal.valueOf(price).setScale(decimals, RoundingMode.HALF_UP).toPlainString())

private fun clockOfStocks(millis: Long): String =
    Formats.timeFormatter().format(Instant.ofEpochMilli(millis).atZone(AppSettings.zoneId))

// --- I dettagli di un titolo ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockSheet(item: StockItem, quote: StockQuote?, onDismiss: () -> Unit) {
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text(displaySymbol(item.symbol), color = NotionWhite, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(item.name, color = NotionGray400, style = MaterialTheme.typography.bodyMedium)
            if (quote == null) {
                Text(WidgetStrings.noConnection, color = NotionGray400, modifier = Modifier.padding(vertical = 24.dp))
                return@Column
            }
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 12.dp)) {
                Text(priceText(quote.price, quote.decimals), color = NotionWhite, fontSize = 34.sp, fontWeight = FontWeight.Light)
                Spacer(modifier = Modifier.width(6.dp))
                Text(quote.currency, color = NotionGray400, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 6.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    changeText(quote.changePercent),
                    color = changeColor(quote.changePercent),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            fun range(low: Double?, high: Double?): String? =
                if (low != null && high != null) "${priceText(low, quote.decimals)} – ${priceText(high, quote.decimals)}" else null
            listOfNotNull(
                quote.previousClose?.let { WidgetStrings.previousClose to priceText(it, quote.decimals) },
                range(quote.dayLow, quote.dayHigh)?.let { WidgetStrings.dayRange to it },
                range(quote.yearLow, quote.yearHigh)?.let { WidgetStrings.yearRange to it },
                // Data e ora come nel resto dell'app (Settings), non all'americana.
                quote.time?.let { WidgetStrings.lastTrade to Formats.dateTime(it) }
            ).forEach { (label, value) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(label, color = NotionGray400, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(value, color = NotionWhite, style = MaterialTheme.typography.bodyMedium)
                }
                HorizontalDivider()
            }
            Text(
                listOf(quote.exchange, WidgetStrings.stocksSource).filter { it.isNotBlank() }.joinToString(" · "),
                color = NotionGray400,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
            )
        }
    }
}

// --- I titoli del widget ---

/**
 * **I titoli**: "Add a stock" in cima, e per ognuno, dal menu ⋮, Move up,
 * Move down, Remove. [startWithSearch]: si apre già sulla ricerca.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockListSheet(widget: StocksWidget, startWithSearch: Boolean, onDismiss: () -> Unit) {
    var searching by remember { mutableStateOf(startWithSearch) }
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
            if (searching) {
                StockSearch(
                    widget = widget,
                    onBack = if (startWithSearch && widget.items.isEmpty()) null else ({ searching = false }),
                    onPick = { result ->
                        WidgetStore.addStock(widget.id, result.toItem())
                        if (startWithSearch) onDismiss() else searching = false
                    }
                )
            } else {
                Text(WidgetStrings.stockList, color = NotionWhite, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(WidgetStrings.stockListNote, color = NotionGray400, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                if (widget.items.size < MAX_STOCKS) AddRow(label = WidgetStrings.addStock, onClick = { searching = true })
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                    items(widget.items, key = { it.symbol }) { item ->
                        val index = widget.items.indexOf(item)
                        StockListRow(
                            item = item,
                            index = index,
                            count = widget.items.size,
                            onMove = { delta -> WidgetStore.moveStock(widget.id, item.symbol, delta) },
                            onRemove = { WidgetStore.removeStock(widget.id, item.symbol) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun StockListRow(item: StockItem, index: Int, count: Int, onMove: (Int) -> Unit, onRemove: () -> Unit) {
    var menu by remember(item.symbol) { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${index + 1}", color = NotionGray400, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(28.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(displaySymbol(item.symbol), color = NotionWhite, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(item.name, color = NotionGray400, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = WidgetStrings.widgetOptions, tint = NotionGray400)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(WidgetStrings.moveUp) }, enabled = index > 0, onClick = { menu = false; onMove(-1) })
                DropdownMenuItem(text = { Text(WidgetStrings.moveDown) }, enabled = index < count - 1, onClick = { menu = false; onMove(1) })
                DropdownMenuItem(
                    text = { Text(WidgetStrings.removeStock, color = MaterialTheme.colorScheme.error) },
                    onClick = { menu = false; onRemove() }
                )
            }
        }
    }
}

/** La ricerca dei titoli per nome o sigla: azioni, indici, cambi, fondi. */
@Composable
private fun StockSearch(widget: StocksWidget, onBack: (() -> Unit)?, onPick: (StockResult) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<StockResult>?>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
            searching = false
            return@LaunchedEffect
        }
        searching = true
        delay(400)
        results = StocksApi.search(query, AppSettings.language.locale.language)
        searching = false
    }
    val added = widget.items.map { it.symbol }.toSet()

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = Strings.back, tint = NotionWhite) }
        }
        Text(WidgetStrings.addStock, color = NotionWhite, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    Spacer(modifier = Modifier.height(8.dp))
    AutoCaps {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text(WidgetStrings.searchStock) },
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
        found.isEmpty() && query.isNotBlank() && !searching ->
            Text(WidgetStrings.noStockFound, color = NotionGray400, modifier = Modifier.padding(8.dp))
        else -> LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
            items(found, key = { it.symbol }) { result ->
                val already = result.symbol in added
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !already) { onPick(result) }
                        .padding(horizontal = 8.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${displaySymbol(result.symbol)}  ·  ${result.symbol}",
                            color = if (already) NotionGray400 else NotionWhite,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(result.name, color = NotionGray400, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOf(result.exchange, result.type).filter { it.isNotBlank() }.joinToString(" · "),
                            color = NotionGray400,
                            fontSize = 11.sp
                        )
                    }
                    if (already) Icon(Icons.Filled.Check, contentDescription = null, tint = NotionGray400)
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}
