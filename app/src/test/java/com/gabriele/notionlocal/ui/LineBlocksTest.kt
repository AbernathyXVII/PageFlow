package com.gabriele.notionlocal.ui

import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.ui.screen.blocksOfLines
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Mentre il database è indietro di un Invio o di un backspace, ogni riga
 * del campo resta col suo blocco: la formattazione non salta di riga.
 */
class LineBlocksTest {

    private fun block(text: String) = BlockEntity(pageId = "p", textJson = text)
    private fun textOf(block: BlockEntity) = block.textJson

    @Test
    fun anEnterOnTheFirstEmptyLineLeavesTheStyledLinesWithTheirBlocks() {
        val blocks = listOf(block(""), block("Titolo"), block("Paragrafo"), block("acqua"))
        val lines = listOf("", "", "Titolo", "Paragrafo", "acqua")
        val aligned = blocksOfLines(lines, blocks, ::textOf)
        assertEquals(listOf("Titolo", "Paragrafo", "acqua"), aligned.blocks.drop(2).map { it!!.textJson })
        assertEquals(1, aligned.blocks.count { it == null })
    }

    @Test
    fun aLineSplitInTwoIsFollowedByTheRightBlocks() {
        val blocks = listOf(block("Paragrafo di prova"), block("acqua"))
        val aligned = blocksOfLines(listOf("Paragrafo", " di prova", "acqua"), blocks, ::textOf)
        assertEquals(1, aligned.insertedAt)
        assertEquals(listOf("Paragrafo di prova", null, "acqua"), aligned.blocks.map { it?.textJson })
    }

    @Test
    fun aBackspaceThatJoinsTwoLinesLeavesTheLinesBelowWithTheirBlocks() {
        val blocks = listOf(block(""), block("Titolo"), block("Paragrafo"), block("acqua"))
        val aligned = blocksOfLines(listOf("Titolo", "Paragrafo", "acqua"), blocks, ::textOf)
        assertEquals(listOf("Titolo", "Paragrafo", "acqua"), aligned.blocks.map { it!!.textJson })
        assertEquals(0, aligned.removedAt)
    }

    @Test
    fun twoQuickEntersAheadOfTheDatabaseStillLeaveTheStyledLinesWithTheirBlocks() {
        val blocks = listOf(block(""), block("Titolo"), block("Paragrafo"))
        val aligned = blocksOfLines(listOf("", "", "", "Titolo", "Paragrafo"), blocks, ::textOf)
        assertEquals(listOf("Titolo", "Paragrafo"), aligned.blocks.takeLast(2).map { it!!.textJson })
        assertEquals(2, aligned.blocks.count { it == null })
    }

    @Test
    fun aMergeKeepsTheFirstOfTheTwoBlocksOnTheJoinedLine() {
        val blocks = listOf(block("A"), block("B"), block("C"))
        val aligned = blocksOfLines(listOf("AB", "C"), blocks, ::textOf)
        assertEquals(listOf("A", "C"), aligned.blocks.map { it!!.textJson })
        assertEquals(1, aligned.removedAt)
    }

    /**
     * Fra righe vuote tutte uguali ogni riga tiene il suo blocco, come farà il database (dal
     * 17/11/2026): le nuove dopo quelle che c'erano, la riga unita via è quella sotto.
     */
    @Test
    fun amongEmptyLinesEachLineKeepsItsOwnBlock() {
        val blocks = listOf(block("Uno")) + (1..3).map { block("") }
        // L'ultima unita a quella sopra: le altre restano le loro.
        assertEquals(blocks.take(3), blocksOfLines(listOf("Uno", "", ""), blocks, ::textOf).blocks)
        // Due Invio in fondo, il database ancora indietro: le nuove sono le ultime.
        assertEquals(blocks + listOf(null, null), blocksOfLines(listOf("Uno", "", "", "", "", ""), blocks, ::textOf).blocks)
    }

    @Test
    fun withTheSameNumberOfLinesEachLineHasTheBlockInItsPlace() {
        val blocks = listOf(block("a"), block("b"))
        assertEquals(blocks, blocksOfLines(listOf("a", "bb"), blocks, ::textOf).blocks)
    }
}
