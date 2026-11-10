package com.gabriele.notionlocal.data.widgets

import com.gabriele.notionlocal.platform.PlatformContext

actual fun prepareCounterSounds(context: PlatformContext) = DesktopSounds.prepare()

actual fun playCounterSound(sound: CounterSound, volume: Int) = DesktopSounds.play(sound, volume)
