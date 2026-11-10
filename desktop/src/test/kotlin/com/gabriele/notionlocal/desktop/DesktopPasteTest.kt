package com.gabriele.notionlocal.desktop

import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.text.AnnotatedString
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.platform.HtmlSelection
import com.gabriele.notionlocal.ui.screen.LinkClipboard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.datatransfer.DataFlavor

/**
 * **Incollare coi link sul PC** (dal 10/11/2026), con degli appunti finti (i
 * veri, di Windows, restano come sono): un pezzo di pagina web arriva col suo
 * link sulla parola giusta; copiando un testo in grassetto, negli appunti va
 * anche il suo HTML.
 */
class DesktopPasteTest {
    private val system = java.awt.datatransfer.Clipboard("prova")

    private val base = object : ClipboardManager {
        override fun getText(): AnnotatedString? = null
        override fun setText(annotatedString: AnnotatedString) = Unit
    }

    private val compose = object : Clipboard {
        override val nativeClipboard: NativeClipboard get() = system
        override suspend fun getClipEntry(): ClipEntry? = null
        override suspend fun setClipEntry(clipEntry: ClipEntry?) = Unit
    }

    @Test
    fun aLinkFromTheBrowserArrivesOnItsWord() {
        val html = "<html><body><!--StartFragment-->Vai su <a href=\"https://www.notion.so\">Notion</a> oggi<!--EndFragment--></body></html>"
        system.setContents(HtmlSelection("Vai su Notion oggi", html), null)
        val pasted = LinkClipboard(base, compose, system = system).getText()!!
        assertEquals("Vai su Notion oggi", pasted.text)
        val link = pasted.getStringAnnotations(0, pasted.length).single { it.tag == "pasted-link" }
        assertEquals("https://www.notion.so", link.item)
        assertEquals("Notion", pasted.text.substring(link.start, link.end))
    }

    @Test
    fun copyingBoldTextPutsItsHtmlToo() {
        val spans = listOf(RichTextSpan("ciao ", bold = true), RichTextSpan("mondo"))
        LinkClipboard(base, compose, richSelection = { spans }, system = system).setText(AnnotatedString("ciao mondo"))
        val contents = system.getContents(null)!!
        assertEquals("ciao mondo", contents.getTransferData(DataFlavor.stringFlavor))
        val html = contents.getTransferData(DataFlavor("text/html;class=java.lang.String")) as String
        assertTrue(html, html.contains("ciao") && (html.contains("<b>") || html.contains("font-weight")))
    }
}
