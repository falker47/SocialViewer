package io.github.falker47.socialviewer.preview

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Test

class RedditPagePolicyTest {
    private val post = "https://www.reddit.com/r/AskReddit/comments/53scc5/what_video_game/"

    @Test fun acceptsPostCommentAndSameThreadReplyNavigation() {
        assertEquals(post, RedditPagePolicy.canonicalTarget(post))
        assertEquals(post + "d7v2abc/", RedditPagePolicy.canonicalTarget(post + "d7v2abc/"))
        assertTrue(RedditPagePolicy.allowsNavigation(post, post + "d7v2abc/?context=3#reply"))
        assertTrue(RedditPagePolicy.allowsNavigation(post, post + "?sort=new"))
        assertTrue(RedditPagePolicy.allowsNavigation(post, "https://www.reddit.com/r/AskReddit/comments/53scc5/"))
    }

    @Test fun canonicalizesOnlyKnownRedditHostsWithoutNetwork() {
        listOf("reddit.com", "www.reddit.com", "old.reddit.com", "new.reddit.com", "m.reddit.com").forEach {
            assertEquals(post, RedditPagePolicy.canonicalTarget(post.replace("www.reddit.com", it) + "?utm_source=share"))
        }
        assertNull(RedditPagePolicy.canonicalTarget(post.replace("www.reddit.com", "unknown.reddit.com")))
    }

    @Test fun rejectsUnsafeUrlsAndEncodedPathIdentities() {
        listOf(
            post.replace("https:", "http:"), post.replace("www.reddit.com", "reddit.com.evil.test"),
            post.replace("www.reddit.com", "user@www.reddit.com"),
            post.replace("www.reddit.com", "www.reddit.com:8443"),
            post.replace("53scc5", "%35%33scc5"), post.replace("/comments/", "/%63omments/"),
            post.replace("/comments/", "//comments/"), post + "extra/segment/",
            "file:///etc/passwd", "content://data", "javascript:alert(1)", "intent://reddit", "not a URL",
        ).forEach { assertNull(it, RedditPagePolicy.canonicalTarget(it)) }
        assertEquals(post, RedditPagePolicy.canonicalTarget(post.replace("www.reddit.com", "www.reddit.com:443")))
    }

    @Test fun doesNotTurnThePreviewIntoAFeedProfileOrLoginBrowser() {
        listOf("/", "/r/AskReddit/", "/user/example/", "/login/", "/register/", "/search/", "/s/opaque", "/comments/53scc5/").forEach {
            assertFalse(it, RedditPagePolicy.allowsNavigation(post, "https://www.reddit.com$it"))
        }
        assertFalse(RedditPagePolicy.allowsNavigation(post, post.replace("53scc5", "other123")))
        assertFalse(RedditPagePolicy.allowsNavigation(post, post.replace("AskReddit", "other")))
        assertFalse(RedditPagePolicy.allowsNavigation(post, "https://example.com/"))
    }

    @Test fun initialPageMustItselfBeValidAndOnlyGetNavigationIsAllowed() {
        assertFalse(RedditPagePolicy.allowsNavigation("https://example.com/", post))
        assertFalse(RedditPagePolicy.allowsNavigation(post, post, "POST"))
        assertFalse(RedditPagePolicy.allowsNavigation(post, post, "DELETE"))
        assertTrue(RedditPagePolicy.allowsNavigation(post, post, "GET"))
    }

    @Test fun previewCarrierNeverClaimsAnExistingEmbedOrOtherProvider() {
        assertTrue(RedditPagePolicy.isPreviewDocument("", post))
        assertFalse(RedditPagePolicy.isPreviewDocument("<iframe>reddit</iframe>", post))
        assertFalse(RedditPagePolicy.isPreviewDocument("", "https://www.reddit.com/"))
        listOf("https://www.tiktok.com/", "https://www.instagram.com/", "https://www.threads.com/", "https://www.youtube.com/", "https://www.pinterest.com/", "https://x.com/", "https://bsky.app/").forEach {
            assertFalse(RedditPagePolicy.isPreviewDocument("", it))
        }
    }
}
