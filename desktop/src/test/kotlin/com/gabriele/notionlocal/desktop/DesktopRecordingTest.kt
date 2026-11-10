package com.gabriele.notionlocal.desktop

import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.data.record.AudioRecorder
import com.gabriele.notionlocal.data.record.Recording
import com.gabriele.notionlocal.data.record.placeRecording
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStream
import java.nio.file.Files
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

/**
 * **Registrare sul PC** (dal 10/11/2026), senza aprire il microfono: quanto è
 * forte il suono, il .wav scritto mentre si registra (senza sapere quanto sarà
 * lungo) che alla fine si apre intero, e il file che va nel suo blocco audio.
 */
class DesktopRecordingTest {
    private val dir = Files.createTempDirectory("pageflow-record").toFile()
    private val context = DesktopContext(dir)

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    @Test
    fun theLevelIsTheLoudestSample() {
        // 16 bit little-endian: 1000, -32768, 500.
        val samples = byteArrayOf(0xE8.toByte(), 0x03, 0x00, 0x80.toByte(), 0xF4.toByte(), 0x01)
        assertEquals(1f, AudioRecorder.peakOf(samples, 0, samples.size), 0.001f)
        assertEquals(1000 / 32768f, AudioRecorder.peakOf(samples, 0, 2), 0.0001f)
        assertEquals(0f, AudioRecorder.peakOf(ByteArray(8), 0, 8), 0f)
    }

    @Test
    fun aWavOfUnknownLengthComesOutWhole() {
        val format = AudioFormat(22_050f, 16, 1, true, false)
        val bytes = 22_050 * 2
        // Come il microfono: si sa quanto è lungo solo quando finisce.
        val source = object : InputStream() {
            var left = bytes
            override fun read(): Int = throw UnsupportedOperationException()
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (left == 0) return -1
                val n = minOf(len - len % 2, left)
                left -= n
                return n
            }
        }
        val file = dir.resolve("prova.wav")
        AudioInputStream(source, format, AudioSystem.NOT_SPECIFIED.toLong()).use { AudioSystem.write(it, AudioFileFormat.Type.WAVE, file) }
        AudioSystem.getAudioInputStream(file).use { assertEquals("un secondo intero", 22_050L, it.frameLength) }
    }

    @Test
    fun aFinishedRecordingGoesIntoItsBlock() = runBlocking {
        AppSettings.init(context)
        val repo = PageRepository(AppDatabase.getInstance(context))
        repo.createPage(PageEntity(id = "p", title = "Paragraph – prova"))
        val block = BlockEntity(pageId = "p", type = BlockType.AUDIO, orderIndex = 0)
        repo.saveBlock(block)
        val store = PageImageStore(context)

        // Scartata: il file se ne va, il blocco resta vuoto.
        store.fileFor("scartata.wav").apply { parentFile?.mkdirs() }.writeBytes(ByteArray(100))
        placeRecording(context, Recording(block.id, "p", "scartata.wav", 1L), "audio/wav", keep = false)
        assertFalse(store.fileFor("scartata.wav").exists())
        assertNull(AppDatabase.getInstance(context).blockDao().getById(block.id)!!.attrs.file)

        // Tenuta: il file va nel blocco audio.
        store.fileFor("tenuta.wav").writeBytes(ByteArray(100))
        placeRecording(context, Recording(block.id, "p", "tenuta.wav", 1L), "audio/wav", keep = true)
        val attrs = AppDatabase.getInstance(context).blockDao().getById(block.id)!!.attrs
        assertEquals("tenuta.wav", attrs.file)
        assertEquals("audio/wav", attrs.mime)
        assertTrue(store.fileFor("tenuta.wav").exists())
    }
}
