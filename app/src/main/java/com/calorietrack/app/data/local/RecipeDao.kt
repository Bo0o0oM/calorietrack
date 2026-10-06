package com.calorietrack.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

  @Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
  suspend fun getRecipeById(id: Long): RecipeEntity?

  @Query("SELECT * FROM recipes WHERE is_active = 1 ORDER BY name COLLATE NOCASE ASC")
  fun getAllActiveRecipes(): Flow<List<RecipeEntity>>

  @Query("""
    SELECT * FROM recipes
    WHERE is_active = 1
      AND (
        name LIKE '%' || :query || '%'
        OR search_keywords LIKE '%' || :query || '%'
      )
    ORDER BY name COLLATE NOCASE ASC
  """)
  fun searchActiveRecipes(query: String): Flow<List<RecipeEntity>>

  @Query("""
    SELECT
      recipe_ingredients.id AS id,
      recipe_ingredients.recipe_id AS recipeId,
      recipe_ingredients.food_id AS foodId,
      recipe_ingredients.quantity_grams AS quantityGrams,
      COALESCE(foods.name, 'Unknown Food') AS foodName,
      COALESCE(foods.calories_per_100g, 0.0) AS caloriesPer100g,
      COALESCE(foods.protein_per_100g, 0.0) AS proteinPer100g,
      COALESCE(foods.carbs_per_100g, 0.0) AS carbsPer100g,
      COALESCE(foods.fat_per_100g, 0.0) AS fatPer100g,
      (COALESCE(foods.calories_per_100g, 0.0) * recipe_ingredients.quantity_grams / 100.0) AS calories,
      (COALESCE(foods.protein_per_100g, 0.0) * recipe_ingredients.quantity_grams / 100.0) AS protein,
      (COALESCE(foods.carbs_per_100g, 0.0) * recipe_ingredients.quantity_grams / 100.0) AS carbs,
      (COALESCE(foods.fat_per_100g, 0.0) * recipe_ingredients.quantity_grams / 100.0) AS fat
    FROM recipe_ingredients
    LEFT JOIN foods ON recipe_ingredients.food_id = foods.id
    WHERE recipe_ingredients.recipe_id = :recipeId
    ORDER BY recipe_ingredients.id ASC
  """)
  fun getIngredientsWithFood(recipeId: Long): Flow<List<RecipeIngredientWithFood>>

  @Query("SELECT * FROM recipe_ingredients WHERE recipe_id = :recipeId ORDER BY id ASC")
  suspend fun getIngredientsForRecipe(recipeId: Long): List<RecipeIngredientEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRecipe(recipe: RecipeEntity): Long

  @Update
  suspend fun updateRecipe(recipe: RecipeEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertIngredients(ingredients: List<RecipeIngredientEntity>)

  @Query("DELETE FROM recipe_ingredients WHERE recipe_id = :recipeId")
  suspend fun deleteIngredientsForRecipe(recipeId: Long)

  @Query("UPDATE recipes SET is_active = 0 WHERE id = :recipeId")
  suspend fun archiveRecipe(recipeId: Long): Int

  @Query("SELECT COUNT(*) FROM recipes WHERE is_active = 1")
  suspend fun countActiveRecipes(): Int

  @Query("SELECT * FROM recipes ORDER BY id ASC")
  suspend fun getAllRecipes(): List<RecipeEntity>

  @Query("SELECT * FROM recipe_ingredients ORDER BY id ASC")
  suspend fun getAllRecipeIngredients(): List<RecipeIngredientEntity>

  @Query("DELETE FROM recipe_ingredients")
  suspend fun deleteAllRecipeIngredients(): Int

  @Query("DELETE FROM recipes")
  suspend fun deleteAllRecipes(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRecipes(recipes: List<RecipeEntity>)

  @Transaction
  suspend fun saveRecipeWithIngredients(
    recipe: RecipeEntity,
    ingredients: List<RecipeIngredientEntity>
  ): Long {
    val targetId = if (recipe.id == 0L) {
      insertRecipe(recipe)
    } else {
      updateRecipe(recipe)
      deleteIngredientsForRecipe(recipe.id)
      recipe.id
    }

    val mappedIngredients = ingredients.map {
      it.copy(id = 0L, recipeId = targetId)
    }
    insertIngredients(mappedIngredients)
    return targetId
  }
}
