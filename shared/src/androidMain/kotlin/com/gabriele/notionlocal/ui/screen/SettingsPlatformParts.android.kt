package com.gabriele.notionlocal.ui.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import com.gabriele.notionlocal.platform.PlatformContext
import com.gabriele.notionlocal.platform.mainActivityIntent

/**
 * Fa ripartire l'app da capo, su una My Space vuota. Le schermate aperte
 * hanno ancora in mano il contenuto di prima — un Annulla rimetterebbe i
 * blocchi cancellati — e con l'attività chiusa se ne vanno anche loro.
 */
actual fun restartAppAfterReset(context: PlatformContext) {
    val activity = context.findActivity() ?: return
    val intent = mainActivityIntent(activity)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    activity.startActivity(intent)
    activity.finish()
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

actual fun appVersionName(context: PlatformContext): String? =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
