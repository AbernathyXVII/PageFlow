package com.gabriele.notionlocal.data.widgets

import java.io.File
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import javax.sound.sampled.LineEvent
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * **I suoni del PC** (dal 10/11/2026), con Java Sound: i clic del contatore
 * (le stesse onde del telefono, `CounterSoundWaves`) e la sveglia di un timer
 * finito o di una fase del pomodoro, che sul telefono è la suoneria scelta.
 * Tutto da un filo a parte: chi chiama non aspetta.
 */
object DesktopSounds {
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "PageFlow-suoni").apply { isDaemon = true } }
    private val format = AudioFormat(COUNTER_SAMPLE_RATE.toFloat(), 16, 1, true, false)

    /** I suoni del contatore pronti, uno per tipo: si riavvolgono e ripartono. */
    private val counter = ConcurrentHashMap<CounterSound, Clip>()

    /** I suoni del contatore pronti prima del primo clic. */
    fun prepare() {
        worker.execute { CounterSound.entries.forEach { counterClip(it) } }
    }

    /** Il suono [sound] a [volume] (da 0 a 100, come sul telefono); se stava ancora suonando, ricomincia. */
    fun play(sound: CounterSound, volume: Int) {
        if (volume <= 0) return
        worker.execute {
            val clip = counterClip(sound) ?: return@execute
            runCatching {
                clip.stop()
                clip.framePosition = 0
                setGain(clip, CounterSoundWaves.gain(volume))
                clip.start()
            }
        }
    }

    /**
     * **La sveglia**: il file scelto per il pomodoro ([uri], un .wav del PC),
     * o la suoneria di PageFlow se non c'è o non si legge (quelle scelte sul
     * telefono, per esempio).
     */
    fun alarm(uri: String?) {
        worker.execute {
            val clip = fileClip(uri) ?: clipOf(alarmWave()) ?: return@execute
            clip.addLineListener { if (it.type == LineEvent.Type.STOP) clip.close() }
            clip.start()
        }
    }

    /**
     * La suoneria di PageFlow: tre note di campanello che salgono (do, mi,
     * sol), due volte, ognuna col suo eco più acuto che si spegne.
     */
    fun alarmWave(): ShortArray {
        val notes = doubleArrayOf(1046.5, 1318.5, 1568.0)
        val noteSeconds = 0.32
        val pause = 0.35
        val round = notes.size * noteSeconds + pause
        val seconds = round * 2
        val count = (seconds * COUNTER_SAMPLE_RATE).roundToInt()
        return ShortArray(count) { i ->
            val t = i.toDouble() / COUNTER_SAMPLE_RATE
            val inRound = t % round
            val note = (inRound / noteSeconds).toInt()
            val value = if (note >= notes.size) 0.0 else {
                val local = inRound - note * noteSeconds
                val f = notes[note]
                // Un attacco di 5 ms per non schioccare, poi si spegne come un campanello.
                val envelope = minOf(1.0, local / 0.005) * exp(-local / 0.12)
                0.45 * envelope * (0.7 * sin(2 * PI * f * t) + 0.3 * sin(2 * PI * f * 2.76 * t))
            }
            (value.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).roundToInt().toShort()
        }
    }

    private fun counterClip(sound: CounterSound): Clip? =
        counter[sound] ?: clipOf(CounterSoundWaves.samples(sound))?.also { counter[sound] = it }

    /** Un suono pronto da [samples] (44,1 kHz, 16 bit, mono); null senza audio. */
    private fun clipOf(samples: ShortArray): Clip? = runCatching {
        val bytes = ByteArray(samples.size * 2)
        samples.forEachIndexed { i, s ->
            bytes[i * 2] = (s.toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = (s.toInt() shr 8).toByte()
        }
        AudioSystem.getClip().apply { open(format, bytes, 0, bytes.size) }
    }.getOrNull()

    /** Il suono del file di [uri], se è un file audio che Java sa leggere (.wav, .aiff, .au). */
    private fun fileClip(uri: String?): Clip? = runCatching {
        val file = uri?.takeIf { it.startsWith("file:") }?.let { File(URI(it)) }?.takeIf { it.isFile } ?: return null
        AudioSystem.getAudioInputStream(file).use { stream -> AudioSystem.getClip().apply { open(stream) } }
    }.getOrNull()

    /** Il volume di [clip], da 0 a 1, in decibel come lo vuole Java Sound. */
    private fun setGain(clip: Clip, gain: Float) {
        val control = runCatching { clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl }.getOrNull() ?: return
        control.value = (20 * log10(max(gain, 0.0001f))).coerceIn(control.minimum, control.maximum)
    }
}
