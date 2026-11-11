package com.gabriele.notionlocal.ui.screen

import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriele.notionlocal.data.entity.InkStrokeEntity
import com.gabriele.notionlocal.data.entity.InkTool
import com.gabriele.notionlocal.data.ink.ERASER_SIZES
import com.gabriele.notionlocal.data.ink.EraserMode
import com.gabriele.notionlocal.data.ink.InkPen
import com.gabriele.notionlocal.data.ink.InkPoint
import com.gabriele.notionlocal.data.ink.InkPrefs
import com.gabriele.notionlocal.data.ink.NEUTRAL_PRESSURE
import com.gabriele.notionlocal.data.ink.nearestPreset
import com.gabriele.notionlocal.data.ink.resolveAnchor
import com.gabriele.notionlocal.data.ink.thicker
import com.gabriele.notionlocal.data.ink.thicknessPresets
import com.gabriele.notionlocal.data.ink.thinner
import com.gabriele.notionlocal.ui.i18n.InkStrings
import com.gabriele.notionlocal.ui.theme.AccentBlue
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.DarkSurfaceVariant
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.viewmodel.InkStroke
import com.gabriele.notionlocal.viewmodel.pressureFactor
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/*
 * **Il disegno a mano sulle pagine** (dal 25/09/2026): lo strato dei
 * tratti sopra la pagina, la penna e il dito che disegnano, e la barra
 * degli strumenti come quella di OneNote che l'utente ha mandato in foto.
 *
 * I tratti stanno **attaccati alle voci della lista** della pagina (la
 * testata, un gruppo di righe, un database...): vedi `InkStrokeEntity`.
 * Per disegnarli bisogna sapere dov'è ogni voce sullo schermo, anche
 * quella appena uscita in alto o in basso, se il suo tratto è lungo e ne
 * sporge: per quelle si contano le altezze che si sono viste passare
 * (`heights`).
 */

/** Dove sono le voci della pagina, e il tratto che si sta disegnando. */
@Stable
class InkPageState(val listState: LazyListState) {

    /** Le chiavi delle voci della lista, nell'ordine: la testata e poi i blocchi. */
    var itemKeys: List<Any> = emptyList()

    /** Blocco → voce della lista (la testata è `HEADER_ANCHOR` → 0). */
    var itemOfBlock: Map<String, Int> = emptyMap()

    /** L'ancoraggio di ogni voce: quello che prende un tratto cominciato lì. */
    var anchorOfItem: List<String> = emptyList()

    /**
     * **Le voci di testo che scorrono di lato** con "Full line view" (dal
     * 19/10/2026), come indici della lista, e di quanto: un tratto attaccato
     * al testo si sposta con lui. Mentre si disegna le righe tornano
     * all'inizio, quindi i tratti nuovi si salvano dove il testo sta fermo.
     */
    var slidingItems: Set<Int> = emptySet()
    var sideOffset: () -> Float = { 0f }

    /** Quanto è alta la copertina, in px: sopra non si disegna. 0 senza copertina. */
    var coverHeightPx: Float = 0f

    /** Le altezze delle voci già viste, per chiave. */
    private val heights = HashMap<Any, Int>()

    /** Il tratto in corso, in px nelle coordinate della pagina (`p`: pressione). */
    val current = mutableStateListOf<InkPoint>()

    /** Dov'è la gomma mentre cancella, in px: per disegnarne il cerchio. */
    var eraserAt by mutableStateOf<Offset?>(null)

    /** L'ultima volta che si è sentita la S Pen, anche sospesa (ms dall'avvio): per il palmo. */
    var lastPenAt: Long = Long.MIN_VALUE / 2

    fun remember(info: LazyListLayoutInfo) {
        for (item in info.visibleItemsInfo) heights[item.key] = item.size
    }

