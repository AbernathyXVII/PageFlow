package com.gabriele.notionlocal.ui.i18n

/**
 * Le voci del **Web Clipper** (dal 20/10/2026): il pannello come quello di
 * OneNote Web Clipper, la foto in corso e "Region". Stessa regola di
 * `Strings`: ogni voce con le sue otto traduzioni.
 */
object WebClipStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    val webClipper get() = "Web Clipper"
    val link get() = t("Link", "Link", "Link", "Lien", "Enlace", "链接", "링크", "リンク")
    val linkPlaceholder get() = t("Paste or type a link…", "Incolla o scrivi un link…", "Link einfügen oder eingeben…", "Collez ou saisissez un lien…", "Pega o escribe un enlace…", "粘贴或输入链接…", "링크를 붙여넣거나 입력하세요…", "リンクを貼り付けるか入力…")
    val paste get() = t("Paste", "Incolla", "Einfügen", "Coller", "Pegar", "粘贴", "붙여넣기", "貼り付け")
    val notALink get() = t("That doesn't look like a link", "Questo non sembra un link", "Das sieht nicht wie ein Link aus", "Cela ne ressemble pas à un lien", "Eso no parece un enlace", "这看起来不像链接", "링크가 아닌 것 같습니다", "リンクではないようです")
    val whatToCapture get() = t(
        "What do you want to capture?",
        "Cosa vuoi catturare?",
        "Was möchtest du erfassen?",
        "Que voulez-vous capturer ?",
        "¿Qué quieres capturar?",
        "你想捕获什么？",
        "무엇을 캡처할까요?",
        "何をキャプチャしますか?"
    )
    val fullPage get() = t("Full Page", "Pagina intera", "Ganze Seite", "Page entière", "Página completa", "整页", "전체 페이지", "ページ全体")
    val region get() = t("Region", "Area", "Bereich", "Zone", "Región", "区域", "영역", "領域")
    val bookmark get() = t("Bookmark", "Segnalibro", "Lesezeichen", "Signet", "Marcador", "书签", "북마크", "ブックマーク")
    val article get() = t("Article", "Articolo", "Artikel", "Article", "Artículo", "文章", "기사", "記事")
    /** "Dark mode" (dal 20/10/2026): la foto col sito scurito. */
    val darkMode get() = t("Dark mode", "Modalità scura", "Dunkelmodus", "Mode sombre", "Modo oscuro", "深色模式", "다크 모드", "ダークモード")
    val darkModeHint get() = t(
        "The page is clipped with dark colors",
        "La pagina viene catturata con i colori scuri",
        "Die Seite wird mit dunklen Farben erfasst",
        "La page est capturée en couleurs sombres",
        "La página se captura con colores oscuros",
        "以深色截取页面",
        "페이지를 어두운 색으로 캡처합니다",
        "ページを暗い色でキャプチャします"
    )
    val textAndImages get() = t("Text and Images", "Testo e immagini", "Text und Bilder", "Texte et images", "Texto e imágenes", "文字和图片", "텍스트와 이미지", "テキストと画像")
    val stepReading get() = t("Reading the article…", "Leggo l'articolo…", "Artikel wird gelesen…", "Lecture de l'article…", "Leyendo el artículo…", "正在读取文章…", "기사를 읽는 중…", "記事を読み込んでいます…")
    fun stepImages(done: Int, total: Int) = t(
        "Downloading the images ($done of $total)…",
        "Scarico le immagini ($done di $total)…",
        "Bilder werden geladen ($done von $total)…",
        "Téléchargement des images ($done sur $total)…",
        "Descargando las imágenes ($done de $total)…",
        "正在下载图片（$done/$total）…",
        "이미지를 내려받는 중($done/$total)…",
        "画像をダウンロードしています（$done/$total）…"
    )
    val noArticle get() = t(
        "No article found in this page",
        "In questa pagina non ho trovato un articolo",
        "Auf dieser Seite wurde kein Artikel gefunden",
        "Aucun article trouvé dans cette page",
        "No se encontró ningún artículo en esta página",
        "此页面中未找到文章",
        "이 페이지에서 기사를 찾지 못했습니다",
        "このページに記事が見つかりません"
    )
    val title get() = t("Title", "Titolo", "Titel", "Titre", "Título", "标题", "제목", "タイトル")
    val titlePlaceholder get() = t("Add a title…", "Aggiungi un titolo…", "Titel hinzufügen…", "Ajouter un titre…", "Añade un título…", "添加标题…", "제목 추가…", "タイトルを追加…")
    val note get() = t("Note", "Nota", "Notiz", "Note", "Nota", "备注", "메모", "メモ")
    val notePlaceholder get() = t("Add a note…", "Aggiungi una nota…", "Notiz hinzufügen…", "Ajouter une note…", "Añade una nota…", "添加备注…", "메모 추가…", "メモを追加…")
    val source get() = t("Source", "Fonte", "Quelle", "Source", "Fuente", "来源", "출처", "ソース")
    val clip get() = t("Clip", "Cattura", "Erfassen", "Capturer", "Capturar", "剪辑", "클립", "クリップ")
    val clipping get() = t("Clipping…", "Cattura in corso…", "Wird erfasst…", "Capture en cours…", "Capturando…", "正在剪辑…", "클립하는 중…", "クリップ中…")
    val stepOpening get() = t("Opening the page…", "Apro la pagina…", "Seite wird geöffnet…", "Ouverture de la page…", "Abriendo la página…", "正在打开页面…", "페이지를 여는 중…", "ページを開いています…")
    val stepCleaning get() = t(
        "Removing cookie banners and loading the images…",
        "Tolgo i banner dei cookie e carico le immagini…",
        "Cookie-Banner werden entfernt, Bilder werden geladen…",
        "Suppression des bannières de cookies et chargement des images…",
        "Quitando los avisos de cookies y cargando las imágenes…",
        "正在移除 Cookie 横幅并加载图片…",
        "쿠키 배너를 없애고 이미지를 불러오는 중…",
        "Cookie バナーを消して画像を読み込んでいます…"
    )
    val stepArticle get() = t("Keeping only the article…", "Tengo solo l'articolo…", "Nur der Artikel bleibt…", "Seul l'article est conservé…", "Dejando solo el artículo…", "只保留文章…", "기사만 남기는 중…", "記事だけを残しています…")
    val stepCapturing get() = t("Taking the picture…", "Faccio la foto…", "Bild wird aufgenommen…", "Prise de la capture…", "Haciendo la captura…", "正在截图…", "캡처하는 중…", "撮影しています…")
    val clipFailed get() = t(
        "This page couldn't be clipped",
        "Non sono riuscito a catturare questa pagina",
        "Diese Seite konnte nicht erfasst werden",
        "Impossible de capturer cette page",
        "No se pudo capturar esta página",
        "无法剪辑此页面",
        "이 페이지를 클립할 수 없습니다",
        "このページをクリップできませんでした"
    )
    val clipped get() = t("Clipped into this page", "Catturata in questa pagina", "In diese Seite übernommen", "Capturée dans cette page", "Capturada en esta página", "已剪辑到此页面", "이 페이지에 클립했습니다", "このページにクリップしました")
    /** La riga sotto la foto, prima del link: come "Clipped from:" di OneNote. */
    val clippedFrom get() = t("Clipped from", "Catturata da", "Erfasst von", "Capturée depuis", "Capturada de", "剪辑自", "클립 출처", "クリップ元")
    val regionHint get() = t(
        "Scroll to the part you want, then tap Select and drag over it.",
        "Scorri fino alla parte che vuoi, poi tocca Seleziona e trascina sopra di lei.",
        "Scrolle zum gewünschten Teil, tippe auf Auswählen und ziehe darüber.",
        "Faites défiler jusqu'à la partie voulue, touchez Sélectionner et faites glisser dessus.",
        "Desplázate hasta la parte que quieras, toca Seleccionar y arrastra sobre ella.",
        "滚动到想要的部分，点按“选择”后在上面拖动。",
        "원하는 부분으로 스크롤한 다음 선택을 누르고 그 위를 드래그하세요.",
        "必要な部分までスクロールし、「選択」をタップしてその上をドラッグしてください。"
    )
    val dragHint get() = t("Drag over the area to clip", "Trascina sopra l'area da catturare", "Ziehe über den Bereich", "Faites glisser sur la zone", "Arrastra sobre la zona", "在要剪辑的区域上拖动", "클립할 영역 위를 드래그하세요", "クリップする範囲をドラッグ")
    val select get() = t("Select", "Seleziona", "Auswählen", "Sélectionner", "Seleccionar", "选择", "선택", "選択")
    val browse get() = t("Scroll", "Scorri", "Scrollen", "Défiler", "Desplazar", "滚动", "스크롤", "スクロール")

    // --- Sul PC (dal 10/11/2026) ---

    val noBrowserOnPc get() = t(
        "To clip on the computer PageFlow uses Microsoft Edge (or Chrome), which wasn't found",
        "Per catturare sul computer PageFlow usa Microsoft Edge (o Chrome), che non c'è",
        "Zum Erfassen am Computer nutzt PageFlow Microsoft Edge (oder Chrome), das nicht gefunden wurde",
        "Pour capturer sur l'ordinateur, PageFlow utilise Microsoft Edge (ou Chrome), introuvable",
        "Para capturar en el ordenador PageFlow usa Microsoft Edge (o Chrome), que no se encontró",
        "在电脑上剪辑需要 Microsoft Edge（或 Chrome），但未找到",
        "컴퓨터에서 클립하려면 Microsoft Edge(또는 Chrome)가 필요한데 찾을 수 없어요",
        "パソコンでクリップするには Microsoft Edge（または Chrome）が必要ですが、見つかりません"
    )
    val regionHintOnPc get() = t(
        "Drag with the mouse over the part you want; the wheel scrolls the page.",
        "Trascina col mouse sopra la parte che vuoi; la rotella scorre la pagina.",
        "Ziehe mit der Maus über den gewünschten Teil; das Mausrad scrollt die Seite.",
        "Faites glisser la souris sur la partie voulue ; la molette fait défiler la page.",
        "Arrastra con el ratón sobre la parte que quieras; la rueda desplaza la página.",
        "用鼠标在想要的部分上拖动；滚轮可滚动页面。",
        "원하는 부분 위를 마우스로 드래그하세요. 휠로 페이지를 스크롤해요.",
        "必要な部分をマウスでドラッグしてください。ホイールでページをスクロールできます。"
    )
}
