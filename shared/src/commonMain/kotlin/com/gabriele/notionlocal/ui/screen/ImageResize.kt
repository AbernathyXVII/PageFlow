package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.ui.i18n.BlockStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import kotlin.math.abs

/** La forma di un'immagine di cui il file non dice le misure: 16:9. */
const val DEFAULT_IMAGE_ASPECT = 0.5625f

/** Più piccolo di così un lato del riquadro non va, con "Full resize". */
val MIN_FRAME_SIDE = 48.dp

/**
 * **Il riquadro di un'immagine nella pagina** (dal 20/10/2026), in pixel: da
 * dove comincia da sinistra, quanto è largo e quanto è alto. L'immagine lo
 * riempie.
 */
data class ImageFrame(val left: Float, val width: Float, val height: Float)

/** Come ridimensionare un'immagine, scelto dal suo menu: le maniglie non si vedono finché non lo si chiede. */
enum class ImageResizeMode { NONE, RESIZE, FULL }

/**
 * Il riquadro com'è salvato nel blocco, su una pagina larga `pageWidth`; `natural` è l'altezza
 * dell'immagine diviso la larghezza, `naturalWidth` quanto è larga l'immagine vera, in pixel dello
 * schermo (null se non si sa).
 *
 * **Senza una misura salvata l'immagine è larga quanto è davvero** (dal 13/11/2026, chiesto
 * dall'utente: "se è un 512x512 non deve essere riempita tutta la pagina"), al massimo quanto la
 * pagina. Prima riempiva sempre la pagina.
 */
fun frameOf(attrs: BlockAttrs, pageWidth: Float, natural: Float, naturalWidth: Float? = null): ImageFrame {
    val width = attrs.width?.let { it.coerceIn(0.01f, 1f) * pageWidth } ?: minOf(naturalWidth ?: pageWidth, pageWidth)
    val left = attrs.left?.let { (it * pageWidth).coerceIn(0f, pageWidth - width) } ?: ((pageWidth - width) / 2f)
    return ImageFrame(left, width, width * (attrs.aspect ?: natural))
}

/**
 * **"Resize"**: le due maniglie ai lati. La larghezza cambia di due volte lo
 * spostamento del dito (l'immagine tiene il suo centro, come prima) e
 * l'altezza la segue, con la forma che ha. `side` -1 sinistra, 1 destra.
 */
fun ImageFrame.widened(side: Int, dx: Float, pageWidth: Float, minWidth: Float): ImageFrame {
    val center = left + width / 2f
    val newWidth = (width + 2f * dx * side).coerceIn(minWidth.coerceAtMost(pageWidth), pageWidth)
    val newLeft = (center - newWidth / 2f).coerceIn(0f, pageWidth - newWidth)
    return ImageFrame(newLeft, newWidth, newWidth * (height / width))
}

/**
 * **"Full resize"**, come una finestra di Windows: si muove solo il bordo
 * afferrato. `edgeX` -1 il bordo sinistro, 1 il destro, 0 nessuno; `edgeY` -1
 * quello in alto, 1 quello in basso, 0 nessuno (gli angoli ne muovono due).
 * L'immagine non esce dalla pagina e non diventa più piccola di `min`.
 */
fun ImageFrame.dragged(edgeX: Int, edgeY: Int, dx: Float, dy: Float, pageWidth: Float, min: Float): ImageFrame {
    var newLeft = left
    var newWidth = width
    var newHeight = height
    when (edgeX) {
        1 -> newWidth = (width + dx).coerceIn(min, pageWidth - left)
        -1 -> {
            newLeft = (left + dx).coerceIn(0f, left + width - min)
            newWidth = width - (newLeft - left)
        }
    }
    when (edgeY) {
        1 -> newHeight = (height + dy).coerceAtLeast(min)
        -1 -> newHeight = (height - dy).coerceAtLeast(min)
    }
    return ImageFrame(newLeft, newWidth, newHeight)
}

/**
 * Le voci da salvare per un riquadro: la larghezza in frazione della pagina
 * (a tutta pagina non serve scriverla), dove comincia (al centro non serve) e,
 * dopo "Full resize", la forma. "Resize" la forma la lascia com'era.
 */
