package com.gabriele.notionlocal.data.patchnotes

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.ui.screen.inlineMarkdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * **Le Patch Notes** (30/09/2026): l'app ha la Cronologia del README, la
 * stessa, e ogni versione ha il suo numero, il giorno e l'ora, in ordine.
 *
 * È anche il controllo della Cronologia stessa: una voce nuova scritta
 * senza numero, con un numero sbagliato o con un'ora che torna indietro fa
 * fallire questo test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PatchNotesTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    /** La Cronologia presa dal README, come la prende il build. */
    private fun readmeChronology(): String {
        val text = File("../README.md").readText(Charsets.UTF_8).removePrefix("﻿")
        val start = text.indexOf("## Changelog")
        val end = text.indexOf("\n## ", start + 1)
        return text.substring(start, end)
    }

    private fun notes() = PatchNotesAsset.load(context)

    @Test
    fun theAppHasTheReadmeChronology() {
        val shipped = context.assets.open(PatchNotesAsset.FILE_NAME).bufferedReader().use { it.readText() }
        assertEquals(readmeChronology(), shipped)
        assertEquals("Changelog", notes().title)
        assertTrue(notes().intro.isNotEmpty())
    }

    @Test
    fun everyVersionHasItsNumberDayAndTime() {
        val versions = notes().versions
        assertTrue("versioni: ${versions.size}", versions.size >= 150)
        // Ogni titolo "### " della Cronologia è una versione letta.
        assertEquals(readmeChronology().lines().count { it.startsWith("### ") }, versions.size)
        // Dalla prima, v0.1, il giorno in cui nasce il progetto.
        val first = versions.last()
        assertEquals(PatchKind.RELEASE, first.kind)
        assertEquals("0.1", first.version)
        assertEquals("08/09/2026", first.date)
        assertEquals("09:19", first.time)
        versions.forEach { v ->
            assertEquals("${v.version}: ${v.emoji}", v.kind.emoji, v.emoji)
            assertTrue("${v.version}: vuota", v.blocks.isNotEmpty())
            // L'ora c'è, o si dice che non si sa; con l'ora, la data è un giorno preciso.
            if (v.time == UNKNOWN_TIME) return@forEach
            assertTrue("${v.version}: ${v.time}", Regex("""\d{2}:\d{2}""").matches(v.time))
            assertTrue("${v.version}: ${v.date}", Regex("""\d{2}/\d{2}/\d{4}""").matches(v.date))
        }
        // Le versioni senza ora sono solo le prime, dell'altra chat.
        val unknown = versions.filter { it.time == UNKNOWN_TIME }
        assertTrue(unknown.isNotEmpty() && unknown.all { versions.indexOf(it) > versions.size - 12 })
    }

    @Test
    fun theNumbersFollowEachOther() {
        // Dal 21/10/2026: un'aggiunta fa salire il secondo numero, una
        // modifica il terzo, una correzione il quarto (v0.8, v0.8.1, v0.8.1.1).
        var added = 0
        var changed = 0
        var fixed = 0
        notes().versions.asReversed().forEach { v ->
            val expected = when (v.kind) {
                PatchKind.RELEASE -> {
                    added++
                    changed = 0
                    fixed = 0
                    "0.$added"
                }
                PatchKind.CHANGE -> {
                    changed++
                    fixed = 0
                    "0.$added.$changed"
                }
                PatchKind.FIX -> {
                    fixed++
                    "0.$added.$changed.$fixed"
                }
            }
            assertEquals(v.title, expected, v.version)
        }
    }

    @Test
    fun everyItemSaysItsPlatformAndFitsItsVersion() {
        // Dal 21/10/2026: accanto all'etichetta di ogni voce la piattaforma
        // (una o più, o "All"), e le etichette sono quelle di adesso (in inglese dal 12/11/2026).
        val platforms = setOf("📱 Mobile", "💻 PC", "🍎 Mac", "🌐 Web App", "🌍 All")
        val item = Regex("""^\s*- \*\*\[([^\]]+)\]\*\*(?: \*\*\[([^\]]+)\]\*\*)?""")
        val kindOf = Regex("""^### \S+ v(\S+) — """)
        var kind: PatchKind? = null
        var version = ""
        readmeChronology().lines().forEach { line ->
            kindOf.find(line)?.let {
                version = it.groupValues[1]
                kind = PatchKind.of(version)
            }
            val m = item.find(line) ?: return@forEach
            val tag = m.groupValues[1]
            if (tag == "Project" || tag == "Note") return@forEach
            assertTrue("$version: etichetta \"$tag\"", tag in setOf("New feature", "Change", "Removed", "Bug fix"))
            val said = m.groupValues[2].split(" · ").filter { it.isNotEmpty() }
            assertTrue("$version: \"$tag\" senza piattaforma", said.isNotEmpty() && said.all { it in platforms })
            // Una correzione corregge soltanto; una modifica non aggiunge.
            if (kind == PatchKind.FIX) assertEquals("$version: $line", "Bug fix", tag)
            if (kind == PatchKind.CHANGE) assertTrue("$version: $line", tag != "New feature")
        }
    }

    @Test
    fun theNewestIsOnTopAndTimeNeverGoesBack() {
        val format = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        val times = notes().versions.filter { it.time != UNKNOWN_TIME }.map { LocalDateTime.parse("${it.date} ${it.time}", format) }
        times.zipWithNext().forEach { (newer, older) -> assertTrue("$older dopo $newer", !older.isAfter(newer)) }
    }

    @Test
    fun theAppVersionIsTheNewest() {
        val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        assertEquals(notes().versions.first().version, versionName)
    }

    @Test
    fun theMarkdownBecomesBlocks() {
        val notes = parsePatchNotes(
            """
            ## Changelog

            A line
            broken by hand.

            ### 🆕 v0.2 — 25/09/2026 · 08:25 — Il titolo
            *(Claude Code on the PC; verified)*

            The user, word for word:
            > «Prima riga
            > seconda riga»

            - **[New feature]** An item
              that continues below
              - An indented item
            1. Numerata
              | a | b |
              |---|---|
              | 1 | 2 |

            ### ✏️ v0.1.1 — 24/09/2026 · 23:30 — Una modifica
            - **[Change]** **[📱 Mobile]** Changed

            ### 🔧 v0.1.0.1 — 24/09/2026 · 23:17 — Una correzione
            - **[Bug fix]** Corretto
            """.trimIndent()
        )
        assertEquals(listOf(NoteBlock.Paragraph("A line broken by hand.")), notes.intro)
        val (release, change, fix) = notes.versions
        assertEquals(PatchKind.CHANGE, change.kind)
        assertEquals("0.1.1", change.version)
        assertEquals("✏️", change.emoji)
        assertEquals("0.1.0.1", fix.version)
        assertEquals(PatchKind.RELEASE, release.kind)
        assertEquals("0.2", release.version)
        assertEquals("25/09/2026", release.date)
        assertEquals("08:25", release.time)
        assertEquals("Il titolo", release.title)
        assertEquals(
            listOf(
                NoteBlock.Paragraph("*(Claude Code on the PC; verified)*"),
                NoteBlock.Paragraph("The user, word for word:"),
                NoteBlock.Quote("«Prima riga seconda riga»"),
                NoteBlock.Bullet(0, "**[New feature]** An item that continues below"),
                NoteBlock.Bullet(1, "An indented item"),
                NoteBlock.Bullet(0, "Numerata", marker = "1."),
                NoteBlock.Table(1, listOf(listOf("a", "b"), listOf("1", "2")))
            ),
            release.blocks
        )
        assertEquals(PatchKind.FIX, fix.kind)
        assertEquals(listOf(NoteBlock.Bullet(0, "**[Bug fix]** Corretto")), fix.blocks)
    }

    @Test
    fun boldItalicStrikeAndCodeInsideALine() {
        val text = inlineMarkdown("a **grassetto** *corsivo* ~~via~~ `codice` 2 * 3")
        assertEquals("a grassetto corsivo via codice 2 * 3", text.text)
        fun styleOf(word: String) = text.spanStyles.filter { text.text.indexOf(word) in it.start until it.end }.map { it.item }
        assertTrue(styleOf("grassetto").any { it.fontWeight == FontWeight.Bold })
        assertTrue(styleOf("corsivo").any { it.fontStyle == FontStyle.Italic })
        assertTrue(styleOf("via").any { it.textDecoration == TextDecoration.LineThrough })
        assertTrue(styleOf("codice").any { it.fontFamily != null })
        // L'asterisco fra due spazi resta un asterisco e non accende il corsivo.
        assertTrue(styleOf("3").none { it.fontStyle == FontStyle.Italic })
        // Il grassetto dentro il corsivo, come nelle righe "*(… **31** …)*".
        val nested = inlineMarkdown("*(schema alla **31**)*")
        assertEquals("(schema alla 31)", nested.text)
        assertTrue(nested.spanStyles.any { it.item.fontWeight == FontWeight.Bold && it.item.fontStyle == FontStyle.Italic })
    }

    @Test
    fun theReviewTagIsRed() {
        // 18/10/2026, chiesto dall'utente: le scelte da rivedere, in rosso e in grassetto.
        val text = inlineMarkdown("**🔴 [To review]** Come Notion")
        val tag = text.text.indexOf("[To review]")
        val styles = text.spanStyles.filter { tag in it.start until it.end }.map { it.item }
        assertTrue(styles.any { it.color != Color.Unspecified && it.fontWeight == FontWeight.Bold })
        // Il resto no.
        assertTrue(text.spanStyles.filter { text.text.indexOf("Come") in it.start until it.end }.none { it.item.color != Color.Unspecified })
    }
}
