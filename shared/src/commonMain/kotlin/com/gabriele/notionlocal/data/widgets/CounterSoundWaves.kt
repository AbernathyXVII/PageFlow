package com.gabriele.notionlocal.data.widgets

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

const val COUNTER_SAMPLE_RATE = 44_100

/**
 * **Le onde dei suoni del contatore** (nel codice comune dal 10/11/2026): fatte
 * coi conti, non lette da un file, e le stesse su telefono (`CounterSounds`) e
 * PC (`DesktopSounds`); ognuno le suona coi suoi attrezzi.
 */
object CounterSoundWaves {

    /**
     * L'onda di un suono, a 44,1 kHz, 16 bit, mono. Sempre la stessa per lo
     * stesso suono (il fruscio del click viene da un caso col seme fisso).
     */
    fun samples(sound: CounterSound): ShortArray {
        val wave: (Double) -> Double
        val seconds: Double
        when (sound) {
            // Un colpo secco: un po' di fruscio e una nota acuta che si spengono subito.
            CounterSound.CLICK -> {
                seconds = 0.03
                val noise = noise(seconds)
                wave = { t -> 0.9 * exp(-t / 0.003) * (0.55 * sin(2 * PI * 1800 * t) + 0.45 * noise(t)) }
            }
            // Lo stesso più basso e più piano: per il meno.
            CounterSound.SOFT_CLICK -> {
                seconds = 0.03
                val noise = noise(seconds, seed = 7)
                wave = { t -> 0.32 * exp(-t / 0.004) * (0.75 * sin(2 * PI * 1100 * t) + 0.25 * noise(t)) }
            }
            // Il ticchettio di un orologio.
            CounterSound.TICK -> {
                seconds = 0.02
                wave = { t -> 0.6 * exp(-t / 0.0018) * sin(2 * PI * 3200 * t) }
            }
            // Una bolla che scoppia: la nota scende.
            CounterSound.POP -> {
                seconds = 0.06
                wave = { t -> 0.7 * exp(-t / 0.015) * sin(2 * PI * (900 * t - 6_500 * t * t)) }
            }
            // Una goccia: la nota sale.
            CounterSound.BUBBLE -> {
                seconds = 0.07
                wave = { t -> 0.5 * exp(-t / 0.02) * sin(2 * PI * (300 * t + 4_500 * t * t)) }
            }
            // Un legnetto: due note insieme, corte.
            CounterSound.WOOD -> {
                seconds = 0.06
                wave = { t -> 0.7 * exp(-t / 0.012) * (0.7 * sin(2 * PI * 900 * t) + 0.3 * sin(2 * PI * 2300 * t)) }
            }
            // Un bip morbido.
            CounterSound.BEEP -> {
                seconds = 0.08
                wave = { t -> 0.35 * envelope(t, seconds, 0.005) * sin(2 * PI * 880 * t) }
            }
            // Una moneta: due note, la seconda più alta e più lunga.
            CounterSound.COIN -> {
                seconds = 0.18
                wave = { t ->
                    val note = if (t < 0.04) sin(2 * PI * 988 * t) else sin(2 * PI * 1319 * t) * exp(-(t - 0.04) / 0.05)
                    0.35 * envelope(t, seconds, 0.003) * note
                }
            }
        }
        val count = (seconds * COUNTER_SAMPLE_RATE).roundToInt()
        return ShortArray(count) { i ->
            val value = wave(i.toDouble() / COUNTER_SAMPLE_RATE).coerceIn(-1.0, 1.0)
            (value * Short.MAX_VALUE).roundToInt().toShort()
        }
    }

    /** Un fruscio sempre uguale, da [seed]: un valore fra -1 e 1 per ogni istante. */
    private fun noise(seconds: Double, seed: Int = 3): (Double) -> Double {
        val random = Random(seed)
        val values = DoubleArray((seconds * COUNTER_SAMPLE_RATE).roundToInt() + 1) { random.nextDouble(-1.0, 1.0) }
        return { t -> values[min((t * COUNTER_SAMPLE_RATE).roundToInt(), values.lastIndex)] }
    }

    /** Sale e scende in [ramp] secondi ai due capi, per non schioccare. */
    private fun envelope(t: Double, seconds: Double, ramp: Double): Double =
        min(1.0, min(t / ramp, (seconds - t) / ramp)).coerceAtLeast(0.0)

    /** Il punto più forte dell'onda, fra 0 e 1: per i test (il meno è più lieve del più). */
    fun peak(sound: CounterSound): Double = samples(sound).maxOf { abs(it.toInt()) } / Short.MAX_VALUE.toDouble()

    /**
     * Dalla barra del volume (da 0 a 100) al guadagno della traccia (da 0 a
     * 1): al quadrato, perché l'orecchio non sente il volume in linea retta.
     * Dritto, quasi tutta la barra suonerebbe forte uguale e il piano
     * starebbe tutto in fondo.
     */
    fun gain(volume: Int): Float {
        val share = volume.coerceIn(0, 100) / 100f
        return share * share
    }

    /** Il file WAV di un suono: l'intestazione di 44 byte e l'onda, 16 bit mono. */
    fun wav(samples: ShortArray): ByteArray {
        val data = samples.size * 2
        val buffer = ByteBuffer.allocate(44 + data).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + data).put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray()).putInt(16)
            .putShort(1) // PCM
            .putShort(1) // mono
            .putInt(COUNTER_SAMPLE_RATE)
            .putInt(COUNTER_SAMPLE_RATE * 2)
            .putShort(2)
            .putShort(16)
        buffer.put("data".toByteArray()).putInt(data)
        samples.forEach { buffer.putShort(it) }
        return buffer.array()
    }
}
