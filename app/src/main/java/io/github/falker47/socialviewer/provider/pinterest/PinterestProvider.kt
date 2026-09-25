package io.github.falker47.socialviewer.provider.pinterest

import android.net.Uri
import io.github.falker47.socialviewer.domain.SocialContent
import io.github.falker47.socialviewer.network.UrlConnectionHttpClient
import io.github.falker47.socialviewer.provider.SocialProvider
import java.net.URI

private const val PINTEREST_DOCUMENT_BASE_URL = "https://www.pinterest.com/"
private const val PINTEREST_WIDGET_SCRIPT = "https://assets.pinterest.com/js/pinit.js"

class PinterestProvider(
    private val http: UrlConnectionHttpClient = UrlConnectionHttpClient(),
) : SocialProvider {
    override val id: String = "pinterest"
    override val displayName: String = "Pinterest"

    override fun supports(uri: Uri): Boolean =
        PinterestUrlPolicy.supports(uri.scheme, uri.host, uri.path)

    override fun resolve(uri: Uri): SocialContent {
        val canonical = canonicalPinterestUrlFor(
            scheme = uri.scheme,
            host = uri.host,
            path = uri.path,
            rawUrl = uri.toString(),
            http = http,
        )

        return resolveCanonicalPinterest(canonical)
    }
}

internal fun canonicalPinterestUrlFor(
    scheme: String?,
    host: String?,
    path: String?,
    rawUrl: String,
    http: UrlConnectionHttpClient,
): String {
    PinterestUrlPolicy.canonicalPinUrl(scheme, host, path)?.let { return it }

    require(PinterestUrlPolicy.requiresRedirectResolution(scheme, host, path)) {
        "URL Pinterest non supportata"
    }

    val finalUrl = http.resolveFinalUrl(rawUrl)
    val finalUri = runCatching { URI(finalUrl) }
        .getOrElse { throw IllegalArgumentException("Redirect Pinterest non valido", it) }

    return PinterestUrlPolicy.canonicalPinUrl(
        scheme = finalUri.scheme,
        host = finalUri.host,
        path = finalUri.path,
    ) ?: throw IllegalArgumentException(
        "Il link di condivisione Pinterest non ha risolto a un Pin pubblico supportato",
    )
}

internal fun resolveCanonicalPinterest(canonicalUrl: String): SocialContent =
    SocialContent(
        providerId = "pinterest",
        providerName = "Pinterest",
        canonicalUrl = canonicalUrl,
        title = null,
        authorName = null,
        documentBaseUrl = PINTEREST_DOCUMENT_BASE_URL,
        embedHtml = pinterestDocument(canonicalUrl),
    )

internal fun pinterestDocument(canonicalUrl: String): String = """
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
          let pinterestResolved = false;

          function socialViewerPinterestRendered() {
            if (pinterestResolved) return;
            const pin = document.getElementById('social-viewer-pin');
            const embed = document.getElementById('embed');
            if (!pin || (embed && embed.children.length > 1)) {
              pinterestResolved = true;
              const status = document.getElementById('status');
              if (status) status.style.display = 'none';
            }
          }

          function socialViewerPinterestError() {
            if (pinterestResolved) return;
            pinterestResolved = true;
            const status = document.getElementById('status');
            if (status) {
              status.textContent = 'Questo Pin Pinterest non è disponibile.';
              status.style.display = 'flex';
            }
          }

          window.addEventListener('DOMContentLoaded', function () {
            const embed = document.getElementById('embed');
            if (embed) {
              new MutationObserver(socialViewerPinterestRendered).observe(
                embed,
                { childList: true, subtree: true }
              );
            }

            window.setTimeout(function () {
              socialViewerPinterestRendered();
              if (!pinterestResolved) socialViewerPinterestError();
            }, 10000);
          });
        </script>
      </head>
      <body>
        <div id="embed">
          <a
            id="social-viewer-pin"
            href="$canonicalUrl"
            data-pin-do="embedPin"
          ></a>
        </div>
        <div id="status">Caricamento Pin…</div>
        <script
          type="text/javascript"
          async
          defer
          data-pin-error="socialViewerPinterestError"
          src="$PINTEREST_WIDGET_SCRIPT"
        ></script>
      </body>
    </html>
""".trimIndent()
