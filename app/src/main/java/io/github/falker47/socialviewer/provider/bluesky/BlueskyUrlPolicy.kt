package io.github.falker47.socialviewer.provider.bluesky

object BlueskyUrlPolicy {
    private const val HOST = "bsky.app"

    fun supports(
        scheme: String?,
        host: String?,
        path: String?,
    ): Boolean = canonicalPostUrl(scheme, host, path) != null

    fun canonicalPostUrl(
        scheme: String?,
        host: String?,
        path: String?,
    ): String? {
        if (!scheme.equals("https", ignoreCase = true)) return null
        if (!host.equals(HOST, ignoreCase = true)) return null

        val segments = pathSegments(path)
        if (segments.size != 4) return null
        if (!segments[0].equals("profile", ignoreCase = true)) return null
        if (!segments[2].equals("post", ignoreCase = true)) return null

        val identifier = canonicalIdentifier(segments[1]) ?: return null
        val recordKey = segments[3]
        if (!isValidRecordKey(recordKey)) return null

        return "https://bsky.app/profile/$identifier/post/$recordKey"
    }

    private fun canonicalIdentifier(value: String): String? = when {
        isValidDid(value) -> value
        isValidHandle(value) -> value.lowercase()
        else -> null
    }

    private fun isValidDid(value: String): Boolean {
        if (!value.startsWith("did:", ignoreCase = true)) return false
        if (value.length > 512) return false

        val firstSeparator = value.indexOf(':')
        val secondSeparator = value.indexOf(':', firstSeparator + 1)
        if (firstSeparator != 3 || secondSeparator <= firstSeparator + 1) return false

        val method = value.substring(firstSeparator + 1, secondSeparator)
        val methodSpecificId = value.substring(secondSeparator + 1)

        return method.all { it in 'a'..'z' || it in '0'..'9' } &&
            methodSpecificId.isNotBlank() &&
            methodSpecificId.all { char ->
                char.isLetterOrDigit() ||
                    char == '.' ||
                    char == ':' ||
                    char == '_' ||
                    char == '%' ||
                    char == '-'
            }
    }

    private fun isValidHandle(value: String): Boolean {
        if (value.length !in 3..253) return false
        if (value.contains("..")) return false

        val labels = value.split('.')
        if (labels.size < 2) return false

        return labels.all { label ->
            label.length in 1..63 &&
                label.firstOrNull()?.isLetterOrDigit() == true &&
                label.lastOrNull()?.isLetterOrDigit() == true &&
                label.all { it.isLetterOrDigit() || it == '-' }
        }
    }

    private fun isValidRecordKey(value: String): Boolean =
        value.length in 1..512 &&
            value != "." &&
            value != ".." &&
            value.all { char ->
                char.isLetterOrDigit() ||
                    char == '.' ||
                    char == '_' ||
                    char == '~' ||
                    char == ':' ||
                    char == '-'
            }

    private fun pathSegments(path: String?): List<String> =
        path
            ?.trim('/')
            ?.split('/')
            ?.filter { it.isNotBlank() }
            .orEmpty()
}
