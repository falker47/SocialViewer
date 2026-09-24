package io.github.falker47.socialviewer.provider.youtube

import android.net.Uri
import io.github.falker47.socialviewer.domain.SocialContent
import io.github.falker47.socialviewer.network.UrlConnectionHttpClient
import io.github.falker47.socialviewer.provider.ProviderConfigurationException
import io.github.falker47.socialviewer.provider.ProviderContentUnavailableException
import io.github.falker47.socialviewer.provider.ProviderPolicyBlockedException
import io.github.falker47.socialviewer.provider.SocialProvider
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val YOUTUBE_DATA_API = "https://www.googleapis.com/youtube/v3/videos"
private const val YOUTUBE_DOCUMENT_BASE_URL = "https://io.github.falker47.socialviewer/"

class YouTubeProvider(
    private val http: UrlConnectionHttpClient = UrlConnectionHttpClient(),
    private val apiKey: String,
    private val apiClientHeaders: Map<String, String> = emptyMap(),
) : SocialProvider {
    override val id: String = "youtube"
    override val displayName: String = "YouTube"

    override fun supports(uri: Uri): Boolean =
        YouTubeUrlPolicy.supports(
            scheme = uri.scheme,
            host = uri.host,
            path = uri.path,
            encodedQuery = uri.encodedQuery,
        )

    override fun resolve(uri: Uri): SocialContent {
        val videoId = YouTubeUrlPolicy.videoId(
            scheme = uri.scheme,
            host = uri.host,
            path = uri.path,
            encodedQuery = uri.encodedQuery,
        ) ?: error("URL YouTube non supportata")

        return resolveYouTubeVideo(
            videoId = videoId,
            http = http,
            apiKey = apiKey,
            apiClientHeaders = apiClientHeaders,
        )
    }
}

internal fun resolveYouTubeVideo(
    videoId: String,
    http: UrlConnectionHttpClient,
    apiKey: String,
    apiClientHeaders: Map<String, String> = emptyMap(),
): SocialContent {
    if (apiKey.isBlank()) {
        throw ProviderConfigurationException(
            providerName = "YouTube",
            userMessage = "YouTube non è configurato in questa build.",
            technicalDetail = "YOUTUBE_API_KEY non configurata",
        )
    }

    val encodedVideoId = URLEncoder.encode(videoId, StandardCharsets.UTF_8.name())
    val encodedApiKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8.name())
    val requestUrl =
        YOUTUBE_DATA_API + "?part=snippet,status&id=" + encodedVideoId + "&key=" + encodedApiKey

    val response = http.get(requestUrl, apiClientHeaders)

    if (response.statusCode !in 200..299) {
        error("YouTube Data API ha risposto HTTP " + response.statusCode)
    }

    val responseHost = runCatching {
        URI(response.finalUrl).host?.lowercase()
    }.getOrNull()
    require(responseHost == "www.googleapis.com") {
        "YouTube Data API ha reindirizzato fuori da Google APIs"
    }

    val json = JSONObject(response.body)
    val items = json.optJSONArray("items")
    if (items == null || items.length() == 0) {
        throw ProviderContentUnavailableException(
            providerName = "YouTube",
            technicalDetail = "YouTube Data API non ha restituito il video",
        )
    }

    val item = items.getJSONObject(0)
    val status = item.optJSONObject("status")
        ?: error("YouTube Data API non ha restituito lo status del video")

    val privacyStatus = status.optString("privacyStatus")
    if (privacyStatus != "public") {
        throw ProviderContentUnavailableException(
            providerName = "YouTube",
            technicalDetail = "Video YouTube non pubblico: " + privacyStatus,
        )
    }

    if (!status.optBoolean("embeddable", false)) {
        throw ProviderContentUnavailableException(
            providerName = "YouTube",
            technicalDetail = "Embedding YouTube disabilitato per il video",
        )
    }

    if (!status.has("madeForKids")) {
        error("YouTube Data API non ha restituito status.madeForKids")
    }

    if (status.getBoolean("madeForKids")) {
        throw ProviderPolicyBlockedException(
            providerName = "YouTube",
            userMessage = "I video YouTube destinati ai bambini non vengono aperti in Social Viewer.",
            technicalDetail = "Video YouTube Made For Kids bloccato prima dell'embed",
        )
    }

    val snippet = item.optJSONObject("snippet")
    val canonicalUrl = YouTubeUrlPolicy.canonicalWatchUrl(videoId)

    return SocialContent(
        providerId = "youtube",
        providerName = "YouTube",
        canonicalUrl = canonicalUrl,
        title = snippet?.optString("title")?.takeIf { it.isNotBlank() },
        authorName = snippet?.optString("channelTitle")?.takeIf { it.isNotBlank() },
        documentBaseUrl = YOUTUBE_DOCUMENT_BASE_URL,
        embedHtml = youtubeDocument(videoId),
    )
}

internal fun youtubeDocument(videoId: String): String = """
    <!doctype html>
    <html>
      <head>
        <meta
          name="viewport"
          content="width=device-width, initial-scale=1, maximum-scale=1"
        />
        <meta name="referrer" content="strict-origin-when-cross-origin" />
        <style>
          * { box-sizing: border-box; }
          html, body {
            margin: 0;
            width: 100%;
            height: 100%;
            background: #000;
            overflow: hidden;
          }
          #stage {
            position: fixed;
            inset: 0;
            display: flex;
            align-items: center;
            justify-content: center;
            background: #000;
          }
          #player {
            width: 100%;
            height: 100%;
            border: 0;
            background: #000;
          }
        </style>
      </head>
      <body>
        <div id="stage">
          <iframe
            id="player"
            src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=0&controls=1&playsinline=1&rel=0"
            title="YouTube player"
            referrerpolicy="strict-origin-when-cross-origin"
            allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture; web-share; fullscreen"
            allowfullscreen>
          </iframe>
        </div>
      </body>
    </html>
""".trimIndent()
