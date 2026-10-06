package com.calorietrack.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

/**
 * Validates the upgrade migration path from Version 4 (Milestone 2I)
 * to Version 5 (Milestone 2J) for Recipes, Recipe Ingredients, and Meal Entry extension.
 */
class Migration4To5Test {

  @Test
  fun migration4To5_executesAllRequiredStatements() {
    val executedSqls = mutableListOf<String>()

    val dbHandler = InvocationHandler { _, method, args ->
      when (method.name) {
        "execSQL" -> {
          executedSqls.add(args[0] as String)
          null
        }
        else -> null
      }
    }

    val fakeDb = Proxy.newProxyInstance(
      SupportSQLiteDatabase::class.java.classLoader,
      arrayOf(SupportSQLiteDatabase::class.java),
      dbHandler
    ) as SupportSQLiteDatabase

    val migration = CalorieTrackDatabase.Migration4To5()
    migration.migrate(fakeDb)

    assertTrue(
      "Migration must create recipes table",
      executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS recipes") }
    )
    assertTrue(
      "Migration must create index on recipes(name)",
      executedSqls.any { it.contains("CREATE INDEX IF NOT EXISTS index_recipes_name ON recipes") }
    )
    assertTrue(
      "Migration must create recipe_ingredients table",
      executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS recipe_ingredients") }
    )
    assertTrue(
      "Migration must add recipe_id column to meal_entries",
      executedSqls.any { it.contains("ALTER TABLE meal_entries ADD COLUMN recipe_id") }
    )
    assertTrue(
      "Migration must add entry_name column to meal_entries",
      executedSqls.any { it.contains("ALTER TABLE meal_entries ADD COLUMN entry_name") }
    )
    assertTrue(
      "Migration must index recipe_id in meal_entries",
      executedSqls.any { it.contains("CREATE INDEX IF NOT EXISTS index_meal_entries_recipe_id") }
    )
  }
}
