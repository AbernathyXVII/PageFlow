package com.gabriele.notionlocal.ui.i18n

import com.gabriele.notionlocal.ui.screen.Biometrics

/** Le scritte delle pagine bloccate con la password (dal 26/09/2026); sul PC l'impronta è Windows Hello (dal 10/11/2026). */
object LockStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    val passwordLock get() = t("Lock with password", "Blocca con password", "Mit Passwort sperren", "Verrouiller par mot de passe", "Bloquear con contraseña", "用密码锁定", "비밀번호로 잠그기", "パスワードでロック")
    val lockedTitle get() = t("This page is locked", "Questa pagina è bloccata", "Diese Seite ist gesperrt", "Cette page est verrouillée", "Esta página está bloqueada", "此页面已锁定", "이 페이지는 잠겨 있습니다", "このページはロックされています")
    val lockedDatabase get() = t("This database is locked", "Questo database è bloccato", "Diese Datenbank ist gesperrt", "Cette base de données est verrouillée", "Esta base de datos está bloqueada", "此数据库已锁定", "이 데이터베이스는 잠겨 있습니다", "このデータベースはロックされています")
    val lockedText get() = if (Biometrics.windowsHello) t(
        "Unlock it with Windows Hello or the password.",
        "Aprila con Windows Hello o con la password.",
        "Mit Windows Hello oder Passwort entsperren.",
        "Déverrouillez-la avec Windows Hello ou le mot de passe.",
        "Desbloquéala con Windows Hello o con la contraseña.",
        "使用 Windows Hello 或密码解锁。",
        "Windows Hello나 비밀번호로 잠금을 해제하세요.",
        "Windows Hello またはパスワードでロックを解除してください。"
    ) else t(
        "Unlock it with your fingerprint or the password.",
        "Aprila con l'impronta o con la password.",
        "Mit Fingerabdruck oder Passwort entsperren.",
        "Déverrouillez-la avec votre empreinte ou le mot de passe.",
        "Desbloquéala con tu huella o con la contraseña.",
        "使用指纹或密码解锁。",
        "지문이나 비밀번호로 잠금을 해제하세요.",
        "指紋またはパスワードでロックを解除してください。"
    )
    val lockedTextPasswordOnly get() = t(
        "Unlock it with the password.",
        "Aprila con la password.",
        "Mit dem Passwort entsperren.",
        "Déverrouillez-la avec le mot de passe.",
        "Desbloquéala con la contraseña.",
        "使用密码解锁。",
        "비밀번호로 잠금을 해제하세요.",
        "パスワードでロックを解除してください。"
    )
    val unlock get() = t("Unlock", "Sblocca", "Entsperren", "Déverrouiller", "Desbloquear", "解锁", "잠금 해제", "ロック解除")
    val useFingerprint get() = if (Biometrics.windowsHello) t("Use Windows Hello", "Usa Windows Hello", "Windows Hello verwenden", "Utiliser Windows Hello", "Usar Windows Hello", "使用 Windows Hello", "Windows Hello 사용", "Windows Hello を使用") else t("Use fingerprint", "Usa l'impronta", "Fingerabdruck verwenden", "Utiliser l'empreinte", "Usar huella", "使用指纹", "지문 사용", "指紋を使用")
    val usePassword get() = t("Use password", "Usa la password", "Passwort verwenden", "Utiliser le mot de passe", "Usar contraseña", "使用密码", "비밀번호 사용", "パスワードを使用")
    val password get() = t("Password", "Password", "Passwort", "Mot de passe", "Contraseña", "密码", "비밀번호", "パスワード")
    val repeatPassword get() = t("Repeat the password", "Ripeti la password", "Passwort wiederholen", "Répétez le mot de passe", "Repite la contraseña", "再次输入密码", "비밀번호 다시 입력", "パスワードをもう一度入力")
    val wrongPassword get() = t("Wrong password", "Password sbagliata", "Falsches Passwort", "Mot de passe incorrect", "Contraseña incorrecta", "密码错误", "비밀번호가 틀렸습니다", "パスワードが違います")
    val passwordsDiffer get() = t("The two passwords are different", "Le due password sono diverse", "Die Passwörter stimmen nicht überein", "Les deux mots de passe sont différents", "Las dos contraseñas son distintas", "两次输入的密码不同", "두 비밀번호가 다릅니다", "2つのパスワードが一致しません")
    val passwordTooShort get() = t("At least 4 characters", "Almeno 4 caratteri", "Mindestens 4 Zeichen", "Au moins 4 caractères", "Al menos 4 caracteres", "至少 4 个字符", "4자 이상", "4文字以上")
    val setPasswordTitle get() = t("Password for locked pages", "Password delle pagine bloccate", "Passwort für gesperrte Seiten", "Mot de passe des pages verrouillées", "Contraseña de las páginas bloqueadas", "锁定页面的密码", "잠긴 페이지 비밀번호", "ロックしたページのパスワード")
    val setPasswordText get() = if (Biometrics.windowsHello) t(
        "One password for all the locked pages. If you forget it, you can set a new one after confirming with Windows Hello.",
        "Una password sola per tutte le pagine bloccate. Se la dimentichi, ne puoi mettere una nuova confermando con Windows Hello.",
        "Ein Passwort für alle gesperrten Seiten. Wenn du es vergisst, kannst du nach der Bestätigung mit Windows Hello ein neues festlegen.",
        "Un seul mot de passe pour toutes les pages verrouillées. Si vous l'oubliez, vous pouvez en définir un nouveau après confirmation avec Windows Hello.",
        "Una sola contraseña para todas las páginas bloqueadas. Si la olvidas, puedes poner otra tras confirmar con Windows Hello.",
        "所有锁定页面共用一个密码。如果忘记了，可以用 Windows Hello 验证后设置新密码。",
        "잠긴 모든 페이지에 비밀번호 하나를 사용합니다. 잊어버리면 Windows Hello로 확인한 후 새 비밀번호를 설정할 수 있습니다.",
        "ロックしたすべてのページに共通のパスワードです。忘れた場合は、Windows Hello で確認してから新しいパスワードを設定できます。"
    ) else t(
        "One password for all the locked pages. If you forget it, you can set a new one with your phone's screen lock.",
        "Una password sola per tutte le pagine bloccate. Se la dimentichi, ne puoi mettere una nuova col blocco schermo del telefono.",
        "Ein Passwort für alle gesperrten Seiten. Wenn du es vergisst, kannst du mit der Displaysperre des Telefons ein neues festlegen.",
        "Un seul mot de passe pour toutes les pages verrouillées. Si vous l'oubliez, vous pouvez en définir un nouveau avec le verrouillage d'écran du téléphone.",
        "Una sola contraseña para todas las páginas bloqueadas. Si la olvidas, puedes poner otra con el bloqueo de pantalla del teléfono.",
        "所有锁定页面共用一个密码。如果忘记了，可以用手机的锁屏验证后设置新密码。",
        "잠긴 모든 페이지에 비밀번호 하나를 사용합니다. 잊어버리면 휴대전화 화면 잠금으로 새 비밀번호를 설정할 수 있습니다.",
        "ロックしたすべてのページに共通のパスワードです。忘れた場合は、スマートフォンの画面ロックで新しいパスワードを設定できます。"
    )
    val alsoFingerprint get() = if (Biometrics.windowsHello) t("Unlock also with Windows Hello", "Sblocca anche con Windows Hello", "Auch mit Windows Hello entsperren", "Déverrouiller aussi avec Windows Hello", "Desbloquear también con Windows Hello", "也可用 Windows Hello 解锁", "Windows Hello로도 잠금 해제", "Windows Hello でもロック解除") else t("Unlock also with fingerprint", "Sblocca anche con l'impronta", "Auch mit Fingerabdruck entsperren", "Déverrouiller aussi avec l'empreinte", "Desbloquear también con huella", "也可用指纹解锁", "지문으로도 잠금 해제", "指紋でもロック解除")
    val forgotPassword get() = t("Forgot the password?", "Password dimenticata?", "Passwort vergessen?", "Mot de passe oublié ?", "¿Olvidaste la contraseña?", "忘记密码？", "비밀번호를 잊으셨나요?", "パスワードを忘れた場合")
    val confirmItsYou get() = t("Confirm it's you", "Conferma che sei tu", "Bestätige, dass du es bist", "Confirmez que c'est vous", "Confirma que eres tú", "确认是您本人", "본인 확인", "本人確認")
    val newPasswordAfterCheck get() = t(
        "Then choose a new password.",
        "Poi scegli una password nuova.",
        "Wähle dann ein neues Passwort.",
        "Choisissez ensuite un nouveau mot de passe.",
        "Luego elige una contraseña nueva.",
        "然后设置新密码。",
        "그다음 새 비밀번호를 정하세요.",
        "その後、新しいパスワードを設定します。"
    )
    val changePassword get() = t("Change password", "Cambia password", "Passwort ändern", "Changer le mot de passe", "Cambiar contraseña", "更改密码", "비밀번호 변경", "パスワードを変更")
    val removePassword get() = t("Remove password", "Togli la password", "Passwort entfernen", "Supprimer le mot de passe", "Quitar contraseña", "移除密码", "비밀번호 제거", "パスワードを削除")
    val removePasswordBlocked get() = t(
        "Unlock all the locked pages first.",
        "Prima sblocca tutte le pagine bloccate.",
        "Entsperre zuerst alle gesperrten Seiten.",
        "Déverrouillez d'abord toutes les pages verrouillées.",
        "Primero desbloquea todas las páginas bloqueadas.",
        "请先解锁所有锁定的页面。",
        "먼저 잠긴 페이지를 모두 잠금 해제하세요.",
        "先にロックしたページをすべて解除してください。"
    )
    val currentPassword get() = t("Current password", "Password attuale", "Aktuelles Passwort", "Mot de passe actuel", "Contraseña actual", "当前密码", "현재 비밀번호", "現在のパスワード")
    val newPassword get() = t("New password", "Password nuova", "Neues Passwort", "Nouveau mot de passe", "Contraseña nueva", "新密码", "새 비밀번호", "新しいパスワード")
    val passwordSet get() = t("Set", "Impostata", "Festgelegt", "Défini", "Configurada", "已设置", "설정됨", "設定済み")
    val passwordNotSet get() = t("Not set", "Non impostata", "Nicht festgelegt", "Non défini", "Sin configurar", "未设置", "설정 안 됨", "未設定")
    val passwordChanged get() = t("Password changed", "Password cambiata", "Passwort geändert", "Mot de passe modifié", "Contraseña cambiada", "密码已更改", "비밀번호가 변경되었습니다", "パスワードを変更しました")
    val protectedByPassword get() = t(
        "Protected by a password",
        "Protezione con password attiva",
        "Mit Passwort geschützt",
        "Protection par mot de passe active",
        "Protección con contraseña activa",
        "受密码保护",
        "비밀번호로 보호됨",
        "パスワードで保護されています"
    )
    val passwordRemoved get() = t("Password removed", "Password tolta", "Passwort entfernt", "Mot de passe supprimé", "Contraseña quitada", "密码已移除", "비밀번호가 제거되었습니다", "パスワードを削除しました")
}
