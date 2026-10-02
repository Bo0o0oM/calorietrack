package com.calorietrack.app.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorietrack.app.data.local.FoodDao
import com.calorietrack.app.data.local.FoodEntity
import com.calorietrack.app.data.local.MealEntryDao
import com.calorietrack.app.data.local.MealEntryEntity
import com.calorietrack.app.util.NutritionCalculator
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun normalizeMealType(mealType: String): String {
  return when (mealType.trim().lowercase(Locale.ROOT)) {
    "lunch" -> "lunch"
    "dinner" -> "dinner"
    "snack", "snacks" -> "snack"
    else -> "breakfast"
  }
}

fun defaultMealTypeForHour(hour: Int): String {
  return when (hour) {
    in 4..10 -> "breakfast"
    in 11..15 -> "lunch"
    in 16..18 -> "snack"
    else -> "dinner"
  }
}

fun formatMealDisplayName(mealType: String): String {
  return when (mealType.trim().lowercase(Locale.ROOT)) {
    "breakfast" -> "Breakfast"
    "lunch" -> "Lunch"
    "dinner" -> "Dinner"
    "snack", "snacks" -> "Snacks"
    else -> "Meal"
  }
}

data class FoodDetailsUiState(
  val isLoading: Boolean = true,
  val food: FoodEntity? = null,
  val selectedMealType: String = "breakfast",
  val quantityInput: String = "100",
  val quantityGrams: Double? = 100.0,
  val isValidQuantity: Boolean = true,
  val quantityErrorMessage: String? = null,
  val calculatedCalories: Double = 0.0,
  val calculatedProtein: Double = 0.0,
  val calculatedCarbs: Double = 0.0,
  val calculatedFat: Double = 0.0,
  val isSaving: Boolean = false,
  val isSaved: Boolean = false,
  val errorMessage: String? = null,
) {
  val destinationMealDisplayName: String
    get() = formatMealDisplayName(selectedMealType)

  val addButtonText: String
    get() = "Add to $destinationMealDisplayName"
}

