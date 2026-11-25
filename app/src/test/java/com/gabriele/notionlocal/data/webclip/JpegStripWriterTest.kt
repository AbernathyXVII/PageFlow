package com.gabriele.notionlocal.data.webclip

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **Il JPEG a strisce del Web Clipper** (20/10/2026): scritto qualche riga
 * alla volta, si legge con un decodificatore qualsiasi (qui quello di Java)
 * e torna con i colori di partenza, a meno dei piccoli scarti del JPEG.
 */
class JpegStripWriterTest {

    /** Una "pagina" di prova: sfumature, colori pieni e righe nere e sottili come il testo. */
    private fun page(width: Int, height: Int) = IntArray(width * height) { i ->
        val x = i % width
        val y = i / width
        when {
            y % 9 == 4 && x % 13 < 9 -> 0xFF000000.toInt()
            x < width / 3 -> 0xFF000000.toInt() or ((x * 255 / width) shl 16) or ((y * 255 / height) shl 8) or 0x40
            x < 2 * width / 3 -> 0xFFE03E3E.toInt()
            else -> 0xFFFFFFFF.toInt()
        }
    }

    private fun encode(pixels: IntArray, width: Int, height: Int, chunk: Int): ByteArray {
        val bytes = ByteArrayOutputStream()
        val writer = JpegStripWriter(bytes, width, height)
        var row = 0
        while (row < height) {
            val rows = minOf(chunk, height - row)
            writer.writeRows(pixels, row * width, rows)
            row += rows
        }
        writer.finish()
        return bytes.toByteArray()
    }

    /**
     * Il decodificatore JPEG di Java (`javax.imageio`): nei test di Android non
     * si vede in compilazione, ma c'è quando i test girano.
     */
    private class Decoded(bytes: ByteArray) {
        private val image: Any = Class.forName("javax.imageio.ImageIO")
            .getMethod("read", java.io.InputStream::class.java)
            .invoke(null, ByteArrayInputStream(bytes))
            ?: error("non si legge")
        private val getRgb = image.javaClass.getMethod("getRGB", Int::class.java, Int::class.java)
        val width = image.javaClass.getMethod("getWidth").invoke(image) as Int
        val height = image.javaClass.getMethod("getHeight").invoke(image) as Int
        fun rgb(x: Int, y: Int) = getRgb.invoke(image, x, y) as Int
    }

    private fun assertDecodesClose(width: Int, height: Int, chunk: Int) {
        val pixels = page(width, height)
        val decoded = Decoded(encode(pixels, width, height, chunk))
        assertEquals(width, decoded.width)
        assertEquals(height, decoded.height)
        var total = 0L
        var worst = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val a = pixels[y * width + x]
                val b = decoded.rgb(x, y)
                for (shift in intArrayOf(16, 8, 0)) {
                    val diff = abs(((a shr shift) and 0xFF) - ((b shr shift) and 0xFF))
                    total += diff
                    worst = maxOf(worst, diff)
                }
            }
        }
        val mean = total.toDouble() / (width * height * 3)
        assertTrue("scarto medio $mean", mean < 6.0)
        assertTrue("scarto massimo $worst", worst < 160)
    }

    @Test
    fun aPageWrittenInStripsReadsBackAsItWas() {
        // Misure che non sono multipli di 8, e strisce di righe dispari.
        assertDecodesClose(width = 37, height = 21, chunk = 3)
        assertDecodesClose(width = 70, height = 45, chunk = 8)
        assertDecodesClose(width = 200, height = 333, chunk = 64)
    }

    /**
     * **La foto più alta che si scrive si apre** (25/11/2026, Asakusa di game8: alta 65535, non si
     * apriva). Il decodificatore di Java è libjpeg, come quelli di Skia e di Android: oltre 65500 dice
     * "Maximum supported image dimension is 65500 pixels".
     */
    @Test
    fun theTallestPhotoStillOpens() {
        val decoded = Decoded(encode(page(8, JpegStripWriter.MAX_SIDE), 8, JpegStripWriter.MAX_SIDE, 4096))
        assertEquals(JpegStripWriter.MAX_SIDE, decoded.height)
    }

    @Test
    fun theSameImageGivesTheSameFileWhateverTheStrips() {
        val pixels = page(64, 50)
        assertTrue(encode(pixels, 64, 50, 1).contentEquals(encode(pixels, 64, 50, 50)))
    }

    @Test
    fun theRowsMustBeExactlyTheHeight() {
        val pixels = page(16, 16)
        val writer = JpegStripWriter(ByteArrayOutputStream(), 16, 16)
        writer.writeRows(pixels, 0, 10)
        assertThrows(IllegalStateException::class.java) { writer.finish() }
        assertThrows(IllegalArgumentException::class.java) { writer.writeRows(pixels, 0, 16) }
        assertThrows(IllegalArgumentException::class.java) { JpegStripWriter(ByteArrayOutputStream(), 10, 70_000) }
    }
}
