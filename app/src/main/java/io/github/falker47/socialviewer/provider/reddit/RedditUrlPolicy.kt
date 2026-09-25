package io.github.falker47.socialviewer.provider.reddit

object RedditUrlPolicy {
    private val directHosts = setOf(
        "reddit.com",
        "www.reddit.com",
        "old.reddit.com",
        "new.reddit.com",
        "m.reddit.com",
    )

    private val redirectHosts = directHosts + "redd.it"

    fun supports(
        scheme: String?,
        host: String?,
        path: String?,
    ): Boolean =
        canonicalPermalink(scheme, host, path) != null ||
            requiresRedirectResolution(scheme, host, path)

    fun canonicalPermalink(
        scheme: String?,
        host: String?,
        path: String?,
    ): String? {
        if (!scheme.equals("https", ignoreCase = true)) return null

        val normalizedHost = host?.lowercase() ?: return null
        if (normalizedHost !in directHosts) return null

        val segments = pathSegments(path)
        if (segments.size !in 4..6) return null
        if (!segments[0].equals("r", ignoreCase = true)) return null
        if (!segments[2].equals("comments", ignoreCase = true)) return null

        val subreddit = segments[1]
        val postId = segments[3]
        if (!isValidSubreddit(subreddit) || !isValidThingId(postId)) return null

        if (segments.size >= 5 && !isValidSlug(segments[4])) return null
        if (segments.size == 6 && !isValidThingId(segments[5])) return null

        return buildString {
            append("https://www.reddit.com/r/")
            append(subreddit)
            append("/comments/")
            append(postId)
            append("/")
            if (segments.size >= 5) {
                append(segments[4])
                append("/")
            }
            if (segments.size == 6) {
                append(segments[5])
                append("/")
            }
        }
    }

    fun requiresRedirectResolution(
        scheme: String?,
        host: String?,
        path: String?,
    ): Boolean {
        if (!scheme.equals("https", ignoreCase = true)) return false

        val normalizedHost = host?.lowercase() ?: return false
        if (normalizedHost !in redirectHosts) return false

        val segments = pathSegments(path)

        if (normalizedHost == "redd.it") {
            return segments.size == 1 && isValidThingId(segments[0])
        }

        return segments.size == 4 &&
            segments[0].lowercase() in setOf("r", "u", "user") &&
            segments[2].equals("s", ignoreCase = true) &&
            isValidShareContext(segments[1]) &&
            isValidShareToken(segments[3])
    }

    private fun pathSegments(path: String?): List<String> =
        path
            ?.trim('/')
            ?.split('/')
            ?.filter { it.isNotBlank() }
            .orEmpty()

    private fun isValidSubreddit(value: String): Boolean =
        value.isNotBlank() &&
            value.all { it.isLetterOrDigit() || it == '_' }

    private fun isValidThingId(value: String): Boolean =
        value.isNotBlank() && value.all(Char::isLetterOrDigit)

    private fun isValidSlug(value: String): Boolean =
        value.isNotBlank() &&
            value.all { it.isLetterOrDigit() || it == '_' || it == '-' }

    private fun isValidShareContext(value: String): Boolean =
        value.isNotBlank() &&
            value.all { it.isLetterOrDigit() || it == '_' || it == '-' }

    private fun isValidShareToken(value: String): Boolean =
        value.isNotBlank() &&
            value.all { it.isLetterOrDigit() || it == '_' || it == '-' }
}
