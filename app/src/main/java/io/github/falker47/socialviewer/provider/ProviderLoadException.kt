package io.github.falker47.socialviewer.provider

class ProviderHttpException(
    val providerName: String,
    val statusCode: Int,
) : IllegalStateException("$providerName metadata returned HTTP $statusCode")

class ProviderInvalidResponseException(
    val providerName: String,
    val statusCode: Int,
) : IllegalStateException("$providerName metadata was not valid JSON (HTTP $statusCode)")
