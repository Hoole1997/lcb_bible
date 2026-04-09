package com.mobile.bible.kjv.ui.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.bible.kjv.data.repository.KjvRepository
import com.mobile.bible.kjv.debug.DebugDateTimeOverride
import com.mobile.bible.kjv.prefs.DailyVerseSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import com.mobile.bible.kjv.data.model.DevotionalDay
import com.mobile.bible.kjv.ui.adapter.JourneyItem
import com.mobile.bible.kjv.ui.adapter.JourneyStatus
import com.mobile.bible.kjv.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 每日经文数据类
 */
data class DailyVerse(
    val text: String,           // 经文内容
    val bookName: String,       // 书籍名称 (如 Genesis)
    val chapter: Int,           // 章节号
    val verse: Int,             // 经文号
    val version: String = "KJV", // 版本
    val specialDay: SpecialDayType? = null  // 特殊日类型（如有）
) {
    /**
     * 格式化位置显示，如 "Genesis 1:1 (KJV)"
     */
    fun getPosition(): String = "$bookName $chapter:$verse ($version)"
}

/**
 * 时段枚举
 */
enum class TimeSlot {
    MORNING,   // 早晨 6:00-12:00
    EVENING,   // 晚间 18:00-24:00
    OTHER      // 其他时段
}

class TodayViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = KjvRepository(application)
    
    private val _dailyVerse = MutableStateFlow<DailyVerse?>(null)
    val dailyVerse: StateFlow<DailyVerse?> = _dailyVerse.asStateFlow()

    private val _todaysJourneyItems = MutableStateFlow<List<JourneyItem>>(emptyList())
    val todaysJourneyItems: StateFlow<List<JourneyItem>> = _todaysJourneyItems.asStateFlow()

    // 用户首次使用时的起始日期，保存后持久化
    private val startDate: Long by lazy {
        if (DailyVerseSettings.startDate == 0L) {
            val today = getTodayStartMillis()
            DailyVerseSettings.startDate = today
            today
        } else {
            DailyVerseSettings.startDate
        }
    }

    init {
        loadDailyVerse()
        loadTodaysJourney()
        
        // Observe daily progress changes to refresh the journey list
        viewModelScope.launch {
            repository.getDailyProgressFlow().collect {
                loadTodaysJourney()
            }
        }
    }

    /**
     * 加载每日经文
     */
    fun loadDailyVerse() {
        viewModelScope.launch {
            val verse = calculateDailyVerse()
            _dailyVerse.value = verse
        }
    }

    /**
     * 获取当前时段
     */
    private fun getCurrentTimeSlot(): TimeSlot {
        DebugDateTimeOverride.get()?.let { override ->
            return override.timeSlot
        }
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 6..11 -> TimeSlot.MORNING     // 6:00-12:00
            hour in 18..23 -> TimeSlot.EVENING    // 18:00-24:00
            else -> TimeSlot.OTHER
        }
    }

    /**
     * 获取有效时段（非读经时段返回最近时段）
     */
    private fun getEffectiveTimeSlot(): TimeSlot {
        val slot = getCurrentTimeSlot()
        if (slot != TimeSlot.OTHER) return slot
        
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (hour < 6) TimeSlot.EVENING else TimeSlot.MORNING
    }

    /**
     * 获取今天0点的时间戳
     */
    private fun getTodayStartMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * 获取用于每日经文计算的“当前日期”（支持 Debug 覆盖）
     */
    private fun getCurrentCalendarForDailyVerse(): Calendar {
        return DebugDateTimeOverride.get()?.calendar ?: Calendar.getInstance()
    }

    /**
     * 获取用于每日经文计算的当天 0 点时间戳（支持 Debug 覆盖）
     */
    private fun getTodayStartMillisForDailyVerse(): Long {
        return getCurrentCalendarForDailyVerse().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * 加载今日旅程数据
     */
    private fun loadTodaysJourney() {
        viewModelScope.launch {
            val items = withContext(Dispatchers.IO) {
                try {
                    val daysSinceInstall = calculateDaysSinceInstall()
                    val allDays = parseDevotionalPlan()
                    
                    // 找到对应的天数数据，如果没有找到，默认显示第一天或循环显示
                    // 根据需求：index 0 代表第1天，index 1 代表第2天
                    val planIndex = if (allDays.isNotEmpty()) {
                        (daysSinceInstall % allDays.size).toInt()
                    } else {
                        0
                    }
                    
                    val dayData = allDays.find { it.index == planIndex } ?: allDays.firstOrNull()
                    
            val completedIds = repository.getCompletedStepIds()

                    dayData?.steps?.map { step ->
                        val (iconRes, bgRes) = when (step.stepId) {
                            1 -> R.drawable.svg_journey_guide to R.mipmap.img_today_journey_item_a
                            2 -> R.drawable.svg_journey_devotional to R.mipmap.img_today_journey_item_b
                            else -> R.drawable.svg_journey_daily to R.mipmap.img_today_journey_item_c
                        }
                        
                        val subtitle = when(step.stepId) {
                            1 -> step.content.verseReference ?: ""
                            else -> step.content.title ?: ""
                        }
                        
                        // "step_id": 1 -> verse_text
                        // "step_id": 2 -> content.text
                        // "step_id": 3 -> content.text
                        val quoteText = when(step.stepId) {
                            1 -> step.content.verseText ?: ""
                            else -> step.content.text ?: ""
                        }

                        val status = if (completedIds.contains(step.stepId)) JourneyStatus.DONE else JourneyStatus.START

                        JourneyItem(
                            stepId = step.stepId,
                            title = "${step.entryTitle} (${step.entryDuration})",
                            subtitle = subtitle,
                            status = status, 
                            iconRes = iconRes,
                            bgRes = bgRes,
                            tags = step.content.keywords ?: emptyList(),
                            quoteText = quoteText,
                            verseReference = step.content.verseReference ?: ""
                        )
                    } ?: emptyList()


                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList<JourneyItem>()
                }
            }
            _todaysJourneyItems.value = items
        }
    }

    /**
     * Mark a step as complete and refresh the list
     */
    fun markStepAsComplete(stepId: Int) {
        viewModelScope.launch {
            repository.markDailyStepComplete(stepId)
            loadTodaysJourney() // Refresh list to update status
        }
    }

    private fun calculateDaysSinceInstall(): Long {
        val today = getTodayStartMillis()
        val diff = today - startDate
        return if (diff < 0) 0 else TimeUnit.MILLISECONDS.toDays(diff)
    }

    private fun parseDevotionalPlan(): List<DevotionalDay> {
        val jsonString = getApplication<Application>().assets.open("daily_devotional_plan.json").bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<DevotionalDay>>() {}.type
        return Gson().fromJson(jsonString, type)
    }

    /**
     * 计算每日经文（主入口）
     * 优先级：节日 > 主日 > 常规读经
     */
    private suspend fun calculateDailyVerse(): DailyVerse? {
        val today = getCurrentCalendarForDailyVerse()
        val specialDayType = SpecialDayCalculator.getSpecialDayType(today)
        
        return when {
            // 1. 节日经文（非主日）
            specialDayType != null && specialDayType != SpecialDayType.SUNDAY -> {
                getSpecialDayVerse(specialDayType)
            }
            // 2. 主日经文
            specialDayType == SpecialDayType.SUNDAY -> {
                getSundayVerse()
            }
            // 3. 常规经文
            else -> {
                getRegularVerse()
            }
        }
    }

    /**
     * 获取节日经文
     */
    private suspend fun getSpecialDayVerse(type: SpecialDayType): DailyVerse? {
        val config = SpecialDayVerse.FESTIVAL_VERSES[type] ?: return null
        val timeSlot = getEffectiveTimeSlot()
        
        val verseRef = when (timeSlot) {
            TimeSlot.MORNING -> config.morningVerse
            TimeSlot.EVENING -> config.eveningVerse ?: config.morningVerse // 晚读无配置时用早读
            TimeSlot.OTHER -> config.morningVerse
        }
        
        return verseRef?.let { ref ->
            fetchVerseByRef(ref, type)
        }
    }

    /**
     * 获取主日经文（早晚读同一节，每周日切换）
     */
    private suspend fun getSundayVerse(): DailyVerse? {
        val todayMillis = getTodayStartMillisForDailyVerse()
        
        // 检查是否是新的一周日（需要切换到下一节）
        if (DailyVerseSettings.lastSundayDate != todayMillis) {
            if (DailyVerseSettings.lastSundayDate != 0L) {
                // 不是首次，索引+1
                DailyVerseSettings.sundayVerseIndex++
            }
            DailyVerseSettings.lastSundayDate = todayMillis
        }
        
        // 根据索引获取经文
        val index = DailyVerseSettings.sundayVerseIndex
        return getVerseByGlobalIndex(index, SpecialDayType.SUNDAY)
    }

    /**
     * 获取常规经文（排除特殊日的进度计算）
     */
    private suspend fun getRegularVerse(): DailyVerse? {
        val startCal = Calendar.getInstance().apply { timeInMillis = startDate }
        val today = getCurrentCalendarForDailyVerse()
        
        // 计算从起始日到今天，有多少个普通读经日
        var normalDays = 0
        val current = startCal.clone() as Calendar
        
        while (current.before(today) || isSameDay(current, today)) {
            if (SpecialDayCalculator.getSpecialDayType(current) == null) {
                if (isSameDay(current, today)) {
                    // 今天是普通日，计算今天的索引
                    break
                }
                normalDays++
            }
            current.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        // 计算全局索引
        val timeSlot = getEffectiveTimeSlot()
        val globalIndex = when (timeSlot) {
            TimeSlot.MORNING -> normalDays * 2
            TimeSlot.EVENING -> normalDays * 2 + 1
            TimeSlot.OTHER -> normalDays * 2  // 不会到这里
        }
        
        return getVerseByGlobalIndex(globalIndex, null)
    }

    /**
     * 根据经文引用获取经文
     */
    private suspend fun fetchVerseByRef(ref: VerseRef, specialDay: SpecialDayType?): DailyVerse? {
        // 根据书名查找书籍
        val book = repository.getBookByName(ref.bookName) ?: return null
        val verseEntity = repository.getVerse(book.id, ref.chapter, ref.verse)
        
        return verseEntity?.let {
            DailyVerse(
                text = it.text,
                bookName = ref.bookName,
                chapter = ref.chapter,
                verse = ref.verse,
                specialDay = specialDay
            )
        }
    }

    /**
     * 根据全局索引获取经文（遍历圣经）
     */
    private suspend fun getVerseByGlobalIndex(index: Int, specialDay: SpecialDayType?): DailyVerse? {
        val books = repository.getAllBooksList()
        if (books.isEmpty()) return null
        
        val totalVerses = repository.getVerseCount()
        val loopedIndex = if (totalVerses > 0) index % totalVerses else index
        
        var remainingIndex = loopedIndex
        
        for (book in books) {
            for (chapter in 1..book.chapters) {
                val versesInChapter = repository.getVerseCountByChapter(book.id, chapter)
                
                if (remainingIndex < versesInChapter) {
                    val verseNumber = remainingIndex + 1
                    val verseEntity = repository.getVerse(book.id, chapter, verseNumber)
                    
                    return verseEntity?.let {
                        DailyVerse(
                            text = it.text,
                            bookName = book.name,
                            chapter = chapter,
                            verse = verseNumber,
                            specialDay = specialDay
                        )
                    }
                }
                
                remainingIndex -= versesInChapter
            }
        }
        
        return null
    }

    /**
     * 判断两个日期是否是同一天
     */
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
               cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }
}