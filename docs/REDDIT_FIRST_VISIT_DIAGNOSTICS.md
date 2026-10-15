# Reddit first-visit diagnostics — debug prototype only

## Current checkpoint — probeRevision 2 (2026-10-15)

The owner's before/after reports from `8602cd4` share the same initial timeline. Before the page tap (7932 ms), an `rpl-dialog-sheet` categorized as dialog/sheet/consent has `open=true`; after the tap it has `open=false` (8066/8114 ms), and focus moves to a dialog/overlay, then body. The owner reports the blur disappearing. The skip-link match count remains 1 through the transition. This directly supports a panel-dismissal mechanism rather than a required post-loading tap. It does NOT establish the exact panel content, consent choice, persistence key, or why the panel body was not visible. The category `consent` comes from a substring match on tag/ID/class; it is not a captured consent dialog title or proof of a cookie-specific prompt.

Sampling defect found and reproduced locally: the initial probe kept only 12 of 25 candidates while `truncated=false` referred only to the DOM scan. Its ranking let zero-box controllers crowd out actual sheet renderers. It also did not sample blur on generic hit-tested elements' `::before`/`::after`. Empty computed styles/zero host boxes, or a zero-height card at viewport+16px, do not by themselves prove that a rendered panel was collapsed/offscreen. In particular `display:contents` does not generate its own layout box.

Revision 2 fixes the diagnostic, NOT Reddit's appearance: prioritize actual sheet renderers, correlate elements with ephemeral probe-local refs (not website IDs), inspect the hit-tested path and computed blur pseudo-styles, distinguish style availability/own boxes, and disclose omitted candidates/root/scan/history bounds. It keeps the 48k native copy limit and the existing schema, adding `probeRevision:2`. Pseudo-style values alone do not prove a pseudo-element was painted. The skip-link treatment and all other website state/controls remain unchanged. No cookie/storage/input/content export, network capture, tap hint, synthetic tap or automatic dismissal is added.

Verification: the original six Chromium fixture tests passed on the exact baseline; five new sampling assertions failed before the change; all eleven then passed. Fixtures reproduce measurement failures only, not the live Reddit bug. Android CI must still pass for this commit before delivery. No independent reviewer is claimed.

Next phone pass: update the existing test copy, not a new package. Its update preserves data. The first before/after evidence has already been supplied. To retry a clean local first visit, manually clear storage ONLY for **SocialViewer Test** (`io.github.falker47.socialviewer.redditdiag`) in Android app information, then reopen it and paste the test permalink. Never clear/uninstall the ordinary SocialViewer or browser. This removes the disposable test copy's settings/site data; no reset is executed from this chat and recurrence is not guaranteed. If blurred, preserve a screenshot and a report before any page tap; then repeat the observed background tap once and copy the after report. A final report contains the earlier automatic snapshots too. Do not repeatedly reset if the condition does not recur. The report must contain `probeRevision:2`.

## Scope and earlier evidence

2026-10-15 owner feedback: remove the overlaid `Vai al contenuto principale` link and diagnose the first-entry blur before adding any tap hint. The link comes from Reddit, not SocialViewer's native controls. A second post opened without blur in the same installation. Exact modal, state key and cause remain unknown. Do not present an extra tap as the fix, remove blur globally, auto-dismiss a dialog or submit consent.

This note supersedes the original probe's no-DOM-mutation boundary only for the owner's explicitly requested skip-link treatment and bounded local layout inspection. The existing Reddit post/comments and consent/access controls are not removed or exported.

## What changed in the first diagnostic build

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

## Verification of the original probe

The browser fixture test uses real local Chromium DOM/layout with a supplied test location (network navigation is unavailable in the editing runtime). It checks targeted removal, open shadow roots/late insertion, preservation of blur/dialogs and real click behavior, bounded layout-only reports and origin refusal. RED: before the adapter the visible link fails the removal assertion; GREEN: all six fixture tests pass. These are synthetic fixtures, not a claim that the real Reddit first-visit bug was reproduced. See the revision-2 checkpoint above for the expanded eleven-test suite.

Android tests cover report parsing and separate debug/release identities; full debug/release compilation, unit tests, lint and packaging must pass in this PR's CI before delivery. Real phone verification and diagnosis remain pending. No merge/publication is authorized by this diagnostic iteration.
