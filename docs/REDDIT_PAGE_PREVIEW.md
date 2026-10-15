# Reddit single-page preview — experimental, debug only

## Approved question — 2026-10-15

The owner rejected the Custom Tab browser handoff in PR #27 and approved trying Reddit's full post page as the **only content view inside SocialViewer**, rather than duplicating it below an embed. The experiment asks whether the public post and expandable replies are usable without sign-in while retaining the SocialViewer shell. This is not approval to ship or merge a new Reddit client.

## What this branch does

- Start from main, not the rejected Custom Tab branch. There is no new Comments button and no Custom Tabs dependency.
- In debug builds only, reuse the existing canonical post/comment and /s/ alias resolver, then load that canonical page directly in one internal WebView. The supported preview path does not first fetch oEmbed.
- Retain the existing native back and Original controls. Original remains a deliberate optional escape; the normal post/comments path never starts an external activity. Native Back retains the current app behavior and exits the item; it is not a browser-history feature.
- The small `Preview · Reddit page` / `Anteprima · Pagina Reddit` label identifies the experiment. Reload recreates the page in place. Page loading, network errors, TLS rejection, HTTP status and renderer loss are surfaced without attributing their cause to an emulator or hiding a provider security page.
- Keep non-Reddit embeds unchanged. The existing `SocialContent` carrier is reused temporarily: empty markup + a validated full Reddit permalink in documentBaseUrl. Only the debug bridge recognizes this combination. This is a throwaway probe contract, not a proposed permanent renderer abstraction.
- All experimental renderer/policy/resources live in src/debug; release has only two no-op bridge functions. Release still uses the original Reddit oEmbed path. Before any production adoption, choose an explicit typed presentation contract and review the product/privacy implications separately.

## Privacy and boundaries

This is Reddit's live page, not a clean native comment list. Its header, advertising, consent, recommendations, sign-in prompts and other provider-controlled UI can appear. No CSS/DOM removal, content extraction, scraping, API key, app-side Reddit API/OAuth, proxy, server, paid service, identity spoofing or automatic anti-bot workaround is added. The page may internally call Reddit services as any normal web page does.

The WebView uses app-scoped provider site data, not the external browser session. First-party site data/consent can persist like the existing app WebViews and remains subject to the app's global Clear provider site data control. It is not an incognito/zero-tracking promise. Third-party cookies, file/content access and mixed content remain blocked; Safe Browsing stays on; TLS errors are cancelled; page permissions and popup windows are rejected. No app-side comments database/history is created.

Top-level navigation is restricted to the same subreddit/post identity, allowing reply permalinks and sorting/query changes. Main-frame POST, other posts/profiles/feeds, login routes, external URLs and custom app schemes are rejected. Ordinary out-of-scope SPA history changes stop/cover the preview. This is not a complete DOM or subresource sandbox: Reddit's in-page controls and client-side behavior remain provider-owned. Any observed route around the intended single-item boundary is a failed acceptance test, not permission to broaden it silently.

A network-security block in the previous emulator Custom Tab does not identify the root cause. This WebView is not guaranteed to cure it. Do not change user agents, use proxy services, repeatedly retry, or require login merely to make a test look green.

## Verification and acceptance

Automated gates: existing full debug tests; new URL/navigation, direct-page/no-oEmbed, request/error, cookie/settings and resource tests; debug lint; release tests proving the experiment is absent; existing release lint, debug assembly and unsigned release AAB packaging. No signed build, version bump or Play change belongs to this experiment.

Local pure-Kotlin policy suites were written first: the absent behavior failed, then all six suites passed after implementation. Android framework/Compose tests require GitHub CI or Android Studio; the editing runtime has no Android SDK and cannot validate live Reddit rendering.

### Test after green CI

Use the already-working SocialViewer project and emulator. Sync the new branch (do not reuse feature/reddit-discussion):

```powershell
.\sync-test-branch.ps1 spike/reddit-page-preview
```

In Android Studio use Sync Project with Gradle Files / Sync Now, keep the **debug** build variant, then Run `app` on the existing emulator/development device. Do not uninstall a Play-signed app or change tester opt-in to work around a debug-signature conflict.

Paste in SocialViewer:

```text
https://www.reddit.com/r/AskReddit/comments/53scc5/what_video_game_should_everybody_play_at_least/
```

Expected: the native SocialViewer header stays visible, the preview label appears, and a **single Reddit page** fills the content area. There is no duplicate post card or Comments-on-Reddit button. Scroll in that page and expand Show more replies without sign-in. Check native Back, reload, portrait/landscape, EN/IT and light/dark app chrome; Reddit itself controls its own theme. Check one real /s/ link and one explicit comment, plus a non-Reddit provider regression. Check that profiles, unrelated posts and Open app do not silently turn the preview into browsing or launch another app.

If the same network-security page appears, keep the screenshot and stop that test as BLOCKED. The displayed HTTP status (when Reddit supplies one) is evidence, not proof of why access was refused. A later controlled comparison on another development device/network is a separate diagnostic, not automatic bypass authorization.

If the page remains visible but the appearance or provider UI is unacceptable, record product rejection rather than continuing release plumbing. Media/fullscreen, all provider regressions and real-device reliability are not claimed from the initial text-post probe.

## Decision gate

Keep the PR Draft and unmerged. Main, rejected PR #27, release/signing identity, Play tracks and tester enrollment remain unchanged. A successful browser test or CI run alone does not pass in-app product acceptance.

## Primary references checked for implementation

- https://developer.android.com/develop/ui/views/layout/webapps/webview
- https://developer.android.com/reference/android/webkit/WebViewClient
- https://developer.android.com/build/build-variants
