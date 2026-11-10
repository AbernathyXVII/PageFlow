package com.gabriele.notionlocal.data.sync

/**
 * **Una pagina cambiata da tutte e due le parti si unisce riga per riga**
 * (dal 10/11/2026, chiesto dall'utente): invece di scegliere la pagina intera
 * più recente, si guardano uno per uno i suoi pezzi — la pagina stessa
 * (titolo, icona, impostazioni), ogni blocco con le celle della sua tabella,
 * ogni tratto di penna, colonna e automazione — e per ognuno vale la regola
 * delle pagine: cambiato da una parte sola vince quella, cancellato da una
 * parte e uguale dall'altra se ne va, nuovo da una parte resta. Solo un pezzo
 * cambiato da tutte e due le parti è un conflitto: vince la parte modificata
 * per ultima, e l'altra versione resta nella cronologia della pagina.
 *
 * Com'era ogni pezzo all'ultima sincronizzazione lo dicono [base] (le
 * impronte di `elementHashes`): senza, la prima volta, niente si cancella.
 */
object SyncMerge {

    data class Merged(val unit: SyncUnit, val conflicts: Int)

    private class Element(val rows: Map<String, List<SyncRow>>, val mine: Boolean) {
        val hash: String by lazy { shortHash(rows) }
    }

    /** Le impronte dei pezzi di una pagina, da ricordare dopo la sincronizzazione. */
    fun elementHashes(unit: SyncUnit): Map<String, String> = elements(unit, mine = true).mapValues { it.value.hash }

    fun merge(mine: SyncUnit, theirs: SyncUnit, base: Map<String, String>, mineIsNewer: Boolean): Merged {
        val here = elements(mine, mine = true)
        val there = elements(theirs, mine = false)
        val chosen = ArrayList<Element>()
        var conflicts = 0
        for (key in here.keys + there.keys) {
            val a = here[key]
            val b = there[key]
            val before = base[key]
            val pick = when {
                a?.hash == b?.hash -> a
                a == null -> if (before != null && b!!.hash == before) null else b
                b == null -> if (before != null && a.hash == before) null else a
                a.hash == before -> b
                b.hash == before -> a
                else -> {
                    conflicts++
                    if (mineIsNewer) a else b
                }
            }
            if (pick != null) chosen += pick
        }
        val tables = LinkedHashMap<String, MutableList<SyncRow>>()
        val origin = HashMap<String, Boolean>()
        for (element in chosen) {
            for ((table, rows) in element.rows) {
                tables.getOrPut(table) { ArrayList() }.addAll(rows)
                if (table == "blocks") rows.forEach { origin[it["id"] as String] = element.mine }
            }
        }
        tables["blocks"]?.let { tables["blocks"] = tidyBlocks(it, origin).toMutableList() }
        return Merged(SyncUnit(mine.id, tables), conflicts)
    }

    /**
     * I blocchi messi insieme: uno dentro un blocco che non c'è più va al
     * primo livello, e dove le due parti hanno usato lo stesso posto (una riga
     * nuova in fondo di qua e una di là) i fratelli si rinumerano, prima i
     * nostri, senza toccare gli altri gruppi.
     */
    private fun tidyBlocks(blocks: List<SyncRow>, origin: Map<String, Boolean>): List<SyncRow> {
        val ids = blocks.mapTo(HashSet()) { it["id"] as String }
        val placed = blocks.map { row ->
            val parent = row["parentBlockId"] as String?
            if (parent != null && parent !in ids) LinkedHashMap(row).apply { put("parentBlockId", null) } else row
        }
        val result = ArrayList<SyncRow>()
        for ((_, siblings) in placed.groupBy { it["parentBlockId"] as String? }) {
            val order = siblings.map { (it["orderIndex"] as? Long) ?: 0L }
            if (order.size == order.toSet().size) {
                result += siblings
                continue
            }
            siblings.sortedWith(
                compareBy<SyncRow>({ (it["orderIndex"] as? Long) ?: 0L }, { if (origin[it["id"]] == true) 0 else 1 }, { it["id"] as String })
            ).forEachIndexed { index, row -> result += LinkedHashMap(row).apply { put("orderIndex", index.toLong()) } }
        }
        return result
    }

    /** I pezzi di una pagina, ognuno con le sue righe; le celle di una tabella stanno col loro blocco. */
    private fun elements(unit: SyncUnit, mine: Boolean): Map<String, Element> {
        val cells = unit.tables["table_cells"].orEmpty().groupBy { it["blockId"] as String? }
        val result = LinkedHashMap<String, Element>()
        for ((table, rows) in unit.tables) {
            if (table == "table_cells") continue
            for (row in rows) {
                val id = row["id"] as String
                val parts = if (table == "blocks") {
                    mapOf("blocks" to listOf(row), "table_cells" to cells[id].orEmpty())
                } else {
                    mapOf(table to listOf(row))
                }
                result["$table:$id"] = Element(parts, mine)
            }
        }
        return result
    }

    /** Un'impronta corta: basta a dire se un pezzo è cambiato, e `sync.json` resta piccolo. */
    private fun shortHash(rows: Map<String, List<SyncRow>>): String = SyncUnit("", rows).hash.take(16)
}
