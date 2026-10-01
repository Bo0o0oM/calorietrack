package com.calorietrack.app.data.local

/**
 * Projection data class holding aggregate totals for a specific date's logged meals.
 */
data class DailyNutritionTotals(
  val totalCalories: Double = 0.0,
  val totalProtein: Double = 0.0,
  val totalCarbs: Double = 0.0,
  val totalFat: Double = 0.0,
)
