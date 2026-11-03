package io.github.falker47.socialviewer.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class InstagramReelQaBoundaryTest {
    @Test
    fun fullInstagramPageIsOnlyUsedByExplicitNonPlayQaBuilds() {
        val shell = readSource("src/main/java/io/github/falker47/socialviewer/ui/SocialViewerApp.kt")
        val page = readSource("src/main/java/io/github/falker47/socialviewer/ui/InstagramReelQaWebView.kt")

        assertTrue(shell.contains("BuildConfig.INSTAGRAM_REEL_PAGE_QA"))
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

    @Test
    fun qaFlagIsEnabledOnlyForExplicitIsolatedBuilds() {
        val gradle = readProjectFile("app/build.gradle.kts")
        val workflow = readProjectFile(".github/workflows/android-ci.yml")

        assertTrue(gradle.contains("buildConfigField(\"boolean\", \"INSTAGRAM_REEL_PAGE_QA\", \"false\")"))
        assertTrue(gradle.contains("create(\"linkQa\")"))
        assertTrue(gradle.contains("create(\"playQa\")"))
        assertTrue(gradle.contains("applicationIdSuffix = \".playqa\""))
        assertTrue(
            "Both isolated QA builds must explicitly opt in; debug and Play release opt out",
            Regex("buildConfigField\\(\"boolean\", \"INSTAGRAM_REEL_PAGE_QA\", \"true\"\\)")
                .findAll(gradle).count() == 2,
        )
        assertTrue(workflow.contains("assemblePlayQa"))
        assertTrue(workflow.contains("app-playQa.apk"))
    }

    @Test
    fun blockedNavigationNeverReloadsTheAllowedUrl() {
        val page = readSource("src/main/java/io/github/falker47/socialviewer/ui/InstagramReelQaWebView.kt")
        assertTrue(page.contains("if (!navigationBlocked && view.tag != reelWebUrl)"))
    }

    private fun readProjectFile(relativePath: String): String =
        sequenceOf(File(relativePath), File("../$relativePath"), File("../../$relativePath"))
            .firstOrNull(File::isFile)
            ?.readText()
            ?: error("Project file not found: $relativePath")

    private fun readSource(relativePath: String): String =
        sequenceOf(File(relativePath), File("app/$relativePath"))
            .firstOrNull(File::isFile)
            ?.readText()
            ?: error("Source file not found: $relativePath")
}
