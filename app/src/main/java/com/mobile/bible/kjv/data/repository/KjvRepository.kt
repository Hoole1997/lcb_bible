package com.mobile.bible.kjv.data.repository

import android.content.Context
import com.mobile.bible.kjv.data.KjvDatabase
import com.mobile.bible.kjv.data.entity.BookEntity
import com.mobile.bible.kjv.data.entity.LevelEntity
import com.mobile.bible.kjv.data.entity.MyPrayerEntity
import com.mobile.bible.kjv.data.entity.QuestionEntity
import com.mobile.bible.kjv.data.entity.UserPropsEntity
import com.mobile.bible.kjv.data.entity.VerseEntity
import com.mobile.bible.kjv.prefs.PlayerWallSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

/**
 * 圣经数据仓库 - 提供统一的数据访问接口
 */
class KjvRepository(context: Context) {

    private val database = KjvDatabase.getInstance(context)
    private val bookDao = database.bookDao()
    private val verseDao = database.verseDao()
    private val answerDao = database.answerDao()
    private val userPropsDao = database.userPropsDao()
    private val myPrayerDao = database.myPrayerDao()

    companion object {
        private const val CORRECT_ANSWERS_TO_PASS = 3
        private val _dailyProgressFlow = kotlinx.coroutines.flow.MutableStateFlow(0)
    }

    // ==================== 书籍相关 ====================

    fun getAllBooks(): Flow<List<BookEntity>> = bookDao.getAllBooks()

    suspend fun getAllBooksList(): List<BookEntity> = bookDao.getAllBooksList()

    fun getBooksByTestament(testament: String): Flow<List<BookEntity>> =
        bookDao.getBooksByTestament(testament)

    fun getOldTestamentBooks(): Flow<List<BookEntity>> = getBooksByTestament("OT")

    fun getNewTestamentBooks(): Flow<List<BookEntity>> = getBooksByTestament("NT")

    suspend fun getBookById(bookId: Int): BookEntity? = bookDao.getBookById(bookId)

    suspend fun getBookByName(name: String): BookEntity? = bookDao.getBookByName(name)

    // ==================== 经文相关 ====================

    fun getVersesByChapter(bookId: Int, chapter: Int): Flow<List<VerseEntity>> =
        verseDao.getVersesByChapter(bookId, chapter)

    suspend fun getVersesByChapterList(bookId: Int, chapter: Int): List<VerseEntity> =
        verseDao.getVersesByChapterList(bookId, chapter)

    suspend fun getVerse(bookId: Int, chapter: Int, verse: Int): VerseEntity? =
        verseDao.getVerse(bookId, chapter, verse)

    fun getVersesByBook(bookId: Int): Flow<List<VerseEntity>> =
        verseDao.getVersesByBook(bookId)

    fun searchVerses(keyword: String): Flow<List<VerseEntity>> =
        verseDao.searchVerses(keyword)

    // ==================== 统计相关 ====================

    suspend fun getBookCount(): Int = bookDao.getBookCount()

    suspend fun getVerseCount(): Int = verseDao.getVerseCount()

    suspend fun getVerseCountByBook(bookId: Int): Int = verseDao.getVerseCountByBook(bookId)

    suspend fun getVerseCountByChapter(bookId: Int, chapter: Int): Int =
        verseDao.getVerseCountByChapter(bookId, chapter)

    suspend fun isDatabasePopulated(): Boolean = getBookCount() > 0

    // ==================== 答题关卡相关 ====================

    fun getAllLevels(): Flow<List<LevelEntity>> = answerDao.getAllLevels()

    suspend fun getAllLevelsList(): List<LevelEntity> = answerDao.getAllLevelsList()

    suspend fun getLevelByNumber(level: Int): LevelEntity? = answerDao.getLevelByNumber(level)

    suspend fun getLevelCount(): Int = answerDao.getLevelCount()

    // ==================== 答题问题相关 ====================

