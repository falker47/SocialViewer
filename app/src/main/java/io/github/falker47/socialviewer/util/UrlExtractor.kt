package io.github.falker47.socialviewer.util

object UrlExtractor {
    private val urlRegex = Regex("https?://[^\\s<>]+", RegexOption.IGNORE_CASE)
    private val trailingPunctuation = charArrayOf('.', ',', ';', ':', '!', '?', ')', ']', '}', '\"', '\'')

    fun firstHttpUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val match = urlRegex.find(text)?.value ?: return null
        return match.trimEnd(*trailingPunctuation)
    }
}
