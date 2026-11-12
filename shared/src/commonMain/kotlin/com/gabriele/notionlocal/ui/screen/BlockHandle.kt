package com.gabriele.notionlocal.ui.screen

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// **I sei puntini accanto al blocco sotto il mouse** (dal 11/11/2026, chiesto
// dall'utente, solo sul PC, come su Notion): passando col mouse su un blocco,
// a sinistra della sua prima riga compaiono i sei puntini; un clic apre il
// menu di quel blocco (`BlockActionsHost`), con le voci del suo tipo: una
// riga di testo, un database, un divisore, un'immagine…
// Tenendoli premuti e trascinando si sposta il blocco (`BlockDrag.kt`).
// ---------------------------------------------------------------------------

/**
 * Il blocco sotto il mouse e le lettere della sua prima riga, dall'alto al
 * basso nella pagina; [start] dove comincia da sinistra (null: dove comincia
 * il testo).
 */
data class BlockSpot(val blockId: String, val top: Float, val bottom: Float, val start: Float? = null)

/**
 * Guarda dove va il mouse sulla pagina e dice ([onSpot]) quale blocco ha
 * sotto. Fra due blocchi resta quello di prima, così si arriva ai puntini
 * senza perderli; uscendo dalla pagina non resta niente. Mentre si tiene
 * premuto (si seleziona) i puntini spariscono.
 */
fun Modifier.blockHover(
    picker: BlockPicker,
    coordinates: () -> LayoutCoordinates?,
    current: () -> BlockSpot? = { null },
    onSpot: (BlockSpot?) -> Unit
): Modifier = pointerInput(picker) {
    val islandLine = HANDLE_HEIGHT.toPx()
    val approach = (HANDLE_GAP + HANDLE_WIDTH + 8.dp).toPx()
    val slack = 8.dp.toPx()
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull() ?: continue
            if (change.type != PointerType.Mouse) continue
            val p = change.position
            when {
                change.pressed -> onSpot(null)
                // Uscendo verso i puntini, che stanno sopra la pagina, il punto è ancora dentro.
                event.type == PointerEventType.Exit -> if (p.x < 0f || p.y < 0f || p.x > size.width || p.y > size.height) onSpot(null)
                else -> {
                    // **Andando verso i puntini di una riga in una colonna** (dal 12/11/2026)
                    // si passa sopra il bordo della colonna accanto: restano quelli di prima.
                    val spot = current()
                    val start = spot?.start
                    if (start != null && p.x in start - approach..start && p.y in spot.top - slack..spot.bottom + slack) continue
                    val page = coordinates()?.takeIf { it.isAttached } ?: continue
                    val (id, line) = picker.lineAt(page.localToWindow(p), islandLine) ?: continue
                    val origin = page.localToWindow(Offset.Zero)
                    onSpot(BlockSpot(id, line.top - origin.y, line.bottom - origin.y, line.start?.minus(origin.x)))
                }
            }
        }
    }
}

/**
 * I sei puntini a metà delle lettere della prima riga di [spotState], che
 * finiscono un poco prima di dove comincia il blocco (di suo, [endsAt]: dove
 * comincia il testo); il clic li passa a [onClick], il trascinamento a
 * [drag] (null: non si trascinano). Lo stato si legge qui
 * dentro: muovendo il mouse si ridisegnano solo i puntini, non la pagina.
 *
 * **Disegnati qui, sui pixel interi** (dal 11/11/2026, trovato dall'utente:
 * "l'icona dei sei puntini è storta"): l'icona di Material, rimpicciolita,
 * cadeva fra un pixel e l'altro e i puntini venivano di grandezze diverse.
 */
