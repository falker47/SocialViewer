package io.github.falker47.socialviewer.provider

import io.github.falker47.socialviewer.network.UrlConnectionHttpClient
import io.github.falker47.socialviewer.provider.pinterest.PinterestPinRequiredException
import io.github.falker47.socialviewer.provider.pinterest.PinterestProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProviderRegistryTest {
    private val shortLink = "https://pin.it/Example123"
    private val pin = "https://www.pinterest.com/pin/1234567890/"

    @Test
    fun directAndRedirectedNonPinDestinationsHaveTheSameError() {
        for (target in listOf(
            "https://it.pinterest.com/example_artist/?invite_code=synthetic-invite&sender=123",
            "https://www.pinterest.com/example_artist/ideas/",
            "https://www.pinterest.com/search/pins/",
        )) {
            val http = RedirectHttpClient(target)
            val registry = ProviderRegistry(listOf(PinterestProvider(http)))

            assertThrows(PinterestPinRequiredException::class.java) {
                registry.resolve(target)
            }
            assertEquals(emptyList<String>(), http.resolutions)

            assertThrows(PinterestPinRequiredException::class.java) {
                registry.resolve(shortLink)
            }
            assertEquals(listOf(shortLink), http.resolutions)
        }
    }

    @Test
    fun directAndRedirectedPinsStillResolveToTheSameCanonicalContent() {
        val http = RedirectHttpClient("https://it.pinterest.com/pin/1234567890/sent/?sender=123")
        val registry = ProviderRegistry(listOf(PinterestProvider(http)))

        assertEquals(pin, registry.resolve(pin).canonicalUrl)
        assertEquals(emptyList<String>(), http.resolutions)
        assertEquals(pin, registry.resolve(shortLink).canonicalUrl)
        assertEquals(listOf(shortLink), http.resolutions)
    }

    @Test
    fun unknownServicesAndUnconfiguredPinterestRemainUnsupported() {
        val registry = ProviderRegistry(listOf(PinterestProvider(RedirectHttpClient(pin))))
        for (url in listOf(
            "https://example.org/item",
            "https://www.pinterest.com.attacker.example/pin/1234567890/",
        )) {
            assertThrows(UnsupportedProviderException::class.java) {
                registry.resolve(url)
            }
        }
        assertThrows(UnsupportedProviderException::class.java) {
            ProviderRegistry(emptyList()).resolve("https://www.pinterest.com/example_artist/")
        }
    }

    @Test
    fun providerSelectionRemainsNullableForProfilesBoardsAndFeeds() {
        val provider = PinterestProvider(RedirectHttpClient(pin))
        val registry = ProviderRegistry(listOf(provider))

        assertSame(provider, registry.providerFor(pin))
        assertSame(provider, registry.providerFor(shortLink))
        assertNull(registry.providerFor("https://it.pinterest.com/example_artist/"))
        assertNull(registry.providerFor("https://www.pinterest.com/example_artist/ideas/"))
        assertNull(registry.providerFor("https://www.pinterest.com/search/pins/"))
    }

    private class RedirectHttpClient(private val finalUrl: String) : UrlConnectionHttpClient() {
        val resolutions = mutableListOf<String>()

        override fun resolveFinalUrl(url: String): String {
            resolutions += url
            return finalUrl
        }

        override fun get(url: String, headers: Map<String, String>): Response =
            error("Pinterest resolution must not fetch API metadata")
    }
}
