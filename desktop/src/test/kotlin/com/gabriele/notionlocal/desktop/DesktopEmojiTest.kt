package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * **Sul PC le emoji di Google** (12/11/2026, chieste dall'utente): la bandiera
 * 🇮🇹 si disegna a colori anche nel testo normale e nei caratteri delle pagine.
 * Con le emoji di Windows al suo posto ci sarebbero le lettere "IT", nere: né
 * verde né rosso. Se un aggiornamento di Compose rompe `PcEmoji`, fallisce qui.
 */
class DesktopEmojiTest {
    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    private fun green(c: Int) = ((c shr 8) and 0xFF) > 110 && ((c shr 16) and 0xFF) < 90 && (c and 0xFF) < 120
    private fun red(c: Int) = ((c shr 16) and 0xFF) > 160 && ((c shr 8) and 0xFF) < 90 && (c and 0xFF) < 100

    @Test
    fun theFlagIsDrawnInColourOnThePc() {
        assertTrue("Noto non è nelle liste dei caratteri", PcEmoji.install())
        val families = listOf(null, FontFamily.Serif, FontFamily.Monospace)
        val scene = onWindowThread {
            ImageComposeScene(width = 300, height = 60 * families.size, density = Density(1f)) {
                PcEmoji.Load()
                Column(Modifier.background(Color.White)) {
                    families.forEach { family ->
                        Text("Ab 🇮🇹", style = TextStyle(fontSize = 40.sp, fontFamily = family, color = Color.Black))
                    }
                }
            }
        }
        try {
            val picture = onWindowThread { scene.render(0L); Bitmap.makeFromImage(scene.render(100_000_000L)) }
            File("build/desktop-screenshots").mkdirs()
            File("build/desktop-screenshots", "emoji_flag.png").writeBytes(Image.makeFromBitmap(picture).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            families.forEachIndexed { row, family ->
                val colours = (row * 60 until (row + 1) * 60).flatMap { y -> (0 until picture.width).map { x -> picture.getColor(x, y) } }
                val greens = colours.count(::green)
                val reds = colours.count(::red)
                assertTrue("$family: verde $greens, rosso $reds", greens > 100 && reds > 100)
            }
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
