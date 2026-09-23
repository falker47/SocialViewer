package io.github.falker47.socialviewer.ui

internal enum class ThemeMode(
    val storageValue: String,
    val displayLabel: String,
) {
    System(storageValue = "system", displayLabel = "Sistema"),
    Light(storageValue = "light", displayLabel = "Chiaro"),
    Dark(storageValue = "dark", displayLabel = "Scuro"),
    ;

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: System
    }
}

internal fun ThemeMode.resolveDark(systemDark: Boolean): Boolean = when (this) {
    ThemeMode.System -> systemDark
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}
