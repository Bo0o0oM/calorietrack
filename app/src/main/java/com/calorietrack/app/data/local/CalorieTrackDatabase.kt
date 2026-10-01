package com.calorietrack.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    FoodEntity::class,
    MealEntryEntity::class,
    DailyGoalEntity::class,
  ],
  version = 1,
  exportSchema = false,
)
abstract class CalorieTrackDatabase : RoomDatabase() {

  abstract fun foodDao(): FoodDao
  abstract fun mealEntryDao(): MealEntryDao
  abstract fun dailyGoalDao(): DailyGoalDao

  companion object {
    private const val DATABASE_NAME = "calorietrack.db"

    @Volatile
    private var INSTANCE: CalorieTrackDatabase? = null

    fun getInstance(context: Context): CalorieTrackDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          CalorieTrackDatabase::class.java,
          DATABASE_NAME,
        ).build().also { INSTANCE = it }
      }
    }
  }
}
