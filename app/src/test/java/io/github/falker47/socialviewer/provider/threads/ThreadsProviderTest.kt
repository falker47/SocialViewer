package io.github.falker47.socialviewer.provider.threads

import io.github.falker47.socialviewer.network.UrlConnectionHttpClient
import io.github.falker47.socialviewer.provider.ProviderContentUnavailableException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadsProviderTest {
    @Test
    fun resolvesTokenlessOEmbedResponseIntoProviderContent() {
        val http = FakeHttpClient(
            UrlConnectionHttpClient.Response(
                statusCode = 200,
                finalUrl = "https://graph.threads.com/oembed",
                body = """{"html":"<blockquote class=\"text-post-media\"></blockquote><script async src=\"https://www.threads.com/embed.js\"></script>","title":"Example","author_name":"alice"}""",
            ),
        )

        val content = resolveCanonicalThreads(
            canonicalUrl = "https://www.threads.com/@alice/post/ABC_123/",
            http = http,
        )

        assertEquals("threads", content.providerId)
        assertEquals("Threads", content.providerName)
        assertEquals("https://www.threads.com/@alice/post/ABC_123/", content.canonicalUrl)
        assertEquals("https://www.threads.com/", content.documentBaseUrl)
        assertEquals("Example", content.title)
        assertEquals("alice", content.authorName)
        assertTrue(content.embedHtml.contains("text-post-media"))
        assertTrue(content.embedHtml.contains("https://www.threads.com/embed.js"))
        assertTrue(
            http.requestedUrl.orEmpty()
                .startsWith("https://graph.threads.com/oembed?url="),
        )
    }

    @Test(expected = ProviderContentUnavailableException::class)
    fun mapsExpectedUnavailableOEmbedResponse() {
        resolveCanonicalThreads(
            canonicalUrl = "https://www.threads.com/t/ABC_123/",
            http = FakeHttpClient(
                UrlConnectionHttpClient.Response(
                    statusCode = 400,
                    finalUrl = "https://graph.threads.com/oembed",
                    body = "{}",
                ),
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOEmbedRedirectOutsideThreadsGraphHost() {
        resolveCanonicalThreads(
            canonicalUrl = "https://www.threads.com/t/ABC_123/",
            http = FakeHttpClient(
                UrlConnectionHttpClient.Response(
                    statusCode = 200,
                    finalUrl = "https://example.com/oembed",
                    body = """{"html":"<blockquote></blockquote>"}""",
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
