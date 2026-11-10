package com.gabriele.notionlocal.data.webclip

import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.PlatformContext
import com.gabriele.notionlocal.platform.readAppAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import java.awt.Rectangle
import java.io.ByteArrayInputStream
import java.io.File
import java.util.Base64
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * **Il motore del Web Clipper sul PC** (dal 10/11/2026): come quello del
 * telefono (`WebClipEngine`), ma con Edge nascosto (`EdgeBrowser`) al posto del
 * WebView. Il sito si apre come su un portatile, largo [cssWidth] pixel, con
 * lo stesso "user agent" e gli stessi script del telefono (banner via,
 * immagini caricate scorrendo, l'articolo); la foto si fa a strisce
 * direttamente dal browser, che sa fotografare anche sotto lo schermo.
 */
class DesktopWebClipEngine(
    private val context: PlatformContext,
    val cssWidth: Int = WEB_CLIP_CSS_WIDTH,
    private val dark: Boolean = false
) : AutoCloseable {
    private var browser: EdgeBrowser? = null

    /** Il titolo della pagina, dopo [load]. */
    var pageTitle: String? = null
        private set

    /** Apre l'indirizzo come su un portatile e aspetta che la pagina sia pronta; l'indirizzo finale, o null. */
    suspend fun load(url: String): String? {
        val profile = File((context as DesktopContext).dataDir, "webclip-browser")
        val edge = EdgeBrowser.launch(profile) ?: return null
        browser = edge
        edge.send("Page.enable")
        edge.send("Network.enable")
        edge.send("Network.setUserAgentOverride", jsonOf("userAgent" to WEB_CLIP_USER_AGENT))
        edge.send(
            "Emulation.setDeviceMetricsOverride",
            jsonOf("width" to cssWidth, "height" to WEB_CLIP_CSS_HEIGHT, "deviceScaleFactor" to 1, "mobile" to false)
        )
        // "Dark mode": il tema scuro del sito, e quelli che non ce l'hanno scuriti dal browser.
        edge.send("Emulation.setEmulatedMedia", jsonOf("features" to listOf(mapOf("name" to "prefers-color-scheme", "value" to if (dark) "dark" else "light"))))
        if (dark) runCatching { edge.send("Emulation.setAutoDarkModeOverride", jsonOf("enabled" to true)) }
        val loaded = edge.send("Page.navigate", jsonOf("url" to url))
        if (loaded["errorText"] != null) return null
        edge.awaitEvent("Page.loadEventFired", LOAD_TIMEOUT_MS)
        // Dopo "caricata" i siti continuano: immagini, caratteri, script.
        withTimeoutOrNull(SETTLE_TIMEOUT_MS) {
            while (string("document.readyState") != "complete") delay(200)
        }
        delay(SETTLE_DELAY_MS)
        pageTitle = string("document.title")?.trim()?.takeIf { it.isNotEmpty() }
        return string("location.href")
    }

    /** La pagina pronta per la foto: senza banner, con le immagini caricate scorrendo, e in cima. */
    suspend fun prepare() {
        cleanUp()
        loadLazyContent()
        // Certi banner arrivano dopo, e solo ora si chiudono i riquadri rimasti vuoti (la pubblicità).
        cleanUp(collapseEmpty = true)
        delay(300)
    }

    /** Lo script che toglie banner, barre fisse e pubblicità (lo stesso del telefono). */
    suspend fun cleanUp(collapseEmpty: Boolean = false) {
        evaluate("window.__noteCollapseEmpty = $collapseEmpty")
        evaluate(asset(WEB_CLIP_CLEANUP_SCRIPT))
    }

    /** Fa scorrere la pagina fino in fondo, una finestra alla volta, perché arrivino le immagini; poi torna in cima. */
    suspend fun loadLazyContent() {
        evaluate(WEB_CLIP_EAGER_IMAGES_SCRIPT)
        var steps = 0
        while (steps < MAX_LAZY_STEPS) {
            val state = browser?.evaluate(
                "(function(){window.scrollBy(0, Math.round(window.innerHeight*0.85));" +
                    "return [window.scrollY, window.innerHeight, Math.max(document.documentElement.scrollHeight, document.body ? document.body.scrollHeight : 0)];})()"
            ) as? JsonArray ?: break
            delay(LAZY_STEP_DELAY_MS)
            val (y, h, total) = state.map { it.jsonPrimitive.double }
            if (y + h >= total - 2) break
            steps++
        }
        delay(LAZY_END_DELAY_MS)
        evaluate("window.scrollTo(0, 0)")
        delay(LAZY_END_DELAY_MS)
    }

    /** "Article": la pagina diventa solo il suo articolo (lo stesso script del telefono); il suo titolo, se lo trova. */
    suspend fun keepOnlyArticle(): String? {
        evaluate(asset(WEB_CLIP_READER_SCRIPT))
        val json = string("noteReader.render($dark)") ?: return null
        return parseClipContent(json)?.title?.takeIf { it.isNotEmpty() }
    }

    /** "Text and Images": l'articolo come blocchi in ordine; null se la pagina non ne ha uno. */
    suspend fun readContent(): ClipContent? {
        evaluate(asset(WEB_CLIP_READER_SCRIPT))
        return string("noteReader.serialize()")?.let(::parseClipContent)
    }

    /**
     * **La foto di tutta la pagina**, in [target]: a strisce, fotografate dal
     * browser anche sotto lo schermo, nel JPEG a strisce (`JpegStripWriter`).
     * Una pagina più alta di quanto sta in un JPEG si fotografa più piccola, ma intera.
     */
    suspend fun captureFullPage(target: File, scale: Float = PC_OUTPUT_SCALE, onProgress: (Float) -> Unit = {}): WebClipImage? {
        val edge = browser ?: return null
        val size = edge.send("Page.getLayoutMetrics")["cssContentSize"] as? JsonObject ?: return null
        val cssHeight = size["height"]!!.jsonPrimitive.double.coerceAtLeast(1.0)
        val outScale = min(scale.toDouble(), JpegStripWriter.MAX_SIDE / cssHeight)
        val outWidth = (cssWidth * outScale).roundToInt().coerceIn(1, JpegStripWriter.MAX_SIDE)
        val outHeight = (cssHeight * outScale).roundToInt().coerceIn(1, JpegStripWriter.MAX_SIDE)
        // La pagina disegnata davvero a quella densità, come su uno schermo fine: con "scale" nella
        // foto il browser la scattava a 1 e poi la ingrandiva, e si vedevano i pixel.
        edge.send(
            "Emulation.setDeviceMetricsOverride",
            jsonOf("width" to cssWidth, "height" to WEB_CLIP_CSS_HEIGHT, "deviceScaleFactor" to outScale, "mobile" to false)
        )
        withContext(Dispatchers.IO) {
            target.outputStream().use { stream ->
                val writer = JpegStripWriter(stream, outWidth, outHeight, quality = PC_JPEG_QUALITY)
                var row = 0
                var pixels = IntArray(0)
                while (row < outHeight) {
                    val fromCss = row / outScale
                    val stripCss = min(STRIP_CSS, cssHeight - fromCss).coerceAtLeast(1.0)
                    val shot = edge.send(
                        "Page.captureScreenshot",
                        jsonOf(
                            "format" to "png",
                            "captureBeyondViewport" to true,
                            "clip" to mapOf("x" to 0, "y" to fromCss, "width" to cssWidth, "height" to stripCss, "scale" to 1)
                        )
                    )
                    val png = Base64.getDecoder().decode(shot["data"]!!.jsonPrimitive.content)
                    val strip = ImageIO.read(ByteArrayInputStream(png)) ?: error("foto non letta")
                    val rows = min(strip.height, outHeight - row)
                    if (rows <= 0) break
                    if (pixels.size < outWidth * rows) pixels = IntArray(outWidth * rows)
                    // La striscia larga quanto la foto (se il browser l'ha fatta di un pixel diversa, il resto è bianco).
                    java.util.Arrays.fill(pixels, 0, outWidth * rows, if (dark) DARK_PAGE else WHITE)
                    strip.getRGB(0, 0, min(strip.width, outWidth), rows, pixels, 0, outWidth)
                    writer.writeRows(pixels, 0, rows)
                    row += rows
                    onProgress(row.toFloat() / outHeight)
                }
                writer.finish()
            }
        }
        return WebClipImage(target, outWidth, outHeight)
    }

    private suspend fun evaluate(script: String) {
        runCatching { browser?.evaluate(script) }
    }

    /** Un risultato di testo del JavaScript, o null. */
    private suspend fun string(script: String): String? =
        runCatching { (browser?.evaluate(script) as? JsonPrimitive)?.takeIf { it.isString }?.content }.getOrNull()

    private fun asset(name: String): String = readAppAsset(context, name).orEmpty()

    override fun close() {
        browser?.close()
        browser = null
    }

    companion object {
        private const val LOAD_TIMEOUT_MS = 30_000L
        private const val SETTLE_TIMEOUT_MS = 10_000L
        private const val SETTLE_DELAY_MS = 1_200L
        private const val MAX_LAZY_STEPS = 120
        private const val LAZY_STEP_DELAY_MS = 220L
        private const val LAZY_END_DELAY_MS = 700L

        /**
         * Pixel veri per pixel CSS nelle foto del PC: 2 (il telefono 1,5). Lo schermo del PC è più largo
         * e la pagina a tutta larghezza: a 1,5 la foto veniva allargata e non era nitida (10/11/2026).
         */
        const val PC_OUTPUT_SCALE = 2f

        /** La qualità del JPEG delle foto del PC: più alta di quella di serie, perché sono quasi tutte testo. */
        const val PC_JPEG_QUALITY = 94

        /** Quanto è alta una striscia della foto, in pixel CSS. */
        private const val STRIP_CSS = 1_500.0
        private const val WHITE = 0xFFFFFFFF.toInt()
        private const val DARK_PAGE = 0xFF191919.toInt()

        /**
         * **Il rettangolo [area] della foto [image]** (in pixel della foto), in un
         * JPEG nuovo in [target]: si legge solo quel pezzo, anche se la foto è lunga.
         */
        fun crop(image: File, area: Rectangle, target: File): WebClipImage? {
            ImageIO.createImageInputStream(image).use { input ->
                val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull() ?: return null
                try {
                    reader.input = input
                    val bounds = area.intersection(Rectangle(0, 0, reader.getWidth(0), reader.getHeight(0)))
                    if (bounds.width < 2 || bounds.height < 2) return null
                    val part = reader.read(0, reader.defaultReadParam.apply { sourceRegion = bounds })
                    val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
                    try {
                        ImageIO.createImageOutputStream(target).use { out ->
                            writer.output = out
                            val params = writer.defaultWriteParam.apply {
                                compressionMode = ImageWriteParam.MODE_EXPLICIT
                                compressionQuality = PC_JPEG_QUALITY / 100f
                            }
                            writer.write(null, IIOImage(part, null, null), params)
                        }
                    } finally {
                        writer.dispose()
                    }
                    return WebClipImage(target, part.width, part.height)
                } finally {
                    reader.dispose()
                }
            }
        }

        /** La foto [image] rimpicciolita fino a circa [width] pixel di larghezza, per sceglierne un pezzo. */
        fun preview(image: File, width: Int): java.awt.image.BufferedImage? =
            ImageIO.createImageInputStream(image).use { input ->
                val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull() ?: return null
                try {
                    reader.input = input
                    val step = (reader.getWidth(0) + width - 1) / width
                    reader.read(0, reader.defaultReadParam.apply { setSourceSubsampling(step.coerceAtLeast(1), step.coerceAtLeast(1), 0, 0) })
                } finally {
                    reader.dispose()
                }
            }
    }
}
