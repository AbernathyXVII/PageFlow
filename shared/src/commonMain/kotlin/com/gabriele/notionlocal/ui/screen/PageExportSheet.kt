package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.Composable
import com.gabriele.notionlocal.data.export.ExportOptions
import com.gabriele.notionlocal.data.export.ExportedFile

/**
 * **"Export"** di una pagina o di un database: il formato, e poi salvarlo,
 * mandarlo a un'altra app o stamparlo. [export] prepara il file; null se non
 * riesce. Sul telefono quella di sempre (`PageExportSheet.android.kt`); sul
 * PC, per ora, non c'è.
 */
@Composable
expect fun PageExportSheet(
    title: String,
    /** Se la pagina ha delle sottopagine: allora c'è "Include subpages". */
    hasSubpages: Boolean,
    /** Se la pagina ha dei disegni a mano: allora, per il PDF, c'è "Include drawings" (dal 03/11/2026). */
    hasDrawings: Boolean = false,
    export: suspend (ExportOptions) -> ExportedFile?,
    onDismiss: () -> Unit,
    /** Manda il file a un'altra app; di suo con la scelta di sistema (`PageExporter.share`). */
    share: ((ExportedFile) -> Unit)? = null
)
