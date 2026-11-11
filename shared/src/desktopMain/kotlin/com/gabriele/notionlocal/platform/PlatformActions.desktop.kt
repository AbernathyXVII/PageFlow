package com.gabriele.notionlocal.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.net.URI

/**
 * **Gli avvisi brevi sul PC**: la finestra (`desktop/Main.kt`) mostra l'ultimo
 * in basso, come il `Toast` del telefono, e poi lo toglie.
 */
object DesktopMessages {
    private val _current = MutableStateFlow<Pair<Long, String>?>(null)
    val current: StateFlow<Pair<Long, String>?> = _current

    fun show(text: String) {
        _current.value = System.nanoTime() to text
    }

    fun dismiss(id: Long) {
        if (_current.value?.first == id) _current.value = null
    }
}

actual fun showMessage(context: PlatformContext, text: String, long: Boolean) = DesktopMessages.show(text)

@Composable
actual fun screenWidthDp(): Dp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }

@Composable
actual fun screenHeightDp(): Dp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }

/** Sul PC: il browser o il programma di posta predefinito. */
actual fun openUrl(context: PlatformContext, url: String): Boolean = runCatching {
    val desktop = Desktop.getDesktop()
    val uri = URI(url.trim())
    if (uri.scheme.equals("mailto", ignoreCase = true) && desktop.isSupported(Desktop.Action.MAIL)) desktop.mail(uri) else desktop.browse(uri)
    true
}.getOrDefault(false)

/** Sul PC il sistema non dice "copiato": lo dice l'app. */
actual fun copyPlainText(context: PlatformContext, label: String, text: String): Boolean {
    runCatching { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null) }
    return false
}

actual fun clipboardHasContent(context: PlatformContext): Boolean = runCatching {
    Toolkit.getDefaultToolkit().systemClipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)
}.getOrDefault(false)

actual fun clipboardText(context: PlatformContext): String? = runCatching {
    Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String
}.getOrNull()

@Composable
actual fun editCommandLabel(command: EditCommand): String = when (command) {
    EditCommand.CUT -> EditorStrings.cut
    EditCommand.COPY -> EditorStrings.copy
    EditCommand.PASTE -> EditorStrings.paste
    EditCommand.SELECT_ALL -> EditorStrings.selectAll
}

actual val isPc: Boolean = true
