package com.gabriele.notionlocal.desktop

import com.gabriele.notionlocal.data.widgets.COUNTER_SAMPLE_RATE
import com.gabriele.notionlocal.data.widgets.CounterSound
import com.gabriele.notionlocal.data.widgets.CounterSoundWaves
import com.gabriele.notionlocal.data.widgets.DesktopSounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioSystem
import kotlin.math.abs

/**
 * **I suoni del PC** (dal 10/11/2026), senza farli sentire: la sveglia dura
 * un paio di secondi, si sente e fra i due giri tace; i suoni del contatore
 * sono quelli del telefono e il loro WAV si apre con Java Sound.
 */
class DesktopSoundsTest {

    @Test
    fun theAlarmRingsTwiceWithAPause() {
        val wave = DesktopSounds.alarmWave()
        val seconds = wave.size.toDouble() / COUNTER_SAMPLE_RATE
        assertTrue("dura $seconds s", seconds in 2.0..3.5)
        val peak = wave.maxOf { abs(it.toInt()) } / Short.MAX_VALUE.toDouble()
        assertTrue("si sente: $peak", peak > 0.2)
        // Nella pausa dopo le tre note del primo giro, silenzio.
        val pause = (1.1 * COUNTER_SAMPLE_RATE).toInt()
        assertEquals(0, abs(wave[pause].toInt()))
    }

    @Test
    fun everyCounterSoundOpensInJavaSound() {
        for (sound in CounterSound.entries) {
            val wav = CounterSoundWaves.wav(CounterSoundWaves.samples(sound))
            AudioSystem.getAudioInputStream(ByteArrayInputStream(wav)).use {
                assertEquals(sound.name, COUNTER_SAMPLE_RATE.toFloat(), it.format.sampleRate)
                assertTrue(sound.name, it.frameLength > 0)
            }
        }
    }
}
