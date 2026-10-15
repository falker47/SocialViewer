package io.github.falker47.socialviewer.preview

import io.github.falker47.socialviewer.BuildConfig
import org.junit.Assert.*
import org.junit.Test

class RedditDiagnosticReleaseIdentityTest {
    @Test fun releaseIdentityAndVersionRemainUnchanged() {
        assertEquals("io.github.falker47.socialviewer", BuildConfig.APPLICATION_ID)
        assertEquals("0.1.5", BuildConfig.VERSION_NAME)
        assertFalse(BuildConfig.DEBUG)
    }

    @Test fun diagnosticClipboardCodeIsAbsentFromRelease() {
        try {
            Class.forName("io.github.falker47.socialviewer.preview.RedditPageDiagnosticsKt")
            fail("Debug diagnostics leaked into release")
        } catch (_: ClassNotFoundException) {
            // Expected: this implementation belongs only to src/debug.
        }
    }
}
