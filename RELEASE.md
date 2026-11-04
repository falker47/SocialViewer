# Google Play release workflow

SocialViewer targets **Google Play** as its initial public distribution channel.

## Fixed release identity

- Application ID: `io.github.falker47.socialviewer`
- Live Play Console checked on 2 November 2026: closed track `Test chiusi - SocialViewer` has `0.1.9` (`versionCode = 10`) available to testers; internal testing has `0.1.4` (`versionCode = 5`); Production is inactive; no pending publication submission was shown. The dashboard showed 12 testers active for 13 consecutive days, short of the 14-day production-access requirement. The highest relevant live version code is 10.
- Prepared source candidate for the existing closed track: `0.1.10` (`versionCode = 11`). See `docs/RELEASE_0.1.10_INSTAGRAM_EMBED.md`. A signed bundle and any Play upload remain unverified.
- Minimum SDK: 26
- Target / compile SDK: 37
- Publishing format: Android App Bundle (`.aab`)
- Signing model: Google Play App Signing + developer-controlled upload key

The application ID is considered frozen before the first Play publication.

## Two signing identities

Google Play App Signing uses two distinct keys:

1. **Upload key** — kept by the developer. It signs the AAB uploaded to Play Console.
2. **App-signing key** — kept by Google Play. Google signs the APKs installed by users with this key.

For a new app, prefer the Google-generated app-signing key. Do not commit or export private signing material into this repository.

## Local upload-key configuration

Signing material is optional for ordinary development and CI. It becomes mandatory only for a publishable Play bundle.

The release signing config intentionally uses a **JKS** upload keystore. AGP 9.4 otherwise defaults the signing store type to PKCS12, so the repository sets `storeType = "JKS"` explicitly.

Copy `keystore.properties.example` to the gitignored `keystore.properties` file and fill in:

```properties
PLAY_UPLOAD_STORE_FILE=/absolute/or/repo-relative/path/to/socialviewer-upload.jks
PLAY_UPLOAD_STORE_PASSWORD=...
PLAY_UPLOAD_KEY_ALIAS=...
PLAY_UPLOAD_KEY_PASSWORD=...
```

The same four values may instead be supplied as Gradle properties or environment variables.

If a signing password contains characters that are unsafe to persist through a Java `.properties` file/encoding path, keep only the non-secret path + alias in `keystore.properties` and inject `PLAY_UPLOAD_STORE_PASSWORD` / `PLAY_UPLOAD_KEY_PASSWORD` through the current process environment for the publish build. Never put the password on the command line.

The repository already ignores:

- `*.jks`
- `*.keystore`
- `keystore.properties`
- `local.properties`

Do not add exceptions that re-include these files.

## YouTube Data API key

`YOUTUBE_API_KEY` remains a build-time value supplied through:

- a Gradle property;
- an environment variable; or
- gitignored `local.properties`.

A publishable Play build must have a non-empty key.

The app sends `X-Android-Package` and `X-Android-Cert`, so Android application restrictions depend on the **runtime signing certificate**.

For the Play-distributed app, authorize:

- package: `io.github.falker47.socialviewer`
- SHA-1: **Google Play app-signing certificate SHA-1**

Do not use the upload-key SHA-1 as the only production restriction: Play-installed APKs are signed with the app-signing key. If local signed-release testing must use the same API key, the Google API-key restriction can additionally authorize the same package with the upload-key SHA-1.

Keep the API restriction limited to **YouTube Data API v3**.

## CI release gate

Normal CI deliberately has no signing secret and no production API key.

It runs:

```text
testDebugUnitTest
lintRelease
assembleDebug
bundleRelease
```

This proves that the release variant compiles, passes lint, and can be packaged as an unsigned AAB without putting release credentials in GitHub.

It does **not** prove that a publishable signed build is correctly configured.

## Publishable Play bundle

After the upload key and YouTube production key are configured locally or in a secure release environment, run:

```text
gradlew.bat playReleaseBundle
```

The custom task fails before the release bundle is accepted as publishable unless:

- `YOUTUBE_API_KEY` is non-empty;
- all four upload-signing values are present;
- the configured keystore file exists.

The resulting signed bundle is the standard release output under:

```text
app/build/outputs/bundle/release/
```

Before uploading, confirm the exact generated filename rather than assuming it.

## Remaining Play Console gates

The SocialViewer Play app, Play App Signing identity and existing internal/closed test tracks are already present. For the current candidate:

1. Read the existing upload-key configuration from the authorized secure environment. Do not generate a replacement key. If it is unavailable, stop before signing.
2. Confirm the existing non-empty YouTube Data API key and its Android restriction for package `io.github.falker47.socialviewer` + the **Play app-signing certificate SHA-1**. Limit the API restriction to YouTube Data API v3. Do not print or rotate the key.
3. Run `gradlew.bat playReleaseBundle`, then independently verify the AAB's package, version, upload certificate, SHA-256 and integrity.
4. Install the signed bundle through the existing test track and complete the physical QA gate against that exact Play-installed revision.
5. Confirm the privacy-policy URL, Data Safety answers and store listing against the shipped build before any track submission.
6. Production access remains unavailable until Play's 12-tester / 14-day condition is met and production access is granted. Do not activate a production release without the developer's explicit approval.
