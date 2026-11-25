package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.theme.NotionGray400

/**
 * **Le righe di un database si spostano trascinandole col mouse** (dal
 * 17/11/2026, chiesto dall'utente: "quando in un database non c'è un ordine
 * selezionato con Sort, permettimi di spostare le pagine semplicemente
 * draggandole con il mouse"). Sul PC, nella tabella e nell'elenco: passando
 * sopra una riga, nel suo margine sinistro c'è la maniglia ⋮⋮ (che si vede
 * solo col mouse sopra di lei, dal 17/11/2026); tenuta e
 * trascinata, la riga si fa chiara e una linea dice dove andrà; lasciata, va
 * lì ([onMove], lo stesso spostamento di "Move up" e "Move down").
 *
 * Con le stesse regole: niente quando un ordinamento decide il posto delle
 * righe o il database è bloccato (allora non c'è nessun [RowDrag], vedi
 * `LocalRowDrag`), e solo fra le righe dello stesso gruppo e dello stesso
 * livello (una sotto-riga fra le sorelle).
 */
class RowDrag(private val onMove: (rowIds: List<String>, target: DatabaseRowEntity, before: Boolean) -> Unit) {
    private class Slot(val row: DatabaseRowEntity, val group: List<String>, val bounds: Rect)

    // Le righe sullo schermo, dove stanno (nella finestra): in una lista lunga solo quelle disegnate.
    private val slots = HashMap<String, Slot>()

    /** La riga che si sta trascinando. */
    var dragging by mutableStateOf<String?>(null)
        private set

    /** Dove andrà: la riga accanto e se prima (vero) o dopo. */
    var target by mutableStateOf<Pair<String, Boolean>?>(null)
        private set

    /**
     * Le righe che vanno insieme (dal 25/11/2026): quella trascinata e, se è fra le scelte con la
     * casella, tutte le scelte (vedi `RowSelection`), che arrivano una dopo l'altra nel loro ordine.
     */
    var moving by mutableStateOf<Set<String>>(emptySet())
        private set

    private var pointerY = 0f

    fun place(row: DatabaseRowEntity, group: List<String>, bounds: Rect) {
        slots[row.id] = Slot(row, group, bounds)
    }

    fun forget(rowId: String) {
        if (dragging != rowId) slots.remove(rowId)
    }

    fun start(rowId: String, y: Float, chosen: Set<String> = emptySet()) {
        dragging = rowId
        moving = if (rowId in chosen) chosen else setOf(rowId)
        pointerY = y
        aim()
    }

    fun move(dy: Float) {
        pointerY += dy
        aim()
    }

    fun drop() {
        val rows = moving.toList()
        val (targetId, before) = target ?: (null to false)
        val to = targetId?.let { slots[it]?.row }
        dragging = null
        target = null
        moving = emptySet()
        if (rows.isNotEmpty() && to != null) onMove(rows, to, before)
    }

    fun cancel() {
        dragging = null
        target = null
        moving = emptySet()
    }

    /** La riga sotto il puntatore, fra quelle dove si può andare: prima di lei se si è sopra la sua metà. */
    private fun aim() {
        val moving = dragging?.let { slots[it] } ?: return
        val others = slots.values
            .filter { it.row.id !in this.moving && it.group == moving.group && it.row.parentRowId == moving.row.parentRowId }
            .sortedBy { it.bounds.top }
        if (others.isEmpty()) {
            target = null
            return
        }
        val below = others.firstOrNull { pointerY < it.bounds.center.y }
        target = if (below != null) below.row.id to true else others.last().row.id to false
    }
}

/** Il trascinamento delle righe di questa vista; null dove non si può (vedi [RowDrag]). */
val LocalRowDrag = staticCompositionLocalOf<RowDrag?> { null }

