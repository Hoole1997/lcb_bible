package com.mobile.bible.kjv.data

import android.content.Context
import android.util.Log
import com.mobile.bible.kjv.data.entity.BookEntity
import com.mobile.bible.kjv.data.entity.LevelEntity
import com.mobile.bible.kjv.data.entity.QuestionEntity
import com.mobile.bible.kjv.data.entity.VerseEntity
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

/**
 * 从 assets 读取 JSON 数据并填充数据库
 */
object KjvDataPopulator {

    private const val TAG = "KjvDataPopulator"

    suspend fun populateDatabase(context: Context, database: KjvDatabase) {
        try {
            Log.d(TAG, "开始填充圣经数据库...")

            // 1. 填充书籍目录
            populateBooks(context, database)

            // 2. 填充经文数据
            populateVerses(context, database)

            // 3. 填充答题数据
            populateAnswerData(context, database)

            Log.d(TAG, "圣经数据库填充完成")
        } catch (e: Exception) {
            Log.e(TAG, "填充数据库失败", e)
        }
    }

    suspend fun populateAnswerDataIfNeeded(context: Context, database: KjvDatabase) {
        try {
            val levelCount = database.answerDao().getLevelCount()
            if (levelCount == 0) {
                Log.d(TAG, "检测到答题数据为空，开始填充...")
                populateAnswerData(context, database)
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查/填充答题数据失败", e)
        }
    }

    private suspend fun populateAnswerData(context: Context, database: KjvDatabase) {
        val jsonString = context.assets.open("bible_answer.json").bufferedReader().use { it.readText() }

        val type = object : TypeToken<List<AnswerLevelJson>>() {}.type
        val levels: List<AnswerLevelJson> = Gson().fromJson(jsonString, type)

        val levelEntities = mutableListOf<LevelEntity>()
        val questionEntities = mutableListOf<QuestionEntity>()

        for (levelData in levels) {
            levelEntities.add(
                LevelEntity(
                    level = levelData.level,
                    theme = levelData.theme
                )
            )

            for (question in levelData.questions) {
                val options = question.options
                questionEntities.add(
                    QuestionEntity(
                        questionId = question.question_id,
                        level = levelData.level,
                        questionText = question.question_text,
                        optionA = options.getOrElse(0) { "" },
                        optionB = options.getOrElse(1) { "" },
                        optionC = options.getOrElse(2) { "" },
                        optionD = options.getOrElse(3) { "" },
                        answerIndex = question.answer_index,
                        reference = question.reference
                    )
                )
            }
        }

        database.answerDao().insertAllLevels(levelEntities)
        database.answerDao().insertAllQuestions(questionEntities)
        Log.d(TAG, "已插入 ${levelEntities.size} 个关卡和 ${questionEntities.size} 个问题")
    }

    private suspend fun populateBooks(context: Context, database: KjvDatabase) {
        val jsonString = context.assets.open("bible.json").bufferedReader().use { it.readText() }
        // 移除JSON中的注释
        val cleanJson = jsonString.replace(Regex("//.*"), "")

        val type = object : TypeToken<List<BookJson>>() {}.type
        val books: List<BookJson> = Gson().fromJson(cleanJson, type)

        Log.d(TAG, "解析到 ${books.size} 本书籍")
        if (books.isNotEmpty()) {
            Log.d(TAG, "第一本书: ${books.first().name}, 最后一本书: ${books.last().name}")
        }

        val bookEntities = books.map { book ->
            BookEntity(
                id = book.id,
                testament = book.testament,
                name = book.name,
                chapters = book.chapters
            )
        }

        database.bookDao().insertAll(bookEntities)
        Log.d(TAG, "已插入 ${bookEntities.size} 本书籍")

        // 验证插入结果
        val savedCount = database.bookDao().getBookCount()
        Log.d(TAG, "数据库中书籍总数: $savedCount")
    }

    // 书籍名称别名映射 (bible_content.json 名称 -> bible.json 名称)
    private val bookNameAliases = mapOf(
        "I Samuel" to "1 Samuel",
        "II Samuel" to "2 Samuel",
        "I Kings" to "1 Kings",
        "II Kings" to "2 Kings",
        "I Chronicles" to "1 Chronicles",
        "II Chronicles" to "2 Chronicles",
        "I Corinthians" to "1 Corinthians",
        "II Corinthians" to "2 Corinthians",
        "I Thessalonians" to "1 Thessalonians",
        "II Thessalonians" to "2 Thessalonians",
        "I Timothy" to "1 Timothy",
        "II Timothy" to "2 Timothy",
        "I Peter" to "1 Peter",
        "II Peter" to "2 Peter",
        "I John" to "1 John",
        "II John" to "2 John",
        "III John" to "3 John",
        "Revelation of John" to "Revelation"
    )

    /**
     * 将 bible_content.json 中的书籍名称标准化为 bible.json 中使用的名称
     */
    private fun normalizeBookName(contentName: String): String {
        return bookNameAliases[contentName] ?: contentName
    }

    private suspend fun populateVerses(context: Context, database: KjvDatabase) {
        // 1. 先加载书籍目录，建立 name -> id 映射
        val bibleJsonString = context.assets.open("bible.json").bufferedReader().use { it.readText() }
        val cleanJson = bibleJsonString.replace(Regex("//.*"), "")
        val booksType = object : TypeToken<List<BookJson>>() {}.type
        val books: List<BookJson> = Gson().fromJson(cleanJson, booksType)
        val bookNameToId: Map<String, Int> = books.associate { it.name to it.id }

        // 2. 加载统一的经文内容文件
        val contentJsonString = context.assets.open("bible_content.json").bufferedReader().use { it.readText() }
        val bibleContent: BibleContentJson = Gson().fromJson(contentJsonString, BibleContentJson::class.java)

        val verseEntities = mutableListOf<VerseEntity>()

        // 3. 遍历所有书籍，通过 name 映射到 bookId（使用别名标准化）
        for (book in bibleContent.books) {
            val normalizedName = normalizeBookName(book.name)
            val bookId = bookNameToId[normalizedName]
            if (bookId == null) {
                Log.w(TAG, "未找到书籍映射: ${book.name} (标准化后: $normalizedName)")
                continue
            }

            for (chapter in book.chapters) {
                for (verse in chapter.verses) {
                    verseEntities.add(
                        VerseEntity(
                            bookId = bookId,
                            chapter = chapter.chapter,
                            verse = verse.verse,
                            text = verse.text
                        )
                    )
                }
            }
            Log.d(TAG, "已处理书籍: ${book.name}, 共 ${book.chapters.size} 章")
        }

        database.verseDao().insertAll(verseEntities)
        Log.d(TAG, "已插入经文总数: ${verseEntities.size} 节")
    }

    // JSON 解析数据类 - 书籍目录 (bible.json)
    private data class BookJson(
        @SerializedName("id") val id: Int,
        @SerializedName("testament") val testament: String,
        @SerializedName("name") val name: String,
        @SerializedName("chapters") val chapters: Int
    )

    // JSON 解析数据类 - 经文内容 (bible_content.json)
    private data class BibleContentJson(
        @SerializedName("books") val books: List<ContentBookJson>
    )

    private data class ContentBookJson(
        @SerializedName("name") val name: String,
        @SerializedName("chapters") val chapters: List<ContentChapterJson>
    )

    private data class ContentChapterJson(
        @SerializedName("chapter") val chapter: Int,
        @SerializedName("verses") val verses: List<ContentVerseJson>
    )

    private data class ContentVerseJson(
        @SerializedName("verse") val verse: Int,
        @SerializedName("text") val text: String
    )

    // 答题 JSON 解析数据类
    private data class AnswerLevelJson(
        @SerializedName("level") val level: Int,
        @SerializedName("theme") val theme: String,
        @SerializedName("questions") val questions: List<AnswerQuestionJson>
    )

    private data class AnswerQuestionJson(
        @SerializedName("question_id") val question_id: String,
        @SerializedName("question_text") val question_text: String,
        @SerializedName("options") val options: List<String>,
        @SerializedName("answer_index") val answer_index: String,
        @SerializedName("reference") val reference: String
    )
}
