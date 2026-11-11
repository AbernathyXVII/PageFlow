package com.gabriele.notionlocal.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import com.gabriele.notionlocal.data.settings.AppSettings

/**
 * **Lo zoom del PC, come quello di un browser** (dal 11/11/2026, chiesto
 * dall'utente): Ctrl + e Ctrl − (anche col tastierino), o la rotella con Ctrl
 * premuto (in su ingrandisce). Non si ingrandisce un'immagine: il tema
 * (`NotionLocalTheme`) cambia la densità di tutta l'app, e la pagina si
 * ridispone, con le righe che vanno a capo prima. Gli scatti sono quelli di
 * Chrome; lo zoom resta (`AppSettings.zoom`).
 */
object PcZoom {
    private val LEVELS = listOf(50, 67, 75, 80, 90, 100, 110, 125, 150, 175, 200, 250, 300)

    /** Uno scatto: più grande ([bigger]) o più piccolo; ai capi resta com'è. */
    fun step(bigger: Boolean) {
        val now = AppSettings.zoom
        AppSettings.zoom = if (bigger) LEVELS.firstOrNull { it > now } ?: now else LEVELS.lastOrNull { it < now } ?: now
    }
}

/** Ctrl + (o Ctrl =, come nei browser) e Ctrl −: vero per ingrandire, falso per rimpicciolire, null per gli altri tasti. */
fun zoomKeyOf(event: KeyEvent): Boolean? {
    if (event.type != KeyEventType.KeyDown || !event.isCtrlPressed) return null
    return when (event.key) {
        Key.Plus, Key.Equals, Key.NumPadAdd -> true
        Key.Minus, Key.NumPadSubtract -> false
        else -> null
    }
}

/**
 * La rotella con Ctrl premuto fa lo zoom e non scorre la pagina: va sul
 * riquadro più esterno della finestra, che la vede prima di tutti. Uno scatto
 * per ogni tacca (il touchpad manda tanti pezzetti: si sommano).
 */
fun Modifier.ctrlWheelZoom(): Modifier = pointerInput(Unit) {
    var pending = 0f
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.type != PointerEventType.Scroll || !event.keyboardModifiers.isCtrlPressed) continue
            pending += event.changes.sumOf { it.scrollDelta.y.toDouble() }.toFloat()
            while (pending <= -1f) { PcZoom.step(bigger = true); pending += 1f }
            while (pending >= 1f) { PcZoom.step(bigger = false); pending -= 1f }
            event.changes.forEach { it.consume() }
        }
    }
}
