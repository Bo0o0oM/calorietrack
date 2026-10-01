package com.calorietrack.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents the target daily calorie and macro goals for a specific date.
 * Using date as the primary key allows goal adjustments over time while retaining historical targets.
 */
@Entity(tableName = "daily_goals")
data class DailyGoalEntity(
  /** ISO-8601 formatted date: yyyy-MM-dd */
  @PrimaryKey
  @ColumnInfo(name = "date")
  val date: String,

  @ColumnInfo(name = "calorie_goal")
  val calorieGoal: Double,

  @ColumnInfo(name = "protein_goal")
  val proteinGoal: Double,

  @ColumnInfo(name = "carbs_goal")
  val carbsGoal: Double,

  @ColumnInfo(name = "fat_goal")
  val fatGoal: Double,
)
