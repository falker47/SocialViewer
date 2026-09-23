package io.github.falker47.socialviewer.util

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

class UrlExtractorTest {
    @Test
    fun extractsFirstUrlFromSharedText() {
        assertEquals(
            "https://vm.tiktok.com/ZM123/",
            UrlExtractor.firstHttpUrl("Guarda questo: https://vm.tiktok.com/ZM123/ 😂"),
        )
    }

    @Test
    fun removesCommonTrailingPunctuation() {
        assertEquals(
            "https://www.tiktok.com/@foo/video/123",
            UrlExtractor.firstHttpUrl("(https://www.tiktok.com/@foo/video/123)."),
        )
    }

    @Test
    fun returnsNullWhenNoHttpUrlExists() {
        assertNull(UrlExtractor.firstHttpUrl("nessun link qui"))
    }
}
