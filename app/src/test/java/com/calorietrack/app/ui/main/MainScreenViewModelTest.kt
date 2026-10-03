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

  private open class FakeMealEntryDao : MealEntryDao {
    override fun getEntriesForDate(date: String): Flow<List<MealEntryEntity>> = flowOf(emptyList())
    override fun getEntriesForDateAndMealType(date: String, mealType: String): Flow<List<MealEntryEntity>> = flowOf(emptyList())
    override suspend fun insert(entry: MealEntryEntity): Long = 1L
    override suspend fun update(entry: MealEntryEntity) {}
    override suspend fun delete(entry: MealEntryEntity) {}
    override suspend fun deleteForDate(date: String) {}
    override fun observeDailyTotals(date: String): Flow<DailyNutritionTotals> =
      flowOf(DailyNutritionTotals(0.0, 0.0, 0.0, 0.0))
    override fun getEntriesWithFoodForDate(date: String): Flow<List<MealEntryWithFood>> = flowOf(emptyList())
    override suspend fun getEntryById(id: Long): MealEntryEntity? = null
    override suspend fun deleteById(id: Long) {}
    override fun getEntriesWithFoodForDateAndMealType(date: String, mealType: String): Flow<List<MealEntryWithFood>> =
      flowOf(emptyList())
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

    val fakeDao = object : FakeMealEntryDao() {
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

    val fakeEntryDao = FakeMealEntryDao()

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

  @Test
  fun uiState_updatesDynamically_whenMealEntryIsEdited() = runTest {
    val fixedDate = LocalDate.of(2026, 10, 2)
    val todayIso = "2026-10-02"

    val initialEntry = MealEntryWithFood(
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
    )
    val initialTotals = DailyNutritionTotals(389.0, 16.9, 66.3, 6.9)

    val totalsFlow = kotlinx.coroutines.flow.MutableStateFlow(initialTotals)
    val entriesFlow = kotlinx.coroutines.flow.MutableStateFlow(listOf(initialEntry))

    val fakeDao = object : FakeMealEntryDao() {
      override fun observeDailyTotals(date: String): Flow<DailyNutritionTotals> = totalsFlow
      override fun getEntriesWithFoodForDate(date: String): Flow<List<MealEntryWithFood>> = entriesFlow
    }

    val viewModel = MainScreenViewModel(
      mealEntryDao = fakeDao,
      dateProvider = { fixedDate },
    )

    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }
    advanceUntilIdle()

    assertEquals(389, viewModel.uiState.value.consumedCalories)
    val initialBreakfast = viewModel.uiState.value.meals.first { it.key == "breakfast" }
    assertEquals(389, initialBreakfast.consumedCalories)
    assertEquals(100.0, initialBreakfast.items[0].quantityGrams)

    // Simulate editing quantity from 100g to 200g
    val editedEntry = initialEntry.copy(
      quantityGrams = 200.0,
      calories = 778.0,
      protein = 33.8,
      carbs = 132.6,
      fat = 13.8,
    )
    totalsFlow.value = DailyNutritionTotals(778.0, 33.8, 132.6, 13.8)
    entriesFlow.value = listOf(editedEntry)
    advanceUntilIdle()

    val updatedState = viewModel.uiState.value
    assertEquals(778, updatedState.consumedCalories)
    val updatedBreakfast = updatedState.meals.first { it.key == "breakfast" }
    assertEquals(778, updatedBreakfast.consumedCalories)
    assertEquals(200.0, updatedBreakfast.items[0].quantityGrams)
    assertEquals(34, updatedState.protein.consumed)
    assertEquals(133, updatedState.carbs.consumed)
    assertEquals(14, updatedState.fat.consumed)
  }

  @Test
  fun uiState_updatesDynamically_whenMealEntryIsDeleted_andReturnsToZeroWhenAllDeleted() = runTest {
    val fixedDate = LocalDate.of(2026, 10, 2)
    val todayIso = "2026-10-02"

    val entry1 = MealEntryWithFood(
      id = 1L,
      date = todayIso,
      mealType = "lunch",
      foodId = 10L,
      quantityGrams = 100.0,
      calories = 200.0,
      protein = 20.0,
      carbs = 10.0,
      fat = 5.0,
      foodName = "Grilled Chicken",
    )
    val entry2 = MealEntryWithFood(
      id = 2L,
      date = todayIso,
      mealType = "lunch",
      foodId = 20L,
      quantityGrams = 50.0,
      calories = 100.0,
      protein = 2.0,
      carbs = 20.0,
      fat = 1.0,
      foodName = "Rice",
    )

    val totalsFlow = kotlinx.coroutines.flow.MutableStateFlow(DailyNutritionTotals(300.0, 22.0, 30.0, 6.0))
    val entriesFlow = kotlinx.coroutines.flow.MutableStateFlow(listOf(entry1, entry2))

    val fakeDao = object : FakeMealEntryDao() {
      override fun observeDailyTotals(date: String): Flow<DailyNutritionTotals> = totalsFlow
      override fun getEntriesWithFoodForDate(date: String): Flow<List<MealEntryWithFood>> = entriesFlow
    }

    val viewModel = MainScreenViewModel(
      mealEntryDao = fakeDao,
      dateProvider = { fixedDate },
    )

    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }
    advanceUntilIdle()

    assertEquals(300, viewModel.uiState.value.consumedCalories)
    val lunch = viewModel.uiState.value.meals.first { it.key == "lunch" }
    assertEquals(2, lunch.items.size)
    assertEquals(300, lunch.consumedCalories)
    assertFalse(viewModel.uiState.value.isEmptyDay)

    // Delete one entry
    totalsFlow.value = DailyNutritionTotals(200.0, 20.0, 10.0, 5.0)
    entriesFlow.value = listOf(entry1)
    advanceUntilIdle()

    assertEquals(200, viewModel.uiState.value.consumedCalories)
    val lunchAfterOneDelete = viewModel.uiState.value.meals.first { it.key == "lunch" }
    assertEquals(1, lunchAfterOneDelete.items.size)
    assertEquals(200, lunchAfterOneDelete.consumedCalories)

    // Delete remaining entry (final food deleted)
    totalsFlow.value = DailyNutritionTotals(0.0, 0.0, 0.0, 0.0)
    entriesFlow.value = emptyList()
    advanceUntilIdle()

    val emptyState = viewModel.uiState.value
    assertEquals(0, emptyState.consumedCalories)
    val lunchAfterFinalDelete = emptyState.meals.first { it.key == "lunch" }
    assertEquals(0, lunchAfterFinalDelete.items.size)
    assertEquals(0, lunchAfterFinalDelete.consumedCalories)
    assertTrue(emptyState.isEmptyDay)
  }

  @Test
  fun uiState_reactsToGoalChanges_recalculatingRemainingAndMacros_whilePreservingLoggedFoods() = runTest {
    val fixedDate = LocalDate.of(2026, 10, 3)
    val todayIso = "2026-10-03"

    val loggedEntry = MealEntryWithFood(
      id = 1L,
      date = todayIso,
      mealType = "breakfast",
      foodId = 10L,
      quantityGrams = 100.0,
      calories = 500.0,
      protein = 30.0,
      carbs = 60.0,
      fat = 15.0,
      foodName = "Healthy Breakfast Bowl",
    )
    val totals = DailyNutritionTotals(500.0, 30.0, 60.0, 15.0)

    val initialGoal = DailyGoalEntity(
      date = todayIso,
      calorieGoal = 2000.0,
      proteinGoal = 140.0,
      carbsGoal = 250.0,
      fatGoal = 70.0,
    )
    val goalFlow = kotlinx.coroutines.flow.MutableStateFlow<DailyGoalEntity?>(initialGoal)

    val fakeEntryDao = object : FakeMealEntryDao() {
      override fun observeDailyTotals(date: String): Flow<DailyNutritionTotals> = flowOf(totals)
      override fun getEntriesWithFoodForDate(date: String): Flow<List<MealEntryWithFood>> = flowOf(listOf(loggedEntry))
    }

    val fakeGoalDao = object : DailyGoalDao {
      override suspend fun getForDate(date: String): DailyGoalEntity? = goalFlow.value
      override fun observeForDate(date: String): Flow<DailyGoalEntity?> = goalFlow
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

    val initialState = viewModel.uiState.value
    assertEquals(500, initialState.consumedCalories)
    assertEquals(2000, initialState.targetCalories)
    assertEquals(1500, initialState.remainingCalories)
    assertEquals(0.25f, initialState.calorieProgress, 0.001f)
    assertEquals(140, initialState.protein.target)
    assertEquals(30, initialState.protein.consumed)
    assertEquals(250, initialState.carbs.target)
    assertEquals(60, initialState.carbs.consumed)
    assertEquals(70, initialState.fat.target)
    assertEquals(15, initialState.fat.consumed)

    // User edits goals in Settings: Calories from 2000 to 1800, Protein to 160, Carbs to 200, Fat to 50
    val updatedGoal = DailyGoalEntity(
      date = todayIso,
      calorieGoal = 1800.0,
      proteinGoal = 160.0,
      carbsGoal = 200.0,
      fatGoal = 50.0,
    )
    goalFlow.value = updatedGoal
    advanceUntilIdle()

    val updatedState = viewModel.uiState.value
    // Consumed calories and logged foods must remain UNCHANGED
    assertEquals(500, updatedState.consumedCalories)
    val breakfast = updatedState.meals.first { it.key == "breakfast" }
    assertEquals(1, breakfast.items.size)
    assertEquals("Healthy Breakfast Bowl", breakfast.items[0].name)
    assertEquals(500, breakfast.consumedCalories)

    // Target, remaining, and progress MUST recalculate
    assertEquals(1800, updatedState.targetCalories)
    assertEquals(1300, updatedState.remainingCalories) // 1800 - 500
    assertEquals(500f / 1800f, updatedState.calorieProgress, 0.001f)

    // Macro targets MUST update while consumed amounts remain unchanged
    assertEquals(160, updatedState.protein.target)
    assertEquals(30, updatedState.protein.consumed)
    assertEquals(200, updatedState.carbs.target)
    assertEquals(60, updatedState.carbs.consumed)
    assertEquals(50, updatedState.fat.target)
    assertEquals(15, updatedState.fat.consumed)
  }
}
