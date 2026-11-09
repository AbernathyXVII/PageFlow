package com.gabriele.notionlocal.data.export

import com.gabriele.notionlocal.platform.PlatformContext
import java.io.File

// shortcut: sul PC il PDF non c'è ancora (il telefono lo disegna con Android); finché manca, "Export" offre solo il Markdown.
actual fun pdfWriter(context: PlatformContext, fileOf: (String) -> File?, untitled: String, drawings: Boolean): PdfWriter? = null
