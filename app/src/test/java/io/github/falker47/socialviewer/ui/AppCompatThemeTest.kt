package io.github.falker47.socialviewer.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class AppCompatThemeTest {
    @Test
    fun everyThemeSocialViewerVariantUsesAppCompatParent() {
        val themeFiles = listOf(
            resourceFile("values/themes.xml"),
            resourceFile("values-night/themes.xml"),
        )

        themeFiles.forEach { file ->
            val factory = DocumentBuilderFactory.newInstance()
            val document = factory.newDocumentBuilder().parse(file)
            val styles = document.getElementsByTagName("style")
            val target = (0 until styles.length)
                .map { styles.item(it) as Element }
                .single { it.getAttribute("name") == "Theme.SocialViewer" }

            assertTrue(
                file.path + " must inherit from Theme.AppCompat; actual=" + target.getAttribute("parent"),
                target.getAttribute("parent").startsWith("Theme.AppCompat"),
            )
        }
    }

    private fun resourceFile(relativePath: String): File =
        sequenceOf(
            File("src/main/res/" + relativePath),
            File("app/src/main/res/" + relativePath),
        ).firstOrNull(File::isFile)
            ?: error(relativePath + " not found from unit-test working directory")
}
