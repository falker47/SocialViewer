package io.github.falker47.socialviewer.provider.tiktok

import android.net.Uri
import io.github.falker47.socialviewer.domain.SocialContent
import io.github.falker47.socialviewer.network.UrlConnectionHttpClient
import io.github.falker47.socialviewer.provider.SocialProvider
import org.json.JSONObject

class TikTokProvider(
    private val http: UrlConnectionHttpClient = UrlConnectionHttpClient(),
    private val firstLoadPlaybackHint: () -> String = {
        "TikTok needs a cookie choice on first use. Complete the cookie prompt in the player, then retry if needed."
    },
) : SocialProvider {
    override val id: String = "tiktok"
    override val displayName: String = "TikTok"

    override fun supports(uri: Uri): Boolean =
        TikTokUrlPolicy.supports(uri.scheme, uri.host)

    override fun resolve(uri: Uri): SocialContent {
        require(supports(uri)) { "URL TikTok non valida" }

        val original = uri.toString()
        val canonical = if (isShortLink(uri)) {
            http.resolveFinalUrl(original)
        } else {
            original
        }
        val canonicalUri = Uri.parse(canonical)
        require(TikTokUrlPolicy.supports(canonicalUri.scheme, canonicalUri.host)) {
            "Il link TikTok ha reindirizzato fuori da TikTok"
        }

        val postId = extractPostId(canonicalUri)
            ?: error("Il link TikTok non contiene un ID di post supportato")

        // Keep oEmbed for public metadata/availability, but render with TikTok's
        // dedicated Embed Player instead of the blockquote + embed.js transformation.
        val oEmbedUrl = Uri.parse("https://www.tiktok.com/oembed")
            .buildUpon()
            .appendQueryParameter("url", canonical)
            .build()
            .toString()

        val response = http.get(oEmbedUrl)
        if (response.statusCode !in 200..299) {
            error("TikTok oEmbed ha risposto HTTP ${response.statusCode}")
        }

        val json = JSONObject(response.body)

        return SocialContent(
            providerId = id,
            providerName = displayName,
            canonicalUrl = canonical,
            title = json.optString("title").takeIf { it.isNotBlank() },
            authorName = json.optString("author_name").takeIf { it.isNotBlank() },
            documentBaseUrl = "https://www.tiktok.com/",
            embedHtml = playerHtml(
                postId = postId,
                firstLoadPlaybackHint = firstLoadPlaybackHint(),
            ),
        )
    }

    private fun isShortLink(uri: Uri): Boolean =
        TikTokUrlPolicy.isShortLink(uri.host, uri.path)

    private fun extractPostId(uri: Uri): String? {
        val segments = uri.pathSegments
        for (index in 0 until segments.lastIndex) {
            if (segments[index] == "video" || segments[index] == "photo") {
                return segments[index + 1].takeIf { id -> id.isNotBlank() && id.all(Char::isDigit) }
            }
        }
        return null
    }

    internal fun playerHtml(
        postId: String,
        firstLoadPlaybackHint: String = "TikTok needs a cookie choice on first use. Complete the cookie prompt in the player, then retry if needed.",
    ): String {
        val quotedFirstLoadPlaybackHint = JSONObject.quote(firstLoadPlaybackHint)

        return """
        <!doctype html>
        <html>
          <head>
            <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1" />
            <style>
              * { box-sizing: border-box; }
              html, body { margin: 0; width: 100%; height: 100%; background: #000; overflow: hidden; }
              #stage { position: fixed; inset: 0; background: #000; }
              #player {
                position: absolute;
                inset: 0;
                width: 100%;
                height: 100%;
                border: 0;
                opacity: 0;
                background: #000;
                transition: opacity 140ms ease-out;
              }
              #loader {
                position: absolute;
                inset: 0;
                z-index: 2;
                display: flex;
                align-items: center;
                justify-content: center;
                background: #000;
                color: #fff;
                font: 14px sans-serif;
              }
              #loader::before {
                content: '';
                width: 26px;
                height: 26px;
                border: 3px solid rgba(255,255,255,.28);
                border-top-color: #fff;
                border-radius: 50%;
                animation: spin .8s linear infinite;
              }
              #player-error {
                position: absolute;
                top: 12px;
                left: 12px;
                right: 12px;
                z-index: 3;
                display: none;
                padding: 10px 12px;
                border-radius: 10px;
                background: rgba(0, 0, 0, .82);
                color: #fff;
                text-align: center;
                font: 13px sans-serif;
                pointer-events: none;
              }
              @keyframes spin { to { transform: rotate(360deg); } }
            </style>
          </head>
          <body>
            <div id="stage">
              <div id="loader" aria-label="Caricamento"></div>
              <div id="player-error" role="status"></div>
              <iframe
                id="player"
                src="https://www.tiktok.com/player/v1/$postId?controls=1&progress_bar=1&play_button=1&volume_control=1&fullscreen_button=1&timestamp=1&autoplay=0&muted=0&rel=0&native_context_menu=0"
                allow="autoplay; encrypted-media; fullscreen"
                allowfullscreen
                title="TikTok player">
              </iframe>
            </div>
            <script>
              (function () {
                const player = document.getElementById('player');
                const loader = document.getElementById('loader');
                const playerError = document.getElementById('player-error');
                const firstLoadPlaybackHint = $quotedFirstLoadPlaybackHint;
                let revealed = false;
                let playerReady = false;
                let waitingForConsent = false;

                function reveal() {
                  playerError.style.display = 'none';
                  waitingForConsent = false;
                  if (revealed) return;
                  revealed = true;
                  player.style.opacity = '1';
                  loader.style.display = 'none';
                }

                player.addEventListener('load', function () {
                  // Avoid exposing the iframe's intermediate layout while TikTok initializes.
                  window.setTimeout(reveal, 250);
                });

                window.addEventListener('message', function (event) {
                  const data = event && event.data;
                  if (!data || !data['x-tiktok-player']) return;

                  if (data.type === 'onPlayerReady') {
                    playerReady = true;
                    reveal();
                    return;
                  }

                  if (data.type === 'onPlayerError') {
                    const value = data.value || {};
                    const errorCode = value.errorCode === undefined ? 'unknown' : value.errorCode;
                    const errorType = value.errorType || 'UNKNOWN';

                    console.warn('TikTok player error', errorCode, errorType);

                    // Physical-device verification showed that TikTok can emit 3001 on
                    // the very first player load while its own cookie-consent surface is
                    // still unresolved. Keep TikTok's iframe fully interactive and give
                    // the user context instead of treating that transient state as a
                    // terminal SocialViewer error.
                    if (!playerReady && errorCode === 3001) {
                      waitingForConsent = true;
                      player.style.opacity = '1';
                      loader.style.display = 'none';
                      playerError.textContent = firstLoadPlaybackHint;
                      playerError.style.display = 'block';
                    }
                  }
                });

                // Safety fallback for unusual WebView/player builds. Do not hide the
                // first-load consent hint while TikTok is waiting for the user's choice.
                window.setTimeout(function () {
                  if (!waitingForConsent) reveal();
                }, 6000);
              })();
            </script>
          </body>
        </html>
    """.trimIndent()
    }
}
