package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.gabriele.notionlocal.platform.screenHeightDp
import kotlin.math.roundToInt

/**
 * **Sul PC le finestrelle si aprono accanto al clic** (dal 11/11/2026,
 * chiesto dall'utente: "si apre una schermata uguale al telefono, ma dato
 * che siamo su PC vorrei che fosse diversa", con le foto dei menu di
 * Notion). Al posto del pannello che sale dal basso, un riquadro che parte
 * dal punto dell'ultimo clic, sempre dentro la finestra (sopra il punto se
 * sotto non c'è posto), senza scurire la pagina. Esc o un clic fuori lo
 * chiudono, come il pannello.
 *
 * Una finestra aperta da un'altra (un colore dal menu di un blocco) prende
 * il suo posto: i clic dentro un riquadro non spostano il punto.
 */
@Composable
actual fun PcPanel(
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    containerColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    val point = remember { LastPress.point }
    val margin = with(LocalDensity.current) { PANEL_MARGIN.roundToPx() }
    Popup(
        popupPositionProvider = remember(point, margin) { BesidePoint(point, margin) },
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
        // Esc chiude tutto il riquadro, come su Notion: anche da un campo di testo dentro.
        onKeyEvent = { event -> (event.key == Key.Escape && event.type == KeyEventType.KeyDown).also { if (it) onDismissRequest() } }
    ) {
        Surface(
            modifier = modifier
                .width(PANEL_WIDTH)
                .heightIn(max = screenHeightDp() - PANEL_MARGIN * 2),
            shape = RoundedCornerShape(10.dp),
            color = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(top = 12.dp), content = content)
        }
    }
}

/** Dove si è premuto l'ultima volta nella finestra (vedi [notePresses]); null prima del primo clic. */
object LastPress {
    var point: Offset? = null
}

/**
 * Segna dove si preme, guardando soltanto. Va sul riquadro che occupa tutta
 * la finestra, a partire dall'angolo in alto a sinistra: i suoi punti sono
 * quelli della finestra.
 */
fun Modifier.notePresses(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.type == PointerEventType.Press) event.changes.firstOrNull()?.let { LastPress.point = it.position }
        }
    }
}

/** L'angolo in alto a sinistra sotto il punto; spostato quanto serve per stare nella finestra. Senza punto, al centro. */
internal class BesidePoint(private val point: Offset?, private val margin: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val width = popupContentSize.width
        val height = popupContentSize.height
        val p = point ?: return IntOffset((windowSize.width - width) / 2, (windowSize.height - height) / 2)
        val x = p.x.roundToInt().coerceAtMost(windowSize.width - margin - width).coerceAtLeast(margin)
        val below = p.y.roundToInt() + margin
        val above = p.y.roundToInt() - margin - height
        val y = when {
            below + height <= windowSize.height - margin -> below
            above >= margin -> above
            else -> (windowSize.height - margin - height).coerceAtLeast(margin)
        }
        return IntOffset(x, y)
    }
}

// Come i menu di Notion (240–290), con un po' di posto per le finestrelle più piene.
private val PANEL_WIDTH = 320.dp
private val PANEL_MARGIN = 8.dp
