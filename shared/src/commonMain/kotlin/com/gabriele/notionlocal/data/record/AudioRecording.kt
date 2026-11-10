package com.gabriele.notionlocal.data.record

import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.PlatformContext
import com.gabriele.notionlocal.ui.i18n.CaptureStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/** Una registrazione in corso: in quale blocco (e pagina) va, in quale file, da quando. */
data class Recording(val blockId: String, val pageId: String, val fileName: String, val startedAt: Long)

/**
 * **Il registratore dei blocchi audio**: una registrazione alla volta, di
 * tutta l'app. Sul telefono un .m4a (vedi `AudioRecorder.kt`); sul PC, dal
 * 10/11/2026, un .wav (vedi `AudioRecorder.desktop.kt`).
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

/**
 * **Fermata una registrazione**, il suo file va nel blocco audio da cui è
 * partita (o sotto, se intanto è cambiato: `PageRepository.putRecording`).
 * Se non va tenuta, il file è vuoto o la pagina non c'è più, il file se ne va.
 */
suspend fun placeRecording(context: PlatformContext, recording: Recording, mime: String, keep: Boolean) = withContext(Dispatchers.IO) {
    val file = PageImageStore(context).fileFor(recording.fileName)
    if (!keep || !file.exists() || file.length() == 0L) {
        file.delete()
        return@withContext
    }
    val attrs = BlockAttrs(
        file = recording.fileName,
        name = CaptureStrings.recordingName(recording.startedAt, AppSettings.zoneId),
        mime = mime,
        size = file.length()
    )
    val placed = runCatching {
        PageRepository(AppDatabase.getInstance(context)).putRecording(recording.blockId, recording.pageId, attrs)
    }.getOrDefault(false)
    if (!placed) file.delete()
}
