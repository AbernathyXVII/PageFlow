package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.platform.emojiFontFamily
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import com.gabriele.notionlocal.data.normalizeForSearch
import com.gabriele.notionlocal.data.IconSymbol
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriele.notionlocal.data.EmojiCatalog
import com.gabriele.notionlocal.data.EmojiCatalogs
import com.gabriele.notionlocal.data.EmojiCategory
import com.gabriele.notionlocal.data.EmojiEntry
import com.gabriele.notionlocal.data.IconChoice
import com.gabriele.notionlocal.data.IconLibrary
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.SKIN_TONE_COUNT
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.withSkinTone
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.ui.i18n.EmojiStrings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.DarkSurface
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.platform.rememberImagePicker

/**
 * **Il selettore delle icone** (dal 07/10/2026, chiesto dall'utente con la
 * barra delle categorie di Notion in foto): lo stesso dappertutto si
 * tocca un'icona per cambiarla — pagine, righe e database, "Edit icon" dei
 * collegamenti, riquadri.
 *
 * - **"Emoji"**: tutte le emoji, nelle categorie di Notion (Recenti,
 *   Persone, Animali e natura, Cibo e bevande, Attività, Viaggi e luoghi,
 *   Oggetti, Simboli, Bandiere) e in fondo le **"Custom"**, le immagini
 *   caricate finora come icona. La barra in basso salta a una categoria e
 *   segna quella che si sta guardando; "+" carica un'immagine nuova.
 *   In cima la ricerca (nelle otto lingue dell'app insieme),
 *   "Random" e il colore della pelle;
 * - **"Icons"** (dal 08/10/2026): le icone piene di Material, bianche come
 *   quelle di Notion; il pallino accanto a "Random" apre lo spettro dei
 *   colori di tutta l'app e le colora tutte;
 * - **"Upload"**: un'immagine dal telefono, come prima;
 * - **"Remove"**, se un'icona c'è.
 *
 * `onPick` applica la scelta e restituisce l'immagine che se ne va, che qui
 * si cancella dalla cartella (null se non ce n'è, o se deve restare, come
 * nei riquadri dove la freccia indietro la rimette).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerSheet(
    store: PageImageStore,
    hasIcon: Boolean,
    onPick: (IconChoice) -> String?,
    onDismiss: () -> Unit,
    /** Le emoji suggerite in cima, col loro titolo: la famiglia "Callout" nei riquadri. */
    suggestionsTitle: String? = null,
    suggestions: List<String> = emptyList()
) {
    val context = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(PickerTabKind.EMOJI) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun choose(choice: IconChoice) {
        if (choice is IconChoice.Emoji) AppSettings.rememberEmoji(choice.emoji)
        val replaced = onPick(choice)
        // Il selettore si chiude subito: la cancellazione non deve morire con lui.
        if (replaced != null) scope.launch(NonCancellable) { store.delete(replaced) }
        onDismiss()
    }

    val upload = rememberImagePicker(
        store = store,
        onStart = {
            busy = true
            error = null
        }
    ) { result ->
        busy = false
        result.fold(
            onSuccess = { choose(IconChoice.Image(it)) },
            onFailure = { error = EditorStrings.imageReadFailed }
        )
    }

    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PickerTab(EmojiStrings.emojiTab, selected = tab == PickerTabKind.EMOJI) { tab = PickerTabKind.EMOJI }
                PickerTab(EmojiStrings.iconsTab, selected = tab == PickerTabKind.ICONS) { tab = PickerTabKind.ICONS }
                PickerTab(EmojiStrings.uploadTab, selected = tab == PickerTabKind.UPLOAD) { tab = PickerTabKind.UPLOAD }
                Spacer(modifier = Modifier.weight(1f))
                if (hasIcon) {
                    Text(
                        EditorStrings.remove,
                        style = MaterialTheme.typography.labelLarge,
                        color = NotionGray400,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(enabled = !busy) { choose(IconChoice.None) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }
            HorizontalDivider(color = DarkSurface)

            if (tab == PickerTabKind.ICONS) {
                IconsTab(enabled = !busy, onSymbol = { choose(IconChoice.Symbol(it)) })
            } else if (tab == PickerTabKind.UPLOAD) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Button(onClick = upload, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Text(EmojiStrings.uploadImage)
                    }
                    Text(
                        EmojiStrings.uploadHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = NotionGray400,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    error?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            } else {
                val catalog by produceState<EmojiCatalog?>(null) {
                    value = EmojiCatalogs.load(context)
                }
                val custom by produceState<List<String>?>(null) {
                    value = IconLibrary.customIcons(context, store)
                }
                EmojiTab(
                    catalog = catalog,
                    custom = custom.orEmpty(),
                    store = store,
                    suggestionsTitle = suggestionsTitle,
                    suggestions = suggestions,
                    enabled = !busy,
                    onEmoji = { choose(IconChoice.Emoji(it)) },
                    onCustom = { name ->
                        // Una copia sua: l'immagine resta anche se l'altra pagina la cambia.
                        scope.launch { store.copy(name)?.let { choose(IconChoice.Image(it)) } }
                    },
                    onUpload = upload
                )
                error?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
            }
        }
    }
}

