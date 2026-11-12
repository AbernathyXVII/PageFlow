package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.platform.PcEmoji
import com.gabriele.notionlocal.platform.emojiPresentationRanges
import com.gabriele.notionlocal.platform.withEmojiFont
import com.gabriele.notionlocal.ui.screen.PageIconView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Sul PC le emoji di Google** (12/11/2026, chieste dall'utente): la bandiera
 * 🇮🇹 si disegna a colori anche nel testo normale e nei caratteri delle pagine
 * (con le emoji di Windows ci sarebbero le lettere "IT", nere). E le emoji che
 * Windows ha anche in lettere (☺️, ♥️) escono a colori nelle icone e nel testo
 * delle pagine, non bianche. Se un aggiornamento di Compose rompe `PcEmoji`,
 * fallisce qui.
 */
class DesktopEmojiTest {
    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    private fun green(c: Int) = ((c shr 8) and 0xFF) > 110 && ((c shr 16) and 0xFF) < 90 && (c and 0xFF) < 120
    private fun red(c: Int) = ((c shr 16) and 0xFF) > 160 && ((c shr 8) and 0xFF) < 90 && (c and 0xFF) < 100
    private fun yellow(c: Int) = ((c shr 16) and 0xFF) > 200 && ((c shr 8) and 0xFF) > 150 && (c and 0xFF) < 90

    private fun Bitmap.count(rows: IntRange, test: (Int) -> Boolean) = rows.sumOf { y -> (0 until width).count { x -> test(getColor(x, y)) } }

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
                val rows = row * 60 until (row + 1) * 60
                val greens = picture.count(rows, ::green)
                val reds = picture.count(rows, ::red)
                assertTrue("$family: verde $greens, rosso $reds", greens > 100 && reds > 100)
            }
        } finally {
            onWindowThread { scene.close() }
        }
    }

    @Test
    fun theWholeSequenceGetsTheEmojiFont() {
        assertEquals(listOf(2..3), emojiPresentationRanges("A ☺️ b"))
        assertEquals(listOf(0..2), emojiPresentationRanges("1️⃣"))
        // Bandiera pirata: 🏴 + U+200D + ☠ + U+FE0F, tutta insieme.
        assertEquals(listOf(0..4), emojiPresentationRanges("🏴‍☠️"))
        // L'occhio nel fumetto: due U+FE0F nella stessa sequenza.
        assertEquals(listOf(0..6), emojiPresentationRanges("👁️‍🗨️"))
        // Pilota col tono della pelle: il tono e la persona prima dell'aereo.
        assertEquals(listOf(0..6), emojiPresentationRanges("🧑🏽‍✈️"))
        assertEquals(listOf(0..3), emojiPresentationRanges("☺️☺️"))
        assertEquals(emptyList<IntRange>(), emojiPresentationRanges("😀 Ab"))
        assertEquals(emptyList<IntRange>(), emojiPresentationRanges("️"))
    }

    @Test
    fun theEmojiWindowsHasAsLettersAreInColour() {
        assertTrue(PcEmoji.install())
        val context = DesktopContext(Files.createTempDirectory("pageflow-emoji").toFile())
        val scene = onWindowThread {
            ImageComposeScene(width = 200, height = 180, density = Density(1f)) {
                PcEmoji.Load()
                CompositionLocalProvider(LocalPlatformContext provides context) {
                    Column(Modifier.background(Color.Black)) {
                        // L'icona di una pagina, come nell'albero e nel selettore.
                        PageIconView(emoji = "☺️", iconImage = null, isDatabase = false, size = 50.dp)
                        // Il testo di una pagina, come lo prepara la trasformazione dell'editor.
                        Text(AnnotatedString("A ♥️").withEmojiFont(), style = TextStyle(fontSize = 40.sp, color = Color.White))
                        // Lo stesso senza: Windows lo scrive in lettere, bianco.
                        Text("A ♥️", style = TextStyle(fontSize = 40.sp, color = Color.White))
                    }
                }
            }
        }
        try {
            val picture = onWindowThread { scene.render(0L); Bitmap.makeFromImage(scene.render(100_000_000L)) }
            File("build/desktop-screenshots").mkdirs()
            File("build/desktop-screenshots", "emoji_text_style.png").writeBytes(Image.makeFromBitmap(picture).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            val icon = picture.count(0 until 50, ::yellow)
            val heart = picture.count(50 until 105, ::red)
            val plain = picture.count(110 until 165, ::red)
            assertTrue("☺️ gialla: $icon", icon > 200)
            assertTrue("♥️ rosso nel testo: $heart", heart > 100)
            assertTrue("senza: $plain (la prova vale solo se qui è bianco)", plain < 20)
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
