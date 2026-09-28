package io.github.falker47.socialviewer.network

import java.net.HttpURLConnection
import java.net.URL

open class UrlConnectionHttpClient {
    data class Response(
        val statusCode: Int,
        val finalUrl: String,
        val body: String,
    )

    open fun resolveFinalUrl(url: String): String {
        val connection = open(url, followRedirects = true)
        return try {
            val code = connection.responseCode
            if (code !in 200..399) {
                error("HTTP $code durante la risoluzione del link")
            }
            connection.url.toString()
        } finally {
            connection.disconnect()
        }
    }

    open fun get(url: String): Response = get(url, emptyMap())

    open fun get(
        url: String,
        headers: Map<String, String>,
    ): Response {
        val connection = open(
            url = url,
            followRedirects = true,
            headers = headers,
        )
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..399) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            Response(
                statusCode = code,
                finalUrl = connection.url.toString(),
                body = body,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun open(
        url: String,
        followRedirects: Boolean,
        headers: Map<String, String> = emptyMap(),
    ): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = followRedirects
            connectTimeout = 10_000
            readTimeout = 15_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json,text/html;q=0.9,*/*;q=0.8")
            setRequestProperty("User-Agent", "SocialViewer/0.1 Android")
            headers.forEach { (name, value) ->
                setRequestProperty(name, value)
            }
        }
}
