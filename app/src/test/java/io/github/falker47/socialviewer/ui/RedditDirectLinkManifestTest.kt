package io.github.falker47.socialviewer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import java.net.URI
import javax.xml.parsers.DocumentBuilderFactory

class RedditDirectLinkManifestTest {
    @Test
    fun redditManifestHostsAndPatternsStayBoundedAndMatchSettingsDefinition() {
        val redditFilters = viewFilters().filter { filter ->
            filter.hosts.any { it in REDDIT_HOSTS }
        }

        assertEquals(1, redditFilters.size)

        val redditComFilter = redditFilters.single {
            it.hosts == setOf("reddit.com", "www.reddit.com")
        }
        assertEquals(setOf("https"), redditComFilter.schemes)
        assertEquals(
            setOf(
                "/r/..*/comments/..*/",
                "/r/..*/comments/..*/..*/",
                "/r/..*/comments/..*/..*/..*/",
                "/s/..*",
                "/r/..*/s/..*",
                "/u/..*/s/..*",
                "/user/..*/s/..*",
            ),
            redditComFilter.pathPatterns,
        )
        assertTrue(redditComFilter.pathPrefixes.isEmpty())
        assertTrue(redditComFilter.literalPaths.isEmpty())

        val manifestHosts = redditFilters.flatMapTo(linkedSetOf()) { it.hosts }
        val settingsHosts = DIRECT_LINK_PROVIDERS
            .single { it.providerId == "reddit" }
            .hosts

        assertEquals(settingsHosts, manifestHosts)
        assertFalse("old.reddit.com" in manifestHosts)
        assertFalse("new.reddit.com" in manifestHosts)
        assertFalse("m.reddit.com" in manifestHosts)
    }

    @Test
    fun declaredRedditRoutesCoverUsefulSingleContentLinks() {
        val claimed = listOf(
            "https://www.reddit.com/r/android/comments/1abc234/",
            "https://reddit.com/r/android/comments/1abc234/example_post/",
            "https://www.reddit.com/r/android/comments/1abc234/example_post/def567/",
            "https://reddit.com/s/AbC123_xYz",
            "https://www.reddit.com/r/android/s/AbC123_xYz/",
            "https://reddit.com/u/example_user/s/AbC123_xYz/",
            "https://www.reddit.com/user/example_user/s/AbC123_xYz/",
        )

        claimed.forEach { url ->
            assertTrue("Expected manifest to claim $url", manifestClaims(url))
        }
    }

    @Test
    fun genericRedditNavigationAndLegacyHostsAreNotClaimed() {
        val notClaimed = listOf(
            "https://www.reddit.com/",
            "https://www.reddit.com/r/android/",
            "https://www.reddit.com/r/all/",
            "https://www.reddit.com/r/popular/",
            "https://www.reddit.com/user/example_user/",
            "https://www.reddit.com/u/example_user/",
            "https://www.reddit.com/search/?q=android",
            "https://www.reddit.com/r/android/wiki/index/",
            "https://www.reddit.com/r/android/about/modqueue/",
            "https://www.reddit.com/message/inbox/",
            "https://www.reddit.com/settings/",
            "https://www.reddit.com/s/",
            "https://www.reddit.com/r/android/s/",
            "https://www.reddit.com/r/android/comments/",
            "https://www.reddit.com/r/android/comments/1abc234/example_post/def567/child/",
            "https://redd.it/",
            "https://redd.it/1abc234",
            "https://old.reddit.com/r/android/comments/1abc234/example_post/",
            "https://new.reddit.com/r/android/comments/1abc234/example_post/",
            "https://m.reddit.com/r/android/comments/1abc234/example_post/",
        )

        notClaimed.forEach { url ->
            assertFalse("Manifest must not claim $url", manifestClaims(url))
        }
    }

    private fun manifestClaims(url: String): Boolean {
        val uri = URI(url)
        val host = uri.host?.lowercase() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        val path = uri.path ?: "/"

        return viewFilters().any { filter ->
            host in filter.hosts &&
                scheme in filter.schemes &&
                when {
                    filter.literalPaths.isNotEmpty() ->
                        filter.literalPaths.any { it == path }

                    filter.pathPrefixes.isNotEmpty() ->
                        filter.pathPrefixes.any { path.startsWith(it) }

                    filter.pathPatterns.isNotEmpty() ->
                        filter.pathPatterns.any { matchesAndroidSimpleGlob(it, path) }

                    else -> true
                }
        }
    }

    /**
     * Minimal deterministic model of Android PATTERN_SIMPLE_GLOB for the subset used by
     * this manifest: literals, '.', '*' and '.*'. Android documents '.*' as lazy and
     * non-backtracking, so a following '/' bounds it to the next path segment.
     */
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
                    if (patternIndex == pattern.length) {
                        return true
                    }

                    val nextLiteral = pattern[patternIndex]
                    require(nextLiteral != '.' && nextLiteral != '*') {
                        "Test matcher only supports a literal after .*"
                    }
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
        val REDDIT_HOSTS = setOf(
            "reddit.com",
            "www.reddit.com",
            "old.reddit.com",
            "new.reddit.com",
            "m.reddit.com",
            "redd.it",
        )
    }
}
