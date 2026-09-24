package io.github.falker47.socialviewer.provider.facebook

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import io.github.falker47.socialviewer.provider.ProviderShareLinkResolutionException
import org.json.JSONArray
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

private const val DEFAULT_RESOLVE_TIMEOUT_MS = 12_000L
private const val FACEBOOK_RESOLVER_PROFILE = "social_viewer_facebook_resolver"

private val FACEBOOK_RESOLVER_HOSTS = setOf(
    "facebook.com",
    "www.facebook.com",
    "m.facebook.com",
    "web.facebook.com",
)

internal fun interface FacebookShareLinkResolver {
    fun resolve(alias: FacebookShareAlias): String
}

/**
 * Resolves only Facebook-generated share aliases into canonical Facebook URLs.
 *
 * This WebView is never exposed as a browser surface. It observes main-frame navigation and reads
 * only URL identity metadata (canonical / og:url), then destroys itself. It does not extract post
 * text, media, comments, or other content.
 */
internal class FacebookBrowserShareLinkResolver(
    context: Context,
    private val timeoutMs: Long = DEFAULT_RESOLVE_TIMEOUT_MS,
) : FacebookShareLinkResolver {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun resolve(alias: FacebookShareAlias): String {
        check(Looper.myLooper() != Looper.getMainLooper()) {
            "Facebook share-link resolution must not block the main thread"
        }

        val latch = CountDownLatch(1)
        val resolved = AtomicReference<String?>(null)
        val failure = AtomicReference<Throwable?>(null)

        mainHandler.post {
            try {
                resolveOnMain(
                    alias = alias,
                    latch = latch,
                    resolved = resolved,
                    failure = failure,
                )
            } catch (t: Throwable) {
                failure.set(t)
                latch.countDown()
            }
        }

        val completed = latch.await(timeoutMs + 5_000L, TimeUnit.MILLISECONDS)
        if (!completed) {
            throw unresolved(
                alias,
                "Timeout durante la risoluzione browser del link Facebook",
            )
        }

        failure.get()?.let { throw it }
        return resolved.get() ?: throw unresolved(
            alias,
            "Facebook non ha esposto un permalink canonico tramite il browser locale",
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun resolveOnMain(
        alias: FacebookShareAlias,
        latch: CountDownLatch,
        resolved: AtomicReference<String?>,
        failure: AtomicReference<Throwable?>,
    ) {
        val finished = AtomicBoolean(false)
        var webView: WebView? = null
        lateinit var timeout: Runnable
        var isolatedProfileActive = false

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
            failure.set(
                unresolved(
                    alias,
                    "Il WebView di sistema non supporta l'isolamento richiesto dal resolver Facebook",
                ),
            )
            latch.countDown()
            return
        }

        val profileStore = ProfileStore.getInstance()
        runCatching {
            // A process crash could leave the named profile on disk. Remove it before reuse so
            // every resolution begins from a fresh browser session.
            profileStore.deleteProfile(FACEBOOK_RESOLVER_PROFILE)
        }.onFailure {
            failure.set(
                unresolved(
                    alias,
                    "Impossibile inizializzare un profilo browser effimero per Facebook",
                ),
            )
            latch.countDown()
            return
        }

        fun completeAfterProfileCleanup(
            value: String?,
            error: Throwable?,
            attempt: Int = 0,
        ) {
            if (!isolatedProfileActive) {
                if (error != null) failure.set(error) else resolved.set(value)
                latch.countDown()
                return
            }

            val cleanup = runCatching {
                profileStore.deleteProfile(FACEBOOK_RESOLVER_PROFILE)
            }
            if (cleanup.isFailure && attempt < 3) {
                mainHandler.postDelayed(
                    { completeAfterProfileCleanup(value, error, attempt + 1) },
                    50L,
                )
                return
            }

            if (cleanup.isFailure) {
                failure.set(
                    unresolved(
                        alias,
                        "Impossibile eliminare lo stato effimero del resolver Facebook",
                    ),
                )
            } else if (error != null) {
                failure.set(error)
            } else {
                resolved.set(value)
            }
            latch.countDown()
        }

        fun finish(value: String? = null, error: Throwable? = null) {
            if (!finished.compareAndSet(false, true)) return

            mainHandler.removeCallbacks(timeout)
            webView?.apply {
                stopLoading()
                clearHistory()
                removeAllViews()
                destroy()
            }
            webView = null

            // Deleting the WebView releases the profile association. Retry briefly because some
            // WebView builds detach that association on the next UI loop.
            mainHandler.post {
                completeAfterProfileCleanup(value, error)
            }
        }

        fun fail(detail: String) {
            finish(error = unresolved(alias, detail))
        }

        fun inspectNavigation(rawUrl: String): Boolean {
            if (!isAllowedFacebookResolverMainFrame(rawUrl)) {
                fail("Facebook ha tentato una navigazione fuori dagli host consentiti")
                return true
            }

            val candidate = selectFacebookResolverCanonicalUrl(
                candidates = listOf(rawUrl),
                expectedKind = alias.expectedKind,
            )
            if (candidate != null) {
                finish(value = candidate)
                return true
            }

            val uri = runCatching { URI(rawUrl) }.getOrNull()
            val unexpectedKind = uri?.let {
                FacebookUrlPolicy.canonicalTarget(
                    scheme = it.scheme,
                    host = it.host,
                    path = it.path,
                )
            }
            if (unexpectedKind != null && unexpectedKind.kind != alias.expectedKind) {
                fail("Il link Facebook condiviso ha risolto verso un tipo di contenuto inatteso")
                return true
            }

            return false
        }

        fun inspectDocument() {
            if (finished.get()) return
            val current = webView ?: return
            current.evaluateJavascript(FACEBOOK_IDENTITY_METADATA_SCRIPT) { rawResult ->
                if (finished.get()) return@evaluateJavascript
                val candidate = selectFacebookResolverCanonicalUrl(
                    candidates = parseFacebookResolverMetadata(rawResult),
                    expectedKind = alias.expectedKind,
                )
                if (candidate != null) {
                    finish(value = candidate)
                }
            }
        }

        val currentWebView = WebView(appContext).also { view ->
            WebViewCompat.setProfile(view, FACEBOOK_RESOLVER_PROFILE)
            isolatedProfileActive = true
        }.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.mediaPlaybackRequiresUserGesture = true
            settings.setSupportMultipleWindows(false)
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.safeBrowsingEnabled = true
            settings.setGeolocationEnabled(false)

            val resolverCookieManager = WebViewCompat.getProfile(this).cookieManager
            resolverCookieManager.setAcceptCookie(true)
            resolverCookieManager.setAcceptThirdPartyCookies(this, false)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    if (!request.isForMainFrame) return false
                    return inspectNavigation(request.url.toString())
                }

                override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                    if (finished.get()) return
                    inspectNavigation(url)
                }

                override fun onPageCommitVisible(view: WebView, url: String) {
                    if (finished.get()) return
                    if (!inspectNavigation(url)) {
                        inspectDocument()
                    }
                }

                override fun onPageFinished(view: WebView, url: String) {
                    if (finished.get()) return
                    if (inspectNavigation(url)) return

                    inspectDocument()
                    mainHandler.postDelayed({ inspectDocument() }, 350L)
                    mainHandler.postDelayed({ inspectDocument() }, 1_000L)
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    if (request.isForMainFrame && !finished.get()) {
                        fail("Errore di navigazione durante la risoluzione browser del link Facebook")
                    }
                }

                override fun onRenderProcessGone(
                    view: WebView,
                    detail: RenderProcessGoneDetail,
                ): Boolean {
                    if (!finished.get()) {
                        fail("Il processo WebView è terminato durante la risoluzione del link Facebook")
                    }
                    return true
                }
            }
        }
        webView = currentWebView

        timeout = Runnable {
            fail("Facebook non ha esposto un permalink canonico entro il timeout del resolver")
        }
        mainHandler.postDelayed(timeout, timeoutMs)
        currentWebView.loadUrl(alias.aliasUrl)
    }
}

