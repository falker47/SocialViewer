package io.github.falker47.socialviewer.ui

import io.github.falker47.socialviewer.provider.ProviderConfigurationException
import io.github.falker47.socialviewer.provider.ProviderContentUnavailableException
import io.github.falker47.socialviewer.provider.ProviderPolicyBlockedException

internal data class ViewerErrorCopy(
    val title: String,
    val message: String,
)

internal fun viewerErrorCopyFor(error: Throwable): ViewerErrorCopy =
    when (error) {
        is ProviderPolicyBlockedException -> ViewerErrorCopy(
            title = "Contenuto non supportato",
            message = error.userMessage,
        )

        is ProviderConfigurationException -> ViewerErrorCopy(
            title = "Provider non configurato",
            message = error.userMessage,
        )

        is ProviderContentUnavailableException -> ViewerErrorCopy(
            title = "Contenuto non disponibile",
            message = "Questo contenuto ${error.providerName} non è disponibile.",
        )

        else -> ViewerErrorCopy(
            title = "Impossibile aprire il contenuto",
            message = error.message ?: "Si è verificato un errore inatteso.",
        )
    }
