package com.gabriele.notionlocal.ui

import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.ui.screen.ClipLine
import com.gabriele.notionlocal.ui.screen.noteSizes
import com.gabriele.notionlocal.ui.screen.parseClipHtml
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **"Keep Source Formatting" legge com'era fatto il testo** (20/10/2026): righe,
 * elenchi, tabelle e grandezze dall'HTML degli appunti, come lo mettono OneNote
 * e un browser.
 */
class ClipHtmlTest {

    /** Come copia OneNote: il carattere sul body, ogni paragrafo col suo, gli elenchi uno dentro l'altro, una tabella. */
    private val oneNote = """
        <html><head><style>p { margin:0 }</style></head>
        <body lang=it style='font-family:Calibri;font-size:11.0pt'>
        <!--StartFragment-->
        <div style='direction:ltr'>
        <p style='margin:0in;font-family:Calibri;font-size:20.0pt'>Capitolo 1</p>
        <p style='margin:0in;font-family:Calibri;font-size:11.0pt'>Testo <span style='font-weight:bold'>importante</span></p>
        <p style='margin:0in;font-family:Calibri;font-size:11.0pt'>&nbsp;</p>
        <ul type=disc style='direction:ltr;margin-top:0in;margin-bottom:0in'>
         <li style='margin-top:0;margin-bottom:0'><span style='font-family:Calibri;font-size:11.0pt'>Primo</span></li>
         <ul type=circle><li><span style='font-size:11.0pt'>Sotto</span></li></ul>
        </ul>
        <ol type=1><li value=1><span style='font-size:11.0pt'>Uno</span></li><li><span style='font-size:11.0pt'>Due</span></li></ol>
        <table border=1 cellpadding=0 cellspacing=0>
         <tr><td><p style='margin:0in;font-size:11.0pt'>A</p></td><td><p style='margin:0in;font-size:11.0pt'>B</p></td></tr>
         <tr><td><p style='margin:0in;font-size:11.0pt'>C</p></td><td><p style='margin:0in;font-size:11.0pt'>D<br>E</p></td></tr>
        </table>
        </div>
        <!--EndFragment-->
        </body></html>
    """.trimIndent()

    private fun shape(line: ClipLine) = "${line.text}|${line.type}|${line.indent}"

    @Test
    fun oneNoteLinesListsAndTable() {
        val lines = parseClipHtml(oneNote)
        assertEquals(
            listOf(
                "Capitolo 1|PARAGRAPH|0",
                "Testo importante|PARAGRAPH|0",
                "|PARAGRAPH|0",
                "Primo|BULLET_LIST_ITEM|0",
                "Sotto|BULLET_LIST_ITEM|1",
                "Uno|NUMBERED_LIST_ITEM|0",
                "Due|NUMBERED_LIST_ITEM|0",
                "A | B|TABLE|0"
            ),
            lines.map { shape(it) }
        )
        assertEquals(listOf(listOf("A", "B"), listOf("C", "D\nE")), lines.last().table)
        // Il grassetto sul suo pezzo.
        assertEquals(listOf("Testo " to false, "importante" to true), lines[1].runs.map { it.text to it.bold })
    }

    @Test
    fun theSizesFollowNoteAndTheSourceBody() {
        val lines = parseClipHtml(oneNote)
        val sizeOf = noteSizes(lines)
        // Il testo normale di OneNote (11 punti) resta normale; il titolo a 20 è grande quanto lì.
        assertNull(sizeOf(lines[1].runs.first().sizePt))
        assertEquals(29, sizeOf(lines[0].runs.first().sizePt))
    }

