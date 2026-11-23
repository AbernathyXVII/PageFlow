package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.Density
import com.gabriele.notionlocal.ui.screen.NoLongPress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * **Sul PC niente pressione lunga** (chiesto dall'utente il 23/11/2026: "togli il tasto sinistro
 * prolungato, quello destro mi basta"): tenuto premuto per un secondo e mezzo, dentro
 * `NoLongPress` non succede niente; fuori sì (la prova che il gesto arriva).
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
class DesktopNoLongPressTest {
    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    private fun longPresses(off: Boolean): Int {
        var count = 0
        val scene = onWindowThread {
            ImageComposeScene(width = 200, height = 200, density = Density(1f)) {
                val box = @androidx.compose.runtime.Composable {
                    Box(Modifier.fillMaxSize().combinedClickable(onClick = {}, onLongClick = { count++ }))
                }
                if (off) NoLongPress { box() } else box()
            }
        }
        try {
            var time = 0L
            fun frames(n: Int) = repeat(n) {
                onWindowThread { scene.render(time) }
                time += 50_000_000L
                Thread.sleep(50)
            }
            frames(3)
            val at = Offset(100f, 100f)
            onWindowThread {
                scene.sendPointerEvent(PointerEventType.Move, at, type = PointerType.Mouse)
                scene.sendPointerEvent(PointerEventType.Press, at, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
            }
            frames(30)
            onWindowThread {
                scene.sendPointerEvent(PointerEventType.Release, at, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
            }
            frames(3)
        } finally {
            onWindowThread { scene.close() }
        }
        return count
    }

    @Test
    fun holdingTheLeftButtonDoesNothing() {
        assertEquals("fuori da NoLongPress la pressione lunga arriva", 1, longPresses(off = false))
        assertEquals("dentro no", 0, longPresses(off = true))
    }
}
