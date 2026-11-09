package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.Composable
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.platform.showMessage
import com.gabriele.notionlocal.ui.i18n.CaptureStrings
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel

@Composable
actual fun rememberRecordStarter(block: BlockEntity): () -> Unit {
    val context = LocalPlatformContext.current
    return { showMessage(context, CaptureStrings.cannotRecord) }
}

// shortcut: sul PC "Scan document" non fa niente (non c'è la fotocamera), da togliere dal menu se dà fastidio.
@Composable
actual fun rememberDocumentScanner(viewModel: PageEditorViewModel): (String) -> Unit = { }
