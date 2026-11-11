package com.gabriele.notionlocal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

/** Sul PC, in sp, quanto va scritto perché dopo [PC_TEXT_SCALE] sullo schermo venga [size]. */
private fun onScreen(size: Float) = (size / PC_TEXT_SCALE).sp

/**
 * **I caratteri del PC, come Notion** (dal 11/11/2026, vedi [PC_TEXT_SCALE]).
 * Le grandezze sono quelle misurate nella foto di Notion dell'utente: il
 * testo, le voci della barra laterale, dei menu e delle tabelle a 12,6; il
 * titolo della pagina a 28,8; il titolo di un database a 21; le scritte
 * piccole (intestazioni grigie, date) a 10,8. Nessuno spazio in più fra le
 * lettere: quello di Material (fino a mezzo punto) faceva le parole più
 * larghe che in Notion.
 */
val PcTypography: Typography = Typography().let { material ->
    fun TextStyle.flat() = copy(letterSpacing = 0.sp)
    Typography(
        displayLarge = material.displayLarge.flat(),
        displayMedium = material.displayMedium.flat(),
        displaySmall = material.displaySmall.flat(),
        headlineLarge = material.headlineLarge.flat(),
        headlineMedium = material.headlineMedium.flat(),
        headlineSmall = material.headlineSmall.flat(),
        titleLarge = Typography.titleLarge.copy(fontSize = onScreen(28.8f), lineHeight = onScreen(36f)),
        titleMedium = Typography.titleMedium.copy(fontSize = onScreen(21f), lineHeight = onScreen(27f)),
        titleSmall = material.titleSmall.flat().copy(fontSize = onScreen(12.6f), lineHeight = onScreen(18f)),
        bodyLarge = Typography.bodyLarge,
        bodyMedium = material.bodyMedium.flat().copy(fontSize = onScreen(12.6f), lineHeight = onScreen(18f)),
        bodySmall = material.bodySmall.flat().copy(fontSize = onScreen(10.8f), lineHeight = onScreen(15f)),
        labelLarge = material.labelLarge.flat().copy(fontSize = onScreen(12.6f), lineHeight = onScreen(18f)),
        labelMedium = material.labelMedium.flat().copy(fontSize = onScreen(10.8f), lineHeight = onScreen(15f)),
        labelSmall = Typography.labelSmall.copy(fontSize = onScreen(10.8f), lineHeight = onScreen(15f))
    )
}
