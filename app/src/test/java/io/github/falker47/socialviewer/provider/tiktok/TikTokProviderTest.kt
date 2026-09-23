package io.github.falker47.socialviewer.provider.tiktok

import org.junit.Test
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class TikTokProviderTest {
    @Test
    fun supportsTikTokHosts() {
        assertTrue(TikTokUrlPolicy.supports("https", "www.tiktok.com"))
        assertTrue(TikTokUrlPolicy.supports("https", "vm.tiktok.com"))
        assertTrue(TikTokUrlPolicy.supports("https", "vt.tiktok.com"))
    }

    @Test
    fun rejectsOtherHostsAndSchemes() {
        assertFalse(TikTokUrlPolicy.supports("https", "instagram.com"))
        assertFalse(TikTokUrlPolicy.supports("http", "www.tiktok.com"))
        assertFalse(TikTokUrlPolicy.supports("https", "faketiktok.com"))
    }

    @Test
    fun identifiesShortLinks() {
        assertTrue(TikTokUrlPolicy.isShortLink("vm.tiktok.com", "/ZM123/"))
        assertTrue(TikTokUrlPolicy.isShortLink("www.tiktok.com", "/t/ZM123/"))
        assertFalse(TikTokUrlPolicy.isShortLink("www.tiktok.com", "/@foo/video/123"))
    }
}
