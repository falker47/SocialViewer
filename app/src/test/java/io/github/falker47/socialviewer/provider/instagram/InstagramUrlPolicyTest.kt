package io.github.falker47.socialviewer.provider.instagram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramUrlPolicyTest {
    @Test
    fun supportsPublicPostAndReelPaths() {
        assertTrue(
            InstagramUrlPolicy.supports(
                "https",
                "www.instagram.com",
                "/p/CODE_123/",
            ),
        )
        assertTrue(
            InstagramUrlPolicy.supports(
                "https",
                "instagram.com",
                "/reel/ABC-def/",
            ),
        )
    }

    @Test
    fun rejectsProfilesStoriesAndLookalikeHosts() {
        assertFalse(InstagramUrlPolicy.supports("https", "www.instagram.com", "/alice/"))
        assertFalse(InstagramUrlPolicy.supports("https", "www.instagram.com", "/stories/alice/123/"))
        assertFalse(InstagramUrlPolicy.supports("https", "fakeinstagram.com", "/p/CODE/"))
        assertFalse(InstagramUrlPolicy.supports("http", "www.instagram.com", "/p/CODE/"))
    }

    @Test
    fun canonicalizesSupportedUrlsWithoutTrackingData() {
        assertEquals(
            "https://www.instagram.com/reel/ABC_123/",
            InstagramUrlPolicy.canonicalUrl(
                "https",
                "instagram.com",
                "/reel/ABC_123/",
            ),
        )
        assertNull(
            InstagramUrlPolicy.canonicalUrl(
                "https",
                "www.instagram.com",
                "/share/reel/ABC_123/",
            ),
        )
    }
}
