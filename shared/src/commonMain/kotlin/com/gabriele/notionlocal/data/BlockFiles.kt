package com.gabriele.notionlocal.data

import com.gabriele.notionlocal.platform.PlatformContext

/**
 * **I file dei blocchi** (immagini, video, audio e file caricati): quello che
 * serve ai blocchi della pagina, nel codice comune dal 04/11/2026. Sul
 * telefono è l'oggetto di sempre (`BlockFiles.android.kt`), che sa anche
 * leggere nome e tipo di un file scelto; sul PC si apre col programma di
 * Windows o del Mac, e le immagini si salvano in Immagini/PageFlow.
 */
expect object BlockFiles {
    /** Il tipo di un'immagine già salvata, letto dal file stesso ("image/jpeg"). */
    fun imageMime(store: PageImageStore, fileName: String): String?

    /** L'estensione che va col tipo: "jpg" per "image/jpeg". */
    fun extensionFor(mime: String?): String?

    /** **Apre il file con un'altra app** (il lettore video, il visore dei PDF…). Falso se nessuna lo sa aprire. */
    fun openWithAnotherApp(context: PlatformContext, store: PageImageStore, fileName: String, mime: String?): Boolean

    /** **Apre un indirizzo** con l'app che lo sa aprire (il browser, la posta…). Falso se nessuna lo sa aprire. */
    fun openUrl(context: PlatformContext, url: String): Boolean

    /** **Salva un'immagine fra le foto** ("Pictures/PageFlow"): lo "scarica" delle immagini. Falso se non si può. */
    suspend fun saveImageToGallery(context: PlatformContext, store: PageImageStore, fileName: String, displayName: String?): Boolean
}
