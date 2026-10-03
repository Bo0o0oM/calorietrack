package com.calorietrack.app.ui.meal

import com.calorietrack.app.data.local.DailyNutritionTotals
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
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
class MealDetailsViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private val fixedDate = LocalDate.of(2026, 10, 3)

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun initialState_emptyMeal_showsEmptyStateAndZeroTotals() = runTest {
    val fakeDao = FakeMealDetailsDao()
    val viewModel = MealDetailsViewModel(
      mealType = "breakfast",
      date = "2026-10-03",
      mealEntryDao = fakeDao,
      dateProvider = { fixedDate },
    )

    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertTrue(state.isEmpty)
    assertEquals("Breakfast", state.displayName)
    assertEquals("Today's entries", state.subtitle)
    assertEquals(0, state.totalCalories)
    assertEquals(0.0, state.totalProtein)
    assertEquals(0.0, state.totalCarbs)
    assertEquals(0.0, state.totalFat)
    assertTrue(state.entries.isEmpty())
  }

  @Test
  fun initialState_loadsEntriesForMealAndDate_calculatesMealTotals() = runTest {
    val fakeDao = FakeMealDetailsDao()
    fakeDao.addEntry(
      MealEntryWithFood(
        id = 1L,
        date = "2026-10-03",
        mealType = "breakfast",
        foodId = 10L,
        quantityGrams = 100.0,
        calories = 389.0,
        protein = 16.9,
        carbs = 66.3,
        fat = 6.9,
        foodName = "Rolled Oats",
      )
    )

    val viewModel = MealDetailsViewModel(
      mealType = "breakfast",
      date = "2026-10-03",
      mealEntryDao = fakeDao,
      dateProvider = { fixedDate },
    )

    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertFalse(state.isEmpty)
    assertEquals(1, state.entries.size)
    assertEquals(389, state.totalCalories)
    assertEquals(16.9, state.totalProtein, 0.001)
    assertEquals(66.3, state.totalCarbs, 0.001)
    assertEquals(6.9, state.totalFat, 0.001)
    assertEquals("Rolled Oats", state.entries[0].foodName)
  }

  @Test
  fun multipleEntries_calculatesTotalsAccuratelyFromStoredValues() = runTest {
    val fakeDao = FakeMealDetailsDao()
    fakeDao.addEntry(
      MealEntryWithFood(
        id = 1L,
        date = "2026-10-03",
        mealType = "lunch",
        foodId = 10L,
        quantityGrams = 100.0,
        calories = 165.0,
        protein = 31.0,
        carbs = 0.0,
        fat = 3.6,
        foodName = "Chicken Breast",
      )
    )
    fakeDao.addEntry(
      MealEntryWithFood(
        id = 2L,
        date = "2026-10-03",
        mealType = "lunch",
        foodId = 20L,
        quantityGrams = 150.0,
        calories = 195.0,
        protein = 4.0,
        carbs = 42.0,
        fat = 0.5,
        foodName = "White Rice",
      )
    )

    val viewModel = MealDetailsViewModel(
      mealType = "lunch",
      date = "2026-10-03",
      mealEntryDao = fakeDao,
      dateProvider = { fixedDate },
    )

    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(2, state.entries.size)
    assertEquals(360, state.totalCalories)
    assertEquals(35.0, state.totalProtein, 0.001)
    assertEquals(42.0, state.totalCarbs, 0.001)
    assertEquals(4.1, state.totalFat, 0.001)
  }

  @Test
  fun mealTypeFiltering_onlyLoadsEntriesForSpecifiedMeal() = runTest {
    val fakeDao = FakeMealDetailsDao()
    fakeDao.addEntry(
      MealEntryWithFood(
        id = 1L,
        date = "2026-10-03",
        mealType = "breakfast",
        foodId = 10L,
        quantityGrams = 50.0,
        calories = 77.5,
        protein = 6.3,
        carbs = 0.5,
        fat = 5.3,
        foodName = "Boiled Egg",
      )
    )
    fakeDao.addEntry(
      MealEntryWithFood(
        id = 2L,
        date = "2026-10-03",
        mealType = "dinner",
        foodId = 20L,
        quantityGrams = 200.0,
        calories = 300.0,
        protein = 20.0,
        carbs = 10.0,
        fat = 15.0,
        foodName = "Paneer Curry",
      )
    )

    val viewModel = MealDetailsViewModel(
      mealType = "breakfast",
      date = "2026-10-03",
      mealEntryDao = fakeDao,
      dateProvider = { fixedDate },
    )

    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(1, state.entries.size)
    assertEquals("Boiled Egg", state.entries[0].foodName)
    assertEquals(78, state.totalCalories)
  }

  @Test
  fun requestDelete_setsPendingDeleteEntry() = runTest {
    val fakeDao = FakeMealDetailsDao()
    val entry = MealEntryWithFood(
      id = 1L,
      date = "2026-10-03",
      mealType = "breakfast",
      foodId = 10L,
      quantityGrams = 50.0,
      calories = 77.5,
      protein = 6.3,
      carbs = 0.5,
      fat = 5.3,
      foodName = "Boiled Egg",
    )
    fakeDao.addEntry(entry)

    val viewModel = MealDetailsViewModel(
      mealType = "breakfast",
      date = "2026-10-03",
      mealEntryDao = fakeDao,
      dateProvider = { fixedDate },
    )

    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }
    advanceUntilIdle()

    viewModel.onRequestDelete(entry)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNotNull(state.entryPendingDelete)
    assertEquals(1L, state.entryPendingDelete?.id)
  }

  @Test
  fun dismissDelete_clearsPendingDeleteEntry() = runTest {
    val fakeDao = FakeMealDetailsDao()
    val entry = MealEntryWithFood(
      id = 1L,
      date = "2026-10-03",
      mealType = "breakfast",
      foodId = 10L,
      quantityGrams = 50.0,
      calories = 77.5,
      protein = 6.3,
      carbs = 0.5,
      fat = 5.3,
      foodName = "Boiled Egg",
    )
    fakeDao.addEntry(entry)

    val viewModel = MealDetailsViewModel(
      mealType = "breakfast",
      date = "2026-10-03",
      mealEntryDao = fakeDao,
      dateProvider = { fixedDate },
    )

    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }
    advanceUntilIdle()

    viewModel.onRequestDelete(entry)
    advanceUntilIdle()
    assertNotNull(viewModel.uiState.value.entryPendingDelete)

    viewModel.onDismissDelete()
    advanceUntilIdle()
    assertNull(viewModel.uiState.value.entryPendingDelete)
  }

  @Test
  fun confirmDelete_deletesEntryById_updatesMealTotals() = runTest {
    val fakeDao = FakeMealDetailsDao()
    val entry1 = MealEntryWithFood(
      id = 1L,
      date = "2026-10-03",
      mealType = "breakfast",
      foodId = 10L,
      quantityGrams = 100.0,
      calories = 389.0,
      protein = 16.9,
      carbs = 66.3,
      fat = 6.9,
      foodName = "Rolled Oats",
    )
    val entry2 = MealEntryWithFood(
      id = 2L,
      date = "2026-10-03",
      mealType = "breakfast",
      foodId = 20L,
      quantityGrams = 50.0,
      calories = 77.5,
      protein = 6.3,
      carbs = 0.5,
      fat = 5.3,
      foodName = "Boiled Egg",
    )
    fakeDao.addEntry(entry1)
    fakeDao.addEntry(entry2)

    val viewModel = MealDetailsViewModel(
      mealType = "breakfast",
      date = "2026-10-03",
      mealEntryDao = fakeDao,
      dateProvider = { fixedDate },
    )

    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }
    advanceUntilIdle()

    assertEquals(2, viewModel.uiState.value.entries.size)
    assertEquals(467, viewModel.uiState.value.totalCalories)

    // Delete entry2 (Boiled Egg)
    viewModel.onRequestDelete(entry2)
    viewModel.onConfirmDelete()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNull(state.entryPendingDelete)
    assertEquals(1, state.entries.size)
    assertEquals("Rolled Oats", state.entries[0].foodName)
    assertEquals(389, state.totalCalories)
  }

  @Test
  fun deleteFinalEntry_triggersEmptyState() = runTest {
    val fakeDao = FakeMealDetailsDao()
    val entry = MealEntryWithFood(
      id = 1L,
      date = "2026-10-03",
      mealType = "breakfast",
      foodId = 10L,
      quantityGrams = 100.0,
      calories = 389.0,
      protein = 16.9,
      carbs = 66.3,
      fat = 6.9,
      foodName = "Rolled Oats",
    )
    fakeDao.addEntry(entry)

    val viewModel = MealDetailsViewModel(
      mealType = "breakfast",
      date = "2026-10-03",
      mealEntryDao = fakeDao,
      dateProvider = { fixedDate },
    )

    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isEmpty)

    viewModel.onRequestDelete(entry)
    viewModel.onConfirmDelete()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.isEmpty)
    assertEquals(0, state.entries.size)
    assertEquals(0, state.totalCalories)
  }
}

