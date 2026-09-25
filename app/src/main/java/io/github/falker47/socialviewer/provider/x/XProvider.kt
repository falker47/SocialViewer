package io.github.falker47.socialviewer.provider.x

import android.net.Uri
import io.github.falker47.socialviewer.domain.SocialContent
import io.github.falker47.socialviewer.network.UrlConnectionHttpClient
import io.github.falker47.socialviewer.provider.ProviderContentUnavailableException
import io.github.falker47.socialviewer.provider.SocialProvider
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val X_OEMBED_ENDPOINT = "https://publish.x.com/oembed"
private const val X_DOCUMENT_BASE_URL = "https://x.com/"
private const val X_WIDGET_SCRIPT = "https://platform.x.com/widgets.js"
private val X_OEMBED_HOSTS = setOf("publish.x.com")

class XProvider(
    private val http: UrlConnectionHttpClient = UrlConnectionHttpClient(),
) : SocialProvider {
    override val id: String = "x"
    override val displayName: String = "X"

    override fun supports(uri: Uri): Boolean =
        XUrlPolicy.supports(uri.scheme, uri.host, uri.path)

    override fun resolve(uri: Uri): SocialContent {
        val canonical = XUrlPolicy.canonicalPostUrl(
            scheme = uri.scheme,
            host = uri.host,
            path = uri.path,
        ) ?: throw IllegalArgumentException("URL X non supportata")

        return resolveCanonicalX(
            canonicalUrl = canonical,
            http = http,
        )
    }
}

internal fun resolveCanonicalX(
    canonicalUrl: String,
    http: UrlConnectionHttpClient,
): SocialContent {
    val encodedUrl = URLEncoder.encode(canonicalUrl, StandardCharsets.UTF_8.name())
    val requestUrl =
        "$X_OEMBED_ENDPOINT?url=$encodedUrl&hide_thread=true&omit_script=true&dnt=true"
    val response = http.get(requestUrl)

    if (response.statusCode in setOf(400, 403, 404, 410)) {
        throw ProviderContentUnavailableException(
            providerName = "X",
            technicalDetail = "X oEmbed ha risposto HTTP ${response.statusCode}",
        )
    }
    if (response.statusCode !in 200..299) {
        error("X oEmbed ha risposto HTTP ${response.statusCode}")
    }

    val responseHost = runCatching {
        URI(response.finalUrl).host?.lowercase()
    }.getOrNull()
    require(responseHost in X_OEMBED_HOSTS) {
        "X oEmbed ha reindirizzato fuori da publish.x.com"
    }

    val json = JSONObject(response.body)
    val returnedProvider = json.optString("provider_name")
    require(
        returnedProvider.isBlank() ||
            returnedProvider.equals("twitter", ignoreCase = true) ||
            returnedProvider.equals("x", ignoreCase = true),
    ) {
        "X oEmbed ha restituito un provider inatteso"
    }

    val providerHtml = json.optString("html").takeIf { it.isNotBlank() }
        ?: error("X oEmbed non ha restituito markup incorporabile")

    return SocialContent(
        providerId = "x",
        providerName = "X",
        canonicalUrl = canonicalUrl,
        title = json.optString("title")
            .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) },
        authorName = json.optString("author_name")
            .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) },
        documentBaseUrl = X_DOCUMENT_BASE_URL,
        embedHtml = xDocument(providerHtml),
    )
}

internal fun xDocument(providerHtml: String): String = """
    <!doctype html>
    <html>
      <head>
        <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1" />
        <meta name="twitter:dnt" content="on" />
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
          let socialViewerXResolved = false;

          function socialViewerXRendered() {
            if (socialViewerXResolved) return;
            const embed = document.getElementById('embed');
            const iframe = embed && embed.querySelector('iframe');
            if (!iframe) return;

            socialViewerXResolved = true;
            embed.style.visibility = 'visible';
            const status = document.getElementById('status');
            if (status) status.style.display = 'none';
          }

          function socialViewerXError() {
            if (socialViewerXResolved) return;
            socialViewerXResolved = true;
            const status = document.getElementById('status');
            if (status) {
              status.textContent = 'Questo post X non è disponibile.';
              status.style.display = 'flex';
            }
          }

          window.addEventListener('DOMContentLoaded', function () {
            const embed = document.getElementById('embed');
            if (embed) {
              new MutationObserver(socialViewerXRendered).observe(
                embed,
                { childList: true, subtree: true }
              );
            }

            window.setTimeout(function () {
              socialViewerXRendered();
              if (!socialViewerXResolved) socialViewerXError();
            }, 12000);
          });
        </script>
      </head>
      <body>
        <div id="embed">$providerHtml</div>
        <div id="status">Caricamento post X…</div>
        <script
          async
          charset="utf-8"
          src="$X_WIDGET_SCRIPT"
          onload="socialViewerXRendered()"
          onerror="socialViewerXError()"
        ></script>
      </body>
    </html>
""".trimIndent()
