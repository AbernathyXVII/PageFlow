package com.gabriele.notionlocal.data.ocr

/**
 * **Leggere il testo delle foto nuove** (dal 04/11/2026, nel codice comune).
 *
 * L'editor, che ora sta nel codice comune, non conosce il lettore di testo
 * di Google: chiama [request], e a fare il lavoro è chi si è registrato. Sul
 * telefono è `ImageTexts`, quando parte (`ImageTexts.init`); prima di allora,
 * come prima, non succede niente. Sul PC, per ora, nessuno.
 */
object PhotoTextRequests {
    @Volatile
    private var onRequest: (Long) -> Unit = {}

    fun register(request: (delayMs: Long) -> Unit) {
        onRequest = request
    }

    fun request(delayMs: Long = 0) = onRequest(delayMs)
}
