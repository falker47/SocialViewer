package io.github.falker47.socialviewer.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

internal const val UI_PREFS_NAME = "social_viewer_ui"
private const val PREF_APP_LANGUAGE = "app_language"

internal enum class AppLanguage(
    val languageTag: String,
) {
    English(languageTag = "en"),
    Italian(languageTag = "it");

    companion object {
        fun fromLanguageTag(languageTag: String?): AppLanguage =
            if (languageTag?.lowercase()?.startsWith("it") == true) Italian else English
    }
}

internal fun initializeAppLanguage(context: Context) {
    val preferences = context.getSharedPreferences(UI_PREFS_NAME, Context.MODE_PRIVATE)
    val configuredLanguage = AppCompatDelegate.getApplicationLocales()
        .get(0)
        ?.language
    val storedLanguage = preferences.getString(PREF_APP_LANGUAGE, null)
    val systemLanguage = context.resources.configuration.locales[0].language
    val resolved = AppLanguage.fromLanguageTag(
        configuredLanguage ?: storedLanguage ?: systemLanguage,
    )

    if (storedLanguage != resolved.languageTag) {
        preferences.edit().putString(PREF_APP_LANGUAGE, resolved.languageTag).apply()
    }

    if (configuredLanguage != resolved.languageTag) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(resolved.languageTag),
        )
    }
}

internal fun setAppLanguage(
    context: Context,
    language: AppLanguage,
) {
    context.getSharedPreferences(UI_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(PREF_APP_LANGUAGE, language.languageTag)
        .apply()

    AppCompatDelegate.setApplicationLocales(
        LocaleListCompat.forLanguageTags(language.languageTag),
    )
}
