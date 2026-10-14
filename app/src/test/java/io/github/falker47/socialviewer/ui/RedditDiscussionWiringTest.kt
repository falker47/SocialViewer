package io.github.falker47.socialviewer.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RedditDiscussionWiringTest {
    @Test
    fun actionLivesInTheReadyPlayerWithoutReplacingTheWebView() {
        val source = sourceFile("java/io/github/falker47/socialviewer/ui/SocialViewerApp.kt").readText()
        val player = source.substringAfter("private fun PlayerScreen(").substringBefore("private fun ErrorScreen(")
        assertEquals(1, Regex("RedditDiscussionAction\\(content = content\\)").findAll(player).count())
        assertTrue(player.contains("EmbedWebView("))
        assertTrue(player.contains("baseUrl = content.documentBaseUrl"))
    }

    @Test
    fun browserDiscoveryUsesNarrowPackageVisibilityQueriesOnly() {
        val document = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(sourceFile("AndroidManifest.xml"))
        val queries = document.getElementsByTagName("queries")
        assertEquals(1, queries.length)
        val actions = (queries.item(0) as org.w3c.dom.Element).getElementsByTagName("action")
        val names = (0 until actions.length).map {
            actions.item(it).attributes.getNamedItemNS("http://schemas.android.com/apk/res/android", "name").nodeValue
        }
        assertTrue(names.contains("android.intent.action.MAIN"))
        assertTrue(names.contains("android.support.customtabs.action.CustomTabsService"))
        assertFalse(sourceFile("AndroidManifest.xml").readText().contains("android.permission.QUERY_ALL_PACKAGES"))
    }

    @Test
    fun buttonHasMatchingEnglishAndItalianResources() {
        fun names(path: String): Set<String> {
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(sourceFile(path))
            val strings = document.getElementsByTagName("string")
            return (0 until strings.length).map { strings.item(it).attributes.getNamedItem("name").nodeValue }.toSet()
        }
        assertEquals(names("res/values/reddit_discussion.xml"), names("res/values-it/reddit_discussion.xml"))
    }

    private fun sourceFile(path: String): File = sequenceOf(File("src/main/$path"), File("app/src/main/$path"))
        .firstOrNull(File::isFile) ?: error("Source file not found: $path")
}
