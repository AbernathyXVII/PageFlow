package com.gabriele.notionlocal.platform

import androidx.compose.ui.graphics.ImageBitmap
import java.io.File

/**
 * **La cartella delle cose che si possono rifare** (stemmi scaricati, file
 * esportati…; dal 04/11/2026): sul telefono la `cacheDir` dell'app, che
 * Android può svuotare; sul PC la cartella `cache` fra i dati di PageFlow.
 */
expect fun cacheDirOf(context: PlatformContext): File

/** La cache sulla memoria condivisa, che solo il telefono ha (`externalCacheDir`); sul PC null. */
expect fun externalCacheDirOf(context: PlatformContext): File?

/** **Un'immagine dai suoi byte** (PNG, JPEG…), da disegnare; null se non si legge. */
expect fun decodeImageBitmap(bytes: ByteArray): ImageBitmap?
