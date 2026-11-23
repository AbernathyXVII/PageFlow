package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.DEFAULT_LOAD_LIMIT
import com.gabriele.notionlocal.ui.i18n.DbStrings

/** Quante righe si vedono subito, e quante in più a ogni "Load more": dal 23/11/2026 di ogni vista ("Load limit"). */
const val DATABASE_ROW_LIMIT = DEFAULT_LOAD_LIMIT

/**
 * **Le prime righe e "Load more"** (dal 18/10/2026, come su Notion, scelto
 * dall'utente): un database dentro una pagina mostra le prime
 * [DATABASE_ROW_LIMIT] righe di ogni elenco (la tabella, ogni gruppo, la
 * galleria, ogni colonna della bacheca), e "Load more" ne aggiunge altrettante.
 * Disegnarle tutte, con un dizionario di diecimila parole, voleva dire secondi
 * per aprire la pagina e rifarle tutte a ogni modifica. A pagina intera invece
 * scorrono tutte e si disegnano solo quelle sullo schermo (vedi `TableLayout`),
 * tranne la bacheca, che ha il limite anche lì.
 *
 * Quante se ne vedono è di questa visita: tornando sulla pagina si riparte
 * dalle prime. Le righe create qui ([created]) si vedono sempre, anche oltre
 * il limite: altrimenti "New page" sembrerebbe non fare niente.
 */
@Stable
class RowLimits(val step: Int = DATABASE_ROW_LIMIT) {
    private val shown = mutableStateMapOf<String, Int>()

    /**
     * Le righe create da questa vista: vedi `DatabaseViewModel.createdHere`.
     * La vista la aggiorna mentre si disegna, prima delle righe.
     */
    var created: Set<String> = emptySet()

    /**
     * Le righe da mostrare di un elenco (`key`: quale), e quante restano fuori.
     * [first] quante subito, se non sono [step] (le fasce della bacheca).
     */
    fun <T> take(key: String, items: List<T>, first: Int = step, id: (T) -> String): Limited<T> {
        val limit = shown[key] ?: first
        if (items.size <= limit) return Limited(items, 0)
        val tail = items.subList(limit, items.size)
        val made = created
        val kept = if (made.isEmpty()) emptyList() else tail.filter { id(it) in made }
        val head = items.subList(0, limit)
        return Limited(if (kept.isEmpty()) head else head + kept, tail.size - kept.size)
    }

    /** "Load more": altre [first] righe di quell'elenco (di suo [step]). */
    fun more(key: String, first: Int = step) {
        shown[key] = (shown[key] ?: first) + first
    }
}

/** Le righe da mostrare e quante ne restano fuori. */
data class Limited<T>(val items: List<T>, val hidden: Int)

/** Le righe di un elenco col limite, se c'è (di suo quello della vista); tutte altrimenti. */
fun <T> RowLimits?.limit(key: String, items: List<T>, first: Int? = null, id: (T) -> String): Limited<T> =
    this?.take(key, items, first ?: step, id) ?: Limited(items, 0)

/**
 * **Le fasce della bacheca** (dal 21/10/2026): le prime 12 e "Load more" ne
 * aggiunge altre 12. Ogni fascia ha tutte le colonne, e con le settimane di
 * vent'anni di vittorie K-pop sarebbero mille fasce da disegnare insieme.
 */
const val BOARD_BAND_LIMIT = 12

/**
 * **I gruppi, 12 alla volta** (dal 26/10/2026): le colonne della bacheca e i
 * gruppi e sottogruppi di una tabella dentro una pagina, che si disegnano
 * tutti insieme. Raggruppando per una data giorno per giorno, le vittorie
 * K-pop dell'utente facevano 4.000 colonne, e la memoria finiva: l'app si
 * chiudeva aprendo la pagina. "Load more" ne aggiunge altri 12.
 */
const val DATABASE_GROUP_LIMIT = 12

/** Null dove le righe si vedono tutte, senza limite. */
val LocalRowLimits = staticCompositionLocalOf<RowLimits?> { null }

/**
 * **Un database a pagina intera disegnato come lista** (dal 18/10/2026): la
 * vista fa solo le righe sullo schermo, e mette in fondo [trailing], il
 * pulsante delle righe nuove. [newRowTick] cresce a ogni riga creata da lì.
 */
class LazyBody(val newRowTick: Int, val trailing: @Composable () -> Unit)

/** Le viste che a pagina intera sono una lista (vedi [LazyBody]); le altre hanno [RowLimits] o non ne hanno bisogno. */
val LAZY_FULL_PAGE_LAYOUTS = setOf(DatabaseLayout.TABLE, DatabaseLayout.LIST, DatabaseLayout.GALLERY)

/** "Load more", col numero delle righe che restano fuori. */
@Composable
fun LoadMoreButton(hidden: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.padding(start = 8.dp).testTag("load-more")) {
        Icon(Icons.Filled.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Text(DbStrings.loadMore, style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            hidden.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
