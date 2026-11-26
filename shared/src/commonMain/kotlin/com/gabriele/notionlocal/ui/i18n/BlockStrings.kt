package com.gabriele.notionlocal.ui.i18n

/**
 * Le voci dei blocchi nuovi del 06/10/2026 — riquadro, citazione, codice,
 * immagine, video, audio, file, segnalibro, link a una pagina, database a
 * pagina intera. Stessa regola di `Strings`: ogni voce con le sue otto
 * traduzioni, e solo testo dell'app.
 */
object BlockStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    // --- Riquadro e citazione ---

    /** Il suggerimento grigio di una citazione vuota, come su Notion. */
    val emptyQuote get() = t("Empty quote", "Citazione vuota", "Leeres Zitat", "Citation vide", "Cita vacía", "空引用", "빈 인용", "空の引用")
    /** La famiglia di emoji dei riquadri, come la chiama Notion. */
    val calloutEmojis get() = t("Callout", "Riquadro", "Hinweisbox", "Encadré", "Destacado", "标注", "콜아웃", "コールアウト")

    // --- Link a una pagina ---

    val linkToPageHint get() = t(
        "Choose the page to link to: it stays where it is.",
        "Scegli la pagina a cui rimandare: resta dov'è.",
        "Wähle die Seite, auf die verwiesen wird: Sie bleibt, wo sie ist.",
        "Choisissez la page à lier : elle reste où elle est.",
        "Elige la página a la que enlazar: se queda donde está.",
        "选择要链接的页面：它会留在原处。",
        "링크할 페이지를 고르세요. 페이지는 그대로 있어요.",
        "リンクするページを選んでください。ページはそのままの場所に残ります。"
    )
    /** Al posto del nome, quando la pagina richiamata è stata cancellata per sempre. */
    val deletedPage get() = t("Deleted page", "Pagina eliminata", "Gelöschte Seite", "Page supprimée", "Página eliminada", "已删除的页面", "삭제된 페이지", "削除されたページ")
    val inTrash get() = t("In the trash", "Nel cestino", "Im Papierkorb", "Dans la corbeille", "En la papelera", "在回收站中", "휴지통에 있음", "ゴミ箱内")
    val changePage get() = t("Change page", "Cambia pagina", "Seite ändern", "Changer de page", "Cambiar página", "更换页面", "페이지 변경", "ページを変更")
    val openPage get() = t("Open", "Apri", "Öffnen", "Ouvrir", "Abrir", "打开", "열기", "開く")

    // --- Immagini, video, audio e file ---

    val addImage get() = t("Add an image", "Aggiungi un'immagine", "Bild hinzufügen", "Ajouter une image", "Añadir una imagen", "添加图片", "이미지 추가", "画像を追加")
    val view get() = t("View", "Guarda", "Ansehen", "Voir", "Ver", "查看", "보기", "表示")
    val alignLeft get() = t("Align left", "Allinea a sinistra", "Linksbündig", "Aligner à gauche", "Alinear a la izquierda", "左对齐", "왼쪽 정렬", "左揃え")
    val alignCenter get() = t("Center", "Al centro", "Zentriert", "Centrer", "Centrar", "居中", "가운데 정렬", "中央揃え")
    val alignRight get() = t("Align right", "Allinea a destra", "Rechtsbündig", "Aligner à droite", "Alinear a la derecha", "右对齐", "오른쪽 정렬", "右揃え")
    val caption get() = t("Caption", "Didascalia", "Bildunterschrift", "Légende", "Leyenda", "说明文字", "캡션", "キャプション")
    val imageTitle get() = t("Title", "Titolo", "Titel", "Titre", "Título", "标题", "제목", "タイトル")
    val captionPlace get() = t("Caption position", "Posizione della didascalia", "Position der Bildunterschrift", "Position de la légende", "Posición de la leyenda", "说明文字位置", "캡션 위치", "キャプションの位置")
    val captionBelow get() = t("Below the image", "Sotto l'immagine", "Unter dem Bild", "Sous l'image", "Debajo de la imagen", "图片下方", "이미지 아래", "画像の下")
    val captionLeft get() = t("Left of the image", "A sinistra dell'immagine", "Links vom Bild", "À gauche de l'image", "A la izquierda de la imagen", "图片左侧", "이미지 왼쪽", "画像の左")
    val captionRight get() = t("Right of the image", "A destra dell'immagine", "Rechts vom Bild", "À droite de l'image", "A la derecha de la imagen", "图片右侧", "이미지 오른쪽", "画像の右")
    val alignTop get() = t("Align top", "In alto", "Oben", "En haut", "Arriba", "顶部对齐", "위쪽 정렬", "上揃え")
    val alignMiddle get() = t("Middle", "Al centro", "Mittig", "Au milieu", "En el medio", "居中", "가운데", "中央")
    val alignBottom get() = t("Align bottom", "In basso", "Unten", "En bas", "Abajo", "底部对齐", "아래쪽 정렬", "下揃え")
    val captionHint get() =t("Write a caption…", "Scrivi una didascalia…", "Bildunterschrift schreiben…", "Écrire une légende…", "Escribe una leyenda…", "写一段说明…", "캡션 쓰기…", "キャプションを書く…")
    val imageTitleHint get() = t("Write a title…", "Scrivi un titolo…", "Titel schreiben…", "Écrire un titre…", "Escribe un título…", "写一个标题…", "제목 쓰기…", "タイトルを書く…")
    val download get() = t("Download", "Scarica", "Herunterladen", "Télécharger", "Descargar", "下载", "다운로드", "ダウンロード")
    val replace get() = t("Replace", "Sostituisci", "Ersetzen", "Remplacer", "Reemplazar", "替换", "바꾸기", "置き換え")
    val imageSaved get() = t("Saved in Pictures/PageFlow", "Salvata in Immagini/PageFlow", "In Bilder/PageFlow gespeichert", "Enregistrée dans Images/PageFlow", "Guardada en Imágenes/PageFlow", "已保存到 图片/PageFlow", "사진/PageFlow에 저장했습니다", "ピクチャ/PageFlow に保存しました")
    val fileSaved get() = t("File saved", "File salvato", "Datei gespeichert", "Fichier enregistré", "Archivo guardado", "文件已保存", "파일을 저장했습니다", "ファイルを保存しました")
    val saveFailed get() = t("Couldn't save the file", "Non sono riuscito a salvare il file", "Datei konnte nicht gespeichert werden", "Impossible d'enregistrer le fichier", "No se pudo guardar el archivo", "无法保存文件", "파일을 저장할 수 없습니다", "ファイルを保存できませんでした")
    val moveBlockHint get() = t(
        "It goes to the bottom of the page you choose.",
        "Va in fondo alla pagina che scegli.",
        "Es kommt ans Ende der gewählten Seite.",
        "Il va en bas de la page choisie.",
        "Va al final de la página que elijas.",
        "它会移到你选择的页面底部。",
        "선택한 페이지의 맨 아래로 이동해요.",
        "選んだページの一番下に移動します。"
    )
    /** Le maniglie ai lati di un'immagine, per allargarla o stringerla. */
    val resizeImage get() = t("Resize", "Ridimensiona", "Größe ändern", "Redimensionner", "Cambiar tamaño", "调整大小", "크기 조정", "サイズを変更")
    /** "Full resize" (dal 20/10/2026): le maniglie su tutti e quattro i lati e sugli angoli, come una finestra. */
    val fullResize get() = t("Full resize", "Ridimensiona liberamente", "Frei skalieren", "Redimensionner librement", "Redimensionar libremente", "自由调整大小", "자유롭게 크기 조정", "自由にサイズ変更")
    val originalSize get() = t("Original size", "Dimensione originale", "Originalgröße", "Taille d'origine", "Tamaño original", "原始大小", "원래 크기", "元のサイズ")

    // --- Video e audio ---

    val addVideo get() = t("Embed or upload a video", "Incorpora o carica un video", "Video einbetten oder hochladen", "Intégrer ou importer une vidéo", "Insertar o subir un vídeo", "嵌入或上传视频", "동영상 삽입 또는 업로드", "動画を埋め込むかアップロード")
    val addAudio get() = t("Add an audio file", "Aggiungi un file audio", "Audiodatei hinzufügen", "Ajouter un fichier audio", "Añadir un archivo de audio", "添加音频文件", "오디오 파일 추가", "音声ファイルを追加")
    val uploadTab get() = t("Upload", "Carica", "Hochladen", "Importer", "Subir", "上传", "업로드", "アップロード")
    val linkTab get() = t("Link", "Link", "Link", "Lien", "Enlace", "链接", "링크", "リンク")
    val chooseVideo get() = t("Choose a video", "Scegli un video", "Video auswählen", "Choisir une vidéo", "Elegir un vídeo", "选择视频", "동영상 선택", "動画を選ぶ")
    val chooseFile get() = t("Choose a file", "Scegli un file", "Datei auswählen", "Choisir un fichier", "Elegir un archivo", "选择文件", "파일 선택", "ファイルを選ぶ")
    val pasteLink get() = t("Paste in https://…", "Incolla https://…", "https://… einfügen", "Collez https://…", "Pega https://…", "粘贴 https://…", "https://… 붙여넣기", "https://… を貼り付け")
    val embedVideo get() = t("Embed video", "Incorpora il video", "Video einbetten", "Intégrer la vidéo", "Insertar vídeo", "嵌入视频", "동영상 삽입", "動画を埋め込む")
    val embedAudio get() = t("Embed audio", "Incorpora l'audio", "Audio einbetten", "Intégrer l'audio", "Insertar audio", "嵌入音频", "오디오 삽입", "音声を埋め込む")
    val videoLinkHint get() = t(
        "YouTube and Vimeo play in their player; other links must point to the video itself (.mp4…).",
        "YouTube e Vimeo si vedono nel loro lettore; gli altri link devono portare al video stesso (.mp4…).",
        "YouTube und Vimeo laufen in ihrem Player; andere Links müssen direkt auf das Video zeigen (.mp4…).",
        "YouTube et Vimeo se lisent dans leur lecteur ; les autres liens doivent mener à la vidéo elle-même (.mp4…).",
        "YouTube y Vimeo se ven en su reproductor; los demás enlaces deben llevar al vídeo mismo (.mp4…).",
        "YouTube 和 Vimeo 在它们自己的播放器中播放；其他链接必须直接指向视频文件（.mp4…）。",
        "YouTube와 Vimeo는 자체 플레이어로 재생돼요. 다른 링크는 동영상 파일 자체(.mp4…)여야 해요.",
        "YouTube と Vimeo はそれぞれのプレーヤーで再生されます。その他のリンクは動画そのもの（.mp4…）を指す必要があります。"
    )
    val notALink get() = t("That doesn't look like a link", "Questo non sembra un link", "Das sieht nicht wie ein Link aus", "Ceci ne ressemble pas à un lien", "Eso no parece un enlace", "这看起来不像链接", "링크가 아닌 것 같아요", "リンクではないようです")
    val play get() = t("Play", "Riproduci", "Abspielen", "Lire", "Reproducir", "播放", "재생", "再生")
    val pause get() = t("Pause", "Pausa", "Pause", "Pause", "Pausa", "暂停", "일시정지", "一時停止")
    val cantPlay get() = t("This video can't be played here", "Questo video non si riesce a vedere qui", "Dieses Video kann hier nicht abgespielt werden", "Cette vidéo ne peut pas être lue ici", "Este vídeo no se puede reproducir aquí", "无法在此播放该视频", "이 동영상은 여기서 재생할 수 없어요", "この動画はここでは再生できません")
    val cantPlayAudio get() = t("This audio can't be played here", "Questo audio non si riesce a sentire qui", "Dieses Audio kann hier nicht abgespielt werden", "Cet audio ne peut pas être lu ici", "Este audio no se puede reproducir aquí", "无法在此播放该音频", "이 오디오는 여기서 재생할 수 없어요", "この音声はここでは再生できません")
    val openInBrowser get() = t("Open in browser or app", "Apri nel browser o nell'app", "Im Browser oder in der App öffnen", "Ouvrir dans le navigateur ou l'app", "Abrir en el navegador o la app", "在浏览器或应用中打开", "브라우저나 앱에서 열기", "ブラウザまたはアプリで開く")
    val openWith get() = t("Open with…", "Apri con…", "Öffnen mit…", "Ouvrir avec…", "Abrir con…", "打开方式…", "다음으로 열기…", "アプリで開く…")
    val noAppToOpen get() = t("No app can open it", "Nessuna app lo sa aprire", "Keine App kann es öffnen", "Aucune app ne peut l'ouvrir", "Ninguna app puede abrirlo", "没有可打开它的应用", "열 수 있는 앱이 없어요", "開けるアプリがありません")
    val fileReadFailed get() = t("Couldn't read that file", "Non sono riuscito a leggere il file", "Datei konnte nicht gelesen werden", "Impossible de lire ce fichier", "No se pudo leer el archivo", "无法读取该文件", "파일을 읽을 수 없어요", "ファイルを読み込めませんでした")

    // --- File e segnalibri ---

    val addFile get() = t("Upload or embed a file", "Carica o incorpora un file", "Datei hochladen oder einbetten", "Importer ou intégrer un fichier", "Subir o insertar un archivo", "上传或嵌入文件", "파일 업로드 또는 삽입", "ファイルをアップロードまたは埋め込む")
    val embedLink get() = t("Embed link", "Incorpora il link", "Link einbetten", "Intégrer le lien", "Insertar enlace", "嵌入链接", "링크 삽입", "リンクを埋め込む")
    val addBookmark get() = t("Add a web bookmark", "Aggiungi un segnalibro web", "Web-Lesezeichen hinzufügen", "Ajouter un signet web", "Añadir un marcador web", "添加网页书签", "웹 북마크 추가", "Webブックマークを追加")
    val createBookmark get() = t("Create bookmark", "Crea il segnalibro", "Lesezeichen erstellen", "Créer le signet", "Crear marcador", "创建书签", "북마크 만들기", "ブックマークを作成")
    val bookmarkHint get() = t(
        "Create a visual bookmark from a link.",
        "Crea un segnalibro con l'anteprima del sito.",
        "Erstellt ein Lesezeichen mit Vorschau aus einem Link.",
        "Créez un signet visuel à partir d'un lien.",
        "Crea un marcador visual a partir de un enlace.",
        "从链接创建可视书签。",
        "링크로 미리보기가 있는 북마크를 만들어요.",
        "リンクからプレビュー付きのブックマークを作成します。"
    )

    // --- Codice ---

    val copyCode get() = t("Copy code", "Copia il codice", "Code kopieren", "Copier le code", "Copiar código", "复制代码", "코드 복사", "コードをコピー")
    val codeCopied get() = t("Code copied", "Codice copiato", "Code kopiert", "Code copié", "Código copiado", "代码已复制", "코드를 복사했습니다", "コードをコピーしました")
    val searchLanguage get() = t("Search for a language…", "Cerca un linguaggio…", "Sprache suchen…", "Rechercher un langage…", "Buscar un lenguaje…", "搜索语言…", "언어 검색…", "言語を検索…")
}
