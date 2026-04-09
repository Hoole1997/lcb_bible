package com.mobile.bible.kjv.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mobile.bible.kjv.data.entity.LevelEntity
import com.mobile.bible.kjv.data.entity.QuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnswerDao {

    // Level 相关操作
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLevels(levels: List<LevelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLevel(level: LevelEntity)

    @Query("SELECT * FROM levels ORDER BY level")
    fun getAllLevels(): Flow<List<LevelEntity>>

    @Query("SELECT * FROM levels ORDER BY level")
    suspend fun getAllLevelsList(): List<LevelEntity>

    @Query("SELECT * FROM levels WHERE level = :level")
    suspend fun getLevelByNumber(level: Int): LevelEntity?

    @Query("SELECT COUNT(*) FROM levels")
    suspend fun getLevelCount(): Int

    // Question 相关操作
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllQuestions(questions: List<QuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity)

    @Query("SELECT * FROM questions WHERE level = :level ORDER BY questionId")
    fun getQuestionsByLevel(level: Int): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE level = :level ORDER BY questionId")
    suspend fun getQuestionsByLevelList(level: Int): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE questionId = :questionId")
    suspend fun getQuestionById(questionId: String): QuestionEntity?

    @Query("SELECT * FROM questions ORDER BY level, questionId")
    fun getAllQuestions(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions ORDER BY level, questionId")
    suspend fun getAllQuestionsList(): List<QuestionEntity>

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getQuestionCount(): Int

    @Query("SELECT COUNT(*) FROM questions WHERE level = :level")
    suspend fun getQuestionCountByLevel(level: Int): Int
}
