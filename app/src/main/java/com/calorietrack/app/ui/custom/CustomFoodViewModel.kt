package com.calorietrack.app.ui.custom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorietrack.app.data.local.FoodDao
import com.calorietrack.app.data.local.FoodEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomFoodUiState(
  val foodId: Long = 0L,
  val mealType: String = "",
  val isEditMode: Boolean = false,
  val isLoading: Boolean = false,
  val name: String = "",
  val servingDescription: String = "",
  val servingGrams: String = "100",
  val caloriesPer100g: String = "",
  val proteinPer100g: String = "",
  val carbsPer100g: String = "",
  val fatPer100g: String = "",
  val searchKeywords: String = "",
  val nameError: String? = null,
  val servingDescriptionError: String? = null,
  val servingGramsError: String? = null,
  val caloriesError: String? = null,
  val proteinError: String? = null,
  val carbsError: String? = null,
  val fatError: String? = null,
  val macroSumError: String? = null,
  val generalError: String? = null,
  val isSaving: Boolean = false,
  val isSaved: Boolean = false,
  val savedFoodId: Long? = null,
) {
  val isValid: Boolean
    get() = nameError == null &&
      servingDescriptionError == null &&
      servingGramsError == null &&
      caloriesError == null &&
      proteinError == null &&
      carbsError == null &&
      fatError == null &&
      macroSumError == null &&
      name.isNotBlank() &&
      servingDescription.isNotBlank() &&
      servingGrams.isNotBlank() &&
      caloriesPer100g.isNotBlank() &&
      proteinPer100g.isNotBlank() &&
      carbsPer100g.isNotBlank() &&
      fatPer100g.isNotBlank()
}

