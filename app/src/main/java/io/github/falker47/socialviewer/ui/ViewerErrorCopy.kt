package io.github.falker47.socialviewer.ui

import io.github.falker47.socialviewer.provider.ProviderContentUnavailableException

internal data class ViewerErrorCopy(
    val title: String,
    val message: String,
)

internal fun viewerErrorCopyFor(error: Throwable): ViewerErrorCopy =
    when (error) {
        is ProviderContentUnavailableException -> ViewerErrorCopy(
            title = "Contenuto non disponibile",
            message = "Questo contenuto ${error.providerName} non è disponibile.",
        )

        else -> ViewerErrorCopy(
            title = "Impossibile aprire il contenuto",
            message = error.message ?: "Si è verificato un errore inatteso.",
        )
    }
