package io.github.falker47.socialviewer.ui

import io.github.falker47.socialviewer.provider.reddit.RedditUrlPolicy
import java.net.URI

/** Only the already-resolved Reddit item may offer a browser discussion action. */
internal object RedditDiscussionPolicy {
    fun urlFor(providerId: String, canonicalUrl: String): String? {
        if (providerId != "reddit") return null
        val uri = runCatching { URI(canonicalUrl) }.getOrNull() ?: return null
        if (uri.rawUserInfo != null || uri.port !in setOf(-1, 443)) return null
        // Do not reinterpret escaped path separators or other encoded identities.
        if (uri.rawPath != uri.path) return null
        return RedditUrlPolicy.canonicalPermalink(uri.scheme, uri.host, uri.path)
    }
}
