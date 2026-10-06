package com.calorietrack.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorietrack.app.data.backup.BackupRepository
import com.calorietrack.app.data.backup.BackupValidator
import com.calorietrack.app.data.backup.CalorieTrackBackup
import com.calorietrack.app.data.backup.ValidationResult
import com.calorietrack.app.data.local.DailyGoalDao
import com.calorietrack.app.data.local.DailyGoalEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
  val caloriesInput: String = "2000",
  val proteinInput: String = "140",
  val carbsInput: String = "250",
  val fatInput: String = "70",
  val caloriesError: String? = null,
  val proteinError: String? = null,
  val carbsError: String? = null,
  val fatError: String? = null,
  val isLoading: Boolean = false,
  val isSaving: Boolean = false,
  val isExporting: Boolean = false,
  val isRestoring: Boolean = false,
  val backupMessage: String? = null,
  val backupError: String? = null,
  val pendingRestoreBackup: CalorieTrackBackup? = null,
  val showRestoreConfirmDialog: Boolean = false,
) {
  val isValid: Boolean
    get() =
      caloriesError == null &&
        proteinError == null &&
        carbsError == null &&
        fatError == null &&
        caloriesInput.isNotBlank() &&
        proteinInput.isNotBlank() &&
        carbsInput.isNotBlank() &&
        fatInput.isNotBlank()
}

