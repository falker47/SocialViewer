package io.github.falker47.socialviewer.provider.x

object XUrlPolicy {
    private val supportedHosts = setOf(
        "x.com",
        "www.x.com",
        "twitter.com",
        "www.twitter.com",
    )

    fun supports(
        scheme: String?,
        host: String?,
        path: String?,
    ): Boolean = canonicalPostUrl(scheme, host, path) != null

    fun canonicalPostUrl(
        scheme: String?,
        host: String?,
        path: String?,
    ): String? {
        if (!scheme.equals("https", ignoreCase = true)) return null

        val normalizedHost = host?.lowercase() ?: return null
        if (normalizedHost !in supportedHosts) return null

        val segments = pathSegments(path)
        if (segments.size != 3) return null
        if (!segments[1].equals("status", ignoreCase = true)) return null

        val username = segments[0]
        val postId = segments[2]
        if (!isValidUsername(username) || !isValidPostId(postId)) return null

        return "https://x.com/$username/status/$postId"
    }

    private fun pathSegments(path: String?): List<String> =
        path
            ?.trim('/')
            ?.split('/')
            ?.filter { it.isNotBlank() }
            .orEmpty()

    private fun isValidUsername(value: String): Boolean =
        value.isNotBlank() &&
            value.length <= 15 &&
            value.all { char ->
                char in 'a'..'z' ||
                    char in 'A'..'Z' ||
                    char in '0'..'9' ||
                    char == '_'
            }

    private fun isValidPostId(value: String): Boolean =
        value.isNotBlank() && value.all { it in '0'..'9' }
}
