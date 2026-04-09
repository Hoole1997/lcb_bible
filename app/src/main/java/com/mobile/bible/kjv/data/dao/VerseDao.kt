package com.mobile.bible.kjv.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mobile.bible.kjv.data.entity.VerseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VerseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(verses: List<VerseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(verse: VerseEntity)

    @Query("SELECT * FROM verses WHERE bookId = :bookId AND chapter = :chapter ORDER BY verse")
    fun getVersesByChapter(bookId: Int, chapter: Int): Flow<List<VerseEntity>>

    @Query("SELECT * FROM verses WHERE bookId = :bookId AND chapter = :chapter ORDER BY verse")
    suspend fun getVersesByChapterList(bookId: Int, chapter: Int): List<VerseEntity>

    @Query("SELECT * FROM verses WHERE bookId = :bookId AND chapter = :chapter AND verse = :verse")
    suspend fun getVerse(bookId: Int, chapter: Int, verse: Int): VerseEntity?

    @Query("SELECT * FROM verses WHERE bookId = :bookId ORDER BY chapter, verse")
    fun getVersesByBook(bookId: Int): Flow<List<VerseEntity>>

    @Query("SELECT * FROM verses WHERE text LIKE '%' || :keyword || '%'")
    fun searchVerses(keyword: String): Flow<List<VerseEntity>>

    @Query("SELECT COUNT(*) FROM verses WHERE bookId = :bookId AND chapter = :chapter")
    suspend fun getVerseCountByChapter(bookId: Int, chapter: Int): Int

    @Query("SELECT COUNT(*) FROM verses")
    suspend fun getVerseCount(): Int

    @Query("SELECT COUNT(*) FROM verses WHERE bookId = :bookId")
    suspend fun getVerseCountByBook(bookId: Int): Int
}
