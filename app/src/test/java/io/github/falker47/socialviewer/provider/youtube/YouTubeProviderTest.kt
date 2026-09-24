package io.github.falker47.socialviewer.provider.youtube

import io.github.falker47.socialviewer.network.UrlConnectionHttpClient
import io.github.falker47.socialviewer.provider.ProviderConfigurationException
import io.github.falker47.socialviewer.provider.ProviderContentUnavailableException
import io.github.falker47.socialviewer.provider.ProviderPolicyBlockedException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeProviderTest {
    @Test
    fun resolvesPublicNonMadeForKidsVideoIntoPrivacyEnhancedPlayer() {
        val http = FakeHttpClient(
            UrlConnectionHttpClient.Response(
                statusCode = 200,
                finalUrl = "https://www.googleapis.com/youtube/v3/videos?part=snippet,status",
                body = """
                    {
                      "items": [{
                        "snippet": {
                          "title": "Example",
                          "channelTitle": "Example channel"
                        },
                        "status": {
                          "privacyStatus": "public",
                          "embeddable": true,
                          "madeForKids": false
                        }
                      }]
                    }
                """.trimIndent(),
            ),
        )

        val content = resolveYouTubeVideo(
            videoId = "dQw4w9WgXcQ",
            http = http,
            apiKey = "test-key",
            apiClientHeaders = mapOf(
                "X-Android-Package" to "io.github.falker47.socialviewer",
                "X-Android-Cert" to "ABC123",
            ),
        )

        assertEquals("youtube", content.providerId)
        assertEquals("YouTube", content.providerName)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", content.canonicalUrl)
        assertEquals("Example channel", content.authorName)
        assertEquals("https://io.github.falker47.socialviewer/", content.documentBaseUrl)
        assertTrue(content.embedHtml.contains("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ"))
        assertTrue(content.embedHtml.contains("autoplay=0"))
        assertTrue(content.embedHtml.contains("rel=0"))
        assertTrue(http.requestedUrl.orEmpty().contains("part=snippet,status"))
        assertEquals(
            "io.github.falker47.socialviewer",
            http.requestedHeaders["X-Android-Package"],
        )
    }

    @Test(expected = ProviderPolicyBlockedException::class)
    fun madeForKidsVideoIsBlockedBeforeEmbedding() {
        resolveYouTubeVideo(
            videoId = "dQw4w9WgXcQ",
            http = FakeHttpClient(publicVideoResponse(madeForKids = true)),
            apiKey = "test-key",
        )
    }

    @Test(expected = ProviderContentUnavailableException::class)
    fun nonEmbeddableVideoIsUnavailable() {
        resolveYouTubeVideo(
            videoId = "dQw4w9WgXcQ",
            http = FakeHttpClient(publicVideoResponse(embeddable = false)),
            apiKey = "test-key",
        )
    }

    @Test(expected = ProviderContentUnavailableException::class)
    fun missingVideoIsUnavailable() {
        resolveYouTubeVideo(
            videoId = "dQw4w9WgXcQ",
            http = FakeHttpClient(
                UrlConnectionHttpClient.Response(
                    statusCode = 200,
                    finalUrl = "https://www.googleapis.com/youtube/v3/videos",
                    body = """{"items":[]}""",
                ),
            ),
            apiKey = "test-key",
        )
    }

    @Test(expected = ProviderConfigurationException::class)
    fun missingApiKeyFailsClosed() {
        resolveYouTubeVideo(
            videoId = "dQw4w9WgXcQ",
            http = FakeHttpClient(publicVideoResponse()),
            apiKey = "",
        )
    }

    private fun publicVideoResponse(
        madeForKids: Boolean = false,
        embeddable: Boolean = true,
    ) = UrlConnectionHttpClient.Response(
        statusCode = 200,
        finalUrl = "https://www.googleapis.com/youtube/v3/videos",
        body = """
            {
              "items": [{
                "snippet": {
                  "title": "Example",
                  "channelTitle": "Channel"
                },
                "status": {
                  "privacyStatus": "public",
                  "embeddable": $embeddable,
                  "madeForKids": $madeForKids
                }
              }]
            }
        """.trimIndent(),
    )

    private class FakeHttpClient(
        private val response: UrlConnectionHttpClient.Response,
    ) : UrlConnectionHttpClient() {
        var requestedUrl: String? = null
        var requestedHeaders: Map<String, String> = emptyMap()

        override fun get(
            url: String,
            headers: Map<String, String>,
        ): UrlConnectionHttpClient.Response {
            requestedUrl = url
            requestedHeaders = headers
            return response
        }
    }
}
