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

    private fun isValidShortcode(value: String): Boolean =
        value.isNotBlank() &&
            value.all { char ->
                char.isLetterOrDigit() || char == '_' || char == '-'
            }
}
