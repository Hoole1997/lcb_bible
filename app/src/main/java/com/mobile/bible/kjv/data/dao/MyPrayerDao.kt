package com.mobile.bible.kjv.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mobile.bible.kjv.data.entity.MyPrayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MyPrayerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayer(prayer: MyPrayerEntity): Long

    @Query("SELECT * FROM my_prayers ORDER BY createdAt DESC")
    fun getAllPrayersFlow(): Flow<List<MyPrayerEntity>>

    @Query("DELETE FROM my_prayers WHERE id = :id")
    suspend fun deletePrayerById(id: Long): Int

    @Query("UPDATE my_prayers SET visibility = :visibility WHERE id = :id")
    suspend fun updateVisibilityById(id: Long, visibility: String): Int

    @Query("UPDATE my_prayers SET createdAt = :createdAt WHERE id = :id")
    suspend fun updateCreatedAtById(id: Long, createdAt: Long): Int

    @Query("SELECT id FROM my_prayers ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestPrayerId(): Long?
}
