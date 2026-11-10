package com.gabriele.notionlocal.data.webclip

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.view.ContextThemeWrapper
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/** Le misure di una pagina aperta, in pixel CSS, e quanti pixel veri vale uno CSS adesso. */
data class WebPageMetrics(val width: Double, val height: Double, val viewportHeight: Double, val scrollY: Double, val scale: Float)

/**
 * **Il motore del Web Clipper** (dal 20/10/2026, chiesto dall'utente: "una
 * cosa uguale identica a OneNote Web Clipper"): un WebView che apre il sito
 * **come sul PC**, largo [cssWidth] pixel (1440, come un portatile: è la
 * larghezza a cui game8 viene come nella foto di OneNote), lo prepara e lo
 * fotografa dall'alto in fondo.
 *
 * - **Come sul PC**: il sito riceve l'"user agent" di Chrome per Windows e la
 *   pagina viene impaginata larga [cssWidth], anche quando dice di volersi
 *   adattare al telefono; in chiaro, come la vede chi non ha il tema scuro.
 * - **Prima dello scatto** (`prepare`): i banner dei cookie via (vedi
 *   `assets/web_clipper_cleanup.js`), la pagina fatta scorrere fino in fondo
 *   perché arrivino le immagini che si caricano solo scorrendo, e di nuovo in cima.
 * - **Lo scatto** (`captureFullPage`): la pagina scorre una finestra alla
 *   volta, ogni finestra si disegna e le sue righe vanno nel JPEG a strisce
 *   (`JpegStripWriter`). In memoria c'è una finestra sola, qualunque sia la
 *   lunghezza della pagina.
 *
 * Il WebView deve stare nella finestra dell'app (anche coperto): staccato,
 * Chrome lo considera nascosto e certe pagine non finiscono di caricarsi.
 */
