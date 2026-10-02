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

  @Query("SELECT * FROM foods ORDER BY name COLLATE NOCASE ASC")
  fun getAll(): Flow<List<FoodEntity>>

  @Query("""
    SELECT * FROM foods
    WHERE name LIKE '%' || :query || '%'
       OR search_keywords LIKE '%' || :query || '%'
    ORDER BY name COLLATE NOCASE ASC
  """)
  fun searchByName(query: String): Flow<List<FoodEntity>>


  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(food: FoodEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(foods: List<FoodEntity>)

  @Update
  suspend fun update(food: FoodEntity)

  @Delete
  suspend fun delete(food: FoodEntity)

  @Query("SELECT COUNT(*) FROM foods")
  suspend fun count(): Int
}