/** Le schede in cima al selettore. */
private enum class PickerTabKind { EMOJI, ICONS, UPLOAD }

/** Una scheda in cima ("Emoji", "Icons", "Upload"), sottolineata quando è quella aperta. */
@Composable
private fun PickerTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) NotionWhite else NotionGray400)
        Spacer(modifier = Modifier.size(4.dp))
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(2.dp)
                .background(if (selected) NotionWhite else Color.Transparent)
        )
    }
}

/** Una sezione della griglia: le emoji di una categoria, o le immagini delle "Custom". */
private class PickerSection(
    val key: String,
    val title: String,
    val emojis: List<String> = emptyList(),
    val images: List<String> = emptyList(),
    /** Cosa dire quando è vuota; null per non mostrarla affatto. */
    val emptyText: String? = null
)

/** Le voci della barra in basso: dove portano e con che disegno. */
private class BarEntry(val sectionKey: String?, val icon: ImageVector, val label: String)

private const val RECENT_KEY = "recent"
private const val CUSTOM_KEY = "custom"

/** La scheda "Emoji": la ricerca, la griglia (che prende lo spazio che resta) e la barra in basso. */
@Composable
private fun ColumnScope.EmojiTab(
    catalog: EmojiCatalog?,
    custom: List<String>,
    store: PageImageStore,
    suggestionsTitle: String?,
    suggestions: List<String>,
    enabled: Boolean,
    onEmoji: (String) -> Unit,
    onCustom: (String) -> Unit,
    onUpload: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var toneMenu by remember { mutableStateOf(false) }
    val tone = AppSettings.skinTone

    fun shown(entry: EmojiEntry) = if (entry.acceptsTone) withSkinTone(entry.emoji, tone) else entry.emoji

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PickerFilter(query, onChange = { query = it }, tag = "emojiFilter", modifier = Modifier.weight(1f))
        IconButton(
            enabled = enabled && catalog != null,
            onClick = { catalog?.all?.randomOrNull()?.let { onEmoji(shown(it)) } }
        ) {
            Icon(Icons.Outlined.Shuffle, contentDescription = EmojiStrings.random, tint = NotionGray400)
        }
        Box {
            IconButton(onClick = { toneMenu = true }) {
                Text(withSkinTone("✋", tone), fontSize = 20.sp, fontFamily = emojiFontFamily)
            }
            DropdownMenu(expanded = toneMenu, onDismissRequest = { toneMenu = false }, modifier = Modifier.background(DarkSheet)) {
                Row(modifier = Modifier.padding(horizontal = 4.dp)) {
                    (0..SKIN_TONE_COUNT).forEach { option ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (option == tone) DarkSurface else Color.Transparent)
                                .clickable {
                                    AppSettings.skinTone = option
                                    toneMenu = false
                                },
                            contentAlignment = Alignment.Center
                        ) { Text(withSkinTone("✋", option), fontSize = 24.sp, fontFamily = emojiFontFamily) }
                    }
                }
            }
        }
    }

    if (catalog == null) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp, color = NotionGray400)
        }
        return
    }

    if (query.isNotBlank()) {
        val typed = query.trim()
        // Un'emoji scritta con la tastiera (niente lettere né numeri) si
        // può usare così com'è, anche se non è nel catalogo.
        val typedEmoji = typed.takeIf { it.length <= 16 && it.none { c -> c.isLetterOrDigit() } }
        val results = remember(query, catalog, tone) { catalog.search(query).map { shown(it) } }
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (results.isEmpty() && typedEmoji == null) {
                Text(
                    EmojiStrings.noResults,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NotionGray400,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(EMOJI_CELL),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (typedEmoji != null && typedEmoji !in results) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(enabled = enabled) { onEmoji(typedEmoji) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(typedEmoji, fontSize = 26.sp, fontFamily = emojiFontFamily)
                                Spacer(modifier = Modifier.size(10.dp))
                                Text(EmojiStrings.useTyped, style = MaterialTheme.typography.bodyMedium, color = NotionWhite)
                            }
                        }
                    }
                    items(results, key = { "r_$it" }) { emoji -> EmojiCell(emoji, enabled) { onEmoji(emoji) } }
                }
            }
        }
        return
    }

    val recent = AppSettings.recentEmojis
    val sections = remember(catalog, custom, recent, tone, suggestions) {
        buildList {
            if (suggestions.isNotEmpty() && suggestionsTitle != null) {
                add(PickerSection("suggested", suggestionsTitle, emojis = suggestions))
            }
            if (recent.isNotEmpty()) add(PickerSection(RECENT_KEY, EmojiStrings.recent, emojis = recent))
            catalog.sections.forEach { (category, entries) ->
                add(PickerSection(category.key, EmojiStrings.category(category), emojis = entries.map { shown(it) }))
            }
            add(PickerSection(CUSTOM_KEY, EmojiStrings.custom, images = custom, emptyText = EmojiStrings.customEmpty))
        }
    }
    // Dove comincia ogni sezione nella griglia, per la barra in basso.
    val starts = remember(sections) {
        var index = 0
        sections.map { section ->
            val start = section.key to index
            index += 1 + section.emojis.size + section.images.size +
                if (section.emojis.isEmpty() && section.images.isEmpty() && section.emptyText != null) 1 else 0
            start
        }
    }
    val gridState = rememberLazyGridState()
    val currentKey by remember(starts) {
        derivedStateOf {
            // In fondo alla griglia l'ultima sezione (le "Custom") non arriva
            // in cima: lì vale l'ultima che si vede.
            val pivot = if (gridState.canScrollForward) {
                gridState.firstVisibleItemIndex
            } else {
                gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: gridState.firstVisibleItemIndex
            }
            starts.lastOrNull { it.second <= pivot }?.first
        }
    }
    var jumpTo by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(jumpTo) {
        val key = jumpTo ?: return@LaunchedEffect
        starts.firstOrNull { it.first == key }?.let { gridState.scrollToItem(it.second) }
        jumpTo = null
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(EMOJI_CELL),
        state = gridState,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.fillMaxWidth().weight(1f)
    ) {
        sections.forEach { section -> section(section, store, enabled, onEmoji, onCustom) }
    }

    val bar = remember {
        listOf(
            BarEntry(RECENT_KEY, Icons.Outlined.Schedule, EmojiStrings.recent),
            BarEntry(EmojiCategory.PEOPLE.key, Icons.Outlined.SentimentSatisfied, EmojiStrings.category(EmojiCategory.PEOPLE)),
            BarEntry(EmojiCategory.NATURE.key, Icons.Outlined.Eco, EmojiStrings.category(EmojiCategory.NATURE)),
            BarEntry(EmojiCategory.FOOD.key, CarrotIcon, EmojiStrings.category(EmojiCategory.FOOD)),
            BarEntry(EmojiCategory.ACTIVITY.key, Icons.Outlined.SportsSoccer, EmojiStrings.category(EmojiCategory.ACTIVITY)),
            BarEntry(EmojiCategory.TRAVEL.key, Icons.Outlined.Flight, EmojiStrings.category(EmojiCategory.TRAVEL)),
            BarEntry(EmojiCategory.OBJECTS.key, Icons.Outlined.Lightbulb, EmojiStrings.category(EmojiCategory.OBJECTS)),
            BarEntry(EmojiCategory.SYMBOLS.key, Icons.Outlined.CheckCircle, EmojiStrings.category(EmojiCategory.SYMBOLS)),
            BarEntry(EmojiCategory.FLAGS.key, Icons.Outlined.Flag, EmojiStrings.category(EmojiCategory.FLAGS)),
            BarEntry(CUSTOM_KEY, Icons.Outlined.GridView, EmojiStrings.custom),
            BarEntry(null, Icons.Filled.AddCircle, EmojiStrings.addCustom)
        )
    }
    HorizontalDivider(color = DarkSurface)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        bar.forEach { entry ->
            // Senza recenti la voce porta in cima, alla prima sezione.
            val target = entry.sectionKey?.let { key -> if (starts.any { it.first == key }) key else starts.firstOrNull()?.first }
            val selected = entry.sectionKey != null && entry.sectionKey == currentKey
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) DarkSurface else Color.Transparent)
                    .clickable(enabled = enabled) {
                        if (entry.sectionKey == null) onUpload() else jumpTo = target
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    entry.icon,
                    contentDescription = entry.label,
                    tint = if (selected) NotionWhite else NotionGray400,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/** Il campo "Filter…" in cima alle schede Emoji e Icons. */
@Composable
private fun PickerFilter(query: String, onChange: (String) -> Unit, tag: String, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(EmojiStrings.filter, style = MaterialTheme.typography.bodyMedium, color = NotionGray400)
            }
            AutoCaps {
                BasicTextField(
                    value = query,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = NotionWhite),
                    cursorBrush = SolidColor(NotionWhite),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth().testTag(tag)
                )
            }
        }
    }
}

