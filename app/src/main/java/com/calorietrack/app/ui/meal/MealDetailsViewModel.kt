package com.calorietrack.app.ui.meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorietrack.app.data.local.MealEntryDao
import com.calorietrack.app.data.local.MealEntryWithFood
import com.calorietrack.app.ui.details.formatMealDisplayName
import com.calorietrack.app.ui.details.normalizeMealType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MealDetailsUiState(
  val isLoading: Boolean = true,
  val mealType: String,
  val date: String,
  val entries: List<MealEntryWithFood> = emptyList(),
  val totalCalories: Int = 0,
  val totalProtein: Double = 0.0,
  val totalCarbs: Double = 0.0,
  val totalFat: Double = 0.0,
  val entryPendingDelete: MealEntryWithFood? = null,
) {
  val displayName: String
    get() = formatMealDisplayName(mealType)

  val subtitle: String
    get() = "Today's entries"

  val isEmpty: Boolean
    get() = !isLoading && entries.isEmpty()
}

class MealDetailsViewModel(
  val mealType: String,
  val date: String = "",
  private val mealEntryDao: MealEntryDao,
  private val dateProvider: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

  private val effectiveDate = date.ifBlank {
    dateProvider().format(DateTimeFormatter.ISO_LOCAL_DATE)
  }
  private val normalizedMeal = normalizeMealType(mealType)

  private val _pendingDelete = MutableStateFlow<MealEntryWithFood?>(null)

  val uiState: StateFlow<MealDetailsUiState> =
    combine(
      mealEntryDao.getEntriesWithFoodForDateAndMealType(effectiveDate, normalizedMeal),
      _pendingDelete,
    ) { entries, pendingDelete ->
      val totalCalories = entries.sumOf { it.calories }.roundToInt()
      val totalProtein = entries.sumOf { it.protein }
      val totalCarbs = entries.sumOf { it.carbs }
      val totalFat = entries.sumOf { it.fat }

      MealDetailsUiState(
        isLoading = false,
        mealType = normalizedMeal,
        date = effectiveDate,
        entries = entries,
        totalCalories = totalCalories,
        totalProtein = totalProtein,
        totalCarbs = totalCarbs,
        totalFat = totalFat,
        entryPendingDelete = pendingDelete,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = MealDetailsUiState(
        isLoading = true,
        mealType = normalizedMeal,
        date = effectiveDate,
      ),
    )

  fun onRequestDelete(entry: MealEntryWithFood) {
    _pendingDelete.value = entry
  }

  fun onDismissDelete() {
    _pendingDelete.value = null
  }

  fun onConfirmDelete() {
    val target = _pendingDelete.value ?: return
    viewModelScope.launch {
      mealEntryDao.deleteById(target.id)
      _pendingDelete.value = null
    }
  }

  class Factory(
    private val mealType: String,
    private val date: String = "",
    private val mealEntryDao: MealEntryDao,
    private val dateProvider: () -> LocalDate = { LocalDate.now() },
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(MealDetailsViewModel::class.java)) {
        return MealDetailsViewModel(
          mealType = mealType,
          date = date,
          mealEntryDao = mealEntryDao,
          dateProvider = dateProvider,
        ) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
  }
}
