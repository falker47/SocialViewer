package io.github.falker47.socialviewer.preview

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream

internal enum class RedditPageIssue {
    NETWORK, HTTP, TLS, OUTSIDE_THREAD_LINK, OUTSIDE_THREAD_PAGE, RENDERER, TIMEOUT,
}

/** No content scraping, native JS bridge, UA substitution or external intent launch. */
internal class RedditPageClient(
    private val initialUrl: String,
    private val onLoading: (Boolean) -> Unit,
    private val onIssue: (RedditPageIssue, Int?) -> Unit,
    private val onRendererGone: (WebView) -> Unit,
) : WebViewClient() {
    @Volatile private var active = true

    fun close() { active = false }

    private fun issue(value: RedditPageIssue, code: Int? = null) {
        if (active) onIssue(value, code)
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (!active) return true
        if (!request.isForMainFrame) return false
        val allowed = RedditPagePolicy.allowsNavigation(initialUrl, request.url.toString(), request.method)
        if (!allowed) issue(RedditPageIssue.OUTSIDE_THREAD_LINK)
        return !allowed
    }

    // shouldOverrideUrlLoading does not cover POST requests. Do not admit a top-level
    // form submission or arbitrary destination through that gap. Subresources stay
    // browser/provider-owned: this is not an ad blocker or a universal network sandbox.
    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        if (request.isForMainFrame && !RedditPagePolicy.allowsNavigation(initialUrl, request.url.toString(), request.method)) {
            view.post { issue(RedditPageIssue.OUTSIDE_THREAD_PAGE) }
            return WebResourceResponse("text/plain", "UTF-8", 403, "Outside preview scope", emptyMap(), ByteArrayInputStream(ByteArray(0)))
        }
        return null
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        if (!active) return
        if (!RedditPagePolicy.allowsNavigation(initialUrl, url)) {
            view.stopLoading()
            issue(RedditPageIssue.OUTSIDE_THREAD_PAGE)
        } else {
            onLoading(true)
        }
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
        if (!active || url == "about:blank") return
        // Detect ordinary SPA history navigation without scraping or altering Reddit.
        if (!RedditPagePolicy.allowsNavigation(initialUrl, url)) {
            view.stopLoading()
            issue(RedditPageIssue.OUTSIDE_THREAD_PAGE)
        }
    }

    override fun onPageFinished(view: WebView, url: String) {
        if (active) {
            if (RedditPagePolicy.allowsNavigation(initialUrl, url)) installRedditPageDiagnostics(view)
            onLoading(false)
        }
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (request.isForMainFrame) issue(RedditPageIssue.NETWORK)
    }

    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
        if (request.isForMainFrame) issue(RedditPageIssue.HTTP, errorResponse.statusCode)
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        handler.cancel()
        issue(RedditPageIssue.TLS)
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        onRendererGone(view)
        issue(RedditPageIssue.RENDERER)
        return true
    }
}
