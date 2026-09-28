package io.github.falker47.socialviewer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinItDiagnosticsTest {
    @Test
    fun tracksOnlyHttpsPinItUrls() {
        assertTrue(PinItDiagnostics.tracks("https://pin.it/1pTjG6L"))
        assertTrue(PinItDiagnostics.tracks("HTTPS://PIN.IT/1pTjG6L"))
        assertFalse(PinItDiagnostics.tracks("http://pin.it/1pTjG6L"))
        assertFalse(PinItDiagnostics.tracks("https://pin.it.example/1pTjG6L"))
        assertFalse(PinItDiagnostics.tracks("https://www.pinterest.com/pin/1098104321628496170/"))
    }

    @Test
    fun safeUrlDropsQueryAndFragment() {
        assertEquals(
            "https://pin.it/1pTjG6L",
            PinItDiagnostics.safeUrl(
                "https://pin.it/1pTjG6L?token=do-not-log#fragment",
            ),
        )
        assertEquals(
            "https://it.pinterest.com/pin/1098104321628496170/",
            PinItDiagnostics.safeUrl(
                "https://it.pinterest.com/pin/1098104321628496170/?utm_source=test#details",
            ),
        )
    }

    @Test
    fun diagnosticLineFlattensControlCharacters() {
        val line = PinItDiagnostics.line(
            "resolve_error",
            "message" to "first line\nsecond line\tthird",
        )

        assertFalse(line.contains("\n"))
        assertFalse(line.contains("\t"))
        assertTrue(line.contains("stage=resolve_error"))
        assertTrue(line.contains("message=first line second line third"))
    }
}
