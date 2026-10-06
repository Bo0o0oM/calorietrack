package com.calorietrack.app.ui.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorietrack.app.data.local.FoodDao
import com.calorietrack.app.data.local.FoodEntity
import com.calorietrack.app.data.local.RecipeDao
import com.calorietrack.app.data.local.RecipeEntity
import com.calorietrack.app.data.local.RecipeIngredientEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class RecipeIngredientDraft(
  val draftId: Long,
  val food: FoodEntity,
  val quantityInput: String = "100",
  val quantityGrams: Double = 100.0,
  val error: String? = null,
) {
  val calories: Double get() = food.caloriesPer100g * quantityGrams / 100.0
  val protein: Double get() = food.proteinPer100g * quantityGrams / 100.0
  val carbs: Double get() = food.carbsPer100g * quantityGrams / 100.0
  val fat: Double get() = food.fatPer100g * quantityGrams / 100.0
}

data class RecipeBuilderUiState(
  val recipeId: Long = 0L,
  val mealType: String = "",
  val isEditMode: Boolean = false,
  val isLoading: Boolean = false,
  val name: String = "",
  val nameError: String? = null,
  val ingredients: List<RecipeIngredientDraft> = emptyList(),
  val ingredientsError: String? = null,
  val cookedWeightInput: String = "",
  val isCookedWeightCustomized: Boolean = false,
  val cookedWeightError: String? = null,
  val searchKeywords: String = "",
  val isFoodPickerOpen: Boolean = false,
  val isSaving: Boolean = false,
  val isSaved: Boolean = false,
  val savedRecipeId: Long? = null,
  val generalError: String? = null,
) {
  val rawWeightGrams: Double get() = ingredients.sumOf { it.quantityGrams }
  val totalCalories: Double get() = ingredients.sumOf { it.calories }
  val totalProtein: Double get() = ingredients.sumOf { it.protein }
  val totalCarbs: Double get() = ingredients.sumOf { it.carbs }
  val totalFat: Double get() = ingredients.sumOf { it.fat }

  val effectiveCookedWeight: Double
    get() = cookedWeightInput.toDoubleOrNull()?.takeIf { it > 0.0 } ?: rawWeightGrams

  val caloriesPer100g: Double
    get() = if (effectiveCookedWeight > 0.0) (totalCalories / effectiveCookedWeight) * 100.0 else 0.0

  val proteinPer100g: Double
    get() = if (effectiveCookedWeight > 0.0) (totalProtein / effectiveCookedWeight) * 100.0 else 0.0

  val carbsPer100g: Double
    get() = if (effectiveCookedWeight > 0.0) (totalCarbs / effectiveCookedWeight) * 100.0 else 0.0

  val fatPer100g: Double
    get() = if (effectiveCookedWeight > 0.0) (totalFat / effectiveCookedWeight) * 100.0 else 0.0
}

