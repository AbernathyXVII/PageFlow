package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.data.ocr.PhotoTextRequests
import com.gabriele.notionlocal.data.entity.NewBlock
import com.gabriele.notionlocal.ui.navigation.LocalPeekControls
import com.gabriele.notionlocal.ui.navigation.LocalPageOpener
import com.gabriele.notionlocal.platform.emojiTransformation
import com.gabriele.notionlocal.platform.withEmojiFont
import com.gabriele.notionlocal.platform.emojiFontFamily
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import com.gabriele.notionlocal.ui.theme.pcOr
import androidx.compose.runtime.produceState
import com.gabriele.notionlocal.ui.theme.chosenSizeSp
import androidx.compose.foundation.layout.PaddingValues
import com.gabriele.notionlocal.platform.isPc
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import java.util.concurrent.atomic.AtomicReference
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.draw.drawWithContent
import com.gabriele.notionlocal.data.chosenEmoji
import com.gabriele.notionlocal.data.hasChosenIcon
import androidx.compose.ui.text.style.TextOverflow
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.settings.TitlePosition
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.i18n.HistoryStrings
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.ui.i18n.ParagraphStrings
import androidx.compose.material.icons.filled.FormatLineSpacing
import com.gabriele.notionlocal.data.entity.ParagraphLook
import com.gabriele.notionlocal.data.entity.paragraph
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.TextStats
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import com.gabriele.notionlocal.data.entity.PageFont
import com.gabriele.notionlocal.ui.theme.PageFontGroup
import com.gabriele.notionlocal.ui.theme.label
import com.gabriele.notionlocal.ui.theme.lookalike
import com.gabriele.notionlocal.ui.theme.group
import com.gabriele.notionlocal.ui.theme.fontFamily
import com.gabriele.notionlocal.ui.theme.DEFAULT_PAGE_FONT_SIZE
import androidx.compose.material3.OutlinedButton
import com.gabriele.notionlocal.ui.theme.MIN_PAGE_FONT_SIZE
import com.gabriele.notionlocal.ui.theme.MAX_PAGE_FONT_SIZE
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.platform.BackHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.ui.platform.LocalLayoutDirection
import com.gabriele.notionlocal.ui.theme.DarkSurfaceVariant
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.SelectAll
import kotlinx.coroutines.flow.flowOf
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatIndentDecrease
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.gabriele.notionlocal.data.entity.InkStrokeEntity
import com.gabriele.notionlocal.data.ink.InkPrefs
import com.gabriele.notionlocal.data.ink.resolveAnchor
import com.gabriele.notionlocal.viewmodel.InkViewModel
import com.gabriele.notionlocal.ui.i18n.LockStrings
import com.gabriele.notionlocal.data.notify.NotificationCommand
import com.gabriele.notionlocal.data.notify.NotificationCommands
import com.gabriele.notionlocal.data.notify.NotificationPermission
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.NotifyStrings
import com.gabriele.notionlocal.ui.theme.AccentBlue
import com.gabriele.notionlocal.ui.theme.TextLinkColor
import java.time.Instant
import java.time.LocalDateTime
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.gabriele.notionlocal.ui.i18n.NotebookStrings
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.MULTILINE_TYPES
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.data.entity.heldListType
import com.gabriele.notionlocal.ui.i18n.BlockStrings
import com.gabriele.notionlocal.ui.theme.CalloutBackground
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.applyPaste
import com.gabriele.notionlocal.data.entity.applyTextEdit
import com.gabriele.notionlocal.data.entity.plainText
import com.gabriele.notionlocal.data.entity.splitLines
import com.gabriele.notionlocal.data.entity.setFormatInRange
import com.gabriele.notionlocal.data.entity.toggleFormatInRange
import com.gabriele.notionlocal.data.entity.withSubscript
import com.gabriele.notionlocal.data.entity.withSuperscript
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.em
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.FloatingButtonBackground
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.i18n.TemplateStrings
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material.icons.outlined.Description
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.foundation.shape.CircleShape
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.ui.theme.SpoilerCover
import com.gabriele.notionlocal.ui.theme.SpoilerRevealed
import com.gabriele.notionlocal.viewmodel.FormatType
import com.gabriele.notionlocal.viewmodel.BackspaceOutcome
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import com.gabriele.notionlocal.viewmodel.SplitOutcome
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import com.gabriele.notionlocal.data.entity.LinkRange
import com.gabriele.notionlocal.data.entity.dateLink
import com.gabriele.notionlocal.data.entity.dateLinkTarget
import com.gabriele.notionlocal.data.entity.pageLink
import com.gabriele.notionlocal.ui.i18n.MentionStrings
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalClipboard
import com.gabriele.notionlocal.viewmodel.export
import com.gabriele.notionlocal.platform.showMessage
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.platform.isKeyboardVisible

/**
 * I tipi che il "+" dà a tutte le righe selezionate insieme (vedi
 * `PageEditorViewModel.updateBlockTypes`): quelli che una riga di testo
 * diventa da sola, senza aprire finestre e senza diventare altro.
 */
private val TURN_INTO_MANY = setOf(
    BlockType.PARAGRAPH,
    BlockType.BULLET_LIST_ITEM, BlockType.NUMBERED_LIST_ITEM,
    BlockType.CHECKBOX, BlockType.TOGGLE,
    BlockType.CALLOUT, BlockType.QUOTE, BlockType.CODE
)

/**
 * I tipi "di testo scorrevole" vengono uniti, quando consecutivi, in un
 * unico campo di testo condiviso (vedi MergedTextRunField) — è quello
 * che rende selezione e backspace naturali tra un blocco e l'altro,
 * esattamente come in un editor di testo normale, invece che isolati
 * blocco per blocco. Tutti gli altri tipi restano "isole" separate,
 * renderizzate come prima tramite BlockRow.
 */
private val FLOWING_TYPES = setOf(
    BlockType.PARAGRAPH,
    BlockType.BULLET_LIST_ITEM, BlockType.NUMBERED_LIST_ITEM,
    // **Anche le caselle da spuntare.** Finché erano isole, ognuna era
    // un campo di testo a sé: andare a capo spostava il fuoco da un
    // campo all'altro, e ogni cambio di specie ne distruggeva uno
    // mentre aveva il cursore — da lì la tastiera che spariva, la
    // maiuscola che faceva uno scatto, le lettere nella riga sbagliata
    // e l'app che si chiudeva. Dentro il campo condiviso andare a capo
    // è solo scrivere un a-capo, come fra due paragrafi.
    BlockType.CHECKBOX
)

/**
 * Lo spazio che il quadratino occupa a inizio riga, scritto come testo
 * e reso invisibile: il quadratino vero viene disegnato sopra.
 *
 * È un carattere di riempimento e non il disegno della casella perché
 * così la larghezza non dipende da quali glifi ha il carattere
 * tipografico, e soprattutto non cambia quando la casella viene
 * spuntata — altrimenti il testo si sposterebbe a ogni tocco.
 */
private const val CHECKBOX_MARKER = "    "

/**
 * Quanto respirano le righe di un gruppo che contiene caselle da
 * spuntare.
 *
 * Il quadratino è piccolo e con le righe attaccate è facile spuntare
 * quella sbagliata col dito. Lo spazio si mette come **altezza di riga
 * dell'intero campo**, non riga per riga: dentro la trasformazione non
 * si può: né uno `ParagraphStyle` con `lineHeight`, né un carattere
 * invisibile ingrandito. Provati tutti e due, e il campo **smetteva di
 * disegnare**: righe vuote, nessun errore — e quella vuotezza finiva
 * nel database, cancellando il testo. Vedi "Strade già tentate".
 *
 * **Il cursore di scrittura è alto quanto la riga**, e questo non si
 * può separare: il cursore lo disegna il campo di testo prendendo il
 * riquadro della riga. Più spazio fra le caselle vuol dire cursore più
 * alto, meno spazio vuol dire cursore normale. Con 34 si notava; qui si
 * sta appena sopra il naturale (24), che lascia il cursore di misura
 * normale e un po' d'aria fra una casella e l'altra.
 */
private val CHECKBOX_LINE_HEIGHT = 27.sp

/**
 * L'altezza di una riga di testo scorrevole senza caselle da spuntare.
 *
 * È quella naturale del testo (16 su 24), ma **scritta a mano invece di
 * lasciarla al carattere**: così non dipende dal segno più grande che
 * la riga contiene. Ventiquattro bastano anche al pallino, che è il
 * segno più grande a 20 e in altezza ne occupa poco più di 23.
 */
private val RUN_LINE_HEIGHT = 24.sp

/**
 * Il testo del campo unito comincia sempre con un a-capo in più, che non
 * appartiene a nessun blocco e non viene mai mostrato né salvato. Serve a
 * dare al backspace premuto a inizio della PRIMA riga un carattere da
 * cancellare: senza, quel gesto lascia il testo identico e non c'è nulla
 * da osservare, perché gli eventi-tasto non arrivano (vedi README).
 *
 * **Deve essere un a-capo**, non uno zero-width space: l'a-capo spezza le
 * parole con certezza, quindi la tastiera non può inglobarlo nella parola
 * che sta componendo. Con lo zero-width space succedeva, e il risultato
 * era una lotta con la tastiera ad ogni tasto — vedi README, "Strade già
 * tentate".
 */
/**
 * Il margine laterale dei blocchi di una pagina.
 *
 * Lo applica ogni blocco per conto suo invece della lista che li
 * contiene: così il database incorporato può non averlo e usare tutta
 * la larghezza, come fa a schermo intero.
 */
private val PAGE_SIDE_PADDING = 20.dp

/**
 * **Sul PC tutta la pagina sta un po' più a destra** (dal 11/11/2026, chiesto
 * dall'utente): nel margine a sinistra ci stanno i sei puntini dei blocchi
 * (`BlockHandle.kt`) senza toccare il testo né l'icona di un database. Si
 * spostano anche i database incorporati e i disegni; la copertina resta a
 * tutta larghezza (`bleedSides`). Sul telefono niente. Dalla terza fase
 * dell'aspetto da Notion (11/11/2026) è il margine di Notion a tutta
 * larghezza, da tutti e due i lati: il testo comincia a 86 come nella foto
 * di Notion dell'utente. Lo stesso margine ha il database aperto a tutta
 * pagina (`DatabaseViewScreen`).
 */
internal val PAGE_INSET = if (isPc) 66.dp else 0.dp

/**
 * **Il testo in una colonna al centro** (dal 12/11/2026, solo sul PC,
 * chiesto dall'utente come in Notion, togliendo "Full width" dai tre
 * puntini): largo 637 punti, quanto in Notion coi caratteri del PC (708 al
 * 90%), al centro dello spazio della pagina — quello accanto alla barra
 * laterale se è fissata, se no tutta la finestra. In una finestra stretta i
 * margini restano quelli di sempre ([PAGE_INSET]).
 */
internal val PC_CENTERED_TEXT_WIDTH = 637.dp

/** Il margine di ogni lato della pagina, larga [areaWidth]. */
fun pageInsetFor(fullWidth: Boolean, areaWidth: Dp): Dp =
    if (!isPc || fullWidth || areaWidth <= 0.dp) PAGE_INSET
    else maxOf(PAGE_INSET, (areaWidth - PC_CENTERED_TEXT_WIDTH - PAGE_SIDE_PADDING * 2) / 2)

/**
 * **Sul PC il margine a sinistra è la metà** (dal 13/11/2026, chiesto
 * dall'utente: "dimezza la distanza tra il margine più a sinistra e il
 * contenuto della prima colonna"): 33 punti invece di 66; i sei puntini ci
 * stanno ancora. Lo stesso spazio c'è prima di ogni colonna che non è la
 * prima (vedi COLUMN_LIST in `BlockRow`), così ogni colonna sta staccata dal
 * suo bordo quanto la prima dal bordo della pagina.
 */
internal val PAGE_LEFT_INSET = PAGE_INSET / 2

/**
 * **A destra un margine un po' più piccolo di quello a sinistra** (dal
 * 13/11/2026, chiesto dall'utente: "fallo un po' più piccolo di quello che
 * c'è a sinistra, ma comunque non nullo"): 24 punti invece di 66, e la
 * barretta di scorrimento (14) ci sta senza coprire il contenuto.
 */
internal val PAGE_RIGHT_INSET = if (isPc) 24.dp else 0.dp

/** Il margine a sinistra, dato quello di ogni lato ([inset]): metà del solito; la pagina al centro resta al centro. */
fun pageLeftInsetFor(inset: Dp): Dp = if (inset == PAGE_INSET) PAGE_LEFT_INSET else inset

/** Il margine a destra, dato quello di ogni lato ([inset]): [PAGE_RIGHT_INSET]; la pagina al centro resta al centro. */
fun pageRightInsetFor(inset: Dp): Dp = if (inset == PAGE_INSET) PAGE_RIGHT_INSET else inset

/** L'ultima larghezza misurata della pagina: una pagina appena aperta parte già al centro, senza un salto. */
private object PageAreaMemory {
    var width: Dp = 0.dp
}

/** Allarga di [left] e [right] chi sta nella pagina coi margini (`PAGE_INSET`): la copertina arriva ai bordi. */
private fun Modifier.bleedSides(left: Dp, right: Dp): Modifier = layout { measurable, constraints ->
    val start = left.roundToPx()
    val end = right.roundToPx()
    if ((start == 0 && end == 0) || !constraints.hasBoundedWidth) {
        val placeable = measurable.measure(constraints)
        return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
    val width = constraints.maxWidth + start + end
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-start, 0) }
}

private const val RUN_LEAD = "\n"

/**
 * **L'a-capo nascosto in testa resta fuori da una selezione** (18/10/2026).
 * "Select all", dalla barra sopra la tastiera o dal menu che si apre
 * tenendo premuto, prendeva anche lui: copiato e incollato diventava una
 * riga in più (visto sul telefono), tagliato valeva un backspace a inizio
 * riga. Il cursore senza selezione non si tocca: a zero è il backspace che
 * il campo unito aspetta.
 */
fun TextFieldValue.leadOutOfSelection(lead: String = RUN_LEAD): TextFieldValue {
    if (selection.collapsed || selection.min >= lead.length || !text.startsWith(lead)) return this
    return copy(
        selection = TextRange(selection.start.coerceAtLeast(lead.length), selection.end.coerceAtLeast(lead.length))
    )
}

/**
 * Quante voci della lista vengono prima dei blocchi: l'intestazione
 * (copertina, icona, titolo), che è una sola.
 *
 * Serve perché la posizione di un blocco fra i `renderItems` e la sua
 * posizione nella lista non coincidono più. Sono due numeri diversi e
 * confonderli porterebbe la pagina a scorrere sulla riga sbagliata.
 */
private const val HEADER_ITEM_COUNT = 1

/** Quanti fotogrammi aspettare che la foto del Web Clipper compaia nella pagina, prima di portarla in vista. */
private const val REVEAL_ATTEMPTS = 30

/**
 * La barra sopra la tastiera: quanto è alto un pulsante e quanto è
 * grande la sua icona.
 *
 * Di serie un `IconButton` è 48 e porta la barra a quasi sessanta punti
 * — su un telefono è una fascia che si mangia una riga e mezzo di
 * testo. Qui i pulsanti sono 40 e le icone 22: restano comodi da
 * centrare col dito (il minimo consigliato è 48 per un bersaglio
 * isolato, ma qui sono dieci in fila e il dito trova comunque quello
 * giusto), e la barra scende sotto i cinquanta.
 */
private val BAR_BUTTON_SIZE = 40.dp

/**
 * Il triangolino di un toggle e lo stacco fra lui e il titolo.
 *
 * Il rientro delle righe dentro un toggle è **la somma dei due**: così
 * il testo di un figlio comincia esattamente sotto il testo del titolo,
 * come su Notion. Prima il rientro era un 20 fisso, un soffio più corto
 * della freccia, e i figli stavano storti di quattro punti a sinistra.
 */
/**
 * I pulsanti sospesi sopra la pagina (indietro e tre puntini): il
 * cerchio, l'icona dentro, lo stacco dai bordi.
 */
private val FLOATING_BUTTON_SIZE: Dp get() = pcOr(28.dp, 36.dp)
private val FLOATING_ICON_SIZE: Dp get() = pcOr(18.dp, 22.dp)
// Sul PC i pulsanti stanno a metà della barra in alto (`PC_TOP_BAR`).
private val FLOATING_BUTTON_MARGIN: Dp get() = pcOr((PC_TOP_BAR - 28.dp) / 2, 10.dp)

/**
 * **Sul PC la barra in alto di Notion** (dal 11/11/2026, terza fase
 * dell'aspetto da Notion, chiesta dall'utente): al posto dei pulsanti tondi
 * sospesi sulla pagina, una fascia alta 40 col fondo della pagina, con
 * indietro, la barra laterale e il percorso (`PageBreadcrumb`) a sinistra, e
 * i tre puntini a destra; i pulsanti sono piatti e grigi. La pagina comincia sotto.
 */
private val PC_TOP_BAR = 40.dp

/** Il colore delle icone dei pulsanti in alto: grigie sul PC, come in Notion. */
private val FloatingTint: Color get() = if (isPc) NotionGray400 else NotionWhite
/** Lo stacco fra indietro e barra laterale, quando ci sono tutti e due. */
private val FLOATING_BUTTON_GAP = 8.dp

/**
 * Quanto spazio lasciare in cima a una pagina **senza copertina**.
 *
 * Con la copertina i pulsanti le stanno sopra ed è proprio quello che
 * si vuole. Senza, sotto ci sarebbero l'icona della pagina e "Add
 * cover", e il pulsante indietro ci finirebbe sopra: si lascia la fascia
 * che occupano, e non di più.
 */
private val FLOATING_BAR_CLEARANCE: Dp get() = FLOATING_BUTTON_SIZE + FLOATING_BUTTON_MARGIN * 2

/** Lo spazio fra il cursore e la barra sopra la tastiera, quando la pagina lo porta in vista. */
private val CARET_KEYBOARD_MARGIN = 12.dp

/**
 * Un pulsante tondo sospeso sopra la pagina.
 *
 * Il fondo è un grigio **semitrasparente**: pieno sarebbe una toppa
 * sulla copertina, del tutto trasparente sparirebbe sulle copertine
 * chiare. Così si legge su qualunque immagine e la lascia intravedere.
 */
@Composable
private fun FloatingPageButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(FLOATING_BUTTON_SIZE)
            // Sul PC piatto, come i pulsanti della barra di Notion: il grigio solo col mouse sopra.
            .clip(if (isPc) RoundedCornerShape(6.dp) else CircleShape)
            .then(if (isPc) Modifier else Modifier.background(FloatingButtonBackground))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** La riga spessa a sinistra di una citazione ("Quote", dal 06/10/2026). */
private val QUOTE_BAR_WIDTH = 3.dp

private val TOGGLE_ARROW_SIZE = 24.dp
private val TOGGLE_ARROW_GAP = 4.dp
/**
 * Di quanto sale il triangolino del toggle. Il triangolo sta in mezzo al
 * suo quadrato di 24, come la riga del titolo sta in mezzo ai suoi 24; ma
 * le lettere non stanno in mezzo alla loro riga, stanno un po' più su
 * (sotto c'è il posto delle gambe di g, p, q). Misurato sul telefono il
 * 26/09/2026: il triangolo stava 2dp sotto il centro delle lettere.
 */
private val TOGGLE_ARROW_LIFT = 2.dp
private val NESTED_INDENT = TOGGLE_ARROW_SIZE + TOGGLE_ARROW_GAP
private val BAR_ICON_SIZE = 22.dp

/**
 * Un pulsante della barra sopra la tastiera. Tutti uguali per
 * costruzione: scriverne uno a mano con una misura diversa è il modo in
 * cui una barra diventa storta.
 */
@Composable
private fun BarButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(BAR_BUTTON_SIZE)
    ) {
        content()
    }
}

/**
 * **Copia, incolla, taglia e seleziona tutto**, nella barra sopra la
 * tastiera (dal 18/10/2026, chiesti dall'utente). Fanno quello che fanno
 * Ctrl+C, Ctrl+V, Ctrl+X e Ctrl+A su una tastiera vera, sul testo dove sta
 * il cursore: i tasti si mandano proprio così, e il campo li tratta come
 * sempre (i link incollati, la riga che si divide…).
 *
 * Tenendo premuto "incolla" si sceglie come, come in Word: Keep Source
 * Formatting (anche il tocco semplice), Match Destination Formatting, Keep
 * Text Only. Vedi `PasteMode`. Il menu non prende il fuoco: il cursore
 * resta nel testo, e l'incolla sa dove andare.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClipboardBarButtons(
    /** "Seleziona tutto": tutta la pagina, non solo il pezzo di testo (dal 20/10/2026, vedi `PageSelection.kt`). */
    onSelectAll: () -> Unit
) {
    val shortcut = rememberEditShortcuts()
    val clipboard = LocalClipboardManager.current as? LinkClipboard
    var pasteMenu by remember { mutableStateOf(false) }
    fun paste(mode: PasteMode) {
        clipboard?.nextPasteMode = mode
        shortcut(EditShortcut.PASTE)
    }
    BarButton(onClick = { shortcut(EditShortcut.COPY) }) {
        Icon(Icons.Filled.ContentCopy, contentDescription = EditorStrings.copy, tint = NotionWhite, modifier = Modifier.size(BAR_ICON_SIZE))
    }
    Box {
        Box(
            modifier = Modifier
                .size(BAR_BUTTON_SIZE)
                .clip(CircleShape)
                .onRightClick({ pasteMenu = true })
                .combinedClickable(
                    onClick = { paste(PasteMode.KEEP_SOURCE) },
                    onLongClick = { pasteMenu = true }
                )
                .semantics { contentDescription = EditorStrings.paste },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.ContentPaste, contentDescription = null, tint = NotionWhite, modifier = Modifier.size(BAR_ICON_SIZE))
        }
        DropdownMenu(
            expanded = pasteMenu,
            onDismissRequest = { pasteMenu = false },
            properties = PopupProperties(focusable = false),
            modifier = Modifier.background(DarkSheet)
        ) {
            listOf(
                PasteMode.KEEP_SOURCE to EditorStrings.pasteKeepSource,
                PasteMode.MATCH_DESTINATION to EditorStrings.pasteMatchDestination,
                PasteMode.TEXT_ONLY to EditorStrings.pasteTextOnly
            ).forEach { (mode, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        pasteMenu = false
                        paste(mode)
                    }
                )
            }
        }
    }
    BarButton(onClick = { shortcut(EditShortcut.CUT) }) {
        Icon(Icons.Filled.ContentCut, contentDescription = EditorStrings.cut, tint = NotionWhite, modifier = Modifier.size(BAR_ICON_SIZE))
    }
    BarButton(onClick = onSelectAll) {
        Icon(Icons.Filled.SelectAll, contentDescription = EditorStrings.selectAll, tint = NotionWhite, modifier = Modifier.size(BAR_ICON_SIZE))
    }
}

/** Quanto può essere larga la voce del font nella barra: "Times New Roman" si accorcia coi puntini. */
private val FONT_CHIP_MAX_WIDTH = 132.dp

/** Quanto può essere larga la voce del corpo del testo: due cifre e la freccetta. */
private val SIZE_CHIP_MAX_WIDTH = 64.dp

/**
 * Una voce della barra Aa che mostra un valore e apre un elenco: il font
 * e il corpo della pagina, **prima di B come su OneNote**. Il valore si
 * legge sempre — il nome del font in uso, il numero del corpo — e la
 * freccetta dice che si tocca per cambiarlo.
 */
@Composable
private fun BarValueChip(
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .height(BAR_BUTTON_SIZE - 8.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, NotionWhite.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
            .clickable(onClickLabel = contentDescription, onClick = onClick)
            .padding(start = 8.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Il `weight` fa accorciare il testo coi puntini lasciando posto
        // alla freccetta, ma **vuole una larghezza massima** nel
        // `modifier` di chi chiama: la barra scorre in orizzontale, quindi
        // senza limite la riga è larga "all'infinito" e Compose dà al
        // testo zero pixel. Era il numero invisibile della voce del corpo
        // (vedi `SIZE_CHIP_MAX_WIDTH`).
        Text(
            text = text,
            color = NotionWhite,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Icon(
            Icons.Filled.ArrowDropDown,
            contentDescription = null,
            tint = NotionWhite,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * **Il menu non prende il fuoco** (`focusable = false`), e non è un
 * dettaglio: un menu che lo prende chiude la tastiera, con la tastiera
 * se ne va la barra (è appoggiata sopra) e con la barra la voce da cui
 * il menu è partito. Così invece si sceglie e si continua a scrivere
 * dove si era, col cursore al suo posto.
 */
private val BarMenuProperties = PopupProperties(focusable = false)

/** Quale menu del tasto destro è aperto sul PC: quello principale, o il "+" e i font che si aprono da lì. */
private enum class PcMenuKind { MAIN, BLOCK_TYPE, FONT }

/** Il menu del tasto destro aperto: [at] è il punto del clic nella finestra, [edit] le azioni del campo. */
private data class PcMenuOpen(val at: Offset, val edit: TextEditActions, val kind: PcMenuKind = PcMenuKind.MAIN)

/**
 * Il font della pagina, dalla barra Aa. Nella barra c'è solo il nome
 * chiesto dall'utente ("Calibri"); qui nell'elenco c'è anche, fra
 * parentesi, il font libero che lo disegna davvero ("Calibri (Carlito)"),
 * come ha chiesto l'utente. In cima "Predefinito", per tornare al font
 * di sistema; poi i tre gruppi separati da un divisore: occidentali,
 * cinesi, giapponesi.
 *
 * I nomi occidentali sono scritti ognuno **col suo font**, come nei menu
 * di Word: sono dentro l'app e si vedono subito. Quelli cinesi e
 * giapponesi no — farli vedere vorrebbe dire scaricarli tutti e sei,
 * decine di megabyte, solo per aprire il menu.
 *
 * **Aprendolo si chiude la barrettina Copy / Cut / Select all** del
 * telefono. Il menu non prende il fuoco (vedi `BarMenuProperties`), quindi
 * il campo resta a fuoco con la sua selezione e il telefono non ha motivo
 * di chiuderla: restava sopra il menu e copriva Garamond e Arial. La
 * selezione resta, si chiude solo la barrettina.
 */
@Composable
private fun FontPickerChip(
    current: PageFont?,
    /** Toccando la casella, prima di aprire il menu: qui si mette da parte la selezione. */
    onOpen: () -> Unit,
    onPick: (PageFont?) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    val textToolbar = LocalTextToolbar.current
    Box {
        BarValueChip(
            text = current?.label ?: EditorStrings.defaultFont,
            contentDescription = EditorStrings.font,
            onClick = {
                onOpen()
                textToolbar.hide()
                open = true
            },
            modifier = Modifier.widthIn(max = FONT_CHIP_MAX_WIDTH)
        )
        FontMenu(expanded = open, current = current, onDismiss = { open = false }) { font ->
            open = false
            onPick(font)
        }
    }
}

/** L'elenco dei font, dalla barra Aa e dal menu del tasto destro sul PC. */
@Composable
private fun FontMenu(
    expanded: Boolean,
    current: PageFont?,
    onDismiss: () -> Unit,
    onPick: (PageFont?) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        properties = BarMenuProperties,
        modifier = Modifier.background(DarkSheet)
    ) {
        FontMenuItem(
            label = EditorStrings.defaultFont,
            selected = current == null,
            fontFamily = null,
            onClick = { onPick(null) }
        )
        PageFontGroup.entries.forEach { group ->
            HorizontalDivider()
            PageFont.entries.filter { it.group == group }.forEach { font ->
                FontMenuItem(
                    label = "${font.label} (${font.lookalike})",
                    selected = current == font,
                    fontFamily = if (group == PageFontGroup.LATIN) font.fontFamily() else null,
                    onClick = { onPick(font) }
                )
            }
        }
    }
}

@Composable
private fun FontMenuItem(
    label: String,
    selected: Boolean,
    fontFamily: FontFamily?,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                color = NotionWhite,
                fontFamily = fontFamily,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        trailingIcon = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null, tint = NotionWhite) }
        } else {
            null
        },
        onClick = onClick
    )
}

/**
 * Il corpo del testo della pagina nella barra Aa: il numero in uso, e
 * toccandolo si apre `FontSizeDialog`, dove lo si scrive.
 *
 * La finestra **non sta qui dentro** ma in fondo alla schermata, accesa
 * da `onClick`: mentre si scrive il numero la tastiera passa alla sua
 * casella, nessuna riga della pagina ha più il cursore, e la barra —
 * che esiste solo quando una riga ce l'ha — se ne va. Una finestra nata
 * dentro la barra se ne andrebbe con lei, a metà numero.
 */
@Composable
private fun FontSizeChip(
    current: Int,
    onClick: () -> Unit
) {
    BarValueChip(
        text = current.toString(),
        contentDescription = EditorStrings.fontSize,
        onClick = onClick,
        modifier = Modifier.widthIn(max = SIZE_CHIP_MAX_WIDTH)
    )
}

/**
 * La finestrella dove si **scrive a mano** il corpo del testo, da 5 a 72,
 * come ha chiesto l'utente dopo aver visto la prima versione (un elenco
 * da cui sceglierlo).
 *
 * Si apre col numero in uso già selezionato, così scrivendo lo si
 * sostituisce senza doverlo cancellare, e con la tastiera dei numeri.
 * Accetta solo cifre, al massimo due; "OK" (o il tasto Fatto della
 * tastiera) si accende solo se il numero sta fra 5 e 72, e sotto la
 * casella c'è scritto quali sono i limiti — diventa rosso se si esce.
 *
 * **"Default"** accanto alla casella (dal 27/10/2026, chiesto dall'utente:
 * "se non mi dovessi ricordare che quello standard è 16") rimette il corpo
 * normale, `DEFAULT_PAGE_FONT_SIZE`, e chiude la finestra.
 *
 * Chiusa la finestra il cursore non torna da solo nella pagina: si
 * tocca il punto dove si vuole riprendere a scrivere. Rimetterlo da qui
 * vorrebbe dire spostare il fuoco a comando mentre la tastiera cambia
 * da numeri a lettere, il passaggio che con la tastiera Samsung dà più
 * problemi (vedi "Peculiarità dell'ambiente di test" nel README).
 */
@Composable
private fun FontSizeDialog(
    current: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val start = current.toString()
    var field by remember { mutableStateOf(TextFieldValue(start, TextRange(0, start.length))) }
    val value = field.text.toIntOrNull()
    val valid = value != null && value in MIN_PAGE_FONT_SIZE..MAX_PAGE_FONT_SIZE
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    FitAlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSheet,
        title = { Text(EditorStrings.fontSize, color = NotionWhite) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AutoCaps {
                        OutlinedTextField(
                            value = field,
                            onValueChange = { typed ->
                                val digits = typed.text.filter { it.isDigit() }.take(2)
                                field = if (digits == typed.text) {
                                    typed
                                } else {
                                    TextFieldValue(digits, TextRange(digits.length))
                                }
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge.copy(color = NotionWhite),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                if (valid) onConfirm(value!!)
                            }),
                            isError = field.text.isNotEmpty() && !valid,
                            modifier = Modifier
                                .width(96.dp)
                                .focusRequester(focus)
                        )
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    OutlinedButton(onClick = { onConfirm(DEFAULT_PAGE_FONT_SIZE) }) {
                        Text(EditorStrings.defaultFontSize, color = NotionWhite)
                    }
                }
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = EditorStrings.fontSizeRange(MIN_PAGE_FONT_SIZE, MAX_PAGE_FONT_SIZE),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (field.text.isEmpty() || valid) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onConfirm(value!!) }) { Text(Strings.done) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings.cancel) }
        }
    )
}

/** Il testo con l'a-capo nascosto reso visibile, per il registro. */
private fun String.forLog(): String = replace("\n", "\\n")

/**
 * Il campo invisibile su cui posare la tastiera per un istante quando
 * la casella di scrittura che ha il fuoco sta per essere distrutta.
 * Vedi il commento dove viene creato, in `PageEditorScreen`.
 */
private val LocalFocusPark = staticCompositionLocalOf<FocusRequester?> { null }

/**
 * Se la pagina aperta è bloccata (la voce "Lock page" del menu): il
 * contenuto si legge e si seleziona, ma non si scrive.
 *
 * Passa da qui e non da un parametro perché i campi di scrittura
 * stanno in fondo a una catena lunga di composable, e infilare lo
 * stesso `Boolean` in otto firme di seguito è un modo sicuro di
 * dimenticarselo in una.
 */
val LocalPageLocked = staticCompositionLocalOf { false }

/**
 * Quanto aspettare, dopo che la tastiera si è chiusa, prima di
 * spegnere il cursore. La tastiera Samsung sparisce e riappare per un
 * istante durante i cambi di fuoco: più lungo del `IME_SETTLE_DELAY_MS`
 * del ViewModel, che di quei cambi è la durata.
 *
 * Alzato da 250 a 450 quando è arrivato il cambio di specie delle
 * caselle da spuntare: lì il fuoco viene tolto e rimesso di proposito,
 * e la tastiera ci mette di più a tornare. Il prezzo è che, chiusa la
 * tastiera a mano, il cursore resta acceso due decimi in più.
 */
private const val IME_CLOSE_SETTLE_MS = 450L

/** Quanto dura il velo sulla riga appena spostata con le frecce. */
private const val MOVE_FLASH_MS = 700

/**
 * Quanto la barra sopra la tastiera aspetta, perso il fuoco, prima di
 * sparire: il tempo che il cursore ci mette ad arrivare sulla riga nuova
 * quando quella di prima cambia specie. Misurato fra 100 e 150 ms (la
 * scrittura nel database più `IME_SETTLE_DELAY_MS`); il doppio per stare
 * larghi. Il prezzo è che passando al titolo della pagina la barra
 * resta tre decimi di secondo in più.
 */
private const val BAR_HANDOFF_GRACE_MS = 300L

/** Quanto è alta la copertina, e quanto grande l'icona-immagine. */
private val COVER_HEIGHT = 160.dp
private val PAGE_ICON_SIZE = 56.dp

// Il quadratino delle caselle da spuntare: grande quanto le lettere
// che gli stanno accanto (una riga di testo è alta 24, le maiuscole
// ne occupano circa 17). La zona di tocco è più larga del disegno,
// perché un bersaglio di 17dp sarebbe scomodo da centrare.
private val CHECKBOX_SIZE = 16.dp
private val CHECKBOX_TOUCH_SIZE = 24.dp

// Entro quanto una divisione va considerata un'eco della tastiera e
// non un secondo Invio: più larga quando torna indietro lo stesso
// pezzo di testo (segno inequivocabile), stretta quando a ripetersi è
// solo la stessa modifica, perché lì bisogna solo escludere che sia
// stato un dito — e un dito due volte in 100ms non ci arriva.
private const val SPLIT_ECHO_MS = 400L
private const val DUPLICATE_EDIT_MS = 100L

/**
 * L'ultima divisione fatta da un campo, per riconoscerne l'eco:
 * il pezzo di testo passato alla riga nuova, la modifica che l'ha
 * prodotta (testo prima → testo dopo) e quando.
 */
private data class SplitEcho(
    val payload: String,
    val transition: Pair<String, String>,
    val at: Long
)

/** Il testo del campo unito per un gruppo di blocchi: a-capo iniziale + una riga per blocco. */
private fun runFieldText(runBlocks: List<BlockEntity>, viewModel: PageEditorViewModel): String =
    RUN_LEAD + runBlocks.joinToString("\n") { viewModel.plainTextOf(it) }

/** Il testo del campo senza l'a-capo iniziale: quello che corrisponde davvero ai blocchi. */
private fun bodyOf(fieldText: String): String =
    if (fieldText.startsWith(RUN_LEAD)) fieldText.substring(1) else fieldText

/**
 * Riconosce il gesto "backspace a inizio riga" da come è cambiato il
 * testo: è sparito esattamente un a-capo, ed è sparito proprio dove si
 * trova ora il cursore. Restituisce l'indice della riga che stava per
 * essere fusa con quella sopra, o -1 se il cambiamento è un altro
 * qualsiasi.
 */
/** Dove comincia la riga e da che numero deve partire l'elenco appena richiesto scrivendo "5.". */
private data class NumberedListPrefix(val lineIndex: Int, val startNumber: Int, val lineStart: Int)

/**
 * Riconosce "ho appena digitato il punto di un `5.` a inizio blocco".
 * Perché scatti devono valere tutte queste condizioni: il cambiamento è
 * l'inserimento di un solo punto, prima del punto ci sono solo cifre, e
 * quelle cifre cominciano esattamente a inizio riga e sono l'unica cosa
 * che c'è. Scrivere un punto dopo una qualsiasi altra cosa — anche
 * `abc 1.` o il punto decimale di `1.5` dopo del testo — non converte
 * niente.
 */
private fun numberedListPrefixJustTyped(oldBody: String, newBody: String, caret: Int): NumberedListPrefix? {
    if (newBody.length != oldBody.length + 1) return null
    if (caret < 2 || caret > newBody.length) return null
    if (newBody[caret - 1] != '.') return null
    // Dopo il punto la riga deve finire: il numero dev'essere tutto
    // quello che c'è scritto.
    if (caret < newBody.length && newBody[caret] != '\n') return null
    if (newBody.removeRange(caret - 1, caret) != oldBody) return null

    val lineStart = newBody.lastIndexOf('\n', caret - 2) + 1
    val digits = newBody.substring(lineStart, caret - 1)
    // Il tetto sulle cifre evita di sforare la capacità di un Int su un
    // incollaggio assurdo.
    //
    // **Lo zero vale.** Di suo un elenco parte da 1, ma chi scrive
    // "0." lo sta chiedendo apposta — si numera così quando c'è un
    // passo zero, un preambolo prima del primo vero punto — e da lì in
    // poi il conteggio prosegue normale: 0, 1, 2.
    if (digits.isEmpty() || digits.length > 9 || !digits.all { it.isDigit() }) return null
    val startNumber = digits.toInt()

    return NumberedListPrefix(
        lineIndex = newBody.take(lineStart).count { it == '\n' },
        startNumber = startNumber,
        lineStart = lineStart
    )
}

/** La riga in cui si è appena scritto "---" (vedi `dividerShortcutJustTyped`). */
data class DividerShortcut(val lineIndex: Int, val lineStart: Int)

/**
 * Le righe che, scritte da sole, diventano un divisore (dal 14/10/2026,
 * chiesto dall'utente, come su Notion): tre trattini attaccati. Anche con
 * la lineetta lunga o media al posto dei primi due, che certe tastiere
 * mettono da sole scrivendo "--".
 */
private val DIVIDER_SHORTCUTS = setOf("---", "—-", "–-")

/**
 * Riconosce "ho appena scritto il terzo trattino di una riga che contiene
 * solo `---`". Come per "5." (`numberedListPrefixJustTyped`): il
 * cambiamento è un solo trattino in più, il cursore è subito dopo, in fondo
 * alla riga, e nella riga non c'è nient'altro. Una riga che ha già "---"
 * scritto, o "---" in mezzo ad altro testo, resta com'è.
 */
fun dividerShortcutJustTyped(oldBody: String, newBody: String, caret: Int): DividerShortcut? {
    if (newBody.length != oldBody.length + 1) return null
    if (caret < 1 || caret > newBody.length) return null
    if (newBody[caret - 1] != '-') return null
    if (newBody.removeRange(caret - 1, caret) != oldBody) return null
    val lineStart = newBody.lastIndexOf('\n', caret - 1) + 1
    val lineEnd = newBody.indexOf('\n', caret).let { if (it < 0) newBody.length else it }
    if (caret != lineEnd) return null
    if (newBody.substring(lineStart, lineEnd) !in DIVIDER_SHORTCUTS) return null
    return DividerShortcut(lineIndex = newBody.take(lineStart).count { it == '\n' }, lineStart = lineStart)
}

/**
 * Dove stanno le due coppie di barre appena chiuse, nel testo che le
 * contiene ancora: `open` è la prima delle quattro, `contentStart` e
 * `contentEnd` delimitano quello che va coperto.
 */
private data class SpoilerMarkup(val open: Int, val contentStart: Int, val contentEnd: Int)

/**
 * Riconosce "ho appena chiuso un `||qualcosa||`", come su Discord e
 * Telegram.
 *
 * Scatta sulla **seconda barra della chiusura**, cioè sul carattere che
 * completa la coppia, e solo se: il cambiamento è l'inserimento di
 * quella sola barra; subito prima ce n'è un'altra; più indietro **sulla
 * stessa riga** c'è una coppia di apertura; e in mezzo c'è almeno un
 * carattere che non sia a sua volta una barra.
 *
 * L'apertura si cerca **dalla più vicina**: scrivendo `||a|| e ||b||` la
 * seconda chiusura deve legarsi alla seconda apertura, non alla prima —
 * altrimenti coprirebbe anche il testo in chiaro che sta nel mezzo.
 */
private fun spoilerJustClosed(oldBody: String, newBody: String, caret: Int): SpoilerMarkup? {
    // `||x||`: cinque caratteri, il minimo perché ci sia qualcosa da
    // coprire.
    if (newBody.length != oldBody.length + 1) return null
    if (caret < 5 || caret > newBody.length) return null
    if (newBody[caret - 1] != '|' || newBody[caret - 2] != '|') return null
    if (newBody.removeRange(caret - 1, caret) != oldBody) return null

    val open = newBody.lastIndexOf("||", startIndex = caret - 5)
    if (open < 0) return null
    val content = newBody.substring(open + 2, caret - 2)
    if (content.isEmpty() || content.contains('|') || content.contains('\n')) return null

    return SpoilerMarkup(open = open, contentStart = open + 2, contentEnd = caret - 2)
}

private fun backspacedLineStart(oldBody: String, newBody: String, caret: Int): Int {
    if (newBody.length != oldBody.length - 1) return -1
    if (caret < 0 || caret >= oldBody.length) return -1
    if (oldBody[caret] != '\n') return -1
    if (oldBody.removeRange(caret, caret + 1) != newBody) return -1
    return oldBody.take(caret).count { it == '\n' } + 1
}

sealed class RenderItem {
    /** `key`: vedi `groupIntoRenderItems`. */
    data class Run(val blocks: List<BlockEntity>, val key: String) : RenderItem()
    data class Island(val block: BlockEntity) : RenderItem()
}

/**
 * **Una voce della lista della pagina, una volta fatta, resta pronta anche
 * fuori dallo schermo** (dal 28/09/2026). La lista smonta quello che esce
 * dallo schermo e lo rifà quando rientra; un database ci mette più di un
 * fotogramma a rifarsi da capo, e risalendo dal fondo lo si rifaceva al
 * primo millimetro di scorrimento: la pagina si fermava un istante prima di
 * muoversi (trovato dall'utente), e poco dopo un altro scatto per le righe
 * di testo sopra. "Appuntata" alla lista (`LocalPinnableContainer`, lo
 * stesso modo in cui la lista tiene vivo un campo col cursore), la voce non
 * si smonta finché si resta sulla pagina, e rientrando è già lì. Le voci
 * non ancora viste si fanno quando arrivano, come prima.
 */
@Composable
private fun KeepComposed() {
    val pinnable = LocalPinnableContainer.current
    DisposableEffect(pinnable) {
        val handle = pinnable?.pin()
        onDispose { handle?.release() }
    }
}

/**
 * **Quello che mostra un gruppo di righe**, confrontabile (dal 18/10/2026):
 * se è uguale a prima la voce non si ridisegna. Le righe e i numeri dei loro
 * elenchi; non tutta la pagina, che a ogni tasto è un'altra.
 */
@Immutable
data class RunContent(val blocks: List<BlockEntity>, val ordinals: Map<String, Int>)

/** **Quello che mostra un'isola**: il blocco e le righe che ha dentro (un toggle). */
@Immutable
data class IslandContent(val block: BlockEntity, val scope: List<BlockEntity>)

/** Come comincia la chiave di un pezzo che continua un gruppo: vedi `PageEditorViewModel.rechunk`. */
private const val CHUNK_KEY_PREFIX = "chunk:"

/** I numeri delle righe di elenco numerato di queste righe. */
private fun List<BlockEntity>.numbersFrom(ordinals: Map<String, Int>): Map<String, Int> {
    if (none { it.type == BlockType.NUMBERED_LIST_ITEM }) return emptyMap()
    return filter { it.type == BlockType.NUMBERED_LIST_ITEM }.associate { it.id to (ordinals[it.id] ?: 1) }
}

/** Il blocco e tutte le righe che ha dentro, a qualunque profondità. */
private fun scopeOf(block: BlockEntity, childrenOf: Map<String?, List<BlockEntity>>): List<BlockEntity> {
    if (childrenOf[block.id].isNullOrEmpty()) return listOf(block)
    val result = mutableListOf(block)
    var i = 0
    while (i < result.size) {
        childrenOf[result[i].id]?.let { result.addAll(it) }
        i++
    }
    return result
}

/**
 * **Gli attrezzi della pagina per le sue voci**, sempre gli stessi (dal
 * 18/10/2026): così una voce non si ridisegna solo perché la pagina ha rifatto
 * le sue funzioni. Chiamano sempre l'ultima versione di quelle della pagina.
 */
@Stable
class PageItemEnv(
    val viewModel: PageEditorViewModel,
    val factory: ViewModelFactory,
    private val focusChanged: State<(String, Boolean) -> Unit>,
    private val slashAction: State<(BlockEntity, SlashAction) -> Unit>,
    private val navigateToPage: State<(String) -> Unit>,
    private val navigateToDatabase: State<(String) -> Unit>,
    private val blockMenu: State<((String) -> Unit)?>
) {
    val onFocusChanged: (String, Boolean) -> Unit = { id, focused -> focusChanged.value(id, focused) }
    val onSlashAction: (BlockEntity, SlashAction) -> Unit = { block, action -> slashAction.value(block, action) }
    val onNavigateToPage: (String) -> Unit = { navigateToPage.value(it) }
    val onNavigateToDatabase: (String) -> Unit = { navigateToDatabase.value(it) }
    private val openMenu: (String) -> Unit = { id -> blockMenu.value?.invoke(id) }

    /** Null su una pagina bloccata, come prima. */
    val onOpenBlockMenu: ((String) -> Unit)? get() = if (blockMenu.value == null) null else openMenu
}

@Composable
private fun rememberPageItemEnv(
    viewModel: PageEditorViewModel,
    factory: ViewModelFactory,
    onFocusChanged: (String, Boolean) -> Unit,
    onSlashAction: (BlockEntity, SlashAction) -> Unit,
    onNavigateToPage: (String) -> Unit,
    onNavigateToDatabase: (String) -> Unit,
    onOpenBlockMenu: ((String) -> Unit)?
): PageItemEnv {
    val focusChanged = rememberUpdatedState(onFocusChanged)
    val slashAction = rememberUpdatedState(onSlashAction)
    val navigateToPage = rememberUpdatedState(onNavigateToPage)
    val navigateToDatabase = rememberUpdatedState(onNavigateToDatabase)
    val blockMenu = rememberUpdatedState(onOpenBlockMenu)
    return remember(viewModel, factory) {
        PageItemEnv(viewModel, factory, focusChanged, slashAction, navigateToPage, navigateToDatabase, blockMenu)
    }
}

/** I valori della pagina che la testata usa e che non sono già stati: vedi `pageHeader`. */
private data class PageHeaderArgs(
    val isTrashed: Boolean,
    val readOnlyPage: Boolean,
    val inNotebook: Boolean,
    val showTemplatePicker: Boolean,
    val onNavigateToPage: (String) -> Unit
)

/** La testata della pagina come voce della lista: si salta se è la stessa. */
@Composable
private fun PageHeaderSlot(content: @Composable () -> Unit) {
    content()
}

/** Un gruppo di righe come voce della pagina: si salta se non è cambiato. */
@Composable
private fun RunItem(
    content: RunContent,
    runKey: String,
    joinsAbove: Boolean,
    joinsBelow: Boolean,
    env: PageItemEnv,
    focusRequestBlockId: String?,
    formatRequest: Pair<String, FormatType>?
) {
    MergedTextRunField(
        runBlocks = content.blocks,
        runKey = runKey,
        ordinals = content.ordinals,
        joinsAbove = joinsAbove,
        joinsBelow = joinsBelow,
        viewModel = env.viewModel,
        focusRequestBlockId = focusRequestBlockId,
        formatRequest = formatRequest,
        onFocusChanged = env.onFocusChanged,
        onSlashAction = env.onSlashAction
    )
}

/** Un'isola come voce della pagina: si salta se non è cambiata. */
@Composable
private fun IslandItem(
    content: IslandContent,
    env: PageItemEnv,
    focusRequestBlockId: String?,
    formatRequest: Pair<String, FormatType>?
) {
    BlockRow(
        block = content.block,
        allBlocks = content.scope,
        depth = 0,
        viewModel = env.viewModel,
        factory = env.factory,
        focusRequestBlockId = focusRequestBlockId,
        formatRequest = formatRequest,
        onFocusChanged = env.onFocusChanged,
        onNavigateToPage = env.onNavigateToPage,
        onNavigateToDatabase = env.onNavigateToDatabase,
        onOpenBlockMenu = env.onOpenBlockMenu
    )
}

/** La chiave della testata (copertina, icona, titolo) nella lista della pagina. */
private const val PAGE_HEADER_KEY = "page-header"
private const val BACKLINKS_KEY = "backlinks"

/** La chiave di una voce della lista: la stessa per la lista e per i disegni. */
fun renderItemKey(item: RenderItem): String = when (item) {
    is RenderItem.Run -> item.key
    is RenderItem.Island -> "island:" + item.block.id
}

/** I blocchi di una voce, nell'ordine: a questi si attacca un disegno fatto lì sopra. */
private fun renderItemBlockIds(item: RenderItem): List<String> = when (item) {
    is RenderItem.Run -> item.blocks.map { it.id }
    is RenderItem.Island -> listOf(item.block.id)
}

/**
 * Raggruppa una lista piatta di blocchi in sequenze di testo scorrevole
 * consecutive e isole singole, preservando l'ordine.
 *
 * **La chiave di un gruppo di righe è quello che gli sta sopra**:
 * l'isola che lo precede, o l'inizio della pagina. Era il suo primo
 * blocco, ma l'Invio a inizio della prima riga mette la riga vuota
 * nuova *sopra* di lui, e con un altro primo blocco il campo veniva
 * rifatto da capo: la tastiera spariva (trovato dall'utente il
 * 26/09/2026, solo sulla prima riga e solo se aveva del testo). Quello
 * che sta sopra, invece, scrivendo non cambia. Due gruppi di righe non
 * possono avere la stessa isola sopra, né stare tutti e due in cima:
 * fra due gruppi c'è sempre un'isola, altrimenti sarebbero uno solo.
 */

fun groupIntoRenderItems(
    blocks: List<BlockEntity>,
    /**
     * **Dove comincia un pezzo** (dal 18/10/2026): id della riga → chiave
     * del pezzo, da `PageEditorViewModel.chunkStartsOf`. Un gruppo di righe
     * lungo diventa più campi di testo, uno per pezzo; il primo tiene la
     * chiave del gruppo, gli altri quella del loro pezzo, che non cambia
     * anche quando cambia la loro prima riga.
     */
    chunkStarts: Map<String, String> = emptyMap()
): List<RenderItem> {
    val result = mutableListOf<RenderItem>()
    var currentRun = mutableListOf<BlockEntity>()
    var currentKey: String? = null
    var islandAbove: BlockEntity? = null
    fun closeRun() {
        if (currentRun.isEmpty()) return
        val key = currentKey ?: runGroupKey(islandAbove, currentRun.first())
        result.add(RenderItem.Run(currentRun.toList(), key))
        currentRun = mutableListOf()
        currentKey = null
    }
    for (block in blocks) {
        if (block.type in FLOWING_TYPES) {
            val chunkKey = chunkStarts[block.id]
            if (chunkKey != null) {
                // Anche in cima al gruppo: un pezzo che ha preso quello sopra
                // (vedi `PageEditorViewModel.keepVisibleTogether`) tiene la
                // sua chiave, e il suo campo, col fuoco, non si rifà.
                closeRun()
                currentKey = chunkKey
            }
            currentRun.add(block)
        } else {
            closeRun()
            result.add(RenderItem.Island(block))
            islandAbove = block
        }
    }
    closeRun()
    return result
}

@Composable
fun PageEditorScreen(
    pageId: String,
    factory: ViewModelFactory,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenUpdates: (String) -> Unit,
    onNavigateToPage: (String) -> Unit,
    onNavigateToDatabase: (String) -> Unit,
    onOpenSidebar: () -> Unit = {},
    /**
     * Le frecce della pagina di una riga (dal 16/10/2026): apre al posto di
     * questa la pagina di un'altra riga dello stesso database. Null dove non
     * servono.
     */
    onOpenSiblingRow: ((rowId: String) -> Unit)? = null,
    viewModel: PageEditorViewModel = viewModel(factory = factory)
) {
    // **Una pagina di un quaderno** (dal 26/09/2026) è la stessa pagina con
    // lo stesso editor, ma i menu non offrono quello che la legherebbe al
    // menu principale — sottopagine, database, collegamenti — e in cima ha
    // l'intestazione di OneNote invece di icona e copertina.
    val page by viewModel.page.collectAsStateWithLifecycle()
    val keyboardWatch = remember { KeyboardCloseWatch() }
    // **I bottoni** (dal 27/10/2026): chi li fa partire e cosa serve alla
    // finestra delle azioni. I blocchi di "Insert blocks" in questa pagina li
    // mette l'editor, così si annullano.
    val buttonRunner = rememberButtonRunController(
        pages = factory.pageRepository,
        onOpenPage = { id, isDatabase -> if (isDatabase) onNavigateToDatabase(id) else onNavigateToPage(id) },
        onInsert = viewModel::insertButtonBlocks
    )
    val buttonEnv = remember(factory) { ButtonEditorEnv(factory.pageRepository, factory, factory.imageStore) }
    // Il testo incollato da un'altra app porta i suoi link: vedi `PasteLinks.kt`.
    // Gli stessi appunti per i campi di prima e per quelli di Compose 1.8 e dopo.
    val linkClipboard = rememberLinkClipboard(
        richSelection = viewModel::selectedRichText,
        selectedLines = viewModel::selectedLines
    )
    CompositionLocalProvider(
        LocalInNotebook provides (page?.notebookSectionId != null),
        LocalKeyboardCloseWatch provides keyboardWatch,
        LocalButtonRunController provides buttonRunner,
        LocalButtonEditorEnv provides buttonEnv,
        // Il correttore della pagina, se è acceso: vedi `ProofingUi.kt`.
        LocalProofing provides rememberPageProofing(page, viewModel),
        LocalClipboardManager provides linkClipboard,
        LocalClipboard provides linkClipboard
    ) {
        PageEditorContent(
            pageId = pageId,
            factory = factory,
            onBack = onBack,
            onOpenSearch = onOpenSearch,
            onOpenUpdates = onOpenUpdates,
            onNavigateToPage = onNavigateToPage,
            onNavigateToDatabase = onNavigateToDatabase,
            onOpenSidebar = onOpenSidebar,
            onOpenSiblingRow = onOpenSiblingRow,
            viewModel = viewModel
        )
        ButtonRunDialogs(buttonRunner)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PageEditorContent(
    pageId: String,
    factory: ViewModelFactory,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenUpdates: (String) -> Unit,
    onNavigateToPage: (String) -> Unit,
    onNavigateToDatabase: (String) -> Unit,
    onOpenSidebar: () -> Unit,
    onOpenSiblingRow: ((rowId: String) -> Unit)?,
    viewModel: PageEditorViewModel
) {
    val inNotebook = LocalInNotebook.current
    LaunchedEffect(pageId) {
        viewModel.load(pageId)
        // Chi richiama la pagina (dal 03/11/2026): anche tornandoci, perché
        // la menzione può essere appena nata nella pagina di prima.
        viewModel.loadBacklinks()
        // Gira anche tornando su questa pagina dall'indietro: se la
        // schermata di prima ha lasciato un Annulla (un "Move to" dai tre
        // puntini, per esempio), da qui in poi è di questa pagina.
        viewModel.adoptPendingPageChange()
    }
    // Annullata la "Duplicate" stando dentro la copia: la copia è nel
    // cestino, e si torna indietro.
    val leavePage by viewModel.leavePage.collectAsStateWithLifecycle()
    LaunchedEffect(leavePage) {
        if (leavePage) {
            viewModel.consumeLeavePage()
            onBack()
        }
    }

    val page by viewModel.page.collectAsStateWithLifecycle()
    val blocks by viewModel.blocks.collectAsStateWithLifecycle()
    val focusRequestBlockId by viewModel.focusRequestBlockId.collectAsStateWithLifecycle()
    val formatRequest by viewModel.formatRequest.collectAsStateWithLifecycle()
    val selectionTypography by viewModel.selectionTypography.collectAsStateWithLifecycle()
    val selectionLink by viewModel.selectionLink.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current
    // "Scan document" (dal 03/11/2026): vedi `rememberDocumentScanner`.
    val scanDocument = rememberDocumentScanner(viewModel)
    var focusedBlockId by remember { mutableStateOf<String?>(null) }
    var showTypeMenu by remember { mutableStateOf(false) }
    var showFormatBar by remember { mutableStateOf(false) }

    // Il pennello: quale riga colorare, se il colore va davanti o
    // dietro, e l'ultimo scelto — riaprendo la finestra si riparte da
    // lì invece che dal rosso, perché colorare due pezzi dello stesso
    // colore è la cosa che si fa più spesso.
    var showColorPicker by remember { mutableStateOf(false) }
    // La finestra dove si scrive il corpo del testo: sta qui e non nella
    // barra per la ragione spiegata in `FontSizeChip`.
    var showFontSizeDialog by remember { mutableStateOf(false) }
    // La finestrella del collegamento: null chiusa, altrimenti l'indirizzo
    // che c'è già ("" se non ce n'è uno).
    var linkDialogFor by remember { mutableStateOf<String?>(null) }
    // **Il menu del tasto destro sul PC** (dal 13/11/2026, vedi `PcContextMenu.kt`):
    // i campi di testo lo aprono da `LocalPageTextMenu`.
    var pcMenu by remember { mutableStateOf<PcMenuOpen?>(null) }
    val pcMenuHost = remember {
        PageTextMenuHost(
            open = { at, edit -> pcMenu = PcMenuOpen(at, edit) },
            isOpen = { pcMenu != null },
            close = { pcMenu = null }
        )
    }
    // **Tutta la pagina selezionata** (dal 20/10/2026, vedi `PageSelection.kt`).
    var pageSelected by remember(pageId) { mutableStateOf(false) }
    // **I blocchi selezionati col mouse** (dal 10/11/2026, vedi `BlockSelection.kt`).
    val selectedBlocks = remember(pageId) { mutableStateOf<Set<String>>(emptySet()) }
    val blockPicker = remember(pageId) { BlockPicker() }
    val pageListCoordinates = remember { AtomicReference<LayoutCoordinates?>(null) }
    val selectionKeys = remember { FocusRequester() }
    var confirmPageDelete by remember { mutableStateOf(false) }
    val selectWholePage: () -> Unit = remember(focusManager) {
        {
            focusManager.clearFocus()
            selectedBlocks.value = emptySet()
            pageSelected = true
        }
    }
    BackHandler(enabled = pageSelected) { pageSelected = false }
    BackHandler(enabled = selectedBlocks.value.isNotEmpty()) { selectedBlocks.value = emptySet() }
    var colorForBlockId by remember { mutableStateOf<String?>(null) }
    var colorOnBackground by remember { mutableStateOf(false) }
    var lastPickedHex by remember { mutableStateOf<String?>(null) }

    // Il menu del blocco (sei puntini): su quale blocco è aperto. Vedi
    // `BlockActionsHost`, che si occupa di tutto il resto.
    var blockMenuFor by remember { mutableStateOf<String?>(null) }
    // Sul PC: il blocco sotto il mouse, coi sei puntini accanto (vedi `BlockHandle.kt`).
    val blockSpot = remember(pageId) { mutableStateOf<BlockSpot?>(null) }
    // L'ultimo punto del mouse sulla pagina, nella finestra (vedi `FollowPageLayout`).
    val lastPagePointer = remember(pageId) { arrayOfNulls<Offset>(1) }

    // "Linked view of data source": su quale riga si sta scegliendo il
    // database da mostrare, e l'elenco fra cui sceglierlo.
    var linkedViewRequest by remember { mutableStateOf<LinkedViewRequest?>(null) }
    // "Link to page": la riga per cui si sta scegliendo la pagina.
    var linkToPageFor by remember { mutableStateOf<BlockEntity?>(null) }
    // "Link to button": la riga per cui si sta scegliendo il bottone (dal 27/10/2026).
    var linkButtonFor by remember { mutableStateOf<BlockEntity?>(null) }
    var linkableDatabases by remember { mutableStateOf<List<PageRepository.LinkableDatabase>?>(null) }
    val askLinkedViewSource: (BlockEntity, Boolean) -> Unit = { block, keepText ->
        // La tastiera se ne va sotto la finestra comunque, e la riga che
        // diventa vista non deve morire col cursore dentro.
        focusManager.clearFocus()
        linkableDatabases = null
        linkedViewRequest = LinkedViewRequest(block, keepText)
        viewModel.loadLinkableDatabases { linkableDatabases = it }
    }

    // Il menu dei tre puntini e le due cose che può aprire.
    var showPageOptions by remember { mutableStateOf(false) }
    // "Proofing" dai tre puntini: vedi `PageProofingSheet`.
    var showProofing by remember { mutableStateOf(false) }
    // "Paragraph" (dal 21/10/2026): della pagina dai tre puntini, o di un
    // gruppo di righe dalla barra sopra la tastiera e dal menu del blocco.
    var showPageParagraph by remember { mutableStateOf(false) }
    var showFullLine by remember { mutableStateOf(false) }
    // "Page history" dai tre puntini (dal 02/11/2026): vedi `PageHistorySheet`.
    var showHistory by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var paragraphLines by remember { mutableStateOf<List<BlockEntity>?>(null) }
    // "Web Clipper" dai tre puntini (dal 20/10/2026): il pannello, poi la foto in corso.
    var webClipperOpen by remember { mutableStateOf(false) }
    // "Move to": dove la pagina non può andare, letto quando si tocca la
    // voce; finché c'è, l'albero per scegliere è aperto.
    var moveExclusions by remember { mutableStateOf<PageRepository.MoveExclusions?>(null) }
    // La scelta aperta è quella di "Move to database" invece di "Move to".
    var movingIntoDatabase by remember { mutableStateOf(false) }
    var confirmTrash by remember { mutableStateOf(false) }
    var showDuplicate by remember { mutableStateOf(false) }
    // "Blocca con password": bloccare (true) o sbloccare (false), mentre se ne occupano le finestre.
    var passwordLockRequest by remember { mutableStateOf<Boolean?>(null) }

    // **Il cursore che cambia riga chiude la modifica in corso.**
    //
    // La cronologia raggruppa per sessione di scrittura, e una sessione
    // finisce quando si va a scrivere da un'altra parte. Il ViewModel se
    // ne accorge da solo quando l'altra riga viene **scritta**, ma non
    // quando ci si limita a spostare il cursore: lì la modifica di prima
    // sarebbe rimasta in sospeso fino alla successiva, e "Updates"
    // avrebbe sempre avuto un pezzo di ritardo.
    LaunchedEffect(focusedBlockId) { viewModel.closeEditSession() }

    val imeVisible = isKeyboardVisible()

    // Una pagina aperta dal cestino si legge e basta, come una bloccata:
    // in cima c'è la barra per ripristinarla o cancellarla per sempre.
    val isTrashed = page?.trashedAt != null
    val readOnlyPage = page?.isLocked == true || isTrashed

    // **Le voci dei template** (dal 26/09/2026, come su Notion): in una
    // pagina di un database ancora vuota, "+ New template" e "Add existing
    // template", in grigio. Se ne vanno toccando un punto qualsiasi della
    // pagina — vuol dire che non servono — o appena c'è qualcosa scritto;
    // riaprendo la pagina ancora vuota tornano. `blocks` vuoto è la pagina
    // non ancora letta: lì non si propone niente, se no le voci
    // lampeggerebbero anche sulle pagine piene.
    var templatePickerDismissed by remember(pageId) { mutableStateOf(false) }
    var choosingTemplate by remember { mutableStateOf(false) }
    var templatePickerBounds by remember { mutableStateOf<Rect?>(null) }
    var pageAreaOrigin by remember { mutableStateOf(Offset.Zero) }
    // Sul PC "Full width" o al centro (vedi `pageInsetFor`): dalla larghezza del riquadro della pagina.
    var pageAreaWidth by remember { mutableStateOf(PageAreaMemory.width) }
    val pageInset = pageInsetFor(page?.paragraph?.fullWidth != false, pageAreaWidth)
    val pageLeftInset = pageLeftInsetFor(pageInset)
    val pageRightInset = pageRightInsetFor(pageInset)
    val showTemplatePicker = page?.isRowPage == true && !readOnlyPage && !templatePickerDismissed &&
        blocks.isNotEmpty() && blocks.all { block ->
            block.type == BlockType.PARAGRAPH && block.parentBlockId == null && viewModel.plainTextOf(block).isEmpty()
        }

    // **Il disegno a mano** (dal 25/09/2026): la matita nei tre puntini.
    // Mentre si disegna il testo non si scrive (la pagina è come
    // bloccata) e in fondo c'è la barra degli strumenti al posto di
    // quella del testo. I disegni si vedono sempre; si fanno solo qui.
    var drawing by rememberSaveable(pageId) { mutableStateOf(false) }
    val drawingMode = drawing && !readOnlyPage
    val inkViewModel: InkViewModel = viewModel(factory = factory)
    LaunchedEffect(pageId) { inkViewModel.load(pageId) }
    // L'app va in secondo piano: "Page history" salva com'è la pagina, come uscendo.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.saveVersionOnLeave() }
    val inkStrokes by inkViewModel.strokes.collectAsStateWithLifecycle()
    val inkCanUndo by inkViewModel.canUndo.collectAsStateWithLifecycle()
    val inkCanRedo by inkViewModel.canRedo.collectAsStateWithLifecycle()
    BackHandler(enabled = drawingMode) { drawing = false }

    // **La barra resta mentre il cursore passa da una riga all'altra.**
    //
    // Quando una riga cambia specie (un divisore, un toggle, una casella
    // che torna testo) la sua casella di scrittura sparisce, e il fuoco
    // si posa un istante sul campo invisibile prima di arrivare alla
    // riga nuova. In quell'istante nessuna riga ha il fuoco, e la barra
    // — che si vede solo se una riga ce l'ha — spariva e tornava: nel
    // video, fotogrammi interi senza barra con la tastiera ferma al suo
    // posto. Qui, perso il fuoco, la barra aspetta un attimo prima di
    // andarsene; se nel frattempo una riga lo riprende, non si è mossa.
    //
    // Chiudendo la tastiera la barra se ne va subito lo stesso: guarda
    // anche `imeVisible`, e l'attesa vale solo a tastiera aperta.
    var barHeld by remember { mutableStateOf(false) }
    LaunchedEffect(focusedBlockId) {
        if (focusedBlockId != null) {
            barHeld = true
        } else {
            delay(BAR_HANDOFF_GRACE_MS)
            barHeld = false
        }
    }

    // Pagina bloccata: niente barra. Tutto quello che ci sta sopra —
    // rientri, spostamenti, "+" e formattazione — modifica, e offrirla
    // spenta sarebbe solo un modo più lento di dire di no.
    val showBar = !readOnlyPage && !drawingMode &&
        (((focusedBlockId != null || barHeld) && imeVisible) || showTypeMenu)

    val isRootPage = pageId == PageEntity.ROOT_PAGE_ID
    // Sul PC le pagine che contengono questa, per il percorso in alto (`PageBreadcrumb`).
    val breadcrumb by produceState(emptyList<PageEntity>(), pageId) {
        if (isPc) value = factory.pageRepository.ancestorsOf(pageId)
    }
    var confirmDeleteForever by remember { mutableStateOf(false) }
    var confirmNotebookDelete by remember { mutableStateOf(false) }

    // Il menu del blocco aperto **senza la barra**: tenendo premuto un
    // collegamento a una pagina, o dai sei puntini di un database dentro
    // la pagina — righe in cui il cursore non entra. Su una pagina
    // bloccata o nel cestino non c'è, come non c'è la barra: quasi tutte
    // le sue voci cambiano la pagina.
    //
    // **Sempre la stessa funzione** (dal 27/10/2026): le isole la leggono
    // (`PageItemEnv.onOpenBlockMenu`), e rifatta a ogni giro della pagina —
    // la tastiera che si apre o si chiude, il cursore che entra nel titolo —
    // le faceva ridisegnare tutte, coi database dentro: nella home dieci,
    // tutti costruiti anche fuori dallo schermo, ed era il lag della
    // tastiera (misurato: fino a 69 ms per fotogramma, su 8).
    val openBlockMenu: ((String) -> Unit)? = remember(readOnlyPage, focusManager) {
        if (readOnlyPage) {
            null
        } else {
            { id ->
                focusManager.clearFocus()
                blockMenuFor = id
            }
        }
    }

    val onFocusChangedCallback: (String, Boolean) -> Unit = { id, focused ->
        if (focused) {
            focusedBlockId = id
        } else if (focusedBlockId == id) {
            focusedBlockId = null
        }
    }
    // I pezzi delle pagine lunghe si ridividono solo quando non si scrive:
    // vedi `PageEditorViewModel.chunkMembers`.
    LaunchedEffect(focusedBlockId) { viewModel.setEditing(focusedBlockId != null) }

    // Chiusa la tastiera, il cursore si spegne.
    //
    // Di suo il gesto indietro chiude solo la tastiera e lascia il
    // cursore che lampeggia dentro il campo: sembra che si stia ancora
    // scrivendo lì. E **non basta un `BackHandler`**: con la tastiera
    // aperta il tasto indietro lo riceve per prima la tastiera, che se
    // lo mangia per chiudersi, e all'app non arriva niente. Verificato
    // col log — il gesto non passava proprio.
    //
    // Quindi il segnale giusto non è il gesto ma la tastiera che si
    // chiude, comunque lo si faccia. L'attesa prima di spegnere il
    // cursore serve perché la tastiera Samsung sparisce e riappare per
    // un istante durante i cambi di fuoco: se in quell'istante
    // togliessimo il fuoco, romperemmo l'Invio che passa da un blocco
    // al successivo. Se la tastiera torna, l'effetto viene annullato e
    // rilanciato, e il cursore resta dov'è.
    //
    // `hasFocus` guarda tutto quello che sta dentro la pagina — il
    // titolo, il testo, le celle del database incorporato — quindi
    // vale ovunque, non solo sui blocchi.
    // Icona e copertina: quale delle due si sta cambiando, e dove
    // finiscono i file.
    val context = LocalPlatformContext.current
    val imageStore = remember(context) { PageImageStore(context) }
    var editingImage by remember { mutableStateOf<PageImageTarget?>(null) }

    // Spostare la copertina. L'inquadratura di prova vive qui finché
    // non si tocca "Save": scrivere nel database ad ogni millimetro di
    // dito vorrebbe dire non poter più annullare, e far scrivere il
    // disco sessanta volte al secondo.
    var movingCover by remember { mutableStateOf(false) }
    var liveCoverScale by remember { mutableStateOf(1f) }
    var liveCoverOffset by remember { mutableStateOf(Offset.Zero) }
    // Misure che servono a tenere l'immagine dentro la striscia: la
    // striscia stessa e l'immagine come è stata letta dal disco.
    var coverBox by remember { mutableStateOf(IntSize.Zero) }
    var coverImageSize by remember { mutableStateOf(IntSize.Zero) }

    var anythingFocused by remember { mutableStateOf(false) }
    // Il cursore che passa da una riga all'altra è un cambio di campo:
    // vedi `KeyboardCloseWatch`. E se il cursore lo stiamo spostando noi —
    // c'è una richiesta di fuoco in viaggio — la tastiera chiusa non vuol
    // dire che l'utente ha finito di scrivere: vuol dire che è in mezzo a
    // un cambio di campo. Spegnere lì il cursore era uno dei modi in cui
    // la tastiera spariva da sola.
    val keyboardWatch = LocalKeyboardCloseWatch.current ?: remember { KeyboardCloseWatch() }
    LaunchedEffect(focusedBlockId) { keyboardWatch.markFocusChange() }
    ClearFocusWhenKeyboardCloses(
        watch = keyboardWatch,
        anythingFocused = anythingFocused,
        focusInFlight = focusRequestBlockId != null,
        settleDelayMs = IME_CLOSE_SETTLE_MS
    )
    // Resta per il caso in cui il cursore è acceso ma la tastiera è
    // già chiusa: lì il gesto indietro arriva davvero.
    BackHandler(enabled = anythingFocused) { focusManager.clearFocus() }

    // **Niente barra in alto: la pagina comincia subito sotto quella del
    // telefono.** La barra occupava una fascia vuota fra l'orologio e la
    // copertina, che serviva solo a ospitare due icone. Adesso la
    // copertina sale fin sotto l'ultimo pixel della barra del telefono, e
    // indietro e tre puntini stanno **sospesi sopra**, in un cerchietto
    // grigio che si legge su qualunque copertina — vedi `FloatingPageButton`.
    //
    // Senza barra, lo Scaffold riserva in alto solo lo spazio della barra
    // del telefono: è esattamente il margine che si vuole.
    Scaffold { padding ->
        val layoutDirection = LocalLayoutDirection.current
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Del riquadro dello Scaffold si prende tutto **tranne
                // il fondo**: là sotto ci pensa la riga qui sotto.
                .padding(
                    top = padding.calculateTopPadding(),
                    start = padding.calculateStartPadding(layoutDirection),
                    end = padding.calculateEndPadding(layoutDirection)
                )
                .background(DarkBackground)
                .onFocusChanged { anythingFocused = it.hasFocus }
                // **La barra di navigazione oppure la tastiera, non le
                // due sommate.** Prima lo spazio per la barra di
                // navigazione veniva dallo Scaffold e quello per la
                // tastiera da `imePadding`: a tastiera aperta si
                // sommavano, e la barra della formattazione restava
                // sospesa di quei pochi punti sopra i tasti. Con
                // l'unione vince la più alta delle due, che è sempre
                // quella giusta: a tastiera aperta la barra di
                // navigazione le sta dietro e non occupa niente.
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
        ) {
            if (isTrashed) {
                TrashBanner(
                    onBack = onBack,
                    onRestore = {
                        viewModel.restoreFromTrash()
                        showMessage(context, Strings.restoredToMainMenu)
                    },
                    onDeleteForever = { confirmDeleteForever = true }
                )
            }
            val topLevelBlocks = remember(blocks) { blocks.filter { it.parentBlockId == null } }
            // I pezzi delle pagine lunghe: vedi `PageEditorViewModel.chunkMembers`.
            val chunkVersion by viewModel.chunkVersion.collectAsStateWithLifecycle()
            // Il "Paragraph" della pagina (dal 21/10/2026): le sue misure, o
            // quelle di Settings. Un gruppo di righe con uno spazio fra i
            // blocchi tutto suo è un campo a sé (vedi `spacingStartsOf`); i
            // pezzi delle pagine lunghe, dove ci sono, tengono la loro chiave.
            val paragraphLook = remember(page?.paragraphJson, AppSettings.blockSpacing, AppSettings.listGaps) {
                viewModel.paragraphLook(page)
            }
            val renderItems = remember(topLevelBlocks, chunkVersion, paragraphLook) {
                groupIntoRenderItems(
                    topLevelBlocks,
                    viewModel.spacingStartsOf(topLevelBlocks) + viewModel.chunkStartsOf(topLevelBlocks)
                )
            }
            // I numeri degli elenchi e le righe dentro ogni riga, una volta
            // per tutta la pagina: ogni voce prende solo i suoi (vedi `RunContent`).
            val ordinals = remember(blocks) { viewModel.numberedOrdinals(blocks) }
            val childrenOf = remember(blocks) { blocks.groupBy { it.parentBlockId } }
            // I gruppi di righe seguiti da un altro pezzo dello stesso gruppo.
            val continuedRuns = remember(renderItems) {
                renderItems.zipWithNext()
                    .filter { (item, next) -> item is RenderItem.Run && next is RenderItem.Run }
                    .map { (item, _) -> renderItemKey(item) }
                    .toSet()
            }
            // E quelli che ne continuano un altro: due gruppi di righe di fila
            // sono sempre pezzi dello stesso.
            val continuingRuns = remember(renderItems) {
                renderItems.zipWithNext()
                    .filter { (item, next) -> item is RenderItem.Run && next is RenderItem.Run }
                    .map { (_, next) -> renderItemKey(next) }
                    .toSet()
            }
            val listState = rememberLazyListState()
            // **Le righe intere** (dal 19/10/2026, Settings → "Full line
            // view"): le righe lunghe non vanno a capo e scorrono di lato,
            // tutte insieme. Vedi `FullLineView.kt`.
            val sideOffset = rememberSaveable(pageId) { mutableFloatStateOf(0f) }
            // Dal 27/10/2026 la pagina può scegliere da sé ("Full or page line",
            // vedi `FullLineChoice.kt`); su "Default" vale Settings.
            val sideScroll = if (fullLinesOf(page)) {
                remember(listState, sideOffset) { SideScroll(listState, sideOffset) }
            } else {
                null
            }
            // **Le righe sullo schermo in un campo solo** (dal 19/10/2026): così la
            // selezione non si ferma al confine fra due pezzi. Vedi `VisibleChunks.kt`.
            val visibleRunLines = remember(listState) { VisibleRunLines() }
            val pageFinger = remember { mutableStateOf(false) }
            KeepVisibleLinesTogether(
                viewModel = viewModel,
                list = listState,
                lines = visibleRunLines,
                fingerDown = pageFinger,
                chunkVersion = chunkVersion,
                blocksVersion = System.identityHashCode(blocks),
                drawing = drawingMode,
                focusedBlockId = focusedBlockId
            )
            // **La riga dei titoli dei database resta in cima** ("Freeze first
            // row", dal 08/10/2026): la pagina dice alla tabella quando scorre,
            // e dove fermarsi, sotto i pulsanti sospesi (vedi `FrozenTable.kt`).
            val stickyViewport = remember(listState) { StickyViewport.forList(listState, FLOATING_BAR_CLEARANCE) }
            val inkState = remember(listState) { InkPageState(listState) }
            val inkCache = remember { InkPathCache() }
            val inkDensity = LocalDensity.current
            // Dove si attacca un tratto: la testata, poi una voce per
            // gruppo di righe o per isola (vedi `InkStrokeEntity`).
            remember(renderItems) {
                inkState.itemKeys = listOf<Any>(PAGE_HEADER_KEY) + renderItems.map { renderItemKey(it) }
                inkState.anchorOfItem = listOf(InkStrokeEntity.HEADER_ANCHOR) +
                    renderItems.map { item -> renderItemBlockIds(item).joinToString(",") }
                inkState.itemOfBlock = buildMap {
                    put(InkStrokeEntity.HEADER_ANCHOR, 0)
                    renderItems.forEachIndexed { index, item ->
                        renderItemBlockIds(item).forEach { put(it, index + 1) }
                    }
                }
                inkState.slidingItems = renderItems.indices.filter { renderItems[it] is RenderItem.Run }.map { it + 1 }.toSet()
            }
            inkState.sideOffset = if (sideScroll == null) {
                { 0f }
            } else {
                { sideScroll.offset }
            }
            inkState.coverHeightPx = if (page?.coverImage != null) with(inkDensity) { COVER_HEIGHT.toPx() } else 0f
            val focusPark = remember { FocusRequester() }

            // **Il salto di un collegamento dentro la pagina** (dal 20/10/2026,
            // l'indice "come su Kindle"; vedi `PageAnchors.kt`): la riga va in
            // cima, sotto i pulsanti sospesi, e si illumina; "Back" riporta
            // dov'era la pagina. Una riga dentro un toggle porta al toggle.
            var linkReturn by remember(pageId) { mutableStateOf<LinkReturn?>(null) }
            // **"Back" quando si è scesi molto** (dal 27/10/2026): vedi `BackToTop.kt`.
            val backToTop = rememberBackToTop(listState)
            val movedBlockNow by viewModel.movedBlock.collectAsStateWithLifecycle()
            val veilColor = MaterialTheme.colorScheme.primary
            val jumpScope = rememberCoroutineScope()
            // **Le menzioni "@"** (dal 03/11/2026, vedi `Mentions.kt`): toccata
            // una pagina la si apre (un database nella sua vista), toccata una
            // data la si cambia col calendario.
            val openMentionedPage: (String) -> Unit = remember(factory) {
                { id ->
                    jumpScope.launch {
                        val target = withContext(Dispatchers.IO) { factory.pageRepository.getPage(id) }
                        when {
                            target == null || target.trashedAt != null ->
                                showMessage(context, MentionStrings.pageGone)
                            target.isDatabase -> onNavigateToDatabase(id)
                            else -> onNavigateToPage(id)
                        }
                    }
                }
            }
            var editingDateMention by remember(pageId) { mutableStateOf<String?>(null) }
            val tapDateMention: (String) -> Unit = remember { { link -> editingDateMention = link } }
            editingDateMention?.let { link ->
                SamsungDateDialog(
                    initial = dateLinkTarget(link) ?: LocalDate.now(),
                    onPick = { date -> viewModel.changeDateMention(link, date, MentionStrings.dateText(date)) },
                    onDismiss = { editingDateMention = null }
                )
            }
            val backlinks by viewModel.backlinks.collectAsStateWithLifecycle()
            val latestRenderItems = rememberUpdatedState(renderItems)
            val latestBlocks = rememberUpdatedState(blocks)
            val jumpMarginPx = with(inkDensity) { (FLOATING_BAR_CLEARANCE + 12.dp).toPx() }
            val jumpToBlock: (String) -> Unit = remember(listState, visibleRunLines) {
                { blockId ->
                    jumpScope.launch {
                        val byId = latestBlocks.value.associateBy { it.id }
                        var anchor = byId[blockId]
                        while (anchor?.parentBlockId != null) anchor = byId[anchor.parentBlockId]
                        val items = latestRenderItems.value
                        val found = anchor?.let { a -> items.indexOfFirst { a.id in renderItemBlockIds(it) } } ?: -1
                        if (anchor == null || found < 0) {
                            showMessage(context, EditorStrings.linkTargetMissing)
                            return@launch
                        }
                        focusManager.clearFocus()
                        val index = found + HEADER_ITEM_COUNT
                        linkReturn = LinkReturn(
                            index = listState.firstVisibleItemIndex,
                            offset = listState.firstVisibleItemScrollOffset,
                            upward = index > listState.firstVisibleItemIndex
                        )
                        listState.scrollToItem(index)
                        val item = items[found]
                        if (item is RenderItem.Run) {
                            // La riga sta dentro il gruppo: si aspetta che sia disegnato per sapere dove.
                            withFrameNanos { }
                            withFrameNanos { }
                            visibleRunLines.lineTops[renderItemKey(item)]?.invoke(anchor.id)?.let { top ->
                                listState.scrollToItem(index, top.roundToInt())
                            }
                        }
                        listState.scrollBy(-jumpMarginPx)
                        viewModel.highlightBlock(anchor.id)
                    }
                }
            }
            // **La foto del Web Clipper appena arrivata** (dal 20/10/2026): la
            // pagina scorre fin lì (senza "Back": non è un collegamento). Si
            // aspetta qualche fotogramma che la pagina la mostri.
            val revealRequest by viewModel.revealBlock.collectAsStateWithLifecycle()
            LaunchedEffect(revealRequest) {
                val blockId = revealRequest ?: return@LaunchedEffect
                viewModel.consumeReveal()
                repeat(REVEAL_ATTEMPTS) {
                    val byId = latestBlocks.value.associateBy { it.id }
                    var anchor = byId[blockId]
                    while (true) {
                        val parentId = anchor?.parentBlockId ?: break
                        anchor = byId[parentId]
                    }
                    val anchorId = anchor?.id
                    val found = if (anchorId == null) -1 else latestRenderItems.value.indexOfFirst { anchorId in renderItemBlockIds(it) }
                    if (found >= 0) {
                        listState.animateScrollToItem(found + HEADER_ITEM_COUNT)
                        listState.scrollBy(-jumpMarginPx)
                        return@LaunchedEffect
                    }
                    withFrameNanos { }
                }
            }

            // **Cosa fa una voce del catalogo**, scelta dal menu "/" o dal
            // menu "+": una funzione sola per tutti e due. Erano due copie
            // quasi uguali, una per menu, e si stavano già allontanando
            // (il "+" non sapeva mettere la vista giusta a un database
            // nuovo); con una sola, la stessa voce fa la stessa cosa da
            // dovunque la si scelga. Vedi `SLASH_ENTRIES`.
            // **"Paragraph"** (dal 21/10/2026, chiesto dall'utente): lo spazio fra i
            // blocchi e dopo i marcatori solo per le righe che il testo selezionato
            // tocca, o per quella del cursore. La selezione si mette da parte prima,
            // come per il pennello; poi il cursore si spegne, perché cambiando lo
            // spazio quelle righe diventano un campo a sé, e un campo che sparisce
            // col cursore dentro fa chiudere l'app.
            val openParagraph: (BlockEntity?) -> Unit = { focused ->
                viewModel.captureSelection()
                val lines = viewModel.capturedSelectionBlocks().ifEmpty { listOfNotNull(focused) }
                if (lines.isNotEmpty()) {
                    focusManager.clearFocus()
                    paragraphLines = lines
                }
            }
            val applyInsertAction: (BlockEntity, SlashAction) -> Unit = { block, action ->
                // Le voci del 03/11/2026: registrare e scansionare.
                if (action == SlashAction.Record || action == SlashAction.Scan) focusManager.clearFocus()
                when (action) {
                    is SlashAction.Type -> if (action.type == BlockType.NOTIFICATION) {
                        // Come il toggle qui sotto, la riga esce dal testo
                        // condiviso. La notifica nasce alle prossime 9:00
                        // e si apre subito la finestra per scegliere
                        // quando.
                        focusPark.requestFocus()
                        viewModel.makeNotification(block, null)
                        NotificationPermission.request()
                    } else {
                        // Diventando toggle la riga esce
                        // dal testo condiviso: se era
                        // l'unica, quel campo muore col
                        // cursore dentro, che è il modo
                        // in cui l'app si chiudeva. Il
                        // fuoco si posa prima sul campo
                        // invisibile, e da lì passa al
                        // titolo del toggle.
                        // Lo stesso per riquadro e citazione (dal
                        // 06/10/2026): anche loro sono righe a sé.
                        if (action.type == BlockType.TOGGLE || action.type in MULTILINE_TYPES) focusPark.requestFocus()
                        // La tabella è un'isola come il database qui
                        // sotto, e come lui non si prende il cursore.
                        if (action.type == BlockType.TABLE) focusManager.clearFocus()
                        viewModel.updateBlockType(block, action.type)
                    }
                    SlashAction.PageLink ->
                        viewModel.convertToPageLink(block) { onNavigateToPage(it) }
                    SlashAction.Divider -> {
                        // Come per il toggle: se la riga
                        // era la prima del suo gruppo di
                        // testo, la casella in cui si sta
                        // scrivendo sparisce. Senza il
                        // posteggio il fuoco restava a
                        // nessuno, la tastiera cominciava
                        // a chiudersi e tornava, e la
                        // pagina scorreva via fino al
                        // database (visto nel video).
                        focusPark.requestFocus()
                        viewModel.convertToDividerAndFocusNext(block)
                    }
                    // Il database nasce dentro la
                    // pagina; la vista gliela mettiamo
                    // appena esiste, così "Calendar
                    // view" dà un calendario e non una
                    // tabella da cambiare a mano.
                    //
                    // **Prima si spegne il cursore** (dal 06/10/2026): la
                    // riga sola sotto un divisore o un altro database è
                    // un gruppo di testo tutto suo, e diventando database
                    // il suo campo spariva col cursore dentro — l'app si
                    // chiudeva (trovato dall'utente il 03/10/2026). Nel
                    // database non c'è niente da scrivere, e la tastiera
                    // se ne va: come per "Linked view of data source".
                    is SlashAction.Database -> {
                        focusManager.clearFocus()
                        viewModel.convertToDatabaseLink(block, simple = action.simple) { databaseId ->
                            action.layout?.let {
                                viewModel.setDatabaseLayout(databaseId, it)
                            }
                        }
                    }
                    SlashAction.LinkedView -> askLinkedViewSource(block, false)
                    // Prima si sceglie la pagina: il cursore si spegne, la
                    // tastiera se ne andrebbe comunque sotto la finestra.
                    SlashAction.LinkToPage -> {
                        focusManager.clearFocus()
                        linkToPageFor = block
                    }
                    // Il blocco nasce vuoto e apre la scelta del file: la
                    // tastiera non serve.
                    is SlashAction.Media -> {
                        focusManager.clearFocus()
                        viewModel.convertToMedia(block, action.type)
                    }
                    // Come "Page": il database nasce e ci si entra.
                    SlashAction.FullPageDatabase -> {
                        focusManager.clearFocus()
                        viewModel.convertToFullPageDatabase(block) { onNavigateToDatabase(it) }
                    }
                    // Il bottone non ha testo da scrivere: si apre la finestra delle azioni.
                    SlashAction.Button -> {
                        focusManager.clearFocus()
                        viewModel.convertToButton(block)
                    }
                    SlashAction.LinkButton -> {
                        focusManager.clearFocus()
                        linkButtonFor = block
                    }
                    // Il cursore va sulla prima riga dentro il blocco.
                    SlashAction.Synced -> viewModel.makeSyncedBlock(block)
                    // La riga esce dal testo condiviso, come il toggle: il fuoco
                    // si posa prima, poi va nella prima colonna.
                    is SlashAction.Columns -> {
                        focusPark.requestFocus()
                        viewModel.makeColumns(block, action.count)
                    }
                    // Un blocco audio che registra subito (vedi `AudioRecorder`).
                    SlashAction.Record -> viewModel.convertToRecording(block)
                    // Lo scanner di Google: le pagine diventano foto qui.
                    SlashAction.Scan -> scanDocument(block.id)
                    SlashAction.NotYet -> Unit
                }
            }
            // **La voce scelta dal menu "+"** (della barra, o del tasto destro sul PC).
            // Con del testo selezionato cambiano tutte le righe che tocca (dal
            // 14/10/2026, chiesto dall'utente): prima solo quella col cursore. Vale
            // per i tipi che una riga di testo diventa da sola; la notifica (che apre
            // la sua finestra), la tabella e il resto restano della riga col cursore.
            // La selezione si è messa da parte aprendo il menu.
            val applyTypeMenuEntry: (BlockEntity, SlashEntry) -> Unit = { block, entry ->
                val action = entry.action
                val selected = viewModel.capturedSelectionBlocks()
                if (action is SlashAction.Type && action.type in TURN_INTO_MANY && selected.isNotEmpty()) {
                    if (action.type == BlockType.TOGGLE || action.type in MULTILINE_TYPES) focusPark.requestFocus()
                    viewModel.updateBlockTypes(selected, action.type)
                } else {
                    // La stessa funzione del menu "/": vedi `applyInsertAction`.
                    applyInsertAction(block, action)
                }
            }

            // **Una riga che non si vede non esiste.** `LazyColumn`
            // costruisce solo le righe dentro lo schermo: il blocco
            // nato premendo Invio in fondo alla parte visibile
            // spuntava dietro la tastiera, non veniva mai costruito, e
            // quindi non poteva prendersi il cursore. Il risultato era
            // il bug per cui Invio sembrava non fare niente e le
            // lettere successive finivano tutte nella riga di prima,
            // attaccate. Qui, prima ancora del fuoco, portiamo la riga
            // dentro lo schermo: costruita quella, il fuoco la trova.
            LaunchedEffect(focusRequestBlockId, renderItems) {
                val wanted = focusRequestBlockId ?: return@LaunchedEffect
                val found = renderItems.indexOfFirst { item ->
                    when (item) {
                        is RenderItem.Run -> item.blocks.any { it.id == wanted }
                        is RenderItem.Island -> item.block.id == wanted
                    }
                }
                if (found < 0) return@LaunchedEffect
                // La prima voce della lista è l'intestazione (copertina,
                // icona, titolo): i blocchi cominciano da quella dopo.
                val index = found + HEADER_ITEM_COUNT
                // Due fotogrammi di respiro: subito dopo un cambio di
                // struttura le misure della lista sono ancora quelle di
                // prima, e scorrere su misure vecchie porta fuori
                // schermo roba che stava benissimo dov'era.
                withFrameNanos { }
                withFrameNanos { }
                val info = listState.layoutInfo
                val visible = info.visibleItemsInfo
                if (visible.isEmpty()) return@LaunchedEffect
                val row = visible.find { it.index == index }
                // **Basta che la riga esista, non che si veda tutta.**
                // Una riga di testo scorrevole è alta quanto tutto il
                // gruppo che contiene — mezza pagina — e pretendere di
                // vederla intera faceva scorrere via quello che stava
                // sopra: sembrava che le caselle sparissero. Di dove
                // finisce esattamente il cursore si occupa già il campo
                // di testo, che si porta dietro la pagina mentre si
                // scrive. Qui si interviene solo quando la riga è
                // **tutta** fuori, sotto o sopra.
                val extraRoom = (visible.first().size) / 2
                when {
                    row != null && row.offset >= info.viewportEndOffset ->
                        listState.animateScrollBy(
                            (row.offset - info.viewportEndOffset + extraRoom).toFloat()
                        )
                    row != null && row.offset + row.size <= info.viewportStartOffset ->
                        listState.animateScrollBy(
                            (row.offset - info.viewportStartOffset).toFloat()
                        )
                    row != null -> Unit // si vede almeno in parte: va bene così
                    index > visible.last().index -> {
                        val last = visible.last()
                        val rowHeight = visible.maxOf { it.size }
                        val below = last.offset + last.size - info.viewportEndOffset
                        listState.animateScrollBy(
                            ((index - last.index) * rowHeight + below + extraRoom).toFloat()
                        )
                    }
                    index < visible.first().index -> listState.animateScrollToItem(index)
                }
            }

            // **Il posteggio della tastiera.**
            //
            // Un campo di testo alto un punto e invisibile, che esiste
            // sempre perché sta fuori dalla lista a scorrimento (dentro,
            // sparirebbe appena esce dallo schermo).
            //
            // Serve nei due momenti in cui un blocco cambia specie — da
            // riga a sé a testo scorrevole — e la sua casella di
            // scrittura viene distrutta. Distruggerla mentre ha il fuoco
            // fa chiudere l'app: la tastiera continua a chiederle dov'è
            // il cursore e non la trova (`LayoutCoordinate ...
            // isAttached`, difetto di Compose 1.6). Riprodotto a comando
            // venti volte su venti.
            //
            // Prima si staccava la tastiera, e infatti l'app non si
            // chiudeva più — ma la tastiera **spariva e tornava**
            // (misurato: `onHidden`, e `onShown` 315 ms dopo). Qui
            // invece il fuoco si posa un istante su questo campo: la
            // tastiera resta aperta perché sotto di sé ha sempre un
            // campo vero, e riparte sul blocco nuovo appena esiste.
            AutoCaps {
                BasicTextField(
                    value = "",
                    onValueChange = { },
                    modifier = Modifier
                        .size(1.dp)
                        .alpha(0f)
                        .focusRequester(focusPark)
                        .markFocusChanges()
                )
            }

            // **Col menu del "+" aperto, la pagina sotto non si tocca.**
            //
            // Toccando una riga di testo il tocco se lo prendeva il
            // campo di scrittura — il gestore qui sotto vede solo quello
            // che i figli non hanno già preso — quindi **il cursore si
            // spostava** e il menu restava aperto. Un velo invisibile
            // steso sopra la pagina intercetta il tocco prima di
            // chiunque: chiude il menu e non lascia passare altro.
            //
            // Copre solo la pagina, non la barra: così il "+" resta un
            // interruttore vero e lo si può ritoccare per chiudere.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onGloballyPositioned { pageAreaOrigin = it.positionInWindow() }
                    .onSizeChanged { size ->
                        val width = with(inkDensity) { size.width.toDp() }
                        pageAreaWidth = width
                        PageAreaMemory.width = width
                    }
                    // Le dita sulla pagina, solo guardate: vedi `LocalPageFingerDown`.
                    .watchPageFingers(pageFinger)
                    // Un tocco fuori dalle voci dei template le manda via.
                    // Si guarda soltanto, prima di tutti e senza prenderlo:
                    // il tocco arriva lo stesso dove doveva, e se era sulla
                    // prima riga ci si comincia a scrivere.
                    .pointerInput(showTemplatePicker) {
                        if (!showTemplatePicker) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            val bounds = templatePickerBounds
                            if (bounds == null || !bounds.contains(down.position + pageAreaOrigin)) {
                                templatePickerDismissed = true
                            }
                        }
                    }
                    .inkLayer(inkState, inkStrokes, inkCache, NotionWhite, originX = pageLeftInset)
                    .then(
                        if (drawingMode) {
                            Modifier.inkInput(
                                state = inkState,
                                originX = pageLeftInset,
                                onStroke = { anchor, pen, points ->
                                    inkViewModel.addStroke(anchor, pen.tool, pen.color, pen.width, points)
                                },
                                onErase = { x, y ->
                                    val info = listState.layoutInfo
                                    val d = inkDensity.density
                                    val settings = InkPrefs.settings
                                    inkViewModel.eraseAt(x / d, y / d, settings.eraserRadius, settings.eraserMode) { stroke ->
                                        resolveAnchor(stroke.entity.anchor, inkState.itemOfBlock)
                                            ?.let { inkState.itemTop(info, it) }
                                            ?.let { 0f to it / d }
                                    }
                                },
                                onEraseEnd = { inkViewModel.endErase() }
                            )
                        } else {
                            Modifier
                        }
                    )
            ) {
            // "Select all" nel menu del testo seleziona tutta la pagina (vedi `PageSelection.kt`).
            val baseTextToolbar = LocalTextToolbar.current
            val wholePageToolbar = remember(baseTextToolbar, selectWholePage) { WholePageTextToolbar(baseTextToolbar, selectWholePage) }
            CompositionLocalProvider(
                LocalTextToolbar provides wholePageToolbar,
                LocalStickyViewport provides stickyViewport,
                LocalFocusPark provides focusPark,
                LocalSideScroll provides sideScroll,
                LocalVisibleRunLines provides visibleRunLines,
                LocalBlockLinkJump provides jumpToBlock,
                LocalPageMentionOpen provides openMentionedPage,
                LocalDateMentionTap provides tapDateMention,
                LocalPageFingerDown provides pageFinger,
                LocalBlockPicker provides blockPicker,
                LocalBlockSelection provides remember(veilColor) { BlockSelectionLook(selectedBlocks, veilColor) },
                // Mentre si disegna, il testo si legge e basta.
                LocalPageLocked provides (readOnlyPage || drawingMode)
                // (Fino al 25/09/2026 qui passavano anche il font e il corpo
                // della pagina intera, `LocalPageTypography`: ora stanno sul
                // testo selezionato, dentro gli span — vedi `fontStyleOf`.)
            ) {
            // **La testata si fa una volta sola** (dal 18/10/2026): copertina,
            // icona, titolo e proprietà della riga. Dentro la lista la si
            // rifaceva a ogni tasto scritto nella pagina, perché a ogni tasto
            // la lista si rifà; così si ridisegna solo quando cambia quello che
            // mostra (la pagina, i suoi stati, e questi valori qui sotto).
            val headerArgs = rememberUpdatedState(
                PageHeaderArgs(isTrashed, readOnlyPage, inNotebook, showTemplatePicker, onNavigateToPage)
            )
            val pageHeader: @Composable () -> Unit = remember(pageId) {
                {
                    val (isTrashed, readOnlyPage, inNotebook, showTemplatePicker, onNavigateToPage) = headerArgs.value
                    Column {
                // La copertina sta sopra a tutto, a tutta larghezza, e si
                // tocca per cambiarla. Senza copertina non occupa spazio:
                // per metterla c'è il pulsante accanto all'icona.
                val coverImage = if (inNotebook) null else page?.coverImage
                // Senza copertina, in cima si lascia la fascia dei
                // pulsanti sospesi: altrimenti indietro e tre puntini
                // finirebbero sopra l'icona.
                // Nel cestino i pulsanti sospesi non ci sono (c'è la barra
                // del cestino), e la fascia resterebbe vuota.
                if (coverImage == null && !isTrashed) {
                    // Sul PC la pagina comincia già sotto la barra in alto.
                    Spacer(modifier = Modifier.height(if (isPc) 24.dp else FLOATING_BAR_CLEARANCE))
                }
                if (coverImage != null) {
                    // Mentre si sposta comanda l'inquadratura di prova:
                    // quella salvata torna solo se si annulla. Così il
                    // dito muove l'immagine subito, senza scrivere nel
                    // database ad ogni millimetro.
                    val shownScale = if (movingCover) liveCoverScale else page?.coverScale ?: 1f
                    val shownOffset = if (movingCover) {
                        liveCoverOffset
                    } else {
                        Offset(page?.coverOffsetX ?: 0f, page?.coverOffsetY ?: 0f)
                    }

                    Box(
                        modifier = Modifier
                            .bleedSides(pageLeftInset, pageRightInset)
                            .fillMaxWidth()
                            .height(COVER_HEIGHT)
                            .onSizeChanged { coverBox = it }
                    ) {
                        CoverImage(
                            fileName = coverImage,
                            store = imageStore,
                            scale = shownScale,
                            offsetFraction = shownOffset,
                            onImageSize = { coverImageSize = it },
                            modifier = Modifier
                                .matchParentSize()
                                .then(
                                    if (movingCover) {
                                        Modifier.pointerInput(coverImage, coverImageSize, coverBox) {
                                            detectTransformGestures { _, pan, zoom, _ ->
                                                val newScale = (liveCoverScale * zoom)
                                                    .coerceIn(1f, MAX_COVER_ZOOM)
                                                val limit = coverPanLimit(
                                                    coverBox,
                                                    coverImageSize.width,
                                                    coverImageSize.height,
                                                    newScale
                                                )
                                                val moved = Offset(
                                                    liveCoverOffset.x * coverBox.width + pan.x,
                                                    liveCoverOffset.y * coverBox.height + pan.y
                                                )
                                                liveCoverScale = newScale
                                                liveCoverOffset = Offset(
                                                    moved.x.coerceIn(-limit.x, limit.x) /
                                                        coverBox.width.coerceAtLeast(1),
                                                    moved.y.coerceIn(-limit.y, limit.y) /
                                                        coverBox.height.coerceAtLeast(1)
                                                )
                                            }
                                        }
                                    } else {
                                        Modifier.clickable(enabled = !isTrashed) { editingImage = PageImageTarget.COVER }
                                    }
                                )
                        )

                        if (movingCover) {
                            CoverMoveOverlay(
                                onCancel = { movingCover = false },
                                onSave = {
                                    viewModel.setCoverTransform(
                                        liveCoverScale,
                                        liveCoverOffset.x,
                                        liveCoverOffset.y
                                    )
                                    movingCover = false
                                }
                            )
                        }
                    }
                }

                // Un template si scrive come una pagina: lo dice una riga in
                // grigio, perché da fuori le due cose non si distinguono.
                if (page?.isTemplate == true) {
                    Row(
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Description,
                            contentDescription = null,
                            tint = NotionGray400,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            TemplateStrings.editingTemplate,
                            style = MaterialTheme.typography.labelMedium,
                            color = NotionGray400
                        )
                    }
                }

                // **Il titolo** (vedi sotto, `TitleField`): dove lo vuole
                // Settings → "Title position" (dal 05/10/2026). Le pagine dei
                // quaderni non hanno icona, e il titolo resta dov'è.
                val titlePosition = if (inNotebook) TitlePosition.BELOW_ICON else AppSettings.titlePosition

                // Mentre si scrive comanda il testo locale, non quello che
                // torna dal database: il salvataggio è immediato ma il giro
                // di ritorno no, e se nel frattempo è stato battuto un
                // altro tasto rimetterebbe il titolo com'era un attimo
                // prima, mangiandosi il carattere. Fuori dalla scrittura
                // comanda il database, così una rinomina fatta altrove si
                // vede appena si torna qui.
                var titleFocused by remember { mutableStateOf(false) }
                var titleText by remember { mutableStateOf(page?.title.orEmpty()) }
                LaunchedEffect(page?.title, titleFocused) {
                    if (!titleFocused) titleText = page?.title.orEmpty()
                }
                // Il correttore anche nel titolo: vedi `ProofingUi.kt`.
                val titleProofing = LocalProofing.current
                val titleMisspellings = rememberMisspellings(if (titleProofing == null) emptyList() else listOf(titleText))
                var titleLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
                var titleTyping by remember { mutableStateOf<Int?>(null) }
                var titleMenu by remember { mutableStateOf<Pair<MisspelledWord, IntRect>?>(null) }

                @Composable
                fun TitleField(modifier: Modifier) {
                    AutoCaps {
                        BasicTextField(
                            value = titleText,
                            // Un titolo sta su una riga sola: l'a capo digitato non
                            // ha senso e l'incolla di un testo che ne contiene va
                            // ripulito. La tastiera offre "fine" e basta: per
                            // scrivere nel corpo si tocca il corpo.
                            onValueChange = {
                                val singleLine = it.replace("\n", "")
                                titleTyping = if (singleLine != titleText) singleLine.length else null
                                titleText = singleLine
                                viewModel.updateTitle(singleLine)
                            },
                            // **Sul PC un titolo lungo va a capo** (dal 11/11/2026, chiesto
                            // dall'utente: con lo zoom si tagliava), come in Notion; gli a capo
                            // restano fuori dal testo, e Invio esce dal titolo come "fine".
                            singleLine = !isPc,
                            visualTransformation = if (titleProofing == null) {
                                emojiTransformation
                            } else {
                                ProofingTransformation(titleMisspellings[titleText].orEmpty(), titleTyping.takeIf { titleFocused })
                            },
                            onTextLayout = { titleLayout = it },
                            // Pagina bloccata (o nel cestino): nemmeno il titolo si tocca.
                            readOnly = readOnlyPage,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            textStyle = MaterialTheme.typography.titleLarge.copy(color = NotionWhite),
                            cursorBrush = SolidColor(NotionWhite),
                            // Una pagina di un quaderno nasce senza titolo: al suo
                            // posto, grigio, "Untitled page", come in OneNote.
                            decorationBox = { field ->
                                Box {
                                    if ((inNotebook || page?.isTemplate == true) && titleText.isEmpty()) {
                                        Text(
                                            if (inNotebook) NotebookStrings.untitledPage else TemplateStrings.untitledTemplate,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = NotionGray400
                                        )
                                    }
                                    field()
                                    titleMenu?.let { (word, at) ->
                                        if (word.end <= titleText.length) {
                                            ProofingMenu(
                                                word = titleText.substring(word.start, word.end),
                                                anchor = at,
                                                onReplace = { with ->
                                                    if (word.end <= titleText.length) {
                                                        val new = titleText.replaceRange(word.start, word.end, with)
                                                        titleText = new
                                                        titleTyping = null
                                                        viewModel.updateTitle(new)
                                                    }
                                                },
                                                onDismiss = { titleMenu = null }
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = modifier
                                .onPreviewKeyEvent { event ->
                                    val enter = event.key == Key.Enter || event.key == Key.NumPadEnter
                                    (isPc && enter && event.type == KeyEventType.KeyDown).also { if (it) focusManager.clearFocus() }
                                }
                                .drawBehind { drawMisspellings(titleLayout) }
                                .proofingTaps(enabled = titleProofing != null && !readOnlyPage, layout = { titleLayout }) { word, at ->
                                    titleMenu = word to at
                                }
                                .markFocusChanges()
                                .onFocusChanged {
                                    titleFocused = it.isFocused
                                    // Uscendo dal titolo la sua modifica si
                                    // chiude, come per una riga qualsiasi.
                                    if (!it.isFocused) viewModel.closeEditSession()
                                }
                        )
                    }
                }

                if (!inNotebook) Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val iconImage = page?.iconImage
                    // **La pagina di una riga di database nasce senza
                    // icona**, come su Notion: niente foglietto 📄 di
                    // riserva, ma "Add icon". Così togliendo l'icona
                    // sparisce davvero — e nel database, accanto al nome
                    // della riga, non resta niente. Un'emoji scelta (dal
                    // 07/10/2026) si vede. Le altre pagine tengono la loro
                    // emoji di sempre.
                    val rowPageWithoutIcon = page?.isRowPage == true && page?.hasChosenIcon != true
                    if (rowPageWithoutIcon) {
                        if (!isTrashed) {
                            Text(
                                text = EditorStrings.addIcon,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable { editingImage = PageImageTarget.ICON }
                                    .padding(vertical = 6.dp)
                            )
                        }
                    } else if (iconImage != null) {
                        PageImage(
                            fileName = iconImage,
                            store = imageStore,
                            contentDescription = EditorStrings.icon,
                            modifier = Modifier
                                .size(PAGE_ICON_SIZE)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !isTrashed) { editingImage = PageImageTarget.ICON }
                        )
                    } else if (page?.iconSymbol != null) {
                        SymbolIcon(
                            page?.iconSymbol,
                            PAGE_ICON_SIZE,
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !isTrashed) { editingImage = PageImageTarget.ICON }
                        )
                    } else {
                        Text(
                            text = page?.icon ?: "📄",
                            style = TextStyle(fontSize = 40.sp, fontFamily = emojiFontFamily),
                            modifier = Modifier.clickable(enabled = !isTrashed) { editingImage = PageImageTarget.ICON }
                        )
                    }

                    // "Add cover" non sta più qui (dal 05/10/2026): è in cima
                    // ai tre puntini. Col titolo accanto all'icona, il titolo.
                    if (titlePosition == TitlePosition.BESIDE_ICON) {
                        Spacer(modifier = Modifier.size(12.dp))
                        TitleField(Modifier.weight(1f))
                    }
                }

                if (titlePosition == TitlePosition.BELOW_ICON) {
                    TitleField(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
                if (inNotebook) {
                    page?.let { NotebookPageStamp(it.createdAt) }
                }

                // **Le proprietà della riga** (dal 26/09/2026), come su
                // Notion: fra il titolo e il testo, separate da una linea.
                if (page?.isRowPage == true && !inNotebook) {
                    RowPageProperties(
                        pageId = pageId,
                        factory = factory,
                        editable = !readOnlyPage,
                        modifier = Modifier.padding(top = 8.dp),
                        onOpenPage = onNavigateToPage
                    )
                }
                if (showTemplatePicker) {
                    TemplatePicker(
                        onNewTemplate = {
                            focusManager.clearFocus()
                            viewModel.createTemplate { id -> onNavigateToPage(id) }
                        },
                        onAddExisting = {
                            focusManager.clearFocus()
                            choosingTemplate = true
                        },
                        modifier = Modifier.onGloballyPositioned { templatePickerBounds = it.boundsInWindow() }
                    )
                }

                Spacer(modifier = Modifier.size(12.dp))
                    }
                }
            }
            val itemEnv = rememberPageItemEnv(
                viewModel = viewModel,
                factory = factory,
                onFocusChanged = onFocusChangedCallback,
                onSlashAction = applyInsertAction,
                onNavigateToPage = onNavigateToPage,
                onNavigateToDatabase = onNavigateToDatabase,
                onOpenBlockMenu = openBlockMenu
            )
            // **Il contenuto della lista resta lo stesso finché non cambia quello
            // che mostra** (dal 27/10/2026). Rifatto a ogni giro della pagina, la
            // lista ripartiva da capo con tutte le voci che tiene pronte — nella
            // home tredici, i database fuori dallo schermo compresi — anche quando
            // a cambiare era solo la tastiera che si apriva o si chiudeva: mezzo
            // millisecondo l'una, abbastanza per perdere il fotogramma.
            val latestRenderItems = rememberUpdatedState(renderItems)
            // I tasti della pagina dentro i suoi campi (vedi `BlockSelection.kt`): Ctrl+A tutta la
            // pagina, Ctrl+Z e Ctrl+Y l'Annulla e il Ripristina della pagina. Vero se presi.
            val pageKey: (KeyEvent) -> Boolean = remember(viewModel, selectWholePage) {
                { event ->
                    val undo = undoKeyOf(event)
                    when {
                        undo == true -> viewModel.undo()
                        undo == false -> viewModel.redo()
                        isSelectAll(event) -> selectWholePage()
                    }
                    undo != null || isSelectAll(event)
                }
            }
            val listContent: LazyListScope.() -> Unit = remember(
                pageHeader, renderItems, itemEnv, paragraphLook, ordinals, childrenOf, continuedRuns,
                continuingRuns, sideScroll, movedBlockNow, veilColor, pageSelected, focusRequestBlockId, formatRequest
            ) {
                {
                    // **La copertina e il titolo scorrono col resto.**
                    //
                    // Stavano fuori dalla lista, quindi restavano
                    // inchiodati in cima mentre il testo scorreva
                    // sotto: con una copertina alta si mangiavano
                    // mezza schermata che non si poteva recuperare.
                    // Messi qui dentro come prima voce se ne vanno
                    // scorrendo, come su Notion.
                    //
                    // Sono **una voce sola** di proposito: gli indici
                    // della lista servono altrove per portare in vista
                    // la riga che prende il cursore, e uno scarto fisso
                    // di uno e' molto piu' facile da tenere giusto di
                    // uno variabile.
                    item(key = PAGE_HEADER_KEY) {
                        KeepComposed()
                        // Nel titolo "Select all" prende solo il titolo.
                        PlainTextToolbar { PageHeaderSlot(pageHeader) }
                    }

                    items(renderItems, key = { item -> renderItemKey(item) }) { item ->
                        KeepComposed()
                        // I campi di testo della voce sanno in quale voce stanno:
                        // con "Full line view" serve a sapere se si vedono.
                        CompositionLocalProvider(
                            LocalSideItemKey provides renderItemKey(item),
                            LocalParagraphLook provides paragraphLook
                        ) {
                        // **Ogni voce si ridisegna solo se è cambiata** (dal
                        // 18/10/2026): riceve solo quello che mostra, e le
                        // richieste di fuoco e di formato solo se sono per lei.
                        // Prima riceveva tutta la pagina, e a ogni tasto si
                        // rifacevano tutte le voci già viste, database compresi.
                        when (item) {
                            is RenderItem.Run -> {
                                val content = RunContent(item.blocks, item.blocks.numbersFrom(ordinals))
                                // Ctrl+A: tutta la pagina, non solo il pezzo di testo (vedi `BlockSelection.kt`).
                                Column(Modifier.pageVeil(pageSelected, veilColor).onPreviewKeyEvent { pageKey(it) }) {
                                RunItem(
                                    content = content,
                                    runKey = item.key,
                                    joinsAbove = item.key in continuingRuns,
                                    joinsBelow = item.key in continuedRuns,
                                    env = itemEnv,
                                    focusRequestBlockId = focusRequestBlockId?.takeIf { id -> item.blocks.any { it.id == id } },
                                    formatRequest = formatRequest?.takeIf { r -> item.blocks.any { it.id == r.first } }
                                )
                                }
                            }
                            is RenderItem.Island -> {
                                val content = IslandContent(item.block, scopeOf(item.block, childrenOf))
                                // Una tabella arrivata al suo bordo non si trascina
                                // dietro il testo della pagina (vedi `KeepSidewaysInside`).
                                // Col pin ha il segnalibro, e arrivandoci si illumina (`PageAnchors.kt`).
                                // **Una colonna, non un `Box`** (dal 20/10/2026): un blocco può
                                // disegnare più pezzi uno sotto l'altro, come il database e la
                                // riga alla sua fine; nel `Box` messo il 19/10 finivano uno
                                // sopra l'altro, e la riga stava in cima al database.
                                // Lo spazio fra i blocchi ("Paragraph"): metà sopra e
                                // metà sotto, come fa l'altezza di riga nei campi di testo.
                                val islandSpacing = paragraphLook.spacingOf(item.block)
                                // Un database dice qual è la sua prima riga (il titolo), per i sei puntini (`BlockHandle.kt`).
                                val firstLine = remember(item.block.id) { if (item.block.type == BlockType.DATABASE_LINK) FirstLine() else null }
                                Column(
                                    (if (sideScroll != null) Modifier.nestedScroll(KeepSidewaysInside) else Modifier)
                                        .padding(vertical = (islandSpacing / 2f).sp.toDpIn(LocalDensity.current))
                                        .anchorDecor(item.block.id, item.block.attrs.linkTarget, movedBlockNow, PAGE_SIDE_PADDING)
                                        .pageVeil(pageSelected || LocalBlockSelection.current?.selected?.value?.contains(item.block.id) == true, veilColor)
                                        .pickableAs(
                                            renderItemKey(item),
                                            lineOf = firstLine?.let { line -> { _, block -> line.boundsIn(block) } },
                                            textInset = with(LocalDensity.current) { PAGE_SIDE_PADDING.toPx() },
                                            // Le colonne: i puntini li hanno le loro righe (vedi `BlockPicker.lineAt`).
                                            handle = item.block.type != BlockType.COLUMN_LIST
                                        ) { item.block.id }
                                        // Nelle celle di un database Ctrl+A resta della cella; in quelle di una
                                        // tabella Ctrl+Z resta della cella (l'Annulla della pagina non le conosce).
                                        .onPreviewKeyEvent { event ->
                                            when (item.block.type) {
                                                BlockType.DATABASE_LINK -> false
                                                BlockType.TABLE -> isSelectAll(event).also { all -> if (all) selectWholePage() }
                                                else -> pageKey(event)
                                            }
                                        }
                                ) {
                                    CompositionLocalProvider(LocalFirstLine provides firstLine) {
                                        IslandItem(
                                            content = content,
                                            env = itemEnv,
                                            focusRequestBlockId = focusRequestBlockId?.takeIf { id -> content.scope.any { it.id == id } },
                                            formatRequest = formatRequest?.takeIf { r -> content.scope.any { it.id == r.first } }
                                        )
                                    }
                                }
                            }
                        }
                        }
                    }

                    // **I backlink** (dal 03/11/2026): in fondo, dopo l'ultima
                    // riga, e solo se qualche pagina richiama questa.
                    if (backlinks.isNotEmpty() && !drawingMode) {
                        item(key = BACKLINKS_KEY) {
                            BacklinksSection(
                                backlinks = backlinks,
                                sidePadding = PAGE_SIDE_PADDING,
                                onOpen = { page -> if (page.isDatabase) onNavigateToDatabase(page.id) else onNavigateToPage(page.id) },
                                onTapAbove = { viewModel.focusLastBlock() }
                            )
                        }
                    }
                }
            }
            // Nel margine a sinistra: un clic apre il menu del blocco, trascinandoli si sposta (vedi `BlockDrag.kt`).
            val dragDensity = LocalDensity.current
            val blockDrag = remember(blockPicker, viewModel, dragDensity) {
                BlockDrag(
                    picker = blockPicker,
                    blocks = { viewModel.blocks.value },
                    side = { with(dragDensity) { DROP_SIDE_ZONE.toPx() } }
                ) { id, drop ->
                    blockSpot.value = null
                    val moving = viewModel.blocks.value.firstOrNull { it.id == id } ?: return@BlockDrag
                    when (drop.place) {
                        DropPlace.BEFORE, DropPlace.AFTER -> viewModel.moveBlockNextTo(moving, drop.targetId, after = drop.place == DropPlace.AFTER)
                        DropPlace.LEFT, DropPlace.RIGHT -> viewModel.moveBlockBeside(moving, drop.targetId, right = drop.place == DropPlace.RIGHT)
                    }
                }
            }
            // Sul PC, nello spazio fra due colonne, la striscia che le allarga (vedi `ColumnEdges.kt`).
            val columnEdges = remember(pageId) { ColumnEdges() }
            val pcEditing = isPc && !readOnlyPage && !drawingMode
            // Sul PC il tasto destro sulle righe apre il menu della pagina (vedi `PcContextMenu.kt`).
            CompositionLocalProvider(
                LocalPageTextMenu provides pcMenuHost.takeIf { pcEditing },
                LocalColumnEdges provides columnEdges.takeIf { pcEditing }
            ) {
                LazyColumn(
                    state = listState,
                    // Sul PC il margine in più a sinistra (vedi `PAGE_INSET`).
                    contentPadding = PaddingValues(start = pageLeftInset, end = pageRightInset, top = if (isPc) PC_TOP_BAR else 0.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        // Il verso del dito, per "Back" quando si è scesi molto (`BackToTop.kt`).
                        .nestedScroll(backToTop.connection)
                        // Con tutta la pagina selezionata, un tocco la lascia (vedi `PageSelection.kt`).
                        .tapToEndPageSelection(pageSelected) { pageSelected = false }
                        // Col mouse: più blocchi trascinando, un divisore con un clic (vedi `BlockSelection.kt`).
                        .onGloballyPositioned { pageListCoordinates.set(it) }
                        .mouseBlockSelection(
                            picker = blockPicker,
                            coordinates = { pageListCoordinates.get() },
                            order = { latestRenderItems.value.flatMap { renderItemBlockIds(it) } },
                            selectsOnClick = { id -> viewModel.blocks.value.firstOrNull { it.id == id }?.type == BlockType.DIVIDER },
                            selected = { selectedBlocks.value.isNotEmpty() },
                            scrollBy = { listState.scrollBy(it) },
                            onSelect = { ids ->
                                if (!ids.isNullOrEmpty() && selectedBlocks.value.isEmpty()) focusManager.clearFocus()
                                pageSelected = false
                                selectedBlocks.value = ids.orEmpty()
                            }
                        )
                        // Sul PC i file portati da Esplora risorse: la linea blu dei blocchi, e lì diventano blocchi (vedi `OutsideFileDrop.kt`).
                        .then(
                            if (!pcEditing) {
                                Modifier
                            } else {
                                Modifier.outsideFileDrop(
                                    store = imageStore,
                                    onMove = { at ->
                                        when {
                                            at == null -> if (blockDrag.dragged == OUTSIDE_FILE) blockDrag.end(false)
                                            blockDrag.dragged == null -> {
                                                blockDrag.start(OUTSIDE_FILE, at)
                                                blockDrag.moveTo(at)
                                            }
                                            blockDrag.dragged == OUTSIDE_FILE -> blockDrag.moveTo(at)
                                        }
                                    },
                                    onDrop = { at ->
                                        // Il posto si decide adesso: i file si copiano dopo, e intanto la pagina può cambiare.
                                        val side = with(dragDensity) { DROP_SIDE_ZONE.toPx() }
                                        val drop = blockPicker.blockAt(at)?.let { (target, box) -> dropFor(OUTSIDE_FILE, target, at, box, side, viewModel.blocks.value) }
                                            ?: blockDrag.drop.takeIf { blockDrag.dragged == OUTSIDE_FILE }
                                        if (blockDrag.dragged == OUTSIDE_FILE) blockDrag.end(false)
                                        val place: (List<DroppedFile>) -> Unit = { files ->
                                            viewModel.insertDroppedBlocks(
                                                files.map { NewBlock(type = it.type, attrs = it.attrs) },
                                                targetId = drop?.targetId,
                                                beside = drop?.place == DropPlace.LEFT || drop?.place == DropPlace.RIGHT,
                                                after = drop?.place == DropPlace.AFTER || drop?.place == DropPlace.RIGHT
                                            )
                                            // Il testo delle foto, per la ricerca.
                                            if (files.any { it.type == BlockType.IMAGE }) PhotoTextRequests.request(delayMs = 1_000)
                                        }
                                        place
                                    }
                                )
                            }
                        )
                        // Sul PC i sei puntini accanto al blocco sotto il mouse (vedi `BlockHandle.kt`).
                        .then(if (isPc) Modifier.blockHover(blockPicker, { pageListCoordinates.get() }, { blockSpot.value }, { lastPagePointer[0] = it }) { blockSpot.value = it } else Modifier)
                        .stickyViewport(stickyViewport)
                        // Di lato, con "Full line view": il primo verso in cui si
                        // muove il dito decide per tutto il gesto.
                        .sideScrollGestures(sideScroll, enabled = !drawingMode)
                        // Tocco sullo spazio vuoto sotto l'ultimo blocco per
                        // continuare a scrivere. Deve rispondere SOLO a tocchi
                        // veri: con Modifier.clickable rispondeva anche al tasto
                        // Invio (Compose lo tratta come attivazione quando un
                        // elemento interno ha il focus), quindi ogni a-capo
                        // faceva anche ripartire focusLastBlock, che riportava
                        // il cursore sull'ultimo blocco *noto prima* che l'a-capo
                        // creasse quello nuovo — il carattere successivo finiva
                        // sulla riga precedente.
                        .pointerInput(Unit) {
                            detectTapGestures { tap ->
                                // **Solo il vuoto sotto l'ultimo blocco.**
                                // Il gestore copre tutta la lista, quindi
                                // prendeva anche lo spazio accanto al
                                // titolo e i buchi fra una riga e l'altra:
                                // toccandoli, il cursore saltava in fondo
                                // alla pagina e si apriva la tastiera, che
                                // non è mai quello che si voleva fare.
                                val info = listState.layoutInfo
                                val last = info.visibleItemsInfo.lastOrNull()
                                if (last != null &&
                                    last.index == info.totalItemsCount - 1 &&
                                    tap.y > last.offset + last.size
                                ) {
                                    viewModel.focusLastBlock()
                                }
                            }
                        },
                    content = listContent
                )
            }
            // **La barretta di scorrimento** (dal 20/10/2026), sul bordo destro.
            PageScrollIndicator(listState, modifier = Modifier.align(Alignment.TopEnd))
            if (isPc && !readOnlyPage && !drawingMode) {
                // Le voci si spostano sotto il mouse fermo: i puntini con loro (vedi `FollowPageLayout`).
                FollowPageLayout(
                    picker = blockPicker,
                    page = { pageListCoordinates.get() },
                    pointer = { lastPagePointer[0] },
                    dragging = { blockDrag.dragged != null },
                    layout = { listState.layoutInfo.visibleItemsInfo.map { Triple(it.key, it.offset, it.size) } },
                    spot = blockSpot
                )
                BlockHandle(blockSpot, endsAt = pageLeftInset + PAGE_SIDE_PADDING, drag = blockDrag) { id -> blockMenuFor = id }
                ColumnEdgeLayer(columnEdges, { pageListCoordinates.get() }) { notches ->
                    // Uno scatto, un ventesimo della pagina, come la rotella sulla pagina.
                    listState.dispatchRawDelta(notches * (pageListCoordinates.get()?.size?.height ?: 0) / 20f)
                }
                pcMenu?.let { open ->
                    val local = pageListCoordinates.get()?.takeIf { it.isAttached }?.windowToLocal(open.at)
                    if (local != null) {
                        Box(modifier = Modifier.offset { IntOffset(local.x.roundToInt(), local.y.roundToInt()) }.size(0.dp)) {
                            PcPageMenu(
                                open = open,
                                focused = viewModel.blocks.value.find { it.id == focusedBlockId },
                                viewModel = viewModel,
                                canUndo = canUndo,
                                canRedo = canRedo,
                                selectionLink = selectionLink,
                                currentFont = selectionTypography.font,
                                currentSize = selectionTypography.size ?: DEFAULT_PAGE_FONT_SIZE,
                                onKind = { kind -> pcMenu = open.copy(kind = kind) },
                                onClose = { pcMenu = null },
                                onBlockMenu = { block ->
                                    focusManager.clearFocus()
                                    blockMenuFor = block.id
                                },
                                onSelectAll = selectWholePage,
                                onTypeEntry = applyTypeMenuEntry,
                                onParagraph = openParagraph,
                                onFontSize = {
                                    viewModel.captureSelection()
                                    if (viewModel.hasCapturedSelection()) {
                                        showFontSizeDialog = true
                                    } else {
                                        showMessage(context, EditorStrings.selectTextFirst)
                                    }
                                },
                                onFont = { font ->
                                    if (!viewModel.applyFontToCapturedSelection(font)) showMessage(context, EditorStrings.selectTextFirst)
                                },
                                onColors = {
                                    viewModel.captureSelection()
                                    showColorPicker = true
                                },
                                onLink = {
                                    viewModel.captureSelection()
                                    linkDialogFor = viewModel.capturedLink().orEmpty()
                                }
                            )
                        }
                    }
                }
                BlockDragLayer(blockDrag, { pageListCoordinates.get() }) { listState.scrollBy(it) }
            }
            // Tutta la pagina, o i blocchi scelti col mouse (null): Copy, Cut, Delete per tutti e due.
            val chosenBlocks = selectedBlocks.value.takeIf { it.isNotEmpty() }
            val anySelection = pageSelected || chosenBlocks != null
            fun endSelection() {
                pageSelected = false
                selectedBlocks.value = emptySet()
            }
            fun copySelection(thenDelete: Boolean) {
                jumpScope.launch {
                    copyPageToClipboard(context, viewModel.pageCopyLines(chosenBlocks))
                    if (thenDelete) viewModel.deletePageContent(chosenBlocks)
                    endSelection()
                }
            }
            fun deleteSelection() {
                if (chosenBlocks == null) {
                    confirmPageDelete = true
                } else {
                    viewModel.deletePageContent(chosenBlocks)
                    endSelection()
                }
            }
            // I tasti della selezione li prende un punto invisibile col fuoco: Canc e Backspace, Ctrl+C, Ctrl+X, Ctrl+A, Esc.
            Box(
                Modifier
                    .size(1.dp)
                    .focusRequester(selectionKeys)
                    .focusable()
                    .onKeyEvent { event ->
                        if (pageKey(event)) return@onKeyEvent true
                        if (!anySelection) return@onKeyEvent false
                        when (selectionKeyOf(event)) {
                            SelectionKey.DELETE -> if (!readOnlyPage) deleteSelection()
                            SelectionKey.COPY -> copySelection(thenDelete = false)
                            SelectionKey.CUT -> if (!readOnlyPage) copySelection(thenDelete = true)
                            SelectionKey.ALL -> selectWholePage()
                            SelectionKey.CLOSE -> endSelection()
                            null -> return@onKeyEvent false
                        }
                        true
                    }
            )
            LaunchedEffect(anySelection) { if (anySelection) runCatching { selectionKeys.requestFocus() } }
            if (anySelection) {
                PageSelectionBar(
                    editable = !readOnlyPage,
                    onCopy = { copySelection(thenDelete = false) },
                    onCut = { copySelection(thenDelete = true) },
                    onDelete = { deleteSelection() },
                    onClose = { endSelection() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                )
            }
            BackToTopButton(
                state = backToTop,
                list = listState,
                allowed = !pageSelected && linkReturn == null && !drawingMode,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
            )
            linkReturn?.takeIf { !pageSelected }?.let { back ->
                LinkBackButton(
                    back = back,
                    onBack = {
                        linkReturn = null
                        jumpScope.launch { listState.scrollToItem(back.index, back.offset) }
                    },
                    onTimeout = { linkReturn = null },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp)
                )
            }
            if (sideScroll != null) {
                SideScrollEffects(sideScroll, drawing = drawingMode)
                SideScrollIndicator(
                    sideScroll,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 20.dp, end = 20.dp, bottom = 14.dp)
                )
            }
            }

            if (showTypeMenu) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
                            detectTapGestures { showTypeMenu = false }
                        }
                )
            }
            }

            if (drawingMode) {
                InkToolbar(
                    canUndo = inkCanUndo,
                    canRedo = inkCanRedo,
                    onUndo = { inkViewModel.undo() },
                    onRedo = { inkViewModel.redo() },
                    onDone = { drawing = false }
                )
            }

            // La barra sopra la tastiera.
            //
            // Ha un grigio **tutto suo**, più chiaro della pagina e
            // diverso dalla tastiera: prima era dello stesso nero della
            // pagina e non si capiva dove finisse una e cominciasse
            // l'altra. Ha due stati — la barra normale
            // (indenta/sposta/+/Aa) e, premendo Aa, quella di
            // formattazione (B/I/U/S) — che si sostituiscono a vicenda
            // nella stessa riga invece di impilarsi.
            if (showBar) {
                val focusedBlock = blocks.find { it.id == focusedBlockId }
                // Il codice ha i suoi colori, quelli del linguaggio: niente
                // Aa né pennello (dal 06/10/2026).
                val aaAvailable = focusedBlock != null && focusedBlock.type != BlockType.CODE
                // **La barra se ne va insieme alla tastiera.**
                //
                // Il suo bordo inferiore sta appoggiato sopra la
                // tastiera, quindi mentre la tastiera scende la barra la
                // segue — ma si ferma al bordo dello schermo e resta lì
                // finché il sistema non dichiara la tastiera chiusa:
                // da fuori sembra che sparisca qualche istante dopo.
                //
                // Negli ultimi punti della discesa la si spinge giù di
                // quanto le manca per uscire, così arriva al bordo nello
                // stesso momento della tastiera. Quando poi il sistema
                // la smonta, è già invisibile e non si vede nessuno
                // scatto.
                //
                // La tastiera si legge **mentre si piazza la barra**, non
                // mentre la si costruisce (dal 27/10/2026): letta qui, a ogni
                // fotogramma della sua corsa si rifaceva tutta la pagina.
                val ime = WindowInsets.ime
                var barHeightPx by remember { mutableStateOf(0) }
                // Col menu "+" aperto la barra si vede anche senza
                // tastiera: lì non deve scivolare da nessuna parte.
                val slides = !showTypeMenu
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { barHeightPx = it.height }
                        .offset {
                            IntOffset(0, if (slides) (barHeightPx - ime.getBottom(this)).coerceAtLeast(0) else 0)
                        }
                        .background(DarkSurfaceVariant)
                        .horizontalScroll(rememberScrollState())
                        // Nessun margine sopra e sotto: l'altezza la
                        // danno i pulsanti, e ogni punto in più qui è
                        // una fascia in più che copre il testo.
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showFormatBar && aaAvailable) {
                        BarButton(onClick = { showFormatBar = false }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = Strings.back, tint = NotionWhite, modifier = Modifier.size(BAR_ICON_SIZE))
                        }
                        // Font e corpo del testo, prima di B come su
                        // OneNote. Dal 25/09/2026 valgono **per il testo
                        // selezionato**, come il colore (prima per tutta la
                        // pagina); le caselle dicono quelli del punto in
                        // cui si è. La selezione si mette da parte appena
                        // le si tocca, come fa il pennello: la finestrella
                        // del corpo porta via il fuoco. Con la pagina
                        // bloccata non si toccano, come il resto del
                        // contenuto.
                        if (!readOnlyPage) {
                            FontPickerChip(
                                current = selectionTypography.font,
                                onOpen = { viewModel.captureSelection() },
                                onPick = { font ->
                                    if (!viewModel.applyFontToCapturedSelection(font)) {
                                        showMessage(context, EditorStrings.selectTextFirst)
                                    }
                                }
                            )
                            FontSizeChip(
                                current = selectionTypography.size ?: DEFAULT_PAGE_FONT_SIZE,
                                onClick = {
                                    viewModel.captureSelection()
                                    if (viewModel.hasCapturedSelection()) {
                                        showFontSizeDialog = true
                                    } else {
                                        showMessage(context, EditorStrings.selectTextFirst)
                                    }
                                }
                            )
                        }
                        BarButton(onClick = {
                            focusedBlock?.let { viewModel.requestFormat(it.id, FormatType.BOLD) }
                        }) {
                            Text("B", color = NotionWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        BarButton(onClick = {
                            focusedBlock?.let { viewModel.requestFormat(it.id, FormatType.ITALIC) }
                        }) {
                            Text("I", color = NotionWhite, fontStyle = FontStyle.Italic, fontSize = 18.sp)
                        }
                        BarButton(onClick = {
                            focusedBlock?.let { viewModel.requestFormat(it.id, FormatType.UNDERLINE) }
                        }) {
                            Text("U", color = NotionWhite, textDecoration = TextDecoration.Underline, fontSize = 18.sp)
                        }
                        BarButton(onClick = {
                            focusedBlock?.let { viewModel.requestFormat(it.id, FormatType.STRIKETHROUGH) }
                        }) {
                            Text("S", color = NotionWhite, textDecoration = TextDecoration.LineThrough, fontSize = 18.sp)
                        }
                        // Pedice e apice, dopo S come in Word: sullo
                        // stesso testo, toccare l'altro li scambia.
                        BarButton(onClick = {
                            focusedBlock?.let { viewModel.requestFormat(it.id, FormatType.SUBSCRIPT) }
                        }) {
                            ScriptGlyph(superscript = false, description = EditorStrings.subscript)
                        }
                        BarButton(onClick = {
                            focusedBlock?.let { viewModel.requestFormat(it.id, FormatType.SUPERSCRIPT) }
                        }) {
                            ScriptGlyph(superscript = true, description = EditorStrings.superscript)
                        }
                        // Spoiler: copre il testo selezionato. L'occhio
                        // sbarrato è il segno che usano sia Discord sia
                        // Telegram per la stessa cosa.
                        BarButton(onClick = {
                            focusedBlock?.let { viewModel.requestFormat(it.id, FormatType.SPOILER) }
                        }) {
                            Icon(
                                Icons.Filled.VisibilityOff,
                                contentDescription = EditorStrings.spoiler,
                                tint = NotionWhite,
                                modifier = Modifier.size(BAR_ICON_SIZE)
                            )
                        }
                    } else {
                        // **Il menu del blocco**, coi sei puntini di
                        // Notion: per la riga in cui sta il cursore. Il
                        // cursore si spegne prima di aprirlo — la
                        // tastiera se ne andrebbe comunque sotto la
                        // finestra, e una riga che "Turn into" fa
                        // sparire non deve morire col fuoco dentro, che è
                        // il modo in cui l'app si chiudeva.
                        BarButton(onClick = {
                            focusedBlock?.let { block ->
                                focusManager.clearFocus()
                                blockMenuFor = block.id
                            }
                        }) {
                            Icon(Icons.Filled.DragIndicator, contentDescription = EditorStrings.blockOptions, tint = NotionWhite, modifier = Modifier.size(BAR_ICON_SIZE))
                        }
                        // **Copia, incolla, taglia, seleziona tutto** (dal
                        // 18/10/2026, chiesti dall'utente): vedi `ClipboardBarButtons`.
                        ClipboardBarButtons(onSelectAll = selectWholePage)
                        BarButton(onClick = { focusedBlock?.let { viewModel.outdentBlock(it) } }) {
                            Icon(Icons.Filled.FormatIndentDecrease, contentDescription = EditorStrings.outdent, tint = NotionWhite, modifier = Modifier.size(BAR_ICON_SIZE))
                        }
                        BarButton(onClick = { focusedBlock?.let { viewModel.indentBlock(it) } }) {
                            Icon(Icons.Filled.FormatIndentIncrease, contentDescription = EditorStrings.indent, tint = NotionWhite, modifier = Modifier.size(BAR_ICON_SIZE))
                        }
                        BarButton(onClick = { focusedBlock?.let { viewModel.moveBlockUp(it) } }) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = EditorStrings.moveUp, tint = NotionWhite, modifier = Modifier.size(BAR_ICON_SIZE))
                        }
                        BarButton(onClick = { focusedBlock?.let { viewModel.moveBlockDown(it) } }) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = EditorStrings.moveDown, tint = NotionWhite, modifier = Modifier.size(BAR_ICON_SIZE))
                        }
                        Box {
                            // Il "+" fa da interruttore: aperto il menu,
                            // ritoccarlo lo richiude. È il gesto che
                            // viene da sé, e adesso funziona perché il
                            // menu non si chiude più da solo al tocco
                            // fuori — altrimenti questo tocco lo avrebbe
                            // chiuso e riaperto nello stesso istante.
                            BarButton(onClick = {
                                // Aprendo il menu la selezione si mette da
                                // parte, come per il pennello: è lei a dire
                                // quali righe cambiare (vedi sotto).
                                if (!showTypeMenu) viewModel.captureSelection()
                                showTypeMenu = !showTypeMenu
                            }) {
                                Icon(Icons.Filled.Add, contentDescription = EditorStrings.addOrChangeBlock, tint = NotionWhite, modifier = Modifier.size(BAR_ICON_SIZE))
                            }
                            BlockTypeMenu(
                                expanded = showTypeMenu,
                                onDismiss = { showTypeMenu = false },
                                onSelect = { entry ->
                                    showTypeMenu = false
                                    val block = focusedBlock ?: return@BlockTypeMenu
                                    applyTypeMenuEntry(block, entry)
                                }
                            )
                        }
                        // **"Paragraph"** (dal 21/10/2026, chiesto dall'utente):
                        // lo spazio fra i blocchi e dopo i marcatori solo per le
                        // righe che il testo selezionato tocca, o per quella del
                        // cursore. La selezione si mette da parte prima, come per
                        // il pennello; poi il cursore si spegne, perché cambiando
                        // lo spazio quelle righe diventano un campo a sé, e un
                        // campo che sparisce col cursore dentro fa chiudere l'app.
                        BarButton(onClick = { openParagraph(focusedBlock) }) {
                            Icon(
                                Icons.Filled.FormatLineSpacing,
                                contentDescription = ParagraphStrings.paragraph,
                                tint = NotionWhite,
                                modifier = Modifier.size(BAR_ICON_SIZE)
                            )
                        }
                        if (aaAvailable) {
                            BarButton(onClick = { showFormatBar = true }) {
                                Text("Aa", color = NotionWhite, fontWeight = FontWeight.Bold)
                            }
                            // Il pennello sta **accanto ad Aa e non
                            // dentro**: il colore non è un interruttore
                            // come grassetto e corsivo, apre una finestra
                            // sua, e metterlo in fila con quelli
                            // avrebbe promesso un comportamento che non
                            // ha.
                            BarButton(onClick = {
                                // La selezione si mette da parte
                                // **adesso**: aprendo la finestra il
                                // campo perde il fuoco e la selezione
                                // si chiude in un cursore, e fra un
                                // attimo non ci sarebbe più niente da
                                // colorare.
                                viewModel.captureSelection()
                                showColorPicker = true
                            }) {
                                Icon(
                                    Icons.Filled.Brush,
                                    contentDescription = EditorStrings.textAndBackgroundColor,
                                    tint = NotionWhite,
                                    modifier = Modifier.size(BAR_ICON_SIZE)
                                )
                            }
                            // **Il collegamento** (dal 17/10/2026, chiesto
                            // dall'utente), accanto al pennello: compare
                            // quando c'è del testo selezionato, o quando il
                            // cursore sta in un link (per cambiarlo o
                            // toglierlo). La selezione si mette da parte
                            // come per il pennello: la finestrella porta via
                            // il fuoco. Dal 27/10/2026 anche col cursore in un
                            // sito scritto come testo ("https://…", "www.…").
                            if (selectionLink.hasText || selectionLink.link != null || selectionLink.onSite) {
                                BarButton(onClick = {
                                    viewModel.captureSelection()
                                    linkDialogFor = viewModel.capturedLink().orEmpty()
                                }) {
                                    Icon(
                                        Icons.Filled.Link,
                                        contentDescription = EditorStrings.link,
                                        tint = NotionWhite,
                                        modifier = Modifier.size(BAR_ICON_SIZE)
                                    )
                                }
                            }
                        }
                        BarButton(onClick = { viewModel.undo() }, enabled = canUndo) {
                            Icon(
                                Icons.Filled.Undo,
                                contentDescription = EditorStrings.undo,
                                tint = if (canUndo) NotionWhite else NotionWhite.copy(alpha = 0.3f),
                                modifier = Modifier.size(BAR_ICON_SIZE)
                            )
                        }
                        BarButton(onClick = { viewModel.redo() }, enabled = canRedo) {
                            Icon(
                                Icons.Filled.Redo,
                                contentDescription = EditorStrings.redo,
                                tint = if (canRedo) NotionWhite else NotionWhite.copy(alpha = 0.3f),
                                modifier = Modifier.size(BAR_ICON_SIZE)
                            )
                        }
                        // Al posto di intercettare il tasto backspace sul
                        // campo di testo (che su alcune tastiere, Samsung
                        // inclusa, può disturbare la connessione con
                        // l'input, e comunque non sembra arrivare affatto
                        // per questo caso specifico), questo pulsante
                        // compare solo quando il blocco a fuoco è già
                        // vuoto — stessa azione, interazione a tocco.
                        // Per i blocchi di testo scorrevole ora uniti,
                        // però, backspace-tra-blocchi funziona già in modo
                        // nativo (vedi MergedTextRunField), quindi questo
                        // pulsante resta utile principalmente per le isole
                        // (checkbox/toggle).
                        if (focusedBlock != null && viewModel.plainTextOf(focusedBlock).isBlank()) {
                            BarButton(onClick = {
                                // Il campo di questa riga sta per morire col
                                // cursore dentro: il fuoco si posa prima sul
                                // campo invisibile, come in tutti gli altri
                                // punti in cui una riga sparisce sotto le dita.
                                focusPark.requestFocus()
                                viewModel.deleteBlockAndFocusPrevious(focusedBlock)
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = EditorStrings.deleteEmptyBlock, tint = NotionWhite, modifier = Modifier.size(BAR_ICON_SIZE))
                            }
                        }
                    }
                }
            }
        }

        // I pulsanti sospesi: stessa altezza, subito sotto la barra del
        // telefono. Ognuno è grande solo quanto il suo cerchio, così
        // intorno la pagina si tocca normalmente. Su una pagina nel
        // cestino non ci sono: al loro posto c'è la barra del cestino, che
        // ha il suo indietro.
        if (!isTrashed) {
            if (isPc) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(top = padding.calculateTopPadding())
                        .fillMaxWidth()
                        .height(PC_TOP_BAR)
                        .background(DarkBackground)
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = padding.calculateTopPadding() + FLOATING_BUTTON_MARGIN, start = FLOATING_BUTTON_MARGIN),
                horizontalArrangement = Arrangement.spacedBy(FLOATING_BUTTON_GAP),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dentro un peek (sul PC, dal 12/11/2026) i suoi comandi al posto di indietro, ☰ e percorso.
                val peekControls = LocalPeekControls.current
                if (isPc && peekControls != null) {
                    peekControls()
                } else {
                if (!isRootPage) {
                    FloatingPageButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = Strings.back,
                            tint = FloatingTint,
                            modifier = Modifier.size(FLOATING_ICON_SIZE)
                        )
                    }
                }
                // La barra laterale: stesso cerchio dei tre puntini, con le
                // tre lineette. Il cursore si spegne prima di aprirla,
                // altrimenti la tastiera resterebbe su sopra la barra.
                if (!LocalSidebarPinned.current) {
                    FloatingPageButton(onClick = {
                        focusManager.clearFocus()
                        onOpenSidebar()
                    }) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = Strings.openSidebar,
                            tint = FloatingTint,
                            modifier = Modifier.size(FLOATING_ICON_SIZE)
                        )
                    }
                }
                // Sul PC il percorso della pagina, come in Notion (`PageBreadcrumb`).
                if (isPc) {
                    PageBreadcrumb(
                        ancestors = breadcrumb,
                        current = page,
                        onOpen = { crumb -> if (crumb.isDatabase) onNavigateToDatabase(crumb.id) else onNavigateToPage(crumb.id) }
                    )
                }
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = padding.calculateTopPadding() + FLOATING_BUTTON_MARGIN, end = FLOATING_BUTTON_MARGIN),
                horizontalArrangement = Arrangement.spacedBy(FLOATING_BUTTON_GAP)
            ) {
                // **La pagina prima e quella dopo** nel database (dal
                // 16/10/2026, chiesto dall'utente), nell'ordine della vista da
                // cui la si è aperta: vedi `RowNavigation`.
                if (page?.isRowPage == true && !inNotebook && onOpenSiblingRow != null) {
                    RowNavigationButtons(
                        pageId = pageId,
                        factory = factory,
                        buttonSize = FLOATING_BUTTON_SIZE,
                        iconSize = FLOATING_ICON_SIZE,
                        background = if (isPc) Color.Transparent else FloatingButtonBackground,
                        tint = FloatingTint,
                        onOpenRow = { rowId ->
                            focusManager.clearFocus()
                            onOpenSiblingRow(rowId)
                        }
                    )
                }
                // **Il lucchetto chiuso** (dal 26/09/2026, chiesto
                // dall'utente): stesso cerchio dei tre puntini, alla loro
                // sinistra, sulle pagine bloccate con la password. Dice che
                // la pagina è protetta; toccato, lo dice anche a parole.
                if (page?.isProtected == true) {
                    FloatingPageButton(onClick = {
                        showMessage(context, LockStrings.protectedByPassword)
                    }) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = LockStrings.protectedByPassword,
                            tint = FloatingTint,
                            modifier = Modifier.size(FLOATING_ICON_SIZE)
                        )
                    }
                }
                FloatingPageButton(onClick = { showPageOptions = true }) {
                    Icon(
                        Icons.Filled.MoreHoriz,
                        contentDescription = Strings.pageOptions,
                        tint = FloatingTint,
                        modifier = Modifier.size(FLOATING_ICON_SIZE)
                    )
                }
            }
        }
        }
    }

    if (confirmNotebookDelete) {
        DeleteForeverDialog(
            title = NotebookStrings.deletePageTitle,
            text = NotebookStrings.deletePageText,
            onDismiss = { confirmNotebookDelete = false },
            onConfirm = {
                confirmNotebookDelete = false
                viewModel.deletePermanently(imageStore) { onBack() }
            }
        )
    }

    if (confirmDeleteForever) {
        DeleteForeverDialog(
            onDismiss = { confirmDeleteForever = false },
            onConfirm = {
                confirmDeleteForever = false
                viewModel.deletePermanently(imageStore) { onBack() }
            }
        )
    }

    val target = editingImage
    if (target == PageImageTarget.ICON) {
        // **Il selettore delle icone** (dal 07/10/2026): emoji, immagine o niente.
        IconPickerSheet(
            store = imageStore,
            hasIcon = page?.hasChosenIcon == true,
            onPick = { choice -> viewModel.setIcon(choice) },
            onDismiss = { editingImage = null }
        )
    } else if (target != null) {
        PageImageSheet(
            target = target,
            hasImage = page?.coverImage != null,
            store = imageStore,
            onPicked = { fileName -> viewModel.setCoverImage(fileName) },
            onReposition = if (target == PageImageTarget.COVER) {
                {
                    // Si riparte da dove si era rimasti, non da capo:
                    // ritoccare un'inquadratura già scelta è la cosa
                    // che si fa più spesso.
                    liveCoverScale = page?.coverScale ?: 1f
                    liveCoverOffset = Offset(
                        page?.coverOffsetX ?: 0f,
                        page?.coverOffsetY ?: 0f
                    )
                    movingCover = true
                }
            } else {
                null
            },
            onDismiss = { editingImage = null }
        )
    }

    if (confirmPageDelete) {
        FitAlertDialog(
            onDismissRequest = { confirmPageDelete = false },
            title = { Text(EditorStrings.deletePageContentTitle) },
            text = { Text(EditorStrings.deletePageContentText) },
            confirmButton = {
                TextButton(onClick = {
                    confirmPageDelete = false
                    pageSelected = false
                    viewModel.deletePageContent()
                }) { Text(Strings.delete, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmPageDelete = false }) { Text(Strings.cancel) } }
        )
    }

    linkDialogFor?.let { current ->
        // Le righe della pagina a cui collegarsi, lette all'apertura.
        val targets = remember { viewModel.linkTargets() }
        // Il testo del link com'è scritto adesso: "Show" lo cambia.
        val shown = remember { viewModel.capturedLinkText() }
        LinkDialog(
            current = current.ifEmpty { null },
            shown = shown,
            targets = targets,
            onDismiss = { linkDialogFor = null },
            onSave = { url, text ->
                linkDialogFor = null
                if (!viewModel.applyLinkToCapturedSelection(url, text)) {
                    showMessage(context, EditorStrings.selectTextFirst)
                }
            },
            onRemove = {
                linkDialogFor = null
                viewModel.applyLinkToCapturedSelection(null)
            }
        )
    }

    if (showFontSizeDialog) {
        FontSizeDialog(
            current = selectionTypography.size ?: DEFAULT_PAGE_FONT_SIZE,
            onDismiss = { showFontSizeDialog = false },
            onConfirm = { size ->
                showFontSizeDialog = false
                viewModel.applySizeToCapturedSelection(size)
            }
        )
    }

    if (showColorPicker) {
        ColorPickerSheet(
            initialHex = lastPickedHex,
            background = colorOnBackground,
            onDismiss = { showColorPicker = false },
            onSwitchTarget = { colorOnBackground = it },
            onPick = { hex ->
                showColorPicker = false
                lastPickedHex = hex ?: lastPickedHex
                viewModel.applyCapturedColor(colorOnBackground, hex)
            }
        )
    }

    // --- Il menu del blocco (sei puntini) ---
    //
    // "Turn into" fa quello che fa la stessa voce dei menu "/" e "+", con
    // due differenze che vengono dal punto da cui si parte: la riga ha già
    // del testo, e non lo si deve perdere — diventa il nome della pagina o
    // del database, la prima cella della tabella, la riga sotto il
    // divisore — e si resta qui invece di entrare nella pagina nuova, come
    // su Notion.
    val applyTurnInto: (BlockEntity, SlashEntry) -> Unit = { block, entry ->
        val text = viewModel.plainTextOf(block)
        when (val action = entry.action) {
            is SlashAction.Type -> if (action.type == BlockType.TABLE) {
                viewModel.convertToTableKeepingText(block)
            } else if (action.type == BlockType.NOTIFICATION) {
                viewModel.makeNotification(block, null)
                NotificationPermission.request()
            } else {
                viewModel.updateBlockType(block, action.type)
            }
            SlashAction.PageLink -> viewModel.convertToPageLink(block, title = text) {}
            SlashAction.Divider -> viewModel.convertToDividerAndFocusNext(block, keepText = true)
            is SlashAction.Database ->
                viewModel.convertToDatabaseLink(block, simple = action.simple, title = text) { databaseId ->
                    action.layout?.let { viewModel.setDatabaseLayout(databaseId, it) }
                }
            SlashAction.LinkedView -> askLinkedViewSource(block, true)
            // Il testo della riga diventa il nome del database, e si resta qui.
            SlashAction.FullPageDatabase -> viewModel.convertToFullPageDatabase(block, title = text) {}
            // Il testo della riga diventa il nome del bottone.
            SlashAction.Button -> viewModel.convertToButton(block, name = text)
            // Il testo della riga diventa la prima riga del blocco.
            SlashAction.Synced -> viewModel.makeSyncedBlock(block)
            // Non sono in "Turn into" (vedi `TurnIntoList`): il testo andrebbe perso.
            is SlashAction.Media, SlashAction.LinkToPage, SlashAction.LinkButton, SlashAction.Record, SlashAction.Scan, is SlashAction.Columns,
            SlashAction.NotYet -> Unit
        }
    }

    linkToPageFor?.let { block ->
        // Lo stesso albero con la ricerca di "Move to" e di "Page link".
        val pickScope = rememberCoroutineScope()
        DestinationPickerSheet(
            factory = factory,
            title = EditorStrings.blockType("Link to page"),
            hint = BlockStrings.linkToPageHint,
            viewModelKey = "link-to-page",
            onDismiss = { linkToPageFor = null },
            onPick = { node ->
                linkToPageFor = null
                pickScope.launch {
                    // Una riga mai aperta si sceglie lo stesso: la sua pagina nasce adesso.
                    val pageId = node.pageId ?: node.rowId?.let { factory.pageRepository.ensureRowPage(it) }
                    if (pageId != null) viewModel.convertToPageMention(block, pageId)
                }
            }
        )
    }

    // La finestra delle azioni di un bottone (dal 27/10/2026): vedi `ButtonEditHost`.
    ButtonEditHost(viewModel)

    linkButtonFor?.let { block ->
        LinkToButtonSheet(
            loadChoices = viewModel::buttonChoices,
            onDismiss = { linkButtonFor = null },
            onPick = { choice ->
                linkButtonFor = null
                viewModel.convertToLinkedButton(block, choice.blockId)
            }
        )
    }

    linkedViewRequest?.let { request ->
        LinkedSourcePickerSheet(
            databases = linkableDatabases,
            onDismiss = { linkedViewRequest = null },
            onPick = { source ->
                linkedViewRequest = null
                viewModel.convertToLinkedView(request.block, source.id, request.keepText)
            }
        )
    }

    blockMenuFor?.let { id ->
        BlockActionsHost(
            blockId = id,
            blocks = blocks,
            viewModel = viewModel,
            factory = factory,
            imageStore = imageStore,
            onTurnInto = applyTurnInto,
            onNavigateToDatabase = onNavigateToDatabase,
            onNavigateToPage = onNavigateToPage,
            onParagraph = { block -> paragraphLines = listOf(block) },
            onDismiss = { blockMenuFor = null }
        )
    }

    // --- Il menu dei tre puntini ---
    //
    // Il conteggio del testo per la voce "X words": si rifà ogni volta
    // che il menu si apre, e fino ad allora la voce non c'è — meglio che
    // mostrare per un istante il numero della volta prima.
    var textStats by remember { mutableStateOf<TextStats?>(null) }
    LaunchedEffect(showPageOptions) {
        textStats = null
        if (showPageOptions) viewModel.loadTextStats { textStats = it }
    }

    // Sta qui fuori dallo Scaffold e non dentro la barra in alto: una
    // finestra che sale dal basso non è figlia del pulsante che
    // l'apre, e messa lì dentro erediterebbe i margini della barra.
    if (choosingTemplate) {
        TemplateChooserSheet(
            factory = factory,
            onDismiss = { choosingTemplate = false },
            onPick = { template ->
                choosingTemplate = false
                viewModel.applyTemplate(template.id, imageStore)
            },
            onNewTemplate = {
                choosingTemplate = false
                viewModel.createTemplate { id -> onNavigateToPage(id) }
            }
        )
    }

    page?.let { current ->
        if (showPageOptions) {
            PageOptionsSheet(
                page = current,
                inNotebook = inNotebook,
                textStats = textStats,
                onAddCover = if (current.coverImage == null && !inNotebook && !isTrashed) {
                    {
                        showPageOptions = false
                        editingImage = PageImageTarget.COVER
                    }
                } else {
                    null
                },
                onDismiss = { showPageOptions = false },
                onToggleFavorite = { viewModel.toggleFavorite() },
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
                    viewModel.loadMoveExclusions { moveExclusions = it }
                },
                // Non per le pagine dei quaderni né per i template, come "Move to".
                onMoveToDatabase = if (inNotebook || current.isTemplate) null else {
                    {
                        showPageOptions = false
                        movingIntoDatabase = true
                        viewModel.loadMoveExclusions { moveExclusions = it }
                    }
                },
                onMoveToTrash = {
                    showPageOptions = false
                    // Il cestino è del menu principale: una pagina di un
                    // quaderno se ne va per sempre, dopo la domanda.
                    if (inNotebook) confirmNotebookDelete = true else confirmTrash = true
                },
                onToggleViewLock = { viewModel.toggleViewLocked() },
                onToggleLock = { viewModel.toggleLocked() },
                onUpdates = {
                    showPageOptions = false
                    onOpenUpdates(current.id)
                },
                onHistory = if (isTrashed) null else {
                    {
                        showPageOptions = false
                        focusManager.clearFocus()
                        showHistory = true
                    }
                },
                onExport = if (isTrashed) null else {
                    {
                        showPageOptions = false
                        focusManager.clearFocus()
                        showExport = true
                    }
                },
                onDraw = if (isTrashed) null else {
                    {
                        showPageOptions = false
                        // La tastiera se ne va: da qui si disegna.
                        focusManager.clearFocus()
                        drawing = true
                    }
                },
                onTogglePasswordLock = if (isRootPage) null else {
                    {
                        showPageOptions = false
                        focusManager.clearFocus()
                        passwordLockRequest = !current.isProtected
                    }
                },
                proofingValue = proofingSummary(current),
                onProofing = {
                    showPageOptions = false
                    showProofing = true
                },
                paragraphValue = pageParagraphSummary(viewModel.paragraphLook(current)),
                onParagraph = {
                    showPageOptions = false
                    // I gruppi di righe possono diventare campi diversi: nessuno
                    // deve sparire col cursore dentro.
                    focusManager.clearFocus()
                    showPageParagraph = true
                },
                fullLineValue = fullLineSummary(current),
                onFullLine = {
                    showPageOptions = false
                    showFullLine = true
                },
                // Solo sul PC (dal 12/11/2026): il menu resta aperto, la pagina cambia dietro.
                fullWidth = current.paragraph.fullWidth != false,
                onToggleFullWidth = if (isPc) {
                    { viewModel.setPageParagraph(current.paragraph.copy(fullWidth = if (current.paragraph.fullWidth == false) null else false)) }
                } else {
                    null
                },
                onWebClipper = if (isTrashed) null else {
                    {
                        showPageOptions = false
                        focusManager.clearFocus()
                        webClipperOpen = true
                    }
                }
            )
        }
        WebClipperFlow(open = webClipperOpen, onClose = { webClipperOpen = false }, viewModel = viewModel, imageStore = imageStore)
        if (showProofing) {
            PageProofingSheet(
                page = current,
                onSetProofing = viewModel::setProofing,
                onStopIgnoring = viewModel::stopIgnoring,
                onDismiss = { showProofing = false }
            )
        }
        if (showFullLine) {
            PageFullLineSheet(
                page = current,
                onSet = viewModel::setFullLineMode,
                onDismiss = { showFullLine = false }
            )
        }
        if (showPageParagraph) {
            PageParagraphSheet(
                page = current,
                onSet = viewModel::setPageParagraph,
                onDismiss = { showPageParagraph = false }
            )
        }
        paragraphLines?.let { lines ->
            LinesParagraphSheet(
                lines = lines,
                look = viewModel.paragraphLook(current),
                onApply = { spacing, gapFor -> viewModel.setLinesParagraph(lines, spacing, gapFor) },
                onDismiss = { paragraphLines = null }
            )
        }
        passwordLockRequest?.let { wanted ->
            PasswordLockFlow(
                wanted = wanted,
                onFinished = { passwordLockRequest = null },
                apply = { viewModel.setPasswordLocked(it) }
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
                    // Come "Move to": si torna indietro, e l'avviso dice dove.
                    viewModel.movePageToDatabase(destination, rowTitle) {
                        showMessage(context, Strings.movedInto(destination.placeName()))
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
                onPick = { destination, _ ->
                    moveExclusions = null
                    // Spostata, questa pagina non sta più dov'era: si
                    // torna indietro, altrimenti si resterebbe su una
                    // schermata che "indietro" riporterebbe in un
                    // posto in cui la pagina non c'è più. L'avviso dice
                    // dov'è finita.
                    viewModel.movePageTo(destination) {
                        showMessage(context, Strings.movedInto(destination.placeName()))
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
                    // Alla copia ci si va subito: è la conferma che è
                    // stata fatta. Se è finita in un altro posto lo dice
                    // anche un avviso, perché da dentro la copia non si
                    // vede dove sta.
                    viewModel.duplicatePage(target, Strings.copySuffix, imageStore) { copyId ->
                        placeName?.let {
                            showMessage(context, Strings.duplicatedInto(it))
                        }
                        if (current.isDatabase) onNavigateToDatabase(copyId) else onNavigateToPage(copyId)
                    }
                }
            )
        }

        if (showHistory) {
            val versions by viewModel.versions.collectAsStateWithLifecycle()
            PageHistorySheet(
                versions = versions,
                loadPreview = viewModel::versionPreview,
                canRestore = !readOnlyPage,
                onRestore = { version, done ->
                    viewModel.restoreVersion(version.id) { restored ->
                        done(restored)
                        if (restored) {
                            // I disegni si leggono una volta sola: vanno riletti.
                            inkViewModel.reload()
                            showHistory = false
                            showMessage(context, HistoryStrings.restored)
                        }
                    }
                },
                onDismiss = { showHistory = false }
            )
        }
        if (showExport) {
            PageExportSheet(
                title = current.title,
                hasSubpages = blocks.any { it.type == BlockType.PAGE_LINK && it.linkedPageId != null },
                hasDrawings = inkStrokes.isNotEmpty(),
                export = { options -> viewModel.export(context, factory, options) },
                onDismiss = { showExport = false }
            )
        }
        if (confirmTrash) {
            MoveToTrashDialog(
                pageTitle = current.title,
                onConfirm = {
                    confirmTrash = false
                    viewModel.moveToTrash { onBack() }
                },
                onDismiss = { confirmTrash = false }
            )
        }
    }
}

/** La riga che diventerà una vista collegata, in attesa che si scelga il database. */
private data class LinkedViewRequest(val block: BlockEntity, val keepText: Boolean)

/**
 * Quello che si vede sopra la copertina mentre la si sposta: come si
 * fa, e i due modi di uscirne.
 *
 * Il suggerimento c'è perché il trascinamento e la pizzicata non si
 * vedono: senza una riga che le nomini, una striscia che
 * improvvisamente non risponde più al tocco sembra rotta.
 */
@Composable
private fun CoverMoveOverlay(onCancel: () -> Unit, onSave: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = EditorStrings.dragToMove,
            style = MaterialTheme.typography.labelMedium,
            color = NotionWhite,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(DarkSheet.copy(alpha = 0.85f), RoundedCornerShape(percent = 50))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PageImageAction(label = Strings.cancel, enabled = true, onClick = onCancel)
            Spacer(modifier = Modifier.size(8.dp))
            PageImageAction(label = EditorStrings.save, enabled = true, onClick = onSave)
        }
    }
}

/**
 * Il menu "+" della barra: **lo stesso catalogo del menu "/"**
 * (`SLASH_ENTRIES`), con le stesse voci, le stesse icone e **lo stesso
 * ordine**, su due colonne riempite riga per riga — la prima voce del
 * "/" è la prima a sinistra, la seconda la prima a destra, la terza la
 * seconda a sinistra, e così via. Le voci non ancora costruite ci sono
 * e sono grigie, come nel "/".
 *
 * Chiesto dall'utente il 24/09/2026: il "+" era rimasto alle nove voci
 * dei primi tempi mentre il "/" cresceva, e i due menu non dicevano più
 * le stesse cose. Leggendo lo stesso elenco non possono più separarsi,
 * e aggiungere una voce al catalogo la mette in tutti e due.
 *
 * A differenza del "/" non ci sono i titoli delle famiglie (Basic blocks,
 * Media, Database): con due colonne riga per riga spezzerebbero l'ordine
 * che l'utente ha chiesto, lasciando buchi a destra a fine famiglia.
 */
@Composable
private fun BlockTypeMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    /** Dal menu del tasto destro sul PC un clic fuori lo chiude, come gli altri menu di Windows. */
    closeOnClickOutside: Boolean = false,
    onSelect: (SlashEntry) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        // **Non si chiude da solo al primo tocco fuori.** Lo chiudono
        // il "+" ritoccato e il tocco sul vuoto della pagina, e basta.
        //
        // La chiusura automatica c'era, e faceva una cosa sgradita: il
        // tocco fuori arrivava **anche** alla pagina sotto, che
        // spostava il cursore. Peggio, chi dei due arrivasse per primo
        // non era detto, quindi non bastava ignorare il tocco "mentre
        // il menu è aperto" — a volte il menu risultava già chiuso.
        // Chiudendolo noi, l'ordine lo decidiamo noi.
        //
        // `focusable = false` resta: un menu che prende il fuoco fa
        // chiudere la tastiera, e qui la tastiera deve restare dov'è.
        properties = PopupProperties(focusable = false, dismissOnClickOutside = closeOnClickOutside)
    ) {
        // Le voci sono più di quante ne stiano sopra la tastiera: il
        // menu scorre (lo fa `DropdownMenu` da sé).
        Column(
            modifier = Modifier
                .width(300.dp)
                .background(DarkBackground)
                .padding(8.dp)
        ) {
            slashEntries().chunked(2).forEach { rowEntries ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowEntries.forEach { entry ->
                        val tint = if (entry.enabled) NotionWhite else NotionWhite.copy(alpha = 0.3f)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = entry.enabled) { onSelect(entry) }
                                .padding(vertical = 12.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                entry.icon,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.size(10.dp))
                            // Tradotti, certi nomi sono lunghi ("Elenco
                            // di cose da fare"): vanno a capo invece di
                            // spingere fuori la colonna accanto.
                            Text(
                                EditorStrings.blockType(entry.label),
                                color = tint,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (rowEntries.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Il menu che si apre scrivendo "/": tutte le cose che si possono
 * mettere in una pagina, divise per famiglia, filtrate da quello che
 * si continua a scrivere dopo la barra.
 *
 * Non è a fuoco (`focusable = false`) di proposito: la tastiera deve
 * restare aperta, perché il modo normale di usarlo è scrivere "/tab"
 * e vedere la lista stringersi.
 */
@Composable
private fun SlashMenu(
    query: String,
    /** La voce che Invio sceglierebbe (vedi `slashKey`), evidenziata. */
    highlighted: SlashEntry?,
    onDismiss: () -> Unit,
    onSelect: (SlashEntry) -> Unit
) {
    // **Con qualcosa scritto, prima le voci che gli somigliano di più.**
    //
    // Prima il filtro teneva ogni voce che *contenesse* le lettere, e le
    // mostrava nell'ordine fisso del catalogo: scrivendo "/d" passavano
    // sedici voci — Heading, Bulleted, Numbered, Video, Code... tutte con
    // una "d" da qualche parte — e "Divider" era la settima, sotto il
    // bordo del riquadro.
    //
    // Adesso vince chi **comincia** con quelle lettere, poi chi ha una
    // **parola** che comincia così ("/list" trova "Bulleted list"), e
    // solo dopo chi le contiene soltanto. Con un filtro le famiglie non
    // si separano più: sarebbe tornato a mettere tutte le voci di base
    // davanti ai database anche quando si sta chiaramente cercando
    // "/data".
    //
    // Si cerca sia nel nome tradotto sia in quello inglese: in italiano
    // "/div" trova "Divisore", ma chi è abituato ai comandi di Notion può
    // continuare a scrivere "/divider".
    val language = AppSettings.language
    val entries = slashEntries()
    val matching = remember(query, language, entries) { if (query.isBlank()) null else slashShown(entries, query) }

    // A ogni lettera la lista ricomincia dall'alto: la voce migliore è
    // la prima, e una lista rimasta scorsa dal filtro di prima la
    // nasconderebbe.
    val scroll = rememberScrollState()
    LaunchedEffect(query) { scroll.scrollTo(0) }

    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        // **Un tasto della tastiera non è un tocco "fuori".** Per Android
        // lo è: la tastiera è un'altra finestra, e un menu che si chiude
        // toccando fuori si chiudeva alla prima lettera battuta dopo la
        // barra. Scrivendo "/d" il menu spariva sulla "d", e il filtro non
        // si vedeva mai. Nelle prove era sfuggito perché la barra e le
        // lettere arrivavano da `adb input text`, che non tocca niente.
        //
        // Si chiude da solo lo stesso quando serve — scelta una voce,
        // tolta la barra, cursore spostato prima di lei o su un'altra
        // riga, uno spazio, fuoco perso: vedi `slashAnchor`.
        properties = PopupProperties(focusable = false, dismissOnClickOutside = false)
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .heightIn(max = 360.dp)
                .background(DarkBackground)
                .verticalScroll(scroll)
                .padding(vertical = 8.dp)
        ) {
            if (matching == null) {
                SlashCategory.entries.filter { category -> entries.any { it.category == category } }.forEach { category ->
                    Text(
                        text = EditorStrings.blockType(category.title),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 14.dp, top = 8.dp, bottom = 4.dp)
                    )
                    entries
                        .filter { it.category == category }
                        .forEach { entry -> SlashMenuRow(entry, entry == highlighted, onSelect) }
                }
            } else if (matching.isEmpty()) {
                Text(
                    text = EditorStrings.nothingWithThatName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            } else {
                matching.forEach { entry -> SlashMenuRow(entry, entry == highlighted, onSelect) }
            }
        }
    }
}

@Composable
private fun SlashMenuRow(entry: SlashEntry, highlighted: Boolean, onSelect: (SlashEntry) -> Unit) {
    // La voce scelta con le frecce resta in vista anche in fondo alla lista.
    val intoView = remember { BringIntoViewRequester() }
    LaunchedEffect(highlighted) { if (highlighted) intoView.bringIntoView() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(intoView)
            .background(if (highlighted) NotionWhite.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(enabled = entry.enabled) { onSelect(entry) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            entry.icon,
            contentDescription = null,
            tint = if (entry.enabled) NotionWhite else NotionWhite.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = EditorStrings.blockType(entry.label),
            color = if (entry.enabled) NotionWhite else NotionWhite.copy(alpha = 0.3f),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * **Le voci del menu "/" nell'ordine in cui si vedono**: con [query] vuota
 * tutte, famiglia per famiglia; se no quelle che le somigliano, prima chi
 * comincia così (vedi `SlashMenu`), e a parità quelle che funzionano davanti
 * a quelle ancora spente. Lo stesso ordine per il menu e per i tasti.
 */
fun slashShown(entries: List<SlashEntry>, query: String): List<SlashEntry> {
    val q = query.trim()
    if (q.isEmpty()) return SlashCategory.entries.flatMap { category -> entries.filter { it.category == category } }
    return entries
        .mapNotNull { entry ->
            listOfNotNull(
                slashMatchRank(EditorStrings.blockType(entry.label), q),
                slashMatchRank(entry.label, q)
            ).minOrNull()?.let { entry to it }
        }
        .sortedWith(compareBy({ it.second }, { !it.first.enabled }))
        .map { it.first }
}

/**
 * Quanto una voce del menu "/" somiglia a quello che si è scritto dopo
 * la barra: 0 se il nome comincia così, 1 se ci comincia una parola del
 * nome ("To-do list" conta "do" come parola), 2 se lo contiene soltanto,
 * null se non c'entra.
 */
private fun slashMatchRank(label: String, query: String): Int? = when {
    label.startsWith(query, ignoreCase = true) -> 0
    label.split(' ', '-').any { it.startsWith(query, ignoreCase = true) } -> 1
    label.contains(query, ignoreCase = true) -> 2
    else -> null
}

/**
 * Invio appena premuto su una riga "/notification …" (vedi
 * `NotificationCommands`): quale riga, e cosa dice. Null se l'ultima
 * modifica non è un Invio, o la riga non è un comando. Vale anche con
 * l'Invio a metà riga: conta la riga intera.
 */
private fun notificationOnEnter(oldBody: String, newBody: String, caret: Int): Pair<Int, NotificationCommand>? {
    if (newBody.length != oldBody.length + 1 || caret < 1 || caret > newBody.length) return null
    if (newBody[caret - 1] != '\n' || newBody.removeRange(caret - 1, caret) != oldBody) return null
    val lineIndex = oldBody.take(caret - 1).count { it == '\n' }
    val line = oldBody.split('\n').getOrNull(lineIndex) ?: return null
    val command = NotificationCommands.parse(line, LocalDateTime.now(AppSettings.zoneId)) ?: return null
    return lineIndex to command
}

/**
 * Una riga-notifica già suonata: la campanella e la data si fanno grigie.
 * **Da sole, nel momento in cui suona**: guardando l'ora solo quando la
 * pagina si ridisegnava, la riga restava blu a notifica arrivata (visto
 * sul telefono il 25/09/2026, alle 22:59).
 */
@Composable
private fun rememberNotificationIsPast(block: BlockEntity): Boolean {
    val at = block.notifyAt
    var past by remember(at) { mutableStateOf(at != null && at <= System.currentTimeMillis()) }
    LaunchedEffect(at) {
        if (at == null || past) return@LaunchedEffect
        delay((at - System.currentTimeMillis()).coerceAtLeast(0))
        past = true
    }
    return past
}

/**
 * **Quando suona la riga-notifica**, in fondo alla riga: toccandola si
 * sceglie il giorno e poi l'ora, con le finestre di Samsung. Appena creata
 * dal menu "/" si apre da sola (`notificationPickerFor`).
 *
 * **Oggi e domani si scrivono per nome** ("Domani 19:34"), non con la
 * data. La prima prova dell'utente, il 25/09/2026: "/notification" senza
 * ora dopo le 9 va alle 9:00 di domani, poi scegliendo solo l'ora il
 * giorno restava domani, e la notifica "non arrivava" ad app chiusa. Con
 * "26.09.2026 19:34" non se n'era accorto; "Domani 19:34" si legge. E se
 * l'ora scelta è già passata lo si dice subito, perché non suonerà.
 */
@Composable
private fun NotificationTimeChip(block: BlockEntity, viewModel: PageEditorViewModel) {
    val at = block.notifyAt ?: return
    val zone = AppSettings.zoneId
    val current = Instant.ofEpochMilli(at).atZone(zone).toLocalDateTime()
    val locked = LocalPageLocked.current
    var pickingDate by remember(block.id) { mutableStateOf(false) }
    var pickedDate by remember(block.id) { mutableStateOf<java.time.LocalDate?>(null) }
    val autoOpen by viewModel.notificationPickerFor.collectAsStateWithLifecycle()
    LaunchedEffect(autoOpen) {
        if (autoOpen == block.id) {
            pickingDate = true
            viewModel.consumeNotificationPicker()
        }
    }
    val past = rememberNotificationIsPast(block)
    val context = LocalPlatformContext.current
    val today = java.time.LocalDate.now(zone)
    val label = when (current.toLocalDate()) {
        today -> "${DbStrings.today} ${Formats.time(at)}"
        today.plusDays(1) -> "${NotifyStrings.tomorrow} ${Formats.time(at)}"
        else -> Formats.dateTime(at)
    }
    Box(
        modifier = Modifier
            .padding(start = 8.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (past) NotionGray400.copy(alpha = 0.15f) else AccentBlue.copy(alpha = 0.15f))
            .clickable(enabled = !locked) { pickingDate = true }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (past) NotionGray400 else AccentBlue,
            maxLines = 1
        )
    }
    if (pickingDate) {
        SamsungDateDialog(
            initial = current.toLocalDate(),
            onPick = { pickedDate = it },
            onDismiss = { pickingDate = false }
        )
    }
    pickedDate?.let { day ->
        SamsungTimeDialog(
            initial = current.toLocalTime(),
            onPick = { time ->
                val chosen = LocalDateTime.of(day, time).atZone(zone).toInstant().toEpochMilli()
                viewModel.setNotificationTime(block, chosen)
                if (chosen <= System.currentTimeMillis()) {
                    showMessage(context, NotifyStrings.alreadyPassed, long = true)
                }
            },
            onDismiss = { pickedDate = null }
        )
    }
}

/**
 * Mappa le posizioni tra il testo "vero" (quello che il campo modifica
 * davvero: solo il contenuto, senza prefissi) e il testo "mostrato"
 * (con • / numeri iniettati davanti a ciascuna riga, non modificabili
 * direttamente) — necessaria perché VisualTransformation aggiunge
 * caratteri che non esistono nel testo reale, non solo stili.
 * Un tocco dentro un prefisso si aggancia all'inizio del contenuto
 * vero di quella riga.
 *
 * `leading` è l'a-capo iniziale nascosto (vedi RUN_LEAD): la prima riga
 * comincia lì, non a zero. È il motivo per cui nessuna posizione
 * mostrata si traduce mai in zero — il cursore non può mettersi prima
 * di quell'a-capo, e proprio per questo "cursore a zero" è un segnale
 * inequivocabile che il backspace l'ha cancellato.
 */
private class MergedRunOffsetMapping(
    private val lineLengths: List<Int>,
    private val prefixLengths: List<Int>,
    private val leading: Int
) : OffsetMapping {
    private val realStarts: List<Int>
    private val dispStarts: List<Int>

    init {
        val rs = mutableListOf(leading)
        val ds = mutableListOf(0)
        for (i in lineLengths.indices) {
            rs.add(rs.last() + lineLengths[i] + 1)
            ds.add(ds.last() + prefixLengths[i] + lineLengths[i] + 1)
        }
        realStarts = rs
        dispStarts = ds
    }

    private fun lineIndexForReal(o: Int): Int {
        for (i in lineLengths.indices) {
            if (o <= realStarts[i] + lineLengths[i]) return i
        }
        return (lineLengths.size - 1).coerceAtLeast(0)
    }

    private fun lineIndexForDisplayed(d: Int): Int {
        for (i in lineLengths.indices) {
            if (d <= dispStarts[i] + prefixLengths[i] + lineLengths[i]) return i
        }
        return (lineLengths.size - 1).coerceAtLeast(0)
    }

    override fun originalToTransformed(offset: Int): Int {
        if (lineLengths.isEmpty()) return 0
        if (offset < leading) return 0
        val i = lineIndexForReal(offset)
        return dispStarts[i] + prefixLengths[i] + (offset - realStarts[i])
    }

    override fun transformedToOriginal(offset: Int): Int {
        if (lineLengths.isEmpty()) return leading
        val i = lineIndexForDisplayed(offset)
        val withinPrefix = offset < dispStarts[i] + prefixLengths[i]
        return if (withinPrefix) realStarts[i] else realStarts[i] + (offset - dispStarts[i] - prefixLengths[i])
    }
}

/**
 * Il rientro è fatto di spazi dentro il prefisso, non con un
 * `ParagraphStyle`. Motivo: applicare uno stile di paragrafo a una parte
 * di un `AnnotatedString` spezza il testo in paragrafi separati, e
 * l'a-capo che finisce al confine fra due paragrafi viene reso come una
 * riga vuota in più — comparivano buchi verticali sopra e sotto ogni
 * riga rientrata. Con gli spazi il testo resta un paragrafo solo.
 */
private const val INDENT_SPACES = "    "

/**
 * Di quanto il segno scende rispetto alla riga di base del testo.
 *
 * È una frazione del corpo del segno, non una misura fissa: così i
 * pallini (scritti grandi) e i rombi (scritti piccoli) scendono in
 * proporzione e restano allineati fra loro. Negativo vuol dire verso il
 * basso.
 *
 * Non è un centraggio calcolato ma un ritocco a occhio, chiesto
 * guardando lo schermo: appoggiati esattamente sulla riga di base
 * sembravano un filo troppo in alto rispetto al testo.
 */
private val BULLET_MARKER_DROP = BaselineShift(-0.08f)

/** Il tipo che erediterebbe una riga nuova creata sotto una di questo tipo: gli elenchi si propagano, i titoli no. */
private fun inheritedListType(previous: BlockType?): BlockType = when (previous) {
    BlockType.BULLET_LIST_ITEM, BlockType.NUMBERED_LIST_ITEM -> previous
    else -> BlockType.PARAGRAPH
}

// La previsione del numero per una riga appena creata non passa più da
// qui: si porta avanti il **conteggio** (vedi `carriedOrdinal` in
// `runPrefixes`) invece di provare a incrementare l'etichetta scritta.
// Con i numeri romani e "Two." non ci sarebbe niente da incrementare.

/**
 * I prefissi di tutte le righe di un gruppo.
 *
 * Le ultime righe possono non avere ancora un blocco corrispondente:
 * subito dopo un Invio la riga esiste già nel campo, ma il blocco è
 * ancora in viaggio verso il database. Per quelle il prefisso viene
 * previsto, ereditandolo dalla riga sopra con la stessa regola che usa
 * `updateRun`. Senza questa previsione la riga nuova comparirebbe per
 * un istante senza numero, con il cursore appoggiato al margine
 * sinistro, per poi saltare a destra appena il numero arriva.
 *
 * Sta qui, fuori dalla trasformazione, perché serve in due posti che
 * devono per forza essere d'accordo: chi disegna i prefissi e chi
 * calcola dove sono finiti sullo schermo per renderli toccabili. Se i
 * due divergessero, si toccherebbe un numero e ne risponderebbe un
 * altro.
 */
/**
 * Dove si trova il cursore nel testo MOSTRATO, cioè quello che
 * comprende anche i prefissi (rientri, • e numeri).
 *
 * Il layout del testo conosce solo le posizioni mostrate, mentre il
 * cursore vive in quelle vere: senza questa conversione non si può
 * sapere a che altezza dello schermo si sta scrivendo.
 */
private fun displayedCaretOffset(
    body: String,
    caretInBody: Int,
    prefixes: List<LinePrefix>
): Int {
    var bodyPos = 0
    var displayPos = 0
    body.split('\n').forEachIndexed { i, line ->
        val prefixLength = prefixes.getOrNull(i)?.text?.length ?: 0
        if (caretInBody <= bodyPos + line.length) {
            return displayPos + prefixLength + (caretInBody - bodyPos)
        }
        bodyPos += line.length + 1
        displayPos += prefixLength + line.length + 1
    }
    return displayPos
}

/**
 * Quale blocco ha ogni riga del campo. `insertedAt` è la riga appena nata
 * con un Invio, `removedAt` il blocco della riga appena sparita col
 * backspace (-1 se non c'è).
 */
class LineBlocks(val blocks: List<BlockEntity?>, val insertedAt: Int, val removedAt: Int)

/**
 * **Il blocco di ogni riga del campo, anche quando il database è indietro.**
 *
 * Subito dopo un Invio il campo ha una riga in più dei blocchi, subito dopo
 * un backspace che unisce due righe una in meno, e per un istante — finché
 * il database non ha scritto — "il blocco numero i" è quello della riga
 * accanto. Chi disegnava in base a quello (il pallino degli elenchi, i font,
 * i colori, pedici e apici) mostrava per un fotogramma le righe sotto senza
 * la loro formattazione, e si vedevano **scattare** (trovato dall'utente il
 * 26/09/2026, andando a capo in cima alla pagina principale).
 *
 * Si cerca **prima dalla fine**: le righe in fondo che combaciano ancora
 * coi loro blocchi dicono quante ce ne sono dopo quelle nate o sparite.
 * Dall'inizio da solo non funziona, perché andando a capo anche la riga
 * divisa cambia testo. Poi dall'inizio, fin dove si può; in mezzo restano
 * le righe cambiate, che prendono i blocchi di mezzo nell'ordine, e quelle
 * in più sono nuove. Vale anche con più righe di differenza — Invio premuti
 * in fretta, che il database scrive uno alla volta.
 */
fun blocksOfLines(
    lines: List<String>,
    runBlocks: List<BlockEntity>,
    textOf: (BlockEntity) -> String
): LineBlocks {
    val n = lines.size
    val m = runBlocks.size
    if (n == m) return LineBlocks(runBlocks, -1, -1)
    var tail = 0
    while (tail < n && tail < m && lines[n - 1 - tail] == textOf(runBlocks[m - 1 - tail])) tail++
    var head = 0
    val maxHead = minOf(n, m) - tail
    while (head < maxHead && lines[head] == textOf(runBlocks[head])) head++
    val blocks = lines.indices.map { i ->
        when {
            i < head -> runBlocks[i]
            // Sotto le righe nuove tutto è sceso (o salito) di tante righe.
            i >= n - tail -> runBlocks[i - n + m]
            // In mezzo: i blocchi di mezzo nell'ordine, poi righe nuove.
            i < m - tail -> runBlocks[i]
            else -> null
        }
    }
    return LineBlocks(
        blocks,
        insertedAt = if (n == m + 1) blocks.indexOfFirst { it == null } else -1,
        // Il primo blocco di mezzo rimasto senza riga.
        removedAt = if (m == n + 1) n - tail else -1
    )
}

/**
 * La formattazione **prevista** delle righe toccate da un Invio o da un
 * backspace che il database non ha ancora scritto: la riga divisa in due
 * tiene i suoi font nelle due metà, le due righe unite li tengono tutti e
 * due. È quello che il database scriverà (`applyTextEdit` e `splitLines`,
 * come in `updateRun`); se il conto non torna, niente previsione.
 */
private fun predictedLineSpans(
    lines: List<String>,
    runBlocks: List<BlockEntity>,
    aligned: LineBlocks,
    viewModel: PageEditorViewModel
): Map<Int, List<RichTextSpan>> {
    val at = aligned.insertedAt
    if (at > 0) {
        val above = aligned.blocks[at - 1] ?: return emptyMap()
        val storedText = viewModel.plainTextOf(above)
        if (storedText == lines[at - 1]) return emptyMap()
        val parts = applyTextEdit(viewModel.spansOf(above), storedText, lines[at - 1] + "\n" + lines[at]).splitLines()
        if (parts.size != 2 || parts[0].plainText() != lines[at - 1] || parts[1].plainText() != lines[at]) return emptyMap()
        return mapOf(at - 1 to parts[0], at to parts[1])
    }
    val removed = aligned.removedAt
    if (removed > 0) {
        val first = runBlocks[removed - 1]
        val second = runBlocks[removed]
        if (viewModel.plainTextOf(first) + viewModel.plainTextOf(second) != lines[removed - 1]) return emptyMap()
        return mapOf(removed - 1 to viewModel.spansOf(first) + viewModel.spansOf(second))
    }
    return emptyMap()
}

fun runPrefixes(
    lines: List<String>,
    runBlocks: List<BlockEntity>,
    /** Il numero di ogni riga di elenco numerato: vedi `PageEditorViewModel.numberedOrdinals`. */
    ordinals: Map<String, Int>,
    viewModel: PageEditorViewModel
): List<LinePrefix> {
    var carriedType: BlockType? = null
    var carriedIndent = 0
    var carriedOrdinal: Int? = null
    // L'elenco di cui la riga sopra teneva il posto: una riga appena nata
    // sotto, che il database non conosce ancora, lo tiene anche lei.
    var carriedHeld: BlockType? = null
    // L'ultimo numero mostrato a ogni rientro: la riga che tiene il posto
    // di un elenco numerato ci scrive (invisibile) il numero dopo, che è
    // largo come quelli intorno.
    val lastNumberAt = mutableMapOf<Int, Int>()

    // Il blocco di ogni riga, anche nell'istante in cui il database è
    // indietro di un Invio o di un backspace: vedi `blocksOfLines`. La
    // riga appena nata non ne ha, e il suo prefisso si prevede.
    val lineBlocks = blocksOfLines(lines, runBlocks) { viewModel.plainTextOf(it) }.blocks

    return lines.indices.map { i ->
        val block = lineBlocks[i]
        val type = block?.type ?: inheritedListType(carriedType)
        val indent = (block?.indentLevel ?: carriedIndent).coerceAtLeast(0)
        val held = if (block != null) block.heldListType else carriedHeld.takeIf { type == BlockType.PARAGRAPH }
        // A che numero è arrivata questa riga. Per una riga che il
        // database non conosce ancora — appena creata con un Invio — si
        // prevede: è quella dopo la riga sopra, se quella era un elenco
        // numerato allo stesso rientro.
        val ordinal = if (type == BlockType.NUMBERED_LIST_ITEM) {
            when {
                block != null -> ordinals[block.id] ?: 1
                carriedOrdinal != null && carriedIndent == indent -> carriedOrdinal!! + 1
                else -> 1
            }
        } else {
            null
        }
        val label = ordinal?.let { numberedMarkerFor(indent, it) }
        if (ordinal != null) lastNumberAt[indent] = ordinal

        carriedType = type
        carriedIndent = indent
        carriedOrdinal = ordinal
        carriedHeld = held

        if (held != null) {
            // Il posto dell'elenco: lo stesso marcatore, invisibile.
            val ghost = when (held) {
                BlockType.BULLET_LIST_ITEM -> bulletMarkerFor(indent) + "  "
                else -> numberedMarkerFor(indent, (lastNumberAt[indent] ?: 0) + 1) + "  "
            }
            return@map LinePrefix(indent = INDENT_SPACES.repeat(indent), marker = ghost, hidden = true, listType = held)
        }

        val marker = when (type) {
            BlockType.BULLET_LIST_ITEM -> bulletMarkerFor(indent) + "  "
            BlockType.NUMBERED_LIST_ITEM -> "$label  "
            BlockType.CHECKBOX -> CHECKBOX_MARKER
            else -> ""
        }
        LinePrefix(indent = INDENT_SPACES.repeat(indent), marker = marker, listType = type.takeIf { marker.isNotEmpty() })
    }
}

/**
 * Le misure per lo spazio dopo i marcatori (vedi `MarkerGapScale`): quanto
 * sono larghi davvero, col carattere del campo, i due spazi dopo numeri e
 * pallini e i quattro della casella, misurati una volta; e dove finisce il
 * quadratino, che `RunCheckboxOverlay` disegna centrato in
 * `CHECKBOX_TOUCH_SIZE` a partire dall'inizio della casella.
 */
@Composable
private fun rememberMarkerGapScale(look: ParagraphLook): MarkerGapScale {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.bodyLarge
    val density = LocalDensity.current
    val widths = remember(measurer, style, density) {
        // La posizione del carattere dopo gli spazi: la larghezza di una
        // riga fatta di soli spazi non si misura (gli spazi in fondo non contano).
        val pair = measurer.measure("  x", style).getHorizontalPosition(2, usePrimaryDirection = true)
        val box = measurer.measure(CHECKBOX_MARKER + "x", style)
            .getHorizontalPosition(CHECKBOX_MARKER.length, usePrimaryDirection = true)
        pair to box
    }
    return remember(look, widths, density) {
        with(density) {
            MarkerGapScale(
                look = look,
                spacePairPx = widths.first,
                checkboxPx = widths.second,
                squareEndPx = (CHECKBOX_TOUCH_SIZE - (CHECKBOX_TOUCH_SIZE - CHECKBOX_SIZE) / 2).toPx(),
                pxPerPt = 1.sp.toPx()
            )
        }
    }
}

/**
 * Colora/stila il testo unito di un gruppo di blocchi di testo
 * scorrevole: aggiunge il prefisso (• o numero) davanti a ciascuna
 * riga in base al tipo del blocco corrispondente, la dimensione/peso
 * per i titoli, e il rientro visivo (indentLevel) come stile di
 * paragrafo — tutto puramente a schermo, il testo vero sotto resta
 * semplice testo con newline a separare i blocchi.
 */
private class MergedRunVisualTransformation(
    private val runBlocks: List<BlockEntity>,
    private val ordinals: Map<String, Int>,
    private val viewModel: PageEditorViewModel,
    /**
     * Dove sta il cursore, in posizioni del testo senza l'a-capo
     * nascosto, oppure `null` se il campo non ha il fuoco. Serve solo
     * agli spoiler: quello che il cursore tocca si scopre. Col campo
     * non a fuoco non c'è nessun cursore da guardare — la selezione
     * resta quella di prima, e senza questo `null` uno spoiler già
     * aperto sarebbe rimasto aperto anche dopo essere usciti.
     */
    private val caret: IntRange?,
    /** Le parole sbagliate di una riga, se il correttore è acceso: vedi `ProofingUi.kt`. */
    private val proof: ((String) -> List<IntRange>)? = null,
    /** Dove finisce quello che si è appena scritto, nel testo vero: la parola lì non si segna ancora. */
    private val typingAt: Int? = null,
    /** Lo spazio dopo i marcatori degli elenchi ("Paragraph"): vedi `MarkerGapScale`. Null = come sempre. */
    private val gapScale: MarkerGapScale? = null
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val hasLead = text.text.startsWith(RUN_LEAD)
        val lines = bodyOf(text.text).split('\n')
        val prefixes = runPrefixes(lines, runBlocks, ordinals, viewModel)
        // Il blocco di ogni riga anche mentre il database è indietro, e la
        // formattazione prevista delle righe appena divise o unite: vedi
        // `blocksOfLines`.
        val aligned = blocksOfLines(lines, runBlocks) { viewModel.plainTextOf(it) }
        val predicted = predictedLineSpans(lines, runBlocks, aligned, viewModel)
        val builder = AnnotatedString.Builder()
        val lineLengths = mutableListOf<Int>()
        val prefixLengths = mutableListOf<Int>()
        // Dove comincia la riga corrente nel testo vero, per tradurre le
        // posizioni degli span in posizioni del cursore.
        var lineBodyStart = 0
        // L'ultima riga che il database conosce: una riga appena nata ha il
        // "Paragraph" di quella da cui nasce (`attrsForNewLine`).
        var lastKnownBlock: BlockEntity? = null

        lines.forEachIndexed { i, line ->
            val block = aligned.blocks[i]
            val prefix = prefixes[i]
            val lineStart = builder.length
            builder.append(prefix.text)
            // **Lo spazio dopo il marcatore** (dal 21/10/2026, "Paragraph"):
            // solo la larghezza degli spazi in fondo al marcatore, vedi
            // `MarkerGapScale`. Nessun carattere cambia.
            val listType = prefix.listType
            if (gapScale != null && listType != null) {
                val gap = gapScale.look.gapOf(block ?: lastKnownBlock, listType)
                if (gap != null) {
                    gapScale.styleFor(prefix, listType, gap, lineStart)?.let { (style, from, to) ->
                        builder.addStyle(style, from, to)
                    }
                }
            }
            if (block != null) lastKnownBlock = block
            val contentStart = builder.length
            // Il segno dell'elenco puntato è più piccolo del testo:
            // vedi `BULLET_MARKER_SIZE`. Si applica al solo marcatore,
            // non al rientro, e non cambia nessun conteggio di
            // caratteri — la traduzione delle posizioni resta intatta.
            val bulletAt = BULLET_MARKERS.indexOfFirst { prefix.marker.startsWith(it) }
            if (bulletAt >= 0) {
                // Solo il simbolo, non i due spazi che lo seguono:
                // toccando anche quelli lo stacco dal testo cambierebbe
                // e gli elenchi puntati non sarebbero più allineati con
                // quelli numerati.
                val symbolStart = lineStart + prefix.indent.length
                builder.addStyle(
                    SpanStyle(
                        fontSize = BULLET_MARKER_SIZES[bulletAt],
                        baselineShift = BULLET_MARKER_DROP
                    ),
                    symbolStart,
                    symbolStart + 1
                )
            }
            // Il posto di un elenco: il marcatore c'è, largo uguale, ma
            // non si vede (vedi `LinePrefix.hidden`).
            if (prefix.hidden) {
                builder.addStyle(
                    SpanStyle(color = Color.Transparent),
                    lineStart + prefix.indent.length,
                    lineStart + prefix.text.length
                )
            }
            builder.append(line)
            lineLengths.add(line.length)
            prefixLengths.add(prefix.text.length)

            when (block?.type) {
                // Una casella spuntata ha il testo sbarrato e smorto,
                // come su Notion. Il quadratino invece lo disegna
                // `RunCheckboxOverlay` sopra al testo di riempimento.
                // Una casella spuntata ha il testo sbarrato e smorto,
                // come su Notion. Il quadratino invece lo disegna
                // `RunCheckboxOverlay` sopra al testo di riempimento.
                BlockType.CHECKBOX -> if (block.isChecked) {
                    builder.addStyle(
                        SpanStyle(
                            textDecoration = TextDecoration.LineThrough,
                            color = NotionGray400
                        ),
                        contentStart,
                        builder.length
                    )
                }
                else -> {}
            }

            // Formattazione inline (Aa): gli span di questo blocco,
            // posizionati rispetto all'inizio del suo contenuto vero
            // (dopo l'eventuale prefisso • o numero).
            //
            // **Gli span salvati sono sempre un giro indietro.** Il campo
            // ha già la lettera appena battuta, il database ce l'avrà fra
            // qualche decina di millisecondi. Prima, in quel fotogramma,
            // non si disegnava niente: per grassetto e corsivo non si
            // notava, ma uno spoiler che per un istante non viene
            // coperto **si legge** — è lo scatto per cui la parola
            // censurata riappariva a ogni tasto.
            //
            // Adesso, invece di rinunciare, si applica agli span la
            // stessa modifica che ha subito il testo: `applyTextEdit`
            // tocca solo il pezzo cambiato e restituisce span che
            // descrivono **esattamente** la riga mostrata, quindi le
            // posizioni non possono sfasarsi.
            //
            // Con un numero di righe diverso dai blocchi, però, no: lì è
            // appena nata o appena sparita una riga, la corrispondenza
            // riga-blocco non è più affidabile, e uno stile preso dal
            // blocco sbagliato sarebbe peggio di nessuno stile.
            // Mentre il database è indietro di una riga, i blocchi sono già
            // abbinati alle righe giuste (`aligned`): quelle rimaste uguali
            // tengono i loro span, e le righe appena divise o unite quelli
            // previsti.
            run {
                val blockSpans = predicted[i] ?: block?.let {
                    val stored = viewModel.spansOf(it)
                    val storedText = stored.plainText()
                    when {
                        storedText == line -> stored
                        lines.size != runBlocks.size -> null
                        else -> applyTextEdit(stored, storedText, line)
                    }
                }
                if (blockSpans != null) {
                    var spanPos = 0
                    for ((spanIndex, span) in blockSpans.withIndex()) {
                        val s = contentStart + spanPos
                        val e = s + span.text.length
                        spanPos += span.text.length
                        // Lo spazio per la freccia dei collegamenti dentro la pagina.
                        builder.markBlockLinkEnd(span, blockSpans.getOrNull(spanIndex + 1), s, e)
                        if (span.bold || span.italic || span.underline || span.strikethrough || span.link != null) {
                            val decorations = mutableListOf<TextDecoration>()
                            // Un collegamento è sempre sottolineato.
                            if (span.underline || span.link != null) decorations.add(TextDecoration.Underline)
                            if (span.strikethrough) decorations.add(TextDecoration.LineThrough)
                            builder.addStyle(
                                SpanStyle(
                                    fontWeight = if (span.bold) FontWeight.Bold else null,
                                    fontStyle = if (span.italic) FontStyle.Italic else null,
                                    textDecoration = if (decorations.isEmpty()) null else TextDecoration.combine(decorations)
                                ),
                                s,
                                e
                            )
                        }
                        // Il blu del collegamento prima del pennello: un
                        // colore scelto apposta vince.
                        linkStyleOf(span)?.let { builder.addStyle(it, s, e) }
                        // I colori **prima** dello spoiler: coperto, il
                        // velo deve vincere su tutto, altrimenti si
                        // leggerebbe il testo colorato attraverso.
                        colorStyleOf(span)?.let { builder.addStyle(it, s, e) }
                        fontStyleOf(span)?.let { builder.addStyle(it, s, e) }
                        scriptStyleOf(span)?.let { builder.addStyle(it, s, e) }
                        if (span.spoiler) {
                            // Posizioni nel testo vero, che è quello in
                            // cui vive il cursore: il prefisso (• o
                            // numero) davanti alla riga esiste solo a
                            // schermo e qui va scontato.
                            val fromBody = lineBodyStart + (s - contentStart)
                            val revealed = touchesCaret(fromBody, fromBody + span.text.length)
                            builder.addStyle(spoilerStyle(revealed), s, e)
                            // Un link coperto non si apre: il tocco
                            // serve a scoprirlo.
                            if (revealed) builder.markLink(span, s, e)
                        } else {
                            builder.markLink(span, s, e)
                        }
                    }
                }
            }
            // Le parole sbagliate della riga (vedi `ProofingUi.kt`), tranne
            // quelle sotto uno spoiler e quella che si sta scrivendo.
            proof?.let { p ->
                val lineSpans = predicted[i] ?: block?.let { b -> viewModel.spansOf(b).takeIf { it.plainText() == line } }
                val typingEnd = typingAt?.let { it - lineBodyStart }?.takeIf { it in 0..line.length }
                builder.markMisspellings(i, p(line), contentStart, lineSpans, typingEnd)
            }
            lineBodyStart += line.length + 1
            if (i < lines.size - 1) builder.append("\n")
        }
        return TransformedText(
            builder.toAnnotatedString().withEmojiFont(),
            MergedRunOffsetMapping(lineLengths, prefixLengths, if (hasLead) 1 else 0)
        )
    }

    /**
     * Vero se il cursore — o la selezione — sta **dentro** questo pezzo
     * di testo. Sui bordi no, di proposito: appena finito di scrivere un
     * `||...||` il cursore resta appoggiato subito dopo l'ultima
     * lettera, e contando anche il bordo lo spoiler sarebbe nato già
     * scoperto. Si vuole invece che si copra all'istante, come su
     * Discord. Per leggerlo basta toccarlo: un tocco cade dentro la
     * parola, non sul suo bordo.
     */
    private fun touchesCaret(start: Int, end: Int): Boolean {
        val c = caret ?: return false
        return c.first < end && c.last > start
    }
}

/**
 * Il velo che copre uno spoiler, o la traccia che resta quando è stato
 * scoperto.
 *
 * Da coperto le lettere si disegnano **trasparenti** invece di essere
 * tolte: restano dove sono, quindi la riga è larga uguale, va a capo
 * dove andava e il cursore si muove come sempre. Sopra ci passa la
 * fascia grigia, che è proprio quello che si vede su Discord.
 */
/**
 * Il colore del testo e quello dietro al testo, o null se questo pezzo
 * non ne ha nessuno dei due.
 *
 * Un esadecimale che non si riesce a leggere viene **ignorato** invece
 * di far saltare tutto: quei codici li scrive l'utente a mano, e una
 * lettera di troppo non deve far sparire il testo.
 */
private fun colorStyleOf(span: RichTextSpan): SpanStyle? {
    val text = hexToColor(span.color)
    val behind = hexToColor(span.background)
    if (text == null && behind == null) return null
    return SpanStyle(
        color = text ?: Color.Unspecified,
        background = behind ?: Color.Unspecified
    )
}

/**
 * Pedice e apice: il testo a sette decimi, spostato sotto o sopra la riga
 * di quanto è alto, come in Word. In `em`, cioè relativo al corpo del suo
 * pezzo di testo (barra Aa): con un corpo più grande crescono anche loro.
 *
 * Solo aspetto: le lettere restano dove sono nel testo, quindi cursore,
 * selezione e ricerca non se ne accorgono.
 */
fun scriptStyleOf(span: RichTextSpan): SpanStyle? = when {
    span.subscript -> SpanStyle(fontSize = SCRIPT_SIZE, baselineShift = SUBSCRIPT_SHIFT)
    span.superscript -> SpanStyle(fontSize = SCRIPT_SIZE, baselineShift = BaselineShift.Superscript)
    else -> null
}

/**
 * Il font e il corpo scelti per questo pezzo di testo dalla barra Aa (dal
 * 25/09/2026). Va aggiunto **prima** di `scriptStyleOf`: il pedice è a sette
 * decimi del corpo del suo pezzo, non di quello normale. Un nome di font che
 * l'app non conosce più si ignora.
 */
fun fontStyleOf(span: RichTextSpan): SpanStyle? {
    val family = span.font?.let { name -> runCatching { PageFont.valueOf(name) }.getOrNull()?.fontFamily() }
    val size = span.size?.takeIf { it != DEFAULT_PAGE_FONT_SIZE }
    if (family == null && size == null) return null
    // Sul PC il tema rimpicciolisce il testo: una grandezza scelta a mano resta quella (`chosenSizeSp`).
    return SpanStyle(fontFamily = family, fontSize = size?.let { chosenSizeSp(it) } ?: TextUnit.Unspecified)
}

/**
 * Da dove a dove sta ogni riga del campo nel testo **mostrato**, coi prefissi
 * (lo stesso conto del lampo di una riga spostata): per velarla e per
 * trovarla col mouse (vedi `BlockSelection.kt`).
 */
private fun shownLineRanges(
    fieldText: String,
    runBlocks: List<BlockEntity>,
    ordinals: Map<String, Int>,
    viewModel: PageEditorViewModel
): List<IntRange> {
    val lines = bodyOf(fieldText).split('\n')
    val prefixes = runPrefixes(lines, runBlocks, ordinals, viewModel)
    var start = 0
    return lines.indices.map { i ->
        val length = (prefixes.getOrNull(i)?.text?.length ?: 0) + lines[i].length
        (start..start + length).also { start += length + 1 }
    }
}

/**
 * Le lettere della prima riga del blocco [id] nel campo, per i sei puntini
 * (`BlockHandle.kt`): dalla riga di base in su quanto è alta una maiuscola.
 * **Non la riga intera** (dal 11/11/2026, trovato dall'utente: i puntini
 * "storti"): nella riga c'è anche lo spazio fra i blocchi, e a metà di lei
 * i puntini stavano un paio di pixel sopra le lettere.
 */
private fun runLineBounds(
    layout: TextLayoutResult,
    id: String,
    fieldText: String,
    runBlocks: List<BlockEntity>,
    ordinals: Map<String, Int>,
    viewModel: PageEditorViewModel
): LineBounds? {
    val index = runBlocks.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: return null
    val range = shownLineRanges(fieldText, runBlocks, ordinals, viewModel).getOrNull(index) ?: return null
    val text = layout.layoutInput.text
    val line = layout.getLineForOffset(range.first.coerceIn(0, text.length))
    // Il corpo del testo della riga (l'ultima lettera: il marcatore davanti può essere più grande).
    val at = (range.last - 1).coerceIn(0, (text.length - 1).coerceAtLeast(0))
    val size = text.spanStyles.lastOrNull { it.start <= at && at < it.end && it.item.fontSize.isSp }?.item?.fontSize
        ?: layout.layoutInput.style.fontSize
    val capHeight = with(layout.layoutInput.density) { if (size.isSp) size.toPx() * CAP_HEIGHT else 0f }
    val baseline = layout.getLineBaseline(line)
    return if (capHeight > 0f) LineBounds(baseline - capHeight, baseline) else LineBounds(layout.getLineTop(line), layout.getLineBottom(line))
}

/** Quanto è alta una maiuscola rispetto al corpo del carattere (circa, per i caratteri della pagina). */
private const val CAP_HEIGHT = 0.7f

/** Il riquadro di una colonna vuota (vedi `ColumnLines`): attorno al campo, un poco oltre le lettere. */
private fun DrawScope.drawEmptyColumn(color: Color) {
    val side = (PAGE_SIDE_PADDING - 6.dp).toPx()
    val extra = 2.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(side, -extra),
        size = Size(size.width - 2 * side, size.height + 2 * extra),
        cornerRadius = CornerRadius(4.dp.toPx())
    )
}

/** Quanto è largo, dall'inizio o dalla fine del testo, il bordo che mette un blocco trascinato accanto a un altro (vedi `BlockDrag.kt`). */
private val DROP_SIDE_ZONE = 60.dp

/** Quanto è alta una colonna senza righe, da toccare per scriverci (vedi `BlockRow`). */
private val EMPTY_COLUMN_HEIGHT = 32.dp

/**
 * Le righe di una colonna ([column]; un blocco finito fra le colonne che non
 * è una colonna si mostra lui), ognuna col suo `key` **in questa stessa
 * funzione**: passate una per una da una funzione a parte, il `key` non
 * bastava a riconoscerle quando ne nasceva una sopra, e la riga col cursore
 * si rifaceva da capo col testo di prima (trovato dall'utente il 12/11/2026:
 * "mi duplica quello che ho scritto rimanendo nello stesso blocco").
 *
 * Ogni riga ha i suoi sei puntini; il gruppo no (dal 13/11/2026, vedi
 * `BlockPicker.lineAt`).
 */
@Composable
private fun ColumnLines(
    column: BlockEntity,
    allBlocks: List<BlockEntity>,
    depth: Int,
    viewModel: PageEditorViewModel,
    factory: ViewModelFactory,
    focusRequestBlockId: String?,
    formatRequest: Pair<String, FormatType>?,
    onFocusChanged: (String, Boolean) -> Unit,
    onNavigateToPage: (String) -> Unit,
    onNavigateToDatabase: (String) -> Unit,
    onOpenBlockMenu: ((String) -> Unit)?
) {
    val look = LocalParagraphLook.current
    val density = LocalDensity.current
    val lines = if (column.type == BlockType.COLUMN) {
        allBlocks.filter { it.parentBlockId == column.id }.sortedBy { it.orderIndex }
    } else {
        listOf(column)
    }
    // **Una colonna vuota si vede** (dal 12/11/2026, chiesto dall'utente: "su
    // Notion Create three columns mi crea un blocco a sinistra, uno al centro
    // e uno a destra"): sul PC un riquadro chiaro dove scrivere, finché la
    // colonna non ha niente di scritto. Prima le colonne appena nate non si
    // vedevano, e la seconda bisognava indovinarla.
    val emptyColumn = isPc && column.type == BlockType.COLUMN && lines.isNotEmpty() &&
        lines.all { it.type == BlockType.PARAGRAPH && viewModel.plainTextOf(it).isEmpty() }
    val emptyLook = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    val sidePaddingPx = with(density) { PAGE_SIDE_PADDING.toPx() }
    val lineHandleSpan = with(density) { 26.dp.toPx() }
    lines.forEachIndexed { index, child ->
        key(child.id) {
            // Un database dice qual è la sua prima riga (il titolo): lì i suoi sei puntini,
            // come nella pagina (trovato dall'utente: "i sei puntini sono stortissimi").
            val lineFirst = remember(child.type) { if (child.type == BlockType.DATABASE_LINK) FirstLine() else null }
            // **Una colonna, non un `Box`** (dal 12/11/2026, trovato dall'utente: "si sono
            // creati dei divider sopra i database"): il database disegna anche la riga alla
            // sua fine, e nel `Box` finiva sopra di lui, in cima. Lo stesso errore del 20/10
            // nelle voci della pagina.
            Column(
                modifier = Modifier
                    .padding(vertical = (look.spacingOf(child) / 2f).sp.toDpIn(density))
                    // Col mouse la riga ha i suoi sei puntini, accanto al suo testo (dal 12/11/2026).
                    .pickableAs(
                        "line:" + child.id,
                        lineOf = { _, c -> lineFirst?.boundsIn(c) ?: LineBounds(0f, minOf(c.size.height.toFloat(), lineHandleSpan), sidePaddingPx) },
                        nested = true,
                        textInset = sidePaddingPx
                    ) { child.id }
                    .then(if (emptyColumn && index == 0) Modifier.drawBehind { drawEmptyColumn(emptyLook) } else Modifier)
            ) {
                CompositionLocalProvider(LocalFirstLine provides lineFirst) {
                BlockRow(
                    block = child,
                    allBlocks = allBlocks,
                    depth = depth,
                    viewModel = viewModel,
                    factory = factory,
                    focusRequestBlockId = focusRequestBlockId,
                    formatRequest = formatRequest,
                    onFocusChanged = onFocusChanged,
                    onNavigateToPage = onNavigateToPage,
                    onNavigateToDatabase = onNavigateToDatabase,
                    onOpenBlockMenu = onOpenBlockMenu
                )
                }
            }
        }
    }
    if (column.type == BlockType.COLUMN && lines.isEmpty() && !LocalPageLocked.current) {
        Box(Modifier.fillMaxWidth().height(EMPTY_COLUMN_HEIGHT).clickable { viewModel.addFirstChild(column) })
    }
}

/** Tutte le righe del blocco [id] nel campo, dall'alto al basso: per trascinarci sopra un blocco (`BlockDrag.kt`). */
private fun runLineExtent(
    layout: TextLayoutResult,
    id: String,
    fieldText: String,
    runBlocks: List<BlockEntity>,
    ordinals: Map<String, Int>,
    viewModel: PageEditorViewModel
): LineBounds? {
    val index = runBlocks.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: return null
    val range = shownLineRanges(fieldText, runBlocks, ordinals, viewModel).getOrNull(index) ?: return null
    val last = layout.layoutInput.text.length
    return LineBounds(
        layout.getLineTop(layout.getLineForOffset(range.first.coerceIn(0, last))),
        layout.getLineBottom(layout.getLineForOffset(range.last.coerceIn(0, last)))
    )
}

/** Il blocco della riga del campo all'altezza [y], per il mouse. */
private fun runLineAt(
    layout: TextLayoutResult,
    y: Float,
    fieldText: String,
    runBlocks: List<BlockEntity>,
    ordinals: Map<String, Int>,
    viewModel: PageEditorViewModel
): String? {
    val offset = layout.getOffsetForPosition(Offset(0f, y))
    val ranges = shownLineRanges(fieldText, runBlocks, ordinals, viewModel)
    val index = ranges.indexOfFirst { offset <= it.last }.takeIf { it >= 0 } ?: ranges.lastIndex
    return runBlocks.getOrNull(index)?.id
}

/**
 * Di quanto vanno allungate le righe perché il pezzo di testo più grande ci
 * stia senza toccare quelle vicine: il suo corpo diviso 16, mai meno di 1.
 *
 * **Vale per tutto il gruppo di righe**, non per la riga sola: l'altezza di
 * riga del campo condiviso è fissa e unica (vedi `lineHeight` in
 * `MergedTextRunField`), e darla riga per riga vorrebbe dire uno
 * `ParagraphStyle` dentro la trasformazione — la strada che in passato ha
 * fatto smettere di disegnare il campo e cancellato del testo ("Strade già
 * tentate" nel README). Per questo una riga più grande delle altre sta in un
 * campo suo (dal 27/10/2026, vedi `PageEditorViewModel.lineLookOf`): prima
 * allungava tutte le righe del suo pezzo da 50.
 */
fun lineScaleFor(spans: List<RichTextSpan>): Float {
    // In sp come li scrive `fontStyleOf`: sul PC una grandezza scelta a mano vale di più (`chosenSizeSp`).
    val largest = spans.maxOfOrNull { span -> span.size?.takeIf { it != DEFAULT_PAGE_FONT_SIZE }?.let { chosenSizeSp(it).value } ?: DEFAULT_PAGE_FONT_SIZE.toFloat() }
        ?: DEFAULT_PAGE_FONT_SIZE.toFloat()
    return (largest / DEFAULT_PAGE_FONT_SIZE).coerceAtLeast(1f)
}

/**
 * **Quanto Compose ha aggiunto sopra la prima riga e sotto l'ultima di un
 * campo** (dal 27/10/2026). Quando un segno della prima o dell'ultima riga
 * sporge dalla riga — il pallino nascosto davanti a una riga rientrata,
 * l'accento di "É" — Compose allunga il testo di quel tanto, ma solo ai due
 * capi: dentro il campo no. Due pezzi di fila avevano così fra loro qualche
 * punto in più, che spariva quando la pagina li univa in un campo solo
 * (`PageEditorViewModel.keepVisibleTogether`): in "Fast notes" fra
 * `</div> <h1 class="hint hanzi">` e "BACK", e scorrendo lo spazio cambiava
 * da solo. Misurato sul telefono: righe di 68 pixel, la prima, "BACK", di 73.
 *
 * Le righe hanno tutte la stessa altezza (il `lineHeight` del campo), quindi
 * quello che la prima e l'ultima hanno in più è proprio l'aggiunta. Con una
 * riga sola non si sa quanto sta sopra e quanto sotto, e non si toglie niente.
 */
class TextEdges {
    var top = 0
        private set
    var bottom = 0
        private set

    fun measure(layout: TextLayoutResult, lineHeightPx: Float) {
        val last = layout.lineCount - 1
        if (last < 1) {
            top = 0
            bottom = 0
            return
        }
        top = (layout.getLineBottom(0) - layout.getLineTop(0) - lineHeightPx).roundToInt().coerceAtLeast(0)
        bottom = (layout.getLineBottom(last) - layout.getLineTop(last) - lineHeightPx).roundToInt().coerceAtLeast(0)
    }
}

/**
 * Toglie al campo quello che Compose gli ha aggiunto ai capi ([TextEdges]),
 * sopra se il pezzo continua quello di sopra ([top]) e sotto se continua in
 * quello di sotto ([bottom]): così due pezzi di fila si toccano come le righe
 * di un campo solo. Il campo sale di quanto si toglie sopra. Le misure le
 * scrive il campo stesso mentre si misura, qui dentro, quindi sono già quelle
 * di questo giro.
 */
fun Modifier.trimTextEdges(edges: TextEdges, top: Boolean, bottom: Boolean): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val cutTop = if (top) edges.top else 0
        val cutBottom = if (bottom) edges.bottom else 0
        layout(placeable.width, (placeable.height - cutTop - cutBottom).coerceAtLeast(0)) {
            placeable.place(0, -cutTop)
        }
    }

private val SCRIPT_SIZE = 0.7.em

// Il pedice scende meno di quanto l'apice sale: con lo spostamento
// standard (metà della sua altezza) il "2" di H₂O finiva sotto le gambe
// di g e p, staccato dalla parola — visto nel disegno di prova.
private val SUBSCRIPT_SHIFT = BaselineShift(-0.3f)

/**
 * Il disegno dei pulsanti pedice e apice nella barra Aa: una x con un 2
 * piccolo in basso a destra (pedice) o in alto a destra (apice), come in
 * Word e OneNote. Scritto e non un'icona, come B, I, U e S accanto.
 *
 * La x sta al centro, al posto di B e U, e il 2 si appoggia all'angolo:
 * scritti in una riga sola, il 2 in alto spingeva giù la x e i due
 * pulsanti non erano allineati fra loro né con gli altri.
 */
@Composable
fun ScriptGlyph(superscript: Boolean, description: String) {
    Box(
        modifier = Modifier
            .size(width = 24.dp, height = 26.dp)
            .semantics { contentDescription = description }
    ) {
        Text("x", color = NotionWhite, fontSize = 18.sp, modifier = Modifier.align(Alignment.Center))
        Text(
            "2",
            color = NotionWhite,
            fontSize = 11.sp,
            // Senza, il 2 si porta dietro l'altezza di riga del testo
            // normale (24) e resta a metà in tutti e due i pulsanti.
            lineHeight = 11.sp,
            modifier = Modifier.align(if (superscript) Alignment.TopEnd else Alignment.BottomEnd)
        )
    }
}

private fun spoilerStyle(revealed: Boolean): SpanStyle =
    if (revealed) {
        SpanStyle(background = SpoilerRevealed)
    } else {
        SpanStyle(color = Color.Transparent, background = SpoilerCover)
    }

/**
 * I quadratini delle caselle da spuntare, disegnati **sopra** il campo
 * di testo condiviso.
 *
 * Dentro un campo di testo non si possono mettere dei bottoni: il testo
 * riserva lo spazio con dei caratteri invisibili (`CHECKBOX_MARKER`) e
 * qui sopra ci si appoggia il quadratino vero, alla posizione che il
 * campo stesso dichiara per quel carattere. Così il quadratino resta
 * incollato alla sua riga anche quando il testo va a capo da solo o
 * cambia rientro, e ha una zona di tocco vera invece di essere un
 * carattere da centrare col dito.
 */
@Composable
private fun RunCheckboxOverlay(
    runBlocks: List<BlockEntity>,
    layout: TextLayoutResult?,
    onToggle: (BlockEntity) -> Unit
) {
    if (layout == null) return
    val density = LocalDensity.current

    // **Una fonte sola: il testo che è stato davvero misurato.**
    //
    // Prima le posizioni si ricalcolavano dal testo corrente del campo
    // e dai blocchi correnti, mentre le coordinate arrivavano dalla
    // misura del fotogramma precedente. Scrivendo, per un istante i due
    // non combaciavano: i quadratini sparivano e riapparivano spostati
    // di lato, ad ogni tasto. Cercando il marcatore dentro il testo che
    // il campo ha misurato, il disegno non può più sfasarsi.
    val laid = layout.layoutInput.text.text
    val markers = mutableListOf<Pair<Int, Int>>() // riga -> posizione del marcatore
    var lineIndex = 0
    var cursor = 0
    while (cursor <= laid.length) {
        val lineEnd = laid.indexOf('\n', cursor).let { if (it < 0) laid.length else it }
        val markerAt = laid.indexOf(CHECKBOX_MARKER, cursor)
        if (markerAt in cursor until lineEnd) markers.add(lineIndex to markerAt)
        lineIndex++
        cursor = lineEnd + 1
    }

    markers.forEach { (line, markerAt) ->
        val block = runBlocks.getOrNull(line) ?: return@forEach
        if (block.type != BlockType.CHECKBOX) return@forEach
        val box = runCatching { layout.getBoundingBox(markerAt) }.getOrNull()
            ?: return@forEach
        with(density) {
            // Centrato nel riquadro della riga. Funziona perché il
            // campo chiede al testo di **centrarsi** dentro la riga
            // alta (`lineHeightStyle`, vedi `CHECKBOX_LINE_HEIGHT`):
            // senza, lo spazio in più finirebbe tutto da una parte e il
            // quadratino scenderebbe rispetto alle lettere.
            Box(
                modifier = Modifier
                    .offset(
                        x = box.left.toDp(),
                        y = (box.top + (box.height - CHECKBOX_TOUCH_SIZE.toPx()) / 2).toDp()
                    )
                    .size(CHECKBOX_TOUCH_SIZE)
            ) {
                CheckboxMark(
                    checked = block.isChecked,
                    onToggle = { onToggle(block) }
                )
            }
        }
    }
}

/**
 * Un unico campo di testo condiviso per una sequenza consecutiva di
 * blocchi di testo scorrevole (paragrafi, titoli, elenchi puntati e
 * numerati). Selezionare e cancellare a cavallo di due blocchi qui è
 * semplicemente selezionare e cancellare del testo — lo stesso
 * meccanismo affidabile che gestisce qualsiasi modifica, non un
 * comportamento speciale da intercettare. onValueChange confronta le
 * righe prima/dopo con viewModel.updateRun, che decide se si tratta di
 * una modifica di testo semplice, di una divisione (Invio/incolla) o
 * di una fusione (backspace a inizio riga, o una selezione a cavallo
 * di più righe cancellata) — è quest'ultimo caso a far funzionare
 * backspace-tra-blocchi in modo nativo, senza intercettare nessun
 * tasto.
 *
 * Unica eccezione alla regola "il testo cambia, il resto segue": se il
 * backspace arriva a inizio di una riga che è un elemento di elenco, la
 * fusione viene rifiutata e al suo posto la riga perde il marcatore
 * (torna paragrafo), come in Notion. Il testo del campo torna quindi
 * quello di prima. Anche questo gesto è riconosciuto da come il testo è
 * cambiato, non da un evento-tasto: sulle righe dopo la prima perché
 * sparisce l'a-capo esattamente dov'è il cursore, sulla prima riga
 * perché sparisce il RUN_LEAD e il cursore finisce a zero — posizione
 * che nessun'altra azione può produrre.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun MergedTextRunField(
    runBlocks: List<BlockEntity>,
    /** Chi è questo campo: la chiave del gruppo, vedi `groupIntoRenderItems`. */
    runKey: String,
    /** I numeri delle righe di elenco numerato di questo campo. */
    ordinals: Map<String, Int>,
    /**
     * Questo pezzo continua quello di sopra, o quello di sotto continua lui
     * (vedi `PageEditorViewModel.chunkMembers`): lì niente margine, e le righe
     * scorrono come in un campo solo.
     */
    joinsAbove: Boolean = false,
    joinsBelow: Boolean = false,
    viewModel: PageEditorViewModel,
    focusRequestBlockId: String?,
    formatRequest: Pair<String, FormatType>?,
    onFocusChanged: (String, Boolean) -> Unit,
    /** Cosa fare con la voce scelta dal menu "/": lo sa la schermata, che può anche navigare. */
    onSlashAction: (BlockEntity, SlashAction) -> Unit
) {
    var fieldValue by remember(runKey) {
        mutableStateOf(TextFieldValue(runFieldText(runBlocks, viewModel), TextRange(1)))
    }
    var lastLineBlockId by remember(runKey) { mutableStateOf<String?>(null) }
    var fieldFocused by remember(runKey) { mutableStateOf(false) }
    // Il correttore: dove finisce quello che si è appena scritto (la parola
    // lì non si segna finché si scrive) e la parola toccata, col suo menu.
    var proofTyping by remember(runKey) { mutableStateOf<Int?>(null) }
    var proofMenu by remember(runKey) { mutableStateOf<Pair<MisspelledWord, IntRect>?>(null) }

    // Se il contenuto "ufficiale" (dal database, via runBlocks) è
    // diverso da quello che il campo mostra localmente, risincronizza —
    // serve per Annulla/Ripristina, che cambia i blocchi da fuori
    // rispetto a questo campo. Nel caso comune (l'utente scrive, il
    // giro di andata e ritorno dal database riporta lo stesso testo
    // appena scritto) non fa nulla, lasciando intatta la selezione
    // corrente.
    // Il testo che ci aspettiamo di ricevere indietro dal database, cioè
    // l'ultimo che abbiamo inviato noi. Serve a distinguere due casi che
    // altrimenti si confondono:
    //  - il database è semplicemente INDIETRO (stiamo ancora scrivendo,
    //    il salvataggio è in viaggio): quello che torna è vecchio, va
    //    ignorato, altrimenti sovrascriverebbe quello appena digitato —
    //    è questo che faceva "perdere" un Invio, costringendo a premerlo
    //    due volte
    //  - il database è cambiato DA FUORI (Annulla/Ripristina, che
    //    riscrive i blocchi da un'altra parte): lì dobbiamo davvero
    //    adeguarci
    var pendingText by remember(runKey) { mutableStateOf<String?>(null) }

    // Annulla/Ripristina riscrive i blocchi da fuori: qualunque nostra
    // modifica in sospeso va scartata, altrimenti bloccherebbe
    // l'adeguamento al nuovo contenuto.
    val externalChangeTick by viewModel.externalChangeTick.collectAsStateWithLifecycle()
    LaunchedEffect(externalChangeTick) {
        pendingText = null
    }

    // Richiesta di formattazione dalla barra Aa: si applica se riguarda
    // uno qualsiasi dei blocchi di questo gruppo, usando la selezione
    // corrente del campo — che la barra non può conoscere, vivendo
    // fuori da qui.
    // **Dove sta la selezione, detto al ViewModel mentre si usa.**
    // Il pennello apre un pannello e il campo perde il fuoco: in quel
    // momento la selezione si chiude e non c'è più niente da colorare.
    // Segnalandola man mano, il pennello può metterla da parte
    // nell'istante in cui lo si tocca. Vedi `reportSelection`.
    LaunchedEffect(fieldValue.selection, fieldFocused, runBlocks) {
        if (!fieldFocused) return@LaunchedEffect
        val sel = fieldValue.selection
        viewModel.reportSelection(
            runBlocks.map { it.id },
            (sel.min - 1).coerceAtLeast(0),
            (sel.max - 1).coerceAtLeast(0)
        )
    }

    LaunchedEffect(formatRequest) {
        if (formatRequest != null && runBlocks.any { it.id == formatRequest.first }) {
            val sel = fieldValue.selection
            viewModel.applyFormatToRun(
                runBlocks.map { it.id },
                (sel.min - 1).coerceAtLeast(0),
                (sel.max - 1).coerceAtLeast(0),
                formatRequest.second
            )
            viewModel.consumeFormatRequest()
        }
    }

    // **Un pezzo vicino entra in questo, la selezione resta** (dal 19/10/2026,
    // vedi `PageEditorViewModel.keepVisibleTogether`): le sue righe arrivano in
    // fondo o in cima, e il cursore o la selezione restano sulle stesse
    // lettere. Il campo le prende nello stesso disegno: aspettando il giro
    // qui sotto, per un istante le righe del pezzo sparito non c'erano più, e
    // la selezione si chiudeva.
    val officialNow = remember(runBlocks) { runFieldText(runBlocks, viewModel) }
    val shownFirstLine = remember(runKey) { arrayOf(runBlocks.first().id) }
    if (pendingText == null && officialNow.length > fieldValue.text.length) {
        val firstLine = runBlocks.first().id
        val body = bodyOf(fieldValue.text)
        if (firstLine == shownFirstLine[0] && officialNow.startsWith(fieldValue.text + "\n")) {
            fieldValue = TextFieldValue(officialNow, fieldValue.selection)
        } else if (firstLine != shownFirstLine[0] && runBlocks.any { it.id == shownFirstLine[0] } &&
            officialNow.startsWith(RUN_LEAD) && officialNow.endsWith("\n" + body)
        ) {
            val shift = officialNow.length - fieldValue.text.length
            val sel = fieldValue.selection
            fieldValue = TextFieldValue(officialNow, TextRange(sel.start + shift, sel.end + shift))
        }
    }
    // **E delle righe ne escono** (dal 27/10/2026): quelle che appaiono
    // diverse da quella col cursore, sopra o sotto (vedi
    // `PageEditorViewModel.keepVisibleTogether`). Il cursore resta sulle stesse
    // lettere: uscendo le righe di sopra, torna indietro di quanto erano lunghe.
    if (pendingText == null && officialNow.length < fieldValue.text.length) {
        val firstLine = runBlocks.first().id
        val sel = fieldValue.selection
        if (firstLine == shownFirstLine[0] && fieldValue.text.startsWith(officialNow + "\n")) {
            val end = officialNow.length
            fieldValue = TextFieldValue(officialNow, TextRange(sel.start.coerceAtMost(end), sel.end.coerceAtMost(end)))
        } else if (firstLine != shownFirstLine[0] && runBlocks.none { it.id == shownFirstLine[0] } &&
            officialNow.startsWith(RUN_LEAD) && fieldValue.text.endsWith("\n" + bodyOf(officialNow))
        ) {
            val shift = fieldValue.text.length - officialNow.length
            fieldValue = TextFieldValue(
                officialNow,
                TextRange(
                    (sel.start - shift).coerceAtLeast(RUN_LEAD.length),
                    (sel.end - shift).coerceAtLeast(RUN_LEAD.length)
                )
            )
        }
    }
    shownFirstLine[0] = runBlocks.first().id

    // **Il testo di un link cambiato dalla sua finestrella** (dal 27/10/2026,
    // vedi `PageEditorViewModel.replaceLinkText`): arriva dal database come
    // un Annulla, e il cursore va in fondo al link invece di restare dove
    // finiva il testo di prima.
    var linkTextCaret by remember(runKey) { mutableStateOf<PageEditorViewModel.LinkTextChange?>(null) }
    val linesNow by rememberUpdatedState(runBlocks)
    LaunchedEffect(runKey) {
        viewModel.linkTextChanges.collect { change ->
            if (linesNow.any { it.id == change.blockId }) linkTextCaret = change
        }
    }

    LaunchedEffect(runBlocks) {
        val official = runFieldText(runBlocks, viewModel)
        when {
            // È arrivato indietro esattamente ciò che avevamo inviato:
            // siamo allineati, non c'è più nulla in sospeso.
            official == pendingText -> pendingText = null
            // C'è ancora una nostra modifica in viaggio e il database
            // non la riflette: quello che è arrivato è vecchio, ignoralo.
            pendingText != null -> {}
            // Nessuna modifica nostra in sospeso e il testo è diverso:
            // è cambiato davvero da fuori, adeguiamoci.
            official != fieldValue.text -> {
                val change = linkTextCaret
                val line = change?.let { c -> runBlocks.indexOfFirst { it.id == c.blockId } } ?: -1
                val linkEnd = if (change != null && line >= 0 && viewModel.spansOf(runBlocks[line]) == change.spans) {
                    linkTextCaret = null
                    var pos = RUN_LEAD.length
                    for (i in 0 until line) pos += viewModel.plainTextOf(runBlocks[i]).length + 1
                    pos + change.caret
                } else {
                    null
                }
                val clampedCursor = (linkEnd ?: fieldValue.selection.end).coerceIn(1, official.length)
                fieldValue = TextFieldValue(official, TextRange(clampedCursor))
            }
        }
    }

    val focusRequester = remember { FocusRequester() }
    val focusPark = LocalFocusPark.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val imeVisible = isKeyboardVisible()

    // Serve a sapere dove sono finiti sullo schermo i numeri degli
    // elenchi, per poterli rendere toccabili (vedi più sotto), e dove
    // si trova il cursore per far scorrere la pagina fin lì.
    var textLayout by remember(runKey) { mutableStateOf<TextLayoutResult?>(null) }

    // Su quale numero è stato tenuto premuto: apre il menu della lista.
    var numberMenuFor by remember(runKey) { mutableStateOf<String?>(null) }

    // **La riga appena spostata con le frecce si illumina** e il velo
    // svanisce: vedi `PageEditorViewModel.movedBlock`.
    val movedBlock by viewModel.movedBlock.collectAsStateWithLifecycle()
    val moveFlash = remember { Animatable(0f) }
    val moveFlashColor = MaterialTheme.colorScheme.primary
    LaunchedEffect(movedBlock) {
        val moved = movedBlock ?: return@LaunchedEffect
        if (runBlocks.none { it.id == moved.blockId }) return@LaunchedEffect
        moveFlash.snapTo(1f)
        moveFlash.animateTo(0f, tween(MOVE_FLASH_MS))
    }

    // Il menu "/". `slashAnchor` è dove sta la barra dentro il testo:
    // finché c'è, quello che si scrive dopo filtra la lista. Si chiude
    // da solo quando la barra sparisce, quando il cursore va prima di
    // lei o quando si scrive uno spazio — perché a quel punto non si
    // sta più scegliendo un blocco, si sta scrivendo.
    var slashAnchor by remember(runKey) { mutableStateOf<Int?>(null) }
    var slashQuery by remember(runKey) { mutableStateOf("") }
    // **Il menu "/" con la tastiera** (dal 12/11/2026, trovato dall'utente sul
    // PC: "/two" e Invio andava a capo invece di fare le colonne): come su
    // Notion, Invio sceglie la voce evidenziata, le frecce la cambiano, Esc
    // chiude. Si riparte dalla prima a ogni lettera.
    var slashHighlight by remember(runKey) { mutableStateOf(0) }
    LaunchedEffect(slashQuery, slashAnchor) { slashHighlight = 0 }
    val slashCatalog = slashEntries()
    val slashChoosable = remember(slashQuery, slashCatalog, AppSettings.language) {
        slashShown(slashCatalog, slashQuery).filter { it.enabled }
    }

    // Il menu "@" (dal 03/11/2026, vedi `Mentions.kt`): come quello "/",
    // `mentionAnchor` è dove sta la chiocciola e quello che segue cerca le
    // pagine. `mentionDateFor` tiene chiocciola e ricerca mentre si sceglie
    // il giorno col calendario, che si porta via il fuoco.
    var mentionAnchor by remember(runKey) { mutableStateOf<Int?>(null) }
    var mentionQuery by remember(runKey) { mutableStateOf("") }
    var mentionDateFor by remember(runKey) { mutableStateOf<Pair<Int, String>?>(null) }

    val caretIntoView = remember { BringIntoViewRequester() }

    // Fa scorrere la pagina dietro al cursore mentre si scrive.
    //
    // Non è ridondante: un campo di testo che **cresce in altezza**
    // invece di scorrere al proprio interno chiede di essere reso
    // visibile solo quando prende il fuoco, non ad ogni spostamento del
    // cursore. Questo campo è così, ed è alto quanto tutto il gruppo di
    // testo che contiene: scrivendo in fondo a una pagina lunga il
    // cursore finiva dietro la tastiera e la pagina restava ferma.
    //
    // Si chiede di mostrare il rettangolo del cursore, non l'intero
    // campo: quando il campo è più alto dello schermo, "mostra il
    // campo" non vuol dire niente.
    //
    // **Col posto dei pulsanti sospesi sopra** (dal 29/09/2026): portato
    // proprio al bordo dello schermo, il cursore finiva dietro indietro e
    // barra laterale. Succedeva con la freccia su che porta una riga sopra
    // un database: la pagina la seguiva, ma la lasciava lì sotto.
    val barClearancePx = with(LocalDensity.current) { FLOATING_BAR_CLEARANCE.toPx() }
    val caretMarginPx = with(LocalDensity.current) { CARET_KEYBOARD_MARGIN.toPx() }
    // **La pagina segue il cursore, non tutto il campo** (dal 19/10/2026):
    // vedi `CaretInView`. Letto ogni volta da capo, così è sempre quello di adesso.
    val caretInView = remember(runKey) { CaretInView() }
    // **Le righe intere** (dal 19/10/2026, "Full line view"): il campo scorre
    // di lato con gli altri, e dice fin dove arrivano le sue righe che si
    // vedono, così la pagina scorre fin lì e non oltre. Vedi `FullLineView.kt`.
    val sideLane = rememberSideLane()
    val sidePadPx = with(LocalDensity.current) { PAGE_SIDE_PADDING.toPx() }
    val sideTopPx = with(LocalDensity.current) { (if (joinsAbove) 0.dp else 4.dp).toPx() }
    if (sideLane != null) {
        sideLane.rightEdge = edge@{ top, bottom ->
            val layout = textLayout ?: return@edge 0f
            if (layout.lineCount == 0) return@edge 0f
            val first = layout.getLineForVerticalPosition((top - sideTopPx).coerceAtLeast(0f))
            val last = layout.getLineForVerticalPosition((bottom - sideTopPx).coerceAtLeast(0f))
            var right = 0f
            for (line in first..last) right = maxOf(right, layout.getLineRight(line))
            val end = sidePadPx + right
            // Righe che stanno nello spazio di sempre: niente da scorrere.
            if (end <= sideLane.viewport - sidePadPx) 0f else end
        }
    }
    caretInView.caretRect = caret@{
        val layout = textLayout ?: return@caret null
        val body = bodyOf(fieldValue.text)
        val caretInBody = (fieldValue.selection.end - 1).coerceIn(0, body.length)
        val prefixes = runPrefixes(body.split('\n'), runBlocks, ordinals, viewModel)
        val displayed = displayedCaretOffset(body, caretInBody, prefixes)
            .coerceIn(0, layout.layoutInput.text.length)
        val caret = layout.getCursorRect(displayed)
        Rect(caret.left, caret.top - barClearancePx, caret.right + 1f, caret.bottom + caretMarginPx)
    }
    LaunchedEffect(fieldValue.selection, textLayout, imeVisible) {
        // lastLineBlockId è valorizzato solo mentre questo campo ha il
        // fuoco: senza, faremmo scorrere la pagina anche per i campi su
        // cui nessuno sta scrivendo.
        if (lastLineBlockId == null || textLayout == null) return@LaunchedEffect
        // Quello che si porta in vista lo decide `followCaretOnly`: il cursore.
        caretIntoView.bringIntoView()
    }

    LaunchedEffect(focusRequestBlockId) {
        if (runBlocks.any { it.id == focusRequestBlockId }) {
            focusRequester.requestFocus()
            // **Sempre, non solo se la tastiera risulta chiusa.**
            // Quando il fuoco arriva qui dopo che è stato tolto al
            // campo di prima — è quello che succede quando una casella
            // da spuntare cambia specie — la tastiera sta ancora
            // scomparendo, e `imeVisible` dice "aperta" mentre in
            // realtà se ne sta andando: la richiesta veniva saltata e
            // la tastiera spariva in faccia a chi stava scrivendo.
            // Chiederla quando è già aperta non fa niente.
            keyboardController?.show()
            // Posiziona il cursore alla fine del blocco richiesto (il
            // caso comune: appena creato da uno split, o appena
            // raggiunto risalendo con focusLastBlock).
            val idx = runBlocks.indexOfFirst { it.id == focusRequestBlockId }
            if (idx >= 0) {
                var pos = 1
                for (i in 0 until idx) pos += viewModel.plainTextOf(runBlocks[i]).length + 1
                val lineLength = viewModel.plainTextOf(runBlocks[idx]).length
                // Chi ha chiesto il fuoco può aver chiesto anche **dove**:
                // un toggle appena tornato testo vuole il cursore a inizio
                // riga, dove si era premuto il backspace. Senza, andrebbe
                // in fondo e il backspace successivo mangerebbe l'ultima
                // lettera invece di unire la riga a quella sopra.
                val wanted = viewModel.pendingCaret.value
                    ?.takeIf { it.first == focusRequestBlockId }
                    ?.second
                pos += wanted?.coerceIn(0, lineLength) ?: lineLength
                if (wanted != null) viewModel.consumePendingCaret()
                fieldValue = fieldValue.copy(selection = TextRange(pos))
            }
            viewModel.consumeFocusRequest()
        }
    }

    fun lineIndexAt(text: String, offset: Int): Int {
        var pos = 0
        val lines = text.split('\n')
        for ((i, line) in lines.withIndex()) {
            if (offset <= pos + line.length) return i
            pos += line.length + 1
        }
        return (lines.size - 1).coerceAtLeast(0)
    }

    // Su quale blocco sta il cursore va ricalcolato anche quando cambia
    // la lista dei blocchi, non solo quando si scrive. Subito dopo un
    // Invio la riga nuova esiste già nel campo ma non ancora in
    // runBlocks: in quell'istante non si riesce ad associarla a nessun
    // blocco, e senza questo ricalcolo il "blocco a fuoco" resterebbe
    // quello di prima — così i pulsanti della barra (rientro, sposta su
    // e giù, elimina) agirebbero sulla riga sbagliata.
    LaunchedEffect(runBlocks, fieldValue.selection) {
        if (lastLineBlockId == null) return@LaunchedEffect
        val lineIndex = lineIndexAt(bodyOf(fieldValue.text), fieldValue.selection.end - 1)
        val id = runBlocks.getOrNull(lineIndex)?.id ?: return@LaunchedEffect
        if (id != lastLineBlockId) {
            lastLineBlockId?.let { onFocusChanged(it, false) }
            onFocusChanged(id, true)
            lastLineBlockId = id
        }
    }

    // **La selezione arrivata in fondo a un pezzo va avanti** (dal 19/10/2026,
    // trovato dall'utente in Permanent notes: trascinando, la selezione si
    // fermava a "Kor", l'ultima riga di un pezzo). Di solito non serve, le
    // righe sullo schermo sono già un campo solo (vedi
    // `PageEditorViewModel.keepVisibleTogether`); serve se si comincia a
    // selezionare appena la pagina si ferma. Solo col dito sul testo: le
    // posizioni del campo non cambiano e il gesto continua. Col dito su una
    // maniglia no, che il campo la perderebbe e la lente resterebbe lì.
    val pageFinger = LocalPageFingerDown.current
    LaunchedEffect(fieldValue.selection, fieldFocused, runBlocks, joinsBelow) {
        val sel = fieldValue.selection
        if (!fieldFocused || sel.collapsed || !joinsBelow || pageFinger?.value != true) return@LaunchedEffect
        val body = bodyOf(fieldValue.text)
        val lines = body.split('\n')
        if (lines.size != runBlocks.size) return@LaunchedEffect
        val to = (sel.max - RUN_LEAD.length).coerceAtLeast(0)
        if (to >= body.length - lines.last().length) viewModel.joinChunkAfter(runBlocks.last().id)
    }

    // Quali righe di questo campo si vedono, per la pagina che tiene insieme
    // quelle sullo schermo (vedi `VisibleRunLines`).
    val visibleLines = LocalVisibleRunLines.current
    val latestRunBlocks = rememberUpdatedState(runBlocks)
    DisposableEffect(visibleLines, runKey) {
        visibleLines?.providers?.put(runKey) { top, bottom ->
            val layout = textLayout
            val blocks = latestRunBlocks.value
            if (layout == null || layout.lineCount == 0 || blocks.isEmpty()) {
                emptyList()
            } else {
                val shown = layout.layoutInput.text.text
                fun blockOf(visualLine: Int): Int {
                    val upTo = layout.getLineStart(visualLine).coerceAtMost(shown.length)
                    var count = 0
                    for (k in 0 until upTo) if (shown[k] == '\n') count++
                    return count.coerceIn(0, blocks.lastIndex)
                }
                val first = blockOf(layout.getLineForVerticalPosition((top - sideTopPx).coerceAtLeast(0f)))
                val last = blockOf(layout.getLineForVerticalPosition((bottom - sideTopPx).coerceAtLeast(0f)))
                blocks.subList(first, last + 1).map { it.id }
            }
        }
        // Dove sta una riga, per il salto dei collegamenti dentro la pagina.
        visibleLines?.lineTops?.put(runKey) { blockId ->
            val line = latestRunBlocks.value.indexOfFirst { it.id == blockId }
            if (line < 0) null else lineTopIn(textLayout, line)?.plus(sideTopPx)
        }
        onDispose {
            visibleLines?.providers?.remove(runKey)
            visibleLines?.lineTops?.remove(runKey)
        }
    }

    // Scelta una voce del menu "/", la barra e quello che le sta
    // dietro spariscono dal testo — è un comando, non roba scritta — e
    // poi si applica al blocco di quella riga.
    fun applySlashEntry(entry: SlashEntry) {
        val anchor = slashAnchor ?: return
        val body = bodyOf(fieldValue.text)
        if (anchor >= body.length) {
            slashAnchor = null
            return
        }
        val end = (anchor + 1 + slashQuery.length).coerceAtMost(body.length)
        val cleaned = body.removeRange(anchor, end)
        val lineIndex = lineIndexAt(cleaned, anchor)
        val block = runBlocks.getOrNull(lineIndex)

        val cleanedText = RUN_LEAD + cleaned
        fieldValue = TextFieldValue(cleanedText, TextRange(anchor + RUN_LEAD.length))
        pendingText = cleanedText
        viewModel.updateRun(
            runBlocks.map { it.id },
            cleaned.split('\n'),
            body.split('\n'),
            lineIndex
        )
        slashAnchor = null
        slashQuery = ""
        // Dopo la scrittura del testo, non prima: così l'ordine in cui
        // arrivano al database è quello giusto e il cambio di tipo non
        // viene riscritto sopra dal salvataggio del testo.
        if (block != null) onSlashAction(block, entry.action)
    }

    /** I tasti del menu "/" aperto: vero se il tasto era suo (vedi `slashHighlight`). */
    fun slashKey(event: KeyEvent): Boolean {
        if (slashAnchor == null || event.type != KeyEventType.KeyDown) return false
        return when (event.key) {
            Key.Enter, Key.NumPadEnter -> slashChoosable.getOrNull(slashHighlight)?.let { applySlashEntry(it); true } ?: false
            Key.DirectionDown -> true.also { slashHighlight = (slashHighlight + 1).coerceAtMost((slashChoosable.size - 1).coerceAtLeast(0)) }
            Key.DirectionUp -> true.also { slashHighlight = (slashHighlight - 1).coerceAtLeast(0) }
            Key.Escape -> true.also { slashAnchor = null }
            else -> false
        }
    }

    // Il corpo più grande scelto con la barra Aa in queste righe: tutte le
    // righe del gruppo si allungano quanto basta per lui (vedi `lineScaleFor`).
    val lineScale = remember(runBlocks) { lineScaleFor(runBlocks.flatMap { viewModel.spansOf(it) }) }

    // **"Paragraph"** (dal 21/10/2026): lo spazio fra i blocchi è lo stesso
    // per tutte le righe del campo (un gruppo con uno spazio suo è un campo a
    // sé, vedi `spacingStartsOf`), e va nell'altezza di riga dell'intero
    // campo, l'unico posto dove il README dice che le misure si toccano
    // senza rischi. Lo spazio dopo i marcatori: vedi `MarkerGapScale`.
    val paragraphLook = LocalParagraphLook.current
    val blockSpacing = runBlocks.firstOrNull()?.let { paragraphLook.spacingOf(it) } ?: paragraphLook.pageSpacing
    val gapScale = rememberMarkerGapScale(paragraphLook)

    // Il correttore (vedi `ProofingUi.kt`): le parole sbagliate di ogni riga.
    val proofing = LocalProofing.current
    val misspellings = rememberMisspellings(if (proofing == null) emptyList() else bodyOf(fieldValue.text).split('\n'))
    val pageLocked = LocalPageLocked.current
    val openLink = rememberLinkOpener()
    // Le righe col pin di questo gruppo: il segnalibro nel margine (vedi `PageAnchors.kt`).
    val pinnedLines = remember(runBlocks) { runBlocks.indices.filter { runBlocks[it].attrs.linkTarget } }

    // Una parola scelta dal menu del correttore prende il posto di quella
    // sbagliata, come se la si fosse scritta: stesso giro di `applySlashEntry`.
    fun replaceMisspelled(word: MisspelledWord, with: String) {
        val body = bodyOf(fieldValue.text)
        val lines = body.split('\n')
        val line = lines.getOrNull(word.line) ?: return
        if (word.end > line.length) return
        val newLines = lines.toMutableList().also { it[word.line] = line.replaceRange(word.start, word.end, with) }
        val lineStart = lines.take(word.line).sumOf { it.length + 1 }
        val newText = RUN_LEAD + newLines.joinToString("\n")
        fieldValue = TextFieldValue(newText, TextRange(RUN_LEAD.length + lineStart + word.start + with.length))
        pendingText = newText
        proofTyping = null
        viewModel.updateRun(runBlocks.map { it.id }, newLines, lines, word.line)
    }

    // I blocchi scelti col mouse, da velare (vedi `BlockSelection.kt`).
    val selectionLook = LocalBlockSelection.current
    // Passati ai blocchi interi, il testo selezionato prima non resta disegnato sotto il velo.
    val blocksChosen by remember(selectionLook) { derivedStateOf { selectionLook?.selected?.value?.isNotEmpty() == true } }
    LaunchedEffect(blocksChosen) {
        if (blocksChosen && !fieldValue.selection.collapsed) fieldValue = fieldValue.copy(selection = TextRange(fieldValue.selection.max))
    }

    // L'altezza di ogni riga del campo: vedi il commento sul `lineHeight`
    // del `textStyle` qui sotto.
    val lineHeight = (
        if (runBlocks.any { it.type == BlockType.CHECKBOX }) CHECKBOX_LINE_HEIGHT else RUN_LINE_HEIGHT
        ).times(lineScale).value.plus(blockSpacing).sp
    val lineHeightPx = with(LocalDensity.current) { ceil(lineHeight.toPx()) }
    val edges = remember { TextEdges() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Prima dei margini: le righe scorrono fino al bordo dello schermo.
            .sideLane(sideLane)
            .padding(
                start = PAGE_SIDE_PADDING,
                end = PAGE_SIDE_PADDING,
                top = if (joinsAbove) 0.dp else 4.dp,
                bottom = if (joinsBelow) 0.dp else 4.dp
            )
            // Due pezzi di fila si toccano come le righe di un campo solo.
            .trimTextEdges(edges, top = joinsAbove, bottom = joinsBelow)
            // Dopo i margini: le coordinate sono quelle del campo.
            .followCaretOnly(caretInView)
    ) {
    // Il campo incolla il testo e basta: `formatPaste` ci rimette link e stili (vedi `PasteLinks.kt`).
    val linkClipboard = LocalClipboard.current as? LinkClipboard
    val mergedTransformation = MergedRunVisualTransformation(
        runBlocks,
        ordinals,
        viewModel,
        // Il cursore serve solo a scoprire gli spoiler che tocca, e
        // senza fuoco non c'è nessun cursore da guardare.
        caret = if (fieldFocused) {
            (fieldValue.selection.min - 1).coerceAtLeast(0)..
                (fieldValue.selection.max - 1).coerceAtLeast(0)
        } else {
            null
        },
        proof = proofing?.let { { line: String -> misspellings[line].orEmpty() } },
        typingAt = if (fieldFocused) proofTyping else null,
        gapScale = gapScale
    )
    PageTextMenuArea {
    AutoCaps {
        BasicTextField(
            value = fieldValue,
            onTextLayout = {
                textLayout = it
                edges.measure(it, lineHeightPx)
            },
            onValueChange = { typedValue ->
                val pastedValue = linkClipboard?.formatPaste(fieldValue, typedValue) ?: typedValue
                // I link di un incolla (vedi `PasteLinks.kt`): si tengono da
                // parte, e il campo resta col testo e basta.
                val pasted = pastedValue.pastedFormat(RUN_LEAD.length)
                val newValue = pastedValue.withoutPastedLinks().leadOutOfSelection()
                val oldText = fieldValue.text
                val oldBody = bodyOf(oldText)
                proofTyping = if (newValue.text != oldText) newValue.selection.end - RUN_LEAD.length else null
                val newText = newValue.text
                val caretRaw = newValue.selection.end

                // Backspace a inizio della PRIMA riga. L'unico carattere che
                // può essere sparito è l'a-capo nascosto in testa, e il
                // cursore resta a zero — posizione che nessun'altra azione
                // può produrre, perché la mappatura non ci porta mai il
                // cursore. Il testo dei blocchi non è cambiato: rimettiamo
                // l'a-capo e togliamo il marcatore, se c'è.
                val atFirstLineStart = newValue.selection.collapsed &&
                    caretRaw == 0 &&
                    oldText.startsWith(RUN_LEAD) &&
                    newText == oldBody

                if (atFirstLineStart) {
                    val outcome = viewModel.backspaceAtLineStartDetailed(runBlocks.map { it.id }, 0)
                    if (outcome == BackspaceOutcome.REMOVED_CLOSE_KEYBOARD) {
                        // La riga vuota sotto un divisore se ne va e non c'è
                        // un'altra riga in cui scrivere: il campo lascia il
                        // fuoco prima di sparire (altrimenti l'app si chiude,
                        // vedi sotto), e la tastiera si chiude.
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                    if (outcome == BackspaceOutcome.MERGED_INTO_ISLAND) {
                        // Questa riga è appena finita dentro la casella di
                        // sopra: sparisce, e con lei questo campo. Il fuoco
                        // si posa sul campo invisibile prima che succeda,
                        // altrimenti l'app si chiude (`LayoutCoordinate ...
                        // isAttached`). Era il caso più facile da incontrare:
                        // riga vuota sotto una casella, backspace.
                        focusPark?.requestFocus()
                    }
                    fieldValue = TextFieldValue(oldText, TextRange(1))
                } else {
                    // L'a-capo iniziale dev'esserci sempre. Nei casi normali
                    // c'è già e passiamo oltre il valore della tastiera così
                    // com'è, senza toccarlo — importante per non disturbare
                    // la composizione dell'IME.
                    val normalizedText = if (newText.startsWith(RUN_LEAD)) newText else RUN_LEAD + newText
                    val shift = normalizedText.length - newText.length
                    val newBody = bodyOf(normalizedText)
                    val caret = (caretRaw + shift - 1).coerceIn(0, newBody.length)

                    // **"/notification 22.10.2026 17:41 Buttare l'immondizia"
                    // e Invio**: la riga diventa una notifica con quel testo, e
                    // si continua a scrivere nella riga nuova sotto. Il comando
                    // sparisce e l'a-capo non entra nel testo: la riga nuova la
                    // fa `makeNotification`. Come per il menu "/", prima il
                    // testo scritto, poi il cambio di tipo.
                    val notification = notificationOnEnter(oldBody, newBody, caret)
                    val notificationBlock = notification?.let { runBlocks.getOrNull(it.first) }
                    if (notification != null && notificationBlock != null) {
                        val (lineIndex, command) = notification
                        val oldLines = oldBody.split('\n')
                        val cleanedLines = oldLines.toMutableList().also { it[lineIndex] = command.text }
                        val lineStart = cleanedLines.take(lineIndex).sumOf { it.length + 1 }
                        val cleanedText = RUN_LEAD + cleanedLines.joinToString("\n")
                        fieldValue = TextFieldValue(cleanedText, TextRange(RUN_LEAD.length + lineStart + command.text.length))
                        pendingText = cleanedText
                        slashAnchor = null
                        slashQuery = ""
                        viewModel.updateRun(runBlocks.map { it.id }, cleanedLines, oldLines, lineIndex)
                        focusPark?.requestFocus()
                        viewModel.makeNotification(notificationBlock, command)
                        NotificationPermission.request()
                        return@BasicTextField
                    }

                    // Il menu "/": si apre appena si scrive la barra, e da
                    // lì in poi quello che si continua a scrivere filtra la
                    // lista. Si chiude quando la barra non c'è più, quando
                    // il cursore torna prima di lei o quando arriva uno
                    // spazio: a quel punto non si sta più scegliendo un
                    // blocco, si sta scrivendo una frase che contiene "/".
                    // Solo a inizio riga o dopo uno spazio (dal 12/11/2026, come su
                    // Notion e come la "@"): da quando Invio sceglie la voce, "e/o" o
                    // "km/h" con Invio in fondo avrebbero cambiato la riga.
                    val justTypedSlash = newBody.length == oldBody.length + 1 &&
                        caret >= 1 && caret <= newBody.length && newBody[caret - 1] == '/' &&
                        (caret == 1 || newBody[caret - 2].isWhitespace())
                    if (justTypedSlash) {
                        slashAnchor = caret - 1
                        slashQuery = ""
                    } else {
                        val anchor = slashAnchor
                        if (anchor != null) {
                            val stillOpen = anchor < newBody.length &&
                                newBody[anchor] == '/' &&
                                caret > anchor
                            val typed = if (stillOpen) {
                                newBody.substring(anchor + 1, caret.coerceAtMost(newBody.length))
                            } else {
                                ""
                            }
                            // Uno spazio chiude il menu solo se non c'è più una voce che
                            // gli somiglia (dal 12/11/2026): "/create two columns" e
                            // Invio, come su Notion, fa le colonne.
                            val spaceEnds = ' ' in typed && slashShown(slashCatalog, typed).none { it.enabled }
                            if (!stillOpen || '\n' in typed || typed.startsWith(' ') || spaceEnds) {
                                slashAnchor = null
                                slashQuery = ""
                            } else {
                                slashQuery = typed
                            }
                        }
                    }

                    // Il menu "@": si apre con la chiocciola a inizio riga o
                    // dopo uno spazio, e si chiude come quello "/" (vedi
                    // `mentionQueryAt`); il nome cercato può avere spazi.
                    if (mentionJustTyped(oldBody, newBody, caret)) {
                        mentionAnchor = caret - 1
                        mentionQuery = ""
                    } else {
                        mentionAnchor?.let { anchor ->
                            val typed = mentionQueryAt(newBody, anchor, caret)
                            if (typed == null) {
                                mentionAnchor = null
                                mentionQuery = ""
                            } else {
                                mentionQuery = typed
                            }
                        }
                    }

                    // "5." scritto da solo a inizio blocco: diventa un elenco
                    // numerato che parte da quel numero, e il testo digitato
                    // sparisce perché da lì in poi il numero lo disegna la
                    // lista.
                    // `||coperto||`: chiusa la seconda coppia di barre, le
                    // quattro barre spariscono — erano un comando, non roba
                    // scritta — e quello che stava in mezzo resta coperto.
                    // Il cursore si ferma subito dopo, in chiaro.
                    val spoiler = spoilerJustClosed(oldBody, newBody, caret)
                    var spoilerBody: String? = null
                    var spoilerCaret = 0
                    if (spoiler != null) {
                        // Prima la coppia di chiusura, poi quella di
                        // apertura: togliendo l'apertura per prima, tutte le
                        // posizioni più avanti scalerebbero di due.
                        val stripped = newBody
                            .removeRange(spoiler.contentEnd, spoiler.contentEnd + 2)
                            .removeRange(spoiler.open, spoiler.open + 2)
                        val from = spoiler.open
                        val to = spoiler.contentEnd - 2
                        val lineStart = stripped.lastIndexOf('\n', from - 1) + 1
                        val lineEnd = stripped.indexOf('\n', from)
                            .let { if (it < 0) stripped.length else it }
                        val applied = viewModel.applySpoilerAtLine(
                            runBlocks.map { it.id },
                            stripped.take(lineStart).count { it == '\n' },
                            stripped.substring(lineStart, lineEnd),
                            from - lineStart,
                            to - lineStart
                        )
                        if (applied) {
                            spoilerBody = stripped
                            spoilerCaret = to
                        }
                    }

                    val listPrefix = numberedListPrefixJustTyped(oldBody, newBody, caret)
                    val listStarted = listPrefix != null &&
                        viewModel.startNumberedListAtLine(
                            runBlocks.map { it.id },
                            listPrefix.lineIndex,
                            listPrefix.startNumber
                        )

                    // **"---" da solo in una riga di testo fa un divisore**
                    // (dal 14/10/2026, chiesto dall'utente, come su Notion).
                    val dividerLine = if (spoiler == null && !listStarted) {
                        dividerShortcutJustTyped(oldBody, newBody, caret)
                    } else {
                        null
                    }
                    val dividerBlock = dividerLine?.let { viewModel.dividerShortcutBlock(runBlocks.map { b -> b.id }, it.lineIndex) }

                    // Backspace a inizio di una riga successiva: lì il testo
                    // cambia davvero (sparisce l'a-capo sopra). Se la riga ha
                    // ancora un marcatore o un rientro da perdere, la fusione
                    // va rifiutata in favore di quello.
                    val backspacedLine = backspacedLineStart(oldBody, newBody, caret)
                    val stepUndone = backspacedLine > 0 &&
                        viewModel.backspaceAtLineStartDetailed(
                            runBlocks.map { it.id },
                            backspacedLine
                        ) != BackspaceOutcome.NOTHING

                    if (spoilerBody != null) {
                        val spoilerText = RUN_LEAD + spoilerBody
                        fieldValue = TextFieldValue(
                            spoilerText,
                            TextRange(RUN_LEAD.length + spoilerCaret)
                        )
                        pendingText = spoilerText
                    } else if (listStarted && listPrefix != null) {
                        val strippedBody = newBody.removeRange(listPrefix.lineStart, caret)
                        val strippedText = RUN_LEAD + strippedBody
                        fieldValue = TextFieldValue(strippedText, TextRange(listPrefix.lineStart + 1))
                        pendingText = strippedText
                    } else if (dividerBlock != null && dividerLine != null) {
                        // I trattini spariscono — erano un comando, non roba
                        // scritta — e la riga, rimasta vuota, diventa il
                        // divisore col cursore sulla riga nuova sotto: lo
                        // stesso giro di "Divider" dal menu "/"
                        // (`applySlashEntry`), testo prima e tipo dopo.
                        val strippedBody = newBody.removeRange(dividerLine.lineStart, caret)
                        val strippedText = RUN_LEAD + strippedBody
                        fieldValue = TextFieldValue(strippedText, TextRange(RUN_LEAD.length + dividerLine.lineStart))
                        pendingText = strippedText
                        viewModel.updateRun(
                            runBlocks.map { it.id },
                            strippedBody.split('\n'),
                            oldBody.split('\n'),
                            dividerLine.lineIndex
                        )
                        onSlashAction(dividerBlock, SlashAction.Divider)
                    } else if (stepUndone) {
                        val oldLines = oldBody.split('\n')
                        var caretAtLineStart = 1
                        for (i in 0 until backspacedLine) caretAtLineStart += oldLines[i].length + 1
                        fieldValue = TextFieldValue(oldText, TextRange(caretAtLineStart))
                    } else {
                        // **Un tocco sulle righe vuote in fondo alla pagina**
                        // (dal 30/09/2026): il cursore va sulla prima di loro
                        // — in una pagina vuota, sulla prima riga — e non dove
                        // cade il dito, col vuoto sopra. Solo quando si sposta
                        // il cursore e basta: l'Invio, che cambia il testo, fa
                        // lo spazio che si vuole. Vedi `trailingBlankRedirect`.
                        val moveOnly = newText == oldText && newValue.selection.collapsed &&
                            newValue.selection != fieldValue.selection
                        val redirectLine = if (moveOnly) {
                            viewModel.trailingBlankRedirect(runBlocks.map { it.id }, newBody.split('\n'), lineIndexAt(newBody, caret))
                        } else {
                            null
                        }
                        if (redirectLine != null) {
                            val lineStart = newBody.split('\n').take(redirectLine).sumOf { it.length + 1 }
                            fieldValue = newValue.copy(selection = TextRange(RUN_LEAD.length + lineStart))
                            val redirectedId = runBlocks[redirectLine].id
                            if (redirectedId != lastLineBlockId) {
                                lastLineBlockId?.let { onFocusChanged(it, false) }
                                onFocusChanged(redirectedId, true)
                                lastLineBlockId = redirectedId
                            }
                            return@BasicTextField
                        }
                        fieldValue = if (shift == 0) {
                            newValue
                        } else {
                            TextFieldValue(
                                normalizedText,
                                TextRange(newValue.selection.start + 1, caretRaw + 1)
                            )
                        }
                        // **"In sospeso" solo se il testo è cambiato davvero.**
                        // Questo ramo scatta anche quando si sposta soltanto il
                        // cursore o si seleziona: lì non parte nessun
                        // salvataggio (`updateRun` esce subito), quindi dal
                        // database non torna niente che azzeri `pendingText`, e
                        // il campo restava convinto per sempre di avere una
                        // modifica in viaggio. Da quel momento ignorava ogni
                        // cambiamento arrivato da fuori: dopo "Duplicate" dal
                        // menu del blocco mostrava ancora le righe di prima coi
                        // segni degli elenchi scalati di una, e la lettera
                        // successiva faceva sparire la copia dal database.
                        // Trovato sul telefono il 24/09/2026.
                        if (normalizedText != oldText) pendingText = normalizedText
                        // L'indice riga si calcola sul testo appena digitato,
                        // non sulle lunghezze salvate nel database tramite
                        // runBlocks — altrimenti, mentre si scrive
                        // velocemente, "su quale riga è il cursore" può
                        // risultare sbagliato per una frazione di secondo,
                        // prima che il giro di andata e ritorno dal database
                        // si allinei di nuovo.
                        val lineIndex = lineIndexAt(newBody, caret)

                        viewModel.updateRun(
                            runBlocks.map { it.id },
                            newBody.split('\n'),
                            // Quello che c'era nel campo un attimo fa: è la
                            // sola versione che corrisponde a ciò che
                            // l'utente vedeva quando ha premuto il tasto.
                            oldBody.split('\n'),
                            // E dov'è il cursore: senza, una riga vuota
                            // inserita in mezzo ad altre righe vuote è
                            // indistinguibile da una aggiunta in fondo.
                            lineIndex,
                            paste = pasted
                        )

                        val currentLineId = runBlocks.getOrNull(lineIndex)?.id
                        if (currentLineId != null && currentLineId != lastLineBlockId) {
                            lastLineBlockId?.let { onFocusChanged(it, false) }
                            onFocusChanged(currentLineId, true)
                            lastLineBlockId = currentLineId
                        }
                    }
                }
            },
            // Se nel gruppo c'è anche una sola casella da spuntare, le
            // righe stanno più larghe: il quadratino è piccolo e attaccate
            // è facile spuntare quella sbagliata. Vale per tutto il campo,
            // non riga per riga — vedi `CHECKBOX_LINE_HEIGHT` per il
            // perché. Dal 27/10/2026 le caselle e le righe normali stanno
            // in campi diversi (`PageEditorViewModel.lineLookOf`), così le
            // righe normali vicine non si allargano con loro.
            // Il testo più grande scelto con la barra Aa allunga qui le righe
            // (`lineScale`): il `textStyle` dell'intero campo è il posto dove
            // il README dice che le misure si possono toccare senza rischi.
            // Font e corpo, invece, stanno sui pezzi di testo (`fontStyleOf`).
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = NotionWhite,
                // **L'altezza della riga è sempre fissata.**
                //
                // Lasciandola decidere al carattere, ogni riga diventava
                // alta quanto il segno più grande che conteneva: le righe
                // coi pallini (scritti a 20) erano più alte di quelle coi
                // rombi (scritti a 13), e l'elenco veniva a passo
                // irregolare — i segni sembravano storti, ma a essere
                // storte erano le righe. Fissandola, tutte le righe sono
                // alte uguale e i segni si appoggiano tutti allo stesso
                // modo, qualunque corpo abbiano.
                lineHeight = lineHeight,
                // **Il testo si centra nella riga alta.** Di suo Compose
                // spartisce lo spazio in più in proporzione a quanto le
                // lettere salgono e scendono, e il risultato è che il testo
                // non sta in mezzo: i quadratini, che invece stanno in
                // mezzo, risultavano disallineati. Chiedendo il centro,
                // testo e quadratino coincidono per costruzione, qualunque
                // sia l'altezza di riga.
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None
                )
            ),
            cursorBrush = SolidColor(NotionWhite),
            // Pagina bloccata: si legge e si seleziona, non si scrive.
            // `readOnly` e non `enabled = false` di proposito — disabilitato
            // il testo non si potrebbe nemmeno selezionare per copiarlo, e
            // bloccare una pagina serve a non rovinarla, non a non usarla.
            readOnly = LocalPageLocked.current,
            // Vedi il commento gemello sui blocchi isola: la maiuscola a
            // inizio frase va chiesta, altrimenti non arriva.
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            ),
            visualTransformation = mergedTransformation,
            modifier = Modifier
                .fillMaxWidth()
                .atLeastLane(sideLane, (2 * sidePadPx).roundToInt())
                .drawBehind { drawMisspellings(textLayout) }
                // Col mouse: quale riga c'è a un'altezza, e il velo sulle righe scelte (vedi `BlockSelection.kt`).
                .pickableAs(
                    runKey,
                    lineOf = { id, _ -> textLayout?.let { layout -> runLineBounds(layout, id, fieldValue.text, runBlocks, ordinals, viewModel) } },
                    extentOf = { id, _ -> textLayout?.let { layout -> runLineExtent(layout, id, fieldValue.text, runBlocks, ordinals, viewModel) } }
                ) { y -> textLayout?.let { layout -> runLineAt(layout, y, fieldValue.text, runBlocks, ordinals, viewModel) } }
                .drawWithContent {
                    drawContent()
                    val chosen = selectionLook?.selected?.value
                    val layout = textLayout
                    if (chosen.isNullOrEmpty() || layout == null || runBlocks.none { it.id in chosen }) return@drawWithContent
                    val ranges = shownLineRanges(fieldValue.text, runBlocks, ordinals, viewModel)
                    val last = layout.layoutInput.text.length
                    runBlocks.forEachIndexed { i, block ->
                        val range = ranges.getOrNull(i)
                        if (block.id !in chosen || range == null) return@forEachIndexed
                        val top = layout.getLineTop(layout.getLineForOffset(range.first.coerceIn(0, last)))
                        val bottom = layout.getLineBottom(layout.getLineForOffset(range.last.coerceIn(0, last)))
                        drawRect(
                            color = selectionLook.veil.copy(alpha = 0.18f),
                            topLeft = Offset(-6.dp.toPx(), top),
                            size = Size(size.width + 12.dp.toPx(), bottom - top)
                        )
                    }
                }
                // L'indice navigabile (vedi `PageAnchors.kt`): la freccia dei
                // collegamenti dentro la pagina e il segnalibro delle righe col pin.
                .drawBehind {
                    drawBlockLinkArrows(textLayout, TextLinkColor)
                    for (line in pinnedLines) {
                        lineTopIn(textLayout, line)?.let { top -> drawPinRibbon(top, left = 0f, color = AccentBlue) }
                    }
                }
                // Un tocco su un testo con un collegamento lo apre (vedi
                // `TextLinks.kt`): prima del correttore, che così non apre
                // il suo menu sopra il link.
                .linkTaps(layout = { textLayout }, onLink = openLink)
                .onPreviewKeyEvent { slashKey(it) }
                .proofingTaps(enabled = proofing != null && !pageLocked, layout = { textLayout }) { word, at ->
                    proofMenu = word to at
                }
                // Col tasto destro il cursore va dove si clicca, poi il menu (vedi `PcContextMenu.kt`).
                .rightClickCaret(
                    enabled = isPc && !pageLocked,
                    layout = { textLayout },
                    value = { fieldValue },
                    transformation = { mergedTransformation },
                    focused = { fieldFocused }
                ) { moved ->
                    fieldValue = moved
                    focusRequester.requestFocus()
                }
                .drawBehind {
                    val strength = moveFlash.value
                    val movedId = movedBlock?.blockId
                    val layout = textLayout
                    if (strength <= 0f || movedId == null || layout == null) return@drawBehind
                    val line = runBlocks.indexOfFirst { it.id == movedId }
                    if (line < 0) return@drawBehind
                    // Da dove a dove sta la riga nel testo **mostrato**, coi
                    // prefissi: lo stesso conto di `displayedCaretOffset`.
                    val lines = bodyOf(fieldValue.text).split('\n')
                    val prefixes = runPrefixes(lines, runBlocks, ordinals, viewModel)
                    fun shownLength(i: Int) = (prefixes.getOrNull(i)?.text?.length ?: 0) + (lines.getOrNull(i)?.length ?: 0)
                    val start = (0 until line).sumOf { shownLength(it) + 1 }
                    val last = layout.layoutInput.text.length
                    val top = layout.getLineTop(layout.getLineForOffset(start.coerceIn(0, last)))
                    val bottom = layout.getLineBottom(layout.getLineForOffset((start + shownLength(line)).coerceIn(0, last)))
                    drawRoundRect(
                        color = moveFlashColor.copy(alpha = 0.3f * strength),
                        topLeft = Offset(-6.dp.toPx(), top),
                        size = Size(size.width + 12.dp.toPx(), bottom - top),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
                .bringIntoViewRequester(caretIntoView)
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    fieldFocused = state.isFocused
                    if (state.isFocused) {
                        val lineIndex = lineIndexAt(bodyOf(fieldValue.text), fieldValue.selection.end - 1)
                        val id = runBlocks.getOrNull(lineIndex)?.id ?: runBlocks.first().id
                        lastLineBlockId = id
                        onFocusChanged(id, true)
                    } else {
                        lastLineBlockId?.let { onFocusChanged(it, false) }
                        lastLineBlockId = null
                        // Perso il fuoco non si sta più scegliendo niente.
                        slashAnchor = null
                        mentionAnchor = null
                    }
                }
        )
    }
    }

    // **Dopo** il campo, non prima: dentro un `Box` i tocchi li riceve
    // per primo l'ultimo figlio, e messi prima i quadratini non
    // rispondevano — il tocco andava tutto al testo.
    RunCheckboxOverlay(
        runBlocks = runBlocks,
        layout = textLayout,
        onToggle = { viewModel.toggleCheckbox(it) }
    )

    proofMenu?.let { (word, at) ->
        val line = bodyOf(fieldValue.text).split('\n').getOrNull(word.line)
        if (line != null && word.end <= line.length) {
            ProofingMenu(
                word = line.substring(word.start, word.end),
                anchor = at,
                onReplace = { replaceMisspelled(word, it) },
                onDismiss = { proofMenu = null }
            )
        }
    }

    if (slashAnchor != null) {
        SlashMenu(
            query = slashQuery,
            highlighted = slashChoosable.getOrNull(slashHighlight),
            onDismiss = { slashAnchor = null },
            onSelect = { entry -> applySlashEntry(entry) }
        )
    }

    // Scelta una voce del menu "@", la chiocciola e quello che le sta dietro
    // diventano il titolo della pagina (o la data) col suo collegamento, più
    // uno spazio per continuare a scrivere. Lo stesso giro di `applySlashEntry`,
    // col collegamento passato come quelli di un incolla.
    fun applyMention(anchor: Int, query: String, shown: String, link: String) {
        mentionAnchor = null
        mentionQuery = ""
        val body = bodyOf(fieldValue.text)
        if (anchor >= body.length || body[anchor] != '@') return
        val end = (anchor + 1 + query.length).coerceAtMost(body.length)
        val text = shown.replace('\n', ' ').ifBlank { Strings.untitled }
        val cleaned = body.replaceRange(anchor, end, if (body.getOrNull(end) == ' ') text else "$text ")
        val cleanedText = RUN_LEAD + cleaned
        fieldValue = TextFieldValue(cleanedText, TextRange(RUN_LEAD.length + anchor + text.length + 1))
        pendingText = cleanedText
        viewModel.updateRun(
            runBlocks.map { it.id },
            cleaned.split('\n'),
            body.split('\n'),
            lineIndexAt(cleaned, anchor),
            links = listOf(LinkRange(anchor, anchor + text.length, link))
        )
    }

    mentionAnchor?.let { anchor ->
        MentionMenu(
            query = mentionQuery,
            candidates = viewModel::mentionCandidates,
            onDismiss = { mentionAnchor = null },
            onPick = { choice ->
                val query = mentionQuery
                when (choice) {
                    is MentionChoice.Page -> applyMention(anchor, query, choice.page.title, pageLink(choice.page.id))
                    is MentionChoice.Day -> {
                        val date = choice.date
                        if (date != null) {
                            applyMention(anchor, query, MentionStrings.dateText(date), dateLink(date))
                        } else {
                            mentionAnchor = null
                            mentionDateFor = anchor to query
                        }
                    }
                }
            }
        )
    }
    mentionDateFor?.let { (anchor, query) ->
        SamsungDateDialog(
            initial = LocalDate.now(),
            onPick = { date ->
                applyMention(anchor, query, MentionStrings.dateText(date), dateLink(date))
                runCatching { focusRequester.requestFocus() }
            },
            onDismiss = { mentionDateFor = null }
        )
    }

    // Tenendo premuto sul numero di un elenco numerato si apre il menu
    // per rimaneggiare la lista (far ripartire il conteggio, rimetterlo
    // in fila, togliere il numero, passare al pallino) — le stesse
    // quattro voci di OneNote. Il numero è disegnato dalla
    // trasformazione visiva, quindi non è un elemento a sé che si possa
    // rendere toccabile: gli mettiamo sopra una zona invisibile,
    // posizionata dove il layout del testo dice che il numero sta.
    // Una zona sovrapposta riceve i tocchi solo dentro i propri confini,
    // quindi tutto il resto del campo continua a comportarsi come prima.
    val layout = textLayout
    if (layout != null) {
        val density = LocalDensity.current
        val lines = bodyOf(fieldValue.text).split('\n')
        val prefixes = runPrefixes(lines, runBlocks, ordinals, viewModel)
        val lineBlocks = blocksOfLines(lines, runBlocks) { viewModel.plainTextOf(it) }.blocks
        var displayStart = 0
        lines.forEachIndexed { i, line ->
            val block = lineBlocks[i]
            val prefix = prefixes[i]
            val markerStart = displayStart + prefix.indent.length
            val markerEnd = displayStart + prefix.text.length
            if (block != null &&
                block.type == BlockType.NUMBERED_LIST_ITEM &&
                prefix.marker.isNotEmpty() &&
                markerEnd <= layout.layoutInput.text.length
            ) {
                val textLine = layout.getLineForOffset(markerStart)
                val left = layout.getHorizontalPosition(markerStart, true)
                val right = layout.getHorizontalPosition(markerEnd, true)
                val top = layout.getLineTop(textLine)
                val bottom = layout.getLineBottom(textLine)
                with(density) {
                    Box(
                        modifier = Modifier
                            .offset(x = left.toDp(), y = top.toDp())
                            .size(width = (right - left).toDp(), height = (bottom - top).toDp())
                            .pointerInput(block.id) {
                                // Solo il tocco prolungato. Prima un
                                // tocco semplice faceva ripartire la
                                // numerazione: comodo da scoprire,
                                // scomodo da subire, perché rinumerava
                                // tutta la lista per un dito appoggiato
                                // male. Ora quella è una delle voci del
                                // menu, dove la si sceglie apposta.
                                detectTapGestures(
                                    onLongPress = { numberMenuFor = block.id }
                                )
                            }
                    )
                }
            }
            displayStart += prefix.text.length + line.length + 1
        }
    }
    }

    val menuBlockId = numberMenuFor
    if (menuBlockId != null) {
        NumberedListMenu(
            hasReset = runBlocks.find { it.id == menuBlockId }?.numberStartsAt != null,
            onDismiss = { numberMenuFor = null },
            onBeginNewList = { viewModel.beginNewListHere(menuBlockId) },
            onContinuePrevious = { viewModel.continuePreviousList(menuBlockId) },
            onRemoveNumber = { viewModel.removeNumber(menuBlockId) },
            onChangeToBullet = { viewModel.changeToBullet(menuBlockId) }
        )
    }
}

/**
 * Il menu del numero di un elenco, tenendo premuto sopra: le stesse
 * quattro voci di OneNote.
 *
 * "Renumber as continuation" c'è anche quando non serve — cioè quando
 * la riga non fa ripartire niente — ma spenta: toglierla del tutto
 * farebbe ballare il menu fra un'apertura e l'altra, e una voce
 * grigia dice anche *perché* non si può fare.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NumberedListMenu(
    hasReset: Boolean,
    onDismiss: () -> Unit,
    onBeginNewList: () -> Unit,
    onContinuePrevious: () -> Unit,
    onRemoveNumber: () -> Unit,
    onChangeToBullet: () -> Unit
) {
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                text = EditorStrings.blockType("Numbered list"),
                style = MaterialTheme.typography.titleMedium,
                color = NotionWhite,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            NumberedListMenuItem(EditorStrings.beginNewList) {
                onBeginNewList()
                onDismiss()
            }
            NumberedListMenuItem(
                label = EditorStrings.renumberContinuation,
                enabled = hasReset
            ) {
                onContinuePrevious()
                onDismiss()
            }
            NumberedListMenuItem(EditorStrings.removeNumber) {
                onRemoveNumber()
                onDismiss()
            }
            NumberedListMenuItem(EditorStrings.changeToBullet) {
                onChangeToBullet()
                onDismiss()
            }
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun NumberedListMenuItem(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = if (enabled) NotionWhite else NotionWhite.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 14.dp)
    )
}

/**
 * Row for a single "island" block (checkbox, toggle and its children,
 * table, divider, page/database link) — renderizzazione a campo
 * singolo invariata rispetto a prima. Usata sia per le isole di primo
 * livello sia, ricorsivamente, per i figli di un TOGGLE (che per ora
 * restano blocchi separati anche se di tipo testo scorrevole — unirli
 * in un campo condiviso anche lì è un passo successivo).
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun BlockRow(
    block: BlockEntity,
    allBlocks: List<BlockEntity>,
    depth: Int,
    viewModel: PageEditorViewModel,
    factory: ViewModelFactory,
    focusRequestBlockId: String?,
    formatRequest: Pair<String, FormatType>?,
    onFocusChanged: (String, Boolean) -> Unit,
    onNavigateToPage: (String) -> Unit,
    onNavigateToDatabase: (String) -> Unit,
    /** Apre il menu del blocco (sei puntini) su un blocco. Null su una pagina bloccata. */
    onOpenBlockMenu: ((String) -> Unit)? = null
) {
    if (block.type == BlockType.TABLE) {
        TableBlockContent(block = block, depth = depth, viewModel = viewModel)
        return
    }

    // **Un blocco sincronizzato** (dal 03/11/2026): una cornice con le sue
    // righe dentro, allo stesso livello del blocco. Vedi `SyncedBlockFrame`.
    if (block.type == BlockType.SYNCED) {
        val syncedChildren = allBlocks.filter { it.parentBlockId == block.id }.sortedBy { it.orderIndex }
        val syncedChanges by viewModel.syncedChanges.collectAsStateWithLifecycle()
        val syncedLook = LocalParagraphLook.current
        val syncedDensity = LocalDensity.current
        SyncedBlockFrame(
            block = block,
            start = PAGE_SIDE_PADDING + NESTED_INDENT * depth,
            end = PAGE_SIDE_PADDING,
            isEmpty = syncedChildren.isEmpty(),
            locked = LocalPageLocked.current,
            refreshKey = syncedChanges,
            pageCount = { viewModel.syncedPageCount(it) },
            onOpenMenu = onOpenBlockMenu?.let { open -> { open(block.id) } },
            onAddFirstLine = { viewModel.addFirstChild(block) }
        ) {
            syncedChildren.forEach { child ->
                // Ogni riga col suo `key`, come dentro un toggle.
                key(child.id) {
                    // Una colonna, non un `Box`: un database qui dentro ha la sua riga di fine sotto, non in cima.
                    Column(modifier = Modifier.padding(vertical = (syncedLook.spacingOf(child) / 2f).sp.toDpIn(syncedDensity))) {
                        BlockRow(
                            block = child,
                            allBlocks = allBlocks,
                            depth = depth,
                            viewModel = viewModel,
                            factory = factory,
                            focusRequestBlockId = focusRequestBlockId,
                            formatRequest = formatRequest,
                            onFocusChanged = onFocusChanged,
                            onNavigateToPage = onNavigateToPage,
                            onNavigateToDatabase = onNavigateToDatabase,
                            onOpenBlockMenu = onOpenBlockMenu
                        )
                    }
                }
            }
        }
        return
    }

    // **Le colonne affiancate** (dal 12/11/2026, chieste dall'utente): sul PC
    // una accanto all'altra, larghe uguali; sul telefono una sotto l'altra,
    // prima tutta la prima, poi la seconda… Le righe di ogni colonna come
    // quelle di un blocco sincronizzato, allo stesso livello e ognuna col suo
    // margine: fra due colonne i due margini fanno lo spazio. Una colonna
    // rimasta senza righe si tocca per ricominciare a scrivere.
    if (block.type == BlockType.COLUMN_LIST) {
        val columns = allBlocks.filter { it.parentBlockId == block.id }.sortedBy { it.orderIndex }
        if (isPc) {
            // **Larghe quanto la loro parte** (dal 13/11/2026), che si cambia trascinando il bordo (vedi `ColumnEdges.kt`).
            val edges = LocalColumnEdges.current
            val weights = columnWeights(columns)
            var draft by remember(block.id) { mutableStateOf<List<Float>?>(null) }
            // Le parti salvate sono arrivate: quelle trascinate non servono più.
            LaunchedEffect(weights) { draft = null }
            val shown = draft?.takeIf { it.size == columns.size } ?: weights
            val latestShown by rememberUpdatedState(shown)
            val latestIds by rememberUpdatedState(columns.map { it.id })
            val dragStart = remember(block.id) { arrayOfNulls<List<Float>>(1) }
            DisposableEffect(block.id, edges) { onDispose { edges?.spots?.remove(block.id) } }
            Row(
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (edges == null) {
                            Modifier
                        } else {
                            Modifier.onGloballyPositioned { coordinates ->
                                edges.spots[block.id] = ColumnEdgeSpot(
                                    coordinates = coordinates,
                                    weights = { latestShown },
                                    onStart = { dragStart[0] = latestShown },
                                    onDrag = { index, dx ->
                                        val start = dragStart[0]
                                        val width = coordinates.size.width
                                        if (start != null && width > 0) draft = movedColumnEdge(start, index, dx * start.sum() / width)
                                    },
                                    onEnd = {
                                        dragStart[0] = null
                                        draft?.let { parts -> viewModel.setColumnWeights(latestIds.zip(parts.map { it / parts.sum() })) }
                                    }
                                )
                            }
                        }
                    )
            ) {
                columns.forEachIndexed { index, column ->
                    key(column.id) {
                        // **Ogni colonna staccata dal suo bordo quanto la prima dal bordo della pagina**
                        // (dal 13/11/2026, chiesto dall'utente: fra la prima e la seconda lo spazio era nullo).
                        Column(Modifier.weight(shown.getOrElse(index) { 1f }).padding(start = if (index > 0) PAGE_LEFT_INSET else 0.dp)) {
                            ColumnLines(
                                column, allBlocks, depth, viewModel, factory, focusRequestBlockId, formatRequest,
                                onFocusChanged, onNavigateToPage, onNavigateToDatabase, onOpenBlockMenu
                            )
                        }
                    }
                }
            }
        } else {
            Column {
                columns.forEach { column ->
                    key(column.id) {
                        ColumnLines(
                            column, allBlocks, depth, viewModel, factory, focusRequestBlockId, formatRequest,
                            onFocusChanged, onNavigateToPage, onNavigateToDatabase, onOpenBlockMenu
                        )
                    }
                }
            }
        }
        return
    }

    // Il codice ha un campo tutto suo: vedi `CodeBlockContent`.
    if (block.type == BlockType.CODE) {
        val codeFocusPark = LocalFocusPark.current
        CodeBlockContent(
            block = block,
            viewModel = viewModel,
            focusRequestBlockId = focusRequestBlockId,
            onFocusChanged = onFocusChanged,
            locked = LocalPageLocked.current,
            onUnmade = { codeFocusPark?.requestFocus() },
            modifier = Modifier.padding(start = PAGE_SIDE_PADDING + NESTED_INDENT * depth, end = PAGE_SIDE_PADDING)
        )
        return
    }

    // Un database dentro una pagina si vede e si modifica lì, senza
    // aprirlo: è il blocco stesso. Per vederlo a schermo intero c'è il
    // menu "..." accanto al suo nome.
    val databasePageId = block.linkedPageId
    if (block.type == BlockType.DATABASE_LINK && databasePageId != null) {
        var confirmDatabaseDeletion by remember(block.id) { mutableStateOf(false) }
        // Se è una vista collegata: "Delete" toglie solo la vista, e lo
        // deve dire, invece di minacciare di cancellare tutte le righe.
        val shownPage by remember(databasePageId) {
            viewModel.observePageInfo(databasePageId)
        }.collectAsStateWithLifecycle(initialValue = null)
        val isLinkedView = shownPage?.sourceDatabaseId != null

        // Un database bloccato con la password (o una vista collegata a
        // lui) dentro una pagina aperta: al suo posto la richiesta di
        // sbloccarlo.
        // Nelle celle del database "Select all" prende solo la cella (vedi `PageSelection.kt`).
        LockGate(databasePageId, factory.pageRepository, fullScreen = false, isDatabase = true) {
        PlainTextToolbar {
        DatabaseContent(
            pageId = databasePageId,
            factory = factory,
            embedded = true,
            onOpenRowPage = onNavigateToPage,
            onOpenFullPage = { onNavigateToDatabase(databasePageId) },
            onDelete = { confirmDatabaseDeletion = true },
            // Il titolo "↗ Nome" di una vista collegata porta al database
            // principale, a schermo intero.
            onOpenSource = onNavigateToDatabase,
            // I sei puntini nella riga degli strumenti: il menu del
            // blocco, con in cima le voci del database.
            onOpenBlockMenu = onOpenBlockMenu?.let { open -> { open(block.id) } },
            // "Turn into page": il blocco diventa un collegamento, e si
            // resta nella pagina (dal 17/10/2026, chiesto dall'utente;
            // prima il database si apriva anche a schermo intero). Per
            // rimetterlo dentro la pagina c'è "Turn into database".
            onTurnIntoPage = { viewModel.turnDatabaseIntoPage(block) },
            // Nessun margine laterale, a differenza degli altri blocchi:
            // una tabella deve poter usare tutta la larghezza, e il
            // rientro della pagina la farebbe partire staccata dal bordo
            // mentre a schermo intero è attaccata.
            modifier = Modifier.padding(start = (depth * 20).dp, top = 8.dp, bottom = 8.dp)
        )
        }
        }
        // **La riga alla fine del database** (dal 18/10/2026, chiesto
        // dall'utente): il testo che viene dopo non sembra più attaccato
        // alla tabella. Come il divisore delle pagine.
        HorizontalDivider(
            modifier = Modifier
                .padding(start = PAGE_SIDE_PADDING + (depth * 20).dp, end = PAGE_SIDE_PADDING, bottom = 8.dp)
                .testTag("database-end-divider")
        )

        if (confirmDatabaseDeletion) {
            FitAlertDialog(
                onDismissRequest = { confirmDatabaseDeletion = false },
                title = { Text(if (isLinkedView) DbStrings.removeLinkedViewTitle else EditorStrings.deleteDatabaseTitle) },
                // Togliere una vista collegata cancella solo la vista: le
                // righe, le colonne e le celle stanno nel database di
                // origine, che non si tocca (vedi `deleteDatabaseBlock`).
                text = { Text(if (isLinkedView) DbStrings.removeLinkedViewText else EditorStrings.deleteDatabaseText) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteDatabaseBlock(block)
                        confirmDatabaseDeletion = false
                    }) { Text(Strings.delete) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDatabaseDeletion = false }) { Text(Strings.cancel) }
                }
            )
        }
        return
    }

    if (block.type == BlockType.PAGE_LINK || block.type == BlockType.DATABASE_LINK) {
        PageLinkBlockContent(
            block = block,
            depth = depth,
            viewModel = viewModel,
            onNavigateToPage = onNavigateToPage,
            onNavigateToDatabase = onNavigateToDatabase,
            onLongPress = onOpenBlockMenu?.let { open -> { open(block.id) } }
        )
        return
    }

    if (block.type == BlockType.VIDEO || block.type == BlockType.AUDIO ||
        block.type == BlockType.FILE || block.type == BlockType.BOOKMARK
    ) {
        val mediaModifier = Modifier.padding(start = PAGE_SIDE_PADDING + (depth * 20).dp, end = PAGE_SIDE_PADDING)
        val mediaLongPress = onOpenBlockMenu?.let { open -> { open(block.id) } }
        val locked = LocalPageLocked.current
        when (block.type) {
            BlockType.VIDEO -> VideoBlockContent(block, viewModel, locked, mediaLongPress, mediaModifier)
            BlockType.AUDIO -> AudioBlockContent(block, viewModel, locked, mediaLongPress, mediaModifier)
            BlockType.FILE -> FileBlockContent(block, viewModel, locked, mediaLongPress, mediaModifier)
            else -> BookmarkBlockContent(block, viewModel, locked, mediaLongPress, mediaModifier)
        }
        return
    }

    if (block.type == BlockType.IMAGE) {
        ImageBlockContent(
            block = block,
            viewModel = viewModel,
            locked = LocalPageLocked.current,
            onLongPress = onOpenBlockMenu?.let { open -> { open(block.id) } },
            modifier = Modifier.padding(start = PAGE_SIDE_PADDING + (depth * 20).dp, end = PAGE_SIDE_PADDING)
        )
        return
    }

    if (block.type == BlockType.PAGE_MENTION) {
        PageMentionBlockContent(
            block = block,
            viewModel = viewModel,
            onOpenPage = { page -> if (page.isDatabase) onNavigateToDatabase(page.id) else onNavigateToPage(page.id) },
            onLongPress = onOpenBlockMenu?.let { open -> { open(block.id) } },
            modifier = Modifier.padding(start = PAGE_SIDE_PADDING + (depth * 20).dp, end = PAGE_SIDE_PADDING)
        )
        return
    }

    if (block.type == BlockType.BUTTON) {
        ButtonBlockContent(
            block = block,
            viewModel = viewModel,
            onLongPress = onOpenBlockMenu?.let { open -> { open(block.id) } },
            modifier = Modifier.padding(start = PAGE_SIDE_PADDING + (depth * 20).dp, end = PAGE_SIDE_PADDING)
        )
        return
    }

    if (block.type == BlockType.DIVIDER) {
        DividerBlockContent(
            block = block,
            depth = depth,
            viewModel = viewModel,
            onLongPress = onOpenBlockMenu?.let { open -> { open(block.id) } }
        )
        return
    }

    // Il testo del campo comincia con un a-capo nascosto, come il campo
    // unito (vedi `RUN_LEAD`): è l'unico modo per accorgersi del
    // backspace premuto a inizio riga. La tastiera non manda eventi
    // tasto che si possano intercettare — provato e documentato — ma
    // se il backspace si mangia quell'a-capo il testo cambia, e un
    // cambiamento si vede. Il carattere dev'essere un a-capo e non
    // qualcosa di invisibile: uno zero-width space la tastiera se lo
    // ingloba nella parola che sta componendo.
    var editState by remember(block.id) {
        val spans = viewModel.spansOf(block)
        val body = spans.plainText()
        mutableStateOf(
            BlockEditState(
                spans,
                TextFieldValue(RUN_LEAD + body, TextRange(RUN_LEAD.length + body.length))
            )
        )
    }
    // L'ultima divisione fatta da questo campo: serve a riconoscere
    // l'eco della tastiera (vedi il commento dentro onValueChange).
    var lastSplit by remember(block.id) { mutableStateOf<SplitEcho?>(null) }
    var fieldFocused by remember(block.id) { mutableStateOf(false) }
    // Su quale numero è stato tenuto premuto: apre il menu della lista.
    var numberMenuFor by remember(block.id) { mutableStateOf<String?>(null) }
    // Il correttore, come nel campo unito: vedi lì.
    var proofTyping by remember(block.id) { mutableStateOf<Int?>(null) }
    var proofMenu by remember(block.id) { mutableStateOf<Pair<MisspelledWord, IntRect>?>(null) }
    var islandLayout by remember(block.id) { mutableStateOf<TextLayoutResult?>(null) }
    val proofing = LocalProofing.current
    val islandMisspellings = rememberMisspellings(if (proofing == null) emptyList() else listOf(bodyOf(editState.fieldValue.text)))

    // Quando il testo del blocco cambia da fuori — la riga di sotto che
    // ci si fonde dentro col backspace — il campo deve rileggerlo.
    // Solo mentre non ha il fuoco: risincronizzarlo mentre si scrive
    // vorrebbe dire litigare con la tastiera ad ogni carattere.
    // Anche quando cambia **solo la formattazione** e non il testo: è
    // il caso del colore messo dal pennello, che scrive nel database
    // mentre questo campo non ha il fuoco. Guardando il solo testo la
    // riga sarebbe rimasta del colore di prima fino alla battuta
    // successiva.
    val storedText = viewModel.plainTextOf(block)
    val storedSpans = viewModel.spansOf(block)
    LaunchedEffect(block.id, storedText, storedSpans, fieldFocused) {
        if (fieldFocused) return@LaunchedEffect
        if (bodyOf(editState.fieldValue.text) == storedText &&
            editState.spans == storedSpans
        ) {
            return@LaunchedEffect
        }
        val spans = storedSpans
        val caret = viewModel.pendingCaret.value
            ?.takeIf { it.first == block.id }
            ?.second
            ?: storedText.length
        editState = BlockEditState(
            spans,
            TextFieldValue(
                RUN_LEAD + storedText,
                TextRange(RUN_LEAD.length + caret.coerceIn(0, storedText.length))
            )
        )
        viewModel.consumePendingCaret()
    }
    // **Il font, il corpo e il colore messi dalla barra Aa o dal pennello**
    // arrivano subito, anche col fuoco (corretto il 30/09/2026): prima il
    // corpo scelto nella finestrella andava nel database ma non qui, e il
    // titolo del toggle restava com'era; alla lettera dopo il campo
    // riscriveva i suoi span e il corpo nuovo si perdeva. Il testo non
    // cambia, quindi tastiera e cursore restano dove sono.
    LaunchedEffect(block.id) {
        viewModel.externalSpans.collect { (id, spans) ->
            if (id == block.id && spans.plainText() == bodyOf(editState.fieldValue.text)) {
                editState = editState.copy(spans = spans)
            }
        }
    }
    // Il testo nuovo di un link (dal 27/10/2026, vedi
    // `PageEditorViewModel.replaceLinkText`): subito, anche col fuoco, col
    // cursore in fondo al link.
    LaunchedEffect(block.id) {
        viewModel.linkTextChanges.collect { change ->
            if (change.blockId == block.id) {
                val text = change.spans.plainText()
                editState = BlockEditState(
                    change.spans,
                    TextFieldValue(RUN_LEAD + text, TextRange(RUN_LEAD.length + change.caret.coerceIn(0, text.length)))
                )
            }
        }
    }
    val focusRequester = remember { FocusRequester() }
    val focusPark = LocalFocusPark.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val imeVisible = isKeyboardVisible()
    // Le voci in più del riquadro: icona e colore (vedi `BlockAttrs`).
    val blockAttrs = remember(block.attrsJson) { block.attrs }
    val rowContext = LocalPlatformContext.current
    val rowImageStore = remember(rowContext) { PageImageStore(rowContext) }
    var editingCalloutIcon by remember(block.id) { mutableStateOf(false) }
    val pageLocked = LocalPageLocked.current
    val openLink = rememberLinkOpener()

    // **Le righe intere** (dal 19/10/2026, "Full line view"): il testo scorre
    // di lato con quello della pagina, e scrivendo il cursore resta in vista.
    // Il campo di un'isola, di suo, lo chiede solo quando prende il fuoco.
    val sideLane = rememberSideLane()
    val islandCaret = remember { BringIntoViewRequester() }
    LaunchedEffect(editState.fieldValue.selection, islandLayout, fieldFocused) {
        if (sideLane == null || !fieldFocused) return@LaunchedEffect
        val layout = islandLayout ?: return@LaunchedEffect
        val at = (editState.fieldValue.selection.end - RUN_LEAD.length).coerceIn(0, layout.layoutInput.text.length)
        islandCaret.bringIntoView(layout.getCursorRect(at))
    }

    LaunchedEffect(focusRequestBlockId) {
        if (focusRequestBlockId == block.id) {
            // Se chi chiede il fuoco dice anche dove mettere il cursore —
            // salendo col backspace da una riga vuota, in fondo a questa —
            // va messo prima di prendere il fuoco: dopo, la tastiera
            // partirebbe dal punto vecchio e lo si vedrebbe saltare.
            viewModel.pendingCaret.value
                ?.takeIf { it.first == block.id }
                ?.let { (_, at) ->
                    val body = bodyOf(editState.fieldValue.text)
                    editState = editState.copy(
                        fieldValue = editState.fieldValue.copy(
                            selection = TextRange(RUN_LEAD.length + at.coerceIn(0, body.length))
                        )
                    )
                    viewModel.consumePendingCaret()
                }
            focusRequester.requestFocus()
            // Vedi il commento gemello nel campo unito: va chiesta
            // sempre, perché subito dopo un cambio di fuoco la
            // tastiera risulta ancora aperta mentre sta sparendo.
            keyboardController?.show()
            viewModel.consumeFocusRequest()
        }
    }

    // Vedi il gemello nel campo unito: la selezione va segnalata mentre
    // la si ha, perché il pennello se la porta via il fuoco.
    LaunchedEffect(editState.fieldValue.selection, fieldFocused) {
        if (!fieldFocused) return@LaunchedEffect
        val sel = editState.fieldValue.selection
        viewModel.reportSelection(
            listOf(block.id),
            (sel.min - RUN_LEAD.length).coerceAtLeast(0),
            (sel.max - RUN_LEAD.length).coerceAtLeast(0)
        )
    }

    LaunchedEffect(formatRequest) {
        if (formatRequest?.first == block.id) {
            // **Meno l'a-capo nascosto.** Gli span contano il testo vero,
            // il campo conta anche il carattere in testa che non si
            // vede: senza questo scarto la formattazione cadeva su una
            // lettera prima di quella selezionata.
            val sel = editState.fieldValue.selection
            val from = (sel.min - RUN_LEAD.length).coerceAtLeast(0)
            val to = (sel.max - RUN_LEAD.length).coerceAtLeast(0)
            val newSpans = when (formatRequest.second) {
                FormatType.BOLD -> toggleFormatInRange(editState.spans, from, to, { it.bold }) { s, v -> s.copy(bold = v) }
                FormatType.ITALIC -> toggleFormatInRange(editState.spans, from, to, { it.italic }) { s, v -> s.copy(italic = v) }
                FormatType.UNDERLINE -> toggleFormatInRange(editState.spans, from, to, { it.underline }) { s, v -> s.copy(underline = v) }
                FormatType.STRIKETHROUGH -> toggleFormatInRange(editState.spans, from, to, { it.strikethrough }) { s, v -> s.copy(strikethrough = v) }
                FormatType.SPOILER -> toggleFormatInRange(editState.spans, from, to, { it.spoiler }) { s, v -> s.copy(spoiler = v) }
                FormatType.SUBSCRIPT -> toggleFormatInRange(editState.spans, from, to, { it.subscript }) { s, v -> s.withSubscript(v) }
                FormatType.SUPERSCRIPT -> toggleFormatInRange(editState.spans, from, to, { it.superscript }) { s, v -> s.withSuperscript(v) }
            }
            editState = editState.copy(spans = newSpans)
            viewModel.updateBlockSpans(block, newSpans)
            viewModel.consumeFormatRequest()
        }
    }

    // Il testo più grande scelto con la barra Aa allunga la riga, come nel
    // testo condiviso (vedi `lineScaleFor`).
    //
    // **E la riga è fatta come quella del testo condiviso**: alta 24 col
    // testo in mezzo. Di suo Compose toglie lo spazio in più sopra la
    // prima riga e sotto l'ultima, e in un campo di una riga sola il
    // testo finiva ~3dp più in alto di tutto quello che gli sta accanto,
    // che invece è alto 24: la freccetta del toggle, i numeri e i
    // pallini, che si scrivono in un testo a parte, sembravano scesi.
    // Visto dall'utente il 25/09/2026 ("storto"), in un toggle con dentro
    // un elenco numerato.
    val lineScale = lineScaleFor(editState.spans)
    val textStyle = MaterialTheme.typography.bodyLarge.let {
        it.copy(
            lineHeight = it.lineHeight * lineScale,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None
            )
        )
    }.let { base ->
        if (block.type == BlockType.CHECKBOX && block.isChecked) {
            base.copy(textDecoration = TextDecoration.LineThrough)
        } else base
    }

    // Le righe di testo dentro un toggle stanno al passo di quelle fuori
    // (24, riga dopo riga): con altri 4dp sopra e sotto ognuna, un elenco
    // dentro un toggle veniva più largo dello stesso elenco fuori. Il
    // titolo del toggle e le altre isole tengono il loro respiro.
    val rowPadding = if (block.type in FLOWING_TYPES) 0.dp else 4.dp
    // Col testo più grande la prima riga si allunga (`lineScale`): la
    // freccetta del toggle, la casella e la campanella scendono di metà
    // dell'allungamento, così restano a metà della riga e non in cima
    // (dal 30/09/2026, col corpo del titolo che finalmente cambia).
    val baseLineHeight = with(LocalDensity.current) { MaterialTheme.typography.bodyLarge.lineHeight.toDp() }
    val markerDrop = baseLineHeight * (lineScale - 1f) / 2
    // **Il riquadro e la citazione** (dal 06/10/2026, chiesti dall'utente):
    // il riquadro è una scatola col suo sfondo, che si allunga con quello che
    // ci si scrive; la citazione è la riga spessa a sinistra, alta quanto il
    // testo. Il resto della riga è quello di ogni isola col testo.
    val quoteBarColor = NotionWhite
    // Lo spazio dopo il marcatore ("Paragraph", dal 21/10/2026): 8 se
    // nessuno sceglie altro, come è sempre stato. La casella ne ha già 4 di
    // suo, fra il quadratino e il bordo della zona di tocco.
    val paragraphLook = LocalParagraphLook.current
    val density = LocalDensity.current
    fun markerGap(type: BlockType, natural: Dp): Dp =
        paragraphLook.gapOf(block, type)?.let { it.sp.toDpIn(density) } ?: natural
    val checkboxGap = paragraphLook.gapOf(block, BlockType.CHECKBOX)
        ?.let { (it.sp.toDpIn(density) - (CHECKBOX_TOUCH_SIZE - CHECKBOX_SIZE) / 2).coerceAtLeast(0.dp) }
        ?: 8.dp
    val islandLook = when (block.type) {
        BlockType.CALLOUT -> Modifier
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(hexToColor(blockAttrs.color) ?: CalloutBackground)
            .padding(horizontal = 12.dp, vertical = 12.dp)
        BlockType.QUOTE -> Modifier
            .padding(vertical = 4.dp)
            .drawBehind {
                drawRect(quoteBarColor, size = Size(QUOTE_BAR_WIDTH.toPx(), size.height))
            }
            .padding(start = QUOTE_BAR_WIDTH + 12.dp)
        else -> Modifier.padding(top = rowPadding, bottom = rowPadding)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = PAGE_SIDE_PADDING + NESTED_INDENT * depth, end = PAGE_SIDE_PADDING)
            .then(islandLook),
        verticalAlignment = Alignment.Top
    ) {
        when (block.type) {
            BlockType.TOGGLE -> {
                // **Triangolino verso destra da chiuso, verso il basso da
                // aperto**, come su Notion. Prima erano le frecce a
                // parentesi di Material, e da aperto puntava **in su**:
                // il segno diceva l'opposto di quello che si vedeva.
                IconButton(
                    onClick = { viewModel.toggleExpanded(block) },
                    modifier = Modifier
                        .padding(top = markerDrop)
                        .size(TOGGLE_ARROW_SIZE)
                        .offset(y = -TOGGLE_ARROW_LIFT)
                ) {
                    Icon(
                        imageVector = if (block.isExpanded) Icons.Filled.ArrowDropDown else Icons.Filled.ArrowRight,
                        contentDescription = if (block.isExpanded) Strings.collapse else Strings.expand,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.size(TOGGLE_ARROW_GAP))
            }

            BlockType.BULLET_LIST_ITEM -> {
                // Stessa sequenza del testo condiviso: un punto dentro
                // un toggle non deve avere un segno diverso da uno
                // fuori, allo stesso rientro.
                Text(
                    bulletMarkerFor(block.indentLevel),
                    style = textStyle.copy(
                        fontSize = bulletMarkerSizeFor(block.indentLevel),
                        baselineShift = BULLET_MARKER_DROP
                    ),
                    modifier = Modifier.padding(end = markerGap(BlockType.BULLET_LIST_ITEM, 8.dp))
                )
            }

            BlockType.NUMBERED_LIST_ITEM -> {
                Text(
                    // Stesso modo di scrivere del testo condiviso: un
                    // numero dentro un toggle non deve essere scritto
                    // diversamente da uno fuori, allo stesso rientro.
                    numberedMarkerFor(
                        block.indentLevel,
                        viewModel.numberedListOrdinal(allBlocks, block)
                    ),
                    // Stesso stile del testo accanto, e nessuno spazio in
                    // più sopra: così il numero sta sulla sua stessa riga.
                    style = textStyle,
                    modifier = Modifier
                        .padding(end = markerGap(BlockType.NUMBERED_LIST_ITEM, 8.dp))
                        // Stesso menu del numero nel testo unito: qui
                        // ci si arriva solo per un elenco numerato
                        // dentro un toggle, ma il gesto dev'essere
                        // quello, non un altro.
                        .onRightClick { numberMenuFor = block.id }
                        .pointerInput(block.id) {
                            detectTapGestures(
                                onLongPress = { numberMenuFor = block.id }
                            )
                        }
                )
            }

            BlockType.CHECKBOX -> {
                Box(modifier = Modifier.padding(top = markerDrop)) {
                    CheckboxMark(
                        checked = block.isChecked,
                        onToggle = { viewModel.toggleCheckbox(block) }
                    )
                }
                Spacer(modifier = Modifier.size(checkboxGap))
            }

            BlockType.NOTIFICATION -> {
                Box(modifier = Modifier.padding(top = markerDrop).size(24.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = if (rememberNotificationIsPast(block)) NotionGray400 else AccentBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
            }

            BlockType.CALLOUT -> if (blockAttrs.hasCalloutIcon) {
                Box(modifier = Modifier.padding(top = markerDrop)) {
                    CalloutIcon(
                        attrs = blockAttrs,
                        store = rowImageStore,
                        onClick = if (pageLocked) null else { { editingCalloutIcon = true } }
                    )
                }
                Spacer(modifier = Modifier.size(10.dp))
            }

            // Il posto di un elenco (vedi `BlockAttrs.listIndent`): lo
            // stesso marcatore degli elementi intorno, invisibile, così il
            // testo resta allineato al loro.
            BlockType.PARAGRAPH -> when (block.heldListType) {
                BlockType.BULLET_LIST_ITEM -> Text(
                    bulletMarkerFor(block.indentLevel),
                    style = textStyle.copy(
                        fontSize = bulletMarkerSizeFor(block.indentLevel),
                        baselineShift = BULLET_MARKER_DROP,
                        color = Color.Transparent
                    ),
                    modifier = Modifier.padding(end = markerGap(BlockType.BULLET_LIST_ITEM, 8.dp))
                )
                BlockType.NUMBERED_LIST_ITEM -> {
                    // Il numero che verrebbe dopo quello di sopra: largo come i vicini.
                    val above = allBlocks
                        .filter {
                            it.parentBlockId == block.parentBlockId && it.orderIndex < block.orderIndex &&
                                it.type == BlockType.NUMBERED_LIST_ITEM && it.indentLevel == block.indentLevel
                        }
                        .maxByOrNull { it.orderIndex }
                    Text(
                        numberedMarkerFor(block.indentLevel, (above?.let { viewModel.numberedListOrdinal(allBlocks, it) } ?: 0) + 1),
                        style = textStyle.copy(color = Color.Transparent),
                        modifier = Modifier.padding(end = markerGap(BlockType.NUMBERED_LIST_ITEM, 8.dp))
                    )
                }
                else -> {}
            }

            else -> { /* PARAGRAPH has no prefix */ }
        }

        val linkClipboard = LocalClipboard.current as? LinkClipboard
        val islandTransformation = RichTextVisualTransformation(
            editState.spans,
            caret = if (fieldFocused) {
                (editState.fieldValue.selection.min - RUN_LEAD.length).coerceAtLeast(0)..
                    (editState.fieldValue.selection.max - RUN_LEAD.length).coerceAtLeast(0)
            } else {
                null
            },
            proof = proofing?.let { { text: String -> islandMisspellings[text].orEmpty() } },
            typingAt = if (fieldFocused) proofTyping else null
        )
        PageTextMenuArea {
        AutoCaps {
            BasicTextField(
                value = editState.fieldValue,
                onValueChange = { typedFieldValue ->
                    val pastedFieldValue = linkClipboard?.formatPaste(editState.fieldValue, typedFieldValue) ?: typedFieldValue
                    // I link di un incolla, come nel campo unito.
                    val pasted = pastedFieldValue.pastedFormat(RUN_LEAD.length)
                    val newFieldValue = pastedFieldValue.withoutPastedLinks().leadOutOfSelection()
                    val oldText = bodyOf(editState.fieldValue.text)
                    proofTyping = if (bodyOf(newFieldValue.text) != oldText) newFieldValue.selection.end - RUN_LEAD.length else null
                    noteLog(
                        "casella ${block.id.takeLast(4)} scrive: " +
                            "'${oldText.forLog()}' -> '${bodyOf(newFieldValue.text).forLog()}' " +
                            "cursore=${newFieldValue.selection.start}..${newFieldValue.selection.end} " +
                            "composizione=${newFieldValue.composition} fuoco=$fieldFocused"
                    )

                    if (!newFieldValue.text.startsWith(RUN_LEAD)) {
                        // È sparito l'a-capo nascosto: significa backspace a
                        // inizio riga, l'unica cosa che può mangiarselo.
                        // Sulla casella da spuntare toglie la casella e
                        // lascia il testo; da lì in poi la riga è testo
                        // normale e si fonde con quella sopra come tutte.
                        val unmade = viewModel.backspaceAtIslandStart(block)
                        editState = editState.copy(
                            fieldValue = TextFieldValue(
                                RUN_LEAD + oldText,
                                TextRange(RUN_LEAD.length)
                            )
                        )
                        if (unmade) {
                            // Il blocco cambia specie e questa casella di
                            // scrittura sparisce. Distruggerla mentre ha il
                            // fuoco fa chiudere l'app, quindi il fuoco va
                            // tolto — ma **non staccando la tastiera**:
                            // `clearFocus()` la chiude e la si vede sparire
                            // e tornare. Si posa invece sul campo
                            // invisibile, che non muore mai, e riparte da lì
                            // sul blocco nuovo.
                            focusPark?.requestFocus()
                        }
                        return@BasicTextField
                    }

                    val newText = bodyOf(newFieldValue.text)
                    if (block.type in MULTILINE_TYPES) {
                        // **Nel riquadro e nella citazione l'Invio va a capo
                        // dentro** (dal 06/10/2026): il blocco si allunga.
                        // Premuto su una riga vuota in fondo, invece, si esce:
                        // la riga vuota se ne va e sotto nasce una riga di
                        // testo, come su Notion.
                        //
                        // Anche quando il testo non cambia (la tastiera che
                        // sposta solo il cursore): lasciato passare, il suo
                        // a-capo veniva preso per un Invio e il blocco si
                        // spezzava in due (visto sul telefono).
                        if (newText == oldText) {
                            editState = editState.copy(fieldValue = newFieldValue)
                            return@BasicTextField
                        }
                        lastSplit = null
                        val caret = (newFieldValue.selection.end - RUN_LEAD.length).coerceAtLeast(0)
                        val enterAtEnd = newText == oldText + "\n" && caret == newText.length
                        if (enterAtEnd && oldText.endsWith("\n")) {
                            val keptText = oldText.dropLast(1)
                            val kept = applyTextEdit(editState.spans, oldText, keptText)
                            editState = BlockEditState(
                                kept,
                                TextFieldValue(RUN_LEAD + keptText, TextRange(RUN_LEAD.length + keptText.length))
                            )
                            viewModel.leaveMultilineBlock(block, kept)
                        } else {
                            val newSpans = applyPaste(applyTextEdit(editState.spans, oldText, newText), pasted)
                            editState = BlockEditState(newSpans, newFieldValue)
                            viewModel.updateBlockSpans(block, newSpans)
                        }
                        return@BasicTextField
                    }
                    if (newText.contains('\n')) {
                        val lines = newText.split('\n')
                        val movedAway = lines.drop(1).joinToString("\n")
                        val now = System.currentTimeMillis()
                        val echo = lastSplit
                        // L'eco della tastiera. Appena diviso il blocco gli
                        // togliamo da sotto il testo che è passato alla riga
                        // nuova; la tastiera Samsung, che aveva ancora quel
                        // testo in mano, a volte lo rimanda indietro con
                        // dentro lo stesso a-capo. Preso alla lettera è un
                        // secondo Invio, e lasciava una riga vuota di troppo.
                        //
                        // Due modi di riconoscerlo, misurati sul telefono:
                        // torna **lo stesso pezzo** da spostare (un Invio
                        // vero non se lo riporterebbe dietro, quel testo è
                        // già altrove), oppure torna **la stessa identica
                        // modifica** a distanza di pochi millisecondi, che
                        // è meno di quanto ci metta un dito a premere due
                        // volte.
                        val transition = oldText to newText
                        val samePayload = movedAway.isNotEmpty() &&
                            echo?.payload == movedAway &&
                            now - echo.at < SPLIT_ECHO_MS
                        val sameEditTwice = echo?.transition == transition &&
                            now - echo.at < DUPLICATE_EDIT_MS
                        if (samePayload || sameEditTwice) {
                            val current = editState.spans.plainText()
                            editState = editState.copy(
                                fieldValue = TextFieldValue(
                                    RUN_LEAD + current,
                                    TextRange(RUN_LEAD.length + current.length)
                                )
                            )
                            return@BasicTextField
                        }
                        lastSplit = SplitEcho(movedAway, transition, now)
                        val firstLine = lines.first()
                        // Con dei link incollati, il testo di ogni riga viene
                        // dallo stesso conto sul testo intero: così i link
                        // delle righe nuove non si perdono.
                        val pastedLines = if (pasted.isEmpty) {
                            null
                        } else {
                            applyPaste(applyTextEdit(editState.spans, oldText, newText), pasted).splitLines()
                                .takeIf { split -> split.size == lines.size && split.indices.all { split[it].plainText() == lines[it] } }
                        }
                        val spansForFirstLine = pastedLines?.first() ?: applyTextEdit(editState.spans, oldText, firstLine)
                        if (block.type == BlockType.TOGGLE && lines.size == 2) {
                            // **Invio nel titolo di un toggle**: la riga nuova
                            // va dentro (toggle aperto) o sotto (chiuso), e il
                            // titolo resta dov'è. Trattato come un paragrafo
                            // qualsiasi, il titolo finiva in una riga nuova
                            // sopra e il toggle restava vuoto. Vedi
                            // `enterInToggle`.
                            editState = BlockEditState(
                                spansForFirstLine,
                                TextFieldValue(
                                    RUN_LEAD + firstLine,
                                    TextRange(RUN_LEAD.length + firstLine.length)
                                )
                            )
                            viewModel.enterInToggle(block, spansForFirstLine, lines[1])
                        } else if (lines.size == 2 && block.type in FLOWING_TYPES &&
                            notificationOnEnter(oldText, newText, (newFieldValue.selection.end - RUN_LEAD.length).coerceAtLeast(0)) != null
                        ) {
                            // "/notification …" e Invio, come nel testo
                            // condiviso: qui la riga resta in questa casella,
                            // che cambia solo aspetto.
                            val command = NotificationCommands.parse(oldText, LocalDateTime.now(AppSettings.zoneId))!!
                            lastSplit = null
                            editState = BlockEditState(
                                listOf(RichTextSpan(text = command.text)),
                                TextFieldValue(RUN_LEAD + command.text, TextRange(RUN_LEAD.length + command.text.length))
                            )
                            viewModel.makeNotification(block, command)
                            NotificationPermission.request()
                        } else if (lines.size == 2 && pasted.lines.isEmpty()) {
                            val outcome = viewModel.splitBlockAt(block, spansForFirstLine, lines[1], pastedLines?.get(1))
                            // La riga nuova è finita sopra e questo campo
                            // resta vivo col fuoco: deve mostrare il testo
                            // dopo il cursore, col cursore all'inizio. È
                            // quello che si vede quando si va a capo.
                            noteLog(
                                "casella ${block.id.takeLast(4)} a-capo: esito=$outcome " +
                                    "prima='${firstLine.forLog()}' dopo='${lines[1].forLog()}'"
                            )
                            editState = if (outcome == SplitOutcome.NOTHING) {
                                // Il ViewModel ha rifiutato la divisione
                                // perché il campo si credeva vuoto mentre nel
                                // database c'era del testo. Si rimette il
                                // campo com'è davvero, invece di lasciarlo
                                // vuoto: era così che il testo "spariva".
                                val real = viewModel.spansOf(block)
                                val realText = real.plainText()
                                BlockEditState(
                                    real,
                                    TextFieldValue(
                                        RUN_LEAD + realText,
                                        TextRange(RUN_LEAD.length + realText.length)
                                    )
                                )
                            } else if (outcome == SplitOutcome.SPLIT_ABOVE) {
                                BlockEditState(
                                    pastedLines?.get(1) ?: listOf(RichTextSpan(text = lines[1])),
                                    TextFieldValue(RUN_LEAD + lines[1], TextRange(RUN_LEAD.length))
                                )
                            } else {
                                BlockEditState(
                                    spansForFirstLine,
                                    TextFieldValue(
                                        RUN_LEAD + firstLine,
                                        TextRange(RUN_LEAD.length + firstLine.length)
                                    )
                                )
                            }
                            if (outcome == SplitOutcome.BECAME_PARAGRAPH) {
                                // Invio su una casella vuota: il blocco
                                // diventa un paragrafo e questa riga viene
                                // distrutta, esattamente come col backspace
                                // a inizio casella. Stesso rimedio: il fuoco
                                // si posa sul campo invisibile invece di
                                // staccare la tastiera.
                                focusPark?.requestFocus()
                            }
                        } else {
                            // Più righe in un colpo solo (un incolla): le
                            // altre diventano blocchi sotto e questo tiene
                            // la prima, come sempre. Anche due sole, se sono
                            // righe copiate col loro tipo (vedi `PastedLine`).
                            editState = BlockEditState(
                                spansForFirstLine,
                                TextFieldValue(
                                    RUN_LEAD + firstLine,
                                    TextRange(RUN_LEAD.length + firstLine.length)
                                )
                            )
                            // Il tipo delle righe copiate intere da notE.
                            var lineAt = 0
                            val lineKinds = lines.map { line ->
                                pasted.lineAt(lineAt, lineAt + line.length).also { lineAt += line.length + 1 }
                            }
                            viewModel.splitBlockAtMultiple(block, lines, pastedLines, lineKinds)
                        }
                    } else if (newText == oldText) {
                        editState = editState.copy(fieldValue = newFieldValue)
                    } else {
                        // **Da qui in poi non c'è più nessuna eco da
                        // aspettarsi.** L'eco della tastiera, se arriva,
                        // arriva subito dopo la divisione e senza niente in
                        // mezzo: appena viene battuto un carattere vero, la
                        // divisione di prima è acqua passata.
                        //
                        // Senza questa riga il riconoscimento restava armato
                        // sul campo, che adesso sopravvive a tutte le
                        // divisioni di fila (prima il fuoco se ne andava su
                        // un campo nuovo e la memoria ripartiva da zero).
                        // Scrivendo in fretta due righe che finiscono con lo
                        // stesso testo, il secondo Invio veniva scambiato
                        // per un'eco e **non andava a capo**.
                        lastSplit = null
                        // Stessa scorciatoia del testo condiviso: "5." scritto
                        // da solo in una riga di testo la fa diventare un
                        // elenco numerato che parte da 5, e il "5." sparisce
                        // perché il numero da lì in poi lo disegna la lista.
                        val caretInBody = (newFieldValue.selection.end - RUN_LEAD.length).coerceAtLeast(0)
                        val listPrefix = if (block.type == BlockType.PARAGRAPH) {
                            numberedListPrefixJustTyped(oldText, newText, caretInBody)
                        } else {
                            null
                        }
                        if (listPrefix != null &&
                            viewModel.startNumberedListInBlock(block.id, listPrefix.startNumber)
                        ) {
                            val stripped = newText.removeRange(listPrefix.lineStart, caretInBody)
                            editState = BlockEditState(
                                listOf(RichTextSpan(text = stripped)),
                                TextFieldValue(RUN_LEAD + stripped, TextRange(RUN_LEAD.length + listPrefix.lineStart))
                            )
                            return@BasicTextField
                        }
                        // E "---" da solo, come nel testo condiviso: la riga
                        // diventa un divisore, col cursore sulla riga nuova
                        // sotto. Il campo sta per sparire col cursore dentro:
                        // prima il fuoco va sul campo invisibile, come per
                        // "Divider" dal menu "/".
                        val divider = if (block.type == BlockType.PARAGRAPH && newText in DIVIDER_SHORTCUTS) {
                            dividerShortcutJustTyped(oldText, newText, caretInBody)
                        } else {
                            null
                        }
                        if (divider != null) {
                            editState = BlockEditState(
                                listOf(RichTextSpan(text = "")),
                                TextFieldValue(RUN_LEAD, TextRange(RUN_LEAD.length))
                            )
                            focusPark?.requestFocus()
                            viewModel.convertToDividerAndFocusNext(block)
                            return@BasicTextField
                        }
                        // Stessa scorciatoia del testo condiviso: `||x||`
                        // toglie le barre e copre quello che c'era in mezzo.
                        val spoiler = spoilerJustClosed(
                            oldText,
                            newText,
                            (newFieldValue.selection.end - RUN_LEAD.length).coerceAtLeast(0)
                        )
                        if (spoiler != null) {
                            val stripped = newText
                                .removeRange(spoiler.contentEnd, spoiler.contentEnd + 2)
                                .removeRange(spoiler.open, spoiler.open + 2)
                            val to = spoiler.contentEnd - 2
                            val newSpans = setFormatInRange(
                                applyTextEdit(editState.spans, oldText, stripped),
                                spoiler.open,
                                to
                            ) { s, _ -> s.copy(spoiler = true) }
                            editState = BlockEditState(
                                newSpans,
                                TextFieldValue(RUN_LEAD + stripped, TextRange(RUN_LEAD.length + to))
                            )
                            viewModel.updateBlockSpans(block, newSpans)
                        } else {
                            val newSpans = applyPaste(applyTextEdit(editState.spans, oldText, newText), pasted)
                            editState = BlockEditState(newSpans, newFieldValue)
                            viewModel.updateBlockSpans(block, newSpans)
                        }
                    }
                },
                textStyle = textStyle.copy(color = NotionWhite),
                cursorBrush = SolidColor(NotionWhite),
                // Vedi il gemello nel campo unito: bloccata si legge, non si
                // scrive.
                readOnly = LocalPageLocked.current,
                // La maiuscola a inizio frase la mette la tastiera, ma solo
                // se glielo si chiede: senza, ogni casella da spuntare
                // cominciava minuscola mentre nel resto del telefono no.
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                visualTransformation = islandTransformation,
                onTextLayout = { islandLayout = it },
                // Una notifica senza testo mostrerà il nome della pagina: il
                // suggerimento lo dice, finché non si scrive niente.
                decorationBox = { inner ->
                    Box {
                        if (block.type == BlockType.NOTIFICATION && bodyOf(editState.fieldValue.text).isEmpty()) {
                            Text(NotifyStrings.textHint, style = textStyle, color = NotionGray400, maxLines = 1)
                        }
                        if (block.type == BlockType.QUOTE && bodyOf(editState.fieldValue.text).isEmpty()) {
                            Text(BlockStrings.emptyQuote, style = textStyle, color = NotionGray400, maxLines = 1)
                        }
                        inner()
                        proofMenu?.let { (word, at) ->
                            val text = bodyOf(editState.fieldValue.text)
                            if (word.end <= text.length) {
                                ProofingMenu(
                                    word = text.substring(word.start, word.end),
                                    anchor = at,
                                    onReplace = { with ->
                                        // Come se la parola la si fosse scritta: stessi span, stesso salvataggio.
                                        val old = bodyOf(editState.fieldValue.text)
                                        if (word.end <= old.length) {
                                            val new = old.replaceRange(word.start, word.end, with)
                                            val newSpans = applyTextEdit(editState.spans, old, new)
                                            editState = BlockEditState(
                                                newSpans,
                                                TextFieldValue(RUN_LEAD + new, TextRange(RUN_LEAD.length + word.start + with.length))
                                            )
                                            proofTyping = null
                                            viewModel.updateBlockSpans(block, newSpans)
                                        }
                                    },
                                    onDismiss = { proofMenu = null }
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .sideLane(sideLane)
                    .bringIntoViewRequester(islandCaret)
                    .drawBehind {
                        drawMisspellings(islandLayout)
                        drawBlockLinkArrows(islandLayout, TextLinkColor)
                    }
                    .linkTaps(layout = { islandLayout }, onLink = openLink)
                    .proofingTaps(enabled = proofing != null && !pageLocked, layout = { islandLayout }) { word, at ->
                        proofMenu = word to at
                    }
                    // Col tasto destro, come nel campo unito.
                    .rightClickCaret(
                        enabled = isPc && !pageLocked,
                        layout = { islandLayout },
                        value = { editState.fieldValue },
                        transformation = { islandTransformation },
                        focused = { fieldFocused }
                    ) { moved ->
                        editState = editState.copy(fieldValue = moved)
                        focusRequester.requestFocus()
                    }
                    .focusRequester(focusRequester)
                    .onFocusChanged { state ->
                        noteLog(
                            "casella ${block.id.takeLast(4)} fuoco -> ${state.isFocused} " +
                                "(cursore ora a ${editState.fieldValue.selection.start})"
                        )
                        fieldFocused = state.isFocused
                        onFocusChanged(block.id, state.isFocused)
                    }
            )
        }
        }

        if (block.type == BlockType.NOTIFICATION) NotificationTimeChip(block, viewModel)
    }

    if (editingCalloutIcon) {
        CalloutIconSheet(
            store = rowImageStore,
            hasIcon = block.attrs.hasCalloutIcon,
            onPick = { choice -> viewModel.updateBlockAttrs(block.id) { it.withIcon(choice) } },
            onDismiss = { editingCalloutIcon = false }
        )
    }

    val menuBlockId = numberMenuFor
    if (menuBlockId != null) {
        NumberedListMenu(
            hasReset = block.numberStartsAt != null,
            onDismiss = { numberMenuFor = null },
            onBeginNewList = { viewModel.beginNewListHere(menuBlockId) },
            onContinuePrevious = { viewModel.continuePreviousList(menuBlockId) },
            onRemoveNumber = { viewModel.removeNumber(menuBlockId) },
            onChangeToBullet = { viewModel.changeToBullet(menuBlockId) }
        )
    }

    val children = allBlocks
        .filter { it.parentBlockId == block.id }
        .sortedBy { it.orderIndex }
    val isOpenToggle = block.type == BlockType.TOGGLE && block.isExpanded
    val shouldShowChildren = children.isNotEmpty() &&
        (block.type != BlockType.TOGGLE || block.isExpanded)

    if (shouldShowChildren) {
        Column {
            children.forEach { child ->
                // **Ogni riga col suo `key`.** Senza, Compose riconosce le
                // righe dalla posizione e non da chi sono. L'Invio in una
                // riga dentro un toggle fa nascere la riga nuova **sopra**
                // (vedi `splitBlockAt`, è quello che tiene ferma la
                // tastiera): la riga col cursore scendeva di un posto, ma
                // il suo campo restava al posto di prima e si ritrovava a
                // mostrare la riga nuova. Il registro l'ha mostrato
                // chiaro: dopo l'Invio la lettera successiva finiva nella
                // riga di sopra, attaccata — "1", Invio, "2" dava "12".
                // Il testo di primo livello non aveva il problema perché
                // la lista che lo contiene le chiavi le ha già.
                key(child.id) {
                    // Lo spazio fra i blocchi ("Paragraph") anche fra le righe
                    // dentro un toggle, metà sopra e metà sotto come fuori.
                    Box(modifier = Modifier.padding(vertical = (paragraphLook.spacingOf(child) / 2f).sp.toDpIn(density))) {
                        BlockRow(
                            block = child,
                            allBlocks = allBlocks,
                            depth = depth + 1,
                            viewModel = viewModel,
                            factory = factory,
                            focusRequestBlockId = focusRequestBlockId,
                            formatRequest = formatRequest,
                            onFocusChanged = onFocusChanged,
                            onNavigateToPage = onNavigateToPage,
                            onNavigateToDatabase = onNavigateToDatabase,
                            onOpenBlockMenu = onOpenBlockMenu
                        )
                    }
                }
            }
        }
    } else if (isOpenToggle && !LocalPageLocked.current) {
        // **Un toggle aperto e vuoto dice che è vuoto, e si tocca per
        // scriverci dentro.** Prima non mostrava niente, e il pulsante
        // per aggiungere compariva solo se c'era già almeno un figlio:
        // in un toggle nuovo non c'era nessun modo di entrare. È la
        // stessa riga grigia che mostra Notion.
        //
        // Con dei figli invece non c'è più nessun pulsante: si aggiunge
        // con l'Invio, dal titolo o dall'ultima riga, come su Notion.
        Text(
            text = EditorStrings.emptyToggle,
            style = MaterialTheme.typography.bodyLarge,
            color = NotionGray400,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.addFirstChild(block) }
                .padding(
                    start = PAGE_SIDE_PADDING + NESTED_INDENT * (depth + 1),
                    end = PAGE_SIDE_PADDING,
                    top = 4.dp,
                    bottom = 4.dp
                )
        )
    }
}

/**
 * Il quadratino di una casella da spuntare.
 *
 * Disegnato a mano invece di usare `Checkbox` di Material: quello
 * porta con sé una zona di tocco da 48dp e un quadrato da 20 che non
 * si possono rimpicciolire, e accanto a una riga di testo alta 24
 * risultava un bottone messo lì in mezzo, più grande delle lettere e
 * disallineato. Qui il quadrato è alto quanto le lettere e sta
 * **centrato sulla prima riga** del testo: la zona di tocco resta
 * larga 24 tutt'intorno, così resta comodo da centrare col dito.
 */
@Composable
private fun CheckboxMark(
    checked: Boolean,
    onToggle: () -> Unit
) {
    val shape = RoundedCornerShape(4.dp)
    // Centrato sulla riga, che ha il testo in mezzo sia nel campo
    // condiviso sia dentro un toggle: non va alzato a mano. (Fino al
    // 25/09/2026 dentro un toggle lo si alzava di 4,5dp, perché lì il
    // testo stava più in alto; ora le due righe sono fatte uguali.)
    Box(
        modifier = Modifier
            .size(CHECKBOX_TOUCH_SIZE)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(CHECKBOX_SIZE)
                .clip(shape)
                .then(
                    if (checked) {
                        Modifier.background(MaterialTheme.colorScheme.primary)
                    } else {
                        Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, shape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/** Testo + selezione di un blocco isola, insieme agli span che li descrivono — un unico stato così i due non possono mai disallinearsi. */
private data class BlockEditState(val spans: List<RichTextSpan>, val fieldValue: TextFieldValue)

/**
 * Colora il testo visualizzato secondo gli span del blocco (grassetto,
 * corsivo, sottolineato, barrato), senza toccare il testo grezzo che il
 * campo sta effettivamente modificando — usata solo dai blocchi isola
 * (checkbox, toggle), che restano su span per blocco singolo.
 */
private class RichTextVisualTransformation(
    private val spans: List<RichTextSpan>,
    /** Vedi il gemello nel campo unito: serve a scoprire gli spoiler toccati dal cursore. */
    private val caret: IntRange? = null,
    /** Vedi i gemelli nel campo unito. */
    private val proof: ((String) -> List<IntRange>)? = null,
    private val typingAt: Int? = null
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // L'a-capo nascosto in testa non si deve vedere: quello che si
        // mostra è il testo senza, e le posizioni vanno tradotte di
        // uno. Se non c'è (un istante di disallineamento, o un campo
        // che non lo usa) la traduzione è l'identità.
        val hasLead = text.text.startsWith(RUN_LEAD)
        // Gli span sono la verità sulla formattazione, ma il testo del
        // campo è la verità su cosa c'è scritto: quando i due non
        // combaciano — succede per un fotogramma mentre si scrive — si
        // mostra il testo, altrimenti sparirebbe la lettera appena
        // battuta.
        val body = if (hasLead) text.text.substring(RUN_LEAD.length) else text.text
        val mapping = if (hasLead) LeadOffsetMapping else OffsetMapping.Identity
        // Vedi il gemello nel campo unito: quando i due divergono — per
        // un fotogramma, mentre si scrive — la stessa modifica si applica
        // agli span invece di rinunciare a disegnarli. Qui non c'è
        // nessuna corrispondenza fra righe e blocchi da poter sbagliare:
        // il blocco è uno solo.
        val storedText = spans.plainText()
        val effective = if (storedText == body) spans else applyTextEdit(spans, storedText, body)
        val builder = AnnotatedString.Builder()
        var pos = 0
        for ((spanIndex, span) in effective.withIndex()) {
            val start = pos
            builder.append(span.text)
            pos += span.text.length
            builder.markBlockLinkEnd(span, effective.getOrNull(spanIndex + 1), start, pos)
            if (span.bold || span.italic || span.underline || span.strikethrough || span.link != null) {
                val decorations = mutableListOf<TextDecoration>()
                // Vedi il gemello nel campo unito: un collegamento è sottolineato.
                if (span.underline || span.link != null) decorations.add(TextDecoration.Underline)
                if (span.strikethrough) decorations.add(TextDecoration.LineThrough)
                builder.addStyle(
                    SpanStyle(
                        fontWeight = if (span.bold) FontWeight.Bold else null,
                        fontStyle = if (span.italic) FontStyle.Italic else null,
                        textDecoration = if (decorations.isEmpty()) null else TextDecoration.combine(decorations)
                    ),
                    start,
                    pos
                )
            }
            linkStyleOf(span)?.let { builder.addStyle(it, start, pos) }
            colorStyleOf(span)?.let { builder.addStyle(it, start, pos) }
            fontStyleOf(span)?.let { builder.addStyle(it, start, pos) }
            scriptStyleOf(span)?.let { builder.addStyle(it, start, pos) }
            if (span.spoiler) {
                // Dentro, non sui bordi: vedi il gemello nel campo unito.
                val revealed = caret != null && caret.first < pos && caret.last > start
                builder.addStyle(spoilerStyle(revealed), start, pos)
                if (revealed) builder.markLink(span, start, pos)
            } else {
                builder.markLink(span, start, pos)
            }
        }
        proof?.let { p -> builder.markMisspellings(0, p(body), 0, effective, typingAt?.takeIf { it in 0..body.length }) }
        return TransformedText(builder.toAnnotatedString().withEmojiFont(), mapping)
    }
}

/**
 * Le posizioni di un campo che comincia con l'a-capo nascosto: sullo
 * schermo tutto è spostato indietro di uno, e nessuna posizione
 * mostrata può corrispondere allo zero — il cursore non riesce a
 * mettersi prima di quell'a-capo, ed è proprio per questo che
 * "l'a-capo è sparito" vuol dire senza ambiguità "backspace a inizio
 * riga".
 */
private object LeadOffsetMapping : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int =
        (offset - RUN_LEAD.length).coerceAtLeast(0)

    override fun transformedToOriginal(offset: Int): Int = offset + RUN_LEAD.length
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PageLinkBlockContent(
    block: BlockEntity,
    depth: Int,
    viewModel: PageEditorViewModel,
    onNavigateToPage: (String) -> Unit,
    onNavigateToDatabase: (String) -> Unit,
    /** Il dito tenuto sul collegamento: apre il menu del blocco. Null su una pagina bloccata. */
    onLongPress: (() -> Unit)? = null
) {
    val linkedPageId = block.linkedPageId
    // **Seguita nel tempo, non letta una volta sola.** Prima il nome e
    // l'icona si leggevano all'apertura e restavano quelli: andava bene
    // finché si potevano cambiare solo da dentro la pagina collegata, ma
    // "Rename" ed "Edit icon" del menu del blocco li cambiano da qui, e
    // la riga deve mostrarli subito.
    val linkedPage by remember(linkedPageId) {
        if (linkedPageId != null) viewModel.observePageInfo(linkedPageId) else flowOf(null)
    }.collectAsStateWithLifecycle(initialValue = null)

    // Il colore dato dal menu del blocco ("Color"): sta in uno span vuoto
    // del collegamento, vedi `PageEditorViewModel.colorWholeBlock`.
    val linkSpan = viewModel.spansOf(block).firstOrNull()
    val nameColor = hexToColor(linkSpan?.color) ?: NotionWhite
    val nameBackground = hexToColor(linkSpan?.background)

    // Sul PC nel modo scelto in Settings (vedi `PageOpener`); altrove a pagina intera.
    val opener = LocalPageOpener.current
    val open: () -> Unit = {
        linkedPageId?.let { id ->
            // Un collegamento può puntare anche a un **database**: è
            // quello che lascia "Turn into page". Aprirlo come pagina
            // di testo mostrerebbe un foglio vuoto al posto delle
            // righe, quindi conta cosa c'è dall'altra parte, non solo
            // il tipo del blocco.
            val isDatabase = block.type == BlockType.DATABASE_LINK || linkedPage?.isDatabase == true
            when {
                opener != null -> opener.open(id, isDatabase, null)
                isDatabase -> onNavigateToDatabase(id)
                else -> onNavigateToPage(id)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = PAGE_SIDE_PADDING + (depth * 20).dp, end = PAGE_SIDE_PADDING, top = 4.dp, bottom = 4.dp)
            .onRightClick(onLongPress)
            .combinedClickable(
                enabled = linkedPageId != null,
                onClick = open,
                onLongClick = onLongPress
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val linkedEmoji = linkedPage?.chosenEmoji
        val linkedSymbol = linkedPage?.iconSymbol
        if (block.type == BlockType.DATABASE_LINK && linkedPage?.iconImage == null && linkedSymbol == null && linkedEmoji == null) {
            Icon(
                Icons.Filled.TableChart,
                contentDescription = null,
                tint = NotionWhite,
                modifier = Modifier.size(20.dp)
            )
        } else {
            // Se la pagina collegata ha un'immagine al posto
            // dell'emoji, qui si vede quella: altrimenti la stessa
            // pagina avrebbe due facce diverse a seconda di dove la si
            // guarda.
            val linkedIconImage = linkedPage?.iconImage
            if (linkedIconImage != null) {
                val linkContext = LocalPlatformContext.current
                PageImage(
                    fileName = linkedIconImage,
                    store = remember(linkContext) { PageImageStore(linkContext) },
                    contentDescription = null,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            } else if (linkedSymbol != null) {
                SymbolIcon(linkedSymbol, 22.dp)
            } else if (linkedPage?.isDatabase == true && linkedEmoji == null) {
                // Un database senza icona sua non ha un'emoji da mostrare:
                // il 📄 scritto nel suo campo è solo il valore di riserva
                // delle pagine. Qui si vede il segno della tabella, come
                // quando il database sta dentro la pagina.
                Icon(
                    Icons.Filled.TableChart,
                    contentDescription = null,
                    tint = NotionWhite,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(linkedPage?.icon ?: "📄", style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(modifier = Modifier.size(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            Text(
                text = linkedPage?.title?.ifBlank { Strings.untitled } ?: Strings.untitled,
                color = nameColor,
                style = MaterialTheme.typography.bodyLarge,
                modifier = if (nameBackground != null) {
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(nameBackground)
                        .padding(horizontal = 4.dp)
                } else {
                    Modifier
                }
            )
        }
        if (linkedPage?.isProtected == true) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = LockStrings.passwordLock,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
        // **Niente cestino a destra** (tolto il 26/09/2026, su richiesta
        // dell'utente): per eliminare si tiene premuto il collegamento, e
        // dal menu del blocco c'è "Move to trash".
    }
}

/**
 * Il divisore non ha campo di testo, quindi niente backspace-elimina:
 * tenendolo premuto si chiede se toglierlo.
 */
@Composable
private fun DividerBlockContent(
    block: BlockEntity,
    depth: Int,
    viewModel: PageEditorViewModel,
    /**
     * **Il tocco lungo apre il menu del blocco** (dal 05/10/2026, chiesto
     * dall'utente), quello della maniglia a sei puntini, con le sole voci
     * che hanno senso per un divisore: "Add above", "Add below" e "Delete"
     * (vedi `BlockActionsHost`). Prima chiedeva soltanto se eliminarlo.
     * Null in una pagina bloccata: lì non fa niente.
     */
    onLongPress: (() -> Unit)?
) {
    // "Preso" dal primo backspace sotto di lui (dal 29/09/2026): si accende
    // d'azzurro, e il secondo backspace lo toglie. Vedi
    // `PageEditorViewModel.backspaceUnderDivider`.
    val armedDivider by viewModel.armedDivider.collectAsStateWithLifecycle()
    val armed = armedDivider == block.id

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = PAGE_SIDE_PADDING + (depth * 20).dp, end = PAGE_SIDE_PADDING)
            .background(
                if (armed) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
                RoundedCornerShape(4.dp)
            )
            .padding(vertical = 12.dp)
            .pointerInput(onLongPress) {
                detectTapGestures(onLongPress = onLongPress?.let { open -> { _ -> open() } })
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(if (armed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
        )
    }
}

/** Le celle scelte col mouse in una tabella: le righe e le colonne del rettangolo. */
data class CellRange(val rows: IntRange, val cols: IntRange) {
    fun contains(row: Int, col: Int) = row in rows && col in cols
}

@Composable
fun TableBlockContent(
    block: BlockEntity,
    depth: Int,
    viewModel: PageEditorViewModel
) {
    // Null finché il database non ha risposto. Ogni casella tiene il suo
    // testo in uno stato locale nato col primo valore che vede: con una
    // lista vuota di partenza nasceva vuota e restava vuota, quindi il
    // testo scritto in una tabella spariva riaprendo la pagina (nel
    // database c'era ancora). La chiave `loaded` fa rinascere le caselle
    // una volta sola, quando le celle arrivano.
    val cells by remember(block.id) { viewModel.tableCellsFlow(block.id) }
        .collectAsStateWithLifecycle(initialValue = null)
    val loaded = cells != null
    val cellMap = remember(cells) {
        cells.orEmpty().associate { (it.rowIndex to it.colIndex) to it.text }
    }
    // **"+ Row", "+ Column" e il cestino solo mentre si scrive nella tabella**
    // (dal 21/10/2026, chiesto dall'utente): sempre accesi restavano in mezzo
    // al testo, sotto ogni tabella di un articolo preso col Web Clipper.
    var editing by remember(block.id) { mutableStateOf(false) }
    // Il testo di ogni casella, nato coi valori del database quando arrivano
    // (vedi sopra): qui e non dentro la casella, così svuotare più celle
    // insieme le svuota anche sullo schermo.
    val texts = remember(block.id, loaded) { mutableStateMapOf<Pair<Int, Int>, String>().apply { putAll(cellMap) } }

    // **Più celle col mouse** (dal 10/11/2026, chiesto dall'utente sul PC):
    // trascinando da una cella all'altra si scelgono le celle del rettangolo.
    // Canc o Backspace le svuotano, e scelte tutte se ne va la tabella; Ctrl+C
    // le copia (una riga per riga, tabulazioni fra le colonne), Ctrl+X anche;
    // Ctrl+A le sceglie tutte; Esc o un clic altrove le lasciano.
    var picked by remember(block.id) { mutableStateOf<CellRange?>(null) }
    val cellBounds = remember(block.id) { HashMap<Pair<Int, Int>, Rect>() }
    val tableCoordinates = remember { AtomicReference<LayoutCoordinates?>(null) }
    val cellKeys = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val context = LocalPlatformContext.current
    val veil = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    val locked = LocalPageLocked.current
    LaunchedEffect(picked != null) { if (picked != null) runCatching { cellKeys.requestFocus() } }
    fun clearPicked(range: CellRange) {
        if (range.rows == 0 until block.tableRows && range.cols == 0 until block.tableCols) {
            viewModel.deleteBlock(block)
            return
        }
        for (r in range.rows) for (c in range.cols) {
            texts[r to c] = ""
            viewModel.setTableCell(block.id, r, c, "")
        }
    }
    fun copyPicked(range: CellRange) {
        val rows = range.rows.map { r -> range.cols.joinToString("\t") { c -> texts[r to c].orEmpty().replace('\n', ' ') } }
        copyPageToClipboard(context, rows.map { PageEditorViewModel.PageCopyLine(listOf(RichTextSpan(text = it)), null) })
    }

    Column(
        modifier = Modifier
            .padding(start = PAGE_SIDE_PADDING + (depth * 20).dp, end = PAGE_SIDE_PADDING, top = 8.dp, bottom = 8.dp)
            .onFocusChanged { editing = it.hasFocus }
            .focusGroup()
            .onGloballyPositioned { tableCoordinates.set(it) }
            .pointerInput(block.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    if (down.type != PointerType.Mouse || !currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
                    val table = tableCoordinates.get() ?: return@awaitEachGesture
                    fun cellAt(position: Offset): Pair<Int, Int>? {
                        val window = runCatching { table.localToWindow(position) }.getOrNull() ?: return null
                        return cellBounds.entries.firstOrNull { it.value.contains(window) }?.key
                    }
                    picked = null
                    val start = cellAt(down.position) ?: return@awaitEachGesture
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        // La rotella fa scorrere la pagina anche mentre si scelgono le celle.
                        if (event.type == PointerEventType.Scroll) continue
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        // Fuori dalla tabella la prende la pagina (più blocchi, vedi `BlockSelection.kt`).
                        if (!change.pressed || change.isConsumed) break
                        val now = cellAt(change.position) ?: continue
                        if (now == start && picked == null) continue
                        change.consume()
                        if (picked == null) focusManager.clearFocus()
                        picked = CellRange(minOf(start.first, now.first)..maxOf(start.first, now.first), minOf(start.second, now.second)..maxOf(start.second, now.second))
                    }
                }
            }
    ) {
        // I tasti delle celle scelte li prende un punto invisibile col fuoco.
        Box(
            Modifier
                .size(1.dp)
                .focusRequester(cellKeys)
                .onFocusChanged { if (!it.isFocused) picked = null }
                .focusable()
                .onKeyEvent { event ->
                    val range = picked ?: return@onKeyEvent false
                    when (selectionKeyOf(event)) {
                        SelectionKey.DELETE -> if (!locked) {
                            picked = null
                            clearPicked(range)
                        }
                        SelectionKey.COPY -> copyPicked(range)
                        SelectionKey.CUT -> if (!locked) {
                            copyPicked(range)
                            picked = null
                            clearPicked(range)
                        }
                        SelectionKey.ALL -> picked = CellRange(0 until block.tableRows, 0 until block.tableCols)
                        SelectionKey.CLOSE -> picked = null
                        null -> return@onKeyEvent false
                    }
                    true
                }
        )
        for (r in 0 until block.tableRows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (c in 0 until block.tableCols) {
                    key(r, c, loaded) {
                        AutoCaps {
                            BasicTextField(
                                value = texts[r to c] ?: "",
                                onValueChange = { newText ->
                                    texts[r to c] = newText
                                    viewModel.setTableCell(block.id, r, c, newText)
                                },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = NotionWhite),
                                cursorBrush = SolidColor(NotionWhite),
                                modifier = Modifier
                                    .weight(1f)
                                    .onGloballyPositioned { cellBounds[r to c] = it.boundsInWindow() }
                                    .background(if (picked?.contains(r, c) == true) veil else Color.Transparent)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline)
                                    .padding(8.dp)
                                    .markFocusChanges()
                            )
                        }
                    }
                }
            }
        }

        if (editing) Row(modifier = Modifier.padding(top = 4.dp)) {
            TextButton(onClick = { viewModel.resizeTable(block, block.tableRows + 1, block.tableCols) }) {
                Text("+ Row", style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = { viewModel.resizeTable(block, block.tableRows, block.tableCols + 1) }) {
                Text("+ Column", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = { viewModel.deleteBlock(block) }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = EditorStrings.deleteTable,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * **Il menu del tasto destro sul PC** (dal 13/11/2026, chiesto dall'utente:
 * "mostrami le stesse identiche impostazioni della toolbar sopra la tastiera
 * del telefono"): le voci della barra, nello stesso ordine, una sotto l'altra,
 * dai sei puntini a Undo e Redo. Aa è un sottomenu con quello che sul telefono
 * è la seconda barra (font, corpo, B, I, U, S, pedice, apice, spoiler); il "+"
 * e il font aprono il loro elenco nello stesso punto. Taglia, copia e incolla
 * sono quelli veri del campo ([PcMenuOpen.edit]), come Ctrl+X, Ctrl+C, Ctrl+V.
 */
@Composable
private fun PcPageMenu(
    open: PcMenuOpen,
    /** La riga col cursore: il clic destro ce l'ha appena messo (vedi `rightClickCaret`). */
    focused: BlockEntity?,
    viewModel: PageEditorViewModel,
    canUndo: Boolean,
    canRedo: Boolean,
    selectionLink: PageEditorViewModel.SelectionLink,
    currentFont: PageFont?,
    currentSize: Int,
    onKind: (PcMenuKind) -> Unit,
    onClose: () -> Unit,
    onBlockMenu: (BlockEntity) -> Unit,
    onSelectAll: () -> Unit,
    onTypeEntry: (BlockEntity, SlashEntry) -> Unit,
    onParagraph: (BlockEntity?) -> Unit,
    onFontSize: () -> Unit,
    onFont: (PageFont?) -> Unit,
    onColors: () -> Unit,
    onLink: () -> Unit
) {
    when (open.kind) {
        PcMenuKind.BLOCK_TYPE -> BlockTypeMenu(expanded = true, onDismiss = onClose, closeOnClickOutside = true) { entry ->
            onClose()
            focused?.let { onTypeEntry(it, entry) }
        }
        PcMenuKind.FONT -> FontMenu(expanded = true, current = currentFont, onDismiss = onClose) { font ->
            onClose()
            onFont(font)
        }
        PcMenuKind.MAIN -> {
            val clipboard = LocalClipboardManager.current as? LinkClipboard
            fun icon(vector: ImageVector): @Composable () -> Unit = { Icon(vector, contentDescription = null, modifier = Modifier.size(16.dp)) }
            fun glyph(
                text: String,
                weight: FontWeight? = null,
                style: FontStyle? = null,
                decoration: TextDecoration? = null
            ): @Composable () -> Unit = {
                Text(text, color = LocalContentColor.current, fontSize = 13.sp, fontWeight = weight, fontStyle = style, textDecoration = decoration)
            }
            /** Un'azione sulla riga col cursore; senza riga la voce è spenta. */
            fun onLine(action: (BlockEntity) -> Unit): (() -> Unit)? = focused?.let { block -> { action(block) } }
            fun format(type: FormatType): (() -> Unit)? = onLine { viewModel.requestFormat(it.id, type) }
            fun paste(mode: PasteMode) {
                clipboard?.nextPasteMode = mode
                open.edit.paste()
            }
            val entries = buildList {
                add(PcMenuItem(EditorStrings.blockOptions, icon(Icons.Filled.DragIndicator), enabled = focused != null, onClick = onLine(onBlockMenu)))
                add(PcMenuSeparator)
                add(PcMenuItem(EditorStrings.copy, icon(Icons.Filled.ContentCopy), hint = "Ctrl+C", onClick = open.edit.copy))
                // Come sul telefono: il clic incolla, e il modo si sceglie a parte (lì tenendo premuto, qui passando sopra).
                add(
                    PcMenuItem(
                        EditorStrings.paste, icon(Icons.Filled.ContentPaste), hint = "Ctrl+V",
                        submenu = listOf(
                            PcMenuItem(EditorStrings.pasteKeepSource, onClick = { paste(PasteMode.KEEP_SOURCE) }),
                            PcMenuItem(EditorStrings.pasteMatchDestination, onClick = { paste(PasteMode.MATCH_DESTINATION) }),
                            PcMenuItem(EditorStrings.pasteTextOnly, onClick = { paste(PasteMode.TEXT_ONLY) })
                        ),
                        onClick = { paste(PasteMode.KEEP_SOURCE) }
                    )
                )
                add(PcMenuItem(EditorStrings.cut, icon(Icons.Filled.ContentCut), hint = "Ctrl+X", onClick = open.edit.cut))
                add(PcMenuItem(EditorStrings.selectAll, icon(Icons.Filled.SelectAll), hint = "Ctrl+A", onClick = onSelectAll))
                add(PcMenuSeparator)
                add(PcMenuItem(EditorStrings.outdent, icon(Icons.Filled.FormatIndentDecrease), enabled = focused != null, onClick = onLine { viewModel.outdentBlock(it) }))
                add(PcMenuItem(EditorStrings.indent, icon(Icons.Filled.FormatIndentIncrease), enabled = focused != null, onClick = onLine { viewModel.indentBlock(it) }))
                add(PcMenuItem(EditorStrings.moveUp, icon(Icons.Filled.ArrowUpward), enabled = focused != null, onClick = onLine { viewModel.moveBlockUp(it) }))
                add(PcMenuItem(EditorStrings.moveDown, icon(Icons.Filled.ArrowDownward), enabled = focused != null, onClick = onLine { viewModel.moveBlockDown(it) }))
                add(PcMenuSeparator)
                add(
                    PcMenuItem(EditorStrings.addOrChangeBlock, icon(Icons.Filled.Add), enabled = focused != null, onClick = {
                        viewModel.captureSelection()
                        onKind(PcMenuKind.BLOCK_TYPE)
                    })
                )
                add(PcMenuItem(ParagraphStrings.paragraph, icon(Icons.Filled.FormatLineSpacing), enabled = focused != null, onClick = { onParagraph(focused) }))
                // Il codice ha i suoi colori, quelli del linguaggio: niente Aa né pennello, come nella barra.
                if (focused != null && focused.type != BlockType.CODE) {
                    add(
                        PcMenuItem(
                            EditorStrings.textFormatting, glyph("Aa", weight = FontWeight.Bold),
                            submenu = listOf(
                                PcMenuItem(EditorStrings.font, hint = currentFont?.label ?: EditorStrings.defaultFont, onClick = {
                                    viewModel.captureSelection()
                                    onKind(PcMenuKind.FONT)
                                }),
                                PcMenuItem(EditorStrings.fontSize, hint = currentSize.toString(), onClick = onFontSize),
                                PcMenuSeparator,
                                PcMenuItem(EditorStrings.bold, glyph("B", weight = FontWeight.Bold), onClick = format(FormatType.BOLD)),
                                PcMenuItem(EditorStrings.italic, glyph("I", style = FontStyle.Italic), onClick = format(FormatType.ITALIC)),
                                PcMenuItem(EditorStrings.underline, glyph("U", decoration = TextDecoration.Underline), onClick = format(FormatType.UNDERLINE)),
                                PcMenuItem(EditorStrings.strikethrough, glyph("S", decoration = TextDecoration.LineThrough), onClick = format(FormatType.STRIKETHROUGH)),
                                PcMenuItem(EditorStrings.subscript, glyph("x\u2082"), onClick = format(FormatType.SUBSCRIPT)),
                                PcMenuItem(EditorStrings.superscript, glyph("x\u00B2"), onClick = format(FormatType.SUPERSCRIPT)),
                                PcMenuItem(EditorStrings.spoiler, icon(Icons.Filled.VisibilityOff), onClick = format(FormatType.SPOILER))
                            )
                        )
                    )
                    add(PcMenuItem(EditorStrings.textAndBackgroundColor, icon(Icons.Filled.Brush), onClick = onColors))
                    if (selectionLink.hasText || selectionLink.link != null || selectionLink.onSite) {
                        add(PcMenuItem(EditorStrings.link, icon(Icons.Filled.Link), onClick = onLink))
                    }
                }
                add(PcMenuSeparator)
                add(PcMenuItem(EditorStrings.undo, icon(Icons.Filled.Undo), hint = "Ctrl+Z", enabled = canUndo, onClick = { viewModel.undo() }))
                add(PcMenuItem(EditorStrings.redo, icon(Icons.Filled.Redo), hint = "Ctrl+Y", enabled = canRedo, onClick = { viewModel.redo() }))
            }
            PcContextMenu(entries, onDismiss = onClose)
        }
    }
}
