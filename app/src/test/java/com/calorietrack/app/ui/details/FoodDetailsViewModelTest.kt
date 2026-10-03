package com.calorietrack.app.ui.details

import com.calorietrack.app.data.local.DailyNutritionTotals
import com.calorietrack.app.data.local.FoodDao
import com.calorietrack.app.data.local.FoodEntity
import com.calorietrack.app.data.local.MealEntryDao
import com.calorietrack.app.data.local.MealEntryEntity
import com.calorietrack.app.data.local.MealEntryWithFood
import java.time.LocalDate
import java.time.LocalTime
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FoodDetailsViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private val sampleFood = FoodEntity(
    id = 42L,
    name = "Boiled Egg",
    servingDescription = "1 large egg (50g)",
    servingGrams = 50.0,
    caloriesPer100g = 155.0,
    proteinPer100g = 12.6,
    carbsPer100g = 1.1,
    fatPer100g = 10.6,
    isCustom = false,
    dataSource = "USDA FoodData Central",
    sourceId = "173424",
    searchKeywords = "egg, boiled egg, anda",
  )

  private lateinit var fakeFoodDao: FakeFoodDao
  private lateinit var fakeMealEntryDao: FakeMealEntryDao
  private val fixedDate = LocalDate.of(2026, 10, 2)
  private val fixedTime = LocalTime.of(8, 30) // 8:30 AM -> breakfast

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    fakeFoodDao = FakeFoodDao(listOf(sampleFood))
    fakeMealEntryDao = FakeMealEntryDao()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun initialState_loadsFood_defaultsToOneHundredGrams_andCalculatesReferenceNutrition() = runTest {
    val viewModel = FoodDetailsViewModel(
      foodId = 42L,
      initialMealType = "breakfast",
      foodDao = fakeFoodDao,
      mealEntryDao = fakeMealEntryDao,
      dateProvider = { fixedDate },
      timeProvider = { fixedTime },
    )
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals(sampleFood, state.food)
    assertEquals("100", state.quantityInput)
    assertEquals(100.0, state.quantityGrams)
    assertTrue(state.isValidQuantity)
    assertNull(state.quantityErrorMessage)
    assertEquals(155.0, state.calculatedCalories, 0.001)
    assertEquals(12.6, state.calculatedProtein, 0.001)
    assertEquals(1.1, state.calculatedCarbs, 0.001)
    assertEquals(10.6, state.calculatedFat, 0.001)
    assertEquals("Add to Breakfast", state.addButtonText)
  }

  @Test
  fun quantityCalculation_atTwoHundredGrams_doublesNutrition() = runTest {
    val viewModel = FoodDetailsViewModel(
      foodId = 42L,
      initialMealType = "lunch",
      foodDao = fakeFoodDao,
      mealEntryDao = fakeMealEntryDao,
      dateProvider = { fixedDate },
      timeProvider = { fixedTime },
    )
    advanceUntilIdle()

    viewModel.onQuantityChanged("200")

    val state = viewModel.uiState.value
    assertEquals("200", state.quantityInput)
    assertEquals(200.0, state.quantityGrams)
    assertTrue(state.isValidQuantity)
    assertEquals(310.0, state.calculatedCalories, 0.001)
    assertEquals(25.2, state.calculatedProtein, 0.001)
    assertEquals(2.2, state.calculatedCarbs, 0.001)
    assertEquals(21.2, state.calculatedFat, 0.001)
    assertEquals("Add to Lunch", state.addButtonText)
  }

  @Test
  fun quantityCalculation_atThirtySevenGrams_scalesAccurately() = runTest {
    val viewModel = FoodDetailsViewModel(
      foodId = 42L,
      initialMealType = "dinner",
      foodDao = fakeFoodDao,
      mealEntryDao = fakeMealEntryDao,
      dateProvider = { fixedDate },
      timeProvider = { fixedTime },
    )
    advanceUntilIdle()

    viewModel.onQuantityChanged("37")

    val state = viewModel.uiState.value
    assertEquals("37", state.quantityInput)
    assertEquals(37.0, state.quantityGrams)
    assertTrue(state.isValidQuantity)
    assertEquals(155.0 * 0.37, state.calculatedCalories, 0.001)
    assertEquals(12.6 * 0.37, state.calculatedProtein, 0.001)
    assertEquals(1.1 * 0.37, state.calculatedCarbs, 0.001)
    assertEquals(10.6 * 0.37, state.calculatedFat, 0.001)
    assertEquals("Add to Dinner", state.addButtonText)
  }

  @Test
  fun quantityValidation_rejectsZeroGrams() = runTest {
    val viewModel = FoodDetailsViewModel(
      foodId = 42L,
      initialMealType = "breakfast",
      foodDao = fakeFoodDao,
      mealEntryDao = fakeMealEntryDao,
      dateProvider = { fixedDate },
      timeProvider = { fixedTime },
    )
    advanceUntilIdle()

    viewModel.onQuantityChanged("0")

    val state = viewModel.uiState.value
    assertFalse(state.isValidQuantity)
    assertNull(state.quantityGrams)
    assertNotNull(state.quantityErrorMessage)
    assertEquals(0.0, state.calculatedCalories)
  }

  @Test
  fun quantityValidation_rejectsNegativeGrams() = runTest {
    val viewModel = FoodDetailsViewModel(
      foodId = 42L,
      initialMealType = "breakfast",
      foodDao = fakeFoodDao,
      mealEntryDao = fakeMealEntryDao,
      dateProvider = { fixedDate },
      timeProvider = { fixedTime },
    )
    advanceUntilIdle()

    viewModel.onQuantityChanged("-25")

    val state = viewModel.uiState.value
    assertFalse(state.isValidQuantity)
    assertNull(state.quantityGrams)
    assertNotNull(state.quantityErrorMessage)
    assertEquals(0.0, state.calculatedCalories)
  }

  @Test
  fun quantityValidation_rejectsNonNumericInput() = runTest {
    val viewModel = FoodDetailsViewModel(
      foodId = 42L,
      initialMealType = "breakfast",
      foodDao = fakeFoodDao,
      mealEntryDao = fakeMealEntryDao,
      dateProvider = { fixedDate },
      timeProvider = { fixedTime },
    )
    advanceUntilIdle()

    viewModel.onQuantityChanged("abc")

    val state = viewModel.uiState.value
    assertFalse(state.isValidQuantity)
    assertNull(state.quantityGrams)
    assertEquals("Please enter a valid number", state.quantityErrorMessage)
  }

  @Test
  fun mealTypePropagation_preservesAllMealTypesAndButtonText() = runTest {
    val meals = listOf(
      "breakfast" to "Add to Breakfast",
      "lunch" to "Add to Lunch",
      "dinner" to "Add to Dinner",
      "snack" to "Add to Snacks",
      "snacks" to "Add to Snacks",
    )

    for ((inputMeal, expectedButtonText) in meals) {
      val viewModel = FoodDetailsViewModel(
        foodId = 42L,
        initialMealType = inputMeal,
        foodDao = fakeFoodDao,
        mealEntryDao = fakeMealEntryDao,
        dateProvider = { fixedDate },
        timeProvider = { fixedTime },
      )
      advanceUntilIdle()

      assertEquals(expectedButtonText, viewModel.uiState.value.addButtonText)
    }
  }

  @Test
  fun mealTypeSelection_allowsSwitchingMealType() = runTest {
    val viewModel = FoodDetailsViewModel(
      foodId = 42L,
      initialMealType = "breakfast",
      foodDao = fakeFoodDao,
      mealEntryDao = fakeMealEntryDao,
      dateProvider = { fixedDate },
      timeProvider = { fixedTime },
    )
    advanceUntilIdle()

    viewModel.onMealTypeSelected("dinner")

    assertEquals("dinner", viewModel.uiState.value.selectedMealType)
    assertEquals("Add to Dinner", viewModel.uiState.value.addButtonText)
  }

  @Test
  fun incrementAndDecrement_stepsByTenGrams() = runTest {
    val viewModel = FoodDetailsViewModel(
      foodId = 42L,
      initialMealType = "breakfast",
      foodDao = fakeFoodDao,
      mealEntryDao = fakeMealEntryDao,
      dateProvider = { fixedDate },
      timeProvider = { fixedTime },
    )
    advanceUntilIdle()

    viewModel.onIncrementQuantity(10.0)
    assertEquals("110", viewModel.uiState.value.quantityInput)
    assertEquals(110.0, viewModel.uiState.value.quantityGrams)

    viewModel.onDecrementQuantity(10.0)
    assertEquals("100", viewModel.uiState.value.quantityInput)
    assertEquals(100.0, viewModel.uiState.value.quantityGrams)
  }

  @Test
  fun selectPreset_updatesQuantityAndRecalculates() = runTest {
    val viewModel = FoodDetailsViewModel(
      foodId = 42L,
      initialMealType = "breakfast",
      foodDao = fakeFoodDao,
      mealEntryDao = fakeMealEntryDao,
      dateProvider = { fixedDate },
      timeProvider = { fixedTime },
    )
    advanceUntilIdle()

    viewModel.onSelectPresetGrams(50.0)

    val state = viewModel.uiState.value
    assertEquals("50", state.quantityInput)
    assertEquals(50.0, state.quantityGrams)
    assertEquals(77.5, state.calculatedCalories, 0.001)
  }

  @Test
  fun logMeal_createsMealEntryEntityWithCorrectDateMealTypeFoodIdQuantityAndNutrition() = runTest {
    val viewModel = FoodDetailsViewModel(
      foodId = 42L,
      initialMealType = "lunch",
      foodDao = fakeFoodDao,
      mealEntryDao = fakeMealEntryDao,
      dateProvider = { fixedDate },
      timeProvider = { fixedTime },
    )
    advanceUntilIdle()

    viewModel.onQuantityChanged("150")

    var callbackCalled = false
    viewModel.logMeal(onSuccess = { callbackCalled = true })
    advanceUntilIdle()

    assertTrue(callbackCalled)
    assertEquals(1, fakeMealEntryDao.insertedEntries.size)

    val logged = fakeMealEntryDao.insertedEntries.first()
    assertEquals("2026-10-02", logged.date)
    assertEquals("lunch", logged.mealType)
    assertEquals(42L, logged.foodId)
    assertEquals(150.0, logged.quantityGrams, 0.001)
    assertEquals(155.0 * 1.5, logged.calories, 0.001)
    assertEquals(12.6 * 1.5, logged.protein, 0.001)
    assertEquals(1.1 * 1.5, logged.carbs, 0.001)
    assertEquals(10.6 * 1.5, logged.fat, 0.001)
  }

  @Test
  fun logMeal_rejectsWhenQuantityIsInvalid() = runTest {
    val viewModel = FoodDetailsViewModel(
      foodId = 42L,
      initialMealType = "breakfast",
      foodDao = fakeFoodDao,
      mealEntryDao = fakeMealEntryDao,
      dateProvider = { fixedDate },
      timeProvider = { fixedTime },
    )
    advanceUntilIdle()

    viewModel.onQuantityChanged("0")

    var callbackCalled = false
    viewModel.logMeal(onSuccess = { callbackCalled = true })
    advanceUntilIdle()

    assertFalse(callbackCalled)
    assertTrue(fakeMealEntryDao.insertedEntries.isEmpty())
  }

  @Test
  fun editMode_preloadsExistingEntryQuantityAndMealType() = runTest {
    val existingEntry = MealEntryEntity(
      id = 100L,
      date = "2026-10-02",
      mealType = "dinner",
      foodId = 42L,
      quantityGrams = 180.0,
      calories = 279.0,
      protein = 22.68,
      carbs = 1.98,
      fat = 19.08,
    )
    fakeMealEntryDao.insertedEntries.add(existingEntry)

    val viewModel = FoodDetailsViewModel(
      foodId = 42L,
      initialMealType = "dinner",
      mealEntryId = 100L,
      foodDao = fakeFoodDao,
      mealEntryDao = fakeMealEntryDao,
      dateProvider = { fixedDate },
      timeProvider = { fixedTime },
    )
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.isEditMode)
    assertEquals("180", state.quantityInput)
    assertEquals(180.0, state.quantityGrams)
    assertEquals("dinner", state.selectedMealType)
    assertEquals("Save Changes", state.addButtonText)
    assertEquals(155.0 * 1.8, state.calculatedCalories, 0.001)
  }

  @Test
  fun editMode_saveUpdatesExistingEntryId_andDoesNotCreateSecondEntry() = runTest {
    val existingEntry = MealEntryEntity(
      id = 100L,
      date = "2026-10-02",
      mealType = "dinner",
      foodId = 42L,
      quantityGrams = 180.0,
      calories = 279.0,
      protein = 22.68,
      carbs = 1.98,
      fat = 19.08,
    )
    fakeMealEntryDao.insertedEntries.add(existingEntry)

    val viewModel = FoodDetailsViewModel(
      foodId = 42L,
      initialMealType = "dinner",
      mealEntryId = 100L,
      foodDao = fakeFoodDao,
      mealEntryDao = fakeMealEntryDao,
      dateProvider = { fixedDate },
      timeProvider = { fixedTime },
    )
    advanceUntilIdle()

    viewModel.onQuantityChanged("250")

    var callbackCalled = false
    viewModel.logMeal(onSuccess = { callbackCalled = true })
    advanceUntilIdle()

    assertTrue(callbackCalled)
    // Confirm exact single entry in DAO (no duplicates)
    assertEquals(1, fakeMealEntryDao.insertedEntries.size)

    val updated = fakeMealEntryDao.insertedEntries.first()
    assertEquals(100L, updated.id) // Same ID preserved
    assertEquals("2026-10-02", updated.date) // Original date preserved
    assertEquals("dinner", updated.mealType)
    assertEquals(250.0, updated.quantityGrams, 0.001)
    assertEquals(155.0 * 2.5, updated.calories, 0.001)
    assertEquals(12.6 * 2.5, updated.protein, 0.001)
  }

  @Test
  fun editMode_cancellationLeavesEntryUnchanged() = runTest {
    val existingEntry = MealEntryEntity(
      id = 100L,
      date = "2026-10-02",
      mealType = "dinner",
      foodId = 42L,
      quantityGrams = 180.0,
      calories = 279.0,
      protein = 22.68,
      carbs = 1.98,
      fat = 19.08,
    )
    fakeMealEntryDao.insertedEntries.add(existingEntry)

    val viewModel = FoodDetailsViewModel(
      foodId = 42L,
      initialMealType = "dinner",
      mealEntryId = 100L,
      foodDao = fakeFoodDao,
      mealEntryDao = fakeMealEntryDao,
      dateProvider = { fixedDate },
      timeProvider = { fixedTime },
    )
    advanceUntilIdle()

    // User changes quantity to 300g, but navigates back/cancels without saving
    viewModel.onQuantityChanged("300")

    assertEquals(1, fakeMealEntryDao.insertedEntries.size)
    val unchanged = fakeMealEntryDao.insertedEntries.first()
    assertEquals(100L, unchanged.id)
    assertEquals(180.0, unchanged.quantityGrams, 0.001)
  }
}