    /** Dove comincia la voce `index`, in px dalla cima della pagina visibile; null se non si sa. */
    fun itemTop(info: LazyListLayoutInfo, index: Int): Float? {
        val visible = info.visibleItemsInfo
        if (visible.isEmpty()) return null
        visible.firstOrNull { it.index == index }?.let { return it.offset.toFloat() }
        val first = visible.first()
        val last = visible.last()
        return if (index < first.index) {
            var top = first.offset.toFloat()
            for (i in index until first.index) top -= heightOf(i) ?: return null
            top
        } else {
            var top = (last.offset + last.size).toFloat()
            for (i in last.index + 1 until index) top += heightOf(i) ?: return null
            top
        }
    }

    private fun heightOf(index: Int): Int? = itemKeys.getOrNull(index)?.let { heights[it] }

    /** Da dove in giù si disegna: sotto la copertina (e dalla cima dello schermo, se è uscita). */
    fun boundary(info: LazyListLayoutInfo): Float {
        val headerTop = itemTop(info, 0) ?: return 0f
        return max(0f, headerTop + coverHeightPx)
    }

    /** La voce sotto il punto `y`: quella a cui si attacca un tratto cominciato lì. */
    fun itemAt(info: LazyListLayoutInfo, y: Float): Pair<Int, Float>? {
        val visible = info.visibleItemsInfo
        if (visible.isEmpty()) return null
        val hit = visible.firstOrNull { y >= it.offset && y < it.offset + it.size }
            ?: if (y < visible.first().offset) visible.first() else visible.last()
        return hit.index to hit.offset.toFloat()
    }
}

/** I percorsi già costruiti di ogni tratto, in px: si rifanno solo se cambia la densità. */
class InkPathCache {
    private val paths = HashMap<String, Path>()
    private var density = 0f

    fun pathOf(stroke: InkStroke, density: Float): Path {
        if (density != this.density) {
            paths.clear()
            this.density = density
        }
        return paths.getOrPut(stroke.id) {
            buildInkPath(stroke.entity.tool, stroke.entity.width, stroke.points, density, 0f, 0f)
        }
    }
}

/**
 * Il percorso di un tratto, in px. `points` in dp (moltiplicati per
 * `scale`) spostati di (dx, dy) px; per il tratto in corso i punti sono
 * già in px, e `scale` è 1.
 *
 * Penna e matita sono **una forma piena** — un cerchio per ogni punto e
 * un trapezio fra due punti, larghi quanto la pressione — così la S Pen
 * premuta fa il tratto più spesso. Pennarello ed evidenziatore sono una
 * linea sola della stessa larghezza, smussata fra un punto e l'altro.
 */
fun buildInkPath(tool: InkTool, width: Float, points: List<InkPoint>, scale: Float, dx: Float, dy: Float, widthScale: Float = scale): Path {
    val path = Path()
    if (points.isEmpty()) return path
    fun x(p: InkPoint) = p.x * scale + dx
    fun y(p: InkPoint) = p.y * scale + dy
    when (tool) {
        InkTool.PEN, InkTool.PENCIL -> {
            val base = width * widthScale / 2f
            val radii = points.map { base * pressureFactor(it.p) }
            for (i in points.indices) addCircle(path, x(points[i]), y(points[i]), radii[i])
            for (i in 1 until points.size) {
                addSegment(path, x(points[i - 1]), y(points[i - 1]), radii[i - 1], x(points[i]), y(points[i]), radii[i])
            }
        }
        InkTool.MARKER, InkTool.HIGHLIGHTER -> {
            path.moveTo(x(points[0]), y(points[0]))
            if (points.size == 1) {
                // Un tocco solo: un puntino, che una linea lunga zero non disegna.
                path.lineTo(x(points[0]) + 0.01f, y(points[0]))
            } else {
                for (i in 1 until points.size) {
                    val a = points[i - 1]
                    val b = points[i]
                    path.quadraticBezierTo(x(a), y(a), (x(a) + x(b)) / 2f, (y(a) + y(b)) / 2f)
                }
                path.lineTo(x(points.last()), y(points.last()))
            }
        }
    }
    return path
}