class SettingsViewModel(
  private val dailyGoalDao: DailyGoalDao,
  private val backupRepository: BackupRepository? = null,
  private val dateProvider: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

  companion object {
    const val DEFAULT_CALORIES = 2000.0
    const val DEFAULT_PROTEIN = 140.0
    const val DEFAULT_CARBS = 250.0
    const val DEFAULT_FAT = 70.0
    const val MAX_CALORIES = 10000.0
    const val MAX_PROTEIN = 500.0
    const val MAX_CARBS = 1000.0
    const val MAX_FAT = 500.0
  }

  private val todayIso: String
    get() = dateProvider().format(DateTimeFormatter.ISO_LOCAL_DATE)

  private val _uiState = MutableStateFlow(SettingsUiState())
  val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

  init {
    loadGoals()
  }

  fun loadGoals() {
    viewModelScope.launch {
      val existingGoal = dailyGoalDao.getForDate(todayIso)
      if (existingGoal != null) {
        _uiState.update {
          it.copy(
            caloriesInput = formatGoalValue(existingGoal.calorieGoal),
            proteinInput = formatGoalValue(existingGoal.proteinGoal),
            carbsInput = formatGoalValue(existingGoal.carbsGoal),
            fatInput = formatGoalValue(existingGoal.fatGoal),
            caloriesError = null,
            proteinError = null,
            carbsError = null,
            fatError = null,
            isLoading = false,
          )
        }
      } else {
        _uiState.update {
          it.copy(
            caloriesInput = formatGoalValue(DEFAULT_CALORIES),
            proteinInput = formatGoalValue(DEFAULT_PROTEIN),
            carbsInput = formatGoalValue(DEFAULT_CARBS),
            fatInput = formatGoalValue(DEFAULT_FAT),
            caloriesError = null,
            proteinError = null,
            carbsError = null,
            fatError = null,
            isLoading = false,
          )
        }
      }
    }
  }

  fun onCaloriesChanged(input: String) {
    _uiState.update {
      it.copy(
        caloriesInput = input,
        caloriesError = validateCalories(input),
      )
    }
  }

  fun onProteinChanged(input: String) {
    _uiState.update {
      it.copy(
        proteinInput = input,
        proteinError = validateProtein(input),
      )
    }
  }

  fun onCarbsChanged(input: String) {
    _uiState.update {
      it.copy(
        carbsInput = input,
        carbsError = validateCarbs(input),
      )
    }
  }

  fun onFatChanged(input: String) {
    _uiState.update {
      it.copy(
        fatInput = input,
        fatError = validateFat(input),
      )
    }
  }

  fun onResetToDefaults() {
    _uiState.update {
      it.copy(
        caloriesInput = formatGoalValue(DEFAULT_CALORIES),
        proteinInput = formatGoalValue(DEFAULT_PROTEIN),
        carbsInput = formatGoalValue(DEFAULT_CARBS),
        fatInput = formatGoalValue(DEFAULT_FAT),
        caloriesError = null,
        proteinError = null,
        carbsError = null,
        fatError = null,
      )
    }
  }

  fun saveGoals(onSaved: () -> Unit) {
    val currentState = _uiState.value
    val caloriesErr = validateCalories(currentState.caloriesInput)
    val proteinErr = validateProtein(currentState.proteinInput)
    val carbsErr = validateCarbs(currentState.carbsInput)
    val fatErr = validateFat(currentState.fatInput)

    if (caloriesErr != null || proteinErr != null || carbsErr != null || fatErr != null) {
      _uiState.update {
        it.copy(
          caloriesError = caloriesErr,
          proteinError = proteinErr,
          carbsError = carbsErr,
          fatError = fatErr,
        )
      }
      return
    }

    val cal = currentState.caloriesInput.trim().toDouble()
    val protein = currentState.proteinInput.trim().toDouble()
    val carbs = currentState.carbsInput.trim().toDouble()
    val fat = currentState.fatInput.trim().toDouble()

    viewModelScope.launch {
      _uiState.update { it.copy(isSaving = true) }
      val entity = DailyGoalEntity(
        date = todayIso,
        calorieGoal = cal,
        proteinGoal = protein,
        carbsGoal = carbs,
        fatGoal = fat,
      )
      dailyGoalDao.upsert(entity)
      _uiState.update { it.copy(isSaving = false) }
      onSaved()
    }
  }

  // --- Backup & Restore Methods ---

  fun exportBackupJson(onReady: (String) -> Unit) {
    val repo = backupRepository
    if (repo == null) {
      _uiState.update { it.copy(backupError = "Backup repository is not configured.") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isExporting = true, backupError = null, backupMessage = null) }
      try {
        val json = repo.exportBackupJson()
        _uiState.update { it.copy(isExporting = false) }
        onReady(json)
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
            isExporting = false,
            backupError = e.message ?: "Failed to generate backup.",
          )
        }
      }
    }
  }

  fun onExportCompleted() {
    _uiState.update {
      it.copy(
        backupMessage = "Backup successfully exported.",
        backupError = null,
      )
    }
  }

  fun onExportFailed(errorMsg: String) {
    _uiState.update {
      it.copy(
        backupError = errorMsg,
        backupMessage = null,
      )
    }
  }

  fun onBackupFileLoaded(jsonContent: String) {
    val repo = backupRepository
    if (repo == null) {
      _uiState.update { it.copy(backupError = "Backup repository is not configured.") }
      return
    }

    try {
      val backup = repo.parseBackupJson(jsonContent)
      val validation = BackupValidator.validate(backup)
      if (validation is ValidationResult.Invalid) {
        _uiState.update {
          it.copy(
            backupError = "Invalid backup: ${validation.reason}",
            backupMessage = null,
          )
        }
        return
      }

      _uiState.update {
        it.copy(
          pendingRestoreBackup = backup,
          showRestoreConfirmDialog = true,
          backupError = null,
          backupMessage = null,
        )
      }
    } catch (e: Exception) {
      _uiState.update {
        it.copy(
          backupError = "Failed to parse backup file: ${e.message}",
          backupMessage = null,
        )
      }
    }
  }

  fun onConfirmRestore(onSuccess: () -> Unit = {}) {
    val repo = backupRepository ?: return
    val backup = _uiState.value.pendingRestoreBackup ?: return

    viewModelScope.launch {
      _uiState.update {
        it.copy(
          isRestoring = true,
          showRestoreConfirmDialog = false,
          backupError = null,
          backupMessage = null,
        )
      }

      val result = repo.restoreBackup(backup)
      if (result.isSuccess) {
        _uiState.update {
          it.copy(
            isRestoring = false,
            pendingRestoreBackup = null,
            backupMessage = "Backup restored successfully.",
            backupError = null,
          )
        }
        loadGoals()
        onSuccess()
      } else {
        _uiState.update {
          it.copy(
            isRestoring = false,
            pendingRestoreBackup = null,
            backupError = "Restore failed: ${result.exceptionOrNull()?.message}",
            backupMessage = null,
          )
        }
      }
    }
  }

  fun onDismissRestoreDialog() {
    _uiState.update {
      it.copy(
        showRestoreConfirmDialog = false,
        pendingRestoreBackup = null,
      )
    }
  }

  fun onDismissBackupFeedback() {
    _uiState.update {
      it.copy(
        backupMessage = null,
        backupError = null,
      )
    }
  }

  private fun validateCalories(input: String): String? {
    val value = input.trim().toDoubleOrNull() ?: return "Enter a valid calorie number"
    if (value <= 0) return "Calories must be greater than 0"
    if (value > MAX_CALORIES) return "Calories cannot exceed ${MAX_CALORIES.toInt()}"
    return null
  }

  private fun validateProtein(input: String): String? {
    val value = input.trim().toDoubleOrNull() ?: return "Enter a valid protein number"
    if (value <= 0) return "Protein must be greater than 0"
    if (value > MAX_PROTEIN) return "Protein cannot exceed ${MAX_PROTEIN.toInt()}g"
    return null
  }

  private fun validateCarbs(input: String): String? {
    val value = input.trim().toDoubleOrNull() ?: return "Enter a valid carbohydrates number"
    if (value <= 0) return "Carbohydrates must be greater than 0"
    if (value > MAX_CARBS) return "Carbohydrates cannot exceed ${MAX_CARBS.toInt()}g"
    return null
  }

  private fun validateFat(input: String): String? {
    val value = input.trim().toDoubleOrNull() ?: return "Enter a valid fat number"
    if (value <= 0) return "Fat must be greater than 0"
    if (value > MAX_FAT) return "Fat cannot exceed ${MAX_FAT.toInt()}g"
    return null
  }

  private fun formatGoalValue(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

  class Factory(
    private val dailyGoalDao: DailyGoalDao,
    private val backupRepository: BackupRepository? = null,
    private val dateProvider: () -> LocalDate = { LocalDate.now() },
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
        return SettingsViewModel(dailyGoalDao, backupRepository, dateProvider) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
  }
}
