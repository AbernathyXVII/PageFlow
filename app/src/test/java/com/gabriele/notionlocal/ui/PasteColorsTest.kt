package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.applyPaste
import com.gabriele.notionlocal.ui.screen.LinkClipboard
import com.gabriele.notionlocal.ui.screen.PasteMode
import com.gabriele.notionlocal.ui.screen.htmlStyleRuns
import com.gabriele.notionlocal.ui.screen.parseClipHtml
import com.gabriele.notionlocal.ui.screen.pastedFormat
import com.gabriele.notionlocal.ui.screen.spansToHtml
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **I colori del testo copiato arrivano con "Keep Source Formatting"**
 * (17/11/2026, chiesto dall'utente: "quando copio qualcosa da una fonte che
 * abbia dei colori e li incollo, non li prende"): il colore del testo e
 * l'evidenziato, come li scrivono Word, OneNote e un browser; il nero, il
 * grigio scuro e il bianco di sempre della fonte no.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PasteColorsTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    // Come un browser: il paragrafo col grigio scuro e il bianco della pagina; un rosso, un evidenziato di Word, un <font> blu.
    private val html = "<html><body><!--StartFragment--><p style=\"color:rgb(32, 33, 36);background-color:rgb(255, 255, 255)\">" +
        "<span style=\"color:#C00000\">Rosso</span> normale <span style='background:yellow;mso-highlight:yellow'>giallo</span> " +
        "<font color=\"#00f\">blu</font></p><!--EndFragment--></body></html>"

    private fun pasteKeepingSource(): AnnotatedString {
        context.getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newHtmlText("Word", "Rosso normale giallo blu", html))
        val base = object : androidx.compose.ui.platform.ClipboardManager {
            override fun getText(): AnnotatedString? = null
            override fun setText(annotatedString: AnnotatedString) = Unit
        }
        return LinkClipboard(context, base).apply { nextPasteMode = PasteMode.KEEP_SOURCE }.getText()!!
    }

    @Test
    fun theColorsOfTheSourceArePasted() {
        val pasted = pasteKeepingSource()
        assertEquals("Rosso normale giallo blu", pasted.text)
        val spans = applyPaste(listOf(RichTextSpan(pasted.text, color = "#123456")), TextFieldValue(pasted).pastedFormat(0))
        assertEquals(
            listOf(
                RichTextSpan("Rosso", color = "#C00000"),
                RichTextSpan(" normale "),
                RichTextSpan("giallo", background = "#FFFF00"),
                RichTextSpan(" "),
                RichTextSpan("blu", color = "#0000FF")
            ),
            spans
        )
    }

    @Test
    fun copiedFromPageFlowTheColorsComeBack() {
        val copied = spansToHtml(listOf(RichTextSpan("A", color = "#E03E3E"), RichTextSpan("B", bold = true, background = "#FBF3DB")))
        val runs = parseClipHtml(copied).single().runs
        assertEquals(listOf("#E03E3E" to null, null to "#FBF3DB"), runs.map { it.color to it.background })
        // Anche dove si cercano solo gli stili (righe copiate col loro tipo).
        assertEquals(listOf("#E03E3E" to null, null to "#FBF3DB"), htmlStyleRuns(copied).map { it.color to it.background })
    }

    @Test
    fun theUsualColorsOfTheSourceStayOut() {
        // Il nero di Word, il trasparente, il bianco, un grigio scuro: niente. Un grigio a metà resta.
        val html = "<p><span style='color:windowtext;background:white'>a</span> <span style='color:#333333;background-color:transparent'>b</span> " +
            "<span style='color:rgba(255, 0, 0, 0)'>c</span> <span style='color:#888888'>d</span></p>"
        val runs = parseClipHtml(html).single().runs
        assertEquals(listOf("a b c " to (null to null), "d" to ("#888888" to null)), runs.map { it.text to (it.color to it.background) })
        // Lo sfondo di un paragrafo o di una cella è la pagina della fonte, non un evidenziato.
        assertEquals(null, parseClipHtml("<div style='background-color:#FFE599'>testo</div>").single().runs.single().background)
    }
}
