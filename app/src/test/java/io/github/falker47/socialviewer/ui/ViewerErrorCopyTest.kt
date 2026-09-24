package io.github.falker47.socialviewer.ui

import io.github.falker47.socialviewer.provider.ProviderContentUnavailableException
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
    fun unexpectedProviderFailureIsNotMislabelledAsUnavailable() {
        val copy = viewerErrorCopyFor(
            IllegalStateException("Instagram oEmbed ha risposto HTTP 500"),
        )

        assertEquals("Impossibile aprire il contenuto", copy.title)
        assertTrue(copy.message.contains("HTTP 500"))
    }
}