/**
 * Le parole del nome di un'icona di Material, per cercarla: "AccessAlarm"
 * è "access alarm", "_3dRotation" è "3d rotation". I nomi sono in inglese,
 * come quelli della libreria.
 */
fun materialIconWords(name: String): List<String> =
    name.trimStart('_')
        .split(Regex("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|(?<=[A-Za-z])(?=[0-9])"))
        .map { it.lowercase() }
        .filter { it.isNotEmpty() }

/** Le icone il cui nome ha tutte le parole scritte, ognuna come inizio di una parola ("alar" trova "Alarm"). */
fun searchMaterialIcons(query: String, names: List<String> = MATERIAL_ICON_NAMES): List<String> {
    val tokens = normalizeForSearch(query).split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return names
    return names.filter { name ->
        val words = materialIconWords(name)
        tokens.all { token -> words.any { it.startsWith(token) } }
    }
}

/**
 * **La scheda "Icons"** (dal 08/10/2026, come quella di Notion nella foto
 * dell'utente): le icone piene di Material, di partenza nel colore del testo
 * (bianche sul tema scuro). Il pallino accanto a "Random" apre lo spettro
 * dei colori di tutta l'app: il colore scelto colora tutte le icone della
 * griglia, va con l'icona scelta e resta per la prossima volta.
 */
