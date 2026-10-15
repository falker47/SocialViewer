package io.github.falker47.socialviewer.preview

import io.github.falker47.socialviewer.BuildConfig
import org.junit.Assert.*
import org.junit.Test

class RedditDiagnosticsTest {
    @Test fun diagnosticInstallHasItsOwnApplicationIdentity() {
        assertEquals("io.github.falker47.socialviewer.redditdiag", BuildConfig.APPLICATION_ID)
        assertTrue(BuildConfig.DEBUG)
    }

    @Test fun acceptsBoundedLayoutReports() {
        val result = validateRedditDiagnosticReport("""{"schema":"SV_REDDIT_DIAG_V1","snapshots":[{"phase":"manual","nodes":[]}]}""")
        assertNotNull(result)
        assertTrue(result!!.contains("SV_REDDIT_DIAG_V1"))
    }

    @Test fun rejectsMissingMalformedOrOversizedReports() {
        listOf(null, "null", "undefined", "{}", "not json", "x".repeat(48_001),
            """{"schema":"SV_REDDIT_DIAG_V1","snapshots":[]}""",
            """{"schema":"OTHER","snapshots":[{}]}""",
        ).forEach { assertNull(validateRedditDiagnosticReport(it)) }
    }
}
