package com.gabriele.notionlocal.ui.navigation

/**
 * Rotte di navigazione dell'app. pageId è passato come argomento di
 * navigazione per sapere quale pagina aprire nell'editor o nella
 * vista database. La rotta HOME apre semplicemente PageEditorScreen
 * puntata su PageEntity.ROOT_PAGE_ID — non esiste più una schermata
 * Home separata.
 */
object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val PAGE_EDITOR = "page/{pageId}"
    const val DATABASE_VIEW = "database/{pageId}"

    /** La cronologia delle modifiche di una pagina: la voce "Updates" del menu. */
    const val UPDATES = "updates/{pageId}"

    // Le voci della barra laterale che aprono una schermata a sé.
    const val FAVORITES = "favorites"
    const val TRASH = "trash"
    const val STARTUP = "startup"
    /** I template delle pagine (dal 26/09/2026). */
    const val TEMPLATES = "templates"
    const val SETTINGS = "settings"

    /** I backup (dal 28/10/2026): vedi `BackupScreen`. */
    const val BACKUP = "backup"
    const val CONNECTIONS = "connections"

    /** Settings → Patch Notes (dal 30/09/2026): le versioni dell'app. */
    const val PATCH_NOTES = "patchNotes"

    /** La pagina di una versione delle Patch Notes ("0.68", "0.67.1"). */
    const val PATCH_NOTE = "patchNote/{version}"

    fun patchNote(version: String) = "patchNote/$version"

    /** Una sezione di un quaderno, con le sue pagine (vedi `NotebookSectionScreen`). */
    const val NOTEBOOK_SECTION = "notebookSection/{sectionId}"

    /** Un quaderno: tutte le sue sezioni e i gruppi (vedi `NotebookScreen`). */
    const val NOTEBOOK = "notebook/{notebookId}"

    fun pageEditor(pageId: String) = "page/$pageId"
    fun databaseView(pageId: String) = "database/$pageId"
    fun updates(pageId: String) = "updates/$pageId"
    fun notebookSection(sectionId: String) = "notebookSection/$sectionId"
    fun notebook(notebookId: String) = "notebook/$notebookId"

    /** La rotta di una pagina, che sia testo o database. */
    fun forPage(pageId: String, isDatabase: Boolean) =
        if (isDatabase) databaseView(pageId) else pageEditor(pageId)

    /** L'id della pagina dentro una rotta "page/…" o "database/…", o null. */
    fun pageIdOf(route: String): String? = when {
        route.startsWith("page/") -> route.removePrefix("page/")
        route.startsWith("database/") -> route.removePrefix("database/")
        else -> null
    }
}
