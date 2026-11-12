package com.gabriele.notionlocal.ui.navigation

/**
 * **Le scorciatoie da tastiera del PC** (dal 09/11/2026): la finestra le
 * riceve tutte (anche senza un campo col cursore) e chiede qui; a rispondere è
 * la navigazione aperta (`AppNavHost`), finché c'è.
 */
object AppShortcuts {
    /** Ctrl+F: la ricerca. */
    var openSearch: (() -> Unit)? = null

    /** Ctrl+N: una pagina nuova (dal 10/11/2026). */
    var newPage: (() -> Unit)? = null

    /** Ctrl+T: una scheda nuova sul menu principale, come il + (dal 12/11/2026, vedi `PcTabs`). */
    var newTab: (() -> Unit)? = null

    /** Ctrl+W: chiude la scheda aperta, come la sua × (dal 12/11/2026). */
    var closeTab: (() -> Unit)? = null
}
