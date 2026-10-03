package com.calorietrack.app.ui.settings

import com.calorietrack.app.data.local.DailyGoalDao
import com.calorietrack.app.data.local.DailyGoalEntity
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private val fixedDate = LocalDate.of(2026, 10, 3)
  private val todayIso = "2026-10-03"

  private class FakeDailyGoalDao : DailyGoalDao {
    private val goals = mutableMapOf<String, DailyGoalEntity>()
    private val goalFlow = MutableStateFlow<Map<String, DailyGoalEntity>>(emptyMap())

    override suspend fun getForDate(date: String): DailyGoalEntity? = goals[date]

    override fun observeForDate(date: String): Flow<DailyGoalEntity?> =
      goalFlow.map { it[date] }

    override suspend fun insert(goal: DailyGoalEntity): Long {
      if (!goals.containsKey(goal.date)) {
        goals[goal.date] = goal
        goalFlow.value = goals.toMap()
        return 1L
      }
      return -1L
    }

    override suspend fun update(goal: DailyGoalEntity) {
      if (goals.containsKey(goal.date)) {
        goals[goal.date] = goal
        goalFlow.value = goals.toMap()
      }
    }

    override suspend fun upsert(goal: DailyGoalEntity) {
      goals[goal.date] = goal
      goalFlow.value = goals.toMap()
    }

    override fun getAllGoals(): Flow<List<DailyGoalEntity>> =
      goalFlow.map { it.values.toList() }

    override fun getDatesWithGoals(): Flow<List<String>> =
      goalFlow.map { it.keys.toList() }

    fun getAllGoalsList(): List<DailyGoalEntity> = goals.values.toList()
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
  fun uiState_initialState_usesDefaultsWhenNoRowExists() = runTest {
    val fakeDao = FakeDailyGoalDao()
    val viewModel = SettingsViewModel(fakeDao, dateProvider = { fixedDate })
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertEquals("2000", state.caloriesInput)
    assertEquals("140", state.proteinInput)
    assertEquals("250", state.carbsInput)
    assertEquals("70", state.fatInput)
    assertNull(state.caloriesError)
    assertNull(state.proteinError)
    assertNull(state.carbsError)
    assertNull(state.fatError)
    assertTrue(state.isValid)
  }

  @Test
  fun uiState_loadGoals_loadsExistingTodayGoals() = runTest {
    val fakeDao = FakeDailyGoalDao()
    fakeDao.upsert(
      DailyGoalEntity(
        date = todayIso,
        calorieGoal = 2200.0,
        proteinGoal = 160.0,
        carbsGoal = 220.0,
        fatGoal = 65.0,
      )
    )

    val viewModel = SettingsViewModel(fakeDao, dateProvider = { fixedDate })
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertEquals("2200", state.caloriesInput)
    assertEquals("160", state.proteinInput)
    assertEquals("220", state.carbsInput)
    assertEquals("65", state.fatInput)
    assertTrue(state.isValid)
  }

  @Test
  fun saveGoals_persistsNewGoalsForToday() = runTest {
    val fakeDao = FakeDailyGoalDao()
    val viewModel = SettingsViewModel(fakeDao, dateProvider = { fixedDate })
    advanceUntilIdle()

    viewModel.onCaloriesChanged("1850")
    viewModel.onProteinChanged("150")
    viewModel.onCarbsChanged("210")
    viewModel.onFatChanged("55")

    var onSavedCalled = false
    viewModel.saveGoals(onSaved = { onSavedCalled = true })
    advanceUntilIdle()

    assertTrue(onSavedCalled)
    val savedGoal = fakeDao.getForDate(todayIso)
    assertNotNull(savedGoal)
    assertEquals(1850.0, savedGoal!!.calorieGoal)
    assertEquals(150.0, savedGoal.proteinGoal)
    assertEquals(210.0, savedGoal.carbsGoal)
    assertEquals(55.0, savedGoal.fatGoal)
  }

  @Test
  fun saveGoals_updatesExistingTodayRow() = runTest {
    val fakeDao = FakeDailyGoalDao()
    fakeDao.upsert(
      DailyGoalEntity(
        date = todayIso,
        calorieGoal = 2000.0,
        proteinGoal = 140.0,
        carbsGoal = 250.0,
        fatGoal = 70.0,
      )
    )

    val viewModel = SettingsViewModel(fakeDao, dateProvider = { fixedDate })
    advanceUntilIdle()

    viewModel.onCaloriesChanged("2300")
    viewModel.saveGoals(onSaved = {})
    advanceUntilIdle()

    val updatedGoal = fakeDao.getForDate(todayIso)
    assertNotNull(updatedGoal)
    assertEquals(2300.0, updatedGoal!!.calorieGoal)
    assertEquals(140.0, updatedGoal.proteinGoal)
  }

  @Test
  fun saveGoals_doesNotCreateDuplicateRowsForToday() = runTest {
    val fakeDao = FakeDailyGoalDao()
    val viewModel = SettingsViewModel(fakeDao, dateProvider = { fixedDate })
    advanceUntilIdle()

    viewModel.onCaloriesChanged("1800")
    viewModel.saveGoals(onSaved = {})
    advanceUntilIdle()

    viewModel.onCaloriesChanged("1900")
    viewModel.saveGoals(onSaved = {})
    advanceUntilIdle()

    viewModel.onCaloriesChanged("2000")
    viewModel.saveGoals(onSaved = {})
    advanceUntilIdle()

    assertEquals(1, fakeDao.getAllGoalsList().size)
    assertEquals(todayIso, fakeDao.getAllGoalsList().first().date)
    assertEquals(2000.0, fakeDao.getAllGoalsList().first().calorieGoal)
  }

  @Test
  fun onCaloriesChanged_rejectsInvalidValues() = runTest {
    val fakeDao = FakeDailyGoalDao()
    val viewModel = SettingsViewModel(fakeDao, dateProvider = { fixedDate })
    advanceUntilIdle()

    // Non-numeric
    viewModel.onCaloriesChanged("abc")
    assertNotNull(viewModel.uiState.value.caloriesError)
    assertFalse(viewModel.uiState.value.isValid)

    // Zero
    viewModel.onCaloriesChanged("0")
    assertNotNull(viewModel.uiState.value.caloriesError)
    assertFalse(viewModel.uiState.value.isValid)

    // Negative
    viewModel.onCaloriesChanged("-200")
    assertNotNull(viewModel.uiState.value.caloriesError)
    assertFalse(viewModel.uiState.value.isValid)

    // Exceeding maximum
    viewModel.onCaloriesChanged("15000")
    assertNotNull(viewModel.uiState.value.caloriesError)
    assertFalse(viewModel.uiState.value.isValid)

    // Valid
    viewModel.onCaloriesChanged("2500")
    assertNull(viewModel.uiState.value.caloriesError)
    assertTrue(viewModel.uiState.value.isValid)
  }

  @Test
  fun onProteinChanged_rejectsInvalidValues() = runTest {
    val fakeDao = FakeDailyGoalDao()
    val viewModel = SettingsViewModel(fakeDao, dateProvider = { fixedDate })
    advanceUntilIdle()

    // Blank
    viewModel.onProteinChanged("")
    assertNotNull(viewModel.uiState.value.proteinError)
    assertFalse(viewModel.uiState.value.isValid)

    // Zero
    viewModel.onProteinChanged("0")
    assertNotNull(viewModel.uiState.value.proteinError)
    assertFalse(viewModel.uiState.value.isValid)

    // Negative
    viewModel.onProteinChanged("-10")
    assertNotNull(viewModel.uiState.value.proteinError)
    assertFalse(viewModel.uiState.value.isValid)

    // Exceeding limit
    viewModel.onProteinChanged("600")
    assertNotNull(viewModel.uiState.value.proteinError)
    assertFalse(viewModel.uiState.value.isValid)

    // Valid
    viewModel.onProteinChanged("180")
    assertNull(viewModel.uiState.value.proteinError)
    assertTrue(viewModel.uiState.value.isValid)
  }

  @Test
  fun onCarbsChanged_rejectsInvalidValues() = runTest {
    val fakeDao = FakeDailyGoalDao()
    val viewModel = SettingsViewModel(fakeDao, dateProvider = { fixedDate })
    advanceUntilIdle()

    // Non-numeric
    viewModel.onCarbsChanged("xyz")
    assertNotNull(viewModel.uiState.value.carbsError)
    assertFalse(viewModel.uiState.value.isValid)

    // Zero
    viewModel.onCarbsChanged("0")
    assertNotNull(viewModel.uiState.value.carbsError)
    assertFalse(viewModel.uiState.value.isValid)

    // Exceeding limit
    viewModel.onCarbsChanged("1500")
    assertNotNull(viewModel.uiState.value.carbsError)
    assertFalse(viewModel.uiState.value.isValid)

    // Valid
    viewModel.onCarbsChanged("275")
    assertNull(viewModel.uiState.value.carbsError)
    assertTrue(viewModel.uiState.value.isValid)
  }

  @Test
  fun onFatChanged_rejectsInvalidValues() = runTest {
    val fakeDao = FakeDailyGoalDao()
    val viewModel = SettingsViewModel(fakeDao, dateProvider = { fixedDate })
    advanceUntilIdle()

    // Non-numeric
    viewModel.onFatChanged("bad")
    assertNotNull(viewModel.uiState.value.fatError)
    assertFalse(viewModel.uiState.value.isValid)

    // Zero
    viewModel.onFatChanged("0")
    assertNotNull(viewModel.uiState.value.fatError)
    assertFalse(viewModel.uiState.value.isValid)

    // Exceeding limit
    viewModel.onFatChanged("700")
    assertNotNull(viewModel.uiState.value.fatError)
    assertFalse(viewModel.uiState.value.isValid)

    // Valid
    viewModel.onFatChanged("65")
    assertNull(viewModel.uiState.value.fatError)
    assertTrue(viewModel.uiState.value.isValid)
  }

  @Test
  fun onResetToDefaults_restoresApplicationDefaultValuesWithoutSavingImmediately() = runTest {
    val fakeDao = FakeDailyGoalDao()
    val viewModel = SettingsViewModel(fakeDao, dateProvider = { fixedDate })
    advanceUntilIdle()

    // Modify all inputs
    viewModel.onCaloriesChanged("1500")
    viewModel.onProteinChanged("100")
    viewModel.onCarbsChanged("150")
    viewModel.onFatChanged("40")

    assertEquals("1500", viewModel.uiState.value.caloriesInput)

    // Reset to defaults
    viewModel.onResetToDefaults()

    val state = viewModel.uiState.value
    assertEquals("2000", state.caloriesInput)
    assertEquals("140", state.proteinInput)
    assertEquals("250", state.carbsInput)
    assertEquals("70", state.fatInput)
    assertNull(state.caloriesError)
    assertNull(state.proteinError)
    assertNull(state.carbsError)
    assertNull(state.fatError)
    assertTrue(state.isValid)

    // Verify DB was NOT modified immediately
    assertNull(fakeDao.getForDate(todayIso))
  }
}