/**
 * Un cerchio fatto di lati, girato **nello stesso verso dei trapezi**: le
 * forme di un tratto si sommano sempre, e dove si sovrappongono non
 * restano buchi.
 */
private fun addCircle(path: Path, cx: Float, cy: Float, r: Float) {
    val sides = (r * 0.8f).toInt().coerceIn(8, 28)
    for (k in 0 until sides) {
        val angle = 2.0 * Math.PI * k / sides
        val px = cx + r * cos(angle).toFloat()
        val py = cy + r * sin(angle).toFloat()
        if (k == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
}

private fun addSegment(path: Path, ax: Float, ay: Float, ar: Float, bx: Float, by: Float, br: Float) {
    val length = hypot(bx - ax, by - ay)
    if (length < 0.01f) return
    val nx = -(by - ay) / length
    val ny = (bx - ax) / length
    val xs = floatArrayOf(ax + nx * ar, bx + nx * br, bx - nx * br, ax - nx * ar)
    val ys = floatArrayOf(ay + ny * ar, by + ny * br, by - ny * br, ay - ny * ar)
    var area = 0f
    for (i in 0 until 4) {
        val j = (i + 1) % 4
        area += xs[i] * ys[j] - xs[j] * ys[i]
    }
    val order = if (area >= 0) intArrayOf(0, 1, 2, 3) else intArrayOf(3, 2, 1, 0)
    path.moveTo(xs[order[0]], ys[order[0]])
    for (k in 1 until 4) path.lineTo(xs[order[k]], ys[order[k]])
    path.close()
}

/** Il colore vero di un tratto: "il colore del testo" segue il tema. */
fun inkColor(color: Long, textColor: Color): Color =
    if (color == InkStrokeEntity.AUTO_COLOR) textColor else Color(color)

/** Disegna un percorso col suo strumento: la matita è un po' trasparente, l'evidenziatore molto. */
private fun DrawScope.drawInk(path: Path, tool: InkTool, width: Float, color: Color) {
    when (tool) {
        InkTool.PEN -> drawPath(path, color)
        InkTool.PENCIL -> drawPath(path, color.copy(alpha = color.alpha * PENCIL_ALPHA))
        InkTool.MARKER -> drawPath(
            path, color,
            style = Stroke(width = width * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        InkTool.HIGHLIGHTER -> drawPath(
            path, color.copy(alpha = HIGHLIGHTER_ALPHA),
            style = Stroke(width = width * density, cap = StrokeCap.Square, join = StrokeJoin.Round)
        )
    }
}

private const val PENCIL_ALPHA = 0.8f
private const val HIGHLIGHTER_ALPHA = 0.38f

/**
 * **Lo strato dei disegni**, sopra la pagina: i tratti salvati (prima gli
 * evidenziatori, che stanno sotto gli altri), il tratto in corso e il
 * cerchio della gomma. Mai sopra la copertina. [originX]: dove comincia la
 * pagina da sinistra (sul PC è spostata, vedi `PAGE_INSET`): i tratti la seguono.
 */
fun Modifier.inkLayer(
    state: InkPageState,
    strokes: List<InkStroke>,
    cache: InkPathCache,
    textColor: Color,
    originX: Dp = 0.dp
): Modifier = drawWithContent {
    drawContent()
    val info = state.listState.layoutInfo
    state.remember(info)
    val boundary = state.boundary(info)
    clipRect(top = boundary) {
        for (pass in 0..1) {
            for (stroke in strokes) {
                val highlighter = stroke.entity.tool == InkTool.HIGHLIGHTER
                if ((pass == 0) != highlighter) continue
                val index = resolveAnchor(stroke.entity.anchor, state.itemOfBlock) ?: continue
                val top = state.itemTop(info, index) ?: continue
                if (top + stroke.bounds.bottom * density < boundary || top + stroke.bounds.top * density > size.height) continue
                val left = originX.toPx() + if (index in state.slidingItems) -state.sideOffset() else 0f
                translate(left = left, top = top) {
                    drawInk(cache.pathOf(stroke, density), stroke.entity.tool, stroke.entity.width, inkColor(stroke.entity.color, textColor))
                }
            }
        }
        val current = state.current.toList()
        val pen = InkPrefs.settings.pen
        if (current.isNotEmpty() && pen != null) {
            val path = buildInkPath(pen.tool, pen.width, current, 1f, 0f, 0f, widthScale = density)
            drawInk(path, pen.tool, pen.width, inkColor(pen.color, textColor))
        }
        state.eraserAt?.let { at ->
            val radius = InkPrefs.settings.eraserRadius * density
            drawCircle(Color.White.copy(alpha = 0.15f), radius, at)
            drawCircle(NotionGray400, radius, at, style = Stroke(width = 1.5f * density))
        }
    }
}

/**
 * **Chi disegna, in modalità disegno.** Guarda ogni tocco **prima** della
 * pagina (`PointerEventPass.Initial`) e se lo prende tutto: mentre si
 * disegna la pagina non riceve niente.
 *
 *  - la **S Pen** disegna sempre; col **tasto laterale** premuto cancella,
 *    come su Samsung Notes;
 *  - **un dito** disegna, se la mano nella barra è accesa (lo è di
 *    partenza); spenta, un dito non fa niente;
 *  - **due dita** scorrono la pagina, sempre: è l'unico modo di scorrere
 *    mentre si disegna, come ha chiesto l'utente il 25/09/2026.
 *
 * **Il palmo.** Mentre la S Pen è vicina allo schermo (Android la sente
 * già a un paio di centimetri) o lo tocca, e per un attimo dopo, i tocchi
 * della mano non contano (`PALM_WINDOW_MS`). E se la mano è arrivata per
 * prima e ha cominciato un segno, appena la penna tocca quel segno si
 * butta e disegna la penna. Prima, invece, la prima S Pen spegneva il dito
 * per sempre, e l'utente non capiva perché il dito non disegnasse più.
 *
 * Sopra la copertina non si comincia a disegnare.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.inkInput(
    state: InkPageState,
    /** Dove comincia la pagina da sinistra: i tratti si salvano rispetto a lei (vedi `inkLayer`). */
    originX: Dp = 0.dp,
    onStroke: (anchor: String, pen: InkPen, points: List<InkPoint>) -> Unit,
    onErase: (xPx: Float, yPx: Float) -> Unit,
    onEraseEnd: () -> Unit
): Modifier = this
    // Dov'è la S Pen, anche solo sospesa sopra lo schermo: per il palmo.
    .pointerInput(state) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.firstOrNull { it.type.isPen() }?.let { state.lastPenAt = it.uptimeMillis }
            }
        }
    }
    .pointerInput(state) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            down.consume()
            if (!down.type.isPen() && down.uptimeMillis - state.lastPenAt < PALM_WINDOW_MS) {
                // Il palmo, con la penna vicina: non disegna e non scorre.
                swallowRest()
                return@awaitEachGesture
            }
            val settings = InkPrefs.settings
            val pen = settings.pen
            val boundary = state.boundary(state.listState.layoutInfo)
            val origin = originX.toPx()

            var trackedId = down.id
            var stylus = down.type.isPen()
            var erasing = false
            var drawing = false

            fun start(change: PointerInputChange, buttonPressed: Boolean) {
                drawing = (stylus || settings.fingerDraws) && change.position.y >= boundary
                erasing = settings.eraserSelected || pen == null ||
                    change.type == PointerType.Eraser ||
                    (stylus && buttonPressed)
            }

            fun pressureOf(change: PointerInputChange) =
                if (stylus) change.pressure.coerceIn(0f, 1f) else NEUTRAL_PRESSURE

            fun take(position: Offset, pressure: Float) {
                if (!drawing) return
                if (erasing) {
                    state.eraserAt = position
                    onErase(position.x - origin, position.y)
                } else {
                    val last = state.current.lastOrNull()
                    if (last == null || hypot(position.x - last.x, position.y - last.y) >= 1f) {
                        state.current += InkPoint(position.x, position.y, pressure)
                    }
                }
            }

            fun dropCurrent() {
                state.current.clear()
                if (erasing && drawing) onEraseEnd()
                state.eraserAt = null
            }

            start(down, currentEvent.buttons.isPrimaryPressed)
            take(down.position, pressureOf(down))
            var scrolled = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                // La penna arriva mentre un dito disegna: il dito era il palmo.
                val penDown = if (stylus) null else event.changes.firstOrNull { it.changedToDown() && it.type.isPen() }
                if (penDown != null) {
                    dropCurrent()
                    trackedId = penDown.id
                    stylus = true
                    start(penDown, event.buttons.isPrimaryPressed)
                    take(penDown.position, pressureOf(penDown))
                    event.changes.forEach { it.consume() }
                    continue
                }
                if (!stylus && event.changes.count { it.pressed } >= 2) {
                    // Due dita: non è un tratto, è scorrere.
                    dropCurrent()
                    scrolled = true
                    panWithTwoFingers(state, event)
                    break
                }
                val change = event.changes.firstOrNull { it.id == trackedId } ?: break
                val pressure = pressureOf(change)
                change.historical.forEach { take(it.position, pressure) }
                take(change.position, pressure)
                event.changes.forEach { it.consume() }
                if (!change.pressed) break
            }

            if (scrolled || !drawing) {
                state.current.clear()
            } else if (erasing) {
                state.eraserAt = null
                onEraseEnd()
            } else if (state.current.isNotEmpty() && pen != null) {
                val info = state.listState.layoutInfo
                val points = state.current.toList()
                val anchorItem = state.itemAt(info, points.first().y)
                val anchor = anchorItem?.let { state.anchorOfItem.getOrNull(it.first) }
                if (anchorItem != null && anchor != null) {
                    val top = anchorItem.second
                    onStroke(anchor, pen, points.map { InkPoint((it.x - origin) / density, (it.y - top) / density, it.p) })
                }
                state.current.clear()
            } else {
                state.current.clear()
            }
        }
    }

