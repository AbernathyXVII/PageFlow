package com.gabriele.notionlocal.data

import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.net.URLEncoder
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Quello che un sito dice di sé: il titolo, la descrizione e l'immagine di anteprima. */
data class LinkPreview(val title: String?, val description: String?, val imageUrl: String?)

/**
 * **Il titolo di una pagina web** (dal 27/10/2026), da mostrare al posto del
 * suo link: nel testo e nelle celle URL ("Page title", vedi
 * `LinkShowChoices`). È quello dell'anteprima dei segnalibri; null senza rete.
 * Nei test si sostituisce [source], che se no andrebbe in rete.
 */
object LinkTitles {
    /** I titoli già letti in questa sessione: lo stesso link in più celle, o riletto, non torna in rete. */
    private val known = ConcurrentHashMap<String, String>()

    @Volatile
    var source: suspend (String) -> String? = { url -> LinkPreviews.fetch(url)?.title }
        set(value) {
            field = value
            known.clear()
        }

    /** Il titolo della pagina all'indirizzo [url], su una riga sola; null se non c'è. */
    suspend fun read(url: String): String? =
        known[url] ?: source(url)?.replace(Regex("\\s+"), " ")?.trim()?.takeIf { it.isNotEmpty() }?.also { known[url] = it }
}

/**
 * **L'anteprima di un segnalibro** (dal 06/10/2026, "Web bookmark"): si legge
 * l'inizio della pagina del sito e se ne prendono il titolo, la descrizione
 * e l'immagine che il sito stesso indica per le anteprime (i tag "og:" che
 * usano anche Notion, WhatsApp e Telegram). Serve la rete; senza, il
 * segnalibro mostra solo l'indirizzo.
 */
object LinkPreviews {

    private const val MAX_PAGE_BYTES = 512 * 1024
    private const val MAX_IMAGE_BYTES = 5 * 1024 * 1024
    private const val TIMEOUT_MS = 6_000
    private const val MAX_REDIRECTS = 5
    /** Una seconda volta se la rete non ha risposto: la prima, sul telefono, può essere ancora addormentata. */
    private const val ATTEMPTS = 2

    // Molti siti danno le anteprime solo a chi sembra un browser.
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0 Mobile Safari/537.36"

    /**
     * Legge il sito e ne prende l'anteprima; null senza rete o se il sito non
     * risponde. **Dal 13/11/2026** (l'utente: il titolo ci metteva tantissimo,
     * e la prima volta "No title found"): si legge solo fino alla fine della
     * `<head>`, dove stanno titolo e anteprime, invece di mezzo MB di pagina; i
     * rimandi da http a https si seguono; se la rete non risponde si riprova
     * una volta; i video di YouTube chiedono il titolo a YouTube stesso.
     */
    suspend fun fetch(url: String): LinkPreview? = withContext(Dispatchers.IO) {
        for (attempt in 1..ATTEMPTS) {
            try {
                youTubeTitle(url)?.let { return@withContext LinkPreview(it, null, null) }
                val (bytes, finalUrl, encoding) = download(url, MAX_PAGE_BYTES, acceptHtml = true) ?: return@withContext null
                val declared = encoding ?: declaredCharset(bytes)
                val text = runCatching { String(bytes, charset(declared ?: "UTF-8")) }.getOrElse { String(bytes, Charsets.UTF_8) }
                return@withContext parse(text, finalUrl)
            } catch (e: IOException) {
                if (attempt == ATTEMPTS) return@withContext null
            } catch (e: Exception) {
                return@withContext null
            }
        }
        null
    }

    private val YOUTUBE = Regex("^https?://(www\\.|m\\.|music\\.)?(youtube\\.com|youtu\\.be)/", RegexOption.IGNORE_CASE)
    private val JSON_TITLE = Regex("\"title\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")

    /**
     * Il titolo di un video di YouTube dal suo oEmbed: la pagina, in Europa,
     * risponde con la richiesta dei cookie, e il titolo sarebbe quello. Null
     * se non è YouTube o se lì non c'è (allora si legge la pagina).
     */
    private fun youTubeTitle(url: String): String? {
        if (!YOUTUBE.containsMatchIn(url)) return null
        val oembed = "https://www.youtube.com/oembed?format=json&url=" + URLEncoder.encode(url, "UTF-8")
        val (bytes, _, _) = download(oembed, 64 * 1024, acceptHtml = false) ?: return null
        val raw = JSON_TITLE.find(String(bytes, Charsets.UTF_8))?.groupValues?.get(1) ?: return null
        return runCatching { Json.decodeFromString<String>("\"$raw\"") }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
    }

