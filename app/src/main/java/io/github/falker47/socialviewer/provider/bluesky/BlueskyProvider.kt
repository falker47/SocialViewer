package io.github.falker47.socialviewer.provider.bluesky

import android.net.Uri
import io.github.falker47.socialviewer.domain.SocialContent
import io.github.falker47.socialviewer.network.UrlConnectionHttpClient
import io.github.falker47.socialviewer.provider.ProviderContentUnavailableException
import io.github.falker47.socialviewer.provider.SocialProvider
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val BLUESKY_OEMBED_ENDPOINT = "https://embed.bsky.app/oembed"
private const val BLUESKY_DOCUMENT_BASE_URL = "https://bsky.app/"
private val BLUESKY_OEMBED_HOSTS = setOf("embed.bsky.app")

class BlueskyProvider(
    private val http: UrlConnectionHttpClient = UrlConnectionHttpClient(),
) : SocialProvider {
    override val id: String = "bluesky"
    override val displayName: String = "Bluesky"

    override fun supports(uri: Uri): Boolean =
        BlueskyUrlPolicy.supports(uri.scheme, uri.host, uri.path)

    override fun resolve(uri: Uri): SocialContent {
        val canonical = BlueskyUrlPolicy.canonicalPostUrl(
            scheme = uri.scheme,
            host = uri.host,
            path = uri.path,
        ) ?: throw IllegalArgumentException("URL Bluesky non supportata")

        return resolveCanonicalBluesky(
            canonicalUrl = canonical,
            http = http,
        )
    }
}

internal fun resolveCanonicalBluesky(
    canonicalUrl: String,
    http: UrlConnectionHttpClient,
): SocialContent {
    val encodedUrl = URLEncoder.encode(canonicalUrl, StandardCharsets.UTF_8.name())
    val response = http.get(
        "$BLUESKY_OEMBED_ENDPOINT?url=$encodedUrl&format=json&maxwidth=550",
    )

    if (response.statusCode in setOf(400, 403, 404, 410, 422)) {
        throw ProviderContentUnavailableException(
            providerName = "Bluesky",
            technicalDetail = "Bluesky oEmbed ha risposto HTTP ${response.statusCode}",
        )
    }
    if (response.statusCode !in 200..299) {
        error("Bluesky oEmbed ha risposto HTTP ${response.statusCode}")
    }

    val responseHost = runCatching {
        URI(response.finalUrl).host?.lowercase()
    }.getOrNull()
    require(responseHost in BLUESKY_OEMBED_HOSTS) {
        "Bluesky oEmbed ha reindirizzato fuori da embed.bsky.app"
    }

    val json = JSONObject(response.body)
    val returnedType = json.optString("type")
    require(returnedType.isBlank() || returnedType.equals("rich", ignoreCase = true)) {
        "Bluesky oEmbed ha restituito un tipo inatteso"
    }

    val returnedProvider = json.optString("provider_name")
    require(
        returnedProvider.isBlank() ||
            returnedProvider.equals("Bluesky Social", ignoreCase = true) ||
            returnedProvider.equals("Bluesky", ignoreCase = true),
    ) {
        "Bluesky oEmbed ha restituito un provider inatteso"
    }

    val providerHtml = json.optString("html").takeIf { it.isNotBlank() }
        ?: error("Bluesky oEmbed non ha restituito markup incorporabile")

    return SocialContent(
        providerId = "bluesky",
        providerName = "Bluesky",
        canonicalUrl = canonicalUrl,
        title = null,
        authorName = json.optString("author_name")
            .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) },
        documentBaseUrl = BLUESKY_DOCUMENT_BASE_URL,
        embedHtml = blueskyDocument(providerHtml),
    )
}

internal fun blueskyDocument(providerHtml: String): String = """
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
            color: #fff;
            font-family: sans-serif;
          }
          body {
            display: flex;
            justify-content: center;
            align-items: flex-start;
            padding: 12px 8px 24px;
            overflow-x: hidden;
          }
          #embed {
            width: 100%;
            max-width: 720px;
            margin: 0 auto;
            display: flex;
            justify-content: center;
            visibility: hidden;
          }
          #status {
            position: fixed;
            inset: 0;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 24px;
            text-align: center;
            background: #000;
            color: #fff;
            font-size: 16px;
            line-height: 1.4;
          }
        </style>
        <script>
          let socialViewerBlueskyResolved = false;

          function socialViewerBlueskyRendered() {
            if (socialViewerBlueskyResolved) return;
            const embed = document.getElementById('embed');
            const iframe = embed && embed.querySelector('iframe');
            if (!iframe) return;

            socialViewerBlueskyResolved = true;
            embed.style.visibility = 'visible';
            const status = document.getElementById('status');
            if (status) status.style.display = 'none';
          }

          function socialViewerBlueskyError() {
            if (socialViewerBlueskyResolved) return;
            socialViewerBlueskyResolved = true;
            const status = document.getElementById('status');
            if (status) {
              status.textContent = 'Questo post Bluesky non è disponibile.';
              status.style.display = 'flex';
            }
          }

          window.addEventListener('DOMContentLoaded', function () {
            const embed = document.getElementById('embed');
            if (embed) {
              new MutationObserver(socialViewerBlueskyRendered).observe(
                embed,
                { childList: true, subtree: true }
              );
            }

            socialViewerBlueskyRendered();
            window.setTimeout(function () {
              socialViewerBlueskyRendered();
              if (!socialViewerBlueskyResolved) socialViewerBlueskyError();
            }, 12000);
          });
        </script>
      </head>
      <body>
        <div id="embed">$providerHtml</div>
        <div id="status">Caricamento post Bluesky…</div>
      </body>
    </html>
""".trimIndent()
