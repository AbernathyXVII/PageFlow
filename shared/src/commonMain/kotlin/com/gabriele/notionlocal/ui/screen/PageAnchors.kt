package com.gabriele.notionlocal.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.blockLinkTarget
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.AccentBlue
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// **L'indice navigabile** (dal 20/10/2026, chiesto dall'utente: "come su
// Kindle"). Le righe col pin (dal menu dei sei puntini) e quelle scritte in
// grande sono punti di arrivo; un pezzo di testo collegato a una di loro
// porta lì con un tocco, la riga si illumina e in basso compare "Back" per
// tornare. I dati stanno in `BlockLinks.kt`.
// ---------------------------------------------------------------------------

/** Il salto a una riga della pagina aperta; null fuori dall'editor (lì il collegamento non si apre). */
val LocalBlockLinkJump = staticCompositionLocalOf<((String) -> Unit)?> { null }

/** Dove si era prima di un salto: la voce della lista in cima e di quanto era scesa. */
data class LinkReturn(val index: Int, val offset: Int, val upward: Boolean)

/** Quanto resta "Back" dopo un salto. */
private const val LINK_BACK_MS = 10_000L

const val LINK_BACK_TAG = "linkBack"

/** Dove comincia la riga [line] (da 0) nel testo mostrato di un gruppo di righe: dopo il suo a-capo. */
fun lineStartIn(shown: CharSequence, line: Int): Int {
    if (line <= 0) return 0
    var count = 0
    for (k in shown.indices) {
        if (shown[k] == '\n' && ++count == line) return k + 1
    }
    return shown.length
}

/** La cima della riga [line] nel campo di un gruppo di righe, in px; null se il campo non è ancora disegnato. */
fun lineTopIn(layout: TextLayoutResult?, line: Int): Float? {
    if (layout == null || layout.lineCount == 0) return null
    val text = layout.layoutInput.text.text
    val start = lineStartIn(text, line).coerceAtMost(text.length)
    return layout.getLineTop(layout.getLineForOffset(start))
}

/**
 * **Il segnalibro di una riga col pin**: un nastrino nel margine a sinistra,
 * all'altezza della riga. [top] e [bottom] sono quelli della riga, nelle
 * coordinate di chi disegna; [left] è dove comincia il testo.
 */
fun DrawScope.drawPinRibbon(top: Float, left: Float, color: Color) {
    val width = 5.dp.toPx()
    val height = 11.dp.toPx()
    val x = left - 14.dp.toPx()
    val y = top + 3.dp.toPx()
    val notch = 3.dp.toPx()
    val ribbon = Path().apply {
        moveTo(x, y)
        lineTo(x + width, y)
        lineTo(x + width, y + height)
        lineTo(x + width / 2, y + height - notch)
        lineTo(x, y + height)
        close()
    }
    drawPath(ribbon, color)
}

/**
 * **La freccia dei collegamenti dentro la pagina**, dopo l'ultima lettera:
 * così si distinguono da quelli a un sito. Sta nello spazio che le lascia
 * [markBlockLinkEnd] (vedi dove si costruisce il testo della riga), o, a
 * fine riga, nel vuoto che c'è già dopo.
 */
fun DrawScope.drawBlockLinkArrows(layout: TextLayoutResult?, color: Color) {
    val text = layout?.layoutInput?.text ?: return
    val marks = text.getStringAnnotations(LINK_TAG, 0, text.length)
    for (mark in marks) {
        if (blockLinkTarget(mark.item) == null) continue
        // Lo stesso collegamento continua nel pezzo dopo: la freccia va là.
        if (marks.any { it.start == mark.end && it.item == mark.item }) continue
        val last = (mark.end - 1).coerceAtMost(text.length - 1)
        if (last < mark.start) continue
        val box = layout.getBoundingBox(last)
        val fontPx = layout.layoutInput.style.fontSize.let { if (it.isSp) it.toPx() else box.height * 0.7f }
        val (startX, endX) = blockLinkArrowSpan(box.right, fontPx)
        val y = box.top + box.height * 0.42f
        val stroke = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
        val head = (endX - startX) * 0.45f
        val path = Path().apply {
            moveTo(startX, y)
            lineTo(endX, y)
            moveTo(endX - head, y - head)
            lineTo(endX, y)
            lineTo(endX - head, y + head)
        }
        drawPath(path, color, style = stroke)
    }
}

/**
 * Dove sta la freccia, da sinistra a destra: subito dopo l'ultima lettera
 * del collegamento, che finisce a [letterRight], dentro la metà dello spazio
 * in più che il carattere dopo ha davanti a sé.
 */
fun blockLinkArrowSpan(letterRight: Float, fontPx: Float): Pair<Float, Float> {
    val room = fontPx * BLOCK_LINK_GAP_EM / 2
    val start = letterRight + room * 0.2f
    return start to start + room * 0.7f
}

/** Lo spazio in più dopo l'ultima lettera di un collegamento dentro la pagina, per la freccia. */
const val BLOCK_LINK_GAP_EM = 0.9f

/**
 * Lo spazio per la freccia dopo un collegamento dentro la pagina, che va da
 * [start] a [end]. [next] è il pezzo di testo dopo: se continua lo stesso
 * collegamento (un pezzo in grassetto in mezzo), la freccia va alla fine di
 * quello.
 *
 * **Lo spazio lo prende il carattere dopo, non l'ultima lettera** (26/10/2026,
 * l'utente: "Emergenc y", con la freccia sopra la y). Android divide lo
 * spazio in più fra prima e dopo la lettera: allargando l'ultima, metà le
 * finiva davanti e la staccava dalle altre; e a fine riga (sul telefono
 * dell'utente, Android 17) la metà dietro non c'era, così la freccia le
 * cadeva sopra. Allargato il carattere dopo (di solito lo spazio), la sua
 * metà davanti è il posto della freccia. Se il collegamento chiude la riga,
 * dopo c'è già il vuoto e non serve niente.
 */
