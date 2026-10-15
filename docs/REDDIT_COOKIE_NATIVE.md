# Reddit: native cookie preferences handoff (debug prototype, 2026-10-15)

## Evidence and why the earlier CSS experiment failed acceptance
On a real Android phone the first Reddit post is blurred in portrait. Rotating the same page to landscape exposes Reddit's cookie-preferences dialog with “Rifiuta i cookie opzionali” and “Accetta tutti”. Reports also show the original Reddit dialog was open prior to a background tap and closed afterward. The debug-only CSS positioning experiment (CI #205) compiled but **did not fix the real portrait behavior**. It is no longer invoked; no further CSS guesses should be stacked onto it.

A community report about another Reddit Android client documents the same invisible-portrait cookie dialog and Reddit's direct DOM buttons. That author used an automatic click; **SocialViewer does not**: https://www.reddit.com/r/Infinity_For_Reddit/comments/1sga4hr/v815_released_fixed_login_issues/

## Current bounded workaround (debug only)
When the genuine Reddit HTTPS cookie dialog \`#data-protection-consent-dialog\` is open **in portrait**, the WebView adapter checks that both recognizable Italian/English Reddit buttons exist. It reports their presence to the native screen. **No choice is submitted without a user tap.**

A native card then presents two equivalent options: **Reject optional cookies** and **Accept all**. When (and only when) the user taps one, \`evaluateJavascript\` relays the choice to the corresponding *existing Reddit button* and waits to see whether Reddit closes its dialog. No cookie values, storage contents, post/comment text, HTML, or account credentials are extracted. Reddit itself remains responsible for consent persistence and notices. Failure is shown rather than pretending the decision succeeded.

This is not a clean native Reddit client, API access bypass or automatic consent. The old “Vai al contenuto principale” visual overlap remains suppressed narrowly by the existing debug-only adapter. Release code and the Play identity remain unchanged.

The old \`reddit-consent-fit.js\` asset is retired and should not be used; the current adapter is \`reddit-consent-choice.js\`. Only the first-visit Reddit dialog is touched; all other providers are unchanged.

## Verification gate
Synthetic Chromium tests verify that opening the Reddit dialog in portrait **does not** choose a setting, that each of the two simulated native actions presses only the matching Reddit button, that unrelated dialogs remain untouched, and that the controls work in both locales and an open shadow root. Android unit and build checks are owned by PR CI. Neither can prove Reddit's live DOM and the real-phone experience.

To verify on the already-connected phone:
1. Sync branch \`spike/reddit-page-preview\`, use Android Studio **debug** and Run the separate **SocialViewer Test**.
2. If it already remembers a cookie decision, manually clear data **only for SocialViewer Test**, never the standard/Play SocialViewer or Chrome.
3. Open the known AskReddit post in portrait. Expected: a *native* Reddit cookie preference card with two options, while Reddit's own webpage may remain blurred behind it.
4. Select your preference, ideally optional-cookie rejection if desired. Expect the card and Reddit blur to disappear and the post/comments to remain scrollable inside SocialViewer. Check “Originale” continues to be an optional external action.
5. If the native card doesn't appear, or the webpage stays blurred after the chosen option, send one screenshot; do not repeat the prior multi-snapshot diagnostics. The current change must not be merged if this fails.

**Keep PR #28 Draft until explicit device acceptance.** No Google Play changes, release bundle signing or merges are authorized by this diagnostic workaround.
