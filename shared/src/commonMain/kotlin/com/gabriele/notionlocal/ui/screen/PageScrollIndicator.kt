package com.gabriele.notionlocal.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import com.gabriele.notionlocal.platform.isPc
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/** Quanto resta la barretta dopo che la pagina si è fermata. */
private const val SCROLL_INDICATOR_LINGER_MS = 900L

/** C'è solo mentre la barretta si vede: per i test. */
const val PAGE_SCROLL_INDICATOR_TAG = "pageScrollIndicator"

/**
 * **La barretta di scorrimento della pagina** (dal 20/10/2026, chiesta
 * dall'utente: "piccola, classica, come le impostazioni del telefono").
 * Sottile e chiara sul bordo destro: compare mentre la pagina scorre e svanisce
 * poco dopo che si è fermata. Non prende i tocchi, che passano alla pagina.
 *
 * La lista non sa quanto è alta la pagina: le sue voci (i pezzi di testo, le
 * immagini, i database) hanno altezze diverse e si misurano solo quando si
 * vedono. Allora si ricorda l'altezza di ogni voce già vista, e quelle mai
 * viste valgono la media: la barretta è esatta dove si è già passati e si
 * assesta scorrendo. In cima e in fondo alla pagina tocca il bordo.
 *
 * **Sul PC si trascina col mouse** (dal 13/11/2026, chiesto dall'utente:
 * "permettimi di draggarla per spostarmi nella pagina"): compare anche col
 * mouse sopra, si prende e la pagina la segue; un clic sulla corsia porta lì
 * la barretta. La rotella sopra di lei fa scorrere la pagina come sempre. Sul
 * telefono resta solo da guardare.
 */
@Composable
fun PageScrollIndicator(list: LazyListState, modifier: Modifier = Modifier) {
    val sizes = remember(list) { ItemSizes() }
    var shown by remember(list) { mutableStateOf(false) }
    var hovered by remember(list) { mutableStateOf(false) }
    var dragging by remember(list) { mutableStateOf(false) }
    LaunchedEffect(list) {
        snapshotFlow { list.isScrollInProgress }.collectLatest { scrolling ->
            if (scrolling) {
                shown = true
            } else {
                delay(SCROLL_INDICATOR_LINGER_MS)
                shown = false
            }
        }
    }
    val alpha by animateFloatAsState(
        targetValue = if (shown || hovered || dragging) 1f else 0f,
        animationSpec = tween(if (shown) 150 else 300),
        label = "page-scroll-indicator"
    )
    val color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (dragging) 0.6f else 0.4f)

    /** Dove sta la barretta in una corsia alta [height] (px), con quanto c'è da scorrere: null se la pagina sta nello schermo. */
    fun Density.thumb(height: Float): Thumb? {
        if (!(list.canScrollForward || list.canScrollBackward)) return null
        val info = list.layoutInfo
        sizes.record(info)
        val margin = 4.dp.toPx()
        val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
        val (scrolled, total) = sizes.extent(info.totalItemsCount, list.firstVisibleItemIndex, list.firstVisibleItemScrollOffset)
        val track = height - 2 * margin
        val (top, length) = scrollThumb(
            viewport = viewport,
            scrolled = scrolled,
            total = total,
            track = track,
            minThumb = 32.dp.toPx(),
            atStart = !list.canScrollBackward,
            atEnd = !list.canScrollForward
        ) ?: return null
        return Thumb(margin + top, length, track, margin, scrolled, total - viewport)
    }

    val grab = if (!isPc) Modifier else Modifier.pointerInput(list) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: continue
                when (event.type) {
                    PointerEventType.Enter -> hovered = true
                    PointerEventType.Exit -> hovered = false
                    // La rotella sopra la barretta: la pagina scorre come col mouse sopra di lei (vedi `BlockDragLayer`).
                    PointerEventType.Scroll -> {
                        list.dispatchRawDelta(change.scrollDelta.y * size.height / 20f)
                        change.consume()
                    }
                    PointerEventType.Press -> {
                        val thumb = thumb(size.height.toFloat()) ?: continue
                        dragging = true
                        val y = change.position.y
                        // Fuori dalla barretta: lei va lì, a metà sotto il mouse.
                        if (y < thumb.top || y > thumb.top + thumb.length) {
                            val fraction = ((y - thumb.margin - thumb.length / 2f) / (thumb.track - thumb.length)).coerceIn(0f, 1f)
                            list.dispatchRawDelta(fraction * thumb.range - thumb.scrolled)
                        }
                        change.consume()
                    }
                    PointerEventType.Move -> if (dragging) {
                        val thumb = thumb(size.height.toFloat())
                        val dy = change.position.y - change.previousPosition.y
                        if (thumb != null && thumb.track > thumb.length) list.dispatchRawDelta(dy * thumb.range / (thumb.track - thumb.length))
                        change.consume()
                    }
                    PointerEventType.Release -> dragging = false
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .width(if (isPc) 14.dp else 10.dp)
            .then(grab)
            .then(if (alpha > 0f) Modifier.testTag(PAGE_SCROLL_INDICATOR_TAG) else Modifier)
    ) {
        if (alpha <= 0f) return@Canvas
        val thumb = thumb(size.height) ?: return@Canvas
        val width = 4.dp.toPx()
        drawRoundRect(
            color = color.copy(alpha = color.alpha * alpha),
            topLeft = Offset(size.width - width - 3.dp.toPx(), thumb.top),
            size = Size(width, thumb.length),
            cornerRadius = CornerRadius(width / 2)
        )
    }
}

