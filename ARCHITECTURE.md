# Architecture decisions

## AD-001 — Provider boundary

Every social network implements `SocialProvider` and returns a normalized `SocialContent`. Platform-specific URL parsing, redirects, API/oEmbed calls, and metadata extraction stay inside that provider.

Reason: provider integrations are the volatile part of the system. The Android shell and player should remain stable.

## AD-002 — Official/public integration first

A provider should prefer documented public embed/oEmbed mechanisms. If a platform requires authentication or blocks public embedding for a piece of content, Social Viewer reports that limitation instead of scraping around it.

Threads uses Meta's current tokenless `https://graph.threads.com/oembed` endpoint for public post URLs. Reddit uses the official `https://www.reddit.com/oembed` surface for public post and single-comment permalinks; Reddit short/share aliases are followed only to recover a supported canonical HTTPS permalink. Pinterest uses the official Pin Widget (`data-pin-do="embedPin"` + `pinit.js`) for one public Pin and resolves `pin.it` only when the final target normalizes to a supported single-Pin URL. X uses the official `https://publish.x.com/oembed` surface plus `https://platform.x.com/widgets.js` for one public post; it does not use the pay-per-use X API. Bluesky uses the official `https://embed.bsky.app/oembed` endpoint for one public `bsky.app/profile/{handle|DID}/post/{rkey}` permalink with no account, credential or backend dependency. Facebook remains frozen as **In pausa / unsupported** because its real-world opaque share aliases could not be resolved reliably without leaving this boundary; the experimental PR #4 stays unmerged. YouTube uses the official Data API only for the mandatory pre-embed status check and the official privacy-enhanced IFrame player for playback.

## AD-003 — Privacy-minimal local state

Social Viewer has no viewing-history database, account state, analytics store, or backend.

Minimal local state is allowed only when it materially supports the viewing flow. Today that includes:

- onboarding completion;
- UI appearance preference (System / Light / Dark, default System);
- provider first-party cookies/preferences required to preserve provider consent choices across items;
- the explicit one-time X embed-consent decision, stored locally and revocable in Settings.

Provider first-party cookies/preferences are intentionally retained across WebView disposal and flushed normally. Third-party cookies remain disabled. The user can explicitly remove the shared provider site data through **Cancella dati del sito**. YouTube starts from the same third-party-cookie block and `youtube-nocookie.com`; any future exception requires repeatable device evidence.

If favorites/history ever become a feature, they must be explicitly opt-in and modeled separately rather than emerging accidentally from browser storage.

## AD-004 — WebView is an implementation detail

`SocialContent` carries renderable embed HTML plus a provider document base URL. TikTok uses its official dedicated player; Instagram and Threads use Meta's official tokenless oEmbed markup; Reddit uses official Reddit Embed/oEmbed markup; Pinterest uses the official Pin Widget; X uses official X oEmbed markup plus `platform.x.com/widgets.js`; Bluesky uses official `embed.bsky.app` oEmbed markup; YouTube uses the official privacy-enhanced IFrame player at `youtube-nocookie.com`. All render through the shared WebView without moving provider retrieval logic into the Android shell.

Threads does not require a new renderer hierarchy: the existing provider-aware document base URL remains sufficient. Third-party cookies stay disabled, provider first-party preferences may persist locally, and main-frame navigation stays blocked.

## AD-005 — Deep links are user-managed and provider-aware

The app declares user-managed web-link filters for TikTok, supported Instagram post/Reel paths, only Threads shorthand `/t/` paths on `threads.com` / legacy `threads.net`, bounded Reddit single-content paths on `reddit.com` / `www.reddit.com`, Pinterest `/pin/` paths on `pinterest.com` / `www.pinterest.com`, and Bluesky single-post paths on `bsky.app`. Social Viewer does not claim verified App Links for provider-owned domains because it cannot publish their `assetlinks.json` files.

Reddit uses API-1-compatible `pathPattern` filters rather than `pathAdvancedPattern`, which is unavailable below API 31. Canonical post/comment routes require the fixed `/r/{sub}/comments/{id}/...` structure, while share aliases require an explicit `/s/` segment. This keeps home, subreddit, profile, feed, search, wiki, mod, message and settings surfaces out of scope. Only `reddit.com` and `www.reddit.com` participate in Android direct-link state; `old.reddit.com`, `new.reddit.com` and `m.reddit.com` remain paste/Share-only for supported permalink families. Physical testing invalidated the previous assumption that `redd.it/{id}` was a working provider input: it redirects to a bare `/comments/{id}` route, while Reddit's oEmbed path requires the full canonical `/r/{subreddit}/comments/{id}/...` permalink. Recovering that missing subreddit/permalink generically would require an additional Reddit data API or page-metadata extraction, both outside the current boundary, so `redd.it` is now unsupported and not claimed. Bluesky uses the legacy-compatible manifest path pattern `/profile/.*/post/.*`; the provider then applies an exact four-segment URL policy with strict handle/DID/record-key validation before any oEmbed request, so ordinary profile pages are not accepted or rendered. Pinterest regional hosts and `pin.it` aliases intentionally remain paste/Share-only so opaque or broader Pinterest URLs are not overclaimed. Canonical Threads `/@user/post/...` paths remain paste/Share-only on the current minSdk-26 baseline: legacy Android path matching cannot safely constrain exactly one username segment without also claiming profile/feed-like surfaces. Facebook is displayed separately as **In pausa** and is not part of direct-link provider counts.

