package com.gabriele.notionlocal.ui.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.gabriele.notionlocal.data.settings.OpenMode

/**
 * **Chi apre una pagina toccata nel contenuto** (dal 12/11/2026, solo sul PC,
 * chiesto dall'utente come in Notion): un collegamento a una pagina, una riga
 * di un database. La apre nel modo [OpenMode] chiesto, o, con `null`, in
 * quello di Settings (`AppSettings.openPagesAs`, di partenza Center peek).
 * Il percorso in alto, la barra laterale, la ricerca e i preferiti aprono
 * invece a pagina intera, come in Notion.
 *
 * Null sul telefono e nei test: lì si apre a pagina intera, come sempre.
 */
fun interface PageOpener {
    fun open(pageId: String, isDatabase: Boolean, mode: OpenMode?)
}

val LocalPageOpener = compositionLocalOf<PageOpener?> { null }

/** Il modo in cui è aperta la pagina dove si sta: nel peek il suo, altrove null (pagina intera). */
val LocalOpenMode = compositionLocalOf<OpenMode?> { null }

/**
 * **I comandi del peek** in alto a sinistra (chiudi, a pagina intera, il modo):
 * dentro un peek la pagina e il database li mettono al posto di indietro, ☰
 * e percorso. Null fuori da un peek.
 */
val LocalPeekControls = compositionLocalOf<(@Composable RowScope.() -> Unit)?> { null }
