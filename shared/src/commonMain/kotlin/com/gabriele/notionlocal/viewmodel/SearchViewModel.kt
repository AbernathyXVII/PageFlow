package com.gabriele.notionlocal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel(private val repository: PageRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _results = MutableStateFlow<List<PageRepository.SearchHit>>(emptyList())
    val results: StateFlow<List<PageRepository.SearchHit>> = _results

    /** I Workspace, per il titolo dei gruppi dei risultati (dal 24/11/2026). */
    private val _workspaces = MutableStateFlow<List<PageEntity>>(emptyList())
    val workspaces: StateFlow<List<PageEntity>> = _workspaces

    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        runSearch(debounce = true)
    }

    /** Un'opzione di ricerca è cambiata: i risultati si rifanno subito, senza aspettare. */
    fun onOptionsChanged() = runSearch(debounce = false)

    private fun runSearch(debounce: Boolean) {
        searchJob?.cancel()
        val query = _query.value
        if (query.isBlank()) {
            _results.value = emptyList()
            return
        }
        // Debounce: aspetta che l'utente smetta di digitare per 250ms
        // prima di interrogare il database, per non fare una query ad
        // ogni singolo carattere digitato.
        searchJob = viewModelScope.launch {
            if (debounce) delay(250)
            _workspaces.value = repository.workspaces()
            _results.value = repository.search(
                query,
                inContent = AppSettings.searchContent,
                inTrash = AppSettings.searchTrash
            )
        }
    }

    /** La pagina di una riga mai aperta, creata al momento: vedi `PageRepository.ensureRowPage`. */
    fun openRow(rowId: String, onReady: (String) -> Unit) {
        viewModelScope.launch { repository.ensureRowPage(rowId)?.let(onReady) }
    }
}
