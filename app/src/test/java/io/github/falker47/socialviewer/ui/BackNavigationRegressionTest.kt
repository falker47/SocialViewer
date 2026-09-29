package io.github.falker47.socialviewer.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BackNavigationRegressionTest {
    @Test
    fun androidBackMatchesInAppBackNavigation() {
        val source = socialViewerAppSource()

        assertTrue(
            "SocialViewerApp must intercept Android back while Settings or viewer content is open",
            source.contains(
                "enabled = screen == AppScreen.Settings || state != ViewerState.Home",
            ),
        )
        assertTrue(
            "Back from Settings must return to the viewer instead of finishing the Activity",
            source.contains("screen == AppScreen.Settings -> screen = AppScreen.Viewer"),
        )
        assertTrue(
            "Back from content/error/consent must return to Home",
            source.contains(
                """state != ViewerState.Home -> {
                screen = AppScreen.Viewer
                state = ViewerState.Home
            }""",
            ),
        )
    }

    private fun socialViewerAppSource(): String {
        val relativePath =
            "src/main/java/io/github/falker47/socialviewer/ui/SocialViewerApp.kt"

        return sequenceOf(
            File(relativePath),
            File("app/$relativePath"),
        ).firstOrNull(File::isFile)
            ?.readText()
            ?: error("SocialViewerApp.kt not found from unit-test working directory")
    }
}
