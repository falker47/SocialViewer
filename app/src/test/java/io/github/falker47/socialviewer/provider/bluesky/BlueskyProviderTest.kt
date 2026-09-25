package io.github.falker47.socialviewer.provider.bluesky

import io.github.falker47.socialviewer.network.UrlConnectionHttpClient
import io.github.falker47.socialviewer.provider.ProviderContentUnavailableException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlueskyProviderTest {
    @Test
    fun resolvesOfficialOEmbedResponseIntoProviderContent() {
        val http = FakeHttpClient(
            response = UrlConnectionHttpClient.Response(
                statusCode = 200,
                finalUrl = "https://embed.bsky.app/oembed",
                body = """{"type":"rich","provider_name":"Bluesky Social","author_name":"Bluesky (@bsky.app)","html":"<blockquote class=\"bluesky-embed\"></blockquote><script async src=\"https://embed.bsky.app/static/embed.js\"></script>"}""",
            ),
        )

        val content = resolveCanonicalBluesky(
            canonicalUrl = "https://bsky.app/profile/bsky.app/post/3l6oveex3ii2l",
            http = http,
        )

        assertEquals("bluesky", content.providerId)
        assertEquals("Bluesky", content.providerName)
        assertEquals(
            "https://bsky.app/profile/bsky.app/post/3l6oveex3ii2l",
            content.canonicalUrl,
        )
        assertEquals("https://bsky.app/", content.documentBaseUrl)
        assertEquals("Bluesky (@bsky.app)", content.authorName)
        assertTrue(content.embedHtml.contains("bluesky-embed"))
        assertTrue(content.embedHtml.contains("https://embed.bsky.app/static/embed.js"))
        assertTrue(content.embedHtml.contains("Questo post Bluesky non è disponibile."))

        val requestUrl = http.requestedUrl.orEmpty()
        assertTrue(requestUrl.startsWith("https://embed.bsky.app/oembed?url="))
        assertTrue(requestUrl.contains("format=json"))
        assertTrue(requestUrl.contains("maxwidth=550"))
        assertTrue(
            requestUrl.contains(
                "https%3A%2F%2Fbsky.app%2Fprofile%2Fbsky.app%2Fpost%2F3l6oveex3ii2l",
            ),
        )
    }

    @Test
    fun toleratesHistoricMissingProviderName() {
        val content = resolveCanonicalBluesky(
            canonicalUrl = "https://bsky.app/profile/bsky.app/post/3l6oveex3ii2l",
            http = FakeHttpClient(
                response = UrlConnectionHttpClient.Response(
                    statusCode = 200,
                    finalUrl = "https://embed.bsky.app/oembed",
                    body = """{"type":"rich","html":"<blockquote class=\"bluesky-embed\"></blockquote>"}""",
                ),
            ),
        )

        assertEquals("bluesky", content.providerId)
    }

    @Test(expected = ProviderContentUnavailableException::class)
    fun mapsExpectedUnavailableOEmbedResponse() {
        resolveCanonicalBluesky(
            canonicalUrl = "https://bsky.app/profile/bsky.app/post/3aaaaaaaaaaaa",
            http = FakeHttpClient(
                response = UrlConnectionHttpClient.Response(
                    statusCode = 404,
                    finalUrl = "https://embed.bsky.app/oembed",
                    body = "{}",
                ),
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOEmbedRedirectOutsideBlueskyEmbedService() {
        resolveCanonicalBluesky(
            canonicalUrl = "https://bsky.app/profile/bsky.app/post/3l6oveex3ii2l",
            http = FakeHttpClient(
                response = UrlConnectionHttpClient.Response(
                    statusCode = 200,
                    finalUrl = "https://example.com/oembed",
                    body = """{"type":"rich","html":"<blockquote></blockquote>"}""",
                ),
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnexpectedOEmbedProvider() {
        resolveCanonicalBluesky(
            canonicalUrl = "https://bsky.app/profile/bsky.app/post/3l6oveex3ii2l",
            http = FakeHttpClient(
                response = UrlConnectionHttpClient.Response(
                    statusCode = 200,
                    finalUrl = "https://embed.bsky.app/oembed",
                    body = """{"provider_name":"Example","type":"rich","html":"<blockquote></blockquote>"}""",
                ),
            ),
        )
    }

    private class FakeHttpClient(
        private val response: UrlConnectionHttpClient.Response,
    ) : UrlConnectionHttpClient() {
        var requestedUrl: String? = null

        override fun get(url: String): UrlConnectionHttpClient.Response {
            requestedUrl = url
            return response
        }
    }
}
