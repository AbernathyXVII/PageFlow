package com.gabriele.notionlocal.data.settings

import android.content.Context
import android.content.SharedPreferences
import com.gabriele.notionlocal.platform.PlatformContext

/** Sul telefono: le `SharedPreferences` di sempre, con lo stesso nome. */
actual fun openSettingsStore(context: PlatformContext, name: String): SettingsStore =
    SharedPreferencesStore(context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE))

private class SharedPreferencesStore(private val prefs: SharedPreferences) : SettingsStore {
    override fun getString(key: String, defValue: String?): String? = prefs.getString(key, defValue)

    // Un backup fatto sul PC ha le impostazioni tutte come testo ("true", "3"): si leggono lo stesso.
    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        try { prefs.getBoolean(key, defValue) } catch (e: ClassCastException) { prefs.getString(key, null)?.toBooleanStrictOrNull() ?: defValue }
    override fun getInt(key: String, defValue: Int): Int =
        try { prefs.getInt(key, defValue) } catch (e: ClassCastException) { prefs.getString(key, null)?.toIntOrNull() ?: defValue }
    override fun getLong(key: String, defValue: Long): Long =
        try { prefs.getLong(key, defValue) } catch (e: ClassCastException) { prefs.getString(key, null)?.toLongOrNull() ?: defValue }
    override fun edit(): SettingsStore.Editor = Editor(prefs.edit())

    private class Editor(private val editor: SharedPreferences.Editor) : SettingsStore.Editor {
        override fun putString(key: String, value: String?) = apply { editor.putString(key, value) }
        override fun putBoolean(key: String, value: Boolean) = apply { editor.putBoolean(key, value) }
        override fun putInt(key: String, value: Int) = apply { editor.putInt(key, value) }
        override fun putLong(key: String, value: Long) = apply { editor.putLong(key, value) }
        override fun remove(key: String) = apply { editor.remove(key) }
        override fun apply() = editor.apply()
    }
}
