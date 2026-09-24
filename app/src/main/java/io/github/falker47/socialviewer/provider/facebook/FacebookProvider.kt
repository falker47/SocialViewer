package io.github.falker47.socialviewer.provider.facebook

import android.net.Uri
import io.github.falker47.socialviewer.domain.SocialContent
import io.github.falker47.socialviewer.network.UrlConnectionHttpClient
import io.github.falker47.socialviewer.provider.ProviderContentUnavailableException
import io.github.falker47.socialviewer.provider.SocialProvider
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val FACEBOOK_POST_OEMBED_ENDPOINT =
    "https://graph.facebook.com/v25.0/oembed_post"
private const val FACEBOOK_VIDEO_OEMBED_ENDPOINT =
    "https://graph.facebook.com/v25.0/oembed_video"
private const val FACEBOOK_DOCUMENT_BASE_URL = "https://www.facebook.com/"
private const val FACEBOOK_SDK_URL =
    "https://connect.facebook.net/en_US/sdk.js#xfbml=1&version=v25.0"

private val FACEBOOK_SDK_SCRIPT_REGEX = Regex(
    """<script\b[^>]*\bsrc=["\']https://connect\.facebook\.net/en_US/sdk\.js(?:[#?][^"\']*)?["\'][^>]*>\s*</script>""",
    RegexOption.IGNORE_CASE,
)

class FacebookProvider(
    private val http: UrlConnectionHttpClient = UrlConnectionHttpClient(),
) : SocialProvider {
    override val id: String = "facebook"
    override val displayName: String = "Facebook"

    override fun supports(uri: Uri): Boolean =
        FacebookUrlPolicy.supports(uri.scheme, uri.host, uri.path)

    override fun resolve(uri: Uri): SocialContent {
        val target = resolveFacebookTarget(
            scheme = uri.scheme,
            host = uri.host,
            path = uri.path,
            http = http,
        )

        return resolveCanonicalFacebook(
            target = target,
            http = http,
        )
    }
}

internal fun resolveFacebookTarget(
    scheme: String?,
    host: String?,
    path: String?,
    http: UrlConnectionHttpClient,
): FacebookCanonicalTarget {
    FacebookUrlPolicy.canonicalTarget(
        scheme = scheme,
        host = host,
        path = path,
    )?.let { return it }

    val shareAlias = FacebookUrlPolicy.shareReelAliasUrl(
        scheme = scheme,
        host = host,
        path = path,
    ) ?: error("URL Facebook non supportata")

    val resolvedUrl = http.resolveFinalUrl(shareAlias)
    val resolvedUri = URI(resolvedUrl)

    val target = FacebookUrlPolicy.canonicalTarget(
        scheme = resolvedUri.scheme,
        host = resolvedUri.host,
        path = resolvedUri.path,
    ) ?: error("Il link Facebook condiviso non ha risolto verso un contenuto supportato")

    require(target.kind == FacebookContentKind.REEL) {
        "Il link Facebook /share/r/ non ha risolto verso un Reel"
    }

    return target
}

internal fun resolveCanonicalFacebook(
    target: FacebookCanonicalTarget,
    http: UrlConnectionHttpClient,
): SocialContent {
    val endpoint = when (target.kind) {
        FacebookContentKind.POST -> FACEBOOK_POST_OEMBED_ENDPOINT
        FacebookContentKind.REEL -> FACEBOOK_VIDEO_OEMBED_ENDPOINT
    }

    val encodedUrl = URLEncoder.encode(target.canonicalUrl, StandardCharsets.UTF_8.name())
    val response = http.get("$endpoint?url=$encodedUrl")

    if (response.statusCode == 400 || response.statusCode == 404) {
        throw ProviderContentUnavailableException(
            providerName = "Facebook",
            technicalDetail = "Facebook oEmbed ha risposto HTTP ${response.statusCode}",
        )
    }
    if (response.statusCode !in 200..299) {
        error("Facebook oEmbed ha risposto HTTP ${response.statusCode}")
    }

    val responseHost = runCatching {
        URI(response.finalUrl).host?.lowercase()
    }.getOrNull()
    require(responseHost == "graph.facebook.com") {
        "Facebook oEmbed ha reindirizzato fuori da Meta"
    }

    val json = JSONObject(response.body)
    val providerHtml = json.optString("html").takeIf { it.isNotBlank() }
        ?: error("Facebook oEmbed non ha restituito markup incorporabile")

    return SocialContent(
        providerId = "facebook",
        providerName = "Facebook",
        canonicalUrl = target.canonicalUrl,
        title = json.optString("title").takeIf { it.isNotBlank() },
        authorName = json.optString("author_name").takeIf { it.isNotBlank() },
        documentBaseUrl = FACEBOOK_DOCUMENT_BASE_URL,
        embedHtml = facebookDocument(providerHtml),
    )
}

internal fun facebookDocument(providerHtml: String): String {
    val embedMarkup = providerHtml.replace(FACEBOOK_SDK_SCRIPT_REGEX, "").trim()

    return """
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
                padding: 12px 0 24px;
                overflow-x: hidden;
              }
              #embed {
                width: 100%;
                display: flex;
                justify-content: center;
              }
              .fb-post,
              .fb-video {
                max-width: 100% !important;
                margin: 0 auto !important;
              }
            </style>
          </head>
          <body>
            <div id="embed">$embedMarkup</div>
            <script async defer crossorigin="anonymous" src="$FACEBOOK_SDK_URL"></script>
          </body>
        </html>
    """.trimIndent()
}
