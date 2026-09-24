package io.github.falker47.socialviewer.provider.youtube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeUrlPolicyTest {
    @Test
    fun acceptsCanonicalWatchUrlAndIgnoresTrackingQuery() {
        assertTrue(
            YouTubeUrlPolicy.supports(
                scheme = "https",
                host = "www.youtube.com",
                path = "/watch",
                encodedQuery = "v=dQw4w9WgXcQ&si=abc&utm_source=share",
            ),
        )
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeUrlPolicy.videoId(
                scheme = "https",
                host = "www.youtube.com",
                path = "/watch",
                encodedQuery = "si=abc&v=dQw4w9WgXcQ",
            ),
        )
    }

    @Test
    fun acceptsMobileShortShortsAndLiveFamilies() {
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeUrlPolicy.videoId("https", "m.youtube.com", "/watch", "v=dQw4w9WgXcQ"),
        )
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeUrlPolicy.videoId("https", "youtu.be", "/dQw4w9WgXcQ", "si=abc"),
        )
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeUrlPolicy.videoId("https", "youtube.com", "/shorts/dQw4w9WgXcQ", null),
        )
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeUrlPolicy.videoId("https", "youtube.com", "/live/dQw4w9WgXcQ", null),
        )
    }

    @Test
    fun rejectsUnsupportedSurfacesAndInvalidIds() {
        assertNull(
            YouTubeUrlPolicy.videoId("https", "youtube.com", "/channel/UC123", null),
        )
        assertNull(
            YouTubeUrlPolicy.videoId("https", "youtube.com", "/playlist", "list=PL123"),
        )
        assertNull(
            YouTubeUrlPolicy.videoId("https", "youtube.com", "/watch", "v=too-short"),
        )
        assertFalse(
            YouTubeUrlPolicy.supports(
                scheme = "http",
                host = "youtu.be",
                path = "/dQw4w9WgXcQ",
                encodedQuery = null,
            ),
        )
    }

    @Test
    fun canonicalizesEverySupportedFamilyToWatchUrl() {
        assertEquals(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            YouTubeUrlPolicy.canonicalWatchUrl("dQw4w9WgXcQ"),
        )
    }
}
