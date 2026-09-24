# Social Viewer — Facebook feasibility result

## Verdict

**NOT FEASIBLE** under the current product/privacy constraints.

## Evidence

Physical-device logged-out smoke testing was performed against the two real Facebook share-link families used by the product:

- `https://www.facebook.com/share/p/1GMwhxUrGi/`
- `https://www.facebook.com/share/r/1HNwyf2jVo/`

Both failed to produce a usable canonical Facebook target.

The tested resolution paths were:

1. normal logged-out HTTP redirect following;
2. a local temporary WebView restricted to URL identity only, observing main-frame navigation plus `rel=canonical` / `og:url`.

The browser-assisted spike did **not** extract post text, images, video, comments, or other content and did not require a Facebook account.

## Product conclusion

Canonical Facebook post/Reel URLs can still be handled by the experimental provider through Meta's official tokenless oEmbed endpoints, but canonical-only support does not satisfy the actual use case because real incoming links are `/share/p/` and `/share/r/`.

Therefore Facebook is **BLOCKED / UNSUPPORTED** for Social Viewer at this time.

Do not introduce further workaround layers in this milestone. In particular, do not add:

- Facebook login/account requirements;
- broader DOM/content scraping;
- backend or database resolution;
- third-party resolver services;
- remote/headless browser automation;
- embedded secrets or API keys.

## Branch / PR rule

`feature/facebook-provider` and PR #4 remain as the experimental record.

**Do not merge PR #4 as completed Facebook support.**

A future Facebook milestone may be reconsidered only if Meta exposes a durable logged-out path that can transform real share aliases into canonical public URLs without violating the product boundary.

TikTok and Instagram remain the verified supported providers on `main`.
