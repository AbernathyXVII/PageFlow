package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

// ---------------------------------------------------------------------------
// **Più blocchi selezionati col mouse** (dal 10/11/2026, chiesto dall'utente
// sul PC: "selezionare più blocchi di testo con il mouse", "cancellare i
// divider se ci clicco sopra e premo Canc o Backspace"). Trascinando dentro
// un campo si seleziona il suo testo come sempre; uscendo verso altri blocchi
// si selezionano i blocchi interi, come su Notion, col velo blu della pagina
// selezionata (`PageSelection.kt`). Un clic su un divisore lo seleziona.
// Poi Canc o Backspace li eliminano, Ctrl+C e Ctrl+X li copiano e tagliano,
// Esc o un clic altrove li lasciano. Solo col mouse: col dito sul telefono
// non cambia niente.
// ---------------------------------------------------------------------------

/**
 * **Dove sta ogni blocco sullo schermo**, per trovare col mouse quello sotto
 * il puntatore: ogni voce della pagina si segna con le sue coordinate, e un
 * campo di più righe sa dire quale riga c'è a una certa altezza.
 */
class BlockPicker {
    private class Entry(val coordinates: LayoutCoordinates, val blockAt: (Float) -> String?)

    private val entries = HashMap<String, Entry>()

    fun put(key: String, coordinates: LayoutCoordinates, blockAt: (Float) -> String?) {
        entries[key] = Entry(coordinates, blockAt)
    }

    fun remove(key: String) {
        entries.remove(key)
    }

    /** La voce e il blocco sotto il punto [window] della finestra, o null fra due voci. */
    fun at(window: Offset): Pair<String, String>? {
        for ((key, entry) in entries) {
            if (!entry.coordinates.isAttached) continue
            val local = entry.coordinates.windowToLocal(window)
            if (local.y < 0f || local.y > entry.coordinates.size.height) continue
            val id = entry.blockAt(local.y) ?: continue
            return key to id
        }
        return null
    }
}

/**
 * Questa voce si segna fra quelle che il mouse può scegliere ([BlockPicker]):
 * [blockAt] dice quale blocco c'è a un'altezza della voce. Se ne va con lei.
 */
@Composable
fun Modifier.pickableAs(key: String, blockAt: (Float) -> String?): Modifier {
    val picker = LocalBlockPicker.current ?: return this
    val latest = rememberUpdatedState(blockAt)
    DisposableEffect(picker, key) { onDispose { picker.remove(key) } }
    return onGloballyPositioned { coordinates -> picker.put(key, coordinates) { y -> latest.value(y) } }
}

/** I blocchi selezionati (quelli di primo livello) e il colore del velo, per chi li disegna. */
class BlockSelectionLook(val selected: State<Set<String>>, val veil: Color)

val LocalBlockPicker = staticCompositionLocalOf<BlockPicker?> { null }
val LocalBlockSelection = staticCompositionLocalOf<BlockSelectionLook?> { null }

/**
 * Il gesto del mouse sulla pagina, guardato prima di tutti e preso solo
 * quando diventa una selezione di blocchi: trascinando fuori dal campo
 * dov'è cominciato, [onSelect] riceve i blocchi dal primo a quello sotto il
 * puntatore ([order], l'ordine della pagina); un clic su un divisore
 * ([selectsOnClick]) seleziona lui; un clic altrove toglie la selezione
 * che c'era ([selected]) e arriva al testo come sempre.
 */
fun Modifier.mouseBlockSelection(
    picker: BlockPicker,
    coordinates: () -> LayoutCoordinates?,
    order: () -> List<String>,
    selectsOnClick: (String) -> Boolean,
    selected: () -> Boolean,
    onSelect: (Set<String>?) -> Unit
): Modifier = pointerInput(picker) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (down.type != PointerType.Mouse || !currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
        val page = coordinates() ?: return@awaitEachGesture
        fun hit(position: Offset) = runCatching { picker.at(page.localToWindow(position)) }.getOrNull()
        val start = hit(down.position)
        val hadSelection = selected()
        var blocks = false
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) {
                if (!blocks) {
                    when {
                        start != null && selectsOnClick(start.second) -> {
                            change.consume()
                            onSelect(setOf(start.second))
                        }
                        hadSelection -> onSelect(null)
                    }
                }
                break
            }
            if (start == null) continue
            val now = hit(change.position) ?: continue
            // Ancora nel campo dov'è cominciato: lì si seleziona il testo.
            if (!blocks && now.first == start.first) continue
            blocks = true
            change.consume()
            val ids = order()
            val from = ids.indexOf(start.second)
            val to = ids.indexOf(now.second)
            if (from < 0 || to < 0) continue
            onSelect(ids.subList(minOf(from, to), maxOf(from, to) + 1).toSet())
        }
    }
}

/** I tasti della selezione: cosa fare, o null se il tasto non le serve. */
enum class SelectionKey { DELETE, COPY, CUT, ALL, CLOSE }

fun selectionKeyOf(event: KeyEvent): SelectionKey? {
    if (event.type != KeyEventType.KeyDown) return null
    return when {
        event.key == Key.Delete || event.key == Key.Backspace -> SelectionKey.DELETE
        event.key == Key.Escape -> SelectionKey.CLOSE
        event.isCtrlPressed && event.key == Key.C -> SelectionKey.COPY
        event.isCtrlPressed && event.key == Key.X -> SelectionKey.CUT
        event.isCtrlPressed && event.key == Key.A -> SelectionKey.ALL
        else -> null
    }
}

/** Ctrl+A nei campi della pagina: tutta la pagina (non solo il pezzo di testo). */
fun isSelectAll(event: KeyEvent): Boolean = event.type == KeyEventType.KeyDown && event.isCtrlPressed && event.key == Key.A
