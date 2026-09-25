package io.github.falker47.socialviewer.provider.pinterest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PinterestUrlPolicyTest {
    @Test
    fun supportsCanonicalSinglePinUrlsIncludingRegionalSeoAndSentForms() {
        assertTrue(
            PinterestUrlPolicy.supports(
                "https",
                "www.pinterest.com",
                "/pin/617415430169271912/",
            ),
        )
        assertTrue(
            PinterestUrlPolicy.supports(
                "https",
                "pinterest.com",
                "/pin/1234567890",
            ),
        )
        assertTrue(
            PinterestUrlPolicy.supports(
                "https",
                "it.pinterest.com",
                "/pin/nasa--754845587535670858/",
            ),
        )
        assertTrue(
            PinterestUrlPolicy.supports(
                "https",
                "www.pinterest.com",
                "/pin/266556871689003952/sent/",
            ),
        )
    }

    @Test
    fun canonicalizesPinsToNumericWwwPinterestForm() {
        assertEquals(
            "https://www.pinterest.com/pin/617415430169271912/",
            PinterestUrlPolicy.canonicalPinUrl(
                "https",
                "pinterest.com",
                "/pin/617415430169271912/",
            ),
        )
        assertEquals(
            "https://www.pinterest.com/pin/754845587535670858/",
            PinterestUrlPolicy.canonicalPinUrl(
                "https",
                "it.pinterest.com",
                "/pin/nasa--754845587535670858/",
            ),
        )
        assertEquals(
            "https://www.pinterest.com/pin/266556871689003952/",
            PinterestUrlPolicy.canonicalPinUrl(
                "https",
                "www.pinterest.com",
                "/pin/266556871689003952/sent/",
            ),
        )
    }

    @Test
    fun recognizesOnlyBoundedPinItShareAliasesForRedirectResolution() {
        assertTrue(
            PinterestUrlPolicy.requiresRedirectResolution(
                "https",
                "pin.it",
                "/AbC123_xYz/",
            ),
        )
        assertFalse(
            PinterestUrlPolicy.requiresRedirectResolution(
                "https",
                "pin.it",
                "/AbC123/extra/",
            ),
        )
        assertFalse(
            PinterestUrlPolicy.requiresRedirectResolution(
                "https",
                "example.com",
                "/AbC123/",
            ),
        )
    }

    @Test
    fun rejectsBoardsProfilesFeedsHttpLookalikesAndMalformedPins() {
        assertFalse(PinterestUrlPolicy.supports("https", "www.pinterest.com", "/alice/"))
        assertFalse(PinterestUrlPolicy.supports("https", "www.pinterest.com", "/alice/ideas/"))
        assertFalse(PinterestUrlPolicy.supports("https", "www.pinterest.com", "/search/pins/"))
        assertFalse(PinterestUrlPolicy.supports("http", "www.pinterest.com", "/pin/1234567890/"))
        assertFalse(
            PinterestUrlPolicy.supports(
                "https",
                "pinterest.example.com",
                "/pin/1234567890/",
            ),
        )
        assertFalse(
            PinterestUrlPolicy.supports(
                "https",
                "help.pinterest.com",
                "/pin/1234567890/",
            ),
        )
        assertFalse(PinterestUrlPolicy.supports("https", "www.pinterest.com", "/pin/not-an-id/"))
        assertFalse(
            PinterestUrlPolicy.supports(
                "https",
                "www.pinterest.com",
                "/pin/slug--not-an-id/",
            ),
        )
        assertFalse(
            PinterestUrlPolicy.supports(
                "https",
                "www.pinterest.com",
                "/pin/--1234567890/",
            ),
        )
        assertFalse(
            PinterestUrlPolicy.supports(
                "https",
                "www.pinterest.com",
                "/pin/1234567890/extra/",
            ),
        )
        assertNull(
            PinterestUrlPolicy.canonicalPinUrl(
                "https",
                "www.pinterest.com",
                "/pin/",
            ),
        )
    }
}
