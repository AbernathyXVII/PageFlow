package com.gabriele.notionlocal.platform

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

actual fun showMessage(context: PlatformContext, text: String, long: Boolean) {
    Toast.makeText(context, text, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}

@Composable
actual fun screenWidthDp(): Dp = LocalConfiguration.current.screenWidthDp.dp

@Composable
actual fun screenHeightDp(): Dp = LocalConfiguration.current.screenHeightDp.dp

/**
 * Sul telefono: l'app che sa aprire l'indirizzo; una pagina web nel browser
 * predefinito (vedi `BlockFiles.openUrl`, che passa da qui).
 */
actual fun openUrl(context: PlatformContext, url: String): Boolean {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    val intent = Intent(Intent.ACTION_VIEW, uri)
    if (uri.scheme?.lowercase() in setOf("http", "https")) {
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
    }
    return runCatching {
        context.startActivity(intent.withTaskFor(context))
        true
    }.getOrDefault(false)
}

/** Dall'attività di PageFlow l'app aperta resta nella sua pila; senza un'attività Android ne vuole una nuova. */
private fun Intent.withTaskFor(context: Context): Intent =
    if (context.hostActivity() == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) else this

private tailrec fun Context.hostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.hostActivity()
    else -> null
}

/** Da Android 13 il "copiato" lo dice già il telefono. */
actual fun copyPlainText(context: PlatformContext, label: String, text: String): Boolean {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}

actual fun clipboardHasContent(context: PlatformContext): Boolean =
    context.getSystemService(ClipboardManager::class.java)?.hasPrimaryClip() == true

actual fun clipboardText(context: PlatformContext): String? {
    val clip = context.getSystemService(ClipboardManager::class.java)?.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()
}

@Composable
actual fun editCommandLabel(command: EditCommand): String = stringResource(
    when (command) {
        EditCommand.CUT -> android.R.string.cut
        EditCommand.COPY -> android.R.string.copy
        EditCommand.PASTE -> android.R.string.paste
        EditCommand.SELECT_ALL -> android.R.string.selectAll
    }
)

actual val isPc: Boolean = false
