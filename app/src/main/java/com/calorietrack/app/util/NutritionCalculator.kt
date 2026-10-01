package com.calorietrack.app.util

/**
 * Result data class holding the exact calculated nutritional values for a given serving weight.
 */
data class CalculatedNutrition(
  val calories: Double,
  val protein: Double,
  val carbs: Double,
  val fat: Double,
)

/**
 * Pure Kotlin mathematical engine for scaling calories and macronutrients based on consumed grams.
 */
object NutritionCalculator {

  /**
   * Scales nutritional values based on per-100g reference values and consumed quantity in grams.
   *
   * @param quantityGrams The weight of the consumed serving in grams (must be > 0).
   * @param caloriesPer100g Energy in kcal per 100 grams.
   * @param proteinPer100g Protein in grams per 100 grams.
   * @param carbsPer100g Carbohydrates in grams per 100 grams.
   * @param fatPer100g Fat in grams per 100 grams.
   * @throws IllegalArgumentException if quantityGrams is zero or negative.
   */
  fun calculate(
    quantityGrams: Double,
    caloriesPer100g: Double,
    proteinPer100g: Double,
    carbsPer100g: Double,
    fatPer100g: Double,
  ): CalculatedNutrition {
    require(quantityGrams > 0.0) {
      "Quantity in grams must be strictly positive (> 0.0), was: $quantityGrams"
    }

    val multiplier = quantityGrams / 100.0

    return CalculatedNutrition(
      calories = caloriesPer100g * multiplier,
      protein = proteinPer100g * multiplier,
      carbs = carbsPer100g * multiplier,
      fat = fatPer100g * multiplier,
    )
  }
}
