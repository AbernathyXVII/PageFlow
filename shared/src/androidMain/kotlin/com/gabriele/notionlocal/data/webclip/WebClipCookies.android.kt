package com.gabriele.notionlocal.data.webclip

import android.webkit.CookieManager

actual fun webClipCookies(url: String): String? = runCatching { CookieManager.getInstance().getCookie(url) }.getOrNull()
