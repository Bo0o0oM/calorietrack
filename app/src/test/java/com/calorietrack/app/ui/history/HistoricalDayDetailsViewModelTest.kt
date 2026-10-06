package com.calorietrack.app.ui.history

import com.calorietrack.app.data.local.DailyGoalDao
import com.calorietrack.app.data.local.DailyGoalEntity
import com.calorietrack.app.data.local.DailyNutritionTotals
import com.calorietrack.app.data.local.DailySummary
import com.calorietrack.app.data.local.MealEntryDao
import com.calorietrack.app.data.local.MealEntryEntity
import com.calorietrack.app.data.local.MealEntryWithFood
import java.time.LocalDate
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoricalDayDetailsViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private val fixedToday = LocalDate.of(2026, 10, 3)
  private val targetDate = "2026-10-01"

  private class TestMealEntryDao : MealEntryDao {
    val entriesFlow = MutableStateFlow<List<MealEntryWithFood>>(emptyList())
    val totalsFlow = MutableStateFlow(DailyNutritionTotals(0.0, 0.0, 0.0, 0.0))

    override fun getEntriesWithFoodForDate(date: String): Flow<List<MealEntryWithFood>> = entriesFlow
    override fun observeDailyTotals(date: String): Flow<DailyNutritionTotals> = totalsFlow

    override fun getEntriesForDate(date: String): Flow<List<MealEntryEntity>> = flowOf(emptyList())
    override fun getEntriesForDateAndMealType(date: String, mealType: String): Flow<List<MealEntryEntity>> = flowOf(emptyList())
    override suspend fun insert(entry: MealEntryEntity): Long = 1L
    override suspend fun update(entry: MealEntryEntity) {}
    override suspend fun delete(entry: MealEntryEntity) {}
    override suspend fun deleteForDate(date: String) {}
    override suspend fun getEntryById(id: Long): MealEntryEntity? = null
    override suspend fun deleteById(id: Long) {}
    override fun getEntriesWithFoodForDateAndMealType(date: String, mealType: String): Flow<List<MealEntryWithFood>> = flowOf(emptyList())
    override fun observeAllDailyTotals(): Flow<List<DailySummary>> = flowOf(emptyList())
    override fun getDatesWithEntries(): Flow<List<String>> = flowOf(emptyList())
    override suspend fun getAllMealEntries(): List<MealEntryEntity> = emptyList()
    override suspend fun deleteAllMealEntries(): Int = 0
    override suspend fun insertAll(entries: List<MealEntryEntity>) {}
  }

  private class TestDailyGoalDao : DailyGoalDao {
    val goalFlow = MutableStateFlow<DailyGoalEntity?>(null)

    override fun observeForDate(date: String): Flow<DailyGoalEntity?> = goalFlow
    override suspend fun getForDate(date: String): DailyGoalEntity? = goalFlow.value
    override suspend fun insert(goal: DailyGoalEntity): Long = 1L
    override suspend fun update(goal: DailyGoalEntity) {}
    override suspend fun upsert(goal: DailyGoalEntity) {}
    override fun getAllGoals(): Flow<List<DailyGoalEntity>> = flowOf(emptyList())
    override fun getDatesWithGoals(): Flow<List<String>> = flowOf(emptyList())
    override suspend fun getAllDailyGoals(): List<DailyGoalEntity> = emptyList()
    override suspend fun deleteAllDailyGoals(): Int = 0
    override suspend fun insertAll(goals: List<DailyGoalEntity>) {}
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
  fun historicalDayDetails_groupsMealsAndCalculatesMealTotalsCorrectly() = runTest {
    val mealDao = TestMealEntryDao()
    val goalDao = TestDailyGoalDao()

    val entries = listOf(
      MealEntryWithFood(
        id = 1L,
        date = targetDate,
        mealType = "breakfast",
        foodId = 10L,
        quantityGrams = 100.0,
        calories = 389.0,
        protein = 16.9,
        carbs = 66.3,
        fat = 6.9,
        foodName = "Rolled Oats",
      ),
      MealEntryWithFood(
        id = 2L,
        date = targetDate,
        mealType = "breakfast",
        foodId = 20L,
        quantityGrams = 50.0,
        calories = 77.5,
        protein = 6.3,
        carbs = 0.5,
        fat = 5.3,
        foodName = "Boiled Egg",
      ),
      MealEntryWithFood(
        id = 3L,
        date = targetDate,
        mealType = "lunch",
        foodId = 30L,
        quantityGrams = 150.0,
        calories = 247.5,
        protein = 46.5,
        carbs = 0.0,
        fat = 5.4,
        foodName = "Chicken Breast",
      ),
      MealEntryWithFood(
        id = 4L,
        date = targetDate,
        mealType = "snacks",
        foodId = 40L,
        quantityGrams = 30.0,
        calories = 175.0,
        protein = 6.0,
        carbs = 6.0,
        fat = 15.0,
        foodName = "Almonds",
      ),
    )

    mealDao.entriesFlow.value = entries
    mealDao.totalsFlow.value = DailyNutritionTotals(
      totalCalories = 389.0 + 77.5 + 247.5 + 175.0, // 889.0
      totalProtein = 16.9 + 6.3 + 46.5 + 6.0,       // 75.7
      totalCarbs = 66.3 + 0.5 + 0.0 + 6.0,          // 72.8
      totalFat = 6.9 + 5.3 + 5.4 + 15.0,            // 32.6
    )

    val viewModel = HistoricalDayDetailsViewModel(
      dateIso = targetDate,
      mealEntryDao = mealDao,
      dailyGoalDao = goalDao,
      dateProvider = { fixedToday },
    )

    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals(889, state.totalCalories)
    assertEquals(76, state.totalProtein)
    assertEquals(73, state.totalCarbs)
    assertEquals(33, state.totalFat)

    // Check all 4 meal sections present
    assertEquals(4, state.meals.size)

    val breakfast = state.meals.first { it.key == "breakfast" }
    assertEquals(467, breakfast.consumedCalories) // 389 + 78
    assertEquals(2, breakfast.items.size)
    assertEquals("Rolled Oats", breakfast.items[0].name)
    assertEquals(100.0, breakfast.items[0].quantityGrams)
    assertEquals(389, breakfast.items[0].calories)
    assertEquals("Boiled Egg", breakfast.items[1].name)

    val lunch = state.meals.first { it.key == "lunch" }
    assertEquals(248, lunch.consumedCalories)
    assertEquals(1, lunch.items.size)
    assertEquals("Chicken Breast", lunch.items[0].name)

    val dinner = state.meals.first { it.key == "dinner" }
    assertEquals(0, dinner.consumedCalories)
    assertTrue(dinner.items.isEmpty())

    val snacks = state.meals.first { it.key == "snack" }
    assertEquals(175, snacks.consumedCalories)
    assertEquals(1, snacks.items.size)
    assertEquals("Almonds", snacks.items[0].name)
  }

  @Test
  fun historicalDayDetails_loadsGoalWhenPresent() = runTest {
    val mealDao = TestMealEntryDao()
    val goalDao = TestDailyGoalDao()

    goalDao.goalFlow.value = DailyGoalEntity(
      date = targetDate,
      calorieGoal = 2100.0,
      proteinGoal = 150.0,
      carbsGoal = 260.0,
      fatGoal = 70.0,
    )

    val viewModel = HistoricalDayDetailsViewModel(
      dateIso = targetDate,
      mealEntryDao = mealDao,
      dailyGoalDao = goalDao,
      dateProvider = { fixedToday },
    )

    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(2100, state.calorieGoal)
    assertEquals(150, state.proteinGoal)
    assertEquals(260, state.carbsGoal)
    assertEquals(70, state.fatGoal)
  }

  @Test
  fun historicalDayDetails_handlesMissingGoalWithoutSubstitutingTodayGoal() = runTest {
    val mealDao = TestMealEntryDao()
    val goalDao = TestDailyGoalDao()
    goalDao.goalFlow.value = null // No goal for targetDate

    val viewModel = HistoricalDayDetailsViewModel(
      dateIso = targetDate,
      mealEntryDao = mealDao,
      dailyGoalDao = goalDao,
      dateProvider = { fixedToday },
    )

    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNull(state.calorieGoal)
    assertNull(state.proteinGoal)
    assertNull(state.carbsGoal)
    assertNull(state.fatGoal)
  }

  @Test
  fun historicalDayDetails_preservesStoredNutritionWithoutRecalculation() = runTest {
    val mealDao = TestMealEntryDao()
    val goalDao = TestDailyGoalDao()

    // Specifically test that the stored values in MealEntryWithFood are what populate the UI
    val storedEntry = MealEntryWithFood(
      id = 99L,
      date = targetDate,
      mealType = "dinner",
      foodId = 55L,
      quantityGrams = 123.4,
      calories = 456.7, // Stored historical value
      protein = 34.5,
      carbs = 12.3,
      fat = 8.9,
      foodName = "Special Dish",
    )

    mealDao.entriesFlow.value = listOf(storedEntry)
    mealDao.totalsFlow.value = DailyNutritionTotals(456.7, 34.5, 12.3, 8.9)

    val viewModel = HistoricalDayDetailsViewModel(
      dateIso = targetDate,
      mealEntryDao = mealDao,
      dailyGoalDao = goalDao,
      dateProvider = { fixedToday },
    )

    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }
    advanceUntilIdle()

    val dinner = viewModel.uiState.value.meals.first { it.key == "dinner" }
    assertEquals(1, dinner.items.size)
    val item = dinner.items.first()
    assertEquals(123.4, item.quantityGrams)
    assertEquals(457, item.calories) // 456.7 rounded
    assertEquals(34.5, item.protein)
    assertEquals(12.3, item.carbs)
    assertEquals(8.9, item.fat)
  }
}
