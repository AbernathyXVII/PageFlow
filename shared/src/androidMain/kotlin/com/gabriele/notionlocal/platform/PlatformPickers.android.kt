package com.gabriele.notionlocal.platform

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalView
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.saveFromGallery
import kotlinx.coroutines.launch

@Composable
actual fun rememberImagePicker(
    store: PageImageStore,
    onStart: () -> Unit,
    onPicked: (Result<String>) -> Unit
): () -> Unit {
    val scope = rememberCoroutineScope()
    val start by rememberUpdatedState(onStart)
    val picked by rememberUpdatedState(onPicked)
    val gallery = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        start()
        scope.launch { picked(store.saveFromGallery(uri)) }
    }
    return remember(gallery) { { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } }
}

actual fun urlHost(url: String): String? = runCatching { Uri.parse(url).host }.getOrNull()

actual fun urlScheme(url: String): String? = runCatching { Uri.parse(url).scheme }.getOrNull()

actual fun urlLastPathSegment(url: String): String? = runCatching { Uri.parse(url).lastPathSegment }.getOrNull()

@Composable
actual fun rememberRootHeight(): () -> Int {
    val view = LocalView.current
    return remember(view) { { view.height } }
}