private fun PointerType.isPen() = this == PointerType.Stylus || this == PointerType.Eraser

/** Per quanto la mano non conta dopo che la S Pen si è allontanata. */
private const val PALM_WINDOW_MS = 600L

/** Si prende il resto di un tocco che non deve fare niente, finché non si stacca. */
private suspend fun AwaitPointerEventScope.swallowRest() {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        event.changes.forEach { it.consume() }
        if (event.changes.none { it.pressed }) return
    }
}

private suspend fun AwaitPointerEventScope.panWithTwoFingers(state: InkPageState, first: PointerEvent) {
    var event = first
    while (true) {
        val moving = event.changes.filter { it.pressed && it.previousPressed }
        if (moving.isNotEmpty()) {
            val dy = moving.map { it.position.y - it.previousPosition.y }.average().toFloat()
            state.listState.dispatchRawDelta(-dy)
        }
        event.changes.forEach { it.consume() }
        if (event.changes.none { it.pressed }) return
        event = awaitPointerEvent(PointerEventPass.Initial)
    }
}

// --- La barra degli strumenti ---

/**
 * **La barra del disegno**, in fondo alla pagina al posto di quella del
 * testo: Fine, Annulla, Ripeti e la mano per disegnare col dito, poi la
 * gomma e le penne come nella foto di OneNote. Toccando lo strumento già in
 * mano (o tenendolo premuto) si aprono spessore, tipo e colori.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InkToolbar(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDone: () -> Unit
) {
    val settings = InkPrefs.settings
    var menuFor by remember { mutableStateOf<Int?>(null) }
    // La penna di cui si sta scegliendo il colore, col selettore del pennello.
    var colorFor by remember { mutableStateOf<Int?>(null) }
    colorFor?.let { slot -> InkColorPicker(slot, onDismiss = { colorFor = null }) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurfaceVariant)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onDone) {
            Icon(Icons.Filled.Check, contentDescription = InkStrings.done, tint = AccentBlue)
        }
        IconButton(onClick = onUndo, enabled = canUndo) {
            Icon(Icons.Filled.Undo, contentDescription = InkStrings.undo, tint = if (canUndo) NotionWhite else NotionGray400)
        }
        IconButton(onClick = onRedo, enabled = canRedo) {
            Icon(Icons.Filled.Redo, contentDescription = InkStrings.redo, tint = if (canRedo) NotionWhite else NotionGray400)
        }
        // La mano sta qui, sempre in vista: in fondo alla barra finiva
        // fuori dallo schermo, e non la si trovava (25/09/2026).
        val finger = settings.fingerDraws
        IconButton(onClick = { InkPrefs.update { it.copy(fingerDraws = !it.fingerDraws) } }) {
            Icon(
                Icons.Filled.TouchApp,
                contentDescription = InkStrings.fingerDraws,
                tint = if (finger) AccentBlue else NotionGray400
            )
        }
        Spacer(Modifier.width(4.dp))

        // -1 la gomma, poi le penne.
        for (slot in -1 until settings.pens.size) {
            val selected = settings.selected == slot
            Box {
                ToolSlot(
                    selected = selected,
                    description = if (slot < 0) InkStrings.eraser else InkStrings.toolName(settings.pens[slot].tool),
                    onClick = {
                        if (selected) menuFor = slot else InkPrefs.update { it.copy(selected = slot) }
                    },
                    onLongClick = {
                        InkPrefs.update { it.copy(selected = slot) }
                        menuFor = slot
                    }
                ) {
                    if (slot < 0) EraserGlyph() else PenGlyph(settings.pens[slot])
                }
                DropdownMenu(
                    expanded = menuFor == slot,
                    onDismissRequest = { menuFor = null },
                    modifier = Modifier.background(DarkSheet)
                ) {
                    if (slot < 0) {
                        EraserMenu()
                    } else {
                        PenMenu(slot, onOpenColors = {
                            menuFor = null
                            colorFor = slot
                        })
                    }
                }
            }
        }

    }
}

/**
 * **Il colore di una penna, col selettore del pennello** (chiesto
 * dall'utente il 25/09/2026: "la stessa palette colori e spettro dei
 * grigi"): il riquadro di tinta e saturazione, la striscia dal bianco al
 * nero, Rosso/Verde/Blu ed Hex. "Colore del testo" rimette quello che
 * segue il tema.
 */
