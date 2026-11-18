package com.gabriele.notionlocal.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.gabriele.notionlocal.data.backup.BackupException
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.platform.PlatformContext
import com.gabriele.notionlocal.ui.i18n.BackupStrings
import java.io.InputStream

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

/** Il nome del file scelto, per la domanda prima del ripristino. */
private fun fileLabel(context: Context, uri: Uri): String =
    runCatching {
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment.orEmpty()
