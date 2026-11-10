package com.gabriele.notionlocal.ui.i18n

/** Le scritte delle notifiche (dal 25/09/2026): canali, timer, pomodoro, date. */
object NotifyStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    // --- I canali, come li mostra Android nelle impostazioni delle notifiche ---

    val channelReminders get() = t("Reminders", "Promemoria", "Erinnerungen", "Rappels", "Recordatorios", "提醒", "리마인더", "リマインダー")
    val channelTimers get() = t("Timer and pomodoro", "Timer e pomodoro", "Timer und Pomodoro", "Minuteur et pomodoro", "Temporizador y pomodoro", "计时器和番茄钟", "타이머와 뽀모도로", "タイマーとポモドーロ")
    /** Le notifiche fisse dei timer che corrono (dal 30/09/2026): vedi `TimerNotifications`. */
    val channelRunningTimers get() = t("Running timers", "Timer in corso", "Laufende Timer", "Minuteurs en cours", "Temporizadores en marcha", "正在运行的计时器", "실행 중인 타이머", "実行中のタイマー")

    /** Il suggerimento di una riga-notifica senza testo, che mostrerà il nome della pagina. */
    val textHint get() = t("Notification (page title)", "Notifica (titolo della pagina)", "Benachrichtigung (Seitentitel)", "Notification (titre de la page)", "Notificación (título de la página)", "通知（页面标题）", "알림(페이지 제목)", "通知（ページのタイトル）")

    val tomorrow get() = t("Tomorrow", "Domani", "Morgen", "Demain", "Mañana", "明天", "내일", "明日")
    val alreadyPassed get() = t(
        "This time has already passed: the notification won't ring.",
        "Quest'ora è già passata: la notifica non suonerà.",
        "Diese Uhrzeit ist schon vorbei: Die Benachrichtigung wird nicht klingeln.",
        "Cette heure est déjà passée : la notification ne sonnera pas.",
        "Esta hora ya ha pasado: la notificación no sonará.",
        "这个时间已经过了：通知不会响。",
        "이미 지난 시간이에요. 알림이 울리지 않아요.",
        "この時刻はもう過ぎています。通知は鳴りません。"
    )

    // --- La pagina Notifications (dal 30/09/2026) ---

    val vibration get() = t("Vibration", "Vibrazione", "Vibration", "Vibration", "Vibración", "振动", "진동", "バイブレーション")
    val priority get() = t("Priority", "Priorità", "Priorität", "Priorité", "Prioridad", "优先级", "우선순위", "優先度")
    val priorityLow get() = t("Low", "Bassa", "Niedrig", "Basse", "Baja", "低", "낮음", "低")
    val priorityNormal get() = t("Normal", "Normale", "Normal", "Normale", "Normal", "普通", "보통", "標準")
    val priorityHigh get() = t("High", "Alta", "Hoch", "Haute", "Alta", "高", "높음", "高")
    val priorityLowDetail get() = t(
        "Silent, only in the notification panel",
        "In silenzio, solo nella tendina",
        "Lautlos, nur in der Benachrichtigungsleiste",
        "Silencieuse, seulement dans le volet",
        "En silencio, solo en el panel",
        "静音，只在通知栏中",
        "무음, 알림창에만",
        "サイレント、通知パネルのみ"
    )
    val priorityNormalDetail get() = t(
        "Sound and vibration, in the notification panel",
        "Suono e vibrazione, nella tendina",
        "Ton und Vibration, in der Benachrichtigungsleiste",
        "Son et vibration, dans le volet",
        "Sonido y vibración, en el panel",
        "声音和振动，在通知栏中",
        "소리와 진동, 알림창에",
        "音とバイブレーション、通知パネルに"
    )
    val priorityHighDetail get() = t(
        "Sound, vibration and a pop-up on the screen",
        "Suono, vibrazione e riquadro sullo schermo",
        "Ton, Vibration und Einblendung auf dem Bildschirm",
        "Son, vibration et bannière à l'écran",
        "Sonido, vibración y aviso en la pantalla",
        "声音、振动和屏幕弹出通知",
        "소리, 진동, 화면 팝업",
        "音、バイブレーション、画面のポップアップ"
    )
    val appIconBadge get() = t("App icon badge", "Pallino sull'icona dell'app", "App-Symbol-Badge", "Pastille sur l'icône de l'app", "Globo en el icono de la app", "应用图标角标", "앱 아이콘 배지", "アプリアイコンのバッジ")
    val appIconBadgeDetail get() = t(
        "A dot on the PageFlow icon while a notification is waiting",
        "Un pallino sull'icona di PageFlow finché c'è una notifica da leggere",
        "Ein Punkt auf dem PageFlow-Symbol, solange eine Benachrichtigung wartet",
        "Une pastille sur l'icône de PageFlow tant qu'une notification attend",
        "Un punto en el icono de PageFlow mientras haya una notificación pendiente",
        "有未读通知时，PageFlow 图标上显示圆点",
        "알림이 남아 있는 동안 PageFlow 아이콘에 점 표시",
        "通知があるあいだ PageFlow のアイコンにドットを表示"
    )
    /** I suoni di notifica del telefono (fino al 05/10/2026 "Phone sounds": ora ci sono anche le suonerie). */
    val phoneSounds get() = t(
        "Notification sounds (default and downloaded)",
        "Suoni di notifica (di serie e scaricati)",
        "Benachrichtigungstöne (vorinstalliert und heruntergeladen)",
        "Sons de notification (d'origine et téléchargés)",
        "Sonidos de notificación (de serie y descargados)",
        "通知声音（自带和下载的）",
        "알림 소리(기본 및 다운로드)",
        "通知音（標準とダウンロード）"
    )
    /** Le suonerie del telefono come suono delle notifiche (dal 05/10/2026, chiesto dall'utente). */
    val ringtones get() = t(
        "Ringtones (default and downloaded)",
        "Suonerie (di serie e scaricate)",
        "Klingeltöne (vorinstalliert und heruntergeladen)",
        "Sonneries (d'origine et téléchargées)",
        "Tonos de llamada (de serie y descargados)",
        "铃声（自带和下载的）",
        "벨소리(기본 및 다운로드)",
        "着信音（標準とダウンロード）"
    )
    /** Il titolo dell'elenco delle suonerie. */
    val ringtonesTitle get() = t("Ringtones", "Suonerie", "Klingeltöne", "Sonneries", "Tonos de llamada", "铃声", "벨소리", "着信音")
    val sendTest get() = t("Send a test notification", "Manda una notifica di prova", "Testbenachrichtigung senden", "Envoyer une notification de test", "Enviar una notificación de prueba", "发送测试通知", "테스트 알림 보내기", "テスト通知を送信")
    val testTitle get() = t("PageFlow", "PageFlow", "PageFlow", "PageFlow", "PageFlow", "PageFlow", "PageFlow", "PageFlow")
    val testText get() = t(
        "This is how your notifications look",
        "Le tue notifiche arriveranno così",
        "So sehen deine Benachrichtigungen aus",
        "Voici à quoi ressembleront vos notifications",
        "Así llegarán tus notificaciones",
        "你的通知会是这样的",
        "알림은 이렇게 표시돼요",
        "通知はこのように届きます"
    )
    val blockedByPhone get() = t(
        "Notifications for PageFlow are off in the phone settings",
        "Le notifiche di PageFlow sono spente nelle impostazioni del telefono",
        "Die Benachrichtigungen von PageFlow sind in den Telefoneinstellungen aus",
        "Les notifications de PageFlow sont désactivées dans les réglages du téléphone",
        "Las notificaciones de PageFlow están desactivadas en los ajustes del teléfono",
        "PageFlow 的通知已在手机设置中关闭",
        "휴대폰 설정에서 PageFlow 알림이 꺼져 있어요",
        "端末の設定で PageFlow の通知がオフになっています"
    )
    val openPhoneSettings get() = t("Open the phone settings", "Apri le impostazioni del telefono", "Telefoneinstellungen öffnen", "Ouvrir les réglages du téléphone", "Abrir los ajustes del teléfono", "打开手机设置", "휴대폰 설정 열기", "端末の設定を開く")
    /** Sul PC (dal 10/11/2026): quando arrivano, e da dove viene il suono. */
    val onPcNote get() = t(
        "On the computer they arrive while PageFlow is open, even minimized, as Windows notifications with Windows' own sound. Off, timers and pomodoro are silent too.",
        "Sul computer arrivano mentre PageFlow è aperta, anche ridotta a icona, come notifiche di Windows, col suono di Windows. Spente, tacciono anche timer e pomodoro.",
        "Auf dem Computer kommen sie, solange PageFlow geöffnet ist (auch minimiert), als Windows-Benachrichtigungen mit dem Windows-Ton. Ausgeschaltet schweigen auch Timer und Pomodoro.",
        "Sur l'ordinateur, elles arrivent tant que PageFlow est ouverte, même réduite, comme notifications Windows avec le son de Windows. Désactivées, minuteurs et pomodoro se taisent aussi.",
        "En el ordenador llegan mientras PageFlow está abierta, aunque esté minimizada, como notificaciones de Windows con su sonido. Desactivadas, también callan temporizadores y pomodoro.",
        "在电脑上，PageFlow 打开时（即使最小化）通知会以 Windows 通知的形式到达，并使用 Windows 的声音。关闭后，计时器和番茄钟也会静音。",
        "컴퓨터에서는 PageFlow가 열려 있는 동안(최소화해도) Windows 알림으로, Windows 소리와 함께 와요. 끄면 타이머와 뽀모도로도 조용해져요.",
        "パソコンでは PageFlow が開いている間（最小化中も）、Windows の通知として Windows の音で届きます。オフにするとタイマーとポモドーロも鳴りません。"
    )
    val unavailableOnPc get() = t(
        "Windows isn't showing notifications for PageFlow right now",
        "Windows adesso non mostra le notifiche di PageFlow",
        "Windows zeigt gerade keine Benachrichtigungen von PageFlow an",
        "Windows n'affiche pas les notifications de PageFlow pour l'instant",
        "Windows no muestra ahora las notificaciones de PageFlow",
        "Windows 目前不显示 PageFlow 的通知",
        "지금은 Windows가 PageFlow 알림을 표시하지 않아요",
        "現在 Windows は PageFlow の通知を表示していません"
    )
    val timersAlwaysRing get() = t(
        "Sound, vibration, priority and badge are for the reminders of pages and databases: timers and pomodoro always ring and pop up.",
        "Suono, vibrazione, priorità e pallino valgono per i promemoria di pagine e database: timer e pomodoro suonano e compaiono sempre.",
        "Ton, Vibration, Priorität und Badge gelten für die Erinnerungen von Seiten und Datenbanken: Timer und Pomodoro klingeln und erscheinen immer.",
        "Son, vibration, priorité et pastille valent pour les rappels des pages et des bases de données : minuteurs et pomodoro sonnent et s'affichent toujours.",
        "Sonido, vibración, prioridad y globo valen para los recordatorios de páginas y bases de datos: temporizadores y pomodoro siempre suenan y aparecen.",
        "声音、振动、优先级和角标适用于页面和数据库的提醒：计时器和番茄钟总会响铃并弹出。",
        "소리, 진동, 우선순위, 배지는 페이지와 데이터베이스의 리마인더에 적용돼요. 타이머와 뽀모도로는 항상 울리고 표시돼요.",
        "サウンド、バイブレーション、優先度、バッジはページとデータベースのリマインダーに適用されます。タイマーとポモドーロは常に鳴って表示されます。"
    )

    // --- Timer e pomodoro ---

    val timeIsUp get() = t("Time's up", "Tempo scaduto", "Die Zeit ist um", "Temps écoulé", "Se acabó el tiempo", "时间到了", "시간이 다 됐어요", "時間です")
    /** Un timer in pausa, nella sua notifica: "Paused · 4:32". */
    fun pausedAt(left: String) = t("Paused · $left", "In pausa · $left", "Pausiert · $left", "En pause · $left", "En pausa · $left", "已暂停 · $left", "일시정지됨 · $left", "一時停止中 · $left")
    fun timerOf(duration: String) = t("Timer · $duration", "Timer · $duration", "Timer · $duration", "Minuteur · $duration", "Temporizador · $duration", "计时器 · $duration", "타이머 · $duration", "タイマー · $duration")
    fun breakTime(minutes: Int) = t(
        "Time for a break · $minutes min",
        "È ora della pausa · $minutes min",
        "Zeit für eine Pause · $minutes Min.",
        "C'est l'heure de la pause · $minutes min",
        "Hora del descanso · $minutes min",
        "该休息了 · $minutes 分钟",
        "휴식 시간 · ${minutes}분",
        "休憩の時間 · ${minutes}分"
    )
    val breakOver get() = t("Break's over", "Pausa finita", "Pause vorbei", "Pause terminée", "Se acabó el descanso", "休息结束", "휴식 끝", "休憩終了")
    fun backTo(session: String, minutes: Int) = t(
        "Back to $session · $minutes min",
        "Si torna a $session · $minutes min",
        "Zurück zu $session · $minutes Min.",
        "Retour à $session · $minutes min",
        "De vuelta a $session · $minutes min",
        "回到$session · $minutes 分钟",
        "$session(으)로 돌아가기 · ${minutes}분",
        "$session に戻る · ${minutes}分"
    )

    // --- Le date dei database con inizio e fine ---

    val starts get() = t("Starts", "Inizio", "Beginn", "Début", "Inicio", "开始", "시작", "開始")
    val ends get() = t("Ends", "Fine", "Ende", "Fin", "Fin", "结束", "종료", "終了")
}
