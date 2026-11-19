package com.gabriele.notionlocal.ui.screen

import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.Stroke
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
        val lineOf: ((String, LayoutCoordinates) -> LineBounds?)?,
        val extentOf: ((String, LayoutCoordinates) -> LineBounds?)?,
        /** Una riga dentro un'altra voce (una colonna): conta anche da sinistra a destra, e vince sulla voce che la contiene. */
        val nested: Boolean,
        /** Quanto il testo sta più all'interno della voce, a sinistra e a destra (in pixel). */
        val textInset: Float,
        /** Se la voce ha i suoi sei puntini: il gruppo di colonne no (vedi `lineAt`). */
        val handle: Boolean
    )

    private val entries = HashMap<String, Entry>()

    fun put(
        key: String,
        coordinates: LayoutCoordinates,
        blockAt: (Float) -> String?,
        lineOf: ((String, LayoutCoordinates) -> LineBounds?)? = null,
        extentOf: ((String, LayoutCoordinates) -> LineBounds?)? = null,
        nested: Boolean = false,
        textInset: Float = 0f,
        handle: Boolean = true
    ) {
        entries[key] = Entry(coordinates, blockAt, lineOf, extentOf, nested, textInset, handle)
    }

    fun remove(key: String) {
        entries.remove(key)
    }

    /**
     * La voce sotto il punto: con [inner] prima le righe dentro le colonne
     * (dal 12/11/2026), che stanno affiancate e quindi contano anche da
     * sinistra a destra; poi le voci della pagina, che contano solo
     * dall'alto al basso (il margine dei sei puntini è ancora loro).
     */
    private fun find(window: Offset, inner: Boolean): Triple<String, Entry, String>? {
        if (inner) {
            for ((key, entry) in entries) {
                if (!entry.nested || !entry.coordinates.isAttached) continue
                val local = entry.coordinates.windowToLocal(window)
                val size = entry.coordinates.size
                if (local.x < 0f || local.x > size.width || local.y < 0f || local.y > size.height) continue
                val id = entry.blockAt(local.y) ?: continue
                return Triple(key, entry, id)
            }
        }
        for ((key, entry) in entries) {
            if (entry.nested || !entry.coordinates.isAttached) continue
            val local = entry.coordinates.windowToLocal(window)
            if (local.y < 0f || local.y > entry.coordinates.size.height) continue
            val id = entry.blockAt(local.y) ?: continue
            return Triple(key, entry, id)
        }
        return null
    }

    /** La voce e il blocco della pagina sotto il punto [window] della finestra, o null fra due voci (per scegliere più blocchi). */
    fun at(window: Offset): Pair<String, String>? = find(window, inner = false)?.let { (key, _, id) -> key to id }

    /**
     * Il blocco sotto il punto e dove sta la sua prima riga nella finestra,
     * per i sei puntini (`BlockHandle.kt`). Un blocco che non dice di più (un
     * divisore, un'immagine) dice solo dove comincia: la sua "riga" è alta al
     * massimo [islandLine].
     */
    fun lineAt(window: Offset, islandLine: Float): Pair<String, LineBounds>? {
        val (_, entry, id) = find(window, inner = true) ?: return null
        // **Il gruppo di colonne non ha puntini suoi** (dal 13/11/2026, come su Notion;
        // trovato dall'utente: "un'icona dei sei puntini è sbucata dal nulla" a metà di
        // un database in colonna): li hanno le sue righe. Fuori da loro restano quelli di prima.
        if (!entry.handle) return null
        val c = entry.coordinates
        val line = entry.lineOf?.invoke(id, c) ?: LineBounds(0f, minOf(c.size.height.toFloat(), islandLine))
        val origin = c.localToWindow(Offset.Zero)
        return id to LineBounds(origin.y + line.top, origin.y + line.bottom, line.start?.let { origin.x + it })
    }

    /**
     * Il blocco sotto il punto e dove sta **tutto** nella finestra, per
     * trascinarci sopra un altro blocco (`BlockDrag.kt`): dall'alto al basso,
     * e da sinistra a destra dove sta il suo testo (nella pagina, o nella
     * colonna). Un blocco
     * che non dice di più (un divisore, un database) è tutta la sua voce.
     */
    fun blockAt(window: Offset): Pair<String, BlockBox>? {
        val (_, entry, id) = find(window, inner = true) ?: return null
        val c = entry.coordinates
        val box = entry.extentOf?.invoke(id, c) ?: LineBounds(0f, c.size.height.toFloat())
        val origin = c.localToWindow(Offset.Zero)
        return id to BlockBox(origin.y + box.top, origin.y + box.bottom, origin.x + entry.textInset, origin.x + c.size.width - entry.textInset)
    }

    /** Dove sta adesso, dall'alto al basso nella finestra, il blocco [id]; null se non si vede (vedi [blockAt]). */
    fun boundsOf(id: String): LineBounds? {
        for (entry in entries.values) {
            val c = entry.coordinates
            if (entry.nested || !c.isAttached) continue
            val box = entry.extentOf?.invoke(id, c) ?: if (entry.extentOf == null && entry.blockAt(0f) == id) LineBounds(0f, c.size.height.toFloat()) else null
            if (box != null) {
                val origin = c.localToWindow(Offset.Zero)
                return LineBounds(origin.y + box.top, origin.y + box.bottom)
            }
        }
        return null
    }

    /**
     * Le voci della pagina nella finestra: da sinistra a destra dove sta il
     * loro testo, e dove finisce in basso l'ultima che si vede. Null senza voci.
     */
    fun pageExtent(): Triple<Float, Float, Float>? {
        var left = Float.MAX_VALUE
        var right = -Float.MAX_VALUE
        var bottom = -Float.MAX_VALUE
        for (entry in entries.values) {
            val c = entry.coordinates
            if (entry.nested || !c.isAttached) continue
            val origin = c.localToWindow(Offset.Zero)
            left = minOf(left, origin.x + entry.textInset)
            right = maxOf(right, origin.x + c.size.width - entry.textInset)
            bottom = maxOf(bottom, origin.y + c.size.height)
        }
        return if (left == Float.MAX_VALUE) null else Triple(left, right, bottom)
    }
}

