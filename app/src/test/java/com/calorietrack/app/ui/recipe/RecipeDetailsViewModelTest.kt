package com.calorietrack.app.ui.recipe

import com.calorietrack.app.data.local.DailyNutritionTotals
import com.calorietrack.app.data.local.DailySummary
import com.calorietrack.app.data.local.MealEntryDao
import com.calorietrack.app.data.local.MealEntryEntity
import com.calorietrack.app.data.local.MealEntryWithFood
import com.calorietrack.app.data.local.RecipeDao
import com.calorietrack.app.data.local.RecipeEntity
import com.calorietrack.app.data.local.RecipeIngredientEntity
import com.calorietrack.app.data.local.RecipeIngredientWithFood
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeDetailsViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private val sampleRecipe = RecipeEntity(
    id = 42L,
    name = "Paneer Tikka Masala",
    cookedWeightGrams = 400.0,
    totalCalories = 800.0,
    totalProtein = 48.0,
    totalCarbs = 20.0,
    totalFat = 60.0,
    caloriesPer100g = 200.0,
    proteinPer100g = 12.0,
    carbsPer100g = 5.0,
    fatPer100g = 15.0,
    isActive = true,
  )

  private class FakeRecipeDao(initialRecipe: RecipeEntity) : RecipeDao {
    val recipes = mutableMapOf(initialRecipe.id to initialRecipe)

    override suspend fun getRecipeById(id: Long): RecipeEntity? = recipes[id]
    override fun getAllActiveRecipes(): Flow<List<RecipeEntity>> =
      flowOf(recipes.values.filter { it.isActive })
    override fun searchActiveRecipes(query: String): Flow<List<RecipeEntity>> =
      flowOf(recipes.values.filter { it.isActive && it.name.contains(query, true) })

    override fun getIngredientsWithFood(recipeId: Long): Flow<List<RecipeIngredientWithFood>> =
      flowOf(
        listOf(
          RecipeIngredientWithFood(
            id = 1L,
            recipeId = recipeId,
            foodId = 1000L,
            quantityGrams = 200.0,
            foodName = "Paneer",
            caloriesPer100g = 265.0,
            proteinPer100g = 18.0,
            carbsPer100g = 6.0,
            fatPer100g = 20.0,
            calories = 530.0,
            protein = 36.0,
            carbs = 12.0,
            fat = 40.0,
          )
        )
      )

    override suspend fun getIngredientsForRecipe(recipeId: Long): List<RecipeIngredientEntity> = emptyList()
    override suspend fun insertRecipe(recipe: RecipeEntity): Long = recipe.id
    override suspend fun updateRecipe(recipe: RecipeEntity) { recipes[recipe.id] = recipe }
    override suspend fun insertIngredients(ingredients: List<RecipeIngredientEntity>) {}
    override suspend fun deleteIngredientsForRecipe(recipeId: Long) {}
    override suspend fun archiveRecipe(recipeId: Long): Int {
      val r = recipes[recipeId]
      return if (r != null) {
        recipes[recipeId] = r.copy(isActive = false)
        1
      } else 0
    }
    override suspend fun countActiveRecipes(): Int = recipes.values.count { it.isActive }
    override suspend fun saveRecipeWithIngredients(recipe: RecipeEntity, ingredients: List<RecipeIngredientEntity>): Long = recipe.id
    override suspend fun getAllRecipes(): List<RecipeEntity> = recipes.values.toList()
    override suspend fun getAllRecipeIngredients(): List<RecipeIngredientEntity> = emptyList()
    override suspend fun deleteAllRecipeIngredients(): Int = 0
    override suspend fun deleteAllRecipes(): Int { val s = recipes.size; recipes.clear(); return s }
    override suspend fun insertRecipes(recipesList: List<RecipeEntity>) { recipesList.forEach { recipes[it.id] = it } }
  }

  private class FakeMealEntryDao : MealEntryDao {
    val entries = mutableMapOf<Long, MealEntryEntity>()
    private var nextId = 1L

    override fun getEntriesForDate(date: String): Flow<List<MealEntryEntity>> = flowOf(emptyList())
    override fun getEntriesForDateAndMealType(date: String, mealType: String): Flow<List<MealEntryEntity>> = flowOf(emptyList())
    override suspend fun insert(entry: MealEntryEntity): Long {
      val id = if (entry.id == 0L) nextId++ else entry.id
      entries[id] = entry.copy(id = id)
      return id
    }
    override suspend fun update(entry: MealEntryEntity) { entries[entry.id] = entry }
    override suspend fun delete(entry: MealEntryEntity) { entries.remove(entry.id) }
    override suspend fun deleteForDate(date: String) {}
    override fun observeDailyTotals(date: String): Flow<DailyNutritionTotals> = flowOf(DailyNutritionTotals(0.0, 0.0, 0.0, 0.0))
    override fun getEntriesWithFoodForDate(date: String): Flow<List<MealEntryWithFood>> = flowOf(emptyList())
    override suspend fun getEntryById(id: Long): MealEntryEntity? = entries[id]
    override suspend fun deleteById(id: Long) { entries.remove(id) }
    override fun getEntriesWithFoodForDateAndMealType(date: String, mealType: String): Flow<List<MealEntryWithFood>> = flowOf(emptyList())
    override fun observeAllDailyTotals(): Flow<List<DailySummary>> = flowOf(emptyList())
    override fun getDatesWithEntries(): Flow<List<String>> = flowOf(emptyList())
    override suspend fun getAllMealEntries(): List<MealEntryEntity> = entries.values.toList()
    override suspend fun deleteAllMealEntries(): Int { val s = entries.size; entries.clear(); return s }
    override suspend fun insertAll(entriesList: List<MealEntryEntity>) { entriesList.forEach { entries[it.id] = it } }
  }

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun initialState_loadsRecipeAndCalculatesDefaultPortion() = runTest {
    val recipeDao = FakeRecipeDao(sampleRecipe)
    val mealEntryDao = FakeMealEntryDao()
    val viewModel = RecipeDetailsViewModel(
      recipeId = 42L,
      recipeDao = recipeDao,
      mealEntryDao = mealEntryDao,
    )
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNotNull(state.recipe)
    assertEquals("Paneer Tikka Masala", state.recipe!!.name)
    assertEquals(1, state.ingredients.size)
    assertEquals(100.0, state.quantityGrams!!, 0.001)
    // 100g of 200 kcal/100g recipe -> 200 kcal
    assertEquals(200.0, state.calculatedCalories, 0.001)
    assertEquals(12.0, state.calculatedProtein, 0.001)
    assertEquals(5.0, state.calculatedCarbs, 0.001)
    assertEquals(15.0, state.calculatedFat, 0.001)
  }

  @Test
  fun onQuantityChanged_scalesPortionNutritionAccurately() = runTest {
    val recipeDao = FakeRecipeDao(sampleRecipe)
    val mealEntryDao = FakeMealEntryDao()
    val viewModel = RecipeDetailsViewModel(
      recipeId = 42L,
      recipeDao = recipeDao,
      mealEntryDao = mealEntryDao,
    )
    advanceUntilIdle()

    // Change portion to 250g
    viewModel.onQuantityChanged("250")
    val state = viewModel.uiState.value
    assertEquals(250.0, state.quantityGrams!!, 0.001)
    assertTrue(state.isValidQuantity)
    // 250g: 200 * 2.5 = 500 kcal, 12 * 2.5 = 30 P, 5 * 2.5 = 12.5 C, 15 * 2.5 = 37.5 F
    assertEquals(500.0, state.calculatedCalories, 0.001)
    assertEquals(30.0, state.calculatedProtein, 0.001)
    assertEquals(12.5, state.calculatedCarbs, 0.001)
    assertEquals(37.5, state.calculatedFat, 0.001)
  }

  @Test
  fun logMeal_insertsMealEntryWithRecipeIdAndEntryName() = runTest {
    val recipeDao = FakeRecipeDao(sampleRecipe)
    val mealEntryDao = FakeMealEntryDao()
    val fixedDate = LocalDate.of(2026, 10, 6)
    val viewModel = RecipeDetailsViewModel(
      recipeId = 42L,
      initialMealType = "lunch",
      recipeDao = recipeDao,
      mealEntryDao = mealEntryDao,
      dateProvider = { fixedDate },
    )
    advanceUntilIdle()

    viewModel.onQuantityChanged("150")

    var logged = false
    viewModel.logMeal { logged = true }
    advanceUntilIdle()

    assertTrue(logged)
    assertEquals(1, mealEntryDao.entries.size)
    val inserted = mealEntryDao.entries.values.first()
    assertEquals("2026-10-06", inserted.date)
    assertEquals("lunch", inserted.mealType)
    assertEquals(0L, inserted.foodId)
    assertEquals(42L, inserted.recipeId)
    assertEquals("Paneer Tikka Masala", inserted.entryName)
    assertEquals(150.0, inserted.quantityGrams, 0.001)
    // 150g -> 300 kcal, 18 P, 7.5 C, 22.5 F
    assertEquals(300.0, inserted.calories, 0.001)
    assertEquals(18.0, inserted.protein, 0.001)
  }

  @Test
  fun editMode_loadsExistingEntryAndUpdatesOnSave() = runTest {
    val recipeDao = FakeRecipeDao(sampleRecipe)
    val mealEntryDao = FakeMealEntryDao()

    // Existing logged meal entry
    mealEntryDao.insert(
      MealEntryEntity(
        id = 99L,
        date = "2026-10-05",
        mealType = "dinner",
        foodId = 0L,
        recipeId = 42L,
        entryName = "Paneer Tikka Masala",
        quantityGrams = 200.0,
        calories = 400.0,
        protein = 24.0,
        carbs = 10.0,
        fat = 30.0,
      )
    )

    val viewModel = RecipeDetailsViewModel(
      recipeId = 42L,
      mealEntryId = 99L,
      recipeDao = recipeDao,
      mealEntryDao = mealEntryDao,
    )
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.isEditMode)
    assertEquals("dinner", state.selectedMealType)
    assertEquals(200.0, state.quantityGrams!!, 0.001)

    // Edit quantity to 180g and mealType to lunch
    viewModel.onQuantityChanged("180")
    viewModel.onMealTypeSelected("lunch")

    var saved = false
    viewModel.logMeal { saved = true }
    advanceUntilIdle()

    assertTrue(saved)
    val updated = mealEntryDao.getEntryById(99L)!!
    assertEquals("2026-10-05", updated.date)
    assertEquals("lunch", updated.mealType)
    assertEquals(180.0, updated.quantityGrams, 0.001)
    assertEquals(360.0, updated.calories, 0.001) // 200 * 1.8 = 360
  }

  @Test
  fun archiveRecipe_marksRecipeInactive() = runTest {
    val recipeDao = FakeRecipeDao(sampleRecipe)
    val mealEntryDao = FakeMealEntryDao()
    val viewModel = RecipeDetailsViewModel(
      recipeId = 42L,
      recipeDao = recipeDao,
      mealEntryDao = mealEntryDao,
    )
    advanceUntilIdle()

    var archived = false
    viewModel.archiveRecipe { archived = true }
    advanceUntilIdle()

    assertTrue(archived)
    assertFalse(recipeDao.getRecipeById(42L)!!.isActive)
  }
}
