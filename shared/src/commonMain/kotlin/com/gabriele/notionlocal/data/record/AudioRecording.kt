package com.gabriele.notionlocal.data.record

import kotlinx.coroutines.flow.StateFlow
import com.gabriele.notionlocal.platform.PlatformContext

/** Una registrazione in corso: in quale blocco (e pagina) va, in quale file, da quando. */
data class Recording(val blockId: String, val pageId: String, val fileName: String, val startedAt: Long)

/**
 * **Il registratore dei blocchi audio** (vedi `AudioRecorder.kt`, sul
 * telefono): una registrazione alla volta, di tutta l'app. Sul PC, per ora,
 * non registra (`start` dice di no).
 */
expect object AudioRecorder {
    /** La registrazione in corso, se c'è. */
    val current: StateFlow<Recording?>

    /** Comincia a registrare nel blocco [blockId]; falso se non si può. */
    fun start(context: PlatformContext, blockId: String, pageId: String): Boolean

    /** Quanto è forte il suono adesso, da 0 a 1. */
    fun level(): Float

    /** Ferma la registrazione: con [keep] il file va nel blocco, se no se ne va. */
    fun stop(context: PlatformContext, keep: Boolean = true)
}
