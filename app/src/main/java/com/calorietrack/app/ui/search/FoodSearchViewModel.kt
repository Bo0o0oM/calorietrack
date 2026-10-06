package com.calorietrack.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorietrack.app.data.local.FoodDao
import com.calorietrack.app.data.local.FoodEntity
import com.calorietrack.app.data.local.RecipeDao
import com.calorietrack.app.data.local.RecipeEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FoodSearchTab {
  ALL,
  MY_FOODS,
  MY_RECIPES,
}

data class FoodSearchUiState(
  val query: String = "",
  val mealType: String = "",
  val selectedTab: FoodSearchTab = FoodSearchTab.ALL,
  val results: List<FoodEntity> = emptyList(),
  val recipeResults: List<RecipeEntity> = emptyList(),
  val isLoading: Boolean = false,
) {
  val isQueryBlank: Boolean
    get() = query.trim().isBlank()

  val isEmptyResult: Boolean
    get() = !isLoading && (if (selectedTab == FoodSearchTab.MY_RECIPES) recipeResults.isEmpty() else results.isEmpty())
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
  private val recipeDao: RecipeDao? = null,
  val mealType: String = "",
) : ViewModel() {

  private val _query = MutableStateFlow("")
  val query: StateFlow<String> = _query.asStateFlow()

  private val _selectedTab = MutableStateFlow(FoodSearchTab.ALL)
  val selectedTab: StateFlow<FoodSearchTab> = _selectedTab.asStateFlow()

  val uiState: StateFlow<FoodSearchUiState> =
    combine(_query, _selectedTab) { rawQuery, tab ->
      Pair(rawQuery, tab)
    }
      .flatMapLatest { (rawQuery, tab) ->
        val trimmed = rawQuery.trim()
        when (tab) {
          FoodSearchTab.ALL -> {
            val flow = if (trimmed.isBlank()) foodDao.getAll() else foodDao.searchByName(trimmed)
            flow.map { foods ->
              FoodSearchUiState(
                query = rawQuery,
                mealType = mealType,
                selectedTab = tab,
                results = foods,
                recipeResults = emptyList(),
                isLoading = false,
              )
            }
          }
          FoodSearchTab.MY_FOODS -> {
            val flow = if (trimmed.isBlank()) foodDao.getMyFoods() else foodDao.searchMyFoods(trimmed)
            flow.map { foods ->
              FoodSearchUiState(
                query = rawQuery,
                mealType = mealType,
                selectedTab = tab,
                results = foods,
                recipeResults = emptyList(),
                isLoading = false,
              )
            }
          }
          FoodSearchTab.MY_RECIPES -> {
            val rDao = recipeDao
            val flow = if (rDao == null) {
              flowOf(emptyList())
            } else if (trimmed.isBlank()) {
              rDao.getAllActiveRecipes()
            } else {
              rDao.searchActiveRecipes(trimmed)
            }
            flow.map { recipes ->
              FoodSearchUiState(
                query = rawQuery,
                mealType = mealType,
                selectedTab = tab,
                results = emptyList(),
                recipeResults = recipes,
                isLoading = false,
              )
            }
          }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue =
          FoodSearchUiState(
            query = "",
            mealType = mealType,
            selectedTab = FoodSearchTab.ALL,
            results = emptyList(),
            recipeResults = emptyList(),
            isLoading = true,
          ),
      )

  fun onQueryChanged(newQuery: String) {
    _query.value = newQuery
  }

  fun onClearQuery() {
    _query.value = ""
  }

  fun onTabSelected(tab: FoodSearchTab) {
    _selectedTab.value = tab
  }

  fun archiveFood(id: Long) {
    viewModelScope.launch {
      foodDao.archiveFood(id)
    }
  }

  fun archiveRecipe(id: Long) {
    viewModelScope.launch {
      recipeDao?.archiveRecipe(id)
    }
  }

  class Factory(
    private val foodDao: FoodDao,
    private val recipeDao: RecipeDao? = null,
    private val mealType: String = "",
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(FoodSearchViewModel::class.java)) {
        return FoodSearchViewModel(foodDao, recipeDao, mealType) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
  }
}
