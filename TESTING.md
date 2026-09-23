# Social Viewer — first-device test

## 1. Bootstrap Gradle

Before the first Android Studio import, double-click `bootstrap-gradle.cmd` on Windows. It downloads the official Gradle 9.6.0 distribution, verifies its SHA-256 checksum, generates the standard wrapper, and copies only the wrapper files into the project.

After success the root folder will contain `gradlew`, `gradlew.bat`, and `gradle/wrapper/*`.

## 2. Open and sync

Open the project root (`social-viewer-mvp`) in Android Studio, not the `app` subfolder.

Required toolchain:

- JDK 17 for Gradle
- Android SDK Platform 37
- Android SDK Build-Tools 36.0.0 or newer compatible tools
- Android SDK Platform-Tools

The project uses AGP 9.4.0 and Gradle 9.6.0.

Run **File > Sync Project with Gradle Files** if sync does not start automatically.

## 3. Run unit tests

From Android Studio's Terminal on Windows:

`gradlew.bat testDebugUnitTest`

Expected result: `BUILD SUCCESSFUL`.

## 4. Connect a real Android phone

Enable Developer options and USB debugging, or pair with Wireless debugging on Android 11+.

Select the physical phone in Android Studio's target-device selector, then run the `app` configuration.

Expected first screen: `Social Viewer`, `Apri un contenuto`, a TikTok URL field with a clipboard icon, `Apri`, and the `Apertura diretta` card. On a fresh app-data install, the first coach mark should appear over the manual-open controls.

## 5. Onboarding smoke test

With fresh app data:

1. Coach mark 1 highlights the manual-open controls and `Avanti` moves to step 2.
2. Coach mark 2 highlights `Apertura diretta` and clearly mentions WhatsApp/browser links.
3. `Non ora` completes onboarding without blocking the app and the coach marks do not return on the next launch.
4. Repeat with fresh app data and choose `Configura apertura diretta`; Android's Open-by-default screen should open.

## 6. Manual URL smoke test

Test all three manual paths with one known-public TikTok video URL:

- put the URL in the clipboard and tap the trailing clipboard icon: it should paste and open immediately;
- type/paste the URL into the field and tap `Apri`;
- leave the field empty, put the URL in the clipboard, and tap `Apri`: it should read the clipboard and open immediately.

Also confirm an invalid clipboard value remains on Home and shows an inline validation error.

Expected path:

1. short URL resolves if necessary;
2. loading spinner appears;
3. TikTok oEmbed is fetched;
4. the embedded player appears;
5. video playback starts after the user's play gesture.

Test at least:

- canonical `https://www.tiktok.com/@.../video/...`
- `https://vm.tiktok.com/...`
- `https://vt.tiktok.com/...`

Do not use a private, removed, login-only, or age-gated item as the first smoke test.

## 7. Share-sheet fallback test

From WhatsApp, Chrome, or another app, share a public TikTok URL as text and choose **Social Viewer**.

Expected: Social Viewer opens directly and starts resolving the URL.

## 8. Direct-link test

Inside Social Viewer tap `Configura` in the `Apertura diretta` card or in Settings.

On Android's **Open by default** screen enable **Open supported links**, then enable the TikTok domains Android offers for this app.

Afterwards tap a TikTok link in WhatsApp. On Android 12+ an unverified web domain normally goes to the browser until the user explicitly approves the app for that domain.

Expected after approval: the matching TikTok link opens Social Viewer directly. After returning from Android settings, Social Viewer should show `Apertura diretta attiva` only when Android reports all declared TikTok hosts as approved; the one-time snackbar should say `Apertura diretta attivata`.

## 9. Settings / privacy smoke test

Open the gear icon on Home. Confirm:

- TikTok shows the actual `Attiva` / `Da configurare` state;
- `Configura` opens Android settings;
- `Cancella dati del sito` asks for confirmation;
- after confirming, a `Dati TikTok cancellati` snackbar appears;
- the next TikTok playback may ask again for provider cookie preferences.

## 10. Negative tests

Confirm that these fail cleanly rather than opening arbitrary content:

- `http://www.tiktok.com/...` (HTTP, not HTTPS)
- `https://example.com/...`
- malformed text with no URL
- a short TikTok URL that redirects outside TikTok (if such a test fixture is ever available)

## 11. What to capture if something fails

Send:

- screenshot of the app/error;
- the exact test URL (only if public);
- Android version and phone model;
- Android Studio **Build** error text if build/sync failed;
- for runtime crashes, Logcat filtered by package `io.github.falker47.socialviewer`.

Do not paste unrelated device/account logs.
