package com.calorietrack.app.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupValidatorTest {

  private val validBackup = CalorieTrackBackup(
    backupFormatVersion = 1,
    applicationVersion = "1.0",
    exportTimestamp = "2026-10-06T12:00:00Z",
    customFoods = listOf(
      BackupCustomFood(
        id = 1000L,
        name = "Homemade Paneer",
        servingDescription = "100 g",
        servingGrams = 100.0,
        caloriesPer100g = 265.0,
        proteinPer100g = 18.0,
        carbsPer100g = 6.0,
        fatPer100g = 20.0,
        isActive = true,
      )
    ),
    recipes = listOf(
      BackupRecipe(
        id = 1L,
        name = "Shahi Paneer",
        cookedWeightGrams = 300.0,
        totalCalories = 650.0,
        totalProtein = 36.8,
        totalCarbs = 13.2,
        totalFat = 52.0,
        caloriesPer100g = 216.67,
        proteinPer100g = 12.27,
        carbsPer100g = 4.4,
        fatPer100g = 17.33,
        isActive = true,
      )
    ),
    recipeIngredients = listOf(
      BackupRecipeIngredient(
        id = 1L,
        recipeId = 1L,
        foodId = 1000L,
        quantityGrams = 200.0,
      )
    ),
    mealEntries = listOf(
      BackupMealEntry(
        id = 1L,
        date = "2026-10-06",
        mealType = "breakfast",
        foodId = 0L,
        recipeId = 1L,
        entryName = "Shahi Paneer",
        quantityGrams = 150.0,
        calories = 325.0,
        protein = 18.4,
        carbs = 6.6,
        fat = 26.0,
      )
    ),
    dailyGoals = listOf(
      BackupDailyGoal(
        id = 1L,
        date = "2026-10-06",
        calorieGoal = 2000.0,
        proteinGoal = 140.0,
        carbsGoal = 250.0,
        fatGoal = 70.0,
      )
    ),
  )

  @Test
  fun validate_validBackup_returnsValid() {
    val result = BackupValidator.validate(validBackup)
    assertEquals(ValidationResult.Valid, result)
  }

  @Test
  fun validate_unsupportedFormatVersion_returnsInvalid() {
    val backup = validBackup.copy(backupFormatVersion = 99)
    val result = BackupValidator.validate(backup)
    assertTrue(result is ValidationResult.Invalid)
    assertTrue((result as ValidationResult.Invalid).reason.contains("Unsupported backup format version"))
  }

  @Test
  fun validate_missingExportTimestamp_returnsInvalid() {
    val backup = validBackup.copy(exportTimestamp = "   ")
    val result = BackupValidator.validate(backup)
    assertTrue(result is ValidationResult.Invalid)
    assertTrue((result as ValidationResult.Invalid).reason.contains("timestamp"))
  }

  @Test
  fun validate_duplicateCustomFoodIds_returnsInvalid() {
    val duplicateFoods = validBackup.customFoods + validBackup.customFoods
    val backup = validBackup.copy(customFoods = duplicateFoods)
    val result = BackupValidator.validate(backup)
    assertTrue(result is ValidationResult.Invalid)
    assertTrue((result as ValidationResult.Invalid).reason.contains("Duplicate custom food ID"))
  }

  @Test
  fun validate_invalidCustomFoodIdLessThan1000_returnsInvalid() {
    val food = validBackup.customFoods.first().copy(id = 50L) // built-in range!
    val backup = validBackup.copy(customFoods = listOf(food))
    val result = BackupValidator.validate(backup)
    assertTrue(result is ValidationResult.Invalid)
    assertTrue((result as ValidationResult.Invalid).reason.contains("must be >= 1000"))
  }

  @Test
  fun validate_duplicateRecipeIds_returnsInvalid() {
    val duplicateRecipes = validBackup.recipes + validBackup.recipes
    val backup = validBackup.copy(recipes = duplicateRecipes)
    val result = BackupValidator.validate(backup)
    assertTrue(result is ValidationResult.Invalid)
    assertTrue((result as ValidationResult.Invalid).reason.contains("Duplicate recipe ID"))
  }

  @Test
  fun validate_recipeIngredientReferencingNonExistentRecipe_returnsInvalid() {
    val badIngredient = validBackup.recipeIngredients.first().copy(recipeId = 9999L)
    val backup = validBackup.copy(recipeIngredients = listOf(badIngredient))
    val result = BackupValidator.validate(backup)
    assertTrue(result is ValidationResult.Invalid)
    assertTrue((result as ValidationResult.Invalid).reason.contains("references non-existent recipe ID"))
  }

  @Test
  fun validate_recipeIngredientReferencingNonExistentFood_returnsInvalid() {
    val badIngredient = validBackup.recipeIngredients.first().copy(foodId = 8888L)
    val backup = validBackup.copy(recipeIngredients = listOf(badIngredient))
    val result = BackupValidator.validate(backup)
    assertTrue(result is ValidationResult.Invalid)
    assertTrue((result as ValidationResult.Invalid).reason.contains("references non-existent food ID"))
  }

  @Test
  fun validate_invalidMealEntryDate_returnsInvalid() {
    val badEntry = validBackup.mealEntries.first().copy(date = "not-a-date")
    val backup = validBackup.copy(mealEntries = listOf(badEntry))
    val result = BackupValidator.validate(backup)
    assertTrue(result is ValidationResult.Invalid)
    assertTrue((result as ValidationResult.Invalid).reason.contains("invalid date format"))
  }

  @Test
  fun validate_invalidMealType_returnsInvalid() {
    val badEntry = validBackup.mealEntries.first().copy(mealType = "midnight_snack")
    val backup = validBackup.copy(mealEntries = listOf(badEntry))
    val result = BackupValidator.validate(backup)
    assertTrue(result is ValidationResult.Invalid)
    assertTrue((result as ValidationResult.Invalid).reason.contains("invalid meal type"))
  }

  @Test
  fun validate_negativeNutritionValue_returnsInvalid() {
    val badFood = validBackup.customFoods.first().copy(caloriesPer100g = -10.0)
    val backup = validBackup.copy(customFoods = listOf(badFood))
    val result = BackupValidator.validate(backup)
    assertTrue(result is ValidationResult.Invalid)
    assertTrue((result as ValidationResult.Invalid).reason.contains("negative nutrition"))
  }

  @Test
  fun validate_nanOrInfiniteNutrition_returnsInvalid() {
    val badRecipe = validBackup.recipes.first().copy(totalCalories = Double.NaN)
    val backup = validBackup.copy(recipes = listOf(badRecipe))
    val result = BackupValidator.validate(backup)
    assertTrue(result is ValidationResult.Invalid)
    assertTrue((result as ValidationResult.Invalid).reason.contains("invalid total nutrition"))
  }

  @Test
  fun validate_zeroOrNegativeQuantity_returnsInvalid() {
    val badIngredient = validBackup.recipeIngredients.first().copy(quantityGrams = 0.0)
    val backup = validBackup.copy(recipeIngredients = listOf(badIngredient))
    val result = BackupValidator.validate(backup)
    assertTrue(result is ValidationResult.Invalid)
    assertTrue((result as ValidationResult.Invalid).reason.contains("invalid quantity"))
  }

  @Test
  fun validate_duplicateDailyGoalDate_returnsInvalid() {
    val goal1 = validBackup.dailyGoals.first()
    val goal2 = goal1.copy(id = 2L, calorieGoal = 2500.0)
    val backup = validBackup.copy(dailyGoals = listOf(goal1, goal2))
    val result = BackupValidator.validate(backup)
    assertTrue(result is ValidationResult.Invalid)
    assertTrue((result as ValidationResult.Invalid).reason.contains("Duplicate daily goal for date"))
  }
}
