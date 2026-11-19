package com.gabriele.notionlocal.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.PageParagraph
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.settings.OpenMode
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.navigation.CenterPeekLayer
import com.gabriele.notionlocal.ui.navigation.PagePeek
import com.gabriele.notionlocal.ui.navigation.PeekActions
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Nel Center peek i margini non cambiano con "Full width"** (18/11/2026, chiesto dall'utente:
 * "sia con il full width attivato o disattivato, il margine rimane uguale, ossia il valore che ha
 * quando il full width è attivato"). La stessa pagina in un peek largo, Full width spento e acceso:
 * il testo comincia nello stesso punto.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopPeekMarginsTest {
    private val dir: File = Files.createTempDirectory("pageflow-peekmargins").toFile()
    private val context = DesktopContext(dir)
    private lateinit var db: AppDatabase

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        db = AppDatabase.getInstance(context)
        val repo = PageRepository(db)
        repo.createPage(PageEntity(id = "stretta", title = "Margini", paragraphJson = PageParagraph.encode(PageParagraph(fullWidth = false))))
        repo.saveBlock(BlockEntity(id = "t", pageId = "stretta", type = BlockType.PARAGRAPH, textJson = """[{"text":"Testo di prova"}]""", orderIndex = 0))
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    @Test
    fun fullWidthDoesNotChangeTheMarginsInTheCenterPeek() {
        val factory = ViewModelFactory(context)
        val owner = ScreenOwner()
        val actions = PeekActions(close = {}, back = {}, changeMode = {}, replace = { _, _ -> }, openSearch = {}, openUpdates = {}, openOutside = { _, _, _ -> })
        val scene = onWindowThread {
            ImageComposeScene(width = 1800, height = 800, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme { CenterPeekLayer(PagePeek("stretta", false, OpenMode.CENTER_PEEK), factory, actions) }
                }
            }
        }
        var time = 0L
        fun frames(count: Int) = repeat(count) {
            onWindowThread { scene.render(time) }
            time += 100_000_000L
            Thread.sleep(30)
        }
        fun shot(name: String): Bitmap = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }.also {
            File("build/desktop-screenshots").mkdirs()
            File("build/desktop-screenshots", "$name.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(it).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        /** Dove comincia il testo chiaro più a sinistra, sotto i comandi del peek. */
        fun textLeft(bitmap: Bitmap): Int = (0 until bitmap.width).first { x ->
            (150 until bitmap.height).any { y -> bitmap.getColor(x, y).let { c -> ((c shr 16) and 0xFF) > 200 && ((c shr 8) and 0xFF) > 200 && (c and 0xFF) > 200 } }
        }
        try {
            frames(50)
            val narrow = textLeft(shot("peek_margins_off"))
            runBlocking { db.pageDao().getById("stretta")!!.let { db.pageDao().update(it.copy(paragraphJson = null)) } }
            frames(30)
            val wide = textLeft(shot("peek_margins_on"))
            assertEquals("il testo comincia allo stesso punto con Full width spento e acceso", wide, narrow)
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
