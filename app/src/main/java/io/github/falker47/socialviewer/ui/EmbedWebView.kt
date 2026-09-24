package io.github.falker47.socialviewer.ui

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EmbedWebView(
    html: String,
    baseUrl: String,
    modifier: Modifier = Modifier,
) {
    var webView: WebView? = null

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                webView = this
                setBackgroundColor(android.graphics.Color.BLACK)

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.mediaPlaybackRequiresUserGesture = true
                settings.setSupportMultipleWindows(false)
                settings.cacheMode = WebSettings.LOAD_DEFAULT

                val currentWebView = this
                CookieManager.getInstance().apply {
                    // Provider consent choices are allowed to persist locally so the user is
                    // not asked again for every single shared item.
                    setAcceptCookie(true)
                    setAcceptThirdPartyCookies(currentWebView, false)
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean {
                        // Keep the embed as a player, not as a gateway into the social network.
                        return request.isForMainFrame
                    }
                }
            }
        },
        update = { view ->
            val documentKey = 31 * baseUrl.hashCode() + html.hashCode()
            if (view.tag != documentKey) {
                view.tag = documentKey
                view.loadDataWithBaseURL(
                    baseUrl,
                    html,
                    "text/html",
                    "UTF-8",
                    null,
                )
            }
        },
    )

    DisposableEffect(Unit) {
        onDispose {
            // Persist provider preferences (including cookie consent) but discard the
            // transient browsing surface itself. We intentionally do not clear cookies,
            // WebStorage, or cache here; clearing them caused TikTok's consent banner to
            // reappear on every item.
            CookieManager.getInstance().flush()
            webView?.apply {
                stopLoading()
                clearHistory()
                loadUrl("about:blank")
                removeAllViews()
                destroy()
            }
            webView = null
        }
    }
}
