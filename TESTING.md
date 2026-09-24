# Social Viewer — device smoke test

This checklist is the gate for changes that touch UI, intents, WebView behavior, direct-link handling, or providers.

Baseline note: TikTok playback plus the light Home/onboarding/Settings flow were verified during the 2026-09-23 MVP pass. Re-run the relevant sections after changes rather than assuming a previous pass covers new code.

## 1. Clone, open and sync

Clone `falker47/SocialViewer` and open the repository root (`SocialViewer`) in Android Studio, not the `app` subfolder.

Required toolchain:

- JDK 17 for Gradle
- Android SDK Platform 37
- Android SDK Build-Tools 36.0.0 or newer compatible tools
- Android SDK Platform-Tools

The project uses AGP 9.4.0 and the committed Gradle 9.6.0 wrapper.

Run **File > Sync Project with Gradle Files** if sync does not start automatically.

The legacy `bootstrap-gradle.*` scripts are not required for a normal clone because the wrapper is already committed.

## 2. Run automated checks

From Android Studio's Terminal on Windows:

`gradlew.bat testDebugUnitTest`

`gradlew.bat assembleDebug`

Expected result: `BUILD SUCCESSFUL`.

The same checks run in GitHub Actions for pushes and pull requests targeting `main`.

## 3. Launch on Android

Use either a physical phone or emulator. For provider/player verification, prefer a real device at least once before merging a provider or WebView change.

Expected Home:

- `Social Viewer`
- `Apri un contenuto`
- TikTok URL field with trailing clipboard icon
- `Apri`
- `Apertura diretta`

## 4. Fresh-install onboarding

Clear app data before this test.

1. Coach mark 1 highlights the manual-open controls.
2. Its card stays visibly above the Android navigation area in both gesture navigation and classic three-button navigation.
3. `Avanti` moves to step 2.
4. Coach mark 2 highlights `Apertura diretta` and explains that a TikTok link tapped in WhatsApp or the browser can open directly in Social Viewer.
5. `Non ora` completes onboarding without blocking the app.
6. Relaunch: coach marks must not return.
7. Repeat with fresh app data and choose `Configura apertura diretta`; Android's **Open by default** screen must open.

## 5. Manual URL flows

With one known-public TikTok URL:

- clipboard icon → paste + validate + open immediately;
- populated field + `Apri` → validate + open;
- empty field + clipboard populated + `Apri` → read clipboard + open.

Invalid clipboard/input must remain on Home and show inline feedback.

## 6. TikTok playback

Test at least:

- canonical `https://www.tiktok.com/@.../video/...`
- `https://vm.tiktok.com/...`
- `https://vt.tiktok.com/...`

Expected path:

1. short link resolves when necessary;
2. loading surface appears;
3. TikTok oEmbed confirms public availability/metadata;
4. the dedicated TikTok `/player/v1/{post_id}` player appears;
5. playback works after the user's play gesture;
6. no raw/intermediate TikTok layout flashes before the player is ready.

Do not use a private, removed, login-only, or age-gated item as the first smoke test.

## 7. Cookie/site-data behavior

1. Make a TikTok cookie/consent choice if prompted.
2. Open another TikTok: the same choice should not be requested again merely because the previous player was disposed.
3. Open Settings → **Cancella dati del sito**.
4. Confirm the warning.
5. Verify the snackbar `Dati TikTok cancellati`.
6. The next TikTok playback may legitimately ask for provider consent/preferences again.

## 8. Direct-link handling

Inside Social Viewer tap `Configura`.

On Android's **Open by default** screen enable supported-link handling and select the TikTok domains Android offers for the app.

After returning:

- Social Viewer reads the real Android domain state;
- `Apertura diretta attiva` appears only when all currently declared TikTok hosts are approved;
- the first transition to active shows `Apertura diretta attivata`.

Then tap a TikTok link from WhatsApp or a browser.

Expected: Social Viewer opens directly and starts resolving/rendering without first flashing the Home screen.

## 9. Share-sheet fallback

Share a public TikTok URL as text from another app and choose **Social Viewer**.

Expected: the app starts resolving the URL. This path is supported but intentionally not promoted in the primary UI.

## 10. Appearance / dark mode

Open Settings → **Aspetto** and verify:

1. On a fresh install, **Sistema** is selected by default.
2. With **Sistema**, switch Android between light and dark theme; Social Viewer must follow the system theme.
3. Select **Chiaro** while Android is dark; the Social Viewer chrome must remain light.
4. Select **Scuro** while Android is light; the Social Viewer chrome must remain dark.
5. Fully close and relaunch the app; the selected mode must persist.
6. In dark mode inspect Home, top bar, URL input, direct-link card, Settings, the clear-site-data dialog, snackbar, Loading, Error, and the chrome above the player. No Social Viewer surface should remain accidentally light.
7. Verify both coach marks remain readable against the scrim in dark mode. A fresh app-data run with Android dark + default **Sistema** is sufficient.
8. Open a TikTok and confirm the remote player itself is not recolored; its black player background remains unchanged.
9. Re-run clipboard paste/open and direct-link configuration to confirm no regression in those flows.

## 11. Negative tests

Confirm clean failure for:

- `http://www.tiktok.com/...`
- `https://example.com/...`
- malformed text with no URL
- a TikTok URL without a supported post/photo ID
- a short TikTok URL that redirects outside TikTok, when a safe fixture is available

## 12. What to capture if something fails

Send:

- screenshot of the app/error;
- the exact public test URL when relevant;
- Android version and phone/emulator model;
- Android Studio **Build** error text for build/sync failures;
- Logcat filtered by package `io.github.falker47.socialviewer` for runtime crashes.

Do not include unrelated device/account logs.

## Instagram provider smoke test

Run this only after CI is green on `feature/instagram-provider`.

1. Sync the branch with:
   ```powershell
   .\sync-test-branch.ps1 feature/instagram-provider
   ```
2. Run the app from Android Studio on an emulator or physical device.
3. Paste and open one public Instagram image/carousel post URL in the form `https://www.instagram.com/p/{shortcode}/`.
4. Paste and open one public Instagram Reel URL in the form `https://www.instagram.com/reel/{shortcode}/`.
5. Confirm the official Instagram embed renders inside Social Viewer and that tapping links inside the embed does not turn the WebView into an Instagram browsing surface.
6. Try an unavailable/private/restricted Instagram item and confirm Social Viewer reaches its normal unavailable-content error state rather than exposing scraped media.
7. Re-open a second Instagram item and note whether provider cookie/consent UI repeats unexpectedly.
8. In Settings, verify that clearing provider site data honestly clears the shared WebView site data and that subsequent provider consent may reappear.
9. Regression-check one canonical public TikTok video and confirm playback still works.
10. Direct opening of Instagram links is intentionally out of scope for this branch; test Instagram through manual paste or Android Share. TikTok direct-link behavior must remain unchanged.

Record PASS only if steps 3-9 behave as expected. If Instagram rendering requires third-party cookies, treat that as a finding to investigate rather than enabling them broadly without evidence.

