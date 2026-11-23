package com.gabriele.notionlocal.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
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
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.settings.OpenMode
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.navigation.CenterPeekLayer
import com.gabriele.notionlocal.ui.navigation.PagePeek
import com.gabriele.notionlocal.ui.navigation.PeekActions
import com.gabriele.notionlocal.ui.screen.RowNavigation
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Le frecce delle pagine di un database anche nel peek** (17/11/2026,
 * chiesto dall'utente: "in Center e Side Peek delle pagine, non hai messo le
 * freccette per navigare tra le pagine di un database"). La pagina della riga
 * di mezzo, nel Center peek: in alto a destra le due frecce, giù porta alla
 * terza riga nello stesso peek, su torna alla seconda.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopPeekRowArrowsTest {
    private val dir: File = Files.createTempDirectory("pageflow-peekarrows").toFile()
    private val context = DesktopContext(dir)

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        val db = AppDatabase.getInstance(context)
        val repo = PageRepository(db)
        repo.createPage(PageEntity(id = "elenco", title = "Elenco", isDatabase = true))
        listOf("Riga A", "Riga B", "Riga C").forEachIndexed { i, title ->
            repo.createPage(PageEntity(id = "p$i", title = title, isRowPage = true))
            db.databaseDao().insertRow(DatabaseRowEntity(id = "r$i", pageId = "elenco", title = title, linkedPageId = "p$i", orderIndex = i))
        }
        // L'ordine della vista da cui la riga si è aperta (vedi `RowNavigation`).
        RowNavigation.remember("elenco", listOf("r0", "r1", "r2"))
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    @Test
    fun theArrowsMoveThroughTheRowsInThePeek() {
        val factory = ViewModelFactory(context)
        val owner = ScreenOwner()
        var peek by mutableStateOf(PagePeek("p1", false, OpenMode.CENTER_PEEK))
        var closed = false
        val actions = PeekActions(
            close = { closed = true },
            back = { peek.back()?.let { peek = it } ?: run { closed = true } },
            changeMode = {},
            replace = { id, isDatabase -> peek = peek.replacedBy(id, isDatabase) },
            openSearch = {},
            openUpdates = {},
            openOutside = { _, _, _ -> }
        )
        val scene = onWindowThread {
            ImageComposeScene(width = 1200, height = 800, density = Density(1f)) {
                CompositionLocalProvider(
                    LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner
                ) {
                    NotionLocalTheme { CenterPeekLayer(peek, factory, actions) }
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
        fun click(at: Offset) {
            onWindowThread {
                scene.sendPointerEvent(PointerEventType.Move, at, type = PointerType.Mouse)
                scene.sendPointerEvent(PointerEventType.Press, at, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
                scene.sendPointerEvent(PointerEventType.Release, at, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
            }
            frames(20)
        }
        // I pulsanti in alto a destra del peek (largo 900, al centro: da 150 a 1050): le colonne
        // chiare nella fascia in alto, raggruppate. I primi due sono le frecce (vedi `RowNavigationButtons`).
        fun buttons(bmp: Bitmap): List<IntRange> {
            fun light(c: Int) = ((c shr 16) and 0xFF) > 90 && ((c shr 8) and 0xFF) > 90 && (c and 0xFF) > 90
            val ys = 36 until 110
            val cols = (700 until 1050).filter { x -> ys.any { y -> light(bmp.getColor(x, y)) } }
            val out = ArrayList<IntRange>()
            for (x in cols) {
                val last = out.lastOrNull()
                if (last != null && x <= last.last + 4) out[out.lastIndex] = last.first..x else out += x..x
            }
            return out
        }
        fun rowOfButtons(bmp: Bitmap): Int {
            fun light(c: Int) = ((c shr 16) and 0xFF) > 90 && ((c shr 8) and 0xFF) > 90 && (c and 0xFF) > 90
            val ys = (36 until 110).filter { y -> (700 until 1050).any { x -> light(bmp.getColor(x, y)) } }
            return (ys.first() + ys.last()) / 2
        }
        try {
            frames(40)
            val first = shot("peekarrows_b")
            val found = buttons(first)
            println("PEEKARROWS pulsanti $found")
            assertTrue("almeno le due frecce e i tre puntini: $found", found.size >= 3)
            val y = rowOfButtons(first).toFloat()
            fun center(r: IntRange) = (r.first + r.last) / 2f
            // Giù: la terza riga.
            click(Offset(center(found[1]), y))
            frames(20)
            assertEquals("giù dalla seconda riga", "p2", peek.pageId)
            shot("peekarrows_c")
            // Su: di nuovo la seconda.
            val again = buttons(shot("peekarrows_c2"))
            click(Offset(center(again[0]), y))
            frames(20)
            assertEquals("su dalla terza riga", "p1", peek.pageId)
            // **La freccia "indietro"** (dal 19/11/2026), a destra del pulsante del modo: le pagine
            // aperte prima nel peek, all'indietro (p2, poi p1), e dalla prima chiude il peek.
            fun backArrow(): Offset = onWindowThread {
                val nodes = scene.semanticsOwners.flatMap { it.getAllSemanticsNodes(mergingEnabled = true) }
                    .filter { node -> node.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("Back") == true }
                assertEquals("una sola freccia indietro: ${nodes.map { it.boundsInRoot }}", 1, nodes.size)
                nodes.single().boundsInRoot.center
            }
            val arrow = backArrow()
            assertTrue("la freccia sta in alto a sinistra del peek: $arrow", arrow.x < 600f && arrow.y < 120f)
            click(arrow)
            assertEquals("p2", peek.pageId)
            click(backArrow())
            assertEquals("p1", peek.pageId)
            assertTrue(!closed)
            click(backArrow())
            assertTrue("dalla prima pagina la freccia chiude il peek", closed)
        } finally {
            onWindowThread { scene.close() }
        }
    }

    /**
     * **Il Center peek si allarga e si stringe dai bordi** (chiesto dall'utente il 23/11/2026): il bordo
     * destro trascinato di 100 a destra lo fa largo 200 di più (resta al centro); trascinato verso
     * l'interno non va sotto la larghezza di prima, 900.
     */
    @Test
    fun theCenterPeekGrowsFromItsEdges() {
        val factory = ViewModelFactory(context)
        val owner = ScreenOwner()
        val peek = PagePeek("p1", false, OpenMode.CENTER_PEEK)
        val actions = PeekActions(close = {}, back = {}, changeMode = {}, replace = { _, _ -> }, openSearch = {}, openUpdates = {}, openOutside = { _, _, _ -> })
        val scene = onWindowThread {
            ImageComposeScene(width = 1500, height = 800, density = Density(1f)) {
                CompositionLocalProvider(
                    LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner
                ) {
                    NotionLocalTheme { CenterPeekLayer(peek, factory, actions) }
                }
            }
        }
        var time = 0L
        fun frames(count: Int) = repeat(count) {
            onWindowThread { scene.render(time) }
            time += 100_000_000L
            Thread.sleep(30)
        }
        fun drag(from: Offset, to: Offset) {
            onWindowThread {
                scene.sendPointerEvent(PointerEventType.Move, from, type = PointerType.Mouse)
                scene.sendPointerEvent(PointerEventType.Press, from, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
            }
            for (step in 1..10) {
                onWindowThread {
                    scene.sendPointerEvent(PointerEventType.Move, Offset(from.x + (to.x - from.x) * step / 10f, from.y), type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true))
                }
                frames(1)
            }
            onWindowThread { scene.sendPointerEvent(PointerEventType.Release, to, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary) }
            frames(5)
        }
        try {
            frames(30)
            // La finestra larga 900 al centro di 1500: da 300 a 1200, e il bordo destro subito fuori.
            drag(Offset(1203f, 400f), Offset(1303f, 400f))
            assertEquals(1100, AppSettings.centerPeekWidth)
            // Ora va da 200 a 1300: il bordo destro, trascinato molto a sinistra, si ferma a 900.
            drag(Offset(1303f, 400f), Offset(900f, 400f))
            assertEquals(AppSettings.CENTER_PEEK_MIN_WIDTH, AppSettings.centerPeekWidth)
        } finally {
            AppSettings.centerPeekWidth = AppSettings.CENTER_PEEK_MIN_WIDTH
            onWindowThread { scene.close() }
        }
    }
}