class RecipeBuilderViewModel(
  val recipeId: Long = 0L,
  val mealType: String = "",
  private val recipeDao: RecipeDao,
  private val foodDao: FoodDao,
) : ViewModel() {

  private var nextDraftId = 1L

  private val _uiState = MutableStateFlow(
    RecipeBuilderUiState(
      recipeId = recipeId,
      mealType = mealType,
      isEditMode = recipeId > 0L,
      isLoading = recipeId > 0L,
    )
  )
  val uiState: StateFlow<RecipeBuilderUiState> = _uiState.asStateFlow()

  // Food search for adding ingredients
  private val _pickerSearchQuery = MutableStateFlow("")
  val pickerSearchQuery: StateFlow<String> = _pickerSearchQuery.asStateFlow()

  @OptIn(ExperimentalCoroutinesApi::class)
  val pickerFoods: StateFlow<List<FoodEntity>> =
    _pickerSearchQuery
      .flatMapLatest { q ->
        val trimmed = q.trim()
        if (trimmed.isBlank()) foodDao.getAll() else foodDao.searchByName(trimmed)
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
      )

  init {
    if (recipeId > 0L) {
      loadRecipeForEdit(recipeId)
    }
  }

  private fun loadRecipeForEdit(id: Long) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, generalError = null) }
      val recipe = recipeDao.getRecipeById(id)
      if (recipe == null) {
        _uiState.update { it.copy(isLoading = false, generalError = "Recipe not found.") }
        return@launch
      }

      val ingredientEntities = recipeDao.getIngredientsForRecipe(id)
      val drafts = ingredientEntities.mapNotNull { ie ->
        val food = foodDao.getById(ie.foodId) ?: return@mapNotNull null
        val qtyText = formatDouble(ie.quantityGrams)
        RecipeIngredientDraft(
          draftId = nextDraftId++,
          food = food,
          quantityInput = qtyText,
          quantityGrams = ie.quantityGrams,
        )
      }

      val cookedWeightText = formatDouble(recipe.cookedWeightGrams)
      val rawWeight = drafts.sumOf { it.quantityGrams }
      val isCustomized = (recipe.cookedWeightGrams != rawWeight)

      _uiState.update {
        it.copy(
          isLoading = false,
          name = recipe.name,
          ingredients = drafts,
          cookedWeightInput = cookedWeightText,
          isCookedWeightCustomized = isCustomized,
          searchKeywords = recipe.searchKeywords,
        )
      }
    }
  }

  fun onNameChanged(newName: String) {
    _uiState.update {
      it.copy(
        name = newName,
        nameError = if (newName.trim().isBlank()) "Recipe name is required" else if (newName.length > 100) "Name cannot exceed 100 characters" else null,
      )
    }
  }

  fun onOpenFoodPicker() {
    _pickerSearchQuery.value = ""
    _uiState.update { it.copy(isFoodPickerOpen = true) }
  }

  fun onCloseFoodPicker() {
    _uiState.update { it.copy(isFoodPickerOpen = false) }
  }

  fun onPickerSearchQueryChanged(q: String) {
    _pickerSearchQuery.value = q
  }

  fun onAddIngredientFood(food: FoodEntity) {
    val newDraft = RecipeIngredientDraft(
      draftId = nextDraftId++,
      food = food,
      quantityInput = "100",
      quantityGrams = 100.0,
    )

    _uiState.update { state ->
      val updatedList = state.ingredients + newDraft
      val newRawWeight = updatedList.sumOf { it.quantityGrams }
      val newCookedWeight = if (state.isCookedWeightCustomized) {
        state.cookedWeightInput
      } else {
        formatDouble(newRawWeight)
      }
      state.copy(
        ingredients = updatedList,
        ingredientsError = null,
        cookedWeightInput = newCookedWeight,
        isFoodPickerOpen = false,
      )
    }
  }

  fun onIngredientQuantityChanged(draftId: Long, input: String) {
    val trimmed = input.trim()
    val parsed = trimmed.toDoubleOrNull()
    val error = when {
      trimmed.isEmpty() -> "Grams required"
      parsed == null -> "Invalid number"
      parsed <= 0.0 -> "Must be > 0"
      parsed > 10000.0 -> "Max 10,000 g"
      else -> null
    }

    _uiState.update { state ->
      val updatedList = state.ingredients.map { draft ->
        if (draft.draftId == draftId) {
          draft.copy(
            quantityInput = input,
            quantityGrams = if (error == null && parsed != null) parsed else draft.quantityGrams,
            error = error,
          )
        } else draft
      }

      val newRawWeight = updatedList.sumOf { it.quantityGrams }
      val newCookedWeight = if (state.isCookedWeightCustomized) {
        state.cookedWeightInput
      } else {
        formatDouble(newRawWeight)
      }

      state.copy(
        ingredients = updatedList,
        cookedWeightInput = newCookedWeight,
      )
    }
  }

  fun onIncrementIngredientQuantity(draftId: Long, step: Double = 10.0) {
    val draft = _uiState.value.ingredients.find { it.draftId == draftId } ?: return
    val newQty = (draft.quantityGrams + step).coerceAtMost(10000.0)
    onIngredientQuantityChanged(draftId, formatDouble(newQty))
  }

  fun onDecrementIngredientQuantity(draftId: Long, step: Double = 10.0) {
    val draft = _uiState.value.ingredients.find { it.draftId == draftId } ?: return
    val newQty = (draft.quantityGrams - step).coerceAtLeast(1.0)
    onIngredientQuantityChanged(draftId, formatDouble(newQty))
  }

  fun onRemoveIngredient(draftId: Long) {
    _uiState.update { state ->
      val updatedList = state.ingredients.filterNot { it.draftId == draftId }
      val newRawWeight = updatedList.sumOf { it.quantityGrams }
      val newCookedWeight = if (state.isCookedWeightCustomized) {
        state.cookedWeightInput
      } else {
        formatDouble(newRawWeight)
      }
      state.copy(
        ingredients = updatedList,
        cookedWeightInput = newCookedWeight,
        ingredientsError = if (updatedList.isEmpty()) "Add at least one ingredient" else null,
      )
    }
  }

  fun onCookedWeightChanged(input: String) {
    val trimmed = input.trim()
    val parsed = trimmed.toDoubleOrNull()
    val error = when {
      trimmed.isEmpty() -> "Cooked weight is required"
      parsed == null -> "Enter a valid number"
      parsed <= 0.0 -> "Cooked weight must be greater than 0"
      parsed > 50000.0 -> "Max 50,000 g"
      else -> null
    }

    _uiState.update {
      it.copy(
        cookedWeightInput = input,
        isCookedWeightCustomized = true,
        cookedWeightError = error,
      )
    }
  }

  fun onResetCookedWeightToRaw() {
    _uiState.update { state ->
      val raw = state.rawWeightGrams
      state.copy(
        cookedWeightInput = formatDouble(raw),
        isCookedWeightCustomized = false,
        cookedWeightError = null,
      )
    }
  }

  fun onSearchKeywordsChanged(keywords: String) {
    _uiState.update { it.copy(searchKeywords = keywords) }
  }

  private fun formatDouble(v: Double): String {
    return if (v % 1.0 == 0.0) "${v.toInt()}" else String.format(Locale.US, "%.1f", v)
  }

  fun validateAll(): Boolean {
    val s = _uiState.value
    val nameErr = when {
      s.name.trim().isBlank() -> "Recipe name is required"
      s.name.length > 100 -> "Name cannot exceed 100 characters"
      else -> null
    }

    val ingredientsErr = when {
      s.ingredients.isEmpty() -> "At least one ingredient is required"
      s.ingredients.any { it.error != null || it.quantityGrams <= 0.0 } -> "Please correct ingredient quantities"
      else -> null
    }

    val cookedWeight = s.cookedWeightInput.trim().toDoubleOrNull()
    val cookedErr = when {
      s.cookedWeightInput.trim().isEmpty() -> "Final cooked weight is required"
      cookedWeight == null -> "Enter a valid number"
      cookedWeight <= 0.0 -> "Final cooked weight must be greater than 0"
      cookedWeight > 50000.0 -> "Max 50,000 g"
      else -> null
    }

    _uiState.update {
      it.copy(
        nameError = nameErr,
        ingredientsError = ingredientsErr,
        cookedWeightError = cookedErr,
      )
    }

    return nameErr == null && ingredientsErr == null && cookedErr == null
  }

  fun saveRecipe(onSuccess: (Long) -> Unit) {
    if (!validateAll()) return
    val s = _uiState.value
    if (s.isSaving) return

    _uiState.update { it.copy(isSaving = true, generalError = null) }

    viewModelScope.launch {
      try {
        val cookedWeight = s.cookedWeightInput.trim().toDouble()
        val totalCalories = s.totalCalories
        val totalProtein = s.totalProtein
        val totalCarbs = s.totalCarbs
        val totalFat = s.totalFat

        val caloriesPer100g = (totalCalories / cookedWeight) * 100.0
        val proteinPer100g = (totalProtein / cookedWeight) * 100.0
        val carbsPer100g = (totalCarbs / cookedWeight) * 100.0
        val fatPer100g = (totalFat / cookedWeight) * 100.0

        val recipeEntity = RecipeEntity(
          id = s.recipeId,
          name = s.name.trim(),
          cookedWeightGrams = cookedWeight,
          totalCalories = totalCalories,
          totalProtein = totalProtein,
          totalCarbs = totalCarbs,
          totalFat = totalFat,
          caloriesPer100g = caloriesPer100g,
          proteinPer100g = proteinPer100g,
          carbsPer100g = carbsPer100g,
          fatPer100g = fatPer100g,
          isActive = true,
          searchKeywords = s.searchKeywords.trim(),
        )

        val ingredientEntities = s.ingredients.map { draft ->
          RecipeIngredientEntity(
            id = 0L,
            recipeId = s.recipeId,
            foodId = draft.food.id,
            quantityGrams = draft.quantityGrams,
          )
        }

        val savedId = recipeDao.saveRecipeWithIngredients(recipeEntity, ingredientEntities)

        _uiState.update {
          it.copy(
            isSaving = false,
            isSaved = true,
            savedRecipeId = savedId,
          )
        }
        onSuccess(savedId)
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
            isSaving = false,
            generalError = e.message ?: "Failed to save recipe.",
          )
        }
      }
    }
  }

  class Factory(
    private val recipeId: Long = 0L,
    private val mealType: String = "",
    private val recipeDao: RecipeDao,
    private val foodDao: FoodDao,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(RecipeBuilderViewModel::class.java)) {
        return RecipeBuilderViewModel(
          recipeId = recipeId,
          mealType = mealType,
          recipeDao = recipeDao,
          foodDao = foodDao,
        ) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
  }
}
