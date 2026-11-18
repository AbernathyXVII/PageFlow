package com.gabriele.notionlocal.data

import androidx.compose.ui.graphics.ImageBitmap
import com.gabriele.notionlocal.platform.PlatformContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.URI
import java.util.Base64
import java.util.UUID

/**
 * Le immagini delle pagine — icone e copertine — dentro la cartella
 * dell'app.
 *
 * Arrivano solo dalla galleria del telefono, e ne viene fatta una copia:
 * l'immagine resta anche se l'originale viene cancellato. (Fino al
 * 25/09/2026 si potevano anche scaricare da un link: tolto su richiesta
 * dell'utente.)
 *
 * Nel database si salva il **nome** del file e non il percorso intero:
 * la cartella dell'app può cambiare posto fra un'installazione e
 * l'altra, o al ripristino di un backup, e un percorso scritto dentro
 * il database diventerebbe un puntatore a niente.
 *
 * **Dal 04/11/2026 è nel codice comune a telefono e PC.** Quello che cambia
 * fra i due è in fondo al file (`expect`): dov'è la cartella, come si legge
 * un'immagine, e se una foto va girata. Prendere un file dalla galleria del
 * telefono (`saveFromGallery`, `saveFile`) sta in `PageImageStore.android.kt`.
 */
class PageImageStore(context: PlatformContext) {

    /** Il contesto dell'app (sul telefono quello dell'applicazione, non della schermata). */
    val appContext: PlatformContext = applicationContextOf(context)

    private val directory: File
        get() = File(filesDirOf(appContext), DIRECTORY).apply { mkdirs() }

    fun fileFor(name: String): File = File(directory, name)

    /**
     * **Copia dentro l'app quello che esce da [open]**: la base di
     * `saveFromGallery` e `saveFile` del telefono, e di quello che sceglierà
     * un file sul PC. Con [mustBeImage] quello che non è un'immagine non
     * resta. Restituisce il nome del file nell'app.
     */
    suspend fun saveFromStream(
        open: () -> InputStream?,
        extension: String = IMAGE_EXTENSION,
        mustBeImage: Boolean,
        cannotOpen: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val name = newName(extension)
        val target = File(directory, name)
        runCatching {
            open().use { input ->
                requireNotNull(input) { cannotOpen }
                target.outputStream().use { output -> input.copyTo(output) }
            }
            if (mustBeImage) {
                require(isImage(target)) {
                    "Quel file non sembra un'immagine."
                }
            }
            name
        // Una copia non riuscita (o che non è un'immagine) non deve
        // restare nella cartella. Fino al 25/09/2026 qui si cancellava un
        // file che aveva per nome il messaggio d'errore, cioè nessuno.
        }.onFailure { target.delete() }
    }

    /** L'estensione di un file scelto, ripulita; "bin" se non c'è. */
    fun cleanExtension(extension: String?): String =
        extension?.lowercase()?.filter { it.isLetterOrDigit() }?.takeIf { it.isNotEmpty() } ?: "bin"

    /**
     * **Un'immagine arrivata dalla rete** (dal 06/10/2026: l'anteprima di un
     * segnalibro). Se non è davvero un'immagine non resta. Il nome del file, o null.
     */
    suspend fun saveImageBytes(bytes: ByteArray): String? = withContext(Dispatchers.IO) {
        val name = newName()
        val target = File(directory, name)
        runCatching {
            target.writeBytes(bytes)
            require(isImage(target))
            name
        }.onFailure { target.delete() }.getOrNull()
    }

    /**
     * **Un'immagine incollata** (dal 18/11/2026, chiesto dall'utente: copiando
     * da OneNote): il file che Office scrive accanto all'HTML copiato
     * (`file:///…/clip_image001.png`) o quella scritta dentro l'HTML
     * (`data:image/png;base64,…`), copiata qui (vedi `pastableImage`). Il nome
     * del file, o null se non c'è o non è un'immagine.
     */
    suspend fun savePastedImage(source: String): String? = withContext(Dispatchers.IO) {
        if (source.startsWith("data:", ignoreCase = true)) {
            val bytes = runCatching { Base64.getMimeDecoder().decode(source.substringAfter("base64,", "")) }.getOrNull()
            return@withContext bytes?.takeIf { it.isNotEmpty() }?.let { saveImageBytes(it) }
        }
        val file = runCatching { File(URI(source.replace(" ", "%20"))) }.getOrNull()?.takeIf { it.isFile } ?: return@withContext null
        saveFromStream({ file.inputStream() }, mustBeImage = true, cannotOpen = file.name).getOrNull()
    }

