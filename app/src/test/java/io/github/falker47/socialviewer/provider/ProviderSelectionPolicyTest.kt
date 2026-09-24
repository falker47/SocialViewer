package io.github.falker47.socialviewer.provider

import io.github.falker47.socialviewer.provider.facebook.FacebookUrlPolicy
import io.github.falker47.socialviewer.provider.instagram.InstagramUrlPolicy
import io.github.falker47.socialviewer.provider.tiktok.TikTokUrlPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderSelectionPolicyTest {
    @Test
    fun representativeUrlsMatchExactlyOneProviderPolicy() {
        assertEquals(
            "tiktok",
            selectedProvider(
                scheme = "https",
                host = "www.tiktok.com",
                path = "/@alice/video/123",
            ),
        )
        assertEquals(
            "instagram",
            selectedProvider(
                scheme = "https",
                host = "www.instagram.com",
                path = "/reel/ABC_123/",
            ),
        )
        assertEquals(
            "facebook",
            selectedProvider(
                scheme = "https",
                host = "www.facebook.com",
                path = "/alice/posts/pfbid123/",
            ),
        )
    }

    private fun selectedProvider(
        scheme: String,
        host: String,
        path: String,
    ): String? {
        val matches = buildList {
            if (TikTokUrlPolicy.supports(scheme, host)) add("tiktok")
            if (InstagramUrlPolicy.supports(scheme, host, path)) add("instagram")
            if (FacebookUrlPolicy.supports(scheme, host, path)) add("facebook")
        }
        return matches.singleOrNull()
    }
}
