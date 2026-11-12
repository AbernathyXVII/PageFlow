package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.ui.navigation.LocalPeekControls
import androidx.compose.material.icons.filled.OpenInNew
import com.gabriele.notionlocal.data.settings.OpenMode
import com.gabriele.notionlocal.ui.navigation.LocalPageOpener
import com.gabriele.notionlocal.platform.emojiTransformation
import com.gabriele.notionlocal.platform.withEmojiFont
import com.gabriele.notionlocal.platform.emojiFontFamily
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.hoverable
import androidx.compose.runtime.produceState
import com.gabriele.notionlocal.ui.theme.pcOr
import com.gabriele.notionlocal.platform.isPc
import androidx.compose.material.icons.filled.BorderTop
import androidx.compose.material.icons.filled.BorderLeft
import com.gabriele.notionlocal.data.hasChosenIcon
import com.gabriele.notionlocal.data.chosenEmoji
import com.gabriele.notionlocal.data.RowIconRef
import com.gabriele.notionlocal.data.entity.DurationUnit
import com.gabriele.notionlocal.data.entity.durationFormatOf
import androidx.compose.material.icons.filled.FormatAlignRight
import com.gabriele.notionlocal.data.entity.parseCalculationAlignments
import com.gabriele.notionlocal.data.entity.cellAlignment
import com.gabriele.notionlocal.data.entity.CellAlignment
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Functions
import com.gabriele.notionlocal.data.formula.FormulaConfig
import com.gabriele.notionlocal.data.formula.FormulaEngine
import com.gabriele.notionlocal.data.formula.FormulaValue
import com.gabriele.notionlocal.ui.i18n.FormulaStrings
import com.gabriele.notionlocal.data.entity.parseViewCalculations
import com.gabriele.notionlocal.data.entity.numberSettings
import com.gabriele.notionlocal.data.entity.numberDisplayKind
import com.gabriele.notionlocal.data.entity.calculationsFor
import com.gabriele.notionlocal.data.entity.calculate
import com.gabriele.notionlocal.data.entity.NumberSettings
import com.gabriele.notionlocal.data.entity.NumberDisplay
import com.gabriele.notionlocal.data.entity.NAME_CALCULATION_KEY
import com.gabriele.notionlocal.data.entity.Calculation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.filled.Lock
import com.gabriele.notionlocal.ui.i18n.LockStrings
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.NorthEast
import com.gabriele.notionlocal.data.entity.DatabaseSnapshot
import com.gabriele.notionlocal.data.entity.RelationConfig
import com.gabriele.notionlocal.data.entity.RollupConfig
import com.gabriele.notionlocal.data.entity.encodeRelationConfig
import com.gabriele.notionlocal.data.entity.encodeRollupConfig
import com.gabriele.notionlocal.data.entity.relationConfig
import com.gabriele.notionlocal.data.entity.relationTitles
import com.gabriele.notionlocal.data.entity.rollupConfig
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.RectangleShape
import com.gabriele.notionlocal.viewmodel.RowSearch
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.VisualTransformation
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.data.entity.LinkLabel
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.i18n.TemplateStrings
import com.gabriele.notionlocal.data.entity.ChartSettings
import com.gabriele.notionlocal.data.entity.parseChartSettings
import androidx.compose.material.icons.filled.Menu
import androidx.compose.foundation.layout.statusBarsPadding
import com.gabriele.notionlocal.platform.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.gabriele.notionlocal.data.entity.TreeRow
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Segment
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.Image
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.repository.PageRepository
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material.icons.filled.BorderVertical
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.gabriele.notionlocal.data.dao.RowCover
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.GALLERY_DEFAULT_PREVIEW
import com.gabriele.notionlocal.data.entity.GALLERY_DEFAULT_SIZE
import com.gabriele.notionlocal.data.entity.GalleryCardPreview
import com.gabriele.notionlocal.data.entity.GalleryCardSize
import com.gabriele.notionlocal.data.repository.RowPreviewLine
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.TouchApp
import com.gabriele.notionlocal.data.buttons.ButtonConfig
import com.gabriele.notionlocal.ui.i18n.ButtonStrings
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Checkbox
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import com.gabriele.notionlocal.data.entity.defaultColumnWidth
import androidx.compose.ui.layout.ContentScale
import com.gabriele.notionlocal.data.entity.rowTree
import com.gabriele.notionlocal.data.entity.hasSubRows
import com.gabriele.notionlocal.data.entity.rowsForCalculation
import com.gabriele.notionlocal.data.entity.parseCalculationSubRows
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.material3.Typography
import androidx.compose.runtime.Stable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.material3.DividerDefaults
import kotlin.math.roundToInt
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.SettingsEthernet
import com.gabriele.notionlocal.data.entity.DEFAULT_COLUMN_WIDTH_DP
import com.gabriele.notionlocal.data.entity.DEFAULT_NAME_COLUMN_WIDTH_DP
import com.gabriele.notionlocal.data.entity.MAX_COLUMN_WIDTH_DP
import com.gabriele.notionlocal.data.entity.snapColumnWidth
import com.gabriele.notionlocal.data.entity.MIN_COLUMN_WIDTH_DP
import com.gabriele.notionlocal.data.entity.fitMinColumnWidth
import com.gabriele.notionlocal.data.entity.namePosition
import com.gabriele.notionlocal.data.entity.minColumnWidth
import androidx.compose.material.icons.filled.WrapText
import androidx.compose.foundation.layout.FlowRow
import com.gabriele.notionlocal.data.entity.CellOverflow
import com.gabriele.notionlocal.data.entity.NAME_COLUMN_KEY
import com.gabriele.notionlocal.data.entity.parseColumnWidths
import com.gabriele.notionlocal.data.entity.parseFitColumns
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gabriele.notionlocal.data.entity.EventDraft
import com.gabriele.notionlocal.data.notify.NotificationTarget
import androidx.compose.material.icons.filled.Event
import com.gabriele.notionlocal.data.entity.CalendarMode
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.DateRange
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.MULTI_VALUE_SEPARATOR
import com.gabriele.notionlocal.data.entity.encodeDateRange
import com.gabriele.notionlocal.data.entity.ColorCellValue
import com.gabriele.notionlocal.data.entity.darkTextOn
import com.gabriele.notionlocal.data.entity.encodeColorCell
import com.gabriele.notionlocal.data.entity.parseColorCell
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.defaultMinSize
import com.gabriele.notionlocal.data.notify.NotificationPermission
import com.gabriele.notionlocal.data.entity.parseDateRange
import com.gabriele.notionlocal.data.entity.MonthlyMode
import com.gabriele.notionlocal.data.entity.Recurrence
import com.gabriele.notionlocal.data.entity.RecurrenceEnd
import com.gabriele.notionlocal.data.entity.RecurrenceFreq
import com.gabriele.notionlocal.data.entity.SORT_BY_NAME
import com.gabriele.notionlocal.data.entity.SortRuleSpec
import com.gabriele.notionlocal.data.entity.GroupLevel
import com.gabriele.notionlocal.data.entity.GroupOrder
import com.gabriele.notionlocal.data.entity.GroupRuleSpec
import com.gabriele.notionlocal.data.entity.existingIn
import com.gabriele.notionlocal.data.entity.sortRules
import com.gabriele.notionlocal.data.entity.SelectOption
import com.gabriele.notionlocal.data.entity.TAG_COLORS
import com.gabriele.notionlocal.data.entity.TimelineZoom
import androidx.compose.ui.text.style.TextAlign
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.DarkSurface
import com.gabriele.notionlocal.ui.theme.DarkSurfaceVariant
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.viewmodel.DatabaseTableState
import com.gabriele.notionlocal.viewmodel.DatabaseViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale
import com.gabriele.notionlocal.platform.showMessage
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.data.export.exportToFile

private val COLUMN_WIDTH = DEFAULT_COLUMN_WIDTH_DP.dp
private val NAME_COLUMN_WIDTH = DEFAULT_NAME_COLUMN_WIDTH_DP.dp

/**
 * **Quanto è larga ogni colonna della tabella** in questa vista (dal
 * 29/09/2026): quella scelta col segnalino, quella adattata al contenuto
 * ("Fit to content") o quella di sempre. La leggono intestazioni, celle e
 * calcoli, così restano in colonna.
 */
@Immutable
private class TableWidths(val name: Dp, private val byKey: Map<String, Dp>) {
    fun of(column: DatabaseColumnEntity): Dp = byKey[column.id] ?: defaultColumnWidth(column.id, column.type).dp
    fun of(key: String): Dp = if (key == NAME_COLUMN_KEY) name else byKey[key] ?: COLUMN_WIDTH
}

private val LocalTableWidths = compositionLocalOf { TableWidths(NAME_COLUMN_WIDTH, emptyMap()) }

/**
 * **"Hide icon"** delle proprietà (dal 27/10/2026, chiesto dall'utente): se il
 * "Aa" della proprietà principale è nascosto, e come nascondere o rimostrare
 * un'icona (`columnId` null = la proprietà principale). Passa da qui come
 * `LocalNameColumnTitle`. Null dove non si può cambiare.
 */
@Stable
class PropertyIcons(val nameHidden: Boolean, val setHidden: (columnId: String?, hidden: Boolean) -> Unit)

val LocalPropertyIcons = compositionLocalOf<PropertyIcons?> { null }

/**
 * **Il segnalino per allargare una colonna** ("Resize column" tenendo
 * premuta l'intestazione): di quale colonna è, e la misura mentre lo si
 * trascina. Resta finché non si tocca la tabella da un'altra parte.
 */
@Stable
private class ColumnResize {
    var key by mutableStateOf<String?>(null)
    var draft by mutableStateOf<Dp?>(null)
    /** Dove porterebbe il dito, prima di agganciarsi alla larghezza di sempre. */
    var raw = 0.dp
    /** Il dito è appena sceso sul segnalino: quel tocco non lo chiude. */
    var handleTouched = false

    fun start(column: String) {
        key = column
        draft = null
    }

    fun stop() {
        key = null
        draft = null
    }
}

private val LocalColumnResize = compositionLocalOf<ColumnResize?> { null }

/** Quanto rientra ogni livello di sotto-righe, e il posto della freccia che le apre. */
private val SUB_ROW_INDENT = 20.dp
private val SUB_ROW_ARROW_SLOT = 24.dp

/** Il nome di quale riga aprire subito per scriverlo (una sotto-riga appena nata), e come dire "fatto". */
@Immutable
private class TitleEditRequest(val rowId: String?, val consume: () -> Unit)

private val LocalTitleEditRequest = compositionLocalOf { TitleEditRequest(null) {} }

private val RESIZE_HANDLE_WIDTH = 24.dp
private val RESIZE_HANDLE_HEIGHT = 40.dp
// Il "+" per una colonna nuova e, accanto, i tre puntini per mostrarle o nasconderle (dal 11/11/2026).
private val ADD_PROPERTY_WIDTH = 88.dp
private val BOARD_COLUMN_WIDTH = 260.dp

/**
 * Le misure del calendario.
 *
 * Nel mese l'altezza di una settimana è fissa: sei settimane devono
 * entrare in una schermata senza costringere a scorrere per vedere il
 * mese, e questo mette un tetto a quante barre ci stanno prima di
 * riassumere le altre con un "+N".
 *
 * Nella settimana invece l'altezza segue quante pagine ci sono davvero:
 * c'è una riga sola, lo spazio c'è, e nascondere pagine quando c'è posto
 * per mostrarle sarebbe gratuito.
 */
private val CALENDAR_MONTH_WEEK_HEIGHT = 84.dp
private val CALENDAR_WEEK_MIN_HEIGHT = 120.dp
private val CALENDAR_WEEK_HEADER = 20.dp
private val CALENDAR_BAR_HEIGHT = 18.dp

/**
 * Quante righe di pagine può riservare una settimana.
 *
 * Ogni pagina datata del database si tiene la sua riga per sempre, in
 * ogni settimana, anche in quelle in cui non compare: le righe sono
 * quindi tante quante le pagine datate. Questo è il tetto oltre il
 * quale si smette di riservarne, perché un database con centinaia di
 * pagine datate renderebbe altissima ogni settimana. Le pagine oltre il
 * tetto finiscono nel "+N" e si guardano dalla tabella.
 */
private const val CALENDAR_MAX_ROWS = 20

/**
 * Un giorno nella vista dell'anno: dodici mesi affiancati tre per riga
 * lasciano poco più di un centimetro a testa, quindi le misure sono
 * quelle minime in cui due cifre restano leggibili.
 */
private val MINI_DAY_SIZE = 13.dp
private val MINI_DAY_FONT = 8.sp

/**
 * Quante settimane si possono sfogliare col dito, e da quale si parte.
 * Il numero non è un limite vero: sono circa centonovant'anni per
 * parte, e le frecce e il pulsante Today portano ovunque comunque.
 */
private const val WEEK_PAGER_PAGES = 20001
private const val WEEK_PAGER_CENTER = WEEK_PAGER_PAGES / 2

/**
 * Le misure della linea del tempo.
 *
 * `TIMELINE_MAX_DAYS` è un tetto vero e non prudenza sprecata: le
 * colonne dei giorni non sono pigre, vengono composte tutte insieme,
 * quindi senza tetto una data sbagliata nell'anno tremila costruirebbe
 * un milione di caselle.
 */
private val TIMELINE_DAY_WIDTH = 40.dp
private val TIMELINE_ROW_HEIGHT = 34.dp
private val TIMELINE_NAME_WIDTH = 120.dp
private val TIMELINE_HEADER_HEIGHT = 36.dp
private val TIMELINE_HOUR_WIDTH = 52.dp
private val TIMELINE_MIN_BAR_WIDTH = 6.dp

/** Quanto passato lasciare in vista aprendo su adesso. */
private val TIMELINE_TODAY_LEAD_IN = 80.dp
private const val TIMELINE_MAX_TICKS = 400

/**
 * Quanto aspettare, dopo che la tastiera si è chiusa, prima di
 * spegnere il cursore. Lo stesso valore dell'editor, per la stessa
 * ragione: la tastiera Samsung sparisce e riappare per un istante
 * durante i cambi di fuoco.
 */
private const val IME_CLOSE_SETTLE_MS = 250L

/** Spessore delle linee della griglia, lo stesso che usa VerticalDivider. */
private val SEPARATOR_WIDTH = 1.dp
// Sul PC come le righe delle tabelle di Notion (vedi `pcOr`).
private val ROW_HEIGHT: Dp get() = pcOr(32.dp, 44.dp)

/** L'icona della pagina accanto al nome di una riga: quanto una lettera maiuscola, poco di più. */
private val ROW_ICON_SIZE = 18.dp

/** La stessa icona dentro le barre del calendario e della linea temporale, che sono basse. */
private val BAR_ICON_SIZE = 12.dp

// **Le date seguono le impostazioni.** Il formato della data (e con lui
// quello di data e ora) è quello scelto in Settings → Date format; mesi e
// giorni si scrivono nella lingua dell'app. Sono letti a ogni uso e non
// fissati una volta: cambiando impostazione, la tabella si riscrive.
private val DATE_TIME_FORMAT: DateTimeFormatter get() = Formats.dateTimeFormatter()
private val DAY_MONTH_FORMAT: DateTimeFormatter
    get() = DateTimeFormatter.ofPattern("d MMM", AppSettings.language.locale)
private val TIME_FORMAT: DateTimeFormatter get() = Formats.timeFormatter()

/**
 * Cosa sta modificando la finestra delle proprietà: una colonna nuova,
 * oppure una esistente.
 */
private sealed class PropertyTarget {
    /** `initialType` serve a chi la propone già sapendo che tipo serve (il calendario una data, la bacheca una selezione). */
    data class New(val initialType: ColumnType = ColumnType.TEXT) : PropertyTarget()
    /** [mode]: tutto, o solo "Edit property" o "Change type" dal menu dell'intestazione (`PropertyMode`). */
    data class Existing(val column: DatabaseColumnEntity, val mode: PropertyMode = PropertyMode.FULL) : PropertyTarget()
}

/**
 * Cosa mostra la finestra di una proprietà che c'è già (dal 27/10/2026). Da
 * "Change type" nel menu dell'intestazione solo i tipi, con sotto quello
 * scelto le sue impostazioni (Number, Duration, Relation, Rollup); dalle
 * impostazioni del database, dove un'intestazione da toccare non c'è (la
 * bacheca, il calendario…), il nome e i tipi insieme come prima.
 */
private enum class PropertyMode { FULL, TYPE }

/** I tipi con impostazioni loro, che compaiono sotto quando li si sceglie nella finestra della proprietà. */
val TYPES_WITH_SETTINGS = setOf(ColumnType.NUMBER, ColumnType.DURATION, ColumnType.RELATION, ColumnType.ROLLUP, ColumnType.BUTTON, ColumnType.FORMULA)

/** Di quale colonna si sceglie il calcolo: `key` è l'id, o `NAME_CALCULATION_KEY`; `type` null = il nome. */
private data class CalculateTarget(val key: String, val type: ColumnType?, val title: String)

/**
 * Il database a schermo intero: una barra in alto per tornare indietro,
 * e sotto lo stesso contenuto che si vede quando il database è dentro
 * una pagina.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DatabaseViewScreen(
    pageId: String,
    factory: ViewModelFactory,
    onBack: () -> Unit,
    onOpenRowPage: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenUpdates: (String) -> Unit,
    onOpenPage: (String) -> Unit,
    onOpenDatabase: (String) -> Unit,
    onOpenSidebar: () -> Unit = {}
) {
    // **Lo stesso ViewModel che usa il contenuto**, non un secondo:
    // stessa chiave, quindi è proprio la stessa istanza. Il menu dei
    // tre puntini sta nella barra in alto, che vive qui fuori, e le sue
    // voci cambiano la pagina — con due ViewModel diversi uno dei due
    // avrebbe sempre in mano una versione vecchia.
    val menuViewModel: DatabaseViewModel = viewModel(factory = factory, key = "database-$pageId")
    LaunchedEffect(pageId) { menuViewModel.load(pageId) }
    val menuPage by menuViewModel.page.collectAsStateWithLifecycle()

    var showPageOptions by remember { mutableStateOf(false) }
    var showDatabaseExport by remember { mutableStateOf(false) }
    var showAutomations by remember { mutableStateOf(false) }
    var moveExclusions by remember { mutableStateOf<PageRepository.MoveExclusions?>(null) }
    // La scelta aperta è quella di "Move to database" invece di "Move to".
    var movingIntoDatabase by remember { mutableStateOf(false) }
    var confirmTrash by remember { mutableStateOf(false) }
    var showDuplicate by remember { mutableStateOf(false) }
    var passwordLockRequest by remember { mutableStateOf<Boolean?>(null) }

    // Se questo database è richiamato come pagina da qualche parte:
    // solo allora "Turn into database" ha un posto in cui rimetterlo.
    // Si ricontrolla ogni volta che si apre il menu, perché nel frattempo
    // può essere cambiato dall'altra schermata.
    var shownAsPage by remember { mutableStateOf(false) }
    LaunchedEffect(pageId, showPageOptions) {
        shownAsPage = menuViewModel.isShownAsPage()
    }

    // Chiusa la tastiera, il cursore si spegne. Stessa cosa della
    // pagina, e per la stessa ragione: il tasto indietro con la
    // tastiera aperta se lo prende la tastiera e all'app non arriva,
    // quindi il segnale è la tastiera che si chiude. Vedi il commento
    // in `PageEditorScreen`.
    val focusManager = LocalFocusManager.current
    var anythingFocused by remember { mutableStateOf(false) }
    val keyboardWatch = remember { KeyboardCloseWatch() }
    ClearFocusWhenKeyboardCloses(
        watch = keyboardWatch,
        anythingFocused = anythingFocused,
        focusInFlight = false,
        settleDelayMs = IME_CLOSE_SETTLE_MS
    )
    BackHandler(enabled = anythingFocused) { focusManager.clearFocus() }

    // Aperto dal cestino: si guarda e basta, e al posto della barra in
    // alto c'è quella del cestino.
    val trashed = menuPage?.trashedAt != null
    var confirmDeleteForever by remember { mutableStateOf(false) }
    val trashContext = LocalPlatformContext.current
    // Sul PC le pagine che contengono il database, per il percorso in alto (`PageBreadcrumb`).
    val ancestors by produceState(emptyList<PageEntity>(), pageId) {
        if (isPc) value = factory.pageRepository.ancestorsOf(pageId)
    }

    Scaffold(
        topBar = {
            if (trashed) {
                Box(modifier = Modifier.statusBarsPadding()) {
                    TrashBanner(
                        onBack = onBack,
                        onRestore = {
                            menuViewModel.restoreFromTrash()
                            showMessage(trashContext, Strings.restoredToMainMenu)
                        },
                        onDeleteForever = { confirmDeleteForever = true }
                    )
                }
            } else if (isPc) {
                // **Sul PC la barra in alto di Notion**, come nelle pagine (dal
                // 11/11/2026, trovato dall'utente: aperto a tutta pagina il
                // database aveva ancora i pulsanti di prima). Vedi `PcTopBar`.
                val peekControls = LocalPeekControls.current
                PcTopBar(
                    start = {
                        // Dentro un peek (dal 12/11/2026) i suoi comandi al posto di indietro, ☰ e percorso.
                        if (peekControls != null) {
                            peekControls()
                            return@PcTopBar
                        }
                        PcBarButton(Icons.Filled.ArrowBack, Strings.back, onBack)
                        if (!LocalSidebarPinned.current) {
                            PcBarButton(Icons.Filled.Menu, Strings.openSidebar) {
                                focusManager.clearFocus()
                                onOpenSidebar()
                            }
                        }
                        PageBreadcrumb(
                            ancestors = ancestors,
                            current = menuPage,
                            onOpen = { crumb -> if (crumb.isDatabase) onOpenDatabase(crumb.id) else onOpenPage(crumb.id) }
                        )
                    },
                    end = {
                        if (menuPage?.isProtected == true) {
                            PcBarButton(Icons.Filled.Lock, LockStrings.protectedByPassword) {
                                showMessage(trashContext, LockStrings.protectedByPassword)
                            }
                        }
                        PcBarButton(Icons.Filled.MoreHoriz, Strings.pageOptions) { showPageOptions = true }
                    }
                )
            } else {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        Row {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = Strings.back)
                            }
                            // La barra laterale, accanto a indietro come
                            // nelle pagine.
                            if (!LocalSidebarPinned.current) {
                                IconButton(onClick = {
                                    focusManager.clearFocus()
                                    onOpenSidebar()
                                }) {
                                    Icon(Icons.Filled.Menu, contentDescription = Strings.openSidebar)
                                }
                            }
                        }
                    },
                    actions = {
                        // Il lucchetto chiuso dei database bloccati con la
                        // password, a sinistra dei tre puntini come nelle
                        // pagine.
                        if (menuPage?.isProtected == true) {
                            IconButton(onClick = {
                                showMessage(trashContext, LockStrings.protectedByPassword)
                            }) {
                                Icon(Icons.Filled.Lock, contentDescription = LockStrings.protectedByPassword)
                            }
                        }
                        // Stessa icona e stesso posto della lente di prima,
                        // come su ogni pagina: un database aperto a schermo
                        // intero **è** una pagina.
                        IconButton(onClick = { showPageOptions = true }) {
                            Icon(Icons.Filled.MoreHoriz, contentDescription = Strings.pageOptions)
                        }
                    }
                )
            }
        }
    ) { padding ->
        // **I bottoni** (dal 27/10/2026): dentro una pagina li fa partire
        // l'editor della pagina; qui, a tutto schermo, questa schermata.
        val buttonRunner = rememberButtonRunController(
            pages = factory.pageRepository,
            onOpenPage = { id, isDatabase -> if (isDatabase) onOpenDatabase(id) else onOpenPage(id) }
        )
        val buttonEnv = remember(factory) { ButtonEditorEnv(factory.pageRepository, factory, factory.imageStore) }
        CompositionLocalProvider(
            LocalKeyboardCloseWatch provides keyboardWatch,
            LocalButtonRunController provides buttonRunner,
            LocalButtonEditorEnv provides buttonEnv
        ) {
        ButtonRunDialogs(buttonRunner)
        DatabaseContent(
            pageId = pageId,
            factory = factory,
            embedded = false,
            onOpenRowPage = onOpenRowPage,
            onOpenFullPage = null,
            onOpenSource = onOpenDatabase,
            modifier = Modifier
                .padding(padding)
                // **Sul PC i margini di Notion**, come nelle pagine (dal 11/11/2026,
                // chiesto dall'utente): prima il database stava a 16 dal bordo.
                // shortcut: la rotella sui margini non scorre le righe; se dà fastidio,
                // i margini vanno dentro ogni riquadro che scorre.
                .padding(horizontal = PAGE_INSET)
                // **Le righe si fanno da parte per la tastiera** (dal
                // 14/10/2026): prima la tastiera ci passava sopra, e le
                // celle in fondo si scrivevano alla cieca. Così il
                // riquadro che scorre si accorcia insieme a lei, un
                // fotogramma alla volta, e la cella su cui si scrive la
                // segue (vedi `keepFocusedAboveKeyboard`). Lo spazio della
                // barra di navigazione, già dato dallo Scaffold, non si
                // somma a quello della tastiera.
                .consumeWindowInsets(padding)
                .imePadding()
                .onFocusChanged { anythingFocused = it.hasFocus }
        )
        }
    }

    menuPage?.let { current ->
        if (showPageOptions) {
            PageOptionsSheet(
                page = current,
                onDismiss = { showPageOptions = false },
                onToggleFavorite = { menuViewModel.setFavorite(!current.isFavorite) },
                onSearch = {
                    showPageOptions = false
                    onOpenSearch()
                },
                onDuplicate = {
                    showPageOptions = false
                    showDuplicate = true
                },
                onMoveTo = {
                    showPageOptions = false
                    movingIntoDatabase = false
                    menuViewModel.loadMoveExclusions { moveExclusions = it }
                },
                onMoveToDatabase = {
                    showPageOptions = false
                    movingIntoDatabase = true
                    menuViewModel.loadMoveExclusions { moveExclusions = it }
                },
                onMoveToTrash = {
                    showPageOptions = false
                    confirmTrash = true
                },
                onToggleViewLock = { menuViewModel.toggleViewLocked() },
                onToggleLock = { menuViewModel.toggleLocked() },
                onTogglePasswordLock = {
                    showPageOptions = false
                    passwordLockRequest = !current.isProtected
                },
                onUpdates = {
                    showPageOptions = false
                    onOpenUpdates(current.id)
                },
                // **"Export"** anche per un database a schermo intero (dal
                // 03/11/2026): la sua tabella, e le pagine delle righe.
                onExport = if (current.trashedAt != null) null else {
                    {
                        showPageOptions = false
                        showDatabaseExport = true
                    }
                },
                // **"Automations"** (dal 03/11/2026): vedi `AutomationsUi.kt`.
                onAutomations = if (current.trashedAt != null || current.isSimpleDatabase) null else {
                    {
                        showPageOptions = false
                        showAutomations = true
                    }
                },
                // Rimesso dentro la pagina, si torna lì: è lì che lo si
                // vuole rivedere, aperto come prima.
                onTurnIntoDatabase = if (shownAsPage) {
                    {
                        showPageOptions = false
                        menuViewModel.turnIntoDatabase { onBack() }
                    }
                } else {
                    null
                }
            )
        }
        if (showAutomations) {
            // Le automazioni stanno nel database vero, non in una sua vista collegata.
            AutomationsSheet(
                databaseId = current.sourceDatabaseId ?: current.id,
                databaseTitle = current.title,
                factory = factory,
                onDismiss = { showAutomations = false }
            )
        }
        if (showDatabaseExport) {
            val exportContext = LocalPlatformContext.current
            PageExportSheet(
                title = current.title,
                hasSubpages = false,
                export = { options -> exportToFile(exportContext, factory, current.id, options, Strings.untitled) },
                onDismiss = { showDatabaseExport = false }
            )
        }
        passwordLockRequest?.let { wanted ->
            PasswordLockFlow(
                wanted = wanted,
                onFinished = { passwordLockRequest = null },
                apply = { menuViewModel.setPasswordLocked(it) }
            )
        }

        moveExclusions?.takeIf { movingIntoDatabase }?.let { exclusions ->
            MoveToDatabasePicker(
                page = current,
                exclusions = exclusions,
                factory = factory,
                onDismiss = { moveExclusions = null },
                onPick = { destination, rowTitle ->
                    moveExclusions = null
                    // Finisce in una pagina nuova del database scelto, aperto
                    // dentro di lei: l'avviso dice in quale database.
                    menuViewModel.movePageToDatabase(destination, rowTitle) {
                        showMessage(trashContext, Strings.movedInto(destination.placeName()))
                        onBack()
                    }
                }
            )
        }
        moveExclusions?.takeIf { !movingIntoDatabase }?.let { exclusions ->
            MovePagePicker(
                page = current,
                exclusions = exclusions,
                factory = factory,
                onDismiss = { moveExclusions = null },
                onPick = { destination, inline ->
                    moveExclusions = null
                    // Come collegamento a pagina, o aperto dentro la pagina
                    // con "Move as inline" (vedi `PageRepository.movePageTo`):
                    // l'avviso dice dove.
                    menuViewModel.movePageTo(destination, inline) {
                        showMessage(trashContext, Strings.movedInto(destination.placeName()))
                        onBack()
                    }
                }
            )
        }

        if (showDuplicate) {
            DuplicateFlow(
                page = current,
                factory = factory,
                onDismiss = { showDuplicate = false },
                onChosen = { target, placeName ->
                    showDuplicate = false
                    // Come nelle pagine: si apre la copia, e un avviso dice
                    // dov'è finita se non è accanto all'originale.
                    menuViewModel.duplicatePage(target, Strings.copySuffix, PageImageStore(trashContext)) { copyId ->
                        placeName?.let {
                            showMessage(trashContext, Strings.duplicatedInto(it))
                        }
                        onOpenDatabase(copyId)
                    }
                }
            )
        }

        if (confirmTrash) {
            MoveToTrashDialog(
                pageTitle = current.title,
                onConfirm = {
                    confirmTrash = false
                    menuViewModel.moveToTrash { onBack() }
                },
                onDismiss = { confirmTrash = false }
            )
        }
    }

    if (confirmDeleteForever) {
        DeleteForeverDialog(
            onDismiss = { confirmDeleteForever = false },
            onConfirm = {
                confirmDeleteForever = false
                menuViewModel.deletePermanently(PageImageStore(trashContext)) { onBack() }
            }
        )
    }
}

/**
 * Il database vero e proprio: titolo, barra della vista, contenuto.
 *
 * Lo stesso pezzo serve in due posti, ed è deliberato che sia uno solo:
 * a schermo intero e dentro una pagina, dove il database è un blocco
 * fra gli altri e si modifica lì senza aprirlo. Averne due copie
 * significherebbe correggere ogni cosa due volte.
 *
 * Le differenze fra i due casi sono due sole:
 *  - incorporato, il titolo è più piccolo e ha accanto il menu "..."
 *    per aprirlo a schermo intero;
 *  - incorporato, **non scorre per conto suo**: prende l'altezza che
 *    gli serve e lascia scorrere la pagina che lo contiene. Due aree
 *    che scorrono una dentro l'altra si rubano il gesto a vicenda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseContent(
    pageId: String,
    factory: ViewModelFactory,
    embedded: Boolean,
    onOpenRowPage: (String) -> Unit,
    onOpenFullPage: (() -> Unit)?,
    onDelete: (() -> Unit)? = null,
    /** Solo dentro una pagina: lo passa il blocco che mostra il database. */
    onTurnIntoPage: (() -> Unit)? = null,
    /**
     * Solo dentro una pagina, e non bloccata: apre il menu del blocco
     * (sei puntini), con in cima le voci del database. Null altrove, e
     * allora i sei puntini non ci sono.
     */
    onOpenBlockMenu: (() -> Unit)? = null,
    /**
     * Solo per una vista collegata: il suo titolo "↗ nome" porta al
     * database di origine, a schermo intero. Riceve l'id dell'origine.
     */
    onOpenSource: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // **Le viste** (dal 29/09/2026, come su Notion): il database è la sua
    // vista di partenza, e può averne altre (`PageEntity.viewHostId`). Qui
    // si sceglie quale mostrare; nome, icona, preferito e blocchi restano
    // quelli del database, di qualunque vista si tratti.
    val hostViewModel: DatabaseViewModel = viewModel(factory = factory, key = "database-$pageId")
    LaunchedEffect(pageId) { hostViewModel.load(pageId) }
    val hostPage by hostViewModel.page.collectAsStateWithLifecycle()
    val extraViews by hostViewModel.views.collectAsStateWithLifecycle()
    // Una vista che non c'è più (tolta, o di un database da cui questo è
    // stato copiato) non vale: si torna a quella del database.
    val activeId = hostPage?.activeViewId?.takeIf { id -> extraViews.any { it.id == id } } ?: pageId
    // Come il resto dell'impaginazione: col database bloccato o con "Lock
    // view" le viste si guardano e si scelgono, ma non si aggiungono, non
    // si rinominano e non si tolgono.
    val canEditViews = hostPage?.let { !it.isLocked && !it.isViewLocked && it.trashedAt == null } == true
    var showViews by remember { mutableStateOf(false) }
    var showNewView by remember { mutableStateOf(false) }
    var renamingView by remember { mutableStateOf<DatabaseView?>(null) }
    var deletingView by remember { mutableStateOf<DatabaseView?>(null) }
    // Una vista nuova col layout Chart: appena si vede, la finestrella
    // dei quattro grafici, come scegliendo "Chart" dalle impostazioni.
    var chartTypeForView by remember { mutableStateOf<String?>(null) }
    // "Edit view" dai tre puntini (dal 08/10/2026): appena la vista si vede,
    // le sue impostazioni, come dall'icona dei cursori.
    var settingsForView by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    // "Page link": le pagine collegate e la scelta, per tutte le viste (le
    // celle sono le stesse) e per le finestre che le mostrano.
    val linkedPages by hostViewModel.linkedPages.collectAsStateWithLifecycle()
    // "Relation" (dal 21/10/2026): i database collegati, per tutte le viste.
    RelationHost(viewModel = hostViewModel, onOpenPage = onOpenRowPage) {
    PageLinkHost(factory = factory, linkedPages = linkedPages, onOpenPage = onOpenRowPage) {
    // Una vista, un ViewModel e un pezzo di schermo nuovi: niente di
    // quello che era aperto su una resta appeso all'altra.
    key(activeId) {
        DatabaseViewBody(
            pageId = activeId,
            hostViewModel = hostViewModel,
            hostPage = hostPage,
            factory = factory,
            embedded = embedded,
            onOpenRowPage = onOpenRowPage,
            onOpenFullPage = onOpenFullPage,
            onDelete = onDelete,
            onTurnIntoPage = onTurnIntoPage,
            onOpenBlockMenu = onOpenBlockMenu,
            onOpenSource = onOpenSource,
            onOpenViews = {
                focusManager.clearFocus()
                showViews = true
            },
            onAddView = if (canEditViews) {
                {
                    focusManager.clearFocus()
                    showNewView = true
                }
            } else {
                null
            },
            startWithChartType = chartTypeForView == activeId,
            onChartTypeStarted = { chartTypeForView = null },
            startWithSettings = settingsForView == activeId,
            onSettingsStarted = { settingsForView = null },
            modifier = modifier
        )
    }
    }
    }

    val host = hostPage
    if (showViews && host != null) {
        ViewsSheet(
            views = databaseViews(host, extraViews),
            activeId = activeId,
            canEdit = canEditViews,
            onPick = { id ->
                showViews = false
                if (id != activeId) hostViewModel.setActiveView(id)
            },
            onEdit = { view ->
                showViews = false
                settingsForView = view.id
                if (view.id != activeId) hostViewModel.setActiveView(view.id)
            },
            onRename = { renamingView = it },
            onDelete = { deletingView = it },
            onAdd = {
                showViews = false
                showNewView = true
            },
            onDismiss = { showViews = false }
        )
    }
    if (showNewView) {
        NewViewSheet(
            ownSource = host?.let { it.sourceDatabaseId ?: it.id }.orEmpty(),
            loadDatabases = hostViewModel::loadDatabases,
            onCreate = { name, layout, sourceId ->
                showNewView = false
                hostViewModel.createView(name, layout, sourceId) { id ->
                    if (layout == DatabaseLayout.CHART) chartTypeForView = id
                }
            },
            onDismiss = { showNewView = false }
        )
    }
    renamingView?.let { view ->
        RenameViewDialog(
            current = view.name,
            onConfirm = { name ->
                renamingView = null
                if (view.named || name.trim() != view.name) hostViewModel.renameView(view.id, name)
            },
            onDismiss = { renamingView = null }
        )
    }
    deletingView?.let { view ->
        DeleteViewDialog(
            name = view.name,
            onConfirm = {
                deletingView = null
                // Quella di partenza è il database: prende le impostazioni della vista dopo.
                if (view.removable) hostViewModel.deleteView(view.id) else hostViewModel.deleteStartingView()
            },
            onDismiss = { deletingView = null }
        )
    }
}

/**
 * Una vista del database: titolo, barra della vista, contenuto. `pageId` è
 * la vista (il database stesso, o una delle sue viste in più); il titolo,
 * il preferito e i blocchi li prende da `hostPage`, il database.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatabaseViewBody(
    pageId: String,
    hostViewModel: DatabaseViewModel,
    hostPage: PageEntity?,
    factory: ViewModelFactory,
    embedded: Boolean,
    onOpenRowPage: (String) -> Unit,
    onOpenFullPage: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onTurnIntoPage: (() -> Unit)?,
    onOpenBlockMenu: (() -> Unit)?,
    onOpenSource: ((String) -> Unit)?,
    /** Il nome della vista: l'elenco delle viste. */
    onOpenViews: () -> Unit,
    /** Il "+" accanto: una vista nuova. Null = spento (database o vista bloccati). */
    onAddView: (() -> Unit)?,
    startWithChartType: Boolean,
    onChartTypeStarted: () -> Unit,
    startWithSettings: Boolean = false,
    onSettingsStarted: () -> Unit = {},
    modifier: Modifier
) {
    // Un ViewModel per vista: la chiave li tiene distinti quando in una
    // stessa pagina c'è più di un database. Per la vista di partenza è
    // quello del database.
    val viewModel: DatabaseViewModel = viewModel(factory = factory, key = "database-$pageId")

    LaunchedEffect(pageId) { viewModel.load(pageId) }
    // Le righe ferme dopo una spunta vanno al loro posto quando la tabella
    // esce dalla composizione, cioè lasciando la pagina: vedi
    // `DatabaseViewModel.checkboxHold`.
    DisposableEffect(viewModel) { onDispose { viewModel.releaseCheckboxHold() } }

    val state by viewModel.tableState.collectAsStateWithLifecycle()
    // **Vista collegata**: `page` è la vista (impostazioni), `dataPage` il
    // database di origine (nome, icona, blocco, database semplice). Per un
    // database vero sono la stessa pagina.
    val dataPage by viewModel.dataPage.collectAsStateWithLifecycle()
    val sourceMissing by viewModel.sourceMissing.collectAsStateWithLifecycle()
    val page by viewModel.page.collectAsStateWithLifecycle()
    val rowIcons by viewModel.rowIcons.collectAsStateWithLifecycle()
    // Copertine e testo delle pagine delle righe: arrivano solo quando la
    // vista è la galleria (vedi `DatabaseViewModel.load`).
    val rowCovers by viewModel.rowCovers.collectAsStateWithLifecycle()
    val rowPreviews by viewModel.rowPreviews.collectAsStateWithLifecycle()
    val lockedRows by viewModel.lockedRows.collectAsStateWithLifecycle()
    // Le prime righe e "Load more" (vedi `RowLimits`): quante se ne vedono
    // per ogni elenco, e le righe create qui, che si vedono sempre.
    val createdHere by viewModel.createdHere.collectAsStateWithLifecycle()
    val rowLimits = remember(pageId) { RowLimits() }
    rowLimits.created = createdHere
    // La riga di cui si sta scegliendo l'icona, dalla finestra delle azioni.
    var iconForRow by remember { mutableStateOf<DatabaseRowEntity?>(null) }

    // "Lock view": si continua a lavorare sulle righe, ma
    // **l'impaginazione non si tocca** — vista, ordine, filtro,
    // raggruppamento, colonne. È proprio il senso della voce:
    // proteggere come è messo insieme il database, non i suoi dati.
    // Serve sia alla barra della vista sia alla finestra delle
    // impostazioni, quindi sta qui in cima e non dentro una delle due.
    val viewLocked = page?.isViewLocked == true || hostPage?.isViewLocked == true

    // L'icona del database: dove stanno i file delle immagini, e se la
    // finestra per sceglierla è aperta. È la stessa finestra delle
    // pagine (Upload / Link / Remove), non una copia.
    val iconContext = LocalPlatformContext.current
    val imageStore = remember(iconContext) { PageImageStore(iconContext) }
    var editingIcon by remember { mutableStateOf(false) }

    var propertyTarget by remember { mutableStateOf<PropertyTarget?>(null) }
    // La finestrella dei quattro grafici della vista Chart (dal 28/09/2026).
    var choosingChartType by remember { mutableStateOf(false) }
    LaunchedEffect(startWithChartType) {
        if (startWithChartType) {
            choosingChartType = true
            onChartTypeStarted()
        }
    }
    // "Calculate" (dal 26/09/2026): di quale colonna si sceglie il calcolo.
    var calculateTarget by remember { mutableStateOf<CalculateTarget?>(null) }
    val calculations = parseViewCalculations(page?.viewCalculations)
    var rowActionsFor by remember { mutableStateOf<DatabaseRowEntity?>(null) }
    // **L'evento che si sta scrivendo** (dal 30/09/2026, il time-blocking):
    // dalla finestra rapida (un quadratino del giorno) o da "More options" /
    // "Edit event" (`eventFullEditor`).
    var eventDraft by remember { mutableStateOf<EventDraft?>(null) }
    var eventFullEditor by remember { mutableStateOf(false) }
    // Dove la si è toccata, se è una riga della tabella: "Move up" e "Move
    // down" ci sono solo lì, dove l'ordine delle righe si vede.
    var rowPlace by remember { mutableStateOf<RowPlace?>(null) }
    LaunchedEffect(rowActionsFor == null) { if (rowActionsFor == null) rowPlace = null }
    // **Toccata la notifica di una riga senza pagina** (dal 30/09/2026): una
    // riga di un database semplice. La notifica ha aperto questo database;
    // trovata la riga, si apre la sua scheda (vedi `NotificationTarget.rowToShow`).
    val rowToShow by NotificationTarget.rowToShow
    LaunchedEffect(rowToShow, state.rows) {
        val id = rowToShow ?: return@LaunchedEffect
        val row = state.rows.find { it.id == id } ?: return@LaunchedEffect
        NotificationTarget.rowToShow.value = null
        rowActionsFor = row
    }
    // La scheda della galleria di cui si sta spostando la copertina
    // ("Reposition cover" nelle azioni della riga): quella scheda smette
    // di rispondere ai tocchi e segue il dito, finché Save o Cancel.
    var repositioningRowId by remember { mutableStateOf<String?>(null) }
    var rowPendingDeletion by remember { mutableStateOf<DatabaseRowEntity?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }
    /** La proprietà da cui parte il filtro aperto dal menu di un'intestazione; null dalle icone. */
    var filterStart by remember { mutableStateOf<String?>(null) }
    // Su quale schermata aprire le impostazioni: il pulsante della vista
    // porta dritto al selettore, l'icona dei cursori all'elenco completo.
    var settingsInitialPage by remember { mutableStateOf(SettingsPage.ROOT) }
    LaunchedEffect(startWithSettings) {
        if (startWithSettings) {
            settingsInitialPage = SettingsPage.ROOT
            showSettings = true
            onSettingsStarted()
        }
    }
    // Cresce ad ogni pagina creata dal pulsante in fondo. La bacheca e
    // il calendario lo osservano per portare in vista quella nuova.
    var newRowTick by remember { mutableStateOf(0) }

    // Aprire una finestra mentre un campo di testo ha il fuoco lascia
    // sullo schermo i pallini della selezione, che restano sopra alla
    // finestra: togliamo il fuoco prima di aprirla.
    val focusManager = LocalFocusManager.current

    // **Database semplice**: le righe sono solo testo. Toccarne una, in
    // qualunque vista, apre la sua **scheda** — nome e proprietà — invece
    // di una pagina, che per queste righe non esiste e non nasce mai.
    // Negli altri database il tocco apre la pagina della riga, come
    // sempre. Una funzione sola per le sei viste, così nessuna può
    // restare indietro.
    val simple = dataPage?.isSimpleDatabase == true
    // Bloccato il database (o solo questa vista, se è collegata): righe e
    // celle si leggono ma non si scrivono.
    // Una vista in più segue il database su cui è nata: bloccato o
    // buttato lui, bloccata anche lei.
    val contentLocked = page?.isLocked == true || dataPage?.isLocked == true || page?.trashedAt != null ||
        hostPage?.isLocked == true || hostPage?.trashedAt != null
    // Una vista in più ha i dati del database su cui è nata, ma non è una
    // vista collegata: il titolo è quello del database, da scrivere qui.
    val linkedSourceId = hostPage?.sourceDatabaseId
    // Sul PC la pagina di una riga si apre nel modo di Settings (vedi `PageOpener`).
    val opener = LocalPageOpener.current
    val openRow: (DatabaseRowEntity) -> Unit = if (simple) {
        { row ->
            focusManager.clearFocus()
            rowActionsFor = row
        }
    } else {
        { row ->
            // **L'ordine in cui si vedono le righe**, per le frecce "pagina
            // prima" e "pagina dopo" della pagina che si apre (dal 16/10/2026):
            // vedi `RowNavigation`.
            val shown = page
            RowNavigation.remember(
                row.pageId,
                displayedRowIds(
                    state,
                    ViewArrangement(
                        layout = shown?.databaseLayout ?: DatabaseLayout.TABLE,
                        tableLevels = viewModel.tableGroupLevels(),
                        boardLevels = viewModel.boardGroupLevels(),
                        dateColumn = viewModel.calendarDateColumn()
                    )
                )
            )
            viewModel.openRow(row) { id -> if (opener != null) opener.open(id, false, null) else onOpenRowPage(id) }
        }
    }

    // La ricerca (la lente accanto a Sort): vive nel ViewModel, così
    // aprendo una pagina trovata e tornando indietro è ancora lì. Tornando,
    // si rilegge il testo delle pagine, che nel frattempo può essere
    // cambiato.
    val editTitleOf by viewModel.editTitleOf.collectAsStateWithLifecycle()
    val searchOpen by viewModel.searchOpen.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var focusSearchField by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.refreshSearchIndex() }

    // Una vista collegata il cui database è stato cancellato o buttato:
    // niente da mostrare, e una riga che lo dice.
    if (sourceMissing) {
        LinkedSourceMissing(
            onOpenBlockMenu = onOpenBlockMenu?.let { open ->
                {
                    focusManager.clearFocus()
                    open()
                }
            },
            modifier = modifier
        )
        return
    }

    // Le Relation e i "Page link" di **questa** vista (dal 21/10/2026): una
    // vista può mostrare le righe di un altro database ("Data source"), e i
    // database collegati sono quelli delle sue righe, non dell'ospite.
    val viewRelated by viewModel.related.collectAsStateWithLifecycle()
    val viewLinkedPages by viewModel.linkedPages.collectAsStateWithLifecycle()
    val openRowPage by rememberUpdatedState(onOpenRowPage)
    val relationActions = remember(viewModel) {
        RelationActions { rowId, onMissing ->
            viewModel.openRelatedRow(rowId) { id -> if (id != null) openRowPage(id) else onMissing() }
        }
    }

    val cellLinkLabels = remember(state.linkLabels, viewModel) { CellLinkLabels(state.linkLabels, viewModel::setCellLinkLabel) }

    var editingButton by remember { mutableStateOf<DatabaseColumnEntity?>(null) }
    // **La formula di una colonna** (dal 28/10/2026): da una cella (con
    // l'anteprima su quella riga) o da "Edit formula" nell'intestazione.
    var editingFormula by remember { mutableStateOf<Pair<DatabaseColumnEntity, String?>?>(null) }
    val formulaEditor: ((DatabaseColumnEntity, String?) -> Unit)? = remember(contentLocked) {
        if (contentLocked) {
            null
        } else {
            { column, rowId ->
                focusManager.clearFocus()
                editingFormula = column to rowId
            }
        }
    }
    val propertyIcons = remember(dataPage?.hideNameIcon, viewModel) {
        PropertyIcons(dataPage?.hideNameIcon == true, viewModel::setPropertyIconHidden)
    }
    // "Change type", "Filter" e "Sort" nel menu di un'intestazione (dal 27/10/2026):
    // il filtro è la stessa finestra dell'icona in cima; "Sort" dal 02/11/2026
    // ordina solo per quella proprietà, e le regole restano nell'icona.
    val propertyMenuActions = remember(viewLocked, contentLocked) {
        PropertyMenuActions(
            rename = if (contentLocked) null else { column, name -> viewModel.renameColumn(column.id, name) },
            delete = if (contentLocked) null else { column -> viewModel.deleteColumn(column) },
            changeType = { column ->
                focusManager.clearFocus()
                propertyTarget = PropertyTarget.Existing(column, PropertyMode.TYPE)
            },
            filter = if (viewLocked) {
                null
            } else {
                { columnId ->
                    focusManager.clearFocus()
                    filterStart = columnId
                    showFilter = true
                }
            },
            sortBy = if (viewLocked) {
                null
            } else {
                { columnId, descending ->
                    focusManager.clearFocus()
                    viewModel.sortFirstBy(columnId, descending)
                }
            },
            removeSort = if (viewLocked) null else { columnId -> viewModel.removeSortRule(columnId) },
            sortRules = { viewModel.page.value?.sortRules().orEmpty() },
            sortKind = viewModel::sortKindOf,
            editButton = if (contentLocked) null else { column ->
                focusManager.clearFocus()
                editingButton = column
            }
        )
    }

    // **Le azioni di una proprietà Button** (dal 27/10/2026): da "Edit
    // actions" nel menu dell'intestazione, o toccando un bottone che non ne
    // ha ancora. Valgono per tutte le righe.
    editingButton?.let { column ->
        ButtonEditorSheet(
            initial = ButtonConfig.parse(column.optionsJson),
            inDatabase = true,
            thisPageDatabaseId = column.pageId,
            onDismiss = { editingButton = null },
            onSave = { viewModel.setButtonConfig(column.id, it) }
        )
    }
    editingFormula?.let { (column, rowId) ->
        FormulaEditorFor(viewModel, column, rowId, onDismiss = { editingFormula = null })
    }

    CompositionLocalProvider(
        LocalFormulaValues provides state.formulas,
        LocalFormulaEditor provides formulaEditor,
        LocalRelated provides viewRelated,
        LocalRelationActions provides relationActions,
        LocalLinkedPages provides viewLinkedPages,
        LocalDatabaseLocked provides contentLocked,
        LocalNameColumnTitle provides dataPage?.nameColumnTitle,
        LocalPropertyIcons provides propertyIcons,
        LocalPropertyMenuActions provides propertyMenuActions,
        LocalButtonColumnEditor provides propertyMenuActions.editButton,
        LocalRowIcons provides rowIcons,
        LocalSimpleDatabase provides simple,
        LocalRowSearch provides state.search,
        LocalCellLinkLabels provides cellLinkLabels,
        LocalTitleEditRequest provides TitleEditRequest(editTitleOf, viewModel::consumeEditTitle),
        LocalRowLimits provides rowLimits
    ) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            // Dentro una pagina scorre la pagina: è lei che deve tenere la
            // cella sopra la tastiera, titolo compreso. A schermo intero lo
            // fa il riquadro delle righe, più sotto.
            .then(if (embedded) Modifier.keepFocusedAboveKeyboard() else Modifier.fillMaxSize())
    ) {
        // Il titolo si può nascondere, ma **solo dentro una pagina**:
        // lì la pagina ha già un suo titolo e un database che si
        // chiama "Untitled" sopra al contenuto è solo una riga di
        // rumore. A schermo intero il titolo c'è sempre, perché è
        // l'unica cosa che dice dove si è finiti.
        val showTitle = !embedded || hostPage?.showEmbeddedTitle != false
        // Mentre si scrive comanda il testo locale, non quello che
        // torna dal database: il salvataggio è immediato ma il giro di
        // ritorno no, e se nel frattempo è stato battuto un altro tasto
        // rimetterebbe il titolo com'era un attimo prima, mangiandosi
        // il carattere. Fuori dalla scrittura comanda il database, così
        // una rinomina fatta nell'altra schermata si vede appena si
        // torna qui.
        var titleFocused by remember { mutableStateOf(false) }
        var titleText by remember { mutableStateOf(hostPage?.title.orEmpty()) }
        LaunchedEffect(hostPage?.title, titleFocused) {
            if (!titleFocused) titleText = hostPage?.title.orEmpty()
        }
        if (showTitle) {
        Row(
            // Dentro una pagina sul PC, i sei puntini del blocco si mettono a metà
            // di questa riga, subito a sinistra dell'icona o del nome (`BlockHandle.kt`).
            modifier = Modifier.fillMaxWidth().reportsFirstLine(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // **L'icona del database, prima del nome.** Solo se c'è:
            // senza, il nome parte dal margine come prima e non resta
            // nessun buco. Toccandola si cambia o si toglie; per
            // metterne una la prima volta c'è la voce "Icon" nelle
            // impostazioni. Più piccola dentro una pagina, dove anche
            // il nome lo è.
            val databaseIcon = dataPage?.iconImage
            val databaseEmoji = dataPage?.chosenEmoji
            val databaseSymbol = dataPage?.iconSymbol
            if (databaseIcon != null) {
                PageImage(
                    fileName = databaseIcon,
                    store = imageStore,
                    contentDescription = EditorStrings.icon,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(if (embedded) 26.dp else 40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .let { base ->
                            if (contentLocked) base else base.clickable { editingIcon = true }
                        }
                )
            } else if (databaseSymbol != null) {
                // Un'icona di Material (dal 08/10/2026), grande quanto l'immagine.
                SymbolIcon(
                    databaseSymbol,
                    if (embedded) 26.dp else 40.dp,
                    Modifier
                        .padding(start = 16.dp)
                        .let { base ->
                            if (contentLocked) base else base.clickable { editingIcon = true }
                        }
                )
            } else if (databaseEmoji != null) {
                // Un'emoji scelta col selettore (dal 07/10/2026), grande
                // quanto l'immagine.
                Text(
                    databaseEmoji,
                    fontSize = if (embedded) 20.sp else 32.sp,
                    fontFamily = emojiFontFamily,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .let { base ->
                            if (contentLocked) base else base.clickable { editingIcon = true }
                        }
                )
            }
            if (linkedSourceId != null) {
                // **Vista collegata: "↗ Nome del database"**, come su
                // Notion. Il nome è quello vivo dell'origine e non si
                // scrive da qui; toccandolo si va al database principale.
                LinkedViewTitle(
                    title = dataPage?.title.orEmpty(),
                    embedded = embedded,
                    onClick = onOpenSource?.let { open -> { open(linkedSourceId) } },
                    modifier = Modifier.weight(1f)
                )
            } else
            // Il titolo sta nel contenuto e non nella barra in alto,
            // come su Notion; incorporato è più contenuto, perché lì
            // il titolo della pagina è un altro.
            AutoCaps {
                BasicTextField(
                    value = titleText,
                    onValueChange = {
                        val singleLine = it.replace("\n", "")
                        titleText = singleLine
                        hostViewModel.updateTitle(singleLine)
                    },
                    singleLine = true,
                    visualTransformation = emojiTransformation,
                    // Pagina bloccata: nemmeno il titolo si tocca.
                    readOnly = hostPage?.isLocked == true,
                    // Un titolo non ha righe: la tastiera deve offrire
                    // "fine", non un invio che non farebbe niente.
                    keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    textStyle = if (embedded) {
                        MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        MaterialTheme.typography.headlineMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .markFocusChanges()
                        .onFocusChanged { titleFocused = it.isFocused }
                )
            }

        }
        }

        val layout = page?.databaseLayout ?: DatabaseLayout.TABLE
        val chartSettings = parseChartSettings(page?.chartSettings)

        // **Toccata la lente, la barra della ricerca prende il posto di
        // questa riga**, lì dov'era, come su Notion: si scrive subito, e
        // la X la chiude e rimette la riga degli strumenti.
        if (searchOpen) {
            DatabaseSearchBar(
                query = searchQuery,
                noResults = searchQuery.isNotBlank() && state.rows.isEmpty(),
                requestFocus = focusSearchField,
                onFocusRequested = { focusSearchField = false },
                onQueryChange = viewModel::setSearchQuery,
                onClose = {
                    focusManager.clearFocus()
                    viewModel.closeSearch()
                }
            )
        } else ViewToolbar(
            firstLine = !showTitle,
            layout = layout,
            viewName = page?.viewName,
            sortActive = page?.sortRules().orEmpty().existingIn(state.columns).isNotEmpty(),
            filterActive = viewModel.filterColumn() != null,
            // Cercare non cambia niente di come è fatto il database: c'è
            // anche con "Lock view".
            onOpenSearch = {
                focusSearchField = true
                viewModel.openSearch()
            },
            onOpenBlockMenu = onOpenBlockMenu?.let { open ->
                {
                    focusManager.clearFocus()
                    open()
                }
            },
            onOpenFullPage = onOpenFullPage,
            onOpenFilter = if (viewLocked) {
                null
            } else {
                {
                    focusManager.clearFocus()
                    filterStart = null
                    showFilter = true
                }
            },
            onOpenSort = if (viewLocked) {
                null
            } else {
                {
                    focusManager.clearFocus()
                    showSort = true
                }
            },
            onOpenViews = onOpenViews,
            onAddView = onAddView,
            onOpenSettings = {
                focusManager.clearFocus()
                settingsInitialPage = SettingsPage.ROOT
                showSettings = true
            }
        )

        Spacer(modifier = Modifier.size(8.dp))

        // A pagina intera le righe scorrono qui sotto: è il riquadro in cima
        // al quale resta la riga dei titoli ("Freeze first row"). Dentro una
        // pagina scorre la pagina, che lo dice da sé.
        val bodyScroll = rememberScrollState()
        val ownViewport = remember(bodyScroll) { StickyViewport.forScroll(bodyScroll) }

        // Se la vista non ha la proprietà su cui si regge, non
        // mostra nulla: un pulsante che crea pagine invisibili è
        // peggio di nessun pulsante. Il messaggio sopra dice cosa
        // manca e offre di crearlo.
        val canShowNewRows = !contentLocked && when (layout) {
            // Il calendario non ha bisogno di una proprietà data
            // già pronta: se manca, la crea la pagina stessa
            // appena la si mette su un giorno.
            DatabaseLayout.TIMELINE -> viewModel.calendarDateColumn() != null
            DatabaseLayout.BOARD -> viewModel.boardGroupColumn() != null
            else -> true
        }
        val newRowsButton: @Composable () -> Unit = {
            if (canShowNewRows) {
            TextButton(
                onClick = {
                    // Una pagina creata da qui deve comparire dove la si
                    // sta guardando. Nella tabella e nell'elenco basta
                    // aggiungerla in fondo; nel calendario finirebbe fra
                    // quelle senza data, e nella bacheca nella colonna in
                    // fondo a destra — cioè fuori da quello che si ha
                    // davanti, che è esattamente il motivo per cui
                    // sembrava non venisse creata.
                    if (layout == DatabaseLayout.CALENDAR) {
                        // Datata oggi, creando la proprietà data se il
                        // database non ne ha ancora una.
                        viewModel.addRowOnDateCreatingColumn(todayAtStartOfDay())
                    } else if (layout == DatabaseLayout.TIMELINE) {
                        val dateColumn = viewModel.calendarDateColumn()
                        if (dateColumn != null) {
                            viewModel.addRowOnDate(dateColumn.id, todayAtStartOfDay())
                        } else {
                            viewModel.addRow()
                        }
                    } else {
                        viewModel.addRow()
                    }
                    newRowTick++
                },
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(if (simple) DbStrings.newRow else DbStrings.newPage)
            }
            }
        }
        // **A pagina intera, solo le righe sullo schermo** (dal 18/10/2026):
        // tabella, elenco e galleria sono una lista che disegna quello che si
        // vede, e il pulsante delle righe nuove è il suo ultimo pezzo. Le altre
        // viste, e tutte dentro una pagina, sono come prima (vedi `RowLimits`).
        val lazyBody = if (!embedded && layout in LAZY_FULL_PAGE_LAYOUTS) LazyBody(newRowTick, newRowsButton) else null

        CompositionLocalProvider(LocalStickyViewport provides (if (embedded) LocalStickyViewport.current else ownViewport)) {
        Column(
            modifier = if (embedded) {
                Modifier.fillMaxWidth()
            } else if (lazyBody != null) {
                Modifier.weight(1f)
            } else {
                Modifier
                    .weight(1f)
                    .verticalScroll(bodyScroll)
                    .keepFocusedAboveKeyboard()
                    .stickyViewport(ownViewport)
            }
        ) {
            when (layout) {
                DatabaseLayout.TABLE -> TableLayout(
                    state = state,
                    viewLocked = viewLocked,
                    onMoveProperty = { column, delta -> viewModel.moveColumn(column.id, delta) },
                    nameColumnOrder = dataPage?.nameColumnOrder ?: -1,
                    onMoveName = { delta -> viewModel.moveColumn(NAME_COLUMN_KEY, delta) },
                    onRenameName = viewModel::renameNameColumn,
                    onHideProperty = { viewModel.setColumnHidden(it.id, true) },
                    // I tre puntini accanto al "+": dritti a "Property visibility".
                    onShowProperties = {
                        settingsInitialPage = SettingsPage.PROPERTY_VISIBILITY
                        showSettings = true
                    },
                    onToggleCenterProperty = { column, alignment ->
                        viewModel.setColumnAlignment(column.id, alignment)
                    },
                    onAddProperty = {
                        focusManager.clearFocus()
                        propertyTarget = PropertyTarget.New()
                    },
                    onTitleChange = { row, title -> viewModel.updateRowTitle(row, title) },
                    onTitleEditing = { editing -> if (editing) viewModel.startTitleEdit() else viewModel.endTitleEdit() },
                    calculations = calculations,
                    onCalculate = {
                        focusManager.clearFocus()
                        calculateTarget = it
                    },
                    calculationAlignments = parseCalculationAlignments(page?.viewCalculations),
                    onCalculationAlign = { key, alignment -> viewModel.setCalculationAlignment(key, alignment) },
                    onSetAllChecked = { column, checked -> viewModel.setAllChecked(column, checked) },
                    columnWidths = parseColumnWidths(page?.columnWidths),
                    fitColumns = parseFitColumns(page?.fitColumns),
                    onSetColumnWidth = { key, width -> viewModel.setColumnWidth(key, width) },
                    onSetFitToContent = { key, fit -> viewModel.setFitToContent(key, fit) },
                    onToggleSubRows = { row -> viewModel.setSubRowsCollapsed(row, !row.subRowsCollapsed) },
                    calculationSubRows = parseCalculationSubRows(page?.viewCalculations),
                    cellOverflow = CellOverflow.of(page?.cellOverflow ?: 0),
                    freezeFirstColumn = page?.freezeFirstColumn == true,
                    freezeFirstRow = page?.freezeFirstRow == true,
                    onOpenRow = openRow,
                    onRowLongPress = { row, place ->
                        focusManager.clearFocus()
                        rowPlace = place
                        rowActionsFor = row
                    },
                    onSetCellValue = { row, column, value ->
                        viewModel.setCellValue(row, column, value)
                    },
                    onAddOption = { column, label ->
                        viewModel.addSelectOption(column.id, label)
                    },
                    onRenameOption = { columnId, old, new ->
                        viewModel.renameSelectOption(columnId, old, new)
                    },
                    onSetOptionColor = { columnId, label, hex ->
                        viewModel.setSelectOptionColor(columnId, label, hex)
                    },
                    onDeleteOption = { columnId, label ->
                        viewModel.deleteSelectOption(columnId, label)
                    },
                    onMoveOptions = { columnId, labels ->
                        viewModel.moveSelectOptions(columnId, labels)
                    },
                    onSetOptionSecondColor = { columnId, label, hex, line ->
                        viewModel.setSelectOptionSecondColor(columnId, label, hex, line)
                    },
                    groupLevels = viewModel.tableGroupLevels(),
                    onCollapseGroup = { column, key, shut -> viewModel.setGroupCollapsed(false, column, key, shut) },
                    onAddRowInGroups = { keys ->
                        viewModel.addRowInGroups(viewModel.tableGroupLevels().zip(keys) { level, key -> level.column.id to key })
                        newRowTick++
                    },
                    lazy = lazyBody
                )

                DatabaseLayout.BOARD -> BoardLayout(
                    state = state,
                    levels = viewModel.boardGroupLevels(),
                    onCollapseGroup = { column, key, shut -> viewModel.setGroupCollapsed(true, column, key, shut) },
                    colorCards = page?.colorBoardCards == true,
                    hideNew = page?.boardHideNew == true,
                    newRowTick = newRowTick,
                    onOpenRow = openRow,
                    onRowLongPress = { row ->
                        focusManager.clearFocus()
                        rowActionsFor = row
                    },
                    onAddRowInGroups = { keys ->
                        viewModel.addRowInGroups(viewModel.boardGroupLevels().zip(keys) { level, key -> level.column.id to key })
                    },
                    onAddGroupProperty = {
                        focusManager.clearFocus()
                        propertyTarget = PropertyTarget.New(ColumnType.SELECT)
                    }
                )

                DatabaseLayout.CALENDAR -> CalendarLayout(
                    state = state,
                    dateColumn = viewModel.calendarDateColumn(),
                    mode = page?.calendarMode ?: CalendarMode.MONTH,
                    newRowTick = newRowTick,
                    onSetMode = { viewModel.setCalendarMode(it) },
                    onOpenRow = openRow,
                    onRowLongPress = { row ->
                        focusManager.clearFocus()
                        rowActionsFor = row
                    },
                    onAddRowOnDate = { millis ->
                        viewModel.addRowOnDateCreatingColumn(millis)
                    },
                    draft = eventDraft,
                    // Col database bloccato i quadratini non creano niente.
                    onSlotTap = if (contentLocked) {
                        null
                    } else {
                        { at ->
                            focusManager.clearFocus()
                            eventFullEditor = false
                            eventDraft = EventDraft.at(at)
                        }
                    },
                    onAddDateProperty = {
                        focusManager.clearFocus()
                        propertyTarget = PropertyTarget.New(ColumnType.DATE)
                    }
                )

                DatabaseLayout.TIMELINE -> TimelineLayout(
                    state = state,
                    dateColumn = viewModel.calendarDateColumn(),
                    zoom = page?.timelineZoom ?: TimelineZoom.DAY,
                    newRowTick = newRowTick,
                    onSetZoom = { viewModel.setTimelineZoom(it) },
                    onOpenRow = openRow,
                    onRowLongPress = { row ->
                        focusManager.clearFocus()
                        rowActionsFor = row
                    },
                    onAddDateProperty = {
                        focusManager.clearFocus()
                        propertyTarget = PropertyTarget.New(ColumnType.DATE)
                    }
                )

                DatabaseLayout.LIST -> ListLayout(
                    state = state,
                    onOpenRow = openRow,
                    onRowLongPress = { row ->
                        focusManager.clearFocus()
                        rowActionsFor = row
                    },
                    lazy = lazyBody
                )

                DatabaseLayout.CHART -> ChartLayout(state = state, settings = chartSettings)

                DatabaseLayout.GALLERY -> GalleryLayout(
                    state = state,
                    // Copertina e testo sono della pagina della riga: in
                    // un database semplice le pagine non ci sono, e le
                    // schede hanno solo nome e proprietà.
                    preview = if (simple) {
                        GalleryCardPreview.NONE
                    } else {
                        page?.galleryCardPreview ?: GALLERY_DEFAULT_PREVIEW
                    },
                    size = page?.galleryCardSize ?: GALLERY_DEFAULT_SIZE,
                    covers = rowCovers,
                    contentPreviews = rowPreviews,
                    lockedRows = lockedRows,
                    onOpenRow = openRow,
                    onRowLongPress = { row ->
                        focusManager.clearFocus()
                        rowActionsFor = row
                    },
                    repositioningRowId = repositioningRowId,
                    onRepositionSave = { row, scale, offset ->
                        viewModel.setRowCardCover(row, scale, offset.x, offset.y)
                        repositioningRowId = null
                    },
                    onRepositionCancel = { repositioningRowId = null },
                    lazy = lazyBody
                )
            }

            // A pagina intera tabella, elenco e galleria l'hanno già in fondo.
            if (lazyBody == null) newRowsButton()
        }
        }
    }
    }

    if (editingIcon) {
        IconPickerSheet(
            store = imageStore,
            hasIcon = dataPage?.hasChosenIcon == true,
            onPick = { viewModel.setIcon(it) },
            onDismiss = { editingIcon = false }
        )
    }

    if (showSettings) {
        SettingsSheet(
            hasIcon = dataPage?.hasChosenIcon == true,
            // Dentro una pagina l'icona sta nei sei puntini ("Edit icon"): qui
            // c'è solo a schermo intero, dove i sei puntini non ci sono (dal
            // 21/10/2026, chiesto dall'utente).
            onEditIcon = if (contentLocked || onOpenBlockMenu != null) {
                null
            } else {
                {
                    showSettings = false
                    editingIcon = true
                }
            },
            columns = state.columns,
            layout = page?.databaseLayout ?: DatabaseLayout.TABLE,
            groupColumn = viewModel.boardGroupColumn(),
            dateColumn = viewModel.calendarDateColumn(),
            tableGroupRules = viewModel.groupRulesShown(board = false),
            boardGroupRules = viewModel.groupRulesShown(board = true),
            groupActions = remember(viewModel) {
                GroupRuleActions(
                    add = viewModel::addGroupRule,
                    setColumn = viewModel::setGroupRuleColumn,
                    move = viewModel::moveGroupRule,
                    remove = viewModel::removeGroupRule,
                    removeAll = viewModel::removeAllGroupRules,
                    setHideEmpty = viewModel::setGroupRuleHideEmpty,
                    setDateBy = viewModel::setGroupRuleDateBy,
                    setOrder = viewModel::setGroupRuleOrder,
                    setManual = viewModel::setGroupRuleManual
                )
            },
            // Tutti i gruppi della proprietà, anche vuoti, tranne quello senza valore (resta in fondo).
            groupsOf = { column, rule ->
                buildRowGroups(state, column, hideEmpty = false, order = GroupOrder.MANUAL, manual = rule.manual).filter { it.key.isNotEmpty() }
            },
            colorBoardCards = page?.colorBoardCards == true,
            onSetColorBoardCards = { viewModel.setColorBoardCards(it) },
            boardHideNew = page?.boardHideNew == true,
            onSetBoardHideNew = { viewModel.setBoardHideNew(it) },
            cellOverflow = CellOverflow.of(page?.cellOverflow ?: 0),
            onSetCellOverflow = { viewModel.setCellOverflow(it) },
            freezeFirstColumn = page?.freezeFirstColumn == true,
            onSetFreezeFirstColumn = { viewModel.setFreezeFirstColumn(it) },
            freezeFirstRow = page?.freezeFirstRow == true,
            onSetFreezeFirstRow = { viewModel.setFreezeFirstRow(it) },
            onSetColumnHidden = { columnId, hidden ->
                viewModel.setColumnHidden(columnId, hidden)
            },
            onSetAllColumnsHidden = { viewModel.setAllColumnsHidden(it) },
            filterColumn = viewModel.filterColumn(),
            filterValue = page?.filterValue.orEmpty(),
            onOpenFilter = {
                showSettings = false
                showFilter = true
            },
            initialPage = settingsInitialPage,
            onDismiss = { showSettings = false },
            // Scelta la vista, la finestra si chiude: è l'unica cosa
            // che si era venuti a fare.
            onSetLayout = {
                viewModel.setLayout(it)
                showSettings = false
                // Scelto "Chart", la finestrella dei quattro grafici.
                if (it == DatabaseLayout.CHART) choosingChartType = true
            },
            chartSettings = parseChartSettings(page?.chartSettings),
            onSetChartSettings = { viewModel.setChartSettings(it) },
            onPickChartType = {
                showSettings = false
                choosingChartType = true
            },
            onSetDateColumn = { viewModel.setCalendarDateColumn(it.id) },
            galleryPreview = page?.galleryCardPreview ?: GALLERY_DEFAULT_PREVIEW,
            gallerySize = page?.galleryCardSize ?: GALLERY_DEFAULT_SIZE,
            simple = simple,
            onSetGalleryPreview = { viewModel.setGalleryCardPreview(it) },
            onSetGallerySize = { viewModel.setGalleryCardSize(it) },
            onEditProperty = { column ->
                showSettings = false
                propertyTarget = PropertyTarget.Existing(column)
            },
            onAddProperty = {
                showSettings = false
                propertyTarget = PropertyTarget.New()
            },
            // L'interruttore del titolo esiste solo per il database
            // dentro una pagina: `embedded` è la stessa condizione per
            // cui il titolo si può nascondere.
            onSetShowTitle = if (embedded) {
                { show -> hostViewModel.setShowEmbeddedTitle(show) }
            } else {
                null
            },
            showTitle = hostPage?.showEmbeddedTitle != false,
            viewLocked = viewLocked
        )
    }

    if (choosingChartType) {
        val current = parseChartSettings(page?.chartSettings)
        ChartTypeDialog(
            current = current.type,
            onPick = {
                viewModel.setChartSettings(current.copy(type = it))
                choosingChartType = false
            },
            onDismiss = { choosingChartType = false }
        )
    }

    if (showFilter) {
        FilterSheet(
            columns = state.columns,
            filterColumnId = page?.filterColumnId,
            filterValue = page?.filterValue.orEmpty(),
            startColumnId = filterStart,
            onSetFilter = { columnId, value -> viewModel.setFilter(columnId, value) },
            onClear = { viewModel.clearFilter() },
            onDismiss = { showFilter = false }
        )
    }

    if (showSort) {
        val rules = page?.sortRules().orEmpty().existingIn(state.columns)
        SortSheet(
            // Un bottone non ha un valore per cui ordinare (dal 27/10/2026).
            columns = state.columns.filter { it.type != ColumnType.BUTTON },
            rules = rules,
            onPickFirst = { columnId ->
                // Scegliendo una proprietà si parte sempre dal crescente: è
                // il verso che ci si aspetta. Ritoccando quella che è già la
                // prima, il verso si rovescia.
                if (rules.firstOrNull()?.column == columnId) {
                    viewModel.toggleSortRule(columnId)
                } else {
                    viewModel.setSort(columnId, descending = false)
                }
            },
            onAdd = { viewModel.addSortRule(it) },
            onToggle = { viewModel.toggleSortRule(it) },
            onMove = { columnId, by -> viewModel.moveSortRule(columnId, by) },
            onRemove = { viewModel.removeSortRule(it) },
            onRemoveAll = { viewModel.clearSortRules() },
            onDismiss = { showSort = false }
        )
    }

    propertyTarget?.let { target ->
        PropertySheet(
            target = target,
            viewModel = viewModel,
            onDismiss = { propertyTarget = null },
            onSave = { name, type, optionsJson, number, duration, twoWayName ->
                when {
                    target is PropertyTarget.New -> viewModel.addColumn(name, type, optionsJson, twoWayName)
                    target is PropertyTarget.Existing && type == ColumnType.RELATION ->
                        viewModel.updateRelationColumn(target.column.id, name, optionsJson, twoWayName)
                    target is PropertyTarget.Existing ->
                        viewModel.updateColumn(target.column.id, name, type, optionsJson, number, duration = duration)
                }
                propertyTarget = null
            },
            onDelete = {
                if (target is PropertyTarget.Existing) viewModel.deleteColumn(target.column)
                propertyTarget = null
            }
        )
    }

    calculateTarget?.let { target ->
        CalculateSheet(
            title = target.title,
            type = target.type,
            current = calculations[target.key],
            onPick = {
                viewModel.setCalculation(target.key, it)
                calculateTarget = null
            },
            onDismiss = { calculateTarget = null },
            // Solo la tabella ha le righe dei calcoli, e le sotto-righe.
            includeSubRows = target.key in parseCalculationSubRows(page?.viewCalculations),
            onIncludeSubRows = { viewModel.setCalculationSubRows(target.key, it) }
        )
    }

    rowActionsFor?.let { row ->
        // La riga arriva dallo stato aggiornato, non dalla copia
        // catturata al momento del tocco: altrimenti le date in fondo
        // e i valori delle proprietà resterebbero quelli di allora.
        val live = state.rows.find { it.id == row.id } ?: row
        // Le righe accanto, come si vedono adesso: nella tabella intera, nel
        // gruppo o nel sottogruppo dove la si è toccata.
        val place = rowPlace
        val siblings = place?.let { rowsAt(state, viewModel.tableGroupLevels(), it.path) }
        // Solo fra le righe dello stesso livello: una sotto-riga si sposta fra
        // le sorelle, sotto la sua riga; una principale fra le principali.
        val level = siblings?.let { list ->
            val shownIds = list.mapTo(HashSet()) { it.id }
            fun parentOf(r: DatabaseRowEntity) = r.parentRowId?.takeIf { it in shownIds && it != r.id }
            list.filter { parentOf(it) == parentOf(live) }
        }
        val index = level?.indexOfFirst { it.id == live.id } ?: -1
        // Con un ordinamento le righe stanno dove dice lui: spostarle a mano
        // non si vedrebbe. Con il database bloccato non si spostano.
        val sorted = page?.sortRules().orEmpty().existingIn(state.columns).isNotEmpty()
        val canMove = !sorted && !contentLocked && index >= 0
        val move = level?.let {
            RowMoveActions(
                onUp = level.getOrNull(index - 1)?.takeIf { canMove }?.let { above ->
                    { viewModel.moveRowNextTo(live, above, before = true) }
                },
                onDown = level.getOrNull(index + 1)?.takeIf { canMove }?.let { below ->
                    { viewModel.moveRowNextTo(live, below, before = false) }
                },
                hint = if (sorted) DbStrings.rowsFollowSort else null
            )
        }
        // "Create sub row": dalla tabella, col database non bloccato. La
        // sotto-riga prende i valori dei gruppi della riga, per starle sotto.
        val onCreateSubRow: (() -> Unit)? = if (place != null && !contentLocked) {
            {
                rowActionsFor = null
                viewModel.addSubRow(live, inherit = viewModel.tableGroupLevels().map { it.column.id })
            }
        } else {
            null
        }
        RowActionsSheet(
            row = live,
            move = move,
            onCreateSubRow = onCreateSubRow,
            // **"Edit event"** (dal 30/09/2026): nel calendario, l'evento
            // intero — ore, ripetizione, colore — nella finestra di "More options".
            onEditEvent = if (page?.databaseLayout == DatabaseLayout.CALENDAR && !contentLocked) {
                {
                    rowActionsFor = null
                    val range = viewModel.calendarDateColumn()?.let { column ->
                        parseDateRange(state.cellValues[live.id to column.id].orEmpty())
                    }
                    eventFullEditor = true
                    eventDraft = EventDraft.of(live, range, AppSettings.zoneId, LocalDate.now().atTime(9, 0))
                }
            } else {
                null
            },
            columns = state.columns,
            cellValues = state.cellValues,
            onDismiss = { rowActionsFor = null },
            onSetCellValue = { column, value -> viewModel.setCellValue(live, column, value) },
            onAddOption = { column, label -> viewModel.addSelectOption(column.id, label) },
            onRenameOption = { columnId, old, new ->
                viewModel.renameSelectOption(columnId, old, new)
            },
            onSetOptionColor = { columnId, label, hex ->
                viewModel.setSelectOptionColor(columnId, label, hex)
            },
            onDeleteOption = { columnId, label -> viewModel.deleteSelectOption(columnId, label) },
            onMoveOptions = { columnId, labels -> viewModel.moveSelectOptions(columnId, labels) },
            onSetOptionSecondColor = { columnId, label, hex, line -> viewModel.setSelectOptionSecondColor(columnId, label, hex, line) },
            hasIcon = rowIcons[live.id] != null,
            iconEditable = !contentLocked,
            simple = simple,
            onTitleChange = { title -> viewModel.updateRowTitle(live, title) },
            onOpenAs = opener?.takeIf { !simple }?.let { pageOpener ->
                { mode ->
                    rowActionsFor = null
                    viewModel.openRow(live) { id -> pageOpener.open(id, false, mode) }
                }
            },
            onEditIcon = {
                rowActionsFor = null
                iconForRow = live
            },
            // Solo dove la copertina si vede nella scheda: galleria con
            // "Page cover", una copertina da spostare, e il contenuto non
            // bloccato.
            onRepositionCover = if (
                page?.databaseLayout == DatabaseLayout.GALLERY &&
                !simple &&
                (page?.galleryCardPreview ?: GALLERY_DEFAULT_PREVIEW) == GalleryCardPreview.PAGE_COVER &&
                rowCovers.containsKey(live.id) &&
                !contentLocked
            ) {
                {
                    rowActionsFor = null
                    repositioningRowId = live.id
                }
            } else {
                null
            },
            onDelete = {
                rowActionsFor = null
                rowPendingDeletion = live
            }
        )
    }

    eventDraft?.let { draft ->
        val saveDraft = {
            viewModel.saveEvent(draft.rowId, draft.title.trim(), draft.cellValue(AppSettings.zoneId), draft.color)
            eventDraft = null
        }
        if (eventFullEditor) {
            EventEditorSheet(
                draft = draft,
                onChange = { eventDraft = it },
                onSave = saveDraft,
                onDismiss = { eventDraft = null }
            )
        } else {
            EventQuickDialog(
                draft = draft,
                onChange = { eventDraft = it },
                onSave = saveDraft,
                onMoreOptions = { eventFullEditor = true },
                onDismiss = { eventDraft = null }
            )
        }
    }

    // L'icona della pagina di una riga: lo stesso selettore delle pagine e
    // dei database (emoji, Upload, Remove). La pagina della riga, se non
    // c'era ancora, nasce qui: l'icona sta nella pagina.
    iconForRow?.let { row ->
        IconPickerSheet(
            store = imageStore,
            hasIcon = rowIcons[row.id] != null,
            onPick = { choice -> viewModel.setRowIcon(row, choice) },
            onDismiss = { iconForRow = null }
        )
    }

    rowPendingDeletion?.let { row ->
        FitAlertDialog(
            onDismissRequest = { rowPendingDeletion = null },
            title = { Text(DbStrings.deletePageTitle) },
            text = {
                // Le sotto-righe non se ne vanno con lei: lo si dice prima.
                val withSubRows = state.rows.any { it.parentRowId == row.id }
                Text(if (withSubRows) DbStrings.deleteRowText + "\n\n" + DbStrings.subRowsStay else DbStrings.deleteRowText)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRow(row, imageStore)
                    rowPendingDeletion = null
                }) { Text(Strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { rowPendingDeletion = null }) { Text(Strings.cancel) }
            }
        )
    }
}

/**
 * La visualizzazione a tabella: la griglia classica, con le colonne che
 * scorrono in orizzontale.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TableLayout(
    state: DatabaseTableState,
    /**
     * **I livelli dei gruppi** (dal 28/10/2026, quanti se ne vogliono): il
     * primo fa i gruppi, il secondo divide ogni gruppo, e così via. Nessuno =
     * la tabella piatta di sempre.
     */
    groupLevels: List<GroupLevel>,
    /** La freccia di un gruppo: (proprietà del livello, chiave del gruppo, chiuso). */
    onCollapseGroup: (String, String, Boolean) -> Unit,
    /** "Lock view": le colonne non si spostano, e le due voci restano grigie. */
    viewLocked: Boolean,
    onMoveProperty: (DatabaseColumnEntity, Int) -> Unit,
    /** Dove sta Name fra le colonne (`PageEntity.nameColumnOrder`, vedi `namePosition`). */
    nameColumnOrder: Int = -1,
    /** "Move to left" (-1) / "Move to right" (+1) di Name. */
    onMoveName: (Int) -> Unit = {},
    /** Rinomina la proprietà principale (vedi `NameHeaderCell`). */
    onRenameName: ((String) -> Unit)? = null,
    onHideProperty: (DatabaseColumnEntity) -> Unit,
    onToggleCenterProperty: (DatabaseColumnEntity, CellAlignment) -> Unit,
    onAddProperty: () -> Unit,
    onTitleChange: (DatabaseRowEntity, String) -> Unit,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    /** Tenendo premuto il nome: la riga, e in quale gruppo la si è toccata. */
    onRowLongPress: (DatabaseRowEntity, RowPlace) -> Unit,
    onSetCellValue: (DatabaseRowEntity, DatabaseColumnEntity, String) -> Unit,
    onAddOption: (DatabaseColumnEntity, String) -> Unit,
    onRenameOption: (String, String, String) -> Unit,
    onSetOptionColor: (String, String, String) -> Unit,
    onDeleteOption: (String, String) -> Unit,
    onMoveOptions: (String, List<String>) -> Unit,
    onSetOptionSecondColor: (String, String, String?, Boolean) -> Unit,
    /** Crea una pagina già dentro un gruppo: riceve le chiavi dei gruppi in cui sta, dal primo livello. */
    onAddRowInGroups: (List<String>) -> Unit,
    /** Si scrive il nome di una riga (true) o si è finito (false): l'ordine resta fermo intanto. */
    onTitleEditing: (Boolean) -> Unit = {},
    /** I calcoli di questa vista, per colonna (vedi `CalculationFooter`). */
    calculations: Map<String, Calculation> = emptyMap(),
    onCalculate: (CalculateTarget) -> Unit = {},
    calculationAlignments: Map<String, CellAlignment> = emptyMap(),
    onCalculationAlign: (String, CellAlignment) -> Unit = { _, _ -> },
    /** "Activate all" / "Deactivate all" di una colonna Checkbox. */
    onSetAllChecked: (DatabaseColumnEntity, Boolean) -> Unit = { _, _ -> },
    /** Le larghezze di questa vista, in dp per chiave (vedi `ColumnWidths.kt`). */
    columnWidths: Map<String, Int> = emptyMap(),
    /** Le colonne larghe quanto il loro contenuto ("Fit to content"). */
    fitColumns: Set<String> = emptySet(),
    onSetColumnWidth: (String, Int) -> Unit = { _, _ -> },
    onSetFitToContent: (String, Boolean) -> Unit = { _, _ -> },
    /** La freccia sul nome di una riga con sotto-righe: le apre o le chiude. */
    onToggleSubRows: (DatabaseRowEntity) -> Unit = {},
    /** I calcoli che contano anche le sotto-righe ("Include sub rows"). */
    calculationSubRows: Set<String> = emptySet(),
    /** Il testo lungo nelle celle: tre puntini, colonna allargata o a capo (vedi `CellOverflow`). */
    cellOverflow: CellOverflow = CellOverflow.TRUNCATE,
    /** "Freeze first column" (dal 08/10/2026): la prima colonna resta a sinistra (vedi `FrozenTable.kt`). */
    freezeFirstColumn: Boolean = false,
    /** "Freeze first row": la riga dei titoli resta in cima scorrendo in giù. */
    freezeFirstRow: Boolean = false,
    /** A pagina intera: una lista che disegna solo le righe sullo schermo (vedi `LazyBody`). */
    lazy: LazyBody? = null,
    /** I tre puntini accanto al "+": "Property visibility" (vedi `AddPropertyCell`). */
    onShowProperties: (() -> Unit)? = null
) {
    val grouped = groupLevels.isNotEmpty()
    // Dentro un contenitore che scorre in orizzontale la larghezza
    // disponibile è illimitata: una linea che chiede "tutta la
    // larghezza" ne ottiene zero e sparisce. Le linee orizzontali vanno
    // quindi disegnate larghe quanto la tabella, calcolandolo.
    // Le colonne nascoste non occupano spazio: la larghezza si conta
    // su quelle che si vedono, altrimenti la tabella resterebbe larga
    // com'era e nasconderne una non servirebbe a niente.
    val shownColumns = state.visibleColumns
    // Name al suo posto fra quelle che si vedono (dal 29/09/2026 si sposta).
    val namePlace = namePosition(shownColumns, nameColumnOrder)
    val resize = remember { ColumnResize() }
    // Con "Lock view" le colonne non cambiano misura: via il segnalino.
    LaunchedEffect(viewLocked) { if (viewLocked) resize.stop() }
    // **Il gesto indietro chiude il segnalino** (dal 29/09/2026): finite le
    // modifiche alla larghezza si torna alla tabella, invece di uscire dalla
    // pagina. Senza segnalino il gesto fa quello di sempre.
    BackHandler(enabled = resize.key != null) { resize.stop() }
    // **"Widen the column"** (dal 30/09/2026): ogni colonna almeno larga
    // quanto il suo contenuto, quindi si misurano tutte.
    val widen = cellOverflow == CellOverflow.WIDEN
    val fitKeys = if (widen) shownColumns.mapTo(mutableSetOf(NAME_COLUMN_KEY)) { it.id } else fitColumns
    val fitted = fitWidths(state, shownColumns, fitKeys, calculations, calculationSubRows)
    // Se c'è almeno una sotto-riga, ogni nome lascia il posto della freccia:
    // così i nomi restano in colonna, con o senza sotto-righe.
    val treeShown = hasSubRows(state.rows)
    // Quale misura vince: vedi `tableColumnWidth`.
    fun widthOf(key: String, default: Dp, min: Int): Dp {
        val dragging = resize.draft?.takeIf { key == resize.key }
        return tableColumnWidth(
            dragging = dragging,
            fit = fitted[key]?.takeIf { key in fitColumns && dragging == null },
            manual = columnWidths[key]?.dp,
            default = default,
            fitted = fitted[key],
            widen = widen,
            fitMin = min.dp
        )
    }
    val widths = TableWidths(
        name = widthOf(NAME_COLUMN_KEY, NAME_COLUMN_WIDTH, fitMinColumnWidth(NAME_COLUMN_KEY)),
        byKey = shownColumns.associate { it.id to widthOf(it.id, defaultColumnWidth(it.id, it.type).dp, fitMinColumnWidth(it.id, it.type)) }
    )
    val columnsWidth = widths.name + SEPARATOR_WIDTH +
        shownColumns.fold(0.dp) { sum, column -> sum + widths.of(column) + SEPARATOR_WIDTH } +
        ADD_PROPERTY_WIDTH
    // **Le linee delle righe vanno fino in fondo** (dal 11/11/2026, chiesto
    // dall'utente, come su Notion): almeno larghe quanto lo spazio che ha la
    // tabella, misurato dal suo contenitore (0 finché non si sa).
    val density = LocalDensity.current
    var available by remember { mutableStateOf(0.dp) }
    val measureAvailable = Modifier.onSizeChanged { size -> available = with(density) { size.width.toDp() } }
    val tableWidth = maxOf(columnsWidth, available)

    // Quali gruppi sono chiusi: nella regola del loro livello (`GroupRuleSpec.collapsed`),
    // così restano chiusi riaprendo la pagina (dal 11/11/2026, chiesto dall'utente).

    // Intestazione e righe scorrono insieme in orizzontale, così le
    // colonne restano allineate.
    val horizontal = rememberScrollState()
    val frozenColumn = remember(horizontal, freezeFirstColumn) { if (freezeFirstColumn) FrozenColumn(horizontal) else null }
    CompositionLocalProvider(
        LocalTableWidths provides widths,
        LocalColumnResize provides resize,
        LocalWrapCells provides (cellOverflow == CellOverflow.WRAP),
        LocalFrozenColumn provides frozenColumn
    ) {
    // **A pagina intera, solo le righe sullo schermo** (dal 18/10/2026): con
    // diecimila righe disegnarle tutte voleva dire secondi. Le stesse righe,
    // intestazioni e calcoli, ma ognuno un pezzo di una lista che fa solo
    // quelli che si vedono; la lista sta dentro lo scorrimento orizzontale,
    // così le colonne restano allineate. "Freeze first row" è l'intestazione
    // ferma della lista, una per gruppo.
    if (lazy != null) {
        val listState = rememberLazyListState()
        // Una riga nuova da "New page" in fondo: la lista scende fin lì.
        val rowCount by rememberUpdatedState(state.rows.size)
        var seenTick by remember { mutableStateOf(lazy.newRowTick) }
        LaunchedEffect(lazy.newRowTick) {
            if (lazy.newRowTick == seenTick) return@LaunchedEffect
            seenTick = lazy.newRowTick
            if (grouped) return@LaunchedEffect
            val before = rowCount
            withTimeoutOrNull(2_000) { snapshotFlow { rowCount }.first { it > before } } ?: return@LaunchedEffect
            listState.animateScrollToItem((listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
        }

        @Composable
        fun HeaderRow() {
            TableHeaderRow(
                columns = shownColumns,
                viewLocked = viewLocked,
                onMoveProperty = onMoveProperty,
                namePlace = namePlace,
                onMoveName = onMoveName,
                onRenameName = onRenameName,
                onHideProperty = onHideProperty,
                onToggleCenterProperty = onToggleCenterProperty,
                onAddProperty = onAddProperty,
                onShowProperties = onShowProperties,
                onCalculate = onCalculate,
                onSetAllChecked = onSetAllChecked,
                fitColumns = fitColumns,
                onSetColumnWidth = onSetColumnWidth,
                onSetFitToContent = onSetFitToContent
            )
            HorizontalDivider(modifier = Modifier.width(tableWidth))
        }

        @Composable
        fun BodyRow(item: TreeRow, place: RowPlace) {
            val row = item.row
            TableBodyRow(
                row = row,
                depth = item.depth,
                hasSubRows = item.hasChildren,
                toggleSlot = treeShown,
                onToggleSubRows = { onToggleSubRows(row) },
                state = state,
                columns = shownColumns,
                onTitleChange = onTitleChange,
                onOpenRow = onOpenRow,
                onRowLongPress = { onRowLongPress(it, place) },
                onSetCellValue = onSetCellValue,
                onAddOption = onAddOption,
                onRenameOption = onRenameOption,
                onSetOptionColor = onSetOptionColor,
                onDeleteOption = onDeleteOption,
                onMoveOptions = onMoveOptions,
                onSetOptionSecondColor = onSetOptionSecondColor,
                onTitleEditing = onTitleEditing,
                namePlace = namePlace
            )
            HorizontalDivider(modifier = Modifier.width(tableWidth))
        }

        // Un elenco di righe (la tabella, o un gruppo): l'intestazione, le
        // righe, i calcoli, e "New page" se è un gruppo.
        val headerBackground = MaterialTheme.colorScheme.background
        fun LazyListScope.section(prefix: String, rows: List<DatabaseRowEntity>, place: RowPlace, onAddRow: (() -> Unit)?) {
            if (freezeFirstRow) {
                stickyHeader(key = "$prefix:header") {
                    Column(modifier = Modifier.background(headerBackground)) { HeaderRow() }
                }
            } else {
                item(key = "$prefix:header") { HeaderRow() }
            }
            items(rowTree(rows), key = { "$prefix:row:" + it.row.id }) { item -> BodyRow(item, place) }
            item(key = "$prefix:calc") {
                CalculationFooter(rows, state, shownColumns, calculations, onCalculate, calculationAlignments, onCalculationAlign, calculationSubRows, namePlace = namePlace)
            }
            if (onAddRow != null) item(key = "$prefix:add") { GroupNewRowButton(onAddRow) }
        }

        // **I gruppi a ogni livello** (dal 28/10/2026): dentro ogni gruppo, i
        // gruppi del livello dopo, un po' più a destra; all'ultimo, le righe.
        fun LazyListScope.groupsAt(depth: Int, rows: List<DatabaseRowEntity>, path: List<String>) {
            val level = groupLevels[depth]
            buildLevelGroups(state, level, rows).forEach { group ->
                val keys = path + group.key
                val key = keys.joinToString(SUB_GROUP_KEY_SEPARATOR)
                val isCollapsed = key in level.collapsed
                item(key = "group:$key") {
                    GroupHeaderRow(
                        group = group,
                        collapsed = isCollapsed,
                        width = tableWidth,
                        indent = SUB_GROUP_INDENT * depth,
                        onToggle = { onCollapseGroup(level.column.id, key, !isCollapsed) }
                    )
                }
                if (!isCollapsed) {
                    if (depth == groupLevels.lastIndex) {
                        section("g:$key", group.rows, RowPlace(keys)) { onAddRowInGroups(keys) }
                    } else {
                        groupsAt(depth + 1, group.rows, keys)
                    }
                }
                if (depth > 0) item(key = "gap:$key") { Spacer(modifier = Modifier.size(8.dp)) }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize().then(measureAvailable)) {
            val viewport = maxWidth
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    // Toccando la tabella fuori dal segnalino, il segnalino se ne va.
                    .pointerInput(resize) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Press) {
                                    if (!resize.handleTouched && resize.key != null) resize.stop()
                                    resize.handleTouched = false
                                }
                            }
                        }
                    }
                    .horizontalScroll(horizontal)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .width(maxOf(tableWidth, viewport))
                        .fillMaxHeight()
                        .keepFocusedAboveKeyboard()
                ) {
                    if (!grouped) {
                        section("table", state.rows, RowPlace(), null)
                    } else {
                        groupsAt(0, state.rows, emptyList())
                        item(key = "groups-end") { Spacer(modifier = Modifier.size(12.dp)) }
                    }
                    item(key = "trailing") { lazy.trailing() }
                }
            }
        }
        return@CompositionLocalProvider
    }
    Column(
        modifier = Modifier
            // Tutto lo spazio che c'è, per sapere fin dove arrivano le linee.
            .fillMaxWidth()
            .then(measureAvailable)
            // Toccando la tabella fuori dal segnalino, il segnalino se ne va.
            // Guarda soltanto: il tocco arriva comunque a chi lo aspetta.
            .pointerInput(resize) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press) {
                            if (!resize.handleTouched && resize.key != null) resize.stop()
                            resize.handleTouched = false
                        }
                    }
                }
            }
            .horizontalScroll(horizontal)
    ) {
        if (!grouped) {
            PinnedHeaderSection(pin = freezeFirstRow, header = {
            TableHeaderRow(
                columns = shownColumns,
                viewLocked = viewLocked,
                onMoveProperty = onMoveProperty,
                namePlace = namePlace,
                onMoveName = onMoveName,
                onRenameName = onRenameName,
                onHideProperty = onHideProperty,
                onToggleCenterProperty = onToggleCenterProperty,
                onAddProperty = onAddProperty,
                onShowProperties = onShowProperties,
                onCalculate = onCalculate,
                onSetAllChecked = onSetAllChecked,
                fitColumns = fitColumns,
                onSetColumnWidth = onSetColumnWidth,
                onSetFitToContent = onSetFitToContent
            )
            HorizontalDivider(modifier = Modifier.width(tableWidth))
            }) {
            // **Ogni riga col suo id** (dal 26/09/2026): con un ordinamento
            // una riga cambia posto, e senza l'id il campo in cui si stava
            // scrivendo finiva su un'altra riga, si chiudeva e la tastiera
            // spariva.
            // Le prime righe e "Load more" (vedi `RowLimits`).
            val limits = LocalRowLimits.current
            val shown = limits.limit("table", rowTree(state.rows)) { it.row.id }
            shown.items.forEach { item ->
                val row = item.row
                key(row.id) {
                TableBodyRow(
                    row = row,
                    depth = item.depth,
                    hasSubRows = item.hasChildren,
                    toggleSlot = treeShown,
                    onToggleSubRows = { onToggleSubRows(row) },
                    state = state,
                    columns = shownColumns,
                    onTitleChange = onTitleChange,
                    onOpenRow = onOpenRow,
                    onRowLongPress = { onRowLongPress(it, RowPlace()) },
                    onSetCellValue = onSetCellValue,
                    onAddOption = onAddOption,
                    onRenameOption = onRenameOption,
                    onSetOptionColor = onSetOptionColor,
                    onDeleteOption = onDeleteOption,
                    onMoveOptions = onMoveOptions,
                    onSetOptionSecondColor = onSetOptionSecondColor,
                    onTitleEditing = onTitleEditing,
                    namePlace = namePlace
                )
                HorizontalDivider(modifier = Modifier.width(tableWidth))
                }
            }
            if (shown.hidden > 0) LoadMoreButton(shown.hidden, onClick = { limits?.more("table") })
            CalculationFooter(state.rows, state, shownColumns, calculations, onCalculate, calculationAlignments, onCalculationAlign, calculationSubRows, namePlace = namePlace)
            }
            return@Column
        }

        // Il corpo di un gruppo o di un sottogruppo: intestazione delle
        // colonne, righe, calcoli e "New page".
        @Composable
        fun GroupBody(rows: List<DatabaseRowEntity>, place: RowPlace, onAddRow: () -> Unit) {
            // Ogni gruppo ha la sua intestazione, ferma finché si è nel gruppo.
            PinnedHeaderSection(pin = freezeFirstRow, header = {
            TableHeaderRow(
                columns = shownColumns,
                viewLocked = viewLocked,
                onMoveProperty = onMoveProperty,
                namePlace = namePlace,
                onMoveName = onMoveName,
                onRenameName = onRenameName,
                onHideProperty = onHideProperty,
                onToggleCenterProperty = onToggleCenterProperty,
                onAddProperty = onAddProperty,
                onShowProperties = onShowProperties,
                onCalculate = onCalculate,
                onSetAllChecked = onSetAllChecked,
                fitColumns = fitColumns,
                onSetColumnWidth = onSetColumnWidth,
                onSetFitToContent = onSetFitToContent
            )
            HorizontalDivider(modifier = Modifier.width(tableWidth))
            }) {
            val limits = LocalRowLimits.current
            val limitKey = "group:$place"
            val shown = limits.limit(limitKey, rowTree(rows)) { it.row.id }
            shown.items.forEach { item ->
                val row = item.row
                key(row.id) {
                TableBodyRow(
                    row = row,
                    depth = item.depth,
                    hasSubRows = item.hasChildren,
                    toggleSlot = treeShown,
                    onToggleSubRows = { onToggleSubRows(row) },
                    state = state,
                    columns = shownColumns,
                    onTitleChange = onTitleChange,
                    onOpenRow = onOpenRow,
                    onRowLongPress = { onRowLongPress(it, place) },
                    onSetCellValue = onSetCellValue,
                    onAddOption = onAddOption,
                    onRenameOption = onRenameOption,
                    onSetOptionColor = onSetOptionColor,
                    onDeleteOption = onDeleteOption,
                    onMoveOptions = onMoveOptions,
                    onSetOptionSecondColor = onSetOptionSecondColor,
                    onTitleEditing = onTitleEditing,
                    namePlace = namePlace
                )
                HorizontalDivider(modifier = Modifier.width(tableWidth))
                }
            }
            if (shown.hidden > 0) LoadMoreButton(shown.hidden, onClick = { limits?.more(limitKey) })
            // I calcoli di questo gruppo, sotto le sue righe.
            CalculationFooter(rows, state, shownColumns, calculations, onCalculate, calculationAlignments, onCalculationAlign, calculationSubRows, namePlace = namePlace)
            // Una pagina creata da dentro un gruppo nasce già con
            // quel valore: altrimenti comparirebbe nel gruppo
            // "senza valore", cioè da tutt'altra parte rispetto a
            // dove la si è chiesta.
            TextButton(
                onClick = onAddRow,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    if (LocalSimpleDatabase.current) DbStrings.newRow else DbStrings.newPage,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            }
        }

        // I primi gruppi e "Load more" (dal 26/10/2026): vedi `DATABASE_GROUP_LIMIT`.
        val groupLimits = LocalRowLimits.current

        // **I gruppi a ogni livello** (dal 28/10/2026, quanti se ne vogliono):
        // dentro ogni gruppo, i gruppi del livello dopo, un po' più a destra;
        // all'ultimo livello, le righe.
        @Composable
        fun GroupsAt(depth: Int, rows: List<DatabaseRowEntity>, path: List<String>) {
            val level = groupLevels[depth]
            val limitKey = if (depth == 0) "table-groups:" + level.column.id else "table-subgroups:" + path.joinToString(SUB_GROUP_KEY_SEPARATOR)
            val shownGroups = groupLimits.limit(
                limitKey, buildLevelGroups(state, level, rows), DATABASE_GROUP_LIMIT
            ) { it.key }
            shownGroups.items.forEach { group ->
                val keys = path + group.key
                val key = keys.joinToString(SUB_GROUP_KEY_SEPARATOR)
                val isCollapsed = key in level.collapsed
                GroupHeaderRow(
                    group = group,
                    collapsed = isCollapsed,
                    width = tableWidth,
                    indent = SUB_GROUP_INDENT * depth,
                    onToggle = { onCollapseGroup(level.column.id, key, !isCollapsed) }
                )
                if (!isCollapsed) {
                    if (depth == groupLevels.lastIndex) {
                        GroupBody(group.rows, RowPlace(keys)) { onAddRowInGroups(keys) }
                    } else {
                        GroupsAt(depth + 1, group.rows, keys)
                    }
                }
                Spacer(modifier = Modifier.size(if (depth == 0) 12.dp else 8.dp))
            }
            if (shownGroups.hidden > 0) {
                LoadMoreButton(shownGroups.hidden, onClick = { groupLimits?.more(limitKey, DATABASE_GROUP_LIMIT) })
            }
        }
        GroupsAt(0, state.rows, emptyList())
    }
    }
}

/**
 * **"Fit to content"**: quanto dev'essere larga ogni colonna in `keys`
 * perché si legga tutto — l'intestazione e ogni cella delle righe
 * mostrate, su una riga sola, coi margini della cella. Come il doppio
 * tocco sul bordo in Excel, ma resta acceso: scrivendo, la colonna segue.
 */
@Composable
private fun fitWidths(
    state: DatabaseTableState,
    columns: List<DatabaseColumnEntity>,
    keys: Set<String>,
    /** Anche il calcolo in fondo alla colonna sta nella colonna: si legge tutto anche lui. */
    calculations: Map<String, Calculation> = emptyMap(),
    subRowKeys: Set<String> = emptySet()
): Map<String, Dp> {
    val measurer = rememberTextMeasurer()
    val typography = MaterialTheme.typography
    val density = LocalDensity.current
    val simple = LocalSimpleDatabase.current
    val icons = LocalRowIcons.current
    val linkedPages = LocalLinkedPages.current
    val related = LocalRelated.current
    val nameLabel = nameColumnLabel()
    val nameIconHidden = LocalPropertyIcons.current?.nameHidden == true
    return remember(state, columns, keys, calculations, subRowKeys, typography, density, simple, icons, linkedPages, related, nameLabel, nameIconHidden) {
        if (keys.isEmpty()) return@remember emptyMap()
        val measure: (String, ComposeTextStyle) -> Dp = { text, style ->
            if (text.isEmpty()) {
                0.dp
            } else {
                with(density) {
                    measurer.measure(AnnotatedString(text), style, maxLines = 1, softWrap = false).size.width.toDp()
                }
            }
        }
        // "SUM  309": la parola piccola, 6 punti, il risultato, e i margini della casella.
        fun calculationWidth(calc: Calculation, value: String): Dp =
            12.dp + measure(DbStrings.calcShortName(calc).uppercase(AppSettings.language.locale), typography.labelSmall) +
                6.dp + measure(value, typography.bodyMedium) + 12.dp
        buildMap {
            if (NAME_COLUMN_KEY in keys) {
                val calc = validCalculation(calculations, NAME_CALCULATION_KEY, null)?.let {
                    fitClamp(calculationWidth(it, nameCalculationText(it, rowsForCalculation(state.rows, NAME_CALCULATION_KEY in subRowKeys))))
                } ?: 0.dp
                // Le sotto-righe rientrano, e con l'albero ogni nome ha il posto della freccia.
                val tree = rowTree(state.rows)
                val depths = tree.associate { it.row.id to it.depth }
                val slot = if (hasSubRows(state.rows)) SUB_ROW_ARROW_SLOT + 4.dp else 0.dp
                val names = nameFitWidth(tree.map { it.row }, simple, icons.keys, typography, measure, nameLabel, nameIconHidden) { row ->
                    SUB_ROW_INDENT * (depths[row.id] ?: 0) + slot
                }
                put(NAME_COLUMN_KEY, maxOf(names, calc))
            }
            columns.filter { it.id in keys }.forEach { column ->
                // L'icona e il suo spazio, se non è nascosta ("Hide icon").
                val icon = if (column.hideIcon) 0.dp else 16.dp + 8.dp
                val header = 12.dp + icon + measure(DbStrings.columnName(column.name), typography.labelLarge) + 12.dp
                val cells = state.rows.maxOfOrNull { row ->
                    cellFitWidth(
                        column, row, state.cellValues[row.id to column.id].orEmpty(), typography, measure,
                        pageTitle = { linkedPages[it]?.title },
                        related = related,
                        linkLabel = state.linkLabels[row.id to column.id]?.text
                    ) + 24.dp
                } ?: 0.dp
                val calc = validCalculation(calculations, column.id, column.type)?.let {
                    calculationWidth(it, columnCalculationText(it, column, rowsForCalculation(state.rows, column.id in subRowKeys), state))
                } ?: 0.dp
                put(column.id, fitClamp(maxOf(header, cells, calc)))
            }
        }
    }
}

/** Un po' di margine per gli arrotondamenti e il cursore, e dentro i limiti di sempre. */
fun fitClamp(width: Dp): Dp = (width + 4.dp).coerceIn(MIN_COLUMN_WIDTH_DP.dp, MAX_COLUMN_WIDTH_DP.dp)

/** La colonna Name: "Aa Name" in cima; sotto icona, nome e OPEN (vedi `NameCell`). */
fun nameFitWidth(
    rows: List<DatabaseRowEntity>,
    simple: Boolean,
    withIcon: Set<String>,
    typography: Typography,
    measure: (String, ComposeTextStyle) -> Dp,
    /** Il nome della colonna: "Name", o quello dato dall'utente. */
    nameLabel: String = DbStrings.name,
    /** Il suo "Aa" nascosto ("Hide icon", dal 27/10/2026). */
    iconHidden: Boolean = false,
    /** Quanto rientra il nome di una riga: le sotto-righe, e il posto della freccia. */
    extraStart: (DatabaseRowEntity) -> Dp = { 0.dp }
): Dp {
    val icon = if (iconHidden) 0.dp else measure("Aa", typography.labelLarge) + 8.dp
    val header = 12.dp + icon + measure(nameLabel, typography.labelLarge) + 12.dp
    val open = if (simple) 0.dp else measure(DbStrings.open, typography.labelSmall) + 16.dp + 8.dp
    val cells = rows.maxOfOrNull { row ->
        12.dp + extraStart(row) + (if (row.id in withIcon) ROW_ICON_SIZE + 6.dp else 0.dp) +
            measure(row.title, typography.bodyMedium) + 12.dp + open
    } ?: 0.dp
    return fitClamp(maxOf(header, cells))
}

/** Quanto occupa il contenuto di una cella, senza i margini: come lo disegna `CellContent`. */
fun cellFitWidth(
    column: DatabaseColumnEntity,
    row: DatabaseRowEntity,
    value: String,
    typography: Typography,
    measure: (String, ComposeTextStyle) -> Dp,
    /** I database collegati dalle Relation (vedi `LocalRelated`). */
    related: Map<String, DatabaseSnapshot> = emptyMap(),
    /**
     * Di una cella URL, la scritta che mostra al posto del link ("Show as…",
     * vedi `DatabaseCellEntity.linkLabel`): si misura lei, che è quello che si
     * vede (corretto il 27/10/2026: "Fit to content" faceva la colonna larga
     * quanto il link intero).
     */
    linkLabel: String? = null,
    /** Il titolo della pagina di una cella "Page link", o null se non c'è più. */
    pageTitle: (String) -> String? = { null }
): Dp = when (column.type) {
    ColumnType.URL -> measure(linkLabel?.takeIf { value.isNotBlank() } ?: value, typography.bodyMedium) + 2.dp
    ColumnType.CHECKBOX -> 24.dp
    // Ogni riga collegata: la freccia, 3 punti, il nome, e 8 fra l'una e l'altra (`RelatedRowLink`).
    ColumnType.RELATION -> relationTitles(value, column.relationConfig()?.let { related[it.database] })
        .fold(0.dp) { sum, title -> sum + 14.dp + 3.dp + measure(title.ifBlank { Strings.untitled }, typography.bodyMedium) + 8.dp }
    // Il quadrato dell'immagine, senza i 12 punti per lato delle altre celle.
    ColumnType.IMAGE -> IMAGE_CELL_SIZE - 24.dp
    // Il bottone (dal 27/10/2026): la scritta, coi margini della pastiglia e l'icona se c'è.
    ColumnType.BUTTON -> ButtonConfig.parse(column.optionsJson).let { config ->
        measure(config.name.ifBlank { DbStrings.columnName(column.name) }, typography.bodyMedium) + 16.dp +
            (if (config.hasIcon) 22.dp else 0.dp)
    }
    // L'icona, 6 punti, il titolo (o "Page not found").
    ColumnType.PAGE_LINK -> if (value.isEmpty()) {
        0.dp
    } else {
        pageTitle(value)?.let { 16.dp + 6.dp + measure(it.ifBlank { Strings.untitled }, typography.bodyMedium) }
            ?: measure(DbStrings.pageNotFound, typography.bodyMedium)
    }
    ColumnType.SELECT, ColumnType.MULTI_SELECT -> {
        val labels = if (column.type == ColumnType.MULTI_SELECT) {
            value.split(MULTI_VALUE_SEPARATOR).filter { it.isNotBlank() }
        } else {
            listOfNotNull(value.takeIf { it.isNotBlank() })
        }
        // Ogni tag: il nome, 8 punti per lato e 4 fra un tag e l'altro (`TagChip`).
        labels.fold(0.dp) { sum, label -> sum + measure(label, typography.labelMedium) + 16.dp + 4.dp }
    }
    ColumnType.DATE -> {
        val range = parseDateRange(value)
        measure(range?.label() ?: "\u2014", typography.bodyMedium) +
            (if (range?.recurrence != null) 18.dp else 0.dp) +
            (if (range?.notify == true) 18.dp else 0.dp)
    }
    ColumnType.COLOR -> {
        val cell = parseColorCell(value)
        measure(cell.text.ifEmpty { if (hexToColor(cell.color) == null) "\u2014" else "" }, typography.bodyMedium)
    }
    ColumnType.DURATION -> measure(durationLabel(value, durationFormatOf(column.durationFormat)) ?: "\u2014", typography.bodyMedium)
    ColumnType.CREATED_TIME, ColumnType.LAST_EDITED_TIME -> {
        val instant = if (column.type == ColumnType.CREATED_TIME) row.createdAt else row.updatedAt ?: row.createdAt
        measure(Instant.ofEpochMilli(instant).atZone(AppSettings.zoneId).format(DATE_TIME_FORMAT), typography.bodyMedium)
    }
    ColumnType.NUMBER -> if (value.isBlank()) {
        0.dp
    } else {
        val settings = column.numberSettings()
        val text = measure(formatNumberValue(value, settings), typography.bodyMedium)
        val number = if (settings.showNumber) text + 8.dp else 0.dp
        when (settings.display) {
            NumberDisplay.NUMBER -> text
            // La barra si stende quanto c'è posto: le si lascia una misura che si veda.
            NumberDisplay.BAR -> number + 64.dp
            NumberDisplay.RING -> number + 20.dp
        }
    }
    // Testo, link, email, telefono: tutto su una riga, più il cursore.
    else -> measure(value, typography.bodyMedium) + 2.dp
}

/**
 * **Il segnalino** fra la fine della colonna e l'inizio di quella a
 * destra, come il bordo delle colonne in Excel: trascinato verso destra la
 * allarga, verso sinistra la stringe. `center` è dove sta il bordo, dal
 * lato sinistro della tabella.
 */
@Composable
private fun ColumnResizeHandle(
    resize: ColumnResize,
    key: String,
    center: Dp,
    current: Dp,
    /** La larghezza di partenza della colonna, a cui ci si aggancia (vedi `snapColumnWidth`). */
    defaultWidth: Int,
    /** Più stretta di così non va (vedi `minColumnWidth`). */
    minWidth: Int = MIN_COLUMN_WIDTH_DP,
    onCommit: (Int) -> Unit
) {
    val density = LocalDensity.current
    val latestCurrent by rememberUpdatedState(current)
    val commit = { resize.draft?.let { onCommit(it.value.roundToInt()) } }
    Box(
        modifier = Modifier
            .offset(x = center - RESIZE_HANDLE_WIDTH / 2, y = (ROW_HEIGHT - RESIZE_HANDLE_HEIGHT) / 2)
            .size(RESIZE_HANDLE_WIDTH, RESIZE_HANDLE_HEIGHT)
            // Il tocco è sul segnalino: la tabella non lo deve chiudere.
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    resize.handleTouched = true
                }
            }
            .pointerInput(key) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        resize.raw = latestCurrent
                        resize.draft = latestCurrent
                    },
                    onDragEnd = { commit() },
                    onDragCancel = { commit() },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        resize.raw += with(density) { amount.toDp() }
                        // Vicino alla larghezza di sempre si aggancia lì, e lo si vede subito.
                        resize.draft = snapColumnWidth(key, resize.raw.value.roundToInt(), defaultWidth, minWidth).dp
                    }
                )
            }
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(RESIZE_HANDLE_WIDTH / 2)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.UnfoldMore,
            contentDescription = DbStrings.resizeColumn,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .size(18.dp)
                .rotate(90f)
        )
    }
}

/**
 * **Dove sta una riga nella tabella**: in nessun gruppo, in un gruppo o in
 * un suo sottogruppo. Serve a "Move up" / "Move down" per sapere quali sono
 * le righe accanto — con la selezione multipla una riga sta in più gruppi,
 * e conta quello in cui la si è toccata.
 */
private data class RowPlace(val path: List<String> = emptyList())

/**
 * Le righe del gruppo in [path] (le chiavi dei gruppi, dal primo livello),
 * come si vedono adesso: tutte, senza gruppi; nessuna, se il gruppo non c'è più.
 */
fun rowsAt(state: DatabaseTableState, levels: List<GroupLevel>, path: List<String>): List<DatabaseRowEntity> {
    var rows = state.rows
    path.forEachIndexed { depth, key ->
        val level = levels.getOrNull(depth) ?: return rows
        rows = buildLevelGroups(state, level, rows).find { it.key == key }?.rows.orEmpty()
    }
    return rows
}

/** Fra la chiave del gruppo e quella del sottogruppo, nell'elenco di quelli chiusi. */
private const val SUB_GROUP_KEY_SEPARATOR = "\u0000"

/** Quanto rientrano le intestazioni dei sottogruppi. */
private val SUB_GROUP_INDENT = 20.dp

/**
 * L'intestazione delle colonne, ripetuta sopra ogni gruppo come su Notion.
 * `columns` sono quelle che si vedono, e con Name al suo posto (`namePlace`)
 * sono la tabella da sinistra a destra: la prima non va a sinistra,
 * l'ultima non va a destra.
 */
@Composable
private fun TableHeaderRow(
    columns: List<DatabaseColumnEntity>,
    viewLocked: Boolean,
    onMoveProperty: (DatabaseColumnEntity, Int) -> Unit,
    /** Dove sta Name fra `columns`: 0 = prima di tutte, `columns.size` = in fondo. */
    namePlace: Int = 0,
    onMoveName: (Int) -> Unit = {},
    onRenameName: ((String) -> Unit)? = null,
    onHideProperty: (DatabaseColumnEntity) -> Unit,
    onToggleCenterProperty: (DatabaseColumnEntity, CellAlignment) -> Unit,
    onAddProperty: () -> Unit,
    onCalculate: (CalculateTarget) -> Unit = {},
    onSetAllChecked: (DatabaseColumnEntity, Boolean) -> Unit = { _, _ -> },
    fitColumns: Set<String> = emptySet(),
    onSetColumnWidth: (String, Int) -> Unit = { _, _ -> },
    onSetFitToContent: (String, Boolean) -> Unit = { _, _ -> },
    /** I tre puntini accanto al "+": vedi `AddPropertyCell`. */
    onShowProperties: (() -> Unit)? = null
) {
    val resize = LocalColumnResize.current
    val widths = LocalTableWidths.current
    val nameLabel = nameColumnLabel()
    // Il posto di ogni colonna nella tabella, Name compresa.
    fun place(index: Int) = if (index < namePlace) index else index + 1
    // **Con "Wrap in the cell" vanno a capo anche i titoli delle colonne**
    // (dal 30/09/2026, come nella tabella di Notion mostrata dall'utente): la
    // riga dei titoli si alza quanto il più lungo.
    val wrap = LocalWrapCells.current
    Box {
    Row(modifier = if (wrap) Modifier.height(IntrinsicSize.Min) else Modifier) {
        TableSlots(
            columns = columns,
            namePlace = namePlace,
            name = {
                NameHeaderCell(
                    onCalculate = { onCalculate(CalculateTarget(NAME_CALCULATION_KEY, null, nameLabel)) },
                    onRename = onRenameName,
                    canMoveLeft = !viewLocked && namePlace > 0,
                    canMoveRight = !viewLocked && namePlace < columns.size,
                    onMove = onMoveName,
                    canResize = !viewLocked,
                    fit = NAME_COLUMN_KEY in fitColumns,
                    onResize = { resize?.start(NAME_COLUMN_KEY) },
                    onFitChange = { onSetFitToContent(NAME_COLUMN_KEY, it) }
                )
                CellSeparator(NAME_COLUMN_KEY, stretch = wrap)
            }
        ) { index, column ->
            PropertyHeaderCell(
                column = column,
                canMoveLeft = !viewLocked && place(index) > 0,
                canMoveRight = !viewLocked && place(index) < columns.size,
                onMove = { delta -> onMoveProperty(column, delta) },
                onHide = { onHideProperty(column) },
                onAlign = { alignment -> onToggleCenterProperty(column, alignment) },
                onCalculate = { onCalculate(CalculateTarget(column.id, column.type, DbStrings.columnName(column.name))) },
                onSetAllChecked = { checked -> onSetAllChecked(column, checked) },
                canResize = !viewLocked,
                fit = column.id in fitColumns,
                onResize = { resize?.start(column.id) },
                onFitChange = { onSetFitToContent(column.id, it) }
            )
            CellSeparator(column.id, stretch = wrap)
        }
        AddPropertyCell(onClick = onAddProperty, onShowProperties = onShowProperties)
    }
    // Il segnalino, sopra il bordo destro della colonna che si allarga:
    // dopo tutte quelle alla sua sinistra, Name compresa, e lei.
    val key = resize?.key
    if (resize != null && key != null) {
        val keys = columns.map { it.id }.toMutableList().apply { add(namePlace, NAME_COLUMN_KEY) }
        val index = keys.indexOf(key)
        // Con la prima colonna ferma, il suo bordo è spostato di quanto la tabella è scorsa.
        val frozen = LocalFrozenColumn.current
        val frozenShift = if (frozen != null && index == 0) with(LocalDensity.current) { frozen.scroll.value.toDp() } else 0.dp
        val edge = if (index < 0) {
            null
        } else {
            keys.take(index + 1).fold(0.dp) { sum, k -> sum + widths.of(k) } + SEPARATOR_WIDTH * index + frozenShift
        }
        if (edge != null) {
            ColumnResizeHandle(
                resize = resize,
                key = key,
                center = edge + SEPARATOR_WIDTH / 2,
                current = widths.of(key),
                defaultWidth = defaultColumnWidth(key, columns.find { it.id == key }?.type),
                minWidth = minColumnWidth(key, columns.find { it.id == key }?.type),
                onCommit = { onSetColumnWidth(key, it) }
            )
        }
    }
    }
}

/**
 * **La riga dei calcoli in fondo alla tabella** (dal 26/09/2026,
 * "Calculate"): sotto ogni colonna col suo calcolo, la parola corta e il
 * risultato ("SUM 309"), sulle righe mostrate (filtrate, cercate, del
 * gruppo). Non c'è finché nessuna colonna mostrata ne ha uno: "None" su
 * tutte la toglie. Toccando un calcolo si cambia.
 */
@Composable
private fun CalculationFooter(
    rows: List<DatabaseRowEntity>,
    state: DatabaseTableState,
    columns: List<DatabaseColumnEntity>,
    calculations: Map<String, Calculation>,
    onCalculate: (CalculateTarget) -> Unit,
    alignments: Map<String, CellAlignment> = emptyMap(),
    onAlign: (String, CellAlignment) -> Unit = { _, _ -> },
    /** I calcoli che contano anche le sotto-righe; gli altri solo le righe principali. */
    subRowKeys: Set<String> = emptySet(),
    /** Dove sta Name fra `columns` (vedi `TableHeaderRow`). */
    namePlace: Int = 0
) {
    val nameCalc = validCalculation(calculations, NAME_CALCULATION_KEY, null)
    val columnCalcs = columns.associate { it.id to validCalculation(calculations, it.id, it.type) }
    if (nameCalc == null && columnCalcs.values.all { it == null }) return
    val widths = LocalTableWidths.current
    val nameLabel = nameColumnLabel()

    Row {
        TableSlots(
            columns = columns,
            namePlace = namePlace,
            name = {
                CalculationCell(
                    width = widths.name,
                    alignment = alignments[NAME_CALCULATION_KEY] ?: CellAlignment.RIGHT,
                    onClick = { onCalculate(CalculateTarget(NAME_CALCULATION_KEY, null, nameLabel)) },
                    onAlign = { onAlign(NAME_CALCULATION_KEY, it) }
                ) {
                    if (nameCalc != null) {
                        CalculationLabel(nameCalc, nameCalculationText(nameCalc, rowsForCalculation(rows, NAME_CALCULATION_KEY in subRowKeys)))
                    }
                }
                Spacer(modifier = Modifier.width(SEPARATOR_WIDTH))
            }
        ) { _, column ->
            val calc = columnCalcs[column.id]
            CalculationCell(
                width = widths.of(column),
                alignment = alignments[column.id] ?: CellAlignment.RIGHT,
                onClick = { onCalculate(CalculateTarget(column.id, column.type, DbStrings.columnName(column.name))) },
                onAlign = { onAlign(column.id, it) }
            ) {
                if (calc != null) {
                    CalculationLabel(calc, columnCalculationText(calc, column, rowsForCalculation(rows, column.id in subRowKeys), state))
                }
            }
            Spacer(modifier = Modifier.width(SEPARATOR_WIDTH))
        }
        Box(modifier = Modifier.width(ADD_PROPERTY_WIDTH).height(ROW_HEIGHT))
    }
}

/** Il calcolo di una colonna, se il suo tipo ce l'ha ancora: cambiato dopo, non si mostra. */
private fun validCalculation(calculations: Map<String, Calculation>, key: String, type: ColumnType?): Calculation? =
    calculations[key]?.takeIf { calc -> calculationsFor(type).values.any { calc in it } }

/** Il risultato del calcolo di Name sulle righe date, come si legge in fondo alla tabella. */
private fun nameCalculationText(calc: Calculation, rows: List<DatabaseRowEntity>): String =
    calculationText(calculate(calc, null, rows.map { it.title }), null)

/** Il risultato del calcolo di una colonna sulle righe date, come si legge in fondo alla tabella. */
private fun columnCalculationText(
    calc: Calculation,
    column: DatabaseColumnEntity,
    rows: List<DatabaseRowEntity>,
    state: DatabaseTableState
): String {
    val values = rows.map { row ->
        when (column.type) {
            ColumnType.CREATED_TIME -> row.createdAt.toString()
            ColumnType.LAST_EDITED_TIME -> (row.updatedAt ?: row.createdAt).toString()
            ColumnType.COLOR -> parseColorCell(state.cellValues[row.id to column.id].orEmpty())
                .let { if (it.isEmpty) "" else state.cellValues[row.id to column.id].orEmpty() }
            // Di una formula il numero o l'istante, non come si legge ("1.234,5").
            ColumnType.FORMULA -> formulaCalculationValue(state.formulas[row.id to column.id])
            else -> state.cellValues[row.id to column.id].orEmpty()
        }
    }
    val result = calculate(calc, column.type, values, startOfDate = { parseDateRange(it)?.start })
    return calculationText(result, column)
}

/** Il valore di una formula per i calcoli in fondo: un numero si somma, una data si confronta, un testo si conta. */
private fun formulaCalculationValue(value: FormulaValue?): String = when (value) {
    is FormulaValue.Num -> FormulaEngine.clean(value.value).takeIf { it.isFinite() }
        ?.let { java.math.BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() }.orEmpty()
    is FormulaValue.Date -> value.millis.toString()
    is FormulaValue.Bool -> if (value.value) "true" else ""
    is FormulaValue.Text -> value.value
    else -> ""
}

/**
 * Una casella della riga dei calcoli: toccandola si sceglie il calcolo,
 * **tenendola premuta** (dal 26/09/2026) dove sta il risultato — a
 * sinistra, al centro o a destra (di suo a destra, come su Notion).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalculationCell(
    width: Dp,
    alignment: CellAlignment,
    onClick: () -> Unit,
    onAlign: (CellAlignment) -> Unit,
    content: @Composable () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .width(width)
                .height(ROW_HEIGHT)
                .onRightClick({ menuOpen = true })
                .combinedClickable(onClick = onClick, onLongClick = { menuOpen = true })
                .padding(horizontal = 12.dp),
            contentAlignment = alignment.box
        ) {
            content()
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            modifier = Modifier.background(DarkSheet)
        ) {
            listOf(
                Triple(CellAlignment.LEFT, DbStrings.alignLeft, Icons.Filled.FormatAlignLeft),
                Triple(CellAlignment.CENTER, DbStrings.center, Icons.Filled.FormatAlignCenter),
                Triple(CellAlignment.RIGHT, DbStrings.alignRight, Icons.Filled.FormatAlignRight)
            ).forEach { (option, label, icon) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    leadingIcon = { Icon(icon, contentDescription = null) },
                    trailingIcon = if (option == alignment) {
                        { Icon(Icons.Filled.Check, contentDescription = DbStrings.selected) }
                    } else {
                        null
                    },
                    onClick = {
                        menuOpen = false
                        onAlign(option)
                    }
                )
            }
        }
    }
}

/** "SUM  309": la parola piccola, grigia e maiuscola, poi il risultato. */
@Composable
private fun CalculationLabel(calc: Calculation, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            DbStrings.calcShortName(calc).uppercase(AppSettings.language.locale),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
    }
}

/**
 * **Name al suo posto fra le colonne** (dal 29/09/2026 si sposta): per
 * intestazione, righe e calcoli, da sinistra a destra, `name` quando tocca
 * a Name e `column` per ogni colonna (con la sua posizione in `columns`).
 * Ognuna col suo `key`: spostandosi, una cella si porta dietro quello che
 * ha dentro.
 */
@Composable
private fun TableSlots(
    columns: List<DatabaseColumnEntity>,
    namePlace: Int,
    /** La riga, per l'evidenziazione della ricerca sulla prima cella ferma. */
    rowId: String? = null,
    name: @Composable () -> Unit,
    column: @Composable (Int, DatabaseColumnEntity) -> Unit
) {
    // **La prima colonna ferma** (dal 08/10/2026): la prima cella, col suo
    // separatore, in un pezzo solo che resta al bordo sinistro (vedi
    // `frozenFirstCell`). Spenta, le celle sono come prima.
    val frozen = LocalFrozenColumn.current
    val found = if (rowId != null) Modifier.foundInside(rowId) else Modifier
    @Composable
    fun first(content: @Composable () -> Unit) {
        if (frozen == null) content() else Row(modifier = Modifier.frozenFirstCell(frozen, found)) { content() }
    }
    columns.forEachIndexed { index, c ->
        if (index == namePlace) key(NAME_COLUMN_KEY) { if (index == 0) first(name) else name() }
        key(c.id) { if (index == 0 && namePlace > 0) first { column(index, c) } else column(index, c) }
    }
    if (namePlace >= columns.size) key(NAME_COLUMN_KEY) { if (columns.isEmpty()) first(name) else name() }
}

/** "New page" in fondo a un gruppo della tabella. */
@Composable
private fun GroupNewRowButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            if (LocalSimpleDatabase.current) DbStrings.newRow else DbStrings.newPage,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/** Una riga della tabella: nome, celle, e lo spazio sotto la colonna "+". */
@Composable
private fun TableBodyRow(
    row: DatabaseRowEntity,
    /** Quanto è rientrata: le sotto-righe stanno sotto la loro riga, un gradino più in là. */
    depth: Int = 0,
    /** Ha sotto-righe: sul nome c'è la freccia per aprirle e chiuderle. */
    hasSubRows: Boolean = false,
    /** Nella tabella c'è l'albero: ogni nome lascia il posto della freccia. */
    toggleSlot: Boolean = false,
    onToggleSubRows: () -> Unit = {},
    state: DatabaseTableState,
    /** Solo le colonne che si vedono: le stesse dell'intestazione. */
    columns: List<DatabaseColumnEntity>,
    onTitleChange: (DatabaseRowEntity, String) -> Unit,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit,
    onSetCellValue: (DatabaseRowEntity, DatabaseColumnEntity, String) -> Unit,
    onAddOption: (DatabaseColumnEntity, String) -> Unit,
    onRenameOption: (String, String, String) -> Unit,
    onSetOptionColor: (String, String, String) -> Unit,
    onDeleteOption: (String, String) -> Unit,
    onMoveOptions: (String, List<String>) -> Unit,
    onSetOptionSecondColor: (String, String, String?, Boolean) -> Unit,
    onTitleEditing: (Boolean) -> Unit = {},
    /** Dove sta Name fra `columns` (vedi `TableHeaderRow`). */
    namePlace: Int = 0
) {
    // **"Wrap cells"**: la riga è alta quanto la sua cella più alta, e
    // tutte le celle (e le linee fra loro) si allungano fin lì.
    val wrap = LocalWrapCells.current
    Row(modifier = Modifier.foundInside(row.id).then(if (wrap) Modifier.height(IntrinsicSize.Min) else Modifier)) {
        TableSlots(
            columns = columns,
            namePlace = namePlace,
            rowId = row.id,
            name = {
                NameCell(
                    row = row,
                    onTitleChange = { onTitleChange(row, it) },
                    onOpen = { onOpenRow(row) },
                    onLongPress = { onRowLongPress(row) },
                    onEditingChange = onTitleEditing,
                    depth = depth,
                    hasSubRows = hasSubRows,
                    toggleSlot = toggleSlot,
                    onToggleSubRows = onToggleSubRows
                )
                CellSeparator(NAME_COLUMN_KEY, stretch = wrap)
            }
        ) { _, column ->
            CellEditor(
                row = row,
                column = column,
                value = state.cellValues[row.id to column.id] ?: "",
                onValueChange = { onSetCellValue(row, column, it) },
                onAddOption = { onAddOption(column, it) },
                onRenameOption = onRenameOption,
                onSetOptionColor = onSetOptionColor,
                onDeleteOption = onDeleteOption,
                onMoveOptions = onMoveOptions,
                onSetOptionSecondColor = onSetOptionSecondColor
            )
            CellSeparator(column.id, stretch = wrap)
        }
        Box(modifier = Modifier.width(ADD_PROPERTY_WIDTH).height(ROW_HEIGHT))
    }
}

/**
 * L'intestazione di un gruppo: la freccia per chiuderlo, il valore
 * (come tag se la proprietà è a tag) e quante pagine contiene.
 */
@Composable
private fun GroupHeaderRow(
    group: RowGroup,
    collapsed: Boolean,
    width: Dp,
    /** Per i sottogruppi, un po' più a destra del loro gruppo. */
    indent: Dp = 0.dp,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(width)
            .clickable { onToggle() }
            .padding(start = indent, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (collapsed) {
                Icons.Filled.KeyboardArrowRight
            } else {
                Icons.Filled.KeyboardArrowDown
            },
            contentDescription = if (collapsed) DbStrings.expandGroup else DbStrings.collapseGroup,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.size(4.dp))
        if (group.color != null) {
            TagChip(label = group.label, color = group.color, secondColor = group.secondColor, colorLine = group.colorLine)
        } else {
            Text(
                text = group.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (group.key.isEmpty()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onBackground
                }
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = group.rows.size.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * La visualizzazione a bacheca: una colonna per ogni valore della
 * proprietà scelta, con le righe come schede dentro la colonna del loro
 * valore.
 *
 * Le righe senza valore non spariscono: finiscono in una colonna
 * dedicata in fondo a destra, dopo quelle con un tag. Davanti
 * ruberebbero il primo sguardo a quelle già smistate, che sono il
 * contenuto vero della bacheca.
 *
 * **Le colonne vengono da [buildRowGroups]** (dal 14/10/2026), come i gruppi
 * della tabella: si raggruppa anche per casella, data, numero o testo, e una
 * riga con un tag cancellato ha comunque la sua colonna invece di sparire.
 *
 * **Con i sottogruppi** (dal 14/10/2026, chiesto dall'utente) la bacheca si
 * divide in fasce orizzontali, come su Notion: una per valore della seconda
 * proprietà, ognuna con tutte le colonne. I nomi delle colonne stanno una
 * volta sola, in cima; quello della fascia resta a sinistra mentre le colonne
 * scorrono, e toccandolo la fascia si chiude.
 */
@Composable
fun BoardLayout(
    state: DatabaseTableState,
    /**
     * **I livelli** (dal 28/10/2026, quanti se ne vogliono): il primo fa le
     * colonne, gli altri le fasce, una dentro l'altra. Nessuno = non c'è una
     * proprietà per le colonne. Con "Hide empty groups" (dal 21/10/2026) del
     * primo, niente colonne senza schede, nemmeno quella senza valore.
     */
    levels: List<GroupLevel>,
    /** La freccia di una fascia: (proprietà del livello, chiave della fascia, chiusa). */
    onCollapseGroup: (String, String, Boolean) -> Unit,
    /** "Color columns": ogni colonna, con le sue schede, del colore del suo tag. */
    colorCards: Boolean = false,
    /** "Hide New" (dal 26/10/2026): niente "+ New" sotto le schede. */
    hideNew: Boolean = false,
    newRowTick: Int,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit,
    /** "New" in una colonna (e nelle sue fasce): la riga nasce coi valori della colonna e delle fasce. */
    onAddRowInGroups: (List<String>) -> Unit,
    onAddGroupProperty: () -> Unit
) {
    val columnsLevel = levels.firstOrNull()
    val groupColumn = columnsLevel?.column
    if (columnsLevel == null || groupColumn == null) {
        // Senza una proprietà a selezione singola non ci sono colonne
        // da costruire. Meglio dirlo che mostrare una schermata vuota
        // lasciando credere che sia rotta.
        MissingPropertyNotice(
            message = DbStrings.boardNeedsSelect,
            actionLabel = DbStrings.addSelectProperty,
            onAction = onAddGroupProperty
        )
        return
    }

    // Tutte le colonne dei tag, anche vuote, e sempre quella senza valore;
    // con "Hide empty groups" solo quelle con almeno una scheda.
    val hideEmptyGroups = columnsLevel.hideEmpty
    val allGroups = buildLevelGroups(state, columnsLevel, keepEmptyNone = !hideEmptyGroups)
    // Le prime colonne e "Load more" (dal 26/10/2026): vedi `DATABASE_GROUP_LIMIT`.
    // Quella senza valore resta sempre, in fondo: è dove nasce una scheda nuova.
    val limits = LocalRowLimits.current
    val columnsKey = "board-columns:" + groupColumn.id
    val shownColumns = limits.limit(columnsKey, allGroups.filter { it.key.isNotEmpty() }, DATABASE_GROUP_LIMIT) { it.key }
    val groups = shownColumns.items + allGroups.filter { it.key.isEmpty() }
    @Composable
    fun moreColumns() {
        if (shownColumns.hidden > 0) {
            BoardColumnBox(null) {
                LoadMoreButton(shownColumns.hidden, onClick = { limits?.more(columnsKey, DATABASE_GROUP_LIMIT) })
            }
        }
    }
    val tagged = groupColumn.type == ColumnType.SELECT || groupColumn.type == ColumnType.MULTI_SELECT
    val onPage = MaterialTheme.colorScheme.onBackground
    // **"Color columns"** (dal 30/09/2026, sul modello di Notion che l'utente
    // ha mostrato): la colonna intera ha un velo del colore del suo tag, le
    // schede uno più carico col bordino, in cima c'è il tag stesso, e il
    // numero e "New" sono del suo colore. La colonna senza valore, e quelle
    // che non sono tag, restano grigie (il colore "Default").
    val tints = groups.associate { group ->
        group.key to if (colorCards) tagColor(group.color ?: TAG_COLORS.first().hex) else null
    }
    fun accentOf(tint: Color?) = tint?.let { lerp(it, onPage, BOARD_ACCENT_LIGHT) }
    // Le proprietà che fanno da colonna e da fascia non si ripetono sulle
    // schede: sono già scritte in cima.
    val hidden = levels.mapTo(HashSet()) { it.column.id }
    val bandLevels = levels.drop(1)

    val boardScroll = rememberScrollState()

    // Una pagina creata dal pulsante in fondo nasce senza tag, quindi
    // nell'ultima colonna: senza scorrere fin lì sembra non essere stata
    // creata affatto.
    LaunchedEffect(newRowTick) {
        if (newRowTick > 0) boardScroll.animateScrollTo(boardScroll.maxValue)
    }

    if (bandLevels.isEmpty()) {
        Row(
            modifier = Modifier.horizontalScroll(boardScroll),
            verticalAlignment = Alignment.Top
        ) {
            groups.forEach { group ->
                val tint = tints[group.key]
                BoardColumnBox(tint) {
                    BoardColumnHeader(group, tagged, tint, accentOf(tint))
                    BoardColumnCards(
                        rows = group.rows,
                        limitKey = "board:" + group.key,
                        state = state,
                        hiddenColumnIds = hidden,
                        tint = tint,
                        accent = accentOf(tint),
                        onOpenRow = onOpenRow,
                        onRowLongPress = onRowLongPress,
                        onAdd = { onAddRowInGroups(listOf(group.key)) }.takeUnless { hideNew }
                    )
                }
            }
            moreColumns()
        }
        return
    }

    // Quali fasce sono chiuse: come per i gruppi della tabella, nella regola del loro livello.

    /**
     * **Le fasce a ogni livello** (dal 28/10/2026): dentro ogni fascia, quelle
     * del livello dopo, un po' più a destra; all'ultimo, le colonne con le
     * loro schede. Le prime fasce e "Load more" (dal 21/10/2026), come le
     * schede: vedi `BOARD_BAND_LIMIT`.
     */
    @Composable
    fun BandsAt(depth: Int, rows: List<DatabaseRowEntity>, path: List<String>) {
        val level = bandLevels[depth]
        val bandsKey = "board-bands:" + level.column.id + if (depth > 0) ":" + path.joinToString(SUB_GROUP_KEY_SEPARATOR) else ""
        val shownBands = limits.limit(
            bandsKey, buildLevelGroups(state, level, rows), BOARD_BAND_LIMIT
        ) { it.key }
        shownBands.items.forEach { band ->
            val keys = path + band.key
            val key = keys.joinToString(SUB_GROUP_KEY_SEPARATOR)
            val isCollapsed = key in level.collapsed
            // Spostato quanto le colonne sono scorse, così il nome resta
            // sempre sul bordo sinistro dello schermo (come la prima
            // colonna ferma della tabella, `frozenFirstCell`).
            Box(modifier = Modifier.offset { IntOffset(boardScroll.value, 0) }) {
                GroupHeaderRow(
                    group = band,
                    collapsed = isCollapsed,
                    width = BOARD_COLUMN_WIDTH,
                    indent = 6.dp + SUB_GROUP_INDENT * depth,
                    onToggle = { onCollapseGroup(level.column.id, key, !isCollapsed) }
                )
            }
            if (!isCollapsed) {
                if (depth == bandLevels.lastIndex) {
                    val inBand = band.rows.mapTo(HashSet()) { it.id }
                    Row(verticalAlignment = Alignment.Top) {
                        groups.forEach { group ->
                            val tint = tints[group.key]
                            val cards = group.rows.filter { it.id in inBand }
                            // Con "Hide New" una colonna senza schede nella fascia
                            // non lascia una striscia colorata vuota: resta solo
                            // il suo posto, così le altre restano in fila.
                            val boxTint = tint.takeUnless { hideNew && cards.isEmpty() }
                            BoardColumnBox(boxTint, top = if (boxTint != null) 8.dp else 0.dp) {
                                BoardColumnCards(
                                    rows = cards,
                                    limitKey = "board:" + key + SUB_GROUP_KEY_SEPARATOR + group.key,
                                    state = state,
                                    hiddenColumnIds = hidden,
                                    tint = tint,
                                    accent = accentOf(tint),
                                    onOpenRow = onOpenRow,
                                    onRowLongPress = onRowLongPress,
                                    onAdd = { onAddRowInGroups(listOf(group.key) + keys) }.takeUnless { hideNew }
                                )
                            }
                        }
                    }
                } else {
                    BandsAt(depth + 1, band.rows, keys)
                }
            }
            Spacer(modifier = Modifier.size(if (depth == 0) 12.dp else 8.dp))
        }
        if (shownBands.hidden > 0) {
            // Fermo sul bordo sinistro come i nomi delle fasce.
            Box(modifier = Modifier.offset { IntOffset(boardScroll.value, 0) }) {
                LoadMoreButton(shownBands.hidden, onClick = { limits?.more(bandsKey, BOARD_BAND_LIMIT) })
            }
        }
    }

    Column(modifier = Modifier.horizontalScroll(boardScroll)) {
        Row(verticalAlignment = Alignment.Top) {
            groups.forEach { group ->
                val tint = tints[group.key]
                BoardColumnBox(tint) {
                    BoardColumnHeader(group, tagged, tint, accentOf(tint))
                }
            }
            moreColumns()
        }
        BandsAt(0, state.rows, emptyList())
    }
}

/** Il riquadro di una colonna della bacheca, col velo del suo colore se c'è. */
@Composable
private fun BoardColumnBox(
    tint: Color?,
    /** Lo spazio in cima quando non c'è l'intestazione a farlo (le colonne nelle fasce). */
    top: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val pageBackground = MaterialTheme.colorScheme.background
    Column(
        modifier = Modifier
            .width(BOARD_COLUMN_WIDTH)
            .padding(horizontal = 6.dp)
            .then(
                if (tint != null) {
                    Modifier
                        .background(lerp(pageBackground, tint, BOARD_COLUMN_TINT), RoundedCornerShape(10.dp))
                        .padding(start = 8.dp, end = 8.dp, top = top, bottom = 8.dp)
                } else {
                    Modifier
                }
            ),
        content = content
    )
}

/** In cima a una colonna della bacheca: il suo valore (il tag, con "Color columns") e quante schede ha. */
@Composable
private fun BoardColumnHeader(group: RowGroup, tagged: Boolean, tint: Color?, accent: Color?) {
    val onPage = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (tint != null && tagged && group.key.isNotEmpty()) {
            TagChip(label = group.label, color = group.color, secondColor = group.secondColor, colorLine = group.colorLine)
        } else {
            Text(
                text = group.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (tint != null) onPage else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = group.rows.size.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = accent ?: MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Le schede di una colonna della bacheca (o della sua parte in una fascia) e
 * "New" sotto; [onAdd] null, con "Hide New", senza.
 */
@Composable
private fun BoardColumnCards(
    rows: List<DatabaseRowEntity>,
    /** Quale colonna (e fascia): le sue prime schede e "Load more", vedi `RowLimits`. */
    limitKey: String,
    state: DatabaseTableState,
    hiddenColumnIds: Set<String>,
    tint: Color?,
    accent: Color?,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit,
    onAdd: (() -> Unit)?
) {
    val pageBackground = MaterialTheme.colorScheme.background
    // **Le prime schede e "Load more"**, anche a pagina intera (dal
    // 18/10/2026, come su Notion): la bacheca non è una lista che disegna
    // solo quello che si vede. Vedi `RowLimits`.
    val limits = LocalRowLimits.current
    val shown = limits.limit(limitKey, rows) { it.id }
    shown.items.forEach { row ->
        BoardCard(
            row = row,
            state = state,
            hiddenColumnIds = hiddenColumnIds,
            tint = tint,
            onOpen = { onOpenRow(row) },
            onLongPress = { onRowLongPress(row) }
        )
        Spacer(modifier = Modifier.size(8.dp))
    }
    if (shown.hidden > 0) LoadMoreButton(shown.hidden, onClick = { limits?.more(limitKey) })

    if (onAdd == null) {
        // "Hide New": niente. Una colonna senza schede resta larga com'è
        // (`BoardColumnBox`), così quelle accanto restano in fila.
    } else if (tint != null && accent != null) {
        // Come su Notion: "New" largo quanto la colonna, col
        // bordo e la scritta del colore del tag.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, lerp(pageBackground, tint, BOARD_BORDER_TINT), RoundedCornerShape(8.dp))
                .clickable { onAdd() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text(DbStrings.newShort, style = MaterialTheme.typography.labelMedium, color = accent)
        }
    } else {
        TextButton(onClick = onAdd) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(DbStrings.newShort, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/**
 * Il messaggio di una vista che non può disegnarsi perché manca la
 * proprietà su cui si regge, **con il pulsante per crearla**.
 *
 * Il pulsante non è un di più: in queste viste non esiste un "+ Add
 * property" (sta nell'intestazione della tabella), quindi un messaggio
 * che si limitasse a dire cosa manca manderebbe a cercare qualcosa che
 * da lì non si vede.
 */
@Composable
private fun MissingPropertyNotice(
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.size(8.dp))
        TextButton(onClick = onAction, contentPadding = PaddingValues(0.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text(actionLabel)
        }
    }
}

/**
 * Una scheda della bacheca: il nome della riga e i valori delle altre
 * proprietà. Con **"Color columns"** (dal 30/09/2026) ha un velo del colore
 * del tag della sua colonna, più carico di quello della colonna, e un
 * bordino: le scritte restano quelle di sempre e si leggono.
 */
@Composable
private fun BoardCard(
    row: DatabaseRowEntity,
    state: DatabaseTableState,
    /** Le proprietà che non si ripetono sulla scheda: quella delle colonne, e delle fasce. */
    hiddenColumnIds: Set<String>,
    /** Il colore del tag della colonna, o null per la scheda di sempre. */
    tint: Color? = null,
    onOpen: () -> Unit,
    onLongPress: () -> Unit
) {
    val pageBackground = MaterialTheme.colorScheme.background
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (tint != null) lerp(pageBackground, tint, BOARD_CARD_TINT) else DarkSurface, RoundedCornerShape(8.dp))
            .then(
                if (tint != null) {
                    Modifier.border(1.dp, lerp(pageBackground, tint, BOARD_BORDER_TINT), RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            )
            .foundInside(row.id, RoundedCornerShape(8.dp))
            .onRightClick { onLongPress() }
            .pointerInput(row.id) {
                detectTapGestures(onTap = { onOpen() }, onLongPress = { onLongPress() })
            }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowIconBadge(row.id, ROW_ICON_SIZE)
            Text(
                text = highlightedTitle(row.title),
                style = MaterialTheme.typography.bodyMedium,
                color = if (row.title.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onBackground
                }
            )
        }

        // La proprietà che fa da colonna non si ripete sulle schede:
        // è già scritta in cima alla colonna che le contiene (e quella
        // delle fasce in cima alla fascia).
        state.visibleColumns
            .filter { it.id !in hiddenColumnIds }
            .forEach { column ->
                val value = state.cellValues[row.id to column.id].orEmpty()
                if (value.isNotBlank()) {
                    Spacer(modifier = Modifier.size(6.dp))
                    CardPropertyValue(column, value, linkLabel = state.linkLabels[row.id to column.id])
                }
            }
    }
}

// Quanto colore del tag prendono, in "Color columns", la colonna, le schede,
// i loro bordi, e quanto si schiariscono il numero e "New".
private const val BOARD_COLUMN_TINT = 0.25f
private const val BOARD_CARD_TINT = 0.45f
private const val BOARD_BORDER_TINT = 0.65f
private const val BOARD_ACCENT_LIGHT = 0.35f

/**
 * La visualizzazione a calendario: un anno, un mese, una settimana o un
 * giorno per volta, con le pagine disegnate come barre lunghe quanto i
 * giorni che occupano — nel giorno (dal 30/09/2026), come blocchi alti
 * quanto le ore, per il time-blocking (vedi `CalendarDayView`).
 *
 * Il periodo mostrato non viene salvato — riaprendo si torna a oggi,
 * che è quasi sempre quello che interessa — mentre la scelta fra anno,
 * mese e settimana sì, perché è un modo di lavorare e non una
 * posizione.
 */
@Composable
private fun CalendarLayout(
    state: DatabaseTableState,
    dateColumn: DatabaseColumnEntity?,
    mode: CalendarMode,
    newRowTick: Int,
    onSetMode: (CalendarMode) -> Unit,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit,
    onAddRowOnDate: (Long) -> Unit,
    /** L'evento nuovo che si sta scrivendo, per vederlo già nella griglia del giorno. */
    draft: EventDraft? = null,
    /** Un quadratino vuoto del giorno toccato: null col database bloccato. */
    onSlotTap: ((LocalDateTime) -> Unit)? = null,
    onAddDateProperty: () -> Unit
) {
    // **Senza proprietà data il calendario si vede lo stesso**, vuoto,
    // sul mese corrente. Prima al suo posto c'era un cartello che
    // chiedeva di creare la proprietà: corretto ma inutile, perché un
    // calendario vuoto si capisce da solo, e quello che si vuole fare
    // — toccare un giorno e scriverci una pagina — funziona comunque:
    // la proprietà data viene creata al volo al primo tocco.

    // Il primo giorno della settimana segue le convenzioni locali:
    // lunedì in Italia, domenica altrove.
    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val today = LocalDate.now()

    // Il periodo mostrato è sempre individuato da un giorno che ci sta
    // dentro, così spostarsi avanti e indietro è la stessa operazione
    // per il mese e per la settimana.
    var anchor by remember { mutableStateOf(today) }

    // Una pagina creata dal pulsante in fondo nasce datata oggi: se si
    // stava guardando un altro periodo, bisogna tornare lì per vederla.
    LaunchedEffect(newRowTick) {
        if (newRowTick > 0) anchor = LocalDate.now()
    }

    val monthGridStart = startOfWeek(anchor.withDayOfMonth(1), firstDayOfWeek)
    val monthWeekCount = (
        ChronoUnit.DAYS.between(monthGridStart, anchor.withDayOfMonth(1)).toInt() +
            anchor.lengthOfMonth() + 6
        ) / 7
    val shownWeekStart = startOfWeek(anchor, firstDayOfWeek)

    // Il pezzo di tempo che si sta guardando. Le ripetizioni si
    // calcolano solo qui dentro: una regola senza fine ("ogni lunedì")
    // genererebbe occorrenze all'infinito, e di quelle del 2043 non se
    // ne fa niente nessuno finché non ci si arriva.
    val windowStart: LocalDate
    val windowEnd: LocalDate
    when (mode) {
        CalendarMode.YEAR -> {
            windowStart = LocalDate.of(anchor.year, 1, 1)
            windowEnd = LocalDate.of(anchor.year, 12, 31)
        }
        CalendarMode.MONTH -> {
            windowStart = monthGridStart
            windowEnd = monthGridStart.plusWeeks(monthWeekCount.toLong())
        }
        CalendarMode.WEEK -> {
            // Una settimana per lato: la settimana si scorre col dito e
            // quelle accanto sono già montate.
            windowStart = shownWeekStart.minusWeeks(1)
            windowEnd = shownWeekStart.plusWeeks(2).plusDays(6)
        }
        CalendarMode.DAY -> {
            // Il giorno e quelli accanto, già montati per lo scorrimento; uno
            // in più per parte per gli eventi che passano la mezzanotte.
            windowStart = anchor.minusDays(2)
            windowEnd = anchor.plusDays(2)
        }
    }

    // Le pagine con la loro data, calcolate una volta sola: ogni
    // settimana le riusa per capire quali la attraversano. Senza
    // proprietà data non ce n'è nessuna, e il calendario resta vuoto.
    //
    // **Una pagina che si ripete resta una pagina sola.** Qui una
    // regola diventa tante comparse quante ne servono per il periodo
    // mostrato, ma sono tutte la stessa riga del database: aprirne una
    // qualsiasi apre quella pagina, e modificarla cambia tutte le
    // volte, perché la cosa modificata è una sola.
    val dated = remember(state.rows, state.cellValues, dateColumn?.id, windowStart, windowEnd) {
        val column = dateColumn ?: return@remember emptyList()
        state.rows.flatMap { row ->
            val range = parseDateRange(state.cellValues[row.id to column.id].orEmpty())
                ?: return@flatMap emptyList()
            val rule = range.recurrence ?: return@flatMap listOf(row to range)
            val firstDay = range.start.toLocalDate()
            val startTime = range.start.toLocalDateTime().toLocalTime()
            // Un impegno di tre giorni che si ripete dura tre giorni
            // tutte le volte: la durata se la porta dietro.
            val lengthDays = range.end?.toLocalDate()
                ?.let { ChronoUnit.DAYS.between(firstDay, it) }
            rule.occurrences(firstDay, windowStart, windowEnd).map { day ->
                val occurrenceStart = LocalDateTime.of(day, startTime).toEpochMillis()
                row to DateRange(
                    start = occurrenceStart,
                    end = lengthDays?.let {
                        LocalDateTime.of(
                            day.plusDays(it),
                            range.end!!.toLocalDateTime().toLocalTime()
                        ).toEpochMillis()
                    },
                    hasTime = range.hasTime,
                    recurrence = rule
                )
            }
        }
    }
    val undated = remember(state.rows, state.cellValues, dateColumn?.id) {
        val column = dateColumn ?: return@remember state.rows
        state.rows.filter { state.cellValues[it.id to column.id].isNullOrBlank() }
    }

    val onAddOnDate: (LocalDate) -> Unit = { date ->
        onAddRowOnDate(date.atStartOfDay(AppSettings.zoneId).toInstant().toEpochMilli())
    }

    // Le righe si contano su tutto il database, una volta sola: non
    // dipendono da cosa si sta guardando, quindi non si rinumerano mai.
    //
    // Si contano sulle date **di partenza**, non sulle comparse
    // generate dalle ripetizioni: quelle cambiano da un mese all'altro
    // e farebbero ballare la riga di una pagina a seconda di dove ci
    // si trova, che è esattamente quello che questa riga fissa vuole
    // evitare.
    val baseDated = remember(state.rows, state.cellValues, dateColumn?.id) {
        val column = dateColumn ?: return@remember emptyList()
        state.rows.mapNotNull { row ->
            parseDateRange(state.cellValues[row.id to column.id].orEmpty())?.let { row to it }
        }
    }
    val rowIndexes = remember(baseDated) { calendarRowIndexes(baseDated) }
    // Quante righe riserva ogni settimana. È lo stesso numero ovunque —
    // la griglia delle righe è una cosa sola, non cambia da una
    // settimana all'altra — e il tetto serve solo a non far diventare
    // smisurato un database con moltissime pagine datate.
    val reservedRows = rowIndexes.size.coerceAtMost(CALENDAR_MAX_ROWS)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    anchor = when (mode) {
                        CalendarMode.YEAR -> anchor.minusYears(1)
                        CalendarMode.MONTH -> anchor.minusMonths(1)
                        CalendarMode.WEEK -> anchor.minusWeeks(1)
                        CalendarMode.DAY -> anchor.minusDays(1)
                    }
                }
            ) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = DbStrings.previous)
            }
            Text(
                text = when (mode) {
                    CalendarMode.YEAR -> anchor.year.toString()
                    CalendarMode.MONTH -> anchor.format(MONTH_FORMAT)
                    CalendarMode.WEEK -> {
                        val start = startOfWeek(anchor, firstDayOfWeek)
                        "${start.format(DAY_MONTH_FORMAT)} – ${start.plusDays(6).format(DATE_FORMAT)}"
                    }
                    CalendarMode.DAY -> "${
                        anchor.dayOfWeek.getDisplayName(TextStyle.FULL, displayLocale)
                            .replaceFirstChar { it.titlecase(displayLocale) }
                    }, ${anchor.format(DATE_FORMAT)}"
                },
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    anchor = when (mode) {
                        CalendarMode.YEAR -> anchor.plusYears(1)
                        CalendarMode.MONTH -> anchor.plusMonths(1)
                        CalendarMode.WEEK -> anchor.plusWeeks(1)
                        CalendarMode.DAY -> anchor.plusDays(1)
                    }
                }
            ) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = DbStrings.next)
            }
        }

        // Con Day i pulsantini sono quattro e centrati su tutta la riga
        // finivano sotto Today: si centrano nello spazio che lui lascia.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
                CalendarMode.entries.forEachIndexed { index, option ->
                    if (index > 0) Spacer(modifier = Modifier.size(6.dp))
                    CalendarModeChip(
                        label = option.displayName(),
                        selected = option == mode,
                        onClick = { onSetMode(option) }
                    )
                }
            }

            // Riporta a oggi da qualunque distanza, in qualunque delle
            // tre modalità: allontanandosi di mesi o di anni tornare
            // indietro con le frecce diventa un lavoro.
            //
            // Sta lì sempre, anche quando si è già su oggi e non
            // farebbe niente: un pulsante che compare e sparisce
            // costringe a cercarlo ogni volta, e la riga cambierebbe
            // aspetto da sola mentre si sfoglia.
            Text(
                text = DbStrings.today,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier = Modifier
                    .background(DarkSurface, RoundedCornerShape(percent = 50))
                    .clickable { anchor = today }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        if (mode == CalendarMode.YEAR) {
            CalendarYearGrid(
                year = anchor.year,
                today = today,
                firstDayOfWeek = firstDayOfWeek,
                dated = dated,
                // Un mese in miniatura si guarda, non ci si lavora:
                // toccarlo porta a vederlo per intero.
                onOpenMonth = { month ->
                    anchor = month.atDay(1)
                    onSetMode(CalendarMode.MONTH)
                }
            )
        } else if (mode != CalendarMode.DAY) {
            Row(modifier = Modifier.fillMaxWidth()) {
                (0..6).forEach { offset ->
                    Text(
                        text = firstDayOfWeek.plus(offset.toLong())
                            .getDisplayName(TextStyle.SHORT, displayLocale),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f).padding(vertical = 6.dp)
                    )
                }
            }
            HorizontalDivider()
        }

        if (mode == CalendarMode.DAY) {
            HorizontalDivider()
            CalendarDayView(
                day = anchor,
                dated = dated,
                draft = draft,
                onOpenRow = onOpenRow,
                onRowLongPress = onRowLongPress,
                onSlotTap = onSlotTap,
                onDayChange = { anchor = it }
            )
            HorizontalDivider()
        } else if (mode == CalendarMode.WEEK) {
            CalendarWeekSwiper(
                weekStart = shownWeekStart,
                today = today,
                dated = dated,
                rowIndexes = rowIndexes,
                reservedRows = reservedRows,
                onOpenRow = onOpenRow,
                onRowLongPress = onRowLongPress,
                onAddRowOnDate = onAddOnDate,
                onWeekChange = { anchor = it }
            )
            HorizontalDivider()
        } else if (mode == CalendarMode.MONTH) {
            // La griglia parte dal primo giorno della settimana che
            // contiene il primo del mese, così le colonne restano
            // allineate ai giorni.
            val shownMonth = YearMonth.from(anchor)

            // Tutte le settimane riservano le stesse righe: è quello
            // che fa restare una barra sempre alla stessa altezza
            // scendendo da una settimana all'altra.
            val monthWeekHeight = (CALENDAR_WEEK_HEADER + CALENDAR_BAR_HEIGHT * reservedRows)
                .coerceAtLeast(CALENDAR_MONTH_WEEK_HEIGHT)

            repeat(monthWeekCount) { index ->
                val weekStart = monthGridStart.plusWeeks(index.toLong())
                CalendarWeekRow(
                    weekStart = weekStart,
                    shownMonth = shownMonth,
                    today = today,
                    segments = calendarSegments(weekStart, dated),
                    rowIndexes = rowIndexes,
                    maxRows = CALENDAR_MAX_ROWS,
                    height = monthWeekHeight,
                    onOpenRow = onOpenRow,
                    onRowLongPress = onRowLongPress,
                    onAddRowOnDate = onAddOnDate
                )
                HorizontalDivider()
            }
        }

        // Le righe senza data non hanno un giorno in cui stare, ma non
        // devono sparire: il calendario sarebbe l'unico posto da cui
        // non si riesce più a raggiungerle.
        if (undated.isNotEmpty()) {
            Text(
                text = DbStrings.noDate(undated.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
            )
            undated.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .foundInside(row.id)
                        .onRightClick { onRowLongPress(row) }
                        .pointerInput(row.id) {
                            detectTapGestures(
                                onTap = { onOpenRow(row) },
                                onLongPress = { onRowLongPress(row) }
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RowIconBadge(row.id, ROW_ICON_SIZE)
                    Text(
                        text = highlightedTitle(row.title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Il primo giorno della settimana che contiene questa data. */
private fun startOfWeek(date: LocalDate, firstDayOfWeek: DayOfWeek): LocalDate {
    val shift = ((date.dayOfWeek.value - firstDayOfWeek.value) + 7) % 7
    return date.minusDays(shift.toLong())
}

/** Quanto è alta una settimana quando è l'unica cosa sullo schermo. */
private fun weekViewHeight(barCount: Int): Dp =
    (CALENDAR_WEEK_HEADER + CALENDAR_BAR_HEIGHT * barCount)
        .coerceAtLeast(CALENDAR_WEEK_MIN_HEIGHT)

/**
 * La settimana che si sposta col dito.
 *
 * Lo scorrimento è un `HorizontalPager`, cioè lo stesso componente con
 * cui si sfogliano le pagine ovunque su Android. **Prima era scritto a
 * mano**: tre settimane affiancate, uno scostamento seguito col dito e
 * una molla per farlo atterrare. Funzionava sulla carta e nelle misure
 * fatte con trascinamenti sintetici risultava perfettamente fluido, ma
 * sotto il dito vero restava uno scatto che tre tentativi non hanno
 * tolto. Rifarsi la fisica dello scorrimento vuol dire rifare anche il
 * lancio, l'aggancio alla pagina, il gesto afferrato a metà corsa e la
 * velocità al rilascio: ognuna di quelle cose è un modo di sbagliare, e
 * quella giusta esiste già.
 *
 * Il pager conta le settimane da `origin`, che resta ferma per tutta la
 * vita del componente: così il numero di pagina di una settimana non
 * cambia mai sotto i piedi dello scorrimento.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarWeekSwiper(
    weekStart: LocalDate,
    today: LocalDate,
    dated: List<Pair<DatabaseRowEntity, DateRange>>,
    rowIndexes: Map<String, Int>,
    reservedRows: Int,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit,
    onAddRowOnDate: (LocalDate) -> Unit,
    onWeekChange: (LocalDate) -> Unit
) {
    val origin = remember { weekStart }
    fun pageOf(date: LocalDate) =
        WEEK_PAGER_CENTER + ChronoUnit.WEEKS.between(origin, date).toInt()

    fun weekOf(page: Int) = origin.plusWeeks((page - WEEK_PAGER_CENTER).toLong())

    val pagerState = rememberPagerState(
        initialPage = pageOf(weekStart),
        pageCount = { WEEK_PAGER_PAGES }
    )

    // Chi guarda deve sapere dove siamo arrivati, ma solo a scorrimento
    // finito: aggiornarlo mentre il dito è ancora giù rifarebbe i conti
    // delle righe a metà gesto.
    LaunchedEffect(pagerState.settledPage) {
        val settled = weekOf(pagerState.settledPage)
        if (settled != weekStart) onWeekChange(settled)
    }

    // E viceversa: se la settimana cambia da fuori (le frecce, Today)
    // il pager ci va senza animazione, perché il salto può essere di
    // mesi.
    LaunchedEffect(weekStart) {
        val target = pageOf(weekStart)
        if (pagerState.currentPage != target && !pagerState.isScrollInProgress) {
            pagerState.scrollToPage(target)
        }
    }

    // Tutte le settimane sono alte uguali: se ognuna prendesse la
    // propria altezza, sfogliando si vedrebbe la riga crescere e
    // rimpicciolirsi. L'altezza tiene conto delle righe **riservate**,
    // non di quante barre ci sono davvero in una settimana: una riga
    // lasciata vuota da una pagina finita occupa spazio come le altre.
    val height = weekViewHeight(reservedRows)

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth().height(height),
        verticalAlignment = Alignment.Top,
        // Le settimane ai lati vengono preparate **prima** che il dito
        // si muova. Senza, la settimana accanto viene composta e
        // misurata nell'istante in cui comincia il trascinamento — una
        // ventina di testi da impaginare proprio nel fotogramma in cui
        // serve tutto il tempo per scorrere, ed è lì che si vedeva lo
        // scatto.
        beyondViewportPageCount = 1
    ) { page ->
        val start = weekOf(page)
        // Ricalcolare i pezzi ad ogni fotogramma di scorrimento è la
        // stessa scansione fatta sessanta volte al secondo per niente:
        // dipende solo dalla settimana e dalle date.
        val segments = remember(start, dated) { calendarSegments(start, dated) }
        CalendarWeekRow(
            weekStart = start,
            shownMonth = null,
            today = today,
            segments = segments,
            rowIndexes = rowIndexes,
            maxRows = reservedRows,
            height = height,
            onOpenRow = onOpenRow,
            onRowLongPress = onRowLongPress,
            onAddRowOnDate = onAddRowOnDate
        )
    }
}

/**
 * L'anno intero: dodici mesi in miniatura, tre per riga.
 *
 * A questa scala le barre non si leggerebbero, quindi non ci sono: i
 * giorni con almeno una pagina sono cerchiati, e basta a far vedere a
 * colpo d'occhio dove si concentra il lavoro. Toccando un mese lo si
 * apre da vicino, che è l'unica cosa sensata da fare con un mese alto
 * un centimetro.
 */
@Composable
private fun CalendarYearGrid(
    year: Int,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
    dated: List<Pair<DatabaseRowEntity, DateRange>>,
    onOpenMonth: (YearMonth) -> Unit
) {
    val busyDays = remember(year, dated) { busyDaysOfYear(year, dated) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp)) {
        repeat(4) { rowIndex ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(3) { columnIndex ->
                    val month = YearMonth.of(year, rowIndex * 3 + columnIndex + 1)
                    MiniMonth(
                        month = month,
                        today = today,
                        firstDayOfWeek = firstDayOfWeek,
                        busyDays = busyDays,
                        onClick = { onOpenMonth(month) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * I giorni dell'anno coperti da almeno una pagina.
 *
 * Tagliare ogni intervallo sull'anno mostrato mette da sé un tetto al
 * ciclo — al massimo trecentosessantasei passi per pagina — quindi una
 * data digitata per sbaglio nell'anno tremila non blocca niente.
 */
private fun busyDaysOfYear(
    year: Int,
    dated: List<Pair<DatabaseRowEntity, DateRange>>
): Set<LocalDate> {
    val yearStart = LocalDate.of(year, 1, 1)
    val yearEnd = LocalDate.of(year, 12, 31)
    val days = mutableSetOf<LocalDate>()
    dated.forEach { (_, range) ->
        val rangeStart = range.start.toLocalDate()
        var day = maxOf(rangeStart, yearStart)
        val last = minOf(range.end?.toLocalDate() ?: rangeStart, yearEnd)
        while (!day.isAfter(last)) {
            days.add(day)
            day = day.plusDays(1)
        }
    }
    return days
}

/** Un mese in miniatura dentro la vista dell'anno. */
@Composable
private fun MiniMonth(
    month: YearMonth,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
    busyDays: Set<LocalDate>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gridStart = startOfWeek(month.atDay(1), firstDayOfWeek)
    Column(
        modifier = modifier
            .pointerInput(month) { detectTapGestures(onTap = { onClick() }) }
            .padding(horizontal = 2.dp, vertical = 5.dp)
    ) {
        Text(
            text = month.month.getDisplayName(TextStyle.SHORT, displayLocale),
            style = MaterialTheme.typography.labelSmall,
            color = if (YearMonth.from(today) == month) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onBackground
            },
            maxLines = 1,
            modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
        )
        repeat(6) { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { dayOfWeek ->
                    val date = gridStart.plusDays((week * 7 + dayOfWeek).toLong())
                    Box(
                        modifier = Modifier.weight(1f).height(MINI_DAY_SIZE),
                        contentAlignment = Alignment.Center
                    ) {
                        // I giorni del mese prima e dopo restano vuoti:
                        // dodici griglie affiancate diventano illeggibili
                        // se ognuna sconfina in quella accanto.
                        if (YearMonth.from(date) == month) {
                            Box(
                                modifier = Modifier
                                    .size(MINI_DAY_SIZE)
                                    .background(
                                        when {
                                            date == today -> MaterialTheme.colorScheme.primary
                                            date in busyDays -> DarkSurfaceVariant
                                            else -> Color.Transparent
                                        },
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    fontSize = MINI_DAY_FONT,
                                    lineHeight = MINI_DAY_FONT,
                                    color = if (date == today) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onBackground
                                    },
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Il pulsantino Anno/Mese/Settimana sopra il calendario. */
@Composable
private fun CalendarModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) {
            MaterialTheme.colorScheme.onBackground
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier
            .background(
                if (selected) DarkSurfaceVariant else Color.Transparent,
                RoundedCornerShape(percent = 50)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    )
}

/**
 * Un pezzo di pagina dentro una settimana: da quale colonna a quale
 * colonna arriva, e se prosegue oltre i bordi della settimana.
 *
 * Una pagina lunga due settimane produce due pezzi, uno per settimana:
 * è così che una barra può "spezzarsi" andando a capo e restare
 * riconoscibile come una cosa sola.
 */
@Immutable
private data class CalendarSegment(
    val row: DatabaseRowEntity,
    /** La data vera della pagina, non il giorno in cui la barra entra nella settimana. */
    val rangeStart: LocalDate,
    val rangeEnd: LocalDate,
    val startColumn: Int,
    val endColumn: Int,
    val continuesBefore: Boolean,
    val continuesAfter: Boolean
)

/** I pezzi di pagina che attraversano questa settimana. */
private fun calendarSegments(
    weekStart: LocalDate,
    dated: List<Pair<DatabaseRowEntity, DateRange>>
): List<CalendarSegment> {
    val weekEnd = weekStart.plusDays(6)

    return dated.mapNotNull { (row, range) ->
        val first = range.start.toLocalDate()
        val last = range.end?.toLocalDate() ?: first
        if (last.isBefore(weekStart) || first.isAfter(weekEnd)) return@mapNotNull null
        CalendarSegment(
            row = row,
            rangeStart = first,
            rangeEnd = last,
            startColumn = maxOf(0, ChronoUnit.DAYS.between(weekStart, first).toInt()),
            endColumn = minOf(6, ChronoUnit.DAYS.between(weekStart, last).toInt()),
            continuesBefore = first.isBefore(weekStart),
            continuesAfter = last.isAfter(weekEnd)
        )
    }
}

/**
 * A ogni pagina datata del database la sua riga, **sua per sempre**:
 * tutte le pagine ordinate per data, la prima in alto.
 *
 * Le pagine si comportano come blocchi impilati. Una pagina tiene la
 * sua riga in ogni settimana, in ogni mese e in ogni anno, e quando la
 * sua data finisce quella riga resta **vuota** per sempre invece di
 * essere occupata da un'altra: così le barre non si rimescolano mai e
 * seguirne una con l'occhio è immediato.
 *
 * Le righe si contano su **tutto il database** e non sul periodo che si
 * sta guardando — è quella la differenza, ed è una scelta esplicita
 * dell'utente. Contarle sul periodo le farebbe ricompattare ad ogni
 * cambio di mese: nessuna pagina ruberebbe la riga di un'altra, ma la
 * stessa pagina si troverebbe a un'altezza diversa a seconda di cosa
 * c'è attorno. Il prezzo è lo spazio vuoto: la decima pagina in ordine
 * di data sta alla decima riga anche nelle settimane in cui è l'unica,
 * con nove righe vuote sopra.
 *
 * A parità di data l'ordine è comunque sempre lo stesso, altrimenti due
 * pagine dello stesso giorno si scambierebbero di posto ad ogni
 * ridisegno.
 */
private fun calendarRowIndexes(
    dated: List<Pair<DatabaseRowEntity, DateRange>>
): Map<String, Int> {
    data class Placed(val row: DatabaseRowEntity, val start: Long, val end: Long)

    // L'ordine guarda l'istante e non il giorno: due pagine dello
    // stesso giorno con un'ora diversa hanno un ordine naturale, ed è
    // quello.
    return dated.map { (row, range) ->
        Placed(row, range.start, range.end ?: range.start)
    }.sortedWith(
        compareBy<Placed> { it.start }
            .thenBy { it.end }
            .thenBy { it.row.title }
            .thenBy { it.row.id }
    ).withIndex().associate { (index, placed) -> placed.row.id to index }
}

/**
 * La riga di una pagina: le sue barre alle loro colonne, con gli spazi
 * vuoti prima, in mezzo e dopo.
 *
 * Le larghezze sono pesi e non misure fisse, così una barra di tre
 * giorni occupa esattamente tre settimi della riga qualunque sia la
 * larghezza dello schermo — che è il senso di "rispettare la lunghezza
 * dei giorni".
 *
 * **Le barre possono essere più d'una** (dal 30/09/2026): una pagina che
 * si ripete ogni giorno compare più volte nella stessa settimana, sempre
 * nella sua riga. Prima ne restava una sola — l'ultima — e le altre
 * finivano nel "+N". Le volte che si accavallano (un impegno di tre
 * giorni ripetuto ogni giorno) partono dal primo giorno libero.
 */
@Composable
private fun CalendarBarRow(
    segments: List<CalendarSegment>,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().height(CALENDAR_BAR_HEIGHT)) {
        var column = 0
        segments.sortedBy { it.startColumn }.forEach { segment ->
            val start = maxOf(segment.startColumn, column)
            if (start > segment.endColumn) return@forEach
            if (start > column) {
                Spacer(modifier = Modifier.weight((start - column).toFloat()))
            }
            val span = segment.endColumn - start + 1
            CalendarBar(
                segment = if (start == segment.startColumn) segment else segment.copy(startColumn = start, continuesBefore = true),
                onOpen = { onOpenRow(segment.row) },
                onLongPress = { onRowLongPress(segment.row) },
                modifier = Modifier.weight(span.toFloat())
            )
            column = segment.endColumn + 1
        }
        if (column < 7) {
            Spacer(modifier = Modifier.weight((7 - column).toFloat()))
        }
    }
}

/**
 * La barra di una pagina: squadrata sui lati dove prosegue oltre la
 * settimana. Del colore del suo evento, se ne ha uno (dal 30/09/2026).
 */
@Composable
private fun CalendarBar(
    segment: CalendarSegment,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val round = 4.dp
    val flat = 0.dp
    val eventColor = segment.row.eventColor
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 1.dp, vertical = 1.dp)
            .background(
                if (eventColor != null) eventBackground(eventColor) else DarkSurface,
                RoundedCornerShape(
                    topStart = if (segment.continuesBefore) flat else round,
                    bottomStart = if (segment.continuesBefore) flat else round,
                    topEnd = if (segment.continuesAfter) flat else round,
                    bottomEnd = if (segment.continuesAfter) flat else round
                )
            )
            .foundInside(segment.row.id, RoundedCornerShape(round))
            .onRightClick { onLongPress() }
            .pointerInput(segment.row.id) {
                detectTapGestures(onTap = { onOpen() }, onLongPress = { onLongPress() })
            }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowIconBadge(segment.row.id, BAR_ICON_SIZE)
            Text(
                text = highlightedTitle(segment.row.title),
                style = MaterialTheme.typography.labelSmall,
                color = eventContentColor(eventColor),
                maxLines = 1
            )
        }
    }
}

/**
 * Una settimana: i numeri dei giorni e sopra di loro le barre.
 *
 * I giorni stanno sotto e le barre sopra, in due strati sovrapposti:
 * gli spazi vuoti fra una barra e l'altra non intercettano il tocco,
 * quindi toccare un punto libero della settimana crea una pagina in
 * quel giorno, mentre toccare una barra apre la sua pagina.
 */
@Composable
private fun CalendarWeekRow(
    weekStart: LocalDate,
    shownMonth: YearMonth?,
    today: LocalDate,
    segments: List<CalendarSegment>,
    rowIndexes: Map<String, Int>,
    maxRows: Int,
    height: Dp,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit,
    onAddRowOnDate: (LocalDate) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().height(height)) {
        Row(modifier = Modifier.fillMaxSize()) {
            repeat(7) { dayIndex ->
                val date = weekStart.plusDays(dayIndex.toLong())
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(date) {
                            detectTapGestures(onTap = { onAddRowOnDate(date) })
                        }
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { dayIndex ->
                    val date = weekStart.plusDays(dayIndex.toLong())
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = when {
                            date == today -> MaterialTheme.colorScheme.primary
                            shownMonth == null || YearMonth.from(date) == shownMonth ->
                                MaterialTheme.colorScheme.onBackground
                            // I giorni del mese prima e dopo restano
                            // visibili ma smorzati: servono a non
                            // spezzare la griglia.
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        },
                        modifier = Modifier.weight(1f).padding(vertical = 2.dp)
                    )
                }
            }

            // Ogni pagina alla riga che le spetta su tutto il periodo.
            // Le righe di chi in questa settimana non c'è restano vuote:
            // è quello che tiene le barre allineate da una settimana
            // all'altra.
            // Il posto di ogni barra dipende solo dai pezzi e dalle
            // righe: rifarlo ad ogni fotogramma di scorrimento è lavoro
            // buttato proprio quando il tempo serve tutto per scorrere.
            val placed = remember(segments, rowIndexes) {
                segments.mapNotNull { segment ->
                    rowIndexes[segment.row.id]?.let { index -> index to segment }
                }
            }
            // Una riga può avere più barre: le volte di una pagina che si
            // ripete (vedi `CalendarBarRow`).
            val visible = placed.filter { it.first < maxRows }.groupBy({ it.first }, { it.second })
            val rowsToDraw = (visible.keys.maxOrNull()?.plus(1)) ?: 0
            repeat(rowsToDraw) { rowIndex ->
                val rowSegments = visible[rowIndex]
                if (rowSegments != null) {
                    CalendarBarRow(
                        segments = rowSegments,
                        onOpenRow = onOpenRow,
                        onRowLongPress = onRowLongPress
                    )
                } else {
                    Spacer(modifier = Modifier.height(CALENDAR_BAR_HEIGHT))
                }
            }
            // Le pagine oltre il tetto, contate una volta anche se si ripetono.
            val hidden = placed.filter { it.first >= maxRows }.map { it.second.row.id }.distinct().size
            if (hidden > 0) {
                Text(
                    text = "+$hidden",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

/**
 * La visualizzazione a elenco: le stesse righe senza la griglia — e, dal
 * 05/10/2026, senza le linee fra una riga e l'altra —, con il nome in
 * evidenza e i valori delle proprietà di fianco, come su Notion.
 *
 * Qui il nome non si scrive sul posto: si tocca la riga e si apre la
 * pagina, che è dove in questa vista si lavora. Il tocco prolungato
 * apre le azioni, come nella tabella.
 */
@Composable
private fun ListLayout(
    state: DatabaseTableState,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit,
    /** A pagina intera: solo le righe sullo schermo (vedi `LazyBody`). */
    lazy: LazyBody? = null
) {
    if (lazy != null) {
        LazyColumn(modifier = Modifier.fillMaxSize().keepFocusedAboveKeyboard()) {
            items(state.rows, key = { it.id }) { row -> ListRow(row, state, onOpenRow, onRowLongPress) }
            item(key = "trailing") { lazy.trailing() }
        }
        return
    }
    // Dentro una pagina: le prime righe e "Load more" (vedi `RowLimits`).
    val limits = LocalRowLimits.current
    val shown = limits.limit("list", state.rows) { it.id }
    Column(modifier = Modifier.fillMaxWidth()) {
        shown.items.forEach { row -> ListRow(row, state, onOpenRow, onRowLongPress) }
        if (shown.hidden > 0) LoadMoreButton(shown.hidden, onClick = { limits?.more("list") })
    }
}

/** Una riga dell'elenco: l'icona, il nome, e a destra i primi due valori. */
@Composable
private fun ListRow(
    row: DatabaseRowEntity,
    state: DatabaseTableState,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .foundInside(row.id)
            .onRightClick { onRowLongPress(row) }
            .pointerInput(row.id) {
                detectTapGestures(
                    onTap = { onOpenRow(row) },
                    onLongPress = { onRowLongPress(row) }
                )
            }
            .padding(horizontal = 16.dp, vertical = pcOr(5.dp, 14.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowIconBadge(row.id, ROW_ICON_SIZE)
        Text(
            text = highlightedTitle(row.title),
            style = MaterialTheme.typography.bodyLarge,
            color = if (row.title.isBlank()) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onBackground
            },
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        // A destra i valori che ci stanno, come fa Notion: solo
        // quelli valorizzati, altrimenti la riga si riempirebbe
        // di trattini.
        state.visibleColumns.take(2).forEach { column ->
            val value = state.cellValues[row.id to column.id].orEmpty()
            if (value.isNotBlank()) {
                Spacer(modifier = Modifier.size(10.dp))
                CardPropertyValue(column, value, linkLabel = state.linkLabels[row.id to column.id])
            }
        }
    }
    // Niente linea fra una riga e l'altra (tolta il 05/10/2026,
    // chiesto dall'utente): come su Notion, le righe si separano
    // con lo spazio e basta.
}

/**
 * Le misure della galleria.
 *
 * Le schede non hanno una larghezza fissa ma **una larghezza minima**
 * per ogni dimensione: in una riga ne entrano quante ci stanno, e si
 * allargano insieme a riempirla. Su un telefono in verticale sono tre
 * piccole, due medie o una grande per riga; girandolo, o su uno schermo
 * più largo, ne entrano di più invece di diventare enormi.
 */
private val GALLERY_MIN_CARD_SMALL = 100.dp
private val GALLERY_MIN_CARD_MEDIUM = 150.dp
private val GALLERY_MIN_CARD_LARGE = 280.dp
private val GALLERY_GAP = 10.dp

/**
 * Larghezza su altezza della parte alta di una scheda. È più bassa della
 * striscia di una copertina di pagina in proporzione, ma non troppo:
 * dentro deve ancora capirsi cos'è l'immagine, e l'anteprima del testo
 * deve avere posto per qualche riga.
 */
private const val GALLERY_PREVIEW_RATIO = 1.6f

/** Quante schede stanno in una riga larga `width`, con la dimensione scelta. */
private fun galleryColumnsFor(width: Dp, size: GalleryCardSize): Int {
    val minCard = when (size) {
        GalleryCardSize.SMALL -> GALLERY_MIN_CARD_SMALL
        GalleryCardSize.MEDIUM -> GALLERY_MIN_CARD_MEDIUM
        GalleryCardSize.LARGE -> GALLERY_MIN_CARD_LARGE
    }
    // Fra n schede ci sono n - 1 spazi: aggiungerne uno alla larghezza
    // disponibile rende il conto giusto con una divisione sola.
    return ((width + GALLERY_GAP) / (minCard + GALLERY_GAP)).toInt().coerceAtLeast(1)
}

/**
 * La visualizzazione a galleria: una griglia di schede, una per pagina,
 * con in alto la copertina della pagina o l'inizio del suo testo e sotto
 * il nome e le proprietà. Come la bacheca e l'elenco, si tocca una
 * scheda per aprire la pagina e la si tiene premuta per le azioni.
 *
 * **Non è una griglia pigra** (`LazyVerticalGrid`), come non lo sono le
 * altre viste: il database dentro una pagina sta in una lista che già
 * scorre, e una griglia che scorre per conto suo lì dentro non si può
 * mettere. Il prezzo è che tutte le schede si costruiscono insieme,
 * immagini comprese — vedi "Limiti noti" nel README.
 *
 * Le schede di una stessa riga sono **alte uguali**: una con tre
 * proprietà accanto a una senza lascerebbe un gradino a metà della
 * griglia, e la riga dopo sembrerebbe cominciare storta.
 */
@Composable
private fun GalleryLayout(
    state: DatabaseTableState,
    preview: GalleryCardPreview,
    size: GalleryCardSize,
    covers: Map<String, RowCover>,
    contentPreviews: Map<String, List<RowPreviewLine>>,
    /** Le righe con la pagina bloccata dalla password: vedi `LockedCardPreview`. */
    lockedRows: Set<String>,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit,
    /** La scheda di cui si sta spostando la copertina, se ce n'è una: vedi `GalleryCard`. */
    repositioningRowId: String? = null,
    onRepositionSave: (DatabaseRowEntity, Float, Offset) -> Unit = { _, _, _ -> },
    onRepositionCancel: () -> Unit = {},
    /** A pagina intera: solo le schede sullo schermo (vedi `LazyBody`). */
    lazy: LazyBody? = null
) {
    @Composable
    fun CardRow(cardsInRow: List<DatabaseRowEntity>, columns: Int, modifier: Modifier = Modifier) {
        GalleryRow(
            cardsInRow, columns, state, preview, size, covers, contentPreviews, lockedRows,
            onOpenRow, onRowLongPress, repositioningRowId, onRepositionSave, onRepositionCancel, modifier
        )
    }
    if (lazy != null) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // Le stesse schede per riga di sempre: i margini ai lati non contano.
            val columns = galleryColumnsFor(maxWidth - 32.dp, size)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.rows.chunked(columns), key = { "cards:" + it.first().id }) { cardsInRow ->
                    CardRow(cardsInRow, columns, Modifier.padding(start = 16.dp, end = 16.dp, bottom = GALLERY_GAP))
                }
                item(key = "trailing") { lazy.trailing() }
            }
        }
        return
    }
    // Dentro una pagina: le prime schede e "Load more" (vedi `RowLimits`).
    val limits = LocalRowLimits.current
    val shown = limits.limit("gallery", state.rows) { it.id }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        val columns = galleryColumnsFor(maxWidth, size)
        Column(verticalArrangement = Arrangement.spacedBy(GALLERY_GAP)) {
            shown.items.chunked(columns).forEach { cardsInRow -> CardRow(cardsInRow, columns) }
        }
    }
    if (shown.hidden > 0) LoadMoreButton(shown.hidden, onClick = { limits?.more("gallery") })
}

/** Una riga di schede della galleria, alte uguali. */
@Composable
private fun GalleryRow(
    cardsInRow: List<DatabaseRowEntity>,
    columns: Int,
    state: DatabaseTableState,
    preview: GalleryCardPreview,
    size: GalleryCardSize,
    covers: Map<String, RowCover>,
    contentPreviews: Map<String, List<RowPreviewLine>>,
    lockedRows: Set<String>,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit,
    repositioningRowId: String?,
    onRepositionSave: (DatabaseRowEntity, Float, Offset) -> Unit,
    onRepositionCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(GALLERY_GAP)
    ) {
        cardsInRow.forEach { row ->
            GalleryCard(
                row = row,
                state = state,
                preview = preview,
                size = size,
                cover = covers[row.id],
                contentLines = contentPreviews[row.id],
                locked = row.id in lockedRows,
                onOpen = { onOpenRow(row) },
                onLongPress = { onRowLongPress(row) },
                repositioning = row.id == repositioningRowId,
                onRepositionSave = { scale, offset -> onRepositionSave(row, scale, offset) },
                onRepositionCancel = onRepositionCancel,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
        // L'ultima riga, se non è piena, tiene le schede
        // larghe come le altre invece di allargarle a
        // riempire lo spazio: una scheda grande il doppio in
        // fondo sembrerebbe più importante delle altre.
        repeat(columns - cardsInRow.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * **L'anteprima di una scheda la cui pagina è bloccata con la password**
 * (dal 26/09/2026, chiesta dall'utente): uno sfondo sempre uguale, grigio
 * scuro sfumato, con un lucchetto bianco in mezzo. Scuro anche col tema
 * chiaro, perché il lucchetto resti bianco e si veda. Uguale per ogni
 * pagina bloccata, così non lascia capire niente di cosa c'è dentro.
 */
@Composable
private fun LockedCardPreview(modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier.background(Brush.verticalGradient(listOf(LockedCardTop, LockedCardBottom))),
        contentAlignment = Alignment.Center
    ) {
        // Il lucchetto cresce con la scheda: piccolo nelle schede piccole,
        // senza diventare enorme in quelle grandi.
        val iconSize = (minOf(maxWidth, maxHeight) * 0.34f).coerceIn(20.dp, 56.dp)
        Box(
            modifier = Modifier
                .size(iconSize * 1.7f)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = LockStrings.passwordLock,
                tint = Color.White,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

private val LockedCardTop = Color(0xFF4A4A4F)
private val LockedCardBottom = Color(0xFF26262A)

/**
 * I due pulsanti mentre si sposta la copertina di una scheda: ✕ lascia
 * com'era, ✓ salva. Tondi e piccoli nell'angolo, invece dei "Cancel" e
 * "Save" della pagina: quelli, insieme, sono più larghi di una scheda
 * media. Il bordo attorno dice che la scheda è in modifica, al posto
 * della scritta "trascina per spostare" che qui non ci starebbe.
 */
@Composable
private fun CardCoverMoveControls(onCancel: () -> Unit, onSave: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.border(2.dp, MaterialTheme.colorScheme.primary)) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CardCoverMoveButton(Icons.Filled.Close, Strings.cancel, onCancel)
            CardCoverMoveButton(Icons.Filled.Check, EditorStrings.save, onSave)
        }
    }
}

@Composable
private fun CardCoverMoveButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(DarkSheet.copy(alpha = 0.9f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = NotionWhite, modifier = Modifier.size(18.dp))
    }
}

/**
 * Una scheda della galleria: l'anteprima in alto, poi nome e proprietà.
 *
 * L'anteprima occupa il suo posto **anche quando è vuota** — una pagina
 * senza copertina, o senza testo — così le schede restano allineate e
 * si vede dove comparirebbe. Con "None" invece non c'è proprio.
 */
@Composable
private fun GalleryCard(
    row: DatabaseRowEntity,
    state: DatabaseTableState,
    preview: GalleryCardPreview,
    size: GalleryCardSize,
    cover: RowCover?,
    contentLines: List<RowPreviewLine>?,
    /** La pagina della riga è bloccata con la password: al posto dell'anteprima, il lucchetto. */
    locked: Boolean,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    /**
     * "Reposition cover": la copertina della scheda segue il dito —
     * trascinare e pizzicare, come nella pagina — e la scheda non si apre
     * né apre le azioni finché non si sceglie ✓ o ✕.
     */
    repositioning: Boolean = false,
    onRepositionSave: (Float, Offset) -> Unit = { _, _ -> },
    onRepositionCancel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalPlatformContext.current
    val store = remember(context) { PageImageStore(context) }

    // L'inquadratura della copertina: quella della scheda, se l'utente ne
    // ha scelta una, altrimenti la stessa della pagina. Mentre la si
    // sposta comanda quella di prova, e il database si scrive solo con ✓.
    val cardScale = cover?.cardCoverScale
    val savedScale = cardScale ?: cover?.coverScale ?: 1f
    val savedOffset = if (cardScale != null) {
        Offset(cover?.cardCoverOffsetX ?: 0f, cover?.cardCoverOffsetY ?: 0f)
    } else {
        Offset(cover?.coverOffsetX ?: 0f, cover?.coverOffsetY ?: 0f)
    }
    var liveScale by remember(row.id, repositioning) { mutableStateOf(savedScale) }
    var liveOffset by remember(row.id, repositioning) { mutableStateOf(savedOffset) }
    var previewBox by remember { mutableStateOf(IntSize.Zero) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }

    /** L'inquadratura di prova tenuta dentro i bordi della scheda, in frazioni della scheda. */
    fun clampedToCard(scale: Float, offsetPx: Offset): Offset {
        val limit = coverPanLimit(previewBox, imageSize.width, imageSize.height, scale)
        return Offset(
            offsetPx.x.coerceIn(-limit.x, limit.x) / previewBox.width.coerceAtLeast(1),
            offsetPx.y.coerceIn(-limit.y, limit.y) / previewBox.height.coerceAtLeast(1)
        )
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .foundInside(row.id)
            .onRightClick { onLongPress() }
            .pointerInput(row.id, repositioning) {
                if (!repositioning) {
                    detectTapGestures(onTap = { onOpen() }, onLongPress = { onLongPress() })
                }
            }
    ) {
        if (preview != GalleryCardPreview.NONE) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(GALLERY_PREVIEW_RATIO)
                    .background(DarkSurfaceVariant)
                    .onSizeChanged { previewBox = it }
            ) {
                if (locked) {
                    // Né la copertina né il testo: copertina e testo di una
                    // pagina bloccata non arrivano nemmeno fin qui (vedi
                    // `DatabaseDao.observeRowCovers`), questo è il loro posto.
                    LockedCardPreview(modifier = Modifier.matchParentSize())
                } else if (preview == GalleryCardPreview.PAGE_COVER && cover != null) {
                    // Tenuta dentro i bordi: l'inquadratura della pagina è
                    // nata su una striscia di un'altra forma, e anche quella
                    // di prova non deve mai scoprire un angolo vuoto.
                    CoverImage(
                        fileName = cover.coverImage,
                        store = store,
                        scale = if (repositioning) liveScale else savedScale,
                        offsetFraction = if (repositioning) liveOffset else savedOffset,
                        onImageSize = { imageSize = it },
                        clampOffset = true,
                        modifier = Modifier
                            .matchParentSize()
                            .then(
                                if (repositioning) {
                                    Modifier.pointerInput(row.id, previewBox, imageSize) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            val newScale = (liveScale * zoom).coerceIn(1f, MAX_COVER_ZOOM)
                                            // Si parte da dove l'immagine si vede davvero
                                            // (già tenuta dentro i bordi), non da dove
                                            // direbbe la frazione salvata per la pagina.
                                            val current = clampedToCard(
                                                newScale,
                                                Offset(liveOffset.x * previewBox.width, liveOffset.y * previewBox.height)
                                            )
                                            liveScale = newScale
                                            liveOffset = clampedToCard(
                                                newScale,
                                                Offset(
                                                    current.x * previewBox.width + pan.x,
                                                    current.y * previewBox.height + pan.y
                                                )
                                            )
                                        }
                                    }
                                } else {
                                    Modifier
                                }
                            )
                    )
                    if (repositioning) {
                        CardCoverMoveControls(
                            onCancel = onRepositionCancel,
                            onSave = {
                                onRepositionSave(
                                    liveScale,
                                    clampedToCard(
                                        liveScale,
                                        Offset(liveOffset.x * previewBox.width, liveOffset.y * previewBox.height)
                                    )
                                )
                            },
                            modifier = Modifier.matchParentSize()
                        )
                    }
                }
                if (preview == GalleryCardPreview.PAGE_CONTENT && !contentLines.isNullOrEmpty()) {
                    GalleryContentPreview(
                        lines = contentLines,
                        size = size,
                        modifier = Modifier.matchParentSize()
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RowIconBadge(row.id, ROW_ICON_SIZE)
                Text(
                    text = highlightedTitle(row.title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (row.title.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Le proprietà come sulle schede della bacheca: solo quelle
            // che si vedono e che hanno un valore, una per riga.
            state.visibleColumns.forEach { column ->
                val value = state.cellValues[row.id to column.id].orEmpty()
                val shown = if (column.type == ColumnType.COLOR) {
                    !parseColorCell(value).isEmpty
                } else {
                    listCellLabel(column, value).isNotBlank()
                }
                if (shown) {
                    Spacer(modifier = Modifier.size(4.dp))
                    CardPropertyValue(column, value, overflow = TextOverflow.Ellipsis, linkLabel = state.linkLabels[row.id to column.id])
                }
            }
        }
    }
}

/**
 * L'inizio del testo della pagina, in piccolo, dentro la parte alta
 * della scheda: "Page content" di Notion.
 *
 * Ogni riga si scrive come nella pagina — gli elenchi col loro segno (lo stesso della pagina, che cambia a ogni
 * rientro), le caselle spuntate o no, i toggle con la freccia — perché
 * l'anteprima serve a riconoscere la pagina, e una pagina si riconosce
 * anche dalla sua forma. Quello che non ci sta viene tagliato dal bordo
 * della scheda, non riassunto.
 */
@Composable
private fun GalleryContentPreview(
    lines: List<RowPreviewLine>,
    size: GalleryCardSize,
    modifier: Modifier = Modifier
) {
    // Il corpo del testo segue la scheda: in quella piccola ci stanno
    // tre dita di larghezza, e un testo normale mostrerebbe due parole.
    val bodySize = when (size) {
        GalleryCardSize.SMALL -> 8.sp
        GalleryCardSize.MEDIUM -> 10.sp
        GalleryCardSize.LARGE -> 12.sp
    }
    val indentStep = when (size) {
        GalleryCardSize.SMALL -> 6.dp
        GalleryCardSize.MEDIUM -> 8.dp
        GalleryCardSize.LARGE -> 12.dp
    }
    Column(
        modifier = modifier
            .clipToBounds()
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        lines.forEach { line ->
            val marker = when (line.type) {
                BlockType.BULLET_LIST_ITEM -> bulletMarkerFor(line.indentLevel) + " "
                BlockType.NUMBERED_LIST_ITEM -> numberedMarkerFor(line.indentLevel, line.ordinal) + " "
                BlockType.CHECKBOX -> if (line.isChecked) "☑ " else "☐ "
                BlockType.TOGGLE -> "▸ "
                else -> ""
            }
            // Il rientro vale solo per il testo scorrevole, come nella
            // pagina: i toggle e le caselle non hanno livelli.
            val indent = when (line.type) {
                BlockType.CHECKBOX, BlockType.TOGGLE -> 0
                else -> line.indentLevel
            }
            Text(
                text = marker + line.text,
                fontSize = bodySize,
                lineHeight = bodySize * 1.3f,
                color = if (line.type == BlockType.CHECKBOX && line.isChecked) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
                textDecoration = if (line.type == BlockType.CHECKBOX && line.isChecked) {
                    TextDecoration.LineThrough
                } else {
                    null
                },
                maxLines = 2,
                overflow = TextOverflow.Clip,
                modifier = Modifier.padding(start = indentStep * indent.coerceAtMost(4))
            )
        }
    }
}

// `DateRange`, `parseDateRange` e `encodeDateRange` stanno in
// `data/entity/DateValue.kt`: il formato del valore serve anche ai
// filtri, che girano nel ViewModel.

private fun LocalDateTime.toEpochMillis(): Long =
    atZone(AppSettings.zoneId).toInstant().toEpochMilli()

/** La mezzanotte di oggi, come la salvano le celle di tipo data. */
private fun todayAtStartOfDay(): Long =
    LocalDate.now().atStartOfDay(AppSettings.zoneId).toInstant().toEpochMilli()

/** Il valore di una cella reso leggibile in una riga sola, per la vista a elenco. */
private fun listCellLabel(column: DatabaseColumnEntity, value: String): String = when (column.type) {
    ColumnType.CHECKBOX -> if (value == "true") "✓" else ""
    ColumnType.COLOR -> parseColorCell(value).text
    ColumnType.NUMBER -> formatNumberValue(value, column)
    ColumnType.DATE -> parseDateRange(value)?.label() ?: value
    ColumnType.DURATION -> durationLabel(value, durationFormatOf(column.durationFormat)).orEmpty()
    ColumnType.MULTI_SELECT -> value.split(MULTI_VALUE_SEPARATOR)
        .filter { it.isNotBlank() }
        .joinToString(", ")
    else -> value
}

/**
 * Il titolo di una vista collegata: la freccia "↗" e il nome del database
 * di origine, come su Notion. Toccandolo si va a quel database; senza
 * `onClick` (non dovrebbe succedere) è solo un'etichetta.
 */
@Composable
private fun LinkedViewTitle(
    title: String,
    embedded: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .let { base -> if (onClick != null) base.clickable(onClick = onClick) else base }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.NorthEast,
            contentDescription = DbStrings.openSourceDatabase,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(if (embedded) 18.dp else 26.dp)
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = title.ifBlank { Strings.untitled },
            style = if (embedded) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Al posto della vista collegata, quando il suo database non c'è più
 * (cancellato per sempre) o è nel cestino: niente righe da mostrare, e
 * una riga che dice perché. I sei puntini restano, per togliere la vista.
 */
@Composable
private fun LinkedSourceMissing(onOpenBlockMenu: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sul PC i sei puntini compaiono col mouse accanto al blocco (`BlockHandle.kt`).
        if (onOpenBlockMenu != null && !isPc) {
            ToolbarIconButton(
                icon = Icons.Filled.DragIndicator,
                contentDescription = EditorStrings.blockOptions,
                onClick = onOpenBlockMenu
            )
        }
        Icon(
            Icons.Filled.LinkOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            text = DbStrings.linkedSourceMissing,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * La barra della ricerca dentro un database: la lente, il campo in cui
 * scrivere, la X per chiuderla. Sta al posto della riga degli strumenti,
 * alla stessa altezza, così la tabella sotto non salta.
 *
 * Il campo prende il cursore e la tastiera **solo quando la si apre**
 * (`requestFocus`): tornando da una pagina trovata la ricerca c'è ancora,
 * ma la tastiera non salta su da sola.
 */
@Composable
private fun DatabaseSearchBar(
    query: String,
    noResults: Boolean,
    requestFocus: Boolean,
    onFocusRequested: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
            onFocusRequested()
        }
    }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(percent = 50)
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.size(8.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            DbStrings.searchInDatabase,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    AutoCaps {
                        BasicTextField(
                            value = query,
                            onValueChange = { onQueryChange(it.replace("\n", "")) },
                            singleLine = true,
                            // "Cerca" sulla tastiera la chiude e basta: la
                            // ricerca si fa già mentre si scrive.
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .markFocusChanges()
                        )
                    }
                }
            }
            ToolbarIconButton(
                icon = Icons.Filled.Close,
                contentDescription = DbStrings.closeSearch,
                onClick = onClose
            )
        }
        if (noResults) {
            Text(
                text = Strings.noResults,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 14.dp, top = 8.dp)
            )
        }
    }
}

/**
 * La ricerca in corso, per chi disegna le righe in fondo alle sette viste:
 * vedi `RowSearch` e `LocalRowIcons`, che passa di qui per la stessa
 * ragione. Null quando non si cerca.
 */
private val LocalRowSearch = compositionLocalOf<RowSearch?> { null }

/**
 * Il giallo della ricerca. **Pieno** sulle lettere trovate nel nome di una
 * riga; **leggero** su tutta la riga quando la parola non è nel nome ma
 * dentro — in una proprietà o nella pagina. È quello che l'utente ha
 * chiesto: "si illuminano o i titoli delle pagine o le pagine dove si
 * trova quello che ho scritto". Giallo e non l'azzurro dell'app, perché
 * l'azzurro vuol dire "selezionato" e "attivo", e una riga trovata non è
 * né l'una né l'altra cosa.
 */
private val SearchHighlight = Color(0xFFFFD54F).copy(alpha = 0.55f)
private val SearchHighlightRow = Color(0xFFFFD54F).copy(alpha = 0.16f)

/**
 * Il nome di una riga come si scrive in una vista: "Untitled" se è vuoto,
 * e con **le lettere cercate illuminate** mentre si cerca.
 */
@Composable
fun highlightedTitle(title: String): AnnotatedString {
    val shown = title.ifBlank { Strings.untitled }
    val query = LocalRowSearch.current?.query
    if (query.isNullOrEmpty() || title.isBlank()) return shown.withEmojiFont()
    return buildAnnotatedString {
        append(shown.withEmojiFont())
        var from = 0
        while (from < shown.length) {
            val at = shown.indexOf(query, from, ignoreCase = true)
            if (at < 0) break
            addStyle(SpanStyle(background = SearchHighlight), at, at + query.length)
            from = at + query.length
        }
    }
}

/**
 * Illumina tutta la riga (o la scheda) quando la ricerca l'ha trovata per
 * quello che ha dentro e non per il nome. `shape` è la forma del
 * riquadro, perché il giallo segua gli angoli arrotondati delle schede.
 */
@Composable
fun Modifier.foundInside(rowId: String, shape: Shape = RectangleShape): Modifier {
    val search = LocalRowSearch.current ?: return this
    return if (rowId in search.foundInside) this.background(SearchHighlightRow, shape) else this
}

/**
 * Quanto sono grandi le icone della barra della vista: 20dp invece dei
 * 24 di serie, dentro un bottone da 36 invece che da 48 (40 fino al
 * 29/09/2026: col "+" delle viste, dentro una pagina il nome "Table" non
 * ci stava più e diventava "Tab…"). Sono numeri
 * unici apposta — le icone lì sopra devono restare tutte della stessa
 * misura, comprese quelle che si aggiungeranno (ricerca, filtri), e
 * scriverle ogni volta a mano è il modo sicuro perché prima o poi una
 * sia diversa dalle altre. Per aggiungerne una si usa
 * `ToolbarIconButton`, non un `IconButton` nudo.
 */
// Sul PC come la riga della vista di Notion (vedi `pcOr`).
private val TOOLBAR_ICON_SIZE: Dp get() = pcOr(16.dp, 20.dp)
private val TOOLBAR_BUTTON_SIZE: Dp get() = pcOr(28.dp, 36.dp)

/** Un'icona della barra della vista. Vedi `TOOLBAR_ICON_SIZE`. */
@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    IconButton(onClick = onClick, modifier = Modifier.size(TOOLBAR_BUTTON_SIZE)) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(TOOLBAR_ICON_SIZE),
            tint = tint
        )
    }
}

/**
 * La riga sotto al titolo: a sinistra la vista attiva (toccata, l'elenco
 * delle viste) e il "+" per aggiungerne una, a destra filtro,
 * ordinamento e impostazioni del database. Su Notion qui c'è anche la
 * ricerca, che manca perché non è ancora stata costruita, e un pulsante
 * blu per aggiungere una pagina, tolto perché faceva la stessa identica
 * cosa di "+ New page" in fondo alla tabella.
 */
@Composable
private fun ViewToolbar(
    layout: DatabaseLayout,
    sortActive: Boolean,
    filterActive: Boolean,
    /** La lente, subito dopo l'ordinamento: apre la barra della ricerca. */
    onOpenSearch: () -> Unit,
    /**
     * I sei puntini del menu del blocco, prima della vista: solo per un
     * database dentro una pagina. Stanno qui e non accanto al nome perché
     * il nome si può nascondere, e i sei puntini devono esserci sempre.
     */
    onOpenBlockMenu: (() -> Unit)?,
    /** Null quando il database è già a schermo intero: non c'è dove aprirlo. */
    onOpenFullPage: (() -> Unit)?,
    /**
     * Null quando la vista è bloccata ("Lock view"): il comando resta
     * **al suo posto, spento**, invece di sparire. Un pulsante che
     * scompare fa pensare a un guasto; uno spento dice che c'è ed è
     * stato messo a riposo apposta.
     */
    onOpenFilter: (() -> Unit)?,
    onOpenSort: (() -> Unit)?,
    /** Il nome della vista; null = quello del layout. */
    viewName: String?,
    /**
     * Il nome della vista: l'elenco delle viste, per passare dall'una
     * all'altra. C'è anche con "Lock view": guardare un'altra vista non
     * cambia com'è fatta nessuna. Il layout di quella di adesso si cambia
     * dalle impostazioni ("Layout").
     */
    onOpenViews: () -> Unit,
    /** Il "+" accanto al nome: una vista nuova. Null = spento. */
    onAddView: (() -> Unit)?,
    onOpenSettings: () -> Unit,
    /** Col titolo nascosto è lei la prima riga del database, per i sei puntini del PC (`BlockHandle.kt`). */
    firstLine: Boolean = false
) {
    val lockedTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .then(if (firstLine) Modifier.reportsFirstLine() else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // A sinistra lo spazio che resta: un nome di vista lungo si accorcia
        // coi puntini invece di spingere fuori le icone di destra.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        // Sul PC i sei puntini compaiono col mouse accanto al titolo (`BlockHandle.kt`).
        if (onOpenBlockMenu != null && !isPc) {
            ToolbarIconButton(
                icon = Icons.Filled.DragIndicator,
                contentDescription = EditorStrings.blockOptions,
                onClick = onOpenBlockMenu
            )
        }
        Row(
            modifier = Modifier
                .weight(1f, fill = false)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(percent = 50)
                )
                .clickable { onOpenViews() }
                .padding(horizontal = pcOr(8.dp, 10.dp), vertical = pcOr(4.dp, 8.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val chipTint = MaterialTheme.colorScheme.onSurfaceVariant
            Icon(
                layout.icon(),
                contentDescription = null,
                modifier = Modifier.size(pcOr(16.dp, 18.dp)),
                tint = chipTint
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                viewName ?: layout.displayName(),
                style = MaterialTheme.typography.labelLarge,
                color = chipTint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            // La freccia dice che si può toccare: prima sembrava
            // un'etichetta e infatti non faceva niente.
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = DbStrings.views,
                modifier = Modifier.size(pcOr(16.dp, 18.dp)),
                tint = chipTint
            )
        }
        // **"+": una vista nuova**, subito a destra del nome, come su
        // Notion. Spento, non sparito, col database o la vista bloccati.
        ToolbarIconButton(
            icon = Icons.Filled.Add,
            contentDescription = DbStrings.addView,
            onClick = onAddView ?: {},
            tint = if (onAddView == null) lockedTint else MaterialTheme.colorScheme.onSurfaceVariant
        )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Il filtro sta prima dell'ordinamento perché è l'ordine in
            // cui agiscono: prima si sceglie **quali** pagine si
            // vedono, poi in che ordine.
            ToolbarIconButton(
                icon = Icons.Filled.FilterList,
                contentDescription = DbStrings.filter,
                onClick = onOpenFilter ?: {},
                // Acceso quando un filtro c'è: senza, una tabella con
                // metà delle pagine sembrerebbe una tabella che ha
                // perso delle pagine. Con la vista bloccata invece si
                // spegne, filtro o non filtro: lì non si cambia niente.
                tint = when {
                    onOpenFilter == null -> lockedTint
                    filterActive -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            ToolbarIconButton(
                icon = Icons.Filled.Sort,
                contentDescription = DbStrings.sort,
                onClick = onOpenSort ?: {},
                // Acceso quando un ordinamento c'è: altrimenti non si
                // saprebbe che le righe non sono più nell'ordine in
                // cui sono state create.
                tint = when {
                    onOpenSort == null -> lockedTint
                    sortActive -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            // La ricerca, accanto all'ordinamento come l'ha chiesta
            // l'utente (e come sta su Notion).
            ToolbarIconButton(
                icon = Icons.Filled.Search,
                contentDescription = Strings.search,
                onClick = onOpenSearch
            )
            // Aprire a schermo intero è una cosa che si fa spesso e di
            // fretta — la tabella dentro la pagina sta stretta — quindi
            // sta qui e non dentro una finestra. Cancellare no: quella
            // resta nelle impostazioni, dove non la si tocca per
            // sbaglio.
            if (onOpenFullPage != null) {
                ToolbarIconButton(
                    icon = Icons.Filled.OpenInFull,
                    contentDescription = DbStrings.openFullPage,
                    onClick = onOpenFullPage
                )
            }
            ToolbarIconButton(
                icon = Icons.Filled.Tune,
                contentDescription = DbStrings.databaseSettings,
                onClick = onOpenSettings
            )
        }
    }
}

/** Quale pagina della finestra dell'ordinamento si vede. */
private enum class SortSheetPage { MAIN, RULES, ADD }

/**
 * La finestra dell'ordinamento.
 *
 * **Le regole, quante se ne vogliono** (dal 17/10/2026, chiesto
 * dall'utente). In cima, "Sort rules" apre una finestra a parte con tutte le
 * regole, numerate nell'ordine in cui contano. "Add a sort rule" sta sempre
 * in alto, sopra l'elenco: aggiungerne tante non vuol dire scendere in
 * fondo. Ogni regola: un tocco ne rovescia il verso; il menu ⋮ la sposta su
 * o giù o la toglie. Prima c'erano solo la prima regola e "Then sort by"
 * (dal 05/10/2026).
 *
 * Sotto "Sort rules", le proprietà come prima: toccandone una diventa la
 * prima regola; accanto, il posto e il verso di quelle che sono già in una
 * regola ("1 ↑").
 *
 * Il verso si cambia toccando la regola, non con due voci separate per
 * crescente e decrescente: sono due stati della stessa cosa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortSheet(
    columns: List<DatabaseColumnEntity>,
    /** Le regole, nell'ordine in cui contano. */
    rules: List<SortRuleSpec>,
    onPickFirst: (String) -> Unit,
    onAdd: (String) -> Unit,
    onToggle: (String) -> Unit,
    onMove: (String, Int) -> Unit,
    onRemove: (String) -> Unit,
    onRemoveAll: () -> Unit,
    onDismiss: () -> Unit
) {
    var shown by remember { mutableStateOf(SortSheetPage.MAIN) }
    val rulesScroll = rememberScrollState()
    // Appena aggiunta una regola, l'elenco scende fino a lei: sta in fondo.
    var revealLast by remember { mutableStateOf(false) }
    LaunchedEffect(revealLast, rules.size, shown) {
        if (revealLast && shown == SortSheetPage.RULES) {
            withFrameNanos { }
            rulesScroll.animateScrollTo(rulesScroll.maxValue)
            revealLast = false
        }
    }

    val nameLabel = nameColumnLabel()
    fun nameOf(id: String): String = if (id == SORT_BY_NAME) {
        nameLabel
    } else {
        columns.find { it.id == id }?.name?.let(DbStrings::columnName) ?: nameLabel
    }

    /** "1 ↑": il posto e il verso della regola su questa proprietà, se c'è. */
    fun badgeOf(id: String): String {
        val index = rules.indexOfFirst { it.column == id }
        if (index < 0) return ""
        return "${index + 1} ${if (rules[index].descending) "↓" else "↑"}"
    }

    /** Il Nome per primo, poi le proprietà; quelle in `except` restano fuori. */
    @Composable
    fun PropertyList(except: Set<String>, badges: Boolean, onChoose: (String) -> Unit) {
        SheetGroup {
            var first = true
            // Il nome della pagina è una colonna della tabella ma non una
            // proprietà: va elencato a mano, e per primo, perché è quello
            // per cui si ordina più spesso.
            if (SORT_BY_NAME !in except) {
                SettingsRow(
                    icon = Icons.Filled.Notes,
                    label = nameLabel,
                    value = if (badges) badgeOf(SORT_BY_NAME) else "",
                    onClick = { onChoose(SORT_BY_NAME) }
                )
                first = false
            }
            columns.filter { it.id !in except }.forEach { column ->
                if (!first) HorizontalDivider()
                first = false
                SettingsRow(
                    icon = column.type.icon(),
                    label = DbStrings.columnName(column.name),
                    value = if (badges) badgeOf(column.id) else "",
                    onClick = { onChoose(column.id) }
                )
            }
        }
    }

    /** Una regola: il suo numero, il verso (tocco = lo rovescia) e il menu ⋮. */
    @Composable
    fun RuleRow(index: Int, rule: SortRuleSpec) {
        var menu by remember(rule.column) { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle(rule.column) }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(22.dp)
            )
            Icon(
                if (rule.descending) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.size(14.dp))
            Text(text = nameOf(rule.column), modifier = Modifier.weight(1f))
            Text(
                text = if (rule.descending) Strings.descending else Strings.ascending,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = DbStrings.moreOptions)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(EditorStrings.moveUp) },
                        leadingIcon = { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null) },
                        enabled = index > 0,
                        onClick = {
                            menu = false
                            onMove(rule.column, -1)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(EditorStrings.moveDown) },
                        leadingIcon = { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null) },
                        enabled = index < rules.lastIndex,
                        onClick = {
                            menu = false
                            onMove(rule.column, 1)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(DbStrings.removeSort, color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menu = false
                            onRemove(rule.column)
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun Caption(text: String) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    /** Il titolo della pagina della finestra; con [onBack], la freccia per tornare indietro. */
    @Composable
    fun Header(title: String, onBack: (() -> Unit)?) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = Strings.back)
                }
            }
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }

    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        when (shown) {
            SortSheetPage.MAIN -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Header(DbStrings.sort, onBack = null)
                Spacer(modifier = Modifier.size(12.dp))
                // In cima, le regole tutte insieme.
                SheetGroup {
                    SettingsRow(
                        icon = Icons.Filled.FormatListNumbered,
                        label = DbStrings.sortRules,
                        value = if (rules.isEmpty()) DbStrings.none else DbStrings.sortRuleCount(rules.size),
                        onClick = { shown = SortSheetPage.RULES }
                    )
                }
                Spacer(modifier = Modifier.size(16.dp))
                Caption(DbStrings.sortBy)
                Spacer(modifier = Modifier.size(8.dp))
                PropertyList(except = emptySet(), badges = true, onChoose = onPickFirst)
                Spacer(modifier = Modifier.size(24.dp))
            }

            // L'intestazione e "Add a sort rule" stanno ferme; scorre solo
            // l'elenco delle regole.
            SortSheetPage.RULES -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Header(DbStrings.sortRules) { shown = SortSheetPage.MAIN }
                Spacer(modifier = Modifier.size(4.dp))
                Caption(DbStrings.sortRulesNote)
                Spacer(modifier = Modifier.size(12.dp))
                // Quando tutte le proprietà sono già in una regola non c'è
                // altro da aggiungere.
                val used = rules.mapTo(HashSet()) { it.column }
                val canAdd = SORT_BY_NAME !in used || columns.any { it.id !in used }
                SheetGroup {
                    SettingsRow(
                        icon = Icons.Filled.Add,
                        label = DbStrings.addSortRule,
                        value = "",
                        onClick = { shown = SortSheetPage.ADD },
                        enabled = canAdd
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rulesScroll)
                ) {
                    if (rules.isEmpty()) {
                        Text(
                            text = DbStrings.noSortRules,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                        )
                    } else {
                        SheetGroup {
                            rules.forEachIndexed { index, rule ->
                                if (index > 0) HorizontalDivider()
                                key(rule.column) { RuleRow(index, rule) }
                            }
                        }
                        Spacer(modifier = Modifier.size(16.dp))
                        SheetGroup {
                            SettingsRow(
                                icon = Icons.Filled.Delete,
                                label = DbStrings.removeAllSorts,
                                value = "",
                                onClick = onRemoveAll,
                                danger = true
                            )
                        }
                    }
                    Spacer(modifier = Modifier.size(24.dp))
                }
            }

            SortSheetPage.ADD -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Header(DbStrings.addSortRule) { shown = SortSheetPage.RULES }
                Spacer(modifier = Modifier.size(12.dp))
                // Una regola nuova va in fondo, in verso crescente.
                PropertyList(except = rules.mapTo(HashSet()) { it.column }, badges = false) { id ->
                    onAdd(id)
                    revealLast = true
                    shown = SortSheetPage.RULES
                }
                Spacer(modifier = Modifier.size(24.dp))
            }
        }
    }
}

/**
 * Le proprietà su cui si può filtrare: tag, date, numeri e caselle da
 * spuntare.
 *
 * Restano fuori il testo libero, l'indirizzo, la mail e il telefono:
 * lì la domanda sarebbe "contiene queste lettere", cioè una ricerca, e
 * la ricerca è un'altra cosa che ancora non c'è. Restano fuori anche
 * "creata il" e "modificata il", che non sono celle ma proprietà della
 * riga.
 */
private fun DatabaseColumnEntity.isFilterable(): Boolean =
    type == ColumnType.SELECT ||
        type == ColumnType.MULTI_SELECT ||
        type == ColumnType.DATE ||
        type == ColumnType.NUMBER ||
        type == ColumnType.CHECKBOX

/**
 * Il filtro detto a parole: "Status: Done, In corso" oppure
 * "Date: 12 Jan 2026 → 15 Jan 2026".
 */
private fun filterSummary(column: DatabaseColumnEntity?, value: String): String {
    if (column == null || value.isBlank()) return DbStrings.none
    when (column.type) {
        ColumnType.DATE -> {
            val range = parseDateRange(value) ?: return DbStrings.none
            return "${DbStrings.columnName(column.name)}: ${range.label()}"
        }
        ColumnType.NUMBER -> {
            val parts = value.split(MULTI_VALUE_SEPARATOR)
            val min = parts.getOrNull(0).orEmpty()
            val max = parts.getOrNull(1).orEmpty()
            val text = when {
                min.isNotBlank() && max.isNotBlank() -> "${Formats.number(min)} – ${Formats.number(max)}"
                min.isNotBlank() -> "≥ ${Formats.number(min)}"
                max.isNotBlank() -> "≤ ${Formats.number(max)}"
                else -> return DbStrings.none
            }
            return "${DbStrings.columnName(column.name)}: $text"
        }
        ColumnType.CHECKBOX -> {
            val chosen = value.split(MULTI_VALUE_SEPARATOR).filter { it.isNotBlank() }
            if (chosen.isEmpty()) return DbStrings.none
            return "${DbStrings.columnName(column.name)}: " + chosen.joinToString(", ") {
                if (it == "true") DbStrings.checked else DbStrings.unchecked
            }
        }
        else -> {
            val labels = value.split(MULTI_VALUE_SEPARATOR).filter { it.isNotBlank() }
            if (labels.isEmpty()) return DbStrings.none
            return "${DbStrings.columnName(column.name)}: ${labels.joinToString(", ")}"
        }
    }
}

/**
 * La finestra del filtro: prima si sceglie la proprietà, poi i valori.
 *
 * È una sola schermata raggiunta da due posti — l'icona nella barra e
 * la voce nelle impostazioni — invece di due schermate gemelle: due
 * copie si sarebbero separate al primo cambiamento, e sarebbe stato il
 * tipo di differenza che si nota solo usandole.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    columns: List<DatabaseColumnEntity>,
    filterColumnId: String?,
    filterValue: String,
    /** Aperta dal menu di un'intestazione (dal 27/10/2026): parte dai valori di quella proprietà. */
    startColumnId: String? = null,
    onSetFilter: (String?, String?) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    // Quale proprietà si sta componendo. Parte da quella già filtrata,
    // così riaprendo il filtro si torna dove lo si era lasciato invece
    // che all'elenco delle proprietà.
    val filterable = columns.filter { it.isFilterable() }
    var chosenId by remember(filterColumnId, startColumnId) {
        mutableStateOf(startColumnId?.takeIf { id -> filterable.any { it.id == id } } ?: filterColumnId)
    }
    val chosen = columns.find { it.id == chosenId }

    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chosen != null) {
                    IconButton(onClick = { chosenId = null }) {
                        Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = Strings.back)
                    }
                }
                Text(
                    text = DbStrings.filter,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                // Il cestino c'è solo quando c'è davvero un filtro da
                // buttare: sempre visibile sarebbe un pulsante che per
                // lo più non fa niente.
                if (filterColumnId != null) {
                    IconButton(onClick = {
                        onClear()
                        chosenId = null
                    }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = DbStrings.removeFilter,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.size(12.dp))

            if (chosen == null) {
                if (filterable.isEmpty()) {
                    Text(
                        text = DbStrings.filterNeedsProperty,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    SheetGroup {
                        filterable.forEachIndexed { index, column ->
                            if (index > 0) HorizontalDivider()
                            SettingsRow(
                                icon = column.type.icon(),
                                label = DbStrings.columnName(column.name),
                                value = if (column.id == filterColumnId) "✓" else null,
                                onClick = { chosenId = column.id }
                            )
                        }
                    }
                }
            } else if (chosen.type == ColumnType.DATE) {
                DateFilterEditor(
                    value = if (chosen.id == filterColumnId) filterValue else "",
                    onChange = { onSetFilter(chosen.id, it) }
                )
            } else if (chosen.type == ColumnType.NUMBER) {
                NumberFilterEditor(
                    value = if (chosen.id == filterColumnId) filterValue else "",
                    onChange = { onSetFilter(chosen.id, it) }
                )
            } else if (chosen.type == ColumnType.CHECKBOX) {
                CheckboxFilterEditor(
                    value = if (chosen.id == filterColumnId) filterValue else "",
                    onChange = { onSetFilter(chosen.id, it) }
                )
            } else {
                TagFilterEditor(
                    column = chosen,
                    value = if (chosen.id == filterColumnId) filterValue else "",
                    onChange = { onSetFilter(chosen.id, it) }
                )
            }

            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

/**
 * La scelta dei tag da far passare: si spuntano, e se ne possono
 * spuntare più d'uno.
 */
@Composable
private fun TagFilterEditor(
    column: DatabaseColumnEntity,
    value: String,
    onChange: (String) -> Unit
) {
    val chosen = value.split(MULTI_VALUE_SEPARATOR).filter { it.isNotBlank() }.toSet()
    val options = parseSelectOptionsFull(column.optionsJson)

    if (options.isEmpty()) {
        Text(
            text = DbStrings.noOptionsYet(DbStrings.columnName(column.name)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        return
    }

    Text(
        text = DbStrings.pagesWithAny,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.size(8.dp))

    SheetGroup {
        options.forEachIndexed { index, option ->
            if (index > 0) HorizontalDivider()
            val isChosen = option.label in chosen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val updated = if (isChosen) chosen - option.label else chosen + option.label
                        onChange(updated.joinToString(MULTI_VALUE_SEPARATOR))
                    }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    TagChip(label = option.label, option = option)
                }
                if (isChosen) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * L'intervallo di numeri da far passare: da x a y.
 *
 * I due estremi sono facoltativi uno per volta — solo "da" vuol dire
 * "da lì in su", solo "a" vuol dire "fino a lì" — perché metà delle
 * domande vere sono aperte da un lato ("sopra i 100") e obbligare a
 * scrivere un secondo numero inventato le renderebbe sbagliate.
 */
@Composable
private fun NumberFilterEditor(
    value: String,
    onChange: (String) -> Unit
) {
    val parts = value.split(MULTI_VALUE_SEPARATOR)
    val min = parts.getOrNull(0).orEmpty()
    val max = parts.getOrNull(1).orEmpty()

    Text(
        text = DbStrings.pagesWithNumberRange,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.size(8.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        AutoCaps {
            OutlinedTextField(
                value = min,
                onValueChange = { onChange(it.filterNumeric() + MULTI_VALUE_SEPARATOR + max) },
                label = { Text(DbStrings.from) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        AutoCaps {
            OutlinedTextField(
                value = max,
                onValueChange = { onChange(min + MULTI_VALUE_SEPARATOR + it.filterNumeric()) },
                label = { Text(DbStrings.to) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(modifier = Modifier.size(8.dp))
    Text(
        text = DbStrings.numberRangeHint,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Tiene solo quello che può far parte di un numero.
 *
 * La tastiera numerica di Android ha lo stesso delle lettere su certi
 * modelli, e un incolla può portare qualunque cosa: un estremo che non
 * è un numero non filtrerebbe niente, e non si capirebbe perché.
 */
private fun String.filterNumeric(): String =
    filterIndexed { index, c ->
        c.isDigit() || c == '.' || (c == '-' && index == 0)
    }

/** Spuntate, non spuntate, o tutte e due. */
@Composable
private fun CheckboxFilterEditor(
    value: String,
    onChange: (String) -> Unit
) {
    val chosen = value.split(MULTI_VALUE_SEPARATOR).filter { it.isNotBlank() }.toSet()

    Text(
        text = DbStrings.pagesWithBox,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.size(8.dp))

    SheetGroup {
        listOf("true" to DbStrings.checked, "false" to DbStrings.unchecked).forEachIndexed { index, (key, label) ->
            if (index > 0) HorizontalDivider()
            val isChosen = key in chosen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val updated = if (isChosen) chosen - key else chosen + key
                        onChange(updated.joinToString(MULTI_VALUE_SEPARATOR))
                    }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (key == "true") {
                        Icons.Filled.CheckBox
                    } else {
                        Icons.Filled.CheckBoxOutlineBlank
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.size(14.dp))
                Text(label, modifier = Modifier.weight(1f))
                if (isChosen) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * La scelta dei giorni da far passare: un giorno solo, oppure da un
 * giorno a un altro.
 *
 * "Fino a" è facoltativo apposta: il caso normale è un giorno solo, e
 * chiedere due date per filtrarne una sarebbe un passaggio in più ogni
 * volta.
 */
@Composable
private fun DateFilterEditor(
    value: String,
    onChange: (String) -> Unit
) {
    val range = parseDateRange(value)
    var picking by remember { mutableStateOf<DateFilterEdge?>(null) }

    Text(
        text = DbStrings.pagesOnDays,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.size(8.dp))

    SheetGroup {
        SettingsRow(
            icon = Icons.Filled.CalendarToday,
            label = if (range?.end == null) DbStrings.day else DbStrings.from,
            value = range?.start?.toLocalDate()?.format(DATE_FORMAT) ?: DbStrings.pickADay,
            onClick = { picking = DateFilterEdge.START }
        )
        // La fine si può scegliere solo quando c'è un inizio: "fino al
        // 5" senza un "dal" non vuol dire niente.
        if (range != null) {
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    SettingsRow(
                        icon = Icons.Filled.Schedule,
                        label = DbStrings.to,
                        value = range.end?.toLocalDate()?.format(DATE_FORMAT) ?: DbStrings.sameDay,
                        onClick = { picking = DateFilterEdge.END }
                    )
                }
                if (range.end != null) {
                    IconButton(onClick = { onChange(encodeDateRange(range.start)) }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = DbStrings.filterSingleDay,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    picking?.let { edge ->
        val initial = when (edge) {
            DateFilterEdge.START -> range?.start?.toLocalDate() ?: LocalDate.now()
            DateFilterEdge.END -> range?.end?.toLocalDate()
                ?: range?.start?.toLocalDate()
                ?: LocalDate.now()
        }
        SamsungDateDialog(
            initial = initial,
            onPick = { day ->
                val millis = day.atStartOfDay(AppSettings.zoneId).toInstant().toEpochMilli()
                when (edge) {
                    DateFilterEdge.START -> {
                        // Spostando l'inizio oltre la fine, la fine
                        // sparisce invece di restare dietro: un
                        // intervallo al contrario non filtrerebbe più
                        // niente, e non si capirebbe perché.
                        val end = range?.end?.takeIf { it >= millis }
                        onChange(encodeDateRange(millis, end))
                    }
                    DateFilterEdge.END -> {
                        val start = range?.start ?: millis
                        onChange(encodeDateRange(start, millis.takeIf { it >= start }))
                    }
                }
            },
            onDismiss = { picking = null }
        )
    }
}

/** Quale dei due estremi del filtro per data si sta scegliendo. */
private enum class DateFilterEdge { START, END }

/**
 * La linea verticale fra due celle della tabella. Quella dopo la colonna
 * che si sta allargando (`afterKey`) è colorata, dall'intestazione
 * all'ultima riga: si vede dove finisce la colonna.
 */
@Composable
private fun CellSeparator(
    afterKey: String? = null,
    /** Con "Wrap cells" la riga può essere più alta: la linea la segue tutta. */
    stretch: Boolean = false
) {
    val resizing = afterKey != null && LocalColumnResize.current?.key == afterKey
    VerticalDivider(
        modifier = if (stretch) Modifier.fillMaxHeight() else Modifier.height(ROW_HEIGHT),
        color = if (resizing) MaterialTheme.colorScheme.primary else DividerDefaults.color
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NameHeaderCell(
    onCalculate: () -> Unit = {},
    /** Rinomina la proprietà principale (dal 18/10/2026); null se non si può. */
    onRename: ((String) -> Unit)? = null,
    /** Name si sposta anche lei (dal 29/09/2026); grigie in fondo, in testa e con "Lock view". */
    canMoveLeft: Boolean = false,
    canMoveRight: Boolean = false,
    onMove: (Int) -> Unit = {},
    /** Con "Lock view" le due voci della larghezza restano grigie. */
    canResize: Boolean = true,
    fit: Boolean = false,
    onResize: () -> Unit = {},
    onFitChange: (Boolean) -> Unit = {}
) {
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    val label = nameColumnLabel()
    val custom = LocalNameColumnTitle.current
    val icons = LocalPropertyIcons.current
    val actions = LocalPropertyMenuActions.current
    // Bloccato il database, il nome resta com'è.
    val rename = onRename?.takeIf { !LocalDatabaseLocked.current }
    val wrap = LocalWrapCells.current
    Box(modifier = if (wrap) Modifier.fillMaxHeight() else Modifier) {
    Row(
        modifier = Modifier
            .width(LocalTableWidths.current.name)
            .clipToBounds()
            .then(if (wrap) Modifier.heightIn(min = ROW_HEIGHT).fillMaxHeight() else Modifier.height(ROW_HEIGHT))
            // Un tocco apre il menu, come sulle altre proprietà (dal 27/10/2026);
            // tenere premuto non fa niente.
            .combinedClickable(onClick = { menuOpen = true }, onLongClick = {})
            .padding(horizontal = 12.dp)
            .cellWrapPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icons?.nameHidden != true) {
            Text(
                "Aa",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.size(8.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // Con "Wrap in the cell" va a capo come gli altri titoli; senza, tre puntini.
            maxLines = if (wrap) Int.MAX_VALUE else 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    }
    if (menuOpen) {
        fun pick(action: () -> Unit): () -> Unit = {
            menuOpen = false
            action()
        }
        PropertyMenuSheet(
            name = label,
            icon = { Text("Aa", style = MaterialTheme.typography.labelLarge) },
            onHeader = rename?.let { pick { renaming = true } },
            onDismiss = { menuOpen = false }
        ) {
            // **Rename** (dal 18/10/2026, chiesto dall'utente): il nome della
            // proprietà principale di questo database.
            PropertyMenuGroup(
                listOf({ OptionRow(Icons.Filled.Edit, DbStrings.renameProperty, enabled = rename != null, onClick = pick { renaming = true }) })
            )
            // **Name si sposta** (dal 29/09/2026: "permettimi di spostare
            // ovunque io voglia anche la colonna Name").
            PropertyMenuGroup(
                listOf(
                    { OptionRow(Icons.Filled.KeyboardArrowLeft, DbStrings.moveLeft, enabled = canMoveLeft, onClick = pick { onMove(-1) }) },
                    { OptionRow(Icons.Filled.KeyboardArrowRight, DbStrings.moveRight, enabled = canMoveRight, onClick = pick { onMove(1) }) }
                )
            )
            // "Sort" come per le altre proprietà (dal 27/10/2026; dal 02/11/2026
            // ordina per il nome, vedi `propertySortRows`); il filtro invece non
            // c'è: in cima al database Name non si filtra.
            if (actions != null) {
                PropertyMenuGroup(propertySortRows(SORT_BY_NAME, actions, onDone = { menuOpen = false }))
            }
            // Anche il nome ha il suo calcolo (quante pagine, quante col nome…),
            // e "Hide icon" per il suo "Aa" (dal 27/10/2026).
            PropertyMenuGroup(
                listOfNotNull(
                    { PropertyMenuChevronRow(Icons.Filled.Functions, DbStrings.calculate, onClick = pick(onCalculate)) },
                    icons?.let { i ->
                        {
                            OptionRow(
                                Icons.Filled.TextFields,
                                if (i.nameHidden) DbStrings.showIcon else DbStrings.hideIcon,
                                onClick = pick { i.setHidden(null, !i.nameHidden) }
                            )
                        }
                    }
                )
            )
            PropertyMenuGroup(columnWidthRows(canResize, fit, onResize = pick(onResize), onFitChange = { menuOpen = false; onFitChange(it) }))
        }
    }
    if (renaming && rename != null) {
        RenameMainPropertyDialog(
            current = custom.orEmpty(),
            onDismiss = { renaming = false },
            onSave = { title ->
                renaming = false
                rename(title)
            }
        )
    }
}

/**
 * **La finestrella per rinominare la proprietà principale** (dal
 * 18/10/2026): il nome di adesso, da cambiare; vuoto torna "Name".
 */
@Composable
private fun RenameMainPropertyDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(TextFieldValue(current, TextRange(current.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    FitAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(DbStrings.renameMainProperty) },
        text = {
            AutoCaps {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.copy(text = it.text.replace("\n", "")) },
                    singleLine = true,
                    placeholder = { Text(DbStrings.name) },
                    supportingText = { Text(DbStrings.mainPropertyHint) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSave(text.text) }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus)
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text.text) }) { Text(EditorStrings.save) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}

/**
 * "Resize column" e "Fit to content", le due voci della larghezza: nel menu
 * di ogni colonna e in quello di Name. Con "Lock view" restano grigie.
 */
private fun columnWidthRows(
    canResize: Boolean,
    fit: Boolean,
    onResize: () -> Unit,
    onFitChange: (Boolean) -> Unit
): List<@Composable () -> Unit> = listOf(
    { OptionRow(icon = Icons.Filled.SettingsEthernet, label = DbStrings.resizeColumn, enabled = canResize, onClick = onResize) },
    {
        OptionRow(
            icon = Icons.Filled.FitScreen,
            label = DbStrings.fitToContent,
            enabled = canResize,
            onClick = { onFitChange(!fit) },
            trailing = {
                Switch(
                    checked = fit,
                    onCheckedChange = null,
                    enabled = canResize,
                    modifier = Modifier.scale(0.75f)
                )
            }
        )
    }
)

/**
 * L'intestazione di una colonna. **Toccarla apre il suo menu** (dal
 * 27/10/2026, chiesto dall'utente: "tenere premuto su una proprietà non farà
 * più nulla"), una finestra a gruppi come su Notion (`PropertyMenu.kt`). Le
 * voci, nell'ordine di sempre: "Move to left" e "Move to right"; "Edit
 * property" e, sotto, "Change type"; "Filter" e "Sort", le stesse finestre
 * delle icone in cima; "Activate all" e "Deactivate all" (solo le caselle);
 * dove sta il contenuto delle celle; "Resize column" e "Fit to content";
 * "Hide", "Hide icon" e "Calculate".
 *
 * Gli spostamenti che non si possono fare restano **grigi** invece di
 * sparire: la prima colonna non va più a sinistra, l'ultima non va più a
 * destra, e con "Lock view" non si sposta niente. Name conta come le altre:
 * dal 29/09/2026 si sposta anche lei, e le colonne le passano accanto. Una voce che
 * sparisce e ricompare a seconda della colonna farebbe cambiare posto
 * alle altre sotto il dito.
 *
 * Nascondere sta qui e non solo nelle impostazioni perché è una cosa
 * che si decide guardando la tabella — "questa non mi serve adesso" —
 * e cercarla in un menu a tre livelli vorrebbe dire non usarla mai.
 * Rimostrarla invece sta nelle impostazioni: una colonna nascosta non
 * ha più un'intestazione da toccare.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PropertyHeaderCell(
    column: DatabaseColumnEntity,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onMove: (Int) -> Unit,
    onHide: () -> Unit,
    onAlign: (CellAlignment) -> Unit,
    onCalculate: () -> Unit = {},
    /** Solo per le caselle: "Activate all" (true) e "Deactivate all" (false). */
    onSetAllChecked: (Boolean) -> Unit = {},
    /** Con "Lock view" le due voci della larghezza restano grigie. */
    canResize: Boolean = true,
    fit: Boolean = false,
    onResize: () -> Unit = {},
    onFitChange: (Boolean) -> Unit = {}
) {
    var menuOpen by remember(column.id) { mutableStateOf(false) }
    var confirmDelete by remember(column.id) { mutableStateOf(false) }
    val dataLocked = LocalDatabaseLocked.current
    val icons = LocalPropertyIcons.current
    val actions = LocalPropertyMenuActions.current

    val wrap = LocalWrapCells.current
    Box(modifier = if (wrap) Modifier.fillMaxHeight() else Modifier) {
        Row(
            modifier = Modifier
                .width(LocalTableWidths.current.of(column))
                .clipToBounds()
                .then(if (wrap) Modifier.heightIn(min = ROW_HEIGHT).fillMaxHeight() else Modifier.height(ROW_HEIGHT))
                // Il tocco apre il menu; tenere premuto non fa niente (dal 27/10/2026),
                // e non conta nemmeno come un tocco quando si alza il dito.
                .combinedClickable(onClick = { menuOpen = true }, onLongClick = {})
                .padding(horizontal = 12.dp)
                .cellWrapPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "Hide icon" (dal 27/10/2026): senza l'icona il nome parte dal bordo.
            if (!column.hideIcon) {
                Icon(
                    column.type.icon(),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
            // Con "Wrap in the cell" il titolo va a capo invece di finire oltre il bordo.
            Text(
                text = DbStrings.columnName(column.name),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = cellMaxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (menuOpen) {
        // Ogni voce chiude il menu e poi fa la sua cosa.
        fun pick(action: () -> Unit): () -> Unit = {
            menuOpen = false
            action()
        }
        PropertyMenuSheet(
            name = DbStrings.columnName(column.name),
            icon = { Icon(column.type.icon(), contentDescription = null, modifier = Modifier.size(20.dp)) },
            onHeader = null,
            onDismiss = { menuOpen = false },
            onRename = actions?.rename?.let { rename -> { name: String -> rename(column, name) } }
        ) {
            // Gli spostamenti **in cima**: l'ha chiesto l'utente, perché
            // spostare è la cosa per cui si apre il menu di un'intestazione.
            PropertyMenuGroup(
                listOf(
                    { OptionRow(Icons.Filled.KeyboardArrowLeft, DbStrings.moveLeft, enabled = canMoveLeft, onClick = pick { onMove(-1) }) },
                    { OptionRow(Icons.Filled.KeyboardArrowRight, DbStrings.moveRight, enabled = canMoveRight, onClick = pick { onMove(1) }) }
                )
            )
            // "Change type": i tipi, e sotto quello di adesso le sue impostazioni
            // (Number, Duration, Relation, Rollup). "Edit property" non c'è più
            // (27/10/2026, chiesto dall'utente): il nome si scrive qui in cima.
            if (actions != null) {
                PropertyMenuGroup(
                    listOf({ PropertyMenuChevronRow(Icons.Filled.SwapHoriz, DbStrings.changeType, onClick = pick { actions.changeType(column) }) })
                )
            }
            // **Le azioni di un bottone** (dal 27/10/2026). Un bottone non ha un
            // valore: non si filtra, non si ordina e non si calcola.
            if (column.type == ColumnType.BUTTON && actions != null) {
                PropertyMenuGroup(
                    listOf({
                        PropertyMenuChevronRow(
                            Icons.Filled.TouchApp,
                            ButtonStrings.editActions,
                            enabled = actions.editButton != null,
                            onClick = pick { actions.editButton?.invoke(column) }
                        )
                    })
                )
            }
            // **La formula** (dal 28/10/2026): la sua finestra, con l'anteprima sulla prima riga.
            if (column.type == ColumnType.FORMULA && actions != null) {
                val editFormula = LocalFormulaEditor.current
                PropertyMenuGroup(
                    listOf({
                        PropertyMenuChevronRow(
                            Icons.Filled.Functions,
                            FormulaStrings.editFormula,
                            enabled = editFormula != null,
                            onClick = pick { editFormula?.invoke(column, null) }
                        )
                    })
                )
            }
            // Il filtro è la stessa finestra dell'icona in cima al database, e
            // parte da questa proprietà; "Sort" ordina per lei, coi due versi qui
            // sotto (dal 02/11/2026, vedi `propertySortRows`). Con "Lock view"
            // grigie, come le icone.
            if (actions != null && column.type != ColumnType.BUTTON) {
                PropertyMenuGroup(
                    listOfNotNull<@Composable () -> Unit>(
                        // Una formula si ordina ma non si filtra: il filtro sceglie fra valori scritti.
                        if (column.type == ColumnType.FORMULA) null else {
                            {
                                PropertyMenuChevronRow(
                                    Icons.Filled.FilterList,
                                    DbStrings.filter,
                                    enabled = actions.filter != null,
                                    onClick = pick { actions.filter?.invoke(column.id) }
                                )
                            }
                        }
                    ) + propertySortRows(column.id, actions, onDone = { menuOpen = false })
                )
            }
            // **Solo per le caselle** (dal 29/09/2026, chiesto dall'utente):
            // spuntarle o toglierle tutte in un colpo, sulle righe che si
            // vedono. Con il database bloccato restano grigie, come le celle.
            if (column.type == ColumnType.CHECKBOX) {
                PropertyMenuGroup(
                    listOf(
                        { OptionRow(Icons.Filled.CheckBox, DbStrings.activateAll, enabled = !dataLocked, onClick = pick { onSetAllChecked(true) }) },
                        { OptionRow(Icons.Filled.CheckBoxOutlineBlank, DbStrings.deactivateAll, enabled = !dataLocked, onClick = pick { onSetAllChecked(false) }) }
                    )
                )
            }
            // **Dove sta il contenuto delle celle** (dal 26/09/2026, per ogni
            // tipo; prima solo le caselle si centravano): a sinistra, al
            // centro o a destra. L'intestazione resta sempre a sinistra.
            PropertyMenuGroup(
                listOf(
                    Triple(CellAlignment.LEFT, DbStrings.alignLeft, Icons.Filled.FormatAlignLeft),
                    Triple(CellAlignment.CENTER, DbStrings.center, Icons.Filled.FormatAlignCenter),
                    Triple(CellAlignment.RIGHT, DbStrings.alignRight, Icons.Filled.FormatAlignRight)
                ).map { (alignment, label, icon) ->
                    {
                        OptionRow(
                            icon = icon,
                            label = label,
                            onClick = pick { onAlign(alignment) },
                            trailing = if (column.cellAlignment == alignment) {
                                { Icon(Icons.Filled.Check, contentDescription = DbStrings.selected, tint = MaterialTheme.colorScheme.primary) }
                            } else {
                                null
                            }
                        )
                    }
                }
            )
            PropertyMenuGroup(columnWidthRows(canResize, fit, onResize = pick(onResize), onFitChange = { menuOpen = false; onFitChange(it) }))
            PropertyMenuGroup(
                listOfNotNull(
                    { OptionRow(Icons.Filled.VisibilityOff, DbStrings.hide, onClick = pick(onHide)) },
                    // **"Hide icon"** (dal 27/10/2026, chiesto dall'utente): l'icona
                    // accanto al nome se ne va, o torna con "Show icon". Accanto, la
                    // sua stessa icona.
                    icons?.let { i ->
                        {
                            OptionRow(
                                column.type.icon(),
                                if (column.hideIcon) DbStrings.showIcon else DbStrings.hideIcon,
                                onClick = pick { i.setHidden(column.id, !column.hideIcon) }
                            )
                        }
                    },
                    // Il calcolo in fondo alla colonna (dal 26/09/2026).
                    column.takeIf { it.type != ColumnType.BUTTON }?.let {
                        { PropertyMenuChevronRow(Icons.Filled.Functions, DbStrings.calculate, onClick = pick(onCalculate)) }
                    }
                )
            )
            // In fondo, in rosso, come su Notion: prima stava dentro "Edit property".
            if (actions != null) {
                PropertyMenuGroup(
                    listOf({
                        OptionRow(
                            Icons.Filled.Delete,
                            DbStrings.deleteProperty,
                            enabled = actions.delete != null,
                            tint = MaterialTheme.colorScheme.error,
                            onClick = pick { confirmDelete = true }
                        )
                    })
                )
            }
        }
    }

    // Con la proprietà se ne vanno i suoi valori in tutte le righe: si chiede.
    if (confirmDelete) {
        FitAlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(DbStrings.deletePropertyTitle(DbStrings.columnName(column.name))) },
            text = { Text(DbStrings.deletePropertyNote) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    actions?.delete?.invoke(column)
                }) { Text(Strings.delete, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(Strings.cancel) } }
        )
    }
}

@Composable
private fun AddPropertyCell(
    onClick: () -> Unit,
    /**
     * **I tre puntini a destra del "+"** (dal 11/11/2026, chiesto dall'utente,
     * come su Notion): aprono "Property visibility", dove si mostrano e si
     * nascondono le colonne. Null: niente puntini.
     */
    onShowProperties: (() -> Unit)? = null
) {
    Row(modifier = Modifier.width(ADD_PROPERTY_WIDTH).height(ROW_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = DbStrings.addProperty,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onShowProperties != null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onShowProperties() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.MoreHoriz,
                    contentDescription = DbStrings.propertyVisibility,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * La cella del nome: il titolo della riga, più il pulsante OPEN che
 * apre la riga come pagina vera. Il tocco prolungato apre le azioni
 * della riga — su Notion è lo stesso gesto.
 *
 * Il nome è testo semplice finché non lo si tocca, e solo allora
 * diventa un campo di scrittura. Non è un vezzo: un campo di testo si
 * prende lui il tocco prolungato per selezionare le parole, quindi
 * finché ce n'era sempre uno il gesto non arrivava mai qui e comparivano
 * invece i pallini della selezione. Così tocco e tocco prolungato sono
 * gestiti da un unico rilevatore e non possono contendersi il gesto.
 */
@Composable
private fun NameCell(
    row: DatabaseRowEntity,
    onTitleChange: (String) -> Unit,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    /** Si comincia (true) o si finisce (false) di scrivere il nome: vedi `DatabaseViewModel.startTitleEdit`. */
    onEditingChange: (Boolean) -> Unit = {},
    /** Le sotto-righe (dal 29/09/2026): vedi `TableBodyRow`. */
    depth: Int = 0,
    hasSubRows: Boolean = false,
    toggleSlot: Boolean = false,
    onToggleSubRows: () -> Unit = {}
) {
    var editing by remember(row.id) { mutableStateOf(false) }
    var localTitle by remember(row.id) { mutableStateOf(row.title) }
    // **Il cursore in fondo al nome** (dal 26/09/2026, chiesto
    // dall'utente): toccando la cella si continua o si cancella il nome, e
    // il campo apriva col cursore all'inizio.
    var fieldValue by remember(row.id) { mutableStateOf(TextFieldValue(row.title, TextRange(row.title.length))) }
    DisposableEffect(editing) {
        // Il valore di adesso: quando l'effetto si chiude `editing` è già
        // cambiato, e leggerlo lì direbbe sempre "non si scrive più".
        val active = editing
        if (active) onEditingChange(true)
        onDispose { if (active) onEditingChange(false) }
    }
    val focusRequester = remember { FocusRequester() }
    // Il campo appena creato non ha ancora il fuoco, e `onFocusChanged`
    // scatta subito dicendo proprio quello: senza questa memoria la
    // cella si richiuderebbe nell'istante in cui la si apre, e il tocco
    // sembrerebbe non fare niente.
    var everFocused by remember(row.id) { mutableStateOf(false) }

    // Una sotto-riga appena creata: il nome si apre da solo per scriverlo.
    val editRequest = LocalTitleEditRequest.current
    LaunchedEffect(editRequest.rowId) {
        if (editRequest.rowId == row.id) {
            fieldValue = TextFieldValue(localTitle, TextRange(localTitle.length))
            everFocused = false
            editing = true
            editRequest.consume()
        }
    }

    // Se il titolo cambia da fuori (un'altra schermata, Annulla) mentre
    // non lo stiamo scrivendo noi, adeguiamoci.
    LaunchedEffect(row.title) {
        if (!editing) localTitle = row.title
    }

    val wrap = LocalWrapCells.current
    // **Il nome tagliato coi tre puntini** (dal 30/09/2026): toccandolo si
    // apre lì sopra il riquadro grigio col nome intero (`FullTextPopup`).
    var cut by remember(row.id) { mutableStateOf(false) }
    var expanded by remember(row.id) { mutableStateOf(false) }
    DisposableEffect(expanded) {
        val active = expanded
        if (active) onEditingChange(true)
        onDispose { if (active) onEditingChange(false) }
    }
    // **Sul PC OPEN si vede solo col mouse sopra la cella**, come in Notion
    // (dal 11/11/2026, quarta fase dell'aspetto da Notion): il suo posto resta.
    val nameHover = remember { MutableInteractionSource() }
    val showOpen = !isPc || nameHover.collectIsHoveredAsState().value
    Row(
        modifier = Modifier
            .width(LocalTableWidths.current.name)
            .clipToBounds()
            .then(if (wrap) Modifier.heightIn(min = ROW_HEIGHT).fillMaxHeight() else Modifier.height(ROW_HEIGHT))
            .hoverable(nameHover),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // L'icona della pagina, se c'è, sta fuori dal campo del nome: resta
        // al suo posto anche mentre il nome si scrive.
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // **Le sotto-righe**: rientrate di un gradino per livello; la
            // freccia, sulle righe che ne hanno, le apre e le chiude. Il
            // posto della freccia c'è su ogni riga, così i nomi restano in
            // colonna.
            if (toggleSlot) {
                Spacer(modifier = Modifier.width(SUB_ROW_INDENT * depth))
                Box(
                    modifier = Modifier
                        .width(SUB_ROW_ARROW_SLOT)
                        .fillMaxHeight()
                        .let { base -> if (hasSubRows) base.clickable { onToggleSubRows() } else base },
                    contentAlignment = Alignment.Center
                ) {
                    if (hasSubRows) {
                        Icon(
                            if (row.subRowsCollapsed) Icons.Filled.KeyboardArrowRight else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (row.subRowsCollapsed) DbStrings.showSubRows else DbStrings.hideSubRows,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            RowIconBadge(row.id, ROW_ICON_SIZE)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (editing) {
                    AutoCaps {
                        BasicTextField(
                            value = fieldValue,
                            onValueChange = { newValue ->
                                // Niente a capo nei nomi delle righe. `singleLine`
                                // fa già mostrare alla tastiera "fine" al posto di
                                // invio, ma non copre l'incolla di un testo che
                                // contiene a capo: quelli li togliamo qui.
                                val singleLineTitle = newValue.text.replace("\n", "")
                                fieldValue = if (singleLineTitle == newValue.text) {
                                    newValue
                                } else {
                                    TextFieldValue(singleLineTitle, TextRange(singleLineTitle.length))
                                }
                                // Spostare il cursore non cambia il nome: si scrive solo se cambia.
                                if (singleLineTitle != localTitle) {
                                    localTitle = singleLineTitle
                                    onTitleChange(singleLineTitle)
                                }
                            },
                            visualTransformation = emojiTransformation,
                            singleLine = !wrap,
                            // Database bloccato: il nome della riga si legge e
                            // si seleziona, non si riscrive.
                            readOnly = LocalDatabaseLocked.current,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { editing = false }),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier
                                .fillMaxWidth()
                                .cellWrapPadding()
                                .focusRequester(focusRequester)
                                .markFocusChanges()
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        everFocused = true
                                    } else if (everFocused) {
                                        editing = false
                                    }
                                }
                        )
                    }
                    LaunchedEffect(Unit) {
                        everFocused = false
                        focusRequester.requestFocus()
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onRightClick { onLongPress() }
                            .pointerInput(row.id, wrap) {
                                detectTapGestures(
                                    onTap = {
                                        if (cut && !wrap) {
                                            expanded = true
                                            return@detectTapGestures
                                        }
                                        fieldValue = TextFieldValue(localTitle, TextRange(localTitle.length))
                                        // **Da capo anche la seconda volta** (26/09/2026): il
                                        // "ha già avuto il fuoco" della volta prima restava
                                        // acceso, e il campo si richiudeva nell'istante in cui
                                        // si riapriva — il nome di una riga si riscriveva una
                                        // volta sola. Trovato provando l'ordinamento.
                                        everFocused = false
                                        editing = true
                                    },
                                    onLongPress = { onLongPress() }
                                )
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = highlightedTitle(localTitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (localTitle.isBlank()) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            },
                            maxLines = cellMaxLines,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { cut = it.isCut() },
                            modifier = Modifier.cellWrapPadding()
                        )
                    }
                }
            }
        }
        // Il riquadro grigio sta qui, nella riga di tutta la cella: così è
        // centrato sulla cella del nome, OPEN compreso.
        if (expanded) {
            FullTextPopup(
                value = localTitle,
                onValueChange = {
                    localTitle = it
                    onTitleChange(it)
                },
                cellWidth = LocalTableWidths.current.name,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                onDismiss = { expanded = false }
            )
        }
        // Niente OPEN in un database semplice: la riga non ha una pagina
        // da aprire. Il nome si scrive qui, toccandolo, e il resto dalla
        // scheda che si apre tenendolo premuto.
        if (!LocalSimpleDatabase.current) {
            Text(
                text = DbStrings.open,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .alpha(if (showOpen) 1f else 0f)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(6.dp)
                    )
                    .clickable(enabled = showOpen) { onOpen() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/** La cella nella tabella: il contenuto, dentro un riquadro della misura della colonna. */
@Composable
private fun CellEditor(
    row: DatabaseRowEntity,
    column: DatabaseColumnEntity,
    value: String,
    onValueChange: (String) -> Unit,
    onAddOption: (String) -> Unit,
    onRenameOption: (String, String, String) -> Unit,
    onSetOptionColor: (String, String, String) -> Unit,
    onDeleteOption: (String, String) -> Unit,
    onMoveOptions: (String, List<String>) -> Unit,
    onSetOptionSecondColor: (String, String, String?, Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .width(LocalTableWidths.current.of(column))
            .clipToBounds()
            .then(if (LocalWrapCells.current) Modifier.heightIn(min = ROW_HEIGHT).fillMaxHeight() else Modifier.height(ROW_HEIGHT))
            // **La cella si mangia i tocchi che non colpiscono niente.**
            // Senza, il tocco sullo spazio vuoto accanto a una casella
            // da spuntare attraversava la tabella e arrivava alla
            // pagina che le sta dietro, che lo intende come "scrivi
            // qui": il cursore finiva sotto al database e si apriva la
            // tastiera. I figli vengono serviti per primi, quindi la
            // casella continua a rispondere normalmente.
            .pointerInput(Unit) { detectTapGestures { } }
            // "Color cell": il colore riempie **tutta** la cella, bordi
            // compresi, non solo lo spazio del testo.
            .then(
                if (column.type == ColumnType.COLOR) {
                    hexToColor(parseColorCell(value).color)?.let { Modifier.background(it) } ?: Modifier
                } else {
                    Modifier
                }
            ),
        contentAlignment = column.cellAlignment.box
    ) {
        // Il margine laterale lo mette il contenuto, non questo
        // riquadro: per la data deve stare **dentro** la zona toccabile
        // (vedi CellContent), altrimenti i dodici punti ai lati non
        // aprono il calendario.
        // Dove sta il contenuto: lo sanno anche i pezzi larghi quanto la
        // cella (il testo, i tag, la data), che si allineano da sé.
        CompositionLocalProvider(LocalCellAlignment provides column.cellAlignment) {
        CellContent(
            row = row,
            column = column,
            value = value,
            cellPadding = 12.dp,
            onValueChange = onValueChange,
            onAddOption = onAddOption,
            onRenameOption = onRenameOption,
            onSetOptionColor = onSetOptionColor,
            onDeleteOption = onDeleteOption,
            onMoveOptions = onMoveOptions,
            onSetOptionSecondColor = onSetOptionSecondColor
        )
        }
    }
}

/**
 * Il contenuto di una cella, che cambia in base al tipo della colonna.
 * Non impone una misura: lo usano sia la tabella (dentro un riquadro
 * della larghezza della colonna) sia la finestra delle azioni di una
 * riga, dove le proprietà stanno una sotto l'altra a tutta larghezza.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CellContent(
    row: DatabaseRowEntity,
    column: DatabaseColumnEntity,
    value: String,
    onValueChange: (String) -> Unit,
    onAddOption: (String) -> Unit,
    onRenameOption: (String, String, String) -> Unit,
    onSetOptionColor: (String, String, String) -> Unit,
    onDeleteOption: (String, String) -> Unit,
    onMoveOptions: (String, List<String>) -> Unit,
    onSetOptionSecondColor: (String, String, String?, Boolean) -> Unit,
    /** Il margine laterale dentro la cella: nella tabella c'è, nella finestra delle proprietà no. */
    cellPadding: Dp = 0.dp
) {
    val cellKey = "${row.id}:${column.id}"

    Box(
        // La data e la cella colorata se lo mettono da sé, dentro la
        // propria zona toccabile.
        modifier = if (column.type == ColumnType.DATE || column.type == ColumnType.COLOR ||
            column.type == ColumnType.PAGE_LINK || column.type == ColumnType.IMAGE ||
            column.type == ColumnType.DURATION ||
            column.type == ColumnType.RELATION || column.type == ColumnType.ROLLUP ||
            column.type == ColumnType.FORMULA
        ) {
            Modifier
        } else {
            Modifier.padding(horizontal = cellPadding)
        },
        contentAlignment = Alignment.CenterStart
    ) {
        when (column.type) {
            ColumnType.CHECKBOX -> {
                Checkbox(
                    checked = value == "true",
                    onCheckedChange = { onValueChange(it.toString()) },
                    modifier = Modifier.size(24.dp)
                )
            }

            ColumnType.SELECT -> TagCell(
                column = column,
                value = value,
                allowMultiple = false,
                onValueChange = onValueChange,
                onAddOption = onAddOption,
                onRenameOption = onRenameOption,
                onSetOptionColor = onSetOptionColor,
                onDeleteOption = onDeleteOption,
                onMoveOptions = onMoveOptions,
                onSetOptionSecondColor = onSetOptionSecondColor
            )

            ColumnType.MULTI_SELECT -> TagCell(
                column = column,
                value = value,
                allowMultiple = true,
                onValueChange = onValueChange,
                onAddOption = onAddOption,
                onRenameOption = onRenameOption,
                onSetOptionColor = onSetOptionColor,
                onDeleteOption = onDeleteOption,
                onMoveOptions = onMoveOptions,
                onSetOptionSecondColor = onSetOptionSecondColor
            )

            ColumnType.DATE -> DateCell(
                value = value,
                onValueChange = onValueChange,
                contentPadding = cellPadding
            )

            ColumnType.PAGE_LINK -> PageLinkCell(
                value = value,
                onValueChange = onValueChange,
                contentPadding = cellPadding,
                alignment = LocalCellAlignment.current.box
            )

            // "Relation" e "Rollup" (dal 21/10/2026): vedi `RelationCell.kt`.
            ColumnType.RELATION -> RelationCell(
                column = column,
                value = value,
                onValueChange = onValueChange,
                contentPadding = cellPadding,
                alignment = LocalCellAlignment.current.box
            )

            ColumnType.ROLLUP -> RollupCell(
                value = value,
                contentPadding = cellPadding,
                alignment = LocalCellAlignment.current.box
            )

            // "Button" (dal 27/10/2026): vedi `ButtonUi.kt`.
            ColumnType.BUTTON -> ButtonCell(column = column, row = row)

            // "Formula" (dal 28/10/2026): vedi `FormulaUi.kt`.
            ColumnType.FORMULA -> FormulaCell(
                column = column,
                row = row,
                text = value,
                contentPadding = cellPadding,
                alignment = LocalCellAlignment.current.box
            )

            ColumnType.DURATION -> DurationCell(
                value = value,
                onValueChange = onValueChange,
                title = DbStrings.columnName(column.name),
                format = durationFormatOf(column.durationFormat),
                contentPadding = cellPadding
            )

            ColumnType.IMAGE -> ImageCell(
                value = value,
                onValueChange = onValueChange,
                title = DbStrings.columnName(column.name),
                inTable = cellPadding > 0.dp
            )

            ColumnType.COLOR -> ColorCell(
                columnName = DbStrings.columnName(column.name),
                value = value,
                onValueChange = onValueChange,
                // Nella tabella il colore lo stende la cella intera
                // (`CellEditor`); nella scheda di una riga è un riquadro.
                inTable = cellPadding > 0.dp,
                contentPadding = cellPadding
            )

            ColumnType.CREATED_TIME, ColumnType.LAST_EDITED_TIME -> {
                // Calcolate dalla riga stessa, non modificabili a mano.
                // Una riga mai toccata dopo la creazione, o creata prima
                // che esistesse questa colonna, mostra la data di
                // creazione invece di restare vuota.
                val instant = if (column.type == ColumnType.CREATED_TIME) {
                    row.createdAt
                } else {
                    row.updatedAt ?: row.createdAt
                }
                Text(
                    text = Instant.ofEpochMilli(instant)
                        .atZone(AppSettings.zoneId)
                        .format(DATE_TIME_FORMAT),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = cellMaxLines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.cellWrapPadding()
                )
            }

            else -> {
                val linkLabels = LocalCellLinkLabels.current
                // TEXT, NUMBER, URL, EMAIL, PHONE: un campo libero, che
                // cambia solo il tipo di tastiera proposta. Testo semplice
                // finché non lo si tocca (dal 30/09/2026): vedi `TextLikeCell`.
                TextLikeCell(
                    value = value,
                    onValueChange = onValueChange,
                    // Anche qui l'invio non avrebbe niente da fare: una
                    // cella sta su una riga sola.
                    keyboardOptions = KeyboardOptions(
                        keyboardType = column.type.keyboardType(),
                        capitalization = column.type.capitalization(),
                        imeAction = ImeAction.Done
                    ),
                    // Un numero si legge nel formato delle impostazioni e della
                    // colonna (1.000.000 o 1,000,000), ma **si scrive grezzo**.
                    shown = if (column.type == ColumnType.NUMBER) {
                        { raw -> formatNumberValue(raw, column) }
                    } else {
                        { raw -> raw }
                    },
                    // **Barra o anello** (dal 26/09/2026): finché non la si tocca,
                    // la cella di un numero li disegna al posto del testo.
                    idleVisual = if (column.type == ColumnType.NUMBER && column.numberDisplayKind != NumberDisplay.NUMBER) {
                        { raw -> NumberVisual(column, raw) }
                    } else {
                        null
                    },
                    // **Un URL si apre** (dal 05/10/2026): tocco = apri, tocco
                    // lungo = modifica, copia, incolla. Vedi `TextLikeCell`.
                    isLink = column.type == ColumnType.URL,
                    // E si mostra come si è scelto (dal 27/10/2026, "Show as…").
                    linkLabel = linkLabels?.labels?.get(row.id to column.id)?.takeIf { column.type == ColumnType.URL },
                    onLinkLabel = linkLabels?.takeIf { column.type == ColumnType.URL }?.let { labels -> { label -> labels.set(row, column, label) } },
                    cellKey = cellKey
                )
            }
        }
    }
}

/**
 * Mostra un numero nel formato delle impostazioni senza cambiare il valore
 * salvato. Si usa solo quando la cella non ha il cursore, quindi le
 * posizioni non contano: tutto si ancora in fondo.
 */
private class NumberDisplayTransformation(private val settings: NumberSettings) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // Il formato della colonna (dal 26/09/2026): cifre decimali, percentuale o valuta.
        val shown = formatNumberValue(text.text, settings)
        return TransformedText(
            AnnotatedString(shown),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = shown.length
                override fun transformedToOriginal(offset: Int): Int = text.length
            }
        )
    }
}

/**
 * Una cella a tag: mostra le etichette scelte come pastiglie colorate e,
 * toccandola, apre la finestra per sceglierne o crearne.
 *
 * Una cella vuota resta vuota, senza trattini o altri segnaposto: lo
 * spazio libero si capisce da solo, e un simbolo lì fa credere che ci
 * sia già scritto qualcosa.
 *
 * `allowMultiple` distingue Select da Multi-select: con la selezione
 * singola scegliere chiude la finestra, con quella multipla resta
 * aperta perché di solito se ne spuntano più di uno.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagCell(
    column: DatabaseColumnEntity,
    value: String,
    allowMultiple: Boolean,
    onValueChange: (String) -> Unit,
    onAddOption: (String) -> Unit,
    onRenameOption: (String, String, String) -> Unit,
    onSetOptionColor: (String, String, String) -> Unit,
    onDeleteOption: (String, String) -> Unit,
    onMoveOptions: (String, List<String>) -> Unit,
    onSetOptionSecondColor: (String, String, String?, Boolean) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val options = parseSelectOptionsFull(column.optionsJson)
    val columnId = column.id
    val selected = if (allowMultiple) {
        value.split(MULTI_VALUE_SEPARATOR).filter { it.isNotBlank() }
    } else {
        listOfNotNull(value.takeIf { it.isNotBlank() })
    }

    if (LocalWrapCells.current) {
        // **"Wrap cells"** (dal 30/09/2026): i tag che non ci stanno vanno a capo.
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSheet = true }
                .cellWrapPadding(),
            horizontalArrangement = Arrangement.spacedBy(4.dp, LocalCellAlignment.current.horizontal),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            selected.forEach { label ->
                TagChip(label = label, option = options.find { it.label == label })
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // **Tutta la cella si tocca, anche vuota** (dal 11/11/2026, bug trovato
                // dall'utente): alta quanto i tag, da vuota era alta zero e il clic non
                // apriva niente. Come la data; il minimo vale dove l'altezza non è fissata.
                .fillMaxHeight()
                .heightIn(min = 18.dp)
                .clickable { showSheet = true },
            horizontalArrangement = LocalCellAlignment.current.arrangement,
            verticalAlignment = Alignment.CenterVertically
        ) {
            selected.forEach { label ->
                TagChip(label = label, option = options.find { it.label == label })
                Spacer(modifier = Modifier.size(4.dp))
            }
        }
    }

    if (showSheet) {
        TagPickerSheet(
            options = options,
            selected = selected,
            allowMultiple = allowMultiple,
            onDismiss = { showSheet = false },
            onToggle = { label ->
                val updated = when {
                    !allowMultiple -> if (label in selected) emptyList() else listOf(label)
                    label in selected -> selected - label
                    else -> selected + label
                }
                onValueChange(updated.joinToString(MULTI_VALUE_SEPARATOR))
                if (!allowMultiple) showSheet = false
            },
            onCreate = { label ->
                onAddOption(label)
                val updated = if (allowMultiple) selected + label else listOf(label)
                onValueChange(updated.joinToString(MULTI_VALUE_SEPARATOR))
                if (!allowMultiple) showSheet = false
            },
            onRenameOption = { old, new -> onRenameOption(columnId, old, new) },
            onSetOptionColor = { label, hex -> onSetOptionColor(columnId, label, hex) },
            onDeleteOption = { label -> onDeleteOption(columnId, label) },
            onMoveOptions = { labels -> onMoveOptions(columnId, labels) },
            onSetOptionSecondColor = { label, hex, line -> onSetOptionSecondColor(columnId, label, hex, line) }
        )
    }
}

/**
 * La schermata per modificare un tag: nome, eliminazione e colore.
 *
 * **Il nome si vede cambiare mentre si scrive** (la pastiglia in cima), ma
 * il tag si rinomina **una volta sola, uscendo dalla modifica**: con
 * "Done", tornando indietro o chiudendo la finestra (dal 05/10/2026).
 * Fino ad allora si rinominava a ogni lettera, e l'elenco dei tag arriva
 * dal database un attimo dopo: in quell'attimo la finestra cercava il tag
 * col nome nuovo, non lo trovava, tornava all'elenco e la tastiera si
 * chiudeva — l'utente poteva scrivere una lettera sola. E scrivendo
 * veloce un rinomina poteva partire dal nome vecchio prima che il
 * precedente fosse salvato, e perdersi.
 */
@Composable
private fun TagOptionEditor(
    option: SelectOption,
    onDone: () -> Unit,
    onRename: (String) -> Unit,
    onSetColor: (String) -> Unit,
    /** Il secondo colore (null: nessuno) e la linea in mezzo. */
    onSetSecondColor: (String?, Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember(option.label) { mutableStateOf(option.label) }
    var pickingColor by remember { mutableStateOf(false) }
    var pickingSecond by remember { mutableStateOf(false) }
    // Già rinominato (o eliminato): l'uscita non deve rifarlo.
    var settled by remember(option.label) { mutableStateOf(false) }
    val currentOption by rememberUpdatedState(option)
    val currentRename by rememberUpdatedState(onRename)

    /** Il nome nuovo, se c'è e se è diverso: un nome vuoto lascia quello di prima. */
    fun commitName() {
        if (settled) return
        settled = true
        val newName = name.trim()
        if (newName.isNotEmpty() && newName != currentOption.label) currentRename(newName)
    }
    // Chiudendo la finestra, o tornando indietro, il nome scritto vale.
    DisposableEffect(option.label) { onDispose { commitName() } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                TagChip(label = name.trim().ifEmpty { option.label }, option = option)
            }
            TextButton(onClick = {
                commitName()
                onDone()
            }) { Text(Strings.done) }
        }
        Spacer(modifier = Modifier.size(12.dp))

        SheetTextField(
            value = name,
            onValueChange = { newName -> name = newName.replace("\n", "") },
            placeholder = DbStrings.tagName,
            leading = Icons.Filled.Notes
        )

        Spacer(modifier = Modifier.size(12.dp))
        SheetGroup {
            SheetAction(Icons.Filled.Delete, Strings.delete, isDestructive = true) {
                settled = true
                onDelete()
            }
        }

        // **Il colore, dallo spettro** (dal 05/10/2026, chiesto dall'utente):
        // lo stesso selettore del testo, delle celle colorate e delle barre
        // (`ColorPickerSheet`), con tinta, luminosità, Rosso/Verde/Blu ed
        // Hex. Prima c'erano solo i dieci colori di Notion (`TAG_COLORS`),
        // che restano quelli dati a giro ai tag nuovi.
        Spacer(modifier = Modifier.size(12.dp))
        SheetGroup {
            TagColorRow(hex = option.color, label = DbStrings.cellColor) { pickingColor = true }
        }

        // **Il secondo colore** (dal 20/10/2026, chiesto dall'utente): la
        // pastiglia diventa metà e metà, attaccate o con una linea in mezzo.
        Spacer(modifier = Modifier.size(12.dp))
        SheetGroup {
            val second = option.secondColor
            if (second == null) {
                SheetAction(Icons.Filled.Add, DbStrings.addSecondColor) { pickingSecond = true }
            } else {
                TagColorRow(hex = second, label = DbStrings.secondColor) { pickingSecond = true }
                HorizontalDivider()
                SettingsToggleRow(
                    icon = Icons.Filled.BorderVertical,
                    label = DbStrings.lineBetweenColors,
                    checked = option.colorLine
                ) { line -> onSetSecondColor(second, line) }
                HorizontalDivider()
                SheetAction(Icons.Filled.Close, DbStrings.removeSecondColor) { onSetSecondColor(null, false) }
            }
        }

        Spacer(modifier = Modifier.size(24.dp))
    }

    if (pickingSecond) {
        ColorPickerSheet(
            initialHex = option.secondColor ?: option.color,
            background = true,
            onDismiss = { pickingSecond = false },
            onSwitchTarget = {},
            onPick = { hex ->
                // Qui "nessun colore" toglie il secondo.
                onSetSecondColor(hex?.uppercase(), option.colorLine)
                pickingSecond = false
            },
            showTargetSwitch = false,
            previewLabel = option.label,
            noColorLabel = DbStrings.removeSecondColor
        )
    }

    if (pickingColor) {
        ColorPickerSheet(
            initialHex = option.color,
            // Il tag è una pastiglia piena: l'anteprima "dietro al testo".
            background = true,
            onDismiss = { pickingColor = false },
            onSwitchTarget = {},
            onPick = { hex ->
                // "Nessun colore" torna al grigio dei tag di Notion ("Default").
                onSetColor(hex?.uppercase() ?: TAG_COLORS.first().hex)
                pickingColor = false
            },
            showTargetSwitch = false,
            previewLabel = option.label,
            noColorLabel = DbStrings.defaultColor
        )
    }
}

/** Una riga del colore di un tag: il quadratino, il nome, il codice e la freccia che apre lo spettro. */
@Composable
private fun TagColorRow(hex: String, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(tagColor(hex), RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.size(14.dp))
        Text(label, modifier = Modifier.weight(1f))
        Text(
            text = hex.uppercase(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * La finestra per scegliere un tag: in cima il campo per crearne uno
 * nuovo, sotto quelli che esistono già.
 *
 * Il campo serve anche da ricerca, ma l'etichetta dice "Create a tag"
 * perché è quello che si fa più spesso quando la si apre; filtrare
 * l'elenco è il vantaggio che si ottiene scrivendo, non lo scopo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagPickerSheet(
    options: List<SelectOption>,
    selected: List<String>,
    allowMultiple: Boolean,
    onDismiss: () -> Unit,
    onToggle: (String) -> Unit,
    onCreate: (String) -> Unit,
    onRenameOption: (String, String) -> Unit,
    onSetOptionColor: (String, String) -> Unit,
    onDeleteOption: (String) -> Unit,
    onMoveOptions: (List<String>) -> Unit,
    onSetOptionSecondColor: (String, String?, Boolean) -> Unit
) {
    var query by remember { mutableStateOf("") }
    // "Reorder", in alto a destra: i tag con le maniglie, vedi `TagReorderList`.
    var reordering by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    var viewport by remember { mutableStateOf<Rect?>(null) }
    val focusManager = LocalFocusManager.current
    // Quale tag si sta modificando. La schermata di modifica vive
    // dentro questa stessa finestra invece che in una sopra l'altra:
    // due finestre sovrapposte si contendono la chiusura col gesto di
    // trascinamento.
    var editing by remember { mutableStateOf<String?>(null) }
    val trimmed = query.trim()
    val matching = options.filter { it.label.contains(trimmed, ignoreCase = true) }
    val exactExists = options.any { it.label.equals(trimmed, ignoreCase = true) }

    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        val optionBeingEdited = options.find { it.label == editing }
        if (optionBeingEdited != null) {
            TagOptionEditor(
                option = optionBeingEdited,
                onDone = { editing = null },
                // Una volta sola, uscendo dalla modifica: vedi `TagOptionEditor`.
                onRename = { newLabel -> onRenameOption(optionBeingEdited.label, newLabel) },
                onSetColor = { hex -> onSetOptionColor(optionBeingEdited.label, hex) },
                onSetSecondColor = { hex, line -> onSetOptionSecondColor(optionBeingEdited.label, hex, line) },
                onDelete = {
                    onDeleteOption(optionBeingEdited.label)
                    editing = null
                }
            )
            return@NoteBottomSheet
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 260.dp)
                .padding(horizontal = 16.dp)
                .onGloballyPositioned { viewport = it.boundsInRoot() }
                .verticalScroll(scroll)
        ) {
            // Con meno di due tag non c'è niente da mettere in ordine.
            val canReorder = reordering || options.size >= 2
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = DbStrings.typeName(if (allowMultiple) ColumnType.MULTI_SELECT else ColumnType.SELECT),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // Il titolo resta al centro senza finire sotto al pulsante.
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (canReorder) 104.dp else 0.dp),
                    textAlign = TextAlign.Center
                )
                if (canReorder) {
                    TextButton(
                        onClick = {
                            focusManager.clearFocus()
                            reordering = !reordering
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) { Text(if (reordering) Strings.done else DbStrings.reorder) }
                }
            }
            Spacer(modifier = Modifier.size(16.dp))

            if (reordering) {
                TagReorderList(
                    options = options,
                    onMove = onMoveOptions,
                    scrollState = scroll,
                    viewport = { viewport }
                )
                Spacer(modifier = Modifier.size(24.dp))
                return@Column
            }

            SheetTextField(
                value = query,
                onValueChange = { query = it.replace("\n", "") },
                placeholder = DbStrings.createATag,
                leading = Icons.Filled.Add
            )

            if (trimmed.isNotEmpty() && !exactExists) {
                Spacer(modifier = Modifier.size(12.dp))
                SheetGroup {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { query = ""; onCreate(trimmed) }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            DbStrings.create,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        TagChip(label = trimmed, color = null)
                    }
                }
            }

            Spacer(modifier = Modifier.size(20.dp))
            Text(
                text = DbStrings.selectExistingOption,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.size(8.dp))

            if (matching.isEmpty()) {
                Text(
                    text = if (options.isEmpty()) {
                        DbStrings.noTagsYet
                    } else {
                        DbStrings.noTagMatches
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                SheetGroup {
                    matching.forEachIndexed { index, option ->
                        if (index > 0) HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(option.label) }
                                .padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TagChip(label = option.label, option = option)
                            Spacer(modifier = Modifier.weight(1f))
                            if (option.label in selected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = DbStrings.selected,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                            }
                            Icon(
                                Icons.Filled.MoreHoriz,
                                contentDescription = DbStrings.editTag,
                                modifier = Modifier
                                    .clickable { editing = option.label }
                                    .padding(4.dp)
                                    .size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

/**
 * La cella di una data. Permette un intervallo, non una data sola:
 * scegliendo un secondo giorno la pagina dura da lì a lì, e il
 * calendario la mostrerà su tutti i giorni che copre.
 *
 * Scegliere solo il primo giorno e confermare resta valido: è il caso
 * normale di una scadenza.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateCell(
    value: String,
    onValueChange: (String) -> Unit,
    contentPadding: Dp = 0.dp
) {
    var showPicker by remember { mutableStateOf(false) }
    val range = parseDateRange(value)

    // La zona toccabile è **tutta la cella**, non il testo.
    //
    // Una cella vuota mostra un trattino largo pochi punti: se il tocco
    // stesse solo lì, ovunque altro cadrebbe sulla pagina sotto il
    // database, che risponde mettendosi a scrivere in fondo. Il margine
    // laterale sta dentro il riquadro, così non lascia fuori nemmeno i
    // bordi.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clickable { showPicker = true }
            .padding(horizontal = contentPadding)
            .cellWrapPadding(),
        contentAlignment = LocalCellAlignment.current.box
    ) {
        // La data col suo nome sta dove chiede la colonna: la riga è larga
        // quanto la cella (il testo si restringe se non ci sta), e a
        // spostarla dentro è la disposizione, non il riquadro intorno.
        Row(horizontalArrangement = LocalCellAlignment.current.arrangement, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = range?.label() ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = cellMaxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            // Una data che si ripete ha le frecce in cerchio accanto.
            if (range?.recurrence != null) {
                Spacer(modifier = Modifier.size(4.dp))
                Icon(
                    Icons.Filled.Repeat,
                    contentDescription = DbStrings.repeat,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
            // "Include notification" acceso: la data manda una notifica.
            if (range?.notify == true) {
                Spacer(modifier = Modifier.size(4.dp))
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = DbStrings.includeNotification,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }

    if (showPicker) {
        DateEditorSheet(
            range = range,
            onConfirm = {
                onValueChange(it)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

/**
 * **Una cella "Color cell"** (dal 26/09/2026, chiesta così dall'utente:
 * "riempire la cella con un colore a mia scelta", e scriverci sopra "come
 * possibilità, non obbligo"). Nella tabella il colore lo stende la cella
 * intera (`CellEditor`) e qui c'è il testo, nero o bianco a seconda di
 * cosa si legge meglio; nella scheda di una riga è un riquadro colorato.
 * Toccandola si apre `ColorCellSheet`.
 */
@Composable
private fun ColorCell(
    columnName: String,
    value: String,
    onValueChange: (String) -> Unit,
    inTable: Boolean,
    contentPadding: Dp
) {
    val cell = parseColorCell(value)
    val fill = hexToColor(cell.color)
    val locked = LocalDatabaseLocked.current
    var editing by remember { mutableStateOf(false) }
    val cellColor = cell.color
    val textColor = when {
        cellColor == null -> MaterialTheme.colorScheme.onBackground
        darkTextOn(cellColor) -> Color.Black
        else -> Color.White
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (inTable) {
                    Modifier.fillMaxHeight()
                } else {
                    Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .then(
                            if (fill != null) {
                                Modifier.background(fill)
                            } else {
                                Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                            }
                        )
                }
            )
            .clickable(enabled = !locked) { editing = true }
            .padding(horizontal = if (inTable) contentPadding else 10.dp)
            .cellWrapPadding(),
        contentAlignment = LocalCellAlignment.current.box
    ) {
        Text(
            // Una cella vuota mostra il trattino, come le altre.
            text = cell.text.ifEmpty { if (fill == null) "—" else "" },
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            maxLines = cellMaxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
    if (editing) {
        ColorCellSheet(
            title = columnName,
            initial = cell,
            onDismiss = { editing = false },
            onConfirm = {
                onValueChange(encodeColorCell(it))
                editing = false
            }
        )
    }
}

/**
 * La finestra di una cella colorata: com'è, il testo da scriverci sopra
 * (se si vuole) e il colore, che si sceglie **col selettore del
 * pennello**, come le penne del disegno. Si salva con "Done".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorCellSheet(
    title: String,
    initial: ColorCellValue,
    onDismiss: () -> Unit,
    onConfirm: (ColorCellValue) -> Unit
) {
    var text by remember { mutableStateOf(initial.text) }
    var color by remember { mutableStateOf(initial.color) }
    var picking by remember { mutableStateOf(false) }
    val fill = hexToColor(color)

    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) { Text(Strings.cancel) }
            TextButton(onClick = { onConfirm(ColorCellValue(color, text.trim())) }) { Text(Strings.done) }
        }
        HorizontalDivider()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            // Com'è la cella, prima di salvarla.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(fill ?: MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = when {
                        color == null -> MaterialTheme.colorScheme.onBackground
                        darkTextOn(color!!) -> Color.Black
                        else -> Color.White
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.size(16.dp))
            SheetTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = DbStrings.colorCellTextHint,
                leading = Icons.Filled.Notes
            )
            Spacer(modifier = Modifier.size(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { picking = true }
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(fill ?: Color.Transparent)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = DbStrings.cellColor,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = color ?: DbStrings.noCellColor,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (picking) {
        ColorPickerSheet(
            initialHex = color,
            // Con "dietro al testo" l'anteprima del selettore è proprio
            // una cella: il colore pieno con la scritta sopra.
            background = true,
            onDismiss = { picking = false },
            onSwitchTarget = {},
            onPick = { hex ->
                color = hex?.uppercase()
                picking = false
            },
            showTargetSwitch = false,
            previewLabel = title
        )
    }
}

/**
 * Il valore di una proprietà su una scheda (bacheca, galleria, elenco):
 * scritto, e per una cella colorata una targhetta del suo colore.
 */
@Composable
private fun CardPropertyValue(
    column: DatabaseColumnEntity,
    value: String,
    overflow: TextOverflow = TextOverflow.Clip,
    /** Di una cella URL, la scritta scelta con "Show as…" (dal 27/10/2026). */
    linkLabel: LinkLabel? = null
) {
    // "Image": la miniatura, intera.
    if (column.type == ColumnType.IMAGE) {
        val context = LocalPlatformContext.current
        val store = remember(context) { PageImageStore(context) }
        PageImage(
            fileName = value,
            store = store,
            contentDescription = DbStrings.columnName(column.name),
            contentScale = ContentScale.Fit,
            placeholder = false,
            modifier = Modifier.size(28.dp)
        )
        return
    }
    // "Relation": le frecce e i nomi delle righe collegate.
    if (column.type == ColumnType.RELATION) {
        val related = LocalRelated.current
        Text(
            text = relationCardText(value, column.relationConfig()?.let { related[it.database] }),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = overflow
        )
        return
    }
    // "Page link": la freccia e il titolo di adesso della pagina collegata.
    if (column.type == ColumnType.PAGE_LINK) {
        val page = LocalLinkedPages.current[value]
        Text(
            text = page?.let { "↗ " + it.title.ifBlank { Strings.untitled } } ?: DbStrings.pageNotFound,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = overflow
        )
        return
    }
    if (column.type != ColumnType.COLOR) {
        Text(
            text = linkLabel?.takeIf { column.type == ColumnType.URL }?.text ?: listCellLabel(column, value),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = overflow
        )
        return
    }
    val cell = parseColorCell(value)
    val fill = hexToColor(cell.color)
    if (fill == null) {
        if (cell.text.isNotEmpty()) {
            Text(
                text = cell.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = overflow
            )
        }
        return
    }
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 28.dp, minHeight = 16.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(fill)
            .padding(horizontal = 6.dp, vertical = 1.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (cell.text.isNotEmpty()) {
            Text(
                text = cell.text,
                style = MaterialTheme.typography.bodySmall,
                color = if (darkTextOn(cell.color!!)) Color.Black else Color.White,
                maxLines = 1,
                overflow = overflow
            )
        }
    }
}

/**
 * La finestra della data, sul modello di quella di Notion: il
 * calendario, e sotto gli interruttori per quello che la data può
 * avere in più.
 *
 * L'ora è **facoltativa** e resta spenta finché non la si accende.
 * Accesa, vale sia per l'inizio che per la fine; spenta, la data
 * continua a valere il giorno intero — che è quello che serve alla
 * grandissima parte delle pagine.
 *
 * Quello che Notion ha e qui non c'è — formato della data, fuso
 * orario — non è stato dimenticato: sono preferenze che qui seguono le
 * impostazioni dell'app.
 *
 * **"Include notification"** (dal 25/09/2026), sotto "Include time":
 * la data manda una notifica col nome della pagina, all'ora scelta; con
 * una data di fine, all'inizio e alla fine. Vedi `data/notify/Reminders.kt`.
 *
 * **La notifica ha sempre un'ora scelta** (dal 16/10/2026, chiesto
 * dall'utente: "Toglila, tanto l'ora manualmente la inserisco sempre").
 * Prima, senza "Include time", suonava alle 9:00; ora accendendo la notifica
 * si accende anche l'ora, e spegnendo l'ora si spegne la notifica. L'ora
 * proposta non è più le 9:00 ma quella di adesso. Le date salvate prima con
 * la notifica senza ora restano com'erano (alle 9:00) finché non le si
 * cambia: aprendole non si tocca niente da sé.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateEditorSheet(
    range: DateRange?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var startDate by remember { mutableStateOf(range?.start?.toLocalDate()) }
    var endDate by remember { mutableStateOf(range?.end?.toLocalDate()) }
    // **La data di fine si chiede con un interruttore**, come su Notion.
    // Col calendario di Material toccare un secondo giorno faceva un
    // periodo; con quello nuovo (dal 25/09/2026) toccare un altro giorno
    // sposta la data, e il periodo si fa solo ad interruttore acceso.
    var withEndDate by remember { mutableStateOf(range?.end != null) }
    var includeTime by remember { mutableStateOf(range?.hasTime == true) }
    var includeNotification by remember { mutableStateOf(range?.notify == true) }
    // Le notifiche, in minuti prima dell'inizio (dal 26/10/2026): vedi `ReminderList`.
    var reminders by remember { mutableStateOf(range?.takeIf { it.notify }?.reminders ?: listOf(0)) }
    // Senza un'ora salvata si propone quella di adesso, al minuto, e la fine
    // un'ora dopo.
    val now = remember { LocalTime.now(AppSettings.zoneId).withSecond(0).withNano(0) }
    var startTime by remember {
        mutableStateOf(
            range?.takeIf { it.hasTime }?.start?.toLocalDateTime()?.toLocalTime() ?: now
        )
    }
    var endTime by remember {
        mutableStateOf(
            range?.takeIf { it.hasTime }?.end?.toLocalDateTime()?.toLocalTime() ?: now.plusHours(1)
        )
    }
    // Quale ora si sta scegliendo: null = nessuna finestra aperta.
    var editingTime by remember { mutableStateOf<Boolean?>(null) }

    // La ripetizione: la regola scelta, l'elenco dei modi rapidi e la
    // schermata per costruirne una su misura.
    var recurrence by remember { mutableStateOf(range?.recurrence) }
    var showRepeatPresets by remember { mutableStateOf(false) }
    var showCustomRecurrence by remember { mutableStateOf(false) }
    // Le ripetizioni si descrivono a partire dal giorno scelto qui
    // sopra ("ogni lunedì" se si è scelto un lunedì): se non è stato
    // scelto ancora niente si parla di oggi.
    val selectedStartDate = startDate ?: LocalDate.now()
    // "Starts": da oggi, o da un giorno scelto, fino al 2100 (vedi sotto).
    var showRepeatFrom by remember { mutableStateOf(false) }
    var pickRepeatStart by remember { mutableStateOf(false) }

    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        // I pulsanti stanno in testa e non in fondo: sotto al
        // calendario e agli interruttori finirebbero fuori schermo, e
        // si sceglierebbe una data senza poterla confermare. Stessa
        // lezione della finestra delle proprietà.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = DbStrings.typeName(ColumnType.DATE),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) { Text(Strings.cancel) }
            TextButton(onClick = {
                // **Con una ripetizione il giorno non va toccato** (dal
                // 26/09/2026, chiesto dall'utente): "ogni sabato" dice già
                // tutto, e si parte da oggi. Prima, senza un giorno toccato,
                // "Done" svuotava la cella e la ripetizione andava persa.
                val chosenStart = startDate ?: if (recurrence != null) LocalDate.now() else null
                if (chosenStart == null) {
                    // Confermare senza aver scelto niente svuota la cella.
                    onConfirm("")
                    return@TextButton
                }
                val startAt = LocalDateTime.of(
                    chosenStart,
                    if (includeTime) startTime else LocalTime.MIDNIGHT
                )
                val endAt = endDate?.takeIf { withEndDate }?.let {
                    LocalDateTime.of(it, if (includeTime) endTime else LocalTime.MIDNIGHT)
                }
                onConfirm(
                    encodeDateRange(
                        start = startAt.toEpochMillis(),
                        end = endAt?.toEpochMillis(),
                        hasTime = includeTime,
                        recurrence = recurrence,
                        notify = includeNotification,
                        reminders = reminders
                    )
                )
            }) { Text(Strings.done) }
        }
        HorizontalDivider()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Il mese come quello di Samsung (vedi `SamsungCalendar`).
            Spacer(modifier = Modifier.height(8.dp))
            SamsungCalendar(
                start = startDate,
                end = endDate.takeIf { withEndDate },
                onPick = { day ->
                    // Ritoccando un giorno col pallino, il pallino se ne va
                    // (dal 30/09/2026); il resto come prima. Vedi `pickCalendarDay`.
                    val (start, end) = pickCalendarDay(day, startDate, endDate, withEndDate)
                    startDate = start
                    endDate = end
                },
                onWheelPick = { day ->
                    startDate = day
                    if (endDate?.let { it <= day } == true) endDate = null
                },
                // I giorni in cui la ripetizione capita, col pallino arancione.
                repeat = recurrence
            )
            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        withEndDate = !withEndDate
                        if (!withEndDate) endDate = null
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DbStrings.endDate,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = withEndDate,
                    onCheckedChange = {
                        withEndDate = it
                        if (!it) endDate = null
                    }
                )
            }

            HorizontalDivider()
            // Senza ora niente notifica: suonerebbe a un'ora non scelta.
            fun setIncludeTime(on: Boolean) {
                includeTime = on
                if (!on) includeNotification = false
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { setIncludeTime(!includeTime) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DbStrings.includeTime,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = includeTime, onCheckedChange = { setIncludeTime(it) })
            }

            if (includeTime) {
                HorizontalDivider()
                DateEditorTimeRow(
                    label = DbStrings.startTime,
                    time = startTime,
                    onClick = { editingTime = true }
                )
                if (withEndDate && endDate != null) {
                    HorizontalDivider()
                    DateEditorTimeRow(
                        label = DbStrings.endTime,
                        time = endTime,
                        onClick = { editingTime = false }
                    )
                }
            }

            // **La notifica della data**, chiesta dall'utente proprio qui:
            // sotto "Include time", perché è l'ora che decide quando suona.
            // Accesa, accende anche l'ora (dal 16/10/2026): è quella scelta
            // qui sopra a dire quando suona, non più le 9:00.
            fun setIncludeNotification(on: Boolean) {
                includeNotification = on
                if (on) {
                    includeTime = true
                    if (reminders.isEmpty()) reminders = listOf(0)
                    NotificationPermission.request()
                }
            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { setIncludeNotification(!includeNotification) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DbStrings.includeNotification,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = includeNotification,
                    onCheckedChange = { setIncludeNotification(it) }
                )
            }
            // **Più notifiche, come su Google Calendar** (dal 26/10/2026):
            // quella all'inizio dice quando suona, come prima sotto
            // l'interruttore; le altre quanto prima. Tolta l'ultima, la
            // notifica si spegne, e riaccendendola si riparte dall'inizio.
            if (includeNotification) {
                ReminderList(
                    reminders = reminders,
                    atStartLabel = DbStrings.notificationWhen(
                        hasTime = includeTime,
                        hasEnd = withEndDate && endDate != null
                    ),
                    onChange = { changed ->
                        if (changed.isEmpty()) {
                            includeNotification = false
                            reminders = listOf(0)
                        } else {
                            reminders = changed
                        }
                    },
                    indent = 32.dp
                )
            }

            // La ripetizione sta sotto all'ora perché è la stessa
            // famiglia di domande — *quando* capita — e perché
            // riguarda la data di partenza scelta qui sopra.
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showRepeatPresets = true }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Repeat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = DbStrings.repeat,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = recurrenceSummary(recurrence, selectedStartDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (recurrence == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1.4f, fill = false)
                )
            }

            // **Da quando si ripete** (dal 26/09/2026): da oggi, o da un
            // giorno scelto, fino al 2100. Senza toccare niente è da oggi.
            if (recurrence != null) {
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRepeatFrom = true }
                        .padding(start = 48.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = DbStrings.repeatStarts,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (startDate == null || startDate == LocalDate.now()) {
                            DbStrings.today
                        } else {
                            selectedStartDate.format(DATE_FORMAT)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider()
            Text(
                text = DbStrings.clear,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onConfirm("") }
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    val editing = editingTime
    if (editing != null) {
        SamsungTimeDialog(
            initial = if (editing) startTime else endTime,
            onPick = { picked -> if (editing) startTime = picked else endTime = picked },
            onDismiss = { editingTime = null }
        )
    }

    if (showRepeatPresets) {
        RepeatPresetDialog(
            start = selectedStartDate,
            current = recurrence,
            onDismiss = { showRepeatPresets = false },
            onPick = { picked ->
                recurrence = picked
                showRepeatPresets = false
            },
            onCustom = {
                showRepeatPresets = false
                showCustomRecurrence = true
            }
        )
    }

    if (showRepeatFrom) {
        FitAlertDialog(
            onDismissRequest = { showRepeatFrom = false },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showRepeatFrom = false }) { Text(Strings.cancel) } },
            title = { Text(DbStrings.repeatFromTitle) },
            text = {
                Column {
                    listOf(DbStrings.repeatFromToday to false, DbStrings.repeatFromChosenDay to true).forEach { (label, choose) ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showRepeatFrom = false
                                    if (choose) {
                                        pickRepeatStart = true
                                    } else {
                                        startDate = LocalDate.now()
                                        if (endDate?.let { it <= LocalDate.now() } == true) endDate = null
                                        recurrence = recurrence?.copy(
                                            end = RecurrenceEnd.OnDate(Recurrence.END_2100.toEpochDay())
                                        )
                                    }
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
        )
    }

    if (pickRepeatStart) {
        SamsungDateDialog(
            initial = selectedStartDate,
            onPick = { picked ->
                startDate = picked
                if (endDate?.let { it <= picked } == true) endDate = null
                recurrence = recurrence?.copy(end = RecurrenceEnd.OnDate(Recurrence.END_2100.toEpochDay()))
            },
            onDismiss = { pickRepeatStart = false }
        )
    }

    if (showCustomRecurrence) {
        CustomRecurrenceDialog(
            start = selectedStartDate,
            initial = recurrence,
            onDismiss = { showCustomRecurrence = false },
            onDone = { rule ->
                recurrence = rule
                showCustomRecurrence = false
            }
        )
    }
}

/**
 * I modi rapidi di ripetere qualcosa, costruiti sulla data scelta:
 * "ogni lunedì" se si è scelto un lunedì, "il terzo martedì del mese"
 * se si è scelto il terzo martedì. Sono le stesse scorciatoie di
 * Google Calendar, e coprono quasi tutti i casi senza aprire niente.
 */
@Composable
fun RepeatPresetDialog(
    start: LocalDate,
    current: Recurrence?,
    onDismiss: () -> Unit,
    onPick: (Recurrence?) -> Unit,
    onCustom: () -> Unit
) {
    val weekdaysOnly = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    )
    val presets: List<Pair<String, Recurrence?>> = listOf(
        DbStrings.doesNotRepeat to null,
        DbStrings.daily to Recurrence(RecurrenceFreq.DAILY),
        DbStrings.weeklyOn(dayName(start.dayOfWeek)) to
            Recurrence(RecurrenceFreq.WEEKLY, weekdays = setOf(start.dayOfWeek)),
        DbStrings.monthlyOn(DbStrings.nthWeekday(start, dayName(start.dayOfWeek))) to
            Recurrence(
                RecurrenceFreq.MONTHLY,
                monthlyMode = MonthlyMode.NTH_WEEKDAY,
                // Scritti nella regola: cambiando poi il giorno da cui parte,
                // "l'ultimo sabato" resta l'ultimo sabato.
                nthWeek = if (start.plusWeeks(1).month != start.month) Recurrence.LAST_WEEK else ((start.dayOfMonth - 1) / 7 + 1).coerceAtMost(4),
                nthWeekday = start.dayOfWeek
            ),
        DbStrings.annuallyOn(start.format(DATE_FORMAT)) to Recurrence(RecurrenceFreq.YEARLY),
        DbStrings.everyWeekday to
            Recurrence(RecurrenceFreq.WEEKLY, weekdays = weekdaysOnly)
    )

    FitAlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } },
        title = { Text(DbStrings.repeat) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                presets.forEach { (label, rule) ->
                    val selected = recurrenceSummary(rule, start) ==
                        recurrenceSummary(current, start)
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(rule) }
                            .padding(vertical = pcOr(6.dp, 12.dp))
                    )
                }
                HorizontalDivider()
                Text(
                    text = DbStrings.custom,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCustom() }
                        .padding(vertical = pcOr(6.dp, 12.dp))
                )
            }
        }
    )
}

/**
 * La schermata per costruire una ripetizione qualsiasi: ogni quanto,
 * in quali giorni, e fino a quando. Ricalca quella di Google
 * Calendar, che è la più chiara in circolazione per una cosa che di
 * suo si presta a diventare un modulo fiscale.
 *
 * Le parti che non servono non si vedono: i sette pallini dei giorni
 * solo per le settimane, la scelta "giorno del mese / ennesimo
 * lunedì" solo per i mesi.
 */
@Composable
fun CustomRecurrenceDialog(
    start: LocalDate,
    initial: Recurrence?,
    onDismiss: () -> Unit,
    onDone: (Recurrence) -> Unit
) {
    var freq by remember { mutableStateOf(initial?.freq ?: RecurrenceFreq.WEEKLY) }
    var interval by remember { mutableStateOf((initial?.interval ?: 1).toString()) }
    var weekdays by remember {
        mutableStateOf(initial?.weekdays?.ifEmpty { setOf(start.dayOfWeek) } ?: setOf(start.dayOfWeek))
    }
    var monthlyMode by remember {
        mutableStateOf(initial?.monthlyMode ?: MonthlyMode.DAY_OF_MONTH)
    }
    // **Il giorno del mese, e "il primo / … / l'ultimo lunedì", scelti qui**
    // (dal 26/09/2026): non serve più toccare prima quel giorno sul calendario.
    var monthDay by remember { mutableStateOf((initial?.monthDay ?: start.dayOfMonth).toString()) }
    var nthIndex by remember {
        mutableStateOf(
            initial?.nthWeek?.let { if (it == Recurrence.LAST_WEEK) 4 else (it - 1).coerceIn(0, 3) }
                ?: if (start.plusWeeks(1).month != start.month) 4 else ((start.dayOfMonth - 1) / 7).coerceAtMost(3)
        )
    }
    var nthWeekday by remember { mutableStateOf(initial?.nthWeekday ?: start.dayOfWeek) }
    var nthMenuOpen by remember { mutableStateOf(false) }
    var endKind by remember {
        mutableStateOf(
            when (initial?.end) {
                is RecurrenceEnd.OnDate -> 1
                is RecurrenceEnd.AfterCount -> 2
                else -> 0
            }
        )
    }
    var endDate by remember {
        mutableStateOf(
            (initial?.end as? RecurrenceEnd.OnDate)?.let { LocalDate.ofEpochDay(it.epochDay) }
                ?: start.plusMonths(3)
        )
    }
    var endCount by remember {
        mutableStateOf(((initial?.end as? RecurrenceEnd.AfterCount)?.count ?: 13).toString())
    }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var freqMenuOpen by remember { mutableStateOf(false) }

    FitAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(DbStrings.customRecurrence) },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } },
        confirmButton = {
            TextButton(onClick = {
                onDone(
                    Recurrence(
                        freq = freq,
                        interval = interval.toIntOrNull()?.coerceIn(1, 999) ?: 1,
                        weekdays = if (freq == RecurrenceFreq.WEEKLY) weekdays else emptySet(),
                        monthlyMode = monthlyMode,
                        monthDay = if (freq == RecurrenceFreq.MONTHLY && monthlyMode == MonthlyMode.DAY_OF_MONTH) {
                            monthDay.toIntOrNull()?.coerceIn(1, 31) ?: start.dayOfMonth
                        } else {
                            null
                        },
                        nthWeek = if (freq == RecurrenceFreq.MONTHLY && monthlyMode == MonthlyMode.NTH_WEEKDAY) {
                            if (nthIndex == 4) Recurrence.LAST_WEEK else nthIndex + 1
                        } else {
                            null
                        },
                        nthWeekday = if (freq == RecurrenceFreq.MONTHLY && monthlyMode == MonthlyMode.NTH_WEEKDAY) {
                            nthWeekday
                        } else {
                            null
                        },
                        end = when (endKind) {
                            1 -> RecurrenceEnd.OnDate(endDate.toEpochDay())
                            2 -> RecurrenceEnd.AfterCount(
                                endCount.toIntOrNull()?.coerceIn(1, 999) ?: 1
                            )
                            else -> RecurrenceEnd.Never
                        }
                    )
                )
            }) { Text(Strings.done) }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(DbStrings.repeatEvery, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.size(12.dp))
                    AutoCaps {
                        OutlinedTextField(
                            value = interval,
                            onValueChange = { typed ->
                                interval = typed.filter { it.isDigit() }.take(3)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(80.dp)
                        )
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Box {
                        Row(
                            modifier = Modifier
                                .clickable { freqMenuOpen = true }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = DbStrings.unit(freq, plural = (interval.toIntOrNull() ?: 1) > 1),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                contentDescription = DbStrings.changeUnit,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        DropdownMenu(
                            expanded = freqMenuOpen,
                            onDismissRequest = { freqMenuOpen = false }
                        ) {
                            RecurrenceFreq.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(DbStrings.unit(option, plural = false))
                                    },
                                    onClick = {
                                        freq = option
                                        freqMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (freq == RecurrenceFreq.WEEKLY) {
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(DbStrings.repeatOn, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.size(8.dp))
                    // L'ordine dei giorni segue le convenzioni locali, come il
                    // calendario: lunedì in Italia. Almeno un giorno resta
                    // acceso: "ogni settimana in nessun giorno" non vuol dire niente.
                    WeekdayChips(
                        isOn = { it in weekdays },
                        onClick = { day ->
                            weekdays = if (day in weekdays && weekdays.size > 1) weekdays - day else weekdays + day
                        }
                    )
                }

                if (freq == RecurrenceFreq.MONTHLY) {
                    Spacer(modifier = Modifier.size(12.dp))
                    // "Il giorno [21]": il numero si scrive.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = monthlyMode == MonthlyMode.DAY_OF_MONTH,
                            onClick = { monthlyMode = MonthlyMode.DAY_OF_MONTH }
                        )
                        Text(DbStrings.on, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.size(12.dp))
                        AutoCaps {
                            OutlinedTextField(
                                value = monthDay,
                                onValueChange = { typed ->
                                    monthDay = typed.filter { it.isDigit() }.take(2)
                                    monthlyMode = MonthlyMode.DAY_OF_MONTH
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(80.dp)
                            )
                        }
                    }
                    // "L'ultima domenica": quale settimana dal menu, il giorno
                    // dai pallini qui sotto.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = monthlyMode == MonthlyMode.NTH_WEEKDAY,
                            onClick = { monthlyMode = MonthlyMode.NTH_WEEKDAY }
                        )
                        Box {
                            Row(
                                modifier = Modifier
                                    .clickable {
                                        monthlyMode = MonthlyMode.NTH_WEEKDAY
                                        nthMenuOpen = true
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = DbStrings.nthWeekdayPhrase(nthIndex, nthWeekday, dayName(nthWeekday), capitalized = true),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (monthlyMode == MonthlyMode.NTH_WEEKDAY) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onBackground
                                    }
                                )
                                Icon(
                                    Icons.Filled.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            DropdownMenu(expanded = nthMenuOpen, onDismissRequest = { nthMenuOpen = false }) {
                                (0..4).forEach { index ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(DbStrings.nthWeekdayPhrase(index, nthWeekday, dayName(nthWeekday), capitalized = true))
                                        },
                                        onClick = {
                                            nthIndex = index
                                            nthMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    if (monthlyMode == MonthlyMode.NTH_WEEKDAY) {
                        Spacer(modifier = Modifier.size(4.dp))
                        WeekdayChips(isOn = { it == nthWeekday }, onClick = { nthWeekday = it })
                    }
                }

                Spacer(modifier = Modifier.size(16.dp))
                Text(DbStrings.ends, style = MaterialTheme.typography.bodyMedium)
                RecurrenceChoiceRow(
                    selected = endKind == 0,
                    label = DbStrings.never,
                    onSelect = { endKind = 0 }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = endKind == 1, onClick = { endKind = 1 })
                    Text(DbStrings.on, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = endDate.format(DATE_FORMAT),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (endKind == 1) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .clickable {
                                endKind = 1
                                showEndDatePicker = true
                            }
                            .padding(vertical = 8.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = endKind == 2, onClick = { endKind = 2 })
                    Text(DbStrings.after, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.size(12.dp))
                    AutoCaps {
                        OutlinedTextField(
                            value = endCount,
                            onValueChange = { typed ->
                                endCount = typed.filter { it.isDigit() }.take(3)
                                endKind = 2
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(90.dp)
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(DbStrings.times, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    )

    if (showEndDatePicker) {
        SamsungDateDialog(
            initial = endDate,
            onPick = { picked ->
                endDate = picked
                endKind = 1
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
}

/** I sette giorni in pallini, lunedì per primo dove si usa così: accesi quelli scelti. */
@Composable
private fun WeekdayChips(isOn: (DayOfWeek) -> Boolean, onClick: (DayOfWeek) -> Unit) {
    Row {
        val first = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        (0..6).forEach { offset ->
            val day = first.plus(offset.toLong())
            val on = isOn(day)
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(34.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onClick(day) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayInitial(day),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Una riga con il pallino di scelta, usata dentro la ripetizione. */
@Composable
private fun RecurrenceChoiceRow(selected: Boolean, label: String, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * La regola di ripetizione detta a parole, come la scriverebbe
 * qualcuno: "Ogni tre settimane il lunedì e il mercoledì, 13 volte".
 *
 * Serve nella riga "Repeat" della finestra della data: una regola
 * riassunta in una riga si controlla a colpo d'occhio, mentre per
 * capire "FREQ=WEEKLY;INT=3" bisogna riaprire la schermata.
 */
fun recurrenceSummary(rule: Recurrence?, start: LocalDate): String {
    if (rule == null) return DbStrings.doesNotRepeat
    val every = rule.interval
    val head = when (rule.freq) {
        RecurrenceFreq.DAILY ->
            if (every == 1) DbStrings.everyDay else DbStrings.everyNDays(every)

        RecurrenceFreq.WEEKLY -> {
            val days = rule.weekdays.ifEmpty { setOf(start.dayOfWeek) }
            val isWeekdays = days == setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
            )
            val names = if (isWeekdays) {
                DbStrings.everyWeekdayShort
            } else {
                days.sortedBy { it.value }.joinToString(", ") { dayName(it) }
            }
            if (every == 1) DbStrings.weeklyOn(names) else DbStrings.everyNWeeksOn(every, names)
        }

        RecurrenceFreq.MONTHLY -> {
            // "primo", "secondo"... oppure "ultimo" quando dopo non ce n'è
            // un altro nello stesso mese: chi dice "l'ultima domenica"
            // intende sempre l'ultima, non la quarta quando ce ne sono
            // cinque. Vedi `DbStrings.nthWeekday`.
            val what = when (rule.monthlyMode) {
                MonthlyMode.DAY_OF_MONTH -> DbStrings.dayOfMonth(rule.monthDay ?: start.dayOfMonth)
                MonthlyMode.NTH_WEEKDAY -> if (rule.nthWeek != null || rule.nthWeekday != null) {
                    val weekday = rule.nthWeekday ?: start.dayOfWeek
                    val index = rule.nthWeek?.let { if (it == Recurrence.LAST_WEEK) 4 else (it - 1).coerceIn(0, 3) }
                        ?: if (start.plusWeeks(1).month != start.month) 4 else ((start.dayOfMonth - 1) / 7).coerceAtMost(3)
                    DbStrings.nthWeekdayPhrase(index, weekday, dayName(weekday))
                } else {
                    DbStrings.nthWeekday(start, dayName(start.dayOfWeek))
                }
            }
            if (every == 1) DbStrings.monthlyOn(what) else DbStrings.everyNMonthsOn(every, what)
        }

        RecurrenceFreq.YEARLY -> {
            val what = start.format(DATE_FORMAT)
            if (every == 1) DbStrings.annuallyOn(what) else DbStrings.everyNYearsOn(every, what)
        }
    }
    return when (val end = rule.end) {
        RecurrenceEnd.Never -> head
        is RecurrenceEnd.OnDate ->
            head + DbStrings.until(LocalDate.ofEpochDay(end.epochDay).format(DATE_FORMAT))
        is RecurrenceEnd.AfterCount ->
            head + DbStrings.nTimes(end.count)
    }
}

private fun dayName(day: DayOfWeek): String =
    day.getDisplayName(TextStyle.FULL, displayLocale)

private fun dayInitial(day: DayOfWeek): String =
    day.getDisplayName(TextStyle.NARROW, displayLocale)

/** Una riga "Start time 09:00" dentro la finestra della data. */
@Composable
private fun DateEditorTimeRow(label: String, time: LocalTime, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = time.format(TIME_FORMAT),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Se il database aperto è **semplice** (`PageEntity.isSimpleDatabase`):
 * le sue righe sono solo testo e non diventano mai pagine. Passa da qui
 * per la stessa ragione di `LocalDatabaseLocked` — lo guardano la cella
 * del nome (niente OPEN) e i pulsanti per aggiungere ("Nuova riga" invece
 * di "Nuova pagina"), in fondo a catene che non hanno altro motivo di
 * saperlo.
 */
private val LocalSimpleDatabase = staticCompositionLocalOf { false }

/**
 * Le icone delle pagine delle righe del database aperto: riga → immagine o
 * emoji. Passa da qui per la stessa ragione di `LocalDatabaseLocked`:
 * il titolo di una riga si disegna in sette viste diverse, in fondo a
 * catene di composable che non hanno altro motivo per saperlo.
 */
private val LocalRowIcons = compositionLocalOf<Map<String, RowIconRef>> { emptyMap() }

/**
 * L'icona della pagina di una riga, prima del suo titolo — solo se ne ha
 * una: una riga senza icona parte dal margine, senza un segnaposto che
 * sembrerebbe un'icona. Va usata dentro una `Row`: disegna l'immagine e lo
 * stacco dal testo.
 */
@Composable
fun RowIconBadge(rowId: String, size: Dp) {
    val icon = LocalRowIcons.current[rowId] ?: return
    val fileName = icon.image
    val emoji = icon.emoji
    val symbol = icon.symbol
    if (fileName != null) {
        val context = LocalPlatformContext.current
        val store = remember(context) { PageImageStore(context) }
        PageImage(
            fileName = fileName,
            store = store,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(3.dp))
        )
    } else if (symbol != null) {
        SymbolIcon(symbol, size)
    } else if (emoji != null) {
        // L'emoji scelta col selettore, grande quanto l'immagine: in sp
        // calcolati dalla misura, così non esce dal posto con i caratteri grandi.
        val fontSize = with(LocalDensity.current) { (size * 0.82f).toSp() }
        Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
            Text(emoji, fontSize = fontSize, maxLines = 1, fontFamily = emojiFontFamily)
        }
    } else {
        return
    }
    Spacer(modifier = Modifier.size(6.dp))
}

/** Le schermate dentro le finestre a scomparsa, come su Notion. */
private enum class SettingsPage {
    ROOT,
    EDIT_PROPERTIES,
    LAYOUT,
    /**
     * I raggruppamenti della **bacheca**: le colonne e le fasce. Dal
     * 28/10/2026 un elenco di regole, quante se ne vogliono, come quello della
     * tabella (`TABLE_GROUP`); "Sub groups" non c'è più.
     */
    GROUP_BY,
    /** I raggruppamenti della **tabella**: gruppi dentro i gruppi, quanti se ne vogliono. */
    TABLE_GROUP,
    /** "Add a group": le proprietà con cui fare un livello in più. */
    GROUP_ADD,
    /** Una regola: la sua proprietà, "Hide empty groups", "Date by" e "Remove group". */
    GROUP_RULE,
    /** Il testo lungo nelle celle: tre puntini, colonna allargata o a capo (dal 30/09/2026). */
    CELL_OVERFLOW,
    /** Quali colonne si vedono nella tabella e quali no. */
    PROPERTY_VISIBILITY,
    DATE_PROPERTY,
    /** Cosa mostra la parte alta delle schede della galleria. */
    CARD_PREVIEW,
    /** Quanto sono grandi le schede della galleria. */
    CARD_SIZE,
    /** Una delle voci del grafico: quale, lo dice `ChartSettingsPage`. */
    CHART
}

/** Le schermate dentro la finestra delle azioni di una riga. */
private enum class RowActionsPage { ROOT, PROPERTIES }

/**
 * **"Move up" e "Move down"** (dal 29/09/2026): la riga sopra quella che
 * le sta sopra, o sotto quella che le sta sotto. Null = grigia (è già in
 * cima o in fondo, c'è un ordinamento o il database è bloccato); `hint`
 * dice perché, quando non si capisce da solo.
 */
private class RowMoveActions(val onUp: (() -> Unit)?, val onDown: (() -> Unit)?, val hint: String?)

/**
 * Le azioni su una singola riga, tenendo premuto sul suo nome.
 *
 * C'è l'icona della pagina, come l'"Edit icon" di Notion. Su Notion
 * questa finestra ha anche Add to Favorites, Comment, Copy link,
 * Duplicate e Move to: mancano perché sarebbero etichette senza niente
 * dietro — i commenti non esistono nell'app, un collegamento da copiare
 * non esiste senza indirizzi, e spostare una riga in un altro database
 * ne perderebbe i valori, visto che le colonne sono diverse. "Delete"
 * cancella la riga per davvero, senza passare dal cestino.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RowActionsSheet(
    row: DatabaseRowEntity,
    /** Solo dalla tabella, dove l'ordine delle righe si vede: altrove null e le voci non ci sono. */
    move: RowMoveActions? = null,
    /** "Create sub row": solo dalla tabella, dove le sotto-righe si vedono. */
    onCreateSubRow: (() -> Unit)? = null,
    /** "Edit event": solo nel calendario (dal 30/09/2026, il time-blocking). */
    onEditEvent: (() -> Unit)? = null,
    columns: List<DatabaseColumnEntity>,
    cellValues: Map<Pair<String, String>, String>,
    onDismiss: () -> Unit,
    onSetCellValue: (DatabaseColumnEntity, String) -> Unit,
    onAddOption: (DatabaseColumnEntity, String) -> Unit,
    onRenameOption: (String, String, String) -> Unit,
    onSetOptionColor: (String, String, String) -> Unit,
    onDeleteOption: (String, String) -> Unit,
    onMoveOptions: (String, List<String>) -> Unit,
    onSetOptionSecondColor: (String, String, String?, Boolean) -> Unit,
    /** Se la pagina della riga ha già un'icona: la voce dice "Change" invece di "None". */
    hasIcon: Boolean,
    /** Spenta con la pagina bloccata o nel cestino: l'icona fa parte del contenuto. */
    iconEditable: Boolean,
    /**
     * Database semplice: la finestra diventa **la scheda della riga** —
     * il nome da scrivere in cima e subito sotto tutte le proprietà, senza
     * la voce dell'icona (una riga senza pagina non ha dove metterla).
     * È quello che si apre toccando una riga, in qualunque vista.
     */
    simple: Boolean = false,
    onTitleChange: (String) -> Unit = {},
    /**
     * **"Open as…"** (dal 12/11/2026, solo sul PC, come in Notion): la pagina
     * della riga in Side peek, Center peek, pagina intera o una scheda nuova.
     * Null sul telefono e per i database semplici: la voce non c'è.
     */
    onOpenAs: ((OpenMode) -> Unit)? = null,
    onEditIcon: () -> Unit,
    /**
     * "Reposition cover": l'inquadratura della copertina nella scheda della
     * galleria. Null dove non ha senso (le altre viste, niente copertina).
     */
    onRepositionCover: (() -> Unit)? = null,
    onDelete: () -> Unit
) {
    var page by remember(row.id) { mutableStateOf(RowActionsPage.ROOT) }

    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (page != RowActionsPage.ROOT) {
                    IconButton(onClick = { page = RowActionsPage.ROOT }) {
                        Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = Strings.back)
                    }
                }
                Text(
                    text = when {
                        page != RowActionsPage.ROOT -> DbStrings.editProperty
                        simple -> DbStrings.row
                        else -> DbStrings.actions
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.size(12.dp))

            when (page) {
                RowActionsPage.ROOT -> if (simple) {
                    SimpleRowNameField(
                        row = row,
                        editable = iconEditable,
                        onTitleChange = onTitleChange
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    RowPropertiesList(
                        row = row,
                        columns = columns,
                        cellValues = cellValues,
                        onSetCellValue = onSetCellValue,
                        onAddOption = onAddOption,
                        onRenameOption = onRenameOption,
                        onSetOptionColor = onSetOptionColor,
                        onDeleteOption = onDeleteOption,
                        onMoveOptions = onMoveOptions,
                        onSetOptionSecondColor = onSetOptionSecondColor
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    if (move != null) {
                        RowMoveGroup(move, onCreateSubRow)
                        Spacer(modifier = Modifier.size(12.dp))
                    }
                    SheetGroup {
                        if (onEditEvent != null) {
                            SheetAction(Icons.Filled.Event, DbStrings.editEvent) { onEditEvent() }
                            HorizontalDivider()
                        }
                        SheetAction(Icons.Filled.Delete, Strings.delete, isDestructive = true) {
                            onDelete()
                        }
                    }
                    Spacer(modifier = Modifier.size(20.dp))
                    Text(
                        text = DbStrings.created(Formats.dateTime(row.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = DbStrings.lastEdited(Formats.dateTime(row.updatedAt ?: row.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    if (move != null) {
                        RowMoveGroup(move, onCreateSubRow)
                        Spacer(modifier = Modifier.size(12.dp))
                    }
                    SheetGroup {
                        if (onEditEvent != null) {
                            SheetAction(Icons.Filled.Event, DbStrings.editEvent) { onEditEvent() }
                            HorizontalDivider()
                        }
                        if (onOpenAs != null) {
                            OpenAsMenu(onOpenAs) { open -> SheetAction(Icons.Filled.OpenInNew, Strings.openAs, onClick = open) }
                            HorizontalDivider()
                        }
                        // Come l'"Edit icon" delle azioni di una riga su
                        // Notion: l'icona della pagina senza doverla aprire.
                        SettingsRow(
                            icon = Icons.Filled.Image,
                            label = EditorStrings.icon,
                            value = if (hasIcon) DbStrings.change else DbStrings.none,
                            enabled = iconEditable,
                            onClick = onEditIcon
                        )
                        HorizontalDivider()
                        if (onRepositionCover != null) {
                            SheetAction(Icons.Filled.OpenWith, DbStrings.repositionCover) {
                                onRepositionCover()
                            }
                            HorizontalDivider()
                        }
                        SettingsRow(
                            icon = Icons.Filled.FormatListBulleted,
                            label = DbStrings.editProperty,
                            value = columns.size.toString(),
                            onClick = { page = RowActionsPage.PROPERTIES }
                        )
                        HorizontalDivider()
                        SheetAction(Icons.Filled.Delete, Strings.delete, isDestructive = true) {
                            onDelete()
                        }
                    }

                    Spacer(modifier = Modifier.size(20.dp))
                    Text(
                        text = DbStrings.created(Formats.dateTime(row.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = DbStrings.lastEdited(Formats.dateTime(row.updatedAt ?: row.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                RowActionsPage.PROPERTIES -> RowPropertiesList(
                    row = row,
                    columns = columns,
                    cellValues = cellValues,
                    onSetCellValue = onSetCellValue,
                    onAddOption = onAddOption,
                    onRenameOption = onRenameOption,
                    onSetOptionColor = onSetOptionColor,
                    onDeleteOption = onDeleteOption,
                    onMoveOptions = onMoveOptions,
                    onSetOptionSecondColor = onSetOptionSecondColor
                )
            }

            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

/** "Move up" e "Move down", in cima alle azioni della riga; la finestra resta aperta per spostarla ancora. */
@Composable
private fun RowMoveGroup(move: RowMoveActions, onCreateSubRow: (() -> Unit)? = null) {
    SheetGroup {
        // **"Create sub row"** (dal 29/09/2026): una riga nuova sotto questa.
        if (onCreateSubRow != null) {
            SheetAction(Icons.Filled.SubdirectoryArrowRight, DbStrings.createSubRow) { onCreateSubRow() }
            HorizontalDivider()
        }
        SheetAction(Icons.Filled.ArrowUpward, EditorStrings.moveUp, enabled = move.onUp != null) {
            move.onUp?.invoke()
        }
        HorizontalDivider()
        SheetAction(Icons.Filled.ArrowDownward, EditorStrings.moveDown, enabled = move.onDown != null) {
            move.onDown?.invoke()
        }
    }
    move.hint?.let {
        Text(
            it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp)
        )
    }
}

/**
 * Le proprietà di una riga, una per riga, ciascuna modificabile sul posto
 * come nella sua cella. Serve in due posti: "Edit property" delle azioni
 * di una riga, e la scheda di una riga di un database semplice.
 */
@Composable
private fun RowPropertiesList(
    row: DatabaseRowEntity,
    columns: List<DatabaseColumnEntity>,
    cellValues: Map<Pair<String, String>, String>,
    onSetCellValue: (DatabaseColumnEntity, String) -> Unit,
    onAddOption: (DatabaseColumnEntity, String) -> Unit,
    onRenameOption: (String, String, String) -> Unit,
    onSetOptionColor: (String, String, String) -> Unit,
    onDeleteOption: (String, String) -> Unit,
    onMoveOptions: (String, List<String>) -> Unit,
    onSetOptionSecondColor: (String, String, String?, Boolean) -> Unit
) {
    Column {
    if (columns.isEmpty()) {
        Text(
            DbStrings.noPropertiesYet,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp)
        )
    }
    columns.forEach { column ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "Hide icon" (dal 27/10/2026): resta il suo posto, così i nomi stanno in colonna.
            if (column.hideIcon) {
                Spacer(modifier = Modifier.size(18.dp))
            } else {
                Icon(
                    column.type.icon(),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = DbStrings.columnName(column.name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(110.dp),
                maxLines = 1
            )
            Spacer(modifier = Modifier.size(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                CellContent(
                    row = row,
                    column = column,
                    value = cellValues[row.id to column.id] ?: "",
                    onValueChange = { onSetCellValue(column, it) },
                    onAddOption = { onAddOption(column, it) },
                    onRenameOption = onRenameOption,
                    onSetOptionColor = onSetOptionColor,
                    onDeleteOption = onDeleteOption,
                    onMoveOptions = onMoveOptions,
                    onSetOptionSecondColor = onSetOptionSecondColor
                )
            }
        }
    }
    }
}

/**
 * **Le proprietà in cima alla pagina di una riga** (dal 26/09/2026, come su
 * Notion): sotto il titolo, una per riga, con l'icona del tipo e il nome, e
 * ciascuna si modifica sul posto come nella sua cella. Ci sono **anche
 * quelle nascoste nella tabella**, con accanto l'occhio sbarrato: nascosta
 * vuol dire "non in quella griglia", non "senza valore". Sotto, "Add a
 * property" e la linea che le separa dal testo della pagina.
 *
 * Scrivere qui è scrivere nella riga: la tabella, la bacheca e le altre viste
 * si aggiornano da sole, e al contrario. Non disegna niente per le pagine che
 * non sono quella di una riga.
 */
@Composable
fun RowPageProperties(
    pageId: String,
    factory: ViewModelFactory,
    /** Falso con la pagina bloccata o nel cestino: le proprietà si leggono e basta. */
    editable: Boolean,
    modifier: Modifier = Modifier,
    /** "Page link": apre la pagina collegata. */
    onOpenPage: (String) -> Unit = {}
) {
    val row by remember(pageId) { factory.pageRepository.observeRowOfPage(pageId) }
        .collectAsStateWithLifecycle(initialValue = null)
    val current = row ?: return
    val databaseId = current.pageId
    // Un ViewModel suo, "leggero": colonne, righe e celle, senza le
    // anteprime della galleria (vedi `DatabaseViewModel.load`).
    val viewModel: DatabaseViewModel = viewModel(factory = factory, key = "row-properties-$databaseId")
    LaunchedEffect(databaseId) { viewModel.load(databaseId, light = true) }
    val state by viewModel.tableState.collectAsStateWithLifecycle()
    val dataPage by viewModel.dataPage.collectAsStateWithLifecycle()
    if (dataPage?.isSimpleDatabase == true) return
    // Il database bloccato blocca anche le sue proprietà, come nella tabella.
    val canEdit = editable && dataPage?.isLocked != true
    var addingProperty by remember { mutableStateOf(false) }
    val linkedPages by viewModel.linkedPages.collectAsStateWithLifecycle()
    // Le formule (dal 28/10/2026): toccandone una, la sua finestra, con l'anteprima su questa riga.
    var editingFormula by remember { mutableStateOf<DatabaseColumnEntity?>(null) }
    val formulaEditor: ((DatabaseColumnEntity, String?) -> Unit)? = remember(canEdit) {
        if (canEdit) { { column, _ -> editingFormula = column } } else null
    }
    editingFormula?.let { column ->
        FormulaEditorFor(viewModel, column, current.id, onDismiss = { editingFormula = null })
    }

    RelationHost(viewModel = viewModel, onOpenPage = onOpenPage) {
    PageLinkHost(factory = factory, linkedPages = linkedPages, onOpenPage = onOpenPage) {
    val cellLinkLabels = remember(state.linkLabels, viewModel) { CellLinkLabels(state.linkLabels, viewModel::setCellLinkLabel) }
    CompositionLocalProvider(
        LocalDatabaseLocked provides !canEdit,
        LocalNameColumnTitle provides dataPage?.nameColumnTitle,
        LocalCellLinkLabels provides cellLinkLabels,
        LocalFormulaValues provides state.formulas,
        LocalFormulaEditor provides formulaEditor
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                // Le proprietà stanno nella pagina che scorre: anche qui la
                // cella su cui si scrive resta sopra la tastiera.
                .keepFocusedAboveKeyboard()
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = if (canEdit) {
                    Modifier
                } else {
                    // Bloccate, nemmeno i tag, le date e le caselle si
                    // aprono: i tocchi si fermano qui.
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                            }
                        }
                    }
                }
            ) {
                state.columns.forEach { column ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // "Hide icon" (dal 27/10/2026): resta il suo posto, così i nomi stanno in colonna.
                        if (column.hideIcon) {
                            Spacer(modifier = Modifier.size(18.dp))
                        } else {
                            Icon(
                                column.type.icon(),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.size(10.dp))
                        Row(
                            modifier = Modifier.width(120.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = DbStrings.columnName(column.name),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (column.hidden) {
                                Spacer(modifier = Modifier.size(4.dp))
                                Icon(
                                    Icons.Filled.VisibilityOff,
                                    contentDescription = TemplateStrings.hiddenInView,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.size(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            CellContent(
                                row = current,
                                column = column,
                                value = state.cellValues[current.id to column.id] ?: "",
                                onValueChange = { viewModel.setCellValue(current, column, it) },
                                onAddOption = { viewModel.addSelectOption(column.id, it) },
                                onRenameOption = { columnId, old, new -> viewModel.renameSelectOption(columnId, old, new) },
                                onSetOptionColor = { columnId, label, hex -> viewModel.setSelectOptionColor(columnId, label, hex) },
                                onDeleteOption = { columnId, label -> viewModel.deleteSelectOption(columnId, label) },
                                onMoveOptions = { columnId, labels -> viewModel.moveSelectOptions(columnId, labels) },
                                onSetOptionSecondColor = { columnId, label, hex, line -> viewModel.setSelectOptionSecondColor(columnId, label, hex, line) }
                            )
                        }
                    }
                }
            }
            if (canEdit) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { addingProperty = true }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        TemplateStrings.addAProperty,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.size(8.dp))
            HorizontalDivider()
        }
    }
    }
    }

    if (addingProperty) {
        PropertySheet(
            target = PropertyTarget.New(),
            viewModel = viewModel,
            onDismiss = { addingProperty = false },
            onSave = { name, type, optionsJson, _, _, twoWayName ->
                viewModel.addColumn(name, type, optionsJson, twoWayName)
                addingProperty = false
            },
            onDelete = { addingProperty = false }
        )
    }
}

/**
 * Il nome di una riga di un database semplice, in cima alla sua scheda.
 *
 * Nelle altre viste il nome non si scrive sul posto — in un database
 * normale si apre la pagina e lo si scrive lì — e una riga senza pagina
 * ha bisogno di un altro posto: questo. Mentre si scrive comanda il testo
 * locale, non quello che torna dal database, per la stessa ragione del
 * titolo del database: il giro di ritorno arriva dopo il tasto
 * successivo, e riscrivendo il campo si mangerebbe il carattere.
 */
@Composable
private fun SimpleRowNameField(
    row: DatabaseRowEntity,
    editable: Boolean,
    onTitleChange: (String) -> Unit
) {
    var name by remember(row.id) { mutableStateOf(row.title) }
    val focusManager = LocalFocusManager.current
    AutoCaps {
        OutlinedTextField(
            value = name,
            onValueChange = { typed ->
                // Niente a capo nel nome, come nella cella della tabella:
                // l'Invio chiude, e un a capo incollato si toglie qui.
                val singleLine = typed.replace("\n", "")
                name = singleLine
                onTitleChange(singleLine)
            },
            label = { Text(nameColumnLabel()) },
            placeholder = { Text(Strings.untitled) },
            singleLine = true,
            readOnly = !editable,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Le impostazioni del database, nella finestra che scorre dal basso.
 *
 * Su Notion qui ci sono una dozzina di voci (Layout, Filter, Sort,
 * Group, Automations...). Ci sono solo quelle che funzionano davvero:
 * le altre arrivano man mano che vengono costruite, perché una voce che
 * non fa niente è peggio di una voce assente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    /** Se il database ha già un'icona: la voce dice "Change" invece di DbStrings.none. */
    hasIcon: Boolean = false,
    /** Null con la pagina bloccata: l'icona fa parte del contenuto. */
    onEditIcon: (() -> Unit)? = null,
    columns: List<DatabaseColumnEntity>,
    layout: DatabaseLayout,
    groupColumn: DatabaseColumnEntity?,
    dateColumn: DatabaseColumnEntity?,
    /**
     * **I raggruppamenti, quanti se ne vogliono** (dal 28/10/2026): le regole
     * della tabella e della bacheca, nell'ordine in cui contano, e cosa farne.
     */
    tableGroupRules: List<GroupRuleSpec> = emptyList(),
    boardGroupRules: List<GroupRuleSpec> = emptyList(),
    groupActions: GroupRuleActions? = null,
    /** I gruppi di una proprietà, nell'ordine a mano della regola: per trascinarli in "Sort groups". */
    groupsOf: (DatabaseColumnEntity, GroupRuleSpec) -> List<RowGroup> = { _, _ -> emptyList() },
    /** "Color columns" della bacheca (dal 30/09/2026). */
    colorBoardCards: Boolean = false,
    onSetColorBoardCards: ((Boolean) -> Unit)? = null,
    /** "Hide New" della bacheca (dal 26/10/2026). */
    boardHideNew: Boolean = false,
    onSetBoardHideNew: ((Boolean) -> Unit)? = null,
    /** Il testo lungo nelle celle della tabella (dal 30/09/2026). */
    cellOverflow: CellOverflow = CellOverflow.TRUNCATE,
    onSetCellOverflow: ((CellOverflow) -> Unit)? = null,
    freezeFirstColumn: Boolean = false,
    onSetFreezeFirstColumn: ((Boolean) -> Unit)? = null,
    freezeFirstRow: Boolean = false,
    onSetFreezeFirstRow: ((Boolean) -> Unit)? = null,
    /** Nasconde o rimostra una colonna, e tutte insieme. */
    onSetColumnHidden: (String, Boolean) -> Unit,
    onSetAllColumnsHidden: (Boolean) -> Unit,
    /** Il filtro attivo, da riassumere; toccarlo apre la sua finestra. */
    filterColumn: DatabaseColumnEntity? = null,
    filterValue: String = "",
    onOpenFilter: () -> Unit,
    initialPage: SettingsPage,
    onDismiss: () -> Unit,
    onSetLayout: (DatabaseLayout) -> Unit,
    onSetDateColumn: (DatabaseColumnEntity) -> Unit,
    /** Anteprima e dimensione delle schede: contano solo per la galleria. */
    galleryPreview: GalleryCardPreview = GALLERY_DEFAULT_PREVIEW,
    gallerySize: GalleryCardSize = GALLERY_DEFAULT_SIZE,
    onSetGalleryPreview: (GalleryCardPreview) -> Unit = {},
    onSetGallerySize: (GalleryCardSize) -> Unit = {},
    /**
     * Database semplice: niente "Card preview" nella galleria (copertina
     * e testo sono delle pagine delle righe, che qui non esistono), e in
     * fondo una riga che dice che cos'è.
     */
    simple: Boolean = false,
    onEditProperty: (DatabaseColumnEntity) -> Unit,
    onAddProperty: () -> Unit,
    /** Null quando il database è a schermo intero: lì il titolo c'è sempre. */
    onSetShowTitle: ((Boolean) -> Unit)? = null,
    showTitle: Boolean = true,
    /**
     * Vista bloccata ("Lock view"): le voci che cambiano
     * l'impaginazione si vedono ma non rispondono. Cancellare invece
     * resta possibile — il lucchetto protegge come è messo insieme il
     * database, non la sua esistenza, e un database che non si può
     * buttare finché non si ricorda dove si era girata la chiave
     * sarebbe una trappola.
     */
    viewLocked: Boolean = false,

    /** La vista Chart: le sue impostazioni, e la finestrella dei tipi. */
    chartSettings: ChartSettings = ChartSettings(),
    onSetChartSettings: (ChartSettings) -> Unit = {},
    onPickChartType: () -> Unit = {}
) {
    var page by remember(initialPage) { mutableStateOf(initialPage) }
    // Per i gruppi da trascinare in "Sort groups": vicino ai bordi la finestra scorre da sola.
    val sheetScroll = rememberScrollState()
    var sheetViewport by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    // La regola che si sta guardando in `GROUP_RULE`, per la sua proprietà.
    var groupRuleColumn by remember { mutableStateOf<String?>(null) }
    val board = layout == DatabaseLayout.BOARD
    val groupRules = if (board) boardGroupRules else tableGroupRules
    val groupsPage = if (board) SettingsPage.GROUP_BY else SettingsPage.TABLE_GROUP
    var chartPage by remember { mutableStateOf(ChartSettingsPage.GROUP) }
    var editingChartRange by remember { mutableStateOf(false) }

    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp)
                .padding(horizontal = 16.dp)
                .onGloballyPositioned { sheetViewport = it.boundsInRoot() }
                .verticalScroll(sheetScroll)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (page != SettingsPage.ROOT) {
                    // Da una regola, o da "Add a group", si torna all'elenco dei gruppi.
                    IconButton(onClick = { page = if (page == SettingsPage.GROUP_ADD || page == SettingsPage.GROUP_RULE) groupsPage else SettingsPage.ROOT }) {
                        Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = Strings.back)
                    }
                }
                Text(
                    text = when (page) {
                        SettingsPage.ROOT -> Strings.settings
                        SettingsPage.EDIT_PROPERTIES -> DbStrings.editProperties
                        SettingsPage.LAYOUT -> DbStrings.layout
                        SettingsPage.GROUP_BY -> DbStrings.groupBy
                        SettingsPage.TABLE_GROUP -> DbStrings.group
                        SettingsPage.GROUP_ADD -> DbStrings.addGroup
                        SettingsPage.GROUP_RULE -> columns.find { it.id == groupRuleColumn }?.name?.let(DbStrings::columnName) ?: DbStrings.group
                        SettingsPage.CELL_OVERFLOW -> DbStrings.longText
                        SettingsPage.PROPERTY_VISIBILITY -> DbStrings.propertyVisibility
                        SettingsPage.DATE_PROPERTY -> DbStrings.dateProperty
                        SettingsPage.CARD_PREVIEW -> DbStrings.cardPreview
                        SettingsPage.CARD_SIZE -> DbStrings.cardSize
                        SettingsPage.CHART -> chartPageTitle(chartPage, chartSettings)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.size(12.dp))

            when (page) {
                SettingsPage.ROOT -> {
                    // **Il grafico** (dal 28/09/2026): sotto "Layout" il tipo,
                    // gli assi e lo stile, come su Notion; il resto dopo.
                    if (layout == DatabaseLayout.CHART) {
                        SheetGroup {
                            SettingsRow(
                                icon = layout.icon(),
                                label = DbStrings.layout,
                                enabled = !viewLocked,
                                value = layout.displayName(),
                                onClick = { page = SettingsPage.LAYOUT }
                            )
                        }
                        Spacer(modifier = Modifier.size(12.dp))
                        ChartSettingsSection(
                            settings = chartSettings,
                            columns = columns,
                            enabled = !viewLocked,
                            onChange = onSetChartSettings,
                            onOpenPage = {
                                chartPage = it
                                page = SettingsPage.CHART
                            },
                            onPickType = onPickChartType,
                            onEditRange = { editingChartRange = true }
                        )
                    }
                    SheetGroup {
                        if (layout != DatabaseLayout.CHART) {
                            SettingsRow(
                                icon = layout.icon(),
                                label = DbStrings.layout,
                                enabled = !viewLocked,
                                value = layout.displayName(),
                                onClick = { page = SettingsPage.LAYOUT }
                            )
                            HorizontalDivider()
                        }
                        // Il filtro non è una schermata dentro le
                        // impostazioni ma **la stessa finestra**
                        // dell'icona nella barra: due copie si
                        // sarebbero separate al primo cambiamento.
                        SettingsRow(
                            icon = Icons.Filled.FilterList,
                            label = DbStrings.filter,
                            enabled = !viewLocked,
                            value = filterSummary(filterColumn, filterValue),
                            onClick = onOpenFilter
                        )
                        // "Group by" riguarda solo la bacheca: nelle
                        // altre viste non avrebbe alcun effetto.
                        if (layout == DatabaseLayout.BOARD) {
                            HorizontalDivider()
                            SettingsRow(
                                icon = Icons.Filled.ViewColumn,
                                label = DbStrings.groupBy,
                                enabled = !viewLocked,
                                value = groupRulesSummary(boardGroupRules, columns),
                                onClick = { page = SettingsPage.GROUP_BY }
                            )
                            // "Hide empty groups" (dal 21/10/2026) e le fasce ("Sub groups",
                            // dal 14/10/2026) stanno dentro "Group by" dal 28/10/2026: ogni
                            // regola ha il suo, e le fasce sono le regole dopo la prima.
                            // **"Hide New"** (dal 26/10/2026, chiesto dall'utente:
                            // "tutte quelle finestre New mi danno parecchio
                            // fastidio"): niente "+ New" sotto le schede.
                            if (groupColumn != null && onSetBoardHideNew != null) {
                                HorizontalDivider()
                                SettingsToggleRow(
                                    icon = Icons.Filled.Add,
                                    label = DbStrings.hideNew,
                                    checked = boardHideNew,
                                    enabled = !viewLocked,
                                    onCheckedChange = onSetBoardHideNew
                                )
                            }
                            // **"Color columns"** (dal 30/09/2026, chiesto
                            // dall'utente): ogni colonna, con le sue schede,
                            // del colore del suo tag.
                            if (onSetColorBoardCards != null) {
                                HorizontalDivider()
                                SettingsToggleRow(
                                    icon = Icons.Filled.Palette,
                                    label = DbStrings.colorColumns,
                                    checked = colorBoardCards,
                                    onCheckedChange = onSetColorBoardCards
                                )
                            }
                        }
                        // Il raggruppamento della tabella: una voce
                        // diversa da "Group by" della bacheca, perché
                        // sono due impostazioni distinte e cambiare
                        // l'una non deve rimescolare l'altra.
                        if (layout == DatabaseLayout.TABLE) {
                            HorizontalDivider()
                            SettingsRow(
                                icon = Icons.Filled.Segment,
                                label = DbStrings.group,
                                enabled = !viewLocked,
                                value = groupRulesSummary(tableGroupRules, columns),
                                onClick = { page = SettingsPage.TABLE_GROUP }
                            )
                            // "Sub groups" (dal 29/09/2026) e i due "Hide empty" stanno
                            // dentro "Group" dal 28/10/2026 (chiesto dall'utente): i
                            // gruppi dentro i gruppi sono le regole dopo la prima, e ogni
                            // regola ha il suo "Hide empty groups".
                            // **"Long text"** (dal 30/09/2026, chiesto
                            // dall'utente): il testo che non ci sta finisce
                            // coi tre puntini, allarga la colonna o va a capo
                            // nella cella. Vedi `CellOverflow`.
                            if (onSetCellOverflow != null) {
                                HorizontalDivider()
                                SettingsRow(
                                    icon = Icons.Filled.WrapText,
                                    label = DbStrings.longText,
                                    value = DbStrings.cellOverflowName(cellOverflow),
                                    onClick = { page = SettingsPage.CELL_OVERFLOW }
                                )
                            }
                            // **Prima colonna e prima riga ferme** (dal
                            // 08/10/2026, chiesto dall'utente): due voci a sé.
                            if (onSetFreezeFirstColumn != null) {
                                HorizontalDivider()
                                SettingsToggleRow(
                                    icon = Icons.Filled.BorderLeft,
                                    label = DbStrings.freezeFirstColumn,
                                    checked = freezeFirstColumn,
                                    enabled = !viewLocked,
                                    onCheckedChange = onSetFreezeFirstColumn
                                )
                            }
                            if (onSetFreezeFirstRow != null) {
                                HorizontalDivider()
                                SettingsToggleRow(
                                    icon = Icons.Filled.BorderTop,
                                    label = DbStrings.freezeFirstRow,
                                    checked = freezeFirstRow,
                                    enabled = !viewLocked,
                                    onCheckedChange = onSetFreezeFirstRow
                                )
                            }
                        }
                        // Come "Group by": riguarda solo il calendario.
                        if (layout == DatabaseLayout.CALENDAR) {
                            HorizontalDivider()
                            SettingsRow(
                                icon = Icons.Filled.CalendarToday,
                                label = DbStrings.dateProperty,
                            enabled = !viewLocked,
                                value = dateColumn?.name?.let(DbStrings::columnName) ?: DbStrings.none,
                                onClick = { page = SettingsPage.DATE_PROPERTY }
                            )
                        }
                        // Le due voci della galleria, dove le mette Notion:
                        // subito sotto la vista. Sono impaginazione, quindi
                        // "Lock view" le spegne come le altre.
                        if (layout == DatabaseLayout.GALLERY) {
                            if (!simple) {
                                HorizontalDivider()
                                SettingsRow(
                                    icon = galleryPreview.icon(),
                                    label = DbStrings.cardPreview,
                                    enabled = !viewLocked,
                                    value = DbStrings.galleryPreviewName(galleryPreview),
                                    onClick = { page = SettingsPage.CARD_PREVIEW }
                                )
                            }
                            HorizontalDivider()
                            SettingsRow(
                                icon = gallerySize.icon(),
                                label = DbStrings.cardSize,
                                enabled = !viewLocked,
                                value = DbStrings.gallerySizeName(gallerySize),
                                onClick = { page = SettingsPage.CARD_SIZE }
                            )
                        }
                        HorizontalDivider()
                        SettingsRow(
                            icon = Icons.Filled.FormatListBulleted,
                            label = DbStrings.editProperties,
                            enabled = !viewLocked,
                            value = columns.size.toString(),
                            onClick = { page = SettingsPage.EDIT_PROPERTIES }
                        )
                        // Il numero accanto dice quante colonne sono
                        // sparite: è l'unico posto da cui ritrovarle,
                        // e senza quel numero non si saprebbe nemmeno
                        // che c'è qualcosa da ritrovare.
                        HorizontalDivider()
                        SettingsRow(
                            icon = Icons.Filled.Visibility,
                            label = DbStrings.propertyVisibility,
                            enabled = !viewLocked,
                            value = columns.count { it.hidden }
                                .let { if (it == 0) DbStrings.allShown else DbStrings.nHidden(it) },
                            onClick = { page = SettingsPage.PROPERTY_VISIBILITY }
                        )
                        // **L'icona del database**, solo a schermo intero (dal
                        // 21/10/2026): dentro una pagina sta nei sei puntini.
                        // Un database senza icona non ha niente da toccare
                        // accanto al nome: questa voce è il modo di
                        // mettergliene una. "Favorite", "Turn into page" e
                        // "Delete" non ci sono più: stanno nei sei puntini e,
                        // a schermo intero, nei tre puntini in alto.
                        if (onEditIcon != null) {
                            HorizontalDivider()
                            SettingsRow(
                                icon = Icons.Filled.Image,
                                label = EditorStrings.icon,
                                value = if (hasIcon) DbStrings.change else DbStrings.none,
                                onClick = onEditIcon
                            )
                        }
                        // Solo dentro una pagina: a schermo intero il
                        // titolo non è negoziabile, e offrire un
                        // interruttore che non fa niente sarebbe
                        // peggio che non offrirlo.
                        if (onSetShowTitle != null) {
                            HorizontalDivider()
                            SettingsToggleRow(
                                icon = Icons.Filled.Title,
                                label = DbStrings.showTitle,
                                checked = showTitle,
                                onCheckedChange = onSetShowTitle
                            )
                        }
                    }

                    // Un database semplice si riconosce solo da quello
                    // che non fa: una riga qui lo dice, così chi tocca
                    // una riga e non vede aprirsi una pagina sa perché.
                    if (simple) {
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(
                            text = DbStrings.simpleDatabaseNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                }

                SettingsPage.CHART -> ChartSettingsDetail(
                    page = chartPage,
                    settings = chartSettings,
                    columns = columns,
                    onChange = onSetChartSettings
                )

                SettingsPage.DATE_PROPERTY -> {
                    val dateColumns = columns.filter { it.type == ColumnType.DATE }
                    if (dateColumns.isEmpty()) {
                        Text(
                            text = DbStrings.calendarNeedsDate,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        SheetGroup {
                            dateColumns.forEachIndexed { index, column ->
                                if (index > 0) HorizontalDivider()
                                SettingsRow(
                                    icon = column.type.icon(),
                                    label = DbStrings.columnName(column.name),
                                    value = if (column.id == dateColumn?.id) "✓" else null,
                                    onClick = { onSetDateColumn(column) }
                                )
                            }
                        }
                    }
                }

                // Scelta una voce la finestra resta aperta sulla stessa
                // schermata, come per la proprietà data e "Group by": il
                // segno di spunta si sposta, e la galleria dietro cambia
                // subito, così si vede l'effetto prima di tornare indietro.
                SettingsPage.CARD_PREVIEW -> {
                    SheetGroup {
                        GalleryCardPreview.entries.forEachIndexed { index, option ->
                            if (index > 0) HorizontalDivider()
                            SettingsRow(
                                icon = option.icon(),
                                label = DbStrings.galleryPreviewName(option),
                                value = if (option == galleryPreview) "✓" else null,
                                onClick = { onSetGalleryPreview(option) }
                            )
                        }
                    }
                }

                SettingsPage.CARD_SIZE -> {
                    SheetGroup {
                        GalleryCardSize.entries.forEachIndexed { index, option ->
                            if (index > 0) HorizontalDivider()
                            SettingsRow(
                                icon = option.icon(),
                                label = DbStrings.gallerySizeName(option),
                                value = if (option == gallerySize) "✓" else null,
                                onClick = { onSetGallerySize(option) }
                            )
                        }
                    }
                }

                SettingsPage.TABLE_GROUP, SettingsPage.GROUP_BY -> GroupRulesPage(
                    board = board,
                    rules = groupRules,
                    columns = columns,
                    enabled = !viewLocked && groupActions != null,
                    actions = groupActions,
                    onAdd = { page = SettingsPage.GROUP_ADD },
                    onOpen = { columnId ->
                        groupRuleColumn = columnId
                        page = SettingsPage.GROUP_RULE
                    },
                    groupsOf = { rule -> columns.find { it.id == rule.column }?.let { groupsOf(it, rule) }.orEmpty() },
                    scrollState = sheetScroll,
                    viewport = { sheetViewport }
                )

                SettingsPage.GROUP_ADD -> GroupAddPage(
                    rules = groupRules,
                    columns = columns,
                    onChoose = { columnId ->
                        groupActions?.add?.invoke(board, columnId)
                        page = groupsPage
                    }
                )

                SettingsPage.GROUP_RULE -> {
                    val rule = groupRules.find { it.column == groupRuleColumn }
                    if (rule == null) {
                        // Tolta (o cambiata da un'altra parte): si torna all'elenco.
                        LaunchedEffect(Unit) { page = groupsPage }
                    } else {
                        GroupRulePage(
                            board = board,
                            rule = rule,
                            rules = groupRules,
                            columns = columns,
                            groups = columns.find { it.id == rule.column }?.let { groupsOf(it, rule) }.orEmpty(),
                            scrollState = sheetScroll,
                            viewport = { sheetViewport },
                            enabled = !viewLocked && groupActions != null,
                            actions = groupActions,
                            onColumnChanged = { groupRuleColumn = it },
                            onRemoved = { page = groupsPage }
                        )
                    }
                }

                SettingsPage.CELL_OVERFLOW -> {
                    // I tre modi, nell'ordine in cui li ha chiesti l'utente.
                    SheetGroup {
                        CellOverflow.entries.forEachIndexed { index, mode ->
                            if (index > 0) HorizontalDivider()
                            SettingsRow(
                                icon = when (mode) {
                                    CellOverflow.TRUNCATE -> Icons.Filled.MoreHoriz
                                    CellOverflow.WIDEN -> Icons.Filled.SettingsEthernet
                                    CellOverflow.WRAP -> Icons.Filled.WrapText
                                },
                                label = DbStrings.cellOverflowName(mode),
                                value = if (mode == cellOverflow) "✓" else null,
                                onClick = { onSetCellOverflow?.invoke(mode) }
                            )
                        }
                    }
                }

                SettingsPage.PROPERTY_VISIBILITY -> {
                    val shown = columns.filterNot { it.hidden }
                    val hidden = columns.filter { it.hidden }

                    if (columns.isEmpty()) {
                        Text(
                            text = DbStrings.noPropertiesYet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }

                    // DbStrings.name non compare in nessuna delle due liste: è
                    // il titolo della pagina, non una proprietà, e una
                    // riga senza nome non si saprebbe più aprire.
                    if (shown.isNotEmpty()) {
                        VisibilitySectionHeader(
                            title = DbStrings.shownInTable,
                            action = DbStrings.hideAll,
                            onAction = { onSetAllColumnsHidden(true) }
                        )
                        SheetGroup {
                            shown.forEachIndexed { index, column ->
                                if (index > 0) HorizontalDivider()
                                PropertyVisibilityRow(
                                    column = column,
                                    hidden = false,
                                    onToggle = { onSetColumnHidden(column.id, true) }
                                )
                            }
                        }
                    }

                    if (hidden.isNotEmpty()) {
                        VisibilitySectionHeader(
                            title = DbStrings.hiddenInTable,
                            action = DbStrings.showAll,
                            onAction = { onSetAllColumnsHidden(false) }
                        )
                        SheetGroup {
                            hidden.forEachIndexed { index, column ->
                                if (index > 0) HorizontalDivider()
                                PropertyVisibilityRow(
                                    column = column,
                                    hidden = true,
                                    onToggle = { onSetColumnHidden(column.id, false) }
                                )
                            }
                        }
                    }

                    if (columns.isNotEmpty() && hidden.isEmpty()) {
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = DbStrings.nothingHidden,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SettingsPage.LAYOUT -> {
                    // Una griglia di riquadri come su Notion. Ci sono solo
                    // le visualizzazioni costruite davvero: le altre
                    // compaiono man mano, perché un riquadro che non
                    // cambia niente sarebbe una bugia.
                    DatabaseLayout.entries.chunked(3).forEach { rowLayouts ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            rowLayouts.forEach { candidate ->
                                LayoutChoice(
                                    layout = candidate,
                                    isSelected = candidate == layout,
                                    onClick = { onSetLayout(candidate) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowLayouts.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                SettingsPage.EDIT_PROPERTIES -> {
                    SheetGroup {
                        columns.forEach { column ->
                            SettingsRow(
                                icon = column.type.icon(),
                                label = DbStrings.columnName(column.name),
                                value = column.type.displayName(),
                                onClick = { onEditProperty(column) }
                            )
                            HorizontalDivider()
                        }
                        SettingsRow(
                            icon = Icons.Filled.Add,
                            label = DbStrings.newProperty,
                            value = null,
                            onClick = onAddProperty
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.size(24.dp))
        }
    }

    if (editingChartRange) {
        ChartRangeDialog(
            settings = chartSettings,
            onConfirm = { min, max ->
                editingChartRange = false
                onSetChartSettings(chartSettings.copy(rangeMin = min, rangeMax = max))
            },
            onDismiss = { editingChartRange = false }
        )
    }
}

/**
 * Il titolo di una delle due liste di "Property visibility", con la
 * scorciatoia per spostarle tutte di là in un colpo solo.
 */
@Composable
private fun VisibilitySectionHeader(
    title: String,
    action: String,
    onAction: () -> Unit
) {
    Spacer(modifier = Modifier.size(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = action,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable { onAction() }
                .padding(vertical = 6.dp, horizontal = 4.dp)
        )
    }
    Spacer(modifier = Modifier.size(6.dp))
}

/**
 * Una proprietà dentro "Property visibility": il suo nome e l'occhio
 * che la sposta nell'altra lista.
 *
 * Tutta la riga è toccabile, non solo l'occhio: su un telefono un
 * bersaglio da venti punti in fondo a destra si sbaglia, e qui non c'è
 * niente di irreversibile da proteggere con la mira.
 */
@Composable
private fun PropertyVisibilityRow(
    column: DatabaseColumnEntity,
    hidden: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            column.type.icon(),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.size(14.dp))
        Text(
            text = DbStrings.columnName(column.name),
            color = if (hidden) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                Color.Unspecified
            },
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (hidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = if (hidden) {
                DbStrings.showInTable(DbStrings.columnName(column.name))
            } else {
                DbStrings.hideFromTable(DbStrings.columnName(column.name))
            },
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * La finestra che scorre dal basso per creare o modificare una
 * proprietà: nome in alto, poi l'elenco dei tipi con la ricerca, come
 * su Notion. Una sola finestra per entrambi i casi, così creazione e
 * modifica non possono allontanarsi col tempo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PropertySheet(
    target: PropertyTarget,
    /** Per "Relation" e "Rollup": le colonne di qua, i database collegati, quelli fra cui scegliere. */
    viewModel: DatabaseViewModel,
    onDismiss: () -> Unit,
    /** Nome, tipo, opzioni, Number, Duration e, per una Relation nei due sensi, il nome della gemella. */
    onSave: (String, ColumnType, String, NumberSettings?, DurationUnit?, String?) -> Unit,
    onDelete: () -> Unit
) {
    val existing = (target as? PropertyTarget.Existing)?.column
    val mode = (target as? PropertyTarget.Existing)?.mode ?: PropertyMode.FULL
    // **Tutto in "Edit property"** (dal 21/10/2026, chiesto dall'utente): il
    // nome, sotto tutti i tipi, e sotto il tipo scelto le sue impostazioni
    // (Number, Duration, Relation, Rollup). Dal 26/09/2026 il tipo si cambiava
    // da una voce a sé, "Change type", che non c'è più.
    var number by remember(existing?.id) { mutableStateOf(existing?.numberSettings() ?: NumberSettings()) }
    var duration by remember(existing?.id) { mutableStateOf(durationFormatOf(existing?.durationFormat)) }

    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var selectedType by remember(existing?.id) {
        mutableStateOf(existing?.type ?: (target as? PropertyTarget.New)?.initialType ?: ColumnType.TEXT)
    }
    var typeQuery by remember(existing?.id) { mutableStateOf("") }
    val typeOrder = remember(existing?.id) {
        existing?.type?.let { current -> listOf(current) + ColumnType.entries.filter { it != current } } ?: ColumnType.entries
    }
    // I tag esistenti si portano dietro etichette e colori, e da qui non
    // si toccano: si salvano così come sono.
    val optionsJson = existing?.optionsJson ?: "[]"

    // Il nome può restare vuoto: in quel caso la proprietà prende il
    // nome del tipo, come su Notion. Prima il pulsante di conferma in
    // quel caso non faceva niente, in silenzio.
    val effectiveName = name.ifBlank { DbStrings.englishTypeName(selectedType) }

    // "Relation" e "Rollup" (dal 21/10/2026): a che cosa si collega, che cosa calcola.
    val tableState by viewModel.tableState.collectAsStateWithLifecycle()
    val related by viewModel.related.collectAsStateWithLifecycle()
    val dataPage by viewModel.dataPage.collectAsStateWithLifecycle()
    val existingRelation = existing?.relationConfig()
    var relationDb by remember(existing?.id) { mutableStateOf(existingRelation?.database) }
    var twoWay by remember(existing?.id) { mutableStateOf(existingRelation?.twoWay == true) }
    var twinName by remember(existing?.id) { mutableStateOf<String?>(null) }
    var rollup by remember(existing?.id) { mutableStateOf(existing?.rollupConfig()) }
    var databases by remember { mutableStateOf<List<PageRepository.LinkableDatabase>?>(null) }
    val wantsDatabases = selectedType == ColumnType.RELATION && existingRelation == null
    LaunchedEffect(wantsDatabases) {
        if (wantsDatabases && databases == null) viewModel.loadDatabases { databases = it }
    }
    val relationColumns = tableState.columns.filter { it.type == ColumnType.RELATION && it.id != existing?.id }
    // Senza una scelta, il Rollup legge la prima Relation, col nome delle righe.
    val rollupNow = rollup?.takeIf { r -> relationColumns.any { it.id == r.relation } }
        ?: relationColumns.firstOrNull()?.let { RollupConfig(it.id) }
    val relationDbName = relationDb?.let { id -> databases?.find { it.page.id == id }?.page?.title ?: related[id]?.page?.title }
    // La gemella si chiama come questo database, se non le si dà un altro nome.
    val twinNameNow = twinName
        ?: existingRelation?.reverse?.let { rev -> related[existingRelation.database]?.columns?.find { it.id == rev }?.name }
        ?: dataPage?.title?.ifBlank { null }
        ?: DbStrings.englishTypeName(ColumnType.RELATION)
    // "Button" (dal 27/10/2026): le azioni, scelte da "Edit actions" qui sotto.
    var buttonConfig by remember(existing?.id) {
        mutableStateOf(existing?.takeIf { it.type == ColumnType.BUTTON }?.let { ButtonConfig.parse(it.optionsJson) } ?: ButtonConfig())
    }
    var editingActions by remember { mutableStateOf(false) }
    // "Formula" (dal 28/10/2026): la formula, scritta da "Edit formula" qui sotto.
    var formula by remember(existing?.id) {
        mutableStateOf(existing?.takeIf { it.type == ColumnType.FORMULA }?.let { FormulaConfig.parse(it.optionsJson).expression } ?: "")
    }
    var editingFormula by remember { mutableStateOf(false) }
    val saveOptions = when (selectedType) {
        ColumnType.RELATION -> relationDb?.let { encodeRelationConfig(RelationConfig(database = it)) } ?: "[]"
        ColumnType.ROLLUP -> rollupNow?.let(::encodeRollupConfig) ?: "[]"
        ColumnType.BUTTON -> ButtonConfig.encode(buttonConfig)
        ColumnType.FORMULA -> FormulaConfig.encode(FormulaConfig(formula))
        // Lasciando una Relation, un Rollup, un bottone o una formula, la loro configurazione non serve più.
        else -> if (existing?.type == ColumnType.RELATION || existing?.type == ColumnType.ROLLUP ||
            existing?.type == ColumnType.BUTTON || existing?.type == ColumnType.FORMULA
        ) {
            "[]"
        } else {
            optionsJson
        }
    }
    // Una Relation senza database non avrebbe righe da collegare.
    val canSave = selectedType != ColumnType.RELATION || relationDb != null
    val linkSettings: @Composable () -> Unit = {
        when (selectedType) {
            ColumnType.RELATION -> RelationSettingsSection(
                databases = databases,
                database = relationDb,
                databaseName = relationDbName,
                fixed = existingRelation != null,
                onDatabase = { relationDb = it },
                twoWay = twoWay,
                onTwoWay = { twoWay = it },
                twinName = twinNameNow,
                onTwinName = { twinName = it }
            )
            ColumnType.ROLLUP -> RollupSettingsSection(
                relations = relationColumns,
                related = related,
                config = rollupNow,
                onChange = { rollup = it }
            )
            else -> Unit
        }
    }

    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Intestazione fissa, fuori dalla parte che scorre: l'elenco
            // dei tipi è lungo, e con i pulsanti in fondo si arrivava a
            // scegliere un tipo senza più vedere come confermare.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        existing == null -> DbStrings.newProperty
                        mode == PropertyMode.TYPE -> DbStrings.changeType
                        else -> DbStrings.editProperty
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onDismiss) { Text(Strings.cancel) }
                TextButton(
                    enabled = canSave,
                    onClick = {
                        onSave(
                            effectiveName,
                            selectedType,
                            saveOptions,
                            number.takeIf { selectedType == ColumnType.NUMBER },
                            duration.takeIf { selectedType == ColumnType.DURATION },
                            twinNameNow.trim().ifBlank { DbStrings.englishTypeName(ColumnType.RELATION) }
                                .takeIf { selectedType == ColumnType.RELATION && twoWay }
                        )
                    }
                ) { Text(if (existing == null) DbStrings.add else EditorStrings.save) }
            }
            HorizontalDivider()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.size(16.dp))

            // "Change type" (dal 27/10/2026) ha solo i tipi: il nome sta in "Edit property".
            if (mode != PropertyMode.TYPE) {
            SheetTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = DbStrings.propertyName,
                leading = Icons.Filled.Notes
            )
            }

            // Qui non si scrivono le opzioni di un tag. Prima c'era un
            // campo per elencarle separate da virgola, ed era una
            // trappola: salvando, le opzioni venivano ricostruite da
            // quel testo, quindi si perdevano i colori assegnati e
            // sparivano i tag creati nel frattempo dalle celle. I tag
            // si creano e si modificano dalla cella, che è l'unico
            // posto in cui esiste anche il loro colore.

            run {
            Spacer(modifier = Modifier.size(if (mode == PropertyMode.TYPE) 0.dp else 20.dp))
            Text(
                DbStrings.type,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.size(8.dp))
            SheetTextField(
                value = typeQuery,
                onValueChange = { typeQuery = it },
                placeholder = Strings.search,
                leading = Icons.Filled.Search
            )
            Spacer(modifier = Modifier.size(8.dp))

            // Per una proprietà che c'è già, il suo tipo in cima, con le sue
            // impostazioni: senza scorrere si vede com'è adesso. Gli altri dopo,
            // nell'ordine di sempre, che non cambia scegliendone un altro.
            typeOrder
                .filter { it.displayName().contains(typeQuery, ignoreCase = true) }
                .forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = type }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            type.icon(),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(type.displayName(), modifier = Modifier.weight(1f))
                        if (type == selectedType) {
                            // Grande come l'icona del tipo: la riga scelta non si alza.
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = DbStrings.selected,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    // Sotto il tipo scelto, le sue impostazioni, dove si sta guardando:
                    // Number (formato, cifre, "Show as"), Duration (fino a quale
                    // unità), Relation e Rollup. Date non ne ha più: Date format e
                    // Time format sono in Settings, per tutta l'app (dal 16/10/2026).
                    // Solo i tipi che le hanno (corretto il 27/10/2026, l'utente: "è come se
                    // si ingrandisse e sposta di poco [...] quello sotto"): per gli altri
                    // il riquadro restava vuoto ma col suo margine sotto, e scegliendoli
                    // le righe sotto scendevano di 8 punti, per risalire scegliendone un altro.
                    if (type == selectedType && type in TYPES_WITH_SETTINGS) {
                        Column(modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)) {
                            when (type) {
                                ColumnType.NUMBER -> NumberSettingsSection(settings = number, onChange = { number = it })
                                ColumnType.DURATION -> DurationSettingsSection(format = duration, onChange = { duration = it })
                                ColumnType.BUTTON -> SheetAction(
                                    Icons.Filled.TouchApp,
                                    ButtonStrings.editActions + " · " + ButtonStrings.actionsCount(buttonConfig.steps.size)
                                ) { editingActions = true }
                                ColumnType.FORMULA -> SheetAction(
                                    Icons.Filled.Functions,
                                    FormulaStrings.editFormula + " · " + formula.trim().ifEmpty { FormulaStrings.noFormula }
                                ) { editingFormula = true }
                                else -> linkSettings()
                            }
                        }
                    }
                }
            }

            // Qui non si sposta la colonna: "Move to left" e "Move to
            // right" stanno in cima al menu che si apre tenendo premuta
            // l'intestazione, e averle in due posti voleva dire due
            // versioni della stessa cosa (in questa finestra, per di
            // più, comparivano e sparivano a seconda della colonna).
            if (existing != null && mode != PropertyMode.TYPE) {
                Spacer(modifier = Modifier.size(20.dp))
                HorizontalDivider()
                SheetAction(Icons.Filled.Delete, DbStrings.deleteProperty, isDestructive = true) {
                    onDelete()
                }
            }

            Spacer(modifier = Modifier.size(24.dp))
        }
        }
    }

    if (editingActions) {
        ButtonEditorSheet(
            initial = buttonConfig,
            inDatabase = true,
            thisPageDatabaseId = existing?.pageId ?: dataPage?.id,
            onDismiss = { editingActions = false },
            onSave = { buttonConfig = it }
        )
    }
    if (editingFormula) {
        FormulaEditorSheet(
            columnName = effectiveName,
            initial = formula,
            columns = tableState.columns.filter { it.id != existing?.id },
            nameTitle = dataPage?.nameColumnTitle,
            preview = { viewModel.previewFormula(existing?.id, it, null) },
            onDismiss = { editingFormula = false },
            onSave = { formula = it }
        )
    }
}

@Composable
private fun SheetAction(
    icon: ImageVector,
    label: String,
    isDestructive: Boolean = false,
    /** Spenta: si vede, grigia, e non risponde. */
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val tint = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        isDestructive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = pcOr(5.dp, 14.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(pcOr(18.dp, 20.dp)), tint = tint)
        Spacer(modifier = Modifier.size(pcOr(10.dp, 12.dp)))
        Text(label, color = tint)
    }
}

/**
 * La maiuscola automatica vale per il testo libero, non per indirizzi e
 * numeri: in un'email o in un URL una maiuscola iniziale messa dalla
 * tastiera è quasi sempre da cancellare.
 */
private fun ColumnType.capitalization(): KeyboardCapitalization = when (this) {
    ColumnType.TEXT -> KeyboardCapitalization.Sentences
    else -> KeyboardCapitalization.None
}

private fun ColumnType.keyboardType(): KeyboardType = when (this) {
    ColumnType.NUMBER -> KeyboardType.Number
    ColumnType.URL -> KeyboardType.Uri
    ColumnType.EMAIL -> KeyboardType.Email
    ColumnType.PHONE -> KeyboardType.Phone
    else -> KeyboardType.Text
}

/** Solo le etichette, per chi non ha bisogno dei colori. */
private fun parseSelectOptions(optionsJson: String): List<String> =
    parseSelectOptionsFull(optionsJson).map { it.label }

/**
 * Quanto tempo sta in una schermata della linea del tempo, e con che
 * passo è segnato l'asse.
 *
 * Il modello è uno solo per tutti gli ingrandimenti: **la larghezza di
 * un giorno**. Da lì discende tutto — dove comincia una barra, quanto è
 * lunga, dove cade oggi — e le caselle dell'asse sono larghe quanti
 * giorni durano. Così un mese di ventotto giorni è più stretto di uno
 * di trentuno e le barre restano incollate alle loro date, invece di
 * scivolare rispetto alle etichette come succederebbe dando a ogni mese
 * la stessa larghezza.
 */
private enum class TimelineTick { HOUR, DAY, WEEK, MONTH }

@Immutable
private data class TimelineScale(
    val dayWidth: Dp,
    val tick: TimelineTick,
    /** Quanto respiro lasciare oltre le date che ci sono. */
    val padDays: Long
)

private fun TimelineZoom.scale(): TimelineScale = when (this) {
    // Un giorno largo quanto ventiquattro caselle da 52: alle ore
    // serve tutta questa larghezza, ed è il motivo per cui l'asse a ore
    // copre pochi giorni e non tutto il database.
    TimelineZoom.HOURS -> TimelineScale(TIMELINE_HOUR_WIDTH * 24, TimelineTick.HOUR, 1)
    TimelineZoom.DAY -> TimelineScale(40.dp, TimelineTick.DAY, 7)
    TimelineZoom.WEEK -> TimelineScale(22.dp, TimelineTick.DAY, 14)
    TimelineZoom.BI_WEEK -> TimelineScale(14.dp, TimelineTick.DAY, 21)
    TimelineZoom.MONTH -> TimelineScale(9.dp, TimelineTick.DAY, 31)
    TimelineZoom.QUARTER -> TimelineScale(3.2.dp, TimelineTick.WEEK, 90)
    TimelineZoom.YEAR -> TimelineScale(0.9.dp, TimelineTick.MONTH, 365)
}

/** Una casella dell'asse: quanto è larga e cosa c'è scritto sopra. */
@Immutable
private data class TimelineTickCell(
    val width: Dp,
    val label: String,
    val weekend: Boolean,
    val isToday: Boolean
)

/**
 * La visualizzazione a linea del tempo: un asse del tempo che scorre in
 * orizzontale, e una riga per pagina con la barra lunga quanto i giorni
 * che copre.
 *
 * È il calendario srotolato. Nel calendario il tempo va a capo ogni
 * settimana e una pagina lunga si spezza in più pezzi; qui il tempo è
 * una riga sola e la barra è intera, che è tutto il punto di una linea
 * del tempo: confrontare a colpo d'occhio durate e sovrapposizioni.
 *
 * L'ordine delle righe è lo stesso del calendario — le pagine per data,
 * la prima in alto — così passando da una vista all'altra non si perde
 * il filo. Qui però ogni riga ha per forza la sua pagina, quindi non
 * restano righe vuote.
 *
 * I nomi stanno in una colonna ferma a sinistra: scorrendo di mesi le
 * barre se ne vanno, e senza i nomi accanto non si saprebbe più di chi
 * è quella che si sta guardando.
 */
@Composable
private fun TimelineLayout(
    state: DatabaseTableState,
    dateColumn: DatabaseColumnEntity?,
    zoom: TimelineZoom,
    newRowTick: Int,
    onSetZoom: (TimelineZoom) -> Unit,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit,
    onAddDateProperty: () -> Unit
) {
    if (dateColumn == null) {
        MissingPropertyNotice(
            message = DbStrings.timelineNeedsDate,
            actionLabel = DbStrings.addDateProperty,
            onAction = onAddDateProperty
        )
        return
    }

    val today = LocalDate.now()
    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek

    val dated = remember(state.rows, state.cellValues, dateColumn.id) {
        state.rows.mapNotNull { row ->
            parseDateRange(state.cellValues[row.id to dateColumn.id].orEmpty())
                ?.let { row to it }
        }
    }
    val undated = remember(state.rows, state.cellValues, dateColumn.id) {
        state.rows.filter { state.cellValues[it.id to dateColumn.id].isNullOrBlank() }
    }

    // Le pagine in ordine di data: lo stesso ordine del calendario, e la
    // stessa regola per i pareggi.
    val bars = remember(dated) {
        dated.map { (row, range) -> timelineBar(row, range) }
            .sortedWith(
                compareBy<TimelineBarData> { it.startAt }
                    .thenBy { it.endAt }
                    .thenBy { it.row.title }
                    .thenBy { it.row.id }
            )
    }

    var showZoomSheet by remember { mutableStateOf(false) }
    val scale = zoom.scale()

    if (bars.isEmpty()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = DbStrings.noPageHasDate,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
            TimelineUndatedRows(undated, onOpenRow, onRowLongPress)
        }
        return
    }

    // L'asse comincia e finisce dove serve: la prima e l'ultima data,
    // oggi sempre compreso, e il respiro che chiede l'ingrandimento.
    // Poi viene allineato all'inizio della sua unità, altrimenti le
    // caselle dei mesi comincerebbero a metà mese.
    val axis = remember(bars, today, zoom, firstDayOfWeek) {
        timelineAxis(
            first = bars.minOf { it.startAt }.toLocalDate(),
            last = bars.maxOf { it.endAt }.toLocalDate(),
            today = today,
            scale = scale,
            firstDayOfWeek = firstDayOfWeek
        )
    }

    val cells = remember(axis, zoom, today, firstDayOfWeek) {
        timelineTickCells(axis, scale, today, firstDayOfWeek)
    }

    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val dayWidthPx = with(density) { scale.dayWidth.toPx() }

    // Si apre su oggi, non all'inizio dell'asse: la pagina più vecchia
    // del database è raramente quella che interessa. Si aspetta che il
    // contenuto sia stato misurato, altrimenti lo spostamento verrebbe
    // tagliato a zero perché la lunghezza non si conosce ancora.
    // Il bersaglio è l'**istante** di adesso, non la mezzanotte di
    // oggi: a ingrandimento "ore" mezzanotte è mezza giornata di
    // distanza, e si aprirebbe lontano da quello che sta succedendo.
    // Il rientro fisso lascia un po' di passato in vista, così adesso
    // non è incollato al bordo sinistro.
    val leadInPx = with(density) { TIMELINE_TODAY_LEAD_IN.toPx() }
    fun todayTarget(): Int =
        (timelineNowOffsetDays(axis.start) * dayWidthPx - leadInPx)
            .toInt().coerceAtLeast(0)

    LaunchedEffect(axis, zoom, newRowTick) {
        snapshotFlow { scroll.maxValue }.first { it > 0 }
        scroll.scrollTo(todayTarget())
    }

    val rowsShown = bars.size.coerceAtMost(CALENDAR_MAX_ROWS)
    val axisWidth = scale.dayWidth * axis.totalDays
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth()) {
        // L'intestazione ferma: a sinistra dove siamo, a destra come si
        // guarda e come tornare a oggi.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimelinePeriodTitle(modifier = Modifier.weight(1f)) {
                val day = (scroll.value / dayWidthPx).toInt()
                    .coerceIn(0, (axis.totalDays - 1).coerceAtLeast(0))
                val date = axis.start.plusDays(day.toLong())
                when (zoom) {
                    TimelineZoom.HOURS -> date.format(DATE_FORMAT)
                    TimelineZoom.YEAR -> date.year.toString()
                    else -> date.format(MONTH_FORMAT)
                }
            }
            TimelineHeaderChip(
                label = zoom.displayName(),
                trailingIcon = Icons.Filled.KeyboardArrowDown,
                onClick = { showZoomSheet = true }
            )
            Spacer(modifier = Modifier.size(8.dp))
            TimelineHeaderChip(
                label = DbStrings.today,
                trailingIcon = null,
                onClick = { scope.launch { scroll.animateScrollTo(todayTarget()) } }
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            // La colonna dei nomi non scorre con l'asse.
            Column(modifier = Modifier.width(TIMELINE_NAME_WIDTH)) {
                Spacer(modifier = Modifier.height(TIMELINE_HEADER_HEIGHT))
                HorizontalDivider()
                bars.take(rowsShown).forEach { bar ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(TIMELINE_ROW_HEIGHT)
                            .foundInside(bar.row.id)
                            .onRightClick { onRowLongPress(bar.row) }
                            .pointerInput(bar.row.id) {
                                detectTapGestures(
                                    onTap = { onOpenRow(bar.row) },
                                    onLongPress = { onRowLongPress(bar.row) }
                                )
                            }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RowIconBadge(bar.row.id, BAR_ICON_SIZE)
                            Text(
                                text = highlightedTitle(bar.row.title),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            VerticalDivider()

            Column(modifier = Modifier.horizontalScroll(scroll)) {
                TimelineAxisHeader(cells = cells)
                HorizontalDivider(modifier = Modifier.width(axisWidth))

                Box {
                    Column {
                        bars.take(rowsShown).forEach { bar ->
                            TimelineBarRow(
                                bar = bar,
                                axisStart = axis.start,
                                totalDays = axis.totalDays,
                                dayWidth = scale.dayWidth,
                                onOpenRow = onOpenRow,
                                onRowLongPress = onRowLongPress
                            )
                        }
                    }
                    // La riga di oggi attraversa tutte le barre: è il
                    // riferimento che dice cosa è in ritardo e cosa no.
                    // Sta all'ora esatta, non a mezzanotte, perché a
                    // ingrandimento "ore" la differenza si vede tutta.
                    val nowOffset = timelineNowOffsetDays(axis.start)
                    if (nowOffset >= 0f && nowOffset <= axis.totalDays) {
                        Box(
                            modifier = Modifier
                                .offset(x = scale.dayWidth * nowOffset)
                                .width(1.dp)
                                .height(TIMELINE_ROW_HEIGHT * rowsShown)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }

        if (bars.size > rowsShown) {
            Text(
                text = "+${bars.size - rowsShown} more, in the table",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp)
            )
        }

        TimelineUndatedRows(undated, onOpenRow, onRowLongPress)
    }

    if (showZoomSheet) {
        TimelineZoomSheet(
            current = zoom,
            onPick = { onSetZoom(it) },
            onDismiss = { showZoomSheet = false }
        )
    }
}

/** L'asse: da quando, per quanti giorni. */
@Immutable
private data class TimelineAxis(val start: LocalDate, val totalDays: Int)

/**
 * I confini dell'asse, allineati all'unità dell'ingrandimento.
 *
 * Il tetto sulle caselle non è prudenza sprecata: non sono pigre,
 * vengono composte tutte insieme. Quando le date sono più larghe di
 * quanto il tetto consenta, la finestra si stringe **attorno a oggi**,
 * che è il punto da cui si guarda quasi sempre.
 */
private fun timelineAxis(
    first: LocalDate,
    last: LocalDate,
    today: LocalDate,
    scale: TimelineScale,
    firstDayOfWeek: DayOfWeek
): TimelineAxis {
    val rawStart = minOf(first, today).minusDays(scale.padDays)
    val rawEnd = maxOf(last, today).plusDays(scale.padDays)

    fun align(date: LocalDate) = when (scale.tick) {
        TimelineTick.HOUR, TimelineTick.DAY -> date
        TimelineTick.WEEK -> startOfWeek(date, firstDayOfWeek)
        TimelineTick.MONTH -> date.withDayOfMonth(1)
    }

    val maxDays = when (scale.tick) {
        TimelineTick.HOUR -> TIMELINE_MAX_TICKS / 24
        TimelineTick.DAY -> TIMELINE_MAX_TICKS
        TimelineTick.WEEK -> TIMELINE_MAX_TICKS * 7
        TimelineTick.MONTH -> TIMELINE_MAX_TICKS * 31
    }.coerceAtLeast(2)

    var start = align(rawStart)
    var end = rawEnd
    if (ChronoUnit.DAYS.between(start, end) > maxDays) {
        start = align(today.minusDays(maxDays / 3L))
        end = start.plusDays(maxDays.toLong())
    }
    val days = (ChronoUnit.DAYS.between(start, end).toInt() + 1).coerceAtLeast(1)
    return TimelineAxis(start, days)
}

/** Le caselle dell'asse, una per unità dell'ingrandimento. */
private fun timelineTickCells(
    axis: TimelineAxis,
    scale: TimelineScale,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek
): List<TimelineTickCell> {
    val cells = mutableListOf<TimelineTickCell>()
    when (scale.tick) {
        TimelineTick.HOUR -> {
            val hourWidth = scale.dayWidth / 24
            repeat(axis.totalDays) { dayIndex ->
                val date = axis.start.plusDays(dayIndex.toLong())
                repeat(24) { hour ->
                    cells += TimelineTickCell(
                        width = hourWidth,
                        label = hour.toString().padStart(2, '0'),
                        weekend = false,
                        isToday = date == today
                    )
                }
            }
        }

        TimelineTick.DAY -> {
            // Con le caselle strette il numero non ci sta su tutte: si
            // scrive una ogni tot, e le altre restano segni muti.
            val every = timelineLabelEvery(scale.dayWidth)
            repeat(axis.totalDays) { index ->
                val date = axis.start.plusDays(index.toLong())
                cells += TimelineTickCell(
                    width = scale.dayWidth,
                    label = if (index % every == 0) date.dayOfMonth.toString() else "",
                    weekend = date.dayOfWeek == DayOfWeek.SATURDAY ||
                        date.dayOfWeek == DayOfWeek.SUNDAY,
                    isToday = date == today
                )
            }
        }

        TimelineTick.WEEK -> {
            // Solo il giorno, e non su tutte le settimane: "13 Sep"
            // vuole il doppio della larghezza di una casella da una
            // settimana, e scritto su ognuna diventa una fila di
            // parole sovrapposte. Il mese lo dice l'intestazione ferma
            // sopra l'asse.
            val cellWidth = scale.dayWidth * 7
            val every = if (cellWidth >= 40.dp) 1 else 2
            var date = axis.start
            var index = 0
            val end = axis.start.plusDays(axis.totalDays.toLong() - 1)
            while (!date.isAfter(end)) {
                val weekEnd = date.plusDays(6)
                cells += TimelineTickCell(
                    width = cellWidth,
                    label = if (index % every == 0) date.dayOfMonth.toString() else "",
                    weekend = false,
                    isToday = !today.isBefore(date) && !today.isAfter(weekEnd)
                )
                date = date.plusWeeks(1)
                index++
            }
        }

        TimelineTick.MONTH -> {
            var month = YearMonth.from(axis.start)
            val end = axis.start.plusDays(axis.totalDays.toLong() - 1)
            while (!month.atDay(1).isAfter(end)) {
                cells += TimelineTickCell(
                    // Larga quanti giorni ha davvero: è così che le
                    // barre restano incollate alle loro date.
                    width = scale.dayWidth * month.lengthOfMonth(),
                    label = month.month
                        .getDisplayName(TextStyle.SHORT, displayLocale),
                    weekend = false,
                    isToday = YearMonth.from(today) == month
                )
                month = month.plusMonths(1)
            }
        }
    }
    return cells
}

/** Ogni quante caselle si scrive il numero, perché due cifre ci stiano. */
private fun timelineLabelEvery(dayWidth: Dp): Int =
    when {
        dayWidth >= 20.dp -> 1
        dayWidth >= 12.dp -> 2
        else -> 5
    }

/** Quanti giorni (con la frazione) separano l'inizio dell'asse da adesso. */
private fun timelineNowOffsetDays(axisStart: LocalDate): Float {
    val now = LocalDateTime.now()
    val minutes = ChronoUnit.MINUTES.between(axisStart.atStartOfDay(), now)
    return minutes / 1440f
}

/**
 * Una pagina sulla linea del tempo: da quando a quando, con l'ora.
 *
 * La fine è **esclusiva**. Una pagina di un giorno senza ora va da
 * mezzanotte a mezzanotte del giorno dopo, cioè occupa il giorno
 * intero: se la fine fosse inclusiva sarebbe lunga zero. Una pagina
 * con l'ora e senza fine è invece un istante, e resta lunga zero —
 * la si vede lo stesso perché le barre hanno una larghezza minima.
 */
@Immutable
private data class TimelineBarData(
    val row: DatabaseRowEntity,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime
)

private fun timelineBar(row: DatabaseRowEntity, range: DateRange): TimelineBarData {
    val startAt = if (range.hasTime) {
        range.start.toLocalDateTime()
    } else {
        range.start.toLocalDate().atStartOfDay()
    }
    val rangeEnd = range.end
    val endAt = when {
        rangeEnd != null && range.hasTime -> rangeEnd.toLocalDateTime()
        rangeEnd != null -> rangeEnd.toLocalDate().plusDays(1).atStartOfDay()
        range.hasTime -> startAt
        else -> range.start.toLocalDate().plusDays(1).atStartOfDay()
    }
    return TimelineBarData(row, startAt, maxOf(endAt, startAt))
}

/**
 * L'asse: una casella per unità, con il fine settimana in ombra.
 *
 * Il periodo non è scritto qui ma nell'intestazione ferma sopra la
 * linea del tempo: scritto sull'asse starebbe all'inizio del suo tratto
 * e scorrendo sparirebbe a sinistra, lasciando una fila di numeri senza
 * sapere di quale mese sono.
 */
@Composable
private fun TimelineAxisHeader(cells: List<TimelineTickCell>) {
    Row {
        cells.forEach { cell ->
            Box(
                modifier = Modifier
                    .width(cell.width)
                    .height(TIMELINE_HEADER_HEIGHT)
                    .background(if (cell.weekend) DarkSurface else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (cell.label.isNotEmpty()) {
                    Text(
                        text = cell.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (cell.isToday) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        },
                        maxLines = 1,
                        // Agli ingrandimenti larghi la casella di un
                        // giorno è più stretta di due cifre: senza
                        // questo il numero viene tagliato a metà e si
                        // legge "2" dove c'è scritto "25". Qui il testo
                        // si misura libero e sborda sulle caselle
                        // accanto, che tanto sono mute.
                        modifier = Modifier.wrapContentWidth(
                            align = Alignment.CenterHorizontally,
                            unbounded = true
                        )
                    )
                }
            }
        }
    }
}

/**
 * Il periodo che si sta guardando, sopra la linea del tempo.
 *
 * Prende il testo da una funzione invece che da un valore: così è
 * questo pezzetto a leggere la posizione dello scorrimento, ed è
 * l'unico a doversi ridisegnare mentre il dito trascina l'asse. Se lo
 * leggesse chi sta sopra, ogni giorno scorso rifarebbe tutta la linea
 * del tempo.
 */
@Composable
private fun TimelinePeriodTitle(modifier: Modifier = Modifier, label: () -> String) {
    Text(
        text = label(),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        modifier = modifier
    )
}

/** Un pulsantino dell'intestazione della linea del tempo. */
@Composable
private fun TimelineHeaderChip(
    label: String,
    trailingIcon: ImageVector?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(DarkSurface, RoundedCornerShape(percent = 50))
            .clickable { onClick() }
            .padding(start = 12.dp, end = if (trailingIcon == null) 12.dp else 6.dp)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1
        )
        if (trailingIcon != null) {
            Icon(
                trailingIcon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** La finestra per scegliere quanto tempo far stare in una schermata. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimelineZoomSheet(
    current: TimelineZoom,
    onPick: (TimelineZoom) -> Unit,
    onDismiss: () -> Unit
) {
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = DbStrings.selectZoomLevel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) { Text(Strings.done) }
        }
        HorizontalDivider()
        TimelineZoom.entries.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onPick(option)
                        onDismiss()
                    }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = option.displayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                if (option == current) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            HorizontalDivider()
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/** La riga di una pagina: lo spazio prima, poi la barra. */
@Composable
private fun TimelineBarRow(
    bar: TimelineBarData,
    axisStart: LocalDate,
    totalDays: Int,
    dayWidth: Dp,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit
) {
    // La posizione è in giorni **con la frazione**, non in giorni
    // interi: è quello che fa valere l'ora. A ingrandimento "ore" una
    // pagina che comincia alle 14:30 deve stare a metà pomeriggio, non
    // a mezzanotte.
    //
    // L'intervallo viene tagliato sull'asse: una data oltre il bordo
    // non deve spingere la barra fuori da quello che è disegnato.
    val axisStartAt = axisStart.atStartOfDay()
    val from = (ChronoUnit.MINUTES.between(axisStartAt, bar.startAt) / 1440f)
        .coerceIn(0f, totalDays.toFloat())
    val to = (ChronoUnit.MINUTES.between(axisStartAt, bar.endAt) / 1440f)
        .coerceIn(from, totalDays.toFloat())

    Box(
        modifier = Modifier
            .width(dayWidth * totalDays)
            .height(TIMELINE_ROW_HEIGHT)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = dayWidth * from)
                // Agli ingrandimenti larghi un giorno solo sarebbe meno
                // di un pixel: la barra non deve sparire, deve restare
                // un segno che si può toccare.
                .width((dayWidth * (to - from)).coerceAtLeast(TIMELINE_MIN_BAR_WIDTH))
                .height(TIMELINE_ROW_HEIGHT - 10.dp)
                .padding(horizontal = 1.dp)
                .background(DarkSurfaceVariant, RoundedCornerShape(4.dp))
                .foundInside(bar.row.id, RoundedCornerShape(4.dp))
                .onRightClick { onRowLongPress(bar.row) }
                .pointerInput(bar.row.id) {
                    detectTapGestures(
                        onTap = { onOpenRow(bar.row) },
                        onLongPress = { onRowLongPress(bar.row) }
                    )
                }
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RowIconBadge(bar.row.id, BAR_ICON_SIZE)
                Text(
                    text = highlightedTitle(bar.row.title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Le pagine senza data, in fondo. Non hanno un posto sull'asse ma non
 * devono sparire: sarebbero raggiungibili solo cambiando vista.
 */
@Composable
private fun TimelineUndatedRows(
    undated: List<DatabaseRowEntity>,
    onOpenRow: (DatabaseRowEntity) -> Unit,
    onRowLongPress: (DatabaseRowEntity) -> Unit
) {
    if (undated.isEmpty()) return
    Text(
        text = DbStrings.noDate(undated.size),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
    )
    undated.forEach { row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .foundInside(row.id)
                .onRightClick { onRowLongPress(row) }
                .pointerInput(row.id) {
                    detectTapGestures(
                        onTap = { onOpenRow(row) },
                        onLongPress = { onRowLongPress(row) }
                    )
                }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RowIconBadge(row.id, ROW_ICON_SIZE)
            Text(
                text = highlightedTitle(row.title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
        }
    }
}