class FakeFoodDao(private val foods: List<FoodEntity>) : FoodDao {
  override suspend fun getById(id: Long): FoodEntity? = foods.find { it.id == id }
  override fun getAll(): Flow<List<FoodEntity>> = flowOf(foods)
  override fun searchByName(query: String): Flow<List<FoodEntity>> = flowOf(foods.filter { it.name.contains(query, ignoreCase = true) })
  override suspend fun insert(food: FoodEntity): Long = food.id
  override suspend fun insertAll(foods: List<FoodEntity>) {}
  override suspend fun update(food: FoodEntity) {}
  override suspend fun delete(food: FoodEntity) {}
  override suspend fun count(): Int = foods.size
}

class FakeMealEntryDao : MealEntryDao {
  val insertedEntries = mutableListOf<MealEntryEntity>()

  override fun getEntriesForDate(date: String): Flow<List<MealEntryEntity>> =
    flowOf(insertedEntries.filter { it.date == date })

  override fun getEntriesForDateAndMealType(date: String, mealType: String): Flow<List<MealEntryEntity>> =
    flowOf(insertedEntries.filter { it.date == date && it.mealType == mealType })

  override suspend fun insert(entry: MealEntryEntity): Long {
    val generatedId = (insertedEntries.size + 1).toLong()
    val withId = if (entry.id == 0L) entry.copy(id = generatedId) else entry
    insertedEntries.add(withId)
    return withId.id
  }

