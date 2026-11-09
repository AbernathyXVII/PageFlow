package com.gabriele.notionlocal.data.record

import com.gabriele.notionlocal.platform.PlatformContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual object AudioRecorder {
    actual val current: StateFlow<Recording?> = MutableStateFlow(null)

    actual fun start(context: PlatformContext, blockId: String, pageId: String): Boolean = false

    actual fun level(): Float = 0f

    actual fun stop(context: PlatformContext, keep: Boolean) = Unit
}
