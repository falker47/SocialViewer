# SocialViewer 0.1.10 — Instagram Reel in-app source release candidate

Prepared 3 November 2026 in PR #40, branch `spike/instagram-reel-inapp-webview-38`.

**No upload, merge, production rollout, or signed bundle is implied by this file.**

## Product implementation

- Official package `io.github.falker47.socialviewer` unchanged.
- Version `0.1.10`, versionCode `11`, provisional until all active Google
  Play tracks confirm it is unused.
- The official `release` variant now uses the identical first-party
  Instagram single-Reel WebView already tested with video/audio on the
  owner's phone in the Link QA app on 3 November 2026.
- The renderer is selected only for canonical public Instagram Reel links.
  Instagram ordinary posts and the seven other providers stay on their
  existing renderers; debug still uses oEmbed for comparison.
- Existing safeguards block off-Reel top-level navigation and react to
  disallowed history changes. No DOM rewrites, login workarounds, media
  extraction, rehosting, proxying, or download mechanisms have been added.

## Verification and remaining requirements

- CI must pass on the **exact source SHA**: unit tests, lint, official debug
  and release builds, Link QA and Play QA checks, official package/version.
- The CI-produced AAB is intentionally **unsigned** and has no production
  YouTube key. It is NOT a Play-uploadable release.
- `playReleaseBundle` needs the already existing secure upload keystore
  and YouTube key; never create or publish replacement credentials.
- Verify Play Console's current closed/internal/production tracks and code
  11 availability before any upload.
- Update Nexus Portfolio privacy PR #75 and Play Data Safety to describe
  the first-party Instagram page and provider-cookie/network behavior.
- Google's WebView spam rule is not a blanket ban on WebViews, but terms
  and permissions of the Instagram provider must still be respected.
- Android's WebViewClient URL/history callbacks cannot guarantee that a
  third-party SPA never shows different content at an unchanged URL. This is
  a known residual limitation; do not falsely claim complete confinement.
- Once a signed Play-distributed build exists, only a focused install/update,
  launch, connectivity, signing/API-key and one-Reel smoke is necessary; do
  not require the owner to repeat the previously accepted playback test.

Reference: issue #38; PR #40; existing Play 0.1.9/code 10 historical state
reported 2 November 2026, not a verified 3 November live track read.
