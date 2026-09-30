# CalorieTrack â€” Step-by-Step Implementation Roadmap

This roadmap breaks development into small, incremental, self-contained milestones. Each milestone can be coded, tested, verified on GitHub Actions, and test-driven on a physical phone before moving to the next.

---

## Milestone 1: Application Shell & Dashboard UI Wireframe
- **Goal**: Replace the template `"Hello Android!"` screen with the visual skeleton of CalorieTrack.
- **Tasks**:
  1. Build the top app bar with app title, date indicator, and icons for History & Settings.
  2. Implement the calorie summary card (Daily Target, Consumed, Remaining).
  3. Implement the macro pills (Protein, Carbs, Fat).
  4. Create the 4 meal section cards (Breakfast, Lunch, Dinner, Snacks) with dummy data and `+ Add Food` buttons.
- **Verification**: App builds cleanly in CI; phone displays the complete visual structure of the dashboard.

---

## Milestone 2: Room Database & Core Schema
- **Goal**: Introduce local SQLite persistence to the application.
- **Tasks**:
  1. Add Android Room dependencies to `gradle/libs.versions.toml` and `app/build.gradle.kts`.
  2. Create the 3 Room entities: `FoodEntity`, `MealEntryEntity`, and `DailyGoalEntity`.
  3. Create DAOs (`FoodDao`, `MealEntryDao`, `DailyGoalDao`) with basic CRUD queries.
  4. Create `AppDatabase` class and a singleton repository to access it.
- **Verification**: Write unit tests verifying that in-memory database inserts and queries return expected data.

---

## Milestone 3: Seed Initial Food Catalogue
- **Goal**: Bundle the pre-packaged offline food database into the APK.
- **Tasks**:
  1. Create a curated list of ~100â€“150 foundational foods (standard USDA reference values for everyday staples).
  2. Structure the dataset into an embedded SQLite database or asset seed file in `app/src/main/assets/`.
  3. Configure Room's `createFromAsset()` to pre-populate the database on first run.
- **Verification**: Database initializes on first app open with 100+ foods pre-populated; unit tests verify food count.

---

## Milestone 4: Food Search Screen
- **Goal**: Enable instant, offline food searching.
- **Tasks**:
  1. Build the `FoodSearchScreen` composable with a search text field and dynamic results list.
  2. Implement `searchFoods(query: String)` in `FoodDao` using indexed SQLite `LIKE` queries.
  3. Connect search results to a `SearchViewModel` exposing real-time search state.
  4. Add empty-state UI for when a search term has no results.
- **Verification**: Typing "egg" or "rice" instantly displays matching foods with calorie tags in under 50ms.

---

## Milestone 5: Food Details & Serving Selection
- **Goal**: View nutrition information and scale servings.
- **Tasks**:
  1. Build the `FoodDetailScreen` showing food title, base serving, calories, and macros.
  2. Create the quantity adjustment input (number stepper and text box).
  3. Implement real-time mathematical scaling: dynamic calories/macros update as the user adjusts the quantity.
- **Verification**: Changing serving from 1.0 to 2.5 multiplies calories and macros accurately on screen; unit tests verify calculation engine.

---

## Milestone 6: Meal Logging & Persistence
- **Goal**: Save selected foods into the daily log.
- **Tasks**:
  1. Add a **"Log Food"** button that creates a `MealEntryEntity` and saves it into Room SQLite.
  2. Connect navigation to pop back to the Dashboard after logging.
  3. Support deleting logged items (swipe-to-delete or tap to remove).
- **Verification**: Logging an item saves it into the local database and persists across app restarts.

---

## Milestone 7: Live Daily Calorie & Macro Dashboard
- **Goal**: Wire the Dashboard to observe real database entries reactively.
- **Tasks**:
  1. Replace Dashboard dummy data with reactive Kotlin `Flow` streams from `MealEntryDao`.
  2. Implement automatic calculation of consumed calories, remaining calories, and macro subtotals.
  3. Display logged items under their respective meal sections (Breakfast, Lunch, Dinner, Snacks).
- **Verification**: Logging an item immediately updates the dashboard progress bar without requiring a manual refresh.

---

## Milestone 8: Daily History & Day Navigation
- **Goal**: Review logs from past days.
- **Tasks**:
  1. Add date-switching controls (left/right day arrows and a date picker) on the Dashboard.
  2. Query `MealEntryDao` filtering by the selected date string (`YYYY-MM-DD`).
  3. Create a dedicated `HistoryScreen` displaying a calendar or list of past days with calorie totals.
- **Verification**: Navigating to yesterday displays yesterday's meal logs; navigating to today returns to today's log.

---

## Milestone 9: User-Created Custom Foods
- **Goal**: Allow users to enter foods that aren't in the default catalogue.
- **Tasks**:
  1. Build the `AddCustomFoodScreen` form (Name, Serving Unit, Calories, Protein, Carbs, Fat).
  2. Insert custom foods into the `foods` table with `is_custom = 1`.
  3. Add a visual "Custom" badge in search results so users can distinguish their own items.
- **Verification**: Custom foods appear immediately in future search results and can be logged to meals like built-in foods.

---

## Milestone 10: Zero-Cost Web Lookup Fallback
- **Goal**: Provide an easy web search shortcut when a food is not in the database.
- **Tasks**:
  1. On the "No results found" search screen, add a **"Search Web for Calories"** action.
  2. Trigger Android's native browser Intent (`Intent.ACTION_VIEW`) to open Google search for `"calories in [food name]"`.
  3. Provide a quick button to pre-fill the Custom Food form with the searched name upon returning to the app.
- **Verification**: Tapping "Search Web" opens the phone browser with nutrition results; returning to the app allows fast manual entry in seconds.

---

## Milestone 11: Polish, Accessibility & Release Preparation
- **Goal**: Final quality assurance, error handling, and release readiness.
- **Tasks**:
  1. Edge-case hardening: handle negative numbers, decimal inputs, and long food names gracefully.
  2. Ensure talkback accessibility labels, high-contrast text, and smooth Compose transitions.
  3. Verify clean operation on lower-end devices (memory usage, CPU footprint).
  4. Final automated CI test pass on GitHub Actions.
- **Verification**: Flawless, crash-free performance in Airplane Mode across multiple physical test devices.
