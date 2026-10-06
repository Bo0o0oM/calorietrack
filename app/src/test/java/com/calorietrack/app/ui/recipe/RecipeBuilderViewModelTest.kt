package com.calorietrack.app.ui.recipe

import com.calorietrack.app.data.local.FoodDao
import com.calorietrack.app.data.local.FoodEntity
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeBuilderViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private val foodA = FoodEntity(
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

  private val foodB = FoodEntity(
    id = 2L,
    name = "Whole Milk",
    servingDescription = "100 ml",
    servingGrams = 100.0,
    caloriesPer100g = 60.0,
    proteinPer100g = 3.2,
    carbsPer100g = 4.8,
    fatPer100g = 3.3,
    isCustom = false,
  )

  private class FakeFoodDao(val foods: List<FoodEntity>) : FoodDao {
    override suspend fun getById(id: Long): FoodEntity? = foods.find { it.id == id }
    override fun getAll(): Flow<List<FoodEntity>> = flowOf(foods)
    override fun searchByName(query: String): Flow<List<FoodEntity>> =
      flowOf(foods.filter { it.name.contains(query, true) })
    override fun getMyFoods(): Flow<List<FoodEntity>> = flowOf(foods.filter { it.isCustom })
    override fun searchMyFoods(query: String): Flow<List<FoodEntity>> =
      flowOf(foods.filter { it.isCustom && it.name.contains(query, true) })
    override suspend fun insert(food: FoodEntity): Long = food.id
    override suspend fun insertAll(foods: List<FoodEntity>) {}
    override suspend fun update(food: FoodEntity) {}
    override suspend fun delete(food: FoodEntity) {}
    override suspend fun archiveFood(id: Long): Int = 0
    override suspend fun getMaxId(): Long? = foods.maxOfOrNull { it.id }
    override suspend fun count(): Int = foods.size
    override suspend fun countActive(): Int = foods.size
    override suspend fun countActiveCustom(): Int = foods.count { it.isCustom }
  }

  private class FakeRecipeDao : RecipeDao {
    val recipes = mutableMapOf<Long, RecipeEntity>()
    val ingredients = mutableMapOf<Long, MutableList<RecipeIngredientEntity>>()
    private var nextRecipeId = 1L

    override suspend fun getRecipeById(id: Long): RecipeEntity? = recipes[id]
    override fun getAllActiveRecipes(): Flow<List<RecipeEntity>> =
      flowOf(recipes.values.filter { it.isActive })
    override fun searchActiveRecipes(query: String): Flow<List<RecipeEntity>> =
      flowOf(recipes.values.filter { it.isActive && it.name.contains(query, true) })

    override fun getIngredientsWithFood(recipeId: Long): Flow<List<RecipeIngredientWithFood>> =
      flowOf(emptyList())

    override suspend fun getIngredientsForRecipe(recipeId: Long): List<RecipeIngredientEntity> =
      ingredients[recipeId] ?: emptyList()

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
        val list = this.ingredients.getOrPut(ing.recipeId) { mutableListOf() }
        list.add(ing)
      }
    }

    override suspend fun deleteIngredientsForRecipe(recipeId: Long) {
      ingredients.remove(recipeId)
    }

    override suspend fun archiveRecipe(recipeId: Long): Int {
      val r = recipes[recipeId]
      return if (r != null) {
        recipes[recipeId] = r.copy(isActive = false)
        1
      } else 0
    }

    override suspend fun countActiveRecipes(): Int = recipes.values.count { it.isActive }

    override suspend fun saveRecipeWithIngredients(
      recipe: RecipeEntity,
      ingredients: List<RecipeIngredientEntity>
    ): Long {
      val targetId = if (recipe.id == 0L) insertRecipe(recipe) else {
        updateRecipe(recipe)
        deleteIngredientsForRecipe(recipe.id)
        recipe.id
      }
      insertIngredients(ingredients.map { it.copy(recipeId = targetId) })
      return targetId
    }
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
  fun initialState_emptyRecipe_startsClean() = runTest {
    val foodDao = FakeFoodDao(listOf(foodA, foodB))
    val recipeDao = FakeRecipeDao()
    val viewModel = RecipeBuilderViewModel(recipeId = 0L, recipeDao = recipeDao, foodDao = foodDao)

    val state = viewModel.uiState.value
    assertEquals(0L, state.recipeId)
    assertFalse(state.isEditMode)
    assertEquals("", state.name)
    assertTrue(state.ingredients.isEmpty())
    assertEquals("", state.cookedWeightInput)
    assertEquals(0.0, state.rawWeightGrams, 0.001)
    assertEquals(0.0, state.totalCalories, 0.001)
  }

  @Test
  fun validation_blankNameOrEmptyIngredients_failsValidation() = runTest {
    val foodDao = FakeFoodDao(listOf(foodA, foodB))
    val recipeDao = FakeRecipeDao()
    val viewModel = RecipeBuilderViewModel(recipeId = 0L, recipeDao = recipeDao, foodDao = foodDao)

    assertFalse(viewModel.validateAll())
    assertNotNull(viewModel.uiState.value.nameError)
    assertNotNull(viewModel.uiState.value.ingredientsError)
  }

  @Test
  fun addIngredients_updatesLiveTotalsAndDefaultCookedWeight() = runTest {
    val foodDao = FakeFoodDao(listOf(foodA, foodB))
    val recipeDao = FakeRecipeDao()
    val viewModel = RecipeBuilderViewModel(recipeId = 0L, recipeDao = recipeDao, foodDao = foodDao)

    // Add 100g Oats (379 kcal, 13.15 P, 67.7 C, 6.52 F)
    viewModel.onAddIngredientFood(foodA)
    var state = viewModel.uiState.value
    assertEquals(1, state.ingredients.size)
    assertEquals(100.0, state.rawWeightGrams, 0.001)
    assertEquals("100", state.cookedWeightInput)
    assertEquals(379.0, state.totalCalories, 0.001)
    assertEquals(379.0, state.caloriesPer100g, 0.001)

    // Add 200g Milk
    viewModel.onAddIngredientFood(foodB)
    val milkDraft = viewModel.uiState.value.ingredients[1]
    viewModel.onIngredientQuantityChanged(milkDraft.draftId, "200")

    state = viewModel.uiState.value
    assertEquals(2, state.ingredients.size)
    // Raw weight: 100 + 200 = 300g
    assertEquals(300.0, state.rawWeightGrams, 0.001)
    assertEquals("300", state.cookedWeightInput)
    // Milk 200g: 120 kcal, 6.4 P, 9.6 C, 6.6 F
    // Total Calories: 379 + 120 = 499 kcal
    assertEquals(499.0, state.totalCalories, 0.001)
    assertEquals(19.55, state.totalProtein, 0.001)
    // Density per 100g = 499 / 300 * 100 = 166.333 kcal
    assertEquals(166.333, state.caloriesPer100g, 0.01)
  }

  @Test
  fun customCookedWeight_changesDensityWithoutChangingTotals() = runTest {
    val foodDao = FakeFoodDao(listOf(foodA))
    val recipeDao = FakeRecipeDao()
    val viewModel = RecipeBuilderViewModel(recipeId = 0L, recipeDao = recipeDao, foodDao = foodDao)

    viewModel.onAddIngredientFood(foodA) // 100g Oats, 379 kcal
    assertEquals(379.0, viewModel.uiState.value.totalCalories, 0.001)
    assertEquals(379.0, viewModel.uiState.value.caloriesPer100g, 0.001)

    // Water evaporates or soup reduces, final cooked weight is 200g
    viewModel.onCookedWeightChanged("200")
    val state = viewModel.uiState.value
    assertTrue(state.isCookedWeightCustomized)
    // Total calories must NEVER change!
    assertEquals(379.0, state.totalCalories, 0.001)
    // Density per 100g is now 379 / 200 * 100 = 189.5 kcal / 100g
    assertEquals(189.5, state.caloriesPer100g, 0.001)

    // Reset back to raw weight
    viewModel.onResetCookedWeightToRaw()
    val resetState = viewModel.uiState.value
    assertFalse(resetState.isCookedWeightCustomized)
    assertEquals("100", resetState.cookedWeightInput)
    assertEquals(379.0, resetState.caloriesPer100g, 0.001)
  }

  @Test
  fun ingredientAdjustments_incrementDecrementAndRemove() = runTest {
    val foodDao = FakeFoodDao(listOf(foodA))
    val recipeDao = FakeRecipeDao()
    val viewModel = RecipeBuilderViewModel(recipeId = 0L, recipeDao = recipeDao, foodDao = foodDao)

    viewModel.onAddIngredientFood(foodA) // 100g
    val draftId = viewModel.uiState.value.ingredients.first().draftId

    // Increment +10g
    viewModel.onIncrementIngredientQuantity(draftId, step = 10.0)
    assertEquals(110.0, viewModel.uiState.value.ingredients.first().quantityGrams, 0.001)

    // Decrement -20g
    viewModel.onDecrementIngredientQuantity(draftId, step = 20.0)
    assertEquals(90.0, viewModel.uiState.value.ingredients.first().quantityGrams, 0.001)

    // Remove ingredient
    viewModel.onRemoveIngredient(draftId)
    assertTrue(viewModel.uiState.value.ingredients.isEmpty())
  }

  @Test
  fun saveRecipe_withValidData_persistsAndCallsCallback() = runTest {
    val foodDao = FakeFoodDao(listOf(foodA))
    val recipeDao = FakeRecipeDao()
    val viewModel = RecipeBuilderViewModel(recipeId = 0L, recipeDao = recipeDao, foodDao = foodDao)

    viewModel.onNameChanged("Oatmeal Porridge")
    viewModel.onAddIngredientFood(foodA)

    var savedId: Long? = null
    viewModel.saveRecipe { savedId = it }
    advanceUntilIdle()

    assertEquals(1L, savedId)
    val persisted = recipeDao.getRecipeById(1L)
    assertNotNull(persisted)
    assertEquals("Oatmeal Porridge", persisted!!.name)
    assertEquals(379.0, persisted.totalCalories, 0.001)
    assertEquals(1, recipeDao.getIngredientsForRecipe(1L).size)
  }

  @Test
  fun editMode_loadsExistingRecipeAndIngredients() = runTest {
    val foodDao = FakeFoodDao(listOf(foodA))
    val recipeDao = FakeRecipeDao()

    // Prepopulate recipe in database
    recipeDao.saveRecipeWithIngredients(
      RecipeEntity(
        id = 10L,
        name = "Loaded Oatmeal",
        cookedWeightGrams = 150.0,
        totalCalories = 379.0,
        totalProtein = 13.15,
        totalCarbs = 67.7,
        totalFat = 6.52,
        caloriesPer100g = 252.67,
        proteinPer100g = 8.77,
        carbsPer100g = 45.13,
        fatPer100g = 4.35,
        isActive = true,
      ),
      listOf(RecipeIngredientEntity(id = 1L, recipeId = 10L, foodId = 1L, quantityGrams = 100.0))
    )

    val viewModel = RecipeBuilderViewModel(recipeId = 10L, recipeDao = recipeDao, foodDao = foodDao)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.isEditMode)
    assertEquals("Loaded Oatmeal", state.name)
    assertEquals(1, state.ingredients.size)
    assertEquals("150", state.cookedWeightInput)
    assertTrue(state.isCookedWeightCustomized)
  }
}