@Composable
private fun ColumnScope.IconsTab(enabled: Boolean, onSymbol: (IconSymbol) -> Unit) {
    var query by remember { mutableStateOf("") }
    var coloring by remember { mutableStateOf(false) }
    val color = AppSettings.iconColor
    val tint = hexToColor(color) ?: NotionWhite

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PickerFilter(query, onChange = { query = it }, tag = "iconFilter", modifier = Modifier.weight(1f))
        IconButton(
            enabled = enabled,
            onClick = { onSymbol(IconSymbol(MATERIAL_ICON_NAMES.random(), color)) }
        ) {
            Icon(Icons.Outlined.Shuffle, contentDescription = EmojiStrings.random, tint = NotionGray400)
        }
        IconButton(onClick = { coloring = true }) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(tint)
                    .border(1.dp, NotionGray400, CircleShape)
                    .semantics { contentDescription = EmojiStrings.iconColor }
            )
        }
    }

    val names = remember(query) { searchMaterialIcons(query) }
    if (names.isEmpty()) {
        Text(
            EmojiStrings.noResults,
            style = MaterialTheme.typography.bodyMedium,
            color = NotionGray400,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(24.dp).weight(1f)
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(EMOJI_CELL),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(names, key = { it }, contentType = { "icon" }) { name ->
                val vector = remember(name) { materialIconByName(name) }
                Box(
                    modifier = Modifier
                        .height(EMOJI_CELL)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = enabled && vector != null) { onSymbol(IconSymbol(name, color)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (vector != null) {
                        Icon(
                            vector,
                            contentDescription = materialIconWords(name).joinToString(" "),
                            tint = tint,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }

    if (coloring) {
        ColorPickerSheet(
            initialHex = color,
            background = false,
            onDismiss = { coloring = false },
            onSwitchTarget = {},
            onPick = { hex ->
                coloring = false
                AppSettings.iconColor = hex
            },
            showTargetSwitch = false,
            previewLabel = EmojiStrings.iconColor
        )
    }
}

/** La griglia della sezione: il titolo su tutta la riga, poi le emoji o le immagini. */
private fun LazyGridScope.section(
    section: PickerSection,
    store: PageImageStore,
    enabled: Boolean,
    onEmoji: (String) -> Unit,
    onCustom: (String) -> Unit
) {
    item(key = "h_${section.key}", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
        Text(
            section.title,
            style = MaterialTheme.typography.labelMedium,
            color = NotionGray400,
            modifier = Modifier.padding(start = 6.dp, top = 12.dp, bottom = 6.dp)
        )
    }
    if (section.emojis.isEmpty() && section.images.isEmpty() && section.emptyText != null) {
        item(key = "e_${section.key}", span = { GridItemSpan(maxLineSpan) }) {
            Text(
                section.emptyText,
                style = MaterialTheme.typography.bodySmall,
                color = NotionGray400,
                modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
            )
        }
    }
    items(section.emojis, key = { "${section.key}_$it" }, contentType = { "emoji" }) { emoji ->
        EmojiCell(emoji, enabled) { onEmoji(emoji) }
    }
    items(section.images, key = { "${section.key}_$it" }, contentType = { "image" }) { name ->
        Box(
            modifier = Modifier
                .height(EMOJI_CELL)
                .clip(RoundedCornerShape(6.dp))
                .clickable(enabled = enabled) { onCustom(name) },
            contentAlignment = Alignment.Center
        ) {
            PageImage(
                fileName = name,
                store = store,
                contentDescription = null,
                modifier = Modifier.size(30.dp).clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun EmojiCell(emoji: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(EMOJI_CELL)
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = 26.sp, fontFamily = emojiFontFamily)
    }
}

private val EMOJI_CELL = 46.dp

/**
 * La carota della barra, come quella di Notion per "Cibo e bevande": fra le
 * icone di Material non c'è. Disegnata a tratto, come le altre della barra.
 */
private val CarrotIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Carrot",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Il corpo, dalla punta in basso a sinistra.
            moveTo(4.5f, 19.5f)
            curveTo(6.5f, 14.5f, 10.5f, 9.8f, 13.4f, 8.6f)
            curveTo(15.0f, 7.9f, 16.6f, 9.4f, 15.8f, 11.0f)
            curveTo(14.4f, 13.6f, 9.6f, 17.6f, 4.5f, 19.5f)
            close()
            // I segni sul corpo.
            moveTo(8.6f, 14.6f)
            lineTo(9.8f, 15.6f)
            moveTo(11.2f, 11.6f)
            lineTo(12.3f, 12.6f)
            // Le foglie.
            moveTo(15.0f, 8.2f)
            lineTo(16.4f, 3.8f)
            moveTo(15.4f, 8.5f)
            lineTo(19.2f, 4.6f)
            moveTo(15.8f, 8.9f)
            lineTo(20.3f, 7.6f)
        }
    }.build()
}
