package io.github.falker47.socialviewer.provider.threads

object ThreadsUrlPolicy {
    private val allowedHosts = setOf(
        "threads.com",
        "www.threads.com",
        "threads.net",
        "www.threads.net",
    )

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

        return when {
            segments.size == 3 &&
                segments[0].startsWith("@") &&
                segments[1].equals("post", ignoreCase = true) -> {
                val username = segments[0].drop(1)
                val shortcode = segments[2]
                if (!isValidUsername(username) || !isValidShortcode(shortcode)) return null
                "https://www.threads.com/@$username/post/$shortcode/"
            }

            segments.size == 2 && segments[0].equals("t", ignoreCase = true) -> {
                val shortcode = segments[1]
                if (!isValidShortcode(shortcode)) return null
                "https://www.threads.com/t/$shortcode/"
            }

            else -> null
        }
    }

    private fun isValidUsername(value: String): Boolean =
        value.isNotBlank() &&
            value.all { char ->
                char.isLetterOrDigit() || char == '_' || char == '.'
            }

    private fun isValidShortcode(value: String): Boolean =
        value.isNotBlank() &&
            value.all { char ->
                char.isLetterOrDigit() || char == '_' || char == '-'
            }
}
