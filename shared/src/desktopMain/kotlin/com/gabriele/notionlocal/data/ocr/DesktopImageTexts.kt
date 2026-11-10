package com.gabriele.notionlocal.data.ocr

import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.WinRtScript
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Rectangle
import java.io.File
import javax.imageio.ImageIO

/**
 * **Il testo delle foto sul PC** (dal 10/11/2026), per la ricerca come sul
 * telefono (`ImageTexts`): le foto dei blocchi aggiunte sul PC si leggono in
 * sottofondo col lettore di testo di Windows (Windows.Media.Ocr, quello dello
 * Strumento di cattura), chiamato da uno script PowerShell. Quelle arrivate
 * dal telefono hanno già il loro testo (`image_texts` viaggia nei backup).
 *
 * Prima i caratteri delle lingue di Windows, poi giapponese, coreano e cinese,
 * di cui si tengono le righe con quei caratteri, come sul telefono.
 */
object DesktopImageTexts {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var context: DesktopContext? = null

    fun init(context: DesktopContext) {
        this.context = context
        // Il codice comune (l'editor) chiede di leggere le foto nuove da lì.
        PhotoTextRequests.register { request(it) }
        request(STARTUP_DELAY_MS)
    }

    /** Legge le foto che mancano, fra [delayMs]. */
    fun request(delayMs: Long = 0) {
        val ctx = context ?: return
        scope.launch {
            delay(delayMs)
            val store = PageImageStore(ctx)
            runCatching { ImageTextScan.readPending(AppDatabase.getInstance(ctx), store) { name -> read(ctx, store.fileFor(name)) } }
        }
    }

    /**
     * Il testo della foto [image]; null se adesso non si legge (PowerShell o il
     * lettore di Windows non rispondono): si riprova la volta dopo.
     */
    suspend fun read(context: DesktopContext, image: File): ReadText? = withContext(Dispatchers.IO) {
        val parts = tiles(image) ?: return@withContext ReadText("")
        try {
            val output = WinRtScript.run(context, "ocr", SCRIPT, parts.map { it.path }, OCR_TIMEOUT_S) ?: return@withContext null
            ReadText(linesOf(output).joinToString("\n"))
        } finally {
            parts.forEach { it.delete() }
        }
    }

    /**
     * Le righe trovate: quelle del lettore delle lingue di Windows, e di
     * giapponese, coreano e cinese solo quelle coi loro caratteri (leggono
     * anche il latino, e lo ripeterebbero), senza gli spazi che Windows mette
     * fra un ideogramma e l'altro. Una volta sola ognuna.
     */
    fun linesOf(output: String): List<String> {
        val lines = LinkedHashSet<String>()
        for (raw in output.lines()) {
            val (kind, text) = raw.split('\t', limit = 2).takeIf { it.size == 2 } ?: continue
            val line = text.trim()
            if (line.isEmpty()) continue
            when (kind) {
                "main" -> lines += line
                "asian" -> if (line.any(::isAsian)) lines += withoutSpacesBetweenIdeograms(line)
            }
        }
        return lines.toList()
    }

    /** Toglie gli spazi fra due caratteri asiatici (segni come "ー" compresi): Windows li mette fra ogni ideogramma. */
    private fun withoutSpacesBetweenIdeograms(line: String): String {
        val result = StringBuilder()
        line.forEachIndexed { i, c ->
            if (c.isWhitespace()) {
                val before = result.lastOrNull()
                val after = line.substring(i + 1).firstOrNull { !it.isWhitespace() }
                if (before != null && after != null && isAsianBlock(before) && isAsianBlock(after)) return@forEachIndexed
            }
            result.append(c)
        }
        return result.toString()
    }

    private fun isAsianBlock(c: Char): Boolean = when (Character.UnicodeBlock.of(c)) {
        Character.UnicodeBlock.HIRAGANA, Character.UnicodeBlock.KATAKANA, Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
        Character.UnicodeBlock.HANGUL_SYLLABLES, Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION,
        Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS -> true
        else -> false
    }

    private fun isAsian(c: Char): Boolean = when (Character.UnicodeScript.of(c.code)) {
        Character.UnicodeScript.HAN, Character.UnicodeScript.HIRAGANA,
        Character.UnicodeScript.KATAKANA, Character.UnicodeScript.HANGUL -> true
        else -> false
    }