@SuppressLint("SetJavaScriptEnabled")
class WebClipEngine(
    context: Context,
    /** Quanto è larga la pagina, in pixel CSS. */
    val cssWidth: Int = DESKTOP_CSS_WIDTH,
    /** Quanto è alta la finestra da cui la si guarda, in pixel CSS. */
    val cssHeight: Int = DESKTOP_CSS_HEIGHT,
    /** Pixel veri per pixel CSS nella foto: 1,5 come lo schermo di un portatile, nitida ingrandita. */
    val outputScale: Float = OUTPUT_SCALE,
    /**
     * **"Dark mode"** (dal 20/10/2026): il sito vede "prefers-color-scheme:
     * dark" (i siti che hanno un tema scuro usano il loro) e Chrome scurisce da
     * sé quelli che non ce l'hanno; "Article" si impagina scuro.
     */
    val dark: Boolean = false
) {
    /** Il colore sotto la pagina, dove il sito non ne disegna uno. */
    private val pageBackground = if (dark) DARK_PAGE else Color.WHITE
    private val appContext = context.applicationContext

    /** Le misure del WebView in pixel veri: la pagina ci sta esattamente in larghezza. */
    val viewWidthPx: Int = (cssWidth * outputScale).roundToInt()
    val viewHeightPx: Int = (cssHeight * outputScale).roundToInt()

    private var loaded: CompletableDeferred<String>? = null

    /** Il titolo della pagina, appena il sito lo dice. */
    var pageTitle: String? = null
        private set

    /** Quanto è caricata la pagina, da 0 a 100. */
    var loadProgress: Int = 0
        private set

    val webView: WebView = WebView(
        // Il tema chiaro: il sito vede "prefers-color-scheme: light", come OneNote; scuro con "Dark mode".
        ContextThemeWrapper(
            context,
            if (dark) android.R.style.Theme_DeviceDefault_NoActionBar else android.R.style.Theme_DeviceDefault_Light_NoActionBar
        )
    ).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.mediaPlaybackRequiresUserGesture = true
        settings.userAgentString = DESKTOP_USER_AGENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) settings.isAlgorithmicDarkeningAllowed = dark
        setInitialScale((outputScale * 100).roundToInt())
        setBackgroundColor(pageBackground)
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                loaded?.complete(url)
            }
        }
        webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView, title: String?) {
                pageTitle = title?.trim()?.takeIf { it.isNotEmpty() }
            }

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                loadProgress = newProgress
            }
        }
        // I siti in cui si entra con l'account (dentro notE) restano aperti per le foto dopo.
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
    }

    /** Apre l'indirizzo e aspetta che la pagina sia caricata (al massimo [timeoutMs]). L'indirizzo finale. */
    suspend fun load(url: String, timeoutMs: Long = LOAD_TIMEOUT_MS): String? {
        val done = CompletableDeferred<String>()
        loaded = done
        pageTitle = null
        webView.loadUrl(url)
        val finalUrl = withTimeoutOrNull(timeoutMs) { done.await() }
        // Dopo "caricata" i siti continuano: immagini, caratteri, script.
        withTimeoutOrNull(SETTLE_TIMEOUT_MS) {
            while (evaluate("document.readyState") != "\"complete\"") delay(200)
        }
        delay(SETTLE_DELAY_MS)
        darkenOlderWebView()
        return finalUrl ?: webView.url
    }

    /**
     * "Dark mode" prima di Android 13, dove Chrome non sa scurire da sé le
     * pagine: i colori si rovesciano, e le immagini si rovesciano di nuovo,
     * così restano com'erano.
     */
    private suspend fun darkenOlderWebView() {
        if (!dark || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
        evaluate(
            """
            (function () {
              var s = document.createElement('style');
              s.textContent = 'html{filter:invert(1) hue-rotate(180deg);background:#fff}' +
                'img,video,picture,canvas,iframe,[style*="background-image"]{filter:invert(1) hue-rotate(180deg)}';
              (document.head || document.documentElement).appendChild(s);
            })()
            """.trimIndent()
        )
    }

    /**
     * La pagina pronta per la foto: larga [cssWidth] come sul PC, senza
     * banner, con le immagini caricate scorrendo, e in cima.
     */
    suspend fun prepare() {
        forceDesktopWidth()
        delay(400)
        cleanUp()
        loadLazyContent()
        // Certi banner arrivano dopo qualche secondo, o dopo lo scorrimento; e solo ora, con
        // le immagini arrivate, si chiudono i riquadri rimasti vuoti (la pubblicità).
        cleanUp(collapseEmpty = true)
        delay(300)
    }

    /** Il sito impaginato largo [cssWidth] anche se chiede la larghezza del telefono. */
    suspend fun forceDesktopWidth() {
        evaluate(
            """
            (function () {
              var m = document.querySelector('meta[name="viewport"]');
              if (!m) { m = document.createElement('meta'); m.name = 'viewport'; (document.head || document.documentElement).appendChild(m); }
              m.setAttribute('content', 'width=$cssWidth');
              return window.innerWidth;
            })()
            """.trimIndent()
        )
    }

    /**
     * Lo script che toglie banner, barre fisse e pubblicità: vedi
     * `assets/web_clipper_cleanup.js`. Con `collapseEmpty` chiude anche i
     * riquadri alti rimasti vuoti: solo dopo lo scorrimento, quando le
     * immagini che arrivano scorrendo ci sono già. Il resoconto, in JSON.
     */
    suspend fun cleanUp(collapseEmpty: Boolean = false): String? {
        evaluate("window.__noteCollapseEmpty = $collapseEmpty")
        return evaluate(asset(CLEANUP_SCRIPT))
    }

    /**
     * **"Article"**: la pagina diventa solo il suo articolo, impaginato pulito
     * (`noteReader.render()`, vedi `assets/web_clipper_reader.js`). Il titolo
     * dell'articolo, se lo trova.
     */
    suspend fun keepOnlyArticle(): String? {
        evaluate(asset(READER_SCRIPT))
        val result = evaluateJson("noteReader.render($dark)") as? String ?: return null
        return runCatching { JSONObject(result).optString("title").trim().takeIf { it.isNotEmpty() } }.getOrNull()
    }

    /**
     * **"Text and Images"**: l'articolo come blocchi in ordine
     * (`noteReader.serialize()`, vedi `assets/web_clipper_reader.js`), già
     * letto (`parseClipContent`); null se la pagina non ne ha uno.
     */
    suspend fun readContent(): ClipContent? {
        evaluate(asset(READER_SCRIPT))
        val json = evaluateJson("noteReader.serialize()") as? String ?: return null
        return parseClipContent(json)
    }

    /**
     * **La foto di un rettangolo** della pagina come la si vede adesso
     * ("Region"), `area` in pixel del WebView: disegnata a [outputScale]
     * pixel per pixel CSS (come la pagina intera), anche se la si guarda
     * rimpicciolita o ingrandita. JPEG in [target].
     */
    suspend fun captureRegion(area: android.graphics.RectF, target: File, scale: Float = OUTPUT_SCALE): WebClipImage? {
        if (area.width() < 2f || area.height() < 2f) return null
        val visualWidth = (evaluateJson("window.visualViewport ? window.visualViewport.width : window.innerWidth") as? Number)?.toDouble()
            ?: return null
        val pixelsPerCss = webView.width / visualWidth
        val k = (scale / pixelsPerCss).toFloat()
        val width = (area.width() * k).roundToInt().coerceIn(1, MAX_REGION_SIDE)
        val height = (area.height() * k).roundToInt().coerceIn(1, MAX_REGION_SIDE)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(pageBackground)
            val canvas = Canvas(bitmap)
            canvas.scale(k, k)
            canvas.translate(-area.left - webView.scrollX, -area.top - webView.scrollY)
            webView.draw(canvas)
            target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        } finally {
            bitmap.recycle()
        }
        return WebClipImage(target, width, height)
    }

    /**
     * Fa scorrere la pagina fino in fondo, una finestra alla volta: le immagini
     * "lazy" e le parti che arrivano scorrendo si caricano. Poi torna in cima.
     */
    suspend fun loadLazyContent() {
        evaluate(WEB_CLIP_EAGER_IMAGES_SCRIPT)
        var steps = 0
        while (steps < MAX_LAZY_STEPS) {
            val state = evaluateJson(
                "(function(){window.scrollBy(0, Math.round(window.innerHeight*0.85));" +
                    "return [window.scrollY, window.innerHeight, Math.max(document.documentElement.scrollHeight, document.body ? document.body.scrollHeight : 0)];})()"
            ) as? JSONArray ?: break
            delay(LAZY_STEP_DELAY_MS)
            if (state.getDouble(0) + state.getDouble(1) >= state.getDouble(2) - 2) break
            steps++
        }
        delay(LAZY_END_DELAY_MS)
        evaluate("window.scrollTo(0, 0)")
        delay(LAZY_END_DELAY_MS)
    }

    /** Larghezza, altezza e finestra della pagina in pixel CSS, e la scala di adesso. */
    suspend fun metrics(): WebPageMetrics? {
        val values = evaluateJson(
            "(function(){var d=document.documentElement,b=document.body;" +
                "return [window.innerWidth, Math.max(d.scrollHeight, b ? b.scrollHeight : 0, d.offsetHeight), window.innerHeight, window.scrollY];})()"
        ) as? JSONArray ?: return null
        // La scala dalla larghezza: `WebView.getScale()` sul telefono restava quella di partenza (3,5).
        val innerWidth = values.getDouble(0)
        val scale = if (innerWidth > 0 && webView.width > 0) (webView.width / innerWidth).toFloat() else outputScale
        return WebPageMetrics(innerWidth, values.getDouble(1), values.getDouble(2), values.getDouble(3), scale)
    }

    /**
     * **La foto di tutta la pagina**, in [target]: una finestra alla volta, dall'alto in fondo.
     * Una pagina più alta di quanto sta in un JPEG (65535 pixel) si fotografa
     * un po' più piccola, ma intera. `onProgress` va da 0 a 1.
     */
    suspend fun captureFullPage(target: File, onProgress: (Float) -> Unit = {}): WebClipImage? {
        val page = metrics() ?: return null
        val pageScale = page.scale.takeIf { it > 0f } ?: outputScale
        val cssHeight = page.height.coerceAtLeast(1.0)
        val outScale = minOf(outputScale.toDouble(), JpegStripWriter.MAX_SIDE / cssHeight)
        val outWidth = (page.width * outScale).roundToInt().coerceIn(1, JpegStripWriter.MAX_SIDE)
        val outHeight = (cssHeight * outScale).roundToInt().coerceIn(1, JpegStripWriter.MAX_SIDE)
        // Dai pixel del WebView a quelli della foto.
        val k = (outScale / pageScale).toFloat()
        val windowRows = (webView.height * k).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(outWidth, windowRows, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val pixels = IntArray(outWidth * windowRows)
        try {
            target.outputStream().use { stream ->
                val writer = JpegStripWriter(stream, outWidth, outHeight)
                var row = 0
                while (row < outHeight) {
                    // La finestra con in cima la riga che serve, o l'ultima possibile.
                    val wantedCss = row / outScale
                    evaluate("window.scrollTo(0, $wantedCss)")
                    delay(SCROLL_SETTLE_MS)
                    val scrolledCss = (evaluateJson("window.scrollY") as? Number)?.toDouble() ?: wantedCss
                    bitmap.eraseColor(pageBackground)
                    canvas.save()
                    canvas.scale(k, k)
                    canvas.translate(-webView.scrollX.toFloat(), -webView.scrollY.toFloat())
                    webView.draw(canvas)
                    canvas.restore()
                    val firstRowShown = (scrolledCss * outScale).roundToInt()
                    val skip = (row - firstRowShown).coerceIn(0, windowRows - 1)
                    val rows = minOf(windowRows - skip, outHeight - row)
                    if (rows <= 0) break
                    bitmap.getPixels(pixels, 0, outWidth, 0, skip, outWidth, rows)
                    writer.writeRows(pixels, 0, rows)
                    row += rows
                    onProgress(row.toFloat() / outHeight)
                }
                // Se la pagina si è accorciata mentre la si fotografava, il fondo è bianco.
                if (row < outHeight) {
                    java.util.Arrays.fill(pixels, pageBackground)
                    while (row < outHeight) {
                        val rows = minOf(windowRows, outHeight - row)
                        writer.writeRows(pixels, 0, rows)
                        row += rows
                    }
                }
                writer.finish()
            }
        } finally {
            bitmap.recycle()
        }
        evaluate("window.scrollTo(0, 0)")
        return WebClipImage(target, outWidth, outHeight)
    }

    /** Esegue del JavaScript nella pagina e ne restituisce il risultato come testo JSON. */
    suspend fun evaluate(script: String): String? = suspendCancellableCoroutine { continuation ->
        webView.evaluateJavascript(script) { result -> if (continuation.isActive) continuation.resume(result) }
    }

    /** Come [evaluate], letto: un numero, un testo, una lista o un oggetto. */
    suspend fun evaluateJson(script: String): Any? {
        val raw = evaluate(script) ?: return null
        if (raw == "null" || raw.isBlank()) return null
        return runCatching { JSONArray("[$raw]").get(0) }.getOrNull()?.takeIf { it != JSONObject.NULL }
    }

    fun destroy() {
        webView.stopLoading()
        webView.destroy()
    }

    private fun asset(name: String): String = appContext.assets.open(name).bufferedReader().use { it.readText() }

    companion object {
        const val DESKTOP_CSS_WIDTH = WEB_CLIP_CSS_WIDTH
        const val DESKTOP_CSS_HEIGHT = WEB_CLIP_CSS_HEIGHT
        const val OUTPUT_SCALE = WEB_CLIP_OUTPUT_SCALE
        const val CLEANUP_SCRIPT = WEB_CLIP_CLEANUP_SCRIPT
        const val READER_SCRIPT = WEB_CLIP_READER_SCRIPT

        /** Lo sfondo di una pagina scura, quello del tema scuro di Notion. */
        private const val DARK_PAGE = 0xFF191919.toInt()

        const val ARTICLE_CSS_WIDTH = WEB_CLIP_ARTICLE_CSS_WIDTH
        private const val MAX_REGION_SIDE = 8000

        const val DESKTOP_USER_AGENT = WEB_CLIP_USER_AGENT

        private const val LOAD_TIMEOUT_MS = 30_000L
        private const val SETTLE_TIMEOUT_MS = 10_000L
        private const val SETTLE_DELAY_MS = 1_200L
        private const val MAX_LAZY_STEPS = 120
        private const val LAZY_STEP_DELAY_MS = 220L
        private const val LAZY_END_DELAY_MS = 700L
        private const val SCROLL_SETTLE_MS = 160L
    }
}
