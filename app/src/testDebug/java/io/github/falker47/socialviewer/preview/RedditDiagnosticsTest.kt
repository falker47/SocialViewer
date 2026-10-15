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

    @Test fun cookieConsentRequiresExplicitNativeSelection() {
        val file = sequenceOf(
            java.io.File("src/debug/assets/reddit-consent-choice.js"),
            java.io.File("app/src/debug/assets/reddit-consent-choice.js"),
        ).first(java.io.File::isFile)
        val script = file.readText()
        assertTrue(script.contains("#data-protection-consent-dialog"))
        assertTrue(script.contains("choose(choice)"))
        assertTrue(script.contains("options[choice].click()"))
        assertFalse(script.contains("document.cookie"))
        assertFalse(script.contains("localStorage"))
        assertFalse(script.contains("setTimeout("))
    }
}