@Composable
private fun InkColorPicker(index: Int, onDismiss: () -> Unit) {
    val pen = InkPrefs.settings.pens.getOrNull(index) ?: return
    ColorPickerSheet(
        initialHex = inkHex(pen.color, NotionWhite),
        background = false,
        onDismiss = onDismiss,
        onSwitchTarget = {},
        onPick = { hex ->
            val color = hex?.let(::hexToInkColor) ?: InkStrokeEntity.AUTO_COLOR
            InkPrefs.updatePen(index) { it.copy(color = color) }
            onDismiss()
        },
        showTargetSwitch = false,
        previewLabel = InkStrings.toolName(pen.tool),
        noColorLabel = InkStrings.textColor
    )
}

/** Il colore di una penna scritto come nel selettore: "#RRGGBB". */
fun inkHex(color: Long, textColor: Color): String {
    val argb = inkColor(color, textColor).toArgb()
    return "#%06X".format(argb and 0xFFFFFF)
}

/** "#RRGGBB" → il colore da salvare, pieno. Null se non è un colore. */
fun hexToInkColor(hex: String): Long? {
    val digits = hex.trim().removePrefix("#")
    if (digits.length != 6) return null
    val rgb = digits.toLongOrNull(16) ?: return null
    return 0xFF000000L or rgb
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolSlot(
    selected: Boolean,
    description: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    glyph: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(width = 40.dp, height = 52.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
            .onRightClick(onLongClick)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, onClickLabel = description),
        contentAlignment = Alignment.Center
    ) {
        glyph()
        if (selected) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = NotionWhite,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(14.dp)
            )
        }
    }
}

