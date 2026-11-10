package com.gabriele.notionlocal.desktop

import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.ocr.DesktopImageTexts
import com.gabriele.notionlocal.data.ocr.ImageTextScan
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.platform.DesktopContext
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

/**
 * **Il testo delle foto sul PC** (dal 10/11/2026): una scritta disegnata qui
 * la legge il lettore di Windows e finisce in `image_texts`; le righe di
 * giapponese, coreano e cinese si tengono solo coi loro caratteri, senza gli
 * spazi fra gli ideogrammi; una foto molto alta si legge a pezzi.
 */
class DesktopImageTextsTest {
    private val dir = Files.createTempDirectory("pageflow-ocr").toFile()
    private val context = DesktopContext(dir)

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    @Test
    fun theLinesAreCleanedLikeOnThePhone() {
        val output = "main\tLista della spesa\nasian\tLista della spesa\nasian\t東 京 タ ワ ー\nmain\tLista della spesa\nrumore"
        assertEquals(listOf("Lista della spesa", "東京タワー"), DesktopImageTexts.linesOf(output))
    }

    @Test
    fun aVeryTallPictureIsReadInPieces() {
        val tall = File(dir, "alta.png")
        ImageIO.write(BufferedImage(1000, 20_000, BufferedImage.TYPE_INT_RGB), "png", tall)
        val parts = DesktopImageTexts.tiles(tall)!!
        try {
            assertEquals(3, parts.size)
            parts.forEach { assertTrue(ImageIO.read(it).height <= 8000) }
        } finally {
            parts.forEach { it.delete() }
        }
    }

    @Test
    fun windowsReadsTheWordsOfAPhoto() = runBlocking<Unit> {
        assumeTrue("serve Windows", System.getProperty("os.name").orEmpty().startsWith("Windows"))
        val store = PageImageStore(context)
        val name = "scritta.png"
        val image = BufferedImage(1200, 300, BufferedImage.TYPE_INT_RGB).apply {
            createGraphics().apply {
                color = Color.WHITE
                fillRect(0, 0, width, height)
                setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                color = Color.BLACK
                font = Font("Arial", Font.BOLD, 72)
                drawString("Ricevuta PageFlow", 60, 180)
                dispose()
            }
        }
        ImageIO.write(image, "png", store.fileFor(name).apply { parentFile?.mkdirs() })
        val repo = PageRepository(AppDatabase.getInstance(context))
        repo.createPage(PageEntity(id = "p", title = "Paragraph – prova"))
        repo.saveBlock(BlockEntity(pageId = "p", type = BlockType.IMAGE, orderIndex = 0, attrsJson = Json.encodeToString(BlockAttrs(file = name))))

        val read = ImageTextScan.readPending(AppDatabase.getInstance(context), store) { DesktopImageTexts.read(context, store.fileFor(it)) }
        assertEquals("letta una foto", 1, read)
        val text = AppDatabase.getInstance(context).imageTextDao().readFiles()
        assertEquals(listOf(name), text)
        val found = DesktopImageTexts.read(context, store.fileFor(name))!!.text
        assertTrue("letto: $found", found.contains("Ricevuta", ignoreCase = true) && found.contains("PageFlow", ignoreCase = true))
    }
}
