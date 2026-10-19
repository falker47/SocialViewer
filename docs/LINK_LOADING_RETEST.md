# Link loading: evidence, candidate fix, and device gate

## Scope and status

This candidate fixes a reproduced cancellation/state defect and classifies
previously generic resolver failures. It does not establish the cause of every
TikTok failure reported on the phone. Keep the PR draft until CI and the physical
device checks below have passed. This is not a release, and no production version
bump or Play Console operation belongs to this candidate.

Baseline: `8fc3dc8e4b323e32261063ea61a126450a22ab80`, version 0.1.7 (8).
The main CI run [39044442626](https://github.com/falker47/SocialViewer/actions/runs/39044442626)
passed on this exact baseline. Full Android compilation is checked in PR CI;
the editing environment does not have an Android SDK prepared.

## Device evidence: 19 October 2026

### Initial 18:26 test, before Link QA

- The user reports inconsistent direct and pasted TikTok opens, Pinterest
  failures, and a separate YouTube unavailable message.
- The 18:26 screenshots confirm installed **0.1.7** and show the same native
  generic error after the requested full TikTok URL paste test. The clipboard
  toast is visible. The screenshot itself does not display the pasted URL.
- Therefore skipping short-link resolution did not resolve the reported test.
  The screenshot alone does not identify an HTTP status, timeout, JSON failure,
  or cancelled request. Do not assert a specific one without new evidence.

### Later Link QA and browser observations

- At 19:27, Link QA displayed TikTok metadata **HTTP 429**, identifying the
  oEmbed request before player creation. The screenshot shows 5G, but not the
  submitted URL or version.
- At 19:45, the screenshot after **Originale** shows Brave on TikTok with
  “La pagina non è disponibile”. The URL prefix matches the requested account;
  the full post ID and the page's HTTP status are not visible.
- A single remote oEmbed GET at 19:48:59 with app headers, no cookies and no
  automatic retry also returned **429**, with no `Retry-After` header. Earlier
  remote checks returned 200. The limiting scope, cause and duration are unknown;
  neither this result nor the browser message proves that the video was removed.
- At 20:00, the user reported browser errors for **5 out of 5 different TikTok
  links**. Their individual URLs/statuses were not provided; do not classify all
  five as oEmbed 429. Further TikTok retries were paused for this session.
- The 20:08 screenshot shows **SocialViewer Link QA** rejecting the pasted
  `https://it.pinterest.com/ansasketching/` profile on Home with “Questo servizio
  non è ancora supportato”, despite Pinterest being listed as supported. The
  user supplied the complete profile URL separately; invitation values are
  intentionally omitted here. This is a reproducible input-classification
  defect, distinct from network resolution.

### User-reported QA2 retests

- At 21:33, the user reported that the requested positive Pinterest comparison
  worked, and that retrying the TikTok link also worked. TikTok loading duration
  varied and was not measured. This does not establish that every earlier
  browser failure is resolved or that QA2 changed the earlier rate limit.
- At 21:56, the user confirmed the requested test of returning Home while TikTok
  was loading: Home remained usable, without the old result taking over.
- These are user-reported results in the QA2 test context; no new screenshot,
  version capture, per-phase timing, or HTTP trace accompanies them.

### Requested Home input control

At 21:56, the user requested a button beside Paste to clear the Home URL input.
QA3 adds an X icon with the localized accessible name **Cancella link** /
**Clear link**. It uses the existing input-change callback to clear both text
and inline validation error. The button remains visible but is disabled when
both are empty. An error can therefore be dismissed even with an empty field.
The shared Home content supplies the same control in compact and tall layouts.

### Earlier public URL checks

TikTok short link:

```text
https://vm.tiktok.com/ZGdCxGHGo/
```

Observed 301 destination, preserving all returned parameters for the comparison:

```text
https://www.tiktok.com/@futuronazionale_official/video/7697707868535180576?_r=1&_t=ZG-9AdgEc7opqS
```

Two independent unauthenticated checks with the app's User-Agent and Accept
headers reached HTTP 200 and obtained valid official oEmbed JSON, HTTP 200,
`type=video`, `embed_product_id=7697707868535180576`. The remote transport was
Python, not Android. Neither check validates WebView playback on the phone.

Pinterest:

```text
https://pin.it/1pTjG6L
```

Observed 308 to Pinterest's shortener API, then 302 and HTTP 200 at
`https://www.pinterest.com/ansasketching/` (invitation parameters omitted).
This is a profile, outside the single-Pin contract. AOSP API 26 and current
HttpURLConnection sources support GET redirects with 308; no 308-specific
transport failure was found. A different response on the phone remains possible.

#### Historical Pinterest fixture and current comparison

The user correctly recalled that `1pTjG6L` was supplied during our earlier tests
and had successful tap/paste attempts. Later failures were also recorded. Do not
attribute its use to the user copying the wrong content or deny the earlier
successes. The effective destination during those successful attempts was not
captured, so their exact cause cannot be reconstructed from the current response.

[PR #18](https://github.com/falker47/SocialViewer/pull/18) and `TESTING.md`
already retired this token as a fixed positive acceptance case: device traces
showed it ending at `/ansasketching/`, while a comparison alias reached a Pin.
The single-Pin boundary existed in the original Pinterest provider; this
candidate does not remove profile support that the provider previously promised.
Temporary user-agent, landing-page and diagnostic experiments were removed
before PR #18 was merged.

At 20:02:04 on 19 October, one current check reconfirmed `1pTjG6L` → profile.
At 20:15:14, one check of the historical positive comparison, with app headers
and no cookies/retries, returned **308 → 302 → 200**:

```text
https://pin.it/2J18qJxfQ
https://www.pinterest.com/pin/677721443976433349/sent/
```

The final URL normalizes to:

```text
https://www.pinterest.com/pin/677721443976433349/
```

This remote check verifies the redirect destination. The later 21:33 report
above supplies the user-reported positive phone result. Keep
`1pTjG6L` as an observed unsupported-destination case; do not hardcode either
token's presumed meaning into the resolver.

YouTube case retained separately:

```text
https://youtu.be/GQ1iOXFjH9w?si=temVM2LB9zUhsv8K
```

The URL policy accepts the ID and ignores `si`. Public oEmbed returned 401;
that does not distinguish the three current Data API rejection reasons or
reproduce the app's authenticated API request. This patch does not change
YouTube's public-only or embeddability requirements.

## Confirmed common defect

`LaunchedEffect(state)` cancels the previous load when the viewer state changes.
The broad catch treated that cancellation as an error and could publish the old
URL's error after Home or a newer successful result. An isolated Kotlin
2.2.10/coroutines 1.11.0 reproduction demonstrated both sequences. The candidate
propagates cancellation and checks active status before returning a load result.
It also publishes only while the captured loading request is still current:
the visible state can change before Compose disposes the old effect. Each
Loading instance has its own identity, including a new attempt for the same URL,
so effect keys and the publication guard agree. Regression tests exercise the
real suspend boundary and viewer state with controlled blocking IO.

Typed errors preserve existing provider acceptance decisions. They distinguish
ordinary network/timeout failures, HTTP failures, malformed TikTok metadata,
and unsupported Pinterest redirect destinations. UI strings contain only safe
categories, provider names and numeric status codes; no raw response bodies,
request tokens, stack traces, or browsing history are displayed or stored.

## Pinterest input classification

Previously, Home treated a null `providerFor()` result as an unknown service,
although provider support also validates the URL's content path. Consequently,
a direct regional Pinterest profile produced “service not supported”, while its
short-link equivalent could reach the specific Pin-required error. An explicitly
delivered intent had a third, generic error path.

The correction shares provider-owned rejection diagnostics between local input
validation and resolution. Pinterest profiles, boards and other non-Pin paths
still fail before content creation; direct unsupported paths do not trigger
HTTP. Manual typing, clipboard/share-text extraction and explicitly delivered
intents explain the same single-Pin requirement. Genuine unknown services retain
the unsupported-service message. Pinterest hosts, accepted Pin paths, redirects,
manifest claims, consent behavior and public-content gates remain unchanged.

## Isolated installable QA artifact

The PR CI produces `socialviewer-link-qa-<source-sha>` containing
`app-linkQa.apk`, provenance, SHA-256, and manifest checks.

- Label: **SocialViewer Link QA**.
- Package: `io.github.falker47.socialviewer.linkqa`.
- Version shown: `0.1.7-link-qa3`; versionCode 8. This adds the Home clear button
  to the input diagnostic correction in `0.1.7-link-qa2`.
- Release-initialized, R8 optimized and non-debuggable, signed with the Android
  debug certificate generated by CI. It is a separate app with separate local state.
- Install alongside Play SocialViewer. Do not uninstall the Play app, clear its
  site data, alter tester enrollment, or enable QA as a default link handler.
- CI has no production YouTube key. A keyless QA YouTube configuration error
  is expected and is not a result for the production YouTube issue.
- A future CI artifact may use a different debug certificate. If Android refuses
  to update an older Link QA installation, uninstall **only SocialViewer Link QA**
  and install the new QA APK. Keep the Play SocialViewer installation intact.

## Next phone check

1. Open **SocialViewer Link QA** from its icon. On first launch choose
   **Avanti → Non ora** to skip direct-link configuration. Enter text in the Home
   field, then tap the X beside Paste. The field must become empty.
2. Enter an invalid link and press Open to show its inline error. Tap the X:
   both text and error must disappear. With neither text nor error the X must
   be disabled. Confirm the accessible label in Italian and English when checking
   those locales.
3. Confirm Paste still opens the copied link. The positive comparison
   `https://pin.it/2J18qJxfQ` and the TikTok retry have user-reported QA2 passes;
   earlier 429 observations do not justify automatic retries or bypassing oEmbed.

## Remaining acceptance before merge

- Returning Home during a pending TikTok load has a user-reported QA2 pass.
  Opening another link while a load is pending still needs its own observation:
  the older result must not replace the newer one.
- Confirm the new QA3 clear control using the phone checks above.
- Retrying a genuine resolver failure must produce the new attempt's result.
- The direct Pinterest profile and its short-link equivalent must receive a
  consistent single-Pin explanation. The positive comparison has a user-reported
  QA2 pass; its remote redirect probe alone would not close that check.
- Exercise IT/EN text, Original, Back, a known-good TikTok video, and the existing
  TikTok photo fallback. Check the actual optimized QA artifact, not debug only.
- Direct intents require an isolated device or explicit targeting; do not steal
  Play domain associations on the main phone merely for this check.
- Production YouTube verification remains on a build with its existing authorized
  key and signing identity; no API access restrictions are weakened for QA.

Automated tests and a successful public endpoint probe cannot substitute for
these phone observations. If the new TikTok message still lacks a useful category,
inspect that observed error before adding speculative provider fallbacks.
