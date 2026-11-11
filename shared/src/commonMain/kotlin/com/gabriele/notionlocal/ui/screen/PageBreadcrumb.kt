package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.NotionGray400

/**
 * **Il percorso della pagina, nella barra in alto del PC** (dal 11/11/2026,
 * terza fase dell'aspetto da Notion, chiesta dall'utente): le pagine che
 * la contengono (`PageRepository.ancestorsOf`) e lei, con icona e nome,
 * separate da "/". Un clic su una pagina del percorso la apre. Con molte
 * pagine si vedono la prima e le due più vicine, e in mezzo "…".
 */
@Composable
fun PageBreadcrumb(
    ancestors: List<PageEntity>,
    current: PageEntity?,
    onOpen: (PageEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val shown: List<PageEntity?> = if (ancestors.size > MAX_SHOWN) {
        listOf(ancestors.first(), null) + ancestors.takeLast(MAX_SHOWN - 1)
    } else {
        ancestors
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        shown.forEach { page ->
            if (page == null) Separator("…") else Crumb(page, onClick = { onOpen(page) })
            Separator("/")
        }
        if (current != null) Crumb(current, onClick = null)
    }
}

@Composable
private fun Crumb(page: PageEntity, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PageIconView(emoji = page.icon, iconImage = page.iconImage, isDatabase = page.isDatabase, size = 16.dp, iconSymbol = page.iconSymbol)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = page.title.ifBlank { Strings.untitled },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 180.dp)
        )
    }
}

@Composable
private fun Separator(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = NotionGray400, modifier = Modifier.padding(horizontal = 1.dp))
}

/** Quante pagine del percorso si vedono al massimo, prima di accorciarlo con "…". */
private const val MAX_SHOWN = 3
