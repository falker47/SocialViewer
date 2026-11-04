package io.github.falker47.socialviewer.provider.instagram

object InstagramUrlPolicy {
    private val allowedHosts = setOf(
        "instagram.com",
        "www.instagram.com",
    )

    private val supportedKinds = setOf("p", "reel")

    fun supports(
        scheme: String?,
        host: String?,
        path: String?,
    ): Boolean = canonicalUrl(scheme, host, path) != null

    fun canonicalUrl(
        scheme: String?,
        host: String?,
        path: String?,
    ): String? {
        if (!scheme.equals("https", ignoreCase = true)) return null

        val normalizedHost = host?.lowercase() ?: return null
        if (normalizedHost !in allowedHosts) return null

        val segments = path
            ?.trim('/')
            ?.split('/')
            ?.filter { it.isNotBlank() }
            .orEmpty()

        if (segments.size != 2) return null

        val kind = segments[0].lowercase()
        val shortcode = segments[1]
        if (kind !in supportedKinds) return null
        if (!isValidShortcode(shortcode)) return null

        return "https://www.instagram.com/$kind/$shortcode/"
    }

    /**
     * External browser continuation observed after Instagram's "Continue on web" action.
     * The query flag is not documented as a stable API; apply it only to canonical
     * public Reel URLs for the user's explicit external browser action.
     */
    fun mobileWebReelUrl(permalink: String): String? {
        val parsed = runCatching { java.net.URI(permalink) }.getOrNull() ?: return null
        if (parsed.rawQuery != null || parsed.rawFragment != null) return null

        val canonical = canonicalUrl(parsed.scheme, parsed.host, parsed.path) ?: return null
        if (permalink != canonical || !canonical.startsWith("https://www.instagram.com/reel/")) {
            return null
        }

        return "$canonical?l=1"
    }

    private fun isValidShortcode(value: String): Boolean =
        value.isNotBlank() &&
            value.all { char ->
                char.isLetterOrDigit() || char == '_' || char == '-'
            }
}
