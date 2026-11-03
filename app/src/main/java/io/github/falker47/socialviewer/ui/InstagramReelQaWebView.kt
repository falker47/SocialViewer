package io.github.falker47.socialviewer.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.falker47.socialviewer.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.github.falker47.socialviewer.provider.instagram.InstagramUrlPolicy

/**
 * Link-QA-only experiment. Opens the provider's own mobile Reel page as a
 * first-party document without scraping/rehosting or entering general browsing.
 * The normal production Instagram path remains the official oEmbed renderer.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun InstagramReelQaWebView(
    reelWebUrl: String,
    modifier: Modifier = Modifier,
    onContentReady: () -> Unit = {},
) {
    val context = LocalContext.current
    val webViewHolder = remember { arrayOfNulls<WebView>(1) }
    val fullscreenController = remember(context) { EmbedFullscreenController(context) }
    val currentOnContentReady = rememberUpdatedState(onContentReady)
    var navigationBlocked by remember(reelWebUrl) { mutableStateOf(false) }

    // A history notification may report SPA URL changes missed by main-frame
    // interception. This is a fail-closed QA observation, not a guarantee that
    // a provider page cannot change content without changing its URL.
    fun failClosed(view: WebView) {
        if (view.tag != reelWebUrl || navigationBlocked) return
        view.tag = null
        view.stopLoading()
        navigationBlocked = true
        currentOnContentReady.value.invoke()
    }

    if (navigationBlocked) {
        Box(
            modifier = modifier.background(ComposeColor.Black),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.instagram_reel_qa_navigation_blocked),
                color = ComposeColor.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp),
            )
        }
        return
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            WebView(viewContext).apply {
                webViewHolder[0] = this
                tag = reelWebUrl
                setBackgroundColor(Color.BLACK)
                webChromeClient = fullscreenController

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.mediaPlaybackRequiresUserGesture = true
                settings.setSupportMultipleWindows(false)
                settings.javaScriptCanOpenWindowsAutomatically = false

                val currentWebView = this
                CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    setAcceptThirdPartyCookies(currentWebView, false)
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean =
                        request.isForMainFrame &&
                            !InstagramUrlPolicy.allowsInAppReelNavigation(
                                reelWebUrl,
                                request.url.toString(),
                            )

                    override fun doUpdateVisitedHistory(
                        view: WebView,
                        url: String,
                        isReload: Boolean,
                    ) {
                        super.doUpdateVisitedHistory(view, url, isReload)
                        if (!InstagramUrlPolicy.allowsInAppReelNavigation(reelWebUrl, url)) {
                            failClosed(view)
                        }
                    }

                    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                        if (!InstagramUrlPolicy.allowsInAppReelNavigation(reelWebUrl, url)) {
                            failClosed(view)
                        }
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        if (view.tag != reelWebUrl) return
                        if (!InstagramUrlPolicy.allowsInAppReelNavigation(reelWebUrl, url)) {
                            failClosed(view)
                            return
                        }
                        currentOnContentReady.value.invoke()
                    }
                }

                // Direct first-party request, unlike the oEmbed HTML rendered
                // through loadDataWithBaseURL. No injection or media extraction.
                loadUrl(reelWebUrl)

                // Loading completion is not playback success. Avoid a permanent
                // native overlay if Instagram's page stalls on Android WebView.
                postDelayed(
                    {
                        if (tag == reelWebUrl) currentOnContentReady.value.invoke()
                    },
                    13_000L,
                )
            }
        },
        update = { view ->
            if (view.tag != reelWebUrl) {
                view.tag = reelWebUrl
                view.loadUrl(reelWebUrl)
            }
        },
    )

    DisposableEffect(fullscreenController) {
        onDispose {
            fullscreenController.close()
            CookieManager.getInstance().flush()
            webViewHolder[0]?.apply {
                tag = null
                stopLoading()
                clearHistory()
                loadUrl("about:blank")
                removeAllViews()
                destroy()
            }
            webViewHolder[0] = null
        }
    }
}