internal fun isAllowedFacebookResolverMainFrame(rawUrl: String): Boolean {
    val uri = runCatching { URI(rawUrl) }.getOrNull() ?: return false
    return uri.scheme.equals("https", ignoreCase = true) &&
        uri.host?.lowercase() in FACEBOOK_RESOLVER_HOSTS
}

internal fun selectFacebookResolverCanonicalUrl(
    candidates: List<String>,
    expectedKind: FacebookContentKind,
): String? =
    candidates.asSequence()
        .mapNotNull { raw ->
            val uri = runCatching { URI(raw) }.getOrNull() ?: return@mapNotNull null
            FacebookUrlPolicy.canonicalTarget(
                scheme = uri.scheme,
                host = uri.host,
                path = uri.path,
            )
        }
        .firstOrNull { target -> target.kind == expectedKind }
        ?.canonicalUrl

internal fun parseFacebookResolverMetadata(rawResult: String?): List<String> {
    if (rawResult.isNullOrBlank() || rawResult == "null") return emptyList()

    return runCatching {
        val array = JSONArray(rawResult)
        buildList {
            for (index in 0 until array.length()) {
                array.optString(index)
                    .takeIf { it.isNotBlank() }
                    ?.let(::add)
            }
        }
    }.getOrDefault(emptyList())
}

private fun unresolved(
    alias: FacebookShareAlias,
    detail: String,
): ProviderShareLinkResolutionException =
    ProviderShareLinkResolutionException(
        providerName = "Facebook",
        canonicalHint = canonicalHintFor(alias),
        technicalDetail = detail,
    )

private const val FACEBOOK_IDENTITY_METADATA_SCRIPT = """
(function() {
  var canonical = document.querySelector('link[rel~="canonical"]');
  var ogUrl = document.querySelector('meta[property="og:url"]');
  return [
    window.location.href,
    canonical ? canonical.href : null,
    ogUrl ? ogUrl.content : null
  ].filter(function(value) {
    return typeof value === 'string' && value.length > 0;
  });
})()
"""
