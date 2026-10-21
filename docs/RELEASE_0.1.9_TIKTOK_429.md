# SocialViewer 0.1.9 (10) — TikTok oEmbed rate-limit fallback

## Scope and evidence

- Intended app/package: **SocialViewer** / `io.github.falker47.socialviewer`, `versionName=0.1.9`, `versionCode=10`.
- Target: the existing **Test chiusi - SocialViewer** Google Play Closed Testing track only; no production promotion, new tracks, or tester settings changes.
- Last Play checkpoint (20 Oct 2026): 0.1.8 (9) available to selected testers. The current Play Console state must be checked before uploading version 10.
- Technical fix: TikTok official player is still loaded when optional `oEmbed` metadata responds HTTP 429. No extra retry/bypass; other provider failures (401/403/404/500 and video 400) remain errors.
- [PR #36](https://github.com/falker47/SocialViewer/pull/36) merged into `main` at commit `17519284c586627f707ce8bf8d822937c9ad695b`; [CI main PASS](https://github.com/falker47/SocialViewer/actions/runs/39607799636).
- On 21 October 2026, owner reported a physical-device **PASS** opening `https://vm.tiktok.com/ZGdQ3ga1n/` by pasting it in the Home of the isolated QA app with the 429 fix. This is specific to that link, build, and observed attempt—not a claim that TikTok will never rate-limit playback.

## Branding and build identity

- **Play release**: main `app_name=SocialViewer` (no `Link QA`), original package `io.github.falker47.socialviewer`, signed with the existing upload JKS.
- **Isolated QA build only**: `app/src/linkQa/res/values/strings.xml` intentionally uses `SocialViewer Link QA` and package suffix `.linkqa`. This variant must never be uploaded to Play.
- CI produces unsigned/keyless release bundles; its green result does not authorize publishing that artifact as a signed Play bundle.
- Existing production YouTube API key is required for a publishable bundle. Preserve signing certificates, R8 optimization, enabled providers, and existing Play track/testers.

## Final local signed AAB gate (not performed in GitHub CI)

1. After this release PR is merged and `main` CI is green, synchronize a **clean** local checkout of the exact `main` SHA and verify package/version/name `SocialViewer / 0.1.9 (10)`.
2. Preserve the previous signed bundle and use the **existing local** `PLAY_UPLOAD_*` signing configuration and `YOUTUBE_API_KEY`. Do not upload any secret/keystore to ChatGPT or GitHub.
3. Run `gradlew.bat clean :app:playReleaseBundle` and verify the actual generated `app/build/outputs/bundle/release/*.aab`, upload certificate (JKS), non-debuggable status, package/version, SHA-256, and presence of production YouTube key without exposing it.
4. In Google Play Console open the **existing** Closed Testing track, confirm versionCode 10 does not conflict with any uploaded bundle, and upload only that exact newly signed AAB.
5. Preserve testers, countries, signing identity and rollout scope. Save the release notes below, review and submit. Record separately **submitted**, **in review**, **available to testers**, and a smoke test of the Play-installed build.

## Release notes — Italian

Corretto un problema che poteva impedire l'apertura di alcuni contenuti TikTok quando il servizio dei metadati limitava le richieste. Migliorata l'affidabilità del caricamento senza modificare le altre piattaforme.

## Release notes — English

Fixed an issue that could prevent some TikTok posts from opening when the metadata service rate-limited requests. Improved link-loading reliability without changes to other platforms.
