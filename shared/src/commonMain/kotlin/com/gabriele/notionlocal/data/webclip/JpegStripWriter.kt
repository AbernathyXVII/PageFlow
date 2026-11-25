package com.gabriele.notionlocal.data.webclip

import java.io.BufferedOutputStream
import java.io.OutputStream
import kotlin.math.abs

/**
 * **Un JPEG scritto a strisce** (dal 20/10/2026, per il Web Clipper): la
 * foto di una pagina web lunga arriva qualche riga alla volta e va subito nel
 * file, senza mai stare tutta in memoria.
 *
 * Il codificatore di Android (`Bitmap.compress`) vuole l'immagine intera:
 * una pagina come quella di game8 a 2160 pixel di larghezza ne è alta più di
 * ventimila, e in memoria sarebbero quasi duecento megabyte. Qui ne stanno
 * otto righe alla volta.
 *
 * JPEG "baseline" come quello delle fotocamere, con le tabelle di Huffman
 * standard e i colori a piena risoluzione (4:4:4): nelle foto delle pagine
 * il testo colorato resta nitido. Lo leggono tutti, `BitmapRegionDecoder`
 * compreso, che serve a mostrarlo a pezzi. Il formato arriva a 65535 pixel
 * per lato, ma chi lo legge si ferma a 65500 (vedi [MAX_SIDE]).
 */
