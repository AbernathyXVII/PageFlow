package com.gabriele.notionlocal.ui.screen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.os.Build
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gabriele.notionlocal.ui.i18n.BlockStrings
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Quante righe del file per striscia, nella pagina. */
private const val STRIP_ROWS = 1024

/** Il lato di un pezzo, a tutto schermo, in pixel del pezzo letto. */
private const val TILE_SIDE = 512

/** Quanti pezzi tenere in memoria a tutto schermo: in RGB_565 sono mezzo megabyte l'uno. */
private const val TILE_CACHE = 96

private fun openDecoder(file: File): BitmapRegionDecoder? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        BitmapRegionDecoder.newInstance(file.path)
    } else {
        @Suppress("DEPRECATION")
        BitmapRegionDecoder.newInstance(file.path, false)
    }
}.getOrNull()

private fun BitmapRegionDecoder.read(left: Int, top: Int, right: Int, bottom: Int, sample: Int): ImageBitmap? = runCatching {
    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.RGB_565
    }
    decodeRegion(android.graphics.Rect(left, top, right, bottom), options)?.asImageBitmap()
}.getOrNull()

/**
 * **Un'immagine alta nella pagina**: larga quanto il suo riquadro, alta
 * quanto vuole la sua forma, disegnata a strisce. Si leggono solo le strisce
 * sullo schermo e quelle subito sopra e sotto; le altre si lasciano andare.
 */
@Composable
actual fun StripImage(file: File, imageWidth: Int, imageHeight: Int, description: String?, modifier: Modifier) {
    val decoder = remember(file) { openDecoder(file) }
    DisposableEffect(decoder) { onDispose { decoder?.recycle() } }
    val windowHeight = LocalView.current.rootView.height.takeIf { it > 0 } ?: 3000
    val strips = remember(file) { mutableStateMapOf<Int, ImageBitmap>() }
    var widthPx by remember { mutableStateOf(0) }
    var visible by remember { mutableStateOf(IntRange.EMPTY) }
    val count = (imageHeight + STRIP_ROWS - 1) / STRIP_ROWS

    LaunchedEffect(decoder, visible, widthPx) {
        val reader = decoder ?: return@LaunchedEffect
        if (widthPx <= 0) return@LaunchedEffect
        strips.keys.filter { it !in visible }.forEach { strips.remove(it) }
        val sample = sampleFor(widthPx.toFloat() / imageWidth)
        for (i in visible) {
            if (i in strips) continue
            val top = i * STRIP_ROWS
            val bottom = min(imageHeight, top + STRIP_ROWS)
            val bitmap = withContext(Dispatchers.IO) { reader.read(0, top, imageWidth, bottom, sample) } ?: continue
            strips[i] = bitmap
        }
    }

    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val shownWidth = constraints.maxWidth
        // Alta quanto vuole la sua forma, o quanto il riquadro che le danno ("Full resize").
        val shownHeight = if (constraints.hasBoundedHeight) constraints.maxHeight.toFloat() else imageHeight * shownWidth.toFloat() / imageWidth
        val scaleY = shownHeight / imageHeight
        val heightDp = with(density) { shownHeight.toDp() }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp)
                .background(Color.White)
                .semantics { description?.let { contentDescription = it } }
                .onSizeChanged { widthPx = it.width }
                .onGloballyPositioned { coordinates ->
                    // Quali strisce sono sullo schermo (e una di margine sopra e sotto).
                    val top = coordinates.positionInWindow().y
                    val stripHeight = STRIP_ROWS * scaleY
                    if (stripHeight <= 0f) return@onGloballyPositioned
                    val first = floor((-top) / stripHeight).toInt() - 1
                    val last = floor((-top + windowHeight) / stripHeight).toInt() + 1
                    val range = max(0, first)..min(count - 1, last)
                    if (range != visible) visible = range
                }
        ) {
            val s = size.height / imageHeight
            strips.forEach { (i, bitmap) ->
                val top = (i * STRIP_ROWS * s).roundToInt()
                val bottom = (min(imageHeight, (i + 1) * STRIP_ROWS) * s).roundToInt()
                drawImage(
                    bitmap,
                    dstOffset = IntOffset(0, top),
                    dstSize = IntSize(size.width.roundToInt(), bottom - top)
                )
            }
        }
    }
}

