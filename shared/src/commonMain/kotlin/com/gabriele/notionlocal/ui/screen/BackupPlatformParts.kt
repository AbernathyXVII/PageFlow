package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.Composable
import com.gabriele.notionlocal.data.backup.BackupFile
import com.gabriele.notionlocal.platform.PlatformContext
import java.io.InputStream

/**
 * **"Carica su Proton Drive"**: "Salva come" col nome del backup, aperto dove
 * si è salvato l'ultima volta. Scelto il posto, [onStart]; copiato, [onDone]
 * (vero se è andata).
 */
@Composable
expect fun rememberBackupUpload(onStart: () -> Unit, onDone: (Boolean) -> Unit): (BackupFile) -> Unit

/** **"Ripristina da un file"**: scelto uno zip, [onPicked] col suo nome e il modo di leggerlo. */
@Composable
expect fun rememberBackupPicker(onPicked: (label: String, open: () -> InputStream) -> Unit): () -> Unit

/** Riapre PageFlow dopo un ripristino preparato: la riapertura mette i file al loro posto. */
expect fun restartAfterRestore(context: PlatformContext)

// Le scritte che cambiano fra telefono e PC.
expect val backupPlaceTitle: String
expect val backupPlaceNote: String
expect val noBackupsHere: String
expect val dailyBackupNote: String
expect val uploadNote: String
