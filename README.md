# Social Viewer

Privacy-minimal Android viewer for **single public social-media links**.

Social Viewer is intentionally not a social client: no feed, no Social Viewer account, no viewing history, no recommendations, no analytics, and no backend. A user opens a public link someone explicitly sent them; the app resolves the matching provider and renders that one piece of content.

## Current baseline — v0.1.1

Implemented providers:

- **TikTok** — verified baseline.
- **Instagram** — verified public posts and Reels via Meta's official tokenless oEmbed path.
- **Threads** — verified public post permalinks, shorthand `/t/` URLs, legacy `threads.net` compatibility and `/t/` direct opening via Meta's official tokenless oEmbed path.
- **Facebook** — **In pausa / unsupported** for the real-world share-link flow. PR #4 remains an unmerged technical record and is not counted as a supported provider.
- **YouTube / Shorts** — verified for public watch URLs, `youtu.be`, Shorts and the narrow `/live/` alias through the official privacy-enhanced IFrame player. Made For Kids videos fail closed before embedding; removed/unavailable content is handled cleanly.
- **Reddit** — verified public post permalinks, single-comment permalinks, `redd.it` short links and Reddit `/s/` share aliases through Reddit's official oEmbed/Embeds surface. Full comment-thread browsing is not part of this provider slice.
- Public availability/metadata are checked through TikTok's oEmbed endpoint.
- Playback uses TikTok's official dedicated `/player/v1/{post_id}` embed player.
- Canonical TikTok URLs plus `vm.tiktok.com` / `vt.tiktok.com` short links are supported.
- Android `ACTION_VIEW` is the preferred direct-link path.
- `ACTION_SEND` text sharing remains an unpromoted technical fallback.
- Manual entry remains available; the trailing clipboard button pastes, validates, and opens in one tap.
- First-run onboarding uses two coach marks on the real Home screen and respects the Android navigation-bar safe area.
- Home and Settings summarize the real Android direct-link state for TikTok, Instagram and Threads; configuration always opens Android's **Open by default** screen.
- Appearance supports **System / Light / Dark**. System is the default, follows Android's current theme, and the selection is persisted locally.

TikTok, Instagram, Threads, YouTube and Reddit playback plus the shared multi-provider UI are verified. Reddit completed its physical-device gate for a public post, a single comment, a real share alias, unavailable/removed behavior, appearance, and existing-provider regressions.

## Product rule

Social Viewer only handles content that is both public and available through a provider-supported public/embed mechanism. It does not bypass login requirements, age gates, private accounts, removals, or other access controls. It does not create recommendation surfaces of its own; provider-native related/advertising surfaces that cannot be removed through supported controls may remain inside an official player. For YouTube, Made For Kids status is checked before embedding and MFK videos are rejected in-app.

## Architecture

```text
incoming Android intent / shared text
              |
              v
          UrlExtractor
              |
              v
        ProviderRegistry
              |
      +-------+---------+---------+
      |           |           |           |
 TikTokProvider InstagramProvider ThreadsProvider YouTubeProvider RedditProvider
      |           |           |           |
      +-----------+-----------+-----------+
                  |
              v
 public provider integration
      |
      v
      SocialContent
      |
      v
 renderer
```

Each platform is isolated behind `SocialProvider`, so provider-specific URL parsing, redirects, API/oEmbed access, and rendering details can evolve without moving that logic into the Android shell.

See `ARCHITECTURE.md` for the current decisions.

## Privacy model

Social Viewer itself:

- has no account system;
- stores no viewing history;
- has no database or backend;
- contains no analytics or ad SDK;
- requests no storage, contacts, location, camera, or microphone permissions;
- disables cleartext HTTP traffic;
- disables third-party cookies in the embedded WebView;
- intentionally retains provider first-party consent/preferences across items;
- exposes **Cancella dati del sito** to remove shared local provider cookies/preferences.

The remote social platform/CDN still receives ordinary network metadata required to serve a public embed. This project does **not** claim network anonymity from the provider.

