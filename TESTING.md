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
- provider-generic URL field with trailing clipboard icon
- `Apri`
- `Apertura diretta`

## 4. Fresh-install onboarding

Clear app data before this test.

1. Coach mark 1 highlights the manual-open controls.
2. Its card stays visibly above the Android navigation area in both gesture navigation and classic three-button navigation.
3. `Avanti` moves to step 2.
4. Coach mark 2 highlights `Apertura diretta` and explains generically that supported links can open directly in Social Viewer after Android configuration.
5. `Non ora` completes onboarding without blocking the app.
6. Relaunch: coach marks must not return.
7. Repeat with fresh app data and choose `Configura apertura diretta`; Android's **Open by default** screen must open.

## 5. Manual URL flows

With one known-public TikTok URL, then repeat with one known-public Instagram post/Reel URL:

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
5. Verify the snackbar `Dati dei provider cancellati`.
6. The next TikTok playback may legitimately ask for provider consent/preferences again.

## 8. Direct-link handling

Inside Social Viewer tap `Configura`.

On Android's **Open by default** screen enable supported-link handling and select the TikTok, Instagram and Bluesky domains Android offers for the app.

After returning:

- Social Viewer reads the real Android domain state;
- Settings shows one compact **Apertura diretta** section with separate provider state, including Bluesky;
- a provider may show **Parziale** when only some of its declared hosts are selected;
- `Apertura diretta attiva` appears on Home only when all declared provider hosts are approved;
- the first transition to fully active shows `Apertura diretta attivata`.

Then tap one supported TikTok link, one supported Instagram post/Reel link and one Bluesky post permalink from WhatsApp or a browser. For Bluesky, also tap a plain `https://bsky.app/profile/<handle>` URL and confirm Social Viewer does not intercept the profile.

Expected: when Android is associated with Social Viewer for that domain, Social Viewer opens directly and starts resolving/rendering without first flashing Home. If a first-party app/browser owns the domain instead, change the association in Android rather than treating Social Viewer as a silent default.

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

## Multi-provider UI + direct-link smoke test

Run this only after CI is green on `feature/multi-provider-ui`.

1. Sync the branch:
   ```powershell
   .\sync-test-branch.ps1 feature/multi-provider-ui
   ```
2. Run the app from Android Studio on an emulator or physical device.
3. Home: confirm the field says `Incolla un link` and shared copy no longer assumes TikTok is the only provider.
4. Manual playback: open one known-public TikTok item, one public Instagram post and one public Instagram Reel. All must render as before.
5. Instagram unavailable/private: confirm the user-facing error is `Questo contenuto Instagram non è disponibile.` and does not expose the expected HTTP 400 detail. Unexpected provider failures must still use the generic error title rather than being mislabeled as normal unavailability.
6. Direct-link settings: open **Impostazioni → Apertura diretta → Configura**. Confirm Android's real **Open by default** screen opens.
7. Select/approve TikTok and Instagram domains where Android allows it. Return to Social Viewer and confirm the provider breakdown reflects the real state (**Attiva / Parziale / Da configurare**) rather than a fake toggle.
8. Direct TikTok: tap a supported TikTok link from another app/browser and confirm Social Viewer resolves it directly when Android is associated with Social Viewer for that domain.
9. Direct Instagram: tap a supported `/p/` or `/reel/` link from another app/browser and confirm Social Viewer resolves it directly when Android is associated with Social Viewer for that domain.
10. Fallbacks: manual paste still works for both providers; Android Share-to-Social-Viewer still resolves a supported shared link.
11. Fresh-install onboarding: both coach marks remain the same two-step flow; their copy mentions TikTok + Instagram and the second CTA opens Android's real direct-link settings.
12. Appearance regression: exercise **Sistema / Chiaro / Scuro** and inspect Home, Settings, onboarding, loading/error chrome and player surroundings.
13. Site-data regression: **Cancella dati del sito** remains provider-generic and may cause either provider to ask for its preferences again.
14. Re-run at least one TikTok video after Instagram/direct-link testing and one Instagram item after TikTok/direct-link testing to catch cross-provider regressions.

Record PASS only if the branch builds, both providers retain playback, the direct-link states match Android's actual settings, and the Instagram unavailable path is clean.


## Threads provider smoke test

Run this only after CI is green on `feature/threads-provider`.

1. Sync the branch:
   ```powershell
   .\sync-test-branch.ps1 feature/threads-provider
   ```
