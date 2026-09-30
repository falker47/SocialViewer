# PR #25 — physical-device retest (no merge, no Play release)

Branch: `fix/tiktok-shortlink-youtube-fullscreen`  
PR: https://github.com/falker47/SocialViewer/pull/25  
Canonical checkpoint: https://trello.com/c/BlUYTg5m/15-social-viewer  
Baseline: `1cefa6d4f761e4436653817fcd9db157c2b3d9e1`, version **0.1.4 (5)**.

## What changed

The exact TikTok URL `https://vm.tiktok.com/ZGdQwpHav/` successfully redirects to
`https://www.tiktok.com/@carlocalendaofficial/photo/7690935067106905377?_r=1&_t=ZG-9AA1JMDqzMf`.
The canonical page returns HTTP 200, while its oEmbed metadata request returns HTTP 400.
This was observed on GitHub Actions with both the existing app user agent and a mobile-browser user agent;
it is not an inferred redirect bug.

Only a canonical `/@user/photo/id` plus oEmbed **400** now falls back to the official TikTok
image player without metadata. Video failures and photo 401/403/404/429/5xx are not hidden.
The shared redirect client, consent-cookie handling, third-party-cookie restrictions and player HTML remain unchanged.
The official player still decides whether a post is available; a real device must confirm image display/swiping.

YouTube's existing player fullscreen icon can invoke a native WebChromeClient custom-view window.
The provider custom view now occupies the fullscreen host directly: Social Viewer adds no toolbar or exit button,
so YouTube keeps the full available video height and its own native enter/exit controls.
Android Back still exits fullscreen before app navigation. The original WebView is not reloaded when entering/exiting.
Disposal, content replacement, duplicate requests and repeated entry remain handled.
The existing manifest already handles orientation/size changes; no forced orientation was added.

The first physical-device retest confirmed TikTok and YouTube fullscreen functionality. That retest also exposed a UI issue:
Social Viewer's redundant exit toolbar consumed too much vertical space. The current refinement removes only that toolbar;
a short second physical retest is still required to confirm the provider-only fullscreen layout on the final commit.

## Automated evidence and boundaries

- Baseline: [Android CI #184](https://github.com/falker47/SocialViewer/actions/runs/36704313495), all build steps passed.
- Actual redirect/oEmbed trace: [diagnostic job](https://github.com/falker47/SocialViewer/actions/runs/36704313495/job/109850831508).
- Test-first commit `dd06ded4d0d336db2f93b3479f7c1301f3c7fd89`:
  [CI #185](https://github.com/falker47/SocialViewer/actions/runs/36704773669) ran 105 tests;
  the three new photo-fallback cases and seven fullscreen cases failed as expected before implementation.
- Use the latest **completed** Android CI run on the PR head for final results, reports and artifact provenance.
- Robolectric tests execute native view/window/back/callback behavior on API 35. They do **not** run Chromium,
  authenticate to YouTube, prove live video playback, or replace the physical-device gate.
- CI intentionally has no production YouTube API key or release signing secrets (see `RELEASE.md`).
  Its `socialviewer-debug-keyless-*` APK is a verification artifact, **not a full YouTube retest build**.
- The AAB packaging step is skipped only for this device-retest branch. The existing check is retained for
  main and other PRs. No version, signing configuration, tester group or landing-page change is included.

## Prepare the device build from the already-configured local project

Use the existing local YouTube key and a debug signing certificate already authorized for the package.
Do not paste keys into chat, commit them, or add them to CI for this task.
From a fresh PowerShell window in the existing SocialViewer repository, this block creates an isolated
worktree without switching, resetting, or cleaning the existing checkout, then builds only a debug APK:

```powershell
$ErrorActionPreference = 'Stop'
$repo = git rev-parse --show-toplevel
if ($LASTEXITCODE -ne 0) { throw 'Apri PowerShell nella cartella del progetto SocialViewer.' }
$repo = $repo.Trim()
$origin = git -C $repo remote get-url origin
if ($LASTEXITCODE -ne 0 -or $origin -notmatch 'github\.com[:/]falker47/SocialViewer(?:\.git)?/?$') {
    throw 'Il remote origin non corrisponde a falker47/SocialViewer.'
}
git -C $repo fetch origin fix/tiktok-shortlink-youtube-fullscreen
if ($LASTEXITCODE -ne 0) { throw 'Fetch fallito: nessuna build avviata.' }
$head = (git -C $repo rev-parse FETCH_HEAD).Trim()
if ($LASTEXITCODE -ne 0) { throw 'Impossibile risolvere il commit da provare.' }
$qa = Join-Path (Split-Path $repo -Parent) ('SocialViewer-PR25-' + [guid]::NewGuid().ToString('N').Substring(0, 8))
git -C $repo worktree add --detach $qa $head
if ($LASTEXITCODE -ne 0) { throw 'Creazione worktree fallita.' }
$local = Join-Path $repo 'local.properties'
if (Test-Path -LiteralPath $local) { Copy-Item -LiteralPath $local -Destination (Join-Path $qa 'local.properties') }
Push-Location $qa
try {
    Write-Host "Commit da confrontare con HEAD della PR: $head"
    & .\gradlew.bat --no-daemon assembleDebug
    if ($LASTEXITCODE -ne 0) { throw 'Build debug fallita. Non installare APK precedenti.' }
    Write-Host "APK: $qa\app\build\outputs\apk\debug\app-debug.apk"
    Start-Process explorer.exe (Join-Path $qa 'app\build\outputs\apk\debug')
} finally {
    Pop-Location
}
```

The helper is a local preparation procedure, not a claim that this local environment was run by CI.
If the key is supplied through an environment variable or a user Gradle property instead of `local.properties`,
keep using that existing mechanism. An API key configured only for the Play signing certificate will not authorize
a debug-signed build: this is a configuration gate, not a fullscreen failure.

**Installation boundary:** the debug APK has the same application ID and a different signature from the Play app.
It cannot update the Play-signed installation in place. Do not automatically uninstall the Play app or change the
tester opt-in. Use the existing debug-test setup; replacing a Play installation on the same phone requires an
explicit local decision and can erase local preferences. No command above installs or uninstalls anything.

## Physical checks required from Mauri

1. **Reported TikTok photo:** paste the exact vm URL. Confirm the unexpected-error screen is gone and the official
   image post actually displays; test image navigation, consent and reopening. Also open a known-working canonical
   TikTok video and a vt short link to check regression behavior.
2. **YouTube:** open a known-working public, embeddable, non-Made-for-Kids video; start playback and use the player's
   fullscreen icon. Confirm the video now uses the full available height with **no Social Viewer toolbar/button**.
   Exit with YouTube's own fullscreen control, re-enter, then use the Android Back gesture/button: both must return
   to the normal player without going directly Home. Check portrait/landscape, repeated entry/exit, no unintended
   restart and no extra audio continuing after leaving the content.
3. **Shared WebView smoke:** open one previously working non-TikTok/non-YouTube provider, then return Home and open
   another item. Confirm no black fullscreen window, stuck loading overlay, lost consent choice or navigation regression.

Record tested commit SHA, device/Android/WebView versions, outcomes and screenshots/video for any failure in the
canonical checkpoint/PR. Keep the PR draft and unmerged until these checks are reported. A new Play release is a
separate, later authorization; no bundle or release preparation is required for this retest.

## Primary platform references

- TikTok Embed Player (video and image posts): https://developers.tiktok.com/doc/embed-player/
- Android WebChromeClient custom-view contract: https://developer.android.com/reference/android/webkit/WebChromeClient
