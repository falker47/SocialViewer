# Reddit first-visit diagnostics — debug prototype only

## Scope and current evidence

2026-10-15 owner feedback: remove the overlaid `Vai al contenuto principale` link and diagnose the first-entry blur before adding any tap hint. The link comes from Reddit, not SocialViewer's native controls. A second post opened without blur in the same installation. Exact modal, state key and cause remain unknown. Do not present an extra tap as the fix, remove blur globally, auto-dismiss a dialog or submit consent.

This note supersedes the original probe's no-DOM-mutation boundary only for the owner's explicitly requested skip-link treatment and bounded local layout inspection. The existing Reddit post/comments and consent/access controls are not removed or exported.

## What changed

The debug build now installs as **SocialViewer Test**, application ID `io.github.falker47.socialviewer.redditdiag`. It is separate from `io.github.falker47.socialviewer`, including app-scoped WebView data. The first installation of this ID starts with fresh local site data; an ordinary update/relaunch does not. A clean profile may reproduce a first-visit condition but does not guarantee a server-side experiment or the original bug will recur. Release identity, version, signing, Play track and opt-in are unchanged. The debug copy is not a substitute for Play testing. Existing package-restricted YouTube API keys may not authorize this diagnostic ID; do not change those keys for this Reddit probe.

The only page mutation hides an anchor with the exact normalized EN/IT skip-link label and a same-document fragment target, excluding post/comment/article content and leaving Original intact. It also checks open shadow roots and late insertions. The actual current Reddit DOM has not been available in the editing environment; this selector must be confirmed on the phone. A count of matches is included in the diagnostic report. Do not broaden selectors indiscriminately if it reports zero.

A first layout snapshot is taken before this treatment, then after it, at 600/2500/8000 milliseconds and around the first page taps. The native **Copia diagnosi / Copy diagnostics** button copies the bounded report. It does not dismiss the page or simulate interaction. It is a developer tool, not an end-user tap hint.

The report stays in the current WebView's memory until explicitly copied. It contains viewport, focus element kind, candidate modal/overlay geometry, display/visibility and blur measurements, plus build/WebView versions. It does not read cookies, storage, input values, request bodies or export post/comment text, HTML, raw IDs/classes or full URLs. It does not upload anything. Closed shadow roots and nodes beyond the bounds cannot be inspected; a snapshot is evidence, not an automatic diagnosis. In particular an initially off-screen dialog must be identified before changing its layout.

## One-pass owner procedure

1. In the existing SocialViewer project terminal: `.\sync-test-branch.ps1 spike/reddit-page-preview`. Sync Gradle and Run `app` with `debug` on the already connected physical phone.
2. Verify the newly opened app is **SocialViewer Test**. Do not uninstall the usual SocialViewer. Complete/skip its new-install onboarding, then paste the known AskReddit permalink in this test app. Do not change default App Links; use paste for this probe.
3. On this first opening, wait about eight seconds without tapping the web page. Whether blurred or clear, take a screenshot, tap the native **Copia diagnosi** control above the page, and paste that report into the chat. This copies the earlier automatic snapshots too.
4. Only after preserving that first report, reproduce the existing single tap on the blank page background when needed, then copy a second report. Compare actual geometry, focus and modal state rather than guessing from blur alone. No second install/build is needed for this before/after pair.

Test permalink:

```text
https://www.reddit.com/r/AskReddit/comments/53scc5/what_video_game_should_everybody_play_at_least/
```

## Repeating a fresh-local-state test

Do not reset the existing/Play app. A new version number alone will not reset site data. Once a first report has been saved, the owner can reset **only SocialViewer Test** in Android app information > Storage and cache > Clear storage, verifying its separate diagnostic identity first. This deletes all data of the disposable test copy, not the ordinary app or browser. Reopen the test copy and repeat. This is a manual explicit test action, not automatic deletion, and no reset has been executed from the chat.

If deeper inspection is necessary, the debug-only WebView explicitly enables inspection on a debuggable app. With the phone connected and the Reddit page open, Chrome on the PC > `chrome://inspect/#devices` > `WebView in io.github.falker47.socialviewer.redditdiag` > inspect. Start with Elements and the dialog/overlay in the saved report. Do not export Network HAR files containing cookies or clear all browser/app data.

## Verification

The browser fixture test uses real local Chromium DOM/layout with a supplied test location (network navigation is unavailable in the editing runtime). It checks targeted removal, open shadow roots/late insertion, preservation of blur/dialogs and real click behavior, bounded layout-only reports and origin refusal. RED: before the adapter the visible link fails the removal assertion; GREEN: all six fixture tests pass. These are synthetic fixtures, not a claim that the real Reddit first-visit bug was reproduced.

Android tests cover report parsing and separate debug/release identities; full debug/release compilation, unit tests, lint and packaging must pass in this PR's CI before delivery. Real phone verification and diagnosis remain pending. No merge/publication is authorized by this diagnostic iteration.
