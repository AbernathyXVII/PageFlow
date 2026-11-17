package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.screen.DatabaseViewScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Nei database niente riquadro grigio col mouse sopra** (17/11/2026,
 * chiesto dall'utente: "quando metto la puntina del mouse sopra una casella o
 * sopra qualsiasi cosa che consente di farlo apparire, viene fuori un
 * rettangolino o quadratino in grigio chiaro […] Toglili"): passando sopra
 * una cella e sopra l'intestazione di una colonna, la foto non cambia.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopCellHoverTest {
    private val dir: File = Files.createTempDirectory("pageflow-cellhover").toFile()
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
        PageRepository(db).createPage(PageEntity(id = "giochi", title = "Giochi", isDatabase = true))
        db.databaseDao().insertColumn(DatabaseColumnEntity(id = "voto", pageId = "giochi", name = "Voto", type = ColumnType.NUMBER, orderIndex = 0))
        listOf("Uno", "Due", "Tre").forEachIndexed { i, title ->
            db.databaseDao().insertRow(DatabaseRowEntity(id = "r$i", pageId = "giochi", title = title, orderIndex = i))
        }
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    @Test
    fun noGrayBoxUnderTheMouse() {
        val factory = ViewModelFactory(context)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 1200, height = 600, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            DatabaseViewScreen(
                                pageId = "giochi", factory = factory, onBack = {}, onOpenRowPage = {}, onOpenSearch = {},
                                onOpenUpdates = {}, onOpenPage = {}, onOpenDatabase = {}
                            )
                        }
                    }
                }
            }
        }
        var time = 0L
        fun frames(count: Int) = repeat(count) {
            onWindowThread { scene.render(time) }
            time += 100_000_000L
            Thread.sleep(30)
        }
        fun picture(name: String): Bitmap = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }.also {
            File("build/desktop-screenshots", name).apply { parentFile.mkdirs() }.writeBytes(Image.makeFromBitmap(it).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        fun hover(at: Offset) {
            onWindowThread { scene.sendPointerEvent(PointerEventType.Move, at, type = PointerType.Mouse) }
            frames(6)
        }
        try {
            frames(40)
            val clean = picture("cellhover_clean.png")
            // Le linee fra le righe (come in DesktopRowDragTest), e quelle fra le colonne.
            val background = clean.getColor(1100, clean.height - 5)
            val lines = (60 until clean.height - 20).filter { y -> (1000..1100).all { x -> clean.getColor(x, y) != background } }
            assertTrue("linee: $lines", lines.size >= 4)
            val rowY = (lines[1] + lines[2]) / 2
            val columns = (0 until 900).filter { x -> clean.getColor(x, rowY) != background && clean.getColor(x, rowY - 8) != background && clean.getColor(x, rowY + 8) != background }
            // La cella di Voto: fra la linea dopo Name e la prossima.
            val voto = columns.filter { it > 150 }.let { it.first() + 3 until it.drop(1).first { x -> x > it.first() + 20 } - 3 }
            fun changed(bitmap: Bitmap, xs: IntRange, ys: IntRange) = xs.sumOf { x -> ys.count { y -> bitmap.getColor(x, y) != clean.getColor(x, y) } }
            // Sopra la cella di Voto della seconda riga.
            hover(Offset((voto.first + voto.last) / 2f, rowY.toFloat()))
            assertEquals("pixel cambiati nella cella", 0, changed(picture("cellhover_cell.png"), voto, lines[1] + 2 until lines[2] - 1))
            // Sopra l'intestazione di Voto.
            val headerY = (lines[0] - 16)
            hover(Offset((voto.first + voto.last) / 2f, headerY.toFloat()))
            assertEquals("pixel cambiati nell'intestazione", 0, changed(picture("cellhover_header.png"), voto, lines[0] - 30 until lines[0] - 1))
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
