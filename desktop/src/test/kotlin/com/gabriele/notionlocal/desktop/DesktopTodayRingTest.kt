package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import com.gabriele.notionlocal.ui.screen.SamsungCalendar
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * **Nel calendario per scegliere la data oggi è cerchiato in bianco** (18/11/2026, chiesto
 * dall'utente: "invece che essere in grassetto arancione sarà semplicemente cerchiato in bianco. E
 * quello che ho selezionato, rimane arancione").
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopTodayRingTest {
    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    @Test
    fun todayIsCircledAndTheChosenDayStaysOrange() {
        val today = LocalDate.now()
        // Un altro giorno dello stesso mese, scelto.
        val chosen = if (today.dayOfMonth > 15) today.minusDays(7) else today.plusDays(7)
        val scene = onWindowThread {
            ImageComposeScene(width = 420, height = 520, density = Density(1f)) {
                NotionLocalTheme {
                    Box(Modifier.fillMaxSize().background(DarkBackground)) {
                        SamsungCalendar(start = chosen, end = null, onPick = {}, onWheelPick = {})
                    }
                }
            }
        }
        try {
            repeat(20) { i -> onWindowThread { scene.render(i * 100_000_000L) }; Thread.sleep(20) }
            val shot = onWindowThread { Bitmap.makeFromImage(scene.render(3_000_000_000L)) }
            File("build/desktop-screenshots").mkdirs()
            File("build/desktop-screenshots", "today_ring_picker.png").writeBytes(Image.makeFromBitmap(shot).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            assertTrue("oggi cerchiato", todayRing(shot, 60) != null)
            fun orange(c: Int) = ((c shr 16) and 0xFF) > 200 && ((c shr 8) and 0xFF) in 100..180 && ((c shr 16) and 0xFF) - (c and 0xFF) > 80
            val orangeDots = (0 until shot.height step 2).sumOf { y -> (0 until shot.width step 2).count { x -> orange(shot.getColor(x, y)) } }
            assertTrue("il giorno scelto resta arancione: $orangeDots", orangeDots > 80)
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
