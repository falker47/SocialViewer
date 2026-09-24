package io.github.falker47.socialviewer.ui

import android.content.SharedPreferences

private const val PREF_THEME_MODE = "theme_mode"

internal fun readThemeMode(preferences: SharedPreferences): ThemeMode =
    ThemeMode.fromStorage(preferences.getString(PREF_THEME_MODE, null))

internal fun writeThemeMode(
    preferences: SharedPreferences,
    mode: ThemeMode,
) {
    preferences.edit().putString(PREF_THEME_MODE, mode.storageValue).apply()
}
