package com.gabriele.notionlocal.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.launch
import java.io.File
import java.net.URI

/**
 * Sul PC: "Apri" di Windows per un file audio che Java sa suonare (.wav,
 * .aiff, .au; gli mp3 no), aperto nella cartella di quello scelto prima.
 */
@Composable
actual fun rememberSoundPicker(title: String, current: String?, onPicked: (uri: String, name: String?) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    val picked by rememberUpdatedState(onPicked)
    return {
        scope.launch {
            val folder = runCatching { current?.takeIf { it.startsWith("file:") }?.let { File(URI(it)).parent } }.getOrNull()
            val file = SystemFileDialog.open(title, SOUND_FILES, folder) ?: return@launch
            picked(file.toURI().toString(), file.name.substringBeforeLast('.'))
        }
    }
}

private val SOUND_FILES = setOf("wav", "aiff", "aif", "au")
