package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.gabriele.notionlocal.data.backup.BackupFile
import com.gabriele.notionlocal.data.backup.Backups
import com.gabriele.notionlocal.platform.PlatformContext
import com.gabriele.notionlocal.ui.i18n.BackupStrings
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.InputStream

@Composable
actual fun rememberBackupUpload(onStart: () -> Unit, onDone: (Boolean) -> Unit): (BackupFile) -> Unit {
    val scope = rememberCoroutineScope()
    val start by rememberUpdatedState(onStart)
    val done by rememberUpdatedState(onDone)
    return { file ->
        val dialog = FileDialog(null as Frame?, BackupStrings.uploadToProton, FileDialog.SAVE).apply {
            Backups.lastUploadDir?.let { directory = it }
            this.file = file.name
            isVisible = true
        }
        dialog.file?.let { name ->
            start()
            scope.launch { done(Backups.upload(file, File(dialog.directory, name))) }
        }
    }
}

@Composable
actual fun rememberBackupPicker(onPicked: (label: String, open: () -> InputStream) -> Unit): () -> Unit {
    val picked by rememberUpdatedState(onPicked)
    return {
        val dialog = FileDialog(null as Frame?, BackupStrings.restoreFromFile, FileDialog.LOAD).apply {
            // Su Windows il filtro è il nome proposto; altrove serve il FilenameFilter.
            file = "*.zip"
            setFilenameFilter { _, name -> name.endsWith(".zip", ignoreCase = true) }
            isVisible = true
        }
        dialog.file?.let { name ->
            val chosen = File(dialog.directory, name)
            picked(name) { chosen.inputStream() }
        }
    }
}

actual fun restartAfterRestore(context: PlatformContext) = relaunchApp()

actual val backupPlaceTitle: String get() = BackupStrings.onThisComputer
actual val backupPlaceNote: String get() = BackupStrings.whereNote(Backups.folder.path)
actual val noBackupsHere: String get() = BackupStrings.noBackupsOnComputer
actual val dailyBackupNote: String get() = BackupStrings.dailyBackupNoteOnPc
actual val uploadNote: String get() = BackupStrings.uploadNoteOnPc
