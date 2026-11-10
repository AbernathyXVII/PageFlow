package com.gabriele.notionlocal.desktop

import com.gabriele.notionlocal.data.webclip.ClipPiece
import com.gabriele.notionlocal.data.webclip.DesktopWebClipEngine
import com.gabriele.notionlocal.data.webclip.EdgeBrowser
import com.gabriele.notionlocal.platform.DesktopContext
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.awt.Rectangle
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

/**
 * **Il Web Clipper sul PC** (dal 10/11/2026), con Edge nascosto vero, su una
 * pagina scritta qui: la foto della pagina intera ha la larghezza di un
 * portatile (1440, a 2 pixel per pixel CSS) e tutta la sua altezza, "Text and Images" ne legge
 * titolo e paragrafi, "Region" ne ritaglia un pezzo. Senza Edge né Chrome il
 * test non parte.
 */
class DesktopWebClipTest {
    private val dir = Files.createTempDirectory("pageflow-webclip").toFile()
    private val context = DesktopContext(dir)

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun page(): File = File(dir, "pagina.html").apply {
        val paragraphs = (1..12).joinToString("") { "<p>Paragrafo numero $it di una pagina di prova abbastanza lunga da essere un articolo vero.</p>" }
        writeText(
            """
            <!doctype html><html><head><meta charset="utf-8"><title>Pagina di prova</title></head>
            <body style="margin:0;font-family:sans-serif">
              <article><h1>Il titolo dell'articolo</h1>$paragraphs</article>
              <div style="height:3000px;background:linear-gradient(#fff,#2f6fde)"></div>
            </body></html>
            """.trimIndent()
        )
    }

    @Test
    fun edgeTakesThePictureAndReadsTheArticle() = runBlocking<Unit> {
        assumeTrue("serve Edge o Chrome", EdgeBrowser.find() != null)
        val url = page().toURI().toString()

        DesktopWebClipEngine(context).use { engine ->
            assertNotNull("la pagina si apre", engine.load(url))
            assertEquals("Pagina di prova", engine.pageTitle)
            engine.prepare()
            val shot = engine.captureFullPage(File(dir, "foto.jpg"))!!
            assertEquals(2880, shot.width)
            assertTrue("tutta l'altezza: ${shot.height}", shot.height > 3000 * 2)
            val read = ImageIO.read(shot.file)
            assertEquals(shot.width, read.width)
            assertEquals(shot.height, read.height)
            // In fondo la pagina è blu: la foto è arrivata fin lì.
            val bottom = read.getRGB(shot.width / 2, shot.height - 5)
            assertTrue("blu in fondo: ${Integer.toHexString(bottom)}", (bottom and 0xFF) > ((bottom shr 16) and 0xFF))

            val region = DesktopWebClipEngine.crop(shot.file, Rectangle(0, 0, 900, 260), File(dir, "pezzo.jpg"))!!
            assertEquals(900, region.width)
            assertEquals(260, region.height)
            // Il titolo alla risoluzione vera, da guardare: le lettere devono essere nitide.
            region.file.copyTo(File("build/desktop-screenshots/webclip_title.jpg").apply { parentFile.mkdirs() }, overwrite = true)
        }

        DesktopWebClipEngine(context).use { engine ->
            engine.load(url)
            val content = engine.readContent()!!
            val text = content.blocks.joinToString("\n") { piece ->
                when (piece) {
                    is ClipPiece.Heading -> piece.spans.joinToString("") { it.text }
                    is ClipPiece.Paragraph -> piece.spans.joinToString("") { it.text }
                    else -> ""
                }
            }
            assertTrue(text, text.contains("Paragrafo numero 7"))
        }
    }
}
