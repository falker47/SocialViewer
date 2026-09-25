package io.github.falker47.socialviewer.provider.reddit

import io.github.falker47.socialviewer.network.UrlConnectionHttpClient
import io.github.falker47.socialviewer.provider.ProviderContentUnavailableException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RedditProviderTest {
    @Test
    fun resolvesOfficialOEmbedResponseIntoProviderContent() {
        val http = FakeHttpClient(
            response = UrlConnectionHttpClient.Response(
                statusCode = 200,
                finalUrl = "https://www.reddit.com/oembed",
                body = """{"provider_name":"reddit","type":"rich","html":"<blockquote class=\"reddit-embed-bq\"></blockquote><script async src=\"https://embed.reddit.com/widgets.js\"></script>","title":"Example post","author_name":"alice"}""",
            ),
        )

        val content = resolveCanonicalReddit(
            canonicalUrl = "https://www.reddit.com/r/android/comments/1abc234/example_post/",
            http = http,
        )

        assertEquals("reddit", content.providerId)
        assertEquals("Reddit", content.providerName)
        assertEquals(
            "https://www.reddit.com/r/android/comments/1abc234/example_post/",
            content.canonicalUrl,
        )
        assertEquals("https://www.reddit.com/", content.documentBaseUrl)
        assertEquals("Example post", content.title)
        assertEquals("alice", content.authorName)
        assertTrue(content.embedHtml.contains("reddit-embed-bq"))
        assertTrue(content.embedHtml.contains("https://embed.reddit.com/widgets.js"))
        assertTrue(
            http.requestedUrl.orEmpty()
                .startsWith("https://www.reddit.com/oembed?url="),
        )
    }

    @Test
    fun resolvesRedditShareAliasOnlyWhenFinalTargetIsSupportedPermalink() {
        val http = FakeHttpClient(
            finalUrl = "https://www.reddit.com/r/android/comments/1abc234/example_post/?share_id=tracking",
        )

        val canonical = canonicalRedditUrlFor(
            scheme = "https",
            host = "www.reddit.com",
            path = "/r/android/s/AbC123_xYz/",
            rawUrl = "https://www.reddit.com/r/android/s/AbC123_xYz/",
            http = http,
        )

        assertEquals(
            "https://www.reddit.com/r/android/comments/1abc234/example_post/",
            canonical,
        )
        assertEquals(
            "https://www.reddit.com/r/android/s/AbC123_xYz/",
            http.resolvedUrl,
        )
    }

    @Test
    fun resolvesReddItShortLinkThroughSameSafeRedirectGate() {
        val http = FakeHttpClient(
            finalUrl = "https://www.reddit.com/comments/1abc234/",
        )

        val canonical = canonicalRedditUrlFor(
            scheme = "https",
            host = "redd.it",
            path = "/1abc234/",
            rawUrl = "https://redd.it/1abc234",
            http = http,
        )

        assertEquals(
            "https://www.reddit.com/comments/1abc234/",
            canonical,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsShareAliasRedirectOutsideSupportedRedditPermalink() {
        canonicalRedditUrlFor(
            scheme = "https",
            host = "www.reddit.com",
            path = "/r/android/s/AbC123_xYz/",
            rawUrl = "https://www.reddit.com/r/android/s/AbC123_xYz/",
            http = FakeHttpClient(finalUrl = "https://example.com/content"),
        )
    }

    @Test(expected = ProviderContentUnavailableException::class)
    fun mapsExpectedUnavailableOEmbedResponse() {
        resolveCanonicalReddit(
            canonicalUrl = "https://www.reddit.com/r/android/comments/1abc234/example_post/",
            http = FakeHttpClient(
                response = UrlConnectionHttpClient.Response(
                    statusCode = 404,
                    finalUrl = "https://www.reddit.com/oembed",
                    body = "{}",
                ),
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOEmbedRedirectOutsideReddit() {
        resolveCanonicalReddit(
            canonicalUrl = "https://www.reddit.com/r/android/comments/1abc234/example_post/",
            http = FakeHttpClient(
                response = UrlConnectionHttpClient.Response(
                    statusCode = 200,
                    finalUrl = "https://example.com/oembed",
                    body = """{"provider_name":"reddit","html":"<blockquote></blockquote>"}""",
                ),
            ),
        )
    }

    private class FakeHttpClient(
        private val response: UrlConnectionHttpClient.Response = UrlConnectionHttpClient.Response(
            statusCode = 200,
            finalUrl = "https://www.reddit.com/oembed",
            body = """{"provider_name":"reddit","html":"<blockquote></blockquote>"}""",
        ),
        private val finalUrl: String? = null,
    ) : UrlConnectionHttpClient() {
        var requestedUrl: String? = null
        var resolvedUrl: String? = null

        override fun get(url: String): UrlConnectionHttpClient.Response {
            requestedUrl = url
            return response
        }

        override fun resolveFinalUrl(url: String): String {
            resolvedUrl = url
            return finalUrl ?: error("Nessun redirect configurato nel fake")
        }
    }
}
