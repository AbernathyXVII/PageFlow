package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
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
// ---------------------------------------------------------------------------

/** Il blocco sotto il mouse e la sua prima riga, dall'alto al basso nella pagina. */
data class BlockSpot(val blockId: String, val top: Float, val bottom: Float)

/**
 * Guarda dove va il mouse sulla pagina e dice ([onSpot]) quale blocco ha
 * sotto. Fra due blocchi resta quello di prima, così si arriva ai puntini
 * senza perderli; uscendo dalla pagina non resta niente. Mentre si tiene
 * premuto (si seleziona) i puntini spariscono.
 */
fun Modifier.blockHover(
    picker: BlockPicker,
    coordinates: () -> LayoutCoordinates?,
    onSpot: (BlockSpot?) -> Unit
): Modifier = pointerInput(picker) {
    val islandLine = HANDLE_HEIGHT.toPx()
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
                    val page = coordinates()?.takeIf { it.isAttached } ?: continue
                    val (id, line) = picker.lineAt(page.localToWindow(p), islandLine) ?: continue
                    val origin = page.localToWindow(Offset.Zero).y
                    onSpot(BlockSpot(id, line.start - origin, line.endInclusive - origin))
                }
            }
        }
    }
}

/**
 * I sei puntini, larghi [width], a metà della prima riga di [spotState]; il
 * clic li passa a [onClick]. Lo stato si legge qui dentro: muovendo il mouse
 * si ridisegnano solo i puntini, non la pagina.
 */
@Composable
fun BlockHandle(spotState: State<BlockSpot?>, width: Dp, onClick: (String) -> Unit) {
    val spot = spotState.value ?: return
    Box(
        modifier = Modifier
            .offset { IntOffset(0, ((spot.top + spot.bottom) / 2f - HANDLE_HEIGHT.toPx() / 2f).roundToInt()) }
            .size(width, HANDLE_HEIGHT)
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick(spot.blockId) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.DragIndicator,
            contentDescription = EditorStrings.blockOptions,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

private val HANDLE_HEIGHT = 24.dp
