package io.github.falker47.socialviewer.provider.pinterest

import android.net.Uri
import io.github.falker47.socialviewer.domain.SocialContent
import io.github.falker47.socialviewer.network.UrlConnectionHttpClient
import io.github.falker47.socialviewer.provider.SocialProvider
import io.github.falker47.socialviewer.util.PinItDiagnostics
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

private const val PINTEREST_DOCUMENT_BASE_URL = "https://www.pinterest.com/"
private const val PINTEREST_WIDGET_SCRIPT = "https://assets.pinterest.com/js/pinit.js"
private const val PINTEREST_REDIRECT_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/153.0 Mobile Safari/537.36"

private val PINTEREST_PIN_URL_IN_BODY = Regex(
    """(?i)(?:https:)?//(?:(?:www|[a-z]{2})\.)?pinterest\.com/pin/[^"'<>\\\s]+""",
)

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
    val diagnosticsEnabled = PinItDiagnostics.tracks(rawUrl)
    val directCanonical = PinterestUrlPolicy.canonicalPinUrl(scheme, host, path)
    if (diagnosticsEnabled) {
        PinItDiagnostics.info(
            "canonical_gate",
            "gate" to "input_url",
            "result" to if (directCanonical != null) "pass" else "miss",
            "url" to PinItDiagnostics.safeUrl(rawUrl),
        )
    }
    directCanonical?.let { return it }

    require(PinterestUrlPolicy.requiresRedirectResolution(scheme, host, path)) {
        "URL Pinterest non supportata"
    }

    // pin.it may finish as a normal HTTP redirect or as a lightweight landing page
    // containing the final Pinterest Pin URL. Resolve both shapes, but accept only
    // a URL that still passes the strict single-Pin policy.
    val response = http.get(
        url = rawUrl,
        headers = mapOf(
            "User-Agent" to PINTEREST_REDIRECT_USER_AGENT,
            "Accept-Language" to "en-US,en;q=0.9",
        ),
    )

    if (diagnosticsEnabled) {
        PinItDiagnostics.info(
            "http_response",
            "requestUrl" to PinItDiagnostics.safeUrl(rawUrl),
            "status" to response.statusCode,
            "finalUrl" to PinItDiagnostics.safeUrl(response.finalUrl),
            "location" to response.location?.let(PinItDiagnostics::safeUrl),
            "contentType" to response.contentType,
            "bodyLength" to response.body.length,
        )
    }

    if (response.statusCode !in 200..399) {
        throw IllegalArgumentException(
            "HTTP ${response.statusCode} durante la risoluzione del link Pinterest",
        )
    }

    val finalCanonical = canonicalPinterestUrlOrNull(response.finalUrl)
    if (diagnosticsEnabled) {
        PinItDiagnostics.info(
            "canonical_gate",
            "gate" to "final_url",
            "result" to if (finalCanonical != null) "pass" else "miss",
            "finalUrl" to PinItDiagnostics.safeUrl(response.finalUrl),
        )
    }
    finalCanonical?.let { return it }

    val bodyCandidates = pinterestPinCandidatesFromBody(response.body)
    val bodyCanonical = bodyCandidates.firstNotNullOfOrNull(::canonicalPinterestUrlOrNull)
    if (diagnosticsEnabled) {
        PinItDiagnostics.info(
            "canonical_gate",
            "gate" to "body_candidate",
            "result" to if (bodyCanonical != null) "pass" else "miss",
            "candidateCount" to bodyCandidates.size,
            "firstCandidate" to bodyCandidates.firstOrNull()?.let(PinItDiagnostics::safeUrl),
        )
    }
    bodyCanonical?.let { return it }

    if (diagnosticsEnabled) {
        PinItDiagnostics.info(
            "canonicalization_failed",
            "gate" to "body_candidate",
            "url" to PinItDiagnostics.safeUrl(rawUrl),
        )
    }
    throw IllegalArgumentException(
        "Il link di condivisione Pinterest non ha risolto a un Pin pubblico supportato",
    )
}

private fun canonicalPinterestUrlOrNull(url: String): String? {
    val uri = runCatching { URI(url) }.getOrNull() ?: return null
    return PinterestUrlPolicy.canonicalPinUrl(
        scheme = uri.scheme,
        host = uri.host,
        path = uri.path,
    )
}

internal fun pinterestPinCandidatesFromBody(body: String): List<String> {
    if (body.isBlank()) return emptyList()

    val normalized = buildList {
        add(body.replace("\\/", "/").replace("&amp;", "&"))
        runCatching {
            URLDecoder.decode(body, StandardCharsets.UTF_8)
                .replace("\\/", "/")
                .replace("&amp;", "&")
        }.getOrNull()?.let(::add)
    }

    return normalized
        .asSequence()
        .flatMap { PINTEREST_PIN_URL_IN_BODY.findAll(it).map(MatchResult::value) }
        .map { candidate ->
            if (candidate.startsWith("//")) "https:$candidate" else candidate
        }
        .distinct()
        .toList()
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
