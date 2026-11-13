package com.gabriele.notionlocal.data.entity

/**
 * **"Show as" di una proprietà Link** (dal 13/11/2026, chiesto dall'utente:
 * non più nel menu del tocco lungo di ogni cella, ma nel menu della
 * proprietà, sotto il suo nome): tutte le celle col link per intero, o col
 * titolo della loro pagina web (letto dal sito, e correggibile cella per
 * cella con la matita: vedi `DatabaseCellEntity.linkLabel`).
 *
 * Sta in `numberDisplay`, che una proprietà Link non usa (è lo "Show as" dei
 * numeri): niente campi nuovi nel database, che sul PC non ha migrazioni.
 * null = com'era prima: ogni cella con la scritta che aveva.
 */
enum class LinkColumnShow { LINK, TITLE }

val DatabaseColumnEntity.linkShow: LinkColumnShow?
    get() = if (type != ColumnType.URL) null else LinkColumnShow.entries.firstOrNull { it.name == numberDisplay }
