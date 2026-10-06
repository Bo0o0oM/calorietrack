# CalorieTrack Backup Format Specification (Version 1)

## 1. Overview

CalorieTrack is designed as an offline, local-first application. To ensure users retain full ownership and portability of their dietary data across devices and installations without relying on external cloud providers, CalorieTrack provides an offline JSON backup format.

Backups capture application-level user data, strictly separated from the internal Room SQLite database file and the built-in 500-food catalogue.

---

## 2. Document Schema & Top-Level Fields

The backup document is a UTF-8 JSON object containing the following root fields:

| Field | Type | Description |
|:---|:---|:---|
| `backupFormatVersion` | Integer | Format specification version (currently `1`). |
| `applicationVersion` | String | Application version that generated the export (e.g. `"1.0"`). |
| `exportTimestamp` | String | ISO-8601 UTC timestamp of the export moment. |
| `customFoods` | Array | List of user-created custom foods (active and archived). |
| `recipes` | Array | List of user-created recipes (active and archived). |
| `recipeIngredients` | Array | Normalized recipe ingredient rows linking recipes to foods. |
| `mealEntries` | Array | Logged meal consumption records and historical snapshots. |
| `dailyGoals` | Array | Personal daily nutritional targets. |

---

## 3. Section Specifications

### 3.1. `customFoods`
Captures user-created custom foods (IDs $\ge 1000$). Built-in catalogue foods (IDs `1..999`) are **never** exported.

```json
{
  "id": 1000,
  "name": "Homemade Paneer",
  "servingDescription": "100 g",
  "servingGrams": 100.0,
  "caloriesPer100g": 265.0,
  "proteinPer100g": 18.0,
  "carbsPer100g": 6.0,
  "fatPer100g": 20.0,
  "isActive": true,
  "dataSource": "User",
  "sourceId": "",
  "searchKeywords": "paneer, cottage cheese"
}
```

### 3.2. `recipes`
Captures user-created multi-ingredient recipes.

```json
{
  "id": 1,
  "name": "My Shahi Paneer",
  "cookedWeightGrams": 300.0,
  "totalCalories": 650.0,
  "totalProtein": 36.8,
  "totalCarbs": 13.2,
  "totalFat": 52.0,
  "caloriesPer100g": 216.67,
  "proteinPer100g": 12.27,
  "carbsPer100g": 4.4,
  "fatPer100g": 17.33,
  "isActive": true,
  "searchKeywords": "curry, paneer"
}
```

### 3.3. `recipeIngredients`
Normalizes individual ingredient quantities. `foodId` may reference a built-in food (`1..999`) or a custom food (`≥ 1000`).

```json
{
  "id": 1,
  "recipeId": 1,
  "foodId": 1000,
  "quantityGrams": 200.0
}
```

### 3.4. `mealEntries`
Contains logged meal events. **Crucially, all historical nutritional snapshots and display identities are exported exactly as stored.**

```json
{
  "id": 1,
  "date": "2026-10-06",
  "mealType": "breakfast",
  "foodId": 0,
  "recipeId": 1,
  "entryName": "My Shahi Paneer",
  "quantityGrams": 150.0,
  "calories": 325.0,
  "protein": 18.4,
  "carbs": 6.6,
  "fat": 26.0
}
```

### 3.5. `dailyGoals`
Captures configured daily nutritional goals.

```json
{
  "id": 1,
  "date": "2026-10-06",
  "calorieGoal": 2200.0,
  "proteinGoal": 150.0,
  "carbsGoal": 260.0,
  "fatGoal": 75.0
}
```

---

## 4. Referential & Validation Invariants

Before any backup can be imported, it must pass strict automated validation:
1. `backupFormatVersion` must match supported versions (Version `1`).
2. Custom food IDs must be $\ge 1000$ and distinct.
3. Recipe IDs must be positive and distinct.
4. Recipe ingredient references must point to existing recipes and existing foods (built-in or custom).
5. Meal entries must reference either a valid food ID, a valid recipe ID, or contain a valid snapshot `entryName`.
6. Nutritional values and quantities must be finite, non-negative numbers.
7. Dates must conform to ISO-8601 `yyyy-MM-dd`.
8. Meal types must be standard categories: `breakfast`, `lunch`, `dinner`, or `snack`.

---

## 5. Restore Policy

- **No Destructive Drops**: Built-in food catalogue rows are never removed.
- **Atomic Replacement**: User-owned tables (`meal_entries`, `recipe_ingredients`, `recipes`, custom foods, `daily_goals`) are replaced inside a single atomic database transaction. If any error occurs, the entire operation is rolled back to the prior state.
- **Identity Preservation**: IDs and relationships from the backup are preserved without re-generation or recalculation.
