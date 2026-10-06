package com.calorietrack.app.data.local

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
 * Validates the core architectural guarantees of Milestone 2J:
 * 1. Recipe calculates total recipe nutrition and per-100g density based on cooked weight.
 * 2. Logging a recipe snapshots the portion nutrition and recipe name into MealEntry.
 * 3. Editing a recipe does NOT alter historical logged MealEntry records.
 * 4. Archiving a recipe marks it is_active = 0, hides it from active searches, but preserves
 *    historical logs (never turning into "Unknown Food" and keeping all nutrition).
 */
class RecipeHistoricalIntegrityTest {

  private class InMemoryDatabase {
    val foods = mutableMapOf<Long, FoodEntity>()
    val recipes = mutableMapOf<Long, RecipeEntity>()
    val recipeIngredients = mutableMapOf<Long, RecipeIngredientEntity>()
    val mealEntries = mutableMapOf<Long, MealEntryEntity>()

    private var nextRecipeId = 1L
    private var nextIngredientId = 1L
    private var nextEntryId = 1L

    val foodDao = object : FoodDao {
      override suspend fun getById(id: Long): FoodEntity? = foods[id]

      override fun getAll(): Flow<List<FoodEntity>> =
        flowOf(foods.values.filter { it.isActive }.sortedBy { it.name })

      override fun searchByName(query: String): Flow<List<FoodEntity>> =
        flowOf(
          foods.values
            .filter { it.isActive && (it.name.contains(query, true) || it.searchKeywords.contains(query, true)) }
            .sortedBy { it.name }
        )

      override fun getMyFoods(): Flow<List<FoodEntity>> =
        flowOf(foods.values.filter { it.isCustom && it.isActive }.sortedBy { it.name })

      override fun searchMyFoods(query: String): Flow<List<FoodEntity>> =
        flowOf(
          foods.values
            .filter { it.isCustom && it.isActive && (it.name.contains(query, true) || it.searchKeywords.contains(query, true)) }
            .sortedBy { it.name }
        )

      override suspend fun insert(food: FoodEntity): Long {
        foods[food.id] = food
        return food.id
      }

      override suspend fun insertAll(foods: List<FoodEntity>) {
        foods.forEach { insert(it) }
      }

      override suspend fun update(food: FoodEntity) {
        foods[food.id] = food
      }

      override suspend fun delete(food: FoodEntity) {
        foods.remove(food.id)
      }

      override suspend fun archiveFood(id: Long): Int {
        val existing = foods[id]
        return if (existing != null && existing.isCustom) {
          foods[id] = existing.copy(isActive = false)
          1
        } else {
          0
        }
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

      override fun getAllActiveRecipes(): Flow<List<RecipeEntity>> =
        flowOf(recipes.values.filter { it.isActive }.sortedBy { it.name })

      override fun searchActiveRecipes(query: String): Flow<List<RecipeEntity>> =
        flowOf(
          recipes.values
            .filter { it.isActive && (it.name.contains(query, true) || it.searchKeywords.contains(query, true)) }
            .sortedBy { it.name }
        )

      override fun getIngredientsWithFood(recipeId: Long): Flow<List<RecipeIngredientWithFood>> {
        val matching = recipeIngredients.values.filter { it.recipeId == recipeId }
        val mapped = matching.map { ing ->
          val food = foods[ing.foodId]
          RecipeIngredientWithFood(
            id = ing.id,
            recipeId = ing.recipeId,
            foodId = ing.foodId,
            quantityGrams = ing.quantityGrams,
            foodName = food?.name ?: "Unknown Food",
            caloriesPer100g = food?.caloriesPer100g ?: 0.0,
            proteinPer100g = food?.proteinPer100g ?: 0.0,
            carbsPer100g = food?.carbsPer100g ?: 0.0,
            fatPer100g = food?.fatPer100g ?: 0.0,
            calories = (food?.caloriesPer100g ?: 0.0) * ing.quantityGrams / 100.0,
            protein = (food?.proteinPer100g ?: 0.0) * ing.quantityGrams / 100.0,
            carbs = (food?.carbsPer100g ?: 0.0) * ing.quantityGrams / 100.0,
            fat = (food?.fatPer100g ?: 0.0) * ing.quantityGrams / 100.0,
          )
        }
        return flowOf(mapped)
      }

      override suspend fun getIngredientsForRecipe(recipeId: Long): List<RecipeIngredientEntity> =
        recipeIngredients.values.filter { it.recipeId == recipeId }

      override suspend fun insertRecipe(recipe: RecipeEntity): Long {
        val id = if (recipe.id == 0L) nextRecipeId++ else recipe.id
        recipes[id] = recipe.copy(id = id)
        return id
      }

      override suspend fun updateRecipe(recipe: RecipeEntity) {
        recipes[recipe.id] = recipe
      }

      override suspend fun insertIngredients(ingredients: List<RecipeIngredientEntity>) {
        ingredients.forEach { ing ->
          val id = if (ing.id == 0L) nextIngredientId++ else ing.id
          recipeIngredients[id] = ing.copy(id = id)
        }
      }

      override suspend fun deleteIngredientsForRecipe(recipeId: Long) {
        recipeIngredients.entries.removeAll { it.value.recipeId == recipeId }
      }

      override suspend fun archiveRecipe(recipeId: Long): Int {
        val existing = recipes[recipeId]
        return if (existing != null) {
          recipes[recipeId] = existing.copy(isActive = false)
          1
        } else {
          0
        }
      }

      override suspend fun countActiveRecipes(): Int = recipes.values.count { it.isActive }

      override suspend fun saveRecipeWithIngredients(
        recipe: RecipeEntity,
        ingredients: List<RecipeIngredientEntity>
      ): Long {
        val targetId = if (recipe.id == 0L) {
          insertRecipe(recipe)
        } else {
          updateRecipe(recipe)
          deleteIngredientsForRecipe(recipe.id)
          recipe.id
        }

        val mapped = ingredients.map { it.copy(id = 0L, recipeId = targetId) }
        insertIngredients(mapped)
        return targetId
      }

      override suspend fun getAllRecipes(): List<RecipeEntity> = recipes.values.toList()
      override suspend fun getAllRecipeIngredients(): List<RecipeIngredientEntity> = recipeIngredients.values.toList()
      override suspend fun deleteAllRecipeIngredients(): Int { val s = recipeIngredients.size; recipeIngredients.clear(); return s }
      override suspend fun deleteAllRecipes(): Int { val s = recipes.size; recipes.clear(); return s }
      override suspend fun insertRecipes(recipesList: List<RecipeEntity>) { recipesList.forEach { recipes[it.id] = it } }
    }

    val mealEntryDao = object : MealEntryDao {
      override fun getEntriesForDate(date: String): Flow<List<MealEntryEntity>> =
        flowOf(mealEntries.values.filter { it.date == date }.sortedBy { it.id })

      override fun getEntriesForDateAndMealType(date: String, mealType: String): Flow<List<MealEntryEntity>> =
        flowOf(mealEntries.values.filter { it.date == date && it.mealType == mealType }.sortedBy { it.id })

      override suspend fun insert(entry: MealEntryEntity): Long {
        val id = if (entry.id == 0L) nextEntryId++ else entry.id
        mealEntries[id] = entry.copy(id = id)
        return id
      }

      override suspend fun update(entry: MealEntryEntity) {
        mealEntries[entry.id] = entry
      }

      override suspend fun delete(entry: MealEntryEntity) {
        mealEntries.remove(entry.id)
      }

      override suspend fun deleteForDate(date: String) {
        mealEntries.entries.removeAll { it.value.date == date }
      }

      override fun observeDailyTotals(date: String): Flow<DailyNutritionTotals> {
        val matching = mealEntries.values.filter { it.date == date }
        return flowOf(
          DailyNutritionTotals(
            totalCalories = matching.sumOf { it.calories },
            totalProtein = matching.sumOf { it.protein },
            totalCarbs = matching.sumOf { it.carbs },
            totalFat = matching.sumOf { it.fat },
          )
        )
      }

      override fun getEntriesWithFoodForDate(date: String): Flow<List<MealEntryWithFood>> {
        val matching = mealEntries.values.filter { it.date == date }
        val result = matching.map { entry ->
          val food = foods[entry.foodId]
          val recipe = entry.recipeId?.let { recipes[it] }
          val resolvedName = entry.entryName ?: recipe?.name ?: food?.name ?: "Unknown Food"
          MealEntryWithFood(
            id = entry.id,
            date = entry.date,
            mealType = entry.mealType,
            foodId = entry.foodId,
            recipeId = entry.recipeId,
            quantityGrams = entry.quantityGrams,
            calories = entry.calories,
            protein = entry.protein,
            carbs = entry.carbs,
            fat = entry.fat,
            foodName = resolvedName,
          )
        }
        return flowOf(result)
      }

      override suspend fun getEntryById(id: Long): MealEntryEntity? = mealEntries[id]

      override suspend fun deleteById(id: Long) {
        mealEntries.remove(id)
      }

      override fun getEntriesWithFoodForDateAndMealType(
        date: String,
        mealType: String
      ): Flow<List<MealEntryWithFood>> =
        getEntriesWithFoodForDate(date)

      override fun observeAllDailyTotals(): Flow<List<DailySummary>> = flowOf(emptyList())

      override fun getDatesWithEntries(): Flow<List<String>> =
        flowOf(mealEntries.values.map { it.date }.distinct().sortedDescending())

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
  }

  @Test
  fun recipe_fullLifecycleMaintainsHistoricalIntegrity() = runTest {
    val db = InMemoryDatabase()

    // 1. Setup ingredients: Paneer (1000) and Cream (1001)
    val paneer = FoodEntity(
      id = 1000L,
      name = "Homemade Paneer",
      servingDescription = "100 g",
      servingGrams = 100.0,
      caloriesPer100g = 265.0,
      proteinPer100g = 18.0,
      carbsPer100g = 6.0,
      fatPer100g = 20.0,
      isCustom = true,
    )
    val cream = FoodEntity(
      id = 1001L,
      name = "Fresh Cream",
      servingDescription = "100 g",
      servingGrams = 100.0,
      caloriesPer100g = 300.0,
      proteinPer100g = 2.0,
      carbsPer100g = 3.0,
      fatPer100g = 30.0,
      isCustom = true,
    )
    db.foodDao.insert(paneer)
    db.foodDao.insert(cream)

    // 2. Create Recipe: "My Shahi Paneer"
    // Paneer 200g -> cal: 530, P: 36, C: 12, F: 40
    // Cream 40g  -> cal: 120, P: 0.8, C: 1.2, F: 12
    // Raw weight = 240g. Total cal = 650, P = 36.8, C = 13.2, F = 52.0
    // Cooked weight = 300g
    // Per 100g: cal = 650/300*100 = 216.6667, P = 12.2667, C = 4.4, F = 17.3333
    val cookedWeight = 300.0
    val totalCalories = 650.0
    val totalProtein = 36.8
    val totalCarbs = 13.2
    val totalFat = 52.0

    val recipeEntity = RecipeEntity(
      name = "My Shahi Paneer",
      cookedWeightGrams = cookedWeight,
      totalCalories = totalCalories,
      totalProtein = totalProtein,
      totalCarbs = totalCarbs,
      totalFat = totalFat,
      caloriesPer100g = (totalCalories / cookedWeight) * 100.0,
      proteinPer100g = (totalProtein / cookedWeight) * 100.0,
      carbsPer100g = (totalCarbs / cookedWeight) * 100.0,
      fatPer100g = (totalFat / cookedWeight) * 100.0,
      isActive = true,
      searchKeywords = "curry, paneer",
    )

    val ingredients = listOf(
      RecipeIngredientEntity(id = 0L, recipeId = 0L, foodId = 1000L, quantityGrams = 200.0),
      RecipeIngredientEntity(id = 0L, recipeId = 0L, foodId = 1001L, quantityGrams = 40.0),
    )

    val recipeId = db.recipeDao.saveRecipeWithIngredients(recipeEntity, ingredients)
    assertEquals(1L, recipeId)

    // 3. User logs a portion of 150g to Breakfast on 2026-10-06
    val consumedGrams = 150.0
    val savedRecipe = db.recipeDao.getRecipeById(recipeId)!!
    val portionNutrition = NutritionCalculator.calculate(
      quantityGrams = consumedGrams,
      caloriesPer100g = savedRecipe.caloriesPer100g,
      proteinPer100g = savedRecipe.proteinPer100g,
      carbsPer100g = savedRecipe.carbsPer100g,
      fatPer100g = savedRecipe.fatPer100g,
    )

    val entryId = db.mealEntryDao.insert(
      MealEntryEntity(
        date = "2026-10-06",
        mealType = "breakfast",
        foodId = 0L,
        recipeId = recipeId,
        entryName = savedRecipe.name,
        quantityGrams = consumedGrams,
        calories = portionNutrition.calories,
        protein = portionNutrition.protein,
        carbs = portionNutrition.carbs,
        fat = portionNutrition.fat,
      )
    )
    assertEquals(1L, entryId)

    // Verify historical snapshot
    var historicalEntries: List<MealEntryWithFood> = emptyList()
    db.mealEntryDao.getEntriesWithFoodForDate("2026-10-06").collect {
      historicalEntries = it
    }
    assertEquals(1, historicalEntries.size)
    val entry = historicalEntries[0]
    assertEquals("My Shahi Paneer", entry.foodName)
    assertEquals(recipeId, entry.recipeId)
    assertEquals(325.0, entry.calories, 0.001) // 650 * (150/300) = 325
    assertEquals(18.4, entry.protein, 0.001)
    assertEquals(6.6, entry.carbs, 0.001)
    assertEquals(26.0, entry.fat, 0.001)

    // 4. User modifies recipe (e.g. adjusts cream to 80g, new cooked weight 350g)
    val updatedRecipe = savedRecipe.copy(
      cookedWeightGrams = 350.0,
      totalCalories = 770.0,
      totalProtein = 37.6,
      totalCarbs = 14.4,
      totalFat = 64.0,
      caloriesPer100g = (770.0 / 350.0) * 100.0,
      proteinPer100g = (37.6 / 350.0) * 100.0,
      carbsPer100g = (14.4 / 350.0) * 100.0,
      fatPer100g = (64.0 / 350.0) * 100.0,
    )
    db.recipeDao.saveRecipeWithIngredients(
      updatedRecipe,
      listOf(
        RecipeIngredientEntity(id = 0L, recipeId = recipeId, foodId = 1000L, quantityGrams = 200.0),
        RecipeIngredientEntity(id = 0L, recipeId = recipeId, foodId = 1001L, quantityGrams = 80.0),
      )
    )

    // 5. CRITICAL: Verify past logged entry is completely unchanged
    db.mealEntryDao.getEntriesWithFoodForDate("2026-10-06").collect {
      historicalEntries = it
    }
    assertEquals(1, historicalEntries.size)
    val pastEntry = historicalEntries[0]
    assertEquals("My Shahi Paneer", pastEntry.foodName)
    assertEquals(325.0, pastEntry.calories, 0.001)
    assertEquals(18.4, pastEntry.protein, 0.001)
    assertEquals(6.6, pastEntry.carbs, 0.001)
    assertEquals(26.0, pastEntry.fat, 0.001)

    // 6. User archives the recipe
    val archiveResult = db.recipeDao.archiveRecipe(recipeId)
    assertEquals(1, archiveResult)

    // Verify it is hidden from active searches
    var activeRecipes: List<RecipeEntity> = emptyList()
    db.recipeDao.getAllActiveRecipes().collect { activeRecipes = it }
    assertEquals(0, activeRecipes.size)

    var searchedRecipes: List<RecipeEntity> = emptyList()
    db.recipeDao.searchActiveRecipes("shahi").collect { searchedRecipes = it }
    assertEquals(0, searchedRecipes.size)

    // 7. CRITICAL: Past logged entry STILL displays recipe name and original nutrition!
    db.mealEntryDao.getEntriesWithFoodForDate("2026-10-06").collect {
      historicalEntries = it
    }
    assertEquals(1, historicalEntries.size)
    val archivedEntry = historicalEntries[0]
    assertEquals("My Shahi Paneer", archivedEntry.foodName)
    assertFalse("Must never resolve to Unknown Food", archivedEntry.foodName == "Unknown Food")
    assertEquals(325.0, archivedEntry.calories, 0.001)
  }
}
