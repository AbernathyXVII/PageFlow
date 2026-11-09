package com.gabriele.notionlocal.desktop

import com.gabriele.notionlocal.data.BlockFiles
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.record.AudioRecorder
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.urlLastPathSegment
import com.gabriele.notionlocal.ui.screen.vimeoId
import com.gabriele.notionlocal.ui.screen.youTubeId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

/**
 * **I file dei blocchi sul PC** (dal 04/11/2026): il tipo di un'immagine
 * letto dai suoi primi byte, l'estensione che va con un tipo, i video di
 * YouTube e Vimeo riconosciuti dall'indirizzo come sul telefono, l'ultimo
 * pezzo di un indirizzo, e il registratore, che sul PC dice di no.
 */
class DesktopFilesTest {
    private val dir: File = Files.createTempDirectory("pageflow-files").toFile()
    private val context = DesktopContext(dir)
    private val store = PageImageStore(context)

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun saved(format: String): String {
        val bytes = ByteArrayOutputStream().also { ImageIO.write(BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), format, it) }.toByteArray()
        val (name, file) = store.newFile(format)
        file.writeBytes(bytes)
        return name
    }

    @Test
    fun imageTypeFromTheFirstBytes() {
        assertEquals("image/png", BlockFiles.imageMime(store, saved("png")))
        assertEquals("image/jpeg", BlockFiles.imageMime(store, saved("jpg")))
        assertEquals("image/gif", BlockFiles.imageMime(store, saved("gif")))
        val (text, file) = store.newFile("txt")
        file.writeText("non è un'immagine")
        assertNull(BlockFiles.imageMime(store, text))
        assertEquals("jpg", BlockFiles.extensionFor("image/jpeg"))
        assertEquals("mp4", BlockFiles.extensionFor("video/mp4"))
        assertNull(BlockFiles.extensionFor(null))
    }

    @Test
    fun youTubeAndVimeoLinks() {
        assertEquals("dQw4w9WgXcQ", youTubeId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=42"))
        assertEquals("dQw4w9WgXcQ", youTubeId("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", youTubeId("https://m.youtube.com/shorts/dQw4w9WgXcQ"))
        assertNull(youTubeId("https://example.com/watch?v=dQw4w9WgXcQ"))
        assertNull(youTubeId("non è un indirizzo"))
        assertEquals("76979871", vimeoId("https://vimeo.com/76979871"))
        assertEquals("76979871", vimeoId("https://player.vimeo.com/video/76979871"))
        assertNull(vimeoId("https://vimeo.com/channels/staffpicks"))
    }

    @Test
    fun lastPieceOfALink() {
        assertEquals("canzone.mp3", urlLastPathSegment("https://example.com/musica/canzone.mp3"))
        assertEquals("musica", urlLastPathSegment("https://example.com/musica/"))
        assertNull(urlLastPathSegment("https://example.com"))
    }

    @Test
    fun noRecordingOnThePc() {
        assertFalse(AudioRecorder.start(context, "blocco", "pagina"))
        assertNull(AudioRecorder.current.value)
        assertEquals(0f, AudioRecorder.level())
    }
}
