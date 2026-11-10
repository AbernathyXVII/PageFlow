package com.gabriele.notionlocal.platform

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

actual fun copyRichText(context: PlatformContext, plain: String, html: String): Boolean = runCatching {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(HtmlSelection(plain, html), null)
}.isSuccess

actual val systemAnnouncesCopies: Boolean get() = false

/** Negli appunti di Windows e del Mac: l'HTML per chi lo sa leggere, il testo per gli altri. */
class HtmlSelection(private val plain: String, private val html: String) : Transferable {
    private val htmlFlavor = DataFlavor("text/html;class=java.lang.String")
    private val flavors = arrayOf(htmlFlavor, DataFlavor.stringFlavor)

    override fun getTransferDataFlavors(): Array<DataFlavor> = flavors.clone()

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavors.any { it == flavor }

    override fun getTransferData(flavor: DataFlavor): Any = when (flavor) {
        htmlFlavor -> html
        DataFlavor.stringFlavor -> plain
        else -> throw UnsupportedFlavorException(flavor)
    }
}
