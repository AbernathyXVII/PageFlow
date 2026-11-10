package com.gabriele.notionlocal.data.webclip

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * **Le immagini di "Text and Images"** (dal 20/10/2026), scaricate come le
 * scarica il browser: lo stesso "user agent" da PC del Web Clipper, la pagina
 * da cui arrivano (certi siti le danno solo a chi viene da loro) e i cookie
 * del sito, così un sito in cui si è entrati col proprio account dà anche le
 * sue immagini.
 */
object WebClipDownloads {

    private const val MAX_IMAGE_BYTES = 25 * 1024 * 1024
    private const val TIMEOUT_MS = 15_000

    /** I byte dell'immagine, o null se non arriva, è troppo grande o non è un'immagine. */
    suspend fun image(url: String, referer: String?): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = true
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.setRequestProperty("User-Agent", WEB_CLIP_USER_AGENT)
                connection.setRequestProperty("Accept", "image/avif,image/webp,image/png,image/jpeg,image/*;q=0.8")
                referer?.let { connection.setRequestProperty("Referer", it) }
                webClipCookies(url)?.let {
                    connection.setRequestProperty("Cookie", it)
                }
                if (connection.responseCode !in 200..299) return@runCatching null
                val type = connection.contentType.orEmpty()
                if (type.isNotEmpty() && !type.startsWith("image/") && !type.startsWith("application/octet-stream")) return@runCatching null
                val out = ByteArrayOutputStream()
                connection.inputStream.use { input ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        out.write(buffer, 0, read)
                        if (out.size() > MAX_IMAGE_BYTES) return@runCatching null
                    }
                }
                out.toByteArray()
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }
}

/** I cookie del browser del Web Clipper per [url] (sul telefono quelli del WebView), se ce ne sono. */
expect fun webClipCookies(url: String): String?
