# Architecture decisions

## AD-001 — Provider boundary

Every social network implements `SocialProvider` and returns a normalized `SocialContent`. Platform-specific URL parsing, redirects, API/oEmbed calls, and metadata extraction stay inside that provider.

Reason: provider integrations are the volatile part of the system. The Android shell and player should remain stable.

## AD-002 — Official/public integration first

A provider should prefer documented public embed/oEmbed mechanisms. If a platform requires authentication or blocks public embedding for a piece of content, Social Viewer reports that limitation instead of scraping around it.

Threads uses Meta's current tokenless `https://graph.threads.com/oembed` endpoint for public post URLs. Facebook remains frozen as **In pausa / unsupported** because its real-world opaque share aliases could not be resolved reliably without leaving this boundary; the experimental PR #4 stays unmerged.

## AD-003 — Privacy-minimal local state

Social Viewer has no viewing-history database, account state, analytics store, or backend.

Minimal local state is allowed only when it materially supports the viewing flow. Today that includes:

- onboarding completion;
- UI appearance preference (System / Light / Dark, default System);
- provider first-party cookies/preferences required to preserve provider consent choices across items.

Provider first-party cookies/preferences are intentionally retained across WebView disposal and flushed normally. Third-party cookies remain disabled. The user can explicitly remove the shared provider site data through **Cancella dati del sito**.

If favorites/history ever become a feature, they must be explicitly opt-in and modeled separately rather than emerging accidentally from browser storage.

## AD-004 — WebView is an implementation detail

`SocialContent` carries renderable embed HTML plus a provider document base URL. TikTok uses its official dedicated player; Instagram and Threads use Meta's official tokenless oEmbed markup. All render through the shared WebView without moving provider retrieval logic into the Android shell.

Threads does not require a new renderer hierarchy: the existing provider-aware document base URL remains sufficient. Third-party cookies stay disabled, provider first-party preferences may persist locally, and main-frame navigation stays blocked.

## AD-005 — Deep links are user-managed and provider-aware

The app declares user-managed web-link filters for TikTok, supported Instagram post/Reel paths, and only Threads shorthand `/t/` paths on `threads.com` / legacy `threads.net`. Social Viewer does not claim verified App Links for provider-owned domains because it cannot publish their `assetlinks.json` files.

Canonical Threads `/@user/post/...` paths remain paste/Share-only on the current minSdk-26 baseline: legacy Android path matching cannot safely constrain exactly one username segment without also claiming profile/feed-like surfaces. Facebook is displayed separately as **In pausa** and is not part of direct-link provider counts.

On Android 12+, `DomainVerificationManager` is the source of truth. The Android shell maps declared hosts to provider-oriented states (**Attiva / Parziale / Da configurare**) and treats the global link-handling permission as a gate. Settings presents one compact **Apertura diretta** area with provider breakdown and a single **Configura** action that opens Android's real **Open by default** screen.

First-party provider apps and the browser may compete for the same domains, so Social Viewer does not imply exclusive ownership. `ACTION_SEND` and manual paste remain fallbacks; the preferred product flow remains tap-on-link → Social Viewer → resolve/render when Android is configured accordingly.
