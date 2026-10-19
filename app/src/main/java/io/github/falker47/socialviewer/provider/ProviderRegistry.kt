package io.github.falker47.socialviewer.provider

import android.net.Uri
import io.github.falker47.socialviewer.domain.SocialContent

class ProviderRegistry(
    private val providers: List<SocialProvider>,
) {
    fun resolve(rawUrl: String): SocialContent {
        val uri = Uri.parse(rawUrl)
        val provider = providers.firstOrNull { it.supports(uri) }
            ?: throw unsupportedUrlError(uri)
        return provider.resolve(uri)
    }

    fun providerFor(rawUrl: String): SocialProvider? {
        val uri = Uri.parse(rawUrl)
        return providers.firstOrNull { it.supports(uri) }
    }

    fun validationErrorFor(rawUrl: String): Exception? {
        val uri = Uri.parse(rawUrl)
        return if (providers.any { it.supports(uri) }) null else unsupportedUrlError(uri)
    }

    private fun unsupportedUrlError(uri: Uri): Exception =
        providers.firstNotNullOfOrNull { it.unsupportedUrlError(uri) }
            ?: UnsupportedProviderException(uri.toString())
}

class UnsupportedProviderException(url: String) :
    IllegalArgumentException("Provider non ancora supportato per: $url")
