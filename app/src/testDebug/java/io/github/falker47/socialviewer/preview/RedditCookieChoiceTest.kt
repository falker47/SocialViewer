package io.github.falker47.socialviewer.preview

import org.junit.Assert.*
import org.junit.Test

class RedditCookieChoiceTest {
    @Test fun onlyExplicitChoicesAreSupported() {
        assertEquals("reject", RedditCookieChoice.REJECT.token)
        assertEquals("accept", RedditCookieChoice.ACCEPT.token)
        assertTrue(parseRedditCookieConsentState("\"ready\""))
        assertFalse(parseRedditCookieConsentState("\"absent\""))
        assertFalse(parseRedditCookieConsentState(null))
        assertTrue(parseRedditCookieConsentSubmission("true"))
        assertFalse(parseRedditCookieConsentSubmission("false"))
    }

    @Test fun consentAssetExposesOnlyUserRequestedActions() {
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
