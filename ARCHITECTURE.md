# Architecture decisions

## AD-001 — Provider boundary

Every social network implements `SocialProvider` and returns a normalized `SocialContent`. Platform-specific URL parsing, redirects, API/oEmbed calls, and metadata extraction stay inside that provider.

Reason: provider integrations are the volatile part of the system. The Android shell and player should remain stable.

## AD-002 — Official/public integration first

A provider should prefer documented public embed/oEmbed mechanisms. If a platform requires authentication or blocks public embedding for a piece of content, Social Viewer reports that limitation instead of scraping around it.

## AD-003 — Privacy-minimal local state

Social Viewer has no viewing-history database, account state, analytics store, or backend.

Minimal local state is allowed only when it materially supports the viewing flow. Today that includes:

- onboarding completion;
- UI appearance preference (System / Light / Dark, default System);
- provider first-party cookies/preferences required to preserve the user's TikTok consent choice across videos.

TikTok first-party cookies/preferences are intentionally retained across player disposal and flushed normally. Third-party cookies remain disabled. The user can explicitly remove provider site data through **Cancella dati del sito**.

If favorites/history ever become a feature, they must be explicitly opt-in and modeled separately rather than emerging accidentally from browser storage.

## AD-004 — WebView is an implementation detail

`SocialContent` carries renderable embed HTML today because TikTok's official dedicated player is embedded in a WebView. A future provider may instead return another renderable representation.

When the second provider is implemented, evolve the model toward a sealed `RenderableContent` type only if the real second integration requires it. Do not pre-emptively force all future providers through one renderer or redesign the abstraction without evidence.

## AD-005 — Deep links are user-managed

The app declares TikTok HTTP(S) intent filters, but it does not claim verified App Links for domains it does not own.

On Android 12+, `DomainVerificationManager` is the source of truth for whether the user has approved the declared TikTok hosts. Social Viewer may display that state and open Android's **Open by default** settings, but it must not present a fake silent toggle.

`ACTION_SEND` remains implemented as a technical fallback; the primary product flow is tap-on-link → Social Viewer → resolve/render.
