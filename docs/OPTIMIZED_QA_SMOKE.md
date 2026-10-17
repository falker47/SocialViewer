# SocialViewer 0.1.6 — optimized release-candidate smoke (17 Oct 2026)

**TEST BUILD ONLY — NEVER UPLOAD THIS QA APK TO PLAY.**

Baseline: `main` `b922703329f8afc5f24af8e8c297da2781b99f26`, release `0.1.6 (7)`.
Scope: combined unmerged PR #31 (AGP 9.4 R8 optimizations) and #32 (edge-to-edge).
PR #30 is instrumentation only. This QA branch must not be merged.
The isolated `qa` build type initializes from `release`, enables R8 code/resource
optimization, remains **non-debuggable**, and uses the Android debug signing key.

## Safe installation boundary

- QA application ID: `io.github.falker47.socialviewer.optimizationqa` (not the Play package).
- Displayed name: `SocialViewer QA`; version: `0.1.6-qa`; versionCode 7.
- Installing the QA APK cannot replace the Play-signed SocialViewer package or erase its storage.
- It has its **own app storage and provider cookies**; on first use it may correctly
  request consent again. Do **not** delete data in the Play app for this test.
- QA registers its own link filters, but do **not** choose it as the Android default
  for any provider domains on the main phone. Test direct-link handling separately
  on an emulator or non-primary Android device.
- GitHub CI has no `YOUTUBE_API_KEY`. Consequently, its downloadable QA APK
  cannot perform a successful YouTube Data API preflight. This is a **known build
  configuration limitation, not a passed provider test**.
- Even a locally configured production YouTube API key may reject the QA app ID
  and debug signing certificate. That is an API key restriction; do not weaken
  production restrictions or share secrets to bypass it. For the final eight-provider
  gate use a separately authorized package/certificate pair or a production-identity
  build on an isolated device, following `TESTING.md`.

## CI gates

1. Same main-baseline test suite, release lint, debug APK, unsigned release AAB
   packaging and size reports.
2. Build the **obfuscated** `assembleQa` APK with a generic Android debug key.
3. Verify package ID and app label using Android SDK tools, plus R8 mapping
   output presence and artifact SHA-256.
4. Download the installable QA APK from the PR workflow artifact only if all pass.
5. No Play/upload/release task runs as part of this CI.

## Physical parity gate (compare the same *known-public URLs* with current Play app)

Record PASS/FAIL/NOT TESTED; fail closed on blockers.

| Area | Check | Result |
| --- | --- | --- |
| Install | Both Play SocialViewer and `SocialViewer QA` visible; Play data intact | NOT TESTED |
| TikTok | canonical video + vm/vt short link; fullscreen; consent preserved in QA | NOT TESTED |
| Instagram | post + Reel, unavailable/error behavior | NOT TESTED |
| Threads | public post/shorthand, no feed escape | NOT TESTED |
| Reddit | post and expandable replies in same WebView; explicit Reject/Accept cookie actions; Back/Original | NOT TESTED |
| Pinterest | single Pin + pin.it alias | NOT TESTED |
| X | privacy notice/consent; loaded public post; revocation setting | NOT TESTED |
| Bluesky | single post + profile links remain outside app scope | NOT TESTED |
| YouTube | watch + Short + player fullscreen; explicit MFK exclusion; **requires locally valid client identity** | NOT TESTED |
| Navigation | paste/clipboard, Share, Back gesture and 3-button navigation, Original opens browser | NOT TESTED |
| Privacy | no login/history/analytics in app; only user chooses cookie consent; Clear site data | NOT TESTED |
| Layout | Android 15+ gesture/3-button, portrait/landscape, light/dark, keyboard/IME, cutouts, no occlusion | NOT TESTED |
| Locales | IT + EN UI, settings, privacy/cookie text parity | NOT TESTED |
| Performance | 5 cold starts and 5 item opens on same device/network, plus memory under repeated browsing | NOT TESTED |

Do not infer behavioral performance gains from lower APK/AAB size. If a screen
fails, send a screenshot, public URL, phone/Android version, and the failing
QA workflow commit. Do not send cookies, API keys or personal browser history.

## Promotion conditions — no automatic publishing

- CI green on exact candidate head.
- Physical parity gate includes all eight supported providers and all key privacy,
  language, cookie, direct-link, video, navigation and layout contracts.
- Separate signable next-release versionCode > 7 from canonical approved `main`;
  verify the uploaded artifact identity, signature, mapping file, and upload certificate.
- Owner approval of each merge and the new Google Play Closed Test release.
- Reuse the existing Closed Testing track and tester enrollment without opt-out.
