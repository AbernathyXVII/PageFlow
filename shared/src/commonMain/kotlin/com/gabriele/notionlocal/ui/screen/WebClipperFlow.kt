package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.Composable
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel

/**
 * **Il Web Clipper dalla pagina** (dal 09/11/2026 fuori da `PageEditorScreen`):
 * con [open] la finestra per scegliere il sito e come prenderlo, poi la
 * cattura, e il risultato nella pagina. Sul telefono quello di sempre (un
 * browser dentro l'app); sul PC, per ora, non c'è.
 */
@Composable
expect fun WebClipperFlow(open: Boolean, onClose: () -> Unit, viewModel: PageEditorViewModel, imageStore: PageImageStore)
