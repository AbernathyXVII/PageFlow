package com.gabriele.notionlocal.ui.screen

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.gabriele.notionlocal.data.BlockFiles
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.saveFile

/** Un file scelto dal telefono, copiato nell'app col suo nome, tipo e peso: per video, audio e file. */
suspend fun importFile(context: Context, store: PageImageStore, uri: Uri): BlockAttrs? {
    val name = BlockFiles.displayName(context, uri)
    val mime = BlockFiles.mimeType(context, uri, name)
    val extension = name?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() } ?: BlockFiles.extensionFor(mime)
    val file = store.saveFile(uri, extension).getOrNull() ?: return null
    return BlockAttrs(file = file, name = name, mime = mime, size = store.fileFor(file).length())
}
