package io.github.falker47.socialviewer.provider.bluesky

object BlueskyUrlPolicy {
    private const val HOST = "bsky.app"
    private val supportedDidMethods = setOf("plc", "web")

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
        if (!value.startsWith("did:")) return false
        if (value.length !in 7..2048) return false

        val secondSeparator = value.indexOf(':', startIndex = 4)
        if (secondSeparator <= 4) return false

        val method = value.substring(4, secondSeparator)
        val methodSpecificId = value.substring(secondSeparator + 1)

        return method in supportedDidMethods &&
            method.all { it in 'a'..'z' } &&
            methodSpecificId.isNotBlank() &&
            !methodSpecificId.endsWith(':') &&
            methodSpecificId.all { char ->
                isAsciiLetterOrDigit(char) ||
                    char == '.' ||
                    char == '_' ||
                    char == ':' ||
                    char == '%' ||
                    char == '-'
            }
    }

    private fun isValidHandle(value: String): Boolean {
        if (value.length !in 3..253) return false
        if (!value.all { isAsciiLetterOrDigit(it) || it == '.' || it == '-' }) return false
        if (value.contains("..")) return false

        val labels = value.split('.')
        if (labels.size < 2) return false
        if (labels.last().firstOrNull()?.let(::isAsciiLetter) != true) return false

        return labels.all { label ->
            label.length in 1..63 &&
                label.firstOrNull()?.let(::isAsciiLetterOrDigit) == true &&
                label.lastOrNull()?.let(::isAsciiLetterOrDigit) == true &&
                label.all { isAsciiLetterOrDigit(it) || it == '-' }
        }
    }

    private fun isValidRecordKey(value: String): Boolean =
        value.length in 1..512 &&
            value != "." &&
            value != ".." &&
            value.all { char ->
                isAsciiLetterOrDigit(char) ||
                    char == '.' ||
                    char == '_' ||
                    char == '~' ||
                    char == ':' ||
                    char == '-'
            }

    private fun isAsciiLetter(char: Char): Boolean =
        char in 'a'..'z' || char in 'A'..'Z'

    private fun isAsciiLetterOrDigit(char: Char): Boolean =
        isAsciiLetter(char) || char in '0'..'9'

    private fun pathSegments(path: String?): List<String> =
        path
            ?.trim('/')
            ?.split('/')
            ?.filter { it.isNotBlank() }
            .orEmpty()
}