@Composable
fun BlockHandle(spotState: State<BlockSpot?>, endsAt: Dp, drag: BlockDrag? = null, onClick: (String) -> Unit) {
    val spot = spotState.value ?: return
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val place = remember { arrayOfNulls<LayoutCoordinates>(1) }
    // Se i puntini se ne vanno mentre si trascina, il blocco resta dov'era.
    DisposableEffect(drag) { onDispose { drag?.end(place = false) } }
    Box(
        modifier = Modifier
            .offset {
                val end = spot.start ?: endsAt.toPx()
                IntOffset(
                    (end - (HANDLE_GAP + HANDLE_WIDTH).toPx()).roundToInt(),
                    ((spot.top + spot.bottom) / 2f - HANDLE_HEIGHT.toPx() / 2f).roundToInt()
                )
            }
            .size(HANDLE_WIDTH, HANDLE_HEIGHT)
            .clip(RoundedCornerShape(4.dp))
            .onGloballyPositioned { place[0] = it }
            .then(
                if (drag == null) Modifier else Modifier.pointerInput(drag) {
                    // Finché si tiene premuto la rotella arriva qui, non alla pagina: gliela si passa.
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Scroll) drag.wheel(event.changes.sumOf { it.scrollDelta.y.toDouble() }.toFloat())
                        }
                    }
                }.pointerInput(drag) {
                    // Il blocco di quando si comincia: lo stato, perché il gesto dura più di un giro.
                    detectDragGestures(
                        onDragStart = { at ->
                            val id = spotState.value?.blockId
                            val c = place[0]?.takeIf { it.isAttached }
                            if (id != null && c != null) drag.start(id, c.localToWindow(at))
                        },
                        onDragEnd = { drag.end(place = true) },
                        onDragCancel = { drag.end(place = false) },
                        onDrag = { change, _ ->
                            change.consume()
                            place[0]?.takeIf { it.isAttached }?.let { drag.moveTo(it.localToWindow(change.position)) }
                        }
                    )
                }
            )
            .semantics { contentDescription = EditorStrings.blockOptions }
            .clickable { onClick(spot.blockId) }
            .drawBehind { drawSixDots(color) }
    )
}

/** Due colonne di tre puntini tondi, nel mezzo, ognuno sugli stessi pixel degli altri. */
private fun DrawScope.drawSixDots(color: Color) {
    val dot = DOT_SIZE.toPx().roundToInt().coerceAtLeast(2)
    val step = DOT_STEP.toPx().roundToInt().coerceAtLeast(dot + 1)
    val left = ((size.width - (dot + step)) / 2f).roundToInt()
    val top = ((size.height - (dot + 2 * step)) / 2f).roundToInt()
    for (column in 0..1) for (row in 0..2) {
        drawCircle(color, radius = dot / 2f, center = Offset(left + column * step + dot / 2f, top + row * step + dot / 2f))
    }
}

/**
 * **La prima riga di un blocco che non è testo** (dal 11/11/2026): il
 * titolo di un database dentro la pagina si segna qui, e i sei puntini si
 * mettono a metà di lui, subito a sinistra dell'icona o del nome.
 */
class FirstLine {
    private var coordinates: LayoutCoordinates? = null
    private var startPx = 0f

    fun report(coordinates: LayoutCoordinates, startPx: Float) {
        this.coordinates = coordinates
        this.startPx = startPx
    }

    /** Dove sta, rispetto al blocco [block]; null se non si vede (un titolo nascosto). */
    fun boundsIn(block: LayoutCoordinates): LineBounds? {
        val line = coordinates?.takeIf { it.isAttached && block.isAttached } ?: return null
        val box = block.localBoundingBoxOf(line, clipBounds = false)
        return LineBounds(box.top, box.bottom, box.left + startPx)
    }
}

val LocalFirstLine = staticCompositionLocalOf<FirstLine?> { null }

/** Questa è la prima riga del blocco che la contiene ([FirstLine]); il contenuto comincia [start] più a destra. */
@Composable
fun Modifier.reportsFirstLine(start: Dp = 0.dp): Modifier {
    val line = LocalFirstLine.current ?: return this
    val startPx = with(LocalDensity.current) { start.toPx() }
    return onGloballyPositioned { line.report(it, startPx) }
}

private val HANDLE_HEIGHT = 24.dp
private val HANDLE_WIDTH = 20.dp
// Da 2 a 5 il 11/11/2026, chiesto dall'utente: i puntini erano troppo attaccati al testo.
private val HANDLE_GAP = 5.dp
private val DOT_SIZE = 3.dp
private val DOT_STEP = 5.dp
