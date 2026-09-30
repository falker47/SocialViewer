package io.github.falker47.socialviewer.provider.tiktok

import android.net.Uri
import io.github.falker47.socialviewer.domain.SocialContent
import io.github.falker47.socialviewer.network.UrlConnectionHttpClient
import io.github.falker47.socialviewer.provider.SocialProvider
import org.json.JSONObject

class TikTokProvider(
    private val http: UrlConnectionHttpClient = UrlConnectionHttpClient(),
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
        val isCanonicalPhoto = canonicalUri.pathSegments.let { segments ->
            segments.size == 3 && segments[0].startsWith("@") &&
                segments[1] == "photo" && segments[2] == postId
        }

        // Keep oEmbed metadata where supported, but render with TikTok's official
        // Embed Player, which supports both video and image posts.
        val oEmbedUrl = Uri.parse("https://www.tiktok.com/oembed")
            .buildUpon()
            .appendQueryParameter("url", canonical)
            .build()
            .toString()

        val response = http.get(oEmbedUrl)
        val json = when {
            response.statusCode in 200..299 -> JSONObject(response.body)
            // The reported vm link resolves successfully to a /@user/photo/id URL,
            // but oEmbed returns 400 for that image post. Missing metadata must not
            // prevent the official image player from handling this supported format.
            // Do not generalize this to video, access, rate-limit or server errors.
            isCanonicalPhoto && response.statusCode == 400 -> null
            else -> error("TikTok oEmbed ha risposto HTTP ${response.statusCode}")
        }

        return SocialContent(
            providerId = id,
            providerName = displayName,
            canonicalUrl = canonical,
            title = json?.optString("title")?.takeIf { it.isNotBlank() },
            authorName = json?.optString("author_name")?.takeIf { it.isNotBlank() },
            documentBaseUrl = "https://www.tiktok.com/",
            embedHtml = playerHtml(postId),
            // TikTok's first WebView load can enter PLAYBACK_ERROR 3001 while its own
            // first-party cookie-consent choice is unresolved. Once that choice creates
            // TikTok's consent cookie, the shared WebView automatically reloads this
            // document exactly once so the same player initializes with the saved choice.
            reloadOnCookieName = "cookie-consent",
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

    internal fun playerHtml(postId: String): String = """
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
              @keyframes spin { to { transform: rotate(360deg); } }
            </style>
          </head>
          <body>
            <div id="stage">
              <div id="loader" aria-label="Caricamento"></div>
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
                let revealed = false;

                function reveal() {
                  if (revealed) return;
                  revealed = true;
                  player.style.opacity = '1';
                  loader.style.display = 'none';
                }

                player.addEventListener('load', function () {
                  window.setTimeout(reveal, 250);
                });

                window.addEventListener('message', function (event) {
                  const data = event && event.data;
                  if (!data || !data['x-tiktok-player']) return;

                  if (data.type === 'onPlayerReady') {
                    reveal();
                    return;
                  }

                  if (data.type === 'onPlayerError') {
                    const value = data.value || {};
                    console.warn(
                      'TikTok player error',
                      value.errorCode === undefined ? 'unknown' : value.errorCode,
                      value.errorType || 'UNKNOWN'
                    );
                    // Keep TikTok's own cookie-consent UI visible and interactive.
                    // Native WebView cookie monitoring handles the one-time reload after
                    // the user's consent choice is persisted.
                    reveal();
                  }
                });

                window.setTimeout(reveal, 6000);
              })();
            </script>
          </body>
        </html>
    """.trimIndent()
}
