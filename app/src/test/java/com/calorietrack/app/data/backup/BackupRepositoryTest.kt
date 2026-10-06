package com.calorietrack.app.data.backup

import com.calorietrack.app.data.local.DailyGoalDao
import com.calorietrack.app.data.local.DailyGoalEntity
import com.calorietrack.app.data.local.DailyNutritionTotals
import com.calorietrack.app.data.local.DailySummary
import com.calorietrack.app.data.local.FoodDao
import com.calorietrack.app.data.local.FoodEntity
import com.calorietrack.app.data.local.MealEntryDao
import com.calorietrack.app.data.local.MealEntryEntity
import com.calorietrack.app.data.local.MealEntryWithFood
import com.calorietrack.app.data.local.RecipeDao
import com.calorietrack.app.data.local.RecipeEntity
import com.calorietrack.app.data.local.RecipeIngredientEntity
import com.calorietrack.app.data.local.RecipeIngredientWithFood
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupRepositoryTest {

  private class TestDatabaseState {
    val foods = mutableMapOf<Long, FoodEntity>()
    val recipes = mutableMapOf<Long, RecipeEntity>()
    val ingredients = mutableMapOf<Long, RecipeIngredientEntity>()
    val mealEntries = mutableMapOf<Long, MealEntryEntity>()
    val dailyGoals = mutableMapOf<String, DailyGoalEntity>()

    val foodDao = object : FoodDao {
      override suspend fun getById(id: Long): FoodEntity? = foods[id]
      override fun getAll(): Flow<List<FoodEntity>> = flowOf(foods.values.filter { it.isActive })
      override fun searchByName(query: String): Flow<List<FoodEntity>> = flowOf(emptyList())
      override fun getMyFoods(): Flow<List<FoodEntity>> = flowOf(foods.values.filter { it.isCustom && it.isActive })
      override fun searchMyFoods(query: String): Flow<List<FoodEntity>> = flowOf(emptyList())
      override suspend fun insert(food: FoodEntity): Long { foods[food.id] = food; return food.id }
      override suspend fun insertAll(foodsList: List<FoodEntity>) { foodsList.forEach { foods[it.id] = it } }
      override suspend fun update(food: FoodEntity) { foods[food.id] = food }
      override suspend fun delete(food: FoodEntity) { foods.remove(food.id) }
      override suspend fun archiveFood(id: Long): Int = 0
      override suspend fun getMaxId(): Long? = foods.keys.maxOrNull()
      override suspend fun count(): Int = foods.size
      override suspend fun countActive(): Int = foods.values.count { it.isActive }
      override suspend fun countActiveCustom(): Int = foods.values.count { it.isCustom && it.isActive }
      override suspend fun getAllCustomFoods(): List<FoodEntity> = foods.values.filter { it.isCustom }
      override suspend fun deleteAllCustomFoods(): Int {
        val count = foods.values.count { it.isCustom }
        foods.entries.removeAll { it.value.isCustom }
        return count
      }
    }

    val recipeDao = object : RecipeDao {
      override suspend fun getRecipeById(id: Long): RecipeEntity? = recipes[id]
      override fun getAllActiveRecipes(): Flow<List<RecipeEntity>> = flowOf(recipes.values.filter { it.isActive })
      override fun searchActiveRecipes(query: String): Flow<List<RecipeEntity>> = flowOf(emptyList())
      override fun getIngredientsWithFood(recipeId: Long): Flow<List<RecipeIngredientWithFood>> = flowOf(emptyList())
      override suspend fun getIngredientsForRecipe(recipeId: Long): List<RecipeIngredientEntity> =
        ingredients.values.filter { it.recipeId == recipeId }
      override suspend fun insertRecipe(recipe: RecipeEntity): Long { recipes[recipe.id] = recipe; return recipe.id }
      override suspend fun updateRecipe(recipe: RecipeEntity) { recipes[recipe.id] = recipe }
      override suspend fun insertIngredients(ingredientsList: List<RecipeIngredientEntity>) {
        ingredientsList.forEach { ingredients[it.id] = it }
      }
      override suspend fun deleteIngredientsForRecipe(recipeId: Long) {
        ingredients.entries.removeAll { it.value.recipeId == recipeId }
      }
      override suspend fun archiveRecipe(recipeId: Long): Int = 0
      override suspend fun countActiveRecipes(): Int = recipes.values.count { it.isActive }
      override suspend fun saveRecipeWithIngredients(recipe: RecipeEntity, ingredientsList: List<RecipeIngredientEntity>): Long = recipe.id
      override suspend fun getAllRecipes(): List<RecipeEntity> = recipes.values.toList()
      override suspend fun getAllRecipeIngredients(): List<RecipeIngredientEntity> = ingredients.values.toList()
      override suspend fun deleteAllRecipeIngredients(): Int {
        val size = ingredients.size
        ingredients.clear()
        return size
      }
      override suspend fun deleteAllRecipes(): Int {
        val size = recipes.size
        recipes.clear()
        return size
      }
      override suspend fun insertRecipes(recipesList: List<RecipeEntity>) {
        recipesList.forEach { recipes[it.id] = it }
      }
    }

    val mealEntryDao = object : MealEntryDao {
      override fun getEntriesForDate(date: String): Flow<List<MealEntryEntity>> = flowOf(emptyList())
      override fun getEntriesForDateAndMealType(date: String, mealType: String): Flow<List<MealEntryEntity>> = flowOf(emptyList())
      override suspend fun insert(entry: MealEntryEntity): Long { mealEntries[entry.id] = entry; return entry.id }
      override suspend fun update(entry: MealEntryEntity) { mealEntries[entry.id] = entry }
      override suspend fun delete(entry: MealEntryEntity) { mealEntries.remove(entry.id) }
      override suspend fun deleteForDate(date: String) {}
      override fun observeDailyTotals(date: String): Flow<DailyNutritionTotals> = flowOf(DailyNutritionTotals(0.0, 0.0, 0.0, 0.0))
      override fun getEntriesWithFoodForDate(date: String): Flow<List<MealEntryWithFood>> = flowOf(emptyList())
      override suspend fun getEntryById(id: Long): MealEntryEntity? = mealEntries[id]
      override suspend fun deleteById(id: Long) { mealEntries.remove(id) }
      override fun getEntriesWithFoodForDateAndMealType(date: String, mealType: String): Flow<List<MealEntryWithFood>> = flowOf(emptyList())
      override fun observeAllDailyTotals(): Flow<List<DailySummary>> = flowOf(emptyList())
      override fun getDatesWithEntries(): Flow<List<String>> = flowOf(emptyList())
      override suspend fun getAllMealEntries(): List<MealEntryEntity> = mealEntries.values.toList()
      override suspend fun deleteAllMealEntries(): Int {
        val size = mealEntries.size
        mealEntries.clear()
        return size
      }
      override suspend fun insertAll(entries: List<MealEntryEntity>) {
        entries.forEach { mealEntries[it.id] = it }
      }
    }

    val dailyGoalDao = object : DailyGoalDao {
      override suspend fun getForDate(date: String): DailyGoalEntity? = dailyGoals[date]
      override fun observeForDate(date: String): Flow<DailyGoalEntity?> = flowOf(null)
      override suspend fun insert(goal: DailyGoalEntity): Long { dailyGoals[goal.date] = goal; return 1L }
      override suspend fun update(goal: DailyGoalEntity) { dailyGoals[goal.date] = goal }
      override suspend fun upsert(goal: DailyGoalEntity) { dailyGoals[goal.date] = goal }
      override fun getAllGoals(): Flow<List<DailyGoalEntity>> = flowOf(dailyGoals.values.toList())
      override fun getDatesWithGoals(): Flow<List<String>> = flowOf(emptyList())
      override suspend fun getAllDailyGoals(): List<DailyGoalEntity> = dailyGoals.values.toList()
      override suspend fun deleteAllDailyGoals(): Int {
        val size = dailyGoals.size
        dailyGoals.clear()
        return size
      }
      override suspend fun insertAll(goals: List<DailyGoalEntity>) {
        goals.forEach { dailyGoals[it.date] = it }
      }
    }
  }

  private lateinit var dbState: TestDatabaseState
  private lateinit var repository: BackupRepository

  @Before
  fun setUp() {
    dbState = TestDatabaseState()
    repository = BackupRepository(
      foodDao = dbState.foodDao,
      recipeDao = dbState.recipeDao,
      mealEntryDao = dbState.mealEntryDao,
      dailyGoalDao = dbState.dailyGoalDao,
      transactionRunner = { block -> block() },
      timestampProvider = { "2026-10-06T12:00:00Z" },
    )
  }

  @Test
  fun createBackup_includesAllUserOwnedDataAndExcludesBuiltInFoods() = runTest {
    // 1. Built-in food (ID 1)
    dbState.foods[1L] = FoodEntity(
      id = 1L,
      name = "Apple",
      servingDescription = "100 g",
      servingGrams = 100.0,
      caloriesPer100g = 52.0,
      proteinPer100g = 0.3,
      carbsPer100g = 14.0,
      fatPer100g = 0.2,
      isCustom = false,
    )

    // 2. Active custom food (ID 1000)
    dbState.foods[1000L] = FoodEntity(
      id = 1000L,
      name = "Homemade Paneer",
      servingDescription = "100 g",
      servingGrams = 100.0,
      caloriesPer100g = 265.0,
      proteinPer100g = 18.0,
      carbsPer100g = 6.0,
      fatPer100g = 20.0,
      isCustom = true,
      isActive = true,
    )

    // 3. Archived custom food (ID 1001)
    dbState.foods[1001L] = FoodEntity(
      id = 1001L,
      name = "Old Shake",
      servingDescription = "1 glass",
      servingGrams = 200.0,
      caloriesPer100g = 100.0,
      proteinPer100g = 10.0,
      carbsPer100g = 10.0,
      fatPer100g = 2.0,
      isCustom = true,
      isActive = false,
    )

    // 4. Recipe (ID 10)
    dbState.recipes[10L] = RecipeEntity(
      id = 10L,
      name = "Paneer Curry",
      cookedWeightGrams = 300.0,
      totalCalories = 600.0,
      totalProtein = 36.0,
      totalCarbs = 12.0,
      totalFat = 40.0,
      caloriesPer100g = 200.0,
      proteinPer100g = 12.0,
      carbsPer100g = 4.0,
      fatPer100g = 13.33,
      isActive = true,
    )

    // 5. Recipe Ingredient (ID 101)
    dbState.ingredients[101L] = RecipeIngredientEntity(
      id = 101L,
      recipeId = 10L,
      foodId = 1000L,
      quantityGrams = 200.0,
    )

    // 6. Meal Entry (ID 50)
    dbState.mealEntries[50L] = MealEntryEntity(
      id = 50L,
      date = "2026-10-06",
      mealType = "lunch",
      foodId = 0L,
      recipeId = 10L,
      entryName = "Paneer Curry",
      quantityGrams = 150.0,
      calories = 300.0,
      protein = 18.0,
      carbs = 6.0,
      fat = 20.0,
    )

    // 7. Daily Goal
    dbState.dailyGoals["2026-10-06"] = DailyGoalEntity(
      date = "2026-10-06",
      calorieGoal = 2100.0,
      proteinGoal = 145.0,
      carbsGoal = 255.0,
      fatGoal = 72.0,
    )

    val backup = repository.createBackup()

    assertEquals(1, backup.backupFormatVersion)
    assertEquals("2026-10-06T12:00:00Z", backup.exportTimestamp)

    // Built-in food (id=1) must NOT be exported!
    assertEquals(2, backup.customFoods.size)
    assertFalse(backup.customFoods.any { it.id == 1L })
    assertTrue(backup.customFoods.any { it.id == 1000L && it.isActive })
    assertTrue(backup.customFoods.any { it.id == 1001L && !it.isActive }) // Archived custom food included

    assertEquals(1, backup.recipes.size)
    assertEquals("Paneer Curry", backup.recipes.first().name)

    assertEquals(1, backup.recipeIngredients.size)
    assertEquals(200.0, backup.recipeIngredients.first().quantityGrams, 0.001)

    assertEquals(1, backup.mealEntries.size)
    val entry = backup.mealEntries.first()
    assertEquals("Paneer Curry", entry.entryName)
    assertEquals(300.0, entry.calories, 0.001)

    assertEquals(1, backup.dailyGoals.size)
    assertEquals(2100.0, backup.dailyGoals.first().calorieGoal, 0.001)
  }

  @Test
  fun exportBackupJson_andParseBackupJson_roundtripsCorrectly() = runTest {
    dbState.foods[1000L] = FoodEntity(
      id = 1000L,
      name = "Homemade Paneer",
      servingDescription = "100 g",
      servingGrams = 100.0,
      caloriesPer100g = 265.0,
      proteinPer100g = 18.0,
      carbsPer100g = 6.0,
      fatPer100g = 20.0,
      isCustom = true,
      isActive = true,
    )

    val jsonString = repository.exportBackupJson()
    assertTrue(jsonString.contains("Homemade Paneer"))
    assertTrue(jsonString.contains("\"backupFormatVersion\": 1"))

    val parsed = repository.parseBackupJson(jsonString)
    assertEquals(1, parsed.backupFormatVersion)
    assertEquals(1, parsed.customFoods.size)
    assertEquals("Homemade Paneer", parsed.customFoods.first().name)
  }

  @Test(expected = SerializationException::class)
  fun parseBackupJson_withMalformedJson_throwsException() {
    repository.parseBackupJson("{ not valid json }")
  }

  @Test
  fun restoreBackup_replacesUserOwnedDataAndPreservesBuiltInFoods() = runTest {
    // Current database state:
    // Built-in food
    dbState.foods[1L] = FoodEntity(
      id = 1L,
      name = "Apple",
      servingDescription = "100 g",
      servingGrams = 100.0,
      caloriesPer100g = 52.0,
      proteinPer100g = 0.3,
      carbsPer100g = 14.0,
      fatPer100g = 0.2,
      isCustom = false,
    )
    // Old custom food to be replaced
    dbState.foods[1000L] = FoodEntity(
      id = 1000L,
      name = "Old Food To Replace",
      servingDescription = "100 g",
      servingGrams = 100.0,
      caloriesPer100g = 100.0,
      proteinPer100g = 10.0,
      carbsPer100g = 10.0,
      fatPer100g = 10.0,
      isCustom = true,
    )
    // Old recipe to be replaced
    dbState.recipes[5L] = RecipeEntity(
      id = 5L,
      name = "Old Recipe",
      cookedWeightGrams = 200.0,
      totalCalories = 200.0,
      totalProtein = 10.0,
      totalCarbs = 10.0,
      totalFat = 10.0,
      caloriesPer100g = 100.0,
      proteinPer100g = 5.0,
      carbsPer100g = 5.0,
      fatPer100g = 5.0,
      isActive = true,
    )

    // Backup to restore
    val backupToRestore = CalorieTrackBackup(
      backupFormatVersion = 1,
      applicationVersion = "1.0",
      exportTimestamp = "2026-10-06T12:00:00Z",
      customFoods = listOf(
        BackupCustomFood(
          id = 1050L,
          name = "Restored Custom Paneer",
          servingDescription = "100 g",
          servingGrams = 100.0,
          caloriesPer100g = 265.0,
          proteinPer100g = 18.0,
          carbsPer100g = 6.0,
          fatPer100g = 20.0,
          isActive = true,
        )
      ),
      recipes = listOf(
        BackupRecipe(
          id = 20L,
          name = "Restored Shahi Paneer",
          cookedWeightGrams = 300.0,
          totalCalories = 650.0,
          totalProtein = 36.8,
          totalCarbs = 13.2,
          totalFat = 52.0,
          caloriesPer100g = 216.67,
          proteinPer100g = 12.27,
          carbsPer100g = 4.4,
          fatPer100g = 17.33,
          isActive = true,
        )
      ),
      recipeIngredients = listOf(
        BackupRecipeIngredient(
          id = 201L,
          recipeId = 20L,
          foodId = 1050L,
          quantityGrams = 200.0,
        )
      ),
      mealEntries = listOf(
        BackupMealEntry(
          id = 77L,
          date = "2026-10-06",
          mealType = "dinner",
          foodId = 0L,
          recipeId = 20L,
          entryName = "Restored Shahi Paneer",
          quantityGrams = 150.0,
          calories = 325.0,
          protein = 18.4,
          carbs = 6.6,
          fat = 26.0,
        )
      ),
      dailyGoals = listOf(
        BackupDailyGoal(
          id = 12L,
          date = "2026-10-06",
          calorieGoal = 2200.0,
          proteinGoal = 150.0,
          carbsGoal = 260.0,
          fatGoal = 75.0,
        )
      ),
    )

    val result = repository.restoreBackup(backupToRestore)
    assertTrue(result.isSuccess)

    // 1. Built-in food (id = 1) must remain completely untouched!
    assertTrue(dbState.foods.containsKey(1L))
    assertEquals("Apple", dbState.foods[1L]!!.name)

    // 2. Old custom food (id = 1000) must be replaced
    assertFalse(dbState.foods.containsKey(1000L))

    // 3. New custom food (id = 1050) restored with preserved ID
    assertTrue(dbState.foods.containsKey(1050L))
    assertEquals("Restored Custom Paneer", dbState.foods[1050L]!!.name)

    // 4. Old recipe replaced, new recipe restored with preserved ID
    assertFalse(dbState.recipes.containsKey(5L))
    assertTrue(dbState.recipes.containsKey(20L))
    assertEquals("Restored Shahi Paneer", dbState.recipes[20L]!!.name)

    // 5. Recipe ingredient restored with preserved ID
    assertTrue(dbState.ingredients.containsKey(201L))
    assertEquals(200.0, dbState.ingredients[201L]!!.quantityGrams, 0.001)

    // 6. Meal entry restored with exact snapshots and preserved ID
    assertTrue(dbState.mealEntries.containsKey(77L))
    val restoredEntry = dbState.mealEntries[77L]!!
    assertEquals("Restored Shahi Paneer", restoredEntry.entryName)
    assertEquals(325.0, restoredEntry.calories, 0.001)
    assertEquals(18.4, restoredEntry.protein, 0.001)

    // 7. Daily goals restored
    assertTrue(dbState.dailyGoals.containsKey("2026-10-06"))
    assertEquals(2200.0, dbState.dailyGoals["2026-10-06"]!!.calorieGoal, 0.001)
  }

  @Test
  fun restoreBackup_invalidBackupFailsValidationAndLeavesDatabaseUntouched() = runTest {
    dbState.foods[1000L] = FoodEntity(
      id = 1000L,
      name = "Current Custom Food",
      servingDescription = "100 g",
      servingGrams = 100.0,
      caloriesPer100g = 200.0,
      proteinPer100g = 10.0,
      carbsPer100g = 10.0,
      fatPer100g = 10.0,
      isCustom = true,
    )

    // Invalid backup with unsupported format version
    val invalidBackup = CalorieTrackBackup(
      backupFormatVersion = 999,
      exportTimestamp = "2026-10-06T12:00:00Z",
    )

    val result = repository.restoreBackup(invalidBackup)
    assertTrue(result.isFailure)

    // Live database MUST be completely untouched!
    assertTrue(dbState.foods.containsKey(1000L))
    assertEquals("Current Custom Food", dbState.foods[1000L]!!.name)
  }
}
