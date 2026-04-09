package com.mobile.bible.kjv.prefs

import com.remax.base.ext.KvIntDelegate
import com.remax.base.ext.KvLongDelegate
import com.remax.base.ext.KvStringDelegate

/**
 * 每日经文设置
 * 存储起始日期用于计算相对天数
 */
object DailyVerseSettings {
    // 起始日期时间戳 (毫秒)，用于计算天数
    var startDate: Long by KvLongDelegate("daily_verse_start_date", 0L)
    
    // 主日经文索引（每周日递增）
    var sundayVerseIndex: Int by KvIntDelegate("sunday_verse_index", 0)
    
    // 上次主日日期（用于判断是否需要递增索引）
    var lastSundayDate: Long by KvLongDelegate("last_sunday_date", 0L)

    // 上次更新进度的日期（用于每日重置）
    var lastProgressDate: Long by KvLongDelegate("last_progress_task_date", 0L)

    // 已完成的任务ID列表 (逗号分隔，例如 "1,2")
    var completedStepIds: String? by KvStringDelegate("completed_step_ids", "")
}
