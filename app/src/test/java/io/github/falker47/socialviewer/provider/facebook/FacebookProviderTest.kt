package io.github.falker47.socialviewer.provider.facebook

import io.github.falker47.socialviewer.network.UrlConnectionHttpClient
import io.github.falker47.socialviewer.provider.ProviderContentUnavailableException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FacebookProviderTest {
    @Test
    fun resolvesPostOEmbedIntoOfficialFacebookDocument() {
        val http = FakeHttpClient(
            UrlConnectionHttpClient.Response(
                statusCode = 200,
                finalUrl = "https://graph.facebook.com/v25.0/oembed_post",
                body = """{"html":"<div id=\"fb-root\"></div><script async src=\"https://connect.facebook.net/en_US/sdk.js\"></script><div class=\"fb-post\" data-href=\"https://www.facebook.com/alice/posts/123\"></div>","title":"Example post","author_name":"Alice"}""",
            ),
        )

        val content = resolveCanonicalFacebook(
            target = FacebookCanonicalTarget(
                kind = FacebookContentKind.POST,
                canonicalUrl = "https://www.facebook.com/alice/posts/123/",
            ),
            http = http,
        )

        assertEquals("facebook", content.providerId)
        assertEquals("Facebook", content.providerName)
        assertEquals("https://www.facebook.com/alice/posts/123/", content.canonicalUrl)
        assertEquals("https://www.facebook.com/", content.documentBaseUrl)
        assertEquals("Example post", content.title)
        assertEquals("Alice", content.authorName)
        assertTrue(content.embedHtml.contains("fb-post"))
        assertTrue(content.embedHtml.contains("https://connect.facebook.net/en_US/sdk.js#xfbml=1&version=v25.0"))
        assertFalse(content.embedHtml.contains("""src="https://connect.facebook.net/en_US/sdk.js"></script>"""))
        assertTrue(
            http.requestedUrl.orEmpty()
                .startsWith("https://graph.facebook.com/v25.0/oembed_post?url="),
        )
    }

    @Test
    fun resolvesReelThroughVideoOEmbedEndpoint() {
        val http = FakeHttpClient(
            UrlConnectionHttpClient.Response(
                statusCode = 200,
                finalUrl = "https://graph.facebook.com/v25.0/oembed_video",
                body = """{"html":"<div class=\"fb-video\" data-href=\"https://www.facebook.com/reel/456\"></div><script src=\"https://connect.facebook.net/en_US/sdk.js#xfbml=1\"></script>"}""",
            ),
        )

        val content = resolveCanonicalFacebook(
            target = FacebookCanonicalTarget(
                kind = FacebookContentKind.REEL,
                canonicalUrl = "https://www.facebook.com/reel/456/",
            ),
            http = http,
        )

        assertTrue(content.embedHtml.contains("fb-video"))
        assertTrue(
            http.requestedUrl.orEmpty()
                .startsWith("https://graph.facebook.com/v25.0/oembed_video?url="),
        )
    }

    @Test
    fun resolvesShareReelAliasBeforeCallingOfficialVideoOEmbed() {
        val http = FakeHttpClient(
            response = UrlConnectionHttpClient.Response(
                statusCode = 200,
                finalUrl = "https://graph.facebook.com/v25.0/oembed_video",
                body = """{"html":"<div class=\"fb-video\"></div><script src=\"https://connect.facebook.net/en_US/sdk.js\"></script>"}""",
            ),
            resolvedUrl = "https://www.facebook.com/reel/1254780559682152/?rdid=test",
        )

        val target = resolveFacebookTarget(
            scheme = "https",
            host = "www.facebook.com",
            path = "/share/r/1HNwyf2jVo/",
            http = http,
        )
        val content = resolveCanonicalFacebook(target, http)

        assertEquals(FacebookContentKind.REEL, target.kind)
        assertEquals(
            "https://www.facebook.com/reel/1254780559682152/",
            target.canonicalUrl,
        )
        assertEquals(
            "https://www.facebook.com/share/r/1HNwyf2jVo/",
            http.requestedResolveUrl,
        )
        assertTrue(
            http.requestedUrl.orEmpty()
                .startsWith("https://graph.facebook.com/v25.0/oembed_video?url="),
        )
        assertEquals("facebook", content.providerId)
    }

    @Test
    fun rejectsShareReelAliasThatRedirectsOutsideSupportedFacebookReel() {
        val error = runCatching {
            resolveFacebookTarget(
                scheme = "https",
                host = "www.facebook.com",
                path = "/share/r/1HNwyf2jVo/",
                http = FakeHttpClient(
                    response = UrlConnectionHttpClient.Response(
                        statusCode = 200,
                        finalUrl = "https://graph.facebook.com/v25.0/oembed_video",
                        body = "{}",
                    ),
                    resolvedUrl = "https://example.com/reel/1254780559682152/",
                ),
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error?.message.orEmpty().contains("non ha risolto"))
    }

    @Test(expected = ProviderContentUnavailableException::class)
    fun expectedFacebookUnavailabilityUsesTypedError() {
        resolveCanonicalFacebook(
            target = FacebookCanonicalTarget(
                kind = FacebookContentKind.POST,
                canonicalUrl = "https://www.facebook.com/alice/posts/123/",
            ),
            http = FakeHttpClient(
                UrlConnectionHttpClient.Response(
                    statusCode = 400,
                    finalUrl = "https://graph.facebook.com/v25.0/oembed_post",
                    body = "{}",
                ),
            ),
        )
    }

    @Test
    fun unexpectedFacebookFailureRemainsUnexpected() {
        val error = runCatching {
            resolveCanonicalFacebook(
                target = FacebookCanonicalTarget(
                    kind = FacebookContentKind.REEL,
                    canonicalUrl = "https://www.facebook.com/reel/456/",
                ),
                http = FakeHttpClient(
                    UrlConnectionHttpClient.Response(
                        statusCode = 500,
                        finalUrl = "https://graph.facebook.com/v25.0/oembed_video",
                        body = "{}",
                    ),
                ),
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertFalse(error is ProviderContentUnavailableException)
        assertTrue(error?.message.orEmpty().contains("HTTP 500"))
    }

    private class FakeHttpClient(
        private val response: UrlConnectionHttpClient.Response,
        private val resolvedUrl: String? = null,
    ) : UrlConnectionHttpClient() {
        var requestedUrl: String? = null
        var requestedResolveUrl: String? = null

        override fun resolveFinalUrl(url: String): String {
            requestedResolveUrl = url
            return resolvedUrl ?: url
        }

        override fun get(url: String): UrlConnectionHttpClient.Response {
            requestedUrl = url
            return response
        }
    }
}
