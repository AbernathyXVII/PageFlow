package com.gabriele.notionlocal.desktop

import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.export.ExportFormat
import com.gabriele.notionlocal.data.export.ExportOptions
import com.gabriele.notionlocal.data.export.exportToFile
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.util.zip.ZipFile

/**
 * **L'esportazione sul PC** (dal 09/11/2026): lo stesso Markdown del telefono
 * (`PageExporter`, nel codice comune), con l'immagine e la sottopagina nello
 * .zip accanto. Il PDF è in `DesktopPdfTest`.
 */
class DesktopExportTest {
    private val dir = Files.createTempDirectory("pageflow-export").toFile()
    private val context = DesktopContext(dir)

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    @Test
    fun aPageBecomesMarkdownOnThePc() = runBlocking {
        AppSettings.init(context)
        val factory = ViewModelFactory(context)
        val repo = factory.pageRepository
        repo.createPage(PageEntity(id = "viaggio", title = "Viaggio"))
        repo.createPage(PageEntity(id = "hotel", parentId = "viaggio", title = "Hotel"))
        PageImageStore(context).fileFor("abc.jpg").apply { parentFile?.mkdirs() }.writeBytes(byteArrayOf(1, 2, 3))
        repo.saveBlock(BlockEntity(pageId = "viaggio", type = BlockType.PAGE_LINK, linkedPageId = "hotel", orderIndex = 0))
        repo.saveBlock(
            BlockEntity(
                pageId = "viaggio", type = BlockType.IMAGE, orderIndex = 1,
                attrsJson = Json.encodeToString(BlockAttrs(file = "abc.jpg", name = "foto mare"))
            )
        )
        repo.saveBlock(BlockEntity(pageId = "hotel", textJson = """[{"text":"Camera 12"}]""", orderIndex = 0))

        val zip = exportToFile(context, factory, "viaggio", ExportOptions(ExportFormat.MARKDOWN, subpages = true), "Untitled")!!
        assertEquals("Viaggio.zip", zip.name)
        ZipFile(zip.file).use { file ->
            assertEquals(listOf("Viaggio.md", "Viaggio/foto mare.jpg", "Viaggio/Hotel.md"), file.entries().toList().map { it.name })
            val hotel = file.getInputStream(file.getEntry("Viaggio/Hotel.md")).readBytes().toString(Charsets.UTF_8)
            assertTrue(hotel, hotel.startsWith("# Hotel\n\nCamera 12"))
        }
    }
}
