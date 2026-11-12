package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.BLOCK_SPACING_STEPS
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.ListGaps
import com.gabriele.notionlocal.data.entity.MARKER_GAP_STEPS
import com.gabriele.notionlocal.data.entity.MARKER_LIST_TYPES
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.PageParagraph
import com.gabriele.notionlocal.data.entity.ParagraphLook
import com.gabriele.notionlocal.data.entity.heldListType
import com.gabriele.notionlocal.data.entity.paragraph
import com.gabriele.notionlocal.data.entity.paragraphAttrs
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.i18n.ParagraphStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite

/**
 * **"Paragraph" nella pagina** (dal 21/10/2026, chiesto dall'utente, come
 * lo spacing del paragrafo di Word): le misure che valgono per i blocchi
 * della pagina aperta. Le dà la schermata, le leggono i campi di testo e le
 * isole. Vedi `ParagraphLook`.
 */
val LocalParagraphLook = compositionLocalOf { ParagraphLook() }

/**
 * Una misura del "Paragraph" fuori dal testo (sopra e sotto un'isola, dopo
 * il marcatore di un elenco dentro un toggle). I punti si contano come il
 * testo, che cresce con la scelta di Android sulla grandezza dei caratteri.
 */
fun TextUnit.toDpIn(density: Density): Dp = with(density) { this@toDpIn.toDp() }

/**
 * **Lo spazio dopo il marcatore, nel campo di testo condiviso.**
 *
 * Il marcatore è scritto nel testo mostrato (vedi `runPrefixes`): il numero
 * o il pallino seguiti da due spazi, o la casella fatta di quattro spazi su
 * cui si disegna il quadratino. Per cambiare lo spazio quegli spazi si
 * allargano o si stringono **solo in larghezza** (`scaleX`), con uno stile
 * sui caratteri che ci sono già: nessun carattere in più o in meno, quindi
 * la traduzione delle posizioni fra testo vero e testo mostrato resta
 * quella di sempre, e l'altezza della riga non cambia. È la differenza con
 * le due strade che nel README stanno fra quelle "già tentate" (un
 * `ParagraphStyle`, un carattere ingrandito), che toccavano l'altezza.
 *
 * Le misure arrivano dalla schermata, che sa quanto sono larghi davvero gli
 * spazi col carattere del campo: [spacePairPx] i due spazi dopo numeri e
 * pallini, [checkboxPx] i quattro della casella, [squareEndPx] dove finisce
 * il quadratino disegnato sopra, [pxPerPt] quanto è un punto.
 */
class MarkerGapScale(
    val look: ParagraphLook,
    private val spacePairPx: Float,
    private val checkboxPx: Float,
    private val squareEndPx: Float,
    private val pxPerPt: Float
) {
    /**
     * Lo stile e dove metterlo, per la riga che comincia a [lineStart] nel
     * testo mostrato con questo [prefix]; null se non c'è niente da cambiare.
     */
    fun styleFor(prefix: LinePrefix, type: BlockType, gapPt: Int, lineStart: Int): Triple<SpanStyle, Int, Int>? {
        val end = lineStart + prefix.text.length
        return if (type == BlockType.CHECKBOX && !prefix.hidden) {
            if (checkboxPx <= 0f) return null
            // Il testo comincia dove finisce il quadratino, più lo spazio scelto.
            val target = squareEndPx + gapPt * pxPerPt
            Triple(scaled(target / checkboxPx), end - prefix.marker.length, end)
        } else {
            if (prefix.marker.length < 2 || spacePairPx <= 0f) return null
            Triple(scaled(gapPt * pxPerPt / spacePairPx), end - 2, end)
        }
    }

    // A zero gli spazi spariscono del tutto: un fattore di zero esatto non
    // si chiede al carattere, basta uno quasi zero.
    private fun scaled(factor: Float) = SpanStyle(textGeometricTransform = TextGeometricTransform(scaleX = factor.coerceAtLeast(0.01f)))
}

// --- Le finestre ---

/** Una misura dello spazio dopo il marcatore: "Default" o "8 pt". */
fun gapLabel(gap: Int?): String = gap?.let { ParagraphStrings.points(it) } ?: ParagraphStrings.default

