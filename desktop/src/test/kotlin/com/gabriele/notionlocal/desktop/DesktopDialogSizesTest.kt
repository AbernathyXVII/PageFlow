package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import com.gabriele.notionlocal.data.entity.Recurrence
import com.gabriele.notionlocal.data.entity.RecurrenceFreq
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.screen.CustomRecurrenceDialog
import com.gabriele.notionlocal.ui.screen.RepeatPresetDialog
import com.gabriele.notionlocal.ui.screen.SamsungTimeDialog
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalTime

/**
 * **Sul PC le finestre grandi quanto il contenuto** (12/11/2026, chiesto
 * dall'utente con le foto di "Set time", larga quanto la finestra, e di
 * "Repeat" e "Custom recurrence", piene di spazio vuoto). Si misura la
 * finestra nella foto: la sua larghezza va dal primo all'ultimo punto del suo
 * colore sulla riga appena sotto il bordo in alto.
 */
class DesktopDialogSizesTest {
    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }
    private val context = DesktopContext(Files.createTempDirectory("pageflow-dialogs").toFile())

    /** Larghezza e altezza della finestra al centro della foto, in punti. */
    private fun shoot(name: String, content: @Composable () -> Unit): Pair<Int, Int> {
        AppSettings.init(context)
        val scene = onWindowThread {
            ImageComposeScene(width = 1200, height = 800, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize().background(Color.Black)) { content() }
                    }
                }
            }
        }
        try {
            val picture = onWindowThread { scene.render(0L); scene.render(100_000_000L); Bitmap.makeFromImage(scene.render(200_000_000L)) }
            File("build/desktop-screenshots").mkdirs()
            File("build/desktop-screenshots", "$name.png").writeBytes(Image.makeFromBitmap(picture).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            val cx = picture.width / 2
            val backdrop = picture.getColor(5, 5)
            // Il bordo in alto: il primo punto, scendendo lungo il centro, che non è lo sfondo.
            val top = (0 until picture.height).first { y -> picture.getColor(cx, y) != backdrop }
            val bottom = (picture.height - 1 downTo 0).first { y -> picture.getColor(cx, y) != backdrop }
            // Sotto gli angoli arrotondati (10) e sopra il titolo.
            val y = top + 12
            val inside = picture.getColor(cx, y)
            var left = cx
            while (left > 0 && picture.getColor(left - 1, y) == inside) left--
            var right = cx
            while (right < picture.width - 1 && picture.getColor(right + 1, y) == inside) right++
            return (right - left + 1) to (bottom - top + 1)
        } finally {
            onWindowThread { scene.close() }
        }
    }

    @Test
    fun theDialogsAreAsLargeAsWhatTheyHold() {
        val start = LocalDate.of(2026, 11, 1)
        val (repeatWidth, repeatHeight) = shoot("dialog_repeat") {
            RepeatPresetDialog(start = start, current = null, onDismiss = {}, onPick = {}, onCustom = {})
        }
        val (customWidth, _) = shoot("dialog_custom_recurrence") {
            CustomRecurrenceDialog(start = start, initial = Recurrence(RecurrenceFreq.MONTHLY), onDismiss = {}, onDone = {})
        }
        val (timeWidth, timeHeight) = shoot("dialog_set_time") {
            SamsungTimeDialog(initial = LocalTime.of(12, 30), onPick = {}, onDismiss = {})
        }
        println("Repeat ${repeatWidth}x$repeatHeight, Custom recurrence $customWidth, Set time ${timeWidth}x$timeHeight")
        // Prima: Repeat e Custom recurrence larghe 560 (il massimo di Material), Set time quanto la finestra.
        assertTrue("Repeat larga $repeatWidth", repeatWidth in 230..360)
        assertTrue("Repeat alta $repeatHeight", repeatHeight < 340)
        assertTrue("Custom recurrence larga $customWidth", customWidth in 240..460)
        assertTrue("Set time larga $timeWidth", timeWidth in 250..270)
        assertTrue("Set time alta $timeHeight", timeHeight < 260)
    }
}
