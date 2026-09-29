package io.github.falker47.socialviewer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OpenOriginalIntentRegressionTest {
    @Test
    fun originalContentAlwaysUsesChooserThatExcludesSocialViewer() {
        val source = socialViewerAppSource()

        assertTrue(
            "Open-original helper must constrain resolution to browser applications",
            source.contains("Intent.CATEGORY_APP_BROWSER"),
        )
        assertTrue(
            "Open-original helper must preserve ACTION_VIEW with the original URL",
            source.contains("Intent(Intent.ACTION_VIEW, Uri.parse(url))"),
        )
        assertTrue(
            "Open-original helper must resolve through a selector instead of app-link ownership",
            source.contains("selector = browserSelector"),
        )
        assertEquals(
            "Every original-content action must use the browser-only helper",
            3,
            Regex("""openOriginalExternal\(context,""").findAll(source).count(),
        )
    }

    @Test
    fun genericExternalLinksKeepNormalActionViewBehavior() {
        val source = socialViewerAppSource()

        assertTrue(
            "Generic external links must stay on the normal ACTION_VIEW path",
            source.contains(
                """private fun openExternal(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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
