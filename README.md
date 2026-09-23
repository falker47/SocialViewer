# Social Viewer

Privacy-minimal Android viewer for **single public social-media links**.

The app is intentionally not a social client: no feed, no account, no history, no recommendations, no analytics, no backend. A user opens a public link someone explicitly sent them; Social Viewer resolves the matching provider and renders that one piece of content.

## MVP status — v0.1.1 + light UI flow

Implemented provider:

- **TikTok** via TikTok's public oEmbed endpoint and official embed markup.
- Handles ordinary TikTok URLs plus `vm.tiktok.com` / `vt.tiktok.com` short links.
- Accepts Android `ACTION_VIEW` links and `ACTION_SEND` text shares.
- Includes a manual URL field as a fallback; the trailing clipboard action pastes, validates, and opens in one tap.
- First-run onboarding uses two coach marks on the real Home screen.
- Settings expose direct-link status/configuration, TikTok site-data clearing, and app/privacy information.

Planned providers:

- Instagram
- Facebook
- Threads / others only where a durable public embedding mechanism exists

## Product rule

Social Viewer only handles content that is both public and available through a provider-supported public/embed mechanism. It does not bypass login requirements, age gates, private accounts, removed content, or other access controls.

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
 public oEmbed/API
      |
      v
      SocialContent
      |
      v
 privacy-hardened WebView
```

Each platform is isolated behind `SocialProvider`, so a provider can be replaced when its public integration changes without rewriting the rest of the app.

## Privacy model

Social Viewer itself:

- has no account system;
- stores no viewing history;
- has no database or backend;
- contains no analytics or ad SDK;
- requests no storage, contacts, location, camera, or microphone permissions;
- keeps provider cookie consent/preferences locally so TikTok does not ask again on every video;
- exposes an explicit **Cancella dati del sito** action to remove WebView cookies/preferences;
- disables cleartext HTTP traffic.

The remote social platform/CDN still receives ordinary network metadata required to serve the public embed (for example IP address and HTTP/WebView metadata). This project does **not** claim network anonymity from the provider.

## Android link handling caveat

TikTok owns its web domains, so Social Viewer cannot publish TikTok's `assetlinks.json` and therefore cannot become a *verified* Android App Link handler for those domains.

On modern Android versions, the user may need to explicitly allow Social Viewer under **Open by default / supported links**. The Home/Settings UI exposes **Apertura diretta** and opens that system settings page when configuration is needed. On Android 12+ the app reads the real domain-association state with `DomainVerificationManager` after the user returns. Sharing text/URLs to Social Viewer remains a technical fallback path but is not promoted in the primary UI.

## First setup on Windows

The source archive includes `bootstrap-gradle.cmd` / `bootstrap-gradle.ps1`. Run the `.cmd` once before the first Android Studio import: it downloads official Gradle 9.6.0, verifies its SHA-256 checksum, and generates the standard Gradle Wrapper locally. Detailed device-test instructions are in `TESTING.md`.

## Build

Requirements:

- Android Studio compatible with Android Gradle Plugin 9.4
- JDK 17
- Android SDK / compileSdk 37

Versions are deliberately pinned in the build files. The current scaffold uses:

- AGP `9.4.0`
- Gradle `9.6.0`
- AGP built-in Kotlin (KGP `2.2.10`) + Compose compiler plugin `2.2.10`
- Compose BOM `2026.09.00`
- Activity Compose `1.13.0`
- kotlinx.coroutines `1.11.0`

Build/debug dall'IDE. Dopo il primo sync puoi generare e committare il Gradle Wrapper (`gradle wrapper`) per rendere build e CI completamente riproducibili da CLI.

## Repository GitHub

Il pacchetto sorgente è pronto per essere inizializzato con l'identità Git dell'utente e pubblicato. La creazione del repository remoto GitHub è deliberatamente lasciata come primo passo operativo perché il connettore disponibile in questa sessione non espone la creazione di nuovi repository.

## Next milestones

1. Device-test the new Home/onboarding/Settings flow on the target phone.
2. Verify direct-link state detection after returning from Android "Open by default" settings.
3. Re-run canonical + `vm.tiktok.com` + `vt.tiktok.com` playback smoke tests to confirm the UI refactor did not regress the player.
4. Add an Instagram provider only after re-validating Meta's current public embedding requirements.
5. Research YouTube/Facebook as separate providers while preserving the provider boundary.
6. Add basic UI/instrumentation tests and CI once the light UI flow is stable.

## Non-goals

- downloading/rehosting social videos;
- extracting private media URLs;
- bypassing authentication or platform restrictions;
- implementing an infinite feed;
- collecting user analytics or behavior history.

## Stato di verifica

- struttura e dipendenze revisionate staticamente;
- policy URL TikTok e parser degli URL condivisi coperti da unit test JVM;
- build Android / riproduzione TikTok su dispositivo non eseguite in questo ambiente perché non dispone di Android SDK/emulatore e il container non ha accesso di rete ai repository Gradle;
- primo gate operativo: sync/build in Android Studio e smoke test con link `vm.tiktok.com`, `vt.tiktok.com` e URL canonico.
