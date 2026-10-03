package com.calorietrack.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

  @Query("SELECT * FROM foods WHERE id = :id LIMIT 1")
  suspend fun getById(id: Long): FoodEntity?

  @Query("SELECT * FROM foods WHERE is_active = 1 ORDER BY name COLLATE NOCASE ASC")
  fun getAll(): Flow<List<FoodEntity>>

  @Query("""
    SELECT * FROM foods
    WHERE is_active = 1
      AND (
        name LIKE '%' || :query || '%'
        OR search_keywords LIKE '%' || :query || '%'
      )
    ORDER BY name COLLATE NOCASE ASC
  """)
  fun searchByName(query: String): Flow<List<FoodEntity>>

  @Query("SELECT * FROM foods WHERE is_custom = 1 AND is_active = 1 ORDER BY name COLLATE NOCASE ASC")
  fun getMyFoods(): Flow<List<FoodEntity>>

  @Query("""
    SELECT * FROM foods
    WHERE is_custom = 1
      AND is_active = 1
      AND (
        name LIKE '%' || :query || '%'
        OR search_keywords LIKE '%' || :query || '%'
      )
    ORDER BY name COLLATE NOCASE ASC
  """)
  fun searchMyFoods(query: String): Flow<List<FoodEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(food: FoodEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(foods: List<FoodEntity>)

  @Update
  suspend fun update(food: FoodEntity)

  @Delete
  suspend fun delete(food: FoodEntity)

  @Query("UPDATE foods SET is_active = 0 WHERE id = :id AND is_custom = 1")
  suspend fun archiveFood(id: Long): Int

  @Query("SELECT MAX(id) FROM foods")
  suspend fun getMaxId(): Long?

  @Query("SELECT COUNT(*) FROM foods")
  suspend fun count(): Int

  @Query("SELECT COUNT(*) FROM foods WHERE is_active = 1")
  suspend fun countActive(): Int

  @Query("SELECT COUNT(*) FROM foods WHERE is_custom = 1 AND is_active = 1")
  suspend fun countActiveCustom(): Int
}