class CustomFoodViewModel(
  val foodId: Long = 0L,
  val mealType: String = "",
  val initialName: String = "",
  private val foodDao: FoodDao,
) : ViewModel() {

  private val _uiState = MutableStateFlow(
    CustomFoodUiState(
      foodId = foodId,
      mealType = mealType,
      isEditMode = foodId > 0L,
      isLoading = foodId > 0L,
      name = initialName,
    )
  )
  val uiState: StateFlow<CustomFoodUiState> = _uiState.asStateFlow()

  init {
    if (foodId > 0L) {
      loadExistingFood(foodId)
    }
  }

  private fun loadExistingFood(id: Long) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, generalError = null) }
      val food = foodDao.getById(id)
      if (food == null) {
        _uiState.update {
          it.copy(
            isLoading = false,
            generalError = "Food item not found.",
          )
        }
        return@launch
      }

      if (!food.isCustom) {
        _uiState.update {
          it.copy(
            isLoading = false,
            generalError = "Built-in catalogue foods cannot be edited.",
          )
        }
        return@launch
      }

      val formatDouble = { v: Double ->
        if (v % 1.0 == 0.0) "${v.toInt()}" else String.format(java.util.Locale.US, "%.1f", v)
      }

      _uiState.update {
        it.copy(
          isLoading = false,
          name = food.name,
          servingDescription = food.servingDescription,
          servingGrams = formatDouble(food.servingGrams),
          caloriesPer100g = formatDouble(food.caloriesPer100g),
          proteinPer100g = formatDouble(food.proteinPer100g),
          carbsPer100g = formatDouble(food.carbsPer100g),
          fatPer100g = formatDouble(food.fatPer100g),
          searchKeywords = food.searchKeywords,
        )
      }
    }
  }

  fun onNameChanged(value: String) {
    _uiState.update {
      it.copy(
        name = value,
        nameError = if (value.trim().isBlank()) "Food name is required" else if (value.length > 100) "Food name cannot exceed 100 characters" else null,
      )
    }
  }

  fun onServingDescriptionChanged(value: String) {
    _uiState.update {
      it.copy(
        servingDescription = value,
        servingDescriptionError = if (value.trim().isBlank()) "Serving description is required" else if (value.length > 100) "Serving description cannot exceed 100 characters" else null,
      )
    }
  }

  fun onServingGramsChanged(value: String) {
    _uiState.update {
      val error = validateDouble(value, "Serving grams", min = 0.1, max = 10000.0)
      it.copy(
        servingGrams = value,
        servingGramsError = error,
      )
    }
  }

  fun onCaloriesChanged(value: String) {
    _uiState.update {
      val error = validateDouble(value, "Calories", min = 0.0, max = 1000.0)
      it.copy(
        caloriesPer100g = value,
        caloriesError = error,
      )
    }
  }

  fun onProteinChanged(value: String) {
    _uiState.update {
      val error = validateDouble(value, "Protein", min = 0.0, max = 100.0)
      val p = value.toDoubleOrNull() ?: 0.0
      val c = it.carbsPer100g.toDoubleOrNull() ?: 0.0
      val f = it.fatPer100g.toDoubleOrNull() ?: 0.0
      val sumError = if (error == null && it.carbsError == null && it.fatError == null && (p + c + f > 100.0)) {
        "Total macros (P + C + F) cannot exceed 100 g per 100 g"
      } else null
      it.copy(
        proteinPer100g = value,
        proteinError = error,
        macroSumError = sumError,
      )
    }
  }

  fun onCarbsChanged(value: String) {
    _uiState.update {
      val error = validateDouble(value, "Carbohydrates", min = 0.0, max = 100.0)
      val p = it.proteinPer100g.toDoubleOrNull() ?: 0.0
      val c = value.toDoubleOrNull() ?: 0.0
      val f = it.fatPer100g.toDoubleOrNull() ?: 0.0
      val sumError = if (error == null && it.proteinError == null && it.fatError == null && (p + c + f > 100.0)) {
        "Total macros (P + C + F) cannot exceed 100 g per 100 g"
      } else null
      it.copy(
        carbsPer100g = value,
        carbsError = error,
        macroSumError = sumError,
      )
    }
  }

  fun onFatChanged(value: String) {
    _uiState.update {
      val error = validateDouble(value, "Fat", min = 0.0, max = 100.0)
      val p = it.proteinPer100g.toDoubleOrNull() ?: 0.0
      val c = it.carbsPer100g.toDoubleOrNull() ?: 0.0
      val f = value.toDoubleOrNull() ?: 0.0
      val sumError = if (error == null && it.proteinError == null && it.carbsError == null && (p + c + f > 100.0)) {
        "Total macros (P + C + F) cannot exceed 100 g per 100 g"
      } else null
      it.copy(
        fatPer100g = value,
        fatError = error,
        macroSumError = sumError,
      )
    }
  }

  fun onSearchKeywordsChanged(value: String) {
    _uiState.update { it.copy(searchKeywords = value) }
  }

  private fun validateDouble(value: String, fieldName: String, min: Double, max: Double): String? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return "$fieldName is required"
    val parsed = trimmed.toDoubleOrNull() ?: return "Enter a valid number"
    if (parsed < min) {
      return if (min > 0.0) "$fieldName must be greater than 0" else "$fieldName cannot be negative"
    }
    if (parsed > max) {
      val maxStr = if (max % 1.0 == 0.0) "${max.toInt()}" else String.format(java.util.Locale.US, "%.1f", max)
      return "$fieldName cannot exceed $maxStr"
    }
    return null
  }

  fun validateAll(): Boolean {
    val s = _uiState.value
    val nameErr = if (s.name.trim().isBlank()) "Food name is required" else if (s.name.length > 100) "Food name cannot exceed 100 characters" else null
    val descErr = if (s.servingDescription.trim().isBlank()) "Serving description is required" else if (s.servingDescription.length > 100) "Serving description cannot exceed 100 characters" else null
    val gramsErr = validateDouble(s.servingGrams, "Serving grams", min = 0.1, max = 10000.0)
    val calErr = validateDouble(s.caloriesPer100g, "Calories", min = 0.0, max = 1000.0)
    val pErr = validateDouble(s.proteinPer100g, "Protein", min = 0.0, max = 100.0)
    val cErr = validateDouble(s.carbsPer100g, "Carbohydrates", min = 0.0, max = 100.0)
    val fErr = validateDouble(s.fatPer100g, "Fat", min = 0.0, max = 100.0)

    val p = s.proteinPer100g.toDoubleOrNull() ?: 0.0
    val c = s.carbsPer100g.toDoubleOrNull() ?: 0.0
    val f = s.fatPer100g.toDoubleOrNull() ?: 0.0
    val sumErr = if (pErr == null && cErr == null && fErr == null && (p + c + f > 100.0)) {
      "Total macros (P + C + F) cannot exceed 100 g per 100 g"
    } else null

    _uiState.update {
      it.copy(
        nameError = nameErr,
        servingDescriptionError = descErr,
        servingGramsError = gramsErr,
        caloriesError = calErr,
        proteinError = pErr,
        carbsError = cErr,
        fatError = fErr,
        macroSumError = sumErr,
      )
    }

    return nameErr == null && descErr == null && gramsErr == null && calErr == null && pErr == null && cErr == null && fErr == null && sumErr == null
  }

  fun saveCustomFood(onSuccess: (Long) -> Unit) {
    if (!validateAll()) return
    val s = _uiState.value
    if (s.isSaving) return

    _uiState.update { it.copy(isSaving = true, generalError = null) }

    viewModelScope.launch {
      try {
        val servingGrams = s.servingGrams.trim().toDouble()
        val calories = s.caloriesPer100g.trim().toDouble()
        val protein = s.proteinPer100g.trim().toDouble()
        val carbs = s.carbsPer100g.trim().toDouble()
        val fat = s.fatPer100g.trim().toDouble()

        val finalId: Long
        if (s.isEditMode && foodId > 0L) {
          val existing = foodDao.getById(foodId)
          if (existing == null || !existing.isCustom) {
            _uiState.update { it.copy(isSaving = false, generalError = "Unable to update non-custom food.") }
            return@launch
          }
          val updated = existing.copy(
            name = s.name.trim(),
            servingDescription = s.servingDescription.trim(),
            servingGrams = servingGrams,
            caloriesPer100g = calories,
            proteinPer100g = protein,
            carbsPer100g = carbs,
            fatPer100g = fat,
            searchKeywords = s.searchKeywords.trim(),
            isCustom = true,
            isActive = true,
            dataSource = FoodEntity.DEFAULT_DATA_SOURCE_CUSTOM,
          )
          foodDao.update(updated)
          finalId = foodId
        } else {
          val maxId = foodDao.getMaxId() ?: (FoodEntity.CUSTOM_MIN_ID - 1L)
          val newId = maxOf(FoodEntity.CUSTOM_MIN_ID, maxId + 1L)
          val newFood = FoodEntity(
            id = newId,
            name = s.name.trim(),
            servingDescription = s.servingDescription.trim(),
            servingGrams = servingGrams,
            caloriesPer100g = calories,
            proteinPer100g = protein,
            carbsPer100g = carbs,
            fatPer100g = fat,
            searchKeywords = s.searchKeywords.trim(),
            isCustom = true,
            isActive = true,
            dataSource = FoodEntity.DEFAULT_DATA_SOURCE_CUSTOM,
            sourceId = null,
          )
          foodDao.insert(newFood)
          finalId = newId
        }

        _uiState.update {
          it.copy(
            isSaving = false,
            isSaved = true,
            savedFoodId = finalId,
          )
        }
        onSuccess(finalId)
      } catch (e: Exception) {
        _uiState.update { it.copy(isSaving = false, generalError = e.message ?: "Failed to save custom food.") }
      }
    }
  }

  class Factory(
    private val foodId: Long = 0L,
    private val mealType: String = "",
    private val initialName: String = "",
    private val foodDao: FoodDao,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(CustomFoodViewModel::class.java)) {
        return CustomFoodViewModel(
          foodId = foodId,
          mealType = mealType,
          initialName = initialName,
          foodDao = foodDao,
        ) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
  }
}
