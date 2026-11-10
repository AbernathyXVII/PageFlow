package com.gabriele.notionlocal.ui.i18n

/** Le scritte di "Page history", le versioni delle pagine (dal 02/11/2026). */
object HistoryStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    val pageHistory get() = t("Page history", "Cronologia della pagina", "Seitenverlauf", "Historique de la page", "Historial de la página", "页面历史", "페이지 기록", "ページの履歴")

    val note get() = t(
        "PageFlow saves a version of this page when you open it, every 10 minutes while it's open and when you leave it, if something changed. All versions from the last 24 hours stay; older ones, one per day for 30 days.",
        "PageFlow salva una versione di questa pagina quando la apri, ogni 10 minuti mentre è aperta e quando la lasci, se è cambiato qualcosa. Le versioni delle ultime 24 ore restano tutte; quelle più vecchie, una al giorno per 30 giorni.",
        "PageFlow speichert eine Version dieser Seite, wenn du sie öffnest, alle 10 Minuten, solange sie offen ist, und wenn du sie verlässt – falls sich etwas geändert hat. Alle Versionen der letzten 24 Stunden bleiben; ältere eine pro Tag, 30 Tage lang.",
        "PageFlow enregistre une version de cette page quand tu l'ouvres, toutes les 10 minutes tant qu'elle est ouverte et quand tu la quittes, si quelque chose a changé. Toutes les versions des dernières 24 heures restent ; les plus anciennes, une par jour pendant 30 jours.",
        "PageFlow guarda una versión de esta página al abrirla, cada 10 minutos mientras está abierta y al salir, si algo cambió. Todas las versiones de las últimas 24 horas se quedan; las más antiguas, una por día durante 30 días.",
        "如果有变化，PageFlow 会在你打开此页面时、页面打开期间每 10 分钟、以及离开时保存一个版本。最近 24 小时的版本全部保留；更早的每天保留一个，保留 30 天。",
        "PageFlow는 변경된 내용이 있으면 이 페이지를 열 때, 열려 있는 동안 10분마다, 그리고 나갈 때 버전을 저장합니다. 최근 24시간의 버전은 모두 남고, 그보다 오래된 버전은 30일 동안 하루에 하나씩 남습니다.",
        "PageFlow は変更があれば、このページを開いたとき、開いている間は 10 分ごと、離れるときにバージョンを保存します。直近 24 時間のバージョンはすべて残り、それより古いものは 1 日 1 つ、30 日間残ります。"
    )

    val noVersions get() = t(
        "No versions yet: the first one is saved as soon as the page has something in it.",
        "Ancora nessuna versione: la prima si salva appena la pagina ha qualcosa dentro.",
        "Noch keine Versionen: Die erste wird gespeichert, sobald die Seite Inhalt hat.",
        "Aucune version pour l'instant : la première est enregistrée dès que la page contient quelque chose.",
        "Aún no hay versiones: la primera se guarda en cuanto la página tenga algo.",
        "还没有版本：页面一有内容就会保存第一个版本。",
        "아직 버전이 없습니다. 페이지에 내용이 생기면 첫 버전이 저장됩니다.",
        "まだバージョンはありません。ページに内容ができるとすぐに最初のバージョンが保存されます。"
    )

    val today get() = t("Today", "Oggi", "Heute", "Aujourd'hui", "Hoy", "今天", "오늘", "今日")
    val yesterday get() = t("Yesterday", "Ieri", "Gestern", "Hier", "Ayer", "昨天", "어제", "昨日")

    fun linesAndWords(lines: Int, words: Int) = t(
        "$lines ${if (lines == 1) "line" else "lines"} · $words ${if (words == 1) "word" else "words"}",
        "$lines ${if (lines == 1) "riga" else "righe"} · $words ${if (words == 1) "parola" else "parole"}",
        "$lines ${if (lines == 1) "Zeile" else "Zeilen"} · $words ${if (words == 1) "Wort" else "Wörter"}",
        "$lines ${if (lines == 1) "ligne" else "lignes"} · $words ${if (words == 1) "mot" else "mots"}",
        "$lines ${if (lines == 1) "línea" else "líneas"} · $words ${if (words == 1) "palabra" else "palabras"}",
        "$lines 行 · $words 词",
        "$lines 줄 · $words 단어",
        "$lines 行 · $words 語"
    )

    val beforeSyncing get() = t("Before syncing", "Prima della sincronizzazione", "Vor dem Synchronisieren", "Avant la synchronisation", "Antes de sincronizar", "同步之前", "동기화하기 전", "同期する前")
    val beforeRestoring get() = t("Before restoring", "Prima del ripristino", "Vor dem Wiederherstellen", "Avant la restauration", "Antes de restaurar", "恢复之前", "복원하기 전", "復元する前")

    val restoreThisVersion get() = t("Restore this version", "Ripristina questa versione", "Diese Version wiederherstellen", "Restaurer cette version", "Restaurar esta versión", "恢复此版本", "이 버전 복원", "このバージョンを復元")

    val restoreTitle get() = t("Restore this version?", "Ripristinare questa versione?", "Diese Version wiederherstellen?", "Restaurer cette version ?", "¿Restaurar esta versión?", "恢复此版本？", "이 버전을 복원할까요?", "このバージョンを復元しますか？")

    fun restoreText(moment: String) = t(
        "The page goes back to how it was on $moment. How it is now is saved in the history first, so you can come back to it.",
        "La pagina torna com'era il $moment. Com'è adesso si salva prima nella cronologia, così ci puoi tornare.",
        "Die Seite wird so, wie sie am $moment war. Der jetzige Stand wird vorher im Verlauf gespeichert, du kannst also zurück.",
        "La page redevient telle qu'elle était le $moment. Son état actuel est d'abord enregistré dans l'historique, pour pouvoir y revenir.",
        "La página vuelve a como estaba el $moment. Cómo está ahora se guarda antes en el historial, para que puedas volver.",
        "页面将恢复到 $moment 时的样子。当前内容会先保存到历史中，你可以随时回来。",
        "페이지가 $moment 상태로 돌아갑니다. 지금 상태는 먼저 기록에 저장되므로 다시 돌아올 수 있습니다.",
        "ページは $moment の状態に戻ります。今の状態は先に履歴に保存されるので、戻ることができます。"
    )

    val restoreNote get() = t(
        "Comes back: title, text, tables and drawings. Icon, cover and what's inside databases and subpages stay as they are now; subpages created later keep their link, at the end of the page.",
        "Tornano: titolo, testo, tabelle e disegni. Icona, copertina e quello che c'è dentro database e sottopagine restano come adesso; le sottopagine nate dopo tengono il loro collegamento, in fondo alla pagina.",
        "Zurück kommen: Titel, Text, Tabellen und Zeichnungen. Symbol, Titelbild und der Inhalt von Datenbanken und Unterseiten bleiben wie jetzt; später erstellte Unterseiten behalten ihren Link am Ende der Seite.",
        "Reviennent : titre, texte, tableaux et dessins. L'icône, la couverture et le contenu des bases de données et sous-pages restent comme maintenant ; les sous-pages créées après gardent leur lien, en bas de la page.",
        "Vuelven: título, texto, tablas y dibujos. El icono, la portada y lo que hay dentro de bases de datos y subpáginas se quedan como ahora; las subpáginas creadas después conservan su enlace, al final de la página.",
        "会恢复：标题、文字、表格和手绘。图标、封面以及数据库和子页面里的内容保持现状；之后创建的子页面会保留链接，放在页面末尾。",
        "복원되는 것: 제목, 텍스트, 표, 그림. 아이콘, 커버, 데이터베이스와 하위 페이지 안의 내용은 지금 그대로이며, 나중에 만든 하위 페이지는 링크가 페이지 끝에 남습니다.",
        "戻るもの：タイトル、テキスト、表、手書き。アイコン、カバー、データベースやサブページの中身は今のままで、後から作ったサブページはリンクがページの最後に残ります。"
    )

    val lockedNote get() = t(
        "The page is locked: unlock it to restore a version.",
        "La pagina è bloccata: sbloccala per ripristinare una versione.",
        "Die Seite ist gesperrt: Entsperre sie, um eine Version wiederherzustellen.",
        "La page est verrouillée : déverrouille-la pour restaurer une version.",
        "La página está bloqueada: desbloquéala para restaurar una versión.",
        "页面已锁定：解锁后才能恢复版本。",
        "페이지가 잠겨 있습니다. 버전을 복원하려면 잠금을 해제하세요.",
        "ページはロックされています。バージョンを復元するにはロックを解除してください。"
    )

    val unreadable get() = t(
        "This version can't be read.",
        "Questa versione non si riesce a leggere.",
        "Diese Version kann nicht gelesen werden.",
        "Cette version ne peut pas être lue.",
        "Esta versión no se puede leer.",
        "无法读取此版本。",
        "이 버전을 읽을 수 없습니다.",
        "このバージョンは読み込めません。"
    )

    val restored get() = t("Version restored", "Versione ripristinata", "Version wiederhergestellt", "Version restaurée", "Versión restaurada", "已恢复版本", "버전이 복원되었습니다", "バージョンを復元しました")
}
