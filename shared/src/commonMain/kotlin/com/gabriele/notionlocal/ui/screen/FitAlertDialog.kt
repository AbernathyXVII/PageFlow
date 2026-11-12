package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gabriele.notionlocal.platform.isPc

/**
 * **Le finestre di conferma e di scelta** (le `AlertDialog` di Material).
 * Sul telefono sono quelle di Material, come sempre. **Sul PC sono larghe
 * quanto quello che c'è dentro** (dal 12/11/2026, chiesto dall'utente con le
 * foto di "Repeat" e "Custom recurrence": "rispetti il contenuto, cosicché
 * non ci sia spazio vuoto inutile"): Material le allarga fino a 560 punti
 * appena una riga dentro è larga quanto può, e lascia 24 punti di margine
 * intorno e prima dei pulsanti. Qui margini più stretti, e l'aspetto delle
 * finestrelle del PC (`PcPanel`): angoli da 10, bordo sottile, ombra.
 *
 * [fitContent] = false per quelle con dentro una lista che scorre
 * (`LazyColumn`) o delle schede (`TabRow`): a quelle non si può chiedere
 * quanto sono larghe, e l'app si chiuderebbe. Restano larghe come prima.
 */
@Composable
fun FitAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    containerColor: Color = AlertDialogDefaults.containerColor,
    properties: DialogProperties = DialogProperties(),
    fitContent: Boolean = true
) {
    if (!isPc) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = confirmButton,
            modifier = modifier,
            dismissButton = dismissButton,
            icon = icon,
            title = title,
            text = text,
            containerColor = containerColor,
            properties = properties
        )
        return
    }
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        Surface(
            modifier = modifier.sizeIn(minWidth = PC_DIALOG_MIN_WIDTH, maxWidth = PC_DIALOG_MAX_WIDTH),
            shape = RoundedCornerShape(10.dp),
            color = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = (if (fitContent) Modifier.width(IntrinsicSize.Max) else Modifier)
                    .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp)
            ) {
                if (icon != null) {
                    Box(modifier = Modifier.align(Alignment.CenterHorizontally).padding(end = 8.dp)) { icon() }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (title != null) {
                    val base = MaterialTheme.typography.titleSmall
                    ProvideTextStyle(base.copy(fontSize = base.fontSize * 1.25f, fontWeight = FontWeight.SemiBold)) {
                        Box(modifier = Modifier.padding(end = 8.dp)) { title() }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (text != null) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                            Box(modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)) { text() }
                        }
                    }
                }
                Row(
                    modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}

private val PC_DIALOG_MIN_WIDTH = 240.dp
private val PC_DIALOG_MAX_WIDTH = 560.dp