    /** La codifica scritta nella pagina (`<meta charset>`), quando il server non la dice. */
    private fun declaredCharset(bytes: ByteArray): String? =
        Regex("charset\\s*=\\s*[\"']?([-\\w.:]+)", RegexOption.IGNORE_CASE)
            .find(String(bytes, 0, minOf(bytes.size, 4096), Charsets.ISO_8859_1))?.groupValues?.get(1)

    /** Scarica l'immagine di anteprima, se non è troppo grande. */
    suspend fun downloadImage(url: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching { download(url, MAX_IMAGE_BYTES, acceptHtml = false)?.first }.getOrNull()
    }

    /**
     * I byte (al massimo `limit`), l'indirizzo dopo i rimandi, e la codifica
     * dichiarata. Di una pagina ([acceptHtml]) solo fino alla fine della
     * `<head>`. I rimandi si seguono a mano: quelli da http a https (e il
     * contrario) `HttpURLConnection` non li segue, e la pagina restava vuota.
     */
    internal fun download(url: String, limit: Int, acceptHtml: Boolean): Triple<ByteArray, String, String?>? {
        var address = url
        repeat(MAX_REDIRECTS + 1) {
            val connection = URL(address).openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.setRequestProperty("User-Agent", USER_AGENT)
                if (acceptHtml) connection.setRequestProperty("Accept", "text/html,application/xhtml+xml")
                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location") ?: return null
                    address = URL(URL(address), location).toString()
                    return@repeat
                }
                if (code !in 200..299) return null
                val charset = connection.contentType?.substringAfter("charset=", "")?.substringBefore(';')?.trim()?.takeIf { it.isNotEmpty() }
                val out = ByteArrayOutputStream()
                connection.inputStream.use { input ->
                    val buffer = ByteArray(16 * 1024)
                    while (out.size() < limit) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        out.write(buffer, 0, read)
                        if (acceptHtml && endsHead(buffer, read)) break
                    }
                }
                if (!acceptHtml && out.size() >= limit) return null
                return Triple(out.toByteArray(), address, charset)
            } finally {
                connection.disconnect()
            }
        }
        return null
    }

    /** In questo pezzo finisce la `<head>` (o comincia il `<body>`): il resto della pagina non serve. */
    private fun endsHead(buffer: ByteArray, length: Int): Boolean {
        val text = String(buffer, 0, length, Charsets.ISO_8859_1).lowercase()
        return "</head" in text || "<body" in text
    }

    private val META_TAG = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val ATTRIBUTE = Regex("([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*(\"([^\"]*)\"|'([^']*)')")
    private val TITLE_TAG = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

    /** Titolo, descrizione e immagine dall'HTML di una pagina; `pageUrl` serve per gli indirizzi relativi. */
    fun parse(html: String, pageUrl: String): LinkPreview {
        val metas = META_TAG.findAll(html).map { tag ->
            ATTRIBUTE.findAll(tag.value).associate { m ->
                m.groupValues[1].lowercase() to (m.groups[3]?.value ?: m.groups[4]?.value.orEmpty())
            }
        }.toList()
        fun meta(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
            metas.firstOrNull { (it["property"] ?: it["name"])?.lowercase() == key }?.get("content")
        }?.let { decodeEntities(it).trim() }?.takeIf { it.isNotEmpty() }

        val title = meta("og:title", "twitter:title")
            ?: TITLE_TAG.find(html)?.groupValues?.get(1)?.let { decodeEntities(it).replace(Regex("\\s+"), " ").trim() }?.takeIf { it.isNotEmpty() }
            // Una pagina fatta dal JavaScript ha il titolo vuoto: almeno il nome del sito.
            ?: meta("og:site_name", "application-name")
        val description = meta("og:description", "twitter:description", "description")
        val image = meta("og:image", "og:image:url", "twitter:image")?.let { resolve(pageUrl, it) }
        return LinkPreview(title, description, image)
    }

    private fun resolve(base: String, link: String): String? =
        runCatching { URL(URL(base), link).toString() }.getOrNull()

    private fun decodeEntities(text: String): String =
        text.replace(Regex("&#(x?)([0-9a-fA-F]+);")) { m ->
            val code = m.groupValues[2].toIntOrNull(if (m.groupValues[1].isEmpty()) 10 else 16)
            code?.let { String(Character.toChars(it)) } ?: m.value
        }
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
}
