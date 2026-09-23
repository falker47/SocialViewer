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
            embedHtml = playerHtml(postId),
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

    private fun playerHtml(postId: String): String = """
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
                  // Avoid exposing the iframe's intermediate layout while TikTok initializes.
                  window.setTimeout(reveal, 250);
                });

                window.addEventListener('message', function (event) {
                  const data = event && event.data;
                  if (data && data['x-tiktok-player'] && data.type === 'onPlayerReady') {
                    reveal();
                  }
                });

                // Safety fallback for unusual WebView/player builds.
                window.setTimeout(reveal, 6000);
              })();
            </script>
          </body>
        </html>
    """.trimIndent()
}
