package com.gabriele.notionlocal.ui

import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.ui.screen.ImageFrame
import com.gabriele.notionlocal.ui.screen.dragged
import com.gabriele.notionlocal.ui.screen.frameOf
import com.gabriele.notionlocal.ui.screen.widened
import com.gabriele.notionlocal.ui.screen.withFrame
import com.gabriele.notionlocal.ui.screen.withOriginalSize
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * **Il riquadro di un'immagine** (20/10/2026): "Resize" tiene il centro e la
 * forma, "Full resize" muove solo il bordo afferrato, come una finestra.
 */
class ImageFrameTest {

    @Test
    fun resizeKeepsTheCenterAndTheShape() {
        val frame = ImageFrame(left = 250f, width = 500f, height = 250f)
        val wider = frame.widened(side = 1, dx = 100f, pageWidth = 1000f, minWidth = 250f)
        assertEquals(ImageFrame(150f, 700f, 350f), wider)
        // Mai oltre la pagina, mai sotto il minimo.
        assertEquals(1000f, frame.widened(1, 900f, 1000f, 250f).width)
        assertEquals(250f, frame.widened(-1, 900f, 1000f, 250f).width)
    }

    @Test
    fun fullResizeMovesOnlyTheEdgeYouHold() {
        val frame = ImageFrame(left = 100f, width = 400f, height = 300f)
        // Il bordo destro: solo la larghezza.
        assertEquals(ImageFrame(100f, 450f, 300f), frame.dragged(1, 0, 50f, 0f, 1000f, 48f))
        // Il bordo sinistro: comincia più a destra, il destro resta dov'era.
        assertEquals(ImageFrame(150f, 350f, 300f), frame.dragged(-1, 0, 50f, 0f, 1000f, 48f))
        // Sotto e sopra: solo l'altezza.
        assertEquals(ImageFrame(100f, 400f, 360f), frame.dragged(0, 1, 0f, 60f, 1000f, 48f))
        assertEquals(ImageFrame(100f, 400f, 240f), frame.dragged(0, -1, 0f, 60f, 1000f, 48f))
        // Un angolo muove due bordi.
        assertEquals(ImageFrame(100f, 420f, 330f), frame.dragged(1, 1, 20f, 30f, 1000f, 48f))
        // Non esce dalla pagina e non sparisce.
        assertEquals(0f, frame.dragged(-1, 0, -500f, 0f, 1000f, 48f).left)
        assertEquals(900f, frame.dragged(1, 0, 900f, 0f, 1000f, 48f).width)
        assertEquals(48f, frame.dragged(0, 1, 0f, -900f, 1000f, 48f).height)
    }

    @Test
    fun theFrameIsSavedOnlyWithWhatChanged() {
        val attrs = BlockAttrs(file = "a.jpg")
        // A tutta pagina e al centro: si ricorda la larghezza (dal 13/11/2026: senza, sarebbe larga quanto l'immagine vera).
        assertEquals(attrs.copy(width = 1f), attrs.withFrame(ImageFrame(0f, 1000f, 500f), 1000f, full = false))
        // "Resize": larghezza, al centro, la forma non si tocca.
        assertEquals(attrs.copy(width = 0.5f), attrs.withFrame(ImageFrame(250f, 500f, 250f), 1000f, full = false))
        // "Full resize": anche dove comincia e la forma.
        assertEquals(attrs.copy(width = 0.4f, left = 0.1f, aspect = 0.5f), attrs.withFrame(ImageFrame(100f, 400f, 200f), 1000f, full = true))
        // "Original size" toglie tutto.
        assertEquals(attrs, attrs.copy(width = 0.4f, left = 0.1f, aspect = 0.5f).withOriginalSize())
        // Riletto dal blocco, il riquadro è lo stesso.
        assertEquals(ImageFrame(100f, 400f, 200f), frameOf(attrs.copy(width = 0.4f, left = 0.1f, aspect = 0.5f), 1000f, natural = 0.75f))
        assertEquals(ImageFrame(250f, 500f, 375f), frameOf(attrs.copy(width = 0.5f), 1000f, natural = 0.75f))
        // **Senza misura salvata, larga quanto l'immagine vera** (dal 13/11/2026), al centro, al massimo la pagina.
        assertEquals(ImageFrame(400f, 200f, 150f), frameOf(attrs, 1000f, natural = 0.75f, naturalWidth = 200f))
        assertEquals(ImageFrame(0f, 1000f, 750f), frameOf(attrs, 1000f, natural = 0.75f, naturalWidth = 3000f))
    }
}
