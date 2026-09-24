# Architecture decisions

## AD-001 — Provider boundary

Every social network implements `SocialProvider` and returns a normalized `SocialContent`. Platform-specific URL parsing, redirects, API/oEmbed calls, and metadata extraction stay inside that provider.

Reason: provider integrations are the volatile part of the system. The Android shell and player should remain stable.

## AD-002 — Official/public integration first

A provider should prefer documented public embed/oEmbed mechanisms. If a platform requires authentication or blocks public embedding for a piece of content, Social Viewer reports that limitation instead of scraping around it.

Facebook uses Meta's official tokenless public oEmbed endpoints: `v25.0/oembed_post` for supported individual posts and `v25.0/oembed_video` for supported Reels. The returned XFBML markup is rendered with the official Facebook SDK using `#xfbml=1&version=v25.0`.

Facebook-generated `/share/p/{code}` and `/share/r/{code}` links are opaque aliases, not official oEmbed URL families. Resolution therefore has two bounded stages: (1) the existing cheap HTTP redirect fast-path; (2) only when that remains opaque, a temporary on-device WebView may render the Facebook share page long enough to observe main-frame navigation and read URL-identity metadata (`rel=canonical` / `og:url`). The resolver accepts only supported HTTPS Facebook canonical post/Reel URLs of the alias's expected kind. It never extracts post text, images, video, comments, or other content and does not provide a browsable Facebook surface.

## AD-003 — Privacy-minimal local state

Social Viewer has no viewing-history database, account state, analytics store, or backend.

Minimal local state is allowed only when it materially supports the viewing flow. Today that includes:

- onboarding completion;
- UI appearance preference (System / Light / Dark, default System);
- provider first-party cookies/preferences required to preserve provider consent choices across items.

Provider first-party cookies/preferences are intentionally retained across the normal embed WebView disposal and flushed normally. Third-party cookies remain disabled. The user can explicitly remove the shared provider site data through **Cancella dati del sito**.

The Facebook share-link resolver is a separate ephemeral WebView surface assigned to a dedicated AndroidX WebKit profile. Resolver cookies/storage are therefore isolated from the normal renderer; first-party cookies may exist only inside that disposable profile while the logged-out page resolves, third-party cookies are disabled, `LOAD_NO_CACHE` is used, main-frame navigation outside the small Facebook host allowlist is rejected, and only URL-identity metadata is read. The WebView is destroyed and the resolver profile is deleted after success/failure/timeout. Devices whose WebView does not support multi-profile isolation fail closed instead of falling back to shared browser state.

If favorites/history ever become a feature, they must be explicitly opt-in and modeled separately rather than emerging accidentally from browser storage.

## AD-004 — WebView is an implementation detail

`SocialContent` carries renderable embed HTML plus a provider document base URL.

- TikTok uses its official dedicated player.
- Instagram uses Meta's official tokenless oEmbed markup.
- Facebook uses Meta's tokenless oEmbed markup plus the official Facebook SDK required to render XFBML.

All three render through the shared WebView. Facebook did not expose a renderer mismatch that justified a new render hierarchy, so the existing `documentBaseUrl + embedHtml` contract remains unchanged.

The Facebook alias resolver is intentionally **not** a fourth renderer: it exists only before provider resolution to transform a share alias into a canonical URL, then is discarded. The shared embed WebView continues to block third-party cookies, allow only first-party provider state needed for provider preferences, and block main-frame navigation so an embed cannot become an in-app social browser.

## AD-005 — Deep links are user-managed and provider-aware

Social Viewer does not claim verified App Links for provider-owned domains because it cannot publish their `assetlinks.json` files.

The manifest declares:

- TikTok hosts already supported by the verified baseline;
- Instagram post/Reel paths;
- Facebook canonical Reel paths plus the narrow `/share/p/` and `/share/r/` alias prefixes handled by the local resolver.

Facebook `/{owner}/posts/{id}` URLs are deliberately omitted from `ACTION_VIEW` because the minSdk-26 matcher cannot constrain them safely. The fixed `/share/p/` and `/share/r/` prefixes can be constrained without overclaiming and are now declared because the local browser-assisted resolver covers the logged-out opaque-alias case. `/share/v/` remains out of scope. With minSdk 26, Android's legacy `pathPattern` cannot constrain the owner to exactly one path segment, so a pattern broad enough for Facebook posts would also claim unrelated Facebook surfaces such as group post paths. `pathAdvancedPattern` is only available from API 31 and therefore is not used as the sole constraint for an app that supports older Android releases. Manual paste and Android Share cover Facebook posts without overclaiming domains/paths.

On Android 12+, `DomainVerificationManager` is the source of truth. The Android shell maps declared hosts to provider-oriented states (**Attiva / Parziale / Da configurare**) and treats the global link-handling permission as a gate. Settings presents one compact **Apertura diretta** area with provider breakdown and one **Configura** action that opens Android's real **Open by default** screen.

First-party provider apps and the browser may compete for the same domains. Social Viewer does not imply exclusive ownership.
