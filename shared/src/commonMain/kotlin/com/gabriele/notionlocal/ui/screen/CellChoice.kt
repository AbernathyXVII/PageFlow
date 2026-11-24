package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.gabriele.notionlocal.ui.theme.DarkBackground

// ---------------------------------------------------------------------------
// **La cella scelta e il pallino per riempire, come in Excel** (dal 24/11/2026,
// chiesto dall'utente con le foto di Notion): scrivendo in una cella di una
// tabella, Esc la lascia scelta, col bordo blu e un pallino nell'angolo in
// basso a destra; trascinando il pallino in giù (o in su), le celle della
// stessa colonna si colorano e, lasciato il mouse, prendono il suo valore.
// Con la cella scelta le frecce si spostano, Invio (o F2) torna a scriverci,
// Canc la svuota, Esc o un clic altrove la lasciano.
// ---------------------------------------------------------------------------

/** C'è solo mentre si vede il pallino della cella scelta: per i test. */
const val CELL_FILL_HANDLE_TAG = "cellFillHandle"

@Stable
class CellChoice {
    /** La cella scelta, "riga:colonna" (la chiave delle celle, vedi `CellEditor`); null = nessuna. */
    var chosen by mutableStateOf<String?>(null)

    /** Mentre si trascina il pallino: le celle che riceveranno il valore. */
    var filling by mutableStateOf<Set<String>>(emptySet())

    /** Dove sta ogni cella che si vede, nella finestra. */
    internal val bounds = HashMap<String, Rect>()

    /** Il valore di una cella, e quello che lo mette in più celle: li dà la tabella (vedi `DatabaseContent`). */
    var valueOf: (String) -> String = { "" }
    var apply: (keys: List<String>, value: String) -> Unit = { _, _ -> }

    /** Si comincia a scrivere nella cella: lo sa `CellGrid`. Falso se lì non si scrive. */
    var edit: (String) -> Boolean = { false }

    /** Le celle della colonna della cella scelta fra lei e l'altezza [y] della finestra, dall'alto. */
    fun cellsTowards(y: Float): List<String> {
        val from = chosen ?: return emptyList()
        val box = bounds[from] ?: return emptyList()
        val column = from.substringAfter(':')
        return bounds.entries.filter { (key, cell) ->
            key != from && key.substringAfter(':') == column && when {
                y > box.bottom -> cell.top >= box.bottom - 1f && cell.top < y
                y < box.top -> cell.bottom <= box.top + 1f && cell.bottom > y
                else -> false
            }
        }.sortedBy { it.value.top }.map { it.key }
    }

    /** La cella accanto alla scelta nel verso ([dx], [dy]); null se non ce n'è. */
    fun next(dx: Int, dy: Int): String? {
        val from = chosen?.let { bounds[it] } ?: return null
        return bounds.entries.filter { (key, b) ->
            key != chosen && when {
                dy > 0 -> b.top >= from.bottom - 1f && b.center.x in from.left..from.right
                dy < 0 -> b.bottom <= from.top + 1f && b.center.x in from.left..from.right
                dx > 0 -> b.left >= from.right - 1f && b.center.y in from.top..from.bottom
                else -> b.right <= from.left + 1f && b.center.y in from.top..from.bottom
            }
        }.minByOrNull { (_, b) -> (b.center - from.center).getDistance() }?.key
    }

    /** I tasti con la cella scelta; vero se il tasto era suo. */
    fun onKey(event: KeyEvent): Boolean {
        val key = chosen ?: return false
        if (event.type != KeyEventType.KeyDown || event.isCtrlPressed || event.isAltPressed) return false
        when (event.key) {
            Key.Escape -> chosen = null
            Key.DirectionUp -> next(0, -1)?.let { chosen = it }
            Key.DirectionDown -> next(0, 1)?.let { chosen = it }
            Key.DirectionLeft -> next(-1, 0)?.let { chosen = it }
            Key.DirectionRight -> next(1, 0)?.let { chosen = it }
            Key.Enter, Key.NumPadEnter, Key.F2 -> {
                chosen = null
                edit(key)
            }
            Key.Delete, Key.Backspace -> apply(listOf(key), "")
            else -> return false
        }
        return true
    }
}

val LocalCellChoice = staticCompositionLocalOf<CellChoice?> { null }

/**
 * I tasti della cella scelta li prende un punto invisibile col fuoco, come le celle scelte delle tabelle semplici;
 * perso il fuoco (un clic su un'altra cella, fuori dalla tabella) la scelta se ne va.
 */
@Composable
fun CellChoiceKeys(choice: CellChoice) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(choice.chosen != null) { if (choice.chosen != null) runCatching { focus.requestFocus() } }
    Box(
        Modifier
            .size(1.dp)
            .focusRequester(focus)
            .onFocusChanged { if (!it.isFocused) choice.chosen = null }
            .focusable()
            .onKeyEvent { choice.onKey(it) }
    )
}

/** Una cella di tabella che si può scegliere: registra dove sta, e scelta ha il bordo blu e il pallino. */
@Composable
fun ChoosableCell(key: String, modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    val choice = LocalCellChoice.current
    if (choice == null) {
        Box(modifier, content = content)
        return
    }
    DisposableEffect(choice, key) { onDispose { choice.bounds.remove(key) } }
    val blue = MaterialTheme.colorScheme.primary
    val chosen = choice.chosen == key
    Box(
        modifier
            // Scelta, sta sopra le celle accanto: il pallino sporge sul suo angolo (anche la sua riga sta sopra le altre).
            .then(if (chosen) Modifier.zIndex(1f) else Modifier)
            .onGloballyPositioned { choice.bounds[key] = it.boundsInWindow() }
            .then(if (key in choice.filling) Modifier.background(blue.copy(alpha = 0.14f)) else Modifier)
            .then(if (chosen) Modifier.border(2.dp, blue) else Modifier)
    ) {
        content()
        if (chosen) FillHandle(choice, Modifier.align(Alignment.BottomEnd))
    }
}

/** Il pallino in basso a destra della cella scelta: trascinato in verticale riempie le celle della colonna. */
@Composable
private fun FillHandle(choice: CellChoice, modifier: Modifier) {
    val blue = MaterialTheme.colorScheme.primary
    var at by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Box(
        modifier
            // **Sull'angolo, come in Excel e Notion** (dal 24/11/2026, chiesto dall'utente): metà dentro e metà fuori.
            .offset(FILL_HANDLE_SIZE / 2 - 1.dp, FILL_HANDLE_SIZE / 2 - 1.dp)
            .size(FILL_HANDLE_SIZE)
            .background(DarkBackground, CircleShape)
            .border(1.5.dp, blue, CircleShape)
            .testTag(CELL_FILL_HANDLE_TAG)
            .onGloballyPositioned { at = it }
            .pointerInput(choice) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    drag(down.id) { change ->
                        val y = at?.takeIf { it.isAttached }?.localToWindow(change.position)?.y
                        if (y != null) choice.filling = choice.cellsTowards(y).toSet()
                        change.consume()
                    }
                    val from = choice.chosen
                    val targets = choice.filling.toList()
                    choice.filling = emptySet()
                    if (from != null && targets.isNotEmpty()) choice.apply(targets, choice.valueOf(from))
                }
            }
    )
}

private val FILL_HANDLE_SIZE = 9.dp