    fun getQuestionsByLevel(level: Int): Flow<List<QuestionEntity>> =
        answerDao.getQuestionsByLevel(level)

    suspend fun getQuestionsByLevelList(level: Int): List<QuestionEntity> =
        answerDao.getQuestionsByLevelList(level)

    suspend fun getQuestionById(questionId: String): QuestionEntity? =
        answerDao.getQuestionById(questionId)

    suspend fun getQuestionCount(): Int = answerDao.getQuestionCount()

    suspend fun getQuestionCountByLevel(level: Int): Int = answerDao.getQuestionCountByLevel(level)

    // ==================== 学习进度相关 ====================

    suspend fun getHighestPassedLevel(): Int = userPropsDao.getHighestPassedLevel() ?: 0

    fun getHighestPassedLevelFlow() = userPropsDao.getHighestPassedLevelFlow()

    suspend fun setHighestPassedLevel(level: Int) {
        initUserPropsIfNeeded()
        userPropsDao.setHighestPassedLevel(level)
    }

    fun getCorrectAnswersToPass(): Int = CORRECT_ANSWERS_TO_PASS

    // ==================== 金币和道具相关 ====================

    suspend fun initUserPropsIfNeeded() {
        if (userPropsDao.getUserProps() == null) {
            userPropsDao.insertUserProps(UserPropsEntity())
        }
    }

    suspend fun getUserProps(): UserPropsEntity? = userPropsDao.getUserProps()

    fun getUserPropsFlow() = userPropsDao.getUserPropsFlow()

    suspend fun getCoins(): Int = userPropsDao.getCoins() ?: 0

    fun getCoinsFlow() = userPropsDao.getCoinsFlow()

    suspend fun addCoins(amount: Int) {
        initUserPropsIfNeeded()
        userPropsDao.addCoins(amount)
    }

    suspend fun deductCoins(amount: Int): Boolean {
        initUserPropsIfNeeded()
        return userPropsDao.deductCoins(amount) > 0
    }

    suspend fun getTimePropCount(): Int = userPropsDao.getTimePropCount() ?: 0

    suspend fun getEraserPropCount(): Int = userPropsDao.getEraserPropCount() ?: 0

    suspend fun buyTimeProp(): Boolean {
        initUserPropsIfNeeded()
        val price = UserPropsEntity.PROP_PRICE
        if (deductCoins(price)) {
            userPropsDao.addTimeProp(1)
            return true
        }
        return false
    }

    suspend fun buyTimeProp(quantity: Int): Boolean {
        initUserPropsIfNeeded()
        val totalPrice = UserPropsEntity.PROP_PRICE * quantity
        if (deductCoins(totalPrice)) {
            userPropsDao.addTimeProp(quantity)
            return true
        }
        return false
    }

    suspend fun buyEraserProp(): Boolean {
        initUserPropsIfNeeded()
        val price = UserPropsEntity.PROP_PRICE
        if (deductCoins(price)) {
            userPropsDao.addEraserProp(1)
            return true
        }
        return false
    }

    suspend fun buyEraserProp(quantity: Int): Boolean {
        initUserPropsIfNeeded()
        val totalPrice = UserPropsEntity.PROP_PRICE * quantity
        if (deductCoins(totalPrice)) {
            userPropsDao.addEraserProp(quantity)
            return true
        }
        return false
    }

    suspend fun useTimeProp(): Boolean {
        return userPropsDao.useTimeProp() > 0
    }

    suspend fun useEraserProp(): Boolean {
        return userPropsDao.useEraserProp() > 0
    }

    suspend fun addTimeProp(count: Int) {
        initUserPropsIfNeeded()
        userPropsDao.addTimeProp(count)
    }

    suspend fun addEraserProp(count: Int) {
        initUserPropsIfNeeded()
        userPropsDao.addEraserProp(count)
    }

    suspend fun addTimePropAndEraserProp(count: Int) {
        initUserPropsIfNeeded()
        userPropsDao.addTimeProp(count)
        userPropsDao.addEraserProp(count)
    }

