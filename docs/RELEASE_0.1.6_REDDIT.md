# SocialViewer 0.1.6 (7) — Reddit discussions

## User-visible changes
- Read a shared public Reddit post and its expandable comments in one in-app view; no external browser handoff is required for reading.
- On the initial Reddit visit, a portrait cookie-consent overlay exposes **Reject optional cookies** and **Accept all**, with no automatic choice. Reddit's own controls process the explicit choice.
- Remove debug-only Reddit preview heading, diagnostics copy action and sample/test surfaces from the shipping variant.
- Preserve Original as an explicit external browser action and the existing provider/link handling behavior.

## Distribution safety
- Package ID remains \`io.github.falker47.socialviewer\`.
- Version is \`0.1.6\` (versionCode 7); replace only on the existing Closed Testing track.
- Play upload keystore and YouTube key are **not** present in GitHub CI; its unsigned bundle is not a publishable release.
- Requires local signed \`playReleaseBundle\` and bundle identity/signature checks before uploading to Play Console.
- Closed Test tester opt-in and eligibility must remain unchanged.

## Verification
- CI: Android unit tests, lint (debug/release), APK and unsigned AAB build.
- Physical-device smoke before Play upload: same AskReddit permalink, cookie preference buttons, comments expansion, Original, Android back, translation and rotation.
- Other providers: at least one already-known representative post per category, and privacy clearance must remain available.
- Keep the existing branch/release in place until the new Closed track displays versionCode 7 and its review/publication state is confirmed.