/** Gli spazi dopo i marcatori in poche parole: quello di tutti, o "per tipo". */
fun gapsSummary(gaps: ListGaps): String = if (gaps.same) gapLabel(gaps.all) else ParagraphStrings.perType

/** Cosa dice la voce "Paragraph" in Settings: "Blocchi: 0 pt · Elenchi: Default". */
fun paragraphSettingsSummary(): String =
    "${ParagraphStrings.blocksShort}: ${ParagraphStrings.points(AppSettings.blockSpacing)} · " +
        "${ParagraphStrings.listsShort}: ${gapsSummary(AppSettings.listGaps)}"

/** Cosa dice la voce "Paragraph" nei tre puntini: le misure che valgono nella pagina. */
fun pageParagraphSummary(look: ParagraphLook): String =
    "${ParagraphStrings.points(look.pageSpacing)} · ${gapsSummary(look.pageGaps)}"

/** Il nome di un elenco, nel suo stepper quando ognuno ha il suo spazio. */
private fun listLabel(type: BlockType): String = when (type) {
    BlockType.NUMBERED_LIST_ITEM -> ParagraphStrings.numbered
    BlockType.BULLET_LIST_ITEM -> ParagraphStrings.bulleted
    else -> ParagraphStrings.todo
}

/** Dove sta [current] fra i passi (o il primo più grande, se non c'è): `order` mette in fila i valori. */
private fun <T> List<T>.indexNear(current: T, order: (T) -> Int): Int =
    indexOf(current).takeIf { it >= 0 }
        ?: indexOfFirst { order(it) >= order(current) }.takeIf { it >= 0 }
        ?: lastIndex

/**
 * Una misura con "−" e "+": i passi sono pochi e fissi (vedi
 * `BLOCK_SPACING_STEPS`), così basta un tocco per volta e si vede subito la
 * pagina cambiare dietro.
 */
@Composable
private fun <T> StepperRow(
    label: String,
    value: T,
    steps: List<T>,
    order: (T) -> Int,
    text: (T) -> String,
    start: Dp,
    onChange: (T) -> Unit
) {
    val at = steps.indexNear(value, order)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = start, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = NotionWhite, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange(steps[at - 1]) }, enabled = at > 0) {
            Icon(Icons.Filled.Remove, contentDescription = ParagraphStrings.less)
        }
        Text(
            text(value),
            color = NotionWhite,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(72.dp)
        )
        IconButton(onClick = { onChange(steps[at + 1]) }, enabled = at < steps.lastIndex) {
            Icon(Icons.Filled.Add, contentDescription = ParagraphStrings.more)
        }
    }
}

@Composable
private fun SpacingStepper(label: String, value: Int, start: Dp, onChange: (Int) -> Unit) =
    StepperRow(label, value, BLOCK_SPACING_STEPS, { it }, { ParagraphStrings.points(it) }, start, onChange)

/** [withDefault]: se fra i passi c'è "Default", lo spazio di sempre (non per un gruppo di righe). */
@Composable
private fun GapStepper(label: String, value: Int?, withDefault: Boolean, start: Dp, onChange: (Int?) -> Unit) {
    val steps = if (withDefault) MARKER_GAP_STEPS else MARKER_GAP_STEPS.filterNotNull()
    StepperRow(label, value, steps, { it ?: -1 }, ::gapLabel, start, onChange)
}

/**
 * Lo spazio dopo i marcatori: l'interruttore "uguale per tutti" e uno
 * stepper, o uno per ogni elenco di [types].
 */
@Composable
private fun GapsEditor(
    gaps: ListGaps,
    types: List<BlockType>,
    withDefault: Boolean,
    start: Dp,
    onChange: (ListGaps) -> Unit
) {
    if (types.size > 1) {
        SwitchItem(ParagraphStrings.sameForLists, checked = gaps.same) { onChange(gaps.withSame(it)) }
    }
    when {
        types.size == 1 -> {
            val type = types.single()
            GapStepper(listLabel(type), gaps.of(type), withDefault, start) { onChange(gaps.with(if (gaps.same) null else type, it)) }
        }
        gaps.same -> GapStepper(ParagraphStrings.allLists, gaps.all, withDefault, start) { onChange(gaps.with(null, it)) }
        else -> types.forEach { type ->
            GapStepper(listLabel(type), gaps.of(type), withDefault, start) { onChange(gaps.with(type, it)) }
        }
    }
}

