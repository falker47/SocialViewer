package io.github.falker47.socialviewer.provider.facebook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FacebookUrlPolicyTest {
    @Test
    fun supportsPublicPostAndReelPaths() {
        assertTrue(
            FacebookUrlPolicy.supports(
                "https",
                "www.facebook.com",
                "/kevinloveofficial/posts/pfbid0nWhZeiMVjz/",
            ),
        )
        assertTrue(
            FacebookUrlPolicy.supports(
                "https",
                "facebook.com",
                "/reel/3305054673010377/",
            ),
        )
        assertTrue(
            FacebookUrlPolicy.supports(
                "https",
                "www.facebook.com",
                "/share/r/1HNwyf2jVo/",
            ),
        )
    }

    @Test
    fun rejectsOutOfScopeAndUnsafeUrls() {
        assertFalse(FacebookUrlPolicy.supports("https", "www.facebook.com", "/kevinloveofficial/"))
        assertFalse(FacebookUrlPolicy.supports("https", "www.facebook.com", "/stories/123/"))
        assertFalse(FacebookUrlPolicy.supports("https", "www.facebook.com", "/groups/example/posts/123/"))
        assertTrue(FacebookUrlPolicy.supports("https", "www.facebook.com", "/share/p/123/"))
        assertFalse(FacebookUrlPolicy.supports("https", "www.facebook.com", "/share/v/123/"))
        assertFalse(FacebookUrlPolicy.supports("https", "fakefacebook.com", "/alice/posts/123/"))
        assertFalse(FacebookUrlPolicy.supports("http", "www.facebook.com", "/alice/posts/123/"))
    }

    @Test
    fun normalizesPostAndReelShareAliasesForSafeRedirectResolution() {
        assertEquals(
            FacebookContentKind.REEL,
            FacebookUrlPolicy.shareAlias(
                "https",
                "facebook.com",
                "/share/r/1HNwyf2jVo/",
            )?.expectedKind,
        )
        assertEquals(
            "https://www.facebook.com/share/r/1HNwyf2jVo/",
            FacebookUrlPolicy.shareAlias(
                "https",
                "facebook.com",
                "/share/r/1HNwyf2jVo/",
            )?.aliasUrl,
        )
        assertEquals(
            FacebookContentKind.POST,
            FacebookUrlPolicy.shareAlias(
                "https",
                "www.facebook.com",
                "/share/p/1GMwhxUrGi/",
            )?.expectedKind,
        )
        assertEquals(
            "https://www.facebook.com/share/p/1GMwhxUrGi/",
            FacebookUrlPolicy.shareAlias(
                "https",
                "www.facebook.com",
                "/share/p/1GMwhxUrGi/",
            )?.aliasUrl,
        )
        assertNull(
            FacebookUrlPolicy.canonicalUrl(
                "https",
                "www.facebook.com",
                "/share/p/1GMwhxUrGi/",
            ),
        )
        assertNull(
            FacebookUrlPolicy.shareAlias(
                "https",
                "www.facebook.com",
                "/share/v/1GMwhxUrGi/",
            ),
        )
    }

    @Test
    fun canonicalizesSupportedUrlsConservatively() {
        assertEquals(
            "https://www.facebook.com/alice/posts/pfbid123/",
            FacebookUrlPolicy.canonicalUrl(
                "https",
                "facebook.com",
                "/alice/posts/pfbid123/",
            ),
        )
        assertEquals(
            "https://www.facebook.com/reel/3305054673010377/",
            FacebookUrlPolicy.canonicalUrl(
                "https",
                "www.facebook.com",
                "/reel/3305054673010377/",
            ),
        )
        assertNull(
            FacebookUrlPolicy.canonicalUrl(
                "https",
                "www.facebook.com",
                "/watch/?v=123",
            ),
        )
    }

    @Test
    fun distinguishesPostAndReelEndpointKinds() {
        assertEquals(
            FacebookContentKind.POST,
            FacebookUrlPolicy.canonicalTarget(
                "https",
                "www.facebook.com",
                "/alice/posts/123/",
            )?.kind,
        )
        assertEquals(
            FacebookContentKind.REEL,
            FacebookUrlPolicy.canonicalTarget(
                "https",
                "www.facebook.com",
                "/reel/123/",
            )?.kind,
        )
    }
}
