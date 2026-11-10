package com.gabriele.notionlocal.data.webclip

import com.gabriele.notionlocal.data.entity.RichTextSpan
import java.io.File
import java.net.URI

/** Quello che si è scelto nel pannello del Web Clipper; `dark` = "Dark mode", per le foto. */
data class WebClipRequest(val url: String, val mode: WebClipMode, val title: String, val note: String, val dark: Boolean = false)

/** Le scelte che fanno una foto, e quindi possono essere scure. */
val WebClipMode.isPicture: Boolean
    get() = this == WebClipMode.FULL_PAGE || this == WebClipMode.REGION || this == WebClipMode.ARTICLE

/** Il grigio della riga "Clipped from", come il testo secondario di Notion. */
const val CLIP_SOURCE_COLOR = "#9B9A97"

/**
 * **Il link scritto o incollato nel Web Clipper**, pronto da aprire: senza
 * spazi ai lati, con "https://" davanti se manca ("game8.co/…" va bene). Null
 * se non è l'indirizzo di un sito: serve un nome con un punto, e solo
 * http o https.
 */
fun webClipUrl(input: String): String? {
    val text = input.trim().takeIf { it.isNotEmpty() && it.none { c -> c.isWhitespace() } } ?: return null
    val withScheme = if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://").containsMatchIn(text)) text else "https://$text"
    val uri = runCatching { URI(withScheme) }.getOrNull() ?: return null
    if (uri.scheme?.lowercase() !in setOf("http", "https")) return null
    val host = uri.host ?: return null
    if (!host.contains('.') || host.startsWith('.') || host.endsWith('.')) return null
    return withScheme
}

/** Il nome del sito di un link ("game8.co"), per chiamare la foto quando non ha un titolo. */
fun webClipHost(url: String): String = runCatching { URI(url).host }.getOrNull()?.removePrefix("www.") ?: url

/**
 * **Le righe sotto la foto** (dal 20/10/2026): il titolo in grassetto, la
 * nota (una riga per ogni sua riga), e "Clipped from" col link, piccolo e
 * grigio, come OneNote. Quelle vuote non ci sono.
 */
fun webClipCaptions(
    title: String?,
    note: String?,
    url: String?,
    clippedFrom: String,
    /** Quanto è grande il titolo, in punti; null = come il testo. "Text and Images" lo vuole grande, come OneNote. */
    titleSize: Int? = null
): List<List<RichTextSpan>> {
    val lines = mutableListOf<List<RichTextSpan>>()
    title?.trim()?.takeIf { it.isNotEmpty() }?.let { lines += listOf(RichTextSpan(text = it.replace('\n', ' '), bold = true, size = titleSize)) }
    note?.lines()?.map { it.trim() }?.filter { it.isNotEmpty() }?.forEach { lines += listOf(RichTextSpan(text = it)) }
    url?.takeIf { it.isNotBlank() }?.let {
        lines += listOf(
            RichTextSpan(text = "$clippedFrom ", color = CLIP_SOURCE_COLOR, size = CLIP_SOURCE_SIZE),
            RichTextSpan(text = it, link = it, size = CLIP_SOURCE_SIZE)
        )
    }
    return lines
}

/** La grandezza della riga "Clipped from", in punti: più piccola del testo normale (16, vedi `RichTextSpan.size`). */
const val CLIP_SOURCE_SIZE = 13

/** Una foto fatta: il file e le sue misure in pixel. */
data class WebClipImage(val file: File, val width: Int, val height: Int)

/** Quanto è larga la pagina fotografata, in pixel CSS: 1440, come un portatile (è la larghezza a cui game8 viene come nella foto di OneNote). */
const val WEB_CLIP_CSS_WIDTH = 1440

/** Quanto è alta la finestra da cui la si guarda, in pixel CSS. */
const val WEB_CLIP_CSS_HEIGHT = 900

/** "Article" più stretto, come una colonna da leggere. */
const val WEB_CLIP_ARTICLE_CSS_WIDTH = 860

/** Pixel veri per pixel CSS nella foto: 1,5 come lo schermo di un portatile, nitida ingrandita. */
const val WEB_CLIP_OUTPUT_SCALE = 1.5f

/** Lo script che toglie banner, barre fisse e pubblicità (`assets/web_clipper_cleanup.js`). */
const val WEB_CLIP_CLEANUP_SCRIPT = "web_clipper_cleanup.js"

/** Lo script che trova l'articolo (`assets/web_clipper_reader.js`). */
const val WEB_CLIP_READER_SCRIPT = "web_clipper_reader.js"

/** Chrome per Windows: i siti mandano la loro pagina per il PC. */
const val WEB_CLIP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"

/** Le immagini che si caricano solo scorrendo ("lazy"), caricate subito. */
const val WEB_CLIP_EAGER_IMAGES_SCRIPT = """
(function () {
  document.querySelectorAll('img[loading="lazy"]').forEach(function (i) { i.loading = 'eager'; });
  document.querySelectorAll('img[data-src]').forEach(function (i) {
    if (!i.getAttribute('src') || i.getAttribute('src').indexOf('data:') === 0) i.src = i.getAttribute('data-src');
  });
})()
"""
