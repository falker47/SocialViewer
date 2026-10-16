package io.github.falker47.socialviewer.preview

import android.net.Uri
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import io.github.falker47.socialviewer.network.UrlConnectionHttpClient
import io.github.falker47.socialviewer.provider.reddit.RedditProvider
import java.io.ByteArrayInputStream
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RedditPageClientTest {
    private val post = "https://www.reddit.com/r/AskReddit/comments/53scc5/what_video_game/"
    private val issues = mutableListOf<Pair<RedditPageIssue, Int?>>()
    private val loading = mutableListOf<Boolean>()
    private var lostView: WebView? = null
    private fun view() = WebView(RuntimeEnvironment.getApplication())
    private fun client() = RedditPageClient(post, { loading += it }, { value, code -> issues += value to code }, { lostView = it })

    @Test fun resolvesTheCanonicalPageWithoutMakingAnOEmbedRequest() {
        val http = object : UrlConnectionHttpClient() {
            override fun get(url: String): UrlConnectionHttpClient.Response = error("Preview must not fetch oEmbed")
            override fun resolveFinalUrl(url: String): String = error("Canonical input must not need redirects")
        }
        val content = RedditProvider(http).resolve(Uri.parse(post))
        assertEquals("reddit", content.providerId)
        assertEquals(post, content.canonicalUrl)
        assertEquals(post, content.documentBaseUrl)
        assertEquals("", content.embedHtml)
    }

    @Test fun existingShareResolverStillFeedsTheSameSinglePagePreview() {
        var resolved: String? = null
        val http = object : UrlConnectionHttpClient() {
            override fun get(url: String): UrlConnectionHttpClient.Response = error("Preview must not fetch oEmbed")
            override fun resolveFinalUrl(url: String): String { resolved = url; return post }
        }
        val alias = "https://www.reddit.com/r/AskReddit/s/Test123"
        val content = RedditProvider(http).resolve(Uri.parse(alias))
        assertEquals(alias, resolved)
        assertEquals(post, content.documentBaseUrl)
        assertTrue(content.embedHtml.isEmpty())
    }

    @Test fun permitsReplyNavigationButNeverLaunchesAnExternalHandler() {
        val client = client()
        val view = view()
        assertFalse(client.shouldOverrideUrlLoading(view, request(post + "reply123/")))
        assertTrue(client.shouldOverrideUrlLoading(view, request("intent://reddit")))
        assertEquals(RedditPageIssue.OUTSIDE_THREAD_LINK, issues.last().first)
        assertTrue(client.shouldOverrideUrlLoading(view, request("https://www.reddit.com/r/AskReddit/")))
    }

    @Test fun interceptsMainFramePostAndOutOfScopeLoadsButLeavesSubresourcesAlone() {
        val client = client()
        val view = view()
        assertNotNull(client.shouldInterceptRequest(view, request(post, "POST")))
        assertNotNull(client.shouldInterceptRequest(view, request("https://evil.example/")))
        assertNull(client.shouldInterceptRequest(view, request(post)))
        assertNull(client.shouldInterceptRequest(view, request("https://www.redditstatic.com/image.png", main = false)))
    }

    @Test fun reportsProviderHttpFailureWithoutCallingItAnEmulatorFailure() {
        val client = client()
        val response = WebResourceResponse("text/plain", "UTF-8", 403, "Forbidden", emptyMap(), ByteArrayInputStream(ByteArray(0)))
        client.onReceivedHttpError(view(), request(post), response)
        assertEquals(listOf(RedditPageIssue.HTTP to 403), issues)
        client.onReceivedHttpError(view(), request(post, main = false), response)
        assertEquals(1, issues.size)
    }

    @Test fun detectsOrdinarySpaHistoryEscapesAndSignalsLoadingCompletion() {
        val client = client()
        val view = view()
        client.onPageStarted(view, post, null)
        client.onPageFinished(view, post)
        assertEquals(listOf(true, false), loading)
        client.doUpdateVisitedHistory(view, "https://www.reddit.com/r/popular/", false)
        assertEquals(RedditPageIssue.OUTSIDE_THREAD_PAGE, issues.single().first)
    }

    @Test fun closedClientIgnoresLateStateCallbacks() {
        val client = client()
        client.close()
        client.onPageStarted(view(), post, null)
        client.onPageFinished(view(), post)
        client.doUpdateVisitedHistory(view(), "https://www.reddit.com/", false)
        assertTrue(issues.isEmpty())
        assertTrue(loading.isEmpty())
    }

    @Test fun rendererDeathIsHandledAndTheSpecificViewIsPassedForCleanup() {
        val client = client()
        val view = view()
        val detail = object : RenderProcessGoneDetail() {
            override fun didCrash(): Boolean = true
            override fun rendererPriorityAtExit(): Int = WebView.RENDERER_PRIORITY_IMPORTANT
        }
        assertTrue(client.onRenderProcessGone(view, detail))
        assertSame(view, lostView)
        assertEquals(RedditPageIssue.RENDERER, issues.single().first)
    }

    @Test fun webViewSettingsKeepFileAndThirdPartyCookieProtections() {
        val view = view()
        configureRedditPageWebView(view)
        assertTrue(view.settings.javaScriptEnabled)
        assertTrue(view.settings.domStorageEnabled)
        assertFalse(view.settings.allowFileAccess)
        assertFalse(view.settings.allowContentAccess)
        assertEquals(WebSettings.MIXED_CONTENT_NEVER_ALLOW, view.settings.mixedContentMode)
        assertEquals(WebSettings.LOAD_NO_CACHE, view.settings.cacheMode)
        assertFalse(CookieManager.getInstance().acceptThirdPartyCookies(view))
        assertFalse(view.settings.javaScriptCanOpenWindowsAutomatically)
    }

    private fun request(value: String, method: String = "GET", main: Boolean = true): WebResourceRequest = object : WebResourceRequest {
        override fun getUrl(): Uri = Uri.parse(value)
        override fun isForMainFrame(): Boolean = main
        override fun isRedirect(): Boolean = false
        override fun hasGesture(): Boolean = true
        override fun getMethod(): String = method
        override fun getRequestHeaders(): Map<String, String> = emptyMap()
    }
}
