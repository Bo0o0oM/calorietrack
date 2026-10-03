package com.calorietrack.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a reference food item from the built-in catalogue or user-defined custom foods.
 * All nutritional values are strictly normalized per 100 grams.
 */
@Entity(
  tableName = "foods",
  indices = [
    Index(value = ["name"]),
    Index(value = ["search_keywords"]),
  ]
)
data class FoodEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,

  @ColumnInfo(name = "name")
  val name: String,

  @ColumnInfo(name = "serving_description")
  val servingDescription: String,

  @ColumnInfo(name = "serving_grams")
  val servingGrams: Double,

  @ColumnInfo(name = "calories_per_100g")
  val caloriesPer100g: Double,

  @ColumnInfo(name = "protein_per_100g")
  val proteinPer100g: Double,

  @ColumnInfo(name = "carbs_per_100g")
  val carbsPer100g: Double,

  @ColumnInfo(name = "fat_per_100g")
  val fatPer100g: Double,

  @ColumnInfo(name = "is_custom")
  val isCustom: Boolean = false,

  @ColumnInfo(name = "data_source", defaultValue = "USDA FoodData Central")
  val dataSource: String = "USDA FoodData Central",

  @ColumnInfo(name = "source_id")
  val sourceId: String? = null,

  @ColumnInfo(name = "search_keywords", defaultValue = "")
  val searchKeywords: String = "",

  @ColumnInfo(name = "is_active", defaultValue = "1")
  val isActive: Boolean = true,
) {

  companion object {
    const val BUILT_IN_MAX_ID = 999L
    const val CUSTOM_MIN_ID = 1000L
    const val DEFAULT_DATA_SOURCE_BUILT_IN = "USDA FoodData Central"
    const val DEFAULT_DATA_SOURCE_CUSTOM = "User"
    const val SOURCE_USDA = "USDA FoodData Central"
    const val SOURCE_USER = "User"
  }
}
