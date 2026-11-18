package com.gabriele.notionlocal.data.entity

import com.gabriele.notionlocal.ui.screen.lineStartIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **L'indice navigabile** (20/10/2026): i collegamenti a una riga della pagina
 * e le righe a cui ci si può collegare.
 */
class BlockLinksTest {

    /** Il testo di ogni riga, per id. */
    private val texts = mutableMapOf<String, List<RichTextSpan>>()

    private fun line(id: String, text: String, order: Int, parent: String? = null, pinned: Boolean = false, size: Int? = null, flagged: Boolean = false): BlockEntity {
        texts[id] = listOf(RichTextSpan(text, size = size))
        return BlockEntity(
            id = id,
            pageId = "p",
            parentBlockId = parent,
            orderIndex = order,
            type = BlockType.PARAGRAPH,
            attrsJson = BlockAttrs.encode(BlockAttrs(linkTarget = pinned, tocFlag = flagged))
        )
    }

    private val spans = { block: BlockEntity -> texts[block.id].orEmpty() }

    @Test
    fun aLinkToALineAndBack() {
        assertEquals("abc", blockLinkTarget(blockLink("abc")))
        assertNull(blockLinkTarget("https://google.com"))
        assertNull(blockLinkTarget(BLOCK_LINK_PREFIX))
        assertNull(blockLinkTarget(null))
    }

    @Test
    fun aLineCountsAsLargeOnlyIfAllOfItIs() {
        assertTrue(isLargeLine(listOf(RichTextSpan("Capitolo 1", size = 24))))
        assertFalse(isLargeLine(listOf(RichTextSpan("Capitolo ", size = 24), RichTextSpan("uno"))))
        // Gli spazi non contano.
        assertTrue(isLargeLine(listOf(RichTextSpan("Titolo", size = 20), RichTextSpan("  "))))
        assertFalse(isLargeLine(listOf(RichTextSpan("normale"))))
        assertFalse(isLargeLine(emptyList()))
    }

    /**
     * **L'indice a destra: solo i blocchi segnati** (18/11/2026, chiesto dall'utente: "un'opzione
     * specifica chiamata Flag (for Table of content)"): né i titoli grandi né le righe col pin, da soli.
     */
    @Test
    fun theTableOfContentsHasOnlyTheFlaggedLines() {
        val blocks = listOf(
            line("titolo", "Kapitel 1", 0, size = 24),
            line("pin", "Wortschatz", 1, pinned = true),
            line("segnato", "Grammatik", 2, size = 24, flagged = true),
            line("toggle", "Übungen", 3),
            line("dentro", "Verben", 0, parent = "toggle", flagged = true)
        )
        assertEquals(listOf("segnato", "dentro"), tocTargetsOf(blocks, spans).map { it.blockId })
        assertEquals(listOf("Grammatik", "Verben"), tocTargetsOf(blocks, spans).map { it.label })
        // Le destinazioni dei collegamenti restano quelle di prima.
        assertEquals(listOf("titolo", "pin", "segnato"), linkTargetsOf(blocks, spans).map { it.blockId })
    }

    @Test
    fun theTargetsInReadingOrder() {
        val blocks = listOf(
            line("c", "Capitolo 2", 2, size = 24),
            line("a", "Indice", 0),
            line("b", "Capitolo 1", 1, pinned = true),
            line("t", "Toggle", 3),
            line("n", "Dentro il toggle", 0, parent = "t", pinned = true)
        )
        val targets = linkTargetsOf(blocks, spans)
        assertEquals(listOf("b", "c", "n"), targets.map { it.blockId })
        assertEquals(LinkTarget("b", "Capitolo 1", pinned = true, level = 0), targets[0])
        assertFalse(targets[1].pinned)
        assertEquals(1, targets[2].level)
    }

    @Test
    fun blocksSavedBeforeAreNotTargets() {
        assertFalse(BlockAttrs.parse("""{"icon":"💡"}""").linkTarget)
        assertTrue(BlockAttrs.parse("""{"linkTarget":true}""").linkTarget)
    }

    @Test
    fun whereALineStartsInAGroup() {
        val shown = "• uno\n• due\ntre"
        assertEquals(0, lineStartIn(shown, 0))
        assertEquals(6, lineStartIn(shown, 1))
        assertEquals(12, lineStartIn(shown, 2))
        assertEquals(shown.length, lineStartIn(shown, 5))
    }
}
