package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
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
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.screen.DatabaseViewScreen
import com.gabriele.notionlocal.ui.screen.hoverBarThumb
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **La barra per scorrere di lato un database** (18/11/2026, chiesto dall'utente: "quando ho la
 * puntina del mouse su un database fai sì che ci sia la piccola barra orizzontale… se tolgo la
 * puntina del mouse, fai sì che scompaia dopo due secondi"; dal 18/11/2026 mezzo secondo: "Lasciamela solo per mezzo secondo").
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopHoverScrollBarTest {
    private val dir: File = Files.createTempDirectory("pageflow-hoverbar").toFile()
    private val context = DesktopContext(dir)

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    @Before
    fun setUp() = runBlocking {
        com.gabriele.notionlocal.ui.screen.LastPress.point = null
        AppSettings.init(context)
        val db = AppDatabase.getInstance(context)
        PageRepository(db).createPage(PageEntity(id = "larga", title = "Larga", isDatabase = true, databaseLayout = DatabaseLayout.TABLE))
        repeat(12) { i -> db.databaseDao().insertColumn(DatabaseColumnEntity(id = "c$i", pageId = "larga", name = "Colonna $i", type = ColumnType.TEXT, orderIndex = i)) }
        repeat(5) { i -> db.databaseDao().insertRow(DatabaseRowEntity(id = "r$i", pageId = "larga", title = "Riga $i", orderIndex = i)) }
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    @Test
    fun theThumbFollowsTheScroll() {
        assertEquals(null, hoverBarThumb(500f, 0, 0))
        assertEquals(0f to 250f, hoverBarThumb(500f, 0, 500))
        assertEquals(250f to 250f, hoverBarThumb(500f, 500, 500))
    }

    @Test
    fun theBarShowsWithTheMouseAndGoesAwayAfterHalfASecond() {
        val factory = ViewModelFactory(context)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 600, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            DatabaseViewScreen(
                                pageId = "larga", factory = factory, onBack = {}, onOpenRowPage = {}, onOpenSearch = {},
                                onOpenUpdates = {}, onOpenPage = {}, onOpenDatabase = {}
                            )
                        }
                    }
                }
            }
        }
        var time = 0L
        fun frames(count: Int, sleep: Long = 30) = repeat(count) {
            onWindowThread { scene.render(time) }
            time += 50_000_000L
            Thread.sleep(sleep)
        }
        fun shot(name: String): Bitmap = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }.also {
            File("build/desktop-screenshots").mkdirs()
            File("build/desktop-screenshots", "$name.png").writeBytes(Image.makeFromBitmap(it).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        fun mouse(type: PointerEventType, at: Offset, pressed: Boolean = false, button: PointerButton? = null) = onWindowThread {
            scene.sendPointerEvent(type, at, type = PointerType.Mouse, buttons = if (pressed) PointerButtons(isPrimaryPressed = true) else PointerButtons(), button = button)
        }
        /** La barra in fondo alla tabella: le colonne più chiare del fondo, nella riga [y]. */
        fun barXs(bitmap: Bitmap, y: Int): List<Int> = (0 until bitmap.width).filter { x -> ((bitmap.getColor(x, y) shr 16) and 0xFF) in 60..140 }
        try {
            frames(40)
            val clean = shot("hoverbar_clean")
            val barY = 600 - 2 - 3
            val before = barXs(clean, barY)
            // Il mouse sulla tabella: la barra compare in fondo, a sinistra (si è all'inizio).
            mouse(PointerEventType.Move, Offset(450f, 300f))
            // Il tempo della comparsa (0,2 s) passa nei fotogrammi; quello vero resta sotto il mezzo secondo.
            frames(5, sleep = 5)
            val shown = shot("hoverbar_shown")
            val bar = barXs(shown, barY) - before.toSet()
            assertTrue("la barra si vede: ${bar.size} punti", bar.size > 100)
            assertTrue("all'inizio la maniglia sta a sinistra: ${bar.min()}", bar.min() < 100)
            // Trascinata a destra, la tabella scorre: le intestazioni cambiano.
            val from = Offset(bar.min() + 20f, barY.toFloat())
            mouse(PointerEventType.Move, from)
            mouse(PointerEventType.Press, from, pressed = true, button = PointerButton.Primary)
            for (step in 1..10) {
                mouse(PointerEventType.Move, from + Offset(step * 30f, 0f), pressed = true)
                frames(1)
            }
            mouse(PointerEventType.Release, from + Offset(300f, 0f), button = PointerButton.Primary)
            frames(2, sleep = 20)
            val dragged = shot("hoverbar_dragged")
            val moved = barXs(dragged, barY) - before.toSet()
            assertTrue("la maniglia è andata a destra: ${moved.min()}", moved.min() > bar.min() + 150)
            val header = 600 / 2
            assertNotEquals("la tabella è scorsa", (0 until 900).map { clean.getColor(it, header) }, (0 until 900).map { dragged.getColor(it, header) })
            // Fermi, dopo mezzo secondo la barra se ne va.
            frames(12, sleep = 80)
            val gone = barXs(shot("hoverbar_gone"), barY) - before.toSet()
            assertTrue("dopo mezzo secondo la barra non c'è più: ${gone.size}", gone.size < 20)
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
