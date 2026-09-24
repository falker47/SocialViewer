package io.github.falker47.socialviewer.ui

import io.github.falker47.socialviewer.provider.ProviderConfigurationException
import io.github.falker47.socialviewer.provider.ProviderContentUnavailableException
import io.github.falker47.socialviewer.provider.ProviderPolicyBlockedException
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
    fun madeForKidsBlockGetsPolicyCopy() {
        val copy = viewerErrorCopyFor(
            ProviderPolicyBlockedException(
                providerName = "YouTube",
                userMessage = "I video YouTube destinati ai bambini non vengono aperti in Social Viewer.",
                technicalDetail = "Made For Kids",
            ),
        )

        assertEquals("Contenuto non supportato", copy.title)
        assertTrue(copy.message.contains("destinati ai bambini"))
        assertFalse(copy.message.contains("Made For Kids"))
    }

    @Test
    fun missingProviderConfigurationGetsCleanCopy() {
        val copy = viewerErrorCopyFor(
            ProviderConfigurationException(
                providerName = "YouTube",
                userMessage = "YouTube non è configurato in questa build.",
                technicalDetail = "YOUTUBE_API_KEY non configurata",
            ),
        )

        assertEquals("Provider non configurato", copy.title)
        assertEquals("YouTube non è configurato in questa build.", copy.message)
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
