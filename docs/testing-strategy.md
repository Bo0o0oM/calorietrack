# CalorieTrack â€” Automated Testing Strategy

To guarantee that CalorieTrack remains reliable, crash-free, and mathematically accurate across updates, we define a clear, pyramid-style testing strategy tailored to an offline, Room-powered Android application.

---

## 1. Testing Pyramid Overview

```
          â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
          â”‚  UI & Integration Tests  â”‚  <-- 10%: Compose UI verification & flows
          â”œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¤
          â”‚  Room DAO / SQLite Tests â”‚  <-- 30%: Fast in-memory SQLite queries
          â”œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¤
          â”‚  Pure Unit & Math Tests  â”‚  <-- 60%: Instant JVM tests (0ms startup)
          â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

Because our PC has modest hardware (dual-core CPU, 8 GB RAM), we prioritize **JVM unit tests** that execute in seconds on the local machine and in GitHub Actions without spinning up emulators.

---

## 2. Test Suites & Specific Test Cases

### Suite 1: Nutrition & Macro Calculation Tests (Pure JVM Unit Tests)
Located in `app/src/test/java/...`: Tests the mathematical engine for scaling calories and macros.

| Test Case | Input | Expected Output | Rationale |
| :--- | :--- | :--- | :--- |
| **Standard Multiplier** | 100 kcal, 10g P, 20g C, 5g F $\times$ 1.5 | 150 kcal, 15g P, 30g C, 7.5g F | Verifies standard scaling math. |
| **Zero Quantity** | 100 kcal $\times$ 0.0 | 0 kcal, 0g P, 0g C, 0g F | Ensures zero input does not throw division errors or produce NaN. |
| **Fractional Quantity** | 78 kcal $\times$ 0.33 | 25.74 kcal | Verifies precision preservation with decimal inputs. |
| **Negative Input Guard** | 100 kcal $\times$ -1.5 | Error / Coerced to 0 | Prevents negative food logging from artificially subtracting calories. |
| **Excessive Quantity Guard** | 100 kcal $\times$ 10,000 | Validation error | Protects against accidental keyboard typos (e.g., logging 50,000 grams). |

---

### Suite 2: Meal & Daily Aggregation Tests (Pure JVM Unit Tests)
Tests summing logic for meals and the entire day.

| Test Case | Scenario | Expected Behavior |
| :--- | :--- | :--- |
| **Single Meal Total** | Breakfast has 2 eggs (140 kcal) + 1 toast (80 kcal). | Breakfast subtotal equals 220 kcal. |
| **Daily Grand Total** | Breakfast: 300 kcal, Lunch: 600 kcal, Dinner: 700 kcal, Snacks: 150 kcal. | Today's total equals 1,750 kcal. |
| **Remaining Calories** | Goal = 2000 kcal, Consumed = 1750 kcal. | Remaining equals 250 kcal. |
| **Over-Budget Calories** | Goal = 2000 kcal, Consumed = 2200 kcal. | Remaining equals 0 (or -200 with an over-budget indicator). |
| **Empty Day** | No meals logged for a new day. | All totals equal 0; zero crashes or null pointer exceptions. |

---

### Suite 3: Room Database & DAO Tests (Robolectric / Instrumented)
Tests SQLite queries, constraints, and cascading deletes.

| Test Case | Operation | Expected Verification |
| :--- | :--- | :--- |
| **Food Insert & Retrieve** | Insert built-in and custom foods into `foods`. | `getAllFoods()` returns both; `getCustomFoods()` returns only custom. |
| **Prefix Search Query** | Database contains "Banana", "Apple", "Bagel". Search query = `"ba"`. | Returns "Banana" and "Bagel" (case-insensitive); does not return "Apple". |
| **Daily Log Filtering** | Insert entries for `2026-09-29` and `2026-09-30`. Query for `2026-09-30`. | Returns strictly entries matching `2026-09-30`. |
| **Cascade Delete** | Delete a food item that has past meal entries. | Meal entry historical data is preserved (cached) or cleanly cascades based on FK definition. |
| **Date Ordering** | Log 3 items at different times. | Entries return ordered chronologically by `logged_at`. |

---

### Suite 4: UI State & ViewModel Tests
Tests the reactive presentation layer.

| Test Case | ViewModel State | Expected UI State |
| :--- | :--- | :--- |
| **Loading State** | Initial database connection initializing. | Shows subtle skeleton or loading placeholder without jarring jumps. |
| **Empty State (First Launch)**| User has logged zero foods today. | Shows friendly greeting: *"No foods logged today. Tap + to start your day!"* |
| **Search Empty State** | User types `"zzxxqq"` (no matching foods). | Shows *"Can't find 'zzxxqq'? Tap here to add custom food."* |
| **Error Recovery** | Malformed input entered in custom food dialog. | Shows inline validation error: *"Please enter valid calories"*; Save button disabled. |

---

## 3. How Tests Are Executed

1. **Locally (Pre-Commit)**:
   ```bash
   ./gradlew testDebugUnitTest --no-daemon
   ```
   Runs in **under 10 seconds** directly on the local machine without starting emulators or consuming significant RAM.

2. **Continuously on GitHub Actions**:
   Every push to `main` triggers our automated CI workflow, which runs `testDebugUnitTest` on GitHub's cloud runners before assembling the APK. If any calculation or test fails, the build breaks immediately, preventing a buggy APK from ever being generated.
