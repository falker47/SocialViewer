# SocialViewer 0.1.10 — Instagram embed and browser fallback

Prepared: 2 November 2026

Application ID: `io.github.falker47.socialviewer`
Version: `0.1.10` (`versionCode = 11`)

## Release notes

- Instagram posts and Reels continue to use Meta's official embed inside SocialViewer.
- When Instagram's embed does not provide inline playback for a Reel, the single **Guarda sul web** action opens the selected Reel in the external browser.
- The same **Guarda sul web** action remains available for all supported providers, with provider-specific URLs preserved.

## Release identity and status

Play Console was checked live on 2 November 2026. The existing closed-testing track was on `0.1.9` (`versionCode = 10`), so this candidate uses the next version code, 11. Application ID remains unchanged.

The `?l=1` query is an observed Instagram browser behavior, not a documented player API. This candidate uses it only for the explicit external browser action. It does not load Instagram's full mobile page inside the app; that permission and the behavior of same-document navigation could not be verified for a commercial WebView distribution.

This file records candidate notes only. It does not mean the signed AAB has been built, uploaded, or released.

The hosted bilingual privacy-policy update is proposed separately in [Nexus-portfolio PR #75](https://github.com/falker47/Nexus-portfolio/pull/75); it is still a draft and has not been published.
