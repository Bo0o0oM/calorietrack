package com.calorietrack.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Validates the integrity and quality of the offline food catalogue dataset
 * and the prepackaged database asset.
 */
class FoodCatalogueValidationTest {

  private lateinit var items: List<FoodEntity>
  private lateinit var catalogueFile: File
  private lateinit var databaseFile: File

  @Before
  fun setUp() {
    val possibleCataloguePaths = listOf(
      File("src/main/assets/source/food_catalogue.json"),
      File("app/src/main/assets/source/food_catalogue.json"),
      File("../app/src/main/assets/source/food_catalogue.json")
    )
    catalogueFile = possibleCataloguePaths.firstOrNull { it.exists() }
      ?: throw IllegalStateException("food_catalogue.json not found in candidate paths: $possibleCataloguePaths")

    val possibleDbPaths = listOf(
      File("src/main/assets/database/calorietrack.db"),
      File("app/src/main/assets/database/calorietrack.db"),
      File("../app/src/main/assets/database/calorietrack.db")
    )
    databaseFile = possibleDbPaths.firstOrNull { it.exists() }
      ?: throw IllegalStateException("calorietrack.db not found in candidate paths: $possibleDbPaths")

    items = FoodCatalogueSeeder.parseCatalogue(catalogueFile.inputStream())
  }

  @Test
  fun catalogueFile_existsAndIsNotEmpty() {
    assertTrue(catalogueFile.exists())
    assertTrue(catalogueFile.length() > 0)
  }

  @Test
  fun prepackagedDatabaseAsset_existsAndIsNotEmpty() {
    assertTrue(databaseFile.exists())
    assertTrue(databaseFile.length() > 0)
  }

  @Test
  fun foodCatalogue_containsAtLeast100Items() {
    assertTrue("Catalogue should have at least 100 items, but had ${items.size}", items.size >= 100)
    assertEquals(104, items.size)
  }

  @Test
  fun foodCatalogue_allIdsAreUniqueAndWithinBuiltInRange() {
    val ids = items.map { it.id }
    assertEquals("Duplicate IDs detected", ids.size, ids.distinct().size)
    items.forEach { item ->
      assertTrue("ID must be positive, got ${item.id}", item.id > 0)
      assertTrue(
        "Built-in ID ${item.id} exceeds reserved range (max ${FoodEntity.BUILT_IN_MAX_ID})",
        item.id <= FoodEntity.BUILT_IN_MAX_ID
      )
    }
  }

  @Test
  fun foodCatalogue_allNamesAreUniqueAndNonBlank() {
    val names = items.map { it.name.trim().lowercase() }
    assertEquals("Duplicate names detected in catalogue", names.size, names.distinct().size)
    items.forEach { item ->
      assertTrue("Name must not be blank for ID ${item.id}", item.name.isNotBlank())
    }
  }

  @Test
  fun foodCatalogue_servingDescriptionsAreValid() {
    items.forEach { item ->
      assertTrue("Serving description must not be blank for ID ${item.id}", item.servingDescription.isNotBlank())
      assertTrue("Serving grams must be greater than zero for ID ${item.id}", item.servingGrams > 0.0)
    }
  }

  @Test
  fun foodCatalogue_macroNutrientsAreNonNegativeAndPlausible() {
    items.forEach { item ->
      assertTrue("Calories must be >= 0 for ID ${item.id}", item.caloriesPer100g >= 0.0)
      assertTrue("Protein must be >= 0 for ID ${item.id}", item.proteinPer100g >= 0.0)
      assertTrue("Carbs must be >= 0 for ID ${item.id}", item.carbsPer100g >= 0.0)
      assertTrue("Fat must be >= 0 for ID ${item.id}", item.fatPer100g >= 0.0)

      // 100g of pure fat is 900 kcal; any food > 950 kcal/100g is suspect
      assertTrue("Calories per 100g unrealistic (> 950) for ${item.name}", item.caloriesPer100g <= 950.0)
      // Total macro grams cannot realistically exceed 100g per 100g
      val totalMacros = item.proteinPer100g + item.carbsPer100g + item.fatPer100g
      assertTrue("Sum of macros per 100g cannot exceed 100g for ${item.name} ($totalMacros g)", totalMacros <= 100.5)
    }
  }

  @Test
  fun foodCatalogue_metadataAndProvenanceAreValid() {
    val sourceIds = items.mapNotNull { it.sourceId }
    assertEquals("Duplicate sourceIds detected in catalogue", sourceIds.size, sourceIds.distinct().size)

    items.forEach { item ->
      assertFalse("Built-in items must not be custom (ID ${item.id})", item.isCustom)
      assertEquals(FoodEntity.SOURCE_USDA, item.dataSource)
      assertNotNull("Source ID should be present for USDA items (ID ${item.id})", item.sourceId)
      assertTrue("Source ID should not be blank for ID ${item.id}", item.sourceId!!.isNotBlank())
    }
  }

  @Test
  fun customFood_supportsUserDataSourceAndIdRange() {
    val customFood = FoodEntity(
      id = FoodEntity.CUSTOM_MIN_ID,
      name = "Homemade Protein Pancake",
      servingDescription = "1 pancake (80g)",
      servingGrams = 80.0,
      caloriesPer100g = 210.0,
      proteinPer100g = 15.0,
      carbsPer100g = 25.0,
      fatPer100g = 5.0,
      isCustom = true,
      dataSource = FoodEntity.SOURCE_USER,
      sourceId = null
    )

    assertTrue(customFood.isCustom)
    assertEquals(FoodEntity.SOURCE_USER, customFood.dataSource)
    assertTrue("Custom food ID must be >= ${FoodEntity.CUSTOM_MIN_ID}", customFood.id >= FoodEntity.CUSTOM_MIN_ID)
  }
}