class JpegStripWriter(
    output: OutputStream,
    val width: Int,
    val height: Int,
    quality: Int = DEFAULT_QUALITY
) {
    init {
        require(width in 1..MAX_SIDE && height in 1..MAX_SIDE) { "JPEG: $width x $height fuori misura" }
    }

    private val out = BufferedOutputStream(output, 64 * 1024)
    private val fdtblY = FloatArray(64)
    private val fdtblUV = FloatArray(64)
    private val yTable = IntArray(64)
    private val uvTable = IntArray(64)

    // Le otto righe che aspettano di essere scritte, come colori ARGB.
    private val pending = IntArray(width * 8)
    private var pendingRows = 0
    private var rowsDone = 0
    private var finished = false

    private var bitBuffer = 0
    private var bitCount = 0
    private var dcY = 0
    private var dcU = 0
    private var dcV = 0

    private val yBlock = FloatArray(64)
    private val uBlock = FloatArray(64)
    private val vBlock = FloatArray(64)
    private val quantized = IntArray(64)
    private val zigzagged = IntArray(64)

    init {
        setQuality(quality.coerceIn(1, 100))
        writeHeaders()
    }

    /** Le righe dopo quelle già date: `rows` righe da `width` colori ARGB, a partire da `offset`. */
    fun writeRows(pixels: IntArray, offset: Int, rows: Int) {
        check(!finished) { "JPEG già chiuso" }
        require(rowsDone + pendingRows + rows <= height) { "Troppe righe per un JPEG alto $height" }
        var row = 0
        while (row < rows) {
            System.arraycopy(pixels, offset + row * width, pending, pendingRows * width, width)
            pendingRows++
            row++
            if (pendingRows == 8) encodePending()
        }
    }

    /** Chiude il file: le ultime righe, e la fine. Le righe date devono essere `height`. */
    fun finish() {
        check(!finished) { "JPEG già chiuso" }
        check(rowsDone + pendingRows == height) { "JPEG: ${rowsDone + pendingRows} righe su $height" }
        if (pendingRows > 0) encodePending()
        // Gli ultimi bit, completati con degli 1 come vuole il formato.
        if (bitCount > 0) writeBits((1 shl (8 - bitCount)) - 1, 8 - bitCount)
        out.write(0xFF)
        out.write(0xD9)
        out.flush()
        finished = true
    }

    private fun setQuality(quality: Int) {
        val scale = if (quality < 50) 5000 / quality else 200 - quality * 2
        for (i in 0 until 64) {
            yTable[ZIGZAG[i]] = ((Y_QUANT[i] * scale + 50) / 100).coerceIn(1, 255)
            uvTable[ZIGZAG[i]] = ((UV_QUANT[i] * scale + 50) / 100).coerceIn(1, 255)
        }
        var k = 0
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                fdtblY[k] = (1.0 / (yTable[ZIGZAG[k]] * AASF[row] * AASF[col] * 8.0)).toFloat()
                fdtblUV[k] = (1.0 / (uvTable[ZIGZAG[k]] * AASF[row] * AASF[col] * 8.0)).toFloat()
                k++
            }
        }
    }

    private fun writeHeaders() {
        // SOI e JFIF.
        writeMarker(0xD8)
        writeMarker(0xE0)
        writeShort(16)
        "JFIF".forEach { out.write(it.code) }
        out.write(0)
        out.write(1); out.write(1)
        out.write(0)
        writeShort(1); writeShort(1)
        out.write(0); out.write(0)
        // Le tabelle di quantizzazione.
        writeMarker(0xDB)
        writeShort(132)
        out.write(0)
        yTable.forEach { out.write(it) }
        out.write(1)
        uvTable.forEach { out.write(it) }
        // Le misure e i tre canali, senza sottocampionare.
        writeMarker(0xC0)
        writeShort(17)
        out.write(8)
        writeShort(height)
        writeShort(width)
        out.write(3)
        out.write(1); out.write(0x11); out.write(0)
        out.write(2); out.write(0x11); out.write(1)
        out.write(3); out.write(0x11); out.write(1)
        // Le tabelle di Huffman standard.
        writeMarker(0xC4)
        writeShort(418)
        writeHuffmanTable(0x00, DC_Y_COUNTS, DC_VALUES)
        writeHuffmanTable(0x10, AC_Y_COUNTS, AC_Y_VALUES)
        writeHuffmanTable(0x01, DC_UV_COUNTS, DC_VALUES)
        writeHuffmanTable(0x11, AC_UV_COUNTS, AC_UV_VALUES)
        // L'inizio dei dati.
        writeMarker(0xDA)
        writeShort(12)
        out.write(3)
        out.write(1); out.write(0x00)
        out.write(2); out.write(0x11)
        out.write(3); out.write(0x11)
        out.write(0); out.write(63); out.write(0)
    }

    private fun writeHuffmanTable(id: Int, counts: IntArray, values: IntArray) {
        out.write(id)
        for (k in 1..16) out.write(counts[k])
        values.forEach { out.write(it) }
    }

    /** Le righe in attesa, larghe un blocco da 8: l'ultima striscia corta ripete la sua ultima riga. */
    private fun encodePending() {
        val rows = pendingRows
        var x = 0
        while (x < width) {
            var k = 0
            for (dy in 0 until 8) {
                val rowStart = (if (dy < rows) dy else rows - 1) * width
                for (dx in 0 until 8) {
                    val color = pending[rowStart + (x + dx).coerceAtMost(width - 1)]
                    val r = (color shr 16) and 0xFF
                    val g = (color shr 8) and 0xFF
                    val b = color and 0xFF
                    yBlock[k] = 0.299f * r + 0.587f * g + 0.114f * b - 128f
                    uBlock[k] = -0.16874f * r - 0.33126f * g + 0.5f * b
                    vBlock[k] = 0.5f * r - 0.41869f * g - 0.08131f * b
                    k++
                }
            }
            dcY = encodeBlock(yBlock, fdtblY, dcY, DC_Y_CODES, DC_Y_LENGTHS, AC_Y_CODES, AC_Y_LENGTHS)
            dcU = encodeBlock(uBlock, fdtblUV, dcU, DC_UV_CODES, DC_UV_LENGTHS, AC_UV_CODES, AC_UV_LENGTHS)
            dcV = encodeBlock(vBlock, fdtblUV, dcV, DC_UV_CODES, DC_UV_LENGTHS, AC_UV_CODES, AC_UV_LENGTHS)
            x += 8
        }
        rowsDone += rows
        pendingRows = 0
    }

    private fun encodeBlock(
        data: FloatArray,
        fdtbl: FloatArray,
        previousDc: Int,
        dcCodes: IntArray,
        dcLengths: IntArray,
        acCodes: IntArray,
        acLengths: IntArray
    ): Int {
        forwardDct(data)
        for (i in 0 until 64) {
            val v = data[i] * fdtbl[i]
            quantized[i] = if (v > 0f) (v + 0.5f).toInt() else (v - 0.5f).toInt()
        }
        for (i in 0 until 64) zigzagged[ZIGZAG[i]] = quantized[i]

        val dc = zigzagged[0]
        val diff = dc - previousDc
        val dcCategory = category(diff)
        writeBits(dcCodes[dcCategory], dcLengths[dcCategory])
        if (dcCategory > 0) writeBits(bitsOf(diff, dcCategory), dcCategory)

        var last = 63
        while (last > 0 && zigzagged[last] == 0) last--
        if (last == 0) {
            writeBits(acCodes[0x00], acLengths[0x00])
            return dc
        }
        var i = 1
        while (i <= last) {
            val start = i
            while (zigzagged[i] == 0 && i <= last) i++
            var zeros = i - start
            if (zeros >= 16) {
                repeat(zeros shr 4) { writeBits(acCodes[0xF0], acLengths[0xF0]) }
                zeros = zeros and 15
            }
            val value = zigzagged[i]
            val cat = category(value)
            val symbol = (zeros shl 4) + cat
            writeBits(acCodes[symbol], acLengths[symbol])
            writeBits(bitsOf(value, cat), cat)
            i++
        }
        if (last != 63) writeBits(acCodes[0x00], acLengths[0x00])
        return dc
    }

    /** La DCT veloce di Arai, Agui e Nakajima (la "float" di libjpeg), sul posto: righe, poi colonne. */
    private fun forwardDct(d: FloatArray) {
        for (pass in 0 until 2) {
            val step = if (pass == 0) 1 else 8
            val next = if (pass == 0) 8 else 1
            var base = 0
            repeat(8) {
                val i0 = base
                val i1 = base + step
                val i2 = base + 2 * step
                val i3 = base + 3 * step
                val i4 = base + 4 * step
                val i5 = base + 5 * step
                val i6 = base + 6 * step
                val i7 = base + 7 * step
                val tmp0 = d[i0] + d[i7]
                val tmp7 = d[i0] - d[i7]
                val tmp1 = d[i1] + d[i6]
                val tmp6 = d[i1] - d[i6]
                val tmp2 = d[i2] + d[i5]
                val tmp5 = d[i2] - d[i5]
                val tmp3 = d[i3] + d[i4]
                val tmp4 = d[i3] - d[i4]

                var tmp10 = tmp0 + tmp3
                val tmp13 = tmp0 - tmp3
                var tmp11 = tmp1 + tmp2
                var tmp12 = tmp1 - tmp2
                d[i0] = tmp10 + tmp11
                d[i4] = tmp10 - tmp11
                val z1 = (tmp12 + tmp13) * 0.707106781f
                d[i2] = tmp13 + z1
                d[i6] = tmp13 - z1

                tmp10 = tmp4 + tmp5
                tmp11 = tmp5 + tmp6
                tmp12 = tmp6 + tmp7
                val z5 = (tmp10 - tmp12) * 0.382683433f
                val z2 = 0.541196100f * tmp10 + z5
                val z4 = 1.306562965f * tmp12 + z5
                val z3 = tmp11 * 0.707106781f
                val z11 = tmp7 + z3
                val z13 = tmp7 - z3
                d[i5] = z13 + z2
                d[i3] = z13 - z2
                d[i1] = z11 + z4
                d[i7] = z11 - z4
                base += next
            }
        }
    }

    private fun writeBits(code: Int, length: Int) {
        var remaining = length
        while (remaining > 0) {
            val take = minOf(remaining, 8 - bitCount)
            val bits = (code shr (remaining - take)) and ((1 shl take) - 1)
            bitBuffer = (bitBuffer shl take) or bits
            bitCount += take
            remaining -= take
            if (bitCount == 8) {
                out.write(bitBuffer)
                // Un byte 0xFF nei dati si scrive seguito da uno zero, per non sembrare un marcatore.
                if (bitBuffer == 0xFF) out.write(0)
                bitBuffer = 0
                bitCount = 0
            }
        }
    }

    private fun writeMarker(marker: Int) {
        out.write(0xFF)
        out.write(marker)
    }

    private fun writeShort(value: Int) {
        out.write((value shr 8) and 0xFF)
        out.write(value and 0xFF)
    }

    companion object {
        /**
         * Il lato più lungo che si scrive: 65500, il limite di libjpeg, che legge i JPEG per Skia
         * (l'app sul PC), Android e Java. Fino al 25/11/2026 era 65535, quello del formato: la foto
         * di una pagina molto lunga finiva alta 65535 e non si apriva da nessuna parte.
         */
        const val MAX_SIDE = 65500
        const val DEFAULT_QUALITY = 88

        /** Quanti bit servono per un valore: la "categoria" del JPEG. */
        private fun category(value: Int): Int {
            var a = abs(value)
            var n = 0
            while (a > 0) {
                n++
                a = a shr 1
            }
            return n
        }

        /** I bit di un valore nella sua categoria: i negativi in complemento a uno. */
        private fun bitsOf(value: Int, category: Int): Int =
            if (value >= 0) value else value + (1 shl category) - 1

        private val ZIGZAG = intArrayOf(
            0, 1, 5, 6, 14, 15, 27, 28,
            2, 4, 7, 13, 16, 26, 29, 42,
            3, 8, 12, 17, 25, 30, 41, 43,
            9, 11, 18, 24, 31, 40, 44, 53,
            10, 19, 23, 32, 39, 45, 52, 54,
            20, 22, 33, 38, 46, 51, 55, 60,
            21, 34, 37, 47, 50, 56, 59, 61,
            35, 36, 48, 49, 57, 58, 62, 63
        )

        private val AASF = doubleArrayOf(
            1.0, 1.387039845, 1.306562965, 1.175875602, 1.0, 0.785694958, 0.541196100, 0.275899379
        )

        private val Y_QUANT = intArrayOf(
            16, 11, 10, 16, 24, 40, 51, 61,
            12, 12, 14, 19, 26, 58, 60, 55,
            14, 13, 16, 24, 40, 57, 69, 56,
            14, 17, 22, 29, 51, 87, 80, 62,
            18, 22, 37, 56, 68, 109, 103, 77,
            24, 35, 55, 64, 81, 104, 113, 92,
            49, 64, 78, 87, 103, 121, 120, 101,
            72, 92, 95, 98, 112, 100, 103, 99
        )

        private val UV_QUANT = intArrayOf(
            17, 18, 24, 47, 99, 99, 99, 99,
            18, 21, 26, 66, 99, 99, 99, 99,
            24, 26, 56, 99, 99, 99, 99, 99,
            47, 66, 99, 99, 99, 99, 99, 99,
            99, 99, 99, 99, 99, 99, 99, 99,
            99, 99, 99, 99, 99, 99, 99, 99,
            99, 99, 99, 99, 99, 99, 99, 99,
            99, 99, 99, 99, 99, 99, 99, 99
        )

        // Le tabelle di Huffman dell'allegato K dello standard: per ogni lunghezza (1–16) quanti codici, e i simboli.
        private val DC_Y_COUNTS = intArrayOf(0, 0, 1, 5, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0)
        private val DC_UV_COUNTS = intArrayOf(0, 0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0)
        private val DC_VALUES = IntArray(12) { it }
        private val AC_Y_COUNTS = intArrayOf(0, 0, 2, 1, 3, 3, 2, 4, 3, 5, 5, 4, 4, 0, 0, 1, 0x7d)
        private val AC_Y_VALUES = intArrayOf(
            0x01, 0x02, 0x03, 0x00, 0x04, 0x11, 0x05, 0x12, 0x21, 0x31, 0x41, 0x06, 0x13, 0x51, 0x61, 0x07,
            0x22, 0x71, 0x14, 0x32, 0x81, 0x91, 0xa1, 0x08, 0x23, 0x42, 0xb1, 0xc1, 0x15, 0x52, 0xd1, 0xf0,
            0x24, 0x33, 0x62, 0x72, 0x82, 0x09, 0x0a, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x25, 0x26, 0x27, 0x28,
            0x29, 0x2a, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49,
            0x4a, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5a, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69,
            0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7a, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89,
            0x8a, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98, 0x99, 0x9a, 0xa2, 0xa3, 0xa4, 0xa5, 0xa6, 0xa7,
            0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6, 0xb7, 0xb8, 0xb9, 0xba, 0xc2, 0xc3, 0xc4, 0xc5,
            0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xd2, 0xd3, 0xd4, 0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda, 0xe1, 0xe2,
            0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9, 0xea, 0xf1, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8,
            0xf9, 0xfa
        )
        private val AC_UV_COUNTS = intArrayOf(0, 0, 2, 1, 2, 4, 4, 3, 4, 7, 5, 4, 4, 0, 1, 2, 0x77)
        private val AC_UV_VALUES = intArrayOf(
            0x00, 0x01, 0x02, 0x03, 0x11, 0x04, 0x05, 0x21, 0x31, 0x06, 0x12, 0x41, 0x51, 0x07, 0x61, 0x71,
            0x13, 0x22, 0x32, 0x81, 0x08, 0x14, 0x42, 0x91, 0xa1, 0xb1, 0xc1, 0x09, 0x23, 0x33, 0x52, 0xf0,
            0x15, 0x62, 0x72, 0xd1, 0x0a, 0x16, 0x24, 0x34, 0xe1, 0x25, 0xf1, 0x17, 0x18, 0x19, 0x1a, 0x26,
            0x27, 0x28, 0x29, 0x2a, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48,
            0x49, 0x4a, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5a, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68,
            0x69, 0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7a, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87,
            0x88, 0x89, 0x8a, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98, 0x99, 0x9a, 0xa2, 0xa3, 0xa4, 0xa5,
            0xa6, 0xa7, 0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6, 0xb7, 0xb8, 0xb9, 0xba, 0xc2, 0xc3,
            0xc4, 0xc5, 0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xd2, 0xd3, 0xd4, 0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda,
            0xe2, 0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9, 0xea, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8,
            0xf9, 0xfa
        )

        private fun codes(counts: IntArray, values: IntArray): Pair<IntArray, IntArray> {
            val codes = IntArray(256)
            val lengths = IntArray(256)
            var code = 0
            var pos = 0
            for (length in 1..16) {
                repeat(counts[length]) {
                    codes[values[pos]] = code
                    lengths[values[pos]] = length
                    pos++
                    code++
                }
                code = code shl 1
            }
            return codes to lengths
        }

        private val DC_Y = codes(DC_Y_COUNTS, DC_VALUES)
        private val DC_UV = codes(DC_UV_COUNTS, DC_VALUES)
        private val AC_Y = codes(AC_Y_COUNTS, AC_Y_VALUES)
        private val AC_UV = codes(AC_UV_COUNTS, AC_UV_VALUES)
        private val DC_Y_CODES = DC_Y.first
        private val DC_Y_LENGTHS = DC_Y.second
        private val DC_UV_CODES = DC_UV.first
        private val DC_UV_LENGTHS = DC_UV.second
        private val AC_Y_CODES = AC_Y.first
        private val AC_Y_LENGTHS = AC_Y.second
        private val AC_UV_CODES = AC_UV.first
        private val AC_UV_LENGTHS = AC_UV.second
    }
}
