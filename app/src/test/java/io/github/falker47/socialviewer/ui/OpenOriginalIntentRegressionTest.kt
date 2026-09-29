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
            "Open-original helper must use an Android chooser",
            source.contains("Intent.createChooser(target, null)"),
        )
        assertTrue(
            "Open-original chooser must exclude SocialViewer's MainActivity",
            source.contains("Intent.EXTRA_EXCLUDE_COMPONENTS") &&
                source.contains("ComponentName(context, MainActivity::class.java)"),
        )
        assertEquals(
            "Every original-content action must use the exclusion helper",
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
