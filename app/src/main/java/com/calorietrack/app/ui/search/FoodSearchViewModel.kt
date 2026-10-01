package com.calorietrack.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorietrack.app.data.local.FoodDao
import com.calorietrack.app.data.local.FoodEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class FoodSearchUiState(
  val query: String = "",
  val mealType: String = "",
  val results: List<FoodEntity> = emptyList(),
  val isLoading: Boolean = false,
) {
  val isQueryBlank: Boolean
    get() = query.trim().isBlank()

  val isEmptyResult: Boolean
    get() = !isLoading && !isQueryBlank && results.isEmpty()
}

/**
 * Formats a contextual subtitle based on the target meal type.
 */
fun formatMealContextSubtitle(mealType: String): String {
  return when (mealType.trim().lowercase()) {
    "breakfast" -> "Add to Breakfast"
    "lunch" -> "Add to Lunch"
    "dinner" -> "Add to Dinner"
    "snack", "snacks" -> "Add to Snacks"
    else -> "Offline Food Catalogue"
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
class FoodSearchViewModel(
  private val foodDao: FoodDao,
  val mealType: String = "",
) : ViewModel() {

  private val _query = MutableStateFlow("")
  val query: StateFlow<String> = _query.asStateFlow()

  val uiState: StateFlow<FoodSearchUiState> =
    _query
      .flatMapLatest { rawQuery ->
        val trimmed = rawQuery.trim()
        val flow =
          if (trimmed.isBlank()) {
            foodDao.getAll()
          } else {
            foodDao.searchByName(trimmed)
          }
        flow.map { foods ->
          FoodSearchUiState(
            query = rawQuery,
            mealType = mealType,
            results = foods,
            isLoading = false,
          )
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue =
          FoodSearchUiState(
            query = "",
            mealType = mealType,
            results = emptyList(),
            isLoading = true,
          ),
      )

  fun onQueryChanged(newQuery: String) {
    _query.value = newQuery
  }

  fun onClearQuery() {
    _query.value = ""
  }

  class Factory(
    private val foodDao: FoodDao,
    private val mealType: String,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(FoodSearchViewModel::class.java)) {
        return FoodSearchViewModel(foodDao, mealType) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
  }
}
