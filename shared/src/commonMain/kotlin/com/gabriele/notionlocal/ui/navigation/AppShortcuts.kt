package com.gabriele.notionlocal.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * **Le scorciatoie da tastiera del PC** (dal 09/11/2026): la finestra le
 * riceve tutte (anche senza un campo col cursore) e chiede qui; a rispondere è
 * la navigazione aperta (`AppNavHost`), finché c'è.
 */
open class Shortcuts {
    /** Ctrl+F: la ricerca. */
    var openSearch: (() -> Unit)? = null

    /** Ctrl+N: una pagina nuova (dal 10/11/2026). */
    var newPage: (() -> Unit)? = null

    /** Ctrl+T: una scheda nuova sul menu principale, come il + (dal 12/11/2026, vedi `PcTabs`). */
    var newTab: (() -> Unit)? = null

    /** Ctrl+W: chiude la scheda aperta, come la sua × (dal 12/11/2026). */
    var closeTab: (() -> Unit)? = null
}

/** Quelle di sempre: sul telefono, nei test e dove una finestra non ne dà di sue. */
object AppShortcuts : Shortcuts()

/**
 * **Le scorciatoie della finestra** (dal 13/11/2026): con le schede staccate
 * in un'altra finestra (vedi `PcTabs`) ogni finestra ha le sue, e Ctrl+T apre
 * la scheda in quella dove si è.
 */
val LocalShortcuts = staticCompositionLocalOf<Shortcuts> { AppShortcuts }