/**
 * **Un'immagine alta a tutto schermo**: si apre larga quanto lo schermo, in
 * cima; si scorre col dito (con lo slancio, come una pagina), si ingrandisce
 * con due dita fino a otto volte, con due tocchi si ingrandisce lì o si torna
 * com'era. Sotto c'è tutta l'immagine piccola, sopra i pezzi nitidi appena
 * letti. La X la chiude, accanto c'è lo scarica (`onDownload`).
 */
@Composable
actual fun TallImageViewer(
    file: File,
    imageWidth: Int,
    imageHeight: Int,
    title: String,
    onDismiss: () -> Unit,
    onDownload: (() -> Unit)?
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val decoder = remember(file) { openDecoder(file) }
        DisposableEffect(decoder) { onDispose { decoder?.recycle() } }
        var viewport by remember { mutableStateOf(IntSize.Zero) }
        // `zoom` 1 = larga quanto lo schermo; `offset` dove sta l'angolo in alto a sinistra dell'immagine.
        var zoom by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }
        val tiles = remember(file) { LinkedHashMap<Long, ImageBitmap>(TILE_CACHE, 0.75f, true) }
        var tilesVersion by remember { mutableStateOf(0) }
        var base by remember(file) { mutableStateOf<ImageBitmap?>(null) }

        fun shownPerImagePixel() = if (viewport.width <= 0) 0f else viewport.width * zoom / imageWidth
        fun clamp() {
            val s = shownPerImagePixel()
            val contentWidth = imageWidth * s
            val contentHeight = imageHeight * s
            offsetX = if (contentWidth <= viewport.width) (viewport.width - contentWidth) / 2f
            else offsetX.coerceIn(viewport.width - contentWidth, 0f)
            offsetY = if (contentHeight <= viewport.height) (viewport.height - contentHeight) / 2f
            else offsetY.coerceIn(viewport.height - contentHeight, 0f)
        }
        fun zoomAround(factor: Float, center: Offset) {
            val before = zoom
            zoom = (zoom * factor).coerceIn(1f, 8f)
            val ratio = zoom / before
            offsetX = center.x - (center.x - offsetX) * ratio
            offsetY = center.y - (center.y - offsetY) * ratio
            clamp()
        }

        // Tutta l'immagine piccola, per vedere subito qualcosa ovunque.
        LaunchedEffect(decoder, viewport.width) {
            val reader = decoder ?: return@LaunchedEffect
            if (viewport.width <= 0 || base != null) return@LaunchedEffect
            // Un quarto dello schermo: poca memoria, e basta finché arrivano i pezzi nitidi.
            val sample = sampleFor(viewport.width / 4f / imageWidth)
            base = withContext(Dispatchers.IO) { reader.read(0, 0, imageWidth, imageHeight, sample) }
        }
        // I pezzi nitidi che servono adesso: si leggono quelli che mancano.
        LaunchedEffect(decoder) {
            val reader = decoder ?: return@LaunchedEffect
            snapshotFlow { neededTiles(viewport, zoom, offsetX, offsetY, imageWidth, imageHeight) }
                .distinctUntilChanged()
                .collectLatest { needed ->
                    for (key in needed) {
                        if (tiles.containsKey(key)) continue
                        val (sample, tx, ty) = unpackTile(key)
                        val side = TILE_SIDE * sample
                        val left = tx * side
                        val top = ty * side
                        val bitmap = withContext(Dispatchers.IO) {
                            reader.read(left, top, min(imageWidth, left + side), min(imageHeight, top + side), sample)
                        } ?: continue
                        tiles[key] = bitmap
                        while (tiles.size > TILE_CACHE) tiles.remove(tiles.keys.first())
                        tilesVersion++
                    }
                }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onSizeChanged {
                    val first = viewport == IntSize.Zero
                    viewport = it
                    if (first) {
                        offsetX = 0f
                        offsetY = 0f
                    }
                    clamp()
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { at -> zoomAround(if (zoom > 1.01f) 1f / zoom else 2.5f, at) })
                }
                .pointerInput(Unit) {
                    val decay = splineBasedDecay<Float>(this)
                    coroutineScope {
                        var fling: Job? = null
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            fling?.cancel()
                            val tracker = VelocityTracker()
                            tracker.addPosition(down.uptimeMillis, down.position)
                            var pinched = false
                            do {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.count { it.pressed }
                                if (pressed > 1) pinched = true
                                val zoomChange = event.calculateZoom()
                                if (zoomChange != 1f) zoomAround(zoomChange, event.calculateCentroid())
                                val pan = event.calculatePan()
                                if (pan != Offset.Zero) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                    clamp()
                                }
                                event.changes.firstOrNull()?.let { tracker.addPosition(it.uptimeMillis, it.position) }
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            } while (event.changes.any { it.pressed })
                            if (!pinched) {
                                val velocity = tracker.calculateVelocity()
                                fling = launch {
                                    var lastX = 0f
                                    var lastY = 0f
                                    launch {
                                        AnimationState(0f, velocity.x).animateDecay(decay) {
                                            offsetX += value - lastX
                                            lastX = value
                                            clamp()
                                        }
                                    }
                                    AnimationState(0f, velocity.y).animateDecay(decay) {
                                        offsetY += value - lastY
                                        lastY = value
                                        clamp()
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize().semantics { contentDescription = title }) {
                // Letto qui perché il disegno si rifaccia quando arriva un pezzo nuovo.
                val version = tilesVersion
                val s = shownPerImagePixel()
                if (s <= 0f || version < 0) return@Canvas
                base?.let { b ->
                    drawImage(
                        b,
                        dstOffset = IntOffset(offsetX.roundToInt(), offsetY.roundToInt()),
                        dstSize = IntSize((imageWidth * s).roundToInt(), (imageHeight * s).roundToInt())
                    )
                }
                for (key in neededTiles(viewport, zoom, offsetX, offsetY, imageWidth, imageHeight)) {
                    val bitmap = tiles[key] ?: continue
                    val (sample, tx, ty) = unpackTile(key)
                    val side = TILE_SIDE * sample
                    val left = (offsetX + tx * side * s).roundToInt()
                    val top = (offsetY + ty * side * s).roundToInt()
                    val right = (offsetX + min(imageWidth, (tx + 1) * side) * s).roundToInt()
                    val bottom = (offsetY + min(imageHeight, (ty + 1) * side) * s).roundToInt()
                    drawImage(bitmap, dstOffset = IntOffset(left, top), dstSize = IntSize(right - left, bottom - top))
                }
            }
            // Su un tondo scuro: la foto di una pagina web è quasi tutta bianca.
            Row(modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)) {
                if (onDownload != null) {
                    IconButton(onClick = onDownload, modifier = Modifier.background(Color.Black.copy(alpha = 0.55f), CircleShape)) {
                        Icon(Icons.Filled.Download, contentDescription = BlockStrings.download, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                }
                IconButton(onClick = onDismiss, modifier = Modifier.background(Color.Black.copy(alpha = 0.55f), CircleShape)) {
                    Icon(Icons.Filled.Close, contentDescription = EditorStrings.close, tint = Color.White)
                }
            }
        }
    }
}

/** Un pezzo come numero solo: la riduzione, la colonna e la riga. */
private fun packTile(sample: Int, tx: Int, ty: Int): Long = (sample.toLong() shl 48) or (tx.toLong() shl 24) or ty.toLong()

private fun unpackTile(key: Long): Triple<Int, Int, Int> =
    Triple((key shr 48).toInt(), ((key shr 24) and 0xFFFFFF).toInt(), (key and 0xFFFFFF).toInt())

/** I pezzi che coprono lo schermo, alla riduzione giusta per l'ingrandimento di adesso. */
private fun neededTiles(viewport: IntSize, zoom: Float, offsetX: Float, offsetY: Float, imageWidth: Int, imageHeight: Int): List<Long> {
    if (viewport.width <= 0 || viewport.height <= 0) return emptyList()
    val s = viewport.width * zoom / imageWidth
    val sample = sampleFor(s)
    val side = TILE_SIDE * sample
    val x0 = max(0f, -offsetX / s)
    val y0 = max(0f, -offsetY / s)
    val x1 = min(imageWidth.toFloat(), x0 + viewport.width / s)
    val y1 = min(imageHeight.toFloat(), y0 + viewport.height / s)
    val out = ArrayList<Long>()
    for (ty in floor(y0 / side).toInt()..(ceil(y1 / side).toInt() - 1).coerceAtLeast(0)) {
        for (tx in floor(x0 / side).toInt()..(ceil(x1 / side).toInt() - 1).coerceAtLeast(0)) {
            out += packTile(sample, tx, ty)
        }
    }
    return out
}
