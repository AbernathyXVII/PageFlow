package com.gabriele.notionlocal.ui.screen

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.DividerStyle
import kotlin.math.PI
import kotlin.math.sin

/**
 * **La riga di un divisore nel suo stile** (dal 18/11/2026, chiesto dall'utente, come in Word):
 * sottile, doppia, spessa, a puntini o a onda, a metà dell'elemento e larga quanto lui. Disegnata
 * attorno a quella sottile di sempre, senza cambiare l'altezza del divisore.
 */
fun Modifier.dividerLine(style: DividerStyle, color: Color): Modifier = drawBehind {
    val y = size.height / 2f
    val thin = 1.dp.toPx()
    when (style) {
        DividerStyle.SINGLE -> drawLine(color, Offset(0f, y), Offset(size.width, y), thin)
        DividerStyle.DOUBLE -> {
            val gap = 1.5.dp.toPx()
            drawLine(color, Offset(0f, y - gap), Offset(size.width, y - gap), thin)
            drawLine(color, Offset(0f, y + gap), Offset(size.width, y + gap), thin)
        }
        DividerStyle.THICK -> drawLine(color, Offset(0f, y), Offset(size.width, y), 3.dp.toPx())
        DividerStyle.DOTTED -> drawLine(
            color, Offset(0f, y), Offset(size.width, y), 2.5.dp.toPx(),
            cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.01f, 6.dp.toPx()))
        )
        DividerStyle.WAVY -> {
            val wave = 8.dp.toPx()
            val height = 2.dp.toPx()
            val path = Path().apply {
                moveTo(0f, y)
                var x = 0f
                while (x <= size.width) {
                    lineTo(x, y + height * sin(2 * PI * x / wave).toFloat())
                    x += 1f
                }
            }
            drawPath(path, color, style = Stroke(width = 1.2.dp.toPx()))
        }
    }
}
