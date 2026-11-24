package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.NotionGray400
import kotlinx.coroutines.launch

/**
 * **Il Workspace in cima alla barra laterale** (dal 24/11/2026, chiesto dall'utente come in Notion): la
 * sua icona e il suo nome, cioè quelli della sua pagina principale; un clic apre l'elenco per passare a un
 * altro (spuntato quello aperto) o crearne uno nuovo, col suo nome. [onSwitch] riceve la pagina principale
 * del Workspace scelto. Il nome e l'icona si cambiano dalla pagina principale, come per le altre pagine.
 */
@Composable
fun WorkspaceSwitcher(repository: PageRepository, onSwitch: (String) -> Unit) {
    val home = AppSettings.homePageId
    val current by remember(home) { repository.observePage(home) }.collectAsStateWithLifecycle(null)
    var open by remember { mutableStateOf(false) }
    var list by remember { mutableStateOf(emptyList<PageEntity>()) }
    var creating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(open) { if (open) list = repository.workspaces() }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable { open = true }
                .semantics { contentDescription = Strings.switchWorkspace }
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            current?.let { PageIconView(emoji = it.icon, iconImage = it.iconImage, isDatabase = false, size = 22.dp, iconSymbol = it.iconSymbol) }
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = current?.title?.ifBlank { Strings.untitled }.orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Filled.UnfoldMore, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            list.forEach { workspace ->
                DropdownMenuItem(
                    text = { Text(workspace.title.ifBlank { Strings.untitled }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = {
                        PageIconView(emoji = workspace.icon, iconImage = workspace.iconImage, isDatabase = false, size = 20.dp, iconSymbol = workspace.iconSymbol)
                    },
                    trailingIcon = if (workspace.id == home) {
                        { Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    } else {
                        null
                    },
                    onClick = {
                        open = false
                        if (workspace.id != home) onSwitch(workspace.id)
                    }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(Strings.newWorkspace) },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = {
                    open = false
                    creating = true
                }
            )
        }
    }
    if (creating) {
        NewWorkspaceDialog(
            onDismiss = { creating = false },
            onCreate = { name ->
                creating = false
                scope.launch { onSwitch(repository.createWorkspace(name)) }
            }
        )
    }
}

/** Il nome del Workspace nuovo: senza nome non si crea. */
@Composable
private fun NewWorkspaceDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    FitAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.newWorkspace) },
        text = {
            AutoCaps {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text(Strings.workspaceName) },
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name.trim()) }, enabled = name.isNotBlank()) { Text(Strings.create) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}
