package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.record.AudioRecorder
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.platform.showMessage
import com.gabriele.notionlocal.ui.i18n.CaptureStrings
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel

/** "Record" sul PC: il microfono di Windows, subito (non c'è un permesso da chiedere come sul telefono). */
@Composable
actual fun rememberRecordStarter(block: BlockEntity): () -> Unit {
    val context = LocalPlatformContext.current
    val current by rememberUpdatedState(block)
    return {
        when {
            AudioRecorder.current.value != null -> showMessage(context, CaptureStrings.alreadyRecording)
            !AudioRecorder.start(context, current.id, current.pageId) -> showMessage(context, CaptureStrings.cannotRecord)
        }
    }
}

// shortcut: sul PC "Scan document" non fa niente (non c'è la fotocamera), da togliere dal menu se dà fastidio.
@Composable
actual fun rememberDocumentScanner(viewModel: PageEditorViewModel): (String) -> Unit = { }
