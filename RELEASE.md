# Google Play release workflow

Social Viewer targets **Google Play** as its initial public distribution channel.

## Fixed release identity

- Application ID: `io.github.falker47.socialviewer`
- Current version: `0.1.1` (`versionCode = 2`)
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

Copy `keystore.properties.example` to the gitignored `keystore.properties` file and fill in:

```properties
PLAY_UPLOAD_STORE_FILE=/absolute/or/repo-relative/path/to/socialviewer-upload.jks
PLAY_UPLOAD_STORE_PASSWORD=...
PLAY_UPLOAD_KEY_ALIAS=...
PLAY_UPLOAD_KEY_PASSWORD=...
```

The same four values may instead be supplied as Gradle properties or environment variables.

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

## First Play Console gate

After this repository plumbing is green:

1. Create the Social Viewer app in Play Console with package `io.github.falker47.socialviewer`.
2. Keep Google Play App Signing enabled with the default Google-generated app-signing key.
3. Generate and securely back up a dedicated RSA upload key / keystore.
4. Configure the four `PLAY_UPLOAD_*` values locally.
5. Read the **app-signing certificate SHA-1** from Play Console → App integrity.
6. Add that package + SHA-1 pair to the Android restrictions of the YouTube Data API key.
7. Optionally add the upload-key SHA-1 too for local signed-release smoke testing.
8. Build with `playReleaseBundle`.
9. Only after the signed release-candidate smoke gate passes, upload the AAB to an internal Play testing track.

Privacy policy, Data Safety, store listing assets and production rollout belong to later release-readiness milestones.
