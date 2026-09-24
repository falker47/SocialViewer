# Social Viewer — device smoke test

This checklist is the gate for changes that touch UI, intents, WebView behavior, direct-link handling, or providers.

## 1. Sync the Facebook branch

From the repository root in PowerShell:

```powershell
.\sync-test-branch.ps1 feature/facebook-provider
```

Open the repository root in Android Studio, sync Gradle if required, then run on an emulator or physical Android device. For Facebook provider verification, use a physical device at least once before merge if available.

## 2. Automated checks

From Android Studio's Terminal on Windows:

```text
gradlew.bat testDebugUnitTest
gradlew.bat assembleDebug
```

Expected result: `BUILD SUCCESSFUL`.

The same two checks must be green in GitHub Actions on the Facebook PR before the manual gate is considered ready.

## 3. Facebook public post

Use one known-public URL shaped like:

`https://www.facebook.com/{owner}/posts/{post-id}/`

Open it through manual paste.

Expected:

1. Social Viewer selects Facebook.
2. Loading appears.
3. Meta oEmbed resolves the item.
4. The official Facebook embed renders inside the existing WebView.
5. Tapping Facebook-native links/actions must not turn the WebView into a general Facebook browser.
6. **Originale** may open the canonical Facebook URL externally.

Facebook post direct-link routing is intentionally not declared in this milestone; paste and Share are the supported entry paths.

## 4. Facebook public Reel

Use one known-public URL shaped like:

`https://www.facebook.com/reel/{reel-id}/`

Verify manual paste first.

Expected: the official Facebook Reel embed renders and playback remains user-initiated where the provider requires it.

Then test the Android direct-link path separately after section 8.

## 5. Facebook unavailable / private / removed

Try a known private, removed, restricted, embedding-disabled, or otherwise unavailable Facebook item.

Expected user-facing result:

`Questo contenuto Facebook non è disponibile.`

Normal expected provider HTTP details must not be shown to the user. A genuinely unexpected failure must still use the generic unexpected-error path rather than being mislabeled as normal unavailability.

## 6. Consent / login / repeated viewing

While testing Facebook, note any provider-controlled cookie consent or login UI.

1. Do not sign in merely to make an unavailable item work.
2. Open a second public Facebook item.
3. Confirm Social Viewer itself does not add a login/account flow.
4. Confirm first-party provider preferences can persist between views.
5. Verify third-party cookies remain blocked.
6. Confirm repeated viewing does not create Social Viewer history/storage beyond existing provider site data.

Provider-owned consent/login surfaces are observations for the smoke gate, not reasons to bypass platform restrictions.

## 7. Site-data behavior

Open **Impostazioni → Cancella dati del sito**.

1. Confirm the warning remains provider-generic.
2. Clear site data.
3. Verify the snackbar `Dati dei provider cancellati`.
4. Open Facebook again; provider consent/preferences may legitimately reappear.
5. Re-test one TikTok and one Instagram item afterward.

## 8. Facebook direct-link handling

Open **Impostazioni → Apertura diretta → Configura**.

On Android's **Open by default** screen, configure Facebook domains if Android offers them. A first-party Facebook app or browser may already own the domain; that platform behavior is not a Social Viewer playback defect.

After returning:

- Settings must include **Facebook** in the same compact provider breakdown used for TikTok and Instagram.
- The state must reflect Android's real **Attiva / Parziale / Da configurare** status.
- No separate Facebook toggle or custom configuration surface should exist.

Test a public Facebook **Reel** link from WhatsApp or a browser. When Android is associated with Social Viewer for Facebook, it should route directly into resolve/render without first showing Home.

Do **not** treat lack of direct routing for `/{owner}/posts/{id}` as a bug in this milestone: those post paths are intentionally not claimed because the minSdk-26 manifest matcher cannot constrain them without overclaiming unrelated Facebook paths. Manual paste and Share are the fallback for posts.

## 9. Manual paste and Share fallback

Verify:

- Facebook public post via manual paste;
- Facebook public Reel via manual paste;
- Facebook public post or Reel shared as text through Android Share → Social Viewer.

All supported cases should resolve to Facebook through the existing provider boundary.

## 10. Shared UI / onboarding

Clear Social Viewer app data and launch again.

1. Home remains compact and provider-generic.
2. Coach mark 1 explains a supported public link without hardcoding a provider list.
3. Coach mark 2 explains direct opening without hardcoding a provider list.
4. The configuration CTA still opens Android's real **Open by default** screen.
5. Settings lists TikTok, Instagram, and Facebook under the existing compact **Apertura diretta** section.
6. No new Facebook-specific settings area or tutorial appears.

## 11. Appearance regression

Exercise **Sistema / Chiaro / Scuro**.

Inspect:

- Home;
- Settings;
- onboarding coach marks;
- Loading;
- Error;
- player chrome around Facebook;
- clear-site-data dialog/snackbar.

No Social Viewer surface should regress visually. Remote provider embeds are not recolored by Social Viewer.

## 12. TikTok regression

Open at least one known-public canonical TikTok video. If convenient also verify one `vm.tiktok.com` or `vt.tiktok.com` short link.

Expected: existing TikTok resolution/playback remains unchanged.

## 13. Instagram regression

Open:

- one public Instagram `/p/` post;
- one public Instagram `/reel/` Reel.

Expected: existing Instagram rendering remains unchanged, including clean unavailable copy for an unavailable Instagram item.

## 14. Navigation boundary

For Facebook, Instagram, and TikTok, interact with any provider-native links visible inside the embed.

Expected: main-frame navigation remains blocked inside Social Viewer. The embedded surface must not become a general-purpose Facebook/Instagram/TikTok browser.

## 15. Negative Facebook URL policy

Manual input must reject as unsupported:

- `http://www.facebook.com/alice/posts/123/`;
- Facebook profile-only URLs;
- Facebook Stories;
- Facebook group-post paths;
- Facebook `/share/...` aliases not explicitly supported;
- lookalike hosts such as `fakefacebook.com`.

No scraping or redirect guessing should be introduced to make those forms work.

## 16. What to capture if something fails

Send:

- screenshot of the app/error;
- exact public test URL when relevant;
- Android version and phone/emulator model;
- whether the Facebook first-party app is installed;
- what Android shows under **Open by default** for Social Viewer/Facebook;
- Android Studio **Build** error text for build/sync failures;
- Logcat filtered by package `io.github.falker47.socialviewer` for runtime crashes.

Do not include unrelated device/account logs.

Record **PASS** only if Facebook post + Reel rendering, expected-unavailable handling, site-data behavior, Facebook Reel direct link (where Android association permits it), System/Light/Dark, TikTok regression, Instagram post+Reel regression, Share/manual fallbacks, and the navigation boundary all behave as expected.
