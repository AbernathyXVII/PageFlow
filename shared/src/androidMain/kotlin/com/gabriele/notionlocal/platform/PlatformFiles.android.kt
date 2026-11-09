package com.gabriele.notionlocal.platform

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

actual fun cacheDirOf(context: PlatformContext): File = context.cacheDir

actual fun externalCacheDirOf(context: PlatformContext): File? = context.externalCacheDir

actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? =
    runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()
