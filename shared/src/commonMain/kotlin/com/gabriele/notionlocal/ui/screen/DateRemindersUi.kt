package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.REMINDER_PRESETS
import com.gabriele.notionlocal.data.entity.ReminderUnit
import com.gabriele.notionlocal.data.entity.isValidReminder
import com.gabriele.notionlocal.data.entity.normalizeReminders
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.i18n.ReminderTexts
import com.gabriele.notionlocal.ui.i18n.Strings

/**
 * **Le notifiche di una data, come su Google Calendar** (dal 26/10/2026,
 * chiesto dall'utente: "la notifica dieci ore prima, tre giorni prima, due
 * settimane prima"): sotto "Include notification" acceso, una riga per
 * ognuna con la X per toglierla, e "Add notification" per aggiungerne
 * un'altra. Tolta l'ultima, la notifica si spegne ([onChange] con l'elenco
 * vuoto: decide chi chiama).
 *
 * [atStartLabel] è come si dice quella all'inizio, che dipende dalla data
 * ("At the start and at the end", "At the chosen time"…). [indent] allinea
 * le righe col testo sopra.
 */
@Composable
fun ReminderList(
    reminders: List<Int>,
    atStartLabel: String,
    onChange: (List<Int>) -> Unit,
    indent: Dp
) {
    var adding by remember { mutableStateOf(false) }
    var custom by remember { mutableStateOf(false) }
    fun label(minutes: Int) = if (minutes == 0) atStartLabel else ReminderTexts.before(minutes)
    Column(modifier = Modifier.fillMaxWidth()) {
        reminders.forEach { minutes ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = indent, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label(minutes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onChange(reminders - minutes) }) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = ReminderTexts.removeNotification,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { adding = true }
                .padding(start = indent, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = ReminderTexts.addNotification,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
    if (adding) {
        AddReminderDialog(
            taken = reminders,
            label = ::label,
            onDismiss = { adding = false },
            onPick = {
                adding = false
                onChange(normalizeReminders(reminders + it))
            },
            onCustom = {
                adding = false
                custom = true
            }
        )
    }
    if (custom) {
        CustomReminderDialog(
            onDismiss = { custom = false },
            onDone = {
                custom = false
                onChange(normalizeReminders(reminders + it))
            }
        )
    }
}

/** Le notifiche che si aggiungono con un tocco (quelle che non ci sono già), e "Custom…". */
@Composable
private fun AddReminderDialog(
    taken: List<Int>,
    label: (Int) -> String,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
    onCustom: () -> Unit
) {
    FitAlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } },
        title = { Text(ReminderTexts.addNotification) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                REMINDER_PRESETS.filter { it !in taken }.forEach { minutes ->
                    Text(
                        text = label(minutes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(minutes) }
                            .padding(vertical = 12.dp)
                    )
                }
                HorizontalDivider()
                Text(
                    text = DbStrings.custom,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCustom() }
                        .padding(vertical = 12.dp)
                )
            }
        }
    )
}

/**
 * **"Custom…"**, come su Google Calendar: un numero, e minuti, ore, giorni o
 * settimane prima. Al massimo un anno.
 */
@Composable
private fun CustomReminderDialog(onDismiss: () -> Unit, onDone: (Int) -> Unit) {
    var amount by remember { mutableStateOf("10") }
    var unit by remember { mutableStateOf(ReminderUnit.HOURS) }
    val number = amount.toIntOrNull() ?: 0
    val valid = isValidReminder(number, unit)
    FitAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(ReminderTexts.customTitle) },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onDone(number * unit.minutes) }) { Text(Strings.done) }
        },
        text = {
            Column {
                AutoCaps {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { typed -> amount = typed.filter { it.isDigit() }.take(3) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(96.dp)
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                ReminderUnit.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { unit = option },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = unit == option, onClick = { unit = option })
                        Text(
                            text = ReminderTexts.unitName(option, number),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                // Un numero troppo grande per l'unità scelta: si dice perché "Done" è spento.
                if (number >= 1 && !valid) {
                    Text(
                        text = ReminderTexts.tooFar,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    )
}
