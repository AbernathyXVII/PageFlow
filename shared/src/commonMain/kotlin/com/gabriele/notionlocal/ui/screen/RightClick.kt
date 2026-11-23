package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput

/**
 * **Il clic destro come la pressione lunga** (dal 09/11/2026, per il PC): dove
 * tenendo premuto col dito si apre un menu — una riga di un database, un
 * blocco, un widget — il clic destro del mouse apre lo stesso. Il clic si
 * ferma qui, così il clic normale (aprire la riga) non parte anche lui. Col
 * dito il tasto destro non c'è, e non cambia niente. Con [action] null, niente.
 */
fun Modifier.onRightClick(action: (() -> Unit)?): Modifier =
    if (action == null) {
        this
    } else {
        composed {
            val current by rememberUpdatedState(action)
            pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        // Uno dentro l'altro (una cella in una riga) se l'è già preso quello fuori: un menu solo.
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed && event.changes.none { it.isConsumed }) {
                            event.changes.forEach { it.consume() }
                            current()
                        }
                    }
                }
            }
        }
    }

/**
 * **La pressione lunga prima di quello che c'è dentro** (dal 18/11/2026): sopra
 * una cella che al tocco fa già altro (un tag, una casella), tenendo premuto col
 * dito si apre [action], e il dito che si alza non arriva alla cella. Un dito
 * che si sposta (per scorrere) o si alza prima non fa niente. Col mouse c'è
 * [onRightClick].
 */
fun Modifier.onLongPressFirst(action: (() -> Unit)?): Modifier =
    if (action == null) {
        this
    } else {
        composed {
            val current by rememberUpdatedState(action)
            pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    if (down.type != PointerType.Touch) return@awaitEachGesture
                    // Null: è passato il tempo della pressione lunga col dito fermo.
                    val ended = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        while (true) {
                            val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed || (change.position - down.position).getDistance() > viewConfiguration.touchSlop) break
                        }
                    }
                    if (ended != null) return@awaitEachGesture
                    current()
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                }
            }
        }
    }
