package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.ui.screen.PcContextMenu
import com.gabriele.notionlocal.ui.screen.PcMenuItem
import com.gabriele.notionlocal.ui.screen.SubmenuBesideItem
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * **Il sottomenu del tasto destro accanto alla sua voce** (17/11/2026,
 * trovato dall'utente: aperto in fondo alla finestra, "la finestra di destra è
 * completamente staccata" e andandoci col mouse si chiudeva): centrato in
 * altezza sulla voce, dentro la finestra.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopSubmenuTest {
    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    @Test
    fun theSubmenuIsCenteredOnItsItemAndInsideTheWindow() {
        val window = IntSize(800, 600)
        // In mezzo: centrato sulla voce, a destra.
        assertEquals(IntOffset(300, 410 - 100), SubmenuBesideItem.calculatePosition(IntRect(100, 395, 300, 425), window, LayoutDirection.Ltr, IntSize(200, 200)))
        // In fondo: tirato su fin dentro la finestra, ma sempre sopra la voce.
        assertEquals(IntOffset(300, 600 - 300), SubmenuBesideItem.calculatePosition(IntRect(100, 540, 300, 570), window, LayoutDirection.Ltr, IntSize(200, 300)))
        // A destra non ci sta: a sinistra del menu.
        assertEquals(IntOffset(450, 410 - 100), SubmenuBesideItem.calculatePosition(IntRect(650, 395, 790, 425), window, LayoutDirection.Ltr, IntSize(200, 200)))
    }

    @Test
    fun openedAtTheBottomTheSubmenuCoversItsItem() {
        val submenu = (1..8).map { PcMenuItem("Voce $it", onClick = {}) }
        val entries = (1..10).map { PcMenuItem("Riga $it", onClick = {}) } + PcMenuItem("Text formatting", submenu = submenu)
        val scene = onWindowThread {
            ImageComposeScene(width = 700, height = 470, density = Density(1f)) {
                NotionLocalTheme {
                    Box(Modifier.fillMaxSize().background(Color.Black)) {
                        // Il menu si apre in basso: la sua ultima voce è vicina al fondo della finestra.
                        Box(Modifier.offset(40.dp, 60.dp).size(0.dp)) { PcContextMenu(entries, onDismiss = {}) }
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
        try {
            frames(20)
            val menu = picture("submenu_closed.png")
            val black = menu.getColor(690, 5)
            // Il menu: le colonne di x dove a metà altezza non è nero; la voce col sottomenu è la sua ultima riga.
            val menuXs = (0 until 700).filter { x -> menu.getColor(x, 200) != black }
            assertTrue("menu: $menuXs", menuXs.isNotEmpty())
            val menuRight = menuXs.last()
            val menuRows = (0 until 470).filter { y -> menu.getColor(menuXs.first() + 20, y) != black }
            val itemY = menuRows.last() - 8 - 15
            onWindowThread { scene.sendPointerEvent(PointerEventType.Move, Offset(menuXs.first() + 60f, itemY.toFloat()), type = PointerType.Mouse) }
            frames(10)
            val open = picture("submenu_open.png")
            // Il sottomenu: a destra del menu, dove prima era nero; dall'alto al basso deve passare per la voce.
            val column = menuRight + 40
            val submenuYs = (0 until 470).filter { y -> open.getColor(column, y) != black }
            assertTrue("sottomenu $submenuYs, voce a $itemY", submenuYs.isNotEmpty() && itemY in submenuYs.first()..submenuYs.last())
            assertTrue("dentro la finestra: ${submenuYs.last()}", submenuYs.last() < 470)
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
