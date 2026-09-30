package com.calorietrack.app.ui.main

import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MacroInfo(
  val name: String,
  val consumed: Int,
  val target: Int,
  val unit: String = "g",
) {
  val progress: Float
    get() = if (target > 0) (consumed.toFloat() / target).coerceIn(0f, 1f) else 0f
}

data class MealSection(
  val key: String,
  val displayName: String,
  val consumedCalories: Int = 0,
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
  timeProvider: () -> LocalTime = { LocalTime.now() },
  dateProvider: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

  private val _uiState =
    MutableStateFlow(
      DashboardUiState(
        greeting = calculateGreeting(timeProvider().hour),
        dateText = formatDateText(dateProvider()),
        consumedCalories = 0,
        targetCalories = 2000,
        isEmptyDay = true,
      )
    )
  val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
}
