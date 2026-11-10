package com.gabriele.notionlocal.desktop

import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.ui.screen.Biometrics
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * **Windows Hello** (dal 10/11/2026): lo script si compila (anche il pezzo in
 * C# che passa a Windows la finestra di PageFlow) e Windows risponde se è
 * pronto. La finestra vera non si apre: aspetterebbe il volto o il PIN.
 */
class DesktopHelloTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun windowsSaysWhetherHelloIsReady() {
        val answer = Biometrics.check(DesktopContext(folder.newFolder("dati")))
        val known = setOf("Available", "DeviceNotPresent", "NotConfiguredForUser", "DisabledByPolicy", "DeviceBusy")
        assertTrue("risposta: $answer", answer in known)
    }
}
