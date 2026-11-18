package io.github.falker47.socialviewer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RedditDiscussionPolicyTest {
    private val post = "https://www.reddit.com/r/AskReddit/comments/53scc5/example_post/"

    @Test
    fun offersTheResolvedPostAndPreservesAnExplicitCommentContext() {
        assertEquals(post, RedditDiscussionPolicy.urlFor("reddit", post))
        val comment = post + "d7v2abc/"
        assertEquals(comment, RedditDiscussionPolicy.urlFor("reddit", comment))
    }

    @Test
    fun normalizesLegacyHostsAndDropsTrackingWithoutAnotherNetworkRequest() {
        assertEquals(post, RedditDiscussionPolicy.urlFor("reddit", post.replace("www.reddit.com", "old.reddit.com")))
        assertEquals(post, RedditDiscussionPolicy.urlFor("reddit", "$post?utm_source=share#ignored"))
    }

    @Test
    fun otherProvidersNeverOfferTheAction() {
        listOf("tiktok", "instagram", "threads", "youtube", "pinterest", "x", "bluesky").forEach {
            assertNull(it, RedditDiscussionPolicy.urlFor(it, post))
        }
    }

    @Test
    fun rejectsUnresolvedAliasesAndNonContentNavigation() {
        listOf(
            "https://www.reddit.com/",
            "https://www.reddit.com/r/AskReddit/",
            "https://www.reddit.com/u/example/",
            "https://www.reddit.com/r/AskReddit/s/opaque123",
            "https://redd.it/53scc5",
        ).forEach { assertNull(it, RedditDiscussionPolicy.urlFor("reddit", it)) }
    }

    @Test
    fun rejectsUnsafeOriginsCredentialsPortsAndEncodedIdentities() {
        listOf(
            post.replace("https:", "http:"),
            post.replace("www.reddit.com", "reddit.com.evil.example"),
            post.replace("www.reddit.com", "www.reddit.com@evil.example"),
            post.replace("www.reddit.com", "user@www.reddit.com"),
            post.replace("www.reddit.com", "www.reddit.com:8443"),
            post.replace("53scc5", "%35%33scc5"),
            "javascript:alert(1)",
            "not a URI",
        ).forEach { assertNull(it, RedditDiscussionPolicy.urlFor("reddit", it)) }
    }
}
