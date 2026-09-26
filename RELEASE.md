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

## First Play Console gate

After this repository plumbing is green:

1. Generate and securely back up a dedicated RSA JKS upload key / keystore.
2. Configure the four `PLAY_UPLOAD_*` values locally.
3. Build the signed AAB with `playReleaseBundle` using the existing non-empty YouTube Data API key. For a local signed-release playback check, the API-key restriction may additionally authorize package `io.github.falker47.socialviewer` with the upload-key SHA-1.
4. In Play Console, create the Social Viewer app, accept the Play App Signing terms and keep the default Google-generated app-signing key. The initial Create app form does not define the Android package; the uploaded bundle carries the frozen application ID.
5. Create an internal-testing release and upload the signed AAB. This establishes the Play package/signing identity; do not roll it out to production.
6. Open Play Console → App integrity / App signing and read the **app-signing certificate SHA-1**.
7. Add package `io.github.falker47.socialviewer` + that Play app-signing SHA-1 to the Android restrictions of the YouTube Data API key. Google API-key restrictions can authorize more than one package/certificate pair, so the debug/upload identities may remain only where they are still needed for testing.
8. Install the Play-generated build from the internal test track and run the signed release-candidate smoke gate.
9. Proceed to store listing, privacy/Data Safety and production rollout only after that gate passes.

Privacy policy, Data Safety, store listing assets and production rollout belong to later release-readiness milestones.
