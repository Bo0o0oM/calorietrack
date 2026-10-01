package com.calorietrack.app.ui.search

import com.calorietrack.app.data.local.FoodDao
import com.calorietrack.app.data.local.FoodEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
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
      ),
    )

  private class FakeFoodDao(private val allFoods: List<FoodEntity>) : FoodDao {
    override fun getAll(): Flow<List<FoodEntity>> = flowOf(allFoods)

    override fun searchByName(query: String): Flow<List<FoodEntity>> {
      val matches = allFoods.filter { it.name.contains(query, ignoreCase = true) }
      return flowOf(matches)
    }

    override suspend fun getById(id: Long): FoodEntity? = allFoods.find { it.id == id }
    override suspend fun insert(food: FoodEntity): Long = food.id
    override suspend fun insertAll(foods: List<FoodEntity>) {}
    override suspend fun update(food: FoodEntity) {}
    override suspend fun delete(food: FoodEntity) {}
    override suspend fun count(): Int = allFoods.size
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
  fun formatMealContextSubtitle_formatsCorrectlyForDifferentMeals() {
    assertEquals("Add to Breakfast", formatMealContextSubtitle("breakfast"))
    assertEquals("Add to Lunch", formatMealContextSubtitle("lunch"))
    assertEquals("Add to Dinner", formatMealContextSubtitle("dinner"))
    assertEquals("Add to Snacks", formatMealContextSubtitle("snack"))
    assertEquals("Add to Snacks", formatMealContextSubtitle("snacks"))
    assertEquals("Offline Food Catalogue", formatMealContextSubtitle(""))
  }
}
