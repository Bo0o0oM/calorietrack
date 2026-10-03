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

  @Query(
    """
    SELECT
      meal_entries.id AS id,
      meal_entries.date AS date,
      meal_entries.meal_type AS mealType,
      meal_entries.food_id AS foodId,
      meal_entries.quantity_grams AS quantityGrams,
      meal_entries.calories AS calories,
      meal_entries.protein AS protein,
      meal_entries.carbs AS carbs,
      meal_entries.fat AS fat,
      COALESCE(foods.name, 'Unknown Food') AS foodName
    FROM meal_entries
    LEFT JOIN foods ON meal_entries.food_id = foods.id
    WHERE meal_entries.date = :date
    ORDER BY meal_entries.id ASC
    """
  )
  fun getEntriesWithFoodForDate(date: String): Flow<List<MealEntryWithFood>>

  @Query("SELECT * FROM meal_entries WHERE id = :id LIMIT 1")
  suspend fun getEntryById(id: Long): MealEntryEntity?

  @Query("DELETE FROM meal_entries WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query(
    """
    SELECT
      meal_entries.id AS id,
      meal_entries.date AS date,
      meal_entries.meal_type AS mealType,
      meal_entries.food_id AS foodId,
      meal_entries.quantity_grams AS quantityGrams,
      meal_entries.calories AS calories,
      meal_entries.protein AS protein,
      meal_entries.carbs AS carbs,
      meal_entries.fat AS fat,
      COALESCE(foods.name, 'Unknown Food') AS foodName
    FROM meal_entries
    LEFT JOIN foods ON meal_entries.food_id = foods.id
    WHERE meal_entries.date = :date
      AND (
        meal_entries.meal_type = :mealType
        OR (:mealType = 'snack' AND meal_entries.meal_type = 'snacks')
        OR (:mealType = 'snacks' AND meal_entries.meal_type = 'snack')
      )
    ORDER BY meal_entries.id ASC
    """
  )
  fun getEntriesWithFoodForDateAndMealType(date: String, mealType: String): Flow<List<MealEntryWithFood>>

  @Query(
    """
    SELECT
      date,
      COALESCE(SUM(calories), 0.0) AS totalCalories,
      COALESCE(SUM(protein), 0.0) AS totalProtein,
      COALESCE(SUM(carbs), 0.0) AS totalCarbs,
      COALESCE(SUM(fat), 0.0) AS totalFat
    FROM meal_entries
    GROUP BY date
    ORDER BY date DESC
    """
  )
  fun observeAllDailyTotals(): Flow<List<DailySummary>>

  @Query("SELECT DISTINCT date FROM meal_entries ORDER BY date DESC")
  fun getDatesWithEntries(): Flow<List<String>>
}

data class DailySummary(
  val date: String,
  val totalCalories: Double,
  val totalProtein: Double,
  val totalCarbs: Double,
  val totalFat: Double,
)

data class MealEntryWithFood(
  val id: Long,
  val date: String,
  val mealType: String,
  val foodId: Long,
  val quantityGrams: Double,
  val calories: Double,
  val protein: Double,
  val carbs: Double,
  val fat: Double,
  val foodName: String,
)
