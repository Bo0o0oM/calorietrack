package com.calorietrack.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a recorded meal consumption event on a specific date.
 * Nutritional totals are denormalized and stored directly to ensure historical log stability.
 */
@Entity(
  tableName = "meal_entries",
  indices = [
    Index(value = ["date"]),
    Index(value = ["meal_type"]),
    Index(value = ["food_id"]),
    Index(value = ["recipe_id"]),
  ]
)
data class MealEntryEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,

  /** ISO-8601 formatted date: yyyy-MM-dd */
  @ColumnInfo(name = "date")
  val date: String,

  /** Standardized lowercase meal category: breakfast, lunch, dinner, snack */
  @ColumnInfo(name = "meal_type")
  val mealType: String,

  @ColumnInfo(name = "food_id")
  val foodId: Long = 0L,

  @ColumnInfo(name = "quantity_grams")
  val quantityGrams: Double,

  @ColumnInfo(name = "calories")
  val calories: Double,

  @ColumnInfo(name = "protein")
  val protein: Double,

  @ColumnInfo(name = "carbs")
  val carbs: Double,

  @ColumnInfo(name = "fat")
  val fat: Double,

  /** Optional reference to a RecipeEntity if this meal entry represents a prepared recipe */
  @ColumnInfo(name = "recipe_id")
  val recipeId: Long? = null,

  /** Snapshot of food or recipe name at the moment of logging */
  @ColumnInfo(name = "entry_name")
  val entryName: String? = null,
) {
  val isRecipe: Boolean
    get() = recipeId != null && recipeId > 0L
}
