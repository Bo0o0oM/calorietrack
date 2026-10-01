# CalorieTrack â€” Local Food Data Strategy

This document details how CalorieTrack manages its offline food catalogue: how foods are packaged, seeded into SQLite, identified, and scaled over time without requiring cloud APIs or schema redesigns.

---

## 1. Packaging Strategy: The Asset-Seeded Database

To guarantee 100% offline functionality on the very first app launch, the initial food catalogue is pre-packaged directly inside the APK as an embedded asset.

### How Room Pre-populates the Database
Android Room natively supports database pre-population from an asset file via the `createFromAsset()` API:

```kotlin
Room.databaseBuilder(context, CalorieTrackDatabase::class.java, "calorietrack.db")
    .createFromAsset("database/calorietrack.db")
    .addMigrations(CalorieTrackDatabase.Migration1To2(context))
    .build()
```

### Benefits of Pre-Bundling:
1. **Zero First-Run Download**: The app doesn't need to ask the user to wait while downloading hundreds of megabytes on first launch.
2. **Instant Search Readiness**: The moment the app opens, the SQLite search indexes are already compiled and queryable.
3. **Low Storage Impact**: A compressed initial database of 104 foundational foods occupies only **~60 KB** in the APK assets.

---

## 2. Food Identification & Differentiation

Every food in the database is uniquely identified by an integer Primary Key (`id`), but distinguished by an `is_custom` flag:

```
[ foods Table ]
 â”œâ”€â”€ id: 101, name: "Egg, whole, boiled", is_custom: 0  <-- Built-in reference food
 â””â”€â”€ id: 102, name: "Protein Shake",     is_custom: 1  <-- User-created custom food
```

### Built-in Foods (`is_custom = 0`):
- Read-only reference foods bundled with the app.
- Protected from accidental deletion so the baseline catalogue is never corrupted.
- Sourced from standardized public nutrition datasets (USDA FoodData Central).
- Deterministic IDs assigned sequentially from 1 to 104.

### Custom Foods (`is_custom = 1`):
- Created locally by the user when an item is not found in the reference catalogue.
- Can be freely edited, updated, or deleted by the user at any time.
- Filterable in search results with a special badge (e.g., *"Custom"* or *"My Food"*).
- IDs automatically generated starting at 1000 to prevent collisions with built-in foods.

---

## 3. Data Source Attribution & Initial V1 Dataset

For the V1 release (Milestone 2B), we bundled a curated set of **104 foundational, everyday foods** across common categories:
- **Proteins**: Eggs, chicken breast, salmon, tofu, Greek yogurt, canned tuna, beef mince, turkey breast, pork chop, cottage cheese.
- **Carbohydrates & Grains**: White rice, brown rice, rolled oats, whole wheat bread, white bread, quinoa, potatoes, sweet potatoes, pasta, corn.
- **Fruits & Vegetables**: Bananas, apples, oranges, blueberries, strawberries, spinach, broccoli, carrots, onions, tomatoes, avocados, cucumber, bell pepper.
- **Dairy & Alternatives**: Whole milk, skim milk, almond milk, soy milk, cheddar cheese, mozzarella, butter.
- **Nuts & Oils**: Olive oil, coconut oil, peanut butter, almonds, walnuts, chia seeds.
- **Legumes & Snacks**: Black beans, chickpeas, lentils, dark chocolate, hummus, popcorn.

### Source Attribution
The nutritional values for built-in foods are sourced exclusively from public-domain, authoritative nutritional databases:
- **USDA FoodData Central (FDC)**: Standard reference data compiled by the United States Department of Agriculture (public domain).
- Built-in foods are normalized to standard metric units (`100g` base serving) alongside intuitive household units (e.g., *"1 medium (118g)"* for a banana or *"1 large (50g)"* for an egg).
- Both raw reference data (`app/src/main/assets/source/food_catalogue.json`) and compiled SQLite binary (`app/src/main/assets/database/calorietrack.db`) are tracked in source assets.


---

## 4. Scaling to Thousands of Foods (Without Redesigning the Schema)

The schema designed in `docs/database-design.md` was intentionally crafted so that expanding from 100 foods to 50,000 foods requires **zero database migrations or architectural redesigns**:

1. **Normalized Nutrition Columns**: Calories, protein, carbs, and fat are clean numeric columns (`REAL`), which query at the same speed whether the table has 100 rows or 100,000 rows.
2. **SQLite B-Tree Indexes**: The index on `foods(name)` allows SQLite to find matching text prefixes in logarithmic time ($O(\log N)$), with a target search execution time of under 15 milliseconds on a mobile CPU.
3. **Future Full-Text Search (FTS5)**: When scaling to a large catalogue of 20,000+ items, SQLite's built-in `FTS5` (Full-Text Search) module can be enabled on the `foods` table with a one-line Room annotation (`@Fts4` or `@Fts5`). This unlocks fuzzy search and multi-word matching (e.g., finding *"Greek yogurt low fat"* from *"yogurt greek"*) without changing the rest of the application.
4. **Offline Update Packs**: In future versions, updated or expanded food catalogues can be distributed as compressed local database patches or SQLite files during app updates without clearing the user's historical meal logs.
