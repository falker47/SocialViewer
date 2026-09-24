# Social Viewer

Privacy-minimal Android viewer for **single public social-media links**.

Social Viewer is intentionally not a social client: no feed, no Social Viewer account, no viewing history, no recommendations, no analytics, and no backend. A user opens a public link someone explicitly sent them; the app resolves the matching provider and renders that one piece of content.

## Current baseline — v0.1.1

Implemented providers:

- **TikTok** — verified baseline.
- **Instagram** — public posts and Reels via Meta's official tokenless oEmbed path; manual paste/share is implemented and pending emulator/device smoke verification.
- Public availability/metadata are checked through TikTok's oEmbed endpoint.
- Playback uses TikTok's official dedicated `/player/v1/{post_id}` embed player.
- Canonical TikTok URLs plus `vm.tiktok.com` / `vt.tiktok.com` short links are supported.
- Android `ACTION_VIEW` is the preferred direct-link path.
- `ACTION_SEND` text sharing remains an unpromoted technical fallback.
- Manual entry remains available; the trailing clipboard button pastes, validates, and opens in one tap.
- First-run onboarding uses two coach marks on the real Home screen and respects the Android navigation-bar safe area.
- Settings expose actual TikTok direct-link state/configuration, shared provider site-data clearing, app/privacy information, and Appearance.
- Appearance supports **System / Light / Dark**. System is the default, follows Android's current theme, and the selection is persisted locally.

The TikTok playback path and the Home/onboarding/Settings/theme flow are verified on Android device/emulator. The Instagram provider is implemented on `feature/instagram-provider` and requires the documented emulator/device smoke gate before merge.

## Product rule

Social Viewer only handles content that is both public and available through a provider-supported public/embed mechanism. It does not bypass login requirements, age gates, private accounts, removals, or other access controls.

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
      +-------+--------+
      |                |
 TikTokProvider   future providers
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

TikTok owns its web domains, so Social Viewer cannot publish TikTok's `assetlinks.json` and cannot become a verified App Link handler for those domains.

On modern Android versions, the user may need to explicitly allow Social Viewer under **Open by default / supported links**. The Home and Settings UI expose **Apertura diretta** and open that system screen when configuration is needed.

On Android 12+, Social Viewer reads the real user-selected domain state through `DomainVerificationManager`; it does not fake an in-app link-handling toggle.

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

Examples: `feature/dark-mode`, `feature/instagram-provider`.

## Next milestones

1. Complete emulator/device smoke verification for the Instagram provider and merge only after PASS.
2. Generalize visible TikTok-specific shared UI and direct-link summaries now that two providers exist.
3. Add Facebook next if the Meta integration can reuse the Instagram provider work.
4. Consider Threads after Facebook; keep YouTube blocked until its policy/product conflicts are explicitly resolved.
5. Evaluate YouTube separately, including link-routing semantics.

Do not pre-emptively redesign the provider architecture before the second real provider demonstrates a need.

## Non-goals

- downloading or rehosting social videos;
- extracting private/internal media URLs;
- bypassing authentication or platform restrictions;
- implementing a feed, messaging, comments client, or social graph;
- collecting user analytics or viewing history.
