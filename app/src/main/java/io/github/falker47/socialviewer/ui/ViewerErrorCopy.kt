package io.github.falker47.socialviewer.ui

import io.github.falker47.socialviewer.provider.ProviderContentUnavailableException
import io.github.falker47.socialviewer.provider.ProviderShareLinkResolutionException

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

        is ProviderShareLinkResolutionException -> ViewerErrorCopy(
            title = "Link ${error.providerName} non risolvibile",
            message = "Facebook non ha fornito il permalink pubblico di questo link di condivisione. " +
                "Apri l'originale e usa, se disponibile, un link ${error.canonicalHint}.",
        )

        else -> ViewerErrorCopy(
            title = "Impossibile aprire il contenuto",
            message = error.message ?: "Si è verificato un errore inatteso.",
        )
    }
