package com.calorietrack.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a user-created recipe composed of one or more food ingredients.
 * Nutritional totals are calculated across ingredients, while per-100g values
 * are normalized based on the final prepared/cooked weight.
 */
@Entity(
  tableName = "recipes",
  indices = [
    Index(value = ["name"]),
    Index(value = ["search_keywords"]),
  ]
)
data class RecipeEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,

  @ColumnInfo(name = "name")
  val name: String,

  /** Final prepared/cooked weight in grams (user-defined or default sum of raw weights). */
  @ColumnInfo(name = "cooked_weight_grams")
  val cookedWeightGrams: Double,

  /** Total recipe calories calculated from all ingredients. */
  @ColumnInfo(name = "total_calories")
  val totalCalories: Double,

  /** Total recipe protein in grams. */
  @ColumnInfo(name = "total_protein")
  val totalProtein: Double,

  /** Total recipe carbohydrates in grams. */
  @ColumnInfo(name = "total_carbs")
  val totalCarbs: Double,

  /** Total recipe fat in grams. */
  @ColumnInfo(name = "total_fat")
  val totalFat: Double,

  /** Normalized calories per 100g of cooked food: (totalCalories / cookedWeightGrams) * 100 */
  @ColumnInfo(name = "calories_per_100g")
  val caloriesPer100g: Double,

  /** Normalized protein per 100g of cooked food: (totalProtein / cookedWeightGrams) * 100 */
  @ColumnInfo(name = "protein_per_100g")
  val proteinPer100g: Double,

  /** Normalized carbohydrates per 100g of cooked food: (totalCarbs / cookedWeightGrams) * 100 */
  @ColumnInfo(name = "carbs_per_100g")
  val carbsPer100g: Double,

  /** Normalized fat per 100g of cooked food: (totalFat / cookedWeightGrams) * 100 */
  @ColumnInfo(name = "fat_per_100g")
  val fatPer100g: Double,

  /** Soft-delete/archiving flag to preserve historical meal entry joins. */
  @ColumnInfo(name = "is_active", defaultValue = "1")
  val isActive: Boolean = true,

  @ColumnInfo(name = "search_keywords", defaultValue = "")
  val searchKeywords: String = "",
)
