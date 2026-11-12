package com.gabriele.notionlocal.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font

/**
 * **Sul PC le emoji di Google** (Noto Color Emoji, licenza OFL, vedi
 * `fonts/Noto-COLRv1-OFL.txt`), chieste dall'utente il 12/11/2026: quelle di
 * Windows non hanno le bandiere, e al posto di 🇮🇹 scrivono "IT". Le lettere
 * restano dei caratteri di Windows: Noto sta in coda alla lista di ogni
 * famiglia generica ("Segoe UI", "Arial", Noto), quindi Skia lo usa solo per
 * quello che quelli non sanno disegnare. Sul PC i caratteri delle pagine sono
 * tutti famiglie generiche (`PageFonts.desktop.kt`): vale ovunque. Dove Windows
 * ha anche la versione in lettere (☺️, ♥️) serve chiedere Noto: vedi
 * `emojiFontFamily` (il carattere viene dalle risorse del modulo `desktop`).
 *
 * shortcut: Compose non ha un modo pubblico per scegliere il carattere di
 * riserva sul PC (risponde sempre prima Windows, con Segoe UI Emoji); si tocca
 * con la reflection la sua lista (`GenericFontFamiliesMapping`) e si svuota la
 * sua cache. Se un aggiornamento di Compose li cambia, `install` restituisce
 * false e tornano le emoji di Windows: `DesktopEmojiTest` se ne accorge.
 */
object PcEmoji {
    /** Il carattere, dentro l'app (5 MB, a vettori: resta nitido con lo zoom). */
    val font: Font? by lazy {
        javaClass.getResourceAsStream("/fonts/Noto-COLRv1.ttf")?.use { Font("PageFlowEmoji", it.readBytes()) }
    }

    /** Lo stesso, come famiglia da chiedere a un testo. */
    val family: FontFamily? by lazy { font?.let { FontFamily(it) } }

    /** All'avvio, prima di scrivere qualunque testo. Vero se Noto è nelle liste. */
    fun install(): Boolean = runCatching {
        val font = font ?: return false
        // Il nome con cui Compose registra il carattere quando lo carica.
        val alias = font.javaClass.getMethod("getCacheKey\$ui_text").invoke(font) as String
        val holder = Class.forName("androidx.compose.ui.text.platform.PlatformFont_skikoKt")
        val field = holder.getDeclaredField("GenericFontFamiliesMapping\$delegate").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val lists = (field.get(null) as Lazy<Map<String, List<String>>>).value as MutableMap<String, List<String>>
        lists.keys.toList().forEach { name ->
            val names = lists.getValue(name)
            if (alias !in names) lists[name] = names + alias
        }
        // Le famiglie già cercate restano nella cache comune con la lista vecchia.
        val cache = Class.forName("androidx.compose.ui.text.font.FontFamilyResolverKt").getMethod("getGlobalTypefaceRequestCache").invoke(null)
        val results = cache.javaClass.getDeclaredField("resultCache").apply { isAccessible = true }.get(cache)
        results.javaClass.getMethod("evictAll").invoke(results)
        true
    }.getOrElse { false }

    /** Una volta per finestra: Skia trova Noto solo dopo che la finestra l'ha caricato. */
    @Composable
    fun Load() {
        val resolver = LocalFontFamilyResolver.current
        remember(resolver) { family?.let { resolver.resolve(it) } }
    }
}
