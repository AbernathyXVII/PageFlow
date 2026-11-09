package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.io.File

/**
 * **Le immagini molto alte** (dal 20/10/2026, per le foto del Web Clipper:
 * una pagina di game8 è 2160 × 19000 pixel). Caricate intere sarebbero
 * centocinquanta megabyte, e rimpicciolite fino a starci (come le altre
 * immagini, vedi `PageImageStore.load`) il testo diventerebbe illeggibile.
 * Qui si leggono **a pezzi** dal file (`BitmapRegionDecoder`), solo quelli
 * sullo schermo, alla risoluzione che serve.
 */
fun isTallImage(width: Int, height: Int): Boolean =
    height > TALL_MIN_HEIGHT && height > width * TALL_MIN_RATIO

/**
 * Solo le immagini davvero lunghe e strette: le foto della fotocamera, anche
 * da dodici megapixel, si leggono intere rimpicciolite come sempre (le strisce
 * non sanno girare una foto scattata in verticale).
 */
internal const val TALL_MIN_HEIGHT = 4096

internal const val TALL_MIN_RATIO = 2.5f

/** Di quanto rimpicciolire leggendo: la potenza di 2 più grande che lascia almeno un pixel letto per pixel mostrato. */
fun sampleFor(pixelsShownPerImagePixel: Float): Int {
    var sample = 1
    while (pixelsShownPerImagePixel * sample * 2 <= 1f && sample < 64) sample *= 2
    return sample
}

/**
 * **Un'immagine alta nella pagina**: larga quanto il suo riquadro, alta
 * quanto vuole la sua forma. Sul telefono disegnata a strisce, lette dal file
 * solo quando servono (`TallImage.kt`); sul PC, per ora, letta intera
 * rimpicciolita.
 */
@Composable
expect fun StripImage(file: File, imageWidth: Int, imageHeight: Int, description: String?, modifier: Modifier = Modifier)

/**
 * **Un'immagine alta a tutto schermo**, da scorrere e ingrandire; la X la
 * chiude, accanto c'è lo scarica ([onDownload]).
 */
@Composable
expect fun TallImageViewer(
    file: File,
    imageWidth: Int,
    imageHeight: Int,
    title: String,
    onDismiss: () -> Unit,
    onDownload: (() -> Unit)? = null
)