/**
 * La barretta: dalla cima [top] lunga [length] nella sua corsia [track], che
 * comincia [margin] più in basso, con quanto si è già sceso ([scrolled]) e
 * quanto si può scendere in tutto ([range]), in px.
 */
class Thumb(val top: Float, val length: Float, val track: Float, val margin: Float, val scrolled: Float, val range: Float)

/** Le altezze delle voci già viste, per posizione; si ricomincia quando cambia il numero di voci. */
class ItemSizes {
    private val sizes = HashMap<Int, Int>()
    private var count = -1

    fun record(info: LazyListLayoutInfo) {
        if (info.totalItemsCount != count) {
            sizes.clear()
            count = info.totalItemsCount
        }
        for (item in info.visibleItemsInfo) sizes[item.index] = item.size
    }

    fun extent(count: Int, firstIndex: Int, firstOffset: Int): Pair<Float, Float> =
        estimateExtent(sizes, count, firstIndex, firstOffset)
}

/**
 * Quanto si è scesi e quanto è alta la pagina, in px: le voci già viste con la
 * loro altezza, le altre con la media.
 */
fun estimateExtent(sizes: Map<Int, Int>, count: Int, firstIndex: Int, firstOffset: Int): Pair<Float, Float> {
    if (count <= 0 || sizes.isEmpty()) return 0f to 0f
    val average = sizes.values.average().toFloat()
    var before = 0f
    var total = 0f
    for (i in 0 until count) {
        val size = sizes[i]?.toFloat() ?: average
        if (i < firstIndex) before += size
        total += size
    }
    return before + firstOffset to total
}

/**
 * Dove sta la barretta nella sua corsia lunga [track]: (cima, altezza). Alta
 * quanto la parte di pagina che si vede, ma mai meno di [minThumb]. Null se la
 * pagina sta tutta nello schermo.
 */
fun scrollThumb(
    viewport: Float,
    scrolled: Float,
    total: Float,
    track: Float,
    minThumb: Float,
    atStart: Boolean,
    atEnd: Boolean
): Pair<Float, Float>? {
    val range = total - viewport
    if (range <= 0f || track <= 0f) return null
    val height = (track * viewport / total).coerceIn(minThumb.coerceAtMost(track), track)
    val fraction = when {
        atEnd -> 1f
        atStart -> 0f
        else -> (scrolled / range).coerceIn(0f, 1f)
    }
    return (track - height) * fraction to height
}
