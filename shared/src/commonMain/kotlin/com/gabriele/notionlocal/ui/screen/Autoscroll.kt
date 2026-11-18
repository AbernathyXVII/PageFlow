package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sign

/** Attorno all'icona, quanto ci si può allontanare senza che la pagina si muova. */
private const val DEAD_ZONE_DP = 12f

/**
 * Quanti punti al secondo scorre la pagina col puntatore a [distance] punti sopra (meno) o sotto
 * l'icona: niente vicino all'icona, poi sempre più svelto, più che in proporzione (fino a 8000).
 */
fun autoscrollSpeed(distance: Float): Float {
    val past = abs(distance) - DEAD_ZONE_DP
    if (past <= 0f) return 0f
    return sign(distance) * (past * 6f + past * past * 0.04f).coerceAtMost(8000f)
}

/**
 * **Il tasto centrale del mouse, come nei browser** (dal 18/11/2026, chiesto dall'utente): un clic
 * col tasto centrale lascia lì un'icona, e la pagina scorre su o giù tanto più veloce quanto più il
 * mouse se ne allontana; vicino all'icona sta ferma. Si ferma con un altro clic, la rotella o un
 * tasto. Tenendo premuto il tasto centrale e muovendo il mouse, si ferma lasciandolo. Il clic
 * centrale non arriva al testo sotto (il cursore resta dov'era). [scrollBy] in pixel.
 */
fun Modifier.middleClickAutoscroll(enabled: Boolean, scrollBy: suspend (Float) -> Unit): Modifier =
    if (!enabled) this else composed {
        // Dove sta l'icona (null = spento) e dov'è il mouse, nelle coordinate di questo elemento.
        var anchor by remember { mutableStateOf<Offset?>(null) }
        var pointer by remember { mutableStateOf(Offset.Zero) }
        val density = LocalDensity.current.density
        LaunchedEffect(anchor) {
            val start = anchor ?: return@LaunchedEffect
            var last = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                val seconds = (now - last) / 1_000_000_000f
                last = now
                val speed = autoscrollSpeed((pointer.y - start.y) / density) * density
                if (speed != 0f) scrollBy(speed * seconds)
            }
        }
        this
            .onPreviewKeyEvent { event ->
                if (anchor == null || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                anchor = null
                // Esc serve solo a fermarlo; gli altri tasti fanno anche quello che fanno sempre.
                event.key == Key.Escape
            }
            .pointerInput(Unit) {
                // Oltre questa distanza, col tasto ancora giù, è "tieni premuto e muovi".
                val holdDistance = 8.dp.toPx()
                awaitPointerEventScope {
                    var held = false
                    while (true) {
                        // Prima di tutto quello che c'è dentro: il testo non deve vedere il tasto centrale.
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: continue
                        val start = anchor
                        if (start == null) {
                            if (event.type == PointerEventType.Press && event.buttons.isTertiaryPressed && !event.buttons.isPrimaryPressed) {
                                anchor = change.position
                                pointer = change.position
                                held = false
                                change.consume()
                            }
                            continue
                        }
                        when (event.type) {
                            PointerEventType.Move -> {
                                pointer = change.position
                                if (event.buttons.isTertiaryPressed && (change.position - start).getDistance() > holdDistance) held = true
                                change.consume()
                            }
                            // Lasciato il tasto centrale dopo averlo tenuto e mosso: finito.
                            PointerEventType.Release -> {
                                if (held && !event.buttons.isTertiaryPressed) anchor = null
                                change.consume()
                            }
                            // Un altro clic, di qualunque tasto: finito, e il clic non fa altro.
                            PointerEventType.Press -> {
                                anchor = null
                                change.consume()
                            }
                            // La rotella ferma lo scorrimento e scorre come sempre.
                            PointerEventType.Scroll -> anchor = null
                            else -> {}
                        }
                    }
                }
            }
            .drawWithContent {
                drawContent()
                anchor?.let { drawAutoscrollIcon(it) }
            }
    }

/** L'icona dei browser: un cerchio chiaro col bordo, una freccia su, una giù e un punto in mezzo. */
private fun DrawScope.drawAutoscrollIcon(at: Offset) {
    val radius = 14.dp.toPx()
    val ink = Color(0xFF3C3C3C)
    drawCircle(Color(0xF2F5F5F5), radius, at)
    drawCircle(ink, radius, at, style = Stroke(width = 1.dp.toPx()))
    drawCircle(ink, 2.dp.toPx(), at)
    val half = 4.dp.toPx()
    val tip = 10.dp.toPx()
    val base = 5.dp.toPx()
    for (dir in listOf(-1f, 1f)) {
        val path = Path().apply {
            moveTo(at.x, at.y + dir * tip)
            lineTo(at.x - half, at.y + dir * base)
            lineTo(at.x + half, at.y + dir * base)
            close()
        }
        drawPath(path, ink)
    }
}
