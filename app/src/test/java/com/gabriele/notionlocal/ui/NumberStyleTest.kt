package com.gabriele.notionlocal.ui

import android.app.Application
import androidx.compose.runtime.snapshots.Snapshot
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.NumberFormatKind
import com.gabriele.notionlocal.data.entity.NumberSettings
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.settings.NumberStyle
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.screen.formatNumberValue
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **I numeri con più di tre cifre** (17/10/2026): in Settings si sceglie se
 * scriverli "2 026", "2026", "2.026", "2˙026" o, come prima, "2,026".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NumberStyleTest {

    private lateinit var before: NumberStyle

    /** Le impostazioni sono stato di Compose: si cambiano in un'istantanea (vedi `GlobalTimeFormatTest`). */
    private fun settings(change: () -> Unit) {
        Snapshot.withMutableSnapshot(change)
        Snapshot.sendApplyNotifications()
    }

    @Before
    fun setUp() {
        AppSettings.init(ApplicationProvider.getApplicationContext())
        before = AppSettings.numberStyle
    }

    @After
    fun tearDown() {
        settings { AppSettings.numberStyle = before }
    }

    @Test
    fun theYearInEveryStyle() {
        assertEquals("2\u00A0026", Formats.number("2026", NumberStyle.SPACE_GROUPS))
        assertEquals("2026", Formats.number("2026", NumberStyle.NO_GROUPS))
        assertEquals("2.026", Formats.number("2026", NumberStyle.DOT_GROUPS))
        assertEquals("2\u02D9026", Formats.number("2026", NumberStyle.HIGH_DOT_GROUPS))
        assertEquals("2,026", Formats.number("2026", NumberStyle.COMMA_GROUPS))
    }

    @Test
    fun decimalsAndBigNumbers() {
        assertEquals("1\u00A0234\u00A0567,50", Formats.number("1234567.50", NumberStyle.SPACE_GROUPS))
        assertEquals("1234567,5", Formats.number("1234567.5", NumberStyle.NO_GROUPS))
        assertEquals("-1\u02D9234\u02D9567,5", Formats.number("-1234567.5", NumberStyle.HIGH_DOT_GROUPS))
        assertEquals("1,234,567.5", Formats.number("1234567.5", NumberStyle.COMMA_GROUPS))
        // Fino a tre cifre non cambia niente; quello che non è un numero resta com'è.
        assertEquals("999", Formats.number("999", NumberStyle.SPACE_GROUPS))
        assertEquals("abc", Formats.number("abc", NumberStyle.HIGH_DOT_GROUPS))
    }

    @Test
    fun theSettingReachesTheCells() {
        settings { AppSettings.numberStyle = NumberStyle.NO_GROUPS }
        assertEquals("2026", formatNumberValue("2026", NumberSettings()))
        settings { AppSettings.numberStyle = NumberStyle.SPACE_GROUPS }
        assertEquals("€1\u00A0500,25", formatNumberValue("1500.25", NumberSettings(format = NumberFormatKind.EUR)))
        // Un prezzo indicativo (dal 24/11/2026): formattato, con la ~ davanti.
        assertEquals("~€1\u00A0500,25", formatNumberValue("~1500.25", NumberSettings(format = NumberFormatKind.EUR)))
    }
}
