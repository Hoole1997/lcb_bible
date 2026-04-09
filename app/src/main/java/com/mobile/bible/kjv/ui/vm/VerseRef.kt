package com.mobile.bible.kjv.ui.vm

/**
 * 经文引用
 */
data class VerseRef(
    val bookName: String,
    val chapter: Int,
    val verse: Int
) {
    companion object {
        /**
         * 从字符串解析经文引用，如 "Luke 2:11"
         */
        fun parse(ref: String): VerseRef? {
            return try {
                // 处理格式如 "1 Corinthians 15:20" 或 "Luke 2:11"
                val lastSpace = ref.lastIndexOf(' ')
                val bookName = ref.substring(0, lastSpace)
                val chapterVerse = ref.substring(lastSpace + 1)
                val parts = chapterVerse.split(":")
                VerseRef(bookName, parts[0].toInt(), parts[1].toInt())
            } catch (e: Exception) {
                null
            }
        }
    }
}