fun BlockAttrs.withFrame(frame: ImageFrame, pageWidth: Float, full: Boolean): BlockAttrs {
    val width = (frame.width / pageWidth).coerceIn(0.01f, 1f)
    val centered = abs(frame.left - (pageWidth - frame.width) / 2f) < 1f
    return copy(
        // Anche larga tutta la pagina si ricorda: senza misura sarebbe larga quanto l'immagine vera.
        width = width,
        left = if (centered) null else frame.left / pageWidth,
        aspect = if (full) frame.height / frame.width else aspect
    )
}

/** "Original size": la sua misura vera (al massimo la pagina), al centro, con la sua forma. */
fun BlockAttrs.withOriginalSize(): BlockAttrs = copy(width = null, left = null, aspect = null)

/** Com'è fatta una maniglia: barretta in piedi (lati), coricata (sopra e sotto) o quadratino (angoli). */
enum class HandleShape { VERTICAL, HORIZONTAL, CORNER }

/** Una maniglia di "Full resize": dove sta sul riquadro e quali bordi muove. */
class FrameHandle(val alignment: Alignment, val shape: HandleShape, val edgeX: Int, val edgeY: Int)

val FULL_HANDLES = listOf(
    FrameHandle(Alignment.CenterStart, HandleShape.VERTICAL, -1, 0),
    FrameHandle(Alignment.CenterEnd, HandleShape.VERTICAL, 1, 0),
    FrameHandle(Alignment.TopCenter, HandleShape.HORIZONTAL, 0, -1),
    FrameHandle(Alignment.BottomCenter, HandleShape.HORIZONTAL, 0, 1),
    FrameHandle(Alignment.TopStart, HandleShape.CORNER, -1, -1),
    FrameHandle(Alignment.TopEnd, HandleShape.CORNER, 1, -1),
    FrameHandle(Alignment.BottomStart, HandleShape.CORNER, -1, 1),
    FrameHandle(Alignment.BottomEnd, HandleShape.CORNER, 1, 1)
)

/**
 * Una maniglia: scura col bordo bianco, che si vede su qualsiasi immagine, in
 * un'area da toccare più grande di lei. `onDrag` riceve lo spostamento del
 * dito, `onDragEnd` quando lo si alza (lì si salva).
 */
@Composable
fun ResizeHandle(shape: HandleShape, modifier: Modifier, onDrag: (Offset) -> Unit, onDragEnd: () -> Unit) {
    val latestDrag by rememberUpdatedState(onDrag)
    val latestEnd by rememberUpdatedState(onDragEnd)
    val touch = when (shape) {
        HandleShape.VERTICAL -> Modifier.size(28.dp, 64.dp)
        HandleShape.HORIZONTAL -> Modifier.size(64.dp, 28.dp)
        HandleShape.CORNER -> Modifier.size(36.dp)
    }
    val mark = when (shape) {
        HandleShape.VERTICAL -> Modifier.size(5.dp, 40.dp)
        HandleShape.HORIZONTAL -> Modifier.size(40.dp, 5.dp)
        HandleShape.CORNER -> Modifier.size(14.dp)
    }
    Box(
        modifier = modifier
            .then(touch)
            .semantics { contentDescription = BlockStrings.resizeImage }
            .pointerInput(Unit) {
                detectDragGestures(onDragEnd = { latestEnd() }, onDragCancel = { latestEnd() }) { change, drag ->
                    change.consume()
                    latestDrag(drag)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = mark
                .clip(RoundedCornerShape(3.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(3.dp))
        )
    }
}

/** Sopra l'immagine mentre la si ridimensiona: tornare alla sua misura, e finire. */
@Composable
fun ResizeToolbar(onOriginalSize: () -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ToolbarPill(Icons.Filled.RestartAlt, BlockStrings.originalSize, onOriginalSize)
        ToolbarPill(Icons.Filled.Check, Strings.done, onDone)
    }
}

@Composable
private fun ToolbarPill(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.size(6.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}
