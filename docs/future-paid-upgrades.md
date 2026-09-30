# CalorieTrack â€” Future Monetization & Premium Strategy (Post-V1)

While CalorieTrack V1 is 100% free and offline with zero paywalls, this document outlines sustainable, user-respecting monetization models that can be explored in future versions without breaking the core offline promise.

---

## 1. Guiding Monetization Principles
1. **Core Tracking Stays Free Forever**: Logging food, viewing calories, tracking macros, and searching the local catalogue must never be locked behind a paywall.
2. **No Recurring Subscriptions for Offline Features**: Avoid predatory $15/month subscriptions. If a feature doesn't cost us ongoing server money to run, users shouldn't be charged a monthly subscription for it.
3. **One-Time "Pro" Lifetime Purchase**: A simple, fair one-time in-app purchase (e.g., $4.99â€“$9.99) to unlock premium convenience features.

---

## 2. Potential Future "Pro" Features (One-Time Unlock)

| Feature | Description | Implementation Concept |
| :--- | :--- | :--- |
| **Advanced Macro Targeting** | Set specific percentage splits (e.g., 40% Carbs, 30% Protein, 30% Fat) or grams per body weight. | Pure client-side logic. |
| **Custom Theme & Dark Mode** | AMOLED pitch-black theme, custom accent colors, and custom app icons. | Jetpack Compose dynamic theming. |
| **Encrypted CSV / JSON Backup**| One-tap export and import of all historical logs to Google Drive or local storage. | Local file serialization. |
| **Extended Offline Food Packs** | Downloadable specialist offline food databases (e.g., Regional cuisines, Keto-specific, Vegan brands). | Optional local SQLite asset packs. |
| **Widget Support** | Home-screen Android Glance widgets showing remaining calories at a glance. | AndroidX Glance Compose widgets. |

---

## 3. What We Will Avoid
- **No Ads**: Banner ads and interstitial video ads ruin user experience, drain phone battery, require tracking SDKs, and compromise privacy.
- **No Data Monetization**: We will never sell aggregated user data or dietary telemetry.
