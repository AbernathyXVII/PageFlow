package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.platform.SystemFileDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.gabriele.notionlocal.data.backup.BackupFile
import com.gabriele.notionlocal.data.backup.Backups
import com.gabriele.notionlocal.platform.PlatformContext
import com.gabriele.notionlocal.ui.i18n.BackupStrings
import kotlinx.coroutines.launch
import java.io.InputStream

@Composable
actual fun rememberBackupUpload(onStart: () -> Unit, onDone: (Boolean) -> Unit): (BackupFile) -> Unit {
    val scope = rememberCoroutineScope()
    val start by rememberUpdatedState(onStart)
    val done by rememberUpdatedState(onDone)
    return { file ->
        scope.launch {
            val target = SystemFileDialog.save(BackupStrings.uploadToProton, file.name, Backups.lastUploadDir) ?: return@launch
            start()
            done(Backups.upload(file, target))
        }
    }
}

@Composable
actual fun rememberBackupPicker(onPicked: (label: String, open: () -> InputStream) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    val picked by rememberUpdatedState(onPicked)
    return {
        scope.launch {
            val chosen = SystemFileDialog.open(BackupStrings.restoreFromFile, setOf("zip")) ?: return@launch
            picked(chosen.name) { chosen.inputStream() }
        }
    }
}

actual fun restartAfterRestore(context: PlatformContext) = relaunchApp()

actual val backupPlaceTitle: String get() = BackupStrings.onThisComputer
actual val backupPlaceNote: String get() = BackupStrings.whereNote(Backups.folder.path)
actual val noBackupsHere: String get() = BackupStrings.noBackupsOnComputer
actual val dailyBackupNote: String get() = BackupStrings.dailyBackupNoteOnPc
actual val uploadNote: String get() = BackupStrings.uploadNoteOnPc
