package com.gabriele.notionlocal.data.entity

/**
 * **Al massimo quattro colonne in un gruppo** (dal 12/11/2026, chiesto
 * dall'utente: "ho già creato nove colonne solo spostando i blocchi, ne
 * vorrei solo quattro"): come le voci dei menu, che arrivano a quattro.
 */
const val MAX_COLUMNS = 4

/**
 * Quante colonne avrebbe il gruppo di [targetId] mettendogli accanto
 * [draggedId] (vedi `PageRepository.moveBlockBeside`): una in più, meno
 * quella che il blocco lascia vuota se viene dallo stesso gruppo. Null se
 * accanto a [targetId] nasce un gruppo nuovo, di due colonne.
 */
fun columnsAfterBeside(blocks: List<BlockEntity>, draggedId: String, targetId: String): Int? {
    val byId = blocks.associateBy { it.id }
    val target = byId[targetId] ?: return null
    val holder = target.parentBlockId?.let { byId[it] }
    val group = when {
        target.type == BlockType.COLUMN_LIST -> target
        target.type == BlockType.COLUMN -> holder
        holder?.type == BlockType.COLUMN -> holder.parentBlockId?.let { byId[it] }
        else -> null
    } ?: return null
    val columns = blocks.count { it.parentBlockId == group.id && it.type == BlockType.COLUMN }
    val from = byId[draggedId]?.parentBlockId?.let { byId[it] }
    val freed = from != null && from.type == BlockType.COLUMN && from.parentBlockId == group.id &&
        blocks.count { it.parentBlockId == from.id } == 1
    return columns + 1 - if (freed) 1 else 0
}
