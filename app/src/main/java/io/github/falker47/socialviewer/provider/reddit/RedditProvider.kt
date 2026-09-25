package io.github.falker47.socialviewer.provider.reddit

import android.net.Uri
import io.github.falker47.socialviewer.domain.SocialContent
import io.github.falker47.socialviewer.network.UrlConnectionHttpClient
import io.github.falker47.socialviewer.provider.ProviderContentUnavailableException
import io.github.falker47.socialviewer.provider.SocialProvider
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val REDDIT_OEMBED_ENDPOINT = "https://www.reddit.com/oembed"
private const val REDDIT_DOCUMENT_BASE_URL = "https://www.reddit.com/"
private val REDDIT_OEMBED_HOSTS = setOf("reddit.com", "www.reddit.com")

class RedditProvider(
    private val http: UrlConnectionHttpClient = UrlConnectionHttpClient(),
) : SocialProvider {
    override val id: String = "reddit"
    override val displayName: String = "Reddit"

    override fun supports(uri: Uri): Boolean =
        RedditUrlPolicy.supports(uri.scheme, uri.host, uri.path)

    override fun resolve(uri: Uri): SocialContent {
        val canonical = canonicalRedditUrlFor(
            scheme = uri.scheme,
            host = uri.host,
            path = uri.path,
            rawUrl = uri.toString(),
            http = http,
        )

        return resolveCanonicalReddit(
            canonicalUrl = canonical,
            http = http,
        )
    }
}

internal fun canonicalRedditUrlFor(
    scheme: String?,
    host: String?,
    path: String?,
    rawUrl: String,
    http: UrlConnectionHttpClient,
): String {
    RedditUrlPolicy.canonicalPermalink(scheme, host, path)?.let { return it }

    require(RedditUrlPolicy.requiresRedirectResolution(scheme, host, path)) {
        "URL Reddit non supportata"
    }

    val finalUrl = http.resolveFinalUrl(rawUrl)
    val finalUri = runCatching { URI(finalUrl) }
        .getOrElse { throw IllegalArgumentException("Redirect Reddit non valido", it) }

    return RedditUrlPolicy.canonicalPermalink(
        scheme = finalUri.scheme,
        host = finalUri.host,
        path = finalUri.path,
    ) ?: throw IllegalArgumentException(
        "Il link di condivisione Reddit non ha risolto a un permalink Reddit pubblico supportato",
    )
}

internal fun resolveCanonicalReddit(
    canonicalUrl: String,
    http: UrlConnectionHttpClient,
): SocialContent {
    val encodedUrl = URLEncoder.encode(canonicalUrl, StandardCharsets.UTF_8.name())
    val response = http.get("$REDDIT_OEMBED_ENDPOINT?url=$encodedUrl")

    if (response.statusCode in setOf(400, 403, 404, 410)) {
        throw ProviderContentUnavailableException(
            providerName = "Reddit",
            technicalDetail = "Reddit oEmbed ha risposto HTTP ${response.statusCode}",
        )
    }
    if (response.statusCode !in 200..299) {
        error("Reddit oEmbed ha risposto HTTP ${response.statusCode}")
    }

    val responseHost = runCatching {
        URI(response.finalUrl).host?.lowercase()
    }.getOrNull()
    require(responseHost in REDDIT_OEMBED_HOSTS) {
        "Reddit oEmbed ha reindirizzato fuori da Reddit"
    }

    val json = JSONObject(response.body)
    val returnedProvider = json.optString("provider_name")
    require(returnedProvider.isBlank() || returnedProvider.equals("reddit", ignoreCase = true)) {
        "Reddit oEmbed ha restituito un provider inatteso"
    }

    val providerHtml = json.optString("html").takeIf { it.isNotBlank() }
        ?: error("Reddit oEmbed non ha restituito markup incorporabile")

    return SocialContent(
        providerId = "reddit",
        providerName = "Reddit",
        canonicalUrl = canonicalUrl,
        title = json.optString("title").takeIf { it.isNotBlank() },
        authorName = json.optString("author_name").takeIf { it.isNotBlank() },
        documentBaseUrl = REDDIT_DOCUMENT_BASE_URL,
        embedHtml = redditDocument(providerHtml),
    )
}

internal fun redditDocument(providerHtml: String): String = """
    <!doctype html>
    <html>
      <head>
        <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1" />
        <style>
          * { box-sizing: border-box; }
          html, body {
            margin: 0;
            width: 100%;
            min-height: 100%;
            background: #000;
          }
          body {
            padding: 12px 8px 24px;
            overflow-x: hidden;
          }
          #embed {
            width: 100%;
            max-width: 720px;
            margin: 0 auto;
          }
        </style>
      </head>
      <body>
        <div id="embed">$providerHtml</div>
      </body>
    </html>
""".trimIndent()