2. Build/run on the physical phone used for the existing provider gates.
3. Public permalink: open one known-public `https://www.threads.com/@<username>/post/<shortcode>/`. It must render a single Threads post without requiring a Social Viewer account or a Threads login.
4. Shorthand: open one real public `https://www.threads.com/t/<shortcode>/`. It must render through the same provider path.
5. Legacy compatibility: if you have a real `threads.net` post or `/t/` URL, open it and confirm it normalizes to the current Threads integration rather than being rejected as an unsupported provider.
6. Unavailable/private/removed: test one known unavailable item. Expected user-facing result is `Questo contenuto Threads non è disponibile.`; no raw expected HTTP 400/404 detail should be shown.
7. Cookie/login surface: note whether the official embed shows any provider-controlled consent or login UI. Do not sign in. Rendering of the public test post must not depend on a Threads account.
8. Repeated viewing/site data: open two public Threads posts in sequence, return Home, then reopen one. Confirm there is no Social Viewer history/feed surface and no unnecessary repeated consent caused by WebView disposal.
9. Clear site data: Settings → **Cancella dati del sito** → confirm. Reopen Threads; provider preferences may legitimately be requested again.
10. Direct link: in Android **Open by default**, associate Social Viewer with Threads domains where Android allows it. Tap a real Threads shorthand `/t/` link from another app/browser. It should open directly in Social Viewer. Canonical `/@user/post/...` is intentionally paste/Share-only on this Android baseline.
11. Settings: TikTok, Instagram and Threads are the active/configurable provider rows. Facebook is shown separately as **In pausa** and is not included in the active-provider count.
12. Main-frame boundary: tap author/profile or other navigational links inside the Threads embed. Social Viewer must not become a Threads browser; main-frame navigation must stay blocked.
13. Appearance: exercise **Sistema / Chiaro / Scuro** with a Threads item loaded and inspect Home, Settings, loading/error chrome and player surroundings.
14. TikTok regression: open one canonical public TikTok video and one vm/vt short link if convenient; playback must remain unchanged.
15. Instagram regression: open one public Instagram post and one public Reel; both must remain unchanged.
16. Record PASS only if Threads permalink + shorthand work, unavailable/privacy/navigation behavior matches the boundary, and TikTok + Instagram regressions pass.


## YouTube provider smoke test

Run this only after CI is green on `feature/youtube-provider`.

1. Configure a local YouTube Data API v3 key. Preferred local setup: add `YOUTUBE_API_KEY=<key>` to gitignored `local.properties`. Restrict the key to YouTube Data API v3; for Android restrictions use package `io.github.falker47.socialviewer` and the signing SHA-1.
2. Sync the branch:
   ```powershell
   .\sync-test-branch.ps1 feature/youtube-provider
   ```
3. Build/run on a physical Android phone.
4. Public watch URL: open one known-public `https://www.youtube.com/watch?v=<videoId>`. Expected: Data API preflight succeeds, then the privacy-enhanced player appears and playback starts only after user interaction.
5. Short URL: test `https://youtu.be/<videoId>`; it must normalize to the same canonical watch URL.
6. Shorts: test `https://www.youtube.com/shorts/<videoId>`; it must render as one video, never a Shorts feed.
7. Mobile URL: test `https://m.youtube.com/watch?v=<videoId>`.
8. Optional live alias: test one public `/live/<videoId>` if available. No live chat or browsing surface should be added.
9. Tracking parameters: repeat a shared URL containing `si`, `utm_*`, or other harmless query parameters. Video-ID extraction must remain stable.
10. Made For Kids gate: use a known MFK video. Expected: **Contenuto non supportato** with copy explaining that videos intended for children are not opened in Social Viewer; the YouTube IFrame must not be loaded. **Apri originale** remains available.
11. Unavailable/private/non-embeddable: confirm clean **Contenuto non disponibile** handling where the Data API exposes the restriction. Do not attempt to bypass login, age, region, Content ID, or uploader embedding restrictions.
12. Player surface: controls remain native; autoplay is off; provider-native related-video/advertising surfaces are accepted. Social Viewer must not overlay, hide, restyle, or intercept them.
13. Privacy-enhanced host: confirm player network/rendering uses `youtube-nocookie.com`. Note whether playback works with Social Viewer's existing third-party-cookie block; do not relax the cookie policy unless this fails repeatably.
14. Referrer/client identity: if the player reports error 153, capture Logcat and the exact URL. The WebView document base URL must provide a Referer; do not work around 153 by disabling identity requirements.
15. Routing: YouTube is intentionally **manual paste / Android Share only** in this milestone. Do not expect YouTube links to appear under Android Open by default for Social Viewer.
16. Navigation boundary: YouTube logo/channel/related actions must not turn the app shell into an unrestricted YouTube browser. Provider-native behavior inside the official player is allowed.
17. Appearance: check System / Light / Dark around the player; the player itself must not be recolored.
18. Regression: re-run one TikTok canonical item, one Instagram post + Reel, and one Threads permalink or /t/ item. Their provider code and behavior must remain unchanged.
19. Record PASS only if the Data API preflight, public watch/short/Shorts playback, MFK fail-closed behavior, privacy-enhanced player, and existing-provider regressions all pass.


## Reddit provider smoke test

Physical-device gate completed successfully on 2026-09-25 for `feature/reddit-provider`.

Verified:

1. Public Reddit post permalink renders as one Reddit item through the official oEmbed/Embeds path.
2. A public single-comment permalink renders as that explicitly linked comment; Social Viewer does not open a comment tree.
3. A real Reddit `/s/` share alias resolves to a supported canonical HTTPS Reddit permalink and then renders normally.
4. Removed/unavailable behavior remains bounded and does not turn Social Viewer into an unrestricted Reddit browser.
5. System / Light / Dark rendering is acceptable around the Reddit embed.
6. TikTok, Instagram, Threads and YouTube regressions pass.
7. Reddit remains manual-paste / Android-Share-only in this slice; it is intentionally absent from Android Open-by-default provider rows.
8. Settings copy distinguishes the number of providers with direct-link handling from the total number of supported providers.

