package com.gabriele.notionlocal.platform

import androidx.compose.runtime.Composable
import com.gabriele.notionlocal.data.PageImageStore

/**
 * **Scegliere un'immagine** (dal 04/11/2026, per le schermate nel codice
 * comune): sul telefono il selettore di foto di sistema, che non chiede il
 * permesso di leggere tutta la galleria (l'utente sceglie una foto e l'app vede
 * solo quella); sul PC la scelta di un file. Scelta l'immagine, [onStart];
 * poi, copiata nell'app con [store], [onPicked] col suo nome (o l'errore).
 * Annullando non succede niente. Restituisce la funzione che apre il selettore.
 */
@Composable
expect fun rememberImagePicker(
    store: PageImageStore,
    onStart: () -> Unit = {},
    onPicked: (Result<String>) -> Unit
): () -> Unit

/** Il sito di un indirizzo ("www.youtube.com"), come lo legge il sistema; null se non c'è. */
expect fun urlHost(url: String): String?

/** Lo schema di un indirizzo ("https"), come lo legge il sistema; null se non c'è. */
expect fun urlScheme(url: String): String?

/** L'ultimo pezzo del percorso di un indirizzo ("canzone.mp3"), come lo legge il sistema; null se non c'è. */
expect fun urlLastPathSegment(url: String): String?

/**
 * L'altezza in pixel della vista dell'app, letta quando serve (non quando si
 * disegna): sul telefono quella della vista di Compose, sul PC della finestra.
 */
@Composable
expect fun rememberRootHeight(): () -> Int
