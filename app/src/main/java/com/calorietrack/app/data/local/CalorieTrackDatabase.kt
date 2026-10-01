package com.calorietrack.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.InputStream

@Database(
  entities = [
    FoodEntity::class,
    MealEntryEntity::class,
    DailyGoalEntity::class,
  ],
  version = 2,
  exportSchema = true,
)
abstract class CalorieTrackDatabase : RoomDatabase() {

  abstract fun foodDao(): FoodDao
  abstract fun mealEntryDao(): MealEntryDao
  abstract fun dailyGoalDao(): DailyGoalDao

  /**
   * Room migration from schema version 1 to 2.
   *
   * Adds the data_source and source_id columns to the foods table,
   * then seeds the 104 built-in catalogue foods idempotently.
   */
  class Migration1To2(
    private val streamProvider: () -> InputStream
  ) : Migration(1, 2) {

    constructor(context: Context) : this({
      context.applicationContext.assets.open("source/food_catalogue.json")
    })

    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL(
        "ALTER TABLE foods ADD COLUMN data_source TEXT NOT NULL DEFAULT 'USDA FoodData Central'"
      )
      db.execSQL(
        "ALTER TABLE foods ADD COLUMN source_id TEXT DEFAULT NULL"
      )
      streamProvider().use { stream ->
        FoodCatalogueSeeder.seedFromStream(db, stream)
      }
    }
  }

  companion object {
    private const val DATABASE_NAME = "calorietrack.db"

    @Volatile
    private var INSTANCE: CalorieTrackDatabase? = null

    fun createMigration1To2(context: Context): Migration = Migration1To2(context)

    fun createMigration1To2(streamProvider: () -> InputStream): Migration = Migration1To2(streamProvider)

    fun getInstance(context: Context): CalorieTrackDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          CalorieTrackDatabase::class.java,
          DATABASE_NAME,
        )
        .createFromAsset("database/calorietrack.db")
        .addMigrations(Migration1To2(context.applicationContext))
        .build().also { INSTANCE = it }
      }
    }
  }
}
