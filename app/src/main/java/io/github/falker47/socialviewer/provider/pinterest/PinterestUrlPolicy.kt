package io.github.falker47.socialviewer.provider.pinterest

object PinterestUrlPolicy {
    private val primaryHosts = setOf(
        "pinterest.com",
        "www.pinterest.com",
    )

    private val regionalPinterestComHost = Regex("^[a-z]{2}\\.pinterest\\.com$")

    fun supports(
        scheme: String?,
        host: String?,
        path: String?,
    ): Boolean =
        canonicalPinUrl(scheme, host, path) != null ||
            requiresRedirectResolution(scheme, host, path)

    fun canonicalPinUrl(
        scheme: String?,
        host: String?,
        path: String?,
    ): String? {
        if (!scheme.equals("https", ignoreCase = true)) return null

        val normalizedHost = host?.lowercase() ?: return null
        if (!isSupportedPinHost(normalizedHost)) return null

        val segments = pathSegments(path)
        if (segments.size !in 2..3) return null
        if (!segments[0].equals("pin", ignoreCase = true)) return null
        if (segments.size == 3 && !segments[2].equals("sent", ignoreCase = true)) return null

        val pinId = pinIdFromPathSegment(segments[1]) ?: return null
        return "https://www.pinterest.com/pin/$pinId/"
    }

    fun requiresRedirectResolution(
        scheme: String?,
        host: String?,
        path: String?,
    ): Boolean {
        if (!scheme.equals("https", ignoreCase = true)) return false
        if (!host.equals("pin.it", ignoreCase = true)) return false

        val segments = pathSegments(path)
        return segments.size == 1 && isValidShareToken(segments[0])
    }

    private fun isSupportedPinHost(host: String): Boolean =
        host in primaryHosts || regionalPinterestComHost.matches(host)

    private fun pinIdFromPathSegment(value: String): String? {
        if (isValidPinId(value)) return value

        val separatorIndex = value.lastIndexOf("--")
        if (separatorIndex <= 0) return null

        val pinId = value.substring(separatorIndex + 2)
        return pinId.takeIf(::isValidPinId)
    }

    private fun pathSegments(path: String?): List<String> =
        path
            ?.trim('/')
            ?.split('/')
            ?.filter { it.isNotBlank() }
            .orEmpty()

    private fun isValidPinId(value: String): Boolean =
        value.isNotBlank() && value.all { it in '0'..'9' }

    private fun isValidShareToken(value: String): Boolean =
        value.isNotBlank() &&
            value.all { char ->
                char in 'a'..'z' ||
                    char in 'A'..'Z' ||
                    char in '0'..'9' ||
                    char == '_' ||
                    char == '-'
            }
}
