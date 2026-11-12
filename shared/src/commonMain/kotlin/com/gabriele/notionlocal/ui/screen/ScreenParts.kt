package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.platform.emojiFontFamily
import com.gabriele.notionlocal.data.IconSymbol
import com.gabriele.notionlocal.data.chosenEmoji
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.AccentBlue
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.platform.LocalPlatformContext

/**
 * I pezzi comuni alle schermate della barra laterale (preferiti, cestino,
 * ricerca, avvio, impostazioni): tutte uguali per costruzione, perché
 * cinque barre in alto scritte a mano diventano cinque barre diverse.
 */

/**
 * **Un'icona di Material scelta come icona** (dal 08/10/2026, la scheda
 * "Icons" del selettore): il disegno pieno, nel suo colore o in quello del
 * testo. Niente, se il nome non c'è più nella libreria.
 */
@Composable
fun SymbolIcon(value: String?, size: Dp, modifier: Modifier = Modifier) {
    val symbol = IconSymbol.parse(value) ?: return
    val vector = remember(symbol.name) { materialIconByName(symbol.name) } ?: return
    Icon(
        vector,
        contentDescription = null,
        tint = hexToColor(symbol.color) ?: NotionWhite,
        modifier = modifier.size(size)
    )
}

/**
 * L'icona di una pagina, come la mostra il suo collegamento: l'immagine
 * scelta se c'è, poi l'icona di Material, altrimenti l'emoji — e per un
 * database senza un'icona scelta il segno della tabella, non l'emoji di
 * riserva del foglietto.
 */
@Composable
fun PageIconView(
    emoji: String,
    iconImage: String?,
    isDatabase: Boolean,
    size: Dp = 20.dp,
    iconSymbol: String? = null
) {
    val context = LocalPlatformContext.current
    val store = remember(context) { PageImageStore(context) }
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        when {
            iconImage != null -> PageImage(
                fileName = iconImage,
                store = store,
                contentDescription = null,
                modifier = Modifier.size(size)
            )
            iconSymbol != null -> SymbolIcon(iconSymbol, size)
            isDatabase && chosenEmoji(emoji) == null -> Icon(
                Icons.Filled.TableChart,
                contentDescription = null,
                tint = NotionGray400,
                modifier = Modifier.size(size)
            )
            else -> Text(emoji, fontSize = (size.value * 0.8f).sp, fontFamily = emojiFontFamily)
        }
    }
}

/**
 * La barra in cima a una pagina aperta dal cestino: dice dov'è, e offre le
 * due cose che si possono fare — ripristinarla o cancellarla per sempre.
 * Ha il suo pulsante indietro, perché i pulsanti sospesi della pagina qui
 * non ci sono.
 */
@Composable
fun TrashBanner(
    onBack: () -> Unit,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TrashBannerBackground)
            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = Strings.back, tint = NotionWhite)
            }
            Text(Strings.pageInTrash, color = NotionWhite, style = MaterialTheme.typography.bodyLarge)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onRestore) {
                Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(Strings.restore)
            }
            OutlinedButton(
                onClick = onDeleteForever,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
            ) {
                Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(Strings.deletePermanently)
            }
        }
    }
}

/** La conferma prima di cancellare per sempre: è l'unica azione del cestino che non si torna indietro. */
@Composable
fun DeleteForeverDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    // Le altre domande prima di cancellare per sempre (i quaderni, dal
    // 26/09/2026) usano la stessa finestra con le loro parole.
    title: String = Strings.deleteForeverTitle,
    text: String = Strings.deleteForeverText
) {
    FitAlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSheet,
        title = { Text(title, color = NotionWhite) },
        text = { Text(text, color = NotionGray400) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(Strings.delete, color = DangerRed) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings.cancel) }
        }
    )
}

/** Il rosso delle azioni che distruggono, e il velo rossastro della barra del cestino. */
private val DangerRed = Color(0xFFE5484D)
private val TrashBannerBackground = Color(0x33E5484D)

/** L'intestazione di un gruppo di voci. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = NotionGray400,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 6.dp)
    )
}

/**
 * Una voce da toccare: icona, testo, eventuale valore sotto e qualcosa a
 * destra. Spenta, si vede grigia e non risponde — le voci promesse ma non
 * ancora costruite si mostrano così.
 */
@Composable
fun SettingsItem(
    title: String,
    icon: ImageVector? = null,
    value: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val tint = if (enabled) NotionWhite else NotionGray400.copy(alpha = 0.6f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && onClick != null) { onClick?.invoke() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(title, color = tint, style = MaterialTheme.typography.bodyLarge)
            if (value != null) {
                Text(
                    value,
                    color = if (enabled) NotionGray400 else NotionGray400.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}

/** Una voce con interruttore. */
@Composable
fun SwitchItem(
    title: String,
    checked: Boolean,
    enabled: Boolean = true,
    detail: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsItem(
        title = title,
        value = detail,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            // I colori di serie facevano sembrare un interruttore spento
            // **più grigio** di uno disattivato: il bordo dello spento era
            // il grigio scurissimo dei contorni, quello del disattivato no.
            // Qui lo spento ha un contorno che si vede, e il disattivato è
            // davvero sbiadito — acceso o spento che sia.
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = AccentBlue,
                    checkedThumbColor = Color.White,
                    uncheckedTrackColor = Color.Transparent,
                    uncheckedThumbColor = NotionGray400,
                    uncheckedBorderColor = NotionGray400,
                    disabledCheckedTrackColor = AccentBlue.copy(alpha = 0.3f),
                    disabledCheckedThumbColor = Color.White.copy(alpha = 0.5f),
                    disabledUncheckedTrackColor = Color.Transparent,
                    disabledUncheckedThumbColor = NotionGray400.copy(alpha = 0.3f),
                    disabledUncheckedBorderColor = NotionGray400.copy(alpha = 0.25f)
                )
            )
        }
    )
}

/** Una scelta fra più, col pallino. */
@Composable
fun RadioItem(
    title: String,
    selected: Boolean,
    detail: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = AccentBlue, unselectedColor = NotionGray400)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(title, color = NotionWhite, style = MaterialTheme.typography.bodyLarge)
            if (detail != null) {
                Text(detail, color = NotionGray400, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
