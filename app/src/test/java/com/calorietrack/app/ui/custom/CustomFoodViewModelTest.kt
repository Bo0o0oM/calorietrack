package com.calorietrack.app.ui.custom

import com.calorietrack.app.data.local.FoodDao
import com.calorietrack.app.data.local.FoodEntity
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
class CustomFoodViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private class TestFoodDao : FoodDao {
    val foods = mutableMapOf<Long, FoodEntity>()

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

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun initialCreateState_isCleanAndValidatesEmptyFields() = runTest {
    val fakeDao = TestFoodDao()
    val viewModel = CustomFoodViewModel(foodId = 0L, mealType = "breakfast", initialName = "Tofu", foodDao = fakeDao)

    val state = viewModel.uiState.value
    assertEquals("Tofu", state.name)
    assertEquals("breakfast", state.mealType)
    assertFalse(state.isEditMode)
    assertFalse(state.isLoading)
    assertEquals("100", state.servingGrams)

    // Validate all fails because calories/macros/servingDescription are blank
    assertFalse(viewModel.validateAll())
    val validatedState = viewModel.uiState.value
    assertNotNull(validatedState.servingDescriptionError)
    assertNotNull(validatedState.caloriesError)
    assertNotNull(validatedState.proteinError)
    assertNotNull(validatedState.carbsError)
    assertNotNull(validatedState.fatError)
  }

  @Test
  fun validateFoodName_rejectsBlankAndOverlyLongNames() = runTest {
    val fakeDao = TestFoodDao()
    val viewModel = CustomFoodViewModel(foodDao = fakeDao)

    viewModel.onNameChanged("   ")
    assertEquals("Food name is required", viewModel.uiState.value.nameError)

    viewModel.onNameChanged("A".repeat(105))
    assertEquals("Food name cannot exceed 100 characters", viewModel.uiState.value.nameError)

    viewModel.onNameChanged("Homemade Paneer")
    assertNull(viewModel.uiState.value.nameError)
  }

  @Test
  fun validateServingGrams_rejectsZeroNegativeAndOver10000() = runTest {
    val fakeDao = TestFoodDao()
    val viewModel = CustomFoodViewModel(foodDao = fakeDao)

    viewModel.onServingGramsChanged("0")
    assertEquals("Serving grams must be greater than 0", viewModel.uiState.value.servingGramsError)

    viewModel.onServingGramsChanged("-10")
    assertEquals("Serving grams must be greater than 0", viewModel.uiState.value.servingGramsError)

    viewModel.onServingGramsChanged("abc")
    assertEquals("Enter a valid number", viewModel.uiState.value.servingGramsError)

    viewModel.onServingGramsChanged("10005")
    assertEquals("Serving grams cannot exceed 10000", viewModel.uiState.value.servingGramsError)

    viewModel.onServingGramsChanged("150.5")
    assertNull(viewModel.uiState.value.servingGramsError)
  }

  @Test
  fun validateCalories_rejectsNegativeAndOver1000() = runTest {
    val fakeDao = TestFoodDao()
    val viewModel = CustomFoodViewModel(foodDao = fakeDao)

    viewModel.onCaloriesChanged("-5")
    assertEquals("Calories cannot be negative", viewModel.uiState.value.caloriesError)

    viewModel.onCaloriesChanged("1050")
    assertEquals("Calories cannot exceed 1000", viewModel.uiState.value.caloriesError)

    viewModel.onCaloriesChanged("265")
    assertNull(viewModel.uiState.value.caloriesError)
  }

  @Test
  fun validateMacros_rejectsNegativeAndOver100AndMacroSumOver100() = runTest {
    val fakeDao = TestFoodDao()
    val viewModel = CustomFoodViewModel(foodDao = fakeDao)

    viewModel.onProteinChanged("-1")
    assertEquals("Protein cannot be negative", viewModel.uiState.value.proteinError)

    viewModel.onProteinChanged("105")
    assertEquals("Protein cannot exceed 100", viewModel.uiState.value.proteinError)

    viewModel.onProteinChanged("50")
    viewModel.onCarbsChanged("40")
    viewModel.onFatChanged("30") // 50 + 40 + 30 = 120 > 100
    assertEquals("Total macros (P + C + F) cannot exceed 100 g per 100 g", viewModel.uiState.value.macroSumError)

    viewModel.onFatChanged("10") // 50 + 40 + 10 = 100 <= 100
    assertNull(viewModel.uiState.value.macroSumError)
  }

