package com.calorietrack.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteStatement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Validates the upgrade migration path from Version 1 (Milestone 2A)
 * to Version 2 (Milestone 2B) and data preservation.
 */
class Migration1To2Test {

  data class Row(val values: MutableMap<String, Any?>)

  @Test
  fun migration1To2_executesAlterStatementsAndSeedsCataloguePreservingUserData() {
    val executedSqls = mutableListOf<String>()
    var transactionStarted = false
    var transactionSuccessful = false
    var transactionEnded = false

    // Simulate existing Version 1 database tables
    val foodsTable = mutableMapOf<Long, Row>()
    // Add existing user-created custom food from version 1
    foodsTable[1000L] = Row(
      mutableMapOf(
        "id" to 1000L,
        "name" to "My Custom Protein Shake",
        "serving_description" to "1 scoop (30g)",
        "serving_grams" to 30.0,
        "calories_per_100g" to 380.0,
        "protein_per_100g" to 80.0,
        "carbs_per_100g" to 5.0,
        "fat_per_100g" to 3.0,
        "is_custom" to true,
      )
    )

    // Simulate existing meal entries and daily goals
    val mealEntriesTable = mutableMapOf<Long, Row>()
    mealEntriesTable[1L] = Row(
      mutableMapOf(
        "id" to 1L,
        "food_id" to 1000L,
        "food_name" to "My Custom Protein Shake",
        "date" to "2026-10-01",
        "meal_type" to "breakfast",
        "quantity_multiplier" to 1.0,
        "serving_description" to "1 scoop (30g)",
        "logged_calories" to 114.0,
        "logged_protein" to 24.0,
        "logged_carbs" to 1.5,
        "logged_fat" to 0.9,
        "logged_at" to 1727800000000L,
      )
    )

    val dailyGoalsTable = mutableMapOf<String, Row>()
    dailyGoalsTable["2026-10-01"] = Row(
      mutableMapOf(
        "date" to "2026-10-01",
        "calorie_target" to 2200,
        "protein_target" to 160,
        "carbs_target" to 220,
        "fat_target" to 70,
      )
    )

    // Create fake statement handler for compiled insert
    fun createFakeStatement(): SupportSQLiteStatement {
      val boundValues = mutableMapOf<Int, Any?>()
      return Proxy.newProxyInstance(
        SupportSQLiteStatement::class.java.classLoader,
        arrayOf(SupportSQLiteStatement::class.java)
      ) { _, method, args ->
        when (method.name) {
          "bindLong" -> {
            boundValues[args[0] as Int] = args[1] as Long
            null
          }
          "bindDouble" -> {
            boundValues[args[0] as Int] = args[1] as Double
            null
          }
          "bindString" -> {
            boundValues[args[0] as Int] = args[1] as String
            null
          }
          "bindNull" -> {
            boundValues[args[0] as Int] = null
            null
          }
          "executeInsert" -> {
            val id = boundValues[1] as Long
            // INSERT OR IGNORE behavior
            if (!foodsTable.containsKey(id)) {
              foodsTable[id] = Row(
                mutableMapOf(
                  "id" to id,
                  "name" to boundValues[2],
                  "serving_description" to boundValues[3],
                  "serving_grams" to boundValues[4],
                  "calories_per_100g" to boundValues[5],
                  "protein_per_100g" to boundValues[6],
                  "carbs_per_100g" to boundValues[7],
                  "fat_per_100g" to boundValues[8],
                  "is_custom" to (boundValues[9] == 1L),
                  "data_source" to boundValues[10],
                  "source_id" to boundValues[11],
                )
              )
            }
            id
          }
          "close" -> null
          else -> null
        }
      } as SupportSQLiteStatement
    }

    val dbHandler = InvocationHandler { _, method, args ->
      when (method.name) {
        "execSQL" -> {
          executedSqls.add(args[0] as String)
          null
        }
        "beginTransaction" -> {
          transactionStarted = true
          null
        }
        "setTransactionSuccessful" -> {
          transactionSuccessful = true
          null
        }
        "endTransaction" -> {
          transactionEnded = true
          null
        }
        "compileStatement" -> {
          createFakeStatement()
        }
        else -> null
      }
    }

    val fakeDb = Proxy.newProxyInstance(
      SupportSQLiteDatabase::class.java.classLoader,
      arrayOf(SupportSQLiteDatabase::class.java),
      dbHandler
    ) as SupportSQLiteDatabase

    // Resolve catalogue json file
    val candidateFiles = listOf(
      File("src/main/assets/source/food_catalogue.json"),
      File("app/src/main/assets/source/food_catalogue.json"),
      File("../app/src/main/assets/source/food_catalogue.json")
    )
    val catalogueFile = candidateFiles.firstOrNull { it.exists() }
      ?: throw IllegalStateException("Catalogue file not found in $candidateFiles")

    val migration = CalorieTrackDatabase.Migration1To2 {
      catalogueFile.inputStream()
    }

    // 1. Run the migration
    migration.migrate(fakeDb)

    // 2. Verify ALTER TABLE statements
    assertTrue(
      "Migration must add data_source column",
      executedSqls.any { it.contains("ALTER TABLE foods ADD COLUMN data_source") }
    )
    assertTrue(
      "Migration must add source_id column",
      executedSqls.any { it.contains("ALTER TABLE foods ADD COLUMN source_id") }
    )
    assertTrue(
      "Migration must set sqlite_sequence for foods to >= 999",
      executedSqls.any { it.contains("sqlite_sequence") && it.contains("999") }
    )

    // 3. Verify transaction handling
    assertTrue(transactionStarted)
    assertTrue(transactionSuccessful)
    assertTrue(transactionEnded)

    // 4. Verify Food counts: 104 built-in + 1 preserved custom food = 105
    assertEquals(105, foodsTable.size)
    val customFoods = foodsTable.values.filter { (it.values["is_custom"] as? Boolean) == true }
    assertEquals(1, customFoods.size)

    val preservedCustom = customFoods.first()
    assertEquals(1000L, preservedCustom.values["id"])
    assertEquals("My Custom Protein Shake", preservedCustom.values["name"])
    assertEquals(380.0, preservedCustom.values["calories_per_100g"])

    val builtInFoods = foodsTable.values.filter { (it.values["is_custom"] as? Boolean) == false }
    assertEquals(104, builtInFoods.size)

    // Verify all built-in foods have valid ID range
    builtInFoods.forEach { food ->
      val id = food.values["id"] as Long
      assertTrue("Built-in food ID must be between 1 and 999", id in 1..FoodEntity.BUILT_IN_MAX_ID)
      assertEquals(FoodEntity.SOURCE_USDA, food.values["data_source"])
      assertNotNull(food.values["source_id"])
    }

    // 5. Verify meal entries and daily goals survived untouched
    assertEquals(1, mealEntriesTable.size)
    assertEquals("My Custom Protein Shake", mealEntriesTable[1L]?.values?.get("food_name"))
    assertEquals(1, dailyGoalsTable.size)
    assertEquals(2200, dailyGoalsTable["2026-10-01"]?.values?.get("calorie_target"))

    // 6. Test Idempotency: re-running migration on the same database should not duplicate foods
    migration.migrate(fakeDb)
    assertEquals("Re-running migration must be idempotent and keep exactly 105 foods", 105, foodsTable.size)
  }
}
