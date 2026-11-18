package io.github.falker47.socialviewer.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent

/** Browser handoff only: never load the discussion into the embedded player. */
internal fun openRedditDiscussion(context: Context, url: String): Boolean {
    if (RedditDiscussionPolicy.urlFor("reddit", url) == null) return false
    val customTabsPackage = try {
        val manager = context.packageManager
        val browserSelector = discussionBrowserSelector()
        val visibleBrowsers = manager.queryIntentActivities(browserSelector, 0)
            .mapNotNull { it.activityInfo?.packageName }
        val defaultBrowser = manager.resolveActivity(
            browserSelector,
            PackageManager.MATCH_DEFAULT_ONLY,
        )?.activityInfo?.packageName
        val candidates = discussionBrowserCandidates(
            visibleBrowsers,
            defaultBrowser,
            context.packageName,
        )
        // Do not resolve the Reddit URL itself: SocialViewer may own its App Links.
        CustomTabsClient.getPackageName(context, candidates, true)
            ?.takeIf { it in candidates }
    } catch (_: SecurityException) {
        null
    }
    return launchDiscussionIntents(
        context,
        redditDiscussionIntents(url, customTabsPackage, context.packageName),
    )
}

internal fun discussionBrowserCandidates(
    visibleBrowsers: List<String>,
    defaultBrowser: String?,
    appPackage: String,
): List<String> {
    val eligible = visibleBrowsers.filter { it.isNotBlank() && it != appPackage }.distinct()
    return eligible.sortedBy { it != defaultBrowser }
}

private fun discussionBrowserSelector(): Intent = Intent(Intent.ACTION_MAIN).apply {
    addCategory(Intent.CATEGORY_APP_BROWSER)
}

internal fun redditDiscussionIntents(
    url: String,
    customTabsPackage: String?,
    appPackage: String,
): List<Intent> {
    val target = RedditDiscussionPolicy.urlFor("reddit", url) ?: return emptyList()
    val uri = Uri.parse(target)
    val intents = mutableListOf<Intent>()
    if (!customTabsPackage.isNullOrBlank() && customTabsPackage != appPackage) {
        intents += CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .intent.apply {
                data = uri
                // An explicit browser package prevents the initial App Links loop.
                setPackage(customTabsPackage)
            }
    }
    // No Custom Tabs service, or its activity disappeared: retain the browser-only escape.
    intents += Intent(Intent.ACTION_VIEW, uri).apply {
        selector = discussionBrowserSelector()
    }
    return intents
}

internal fun launchDiscussionIntents(context: Context, intents: List<Intent>): Boolean {
    for (candidate in intents) {
        try {
            val intent = Intent(candidate)
            if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return true
        } catch (_: ActivityNotFoundException) {
            // Try the bounded browser fallback; never retry with an unrestricted VIEW.
        } catch (_: SecurityException) {
            // A browser can become unavailable between discovery and launch.
        }
    }
    return false
}
