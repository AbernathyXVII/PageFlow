package com.gabriele.notionlocal.ui.screen

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private fun MediaSource.toUri(): Uri = when (this) {
    is MediaSource.LocalFile -> Uri.fromFile(file)
    is MediaSource.Link -> Uri.parse(url)
}

@Composable
actual fun InlineVideoPlayer(source: MediaSource, modifier: Modifier, onLongPress: (() -> Unit)?) =
    InlineVideoPlayer(source.toUri(), modifier, onLongPress)

@Composable
actual fun InlineAudioPlayer(source: MediaSource, title: String, modifier: Modifier) =
    InlineAudioPlayer(source.toUri(), title, modifier)
