package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.ui.theme.AccentBlue
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// **Spostare i blocchi trascinando i sei puntini** (dal 12/11/2026, chiesto
// dall'utente, solo sul PC, come su Notion): tenendo premuti i sei puntini
// accanto a un blocco (`BlockHandle.kt`) e trascinando, una linea blu dice
// dove andrà, sopra o sotto il blocco sotto il mouse secondo la metà; vicino
// al bordo in alto o in basso la pagina scorre da sola, e scorre anche con
// la rotella (dal 12/11/2026, trovato dall'utente: "non posso scrollare con
// il mouse"; il mouse è dei puntini finché si tiene premuto, e la rotella
// arrivava a loro invece che alla pagina). Lasciando, il blocco
// va lì con "Move to line…" (`PageEditorViewModel.moveBlockAfter`): si può
// annullare, e arrivato si illumina. Righe di testo, pagine, database,
// divisori: tutti i blocchi di primo livello della pagina.
// ---------------------------------------------------------------------------

/** Dove cadrà il blocco trascinato: subito sotto [afterId] (null: in cima alla pagina), con la linea blu all'altezza [lineY] della finestra. */
data class BlockDrop(val afterId: String?, val lineY: Float)

/**
 * Dove cade [dragged] col mouse sul blocco [target], che nella finestra sta
 * fra [box] `top` e `bottom`: sotto di lui con [below], se no sopra. [order]
 * sono i blocchi della pagina dall'alto. Null se il blocco resterebbe dov'è
 * (sopra se stesso, o subito sopra o sotto).
 */
fun dropFor(dragged: String, target: String, below: Boolean, box: LineBounds, order: List<String>): BlockDrop? {
    if (target == dragged) return null
    val rest = order.filter { it != dragged }
    val at = rest.indexOf(target)
    if (at < 0) return null
    val afterId = if (below) target else rest.getOrNull(at - 1)
    if (afterId == order.getOrNull(order.indexOf(dragged) - 1)) return null
    return BlockDrop(afterId, if (below) box.bottom else box.top)
}

/**
 * Il trascinamento in corso: quale blocco ([dragged]), dove cadrebbe
 * ([drop]) e dov'è il puntatore. [order] dà i blocchi della pagina
 * dall'alto; [onDrop] riceve il blocco lasciato e quello sotto cui va.
 */
class BlockDrag(
    private val picker: BlockPicker,
    private val order: () -> List<String>,
    private val onDrop: (String, String?) -> Unit
) {
    var dragged by mutableStateOf<String?>(null)
        private set
    var drop by mutableStateOf<BlockDrop?>(null)
        private set
    /** Il puntatore, nella finestra. */
    var pointer = Offset.Zero
        private set
    /** Gli scatti della rotella arrivati ai puntini mentre si trascina, che la pagina deve ancora scorrere. */
    private var wheel = 0f

    fun start(blockId: String, window: Offset) {
        dragged = blockId
        drop = null
        pointer = window
        wheel = 0f
    }

    /** La rotella mentre si trascina: [notches] scatti, in giù se più di zero. */
    fun wheel(notches: Float) {
        if (dragged != null) wheel += notches
    }

    /** Gli scatti ancora da scorrere, che da qui in poi sono della pagina. */
    fun takeWheel(): Float = wheel.also { wheel = 0f }

    fun moveTo(window: Offset) {
        pointer = window
        follow()
    }

    /** Di nuovo il blocco sotto il puntatore (anche fermo, se la pagina è scorsa). Fra due blocchi resta il posto di prima. */
    fun follow() {
        val id = dragged ?: return
        val (target, box) = picker.blockAt(pointer) ?: return
        drop = dropFor(id, target, below = pointer.y > (box.top + box.bottom) / 2f, box = box, order = order())
    }

    /** Fine del trascinamento: con [place] il blocco va dove dice la linea blu, se c'è. */
    fun end(place: Boolean) {
        val id = dragged
        val at = drop
        dragged = null
        drop = null
        wheel = 0f
        if (place && id != null && at != null) onDrop(id, at.afterId)
    }
}

/**
 * Mentre si trascina un blocco: la linea blu dove cadrà, larga quanto il
 * testo (a [inset] dai bordi), e la pagina che scorre ([scrollBy]) col
 * puntatore vicino al bordo in alto o in basso di [page] e con la rotella:
 * uno scatto, un ventesimo della pagina, come la rotella di Windows su una
 * lista di Compose (`WindowsWinUIConfig`).
 */
@Composable
fun BlockDragLayer(drag: BlockDrag, page: () -> LayoutCoordinates?, inset: Dp, scrollBy: suspend (Float) -> Unit) {
    if (drag.dragged == null) return
    val zone = with(LocalDensity.current) { EDGE_ZONE.toPx() }
    LaunchedEffect(drag) {
        while (true) {
            val list = page()?.takeIf { it.isAttached }
            if (list != null) {
                val height = list.size.height.toFloat()
                val step = edgeScrollStep(drag.pointer.y - list.localToWindow(Offset.Zero).y, height, zone) +
                    drag.takeWheel() * height / 20f
                if (step != 0f) {
                    scrollBy(step)
                    drag.follow()
                }
            }
            delay(EDGE_FRAME_MS)
        }
    }
    val drop = drag.drop ?: return
    Box(
        Modifier
            .offset {
                val top = page()?.takeIf { it.isAttached }?.localToWindow(Offset.Zero)?.y ?: 0f
                IntOffset(0, (drop.lineY - top - DROP_LINE.toPx() / 2f).roundToInt())
            }
            .padding(horizontal = inset)
            .fillMaxWidth()
            .height(DROP_LINE)
            .background(AccentBlue, RoundedCornerShape(2.dp))
    )
}

private val DROP_LINE = 3.dp
