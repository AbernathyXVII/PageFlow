package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.io.File

/** Da dove viene un video o un audio da ascoltare: un file dell'app o un indirizzo. */
sealed class MediaSource {
    data class LocalFile(val file: File) : MediaSource()
    data class Link(val url: String) : MediaSource()
}

/*
 * **I lettori dei blocchi video e audio** (dal 04/11/2026, per i blocchi nel
 * codice comune): sul telefono quelli dell'app (`MediaPlayers.kt`: il
 * lettore di Android, e YouTube e Vimeo nel loro lettore dentro la pagina);
 * sul PC, per ora, un riquadro che apre il video o l'audio col programma del
 * computer.
 */

/** Un video da guardare qui; tenendolo premuto, [onLongPress]. */
@Composable
expect fun InlineVideoPlayer(source: MediaSource, modifier: Modifier = Modifier, onLongPress: (() -> Unit)? = null)

/** Un audio da ascoltare qui, col suo titolo. */
@Composable
expect fun InlineAudioPlayer(source: MediaSource, title: String, modifier: Modifier = Modifier)

/** Il lettore di YouTube o di Vimeo, dentro la pagina. */
@Composable
expect fun EmbeddedVideoPlayer(embedUrl: String, modifier: Modifier = Modifier)

/** L'id di un video di YouTube, da tutti i modi in cui si scrive il suo indirizzo; null se non lo è. */
expect fun youTubeId(url: String): String?

/** Il numero di un video di Vimeo ("vimeo.com/123456"); null se non lo è. */
expect fun vimeoId(url: String): String?
