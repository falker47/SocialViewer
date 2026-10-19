package io.github.falker47.socialviewer.ui

import io.github.falker47.socialviewer.R
import io.github.falker47.socialviewer.network.LinkResolutionHttpException
import io.github.falker47.socialviewer.provider.ProviderConfigurationException
import io.github.falker47.socialviewer.provider.ProviderContentUnavailableException
import io.github.falker47.socialviewer.provider.ProviderHttpException
import io.github.falker47.socialviewer.provider.ProviderInvalidResponseException
import io.github.falker47.socialviewer.provider.ProviderPolicyBlockedException
import io.github.falker47.socialviewer.provider.pinterest.PinterestPinRequiredException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale
import javax.net.ssl.SSLHandshakeException
import javax.xml.parsers.DocumentBuilderFactory

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

    @Test
    fun transportFailuresDoNotExposeTechnicalDetailsOrClaimContentIsUnavailable() {
        for (failure in listOf(
            IOException("https://example.com/?token=private-link-token"),
            UnknownHostException("private-link-token"),
            SSLHandshakeException("private-link-token"),
        )) {
            val copy = viewerErrorCopyFor(failure)

            assertEquals(R.string.error_title_connection_failed, copy.titleRes)
            assertEquals(R.string.error_connection_failed, copy.messageRes)
            assertEquals(emptyList<Any>(), copy.messageArgs)
        }
    }

    @Test
    fun newFailureMessagesFormatCorrectlyInEnglishAndItalian() {
        val cases = listOf(
            MessageCase(
                error = SocketTimeoutException("private-link-token"),
                titleRes = R.string.error_title_connection_failed,
                messageRes = R.string.error_request_timeout,
                resourceName = "error_request_timeout",
                expectedArgs = emptyList(),
                english = "The service did not respond in time. Try again.",
                italian = "Il servizio non ha risposto in tempo. Riprova.",
            ),
            MessageCase(
                error = IOException("private-link-token"),
                titleRes = R.string.error_title_connection_failed,
                messageRes = R.string.error_connection_failed,
                resourceName = "error_connection_failed",
                expectedArgs = emptyList(),
                english = "The connection to the service could not be completed. Try again.",
                italian = "Non è stato possibile completare la connessione al servizio. Riprova.",
            ),
            MessageCase(
                error = LinkResolutionHttpException(503),
                titleRes = R.string.error_title_open_failed,
                messageRes = R.string.error_link_http_format,
                resourceName = "error_link_http_format",
                expectedArgs = listOf(503),
                english = "The service returned HTTP 503 while opening the link. Try again.",
                italian = "Il servizio ha risposto HTTP 503 durante l’apertura del link. Riprova.",
            ),
            MessageCase(
                error = ProviderHttpException("TikTok", 403),
                titleRes = R.string.error_title_open_failed,
                messageRes = R.string.error_provider_metadata_http_format,
                resourceName = "error_provider_metadata_http_format",
                expectedArgs = listOf("TikTok", 403),
                english = "TikTok did not return the content details (HTTP 403). Try again.",
                italian = "TikTok non ha restituito i dati del contenuto (HTTP 403). Riprova.",
            ),
            MessageCase(
                error = ProviderInvalidResponseException("TikTok", 200),
                titleRes = R.string.error_title_open_failed,
                messageRes = R.string.error_provider_invalid_response_format,
                resourceName = "error_provider_invalid_response_format",
                expectedArgs = listOf("TikTok", 200),
                english = "Invalid response from TikTok (HTTP 200). Try again.",
                italian = "Risposta non valida da TikTok (HTTP 200). Riprova.",
            ),
            MessageCase(
                error = PinterestPinRequiredException(),
                titleRes = R.string.error_title_unsupported_link,
                messageRes = R.string.error_pinterest_pin_required,
                resourceName = "error_pinterest_pin_required",
                expectedArgs = emptyList(),
                english = "This Pinterest link did not lead to a supported public Pin. Open a direct link to the Pin.",
                italian = "Questo link Pinterest non ha portato a un Pin pubblico supportato. Apri il link diretto al Pin.",
            ),
        )

        for (case in cases) {
            val copy = viewerErrorCopyFor(case.error)
            assertEquals(case.titleRes, copy.titleRes)
            assertEquals(case.messageRes, copy.messageRes)
            assertEquals(case.expectedArgs, copy.messageArgs)

            for ((directory, locale, expected) in listOf(
                Triple("values", Locale.ENGLISH, case.english),
                Triple("values-it", Locale.ITALIAN, case.italian),
            )) {
                val format = stringResource(directory, case.resourceName)
                val message = String.format(locale, format, *copy.messageArgs.toTypedArray())
                assertEquals(expected, message)
                assertFalse(message.contains("private-link-token"))
            }
        }
    }

    private data class MessageCase(
        val error: Throwable,
        val titleRes: Int,
        val messageRes: Int,
        val resourceName: String,
        val expectedArgs: List<Any>,
        val english: String,
        val italian: String,
    )

    private fun stringResource(directory: String, name: String): String {
        val path = "src/main/res/$directory/strings.xml"
        val file = listOf(File(path), File("app/$path")).first { it.isFile }
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val strings = document.getElementsByTagName("string")
        for (index in 0 until strings.length) {
            val item = strings.item(index)
            if (item.attributes.getNamedItem("name").nodeValue == name) return item.textContent
        }
        error("Missing string resource: $name")
    }
}
