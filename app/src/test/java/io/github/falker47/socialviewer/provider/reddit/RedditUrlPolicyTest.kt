package io.github.falker47.socialviewer.provider.reddit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RedditUrlPolicyTest {
    @Test
    fun supportsCanonicalPostsAndSingleComments() {
        assertTrue(
            RedditUrlPolicy.supports(
                "https",
                "www.reddit.com",
                "/r/android/comments/1abc234/example_post/",
            ),
        )
        assertTrue(
            RedditUrlPolicy.supports(
                "https",
                "old.reddit.com",
                "/r/android/comments/1abc234/example_post/def567/",
            ),
        )
    }

    @Test
    fun canonicalizesSupportedPermalinksToWwwReddit() {
        assertEquals(
            "https://www.reddit.com/r/android/comments/1abc234/example_post/",
            RedditUrlPolicy.canonicalPermalink(
                "https",
                "new.reddit.com",
                "/r/android/comments/1abc234/example_post/",
            ),
        )
        assertEquals(
            "https://www.reddit.com/r/android/comments/1abc234/example_post/def567/",
            RedditUrlPolicy.canonicalPermalink(
                "https",
                "reddit.com",
                "/r/android/comments/1abc234/example_post/def567/",
            ),
        )
    }

    @Test
    fun recognizesOnlyBoundedShareAndShortAliasesForRedirectResolution() {
        assertTrue(
            RedditUrlPolicy.requiresRedirectResolution(
                "https",
                "www.reddit.com",
                "/r/android/s/AbC123_xYz/",
            ),
        )
        assertTrue(
            RedditUrlPolicy.requiresRedirectResolution(
                "https",
                "reddit.com",
                "/u/example_user/s/AbC123_xYz/",
            ),
        )
        assertTrue(
            RedditUrlPolicy.requiresRedirectResolution(
                "https",
                "reddit.com",
                "/s/AbC123_xYz/",
            ),
        )
        assertTrue(
            RedditUrlPolicy.requiresRedirectResolution(
                "https",
                "redd.it",
                "/1abc234/",
            ),
        )

        assertFalse(
            RedditUrlPolicy.requiresRedirectResolution(
                "https",
                "www.reddit.com",
                "/r/android/",
            ),
        )
        assertFalse(
            RedditUrlPolicy.requiresRedirectResolution(
                "https",
                "example.com",
                "/s/AbC123_xYz/",
            ),
        )
    }

    @Test
    fun rejectsFeedsProfilesHttpLookalikesAndExtraCommentTreePaths() {
        assertFalse(RedditUrlPolicy.supports("https", "www.reddit.com", "/r/android/"))
        assertFalse(RedditUrlPolicy.supports("https", "www.reddit.com", "/user/example/"))
        assertFalse(
            RedditUrlPolicy.supports(
                "https",
                "www.reddit.com",
                "/r/android/comments/1abc234/example_post/def567/child/",
            ),
        )
        assertFalse(
            RedditUrlPolicy.supports(
                "http",
                "www.reddit.com",
                "/r/android/comments/1abc234/example_post/",
            ),
        )
        assertFalse(
            RedditUrlPolicy.supports(
                "https",
                "reddit.example.com",
                "/r/android/comments/1abc234/example_post/",
            ),
        )
        assertNull(
            RedditUrlPolicy.canonicalPermalink(
                "https",
                "www.reddit.com",
                "/r/android/comments/",
            ),
        )
    }
}