## Android direct-link handling

TikTok, Instagram and Threads own their web domains, so Social Viewer cannot publish the providers' `assetlinks.json` files and cannot self-verify those domains. YouTube and Reddit are intentionally not declared for direct-link handling in their current implementation slices.

The manifest declares TikTok web links, supported Instagram post/Reel paths, and only the safely constrainable Threads shorthand `/t/` paths on `threads.com` and legacy `threads.net`. Canonical Threads `/@user/post/...` permalinks remain available through manual paste/Android Share because the minSdk-26 path matcher cannot express that route narrowly without overclaiming profile/feed surfaces. On modern Android versions, the user may explicitly associate Social Viewer with declared provider domains under **Open by default / supported links**. First-party apps or the browser may compete for the same domains.

On Android 12+, Social Viewer reads the real per-domain user state through `DomainVerificationManager`, summarizes it per provider, and opens the Android system screen for changes. It does not expose a fake in-app toggle. Manual paste and Android Share remain fallbacks; they are the current YouTube and Reddit routing modes. Settings explicitly labels the direct-link count so it is not confused with the total number of supported providers.

## YouTube Data API configuration

YouTube playback itself is keyless, but current YouTube policy requires a Made For Kids lookup before embedding. The app therefore expects `YOUTUBE_API_KEY` at build time.

Use one of these local-only sources:

- `YOUTUBE_API_KEY=...` in the repository `local.properties` file (already gitignored);
- Gradle property `YOUTUBE_API_KEY`;
- environment variable `YOUTUBE_API_KEY`.

Do not commit the key. Restrict it to the **YouTube Data API v3** and, for Android application restrictions, package `io.github.falker47.socialviewer` plus the signing certificate SHA-1. The client sends `X-Android-Package` and `X-Android-Cert` headers for this purpose.

CI intentionally builds with an empty key; deterministic tests use fixtures and do not call YouTube live.

## Build

Repository: https://github.com/falker47/SocialViewer

Requirements:

- Android Studio compatible with Android Gradle Plugin 9.4
- JDK 17
- Android SDK Platform 37

Pinned project versions:

- AGP `9.4.0`
- Gradle `9.6.0`
- Compose compiler plugin `2.2.10`
- Compose BOM `2026.09.00`
- Activity Compose `1.13.0`
- kotlinx.coroutines `1.11.0`

The standard Gradle Wrapper is committed. After cloning, open the repository root in Android Studio and sync; the old bootstrap scripts are no longer required for normal setup.

CLI checks:

```text
gradlew.bat testDebugUnitTest
gradlew.bat assembleDebug
```

Detailed smoke-test steps are in `TESTING.md`.

## Continuous integration

GitHub Actions runs on pushes and pull requests targeting `main` and executes:

```text
testDebugUnitTest
assembleDebug
```

Device/WebView/provider behavior still requires a real Android smoke test before a feature is considered verified.

## Development workflow

- `main` is the last verified baseline.
- Non-trivial work uses a `feature/...` branch.
- Open a PR back to `main`.
- CI must be green.
- Test device/emulator behavior when the change touches UI, WebView, intents, or providers.
- Merge only after the relevant smoke test passes.
- Truly isolated micro-fixes may go directly to `main` when they are easy to verify and revert.

Examples: `feature/dark-mode`, `feature/instagram-provider`, `feature/multi-provider-ui`.

## Next milestone

Reddit is complete and verified after its physical-device gate. Facebook remains frozen as **In pausa / unsupported** and PR #4 remains unmerged.

The next provider-expansion milestone is Pinterest. A richer read-only comments experience is a separate future product/architecture question: Reddit's Embeds can render a single comment permalink, but loading a post's comment thread would require Reddit Data API access rather than the current zero-account oEmbed path.

## Non-goals

- downloading or rehosting social videos;
- extracting private/internal media URLs;
- bypassing authentication or platform restrictions;
- implementing a feed, messaging, comments client, or social graph;
- collecting user analytics or viewing history.
