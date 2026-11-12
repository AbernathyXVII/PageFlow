package com.gabriele.notionlocal.ui.i18n

/**
 * Le voci dell'editor delle pagine: i tipi di blocco (menu "/" e "+"), la
 * barra sopra la tastiera, le immagini, il menu degli elenchi numerati, il
 * selettore dei colori e la cronologia. Stessa regola di `Strings`: ogni
 * voce con le sue otto traduzioni, e solo testo dell'app.
 */
object EditorStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    // --- Font e corpo della pagina, nella barra Aa ---

    /** La voce del font quando la pagina non ne ha scelto uno: quello di sistema. */
    val defaultFont get() = t("Default", "Predefinito", "Standard", "Par défaut", "Predeterminado", "默认", "기본", "デフォルト")
    val font get() = t("Font", "Carattere", "Schriftart", "Police", "Fuente", "字体", "글꼴", "フォント")
    val fontSize get() = t("Font size", "Dimensione del carattere", "Schriftgröße", "Taille de police", "Tamaño de fuente", "字号", "글꼴 크기", "フォントサイズ")
    /** Toccando font o corpo senza niente di selezionato: valgono per il testo selezionato. */
    val selectTextFirst get() = t("Select some text first", "Seleziona prima del testo", "Wähle zuerst Text aus", "Sélectionnez d'abord du texte", "Selecciona primero un texto", "请先选择文字", "먼저 텍스트를 선택하세요", "先にテキストを選択してください")
    fun fontSizeRange(min: Int, max: Int) = t("From $min to $max", "Da $min a $max", "Von $min bis $max", "De $min à $max", "De $min a $max", "$min 到 $max", "$min~$max", "$min～$max")
    /** Il pulsante che rimette il corpo normale (16) nella finestra del corpo. */
    val defaultFontSize get() = t("Default", "Predefinito", "Standard", "Par défaut", "Predeterminado", "默认", "기본값", "デフォルト")

    // --- Il menu di un blocco (sei puntini nella barra, o il dito tenuto su una pagina) ---

    val blockOptions get() = t("Block options", "Opzioni del blocco", "Blockoptionen", "Options du bloc", "Opciones del bloque", "块选项", "블록 옵션", "ブロックのオプション")
    val turnInto get() = t("Turn into", "Trasforma in", "Umwandeln in", "Transformer en", "Convertir en", "转换为", "전환", "変換")
    val color get() = t("Color", "Colore", "Farbe", "Couleur", "Color", "颜色", "색상", "カラー")
    val editIcon get() = t("Edit icon", "Modifica icona", "Symbol bearbeiten", "Modifier l'icône", "Editar icono", "编辑图标", "아이콘 편집", "アイコンを編集")
    val addToFavorites get() = t("Add to favorites", "Aggiungi ai preferiti", "Zu Favoriten hinzufügen", "Ajouter aux favoris", "Añadir a favoritos", "添加到收藏", "즐겨찾기에 추가", "お気に入りに追加")
    val removeFromFavorites get() = t("Remove from favorites", "Togli dai preferiti", "Aus Favoriten entfernen", "Retirer des favoris", "Quitar de favoritos", "从收藏中移除", "즐겨찾기에서 제거", "お気に入りから削除")
    // --- Copia, incolla, taglia, seleziona tutto nella barra (dal 18/10/2026) ---
    val copy get() = t("Copy", "Copia", "Kopieren", "Copier", "Copiar", "复制", "복사", "コピー")
    val paste get() = t("Paste", "Incolla", "Einfügen", "Coller", "Pegar", "粘贴", "붙여넣기", "貼り付け")
    val cut get() = t("Cut", "Taglia", "Ausschneiden", "Couper", "Cortar", "剪切", "잘라내기", "切り取り")
    val selectAll get() = t("Select all", "Seleziona tutto", "Alles auswählen", "Tout sélectionner", "Seleccionar todo", "全选", "모두 선택", "すべて選択")
    val pasteKeepSource get() = t("Keep Source Formatting", "Mantieni formattazione originale", "Ursprüngliche Formatierung beibehalten", "Conserver la mise en forme source", "Mantener formato de origen", "保留源格式", "원본 서식 유지", "元の書式を保持")
    val pasteMatchDestination get() = t("Match Destination Formatting", "Usa formattazione di destinazione", "Zielformatierung übernehmen", "Respecter la mise en forme de destination", "Usar formato de destino", "匹配目标格式", "대상 서식과 일치", "貼り付け先の書式に合わせる")
    val pasteTextOnly get() = t("Keep Text Only", "Mantieni solo testo", "Nur den Text übernehmen", "Conserver uniquement le texte", "Mantener solo texto", "只保留文本", "텍스트만 유지", "テキストのみ保持")
    val rename get() = t("Rename", "Rinomina", "Umbenennen", "Renommer", "Cambiar nombre", "重命名", "이름 바꾸기", "名前を変更")
    val turnIntoSimpleDatabase get() = t("Turn into simple database", "Trasforma in database semplice", "In einfache Datenbank umwandeln", "Convertir en base de données simple", "Convertir en base de datos simple", "转换为简单数据库", "단순 데이터베이스로 전환", "シンプルなデータベースに変換")
    val turnIntoComplexDatabase get() = t("Turn into complex database", "Trasforma in database complesso", "In komplexe Datenbank umwandeln", "Convertir en base de données complexe", "Convertir en base de datos compleja", "转换为复杂数据库", "복합 데이터베이스로 전환", "複合データベースに変換")
    val lockDatabase get() = t("Lock database", "Blocca il database", "Datenbank sperren", "Verrouiller la base de données", "Bloquear base de datos", "锁定数据库", "데이터베이스 잠금", "データベースをロック")
    val openAsPage get() = t("Open as page", "Apri come pagina", "Als Seite öffnen", "Ouvrir en tant que page", "Abrir como página", "以页面打开", "페이지로 열기", "ページとして開く")
    val simpleDatabaseLossTitle get() = t("Turn into simple database?", "Trasformare in database semplice?", "In einfache Datenbank umwandeln?", "Convertir en base de données simple ?", "¿Convertir en base de datos simple?", "转换为简单数据库？", "단순 데이터베이스로 전환할까요?", "シンプルなデータベースに変換しますか？")

    /** Quante pagine di riga con del contenuto "Turn into simple database" cancellerebbe per sempre. */
    fun simpleDatabaseLossText(n: Int) = if (n == 1) {
        t(
            "1 row page has content: it will be deleted forever, with everything inside it. Row names and properties stay.",
            "1 pagina di riga ha del contenuto: verrà cancellata per sempre, con tutto quello che c'è dentro. I nomi delle righe e le proprietà restano.",
            "1 Zeilenseite hat Inhalt: Sie wird endgültig gelöscht, mit allem, was darin ist. Zeilennamen und Eigenschaften bleiben erhalten.",
            "1 page de ligne a du contenu : elle sera supprimée définitivement, avec tout ce qu'elle contient. Les noms des lignes et les propriétés restent.",
            "1 página de fila tiene contenido: se eliminará para siempre, con todo lo que contiene. Los nombres de las filas y las propiedades se mantienen.",
            "1 个行页面有内容：它将被永久删除，连同其中的所有内容。行名称和属性会保留。",
            "행 페이지 1개에 내용이 있습니다. 안의 내용과 함께 영구적으로 삭제됩니다. 행 이름과 속성은 유지됩니다.",
            "内容のある行ページが 1 件あります。中身ごと完全に削除されます。行の名前とプロパティは残ります。"
        )
    } else {
        t(
            "$n row pages have content: they will be deleted forever, with everything inside them. Row names and properties stay.",
            "$n pagine di riga hanno del contenuto: verranno cancellate per sempre, con tutto quello che c'è dentro. I nomi delle righe e le proprietà restano.",
            "$n Zeilenseiten haben Inhalt: Sie werden endgültig gelöscht, mit allem, was darin ist. Zeilennamen und Eigenschaften bleiben erhalten.",
            "$n pages de ligne ont du contenu : elles seront supprimées définitivement, avec tout ce qu'elles contiennent. Les noms des lignes et les propriétés restent.",
            "$n páginas de fila tienen contenido: se eliminarán para siempre, con todo lo que contienen. Los nombres de las filas y las propiedades se mantienen.",
            "$n 个行页面有内容：它们将被永久删除，连同其中的所有内容。行名称和属性会保留。",
            "행 페이지 ${n}개에 내용이 있습니다. 안의 내용과 함께 영구적으로 삭제됩니다. 행 이름과 속성은 유지됩니다.",
            "内容のある行ページが $n 件あります。中身ごと完全に削除されます。行の名前とプロパティは残ります。"
        )
    }

    /**
     * Il nome di un tipo di blocco nella lingua dell'app, partendo da quello
     * inglese. I cataloghi dei menu restano scritti in inglese — è il nome
     * con cui li si riconosce nel codice e con cui si cerca nel menu "/" —
     * e si traducono solo quando si mostrano.
     */
    fun blockType(english: String): String = when (english) {
        "Text" -> t("Text", "Testo", "Text", "Texte", "Texto", "文本", "텍스트", "テキスト")
        "Bulleted list" -> t("Bulleted list", "Elenco puntato", "Aufzählung", "Liste à puces", "Lista con viñetas", "项目符号列表", "글머리 기호 목록", "箇条書きリスト")
        "Numbered list" -> t("Numbered list", "Elenco numerato", "Nummerierte Liste", "Liste numérotée", "Lista numerada", "编号列表", "번호 매기기 목록", "番号付きリスト")
        "To-do list" -> t("To-do list", "Elenco di cose da fare", "To-do-Liste", "Liste de tâches", "Lista de tareas", "待办清单", "할 일 목록", "ToDoリスト")
        "Notification" -> t("Notification", "Notifica", "Benachrichtigung", "Notification", "Notificación", "通知", "알림", "通知")
        "Toggle list" -> t("Toggle list", "Elenco a scomparsa", "Aufklappliste", "Liste dépliante", "Lista desplegable", "折叠列表", "토글 목록", "トグルリスト")
        "Table" -> t("Table", "Tabella", "Tabelle", "Tableau", "Tabla", "表格", "표", "テーブル")
        "Page" -> t("Page", "Pagina", "Seite", "Page", "Página", "页面", "페이지", "ページ")
        "Database" -> t("Database", "Database", "Datenbank", "Base de données", "Base de datos", "数据库", "데이터베이스", "データベース")
        "Divider" -> t("Divider", "Divisore", "Trennlinie", "Séparateur", "Divisor", "分割线", "구분선", "区切り線")
        "Callout" -> t("Callout", "Riquadro", "Hinweisbox", "Encadré", "Destacado", "标注", "콜아웃", "コールアウト")
        "Quote" -> t("Quote", "Citazione", "Zitat", "Citation", "Cita", "引用", "인용", "引用")
        "Link to page" -> t("Link to page", "Link a una pagina", "Link zu einer Seite", "Lien vers une page", "Enlace a una página", "链接到页面", "페이지 링크", "ページへのリンク")
        "Button" -> t("Button", "Pulsante", "Schaltfläche", "Bouton", "Botón", "按钮", "버튼", "ボタン")
        "Link to button" -> t("Link to button", "Collegamento a pulsante", "Link zu Schaltfläche", "Lien vers un bouton", "Enlace a botón", "链接到按钮", "버튼 링크", "ボタンへのリンク")
        "Synced block" -> SyncStrings.syncedBlock
        "Create two columns" -> t("Create two columns", "Crea due colonne", "Zwei Spalten erstellen", "Créer deux colonnes", "Crear dos columnas", "创建两栏", "2단 만들기", "2列を作成")
        "Create three columns" -> t("Create three columns", "Crea tre colonne", "Drei Spalten erstellen", "Créer trois colonnes", "Crear tres columnas", "创建三栏", "3단 만들기", "3列を作成")
        "Create four columns" -> t("Create four columns", "Crea quattro colonne", "Vier Spalten erstellen", "Créer quatre colonnes", "Crear cuatro columnas", "创建四栏", "4단 만들기", "4列を作成")
        "Columns" -> t("Columns", "Colonne", "Spalten", "Colonnes", "Columnas", "分栏", "단", "列")
        "Record audio" -> CaptureStrings.recordAudio
        "Scan document" -> CaptureStrings.scanDocument
        "Image" -> t("Image", "Immagine", "Bild", "Image", "Imagen", "图片", "이미지", "画像")
        "Video" -> t("Video", "Video", "Video", "Vidéo", "Vídeo", "视频", "동영상", "動画")
        "Audio" -> t("Audio", "Audio", "Audio", "Audio", "Audio", "音频", "오디오", "音声")
        "Code" -> t("Code", "Codice", "Code", "Code", "Código", "代码", "코드", "コード")
        "File" -> t("File", "File", "Datei", "Fichier", "Archivo", "文件", "파일", "ファイル")
        "Web bookmark" -> t("Web bookmark", "Segnalibro web", "Web-Lesezeichen", "Signet web", "Marcador web", "网页书签", "웹 북마크", "Webブックマーク")
        "Table view" -> t("Table view", "Vista tabella", "Tabellenansicht", "Vue tableau", "Vista de tabla", "表格视图", "표 보기", "テーブルビュー")
        "Board view" -> t("Board view", "Vista bacheca", "Board-Ansicht", "Vue Kanban", "Vista de tablero", "看板视图", "보드 보기", "ボードビュー")
        "Gallery view" -> t("Gallery view", "Vista galleria", "Galerieansicht", "Vue galerie", "Vista de galería", "画廊视图", "갤러리 보기", "ギャラリービュー")
        "List view" -> t("List view", "Vista elenco", "Listenansicht", "Vue liste", "Vista de lista", "列表视图", "목록 보기", "リストビュー")
        "Calendar view" -> t("Calendar view", "Vista calendario", "Kalenderansicht", "Vue calendrier", "Vista de calendario", "日历视图", "캘린더 보기", "カレンダービュー")
        "Timeline view" -> t("Timeline view", "Vista linea temporale", "Zeitleistenansicht", "Vue chronologie", "Vista de cronograma", "时间线视图", "타임라인 보기", "タイムラインビュー")
        "Chart view" -> t("Chart view", "Vista grafico", "Diagrammansicht", "Vue graphique", "Vista de gráfico", "图表视图", "차트 보기", "グラフビュー")
        "Database - Inline" -> t("Database - Inline", "Database - In linea", "Datenbank – Inline", "Base de données – Intégrée", "Base de datos – En línea", "数据库 - 内嵌", "데이터베이스 - 인라인", "データベース - インライン")
        "Database - Full page" -> t("Database - Full page", "Database - Pagina intera", "Datenbank – Ganze Seite", "Base de données – Pleine page", "Base de datos – Página completa", "数据库 - 整页", "데이터베이스 - 전체 페이지", "データベース - フルページ")
        "Simple database" -> t("Simple database", "Database semplice", "Einfache Datenbank", "Base de données simple", "Base de datos simple", "简单数据库", "간단한 데이터베이스", "シンプルなデータベース")
        "Linked view of data source" -> t("Linked view of data source", "Vista collegata di un'origine dati", "Verknüpfte Ansicht einer Datenquelle", "Vue liée d'une source de données", "Vista vinculada de una fuente de datos", "数据源的关联视图", "데이터 소스의 연결된 보기", "データソースのリンクビュー")
        "Basic blocks" -> t("Basic blocks", "Blocchi di base", "Basisblöcke", "Blocs de base", "Bloques básicos", "基础块", "기본 블록", "基本ブロック")
        "Media" -> t("Media", "Media", "Medien", "Médias", "Multimedia", "媒体", "미디어", "メディア")
        else -> english
    }

    val nothingWithThatName get() = t("Nothing with that name", "Niente con questo nome", "Nichts mit diesem Namen", "Rien avec ce nom", "Nada con ese nombre", "没有匹配的项目", "해당 이름의 항목이 없습니다", "該当する項目はありません")

    // --- La barra sopra la tastiera ---

    val spoiler get() = t("Spoiler", "Spoiler", "Spoiler", "Spoiler", "Spoiler", "剧透", "스포일러", "ネタバレ")
    /** Il collegamento sul testo selezionato (dal 17/10/2026): l'icona e la sua finestrella. */
    val link get() = t("Link", "Link", "Link", "Lien", "Enlace", "链接", "링크", "リンク")
    val linkHint get() = t("Paste or type a link", "Incolla o scrivi un link", "Link einfügen oder eingeben", "Collez ou saisissez un lien", "Pega o escribe un enlace", "粘贴或输入链接", "링크를 붙여넣거나 입력하세요", "リンクを貼り付けるか入力")
    val removeLink get() = t("Remove link", "Togli il link", "Link entfernen", "Supprimer le lien", "Quitar enlace", "移除链接", "링크 삭제", "リンクを削除")
    val cantOpenLink get() = t("Can't open this link", "Impossibile aprire questo link", "Dieser Link lässt sich nicht öffnen", "Impossible d'ouvrir ce lien", "No se puede abrir este enlace", "无法打开此链接", "이 링크를 열 수 없습니다", "このリンクを開けません")

    // --- I collegamenti dentro la pagina (dal 20/10/2026), vedi `BlockLinks.kt` ---
    val pinAsLinkTarget get() = t("Pin as link target", "Fissa come destinazione", "Als Linkziel anheften", "Épingler comme cible de lien", "Fijar como destino de enlace", "固定为链接目标", "링크 대상으로 고정", "リンク先として固定")
    val unpinLinkTarget get() = t("Unpin link target", "Togli dalle destinazioni", "Linkziel lösen", "Retirer des cibles de lien", "Quitar de los destinos", "取消固定链接目标", "링크 대상 고정 해제", "リンク先の固定を解除")
    val linkWeb get() = t("Web", "Web", "Web", "Web", "Web", "网页", "웹", "ウェブ")
    val linkInThisPage get() = t("In this page", "In questa pagina", "Auf dieser Seite", "Dans cette page", "En esta página", "本页内", "이 페이지", "このページ内")
    // --- Quello che si vede di un link (dal 27/10/2026), vedi `LinkShowChoices` ---
    val linkShow get() = t("Show", "Mostra", "Anzeigen", "Afficher", "Mostrar", "显示", "표시", "表示")
    val linkShowLink get() = t("Full link", "Il link per intero", "Ganzer Link", "Lien complet", "Enlace completo", "完整链接", "전체 링크", "リンク全体")
    val linkShowTitle get() = t("Page title", "Il titolo della pagina", "Seitentitel", "Titre de la page", "Título de la página", "网页标题", "페이지 제목", "ページのタイトル")
    val linkShowText get() = t("Your text", "Una scritta tua", "Eigener Text", "Votre texte", "Tu texto", "自定义文字", "직접 입력한 텍스트", "自分で入力した文字")
    val linkTextHint get() = t("Text to show", "Il testo da mostrare", "Angezeigter Text", "Texte à afficher", "Texto que se muestra", "要显示的文字", "표시할 텍스트", "表示する文字")
    val linkReadingTitle get() = t("Reading the title…", "Leggo il titolo…", "Titel wird gelesen…", "Lecture du titre…", "Leyendo el título…", "正在读取标题…", "제목을 읽는 중…", "タイトルを読み込み中…")
    val linkNoTitle get() = t("No title found", "Titolo non trovato", "Kein Titel gefunden", "Aucun titre trouvé", "No se encontró el título", "未找到标题", "제목을 찾을 수 없어요", "タイトルが見つかりません")
    val noLinkTargets get() = t(
        "Nothing to link to yet. Pin a line from its six-dot menu, or write it in large text.",
        "Ancora niente a cui collegarsi. Fissa una riga dal suo menu dei sei puntini, o scrivila in grande.",
        "Noch keine Ziele. Hefte eine Zeile über ihr Sechs-Punkte-Menü an oder schreibe sie groß.",
        "Aucune cible pour l'instant. Épinglez une ligne depuis son menu à six points, ou écrivez-la en grand.",
        "Aún no hay destinos. Fija una línea desde su menú de seis puntos, o escríbela en grande.",
        "还没有可链接的位置。从六点菜单固定一行，或用大字书写。",
        "아직 연결할 곳이 없습니다. 점 6개 메뉴에서 줄을 고정하거나 큰 글씨로 쓰세요.",
        "リンク先がまだありません。6点メニューから行を固定するか、大きな文字で書いてください。"
    )
    val linkTargetMissing get() = t(
        "The linked line is no longer in this page",
        "La riga collegata non è più in questa pagina",
        "Die verlinkte Zeile ist nicht mehr auf dieser Seite",
        "La ligne liée n'est plus dans cette page",
        "La línea enlazada ya no está en esta página",
        "链接的行已不在此页面中",
        "연결된 줄이 이 페이지에 더 이상 없습니다",
        "リンク先の行はこのページにもうありません"
    )
    // --- Tutta la pagina selezionata (dal 20/10/2026), vedi `PageSelection.kt` ---
    val pageCopied get() = t("Page copied", "Pagina copiata", "Seite kopiert", "Page copiée", "Página copiada", "页面已复制", "페이지가 복사됨", "ページをコピーしました")
    val deletePageContentTitle get() = t(
        "Delete everything in this page?",
        "Eliminare tutto il contenuto della pagina?",
        "Alles auf dieser Seite löschen?",
        "Supprimer tout le contenu de cette page ?",
        "¿Eliminar todo el contenido de esta página?",
        "删除此页面中的所有内容？",
        "이 페이지의 모든 내용을 삭제할까요?",
        "このページの内容をすべて削除しますか？"
    )
    val deletePageContentText get() = t(
        "Subpages and databases stay where they are. You can undo it.",
        "Le sottopagine e i database restano dove sono. Si può annullare.",
        "Unterseiten und Datenbanken bleiben, wo sie sind. Du kannst es rückgängig machen.",
        "Les sous-pages et les bases de données restent en place. Vous pouvez annuler.",
        "Las subpáginas y las bases de datos se quedan donde están. Se puede deshacer.",
        "子页面和数据库会保留在原处。可以撤销。",
        "하위 페이지와 데이터베이스는 그대로 남습니다. 실행 취소할 수 있습니다.",
        "サブページとデータベースはそのまま残ります。元に戻せます。"
    )
    val untitledLine get() = t("Empty line", "Riga vuota", "Leere Zeile", "Ligne vide", "Línea vacía", "空行", "빈 줄", "空の行")
    val subscript get() = t("Subscript", "Pedice", "Tiefgestellt", "Indice", "Subíndice", "下标", "아래 첨자", "下付き文字")
    val superscript get() = t("Superscript", "Apice", "Hochgestellt", "Exposant", "Superíndice", "上标", "위 첨자", "上付き文字")
    val outdent get() = t("Outdent", "Riduci rientro", "Einzug verkleinern", "Diminuer le retrait", "Reducir sangría", "减少缩进", "내어쓰기", "インデントを減らす")
    val indent get() = t("Indent", "Aumenta rientro", "Einzug vergrößern", "Augmenter le retrait", "Aumentar sangría", "增加缩进", "들여쓰기", "インデントを増やす")
    val moveUp get() = t("Move up", "Sposta su", "Nach oben", "Monter", "Subir", "上移", "위로 이동", "上へ移動")
    val moveDown get() = t("Move down", "Sposta giù", "Nach unten", "Descendre", "Bajar", "下移", "아래로 이동", "下へ移動")
    /** "Move to line…" (dal 20/10/2026): il blocco sotto una riga qualsiasi della pagina, vedi `MoveToLineSheet`. */
    val moveToLine get() = t("Move to line…", "Sposta sotto una riga…", "Unter eine Zeile verschieben…", "Déplacer sous une ligne…", "Mover bajo una línea…", "移到某行下方…", "줄 아래로 이동…", "行の下へ移動…")
    val moveToLineHint get() = t(
        "Tap the line to put it under",
        "Tocca la riga sotto cui metterlo",
        "Tippe auf die Zeile, unter die es soll",
        "Touchez la ligne sous laquelle le placer",
        "Toca la línea debajo de la que ponerlo",
        "点按要放在其下方的行",
        "아래에 둘 줄을 누르세요",
        "下に置く行をタップしてください"
    )
    val topOfPage get() = t("Top of the page", "In cima alla pagina", "Ganz oben auf der Seite", "En haut de la page", "Arriba del todo", "页面顶部", "페이지 맨 위", "ページの一番上")
    val addAbove get() = t("Add above", "Aggiungi sopra", "Oberhalb hinzufügen", "Ajouter au-dessus", "Añadir arriba", "在上方添加", "위에 추가", "上に追加")
    val addBelow get() = t("Add below", "Aggiungi sotto", "Unterhalb hinzufügen", "Ajouter en dessous", "Añadir abajo", "在下方添加", "아래에 추가", "下に追加")
    val addOrChangeBlock get() = t("Add or change block type", "Aggiungi o cambia tipo di blocco", "Blocktyp hinzufügen oder ändern", "Ajouter ou changer le type de bloc", "Añadir o cambiar el tipo de bloque", "添加或更改块类型", "블록 유형 추가 또는 변경", "ブロックの種類を追加・変更")
    val textAndBackgroundColor get() = t("Text and background color", "Colore del testo e dello sfondo", "Text- und Hintergrundfarbe", "Couleur du texte et du fond", "Color del texto y del fondo", "文字和背景颜色", "글자 및 배경 색상", "文字と背景の色")
    val undo get() = t("Undo", "Annulla", "Rückgängig", "Annuler", "Deshacer", "撤销", "실행 취소", "元に戻す")
    val redo get() = t("Redo", "Ripeti", "Wiederholen", "Rétablir", "Rehacer", "重做", "다시 실행", "やり直す")
    val deleteEmptyBlock get() = t("Delete empty block", "Elimina il blocco vuoto", "Leeren Block löschen", "Supprimer le bloc vide", "Eliminar el bloque vacío", "删除空块", "빈 블록 삭제", "空のブロックを削除")

    // --- Icona e copertina ---

    val icon get() = t("Icon", "Icona", "Symbol", "Icône", "Icono", "图标", "아이콘", "アイコン")
    val cover get() = t("Cover", "Copertina", "Titelbild", "Couverture", "Portada", "封面", "커버", "カバー")
    val addCover get() = t("Add cover", "Aggiungi copertina", "Titelbild hinzufügen", "Ajouter une couverture", "Añadir portada", "添加封面", "커버 추가", "カバーを追加")
    val addIcon get() = t("Add icon", "Aggiungi icona", "Symbol hinzufügen", "Ajouter une icône", "Añadir icono", "添加图标", "아이콘 추가", "アイコンを追加")
    val viewFullScreen get() = t("View full screen", "Vedi a tutto schermo", "Im Vollbild ansehen", "Voir en plein écran", "Ver en pantalla completa", "全屏查看", "전체 화면으로 보기", "全画面で見る")
    val close get() = t("Close", "Chiudi", "Schließen", "Fermer", "Cerrar", "关闭", "닫기", "閉じる")
    val upload get() = t("Upload", "Carica", "Hochladen", "Importer", "Subir", "上传", "업로드", "アップロード")
    val reposition get() = t("Reposition", "Riposiziona", "Neu positionieren", "Repositionner", "Reposicionar", "调整位置", "위치 조정", "位置を調整")
    val remove get() = t("Remove", "Rimuovi", "Entfernen", "Retirer", "Quitar", "移除", "제거", "削除")
    val dragToMove get() = t("Drag to move · pinch to zoom", "Trascina per spostare · pizzica per ingrandire", "Ziehen zum Verschieben · Zwei Finger zum Zoomen", "Glissez pour déplacer · pincez pour zoomer", "Arrastra para mover · pellizca para ampliar", "拖动以移动 · 双指缩放", "드래그하여 이동 · 핀치하여 확대", "ドラッグで移動・ピンチで拡大")
    val save get() = t("Save", "Salva", "Speichern", "Enregistrer", "Guardar", "保存", "저장", "保存")
    val imageReadFailed get() = t("Couldn't read the image.", "Non sono riuscito a leggere l'immagine.", "Das Bild konnte nicht gelesen werden.", "Impossible de lire l'image.", "No se pudo leer la imagen.", "无法读取图片。", "이미지를 읽을 수 없습니다.", "画像を読み込めませんでした。")

    // --- Blocchi e finestre dell'editor ---

    val emptyToggle get() = t(
        "Empty toggle. Tap to add a block inside.",
        "Toggle vuoto. Tocca per aggiungere un blocco.",
        "Leerer Toggle. Tippe, um einen Block hinzuzufügen.",
        "Liste dépliante vide. Touchez pour ajouter un bloc.",
        "Desplegable vacío. Toca para añadir un bloque.",
        "空的折叠列表。点按以在其中添加块。",
        "빈 토글입니다. 탭하여 블록을 추가하세요.",
        "空のトグルです。タップしてブロックを追加します。"
    )
    val deleteTable get() = t("Delete table", "Elimina tabella", "Tabelle löschen", "Supprimer le tableau", "Eliminar tabla", "删除表格", "표 삭제", "テーブルを削除")
    val deleteDatabaseTitle get() = t("Delete this database?", "Eliminare questo database?", "Diese Datenbank löschen?", "Supprimer cette base de données ?", "¿Eliminar esta base de datos?", "删除此数据库？", "이 데이터베이스를 삭제할까요?", "このデータベースを削除しますか？")
    val deleteDatabaseText get() = t(
        "The database, its properties and all its pages will be permanently deleted. This can't be undone.",
        "Il database, le sue proprietà e tutte le sue pagine verranno eliminati definitivamente. Non si può annullare.",
        "Die Datenbank, ihre Eigenschaften und alle ihre Seiten werden endgültig gelöscht. Das kann nicht rückgängig gemacht werden.",
        "La base de données, ses propriétés et toutes ses pages seront supprimées définitivement. Action irréversible.",
        "La base de datos, sus propiedades y todas sus páginas se eliminarán definitivamente. No se puede deshacer.",
        "该数据库、其属性及其所有页面都将被永久删除，且无法撤销。",
        "데이터베이스와 속성, 모든 페이지가 영구 삭제됩니다. 되돌릴 수 없습니다.",
        "データベースとそのプロパティ、すべてのページが完全に削除されます。元に戻すことはできません。"
    )

    // --- Il menu degli elenchi numerati ---

    val beginNewList get() = t("Begin a new list here", "Inizia qui un nuovo elenco", "Hier eine neue Liste beginnen", "Commencer une nouvelle liste ici", "Empezar una lista nueva aquí", "从这里开始新列表", "여기서 새 목록 시작", "ここから新しいリストを開始")
    val renumberContinuation get() = t("Renumber as continuation of previous list", "Rinumera come continuazione dell'elenco precedente", "Als Fortsetzung der vorherigen Liste nummerieren", "Renuméroter à la suite de la liste précédente", "Renumerar como continuación de la lista anterior", "接续上一个列表编号", "이전 목록에 이어서 번호 매기기", "前のリストの続きとして番号を振り直す")
    val removeNumber get() = t("Remove number", "Rimuovi il numero", "Nummer entfernen", "Supprimer le numéro", "Quitar el número", "移除编号", "번호 제거", "番号を削除")
    val changeToBullet get() = t("Change to bullet", "Cambia in elenco puntato", "In Aufzählungspunkt ändern", "Changer en puce", "Cambiar a viñeta", "改为项目符号", "글머리 기호로 변경", "箇条書きに変更")

    // --- Il pennello ---

    val colorText get() = t("Text", "Testo", "Text", "Texte", "Texto", "文字", "글자", "文字")
    val colorBackground get() = t("Background", "Sfondo", "Hintergrund", "Fond", "Fondo", "背景", "배경", "背景")
    val textColor get() = t("Text color", "Colore del testo", "Textfarbe", "Couleur du texte", "Color del texto", "文字颜色", "글자 색상", "文字の色")
    val backgroundColor get() = t("Background color", "Colore dello sfondo", "Hintergrundfarbe", "Couleur du fond", "Color del fondo", "背景颜色", "배경 색상", "背景の色")
    val red get() = t("Red", "Rosso", "Rot", "Rouge", "Rojo", "红", "빨강", "赤")
    val green get() = t("Green", "Verde", "Grün", "Vert", "Verde", "绿", "초록", "緑")
    val blue get() = t("Blue", "Blu", "Blau", "Bleu", "Azul", "蓝", "파랑", "青")
    val noColor get() = t("No color", "Nessun colore", "Keine Farbe", "Aucune couleur", "Sin color", "无颜色", "색상 없음", "色なし")
    /** Gli ultimi colori usati, sotto il campo Hex del selettore (dal 05/10/2026). */
    val recentColors get() = t("Recent colors", "Colori recenti", "Zuletzt verwendet", "Couleurs récentes", "Colores recientes", "最近使用的颜色", "최근 사용한 색", "最近使用した色")
    /** I colori preferiti, sotto i recenti (dal 20/10/2026); la stella accanto all'anteprima li aggiunge e li toglie. */
    val favoriteColors get() = t("Favorite colors", "Colori preferiti", "Lieblingsfarben", "Couleurs favorites", "Colores favoritos", "收藏的颜色", "즐겨찾는 색", "お気に入りの色")
    val addFavoriteColor get() = t("Add to favorite colors", "Aggiungi ai colori preferiti", "Zu den Lieblingsfarben hinzufügen", "Ajouter aux couleurs favorites", "Añadir a los colores favoritos", "添加到收藏的颜色", "즐겨찾는 색에 추가", "お気に入りの色に追加")
    val removeFavoriteColor get() = t("Remove from favorite colors", "Togli dai colori preferiti", "Aus den Lieblingsfarben entfernen", "Retirer des couleurs favorites", "Quitar de los colores favoritos", "从收藏的颜色中移除", "즐겨찾는 색에서 제거", "お気に入りの色から削除")
    val favoriteColorsHint get() = t(
        "Tap the star to keep the current color here",
        "Tocca la stella per tenere qui il colore scelto",
        "Tippe auf den Stern, um die aktuelle Farbe hier zu behalten",
        "Touchez l'étoile pour garder ici la couleur actuelle",
        "Toca la estrella para guardar aquí el color actual",
        "点按星标，把当前颜色保存在这里",
        "별을 눌러 현재 색을 여기에 저장하세요",
        "星をタップすると、今の色をここに保存できます"
    )
    val apply get() = t("Apply", "Applica", "Anwenden", "Appliquer", "Aplicar", "应用", "적용", "適用")

    // --- La cronologia ("Updates") ---

    fun noChangesYet(title: String) = t(
        "No changes recorded for \"$title\" yet.\n\nWhatever is written or corrected from now on shows up here: one line per edit, not one per keystroke.",
        "Nessuna modifica registrata per \"$title\".\n\nQuello che scrivi o correggi da ora in poi compare qui: una riga per modifica, non una per tasto premuto.",
        "Für „$title“ wurden noch keine Änderungen aufgezeichnet.\n\nAlles, was ab jetzt geschrieben oder korrigiert wird, erscheint hier: eine Zeile pro Änderung, nicht pro Tastendruck.",
        "Aucune modification enregistrée pour « $title » pour l'instant.\n\nTout ce qui est écrit ou corrigé à partir de maintenant apparaîtra ici : une ligne par modification, pas une par touche.",
        "Todavía no hay cambios registrados en «$title».\n\nTodo lo que escribas o corrijas a partir de ahora aparecerá aquí: una línea por cambio, no una por tecla.",
        "“$title”还没有记录任何修改。\n\n从现在起写入或更正的内容都会显示在这里：每次修改一行，而不是每次按键一行。",
        "\"$title\"에 기록된 변경 사항이 아직 없습니다.\n\n지금부터 작성하거나 수정한 내용이 여기에 표시됩니다. 키 입력마다가 아니라 수정마다 한 줄씩 기록됩니다.",
        "「$title」の変更履歴はまだありません。\n\nこれから書いたり修正したりした内容がここに表示されます。キー入力ごとではなく、変更ごとに1行ずつ記録されます。"
    )
    val thisPage get() = t("this page", "questa pagina", "diese Seite", "cette page", "esta página", "此页面", "이 페이지", "このページ")
    val editAdded get() = t("added", "aggiunto", "hinzugefügt", "ajouté", "añadido", "新增", "추가됨", "追加")
    val editDeleted get() = t("deleted", "eliminato", "gelöscht", "supprimé", "eliminado", "删除", "삭제됨", "削除")
    val editTitle get() = t("title", "titolo", "Titel", "titre", "título", "标题", "제목", "タイトル")
}
