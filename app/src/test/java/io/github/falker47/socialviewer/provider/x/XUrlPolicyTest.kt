package io.github.falker47.socialviewer.provider.x

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XUrlPolicyTest {
    @Test
    fun supportsCanonicalXAndLegacyTwitterPostUrls() {
        assertTrue(
            XUrlPolicy.supports(
                "https",
                "x.com",
                "/Interior/status/463440424141459456",
            ),
        )
        assertTrue(
            XUrlPolicy.supports(
                "https",
                "www.twitter.com",
                "/Interior/status/463440424141459456/",
            ),
        )
    }

    @Test
    fun canonicalizesXAndLegacyTwitterToX() {
        assertEquals(
            "https://x.com/Interior/status/463440424141459456",
            XUrlPolicy.canonicalPostUrl(
                "https",
                "www.x.com",
                "/Interior/status/463440424141459456/",
            ),
        )
        assertEquals(
            "https://x.com/Interior/status/463440424141459456",
            XUrlPolicy.canonicalPostUrl(
                "https",
                "twitter.com",
                "/Interior/status/463440424141459456",
            ),
        )
    }

    @Test
    fun queryTrackingDoesNotAffectPathCanonicalization() {
        assertEquals(
            "https://x.com/Interior/status/463440424141459456",
            XUrlPolicy.canonicalPostUrl(
                "https",
                "x.com",
                "/Interior/status/463440424141459456",
            ),
        )
    }

    @Test
    fun rejectsNonPostPathsHttpLookalikesAndExtraNavigationPaths() {
        assertFalse(XUrlPolicy.supports("https", "x.com", "/home"))
        assertFalse(XUrlPolicy.supports("https", "x.com", "/Interior"))
        assertFalse(XUrlPolicy.supports("https", "x.com", "/search"))
        assertFalse(
            XUrlPolicy.supports(
                "https",
                "x.com",
                "/Interior/status/463440424141459456/photo/1",
            ),
        )
        assertFalse(
            XUrlPolicy.supports(
                "http",
                "x.com",
                "/Interior/status/463440424141459456",
            ),
        )
        assertFalse(
            XUrlPolicy.supports(
                "https",
                "x.example.com",
                "/Interior/status/463440424141459456",
            ),
        )
        assertNull(
            XUrlPolicy.canonicalPostUrl(
                "https",
                "x.com",
                "/Interior/status/not-a-post-id",
            ),
        )
    }
}
