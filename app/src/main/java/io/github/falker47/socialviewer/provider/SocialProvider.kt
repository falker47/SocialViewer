package io.github.falker47.socialviewer.provider

import android.net.Uri
import io.github.falker47.socialviewer.domain.SocialContent

interface SocialProvider {
    val id: String
    val displayName: String

    fun supports(uri: Uri): Boolean

    /**
     * Resolve a public social URL into renderable content.
     *
     * Implementations must use official/public embedding mechanisms where available.
     * They must not bypass authentication, access controls, or private-content gates.
     */
    fun resolve(uri: Uri): SocialContent
}

class ProviderContentUnavailableException(
    val providerName: String,
    val technicalDetail: String,
) : IllegalStateException(technicalDetail)

