package io.github.falker47.socialviewer.preview

import android.webkit.WebView

/** Reddit page adjustments never choose cookies or remove consent notices. */
internal fun installRedditPageTreatment(view: WebView) {
    if (RedditPagePolicy.canonicalTarget(view.url.orEmpty()) == null) return
    runCatching {
        val script = view.context.assets.open("reddit-skip-link.js").bufferedReader().use { it.readText() }
        view.evaluateJavascript(script, null)
    }
}

internal fun installRedditCookieChoice(view: WebView) {
    if (RedditPagePolicy.canonicalTarget(view.url.orEmpty()) == null) return
    runCatching {
        val script = view.context.assets.open("reddit-consent-choice.js").bufferedReader().use { it.readText() }
        view.evaluateJavascript(script, null)
    }
}
