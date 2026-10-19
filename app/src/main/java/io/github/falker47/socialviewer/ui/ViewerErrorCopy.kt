package io.github.falker47.socialviewer.ui

import io.github.falker47.socialviewer.R
import io.github.falker47.socialviewer.network.LinkResolutionHttpException
import io.github.falker47.socialviewer.provider.ProviderConfigurationException
import io.github.falker47.socialviewer.provider.ProviderContentUnavailableException
import io.github.falker47.socialviewer.provider.ProviderHttpException
import io.github.falker47.socialviewer.provider.ProviderInvalidResponseException
import io.github.falker47.socialviewer.provider.ProviderPolicyBlockedException
import io.github.falker47.socialviewer.provider.pinterest.PinterestPinRequiredException
import java.io.IOException
import java.net.SocketTimeoutException

internal data class ViewerErrorCopy(
    val titleRes: Int,
    val messageRes: Int,
    val messageArgs: List<Any> = emptyList(),
)

internal fun viewerErrorCopyFor(error: Throwable): ViewerErrorCopy =
    when (error) {
        is SocketTimeoutException -> ViewerErrorCopy(
            titleRes = R.string.error_title_connection_failed,
            messageRes = R.string.error_request_timeout,
        )

        is IOException -> ViewerErrorCopy(
            titleRes = R.string.error_title_connection_failed,
            messageRes = R.string.error_connection_failed,
        )

        is LinkResolutionHttpException -> ViewerErrorCopy(
            titleRes = R.string.error_title_open_failed,
            messageRes = R.string.error_link_http_format,
            messageArgs = listOf(error.statusCode),
        )

        is ProviderHttpException -> ViewerErrorCopy(
            titleRes = R.string.error_title_open_failed,
            messageRes = R.string.error_provider_metadata_http_format,
            messageArgs = listOf(error.providerName, error.statusCode),
        )

        is ProviderInvalidResponseException -> ViewerErrorCopy(
            titleRes = R.string.error_title_open_failed,
            messageRes = R.string.error_provider_invalid_response_format,
            messageArgs = listOf(error.providerName, error.statusCode),
        )

        is PinterestPinRequiredException -> ViewerErrorCopy(
            titleRes = R.string.error_title_unsupported_link,
            messageRes = R.string.error_pinterest_pin_required,
        )

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
