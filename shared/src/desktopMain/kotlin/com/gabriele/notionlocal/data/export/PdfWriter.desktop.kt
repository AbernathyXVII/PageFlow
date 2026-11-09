package com.gabriele.notionlocal.data.export

import com.gabriele.notionlocal.platform.PlatformContext
import java.io.File

/**
 * Il PDF del PC (vedi `DesktopPdfBackend`); null senza i caratteri di Windows.
 * Coi disegni, le proporzioni di un telefono largo [PHONE_WIDTH_DP]: i tratti
 * sono in dp di quello su cui sono fatti, che qui non si sa.
 */
actual fun pdfWriter(context: PlatformContext, fileOf: (String) -> File?, untitled: String, drawings: Boolean): PdfWriter? {
    val backend = defaultPdfBackend() ?: return null
    val pdf = PdfExport(fileOf, untitled, if (drawings) PdfLayout.phone(PHONE_WIDTH_DP) else PdfLayout.DOCUMENT, backend = backend)
    return PdfWriter { pages, out -> pdf.write(pages, out) }
}

/** La larghezza più comune di un telefono Android, in dp. */
private const val PHONE_WIDTH_DP = 411f
