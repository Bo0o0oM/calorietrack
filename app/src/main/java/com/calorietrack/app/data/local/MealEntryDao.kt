package com.calorietrack.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MealEntryDao {

  @Query("SELECT * FROM meal_entries WHERE date = :date ORDER BY id ASC")
  fun getEntriesForDate(date: String): Flow<List<MealEntryEntity>>

  @Query("SELECT * FROM meal_entries WHERE date = :date AND meal_type = :mealType ORDER BY id ASC")
  fun getEntriesForDateAndMealType(date: String, mealType: String): Flow<List<MealEntryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(entry: MealEntryEntity): Long

  @Update
  suspend fun update(entry: MealEntryEntity)

  @Delete
  suspend fun delete(entry: MealEntryEntity)

  @Query("DELETE FROM meal_entries WHERE date = :date")
  suspend fun deleteForDate(date: String)

  @Query(
    """
    SELECT 
      COALESCE(SUM(calories), 0.0) AS totalCalories,
      COALESCE(SUM(protein), 0.0) AS totalProtein,
      COALESCE(SUM(carbs), 0.0) AS totalCarbs,
      COALESCE(SUM(fat), 0.0) AS totalFat
    FROM meal_entries
    WHERE date = :date
    """
  )
  fun observeDailyTotals(date: String): Flow<DailyNutritionTotals>
}
