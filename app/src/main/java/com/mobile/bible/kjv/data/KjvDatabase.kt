package com.mobile.bible.kjv.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mobile.bible.kjv.data.dao.AnswerDao
import com.mobile.bible.kjv.data.dao.BookDao
import com.mobile.bible.kjv.data.dao.MyPrayerDao
import com.mobile.bible.kjv.data.dao.UserPropsDao
import com.mobile.bible.kjv.data.dao.VerseDao
import com.mobile.bible.kjv.data.entity.BookEntity
import com.mobile.bible.kjv.data.entity.LevelEntity
import com.mobile.bible.kjv.data.entity.MyPrayerEntity
import com.mobile.bible.kjv.data.entity.QuestionEntity
import com.mobile.bible.kjv.data.entity.UserPropsEntity
import com.mobile.bible.kjv.data.entity.VerseEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [BookEntity::class, VerseEntity::class, LevelEntity::class, QuestionEntity::class, UserPropsEntity::class, MyPrayerEntity::class],
    version = 2,
    exportSchema = true
)
abstract class KjvDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun verseDao(): VerseDao
    abstract fun answerDao(): AnswerDao
    abstract fun userPropsDao(): UserPropsDao
    abstract fun myPrayerDao(): MyPrayerDao

    companion object {
        private const val DATABASE_NAME = "kjv_database"
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS my_prayers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        username TEXT NOT NULL,
                        content TEXT NOT NULL,
                        visibility TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: KjvDatabase? = null

        fun getInstance(context: Context): KjvDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): KjvDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                KjvDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            INSTANCE?.let { database ->
                                // 检查并填充答题数据
                                KjvDataPopulator.populateAnswerDataIfNeeded(context, database)
                            }
                        }
                    }
                })
                .build()
        }
    }
}