Full Reddit comment-thread browsing is intentionally outside this gate. If implemented later, test it as a separate Data API feature with explicit OAuth/policy/rate-limit coverage rather than as an extension of the oEmbed smoke test.


## Pinterest provider smoke test

Physical-device gate completed successfully on 2026-09-25 for `feature/pinterest-provider`.

Verified:

1. Standard public numeric Pin renders through Pinterest's official Pin Widget.
2. Pinterest SEO and regional `*.pinterest.com` Pin URLs normalize to the canonical numeric `www.pinterest.com/pin/{id}/` form.
3. A real `pin.it` share alias that resolves through Pinterest's `/sent/` share path normalizes to the same single Pin and renders correctly.
4. A nonexistent Pin exits the loading state and shows `Questo Pin Pinterest non è disponibile.` rather than leaving an indefinite black surface.
5. Canonical Pinterest `/pin/` direct-link handling works through Android Open by default.
6. System / Light / Dark sanity passed around the Pinterest embed.
7. TikTok, Instagram, Threads, YouTube and Reddit regressions all passed after the Pinterest changes.
8. Board, profile, feed and arbitrary Pinterest navigation remain outside the provider boundary.
9. `pin.it` and regional hosts remain manual-paste / Android-Share-only; the manifest claims only canonical Pinterest hosts and `/pin/` paths.
10. The implementation has no Pinterest API key, OAuth, backend or paid/billing dependency.

### Known Reddit visual polish note

During the final Pinterest regression pass, Reddit occasionally exposed a brief fragment of pre-widget text immediately before the official Reddit card finished transforming. The post still rendered correctly, no functional regression was observed, and the Pinterest branch did not modify Reddit provider/WebView code. Track this separately as a non-blocking Reddit loading-transition polish item rather than reopening the Pinterest gate.


## X provider smoke test

Physical-device gate completed successfully on 2026-09-25 for `feature/x-provider`.

Verified:

1. A public canonical `x.com/{user}/status/{id}` permalink renders as one X post through the official X for Websites path.
2. A legacy `twitter.com/{user}/status/{id}` permalink normalizes to the same canonical X post and renders correctly.
3. Tracking/query parameters do not alter post identity after canonicalization.
4. A syntactically valid unavailable post exits loading cleanly; no indefinite black surface occurs.
5. No X login, OAuth, developer key, backend, paid API read or billing-dependent X API is required.
6. The first X load shows the privacy notice before any X network/embed load. After approval, the choice is stored locally and subsequent X links open directly without repeating the prompt.
7. Settings → Privacy and site data → **Revoca autorizzazione** clears the local X embed-consent decision; the next X load shows the notice again.
8. Third-party cookies remain blocked; `dnt=true`, `hide_thread=true` and `omit_script=true` are requested from X oEmbed.
9. X remains manual-paste / Android-Share-only in this slice; no X ACTION_VIEW filters are declared because the supported status route cannot be constrained safely enough on the minSdk-26 manifest matcher.
10. System / Light / Dark sanity passed.
11. TikTok, Instagram, Threads, YouTube, Reddit and Pinterest regressions all passed after the X changes.
12. Facebook PR #4 and the Reddit loading-transition polish item were not modified.


## Bluesky provider smoke test

Physical-device gate completed successfully on 2026-09-25 for `feature/bluesky-provider`.

Verified:

1. A public handle permalink `https://bsky.app/profile/{handle}/post/{rkey}` renders as one Bluesky post through the official `embed.bsky.app/oembed` path.
2. The equivalent DID permalink renders the same single post through the same provider path.
3. Harmless tracking query parameters do not alter post identity after canonicalization.
4. A syntactically valid unavailable post exits loading cleanly rather than leaving an indefinite black surface.
5. No Bluesky login, OAuth, API key, backend, scraping, remote browser or billing-dependent service is required.
6. Strict URL policy rejects profiles, feeds, extra path segments, HTTP URLs, lookalike domains, invalid handles/DIDs and invalid record keys.
7. User-managed Android direct-link handling is enabled for `bsky.app` single-post paths through `/profile/.*/post/.*`.
8. On a physical device, a real Bluesky post permalink opens directly in Social Viewer after enabling `bsky.app` under Open by default.
9. A plain Bluesky profile URL `https://bsky.app/profile/{handle}` is not intercepted by Social Viewer.
10. Third-party cookies remain blocked and main-frame navigation remains blocked.
11. TikTok, Instagram, Threads, YouTube, Reddit, Pinterest and X regressions all passed before the final direct-link patch; the direct-link patch is isolated to Bluesky manifest/state handling and passed CI plus the final physical direct-link/profile-non-interception check.
12. Facebook PR #4 and the Reddit loading-transition polish item were not modified.
