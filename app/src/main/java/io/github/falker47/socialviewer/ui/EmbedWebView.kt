package io.github.falker47.socialviewer.ui

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private const val EMBED_REVEAL_DELAY_MS = 220L
private const val EMBED_REVEAL_FALLBACK_MS = 13_000L

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EmbedWebView(
    html: String,
    baseUrl: String,
    modifier: Modifier = Modifier,
    onContentReady: () -> Unit = {},
) {
    var webView: WebView? = null
    val currentOnContentReady = rememberUpdatedState(onContentReady)

    fun revealIfCurrent(view: WebView, documentKey: Int) {
        if (view.tag != documentKey || view.alpha != 0f) return

        view.alpha = 1f
        view.isEnabled = true
        currentOnContentReady.value.invoke()
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                webView = this
                alpha = 0f
                isEnabled = false
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

                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        val documentKey = view.tag as? Int ?: return

                        // onPageFinished means the provider document completed, but oEmbed/widget
                        // scripts can still be committing their final transformed frame. Wait for a
                        // visual-state callback plus a short quiet window before exposing the WebView.
                        view.postVisualStateCallback(
                            documentKey.toLong(),
                            object : WebView.VisualStateCallback() {
                                override fun onComplete(requestId: Long) {
                                    view.postDelayed(
                                        { revealIfCurrent(view, documentKey) },
                                        EMBED_REVEAL_DELAY_MS,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        },
        update = { view ->
            val documentKey = 31 * baseUrl.hashCode() + html.hashCode()
            if (view.tag != documentKey) {
                view.tag = documentKey
                view.alpha = 0f
                view.isEnabled = false
                view.loadDataWithBaseURL(
                    baseUrl,
                    html,
                    "text/html",
                    "UTF-8",
                    null,
                )

                // Safety fallback: a provider-side script failure must not leave the native
                // loading overlay permanently stuck. Provider HTML can then surface its own
                // bounded error/status UI.
                view.postDelayed(
                    { revealIfCurrent(view, documentKey) },
                    EMBED_REVEAL_FALLBACK_MS,
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
                tag = null
                alpha = 0f
                isEnabled = false
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
