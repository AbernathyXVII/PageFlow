package com.gabriele.notionlocal.ui.screen

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.gabriele.notionlocal.platform.isPc

/**
 * **Interruttori e pallini di scelta senza l'alone sul PC** (dal 18/11/2026, chiesto dall'utente):
 * cliccando sul pallino di un interruttore, come "Full width", gli compariva intorno un cerchio
 * più grande, chiaro e trasparente (l'onda di Material). Sul PC niente onda; sul telefono resta,
 * sotto il dito dice che il tocco è arrivato. Hanno i nomi di quelli di Material: nel pacchetto
 * `ui.screen` prendono il loro posto (chi importa quelli di Material li salta).
 */
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors()
) = NoHaloOnPc {
    androidx.compose.material3.Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled, colors = colors)
}

/** Vedi [Switch]. */
@Composable
fun RadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: RadioButtonColors = RadioButtonDefaults.colors()
) = NoHaloOnPc {
    androidx.compose.material3.RadioButton(selected = selected, onClick = onClick, modifier = modifier, enabled = enabled, colors = colors)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoHaloOnPc(content: @Composable () -> Unit) {
    if (isPc) CompositionLocalProvider(LocalRippleConfiguration provides null, content = content) else content()
}
