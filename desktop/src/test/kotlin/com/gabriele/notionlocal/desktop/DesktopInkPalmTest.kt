package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.ui.screen.InkPageState
import com.gabriele.notionlocal.ui.screen.inkInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * **Il palmo col dito acceso** (24/11/2026, trovato dall'utente nel collaudo: "Il palmo lascia i segni, ma solo se
 * attivo l'iconcina della mano"): con la penna nel raggio la mano non disegna, anche se la penna sta ferma; andata
 * via la penna, il dito disegna di nuovo. Il segno del palmo cominciato prima della penna (Android annulla il tocco,
 * o la penna sospesa arriva mentre la mano disegna) qui non si può fare: la scena di prova non annulla tocchi e non
 * manda la penna sospesa durante un tocco.
 */
@OptIn(ExperimentalComposeUiApi::class)
class DesktopInkPalmTest {

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    @Test
    fun thePalmDoesNotDrawWhileThePenIsNear() {
        var strokes = 0
        val scene = onWindowThread {
            ImageComposeScene(width = 400, height = 600, density = Density(1f)) {
                val list = rememberLazyListState()
                val state = remember { InkPageState(list).apply { anchorOfItem = List(30) { "riga$it" } } }
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(state = list, modifier = Modifier.fillMaxSize()) {
                        items(30) { Box(Modifier.fillMaxWidth().height(40.dp)) }
                    }
                    Box(
                        Modifier.fillMaxSize().inkInput(
                            state = state,
                            onStroke = { _, _, _ -> strokes++ },
                            onErase = { _, _ -> },
                            onEraseEnd = {}
                        )
                    )
                }
            }
        }
        var now = 1_000_000L
        fun frames() = repeat(5) { onWindowThread { scene.render(now * 1_000_000L) } }
        fun send(type: PointerEventType, at: Offset, pointer: PointerType, pressed: Boolean) = onWindowThread {
            scene.sendPointerEvent(
                type, at, type = pointer, timeMillis = now,
                buttons = if (pressed) PointerButtons(isPrimaryPressed = true) else PointerButtons()
            )
        }
        /** Un tratto del dito da (50, y) a (250, y), in 10 passi. */
        fun fingerStroke(y: Float, during: () -> Unit = {}) {
            send(PointerEventType.Press, Offset(50f, y), PointerType.Touch, true)
            for (step in 1..10) {
                now += 16
                send(PointerEventType.Move, Offset(50f + step * 20f, y), PointerType.Touch, true)
                if (step == 5) during()
            }
            now += 16
            send(PointerEventType.Release, Offset(250f, y), PointerType.Touch, false)
            frames()
        }
        try {
            frames()
            fingerStroke(100f)
            assertEquals("senza penna il dito disegna", 1, strokes)

            // La penna sospesa, poi ferma per due secondi (più della finestra di un secondo): la mano non disegna.
            send(PointerEventType.Enter, Offset(300f, 300f), PointerType.Stylus, false)
            send(PointerEventType.Move, Offset(300f, 300f), PointerType.Stylus, false)
            now += 2_000
            fingerStroke(200f)
            assertEquals("con la penna nel raggio il palmo non disegna", 1, strokes)

            // La penna se ne va. Sul telefono Android lo dice (l'uscita della penna sospesa); la scena di prova del
            // PC la manda solo per il mouse, quindi qui vale la rete di sicurezza: tre secondi senza notizie della
            // penna e il dito disegna di nuovo.
            send(PointerEventType.Exit, Offset(300f, 300f), PointerType.Stylus, false)
            now += 3_500
            fingerStroke(300f)
            assertEquals("senza notizie della penna il dito disegna", 2, strokes)
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
