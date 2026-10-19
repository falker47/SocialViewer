# SocialViewer 0.1.8 (9) — link loading and Home controls

## Release identity and authorization

- Package: `io.github.falker47.socialviewer`.
- Version: `0.1.8`, versionCode `9`.
- Destination: the existing **Test chiusi - SocialViewer** Closed Testing track.
- On 19 October 2026 at 22:31 Europe/Rome, the owner reported QA3 working,
  requested the final Clear/Paste arrangement, and authorized the update.
- Live Play Console before this preparation showed version 8 (0.1.7) available
  to closed testers, latest uploaded bundle 8, and no unpublished changes.
- Source preparation, signed AAB verification, submission for review and
  availability to testers are distinct states. This document does not claim
  that 0.1.8 has already been submitted or published.

## Changes

- Obsolete or cancelled link loads cannot overwrite Home or a newer request.
- Resolver errors distinguish connection/timeout, HTTP, malformed TikTok
  metadata and unsupported Pinterest destinations, with Italian/English copy.
- Recognized Pinterest profiles receive the single-Pin explanation in both
  manual input and short-link resolution.
- Home includes a clear button that removes the URL and its inline error.
  The final order is **Clear, Paste**, with icons 4 dp closer overall and the
  original separate button hit areas preserved.

Provider admission rules, public-content and embedding requirements, X consent,
the existing TikTok photo-only fallback, network retry behavior and YouTube
API-key restrictions are preserved.

## Evidence and remaining observations

- QA2 source `adae467` passed 153 automated tests and Android CI.
- QA3 source `9ccdcd1` passed 153 automated tests, release lint, optimized QA
  packaging, identity and signature checks in [CI 39316601346](https://github.com/falker47/SocialViewer/actions/runs/39316601346).
- The owner reported the positive Pinterest comparison and a retried TikTok
  working at 21:33, returning Home during a pending load working at 21:56,
  and the QA3 clear control working at 22:31. These are user reports, not
  instrumented traffic or exhaustive provider tests.
- The final source and merged main each require their own green CI. Earlier
  green runs do not certify the new version bump and final arrangement.
- TikTok's earlier 429 cause and variable latency remain unmeasured. The
  successful retest does not prove that app code removed a provider rate limit.
- CI QA is keyless for YouTube. Its result does not validate Play YouTube.
  Carry the remaining per-case checks in `LINK_LOADING_RETEST.md` into the
  configured or Play-distributed update without recording unobserved passes.

## Final signed bundle

Use the existing configured local release environment. The editing environment
and ordinary GitHub CI do not hold the upload keystore or production YouTube key.

1. Check that the local worktree is clean; synchronize canonical `main` with
   `git fetch origin`, `git switch main`, and `git pull --ff-only origin main`.
   Verify the exact merged SHA, package, versionCode 9 and versionName 0.1.8.
2. Preserve any previous signed AAB outside the build output, then perform a
   clean build with `gradlew.bat clean :app:playReleaseBundle`. The existing
   publish-config gate requires the production YouTube key and upload signing.
3. Verify the newly produced `app/build/outputs/bundle/release/app-release.aab`
   with `jarsigner -verify`, verify the registered upload certificate using
   `keytool -printcert -jarfile`, and check the manifest with the existing
   official bundletool. Expected package/version: the release identity above.
4. Record the new AAB SHA-256 and size, the source SHA and generated R8 mapping.
   Do not substitute the CI unsigned AAB or the separately packaged QA APK.

## Play submission

After signed-bundle verification, use the existing **Test chiusi - SocialViewer**
track and upload that exact version 9 AAB. Preserve the tester lists, countries,
app-signing identity and production track. Confirm Play recognizes **9 (0.1.8)**,
save the release notes below, review the changes, and submit the authorized
closed-test update. Record review/submission and tester availability separately.
When available, install through Play as an existing tester and check the final
Home arrangement and the outstanding configured-provider cases.

## Release notes — Italian

Migliorata la gestione del caricamento dei link e dei messaggi di errore. Aggiunto
un pulsante per cancellare il link nella Home, accanto a Incolla. Messaggi più
chiari per i link Pinterest che non rimandano a un singolo Pin.

## Release notes — English

Improved link loading and error messages. Added a button to clear the link on
Home, next to Paste. Clearer messages for Pinterest links that do not point to
an individual Pin.
