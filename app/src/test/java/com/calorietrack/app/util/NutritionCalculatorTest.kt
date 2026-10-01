package com.calorietrack.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NutritionCalculatorTest {

  private val baseCalories = 165.0 // e.g. chicken breast per 100g
  private val baseProtein = 31.0
  private val baseCarbs = 0.0
  private val baseFat = 3.6

  @Test
  fun calculate_oneHundredGrams_returnsExactReferenceValues() {
    val result = NutritionCalculator.calculate(
      quantityGrams = 100.0,
      caloriesPer100g = baseCalories,
      proteinPer100g = baseProtein,
      carbsPer100g = baseCarbs,
      fatPer100g = baseFat,
    )

    assertEquals(165.0, result.calories, 0.0001)
    assertEquals(31.0, result.protein, 0.0001)
    assertEquals(0.0, result.carbs, 0.0001)
    assertEquals(3.6, result.fat, 0.0001)
  }

  @Test
  fun calculate_twoHundredGrams_doublesAllValues() {
    val result = NutritionCalculator.calculate(
      quantityGrams = 200.0,
      caloriesPer100g = baseCalories,
      proteinPer100g = baseProtein,
      carbsPer100g = baseCarbs,
      fatPer100g = baseFat,
    )

    assertEquals(330.0, result.calories, 0.0001)
    assertEquals(62.0, result.protein, 0.0001)
    assertEquals(0.0, result.carbs, 0.0001)
    assertEquals(7.2, result.fat, 0.0001)
  }

  @Test
  fun calculate_fiftyGrams_halvesAllValues() {
    val result = NutritionCalculator.calculate(
      quantityGrams = 50.0,
      caloriesPer100g = baseCalories,
      proteinPer100g = baseProtein,
      carbsPer100g = baseCarbs,
      fatPer100g = baseFat,
    )

    assertEquals(82.5, result.calories, 0.0001)
    assertEquals(15.5, result.protein, 0.0001)
    assertEquals(0.0, result.carbs, 0.0001)
    assertEquals(1.8, result.fat, 0.0001)
  }

  @Test
  fun calculate_oneHundredFiftyGrams_multipliesByOneAndHalf() {
    val result = NutritionCalculator.calculate(
      quantityGrams = 150.0,
      caloriesPer100g = baseCalories,
      proteinPer100g = baseProtein,
      carbsPer100g = baseCarbs,
      fatPer100g = baseFat,
    )

    assertEquals(247.5, result.calories, 0.0001)
    assertEquals(46.5, result.protein, 0.0001)
    assertEquals(0.0, result.carbs, 0.0001)
    assertEquals(5.4, result.fat, 0.0001)
  }

  @Test
  fun calculate_zeroGrams_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException::class.java) {
      NutritionCalculator.calculate(
        quantityGrams = 0.0,
        caloriesPer100g = baseCalories,
        proteinPer100g = baseProtein,
        carbsPer100g = baseCarbs,
        fatPer100g = baseFat,
      )
    }
  }

  @Test
  fun calculate_negativeGrams_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException::class.java) {
      NutritionCalculator.calculate(
        quantityGrams = -50.0,
        caloriesPer100g = baseCalories,
        proteinPer100g = baseProtein,
        carbsPer100g = baseCarbs,
        fatPer100g = baseFat,
      )
    }
  }

  @Test
  fun calculate_decimalGrams_scalesAccuratelyWithoutIntermediateRounding() {
    val result = NutritionCalculator.calculate(
      quantityGrams = 33.3,
      caloriesPer100g = 100.0,
      proteinPer100g = 10.0,
      carbsPer100g = 20.0,
      fatPer100g = 5.0,
    )

    assertEquals(33.3, result.calories, 0.0001)
    assertEquals(3.33, result.protein, 0.0001)
    assertEquals(6.66, result.carbs, 0.0001)
    assertEquals(1.665, result.fat, 0.0001)
  }
}
