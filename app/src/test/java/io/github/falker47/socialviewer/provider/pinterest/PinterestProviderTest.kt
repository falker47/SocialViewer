package io.github.falker47.socialviewer.provider.pinterest

import io.github.falker47.socialviewer.network.UrlConnectionHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PinterestProviderTest {
    @Test
    fun buildsOfficialPinWidgetWithoutApiDependency() {
        val content = resolveCanonicalPinterest(
            canonicalUrl = "https://www.pinterest.com/pin/617415430169271912/",
        )

        assertEquals("pinterest", content.providerId)
        assertEquals("Pinterest", content.providerName)
        assertEquals(
            "https://www.pinterest.com/pin/617415430169271912/",
            content.canonicalUrl,
        )
        assertEquals("https://www.pinterest.com/", content.documentBaseUrl)
        assertNull(content.title)
        assertNull(content.authorName)
        assertTrue(content.embedHtml.contains("""data-pin-do="embedPin""""))
        assertTrue(
            content.embedHtml.contains(
                """href="https://www.pinterest.com/pin/617415430169271912/"""",
            ),
        )
        assertTrue(
            content.embedHtml.contains("https://assets.pinterest.com/js/pinit.js"),
        )
    }

    @Test
    fun directPinCanonicalizationDoesNotNeedRedirectResolution() {
        val http = FakeHttpClient(finalUrl = "https://example.com/should-not-be-used")

        val canonical = canonicalPinterestUrlFor(
            scheme = "https",
            host = "pinterest.com",
            path = "/pin/617415430169271912/",
            rawUrl = "https://pinterest.com/pin/617415430169271912/?utm_source=test",
            http = http,
        )

        assertEquals(
            "https://www.pinterest.com/pin/617415430169271912/",
            canonical,
        )
        assertNull(http.resolvedUrl)
    }

    @Test
    fun resolvesPinItAliasOnlyWhenFinalTargetIsSupportedPin() {
        val http = FakeHttpClient(
            finalUrl = "https://www.pinterest.com/pin/617415430169271912/?share_id=tracking",
        )

        val canonical = canonicalPinterestUrlFor(
            scheme = "https",
            host = "pin.it",
            path = "/AbC123_xYz/",
            rawUrl = "https://pin.it/AbC123_xYz",
            http = http,
        )

        assertEquals(
            "https://www.pinterest.com/pin/617415430169271912/",
            canonical,
        )
        assertEquals("https://pin.it/AbC123_xYz", http.resolvedUrl)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPinItAliasRedirectOutsidePinterest() {
        canonicalPinterestUrlFor(
            scheme = "https",
            host = "pin.it",
            path = "/AbC123/",
            rawUrl = "https://pin.it/AbC123",
            http = FakeHttpClient(finalUrl = "https://example.com/content"),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPinItAliasRedirectToPinterestBoard() {
        canonicalPinterestUrlFor(
            scheme = "https",
            host = "pin.it",
            path = "/AbC123/",
            rawUrl = "https://pin.it/AbC123",
            http = FakeHttpClient(finalUrl = "https://www.pinterest.com/example/board/"),
        )
    }

    private class FakeHttpClient(
        private val finalUrl: String,
    ) : UrlConnectionHttpClient() {
        var resolvedUrl: String? = null

        override fun resolveFinalUrl(url: String): String {
            resolvedUrl = url
            return finalUrl
        }
    }
}
