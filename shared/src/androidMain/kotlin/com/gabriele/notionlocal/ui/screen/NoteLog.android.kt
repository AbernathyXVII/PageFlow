package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Diagnostica temporanea per i difetti delle caselle da spuntare che
 * **non si riescono a riprodurre da computer**: il cursore che salta a
 * inizio riga e l'Invio che cancella quello che si è scritto.
 *
 * Dipendono dalla tastiera vera — `adb shell input text` scrive senza
 * passare dall'IME, quindi la composizione delle parole, la maiuscola
 * automatica e i tempi reali non entrano mai in gioco. L'unico modo di
 * vederli è che li riproduca una persona col dito e che qui resti
 * scritto cosa è arrivato davvero al campo.
 *
 * Si legge con `adb logcat -d -s NOTE:D`. **Da togliere** quando i due
 * difetti sono chiusi.
 */
internal actual fun noteLog(message: String) {
    android.util.Log.d("NOTE", message)
}
