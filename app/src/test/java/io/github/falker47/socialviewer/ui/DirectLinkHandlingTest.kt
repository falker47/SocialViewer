package io.github.falker47.socialviewer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectLinkHandlingTest {
    @Test
    fun providerHostMappingIncludesAllDeclaredDirectLinkProviders() {
        assertEquals(
            listOf("tiktok", "instagram", "threads", "reddit", "pinterest", "bluesky"),
            DIRECT_LINK_PROVIDERS.map { it.providerId },
        )
        assertEquals(
            setOf(
                "tiktok.com",
                "www.tiktok.com",
                "m.tiktok.com",
                "vm.tiktok.com",
                "vt.tiktok.com",
            ),
            DIRECT_LINK_PROVIDERS.first { it.providerId == "tiktok" }.hosts,
        )
        assertEquals(
            setOf("instagram.com", "www.instagram.com"),
            DIRECT_LINK_PROVIDERS.first { it.providerId == "instagram" }.hosts,
        )
        assertEquals(
            setOf("threads.com", "www.threads.com", "threads.net", "www.threads.net"),
            DIRECT_LINK_PROVIDERS.first { it.providerId == "threads" }.hosts,
        )
        assertEquals(
            setOf("reddit.com", "www.reddit.com", "redd.it"),
            DIRECT_LINK_PROVIDERS.first { it.providerId == "reddit" }.hosts,
        )
        assertEquals(
            setOf("pinterest.com", "www.pinterest.com"),
            DIRECT_LINK_PROVIDERS.first { it.providerId == "pinterest" }.hosts,
        )
        assertEquals(
            setOf("bsky.app"),
            DIRECT_LINK_PROVIDERS.first { it.providerId == "bluesky" }.hosts,
        )
    }

    @Test
    fun allDeclaredHostsApprovedMakesAllProvidersActive() {
        val allHosts = DIRECT_LINK_PROVIDERS.flatMap { it.hosts }.toSet()
        val state = buildDirectLinkHandlingState(
            platformStateAvailable = true,
            linkHandlingAllowed = true,
            approvedHosts = allHosts,
        )

        assertTrue(state.allProvidersActive)
        assertEquals(6, state.activeProviderCount)
        assertTrue(state.providers.all { it.status == DirectLinkProviderStatus.ACTIVE })
    }

    @Test
    fun providerStatesRemainIndependent() {
        val tikTokHosts = DIRECT_LINK_PROVIDERS
            .first { it.providerId == "tiktok" }
            .hosts

        val state = buildDirectLinkHandlingState(
            platformStateAvailable = true,
            linkHandlingAllowed = true,
            approvedHosts = tikTokHosts + "instagram.com",
        )

        assertFalse(state.allProvidersActive)
        assertTrue(state.anyProviderConfigured)
        assertEquals(1, state.activeProviderCount)
        assertEquals(
            DirectLinkProviderStatus.ACTIVE,
            state.providers.first { it.definition.providerId == "tiktok" }.status,
        )
        assertEquals(
            DirectLinkProviderStatus.PARTIAL,
            state.providers.first { it.definition.providerId == "instagram" }.status,
        )
        assertEquals(
            DirectLinkProviderStatus.NEEDS_SETUP,
            state.providers.first { it.definition.providerId == "pinterest" }.status,
        )
    }

    @Test
    fun redditStateIsPartialUntilAllDeclaredRedditHostsAreApproved() {
        val state = buildDirectLinkHandlingState(
            platformStateAvailable = true,
            linkHandlingAllowed = true,
            approvedHosts = setOf("reddit.com", "www.reddit.com"),
        )

        val reddit = state.providers.first { it.definition.providerId == "reddit" }
        assertEquals(DirectLinkProviderStatus.PARTIAL, reddit.status)
        assertEquals(setOf("reddit.com", "www.reddit.com"), reddit.approvedHosts)

        val completedState = buildDirectLinkHandlingState(
            platformStateAvailable = true,
            linkHandlingAllowed = true,
            approvedHosts = setOf("reddit.com", "www.reddit.com", "redd.it"),
        )

        assertEquals(
            DirectLinkProviderStatus.ACTIVE,
            completedState.providers.first { it.definition.providerId == "reddit" }.status,
        )
    }

    @Test
    fun globalAndroidLinkHandlingGateOverridesHostSelection() {
        val allHosts = DIRECT_LINK_PROVIDERS.flatMap { it.hosts }.toSet()
        val state = buildDirectLinkHandlingState(
            platformStateAvailable = true,
            linkHandlingAllowed = false,
            approvedHosts = allHosts,
        )

        assertFalse(state.allProvidersActive)
        assertFalse(state.anyProviderConfigured)
        assertTrue(state.providers.all { it.status == DirectLinkProviderStatus.NEEDS_SETUP })
    }
}
