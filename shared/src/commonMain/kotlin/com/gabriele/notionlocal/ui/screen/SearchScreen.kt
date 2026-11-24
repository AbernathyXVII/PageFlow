package com.gabriele.notionlocal.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository.SearchHit
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.AccentBlue
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.DarkSurfaceVariant
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionGray900
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.viewmodel.SearchViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory

/**
 * La ricerca in tutta l'app.
 *
 * Il campo è in alto e prende subito il cursore; sotto, dietro l'icona
 * delle regolazioni, le opzioni. Ne funzionano due — cercare anche nel
 * contenuto delle pagine, e anche nel cestino — e le altre si vedono
 * grigie, ferme dove resteranno. "Cerca nei titoli" è grigio **ma
 * acceso**: nei titoli si cerca sempre, spegnerlo non si può ancora.
 *
 * Ogni risultato mostra, se la parola è nel testo, il pezzo di frase in
 * cui compare, con la parola evidenziata: è quello che dice se è la
 * pagina giusta senza aprirla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    factory: ViewModelFactory,
    onBack: () -> Unit,
    onOpenHit: (SearchHit) -> Unit,
    viewModel: SearchViewModel = viewModel(factory = factory)
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val workspaces by viewModel.workspaces.collectAsStateWithLifecycle()
    // **I risultati a gruppi, un Workspace per volta** (dal 24/11/2026, chiesto dall'utente): prima quello
    // aperto, poi gli altri nel loro ordine, ognuno col suo titolo. Con un Workspace solo, niente titoli.
    val home = AppSettings.homePageId
    val groups = remember(results, workspaces, home) {
        val order = listOf(home) + workspaces.map { it.id }.filter { it != home }
        results.groupBy { it.workspaceId }.entries
            .sortedBy { (id, _) -> order.indexOf(id).let { if (it < 0) Int.MAX_VALUE else it } }
            .map { (id, hits) -> workspaces.firstOrNull { it.id == id } to hits }
    }
    val focusRequester = remember { FocusRequester() }
    var showOptions by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Il cursore subito nel campo: si apre la ricerca per scrivere.
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(Strings.searchHint, color = NotionGray400, style = MaterialTheme.typography.bodyLarge)
                        }
                        AutoCaps {
                            BasicTextField(
                                value = query,
                                onValueChange = { viewModel.onQueryChange(it) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = NotionWhite),
                                cursorBrush = SolidColor(NotionWhite),
                                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = Strings.back, tint = NotionWhite)
                    }
                },
                actions = {
                    IconButton(onClick = { showOptions = !showOptions }) {
                        Icon(
                            Icons.Filled.Tune,
                            contentDescription = Strings.searchOptions,
                            tint = if (showOptions) AccentBlue else NotionWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            AnimatedVisibility(visible = showOptions) {
                SearchOptions(onChanged = { viewModel.onOptionsChanged() })
            }
            when {
                query.isBlank() -> CenteredHint(Strings.searchPrompt)
                results.isEmpty() -> CenteredHint(Strings.noResults)
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Prima la riga, poi la pagina: il risultato di una
                    // riga di un database semplice ha anche la pagina (il
                    // database, dove porta), e due righe dello stesso
                    // database — o il database stesso trovato per nome —
                    // avrebbero avuto la stessa chiave, cosa che una
                    // `LazyColumn` non perdona: l'app si chiude.
                    groups.forEachIndexed { index, (workspace, hits) ->
                        if (workspaces.size > 1) {
                            item(key = "workspace:${hits.first().workspaceId}") {
                                WorkspaceHeader(workspace, divider = index > 0)
                            }
                        }
                        items(hits, key = { it.rowId?.let { id -> "row:$id" } ?: it.pageId.orEmpty() }) { hit ->
                            SearchResultRow(hit = hit, onClick = { onOpenHit(hit) })
                        }
                    }
                }
            }
        }
    }
}

/** Il titolo di un gruppo di risultati: un divisore (non sopra il primo) e il nome del Workspace con la sua icona. */
@Composable
private fun WorkspaceHeader(workspace: PageEntity?, divider: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (divider) HorizontalDivider(color = NotionGray900, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (workspace != null) {
                PageIconView(emoji = workspace.icon, iconImage = workspace.iconImage, isDatabase = false, size = 18.dp, iconSymbol = workspace.iconSymbol)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = workspace?.title?.ifBlank { Strings.untitled } ?: Strings.untitled,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = NotionGray400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Le dieci opzioni della ricerca, nell'ordine chiesto. */
@Composable
private fun SearchOptions(onChanged: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(Strings.searchOptions)
        SwitchItem(Strings.searchInContent, checked = AppSettings.searchContent) {
            AppSettings.searchContent = it
            onChanged()
        }
        SwitchItem(Strings.searchInTitles, checked = true, enabled = false) {}
        SwitchItem(Strings.searchInTags, checked = false, enabled = false) {}
        SwitchItem(Strings.searchInAttachments, checked = false, enabled = false) {}
        SwitchItem(Strings.searchInArchived, checked = false, enabled = false) {}
        SwitchItem(Strings.searchInTrash, checked = AppSettings.searchTrash) {
            AppSettings.searchTrash = it
            onChanged()
        }
        SwitchItem(Strings.caseSensitive, checked = false, enabled = false) {}
        SwitchItem(Strings.wholeWords, checked = false, enabled = false) {}
        SwitchItem(Strings.autoIndexing, checked = false, enabled = false) {}
        SwitchItem(Strings.ocrSearch, checked = false, enabled = false) {}
        HorizontalDivider(color = NotionGray900)
    }
}

@Composable
private fun CenteredHint(text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = NotionGray400)
    }
}

@Composable
private fun SearchResultRow(hit: SearchHit, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        PageIconView(emoji = hit.emoji, iconImage = hit.iconImage, isDatabase = hit.isDatabase, size = 22.dp, iconSymbol = hit.iconSymbol)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    hit.title.ifBlank { Strings.untitled },
                    color = NotionWhite,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (hit.inTrash) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        Strings.inTrash,
                        color = NotionGray400,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkSurfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            val snippet = hit.snippet
            if (snippet != null) {
                Text(
                    text = buildAnnotatedString {
                        append(snippet.substring(0, hit.matchStart.coerceIn(0, snippet.length)))
                        val end = (hit.matchStart + hit.matchLength).coerceIn(0, snippet.length)
                        withStyle(SpanStyle(color = NotionWhite, fontWeight = FontWeight.SemiBold, background = AccentBlue.copy(alpha = 0.25f))) {
                            append(snippet.substring(hit.matchStart.coerceIn(0, snippet.length), end))
                        }
                        append(snippet.substring(end))
                    },
                    color = NotionGray400,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
