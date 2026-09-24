package io.github.falker47.socialviewer.provider.instagram

import io.github.falker47.socialviewer.network.UrlConnectionHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramProviderTest {
    @Test
    fun resolvesTokenlessOEmbedResponseIntoProviderContent() {
        val http = FakeHttpClient(
            UrlConnectionHttpClient.Response(
                statusCode = 200,
                finalUrl = "https://graph.facebook.com/v25.0/instagram_oembed",
                body = """{"html":"<blockquote class=\"instagram-media\"></blockquote><script async src=\"https://www.instagram.com/embed.js\"></script>","title":"Example","author_name":"alice"}""",
            ),
        )

        val content = resolveCanonicalInstagram(
            canonicalUrl = "https://www.instagram.com/p/CODE_123/",
            http = http,
        )

        assertEquals("instagram", content.providerId)
        assertEquals("Instagram", content.providerName)
        assertEquals("https://www.instagram.com/p/CODE_123/", content.canonicalUrl)
        assertEquals("https://www.instagram.com/", content.documentBaseUrl)
        assertEquals("Example", content.title)
        assertEquals("alice", content.authorName)
        assertTrue(content.embedHtml.contains("instagram-media"))
        assertTrue(content.embedHtml.contains("https://www.instagram.com/embed.js"))
        assertTrue(
            http.requestedUrl.orEmpty()
                .startsWith("https://graph.facebook.com/v25.0/instagram_oembed?url="),
        )
    }

    @Test(expected = IllegalStateException::class)
    fun rejectsUnavailableOEmbedResponse() {
        resolveCanonicalInstagram(
            canonicalUrl = "https://www.instagram.com/reel/CODE_123/",
            http = FakeHttpClient(
                UrlConnectionHttpClient.Response(
                    statusCode = 404,
                    finalUrl = "https://graph.facebook.com/v25.0/instagram_oembed",
                    body = "{}",
                ),
            ),
        )
    }

    private class FakeHttpClient(
        private val response: UrlConnectionHttpClient.Response,
    ) : UrlConnectionHttpClient() {
        var requestedUrl: String? = null

        override fun get(url: String): Response {
            requestedUrl = url
            return response
        }
    }
}
