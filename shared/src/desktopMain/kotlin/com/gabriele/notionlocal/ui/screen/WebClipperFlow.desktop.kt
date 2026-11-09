package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel

// shortcut: sul PC il Web Clipper si chiude subito (serve un browser dentro l'app), da fare se lo si vuole sul PC.
@Composable
actual fun WebClipperFlow(open: Boolean, onClose: () -> Unit, viewModel: PageEditorViewModel, imageStore: PageImageStore) {
    if (open) LaunchedEffect(Unit) { onClose() }
}
