package com.calorietrack.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyGoalDao {

  @Query("SELECT * FROM daily_goals WHERE date = :date LIMIT 1")
  suspend fun getForDate(date: String): DailyGoalEntity?

  @Query("SELECT * FROM daily_goals WHERE date = :date LIMIT 1")
  fun observeForDate(date: String): Flow<DailyGoalEntity?>

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insert(goal: DailyGoalEntity): Long

  @Update
  suspend fun update(goal: DailyGoalEntity)

  @Upsert
  suspend fun upsert(goal: DailyGoalEntity)

  @Query("SELECT * FROM daily_goals ORDER BY date DESC")
  fun getAllGoals(): Flow<List<DailyGoalEntity>>

  @Query("SELECT DISTINCT date FROM daily_goals ORDER BY date DESC")
  fun getDatesWithGoals(): Flow<List<String>>
}