/** La punta di una penna, col suo colore, come nella barra di OneNote. */
@Composable
private fun PenGlyph(pen: InkPen) {
    val ink = inkColor(pen.color, NotionWhite)
    val body = Color(0xFFE6E6E6)
    Canvas(modifier = Modifier.size(width = 22.dp, height = 40.dp)) {
        val w = size.width
        val h = size.height
        when (pen.tool) {
            InkTool.HIGHLIGHTER -> {
                // Punta a scalpello, larga.
                drawRect(body, topLeft = Offset(w * 0.15f, h * 0.45f), size = Size(w * 0.7f, h * 0.55f))
                drawRect(ink, topLeft = Offset(w * 0.15f, h * 0.62f), size = Size(w * 0.7f, h * 0.1f))
                val tip = Path().apply {
                    moveTo(w * 0.25f, h * 0.45f); lineTo(w * 0.75f, h * 0.45f)
                    lineTo(w * 0.75f, h * 0.12f); lineTo(w * 0.25f, h * 0.24f); close()
                }
                drawPath(tip, ink)
            }
            InkTool.MARKER -> {
                drawRect(body, topLeft = Offset(w * 0.18f, h * 0.4f), size = Size(w * 0.64f, h * 0.6f))
                drawRect(ink, topLeft = Offset(w * 0.18f, h * 0.58f), size = Size(w * 0.64f, h * 0.1f))
                val cone = Path().apply {
                    moveTo(w * 0.18f, h * 0.4f); lineTo(w * 0.82f, h * 0.4f)
                    lineTo(w * 0.64f, h * 0.18f); lineTo(w * 0.36f, h * 0.18f); close()
                }
                drawPath(cone, body)
                drawRect(ink, topLeft = Offset(w * 0.36f, h * 0.06f), size = Size(w * 0.28f, h * 0.12f))
            }
            InkTool.PEN, InkTool.PENCIL -> {
                val coneColor = if (pen.tool == InkTool.PENCIL) Color(0xFFE8C9A0) else body
                drawRect(body, topLeft = Offset(w * 0.22f, h * 0.42f), size = Size(w * 0.56f, h * 0.58f))
                drawRect(ink, topLeft = Offset(w * 0.22f, h * 0.6f), size = Size(w * 0.56f, h * 0.08f))
                val cone = Path().apply {
                    moveTo(w * 0.22f, h * 0.42f); lineTo(w * 0.78f, h * 0.42f)
                    lineTo(w * 0.5f, h * 0.05f); close()
                }
                drawPath(cone, coneColor)
                val tip = Path().apply {
                    moveTo(w * 0.4f, h * 0.18f); lineTo(w * 0.6f, h * 0.18f)
                    lineTo(w * 0.5f, h * 0.05f); close()
                }
                drawPath(tip, ink)
            }
        }
    }
}

