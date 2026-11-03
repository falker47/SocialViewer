# SocialViewer — Play-like isolated QA gate for Instagram Reel #38

Prepared: 3 November 2026. Source branch `spike/instagram-reel-inapp-webview-38`.

## What changed

- An explicit build-time flag `BuildConfig.INSTAGRAM_REEL_PAGE_QA` is OFF by
  default (ordinary debug and Play `release`) and ON only for isolated
  `linkQa` and `playQa` builds.
- `playQa` uses R8-optimized release settings and separate
  `io.github.falker47.socialviewer.playqa` application identity,
  with the debug test certificate, not the Google Play upload key.
- The experimental first-party Instagram Reel WebView has a fail-closed
  state guard so a pending Compose AndroidView update cannot reload a page
  after a rejected navigation.
- CI builds and inspects both isolated packages and runs `testPlayQaUnitTest`.

## What is proved and what remains

The previous Link QA APK at `049eddaa` was reported to play video/audio on a
real phone; that observation is not proof for this new source revision. The
new `playQa` APK requires separate physical QA.

The JavaScript site may replace visible content while retaining the same URL,
so the URL allowlist and visited-history observer cannot guarantee single-item
isolation. Do not add DOM hacks, media extraction, content proxying or bypasses.
A provider-approved playback path or applicable permission, together with
demonstrable product confinement, is still required before a production merge.

This build does **not** modify `release` behavior, package identity or Play
version code (`0.1.9`, code `10`) and must not be uploaded to Google Play.
A later official RC must use a checked unused versionCode and actual secure
release-signing credentials. The privacy-policy draft PR #75 currently
describes the official oEmbed production behavior and must not be released as
a description of a production full-page Instagram WebView.

Physical test matrix: see `TESTING.md`.
