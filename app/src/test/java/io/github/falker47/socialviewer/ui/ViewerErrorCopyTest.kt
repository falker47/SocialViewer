package io.github.falker47.socialviewer.ui

import io.github.falker47.socialviewer.R
import io.github.falker47.socialviewer.provider.ProviderConfigurationException
import io.github.falker47.socialviewer.provider.ProviderContentUnavailableException
import io.github.falker47.socialviewer.provider.ProviderPolicyBlockedException
import org.junit.Assert.assertEquals
import org.junit.Test

class ViewerErrorCopyTest {
    @Test
    fun expectedInstagramUnavailabilityGetsLocalizedResourceCopy() {
        val copy = viewerErrorCopyFor(
            ProviderContentUnavailableException(
                providerName = "Instagram",
                technicalDetail = "Instagram oEmbed ha risposto HTTP 400",
            ),
        )

        assertEquals(R.string.error_title_unavailable, copy.titleRes)
        assertEquals(R.string.error_content_unavailable_format, copy.messageRes)
        assertEquals(listOf("Instagram"), copy.messageArgs)
    }

    @Test
    fun madeForKidsBlockGetsLocalizedPolicyCopy() {
        val copy = viewerErrorCopyFor(
            ProviderPolicyBlockedException(
                providerName = "YouTube",
                userMessage = "legacy provider copy",
                technicalDetail = "Made For Kids",
            ),
        )

        assertEquals(R.string.error_title_policy_blocked, copy.titleRes)
        assertEquals(R.string.error_youtube_mfk, copy.messageRes)
    }

    @Test
    fun missingProviderConfigurationGetsLocalizedCopy() {
        val copy = viewerErrorCopyFor(
            ProviderConfigurationException(
                providerName = "YouTube",
                userMessage = "legacy provider copy",
                technicalDetail = "YOUTUBE_API_KEY non configurata",
            ),
        )

        assertEquals(R.string.error_title_provider_configuration, copy.titleRes)
        assertEquals(R.string.error_provider_not_configured_format, copy.messageRes)
        assertEquals(listOf("YouTube"), copy.messageArgs)
    }

    @Test
    fun unexpectedProviderFailureUsesGenericLocalizedCopy() {
        val copy = viewerErrorCopyFor(
            IllegalStateException("Instagram oEmbed ha risposto HTTP 500"),
        )

        assertEquals(R.string.error_title_open_failed, copy.titleRes)
        assertEquals(R.string.error_unexpected, copy.messageRes)
    }
}
