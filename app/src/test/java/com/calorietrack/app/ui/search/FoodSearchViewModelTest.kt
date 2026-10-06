package com.calorietrack.app.ui.search

import com.calorietrack.app.data.local.FoodDao
import com.calorietrack.app.data.local.FoodEntity
import com.calorietrack.app.data.local.RecipeDao
import com.calorietrack.app.data.local.RecipeEntity
import com.calorietrack.app.data.local.RecipeIngredientEntity
import com.calorietrack.app.data.local.RecipeIngredientWithFood
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FoodSearchViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private val sampleCatalogue =
    listOf(
      FoodEntity(
        id = 1L,
        name = "Apple, raw, with skin",
        servingDescription = "1 medium apple (approx. 182g)",
        servingGrams = 182.0,
        caloriesPer100g = 52.0,
        proteinPer100g = 0.26,
        carbsPer100g = 13.81,
        fatPer100g = 0.17,
        isCustom = false,
        dataSource = "USDA FoodData Central",
        sourceId = "171688",
        searchKeywords = "apple, seb, fruit",
      ),
      FoodEntity(
        id = 2L,
        name = "Chicken Breast, cooked",
        servingDescription = "1 breast (approx. 172g)",
        servingGrams = 172.0,
        caloriesPer100g = 165.0,
        proteinPer100g = 31.02,
        carbsPer100g = 0.0,
        fatPer100g = 3.57,
        isCustom = false,
        dataSource = "USDA FoodData Central",
        sourceId = "171077",
        searchKeywords = "chicken, murgh, poultry, meat",
      ),
      FoodEntity(
        id = 3L,
        name = "Egg, whole, hard-boiled",
        servingDescription = "1 large egg (approx. 50g)",
        servingGrams = 50.0,
        caloriesPer100g = 155.0,
        proteinPer100g = 12.58,
        carbsPer100g = 1.12,
        fatPer100g = 10.61,
        isCustom = false,
        dataSource = "USDA FoodData Central",
        sourceId = "173424",
        searchKeywords = "egg, anda, boiled egg",
      ),
    )

  private class FakeFoodDao(initialFoods: List<FoodEntity>) : FoodDao {
    private val allFoods = initialFoods.toMutableList()
    private val flow = kotlinx.coroutines.flow.MutableStateFlow<List<FoodEntity>>(allFoods.toList())

    override fun getAll(): Flow<List<FoodEntity>> =
      flow.map { list -> list.filter { it.isActive } }

    override fun searchByName(query: String): Flow<List<FoodEntity>> =
      flow.map { list ->
        list.filter {
          it.isActive && (it.name.contains(query, ignoreCase = true) || it.searchKeywords.contains(query, ignoreCase = true))
        }
      }

    override fun getMyFoods(): Flow<List<FoodEntity>> =
      flow.map { list -> list.filter { it.isCustom && it.isActive } }

    override fun searchMyFoods(query: String): Flow<List<FoodEntity>> =
      flow.map { list ->
        list.filter {
          it.isCustom && it.isActive && (it.name.contains(query, ignoreCase = true) || it.searchKeywords.contains(query, ignoreCase = true))
        }
      }

    override suspend fun getById(id: Long): FoodEntity? = allFoods.find { it.id == id }

    override suspend fun insert(food: FoodEntity): Long {
      allFoods.removeAll { it.id == food.id }
      allFoods.add(food)
      flow.value = allFoods.toList()
      return food.id
    }

    override suspend fun insertAll(foods: List<FoodEntity>) {
      foods.forEach { insert(it) }
    }

    override suspend fun update(food: FoodEntity) {
      insert(food)
    }

    override suspend fun delete(food: FoodEntity) {
      allFoods.removeAll { it.id == food.id }
      flow.value = allFoods.toList()
    }

    override suspend fun archiveFood(id: Long): Int {
      val index = allFoods.indexOfFirst { it.id == id && it.isCustom }
      return if (index >= 0) {
        allFoods[index] = allFoods[index].copy(isActive = false)
        flow.value = allFoods.toList()
        1
      } else {
        0
      }
    }

    override suspend fun getMaxId(): Long? = allFoods.maxOfOrNull { it.id }
    override suspend fun count(): Int = allFoods.size
    override suspend fun countActive(): Int = allFoods.count { it.isActive }
    override suspend fun countActiveCustom(): Int = allFoods.count { it.isCustom && it.isActive }
    override suspend fun getAllCustomFoods(): List<FoodEntity> = allFoods.filter { it.isCustom }
    override suspend fun deleteAllCustomFoods(): Int = 0
  }

  private class FakeRecipeDao(initialRecipes: List<RecipeEntity> = emptyList()) : RecipeDao {
    private val allRecipes = initialRecipes.toMutableList()
    private val flow = kotlinx.coroutines.flow.MutableStateFlow<List<RecipeEntity>>(allRecipes.toList())

    override suspend fun getRecipeById(id: Long): RecipeEntity? = allRecipes.find { it.id == id }

    override fun getAllActiveRecipes(): Flow<List<RecipeEntity>> =
      flow.map { list -> list.filter { it.isActive } }

    override fun searchActiveRecipes(query: String): Flow<List<RecipeEntity>> =
      flow.map { list ->
        list.filter {
          it.isActive && (it.name.contains(query, ignoreCase = true) || it.searchKeywords.contains(query, ignoreCase = true))
        }
      }

    override fun getIngredientsWithFood(recipeId: Long): Flow<List<RecipeIngredientWithFood>> = flowOf(emptyList())
    override suspend fun getIngredientsForRecipe(recipeId: Long): List<RecipeIngredientEntity> = emptyList()
    override suspend fun insertRecipe(recipe: RecipeEntity): Long {
      allRecipes.add(recipe)
      flow.value = allRecipes.toList()
      return recipe.id
    }
    override suspend fun updateRecipe(recipe: RecipeEntity) {
      allRecipes.removeAll { it.id == recipe.id }
      allRecipes.add(recipe)
      flow.value = allRecipes.toList()
    }
    override suspend fun insertIngredients(ingredients: List<RecipeIngredientEntity>) {}
    override suspend fun deleteIngredientsForRecipe(recipeId: Long) {}
    override suspend fun archiveRecipe(recipeId: Long): Int {
      val index = allRecipes.indexOfFirst { it.id == recipeId }
      return if (index >= 0) {
        allRecipes[index] = allRecipes[index].copy(isActive = false)
        flow.value = allRecipes.toList()
        1
      } else 0
    }
    override suspend fun countActiveRecipes(): Int = allRecipes.count { it.isActive }
    override suspend fun saveRecipeWithIngredients(recipe: RecipeEntity, ingredients: List<RecipeIngredientEntity>): Long = recipe.id
    override suspend fun getAllRecipes(): List<RecipeEntity> = allRecipes.toList()
    override suspend fun getAllRecipeIngredients(): List<RecipeIngredientEntity> = emptyList()
    override suspend fun deleteAllRecipeIngredients(): Int = 0
    override suspend fun deleteAllRecipes(): Int { val s = allRecipes.size; allRecipes.clear(); return s }
    override suspend fun insertRecipes(recipesList: List<RecipeEntity>) { allRecipes.addAll(recipesList) }
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
  fun initialState_withBlankQuery_emitsAllFoodsFromCatalogue() = runTest {
    val fakeDao = FakeFoodDao(sampleCatalogue)
    val viewModel = FoodSearchViewModel(fakeDao, mealType = "breakfast")
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("", state.query)
    assertEquals("breakfast", state.mealType)
    assertFalse(state.isLoading)
    assertTrue(state.isQueryBlank)
    assertFalse(state.isEmptyResult)
    assertEquals(3, state.results.size)
    assertEquals("Apple, raw, with skin", state.results[0].name)
  }

  @Test
  fun searchQuery_withMatchingText_filtersResults() = runTest {
    val fakeDao = FakeFoodDao(sampleCatalogue)
    val viewModel = FoodSearchViewModel(fakeDao, mealType = "lunch")
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onQueryChanged("egg")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("egg", state.query)
    assertEquals(1, state.results.size)
    assertEquals("Egg, whole, hard-boiled", state.results[0].name)
    assertEquals(155.0, state.results[0].caloriesPer100g, 0.001)
  }

  @Test
  fun searchQuery_withSurroundingWhitespace_trimsQueryBeforeSearching() = runTest {
    val fakeDao = FakeFoodDao(sampleCatalogue)
    val viewModel = FoodSearchViewModel(fakeDao, mealType = "dinner")
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onQueryChanged("   chicken   ")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("   chicken   ", state.query)
    assertEquals(1, state.results.size)
    assertEquals("Chicken Breast, cooked", state.results[0].name)
  }

  @Test
  fun searchQuery_withNonMatchingText_emitsEmptyListAndFlagsEmptyResult() = runTest {
    val fakeDao = FakeFoodDao(sampleCatalogue)
    val viewModel = FoodSearchViewModel(fakeDao, mealType = "snack")
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onQueryChanged("avocado")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("avocado", state.query)
    assertTrue(state.results.isEmpty())
    assertTrue(state.isEmptyResult)
  }

  @Test
  fun onClearQuery_resetsQueryAndReturnsAllCatalogueFoods() = runTest {
    val fakeDao = FakeFoodDao(sampleCatalogue)
    val viewModel = FoodSearchViewModel(fakeDao, mealType = "")
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onQueryChanged("egg")
    advanceUntilIdle()
    assertEquals(1, viewModel.uiState.value.results.size)

    viewModel.onClearQuery()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("", state.query)
    assertEquals(3, state.results.size)
  }

  @Test
  fun searchQuery_withSearchKeyword_matchesFoodsByKeyword() = runTest {
    val fakeDao = FakeFoodDao(sampleCatalogue)
    val viewModel = FoodSearchViewModel(fakeDao, mealType = "breakfast")
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    // "seb" is Hindi keyword for Apple
    viewModel.onQueryChanged("seb")
    advanceUntilIdle()
    val state = viewModel.uiState.value
    assertEquals(1, state.results.size)
    assertEquals("Apple, raw, with skin", state.results[0].name)

    // "murgh" is Hindi keyword for Chicken
    viewModel.onQueryChanged("murgh")
    advanceUntilIdle()
    assertEquals(1, viewModel.uiState.value.results.size)
    assertEquals("Chicken Breast, cooked", viewModel.uiState.value.results[0].name)

    // "anda" is Hindi keyword for Egg
    viewModel.onQueryChanged("anda")
    advanceUntilIdle()
    assertEquals(1, viewModel.uiState.value.results.size)
    assertEquals("Egg, whole, hard-boiled", viewModel.uiState.value.results[0].name)
  }

  @Test
  fun searchQuery_rapidSequentialInput_maintainsStateCorrectly() = runTest {
    val fakeDao = FakeFoodDao(sampleCatalogue)
    val viewModel = FoodSearchViewModel(fakeDao, mealType = "lunch")
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    // Simulate typing character by character: "e" -> "eg" -> "egg"
    viewModel.onQueryChanged("e")
    viewModel.onQueryChanged("eg")
    viewModel.onQueryChanged("egg")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("egg", state.query)
    assertEquals(1, state.results.size)
    assertEquals("Egg, whole, hard-boiled", state.results[0].name)
  }

  @Test
  fun formatMealContextSubtitle_formatsCorrectlyForDifferentMeals() {
    assertEquals("Add to Breakfast", formatMealContextSubtitle("breakfast"))
    assertEquals("Add to Lunch", formatMealContextSubtitle("lunch"))
    assertEquals("Add to Dinner", formatMealContextSubtitle("dinner"))
    assertEquals("Add to Snacks", formatMealContextSubtitle("snack"))
    assertEquals("Add to Snacks", formatMealContextSubtitle("snacks"))
    assertEquals("Offline Food Catalogue", formatMealContextSubtitle(""))
  }

  @Test
  fun tabSelection_switchesBetweenAllAndMyFoods() = runTest {
    val customFood = FoodEntity(
      id = 1000L,
      name = "Homemade Paneer",
      servingDescription = "100 g",
      servingGrams = 100.0,
      caloriesPer100g = 265.0,
      proteinPer100g = 18.0,
      carbsPer100g = 6.0,
      fatPer100g = 20.0,
      isCustom = true,
      dataSource = "User",
      searchKeywords = "paneer, cottage cheese",
    )
    val fakeDao = FakeFoodDao(sampleCatalogue + customFood)
    val viewModel = FoodSearchViewModel(fakeDao, mealType = "lunch")
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    advanceUntilIdle()
    // Default tab is ALL: 3 built-in + 1 custom = 4 foods
    assertEquals(FoodSearchTab.ALL, viewModel.uiState.value.selectedTab)
    assertEquals(4, viewModel.uiState.value.results.size)

    // Switch to MY_FOODS
    viewModel.onTabSelected(FoodSearchTab.MY_FOODS)
    advanceUntilIdle()

    assertEquals(FoodSearchTab.MY_FOODS, viewModel.uiState.value.selectedTab)
    assertEquals(1, viewModel.uiState.value.results.size)
    assertEquals("Homemade Paneer", viewModel.uiState.value.results[0].name)
    assertTrue(viewModel.uiState.value.results[0].isCustom)
  }

  @Test
  fun searchQuery_withinMyFoods_filtersOnlyCustomFoods() = runTest {
    val custom1 = FoodEntity(
      id = 1000L,
      name = "Homemade Paneer",
      servingDescription = "100 g",
      servingGrams = 100.0,
      caloriesPer100g = 265.0,
      proteinPer100g = 18.0,
      carbsPer100g = 6.0,
      fatPer100g = 20.0,
      isCustom = true,
      dataSource = "User",
      searchKeywords = "paneer, cottage cheese",
    )
    val custom2 = FoodEntity(
      id = 1001L,
      name = "Protein Oats Shake",
      servingDescription = "1 glass (300g)",
      servingGrams = 300.0,
      caloriesPer100g = 110.0,
      proteinPer100g = 10.0,
      carbsPer100g = 12.0,
      fatPer100g = 2.0,
      isCustom = true,
      dataSource = "User",
      searchKeywords = "shake, smoothie",
    )
    val fakeDao = FakeFoodDao(sampleCatalogue + listOf(custom1, custom2))
    val viewModel = FoodSearchViewModel(fakeDao, mealType = "dinner")
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onTabSelected(FoodSearchTab.MY_FOODS)
    advanceUntilIdle()
    assertEquals(2, viewModel.uiState.value.results.size)

    viewModel.onQueryChanged("paneer")
    advanceUntilIdle()
    assertEquals(1, viewModel.uiState.value.results.size)
    assertEquals("Homemade Paneer", viewModel.uiState.value.results[0].name)

    // Searching for built-in food name while in My Foods yields 0
    viewModel.onQueryChanged("apple")
    advanceUntilIdle()
    assertEquals(0, viewModel.uiState.value.results.size)
    assertTrue(viewModel.uiState.value.isEmptyResult)
  }

  @Test
  fun archiveFood_removesCustomFoodFromActiveResults() = runTest {
    val custom = FoodEntity(
      id = 1000L,
      name = "Homemade Paneer",
      servingDescription = "100 g",
      servingGrams = 100.0,
      caloriesPer100g = 265.0,
      proteinPer100g = 18.0,
      carbsPer100g = 6.0,
      fatPer100g = 20.0,
      isCustom = true,
      dataSource = "User",
    )
    val fakeDao = FakeFoodDao(sampleCatalogue + custom)
    val viewModel = FoodSearchViewModel(fakeDao, mealType = "lunch")
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onTabSelected(FoodSearchTab.MY_FOODS)
    advanceUntilIdle()
    assertEquals(1, viewModel.uiState.value.results.size)

    viewModel.archiveFood(1000L)
    advanceUntilIdle()

    assertEquals(0, viewModel.uiState.value.results.size)
    assertTrue(viewModel.uiState.value.isEmptyResult)
  }

  @Test
  fun tabSelection_switchesToMyRecipesAndEmitsActiveRecipes() = runTest {
    val sampleRecipe = RecipeEntity(
      id = 1L,
      name = "Shahi Paneer",
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
      searchKeywords = "curry, paneer",
    )
    val fakeFoodDao = FakeFoodDao(sampleCatalogue)
    val fakeRecipeDao = FakeRecipeDao(listOf(sampleRecipe))
    val viewModel = FoodSearchViewModel(fakeFoodDao, fakeRecipeDao, mealType = "dinner")
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    advanceUntilIdle()
    assertEquals(FoodSearchTab.ALL, viewModel.uiState.value.selectedTab)
    assertEquals(3, viewModel.uiState.value.results.size)
    assertTrue(viewModel.uiState.value.recipeResults.isEmpty())

    // Switch to MY_RECIPES
    viewModel.onTabSelected(FoodSearchTab.MY_RECIPES)
    advanceUntilIdle()

    assertEquals(FoodSearchTab.MY_RECIPES, viewModel.uiState.value.selectedTab)
    assertTrue(viewModel.uiState.value.results.isEmpty())
    assertEquals(1, viewModel.uiState.value.recipeResults.size)
    assertEquals("Shahi Paneer", viewModel.uiState.value.recipeResults[0].name)
  }

  @Test
  fun searchQuery_withinMyRecipes_filtersRecipesByNameAndKeywords() = runTest {
    val recipe1 = RecipeEntity(
      id = 1L,
      name = "Shahi Paneer",
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
      searchKeywords = "curry, cottage cheese",
    )
    val recipe2 = RecipeEntity(
      id = 2L,
      name = "Oatmeal Bowl",
      cookedWeightGrams = 200.0,
      totalCalories = 300.0,
      totalProtein = 10.0,
      totalCarbs = 50.0,
      totalFat = 5.0,
      caloriesPer100g = 150.0,
      proteinPer100g = 5.0,
      carbsPer100g = 25.0,
      fatPer100g = 2.5,
      isActive = true,
      searchKeywords = "breakfast, porridge",
    )
    val fakeFoodDao = FakeFoodDao(sampleCatalogue)
    val fakeRecipeDao = FakeRecipeDao(listOf(recipe1, recipe2))
    val viewModel = FoodSearchViewModel(fakeFoodDao, fakeRecipeDao, mealType = "breakfast")
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onTabSelected(FoodSearchTab.MY_RECIPES)
    advanceUntilIdle()
    assertEquals(2, viewModel.uiState.value.recipeResults.size)

    // Filter by name
    viewModel.onQueryChanged("paneer")
    advanceUntilIdle()
    assertEquals(1, viewModel.uiState.value.recipeResults.size)
    assertEquals("Shahi Paneer", viewModel.uiState.value.recipeResults[0].name)

    // Filter by keyword
    viewModel.onQueryChanged("porridge")
    advanceUntilIdle()
    assertEquals(1, viewModel.uiState.value.recipeResults.size)
    assertEquals("Oatmeal Bowl", viewModel.uiState.value.recipeResults[0].name)

    // Non-matching query
    viewModel.onQueryChanged("pizza")
    advanceUntilIdle()
    assertEquals(0, viewModel.uiState.value.recipeResults.size)
    assertTrue(viewModel.uiState.value.isEmptyResult)
  }

  @Test
  fun archiveRecipe_removesRecipeFromActiveResults() = runTest {
    val recipe = RecipeEntity(
      id = 1L,
      name = "Shahi Paneer",
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
    val fakeFoodDao = FakeFoodDao(sampleCatalogue)
    val fakeRecipeDao = FakeRecipeDao(listOf(recipe))
    val viewModel = FoodSearchViewModel(fakeFoodDao, fakeRecipeDao, mealType = "dinner")
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onTabSelected(FoodSearchTab.MY_RECIPES)
    advanceUntilIdle()
    assertEquals(1, viewModel.uiState.value.recipeResults.size)

    viewModel.archiveRecipe(1L)
    advanceUntilIdle()
    assertEquals(0, viewModel.uiState.value.recipeResults.size)
    assertTrue(viewModel.uiState.value.isEmptyResult)
  }
}
