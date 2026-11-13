package com.gabriele.notionlocal.ui.screen

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.BlockType

/**
 * **I file trascinati da fuori dentro la pagina** (dal 13/11/2026, chiesto
 * dall'utente con la foto di Notion): sul PC un'immagine o un file preso da
 * Esplora risorse e portato sopra la pagina mostra la stessa linea blu dei
 * blocchi spostati coi sei puntini (`BlockDrag.kt`) — sopra o sotto una riga,
 * o di lato per metterlo in colonna — e lasciandolo diventa un blocco lì:
 * un'immagine, un video, un audio o un file.
 */
class DroppedFile(val type: BlockType, val attrs: BlockAttrs)

/** Il blocco trascinato quando arriva da fuori: non è nella pagina (vedi `dropFor`). */
const val OUTSIDE_FILE = "\u0000outside-file"

/**
 * Sul PC: i file trascinati sopra [this]. [onMove] riceve il punto nella
 * finestra mentre si trascina, null quando si esce o si lascia; [onDrop] il
 * punto dove si lascia, e restituisce chi mette i blocchi (null: niente), a
 * cui arrivano i file già copiati nell'app ([store]). Sul telefono niente.
 */
expect fun Modifier.outsideFileDrop(
    store: PageImageStore,
    onMove: (Offset?) -> Unit,
    onDrop: (Offset) -> ((List<DroppedFile>) -> Unit)?
): Modifier
