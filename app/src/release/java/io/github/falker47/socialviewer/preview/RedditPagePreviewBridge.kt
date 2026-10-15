@file:Suppress("UNUSED_PARAMETER")

package io.github.falker47.socialviewer.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.falker47.socialviewer.domain.SocialContent

// The experiment cannot activate in a release APK/AAB. Keep the existing oEmbed path.
internal fun redditPagePreviewContent(canonicalUrl: String): SocialContent? = null

@Composable
internal fun RedditPagePreview(
    html: String,
    baseUrl: String,
    modifier: Modifier,
    onContentReady: () -> Unit,
): Boolean = false
