package io.github.falker47.socialviewer.provider.pinterest

class PinterestPinRequiredException : IllegalArgumentException(
    "Pinterest share link did not resolve to a supported public Pin",
)
