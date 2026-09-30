# CalorieTrack â€” Product Requirements Document (PRD)

## 1. Product Purpose
CalorieTrack is a lightweight, privacy-respecting, offline-first Android application designed to help individuals monitor their daily calorie intake and macronutrients (protein, carbohydrates, and fats) effortlessly.

Unlike mainstream calorie trackers that require account sign-ups, persistent internet connections, cloud subscriptions, and ad tracking, CalorieTrack operates entirely on the user's device with zero mandatory external dependencies.

---

## 2. Target User
- **Everyday Health & Fitness Enthusiasts**: People who want to track what they eat to lose, maintain, or gain weight without friction.
- **Privacy-Conscious Individuals**: Users who do not want their dietary habits, weight goals, or personal health metrics uploaded to corporate cloud databases.
- **Users with Intermittent or No Internet Connectivity**: People in areas with spotty cell reception, travelers, or those who keep mobile data off to conserve battery and data plans.
- **Users with Budget Android Hardware**: People using everyday smartphones with modest specs who need a snappy, lightweight app that doesn't drain their battery or lag.

---

## 3. Core User Problem
Most modern calorie-tracking apps (e.g., MyFitnessPal, LoseIt, Lifesum) suffer from severe product bloat:
1. **Mandatory Accounts**: Forcing users to create accounts before logging a single meal.
2. **Online-Only Gatekeeping**: Failing or hanging when the phone has poor or no internet connectivity.
3. **Aggressive Monetization**: Locking basic features like barcode scanning or macro tracking behind expensive recurring monthly subscriptions ($10â€“$20/month).
4. **Sluggish Performance & Battery Drain**: Heavy social feeds, advertising networks, and background trackers that make the app slow to open and navigate.

**CalorieTrack solves this by being fast, completely free to run, 100% offline, and focused strictly on the core logging experience.**

---

## 4. Primary User Journeys

### Journey A: First-Time Setup
1. User installs and opens CalorieTrack.
2. User is greeted with a clean, welcoming onboarding prompt to set their **Daily Calorie Target** (e.g., 2,000 kcal).
3. The target is saved locally, and the user lands on today's Dashboard.

### Journey B: Logging a Meal
1. User opens the app and taps **"+ Add Food"** under a meal category (Breakfast, Lunch, Dinner, Snack).
2. User types in a search bar (e.g., "Egg", "Rice", "Banana").
3. Instant search results appear from the local offline database.
4. User selects "Boiled Egg", adjusts the quantity/serving (e.g., 2 large eggs), and views the auto-calculated calories and macros.
5. User taps **"Log Food"**. The dashboard immediately updates to reflect the new calories consumed and remaining.

### Journey C: Reviewing Daily History
1. User opens the app at the end of the day or week.
2. User taps the **History** tab to see past days.
3. Each day clearly displays total calories consumed against the target, plus a breakdown of total protein, carbs, and fats.

---

## 5. Functional Requirements (V1)
- **FR-1: Daily Calorie Goal**: User can set and update a daily calorie target (kcal).
- **FR-2: Food Catalogue Search**: Real-time text search over an offline food catalogue.
- **FR-3: Serving Size & Quantity Adjustment**: Users can scale nutrition values by entering serving quantities (e.g., 1.5 cups, 200 grams, 3 items).
- **FR-4: Meal Categorization**: Grouping logged foods into 4 standard meals: Breakfast, Lunch, Dinner, and Snacks.
- **FR-5: Daily Nutrition Totals**: Automatic summation of total calories (kcal), protein (g), carbohydrates (g), and fat (g) for the day.
- **FR-6: Progress Visualization**: A clear visual indicator showing calories consumed vs. calories remaining.
- **FR-7: Daily Log History**: Browsing past days by date to review previous logs.
- **FR-8: Custom Food Creation**: Allowing users to create and save custom food items locally when a food isn't in the default catalogue.

---

## 6. Non-Functional Requirements
- **Performance Target**: Target search response of under 50ms using local SQLite indexes; target app launch time under 1.5 seconds.
- **Low Resource Target**: Targeted memory footprint of under 40 MB while idle; minimal CPU usage goal to preserve battery life on low-end devices.
- **Storage Footprint**: The app bundle and initial database combined should remain under 25 MB.
- **Stability**: Zero crashes when offline; graceful error recovery; data persistence even if the app process is abruptly killed by Android.
- **Compatibility**: Android 8.0 (API 26) through Android 16 (API 36).

---

## 7. Offline-First Requirement
- **Zero Server Dependency**: The app must provide 100% of its core logging, searching, calculation, and history viewing functionality without contacting any network server.
- **Local SQLite / Room Engine**: All data operations (insert, query, update, delete) are executed strictly on the phone's internal storage.
- **Airplane Mode Test**: The entire app must pass manual and automated verification in Airplane Mode with Wi-Fi and Cellular disabled.

---

## 8. Data Ownership & Privacy Principles
- **Your Phone, Your Data**: The user's nutritional records, body goals, and food history remain exclusively on their physical device.
- **No Telemetry / No Tracking**: No third-party analytical SDKs (no Google Analytics, no Firebase, no Facebook SDK, no ad trackers).
- **No Account Required**: The user never provides an email address, phone number, password, or name.

---

## 9. Nutrition Data Disclaimer
> [!IMPORTANT]
> CalorieTrack is an informational tracking tool and does not provide medical or clinical nutritional advice. Calorie and macronutrient estimates in the built-in database are derived from standard public reference catalogues (such as the USDA FoodData Central reference values). Actual nutritional content in branded or prepared foods may vary.

---

## 10. Definition of Success for V1
The V1 release is considered successful when:
1. A user can open the app with no internet connection, set their target, search a food from the local catalogue, and log it to a meal in **under 15 seconds**.
2. The dashboard updates accurately with correct math for calories, protein, carbs, and fats.
3. The user can close the app, restart the phone, reopen the app, and observe that all data persists accurately.
4. The APK builds reliably on GitHub Actions and can be installed cleanly on physical Android devices running API 26 through API 36.
