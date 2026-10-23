# Reddit discussion browser handoff

Approved scope (2026-10-14): add **Comments on Reddit / Commenti su Reddit** below the existing Reddit viewer. This is a user-initiated external browser handoff, not a native comments client.

## Behavior and boundaries

- Only a resolved Reddit post or explicit single-comment permalink offers the action. The provider's canonical item identity is reused; an explicit comment keeps its context. Incoming share aliases are still resolved by the existing provider before the action appears.
- Prefer a Custom Tab in a discovered browser, explicitly targeting its package to avoid sending the initial URL through SocialViewer's own App Links. Prefer the user's visible default browser when it supports Custom Tabs. If no service is available or launch fails, use a browser-only selector. Never fall back to an unrestricted ACTION_VIEW or an in-app Reddit WebView. If no browser can open it, show a localized message and keep the post open.
- The current post and existing embed stay in place while the browser is above the app. Closing the tab returns to that viewer under normal Activity lifecycle conditions. Process death is not a promise of persisted viewing history.
- A standard Custom Tab uses the browser's session and privacy settings; it is **not automatically private**. SocialViewer does not read browser cookies, credentials, comments or navigation. Clearing provider WebView data in SocialViewer does not clear browser data.
- Reddit's interface, advertising, sign-in prompts, moderation, age/access restrictions and browser navigation remain provider/browser-owned. Do not remove or bypass them. This external surface is not subject to the embedded player's single-item navigation restriction.
- No Reddit Data API, OAuth, JSON/RSS scraper, backend, resolver service, API key, billing, cache of comments, analytics or viewing history is added. No browser prefetch or request occurs before the user taps the action.
- Existing Original action, provider retrieval, WebView cookie/navigation policy, direct-link ownership, version/signing and Google Play tracks are unchanged.

## Verification

Automated: URL/provider gating; safe canonicalization; post and explicit-comment identity; Custom Tab package pinning; browser-only fallback; unavailable/security-failing browser handling; narrow Android package visibility; UI wiring and EN/IT resource parity. Run the existing full CI: `testDebugUnitTest`, `lintRelease`, `assembleDebug`, `bundleRelease` (unsigned packaging only).

Local research evidence: Mauri reported reading and expanding replies in the regular mobile browser without Reddit login on the test below. This is evidence for that discussion/browser only, not a guarantee for all Reddit content or for the Custom Tab implementation.

## Physical-device gate — pending, do not merge or publish yet

From the local repository, sync the feature branch:

```powershell
.\sync-test-branch.ps1 feature/reddit-discussion
```

Run the app from Android Studio on the existing development device or emulator. Do not uninstall or overwrite a Play-signed installation to resolve a signing conflict; use a separate test device/emulator in that case. Do not change the Closed Testing opt-in or tester account.

Test URL:

```text
https://www.reddit.com/r/AskReddit/comments/53scc5/what_video_game_should_everybody_play_at_least/
```

1. Open it in SocialViewer, including a normal direct-link launch with Reddit domains assigned to SocialViewer. Tap Comments on Reddit. Confirm the browser tab opens, not another SocialViewer activity and not the Reddit app.
2. Scroll to the discussion and expand Show more replies without signing in. The reply/composer invitation is distinct from reading and may be dismissed normally.
3. Close the tab with X/Back and confirm the same post is still displayed. Repeat once. Also test a real Reddit /s/ share link after its normal resolution, and an explicit comment permalink.
4. Check the button in Italian/English, light/dark, larger font and landscape. It must not overlap content or system navigation. Existing Original must still open in the browser. One non-Reddit provider must remain unchanged and show no new button.
5. On an emulator/device without a compatible Custom Tabs service, verify the browser-only fallback; a device with no available browser must show the localized error without crashing. Browser-dependent gates not exercised must stay explicitly unverified.

CI APKs are keyless debug builds: YouTube needs the established locally configured key/signing identity for its own playback test. No production API key or release keystore is supplied by this feature.

## Official references checked 2026-10-14

- https://developer.android.com/jetpack/androidx/releases/browser — AndroidX Browser 1.10.0 stable, minSdk 23 (project minSdk 26).
- https://developer.chrome.com/docs/android/custom-tabs/guide-get-started — browser-backed Custom Tabs and package visibility.
- https://developer.chrome.com/docs/android/custom-tabs/overview — browser ownership and session behavior.
