package com.gabriele.notionlocal.desktop

import org.jetbrains.skia.Bitmap

/**
 * **Il numero di oggi nel calendario: quello cerchiato** (dal 18/11/2026 oggi non è più colorato ma
 * ha un cerchio bianco attorno). Cercato sotto [fromY]: un punto chiaro a sinistra e uno a destra
 * alla stessa altezza, lontani un diametro, e i punti chiari in cima e in fondo a metà fra loro.
 * Il centro e il raggio del cerchio, o null.
 */
fun todayRing(bitmap: Bitmap, fromY: Int): Triple<Int, Int, Int>? {
    fun bright(x: Int, y: Int) = x in 0 until bitmap.width && y in 0 until bitmap.height &&
        bitmap.getColor(x, y).let { c -> ((c shr 16) and 0xFF) > 110 && ((c shr 8) and 0xFF) > 110 && (c and 0xFF) > 110 }
    fun near(x: Int, y: Int) = (-1..1).any { dx -> (-1..1).any { dy -> bright(x + dx, y + dy) } }
    for (y in fromY until bitmap.height) {
        for (x in 0 until bitmap.width) {
            if (!bright(x, y)) continue
            for (diameter in 16..26) {
                val r = diameter / 2
                val cx = x + r
                // I lati, la cima e il fondo; e dentro, a metà raggio sopra e sotto il centro, niente (non un numero pieno).
                if (near(x + diameter, y) && near(cx, y - r) && near(cx, y + r) &&
                    !bright(cx - r + 2, y - r + 2) && !bright(cx + r - 2, y + r - 2)
                ) {
                    // La cima e il fondo veri del cerchio, sulla sua colonna di mezzo.
                    val column = (y - r - 4..y + r + 4).filter { yy -> (-1..1).any { dx -> bright(cx + dx, yy) } }
                    val top = column.first()
                    val bottom = column.last()
                    return Triple(cx, (top + bottom) / 2, (bottom - top) / 2 + 1)
                }
            }
        }
    }
    return null
}
