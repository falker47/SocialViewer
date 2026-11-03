package io.github.falker47.socialviewer.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class InstagramReelQaBoundaryTest {
    @Test
    fun fullInstagramPageIsOnlyUsedByIsolatedLinkQaAndNeverGeneralProviders() {
        val shell = readSource("src/main/java/io/github/falker47/socialviewer/ui/SocialViewerApp.kt")
        val page = readSource("src/main/java/io/github/falker47/socialviewer/ui/InstagramReelQaWebView.kt")

        assertTrue(shell.contains("BuildConfig.APPLICATION_ID.endsWith(\".linkqa\")"))
        assertTrue(shell.contains("content.providerId == \"instagram\""))
        assertTrue(shell.contains("InstagramUrlPolicy.mobileWebReelUrl(content.canonicalUrl)"))
        assertTrue(shell.contains("if (firstPartyReelQaUrl != null)"))

        assertTrue(page.contains("request.isForMainFrame &&"))
        assertTrue(page.contains("InstagramUrlPolicy.allowsInAppReelNavigation("))
        assertTrue(page.contains("setAcceptThirdPartyCookies(currentWebView, false)"))
        assertTrue(page.contains("settings.setSupportMultipleWindows(false)"))
        assertTrue(page.contains("settings.mediaPlaybackRequiresUserGesture = true"))
    }

    @Test
    fun spaHistoryChangesFailClosedRatherThanDisplayingAnotherReel() {
        val page = readSource("src/main/java/io/github/falker47/socialviewer/ui/InstagramReelQaWebView.kt")

        // This callback supplements, but cannot replace, a physical SPA audit.
        assertTrue(page.contains("override fun doUpdateVisitedHistory("))
        assertTrue(page.contains("!InstagramUrlPolicy.allowsInAppReelNavigation(reelWebUrl, url)"))
        assertTrue(page.contains("navigationBlocked = true"))
        assertTrue(page.contains("if (navigationBlocked)"))
        assertTrue(page.contains("view.tag = null"))
        assertTrue(page.contains("settings.javaScriptCanOpenWindowsAutomatically = false"))
    }

    private fun readSource(relativePath: String): String =
        sequenceOf(File(relativePath), File("app/$relativePath"))
            .firstOrNull(File::isFile)
            ?.readText()
            ?: error("Source file not found: $relativePath")
}
