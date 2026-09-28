package io.github.falker47.socialviewer.ui

import android.content.SharedPreferences

private const val PREF_THEME_MODE = "theme_mode"

internal fun readThemeMode(
    preferences: SharedPreferences,
    systemDark: Boolean,
): ThemeMode {
    val stored = preferences.getString(PREF_THEME_MODE, null)
    val resolved = ThemeMode.fromStorage(stored, systemDark)
    if (stored != resolved.storageValue) {
        writeThemeMode(preferences, resolved)
    }
    return resolved
}

internal fun writeThemeMode(
    preferences: SharedPreferences,
    mode: ThemeMode,
) {
    preferences.edit().putString(PREF_THEME_MODE, mode.storageValue).apply()
}
