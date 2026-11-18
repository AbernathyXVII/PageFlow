package com.gabriele.notionlocal.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.platform.isPc
import kotlinx.coroutines.delay

/** Quanto resta la barra dopo l'ultimo movimento del mouse sul database, o dopo che è uscito. */
// Mezzo secondo dal 18/11/2026 (chiesto dall'utente; prima 2 secondi).
private const val HOVER_BAR_MS = 500L

/**
 * Dove sta la maniglia della barra larga [width]: (inizio, larghezza), con [value] dello
 * scorrimento su [max] e la parte che si vede larga [width]. Null se non c'è niente da scorrere.
 */
fun hoverBarThumb(width: Float, value: Int, max: Int): Pair<Float, Float>? {
    if (max <= 0 || width <= 0f) return null
    val thumb = (width * width / (width + max)).coerceAtLeast(minOf(width, 32f))
    return (width - thumb) * value / max to thumb
}

/**
 * **La barra per scorrere di lato un database, sul PC** (dal 18/11/2026, chiesto dall'utente):
 * quando la tabella (o la bacheca) è più larga di quanto si vede, col mouse sopra compare in fondo una
 * barra sottile, da trascinare; se ne va da sola mezzo secondo dopo che il mouse si è fermato o è uscito.
 * Disegnata sopra il database, senza spostare niente. Va messa prima di `horizontalScroll([scroll])`:
 * così misura la parte che si vede.
 */
fun Modifier.hoverScrollBar(scroll: ScrollState): Modifier = if (!isPc) this else composed {
    var pokes by remember { mutableIntStateOf(0) }
    var shown by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(pokes) {
        if (pokes == 0) return@LaunchedEffect
        shown = true
        delay(HOVER_BAR_MS)
        if (!dragging) shown = false
    }
    val alpha by animateFloatAsState(if (shown && scroll.maxValue > 0) 1f else 0f, tween(200))
    this
        .pointerInput(scroll) {
            val barHeight = 10.dp.toPx()
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull() ?: continue
                    when (event.type) {
                        PointerEventType.Enter, PointerEventType.Move, PointerEventType.Scroll, PointerEventType.Exit -> pokes++
                        PointerEventType.Press -> {
                            // Premuto sulla barra, mentre si vede: la si trascina.
                            val thumb = hoverBarThumb(size.width.toFloat(), scroll.value, scroll.maxValue)
                            val onBar = shown && thumb != null && change.position.y >= size.height - barHeight
                            if (!onBar) continue
                            change.consume()
                            dragging = true
                            var last = change.position.x
                            // Un clic fuori dalla maniglia la porta lì.
                            if (last !in thumb!!.first..(thumb.first + thumb.second)) {
                                scroll.dispatchRawDelta((last - thumb.first - thumb.second / 2f) * (scroll.maxValue / (size.width - thumb.second)))
                            }
                            while (true) {
                                val drag = awaitPointerEvent(PointerEventPass.Initial)
                                val moved = drag.changes.firstOrNull() ?: break
                                moved.consume()
                                if (drag.type == PointerEventType.Release || !moved.pressed) break
                                val now = hoverBarThumb(size.width.toFloat(), scroll.value, scroll.maxValue) ?: break
                                scroll.dispatchRawDelta((moved.position.x - last) * (scroll.maxValue / (size.width - now.second)))
                                last = moved.position.x
                                pokes++
                            }
                            dragging = false
                            pokes++
                        }
                        else -> {}
                    }
                }
            }
        }
        .drawWithContent {
            drawContent()
            if (alpha <= 0f) return@drawWithContent
            val (left, width) = hoverBarThumb(size.width, scroll.value, scroll.maxValue) ?: return@drawWithContent
            val thickness = 5.dp.toPx()
            drawRoundRect(
                color = Color.White.copy(alpha = (if (dragging) 0.5f else 0.32f) * alpha),
                topLeft = Offset(left, size.height - thickness - 2.dp.toPx()),
                size = Size(width, thickness),
                cornerRadius = CornerRadius(thickness / 2f)
            )
        }
}
