package io.github.falker47.socialviewer.preview

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RedditPageResourcesTest {
    @Test fun experimentalEnglishAndItalianResourcesHaveMatchingKeys() {
        fun names(locale: String): Set<String> {
            val path = "src/debug/res/$locale/reddit_page_preview.xml"
            val file = sequenceOf(File(path), File("app/$path")).first(File::isFile)
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            val strings = document.getElementsByTagName("string")
            return (0 until strings.length).map { strings.item(it).attributes.getNamedItem("name").nodeValue }.toSet()
        }
        val english = names("values")
        assertTrue(english.isNotEmpty())
        assertEquals(english, names("values-it"))
    }
}
