# Social Viewer

Privacy-minimal Android viewer for **single public social-media links**.

Social Viewer is intentionally not a social client: no feed, no Social Viewer account, no viewing history, no recommendations, no analytics, and no backend. A user opens a public link someone explicitly sent them; the app resolves the matching provider and renders that one piece of content.

## Current baseline — v0.1.1

Verified on `main`:

- **TikTok** — public supported items through TikTok's official oEmbed + dedicated player path.
- **Instagram** — public posts and Reels through Meta's official tokenless oEmbed path.
- provider-generic Home/onboarding/Settings;
- provider-aware Android **Open by default** state;
- System / Light / Dark appearance;
- provider-generic local site-data clearing.

Current feature milestone:

- **Facebook** is implemented on `feature/facebook-provider` for public individual `/{owner}/posts/{post-id}` posts and `/reel/{reel-id}` Reels.
- Facebook uses Meta's official tokenless `v25.0/oembed_post` and `v25.0/oembed_video` endpoints.
- Returned Facebook markup is rendered with Meta's official SDK using the required `#xfbml=1&version=v25.0` fragment.
- No Meta access token, developer app, backend, database, Social Viewer account, or embedded secret is used.
- Facebook remains pending CI + Android manual smoke verification and must not be merged before that gate passes.

## Product rule

Social Viewer only handles content that is both public and available through a provider-supported public/embed mechanism. It does not bypass login requirements, age gates, private accounts, removals, embedding restrictions, or other access controls.

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
      +-------+---------+----------+
      |                 |          |
 TikTokProvider   InstagramProvider FacebookProvider
      |                 |          |
      +-------+---------+----------+
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

Each platform is isolated behind `SocialProvider`, so provider-specific URL policy, API/oEmbed access, and rendering details stay outside the Android shell.

`SocialContent` carries a provider document base URL plus embed HTML. Facebook fits that existing contract, so no new renderer hierarchy was introduced.

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

TikTok, Instagram, and Facebook own their web domains, so Social Viewer cannot publish the providers' `assetlinks.json` files and does not claim verified App Links for those domains.

The manifest declares:

- the existing TikTok web links;
- supported Instagram `/p/` and `/reel/` paths;
- Facebook `/reel/` paths.

Facebook individual post URLs remain supported through manual paste and Android Share, but are intentionally **not** declared for direct opening. Social Viewer's minSdk is 26; the legacy manifest path matcher cannot express exactly one owner segment in `/{owner}/posts/{id}` without also claiming unrelated Facebook paths. The API-31 advanced matcher cannot safely serve as the only constraint for the full supported Android range.

On Android 12+, `DomainVerificationManager` remains the source of truth for the user-managed state. Settings summarizes it per provider and opens Android's real **Open by default** screen. First-party apps or the browser may compete for the same provider domains.

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

The standard Gradle Wrapper is committed. After cloning, open the repository root in Android Studio and sync.

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

Current branch: `feature/facebook-provider`.

## Next milestones

1. Complete CI and the documented Facebook manual smoke gate; merge only after PASS.
2. After Facebook is verified, evaluate **Threads** as the next provider milestone without bundling it into the Facebook work.
3. Keep YouTube blocked until its previously identified policy/product constraints are explicitly accepted or resolved.

Keep adding and verifying one provider at a time.

## Non-goals

- downloading or rehosting social videos;
- extracting private/internal media URLs;
- bypassing authentication or platform restrictions;
- implementing a feed, messaging, comments client, profile browser, or social graph;
- collecting user analytics or viewing history.
