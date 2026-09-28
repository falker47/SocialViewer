package io.github.falker47.socialviewer.ui

import io.github.falker47.socialviewer.R
import io.github.falker47.socialviewer.provider.ProviderConfigurationException
import io.github.falker47.socialviewer.provider.ProviderContentUnavailableException
import io.github.falker47.socialviewer.provider.ProviderPolicyBlockedException

internal data class ViewerErrorCopy(
    val titleRes: Int,
    val messageRes: Int,
    val messageArgs: List<Any> = emptyList(),
)

internal fun viewerErrorCopyFor(error: Throwable): ViewerErrorCopy =
    when (error) {
        is ProviderPolicyBlockedException -> ViewerErrorCopy(
            titleRes = R.string.error_title_policy_blocked,
            messageRes = if (error.providerName == "YouTube") {
                R.string.error_youtube_mfk
            } else {
                R.string.error_unexpected
            },
        )

        is ProviderConfigurationException -> ViewerErrorCopy(
            titleRes = R.string.error_title_provider_configuration,
            messageRes = R.string.error_provider_not_configured_format,
            messageArgs = listOf(error.providerName),
        )

        is ProviderContentUnavailableException -> ViewerErrorCopy(
            titleRes = R.string.error_title_unavailable,
            messageRes = R.string.error_content_unavailable_format,
            messageArgs = listOf(error.providerName),
        )

        else -> ViewerErrorCopy(
            titleRes = R.string.error_title_open_failed,
            messageRes = R.string.error_unexpected,
        )
    }