@Composable
private fun SubTitle(text: String, start: Dp) {
    Text(
        text,
        color = NotionWhite,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(start = start, end = start, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun Note(text: String, start: Dp) {
    Text(
        text,
        color = NotionGray400,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(start = start, end = start, bottom = 4.dp)
    )
}

/**
 * Settings → "Paragraph": lo spazio fra i blocchi e quello dopo i marcatori
 * degli elenchi, per tutte le pagine che non scelgono altro.
 */
@Composable
fun ParagraphSettingsDialog(onDismiss: () -> Unit) {
    FitAlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSheet,
        title = { Text(ParagraphStrings.paragraph, color = NotionWhite) },
        text = {
            // Tutto allineato al testo delle voci (`SettingsItem`, 20dp dal bordo).
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Note(ParagraphStrings.settingsNote, 20.dp)
                SubTitle(ParagraphStrings.spaceBetweenBlocks, 20.dp)
                Note(ParagraphStrings.spaceBetweenBlocksNote, 20.dp)
                SpacingStepper(ParagraphStrings.blocksShort, AppSettings.blockSpacing, 20.dp) { AppSettings.blockSpacing = it }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SubTitle(ParagraphStrings.markerGap, 20.dp)
                Note(ParagraphStrings.markerGapNote, 20.dp)
                GapsEditor(AppSettings.listGaps, MARKER_LIST_TYPES, withDefault = true, start = 20.dp) { AppSettings.listGaps = it }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(Strings.done) } }
    )
}

/**
 * "Paragraph" dai tre puntini: per ognuna delle due misure, come Settings o
 * solo per questa pagina.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageParagraphSheet(
    page: PageEntity,
    onSet: (PageParagraph) -> Unit,
    onDismiss: () -> Unit
) {
    val paragraph = page.paragraph
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
            Text(ParagraphStrings.paragraph, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                ParagraphStrings.pageNote,
                color = NotionGray400,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )

            SubTitle(ParagraphStrings.spaceBetweenBlocks, 0.dp)
            OptionsGroup {
                RadioItem(
                    title = ParagraphStrings.asInSettings(ParagraphStrings.points(AppSettings.blockSpacing)),
                    selected = paragraph.blockSpacing == null,
                    onClick = { onSet(paragraph.copy(blockSpacing = null)) }
                )
                RadioItem(
                    title = ParagraphStrings.onlyThisPage,
                    selected = paragraph.blockSpacing != null,
                    onClick = { onSet(paragraph.copy(blockSpacing = paragraph.blockSpacing ?: AppSettings.blockSpacing)) }
                )
                paragraph.blockSpacing?.let { spacing ->
                    SpacingStepper(ParagraphStrings.blocksShort, spacing, 16.dp) { onSet(paragraph.copy(blockSpacing = it)) }
                }
            }

            SubTitle(ParagraphStrings.markerGap, 0.dp)
            OptionsGroup {
                RadioItem(
                    title = ParagraphStrings.asInSettings(gapsSummary(AppSettings.listGaps)),
                    selected = paragraph.gaps == null,
                    onClick = { onSet(paragraph.copy(gaps = null)) }
                )
                RadioItem(
                    title = ParagraphStrings.onlyThisPage,
                    selected = paragraph.gaps != null,
                    onClick = { onSet(paragraph.copy(gaps = paragraph.gaps ?: AppSettings.listGaps)) }
                )
                paragraph.gaps?.let { gaps ->
                    GapsEditor(gaps, MARKER_LIST_TYPES, withDefault = true, start = 16.dp) { onSet(paragraph.copy(gaps = it)) }
                }
            }
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}

/** L'elenco di cui una riga ha il marcatore (o tiene il posto), o null. */
private fun BlockEntity.markerListType(): BlockType? = type.takeIf { it in MARKER_LIST_TYPES } ?: heldListType

/**
 * Gli spazi dopo i marcatori che queste righe hanno già di loro, o null se
 * nessuna ne ha uno suo (seguono la pagina).
 */
fun linesGaps(lines: List<BlockEntity>, types: List<BlockType>): ListGaps? {
    val own = lines.mapNotNull { line -> line.markerListType()?.let { it to line.paragraphAttrs?.markerGap } }
    if (own.none { it.second != null }) return null
    val byType = types.associateWith { type -> own.firstOrNull { it.first == type && it.second != null }?.second }
    val values = byType.values.filterNotNull().distinct()
    return if (values.size <= 1) {
        ListGaps(same = true, all = values.firstOrNull())
    } else {
        ListGaps(
            same = false,
            numbered = byType[BlockType.NUMBERED_LIST_ITEM],
            bulleted = byType[BlockType.BULLET_LIST_ITEM],
            todo = byType[BlockType.CHECKBOX]
        )
    }
}

/**
 * **"Paragraph" di un gruppo di righe** (le righe del testo selezionato, o
 * quella del cursore, o il blocco del menu dei sei puntini): come la pagina
 * o solo per loro. Ogni tocco si vede subito nella pagina dietro.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinesParagraphSheet(
    lines: List<BlockEntity>,
    look: ParagraphLook,
    onApply: (spacing: Int?, gapFor: (BlockType) -> Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val types = remember(lines) { MARKER_LIST_TYPES.filter { type -> lines.any { it.markerListType() == type } } }
    var spacing by remember(lines) { mutableStateOf(lines.map { it.paragraphAttrs?.spacing }.distinct().singleOrNull()) }
    var gaps by remember(lines) { mutableStateOf(linesGaps(lines, types)) }
    fun apply(newSpacing: Int?, newGaps: ListGaps?) {
        spacing = newSpacing
        gaps = newGaps
        onApply(newSpacing) { type -> newGaps?.of(type) }
    }
    // Il punto di partenza di "Only these lines": le misure della pagina, e
    // per gli spazi senza misura (Default) quella di sempre, 8.
    val pageGaps = look.pageGaps.let { g ->
        ListGaps(
            same = g.same,
            all = g.all ?: NATURAL_GAP,
            numbered = g.numbered ?: NATURAL_GAP,
            bulleted = g.bulleted ?: NATURAL_GAP,
            todo = g.todo ?: NATURAL_GAP
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            Text(ParagraphStrings.paragraph, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                ParagraphStrings.lines(lines.size) + " · " + ParagraphStrings.linesNote,
                color = NotionGray400,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )

            SubTitle(ParagraphStrings.spaceBetweenBlocks, 0.dp)
            OptionsGroup {
                RadioItem(
                    title = ParagraphStrings.asInPage(ParagraphStrings.points(look.pageSpacing)),
                    selected = spacing == null,
                    onClick = { apply(null, gaps) }
                )
                RadioItem(
                    title = ParagraphStrings.onlyTheseLines,
                    selected = spacing != null,
                    onClick = { apply(spacing ?: look.pageSpacing, gaps) }
                )
                spacing?.let { value ->
                    SpacingStepper(ParagraphStrings.blocksShort, value, 16.dp) { apply(it, gaps) }
                }
            }

            if (types.isNotEmpty()) {
                SubTitle(ParagraphStrings.markerGap, 0.dp)
                OptionsGroup {
                    RadioItem(
                        // Con un solo elenco fra le righe, la misura sua.
                        title = ParagraphStrings.asInPage(
                            types.singleOrNull()?.let { gapLabel(look.pageGaps.of(it)) } ?: gapsSummary(look.pageGaps)
                        ),
                        selected = gaps == null,
                        onClick = { apply(spacing, null) }
                    )
                    RadioItem(
                        title = ParagraphStrings.onlyTheseLines,
                        selected = gaps != null,
                        onClick = { apply(spacing, gaps ?: pageGaps) }
                    )
                    gaps?.let { value ->
                        GapsEditor(value, types, withDefault = false, start = 16.dp) { apply(spacing, it) }
                    }
                }
            }
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}

/** Lo spazio dopo il marcatore che gli elenchi dentro i toggle hanno sempre avuto, in punti. */
private const val NATURAL_GAP = 8
