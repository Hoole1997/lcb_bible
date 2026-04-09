package com.mobile.bible.kjv.ui.vm

/**
 * 特殊日类型
 */
enum class SpecialDayType(val displayName: String) {
    SUNDAY("Sunday"),           // 主日 - 每周日
    CHRISTMAS("Christmas"),     // 圣诞节 - 12月25日
    GOOD_FRIDAY("Good Friday"), // 受难日 - 复活节前周五
    EASTER("Easter"),           // 复活节 - 春分后满月周日
    ASCENSION("Ascension"),     // 升天节 - 复活节后40天
    PENTECOST("Pentecost"),     // 五旬节 - 复活节后50天
    THANKSGIVING("Thanksgiving"), // 感恩节 - 11月第4个周四
    EPIPHANY("Epiphany"),       // 主显节 - 1月6日
    PALM_SUNDAY("Palm Sunday"), // 棕枝主日 - 复活节前周日
    PAUL_DAY("Paul Day")        // 保罗日 - 1月25日
}

/**
 * 特殊日经文配置
 */
data class SpecialDayVerse(
    val type: SpecialDayType,
    val morningVerse: VerseRef?,
    val eveningVerse: VerseRef?
) {
    companion object {
        /**
         * 所有特殊节日的经文配置
         */
        val FESTIVAL_VERSES = mapOf(
            SpecialDayType.CHRISTMAS to SpecialDayVerse(
                SpecialDayType.CHRISTMAS,
                VerseRef.parse("Luke 2:11"),
                VerseRef.parse("Isaiah 9:6")
            ),
            SpecialDayType.GOOD_FRIDAY to SpecialDayVerse(
                SpecialDayType.GOOD_FRIDAY,
                VerseRef.parse("Matthew 27:50"),
                VerseRef.parse("Isaiah 53:5")
            ),
            SpecialDayType.EASTER to SpecialDayVerse(
                SpecialDayType.EASTER,
                VerseRef.parse("Matthew 28:6"),
                VerseRef.parse("1 Corinthians 15:20")
            ),
            SpecialDayType.ASCENSION to SpecialDayVerse(
                SpecialDayType.ASCENSION,
                VerseRef.parse("Acts 1:11"),
                VerseRef.parse("Ephesians 1:20")
            ),
            SpecialDayType.PENTECOST to SpecialDayVerse(
                SpecialDayType.PENTECOST,
                VerseRef.parse("Acts 2:4"),
                VerseRef.parse("Joel 2:28")
            ),
            SpecialDayType.THANKSGIVING to SpecialDayVerse(
                SpecialDayType.THANKSGIVING,
                VerseRef.parse("Psalms 100:4"),
                VerseRef.parse("Philippians 4:6")
            ),
            SpecialDayType.EPIPHANY to SpecialDayVerse(
                SpecialDayType.EPIPHANY,
                VerseRef.parse("Matthew 2:11"),
                VerseRef.parse("Isaiah 60:1")
            ),
            SpecialDayType.PALM_SUNDAY to SpecialDayVerse(
                SpecialDayType.PALM_SUNDAY,
                VerseRef.parse("John 12:13"),
                VerseRef.parse("Zechariah 9:9")
            ),
            SpecialDayType.PAUL_DAY to SpecialDayVerse(
                SpecialDayType.PAUL_DAY,
                VerseRef.parse("Acts 9:5"),
                null  // 晚读无指定经文
            )
        )
    }
}
