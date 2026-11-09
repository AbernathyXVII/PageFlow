package com.gabriele.notionlocal.ui.i18n

/**
 * Le scritte dei backup (dal 28/10/2026): la pagina Backup, "Carica su
 * Proton Drive", il ripristino e il promemoria. Stessa regola di `Strings`:
 * ogni voce con le sue otto traduzioni.
 */
object BackupStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    // --- Com'è adesso ---

    val lastBackup get() = t("Last backup", "Ultimo backup", "Letzte Sicherung", "Dernière sauvegarde", "Última copia", "上次备份", "마지막 백업", "前回のバックアップ")
    val noBackupYet get() = t("No backup yet", "Ancora nessun backup", "Noch keine Sicherung", "Aucune sauvegarde", "Aún no hay copias", "还没有备份", "아직 백업이 없어요", "まだバックアップはありません")
    val lastUpload get() = t("Last upload to Proton Drive", "Ultimo caricamento su Proton Drive", "Letzter Upload zu Proton Drive", "Dernier envoi sur Proton Drive", "Última subida a Proton Drive", "上次上传到 Proton Drive", "Proton Drive 마지막 업로드", "Proton Drive への前回のアップロード")
    val neverUploaded get() = t("Never", "Mai", "Nie", "Jamais", "Nunca", "从未", "없음", "なし")
    fun todayAt(time: String) = t("Today at $time", "Oggi alle $time", "Heute um $time", "Aujourd'hui à $time", "Hoy a las $time", "今天 $time", "오늘 $time", "今日 $time")
    fun yesterdayAt(time: String) = t("Yesterday at $time", "Ieri alle $time", "Gestern um $time", "Hier à $time", "Ayer a las $time", "昨天 $time", "어제 $time", "昨日 $time")

    // --- Fare e caricare ---

    val backupNow get() = t("Back up now", "Fai un backup adesso", "Jetzt sichern", "Sauvegarder maintenant", "Hacer copia ahora", "立即备份", "지금 백업", "今すぐバックアップ")
    val backingUp get() = t("Backing up…", "Backup in corso…", "Sicherung läuft…", "Sauvegarde…", "Haciendo copia…", "正在备份…", "백업 중…", "バックアップ中…")
    val backupDone get() = t("Backup saved", "Backup salvato", "Sicherung gespeichert", "Sauvegarde enregistrée", "Copia guardada", "备份已保存", "백업을 저장했어요", "バックアップを保存しました")
    val backupFailed get() = t("The backup didn't work", "Il backup non è riuscito", "Die Sicherung ist fehlgeschlagen", "La sauvegarde a échoué", "La copia no se pudo hacer", "备份失败", "백업하지 못했어요", "バックアップできませんでした")
    val uploadToProton get() = t("Upload to Proton Drive", "Carica su Proton Drive", "Zu Proton Drive hochladen", "Envoyer sur Proton Drive", "Subir a Proton Drive", "上传到 Proton Drive", "Proton Drive에 업로드", "Proton Drive にアップロード")
    val uploading get() = t("Uploading…", "Caricamento…", "Wird hochgeladen…", "Envoi…", "Subiendo…", "正在上传…", "업로드 중…", "アップロード中…")
    val uploaded get() = t("Backup uploaded", "Backup caricato", "Sicherung hochgeladen", "Sauvegarde envoyée", "Copia subida", "备份已上传", "백업을 업로드했어요", "バックアップをアップロードしました")
    val uploadFailed get() = t("The upload didn't work", "Il caricamento non è riuscito", "Der Upload ist fehlgeschlagen", "L'envoi a échoué", "No se pudo subir", "上传失败", "업로드하지 못했어요", "アップロードできませんでした")
    val protonNote get() = t(
        "Proton Drive doesn't let apps save on their own: in the window that opens, choose the folder the first time, then just tap Save.",
        "Proton Drive non lascia salvare le app da sole: nella finestra che si apre scegli la cartella la prima volta, poi basta toccare Salva.",
        "Proton Drive lässt Apps nicht selbst speichern: Wähle im Fenster beim ersten Mal den Ordner, danach einfach auf Speichern tippen.",
        "Proton Drive ne laisse pas les applis enregistrer seules : dans la fenêtre, choisissez le dossier la première fois, puis touchez Enregistrer.",
        "Proton Drive no deja que las apps guarden solas: en la ventana elige la carpeta la primera vez, luego solo toca Guardar.",
        "Proton Drive 不允许应用自行保存：第一次在弹出的窗口中选择文件夹，之后只需点按“保存”。",
        "Proton Drive는 앱이 직접 저장하지 못하게 해요. 처음에 열리는 창에서 폴더를 고르면, 다음부터는 저장만 누르면 돼요.",
        "Proton Drive はアプリによる自動保存を許可していません。初回は開いた画面でフォルダーを選び、次からは「保存」をタップするだけです。"
    )

    // --- Ogni giorno e il promemoria ---

    val automatic get() = t("Automatic", "Automatico", "Automatisch", "Automatique", "Automático", "自动", "자동", "自動")
    val dailyBackup get() = t("Daily backup", "Backup ogni giorno", "Tägliche Sicherung", "Sauvegarde quotidienne", "Copia diaria", "每日备份", "매일 백업", "毎日のバックアップ")
    val dailyBackupNote get() = t(
        "Once a day, when you leave the app. The last 7 are kept.",
        "Una volta al giorno, quando esci dall'app. Si tengono gli ultimi 7.",
        "Einmal am Tag, wenn du die App verlässt. Die letzten 7 bleiben.",
        "Une fois par jour, quand vous quittez l'appli. Les 7 dernières sont gardées.",
        "Una vez al día, al salir de la app. Se guardan las últimas 7.",
        "每天一次，在离开应用时。保留最近 7 份。",
        "하루에 한 번, 앱을 나갈 때요. 최근 7개를 남겨요.",
        "1日1回、アプリを離れるときに。最新の7件を残します。"
    )
    val reminder get() = t("Upload reminder", "Promemoria per caricare", "Upload-Erinnerung", "Rappel d'envoi", "Recordatorio de subida", "上传提醒", "업로드 알림", "アップロードのリマインダー")
    fun afterDays(days: Int) = t("After $days days", "Dopo $days giorni", "Nach $days Tagen", "Après $days jours", "Después de $days días", "$days 天后", "${days}일 후", "${days}日後")
    val never get() = t("Never", "Mai", "Nie", "Jamais", "Nunca", "从不", "안 함", "しない")
    val reminderTitle get() = t("Upload a backup to Proton Drive", "Carica un backup su Proton Drive", "Lade eine Sicherung zu Proton Drive hoch", "Envoyez une sauvegarde sur Proton Drive", "Sube una copia a Proton Drive", "将备份上传到 Proton Drive", "Proton Drive에 백업을 업로드하세요", "Proton Drive にバックアップをアップロード")
    fun reminderText(days: Long?): String = if (days == null) {
        t(
            "You haven't uploaded one yet: tap to do it.",
            "Non ne hai ancora caricato nessuno: tocca per farlo.",
            "Du hast noch keine hochgeladen: Tippe hier.",
            "Vous n'en avez encore envoyé aucune : touchez pour le faire.",
            "Aún no has subido ninguna: toca para hacerlo.",
            "你还没有上传过备份：点按即可上传。",
            "아직 업로드한 백업이 없어요. 눌러서 업로드하세요.",
            "まだアップロードしていません。タップしてアップロードしてください。"
        )
    } else {
        t(
            "The last one is from $days days ago: tap to upload a new one.",
            "L'ultimo è di $days giorni fa: tocca per caricarne uno nuovo.",
            "Die letzte ist $days Tage alt: Tippe, um eine neue hochzuladen.",
            "La dernière date de $days jours : touchez pour en envoyer une nouvelle.",
            "La última es de hace $days días: toca para subir una nueva.",
            "上一次是 $days 天前：点按上传新的备份。",
            "마지막 업로드는 ${days}일 전이에요. 눌러서 새로 업로드하세요.",
            "前回は${days}日前です。タップして新しいものをアップロードしてください。"
        )
    }

    // --- Sul telefono ---

    val onThisPhone get() = t("On this phone", "Su questo telefono", "Auf diesem Telefon", "Sur ce téléphone", "En este teléfono", "此手机上", "이 휴대폰", "このスマートフォン")
    val whereNote get() = t(
        "In Download/PageFlow backup: they stay even if PageFlow is uninstalled.",
        "In Download/PageFlow backup: restano anche se PageFlow viene disinstallata.",
        "In Download/PageFlow backup: Sie bleiben auch, wenn PageFlow deinstalliert wird.",
        "Dans Download/PageFlow backup : elles restent même si PageFlow est désinstallée.",
        "En Download/PageFlow backup: se quedan aunque desinstales PageFlow.",
        "位于 Download/PageFlow backup：即使卸载 PageFlow 也会保留。",
        "Download/PageFlow backup에 있어요. PageFlow를 삭제해도 남아요.",
        "Download/PageFlow backup にあります。PageFlow をアンインストールしても残ります。"
    )
    val noBackupsOnPhone get() = t(
        "There are no backups on this phone yet.",
        "Su questo telefono non ci sono ancora backup.",
        "Auf diesem Telefon gibt es noch keine Sicherungen.",
        "Il n'y a pas encore de sauvegarde sur ce téléphone.",
        "Todavía no hay copias en este teléfono.",
        "此手机上还没有备份。",
        "이 휴대폰에는 아직 백업이 없어요.",
        "このスマートフォンにはまだバックアップがありません。"
    )
    val beforeRestore get() = t("Before a restore", "Prima di un ripristino", "Vor einer Wiederherstellung", "Avant une restauration", "Antes de restaurar", "恢复之前", "복원 전", "復元前")
    val deleteTitle get() = t("Delete this backup?", "Eliminare questo backup?", "Diese Sicherung löschen?", "Supprimer cette sauvegarde ?", "¿Eliminar esta copia?", "删除此备份？", "이 백업을 삭제할까요?", "このバックアップを削除しますか？")

    // --- Il ripristino ---

    val restore get() = t("Restore", "Ripristina", "Wiederherstellen", "Restaurer", "Restaurar", "恢复", "복원", "復元")
    val restoreFromFile get() = t("Restore from a file…", "Ripristina da un file…", "Aus einer Datei wiederherstellen…", "Restaurer depuis un fichier…", "Restaurar desde un archivo…", "从文件恢复…", "파일에서 복원…", "ファイルから復元…")
    val restoreFromFileNote get() = t(
        "A PageFlow backup, also downloaded from Proton Drive.",
        "Un backup di PageFlow, anche scaricato da Proton Drive.",
        "Eine PageFlow-Sicherung, auch von Proton Drive heruntergeladen.",
        "Une sauvegarde PageFlow, même téléchargée depuis Proton Drive.",
        "Una copia de PageFlow, también descargada de Proton Drive.",
        "PageFlow 备份，也可以是从 Proton Drive 下载的。",
        "PageFlow 백업이에요. Proton Drive에서 받은 것도 돼요.",
        "PageFlow のバックアップ（Proton Drive からダウンロードしたものも可）。"
    )
    val restoreTitle get() = t("Restore this backup?", "Ripristinare questo backup?", "Diese Sicherung wiederherstellen?", "Restaurer cette sauvegarde ?", "¿Restaurar esta copia?", "恢复此备份？", "이 백업을 복원할까요?", "このバックアップを復元しますか？")
    fun restoreText(date: String) = t(
        "Everything in PageFlow now will be replaced by the backup of $date. First a backup of now is made, so you can go back. Then PageFlow restarts.",
        "Tutto quello che c'è ora in PageFlow sarà sostituito dal backup del $date. Prima si fa un backup di adesso, così puoi tornare indietro. Poi PageFlow si riavvia.",
        "Alles, was jetzt in PageFlow ist, wird durch die Sicherung vom $date ersetzt. Vorher wird der jetzige Stand gesichert, damit du zurückkannst. Dann startet PageFlow neu.",
        "Tout ce qui est dans PageFlow sera remplacé par la sauvegarde du $date. Une sauvegarde de l'état actuel est faite d'abord, pour pouvoir revenir en arrière. Puis PageFlow redémarre.",
        "Todo lo que hay ahora en PageFlow se sustituirá por la copia del $date. Antes se hace una copia de ahora, para poder volver atrás. Luego PageFlow se reinicia.",
        "PageFlow 中现在的所有内容都将被 $date 的备份替换。会先备份当前内容，以便你还原。随后 PageFlow 会重新启动。",
        "지금 PageFlow에 있는 모든 내용이 $date 백업으로 바뀌어요. 먼저 지금 상태를 백업해서 되돌릴 수 있어요. 그다음 PageFlow가 다시 시작돼요.",
        "PageFlow の現在の内容はすべて $date のバックアップに置き換わります。先に現在の状態をバックアップするので元に戻せます。その後 PageFlow が再起動します。"
    )
    val restoring get() = t("Restoring…", "Ripristino in corso…", "Wird wiederhergestellt…", "Restauration…", "Restaurando…", "正在恢复…", "복원 중…", "復元中…")
    val notABackup get() = t("This file isn't a PageFlow backup", "Questo file non è un backup di PageFlow", "Diese Datei ist keine PageFlow-Sicherung", "Ce fichier n'est pas une sauvegarde PageFlow", "Este archivo no es una copia de PageFlow", "此文件不是 PageFlow 备份", "이 파일은 PageFlow 백업이 아니에요", "このファイルは PageFlow のバックアップではありません")
    val tooNew get() = t(
        "This backup comes from a newer PageFlow: update the app first",
        "Questo backup viene da una versione più nuova di PageFlow: prima aggiorna l'app",
        "Diese Sicherung stammt von einer neueren PageFlow-Version: Aktualisiere zuerst die App",
        "Cette sauvegarde vient d'une version plus récente de PageFlow : mettez d'abord l'appli à jour",
        "Esta copia viene de una versión más nueva de PageFlow: actualiza primero la app",
        "此备份来自较新版本的 PageFlow：请先更新应用",
        "더 최신 PageFlow의 백업이에요. 먼저 앱을 업데이트하세요",
        "このバックアップは新しいバージョンの PageFlow のものです。先にアプリを更新してください"
    )
    val restoreClosesOnPc get() = t(
        "On this computer PageFlow then closes: open it again to find the backup. What the computer has now is kept in its data folder, in \"before-restore\".",
        "Su questo computer PageFlow poi si chiude: riaprila per trovare il backup. Quello che il computer ha adesso resta nella sua cartella dei dati, in \"before-restore\".",
        "Auf diesem Computer wird PageFlow dann geschlossen: Öffne es erneut, um die Sicherung zu sehen. Der jetzige Stand bleibt im Datenordner unter \"before-restore\".",
        "Sur cet ordinateur, PageFlow se ferme ensuite : rouvrez-le pour retrouver la sauvegarde. Ce que l'ordinateur contient maintenant reste dans son dossier de données, sous \"before-restore\".",
        "En este ordenador PageFlow se cierra después: ábrelo de nuevo para ver la copia. Lo que el ordenador tiene ahora queda en su carpeta de datos, en \"before-restore\".",
        "在这台电脑上，PageFlow 随后会关闭：重新打开即可看到备份。电脑上现有的内容会保留在数据文件夹的 \"before-restore\" 中。",
        "이 컴퓨터에서는 PageFlow가 닫혀요. 다시 열면 백업이 보여요. 지금 컴퓨터에 있는 내용은 데이터 폴더의 \"before-restore\"에 남아요.",
        "このコンピューターでは PageFlow がその後閉じます。もう一度開くとバックアップが表示されます。今の内容はデータフォルダーの \"before-restore\" に残ります。"
    )
    val tooOldForPc get() = t(
        "This backup comes from an older PageFlow: update the app on the phone, make a new backup and open that one",
        "Questo backup viene da una versione più vecchia di PageFlow: aggiorna l'app sul telefono, fai un nuovo backup e apri quello",
        "Diese Sicherung stammt von einer älteren PageFlow-Version: Aktualisiere die App auf dem Handy, erstelle eine neue Sicherung und öffne diese",
        "Cette sauvegarde vient d'une version plus ancienne de PageFlow : mettez à jour l'appli sur le téléphone, faites une nouvelle sauvegarde et ouvrez-la",
        "Esta copia viene de una versión más antigua de PageFlow: actualiza la app en el móvil, haz una copia nueva y abre esa",
        "此备份来自较旧版本的 PageFlow：请在手机上更新应用，重新备份后打开新的备份",
        "이전 PageFlow의 백업이에요. 휴대폰에서 앱을 업데이트하고 새로 백업한 뒤 그 백업을 여세요",
        "このバックアップは古いバージョンの PageFlow のものです。スマホのアプリを更新し、新しくバックアップしてそれを開いてください"
    )
    val restoreFailed get() = t("The restore didn't work", "Il ripristino non è riuscito", "Die Wiederherstellung ist fehlgeschlagen", "La restauration a échoué", "No se pudo restaurar", "恢复失败", "복원하지 못했어요", "復元できませんでした")
}
