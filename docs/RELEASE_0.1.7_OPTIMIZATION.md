# SocialViewer 0.1.7 (8) — optimized Android release

## Scope
- Release identity: `io.github.falker47.socialviewer`, `versionName = 0.1.7`, `versionCode = 8`.
- Android Gradle Plugin 9.4 optimized R8 release pipeline: code shrinking, obfuscation, optimization, and resource shrinking.
- Android edge-to-edge with AndroidX Activity and modern WindowCompat icon contrast; legacy bar color APIs removed; `adjustResize` for the keyboard.
- No intended product changes to the eight providers, Reddit post/comments or explicit consent, TikTok cookies, privacy/site data, link handling, video, fullscreen, themes or IT/EN localization.
- No package ID, app-signing identity, upload key or Google Play Closed Testing track changes.

## Measured build impact (keyless CI, not Play download size)
The 0.1.6 baseline CI before shrinking: APK 12.344 MiB, AAB 12.253 MiB.
The optimized CI: APK about 1.559 MiB (−87.4%), AAB about 3.746 MiB (−69.4%).
These are package-size results, **not startup, battery or memory benchmarks**.
A production YouTube API key can alter the compiled release compared with keyless CI.

## Quality gates completed
- CI on main instrumentation PR #30: PASS.
- CI on AGP 9.4 R8 PR #31: PASS.
- CI on edge-to-edge PR #32: PASS with five regression tests.
- Combined installable QA PR #33: CI #215 PASS, 127 unit tests, release lint and keyless optimized APK PASS.
- **Owner's device report (2026-10-17):** combined optimized SocialViewer QA tested and described as functioning perfectly. No individual list of provider cases supplied; do not infer coverage for a test that the artifact cannot perform.
- This update requires its own final release 0.1.7 (8) CI PASS after the version bump.

## Remaining release gate — required before Play submission
1. Build from clean, synchronized canonical **main** after all release PR merges; check expected HEAD SHA and verify `versionCode = 8` / `versionName = 0.1.7`.
2. Produce the new **signed** `playReleaseBundle` locally with the existing Google Play upload JKS and the nonempty production `YOUTUBE_API_KEY`. Do not transfer either secret to GitHub CI or ChatGPT.
3. Verify `jarsigner -verify`, upload certificate SHA-1 against the already registered Play upload key, and bundletool manifest `applicationId/versionCode/versionName`. Preserve/backup the old signed 0.1.6 bundle; do not reuse its stale bytes. Confirm R8 mapping is generated; modern Android App Bundles normally carry deobfuscation mapping automatically.
4. YouTube was **not** a valid functional test on the CI QA APK because it had no production API key and a different package/signing identity. Verify YouTube playback/fullscreen/MFK policy with a correctly signed/configured candidate or smoke the Play-distributed updated version; until then, do not claim eight-provider coverage.
5. In Play Console, confirm there is no existing pending release, open the **existing** `Test chiusi - SocialViewer` Closed Testing track, and upload only the correctly signed 0.1.7 (8) AAB; do not create a new track, replace tester lists, re-enroll testers or change production.
6. Confirm release review/submission and actual Play Console publication state separately. Install from the Play store as an existing closed tester and re-check YouTube, Reddit comments/cookies, links, themes, fullscreen and navigation on version 8. Keep this as a follow-up if Play's review is pending.

## Suggested Play Console release notes (IT)
App più leggera grazie all'ottimizzazione del codice e delle risorse. Migliorata la compatibilità con la visualizzazione edge-to-edge di Android. Tutti i provider e le funzionalità restano disponibili.

## Suggested Play Console release notes (EN)
Smaller app through code and resource optimization. Improved Android edge-to-edge compatibility. All existing providers and features remain available.

## Distribution boundary
The CI AAB lacks developer signing and the production YouTube key and must never be uploaded.
The `qa/socialviewer-optimized-smoke-20261017` branch produces only a separately named test package `io.github.falker47.socialviewer.optimizationqa`, **not** the Play release.
Publish to the existing Closed Testing track only after signing, identity, and explicit owner authorization. Owner approved proceeding with the update after successful device testing on 2026-10-17, but signing and Play publication remain independently verifiable stages.
