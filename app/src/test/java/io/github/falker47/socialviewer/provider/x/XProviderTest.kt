package io.github.falker47.socialviewer.provider.x

import io.github.falker47.socialviewer.network.UrlConnectionHttpClient
import io.github.falker47.socialviewer.provider.ProviderContentUnavailableException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XProviderTest {
    @Test
    fun resolvesOfficialOEmbedResponseIntoProviderContent() {
        val http = FakeHttpClient(
            response = UrlConnectionHttpClient.Response(
                statusCode = 200,
                finalUrl = "https://publish.x.com/oembed",
                body = """{"provider_name":"Twitter","type":"rich","html":"<blockquote class=\"twitter-tweet\"></blockquote>","author_name":"US Dept of Interior"}""",
            ),
        )

        val content = resolveCanonicalX(
            canonicalUrl = "https://x.com/Interior/status/463440424141459456",
            http = http,
        )

        assertEquals("x", content.providerId)
        assertEquals("X", content.providerName)
        assertEquals(
            "https://x.com/Interior/status/463440424141459456",
            content.canonicalUrl,
        )
        assertEquals("https://x.com/", content.documentBaseUrl)
        assertEquals("US Dept of Interior", content.authorName)
        assertTrue(content.embedHtml.contains("twitter-tweet"))
        assertTrue(content.embedHtml.contains("https://platform.x.com/widgets.js"))
        assertTrue(content.embedHtml.contains("""name="twitter:dnt" content="on""""))
        assertTrue(content.embedHtml.contains("Questo post X non è disponibile."))

        val requestUrl = http.requestedUrl.orEmpty()
        assertTrue(requestUrl.startsWith("https://publish.x.com/oembed?url="))
        assertTrue(requestUrl.contains("hide_thread=true"))
        assertTrue(requestUrl.contains("omit_script=true"))
        assertTrue(requestUrl.contains("dnt=true"))
        assertTrue(requestUrl.contains("https%3A%2F%2Fx.com%2FInterior%2Fstatus%2F463440424141459456"))
    }

    @Test(expected = ProviderContentUnavailableException::class)
    fun mapsExpectedUnavailableOEmbedResponse() {
        resolveCanonicalX(
            canonicalUrl = "https://x.com/Interior/status/463440424141459456",
            http = FakeHttpClient(
                response = UrlConnectionHttpClient.Response(
                    statusCode = 404,
                    finalUrl = "https://publish.x.com/oembed",
                    body = "{}",
                ),
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOEmbedRedirectOutsidePublishX() {
        resolveCanonicalX(
            canonicalUrl = "https://x.com/Interior/status/463440424141459456",
            http = FakeHttpClient(
                response = UrlConnectionHttpClient.Response(
                    statusCode = 200,
                    finalUrl = "https://example.com/oembed",
                    body = """{"provider_name":"Twitter","html":"<blockquote></blockquote>"}""",
                ),
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnexpectedOEmbedProvider() {
        resolveCanonicalX(
            canonicalUrl = "https://x.com/Interior/status/463440424141459456",
            http = FakeHttpClient(
                response = UrlConnectionHttpClient.Response(
                    statusCode = 200,
                    finalUrl = "https://publish.x.com/oembed",
                    body = """{"provider_name":"Example","html":"<blockquote></blockquote>"}""",
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
