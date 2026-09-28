package io.github.falker47.socialviewer.ui

internal fun cookieHeaderContains(
    cookieHeader: String?,
    cookieName: String,
): Boolean {
    if (cookieHeader.isNullOrBlank() || cookieName.isBlank()) return false

    return cookieHeader
        .split(';')
        .any { part -> part.substringBefore('=').trim() == cookieName }
}
