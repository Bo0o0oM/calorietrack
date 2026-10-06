package com.calorietrack.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a single ingredient entry in a recipe, linking to a FoodEntity.
 */
@Entity(
  tableName = "recipe_ingredients",
  foreignKeys = [
    ForeignKey(
      entity = RecipeEntity::class,
      parentColumns = ["id"],
      childColumns = ["recipe_id"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index(value = ["recipe_id"]),
    Index(value = ["food_id"]),
  ]
)
data class RecipeIngredientEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,

  @ColumnInfo(name = "recipe_id")
  val recipeId: Long,

  @ColumnInfo(name = "food_id")
  val foodId: Long,

  @ColumnInfo(name = "quantity_grams")
  val quantityGrams: Double,
)

/**
 * Joined presentation model for a recipe ingredient along with the resolved food details.
 */
data class RecipeIngredientWithFood(
  val id: Long,
  val recipeId: Long,
  val foodId: Long,
  val quantityGrams: Double,
  val foodName: String,
  val caloriesPer100g: Double,
  val proteinPer100g: Double,
  val carbsPer100g: Double,
  val fatPer100g: Double,
  val calories: Double,
  val protein: Double,
  val carbs: Double,
  val fat: Double,
)
