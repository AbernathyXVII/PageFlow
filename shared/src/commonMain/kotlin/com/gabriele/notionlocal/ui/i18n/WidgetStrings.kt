package com.gabriele.notionlocal.ui.i18n

/**
 * Le voci dei widget della barra laterale. Stessa regola di `Strings`:
 * ogni voce con le sue otto traduzioni, e solo testo dell'app.
 */
object WidgetStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    // --- La sezione ---

    val hideWidgets get() = t("Hide widgets", "Nascondi i widget", "Widgets ausblenden", "Masquer les widgets", "Ocultar widgets", "隐藏小组件", "위젯 숨기기", "ウィジェットを隠す")
    val showWidgets get() = t("Show widgets", "Mostra i widget", "Widgets anzeigen", "Afficher les widgets", "Mostrar widgets", "显示小组件", "위젯 보기", "ウィジェットを表示")
    val addWidget get() = t("Add widget", "Aggiungi widget", "Widget hinzufügen", "Ajouter un widget", "Añadir widget", "添加小组件", "위젯 추가", "ウィジェットを追加")
    val showDecimals get() = t("Show decimals", "Mostra i decimali", "Dezimalstellen zeigen", "Afficher les décimales", "Mostrar decimales", "显示小数", "소수점 표시", "小数を表示")
    val widgetOptions get() = t("Widget options", "Opzioni del widget", "Widget-Optionen", "Options du widget", "Opciones del widget", "小组件选项", "위젯 옵션", "ウィジェットのオプション")
    val moveUp get() = t("Move up", "Sposta su", "Nach oben", "Monter", "Subir", "上移", "위로 이동", "上へ移動")
    val moveDown get() = t("Move down", "Sposta giù", "Nach unten", "Descendre", "Bajar", "下移", "아래로 이동", "下へ移動")
    val remove get() = t("Remove", "Rimuovi", "Entfernen", "Retirer", "Quitar", "移除", "제거", "削除")
    val removeWidgetTitle get() = t("Remove this widget?", "Rimuovere questo widget?", "Dieses Widget entfernen?", "Retirer ce widget ?", "¿Quitar este widget?", "移除此小组件？", "이 위젯을 제거할까요?", "このウィジェットを削除しますか？")
    val removeWidgetText get() = t(
        "What it contains goes away with it.",
        "Se ne va con quello che contiene.",
        "Sein Inhalt geht mit.",
        "Son contenu part avec lui.",
        "Se va con lo que contiene.",
        "其中的内容会一起删除。",
        "안에 있는 내용도 함께 사라집니다.",
        "中身も一緒に消えます。"
    )

    fun kindName(kind: com.gabriele.notionlocal.data.widgets.WidgetKind): String = when (kind) {
        com.gabriele.notionlocal.data.widgets.WidgetKind.TIME_ZONES -> timeZones
        com.gabriele.notionlocal.data.widgets.WidgetKind.LIFE_PROGRESS -> lifeProgress
        com.gabriele.notionlocal.data.widgets.WidgetKind.COUNTER -> counter
        com.gabriele.notionlocal.data.widgets.WidgetKind.POMODORO -> pomodoro
        com.gabriele.notionlocal.data.widgets.WidgetKind.STOPWATCH -> stopwatch
        com.gabriele.notionlocal.data.widgets.WidgetKind.TIMER -> timer
        com.gabriele.notionlocal.data.widgets.WidgetKind.WEATHER -> weather
        com.gabriele.notionlocal.data.widgets.WidgetKind.SPORTS -> sports
        com.gabriele.notionlocal.data.widgets.WidgetKind.STOCKS -> stocks
        com.gabriele.notionlocal.data.widgets.WidgetKind.COUNTDOWN -> countdown
    }

    // --- Countdown (dal 27/10/2026), vedi `Countdown.kt` ---

    val countdown get() = t("Countdown", "Conto alla rovescia", "Countdown", "Compte à rebours", "Cuenta atrás", "倒计时", "카운트다운", "カウントダウン")
    val addCountdown get() = t("Add countdown", "Aggiungi un conto alla rovescia", "Countdown hinzufügen", "Ajouter un compte à rebours", "Añadir cuenta atrás", "添加倒计时", "카운트다운 추가", "カウントダウンを追加")
    val editCountdown get() = t("Edit countdown", "Modifica il conto alla rovescia", "Countdown bearbeiten", "Modifier le compte à rebours", "Editar cuenta atrás", "编辑倒计时", "카운트다운 편집", "カウントダウンを編集")
    val christmas get() = t("Christmas", "Natale", "Weihnachten", "Noël", "Navidad", "圣诞节", "크리스마스", "クリスマス")
    val countdownMonths get() = t("Months", "Mesi", "Monate", "Mois", "Meses", "月", "개월", "か月")
    val countdownWeeks get() = t("Weeks", "Settimane", "Wochen", "Semaines", "Semanas", "周", "주", "週")
    val countdownDays get() = t("Days", "Giorni", "Tage", "Jours", "Días", "天", "일", "日")
    val countdownHours get() = t("Hours", "Ore", "Stunden", "Heures", "Horas", "时", "시간", "時間")
    val countdownMinutes get() = t("Minutes", "Minuti", "Minuten", "Minutes", "Minutos", "分", "분", "分")
    val countdownSeconds get() = t("Seconds", "Secondi", "Sekunden", "Secondes", "Segundos", "秒", "초", "秒")
    val fieldYear get() = t("Year", "Anno", "Jahr", "Année", "Año", "年", "년", "年")
    val fieldMonth get() = t("Month", "Mese", "Monat", "Mois", "Mes", "月", "월", "月")
    val fieldDay get() = t("Day", "Giorno", "Tag", "Jour", "Día", "日", "일", "日")
    val fieldHour get() = t("Hour", "Ora", "Stunde", "Heure", "Hora", "时", "시", "時")
    val fieldMinute get() = t("Minute", "Minuto", "Minute", "Minute", "Minuto", "分", "분", "分")
    val fieldSecond get() = t("Second", "Secondo", "Sekunde", "Seconde", "Segundo", "秒", "초", "秒")
    val countdownPastNote get() = t(
        "A date in the past counts the time since then",
        "Una data passata conta il tempo trascorso da allora",
        "Ein vergangenes Datum zählt die Zeit seitdem",
        "Une date passée compte le temps écoulé depuis",
        "Una fecha pasada cuenta el tiempo transcurrido desde entonces",
        "过去的日期会计算从那时起经过的时间",
        "지난 날짜는 그때부터 지난 시간을 세요",
        "過去の日付はそれから経った時間を数えます"
    )
    val noSuchDate get() = t("This date doesn't exist", "Questa data non esiste", "Dieses Datum gibt es nicht", "Cette date n'existe pas", "Esta fecha no existe", "此日期不存在", "존재하지 않는 날짜예요", "この日付は存在しません")
    val stocks get() = t("Stocks", "Borsa", "Börse", "Bourse", "Bolsa", "股票", "주식", "株式")

    // --- Borsa (dal 17/10/2026) ---

    val addStock get() = t("Add a stock", "Aggiungi un titolo", "Wertpapier hinzufügen", "Ajouter un titre", "Añadir un valor", "添加股票", "종목 추가", "銘柄を追加")
    val stockList get() = t("Stocks", "Titoli", "Wertpapiere", "Titres", "Valores", "股票", "종목", "銘柄")
    val stockListNote get() = t(
        "In the order you choose. Prices can be up to 15 minutes late.",
        "Nell'ordine che scegli. I prezzi possono arrivare fino a 15 minuti in ritardo.",
        "In deiner Reihenfolge. Kurse können bis zu 15 Minuten verzögert sein.",
        "Dans l'ordre que vous choisissez. Les cours peuvent avoir jusqu'à 15 minutes de retard.",
        "En el orden que elijas. Los precios pueden llegar hasta 15 minutos tarde.",
        "按你选择的顺序。价格可能延迟最多 15 分钟。",
        "원하는 순서대로. 가격은 최대 15분 늦을 수 있어요.",
        "好きな順番で。価格は最大15分遅れることがあります。"
    )
    val searchStock get() = t("Search by name or symbol", "Cerca per nome o sigla", "Nach Name oder Kürzel suchen", "Rechercher par nom ou symbole", "Buscar por nombre o símbolo", "按名称或代码搜索", "이름 또는 기호로 검색", "名前またはシンボルで検索")
    val noStockFound get() = t("Nothing found", "Niente trovato", "Nichts gefunden", "Rien trouvé", "Nada encontrado", "未找到", "찾을 수 없어요", "見つかりません")
    val removeStock get() = t("Remove", "Togli", "Entfernen", "Retirer", "Quitar", "移除", "제거", "削除")
    val previousClose get() = t("Previous close", "Chiusura precedente", "Vortagesschluss", "Clôture précédente", "Cierre anterior", "前收盘价", "전일 종가", "前日終値")
    val dayRange get() = t("Day range", "Minimo e massimo di oggi", "Tagesspanne", "Fourchette du jour", "Rango del día", "日内区间", "일일 범위", "1日の値幅")
    val yearRange get() = t("52-week range", "Minimo e massimo di un anno", "52-Wochen-Spanne", "Fourchette sur 52 semaines", "Rango de 52 semanas", "52 周区间", "52주 범위", "52週の値幅")
    val lastTrade get() = t("Last trade", "Ultimo scambio", "Letzter Handel", "Dernière transaction", "Última operación", "最后交易", "마지막 거래", "最終取引")
    val stocksSource get() = t("Data: Yahoo Finance · may be delayed", "Dati: Yahoo Finance · possono essere in ritardo", "Daten: Yahoo Finance · ggf. verzögert", "Données : Yahoo Finance · peuvent être différées", "Datos: Yahoo Finance · pueden llegar tarde", "数据：Yahoo Finance · 可能延迟", "데이터: Yahoo Finance · 지연될 수 있음", "データ：Yahoo Finance · 遅延の可能性あり")
    val sports get() = t("Sports", "Sport", "Sport", "Sports", "Deportes", "体育", "스포츠", "スポーツ")

    // --- Sport (dal 17/10/2026) ---

    val addCompetition get() = t("Add a competition", "Aggiungi una competizione", "Wettbewerb hinzufügen", "Ajouter une compétition", "Añadir una competición", "添加赛事", "대회 추가", "大会を追加")
    val competitions get() = t("Competitions", "Competizioni", "Wettbewerbe", "Compétitions", "Competiciones", "赛事", "대회", "大会")
    val competitionsNote get() = t(
        "The first one is the one you see first. Up to 10.",
        "La prima è quella che si vede per prima. Fino a 10.",
        "Der erste wird zuerst angezeigt. Bis zu 10.",
        "La première est celle qu'on voit en premier. Jusqu'à 10.",
        "La primera es la que se ve primero. Hasta 10.",
        "第一个最先显示。最多 10 个。",
        "첫 번째가 먼저 보여요. 최대 10개.",
        "最初のものが最初に表示されます。最大10件。"
    )
    val makeFirst get() = t("Show first", "Mostra per prima", "Zuerst zeigen", "Afficher en premier", "Mostrar primero", "首先显示", "먼저 표시", "最初に表示")
    val removeCompetition get() = t("Remove competition", "Togli la competizione", "Wettbewerb entfernen", "Retirer la compétition", "Quitar competición", "移除赛事", "대회 제거", "大会を削除")
    val matches get() = t("Matches", "Partite", "Spiele", "Matchs", "Partidos", "比赛", "경기", "試合")
    val standings get() = t("Standings", "Classifica", "Tabelle", "Classement", "Clasificación", "积分榜", "순위", "順位表")
    val ranking get() = t("Ranking", "Classifica", "Rangliste", "Classement", "Ranking", "排名", "랭킹", "ランキング")
    val noStandings get() = t("No standings for this competition", "Nessuna classifica per questa competizione", "Keine Tabelle für diesen Wettbewerb", "Pas de classement pour cette compétition", "Sin clasificación para esta competición", "此赛事没有积分榜", "이 대회는 순위가 없어요", "この大会には順位表がありません")
    val noMatches get() = t("No matches in these days", "Nessuna partita in questi giorni", "Keine Spiele in diesen Tagen", "Aucun match ces jours-ci", "Ningún partido estos días", "这几天没有比赛", "요즘 경기가 없어요", "この数日は試合がありません")
    val finalScore get() = t("Final", "Finale", "Ende", "Terminé", "Final", "完赛", "종료", "終了")
    val postponed get() = t("Postponed", "Rinviata", "Verschoben", "Reporté", "Aplazado", "延期", "연기", "延期")
    val team get() = t("Team", "Squadra", "Team", "Équipe", "Equipo", "球队", "팀", "チーム")
    val player get() = t("Player", "Giocatore", "Spieler", "Joueur", "Jugador", "球员", "선수", "選手")
    val sportsSource get() = t("Data: ESPN", "Dati: ESPN", "Daten: ESPN", "Données : ESPN", "Datos: ESPN", "数据：ESPN", "데이터: ESPN", "データ：ESPN")
    fun matchday(n: Int) = t("Matchday $n", "Giornata $n", "Spieltag $n", "Journée $n", "Jornada $n", "第${n}轮", "${n}라운드", "第${n}節")
    val previousCompetition get() = t("Previous competition", "Competizione precedente", "Vorheriger Wettbewerb", "Compétition précédente", "Competición anterior", "上一个赛事", "이전 대회", "前の大会")
    val nextCompetition get() = t("Next competition", "Competizione successiva", "Nächster Wettbewerb", "Compétition suivante", "Competición siguiente", "下一个赛事", "다음 대회", "次の大会")

    // --- Le stagioni e i migliori giocatori (dal 17/10/2026) ---
    val season get() = t("Season", "Stagione", "Saison", "Saison", "Temporada", "赛季", "시즌", "シーズン")
    val currentSeason get() = t("current", "in corso", "laufend", "en cours", "en curso", "本赛季", "현재", "今季")
    val players get() = t("Players", "Giocatori", "Spieler", "Joueurs", "Jugadores", "球员", "선수", "選手")
    val previousRound get() = t("Previous matchday", "Giornata precedente", "Vorheriger Spieltag", "Journée précédente", "Jornada anterior", "上一轮", "이전 라운드", "前の節")
    val nextRound get() = t("Next matchday", "Giornata successiva", "Nächster Spieltag", "Journée suivante", "Jornada siguiente", "下一轮", "다음 라운드", "次の節")
    val showLess get() = t("Show less", "Mostra meno", "Weniger anzeigen", "Afficher moins", "Mostrar menos", "收起", "간략히 보기", "表示を減らす")
    fun showAllPlayers(count: Int) = t("Show all ($count)", "Mostra tutti ($count)", "Alle anzeigen ($count)", "Tout afficher ($count)", "Mostrar todos ($count)", "全部显示 ($count)", "모두 표시 ($count)", "すべて表示 ($count)")
    val goalsLabel get() = t("Goals", "Gol", "Tore", "Buts", "Goles", "进球", "골", "ゴール")

    // --- Una partita da vicino (dal 17/10/2026) ---
    val events get() = t("Events", "Eventi", "Ereignisse", "Événements", "Eventos", "事件", "이벤트", "イベント")
    val statistics get() = t("Statistics", "Statistiche", "Statistiken", "Statistiques", "Estadísticas", "统计", "통계", "スタッツ")
    val scoreTab get() = t("Score", "Punteggio", "Spielstand", "Score", "Marcador", "比分", "점수", "スコア")
    fun additionalTime(n: Int) = t("Additional time $n", "Recupero $n'", "Nachspielzeit $n", "Temps additionnel $n", "Tiempo añadido $n", "补时 $n 分钟", "추가 시간 ${n}분", "アディショナルタイム $n")
    val halfTimeShort get() = t("HT", "INT", "HZ", "MT", "DESC", "半场", "하프타임", "HT")
    val fullTimeShort get() = t("FT", "FIN", "Ende", "FIN", "FIN", "全场", "종료", "FT")
    val goalCancelled get() = t("Goal cancelled", "Gol annullato", "Tor aberkannt", "But annulé", "Gol anulado", "进球无效", "골 취소", "ゴール取り消し")
    val missedPenalty get() = t("Missed penalty", "Rigore sbagliato", "Elfmeter verschossen", "Penalty manqué", "Penalti fallado", "点球未进", "페널티킥 실축", "PK失敗")
    val ownGoalMark get() = t("OG", "AG", "ET", "CSC", "PP", "乌龙", "자책골", "OG")
    val penaltyMark get() = t("Pen", "Rig", "Elfm.", "Pén.", "Pen.", "点球", "PK", "PK")
    fun assistBy(name: String) = t("Assist: $name", "Assist: $name", "Vorlage: $name", "Passe déc. : $name", "Asistencia: $name", "助攻：$name", "어시스트: $name", "アシスト: $name")
    val noEvents get() = t("No events yet", "Ancora nessun evento", "Noch keine Ereignisse", "Aucun événement pour l'instant", "Aún no hay eventos", "暂无事件", "아직 이벤트가 없어요", "まだイベントはありません")
    val noStats get() = t("No statistics for this match", "Nessuna statistica per questa partita", "Keine Statistiken für dieses Spiel", "Aucune statistique pour ce match", "Sin estadísticas para este partido", "此比赛没有统计", "이 경기는 통계가 없어요", "この試合のスタッツはありません")
    val totalShort get() = t("T", "Tot", "Ges.", "T", "T", "总", "합계", "計")

    fun cardReason(reason: com.gabriele.notionlocal.data.widgets.CardReason): String = when (reason) {
        com.gabriele.notionlocal.data.widgets.CardReason.FOUL -> t("Foul", "Fallo", "Foul", "Faute", "Falta", "犯规", "파울", "ファウル")
        com.gabriele.notionlocal.data.widgets.CardReason.HANDBALL -> t("Handball", "Fallo di mano", "Handspiel", "Main", "Mano", "手球", "핸드볼", "ハンド")
        com.gabriele.notionlocal.data.widgets.CardReason.VIOLENT_CONDUCT -> t("Violent conduct", "Condotta violenta", "Tätlichkeit", "Comportement violent", "Conducta violenta", "暴力行为", "폭력 행위", "乱暴な行為")
        com.gabriele.notionlocal.data.widgets.CardReason.EXCESSIVE_CELEBRATION -> t("Excessive celebration", "Esultanza eccessiva", "Übertriebener Torjubel", "Célébration excessive", "Celebración excesiva", "庆祝过度", "과도한 세리머니", "過度な喜び")
        com.gabriele.notionlocal.data.widgets.CardReason.DANGEROUS_PLAY -> t("Dangerous play", "Gioco pericoloso", "Gefährliches Spiel", "Jeu dangereux", "Juego peligroso", "危险动作", "위험한 플레이", "危険なプレー")
        com.gabriele.notionlocal.data.widgets.CardReason.TIME_WASTING -> t("Time wasting", "Perdita di tempo", "Zeitspiel", "Gain de temps", "Pérdida de tiempo", "拖延时间", "시간 지연", "遅延行為")
        com.gabriele.notionlocal.data.widgets.CardReason.UNSPORTING -> t("Unsporting behaviour", "Comportamento antisportivo", "Unsportliches Verhalten", "Comportement antisportif", "Conducta antideportiva", "非体育道德行为", "비신사적 행위", "非紳士的行為")
        com.gabriele.notionlocal.data.widgets.CardReason.DISSENT -> t("Dissent", "Proteste", "Meckern", "Contestation", "Protestas", "异议", "항의", "異議")
        com.gabriele.notionlocal.data.widgets.CardReason.SIMULATION -> t("Simulation", "Simulazione", "Schwalbe", "Simulation", "Simulación", "假摔", "시뮬레이션", "シミュレーション")
    }

    /** Il nome di una statistica di ESPN; null se non è fra quelle tradotte. */
    fun statLabel(key: String): String? = when (key) {
        "possessionPct" -> t("Ball possession", "Possesso palla", "Ballbesitz", "Possession", "Posesión", "控球率", "점유율", "ボール支配率")
        "totalShots" -> t("Total shots", "Tiri totali", "Schüsse", "Tirs", "Tiros totales", "射门", "슈팅", "シュート")
        "shotsOnTarget" -> t("Shots on target", "Tiri in porta", "Schüsse aufs Tor", "Tirs cadrés", "Tiros a puerta", "射正", "유효 슈팅", "枠内シュート")
        "wonCorners" -> t("Corner kicks", "Calci d'angolo", "Ecken", "Corners", "Saques de esquina", "角球", "코너킥", "コーナーキック")
        "offsides" -> t("Offsides", "Fuorigioco", "Abseits", "Hors-jeu", "Fueras de juego", "越位", "오프사이드", "オフサイド")
        "foulsCommitted" -> t("Fouls", "Falli", "Fouls", "Fautes", "Faltas", "犯规", "파울", "ファウル")
        "yellowCards" -> t("Yellow cards", "Cartellini gialli", "Gelbe Karten", "Cartons jaunes", "Tarjetas amarillas", "黄牌", "옐로카드", "イエローカード")
        "redCards" -> t("Red cards", "Cartellini rossi", "Rote Karten", "Cartons rouges", "Tarjetas rojas", "红牌", "레드카드", "レッドカード")
        "saves" -> t("Goalkeeper saves", "Parate", "Paraden", "Arrêts", "Paradas", "扑救", "선방", "セーブ")
        "totalPasses" -> t("Passes", "Passaggi", "Pässe", "Passes", "Pases", "传球", "패스", "パス")
        "accuratePasses" -> t("Accurate passes", "Passaggi riusciti", "Angekommene Pässe", "Passes réussies", "Pases precisos", "成功传球", "정확한 패스", "パス成功")
        "totalTackles" -> t("Tackles", "Contrasti", "Zweikämpfe", "Tacles", "Entradas", "抢断", "태클", "タックル")
        "interceptions" -> t("Interceptions", "Intercetti", "Abgefangene Bälle", "Interceptions", "Intercepciones", "拦截", "인터셉트", "インターセプト")
        "fieldGoalsMade-fieldGoalsAttempted" -> t("Field goals", "Tiri dal campo", "Würfe aus dem Feld", "Tirs", "Tiros de campo", "投篮", "야투", "フィールドゴール")
        "threePointFieldGoalsMade-threePointFieldGoalsAttempted" -> t("3-pointers", "Tiri da tre", "Dreier", "Tirs à 3 points", "Triples", "三分球", "3점슛", "3ポイント")
        "freeThrowsMade-freeThrowsAttempted" -> t("Free throws", "Tiri liberi", "Freiwürfe", "Lancers francs", "Tiros libres", "罚球", "자유투", "フリースロー")
        "totalRebounds" -> t("Rebounds", "Rimbalzi", "Rebounds", "Rebonds", "Rebotes", "篮板", "리바운드", "リバウンド")
        "assists" -> t("Assists", "Assist", "Assists", "Passes décisives", "Asistencias", "助攻", "어시스트", "アシスト")
        "steals" -> t("Steals", "Palle rubate", "Steals", "Interceptions", "Robos", "抢断", "스틸", "スティール")
        "blocks" -> t("Blocks", "Stoppate", "Blocks", "Contres", "Tapones", "盖帽", "블록", "ブロック")
        "turnovers" -> t("Turnovers", "Palle perse", "Ballverluste", "Pertes de balle", "Pérdidas", "失误", "턴오버", "ターンオーバー")
        "firstDowns" -> t("First downs", "Primi down", "First Downs", "Premiers essais", "Primeros downs", "首攻", "퍼스트 다운", "ファーストダウン")
        "totalYards" -> t("Total yards", "Yard totali", "Raumgewinn", "Yards totaux", "Yardas totales", "总码数", "총 야드", "総獲得ヤード")
        "netPassingYards" -> t("Passing yards", "Yard su passaggio", "Passing-Yards", "Yards à la passe", "Yardas por pase", "传球码数", "패싱 야드", "パスヤード")
        "rushingYards" -> t("Rushing yards", "Yard su corsa", "Rushing-Yards", "Yards à la course", "Yardas por tierra", "冲球码数", "러싱 야드", "ランヤード")
        "totalPenaltiesYards" -> t("Penalties", "Penalità", "Strafen", "Pénalités", "Castigos", "罚码", "페널티", "反則")
        "possessionTime" -> t("Time of possession", "Tempo di possesso", "Ballbesitzzeit", "Temps de possession", "Tiempo de posesión", "控球时间", "점유 시간", "ボール保持時間")
        else -> null
    }

    fun sportName(sport: com.gabriele.notionlocal.data.widgets.Sport): String = when (sport) {
        com.gabriele.notionlocal.data.widgets.Sport.SOCCER -> t("Football", "Calcio", "Fußball", "Football", "Fútbol", "足球", "축구", "サッカー")
        com.gabriele.notionlocal.data.widgets.Sport.TENNIS -> t("Tennis", "Tennis", "Tennis", "Tennis", "Tenis", "网球", "테니스", "テニス")
        com.gabriele.notionlocal.data.widgets.Sport.BASKETBALL -> t("Basketball", "Basket", "Basketball", "Basket", "Baloncesto", "篮球", "농구", "バスケットボール")
        com.gabriele.notionlocal.data.widgets.Sport.BASEBALL -> t("Baseball", "Baseball", "Baseball", "Baseball", "Béisbol", "棒球", "야구", "野球")
        com.gabriele.notionlocal.data.widgets.Sport.FOOTBALL -> t("American football", "Football americano", "American Football", "Football américain", "Fútbol americano", "美式橄榄球", "미식축구", "アメリカンフットボール")
    }
    val weather get() = t("Weather", "Meteo", "Wetter", "Météo", "Tiempo", "天气", "날씨", "天気")

    // --- Meteo (dal 17/10/2026) ---

    val addCity get() = t("Add a city", "Aggiungi una città", "Stadt hinzufügen", "Ajouter une ville", "Añadir una ciudad", "添加城市", "도시 추가", "都市を追加")
    val weatherCities get() = t("Cities", "Città", "Städte", "Villes", "Ciudades", "城市", "도시", "都市")
    val weatherCitiesNote get() = t(
        "The first one is the main city. Up to 10.",
        "La prima è la città principale. Fino a 10.",
        "Die erste ist die Hauptstadt. Bis zu 10.",
        "La première est la ville principale. Jusqu'à 10.",
        "La primera es la ciudad principal. Hasta 10.",
        "第一个是主要城市。最多 10 个。",
        "첫 번째가 기본 도시예요. 최대 10개.",
        "最初がメインの都市です。最大10件。"
    )
    val mainCity get() = t("Main", "Principale", "Haupt", "Principale", "Principal", "主要", "기본", "メイン")
    val changeCity get() = t("Change city", "Cambia città", "Stadt ändern", "Changer de ville", "Cambiar ciudad", "更换城市", "도시 변경", "都市を変更")
    val makeMainCity get() = t("Make main city", "Rendi principale", "Zur Hauptstadt machen", "Définir comme principale", "Hacer principal", "设为主要城市", "기본 도시로 설정", "メインにする")
    val removeCity get() = t("Remove city", "Togli la città", "Stadt entfernen", "Retirer la ville", "Quitar ciudad", "移除城市", "도시 제거", "都市を削除")
    val searchWeatherCity get() = t("Search a city", "Cerca una città", "Stadt suchen", "Rechercher une ville", "Buscar una ciudad", "搜索城市", "도시 검색", "都市を検索")
    val noCityFound get() = t("No city with this name", "Nessuna città con questo nome", "Keine Stadt mit diesem Namen", "Aucune ville de ce nom", "Ninguna ciudad con este nombre", "没有这个名字的城市", "이 이름의 도시가 없어요", "この名前の都市はありません")
    val noConnection get() = t("No connection", "Nessuna connessione", "Keine Verbindung", "Pas de connexion", "Sin conexión", "无网络连接", "연결 없음", "接続なし")
    val loading get() = t("Loading…", "Caricamento…", "Wird geladen…", "Chargement…", "Cargando…", "正在加载…", "불러오는 중…", "読み込み中…")
    val refresh get() = t("Refresh", "Aggiorna", "Aktualisieren", "Actualiser", "Actualizar", "刷新", "새로고침", "更新")
    fun updatedAt(time: String) = t("Updated $time", "Aggiornato alle $time", "Aktualisiert $time", "Mis à jour à $time", "Actualizado a las $time", "更新于 $time", "$time 업데이트", "$time 更新")
    fun maxMin(max: String, min: String) = t("H $max · L $min", "Max $max · Min $min", "H $max · T $min", "Max $max · Min $min", "Máx $max · Mín $min", "最高 $max · 最低 $min", "최고 $max · 최저 $min", "最高 $max · 最低 $min")
    fun feelsLike(value: String) = t("Feels like $value", "Percepita $value", "Gefühlt $value", "Ressenti $value", "Sensación $value", "体感 $value", "체감 $value", "体感 $value")
    fun humidity(value: Int) = t("Humidity $value%", "Umidità $value%", "Feuchte $value%", "Humidité $value%", "Humedad $value%", "湿度 $value%", "습도 $value%", "湿度 $value%")
    fun wind(kmh: Int) = t("Wind $kmh km/h", "Vento $kmh km/h", "Wind $kmh km/h", "Vent $kmh km/h", "Viento $kmh km/h", "风速 $kmh km/h", "바람 $kmh km/h", "風 $kmh km/h")
    val hourByHour get() = t("Hour by hour", "Ora per ora", "Stündlich", "Heure par heure", "Hora a hora", "逐小时", "시간별", "1時間ごと")
    val today get() = t("Today", "Oggi", "Heute", "Aujourd'hui", "Hoy", "今天", "오늘", "今日")
    val tomorrow get() = t("Tomorrow", "Domani", "Morgen", "Demain", "Mañana", "明天", "내일", "明日")
    val weatherSource get() = t("Data: Open-Meteo", "Dati: Open-Meteo", "Daten: Open-Meteo", "Données : Open-Meteo", "Datos: Open-Meteo", "数据：Open-Meteo", "데이터: Open-Meteo", "データ：Open-Meteo")

    fun skyName(sky: com.gabriele.notionlocal.data.widgets.WeatherSky): String = when (sky) {
        com.gabriele.notionlocal.data.widgets.WeatherSky.CLEAR -> t("Clear", "Sereno", "Klar", "Dégagé", "Despejado", "晴", "맑음", "快晴")
        com.gabriele.notionlocal.data.widgets.WeatherSky.MAINLY_CLEAR -> t("Mostly clear", "Quasi sereno", "Überwiegend klar", "Plutôt dégagé", "Mayormente despejado", "大部晴朗", "대체로 맑음", "晴れ")
        com.gabriele.notionlocal.data.widgets.WeatherSky.PARTLY_CLOUDY -> t("Partly cloudy", "Poco nuvoloso", "Teilweise bewölkt", "Partiellement nuageux", "Parcialmente nublado", "局部多云", "구름 조금", "所により曇り")
        com.gabriele.notionlocal.data.widgets.WeatherSky.OVERCAST -> t("Cloudy", "Nuvoloso", "Bewölkt", "Couvert", "Nublado", "阴", "흐림", "曇り")
        com.gabriele.notionlocal.data.widgets.WeatherSky.FOG -> t("Fog", "Nebbia", "Nebel", "Brouillard", "Niebla", "雾", "안개", "霧")
        com.gabriele.notionlocal.data.widgets.WeatherSky.DRIZZLE -> t("Drizzle", "Pioviggine", "Nieselregen", "Bruine", "Llovizna", "毛毛雨", "이슬비", "霧雨")
        com.gabriele.notionlocal.data.widgets.WeatherSky.FREEZING_DRIZZLE -> t("Freezing drizzle", "Pioviggine gelata", "Gefrierender Niesel", "Bruine verglaçante", "Llovizna helada", "冻毛毛雨", "어는 이슬비", "着氷性の霧雨")
        com.gabriele.notionlocal.data.widgets.WeatherSky.RAIN -> t("Rain", "Pioggia", "Regen", "Pluie", "Lluvia", "雨", "비", "雨")
        com.gabriele.notionlocal.data.widgets.WeatherSky.FREEZING_RAIN -> t("Freezing rain", "Pioggia gelata", "Gefrierender Regen", "Pluie verglaçante", "Lluvia helada", "冻雨", "어는 비", "着氷性の雨")
        com.gabriele.notionlocal.data.widgets.WeatherSky.SNOW -> t("Snow", "Neve", "Schnee", "Neige", "Nieve", "雪", "눈", "雪")
        com.gabriele.notionlocal.data.widgets.WeatherSky.SNOW_GRAINS -> t("Snow grains", "Granelli di neve", "Schneegriesel", "Neige en grains", "Granos de nieve", "米雪", "싸락눈", "霧雪")
        com.gabriele.notionlocal.data.widgets.WeatherSky.RAIN_SHOWERS -> t("Showers", "Rovesci", "Schauer", "Averses", "Chubascos", "阵雨", "소나기", "にわか雨")
        com.gabriele.notionlocal.data.widgets.WeatherSky.SNOW_SHOWERS -> t("Snow showers", "Rovesci di neve", "Schneeschauer", "Averses de neige", "Chubascos de nieve", "阵雪", "소낙눈", "にわか雪")
        com.gabriele.notionlocal.data.widgets.WeatherSky.THUNDERSTORM -> t("Thunderstorm", "Temporale", "Gewitter", "Orage", "Tormenta", "雷雨", "뇌우", "雷雨")
        com.gabriele.notionlocal.data.widgets.WeatherSky.THUNDERSTORM_HAIL -> t("Thunderstorm with hail", "Temporale con grandine", "Gewitter mit Hagel", "Orage avec grêle", "Tormenta con granizo", "雷雨伴冰雹", "우박을 동반한 뇌우", "雹を伴う雷雨")
    }
    val stopwatch get() = t("Stopwatch", "Cronometro", "Stoppuhr", "Chronomètre", "Cronómetro", "秒表", "스톱워치", "ストップウォッチ")
    val timer get() = t("Timer", "Timer", "Timer", "Minuteur", "Temporizador", "计时器", "타이머", "タイマー")
    val timeZones get() = t("Time zones", "Fusi orari", "Zeitzonen", "Fuseaux horaires", "Zonas horarias", "时区", "시간대", "タイムゾーン")
    val lifeProgress get() = t("Life progress", "Avanzamento", "Fortschritt", "Progression", "Progreso", "进度", "진행률", "進捗")
    val counter get() = t("Counter", "Contatore", "Zähler", "Compteur", "Contador", "计数器", "카운터", "カウンター")
    val pomodoro get() = t("Pomodoro", "Pomodoro", "Pomodoro", "Pomodoro", "Pomodoro", "番茄钟", "뽀모도로", "ポモドーロ")

    // --- Fusi orari ---

    val addClock get() = t("Add clock", "Aggiungi orologio", "Uhr hinzufügen", "Ajouter une horloge", "Añadir reloj", "添加时钟", "시계 추가", "時計を追加")
    val addToList get() = t("Add to the list", "Aggiungi all'elenco", "Zur Liste hinzufügen", "Ajouter à la liste", "Añadir a la lista", "添加到列表", "목록에 추가", "リストに追加")
    val analog get() = t("Analog", "Analogico", "Analog", "Analogique", "Analógico", "指针", "아날로그", "アナログ")
    val digital get() = t("Digital", "Digitale", "Digital", "Numérique", "Digital", "数字", "디지털", "デジタル")
    val changeTimeZone get() = t("Change time zone", "Cambia fuso orario", "Zeitzone ändern", "Changer de fuseau", "Cambiar zona horaria", "更改时区", "시간대 변경", "タイムゾーンを変更")
    val chooseTimeZone get() = t("Choose a time zone", "Scegli un fuso orario", "Zeitzone wählen", "Choisir un fuseau horaire", "Elegir zona horaria", "选择时区", "시간대 선택", "タイムゾーンを選択")
    val searchCity get() = t("Search a city or a region", "Cerca una città o una regione", "Stadt oder Region suchen", "Rechercher une ville ou une région", "Buscar ciudad o región", "搜索城市或地区", "도시 또는 지역 검색", "都市や地域を検索")
    val day get() = t("Day", "Giorno", "Tag", "Jour", "Día", "白天", "낮", "昼")
    val night get() = t("Night", "Notte", "Nacht", "Nuit", "Noche", "夜晚", "밤", "夜")

    // --- Avanzamento ---

    val year get() = t("Year", "Anno", "Jahr", "Année", "Año", "年", "연", "年")
    val month get() = t("Month", "Mese", "Monat", "Mois", "Mes", "月", "월", "月")
    val week get() = t("Week", "Settimana", "Woche", "Semaine", "Semana", "周", "주", "週")
    val dayOfToday get() = t("Day", "Giorno", "Tag", "Jour", "Día", "日", "일", "日")
    val addDate get() = t("Add a date", "Aggiungi una data", "Datum hinzufügen", "Ajouter une date", "Añadir una fecha", "添加日期", "날짜 추가", "日付を追加")
    val editDate get() = t("Edit", "Modifica", "Bearbeiten", "Modifier", "Editar", "编辑", "편집", "編集")
    val name get() = t("Name", "Nome", "Name", "Nom", "Nombre", "名称", "이름", "名前")
    val from get() = t("From", "Da", "Von", "Du", "Desde", "从", "시작", "開始")
    val to get() = t("To", "A", "Bis", "Au", "Hasta", "到", "끝", "終了")
    val endBeforeStart get() = t("The end comes before the start.", "La fine viene prima dell'inizio.", "Das Ende liegt vor dem Anfang.", "La fin est avant le début.", "El final es anterior al inicio.", "结束早于开始。", "끝이 시작보다 빠릅니다.", "終了が開始より前です。")
    val barColor get() = t("Color", "Colore", "Farbe", "Couleur", "Color", "颜色", "색상", "色")

    // --- Contatore ---

    val increase get() = t("Plus one", "Più uno", "Plus eins", "Plus un", "Más uno", "加一", "하나 더하기", "1つ増やす")
    val decrease get() = t("Minus one", "Meno uno", "Minus eins", "Moins un", "Menos uno", "减一", "하나 빼기", "1つ減らす")
    val reset get() = t("Reset", "Azzera", "Zurücksetzen", "Réinitialiser", "Reiniciar", "重置", "초기화", "リセット")
    val rename get() = EditorStrings.rename

    // --- I suoni del contatore (dal 18/10/2026) ---
    val sounds get() = t("Sounds", "Suoni", "Töne", "Sons", "Sonidos", "声音", "소리", "サウンド")
    val soundsOn get() = t("Play sounds", "Suoni attivi", "Töne abspielen", "Jouer les sons", "Reproducir sonidos", "播放声音", "소리 재생", "サウンドを再生")
    val plusSound get() = t("Sound for +", "Suono del +", "Ton für +", "Son pour +", "Sonido para +", "+ 的声音", "+ 소리", "+ の音")
    val minusSound get() = t("Sound for −", "Suono del −", "Ton für −", "Son pour −", "Sonido para −", "− 的声音", "− 소리", "− の音")
    val soundsNote get() = t(
        "Tap a sound to hear it. 100% is the phone's System volume; in silent mode they don't play.",
        "Tocca un suono per sentirlo. Il 100% è il volume \"Sistema\" del telefono; in silenzioso non si sentono.",
        "Tippe auf einen Ton, um ihn zu hören. 100 % ist die Systemlautstärke des Telefons; im Lautlos-Modus sind sie stumm.",
        "Touchez un son pour l'écouter. 100 % correspond au volume Système du téléphone ; en mode silencieux, ils ne sont pas joués.",
        "Toca un sonido para oírlo. El 100 % es el volumen del sistema del teléfono; en modo silencio no suenan.",
        "点按声音即可试听。100% 即手机的系统音量；静音模式下不会播放。",
        "소리를 탭하면 들을 수 있어요. 100%는 휴대전화의 시스템 볼륨이에요. 무음 모드에서는 재생되지 않아요.",
        "音をタップすると試聴できます。100% はスマホのシステム音量です。マナーモードでは鳴りません。"
    )
    val volume get() = t("Volume", "Volume", "Lautstärke", "Volume", "Volumen", "音量", "볼륨", "音量")
    fun soundName(sound: com.gabriele.notionlocal.data.widgets.CounterSound): String = when (sound) {
        com.gabriele.notionlocal.data.widgets.CounterSound.CLICK -> t("Click", "Click", "Klick", "Clic", "Clic", "咔哒", "클릭", "クリック")
        com.gabriele.notionlocal.data.widgets.CounterSound.SOFT_CLICK -> t("Soft click", "Click lieve", "Leiser Klick", "Clic doux", "Clic suave", "轻咔哒", "부드러운 클릭", "ソフトクリック")
        com.gabriele.notionlocal.data.widgets.CounterSound.TICK -> t("Tick", "Tic", "Tick", "Tic", "Tic", "滴答", "틱", "チック")
        com.gabriele.notionlocal.data.widgets.CounterSound.POP -> t("Pop", "Pop", "Plopp", "Pop", "Pop", "啵", "팝", "ポン")
        com.gabriele.notionlocal.data.widgets.CounterSound.BUBBLE -> t("Drop", "Goccia", "Tropfen", "Goutte", "Gota", "水滴", "물방울", "しずく")
        com.gabriele.notionlocal.data.widgets.CounterSound.WOOD -> t("Wood", "Legno", "Holz", "Bois", "Madera", "木块", "나무", "ウッド")
        com.gabriele.notionlocal.data.widgets.CounterSound.BEEP -> t("Beep", "Bip", "Piep", "Bip", "Bip", "哔", "삐", "ビープ")
        com.gabriele.notionlocal.data.widgets.CounterSound.COIN -> t("Coin", "Moneta", "Münze", "Pièce", "Moneda", "硬币", "동전", "コイン")
    }

    // --- Pomodoro ---

    val session get() = t("Session", "Sessione", "Sitzung", "Session", "Sesión", "专注", "세션", "セッション")
    val breakTime get() = t("Break", "Pausa", "Pause", "Pause", "Descanso", "休息", "휴식", "休憩")
    val breakLength get() = t("Break length", "Durata della pausa", "Pausendauer", "Durée de la pause", "Duración del descanso", "休息时长", "휴식 길이", "休憩の長さ")
    val minutesShort get() = t("min", "min", "Min.", "min", "min", "分钟", "분", "分")
    val minutes get() = t("Minutes", "Minuti", "Minuten", "Minutes", "Minutos", "分钟", "분", "分")
    val seconds get() = t("Seconds", "Secondi", "Sekunden", "Secondes", "Segundos", "秒", "초", "秒")
    val start get() = t("Start", "Avvia", "Starten", "Démarrer", "Iniciar", "开始", "시작", "開始")
    val pause get() = t("Pause", "Pausa", "Pausieren", "Pause", "Pausar", "暂停", "일시정지", "一時停止")
    val timerSettings get() = t("Timer settings", "Impostazioni del timer", "Timer-Einstellungen", "Réglages du minuteur", "Ajustes del temporizador", "计时器设置", "타이머 설정", "タイマー設定")

    /** Il suono di fine fase del pomodoro, nell'ingranaggio. */
    val sound get() = t("Sound", "Suono", "Ton", "Son", "Sonido", "声音", "소리", "サウンド")
    /** Il suono di quando ricomincia la sessione di studio (dal 28/09/2026). */
    val studySound get() = t("Study sound", "Suono dello studio", "Lernton", "Son de l'étude", "Sonido del estudio", "学习提示音", "공부 알림음", "勉強のサウンド")
    /** Il suono di quando comincia la pausa (dal 28/09/2026). */
    val breakSound get() = t("Break sound", "Suono della pausa", "Pausenton", "Son de la pause", "Sonido del descanso", "休息提示音", "휴식 알림음", "休憩のサウンド")
    val setTime get() = t("Set the time", "Imposta il tempo", "Zeit einstellen", "Régler le temps", "Ajustar el tiempo", "设置时间", "시간 설정", "時間を設定")
    val sessionName get() = t("Session name", "Nome della sessione", "Name der Sitzung", "Nom de la session", "Nombre de la sesión", "专注名称", "세션 이름", "セッション名")

    // --- Cronometro e timer (dal 25/09/2026) ---

    val lap get() = t("Lap", "Giro", "Runde", "Tour", "Vuelta", "计次", "랩", "ラップ")
    val lapTimes get() = t("Lap times", "Tempo del giro", "Rundenzeit", "Temps du tour", "Tiempo de vuelta", "计次时间", "구간 기록", "ラップタイム")
    val overallTime get() = t("Overall time", "Tempo totale", "Gesamtzeit", "Temps total", "Tiempo total", "总时间", "전체 시간", "合計時間")
    val stop get() = t("Stop", "Stop", "Stopp", "Arrêter", "Detener", "停止", "중지", "停止")
    val resume get() = t("Resume", "Riprendi", "Fortsetzen", "Reprendre", "Reanudar", "继续", "계속", "再開")
    val hours get() = t("Hours", "Ore", "Stunden", "Heures", "Horas", "小时", "시간", "時間")
    val addTimer get() = t("Add timer", "Aggiungi timer", "Timer hinzufügen", "Ajouter un minuteur", "Añadir temporizador", "添加计时器", "타이머 추가", "タイマーを追加")
    val deleteTimer get() = t("Delete timer", "Elimina timer", "Timer löschen", "Supprimer le minuteur", "Eliminar temporizador", "删除计时器", "타이머 삭제", "タイマーを削除")
    val resetTimer get() = t("Reset timer", "Azzera il timer", "Timer zurücksetzen", "Réinitialiser le minuteur", "Reiniciar temporizador", "重置计时器", "타이머 초기화", "タイマーをリセット")
    val timerEndsAt get() = t("Ends at", "Finisce alle", "Endet um", "Se termine à", "Termina a las", "结束于", "종료 시각", "終了時刻")
}
