package com.gabriele.notionlocal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.repository.PageRepository.PageTreeNode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * La barra laterale: l'albero delle pagine.
 *
 * L'albero si carica **un ramo alla volta**, quando lo si apre: leggerlo
 * tutto a ogni apertura della barra vorrebbe dire attraversare ogni
 * pagina e ogni database dell'archivio per mostrarne una manciata. Si
 * ricarica (i soli rami aperti) ogni volta che la barra si apre, così
 * una pagina appena creata o rinominata c'è già.
 */
class SidebarViewModel(private val repository: PageRepository) : ViewModel() {

    private val _root = MutableStateFlow<PageTreeNode?>(null)
    val root: StateFlow<PageTreeNode?> = _root

    /** I figli già letti, per chiave del nodo (`PageTreeNode.key`). */
    private val _children = MutableStateFlow<Map<String, List<PageTreeNode>>>(emptyMap())
    val children: StateFlow<Map<String, List<PageTreeNode>>> = _children

    /** I rami aperti. La pagina principale parte aperta: è da lì che si comincia. */
    private val _expanded = MutableStateFlow(setOf(PageEntity.ROOT_PAGE_ID))
    val expanded: StateFlow<Set<String>> = _expanded

    /** La pagina principale che l'albero mostrava: cambiato Workspace, la nuova parte aperta. */
    private var shownHome: String? = null

    fun refresh() {
        viewModelScope.launch {
            _root.value = repository.treeRoot()
            val home = _root.value?.pageId
            if (home != null && home != shownHome) {
                shownHome = home
                _expanded.value = _expanded.value + home
            }
            val loaded = mutableMapOf<String, List<PageTreeNode>>()
            for (key in _expanded.value) {
                if (key.startsWith("row:")) continue
                loaded[key] = repository.treeChildren(key)
            }
            _children.value = loaded
        }
    }

    fun toggle(node: PageTreeNode) {
        val pageId = node.pageId ?: return
        if (pageId in _expanded.value) {
            _expanded.value = _expanded.value - pageId
            return
        }
        _expanded.value = _expanded.value + pageId
        viewModelScope.launch {
            _children.value = _children.value + (pageId to repository.treeChildren(pageId))
        }
    }

    /** La pagina di una riga mai aperta nasce qui, al primo tocco. */
    fun openRow(rowId: String, onReady: (String) -> Unit) {
        viewModelScope.launch { repository.ensureRowPage(rowId)?.let(onReady) }
    }
}

/**
 * La scelta della pagina in cui mettere una copia: lo stesso albero della
 * barra laterale, coi rami aperti per conto suo, più una ricerca per
 * titolo che arriva anche alle righe dei database sepolte in fondo.
 */
class PagePickerViewModel(private val repository: PageRepository) : ViewModel() {

    private val _root = MutableStateFlow<PageTreeNode?>(null)
    val root: StateFlow<PageTreeNode?> = _root

    private val _children = MutableStateFlow<Map<String, List<PageTreeNode>>>(emptyMap())
    val children: StateFlow<Map<String, List<PageTreeNode>>> = _children

    private val _expanded = MutableStateFlow(setOf(PageEntity.ROOT_PAGE_ID))
    val expanded: StateFlow<Set<String>> = _expanded

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _results = MutableStateFlow<List<PageRepository.DestinationHit>>(emptyList())
    val results: StateFlow<List<PageRepository.DestinationHit>> = _results

    private var searchJob: Job? = null

    /** "Move to database" (dal 05/10/2026): cercando si trovano i database, non le pagine. */
    var databasesOnly = false

    /** Si riparte da capo a ogni apertura: l'albero può essere cambiato, e la ricerca di prima non serve più. */
    fun reset() {
        // Dalla pagina principale del Workspace aperto (dal 24/11/2026).
        val home = repository.homeId()
        _expanded.value = setOf(home)
        setQuery("")
        viewModelScope.launch {
            _root.value = repository.treeRoot()
            _children.value = mapOf(home to repository.treeChildren(home))
        }
    }

    fun toggle(node: PageTreeNode) {
        val pageId = node.pageId ?: return
        if (pageId in _expanded.value) {
            _expanded.value = _expanded.value - pageId
            return
        }
        _expanded.value = _expanded.value + pageId
        viewModelScope.launch {
            _children.value = _children.value + (pageId to repository.treeChildren(pageId))
        }
    }

    fun setQuery(text: String) {
        _query.value = text
        searchJob?.cancel()
        if (text.isBlank()) {
            _results.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            _results.value = if (databasesOnly) {
                repository.searchDatabaseDestinations(text)
            } else {
                repository.searchDestinations(text)
            }
        }
    }
}

/** L'elenco dei preferiti. L'ordine lo decide la schermata, dalle impostazioni. */
class FavoritesViewModel(repository: PageRepository) : ViewModel() {
    val favorites: StateFlow<List<PageEntity>> = repository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/**
 * Il cestino: l'elenco, il ripristino, la cancellazione definitiva.
 *
 * Tiene il magazzino delle immagini perché cancellare una pagina per
 * davvero vuol dire anche togliere dalla cartella la sua icona e la sua
 * copertina: altrimenti resterebbero lì per sempre, senza nessuno che le
 * mostri.
 */
class TrashViewModel(
    private val repository: PageRepository,
    private val imageStore: PageImageStore
) : ViewModel() {

    val trash: StateFlow<List<PageEntity>> = repository.observeTrash()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun restore(pageId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.restoreFromTrash(pageId)
            onDone()
        }
    }

    fun deletePermanently(pageId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deletePermanently(pageId).forEach { imageStore.delete(it) }
            onDone()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash().forEach { imageStore.delete(it) }
        }
    }
}

/**
 * I template delle pagine (dal 26/09/2026, vedi `PageEntity.isTemplate`):
 * l'elenco, uno nuovo da scrivere, uno da mandare nel cestino.
 */
class TemplatesViewModel(private val repository: PageRepository) : ViewModel() {
    val templates: StateFlow<List<PageEntity>> = repository.observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Crea un template vuoto; `onCreated` riceve il suo id, per aprirlo. */
    fun create(onCreated: (String) -> Unit) {
        viewModelScope.launch { onCreated(repository.createTemplate()) }
    }

    fun moveToTrash(templateId: String) {
        viewModelScope.launch { repository.moveToTrash(templateId) }
    }
}

/** La finestra di avvio: serve l'elenco delle pagine fra cui sceglierne una. */
class StartupViewModel(private val repository: PageRepository) : ViewModel() {

    private val _pages = MutableStateFlow<List<PageEntity>>(emptyList())
    val pages: StateFlow<List<PageEntity>> = _pages

    private val _chosen = MutableStateFlow<PageEntity?>(null)
    val chosen: StateFlow<PageEntity?> = _chosen

    fun load(chosenId: String?) {
        viewModelScope.launch {
            _pages.value = repository.startupCandidates()
            _chosen.value = chosenId?.let { repository.getPage(it) }
        }
    }
}
