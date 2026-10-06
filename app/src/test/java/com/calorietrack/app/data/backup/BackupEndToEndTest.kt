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
import com.calorietrack.app.util.NutritionCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates the full end-to-end backup export, mutation, and restore lifecycle:
 * Custom Food → Recipe → Logged Breakfast Meal Entry → Export Backup →
 * Corrupt/Mutate/Clear Database → Import Backup → Verify Full Restoration.
 */
class BackupEndToEndTest {

  private class InMemoryDatabaseState {
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
      override suspend fun archiveFood(id: Long): Int {
        val f = foods[id]
        return if (f != null && f.isCustom) { foods[id] = f.copy(isActive = false); 1 } else 0
      }
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
      override suspend fun archiveRecipe(recipeId: Long): Int {
        val r = recipes[recipeId]
        return if (r != null) { recipes[recipeId] = r.copy(isActive = false); 1 } else 0
      }
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

  @Test
  fun fullLifecycle_exportModifyRestore_restoresAllUserDataExactly() = runTest {
    val dbState = InMemoryDatabaseState()
    val repository = BackupRepository(
      foodDao = dbState.foodDao,
      recipeDao = dbState.recipeDao,
      mealEntryDao = dbState.mealEntryDao,
      dailyGoalDao = dbState.dailyGoalDao,
      transactionRunner = { block -> block() },
      timestampProvider = { "2026-10-06T12:00:00Z" },
    )

    // 1. Built-in food: Apple (ID 1)
    dbState.foods[1L] = FoodEntity(
      id = 1L,
      name = "Apple, raw",
      servingDescription = "100 g",
      servingGrams = 100.0,
      caloriesPer100g = 52.0,
      proteinPer100g = 0.26,
      carbsPer100g = 13.81,
      fatPer100g = 0.17,
      isCustom = false,
      isActive = true,
    )

    // 2. Custom Food: "My Paneer" (ID 1000)
    // 100 g, 265 kcal, 18 P, 6 C, 20 F
    dbState.foods[1000L] = FoodEntity(
      id = 1000L,
      name = "My Paneer",
      servingDescription = "100 g",
      servingGrams = 100.0,
      caloriesPer100g = 265.0,
      proteinPer100g = 18.0,
      carbsPer100g = 6.0,
      fatPer100g = 20.0,
      isCustom = true,
      isActive = true,
    )

    // 3. Custom Food: "Fresh Cream" (ID 1001)
    // 100 g, 300 kcal, 2 P, 3 C, 30 F
    dbState.foods[1001L] = FoodEntity(
      id = 1001L,
      name = "Fresh Cream",
      servingDescription = "100 g",
      servingGrams = 100.0,
      caloriesPer100g = 300.0,
      proteinPer100g = 2.0,
      carbsPer100g = 3.0,
      fatPer100g = 30.0,
      isCustom = true,
      isActive = true,
    )

    // 4. Recipe: "My Shahi Paneer" (ID 1)
    // Paneer 200g (530 cal, 36 P, 12 C, 40 F)
    // Cream 40g (120 cal, 0.8 P, 1.2 C, 12 F)
    // Cooked weight 300g
    // Total: 650 kcal, 36.8 P, 13.2 C, 52.0 F
    // Per 100g: 216.67 kcal, 12.27 P, 4.4 C, 17.33 F
    dbState.recipes[1L] = RecipeEntity(
      id = 1L,
      name = "My Shahi Paneer",
      cookedWeightGrams = 300.0,
      totalCalories = 650.0,
      totalProtein = 36.8,
      totalCarbs = 13.2,
      totalFat = 52.0,
      caloriesPer100g = (650.0 / 300.0) * 100.0,
      proteinPer100g = (36.8 / 300.0) * 100.0,
      carbsPer100g = (13.2 / 300.0) * 100.0,
      fatPer100g = (52.0 / 300.0) * 100.0,
      isActive = true,
      searchKeywords = "curry, paneer",
    )

    dbState.ingredients[1L] = RecipeIngredientEntity(
      id = 1L,
      recipeId = 1L,
      foodId = 1000L,
      quantityGrams = 200.0,
    )
    dbState.ingredients[2L] = RecipeIngredientEntity(
      id = 2L,
      recipeId = 1L,
      foodId = 1001L,
      quantityGrams = 40.0,
    )

    // 5. Log 150g to Breakfast
    val portionNutrition = NutritionCalculator.calculate(
      quantityGrams = 150.0,
      caloriesPer100g = dbState.recipes[1L]!!.caloriesPer100g,
      proteinPer100g = dbState.recipes[1L]!!.proteinPer100g,
      carbsPer100g = dbState.recipes[1L]!!.carbsPer100g,
      fatPer100g = dbState.recipes[1L]!!.fatPer100g,
    )

    dbState.mealEntries[1L] = MealEntryEntity(
      id = 1L,
      date = "2026-10-06",
      mealType = "breakfast",
      foodId = 0L,
      recipeId = 1L,
      entryName = "My Shahi Paneer",
      quantityGrams = 150.0,
      calories = portionNutrition.calories,
      protein = portionNutrition.protein,
      carbs = portionNutrition.carbs,
      fat = portionNutrition.fat,
    )

    // 6. Daily Goal
    dbState.dailyGoals["2026-10-06"] = DailyGoalEntity(
      date = "2026-10-06",
      calorieGoal = 2000.0,
      proteinGoal = 140.0,
      carbsGoal = 250.0,
      fatGoal = 70.0,
    )

    // Step A: Export Backup
    val exportedJson = repository.exportBackupJson()
    assertNotNull(exportedJson)
    assertTrue(exportedJson.contains("My Shahi Paneer"))
    assertTrue(exportedJson.contains("My Paneer"))

    // Step B: Modify/Corrupt/Clear User-Owned Data in Live Database
    dbState.foods.remove(1000L)
    dbState.foods.remove(1001L)
    dbState.recipes.clear()
    dbState.ingredients.clear()
    dbState.mealEntries.clear()
    dbState.dailyGoals.clear()

    // Step C: Import Backup
    val parsedBackup = repository.parseBackupJson(exportedJson)
    val restoreResult = repository.restoreBackup(parsedBackup)
    assertTrue(restoreResult.isSuccess)

    // Step D: Verify Full Restoration
    // 1. Built-in food was preserved
    assertTrue(dbState.foods.containsKey(1L))
    assertEquals("Apple, raw", dbState.foods[1L]!!.name)

    // 2. Custom foods restored
    assertTrue(dbState.foods.containsKey(1000L))
    val restoredPaneer = dbState.foods[1000L]!!
    assertEquals("My Paneer", restoredPaneer.name)
    assertEquals(265.0, restoredPaneer.caloriesPer100g, 0.001)
    assertEquals(18.0, restoredPaneer.proteinPer100g, 0.001)
    assertTrue(restoredPaneer.isCustom)
    assertTrue(restoredPaneer.isActive)

    assertTrue(dbState.foods.containsKey(1001L))
    assertEquals("Fresh Cream", dbState.foods[1001L]!!.name)

    // 3. Recipe restored
    assertTrue(dbState.recipes.containsKey(1L))
    val restoredRecipe = dbState.recipes[1L]!!
    assertEquals("My Shahi Paneer", restoredRecipe.name)
    assertEquals(300.0, restoredRecipe.cookedWeightGrams, 0.001)
    assertEquals(650.0, restoredRecipe.totalCalories, 0.001)
    assertTrue(restoredRecipe.isActive)

    // 4. Recipe ingredients restored
    assertEquals(2, dbState.ingredients.size)
    assertTrue(dbState.ingredients.containsKey(1L))
    assertTrue(dbState.ingredients.containsKey(2L))
    assertEquals(200.0, dbState.ingredients[1L]!!.quantityGrams, 0.001)
    assertEquals(40.0, dbState.ingredients[2L]!!.quantityGrams, 0.001)

    // 5. Breakfast meal entry restored with exact snapshots
    assertTrue(dbState.mealEntries.containsKey(1L))
    val restoredEntry = dbState.mealEntries[1L]!!
    assertEquals("2026-10-06", restoredEntry.date)
    assertEquals("breakfast", restoredEntry.mealType)
    assertEquals(1L, restoredEntry.recipeId)
    assertEquals("My Shahi Paneer", restoredEntry.entryName)
    assertEquals(150.0, restoredEntry.quantityGrams, 0.001)
    assertEquals(325.0, restoredEntry.calories, 0.001) // 650 * (150/300) = 325
    assertEquals(18.4, restoredEntry.protein, 0.001)
    assertEquals(6.6, restoredEntry.carbs, 0.001)
    assertEquals(26.0, restoredEntry.fat, 0.001)

    // 6. Daily Goal restored
    assertTrue(dbState.dailyGoals.containsKey("2026-10-06"))
    assertEquals(2000.0, dbState.dailyGoals["2026-10-06"]!!.calorieGoal, 0.001)
  }
}
