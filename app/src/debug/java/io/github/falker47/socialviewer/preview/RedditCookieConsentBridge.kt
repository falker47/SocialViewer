package io.github.falker47.socialviewer.preview

import android.webkit.WebView

internal enum class RedditCookieChoice(val token: String) {
    REJECT("reject"),
    ACCEPT("accept"),
}

internal fun parseRedditCookieConsentState(raw: String?): Boolean = raw == "\"ready\""
internal fun parseRedditCookieConsentSubmission(raw: String?): Boolean = raw == "true"

internal fun inspectRedditCookieChoice(view: WebView, onResult: (Boolean) -> Unit) {
    if (RedditPagePolicy.canonicalTarget(view.url.orEmpty()) == null) {
        onResult(false)
        return
    }
    view.evaluateJavascript("window.__svCookieChoice?.state() || 'absent'") { raw ->
        onResult(parseRedditCookieConsentState(raw))
    }
}

/** The .click() is issued only after an explicit tap on a native choice button. */
internal fun submitRedditCookieChoice(
    view: WebView,
    choice: RedditCookieChoice,
    onResult: (Boolean) -> Unit,
) {
    if (RedditPagePolicy.canonicalTarget(view.url.orEmpty()) == null) {
        onResult(false)
        return
    }
    val script = "window.__svCookieChoice?.choose('" + choice.token + "') === true"
    view.evaluateJavascript(script) { raw ->
        onResult(parseRedditCookieConsentSubmission(raw))
    }
}
