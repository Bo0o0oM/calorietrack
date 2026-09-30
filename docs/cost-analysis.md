# CalorieTrack â€” Infrastructure & Operating Cost Analysis

As a solo founder launching an initial product, ongoing server bills, cloud subscriptions, and API paywalls represent an existential financial risk. This document breaks down the operating costs of CalorieTrack for V1.

---

## 1. V1 Recurring Operating Costs

| Service / Resource | Provider | Monthly Cost (USD) | Notes |
| :--- | :--- | :---: | :--- |
| **Backend Servers (Compute)** | None | **$0.00** | Architecture is 100% local-first; no servers. |
| **Cloud Database** | None (Local SQLite) | **$0.00** | Data lives on user's phone flash storage. |
| **User Authentication** | None | **$0.00** | No accounts, no Auth0, no Firebase Auth. |
| **Third-Party Nutrition APIs** | None (Pre-bundled USDA) | **$0.00** | Local seed dataset; no recurring API fees. |
| **AI / LLM API Tokens** | None | **$0.00** | Pure deterministic math; no OpenAI / Claude calls. |
| **CI / CD Automated Builds** | GitHub Actions | **$0.00** | Free tier provides 2,000 monthly minutes for public repos. |
| **Code Hosting & Git** | GitHub | **$0.00** | Free public repository. |
| **Total Monthly Operating Cost**| â€” | **$0.00 / month** | **Completely free to run indefinitely.** |

---

## 2. One-Time Setup Costs

| Item | Cost (USD) | Status / Timing |
| :--- | :---: | :--- |
| **Google Play Developer Account** | **$25.00** (one-time fee) | Required only when ready to publish publicly to Google Play. |
| **Development Machine** | **$0.00** | Existing Windows 10 PC (dual-core, 8 GB RAM). |
| **Test Hardware** | **$0.00** | Existing Redmi K20 Pro physical Android phone. |

---

## 3. Comparison With Competitors

Traditional fitness apps incur steep operational overhead:
- **Competitor Monthly Burn**: $500 to $2,500/month for Nutritionix/Edamam API access + AWS cloud database hosting + Firebase authentication.
- **CalorieTrack Advantage**: **Zero fixed or variable cloud costs**. Whether the app has 10 users or 100,000 users, our server bill is **$0.00**. Every user's device provides its own compute and storage for free.
