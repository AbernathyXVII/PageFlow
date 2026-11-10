package com.gabriele.notionlocal.data.record

import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.platform.PlatformContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.util.UUID
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.TargetDataLine
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max

/**
 * **Registrare l'audio sul PC** (dal 10/11/2026): dal microfono di Windows,
 * con Java Sound, in un .wav nella cartella delle immagini dell'app; fermata,
 * il file va nel suo blocco come sul telefono (`placeRecording`).
 *
 * shortcut: WAV e non l'AAC (.m4a) del telefono, perché Java non sa scriverlo: circa 2,6 MB al minuto invece di 0,7.
 * Se Windows nega il microfono alle app del desktop la registrazione parte ma resta muta.
 */
actual object AudioRecorder {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _current = MutableStateFlow<Recording?>(null)
    actual val current: StateFlow<Recording?> = _current
    private var line: TargetDataLine? = null
    private var writer: Thread? = null

    /** Il suono più forte dall'ultima volta che lo si è chiesto, da 0 a 1. */
    @Volatile
    private var peak = 0f

    @Synchronized
    actual fun start(context: PlatformContext, blockId: String, pageId: String): Boolean {
        if (_current.value != null) return false
        val name = "${UUID.randomUUID()}.wav"
        val file = PageImageStore(context).fileFor(name)
        val mic = openMicrophone() ?: return false
        file.parentFile?.mkdirs()
        line = mic
        peak = 0f
        // Il file si scrive mentre si registra; quando il microfono si chiude, Java finisce il .wav.
        writer = thread(name = "PageFlow-registrazione", isDaemon = true) {
            runCatching {
                AudioInputStream(MicrophoneStream(mic), mic.format, AudioSystem.NOT_SPECIFIED.toLong()).use {
                    AudioSystem.write(it, AudioFileFormat.Type.WAVE, file)
                }
            }
        }
        _current.value = Recording(blockId, pageId, name, System.currentTimeMillis())
        return true
    }

    /** Il microfono, alla prima qualità che accetta (mono, 16 bit); null se non c'è o è occupato. */
    private fun openMicrophone(): TargetDataLine? {
        for (rate in SAMPLE_RATES) {
            val format = AudioFormat(rate, 16, 1, true, false)
            val mic = runCatching { AudioSystem.getTargetDataLine(format) }.getOrNull() ?: continue
            val opened = runCatching {
                mic.open(format)
                mic.start()
            }.isSuccess
            if (opened) return mic
            runCatching { mic.close() }
        }
        return null
    }

    actual fun level(): Float = peak.also { peak = 0f }

    actual fun stop(context: PlatformContext, keep: Boolean) {
        val finish = stopRecording(context, keep) ?: return
        scope.launch { finish() }
    }

    /** Chiudendo PageFlow mentre registra: la registrazione si ferma e va nel suo blocco prima di uscire. */
    fun stopBeforeExit(context: PlatformContext) {
        val finish = stopRecording(context, keep = true) ?: return
        runBlocking { finish() }
    }

    /** Ferma il microfono; restituisce il lavoro che resta: aspettare il file e metterlo nel blocco. */
    private fun stopRecording(context: PlatformContext, keep: Boolean): (suspend () -> Unit)? {
        val (recording, mic, thread) = synchronized(this) {
            val now = _current.value ?: return null
            val parts = Triple(now, line, writer)
            line = null
            writer = null
            _current.value = null
            parts
        }
        runCatching {
            mic?.stop()
            mic?.close()
        }
        return {
            thread?.join(FINISH_TIMEOUT_MS)
            val file = PageImageStore(context).fileFor(recording.fileName)
            // Un .wav senza suono ha solo l'intestazione.
            placeRecording(context, recording, "audio/wav", keep && file.length() > WAV_HEADER_BYTES)
        }
    }

    /** Il microfono come flusso di byte, che intanto misura quanto è forte il suono. */
    private class MicrophoneStream(private val mic: TargetDataLine) : InputStream() {
        override fun read(): Int = throw UnsupportedOperationException()

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val n = mic.read(b, off, len - len % 2)
            if (n <= 0) return if (mic.isOpen) 0 else -1
            peak = max(peak, peakOf(b, off, n))
            return n
        }
    }

    /** Il campione più forte (16 bit, little-endian) fra [off] e [off] + [n], da 0 a 1. */
    fun peakOf(bytes: ByteArray, off: Int, n: Int): Float {
        var loudest = 0
        var i = off
        while (i + 1 < off + n) {
            val sample = (bytes[i].toInt() and 0xFF) or (bytes[i + 1].toInt() shl 8)
            loudest = max(loudest, abs(sample))
            i += 2
        }
        return (loudest / 32768f).coerceIn(0f, 1f)
    }

    private val SAMPLE_RATES = floatArrayOf(22_050f, 44_100f, 48_000f, 16_000f)
    private const val WAV_HEADER_BYTES = 44L
    private const val FINISH_TIMEOUT_MS = 5_000L
}
