package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.Density
import com.gabriele.notionlocal.ui.screen.Switch
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * **Niente alone intorno al pallino di un interruttore sul PC** (18/11/2026, chiesto dall'utente:
 * "quando clicco su un pallino… c'è un altro pallino più grande molto chiaro e trasparente… Lo puoi
 * togliere?"). Col mouse premuto sull'interruttore: quello di Material ha il cerchio chiaro
 * intorno al pallino, il nostro no.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopSwitchHaloTest {
    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    /** Quanti punti, attorno all'interruttore ma fuori dalla sua pista, non sono del colore del fondo, col mouse premuto. */
    private fun haloPixels(name: String, switch: @Composable (Boolean, (Boolean) -> Unit) -> Unit): Int {
        var checked by mutableStateOf(false)
        val scene = onWindowThread {
            ImageComposeScene(width = 200, height = 120, density = Density(1f)) {
                NotionLocalTheme {
                    Box(Modifier.fillMaxSize().background(DarkBackground), contentAlignment = Alignment.Center) {
                        switch(checked) { checked = it }
                    }
                }
            }
        }
        var time = 0L
        fun frames(count: Int) = repeat(count) {
            onWindowThread { scene.render(time) }
            time += 16_000_000L
            Thread.sleep(10)
        }
        try {
            frames(10)
            val idle = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }
            // Il pallino spento sta a sinistra della pista (larga 52, alta 32, al centro della scena).
            val thumb = Offset(100f - 26f + 16f, 60f)
            onWindowThread {
                scene.sendPointerEvent(PointerEventType.Move, thumb, type = PointerType.Mouse)
                scene.sendPointerEvent(PointerEventType.Press, thumb, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
            }
            frames(15)
            val pressed = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }
            File("build/desktop-screenshots").mkdirs()
            File("build/desktop-screenshots", "switch_halo_$name.png").writeBytes(Image.makeFromBitmap(pressed).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            // Sopra e sotto la pista (alta 32: da 44 a 76), dove arriva solo l'alone (raggio 20 dal pallino).
            return (thumb.x.toInt() - 18..thumb.x.toInt() + 18).sumOf { x ->
                (listOf(40..43, 77..80)).sumOf { ys -> ys.count { y -> pressed.getColor(x, y) != idle.getColor(x, y) } }
            }
        } finally {
            onWindowThread {
                scene.sendPointerEvent(PointerEventType.Release, Offset(0f, 0f), type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
                scene.close()
            }
        }
    }

    @Test
    fun theSwitchHasNoHaloOnThePc() {
        val material = haloPixels("material") { checked, onChange -> androidx.compose.material3.Switch(checked = checked, onCheckedChange = onChange) }
        assertTrue("quello di Material ha l'alone: $material", material > 20)
        val ours = haloPixels("nostro") { checked, onChange -> Switch(checked = checked, onCheckedChange = onChange) }
        assertEquals("il nostro no", 0, ours)
    }
}
