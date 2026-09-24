package io.github.falker47.socialviewer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectLinkHandlingTest {
    @Test
    fun providerHostMappingIncludesTikTokInstagramAndThreads() {
        assertEquals(
            listOf("tiktok", "instagram", "threads"),
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
        assertEquals(3, state.activeProviderCount)
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
