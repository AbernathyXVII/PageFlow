package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.settings.StartupMode
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.viewmodel.StartupViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory

/**
 * Da dove parte l'app: l'ultima pagina aperta, il menu principale, o una
 * pagina scelta qui. La scelta vale dal prossimo avvio.
 *
 * Se la pagina scelta non c'è più (cancellata, o nel cestino) l'app parte
 * dal menu principale: meglio che aprire una pagina vuota.
 */
@Composable
fun StartupScreen(
    factory: ViewModelFactory,
    onBack: () -> Unit,
    viewModel: StartupViewModel = viewModel(factory = factory)
) {
    val pages by viewModel.pages.collectAsStateWithLifecycle()
    val chosen by viewModel.chosen.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf(false) }
    val mode = AppSettings.startupMode

    LaunchedEffect(AppSettings.startupPageId) { viewModel.load(AppSettings.startupPageId) }

    Scaffold(
        topBar = { WorkspaceTopBar(title = Strings.startupWindow, onBack = onBack) },
        containerColor = DarkBackground
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                Strings.startupDescription,
                color = NotionGray400,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
            RadioItem(
                title = Strings.lastVisitedPage,
                selected = mode == StartupMode.LAST_VISITED,
                onClick = { AppSettings.startupMode = StartupMode.LAST_VISITED }
            )
            RadioItem(
                title = Strings.mainMenu,
                selected = mode == StartupMode.HOME,
                onClick = { AppSettings.startupMode = StartupMode.HOME }
            )
            RadioItem(
                title = Strings.specificPage,
                detail = chosen?.title?.ifBlank { Strings.untitled } ?: Strings.noPageChosen,
                selected = mode == StartupMode.SPECIFIC_PAGE,
                onClick = {
                    AppSettings.startupMode = StartupMode.SPECIFIC_PAGE
                    if (chosen == null) picking = true
                }
            )
            if (mode == StartupMode.SPECIFIC_PAGE) {
                TextButton(
                    onClick = { picking = true },
                    modifier = Modifier.padding(start = 56.dp)
                ) { Text(Strings.choosePage) }
            }
        }
    }

    if (picking) {
        FitAlertDialog(
        fitContent = false,
            onDismissRequest = { picking = false },
            containerColor = DarkSheet,
            title = { Text(Strings.choosePage, color = NotionWhite) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(pages, key = { it.id }) { page ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    AppSettings.startupPageId = page.id
                                    AppSettings.startupMode = StartupMode.SPECIFIC_PAGE
                                    picking = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PageIconView(emoji = page.icon, iconImage = page.iconImage, isDatabase = page.isDatabase, iconSymbol = page.iconSymbol)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                page.title.ifBlank { Strings.untitled },
                                color = NotionWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { picking = false }) { Text(Strings.cancel) }
            }
        )
    }
}
