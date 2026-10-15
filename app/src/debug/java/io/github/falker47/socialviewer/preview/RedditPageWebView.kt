package io.github.falker47.socialviewer.preview

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.falker47.socialviewer.R
import kotlinx.coroutines.delay

@SuppressLint("SetJavaScriptEnabled")
internal fun configureRedditPageWebView(view: WebView) {
    if ((view.context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
        WebView.setWebContentsDebuggingEnabled(true)
    }
    view.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        allowFileAccess = false
        allowContentAccess = false
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        mediaPlaybackRequiresUserGesture = true
        safeBrowsingEnabled = true
        javaScriptCanOpenWindowsAutomatically = false
        setSupportMultipleWindows(true)
        useWideViewPort = true
        loadWithOverviewMode = true
        cacheMode = WebSettings.LOAD_NO_CACHE
    }
    CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(view, false)
    }
    view.webChromeClient = object : WebChromeClient() {
        override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean = false
        override fun onPermissionRequest(request: PermissionRequest) { request.deny() }
        override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
            callback.invoke(origin, false, false)
        }
    }
}

@Composable
internal fun RedditPageWebView(url: String, modifier: Modifier, onContentReady: () -> Unit) {
    var attempt by remember { mutableStateOf(0) }
    var loading by remember(url, attempt) { mutableStateOf(true) }
    var issue by remember(url, attempt) { mutableStateOf<RedditPageIssue?>(null) }
    var httpCode by remember(url, attempt) { mutableStateOf(0) }
    val ready = rememberUpdatedState(onContentReady)
    val activeView = remember { arrayOfNulls<WebView>(1) }
    var showCookieChoice by remember(url, attempt) { mutableStateOf(false) }
    var cookieChoiceBusy by remember(url, attempt) { mutableStateOf(false) }
    var cookieChoiceError by remember(url, attempt) { mutableStateOf(false) }

    fun chooseCookieOption(choice: RedditCookieChoice) {
        val view = activeView[0] ?: return
        if (cookieChoiceBusy) return
        cookieChoiceBusy = true
        cookieChoiceError = false
        submitRedditCookieChoice(view, choice) { sent ->
            if (activeView[0] !== view) return@submitRedditCookieChoice
            if (!sent) {
                cookieChoiceBusy = false
                cookieChoiceError = true
                return@submitRedditCookieChoice
            }
            view.postDelayed({
                if (activeView[0] === view && view.isAttachedToWindow) {
                    inspectRedditCookieChoice(view) { stillPending ->
                        if (activeView[0] !== view) return@inspectRedditCookieChoice
                        cookieChoiceBusy = false
                        showCookieChoice = stillPending
                        cookieChoiceError = stillPending
                    }
                }
            }, 1_500L)
        }
    }

    // Unlike a widget, expose the actual page/loading state immediately. A Reddit
    // security/consent/error page must not remain hidden behind the embed overlay.
    LaunchedEffect(url) { ready.value.invoke() }
    // First-visit Reddit cookie dialogs can be invisible in a portrait WebView.
    // Present native choice controls, but never submit anything automatically.
    LaunchedEffect(url, attempt) {
        repeat(45) {
            delay(400L)
            val view = activeView[0]
            if (view != null && view.isAttachedToWindow) {
                inspectRedditCookieChoice(view) { pending ->
                    if (activeView[0] !== view) return@inspectRedditCookieChoice
                    if (pending) showCookieChoice = true
                    else if (!cookieChoiceBusy) {
                        showCookieChoice = false
                        cookieChoiceError = false
                    }
                }
            }
        }
    }
    LaunchedEffect(url, attempt) {
        delay(15_000)
        if (loading && issue == null) {
            issue = RedditPageIssue.TIMEOUT
            loading = false
        }
    }

    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.reddit_page_preview_label), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { activeView[0]?.let(::copyRedditPageDiagnostics) }) {
                Text(stringResource(R.string.reddit_diagnostics_copy))
            }
            IconButton(onClick = { attempt += 1 }) {
                Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.reddit_page_preview_reload))
            }
        }
        if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        issue?.let { value ->
            val message = when (value) {
                RedditPageIssue.HTTP -> stringResource(R.string.reddit_page_preview_http, httpCode)
                RedditPageIssue.TLS -> stringResource(R.string.reddit_page_preview_tls)
                RedditPageIssue.OUTSIDE_THREAD_LINK, RedditPageIssue.OUTSIDE_THREAD_PAGE -> stringResource(R.string.reddit_page_preview_scope)
                RedditPageIssue.TIMEOUT -> stringResource(R.string.reddit_page_preview_timeout)
                RedditPageIssue.RENDERER -> stringResource(R.string.reddit_page_preview_renderer)
                RedditPageIssue.NETWORK -> stringResource(R.string.reddit_page_preview_network)
            }
            Text(message, modifier = Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodySmall)
        }
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            key(url, attempt) {
                val holder = remember { arrayOfNulls<WebView>(1) }
                val clientHolder = remember { arrayOfNulls<RedditPageClient>(1) }
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            holder[0] = this
                            activeView[0] = this
                            configureRedditPageWebView(this)
                            val client = RedditPageClient(
                                initialUrl = url,
                                onLoading = { loading = it },
                                onIssue = { value, code ->
                                    // Keep a rejected navigation covered even if a subsequent
                                    // platform HTTP error callback arrives for that same request.
                                    if (issue != RedditPageIssue.OUTSIDE_THREAD_PAGE && issue != RedditPageIssue.RENDERER) {
                                        issue = value
                                        httpCode = code ?: 0
                                    }
                                    loading = false
                                },
                                onRendererGone = { dead ->
                                    if (holder[0] === dead) {
                                        holder[0] = null
                                        if (activeView[0] === dead) activeView[0] = null
                                        (dead.parent as? ViewGroup)?.removeView(dead)
                                        dead.destroy()
                                    }
                                },
                            )
                            clientHolder[0] = client
                            webViewClient = client
                            setDownloadListener { _, _, _, _, _ -> issue = RedditPageIssue.OUTSIDE_THREAD_LINK }
                            loadUrl(url)
                        }
                    },
                )
                DisposableEffect(Unit) {
                    onDispose {
                        clientHolder[0]?.close()
                        clientHolder[0] = null
                        CookieManager.getInstance().flush()
                        if (activeView[0] === holder[0]) activeView[0] = null
                        holder[0]?.apply {
                            stopLoading()
                            clearHistory()
                            removeAllViews()
                            destroy()
                        }
                        holder[0] = null
                    }
                }
            }
            if (showCookieChoice && issue != RedditPageIssue.OUTSIDE_THREAD_PAGE && issue != RedditPageIssue.RENDERER) {
                RedditCookieConsentOverlay(
                    busy = cookieChoiceBusy,
                    error = cookieChoiceError,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onReject = { chooseCookieOption(RedditCookieChoice.REJECT) },
                    onAccept = { chooseCookieOption(RedditCookieChoice.ACCEPT) },
                )
            }
            if (issue == RedditPageIssue.OUTSIDE_THREAD_PAGE || issue == RedditPageIssue.RENDERER) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
            }
        }
    }
}
