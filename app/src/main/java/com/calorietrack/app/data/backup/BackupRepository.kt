package com.calorietrack.app.data.backup

import androidx.room.withTransaction
import com.calorietrack.app.data.local.CalorieTrackDatabase
import com.calorietrack.app.data.local.DailyGoalDao
import com.calorietrack.app.data.local.DailyGoalEntity
import com.calorietrack.app.data.local.FoodDao
import com.calorietrack.app.data.local.FoodEntity
import com.calorietrack.app.data.local.MealEntryDao
import com.calorietrack.app.data.local.MealEntryEntity
import com.calorietrack.app.data.local.RecipeDao
import com.calorietrack.app.data.local.RecipeEntity
import com.calorietrack.app.data.local.RecipeIngredientEntity
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlinx.serialization.json.Json

class BackupRepository(
  private val foodDao: FoodDao,
  private val recipeDao: RecipeDao,
  private val mealEntryDao: MealEntryDao,
  private val dailyGoalDao: DailyGoalDao,
  private val transactionRunner: suspend (suspend () -> Unit) -> Unit,
  private val json: Json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
  },
  private val timestampProvider: () -> String = { DateTimeFormatter.ISO_INSTANT.format(Instant.now()) },
) {

  constructor(
    database: CalorieTrackDatabase,
    foodDao: FoodDao,
    recipeDao: RecipeDao,
    mealEntryDao: MealEntryDao,
    dailyGoalDao: DailyGoalDao,
    json: Json = Json {
      prettyPrint = true
      ignoreUnknownKeys = true
      encodeDefaults = true
    },
    timestampProvider: () -> String = { DateTimeFormatter.ISO_INSTANT.format(Instant.now()) },
  ) : this(
    foodDao = foodDao,
    recipeDao = recipeDao,
    mealEntryDao = mealEntryDao,
    dailyGoalDao = dailyGoalDao,
    transactionRunner = { block -> database.withTransaction { block() } },
    json = json,
    timestampProvider = timestampProvider,
  )

  /**
   * Serializes current user-owned data into a versioned CalorieTrackBackup instance.
   * Built-in food catalogue (IDs 1..999) is strictly excluded.
   */
  suspend fun createBackup(): CalorieTrackBackup {
    val customFoods = foodDao.getAllCustomFoods().map { entity ->
      BackupCustomFood(
        id = entity.id,
        name = entity.name,
        servingDescription = entity.servingDescription,
        servingGrams = entity.servingGrams,
        caloriesPer100g = entity.caloriesPer100g,
        proteinPer100g = entity.proteinPer100g,
        carbsPer100g = entity.carbsPer100g,
        fatPer100g = entity.fatPer100g,
        isActive = entity.isActive,
        dataSource = entity.dataSource,
        sourceId = entity.sourceId,
        searchKeywords = entity.searchKeywords,
      )
    }

    val recipes = recipeDao.getAllRecipes().map { entity ->
      BackupRecipe(
        id = entity.id,
        name = entity.name,
        cookedWeightGrams = entity.cookedWeightGrams,
        totalCalories = entity.totalCalories,
        totalProtein = entity.totalProtein,
        totalCarbs = entity.totalCarbs,
        totalFat = entity.totalFat,
        caloriesPer100g = entity.caloriesPer100g,
        proteinPer100g = entity.proteinPer100g,
        carbsPer100g = entity.carbsPer100g,
        fatPer100g = entity.fatPer100g,
        isActive = entity.isActive,
        searchKeywords = entity.searchKeywords,
      )
    }

    val recipeIngredients = recipeDao.getAllRecipeIngredients().map { entity ->
      BackupRecipeIngredient(
        id = entity.id,
        recipeId = entity.recipeId,
        foodId = entity.foodId,
        quantityGrams = entity.quantityGrams,
      )
    }

    val mealEntries = mealEntryDao.getAllMealEntries().map { entity ->
      BackupMealEntry(
        id = entity.id,
        date = entity.date,
        mealType = entity.mealType,
        foodId = entity.foodId,
        recipeId = entity.recipeId,
        entryName = entity.entryName,
        quantityGrams = entity.quantityGrams,
        calories = entity.calories,
        protein = entity.protein,
        carbs = entity.carbs,
        fat = entity.fat,
      )
    }

    val dailyGoals = dailyGoalDao.getAllDailyGoals().mapIndexed { index, entity ->
      BackupDailyGoal(
        id = (index + 1).toLong(),
        date = entity.date,
        calorieGoal = entity.calorieGoal,
        proteinGoal = entity.proteinGoal,
        carbsGoal = entity.carbsGoal,
        fatGoal = entity.fatGoal,
      )
    }

    return CalorieTrackBackup(
      backupFormatVersion = CalorieTrackBackup.CURRENT_BACKUP_FORMAT_VERSION,
      applicationVersion = "1.0",
      exportTimestamp = timestampProvider(),
      customFoods = customFoods,
      recipes = recipes,
      recipeIngredients = recipeIngredients,
      mealEntries = mealEntries,
      dailyGoals = dailyGoals,
    )
  }

  /**
   * Generates formatted human-readable JSON string of the backup.
   */
  suspend fun exportBackupJson(): String {
    val backup = createBackup()
    return json.encodeToString(CalorieTrackBackup.serializer(), backup)
  }

  /**
   * Parses JSON string into CalorieTrackBackup. Throws exception if syntax is invalid.
   */
  fun parseBackupJson(jsonString: String): CalorieTrackBackup {
    return json.decodeFromString(CalorieTrackBackup.serializer(), jsonString)
  }

  /**
   * Atomically validates and restores user data from the backup into SQLite.
   * Clears existing user data and restores entities while keeping built-in foods untouched.
   */
  suspend fun restoreBackup(backup: CalorieTrackBackup): Result<Unit> {
    val validation = BackupValidator.validate(backup)
    if (validation is ValidationResult.Invalid) {
      return Result.failure(IllegalArgumentException(validation.reason))
    }

    return try {
      transactionRunner {
        // 1. Delete existing user data in reverse referential dependency order
        mealEntryDao.deleteAllMealEntries()
        recipeDao.deleteAllRecipeIngredients()
        recipeDao.deleteAllRecipes()
        foodDao.deleteAllCustomFoods()
        dailyGoalDao.deleteAllDailyGoals()

        // 2. Insert custom foods first (preserving backup IDs)
        val foodEntities = backup.customFoods.map { b ->
          FoodEntity(
            id = b.id,
            name = b.name,
            servingDescription = b.servingDescription,
            servingGrams = b.servingGrams,
            caloriesPer100g = b.caloriesPer100g,
            proteinPer100g = b.proteinPer100g,
            carbsPer100g = b.carbsPer100g,
            fatPer100g = b.fatPer100g,
            isCustom = true,
            isActive = b.isActive,
            dataSource = b.dataSource,
            sourceId = b.sourceId,
            searchKeywords = b.searchKeywords,
          )
        }
        if (foodEntities.isNotEmpty()) {
          foodDao.insertAll(foodEntities)
        }

        // 3. Insert recipes (preserving backup IDs)
        val recipeEntities = backup.recipes.map { b ->
          RecipeEntity(
            id = b.id,
            name = b.name,
            cookedWeightGrams = b.cookedWeightGrams,
            totalCalories = b.totalCalories,
            totalProtein = b.totalProtein,
            totalCarbs = b.totalCarbs,
            totalFat = b.totalFat,
            caloriesPer100g = b.caloriesPer100g,
            proteinPer100g = b.proteinPer100g,
            carbsPer100g = b.carbsPer100g,
            fatPer100g = b.fatPer100g,
            isActive = b.isActive,
            searchKeywords = b.searchKeywords,
          )
        }
        if (recipeEntities.isNotEmpty()) {
          recipeDao.insertRecipes(recipeEntities)
        }

        // 4. Insert recipe ingredients (preserving backup IDs)
        val ingredientEntities = backup.recipeIngredients.map { b ->
          RecipeIngredientEntity(
            id = b.id,
            recipeId = b.recipeId,
            foodId = b.foodId,
            quantityGrams = b.quantityGrams,
          )
        }
        if (ingredientEntities.isNotEmpty()) {
          recipeDao.insertIngredients(ingredientEntities)
        }

        // 5. Insert meal entries (preserving backup IDs and immutable snapshots)
        val mealEntities = backup.mealEntries.map { b ->
          MealEntryEntity(
            id = b.id,
            date = b.date,
            mealType = b.mealType,
            foodId = b.foodId,
            recipeId = b.recipeId,
            entryName = b.entryName,
            quantityGrams = b.quantityGrams,
            calories = b.calories,
            protein = b.protein,
            carbs = b.carbs,
            fat = b.fat,
          )
        }
        if (mealEntities.isNotEmpty()) {
          mealEntryDao.insertAll(mealEntities)
        }

        // 6. Insert daily goals
        val goalEntities = backup.dailyGoals.map { b ->
          DailyGoalEntity(
            date = b.date,
            calorieGoal = b.calorieGoal,
            proteinGoal = b.proteinGoal,
            carbsGoal = b.carbsGoal,
            fatGoal = b.fatGoal,
          )
        }
        if (goalEntities.isNotEmpty()) {
          dailyGoalDao.insertAll(goalEntities)
        }
      }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
