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
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
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
 * **L'indice a destra della pagina** (18/11/2026, chiesto dall'utente come la "Table of contents"
 * di Notion: "delle lineette che rappresentano i titoli… farà vedere quelli che sono stati linkati
 * tramite la funzione Link -> In this page").
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopPageContentsTest {
    private val dir: File = Files.createTempDirectory("pageflow-contents").toFile()
    private val context = DesktopContext(dir)

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        val repo = PageRepository(AppDatabase.getInstance(context))
        repo.createPage(PageEntity(id = "lunga", title = "Indice"))
        val pinned = mapOf(5 to "Primo punto", 40 to "Secondo punto", 75 to "Terzo punto")
        repeat(90) { i ->
            val text = pinned[i] ?: "Riga $i"
            val attrs = if (i in pinned) """{"linkTarget":true}""" else "{}"
            repo.saveBlock(BlockEntity(id = "r$i", pageId = "lunga", type = BlockType.PARAGRAPH, textJson = """[{"text":"$text"}]""", attrsJson = attrs, orderIndex = i))
        }
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    @Test
    fun theDashesOpenTheListAndAClickGoesThere() {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }
        val scene = onWindowThread {
            ImageComposeScene(width = 1000, height = 700, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            PageEditorScreen(
                                pageId = "lunga", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
                                onNavigateToPage = {}, onNavigateToDatabase = {}, viewModel = viewModel
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
            File("build/desktop-screenshots").mkdirs()
            File("build/desktop-screenshots", "$name.png").writeBytes(Image.makeFromBitmap(it).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        fun lit(c: Int) = ((c shr 16) and 0xFF) > 60
        /** Le lineette nel margine a destra: per ognuna l'altezza e se è chiara (il punto dove si è). */
        fun dashes(bitmap: Bitmap): List<Pair<Int, Boolean>> {
            val rows = (100 until 400).filter { y -> (950 until 990).count { x -> lit(bitmap.getColor(x, y)) } >= 8 }
            return rows.filterIndexed { i, y -> i == 0 || rows[i - 1] < y - 1 }.map { y ->
                y to ((950 until 990).maxOf { x -> (bitmap.getColor(x, y) shr 16) and 0xFF } > 180)
            }
        }
        fun mouse(type: PointerEventType, at: Offset, pressed: Boolean = false, button: PointerButton? = null) = onWindowThread {
            scene.sendPointerEvent(type, at, type = PointerType.Mouse, buttons = if (pressed) PointerButtons(isPrimaryPressed = true) else PointerButtons(), button = button)
        }
        try {
            frames(50)
            val clean = shot("contents_start")
            val start = dashes(clean)
            assertEquals("tre lineette, una per punto: $start", 3, start.size)
            assertTrue("in cima nessun punto ancora raggiunto: $start", start.none { it.second })
            // Col mouse sopra, l'elenco coi nomi: si apre verso sinistra.
            val railY = start[1].first.toFloat()
            mouse(PointerEventType.Move, Offset(975f, railY))
            frames(5)
            val open = shot("contents_open")
            val panel = (700 until 990).count { x -> open.getColor(x, railY.toInt()) != clean.getColor(x, railY.toInt()) }
            assertTrue("l'elenco aperto: $panel punti cambiati", panel > 100)
            // Un clic sul secondo nome: la pagina va lì, e la seconda lineetta è quella chiara.
            val names = (100 until 450).filter { y -> (760 until 940).any { x -> open.getColor(x, y).let { c -> ((c shr 16) and 0xFF) > 120 } } }
            val labelRows = names.filterIndexed { i, y -> i == 0 || names[i - 1] < y - 2 }
            assertTrue("tre nomi: $labelRows", labelRows.size >= 3)
            val at = Offset(850f, labelRows[1] + 5f)
            mouse(PointerEventType.Move, at)
            mouse(PointerEventType.Press, at, pressed = true, button = PointerButton.Primary)
            mouse(PointerEventType.Release, at, button = PointerButton.Primary)
            frames(10)
            mouse(PointerEventType.Move, Offset(500f, 650f))
            frames(10)
            val jumped = dashes(shot("contents_jumped"))
            assertEquals("la seconda lineetta chiara: $jumped", listOf(false, true, false), jumped.map { it.second })
            // In fondo alla pagina, l'ultimo punto.
            repeat(20) {
                onWindowThread { scene.sendPointerEvent(PointerEventType.Scroll, Offset(500f, 400f), scrollDelta = Offset(0f, 5f), type = PointerType.Mouse) }
                frames(1)
            }
            frames(10)
            assertEquals("in fondo la terza", listOf(false, false, true), dashes(shot("contents_end")).map { it.second })
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
