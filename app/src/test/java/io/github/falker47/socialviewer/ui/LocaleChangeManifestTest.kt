package io.github.falker47.socialviewer.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class LocaleChangeManifestTest {
    @Test
    fun mainActivityHandlesLocaleAndLayoutDirectionChanges() {
        val manifest = sequenceOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
        ).firstOrNull(File::isFile)
            ?: error("AndroidManifest.xml not found from unit-test working directory")

        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }
        val document = factory.newDocumentBuilder().parse(manifest)
        val activities = document.getElementsByTagName("activity")
        val mainActivity = (0 until activities.length)
            .map { activities.item(it) as Element }
            .single { it.androidAttribute("name") == ".MainActivity" }

        val configChanges = mainActivity.androidAttribute("configChanges")
            .split('|')
            .toSet()

        assertTrue("MainActivity must handle locale changes", "locale" in configChanges)
        assertTrue(
            "MainActivity must handle layoutDirection changes",
            "layoutDirection" in configChanges,
        )
    }

    private fun Element.androidAttribute(name: String): String =
        getAttributeNS(ANDROID_NAMESPACE, name)

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}
