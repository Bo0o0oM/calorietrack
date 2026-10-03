package com.calorietrack.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorietrack.app.data.local.DailyGoalDao
import com.calorietrack.app.data.local.DailyGoalEntity
import com.calorietrack.app.data.local.DailySummary
import com.calorietrack.app.data.local.MealEntryDao
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HistoryDayItem(
  val dateIso: String,
  val formattedDate: String,
  val totalCalories: Int,
  val calorieGoal: Int?,
  val totalProtein: Int,
  val proteinGoal: Int?,
  val totalCarbs: Int,
  val carbsGoal: Int?,
  val totalFat: Int,
  val fatGoal: Int?,
)

data class HistoryUiState(
  val days: List<HistoryDayItem> = emptyList(),
  val isLoading: Boolean = false,
) {
  val isEmpty: Boolean
    get() = !isLoading && days.isEmpty()
}

class HistoryViewModel(
  private val mealEntryDao: MealEntryDao,
  private val dailyGoalDao: DailyGoalDao,
  private val dateProvider: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

  val uiState: StateFlow<HistoryUiState> =
    combine(
      mealEntryDao.observeAllDailyTotals(),
      dailyGoalDao.getAllGoals(),
    ) { summaries: List<DailySummary>, goals: List<DailyGoalEntity> ->
      val summariesByDate = summaries.associateBy { it.date }
      val goalsByDate = goals.associateBy { it.date }

      // A date qualifies if there is at least 1 meal entry OR a daily goal
      val allDates = (summariesByDate.keys + goalsByDate.keys).distinct().sortedDescending()
      val today = dateProvider()

      val dayItems =
        allDates.map { dateIso ->
          val summary = summariesByDate[dateIso]
          val goal = goalsByDate[dateIso]

          val calories = summary?.totalCalories?.roundToInt() ?: 0
          val protein = summary?.totalProtein?.roundToInt() ?: 0
          val carbs = summary?.totalCarbs?.roundToInt() ?: 0
          val fat = summary?.totalFat?.roundToInt() ?: 0

          val calGoal = goal?.calorieGoal?.roundToInt()
          val pGoal = goal?.proteinGoal?.roundToInt()
          val cGoal = goal?.carbsGoal?.roundToInt()
          val fGoal = goal?.fatGoal?.roundToInt()

          HistoryDayItem(
            dateIso = dateIso,
            formattedDate = HistoryDateFormatter.formatFriendlyDate(dateIso, today),
            totalCalories = calories,
            calorieGoal = calGoal,
            totalProtein = protein,
            proteinGoal = pGoal,
            totalCarbs = carbs,
            carbsGoal = cGoal,
            totalFat = fat,
            fatGoal = fGoal,
          )
        }

      HistoryUiState(days = dayItems, isLoading = false)
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = HistoryUiState(isLoading = true),
    )

  class Factory(
    private val mealEntryDao: MealEntryDao,
    private val dailyGoalDao: DailyGoalDao,
    private val dateProvider: () -> LocalDate = { LocalDate.now() },
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
        return HistoryViewModel(mealEntryDao, dailyGoalDao, dateProvider) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
  }
}
