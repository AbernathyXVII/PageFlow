package com.gabriele.notionlocal.data.ocr

import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.ImageTextEntity
import com.gabriele.notionlocal.data.entity.attrs
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Quello che il lettore ha trovato in una foto: [text] vuoto se non c'è
 * niente di scritto; [complete] falso se qualche alfabeto non si è potuto
 * leggere (il suo modello non era ancora arrivato).
 */
data class ReadText(val text: String, val complete: Boolean = true)

/**
 * **Le foto da leggere** (nel codice comune dal 10/11/2026): quelle dei blocchi
 * non ancora lette, una alla volta, col lettore di ogni sistema (sul telefono
 * ML Kit, `ImageTexts`; sul PC Windows, `DesktopImageTexts`), e il testo in
 * `image_texts`, dove lo trova la ricerca.
 */
object ImageTextScan {
    private val running = Mutex()

    /**
     * Le foto dei blocchi non ancora lette, con [read] (dal nome del file).
     * Restituisce quante ne ha lette. Due giri insieme non si pestano i piedi:
     * il secondo aspetta il primo e trova già fatto quello che ha fatto lui.
     * Una foto che [read] non riesce ancora a leggere (null) resta da leggere.
     */
    suspend fun readPending(db: AppDatabase, store: PageImageStore, read: suspend (name: String) -> ReadText?): Int = running.withLock {
        val done = db.imageTextDao().readFiles().toHashSet()
        val files = db.blockDao().getImageBlocks().mapNotNull { it.attrs.file }.distinct().filter { it !in done }
        var count = 0
        for (name in files) {
            if (!store.fileFor(name).exists()) continue
            val found = read(name) ?: continue
            db.imageTextDao().upsert(
                ImageTextEntity(file = name, text = found.text.trim(), readAt = if (found.complete) System.currentTimeMillis() else 0)
            )
            count++
        }
        count
    }
}
