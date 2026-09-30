package io.github.falker47.socialviewer.provider.tiktok

import android.net.Uri
import io.github.falker47.socialviewer.network.UrlConnectionHttpClient
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TikTokPhotoResolutionTest {
    private val photo = "https://www.tiktok.com/@carlocalendaofficial/photo/7690935067106905377?_r=1&_t=ZG-9AA1JMDqzMf"
    private val video = "https://www.tiktok.com/@example/video/1234567890"

    @Test fun reportedVmLinkRendersPhotoDespiteOembed400() {
        val http = FakeHttp(photo, 400)
        val content = TikTokProvider(http).resolve(Uri.parse("https://vm.tiktok.com/ZGdQwpHav/"))
        assertEquals(listOf("https://vm.tiktok.com/ZGdQwpHav/"), http.resolutions)
        assertEquals(photo, content.canonicalUrl)
        assertTrue(content.embedHtml.contains("/player/v1/7690935067106905377?"))
        assertEquals("cookie-consent", content.reloadOnCookieName)
        assertNull(content.title)
        assertNull(content.authorName)
    }

    @Test fun canonicalPhotoDoesNotNeedRedirectResolution() {
        val http = FakeHttp(photo, 400)
        val content = TikTokProvider(http).resolve(Uri.parse(photo))
        assertTrue(http.resolutions.isEmpty())
        assertTrue(content.embedHtml.contains("/player/v1/7690935067106905377?"))
    }

    @Test fun vtPhotoAndTPathUseTheSamePhotoFallback() {
        for (url in listOf("https://vt.tiktok.com/ZGdQwpHav/", "https://www.tiktok.com/t/ZGdQwpHav/")) {
            val http = FakeHttp(photo, 400)
            assertEquals(photo, TikTokProvider(http).resolve(Uri.parse(url)).canonicalUrl)
            assertEquals(listOf(url), http.resolutions)
        }
    }

    @Test fun successfulPhotoMetadataIsPreserved() {
        val http = FakeHttp(photo, 200)
        val content = TikTokProvider(http).resolve(Uri.parse(photo))
        assertEquals("A public post", content.title)
        assertEquals("Example", content.authorName)
    }

    @Test fun canonicalAndVtVideosStillRequireSuccessfulOembed() {
        for (url in listOf(video, "https://vt.tiktok.com/example/")) {
            val http = FakeHttp(video, 200)
            val content = TikTokProvider(http).resolve(Uri.parse(url))
            assertEquals("A public post", content.title)
            assertTrue(content.embedHtml.contains("/player/v1/1234567890?"))
            assertEquals(1, http.gets.size)
        }
    }

    @Test fun video400IsNotSilentlyAccepted() {
        assertThrows(IllegalStateException::class.java) {
            TikTokProvider(FakeHttp(video, 400)).resolve(Uri.parse(video))
        }
    }

    @Test fun photoAccessErrorsAreNotSilentlyAccepted() {
        for (status in listOf(401, 403, 404, 429, 500)) {
            assertThrows(IllegalStateException::class.java) {
                TikTokProvider(FakeHttp(photo, status)).resolve(Uri.parse(photo))
            }
        }
    }

    @Test fun shortLinkRedirectOutsideTikTokIsRejectedBeforeMetadata() {
        val http = FakeHttp("https://example.com/@example/photo/123", 400)
        assertThrows(IllegalArgumentException::class.java) {
            TikTokProvider(http).resolve(Uri.parse("https://vm.tiktok.com/example/"))
        }
        assertTrue(http.gets.isEmpty())
    }

    @Test fun invalidPostIdIsRejectedBeforeMetadata() {
        val http = FakeHttp(photo, 400)
        assertThrows(IllegalStateException::class.java) {
            TikTokProvider(http).resolve(Uri.parse("https://www.tiktok.com/@example/photo/not-an-id"))
        }
        assertTrue(http.gets.isEmpty())
    }

    private class FakeHttp(val target: String, val status: Int) : UrlConnectionHttpClient() {
        val resolutions = mutableListOf<String>()
        val gets = mutableListOf<String>()
        override fun resolveFinalUrl(url: String): String {
            resolutions += url
            return target
        }
        override fun get(url: String): Response {
            gets += url
            return Response(status, url, if (status == 200) {
                """{"title":"A public post","author_name":"Example"}"""
            } else "Bad Request")
        }
    }
}
