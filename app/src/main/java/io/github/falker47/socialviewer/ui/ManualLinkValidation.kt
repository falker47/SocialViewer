package io.github.falker47.socialviewer.ui

import android.content.Context
import android.net.Uri
import io.github.falker47.socialviewer.R
import io.github.falker47.socialviewer.provider.ProviderRegistry
import io.github.falker47.socialviewer.util.UrlExtractor

internal sealed interface ManualLinkResult {
    data class Valid(val url: String) : ManualLinkResult
    data class Invalid(val message: String, val displayValue: String) : ManualLinkResult
}

internal fun validateManualLink(
    rawValue: String,
    registry: ProviderRegistry,
    context: Context,
): ManualLinkResult {
    val trimmed = rawValue.trim()
    val extracted = UrlExtractor.firstHttpUrl(trimmed)
    val candidate = extracted ?: trimmed

    if (candidate.isBlank()) {
        return ManualLinkResult.Invalid(
            message = context.getString(R.string.error_no_link_clipboard),
            displayValue = "",
        )
    }

    val uri = Uri.parse(candidate)
    if (!uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank()) {
        return ManualLinkResult.Invalid(
            message = context.getString(R.string.error_invalid_link),
            displayValue = candidate,
        )
    }

    registry.validationErrorFor(candidate)?.let { error ->
        val copy = viewerErrorCopyFor(error)
        return ManualLinkResult.Invalid(
            message = context.getString(copy.messageRes, *copy.messageArgs.toTypedArray()),
            displayValue = candidate,
        )
    }

    return ManualLinkResult.Valid(candidate)
}
