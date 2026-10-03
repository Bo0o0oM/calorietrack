package com.calorietrack.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorietrack.app.data.local.DailyGoalDao
import com.calorietrack.app.data.local.MealEntryDao
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HistoricalFoodItem(
  val id: Long,
  val foodId: Long,
  val name: String,
  val quantityGrams: Double,
  val calories: Int,
  val protein: Double,
  val carbs: Double,
  val fat: Double,
)

data class HistoricalMealSection(
  val key: String,
  val displayName: String,
  val consumedCalories: Int = 0,
  val items: List<HistoricalFoodItem> = emptyList(),
)

data class HistoricalDayDetailsUiState(
  val dateIso: String,
  val formattedDate: String,
  val totalCalories: Int = 0,
  val calorieGoal: Int? = null,
  val totalProtein: Int = 0,
  val proteinGoal: Int? = null,
  val totalCarbs: Int = 0,
  val carbsGoal: Int? = null,
  val totalFat: Int = 0,
  val fatGoal: Int? = null,
  val meals: List<HistoricalMealSection> = emptyList(),
  val isLoading: Boolean = true,
)

class HistoricalDayDetailsViewModel(
  private val dateIso: String,
  private val mealEntryDao: MealEntryDao,
  private val dailyGoalDao: DailyGoalDao,
  private val dateProvider: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

  val uiState: StateFlow<HistoricalDayDetailsUiState> =
    combine(
      mealEntryDao.getEntriesWithFoodForDate(dateIso),
      mealEntryDao.observeDailyTotals(dateIso),
      dailyGoalDao.observeForDate(dateIso),
    ) { entries, totals, goal ->
      val today = dateProvider()

      val consumedCalories = totals.totalCalories.roundToInt()
      val consumedProtein = totals.totalProtein.roundToInt()
      val consumedCarbs = totals.totalCarbs.roundToInt()
      val consumedFat = totals.totalFat.roundToInt()

      val calGoal = goal?.calorieGoal?.roundToInt()
      val pGoal = goal?.proteinGoal?.roundToInt()
      val cGoal = goal?.carbsGoal?.roundToInt()
      val fGoal = goal?.fatGoal?.roundToInt()

      val mealDefinitions =
        listOf(
          "breakfast" to "Breakfast",
          "lunch" to "Lunch",
          "dinner" to "Dinner",
          "snack" to "Snacks",
        )

      val mealSections =
        mealDefinitions.map { (key, displayName) ->
          val matchingEntries =
            entries.filter {
              val normalized = it.mealType.trim().lowercase(Locale.ROOT)
              normalized == key || (key == "snack" && normalized == "snacks")
            }
          val items =
            matchingEntries.map { entry ->
              HistoricalFoodItem(
                id = entry.id,
                foodId = entry.foodId,
                name = entry.foodName,
                quantityGrams = entry.quantityGrams,
                calories = entry.calories.roundToInt(),
                protein = entry.protein,
                carbs = entry.carbs,
                fat = entry.fat,
              )
            }
          HistoricalMealSection(
            key = key,
            displayName = displayName,
            consumedCalories = items.sumOf { it.calories },
            items = items,
          )
        }

      HistoricalDayDetailsUiState(
        dateIso = dateIso,
        formattedDate = HistoryDateFormatter.formatDayDetailHeader(dateIso, today),
        totalCalories = consumedCalories,
        calorieGoal = calGoal,
        totalProtein = consumedProtein,
        proteinGoal = pGoal,
        totalCarbs = consumedCarbs,
        carbsGoal = cGoal,
        totalFat = consumedFat,
        fatGoal = fGoal,
        meals = mealSections,
        isLoading = false,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue =
        HistoricalDayDetailsUiState(
          dateIso = dateIso,
          formattedDate = HistoryDateFormatter.formatDayDetailHeader(dateIso, dateProvider()),
          isLoading = true,
        ),
    )

  class Factory(
    private val dateIso: String,
    private val mealEntryDao: MealEntryDao,
    private val dailyGoalDao: DailyGoalDao,
    private val dateProvider: () -> LocalDate = { LocalDate.now() },
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(HistoricalDayDetailsViewModel::class.java)) {
        return HistoricalDayDetailsViewModel(dateIso, mealEntryDao, dailyGoalDao, dateProvider) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
  }
}
