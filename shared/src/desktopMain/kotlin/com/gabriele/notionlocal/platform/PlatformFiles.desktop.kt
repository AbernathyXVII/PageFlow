package com.gabriele.notionlocal.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.io.File

actual fun cacheDirOf(context: PlatformContext): File {
    val base = (context as? DesktopContext)?.dataDir ?: File(System.getProperty("java.io.tmpdir"), "PageFlow")
    return File(base, "cache").apply { mkdirs() }
}

actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? =
    runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()

actual fun externalCacheDirOf(context: PlatformContext): File? = null