class FakeMealDetailsDao : MealEntryDao {
  private val allEntries = MutableStateFlow<List<MealEntryWithFood>>(emptyList())

  fun addEntry(entry: MealEntryWithFood) {
    allEntries.value = allEntries.value + entry
  }

  override fun getEntriesWithFoodForDateAndMealType(
    date: String,
    mealType: String,
  ): Flow<List<MealEntryWithFood>> {
    val flow = MutableStateFlow<List<MealEntryWithFood>>(emptyList())
    // Keep in sync with allEntries
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined).launch {
      allEntries.collect { entries ->
        val filtered = entries.filter {
          it.date == date && (
            it.mealType == mealType ||
              (mealType == "snack" && it.mealType == "snacks") ||
              (mealType == "snacks" && it.mealType == "snack")
            )
        }
        flow.value = filtered
      }
    }
    return flow.asStateFlow()
  }

  override suspend fun deleteById(id: Long) {
    allEntries.value = allEntries.value.filterNot { it.id == id }
  }

  override fun getEntriesForDate(date: String): Flow<List<MealEntryEntity>> =
    throw UnsupportedOperationException()

  override fun getEntriesForDateAndMealType(date: String, mealType: String): Flow<List<MealEntryEntity>> =
    throw UnsupportedOperationException()

  override suspend fun insert(entry: MealEntryEntity): Long = 1L
  override suspend fun update(entry: MealEntryEntity) {}
  override suspend fun delete(entry: MealEntryEntity) {}
  override suspend fun deleteForDate(date: String) {}
  override fun observeDailyTotals(date: String): Flow<DailyNutritionTotals> =
    throw UnsupportedOperationException()
  override fun getEntriesWithFoodForDate(date: String): Flow<List<MealEntryWithFood>> =
    throw UnsupportedOperationException()
  override suspend fun getEntryById(id: Long): MealEntryEntity? = null
  override fun observeAllDailyTotals(): Flow<List<com.calorietrack.app.data.local.DailySummary>> =
    throw UnsupportedOperationException()
  override fun getDatesWithEntries(): Flow<List<String>> =
    throw UnsupportedOperationException()
}
