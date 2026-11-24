package com.gabriele.notionlocal.ui.i18n

/** Le scritte dei quaderni, come quelli di OneNote (dal 26/09/2026). */
object NotebookStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    val notebooks get() = t("Notebooks", "Quaderni", "Notizbücher", "Blocs-notes", "Blocs de notas", "笔记本", "전자 필기장", "ノートブック")
    val newNotebook get() = t("New notebook", "Nuovo quaderno", "Neues Notizbuch", "Nouveau bloc-notes", "Nuevo bloc de notas", "新建笔记本", "새 전자 필기장", "新しいノートブック")
    val noNotebooks get() = t("No notebooks yet", "Ancora nessun quaderno", "Noch keine Notizbücher", "Aucun bloc-notes pour l'instant", "Todavía no hay blocs de notas", "还没有笔记本", "아직 전자 필기장이 없습니다", "ノートブックはまだありません")
    val newSection get() = t("New section", "Nuova sezione", "Neuer Abschnitt", "Nouvelle section", "Nueva sección", "新建分区", "새 섹션", "新しいセクション")
    val newSectionGroup get() = t("New section group", "Nuovo gruppo di sezioni", "Neue Abschnittsgruppe", "Nouveau groupe de sections", "Nuevo grupo de secciones", "新建分区组", "새 섹션 그룹", "新しいセクション グループ")
    val defaultNotebookName get() = t("My Notebook", "Il mio quaderno", "Mein Notizbuch", "Mon bloc-notes", "Mi bloc de notas", "我的笔记本", "내 전자 필기장", "マイ ノートブック")

    fun defaultSectionName(n: Int) = t(
        "New Section $n", "Nuova sezione $n", "Neuer Abschnitt $n", "Nouvelle section $n",
        "Nueva sección $n", "新分区 $n", "새 섹션 $n", "新しいセクション $n"
    )

    fun defaultGroupName(n: Int) = t(
        "New Section Group $n", "Nuovo gruppo $n", "Neue Abschnittsgruppe $n", "Nouveau groupe $n",
        "Nuevo grupo $n", "新分区组 $n", "새 섹션 그룹 $n", "新しいセクション グループ $n"
    )

    val name get() = t("Name", "Nome", "Name", "Nom", "Nombre", "名称", "이름", "名前")
    val create get() = t("Create", "Crea", "Erstellen", "Créer", "Crear", "创建", "만들기", "作成")
    val rename get() = t("Rename", "Rinomina", "Umbenennen", "Renommer", "Cambiar nombre", "重命名", "이름 바꾸기", "名前の変更")
    val color get() = t("Color", "Colore", "Farbe", "Couleur", "Color", "颜色", "색", "色")
    val notebookColor get() = t("Notebook color", "Colore del quaderno", "Notizbuchfarbe", "Couleur du bloc-notes", "Color del bloc de notas", "笔记本颜色", "전자 필기장 색", "ノートブックの色")
    val sectionColor get() = t("Section color", "Colore della sezione", "Abschnittsfarbe", "Couleur de la section", "Color de la sección", "分区颜色", "섹션 색", "セクションの色")
    val notebookIcon get() = t("Notebook icon", "Icona del quaderno", "Notizbuchsymbol", "Icône du bloc-notes", "Icono del bloc de notas", "笔记本图标", "전자 필기장 아이콘", "ノートブックのアイコン")
    val passwordProtectSection get() = t(
        "Password protect this section", "Proteggi questa sezione con la password",
        "Diesen Abschnitt mit Kennwort schützen", "Protéger cette section par mot de passe",
        "Proteger esta sección con contraseña", "用密码保护此分区", "이 섹션을 암호로 보호",
        "このセクションをパスワードで保護"
    )
    val moveUp get() = t("Move up", "Sposta su", "Nach oben", "Monter", "Subir", "上移", "위로 이동", "上へ移動")
    val moveDown get() = t("Move down", "Sposta giù", "Nach unten", "Descendre", "Bajar", "下移", "아래로 이동", "下へ移動")
    val addPage get() = t("Add page", "Aggiungi pagina", "Seite hinzufügen", "Ajouter une page", "Agregar página", "添加页", "페이지 추가", "ページの追加")
    val untitledPage get() = t("Untitled page", "Pagina senza titolo", "Unbenannte Seite", "Page sans titre", "Página sin título", "无标题页", "제목 없는 페이지", "無題のページ")
    val noPages get() = t(
        "No pages in this section yet", "Ancora nessuna pagina in questa sezione",
        "Noch keine Seiten in diesem Abschnitt", "Aucune page dans cette section pour l'instant",
        "Todavía no hay páginas en esta sección", "此分区中还没有页面", "이 섹션에 아직 페이지가 없습니다",
        "このセクションにはまだページがありません"
    )
    fun pageCount(n: Int) = t(
        if (n == 1) "1 page" else "$n pages",
        if (n == 1) "1 pagina" else "$n pagine",
        if (n == 1) "1 Seite" else "$n Seiten",
        if (n == 1) "1 page" else "$n pages",
        if (n == 1) "1 página" else "$n páginas",
        "$n 页", "${n}페이지", "$n ページ"
    )
    val noSections get() = t(
        "No sections yet", "Ancora nessuna sezione", "Noch keine Abschnitte", "Aucune section pour l'instant",
        "Todavía no hay secciones", "还没有分区", "아직 섹션이 없습니다", "セクションはまだありません"
    )
    val emptyGroup get() = t("Empty group", "Gruppo vuoto", "Leere Gruppe", "Groupe vide", "Grupo vacío", "空分区组", "빈 그룹", "空のグループ")
    val lockedSection get() = t(
        "This section is locked", "Questa sezione è bloccata", "Dieser Abschnitt ist gesperrt",
        "Cette section est verrouillée", "Esta sección está bloqueada", "此分区已锁定",
        "이 섹션은 잠겨 있습니다", "このセクションはロックされています"
    )
    val deletePage get() = t("Delete page", "Elimina pagina", "Seite löschen", "Supprimer la page", "Eliminar página", "删除页", "페이지 삭제", "ページの削除")

    val deleteNotebookTitle get() = t(
        "Delete this notebook?", "Eliminare questo quaderno?", "Dieses Notizbuch löschen?",
        "Supprimer ce bloc-notes ?", "¿Eliminar este bloc de notas?", "删除此笔记本？",
        "이 전자 필기장을 삭제할까요?", "このノートブックを削除しますか？"
    )
    val deleteSectionTitle get() = t(
        "Delete this section?", "Eliminare questa sezione?", "Diesen Abschnitt löschen?",
        "Supprimer cette section ?", "¿Eliminar esta sección?", "删除此分区？",
        "이 섹션을 삭제할까요?", "このセクションを削除しますか？"
    )
    val deleteGroupTitle get() = t(
        "Delete this section group?", "Eliminare questo gruppo di sezioni?", "Diese Abschnittsgruppe löschen?",
        "Supprimer ce groupe de sections ?", "¿Eliminar este grupo de secciones?", "删除此分区组？",
        "이 섹션 그룹을 삭제할까요?", "このセクション グループを削除しますか？"
    )
    val deletePageTitle get() = t(
        "Delete this page?", "Eliminare questa pagina?", "Diese Seite löschen?",
        "Supprimer cette page ?", "¿Eliminar esta página?", "删除此页？",
        "이 페이지를 삭제할까요?", "このページを削除しますか？"
    )

    /** "…e le sue N pagine se ne vanno per sempre": sotto la domanda, prima di cancellare. */
    fun deleteContentsText(pages: Int) = t(
        "Everything inside goes with it, $pages ${if (pages == 1) "page" else "pages"} included. It can't be undone.",
        "Se ne va con tutto quello che contiene, ${if (pages == 1) "1 pagina" else "$pages pagine"} comprese. Non si può annullare.",
        "Alles darin wird mitgelöscht, auch $pages ${if (pages == 1) "Seite" else "Seiten"}. Das kann nicht rückgängig gemacht werden.",
        "Tout son contenu part avec, $pages ${if (pages == 1) "page comprise" else "pages comprises"}. Impossible d'annuler.",
        "Se elimina con todo lo que contiene, $pages ${if (pages == 1) "página incluida" else "páginas incluidas"}. No se puede deshacer.",
        "其中的所有内容（包括 $pages 个页面）都会一起删除。此操作无法撤销。",
        "안에 있는 모든 것(페이지 ${pages}개 포함)이 함께 삭제됩니다. 되돌릴 수 없습니다.",
        "中身もすべて（${pages} ページを含む）一緒に削除されます。元に戻せません。"
    )
    val deletePageText get() = t(
        "The page is deleted permanently. It can't be undone.",
        "La pagina viene eliminata per sempre. Non si può annullare.",
        "Die Seite wird endgültig gelöscht. Das kann nicht rückgängig gemacht werden.",
        "La page est supprimée définitivement. Impossible d'annuler.",
        "La página se elimina definitivamente. No se puede deshacer.",
        "该页面将被永久删除。此操作无法撤销。",
        "페이지가 영구적으로 삭제됩니다. 되돌릴 수 없습니다.",
        "ページは完全に削除されます。元に戻せません。"
    )
}
