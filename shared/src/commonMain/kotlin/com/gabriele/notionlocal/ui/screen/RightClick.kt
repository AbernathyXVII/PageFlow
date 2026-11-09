package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
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
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            event.changes.forEach { it.consume() }
                            current()
                        }
                    }
                }
            }
        }
    }
