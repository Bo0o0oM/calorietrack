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
    RecipeEntity::class,
    RecipeIngredientEntity::class,
  ],
  version = 5,
  exportSchema = true,
)
abstract class CalorieTrackDatabase : RoomDatabase() {

  abstract fun foodDao(): FoodDao
  abstract fun mealEntryDao(): MealEntryDao
  abstract fun dailyGoalDao(): DailyGoalDao
  abstract fun recipeDao(): RecipeDao

  /**
   * Room migration from schema version 1 to 2.
   *
   * Adds the data_source and source_id columns to the foods table,
   * then seeds built-in catalogue foods idempotently.
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
        FoodCatalogueSeeder.seedFromStream(db, stream, hasSearchKeywords = false)
      }
    }
  }

  /**
   * Room migration from schema version 2 to 3.
   *
   * Adds the search_keywords column and index to the foods table,
   * then updates and seeds the expanded built-in catalogue foods idempotently.
   */
  class Migration2To3(
    private val streamProvider: () -> InputStream
  ) : Migration(2, 3) {

    constructor(context: Context) : this({
      context.applicationContext.assets.open("source/food_catalogue.json")
    })

    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL(
        "ALTER TABLE foods ADD COLUMN search_keywords TEXT NOT NULL DEFAULT ''"
      )
      db.execSQL(
        "CREATE INDEX IF NOT EXISTS index_foods_search_keywords ON foods (search_keywords)"
      )
      streamProvider().use { stream ->
        FoodCatalogueSeeder.seedFromStream(db, stream, hasSearchKeywords = true)
      }
    }
  }

  /**
   * Room migration from schema version 3 to 4.
   *
   * Adds the is_active column to the foods table to support soft-deletion / archiving
   * of custom foods while preserving historical meal entry join integrity.
   */
  class Migration3To4 : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL(
        "ALTER TABLE foods ADD COLUMN is_active INTEGER NOT NULL DEFAULT 1"
      )
    }
  }

  /**
   * Room migration from schema version 4 to 5.
   *
   * Adds recipes and recipe_ingredients tables, and alters meal_entries to add
   * recipe_id and entry_name columns to support custom meal / recipe creation and logging.
   */
  class Migration4To5 : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS recipes (
          id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
          name TEXT NOT NULL,
          cooked_weight_grams REAL NOT NULL,
          total_calories REAL NOT NULL,
          total_protein REAL NOT NULL,
          total_carbs REAL NOT NULL,
          total_fat REAL NOT NULL,
          calories_per_100g REAL NOT NULL,
          protein_per_100g REAL NOT NULL,
          carbs_per_100g REAL NOT NULL,
          fat_per_100g REAL NOT NULL,
          is_active INTEGER NOT NULL DEFAULT 1,
          search_keywords TEXT NOT NULL DEFAULT ''
        )
        """.trimIndent()
      )
      db.execSQL("CREATE INDEX IF NOT EXISTS index_recipes_name ON recipes (name)")
      db.execSQL("CREATE INDEX IF NOT EXISTS index_recipes_search_keywords ON recipes (search_keywords)")

      db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS recipe_ingredients (
          id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
          recipe_id INTEGER NOT NULL,
          food_id INTEGER NOT NULL,
          quantity_grams REAL NOT NULL,
          FOREIGN KEY(recipe_id) REFERENCES recipes(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent()
      )
      db.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_ingredients_recipe_id ON recipe_ingredients (recipe_id)")
      db.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_ingredients_food_id ON recipe_ingredients (food_id)")

      db.execSQL("ALTER TABLE meal_entries ADD COLUMN recipe_id INTEGER DEFAULT NULL")
      db.execSQL("ALTER TABLE meal_entries ADD COLUMN entry_name TEXT DEFAULT NULL")
      db.execSQL("CREATE INDEX IF NOT EXISTS index_meal_entries_recipe_id ON meal_entries (recipe_id)")
    }
  }

  companion object {
    private const val DATABASE_NAME = "calorietrack.db"

    @Volatile
    private var INSTANCE: CalorieTrackDatabase? = null

    fun createMigration1To2(context: Context): Migration = Migration1To2(context)
    fun createMigration1To2(streamProvider: () -> InputStream): Migration = Migration1To2(streamProvider)

    fun createMigration2To3(context: Context): Migration = Migration2To3(context)
    fun createMigration2To3(streamProvider: () -> InputStream): Migration = Migration2To3(streamProvider)

    fun createMigration3To4(): Migration = Migration3To4()

    fun createMigration4To5(): Migration = Migration4To5()

    fun getInstance(context: Context): CalorieTrackDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          CalorieTrackDatabase::class.java,
          DATABASE_NAME,
        )
        .createFromAsset("database/calorietrack.db")
        .addMigrations(
          Migration1To2(context.applicationContext),
          Migration2To3(context.applicationContext),
          Migration3To4(),
          Migration4To5(),
        )
        .build().also { INSTANCE = it }
      }
    }
  }
}
