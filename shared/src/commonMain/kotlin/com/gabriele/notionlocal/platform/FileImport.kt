package com.gabriele.notionlocal.platform

import androidx.compose.runtime.Composable
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockAttrs

/*
 * **Scegliere un file, copiarlo nell'app, salvarne una copia** (dal
 * 04/11/2026, per i blocchi nel codice comune): sul telefono i selettori di
 * sistema di sempre (foto, documenti, "Salva in…"); sul PC la finestra di
 * Windows o del Mac per scegliere o salvare un file.
 */

/** Un'immagine scelta e copiata nell'app: il nome del file nell'app e quello che aveva. */
data class PickedImage(val file: String, val name: String?)

/** Cosa si sceglie per un blocco: un video, un audio, o un file qualsiasi. */
enum class FileKind { VIDEO, AUDIO, ANY }

/**
 * Un'immagine per un blocco: scelta, [onStart]; copiata nell'app,
 * [onImported] col suo nome (o l'errore). Annullando non succede niente.
 * Restituisce la funzione che apre il selettore.
 */
@Composable
expect fun rememberImageImporter(
    store: PageImageStore,
    onStart: () -> Unit = {},
    onImported: (Result<PickedImage>) -> Unit
): () -> Unit

/**
 * Un video, un audio o un file per un blocco: scelto, [onStart]; copiato
 * nell'app, [onImported] coi suoi dati (file, nome, tipo, peso), o null se
 * non si è riusciti a leggerlo.
 */
@Composable
expect fun rememberFileImporter(
    store: PageImageStore,
    kind: FileKind,
    onStart: () -> Unit = {},
    onImported: (BlockAttrs?) -> Unit
): () -> Unit

/**
 * **"Salva una copia"**: il file dell'app dove sceglie l'utente, col nome
 * proposto; poi [onDone] (vero se è andata). [mime] è il tipo del file, che
 * il telefono vuole sapere prima.
 */
@Composable
expect fun rememberSaveCopy(
    store: PageImageStore,
    mime: String?,
    onDone: (Boolean) -> Unit
): (file: String, name: String) -> Unit