/** Dove sta un blocco nella finestra, da [top] a [bottom] e da [left] a [right] (vedi `BlockPicker.blockAt`). */
data class BlockBox(val top: Float, val bottom: Float, val left: Float, val right: Float)

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
fun Modifier.pickableAs(
    key: String,
    lineOf: ((String, LayoutCoordinates) -> LineBounds?)? = null,
    extentOf: ((String, LayoutCoordinates) -> LineBounds?)? = null,
    nested: Boolean = false,
    textInset: Float = 0f,
    handle: Boolean = true,
    blockAt: (Float) -> String?
): Modifier {
    val picker = LocalBlockPicker.current ?: return this
    val latest = rememberUpdatedState(blockAt)
    val latestLine = rememberUpdatedState(lineOf)
    val latestExtent = rememberUpdatedState(extentOf)
    DisposableEffect(picker, key) { onDispose { picker.remove(key) } }
    return onGloballyPositioned { coordinates ->
        picker.put(
            key, coordinates, { y -> latest.value(y) },
            latestLine.value?.let { { id, c -> latestLine.value?.invoke(id, c) } },
            latestExtent.value?.let { { id, c -> latestExtent.value?.invoke(id, c) } },
            nested,
            textInset,
            handle
        )
    }
}

/** I blocchi selezionati (quelli di primo livello) e il colore del velo, per chi li disegna. */
class BlockSelectionLook(val selected: State<Set<String>>, val veil: Color)

val LocalBlockPicker = staticCompositionLocalOf<BlockPicker?> { null }
val LocalBlockSelection = staticCompositionLocalOf<BlockSelectionLook?> { null }

/**
 * **Maiusc+freccia oltre il campo: i blocchi interi** (dal 17/11/2026,
 * trovato dall'utente sul PC: selezionando con Maiusc e le frecce "non posso
 * andare giù o su perché il titolone bugga tutto"): un titolo più grande sta
 * in un campo suo, e la selezione del testo non esce da un campo. Arrivati in
 * cima o in fondo al campo, come in Notion si passa ai blocchi interi: dalla
 * riga [anchor] (dove è cominciata la selezione) a quella dopo (o prima, con
 * [down] falso) di [edge], l'ultima (o la prima) del campo. Vero se è
 * successo; null dove non si può.
 */
