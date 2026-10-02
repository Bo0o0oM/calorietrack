package com.calorietrack.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorietrack.app.data.local.DailyGoalDao
import com.calorietrack.app.data.local.MealEntryDao
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class MacroInfo(
  val name: String,
  val consumed: Int,
  val target: Int,
  val unit: String = "g",
) {
  val progress: Float
    get() = if (target > 0) (consumed.toFloat() / target).coerceIn(0f, 1f) else 0f
}

data class LoggedFoodItem(
  val id: Long,
  val foodId: Long,
  val name: String,
  val quantityGrams: Double,
  val calories: Int,
)

data class MealSection(
  val key: String,
  val displayName: String,
  val consumedCalories: Int = 0,
  val items: List<LoggedFoodItem> = emptyList(),
)

data class DashboardUiState(
  val greeting: String,
  val dateText: String,
  val consumedCalories: Int = 0,
  val targetCalories: Int = 2000,
  val protein: MacroInfo = MacroInfo("Protein", 0, 140),
  val carbs: MacroInfo = MacroInfo("Carbs", 0, 250),
  val fat: MacroInfo = MacroInfo("Fat", 0, 70),
  val meals: List<MealSection> =
    listOf(
      MealSection("breakfast", "Breakfast"),
      MealSection("lunch", "Lunch"),
      MealSection("dinner", "Dinner"),
      MealSection("snack", "Snacks"),
    ),
  val isEmptyDay: Boolean = true,
) {
  val remainingCalories: Int
    get() = (targetCalories - consumedCalories).coerceAtLeast(0)

  val calorieProgress: Float
    get() = if (targetCalories > 0) (consumedCalories.toFloat() / targetCalories).coerceIn(0f, 1f) else 0f
}

fun calculateGreeting(hourOfDay: Int): String =
  when (hourOfDay) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
  }

fun formatDateText(date: LocalDate): String {
  val formatter = DateTimeFormatter.ofPattern("EEE, MMM d")
  return "Today, ${date.format(formatter)}"
}

class MainScreenViewModel(
  private val mealEntryDao: MealEntryDao? = null,
  private val dailyGoalDao: DailyGoalDao? = null,
  timeProvider: () -> LocalTime = { LocalTime.now() },
  dateProvider: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

  private val initialGreeting = calculateGreeting(timeProvider().hour)
  private val initialDate = dateProvider()
  private val initialDateText = formatDateText(initialDate)
  private val todayIso = initialDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

  val uiState: StateFlow<DashboardUiState> =
    if (mealEntryDao == null) {
      MutableStateFlow(
        DashboardUiState(
          greeting = initialGreeting,
          dateText = initialDateText,
          consumedCalories = 0,
          targetCalories = 2000,
          isEmptyDay = true,
        )
      ).asStateFlow()
    } else {
      combine(
        mealEntryDao.getEntriesWithFoodForDate(todayIso),
        mealEntryDao.observeDailyTotals(todayIso),
        dailyGoalDao?.observeForDate(todayIso) ?: flowOf(null),
      ) { entries, totals, goal ->
        val targetCalories = goal?.calorieGoal?.roundToInt() ?: 2000
        val targetProtein = goal?.proteinGoal?.roundToInt() ?: 140
        val targetCarbs = goal?.carbsGoal?.roundToInt() ?: 250
        val targetFat = goal?.fatGoal?.roundToInt() ?: 70

        val consumedCalories = totals.totalCalories.roundToInt()
        val consumedProtein = totals.totalProtein.roundToInt()
        val consumedCarbs = totals.totalCarbs.roundToInt()
        val consumedFat = totals.totalFat.roundToInt()

        val mealDefinitions = listOf(
          "breakfast" to "Breakfast",
          "lunch" to "Lunch",
          "dinner" to "Dinner",
          "snack" to "Snacks",
        )

        val mealSections = mealDefinitions.map { (key, displayName) ->
          val matchingEntries = entries.filter {
            val normalized = it.mealType.trim().lowercase(Locale.ROOT)
            normalized == key || (key == "snack" && normalized == "snacks")
          }
          val items = matchingEntries.map { entry ->
            LoggedFoodItem(
              id = entry.id,
              foodId = entry.foodId,
              name = entry.foodName,
              quantityGrams = entry.quantityGrams,
              calories = entry.calories.roundToInt(),
            )
          }
          MealSection(
            key = key,
            displayName = displayName,
            consumedCalories = items.sumOf { it.calories },
            items = items,
          )
        }

        DashboardUiState(
          greeting = initialGreeting,
          dateText = initialDateText,
          consumedCalories = consumedCalories,
          targetCalories = targetCalories,
          protein = MacroInfo("Protein", consumedProtein, targetProtein),
          carbs = MacroInfo("Carbs", consumedCarbs, targetCarbs),
          fat = MacroInfo("Fat", consumedFat, targetFat),
          meals = mealSections,
          isEmptyDay = entries.isEmpty(),
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(
          greeting = initialGreeting,
          dateText = initialDateText,
          consumedCalories = 0,
          targetCalories = 2000,
          isEmptyDay = true,
        ),
      )
    }

  class Factory(
    private val mealEntryDao: MealEntryDao,
    private val dailyGoalDao: DailyGoalDao? = null,
    private val timeProvider: () -> LocalTime = { LocalTime.now() },
    private val dateProvider: () -> LocalDate = { LocalDate.now() },
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(MainScreenViewModel::class.java)) {
        return MainScreenViewModel(
          mealEntryDao = mealEntryDao,
          dailyGoalDao = dailyGoalDao,
          timeProvider = timeProvider,
          dateProvider = dateProvider,
        ) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
  }
}
