package com.gabriele.notionlocal.data.widgets

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **I suoni del contatore** (18/10/2026, chiesti dall'utente): un click su
 * +, uno più lieve su −, da spegnere o cambiare. I suoni li disegna l'app.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CounterSoundsTest {

    @Test
    fun everySoundIsShortAudibleAndItsOwn() {
        val waves = CounterSound.entries.associateWith { CounterSoundWaves.samples(it) }
        waves.forEach { (sound, samples) ->
            // Corto: un tocco non deve aspettare il suono di prima.
            assertTrue(sound.name, samples.size in 100..(COUNTER_SAMPLE_RATE / 4))
            assertTrue(sound.name, CounterSoundWaves.peak(sound) > 0.1)
        }
        // Tutti diversi.
        assertEquals(CounterSound.entries.size, waves.values.map { it.toList() }.toSet().size)
        // Sempre uguali (il fruscio ha il seme fisso).
        assertTrue(CounterSoundWaves.samples(CounterSound.CLICK).contentEquals(CounterSoundWaves.samples(CounterSound.CLICK)))
        // Il meno, di partenza, è più lieve del più.
        assertTrue(CounterSoundWaves.peak(CounterSound.SOFT_CLICK) < CounterSoundWaves.peak(CounterSound.CLICK) / 2)
        // Suonarli senza audio vero non rompe niente, nemmeno a volume zero.
        CounterSounds.play(CounterSound.POP)
        CounterSounds.play(CounterSound.POP, volume = 0)
    }

    @Test
    fun theVolumeBarFollowsTheEar() {
        assertEquals(0f, CounterSoundWaves.gain(0), 0f)
        assertEquals(1f, CounterSoundWaves.gain(100), 0f)
        // A metà barra un quarto del guadagno: all'orecchio, circa metà.
        assertEquals(0.25f, CounterSoundWaves.gain(50), 0.001f)
        // Fuori dalla barra si ferma ai capi; e sale sempre.
        assertEquals(1f, CounterSoundWaves.gain(150), 0f)
        assertEquals(0f, CounterSoundWaves.gain(-10), 0f)
        assertTrue((0..100).zipWithNext().all { (a, b) -> CounterSoundWaves.gain(a) < CounterSoundWaves.gain(b) })
    }

    @Test
    fun oldCountersHaveSoundsOnWithTheDefaults() {
        val json = Json { ignoreUnknownKeys = true }
        val old = json.decodeFromString<Widget>("""{"type":"counter","id":"c1","value":7,"name":"Caffè"}""") as CounterWidget
        assertEquals(7, old.value)
        assertTrue(old.soundsOn)
        assertEquals(CounterSound.CLICK, CounterSound.of(old.plusSound))
        assertEquals(CounterSound.SOFT_CLICK, CounterSound.of(old.minusSound))
        // Il volume, che i contatori di prima non hanno: tutto, come prima.
        assertEquals(100, old.volume)
        // Un nome che non c'è più torna al click.
        assertEquals(CounterSound.CLICK, CounterSound.of("NESSUNO"))
    }

    @Test
    fun eachSoundBecomesAWavFileForTheSoundPool() {
        // 27/10/2026: i suoni passano dal `SoundPool`, che li legge da un file.
        val samples = CounterSoundWaves.samples(CounterSound.CLICK)
        val wav = CounterSoundWaves.wav(samples)
        val header = java.nio.ByteBuffer.wrap(wav).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        assertEquals(44 + samples.size * 2, wav.size)
        assertEquals("RIFF", String(wav, 0, 4))
        assertEquals(wav.size - 8, header.getInt(4))
        assertEquals("WAVEfmt ", String(wav, 8, 8))
        assertEquals(1, header.getShort(20).toInt()) // PCM
        assertEquals(1, header.getShort(22).toInt()) // mono
        assertEquals(COUNTER_SAMPLE_RATE, header.getInt(24))
        assertEquals(16, header.getShort(34).toInt())
        assertEquals("data", String(wav, 36, 4))
        assertEquals(samples[10], header.getShort(44 + 20))
        // Prepararli e suonarli non si ferma su niente, anche prima che siano pronti.
        CounterSounds.prepare(ApplicationProvider.getApplicationContext())
        CounterSounds.play(CounterSound.CLICK, volume = 60)
    }

    @Test
    fun soundsAreSavedWithTheCounter() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WidgetStore.init(context)
        WidgetStore.add(WidgetKind.COUNTER)
        val id = WidgetStore.state.widgets.filterIsInstance<CounterWidget>().last().id
        try {
            WidgetStore.setCounterSounds(id, on = false, plus = CounterSound.WOOD, minus = CounterSound.TICK)
            val counter = WidgetStore.state.widgets.filterIsInstance<CounterWidget>().single { it.id == id }
            assertFalse(counter.soundsOn)
            assertEquals("WOOD", counter.plusSound)
            assertEquals("TICK", counter.minusSound)
            // Il volume (18/10/2026): da 0 a 100, oltre non va.
            WidgetStore.setCounterVolume(id, 40)
            assertEquals(40, WidgetStore.state.widgets.filterIsInstance<CounterWidget>().single { it.id == id }.volume)
            WidgetStore.setCounterVolume(id, 250)
            assertEquals(100, WidgetStore.state.widgets.filterIsInstance<CounterWidget>().single { it.id == id }.volume)
            WidgetStore.setCounterVolume(id, -5)
            assertEquals(0, WidgetStore.state.widgets.filterIsInstance<CounterWidget>().single { it.id == id }.volume)
        } finally {
            WidgetStore.remove(id)
        }
    }
}
