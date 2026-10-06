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
 * Validates the core architectural guarantee of Milestone 2I:
 * 1. Custom food IDs start at >= 1000
 * 2. Editing a custom food does NOT mutate past logged MealEntry snapshots
 * 3. Archiving a custom food hides it from catalogue/search while preserving
 *    the food name and snapshot nutrition on historical days (no "Unknown Food")
 * 4. Built-in foods cannot be archived
 */
class CustomFoodHistoricalIntegrityTest {

  private class InMemoryDatabase {
    val foods = mutableMapOf<Long, FoodEntity>()
    val mealEntries = mutableMapOf<Long, MealEntryEntity>()

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
    }

    val mealEntryDao = object : MealEntryDao {
      override fun getEntriesForDate(date: String): Flow<List<MealEntryEntity>> =
        flowOf(mealEntries.values.filter { it.date == date }.sortedBy { it.id })

      override fun getEntriesForDateAndMealType(date: String, mealType: String): Flow<List<MealEntryEntity>> =
        flowOf(mealEntries.values.filter { it.date == date && it.mealType == mealType }.sortedBy { it.id })

      override suspend fun insert(entry: MealEntryEntity): Long {
        val id = if (entry.id == 0L) (mealEntries.size + 1).toLong() else entry.id
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
            foodName = entry.entryName ?: food?.name ?: "Unknown Food",
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
    }
  }

  @Test
  fun customFood_fullLifecyclePreservesHistoricalLogs() = runTest {
    val db = InMemoryDatabase()

    // 1. Built-in food (id = 1)
    db.foodDao.insert(
      FoodEntity(
        id = 1L,
        name = "Rolled Oats",
        servingDescription = "40 g",
        servingGrams = 40.0,
        caloriesPer100g = 379.0,
        proteinPer100g = 13.15,
        carbsPer100g = 67.7,
        fatPer100g = 6.52,
        isCustom = false,
      )
    )

    // 2. Create custom food with ID >= 1000
    val maxId = db.foodDao.getMaxId() ?: 999L
    val customId = maxOf(FoodEntity.CUSTOM_MIN_ID, maxId + 1L)
    assertEquals(1000L, customId)

    val customFood = FoodEntity(
      id = customId,
      name = "Homemade Paneer",
      servingDescription = "100 g",
      servingGrams = 100.0,
      caloriesPer100g = 265.0,
      proteinPer100g = 18.0,
      carbsPer100g = 6.0,
      fatPer100g = 20.0,
      isCustom = true,
      dataSource = FoodEntity.DEFAULT_DATA_SOURCE_CUSTOM,
      searchKeywords = "paneer, cottage cheese",
    )
    db.foodDao.insert(customFood)

    // 3. Log 150g of custom food to Breakfast on 2026-10-02
    val nutrition = NutritionCalculator.calculate(
      quantityGrams = 150.0,
      caloriesPer100g = customFood.caloriesPer100g,
      proteinPer100g = customFood.proteinPer100g,
      carbsPer100g = customFood.carbsPer100g,
      fatPer100g = customFood.fatPer100g,
    )
    val entryId = db.mealEntryDao.insert(
      MealEntryEntity(
        date = "2026-10-02",
        mealType = "breakfast",
        foodId = customId,
        quantityGrams = 150.0,
        calories = nutrition.calories,
        protein = nutrition.protein,
        carbs = nutrition.carbs,
        fat = nutrition.fat,
      )
    )
    assertEquals(1L, entryId)

    // Verify historical snapshot
    var historicalEntries: List<MealEntryWithFood> = emptyList()
    db.mealEntryDao.getEntriesWithFoodForDate("2026-10-02").collect {
      historicalEntries = it
    }
    assertEquals(1, historicalEntries.size)
    val loggedEntry = historicalEntries[0]
    assertEquals("Homemade Paneer", loggedEntry.foodName)
    assertEquals(397.5, loggedEntry.calories, 0.001)
    assertEquals(27.0, loggedEntry.protein, 0.001)
    assertEquals(9.0, loggedEntry.carbs, 0.001)
    assertEquals(30.0, loggedEntry.fat, 0.001)

    // 4. User edits custom food nutrition in the catalogue
    val editedFood = customFood.copy(
      caloriesPer100g = 300.0,
      proteinPer100g = 22.0,
      carbsPer100g = 4.0,
      fatPer100g = 24.0,
    )
    db.foodDao.update(editedFood)

    // Verify historical log on 2026-10-02 STILL has original snapshot calories/macros!
    db.mealEntryDao.getEntriesWithFoodForDate("2026-10-02").collect {
      historicalEntries = it
    }
    assertEquals(397.5, historicalEntries[0].calories, 0.001)
    assertEquals(27.0, historicalEntries[0].protein, 0.001)
    assertEquals(9.0, historicalEntries[0].carbs, 0.001)
    assertEquals(30.0, historicalEntries[0].fat, 0.001)

    // 5. User removes/archives the custom food from My Foods
    val archivedRows = db.foodDao.archiveFood(customId)
    assertEquals(1, archivedRows)

    // Verify it is no longer returned in active search or all foods
    var activeFoods: List<FoodEntity> = emptyList()
    db.foodDao.getAll().collect { activeFoods = it }
    assertEquals(1, activeFoods.size)
    assertEquals("Rolled Oats", activeFoods[0].name)

    var myFoods: List<FoodEntity> = emptyList()
    db.foodDao.getMyFoods().collect { myFoods = it }
    assertEquals(0, myFoods.size)

    // 6. CRUCIAL: Historical meal entry STILL resolves food name and original calories!
    db.mealEntryDao.getEntriesWithFoodForDate("2026-10-02").collect {
      historicalEntries = it
    }
    assertEquals(1, historicalEntries.size)
    assertEquals("Homemade Paneer", historicalEntries[0].foodName)
    assertFalse("Must not resolve to Unknown Food", historicalEntries[0].foodName == "Unknown Food")
    assertEquals(397.5, historicalEntries[0].calories, 0.001)

    // 7. Verify built-in foods cannot be archived
    val builtInArchiveResult = db.foodDao.archiveFood(1L)
    assertEquals(0, builtInArchiveResult)
    assertTrue("Built-in food must remain active", db.foodDao.getById(1L)!!.isActive)
  }
}