val LocalBlockExtend = staticCompositionLocalOf<((anchor: String, edge: String, down: Boolean) -> Boolean)?> { null }

/** I blocchi di [order] da [a] a [b] compresi, in qualunque verso; vuoto se uno dei due non c'è. */
fun blockRange(order: List<String>, a: String, b: String): Set<String> {
    val from = order.indexOf(a)
    val to = order.indexOf(b)
    if (from < 0 || to < 0) return emptySet()
    return order.subList(minOf(from, to), maxOf(from, to) + 1).toSet()
}

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
    /** Il rettangolo azzurro, nelle coordinate della pagina (vedi [drawMarquee]); null: niente rettangolo. */
    marquee: MutableState<Rect?>? = null,
    /**
     * I blocchi che col mouse si trascinano da soli (un database: le righe dalla maniglia, le
     * schede, le colonne): premuti fuori dalla colonna del testo non fanno partire il rettangolo.
     */
    ownsDrag: (String) -> Boolean = { false },
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

            // **Il rettangolo azzurro, come sul desktop di Windows e del Mac** (dal 17/11/2026, chiesto
            // dall'utente: "selezionare i blocchi di una pagina già ancora da prima di toccarli"): premuto
            // fuori dal testo (nei margini ai lati, o sotto l'ultimo blocco) e trascinando, i blocchi che
            // il rettangolo tocca sono scelti. Si aggancia al primo blocco che tocca, e lo segue mentre la
            // pagina scorre (al bordo o con la rotella): la scelta va da lui a quello verso il puntatore.
            // Dal 19/11/2026 non su un database (l'utente: spostando una pagina "mi appare quel famoso
            // rettangolino", perché la maniglia delle righe sta a sinistra della colonna del testo).
            val boxing = marquee != null && (start == null || !ownsDrag(start.second)) && runCatching {
                val at = page.localToWindow(down.position)
                val (left, right, bottom) = picker.pageExtent() ?: return@runCatching false
                at.x < left || at.x > right || (start == null && at.y > bottom)
            }.getOrDefault(false)
            var anchor: String? = null
            var anchorDy = 0f
            var startY = down.position.y
            fun topOf(id: String): Float? = runCatching { picker.boundsOf(id) }.getOrNull()?.let { page.windowToLocal(Offset(0f, it.top)).y }
            /** Il primo blocco andando da [fromY] verso [towardY], o null se in mezzo non ce n'è. */
            fun nearest(fromY: Float, towardY: Float): String? {
                val step = if (towardY >= fromY) 4f else -4f
                var y = fromY
                while (if (step > 0) y <= towardY else y >= towardY) {
                    hit(Offset(down.position.x, y))?.let { return it.second }
                    y += step
                }
                return null
            }
            fun followBox(position: Offset) {
                val id = anchor
                if (id == null) {
                    anchor = nearest(startY, position.y)
                    anchor?.let { found -> topOf(found)?.let { anchorDy = startY - it } }
                } else {
                    // Il blocco di partenza si è spostato con la pagina; fuori dallo schermo, il rettangolo parte dal bordo.
                    startY = topOf(id)?.let { it + anchorDy } ?: if (startY < position.y) 0f else size.height.toFloat()
                }
                marquee?.value = Rect(
                    minOf(down.position.x, position.x), minOf(startY, position.y),
                    maxOf(down.position.x, position.x), maxOf(startY, position.y)
                )
                val from = anchor
                val to = hit(position)?.second ?: nearest(position.y, startY)
                val ids = order()
                onSelect(if (from != null && to != null) blockRange(ids, from, to) else emptySet())
            }

            /**
             * Sotto il puntatore: nella riga dov'è cominciato si seleziona il testo, appena fuori i
             * blocchi interi, **come in Notion** (dal 17/11/2026, scelto dall'utente: prima il testo
             * continuava fino al bordo del campo, e un titolo grande, che sta in un campo suo, faceva
             * passare ai blocchi a metà strada). Poi si resta sui blocchi. Vero se ora si scelgono blocchi.
             */
            fun follow(position: Offset): Boolean {
                if (start == null) return false
                val now = hit(position) ?: return blocks
                if (!blocks && now.second == start.second) return false
                blocks = true
                val ids = order()
                val from = ids.indexOf(start.second)
                val to = ids.indexOf(now.second)
                if (from >= 0 && to >= 0) onSelect(ids.subList(minOf(from, to), maxOf(from, to) + 1).toSet())
                return true
            }

            val zone = EDGE_ZONE.toPx()
            fun edgeSpeed(y: Float): Float = edgeScrollStep(y, size.height.toFloat(), zone)
            var edge: Job? = null
            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    // La rotella mentre si seleziona fa scorrere la pagina: non è un trascinamento, ma
                    // sotto il puntatore fermo arrivano altre righe, e la selezione le segue (dal 17/11/2026).
                    if (event.type == PointerEventType.Scroll) {
                        launch {
                            delay(EDGE_FRAME_MS * 2)
                            if (boxing) {
                                if (moved) followBox(pointer)
                            } else {
                                follow(pointer)
                            }
                        }
                        continue
                    }
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) {
                        // Lasciato il rettangolo: i blocchi restano scelti, e il clic non arriva alla pagina.
                        if (boxing && moved) change.consume()
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
                    if (boxing) {
                        if (moved) {
                            blocks = true
                            followBox(pointer)
                            change.consume()
                        }
                    } else if (follow(pointer)) {
                        change.consume()
                    }
                    if (moved && edge?.isActive != true && edgeSpeed(pointer.y) != 0f) {
                        edge = launch {
                            while (true) {
                                val step = edgeSpeed(pointer.y)
                                if (step == 0f) break
                                scrollBy(step)
                                if (boxing) followBox(pointer) else follow(pointer)
                                delay(EDGE_FRAME_MS)
                            }
                        }
                    }
                }
            } finally {
                edge?.cancel()
                marquee?.value = null
            }
        }
    }
}

