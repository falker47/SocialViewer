package io.github.falker47.socialviewer.provider.facebook

import io.github.falker47.socialviewer.network.UrlConnectionHttpClient
import io.github.falker47.socialviewer.provider.ProviderShareLinkResolutionException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FacebookShareLinkResolutionTest {
    @Test
    fun fallsBackToBrowserResolverWhenCheapRedirectRemainsOpaque() {
        val resolver = FakeResolver("https://www.facebook.com/reel/1254780559682152/?rdid=test")

        val target = resolveFacebookTarget(
            scheme = "https",
            host = "www.facebook.com",
            path = "/share/r/1HNwyf2jVo/",
            http = RedirectHttpClient("https://www.facebook.com/share/r/1HNwyf2jVo/"),
            shareLinkResolver = resolver,
        )

        assertEquals(FacebookContentKind.REEL, target.kind)
        assertEquals(
            "https://www.facebook.com/reel/1254780559682152/",
            target.canonicalUrl,
        )
        assertEquals(
            "https://www.facebook.com/share/r/1HNwyf2jVo/",
            resolver.seenAlias?.aliasUrl,
        )
    }

    @Test
    fun browserResolverResultMustStillBeSupportedFacebookCanonicalUrl() {
        val error = runCatching {
            resolveFacebookTarget(
                scheme = "https",
                host = "www.facebook.com",
                path = "/share/p/1GMwhxUrGi/",
                http = RedirectHttpClient("https://www.facebook.com/share/p/1GMwhxUrGi/"),
                shareLinkResolver = FakeResolver("https://example.com/posts/123/"),
            )
        }.exceptionOrNull()

        assertTrue(error is ProviderShareLinkResolutionException)
    }

    private class RedirectHttpClient(
        private val finalUrl: String,
    ) : UrlConnectionHttpClient() {
        override fun resolveFinalUrl(url: String): String = finalUrl
    }

    private class FakeResolver(
        private val resolvedUrl: String,
    ) : FacebookShareLinkResolver {
        var seenAlias: FacebookShareAlias? = null

        override fun resolve(alias: FacebookShareAlias): String {
            seenAlias = alias
            return resolvedUrl
        }
    }
}
