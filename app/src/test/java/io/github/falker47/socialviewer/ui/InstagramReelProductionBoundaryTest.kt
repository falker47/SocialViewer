package io.github.falker47.socialviewer.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class InstagramReelProductionBoundaryTest {
    @Test
    fun instagramReelFirstPartyPageIsAvailableInNormalProductionBuilds() {
        val shell = readSource("src/main/java/io/github/falker47/socialviewer/ui/SocialViewerApp.kt")
        val player = readSource("src/main/java/io/github/falker47/socialviewer/ui/InstagramReelWebView.kt")

        // The QA prototype succeeded on a device, but the old implementation
        // hides that player behind a package-id check.
        assertFalse(shell.contains("BuildConfig.APPLICATION_ID.endsWith(\".linkqa\")"))
        assertTrue(shell.contains("content.providerId == \"instagram\""))
        assertTrue(shell.contains("InstagramUrlPolicy.mobileWebReelUrl(content.canonicalUrl)"))
        assertTrue(shell.contains("InstagramReelWebView("))

        // Preserve provider-page boundaries instead of opening Instagram
        // as a generic in-app browser.
        assertTrue(player.contains("InstagramUrlPolicy.allowsInAppReelNavigation("))
        assertTrue(player.contains("setAcceptThirdPartyCookies(currentWebView, false)"))
        assertTrue(player.contains("settings.javaScriptCanOpenWindowsAutomatically = false"))
        assertTrue(player.contains("settings.setSupportMultipleWindows(false)"))
        assertTrue(player.contains("settings.mediaPlaybackRequiresUserGesture = true"))
    }

    @Test
    fun ordinaryInstagramPostsAndOtherProvidersKeepTheirOfficialEmbedPaths() {
        val shell = readSource("src/main/java/io/github/falker47/socialviewer/ui/SocialViewerApp.kt")

        // URL policy returns null on Instagram /p/ posts and on any other
        // provider, so the regular official embed is still selected.
        assertTrue(shell.contains("content.providerId == \"instagram\""))
        assertTrue(shell.contains("if (firstPartyReelUrl != null)"))
        assertTrue(shell.contains("EmbedWebView("))
        assertTrue(shell.contains("RedditPagePreview("))
    }

    private fun readSource(relativePath: String): String =
        sequenceOf(File(relativePath), File("app/$relativePath"))
            .firstOrNull(File::isFile)
            ?.readText()
            ?: error("Source file not found: $relativePath")
}