/**
 * Una riga che si può trascinare: dice dove sta, si fa chiara mentre la si
 * trascina, mostra la linea quando ci si sta per mettere accanto, e la
 * maniglia nel suo margine sinistro col mouse sopra. Senza [LocalRowDrag] è
 * la riga e basta.
 */
@Composable
fun RowDragBox(
    row: DatabaseRowEntity,
    group: List<String>,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val drag = LocalRowDrag.current
    // La casella per sceglierla, nel margine a sinistra (dal 25/11/2026, vedi `RowSelection`).
    val selection = LocalRowSelection.current
    if (drag == null && selection == null) {
        Box(modifier, content = content)
        return
    }
    var hovered by remember { mutableStateOf(false) }
    if (drag != null) DisposableEffect(drag, row.id) { onDispose { drag.forget(row.id) } }
    val line = MaterialTheme.colorScheme.primary
    val gutter = if (selection != null) ROW_SELECT_GUTTER else 0.dp
    val checked = selection?.selected?.contains(row.id) == true
    Box(
        modifier = modifier
            .then(if (selection != null) Modifier.bleedStart(gutter).widthIn(min = LocalTableLineWidth.current + gutter) else Modifier)
            .then(if (drag != null) Modifier.onGloballyPositioned { drag.place(row, group, it.boundsInRoot()) } else Modifier)
            .pointerInput(row.id) {
                awaitPointerEventScope {
                    while (true) {
                        when (awaitPointerEvent().type) {
                            PointerEventType.Enter -> hovered = true
                            PointerEventType.Exit -> hovered = false
                        }
                    }
                }
            }
            .graphicsLayer { alpha = if (drag != null && row.id in drag.moving) 0.4f else 1f }
            .drawWithContent {
                drawContent()
                val left = gutter.toPx()
                if (checked) drawRect(SelectedRowTint, topLeft = Offset(left, 0f), size = Size(size.width - left, size.height))
                val aim = drag?.target
                if (aim != null && aim.first == row.id) {
                    val y = if (aim.second) 1f else size.height - 1f
                    drawLine(line, Offset(left, y), Offset(size.width, y), strokeWidth = 2.dp.toPx())
                }
            }
    ) {
        Box(Modifier.padding(start = gutter), content = content)
        // Col mouse sopra la riga, o spuntata (dal 25/11/2026, chiesto dall'utente: "visibile solo se ci metto il mouse sopra").
        if (selection != null && (hovered || checked)) {
            SelectionCheckbox(
                checked = checked,
                onClick = { selection.toggle(row.id) },
                description = DbStrings.selectRow,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = SELECT_BOX_START)
            )
        }
        if (drag != null && (hovered || drag.dragging == row.id)) {
            val at = remember { arrayOfNulls<LayoutCoordinates>(1) }
            // Invisibile finché il mouse non è sopra di lei (dal 17/11/2026, come i sei puntini dei blocchi).
            val hover = remember { MutableInteractionSource() }
            val overHandle by hover.collectIsHoveredAsState()
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = gutter)
                    .size(width = ROW_HANDLE_WIDTH, height = ROW_HANDLE_HEIGHT)
                    .hoverable(hover)
                    .semantics { contentDescription = DbStrings.moveRow }
                    .onGloballyPositioned { at[0] = it }
                    .pointerInput(row.id) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val y = at[0]?.takeIf { it.isAttached }?.localToRoot(offset)?.y ?: return@detectDragGestures
                                drag.start(row.id, y, selection?.selected.orEmpty())
                            },
                            onDragEnd = { drag.drop() },
                            onDragCancel = { drag.cancel() }
                        ) { change, amount ->
                            change.consume()
                            drag.move(amount.y)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (overHandle || drag.dragging == row.id) {
                    Icon(Icons.Filled.DragIndicator, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

/** La maniglia sta nel margine prima del testo: 12 punti nella tabella, 16 nell'elenco. */
private val ROW_HANDLE_WIDTH = 12.dp
private val ROW_HANDLE_HEIGHT = 22.dp
