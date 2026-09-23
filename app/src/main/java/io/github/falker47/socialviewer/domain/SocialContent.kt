package io.github.falker47.socialviewer.domain

data class SocialContent(
    val providerId: String,
    val providerName: String,
    val canonicalUrl: String,
    val title: String?,
    val authorName: String?,
    val embedHtml: String,
)