class FoodDetailsViewModel(
  val foodId: Long,
  val initialMealType: String = "",
  private val foodDao: FoodDao,
  private val mealEntryDao: MealEntryDao,
  private val dateProvider: () -> LocalDate = { LocalDate.now() },
  private val timeProvider: () -> LocalTime = { LocalTime.now() },
) : ViewModel() {

  private val _uiState = MutableStateFlow(
    FoodDetailsUiState(
      isLoading = true,
      selectedMealType = resolveInitialMealType(initialMealType),
    )
  )
  val uiState: StateFlow<FoodDetailsUiState> = _uiState.asStateFlow()

  init {
    loadFood()
  }

  private fun resolveInitialMealType(rawMealType: String): String {
    return if (rawMealType.isNotBlank()) {
      normalizeMealType(rawMealType)
    } else {
      defaultMealTypeForHour(timeProvider().hour)
    }
  }

  fun loadFood() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      val foundFood = foodDao.getById(foodId)
      if (foundFood != null) {
        val initialNutrition = NutritionCalculator.calculate(
          quantityGrams = 100.0,
          caloriesPer100g = foundFood.caloriesPer100g,
          proteinPer100g = foundFood.proteinPer100g,
          carbsPer100g = foundFood.carbsPer100g,
          fatPer100g = foundFood.fatPer100g,
        )
        _uiState.update {
          it.copy(
            isLoading = false,
            food = foundFood,
            quantityInput = "100",
            quantityGrams = 100.0,
            isValidQuantity = true,
            quantityErrorMessage = null,
            calculatedCalories = initialNutrition.calories,
            calculatedProtein = initialNutrition.protein,
            calculatedCarbs = initialNutrition.carbs,
            calculatedFat = initialNutrition.fat,
          )
        }
      } else {
        _uiState.update {
          it.copy(
            isLoading = false,
            errorMessage = "Food item not found in catalogue.",
          )
        }
      }
    }
  }

  fun onQuantityChanged(input: String) {
    val trimmed = input.trim()
    val currentFood = _uiState.value.food

    if (trimmed.isEmpty()) {
      _uiState.update {
        it.copy(
          quantityInput = input,
          quantityGrams = null,
          isValidQuantity = false,
          quantityErrorMessage = "Enter quantity in grams",
          calculatedCalories = 0.0,
          calculatedProtein = 0.0,
          calculatedCarbs = 0.0,
          calculatedFat = 0.0,
        )
      }
      return
    }

    val parsed = trimmed.toDoubleOrNull()
    if (parsed == null) {
      _uiState.update {
        it.copy(
          quantityInput = input,
          quantityGrams = null,
          isValidQuantity = false,
          quantityErrorMessage = "Please enter a valid number",
          calculatedCalories = 0.0,
          calculatedProtein = 0.0,
          calculatedCarbs = 0.0,
          calculatedFat = 0.0,
        )
      }
      return
    }

    if (parsed <= 0.0) {
      _uiState.update {
        it.copy(
          quantityInput = input,
          quantityGrams = null,
          isValidQuantity = false,
          quantityErrorMessage = "Quantity must be greater than 0 g",
          calculatedCalories = 0.0,
          calculatedProtein = 0.0,
          calculatedCarbs = 0.0,
          calculatedFat = 0.0,
        )
      }
      return
    }

    if (parsed > 10000.0) {
      _uiState.update {
        it.copy(
          quantityInput = input,
          quantityGrams = null,
          isValidQuantity = false,
          quantityErrorMessage = "Quantity cannot exceed 10,000 g",
          calculatedCalories = 0.0,
          calculatedProtein = 0.0,
          calculatedCarbs = 0.0,
          calculatedFat = 0.0,
        )
      }
      return
    }

    if (currentFood != null) {
      val nutrition = NutritionCalculator.calculate(
        quantityGrams = parsed,
        caloriesPer100g = currentFood.caloriesPer100g,
        proteinPer100g = currentFood.proteinPer100g,
        carbsPer100g = currentFood.carbsPer100g,
        fatPer100g = currentFood.fatPer100g,
      )
      _uiState.update {
        it.copy(
          quantityInput = input,
          quantityGrams = parsed,
          isValidQuantity = true,
          quantityErrorMessage = null,
          calculatedCalories = nutrition.calories,
          calculatedProtein = nutrition.protein,
          calculatedCarbs = nutrition.carbs,
          calculatedFat = nutrition.fat,
        )
      }
    } else {
      _uiState.update {
        it.copy(
          quantityInput = input,
          quantityGrams = parsed,
          isValidQuantity = true,
          quantityErrorMessage = null,
        )
      }
    }
  }

  fun onIncrementQuantity(step: Double = 10.0) {
    val current = _uiState.value.quantityGrams ?: 100.0
    val newGrams = (current + step).coerceAtMost(10000.0)
    val text = if (newGrams % 1.0 == 0.0) "${newGrams.toInt()}" else String.format(Locale.US, "%.1f", newGrams)
    onQuantityChanged(text)
  }

  fun onDecrementQuantity(step: Double = 10.0) {
    val current = _uiState.value.quantityGrams ?: 100.0
    val newGrams = (current - step).coerceAtLeast(1.0)
    val text = if (newGrams % 1.0 == 0.0) "${newGrams.toInt()}" else String.format(Locale.US, "%.1f", newGrams)
    onQuantityChanged(text)
  }

  fun onSelectPresetGrams(grams: Double) {
    val text = if (grams % 1.0 == 0.0) "${grams.toInt()}" else String.format(Locale.US, "%.1f", grams)
    onQuantityChanged(text)
  }

  fun onMealTypeSelected(mealType: String) {
    _uiState.update { it.copy(selectedMealType = normalizeMealType(mealType)) }
  }

  fun logMeal(onSuccess: () -> Unit) {
    val state = _uiState.value
    val currentFood = state.food ?: return
    val grams = state.quantityGrams ?: return
    if (grams <= 0.0 || state.isSaving) return

    _uiState.update { it.copy(isSaving = true) }
    viewModelScope.launch {
      try {
        val nutrition = NutritionCalculator.calculate(
          quantityGrams = grams,
          caloriesPer100g = currentFood.caloriesPer100g,
          proteinPer100g = currentFood.proteinPer100g,
          carbsPer100g = currentFood.carbsPer100g,
          fatPer100g = currentFood.fatPer100g,
        )
        val entry = MealEntryEntity(
          date = dateProvider().format(DateTimeFormatter.ISO_LOCAL_DATE),
          mealType = normalizeMealType(state.selectedMealType),
          foodId = currentFood.id,
          quantityGrams = grams,
          calories = nutrition.calories,
          protein = nutrition.protein,
          carbs = nutrition.carbs,
          fat = nutrition.fat,
        )
        mealEntryDao.insert(entry)
        _uiState.update { it.copy(isSaving = false, isSaved = true) }
        onSuccess()
      } catch (e: Exception) {
        _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
      }
    }
  }

  class Factory(
    private val foodId: Long,
    private val initialMealType: String,
    private val foodDao: FoodDao,
    private val mealEntryDao: MealEntryDao,
    private val dateProvider: () -> LocalDate = { LocalDate.now() },
    private val timeProvider: () -> LocalTime = { LocalTime.now() },
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(FoodDetailsViewModel::class.java)) {
        return FoodDetailsViewModel(
          foodId = foodId,
          initialMealType = initialMealType,
          foodDao = foodDao,
          mealEntryDao = mealEntryDao,
          dateProvider = dateProvider,
          timeProvider = timeProvider,
        ) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
  }
}
