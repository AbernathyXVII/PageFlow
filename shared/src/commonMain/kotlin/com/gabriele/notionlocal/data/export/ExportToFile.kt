package com.gabriele.notionlocal.data.export

import com.gabriele.notionlocal.platform.PlatformContext
import com.gabriele.notionlocal.viewmodel.ViewModelFactory

/**
 * **Una pagina o un database in un file** (Markdown o PDF, vedi [ExportOptions]),
 * preparato da [PageExporter]. Null se non riesce, o se questo sistema non sa
 * fare il formato chiesto (il PDF sul PC, per ora).
 */
suspend fun exportToFile(
    context: PlatformContext,
    factory: ViewModelFactory,
    pageId: String,
    options: ExportOptions,
    untitled: String
): ExportedFile? = runCatching {
    PageExporter(context, factory.pageRepository, DatabaseReader(factory, factory.pageRepository)).export(pageId, options, untitled)
}.getOrNull()
