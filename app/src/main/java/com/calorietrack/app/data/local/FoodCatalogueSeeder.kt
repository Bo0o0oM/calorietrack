package com.calorietrack.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Seeder and parser for built-in food items.
 *
 * Guarantees idempotent insertion of the offline food catalogue into SQLite.
 * Uses INSERT OR IGNORE to ensure that existing user foods or previously seeded items
 * are never duplicated or overwritten.
 */
object FoodCatalogueSeeder {

  const val BUILT_IN_MAX_ID = 999L
  const val CUSTOM_MIN_ID = 1000L
  const val DEFAULT_DATA_SOURCE_BUILT_IN = "USDA FoodData Central"
  const val DEFAULT_DATA_SOURCE_CUSTOM = "User"

  /**
   * Parses the JSON catalogue stream into a list of [FoodEntity].
   */
  fun parseCatalogue(stream: InputStream): List<FoodEntity> {
    val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
    val json = reader.readText()
    return parseCatalogueJson(json)
  }

  /**
   * Pure Kotlin JSON parser tailored for the flat structure of food_catalogue.json.
   * Avoids platform-specific JSON library stubs in unit tests.
   */
  fun parseCatalogueJson(json: String): List<FoodEntity> {
    val list = mutableListOf<FoodEntity>()
    val objectRegex = Regex("""\{([^{}]+)\}""")
    val propRegex = Regex(""""([^"]+)"\s*:\s*("(?:\\.|[^"\\])*"|[^,\n}]+)""")

    for (match in objectRegex.findAll(json)) {
      val body = match.groupValues[1]
      val pairs = mutableMapOf<String, String>()
      for (propMatch in propRegex.findAll(body)) {
        val key = propMatch.groupValues[1].trim()
        var value = propMatch.groupValues[2].trim()
        if (value.startsWith("\"") && value.endsWith("\"")) {
          value = value.substring(1, value.length - 1).replace("\\\"", "\"")
        }
        pairs[key] = value
      }

      val id = pairs["id"]?.toLongOrNull() ?: continue
      val name = pairs["name"] ?: continue
      val servingDescription = pairs["servingDescription"] ?: ""
      val servingGrams = pairs["servingGrams"]?.toDoubleOrNull() ?: 0.0
      val calories = pairs["caloriesPer100g"]?.toDoubleOrNull() ?: 0.0
      val protein = pairs["proteinPer100g"]?.toDoubleOrNull() ?: 0.0
      val carbs = pairs["carbsPer100g"]?.toDoubleOrNull() ?: 0.0
      val fat = pairs["fatPer100g"]?.toDoubleOrNull() ?: 0.0
      val isCustom = pairs["isCustom"]?.toBooleanStrictOrNull() ?: false
      val dataSource = pairs["dataSource"] ?: DEFAULT_DATA_SOURCE_BUILT_IN
      val sourceId = pairs["sourceId"]?.takeIf { it != "null" }
      val searchKeywords = pairs["searchKeywords"] ?: ""

      list.add(
        FoodEntity(
          id = id,
          name = name,
          servingDescription = servingDescription,
          servingGrams = servingGrams,
          caloriesPer100g = calories,
          proteinPer100g = protein,
          carbsPer100g = carbs,
          fatPer100g = fat,
          isCustom = isCustom,
          dataSource = dataSource,
          sourceId = sourceId,
          searchKeywords = searchKeywords,
        )
      )
    }
    return list
  }

  /**
   * Seeds the database with built-in catalogue items using INSERT OR IGNORE.
   * Also ensures the sqlite_sequence counter starts at 999 so user custom foods
   * auto-increment starting at 1000.
   */
  fun seedFromStream(
    db: SupportSQLiteDatabase,
    stream: InputStream,
    hasSearchKeywords: Boolean = true
  ): Int {
    val foods = parseCatalogue(stream)
    return seedFoods(db, foods, hasSearchKeywords)
  }

  /**
   * Seeds the database with the given food items idempotently within a transaction.
   * Returns the count of items attempted to seed.
   */
  fun seedFoods(
    db: SupportSQLiteDatabase,
    foods: List<FoodEntity>,
    hasSearchKeywords: Boolean = true
  ): Int {
    db.beginTransaction()
    try {
      val insertSql = if (hasSearchKeywords) {
        """
        INSERT OR IGNORE INTO foods (
          id, name, serving_description, serving_grams,
          calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g,
          is_custom, data_source, source_id, search_keywords
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
      } else {
        """
        INSERT OR IGNORE INTO foods (
          id, name, serving_description, serving_grams,
          calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g,
          is_custom, data_source, source_id
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
      }

      val insertStmt = db.compileStatement(insertSql)

      val updateStmt = if (hasSearchKeywords) {
        db.compileStatement(
          """
          UPDATE foods SET search_keywords = ?
          WHERE id = ? AND (search_keywords IS NULL OR search_keywords = '')
          """.trimIndent()
        )
      } else {
        null
      }

      for (food in foods) {
        insertStmt.bindLong(1, food.id)
        insertStmt.bindString(2, food.name)
        insertStmt.bindString(3, food.servingDescription)
        insertStmt.bindDouble(4, food.servingGrams)
        insertStmt.bindDouble(5, food.caloriesPer100g)
        insertStmt.bindDouble(6, food.proteinPer100g)
        insertStmt.bindDouble(7, food.carbsPer100g)
        insertStmt.bindDouble(8, food.fatPer100g)
        insertStmt.bindLong(9, if (food.isCustom) 1L else 0L)
        insertStmt.bindString(10, food.dataSource)
        if (food.sourceId != null) {
          insertStmt.bindString(11, food.sourceId)
        } else {
          insertStmt.bindNull(11)
        }
        if (hasSearchKeywords) {
          insertStmt.bindString(12, food.searchKeywords)
        }
        insertStmt.executeInsert()

        // Populate search_keywords for pre-existing records if they were blank
        if (hasSearchKeywords && updateStmt != null && food.searchKeywords.isNotEmpty()) {
          updateStmt.bindString(1, food.searchKeywords)
          updateStmt.bindLong(2, food.id)
          updateStmt.executeUpdateDelete()
        }
      }

      // Ensure sqlite_sequence for 'foods' is at least 999 so auto-increment IDs for custom foods start at 1000
      db.execSQL(
        """
        INSERT OR REPLACE INTO sqlite_sequence (name, seq)
        SELECT 'foods', 999
        WHERE NOT EXISTS (SELECT 1 FROM sqlite_sequence WHERE name = 'foods')
           OR (SELECT seq FROM sqlite_sequence WHERE name = 'foods') < 999
        """.trimIndent()
      )

      db.setTransactionSuccessful()
      return foods.size
    } finally {
      db.endTransaction()
    }
  }
}
