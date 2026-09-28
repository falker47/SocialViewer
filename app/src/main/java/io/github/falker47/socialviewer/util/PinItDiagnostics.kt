package io.github.falker47.socialviewer.util

import android.util.Log
import java.net.URI

internal object PinItDiagnostics {
    const val TAG: String = "SVPinItDiag"

    fun tracks(rawUrl: String?): Boolean {
        if (rawUrl.isNullOrBlank()) return false
        return runCatching {
            val uri = URI(rawUrl.trim())
            uri.scheme.equals("https", ignoreCase = true) &&
                uri.host.equals("pin.it", ignoreCase = true)
        }.getOrDefault(false)
    }

    fun info(
        stage: String,
        vararg fields: Pair<String, Any?>,
    ) {
        runCatching {
            Log.i(TAG, line(stage, *fields))
        }
    }

    fun error(
        stage: String,
        url: String,
        throwable: Throwable,
    ) {
        runCatching {
            Log.e(
                TAG,
                line(
                    stage,
                    "url" to safeUrl(url),
                    "exception" to throwable.javaClass.name,
                    "message" to throwable.message,
                ),
            )
        }
    }

    internal fun line(
        stage: String,
        vararg fields: Pair<String, Any?>,
    ): String = buildString {
        append("stage=")
        append(sanitize(stage))
        fields.forEach { (key, value) ->
            append(" | ")
            append(sanitize(key))
            append("=")
            append(sanitize(value?.toString() ?: "<none>"))
        }
    }

    internal fun safeUrl(rawUrl: String?): String {
        if (rawUrl.isNullOrBlank()) return "<none>"
        return runCatching {
            val uri = URI(rawUrl.trim())
            val path = uri.rawPath.orEmpty().ifBlank { "/" }
            when {
                uri.scheme != null && uri.host != null -> buildString {
                    append(uri.scheme.lowercase())
                    append("://")
                    append(uri.host.lowercase())
                    if (uri.port >= 0) append(":").append(uri.port)
                    append(path)
                }

                uri.host != null -> "//${uri.host.lowercase()}$path"
                uri.rawPath != null -> path
                else -> sanitize(rawUrl)
            }
        }.getOrElse {
            sanitize(rawUrl)
        }
    }

    private fun sanitize(value: String): String =
        value
            .replace(Regex("[\\r\\n\\t]+"), " ")
            .trim()
            .take(MAX_FIELD_LENGTH)

    private const val MAX_FIELD_LENGTH = 400
}
