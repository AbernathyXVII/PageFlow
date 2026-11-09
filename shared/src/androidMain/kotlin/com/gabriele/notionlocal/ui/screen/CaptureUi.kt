package com.gabriele.notionlocal.ui.screen

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.record.AudioRecorder
import com.gabriele.notionlocal.ui.i18n.CaptureStrings
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.gabriele.notionlocal.data.saveFromGallery
import com.gabriele.notionlocal.platform.showMessage
import com.gabriele.notionlocal.platform.LocalPlatformContext


/**
 * Comincia a registrare nel blocco audio [block], chiedendo prima il
 * microfono se serve. Una registrazione già in corso altrove lo dice.
 */
@Composable
actual fun rememberRecordStarter(block: BlockEntity): () -> Unit {
    val context = LocalPlatformContext.current
    val current by rememberUpdatedState(block)
    val start = {
        when {
            AudioRecorder.current.value != null -> toast(context, CaptureStrings.alreadyRecording)
            !AudioRecorder.start(context, current.id, current.pageId) -> toast(context, CaptureStrings.cannotRecord)
        }
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) start() else toast(context, CaptureStrings.micDenied)
    }
    return {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            start()
        } else {
            ask.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

/**
 * **"Scan document"** (dal 03/11/2026): apre lo scanner di Google (ML Kit,
 * dentro Google Play Services) — i bordi del foglio trovati da sé, il
 * ritaglio, i filtri, più pagine di fila — e le pagine diventano foto al
 * posto della riga [String] (l'id del blocco), o sotto di lei. Il loro testo
 * si legge poi per la ricerca (`ImageTexts`).
 */
@Composable
actual fun rememberDocumentScanner(viewModel: PageEditorViewModel): (String) -> Unit {
    val context = LocalPlatformContext.current
    val store = remember(context) { PageImageStore(context) }
    // L'id della riga sopravvive anche se l'app, mentre si scansiona, viene ricreata.
    var target by rememberSaveable { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val blockId = target
        target = null
        if (result.resultCode != Activity.RESULT_OK || blockId == null) return@rememberLauncherForActivityResult
        val pages = GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pages.orEmpty().map { it.imageUri }
        if (pages.isEmpty()) return@rememberLauncherForActivityResult
        viewModel.insertScannedPages(blockId, pages, store, save = { store.saveFromGallery(it).getOrNull() }) {
            toast(context, CaptureStrings.scanFailed)
        }
    }
    return { blockId ->
        val activity = context.findActivity()
        if (activity == null) {
            toast(context, CaptureStrings.scanUnavailable)
        } else {
            target = blockId
            GmsDocumentScanning.getClient(scannerOptions()).getStartScanIntent(activity)
                .addOnSuccessListener { sender -> launcher.launch(IntentSenderRequest.Builder(sender).build()) }
                .addOnFailureListener {
                    target = null
                    toast(context, CaptureStrings.scanUnavailable)
                }
        }
    }
}

private fun scannerOptions(): GmsDocumentScannerOptions = GmsDocumentScannerOptions.Builder()
    .setGalleryImportAllowed(true)
    .setPageLimit(MAX_SCAN_PAGES)
    .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
    .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
    .build()

private fun toast(context: Context, text: String) = showMessage(context, text, long = true)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val MAX_SCAN_PAGES = 50
