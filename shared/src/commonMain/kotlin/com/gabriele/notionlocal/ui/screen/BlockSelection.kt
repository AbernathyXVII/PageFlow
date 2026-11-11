package com.gabriele.notionlocal.ui.screen

import androidx.compose.ui.input.key.isShiftPressed
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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Job

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
    private class Entry(
        val coordinates: LayoutCoordinates,
        val blockAt: (Float) -> String?,
        val lineOf: ((String, LayoutCoordinates) -> LineBounds?)?
    )

    private val entries = HashMap<String, Entry>()

    fun put(key: String, coordinates: LayoutCoordinates, blockAt: (Float) -> String?, lineOf: ((String, LayoutCoordinates) -> LineBounds?)? = null) {
        entries[key] = Entry(coordinates, blockAt, lineOf)
    }

    fun remove(key: String) {
        entries.remove(key)
    }

    private fun find(window: Offset): Triple<String, Entry, String>? {
        for ((key, entry) in entries) {
            if (!entry.coordinates.isAttached) continue
            val local = entry.coordinates.windowToLocal(window)
            if (local.y < 0f || local.y > entry.coordinates.size.height) continue
            val id = entry.blockAt(local.y) ?: continue
            return Triple(key, entry, id)
        }
        return null
    }

    /** La voce e il blocco sotto il punto [window] della finestra, o null fra due voci. */
    fun at(window: Offset): Pair<String, String>? = find(window)?.let { (key, _, id) -> key to id }

    /**
     * Il blocco sotto il punto e dove sta la sua prima riga nella finestra,
     * per i sei puntini (`BlockHandle.kt`). Un blocco che non dice di più (un
     * divisore, un'immagine) dice solo dove comincia: la sua "riga" è alta al
     * massimo [islandLine].
     */
    fun lineAt(window: Offset, islandLine: Float): Pair<String, LineBounds>? {
        val (_, entry, id) = find(window) ?: return null
        val c = entry.coordinates
        val line = entry.lineOf?.invoke(id, c) ?: LineBounds(0f, minOf(c.size.height.toFloat(), islandLine))
        val origin = c.localToWindow(Offset.Zero)
        return id to LineBounds(origin.y + line.top, origin.y + line.bottom, line.start?.let { origin.x + it })
    }
}

/**
 * Dove sta la prima riga di un blocco: dall'alto al basso le sue lettere, e
 * dove comincia da sinistra (null: dove comincia il testo della pagina).
 */
class LineBounds(val top: Float, val bottom: Float, val start: Float? = null)

/**
 * Questa voce si segna fra quelle che il mouse può scegliere ([BlockPicker]):
 * [blockAt] dice quale blocco c'è a un'altezza della voce, [lineOf] dove sta
 * la prima riga di un blocco, rispetto alla voce (null: la voce lo dice da
 * sé). Se ne va con lei.
 */
