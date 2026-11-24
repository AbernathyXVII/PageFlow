package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.gabriele.notionlocal.ui.screen.LINK_TAG
import com.gabriele.notionlocal.ui.screen.linkTaps
import org.jetbrains.skia.Surface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * **La manina sui link, sul PC** (24/11/2026, chiesto dall'utente: "quando sposto la puntina del mouse
 * su un link, fai sì che venga fuori l'icona classica di apertura dei link come in Windows"): sopra le
 * lettere di un link il puntatore è la mano; sul resto del testo resta la I per scrivere.
 */
@OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
class DesktopLinkHandTest {

    @Test
    fun theHandOnlyOverTheLink() {
        var icon: PointerIcon? = null
        var layout: TextLayoutResult? = null
        val scene = CanvasLayersComposeScene(
            density = Density(1f),
            size = IntSize(400, 100),
            platformContext = object : PlatformContext.Empty() {
                override fun setPointerIcon(pointerIcon: PointerIcon) {
                    icon = pointerIcon
                }
            }
        )
        val shown = buildAnnotatedString {
            append("vai su ")
            val start = length
            append("google")
            addStringAnnotation(LINK_TAG, "https://google.com", start, length)
            append(" e torna")
        }
        scene.setContent {
            BasicTextField(
                value = TextFieldValue(shown.text),
                onValueChange = {},
                visualTransformation = VisualTransformation { TransformedText(shown, OffsetMapping.Identity) },
                onTextLayout = { layout = it },
                textStyle = TextStyle(fontSize = 20.sp),
                modifier = Modifier.fillMaxWidth().linkTaps(layout = { layout }, onLink = {})
            )
        }
        val surface = Surface.makeRasterN32Premul(400, 100)
        fun frame() = scene.render(surface.canvas.asComposeCanvas(), System.nanoTime())
        frame()
        val text = layout!!.getBoundingBox(2).center
        val link = layout!!.getBoundingBox(9).center
        fun moveTo(at: Offset) {
            scene.sendPointerEvent(PointerEventType.Move, at)
            frame()
        }
        try {
            scene.sendPointerEvent(PointerEventType.Enter, text)
            moveTo(text)
            assertNotEquals("sul testo la I, non la mano", PointerIcon.Hand, icon)
            moveTo(link)
            assertEquals("sul link la mano", PointerIcon.Hand, icon)
            moveTo(text)
            assertNotEquals("tornati sul testo", PointerIcon.Hand, icon)
        } finally {
            scene.close()
        }
    }
}
