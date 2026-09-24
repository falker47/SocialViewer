package io.github.falker47.socialviewer.provider.youtube

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal object YouTubeUrlPolicy {
    private val videoIdPattern = Regex("^[A-Za-z0-9_-]{11}$")
    private val youtubeHosts = setOf(
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com",
    )

    fun supports(
        scheme: String?,
        host: String?,
        path: String?,
        encodedQuery: String?,
    ): Boolean = videoId(scheme, host, path, encodedQuery) != null

    fun videoId(
        scheme: String?,
        host: String?,
        path: String?,
        encodedQuery: String?,
    ): String? {
        if (!scheme.equals("https", ignoreCase = true)) return null

        val normalizedHost = host
            ?.lowercase()
            ?.removeSuffix(".")
            ?: return null

        val segments = path
            .orEmpty()
            .split('/')
            .filter { it.isNotBlank() }

        val candidate = when {
            normalizedHost == "youtu.be" && segments.size == 1 ->
                segments.single()

            normalizedHost in youtubeHosts && segments.isEmpty() ->
                null

            normalizedHost in youtubeHosts && segments.firstOrNull() == "watch" ->
                queryParameter(encodedQuery, "v")

            normalizedHost in youtubeHosts &&
                segments.size == 2 &&
                segments.first() in setOf("shorts", "live") ->
                segments[1]

            else -> null
        }

        return candidate?.takeIf(videoIdPattern::matches)
    }

    fun canonicalWatchUrl(videoId: String): String {
        require(videoIdPattern.matches(videoId)) { "YouTube video ID non valido" }
        return "https://www.youtube.com/watch?v=$videoId"
    }

    private fun queryParameter(
        encodedQuery: String?,
        name: String,
    ): String? {
        if (encodedQuery.isNullOrBlank()) return null

        return encodedQuery
            .split('&')
            .asSequence()
            .map { pair ->
                val parts = pair.split('=', limit = 2)
                val key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8.name())
                val value = parts.getOrNull(1)
                    ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
                key to value
            }
            .firstOrNull { (key, _) -> key == name }
            ?.second
    }
}
