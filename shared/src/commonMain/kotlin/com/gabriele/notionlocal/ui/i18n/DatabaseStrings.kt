package com.gabriele.notionlocal.ui.i18n

import com.gabriele.notionlocal.data.entity.TimeFormatKind
import com.gabriele.notionlocal.data.entity.CalendarMode
import com.gabriele.notionlocal.data.entity.Calculation
import com.gabriele.notionlocal.data.entity.CalculationGroup
import com.gabriele.notionlocal.data.entity.NumberFormatKind
import com.gabriele.notionlocal.data.entity.DurationUnit
import com.gabriele.notionlocal.data.entity.CellOverflow
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.GalleryCardPreview
import com.gabriele.notionlocal.data.entity.GalleryCardSize
import com.gabriele.notionlocal.data.entity.RecurrenceFreq
import com.gabriele.notionlocal.data.entity.TimelineZoom
import com.gabriele.notionlocal.data.settings.AppLanguage
import com.gabriele.notionlocal.data.settings.AppSettings
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Le voci dei database: tipi di proprietà, viste, filtri, ordinamento,
 * tag, date che si ripetono, impostazioni. Stessa regola di `Strings`.
 */
object DbStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    // --- Nomi di tipi, viste, zoom ---

    /** Il nome inglese di un tipo: è quello che si salva quando una proprietà nasce senza nome. */
    fun englishTypeName(type: ColumnType): String = when (type) {
        ColumnType.TEXT -> "Text"
        ColumnType.NUMBER -> "Number"
        ColumnType.SELECT -> "Select"
        ColumnType.MULTI_SELECT -> "Multi-select"
        ColumnType.DATE -> "Date"
        ColumnType.DURATION -> "Duration"
        ColumnType.CHECKBOX -> "Checkbox"
        ColumnType.COLOR -> "Color cell"
        ColumnType.URL -> "URL"
        ColumnType.EMAIL -> "Email"
        ColumnType.PHONE -> "Phone"
        ColumnType.PAGE_LINK -> "Page link"
        ColumnType.RELATION -> "Relation"
        ColumnType.ROLLUP -> "Rollup"
        ColumnType.IMAGE -> "Image"
        ColumnType.CREATED_TIME -> "Created time"
        ColumnType.LAST_EDITED_TIME -> "Last edited time"
        ColumnType.BUTTON -> "Button"
        ColumnType.FORMULA -> "Formula"
    }

    fun typeName(type: ColumnType): String = when (type) {
        ColumnType.TEXT -> t("Text", "Testo", "Text", "Texte", "Texto", "文本", "텍스트", "テキスト")
        ColumnType.NUMBER -> t("Number", "Numero", "Zahl", "Nombre", "Número", "数字", "숫자", "数値")
        ColumnType.SELECT -> t("Select", "Selezione", "Auswahl", "Sélection", "Selección", "单选", "선택", "セレクト")
        ColumnType.MULTI_SELECT -> t("Multi-select", "Selezione multipla", "Mehrfachauswahl", "Sélection multiple", "Selección múltiple", "多选", "다중 선택", "マルチセレクト")
        ColumnType.DATE -> t("Date", "Data", "Datum", "Date", "Fecha", "日期", "날짜", "日付")
        ColumnType.DURATION -> t("Duration", "Durata", "Dauer", "Durée", "Duración", "时长", "기간", "期間")
        ColumnType.CHECKBOX -> t("Checkbox", "Casella", "Kontrollkästchen", "Case à cocher", "Casilla", "复选框", "체크박스", "チェックボックス")
        ColumnType.COLOR -> t("Color cell", "Cella colorata", "Farbzelle", "Cellule colorée", "Celda de color", "颜色单元格", "색상 셀", "色付きセル")
        ColumnType.URL -> t("URL", "URL", "URL", "URL", "URL", "网址", "URL", "URL")
        ColumnType.EMAIL -> t("Email", "Email", "E-Mail", "E-mail", "Correo", "电子邮件", "이메일", "メール")
        ColumnType.PHONE -> t("Phone", "Telefono", "Telefon", "Téléphone", "Teléfono", "电话", "전화", "電話")
        ColumnType.PAGE_LINK -> t("Page link", "Collegamento a pagina", "Seitenlink", "Lien de page", "Enlace a página", "页面链接", "페이지 링크", "ページリンク")
        ColumnType.RELATION -> t("Relation", "Relazione", "Relation", "Relation", "Relación", "关联", "관계형", "リレーション")
        ColumnType.ROLLUP -> t("Rollup", "Rollup", "Rollup", "Rollup", "Rollup", "汇总", "롤업", "ロールアップ")
        ColumnType.IMAGE -> t("Image", "Immagine", "Bild", "Image", "Imagen", "图片", "이미지", "画像")
        ColumnType.CREATED_TIME -> t("Created time", "Data di creazione", "Erstellt am", "Date de création", "Fecha de creación", "创建时间", "생성 일시", "作成日時")
        ColumnType.LAST_EDITED_TIME -> t("Last edited time", "Ultima modifica", "Zuletzt bearbeitet", "Dernière modification", "Última edición", "上次编辑时间", "마지막 편집 일시", "最終更新日時")
        ColumnType.BUTTON -> t("Button", "Pulsante", "Schaltfläche", "Bouton", "Botón", "按钮", "버튼", "ボタン")
        ColumnType.FORMULA -> t("Formula", "Formula", "Formel", "Formule", "Fórmula", "公式", "수식", "数式")
    }

    /**
     * Il nome di una proprietà come si mostra.
     *
     * Una proprietà lasciata senza nome prende quello inglese del suo tipo
     * ("Number", "Date"): quel nome l'ha scritto l'app, non l'utente, e si
     * traduce. Qualunque altro nome è dell'utente e resta com'è — anche se
     * per caso fosse "Date" scritto a mano: non c'è modo di distinguerli,
     * ed è lo stesso nome che l'app avrebbe dato.
     */
    fun columnName(stored: String): String =
        ColumnType.entries.firstOrNull { englishTypeName(it) == stored }?.let { typeName(it) } ?: stored

    fun layoutName(layout: DatabaseLayout): String = when (layout) {
        DatabaseLayout.TABLE -> t("Table", "Tabella", "Tabelle", "Tableau", "Tabla", "表格", "표", "テーブル")
        DatabaseLayout.BOARD -> t("Board", "Bacheca", "Board", "Kanban", "Tablero", "看板", "보드", "ボード")
        DatabaseLayout.CALENDAR -> t("Calendar", "Calendario", "Kalender", "Calendrier", "Calendario", "日历", "캘린더", "カレンダー")
        DatabaseLayout.TIMELINE -> t("Timeline", "Linea temporale", "Zeitleiste", "Chronologie", "Cronograma", "时间线", "타임라인", "タイムライン")
        DatabaseLayout.LIST -> t("List", "Elenco", "Liste", "Liste", "Lista", "列表", "목록", "リスト")
        DatabaseLayout.GALLERY -> t("Gallery", "Galleria", "Galerie", "Galerie", "Galería", "画廊", "갤러리", "ギャラリー")
        DatabaseLayout.CHART -> t("Chart", "Grafico", "Diagramm", "Graphique", "Gráfico", "图表", "차트", "グラフ")
    }

    fun galleryPreviewName(preview: GalleryCardPreview): String = when (preview) {
        GalleryCardPreview.NONE -> none
        GalleryCardPreview.PAGE_COVER -> t("Page cover", "Copertina della pagina", "Titelbild der Seite", "Couverture de la page", "Portada de la página", "页面封面", "페이지 커버", "ページカバー")
        GalleryCardPreview.PAGE_CONTENT -> t("Page content", "Contenuto della pagina", "Seiteninhalt", "Contenu de la page", "Contenido de la página", "页面内容", "페이지 콘텐츠", "ページコンテンツ")
    }

    // Al femminile dove la lingua lo chiede: si parla di "scheda",
    // "carte", "tarjeta".
    fun gallerySizeName(size: GalleryCardSize): String = when (size) {
        GalleryCardSize.SMALL -> t("Small", "Piccola", "Klein", "Petite", "Pequeña", "小", "작게", "小")
        GalleryCardSize.MEDIUM -> t("Medium", "Media", "Mittel", "Moyenne", "Mediana", "中", "중간", "中")
        GalleryCardSize.LARGE -> t("Large", "Grande", "Groß", "Grande", "Grande", "大", "크게", "大")
    }

    fun calendarModeName(mode: CalendarMode): String = when (mode) {
        CalendarMode.YEAR -> year
        CalendarMode.MONTH -> month
        CalendarMode.WEEK -> week
        CalendarMode.DAY -> day
    }

    // Il time-blocking come Google Calendar (dal 30/09/2026): la finestra
    // rapida di un evento e quella di "More options".
    val addTitle get() = t("Add title", "Aggiungi un titolo", "Titel hinzufügen", "Ajouter un titre", "Añadir título", "添加标题", "제목 추가", "タイトルを追加")
    val moreOptions get() = t("More options", "Altre opzioni", "Weitere Optionen", "Plus d'options", "Más opciones", "更多选项", "옵션 더보기", "その他のオプション")
    val allDay get() = t("All day", "Tutto il giorno", "Ganztägig", "Toute la journée", "Todo el día", "全天", "종일", "終日")
    val noTitle get() = t("(No title)", "(Senza titolo)", "(Kein Titel)", "(Sans titre)", "(Sin título)", "（无标题）", "(제목 없음)", "(タイトルなし)")
    val editEvent get() = t("Edit event", "Modifica evento", "Termin bearbeiten", "Modifier l'événement", "Editar evento", "编辑活动", "일정 수정", "予定を編集")
    val defaultColor get() = t("Default", "Predefinito", "Standard", "Par défaut", "Predeterminado", "默认", "기본", "デフォルト")

    fun zoomName(zoom: TimelineZoom): String = when (zoom) {
        TimelineZoom.HOURS -> t("Hours", "Ore", "Stunden", "Heures", "Horas", "小时", "시간", "時間")
        TimelineZoom.DAY -> day
        TimelineZoom.WEEK -> week
        TimelineZoom.BI_WEEK -> t("Bi-week", "Due settimane", "Zwei Wochen", "Deux semaines", "Dos semanas", "双周", "2주", "2週間")
        TimelineZoom.MONTH -> month
        TimelineZoom.QUARTER -> t("Quarter", "Trimestre", "Quartal", "Trimestre", "Trimestre", "季度", "분기", "四半期")
        TimelineZoom.YEAR -> year
    }

    val year get() = t("Year", "Anno", "Jahr", "Année", "Año", "年", "년", "年")
    val month get() = t("Month", "Mese", "Monat", "Mois", "Mes", "月", "월", "月")
    val week get() = t("Week", "Settimana", "Woche", "Semaine", "Semana", "周", "주", "週")
    val day get() = t("Day", "Giorno", "Tag", "Jour", "Día", "日", "일", "日")

    /** Il nome di un colore dei tag. */
    fun tagColorName(english: String): String = when (english) {
        "Default" -> t("Default", "Predefinito", "Standard", "Par défaut", "Predeterminado", "默认", "기본", "デフォルト")
        "Gray" -> t("Gray", "Grigio", "Grau", "Gris", "Gris", "灰色", "회색", "グレー")
        "Brown" -> t("Brown", "Marrone", "Braun", "Marron", "Marrón", "棕色", "갈색", "ブラウン")
        "Orange" -> t("Orange", "Arancione", "Orange", "Orange", "Naranja", "橙色", "주황색", "オレンジ")
        "Yellow" -> t("Yellow", "Giallo", "Gelb", "Jaune", "Amarillo", "黄色", "노란색", "イエロー")
        "Green" -> t("Green", "Verde", "Grün", "Vert", "Verde", "绿色", "초록색", "グリーン")
        "Blue" -> t("Blue", "Blu", "Blau", "Bleu", "Azul", "蓝色", "파란색", "ブルー")
        "Purple" -> t("Purple", "Viola", "Lila", "Violet", "Morado", "紫色", "보라색", "パープル")
        "Pink" -> t("Pink", "Rosa", "Rosa", "Rose", "Rosa", "粉色", "분홍색", "ピンク")
        "Red" -> t("Red", "Rosso", "Rot", "Rouge", "Rojo", "红色", "빨간색", "レッド")
        else -> english
    }

    // --- Contenuto ---

    val name get() = t("Name", "Nome", "Name", "Nom", "Nombre", "名称", "이름", "名前")

    /** Il nome della proprietà principale: quello scelto per il database, o "Name". */
    fun nameColumn(custom: String?): String = custom?.trim()?.takeIf { it.isNotEmpty() } ?: name
    val renameProperty get() = t("Rename", "Rinomina", "Umbenennen", "Renommer", "Cambiar nombre", "重命名", "이름 바꾸기", "名前を変更")
    val renameMainProperty get() = t("Rename the main property", "Rinomina la proprietà principale", "Haupteigenschaft umbenennen", "Renommer la propriété principale", "Cambiar nombre de la propiedad principal", "重命名主属性", "기본 속성 이름 바꾸기", "メインプロパティの名前を変更")
    val mainPropertyHint get() = t("Empty: back to \"Name\"", "Vuoto: torna \"Name\"", "Leer: wieder \"Name\"", "Vide : de nouveau \"Name\"", "Vacío: vuelve a \"Name\"", "留空：恢复为“Name”", "비우면 \"Name\"으로", "空欄で「Name」に戻る")
    val none get() = t("None", "Nessuno", "Keine", "Aucun", "Ninguno", "无", "없음", "なし")
    val newPage get() = t("New page", "Nuova pagina", "Neue Seite", "Nouvelle page", "Nueva página", "新建页面", "새 페이지", "新規ページ")
    // Database semplice: le righe non sono pagine, e i pulsanti lo dicono.
    val newRow get() = t("New row", "Nuova riga", "Neue Zeile", "Nouvelle ligne", "Nueva fila", "新建行", "새 행", "新規行")
    /** Sotto le prime righe di un database dentro una pagina (dal 18/10/2026, come su Notion). */
    val loadMore get() = t("Load more", "Carica altre", "Mehr laden", "Charger plus", "Cargar más", "加载更多", "더 불러오기", "さらに読み込む")
    val row get() = t("Row", "Riga", "Zeile", "Ligne", "Fila", "行", "행", "行")
    val simpleDatabaseNote get() = t(
        "Simple database: rows are just text, they don't open as pages.",
        "Database semplice: le righe sono solo testo, non si aprono come pagine.",
        "Einfache Datenbank: Zeilen sind nur Text und öffnen sich nicht als Seiten.",
        "Base de données simple : les lignes sont du texte, elles ne s'ouvrent pas comme des pages.",
        "Base de datos simple: las filas son solo texto, no se abren como páginas.",
        "简单数据库：各行只是文本，不会作为页面打开。",
        "단순 데이터베이스: 행은 텍스트일 뿐이며 페이지로 열리지 않습니다.",
        "シンプルデータベース：行はテキストのみで、ページとしては開きません。"
    )
    val newShort get() = t("New", "Nuova", "Neu", "Nouvelle", "Nueva", "新建", "새로 만들기", "新規")
    val open get() = t("OPEN", "APRI", "ÖFFNEN", "OUVRIR", "ABRIR", "打开", "열기", "開く")
    val deletePageTitle get() = t("Delete this page?", "Eliminare questa pagina?", "Diese Seite löschen?", "Supprimer cette page ?", "¿Eliminar esta página?", "删除此页面？", "이 페이지를 삭제할까요?", "このページを削除しますか？")
    val deleteRowText get() = t(
        "The row and its values will be deleted. This can't be undone.",
        "La riga e i suoi valori verranno eliminati. Non si può annullare.",
        "Die Zeile und ihre Werte werden gelöscht. Das kann nicht rückgängig gemacht werden.",
        "La ligne et ses valeurs seront supprimées. Action irréversible.",
        "Se eliminarán la fila y sus valores. No se puede deshacer.",
        "该行及其值将被删除，且无法撤销。",
        "행과 값이 삭제됩니다. 되돌릴 수 없습니다.",
        "行とその値が削除されます。元に戻すことはできません。"
    )
    val expandGroup get() = t("Expand group", "Espandi gruppo", "Gruppe aufklappen", "Déplier le groupe", "Expandir grupo", "展开分组", "그룹 펼치기", "グループを展開")
    val collapseGroup get() = t("Collapse group", "Comprimi gruppo", "Gruppe zuklappen", "Replier le groupe", "Contraer grupo", "折叠分组", "그룹 접기", "グループを折りたたむ")
    // --- "Color cell" ---

    val colorCellTextHint get() = t("Text (optional)", "Testo (facoltativo)", "Text (optional)", "Texte (facultatif)", "Texto (opcional)", "文本（可选）", "텍스트(선택)", "テキスト（任意）")
    val cellColor get() = t("Color", "Colore", "Farbe", "Couleur", "Color", "颜色", "색상", "色")
    val noCellColor get() = t("No color", "Nessun colore", "Keine Farbe", "Aucune couleur", "Sin color", "无颜色", "색상 없음", "色なし")

    fun noValue(property: String) = t("No $property", "Senza $property", "Ohne $property", "Sans $property", "Sin $property", "无$property", "$property 없음", "$property なし")
    val checked get() = t("Checked", "Spuntata", "Abgehakt", "Cochée", "Marcada", "已勾选", "선택됨", "チェック済み")
    val unchecked get() = t("Unchecked", "Non spuntata", "Nicht abgehakt", "Non cochée", "Sin marcar", "未勾选", "선택 안 됨", "未チェック")
    val previous get() = t("Previous", "Precedente", "Zurück", "Précédent", "Anterior", "上一个", "이전", "前へ")
    val next get() = t("Next", "Successivo", "Weiter", "Suivant", "Siguiente", "下一个", "다음", "次へ")
    val today get() = t("Today", "Oggi", "Heute", "Aujourd'hui", "Hoy", "今天", "오늘", "今日")
    /** Il calendario sul PC (dal 13/11/2026): un giorno con più di cinque eventi. */
    val showMore get() = t("Show more", "Mostra altro", "Mehr anzeigen", "Afficher plus", "Mostrar más", "显示更多", "더 보기", "もっと見る")
    val showLess get() = t("Show less", "Mostra meno", "Weniger anzeigen", "Afficher moins", "Mostrar menos", "收起", "간략히 보기", "表示を減らす")
    fun noDate(count: Int) = t("No date ($count)", "Senza data ($count)", "Ohne Datum ($count)", "Sans date ($count)", "Sin fecha ($count)", "无日期（$count）", "날짜 없음 ($count)", "日付なし（$count）")

    val boardNeedsSelect get() = t(
        "A board puts each page in a column, so it needs a Select property to group by.",
        "Una bacheca mette ogni pagina in una colonna, quindi le serve una proprietà Selezione per raggruppare.",
        "Ein Board ordnet jede Seite einer Spalte zu und braucht dafür eine Auswahl-Eigenschaft zum Gruppieren.",
        "Un tableau Kanban place chaque page dans une colonne : il lui faut une propriété Sélection pour regrouper.",
        "Un tablero coloca cada página en una columna, así que necesita una propiedad Selección para agrupar.",
        "看板会把每个页面放进一列，因此需要一个“单选”属性来分组。",
        "보드는 각 페이지를 열에 배치하므로 그룹화할 선택 속성이 필요합니다.",
        "ボードは各ページを列に配置するため、グループ化に使うセレクトプロパティが必要です。"
    )
    val addSelectProperty get() = t("Add a Select property", "Aggiungi una proprietà Selezione", "Auswahl-Eigenschaft hinzufügen", "Ajouter une propriété Sélection", "Añadir una propiedad Selección", "添加单选属性", "선택 속성 추가", "セレクトプロパティを追加")
    val timelineNeedsDate get() = t(
        "A timeline stretches each page over the days it covers, so it needs a Date property.",
        "Una linea temporale distende ogni pagina sui giorni che copre, quindi le serve una proprietà Data.",
        "Eine Zeitleiste zieht jede Seite über die Tage, die sie umfasst, und braucht daher eine Datumseigenschaft.",
        "Une chronologie étale chaque page sur les jours qu'elle couvre : il lui faut une propriété Date.",
        "Un cronograma extiende cada página por los días que abarca, así que necesita una propiedad Fecha.",
        "时间线会把每个页面铺开到它覆盖的日期上，因此需要一个日期属性。",
        "타임라인은 각 페이지를 해당 기간에 걸쳐 표시하므로 날짜 속성이 필요합니다.",
        "タイムラインは各ページを期間にわたって表示するため、日付プロパティが必要です。"
    )
    val addDateProperty get() = t("Add a Date property", "Aggiungi una proprietà Data", "Datumseigenschaft hinzufügen", "Ajouter une propriété Date", "Añadir una propiedad Fecha", "添加日期属性", "날짜 속성 추가", "日付プロパティを追加")
    val noPageHasDate get() = t(
        "No page has a date yet. Give one a date and it will show up here as a bar.",
        "Nessuna pagina ha ancora una data. Dagliene una e comparirà qui come una barra.",
        "Noch keine Seite hat ein Datum. Gib einer Seite ein Datum, dann erscheint sie hier als Balken.",
        "Aucune page n'a encore de date. Donnez-en une à une page et elle apparaîtra ici sous forme de barre.",
        "Ninguna página tiene fecha todavía. Ponle una a alguna y aparecerá aquí como una barra.",
        "还没有页面设置日期。为页面设置日期后，它会在这里显示为一条横条。",
        "아직 날짜가 있는 페이지가 없습니다. 페이지에 날짜를 지정하면 여기에 막대로 표시됩니다.",
        "日付のあるページはまだありません。日付を設定すると、ここにバーとして表示されます。"
    )
    val selectZoomLevel get() = t("Select a zoom level", "Scegli un livello di zoom", "Zoomstufe wählen", "Choisir un niveau de zoom", "Elegir nivel de zoom", "选择缩放级别", "확대 수준 선택", "ズームレベルを選択")

    // --- Barra della vista ---

    val changeLayout get() = t("Change layout", "Cambia vista", "Ansicht ändern", "Changer de vue", "Cambiar vista", "更改视图", "보기 변경", "ビューを変更")
    val filter get() = t("Filter", "Filtro", "Filter", "Filtre", "Filtro", "筛选", "필터", "フィルター")
    val sort get() = t("Sort", "Ordina", "Sortieren", "Trier", "Ordenar", "排序", "정렬", "並べ替え")
    // Le viste collegate ("Linked view of data source").
    val openSourceDatabase get() = t("Open the source database", "Apri il database di origine", "Quelldatenbank öffnen", "Ouvrir la base de données source", "Abrir la base de datos de origen", "打开源数据库", "원본 데이터베이스 열기", "元のデータベースを開く")
    val linkedSourceMissing get() = t(
        "The database shown here was deleted or is in the trash.",
        "Il database mostrato qui è stato cancellato o è nel cestino.",
        "Die hier gezeigte Datenbank wurde gelöscht oder liegt im Papierkorb.",
        "La base de données affichée ici a été supprimée ou est dans la corbeille.",
        "La base de datos que se mostraba aquí se eliminó o está en la papelera.",
        "此处显示的数据库已被删除或在回收站中。",
        "여기에 표시되던 데이터베이스가 삭제되었거나 휴지통에 있습니다.",
        "ここに表示していたデータベースは削除されたか、ゴミ箱にあります。"
    )
    val linkedViewTitle get() = t("Linked view of data source", "Vista collegata di un database", "Verknüpfte Ansicht einer Datenquelle", "Vue liée d'une source de données", "Vista vinculada de una fuente de datos", "数据源的关联视图", "데이터 소스의 연결된 보기", "データソースのリンクビュー")
    val linkedViewHint get() = t(
        "Choose the database to show here. It stays the same database: pages, properties and everything written inside are in sync. Filters, sorting, grouping and hidden properties are this view's own.",
        "Scegli il database da mostrare qui. Resta lo stesso database: pagine, proprietà e tutto quello che c'è scritto sono sincronizzati. Filtri, ordinamento, raggruppamento e proprietà nascoste sono solo di questa vista.",
        "Wähle die Datenbank, die hier angezeigt wird. Es bleibt dieselbe Datenbank: Seiten, Eigenschaften und Inhalte sind synchron. Filter, Sortierung, Gruppierung und ausgeblendete Eigenschaften gehören nur zu dieser Ansicht.",
        "Choisissez la base de données à afficher ici. C'est la même base : pages, propriétés et contenu restent synchronisés. Filtres, tri, regroupement et propriétés masquées sont propres à cette vue.",
        "Elige la base de datos que se mostrará aquí. Sigue siendo la misma: páginas, propiedades y contenido están sincronizados. Filtros, orden, agrupación y propiedades ocultas son solo de esta vista.",
        "选择要在此显示的数据库。它仍是同一个数据库：页面、属性和内容都保持同步。筛选、排序、分组和隐藏的属性只属于此视图。",
        "여기에 표시할 데이터베이스를 선택하세요. 같은 데이터베이스이므로 페이지, 속성, 내용이 모두 동기화됩니다. 필터, 정렬, 그룹, 숨긴 속성은 이 보기에만 적용됩니다.",
        "ここに表示するデータベースを選んでください。同じデータベースなので、ページ、プロパティ、内容はすべて同期されます。フィルター、並べ替え、グループ化、非表示のプロパティはこのビューだけのものです。"
    )
    val noDatabasesToLink get() = t("There are no databases yet.", "Non ci sono ancora database.", "Es gibt noch keine Datenbanken.", "Il n'y a pas encore de base de données.", "Todavía no hay bases de datos.", "还没有数据库。", "아직 데이터베이스가 없습니다.", "まだデータベースがありません。")
    val removeLinkedViewTitle get() = t("Remove this linked view?", "Togliere questa vista collegata?", "Diese verknüpfte Ansicht entfernen?", "Retirer cette vue liée ?", "¿Quitar esta vista vinculada?", "移除此关联视图？", "이 연결된 보기를 제거할까요?", "このリンクビューを削除しますか？")
    val removeLinkedViewText get() = t(
        "Only this view goes away: the database and its pages stay where they are.",
        "Se ne va solo questa vista: il database e le sue pagine restano dove sono.",
        "Nur diese Ansicht verschwindet: Die Datenbank und ihre Seiten bleiben, wo sie sind.",
        "Seule cette vue disparaît : la base de données et ses pages restent où elles sont.",
        "Solo desaparece esta vista: la base de datos y sus páginas se quedan donde están.",
        "只会移除此视图：数据库及其页面保留在原处。",
        "이 보기만 사라집니다. 데이터베이스와 페이지는 그대로 남습니다.",
        "このビューだけが消えます。データベースとそのページはそのまま残ります。"
    )

    // La ricerca dentro il database (la lente accanto a Sort).
    val searchInDatabase get() = t("Search this database", "Cerca in questo database", "Diese Datenbank durchsuchen", "Rechercher dans cette base de données", "Buscar en esta base de datos", "在此数据库中搜索", "이 데이터베이스에서 검색", "このデータベースを検索")
    val closeSearch get() = t("Close search", "Chiudi la ricerca", "Suche schließen", "Fermer la recherche", "Cerrar búsqueda", "关闭搜索", "검색 닫기", "検索を閉じる")
    val openFullPage get() = t("Open as full page", "Apri a pagina intera", "Als ganze Seite öffnen", "Ouvrir en pleine page", "Abrir a página completa", "以整页打开", "전체 페이지로 열기", "フルページで開く")
    val databaseSettings get() = t("Database settings", "Impostazioni del database", "Datenbankeinstellungen", "Paramètres de la base de données", "Ajustes de la base de datos", "数据库设置", "데이터베이스 설정", "データベースの設定")
    val removeSort get() = t("Remove sort", "Rimuovi ordinamento", "Sortierung entfernen", "Supprimer le tri", "Quitar orden", "移除排序", "정렬 제거", "並べ替えを解除")
    // Le regole di ordinamento, quante se ne vogliono (dal 17/10/2026).
    val sortBy get() = t("Sort by", "Ordina per", "Sortieren nach", "Trier par", "Ordenar por", "排序依据", "정렬 기준", "並べ替えの基準")
    val sortRules get() = t("Sort rules", "Regole di ordinamento", "Sortierregeln", "Règles de tri", "Reglas de orden", "排序规则", "정렬 규칙", "並べ替えルール")
    val addSortRule get() = t("Add a sort rule", "Aggiungi una regola", "Sortierregel hinzufügen", "Ajouter une règle de tri", "Añadir una regla de orden", "添加排序规则", "정렬 규칙 추가", "並べ替えルールを追加")
    val noSortRules get() = t("No sort rules yet", "Ancora nessuna regola", "Noch keine Sortierregeln", "Aucune règle de tri pour l'instant", "Aún no hay reglas de orden", "还没有排序规则", "아직 정렬 규칙이 없어요", "並べ替えルールはまだありません")
    val removeAllSorts get() = t("Remove all sorts", "Togli tutti gli ordinamenti", "Alle Sortierungen entfernen", "Supprimer tous les tris", "Quitar todos los órdenes", "移除所有排序", "모든 정렬 제거", "すべての並べ替えを解除")
    // "Sort" nel menu di una proprietà (dal 02/11/2026): le due scelte, con le parole dei suoi valori.
    val sortAToZ get() = t("A → Z", "A → Z", "A → Z", "A → Z", "A → Z", "A → Z", "A → Z", "A → Z")
    val sortZToA get() = t("Z → A", "Z → A", "Z → A", "Z → A", "Z → A", "Z → A", "Z → A", "Z → A")
    val largestFirst get() = t("Largest → smallest", "Dal più grande al più piccolo", "Größte → kleinste", "Du plus grand au plus petit", "De mayor a menor", "从大到小", "큰 값부터", "大きい順")
    val smallestFirst get() = t("Smallest → largest", "Dal più piccolo al più grande", "Kleinste → größte", "Du plus petit au plus grand", "De menor a mayor", "从小到大", "작은 값부터", "小さい順")
    val checkedFirst get() = t("Checked first", "Prima le spuntate", "Angehakte zuerst", "Cochées d'abord", "Marcadas primero", "已勾选的在前", "체크된 항목부터", "チェック済みが先")
    val uncheckedFirst get() = t("Unchecked first", "Prima quelle senza spunta", "Nicht angehakte zuerst", "Non cochées d'abord", "Sin marcar primero", "未勾选的在前", "체크 안 된 항목부터", "未チェックが先")
    val sortRulesNote get() = t(
        "The first rule counts most: each one after it only orders the rows that are equal for the ones above.",
        "La prima regola conta di più: ognuna delle seguenti ordina solo le righe che per quelle sopra sono pari.",
        "Die erste Regel zählt am meisten: Jede weitere sortiert nur Zeilen, die bei den Regeln darüber gleich sind.",
        "La première règle compte le plus : chacune des suivantes ne trie que les lignes égales selon celles du dessus.",
        "La primera regla cuenta más: cada una de las siguientes solo ordena las filas iguales según las de arriba.",
        "第一条规则最优先：之后的每条规则只对按上面规则相同的行排序。",
        "첫 번째 규칙이 가장 우선이에요. 다음 규칙은 위 규칙에서 같은 행에만 적용돼요.",
        "最初のルールが最優先です。次のルールは、上のルールで同じになった行だけを並べ替えます。"
    )
    fun sortRuleCount(n: Int) = if (n == 1) {
        t("1 rule", "1 regola", "1 Regel", "1 règle", "1 regla", "1 条规则", "규칙 1개", "1 件のルール")
    } else {
        t("$n rules", "$n regole", "$n Regeln", "$n règles", "$n reglas", "$n 条规则", "규칙 ${n}개", "$n 件のルール")
    }

    // --- Filtro ---

    val removeFilter get() = t("Remove filter", "Rimuovi filtro", "Filter entfernen", "Supprimer le filtre", "Quitar filtro", "移除筛选", "필터 제거", "フィルターを解除")
    val filterNeedsProperty get() = t(
        "Filtering picks the pages whose property has a value you choose, so it needs a Select, Multi-select, Date, Number or Checkbox property.",
        "Il filtro sceglie le pagine la cui proprietà ha un valore a tua scelta, quindi serve una proprietà Selezione, Selezione multipla, Data, Numero o Casella.",
        "Der Filter wählt Seiten, deren Eigenschaft einen gewählten Wert hat, und braucht daher eine Eigenschaft vom Typ Auswahl, Mehrfachauswahl, Datum, Zahl oder Kontrollkästchen.",
        "Le filtre retient les pages dont la propriété a la valeur choisie : il faut donc une propriété Sélection, Sélection multiple, Date, Nombre ou Case à cocher.",
        "El filtro elige las páginas cuya propiedad tiene el valor que eliges, así que necesita una propiedad Selección, Selección múltiple, Fecha, Número o Casilla.",
        "筛选会挑出属性等于所选值的页面，因此需要单选、多选、日期、数字或复选框属性。",
        "필터는 속성 값이 선택한 값인 페이지를 고르므로 선택, 다중 선택, 날짜, 숫자 또는 체크박스 속성이 필요합니다.",
        "フィルターは、プロパティが選んだ値のページを抽出します。セレクト、マルチセレクト、日付、数値、チェックボックスのいずれかのプロパティが必要です。"
    )
    fun noOptionsYet(property: String) = t(
        "\"$property\" has no options yet, so there is nothing to filter by.",
        "\"$property\" non ha ancora opzioni, quindi non c'è niente da filtrare.",
        "„$property“ hat noch keine Optionen, daher gibt es nichts zu filtern.",
        "« $property » n'a pas encore d'options : il n'y a rien à filtrer.",
        "«$property» todavía no tiene opciones, así que no hay nada que filtrar.",
        "“$property”还没有选项，因此无可筛选。",
        "\"$property\"에 아직 옵션이 없어 필터링할 항목이 없습니다.",
        "「$property」にはまだ選択肢がないため、絞り込めるものがありません。"
    )
    val pagesWithAny get() = t("Pages with any of these", "Pagine con almeno uno di questi", "Seiten mit einem dieser Werte", "Pages ayant l'une de ces valeurs", "Páginas con alguno de estos", "包含其中任一项的页面", "다음 중 하나가 있는 페이지", "いずれかを含むページ")
    val pagesWithNumberRange get() = t("Pages with a number in this range", "Pagine con un numero in questo intervallo", "Seiten mit einer Zahl in diesem Bereich", "Pages dont le nombre est dans cet intervalle", "Páginas con un número en este rango", "数字在此范围内的页面", "이 범위의 숫자가 있는 페이지", "この範囲の数値を持つページ")
    val from get() = t("From", "Da", "Von", "De", "Desde", "从", "시작", "開始")
    val to get() = t("To", "A", "Bis", "À", "Hasta", "到", "끝", "終了")
    val numberRangeHint get() = t(
        "Leave one side empty for \"from here on\" or \"up to here\". Pages without a number never pass.",
        "Lascia vuoto un lato per \"da qui in poi\" o \"fino a qui\". Le pagine senza numero non passano mai.",
        "Lass eine Seite leer für „ab hier“ oder „bis hier“. Seiten ohne Zahl werden nie angezeigt.",
        "Laissez un côté vide pour « à partir d'ici » ou « jusqu'ici ». Les pages sans nombre ne sont jamais retenues.",
        "Deja un lado vacío para «desde aquí» o «hasta aquí». Las páginas sin número nunca pasan.",
        "留空一侧表示“从此开始”或“到此为止”。没有数字的页面永远不会显示。",
        "\"여기부터\" 또는 \"여기까지\"로 하려면 한쪽을 비워 두세요. 숫자가 없는 페이지는 표시되지 않습니다.",
        "「ここから」や「ここまで」にするには片方を空欄にします。数値のないページは表示されません。"
    )
    val pagesWithBox get() = t("Pages with the box like this", "Pagine con la casella così", "Seiten mit so gesetztem Kästchen", "Pages avec la case ainsi", "Páginas con la casilla así", "复选框为此状态的页面", "체크박스가 이 상태인 페이지", "チェックボックスがこの状態のページ")
    val pagesOnDays get() = t("Pages on these days", "Pagine in questi giorni", "Seiten an diesen Tagen", "Pages de ces jours", "Páginas en estos días", "这些日期的页面", "이 날짜의 페이지", "この日付のページ")
    val pickADay get() = t("Pick a day", "Scegli un giorno", "Tag wählen", "Choisir un jour", "Elige un día", "选择日期", "날짜 선택", "日を選択")
    val sameDay get() = t("Same day", "Stesso giorno", "Gleicher Tag", "Même jour", "Mismo día", "同一天", "같은 날", "同じ日")
    val filterSingleDay get() = t("Filter a single day", "Filtra un giorno solo", "Nur einen Tag filtern", "Filtrer un seul jour", "Filtrar un solo día", "只筛选一天", "하루만 필터", "1日だけ絞り込む")

    // --- Proprietà ---

    val editProperty get() = t("Edit property", "Modifica proprietà", "Eigenschaft bearbeiten", "Modifier la propriété", "Editar propiedad", "编辑属性", "속성 편집", "プロパティを編集")
    val alignLeft get() = t("Align left", "Allinea a sinistra", "Linksbündig", "Aligner à gauche", "Alinear a la izquierda", "左对齐", "왼쪽 정렬", "左揃え")
    val center get() = t("Center", "Centra", "Zentrieren", "Centrer", "Centrar", "居中", "가운데 정렬", "中央揃え")
    val alignRight get() = t("Align right", "Allinea a destra", "Rechtsbündig", "Aligner à droite", "Alinear a la derecha", "右对齐", "오른쪽 정렬", "右揃え")
    val hide get() = t("Hide", "Nascondi", "Ausblenden", "Masquer", "Ocultar", "隐藏", "숨기기", "非表示")
    val rowsFollowSort get() = t(
        "The rows follow the sort: remove it to move them by hand.",
        "Le righe seguono l'ordinamento: toglilo per spostarle a mano.",
        "Die Zeilen folgen der Sortierung: entferne sie, um sie von Hand zu verschieben.",
        "Les lignes suivent le tri : retirez-le pour les déplacer à la main.",
        "Las filas siguen el orden: quítalo para moverlas a mano.",
        "行按排序排列：取消排序后才能手动移动。",
        "행이 정렬을 따릅니다. 직접 옮기려면 정렬을 해제하세요.",
        "行は並べ替えに従います。手で動かすには並べ替えを外してください。"
    )
    val activateAll get() = t("Activate all", "Attiva tutte", "Alle aktivieren", "Tout activer", "Activar todas", "全部勾选", "모두 선택", "すべてオン")
    val deactivateAll get() = t("Deactivate all", "Disattiva tutte", "Alle deaktivieren", "Tout désactiver", "Desactivar todas", "全部取消勾选", "모두 해제", "すべてオフ")
    val addProperty get() = t("Add property", "Aggiungi proprietà", "Eigenschaft hinzufügen", "Ajouter une propriété", "Añadir propiedad", "添加属性", "속성 추가", "プロパティを追加")
    val newProperty get() = t("New property", "Nuova proprietà", "Neue Eigenschaft", "Nouvelle propriété", "Nueva propiedad", "新属性", "새 속성", "新しいプロパティ")
    val add get() = t("Add", "Aggiungi", "Hinzufügen", "Ajouter", "Añadir", "添加", "추가", "追加")
    val propertyName get() = t("Property name", "Nome della proprietà", "Name der Eigenschaft", "Nom de la propriété", "Nombre de la propiedad", "属性名称", "속성 이름", "プロパティ名")
    val type get() = t("Type", "Tipo", "Typ", "Type", "Tipo", "类型", "유형", "種類")
    val moveLeft get() = t("Move to left","Sposta a sinistra", "Nach links", "Déplacer à gauche", "Mover a la izquierda", "左移", "왼쪽으로 이동", "左へ移動")
    val moveRight get() = t("Move to right","Sposta a destra", "Nach rechts", "Déplacer à droite", "Mover a la derecha", "右移", "오른쪽으로 이동", "右へ移動")
    val deleteProperty get() = t("Delete property", "Elimina proprietà", "Eigenschaft löschen", "Supprimer la propriété", "Eliminar propiedad", "删除属性", "속성 삭제", "プロパティを削除")
    fun deletePropertyTitle(name: String) = t(
        "Delete \"$name\"?",
        "Eliminare \"$name\"?",
        "\"$name\" löschen?",
        "Supprimer « $name » ?",
        "¿Eliminar \"$name\"?",
        "删除“$name”？",
        "'$name'을(를) 삭제할까요?",
        "「$name」を削除しますか？"
    )
    val deletePropertyNote get() = t(
        "Its values in every row are deleted too.",
        "Se ne vanno anche i suoi valori in tutte le righe.",
        "Auch ihre Werte in allen Zeilen werden gelöscht.",
        "Ses valeurs dans toutes les lignes sont aussi supprimées.",
        "También se eliminan sus valores en todas las filas.",
        "所有行中它的值也会被删除。",
        "모든 행의 값도 함께 삭제돼요.",
        "すべての行のこのプロパティの値も削除されます。"
    )
    val noPropertiesYet get() = t("This database has no properties yet.", "Questo database non ha ancora proprietà.", "Diese Datenbank hat noch keine Eigenschaften.", "Cette base de données n'a pas encore de propriétés.", "Esta base de datos aún no tiene propiedades.", "此数据库还没有属性。", "이 데이터베이스에는 아직 속성이 없습니다.", "このデータベースにはまだプロパティがありません。")

    // --- Tag ---

    val tagName get() = t("Tag name", "Nome del tag", "Tag-Name", "Nom du tag", "Nombre de la etiqueta", "标签名称", "태그 이름", "タグ名")
    val selected get() = t("Selected", "Selezionato", "Ausgewählt", "Sélectionné", "Seleccionado", "已选择", "선택됨", "選択済み")
    val createATag get() = t("Create a tag", "Crea un tag", "Tag erstellen", "Créer un tag", "Crear una etiqueta", "创建标签", "태그 만들기", "タグを作成")
    val create get() = t("Create", "Crea", "Erstellen", "Créer", "Crear", "创建", "만들기", "作成")
    val selectExistingOption get() = t("Select an existing option", "Scegli un'opzione esistente", "Vorhandene Option wählen", "Choisir une option existante", "Elegir una opción existente", "选择现有选项", "기존 옵션 선택", "既存の選択肢を選ぶ")
    val noTagsYet get() = t(
        "No tags yet — write above to create the first one.",
        "Ancora nessun tag: scrivi qui sopra per creare il primo.",
        "Noch keine Tags – schreib oben, um den ersten zu erstellen.",
        "Aucun tag pour l'instant — écrivez ci-dessus pour créer le premier.",
        "Aún no hay etiquetas: escribe arriba para crear la primera.",
        "还没有标签——在上方输入即可创建第一个。",
        "아직 태그가 없습니다. 위에 입력해 첫 태그를 만드세요.",
        "タグはまだありません。上に入力して最初のタグを作成します。"
    )
    val noTagMatches get() = t("No tag matches what you wrote.", "Nessun tag corrisponde a quello che hai scritto.", "Kein Tag passt zu deiner Eingabe.", "Aucun tag ne correspond à ce que vous avez écrit.", "Ninguna etiqueta coincide con lo que escribiste.", "没有与输入内容匹配的标签。", "입력한 내용과 일치하는 태그가 없습니다.", "入力内容に一致するタグはありません。")
    val editTag get() = t("Edit tag", "Modifica tag", "Tag bearbeiten", "Modifier le tag", "Editar etiqueta", "编辑标签", "태그 편집", "タグを編集")
    val addSecondColor get() = t("Add a second color", "Aggiungi un secondo colore", "Zweite Farbe hinzufügen", "Ajouter une seconde couleur", "Añadir un segundo color", "添加第二种颜色", "두 번째 색상 추가", "2つ目の色を追加")
    val secondColor get() = t("Second color", "Secondo colore", "Zweite Farbe", "Seconde couleur", "Segundo color", "第二种颜色", "두 번째 색상", "2つ目の色")
    val lineBetweenColors get() = t("Line between colors", "Linea tra i colori", "Linie zwischen den Farben", "Ligne entre les couleurs", "Línea entre los colores", "颜色之间的分隔线", "색상 사이 구분선", "色の間に線")
    val removeSecondColor get() = t("Remove second color", "Togli il secondo colore", "Zweite Farbe entfernen", "Retirer la seconde couleur", "Quitar el segundo color", "移除第二种颜色", "두 번째 색상 제거", "2つ目の色を削除")
    val reorder get() = t("Reorder", "Riordina", "Neu ordnen", "Réorganiser", "Reordenar", "排序", "순서 변경", "並べ替え")
    val dragToReorder get() = t("Drag to reorder", "Trascina per riordinare", "Zum Neuordnen ziehen", "Faire glisser pour réorganiser", "Arrastra para reordenar", "拖动以排序", "끌어서 순서 변경", "ドラッグして並べ替え")

    // --- Data e ripetizioni ---

    val includeTime get() = t("Include time", "Includi l'ora", "Uhrzeit einschließen", "Inclure l'heure", "Incluir hora", "包含时间", "시간 포함", "時刻を含める")
    val includeNotification get() = t("Include notification", "Includi notifica", "Benachrichtigung einschließen", "Inclure une notification", "Incluir notificación", "包含通知", "알림 포함", "通知を含める")

    /** Quando suona la notifica di una data, detto sotto l'interruttore. */
    fun notificationWhen(hasTime: Boolean, hasEnd: Boolean): String = when {
        hasTime && hasEnd -> t("At the start and at the end", "All'inizio e alla fine", "Zu Beginn und am Ende", "Au début et à la fin", "Al inicio y al final", "在开始和结束时", "시작할 때와 끝날 때", "開始時と終了時")
        hasTime -> t("At the chosen time", "All'ora scelta", "Zur gewählten Uhrzeit", "À l'heure choisie", "A la hora elegida", "在所选时间", "선택한 시간에", "選んだ時刻に")
        hasEnd -> t("At 9:00 on the first and last day", "Alle 9:00 del primo e dell'ultimo giorno", "Um 9:00 am ersten und letzten Tag", "À 9:00 le premier et le dernier jour", "A las 9:00 el primer y el último día", "第一天和最后一天的 9:00", "첫날과 마지막 날 9:00에", "初日と最終日の 9:00 に")
        else -> t("At 9:00 on that day", "Alle 9:00 di quel giorno", "Um 9:00 an diesem Tag", "À 9:00 ce jour-là", "A las 9:00 de ese día", "当天 9:00", "그날 9:00에", "その日の 9:00 に")
    }
    val startDate get() = t("Start date", "Data di inizio", "Startdatum", "Date de début", "Fecha de inicio", "开始日期", "시작 날짜", "開始日")
    val startTime get() = t("Start time", "Ora di inizio", "Startzeit", "Heure de début", "Hora de inicio", "开始时间", "시작 시간", "開始時刻")
    val endDate get() = t("End date", "Data di fine", "Enddatum", "Date de fin", "Fecha de fin", "结束日期", "종료 날짜", "終了日")
    val endTime get() = t("End time", "Ora di fine", "Endzeit", "Heure de fin", "Hora de fin", "结束时间", "종료 시간", "終了時刻")
    val repeat get() = t("Repeat", "Ripeti", "Wiederholen", "Répéter", "Repetir", "重复", "반복", "繰り返し")
    val clear get() = t("Clear", "Cancella", "Löschen", "Effacer", "Borrar", "清除", "지우기", "クリア")
    val ok get() = t("OK", "OK", "OK", "OK", "Aceptar", "确定", "확인", "OK")
    // --- "Duration" (dal 29/09/2026) ---

    /** Quanto dura, a parole nella lingua dell'app: vedi `durationWords`. */
    fun durationText(units: List<Pair<DurationUnit, Long>>): String = durationWords(units, AppSettings.language)
    val durationChooseBoth get() = t("Choose the start and the end", "Scegli l'inizio e la fine", "Wähle Beginn und Ende", "Choisis le début et la fin", "Elige el inicio y el fin", "请选择开始和结束", "시작과 끝을 선택하세요", "開始と終了を選んでください")
    val durationEndBeforeStart get() = t("The end comes before the start", "La fine viene prima dell'inizio", "Das Ende liegt vor dem Beginn", "La fin précède le début", "El fin es anterior al inicio", "结束早于开始", "끝이 시작보다 앞섭니다", "終了が開始より前です")
    /** Fino a quale unità si scrive una durata: le scelte di "Show as". */
    fun durationFormatName(unit: DurationUnit): String = when (unit) {
        DurationUnit.YEARS -> t("Years", "Anni", "Jahre", "Années", "Años", "年", "년", "年")
        DurationUnit.MONTHS -> t("Years and months", "Anni e mesi", "Jahre und Monate", "Années et mois", "Años y meses", "年和月", "년, 개월", "年と月")
        DurationUnit.DAYS -> t("Years, months and days", "Anni, mesi e giorni", "Jahre, Monate und Tage", "Années, mois et jours", "Años, meses y días", "年、月、天", "년, 개월, 일", "年・月・日")
        DurationUnit.HOURS -> t("Years, months, days and hours", "Anni, mesi, giorni e ore", "Jahre, Monate, Tage und Stunden", "Années, mois, jours et heures", "Años, meses, días y horas", "年、月、天、小时", "년, 개월, 일, 시간", "年・月・日・時間")
        DurationUnit.MINUTES -> t("Years, months, days, hours and minutes", "Anni, mesi, giorni, ore e minuti", "Jahre, Monate, Tage, Stunden und Minuten", "Années, mois, jours, heures et minutes", "Años, meses, días, horas y minutos", "年、月、天、小时、分钟", "년, 개월, 일, 시간, 분", "年・月・日・時間・分")
    }

    // I selettori di data e ora come quelli di Samsung (vedi `SamsungPickers.kt`).
    /** "Color columns" (dal 30/09/2026): nelle impostazioni della bacheca. */
    val colorColumns get() = t("Color columns", "Colora le colonne", "Spalten einfärben", "Colorer les colonnes", "Colorear columnas", "为列着色", "열 색상 입히기", "列に色を付ける")
    /** "Long text" (dal 30/09/2026): nelle impostazioni di una tabella, cosa fa il testo che non ci sta. */
    val longText get() = t("Long text", "Testo lungo", "Langer Text", "Texte long", "Texto largo", "长文本", "긴 텍스트", "長いテキスト")
    fun cellOverflowName(mode: CellOverflow): String = when (mode) {
        CellOverflow.TRUNCATE -> t("Show …", "Tre puntini", "Mit … kürzen", "Afficher …", "Mostrar …", "显示 …", "… 표시", "… で省略")
        CellOverflow.WIDEN -> t("Widen the column", "Allarga la colonna", "Spalte verbreitern", "Élargir la colonne", "Ensanchar la columna", "加宽列", "열 넓히기", "列を広げる")
        CellOverflow.WRAP -> t("Wrap in the cell", "A capo nella cella", "In der Zelle umbrechen", "Retour à la ligne", "Ajustar en la celda", "在单元格内换行", "셀 안에서 줄바꿈", "セル内で折り返す")
    }
    val setDate get() = t("Set date", "Imposta data", "Datum festlegen", "Définir la date", "Establecer fecha", "设置日期", "날짜 설정", "日付を設定")
    val setTime get() = t("Set time", "Imposta ora", "Uhrzeit festlegen", "Définir l'heure", "Establecer hora", "设置时间", "시간 설정", "時刻を設定")
    val previousMonth get() = t("Previous month", "Mese precedente", "Vorheriger Monat", "Mois précédent", "Mes anterior", "上个月", "이전 달", "前の月")
    val nextMonth get() = t("Next month", "Mese successivo", "Nächster Monat", "Mois suivant", "Mes siguiente", "下个月", "다음 달", "次の月")
    val doesNotRepeat get() = t("Does not repeat", "Non si ripete", "Keine Wiederholung", "Ne se répète pas", "No se repite", "不重复", "반복 안 함", "繰り返さない")
    val daily get() = t("Daily", "Ogni giorno", "Täglich", "Tous les jours", "Diariamente", "每天", "매일", "毎日")
    fun weeklyOn(days: String) = t("Weekly on $days", "Ogni settimana di $days", "Wöchentlich am $days", "Chaque semaine le $days", "Cada semana el $days", "每周的$days", "매주 $days", "毎週$days")
    fun monthlyOn(what: String) = t("Monthly on $what", "Ogni mese $what", "Monatlich $what", "Chaque mois $what", "Cada mes $what", "每月$what", "매월 $what", "毎月$what")
    fun annuallyOn(date: String) = t("Annually on $date", "Ogni anno il $date", "Jährlich am $date", "Chaque année le $date", "Cada año el $date", "每年的$date", "매년 $date", "毎年$date")
    val everyWeekday get() = t("Every weekday (Monday to Friday)", "Ogni giorno feriale (dal lunedì al venerdì)", "Jeden Werktag (Montag bis Freitag)", "Tous les jours ouvrés (du lundi au vendredi)", "Todos los días laborables (de lunes a viernes)", "每个工作日（周一至周五）", "매 평일 (월요일~금요일)", "毎平日（月曜〜金曜）")
    val everyWeekdayShort get() = t("every weekday", "ogni giorno feriale", "jeden Werktag", "tous les jours ouvrés", "todos los días laborables", "每个工作日", "평일", "平日")
    val custom get() = t("Custom…", "Personalizzata…", "Benutzerdefiniert…", "Personnalisée…", "Personalizada…", "自定义…", "사용자 지정…", "カスタム…")
    val customRecurrence get() = t("Custom recurrence", "Ripetizione personalizzata", "Benutzerdefinierte Wiederholung", "Répétition personnalisée", "Repetición personalizada", "自定义重复", "사용자 지정 반복", "カスタムの繰り返し")
    val repeatEvery get() = t("Repeat every", "Ripeti ogni", "Wiederholen alle", "Répéter tous les", "Repetir cada", "重复间隔", "반복 주기", "繰り返す間隔")
    val changeUnit get() = t("Change unit", "Cambia unità", "Einheit ändern", "Changer d'unité", "Cambiar unidad", "更改单位", "단위 변경", "単位を変更")
    val repeatOn get() = t("Repeat on", "Ripeti di", "Wiederholen am", "Répéter le", "Repetir el", "重复于", "반복 요일", "繰り返す曜日")
    val ends get() = t("Ends", "Termina", "Endet", "Se termine", "Termina", "结束", "종료", "終了")
    val never get() = t("Never", "Mai", "Nie", "Jamais", "Nunca", "永不", "없음", "なし")
    val on get() = t("On", "Il", "Am", "Le", "El", "于", "날짜", "日付")
    val after get() = t("After", "Dopo", "Nach", "Après", "Después de", "重复", "반복", "回数")
    val times get() = t("times", "volte", "Mal", "fois", "veces", "次后", "회 후", "回")
    val everyDay get() = t("Every day", "Ogni giorno", "Jeden Tag", "Tous les jours", "Todos los días", "每天", "매일", "毎日")
    fun everyNDays(n: Int) = t("Every $n days", "Ogni $n giorni", "Alle $n Tage", "Tous les $n jours", "Cada $n días", "每${n}天", "${n}일마다", "${n}日ごと")
    fun everyNWeeksOn(n: Int, days: String) = t("Every $n weeks on $days", "Ogni $n settimane di $days", "Alle $n Wochen am $days", "Toutes les $n semaines le $days", "Cada $n semanas el $days", "每${n}周的$days", "${n}주마다 $days", "${n}週ごとの$days")
    fun everyNMonthsOn(n: Int, what: String) = t("Every $n months on $what", "Ogni $n mesi $what", "Alle $n Monate $what", "Tous les $n mois $what", "Cada $n meses $what", "每${n}个月$what", "${n}개월마다 $what", "${n}か月ごと$what")
    fun everyNYearsOn(n: Int, date: String) = t("Every $n years on $date", "Ogni $n anni il $date", "Alle $n Jahre am $date", "Tous les $n ans le $date", "Cada $n años el $date", "每${n}年的$date", "${n}년마다 $date", "${n}年ごとの$date")
    fun until(date: String) = t(", until $date", ", fino al $date", ", bis $date", ", jusqu'au $date", ", hasta el $date", "，直到$date", ", ${date}까지", "、${date}まで")
    fun nTimes(n: Int) = t(", $n times", ", $n volte", ", $n Mal", ", $n fois", ", $n veces", "，共${n}次", ", ${n}회", "、${n}回")

    /** "il giorno 5": il giorno del mese dentro una frase sulle ripetizioni. */
    fun dayOfMonth(n: Int) = t("day $n", "il giorno $n", "am $n.", "le $n", "el día $n", "${n}日", "${n}일", "${n}日")

    /** "On day 5": la stessa cosa come voce da scegliere. */
    fun onDayOfMonth(n: Int) = t("On day $n", "Il giorno $n", "Am $n.", "Le $n", "El día $n", "${n}日", "${n}일", "${n}日")

    /**
     * "il primo lunedì", "l'ultima domenica": quale settimana del mese e che
     * giorno. In italiano la domenica è femminile e vuole "la prima", non
     * "il primo".
     */
    fun nthWeekday(date: LocalDate, dayName: String, capitalized: Boolean = false): String {
        val last = date.plusWeeks(1).month != date.month
        val index = if (last) 4 else ((date.dayOfMonth - 1) / 7).coerceAtMost(3)
        return nthWeekdayPhrase(index, date.dayOfWeek, dayName, capitalized)
    }

    /**
     * La stessa frase con settimana e giorno scelti apposta (dal 26/09/2026):
     * `index` 0–3 per la prima–quarta, 4 per l'ultima.
     */
    fun nthWeekdayPhrase(index: Int, weekday: DayOfWeek, dayName: String, capitalized: Boolean = false): String {
        val feminine = weekday == DayOfWeek.SUNDAY
        val phrase = when (AppSettings.language) {
            AppLanguage.ENGLISH -> "the ${listOf("first", "second", "third", "fourth", "last")[index]} $dayName"
            AppLanguage.ITALIAN -> if (feminine) {
                "la ${listOf("prima", "seconda", "terza", "quarta", "ultima")[index]} $dayName"
            } else {
                "il ${listOf("primo", "secondo", "terzo", "quarto", "ultimo")[index]} $dayName"
            }
            AppLanguage.GERMAN -> "am ${listOf("ersten", "zweiten", "dritten", "vierten", "letzten")[index]} $dayName"
            AppLanguage.FRENCH -> "le ${listOf("premier", "deuxième", "troisième", "quatrième", "dernier")[index]} $dayName"
            AppLanguage.SPANISH -> "el ${listOf("primer", "segundo", "tercer", "cuarto", "último")[index]} $dayName"
            AppLanguage.CHINESE -> "${listOf("第一个", "第二个", "第三个", "第四个", "最后一个")[index]}$dayName"
            AppLanguage.KOREAN -> "${listOf("첫째", "둘째", "셋째", "넷째", "마지막")[index]} $dayName"
            AppLanguage.JAPANESE -> "${listOf("第1", "第2", "第3", "第4", "最終")[index]}$dayName"
        }
        return if (capitalized) phrase.replaceFirstChar { it.uppercase(AppSettings.language.locale) } else phrase
    }

    // --- Da quando si ripete (dal 26/09/2026) ---

    val repeatStarts get() = t("Starts", "Inizia", "Beginnt", "Commence", "Empieza", "开始", "시작", "開始")
    val repeatFromTitle get() = t("Repeat from", "Ripeti a partire da", "Wiederholen ab", "Répéter à partir de", "Repetir desde", "重复开始于", "반복 시작", "繰り返しの開始")
    val repeatFromToday get() = t(
        "From today until 2100", "Da oggi fino al 2100", "Ab heute bis 2100", "À partir d'aujourd'hui jusqu'en 2100",
        "Desde hoy hasta 2100", "从今天到2100年", "오늘부터 2100년까지", "今日から2100年まで"
    )
    val repeatFromChosenDay get() = t(
        "From a day I choose until 2100", "Da un giorno che scelgo io fino al 2100", "Ab einem Tag meiner Wahl bis 2100",
        "À partir d'un jour que je choisis jusqu'en 2100", "Desde un día que elijo hasta 2100", "从我选择的日期到2100年",
        "내가 고른 날부터 2100년까지", "選んだ日から2100年まで"
    )

    // --- Le colonne Number e i calcoli (dal 26/09/2026) ---

    val changeType get() = t("Change type", "Cambia tipo", "Typ ändern", "Changer le type", "Cambiar tipo", "更改类型", "유형 변경", "種類を変更")
    val calculate get() = t("Calculate", "Calcola", "Berechnen", "Calculer", "Calcular", "计算", "계산", "計算")
    val numberFormat get() = t("Number format", "Formato del numero", "Zahlenformat", "Format du nombre", "Formato de número", "数字格式", "숫자 형식", "数値の形式")
    val decimalPlaces get() = t("Decimal places", "Cifre decimali", "Dezimalstellen", "Décimales", "Decimales", "小数位数", "소수 자릿수", "小数点以下の桁数")
    val showAs get() = t("Show as", "Mostra come", "Anzeigen als", "Afficher comme", "Mostrar como", "显示为", "표시 방식", "表示方法")
    val displayNumber get() = t("Number", "Numero", "Zahl", "Nombre", "Número", "数字", "숫자", "数値")
    val displayBar get() = t("Bar", "Barra", "Balken", "Barre", "Barra", "条形", "막대", "バー")
    val displayRing get() = t("Ring", "Anello", "Ring", "Anneau", "Anillo", "圆环", "링", "リング")
    val color get() = t("Color", "Colore", "Farbe", "Couleur", "Color", "颜色", "색", "色")
    val showNumber get() = t("Show number", "Mostra il numero", "Zahl anzeigen", "Afficher le nombre", "Mostrar número", "显示数字", "숫자 표시", "数値を表示")
    val position get() = t("Position", "Posizione", "Position", "Position", "Posición", "位置", "위치", "位置")
    val positionLeft get() = t("Left", "A sinistra", "Links", "Gauche", "Izquierda", "左侧", "왼쪽", "左")
    val positionCenter get() = t("Center", "Al centro", "Mitte", "Centre", "Centro", "居中", "가운데", "中央")
    val defaultDecimals get() = t("Default", "Predefinito", "Standard", "Par défaut", "Predeterminado", "默认", "기본값", "デフォルト")
    val filterFormats get() = t("Filter formats…", "Cerca un formato…", "Formate filtern…", "Filtrer les formats…", "Filtrar formatos…", "筛选格式…", "형식 필터…", "形式を絞り込む…")
    val formatPercent get() = t("Percent", "Percentuale", "Prozent", "Pourcentage", "Porcentaje", "百分比", "퍼센트", "パーセント")
    val appliesToAllViews get() = t(
        "Changes apply to all views showing this property.",
        "Vale in tutte le viste che mostrano questa proprietà.",
        "Gilt für alle Ansichten mit dieser Eigenschaft.",
        "S'applique à toutes les vues qui affichent cette propriété.",
        "Se aplica a todas las vistas que muestran esta propiedad.",
        "更改适用于显示此属性的所有视图。",
        "이 속성을 표시하는 모든 보기에 적용됩니다.",
        "このプロパティを表示するすべてのビューに適用されます。"
    )

    /** Il nome di un formato: "Number" e "Percent" tradotti, le valute come le ha scritte l'utente. */
    fun numberFormatName(kind: NumberFormatKind): String = when (kind) {
        NumberFormatKind.NUMBER -> displayNumber
        NumberFormatKind.PERCENT -> formatPercent
        else -> kind.label
    }

    val timeFormat get() = t("Time format", "Formato dell'ora", "Zeitformat", "Format de l'heure", "Formato de hora", "时间格式", "시간 형식", "時刻の形式")
    val timeHidden get() = t("Hidden", "Nascosta", "Ausgeblendet", "Masquée", "Oculta", "隐藏", "숨김", "非表示")
    val time12 get() = t("12 hour", "12 ore", "12 Stunden", "12 heures", "12 horas", "12 小时制", "12시간", "12 時間")
    val time24 get() = t("24 hour", "24 ore", "24 Stunden", "24 heures", "24 horas", "24 小时制", "24시간", "24 時間")
    val timeHiddenNote get() = t(
        "No time in database dates",
        "Niente ora nelle date dei database",
        "Keine Uhrzeit in Datenbank-Daten",
        "Pas d'heure dans les dates des bases de données",
        "Sin hora en las fechas de las bases de datos",
        "数据库日期中不显示时间",
        "데이터베이스 날짜에 시간 없음",
        "データベースの日付に時刻を表示しない"
    )

    fun timeFormatName(time: TimeFormatKind): String = when (time) {
        TimeFormatKind.HIDDEN -> timeHidden
        TimeFormatKind.H12 -> time12
        TimeFormatKind.H24 -> time24
    }

    val calcCount get() = t("Count", "Conteggio", "Anzahl", "Nombre", "Recuento", "计数", "개수", "カウント")
    val calcPercent get() = t("Percent", "Percentuale", "Prozent", "Pourcentage", "Porcentaje", "百分比", "퍼센트", "パーセント")
    val calcDate get() = t("Date", "Data", "Datum", "Date", "Fecha", "日期", "날짜", "日付")
    val calcMore get() = t("More options", "Altre opzioni", "Weitere Optionen", "Plus d'options", "Más opciones", "更多选项", "더 보기", "その他のオプション")

    fun calcGroupName(group: CalculationGroup): String = when (group) {
        CalculationGroup.COUNT -> calcCount
        CalculationGroup.PERCENT -> calcPercent
        CalculationGroup.DATE -> calcDate
        CalculationGroup.MORE -> calcMore
    }

    /** Il nome di un calcolo, nel menu "Calculate". */
    fun calcName(calc: Calculation): String = when (calc) {
        Calculation.COUNT_ALL -> t("Count all", "Conta tutte", "Alle zählen", "Tout compter", "Contar todo", "全部计数", "모두 세기", "すべてカウント")
        Calculation.COUNT_VALUES -> t("Count values", "Conta i valori", "Werte zählen", "Compter les valeurs", "Contar valores", "值计数", "값 세기", "値をカウント")
        Calculation.COUNT_UNIQUE -> t("Count unique values", "Conta i valori unici", "Eindeutige Werte zählen", "Compter les valeurs uniques", "Contar valores únicos", "唯一值计数", "고유 값 세기", "一意の値をカウント")
        Calculation.COUNT_EMPTY -> t("Count empty", "Conta le vuote", "Leere zählen", "Compter les vides", "Contar vacíos", "空值计数", "빈 항목 세기", "空をカウント")
        Calculation.COUNT_NOT_EMPTY -> t("Count not empty", "Conta le non vuote", "Nicht leere zählen", "Compter les non vides", "Contar no vacíos", "非空值计数", "비어 있지 않은 항목 세기", "空でないものをカウント")
        Calculation.CHECKED -> t("Checked", "Spuntate", "Angehakt", "Cochées", "Marcadas", "已勾选", "체크됨", "チェック済み")
        Calculation.UNCHECKED -> t("Unchecked", "Non spuntate", "Nicht angehakt", "Non cochées", "Sin marcar", "未勾选", "체크 안 됨", "未チェック")
        Calculation.PERCENT_EMPTY -> t("Percent empty", "Percentuale vuote", "Prozent leer", "Pourcentage vide", "Porcentaje vacío", "空值百分比", "빈 항목 비율", "空の割合")
        Calculation.PERCENT_NOT_EMPTY -> t("Percent not empty", "Percentuale non vuote", "Prozent nicht leer", "Pourcentage non vide", "Porcentaje no vacío", "非空值百分比", "비어 있지 않은 항목 비율", "空でない割合")
        Calculation.EARLIEST_DATE -> t("Earliest date", "Data più vecchia", "Frühestes Datum", "Date la plus ancienne", "Fecha más antigua", "最早日期", "가장 이른 날짜", "最も早い日付")
        Calculation.LATEST_DATE -> t("Latest date", "Data più recente", "Spätestes Datum", "Date la plus récente", "Fecha más reciente", "最晚日期", "가장 늦은 날짜", "最も遅い日付")
        Calculation.DATE_RANGE -> t("Date range", "Intervallo di date", "Datumsbereich", "Plage de dates", "Rango de fechas", "日期范围", "날짜 범위", "日付の範囲")
        Calculation.SUM -> t("Sum", "Somma", "Summe", "Somme", "Suma", "求和", "합계", "合計")
        Calculation.AVERAGE -> t("Average", "Media", "Durchschnitt", "Moyenne", "Promedio", "平均值", "평균", "平均")
        Calculation.MEDIAN -> t("Median", "Mediana", "Median", "Médiane", "Mediana", "中位数", "중앙값", "中央値")
        Calculation.MIN -> t("Min", "Minimo", "Min", "Min", "Mín", "最小值", "최소", "最小")
        Calculation.MAX -> t("Max", "Massimo", "Max", "Max", "Máx", "最大值", "최대", "最大")
        Calculation.RANGE -> t("Range", "Intervallo", "Spanne", "Étendue", "Rango", "范围", "범위", "範囲")
    }

    /** La parola corta in fondo alla colonna, prima del risultato: "SUM 309". */
    fun calcShortName(calc: Calculation): String = when (calc) {
        Calculation.COUNT_ALL -> calcCount
        Calculation.COUNT_VALUES -> t("Values", "Valori", "Werte", "Valeurs", "Valores", "值", "값", "値")
        Calculation.COUNT_UNIQUE -> t("Unique", "Unici", "Eindeutig", "Uniques", "Únicos", "唯一", "고유", "一意")
        Calculation.COUNT_EMPTY, Calculation.PERCENT_EMPTY ->
            t("Empty", "Vuote", "Leer", "Vides", "Vacíos", "空", "빈 항목", "空")
        Calculation.COUNT_NOT_EMPTY, Calculation.PERCENT_NOT_EMPTY ->
            t("Not empty", "Non vuote", "Nicht leer", "Non vides", "No vacíos", "非空", "비어 있지 않음", "空でない")
        Calculation.EARLIEST_DATE -> t("Earliest", "La più vecchia", "Frühestes", "Plus ancienne", "Más antigua", "最早", "가장 이른", "最も早い")
        Calculation.LATEST_DATE -> t("Latest", "La più recente", "Spätestes", "Plus récente", "Más reciente", "最晚", "가장 늦은", "最も遅い")
        else -> calcName(calc)
    }

    fun daysCount(n: Long) = t(
        if (n == 1L) "1 day" else "$n days",
        if (n == 1L) "1 giorno" else "$n giorni",
        if (n == 1L) "1 Tag" else "$n Tage",
        if (n == 1L) "1 jour" else "$n jours",
        if (n == 1L) "1 día" else "$n días",
        "$n 天", "${n}일", "$n 日"
    )

    /** L'unità di una ripetizione, al singolare o al plurale. */
    fun unit(freq: RecurrenceFreq, plural: Boolean): String = when (freq) {
        RecurrenceFreq.DAILY -> if (plural) t("days", "giorni", "Tage", "jours", "días", "天", "일", "日") else t("day", "giorno", "Tag", "jour", "día", "天", "일", "日")
        RecurrenceFreq.WEEKLY -> if (plural) t("weeks", "settimane", "Wochen", "semaines", "semanas", "周", "주", "週") else t("week", "settimana", "Woche", "semaine", "semana", "周", "주", "週")
        RecurrenceFreq.MONTHLY -> if (plural) t("months", "mesi", "Monate", "mois", "meses", "个月", "개월", "か月") else t("month", "mese", "Monat", "mois", "mes", "个月", "개월", "か月")
        RecurrenceFreq.YEARLY -> if (plural) t("years", "anni", "Jahre", "ans", "años", "年", "년", "年") else t("year", "anno", "Jahr", "an", "año", "年", "년", "年")
    }

    // --- Azioni sulla riga ---

    val actions get() = t("Actions", "Azioni", "Aktionen", "Actions", "Acciones", "操作", "작업", "操作")
    fun created(date: String) = t("Created $date", "Creata il $date", "Erstellt am $date", "Créée le $date", "Creada el $date", "创建于 $date", "$date 생성", "$date に作成")
    fun lastEdited(date: String) = t("Last edited $date", "Ultima modifica il $date", "Zuletzt bearbeitet am $date", "Modifiée le $date", "Editada el $date", "最后编辑于 $date", "$date 마지막 편집", "$date に最終更新")

    // --- Impostazioni del database ---

    val editProperties get() = t("Edit properties", "Modifica le proprietà", "Eigenschaften bearbeiten", "Modifier les propriétés", "Editar propiedades", "编辑属性", "속성 편집", "プロパティを編集")
    val layout get() = t("Layout", "Vista", "Layout", "Disposition", "Diseño", "布局", "레이아웃", "レイアウト")
    val groupBy get() = t("Group by", "Raggruppa per", "Gruppieren nach", "Regrouper par", "Agrupar por", "分组依据", "그룹화 기준", "グループ化")
    val group get() = t("Group", "Gruppo", "Gruppe", "Groupe", "Grupo", "分组", "그룹", "グループ")
    // --- I raggruppamenti, quanti se ne vogliono (dal 28/10/2026) ---

    val addGroup get() = t("Add a group", "Aggiungi un gruppo", "Gruppe hinzufügen", "Ajouter un groupe", "Añadir un grupo", "添加分组", "그룹 추가", "グループを追加")
    val noGroups get() = t("No groups yet", "Ancora nessun gruppo", "Noch keine Gruppen", "Aucun groupe pour l'instant", "Aún no hay grupos", "还没有分组", "아직 그룹이 없어요", "グループはまだありません")
    val removeGroup get() = t("Remove group", "Togli il gruppo", "Gruppe entfernen", "Supprimer le groupe", "Quitar el grupo", "移除分组", "그룹 제거", "グループを解除")
    val removeAllGroups get() = t("Remove all groups", "Togli tutti i gruppi", "Alle Gruppen entfernen", "Supprimer tous les groupes", "Quitar todos los grupos", "移除所有分组", "모든 그룹 제거", "すべてのグループを解除")
    val boardColumns get() = t("Columns", "Colonne", "Spalten", "Colonnes", "Columnas", "列", "열", "列")
    val boardBands get() = t("Bands", "Fasce", "Bänder", "Bandes", "Franjas", "横带", "가로 구역", "帯")
    val groupRulesNote get() = t(
        "Groups inside groups, in this order: the first one makes the groups, the next one splits each of them, and so on.",
        "Gruppi dentro i gruppi, in quest'ordine: il primo fa i gruppi, il successivo divide ognuno di loro, e così via.",
        "Gruppen in Gruppen, in dieser Reihenfolge: Die erste bildet die Gruppen, die nächste teilt jede davon, und so weiter.",
        "Des groupes dans les groupes, dans cet ordre : le premier crée les groupes, le suivant divise chacun d'eux, et ainsi de suite.",
        "Grupos dentro de grupos, en este orden: el primero crea los grupos, el siguiente divide cada uno, y así sucesivamente.",
        "分组中的分组，按此顺序：第一个创建分组，下一个再拆分每个分组，依此类推。",
        "그룹 안의 그룹, 이 순서대로: 첫 번째가 그룹을 만들고, 다음 것이 각 그룹을 나누는 식이에요.",
        "グループの中のグループ。この順に、最初のものがグループを作り、次のものがそれぞれを分けます。"
    )
    val boardGroupRulesNote get() = t(
        "The first one makes the columns; the others make the bands across them, one inside the other.",
        "Il primo fa le colonne; gli altri le fasce orizzontali, una dentro l'altra.",
        "Die erste bildet die Spalten, die anderen die Bänder quer darüber, eines im anderen.",
        "Le premier crée les colonnes ; les autres les bandes horizontales, l'une dans l'autre.",
        "El primero crea las columnas; los demás, las franjas horizontales, una dentro de otra.",
        "第一个创建列；其余的创建横带，一层套一层。",
        "첫 번째는 열을, 나머지는 가로 구역을 만들어요. 하나가 다른 것 안에 들어가요.",
        "最初のものが列を、残りが横の帯を作ります。帯は入れ子になります。"
    )
    val sortGroups get() = t("Sort groups", "Ordina i gruppi", "Gruppen sortieren", "Trier les groupes", "Ordenar grupos", "分组排序", "그룹 정렬", "グループの並べ替え")
    val manualOrder get() = t("Manual", "A mano", "Manuell", "Manuel", "Manual", "手动", "수동", "手動")
    val dragGroupsNote get() = t(
        "Drag ≡ to put the groups in the order you want. A new group goes at the end; the one with no value stays last.",
        "Trascina ≡ per mettere i gruppi nell'ordine che vuoi. Un gruppo nuovo va in fondo; quello senza valore resta l'ultimo.",
        "Ziehe ≡, um die Gruppen zu ordnen. Eine neue Gruppe kommt ans Ende; die ohne Wert bleibt die letzte.",
        "Faites glisser ≡ pour ordonner les groupes. Un nouveau groupe va à la fin ; celui sans valeur reste le dernier.",
        "Arrastra ≡ para ordenar los grupos. Un grupo nuevo va al final; el que no tiene valor queda el último.",
        "拖动 ≡ 按你想要的顺序排列分组。新分组放在最后；没有值的分组始终在最后。",
        "≡를 끌어 그룹 순서를 정하세요. 새 그룹은 맨 뒤로 가고, 값이 없는 그룹은 항상 마지막이에요.",
        "≡ をドラッグしてグループを並べ替えます。新しいグループは最後に、値のないグループは常に最後です。"
    )
    val groupsNeedMoreProperties get() = t(
        "Every property that can make groups is already in a group.",
        "Tutte le proprietà che possono fare gruppi sono già in un gruppo.",
        "Jede Eigenschaft, die Gruppen bilden kann, ist schon in einer Gruppe.",
        "Toutes les propriétés qui peuvent créer des groupes sont déjà dans un groupe.",
        "Todas las propiedades que pueden crear grupos ya están en un grupo.",
        "所有可以分组的属性都已在分组中。",
        "그룹을 만들 수 있는 속성이 모두 이미 그룹에 있어요.",
        "グループを作れるプロパティはすべて使われています。"
    )
    val propertyVisibility get() = t("Property visibility", "Visibilità delle proprietà", "Sichtbarkeit der Eigenschaften", "Visibilité des propriétés", "Visibilidad de propiedades", "属性可见性", "속성 표시", "プロパティの表示")
    val dateProperty get() = t("Date property", "Proprietà data", "Datumseigenschaft", "Propriété de date", "Propiedad de fecha", "日期属性", "날짜 속성", "日付プロパティ")
    val hideEmptyGroups get() = t("Hide empty groups", "Nascondi i gruppi vuoti", "Leere Gruppen ausblenden", "Masquer les groupes vides", "Ocultar grupos vacíos", "隐藏空分组", "빈 그룹 숨기기", "空のグループを非表示")

    /** "Hide New" della bacheca (dal 26/10/2026), con la parola del pulsante com'è scritta sotto le schede. */
    val hideNew: String get() {
        val word = newShort
        return t("Hide “$word”", "Nascondi “$word”", "„$word“ ausblenden", "Masquer « $word »", "Ocultar «$word»", "隐藏“$word”", "‘$word’ 숨기기", "「$word」を非表示")
    }
    val allShown get() = t("All shown", "Tutte visibili", "Alle sichtbar", "Toutes affichées", "Todas visibles", "全部显示", "모두 표시", "すべて表示")
    fun nHidden(n: Int) = t("$n hidden", "$n nascoste", "$n ausgeblendet", "$n masquées", "$n ocultas", "已隐藏 $n 个", "${n}개 숨김", "${n}件非表示")
    val change get() = t("Change", "Cambia", "Ändern", "Changer", "Cambiar", "更改", "변경", "変更")

    /** Nelle azioni di una scheda della galleria: l'inquadratura della copertina, solo nella scheda. */
    val repositionCover get() = t("Reposition cover", "Riposiziona copertina", "Titelbild neu positionieren", "Repositionner la couverture", "Reposicionar portada", "调整封面位置", "커버 위치 조정", "カバーの位置を調整")
    // --- "Page link" (dal 29/09/2026) ---

    val pageLinkHint get() = t(
        "Choose the page to link: search it by name, or open the pages to find it.",
        "Scegli la pagina da collegare: cercala per nome, o apri le pagine per trovarla.",
        "Wähle die Seite, die verknüpft werden soll: suche sie nach Namen oder öffne die Seiten.",
        "Choisissez la page à lier : cherchez-la par son nom ou ouvrez les pages pour la trouver.",
        "Elige la página que enlazar: búscala por su nombre o abre las páginas para encontrarla.",
        "选择要链接的页面：按名称搜索，或展开页面查找。",
        "연결할 페이지를 고르세요. 이름으로 검색하거나 페이지를 펼쳐 찾으세요.",
        "リンクするページを選んでください。名前で検索するか、ページを開いて探します。"
    )
    val removeLink get() = t("Remove link", "Togli il collegamento", "Link entfernen", "Retirer le lien", "Quitar enlace", "移除链接", "링크 제거", "リンクを外す")
    val pageNotFound get() = t("Page not found", "Pagina non trovata", "Seite nicht gefunden", "Page introuvable", "Página no encontrada", "找不到页面", "페이지를 찾을 수 없음", "ページが見つかりません")

    // --- Le sotto-righe (dal 29/09/2026) ---

    val createSubRow get() = t("Create sub row", "Crea una sotto-riga", "Unterzeile erstellen", "Créer une sous-ligne", "Crear subfila", "创建子行", "하위 행 만들기", "サブ行を作成")
    val includeSubRows get() = t("Include sub rows", "Includi le sotto-righe", "Unterzeilen einbeziehen", "Inclure les sous-lignes", "Incluir subfilas", "包括子行", "하위 행 포함", "サブ行も含める")
    val showSubRows get() = t("Show sub rows", "Mostra le sotto-righe", "Unterzeilen zeigen", "Afficher les sous-lignes", "Mostrar subfilas", "显示子行", "하위 행 표시", "サブ行を表示")
    val hideSubRows get() = t("Hide sub rows", "Nascondi le sotto-righe", "Unterzeilen ausblenden", "Masquer les sous-lignes", "Ocultar subfilas", "隐藏子行", "하위 행 숨기기", "サブ行を隠す")
    val subRowsStay get() = t(
        "Its sub rows stay, one level up.",
        "Le sue sotto-righe restano, un livello più su.",
        "Ihre Unterzeilen bleiben, eine Ebene höher.",
        "Ses sous-lignes restent, un niveau plus haut.",
        "Sus subfilas se quedan, un nivel más arriba.",
        "它的子行会保留，并上移一级。",
        "하위 행은 한 단계 위로 올라가 남습니다.",
        "サブ行は1段上に移って残ります。"
    )

    // --- La larghezza delle colonne (dal 29/09/2026) ---

    val resizeColumn get() = t("Resize column", "Ridimensiona la colonna", "Spaltenbreite ändern", "Redimensionner la colonne", "Cambiar ancho de columna", "调整列宽", "열 너비 조정", "列幅を変更")
    val fitToContent get() = t("Fit to content", "Adatta al contenuto", "An Inhalt anpassen", "Ajuster au contenu", "Ajustar al contenido", "适应内容", "내용에 맞추기", "内容に合わせる")

    // --- Le viste (dal 29/09/2026) ---

    val views get() = t("Views", "Viste", "Ansichten", "Vues", "Vistas", "视图", "보기", "ビュー")
    val newView get() = t("New view", "Nuova vista", "Neue Ansicht", "Nouvelle vue", "Nueva vista", "新建视图", "새 보기", "新しいビュー")
    val addView get() = t("Add view", "Aggiungi una vista", "Ansicht hinzufügen", "Ajouter une vue", "Añadir vista", "添加视图", "보기 추가", "ビューを追加")
    val viewName get() = t("View name", "Nome della vista", "Name der Ansicht", "Nom de la vue", "Nombre de la vista", "视图名称", "보기 이름", "ビュー名")
    /** "Data source" di una vista nuova (dal 21/10/2026): le righe di quale database. */
    val dataSource get() = t("Data source", "Sorgente dei dati", "Datenquelle", "Source de données", "Fuente de datos", "数据源", "데이터 소스", "データソース")
    val dataSourceHint get() = t(
        "The rows this view shows: this database's, or another one's. Its properties are that database's.",
        "Le righe che mostra la vista: quelle di questo database o di un altro. Le proprietà sono di quel database.",
        "Die Zeilen dieser Ansicht: aus dieser oder einer anderen Datenbank. Die Eigenschaften sind die jener Datenbank.",
        "Les lignes de cette vue : celles de cette base ou d'une autre. Les propriétés sont celles de cette base.",
        "Las filas de esta vista: las de esta base o las de otra. Las propiedades son las de esa base.",
        "此视图显示的行：来自此数据库或其他数据库。属性属于那个数据库。",
        "이 보기에 표시되는 행: 이 데이터베이스 또는 다른 데이터베이스의 행. 속성은 그 데이터베이스의 것입니다.",
        "このビューに表示する行：このデータベースか別のデータベースのもの。プロパティはそのデータベースのものです。"
    )
    val viewOptions get() = t("View options", "Opzioni della vista", "Ansichtsoptionen", "Options de la vue", "Opciones de la vista", "视图选项", "보기 옵션", "ビューのオプション")
    val freezeFirstColumn get() = t("Freeze first column", "Blocca la prima colonna", "Erste Spalte fixieren", "Figer la première colonne", "Fijar la primera columna", "冻结首列", "첫 열 고정", "最初の列を固定")
    /** È la riga con i nomi delle proprietà, non una riga del database: il nome lo dice (dal 20/10/2026). */
    val freezeFirstRow get() = t("Freeze Properties Row", "Blocca la riga delle proprietà", "Eigenschaftenzeile fixieren", "Figer la ligne des propriétés", "Fijar la fila de propiedades", "冻结属性行", "속성 행 고정", "プロパティ行を固定")
    val editView get() = t("Edit view", "Modifica la vista", "Ansicht bearbeiten", "Modifier la vue", "Editar vista", "编辑视图", "보기 편집", "ビューを編集")
    val deleteView get() = t("Delete view", "Elimina la vista", "Ansicht löschen", "Supprimer la vue", "Eliminar vista", "删除视图", "보기 삭제", "ビューを削除")
    val deleteViewMessage get() = t(
        "The view goes away with its settings. The pages stay in the database.",
        "La vista se ne va con le sue impostazioni. Le pagine restano nel database.",
        "Die Ansicht verschwindet mit ihren Einstellungen. Die Seiten bleiben in der Datenbank.",
        "La vue disparaît avec ses réglages. Les pages restent dans la base de données.",
        "La vista desaparece con sus ajustes. Las páginas se quedan en la base de datos.",
        "视图及其设置将被删除，页面仍保留在数据库中。",
        "보기와 설정이 삭제됩니다. 페이지는 데이터베이스에 남습니다.",
        "ビューと設定が削除されます。ページはデータベースに残ります。"
    )
    val showTitle get() = t("Show title", "Mostra il titolo", "Titel anzeigen", "Afficher le titre", "Mostrar título", "显示标题", "제목 표시", "タイトルを表示")
    val cardPreview get() = t("Card preview", "Anteprima della scheda", "Kartenvorschau", "Aperçu de la carte", "Vista previa de la tarjeta", "卡片预览", "카드 미리보기", "カードプレビュー")
    val cardSize get() = t("Card size", "Dimensione della scheda", "Kartengröße", "Taille de la carte", "Tamaño de la tarjeta", "卡片大小", "카드 크기", "カードサイズ")
    val calendarNeedsDate get() = t(
        "A calendar places rows by a Date property, and this database doesn't have one yet.",
        "Un calendario colloca le righe in base a una proprietà Data, e questo database non ne ha ancora una.",
        "Ein Kalender ordnet Zeilen nach einer Datumseigenschaft an, und diese Datenbank hat noch keine.",
        "Un calendrier place les lignes selon une propriété Date, et cette base de données n'en a pas encore.",
        "Un calendario coloca las filas según una propiedad Fecha, y esta base de datos aún no tiene ninguna.",
        "日历会按日期属性排列各行，而此数据库还没有日期属性。",
        "캘린더는 날짜 속성으로 행을 배치하는데, 이 데이터베이스에는 아직 날짜 속성이 없습니다.",
        "カレンダーは日付プロパティで行を配置しますが、このデータベースにはまだありません。"
    )
    val groupingNeedsProperty get() = t(
        "Grouping puts pages with the same value together, so it needs a property to look at.",
        "Il raggruppamento mette insieme le pagine con lo stesso valore, quindi serve una proprietà da guardare.",
        "Beim Gruppieren werden Seiten mit gleichem Wert zusammengefasst; dafür braucht es eine Eigenschaft.",
        "Le regroupement réunit les pages ayant la même valeur : il faut donc une propriété à examiner.",
        "Agrupar junta las páginas con el mismo valor, así que necesita una propiedad en la que fijarse.",
        "分组会把值相同的页面放在一起，因此需要一个可参照的属性。",
        "그룹화는 값이 같은 페이지를 모으므로 기준이 될 속성이 필요합니다.",
        "グループ化は同じ値のページをまとめるため、基準となるプロパティが必要です。"
    )
    val shownInTable get() = t("Shown in table", "Visibili nella tabella", "In der Tabelle sichtbar", "Affichées dans le tableau", "Visibles en la tabla", "在表格中显示", "표에 표시됨", "テーブルに表示")
    val hideAll get() = t("Hide all", "Nascondi tutte", "Alle ausblenden", "Tout masquer", "Ocultar todas", "全部隐藏", "모두 숨기기", "すべて非表示")
    val hiddenInTable get() = t("Hidden in table", "Nascoste nella tabella", "In der Tabelle ausgeblendet", "Masquées dans le tableau", "Ocultas en la tabla", "在表格中隐藏", "표에서 숨김", "テーブルで非表示")
    val showAll get() = t("Show all", "Mostra tutte", "Alle einblenden", "Tout afficher", "Mostrar todas", "全部显示", "모두 표시", "すべて表示")
    val nothingHidden get() = t(
        "Nothing is hidden. Holding a column header in the table hides it from here on.",
        "Non è nascosto niente. Tenendo premuta l'intestazione di una colonna nella tabella la si nasconde da qui in poi.",
        "Nichts ist ausgeblendet. Langes Drücken auf eine Spaltenüberschrift in der Tabelle blendet sie ab jetzt aus.",
        "Rien n'est masqué. Un appui long sur l'en-tête d'une colonne du tableau la masque désormais.",
        "No hay nada oculto. Mantener pulsado el encabezado de una columna en la tabla la oculta a partir de entonces.",
        "没有隐藏的内容。在表格中长按列标题即可将其隐藏。",
        "숨겨진 항목이 없습니다. 표에서 열 머리글을 길게 누르면 숨길 수 있습니다.",
        "非表示の項目はありません。テーブルで列見出しを長押しすると非表示になります。"
    )
    fun showInTable(property: String) = t("Show $property in the table", "Mostra $property nella tabella", "$property in der Tabelle anzeigen", "Afficher $property dans le tableau", "Mostrar $property en la tabla", "在表格中显示$property", "표에 $property 표시", "テーブルに${property}を表示")
    fun hideFromTable(property: String) = t("Hide $property from the table", "Nascondi $property dalla tabella", "$property in der Tabelle ausblenden", "Masquer $property du tableau", "Ocultar $property de la tabla", "在表格中隐藏$property", "표에서 $property 숨기기", "テーブルで${property}を非表示")

    // --- Le celle URL (dal 05/10/2026): un tocco apre, il tocco lungo il menu ---

    val editLink get() = t("Edit link", "Modifica link", "Link bearbeiten", "Modifier le lien", "Editar enlace", "编辑链接", "링크 편집", "リンクを編集")
    val copyLink get() = t("Copy link", "Copia link", "Link kopieren", "Copier le lien", "Copiar enlace", "复制链接", "링크 복사", "リンクをコピー")
    val pasteLink get() = t("Paste link", "Incolla link", "Link einfügen", "Coller le lien", "Pegar enlace", "粘贴链接", "링크 붙여넣기", "リンクを貼り付け")
    val linkCopied get() = t("Link copied", "Link copiato", "Link kopiert", "Lien copié", "Enlace copiado", "链接已复制", "링크를 복사했어요", "リンクをコピーしました")
    val showLinkAs get() = t("Show as…", "Mostra come…", "Anzeigen als…", "Afficher comme…", "Mostrar como…", "显示为…", "표시 방식…", "表示方法…")
    // "Hide icon" tenendo premuta una proprietà (dal 27/10/2026).
    val hideIcon get() = t("Hide icon", "Nascondi icona", "Symbol ausblenden", "Masquer l'icône", "Ocultar icono", "隐藏图标", "아이콘 숨기기", "アイコンを隠す")
    val showIcon get() = t("Show icon", "Mostra icona", "Symbol anzeigen", "Afficher l'icône", "Mostrar icono", "显示图标", "아이콘 보이기", "アイコンを表示")
    val cantOpenLink get() = t(
        "This link can't be opened",
        "Questo link non si può aprire",
        "Dieser Link lässt sich nicht öffnen",
        "Impossible d'ouvrir ce lien",
        "No se puede abrir este enlace",
        "无法打开此链接",
        "이 링크를 열 수 없어요",
        "このリンクは開けません"
    )
}