fun AnnotatedString.Builder.markBlockLinkEnd(span: RichTextSpan, next: RichTextSpan?, start: Int, end: Int) {
    if (end <= start || blockLinkTarget(span.link) == null || next?.link == span.link) return
    if (next == null || next.text.isEmpty() || next.text[0] == '\n') return
    addStyle(SpanStyle(letterSpacing = BLOCK_LINK_GAP_EM.em), end, end + 1)
}

/**
 * Una riga a sé (riquadro, toggle, citazione…) che fa da punto di arrivo: il
 * nastrino nel margine se ha il pin, e il velo che si illumina e svanisce
 * quando ci si arriva con un collegamento (o la si sposta con le frecce).
 */
@Composable
fun Modifier.anchorDecor(
    blockId: String,
    pinned: Boolean,
    moved: PageEditorViewModel.MovedBlock?,
    left: androidx.compose.ui.unit.Dp
): Modifier {
    val flash = remember(blockId) { Animatable(0f) }
    LaunchedEffect(moved) {
        if (moved?.blockId != blockId) return@LaunchedEffect
        flash.snapTo(1f)
        // Arrivati con un collegamento, la luce resta piena un secondo (dal 18/11/2026).
        delay(moved.holdMs)
        flash.animateTo(0f, tween(ANCHOR_FLASH_MS))
    }
    val flashColor = MaterialTheme.colorScheme.primary
    return drawWithContent {
        drawContent()
        if (pinned) drawPinRibbon(top = 4.dp.toPx(), left = left.toPx(), color = AccentBlue)
        val strength = flash.value
        if (strength > 0f) {
            drawRoundRect(
                color = flashColor.copy(alpha = 0.3f * strength),
                topLeft = Offset(left.toPx() - 6.dp.toPx(), 0f),
                size = Size(size.width - 2 * left.toPx() + 12.dp.toPx(), size.height),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        }
    }
}

private const val ANCHOR_FLASH_MS = 700

/** La riga arrivata con un collegamento o spostata (vedi `PageEditorViewModel.movedBlock`), per [nestedAnchor]. */
val LocalMovedBlock = staticCompositionLocalOf<PageEditorViewModel.MovedBlock?> { null }

/** Dove stanno adesso le righe dentro un altro blocco ([nestedAnchor]): l'indice e i collegamenti ci arrivano. */
class NestedSpots {
    val coordinates = HashMap<String, LayoutCoordinates>()
}

val LocalNestedSpots = staticCompositionLocalOf<NestedSpots?> { null }

/**
 * **Una riga dentro un altro blocco** (colonne, toggle, citazioni con righe,
 * blocchi sincronizzati) **si illumina lei sola** quando ci si arriva (dal
 * 18/11/2026, trovato dall'utente: con l'indice su una Quote dentro le colonne
 * si illuminava tutta la pagina, cioè il blocco di colonne), e dice dove sta
 * perché la pagina ci scorra (vedi `jumpToBlock`). Il nastrino del pin no: le
 * righe dentro un blocco non l'avevano.
 */
@Composable
fun Modifier.nestedAnchor(blockId: String, left: androidx.compose.ui.unit.Dp): Modifier {
    val spots = LocalNestedSpots.current
    DisposableEffect(blockId, spots) { onDispose { spots?.coordinates?.remove(blockId) } }
    return (if (spots == null) this else onGloballyPositioned { spots.coordinates[blockId] = it })
        .anchorDecor(blockId, pinned = false, moved = LocalMovedBlock.current, left = left)
}

/**
 * **"Back"**, in basso dopo un salto (come su Kindle): riporta dov'era la
 * pagina. La freccia dice da che parte. Se ne va da sola dopo un po'.
 */
@Composable
fun LinkBackButton(back: LinkReturn, onBack: () -> Unit, onTimeout: () -> Unit, modifier: Modifier = Modifier) {
    val timeout by rememberUpdatedState(onTimeout)
    LaunchedEffect(back) {
        delay(LINK_BACK_MS)
        timeout()
    }
    BackPill(upward = back.upward, tag = LINK_BACK_TAG, onClick = onBack, modifier = modifier)
}

/**
 * La pillola "Back" con la sua freccia, in su o in giù: quella dell'indice e,
 * dal 27/10/2026, quella che riporta in cima a una pagina lunga (`BackToTop.kt`).
 */
@Composable
fun BackPill(upward: Boolean, tag: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .testTag(tag)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (upward) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(Strings.back, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * **La riga trovata dalla ricerca** (dal 19/11/2026, chiesto dall'utente: "la pagina deve
 * scorrere fino al punto in cui quella voce che ho cercato si trova"): aprendo un risultato,
 * la ricerca lascia qui la pagina e il blocco; la pagina, appena si apre, lo prende, ci
 * scorre e lo illumina come un collegamento (senza "Back"). Sul PC e sul telefono.
 */
object SearchJump {
    private var pending: Pair<String, String>? = null

    fun request(pageId: String, blockId: String) {
        pending = pageId to blockId
    }

    /** Il blocco da raggiungere nella pagina [pageId], una volta sola; null se non ce n'è. */
    fun take(pageId: String): String? = pending?.takeIf { it.first == pageId }?.second?.also { pending = null }
}
