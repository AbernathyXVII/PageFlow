package com.gabriele.notionlocal.data.backup

import java.io.InputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Un backup salvato: come si chiama, quando è nato e quanto pesa (dove sta lo sa ogni sistema). */
expect class BackupFile {
    val name: String
    val createdAt: Long
    val size: Long
}

/**
 * **I backup**, per la pagina Backup (comune dal 09/11/2026): sul telefono in
 * Download/PageFlow backup, sul PC in Documenti/PageFlow backup. Il resto
 * (il backup di ogni giorno, il promemoria, il ripristino all'avvio) ce
 * l'ha ogni sistema per sé.
 */
expect object Backups {
    /** Dopo quanti giorni senza caricare arriva il promemoria; 0 = mai. */
    val REMIND_CHOICES: List<Int>
    var autoBackup: Boolean
        private set
    var remindDays: Int
        private set
    var lastBackupAt: Long
        private set
    var lastUploadAt: Long
        private set

    /** Cresce a ogni backup nuovo, caricato o cancellato: la pagina Backup rilegge l'elenco. */
    var changes: Int
        private set

    fun setAuto(on: Boolean)
    fun setRemind(days: Int)

    /** I backup, dal più nuovo; nessuno se non si leggono. */
    fun list(): List<BackupFile>

    /** Un backup adesso (o ogni giorno, o prima di un ripristino). */
    suspend fun backupNow(reason: BackupReason = BackupReason.MANUAL): BackupFile
    fun delete(file: BackupFile): Boolean
    fun open(file: BackupFile): InputStream

    /** Toglie dallo zip il backup che [open] legge, lo controlla e fa un backup di adesso; lo mette al suo posto la riapertura. */
    suspend fun prepareRestore(open: () -> InputStream): BackupManifest
}

/** Come comincia il nome di un backup: "PageFlow-backup-2026-10-28-1432.zip". */
const val BACKUP_PREFIX = "PageFlow-backup-"

/** Come cominciava quando l'app si chiamava notE (fino al 03/11/2026). */
const val OLD_BACKUP_PREFIX = "notE-backup-"

/** Il nome di un backup, di adesso o di prima. */
fun isBackupName(name: String) =
    (name.startsWith(BACKUP_PREFIX) || name.startsWith(OLD_BACKUP_PREFIX)) && name.endsWith(".zip")

/** Il nome nuovo di un backup col nome di prima. */
fun renamedBackup(name: String) =
    if (name.startsWith(OLD_BACKUP_PREFIX)) BACKUP_PREFIX + name.removePrefix(OLD_BACKUP_PREFIX) else name

/** "PageFlow-backup-2026-10-28-143205.zip"; prima di un ripristino, con "-before-restore"; prima di sincronizzare, "-before-sync". */
fun backupFileName(at: Long, reason: BackupReason, zone: ZoneId): String {
    val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss").format(Instant.ofEpochMilli(at).atZone(zone))
    val tail = when (reason) {
        BackupReason.BEFORE_RESTORE -> "-before-restore"
        BackupReason.BEFORE_SYNC -> "-before-sync"
        else -> ""
    }
    return "$BACKUP_PREFIX$stamp$tail.zip"
}

/** Il backup di ogni giorno è da fare se l'ultimo non è di oggi. */
fun isBackupDue(lastBackupAt: Long, now: Long, zone: ZoneId): Boolean =
    lastBackupAt <= 0 ||
        Instant.ofEpochMilli(lastBackupAt).atZone(zone).toLocalDate() != Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

private const val DAY = 24L * 60 * 60 * 1000

/**
 * Se è ora del promemoria per caricare: passati [remindDays] giorni
 * dall'ultimo caricamento (o dal primo backup, se non se n'è mai caricato
 * uno), e non più di uno al giorno.
 */
fun isUploadReminderDue(now: Long, remindDays: Int, lastUploadAt: Long, firstBackupAt: Long, lastReminderAt: Long): Boolean {
    if (remindDays <= 0 || firstBackupAt <= 0) return false
    val since = maxOf(lastUploadAt, firstBackupAt)
    if (now - since < remindDays * DAY) return false
    return now - lastReminderAt >= DAY
}

/** I giorni interi passati dall'ultimo caricamento, o null se non se n'è mai fatto uno. */
fun daysSince(lastUploadAt: Long, now: Long): Long? = lastUploadAt.takeIf { it > 0 }?.let { (now - it) / DAY }