/** Il rettangolo azzurro di [mouseBlockSelection], sopra la pagina: un velo di [color] e il suo bordo. */
fun Modifier.drawMarquee(marquee: State<Rect?>, color: Color): Modifier = drawWithContent {
    drawContent()
    val box = marquee.value ?: return@drawWithContent
    drawRect(color.copy(alpha = 0.15f), topLeft = box.topLeft, size = box.size)
    drawRect(color.copy(alpha = 0.7f), topLeft = box.topLeft, size = box.size, style = Stroke(width = 1.dp.toPx()))
}

/**
 * Di quanto scorre da sola la pagina alta [height] a ogni giro, col puntatore
 * all'altezza [y]: verso l'alto (meno) o il basso entro [zone] dal bordo, più
 * veloce quanto più ci si avvicina, il doppio oltre. 0 lontano dai bordi.
 */
fun edgeScrollStep(y: Float, height: Float, zone: Float): Float = when {
    y < zone -> -EDGE_MAX_STEP * ((zone - y) / zone).coerceAtMost(2f)
    y > height - zone -> EDGE_MAX_STEP * ((y - (height - zone)) / zone).coerceAtMost(2f)
    else -> 0f
}

/** Quanto vicino al bordo comincia lo scorrimento da soli, e quanto al massimo per giro (raddoppia oltre il bordo). */
internal val EDGE_ZONE = 48.dp
private const val EDGE_MAX_STEP = 18f
internal const val EDGE_FRAME_MS = 16L

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

/**
 * **Ctrl+A: prima il blocco, poi la pagina** (dal 18/11/2026, chiesto dall'utente, come su Notion):
 * il campo col cursore ci mette [inField], che sceglie il testo del suo blocco e dice sì; se è già
 * tutto scelto (o il blocco è vuoto) dice no, e Ctrl+A sceglie tutta la pagina come prima.
 */
class BlockSelectAll {
    var inField: (() -> Boolean)? = null
}

val LocalBlockSelectAll = staticCompositionLocalOf<BlockSelectAll?> { null }

/**
 * Nel testo [text] scelto da [from] a [to]: il blocco (la riga) del cursore da scegliere tutto, o
 * null se è già tutto scelto, è vuoto o la scelta va già oltre.
 */
fun ownLineToSelect(text: String, from: Int, to: Int): IntRange? {
    val start = text.lastIndexOf('\n', from - 1) + 1
    val end = text.indexOf('\n', from).let { if (it < 0) text.length else it }
    if (start == end || to > end || (from <= start && to >= end)) return null
    return start until end
}
