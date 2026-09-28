package io.github.falker47.socialviewer.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CookieHeaderTest {
    @Test
    fun matchesExactCookieNameInRequestHeader() {
        val header = "ttwid=abc; cookie-consent=%7Bchoice%7D; msToken=xyz"

        assertTrue(cookieHeaderContains(header, "cookie-consent"))
        assertFalse(cookieHeaderContains(header, "cookie"))
        assertFalse(cookieHeaderContains(header, "missing"))
    }

    @Test
    fun handlesEmptyCookieHeaders() {
        assertFalse(cookieHeaderContains(null, "cookie-consent"))
        assertFalse(cookieHeaderContains("", "cookie-consent"))
    }
}
