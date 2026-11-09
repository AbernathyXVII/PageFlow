package com.gabriele.notionlocal.platform

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.gabriele.notionlocal.data.BlockFiles
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.saveFromGallery
import com.gabriele.notionlocal.ui.screen.importFile
import kotlinx.coroutines.launch

@Composable
actual fun rememberImageImporter(
    store: PageImageStore,
    onStart: () -> Unit,
    onImported: (Result<PickedImage>) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val start by rememberUpdatedState(onStart)
    val imported by rememberUpdatedState(onImported)
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        start()
        scope.launch {
            val name = BlockFiles.displayName(context, uri)
            imported(store.saveFromGallery(uri).map { PickedImage(it, name) })
        }
    }
    return remember(gallery) { { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } }
}

@Composable
actual fun rememberFileImporter(
    store: PageImageStore,
    kind: FileKind,
    onStart: () -> Unit,
    onImported: (BlockAttrs?) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val start by rememberUpdatedState(onStart)
    val imported by rememberUpdatedState(onImported)
    val import: (Uri?) -> Unit = { uri ->
        if (uri != null) {
            start()
            scope.launch { imported(importFile(context, store, uri)) }
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { import(it) }
    val documents = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { import(it) }
    return {
        when (kind) {
            FileKind.VIDEO -> gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            FileKind.AUDIO -> documents.launch(arrayOf("audio/*"))
            FileKind.ANY -> documents.launch(arrayOf("*/*"))
        }
    }
}

@Composable
actual fun rememberSaveCopy(
    store: PageImageStore,
    mime: String?,
    onDone: (Boolean) -> Unit
): (file: String, name: String) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val done by rememberUpdatedState(onDone)
    // Il file da salvare, finché il "Salva in…" di sistema è aperto.
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    val saveAs = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(mime ?: "application/octet-stream")
    ) { target ->
        val file = pending
        if (target == null || file == null) return@rememberLauncherForActivityResult
        scope.launch { done(BlockFiles.copyTo(context, store, file, target)) }
    }
    return { file, name ->
        pending = file
        saveAs.launch(name)
    }
}