    @Test
    fun aWebPageWithHeadingsListsLinksAndATable() {
        val html = """
            <h2>Ricette</h2><p>Pasta <a href="https://example.com/pasta">al pomodoro</a></p>
            <ol><li>Acqua</li><li>Sale<ol><li>grosso</li></ol></li></ol>
            <table><thead><tr><th>Nome</th><th>Tempo</th></tr></thead>
            <tbody><tr><td>Pasta</td><td>10 min</td></tr></tbody></table>
        """.trimIndent()
        val lines = parseClipHtml(html)
        assertEquals(
            listOf(
                "Ricette|PARAGRAPH|0",
                "Pasta al pomodoro|PARAGRAPH|0",
                "Acqua|NUMBERED_LIST_ITEM|0",
                "Sale|NUMBERED_LIST_ITEM|0",
                "grosso|NUMBERED_LIST_ITEM|1",
                "Nome | Tempo|TABLE|0"
            ),
            lines.map { shape(it) }
        )
        assertEquals(listOf(listOf("Nome", "Tempo"), listOf("Pasta", "10 min")), lines.last().table)
        assertEquals("https://example.com/pasta", lines[1].runs.last().link)
        // Il titolo è in grassetto e, col testo a 12 punti dei browser, grande una volta e mezza.
        assertTrue(lines[0].runs.single().bold)
        assertEquals(24, noteSizes(lines)(lines[0].runs.single().sizePt))
        assertNull(noteSizes(lines)(lines[1].runs.first().sizePt))
    }

    @Test
    fun aOneCellTableIsJustText() {
        val lines = parseClipHtml("<p>Prima</p><table><tr><td>Solo <b>questo</b></td></tr></table><p>Dopo</p>")
        assertEquals(listOf("Prima|PARAGRAPH|0", "Solo questo|PARAGRAPH|0", "Dopo|PARAGRAPH|0"), lines.map { shape(it) })
    }

    /**
     * **Le immagini fra le righe copiate** (18/11/2026, chiesto dall'utente): ognuna una riga sua al
     * suo posto, da un file accanto (OneNote, dentro `<![if !vml]>`) o scritta nell'HTML; da un
     * indirizzo web no. Una voce d'elenco con un'immagine in mezzo resta voce prima e dopo.
     */
    @Test
    fun theImagesAreLinesOfTheirOwn() {
        val html = "<body style='font-size:11.0pt'><!--StartFragment-->" +
            "<p>Prima</p>" +
            "<p><!--[if gte vml 1]><v:imagedata src=\"file:///C:/x/clip_image001.png\"/><![endif]--><![if !vml]>" +
            "<img width=120 height=60 src=\"file:///C:/Users/Me/AppData/Local/Temp/msohtmlclip1/01/clip_image002.png\"><![endif]></p>" +
            "<p>Testo <img src='data:image/png;base64,iVBORw0KGgo='> dopo</p>" +
            "<p><img src=\"https://example.com/foto.png\">Dal web</p>" +
            "<ul><li>Voce <img src=\"file:///C:/a.png\"> continua</li></ul>" +
            "<!--EndFragment--></body>"
        val lines = parseClipHtml(html)
        assertEquals(
            listOf(
                "Prima|PARAGRAPH|0",
                "Image|IMAGE|0",
                "Testo|PARAGRAPH|0",
                "Image|IMAGE|0",
                "dopo|PARAGRAPH|0",
                "Dal web|PARAGRAPH|0",
                "Voce|BULLET_LIST_ITEM|0",
                "Image|IMAGE|0",
                "continua|BULLET_LIST_ITEM|0"
            ),
            lines.map { shape(it) }
        )
        assertEquals(
            listOf("file:///C:/Users/Me/AppData/Local/Temp/msohtmlclip1/01/clip_image002.png", "data:image/png;base64,iVBORw0KGgo=", "file:///C:/a.png"),
            lines.mapNotNull { it.image }
        )
        // Il segnaposto non conta per la grandezza del testo.
        assertNull(noteSizes(lines)(11f))
    }

    @Test
    fun spacesAndEmptyLinesAtTheEdgesAreNotText() {
        val lines = parseClipHtml("<div>\n  <p>&nbsp;</p>\n  <p>  Ciao   mondo  </p><br>\n</div>")
        assertEquals(listOf("Ciao mondo"), lines.map { it.text })
        // Senza grandezze nella fonte, nessuna grandezza.
        assertNull(noteSizes(lines)(lines.single().runs.single().sizePt))
    }
}
