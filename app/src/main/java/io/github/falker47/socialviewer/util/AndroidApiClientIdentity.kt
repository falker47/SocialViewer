package io.github.falker47.socialviewer.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

internal fun googleAndroidApiClientHeaders(context: Context): Map<String, String> {
    val certificateSha1 = signingCertificateSha1(context) ?: return emptyMap()

    return mapOf(
        "X-Android-Package" to context.packageName,
        "X-Android-Cert" to certificateSha1,
    )
}

@Suppress("DEPRECATION")
private fun signingCertificateSha1(context: Context): String? = runCatching {
    val packageManager = context.packageManager
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNING_CERTIFICATES,
        )
    } else {
        packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNATURES,
        )
    }

    val signature = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.signingInfo
            ?.apkContentsSigners
            ?.firstOrNull()
    } else {
        packageInfo.signatures?.firstOrNull()
    } ?: return@runCatching null

    MessageDigest.getInstance("SHA-1")
        .digest(signature.toByteArray())
        .joinToString(separator = "") { byte ->
            "%02X".format(byte.toInt() and 0xFF)
        }
}.getOrNull()
