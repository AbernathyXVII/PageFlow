package com.gabriele.notionlocal.ui.i18n

/**
 * Le scritte della sincronizzazione Wi‑Fi (dal 10/11/2026; `SyncStrings` sono quelle dei blocchi sincronizzati), nella pagina
 * "Connections" e nella domanda del codice. Stessa regola di `Strings`: ogni
 * voce con le sue otto traduzioni.
 */
object WifiSyncStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    // --- Il telefono ---
    val syncWithPc get() = t("Sync with the PC", "Sincronizza col PC", "Mit dem PC synchronisieren", "Synchroniser avec le PC", "Sincronizar con el PC", "与电脑同步", "PC와 동기화", "PCと同期")
    val lastSync get() = t("Last sync", "Ultima sincronizzazione", "Letzte Synchronisierung", "Dernière synchronisation", "Última sincronización", "上次同步", "마지막 동기화", "前回の同期")
    val never get() = t("Never", "Mai", "Nie", "Jamais", "Nunca", "从未", "없음", "なし")
    fun pairedWith(name: String) = t("Paired with $name", "Abbinato a $name", "Gekoppelt mit $name", "Associé à $name", "Vinculado con $name", "已与 $name 配对", "$name 와(과) 연결됨", "$name とペアリング済み")
    val forgetPc get() = t("Forget this PC", "Dimentica il PC", "Diesen PC vergessen", "Oublier ce PC", "Olvidar este PC", "忘记此电脑", "이 PC 삭제", "このPCを削除")
    val searching get() = t("Looking for the PC on the home network…", "Cerco il PC sulla rete di casa…", "Suche den PC im Heimnetz…", "Recherche du PC sur le réseau domestique…", "Buscando el PC en la red de casa…", "正在家庭网络上查找电脑…", "집 네트워크에서 PC를 찾는 중…", "家のネットワークでPCを探しています…")
    fun connecting(name: String) = t("Connecting to $name…", "Mi collego a $name…", "Verbinde mit $name…", "Connexion à $name…", "Conectando con $name…", "正在连接 $name…", "$name 에 연결 중…", "$name に接続中…")
    val pairing get() = t("Pairing: check the code on both screens…", "Abbinamento: controlla il codice sui due schermi…", "Koppeln: Code auf beiden Bildschirmen prüfen…", "Association : vérifiez le code sur les deux écrans…", "Vinculando: comprueba el código en las dos pantallas…", "配对中：请核对两个屏幕上的代码…", "연결 중: 두 화면의 코드를 확인하세요…", "ペアリング中：両方の画面のコードを確認してください…")
    val backingUp get() = t("Making a backup first…", "Prima faccio un backup…", "Zuerst eine Sicherung…", "D'abord une sauvegarde…", "Primero una copia de seguridad…", "先进行备份…", "먼저 백업하는 중…", "まずバックアップしています…")
    val comparing get() = t("Comparing the pages…", "Confronto le pagine…", "Vergleiche die Seiten…", "Comparaison des pages…", "Comparando las páginas…", "正在比较页面…", "페이지 비교 중…", "ページを比較しています…")
    val files get() = t("Exchanging files…", "Scambio i file…", "Tausche Dateien aus…", "Échange des fichiers…", "Intercambiando archivos…", "正在交换文件…", "파일 교환 중…", "ファイルをやり取りしています…")
    val pages get() = t("Exchanging pages…", "Scambio le pagine…", "Tausche Seiten aus…", "Échange des pages…", "Intercambiando páginas…", "正在交换页面…", "페이지 교환 중…", "ページをやり取りしています…")

    fun done(name: String, sent: Int, received: Int, trashed: Int, conflicts: Int, files: Int): String {
        val base = t(
            "Synced with $name: $sent sent, $received received.",
            "Sincronizzato con $name: $sent mandati, $received arrivati.",
            "Mit $name synchronisiert: $sent gesendet, $received empfangen.",
            "Synchronisé avec $name : $sent envoyés, $received reçus.",
            "Sincronizado con $name: $sent enviados, $received recibidos.",
            "已与 $name 同步：发送 $sent 项，接收 $received 项。",
            "$name 와(과) 동기화됨: $sent 개 보냄, $received 개 받음.",
            "$name と同期しました：送信 $sent、受信 $received。"
        )
        val extra = buildList {
            if (trashed > 0) add(t("$trashed to the trash", "$trashed nel cestino", "$trashed in den Papierkorb", "$trashed à la corbeille", "$trashed a la papelera", "$trashed 项移入回收站", "$trashed 개 휴지통으로", "$trashed 件をゴミ箱へ"))
            if (conflicts > 0) {
                add(
                    t(
                        "$conflicts changed on both sides (the other version is in the page history)",
                        "$conflicts cambiati da tutte e due le parti (l'altra versione è nella cronologia della pagina)",
                        "$conflicts auf beiden Seiten geändert (die andere Version ist im Seitenverlauf)",
                        "$conflicts modifiés des deux côtés (l'autre version est dans l'historique de la page)",
                        "$conflicts cambiados en los dos lados (la otra versión está en el historial de la página)",
                        "$conflicts 项两边都改过（另一版本在页面历史中）",
                        "$conflicts 개가 양쪽에서 바뀜 (다른 버전은 페이지 기록에 있음)",
                        "$conflicts 件が両方で変更（もう一方は履歴にあります）"
                    )
                )
            }
            if (files > 0) add(t("$files files", "$files file", "$files Dateien", "$files fichiers", "$files archivos", "$files 个文件", "파일 $files 개", "ファイル $files 件"))
        }
        return if (extra.isEmpty()) base else base + " " + extra.joinToString(", ") + "."
    }

    val notFound get() = t(
        "PC not found. On the PC, open PageFlow → Connections and turn on \"Wait for the phone\"; the phone and the PC must be on the same home network, and Windows must let PageFlow use it.",
        "PC non trovato. Sul PC apri PageFlow → Connections e accendi \"Aspetta il telefono\"; telefono e PC devono essere sulla stessa rete di casa, e Windows deve lasciarla usare a PageFlow.",
        "PC nicht gefunden. Öffne am PC PageFlow → Connections und schalte \"Auf das Telefon warten\" ein; Telefon und PC müssen im selben Heimnetz sein, und Windows muss PageFlow das Netzwerk erlauben.",
        "PC introuvable. Sur le PC, ouvrez PageFlow → Connections et activez « Attendre le téléphone » ; le téléphone et le PC doivent être sur le même réseau domestique, et Windows doit autoriser PageFlow à l'utiliser.",
        "PC no encontrado. En el PC, abre PageFlow → Connections y activa \"Esperar al teléfono\"; el teléfono y el PC deben estar en la misma red de casa, y Windows debe dejar que PageFlow la use.",
        "未找到电脑。请在电脑上打开 PageFlow → Connections 并开启“等待手机”；手机和电脑需在同一家庭网络，且 Windows 需允许 PageFlow 使用网络。",
        "PC를 찾을 수 없습니다. PC에서 PageFlow → Connections를 열고 \"휴대폰 기다리기\"를 켜세요. 휴대폰과 PC는 같은 집 네트워크에 있어야 하고, Windows가 PageFlow의 네트워크 사용을 허용해야 합니다.",
        "PCが見つかりません。PCで PageFlow → Connections を開き「スマホを待つ」をオンにしてください。スマホとPCは同じ家のネットワークにあり、WindowsがPageFlowのネットワーク使用を許可している必要があります。"
    )
    val differentVersion get() = t(
        "The phone and the PC have different versions of PageFlow: update the older one.",
        "Il telefono e il PC hanno versioni diverse di PageFlow: aggiorna quella più vecchia.",
        "Telefon und PC haben verschiedene PageFlow-Versionen: aktualisiere die ältere.",
        "Le téléphone et le PC ont des versions différentes de PageFlow : mettez à jour la plus ancienne.",
        "El teléfono y el PC tienen versiones distintas de PageFlow: actualiza la más antigua.",
        "手机和电脑的 PageFlow 版本不同：请更新较旧的那个。",
        "휴대폰과 PC의 PageFlow 버전이 다릅니다. 오래된 쪽을 업데이트하세요.",
        "スマホとPCのPageFlowのバージョンが違います。古い方を更新してください。"
    )
    val refused get() = t("Pairing canceled.", "Abbinamento annullato.", "Koppeln abgebrochen.", "Association annulée.", "Vinculación cancelada.", "已取消配对。", "연결이 취소되었습니다.", "ペアリングをキャンセルしました。")
    val pairingLost get() = t(
        "The PC doesn't recognize this phone anymore: try again to pair them.",
        "Il PC non riconosce più questo telefono: riprova per abbinarli di nuovo.",
        "Der PC erkennt dieses Telefon nicht mehr: versuche es erneut, um sie zu koppeln.",
        "Le PC ne reconnaît plus ce téléphone : réessayez pour les associer.",
        "El PC ya no reconoce este teléfono: inténtalo de nuevo para vincularlos.",
        "电脑不再识别此手机：请重试以重新配对。",
        "PC가 이 휴대폰을 더 이상 인식하지 않습니다. 다시 시도해 연결하세요.",
        "PCがこのスマホを認識しなくなりました。もう一度試してペアリングしてください。"
    )
    val network get() = t("The connection broke: try again.", "La connessione si è interrotta: riprova.", "Die Verbindung wurde unterbrochen: versuche es erneut.", "La connexion a été interrompue : réessayez.", "La conexión se cortó: inténtalo de nuevo.", "连接中断：请重试。", "연결이 끊어졌습니다. 다시 시도하세요.", "接続が切れました。もう一度試してください。")
    val whatSyncs get() = t(
        "Pages, databases, notebooks, files and page history go both ways. A page changed on both sides is merged line by line; only a line changed on both keeps the newest change, and the other one stays in the page history. Settings and widgets stay on each device. The first time, both make a backup.",
        "Pagine, database, quaderni, file e cronologia vanno nei due sensi. Una pagina cambiata da tutte e due le parti si unisce riga per riga; solo una riga cambiata da tutte e due tiene l'ultima modifica, e l'altra resta nella cronologia della pagina. Impostazioni e widget restano di ogni dispositivo. La prima volta tutti e due fanno un backup.",
        "Seiten, Datenbanken, Notizbücher, Dateien und Verlauf gehen in beide Richtungen. Eine auf beiden Seiten geänderte Seite wird Zeile für Zeile zusammengeführt; nur bei einer auf beiden Seiten geänderten Zeile gewinnt die neueste Änderung, die andere bleibt im Seitenverlauf. Einstellungen und Widgets bleiben auf jedem Gerät. Beim ersten Mal machen beide eine Sicherung.",
        "Pages, bases de données, carnets, fichiers et historique vont dans les deux sens. Une page modifiée des deux côtés est fusionnée ligne par ligne ; seule une ligne modifiée des deux côtés garde la modification la plus récente, l'autre reste dans l'historique de la page. Les réglages et widgets restent sur chaque appareil. La première fois, les deux font une sauvegarde.",
        "Páginas, bases de datos, cuadernos, archivos e historial van en los dos sentidos. Una página cambiada en los dos lados se une línea por línea; solo una línea cambiada en los dos conserva el cambio más reciente, y la otra queda en el historial de la página. Los ajustes y widgets se quedan en cada dispositivo. La primera vez, los dos hacen una copia de seguridad.",
        "页面、数据库、笔记本、文件和历史双向同步。两边都改过的页面会逐行合并；只有两边都改过的同一行以最新修改为准，另一版本保留在页面历史中。设置和小组件留在各自设备上。第一次同步时两边都会备份。",
        "페이지, 데이터베이스, 노트북, 파일, 기록이 양방향으로 동기화됩니다. 양쪽에서 바뀐 페이지는 줄 단위로 합쳐지고, 양쪽에서 바뀐 같은 줄만 최신 변경이 우선하며 다른 버전은 페이지 기록에 남습니다. 설정과 위젯은 각 기기에 남습니다. 처음에는 양쪽 모두 백업합니다.",
        "ページ、データベース、ノート、ファイル、履歴が双方向に同期されます。両方で変更されたページは行ごとにまとめられ、両方で変更された同じ行だけ新しい変更が優先され、もう一方はページ履歴に残ります。設定とウィジェットは各端末に残ります。初回は両方でバックアップします。"
    )

    // --- Il PC ---
    val waitForPhone get() = t("Wait for the phone", "Aspetta il telefono", "Auf das Telefon warten", "Attendre le téléphone", "Esperar al teléfono", "等待手机", "휴대폰 기다리기", "スマホを待つ")
    fun waitingOn(addresses: String) = t("Waiting on the network ($addresses)", "In attesa sulla rete ($addresses)", "Wartet im Netzwerk ($addresses)", "En attente sur le réseau ($addresses)", "Esperando en la red ($addresses)", "正在网络上等待（$addresses）", "네트워크에서 기다리는 중 ($addresses)", "ネットワークで待機中（$addresses）")
    val off get() = t("Off", "Spento", "Aus", "Désactivé", "Apagado", "已关闭", "꺼짐", "オフ")
    val portBusy get() = t(
        "The network port is busy: is another PageFlow open?",
        "La porta di rete è occupata: c'è un altro PageFlow aperto?",
        "Der Netzwerkport ist belegt: ist ein anderes PageFlow geöffnet?",
        "Le port réseau est occupé : un autre PageFlow est-il ouvert ?",
        "El puerto de red está ocupado: ¿hay otro PageFlow abierto?",
        "网络端口被占用：是否打开了另一个 PageFlow？",
        "네트워크 포트가 사용 중입니다. 다른 PageFlow가 열려 있나요?",
        "ネットワークポートが使用中です。別のPageFlowが開いていませんか？"
    )
    val pcNote get() = t(
        "On the phone: Connections → \"Sync with the PC\", on the same home network (the PC can be on a cable). The first time, Windows asks whether PageFlow may use the network: choose Allow, ticking the kind of network you are on (at home Windows may call it public).",
        "Sul telefono: Connections → \"Sincronizza col PC\", sulla stessa rete di casa (il PC può essere col cavo). La prima volta Windows chiede se PageFlow può usare la rete: scegli Consenti, spuntando il tipo di rete in cui sei (a casa Windows può chiamarla pubblica).",
        "Am Telefon: Connections → \"Mit dem PC synchronisieren\", im selben Heimnetz (der PC darf per Kabel verbunden sein). Beim ersten Mal fragt Windows, ob PageFlow das Netzwerk nutzen darf: wähle Zulassen und hake die Art deines Netzwerks an (zu Hause nennt Windows es eventuell öffentlich).",
        "Sur le téléphone : Connections → « Synchroniser avec le PC », sur le même réseau domestique (le PC peut être en câble). La première fois, Windows demande si PageFlow peut utiliser le réseau : choisissez Autoriser en cochant le type de réseau où vous êtes (à la maison, Windows peut l'appeler public).",
        "En el teléfono: Connections → \"Sincronizar con el PC\", en la misma red de casa (el PC puede ir por cable). La primera vez Windows pregunta si PageFlow puede usar la red: elige Permitir marcando el tipo de red en la que estás (en casa Windows puede llamarla pública).",
        "在手机上：Connections →“与电脑同步”，连接同一家庭网络（电脑可以用网线）。第一次 Windows 会询问是否允许 PageFlow 使用网络：选择“允许”，并勾选你所在的网络类型（在家里 Windows 可能称其为公用网络）。",
        "휴대폰에서: Connections → \"PC와 동기화\", 같은 집 네트워크에서 (PC는 유선이어도 됩니다). 처음에는 Windows가 PageFlow의 네트워크 사용을 묻습니다. 허용을 선택하고 현재 네트워크 종류를 체크하세요 (집에서도 Windows가 공용이라고 부를 수 있습니다).",
        "スマホで：Connections →「PCと同期」、同じ家のネットワークで（PCは有線でも大丈夫）。初回はWindowsがPageFlowのネットワーク使用を尋ねます：「許可」を選び、今いるネットワークの種類にチェックを入れてください（自宅でもWindowsはパブリックと呼ぶことがあります）。"
    )
    val pairedPhones get() = t("Paired phones", "Telefoni abbinati", "Gekoppelte Telefone", "Téléphones associés", "Teléfonos vinculados", "已配对的手机", "연결된 휴대폰", "ペアリング済みのスマホ")
    val noPhones get() = t("No phone yet", "Ancora nessun telefono", "Noch kein Telefon", "Aucun téléphone pour l'instant", "Ningún teléfono todavía", "还没有手机", "아직 휴대폰 없음", "まだスマホはありません")
    val remove get() = t("Remove", "Rimuovi", "Entfernen", "Retirer", "Quitar", "移除", "삭제", "削除")
    fun syncedWith(name: String) = t("Synced with $name", "Sincronizzato con $name", "Mit $name synchronisiert", "Synchronisé avec $name", "Sincronizado con $name", "已与 $name 同步", "$name 와(과) 동기화됨", "$name と同期しました")

    // --- La domanda del codice ---
    fun pairTitle(name: String) = t("Pair with $name?", "Abbinare $name?", "Mit $name koppeln?", "Associer $name ?", "¿Vincular con $name?", "与 $name 配对？", "$name 와(과) 연결할까요?", "$name とペアリングしますか？")
    val pairText get() = t(
        "Check that the other screen shows the same code:",
        "Controlla che sull'altro schermo ci sia lo stesso codice:",
        "Prüfe, ob der andere Bildschirm denselben Code zeigt:",
        "Vérifiez que l'autre écran affiche le même code :",
        "Comprueba que la otra pantalla muestra el mismo código:",
        "请确认另一个屏幕显示相同的代码：",
        "다른 화면에 같은 코드가 표시되는지 확인하세요:",
        "もう一方の画面に同じコードが表示されているか確認してください："
    )
    val pair get() = t("Pair", "Abbina", "Koppeln", "Associer", "Vincular", "配对", "연결", "ペアリング")
}
