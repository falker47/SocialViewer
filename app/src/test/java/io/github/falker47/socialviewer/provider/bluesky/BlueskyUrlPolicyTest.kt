package io.github.falker47.socialviewer.provider.bluesky

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlueskyUrlPolicyTest {
    @Test
    fun supportsCanonicalHandleAndDidPostUrls() {
        assertTrue(
            BlueskyUrlPolicy.supports(
                "https",
                "bsky.app",
                "/profile/bsky.app/post/3l6oveex3ii2l",
            ),
        )
        assertTrue(
            BlueskyUrlPolicy.supports(
                "https",
                "bsky.app",
                "/profile/did:plc:z72i7hdynmk6r22z27h6tvur/post/3l6oveex3ii2l/",
            ),
        )
    }

    @Test
    fun canonicalizesHandleCaseAndDropsTrackingByPathIdentity() {
        assertEquals(
            "https://bsky.app/profile/example.com/post/3l6oveex3ii2l",
            BlueskyUrlPolicy.canonicalPostUrl(
                "https",
                "bsky.app",
                "/profile/Example.COM/post/3l6oveex3ii2l/",
            ),
        )
    }

    @Test
    fun preservesDidAndRecordKeyIdentity() {
        assertEquals(
            "https://bsky.app/profile/did:plc:z72i7hdynmk6r22z27h6tvur/post/3l6oveex3ii2l",
            BlueskyUrlPolicy.canonicalPostUrl(
                "https",
                "bsky.app",
                "/profile/did:plc:z72i7hdynmk6r22z27h6tvur/post/3l6oveex3ii2l",
            ),
        )
    }

    @Test
    fun rejectsProfilesFeedsHttpLookalikesAndExtraPaths() {
        assertFalse(BlueskyUrlPolicy.supports("https", "bsky.app", "/"))
        assertFalse(BlueskyUrlPolicy.supports("https", "bsky.app", "/profile/bsky.app"))
        assertFalse(
            BlueskyUrlPolicy.supports(
                "https",
                "bsky.app",
                "/profile/bsky.app/post/3l6oveex3ii2l/replies",
            ),
        )
        assertFalse(
            BlueskyUrlPolicy.supports(
                "http",
                "bsky.app",
                "/profile/bsky.app/post/3l6oveex3ii2l",
            ),
        )
        assertFalse(
            BlueskyUrlPolicy.supports(
                "https",
                "bsky.example.com",
                "/profile/bsky.app/post/3l6oveex3ii2l",
            ),
        )
        assertFalse(
            BlueskyUrlPolicy.supports(
                "https",
                "www.bsky.app",
                "/profile/bsky.app/post/3l6oveex3ii2l",
            ),
        )
        assertNull(
            BlueskyUrlPolicy.canonicalPostUrl(
                "https",
                "bsky.app",
                "/profile/not_a_handle/post/3l6oveex3ii2l",
            ),
        )
        assertFalse(
            BlueskyUrlPolicy.supports(
                "https",
                "bsky.app",
                "/profile/example.123/post/3l6oveex3ii2l",
            ),
        )
        assertFalse(
            BlueskyUrlPolicy.supports(
                "https",
                "bsky.app",
                "/profile/éxample.com/post/3l6oveex3ii2l",
            ),
        )
        assertFalse(
            BlueskyUrlPolicy.supports(
                "https",
                "bsky.app",
                "/profile/bsky.app/post/ünicode",
            ),
        )
        assertFalse(
            BlueskyUrlPolicy.supports(
                "https",
                "bsky.app",
                "/profile/did:key:zExample/post/3l6oveex3ii2l",
            ),
        )
    }
}
