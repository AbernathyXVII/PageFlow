package com.gabriele.notionlocal.data

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * **I file dei blocchi** (dal 06/10/2026): immagini, video, audio e file
 * caricati dal telefono. Stanno nella stessa cartella delle icone e delle
 * copertine (`PageImageStore`), per nome; qui c'è quello che serve per
 * leggerne il nome e il tipo all'arrivo, aprirli con un'altra app e
 * salvarne una copia fuori dall'app.
 */
actual object BlockFiles {

    /** Il nome del file com'era sul telefono ("vacanze.mp4"), se chi l'ha dato lo dice. */
    fun displayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()?.takeIf { it.isNotBlank() }

    /** Il tipo del file ("video/mp4"), da chi l'ha dato o dall'estensione del nome. */
    fun mimeType(context: Context, uri: Uri, name: String?): String? =
        context.contentResolver.getType(uri)
            ?: name?.substringAfterLast('.', "")?.lowercase()?.takeIf { it.isNotEmpty() }
                ?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }

    /** Il tipo di un'immagine già salvata, letto dal file stesso ("image/jpeg"). */
    actual fun imageMime(store: PageImageStore, fileName: String): String? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(store.fileFor(fileName).path, bounds)
        bounds.outMimeType
    }.getOrNull()

    /** L'estensione che va col tipo: "jpg" per "image/jpeg". */
    actual fun extensionFor(mime: String?): String? =
        mime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }

    /** Un indirizzo che le altre app possono leggere, per un file dell'app. */
    fun contentUri(context: Context, store: PageImageStore, fileName: String): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.files", store.fileFor(fileName))

    /**
     * **Apre il file con un'altra app** (il lettore video, il visore dei
     * PDF…), con la scelta di sistema. Falso se nessuna app lo sa aprire.
     */
    actual fun openWithAnotherApp(context: Context, store: PageImageStore, fileName: String, mime: String?): Boolean {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(contentUri(context, store, fileName), mime ?: "*/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return runCatching {
            // Dalla pila di notE, come i link (vedi `openUrl`): "indietro"
            // dal lettore torna qui.
            context.startActivity(Intent.createChooser(intent, null).withTaskFor(context))
            true
        }.getOrElse { false }
    }

    /**
     * **Apre un indirizzo** con l'app che lo sa aprire: una pagina web **nel
     * browser predefinito del telefono** (per l'utente Firefox), `mailto:`
     * con la posta, `tel:` col telefono. Se c'è un'app fatta apposta per quel
     * sito (YouTube per un video di YouTube), Android apre quella. Falso se
     * nessuna app lo sa aprire. Vale per i link sul testo, i segnalibri, i
     * video e gli audio con un indirizzo e le celle URL, email e telefono dei
     * database.
     *
     * Dal 17/10/2026 le pagine web si aprivano in una "Custom Tab", il
     * riquadro del browser sopra l'app, perché con la scheda normale di
     * Firefox "indietro" aveva mostrato l'app usata prima di notE. Il
     * 18/10/2026 l'utente ha chiesto il browser vero: «vorrei che me lo
     * facesse aprire nel browser di default che nel mio caso è Firefox».
     */
    actual fun openUrl(context: Context, url: String): Boolean = com.gabriele.notionlocal.platform.openUrl(context, url)

    /** Dall'attività di notE l'app aperta resta nella sua pila; senza un'attività Android ne vuole una nuova. */
    private fun Intent.withTaskFor(context: Context): Intent =
        if (context.hostActivity() == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) else this

    private tailrec fun Context.hostActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.hostActivity()
        else -> null
    }

    /**
     * **Salva un'immagine nella Galleria**, nella cartella "Pictures/notE":
     * è lo "scarica" delle immagini. Su Android 9 e prima, dove serve un
     * permesso che l'app non ha, risponde falso e chi chiama fa scegliere
     * dove salvarla.
     */
    actual suspend fun saveImageToGallery(context: Context, store: PageImageStore, fileName: String, displayName: String?): Boolean =
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext false
            runCatching {
                val mime = imageMime(store, fileName) ?: "image/jpeg"
                val name = displayName?.takeIf { it.contains('.') }
                    ?: "PageFlow-${System.currentTimeMillis()}.${extensionFor(mime) ?: "jpg"}"
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, mime)
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PageFlow")
                }
                val resolver = context.contentResolver
                val target = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@runCatching false
                resolver.openOutputStream(target)?.use { out ->
                    store.fileFor(fileName).inputStream().use { it.copyTo(out) }
                } ?: return@runCatching false
                true
            }.getOrDefault(false)
        }

    /** Copia un file dell'app dove l'utente ha scelto (il "Save to…" di sistema). */
    suspend fun copyTo(context: Context, store: PageImageStore, fileName: String, target: Uri): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(target)?.use { out ->
                    store.fileFor(fileName).inputStream().use { it.copyTo(out) }
                } != null
            }.getOrDefault(false)
        }
}
