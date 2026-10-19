package io.github.falker47.socialviewer.ui

import android.content.Context
import android.content.res.Configuration
import io.github.falker47.socialviewer.network.UrlConnectionHttpClient
import io.github.falker47.socialviewer.provider.ProviderRegistry
import io.github.falker47.socialviewer.provider.pinterest.PinterestProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ManualLinkValidationTest {
    private val profile =
        "https://it.pinterest.com/example_artist/?invite_code=synthetic-invite&sender=123"
    private val pin = "https://www.pinterest.com/pin/1234567890/"
    private val registry = ProviderRegistry(listOf(PinterestProvider(NoNetworkHttpClient())))

    @Test
    fun directAndSharedPinterestDestinationsExplainThePinRequirementInBothLanguages() {
        for ((locale, message) in listOf(
            Locale.ENGLISH to
                "This Pinterest link did not lead to a supported public Pin. Open a direct link to the Pin.",
            Locale.ITALIAN to
                "Questo link Pinterest non ha portato a un Pin pubblico supportato. Apri il link diretto al Pin.",
        )) {
            val context = contextFor(locale)
            for (url in listOf(
                profile,
                "https://www.pinterest.com/example_artist/ideas/",
                "https://www.pinterest.com/search/pins/",
            )) {
                for (input in listOf(url, "Guarda questo link: $url")) {
                    assertEquals(
                        ManualLinkResult.Invalid(message, url),
                        validateManualLink(input, registry, context),
                    )
                }
            }
        }
    }

    @Test
    fun validPinsAndShareLinksRemainValidWithoutResolvingThem() {
        val context = contextFor(Locale.ITALIAN)
        for (url in listOf(
            pin,
            "https://it.pinterest.com/pin/example--1234567890/",
            "https://www.pinterest.com/pin/1234567890/sent/?sender=123",
            "https://pin.it/Example123",
        )) {
            assertEquals(
                ManualLinkResult.Valid(url),
                validateManualLink(url, registry, context),
            )
        }
    }

    @Test
    fun unknownServicesAndUnconfiguredPinterestKeepTheServiceMessage() {
        for ((locale, message) in listOf(
            Locale.ENGLISH to "This service isn’t supported yet.",
            Locale.ITALIAN to "Questo servizio non è ancora supportato.",
        )) {
            val context = contextFor(locale)
            for (url in listOf(
                "https://example.org/item",
                "https://it.pinterest.com.attacker.example/pin/1234567890/",
                "https://pinterest.example/pin/1234567890/",
            )) {
                assertEquals(
                    ManualLinkResult.Invalid(message, url),
                    validateManualLink(url, registry, context),
                )
            }
            assertEquals(
                ManualLinkResult.Invalid(message, profile),
                validateManualLink(profile, ProviderRegistry(emptyList()), context),
            )
        }
    }

    @Test
    fun emptyAndMalformedInputKeepTheirMessagesAndDisplayValues() {
        for ((locale, messages) in listOf(
            Locale.ENGLISH to listOf(
                "No link found in the clipboard.",
                "This doesn’t look like a valid link.",
            ),
            Locale.ITALIAN to listOf(
                "Nessun link negli appunti.",
                "Questo non sembra un link valido.",
            ),
        )) {
            val context = contextFor(locale)
            assertEquals(
                ManualLinkResult.Invalid(messages[0], ""),
                validateManualLink(" \n ", registry, context),
            )
            for (value in listOf(
                "not-a-url",
                "http://www.pinterest.com/pin/1234567890/",
                "https:///pin/1234567890/",
            )) {
                assertEquals(
                    ManualLinkResult.Invalid(messages[1], value),
                    validateManualLink("  $value  ", registry, context),
                )
            }
        }
    }

    @Test
    fun sharedTextStillSelectsTheFirstUrlAndTrimsTrailingPunctuation() {
        assertEquals(
            ManualLinkResult.Valid(pin),
            validateManualLink(
                "  Guarda questo Pin: $pin).\nAltro: https://www.pinterest.com/pin/999/  ",
                registry,
                contextFor(Locale.ITALIAN),
            ),
        )
    }

    private fun contextFor(locale: Locale): Context {
        val application = RuntimeEnvironment.getApplication()
        val configuration = Configuration(application.resources.configuration).apply {
            setLocale(locale)
        }
        return application.createConfigurationContext(configuration)
    }

    private class NoNetworkHttpClient : UrlConnectionHttpClient() {
        override fun resolveFinalUrl(url: String): String =
            error("Input validation must not resolve network redirects")

        override fun get(url: String, headers: Map<String, String>): Response =
            error("Input validation must not request provider data")
    }
}
