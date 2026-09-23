package io.github.falker47.socialviewer.ui

import android.content.Context
import android.content.Intent
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.net.Uri
import android.os.Build
import android.provider.Settings

private val TIKTOK_LINK_HOSTS = setOf(
    "tiktok.com",
    "www.tiktok.com",
    "m.tiktok.com",
    "vm.tiktok.com",
    "vt.tiktok.com",
)

internal fun isTikTokDirectLinkHandlingActive(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false

    return runCatching {
        val manager = context.getSystemService(DomainVerificationManager::class.java)
        val userState = manager.getDomainVerificationUserState(context.packageName) ?: return false
        if (!userState.isLinkHandlingAllowed) return false

        TIKTOK_LINK_HOSTS.all { host ->
            when (userState.hostToStateMap[host]) {
                DomainVerificationUserState.DOMAIN_STATE_SELECTED,
                DomainVerificationUserState.DOMAIN_STATE_VERIFIED -> true

                else -> false
            }
        }
    }.getOrDefault(false)
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
