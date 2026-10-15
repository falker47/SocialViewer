# Reddit cookie consent — portrait WebView layout probe (debug only)

## Root-cause evidence and narrowly scoped change (2026-10-15)
On the physical phone, first-visit Reddit shows a blurred post in portrait. Rotating the same open page to landscape reveals Reddit's **"Conferma le tue preferenze sui cookie"** modal, including **"Rifiuta i cookie opzionali"** and **"Accetta tutti"**. This identifies the previously invisible panel as Reddit's cookie-preferences dialog, not an intentional SocialViewer tap-to-continue screen. Its precise broken CSS/animation rule is not yet independently identified. Prior reports showed an open \`rpl-dialog-sheet\` that closes on background tap.

A separate Reddit Android-client report documents the same portrait-only cookie dialog issue; its published workaround programmatically clicks a consent button. SocialViewer explicitly does **not** do that. Reference: https://www.reddit.com/r/Infinity_For_Reddit/comments/1sga4hr/v815_released_fixed_login_issues/

The debug-only \`reddit-consent-fit.js\` runs after page finished in the existing Reddit WebView. On an allowlisted Reddit HTTPS host, it locates **only** the exact \`#data-protection-consent-dialog\` and its \`rpl-modal-card\`, even within open shadow roots. Only while the dialog is open in portrait **and its card is out of the viewport or zero-sized**, it constrains/repositions that card within the WebView and enables vertical scrolling. The existing choices remain Reddit's real controls; no synthetic tap, accepting/refusing cookies, fake user agent, cookie read/write, login, backdrop removal or data extraction is implemented. Original styles are restored after close/rotation. A small poll handles delayed dialog insertion; cached host avoids repeated whole-page scans.

This is a layout hypothesis with passing synthetic-browser tests, **not proof that it fixes Reddit's current live DOM**. Reddit may change its dialog selector/structure. No release, main, other providers, Play enrollment or signing is changed.

## Acceptance test after CI green
1. Sync \`spike/reddit-page-preview\` and Run the **debug** variant on the already-connected physical phone. Confirm it opens **SocialViewer Test** (separate app, not the Play installation). No new package.
2. Because the test copy may remember a prior choice, clear storage **only** for **SocialViewer Test** in Android app settings if the first-visit prompt does not return. Never clear the ordinary SocialViewer or Chrome.
3. Paste the known AskReddit post into **SocialViewer Test**. In **portrait without rotating**, expect the Reddit cookie choices to be visible and tappable in the WebView. Choose personally, preferably **Rifiuta i cookie opzionali** if desired; the app must not choose for you.
4. Confirm blur disappears after the actual choice, comments are readable and expandable, the skip-to-content pill stays absent, and opening a second Reddit post does not show the dialog again. Check navigation remains within the app.
5. If the pane is still invisible, do **not** tap the background or clear data repeatedly; report a screenshot with exact behavior. Keep the PR Draft.

Synthetic local Chromium fixture tests use one portrait viewport with a zero-height/out-of-viewport dialog, late open-shadow insertion, closed-dialog/no-op, unrelated modal/no-op, rotation/style restoration, and preservation of user decision. The untouched site may still behave differently. CI must verify Android build and tests before device acceptance.
