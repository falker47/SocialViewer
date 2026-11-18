package io.github.falker47.socialviewer.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RedditDiscussionLauncherTest {
    private val post = "https://www.reddit.com/r/AskReddit/comments/53scc5/example_post/"
    private val appPackage = "io.github.falker47.socialviewer"
    private val browserPackage = "example.browser"

    @Test
    fun customTabIsExplicitlyPinnedToTheBrowserAndKeepsTheSameItem() {
        val intents = redditDiscussionIntents(post, browserPackage, appPackage)
        assertEquals(2, intents.size)
        val tab = intents.first()
        assertEquals(Intent.ACTION_VIEW, tab.action)
        assertEquals(post, tab.data.toString())
        assertEquals(browserPackage, tab.`package`)
        assertNull(tab.selector)
        assertTrue(tab.hasExtra(CustomTabsIntent.EXTRA_SESSION))
    }

    @Test
    fun fallbackCannotResolveThroughSocialViewerAppLinks() {
        val fallback = redditDiscussionIntents(post, browserPackage, appPackage).last()
        assertEquals(Intent.ACTION_VIEW, fallback.action)
        assertEquals(post, fallback.data.toString())
        assertEquals(Intent.ACTION_MAIN, fallback.selector?.action)
        assertTrue(fallback.selector?.hasCategory(Intent.CATEGORY_APP_BROWSER) == true)
    }

    @Test
    fun absentCustomTabsAndSelfPackageUseOnlyTheBrowserEscape() {
        listOf(null, "", appPackage).forEach { unavailable ->
            val intents = redditDiscussionIntents(post, unavailable, appPackage)
            assertEquals(1, intents.size)
            assertTrue(intents.single().selector?.hasCategory(Intent.CATEGORY_APP_BROWSER) == true)
        }
    }

    @Test
    fun browserSelectionPrefersTheVisibleDefaultAndExcludesTheApp() {
        assertEquals(
            listOf("default.browser", browserPackage),
            discussionBrowserCandidates(
                listOf(browserPackage, appPackage, "default.browser", browserPackage, ""),
                "default.browser",
                appPackage,
            ),
        )
        assertEquals(listOf(browserPackage), discussionBrowserCandidates(listOf(browserPackage), "missing.browser", appPackage))
    }

    @Test
    fun invalidTargetDoesNotCreateOrLaunchAnyIntent() {
        val intents = redditDiscussionIntents("https://evil.example/", browserPackage, appPackage)
        val context = RecordingContext(RuntimeEnvironment.getApplication())
        assertTrue(intents.isEmpty())
        assertFalse(launchDiscussionIntents(context, intents))
        assertTrue(context.started.isEmpty())
    }

    @Test
    fun successfulCustomTabDoesNotAlsoOpenTheFallback() {
        val context = RecordingContext(RuntimeEnvironment.getApplication())
        assertTrue(launchDiscussionIntents(context, redditDiscussionIntents(post, browserPackage, appPackage)))
        assertEquals(1, context.started.size)
        assertEquals(browserPackage, context.started.single().`package`)
    }

    @Test
    fun disappearedCustomTabFallsBackToTheBrowser() {
        val context = RecordingContext(RuntimeEnvironment.getApplication(), failures = 1)
        assertTrue(launchDiscussionIntents(context, redditDiscussionIntents(post, browserPackage, appPackage)))
        assertEquals(2, context.started.size)
        assertTrue(context.started.last().selector?.hasCategory(Intent.CATEGORY_APP_BROWSER) == true)
    }

    @Test
    fun unavailableBrowsersReturnFailureWithoutCrashing() {
        val context = RecordingContext(RuntimeEnvironment.getApplication(), failures = 2)
        assertFalse(launchDiscussionIntents(context, redditDiscussionIntents(post, browserPackage, appPackage)))
        assertEquals(2, context.started.size)
    }

    @Test
    fun browserSecurityFailureAlsoUsesTheBoundedFallback() {
        val context = RecordingContext(RuntimeEnvironment.getApplication(), failures = 1, securityFailure = true)
        assertTrue(launchDiscussionIntents(context, redditDiscussionIntents(post, browserPackage, appPackage)))
        assertTrue(context.started.last().selector?.hasCategory(Intent.CATEGORY_APP_BROWSER) == true)
    }

    private class RecordingContext(
        base: Context,
        private val failures: Int = 0,
        private val securityFailure: Boolean = false,
    ) : ContextWrapper(base) {
        val started = mutableListOf<Intent>()
        override fun startActivity(intent: Intent) {
            started += Intent(intent)
            if (started.size <= failures) {
                if (securityFailure) throw SecurityException("Browser unavailable")
                throw ActivityNotFoundException("Browser unavailable")
            }
        }
    }
}