    fun isRewardLevel(level: Int): Boolean {
        return when (level) {
            3, 5, 7, 10 -> true
            else -> level > 10 && level % 5 == 0
        }
    }

    // ==================== 每日灵修进度相关 ====================

    fun getDailyProgressFlow(): Flow<Int> {
        // 初始化 flow 的值
        GlobalScope.launch(Dispatchers.IO) {
            _dailyProgressFlow.emit(calculateDailyProgress())
        }
        return _dailyProgressFlow
    }

    /**
     * 标记每日任务完成
     */
    suspend fun markDailyStepComplete(stepId: Int) {
        checkAndResetDailyProgress()
        
        val currentIds = getCompletedStepIds().toMutableSet()
        if (currentIds.add(stepId)) {
            // save back
            com.mobile.bible.kjv.prefs.DailyVerseSettings.completedStepIds = currentIds.joinToString(",")
            // update flow
            _dailyProgressFlow.emit(calculateDailyProgress())
        }
    }

    /**
     * 获取今日已完成的任务ID集合
     */
    fun getCompletedStepIds(): Set<Int> {
        checkAndResetDailyProgress()
        val idsStr = com.mobile.bible.kjv.prefs.DailyVerseSettings.completedStepIds
        if (idsStr.isNullOrEmpty()) return emptySet()
        return idsStr!!.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    }

    /**
     * 获取每日任务总数
     */
    fun getDailyTotalSteps(): Int = 3

    private fun checkAndResetDailyProgress() {
        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        if (com.mobile.bible.kjv.prefs.DailyVerseSettings.lastProgressDate != today) {
            // New day, reset
            com.mobile.bible.kjv.prefs.DailyVerseSettings.lastProgressDate = today
            com.mobile.bible.kjv.prefs.DailyVerseSettings.completedStepIds = ""
            // We should also emit 0 to flow if we are in a suspend function or have scope, 
            // but checkAndReset is often called synchronously. 
            // ideally we update flow when we detect reset.
             _dailyProgressFlow.tryEmit(0)
        }
    }

    private fun calculateDailyProgress(): Int {
        checkAndResetDailyProgress()
        val completedCount = getCompletedStepIds().size
        val total = getDailyTotalSteps()
        if (total == 0) return 0
        return (completedCount.toFloat() / total * 100).toInt()
    }

    // ==================== 祈祷墙相关 ====================

    suspend fun saveMyPrayer(
        username: String,
        content: String,
        visibility: String,
        createdAt: Long = System.currentTimeMillis()
    ): Long {
        return myPrayerDao.insertPrayer(
            MyPrayerEntity(
                username = username,
                content = content,
                visibility = visibility,
                createdAt = createdAt
            )
        )
    }

    fun getMyPrayersFlow(): Flow<List<MyPrayerEntity>> = myPrayerDao.getAllPrayersFlow()

    suspend fun deleteMyPrayerById(id: Long): Boolean = myPrayerDao.deletePrayerById(id) > 0

    suspend fun updateMyPrayerVisibility(id: Long, visibility: String): Boolean {
        return myPrayerDao.updateVisibilityById(id, visibility) > 0
    }

    suspend fun stickyMyPrayerToTop(id: Long): Boolean {
        val previousStickyId = PlayerWallSettings.stickyPrayerId
        val now = System.currentTimeMillis()
        val updated = myPrayerDao.updateCreatedAtById(id, now) > 0
        if (updated) {
            PlayerWallSettings.stickyPrayerId = id
            if (previousStickyId > 0L && previousStickyId != id) {
                // 新置顶生效后，上一条置顶视为倒计时结束并移除
                myPrayerDao.deletePrayerById(previousStickyId)
            }
        }
        return updated
    }

    suspend fun stickyLatestMyPrayerToTop(): Boolean {
        val latestId = myPrayerDao.getLatestPrayerId() ?: return false
        return stickyMyPrayerToTop(latestId)
    }
}
