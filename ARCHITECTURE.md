# Architecture decisions

## AD-001 — Provider boundary

Every social network implements `SocialProvider` and returns a normalized `SocialContent`. Platform-specific URL parsing, redirects, API/oEmbed calls, and metadata extraction stay inside that provider.

Reason: provider integrations are the volatile part of the system. The Android shell and player should remain stable.

## AD-002 — Official/public integration first

A provider should prefer documented public embed/oEmbed mechanisms. If a platform requires authentication or blocks public embedding for a piece of content, Social Viewer reports that limitation instead of scraping around it.

Facebook's official tokenless public oEmbed endpoints still work for supported canonical public posts and Reels, but the provider is **not product-supported** because Facebook-generated `/share/p/{code}` and `/share/r/{code}` aliases are the real input family for the target use case.

The feasibility spike tested both allowed resolution mechanisms: logged-out HTTP redirects and a local ephemeral WebView restricted to URL identity only (main-frame navigation plus `rel=canonical` / `og:url`). On physical-device smoke testing, neither real alias family produced a usable canonical target. The spike verdict is therefore **NOT FEASIBLE** under the current boundary.

No broader DOM/content scraping, login/session requirement, backend resolver, third-party service, or remote headless browser is introduced. Facebook remains BLOCKED/UNSUPPORTED until the provider exposes a durable logged-out path compatible with these constraints.

## AD-003 — Privacy-minimal local state

Social Viewer has no viewing-history database, account state, analytics store, or backend.

Minimal local state is allowed only when it materially supports the viewing flow. Today that includes:

- onboarding completion;
- UI appearance preference (System / Light / Dark, default System);
- provider first-party cookies/preferences required to preserve provider consent choices across items.

Provider first-party cookies/preferences are intentionally retained across WebView disposal and flushed normally. Third-party cookies remain disabled. The user can explicitly remove the shared provider site data through **Cancella dati del sito**.

If favorites/history ever become a feature, they must be explicitly opt-in and modeled separately rather than emerging accidentally from browser storage.

## AD-004 — WebView is an implementation detail

`SocialContent` carries renderable embed HTML plus a provider document base URL.

- TikTok uses its official dedicated player.
- Instagram uses Meta's official tokenless oEmbed markup.
- Facebook uses Meta's tokenless oEmbed markup plus the official Facebook SDK required to render XFBML.

All three render through the shared WebView. Facebook did not expose a renderer mismatch that justified a new render hierarchy, so the existing `documentBaseUrl + embedHtml` contract remains unchanged.

The shared WebView continues to block third-party cookies, allow only first-party provider state needed for provider preferences, and block main-frame navigation so an embed cannot become an in-app social browser.

## AD-005 — Deep links are user-managed and provider-aware

Social Viewer does not claim verified App Links for provider-owned domains because it cannot publish their `assetlinks.json` files.

The manifest on the experimental Facebook branch declares only canonical Facebook Reel paths. Facebook `/share/p/` and `/share/r/` are deliberately not claimed because the product cannot reliably resolve them logged-out; intercepting them would route users into a known unsupported path.

Canonical `/{owner}/posts/{id}` direct links are also omitted because the minSdk-26 matcher cannot constrain exactly one owner segment without overclaiming unrelated Facebook paths. Since the Facebook milestone is blocked and unmerged, these experimental Facebook declarations do not become part of the verified `main` baseline.

On Android 12+, `DomainVerificationManager` is the source of truth. The Android shell maps declared hosts to provider-oriented states (**Attiva / Parziale / Da configurare**) and treats the global link-handling permission as a gate. Settings presents one compact **Apertura diretta** area with provider breakdown and one **Configura** action that opens Android's real **Open by default** screen.

First-party provider apps and the browser may compete for the same domains. Social Viewer does not imply exclusive ownership.
