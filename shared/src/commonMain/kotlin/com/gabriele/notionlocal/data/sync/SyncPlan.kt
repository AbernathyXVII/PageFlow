package com.gabriele.notionlocal.data.sync

/**
 * **Cosa fare per ogni pezzo** (dal 10/11/2026), confrontando com'è qui,
 * com'è dall'altro lato e com'era all'ultima sincronizzazione ([base], le
 * impronte di allora):
 * - cambiato da una parte sola: va dall'altra;
 * - nuovo da una parte: va dall'altra;
 * - cancellato da una parte e uguale dall'altra: nel cestino dall'altra
 *   (cancellato e cambiato dall'altra: torna, vince la modifica);
 * - cambiato da tutte e due: vince l'ultima modifica (a pari merito, questo
 *   lato), e l'altra resta nella cronologia della pagina (`BEFORE_SYNC`).
 */
data class SyncPlan(
    /** I pezzi nostri che vanno di là. */
    val send: Set<String>,
    /** I pezzi di là che vengono qui. */
    val fetch: Set<String>,
    /** Cancellati di là, e qui uguali a prima: nel cestino qui. */
    val trashHere: Set<String>,
    /** Cancellati qui, e di là uguali a prima: nel cestino di là. */
    val trashThere: Set<String>,
    /** Cambiati da tutte e due le parti (sono anche in [send] o [fetch]). */
    val conflicts: Set<String>
) {
    companion object {
        fun of(here: Map<String, UnitState>, there: Map<String, UnitState>, base: Map<String, String>): SyncPlan {
            val send = HashSet<String>()
            val fetch = HashSet<String>()
            val trashHere = HashSet<String>()
            val trashThere = HashSet<String>()
            val conflicts = HashSet<String>()
            for (id in here.keys + there.keys) {
                val mine = here[id]
                val theirs = there[id]
                val before = base[id]
                if (mine?.hash == theirs?.hash) continue
                when {
                    mine == null -> if (before != null && theirs!!.hash == before) trashThere += id else fetch += id
                    theirs == null -> if (before != null && mine.hash == before) trashHere += id else send += id
                    mine.hash == before -> fetch += id
                    theirs.hash == before -> send += id
                    else -> {
                        conflicts += id
                        if (mine.changedAt >= theirs.changedAt) send += id else fetch += id
                    }
                }
            }
            return SyncPlan(send, fetch, trashHere, trashThere, conflicts)
        }

        /**
         * **Le impronte da ricordare** dopo la sincronizzazione: quelle uguali
         * sui due lati; per un pezzo cancellato da una parte e messo nel
         * cestino dall'altra, quella del cestino (così la volta dopo non
         * torna). Gli altri, rimasti diversi, tengono quella di prima.
         */
        fun newBase(
            here: Map<String, UnitState>,
            there: Map<String, UnitState>,
            plan: SyncPlan,
            base: Map<String, String>
        ): Map<String, String> {
            val result = HashMap<String, String>()
            for (id in here.keys + there.keys + base.keys) {
                val mine = here[id]?.hash
                val theirs = there[id]?.hash
                val kept = when {
                    mine == theirs -> mine
                    mine == null && id in plan.trashThere -> theirs
                    theirs == null && id in plan.trashHere -> mine
                    else -> base[id]
                }
                if (kept != null) result[id] = kept
            }
            return result
        }
    }
}
