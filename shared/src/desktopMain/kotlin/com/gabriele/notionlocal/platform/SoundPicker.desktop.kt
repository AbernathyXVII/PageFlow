package com.gabriele.notionlocal.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.URI

/**
 * Sul PC: "Apri" di Windows per un file audio che Java sa suonare (.wav,
 * .aiff, .au; gli mp3 no), aperto nella cartella di quello scelto prima.
 */
@Composable
actual fun rememberSoundPicker(title: String, current: String?, onPicked: (uri: String, name: String?) -> Unit): () -> Unit {
    val picked by rememberUpdatedState(onPicked)
    return {
        val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD).apply {
            runCatching { current?.takeIf { it.startsWith("file:") }?.let { File(URI(it)).parent } }.getOrNull()?.let { directory = it }
            // Su Windows il filtro è il nome proposto; altrove serve il FilenameFilter.
            file = "*.wav"
            setFilenameFilter { _, name -> name.substringAfterLast('.', "").lowercase() in SOUND_FILES }
            isVisible = true
        }
        dialog.file?.let { name -> picked(File(dialog.directory, name).toURI().toString(), name.substringBeforeLast('.')) }
    }
}

private val SOUND_FILES = setOf("wav", "aiff", "aif", "au")