@Composable
fun Modifier.pickableAs(key: String, lineOf: ((String, LayoutCoordinates) -> LineBounds?)? = null, blockAt: (Float) -> String?): Modifier {
    val picker = LocalBlockPicker.current ?: return this
    val latest = rememberUpdatedState(blockAt)
    val latestLine = rememberUpdatedState(lineOf)
    DisposableEffect(picker, key) { onDispose { picker.remove(key) } }
    return onGloballyPositioned { coordinates ->
        picker.put(key, coordinates, { y -> latest.value(y) }, latestLine.value?.let { { id, c -> latestLine.value?.invoke(id, c) } })
    }
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
 *
 * **Trascinando vicino al bordo in alto o in basso la pagina scorre da sola**
 * (dal 10/11/2026, chiesto dall'utente: "non mi fa scorrere con il mouse se
 * con esso seleziono il testo"), più veloce quanto più si è vicini al bordo
 * o oltre, con [scrollBy]: anche mentre si seleziona il testo di un campo, e
 * se sotto il puntatore arriva un altro blocco si passa ai blocchi interi.
 * La rotella resta della pagina.
 */
fun Modifier.mouseBlockSelection(
    picker: BlockPicker,
    coordinates: () -> LayoutCoordinates?,
    order: () -> List<String>,
    selectsOnClick: (String) -> Boolean,
    selected: () -> Boolean,
    scrollBy: suspend (Float) -> Unit,
    onSelect: (Set<String>?) -> Unit
): Modifier = pointerInput(picker) {
    coroutineScope {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (down.type != PointerType.Mouse || !currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
            val page = coordinates() ?: return@awaitEachGesture
            fun hit(position: Offset) = runCatching { picker.at(page.localToWindow(position)) }.getOrNull()
            val start = hit(down.position)
            val hadSelection = selected()
            var blocks = false
            var pointer = down.position
            var moved = false

            /**
             * Sotto il puntatore: nel campo dov'è cominciato si seleziona il testo, fuori i blocchi.
             * Con [scrolling] (la pagina che scorre da sola sotto il puntatore fermo) anche un'altra
             * riga dello stesso campo: il testo selezionato non seguirebbe la pagina. Vero se ora si scelgono blocchi.
             */
            fun follow(position: Offset, scrolling: Boolean = false): Boolean {
                if (start == null) return false
                val now = hit(position) ?: return blocks
                if (!blocks && now.first == start.first && !(scrolling && now.second != start.second)) return false
                blocks = true
                val ids = order()
                val from = ids.indexOf(start.second)
                val to = ids.indexOf(now.second)
                if (from >= 0 && to >= 0) onSelect(ids.subList(minOf(from, to), maxOf(from, to) + 1).toSet())
                return true
            }

            val zone = EDGE_ZONE.toPx()
            fun edgeSpeed(y: Float): Float = when {
                y < zone -> -EDGE_MAX_STEP * ((zone - y) / zone).coerceAtMost(2f)
                y > size.height - zone -> EDGE_MAX_STEP * ((y - (size.height - zone)) / zone).coerceAtMost(2f)
                else -> 0f
            }
            var edge: Job? = null
            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    // La rotella mentre si seleziona fa scorrere la pagina: non è un trascinamento.
                    if (event.type == PointerEventType.Scroll) continue
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) {
                        if (!blocks) {
                            when {
                                !moved && start != null && selectsOnClick(start.second) -> {
                                    change.consume()
                                    onSelect(setOf(start.second))
                                }
                                hadSelection -> onSelect(null)
                            }
                        }
                        break
                    }
                    pointer = change.position
                    if ((pointer - down.position).getDistance() > viewConfiguration.touchSlop) moved = true
                    if (follow(pointer)) change.consume()
                    if (moved && edge?.isActive != true && edgeSpeed(pointer.y) != 0f) {
                        edge = launch {
                            while (true) {
                                val step = edgeSpeed(pointer.y)
                                if (step == 0f) break
                                scrollBy(step)
                                follow(pointer, scrolling = true)
                                delay(EDGE_FRAME_MS)
                            }
                        }
                    }
                }
            } finally {
                edge?.cancel()
            }
        }
    }
}

/** Quanto vicino al bordo comincia lo scorrimento da soli, e quanto al massimo per giro (raddoppia oltre il bordo). */
private val EDGE_ZONE = 48.dp
private const val EDGE_MAX_STEP = 18f
private const val EDGE_FRAME_MS = 16L

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

/**
 * **Ctrl+Z e Ctrl+Y (o Ctrl+Maiusc+Z) sono l'Annulla della pagina** (dal
 * 11/11/2026, trovato dall'utente sul PC: "CTRL + Z non funziona
 * correttamente"). Il campo di testo di Compose ha un annulla suo, che
 * conosce solo il testo del campo: rimetteva un testo vecchio del gruppo di
 * righe, che la pagina riscriveva sui blocchi, e non sapeva niente di
 * divisori o blocchi eliminati. Vero per Annulla, falso per Ripristina, null
 * per gli altri tasti.
 */
fun undoKeyOf(event: KeyEvent): Boolean? {
    if (event.type != KeyEventType.KeyDown || !event.isCtrlPressed) return null
    return when {
        event.key == Key.Z && !event.isShiftPressed -> true
        event.key == Key.Y || (event.key == Key.Z && event.isShiftPressed) -> false
        else -> null
    }
}

/** Ctrl+A nei campi della pagina: tutta la pagina (non solo il pezzo di testo). */
fun isSelectAll(event: KeyEvent): Boolean = event.type == KeyEventType.KeyDown && event.isCtrlPressed && event.key == Key.A
