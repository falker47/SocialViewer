package io.github.falker47.socialviewer.provider.instagram

import android.net.Uri
import io.github.falker47.socialviewer.domain.SocialContent
import io.github.falker47.socialviewer.network.UrlConnectionHttpClient
import io.github.falker47.socialviewer.provider.ProviderContentUnavailableException
import io.github.falker47.socialviewer.provider.SocialProvider
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val INSTAGRAM_OEMBED_ENDPOINT =
    "https://graph.facebook.com/v25.0/instagram_oembed"
private const val INSTAGRAM_DOCUMENT_BASE_URL = "https://www.instagram.com/"

class InstagramProvider(
    private val http: UrlConnectionHttpClient = UrlConnectionHttpClient(),
) : SocialProvider {
    override val id: String = "instagram"
    override val displayName: String = "Instagram"

    override fun supports(uri: Uri): Boolean =
        InstagramUrlPolicy.supports(uri.scheme, uri.host, uri.path)

    override fun resolve(uri: Uri): SocialContent {
        val canonical = InstagramUrlPolicy.canonicalUrl(
            scheme = uri.scheme,
            host = uri.host,
            path = uri.path,
        ) ?: error("URL Instagram non supportata")

        return resolveCanonicalInstagram(
            canonicalUrl = canonical,
            http = http,
        )
    }
}

internal fun resolveCanonicalInstagram(
    canonicalUrl: String,
    http: UrlConnectionHttpClient,
): SocialContent {
    val encodedUrl = URLEncoder.encode(canonicalUrl, StandardCharsets.UTF_8.name())
    val response = http.get("$INSTAGRAM_OEMBED_ENDPOINT?url=$encodedUrl")

    if (response.statusCode == 400 || response.statusCode == 404) {
        throw ProviderContentUnavailableException(
            providerName = "Instagram",
            technicalDetail = "Instagram oEmbed ha risposto HTTP ${response.statusCode}",
        )
    }
    if (response.statusCode !in 200..299) {
        error("Instagram oEmbed ha risposto HTTP ${response.statusCode}")
    }

    val responseHost = runCatching {
        URI(response.finalUrl).host?.lowercase()
    }.getOrNull()
    require(responseHost == "graph.facebook.com") {
        "Instagram oEmbed ha reindirizzato fuori da Meta"
    }

    val json = JSONObject(response.body)
    val providerHtml = json.optString("html").takeIf { it.isNotBlank() }
        ?: error("Instagram oEmbed non ha restituito markup incorporabile")

    return SocialContent(
        providerId = "instagram",
        providerName = "Instagram",
        canonicalUrl = canonicalUrl,
        title = json.optString("title").takeIf { it.isNotBlank() },
        authorName = json.optString("author_name").takeIf { it.isNotBlank() },
        documentBaseUrl = INSTAGRAM_DOCUMENT_BASE_URL,
        embedHtml = instagramDocument(providerHtml),
    )
}

internal fun instagramDocument(providerHtml: String): String = """
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
            display: flex;
            justify-content: center;
            align-items: flex-start;
            padding: 12px 0 24px;
            overflow-x: hidden;
          }
          #embed {
            width: 100%;
            display: flex;
            justify-content: center;
          }
          .instagram-media {
            min-width: 0 !important;
            width: calc(100% - 16px) !important;
            max-width: 540px !important;
            margin: 0 auto !important;
          }
        </style>
      </head>
      <body>
        <div id="embed">$providerHtml</div>
      </body>
    </html>
""".trimIndent()
