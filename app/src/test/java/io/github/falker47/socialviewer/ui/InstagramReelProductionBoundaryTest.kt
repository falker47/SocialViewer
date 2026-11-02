package io.github.falker47.socialviewer.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class InstagramReelProductionBoundaryTest {
    @Test
    fun productionInstagramReelsUseTheOfficialEmbedAndExternalBrowserEscape() {
        val shell = readSource("src/main/java/io/github/falker47/socialviewer/ui/SocialViewerApp.kt")
        val embed = readSource("src/main/java/io/github/falker47/socialviewer/ui/EmbedWebView.kt")

        // Permission for full Instagram webpages inside an app WebView is not
        // verified. Keep rendering on Meta's official embed and provide an
        // explicit external browser escape when that embed cannot play inline.
        assertFalse(shell.contains("InstagramReelWebView("))
        assertFalse(shell.contains("InstagramUrlPolicy.mobileWebReelUrl(content.canonicalUrl)"))
        assertTrue(shell.contains("EmbedWebView("))
        assertTrue(shell.contains("InstagramUrlPolicy.mobileWebReelUrl(url) ?: url"))

        assertTrue(embed.contains("setAcceptThirdPartyCookies(currentWebView, false)"))
        assertTrue(embed.contains("return request.isForMainFrame"))
        assertTrue(embed.contains("webChromeClient = fullscreenController"))
    }

    @Test
    fun ordinaryInstagramPostsAndOtherProvidersKeepTheirOfficialEmbedPaths() {
        val shell = readSource("src/main/java/io/github/falker47/socialviewer/ui/SocialViewerApp.kt")

        // Instagram posts/Reels and other providers share the existing
        // provider-approved embed renderer; there is no full-site WebView path.
        assertFalse(shell.contains("firstPartyReelUrl"))
        assertFalse(shell.contains("InstagramReelWebView("))
        assertTrue(shell.contains("EmbedWebView("))
        assertTrue(shell.contains("RedditPagePreview("))
    }

    private fun readSource(relativePath: String): String =
        sequenceOf(File(relativePath), File("app/$relativePath"))
            .firstOrNull(File::isFile)
            ?.readText()
            ?: error("Source file not found: $relativePath")
}
