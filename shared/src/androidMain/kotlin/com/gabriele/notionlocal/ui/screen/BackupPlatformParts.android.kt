package com.gabriele.notionlocal.ui.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.gabriele.notionlocal.data.backup.BackupException
import com.gabriele.notionlocal.data.backup.BackupFile
import com.gabriele.notionlocal.data.backup.Backups
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.platform.PlatformContext
import com.gabriele.notionlocal.ui.i18n.BackupStrings
import kotlinx.coroutines.launch
import java.io.InputStream

@Composable
actual fun rememberBackupUpload(onStart: () -> Unit, onDone: (Boolean) -> Unit): (BackupFile) -> Unit {
    val scope = rememberCoroutineScope()
    var toUpload by remember { mutableStateOf<BackupFile?>(null) }
    val launcher = rememberLauncherForActivityResult(CreateZipDocument()) { uri ->
        val file = toUpload
        toUpload = null
        if (uri != null && file != null) {
            onStart()
            scope.launch { onDone(Backups.upload(file, uri)) }
        }
    }
    return { file ->
        toUpload = file
        launcher.launch(file.name to Backups.lastUploadUri?.let(Uri::parse))
    }
}

@Composable
actual fun rememberBackupPicker(onPicked: (label: String, open: () -> InputStream) -> Unit): () -> Unit {
    val context = LocalPlatformContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            onPicked(fileLabel(context, uri)) {
                context.contentResolver.openInputStream(uri) ?: throw BackupException(BackupException.Problem.NOT_A_BACKUP, "Non si apre")
            }
        }
    }
    return { launcher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) }
}

/**
 * **Riapre notE da capo** dopo un ripristino preparato: la nuova apertura
 * mette i file al loro posto prima di leggerli (`Backups.applyPendingRestore`).
 */
actual fun restartAfterRestore(context: PlatformContext) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) ?: return
    context.startActivity(intent)
    Runtime.getRuntime().exit(0)
}

actual val backupPlaceTitle: String get() = BackupStrings.onThisPhone
actual val backupPlaceNote: String get() = BackupStrings.whereNote("Download/PageFlow backup")
actual val noBackupsHere: String get() = BackupStrings.noBackupsOnPhone
actual val dailyBackupNote: String get() = BackupStrings.dailyBackupNote
actual val uploadNote: String get() = BackupStrings.protonNote

/** Il nome del file scelto, per la domanda prima del ripristino. */
private fun fileLabel(context: Context, uri: Uri): String =
    runCatching {
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment.orEmpty()

/**
 * **"Salva come"** per un backup: il nome già scritto e, dalla seconda volta,
 * la finestra aperta dove si è salvato l'ultima volta (su Proton Drive).
 */
private class CreateZipDocument : ActivityResultContract<Pair<String, Uri?>, Uri?>() {
    override fun createIntent(context: Context, input: Pair<String, Uri?>): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/zip")
            .putExtra(Intent.EXTRA_TITLE, input.first)
            .apply { input.second?.let { putExtra(DocumentsContract.EXTRA_INITIAL_URI, it) } }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        intent?.data?.takeIf { resultCode == Activity.RESULT_OK }
}
