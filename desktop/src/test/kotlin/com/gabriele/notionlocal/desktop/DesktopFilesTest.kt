package com.gabriele.notionlocal.desktop

import com.gabriele.notionlocal.data.BlockFiles
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.urlLastPathSegment
import com.gabriele.notionlocal.ui.screen.vimeoId
import com.gabriele.notionlocal.ui.screen.youTubeId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import kotlinx.coroutines.runBlocking
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
 * pezzo di un indirizzo (il registratore è in `DesktopRecordingTest`).
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
    fun downloadedImageNamesAreSafe() {
        // "Download" salva in <cartella dell'utente>/Pictures/PageFlow: qui la cartella dell'utente è quella del test.
        val home = System.getProperty("user.home")
        System.setProperty("user.home", dir.path)
        try {
            val image = saved("png")
            fun download(name: String?) = runBlocking { BlockFiles.saveImageToGallery(context, store, image, name) }
            assertTrue(download("Un grafico delle emissioni."))
            assertTrue(download("..\\..\\fuori.png"))
            assertTrue(download("Fonte: OWID?"))
            assertTrue(download("foto.png"))
            assertEquals(
                setOf("Un grafico delle emissioni.png", "fuori.png", "Fonte_ OWID_.png", "foto.png"),
                File(dir, "Pictures/PageFlow").list()!!.toSet()
            )
        } finally {
            System.setProperty("user.home", home)
        }
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
}
