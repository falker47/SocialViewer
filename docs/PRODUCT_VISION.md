# SocialViewer — Product Vision

**Canonical identity (3 November 2026)**

- **Product name:** SocialViewer
- **Italian catchphrase:** Apri il contenuto, non il feed.
- **English catchphrase:** Open the content, not the feed.

## Mission and value

SocialViewer helps people open one specific public social-media post or video that friends or relatives have shared, even when the recipient does not use or want to install the originating social app. The value is **seeing what others send you without having to use the social network itself**.

It is not a place to browse many social networks or spend more time in them. The user should see the shared item and return to the original conversation.

## People and examples

- **Parents, grandparents and less experienced users:** someone receives a public TikTok video through WhatsApp but does not have TikTok installed. SocialViewer offers a focused way to view the supported item.
- **Social detox and intentional non-users:** someone uninstalls social apps to avoid distracting feeds but still wants to watch a public Reel or post sent by a friend.
- **Occasional recipients:** someone simply needs to open a single link without discovering profiles, recommendations or timelines.

None of these use cases requires a SocialViewer account.

## Primary journey

**Receive a link → tap it → SocialViewer opens the supported content → view it → return to the conversation.**

Android direct opening (ACTION_VIEW / **Open by default**) is the preferred route when the user's settings, the provider's domain and competing apps allow it. SocialViewer does not own the providers' domains or their `assetlinks.json` files and cannot guarantee automatic routing of every link.

**Alternatives:** manual paste and Android's Share menu. They are supported fallbacks, not the lead product story.

## Boundaries and limitations

SocialViewer handles **individual publicly accessible items** from implemented providers. It is **not** a cross-platform feed aggregator, a social-network client, an account manager, a messaging platform or a place for publishing, commenting, recommendations, infinite scrolling or social graphs.

It does **not** download, scrape, extract private/internal media URLs, proxy or rehost content. It cannot bypass logins, private visibility, age limits, geographic restrictions, removals, embedding rules or provider terms. **Not all public-looking items are viewable without a provider account**, and **not all links open automatically**. Provider-owned login prompts, recommendations or ads may still appear inside permitted embeds; do not promise that every provider-controlled UI element can be removed.

SocialViewer has no own account system, developer-operated backend, analytics SDK or viewing-history database. To display external content it contacts provider services, which may receive network/device information and use first-party site data. The precise disclosures belong to the [privacy policy](../PRIVACY.md) and must be reconciled with the Play-signed app before publication.

## Glossary

| Term | Meaning |
| --- | --- |
| **SocialViewer account** | An account in this app. None exists or is required. |
| **Provider account** | An account at TikTok, Instagram, YouTube or another source. A provider may require one for a given item. |
| **Provider app** | The originating social network's mobile app. The user need not install that app for items SocialViewer can display. |
| **Direct opening** | Android routing an eligible link to SocialViewer, subject to user-managed domain settings and other installed apps. |
| **Supported item** | A single public content URL recognized and displayable through a permitted provider integration. |

## Document roles

This file is the lasting **product-positioning reference**, not a live release or provider-status report. [README](../README.md) gives an overview; [Architecture](../ARCHITECTURE.md) explains technical choices; [Testing](../TESTING.md) defines checks; [Release](../RELEASE.md) records distribution procedures. Verify live build, CI, Google Play and provider states with their respective authoritative sources.
