package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.Composable
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.platform.showMessage
import com.gabriele.notionlocal.ui.i18n.CaptureStrings

@Composable
actual fun rememberRecordStarter(block: BlockEntity): () -> Unit {
    val context = LocalPlatformContext.current
    return { showMessage(context, CaptureStrings.cannotRecord) }
}
