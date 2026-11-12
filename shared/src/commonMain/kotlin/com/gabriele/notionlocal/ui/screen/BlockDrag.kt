package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.MAX_COLUMNS
import com.gabriele.notionlocal.data.entity.columnsAfterBeside
import com.gabriele.notionlocal.ui.theme.AccentBlue
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// **Spostare i blocchi trascinando i sei puntini** (dal 12/11/2026, chiesto
// dall'utente, solo sul PC, come su Notion): tenendo premuti i sei puntini
// accanto a un blocco (`BlockHandle.kt`) e trascinando, una linea blu dice
// dove andrà, sopra o sotto il blocco sotto il mouse secondo la metà; vicino
// al bordo in alto o in basso la pagina scorre da sola, e scorre anche con
// la rotella (trovato dall'utente: "non posso scrollare con il mouse"; il
// mouse è dei puntini finché si tiene premuto, e la rotella arrivava a loro
// invece che alla pagina). Lasciando, il blocco va lì: si può annullare, e
// arrivato si illumina.
//
// **Le colonne trascinando** (dal 12/11/2026, chiesto dall'utente con le
// foto di Notion): col mouse sopra l'inizio o la fine del testo di un blocco
// la linea è verticale, a sinistra o a destra, e lasciando il blocco gli va
// accanto: nascono le colonne, o se ne aggiunge una (vedi
// `PageRepository.moveBlockBeside`). Nel margine dei puntini, cioè
// trascinando dritto in giù, la linea resta orizzontale. Anche le righe
// dentro le colonne hanno i loro puntini, e si spostano fra le colonne e
// fuori.
// ---------------------------------------------------------------------------

/** Dove va il blocco trascinato rispetto a quello sotto il mouse. */
enum class DropPlace { BEFORE, AFTER, LEFT, RIGHT }

/**
 * Dove cadrà il blocco trascinato: [place] rispetto a [targetId]. La linea
 * blu, nella finestra, comincia in ([x], [y]) ed è lunga [length]: in giù se
 * [vertical], se no verso destra.
 */
data class BlockDrop(
    val targetId: String,
    val place: DropPlace,
    val x: Float,
    val y: Float,
    val length: Float,
    val vertical: Boolean
)

/**
 * Dove cade [dragged] col mouse in [pointer] sul blocco [target], il cui
 * testo nella finestra sta in [box]: accanto, se il mouse
 * è sopra l'inizio o la fine del testo (entro [side]); se no sopra o sotto,
 * secondo la metà. [blocks] sono quelli della pagina. Null se il blocco
 * resterebbe dov'è (sopra se stesso, o subito sopra o sotto), se finirebbe
 * dentro se stesso, o se un gruppo di colonne finirebbe in una colonna.
 */
fun dropFor(dragged: String, target: String, pointer: Offset, box: BlockBox, side: Float, blocks: List<BlockEntity>): BlockDrop? {
    val byId = blocks.associateBy { it.id }
    val moving = byId[dragged] ?: return null
    // Dentro se stesso no: si risale dalla riga sotto il mouse.
    var up: BlockEntity? = byId[target]
    while (up != null) {
        if (up.id == dragged) return null
        up = up.parentBlockId?.let { byId[it] }
    }
    val holder = byId[target]?.parentBlockId?.let { byId[it] }
    val groupMoving = moving.type == BlockType.COLUMN_LIST || moving.type == BlockType.COLUMN
    if (groupMoving && holder?.type == BlockType.COLUMN) return null
    val textStart = box.left
    val textEnd = box.right
    val zone = minOf(side, (textEnd - textStart) / 4f)
    // Un gruppo con già quattro colonne non ne prende un'altra: niente linea verticale.
    val besideAllowed = !groupMoving && (columnsAfterBeside(blocks, dragged, target) ?: 0) <= MAX_COLUMNS
    if (besideAllowed) {
        if (pointer.x >= textStart && pointer.x < textStart + zone) {
            return BlockDrop(target, DropPlace.LEFT, textStart - LINE_GAP, box.top, box.bottom - box.top, vertical = true)
        }
        if (pointer.x > textEnd - zone) {
            return BlockDrop(target, DropPlace.RIGHT, textEnd + LINE_GAP, box.top, box.bottom - box.top, vertical = true)
        }
    }
    val below = pointer.y > (box.top + box.bottom) / 2f
    val siblings = blocks.filter { it.parentBlockId == moving.parentBlockId }.sortedBy { it.orderIndex }
    val at = siblings.indexOfFirst { it.id == dragged }
    val stays = if (below) siblings.getOrNull(at - 1)?.id == target else siblings.getOrNull(at + 1)?.id == target
    if (stays) return null
    return BlockDrop(target, if (below) DropPlace.AFTER else DropPlace.BEFORE, textStart, if (below) box.bottom else box.top, textEnd - textStart, vertical = false)
}

/** Quanto la linea verticale sta staccata dal testo. */
private const val LINE_GAP = 6f

/**
 * Il trascinamento in corso: quale blocco ([dragged]), dove cadrebbe
 * ([drop]) e dov'è il puntatore. [blocks] dà i blocchi della pagina, [side]
 * (in pixel) quanto è largo il bordo del testo che li mette accanto;
 * [onDrop] riceve il blocco lasciato e dove va.
 */
class BlockDrag(
    private val picker: BlockPicker,
    private val blocks: () -> List<BlockEntity>,
    private val side: () -> Float,
    private val onDrop: (String, BlockDrop) -> Unit
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
        drop = dropFor(id, target, pointer, box, side(), blocks())
    }

    /** Fine del trascinamento: con [place] il blocco va dove dice la linea blu, se c'è. */
    fun end(place: Boolean) {
        val id = dragged
        val at = drop
        dragged = null
        drop = null
        wheel = 0f
        if (place && id != null && at != null) onDrop(id, at)
    }
}

/**
 * Mentre si trascina un blocco: la linea blu dove cadrà (orizzontale larga
 * quanto il testo, o verticale accanto a lui), e la pagina che scorre
 * ([scrollBy]) col puntatore vicino al bordo in alto o in basso di [page] e
 * con la rotella: uno scatto, un ventesimo della pagina, come la rotella di
 * Windows su una lista di Compose (`WindowsWinUIConfig`).
 */
@Composable
fun BlockDragLayer(drag: BlockDrag, page: () -> LayoutCoordinates?, scrollBy: suspend (Float) -> Unit) {
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
    val density = LocalDensity.current
    val length = with(density) { drop.length.toDp() }
    Box(
        Modifier
            .offset {
                val origin = page()?.takeIf { it.isAttached }?.localToWindow(Offset.Zero) ?: Offset.Zero
                val half = DROP_LINE.toPx() / 2f
                IntOffset(
                    (drop.x - origin.x - if (drop.vertical) half else 0f).roundToInt(),
                    (drop.y - origin.y - if (drop.vertical) 0f else half).roundToInt()
                )
            }
            .then(if (drop.vertical) Modifier.width(DROP_LINE).height(length) else Modifier.width(length).height(DROP_LINE))
            .background(AccentBlue, RoundedCornerShape(2.dp))
    )
}

private val DROP_LINE = 3.dp
