package com.calorietrack.app.ui.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorietrack.app.data.local.MealEntryDao
import com.calorietrack.app.data.local.MealEntryEntity
import com.calorietrack.app.data.local.RecipeDao
import com.calorietrack.app.data.local.RecipeEntity
import com.calorietrack.app.data.local.RecipeIngredientWithFood
import com.calorietrack.app.ui.details.defaultMealTypeForHour
import com.calorietrack.app.ui.details.formatMealDisplayName
import com.calorietrack.app.ui.details.normalizeMealType
import com.calorietrack.app.util.NutritionCalculator
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecipeDetailsUiState(
  val isLoading: Boolean = true,
  val recipe: RecipeEntity? = null,
  val ingredients: List<RecipeIngredientWithFood> = emptyList(),
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
  val isEditMode: Boolean = false,
  val existingEntryDate: String = "",
) {
  val destinationMealDisplayName: String
    get() = formatMealDisplayName(selectedMealType)

  val addButtonText: String
    get() = if (isEditMode) "Save Changes" else "Add to $destinationMealDisplayName"
}

class RecipeDetailsViewModel(
  val recipeId: Long,
  val initialMealType: String = "",
  val mealEntryId: Long = 0L,
  private val recipeDao: RecipeDao,
  private val mealEntryDao: MealEntryDao,
  private val dateProvider: () -> LocalDate = { LocalDate.now() },
  private val timeProvider: () -> LocalTime = { LocalTime.now() },
) : ViewModel() {

  private val _uiState = MutableStateFlow(
    RecipeDetailsUiState(
      isLoading = true,
      selectedMealType = resolveInitialMealType(initialMealType),
      isEditMode = mealEntryId > 0L,
    )
  )
  val uiState: StateFlow<RecipeDetailsUiState> = _uiState.asStateFlow()

  init {
    loadRecipe()
  }

  private fun resolveInitialMealType(rawMealType: String): String {
    return if (rawMealType.isNotBlank()) {
      normalizeMealType(rawMealType)
    } else {
      defaultMealTypeForHour(timeProvider().hour)
    }
  }

  fun loadRecipe() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }

      if (mealEntryId > 0L) {
        val existingEntry = mealEntryDao.getEntryById(mealEntryId)
        if (existingEntry != null) {
          val targetRecipeId = if (recipeId > 0L) recipeId else (existingEntry.recipeId ?: 0L)
          val foundRecipe = recipeDao.getRecipeById(targetRecipeId)
          if (foundRecipe != null) {
            val ingredients = recipeDao.getIngredientsWithFood(targetRecipeId).first()
            val nutrition = NutritionCalculator.calculate(
              quantityGrams = existingEntry.quantityGrams,
              caloriesPer100g = foundRecipe.caloriesPer100g,
              proteinPer100g = foundRecipe.proteinPer100g,
              carbsPer100g = foundRecipe.carbsPer100g,
              fatPer100g = foundRecipe.fatPer100g,
            )
            val qtyText = if (existingEntry.quantityGrams % 1.0 == 0.0) {
              "${existingEntry.quantityGrams.toInt()}"
            } else {
              String.format(Locale.US, "%.1f", existingEntry.quantityGrams)
            }

            _uiState.update {
              it.copy(
                isLoading = false,
                recipe = foundRecipe,
                ingredients = ingredients,
                selectedMealType = existingEntry.mealType,
                quantityInput = qtyText,
                quantityGrams = existingEntry.quantityGrams,
                isValidQuantity = true,
                quantityErrorMessage = null,
                calculatedCalories = nutrition.calories,
                calculatedProtein = nutrition.protein,
                calculatedCarbs = nutrition.carbs,
                calculatedFat = nutrition.fat,
                isEditMode = true,
                existingEntryDate = existingEntry.date,
              )
            }
            return@launch
          }
        }
      }

      val foundRecipe = recipeDao.getRecipeById(recipeId)
      if (foundRecipe != null) {
        val ingredients = recipeDao.getIngredientsWithFood(recipeId).first()
        val defaultGrams = 100.0
        val initialNutrition = NutritionCalculator.calculate(
          quantityGrams = defaultGrams,
          caloriesPer100g = foundRecipe.caloriesPer100g,
          proteinPer100g = foundRecipe.proteinPer100g,
          carbsPer100g = foundRecipe.carbsPer100g,
          fatPer100g = foundRecipe.fatPer100g,
        )

        _uiState.update {
          it.copy(
            isLoading = false,
            recipe = foundRecipe,
            ingredients = ingredients,
            quantityInput = "100",
            quantityGrams = defaultGrams,
            isValidQuantity = true,
            quantityErrorMessage = null,
            calculatedCalories = initialNutrition.calories,
            calculatedProtein = initialNutrition.protein,
            calculatedCarbs = initialNutrition.carbs,
            calculatedFat = initialNutrition.fat,
            isEditMode = false,
          )
        }
      } else {
        _uiState.update {
          it.copy(
            isLoading = false,
            errorMessage = "Recipe not found.",
          )
        }
      }
    }
  }

  fun onQuantityChanged(input: String) {
    val trimmed = input.trim()
    val currentRecipe = _uiState.value.recipe

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

    if (currentRecipe != null) {
      val nutrition = NutritionCalculator.calculate(
        quantityGrams = parsed,
        caloriesPer100g = currentRecipe.caloriesPer100g,
        proteinPer100g = currentRecipe.proteinPer100g,
        carbsPer100g = currentRecipe.carbsPer100g,
        fatPer100g = currentRecipe.fatPer100g,
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
    val currentRecipe = state.recipe ?: return
    val grams = state.quantityGrams ?: return
    if (grams <= 0.0 || state.isSaving) return

    _uiState.update { it.copy(isSaving = true) }
    viewModelScope.launch {
      try {
        val nutrition = NutritionCalculator.calculate(
          quantityGrams = grams,
          caloriesPer100g = currentRecipe.caloriesPer100g,
          proteinPer100g = currentRecipe.proteinPer100g,
          carbsPer100g = currentRecipe.carbsPer100g,
          fatPer100g = currentRecipe.fatPer100g,
        )
        if (mealEntryId > 0L) {
          val entryDate = state.existingEntryDate.ifBlank {
            dateProvider().format(DateTimeFormatter.ISO_LOCAL_DATE)
          }
          val updatedEntry = MealEntryEntity(
            id = mealEntryId,
            date = entryDate,
            mealType = normalizeMealType(state.selectedMealType),
            foodId = 0L,
            quantityGrams = grams,
            calories = nutrition.calories,
            protein = nutrition.protein,
            carbs = nutrition.carbs,
            fat = nutrition.fat,
            recipeId = currentRecipe.id,
            entryName = currentRecipe.name,
          )
          mealEntryDao.update(updatedEntry)
        } else {
          val entry = MealEntryEntity(
            date = dateProvider().format(DateTimeFormatter.ISO_LOCAL_DATE),
            mealType = normalizeMealType(state.selectedMealType),
            foodId = 0L,
            quantityGrams = grams,
            calories = nutrition.calories,
            protein = nutrition.protein,
            carbs = nutrition.carbs,
            fat = nutrition.fat,
            recipeId = currentRecipe.id,
            entryName = currentRecipe.name,
          )
          mealEntryDao.insert(entry)
        }
        _uiState.update { it.copy(isSaving = false, isSaved = true) }
        onSuccess()
      } catch (e: Exception) {
        _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
      }
    }
  }

  fun archiveRecipe(onSuccess: () -> Unit) {
    val id = _uiState.value.recipe?.id ?: recipeId
    if (id <= 0L) return

    viewModelScope.launch {
      try {
        recipeDao.archiveRecipe(id)
        onSuccess()
      } catch (e: Exception) {
        _uiState.update { it.copy(errorMessage = e.message ?: "Failed to delete recipe.") }
      }
    }
  }

  class Factory(
    private val recipeId: Long,
    private val initialMealType: String = "",
    private val mealEntryId: Long = 0L,
    private val recipeDao: RecipeDao,
    private val mealEntryDao: MealEntryDao,
    private val dateProvider: () -> LocalDate = { LocalDate.now() },
    private val timeProvider: () -> LocalTime = { LocalTime.now() },
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(RecipeDetailsViewModel::class.java)) {
        return RecipeDetailsViewModel(
          recipeId = recipeId,
          initialMealType = initialMealType,
          mealEntryId = mealEntryId,
          recipeDao = recipeDao,
          mealEntryDao = mealEntryDao,
          dateProvider = dateProvider,
          timeProvider = timeProvider,
        ) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
  }
}
