package io.github.falker47.socialviewer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Static Android integration safeguards. These are not substitutes for
 * Android 15+ device QA of insets, the keyboard, or embedded WebViews.
 */
class EdgeToEdgeContractTest {
    @Test
    fun activityEnablesEdgeToEdgeAfterSuperAndBeforeCompose() {
        val source = mainSource("java/io/github/falker47/socialviewer/MainActivity.kt").readText()
        val create = source.indexOf("super.onCreate(savedInstanceState)")
        val edge = source.indexOf("enableEdgeToEdge()")
        val compose = source.indexOf("setContent {")

        assertTrue("Activity must call super.onCreate", create >= 0)
        assertTrue("Edge-to-edge must follow Activity creation", edge > create)
        assertTrue("Edge-to-edge must be configured before Compose content", compose > edge)
    }

    @Test
    fun appearanceUsesInsetsControllerAndNoLegacySystemUiFlagWrites() {
        val source = mainSource("java/io/github/falker47/socialviewer/ui/SocialViewerApp.kt").readText()

        assertTrue(source.contains("WindowCompat.getInsetsController(window, window.decorView)"))
        assertTrue(source.contains("isAppearanceLightStatusBars"))
        assertTrue(source.contains("isAppearanceLightNavigationBars"))
        assertFalse("Legacy status bar color mutation", source.contains("window.statusBarColor ="))
        assertFalse("Legacy navigation bar color mutation", source.contains("window.navigationBarColor ="))
        assertFalse("Legacy systemUiVisibility flags", source.contains("systemUiVisibility"))
    }

    @Test
    fun bothThemesAvoidDeprecatedBarColors() {
        listOf("values/themes.xml", "values-night/themes.xml").forEach { path ->
            val styles = parse(mainSource("res/" + path)).getElementsByTagName("style")
            val style = (0 until styles.length)
                .map { styles.item(it) as Element }
                .single { it.getAttribute("name") == "Theme.SocialViewer" }
            val items = style.getElementsByTagName("item")
            val names = (0 until items.length)
                .map { (items.item(it) as Element).getAttribute("name") }

            assertFalse(path + " still sets statusBarColor", "android:statusBarColor" in names)
            assertFalse(path + " still sets navigationBarColor", "android:navigationBarColor" in names)
        }
    }

    @Test
    fun mainActivityKeepsKeyboardResizeAndNavigationContracts() {
        val manifest = parse(mainSource("AndroidManifest.xml"))
        val activities = manifest.getElementsByTagName("activity")
        val mainActivity = (0 until activities.length)
            .map { activities.item(it) as Element }
            .single { it.getAttributeNS(ANDROID_NS, "name") == ".MainActivity" }

        assertEquals("adjustResize", mainActivity.getAttributeNS(ANDROID_NS, "windowSoftInputMode"))
        assertEquals("singleTop", mainActivity.getAttributeNS(ANDROID_NS, "launchMode"))
    }

    private fun parse(file: File) = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }.newDocumentBuilder().parse(file)

    private fun mainSource(path: String): File = sequenceOf(
        File("src/main/" + path),
        File("app/src/main/" + path),
    ).firstOrNull(File::isFile)
        ?: error("Missing application source file: " + path)

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
