package com.gabriele.notionlocal.ui.screen

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.gabriele.notionlocal.data.PageImageStore

/** Sul telefono i file non si trascinano da fuori: la pagina resta com'è. */
actual fun Modifier.outsideFileDrop(
    store: PageImageStore,
    onMove: (Offset?) -> Unit,
    onDrop: (Offset) -> ((List<DroppedFile>) -> Unit)?
): Modifier = this
