# CalorieTrack â€” MVP Scope & Feature Prioritization

To keep the first release fast, lightweight, and rock-solid on lower-end hardware, CalorieTrack follows strict scope boundaries. Features are categorized using the MoSCoW method (Must Have, Should Have, Later).

---

## 1. MUST HAVE (V1 Core Release)

These features form the baseline of a functioning, enjoyable offline calorie tracker. Without these, the app cannot launch.

| Feature | Description | Technical Implementation |
| :--- | :--- | :--- |
| **Daily Calorie Target** | Ability to set and edit a daily calorie goal (e.g. 2000 kcal). | Room `DailyGoal` table or persistent key-value store. |
| **Local Food Catalogue** | Pre-bundled database of ~100â€“150 common foods with verified calories and macros. | SQLite database pre-populated in app assets. |
| **Instant Food Search** | Real-time search by food name as the user types. | SQLite `LIKE` or FTS query indexed on food name. |
| **Food Details & Servings** | View nutrition for a selected food and multiply values by quantity (e.g., 1.5 servings). | Pure Kotlin math: `base_nutrient * quantity`. |
| **Meal Categorization** | Log foods into Breakfast, Lunch, Dinner, or Snacks. | Room `MealEntry` table with `meal_type` column. |
| **Daily Calorie & Macro Totals**| Sum total calories, protein, carbs, and fat consumed today. | SQLite `SUM()` query grouped by date. |
| **Progress Dashboard** | Clean visual representation of consumed vs. remaining calories. | Jetpack Compose progress bars and stat cards. |
| **Daily History** | Calendar or day-picker to review meal logs from previous dates. | Room query filtering by ISO-8601 date string (`YYYY-MM-DD`). |
| **Custom Food Creation** | Simple form allowing users to add unlisted foods locally. | Room `Food` table with `is_custom = 1`. |

---

## 2. SHOULD HAVE (V1.1 Quick Follow-Up)

Features that substantially improve day-to-day usability without bloating the app architecture.

- **Recent Foods List**: Quick-add list showing the last 10â€“15 foods the user logged, avoiding repetitive searching.
- **Favorite Foods**: Ability to "star" a food item so it appears at the top of the search screen.
- **Delete / Edit Logged Item**: Swipe-to-delete or tap-to-adjust serving for an already-logged item.
- **Copy Yesterday's Meal**: Button to quickly duplicate a meal (e.g., copying yesterday's breakfast to today).
- **Water Intake Tracker**: A minimal local counter for daily glasses of water.

---

## 3. LATER (Future Roadmap / Post-V1)

Features explicitly deferred to keep V1 fast, stable, and focused on core mechanics.

- **Offline Camera Barcode Scanner**: Local on-device barcode recognition (using ML Kit offline barcode models) without cloud dependencies.
- **Local Data Export & Backup**: Exporting food logs to a plain CSV file or importing a backup JSON file from the phone's SD card / Documents folder.
- **Weight Tracking & Charts**: Daily weigh-in logging and progress line graphs over weeks/months.
- **Expanded Food Datasets**: Expandable offline datasets (thousands of items) packaged as optional downloadable SQLite packs.
- **Dark Mode / Dynamic Color**: Material You dynamic theming matching the Android wallpaper.

---

## 4. WHAT WE WILL NOT BUILD (Anti-Goals)

To prevent feature creep, corporate bloat, and unexpected maintenance costs, the following will **never** be part of CalorieTrack's core architecture:

| Prohibited Feature | Rationale |
| :--- | :--- |
| **User Accounts & Logins** | Requires authentication servers, passwords, token refresh, and privacy liability. |
| **Cloud Databases (Firebase/Supabase)**| Introduces network latency, offline sync conflicts, and recurring hosting bills. |
| **AI Chatbots & Generative AI** | Costly API tokens, high latency, requires internet, prone to hallucinations on nutrition. |
| **Paid Third-Party Nutrition APIs** | High API subscription fees ($100â€“$500/month), rate limits, and breaks offline usage. |
| **Subscriptions & Paywalls** | Ruins user trust. The core app should remain 100% free and functional. |
| **Social Feeds & Community Boards** | Unnecessary bloat that distracts from personal health tracking. |
