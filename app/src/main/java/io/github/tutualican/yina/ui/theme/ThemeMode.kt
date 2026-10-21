package io.github.tutualican.yina.ui.theme

import android.content.Context

/**
 * The user's appearance choice. [SYSTEM] follows the Android setting; the
 * other two are explicit overrides that survive app restarts.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

class ThemePreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("yina_settings", Context.MODE_PRIVATE)

    var mode: ThemeMode
        get() = ThemeMode.fromStorage(prefs.getString(KEY_THEME, null))
        set(value) {
            prefs.edit().putString(KEY_THEME, value.name).apply()
        }

    private companion object {
        const val KEY_THEME = "theme_mode"
    }
}
