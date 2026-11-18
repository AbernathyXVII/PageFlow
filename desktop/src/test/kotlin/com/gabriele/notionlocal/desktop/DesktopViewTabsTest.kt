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
import com.gabriele.notionlocal.data.entity.DatabaseLayout
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
 * **Sul PC le viste di un database in fila** (18/11/2026, chiesto dall'utente: "come fa Notion, su
 * PC concedimi di vedere più viste di un database in ordine orizzontalmente… se ce ne sono troppe…
 * una barra di scorrimento. Più a sinistra lascia sempre il pulsante… in ordine verticalmente").
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopViewTabsTest {
    private val dir: File = Files.createTempDirectory("pageflow-viewtabs").toFile()
    private val context = DesktopContext(dir)
    private lateinit var db: AppDatabase
    private val viewIds = mutableListOf<String>()

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    @Before
    fun setUp() = runBlocking {
        com.gabriele.notionlocal.ui.screen.LastPress.point = null
        AppSettings.init(context)
        db = AppDatabase.getInstance(context)
        val repo = PageRepository(db)
        repo.createPage(PageEntity(id = "db", title = "Database principale", isDatabase = true, databaseLayout = DatabaseLayout.TABLE))
        val layouts = listOf(DatabaseLayout.BOARD, DatabaseLayout.LIST, DatabaseLayout.TABLE, DatabaseLayout.GALLERY)
        for (i in 1..8) viewIds += repo.createView("db", "Vista numero $i", layouts[i % layouts.size])!!
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    @Test
    fun theViewsStandInARow() {
        val factory = ViewModelFactory(context)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 500, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            DatabaseViewScreen(
                                pageId = "db", factory = factory, onBack = {}, onOpenRowPage = {}, onOpenSearch = {},
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
        fun shot(name: String): Bitmap = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }.also {
            File("build/desktop-screenshots", "$name.png").apply { parentFile.mkdirs() }.writeBytes(Image.makeFromBitmap(it).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        fun click(x: Float, y: Float) = onWindowThread {
            val at = Offset(x, y)
            scene.sendPointerEvent(PointerEventType.Move, at, type = PointerType.Mouse)
            scene.sendPointerEvent(PointerEventType.Press, at, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
            scene.sendPointerEvent(PointerEventType.Release, at, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
        }
        fun active(): String? = runBlocking { db.pageDao().getById("db")?.activeViewId }
        /** La riga della barra delle viste: dove c'è del testo chiaro, da sinistra a destra. */
        fun brightColumns(bitmap: Bitmap, y: IntRange = 90..106): List<Int> =
            (0 until bitmap.width).filter { x -> y.any { yy -> bitmap.getColor(x, yy).let { c -> ((c shr 16) and 0xFF) > 150 && (c and 0xFF) > 150 } } }
        try {
            frames(40)
            val start = shot("viewtabs_start")
            // L'ultima creata è davanti, e la fila è scorsa fino a lei: la barra sotto ha la sua maniglia.
            assertEquals(viewIds.last(), active())
            val thumb = (300 until 700).filter { x -> start.getColor(x, 115).let { c -> ((c shr 16) and 0xFF) in 50..110 } }
            assertTrue("la barra di scorrimento: $thumb", thumb.size > 100)
            // Le icone a destra restano dove stanno: niente scheda le copre.
            val right = brightColumns(start).filter { it > 700 }
            assertTrue("le icone a destra: $right", right.isNotEmpty() && right.max() < 820)

            // Un clic su "Vista numero 6": davanti lei, e la fila non torna all'inizio.
            click(370f, 98f)
            frames(20)
            assertEquals(viewIds[5], active())
            val six = shot("viewtabs_six")
            // L'icona di "Vista numero 5" è dov'era.
            assertEquals("la fila resta dov'era", brightColumns(start).first { it >= 170 }, brightColumns(six).first { it >= 170 })

            // Anche la rotella col mouse sopra fa scorrere la fila.
            onWindowThread { scene.sendPointerEvent(PointerEventType.Scroll, Offset(400f, 98f), scrollDelta = Offset(0f, -3f), type = PointerType.Mouse) }
            frames(10)
            // Su: la fila torna verso l'inizio, e la maniglia della barra va a sinistra.
            fun thumbStart(bitmap: Bitmap) = (100 until 700).first { x -> bitmap.getColor(x, 115).let { c -> ((c shr 16) and 0xFF) in 50..110 } }
            val thumbBefore = thumbStart(six)
            val wheeled = thumbStart(shot("viewtabs_wheel"))
            assertTrue("la rotella sposta la fila: maniglia da $thumbBefore a $wheeled", wheeled < thumbBefore - 30)
            // Col touchpad la fila scorre fino all'inizio: lì la vista di partenza.
            repeat(10) {
                onWindowThread { scene.sendPointerEvent(PointerEventType.Scroll, Offset(400f, 98f), scrollDelta = Offset(-10f, 0f), type = PointerType.Mouse) }
                frames(1)
            }
            frames(10)
            shot("viewtabs_scrolled")
            click(140f, 98f)
            frames(20)
            assertEquals("la vista di partenza (nessuna in più davanti)", null, active())

            // Il pulsante più a sinistra: tutte le viste in colonna.
            val before = shot("viewtabs_before_list")
            click(96f, 99f)
            frames(20)
            val list = shot("viewtabs_list")
            val changed = (0 until list.height step 4).sumOf { y -> (0 until list.width step 4).count { x -> list.getColor(x, y) != before.getColor(x, y) } }
            assertTrue("l'elenco delle viste: $changed punti cambiati", changed > 2000)
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
