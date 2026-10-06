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
class HistoryViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private val fixedToday = LocalDate.of(2026, 10, 3)

  private class TestMealEntryDao : MealEntryDao {
    val dailySummaries = MutableStateFlow<List<DailySummary>>(emptyList())

    override fun observeAllDailyTotals(): Flow<List<DailySummary>> = dailySummaries
    override fun getDatesWithEntries(): Flow<List<String>> =
      dailySummaries.map { list -> list.map { it.date }.distinct().sortedDescending() }

    override fun getEntriesForDate(date: String): Flow<List<MealEntryEntity>> = flowOf(emptyList())
    override fun getEntriesForDateAndMealType(date: String, mealType: String): Flow<List<MealEntryEntity>> = flowOf(emptyList())
    override suspend fun insert(entry: MealEntryEntity): Long = 1L
    override suspend fun update(entry: MealEntryEntity) {}
    override suspend fun delete(entry: MealEntryEntity) {}
    override suspend fun deleteForDate(date: String) {}
    override fun observeDailyTotals(date: String): Flow<DailyNutritionTotals> = flowOf(DailyNutritionTotals(0.0, 0.0, 0.0, 0.0))
    override fun getEntriesWithFoodForDate(date: String): Flow<List<MealEntryWithFood>> = flowOf(emptyList())
    override suspend fun getEntryById(id: Long): MealEntryEntity? = null
    override suspend fun deleteById(id: Long) {}
    override fun getEntriesWithFoodForDateAndMealType(date: String, mealType: String): Flow<List<MealEntryWithFood>> = flowOf(emptyList())
    override suspend fun getAllMealEntries(): List<MealEntryEntity> = emptyList()
    override suspend fun deleteAllMealEntries(): Int = 0
    override suspend fun insertAll(entries: List<MealEntryEntity>) {}
  }

  private class TestDailyGoalDao : DailyGoalDao {
    val goals = MutableStateFlow<List<DailyGoalEntity>>(emptyList())

    override fun getAllGoals(): Flow<List<DailyGoalEntity>> = goals
    override fun getDatesWithGoals(): Flow<List<String>> =
      goals.map { list -> list.map { it.date }.distinct().sortedDescending() }

    override suspend fun getForDate(date: String): DailyGoalEntity? = goals.value.find { it.date == date }
    override fun observeForDate(date: String): Flow<DailyGoalEntity?> =
      goals.map { list -> list.find { it.date == date } }

    override suspend fun insert(goal: DailyGoalEntity): Long = 1L
    override suspend fun update(goal: DailyGoalEntity) {}
    override suspend fun upsert(goal: DailyGoalEntity) {}
    override suspend fun getAllDailyGoals(): List<DailyGoalEntity> = goals.value
    override suspend fun deleteAllDailyGoals(): Int = 0
    override suspend fun insertAll(newGoals: List<DailyGoalEntity>) {}
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
  fun history_emptyWhenNoEntriesAndNoGoals() = runTest {
    val mealDao = TestMealEntryDao()
    val goalDao = TestDailyGoalDao()
    val viewModel = HistoryViewModel(mealDao, goalDao, dateProvider = { fixedToday })

    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.isEmpty)
    assertEquals(0, viewModel.uiState.value.days.size)
  }

  @Test
  fun history_returnsDatesContainingMealEntries() = runTest {
    val mealDao = TestMealEntryDao()
    val goalDao = TestDailyGoalDao()

    mealDao.dailySummaries.value = listOf(
      DailySummary(
        date = "2026-10-01",
        totalCalories = 1750.0,
        totalProtein = 120.0,
        totalCarbs = 210.0,
        totalFat = 55.0,
      )
    )

    val viewModel = HistoryViewModel(mealDao, goalDao, dateProvider = { fixedToday })
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isEmpty)
    assertEquals(1, state.days.size)
    assertEquals("2026-10-01", state.days[0].dateIso)
    assertEquals(1750, state.days[0].totalCalories)
  }

  @Test
  fun history_returnsDatesContainingSavedGoals() = runTest {
    val mealDao = TestMealEntryDao()
    val goalDao = TestDailyGoalDao()

    goalDao.goals.value = listOf(
      DailyGoalEntity(
        date = "2026-09-30",
        calorieGoal = 2200.0,
        proteinGoal = 160.0,
        carbsGoal = 230.0,
        fatGoal = 70.0,
      )
    )

    val viewModel = HistoryViewModel(mealDao, goalDao, dateProvider = { fixedToday })
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(1, state.days.size)
    assertEquals("2026-09-30", state.days[0].dateIso)
    assertEquals(0, state.days[0].totalCalories) // No entries yet, so consumed is 0
    assertEquals(2200, state.days[0].calorieGoal)
  }

  @Test
  fun history_duplicateDatesAreRemoved() = runTest {
    val mealDao = TestMealEntryDao()
    val goalDao = TestDailyGoalDao()

    // Same date present in BOTH meal entries and goals
    mealDao.dailySummaries.value = listOf(
      DailySummary("2026-10-02", 1950.0, 140.0, 240.0, 60.0)
    )
    goalDao.goals.value = listOf(
      DailyGoalEntity("2026-10-02", 2000.0, 150.0, 250.0, 65.0)
    )

    val viewModel = HistoryViewModel(mealDao, goalDao, dateProvider = { fixedToday })
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(1, state.days.size)
    val day = state.days[0]
    assertEquals("2026-10-02", day.dateIso)
    assertEquals(1950, day.totalCalories)
    assertEquals(2000, day.calorieGoal)
  }

  @Test
  fun history_datesSortNewestFirst() = runTest {
    val mealDao = TestMealEntryDao()
    val goalDao = TestDailyGoalDao()

    mealDao.dailySummaries.value = listOf(
      DailySummary("2026-09-28", 1600.0, 100.0, 200.0, 50.0),
      DailySummary("2026-10-02", 2100.0, 150.0, 250.0, 70.0),
      DailySummary("2026-09-30", 1800.0, 120.0, 220.0, 60.0),
    )

    val viewModel = HistoryViewModel(mealDao, goalDao, dateProvider = { fixedToday })
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }
    advanceUntilIdle()

    val dates = viewModel.uiState.value.days.map { it.dateIso }
    assertEquals(listOf("2026-10-02", "2026-09-30", "2026-09-28"), dates)
  }

  @Test
  fun history_aggregatesCaloriesProteinCarbsAndFatCorrectly() = runTest {
    val mealDao = TestMealEntryDao()
    val goalDao = TestDailyGoalDao()

    mealDao.dailySummaries.value = listOf(
      DailySummary(
        date = "2026-10-01",
        totalCalories = 2145.8,
        totalProtein = 155.4,
        totalCarbs = 239.6,
        totalFat = 66.2,
      )
    )

    val viewModel = HistoryViewModel(mealDao, goalDao, dateProvider = { fixedToday })
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }
    advanceUntilIdle()

    val day = viewModel.uiState.value.days.first()
    assertEquals(2146, day.totalCalories) // 2145.8 rounded
    assertEquals(155, day.totalProtein)   // 155.4 rounded
    assertEquals(240, day.totalCarbs)     // 239.6 rounded
    assertEquals(66, day.totalFat)        // 66.2 rounded
  }

  @Test
  fun history_handlesMissingHistoricalGoalGracefully() = runTest {
    val mealDao = TestMealEntryDao()
    val goalDao = TestDailyGoalDao()

    mealDao.dailySummaries.value = listOf(
      DailySummary("2026-10-01", 1800.0, 130.0, 220.0, 60.0)
    )
    // No goal exists for 2026-10-01 in goalDao

    val viewModel = HistoryViewModel(mealDao, goalDao, dateProvider = { fixedToday })
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }
    advanceUntilIdle()

    val day = viewModel.uiState.value.days.first()
    assertEquals(1800, day.totalCalories)
    assertNull(day.calorieGoal)
    assertNull(day.proteinGoal)
    assertNull(day.carbsGoal)
    assertNull(day.fatGoal)
  }

  @Test
  fun history_formatsDatesWithFriendlyLabels() = runTest {
    val mealDao = TestMealEntryDao()
    val goalDao = TestDailyGoalDao()

    mealDao.dailySummaries.value = listOf(
      DailySummary("2026-10-03", 2000.0, 140.0, 250.0, 70.0), // Today
      DailySummary("2026-10-02", 1900.0, 130.0, 240.0, 65.0), // Yesterday
      DailySummary("2026-09-25", 1850.0, 125.0, 230.0, 60.0), // Older
    )

    val viewModel = HistoryViewModel(mealDao, goalDao, dateProvider = { fixedToday })
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }
    advanceUntilIdle()

    val days = viewModel.uiState.value.days
    assertEquals("Today", days[0].formattedDate)
    assertEquals("Yesterday", days[1].formattedDate)
    assertEquals("September 25, 2026", days[2].formattedDate)
  }

  @Test
  fun history_multipleHistoricalDatesHandledCorrectly() = runTest {
    val mealDao = TestMealEntryDao()
    val goalDao = TestDailyGoalDao()

    val summaries = (1..5).map { day ->
      val dateStr = "2026-09-%02d".format(day)
      DailySummary(dateStr, 1500.0 + day * 100, 100.0 + day * 10, 180.0 + day * 10, 50.0 + day * 2)
    }
    mealDao.dailySummaries.value = summaries

    val viewModel = HistoryViewModel(mealDao, goalDao, dateProvider = { fixedToday })
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }
    advanceUntilIdle()

    assertEquals(5, viewModel.uiState.value.days.size)
    assertEquals("2026-09-05", viewModel.uiState.value.days[0].dateIso)
    assertEquals("2026-09-01", viewModel.uiState.value.days[4].dateIso)
  }
}