    /**
     * Le misure di un'immagine salvata (larghezza, altezza) **come la si
     * guarda**, lette senza caricarla; null se non c'è. Una foto scattata in
     * verticale è salvata coricata, con l'etichetta che dice di girarla (vedi
     * `uprightOf` sul telefono): le sue misure si scambiano.
     */
    fun imageSize(name: String): Pair<Int, Int>? = runCatching {
        val file = File(directory, name)
        val (width, height) = imageBoundsOf(file) ?: return@runCatching null
        if (imageIsTurned(file)) height to width else width to height
    }.getOrNull()

    /**
     * **Un file nuovo che l'app scrive da sé** (dal 20/10/2026: la foto del
     * Web Clipper, scritta a strisce mentre la pagina scorre): il nome da
     * salvare nel blocco e il file dove scrivere.
     */
    fun newFile(extension: String): Pair<String, File> {
        val name = newName(extension)
        return name to File(directory, name)
    }

    /**
     * Una copia del file con un nome suo, per una pagina duplicata.
     *
     * Le due pagine **non possono condividere lo stesso file**: cambiando
     * l'icona di una, o cancellandola per sempre, il file se ne va dalla
     * cartella, e l'altra resterebbe con un'immagine che non c'è più.
     * Null se il file non c'è: la copia nasce senza immagine.
     */
    suspend fun copy(name: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val source = File(directory, name)
            if (!source.exists()) return@runCatching null
            // Con la sua estensione: un video copiato resta un ".mp4", e
            // l'app che lo apre lo riconosce (dal 06/10/2026, coi file dei blocchi).
            val copyName = newName(source.extension.ifBlank { IMAGE_EXTENSION })
            source.copyTo(File(directory, copyName))
            copyName
        }.getOrNull()
    }

    /**
     * Toglie un file che non serve più. Un'icona sostituita lascerebbe
     * altrimenti il vecchio file nella cartella per sempre, senza che
     * nessuno possa più arrivarci.
     */
    suspend fun delete(name: String?) = withContext(Dispatchers.IO) {
        if (name.isNullOrBlank()) return@withContext
        runCatching { File(directory, name).delete() }
        Unit
    }

    /**
     * Legge l'immagine già rimpicciolita a quanto serve davvero, e girata
     * come va guardata.
     *
     * Una foto da dodici megapixel in memoria sono quarantotto
     * megabyte: caricarla intera per mostrarla larga un centimetro
     * farebbe chiudere l'app, e non si vedrebbe meglio. Oltre [MAX_PIXELS]
     * si scende ancora: il file resta quello che è, a essere meno fine è
     * solo la copia che si guarda.
     */
    suspend fun load(name: String, targetWidthPx: Int): PlatformBitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(directory, name)
            if (!file.exists()) return@runCatching null
            decodeImage(file, targetWidthPx.coerceAtLeast(1), MAX_PIXELS)
        }.getOrNull()
    }

    /** Tutte le immagini delle pagine, via: per "Elimina tutto il contenuto" (vedi `AppCleanup`). */
    fun deleteAll() {
        directory.listFiles()?.forEach { it.delete() }
    }

    /** Decodifica i soli contorni: se non sono quelli di un'immagine, non lo è. */
    private fun isImage(file: File): Boolean = imageBoundsOf(file) != null

    private fun newName(extension: String = IMAGE_EXTENSION) = "${UUID.randomUUID()}.$extension"

    companion object {
        /** La cartella delle immagini e dei file: va anche nei backup (vedi `Backups`). */
        const val DIRECTORY = "page-images"
        const val IMAGE_EXTENSION = "img"

        /** Sei megapixel in memoria sono ventiquattro megabyte: è il tetto. */
        const val MAX_PIXELS = 6_000_000L
    }
}

/** Un'immagine in memoria: la `Bitmap` di Android sul telefono, quella di Skia sul PC. */
expect class PlatformBitmap

/** La stessa immagine, da disegnare con Compose. */
expect fun PlatformBitmap.toImageBitmap(): ImageBitmap

/** Il contesto dell'applicazione (sul PC è lo stesso). */
expect fun applicationContextOf(context: PlatformContext): PlatformContext

/** La cartella dei file dell'app: `filesDir` sul telefono, la cartella dei dati sul PC. */
expect fun filesDirOf(context: PlatformContext): File

/** Larghezza e altezza come sono salvate nel file (non girate); null se non è un'immagine. */
expect fun imageBoundsOf(file: File): Pair<Int, Int>?

/** Vero se l'etichetta della foto (EXIF) dice di girarla di 90 gradi: larghezza e altezza si scambiano. */
expect fun imageIsTurned(file: File): Boolean

/**
 * L'immagine di [file] larga circa [targetWidthPx] (non meno), al più
 * [maxPixels] pixel, girata come va guardata; null se non si legge.
 */
expect fun decodeImage(file: File, targetWidthPx: Int, maxPixels: Long): PlatformBitmap?