    /**
     * La foto rimpicciolita fino a [READ_WIDTH_PX] (come sul telefono) e, se è
     * molto alta, a pezzi di al più [TILE_HEIGHT_PX]: il lettore di Windows non
     * accetta immagini più grandi di 10000 pixel. PNG a parte, da cancellare;
     * null se non è un'immagine che Java sa aprire.
     */
    fun tiles(image: File): List<File>? = runCatching {
        ImageIO.createImageInputStream(image).use { input ->
            val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull() ?: return null
            try {
                reader.input = input
                val width = reader.getWidth(0)
                val height = reader.getHeight(0)
                val step = ((width + READ_WIDTH_PX - 1) / READ_WIDTH_PX).coerceAtLeast(1)
                val tileSource = TILE_HEIGHT_PX * step
                (0 until height step tileSource).map { top ->
                    val region = Rectangle(0, top, width, minOf(tileSource, height - top))
                    val part = reader.read(0, reader.defaultReadParam.apply {
                        sourceRegion = region
                        setSourceSubsampling(step, step, 0, 0)
                    })
                    File.createTempFile("pageflow-ocr", ".png").also { ImageIO.write(part, "png", it) }
                }
            } finally {
                reader.dispose()
            }
        }
    }.getOrNull()

    private const val READ_WIDTH_PX = 2000
    private const val TILE_HEIGHT_PX = 8000
    private const val STARTUP_DELAY_MS = 8_000L
    private const val OCR_TIMEOUT_S = 120L

    /** Lo script (dopo l'aiuto di `WinRtScript`): per ogni immagine, una riga "main<TAB>testo" o "asian<TAB>testo" per riga trovata. */
    private val SCRIPT = """
        [Windows.Storage.StorageFile, Windows.Storage, ContentType = WindowsRuntime] | Out-Null
        [Windows.Media.Ocr.OcrEngine, Windows.Foundation, ContentType = WindowsRuntime] | Out-Null
        [Windows.Graphics.Imaging.BitmapDecoder, Windows.Graphics, ContentType = WindowsRuntime] | Out-Null
        [Windows.Globalization.Language, Windows.Globalization, ContentType = WindowsRuntime] | Out-Null
        ${'$'}engines = @()
        ${'$'}main = [Windows.Media.Ocr.OcrEngine]::TryCreateFromUserProfileLanguages()
        if (${'$'}main) { ${'$'}engines += ,@('main', ${'$'}main) }
        foreach (${'$'}tag in 'ja', 'ko', 'zh-Hans-CN') {
            ${'$'}language = [Windows.Globalization.Language]::new(${'$'}tag)
            if ([Windows.Media.Ocr.OcrEngine]::IsLanguageSupported(${'$'}language)) {
                ${'$'}engines += ,@('asian', [Windows.Media.Ocr.OcrEngine]::TryCreateFromLanguage(${'$'}language))
            }
        }
        if (${'$'}engines.Count -eq 0) { exit 3 }
        foreach (${'$'}path in ${'$'}Arguments) {
            ${'$'}file = Await ([Windows.Storage.StorageFile]::GetFileFromPathAsync(${'$'}path)) ([Windows.Storage.StorageFile])
            ${'$'}stream = Await (${'$'}file.OpenAsync([Windows.Storage.FileAccessMode]::Read)) ([Windows.Storage.Streams.IRandomAccessStream])
            ${'$'}decoder = Await ([Windows.Graphics.Imaging.BitmapDecoder]::CreateAsync(${'$'}stream)) ([Windows.Graphics.Imaging.BitmapDecoder])
            ${'$'}bitmap = Await (${'$'}decoder.GetSoftwareBitmapAsync()) ([Windows.Graphics.Imaging.SoftwareBitmap])
            foreach (${'$'}engine in ${'$'}engines) {
                ${'$'}result = Await (${'$'}engine[1].RecognizeAsync(${'$'}bitmap)) ([Windows.Media.Ocr.OcrResult])
                foreach (${'$'}line in ${'$'}result.Lines) { ${'$'}engine[0] + "`t" + ${'$'}line.Text }
            }
            ${'$'}stream.Dispose()
        }
    """
}
