package io.github.falker47.socialviewer.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import java.net.URI
import javax.xml.parsers.DocumentBuilderFactory

class DirectLinkManifestCoverageTest {
    @Test
    fun tiktokWildcardCoversCurrentAndFutureSubdomainsWithoutExtraCheckboxHosts() {
        assertTrue(manifestClaims("https://www.tiktok.com/@user/video/1234567890123456789"))
        assertTrue(manifestClaims("https://vm.tiktok.com/ABC123/"))
        assertTrue(manifestClaims("https://future.tiktok.com/@user/video/1234567890123456789"))

        val tiktokHosts = viewFilters()
            .flatMap { it.hosts }
            .filter { it == "tiktok.com" || it.endsWith(".tiktok.com") }
            .toSet()

        assertTrue("tiktok.com" in tiktokHosts)
        assertTrue("*.tiktok.com" in tiktokHosts)
        assertFalse("www.tiktok.com" in tiktokHosts)
        assertFalse("vm.tiktok.com" in tiktokHosts)
    }

    @Test
    fun pinterestShortAliasIsClaimedButPinterestNavigationIsNotBroadened() {
        assertTrue(manifestClaims("https://pin.it/AbC123xyz"))
        assertTrue(manifestClaims("https://www.pinterest.com/pin/123456789/"))

        assertFalse(manifestClaims("https://pin.it/"))
        assertFalse(manifestClaims("https://www.pinterest.com/"))
        assertFalse(manifestClaims("https://www.pinterest.com/example-user/"))
        assertFalse(manifestClaims("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertFalse(manifestClaims("https://youtu.be/dQw4w9WgXcQ"))
    }

    @Test
    fun redditWildcardAddsLegacyPostAndShareHostsButNotNavigation() {
        assertTrue(
            manifestClaims(
                "https://old.reddit.com/r/android/comments/1abc234/example_post/",
            ),
        )
        assertTrue(
            manifestClaims(
                "https://m.reddit.com/r/android/s/AbC123_xYz/",
            ),
        )

        assertFalse(manifestClaims("https://old.reddit.com/r/android/"))
        assertFalse(manifestClaims("https://new.reddit.com/user/example_user/"))
        assertFalse(manifestClaims("https://redd.it/1abc234"))
    }

    private fun manifestClaims(url: String): Boolean {
        val uri = URI(url)
        val host = uri.host?.lowercase() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        val path = uri.path ?: "/"

        return viewFilters().any { filter ->
            val hasPathConstraint =
                filter.literalPaths.isNotEmpty() ||
                    filter.pathPrefixes.isNotEmpty() ||
                    filter.pathPatterns.isNotEmpty()
            val pathMatches =
                !hasPathConstraint ||
                    filter.literalPaths.any { it == path } ||
                    filter.pathPrefixes.any { path.startsWith(it) } ||
                    filter.pathPatterns.any { matchesAndroidSimpleGlob(it, path) }

            filter.hosts.any { manifestHost -> hostMatches(manifestHost, host) } &&
                scheme in filter.schemes &&
                pathMatches
        }
    }

    private fun hostMatches(manifestHost: String, actualHost: String): Boolean =
        if (manifestHost.startsWith("*.")) {
            actualHost.endsWith(manifestHost.removePrefix("*")) &&
                actualHost != manifestHost.removePrefix("*.")
        } else {
            actualHost == manifestHost
        }

    private fun matchesAndroidSimpleGlob(pattern: String, value: String): Boolean {
        var patternIndex = 0
        var valueIndex = 0

        while (patternIndex < pattern.length) {
            val patternChar = pattern[patternIndex]
            val followedByStar =
                patternIndex + 1 < pattern.length && pattern[patternIndex + 1] == '*'

            if (followedByStar) {
                if (patternChar == '.') {
                    patternIndex += 2
                    if (patternIndex == pattern.length) return true

                    val nextLiteral = pattern[patternIndex]
                    val stop = value.indexOf(nextLiteral, valueIndex)
                    if (stop < 0) return false
                    valueIndex = stop
                } else {
                    while (valueIndex < value.length && value[valueIndex] == patternChar) {
                        valueIndex += 1
                    }
                    patternIndex += 2
                }
                continue
            }

            if (valueIndex >= value.length) return false
            if (patternChar != '.' && patternChar != value[valueIndex]) return false

            patternIndex += 1
            valueIndex += 1
        }

        return valueIndex == value.length
    }

    private fun viewFilters(): List<ViewFilter> {
        val manifest = sequenceOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
        ).firstOrNull(File::isFile)
            ?: error("AndroidManifest.xml not found from unit-test working directory")

        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }
        val document = factory.newDocumentBuilder().parse(manifest)
        val filters = document.getElementsByTagName("intent-filter")

        return buildList {
            for (index in 0 until filters.length) {
                val filter = filters.item(index) as Element
                val actions = filter.getElementsByTagName("action")
                val isView = (0 until actions.length).any { actionIndex ->
                    val action = actions.item(actionIndex) as Element
                    action.androidAttribute("name") == "android.intent.action.VIEW"
                }
                if (!isView) continue

                val hosts = linkedSetOf<String>()
                val schemes = linkedSetOf<String>()
                val literalPaths = linkedSetOf<String>()
                val pathPrefixes = linkedSetOf<String>()
                val pathPatterns = linkedSetOf<String>()
                val dataNodes = filter.getElementsByTagName("data")

                for (dataIndex in 0 until dataNodes.length) {
                    val data = dataNodes.item(dataIndex) as Element
                    data.androidAttribute("host").takeIf(String::isNotBlank)?.let {
                        hosts += it.lowercase()
                    }
                    data.androidAttribute("scheme").takeIf(String::isNotBlank)?.let {
                        schemes += it.lowercase()
                    }
                    data.androidAttribute("path").takeIf(String::isNotBlank)?.let(literalPaths::add)
                    data.androidAttribute("pathPrefix").takeIf(String::isNotBlank)?.let(pathPrefixes::add)
                    data.androidAttribute("pathPattern").takeIf(String::isNotBlank)?.let(pathPatterns::add)
                }

                add(
                    ViewFilter(
                        hosts = hosts,
                        schemes = schemes,
                        literalPaths = literalPaths,
                        pathPrefixes = pathPrefixes,
                        pathPatterns = pathPatterns,
                    ),
                )
            }
        }
    }

    private fun Element.androidAttribute(name: String): String =
        getAttributeNS(ANDROID_NAMESPACE, name)

    private data class ViewFilter(
        val hosts: Set<String>,
        val schemes: Set<String>,
        val literalPaths: Set<String>,
        val pathPrefixes: Set<String>,
        val pathPatterns: Set<String>,
    )

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}
