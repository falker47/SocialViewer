package io.github.falker47.socialviewer.network

class LinkResolutionHttpException(
    val statusCode: Int,
) : IllegalStateException("Link resolution returned HTTP $statusCode")
