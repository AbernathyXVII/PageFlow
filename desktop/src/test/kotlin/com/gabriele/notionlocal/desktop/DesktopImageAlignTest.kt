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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.semantics.getOrNull
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
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.CAPTION_CENTER
import com.gabriele.notionlocal.data.entity.CAPTION_END
import com.gabriele.notionlocal.data.entity.CAPTION_START
import com.gabriele.notionlocal.data.entity.IMAGE_CENTER
import com.gabriele.notionlocal.data.entity.IMAGE_LEFT
import com.gabriele.notionlocal.data.entity.IMAGE_RIGHT
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.MediaRequestKind
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
 * **Le immagini a sinistra, al centro o a destra, con didascalia e titolo** (18/11/2026, chiesto
 * dall'utente come su Notion: "un testo che seguirà la posizione dell'immagine in base
 * all'allignment… Implementa anche la stessa cosa per un titolo").
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopImageAlignTest {
    private val dir: File = Files.createTempDirectory("pageflow-imagealign").toFile()
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
        repo.createPage(PageEntity(id = "foto", title = "Foto"))
        // Un PNG rosso di 200 x 100.
        val image = java.awt.image.BufferedImage(200, 100, java.awt.image.BufferedImage.TYPE_INT_RGB)
        image.createGraphics().apply { color = java.awt.Color(0xE02020); fillRect(0, 0, 200, 100); dispose() }
        val bytes = java.io.ByteArrayOutputStream().also { javax.imageio.ImageIO.write(image, "png", it) }.toByteArray()
        val file = PageImageStore(context).saveImageBytes(bytes)!!
        repo.saveBlock(BlockEntity(id = "img", pageId = "foto", type = BlockType.IMAGE, textJson = "[]", attrsJson = BlockAttrs.encode(BlockAttrs(file = file, name = "rosso.png")), orderIndex = 0))
        repo.saveBlock(BlockEntity(id = "sotto", pageId = "foto", type = BlockType.PARAGRAPH, textJson = """[{"text":"Riga sotto"}]""", orderIndex = 1))
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    @Test
    fun theImageGoesLeftCenterRightWithItsTexts() {
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
                                pageId = "foto", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
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
        fun type(text: String) = text.forEach { char ->
            onWindowThread {
                val awt = java.awt.event.KeyEvent(java.awt.Canvas(), java.awt.event.KeyEvent.KEY_TYPED, 0L, 0, java.awt.event.KeyEvent.VK_UNDEFINED, char)
                scene.sendKeyEvent(KeyEvent(Key.Unknown, KeyEventType.Unknown, codePoint = char.code, nativeEvent = awt))
            }
            frames(2)
        }
        fun red(c: Int) = ((c shr 16) and 0xFF) > 200 && ((c shr 8) and 0xFF) < 60 && (c and 0xFF) < 60
        /** Da dove a dove va il rosso dell'immagine, e dove comincia e finisce in altezza. */
        fun redBox(bitmap: Bitmap): Pair<IntRange, IntRange> {
            val xs = (0 until bitmap.width).filter { x -> (0 until bitmap.height step 4).any { y -> red(bitmap.getColor(x, y)) } }
            val ys = (0 until bitmap.height).filter { y -> (0 until bitmap.width step 4).any { x -> red(bitmap.getColor(x, y)) } }
            return (xs.first()..xs.last()) to (ys.first()..ys.last())
        }
        fun bright(c: Int) = ((c shr 16) and 0xFF) > 120 && ((c shr 8) and 0xFF) > 120 && (c and 0xFF) > 120
        /** Le colonne con del testo chiaro fra le altezze [ys]. */
        fun textXs(bitmap: Bitmap, ys: IntRange): List<Int> = (0 until bitmap.width).filter { x -> ys.any { y -> bright(bitmap.getColor(x, y)) } }
        fun align(side: String) {
            viewModel.updateBlockAttrs("img") { it.copy(align = side, left = null) }
            frames(10)
        }
        fun attrs() = runBlocking { db.blockDao().getBlocksForPageOnce("foto").first { it.id == "img" }.attrs }
        try {
            frames(40)
            val (centered, _) = redBox(shot("imagealign_start"))
            align(IMAGE_LEFT)
            val (left, _) = redBox(shot("imagealign_left"))
            align(IMAGE_RIGHT)
            val (right, rightYs) = redBox(shot("imagealign_right"))
            assertEquals("larga sempre 200", listOf(200, 200, 200), listOf(centered, left, right).map { it.last - it.first + 1 })
            assertTrue("a sinistra prima del centro, a destra dopo: $left $centered $right", left.first < centered.first && centered.first < right.first)
            assertEquals("al centro: in mezzo fra le due", (left.first + right.first) / 2.0, centered.first.toDouble(), 2.0)

            // La didascalia dal menu: il campo col cursore, si scrive, va sotto l'immagine e finisce dove finisce lei.
            viewModel.requestMedia("img", MediaRequestKind.CAPTION)
            frames(15)
            type("Didascalia")
            frames(10)
            assertEquals("Didascalia", attrs().caption)
            val withCaption = shot("imagealign_caption")
            val (_, imageYs) = redBox(withCaption)
            val captionXs = textXs(withCaption, imageYs.last + 4..imageYs.last + 24)
            assertTrue("la didascalia sotto, allineata a destra con l'immagine: $captionXs $right", captionXs.isNotEmpty() && captionXs.last() in right.last - 6..right.last && captionXs.first() > right.first)

            // Il titolo sopra, anche lui a destra.
            viewModel.requestMedia("img", MediaRequestKind.TITLE)
            frames(15)
            type("Titolo")
            frames(10)
            assertEquals("Titolo", attrs().imageTitle)
            val withTitle = shot("imagealign_title")
            val (_, titledYs) = redBox(withTitle)
            assertTrue("l'immagine è scesa sotto il titolo", titledYs.first > rightYs.first)
            val titleXs = textXs(withTitle, titledYs.first - 26..titledYs.first - 4)
            assertTrue("il titolo sopra, a destra: $titleXs", titleXs.isNotEmpty() && titleXs.last() in right.last - 6..right.last)

            // Al centro i due testi la seguono.
            align(IMAGE_CENTER)
            val centeredShot = shot("imagealign_texts_center")
            val (middle, middleYs) = redBox(centeredShot)
            val centeredCaption = textXs(centeredShot, middleYs.last + 4..middleYs.last + 24)
            assertEquals("la didascalia al centro dell'immagine", (middle.first + middle.last) / 2.0, (centeredCaption.first() + centeredCaption.last()) / 2.0, 6.0)

            // Svuotata la didascalia e usciti dal campo, se ne va.
            viewModel.requestMedia("img", MediaRequestKind.CAPTION)
            frames(15)
            repeat("Didascalia".length) {
                onWindowThread {
                    scene.sendKeyEvent(KeyEvent(Key.Backspace, KeyEventType.KeyDown))
                    scene.sendKeyEvent(KeyEvent(Key.Backspace, KeyEventType.KeyUp))
                }
                frames(1)
            }
            onWindowThread {
                val at = Offset(300f, 650f)
                scene.sendPointerEvent(PointerEventType.Move, at, type = PointerType.Mouse)
                scene.sendPointerEvent(PointerEventType.Press, at, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
                scene.sendPointerEvent(PointerEventType.Release, at, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
            }
            frames(15)
            shot("imagealign_caption_gone")
            assertEquals("la didascalia vuota se ne va", null, attrs().caption)
            assertEquals("Titolo", attrs().imageTitle)
        } finally {
            onWindowThread { scene.close() }
        }
    }

    /**
     * **La didascalia a destra o a sinistra dell'immagine, e il suo allineamento** (26/11/2026, chiesto
     * dall'utente: "sinistra, centro, destra oppure sopra, centro, sotto, in ogni parte dell'immagine dove si
     * trova il caption").
     */
    @Test
    fun theCaptionGoesBesideTheImage() {
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
                                pageId = "foto", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
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
        fun red(c: Int) = ((c shr 16) and 0xFF) > 200 && ((c shr 8) and 0xFF) < 60 && (c and 0xFF) < 60
        fun redBox(bitmap: Bitmap): Pair<IntRange, IntRange> {
            val xs = (0 until bitmap.width).filter { x -> (0 until bitmap.height step 4).any { y -> red(bitmap.getColor(x, y)) } }
            val ys = (0 until bitmap.height).filter { y -> (0 until bitmap.width step 4).any { x -> red(bitmap.getColor(x, y)) } }
            return (xs.first()..xs.last()) to (ys.first()..ys.last())
        }
        fun bright(c: Int) = ((c shr 16) and 0xFF) > 120 && ((c shr 8) and 0xFF) > 120 && (c and 0xFF) > 120
        /** Dove c'è del testo chiaro nel rettangolo [xs] × [ys]: le colonne e le righe. */
        fun text(bitmap: Bitmap, xs: IntRange, ys: IntRange): Pair<List<Int>, List<Int>> =
            xs.filter { x -> ys.any { y -> bright(bitmap.getColor(x, y)) } } to ys.filter { y -> xs.any { x -> bright(bitmap.getColor(x, y)) } }
        fun change(edit: (BlockAttrs) -> BlockAttrs) {
            viewModel.updateBlockAttrs("img", edit)
            frames(10)
        }
        fun attrs() = runBlocking { db.blockDao().getBlocksForPageOnce("foto").first { it.id == "img" }.attrs }
        fun mouse(type: PointerEventType, at: Offset, buttons: PointerButtons = PointerButtons(), button: PointerButton? = null) = onWindowThread {
            scene.sendPointerEvent(type, at, type = PointerType.Mouse, buttons = buttons, button = button)
        }
        fun click(at: Offset, secondary: Boolean = false) {
            val button = if (secondary) PointerButton.Secondary else PointerButton.Primary
            mouse(PointerEventType.Move, at)
            mouse(PointerEventType.Press, at, PointerButtons(isPrimaryPressed = !secondary, isSecondaryPressed = secondary), button)
            mouse(PointerEventType.Release, at, PointerButtons(), button)
            frames(8)
        }
        fun node(label: String): androidx.compose.ui.geometry.Rect = onWindowThread {
            scene.semanticsOwners.flatMap { it.getAllSemanticsNodes(mergingEnabled = false) }.firstOrNull { node ->
                node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } == label
            }?.boundsInRoot
        } ?: throw AssertionError("manca \"$label\"")
        try {
            frames(40)
            // A sinistra, con la didascalia a destra: comincia dopo l'immagine, in alto.
            change { it.copy(align = IMAGE_LEFT, left = null, caption = "Accanto", captionSide = IMAGE_RIGHT) }
            val top = shot("captionside_right_top")
            val (image, imageYs) = redBox(top)
            val (xs, ys) = text(top, image.last + 1 until top.width, imageYs)
            assertTrue("a destra dell'immagine, staccata: $xs $image", xs.isNotEmpty() && xs.first() >= image.last + 8 && xs.first() <= image.last + 20)
            assertTrue("in alto: $ys $imageYs", ys.first() - imageYs.first in 0..14)
            val middle = (imageYs.first + imageYs.last) / 2.0

            change { it.copy(captionAlign = CAPTION_CENTER) }
            val (_, centerYs) = text(shot("captionside_right_center"), image.last + 1 until top.width, imageYs)
            assertEquals("al centro dell'immagine", middle, (centerYs.first() + centerYs.last()) / 2.0, 4.0)

            change { it.copy(captionAlign = CAPTION_END) }
            val (_, bottomYs) = text(shot("captionside_right_bottom"), image.last + 1 until top.width, imageYs)
            assertTrue("in basso: $bottomYs $imageYs", imageYs.last - bottomYs.last() in 0..14)

            // Ci si scrive: il clic dopo il testo mette il cursore in fondo (la didascalia sta fuori dall'immagine).
            val textMid = Offset(xs.last() + 30f, ((bottomYs.first() + bottomYs.last()) / 2).toFloat())
            click(textMid)
            onWindowThread {
                val awt = java.awt.event.KeyEvent(java.awt.Canvas(), java.awt.event.KeyEvent.KEY_TYPED, 0L, 0, java.awt.event.KeyEvent.VK_UNDEFINED, '!')
                scene.sendKeyEvent(KeyEvent(Key.Unknown, KeyEventType.Unknown, codePoint = '!'.code, nativeEvent = awt))
            }
            frames(10)
            assertEquals("Accanto!", attrs().caption)

            // A destra, con la didascalia a sinistra: finisce poco prima dell'immagine.
            change { it.copy(align = IMAGE_RIGHT, captionSide = IMAGE_LEFT, captionAlign = null) }
            val leftShot = shot("captionside_left")
            val (rightImage, rightYs) = redBox(leftShot)
            val (leftXs, _) = text(leftShot, 0 until rightImage.first, rightYs)
            assertTrue("a sinistra, vicino all'immagine: $leftXs $rightImage", leftXs.isNotEmpty() && leftXs.last() in rightImage.first - 20..rightImage.first - 8)

            // Sotto, l'immagine al centro e la didascalia a sinistra: comincia dove comincia lei.
            change { it.copy(align = IMAGE_CENTER, captionSide = null, captionAlign = CAPTION_START) }
            val belowShot = shot("captionside_below_start")
            val (centered, centeredYs) = redBox(belowShot)
            val (belowXs, _) = text(belowShot, 0 until belowShot.width, centeredYs.last + 4..centeredYs.last + 24)
            assertTrue("sotto, a sinistra: $belowXs $centered", belowXs.isNotEmpty() && belowXs.first() in centered.first..centered.first + 6)

            // Dal menu: clic destro sull'immagine, "Caption position", a destra, al centro.
            click(Offset(((centered.first + centered.last) / 2).toFloat(), ((centeredYs.first + centeredYs.last) / 2).toFloat()), secondary = true)
            click(node("Caption position").center)
            click(node("Right of the image").center)
            assertEquals(IMAGE_RIGHT, attrs().captionSide)
            assertEquals("cambiando parte l'allineamento torna quello di partenza", null, attrs().captionAlign)
            click(node("Middle").center)
            assertEquals(CAPTION_CENTER, attrs().captionAlign)
            shot("captionside_menu")
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
