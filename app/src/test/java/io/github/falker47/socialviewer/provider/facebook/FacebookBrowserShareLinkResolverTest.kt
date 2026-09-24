package io.github.falker47.socialviewer.provider.facebook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FacebookBrowserShareLinkResolverTest {
    @Test
    fun selectsCanonicalPostFromIdentityMetadataWhileLocationRemainsAlias() {
        assertEquals(
            "https://www.facebook.com/alice/posts/pfbid123/",
            selectFacebookResolverCanonicalUrl(
                candidates = listOf(
                    "https://www.facebook.com/share/p/1GMwhxUrGi/",
                    "https://www.facebook.com/alice/posts/pfbid123/?rdid=test",
                ),
                expectedKind = FacebookContentKind.POST,
            ),
        )
    }

    @Test
    fun rejectsCanonicalCandidateOfWrongContentKind() {
        assertNull(
            selectFacebookResolverCanonicalUrl(
                candidates = listOf("https://www.facebook.com/reel/123/"),
                expectedKind = FacebookContentKind.POST,
            ),
        )
    }

    @Test
    fun resolverMainFrameAllowlistIsHttpsFacebookOnly() {
        assertTrue(isAllowedFacebookResolverMainFrame("https://www.facebook.com/share/r/code/"))
        assertTrue(isAllowedFacebookResolverMainFrame("https://m.facebook.com/share/p/code/"))
        assertFalse(isAllowedFacebookResolverMainFrame("http://www.facebook.com/share/r/code/"))
        assertFalse(isAllowedFacebookResolverMainFrame("https://example.com/reel/123/"))
    }

    @Test
    fun parsesOnlyReturnedUrlIdentityValues() {
        assertEquals(
            listOf(
                "https://www.facebook.com/share/r/code/",
                "https://www.facebook.com/reel/123/",
            ),
            parseFacebookResolverMetadata(
                "[\"https://www.facebook.com/share/r/code/\",\"https://www.facebook.com/reel/123/\"]",
            ),
        )
    }
}
