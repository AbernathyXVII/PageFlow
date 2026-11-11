package com.gabriele.notionlocal.ui.theme

import com.gabriele.notionlocal.platform.isPc
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.gabriele.notionlocal.data.settings.AppSettings

/**
 * Scuro coi grigi di Notion, chiaro coi suoi bianchi sporchi, o come il
 * telefono: la scelta sta nelle impostazioni (`AppSettings.themeMode`).
 *
 * Niente colori dinamici (la tavolozza generata dallo sfondo del
 * telefono): erano attivi all'inizio, ma tingevano di viola o di verde
 * proprio gli sfondi che devono restare quei grigi precisi.
 *
 * Gli schemi si costruiscono a ogni disegno e non una volta per tutte:
 * leggono i colori di `Color.kt`, che cambiano col tema, e costruiti una
 * volta sola resterebbero fermi a quello dell'avvio.
 */
@Composable
fun NotionLocalTheme(content: @Composable () -> Unit) {
    val colors = if (AppSettings.isDark) {
        darkColorScheme(
            primary = AccentBlue,
            onPrimary = androidx.compose.ui.graphics.Color.White,
            primaryContainer = DarkSurfaceVariant,
            background = DarkBackground,
            surface = DarkBackground,
            surfaceVariant = DarkSurface,
            onBackground = NotionWhite,
            onSurface = NotionWhite,
            onSurfaceVariant = NotionGray400,
            outline = NotionGray900
        )
    } else {
        lightColorScheme(
            primary = AccentBlue,
            onPrimary = androidx.compose.ui.graphics.Color.White,
            primaryContainer = DarkSurfaceVariant,
            background = DarkBackground,
            surface = DarkBackground,
            surfaceVariant = DarkSurface,
            onBackground = NotionWhite,
            onSurface = NotionWhite,
            onSurfaceVariant = NotionGray400,
            outline = NotionGray900
        )
    }
    // Sul PC il testo come Notion (vedi `PC_TEXT_SCALE`): una volta sola, anche se il tema si annida.
    val density = LocalDensity.current
    val scaled = isPc && !LocalTextScaled.current
    CompositionLocalProvider(
        LocalDensity provides if (scaled) Density(density.density, density.fontScale * PC_TEXT_SCALE) else density,
        LocalTextScaled provides (LocalTextScaled.current || scaled)
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = if (isPc) PcTypography else Typography,
            content = content
        )
    }
}

/** Vero dentro un tema che ha già rimpicciolito il testo del PC. */
private val LocalTextScaled = staticCompositionLocalOf { false }
