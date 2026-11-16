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
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.text.AnnotatedString
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
import com.gabriele.notionlocal.ui.screen.LinkClipboard
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.awt.datatransfer.StringSelection
import java.io.File
import java.nio.file.Files

/**
 * **Le righe che tengono il posto di un elenco, allineate, e il cursore su di
 * loro** (16/11/2026, trovato dall'utente sul PC con una foto: "il text cursor
 * mi va un po' più a sinistra rispetto a dove dovrei scrivere"). Righe
 * incollate da Windows con il "\r" in fondo, in cima a un pezzo di pagina
 * (qui sotto un titolo grande, che sta in un campo suo). Appunti finti: quelli
 * di Windows restano come sono.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopListColumnTest {
    private val dir: File = Files.createTempDirectory("pageflow-listcolumn").toFile()
    private val context = DesktopContext(dir)
    private lateinit var db: AppDatabase
    private val system = java.awt.datatransfer.Clipboard("prova")

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
        repo.createPage(PageEntity(id = "colonna", title = "Colonna"))
        val held = """{"listIndent":"NUMBERED_LIST_ITEM"}"""
        val blocks = listOf(
            // Numeri a due cifre: 9, poi (dopo le righe in colonna) 10.
            BlockEntity(id = "n1", pageId = "colonna", type = BlockType.NUMBERED_LIST_ITEM, textJson = """[{"text":"Nove"}]""", numberStartsAt = 9),
            BlockEntity(id = "t1", pageId = "colonna", type = BlockType.PARAGRAPH, textJson = """[{"text":"Titolo grande","size":24}]"""),
            BlockEntity(id = "h1", pageId = "colonna", type = BlockType.PARAGRAPH, textJson = """[{"text":"In colonna\r"}]""", attrsJson = held),
            BlockEntity(id = "h2", pageId = "colonna", type = BlockType.PARAGRAPH, textJson = """[{"text":"\r"}]""", attrsJson = held),
            BlockEntity(id = "n2", pageId = "colonna", type = BlockType.NUMBERED_LIST_ITEM, textJson = """[{"text":"Dieci"}]"""),
            BlockEntity(id = "p1", pageId = "colonna", type = BlockType.PARAGRAPH, textJson = """[{"text":"Fine"}]""")
        )
        blocks.forEachIndexed { i, block -> repo.saveBlock(block.copy(orderIndex = i)) }
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun jsons(): List<String> = runBlocking {
        db.blockDao().getBlocksForPageOnce("colonna").sortedBy { it.orderIndex }.map { it.textJson }
    }

    private fun texts(): List<String> = jsons().map { json ->
        Regex("\"text\":\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(json).joinToString("") { it.groupValues[1] }
    }

    @Test
    fun heldLinesLineUpAndTheCaretSitsWhereTheTextGoes() {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        val base = object : ClipboardManager {
            override fun getText(): AnnotatedString? = null
            override fun setText(annotatedString: AnnotatedString) = Unit
        }
        val compose = object : Clipboard {
            override val nativeClipboard: NativeClipboard get() = system
            override suspend fun getClipEntry(): ClipEntry? = null
            override suspend fun setClipEntry(clipEntry: ClipEntry?) = Unit
        }
        val clipboard = LinkClipboard(base, compose, system = system)
        fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 900, density = Density(1f)) {
                CompositionLocalProvider(
                    LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner,
                    LocalClipboardManager provides clipboard, LocalClipboard provides clipboard
                ) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            PageEditorScreen(
                                pageId = "colonna", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
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
        fun key(k: Key, ctrl: Boolean = false) {
            onWindowThread {
                scene.sendKeyEvent(KeyEvent(k, KeyEventType.KeyDown, isCtrlPressed = ctrl))
                scene.sendKeyEvent(KeyEvent(k, KeyEventType.KeyUp, isCtrlPressed = ctrl))
            }
            frames(4)
        }
        fun press(at: Offset) = onWindowThread {
            scene.sendPointerEvent(PointerEventType.Move, at, type = PointerType.Mouse)
            scene.sendPointerEvent(PointerEventType.Press, at, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
            scene.sendPointerEvent(PointerEventType.Release, at, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
        }
        try {
            frames(40)
            val clean = shot("listcolumn_page")
            fun bright(c: Int) = ((c shr 16) and 0xFF) > 140 && ((c shr 8) and 0xFF) > 140 && (c and 0xFF) > 140
            val rows = (170 until 760).filter { y -> (40 until 400).any { x -> bright(clean.getColor(x, y)) } }
            val bands = ArrayList<IntRange>()
            for (y in rows) {
                val last = bands.lastOrNull()
                if (last != null && y <= last.last + 1) bands[bands.lastIndex] = last.first..y else bands += y..y
            }
            bands.removeAll { it.last - it.first < 5 }
            println("LISTCOLUMN bande $bands")
            // Le righe scritte: 9. Nove, Titolo grande, In colonna, (la riga vuota), 10. Dieci, Fine.
            assertEquals("righe sullo schermo: $bands", 5, bands.size)
            fun ink(bmp: Bitmap, band: IntRange) = (40 until 600).filter { x -> band.any { y -> bright(bmp.getColor(x, y)) } }
            // Dove comincia il testo dopo il numero: dopo il primo stacco largo (i due spazi).
            fun textAfterMarker(columns: List<Int>) = columns.zipWithNext().first { (a, b) -> b - a > 5 }.second
            val heldStart = ink(clean, bands[2]).first()
            val itemText = textAfterMarker(ink(clean, bands[3]))
            println("LISTCOLUMN riga in colonna da $heldStart, testo di \"10. Dieci\" da $itemText")
            assertTrue("la riga in colonna comincia col testo delle voci: $heldStart contro $itemText", kotlin.math.abs(heldStart - itemText) <= 1)

            // Sulla riga vuota (che aveva solo il "\r"), il cursore sta dove comincia il testo.
            val emptyY = (bands[2].last + bands[3].first) / 2f
            press(Offset(600f, emptyY))
            frames(1)
            val withCaret = shot("listcolumn_caret")
            val emptyBand = (emptyY.toInt() - 6)..(emptyY.toInt() + 6)
            // A sinistra, sotto il mouse, ci sono i sei puntini della riga: non sono il cursore.
            val caret = ink(withCaret, emptyBand).filter { it >= 50 }
            println("LISTCOLUMN cursore $caret")
            assertTrue("il cursore si vede: $caret", caret.isNotEmpty())
            assertTrue("il cursore sta col testo: ${caret.first()} contro $heldStart", kotlin.math.abs(caret.first() - heldStart) <= 3)

            // Un incolla da Windows ("\r\n"): niente "\r" nelle righe.
            system.setContents(StringSelection("Uno\r\nDue"), null)
            press(Offset(600f, (bands[4].first + bands[4].last) / 2f))
            frames(5)
            key(Key.MoveEnd)
            key(Key.V, ctrl = true)
            frames(10)
            println("LISTCOLUMN dopo l'incolla ${texts()}")
            assertEquals(listOf("Nove", "Titolo grande", "In colonna\\r", "\\r", "Dieci", "FineUno", "Due"), texts())
            assertFalse("niente \\r nelle righe incollate: ${jsons().takeLast(2)}", jsons().takeLast(2).any { "\\r" in it })
            shot("listcolumn_end")
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
