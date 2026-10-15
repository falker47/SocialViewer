package io.github.falker47.socialviewer.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import io.github.falker47.socialviewer.domain.SocialContent

/** Temporary, debug-only carrier. Release has a no-op bridge with the same signature. */
internal fun redditPagePreviewContent(canonicalUrl: String): SocialContent? {
    val target = RedditPagePolicy.canonicalTarget(canonicalUrl) ?: return null
    return SocialContent(
        providerId = "reddit",
        providerName = "Reddit",
        canonicalUrl = target,
        title = null,
        authorName = null,
        documentBaseUrl = target,
        embedHtml = "",
    )
}

@Composable
internal fun RedditPagePreview(
    html: String,
    baseUrl: String,
    modifier: Modifier,
    onContentReady: () -> Unit,
): Boolean {
    if (!RedditPagePolicy.isPreviewDocument(html, baseUrl)) return false
    val target = requireNotNull(RedditPagePolicy.canonicalTarget(baseUrl))
    key(target) {
        RedditPageWebView(target, modifier, onContentReady)
    }
    return true
}
