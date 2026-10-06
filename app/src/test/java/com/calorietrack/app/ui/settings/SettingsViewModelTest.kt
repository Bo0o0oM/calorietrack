package com.calorietrack.app.ui.settings

import com.calorietrack.app.data.backup.BackupRepository
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

    override suspend fun getAllDailyGoals(): List<DailyGoalEntity> = goals.values.toList()

    override suspend fun deleteAllDailyGoals(): Int {
      val count = goals.size
      goals.clear()
      goalFlow.value = emptyMap()
      return count
    }

    override suspend fun insertAll(goalsList: List<DailyGoalEntity>) {
      goalsList.forEach { goals[it.date] = it }
      goalFlow.value = goals.toMap()
    }

    fun getAllGoalsList(): List<DailyGoalEntity> = goals.values.toList()
  }

  private fun createFakeBackupRepository(dailyGoalDao: FakeDailyGoalDao): BackupRepository {
    val foods = mutableMapOf<Long, com.calorietrack.app.data.local.FoodEntity>()
    val recipes = mutableMapOf<Long, com.calorietrack.app.data.local.RecipeEntity>()
    val ingredients = mutableMapOf<Long, com.calorietrack.app.data.local.RecipeIngredientEntity>()
    val mealEntries = mutableMapOf<Long, com.calorietrack.app.data.local.MealEntryEntity>()

    val fakeFoodDao = object : com.calorietrack.app.data.local.FoodDao {
      override suspend fun getById(id: Long): com.calorietrack.app.data.local.FoodEntity? = foods[id]
      override fun getAll(): Flow<List<com.calorietrack.app.data.local.FoodEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
      override fun searchByName(query: String): Flow<List<com.calorietrack.app.data.local.FoodEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
      override fun getMyFoods(): Flow<List<com.calorietrack.app.data.local.FoodEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
      override fun searchMyFoods(query: String): Flow<List<com.calorietrack.app.data.local.FoodEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
      override suspend fun insert(food: com.calorietrack.app.data.local.FoodEntity): Long { foods[food.id] = food; return food.id }
      override suspend fun insertAll(foodsList: List<com.calorietrack.app.data.local.FoodEntity>) { foodsList.forEach { foods[it.id] = it } }
      override suspend fun update(food: com.calorietrack.app.data.local.FoodEntity) { foods[food.id] = food }
      override suspend fun delete(food: com.calorietrack.app.data.local.FoodEntity) { foods.remove(food.id) }
      override suspend fun archiveFood(id: Long): Int = 0
      override suspend fun getMaxId(): Long? = foods.keys.maxOrNull()
      override suspend fun count(): Int = foods.size
      override suspend fun countActive(): Int = foods.values.count { it.isActive }
      override suspend fun countActiveCustom(): Int = foods.values.count { it.isCustom && it.isActive }
      override suspend fun getAllCustomFoods(): List<com.calorietrack.app.data.local.FoodEntity> = foods.values.filter { it.isCustom }
      override suspend fun deleteAllCustomFoods(): Int {
        val count = foods.values.count { it.isCustom }
        foods.entries.removeAll { it.value.isCustom }
        return count
      }
    }

    val fakeRecipeDao = object : com.calorietrack.app.data.local.RecipeDao {
      override suspend fun getRecipeById(id: Long): com.calorietrack.app.data.local.RecipeEntity? = recipes[id]
      override fun getAllActiveRecipes(): Flow<List<com.calorietrack.app.data.local.RecipeEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
      override fun searchActiveRecipes(query: String): Flow<List<com.calorietrack.app.data.local.RecipeEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
      override fun getIngredientsWithFood(recipeId: Long): Flow<List<com.calorietrack.app.data.local.RecipeIngredientWithFood>> = kotlinx.coroutines.flow.flowOf(emptyList())
      override suspend fun getIngredientsForRecipe(recipeId: Long): List<com.calorietrack.app.data.local.RecipeIngredientEntity> =
        ingredients.values.filter { it.recipeId == recipeId }
      override suspend fun insertRecipe(recipe: com.calorietrack.app.data.local.RecipeEntity): Long { recipes[recipe.id] = recipe; return recipe.id }
      override suspend fun updateRecipe(recipe: com.calorietrack.app.data.local.RecipeEntity) { recipes[recipe.id] = recipe }
      override suspend fun insertIngredients(ingredientsList: List<com.calorietrack.app.data.local.RecipeIngredientEntity>) {
        ingredientsList.forEach { ingredients[it.id] = it }
      }
      override suspend fun deleteIngredientsForRecipe(recipeId: Long) {
        ingredients.entries.removeAll { it.value.recipeId == recipeId }
      }
      override suspend fun archiveRecipe(recipeId: Long): Int = 0
      override suspend fun countActiveRecipes(): Int = recipes.values.count { it.isActive }
      override suspend fun saveRecipeWithIngredients(recipe: com.calorietrack.app.data.local.RecipeEntity, ingredientsList: List<com.calorietrack.app.data.local.RecipeIngredientEntity>): Long = recipe.id
      override suspend fun getAllRecipes(): List<com.calorietrack.app.data.local.RecipeEntity> = recipes.values.toList()
      override suspend fun getAllRecipeIngredients(): List<com.calorietrack.app.data.local.RecipeIngredientEntity> = ingredients.values.toList()
      override suspend fun deleteAllRecipeIngredients(): Int {
        val size = ingredients.size
        ingredients.clear()
        return size
      }
      override suspend fun deleteAllRecipes(): Int {
        val size = recipes.size
        recipes.clear()
        return size
      }
      override suspend fun insertRecipes(recipesList: List<com.calorietrack.app.data.local.RecipeEntity>) {
        recipesList.forEach { recipes[it.id] = it }
      }
    }

    val fakeMealEntryDao = object : com.calorietrack.app.data.local.MealEntryDao {
      override fun getEntriesForDate(date: String): Flow<List<com.calorietrack.app.data.local.MealEntryEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
      override fun getEntriesForDateAndMealType(date: String, mealType: String): Flow<List<com.calorietrack.app.data.local.MealEntryEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
      override suspend fun insert(entry: com.calorietrack.app.data.local.MealEntryEntity): Long { mealEntries[entry.id] = entry; return entry.id }
      override suspend fun update(entry: com.calorietrack.app.data.local.MealEntryEntity) { mealEntries[entry.id] = entry }
      override suspend fun delete(entry: com.calorietrack.app.data.local.MealEntryEntity) { mealEntries.remove(entry.id) }
      override suspend fun deleteForDate(date: String) {}
      override fun observeDailyTotals(date: String): Flow<com.calorietrack.app.data.local.DailyNutritionTotals> =
        kotlinx.coroutines.flow.flowOf(com.calorietrack.app.data.local.DailyNutritionTotals(0.0, 0.0, 0.0, 0.0))
      override fun getEntriesWithFoodForDate(date: String): Flow<List<com.calorietrack.app.data.local.MealEntryWithFood>> = kotlinx.coroutines.flow.flowOf(emptyList())
      override suspend fun getEntryById(id: Long): com.calorietrack.app.data.local.MealEntryEntity? = mealEntries[id]
      override suspend fun deleteById(id: Long) { mealEntries.remove(id) }
      override fun getEntriesWithFoodForDateAndMealType(date: String, mealType: String): Flow<List<com.calorietrack.app.data.local.MealEntryWithFood>> = kotlinx.coroutines.flow.flowOf(emptyList())
      override fun observeAllDailyTotals(): Flow<List<com.calorietrack.app.data.local.DailySummary>> = kotlinx.coroutines.flow.flowOf(emptyList())
      override fun getDatesWithEntries(): Flow<List<String>> = kotlinx.coroutines.flow.flowOf(emptyList())
      override suspend fun getAllMealEntries(): List<com.calorietrack.app.data.local.MealEntryEntity> = mealEntries.values.toList()
      override suspend fun deleteAllMealEntries(): Int {
        val size = mealEntries.size
        mealEntries.clear()
        return size
      }
      override suspend fun insertAll(entries: List<com.calorietrack.app.data.local.MealEntryEntity>) {
        entries.forEach { mealEntries[it.id] = it }
      }
    }

    return BackupRepository(
      foodDao = fakeFoodDao,
      recipeDao = fakeRecipeDao,
      mealEntryDao = fakeMealEntryDao,
      dailyGoalDao = dailyGoalDao,
      transactionRunner = { block -> block() },
    )
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

  @Test
  fun exportBackupJson_success_invokesOnReadyWithExportedJson() = runTest {
    val fakeDao = FakeDailyGoalDao()
    val backupRepo = createFakeBackupRepository(fakeDao)
    val viewModel = SettingsViewModel(fakeDao, backupRepository = backupRepo, dateProvider = { fixedDate })
    advanceUntilIdle()

    var exportedContent: String? = null
    viewModel.exportBackupJson { json ->
      exportedContent = json
    }
    advanceUntilIdle()

    assertNotNull(exportedContent)
    assertTrue(exportedContent!!.contains("\"backupFormatVersion\": 1"))
    assertFalse(viewModel.uiState.value.isExporting)
  }

  @Test
  fun exportBackupJson_whenRepositoryNull_setsBackupError() = runTest {
    val fakeDao = FakeDailyGoalDao()
    val viewModel = SettingsViewModel(fakeDao, backupRepository = null, dateProvider = { fixedDate })
    advanceUntilIdle()

    var called = false
    viewModel.exportBackupJson { called = true }
    advanceUntilIdle()

    assertFalse(called)
    assertNotNull(viewModel.uiState.value.backupError)
  }

  @Test
  fun onExportCompleted_updatesBackupMessage() = runTest {
    val fakeDao = FakeDailyGoalDao()
    val viewModel = SettingsViewModel(fakeDao, dateProvider = { fixedDate })

    viewModel.onExportCompleted()

    assertEquals("Backup successfully exported.", viewModel.uiState.value.backupMessage)
    assertNull(viewModel.uiState.value.backupError)
  }

  @Test
  fun onExportFailed_updatesBackupError() = runTest {
    val fakeDao = FakeDailyGoalDao()
    val viewModel = SettingsViewModel(fakeDao, dateProvider = { fixedDate })

    viewModel.onExportFailed("Could not write file")

    assertEquals("Could not write file", viewModel.uiState.value.backupError)
    assertNull(viewModel.uiState.value.backupMessage)
  }

  @Test
  fun onBackupFileLoaded_validJson_showsConfirmationDialogAndPendingBackup() = runTest {
    val fakeDao = FakeDailyGoalDao()
    val backupRepo = createFakeBackupRepository(fakeDao)
    val viewModel = SettingsViewModel(fakeDao, backupRepository = backupRepo, dateProvider = { fixedDate })
    advanceUntilIdle()

    val validJson = backupRepo.exportBackupJson()
    viewModel.onBackupFileLoaded(validJson)

    assertTrue(viewModel.uiState.value.showRestoreConfirmDialog)
    assertNotNull(viewModel.uiState.value.pendingRestoreBackup)
    assertNull(viewModel.uiState.value.backupError)
  }

  @Test
  fun onBackupFileLoaded_invalidJson_setsBackupError() = runTest {
    val fakeDao = FakeDailyGoalDao()
    val backupRepo = createFakeBackupRepository(fakeDao)
    val viewModel = SettingsViewModel(fakeDao, backupRepository = backupRepo, dateProvider = { fixedDate })
    advanceUntilIdle()

    viewModel.onBackupFileLoaded("{ invalid json ...")

    assertFalse(viewModel.uiState.value.showRestoreConfirmDialog)
    assertNull(viewModel.uiState.value.pendingRestoreBackup)
    assertNotNull(viewModel.uiState.value.backupError)
    assertTrue(viewModel.uiState.value.backupError!!.contains("Failed to parse"))
  }

  @Test
  fun onBackupFileLoaded_invalidBackupContent_setsBackupError() = runTest {
    val fakeDao = FakeDailyGoalDao()
    val backupRepo = createFakeBackupRepository(fakeDao)
    val viewModel = SettingsViewModel(fakeDao, backupRepository = backupRepo, dateProvider = { fixedDate })
    advanceUntilIdle()

    // backupFormatVersion is 99 (unsupported)
    val unsupportedBackupJson = """
      {
        "backupFormatVersion": 99,
        "exportTimestamp": "2026-10-06T12:00:00Z",
        "applicationVersion": "1.0",
        "customFoods": [],
        "recipes": [],
        "recipeIngredients": [],
        "mealEntries": [],
        "dailyGoals": []
      }
    """.trimIndent()

    viewModel.onBackupFileLoaded(unsupportedBackupJson)

    assertFalse(viewModel.uiState.value.showRestoreConfirmDialog)
    assertNull(viewModel.uiState.value.pendingRestoreBackup)
    assertNotNull(viewModel.uiState.value.backupError)
    assertTrue(viewModel.uiState.value.backupError!!.contains("Unsupported backup format version"))
  }

  @Test
  fun onConfirmRestore_success_restoresBackupAndReloadsGoals() = runTest {
    val fakeDao = FakeDailyGoalDao()
    val backupRepo = createFakeBackupRepository(fakeDao)
    val viewModel = SettingsViewModel(fakeDao, backupRepository = backupRepo, dateProvider = { fixedDate })
    advanceUntilIdle()

    val backupToRestoreJson = """
      {
        "backupFormatVersion": 1,
        "exportTimestamp": "2026-10-06T12:00:00Z",
        "applicationVersion": "1.0",
        "customFoods": [],
        "recipes": [],
        "recipeIngredients": [],
        "mealEntries": [],
        "dailyGoals": [
          {
            "id": 1,
            "date": "$todayIso",
            "calorieGoal": 2400.0,
            "proteinGoal": 175.0,
            "carbsGoal": 260.0,
            "fatGoal": 75.0
          }
        ]
      }
    """.trimIndent()

    viewModel.onBackupFileLoaded(backupToRestoreJson)
    assertTrue(viewModel.uiState.value.showRestoreConfirmDialog)

    var successCallbackInvoked = false
    viewModel.onConfirmRestore(onSuccess = { successCallbackInvoked = true })
    advanceUntilIdle()

    assertTrue(successCallbackInvoked)
    assertFalse(viewModel.uiState.value.showRestoreConfirmDialog)
    assertNull(viewModel.uiState.value.pendingRestoreBackup)
    assertEquals("Backup restored successfully.", viewModel.uiState.value.backupMessage)

    // Goals should have been reloaded
    val reloadedState = viewModel.uiState.value
    assertEquals("2400", reloadedState.caloriesInput)
    assertEquals("175", reloadedState.proteinInput)
    assertEquals("260", reloadedState.carbsInput)
    assertEquals("75", reloadedState.fatInput)
  }

  @Test
  fun onDismissRestoreDialog_clearsDialogAndPendingBackup() = runTest {
    val fakeDao = FakeDailyGoalDao()
    val backupRepo = createFakeBackupRepository(fakeDao)
    val viewModel = SettingsViewModel(fakeDao, backupRepository = backupRepo, dateProvider = { fixedDate })
    advanceUntilIdle()

    val validJson = backupRepo.exportBackupJson()
    viewModel.onBackupFileLoaded(validJson)
    assertTrue(viewModel.uiState.value.showRestoreConfirmDialog)
    assertNotNull(viewModel.uiState.value.pendingRestoreBackup)

    viewModel.onDismissRestoreDialog()

    assertFalse(viewModel.uiState.value.showRestoreConfirmDialog)
    assertNull(viewModel.uiState.value.pendingRestoreBackup)
  }

  @Test
  fun onDismissBackupFeedback_clearsMessages() = runTest {
    val fakeDao = FakeDailyGoalDao()
    val viewModel = SettingsViewModel(fakeDao, dateProvider = { fixedDate })

    viewModel.onExportCompleted()
    assertNotNull(viewModel.uiState.value.backupMessage)

    viewModel.onDismissBackupFeedback()
    assertNull(viewModel.uiState.value.backupMessage)
    assertNull(viewModel.uiState.value.backupError)
  }
}
