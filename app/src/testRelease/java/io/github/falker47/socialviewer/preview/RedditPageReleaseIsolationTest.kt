package io.github.falker47.socialviewer.preview

import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class RedditPageReleaseIsolationTest {
    @Test fun releaseNeverSelectsThePagePreview() {
        assertNull(redditPagePreviewContent("https://www.reddit.com/r/AskReddit/comments/53scc5/what_video_game/"))
    }

    @Test fun experimentalRendererAndPolicyAreNotOnTheReleaseClasspath() {
        listOf("RedditPagePolicy", "RedditPageClient", "RedditPageWebViewKt").forEach { name ->
            try {
                Class.forName("io.github.falker47.socialviewer.preview.$name")
                fail("Debug-only prototype leaked into release: $name")
            } catch (_: ClassNotFoundException) {
                // Expected: the release source set contains only the no-op bridge.
            }
        }
    }
}
