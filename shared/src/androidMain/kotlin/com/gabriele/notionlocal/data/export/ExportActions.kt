package com.gabriele.notionlocal.data.export

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.FileProvider
import com.gabriele.notionlocal.platform.PlatformContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

actual fun pdfWriter(context: PlatformContext, fileOf: (String) -> File?, untitled: String, drawings: Boolean): PdfWriter? {
    val pdf = PdfExport(fileOf, untitled, if (drawings) PdfLayout.phone(context) else PdfLayout.DOCUMENT)
    return PdfWriter { pages, out -> pdf.write(pages, out) }
}

/** **Mandare, salvare, stampare** un file esportato, sul telefono. */
object ExportActions {
    private fun uriOf(context: Context, exported: ExportedFile): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.files", exported.file)

    /** **Manda** il file con un'altra app (WhatsApp, la posta, Drive…), con la scelta di sistema. */
    fun share(context: Context, exported: ExportedFile, title: String) {
        val chooser = shareIntent(uriOf(context, exported), exported, title)
        if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /** La scelta di sistema delle app a cui mandare il file [uri]. */
    fun shareIntent(uri: Uri, exported: ExportedFile, title: String): Intent {
        val send = Intent(Intent.ACTION_SEND)
            .setType(exported.mime)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, title)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(exported.name, uri)
        return Intent.createChooser(send, null)
    }

    /** **Salva** il file dove l'utente ha scelto (vedi [SaveExport]). */
    suspend fun copyTo(context: Context, exported: ExportedFile, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri, "wt")!!.use { out ->
                FileInputStream(exported.file).use { it.copyTo(out) }
            }
        }.isSuccess
    }

    /** **Stampa** il PDF, con la finestra di stampa di sistema (che sa anche salvarlo come PDF). */
    fun print(context: Context, exported: ExportedFile, title: String) {
        val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val attributes = PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build()
        manager.print(title, FilePrintAdapter(exported.file, exported.name), attributes)
    }
}

/**
 * La finestra di sistema "Salva in…": il nome e il tipo del file sono quelli
 * del file esportato. Restituisce dove salvarlo, o null se l'utente ha
 * annullato.
 */
class SaveExport : ActivityResultContract<ExportedFile, Uri?>() {
    override fun createIntent(context: Context, input: ExportedFile): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(input.mime)
            .putExtra(Intent.EXTRA_TITLE, input.name)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        intent?.data?.takeIf { resultCode == Activity.RESULT_OK }
}

/** Passa alla stampa un PDF già pronto: i fogli sono già impaginati in A4. */
private class FilePrintAdapter(private val file: File, private val name: String) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (cancellationSignal.isCanceled) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder(name)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .build()
        callback.onLayoutFinished(info, oldAttributes == null)
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback
    ) {
        try {
            FileInputStream(file).use { input ->
                FileOutputStream(destination.fileDescriptor).use { input.copyTo(it) }
            }
            if (cancellationSignal.isCanceled) callback.onWriteCancelled() else callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed(e.message)
        }
    }
}
