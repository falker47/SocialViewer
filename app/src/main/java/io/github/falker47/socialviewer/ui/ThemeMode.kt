package io.github.falker47.socialviewer.ui

internal enum class ThemeMode(
    val storageValue: String,
) {
    Light(storageValue = "light"),
    Dark(storageValue = "dark");

    companion object {
        fun fromStorage(value: String?, systemDark: Boolean): ThemeMode =
            entries.firstOrNull { it.storageValue == value }
                ?: if (systemDark) Dark else Light
    }
}

internal val ThemeMode.isDark: Boolean
    get() = this == ThemeMode.Dark