On Android 12+, `DomainVerificationManager` is the source of truth. The Android shell maps declared hosts to provider-oriented states (**Attiva / Parziale / Da configurare**) and treats the global link-handling permission as a gate. Settings presents one compact **Apertura diretta** area with provider breakdown and a single **Configura** action that opens Android's real **Open by default** screen.

First-party provider apps and the browser may compete for the same domains, so Social Viewer does not imply exclusive ownership. `ACTION_SEND` and manual paste remain fallbacks; the preferred product flow remains tap-on-link → Social Viewer → resolve/render when Android is configured accordingly. YouTube and X intentionally stay manual-paste/Android-Share-only in their current slices; no YouTube or X domain `ACTION_VIEW` filters are declared. Reddit direct opening is intentionally partial by URL family/host rather than broad by domain. For X, the supported `/{username}/status/{id}` route cannot be constrained safely enough by the legacy manifest matcher on the minSdk-26 baseline without overclaiming profiles or other X surfaces. The UI labels this section specifically as direct-link capability rather than total provider support.


## AD-006 — YouTube provider-native surface compromise

Social Viewer does not build feeds or recommendations. For YouTube only, provider-native related-video and advertising surfaces inside the official IFrame player are accepted because YouTube does not expose a supported way to remove them and policy forbids suppressing standard player surfaces. Social Viewer must never cover, restyle, or script them away.

`rel=0` is used only for its documented current behavior (related videos limited to the same channel), not as a recommendation kill switch. Autoplay remains disabled.

Current policy requires a Made For Kids lookup for every embedded video. The provider calls `videos.list?part=snippet,status` before creating `SocialContent`. It requires `privacyStatus=public`, `embeddable=true`, and an explicit `madeForKids=false`. MFK videos fail closed before any YouTube player is loaded and the user can open the original URL instead.

The Data API key is a build-time credential/configuration dependency, never a repository secret. Android package/certificate identity headers are attached so the key can be application-restricted, while API restrictions should still limit it to YouTube Data API v3.


## AD-007 — Comment surfaces remain provider-specific

The current product boundary renders one explicitly shared item. Reddit Embeds can represent either a post or one explicitly linked comment, but a post embed does not become a comment-tree client. Loading a post's comment listing would require a distinct Reddit Data API integration with registered OAuth access and its own policy/rate-limit handling. That work must be evaluated as a separate feature rather than inferred from the existing oEmbed provider.

The same rule applies across providers: comment viewing is enabled only when a provider exposes a documented, supportable mechanism that can remain read-only and bounded to the current item. Social Viewer must not simulate comments by scraping provider pages or by relaxing main-frame navigation into a general social browser.


## AD-008 — X embed privacy gate

X public-post rendering uses X for Websites rather than the pay-per-use X API. The provider calls `publish.x.com/oembed` with `hide_thread=true`, `omit_script=true` and `dnt=true`, then renders the returned official markup with `platform.x.com/widgets.js`. No X developer credential, OAuth flow, Social Viewer backend, scraping or remote browser is introduced.

Because X's current policy requires notice and consent for Embedded Posts / X for Websites where applicable, the first X load is gated before any request to X. After explicit approval, Social Viewer stores a local consent flag so later X links open without repeated prompts. The user can revoke that decision from Settings, after which the next X load is gated again. Third-party cookies remain blocked and main-frame navigation remains blocked. This consent state is product/privacy state, not viewing history.

The renderer includes a bounded local timeout so provider/widget failure cannot leave an indefinite black/loading surface. Deleted, protected or otherwise unavailable posts fail cleanly rather than triggering scraping or login bypass.


## AD-009 — Bluesky official oEmbed + bounded direct links

Bluesky single-post rendering uses the official `https://embed.bsky.app/oembed` endpoint directly from public `https://bsky.app/profile/{handle|DID}/post/{rkey}` permalinks. The provider requires no Bluesky login, OAuth flow, API key, backend, database, scraping, remote browser or paid service. It validates oEmbed redirects back to `embed.bsky.app`, maps expected unavailable responses to the shared unavailable-content path, and keeps a bounded local loading fallback.

Bluesky direct-link handling is user-managed through Android's existing Open-by-default flow. The manifest declares only `bsky.app` paths matching `/profile/.*/post/.*`; provider-level validation remains stricter and rejects profiles, feeds, extra navigation segments, HTTP URLs and lookalike domains. A physical-device gate verified that a real Bluesky post opens directly when `bsky.app` is selected for Social Viewer while a plain `/profile/{identifier}` URL is not intercepted.
