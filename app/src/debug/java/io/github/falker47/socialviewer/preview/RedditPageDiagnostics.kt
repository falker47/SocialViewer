package io.github.falker47.socialviewer.preview

import android.content.ClipData
import android.content.ClipboardManager
import android.webkit.WebView
import android.widget.Toast
import io.github.falker47.socialviewer.BuildConfig
import io.github.falker47.socialviewer.R
import org.json.JSONObject

/** Installed only in the debug preview, on a validated Reddit main document. */
internal fun installRedditPageDiagnostics(view: WebView) {
    if (RedditPagePolicy.canonicalTarget(view.url.orEmpty()) == null) return
    runCatching {
        val script = view.context.assets.open("reddit-diagnostics.js")
            .bufferedReader().use { it.readText() }
        view.evaluateJavascript(script, null)
    }
}

internal fun validateRedditDiagnosticReport(raw: String?): String? = runCatching {
    if (raw == null || raw.length > 48_000) return null
    val json = JSONObject(raw)
    if (json.optString("schema") != "SV_REDDIT_DIAG_V1") return null
    val snapshots = json.optJSONArray("snapshots") ?: return null
    if (snapshots.length() !in 1..10) return null
    json.toString()
}.getOrNull()

/** Explicit user copy only. No upload, persistent report file or JavaScript bridge. */
internal fun copyRedditPageDiagnostics(view: WebView) {
    val context = view.context
    if (RedditPagePolicy.canonicalTarget(view.url.orEmpty()) == null) {
        Toast.makeText(context, R.string.reddit_diagnostics_unavailable, Toast.LENGTH_SHORT).show()
        return
    }
    view.evaluateJavascript("window.__svRedditDiag ? window.__svRedditDiag.report() : null") { raw ->
        if (!view.isAttachedToWindow) return@evaluateJavascript
        val report = validateRedditDiagnosticReport(raw)
        val copied = report != null && runCatching {
            val payload = JSONObject(report)
                .put("appVersion", BuildConfig.VERSION_NAME)
                .put("webViewVersion", WebView.getCurrentWebViewPackage()?.versionName ?: "unknown")
            val clipboard = context.getSystemService(ClipboardManager::class.java)
                ?: error("Clipboard unavailable")
            clipboard.setPrimaryClip(ClipData.newPlainText("SV_REDDIT_DIAG_V1", payload.toString()))
            true
        }.getOrDefault(false)
        Toast.makeText(
            context,
            if (copied) R.string.reddit_diagnostics_copied else R.string.reddit_diagnostics_unavailable,
            Toast.LENGTH_SHORT,
        ).show()
    }
}
