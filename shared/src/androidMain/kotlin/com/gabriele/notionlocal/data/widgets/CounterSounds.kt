package com.gabriele.notionlocal.data.widgets

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

object CounterSounds {

    // **Subito, senza far aspettare il numero** (dal 27/10/2026, chiesto
    // dall'utente: il + e il − del Counter avevano "quel piccolo delay").
    // Misurato sul telefono: far partire il suono con `AudioTrack` (stop,
    // ricarica, play) teneva fermo il filo principale 65-74 ms a ogni tocco,
    // e il numero cambiava solo dopo. Ora i suoni stanno in un `SoundPool`, il
    // lettore di Android per i suoni brevi: `play` torna subito e il suono
    // parte dal suo filo, con meno attesa. Finché il `SoundPool` non è pronto
    // (appena aperta l'app) suona come prima, ma da un filo a parte.

    private val worker = Executors.newSingleThreadExecutor()
    @Volatile
    private var pool: SoundPool? = null
    /** I suoni pronti nel `SoundPool`, col loro numero. */
    private val ready = ConcurrentHashMap<CounterSound, Int>()
    private val loading = ConcurrentHashMap<Int, CounterSound>()

    /**
     * Prepara i suoni, una volta sola e da un filo a parte: ognuno diventa un
     * piccolo file WAV nella cache dell'app, che il `SoundPool` carica. Si
     * chiama quando il contatore si vede, così al primo tocco sono pronti.
     */
    fun prepare(context: Context) {
        val dir = File(context.applicationContext.cacheDir, "counter_sounds")
        worker.execute {
            if (pool != null) return@execute
            runCatching {
                val sounds = SoundPool.Builder()
                    .setMaxStreams(4)
                    .setAudioAttributes(sonification())
                    .build()
                sounds.setOnLoadCompleteListener { _, id, status ->
                    val sound = loading.remove(id)
                    if (status == 0 && sound != null) ready[sound] = id
                }
                pool = sounds
                dir.mkdirs()
                CounterSound.entries.forEach { sound ->
                    val bytes = CounterSoundWaves.wav(CounterSoundWaves.samples(sound))
                    val file = File(dir, "${sound.name}.wav")
                    if (!file.exists() || file.length() != bytes.size.toLong()) file.writeBytes(bytes)
                    loading[sounds.load(file.path, 1)] = sound
                }
            }
        }
    }

    /**
     * Suona [sound] a [volume] (da 0 a 100, vedi [gain]). A zero, o senza
     * audio, non fa niente. Non aspetta il suono: torna subito.
     */
    fun play(sound: CounterSound, volume: Int = 100) {
        if (volume <= 0) return
        val gain = CounterSoundWaves.gain(volume)
        val id = ready[sound]
        val sounds = pool
        if (id != null && sounds != null) {
            sounds.play(id, gain, gain, 1, 0, 1f)
        } else {
            worker.execute { playWithTrack(sound, gain) }
        }
    }

    /** Come il suono dei tasti: segue il volume di sistema e il silenzioso. */
    private fun sonification(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val tracks = mutableMapOf<CounterSound, AudioTrack>()

    /** Il modo di prima, finché il `SoundPool` non è pronto: se stava ancora suonando, ricomincia. */
    @Synchronized
    private fun playWithTrack(sound: CounterSound, gain: Float) {
        runCatching {
            val track = tracks.getOrPut(sound) { trackFor(CounterSoundWaves.samples(sound)) }
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            track.setVolume(gain)
            track.reloadStaticData()
            track.play()
        }
    }

    private fun trackFor(samples: ShortArray): AudioTrack {
        val track = AudioTrack.Builder()
            .setAudioAttributes(sonification())
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(COUNTER_SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * 2)
            .build()
        track.write(samples, 0, samples.size)
        return track
    }
}
