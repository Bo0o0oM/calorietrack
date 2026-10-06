package com.calorietrack.app.data.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Versioned root backup document for CalorieTrack.
 * Decoupled from internal Room entities to ensure future database evolutions do not break backups.
 */
@Serializable
data class CalorieTrackBackup(
  @SerialName("backupFormatVersion")
  val backupFormatVersion: Int = CURRENT_BACKUP_FORMAT_VERSION,

  @SerialName("applicationVersion")
  val applicationVersion: String = "1.0",

  @SerialName("exportTimestamp")
  val exportTimestamp: String,

  @SerialName("customFoods")
  val customFoods: List<BackupCustomFood> = emptyList(),

  @SerialName("recipes")
  val recipes: List<BackupRecipe> = emptyList(),

  @SerialName("recipeIngredients")
  val recipeIngredients: List<BackupRecipeIngredient> = emptyList(),

  @SerialName("mealEntries")
  val mealEntries: List<BackupMealEntry> = emptyList(),

  @SerialName("dailyGoals")
  val dailyGoals: List<BackupDailyGoal> = emptyList(),
) {
  companion object {
    const val CURRENT_BACKUP_FORMAT_VERSION = 1
  }
}

/**
 * Backup representation of a user-created custom food.
 * Built-in catalogue foods (IDs 1..999) are never exported.
 */
@Serializable
data class BackupCustomFood(
  @SerialName("id")
  val id: Long,

  @SerialName("name")
  val name: String,

  @SerialName("servingDescription")
  val servingDescription: String = "",

  @SerialName("servingGrams")
  val servingGrams: Double = 100.0,

  @SerialName("caloriesPer100g")
  val caloriesPer100g: Double,

  @SerialName("proteinPer100g")
  val proteinPer100g: Double,

  @SerialName("carbsPer100g")
  val carbsPer100g: Double,

  @SerialName("fatPer100g")
  val fatPer100g: Double,

  @SerialName("isActive")
  val isActive: Boolean = true,

  @SerialName("dataSource")
  val dataSource: String = "User",

  @SerialName("sourceId")
  val sourceId: String? = null,

  @SerialName("searchKeywords")
  val searchKeywords: String = "",
)

/**
 * Backup representation of a user-created recipe.
 */
@Serializable
data class BackupRecipe(
  @SerialName("id")
  val id: Long,

  @SerialName("name")
  val name: String,

  @SerialName("cookedWeightGrams")
  val cookedWeightGrams: Double,

  @SerialName("totalCalories")
  val totalCalories: Double,

  @SerialName("totalProtein")
  val totalProtein: Double,

  @SerialName("totalCarbs")
  val totalCarbs: Double,

  @SerialName("totalFat")
  val totalFat: Double,

  @SerialName("caloriesPer100g")
  val caloriesPer100g: Double,

  @SerialName("proteinPer100g")
  val proteinPer100g: Double,

  @SerialName("carbsPer100g")
  val carbsPer100g: Double,

  @SerialName("fatPer100g")
  val fatPer100g: Double,

  @SerialName("isActive")
  val isActive: Boolean = true,

  @SerialName("searchKeywords")
  val searchKeywords: String = "",
)

/**
 * Backup representation of an individual ingredient in a recipe.
 */
@Serializable
data class BackupRecipeIngredient(
  @SerialName("id")
  val id: Long,

  @SerialName("recipeId")
  val recipeId: Long,

  @SerialName("foodId")
  val foodId: Long,

  @SerialName("quantityGrams")
  val quantityGrams: Double,
)

/**
 * Backup representation of a logged meal entry.
 * Stores exact immutable historical nutrition and food/recipe snapshots.
 */
@Serializable
data class BackupMealEntry(
  @SerialName("id")
  val id: Long,

  @SerialName("date")
  val date: String,

  @SerialName("mealType")
  val mealType: String,

  @SerialName("foodId")
  val foodId: Long = 0L,

  @SerialName("recipeId")
  val recipeId: Long? = null,

  @SerialName("entryName")
  val entryName: String? = null,

  @SerialName("quantityGrams")
  val quantityGrams: Double,

  @SerialName("calories")
  val calories: Double,

  @SerialName("protein")
  val protein: Double,

  @SerialName("carbs")
  val carbs: Double,

  @SerialName("fat")
  val fat: Double,
)

/**
 * Backup representation of a daily nutritional target.
 */
@Serializable
data class BackupDailyGoal(
  @SerialName("id")
  val id: Long = 0L,

  @SerialName("date")
  val date: String,

  @SerialName("calorieGoal")
  val calorieGoal: Double,

  @SerialName("proteinGoal")
  val proteinGoal: Double,

  @SerialName("carbsGoal")
  val carbsGoal: Double,

  @SerialName("fatGoal")
  val fatGoal: Double,
)
