package com.gabriele.notionlocal.data.entity

/*
 * **La proprietà principale come le altre** (dal 13/11/2026, chiesto
 * dall'utente: "permettimi di modificare anche il tipo della colonna
 * principale … trattata come qualsiasi altra colonna, quindi con la
 * possibilità di nasconderla"). Due cose:
 *
 * - **Name nascosta**: [NAME_COLUMN_KEY] fra le proprietà nascoste della vista
 *   (`PageEntity.viewHiddenColumnIds`), come una proprietà qualsiasi;
 * - **la colonna che dà il nome alle righe**: "Change type" su Name la fa
 *   diventare una colonna vera di quel tipo, al suo posto, coi valori di
 *   prima; Name si nasconde, e ogni riga si chiama come il valore scritto in
 *   quella colonna (scelto dall'utente: "il valore della colonna"). Il segno
 *   "__title__=id" sta sul database con i dati, nello stesso campo: lì non
 *   serve ad altro (le proprietà nascoste del database stanno nelle colonne).
 *
 * Scritte lì e non in due campi nuovi di proposito: un campo nuovo vuol dire
 * cambiare il database del telefono e del PC, e il PC non sa ancora farlo su
 * quello che c'è (e un database più nuovo bloccherebbe l'altro dispositivo
 * quando si sincronizzano).
 */

private const val TITLE_MARK = "__title__="

/** I segni scritti fra le proprietà nascoste: gli id e i due segni qui sopra. */
fun hiddenMarksOf(raw: String?): Set<String> = raw.orEmpty().split(",").filter { it.isNotBlank() }.toSet()

private fun encodeMarks(marks: Collection<String>): String? = marks.joinToString(",").ifEmpty { null }

/** Name non si vede in questa vista. */
fun PageEntity.nameColumnHidden(): Boolean = NAME_COLUMN_KEY in hiddenMarksOf(viewHiddenColumnIds)

/** La colonna da cui le righe di questo database (quello con i dati) prendono il nome, o null: il nome è il loro. */
fun PageEntity.titleColumnId(): String? =
    hiddenMarksOf(viewHiddenColumnIds).firstOrNull { it.startsWith(TITLE_MARK) }?.removePrefix(TITLE_MARK)?.takeIf { it.isNotBlank() }

/** [raw] con Name nascosta o no. */
fun withNameHidden(raw: String?, hidden: Boolean): String? {
    val marks = hiddenMarksOf(raw) - NAME_COLUMN_KEY
    return encodeMarks(if (hidden) marks + NAME_COLUMN_KEY else marks)
}

/** [raw] con [columnId] come colonna dei nomi (null: nessuna). */
fun withTitleColumn(raw: String?, columnId: String?): String? =
    encodeMarks(hiddenMarksOf(raw).filterNot { it.startsWith(TITLE_MARK) } + listOfNotNull(columnId?.let { TITLE_MARK + it }))
