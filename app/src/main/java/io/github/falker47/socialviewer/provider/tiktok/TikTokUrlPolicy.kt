package io.github.falker47.socialviewer.provider.tiktok

internal object TikTokUrlPolicy {
    fun supports(scheme: String?, host: String?): Boolean {
        if (!scheme.equals("https", ignoreCase = true)) return false
        val normalizedHost = host?.lowercase() ?: return false
        return normalizedHost == "tiktok.com" || normalizedHost.endsWith(".tiktok.com")
    }

    fun isShortLink(host: String?, path: String?): Boolean {
        val normalizedHost = host?.lowercase().orEmpty()
        return normalizedHost == "vm.tiktok.com" ||
            normalizedHost == "vt.tiktok.com" ||
            path.orEmpty().startsWith("/t/")
    }
}
