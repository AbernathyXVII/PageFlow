package com.gabriele.notionlocal.data

import com.gabriele.notionlocal.platform.PlatformContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File

private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif")

actual object BlockFiles {

    /** Dai primi byte del file: JPEG, PNG, GIF, WebP, BMP, HEIC. */
    actual fun imageMime(store: PageImageStore, fileName: String): String? = runCatching {
        val head = ByteArray(16)
        val read = store.fileFor(fileName).inputStream().use { it.read(head) }
        if (read < 4) return@runCatching null
        fun at(i: Int) = head[i].toInt() and 0xFF
        fun text(from: Int, length: Int) = String(head, from, length, Charsets.ISO_8859_1)
        when {
            at(0) == 0xFF && at(1) == 0xD8 -> "image/jpeg"
            at(0) == 0x89 && text(1, 3) == "PNG" -> "image/png"
            text(0, 3) == "GIF" -> "image/gif"
            read >= 12 && text(0, 4) == "RIFF" && text(8, 4) == "WEBP" -> "image/webp"
            text(0, 2) == "BM" -> "image/bmp"
            read >= 12 && text(4, 4) == "ftyp" && text(8, 4).startsWith("hei") -> "image/heic"
            else -> null
        }
    }.getOrNull()

    actual fun extensionFor(mime: String?): String? = when (mime?.lowercase()) {
        null -> null
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/gif" -> "gif"
        "image/webp" -> "webp"
        "image/bmp" -> "bmp"
        "image/heic" -> "heic"
        "video/mp4" -> "mp4"
        "video/quicktime" -> "mov"
        "video/webm" -> "webm"
        "video/x-matroska" -> "mkv"
        "audio/mpeg" -> "mp3"
        "audio/mp4" -> "m4a"
        "audio/wav", "audio/x-wav" -> "wav"
        "audio/ogg" -> "ogg"
        "audio/flac" -> "flac"
        "application/pdf" -> "pdf"
        "application/zip" -> "zip"
        "text/plain" -> "txt"
        else -> null
    }

    actual fun openWithAnotherApp(context: PlatformContext, store: PageImageStore, fileName: String, mime: String?): Boolean =
        runCatching { Desktop.getDesktop().open(store.fileFor(fileName)) }.isSuccess

    actual fun openUrl(context: PlatformContext, url: String): Boolean = com.gabriele.notionlocal.platform.openUrl(context, url)

    actual suspend fun saveImageToGallery(context: PlatformContext, store: PageImageStore, fileName: String, displayName: String?): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val folder = File(File(System.getProperty("user.home"), "Pictures"), "PageFlow").apply { mkdirs() }
                val extension = extensionFor(imageMime(store, fileName)) ?: "jpg"
                // Il nome può venire da una pagina web (l'alt del Web Clipper): solo l'ultimo pezzo,
                // senza i caratteri che Windows vieta né i punti finali, e con l'estensione vera.
                val base = displayName?.substringAfterLast('/')?.substringAfterLast('\\')
                    ?.replace(Regex("[<>:\"|?*\\x00-\\x1F]"), "_")?.trim()?.trimEnd('.')?.takeIf { it.isNotBlank() }
                val name = when {
                    base == null -> "PageFlow-${System.currentTimeMillis()}.$extension"
                    base.substringAfterLast('.', "").lowercase() in IMAGE_EXTENSIONS -> base
                    else -> "$base.$extension"
                }
                var target = File(folder, name)
                var copy = 1
                while (target.exists()) target = File(folder, name.substringBeforeLast('.') + " ($copy)." + name.substringAfterLast('.')).also { copy++ }
                store.fileFor(fileName).copyTo(target)
                true
            }.getOrDefault(false)
        }
}
