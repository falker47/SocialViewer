# SocialViewer — Play-like isolated QA gate for Instagram Reel #38

Prepared: 3 November 2026. Source branch `spike/instagram-reel-inapp-webview-38`.

## What changed

- BuildConfig uses `INSTAGRAM_REEL_FIRST_PARTY_ENABLED` (OFF in normal
  debug, ON in official `release` and both isolated QA variants).
- `playQa` uses R8-optimized release settings and separate
  `io.github.falker47.socialviewer.playqa` application identity,
  with the debug test certificate, not the Google Play upload key.
- The experimental first-party Instagram Reel WebView has a fail-closed
  state guard so a pending Compose AndroidView update cannot reload a page
  after a rejected navigation.
- CI runs the existing `testDebugUnitTest` suite for shared logic and
  builds/inspects both isolated QA packages; `testPlayQaUnitTest` is not
  available in this project's AGP 9.4 configuration.

## What is proved and what remains

The previous Link QA APK at `049eddaa` was reported to play video/audio on a
real phone; that observation is not proof for this new source revision. The
new `playQa` APK requires separate physical QA.

The JavaScript site may replace visible content while retaining the same URL,
so the URL allowlist and visited-history observer cannot guarantee single-item
isolation. Do not add DOM hacks, media extraction, content proxying or bypasses.
A provider-approved playback path or applicable permission, together with
demonstrable product confinement, is still required before a production merge.

Release source on PR #40 now enables the previously verified Reel WebView.
Provisional source version: `0.1.10` / code 11, official app ID unchanged.
CI builds an unsigned AAB, not uploadable to Play; the existing signing
keystore, production YouTube key and unused Play versionCode still require
live verification. Privacy policy draft PR #75 must be updated before
publication to describe the release behavior correctly.

Physical test matrix: see `TESTING.md`.
