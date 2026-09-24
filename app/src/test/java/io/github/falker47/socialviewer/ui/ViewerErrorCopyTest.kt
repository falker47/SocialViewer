package io.github.falker47.socialviewer.ui

import io.github.falker47.socialviewer.provider.ProviderContentUnavailableException
import io.github.falker47.socialviewer.provider.ProviderShareLinkResolutionException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewerErrorCopyTest {
    @Test
    fun expectedInstagramUnavailabilityGetsCleanUserCopy() {
        val copy = viewerErrorCopyFor(
            ProviderContentUnavailableException(
                providerName = "Instagram",
                technicalDetail = "Instagram oEmbed ha risposto HTTP 400",
            ),
        )

        assertEquals("Contenuto non disponibile", copy.title)
        assertEquals("Questo contenuto Instagram non è disponibile.", copy.message)
        assertFalse(copy.message.contains("HTTP"))
    }

    @Test
    fun expectedFacebookUnavailabilityGetsCleanUserCopy() {
        val copy = viewerErrorCopyFor(
            ProviderContentUnavailableException(
                providerName = "Facebook",
                technicalDetail = "Facebook oEmbed ha risposto HTTP 400",
            ),
        )

        assertEquals("Contenuto non disponibile", copy.title)
        assertEquals("Questo contenuto Facebook non è disponibile.", copy.message)
        assertFalse(copy.message.contains("HTTP"))
    }

    @Test
    fun unresolvedFacebookShareLinkGetsActionableCopy() {
        val copy = viewerErrorCopyFor(
            ProviderShareLinkResolutionException(
                providerName = "Facebook",
                canonicalHint = "facebook.com/reel/…",
                technicalDetail = "Facebook non ha esposto un permalink canonico",
            ),
        )

        assertEquals("Link Facebook non risolvibile", copy.title)
        assertTrue(copy.message.contains("permalink pubblico"))
        assertTrue(copy.message.contains("facebook.com/reel/…"))
        assertFalse(copy.message.contains("HTTP"))
    }

    @Test
    fun unexpectedProviderFailureIsNotMislabelledAsUnavailable() {
        val copy = viewerErrorCopyFor(
            IllegalStateException("Instagram oEmbed ha risposto HTTP 500"),
        )

        assertEquals("Impossibile aprire il contenuto", copy.title)
        assertTrue(copy.message.contains("HTTP 500"))
    }
}
