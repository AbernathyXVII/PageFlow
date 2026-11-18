package com.gabriele.notionlocal.data.entity

// ---------------------------------------------------------------------------
// **I collegamenti dentro la pagina** (dal 20/10/2026, chiesto dall'utente:
// "un indice navigabile, come su Kindle"). Un pezzo di testo può portare a
// una riga della stessa pagina invece che a un sito: il suo `link` è
// `note://block/<id della riga>`. Il collegamento segue la riga, non il suo
// testo: rinominata o spostata, la si ritrova lo stesso.
// ---------------------------------------------------------------------------

/** L'inizio dei collegamenti a una riga della pagina. */
const val BLOCK_LINK_PREFIX = "note://block/"

/** Il collegamento a una riga della pagina. */
fun blockLink(blockId: String): String = BLOCK_LINK_PREFIX + blockId

/** La riga a cui porta un collegamento, o null se porta da un'altra parte. */
fun blockLinkTarget(link: String?): String? =
    link?.takeIf { it.startsWith(BLOCK_LINK_PREFIX) }?.removePrefix(BLOCK_LINK_PREFIX)?.takeIf { it.isNotBlank() }

/** Da quanti punti in su una riga scritta tutta così grande conta come un titolo. */
const val LARGE_TEXT_SIZE = 20

/** Il corpo del testo quando non ne è scelto uno (vedi `RichTextSpan.size`). */
private const val BODY_TEXT_SIZE = 16

/**
 * Una riga a cui si può collegare del testo: quelle col pin e quelle scritte
 * tutte in grande. [level] è quanto è rientrata, per l'elenco.
 */
data class LinkTarget(val blockId: String, val label: String, val pinned: Boolean, val level: Int)

/** Se tutto quello che c'è scritto sulla riga è in grande, da [LARGE_TEXT_SIZE] punti in su. */
fun isLargeLine(spans: List<RichTextSpan>): Boolean {
    val written = spans.filter { it.text.isNotBlank() }
    return written.isNotEmpty() && written.all { (it.size ?: BODY_TEXT_SIZE) >= LARGE_TEXT_SIZE }
}

/**
 * Le righe della pagina a cui si può collegare del testo, **nell'ordine in cui
 * si leggono**: ogni riga, poi quelle dentro di lei (un toggle), poi la
 * successiva.
 */
fun linkTargetsOf(
    blocks: List<BlockEntity>,
    spansOf: (BlockEntity) -> List<RichTextSpan>,
    /** Quali righe: di suo quelle col pin e quelle scritte tutte in grande. */
    include: (BlockEntity, List<RichTextSpan>) -> Boolean = { block, spans -> block.attrs.linkTarget || isLargeLine(spans) }
): List<LinkTarget> {
    val children = blocks.groupBy { it.parentBlockId }.mapValues { (_, list) -> list.sortedBy { it.orderIndex } }
    val targets = mutableListOf<LinkTarget>()
    fun visit(parentId: String?, depth: Int) {
        for (block in children[parentId].orEmpty()) {
            val spans = spansOf(block)
            val pinned = block.attrs.linkTarget
            if (include(block, spans)) {
                val label = spans.plainText().lineSequence().firstOrNull()?.trim().orEmpty()
                targets += LinkTarget(block.id, label, pinned, depth + block.indentLevel)
            }
            visit(block.id, depth + 1)
        }
    }
    visit(null, 0)
    return targets
}

/**
 * **Le voci dell'indice a destra della pagina** (dal 18/11/2026, chiesto dall'utente): solo i
 * blocchi segnati con "Flag (for Table of contents)" (`BlockAttrs.tocFlag`), nell'ordine in cui si
 * leggono. Prima c'erano tutte le destinazioni dei collegamenti, titoli grandi compresi.
 */
fun tocTargetsOf(blocks: List<BlockEntity>, spansOf: (BlockEntity) -> List<RichTextSpan>): List<LinkTarget> =
    linkTargetsOf(blocks, spansOf) { block, _ -> block.attrs.tocFlag }