/** La gomma: bianca con la fascia viola, come quella della foto. */
@Composable
private fun EraserGlyph() {
    Canvas(modifier = Modifier.size(width = 22.dp, height = 40.dp)) {
        val w = size.width
        val h = size.height
        drawRect(Color(0xFFE6E6E6), topLeft = Offset(w * 0.15f, h * 0.08f), size = Size(w * 0.7f, h * 0.62f))
        drawRect(Color(0xFFB04FD6), topLeft = Offset(w * 0.15f, h * 0.7f), size = Size(w * 0.7f, h * 0.22f))
    }
}

@Composable
private fun MenuTitle(text: String) {
    Text(
        text,
        color = NotionWhite,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 6.dp)
    )
}

/** "Thickness": "−", i cinque spessori (quello in uso cerchiato), "+". */
@Composable
private fun ThicknessRow(presets: List<Float>, value: Float, onChange: (Float) -> Unit) {
    val current = nearestPreset(presets, value)
    Row(
        modifier = Modifier.padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onChange(thinner(value)) }, modifier = Modifier.size(36.dp)) {
            Text("—", color = NotionWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        presets.forEachIndexed { index, preset ->
            val dot = (4 + index * 3).dp
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .then(if (index == current) Modifier.border(1.5.dp, NotionWhite, CircleShape) else Modifier)
                    .clickable { onChange(preset) },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(dot).clip(CircleShape).background(NotionWhite))
            }
        }
        IconButton(onClick = { onChange(thicker(value)) }, modifier = Modifier.size(36.dp)) {
            Text("+", color = NotionWhite, fontSize = 20.sp)
        }
    }
}

