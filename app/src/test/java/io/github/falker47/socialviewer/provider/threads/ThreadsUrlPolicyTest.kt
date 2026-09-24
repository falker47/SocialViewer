package io.github.falker47.socialviewer.provider.threads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadsUrlPolicyTest {
    @Test
    fun supportsPublicPermalinkAndShorthandAcrossCurrentAndLegacyDomains() {
        assertTrue(
            ThreadsUrlPolicy.supports(
                "https",
                "www.threads.com",
                "/@threads/post/DWjTI0cgH5O/",
            ),
        )
        assertTrue(
            ThreadsUrlPolicy.supports(
                "https",
                "threads.com",
                "/t/DWjTI0cgH5O/",
            ),
        )
        assertTrue(
            ThreadsUrlPolicy.supports(
                "https",
                "www.threads.net",
                "/@threads/post/DWjTI0cgH5O/",
            ),
        )
    }

    @Test
    fun canonicalizesLegacyUrlsToThreadsCom() {
        assertEquals(
            "https://www.threads.com/@alice.name/post/ABC_123-def/",
            ThreadsUrlPolicy.canonicalUrl(
                "https",
                "threads.net",
                "/@alice.name/post/ABC_123-def/",
            ),
        )
        assertEquals(
            "https://www.threads.com/t/ABC_123-def/",
            ThreadsUrlPolicy.canonicalUrl(
                "https",
                "www.threads.net",
                "/t/ABC_123-def/",
            ),
        )
    }

    @Test
    fun rejectsProfilesUnsupportedRoutesHttpAndLookalikeHosts() {
        assertFalse(ThreadsUrlPolicy.supports("https", "www.threads.com", "/@alice/"))
        assertFalse(ThreadsUrlPolicy.supports("https", "www.threads.com", "/@alice/replies/ABC/"))
        assertFalse(ThreadsUrlPolicy.supports("http", "www.threads.com", "/t/ABC/"))
        assertFalse(ThreadsUrlPolicy.supports("https", "threads.example.com", "/t/ABC/"))
        assertNull(
            ThreadsUrlPolicy.canonicalUrl(
                "https",
                "www.threads.com",
                "/@alice/post/",
            ),
        )
    }
}
