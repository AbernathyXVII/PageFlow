package com.gabriele.notionlocal.platform

import androidx.compose.runtime.Composable

/**
 * **Scegliere un suono** (dal 04/11/2026; i suoni del pomodoro): sul telefono
 * l'elenco delle suonerie di sistema, intitolato [title] e aperto su
 * [current]; scelto un suono, [onPicked] col suo indirizzo e il suo nome (se
 * il telefono lo sa). Sul PC, dal 10/11/2026, "Apri" per un file .wav.
 * Restituisce la funzione che apre l'elenco.
 */
@Composable
expect fun rememberSoundPicker(title: String, current: String?, onPicked: (uri: String, name: String?) -> Unit): () -> Unit
