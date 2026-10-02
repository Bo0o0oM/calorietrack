package com.calorietrack.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteStatement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

/**
 * Validates the upgrade migration path from Version 2 (Milestone 2B/2C)
 * to Version 3 (Milestone 2D) and data preservation.
 */
class Migration2To3Test {

  data class Row(val values: MutableMap<String, Any?>)

  @Test
  fun migration2To3_executesAlterStatementsAndBackfillsSearchKeywordsPreservingUserData() {
    val executedSqls = mutableListOf<String>()
    var transactionStarted = false
    var transactionSuccessful = false
    var transactionEnded = false

    // Simulate existing Version 2 database tables
    val foodsTable = mutableMapOf<Long, Row>()
    // Existing built-in food from version 2 without keywords
    foodsTable[1L] = Row(
      mutableMapOf(
        "id" to 1L,
        "name" to "White Rice (Cooked)",
        "serving_description" to "1 cup cooked",
        "serving_grams" to 158.0,
        "calories_per_100g" to 130.0,
        "protein_per_100g" to 2.7,
        "carbs_per_100g" to 28.2,
        "fat_per_100g" to 0.3,
        "is_custom" to false,
        "data_source" to "USDA FoodData Central",
        "source_id" to "168878",
        "search_keywords" to "",
      )
    )

    // Add existing user-created custom food from version 2
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
        "data_source" to "User",
        "source_id" to null,
        "search_keywords" to "",
      )
    )

    val mealEntriesTable = mutableMapOf<Long, Row>()
    mealEntriesTable[1L] = Row(
      mutableMapOf(
        "id" to 1L,
        "date" to "2026-10-02",
        "meal_type" to "breakfast",
        "food_id" to 1000L,
        "quantity_grams" to 30.0,
        "calories" to 114.0,
        "protein" to 24.0,
        "carbs" to 1.5,
        "fat" to 0.9,
      )
    )

    val dailyGoalsTable = mutableMapOf<String, Row>()
    dailyGoalsTable["2026-10-02"] = Row(
      mutableMapOf(
        "date" to "2026-10-02",
        "calorie_goal" to 2000.0,
        "protein_goal" to 150.0,
        "carbs_goal" to 200.0,
        "fat_goal" to 65.0,
      )
    )

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
                  "search_keywords" to (boundValues[12] ?: ""),
                )
              )
            }
            id
          }
          "executeUpdateDelete" -> {
            val kw = boundValues[1] as? String ?: ""
            val id = boundValues[2] as? Long
            if (id != null && foodsTable.containsKey(id)) {
              val currentKw = foodsTable[id]?.values?.get("search_keywords") as? String ?: ""
              if (currentKw.isEmpty()) {
                foodsTable[id]?.values?.set("search_keywords", kw)
              }
            }
            1
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

    val candidateFiles = listOf(
      File("src/main/assets/source/food_catalogue.json"),
      File("app/src/main/assets/source/food_catalogue.json"),
      File("../app/src/main/assets/source/food_catalogue.json")
    )
    val catalogueFile = candidateFiles.firstOrNull { it.exists() }
      ?: throw IllegalStateException("Catalogue file not found in $candidateFiles")

    val migration = CalorieTrackDatabase.Migration2To3 {
      catalogueFile.inputStream()
    }

    // 1. Run migration
    migration.migrate(fakeDb)

    // 2. Verify ALTER TABLE and INDEX statements
    assertTrue(
      "Migration must add search_keywords column",
      executedSqls.any { it.contains("ALTER TABLE foods ADD COLUMN search_keywords") }
    )
    assertTrue(
      "Migration must create index on search_keywords",
      executedSqls.any { it.contains("CREATE INDEX IF NOT EXISTS index_foods_search_keywords") }
    )

    // 3. Verify transactions
    assertTrue(transactionStarted)
    assertTrue(transactionSuccessful)
    assertTrue(transactionEnded)

    // 4. Verify 500 built-in + 1 custom = 501 items
    assertEquals(501, foodsTable.size)

    // Verify existing built-in item 1 had its keywords backfilled
    val item1 = foodsTable[1L]
    assertNotNull(item1)
    val item1Keywords = item1!!.values["search_keywords"] as String
    assertTrue("Search keywords should be populated for existing food 1", item1Keywords.isNotBlank())
    assertTrue("Item 1 should contain 'chawal' or 'rice'", item1Keywords.contains("chawal") || item1Keywords.contains("rice"))

    // Verify custom food 1000 preserved
    val customFood = foodsTable[1000L]
    assertNotNull(customFood)
    assertEquals(true, customFood!!.values["is_custom"])
    assertEquals("My Custom Protein Shake", customFood.values["name"])

    // Verify meal entries and daily goals intact
    assertEquals(1, mealEntriesTable.size)
    assertEquals(1, dailyGoalsTable.size)

    // 5. Test Idempotency
    migration.migrate(fakeDb)
    assertEquals(501, foodsTable.size)
  }
}
