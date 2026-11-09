package com.gabriele.notionlocal.data.ocr

import android.content.Context
import android.graphics.Bitmap
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.ImageTextEntity
import com.gabriele.notionlocal.data.entity.attrs
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

/**
 * Quello che il lettore ha trovato in una foto: [text] vuoto se non c'è
 * niente di scritto; [complete] falso se qualche alfabeto non si è potuto
 * leggere (il suo modello non era ancora arrivato).
 */
data class ReadText(val text: String, val complete: Boolean = true)

/**
 * **Chi legge il testo di una foto.** Null se adesso non si può proprio (il
 * modello non è ancora arrivato): si riprova la volta dopo.
 */
fun interface TextReader {
    suspend fun read(image: Bitmap): ReadText?
}

/**
 * **Il testo delle foto, per la ricerca** (dal 03/11/2026, chiesto
 * dall'utente: scansionare i documenti "magari con la ricerca dentro il
 * testo delle foto"). Ogni immagine di un blocco si legge una volta, in
 * sottofondo, e quello che c'è scritto va in `image_texts`: la ricerca lo
 * trova come il testo delle pagine (`PageRepository.search`).
 *
 * Si leggono all'avvio quelle che mancano, una alla volta, e dopo ogni
 * foto o scansione nuova ([request]). Una foto che il lettore non riesce
 * ancora a leggere resta da leggere.
 */
object ImageTexts {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val running = Mutex()

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var reader: TextReader? = null

    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        reader = MlKitTextReader()
        // Il codice comune (l'editor) chiede di leggere le foto nuove da lì.
        PhotoTextRequests.register { request(it) }
        // **Una volta sola** (03/11/2026): la prima prova sul telefono ha letto
        // delle foto prima che arrivassero i modelli di cinese, giapponese e
        // coreano, e le ha segnate come lette per intero. Si rileggono tutte.
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_REREAD_ALL, false)) {
            scope.launch {
                runCatching { AppDatabase.getInstance(app).imageTextDao().markAllToReread() }
                    .onSuccess { prefs.edit().putBoolean(KEY_REREAD_ALL, true).apply() }
            }
        }
        // Dopo l'avvio, per non rubare tempo alla prima pagina.
        request(delayMs = STARTUP_DELAY_MS)
    }

    /** Legge le foto che mancano, fra [delayMs]. */
    fun request(delayMs: Long = 0) {
        val context = appContext ?: return
        val reader = reader ?: return
        scope.launch {
            delay(delayMs)
            runCatching { readPending(AppDatabase.getInstance(context), PageImageStore(context), reader) }
        }
    }

    /**
     * Le foto dei blocchi non ancora lette, una alla volta, con [reader].
     * Restituisce quante ne ha lette. Due giri insieme non si pestano i piedi:
     * il secondo aspetta il primo e trova già fatto quello che ha fatto lui.
     */
    suspend fun readPending(db: AppDatabase, store: PageImageStore, reader: TextReader): Int = running.withLock {
        val done = db.imageTextDao().readFiles().toHashSet()
        val files = db.blockDao().getImageBlocks().mapNotNull { it.attrs.file }.distinct().filter { it !in done }
        var count = 0
        for (name in files) {
            if (!store.fileFor(name).exists()) continue
            val image = store.load(name, READ_WIDTH_PX)
            // Un file che non si apre come immagine: letto, e niente scritto.
            val read = try {
                if (image == null) ReadText("") else reader.read(image)
            } finally {
                image?.recycle()
            } ?: continue
            db.imageTextDao().upsert(
                ImageTextEntity(file = name, text = read.text.trim(), readAt = if (read.complete) System.currentTimeMillis() else 0)
            )
            count++
        }
        count
    }

    /** Larga abbastanza per leggere le scritte piccole di un foglio A4. */
    private const val READ_WIDTH_PX = 2000
    private const val STARTUP_DELAY_MS = 8_000L
    private const val PREFS = "image_texts"
    private const val KEY_REREAD_ALL = "rereadAll1"
}

/**
 * **Il lettore di ML Kit**, dentro Google Play Services: prima i caratteri
 * latini, poi cinese, giapponese e coreano, di cui si tengono solo le righe
 * con quei caratteri (tutti e tre leggono anche il latino, e lo
 * ripeterebbero).
 */
private class MlKitTextReader : TextReader {
    private val latin by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val asian: List<TextRecognizer> by lazy {
        listOf(
            TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()),
            TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build()),
            TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
        )
    }

    override suspend fun read(image: Bitmap): ReadText? {
        val input = InputImage.fromBitmap(image, 0)
        val lines = latin.process(input).awaitOrNull()?.lines() ?: return null
        val all = LinkedHashSet(lines)
        var complete = true
        for (recognizer in asian) {
            val found = recognizer.process(input).awaitOrNull()
            if (found == null) complete = false
            found?.lines()?.filter { line -> line.any(::isAsian) }?.let(all::addAll)
        }
        return ReadText(all.joinToString("\n"), complete)
    }

    private fun Text.lines(): List<String> =
        textBlocks.flatMap { block -> block.lines.map { it.text.trim() } }.filter { it.isNotEmpty() }

    private fun isAsian(c: Char): Boolean = when (Character.UnicodeScript.of(c.code)) {
        Character.UnicodeScript.HAN, Character.UnicodeScript.HIRAGANA,
        Character.UnicodeScript.KATAKANA, Character.UnicodeScript.HANGUL -> true
        else -> false
    }
}

private suspend fun <T> Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task -> continuation.resume(if (task.isSuccessful) task.result else null) }
}