@Composable
private fun EraserMenu() {
    val settings = InkPrefs.settings
    Column(modifier = Modifier.width(260.dp).padding(bottom = 6.dp)) {
        MenuTitle(InkStrings.thickness)
        ThicknessRow(ERASER_SIZES, settings.eraserRadius) { r -> InkPrefs.update { it.copy(eraserRadius = r) } }
        MenuTitle(InkStrings.types)
        for (mode in EraserMode.entries) {
            val chosen = settings.eraserMode == mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { InkPrefs.update { it.copy(eraserMode = mode) } }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .border(1.dp, if (chosen) AccentBlue else NotionGray400, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Il tratto intero, o solo un pezzetto.
                    Canvas(Modifier.size(14.dp)) {
                        val y = size.height / 2
                        if (mode == EraserMode.STROKE) {
                            drawLine(Color(0xFFB04FD6), Offset(0f, y), Offset(size.width, y), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                        } else {
                            drawCircle(Color(0xFFB04FD6), radius = 3.dp.toPx(), center = Offset(size.width / 2, y))
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    if (mode == EraserMode.STROKE) InkStrings.strokeEraser else InkStrings.pointEraser,
                    color = NotionWhite,
                    modifier = Modifier.weight(1f)
                )
                if (chosen) Icon(Icons.Filled.Check, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun PenMenu(index: Int, onOpenColors: () -> Unit) {
    val pen = InkPrefs.settings.pens.getOrNull(index) ?: return
    Column(modifier = Modifier.width(280.dp).padding(bottom = 8.dp)) {
        MenuTitle(InkStrings.thickness)
        ThicknessRow(thicknessPresets(pen.tool), pen.width) { w -> InkPrefs.updatePen(index) { it.copy(width = w) } }

        MenuTitle(InkStrings.types)
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (tool in InkTool.entries) {
                val chosen = pen.tool == tool
                Text(
                    InkStrings.toolName(tool),
                    color = if (chosen) Color.White else NotionWhite,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (chosen) AccentBlue else Color.White.copy(alpha = 0.08f))
                        .clickable {
                            // Cambiando tipo, lo spessore va fra quelli del tipo nuovo.
                            InkPrefs.updatePen(index) {
                                val presets = thicknessPresets(tool)
                                it.copy(tool = tool, width = presets[nearestPreset(thicknessPresets(it.tool), it.width)])
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }

        MenuTitle(InkStrings.colors)
        // Il colore in uso; toccandolo si apre il selettore del pennello.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenColors)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pen.color == InkStrokeEntity.AUTO_COLOR) {
                // "Il colore del testo": mezzo bianco e mezzo nero.
                Canvas(Modifier.size(32.dp)) {
                    drawCircle(Color.Black)
                    drawArc(Color.White, -90f, 180f, useCenter = true)
                    drawCircle(NotionGray400, style = Stroke(1.dp.toPx()))
                }
            } else {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(pen.color))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                if (pen.color == InkStrokeEntity.AUTO_COLOR) InkStrings.textColor else inkHex(pen.color, NotionWhite),
                color = NotionWhite,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = InkStrings.colors, tint = NotionGray400)
        }
    }
}
