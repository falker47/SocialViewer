package io.github.falker47.socialviewer.provider.facebook

internal enum class FacebookContentKind {
    POST,
    REEL,
}

internal data class FacebookCanonicalTarget(
    val kind: FacebookContentKind,
    val canonicalUrl: String,
)

object FacebookUrlPolicy {
    private val allowedHosts = setOf(
        "facebook.com",
        "www.facebook.com",
    )

    fun supports(
        scheme: String?,
        host: String?,
        path: String?,
    ): Boolean = canonicalTarget(scheme, host, path) != null

    fun canonicalUrl(
        scheme: String?,
        host: String?,
        path: String?,
    ): String? = canonicalTarget(scheme, host, path)?.canonicalUrl

    fun canonicalTarget(
        scheme: String?,
        host: String?,
        path: String?,
    ): FacebookCanonicalTarget? {
        if (!scheme.equals("https", ignoreCase = true)) return null

        val normalizedHost = host?.lowercase() ?: return null
        if (normalizedHost !in allowedHosts) return null

        val segments = path
            ?.trim('/')
            ?.split('/')
            ?.filter { it.isNotBlank() }
            .orEmpty()

        if (segments.size == 2 && segments[0].equals("reel", ignoreCase = true)) {
            val reelId = segments[1]
            if (!isSafeSegment(reelId)) return null

            return FacebookCanonicalTarget(
                kind = FacebookContentKind.REEL,
                canonicalUrl = "https://www.facebook.com/reel/$reelId/",
            )
        }

        if (segments.size == 3 && segments[1].equals("posts", ignoreCase = true)) {
            val owner = segments[0]
            val postId = segments[2]
            if (!isSafeSegment(owner) || !isSafeSegment(postId)) return null

            return FacebookCanonicalTarget(
                kind = FacebookContentKind.POST,
                canonicalUrl = "https://www.facebook.com/$owner/posts/$postId/",
            )
        }

        return null
    }

    private fun isSafeSegment(value: String): Boolean =
        value.isNotBlank() &&
            value.length <= 256 &&
            value.all { char ->
                char.isLetterOrDigit() || char == '.' || char == '_' || char == '-'
            }
}
