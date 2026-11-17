package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Il rettangolo azzurro per scegliere i blocchi** (17/11/2026, chiesto
 * dall'utente: "selezionare i blocchi di una pagina già ancora da prima di
 * toccarli", come i file sul desktop di Windows e del Mac): premuto nel
 * margine a sinistra accanto a "Due" e trascinato fino a "Quattro", si vede il
 * rettangolo e sono scelti "Due", "Tre" e "Quattro"; premuto sotto l'ultimo
 * blocco e tirato su fino all'altro, gli ultimi due.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopMarqueeTest {
    private val dir: File = Files.createTempDirectory("pageflow-marquee").toFile()
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
        repo.createPage(PageEntity(id = "rettangolo", title = "R"))
        listOf("Uno", "Due", "Tre", "Quattro", "Cinque", "Sei").forEachIndexed { i, text ->
            repo.saveBlock(BlockEntity(id = "b$i", pageId = "rettangolo", type = BlockType.PARAGRAPH, textJson = """[{"text":"$text"}]""", orderIndex = i))
        }
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun ids(): List<String> = runBlocking { db.blockDao().getBlocksForPageOnce("rettangolo").sortedBy { it.orderIndex }.map { it.id } }

    @Test
    fun aBlueBoxChoosesTheBlocksItTouches() {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 700, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            PageEditorScreen(
                                pageId = "rettangolo", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
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
            File("build/desktop-screenshots", "$name.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(it).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        fun bright(c: Int) = ((c shr 16) and 0xFF) > 150 && ((c shr 8) and 0xFF) > 150 && (c and 0xFF) > 150
        fun bands(bitmap: Bitmap): List<IntRange> {
            val rows = (60 until bitmap.height).filter { y -> (40 until 800).any { x -> bright(bitmap.getColor(x, y)) } }
            val result = ArrayList<IntRange>()
            for (y in rows) {
                val last = result.lastOrNull()
                if (last != null && y <= last.last + 3) result[result.lastIndex] = last.first..y else result += y..y
            }
            return result
        }
        fun center(band: IntRange) = (band.first + band.last) / 2f
        /** Premuto in [from], trascinato a passi fino a [to]; la foto prima di lasciare. */
        fun drag(from: Offset, to: Offset, name: String): Bitmap {
            onWindowThread {
                scene.sendPointerEvent(PointerEventType.Move, from, type = PointerType.Mouse)
                scene.sendPointerEvent(PointerEventType.Press, from, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
            }
            frames(3)
            for (step in 1..8) {
                val at = Offset(from.x + (to.x - from.x) * step / 8f, from.y + (to.y - from.y) * step / 8f)
                onWindowThread { scene.sendPointerEvent(PointerEventType.Move, at, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true)) }
                frames(1)
            }
            frames(2)
            val during = shot(name)
            onWindowThread { scene.sendPointerEvent(PointerEventType.Release, to, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary) }
            frames(6)
            return during
        }
        fun delete() {
            onWindowThread {
                scene.sendKeyEvent(KeyEvent(Key.Delete, KeyEventType.KeyDown))
                scene.sendKeyEvent(KeyEvent(Key.Delete, KeyEventType.KeyUp))
            }
            frames(10)
        }
        try {
            frames(40)
            // Le ultime sei righe: "Uno"… "Sei".
            val lines = bands(shot("marquee_start")).takeLast(6)
            // Dal margine a sinistra accanto a "Due" fino a "Quattro" (più a destra, sopra il testo).
            val during = drag(Offset(12f, center(lines[1])), Offset(200f, center(lines[3])), "marquee_drawn")
            // Il rettangolo si vede: fra il margine e il testo il colore è cambiato (dentro il velo e il bordo).
            val background = during.getColor(5, during.height - 5)
            assertTrue("si vede il rettangolo", (lines[1].last..lines[3].first).any { y -> during.getColor(30, y) != background })
            delete()
            assertEquals("tolti \"Due\", \"Tre\" e \"Quattro\"", listOf("b0", "b4", "b5"), ids())

            // Da sotto l'ultimo blocco, tirato su fino a "Cinque": "Cinque" e "Sei".
            val after = bands(shot("marquee_after_first")).takeLast(3)
            drag(Offset(300f, center(after[2]) + 60f), Offset(300f, center(after[1])), "marquee_from_below")
            delete()
            assertEquals("tolti \"Cinque\" e \"Sei\"", listOf("b0"), ids())
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
