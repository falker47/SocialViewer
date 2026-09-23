# Architecture decisions

## AD-001 — Provider boundary

Every social network implements `SocialProvider` and returns a normalized `SocialContent`. Platform-specific URL parsing, redirects, API/oEmbed calls, and metadata extraction stay inside that provider.

Reason: provider integrations are the volatile part of the system. The Android shell and player should remain stable.

## AD-002 — Official/public integration first

A provider should prefer documented public embed/oEmbed mechanisms. If a platform requires authentication or blocks public embedding for a piece of content, Social Viewer reports that limitation instead of scraping around it.

## AD-003 — Stateless by default

No history/database layer is present in the MVP. WebView state is cleared on disposal. If favorites/history ever become a feature, they must be explicitly opt-in and modeled separately rather than emerging accidentally from browser storage.

## AD-004 — WebView is an implementation detail

`SocialContent` carries renderable embed HTML today because TikTok's official mechanism is an embed. A future provider may instead return a native media descriptor. When the second provider is implemented, evolve the model toward a sealed `RenderableContent` type if needed rather than forcing every provider through WebView.

## AD-005 — Deep links are user-managed

The app declares TikTok HTTP(S) intent filters, but it does not claim verified App Links for domains it does not own. The UX includes a settings shortcut and a share-intent fallback.
