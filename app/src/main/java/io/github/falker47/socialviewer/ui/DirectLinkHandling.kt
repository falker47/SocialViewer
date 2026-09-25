package io.github.falker47.socialviewer.ui

import android.content.Context
import android.content.Intent
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.net.Uri
import android.os.Build
import android.provider.Settings

internal enum class DirectLinkProviderStatus {
    ACTIVE,
    PARTIAL,
    NEEDS_SETUP,
}

internal data class DirectLinkProviderDefinition(
    val providerId: String,
    val displayName: String,
    val hosts: Set<String>,
)

internal val DIRECT_LINK_PROVIDERS = listOf(
    DirectLinkProviderDefinition(
        providerId = "tiktok",
        displayName = "TikTok",
        hosts = linkedSetOf(
            "tiktok.com",
            "www.tiktok.com",
            "m.tiktok.com",
            "vm.tiktok.com",
            "vt.tiktok.com",
        ),
    ),
    DirectLinkProviderDefinition(
        providerId = "instagram",
        displayName = "Instagram",
        hosts = linkedSetOf(
            "instagram.com",
            "www.instagram.com",
        ),
    ),
    DirectLinkProviderDefinition(
        providerId = "threads",
        displayName = "Threads",
        hosts = linkedSetOf(
            "threads.com",
            "www.threads.com",
            "threads.net",
            "www.threads.net",
        ),
    ),
    DirectLinkProviderDefinition(
        providerId = "reddit",
        displayName = "Reddit",
        hosts = linkedSetOf(
            "reddit.com",
            "www.reddit.com",
            "redd.it",
        ),
    ),
    DirectLinkProviderDefinition(
        providerId = "pinterest",
        displayName = "Pinterest",
        hosts = linkedSetOf(
            "pinterest.com",
            "www.pinterest.com",
        ),
    ),
    DirectLinkProviderDefinition(
        providerId = "bluesky",
        displayName = "Bluesky",
        hosts = linkedSetOf(
            "bsky.app",
        ),
    ),
)

internal data class DirectLinkProviderState(
    val definition: DirectLinkProviderDefinition,
    val status: DirectLinkProviderStatus,
    val approvedHosts: Set<String>,
)

internal data class DirectLinkHandlingState(
    val platformStateAvailable: Boolean,
    val linkHandlingAllowed: Boolean,
    val providers: List<DirectLinkProviderState>,
) {
    val activeProviderCount: Int
        get() = providers.count { it.status == DirectLinkProviderStatus.ACTIVE }

    val allProvidersActive: Boolean
        get() = providers.isNotEmpty() &&
            providers.all { it.status == DirectLinkProviderStatus.ACTIVE }

    val anyProviderConfigured: Boolean
        get() = providers.any { it.status != DirectLinkProviderStatus.NEEDS_SETUP }
}

internal fun buildDirectLinkHandlingState(
    platformStateAvailable: Boolean,
    linkHandlingAllowed: Boolean,
    approvedHosts: Set<String>,
): DirectLinkHandlingState {
    val normalizedApprovedHosts = approvedHosts.mapTo(mutableSetOf()) { it.lowercase() }

    val providers = DIRECT_LINK_PROVIDERS.map { definition ->
        val approvedForProvider = definition.hosts.intersect(normalizedApprovedHosts)
        val status = when {
            !platformStateAvailable || !linkHandlingAllowed ->
                DirectLinkProviderStatus.NEEDS_SETUP

            approvedForProvider.containsAll(definition.hosts) ->
                DirectLinkProviderStatus.ACTIVE

            approvedForProvider.isNotEmpty() ->
                DirectLinkProviderStatus.PARTIAL

            else ->
                DirectLinkProviderStatus.NEEDS_SETUP
        }

        DirectLinkProviderState(
            definition = definition,
            status = status,
            approvedHosts = approvedForProvider,
        )
    }

    return DirectLinkHandlingState(
        platformStateAvailable = platformStateAvailable,
        linkHandlingAllowed = linkHandlingAllowed,
        providers = providers,
    )
}

internal fun queryDirectLinkHandlingState(context: Context): DirectLinkHandlingState {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return buildDirectLinkHandlingState(
            platformStateAvailable = false,
            linkHandlingAllowed = false,
            approvedHosts = emptySet(),
        )
    }

    return runCatching {
        val manager = context.getSystemService(DomainVerificationManager::class.java)
        val userState = manager.getDomainVerificationUserState(context.packageName)

        if (userState == null) {
            buildDirectLinkHandlingState(
                platformStateAvailable = true,
                linkHandlingAllowed = false,
                approvedHosts = emptySet(),
            )
        } else {
            val approvedHosts = userState.hostToStateMap
                .filterValues { state ->
                    state == DomainVerificationUserState.DOMAIN_STATE_SELECTED ||
                        state == DomainVerificationUserState.DOMAIN_STATE_VERIFIED
                }
                .keys

            buildDirectLinkHandlingState(
                platformStateAvailable = true,
                linkHandlingAllowed = userState.isLinkHandlingAllowed,
                approvedHosts = approvedHosts,
            )
        }
    }.getOrElse {
        buildDirectLinkHandlingState(
            platformStateAvailable = true,
            linkHandlingAllowed = false,
            approvedHosts = emptySet(),
        )
    }
}

internal fun openDefaultLinkSettings(context: Context) {
    val packageUri = Uri.parse("package:${context.packageName}")

    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, packageUri)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
    }

    runCatching { context.startActivity(intent) }
        .onFailure {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri),
            )
        }
}
