package com.calorietrack.app.data.backup

import com.calorietrack.app.data.local.FoodEntity
import java.time.LocalDate
import java.time.format.DateTimeParseException

sealed class ValidationResult {
  data object Valid : ValidationResult()
  data class Invalid(val reason: String) : ValidationResult()
}

object BackupValidator {

  private val VALID_MEAL_TYPES = setOf("breakfast", "lunch", "dinner", "snack", "snacks")

  /**
   * Validates a parsed backup document against all referential, structural, and numerical integrity rules.
   *
   * @param backup The parsed CalorieTrack backup.
   * @param isBuiltInFoodId Predicate to verify if an ID belongs to the built-in food catalogue (defaults to 1..999).
   */
  fun validate(
    backup: CalorieTrackBackup,
    isBuiltInFoodId: (Long) -> Boolean = { it in 1L until FoodEntity.CUSTOM_MIN_ID },
  ): ValidationResult {
    // 1. Format Version Check
    if (backup.backupFormatVersion != CalorieTrackBackup.CURRENT_BACKUP_FORMAT_VERSION) {
      return ValidationResult.Invalid(
        "Unsupported backup format version: ${backup.backupFormatVersion}. Supported version is ${CalorieTrackBackup.CURRENT_BACKUP_FORMAT_VERSION}."
      )
    }

    // 2. Metadata Check
    if (backup.exportTimestamp.isBlank()) {
      return ValidationResult.Invalid("Backup export timestamp is missing.")
    }

    // 3. Custom Foods Validation
    val customFoodIds = mutableSetOf<Long>()
    for (food in backup.customFoods) {
      if (food.id < FoodEntity.CUSTOM_MIN_ID) {
        return ValidationResult.Invalid("Custom food '${food.name}' has invalid ID ${food.id}. Custom food IDs must be >= ${FoodEntity.CUSTOM_MIN_ID}.")
      }
      if (!customFoodIds.add(food.id)) {
        return ValidationResult.Invalid("Duplicate custom food ID: ${food.id}.")
      }
      if (food.name.isBlank()) {
        return ValidationResult.Invalid("Custom food ID ${food.id} has a blank name.")
      }
      if (!isFiniteAndNonNegative(food.caloriesPer100g, food.proteinPer100g, food.carbsPer100g, food.fatPer100g)) {
        return ValidationResult.Invalid("Custom food '${food.name}' has invalid or negative nutrition values.")
      }
      if (!isFiniteAndPositive(food.servingGrams)) {
        return ValidationResult.Invalid("Custom food '${food.name}' has invalid serving grams: ${food.servingGrams}.")
      }
    }

    // 4. Recipes Validation
    val recipeIds = mutableSetOf<Long>()
    for (recipe in backup.recipes) {
      if (recipe.id <= 0L) {
        return ValidationResult.Invalid("Recipe '${recipe.name}' has invalid ID: ${recipe.id}.")
      }
      if (!recipeIds.add(recipe.id)) {
        return ValidationResult.Invalid("Duplicate recipe ID: ${recipe.id}.")
      }
      if (recipe.name.isBlank()) {
        return ValidationResult.Invalid("Recipe ID ${recipe.id} has a blank name.")
      }
      if (!isFiniteAndPositive(recipe.cookedWeightGrams)) {
        return ValidationResult.Invalid("Recipe '${recipe.name}' has invalid cooked weight: ${recipe.cookedWeightGrams}.")
      }
      if (!isFiniteAndNonNegative(recipe.totalCalories, recipe.totalProtein, recipe.totalCarbs, recipe.totalFat)) {
        return ValidationResult.Invalid("Recipe '${recipe.name}' has invalid total nutrition values.")
      }
      if (!isFiniteAndNonNegative(recipe.caloriesPer100g, recipe.proteinPer100g, recipe.carbsPer100g, recipe.fatPer100g)) {
        return ValidationResult.Invalid("Recipe '${recipe.name}' has invalid per-100g nutrition values.")
      }
    }

    // 5. Recipe Ingredients Validation
    val ingredientIds = mutableSetOf<Long>()
    for (ingredient in backup.recipeIngredients) {
      if (ingredient.id <= 0L) {
        return ValidationResult.Invalid("Recipe ingredient has invalid ID: ${ingredient.id}.")
      }
      if (!ingredientIds.add(ingredient.id)) {
        return ValidationResult.Invalid("Duplicate recipe ingredient ID: ${ingredient.id}.")
      }
      if (!recipeIds.contains(ingredient.recipeId)) {
        return ValidationResult.Invalid("Recipe ingredient ID ${ingredient.id} references non-existent recipe ID ${ingredient.recipeId}.")
      }
      val foodExists = isBuiltInFoodId(ingredient.foodId) || customFoodIds.contains(ingredient.foodId)
      if (!foodExists) {
        return ValidationResult.Invalid("Recipe ingredient ID ${ingredient.id} references non-existent food ID ${ingredient.foodId}.")
      }
      if (!isFiniteAndPositive(ingredient.quantityGrams)) {
        return ValidationResult.Invalid("Recipe ingredient ID ${ingredient.id} has invalid quantity: ${ingredient.quantityGrams}.")
      }
    }

    // 6. Meal Entries Validation
    val mealEntryIds = mutableSetOf<Long>()
    for (entry in backup.mealEntries) {
      if (entry.id <= 0L) {
        return ValidationResult.Invalid("Meal entry has invalid ID: ${entry.id}.")
      }
      if (!mealEntryIds.add(entry.id)) {
        return ValidationResult.Invalid("Duplicate meal entry ID: ${entry.id}.")
      }
      if (!isValidIsoDate(entry.date)) {
        return ValidationResult.Invalid("Meal entry ID ${entry.id} has invalid date format '${entry.date}'. Expected yyyy-MM-dd.")
      }
      if (!VALID_MEAL_TYPES.contains(entry.mealType.trim().lowercase())) {
        return ValidationResult.Invalid("Meal entry ID ${entry.id} has invalid meal type '${entry.mealType}'.")
      }
      if (!isFiniteAndPositive(entry.quantityGrams)) {
        return ValidationResult.Invalid("Meal entry ID ${entry.id} has invalid quantity: ${entry.quantityGrams}.")
      }
      if (!isFiniteAndNonNegative(entry.calories, entry.protein, entry.carbs, entry.fat)) {
        return ValidationResult.Invalid("Meal entry ID ${entry.id} has invalid or negative nutrition values.")
      }

      val isRecipeEntry = entry.recipeId != null && entry.recipeId > 0L
      val isFoodEntry = entry.foodId > 0L

      if (!isRecipeEntry && !isFoodEntry) {
        return ValidationResult.Invalid("Meal entry ID ${entry.id} must reference either a food ID or a recipe ID.")
      }

      if (isRecipeEntry) {
        val recipeValid = recipeIds.contains(entry.recipeId) || !entry.entryName.isNullOrBlank()
        if (!recipeValid) {
          return ValidationResult.Invalid("Meal entry ID ${entry.id} references non-existent recipe ID ${entry.recipeId}.")
        }
      }

      if (isFoodEntry) {
        val foodValid = isBuiltInFoodId(entry.foodId) || customFoodIds.contains(entry.foodId) || !entry.entryName.isNullOrBlank()
        if (!foodValid) {
          return ValidationResult.Invalid("Meal entry ID ${entry.id} references non-existent food ID ${entry.foodId}.")
        }
      }
    }

    // 7. Daily Goals Validation
    val dailyGoalDates = mutableSetOf<String>()
    val dailyGoalIds = mutableSetOf<Long>()
    for (goal in backup.dailyGoals) {
      if (goal.id > 0L && !dailyGoalIds.add(goal.id)) {
        return ValidationResult.Invalid("Duplicate daily goal ID: ${goal.id}.")
      }
      if (!isValidIsoDate(goal.date)) {
        return ValidationResult.Invalid("Daily goal has invalid date '${goal.date}'. Expected yyyy-MM-dd.")
      }
      if (!dailyGoalDates.add(goal.date)) {
        return ValidationResult.Invalid("Duplicate daily goal for date '${goal.date}'.")
      }
      if (!isFiniteAndNonNegative(goal.calorieGoal, goal.proteinGoal, goal.carbsGoal, goal.fatGoal)) {
        return ValidationResult.Invalid("Daily goal for date '${goal.date}' has invalid or negative values.")
      }
    }

    return ValidationResult.Valid
  }

  private fun isValidIsoDate(dateString: String): Boolean {
    if (dateString.length != 10) return false
    return try {
      LocalDate.parse(dateString)
      true
    } catch (_: DateTimeParseException) {
      false
    }
  }

  private fun isFiniteAndPositive(v: Double): Boolean {
    return !v.isNaN() && !v.isInfinite() && v > 0.0 && v <= 50000.0
  }

  private fun isFiniteAndNonNegative(vararg values: Double): Boolean {
    return values.all { !it.isNaN() && !it.isInfinite() && it >= 0.0 }
  }
}
