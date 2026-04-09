package com.mobile.bible.kjv.ui.vm

import java.util.Calendar

/**
 * 特殊日计算器
 * 计算各种基督教节日的日期
 */
object SpecialDayCalculator {

    /**
     * 检查指定日期是否是特殊日，返回特殊日类型（按优先级）
     * 优先级：节日 > 主日 > null
     */
    fun getSpecialDayType(calendar: Calendar): SpecialDayType? {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        // 1. 检查固定日期节日
        when {
            // 圣诞节 - 12月25日
            month == Calendar.DECEMBER && day == 25 -> return SpecialDayType.CHRISTMAS
            // 主显节 - 1月6日
            month == Calendar.JANUARY && day == 6 -> return SpecialDayType.EPIPHANY
            // 保罗日 - 1月25日
            month == Calendar.JANUARY && day == 25 -> return SpecialDayType.PAUL_DAY
        }
        
        // 2. 检查复活节相关节日
        val easter = calculateEaster(year)
        
        // 复活节
        if (isSameDay(calendar, easter)) return SpecialDayType.EASTER
        
        // 棕枝主日 - 复活节前7天
        val palmSunday = (easter.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7) }
        if (isSameDay(calendar, palmSunday)) return SpecialDayType.PALM_SUNDAY
        
        // 受难日 - 复活节前2天
        val goodFriday = (easter.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -2) }
        if (isSameDay(calendar, goodFriday)) return SpecialDayType.GOOD_FRIDAY
        
        // 升天节 - 复活节后39天(第40天)
        val ascension = (easter.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 39) }
        if (isSameDay(calendar, ascension)) return SpecialDayType.ASCENSION
        
        // 五旬节 - 复活节后49天(第50天)
        val pentecost = (easter.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 49) }
        if (isSameDay(calendar, pentecost)) return SpecialDayType.PENTECOST
        
        // 3. 检查感恩节 - 11月第4个周四
        if (month == Calendar.NOVEMBER && dayOfWeek == Calendar.THURSDAY) {
            val thanksgiving = calculateNthWeekday(year, Calendar.NOVEMBER, Calendar.THURSDAY, 4)
            if (isSameDay(calendar, thanksgiving)) return SpecialDayType.THANKSGIVING
        }
        
        // 4. 最后检查主日（每周日）
        if (dayOfWeek == Calendar.SUNDAY) return SpecialDayType.SUNDAY
        
        return null
    }

    /**
     * 计算复活节日期 (Computus - Anonymous Gregorian algorithm)
     * 复活节 = 春分后第一个满月后的第一个周日
     */
    fun calculateEaster(year: Int): Calendar {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31 - 1  // 0-indexed for Calendar
        val day = ((h + l - 7 * m + 114) % 31) + 1
        
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    /**
     * 计算某月第N个星期几的日期
     * @param nthWeek 第几个（1-5）
     */
    private fun calculateNthWeekday(year: Int, month: Int, dayOfWeek: Int, nthWeek: Int): Calendar {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        
        // 找到该月第一个指定星期几
        while (cal.get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        // 加上 (n-1) 周
        cal.add(Calendar.WEEK_OF_MONTH, nthWeek - 1)
        
        return cal
    }

    /**
     * 判断两个日期是否是同一天
     */
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
               cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    /**
     * 计算从起始日期到目标日期之间，有多少个非特殊日（用于常规进度计算）
     */
    fun countNormalDays(startDate: Calendar, endDate: Calendar): Int {
        var count = 0
        val current = startDate.clone() as Calendar
        
        while (current.before(endDate) || isSameDay(current, endDate)) {
            if (getSpecialDayType(current) == null) {
                count++
            }
            current.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        // 不包含结束日期本身（因为结束日期如果是普通日，会单独处理）
        if (isSameDay(current, endDate) && getSpecialDayType(endDate) == null) {
            count--
        }
        
        return count
    }
}