  override suspend fun update(entry: MealEntryEntity) {
    val index = insertedEntries.indexOfFirst { it.id == entry.id }
    if (index != -1) {
      insertedEntries[index] = entry
    }
  }

  override suspend fun delete(entry: MealEntryEntity) {
    insertedEntries.removeAll { it.id == entry.id }
  }

  override suspend fun deleteById(id: Long) {
    insertedEntries.removeAll { it.id == id }
  }

  override suspend fun getEntryById(id: Long): MealEntryEntity? =
    insertedEntries.find { it.id == id }

  override suspend fun deleteForDate(date: String) {
    insertedEntries.removeAll { it.date == date }
  }

  override fun observeDailyTotals(date: String): Flow<DailyNutritionTotals> {
    val matching = insertedEntries.filter { it.date == date }
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
    val matching = insertedEntries.filter { it.date == date }
    return flowOf(
      matching.map {
        MealEntryWithFood(
          id = it.id,
          date = it.date,
          mealType = it.mealType,
          foodId = it.foodId,
          quantityGrams = it.quantityGrams,
          calories = it.calories,
          protein = it.protein,
          carbs = it.carbs,
          fat = it.fat,
          foodName = "Test Food ${it.foodId}",
        )
      }
    )
  }

  override fun getEntriesWithFoodForDateAndMealType(date: String, mealType: String): Flow<List<MealEntryWithFood>> {
    val matching = insertedEntries.filter {
      it.date == date && (it.mealType == mealType || (mealType == "snack" && it.mealType == "snacks") || (mealType == "snacks" && it.mealType == "snack"))
    }
    return flowOf(
      matching.map {
        MealEntryWithFood(
          id = it.id,
          date = it.date,
          mealType = it.mealType,
          foodId = it.foodId,
          quantityGrams = it.quantityGrams,
          calories = it.calories,
          protein = it.protein,
          carbs = it.carbs,
          fat = it.fat,
          foodName = "Test Food ${it.foodId}",
        )
      }
    )
  }

  override fun observeAllDailyTotals(): Flow<List<com.calorietrack.app.data.local.DailySummary>> = flowOf(emptyList())

  override fun getDatesWithEntries(): Flow<List<String>> =
    flowOf(insertedEntries.map { it.date }.distinct().sortedDescending())
}
