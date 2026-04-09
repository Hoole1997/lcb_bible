package com.mobile.bible.kjv.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mobile.bible.kjv.data.entity.UserPropsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPropsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUserProps(userProps: UserPropsEntity)

    @Update
    suspend fun updateUserProps(userProps: UserPropsEntity)

    @Query("SELECT * FROM user_props WHERE id = 1")
    suspend fun getUserProps(): UserPropsEntity?

    @Query("SELECT * FROM user_props WHERE id = 1")
    fun getUserPropsFlow(): Flow<UserPropsEntity?>

    @Query("SELECT coins FROM user_props WHERE id = 1")
    suspend fun getCoins(): Int?

    @Query("SELECT coins FROM user_props WHERE id = 1")
    fun getCoinsFlow(): Flow<Int?>

    @Query("UPDATE user_props SET coins = coins + :amount WHERE id = 1")
    suspend fun addCoins(amount: Int)

    @Query("UPDATE user_props SET coins = coins - :amount WHERE id = 1 AND coins >= :amount")
    suspend fun deductCoins(amount: Int): Int

    @Query("SELECT timePropCount FROM user_props WHERE id = 1")
    suspend fun getTimePropCount(): Int?

    @Query("UPDATE user_props SET timePropCount = timePropCount + :count WHERE id = 1")
    suspend fun addTimeProp(count: Int)

    @Query("UPDATE user_props SET timePropCount = timePropCount - 1 WHERE id = 1 AND timePropCount > 0")
    suspend fun useTimeProp(): Int

    @Query("SELECT eraserPropCount FROM user_props WHERE id = 1")
    suspend fun getEraserPropCount(): Int?

    @Query("UPDATE user_props SET eraserPropCount = eraserPropCount + :count WHERE id = 1")
    suspend fun addEraserProp(count: Int)

    @Query("UPDATE user_props SET eraserPropCount = eraserPropCount - 1 WHERE id = 1 AND eraserPropCount > 0")
    suspend fun useEraserProp(): Int

    @Query("SELECT highestPassedLevel FROM user_props WHERE id = 1")
    suspend fun getHighestPassedLevel(): Int?

    @Query("SELECT highestPassedLevel FROM user_props WHERE id = 1")
    fun getHighestPassedLevelFlow(): Flow<Int?>

    @Query("UPDATE user_props SET highestPassedLevel = :level WHERE id = 1 AND :level > highestPassedLevel")
    suspend fun setHighestPassedLevel(level: Int): Int
}
