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

## Milestone 2A: Room Database Foundation & Calculations (Completed)
- **Goal**: Introduce local Room SQLite persistence and exact nutrition math utilities.
- **Tasks**:
  1. Add Android Room 2.8.5 dependencies and KSP configuration.
  2. Create Room entities: `FoodEntity`, `MealEntryEntity`, and `DailyGoalEntity`.
  3. Create DAOs (`FoodDao`, `MealEntryDao`, `DailyGoalDao`) with queries and Flow streams.
  4. Create `CalorieTrackDatabase` and singleton provider `DatabaseProvider`.
  5. Create `NutritionCalculator` and comprehensive unit test suite.
- **Verification**: `testDebugUnitTest` and `assembleDebug` pass cleanly.

---

## Milestone 2B: Offline Food Catalogue Asset (Completed)
- **Goal**: Bundle the pre-packaged offline food database into the APK.
- **Tasks**:
  1. Curate verified foundational foods (104 items from USDA FoodData Central) with deterministic IDs and 100g normalization.
  2. Structure dataset into `app/src/main/assets/source/food_catalogue.json`.
  3. Generate prepackaged SQLite asset `app/src/main/assets/database/calorietrack.db` with matching Room identity hash.
  4. Update `CalorieTrackDatabase` to version 2 with `Migration1To2` (seeding built-in foods on upgrade) and `.createFromAsset("database/calorietrack.db")`.
  5. Export database schemas (`room.schemaLocation`) via KSP.
  6. Create `FoodCatalogueValidationTest` and `Migration1To2Test` ensuring catalogue integrity, upgrade data preservation, and idempotency.
- **Verification**: `testDebugUnitTest` passes; fresh install and upgrade migration verified.



---

## Milestone 2C: Offline Food Search Experience (Completed)
- **Goal**: Enable instant, offline food searching backed by Room.
- **Tasks**:
  1. Build `FoodSearchScreen` with top app bar, search input textfield, clear action, and dynamic scrolling list.
  2. Implement `FoodSearchViewModel` observing `FoodDao.getAll()` on blank query and `FoodDao.searchByName(query)` on input.
  3. Contextually propagate `mealType` with clean subtitles ("Add to Breakfast", "Add to Snacks").
  4. Display item names, serving descriptions, calories per 100g, and macro breakdowns.
  5. Add intentional empty state when no items match the query.
  6. Fix Dashboard EmptyDayCard clickability, Snacks terminology, and bottom FAB scroll clearance.
  7. Add `FoodSearchViewModelTest` verifying state transitions.
- **Verification**: `testDebugUnitTest` and `assembleDebug` pass cleanly; search responds instantaneously without internet.

---

## Milestone 2D: Search Input Bug Fix & 500-Item Indian Catalogue Expansion (Completed)
- **Goal**: Resolve hardware keyboard/IME cursor jumps in search and expand the catalogue to 500 Indian-focused foods with bilingual search.
- **Tasks**:
  1. Fix the Compose `OutlinedTextField` IME cursor jumping issue by migrating to `rememberTextFieldState()` / `BasicTextField` with synchronous in-memory text state and reactive query propagation.
  2. Expand built-in offline catalogue from 104 to 500 items, strongly prioritizing an Indian-focused diet (grains, dals, dairy, vegetables, fruits, nuts, spices, dishes).
  3. Introduce Room Schema Version 3 with `search_keywords TEXT NOT NULL DEFAULT ''` and SQLite index `index_foods_search_keywords`.
  4. Implement non-destructive `Migration2To3` with automatic keyword backfilling, new item seeding, and user data preservation.
  5. Update `FoodDao.searchByName(query)` to query both `name` and `search_keywords` via SQL `LIKE`.
  6. Add unit tests in `FoodCatalogueValidationTest`, `Migration2To3Test`, and `FoodSearchViewModelTest` verifying 500 items, bilingual search keyword matching, sequential typing, and migration safety.
- **Verification**: `testDebugUnitTest` (34 tests) and `assembleDebug` pass cleanly; tested with bilingual keywords ("chole", "roti", "dahi", "paneer", "bhindi").

---

## Milestone 2E: Food Details, Quantity Selection & Meal Logging (Completed)
- **Goal**: Implement the end-to-end flow from search result click to food details, gram-based quantity selection, real-time nutrition calculation, meal logging, and reactive dashboard updates.
- **Tasks**:
  1. Build `FoodDetailsScreen` displaying food title, serving description, reference values per 100g, destination meal selector, gram quantity input with increment/decrement steppers & quick presets, and calculated nutrition preview cards.
  2. Implement `FoodDetailsViewModel` utilizing `NutritionCalculator` to dynamically calculate energy and macronutrients, reject zero/negative amounts, and preserve/switch destination `mealType`.
  3. Log meals directly into Room SQLite as `MealEntryEntity` with current date (`yyyy-MM-dd`), normalized `mealType`, food ID, exact quantity in grams, and calculated nutrition totals.
  4. Connect `Navigation.kt` with `FoodDetailsNavKey`, wiring food card clicks in `FoodSearchScreen` to navigate to `FoodDetailsScreen`, and popping back to `Main` upon logging.
  5. Upgrade `MainScreenViewModel` and `MainScreen` to reactively observe `MealEntryDao` and `DailyGoalDao` via Room `Flow`, displaying logged food items under their respective meal cards, updating consumed/remaining calories, macro progress indicators, and empty day cards.
  6. Add unit tests for 100g, 200g, arbitrary 37g scaling, zero/negative rejection, meal logging persistence, multiple meal entries grouping, and dashboard totals.
- **Verification**: `testDebugUnitTest` (47 tests) and `assembleDebug` pass cleanly; end-to-end meal logging verified offline without schema changes.

---

## Milestone 2F: Meal Details, Edit, and Delete (Completed)
- **Goal**: Implement dedicated meal management allowing users to view logged foods for any meal, edit quantities with recalculated nutrition, delete entries with confirmation, and see real-time updates across the app.
- **Tasks**:
  1. Build `MealDetailsScreen` displaying meal total summary (calories, protein, carbs, fat), entry cards with food name, quantity in grams, calories, compact macros `P · C · F`, edit/delete actions, and an empty meal state with `+ Add Food` action.
  2. Implement `MealDetailsViewModel` observing `MealEntryDao.getEntriesWithFoodForDateAndMealType(date, mealType)`, computing meal totals directly from stored entries, and managing delete confirmation dialog state.
  3. Expand `MealEntryDao` with `getEntryById(id)`, `deleteById(id)`, and `getEntriesWithFoodForDateAndMealType(date, mealType)` reactive Flow stream without database schema changes.
  4. Extend `FoodDetailsScreen` and `FoodDetailsViewModel` to support edit mode (`mealEntryId > 0L`), preloading existing entry details, updating the existing entry without creating duplicates, and displaying "Edit Entry" in the top bar.
  5. Update `Navigation.kt` and `NavigationKeys.kt` with `MealDetailsNavKey(mealType, date)`, wiring dashboard meal card taps to open `MealDetailsScreen` while keeping the `+` button direct shortcut to `FoodSearchScreen`, and returning to `MealDetailsScreen` upon edit completion.
  6. Add comprehensive unit tests in `FoodDetailsViewModelTest`, `MealDetailsViewModelTest`, and `MainScreenViewModelTest` verifying preloading, updating with identical ID, cancellation safety, deletion with confirmation, empty state when the final item is deleted, and immediate reactive dashboard reflection.
- **Verification**: `testDebugUnitTest` (55 tests) and `assembleDebug` pass cleanly; edit and delete workflows verified offline without schema changes.

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
