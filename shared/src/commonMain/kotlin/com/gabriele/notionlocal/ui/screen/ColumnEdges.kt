package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.data.entity.attrs
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.ui.navigation.horizontalResizeCursor
import com.gabriele.notionlocal.ui.theme.AccentBlue
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// **La larghezza delle colonne della pagina col mouse** (dal 13/11/2026,
// chiesto dall'utente: "stessa cosa per i blocchi che si possono spostare tra
// le colonne", come la lineetta delle colonne dei database): sul PC, nello
// spazio fra due colonne c'è una striscia sottile; col mouse sopra diventa una
// linea blu e il puntatore la doppia freccia, e trascinandola le due colonne
// accanto cambiano larghezza. Ogni colonna si ricorda la sua parte
// (`BlockAttrs.width` del blocco COLUMN). Le strisce stanno sopra la pagina,
// come i sei puntini (`BlockHandle.kt`), così i puntini delle righe vicine non
// le coprono: ogni gruppo di colonne dice qui dov'è, e la pagina le disegna.
// ---------------------------------------------------------------------------

/** Una colonna non scende sotto questa parte del gruppo. */
const val MIN_COLUMN_SHARE = 0.1f

/** Quanto è larga la striscia che si prende col mouse. */
private val COLUMN_EDGE_STRIP = 10.dp

/**
 * Un gruppo di colonne sullo schermo: dove sta ([coordinates]) e le parti
 * delle sue colonne ([weights]). Il trascinamento comincia ([onStart]), il
 * bordo [index] è spostato in tutto di `dx` pixel ([onDrag]), finisce ([onEnd]).
 */
class ColumnEdgeSpot(
    val coordinates: LayoutCoordinates,
    val weights: () -> List<Float>,
    val onStart: () -> Unit,
    val onDrag: (index: Int, dx: Float) -> Unit,
    val onEnd: () -> Unit
) {
    /** Dove sta nella finestra il bordo fra la colonna [index] e la seguente. */
    fun edgeX(index: Int): Float? {
        if (!coordinates.isAttached) return null
        val parts = weights()
        if (index !in 0 until parts.size - 1) return null
        val left = coordinates.localToWindow(Offset.Zero).x
        return left + coordinates.size.width * parts.take(index + 1).sum() / parts.sum()
    }
}

/** I gruppi di colonne della pagina, per chiave del blocco COLUMN_LIST. */
class ColumnEdges {
    val spots = mutableStateMapOf<String, ColumnEdgeSpot>()
}

val LocalColumnEdges = staticCompositionLocalOf<ColumnEdges?> { null }

/** Le parti delle colonne di un gruppo: quelle salvate, e per le colonne che non ce l'hanno la media delle altre. */
fun columnWeights(columns: List<BlockEntity>): List<Float> {
    val known = columns.mapNotNull { it.attrs.width }
    val fallback = if (known.isEmpty()) 1f else known.average().toFloat()
    return columns.map { (it.attrs.width ?: fallback).coerceAtLeast(0.01f) }
}

/** [weights] col bordo [index] spostato di [delta] (nella stessa unità): nessuna delle due colonne sotto [MIN_COLUMN_SHARE] del gruppo. */
fun movedColumnEdge(weights: List<Float>, index: Int, delta: Float): List<Float> {
    val min = weights.sum() * MIN_COLUMN_SHARE
    val moved = delta.coerceIn(minOf(0f, min - weights[index]), maxOf(0f, weights[index + 1] - min))
    return weights.toMutableList().also {
        it[index] += moved
        it[index + 1] -= moved
    }
}

/** Le strisce di tutti i gruppi di colonne, sopra la pagina [page]; la rotella sopra di loro va alla pagina ([onWheel]). */
@Composable
fun ColumnEdgeLayer(edges: ColumnEdges, page: () -> LayoutCoordinates?, onWheel: (Float) -> Unit) {
    edges.spots.forEach { (listId, spot) ->
        repeat(spot.weights().size - 1) { index ->
            key(listId, index) { ColumnEdgeStrip(spot, index, page, onWheel) }
        }
    }
}

@Composable
private fun ColumnEdgeStrip(spot: ColumnEdgeSpot, index: Int, page: () -> LayoutCoordinates?, onWheel: (Float) -> Unit) {
    val density = LocalDensity.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var dragging by remember { mutableStateOf(false) }
    val latestSpot by rememberUpdatedState(spot)
    val latestWheel by rememberUpdatedState(onWheel)
    val where = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val height = with(density) { spot.coordinates.size.height.toDp() }
    Box(
        modifier = Modifier
            .offset {
                val origin = page()?.takeIf { it.isAttached }?.localToWindow(Offset.Zero) ?: Offset.Zero
                val x = latestSpot.edgeX(index) ?: -10_000f
                val top = if (latestSpot.coordinates.isAttached) latestSpot.coordinates.localToWindow(Offset.Zero).y else 0f
                IntOffset((x - origin.x - COLUMN_EDGE_STRIP.toPx() / 2f).roundToInt(), (top - origin.y).roundToInt())
            }
            .size(COLUMN_EDGE_STRIP, height)
            .onGloballyPositioned { where[0] = it }
            .hoverable(interaction)
            .horizontalResizeCursor()
            // La rotella sopra la striscia fa scorrere la pagina, come altrove.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type != PointerEventType.Scroll) continue
                        latestWheel(event.changes.first().scrollDelta.y)
                        event.changes.forEach { it.consume() }
                    }
                }
            }
            // Lo spostamento si conta nella finestra: la striscia si muove col bordo, sotto il mouse.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val startX = where[0]?.takeIf { it.isAttached }?.localToWindow(down.position)?.x ?: return@awaitEachGesture
                    down.consume()
                    dragging = true
                    latestSpot.onStart()
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            val x = where[0]?.takeIf { it.isAttached }?.localToWindow(change.position)?.x ?: continue
                            change.consume()
                            latestSpot.onDrag(index, x - startX)
                        }
                    } finally {
                        dragging = false
                        latestSpot.onEnd()
                    }
                }
            }
            .drawBehind {
                if (hovered || dragging) {
                    drawLine(AccentBlue, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), strokeWidth = 2.dp.toPx())
                }
            }
    )
}
