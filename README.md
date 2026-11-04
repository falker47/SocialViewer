# SocialViewer

**Open the content, not the feed.**

SocialViewer is an Android app that helps you view individual public social-media posts and videos shared by friends or family, without having to install the original social network's app.

It's designed for people who rarely use social media, want to spend less time scrolling, or simply want to see something someone sent them.

Open the shared content, watch or read it, and return to your conversation. No feed to browse, no recommendations to follow, and no SocialViewer account to create.

## Get SocialViewer

SocialViewer is currently available through **Google Play closed testing**. A public production release is not yet confirmed.

[**Join the Android test**](https://falker47.github.io/Nexus-portfolio/testers.html)

The tester page explains how to join the Google Group, opt in through Google Play, and install the app. Participants are asked to remain enrolled for 14 consecutive days and report any issues they encounter.

The version available through Google Play may differ from the source on `main`, as release builds can be prepared and tested separately.

## How it works

You can open a supported public link in three ways:

1. **Tap a link.** When Android's link-handling settings are configured, eligible links can open directly in SocialViewer.
2. **Share it.** Use Android's Share menu to send a link to SocialViewer.
3. **Paste it.** Open SocialViewer and paste a link into the input field. The clipboard button pastes and opens a valid link in one step.

For direct opening, go to SocialViewer's Settings and use the link-handling configuration to access Android's **Open by default** settings.

Direct opening is available for supported URL formats from TikTok, Instagram, Threads, Reddit, Pinterest and Bluesky. Android and other installed apps determine which application actually receives a link, so automatic opening cannot be guaranteed.

YouTube and X use manual paste or Android Share. Some Threads URLs also require these methods.

The interface is available in English and Italian, with light and dark themes.

## Supported platforms

The following integrations are implemented in the `main` source:

| Platform | Supported content |
|---|---|
| **TikTok** | Individual public videos and supported shared links, including common shortened URLs |
| **Instagram** | Public posts and Reels, subject to Meta's embedding restrictions |
| **Threads** | Individual public posts, including supported short links |
| **YouTube** | Public videos, Shorts and supported live-video links |
| **Reddit** | Individual public posts and explicitly linked comments |
| **Pinterest** | Individual public Pins, including `pin.it` links |
| **X (Twitter)** | Individual public posts |
| **Bluesky** | Individual public posts |

Integrations use provider-supported public embedding mechanisms rather than downloading or rehosting media.

**Important limitations**

- Some Instagram Reels may display a preview without playable video. Opening the original content in a browser may be necessary.
- Reddit's `redd.it` short links are not supported, and viewing a post does not provide a full comment-thread browser.
- YouTube videos marked *Made for Kids* are rejected by the app's pre-embed checks.
- Facebook is currently unsupported. Its share-link flow could not be supported reliably within the project's constraints.
- Private, deleted, restricted or unavailable content cannot be accessed. Some providers may require their own account or login even for content that appears public.

SocialViewer cannot guarantee that every public link will work. Provider restrictions and changes to external embedding services can affect availability.

## Privacy

SocialViewer is designed to minimize the data handled by the app itself.

It has no SocialViewer account system, developer-operated backend, viewing-history database, analytics SDK or advertising SDK. It requests internet access but no permissions for location, contacts, camera, microphone or personal files.

Some information is stored locally to make the app usable, including:

- Language, appearance and onboarding preferences.
- First-party cookies and site preferences used by embedded providers.
- Your consent choice before loading X embeds, which you can revoke in Settings.

Third-party cookies are disabled in the embedded WebView, and provider site data can be cleared from Settings.

**SocialViewer does not make you anonymous to social platforms.** Opening embedded content still connects to external services. Those providers may receive network and device information, process interactions, and display their own advertising or related-content elements.

The app does not bypass authentication, age restrictions, private accounts or other provider access controls.

Read the full [Privacy Policy](https://falker47.github.io/Nexus-portfolio/socialviewer-privacy.html), available in English and Italian.

## Development

SocialViewer is built with Kotlin, Jetpack Compose and Android WebView.

Its architecture separates platform-specific integrations from the Android interface. Each `SocialProvider` handles its supported URLs and public content retrieval, while the shared application logic handles incoming links, loading states and rendering.

This makes it possible to maintain or extend individual integrations without duplicating the core viewer.

See [ARCHITECTURE.md](ARCHITECTURE.md) for the technical design.

### Build locally

Requirements:

- Android Studio compatible with Android Gradle Plugin 9.4
- JDK 17
- Android SDK Platform 37

Clone the repository, open its root directory in Android Studio, and synchronize the Gradle project. The Gradle Wrapper is already included.

Useful commands from the repository root:

- `./gradlew testDebugUnitTest` — run unit tests.
- `./gradlew lintRelease` — check the release variant.
- `./gradlew assembleDebug` — build a debug APK.
- `./gradlew bundleRelease` — build a release Android App Bundle, unsigned unless signing is configured.

On Windows, use `gradlew.bat` instead of `./gradlew`.

### YouTube API configuration

YouTube playback uses the official embedded player, but the app requires a YouTube Data API v3 key to check video availability and *Made for Kids* status before playback.

Set `YOUTUBE_API_KEY` using one of the supported local sources:

- `local.properties` (gitignored)
- A Gradle property
- An environment variable

Never commit API keys or signing credentials. Restrict the YouTube key to the required API and the appropriate Android package and signing certificate.

CI intentionally runs without a production YouTube key, so successful automated checks do not establish that YouTube playback works in a distributed build.

### Testing and releases

GitHub Actions runs unit tests, release lint and Android build/packaging checks. These verify the source and generated artifacts but cannot replace testing on an Android device.

Provider playback, WebView behavior, direct-link handling and Play-signed builds require separate verification.

For detailed instructions, see [TESTING.md](TESTING.md) and [RELEASE.md](RELEASE.md).

A signed release bundle requires the developer's upload-signing configuration and a production YouTube API key. Building an AAB does not automatically publish it to Google Play.

## License and credits

Developed by **Maurizio Falconi (falker47)** under the Falker Studio project identity.

No license file is currently included in this repository. Public access to the source code does not by itself grant permission to reuse or redistribute it.

SocialViewer uses external social-media services and their official or public embedding mechanisms. Third-party content and services remain subject to their respective owners' terms.

For questions, bug reports or feedback, contact [falker.dev@gmail.com](mailto:falker.dev@gmail.com).
