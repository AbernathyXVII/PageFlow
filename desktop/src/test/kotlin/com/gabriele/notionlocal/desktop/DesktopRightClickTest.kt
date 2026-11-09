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
import androidx.compose.ui.unit.Density
import com.gabriele.notionlocal.ui.screen.onRightClick
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * **Il clic destro sul PC** (dal 09/11/2026): dove tenendo premuto si apre un
 * menu, il clic destro apre lo stesso menu, e il clic normale (aprire la riga)
 * non parte anche lui. Il clic sinistro resta il clic di sempre.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
class DesktopRightClickTest {

    private var menus = 0
    private var opens = 0

    private fun scene() = ImageComposeScene(width = 200, height = 200, density = Density(1f)) {
        Box(
            Modifier
                .fillMaxSize()
                .onRightClick { menus++ }
                .combinedClickable(onClick = { opens++ }, onLongClick = { menus++ })
        )
    }

    private fun ImageComposeScene.click(button: PointerButton, buttons: PointerButtons) {
        val at = Offset(100f, 100f)
        sendPointerEvent(PointerEventType.Move, at)
        sendPointerEvent(PointerEventType.Press, at, buttons = buttons, button = button)
        render()
        sendPointerEvent(PointerEventType.Release, at, buttons = PointerButtons(), button = button)
        render()
    }

    @Test
    fun rightClickOpensTheMenuOnly() {
        val scene = scene()
        scene.render()
        scene.click(PointerButton.Secondary, PointerButtons(isSecondaryPressed = true))
        scene.close()
        assertEquals(1, menus)
        assertEquals(0, opens)
    }

    @Test
    fun leftClickStillOpens() {
        val scene = scene()
        scene.render()
        scene.click(PointerButton.Primary, PointerButtons(isPrimaryPressed = true))
        scene.close()
        assertEquals(0, menus)
        assertEquals(1, opens)
    }
}
