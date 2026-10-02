package com.calorietrack.app.ui.main

import com.calorietrack.app.data.local.DailyGoalDao
import com.calorietrack.app.data.local.DailyGoalEntity
import com.calorietrack.app.data.local.DailyNutritionTotals
import com.calorietrack.app.data.local.MealEntryDao
import com.calorietrack.app.data.local.MealEntryEntity
import com.calorietrack.app.data.local.MealEntryWithFood
import java.time.LocalDate
import java.time.LocalTime
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
class MainScreenViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun uiState_initialState_hasZeroConsumedAndTargetRemaining() = runTest {
    val fixedTime = LocalTime.of(9, 30) // 9:30 AM
    val fixedDate = LocalDate.of(2026, 9, 30)
    val viewModel = MainScreenViewModel(timeProvider = { fixedTime }, dateProvider = { fixedDate })

    val state = viewModel.uiState.first()

    assertEquals(0, state.consumedCalories)
    assertEquals(2000, state.targetCalories)
    assertEquals(2000, state.remainingCalories)
    assertEquals(0f, state.calorieProgress)
    assertTrue(state.isEmptyDay)
  }

  @Test
  fun uiState_initialState_hasExpectedMacrosAndZeroConsumed() = runTest {
    val viewModel = MainScreenViewModel()
    val state = viewModel.uiState.first()

    assertEquals("Protein", state.protein.name)
    assertEquals(0, state.protein.consumed)
    assertEquals(140, state.protein.target)

    assertEquals("Carbs", state.carbs.name)
    assertEquals(0, state.carbs.consumed)
    assertEquals(250, state.carbs.target)

    assertEquals("Fat", state.fat.name)
    assertEquals(0, state.fat.consumed)
    assertEquals(70, state.fat.target)
  }

  @Test
  fun uiState_initialState_containsAllFourMealSections() = runTest {
    val viewModel = MainScreenViewModel()
    val state = viewModel.uiState.first()

    assertEquals(4, state.meals.size)
    val mealKeys = state.meals.map { it.key }
    assertEquals(listOf("breakfast", "lunch", "dinner", "snack"), mealKeys)
    state.meals.forEach { meal ->
      assertEquals(0, meal.consumedCalories)
      assertTrue(meal.items.isEmpty())
    }
  }

  @Test
  fun greeting_returnsAppropriateGreetingByHour() {
    assertEquals("Good morning", calculateGreeting(5))
    assertEquals("Good morning", calculateGreeting(11))
    assertEquals("Good afternoon", calculateGreeting(12))
    assertEquals("Good afternoon", calculateGreeting(16))
    assertEquals("Good evening", calculateGreeting(17))
    assertEquals("Good evening", calculateGreeting(22))
    assertEquals("Good evening", calculateGreeting(2))
  }

  @Test
  fun dashboardUiState_remainingAndProgress_computeAccurately() {
    val state = DashboardUiState(
      greeting = "Good morning",
      dateText = "Today, Sep 30",
      consumedCalories = 750,
      targetCalories = 2000,
      isEmptyDay = false,
    )

    assertEquals(1250, state.remainingCalories)
    assertEquals(0.375f, state.calorieProgress, 0.001f)
  }

  @Test
  fun uiState_withMealEntries_calculatesTotalsAndPopulatesMealCards() = runTest {
    val fixedDate = LocalDate.of(2026, 10, 2)
    val fixedTime = LocalTime.of(12, 0)
    val todayIso = "2026-10-02"

    val fakeEntries = listOf(
      MealEntryWithFood(
        id = 1L,
        date = todayIso,
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
        date = todayIso,
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
        date = todayIso,
        mealType = "lunch",
        foodId = 30L,
        quantityGrams = 150.0,
        calories = 247.5,
        protein = 46.5,
        carbs = 0.0,
        fat = 5.4,
        foodName = "Chicken Breast",
      ),
    )

    val fakeTotals = DailyNutritionTotals(
      totalCalories = 389.0 + 77.5 + 247.5, // 714.0
      totalProtein = 16.9 + 6.3 + 46.5,     // 69.7
      totalCarbs = 66.3 + 0.5 + 0.0,        // 66.8
      totalFat = 6.9 + 5.3 + 5.4,           // 17.6
    )

    val fakeDao = object : MealEntryDao {
      override fun getEntriesForDate(date: String): Flow<List<MealEntryEntity>> = flowOf(emptyList())
      override fun getEntriesForDateAndMealType(date: String, mealType: String): Flow<List<MealEntryEntity>> = flowOf(emptyList())
      override suspend fun insert(entry: MealEntryEntity): Long = 1L
      override suspend fun update(entry: MealEntryEntity) {}
      override suspend fun delete(entry: MealEntryEntity) {}
      override suspend fun deleteForDate(date: String) {}
      override fun observeDailyTotals(date: String): Flow<DailyNutritionTotals> = flowOf(fakeTotals)
      override fun getEntriesWithFoodForDate(date: String): Flow<List<MealEntryWithFood>> = flowOf(fakeEntries)
    }

    val viewModel = MainScreenViewModel(
      mealEntryDao = fakeDao,
      dailyGoalDao = null,
      timeProvider = { fixedTime },
      dateProvider = { fixedDate },
    )

    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }
    advanceUntilIdle()

    val state = viewModel.uiState.value

    assertFalse(state.isEmptyDay)
    assertEquals(714, state.consumedCalories)
    assertEquals(1286, state.remainingCalories)
    assertEquals(70, state.protein.consumed)
    assertEquals(67, state.carbs.consumed)
    assertEquals(18, state.fat.consumed)

    // Check breakfast card
    val breakfast = state.meals.first { it.key == "breakfast" }
    assertEquals(467, breakfast.consumedCalories) // 389 + 78
    assertEquals(2, breakfast.items.size)
    assertEquals("Rolled Oats", breakfast.items[0].name)
    assertEquals(100.0, breakfast.items[0].quantityGrams)
    assertEquals(389, breakfast.items[0].calories)
    assertEquals("Boiled Egg", breakfast.items[1].name)
    assertEquals(50.0, breakfast.items[1].quantityGrams)
    assertEquals(78, breakfast.items[1].calories)

    // Check lunch card
    val lunch = state.meals.first { it.key == "lunch" }
    assertEquals(248, lunch.consumedCalories)
    assertEquals(1, lunch.items.size)
    assertEquals("Chicken Breast", lunch.items[0].name)
    assertEquals(150.0, lunch.items[0].quantityGrams)
    assertEquals(248, lunch.items[0].calories)

    // Check empty dinner and snack cards
    val dinner = state.meals.first { it.key == "dinner" }
    assertEquals(0, dinner.consumedCalories)
    assertTrue(dinner.items.isEmpty())

    val snack = state.meals.first { it.key == "snack" }
    assertEquals(0, snack.consumedCalories)
    assertTrue(snack.items.isEmpty())
  }

  @Test
  fun uiState_withCustomDailyGoal_usesTargetFromGoal() = runTest {
    val fixedDate = LocalDate.of(2026, 10, 2)
    val customGoal = DailyGoalEntity(
      date = "2026-10-02",
      calorieGoal = 2400.0,
      proteinGoal = 180.0,
      carbsGoal = 280.0,
      fatGoal = 80.0,
    )

    val fakeEntryDao = object : MealEntryDao {
      override fun getEntriesForDate(date: String): Flow<List<MealEntryEntity>> = flowOf(emptyList())
      override fun getEntriesForDateAndMealType(date: String, mealType: String): Flow<List<MealEntryEntity>> = flowOf(emptyList())
      override suspend fun insert(entry: MealEntryEntity): Long = 1L
      override suspend fun update(entry: MealEntryEntity) {}
      override suspend fun delete(entry: MealEntryEntity) {}
      override suspend fun deleteForDate(date: String) {}
      override fun observeDailyTotals(date: String): Flow<DailyNutritionTotals> =
        flowOf(DailyNutritionTotals(0.0, 0.0, 0.0, 0.0))
      override fun getEntriesWithFoodForDate(date: String): Flow<List<MealEntryWithFood>> = flowOf(emptyList())
    }

    val fakeGoalDao = object : DailyGoalDao {
      override suspend fun getForDate(date: String): DailyGoalEntity? = customGoal
      override fun observeForDate(date: String): Flow<DailyGoalEntity?> = flowOf(customGoal)
      override suspend fun insert(goal: DailyGoalEntity): Long = 1L
      override suspend fun update(goal: DailyGoalEntity) {}
      override suspend fun upsert(goal: DailyGoalEntity) {}
    }

    val viewModel = MainScreenViewModel(
      mealEntryDao = fakeEntryDao,
      dailyGoalDao = fakeGoalDao,
      dateProvider = { fixedDate },
    )

    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }
    advanceUntilIdle()

    val state = viewModel.uiState.value

    assertEquals(2400, state.targetCalories)
    assertEquals(2400, state.remainingCalories)
    assertEquals(180, state.protein.target)
    assertEquals(280, state.carbs.target)
    assertEquals(80, state.fat.target)
  }
}