  @Test
  fun saveCustomFood_allocatesIdStartingAt1000AndInserts() = runTest {
    val fakeDao = TestFoodDao()
    // Add existing built-in foods 1..50
    for (i in 1L..50L) {
      fakeDao.insert(
        FoodEntity(
          id = i,
          name = "Food $i",
          servingDescription = "100 g",
          servingGrams = 100.0,
          caloriesPer100g = 100.0,
          proteinPer100g = 10.0,
          carbsPer100g = 10.0,
          fatPer100g = 5.0,
          isCustom = false,
        )
      )
    }

    val viewModel = CustomFoodViewModel(foodId = 0L, mealType = "lunch", foodDao = fakeDao)
    viewModel.onNameChanged("Homemade Paneer")
    viewModel.onServingDescriptionChanged("100 g")
    viewModel.onServingGramsChanged("100")
    viewModel.onCaloriesChanged("265")
    viewModel.onProteinChanged("18")
    viewModel.onCarbsChanged("6")
    viewModel.onFatChanged("20")
    viewModel.onSearchKeywordsChanged("paneer, cottage cheese")

    var savedId = 0L
    viewModel.saveCustomFood { id -> savedId = id }
    advanceUntilIdle()

    assertEquals(1000L, savedId)
    val savedFood = fakeDao.getById(1000L)
    assertNotNull(savedFood)
    assertEquals("Homemade Paneer", savedFood!!.name)
    assertEquals(1000L, savedFood.id)
    assertTrue("Custom food must have isCustom = true", savedFood.isCustom)
    assertTrue("Custom food must have isActive = true", savedFood.isActive)
    assertEquals("User", savedFood.dataSource)
    assertEquals(265.0, savedFood.caloriesPer100g, 0.001)
    assertEquals(18.0, savedFood.proteinPer100g, 0.001)
    assertEquals(6.0, savedFood.carbsPer100g, 0.001)
    assertEquals(20.0, savedFood.fatPer100g, 0.001)
    assertEquals("paneer, cottage cheese", savedFood.searchKeywords)
  }

  @Test
  fun saveSecondCustomFood_allocatesIncrementedId() = runTest {
    val fakeDao = TestFoodDao()
    // Existing custom food with ID 1000
    fakeDao.insert(
      FoodEntity(
        id = 1000L,
        name = "First Custom Food",
        servingDescription = "100 g",
        servingGrams = 100.0,
        caloriesPer100g = 150.0,
        proteinPer100g = 10.0,
        carbsPer100g = 10.0,
        fatPer100g = 5.0,
        isCustom = true,
      )
    )

    val viewModel = CustomFoodViewModel(foodId = 0L, foodDao = fakeDao)
    viewModel.onNameChanged("Second Custom Food")
    viewModel.onServingDescriptionChanged("50 g")
    viewModel.onServingGramsChanged("50")
    viewModel.onCaloriesChanged("200")
    viewModel.onProteinChanged("15")
    viewModel.onCarbsChanged("5")
    viewModel.onFatChanged("10")

    var savedId = 0L
    viewModel.saveCustomFood { id -> savedId = id }
    advanceUntilIdle()

    assertEquals(1001L, savedId)
    val secondFood = fakeDao.getById(1001L)
    assertNotNull(secondFood)
    assertEquals(1001L, secondFood!!.id)
  }

  @Test
  fun editCustomFood_preloadsAndUpdatesExistingEntityWithoutCreatingDuplicate() = runTest {
    val fakeDao = TestFoodDao()
    fakeDao.insert(
      FoodEntity(
        id = 1002L,
        name = "Keto Bread",
        servingDescription = "1 slice (40g)",
        servingGrams = 40.0,
        caloriesPer100g = 220.0,
        proteinPer100g = 12.0,
        carbsPer100g = 4.0,
        fatPer100g = 14.0,
        isCustom = true,
        dataSource = "User",
        searchKeywords = "bread, low carb",
      )
    )

    val viewModel = CustomFoodViewModel(foodId = 1002L, foodDao = fakeDao)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.isEditMode)
    assertEquals("Keto Bread", state.name)
    assertEquals("1 slice (40g)", state.servingDescription)
    assertEquals("40", state.servingGrams)
    assertEquals("220", state.caloriesPer100g)
    assertEquals("12", state.proteinPer100g)
    assertEquals("4", state.carbsPer100g)
    assertEquals("14", state.fatPer100g)
    assertEquals("bread, low carb", state.searchKeywords)

    // Edit calories and carbs
    viewModel.onCaloriesChanged("230")
    viewModel.onCarbsChanged("5")

    var updatedId = 0L
    viewModel.saveCustomFood { id -> updatedId = id }
    advanceUntilIdle()

    assertEquals(1002L, updatedId)
    assertEquals(1, fakeDao.foods.size) // No duplicate row created
    val updatedFood = fakeDao.getById(1002L)
    assertNotNull(updatedFood)
    assertEquals(230.0, updatedFood!!.caloriesPer100g, 0.001)
    assertEquals(5.0, updatedFood.carbsPer100g, 0.001)
    assertEquals(1002L, updatedFood.id)
  }

  @Test
  fun editBuiltInFood_isForbidden() = runTest {
    val fakeDao = TestFoodDao()
    fakeDao.insert(
      FoodEntity(
        id = 1L,
        name = "White Rice",
        servingDescription = "1 cup",
        servingGrams = 158.0,
        caloriesPer100g = 130.0,
        proteinPer100g = 2.7,
        carbsPer100g = 28.2,
        fatPer100g = 0.3,
        isCustom = false,
      )
    )

    val viewModel = CustomFoodViewModel(foodId = 1L, foodDao = fakeDao)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("Built-in catalogue foods cannot be edited.", state.generalError)

    var saved = false
    viewModel.saveCustomFood { saved = true }
    advanceUntilIdle()

    assertFalse("Built-in food save must be prevented", saved)
  }
}
