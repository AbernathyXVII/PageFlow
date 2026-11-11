package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.Density
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.ui.theme.PcZoom
import com.gabriele.notionlocal.ui.theme.ctrlWheelZoom
import com.gabriele.notionlocal.ui.theme.zoomKeyOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Lo zoom del PC, come in un browser** (dal 11/11/2026, chiesto
 * dall'utente): Ctrl + e Ctrl −, e Ctrl con la rotella. Il testo non si
 * ingrandisce come un'immagine: va a capo prima, la pagina si ridispone.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopZoomTest {
    private val dir: File = Files.createTempDirectory("pageflow-zoom").toFile()
    private val context = DesktopContext(dir)

    @Before
    fun setUp() {
        AppSettings.init(context)
        AppSettings.zoom = 100
    }

    @After
    fun tearDown() {
        // `AppSettings` è uno solo per tutti i test: gli altri si aspettano il 100%.
        AppSettings.zoom = 100
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    @Test
    fun theKeysAndTheSteps() {
        fun ctrl(key: Key) = zoomKeyOf(KeyEvent(key, KeyEventType.KeyDown, isCtrlPressed = true))
        assertEquals(true, ctrl(Key.Plus))
        assertEquals(true, ctrl(Key.Equals))
        assertEquals(true, ctrl(Key.NumPadAdd))
        assertEquals(false, ctrl(Key.Minus))
        assertEquals(false, ctrl(Key.NumPadSubtract))
        assertEquals(null, zoomKeyOf(KeyEvent(Key.Plus, KeyEventType.KeyDown)))
        assertEquals(null, ctrl(Key.A))
        // Gli scatti di Chrome, e ai capi si resta lì.
        PcZoom.step(bigger = true)
        assertEquals(110, AppSettings.zoom)
        PcZoom.step(bigger = false)
        PcZoom.step(bigger = false)
        assertEquals(90, AppSettings.zoom)
        repeat(20) { PcZoom.step(bigger = false) }
        assertEquals(50, AppSettings.zoom)
        repeat(20) { PcZoom.step(bigger = true) }
        assertEquals(300, AppSettings.zoom)
    }

    /** Lo stesso testo, nella stessa larghezza: col 150% va a capo più volte. */
    @Test
    fun theTextReflowsLikeInABrowser() {
        var lines = 0
        val text = "Lo zoom come in un browser: il testo diventa più grande e va a capo prima, perché la pagina si ridispone. ".repeat(3)
        val scene = onWindowThread {
            ImageComposeScene(width = 400, height = 600, density = Density(1f)) {
                NotionLocalTheme {
                    Box(Modifier.fillMaxWidth()) { Text(text, onTextLayout = { lines = it.lineCount }) }
                }
            }
        }
        try {
            onWindowThread { scene.render(0L) }
            val atNormal = lines
            AppSettings.zoom = 150
            onWindowThread { scene.render(100_000_000L); scene.render(200_000_000L) }
            assertTrue("al 150% più righe ($lines) che al 100% ($atNormal)", lines > atNormal && atNormal > 0)
        } finally {
            onWindowThread { scene.close() }
        }
    }

    /** Ctrl con la rotella in su ingrandisce e la pagina non scorre; senza Ctrl la rotella scorre come sempre. */
    @Test
    fun ctrlAndTheWheel() {
        val list = LazyListState()
        val scene = onWindowThread {
            ImageComposeScene(width = 400, height = 600, density = Density(1f)) {
                NotionLocalTheme {
                    Box(Modifier.fillMaxSize().ctrlWheelZoom()) {
                        LazyColumn(state = list, modifier = Modifier.fillMaxSize()) {
                            items(200) { Text("Riga $it") }
                        }
                    }
                }
            }
        }
        fun wheel(dy: Float, ctrl: Boolean) = onWindowThread {
            scene.sendPointerEvent(
                PointerEventType.Scroll, Offset(200f, 300f), scrollDelta = Offset(0f, dy), type = PointerType.Mouse,
                keyboardModifiers = PointerKeyboardModifiers(isCtrlPressed = ctrl)
            )
            scene.render(0L)
        }
        try {
            onWindowThread { scene.render(0L) }
            wheel(-1f, ctrl = true)
            assertEquals(110, AppSettings.zoom)
            wheel(1f, ctrl = true)
            wheel(1f, ctrl = true)
            assertEquals(90, AppSettings.zoom)
            assertEquals("con Ctrl la pagina non scorre", 0, list.firstVisibleItemIndex + list.firstVisibleItemScrollOffset)
            wheel(3f, ctrl = false)
            repeat(10) { onWindowThread { scene.render(it * 50_000_000L) } }
            assertEquals("senza Ctrl niente zoom", 90, AppSettings.zoom)
            assertTrue("senza Ctrl la pagina scorre", list.firstVisibleItemIndex + list.firstVisibleItemScrollOffset > 0)
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
