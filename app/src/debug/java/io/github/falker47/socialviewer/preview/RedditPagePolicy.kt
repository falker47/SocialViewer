package io.github.falker47.socialviewer.preview

import java.net.URI

/** Development-only main-frame policy; it is not an API or a comments scraper. */
internal object RedditPagePolicy {
    private val hosts = setOf("reddit.com", "www.reddit.com", "old.reddit.com", "new.reddit.com", "m.reddit.com")
    private val subreddit = Regex("[A-Za-z0-9_]+")
    private val thingId = Regex("[A-Za-z0-9]+")
    private val slug = Regex("[A-Za-z0-9_-]+")

    fun canonicalTarget(url: String): String? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", ignoreCase = true) || uri.host?.lowercase() !in hosts) return null
        if (uri.rawUserInfo != null || uri.port !in setOf(-1, 443)) return null
        val path = uri.rawPath ?: return null
        if (path != uri.path || !path.startsWith('/') || path.contains("//")) return null
        val parts = path.removePrefix("/").removeSuffix("/").split('/')
        if (parts.size !in 4..6 || parts[0] != "r" || parts[2] != "comments") return null
        if (!subreddit.matches(parts[1]) || !thingId.matches(parts[3])) return null
        if (parts.size >= 5 && !slug.matches(parts[4])) return null
        if (parts.size == 6 && !thingId.matches(parts[5])) return null
        return "https://www.reddit.com/" + parts.joinToString("/") + "/"
    }

    fun allowsNavigation(initialUrl: String, targetUrl: String, method: String = "GET"): Boolean {
        if (method != "GET") return false
        val initial = canonicalTarget(initialUrl) ?: return false
        val target = canonicalTarget(targetUrl) ?: return false
        val a = URI(initial).path.split('/')
        val b = URI(target).path.split('/')
        return a[2].equals(b[2], ignoreCase = true) && a[4].equals(b[4], ignoreCase = true)
    }

    // Temporary prototype carrier: no markup plus a validated full Reddit permalink.
    // Existing embeds use non-empty markup and origin-only base URLs, and never match.
    fun isPreviewDocument(html: String, baseUrl: String): Boolean =
        html.isEmpty() && canonicalTarget(baseUrl) != null
}
