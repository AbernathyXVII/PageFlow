package com.gabriele.notionlocal.desktop

import com.gabriele.notionlocal.data.EmojiCatalogs
import com.gabriele.notionlocal.data.widgets.CounterWidget
import com.gabriele.notionlocal.data.widgets.WidgetKind
import com.gabriele.notionlocal.data.widgets.WidgetStore
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.cacheDirOf
import com.gabriele.notionlocal.platform.decodeImageBitmap
import com.gabriele.notionlocal.platform.readAppAsset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

/**
 * **I widget, le emoji e la cache sul PC** (dal 04/11/2026): `WidgetStore`
 * salva nel file `widgets.properties` della cartella dei dati (sul telefono
 * nelle `SharedPreferences` "widgets"), le emoji del selettore arrivano dalle
 * risorse del programma (gli stessi file degli asset del telefono), e gli
 * stemmi si leggono con Skia.
 */
class DesktopWidgetsTest {

    companion object {
        // `WidgetStore` è uno solo per tutti i test: la cartella è quella di `TestWidgets`.
        private val dir: File = TestWidgets.dir
        private val context = TestWidgets.context
    }

    @Test
    fun counterIsSavedInTheDesktopSettingsFile() {
        WidgetStore.init(context)
        WidgetStore.add(WidgetKind.COUNTER)
        val counter = WidgetStore.state.widgets.filterIsInstance<CounterWidget>().last()
        WidgetStore.changeCounter(counter.id, +3)
        assertEquals(3, WidgetStore.state.widgets.filterIsInstance<CounterWidget>().single { it.id == counter.id }.value)

        // Il salvataggio è su un filo a parte: si aspetta che il file arrivi.
        val file = File(dir, "widgets.properties")
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline && !(file.exists() && file.readText().contains(counter.id))) {
            Thread.sleep(50)
        }
        assertTrue("il contatore nel file dei widget", file.readText().contains(counter.id))
    }

    @Test
    fun emojiComeFromTheProgramResources() {
        assertNotNull("emoji.txt fra le risorse", readAppAsset(context, "emoji/emoji.txt"))
        assertNull(readAppAsset(context, "emoji/non-esiste.txt"))
        val catalog = runBlocking { EmojiCatalogs.load(context) }
        assertTrue("le categorie delle emoji", catalog.sections.size >= 5)
        assertTrue("la faccina che ride", catalog.sections.any { (_, entries) -> entries.any { it.emoji == "😀" } })
        assertTrue("la ricerca per nome", catalog.search("grinning").isNotEmpty())
    }

    @Test
    fun cacheFolderAndImagesFromBytes() {
        val cache = cacheDirOf(context)
        assertEquals(File(dir, "cache"), cache)
        assertTrue(cache.isDirectory)

        val png = ByteArrayOutputStream().also { ImageIO.write(BufferedImage(6, 4, BufferedImage.TYPE_INT_ARGB), "png", it) }.toByteArray()
        val bitmap = decodeImageBitmap(png)
        assertNotNull(bitmap)
        assertEquals(6, bitmap!!.width)
        assertEquals(4, bitmap.height)
        assertNull(decodeImageBitmap(byteArrayOf(1, 2, 3)))
    }
}
